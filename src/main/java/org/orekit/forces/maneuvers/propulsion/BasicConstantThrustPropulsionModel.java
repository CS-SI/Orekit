/* Copyright 2002-2021 CS GROUP
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.utils.Constants;
import org.orekit.utils.ParameterDriver;

/** Constant thrust propulsion model with:
 *  - Constant thrust direction in spacecraft frame
 *  - Parameter drivers (for estimation) for the thrust norm or the flow rate.
 * Note that both parameters CANNOT be selected at the same time since they depend on one another.
 * @author Maxime Journot
 * @since 10.2
 */
public class BasicConstantThrustPropulsionModel extends AbstractConstantThrustPropulsionModel {

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
    public static final double THRUST_SCALE = FastMath.scalb(1.0, -5);

    /** Flow rate scaling factor.
     * <p>
     * We use a power of 2 to avoid numeric noise introduction
     * in the multiplications/divisions sequences.
     * </p>
     */
    public static final double FLOW_RATE_SCALE = FastMath.scalb(1.0, -12);

    /** Driver for thrust parameter. */
    private final ParameterDriver thrustDriver;

    /** Driver for flow rate parameter. */
    private final ParameterDriver flowRateDriver;

    /** Thrust direction in spacecraft frame. */
    private final Vector3D direction;

    /** Simple constructor.
     * @param thrust thrust (N)
     * @param isp isp (s)
     * @param direction direction in spacecraft frame
     * @param name name of the maneuver
     */
    public BasicConstantThrustPropulsionModel(final double thrust,
                                              final double isp,
                                              final Vector3D direction,
                                              final String name) {
        super(thrust, isp, direction, name);
        this.direction = direction.normalize();

        final double initialFlowRate = -thrust / (Constants.G0_STANDARD_GRAVITY * isp);

        // Build the parameter drivers, using maneuver name as prefix
        this.thrustDriver   = new ParameterDriver(name + THRUST, thrust, THRUST_SCALE,
                                                  0.0, Double.POSITIVE_INFINITY);
        this.flowRateDriver = new ParameterDriver(name + FLOW_RATE, initialFlowRate, FLOW_RATE_SCALE,
                                                  Double.NEGATIVE_INFINITY, 0.0 );
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getThrustVector() {
        // Thrust vector does not depend on spacecraft state for a constant maneuver.
        return direction.scalarMultiply(thrustDriver.getValue());
    }

    /** {@inheritDoc} */
    @Override
    public double getFlowRate() {
        // Flow rate does not depend on spacecraft state for a constant maneuver.
        return flowRateDriver.getValue();
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        final List<ParameterDriver> drivers = new ArrayList<>(2);
        drivers.add(thrustDriver);
        drivers.add(flowRateDriver);
        return Collections.unmodifiableList(drivers);
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getThrustVector(final double[] parameters) {
        // Thrust vector does not depend on spacecraft state for a constant maneuver.
        return direction.scalarMultiply(parameters[0]);
    }

    /** {@inheritDoc} */
    @Override
    public double getFlowRate(final double[] parameters) {
        return parameters[1];
    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> FieldVector3D<T> getThrustVector(final T[] parameters) {
        return new FieldVector3D<T>(parameters[0], direction);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> T getFlowRate(final T[] parameters) {
        return parameters[1];
    }
}
