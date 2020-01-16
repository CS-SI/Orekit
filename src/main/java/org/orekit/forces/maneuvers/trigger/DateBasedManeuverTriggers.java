/* Copyright 2002-2020 CS Systèmes d'Information
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
package org.orekit.forces.maneuvers.trigger;

import java.util.stream.Stream;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.ode.events.Action;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldDateDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;

/** Maneuver triggers based on a start and end date, with no parameter drivers.
 * @author Maxime Journot
 */
public class DateBasedManeuverTriggers implements ManeuverTriggers {

    /** Start of the maneuver. */
    private final AbsoluteDate startDate;

    /** End of the maneuver. */
    private final AbsoluteDate endDate;

    /** State of the engine, currently firing or not. */
    private boolean firing;

    public DateBasedManeuverTriggers(final AbsoluteDate date,
                                     final double duration) {
        if (duration >= 0) {
            this.startDate = date;
            this.endDate   = date.shiftedBy(duration);
        } else {
            this.endDate   = date;
            this.startDate = endDate.shiftedBy(duration);
        }
        this.firing = false;
    }

    /** Get the start date.
     * @return the start date
     */
    public AbsoluteDate getStartDate() {
        return startDate;
    }

    /** Get the end date.
     * @return the end date
     */
    public AbsoluteDate getEndDate() {
        return endDate;
    }

    /** Get the duration of the maneuver (s).
     * duration = endDate - startDate
     * @return the duration of the maneuver (s)
     */
    public double getDuration() {
        return endDate.durationFrom(startDate);
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState initialState, final AbsoluteDate target) {
        // set the initial value of firing
        final AbsoluteDate sDate = initialState.getDate();
        final boolean isForward = sDate.compareTo(target) < 0;
        final boolean isBetween =
                startDate.compareTo(sDate) < 0 && endDate.compareTo(sDate) > 0;
        final boolean isOnStart = startDate.compareTo(sDate) == 0;
        final boolean isOnEnd = endDate.compareTo(sDate) == 0;

        firing = isBetween || (isForward && isOnStart) || (!isForward && isOnEnd);
    }

    /** {@inheritDoc} */
    @Override
    public Stream<EventDetector> getEventsDetectors() {
        // In forward propagation direction, firing must be enabled
        // at start time and disabled at end time; in backward
        // propagation direction, firing must be enabled
        // at end time and disabled at start time
        final DateDetector startDetector = new DateDetector(startDate).
            withHandler((SpacecraftState state, DateDetector d, boolean increasing) -> {
                firing = d.isForward();
                return Action.RESET_DERIVATIVES;
            }
            );
        final DateDetector endDetector = new DateDetector(endDate).
            withHandler((SpacecraftState state, DateDetector d, boolean increasing) -> {
                firing = !d.isForward();
                return Action.RESET_DERIVATIVES;
            });
        return Stream.of(startDetector, endDetector);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventsDetectors(final Field<T> field) {
        // In forward propagation direction, firing must be enabled
        // at start time and disabled at end time; in backward
        // propagation direction, firing must be enabled
        // at end time and disabled at start time
        final FieldDateDetector<T> startDetector = new FieldDateDetector<>(new FieldAbsoluteDate<>(field, startDate)).
            withHandler((FieldSpacecraftState<T> state, FieldDateDetector<T> d, boolean increasing) -> {
                firing = d.isForward();
                return Action.RESET_DERIVATIVES;
            });
        final FieldDateDetector<T> endDetector = new FieldDateDetector<>(new FieldAbsoluteDate<>(field, endDate)).
            withHandler((FieldSpacecraftState<T> state, FieldDateDetector<T> d, boolean increasing) -> {
                firing = !d.isForward();
                return Action.RESET_DERIVATIVES;
            });
        return Stream.of(startDetector, endDetector);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isFiring(final double[] parameters) {
        // Firing state does not depend on a parameter driver here
        return firing;
    }

    @Override
    public <T extends RealFieldElement<T>> boolean isFiring(final T parameters[]) {
        // Firing state does not depend on a parameter driver here
        return firing;
    }
}
