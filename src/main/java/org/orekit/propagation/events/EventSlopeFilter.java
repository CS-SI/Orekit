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

import java.util.Arrays;

import org.hipparchus.ode.events.Action;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;

/** Wrapper used to detect only increasing or decreasing events.
 *
 * <p>This class is heavily based on the class EventFilter from the
 * Hipparchus library. The changes performed consist in replacing
 * raw types (double and double arrays) with space dynamics types
 * ({@link AbsoluteDate}, {@link SpacecraftState}).</p>
 *
 * <p>General {@link EventDetector events} are defined implicitly
 * by a {@link EventDetector#g(SpacecraftState) g function} crossing
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
 * <p>Users can wrap a regular {@link EventDetector event detector} in
 * an instance of this class and provide this wrapping instance to
 * a {@link org.orekit.propagation.Propagator}
 * in order to avoid wasting time looking for uninteresting events.
 * The wrapper will intercept the calls to the {@link
 * EventDetector#g(SpacecraftState) g function} and to the {@link
 * EventHandler#eventOccurred(SpacecraftState, EventDetector, boolean)
 * eventOccurred} method in order to ignore uninteresting events. The
 * wrapped regular {@link EventDetector event detector} will then see only
 * the interesting events, i.e. either only {@code increasing} events or
 * only {@code decreasing} events. The number of calls to the {@link
 * EventDetector#g(SpacecraftState) g function} will also be reduced.</p>
 * @see EventEnablingPredicateFilter
 * @param <T> type of the detector
 */

public class EventSlopeFilter<T extends EventDetector> implements EventDetector {

    /** Number of past transformers updates stored. */
    private static final int HISTORY_SIZE = 100;

    /** Wrapped event detector. */
    private final T rawDetector;

    /** Filter to use. */
    private final FilterType filterType;

    /** Transformers of the g function. */
    private final Transformer[] transformers;

    /** Update time of the transformers. */
    private final AbsoluteDate[] updates;

    /** Indicator for forward integration. */
    private boolean forward;

    /** Extreme time encountered so far. */
    private AbsoluteDate extremeT;

    /** Event detection settings. */
    private final EventDetectionSettings detectionSettings;

    /** Specialized event handler. */
    private final LocalHandler<T> handler;

    /** Wrap an {@link EventDetector event detector}.
     * @param rawDetector event detector to wrap
     * @param filter filter to use
     */
    public EventSlopeFilter(final T rawDetector, final FilterType filter) {
        this(rawDetector.getDetectionSettings(), rawDetector, filter);
    }

    /** Constructor with full parameters.
     * @param detectionSettings event detection settings
     * @param rawDetector event detector to wrap
     * @param filterType filter to use
     * @since 13.0
     */
    public EventSlopeFilter(final EventDetectionSettings detectionSettings,
                            final T rawDetector, final FilterType filterType) {
        this.detectionSettings = detectionSettings;
        this.handler = new LocalHandler<>();
        this.rawDetector  = rawDetector;
        this.filterType = filterType;
        this.transformers = new Transformer[HISTORY_SIZE];
        this.updates      = new AbsoluteDate[HISTORY_SIZE];
    }

    /** {@inheritDoc} */
    @Override
    public EventHandler getHandler() {
        return handler;
    }


    @Override
    public EventDetectionSettings getDetectionSettings() {
        return detectionSettings;
    }

    /**
     * Builds a new instance from the input detection settings.
     * @param settings event detection settings to be used
     * @return a new detector
     */
    public EventSlopeFilter<T> withDetectionSettings(final EventDetectionSettings settings) {
        return new EventSlopeFilter<>(settings, rawDetector, filterType);
    }

    /**
     * Get the wrapped raw detector.
     * @return the wrapped raw detector
     * @since 11.1
     */
    public T getDetector() {
        return rawDetector;
    }

    /** Get filter type.
     * @return filter type
     * @deprecated since 13.0 (use getFilterType)
     */
    @Deprecated
    public FilterType getFilter() {
        return getFilterType();
    }

    /** Get filter type.
     * @return filter type
     * @since 13.0
     */
    public FilterType getFilterType() {
        return filterType;
    }

    /**  {@inheritDoc} */
    @Override
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
        EventDetector.super.init(s0, t);

        // delegate to raw detector
        rawDetector.init(s0, t);

        // initialize events triggering logic
        forward  = AbstractDetector.checkIfForward(s0, t);
        extremeT = forward ? AbsoluteDate.PAST_INFINITY : AbsoluteDate.FUTURE_INFINITY;
        Arrays.fill(transformers, Transformer.UNINITIALIZED);
        Arrays.fill(updates, extremeT);

    }

    /**  {@inheritDoc} */
    @Override
    public void reset(final SpacecraftState state, final AbsoluteDate target) {
        EventDetector.super.reset(state, target);
        rawDetector.reset(state, target);
    }

    /**  {@inheritDoc} */
    @Override
    public void finish(final SpacecraftState state) {
        EventDetector.super.finish(state);
        rawDetector.finish(state);
    }

    /**  {@inheritDoc} */
    @Override
    public double g(final SpacecraftState s) {

        final double rawG = rawDetector.g(s);

        // search which transformer should be applied to g
        if (isForward()) {
            final int last = transformers.length - 1;
            if (extremeT.compareTo(s.getDate()) < 0) {
                // we are at the forward end of the history

                // check if a new rough root has been crossed
                final Transformer previous = transformers[last];
                final Transformer next     = filterType.selectTransformer(previous, rawG, forward);
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
                final Transformer next     = filterType.selectTransformer(previous, rawG, forward);
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
    private static class LocalHandler<T extends EventDetector> implements EventHandler {

        /** {@inheritDoc} */
        public Action eventOccurred(final SpacecraftState s, final EventDetector detector, final boolean increasing) {
            @SuppressWarnings("unchecked")
            final EventSlopeFilter<T> esf = (EventSlopeFilter<T>) detector;
            return esf.rawDetector.getHandler().eventOccurred(s, esf.rawDetector, esf.filterType.getTriggeredIncreasing());
        }

        /** {@inheritDoc} */
        @Override
        public SpacecraftState resetState(final EventDetector detector, final SpacecraftState oldState) {
            @SuppressWarnings("unchecked")
            final EventSlopeFilter<T> esf = (EventSlopeFilter<T>) detector;
            return esf.rawDetector.getHandler().resetState(esf.rawDetector, oldState);
        }

    }

}
