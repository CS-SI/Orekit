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
package org.orekit.forces.maneuvers;

import java.util.stream.Stream;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.events.Action;
import org.hipparchus.util.FastMath;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.forces.AbstractForceModel;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldDateDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.numerical.FieldTimeDerivativesEquations;
import org.orekit.propagation.numerical.TimeDerivativesEquations;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.ParameterDriver;

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

    /** Nominal start of the maneuver. */
    private final AbsoluteDate startDate;

    /** Nominal end of the maneuver. */
    private final AbsoluteDate endDate;

    /** The attitude to override during the maneuver, if set. */
    private final AttitudeProvider attitudeOverride;

    /** Direction of the acceleration in satellite frame. */
    private final Vector3D direction;

    /** User-defined name of the maneuver.
     * This String attribute is empty by default.
     * It is added as a prefix to the parameter drivers of the maneuver.
     * The purpose is to differentiate between drivers in the case where several maneuvers
     * were added to a propagator force model.
     * Additionally, the user can retrieve the whole maneuver by looping on the force models of a propagator,
     * scanning for its name.
     * @since 9.2
     */
    private final String name;

    /** Triggered date of engine start. */
    private AbsoluteDate triggeredStart;

    /** Triggered date of engine stop. */
    private AbsoluteDate triggeredEnd;

    /** Propagation direction. */
    private boolean forward;

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
     * @param attitudeOverride the attitude provider to use for the maneuver, or
     * null if the attitude from the propagator should be used
     * @param direction the acceleration direction in satellite frame.
     * @since 9.2
     */
    public ConstantThrustManeuver(final AbsoluteDate date, final double duration,
                                  final double thrust, final double isp,
                                  final AttitudeProvider attitudeOverride, final Vector3D direction) {
        this(date, duration, thrust, isp, attitudeOverride, direction, "");
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
     * @param name name of the maneuver, used as a prefix for the {@link #getParametersDrivers() parameters drivers}
     * @since 9.0
     */
    public ConstantThrustManeuver(final AbsoluteDate date, final double duration,
                                  final double thrust, final double isp,
                                  final Vector3D direction,
                                  final String name) {
        this(date, duration, thrust, isp, null, direction, name);
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
     * @param attitudeOverride the attitude provider to use for the maneuver, or
     * null if the attitude from the propagator should be used
     * @param direction the acceleration direction in satellite frame
     * @param name name of the maneuver, used as a prefix for the {@link #getParametersDrivers() parameters drivers}
     * @since 9.2
     */
    public ConstantThrustManeuver(final AbsoluteDate date, final double duration,
                                  final double thrust, final double isp,
                                  final AttitudeProvider attitudeOverride, final Vector3D direction,
                                  final String name) {

        if (duration >= 0) {
            this.startDate = date;
            this.endDate   = date.shiftedBy(duration);
        } else {
            this.endDate   = date;
            this.startDate = endDate.shiftedBy(duration);
        }

        final double flowRate  = -thrust / (Constants.G0_STANDARD_GRAVITY * isp);
        this.attitudeOverride  = attitudeOverride;
        this.direction         = direction.normalize();
        this.name              = name;
        this.triggeredStart    = null;
        this.triggeredEnd      = null;

        // Build the parameter drivers, using maneuver name as prefix
        ParameterDriver tpd = null;
        ParameterDriver fpd = null;
        try {
            tpd = new ParameterDriver(name + THRUST, thrust, THRUST_SCALE,
                                      0.0, Double.POSITIVE_INFINITY);
            fpd = new ParameterDriver(name + FLOW_RATE, flowRate, FLOW_RATE_SCALE,
                                      Double.NEGATIVE_INFINITY, 0.0 );
        } catch (OrekitException oe) {
            // this should never occur as valueChanged above never throws an exception
            throw new OrekitInternalError(oe);
        }

        this.thrustDriver   = tpd;
        this.flowRateDriver = fpd;

    }

    /** {@inheritDoc} */
    @Override
    public boolean dependsOnPositionOnly() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
        // set the initial value of firing
        final AbsoluteDate sDate = s0.getDate();
        forward                 = sDate.compareTo(t) < 0;
        final boolean isBetween = sDate.isBetween(startDate, endDate);
        final boolean isOnStart = startDate.compareTo(sDate) == 0;
        final boolean isOnEnd = endDate.compareTo(sDate) == 0;

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

    /** Get the thrust.
     * @return thrust force (N).
     */
    public double getThrust() {
        return thrustDriver.getValue();
    }

    /** Get the specific impulse.
     * @return specific impulse (s).
     */
    public double getISP() {
        final double thrust   = getThrust();
        final double flowRate = getFlowRate();
        return -thrust / (Constants.G0_STANDARD_GRAVITY * flowRate);
    }

    /** Get the flow rate.
     * @return flow rate (negative, kg/s).
     */
    public double getFlowRate() {
        return flowRateDriver.getValue();
    }

    /** Get the direction.
     * @return the direction
     * @since 9.2
     */
    public Vector3D getDirection() {
        return direction;
    }

    /** Get the name.
     * @return the name
     * @since 9.2
     */
    public String getName() {
        return name;
    }

    /** Get the start date.
     * @return the start date
     * @since 9.2
     */
    public AbsoluteDate getStartDate() {
        return startDate;
    }

    /** Get the end date.
     * @return the end date
     * @since 9.2
     */
    public AbsoluteDate getEndDate() {
        return endDate;
    }

    /** Get the duration of the maneuver (s).
     * duration = endDate - startDate
     * @return the duration of the maneuver (s)
     * @since 9.2
     */
    public double getDuration() {
        return endDate.durationFrom(startDate);
    }

    /** Get the attitude override used for the maneuver.
     * @return the attitude override
     * @since 9.2
     */
    public AttitudeProvider getAttitudeOverride() {
        return attitudeOverride;
    }

    /** {@inheritDoc} */
    @Override
    public void addContribution(final SpacecraftState s, final TimeDerivativesEquations adder) {
        if (isFiring(s)) {

            // compute thrust acceleration in inertial frame
            final double[] parameters = getParameters();
            adder.addNonKeplerianAcceleration(acceleration(s, parameters));

            // compute flow rate
            adder.addMassDerivative(parameters[1]);

        }
    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> void
        addContribution(final FieldSpacecraftState<T> s,
                        final FieldTimeDerivativesEquations<T> adder) {
        if (isFiring(s)) {

            final T[] parameters = getParameters(s.getDate().getField());

            // compute thrust acceleration in inertial frame
            adder.addNonKeplerianAcceleration(acceleration(s, parameters));

            // compute flow rate
            adder.addMassDerivative(parameters[1]);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D acceleration(final SpacecraftState state, final double[] parameters) {
        if (isFiring(state)) {
            final double thrust = parameters[0];
            final Attitude attitude =
                            attitudeOverride == null ?
                            state.getAttitude() :
                            attitudeOverride.getAttitude(state.getOrbit(),
                                                         state.getDate(),
                                                         state.getFrame());
            return new Vector3D(thrust / state.getMass(),
                                attitude.getRotation().applyInverseTo(direction));
        } else {
            return Vector3D.ZERO;
        }
    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> FieldVector3D<T> acceleration(final FieldSpacecraftState<T> s,
                                                                         final T[] parameters) {
        if (isFiring(s)) {
            // compute thrust acceleration in inertial frame
            final T thrust = parameters[0];
            final FieldAttitude<T> attitude =
                            attitudeOverride == null ?
                            s.getAttitude() :
                            attitudeOverride.getAttitude(s.getOrbit(),
                                                         s.getDate(),
                                                         s.getFrame());
            return new FieldVector3D<>(s.getMass().reciprocal().multiply(thrust),
                                       attitude.getRotation().applyInverseTo(direction));
        } else {
            // constant (and null) acceleration when not firing
            return FieldVector3D.getZero(s.getMass().getField());
        }
    }

    /** {@inheritDoc} */
    @Override
    public Stream<EventDetector> getEventsDetectors() {
        // in forward propagation direction, firing must be enabled
        // at start time and disabled at end time; in backward
        // propagation direction, firing must be enabled
        // at end time and disabled at start time
        final DateDetector startDetector = new DateDetector(startDate).
            withHandler((SpacecraftState state, DateDetector d, boolean increasing) -> {
                triggeredStart = state.getDate();
                return Action.RESET_DERIVATIVES;
            });
        final DateDetector endDetector = new DateDetector(endDate).
            withHandler((SpacecraftState state, DateDetector d, boolean increasing) -> {
                triggeredEnd = state.getDate();
                return Action.RESET_DERIVATIVES;
            });
        return Stream.of(startDetector, endDetector);
    }

    /** {@inheritDoc} */
    @Override
    public ParameterDriver[] getParametersDrivers() {
        return new ParameterDriver[] {
            thrustDriver, flowRateDriver
        };
    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventsDetectors(final Field<T> field) {
        // in forward propagation direction, firing must be enabled
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

    /** Check if maneuvering is on.
     * @param s current state
     * @return true if maneuver is on at this state
     * @since 10.1
     */
    public boolean isFiring(final SpacecraftState s) {
        return isFiring(s.getDate());
    }

    /** Check if maneuvering is on.
     * @param s current state
     * @param <T> type of the field elements
     * @return true if maneuver is on at this state
     * @since 10.1
     */
    public <T extends RealFieldElement<T>> boolean isFiring(final FieldSpacecraftState<T> s) {
        return isFiring(s.getDate().toAbsoluteDate());
    }

    /** Check if maneuvering is on.
     * @param date current date
     * @return true if maneuver is on at this date
     * @since 10.1
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
