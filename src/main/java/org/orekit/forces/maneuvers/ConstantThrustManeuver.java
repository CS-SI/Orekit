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
package org.orekit.forces.maneuvers;

import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.apache.commons.math3.geometry.euclidean.threed.FieldRotation;
import org.apache.commons.math3.geometry.euclidean.threed.FieldVector3D;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.ode.AbstractParameterizable;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.ForceModel;
import org.orekit.frames.Frame;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.numerical.TimeDerivativesEquations;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

/** This class implements a simple maneuver with constant thrust.
 * <p>The maneuver is defined by a direction in satellite frame.
 * The current attitude of the spacecraft, defined by the current
 * spacecraft state, will be used to compute the thrust direction in
 * inertial frame. A typical case for tangential maneuvers is to use a
 * {@link org.orekit.attitudes.LofOffset LOF aligned} attitude provider
 * for state propagation and a velocity increment along the +X satellite axis.</p>
 * @author Fabien Maussion
 * @author V&eacute;ronique Pommier-Maurussane
 * @author Luc Maisonobe
 */
public class ConstantThrustManeuver extends AbstractParameterizable implements ForceModel {

    /** Parameter name for thrust. */
    private static final String THRUST = "thrust";

    /** Parameter name for flow rate. */
    private static final String FLOW_RATE = "flow rate";

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
        this.flowRate  = -thrust / (Constants.G0_STANDARD_GRAVITY * isp);
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
        return -thrust / (Constants.G0_STANDARD_GRAVITY * flowRate);
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
    public FieldVector3D<DerivativeStructure> accelerationDerivatives(final AbsoluteDate date, final Frame frame,
                                                                      final FieldVector3D<DerivativeStructure> position,
                                                                      final FieldVector3D<DerivativeStructure> velocity,
                                                                      final FieldRotation<DerivativeStructure> rotation,
                                                                      final DerivativeStructure mass)
        throws OrekitException {
        if (firing) {
            return new FieldVector3D<DerivativeStructure>(mass.reciprocal().multiply(thrust),
                    rotation.applyInverseTo(direction));
        } else {
            // constant (and null) acceleration when not firing
            final int parameters = mass.getFreeParameters();
            final int order      = mass.getOrder();
            final DerivativeStructure zero = new DerivativeStructure(parameters, order, 0.0);
            return new FieldVector3D<DerivativeStructure>(zero, zero, zero);
        }
    }

    /** {@inheritDoc} */
    public FieldVector3D<DerivativeStructure> accelerationDerivatives(final SpacecraftState s, final String paramName)
        throws OrekitException {

        complainIfNotSupported(paramName);

        if (firing) {

            if (THRUST.equals(paramName)) {
                final DerivativeStructure thrustDS = new DerivativeStructure(1, 1, 0, thrust);
                return new FieldVector3D<DerivativeStructure>(thrustDS.divide(s.getMass()),
                                                              s.getAttitude().getRotation().applyInverseTo(direction));
            } else if (FLOW_RATE.equals(paramName)) {
                // acceleration does not depend on flow rate (only mass decrease does)
                final DerivativeStructure zero = new DerivativeStructure(1, 1, 0.0);
                return new FieldVector3D<DerivativeStructure>(zero, zero, zero);
            } else {
                throw new OrekitException(OrekitMessages.UNSUPPORTED_PARAMETER_NAME, paramName,
                                          THRUST + ", " + FLOW_RATE);
            }

        } else {
            // constant (and null) acceleration when not firing
            final DerivativeStructure zero = new DerivativeStructure(1, 1, 0.0);
            return new FieldVector3D<DerivativeStructure>(zero, zero, zero);
        }

    }

    /** {@inheritDoc} */
    public EventDetector[] getEventsDetectors() {
        return new EventDetector[] {
            new DateDetector(startDate).withHandler(new FiringStartHandler()),
            new DateDetector(endDate).withHandler(new FiringStopHandler())
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

    /** Handler for start of maneuver. */
    private class FiringStartHandler implements EventHandler<DateDetector> {

        /** {@inheritDoc} */
        @Override
        public EventHandler.Action eventOccurred(final SpacecraftState s,
                                                 final DateDetector detector,
                                                 final boolean increasing) {
            if (detector.isForward()) {
                // we are in the forward direction,
                // starting now, the maneuver is ON as it has just started
                firing = true;
            } else {
                // we are in the backward direction,
                // starting now, the maneuver is OFF as it has not started yet
                firing = false;
            }
            return EventHandler.Action.RESET_DERIVATIVES;
        }

        /** {@inheritDoc} */
        @Override
        public SpacecraftState resetState(final DateDetector detector, final SpacecraftState oldState) {
            return oldState;
        }

    }

    /** Handler for end of maneuver. */
    private class FiringStopHandler implements EventHandler<DateDetector> {

        /** {@inheritDoc} */
        public EventHandler.Action eventOccurred(final SpacecraftState s,
                                                 final DateDetector detector,
                                                 final boolean increasing) {
            if (detector.isForward()) {
                // we are in the forward direction,
                // starting now, the maneuver is OFF as it has just ended
                firing = false;
            } else {
                // we are in the backward direction,
                // starting now, the maneuver is ON as it has not ended yet
                firing = true;
            }
            return EventHandler.Action.RESET_DERIVATIVES;
        }

        /** {@inheritDoc} */
        @Override
        public SpacecraftState resetState(final DateDetector detector, final SpacecraftState oldState) {
            return oldState;
        }

    }

}
