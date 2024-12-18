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
import org.orekit.control.indirect.adjoint.CartesianAdjointDerivativesProvider;
import org.orekit.propagation.events.EventDetectorsProvider;

/**
 * Interface to definite cost function in the frame of Pontryagin's Maximum Principle using Cartesian coordinates.
 * It provides the link between the optimal control and the adjoint variables. This relationship is obtained by maximizing the Hamiltonian.
 * The choice of control vector impacts on it.
 * Both standard (double type) and (Calculus)Field versions are to be implemented by inheritors.
 * @author Romain Serra
 * @see CartesianAdjointDerivativesProvider
 * @since 12.2
 */
public interface CartesianCost extends EventDetectorsProvider {

    /** Getter for adjoint vector name.
     * @return adjoint vector name
     */
    String getAdjointName();

    /** Getter for adjoint vector dimension. Default is 7 (six for Cartesian coordinates and one for mass).
     * @return adjoint dimension
     */
    default int getAdjointDimension() {
        return 7;
    }

    /** Getter for mass flow rate factor. It is negated and multiplied by the thrust force magnitude to obtain the mass time derivative.
     * The fact that it is a constant means that the exhaust speed is assumed to be independent of time.
     * @return mass flow rate factor
     */
    double getMassFlowRateFactor();

    /**
     * Computes the thrust acceleration vector in propagation frame from the adjoint variables and the mass.
     * @param adjointVariables adjoint vector
     * @param mass mass
     * @return thrust vector
     */
    Vector3D getThrustAccelerationVector(double[] adjointVariables, double mass);

    /**
     * Computes the thrust acceleration vector in propagation frame from the adjoint variables and the mass.
     * @param adjointVariables adjoint vector
     * @param mass mass
     * @param <T> field type
     * @return thrust vector
     */
    <T extends CalculusFieldElement<T>> FieldVector3D<T> getFieldThrustAccelerationVector(T[] adjointVariables, T mass);

    /**
     * Update the adjoint derivatives if necessary.
     *
     * @param adjointVariables   adjoint vector
     * @param mass               mass
     * @param adjointDerivatives derivatives to update
     */
    void updateAdjointDerivatives(double[] adjointVariables, double mass, double[] adjointDerivatives);

    /**
     * Update the adjoint derivatives if necessary.
     *
     * @param <T>                field type
     * @param adjointVariables   adjoint vector
     * @param mass               mass
     * @param adjointDerivatives derivatives to update
     */
    <T extends CalculusFieldElement<T>> void updateFieldAdjointDerivatives(T[] adjointVariables, T mass, T[] adjointDerivatives);

    /**
     * Computes the Hamiltonian contribution of the cost function.
     * @param adjointVariables adjoint vector
     * @param mass mass
     * @return contribution to Hamiltonian
     */
    double getHamiltonianContribution(double[] adjointVariables, double mass);

    /**
     * Computes the Hamiltonian contribution of the cost function.
     * @param adjointVariables adjoint vector
     * @param mass mass
     * @param <T> field type
     * @return contribution to Hamiltonian
     */
    <T extends CalculusFieldElement<T>> T getFieldHamiltonianContribution(T[] adjointVariables, T mass);
}
