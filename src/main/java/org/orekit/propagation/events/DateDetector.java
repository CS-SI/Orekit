/* Copyright 2002-2023 CS GROUP
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
import java.util.Collections;
import java.util.List;

import org.hipparchus.ode.events.Action;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.StopOnEvent;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeStamped;

/** Finder for date events.
 * <p>This class finds date events (i.e. occurrence of some predefined dates).</p>
 * <p>As of version 5.1, it is an enhanced date detector:</p>
 * <ul>
 *   <li>it can be defined without prior date ({@link #DateDetector(TimeStamped...)})</li>
 *   <li>several dates can be added ({@link #addEventDate(AbsoluteDate)})</li>
 * </ul>
 * <p>The gap between the added dates must be more than the minGap.</p>
 * <p>The default implementation behavior is to {@link Action#STOP stop}
 * propagation at the first event date occurrence. This can be changed by calling
 * {@link #withHandler(EventHandler)} after construction.</p>
 * @see org.orekit.propagation.Propagator#addEventDetector(EventDetector)
 * @author Luc Maisonobe
 * @author Pascal Parraud
 */
public class DateDetector extends AbstractDetector<DateDetector> implements TimeStamped {

    /** Default value for max check.
     * @since 12.0
     */
    public static final double DEFAULT_MAX_CHECK = 1.0e10;

    /** Default value for minimum gap between added dates.
     * @since 12.0
     */
    public static final double DEFAULT_MIN_GAP = 1.0;

    /** Default value for convergence threshold.
     * @since 12.0
     */
    public static final double DEFAULT_THRESHOLD = 1.0e-13;

    /** Minimum gap between added dates.
     * @since 12.0
     */
    private final double minGap;

    /** Last date for g computation. */
    private AbsoluteDate gDate;

    /** List of event dates. */
    private final ArrayList<EventDate> eventDateList;

    /** Current event date. */
    private int currentIndex;

    /** Build a new instance.
     * <p>First event dates are set here, but others can be
     * added later with {@link #addEventDate(AbsoluteDate)}.</p>
     * @param dates list of event dates
     * @see #addEventDate(AbsoluteDate)
     * @since 12.0
     */
    public DateDetector(final TimeStamped... dates) {
        this(s-> DEFAULT_MAX_CHECK, DEFAULT_THRESHOLD, DEFAULT_MAX_ITER, new StopOnEvent(), DEFAULT_MIN_GAP, dates);
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
     * @param minGap minimum gap between added dates (s)
     * @param dates list of event dates
     * @since 12.0
     */
    protected DateDetector(final AdaptableInterval maxCheck, final double threshold, final int maxIter,
                           final EventHandler handler, final double minGap, final TimeStamped... dates) {
        super(maxCheck, threshold, maxIter, handler);
        this.currentIndex  = -1;
        this.gDate         = null;
        this.eventDateList = new ArrayList<DateDetector.EventDate>(dates.length);
        for (final TimeStamped ts : dates) {
            addEventDate(ts.getDate());
        }
        this.minGap        = minGap;
    }

    /**
     * Setup minimum gap between added dates.
     * @param newMinGap new minimum gap between added dates
     * @return a new detector with updated configuration (the instance is not changed)
     * @since 12.0
     */
    public DateDetector withMinGap(final double newMinGap) {
        return new DateDetector(getMaxCheckInterval(), getThreshold(), getMaxIterationCount(),
                                getHandler(), newMinGap,
                                eventDateList.toArray(new EventDate[eventDateList.size()]));
    }

    /** {@inheritDoc} */
    @Override
    protected DateDetector create(final AdaptableInterval newMaxCheck, final double newThreshold,
                                  final int newMaxIter, final EventHandler newHandler) {
        return new DateDetector(newMaxCheck, newThreshold, newMaxIter, newHandler, minGap,
                                eventDateList.toArray(new EventDate[eventDateList.size()]));
    }

    /** Get all event dates currently managed, in chronological order.
     * @return all event dates currently managed, in chronological order
     * @since 11.1
     */
    public List<TimeStamped> getDates() {
        return Collections.unmodifiableList(eventDateList);
    }

    /** Compute the value of the switching function.
     * This function measures the difference between the current and the target date.
     * @param s the current state information: date, kinematics, attitude
     * @return value of the switching function
     */
    public double g(final SpacecraftState s) {
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
     * @see #DateDetector(TimeStamped...)
     */
    public void addEventDate(final AbsoluteDate target) throws IllegalArgumentException {
        final boolean increasing;
        if (currentIndex < 0) {
            increasing = (gDate == null) ? true : target.durationFrom(gDate) > 0.0;
            currentIndex = 0;
            eventDateList.add(new EventDate(target, increasing));
        } else {
            final int          lastIndex = eventDateList.size() - 1;
            final AbsoluteDate firstDate = eventDateList.get(0).getDate();
            final AbsoluteDate lastDate  = eventDateList.get(lastIndex).getDate();
            if (firstDate.durationFrom(target) > minGap) {
                increasing = !eventDateList.get(0).isgIncrease();
                eventDateList.add(0, new EventDate(target, increasing));
                currentIndex++;
            } else if (target.durationFrom(lastDate) > minGap) {
                increasing = !eventDateList.get(lastIndex).isgIncrease();
                eventDateList.add(new EventDate(target, increasing));
            } else {
                throw new OrekitIllegalArgumentException(OrekitMessages.EVENT_DATE_TOO_CLOSE,
                                                         target,
                                                         firstDate,
                                                         lastDate,
                                                         minGap,
                                                         firstDate.durationFrom(target),
                                                         target.durationFrom(lastDate));
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
    private static class EventDate implements TimeStamped {

        /** Event date. */
        private final AbsoluteDate eventDate;

        /** Flag for g function way around event date. */
        private final boolean gIncrease;

        /** Simple constructor.
         * @param date date
         * @param increase if true, g function increases around event date
         */
        EventDate(final AbsoluteDate date, final boolean increase) {
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
