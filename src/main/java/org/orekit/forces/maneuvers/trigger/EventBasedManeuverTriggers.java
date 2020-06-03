/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.AbstractDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;

/** Maneuver triggers based on start and stop detectors.
 * This allow a succession of burn interval.
 * The thruster starts firing when the start detector becomes positive.
 * The thruster stops firing when the stop detector becomes positive.
 * The 2 detectors should not be positive at the same time.
 * They can be both negative 
 * @author Mikael Fillastre
 * @author Andrea Fiorentino
 */
public class EventBasedManeuverTriggers implements ManeuverTriggers, EventHandler<EventDetector> {

	/** detectors to start firing, only detect increasing sign change */
    private final AbstractDetector<? extends EventDetector> startFiringDetector;
	/** detectors to stop firing, only detect increasing sign change. 
	 * e.g. it can be a negate detector of the start detector*/
    private final AbstractDetector<? extends EventDetector> stopFiringDetector;
    
    /** flag for init method, called several times : force models + each detector */
    private boolean initialized;

    /** Triggered date of engine start. */
    private AbsoluteDate triggeredStart;

    /** Triggered date of engine stop. */
    private AbsoluteDate triggeredEnd;

    /** Propagation direction. */
    private boolean forward;

    public EventBasedManeuverTriggers (
            final AbstractDetector<? extends EventDetector> startFiringDetector,
            final AbstractDetector<? extends EventDetector> stopFiringDetector) {
        this.startFiringDetector = startFiringDetector.withHandler(this);
        this.stopFiringDetector = stopFiringDetector.withHandler(this);
        this.triggeredStart = null;
        this.triggeredEnd = null;
        initialized = false;
        forward = true; // because init not called from DSST
    }

    public AbstractDetector<? extends EventDetector> getStartFiringDetector() {
        return startFiringDetector;
    }

    public AbstractDetector<? extends EventDetector> getStopFiringDetector() {
        return stopFiringDetector;
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState initialState, final AbsoluteDate target) {

        if (!initialized) {

            initialized = true;
            if (stopFiringDetector == null) {
                throw new OrekitException(OrekitMessages.PARAMETER_NOT_SET, "stopFiringDetector", "EventBasedManeuverTriggers");
            }
            if (startFiringDetector == null) {
                throw new OrekitException(OrekitMessages.PARAMETER_NOT_SET, "startFiringDetector", "EventBasedManeuverTriggers");
            }
            final AbsoluteDate sDate = initialState.getDate();
            this.forward = sDate.compareTo(target) < 0;

            checkInitialFiringState(initialState);

        } // multiples calls to init : because it is a force model and by each detector
    }

    /**
     * can be overloaded by sub classes
     * 
     * @param initialState
     * @param target
     */
    protected void checkInitialFiringState(final SpacecraftState initialState) {
        if (isFiringOnInitialState(initialState)) {
            setFiring(true, initialState.getDate());
        }
    }

    /**
     * can be called by sub classes
     * 
     * @param initialState
     * @param target
     */
    protected boolean isFiringOnInitialState(final SpacecraftState initialState) {
        // set the initial value of firing
        double insideThrustArcG = startFiringDetector.g(initialState);
        boolean isInsideThrustArc = false;

        if (insideThrustArcG == 0) {
            // bound of arc
            // check state for the next second (which can be forward or backward)
            double nextSecond = isForward() ? 1 : -1;
            double nextValue = startFiringDetector.g(initialState.shiftedBy(nextSecond));
            isInsideThrustArc = nextValue > 0;
        } else {
            isInsideThrustArc = insideThrustArcG > 0;
        }
        return isInsideThrustArc;
    }

    /** {@inheritDoc} */
    @Override
    public Stream<EventDetector> getEventsDetectors() {
        return Stream.of(startFiringDetector, stopFiringDetector);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventsDetectors(
            final Field<T> field) {
    	// not implemented, it depends on the input detectors
    	throw new OrekitException(OrekitMessages.FUNCTION_NOT_IMPLEMENTED, "EventBasedManeuverTriggers.getFieldEventsDetectors");
    }

    public void setFiring(boolean firing, AbsoluteDate date) {
        if (firing != isFiring(date)) {
            if (isForward()) {
                if (firing) {
                	if (!date.equals(triggeredEnd)) {
                		triggeredStart = date;
                	} // else no gap between stop and start, can not handle correctly : skip it
                } else {
                    triggeredEnd = date;
                }
            } else { // backward propagation
                if (firing) { // start firing by end date
                	if (!date.equals(triggeredStart)) {
                		triggeredEnd = date;
                	} // else no gap between stop and start, can not handle correctly : skip it
                } else {
                    triggeredStart = date;
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isFiring(final AbsoluteDate date, final double[] parameters) {
        // Firing state does not depend on a parameter driver here
        return isFiring(date);
    }

    @Override
    public <T extends RealFieldElement<T>> boolean isFiring(final FieldAbsoluteDate<T> date,
            final T parameters[]) {
        // Firing state does not depend on a parameter driver here
        return isFiring(date.toAbsoluteDate());
    }

    @Override
    public Action eventOccurred(SpacecraftState s, EventDetector detector, boolean increasing) {
        Action action = Action.CONTINUE; // default not taken into account
        boolean detectorManaged = getEventsDetectors()
                .anyMatch(managedDetector -> managedDetector.equals(detector));
        if (detectorManaged) {
            action = Action.RESET_EVENTS;
            if (isForward()) {
                if (increasing) {
                    if (detector.equals(startFiringDetector)) { // start of firing arc
                        setFiring(true, s.getDate());
                        action = Action.RESET_DERIVATIVES;
                    } else if (detector.equals(stopFiringDetector)) {// end of firing arc
                        setFiring(false, s.getDate());
                        action = Action.RESET_DERIVATIVES;
                    }
                }
            } else { // backward propagation. We could write a code on 3 lines but that would be
                     // harder to understand and debug. So we do prefer explicit code
                if (!increasing) {
                    if (detector.equals(startFiringDetector)) { // end of firing arc
                        setFiring(false, s.getDate());
                        action = Action.RESET_DERIVATIVES;
                    } else if (detector.equals(stopFiringDetector)) {// start of firing arc
                        setFiring(true, s.getDate());
                        action = Action.RESET_DERIVATIVES;
                    }
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
        if (isForward()) {
            if (triggeredStart == null) {
                // explicitly ignores state date, as propagator did not allow us to introduce
                // discontinuity
                return false;
            } else if (date.isBefore(triggeredStart)) {
                // we are unambiguously before maneuver start
                return false;
            } else {
                // after start date
                if (getTriggeredEnd() == null) {
                    // explicitly ignores state date, as propagator did not allow us to introduce
                    // discontinuity
                    return true;
                } else if (triggeredStart.isAfter(getTriggeredEnd())) {
                    // last event is a start of maneuver, end not set yet
                    // we are unambiguously before maneuver end
                    return true;
                } else if (date.isBefore(getTriggeredEnd())) {
                    // we are unambiguously before maneuver end
                    return true;
                } else {
                    // we are at or after maneuver end
                    return false;
                }
            }
        } else { // backward propagation, start firing by triggeredEnd
            if (getTriggeredEnd() == null) {
                // explicitly ignores state date, as propagator did not allow us to introduce
                // discontinuity
                return false;
            } else if (date.isAfter(getTriggeredEnd())) {
                // we are unambiguously after maneuver end
                return false;
            } else {
                if (triggeredStart == null) {
                    // explicitly ignores state date, as propagator did not allow us to introduce
                    // discontinuity
                    return true;
                } else if (getTriggeredEnd().isBefore(triggeredStart)) {
                    // last event is a end of maneuver (which means firing in backward propagation)
                    // , start not set yet
                    // we are unambiguously before maneuver end
                    return true;
                } else if (date.isAfter(triggeredStart)) {
                    // we are unambiguously after maneuver start
                    return true;
                } else {
                    // we are at or before maneuver start
                    return false;
                }
            }
        }
    }

    public boolean isForward() {
        return forward;
    }

    public AbsoluteDate getTriggeredEnd() {
        return triggeredEnd;
    }

    public AbsoluteDate getTriggeredStart() {
        return triggeredStart;
    }

}
