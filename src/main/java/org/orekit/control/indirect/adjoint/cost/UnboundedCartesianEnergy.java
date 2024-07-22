/* Copyright 2022-2024 Romain Serra
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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;

/**
 * Class for unbounded energy cost with Cartesian coordinates.
 * Here, the control vector is chosen as the acceleration given by thrusting, expressed in the propagation frame.
 * This leads to the optimal thrust being in the same direction than the adjoint velocity.
 * @author Romain Serra
 * @see UnboundedCartesianEnergyNeglectingMass
 * @since 12.2
 */
public class UnboundedCartesianEnergy extends AbstractUnboundedCartesianEnergy {

    /**
     * Constructor.
     * @param massFlowRateFactor mass flow rate factor (must be non-negative)
     */
    public UnboundedCartesianEnergy(final double massFlowRateFactor) {
        super(massFlowRateFactor);
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getThrustVector(final double[] adjointVariables, final double mass) {
        final double norm = getAdjointVelocityNorm(adjointVariables);
        final double factor = mass * (1. - getMassFlowRateFactor() * adjointVariables[6] / norm);
        return new Vector3D(adjointVariables[3], adjointVariables[4], adjointVariables[5]).scalarMultiply(factor);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldVector3D<T> getThrustVector(final T[] adjointVariables, final T mass) {
        final T norm = getAdjointVelocityNorm(adjointVariables);
        final T factor = mass.multiply(adjointVariables[6].multiply(-getMassFlowRateFactor()).divide(norm).add(1));
        return new FieldVector3D<>(adjointVariables[3], adjointVariables[4], adjointVariables[5]).scalarMultiply(factor);
    }

    /** {@inheritDoc} */
    @Override
    public void updateAdjointDerivatives(final double[] adjointVariables, final double mass, final double[] adjointDerivatives) {
        final double adjointVelocityNorm = getAdjointVelocityNorm(adjointVariables);
        final double factor = getMassFlowRateFactor() * adjointVariables[6];
        adjointDerivatives[6] = factor * (mass * factor - adjointVelocityNorm);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> void updateAdjointDerivatives(final T[] adjointVariables, final T mass, final T[] adjointDerivatives) {
        final T adjointVelocityNorm = getAdjointVelocityNorm(adjointVariables);
        final T factor = adjointVariables[6].multiply(getMassFlowRateFactor());
        adjointDerivatives[6] = factor.multiply(mass.multiply(factor).subtract(adjointVelocityNorm));
    }
}
