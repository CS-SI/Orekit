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


import java.util.ArrayList;
import java.util.List;

import org.hipparchus.RealFieldElement;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.time.FieldAbsoluteDate;

/** This class logs events detectors events during propagation.
 *
 * <p>As {@link FieldEventDetector events detectors} are triggered during
 * orbit propagation, an event specific {@link
 * FieldEventDetector#eventOccurred(FieldSpacecraftState, boolean) eventOccurred}
 * method is called. This class can be used to add a global logging
 * feature registering all events with their corresponding states in
 * a chronological sequence (or reverse-chronological if propagation
 * occurs backward).<p>
 * <p>This class works by wrapping user-provided {@link FieldEventDetector
 * events detectors} before they are registered to the propagator. The
 * wrapper monitor the calls to {@link
 * FieldEventDetector#eventOccurred(FieldSpacecraftState, boolean) eventOccurred}
 * and store the corresponding events as {@link FieldLoggedEvent} instances.
 * After propagation is complete, the user can retrieve all the events
 * that have occurred at once by calling method {@link #getLoggedEvents()}.</p>
 *
 * @author Luc Maisonobe
 */
public class FieldEventsLogger<T extends RealFieldElement<T>> {



    /** List of occurred events. */
    private final List<FieldLoggedEvent<T>> log;

    /** Simple constructor.
     * <p>
     * Build an empty logger for events detectors.
     * </p>
     */
    public FieldEventsLogger() {
        log = new ArrayList<FieldEventsLogger.FieldLoggedEvent<T>>();
    }

    /** Monitor an event detector.
     * <p>
     * In order to monitor an event detector, it must be wrapped thanks to
     * this method as follows:
     * </p>
     * <pre>
     * Propagator propagator = new XyzPropagator(...);
     * EventsLogger logger = new EventsLogger();
     * FieldEventDetector<T> detector = new UvwDetector(...);
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
     * @param <D> class type for the generic version
     */
    public <D extends FieldEventDetector<T>> FieldEventDetector<T> monitorDetector(final D monitoredDetector) {
        return new FieldLoggingWrapper<D>(monitoredDetector);
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
        return new ArrayList<FieldEventsLogger.FieldLoggedEvent<T>>(log);
    }

    /** Class for logged events entries. */
    public static class FieldLoggedEvent <T extends RealFieldElement<T>> {

        /** Event detector triggered. */
        private final FieldEventDetector<T> detector;

        /** Triggering state. */
        private final FieldSpacecraftState<T> state;

        /** Increasing/decreasing status. */
        private final boolean increasing;

        /** Simple constructor.
         * @param detectorN detector for event that was triggered
         * @param stateN state at event trigger date
         * @param increasingN indicator if the event switching function was increasing
         * or decreasing at event occurrence date
         */
        private FieldLoggedEvent(final FieldEventDetector<T> detectorN, final FieldSpacecraftState<T> stateN, final boolean increasingN) {
            detector   = detectorN;
            state      = stateN;
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
         * @see FieldEventDetector#eventOccurred(FieldSpacecraftState, boolean)
         */
        public FieldSpacecraftState<T> getState() {
            return state;
        }

        /** Get the Increasing/decreasing status of the event.
         * @return increasing/decreasing status of the event
         * @see FieldEventDetector#eventOccurred(FieldSpacecraftState, boolean)
         */
        public boolean isIncreasing() {
            return increasing;
        }

    }

    /** Internal wrapper for events detectors.
     * @param <D> class type for the generic version
     */
    private class FieldLoggingWrapper<D extends FieldEventDetector<T>> extends FieldAbstractDetector<FieldLoggingWrapper<D>, T> {

        /** Wrapped events detector. */
        private final D detector;

        /** Simple constructor.
         * @param detector events detector to wrap
         */
        FieldLoggingWrapper(final D detector) {
            this(detector.getMaxCheckInterval(), detector.getThreshold(),
                 detector.getMaxIterationCount(), new FieldLocalHandler<D>(),
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
        private FieldLoggingWrapper(final T maxCheck, final T threshold,
                               final int maxIter, final FieldEventHandler<? super FieldLoggingWrapper<D>, T> handler,
                               final D detector) {
            super(maxCheck, threshold, maxIter, handler);
            this.detector = detector;
        }

        /** {@inheritDoc} */
        @Override
        protected FieldLoggingWrapper<D> create(final T newMaxCheck, final T newThreshold,
                                           final int newMaxIter, final FieldEventHandler<? super FieldLoggingWrapper<D>, T> newHandler) {
            return new FieldLoggingWrapper<D>(newMaxCheck, newThreshold, newMaxIter, newHandler, detector);
        }

        /** Log an event.
         * @param state state at event trigger date
         * @param increasing indicator if the event switching function was increasing
         */
        public void logEvent(final FieldSpacecraftState<T> state, final boolean increasing) {
            log.add(new FieldLoggedEvent<T>(detector, state, increasing));
        }

        /** {@inheritDoc} */
        public void init(final FieldSpacecraftState<T> s0, final FieldAbsoluteDate<T> t) {
            detector.init(s0, t);
        }

        /** {@inheritDoc} */
        public T g(final FieldSpacecraftState<T> s) throws OrekitException {
            return detector.g(s);
        }

    }

    /** Local class for handling events.
     * @param <D> class type for the generic version
     */
    private class FieldLocalHandler<D extends FieldEventDetector<T>> implements FieldEventHandler<FieldLoggingWrapper<D>, T> {

        /** {@inheritDoc} */
        public Action eventOccurred(final FieldSpacecraftState<T> s, final FieldLoggingWrapper<D> wrapper, final boolean increasing)
            throws OrekitException {
            wrapper.logEvent(s, increasing);
            return wrapper.detector.eventOccurred(s, increasing);
        }

        /** {@inheritDoc} */
        @Override
        public FieldSpacecraftState<T> resetState(final FieldLoggingWrapper<D> wrapper, final FieldSpacecraftState<T> oldState)
            throws OrekitException {
            return wrapper.detector.resetState(oldState);
        }

    }

}
