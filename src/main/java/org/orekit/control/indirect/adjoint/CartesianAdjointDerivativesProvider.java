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
package org.orekit.control.indirect.adjoint;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.control.indirect.adjoint.cost.CartesianCost;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.AdditionalDerivativesProvider;
import org.orekit.propagation.integration.CombinedDerivatives;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

/**
 * Class defining the adjoint dynamics, as defined in the Pontryagin Maximum Principle, in the case where Cartesian coordinates in an inertial frame are the dependent variable.
 * The time derivatives of the adjoint variables are obtained by differentiating the so-called Hamiltonian.
 * They depend on the force model and the cost being minimized.
 * For the former, it is the user's responsibility to make sure the provided {@link CartesianAdjointEquationTerm} are consistent with the {@link org.orekit.forces.ForceModel}.
 * For the latter, the cost function is represented through the interface {@link CartesianCost}.
 * @author Romain Serra
 * @see AdditionalDerivativesProvider
 * @see org.orekit.propagation.numerical.NumericalPropagator
 * @since 12.2
 */
public class CartesianAdjointDerivativesProvider extends AbstractCartesianAdjointDerivativesProvider implements AdditionalDerivativesProvider {

    /** Contributing terms to the adjoint equation. */
    private final CartesianAdjointEquationTerm[] adjointEquationTerms;

    /**
     * Constructor.
     * @param name name of variables
     * @param cost cost function
     * @param adjointEquationTerms terms contributing to the adjoint equations. If none, then the propagator should have no forces, not even a Newtonian attraction.
     */
    public CartesianAdjointDerivativesProvider(final String name, final CartesianCost cost,
                                               final CartesianAdjointEquationTerm... adjointEquationTerms) {
        super(name, cost);
        this.adjointEquationTerms = adjointEquationTerms;
    }

    /** {@inheritDoc} */
    @Override
    public void init(final SpacecraftState initialState, final AbsoluteDate target) {
        AdditionalDerivativesProvider.super.init(initialState, target);
        if (initialState.isOrbitDefined() && initialState.getOrbit().getType() != OrbitType.CARTESIAN) {
            throw new OrekitException(OrekitMessages.WRONG_COORDINATES_FOR_ADJOINT_EQUATION);
        }
    }

    /** {@inheritDoc} */
    @Override
    public CombinedDerivatives combinedDerivatives(final SpacecraftState state) {
        // pre-computations
        final double[] adjointVariables = state.getAdditionalState(getName());
        final int adjointDimension = getDimension();
        final double[] additionalDerivatives = new double[adjointDimension];
        final double[] cartesianVariablesAndMass = new double[7];
        final PVCoordinates pvCoordinates = state.getPVCoordinates();
        System.arraycopy(pvCoordinates.getPosition().toArray(), 0, cartesianVariablesAndMass, 0, 3);
        System.arraycopy(pvCoordinates.getVelocity().toArray(), 0, cartesianVariablesAndMass, 3, 3);
        final double mass = state.getMass();
        cartesianVariablesAndMass[6] = mass;

        // mass flow rate and control acceleration
        final double[] mainDerivativesIncrements = new double[7];
        final Vector3D thrustVector = getCost().getThrustVector(adjointVariables, mass);
        mainDerivativesIncrements[3] = thrustVector.getX() / mass;
        mainDerivativesIncrements[4] = thrustVector.getY() / mass;
        mainDerivativesIncrements[5] = thrustVector.getZ() / mass;
        mainDerivativesIncrements[6] = -getCost().getMassFlowRateFactor();

        // Cartesian position adjoint
        additionalDerivatives[3] = -adjointVariables[0];
        additionalDerivatives[4] = -adjointVariables[1];
        additionalDerivatives[5] = -adjointVariables[2];

        // Cartesian velocity adjoint
        final AbsoluteDate date = state.getDate();
        for (final CartesianAdjointEquationTerm equationTerm: adjointEquationTerms) {
            final double[] contribution = equationTerm.getVelocityAdjointContribution(date, cartesianVariablesAndMass, adjointVariables);
            additionalDerivatives[0] += contribution[0];
            additionalDerivatives[1] += contribution[1];
            additionalDerivatives[2] += contribution[2];
        }

        // other
        getCost().updateAdjointDerivatives(adjointVariables, additionalDerivatives);

        return new CombinedDerivatives(additionalDerivatives, mainDerivativesIncrements);
    }
}
