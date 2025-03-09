/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

/** Wrapper used to detect only increasing or decreasing events.
 *
 * <p>This class is heavily based on the class EventFilter from the
 * Hipparchus library. The changes performed consist in replacing
 * raw types (double and double arrays) with space dynamics types
 * ({@link FieldAbsoluteDate}, {@link FieldSpacecraftState}).</p>
 *
 * <p>General {@link FieldEventDetector events} are defined implicitly
 * by a {@link FieldEventDetector#g(FieldSpacecraftState) g function} crossing
 * zero. This function needs to be continuous in the event neighborhood,
 * and its sign must remain consistent between events. This implies that
 * during an orbit propagation, events triggered are alternately events
 * for which the function increases from negative to positive values,
 * and events for which the function decreases from positive to
 * negative values.
 * </p>
 *
 * <p>Sometimes, users are only interested in one type of event (say
 * increasing events for example) and not in the other type. In these
 * cases, looking precisely for all events location and triggering
 * events that will later be ignored is a waste of computing time.</p>
 *
 * <p>Users can wrap a regular {@link FieldEventDetector event detector} in
 * an instance of this class and provide this wrapping instance to
 * a {@link org.orekit.propagation.FieldPropagator}
 * in order to avoid wasting time looking for uninteresting events.
 * The wrapper will intercept the calls to the {@link
 * FieldEventDetector#g(FieldSpacecraftState) g function} and to the {@link
 * FieldEventHandler#eventOccurred(FieldSpacecraftState, FieldEventDetector, boolean)
 * eventOccurred} method in order to ignore uninteresting events. The
 * wrapped regular {@link FieldEventDetector event detector} will then see only
 * the interesting events, i.e. either only {@code increasing} events or
 * only {@code decreasing} events. The number of calls to the {@link
 * FieldEventDetector#g(FieldSpacecraftState) g function} will also be reduced.</p>
 * @see FieldEventEnablingPredicateFilter
 * @param <D> type of the detector
 * @param <T> type of the field elements
 */

public class FieldEventSlopeFilter<D extends FieldEventDetector<T>, T extends CalculusFieldElement<T>>
    implements FieldEventDetector<T> {

    /** Number of past transformers updates stored. */
    private static final int HISTORY_SIZE = 100;

    /** Wrapped event detector. */
    private final D rawDetector;

    /** Filter to use. */
    private final FilterType filterType;

    /** Transformers of the g function. */
    private final Transformer[] transformers;

    /** Update time of the transformers. */
    private final FieldAbsoluteDate<T>[] updates;

    /** Event detection settings. */
    private final FieldEventDetectionSettings<T> detectionSettings;

    /** Specialized event handler. */
    private final LocalHandler<D, T> handler;

    /** Indicator for forward integration. */
    private boolean forward;

    /** Extreme time encountered so far. */
    private FieldAbsoluteDate<T> extremeT;

    /** Wrap an {@link EventDetector event detector}.
     * @param rawDetector event detector to wrap
     * @param filterType filter to use
     */
    public FieldEventSlopeFilter(final D rawDetector, final FilterType filterType) {
        this(rawDetector.getDetectionSettings(), rawDetector, filterType);
    }

    /** Constructor with full parameters.
     * @param detectionSettings event detection settings
     * @param rawDetector event detector to wrap
     * @param filterType filter to use
     * since 13.0
     */
    @SuppressWarnings("unchecked")
    public FieldEventSlopeFilter(final FieldEventDetectionSettings<T> detectionSettings,
                                 final D rawDetector, final FilterType filterType) {
        this.detectionSettings = detectionSettings;
        this.handler = new LocalHandler<>();
        this.rawDetector  = rawDetector;
        this.filterType = filterType;
        this.transformers = new Transformer[HISTORY_SIZE];
        this.updates      = (FieldAbsoluteDate<T>[]) Array.newInstance(FieldAbsoluteDate.class, HISTORY_SIZE);
    }

    /**
     * Builds a new instance from the input detection settings.
     * @param settings event detection settings to be used
     * @return a new detector
     */
    public FieldEventSlopeFilter<D, T> withDetectionSettings(final FieldEventDetectionSettings<T> settings) {
        return new FieldEventSlopeFilter<>(settings, rawDetector, filterType);
    }

    /** Get filter type.
     * @return filter type
     * @since 13.0
     */
    public FilterType getFilterType() {
        return filterType;
    }

    @Override
    public FieldEventHandler<T> getHandler() {
        return handler;
    }

    @Override
    public FieldEventDetectionSettings<T> getDetectionSettings() {
        return detectionSettings;
    }

    /**
     * Get the wrapped raw detector.
     * @return the wrapped raw detector
     */
    public D getDetector() {
        return rawDetector;
    }

    /**  {@inheritDoc} */
    @Override
    public void init(final FieldSpacecraftState<T> s0,
                     final FieldAbsoluteDate<T> t) {
        FieldEventDetector.super.init(s0, t);

        // delegate to raw detector
        rawDetector.init(s0, t);

        // initialize events triggering logic
        forward  = FieldAbstractDetector.checkIfForward(s0, t);
        extremeT = forward ?
                   FieldAbsoluteDate.getPastInfinity(t.getField()) :
                   FieldAbsoluteDate.getFutureInfinity(t.getField());
        Arrays.fill(transformers, Transformer.UNINITIALIZED);
        Arrays.fill(updates, extremeT);

    }

    /**  {@inheritDoc} */
    @Override
    public void reset(final FieldSpacecraftState<T> state, final FieldAbsoluteDate<T> target) {
        FieldEventDetector.super.reset(state, target);
        rawDetector.reset(state, target);
    }

    /**  {@inheritDoc} */
    @Override
    public void finish(final FieldSpacecraftState<T> state) {
        FieldEventDetector.super.finish(state);
        rawDetector.finish(state);
    }

    /**  {@inheritDoc} */
    @Override
    public T g(final FieldSpacecraftState<T> s) {

        final T rawG = rawDetector.g(s);

        // search which transformer should be applied to g
        if (isForward()) {
            final int last = transformers.length - 1;
            if (extremeT.compareTo(s.getDate()) < 0) {
                // we are at the forward end of the history

                // check if a new rough root has been crossed
                final Transformer previous = transformers[last];
                final Transformer next     = filterType.selectTransformer(previous, rawG.getReal(), forward);
                if (next != previous) {
                    // there is a root somewhere between extremeT and t.
                    // the new transformer is valid for t (this is how we have just computed
                    // it above), but it is in fact valid on both sides of the root, so
                    // it was already valid before t and even up to previous time. We store
                    // the switch at extremeT for safety, to ensure the previous transformer
                    // is not applied too close of the root
                    System.arraycopy(updates,      1, updates,      0, last);
                    System.arraycopy(transformers, 1, transformers, 0, last);
                    updates[last]      = extremeT;
                    transformers[last] = next;
                }

                extremeT = s.getDate();

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
                final Transformer next     = filterType.selectTransformer(previous, rawG.getReal(), forward);
                if (next != previous) {
                    // there is a root somewhere between extremeT and t.
                    // the new transformer is valid for t (this is how we have just computed
                    // it above), but it is in fact valid on both sides of the root, so
                    // it was already valid before t and even up to previous time. We store
                    // the switch at extremeT for safety, to ensure the previous transformer
                    // is not applied too close of the root
                    System.arraycopy(updates,      0, updates,      1, updates.length - 1);
                    System.arraycopy(transformers, 0, transformers, 1, transformers.length - 1);
                    updates[0]      = extremeT;
                    transformers[0] = next;
                }

                extremeT = s.getDate();

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

    /** Check if the current propagation is forward or backward.
     * @return true if the current propagation is forward
     */
    public boolean isForward() {
        return forward;
    }

    /** Local handler. */
    private static class LocalHandler<D extends FieldEventDetector<T>, T extends CalculusFieldElement<T>> implements FieldEventHandler<T> {

        /** {@inheritDoc} */
        public Action eventOccurred(final FieldSpacecraftState<T> s, final FieldEventDetector<T> detector, final boolean increasing) {
            final FieldEventSlopeFilter<D, T> esf = (FieldEventSlopeFilter<D, T>) detector;
            return esf.rawDetector.getHandler().eventOccurred(s, esf.rawDetector, esf.filterType.getTriggeredIncreasing());
        }

        /** {@inheritDoc} */
        @Override
        public FieldSpacecraftState<T> resetState(final FieldEventDetector<T> detector, final FieldSpacecraftState<T> oldState) {
            final FieldEventSlopeFilter<D, T> esf = (FieldEventSlopeFilter<D, T>) detector;
            return esf.rawDetector.getHandler().resetState(esf.rawDetector, oldState);
        }

    }

}
