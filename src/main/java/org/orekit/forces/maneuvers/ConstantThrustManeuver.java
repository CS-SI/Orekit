/* Copyright 2002-2017 CS Systèmes d'Information
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

import java.util.stream.Stream;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.AbstractForceModel;
import org.orekit.frames.Frame;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldDateDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.propagation.numerical.FieldTimeDerivativesEquations;
import org.orekit.propagation.numerical.TimeDerivativesEquations;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterObserver;

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
public class ConstantThrustManeuver extends AbstractForceModel {

    /** Parameter name for thrust. */
    public static final String THRUST = "thrust";

    /** Parameter name for flow rate. */
    public static final String FLOW_RATE = "flow rate";

    /** Thrust scaling factor.
     * <p>
     * We use a power of 2 to avoid numeric noise introduction
     * in the multiplications/divisions sequences.
     * </p>
     */
    private static final double THRUST_SCALE = FastMath.scalb(1.0, -5);

    /** Flow rate scaling factor.
     * <p>
     * We use a power of 2 to avoid numeric noise introduction
     * in the multiplications/divisions sequences.
     * </p>
     */
    private static final double FLOW_RATE_SCALE = FastMath.scalb(1.0, -12);

    /** Driver for thrust parameter. */
    private final ParameterDriver thrustDriver;

    /** Driver for flow rate parameter. */
    private final ParameterDriver flowRateDriver;

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

    /** Factory for the DerivativeStructure instances. */
    private final DSFactory factory;

    /** Simple constructor for a constant direction and constant thrust.
     * <p>
     * Calling this constructor is equivalent to call {@link
     * #ConstantThrustManeuver(AbsoluteDate, double, double, double, Vector3D, String)
     * ConstantThrustManeuver(date, duration, thrust, isp, direction, "")},
     * hence not using any prefix for the parameters drivers names.
     * </p>
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
        this(date, duration, thrust, isp, direction, "");
    }

    /** Simple constructor for a constant direction and constant thrust.
     * <p>
     * If the {@code driversNamePrefix} is empty, the names will
     * be {@link #THRUST "thrust"} and {@link #FLOW_RATE "flow rate"}, otherwise
     * the prefix is prepended to these fixed strings. A typical use case is to
     * use something like "1A-" or "2B-" as a prefix corresponding to the
     * name of the thruster to use, so separate parameters can be adjusted
     * for the different thrusters involved during an orbit determination
     * where maneuvers parameters are estimated.
     * </p>
     * @param date maneuver date
     * @param duration the duration of the thrust (s) (if negative,
     * the date is considered to be the stop date)
     * @param thrust the thrust force (N)
     * @param isp engine specific impulse (s)
     * @param direction the acceleration direction in satellite frame
     * @param driversNamePrefix prefix for the {@link #getParametersDrivers() parameters drivers}
     * @since 9.0
     */
    public ConstantThrustManeuver(final AbsoluteDate date, final double duration,
                                  final double thrust, final double isp,
                                  final Vector3D direction,
                                  final String driversNamePrefix) {

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

        ParameterDriver tpd = null;
        ParameterDriver fpd = null;
        try {
            tpd = new ParameterDriver(driversNamePrefix + THRUST, thrust, THRUST_SCALE,
                                      0.0, Double.POSITIVE_INFINITY);
            tpd.addObserver(new ParameterObserver() {
                /** {@inheritDoc} */
                @Override
                public void valueChanged(final double previousValue, final ParameterDriver driver) {
                    ConstantThrustManeuver.this.thrust = driver.getValue();
                }
            });
            fpd = new ParameterDriver(driversNamePrefix + FLOW_RATE, flowRate, FLOW_RATE_SCALE,
                                      Double.NEGATIVE_INFINITY, 0.0 );
            fpd.addObserver(new ParameterObserver() {
                /** {@inheritDoc} */
                @Override
                public void valueChanged(final double previousValue, final ParameterDriver driver) {
                    ConstantThrustManeuver.this.flowRate = driver.getValue();
                }
            });
            factory = new DSFactory(1, 1);
        } catch (OrekitException oe) {
            // this should never occur as valueChanged above never throws an exception
            throw new OrekitInternalError(oe);
        }

        this.thrustDriver   = tpd;
        this.flowRateDriver = fpd;

    }

    @Override
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
        // set the initial value of firing
        final AbsoluteDate sDate = s0.getDate();
        final boolean isForward = sDate.compareTo(t) < 0;
        final boolean isBetween =
                startDate.compareTo(sDate) < 0 && endDate.compareTo(sDate) > 0;
        final boolean isOnStart = startDate.compareTo(sDate) == 0;
        final boolean isOnEnd = endDate.compareTo(sDate) == 0;

        firing = isBetween || (isForward && isOnStart) || (!isForward && isOnEnd);
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

    @Override
    public <T extends RealFieldElement<T>> void
        addContribution(final FieldSpacecraftState<T> s,
                        final FieldTimeDerivativesEquations<T> adder)
            throws OrekitException {
        final T zero = s.getA().getField().getZero();
        if (firing) {

            // compute thrust acceleration in inertial frame
            adder.addAcceleration(new FieldVector3D<>(s.getMass().reciprocal().multiply(thrust),
                                                      s.getAttitude().getRotation().applyInverseTo(direction)),
                                  s.getFrame());

            // compute flow rate
            adder.addMassDerivative(zero.add(flowRate));

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
            return new FieldVector3D<>(mass.reciprocal().multiply(thrust), rotation.applyInverseTo(direction));
        } else {
            // constant (and null) acceleration when not firing
            return FieldVector3D.getZero(mass.getField());
        }
    }

    /** {@inheritDoc} */
    public FieldVector3D<DerivativeStructure> accelerationDerivatives(final SpacecraftState s, final String paramName)
        throws OrekitException {

        complainIfNotSupported(paramName);

        if (firing) {

            if (thrustDriver.getName().equals(paramName)) {
                final DerivativeStructure thrustDS = factory.variable(0, thrust);
                return new FieldVector3D<>(thrustDS.divide(s.getMass()),
                                           s.getAttitude().getRotation().applyInverseTo(direction));
            } else if (flowRateDriver.getName().equals(paramName)) {
                // parameter is flow rate
                // acceleration does not depend on flow rate (only mass decrease does)
                return FieldVector3D.getZero(factory.getDerivativeField());
            } else {
                throw new OrekitException(OrekitMessages.UNSUPPORTED_PARAMETER_NAME, paramName,
                                          thrustDriver.getName() + ", " +
                                          flowRateDriver.getName());
            }

        } else {
            // constant (and null) acceleration when not firing
            return FieldVector3D.getZero(factory.getDerivativeField());
        }

    }

    /** {@inheritDoc} */
    public Stream<EventDetector> getEventsDetectors() {
        // in forward propagation direction, firing must be enabled
        // at start time and disabled at end time; in backward
        // propagation direction, firing must be enabled
        // at end time and disabled at start time
        final DateDetector startDetector = new DateDetector(startDate).
            withHandler((SpacecraftState state, DateDetector d, boolean increasing) -> {
                firing = d.isForward();
                return EventHandler.Action.RESET_DERIVATIVES;
            });
        final DateDetector endDetector = new DateDetector(endDate).
            withHandler((SpacecraftState state, DateDetector d, boolean increasing) -> {
                firing = !d.isForward();
                return EventHandler.Action.RESET_DERIVATIVES;
            });
        return Stream.of(startDetector, endDetector);
    }

    /** {@inheritDoc} */
    public ParameterDriver[] getParametersDrivers() {
        return new ParameterDriver[] {
            thrustDriver, flowRateDriver
        };
    }

    @Override
    public <T extends RealFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventsDetectors(final Field<T> field) {
        // in forward propagation direction, firing must be enabled
        // at start time and disabled at end time; in backward
        // propagation direction, firing must be enabled
        // at end time and disabled at start time
        final FieldDateDetector<T> startDetector = new FieldDateDetector<>(new FieldAbsoluteDate<>(field, startDate)).
            withHandler((FieldSpacecraftState<T> state, FieldDateDetector<T> d, boolean increasing) -> {
                firing = d.isForward();
                return FieldEventHandler.Action.RESET_DERIVATIVES;
            });
        final FieldDateDetector<T> endDetector = new FieldDateDetector<>(new FieldAbsoluteDate<>(field, endDate)).
            withHandler((FieldSpacecraftState<T> state, FieldDateDetector<T> d, boolean increasing) -> {
                firing = !d.isForward();
                return FieldEventHandler.Action.RESET_DERIVATIVES;
            });
        return Stream.of(startDetector, endDetector);
    }

}
