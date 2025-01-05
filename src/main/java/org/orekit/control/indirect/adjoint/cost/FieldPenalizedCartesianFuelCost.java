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
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

/**
 * Abstract class for fuel cost with a penalty term proportional to a weight parameter epsilon.
 * This is typically used in a continuation method, starting from epsilon equal to 1
 * and going towards 0 where the fuel cost is recovered. The point is to enhance convergence.
 * The control vector is the normalized (by the upper bound on magnitude) thrust force in propagation frame.
 * See the following reference:
 * BERTRAND, Régis et EPENOY, Richard. New smoothing techniques for solving bang–bang optimal control problems—numerical results and statistical interpretation.
 * Optimal Control Applications and Methods, 2002, vol. 23, no 4, p. 171-197.
 *
 * @author Romain Serra
 * @since 13.0
 * @see FieldCartesianFuelCost
 * @see PenalizedCartesianFuelCost
 */
public abstract class FieldPenalizedCartesianFuelCost<T extends CalculusFieldElement<T>>
        extends FieldAbstractCartesianCost<T> {

    /** Maximum value of thrust force Euclidean norm. */
    private final T maximumThrustMagnitude;

    /** Penalty weight. */
    private final T epsilon;

    /**
     * Constructor.
     *
     * @param name               adjoint name
     * @param massFlowRateFactor mass flow rate factor
     * @param maximumThrustMagnitude maximum thrust magnitude
     * @param epsilon penalty weight
     */
    protected FieldPenalizedCartesianFuelCost(final String name, final T massFlowRateFactor,
                                              final T maximumThrustMagnitude, final T epsilon) {
        super(name, massFlowRateFactor);
        final double epsilonReal = epsilon.getReal();
        if (epsilonReal < 0 || epsilonReal > 1) {
            throw new OrekitException(OrekitMessages.INVALID_PARAMETER_RANGE, "epsilon", epsilonReal, 0, 1);
        }
        this.maximumThrustMagnitude = maximumThrustMagnitude;
        this.epsilon = epsilon;
    }

    /** Getter for the penalty weight epsilon.
     * @return epsilon
     */
    public T getEpsilon() {
        return epsilon;
    }

    /** Getter for maximum thrust magnitude.
     * @return maximum thrust
     */
    public T getMaximumThrustMagnitude() {
        return maximumThrustMagnitude;
    }

    /**
     * Evaluate the penalty term (without the weight), assumed to be a function of the control norm.
     * @param controlNorm Euclidean norm of control vector
     * @return penalty function
     */
    public abstract T evaluateFieldPenaltyFunction(T controlNorm);

    /**
     * Computes the direction of thrust.
     * @param adjointVariables adjoint vector
     * @return thrust direction
     */
    protected FieldVector3D<T> getFieldThrustDirection(final T[] adjointVariables) {
        return new FieldVector3D<>(adjointVariables[3], adjointVariables[4], adjointVariables[5]).normalize();
    }

    /** {@inheritDoc} */
    @Override
    public T getFieldHamiltonianContribution(final T[] adjointVariables, final T mass) {
        final FieldVector3D<T> thrustForce = getFieldThrustAccelerationVector(adjointVariables,
                mass).scalarMultiply(mass);
        final T controlNorm = thrustForce.getNorm().divide(getMaximumThrustMagnitude());
        return controlNorm.add(getEpsilon().multiply(evaluateFieldPenaltyFunction(controlNorm)));
    }

}
