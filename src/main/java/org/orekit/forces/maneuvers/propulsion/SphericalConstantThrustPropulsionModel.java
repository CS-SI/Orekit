/* Copyright 2022-2025 Romain Serra
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

package org.orekit.forces.maneuvers.propulsion;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.orekit.forces.maneuvers.Control3DVectorCostType;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Constant thrust propulsion model with:
 *  - Constant thrust direction in spacecraft frame
 *  - Parameter drivers (for estimation) for the thrust vector in spherical coordinates.
 * @author Romain Serra
 * @since 13.1
 * @see BasicConstantThrustPropulsionModel
 */
public class SphericalConstantThrustPropulsionModel extends AbstractConstantThrustPropulsionModel {

    /** Parameter name for thrust magnitude. */
    public static final String THRUST_MAGNITUDE = "thrust magnitude";

    /** Parameter name for thrust right ascension. */
    public static final String THRUST_RIGHT_ASCENSION = "thrust alpha";

    /** Parameter name for thrust declination. */
    public static final String THRUST_DECLINATION = "thrust declination";

    /** Thrust scaling factor.
     * <p>
     * We use a power of 2 to avoid numeric noise introduction
     * in the multiplications/divisions sequences.
     * </p>
     */
    public static final double THRUST_SCALE = FastMath.scalb(1.0, -5);

    /** Driver for thrust magnitude. */
    private final ParameterDriver thrustMagnitude;

    /** Driver for thrust right ascension in spacecraft frame. */
    private final ParameterDriver thrustRightAscension;

    /** Driver for thrust declination in spacecraft frame. */
    private final ParameterDriver thrustDeclination;

    /** Mass flow rate factor. */
    private final double massFlowRateFactor;

    /** Generic constructor.
     * @param isp isp (s)
     * @param thrustMagnitude thrust magnitude (N)
     * @param thrustDirection thrust direction in spacecraft frame
     * @param name name of the maneuver
     */
    public SphericalConstantThrustPropulsionModel(final double isp, final double thrustMagnitude,
                                                  final Vector3D thrustDirection, final String name) {
        super(isp, thrustMagnitude, thrustDirection, Control3DVectorCostType.TWO_NORM, name);
        this.massFlowRateFactor = -1. / ThrustPropulsionModel.getExhaustVelocity(isp);

        // Build the parameter drivers, using maneuver name as prefix
        this.thrustMagnitude   = new ParameterDriver(name + THRUST_MAGNITUDE, thrustMagnitude, THRUST_SCALE,
                0.0, Double.POSITIVE_INFINITY);
        this.thrustRightAscension   = new ParameterDriver(name + THRUST_RIGHT_ASCENSION, thrustDirection.getAlpha(), 1.,
                Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        this.thrustDeclination   = new ParameterDriver(name + THRUST_DECLINATION, thrustDirection.getDelta(), 1.,
                -MathUtils.SEMI_PI, MathUtils.SEMI_PI);
    }

    /** Constructor with thrust vector.
     * @param isp isp (s)
     * @param thrustVector thrust vector in spacecraft frame (N)
     * @param name name of the maneuver
     */
    public SphericalConstantThrustPropulsionModel(final double isp, final Vector3D thrustVector, final String name) {
        this(isp, thrustVector.getNorm(), thrustVector.normalize(), name);
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getThrustVector() {
        // Thrust vector does not depend on spacecraft state for a constant maneuver.
        // thrustDriver as only 1 value estimated over the whole time period
        // by construction thrustDriver has only 1 value estimated over the all period
        // that is why no argument is acceptable
        return new Vector3D(thrustRightAscension.getValue(), thrustDeclination.getValue()).scalarMultiply(thrustMagnitude.getValue());
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getThrustVector(final AbsoluteDate date) {
        // Thrust vector does not depend on spacecraft state for a constant maneuver.
        return new Vector3D(thrustRightAscension.getValue(date), thrustDeclination.getValue(date)).scalarMultiply(thrustMagnitude.getValue(date));
    }

    /** {@inheritDoc} */
    @Override
    public double getFlowRate() {
        // Thrust vector does not depend on spacecraft state for a constant maneuver.
        // thrustDriver has only 1 value estimated over the whole time period
        // by construction thrustDriver has only 1 value estimated over the all period
        // that is why no argument is acceptable
        return getThrustVector().getNorm() * massFlowRateFactor;
    }

    /** {@inheritDoc} */
    @Override
    public double getFlowRate(final AbsoluteDate date) {
        return getThrustVector(date).getNorm() * massFlowRateFactor;
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.unmodifiableList(Arrays.asList(thrustMagnitude, thrustRightAscension, thrustDeclination));
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getThrustVector(final double[] parameters) {
        // Thrust vector does not depend on spacecraft state for a constant maneuver.
        return new Vector3D(parameters[1], parameters[2]).scalarMultiply(parameters[0]);
    }

    /** {@inheritDoc} */
    @Override
    public double getFlowRate(final double[] parameters) {
        return getThrustVector(parameters).getNorm() * massFlowRateFactor;
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldVector3D<T> getThrustVector(final T[] parameters) {
        return new FieldVector3D<>(parameters[1], parameters[2]).scalarMultiply(parameters[0]);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> T getFlowRate(final T[] parameters) {
        return getThrustVector(parameters).getNorm().multiply(massFlowRateFactor);
    }
}
