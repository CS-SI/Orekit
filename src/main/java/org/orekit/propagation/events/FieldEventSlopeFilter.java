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
    extends FieldAbstractDetector<FieldEventSlopeFilter<D, T>, T> {

    /** Number of past transformers updates stored. */
    private static final int HISTORY_SIZE = 100;

    /** Wrapped event detector. */
    private final D rawDetector;

    /** Filter to use. */
    private final FilterType filter;

    /** Transformers of the g function. */
    private final Transformer[] transformers;

    /** Update time of the transformers. */
    private final FieldAbsoluteDate<T>[] updates;

    /** Indicator for forward integration. */
    private boolean forward;

    /** Extreme time encountered so far. */
    private FieldAbsoluteDate<T> extremeT;

    /** Wrap an {@link EventDetector event detector}.
     * @param rawDetector event detector to wrap
     * @param filter filter to use
     */
    public FieldEventSlopeFilter(final D rawDetector, final FilterType filter) {
        this(rawDetector.getMaxCheckInterval(), rawDetector.getThreshold(),
             rawDetector.getMaxIterationCount(), new LocalHandler<>(),
             rawDetector, filter);
    }

    /** Protected constructor with full parameters.
     * <p>
     * This constructor is not public as users are expected to use the builder
     * API with the various {@code withXxx()} methods to set up the instance
     * in a readable manner without using a huge amount of parameters.
     * </p>
     * @param maxCheck maximum checking interval
     * @param threshold convergence threshold (s)
     * @param maxIter maximum number of iterations in the event time search
     * @param handler event handler to call at event occurrences
     * @param rawDetector event detector to wrap
     * @param filter filter to use
     */
    @SuppressWarnings("unchecked")
    protected FieldEventSlopeFilter(final FieldAdaptableInterval<T> maxCheck, final T threshold,
                                    final int maxIter, final FieldEventHandler<T> handler,
                                    final D rawDetector, final FilterType filter) {
        super(maxCheck, threshold, maxIter, handler);
        this.rawDetector  = rawDetector;
        this.filter       = filter;
        this.transformers = new Transformer[HISTORY_SIZE];
        this.updates      = (FieldAbsoluteDate<T>[]) Array.newInstance(FieldAbsoluteDate.class, HISTORY_SIZE);
    }

    /** {@inheritDoc} */
    @Override
    protected FieldEventSlopeFilter<D, T> create(final FieldAdaptableInterval<T> newMaxCheck, final T newThreshold,
                                                 final int newMaxIter, final FieldEventHandler<T> newHandler) {
        return new FieldEventSlopeFilter<>(newMaxCheck, newThreshold, newMaxIter, newHandler, rawDetector, filter);
    }

    /**
     * Get the wrapped raw detector.
     * @return the wrapped raw detector
     */
    public D getDetector() {
        return rawDetector;
    }

    /**  {@inheritDoc} */
    public void init(final FieldSpacecraftState<T> s0,
                     final FieldAbsoluteDate<T> t) {
        super.init(s0, t);

        // delegate to raw detector
        rawDetector.init(s0, t);

        // initialize events triggering logic
        forward  = t.compareTo(s0.getDate()) >= 0;
        extremeT = forward ?
                   FieldAbsoluteDate.getPastInfinity(t.getField()) :
                   FieldAbsoluteDate.getFutureInfinity(t.getField());
        Arrays.fill(transformers, Transformer.UNINITIALIZED);
        Arrays.fill(updates, extremeT);

    }

    /**  {@inheritDoc} */
    public T g(final FieldSpacecraftState<T> s) {

        final T rawG = rawDetector.g(s);

        // search which transformer should be applied to g
        if (forward) {
            final int last = transformers.length - 1;
            if (extremeT.compareTo(s.getDate()) < 0) {
                // we are at the forward end of the history

                // check if a new rough root has been crossed
                final Transformer previous = transformers[last];
                final Transformer next     = filter.selectTransformer(previous, rawG.getReal(), forward);
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
                final Transformer next     = filter.selectTransformer(previous, rawG.getReal(), forward);
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

    /** Local handler. */
    private static class LocalHandler<D extends FieldEventDetector<T>, T extends CalculusFieldElement<T>> implements FieldEventHandler<T> {

        /** {@inheritDoc} */
        public Action eventOccurred(final FieldSpacecraftState<T> s, final FieldEventDetector<T> detector, final boolean increasing) {
            @SuppressWarnings("unchecked")
            final FieldEventSlopeFilter<D, T> esf = (FieldEventSlopeFilter<D, T>) detector;
            return esf.rawDetector.getHandler().eventOccurred(s, esf.rawDetector, esf.filter.getTriggeredIncreasing());
        }

        /** {@inheritDoc} */
        @Override
        public FieldSpacecraftState<T> resetState(final FieldEventDetector<T> detector, final FieldSpacecraftState<T> oldState) {
            @SuppressWarnings("unchecked")
            final FieldEventSlopeFilter<D, T> esf = (FieldEventSlopeFilter<D, T>) detector;
            return esf.rawDetector.getHandler().resetState(esf.rawDetector, oldState);
        }

    }

}
