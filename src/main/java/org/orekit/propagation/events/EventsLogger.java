/* Copyright 2002-2024 CS GROUP
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

import org.hipparchus.ode.events.Action;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeStamped;

/** This class logs events detectors events during propagation.
 *
 * <p>As {@link EventDetector events detectors} are triggered during
 * orbit propagation, an event specific {@link
 * EventHandler#eventOccurred(SpacecraftState, EventDetector, boolean) eventOccurred}
 * method is called. This class can be used to add a global logging
 * feature registering all events with their corresponding states in
 * a chronological sequence (or reverse-chronological if propagation
 * occurs backward).
 * <p>This class works by wrapping user-provided {@link EventDetector
 * events detectors} before they are registered to the propagator. The
 * wrapper monitor the calls to {@link
 * EventHandler#eventOccurred(SpacecraftState, EventDetector, boolean) eventOccurred}
 * and store the corresponding events as {@link LoggedEvent} instances.
 * After propagation is complete, the user can retrieve all the events
 * that have occurred at once by calling method {@link #getLoggedEvents()}.</p>
 *
 * @author Luc Maisonobe
 */
public class EventsLogger {

    /** List of occurred events. */
    private final List<LoggedEvent> log;

    /** Simple constructor.
     * <p>
     * Build an empty logger for events detectors.
     * </p>
     */
    public EventsLogger() {
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
     * EventDetector detector = new UvwDetector(...);
     * propagator.addEventDetector(logger.monitorDetector(detector));
     * </pre>
     * <p>
     * Note that the event detector returned by the {@link
     * LoggedEvent#getEventDetector() getEventDetector} method in
     * {@link LoggedEvent LoggedEvent} instances returned by {@link
     * #getLoggedEvents()} are the {@code monitoredDetector} instances
     * themselves, not the wrapping detector returned by this method.
     * </p>
     * @param monitoredDetector event detector to monitor
     * @return the wrapping detector to add to the propagator
     * @param <T> class type for the generic version
     */
    public <T extends EventDetector> EventDetector monitorDetector(final T monitoredDetector) {
        return new LoggingWrapper(monitoredDetector);
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
    public List<LoggedEvent> getLoggedEvents() {
        return new ArrayList<>(log);
    }

    /** Class for logged events entries. */
    public static class LoggedEvent implements TimeStamped {

        /** Event detector triggered. */
        private final EventDetector detector;

        /** Triggering state. */
        private final SpacecraftState state;

        /** Increasing/decreasing status. */
        private final boolean increasing;

        /** Simple constructor.
         * @param detector detector for event that was triggered
         * @param state state at event trigger date
         * @param increasing indicator if the event switching function was increasing
         * or decreasing at event occurrence date
         */
        private LoggedEvent(final EventDetector detector, final SpacecraftState state, final boolean increasing) {
            this.detector   = detector;
            this.state      = state;
            this.increasing = increasing;
        }

        /** Get the event detector triggered.
         * @return event detector triggered
         */
        public EventDetector getEventDetector() {
            return detector;
        }

        /** {@inheritDoc} */
        @Override
        public AbsoluteDate getDate() {
            return state.getDate();
        }

        /** Get the triggering state.
         * @return triggering state
         * @see EventHandler#eventOccurred(SpacecraftState, EventDetector, boolean)
         */
        public SpacecraftState getState() {
            return state;
        }

        /** Get the Increasing/decreasing status of the event.
         * @return increasing/decreasing status of the event
         * @see EventHandler#eventOccurred(SpacecraftState, EventDetector, boolean)
         */
        public boolean isIncreasing() {
            return increasing;
        }

    }

    /** Internal wrapper for events detectors. */
    private class LoggingWrapper implements DetectorModifier {

        /** Wrapped event detector. */
        private final EventDetector detector;

        /** Simple constructor.
         * @param detector events detector to wrap
         */
        LoggingWrapper(final EventDetector detector) {
            this.detector = detector;
        }

        /** Log an event.
         * @param state state at event trigger date
         * @param increasing indicator if the event switching function was increasing
         */
        public void logEvent(final SpacecraftState state, final boolean increasing) {
            log.add(new LoggedEvent(getDetector(), state, increasing));
        }

        /** {@inheritDoc} */
        @Override
        public EventDetector getDetector() {
            return detector;
        }

        /** {@inheritDoc} */
        @Override
        public EventHandler getHandler() {
            final EventHandler handler = getDetector().getHandler();

            return new EventHandler() {

                /** {@inheritDoc} */
                public Action eventOccurred(final SpacecraftState s, final EventDetector d, final boolean increasing) {
                    logEvent(s, increasing);
                    return handler.eventOccurred(s, getDetector(), increasing);
                }

                /** {@inheritDoc} */
                @Override
                public SpacecraftState resetState(final EventDetector d, final SpacecraftState oldState) {
                    return handler.resetState(getDetector(), oldState);
                }

            };
        }

    }

}
