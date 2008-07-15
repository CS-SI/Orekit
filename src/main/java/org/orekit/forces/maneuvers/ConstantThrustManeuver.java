/* Copyright 2002-2008 CS Communication & Systèmes
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

import org.apache.commons.math.geometry.Vector3D;
import org.orekit.errors.OrekitException;
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
 * {@link org.orekit.attitudes.LofOffset LOF aligned} attitude law for state propagation and a
 * velocity increment along the +X satellite axis.</p>
 * @author Fabien Maussion
 * @author V&eacute;ronique Pommier-Maurussane
 * @author Luc Maisonobe
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
public class ConstantThrustManeuver implements ForceModel {

    /** Reference gravity acceleration constant (m/s<sup>2</sup>). */
    public static final double G0 = 9.80665;

    /** Serializable UID. */
    private static final long serialVersionUID = 5349622732741384211L;

    /** State of the engine. */
    private boolean firing;

    /** Start of the maneuver. */
    private final AbsoluteDate startDate;

    /** End of the maneuver. */
    private final AbsoluteDate endDate;

    /** Engine thrust. */
    private final double thrust;

    /** Engine flow-rate. */
    private final double flowRate;

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

        if (duration >= 0) {
            this.startDate = date;
            this.endDate   = new AbsoluteDate(date, duration);
        } else {
            this.endDate   = date;
            this.startDate = new AbsoluteDate(endDate, duration);
        }

        this.thrust    = thrust;
        this.flowRate  = -thrust / (G0 * isp);
        this.direction = direction.normalize();
        firing = false;

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

    /** Detector for start of maneuver. */
    private class FiringStartDetector extends DateDetector {

        /** Serializable UID. */
        private static final long serialVersionUID = 2934194165854130641L;

        /** Build an instance. */
        public FiringStartDetector() {
            super(startDate);
        }

        /** {@inheritDoc} */
        public int eventOccurred(final SpacecraftState s) {
            // start the maneuver
            firing = true;
            return RESET_DERIVATIVES;
        }

    }

    /** Detector for end of maneuver. */
    private class FiringStopDetector extends DateDetector {

        /** Serializable UID. */
        private static final long serialVersionUID = 1909257179592824650L;

        /** Build an instance. */
        public FiringStopDetector() {
            super(endDate);
        }

        /** {@inheritDoc} */
        public int eventOccurred(final SpacecraftState s) {
            // stop the maneuver
            firing = false;
            return RESET_DERIVATIVES;
        }

    }

}
