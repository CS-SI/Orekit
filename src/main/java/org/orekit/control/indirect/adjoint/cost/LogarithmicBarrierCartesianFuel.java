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
import org.hipparchus.util.FastMath;

/**
 * Fuel cost penalized with a logarithmic term, which is a barrier so is not defined for epsilon equal to 0 or 1.
 *
 * @author Romain Serra
 * @since 13.0
 */
public class LogarithmicBarrierCartesianFuel extends PenalizedCartesianFuelCost {

    /**
     * Constructor.
     *
     * @param name                   adjoint name
     * @param massFlowRateFactor     mass flow rate factor
     * @param maximumThrustMagnitude maximum thrust magnitude
     * @param epsilon                penalty weight
     */
    public LogarithmicBarrierCartesianFuel(final String name, final double massFlowRateFactor,
                                           final double maximumThrustMagnitude, final double epsilon) {
        super(name, massFlowRateFactor, maximumThrustMagnitude, epsilon);
    }

    /** {@inheritDoc} */
    @Override
    public double evaluatePenaltyFunction(final double controlNorm) {
        return FastMath.log(controlNorm) + FastMath.log(1. - controlNorm);
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getThrustAccelerationVector(final double[] adjointVariables, final double mass) {
        final double thrustForceMagnitude = getThrustForceMagnitude(adjointVariables, mass);
        return getThrustDirection(adjointVariables).scalarMultiply(thrustForceMagnitude / mass);
    }

    /** {@inheritDoc} */
    @Override
    public void updateAdjointDerivatives(final double[] adjointVariables, final double mass,
                                         final double[] adjointDerivatives) {
        adjointDerivatives[6] += getAdjointVelocityNorm(adjointVariables) * getThrustForceMagnitude(adjointVariables,
                mass) / (mass * mass);
    }

    /**
     * Computes the Euclidean norm of the thrust force.
     * @param adjointVariables adjoint variables
     * @param mass mass
     * @return thrust force magnitude
     */
    private double getThrustForceMagnitude(final double[] adjointVariables, final double mass) {
        final double twoEpsilon = getEpsilon() * 2;
        final double otherTerm = getAdjointVelocityNorm(adjointVariables) / mass - getMassFlowRateFactor() * adjointVariables[6] - 1;
        return twoEpsilon * getMaximumThrustMagnitude() / (twoEpsilon + otherTerm + FastMath.sqrt(otherTerm * otherTerm + twoEpsilon * twoEpsilon));
    }
}
