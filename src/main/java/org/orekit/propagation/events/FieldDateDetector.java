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

import org.orekit.errors.OrekitIllegalArgumentException;
import org.hipparchus.RealFieldElement;
import org.orekit.errors.OrekitException;
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
 *   <li>it can be defined without prior date ({@link #FieldDateDetector(RealFieldElement, RealFieldElement, FieldTimeStamped...)})</li>
 *   <li>several dates can be added ({@link #addEventDate(FieldAbsoluteDate)})</li>
 * </ul>
 * <p>The gap between the added dates must be more than the maxCheck.</p>
 * <p>The default implementation behavior is to {@link
 * org.orekit.propagation.events.handlers.FieldEventHandler.Action#STOP stop}
 * propagation at the first event date occurrence. This can be changed by calling
 * {@link #withHandler(FieldEventHandler)} after construction.</p>
 * @see org.orekit.propagation.FieldPropagator#addEventDetector(FieldEventDetector)
 * @author Luc Maisonobe
 * @author Pascal Parraud
 */
public class FieldDateDetector<T extends RealFieldElement<T>> extends FieldAbstractDetector<FieldDateDetector<T>, T>
    implements FieldTimeStamped<T> {

    /** Last date for g computation. */
    private FieldAbsoluteDate<T> gDate;

    /** List of event dates. */
    private final ArrayList<FieldEventDate<T>> eventDateList;

    /** Current event date. */
    private int currentIndex;

    /** Build a new instance.
     * <p>First event dates are set here, but others can be
     * added later with {@link #addEventDate(FieldAbsoluteDate)}.</p>
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     * @param dates list of event dates
     * @see #addEventDate(FieldAbsoluteDate)
     */
    @SafeVarargs
    public FieldDateDetector(final T maxCheck, final T threshold, final FieldTimeStamped<T> ... dates) {
        this(maxCheck, threshold, DEFAULT_MAX_ITER, new FieldStopOnEvent<FieldDateDetector<T>, T>(), dates);
    }

    /** Build a new instance.
     * <p>This constructor is dedicated to single date detection.
     * {@link #getMaxCheckInterval() max check interval} is set to 1.0e10, so almost
     * no other date can be added. Tolerance is set to 1.0e-9.</p>
     * @param target target date
     * @see #addEventDate(FieldAbsoluteDate)
     */
    public FieldDateDetector(final FieldAbsoluteDate<T> target) {
        this(target.getField().getZero().add(1.0e10), target.getField().getZero().add(1.e-9), target);
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
     * @param dates list of event dates
     */
    @SafeVarargs
    private FieldDateDetector(final T maxCheck, final T threshold,
                              final int maxIter, final FieldEventHandler<? super FieldDateDetector<T>, T> handler,
                              final FieldTimeStamped<T> ... dates) {
        super(maxCheck, threshold, maxIter, handler);
        this.currentIndex  = -1;
        this.gDate         = null;
        this.eventDateList = new ArrayList<FieldDateDetector.FieldEventDate<T>>(dates.length);
        for (final FieldTimeStamped<T> ts : dates) {
            addEventDate(ts.getDate());
        }
    }

    /** {@inheritDoc} */
    @Override
    protected FieldDateDetector<T> create(final T newMaxCheck, final T newThreshold,
                                          final int newMaxIter, final FieldEventHandler<? super FieldDateDetector<T>, T> newHandler) {
        @SuppressWarnings("unchecked")
        final FieldTimeStamped<T>[] dates = eventDateList.toArray(new FieldEventDate[eventDateList.size()]);
        return new FieldDateDetector<T>(newMaxCheck, newThreshold, newMaxIter, newHandler, dates);
    }

    /** Compute the value of the switching function.
     * This function measures the difference between the current and the target date.
     * @param s the current state information: date, kinematics, attitude
     * @return value of the switching function
     * @exception OrekitException if some specific error occurs
     */
    public T g(final FieldSpacecraftState<T> s) throws OrekitException {
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
     * @see #FieldDateDetector(RealFieldElement, RealFieldElement, FieldTimeStamped...)
     */
    public void addEventDate(final FieldAbsoluteDate<T> target) throws IllegalArgumentException {
        final boolean increasing;
        if (currentIndex < 0) {
            increasing = (gDate == null) ? true : target.durationFrom(gDate).getReal() > 0.0;
            currentIndex = 0;
            eventDateList.add(new FieldEventDate<T>(target, increasing));
        } else {
            final int lastIndex = eventDateList.size() - 1;
            if (eventDateList.get(0).getDate().durationFrom(target).getReal() > getMaxCheckInterval().getReal()) {
                increasing = !eventDateList.get(0).isgIncrease();
                eventDateList.add(0, new FieldEventDate<T>(target, increasing));
                currentIndex++;
            } else if (target.durationFrom(eventDateList.get(lastIndex).getDate()).getReal() > getMaxCheckInterval().getReal()) {
                increasing = !eventDateList.get(lastIndex).isgIncrease();
                eventDateList.add(new FieldEventDate<T>(target, increasing));
            } else {
                throw new OrekitIllegalArgumentException(OrekitMessages.EVENT_DATE_TOO_CLOSE,
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
    private static class FieldEventDate<T extends RealFieldElement<T>> implements FieldTimeStamped<T> {

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
