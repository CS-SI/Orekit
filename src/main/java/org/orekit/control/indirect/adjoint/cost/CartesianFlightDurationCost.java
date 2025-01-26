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
package org.orekit.control.indirect.adjoint.cost;


import org.hipparchus.geometry.euclidean.threed.Vector3D;

/**
 * Class for minimizing the flight duration (a.k.a. time of flight) with Cartesian coordinates.
 * It is the integral over time of the constant one. The control is assumed to be bounded.
 * It also assumes that no external acceleration depends on mass.
 * If the mass flow rate factor is zero, then there is no adjoint for the mass.
 *
 * @author Romain Serra
 * @see CartesianCost
 * @since 13.0
 */
public class CartesianFlightDurationCost extends AbstractCartesianCost {

    /**
     * Maximum value of thrust force Euclidean norm.
     */
    private final double maximumThrustMagnitude;

    /**
     * Constructor.
     *
     * @param name                   name
     * @param massFlowRateFactor     mass flow rate factor
     * @param maximumThrustMagnitude maximum thrust magnitude
     */
    public CartesianFlightDurationCost(final String name, final double massFlowRateFactor,
                                       final double maximumThrustMagnitude) {
        super(name, massFlowRateFactor);
        this.maximumThrustMagnitude = maximumThrustMagnitude;
    }

    /**
     * Getter for maximum thrust magnitude.
     *
     * @return maximum thrust
     */
    public double getMaximumThrustMagnitude() {
        return maximumThrustMagnitude;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Vector3D getThrustAccelerationVector(final double[] adjointVariables, final double mass) {
        return new Vector3D(adjointVariables[3], adjointVariables[4], adjointVariables[5]).normalize()
                .scalarMultiply(maximumThrustMagnitude);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateAdjointDerivatives(final double[] adjointVariables, final double mass,
                                         final double[] adjointDerivatives) {
        if (getAdjointDimension() > 6) {
            adjointDerivatives[6] += getAdjointVelocityNorm(adjointVariables) * maximumThrustMagnitude / (mass * mass);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getHamiltonianContribution(final double[] adjointVariables, final double mass) {
        return -1.;
    }
}
