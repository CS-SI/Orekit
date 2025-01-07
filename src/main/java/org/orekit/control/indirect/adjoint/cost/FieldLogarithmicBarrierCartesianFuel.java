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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.FastMath;

/**
 * Fuel cost penalized with a logarithmic term, which is a barrier so is not defined for epsilon equal to 0 or 1.
 *
 * @author Romain Serra
 * @since 13.0
 */
public class FieldLogarithmicBarrierCartesianFuel<T extends CalculusFieldElement<T>>
        extends FieldPenalizedCartesianFuelCost<T> {

    /**
     * Constructor.
     *
     * @param name                   adjoint name
     * @param massFlowRateFactor     mass flow rate factor
     * @param maximumThrustMagnitude maximum thrust magnitude
     * @param epsilon                penalty weight
     */
    public FieldLogarithmicBarrierCartesianFuel(final String name, final T massFlowRateFactor,
                                                final T maximumThrustMagnitude, final T epsilon) {
        super(name, massFlowRateFactor, maximumThrustMagnitude, epsilon);
    }

    /** {@inheritDoc} */
    @Override
    public T evaluateFieldPenaltyFunction(final T controlNorm) {
        return FastMath.log(controlNorm).add(FastMath.log(controlNorm.negate().add(1)));
    }

    /** {@inheritDoc} */
    @Override
    public FieldVector3D<T> getFieldThrustAccelerationVector(final T[] adjointVariables, final T mass) {
        final T thrustForceMagnitude = getThrustForceMagnitude(adjointVariables, mass);
        return getFieldThrustDirection(adjointVariables).scalarMultiply(thrustForceMagnitude.divide(mass));
    }

    /** {@inheritDoc} */
    @Override
    public void updateFieldAdjointDerivatives(final T[] adjointVariables, final T mass, final T[] adjointDerivatives) {
        adjointDerivatives[6] = adjointDerivatives[6].add(getFieldAdjointVelocityNorm(adjointVariables)
                .multiply(getThrustForceMagnitude(adjointVariables, mass)).divide(mass.square()));
    }

    /**
     * Computes the Euclidean norm of the thrust force.
     * @param adjointVariables adjoint variables
     * @param mass mass
     * @return thrust force magnitude
     */
    private T getThrustForceMagnitude(final T[] adjointVariables, final T mass) {
        final T twoEpsilon = getEpsilon().multiply(2);
        final T otherTerm = getFieldAdjointVelocityNorm(adjointVariables).divide(mass).subtract(getMassFlowRateFactor()
                .multiply(adjointVariables[6])).subtract(1);
        return twoEpsilon.multiply(getMaximumThrustMagnitude())
                .divide(twoEpsilon.add(otherTerm).add(FastMath.sqrt(otherTerm.square().add(twoEpsilon.square()))));
    }

    /** {@inheritDoc} */
    @Override
    public LogarithmicBarrierCartesianFuel toCartesianCost() {
        return new LogarithmicBarrierCartesianFuel(getAdjointName(), getMassFlowRateFactor().getReal(),
                getMaximumThrustMagnitude().getReal(), getEpsilon().getReal());
    }
}
