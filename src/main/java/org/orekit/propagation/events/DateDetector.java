/* Copyright 2002-2011 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeStamped;

/** Finder for date events.
 * <p>This class finds date events (i.e. occurrence of some predefined dates).</p>
 * <p>As of version 5.1, it is an enhanced date detector:</p>
 * <ul>
 *   <li>it can be defined without prior date ({@link #DateDetector(double,double)})</li>
 *   <li>several dates can be added ({@link #addEventDate(AbsoluteDate)})</li>
 * </ul>
 * <p>The gap between the added dates must be more than the maxCheck.</p>
 * <p>The default implementation behavior is to {@link EventDetector#STOP stop}
 * propagation at the first event date occurrence. This can be changed by
 * overriding the {@link #eventOccurred(SpacecraftState, boolean) eventOccurred}
 * method in a derived class.</p>
 * @see org.orekit.propagation.Propagator#addEventDetector(EventDetector)
 * @author Luc Maisonobe
 * @author Pascal Parraud
 */
public class DateDetector extends AbstractDetector implements TimeStamped {

    /** Serializable UID. */
    private static final long serialVersionUID = -334171965326514174L;

    /** Last date for g computation. */
    private AbsoluteDate gDate;

    /** List of event dates. */
    private final ArrayList<EventDate> eventDateList;

    /** Current event date. */
    private int currentIndex;

    /** Build a new instance.
     * <p>This constructor is dedicated to date detection
     * when the event date is not known before propagating.
     * It can be triggered later by adding some event date,
     * it then acts like a timer.</p>
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     * @see #addEventDate(AbsoluteDate)
     */
    public DateDetector(final double maxCheck, final double threshold) {
        super(maxCheck, threshold);
        this.eventDateList = new ArrayList<EventDate>();
        this.currentIndex = -1;
        this.gDate = null;
    }

    /** Build a new instance.
     * <p>First event date is set here, but others can be
     * added later with {@link #addEventDate(AbsoluteDate)}.</p>
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     * @param target target date
     * @see #addEventDate(AbsoluteDate)
     */
    public DateDetector(final double maxCheck, final double threshold, final AbsoluteDate target) {
        this(maxCheck, threshold);
        this.addEventDate(target);
    }

    /** Build a new instance.
     * <p>This constructor is dedicated to single date detection.
     * MaxCheck is set to 10.e9, so almost no other date can be
     * added. Tolerance is set to 10.e-10.</p>
     * @param target target date
     * @see #addEventDate(AbsoluteDate)
     */
    public DateDetector(final AbsoluteDate target) {
        this(10.e9, 10.e-10, target);
    }

    /** {@inheritDoc} */
    public void init(SpacecraftState s0, AbsoluteDate t) {
    }

    /** Handle a date event and choose what to do next.
     * <p>The default implementation behavior is to {@link
     * EventDetector#STOP stop} propagation at date occurrence.</p>
     * @param s the current state information : date, kinematics, attitude
     * @param increasing the way of the switching function is not guaranted
     * as it can change according to the added event dates.
     * @return {@link #STOP}
     * @exception OrekitException if some specific error occurs
     */
    public Action eventOccurred(final SpacecraftState s, final boolean increasing)
        throws OrekitException {
        return Action.STOP;
    }

    /** Compute the value of the switching function.
     * This function measures the difference between the current and the target date.
     * @param s the current state information: date, kinematics, attitude
     * @return value of the switching function
     * @exception OrekitException if some specific error occurs
     */
    public double g(final SpacecraftState s) throws OrekitException {
        gDate = s.getDate();
        if (currentIndex < 0) {
            return -1.0;
        } else {
            final EventDate event = getClosest(gDate);
            return event.isgIncrease() ? gDate.durationFrom(event.getDate()) : event.getDate().durationFrom(gDate);
        }
    }

    /** Get the current event date according to the propagator.
     * @return event date
     */
    public AbsoluteDate getDate() {
        return currentIndex < 0 ? null : eventDateList.get(currentIndex).getDate();
    }

    /** Add an event date.
     * <p>The date to add must be:</p>
     * <ul>
     *   <li>less than the smallest already registered event date minus the maxCheck</li>
     *   <li>or more than the largest already registered event date plus the maxCheck</li>
     * </ul>
     * @param target target date
     * @throws IllegalArgumentException if the date is too close from already defined interval
     * @see #DateDetector(double, double)
     */
    public void addEventDate(final AbsoluteDate target) throws IllegalArgumentException {
        final boolean increasing;
        if (currentIndex < 0) {
            increasing = (gDate == null) ? true : target.durationFrom(gDate) > 0.0;
            currentIndex = 0;
            eventDateList.add(new EventDate(target, increasing));
        } else {
            final int lastIndex = eventDateList.size() - 1;
            if (eventDateList.get(0).getDate().durationFrom(target) > getMaxCheckInterval()) {
                increasing = !eventDateList.get(0).isgIncrease();
                eventDateList.add(0, new EventDate(target, increasing));
                currentIndex++;
            } else if (target.durationFrom(eventDateList.get(lastIndex).getDate()) > getMaxCheckInterval()) {
                increasing = !eventDateList.get(lastIndex).isgIncrease();
                eventDateList.add(new EventDate(target, increasing));
            } else {
                throw OrekitException.createIllegalArgumentException(OrekitMessages.EVENT_DATE_TOO_CLOSE,
                                                                     target,
                                                                     eventDateList.get(0).getDate(),
                                                                     eventDateList.get(lastIndex).getDate(),
                                                                     getMaxCheckInterval());
            }
        }
    }

    /** Get the closest EventDate to the target date.
     * @param target target date
     * @return current EventDate
     */
    private EventDate getClosest(final AbsoluteDate target) {
        final double dt = target.durationFrom(eventDateList.get(currentIndex).getDate());
        if (dt < 0.0 && currentIndex > 0) {
            boolean found = false;
            while (currentIndex > 0 && !found) {
                if (target.durationFrom(eventDateList.get(currentIndex - 1).getDate()) < eventDateList.get(currentIndex).getDate().durationFrom(target)) {
                    currentIndex--;
                } else {
                    found = true;
                }
            }
        } else if (dt > 0.0 && currentIndex < eventDateList.size() - 1) {
            final int maxIndex = eventDateList.size() - 1;
            boolean found = false;
            while (currentIndex < maxIndex && !found) {
                if (target.durationFrom(eventDateList.get(currentIndex + 1).getDate()) > eventDateList.get(currentIndex).getDate().durationFrom(target)) {
                    currentIndex++;
                } else {
                    found = true;
                }
            }
        }
        return eventDateList.get(currentIndex);
    }

    /** Event date specification. */
    private static class EventDate implements Serializable, TimeStamped {

        /** Serializable UID. */
        private static final long serialVersionUID = -7641032576122527149L;

        /** Event date. */
        private final AbsoluteDate eventDate;

        /** Flag for g function way around event date. */
        private final boolean gIncrease;

        /** Simple constructor.
         * @param date date
         * @param increase if true, g function increases around event date
         */
        public EventDate(final AbsoluteDate date, final boolean increase) {
            this.eventDate = date;
            this.gIncrease = increase;
        }

        /** Getter for event date.
         * @return event date
         */
        public AbsoluteDate getDate() {
            return eventDate;
        }

        /** Getter for g function way at event date.
         * @return g function increasing flag
         */
        public boolean isgIncrease() {
            return gIncrease;
        }

    }

}
