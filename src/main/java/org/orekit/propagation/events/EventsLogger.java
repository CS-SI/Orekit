/* Copyright 2002-2016 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;

/** This class logs events detectors events during propagation.
 *
 * <p>As {@link EventDetector events detectors} are triggered during
 * orbit propagation, an event specific {@link
 * EventDetector#eventOccurred(SpacecraftState, boolean) eventOccurred}
 * method is called. This class can be used to add a global logging
 * feature registering all events with their corresponding states in
 * a chronological sequence (or reverse-chronological if propagation
 * occurs backward).
 * <p>This class works by wrapping user-provided {@link EventDetector
 * events detectors} before they are registered to the propagator. The
 * wrapper monitor the calls to {@link
 * EventDetector#eventOccurred(SpacecraftState, boolean) eventOccurred}
 * and store the corresponding events as {@link LoggedEvent} instances.
 * After propagation is complete, the user can retrieve all the events
 * that have occurred at once by calling method {@link #getLoggedEvents()}.</p>
 *
 * @author Luc Maisonobe
 */
public class EventsLogger implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = -8643810076248572648L;

    /** List of occurred events. */
    private final List<LoggedEvent> log;

    /** Simple constructor.
     * <p>
     * Build an empty logger for events detectors.
     * </p>
     */
    public EventsLogger() {
        log = new ArrayList<EventsLogger.LoggedEvent>();
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
        return new LoggingWrapper<T>(monitoredDetector);
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
        return new ArrayList<EventsLogger.LoggedEvent>(log);
    }

    /** Class for logged events entries. */
    public static class LoggedEvent implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 20131202L;

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

        /** Get the triggering state.
         * @return triggering state
         * @see EventDetector#eventOccurred(SpacecraftState, boolean)
         */
        public SpacecraftState getState() {
            return state;
        }

        /** Get the Increasing/decreasing status of the event.
         * @return increasing/decreasing status of the event
         * @see EventDetector#eventOccurred(SpacecraftState, boolean)
         */
        public boolean isIncreasing() {
            return increasing;
        }

    }

    /** Internal wrapper for events detectors.
     * @param <T> class type for the generic version
     */
    private class LoggingWrapper<T extends EventDetector> extends AbstractDetector<LoggingWrapper<T>> {

        /** Serializable UID. */
        private static final long serialVersionUID = 20131118L;

        /** Wrapped events detector. */
        private final T detector;

        /** Simple constructor.
         * @param detector events detector to wrap
         */
        LoggingWrapper(final T detector) {
            this(detector.getMaxCheckInterval(), detector.getThreshold(),
                 detector.getMaxIterationCount(), new LocalHandler<T>(),
                 detector);
        }

        /** Private constructor with full parameters.
         * <p>
         * This constructor is private as users are expected to use the builder
         * API with the various {@code withXxx()} methods to set up the instance
         * in a readable manner without using a huge amount of parameters.
         * </p>
         * @param maxCheck maximum checking interval (s)
         * @param threshold convergence threshold (s)
         * @param maxIter maximum number of iterations in the event time search
         * @param handler event handler to call at event occurrences
         * @param detector events detector to wrap
         * @since 6.1
         */
        private LoggingWrapper(final double maxCheck, final double threshold,
                               final int maxIter, final EventHandler<? super LoggingWrapper<T>> handler,
                               final T detector) {
            super(maxCheck, threshold, maxIter, handler);
            this.detector = detector;
        }

        /** {@inheritDoc} */
        @Override
        protected LoggingWrapper<T> create(final double newMaxCheck, final double newThreshold,
                                           final int newMaxIter, final EventHandler<? super LoggingWrapper<T>> newHandler) {
            return new LoggingWrapper<T>(newMaxCheck, newThreshold, newMaxIter, newHandler, detector);
        }

        /** Log an event.
         * @param state state at event trigger date
         * @param increasing indicator if the event switching function was increasing
         */
        public void logEvent(final SpacecraftState state, final boolean increasing) {
            log.add(new LoggedEvent(detector, state, increasing));
        }

        /** {@inheritDoc} */
        public void init(final SpacecraftState s0, final AbsoluteDate t) {
            detector.init(s0, t);
        }

        /** {@inheritDoc} */
        public double g(final SpacecraftState s) throws OrekitException {
            return detector.g(s);
        }

    }

    /** Local class for handling events.
     * @param <T> class type for the generic version
     */
    private static class LocalHandler<T extends EventDetector> implements EventHandler<LoggingWrapper<T>> {

        /** {@inheritDoc} */
        public Action eventOccurred(final SpacecraftState s, final LoggingWrapper<T> wrapper, final boolean increasing)
            throws OrekitException {
            wrapper.logEvent(s, increasing);
            return wrapper.detector.eventOccurred(s, increasing);
        }

        /** {@inheritDoc} */
        @Override
        public SpacecraftState resetState(final LoggingWrapper<T> wrapper, final SpacecraftState oldState)
            throws OrekitException {
            return wrapper.detector.resetState(oldState);
        }

    }

}
