/* Contributed in the public domain.
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
package org.orekit.propagation.events.handlers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.ode.events.Action;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;

/**
 * Handler that will record every time an event occurs and always return {@link
 * Action#CONTINUE}.
 *
 * <p> As this handler stores all observed events it may consume large amounts
 * of memory depending on the duration of propagation and the frequency of events.
 * </p>
 *
 * @author Evan Ward
 * @see RecordAndContinue
 * @since 9.3
 * @param <T> type of the field element
 */
public class FieldRecordAndContinue <T extends CalculusFieldElement<T>> implements FieldEventHandler<T> {

    /** A single event detected during propagation.
     * @param <T> type of the field element
     */
    public static class Event<T extends CalculusFieldElement<T>> {

        /** The observed state. */
        private final FieldSpacecraftState<T> state;
        /** The detector. */
        private final FieldEventDetector<T> detector;
        /** The sign of the derivative of the g function. */
        private final boolean increasing;

        /**
         * Create a new event.
         *
         * @param detector   of the event.
         * @param state      of the event.
         * @param increasing if the g function is increasing.
         */
        private Event(final FieldEventDetector<T> detector,
                      final FieldSpacecraftState<T> state,
                      final boolean increasing) {
            this.detector = detector;
            this.state = state;
            this.increasing = increasing;
        }

        /**
         * Get the detector.
         *
         * @return the detector that found the event.
         * @see EventHandler#eventOccurred(SpacecraftState, EventDetector, boolean)
         */
        public FieldEventDetector<T> getDetector() {
            return detector;
        }

        /**
         * Is the g() function increasing?
         *
         * @return if the sign of the derivative of the g function is positive (true) or
         * negative (false).
         * @see EventHandler#eventOccurred(SpacecraftState, EventDetector, boolean)
         */
        public boolean isIncreasing() {
            return increasing;
        }

        /**
         * Get the spacecraft's state at the event.
         *
         * @return the satellite's state when the event was triggered.
         * @see EventHandler#eventOccurred(SpacecraftState, EventDetector, boolean)
         */
        public FieldSpacecraftState<T> getState() {
            return state;
        }

        @Override
        public String toString() {
            return "Event{" +
                    "state=" + state +
                    ", increasing=" + increasing +
                    ", detector=" + detector +
                    '}';
        }
    }

    /** Observed events. */
    private final List<Event<T>> events;

    /** Create a new handler using an {@link ArrayList} to store events. */
    public FieldRecordAndContinue() {
        this(new ArrayList<>());
    }

    /**
     * Create a handler using the given collection to store events.
     *
     * @param events collection.
     */
    public FieldRecordAndContinue(final List<Event<T>> events) {
        this.events = events;
    }

    /**
     * Get the events passed to this handler.
     *
     * <p> Note the returned list of events is in the order the events were
     * passed to this handler by calling {@link #eventOccurred(FieldSpacecraftState,
     * FieldEventDetector, boolean)}. This may or may not be chronological order.
     *
     * <p> Also not that this method returns a view of the internal collection
     * used to store events and calling any of this handler's methods may modify both the
     * underlying collection and the returned view. If a snapshot of the events up to a
     * certain point is needed create a copy of the returned collection.
     *
     * @return the events observed by the handler in the order they were observed.
     */
    public List<Event<T>> getEvents() {
        return Collections.unmodifiableList(this.events);
    }

    /** Clear all stored events. */
    public void clear() {
        this.events.clear();
    }

    @Override
    public Action eventOccurred(final FieldSpacecraftState<T> s,
                                final FieldEventDetector<T> detector,
                                final boolean increasing) {
        events.add(new Event<>(detector, s, increasing));
        return Action.CONTINUE;
    }

}
