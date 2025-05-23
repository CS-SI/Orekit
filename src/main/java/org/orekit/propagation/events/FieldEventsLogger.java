/* Copyright 2002-2025 CS GROUP
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


import java.util.ArrayList;
import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.ode.events.Action;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.time.FieldAbsoluteDate;

/** This class logs events detectors events during propagation.
 *
 * <p>As {@link FieldEventDetector events detectors} are triggered during
 * orbit propagation, an event specific {@link
 * FieldEventHandler#eventOccurred(FieldSpacecraftState, FieldEventDetector, boolean) eventOccurred}
 * method is called. This class can be used to add a global logging
 * feature registering all events with their corresponding states in
 * a chronological sequence (or reverse-chronological if propagation
 * occurs backward).
 * <p>This class works by wrapping user-provided {@link FieldEventDetector
 * events detectors} before they are registered to the propagator. The
 * wrapper monitor the calls to {@link
 * FieldEventHandler#eventOccurred(FieldSpacecraftState, FieldEventDetector, boolean) eventOccurred}
 * and store the corresponding events as {@link FieldLoggedEvent} instances.
 * After propagation is complete, the user can retrieve all the events
 * that have occurred at once by calling method {@link #getLoggedEvents()}.</p>
 *
 * @author Luc Maisonobe
 * @param <T> type of the field elements
 */
public class FieldEventsLogger<T extends CalculusFieldElement<T>> {

    /** List of occurred events. */
    private final List<FieldLoggedEvent<T>> log;

    /** Simple constructor.
     * <p>
     * Build an empty logger for events detectors.
     * </p>
     */
    public FieldEventsLogger() {
        log = new ArrayList<>();
    }

    /** Monitor an event detector.
     * <p>
     * In order to monitor an event detector, it must be wrapped thanks to
     * this method as follows:
     * </p>
     * <pre>
     * Propagator propagator = new XyzPropagator(...);
     * EventsLogger logger = new EventsLogger();
     * FieldEventDetector&lt;T&gt; detector = new UvwDetector(...);
     * propagator.addEventDetector(logger.monitorDetector(detector));
     * </pre>
     * <p>
     * Note that the event detector returned by the {@link
     * FieldLoggedEvent#getEventDetector() getEventDetector} method in
     * {@link FieldLoggedEvent FieldLoggedEvent} instances returned by {@link
     * #getLoggedEvents()} are the {@code monitoredDetector} instances
     * themselves, not the wrapping detector returned by this method.
     * </p>
     * @param monitoredDetector event detector to monitor
     * @return the wrapping detector to add to the propagator
     */
    public FieldEventDetector<T> monitorDetector(final FieldEventDetector<T> monitoredDetector) {
        return new FieldLoggingWrapper(monitoredDetector);
    }

    /** Clear the logged events.
     */
    public void clearLoggedEvents() {
        log.clear();
    }

    /** Get an immutable copy of the logged events.
     * <p>
     * The copy is independent of the logger. It is preserved
     * event if the {@link #clearLoggedEvents() clearLoggedEvents} method
     * is called and the logger reused in another propagation.
     * </p>
     * @return an immutable copy of the logged events
     */
    public List<FieldLoggedEvent<T>> getLoggedEvents() {
        return new ArrayList<>(log);
    }

    /** Class for logged events entries.
     * @param <T> type of the field elements
     */
    public static class FieldLoggedEvent <T extends CalculusFieldElement<T>> {

        /** Event detector triggered. */
        private final FieldEventDetector<T> detector;

        /** Triggering state. */
        private final FieldSpacecraftState<T> state;

        /** Increasing/decreasing status. */
        private final boolean increasing;

        /** Reset state if any, otherwise event state. */
        private final FieldSpacecraftState<T> resetState;

        /** Constructor.
         * @param detectorN detector for event that was triggered
         * @param stateN state at event trigger date
         * @param resetStateN reset state if any, otherwise same as event state
         * @param increasingN indicator if the event switching function was increasing
         * or decreasing at event occurrence date
         * @since 13.1
         */
        private FieldLoggedEvent(final FieldEventDetector<T> detectorN, final FieldSpacecraftState<T> stateN,
                                 final FieldSpacecraftState<T> resetStateN, final boolean increasingN) {
            detector = detectorN;
            state      = stateN;
            resetState = resetStateN;
            increasing = increasingN;
        }

        /** Get the event detector triggered.
         * @return event detector triggered
         */
        public FieldEventDetector<T> getEventDetector() {
            return detector;
        }

        /** Get the triggering state.
         * @return triggering state
         * @see FieldEventHandler#eventOccurred(FieldSpacecraftState, FieldEventDetector, boolean)
         */
        public FieldSpacecraftState<T> getState() {
            return state;
        }

        /**
         * Get the reset state.
         * @return reset state
         * @since 13.1
         */
        public FieldSpacecraftState<T> getResetState() {
            return resetState;
        }

        /** Get the Increasing/decreasing status of the event.
         * @return increasing/decreasing status of the event
         * @see FieldEventHandler#eventOccurred(FieldSpacecraftState, FieldEventDetector, boolean)
         */
        public boolean isIncreasing() {
            return increasing;
        }

    }

    /** Internal wrapper for events detectors. */
    private class FieldLoggingWrapper implements FieldDetectorModifier<T> {

        /** Wrapped detector. */
        private final FieldEventDetector<T> wrappedDetector;

        /** Simple constructor.
         * @param detector events detector to wrap
         */
        FieldLoggingWrapper(final FieldEventDetector<T> detector) {
            this.wrappedDetector = detector;
        }

        @Override
        public FieldEventDetector<T> getDetector() {
            return wrappedDetector;
        }

        /** Log an event.
         * @param state state at event trigger date
         * @param resetState reset state if any, otherwise same as event state
         * @param increasing indicator if the event switching function was increasing
         */
        void logEvent(final FieldSpacecraftState<T> state, final FieldSpacecraftState<T> resetState,
                      final boolean increasing) {
            log.add(new FieldLoggedEvent<>(getDetector(), state, resetState, increasing));
        }

        /** {@inheritDoc} */
        @Override
        public FieldEventHandler<T> getHandler() {

            final FieldEventHandler<T> handler = getDetector().getHandler();

            return new FieldEventHandler<T>() {

                private FieldSpacecraftState<T> lastTriggeringState = null;
                private FieldSpacecraftState<T> lastResetState = null;

                @Override
                public void init(final FieldSpacecraftState<T> initialState, final FieldAbsoluteDate<T> target,
                                 final FieldEventDetector<T> detector) {
                    FieldEventHandler.super.init(initialState, target, detector);
                    lastResetState = null;
                    lastTriggeringState = null;
                }

                /** {@inheritDoc} */
                @Override
                public Action eventOccurred(final FieldSpacecraftState<T> s,
                                            final FieldEventDetector<T> d,
                                            final boolean increasing) {
                    final Action action = handler.eventOccurred(s, getDetector(), increasing);
                    if (action == Action.RESET_STATE) {
                        lastResetState = resetState(getDetector(), s);
                    } else {
                        lastResetState = s;
                    }
                    lastTriggeringState = s;
                    logEvent(s, lastResetState, increasing);
                    return action;
                }

                /** {@inheritDoc} */
                @Override
                public FieldSpacecraftState<T> resetState(final FieldEventDetector<T> d,
                                                          final FieldSpacecraftState<T> oldState) {
                    if (lastTriggeringState != oldState) {
                        lastTriggeringState = oldState;
                        lastResetState = handler.resetState(getDetector(), oldState);
                    }
                    return lastResetState;
                }

            };
        }

    }

}
