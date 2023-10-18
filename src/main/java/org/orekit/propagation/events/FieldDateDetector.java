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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.ode.events.Action;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.propagation.events.handlers.FieldStopOnEvent;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.FieldTimeStamped;

/** Finder for date events.
 * <p>This class finds date events (i.e. occurrence of some predefined dates).</p>
 * <p>As of version 5.1, it is an enhanced date detector:</p>
 * <ul>
 *   <li>it can be defined without prior date ({@link #FieldDateDetector(Field, FieldTimeStamped...)})</li>
 *   <li>several dates can be added ({@link #addEventDate(FieldAbsoluteDate)})</li>
 * </ul>
 * <p>The gap between the added dates must be more than the minGap.</p>
 * <p>The default implementation behavior is to {@link Action#STOP stop}
 * propagation at the first event date occurrence. This can be changed by calling
 * {@link #withHandler(FieldEventHandler)} after construction.</p>
 * @see org.orekit.propagation.FieldPropagator#addEventDetector(FieldEventDetector)
 * @author Luc Maisonobe
 * @author Pascal Parraud
 * @param <T> type of the field elements
 */
public class FieldDateDetector<T extends CalculusFieldElement<T>> extends FieldAbstractDetector<FieldDateDetector<T>, T>
    implements FieldTimeStamped<T> {

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
    private FieldAbsoluteDate<T> gDate;

    /** List of event dates. */
    private final ArrayList<FieldEventDate<T>> eventDateList;

    /** Current event date. */
    private int currentIndex;

    /** Build a new instance.
     * <p>First event dates are set here, but others can be
     * added later with {@link #addEventDate(FieldAbsoluteDate)}.</p>
     * @param field field to which dates belong
     * @param dates list of event dates
     * @see #addEventDate(FieldAbsoluteDate)
     * @since 12.0
     */
    @SafeVarargs
    public FieldDateDetector(final Field<T> field, final FieldTimeStamped<T>... dates) {
        this(s-> DEFAULT_MAX_CHECK, field.getZero().newInstance(DEFAULT_THRESHOLD),
             DEFAULT_MAX_ITER, new FieldStopOnEvent<>(), DEFAULT_MIN_GAP, dates);
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
     */
    @SafeVarargs
    protected FieldDateDetector(final FieldAdaptableInterval<T> maxCheck, final T threshold,
                                final int maxIter, final FieldEventHandler<T> handler,
                                final double minGap, final FieldTimeStamped<T>... dates) {
        super(maxCheck, threshold, maxIter, handler);
        this.currentIndex  = -1;
        this.gDate         = null;
        this.eventDateList = new ArrayList<FieldDateDetector.FieldEventDate<T>>(dates.length);
        for (final FieldTimeStamped<T> ts : dates) {
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
    public FieldDateDetector<T> withMinGap(final double newMinGap) {
        @SuppressWarnings("unchecked")
        final FieldTimeStamped<T>[] dates = eventDateList.toArray(new FieldEventDate[eventDateList.size()]);
        return new FieldDateDetector<>(getMaxCheckInterval(), getThreshold(), getMaxIterationCount(),
                                       getHandler(), newMinGap, dates);
    }

    /** {@inheritDoc} */
    @Override
    protected FieldDateDetector<T> create(final FieldAdaptableInterval<T> newMaxCheck, final T newThreshold,
                                          final int newMaxIter, final FieldEventHandler<T> newHandler) {
        @SuppressWarnings("unchecked")
        final FieldTimeStamped<T>[] dates = eventDateList.toArray(new FieldEventDate[eventDateList.size()]);
        return new FieldDateDetector<>(newMaxCheck, newThreshold, newMaxIter, newHandler, minGap, dates);
    }

    /** Get all event field dates currently managed, in chronological order.
     * @return all event field dates currently managed, in chronological order
     * @since 12.0
     */
    public List<FieldTimeStamped<T>> getDates() {
        return Collections.unmodifiableList(eventDateList);
    }

    /** Compute the value of the switching function.
     * This function measures the difference between the current and the target date.
     * @param s the current state information: date, kinematics, attitude
     * @return value of the switching function
     */
    public T g(final FieldSpacecraftState<T> s) {
        gDate = s.getDate();
        if (currentIndex < 0) {
            return s.getA().getField().getZero().add(-1);
        } else {
            final FieldEventDate<T> event = getClosest(gDate);
            return event.isgIncrease() ? gDate.durationFrom(event.getDate()) : event.getDate().durationFrom(gDate);
        }
    }

    /** Get the current event date according to the propagator.
     * @return event date
     */
    public FieldAbsoluteDate<T> getDate() {
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
     * @see #FieldDateDetector(Field, FieldTimeStamped...)
     */
    public void addEventDate(final FieldAbsoluteDate<T> target) throws IllegalArgumentException {
        final boolean increasing;
        if (currentIndex < 0) {
            increasing = (gDate == null) ? true : target.durationFrom(gDate).getReal() > 0.0;
            currentIndex = 0;
            eventDateList.add(new FieldEventDate<>(target, increasing));
        } else {
            final                      int lastIndex = eventDateList.size() - 1;
            final FieldAbsoluteDate<T> firstDate     = eventDateList.get(0).getDate();
            final FieldAbsoluteDate<T> lastDate      = eventDateList.get(lastIndex).getDate();
            if (firstDate.durationFrom(target).getReal() > minGap) {
                increasing = !eventDateList.get(0).isgIncrease();
                eventDateList.add(0, new FieldEventDate<>(target, increasing));
                currentIndex++;
            } else if (target.durationFrom(lastDate).getReal() > minGap) {
                increasing = !eventDateList.get(lastIndex).isgIncrease();
                eventDateList.add(new FieldEventDate<>(target, increasing));
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
    private FieldEventDate<T> getClosest(final FieldAbsoluteDate<T> target) {
        final T dt = target.durationFrom(eventDateList.get(currentIndex).getDate());
        if (dt.getReal() < 0.0 && currentIndex > 0) {
            boolean found = false;
            while (currentIndex > 0 && !found) {
                if (target.durationFrom(eventDateList.get(currentIndex - 1).getDate()).getReal() < eventDateList.get(currentIndex).getDate().durationFrom(target).getReal()) {
                    currentIndex--;
                } else {
                    found = true;
                }
            }
        } else if (dt.getReal() > 0.0 && currentIndex < eventDateList.size() - 1) {
            final int maxIndex = eventDateList.size() - 1;
            boolean found = false;
            while (currentIndex < maxIndex && !found) {
                if (target.durationFrom(eventDateList.get(currentIndex + 1).getDate()).getReal() > eventDateList.get(currentIndex).getDate().durationFrom(target).getReal()) {
                    currentIndex++;
                } else {
                    found = true;
                }
            }
        }
        return eventDateList.get(currentIndex);
    }

    /** Event date specification. */
    private static class FieldEventDate<T extends CalculusFieldElement<T>> implements FieldTimeStamped<T> {

        /** Event date. */
        private final FieldAbsoluteDate<T> eventDate;

        /** Flag for g function way around event date. */
        private final boolean gIncrease;

        /** Simple constructor.
         * @param date date
         * @param increase if true, g function increases around event date
         */
        FieldEventDate(final FieldAbsoluteDate<T> date, final boolean increase) {
            this.eventDate = date;
            this.gIncrease = increase;
        }

        /** Getter for event date.
         * @return event date
         */
        public FieldAbsoluteDate<T> getDate() {
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
