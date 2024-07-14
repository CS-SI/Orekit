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
package org.orekit.control.indirect;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.MathArrays;
import org.orekit.control.indirect.adjoint.CartesianAdjointEquationTerm;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.integration.AdditionalDerivativesProvider;
import org.orekit.propagation.integration.FieldAdditionalDerivativesProvider;
import org.orekit.propagation.integration.FieldCombinedDerivatives;
import org.orekit.utils.FieldPVCoordinates;

/**
 * Class defining the Field version of the adjoint dynamics for Cartesian coordinates, as defined in the Pontryagin Maximum Principle.
 * @author Romain Serra
 * @see AdditionalDerivativesProvider
 * @see org.orekit.propagation.numerical.NumericalPropagator
 * @since 12.2
 */
public class FieldCartesianAdjointDerivativesProvider<T extends CalculusFieldElement<T>> implements FieldAdditionalDerivativesProvider<T> {

    /** Name of the additional variables. */
    private final String name;

    /** Cost function. */
    private final CartesianCost cost;

    /** Contributing terms to the adjoint equation. */
    private final CartesianAdjointEquationTerm[] adjointEquationTerms;

    /**
     * Constructor.
     * @param name name of variables
     * @param cost cost function
     * @param adjointEquationTerms terms contributing to the adjoint equations
     */
    public FieldCartesianAdjointDerivativesProvider(final String name, final CartesianCost cost,
                                                    final CartesianAdjointEquationTerm... adjointEquationTerms) {
        this.name = name;
        this.cost = cost;
        this.adjointEquationTerms = adjointEquationTerms;
    }

    /**
     * Getter for cost.
     * @return cost
     */
    public CartesianCost getCost() {
        return cost;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return name;
    }

    /** {@inheritDoc} */
    @Override
    public int getDimension() {
        return cost.getAdjointDimension();
    }

    /** {@inheritDoc} */
    @Override
    public FieldCombinedDerivatives<T> combinedDerivatives(final FieldSpacecraftState<T> state) {
        // pre-computations
        final T mass = state.getMass();
        final T[] adjointVariables = state.getAdditionalState(getName());
        final int adjointDimension = getDimension();
        final T[] additionalDerivatives = MathArrays.buildArray(mass.getField(), adjointDimension);
        final T[] cartesianVariablesAndMass = MathArrays.buildArray(mass.getField(), 7);
        final FieldPVCoordinates<T> pvCoordinates = state.getPVCoordinates();
        System.arraycopy(pvCoordinates.getPosition().toArray(), 0, cartesianVariablesAndMass, 0, 3);
        System.arraycopy(pvCoordinates.getVelocity().toArray(), 0, cartesianVariablesAndMass, 3, 3);
        cartesianVariablesAndMass[6] = mass;

        // mass flow rate and control acceleration
        final T[] mainDerivativesIncrements = MathArrays.buildArray(mass.getField(), 7);
        final FieldVector3D<T> thrustVector = cost.getThrustVector(adjointVariables, mass);
        mainDerivativesIncrements[3] = thrustVector.getX().divide(mass);
        mainDerivativesIncrements[4] = thrustVector.getY().divide(mass);
        mainDerivativesIncrements[5] = thrustVector.getZ().divide(mass);
        mainDerivativesIncrements[6] = mass.newInstance(-cost.getMassFlowRate());

        // Cartesian position adjoint
        additionalDerivatives[3] = additionalDerivatives[0].negate();
        additionalDerivatives[4] = additionalDerivatives[1].negate();
        additionalDerivatives[5] = additionalDerivatives[2].negate();

        // Cartesian velocity adjoint
        for (final CartesianAdjointEquationTerm equationTerm: adjointEquationTerms) {
            final T[] contribution = equationTerm.getVelocityAdjointContribution(cartesianVariablesAndMass, adjointVariables);
            additionalDerivatives[0] = additionalDerivatives[0].add(contribution[0]);
            additionalDerivatives[1] = additionalDerivatives[1].add(contribution[1]);
            additionalDerivatives[2] = additionalDerivatives[2].add(contribution[2]);
        }

        // other
        cost.updateAdjointDerivatives(adjointVariables, additionalDerivatives);

        return new FieldCombinedDerivatives<>(additionalDerivatives, mainDerivativesIncrements);
    }
}
