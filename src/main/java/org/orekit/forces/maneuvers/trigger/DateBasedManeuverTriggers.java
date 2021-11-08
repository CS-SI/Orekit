/* Copyright 2002-2021 CS GROUP
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
package org.orekit.forces.maneuvers.trigger;

import java.lang.reflect.Array;
import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.events.FieldAbstractDetector;
import org.orekit.propagation.events.FieldDateDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.events.handlers.ContinueOnEvent;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeStamped;

/** Maneuver triggers based on a start and end date, with no parameter drivers.
 * @author Maxime Journot
 * @since 10.2
 */
public class DateBasedManeuverTriggers extends IntervalEventTrigger<DateDetector> {

    /** Simple constructor.
     * @param date start (or end) data of the maneuver
     * @param duration maneuver duration (if positive, maneuver is from date to date + duration,
     * if negative, maneuver will be from date - duration to date)
     */
    public DateBasedManeuverTriggers(final AbsoluteDate date, final double duration) {
        super(createDetector(date, duration).withHandler(new ContinueOnEvent<>()));
    }

    /** Create a date detector from one boundary and signed duration.
     * @param date start (or end) data of the maneuver
     * @param duration maneuver duration (if positive, maneuver is from date to date + duration,
     * if negative, maneuver will be from date - duration to date)
     * @return date detector
     * @since 11.1
     */
    private static DateDetector createDetector(final AbsoluteDate date, final double duration) {
        if (duration >= 0) {
            return new DateDetector( 0.5 * duration, 1.0e-10, date, date.shiftedBy(duration));
        } else {
            return new DateDetector(-0.5 * duration, 1.0e-10, date.shiftedBy(duration), date);
        }
    }

    /** Get the start date.
     * @return the start date
     */
    public AbsoluteDate getStartDate() {
        return getFiringIntervalDetector().getDates().get(0).getDate();
    }

    /** Get the end date.
     * @return the end date
     */
    public AbsoluteDate getEndDate() {
        return getFiringIntervalDetector().getDates().get(1).getDate();
    }

    /** Get the duration of the maneuver (s).
     * duration = endDate - startDate
     * @return the duration of the maneuver (s)
     */
    public double getDuration() {
        return getEndDate().durationFrom(getStartDate());
    }

    /** {@inheritDoc} */
    @Override
    protected <D extends FieldEventDetector<S>, S extends CalculusFieldElement<S>>
        FieldAbstractDetector<D, S> convertIntervalDetector(final Field<S> field, final DateDetector detector) {

        // here, we must set maxCheckInterval, otherwise we will get an exception when adding the second date,
        // because the it must be more than maxCheckInterval after the first,
        // and the default maxCheckInterval is 1.0e10 seconds…
        final S maxCheck = field.getZero().newInstance(detector.getMaxCheckInterval());

        final List<TimeStamped> dates = detector.getDates();
        @SuppressWarnings("unchecked")
        final FieldAbsoluteDate<S>[] fDates =  (FieldAbsoluteDate<S>[]) Array.newInstance(FieldAbsoluteDate.class, dates.size());
        for (int i = 0; i < dates.size(); ++i) {
            fDates[i] = new FieldAbsoluteDate<>(field, dates.get(i).getDate());
        }

        @SuppressWarnings("unchecked")
        final FieldAbstractDetector<D, S> converted = (FieldAbstractDetector<D, S>) new FieldDateDetector<S>(maxCheck, null, fDates);
        return converted;

    }

}
