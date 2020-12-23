/* Copyright 2020 Exotrail
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Exotrail licenses this file to You under the Apache License, Version 2.0
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
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.AbstractDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;

/**
 * Maneuver triggers based on start and stop detectors. This allow a succession
 * of burn interval. The thruster starts firing when the start detector becomes
 * positive. The thruster stops firing when the stop detector becomes positive.
 * The 2 detectors should not be positive at the same time. A date detector is
 * not suited as it does not delimit an interval. They can be both negative at
 * the same time.
 * @author Mikael Fillastre
 * @author Andrea Fiorentino
 * @since 10.2
 */
public class EventBasedManeuverTriggers implements ManeuverTriggers, EventHandler<EventDetector> {

    /** Detector to start firing, only detect increasing sign change. */
    private final AbstractDetector<? extends EventDetector> startFiringDetector;
    /**
     * Detector to stop firing, only detect increasing sign change. e.g. it can be a
     * negate detector of the start detector
     */
    private final AbstractDetector<? extends EventDetector> stopFiringDetector;

    /**
     * Flag for init method, called several times : force models + each detector.
     */
    private boolean initialized;

    /** Triggered date of engine start. */
    private AbsoluteDate triggeredStart;

    /** Triggered date of engine stop. */
    private AbsoluteDate triggeredEnd;

    /**
     * Constructor.
     * @param startFiringDetector Detector to start firing, only detect increasing
     *                            sign change
     * @param stopFiringDetector  Detector to stop firing, only detect increasing
     *                            sign change. e.g. it can be a negate detector of
     *                            the start detector.
     */
    public EventBasedManeuverTriggers(final AbstractDetector<? extends EventDetector> startFiringDetector,
            final AbstractDetector<? extends EventDetector> stopFiringDetector) {
        if (startFiringDetector == null) {
            throw new OrekitException(OrekitMessages.PARAMETER_NOT_SET, "stopFiringDetector",
                    EventBasedManeuverTriggers.class.getSimpleName());
        }
        if (stopFiringDetector == null) {
            throw new OrekitException(OrekitMessages.PARAMETER_NOT_SET, "startFiringDetector",
                    EventBasedManeuverTriggers.class.getSimpleName());
        }
        this.startFiringDetector = startFiringDetector.withHandler(this);
        this.stopFiringDetector = stopFiringDetector.withHandler(this);
        this.triggeredStart = null;
        this.triggeredEnd = null;
        initialized = false;
    }

    /**
     * Getter for the start firing detector.
     * @return Detectors to start firing,
     */
    public AbstractDetector<? extends EventDetector> getStartFiringDetector() {
        return startFiringDetector;
    }

    /**
     * Getter for the stop firing detector.
     * @return Detectors to stop firing
     */
    public AbstractDetector<? extends EventDetector> getStopFiringDetector() {
        return stopFiringDetector;
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState initialState, final AbsoluteDate target) {

        if (!initialized) {

            initialized = true;
            final AbsoluteDate sDate = initialState.getDate();
            if (sDate.compareTo(target) > 0) {
                // backward propagation not managed because events on detectors can not be
                // reversed :
                // the stop event of the maneuver in forward direction won't be the start in the
                // backward.
                // e.g. if a stop detector is combination of orbit position and system
                // constraint
                throw new OrekitException(OrekitMessages.FUNCTION_NOT_IMPLEMENTED,
                        "EventBasedManeuverTriggers in backward propagation");
            }

            checkInitialFiringState(initialState);

        } // multiples calls to init : because it is a force model and by each detector
    }

    /**
     * Method to set the firing state on initialization. can be overloaded by sub
     * classes.
     *
     * @param initialState initial spacecraft state
     */
    protected void checkInitialFiringState(final SpacecraftState initialState) {
        if (isFiringOnInitialState(initialState)) {
            setFiring(true, initialState.getDate());
        }
    }

    /**
     * Method to check if the thruster is firing on initialization. can be called by
     * sub classes
     *
     * @param initialState initial spacecraft state
     * @return true if firing
     */
    protected boolean isFiringOnInitialState(final SpacecraftState initialState) {
        // set the initial value of firing
        final double insideThrustArcG = getStartFiringDetector().g(initialState);
        boolean isInsideThrustArc = false;

        if (insideThrustArcG == 0) {
            // bound of arc
            // check state for the next second (which can be forward or backward)
            final double nextSecond = 1;
            final double nextValue = getStartFiringDetector().g(initialState.shiftedBy(nextSecond));
            isInsideThrustArc = nextValue > 0;
        } else {
            isInsideThrustArc = insideThrustArcG > 0;
        }
        return isInsideThrustArc;
    }

    /** {@inheritDoc} */
    @Override
    public Stream<EventDetector> getEventsDetectors() {
        return Stream.of(getStartFiringDetector(), getStopFiringDetector());
    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventsDetectors(final Field<T> field) {
        // not implemented, it depends on the input detectors
        throw new OrekitException(OrekitMessages.FUNCTION_NOT_IMPLEMENTED,
                "EventBasedManeuverTriggers.getFieldEventsDetectors");
    }

    /**
     * Set the firing start or end date depending on the firing flag. There is no
     * effect if the firing state is not changing.
     * @param firing true to start a maneuver, false to stop
     * @param date   date of event
     */
    public void setFiring(final boolean firing, final AbsoluteDate date) {
        if (firing != isFiring(date)) {
            if (firing) {
                if (!date.equals(triggeredEnd)) {
                    triggeredStart = date;
                } // else no gap between stop and start, can not handle correctly : skip it
            } else {
                triggeredEnd = date;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isFiring(final AbsoluteDate date, final double[] parameters) {
        // Firing state does not depend on a parameter driver here
        return isFiring(date);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> boolean isFiring(final FieldAbsoluteDate<T> date, final T[] parameters) {
        // Firing state does not depend on a parameter driver here
        return isFiring(date.toAbsoluteDate());
    }

    /** {@inheritDoc} */
    @Override
    public Action eventOccurred(final SpacecraftState s, final EventDetector detector, final boolean increasing) {
        Action action = Action.CONTINUE; // default not taken into account
        final boolean detectorManaged = getEventsDetectors()
                .anyMatch(managedDetector -> managedDetector.equals(detector));
        if (detectorManaged) {
            action = Action.RESET_EVENTS;
            if (increasing) {
                if (detector.equals(getStartFiringDetector())) { // start of firing arc
                    setFiring(true, s.getDate());
                    action = Action.RESET_DERIVATIVES;
                } else if (detector.equals(getStopFiringDetector())) { // end of firing arc
                    setFiring(false, s.getDate());
                    action = Action.RESET_DERIVATIVES;
                }
            }
        }
        return action;
    }

    /**
     * Check if maneuvering is on.
     *
     * @param date current date
     * @return true if maneuver is on at this date
     */
    public boolean isFiring(final AbsoluteDate date) {
        if (triggeredStart == null) {
            // explicitly ignores state date, as propagator did not allow us to introduce
            // discontinuity
            return false;
        } else if (date.isBefore(triggeredStart)) {
            // we are unambiguously before maneuver start
            // robustness, we should not pass here
            return false;
        } else {
            // after start date
            if (getTriggeredEnd() == null) {
                // explicitly ignores state date, as propagator did not allow us to introduce
                // discontinuity
                return true;
            } else if (getTriggeredStart().isAfter(getTriggeredEnd())) {
                // last event is a start of maneuver, end not set yet
                // we are unambiguously before maneuver end
                return true;
            } else if (date.isBefore(getTriggeredEnd())) {
                // we are unambiguously before maneuver end
                // robustness, we should not pass here
                return true;
            } else {
                // we are at or after maneuver end
                return false;
            }
        }
    }

    /**
     * Getter for the triggered date of engine stop.
     * @return Triggered date of engine stop
     */
    public AbsoluteDate getTriggeredEnd() {
        return triggeredEnd;
    }

    /**
     * Getter triggered date of engine start.
     * @return Triggered date of engine start
     */
    public AbsoluteDate getTriggeredStart() {
        return triggeredStart;
    }

}
