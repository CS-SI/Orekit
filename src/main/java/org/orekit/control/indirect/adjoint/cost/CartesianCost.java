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
 * Interface to definite cost function in indirect control using Cartesian coordinates.
 * It provides the link between the optimal control and the adjoint variables. This relationship is obtained by maximizing the Hamiltonian.
 * Both standard (double type) and (Calculus)Field versions are to be implemented by inheritors.
 * @author Romain Serra
 * @see CartesianAdjointDerivativesProvider
 * @since 12.2
 */
public interface CartesianCost extends EventDetectorsProvider {

    /** Getter for adjoint vector dimension. Default is 7 (six for Cartesian coordinates and one for mass).
     * @return adjoint dimension
     */
    default int getAdjointDimension() {
        return 7;
    }

    /** Getter for mass flow rate factor. It is multiplied by the thrust magnitude to obtain the mass time derivative.
     * @return mass flow rate factor
     */
    double getMassFlowRateFactor();

    /**
     * Computes the thrust vector in propagation frame from the adjoint variables and the mass.
     * @param adjointVariables adjoint vector
     * @param mass mass
     * @return thrust vector
     */
    Vector3D getThrustVector(double[] adjointVariables, double mass);

    /**
     * Computes the thrust vector in propagation frame from the adjoint variables and the mass.
     * @param adjointVariables adjoint vector
     * @param mass mass
     * @param <T> field type
     * @return thrust vector
     */
    <T extends CalculusFieldElement<T>> FieldVector3D<T> getThrustVector(T[] adjointVariables, T mass);

    /**
     * Update the adjoint derivatives if necessary.
     * @param adjointVariables adjoint vector
     * @param adjointDerivatives derivatives to update
     */
    void updateAdjointDerivatives(double[] adjointVariables, double[] adjointDerivatives);

    /**
     * Update the adjoint derivatives if necessary.
     * @param <T> field type
     * @param adjointVariables adjoint vector
     * @param adjointDerivatives derivatives to update
     */
    <T extends CalculusFieldElement<T>> void updateAdjointDerivatives(T[] adjointVariables, T[] adjointDerivatives);

}
