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
package org.orekit.forces.maneuvers;

import org.apache.commons.math.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.forces.AbstractParameterizable;
import org.orekit.forces.ForceModel;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.numerical.TimeDerivativesEquations;
import org.orekit.time.AbsoluteDate;

/** This class implements a simple maneuver with constant thrust.
 * <p>The maneuver is defined by a direction in satelliteframe.
 * The current attitude of the spacecraft, defined by the current
 * spacecraft state, will be used to compute the thrust direction in
 * inertial frame. A typical case for tangential maneuvers is to use a
 * {@link org.orekit.attitudes.LofOffset LOF aligned} attitude provider for state propagation and a
 * velocity increment along the +X satellite axis.</p>
 * @author Fabien Maussion
 * @author V&eacute;ronique Pommier-Maurussane
 * @author Luc Maisonobe
 */
public class ConstantThrustManeuver extends AbstractParameterizable implements ForceModel {

    /** Reference gravity acceleration constant (m/s<sup>2</sup>). */
    public static final double G0 = 9.80665;

    /** Parameter name for thrust. */
    private static final String THRUST = "thrust";

    /** Parameter name for flow rate. */
    private static final String FLOW_RATE = "flow rate";

    /** Serializable UID. */
    private static final long serialVersionUID = 5349622732741384211L;

    /** State of the engine. */
    private boolean firing;

    /** Start of the maneuver. */
    private final AbsoluteDate startDate;

    /** End of the maneuver. */
    private final AbsoluteDate endDate;

    /** Engine thrust. */
    private double thrust;

    /** Engine flow-rate. */
    private double flowRate;

    /** Direction of the acceleration in satellite frame. */
    private final Vector3D direction;

    /** Simple constructor for a constant direction and constant thrust.
     * @param date maneuver date
     * @param duration the duration of the thrust (s) (if negative,
     * the date is considered to be the stop date)
     * @param thrust the thrust force (N)
     * @param isp engine specific impulse (s)
     * @param direction the acceleration direction in satellite frame.
     */
    public ConstantThrustManeuver(final AbsoluteDate date, final double duration,
                                  final double thrust, final double isp,
                                  final Vector3D direction) {

        super(THRUST, FLOW_RATE);
        if (duration >= 0) {
            this.startDate = date;
            this.endDate   = date.shiftedBy(duration);
        } else {
            this.endDate   = date;
            this.startDate = endDate.shiftedBy(duration);
        }

        this.thrust    = thrust;
        this.flowRate  = -thrust / (G0 * isp);
        this.direction = direction.normalize();
        firing = false;

    }

    /** Get the thrust.
     * @return thrust force (N).
     */
    public double getThrust() {
        return thrust;
    }

    /** Get the specific impulse.
     * @return specific impulse (s).
     */
    public double getISP() {
        return -thrust / (G0 * flowRate);
    }

    /** Get the flow rate.
     * @return flow rate (negative, kg/s).
     */
    public double getFlowRate() {
        return flowRate;
    }

    /** {@inheritDoc} */
    public void addContribution(final SpacecraftState s, final TimeDerivativesEquations adder)
        throws OrekitException {

        if (firing) {

            // compute thrust acceleration in inertial frame
            adder.addAcceleration(new Vector3D(thrust / s.getMass(),
                                               s.getAttitude().getRotation().applyInverseTo(direction)),
                                  s.getFrame());

            // compute flow rate
            adder.addMassDerivative(flowRate);

        }

    }

    /** {@inheritDoc} */
    public EventDetector[] getEventsDetectors() {
        return new EventDetector[] {
            new FiringStartDetector(), new FiringStopDetector()
        };
    }

    /** {@inheritDoc} */
    public double getParameter(final String name)
        throws IllegalArgumentException {
        complainIfNotSupported(name);
        if (name.equals(THRUST)) {
            return thrust;
        }
        return flowRate;
    }

    /** {@inheritDoc} */
    public void setParameter(final String name, final double value)
        throws IllegalArgumentException {
        complainIfNotSupported(name);
        if (name.equals(THRUST)) {
            thrust = value;
        } else {
            flowRate = value;
        }
    }

    /** Detector for start of maneuver. */
    private class FiringStartDetector extends DateDetector {

        /** Serializable UID. */
        private static final long serialVersionUID = -6518235379334993498L;

        /** Build an instance. */
        public FiringStartDetector() {
            super(startDate);
        }

        /** {@inheritDoc} */
        public Action eventOccurred(final SpacecraftState s, final boolean increasing) {
            // start the maneuver
            firing = true;
            return Action.RESET_DERIVATIVES;
        }

    }

    /** Detector for end of maneuver. */
    private class FiringStopDetector extends DateDetector {

        /** Serializable UID. */
        private static final long serialVersionUID = -8037677613943782679L;

        /** Build an instance. */
        public FiringStopDetector() {
            super(endDate);
        }

        /** {@inheritDoc} */
        public Action eventOccurred(final SpacecraftState s, final boolean increasing) {
            // stop the maneuver
            firing = false;
            return Action.RESET_DERIVATIVES;
        }

    }

}
