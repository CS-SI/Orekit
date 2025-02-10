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
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.MathArrays;
import org.orekit.control.indirect.adjoint.CartesianAdjointDerivativesProvider;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.integration.FieldAdditionalDerivativesProvider;
import org.orekit.propagation.integration.FieldCombinedDerivatives;

import java.util.stream.Stream;

/**
 * Interface to definite cost function in the frame of Pontryagin's Maximum Principle using Cartesian coordinates.
 * It provides the link between the optimal control and the adjoint variables. This relationship is obtained by maximizing the Hamiltonian.
 * The choice of control vector impacts on it.
 * @author Romain Serra
 * @see CartesianAdjointDerivativesProvider
 * @since 13.0
 */
public interface FieldCartesianCost<T extends CalculusFieldElement<T>> {

    /** Getter for adjoint vector name.
     * @return adjoint vector name
     */
    String getAdjointName();

    /** Getter for adjoint vector dimension.
     * @return adjoint dimension
     */
    int getAdjointDimension();

    /** Getter for mass flow rate factor. It is negated and multiplied by the thrust force magnitude to obtain the mass time derivative.
     * The fact that it is a constant means that the exhaust speed is assumed to be independent of time.
     * @return mass flow rate factor
     */
    T getMassFlowRateFactor();

    /**
     * Computes the thrust acceleration vector in propagation frame from the adjoint variables and the mass.
     * @param adjointVariables adjoint vector
     * @param mass mass
     * @return thrust vector
     */
    FieldVector3D<T> getFieldThrustAccelerationVector(T[] adjointVariables, T mass);

    /**
     * Update the adjoint derivatives if necessary.
     *
     * @param adjointVariables   adjoint vector
     * @param mass               mass
     * @param adjointDerivatives derivatives to update
     */
    void updateFieldAdjointDerivatives(T[] adjointVariables, T mass, T[] adjointDerivatives);

    /**
     * Computes the Hamiltonian contribution to the cost function.
     * It equals the Lagrange-form integrand multiplied by -1.
     * @param adjointVariables adjoint vector
     * @param mass mass
     * @return contribution to Hamiltonian
     */
    T getFieldHamiltonianContribution(T[] adjointVariables, T mass);

    /**
     * Get the detectors needed for propagation.
     * @param field field
     * @return event detectors
     */
    default Stream<FieldEventDetector<T>> getFieldEventDetectors(final Field<T> field) {
        return Stream.of();
    }

    /**
     * Get the derivatives provider to be able to integrate the cost function.
     * @param name name of cost as additional state variable
     * @return derivatives provider
     * @since 13.0
     */
    default FieldAdditionalDerivativesProvider<T> getCostDerivativeProvider(final String name) {
        return new FieldAdditionalDerivativesProvider<T>() {

            @Override
            public String getName() {
                return name;
            }

            @Override
            public int getDimension() {
                return 1;
            }

            @Override
            public boolean yields(final FieldSpacecraftState<T> state) {
                return !state.hasAdditionalData(getAdjointName());
            }

            @Override
            public FieldCombinedDerivatives<T> combinedDerivatives(final FieldSpacecraftState<T> s) {
                final T mass = s.getMass();
                final T[] derivatives = MathArrays.buildArray(mass.getField(), 1);
                final T[] adjoint = s.getAdditionalData(getAdjointName());
                final T hamiltonianContribution = getFieldHamiltonianContribution(adjoint, s.getMass());
                derivatives[0] = hamiltonianContribution.negate();
                return new FieldCombinedDerivatives<>(derivatives, null);
            }
        };
    }

    /**
     * Method returning equivalent in non-Field.
     * @return cost function for non-Field applications
     */
    CartesianCost toCartesianCost();
}
