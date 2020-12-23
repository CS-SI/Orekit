/* Copyright 2002-2020 CS GROUP
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
 * @since 10.2
 */
public class DateBasedManeuverTriggers implements ManeuverTriggers {

    /** Start of the maneuver. */
    private final AbsoluteDate startDate;

    /** End of the maneuver. */
    private final AbsoluteDate endDate;

    /** Triggered date of engine start. */
    private AbsoluteDate triggeredStart;

    /** Triggered date of engine stop. */
    private AbsoluteDate triggeredEnd;

    /** Propagation direction. */
    private boolean forward;

    public DateBasedManeuverTriggers(final AbsoluteDate date,
                                     final double duration) {
        if (duration >= 0) {
            this.startDate = date;
            this.endDate   = date.shiftedBy(duration);
        } else {
            this.endDate   = date;
            this.startDate = endDate.shiftedBy(duration);
        }
        this.triggeredStart    = null;
        this.triggeredEnd      = null;

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
        this.forward             = sDate.compareTo(target) < 0;
        final boolean isBetween  = sDate.isBetween(startDate, endDate);
        final boolean isOnStart  = startDate.compareTo(sDate) == 0;
        final boolean isOnEnd    = endDate.compareTo(sDate) == 0;

        triggeredStart = null;
        triggeredEnd   = null;
        if (forward) {
            if (isBetween || isOnStart) {
                triggeredStart = startDate;
            }
        } else {
            if (isBetween || isOnEnd) {
                triggeredEnd = endDate;
            }
        }
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
                triggeredStart = state.getDate();
                return Action.RESET_DERIVATIVES;
            }
            );
        final DateDetector endDetector = new DateDetector(endDate).
            withHandler((SpacecraftState state, DateDetector d, boolean increasing) -> {
                triggeredEnd = state.getDate();
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
                triggeredStart = state.getDate().toAbsoluteDate();
                return Action.RESET_DERIVATIVES;
            });
        final FieldDateDetector<T> endDetector = new FieldDateDetector<>(new FieldAbsoluteDate<>(field, endDate)).
            withHandler((FieldSpacecraftState<T> state, FieldDateDetector<T> d, boolean increasing) -> {
                triggeredEnd = state.getDate().toAbsoluteDate();
                return Action.RESET_DERIVATIVES;
            });
        return Stream.of(startDetector, endDetector);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isFiring(final AbsoluteDate date, final double[] parameters) {
        // Firing state does not depend on a parameter driver here
        return isFiring(date);
    }

    @Override
    public <T extends RealFieldElement<T>> boolean isFiring(final FieldAbsoluteDate<T> date,
                                                            final T[] parameters) {
        // Firing state does not depend on a parameter driver here
        return isFiring(date.toAbsoluteDate());
    }

    /** Check if maneuvering is on.
     * @param date current date
     * @return true if maneuver is on at this date
     */
    public boolean isFiring(final AbsoluteDate date) {
        if (forward) {
            if (triggeredStart == null) {
                // explicitly ignores state date, as propagator did not allow us to introduce discontinuity
                return false;
            } else if (date.durationFrom(triggeredStart) < 0.0) {
                // we are unambiguously before maneuver start
                return false;
            } else {
                if (triggeredEnd == null) {
                    // explicitly ignores state date, as propagator did not allow us to introduce discontinuity
                    return true;
                } else if (date.durationFrom(triggeredEnd) < 0.0) {
                    // we are unambiguously before maneuver end
                    return true;
                } else {
                    // we are at or after maneuver end
                    return false;
                }
            }
        } else {
            if (triggeredEnd == null) {
                // explicitly ignores state date, as propagator did not allow us to introduce discontinuity
                return false;
            } else if (date.durationFrom(triggeredEnd) > 0.0) {
                // we are unambiguously after maneuver end
                return false;
            } else {
                if (triggeredStart == null) {
                    // explicitly ignores state date, as propagator did not allow us to introduce discontinuity
                    return true;
                } else if (date.durationFrom(triggeredStart) > 0.0) {
                    // we are unambiguously after maneuver start
                    return true;
                } else {
                    // we are at or before maneuver start
                    return false;
                }
            }
        }
    }

}
