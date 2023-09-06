/* Copyright 2002-2023 CS GROUP
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.forces.maneuvers.Control3DVectorCostType;
import org.orekit.time.AbsoluteDate;
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

    /** Generic constructor.
     * @param thrust thrust (N)
     * @param isp isp (s)
     * @param direction direction in spacecraft frame
     * @param control3DVectorCostType control cost type
     * @param name name of the maneuver
     * @since 12.0
     */
    public BasicConstantThrustPropulsionModel(final double thrust,
                                              final double isp,
                                              final Vector3D direction,
                                              final Control3DVectorCostType control3DVectorCostType,
                                              final String name) {
        super(thrust, isp, direction, control3DVectorCostType, name);
        this.direction = direction.normalize();

        final double initialFlowRate = super.getInitialFlowRate();

        // Build the parameter drivers, using maneuver name as prefix
        this.thrustDriver   = new ParameterDriver(name + THRUST, thrust, THRUST_SCALE,
                                                  0.0, Double.POSITIVE_INFINITY);
        this.flowRateDriver = new ParameterDriver(name + FLOW_RATE, initialFlowRate, FLOW_RATE_SCALE,
                                                  Double.NEGATIVE_INFINITY, 0.0 );
    }

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
        this(thrust, isp, direction, DEFAULT_CONTROL_3D_VECTOR_COST_TYPE, name);
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getThrustVector() {
        // Thrust vector does not depend on spacecraft state for a constant maneuver.
        // thrustDriver as only 1 value estimated over the whole time period
        // by construction thrustDriver has only 1 value estimated over the all period
        // that is why no argument is acceptable
        return direction.scalarMultiply(thrustDriver.getValue());
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getThrustVector(final AbsoluteDate date) {
        // Thrust vector does not depend on spacecraft state for a constant maneuver.
        return direction.scalarMultiply(thrustDriver.getValue(date));
    }

    /** {@inheritDoc} */
    @Override
    public double getFlowRate() {
        // Thrust vector does not depend on spacecraft state for a constant maneuver.
        // thrustDriver has only 1 value estimated over the whole time period
        // by construction thrustDriver has only 1 value estimated over the all period
        // that is why no argument is acceptable
        return flowRateDriver.getValue();
    }

    /** {@inheritDoc} */
    @Override
    public double getFlowRate(final AbsoluteDate date) {
        return flowRateDriver.getValue(date);
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.unmodifiableList(Arrays.asList(thrustDriver, flowRateDriver));
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
    public <T extends CalculusFieldElement<T>> FieldVector3D<T> getThrustVector(final T[] parameters) {
        return new FieldVector3D<>(parameters[0], direction);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> T getFlowRate(final T[] parameters) {
        return parameters[1];
    }
}
