/* Copyright 2022-2025 Luc Maisonobe
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * CS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.orekit.propagation.events;

import java.lang.reflect.Array;
import java.util.Arrays;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.ode.events.Action;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.time.FieldAbsoluteDate;

/** Wrapper used to detect events only when enabled by an external predicated function.
 *
 * <p>General {@link FieldEventDetector events} are defined implicitly
 * by a {@link FieldEventDetector#g(FieldSpacecraftState) g function} crossing
 * zero. This implies that during an orbit propagation, events are
 * triggered at all zero crossings.
 * </p>
 *
 * <p>Sometimes, users would like to enable or disable events by themselves,
 * for example to trigger them only for certain orbits, or to check elevation
 * maximums only when elevation itself is positive (i.e. they want to
 * discard elevation maximums below ground). In these cases, looking precisely
 * for all events location and triggering events that will later be ignored
 * is a waste of computing time.</p>
 *
 * <p>Users can wrap a regular {@link FieldEventDetector event detector} in
 * an instance of this class and provide this wrapping instance to
 * a {@link org.orekit.propagation.FieldPropagator}
 * in order to avoid wasting time looking for uninteresting events.
 * The wrapper will intercept the calls to the {@link
 * FieldEventDetector#g(FieldSpacecraftState) g function} and to the {@link
 * FieldEventHandler#eventOccurred(FieldSpacecraftState, FieldEventDetector, boolean)
 * eventOccurred} method in order to ignore uninteresting events. The
 * wrapped regular {@link FieldEventDetector event detector} will the see only
 * the interesting events, i.e. either only events that occur when a
 * user-provided event enabling predicate function is true, ignoring all events
 * that occur when the event enabling predicate function is false. The number of
 * calls to the {@link FieldEventDetector#g(FieldSpacecraftState) g function} will also be
 * reduced.</p>
 * @param <T> type of the field elements
 * @see FieldEventSlopeFilter
 * @since 12.0
 */
public class FieldEventEnablingPredicateFilter<T extends CalculusFieldElement<T>> implements FieldDetectorModifier<T> {

    /** Number of past transformers updates stored. */
    private static final int HISTORY_SIZE = 100;

    /** Wrapped event detector. */
    private final FieldEventDetector<T> rawDetector;

    /** Enabling predicate function. */
    private final FieldEnablingPredicate<T> predicate;

    /** Transformers of the g function. */
    private final Transformer[] transformers;

    /** Update time of the transformers. */
    private final FieldAbsoluteDate<T>[] updates;

    /** Event detection settings. */
    private final FieldEventDetectionSettings<T> detectionSettings;

    /** Specialized event handler. */
    private final LocalHandler<T> handler;

    /** Indicator for forward integration. */
    private boolean forward;

    /** Extreme time encountered so far. */
    private FieldAbsoluteDate<T> extremeT;

    /** Detector function value at extremeT. */
    private T extremeG;

    /** Wrap an {@link EventDetector event detector}.
     * @param rawDetector event detector to wrap
     * @param enabler event enabling predicate function to use
     */
    public FieldEventEnablingPredicateFilter(final FieldEventDetector<T> rawDetector,
                                             final FieldEnablingPredicate<T> enabler) {
        this(rawDetector.getDetectionSettings(), rawDetector, enabler);
    }

    /** Constructor with full parameters.
     * @param detectionSettings event detection settings
     * @param rawDetector event detector to wrap
     * @param enabler event enabling function to use
     * @since 13.0
     */
    @SuppressWarnings("unchecked")
    public FieldEventEnablingPredicateFilter(final FieldEventDetectionSettings<T> detectionSettings,
                                             final FieldEventDetector<T> rawDetector,
                                             final FieldEnablingPredicate<T> enabler) {
        this.detectionSettings = detectionSettings;
        this.handler = new LocalHandler<>();
        this.rawDetector  = rawDetector;
        this.predicate = enabler;
        this.transformers = new Transformer[HISTORY_SIZE];
        this.updates      = (FieldAbsoluteDate<T>[]) Array.newInstance(FieldAbsoluteDate.class, HISTORY_SIZE);
    }

    /**
     * Builds a new instance from the input detection settings.
     * @param settings event detection settings to be used
     * @return a new detector
     */
    public FieldEventEnablingPredicateFilter<T> withDetectionSettings(final FieldEventDetectionSettings<T> settings) {
        return new FieldEventEnablingPredicateFilter<>(settings, rawDetector, predicate);
    }

    /**
     * Get the wrapped raw detector.
     * @return the wrapped raw detector
     */
    public FieldEventDetector<T> getDetector() {
        return rawDetector;
    }

    /**  {@inheritDoc} */
    @Override
    public FieldEventHandler<T> getHandler() {
        return handler;
    }

    /**  {@inheritDoc} */
    @Override
    public FieldEventDetectionSettings<T> getDetectionSettings() {
        return detectionSettings;
    }

    /**
     * Getter for the enabling predicate.
     * @return predicate
     * @since 13.1
     */
    public FieldEnablingPredicate<T> getPredicate() {
        return predicate;
    }

    /**  {@inheritDoc} */
    @Override
    public boolean dependsOnTimeOnly() {
        return false;  // cannot know what predicate needs
    }

    /**  {@inheritDoc} */
    @Override
    public void init(final FieldSpacecraftState<T> s0, final FieldAbsoluteDate<T> t) {
        FieldDetectorModifier.super.init(s0, t);

        // initialize events triggering logic
        forward  = FieldAbstractDetector.checkIfForward(s0, t);
        extremeT = forward ?
                   FieldAbsoluteDate.getPastInfinity(t.getField()) :
                   FieldAbsoluteDate.getFutureInfinity(t.getField());
        extremeG = t.getField().getZero().newInstance(Double.NaN);
        Arrays.fill(transformers, Transformer.UNINITIALIZED);
        Arrays.fill(updates, extremeT);

    }

    @Override
    public void reset(final FieldSpacecraftState<T> state, final FieldAbsoluteDate<T> target) {
        FieldDetectorModifier.super.reset(state, target);
        forward  = FieldAbstractDetector.checkIfForward(state, target);
        extremeT = forward ?
                FieldAbsoluteDate.getPastInfinity(target.getField()) :
                FieldAbsoluteDate.getFutureInfinity(target.getField());
        extremeG = target.getField().getZero().newInstance(Double.NaN);
    }

    /**  {@inheritDoc} */
    @Override
    public T g(final FieldSpacecraftState<T> s) {

        final T       rawG      = rawDetector.g(s);
        final boolean isEnabled = predicate.eventIsEnabled(s, rawDetector, rawG);
        if (extremeG.isNaN()) {
            extremeG = rawG;
        }

        // search which transformer should be applied to g
        if (isForward()) {
            final int last = transformers.length - 1;
            if (extremeT.compareTo(s.getDate()) < 0) {
                // we are at the forward end of the history

                // check if enabled status has changed
                final Transformer previous = transformers[last];
                final Transformer next     = selectTransformer(previous, extremeG, isEnabled);
                if (next != previous) {
                    // there is a status change somewhere between extremeT and t.
                    // the new transformer is valid for t (this is how we have just computed
                    // it above), but it is in fact valid on both sides of the change, so
                    // it was already valid before t and even up to previous time. We store
                    // the switch at extremeT for safety, to ensure the previous transformer
                    // is not applied too close of the root
                    System.arraycopy(updates,      1, updates,      0, last);
                    System.arraycopy(transformers, 1, transformers, 0, last);
                    updates[last]      = extremeT;
                    transformers[last] = next;
                }

                extremeT = s.getDate();
                extremeG = rawG;

                // apply the transform
                return next.transformed(rawG);

            } else {
                // we are in the middle of the history

                // select the transformer
                for (int i = last; i > 0; --i) {
                    if (updates[i].compareTo(s.getDate()) <= 0) {
                        // apply the transform
                        return transformers[i].transformed(rawG);
                    }
                }

                return transformers[0].transformed(rawG);

            }
        } else {
            if (s.getDate().compareTo(extremeT) < 0) {
                // we are at the backward end of the history

                // check if a new rough root has been crossed
                final Transformer previous = transformers[0];
                final Transformer next     = selectTransformer(previous, extremeG, isEnabled);
                if (next != previous) {
                    // there is a status change somewhere between extremeT and t.
                    // the new transformer is valid for t (this is how we have just computed
                    // it above), but it is in fact valid on both sides of the change, so
                    // it was already valid before t and even up to previous time. We store
                    // the switch at extremeT for safety, to ensure the previous transformer
                    // is not applied too close of the root
                    System.arraycopy(updates,      0, updates,      1, updates.length - 1);
                    System.arraycopy(transformers, 0, transformers, 1, transformers.length - 1);
                    updates[0]      = extremeT;
                    transformers[0] = next;
                }

                extremeT = s.getDate();
                extremeG = rawG;

                // apply the transform
                return next.transformed(rawG);

            } else {
                // we are in the middle of the history

                // select the transformer
                for (int i = 0; i < updates.length - 1; ++i) {
                    if (s.getDate().compareTo(updates[i]) <= 0) {
                        // apply the transform
                        return transformers[i].transformed(rawG);
                    }
                }

                return transformers[updates.length - 1].transformed(rawG);

            }
        }

    }

    /** Get next function transformer in the specified direction.
     * @param previous transformer active on the previous point with respect
     * to integration direction (may be null if no previous point is known)
     * @param previousG value of the g function at the previous point
     * @param isEnabled if true the event should be enabled now
     * @return next transformer transformer
     */
    private Transformer selectTransformer(final Transformer previous, final T previousG, final boolean isEnabled) {
        if (isEnabled) {
            // we need to select a transformer that can produce zero crossings,
            // so it is either Transformer.PLUS or Transformer.MINUS
            switch (previous) {
                case UNINITIALIZED :
                    return Transformer.PLUS; // this initial choice is arbitrary, it could have been Transformer.MINUS
                case MIN :
                    return previousG.getReal() >= 0 ? Transformer.MINUS : Transformer.PLUS;
                case MAX :
                    return previousG.getReal() >= 0 ? Transformer.PLUS : Transformer.MINUS;
                default :
                    return previous;
            }
        } else {
            // we need to select a transformer that cannot produce any zero crossings,
            // so it is either Transformer.MAX or Transformer.MIN
            switch (previous) {
                case UNINITIALIZED :
                    return Transformer.MAX; // this initial choice is arbitrary, it could have been Transformer.MIN
                case PLUS :
                    return previousG.getReal() >= 0 ? Transformer.MAX : Transformer.MIN;
                case MINUS :
                    return previousG.getReal() >= 0 ? Transformer.MIN : Transformer.MAX;
                default :
                    return previous;
            }
        }
    }

    /** Check if the current propagation is forward or backward.
     * @return true if the current propagation is forward
     */
    public boolean isForward() {
        return forward;
    }

    /** Local handler.
     * @param <T> type of the field elements
     */
    private static class LocalHandler<T extends CalculusFieldElement<T>> implements FieldEventHandler<T> {

        /** {@inheritDoc} */
        public Action eventOccurred(final FieldSpacecraftState<T> s, final FieldEventDetector<T> detector, final boolean increasing) {
            final FieldEventEnablingPredicateFilter<T> ef = (FieldEventEnablingPredicateFilter<T>) detector;
            final Transformer transformer = ef.forward ? ef.transformers[ef.transformers.length - 1] : ef.transformers[0];
            return ef.rawDetector.getHandler().eventOccurred(s, ef.rawDetector, transformer == Transformer.PLUS ? increasing : !increasing);
        }

        /** {@inheritDoc} */
        @Override
        public FieldSpacecraftState<T> resetState(final FieldEventDetector<T> detector, final FieldSpacecraftState<T> oldState) {
            final FieldEventEnablingPredicateFilter<T> ef = (FieldEventEnablingPredicateFilter<T>) detector;
            return ef.rawDetector.getHandler().resetState(ef.rawDetector, oldState);
        }

    }

}
