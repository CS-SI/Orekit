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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.orekit.control.indirect.adjoint.cost.FieldCartesianCost;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.integration.FieldAdditionalDerivativesProvider;
import org.orekit.propagation.integration.FieldCombinedDerivatives;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinates;

/**
 * Class defining the Field version of the adjoint dynamics for Cartesian coordinates, as defined in the Pontryagin Maximum Principle.
 * @author Romain Serra
 * @see FieldAdditionalDerivativesProvider
 * @see org.orekit.propagation.numerical.FieldNumericalPropagator
 * @see CartesianAdjointDerivativesProvider
 * @since 12.2
 */
public class FieldCartesianAdjointDerivativesProvider<T extends CalculusFieldElement<T>> implements FieldAdditionalDerivativesProvider<T> {

    /** Contributing terms to the adjoint equation. */
    private final CartesianAdjointEquationTerm[] adjointEquationTerms;

    /** Cost function. */
    private final FieldCartesianCost<T> cost;

    /**
     * Constructor.
     * @param cost cost function
     * @param adjointEquationTerms terms contributing to the adjoint equations
     */
    public FieldCartesianAdjointDerivativesProvider(final FieldCartesianCost<T> cost,
                                                    final CartesianAdjointEquationTerm... adjointEquationTerms) {
        this.cost = cost;
        this.adjointEquationTerms = adjointEquationTerms;
    }

    /**
     * Getter for the cost.
     * @return cost
     */
    public FieldCartesianCost<T> getCost() {
        return cost;
    }

    /** Getter for the name.
     * @return name */
    public String getName() {
        return cost.getAdjointName();
    }

    /** Getter for the dimension.
     * @return dimension
     */
    public int getDimension() {
        return cost.getAdjointDimension();
    }

    /** {@inheritDoc} */
    @Override
    public void init(final FieldSpacecraftState<T> initialState, final FieldAbsoluteDate<T> target) {
        FieldAdditionalDerivativesProvider.super.init(initialState, target);
        if (initialState.isOrbitDefined() && initialState.getOrbit().getType() != OrbitType.CARTESIAN) {
            throw new OrekitException(OrekitMessages.WRONG_COORDINATES_FOR_ADJOINT_EQUATION);
        }
    }

    /** {@inheritDoc} */
    @Override
    public FieldCombinedDerivatives<T> combinedDerivatives(final FieldSpacecraftState<T> state) {
        // pre-computations
        final T mass = state.getMass();
        final T[] adjointVariables = state.getAdditionalState(getName());
        final int adjointDimension = getDimension();
        final T[] additionalDerivatives = MathArrays.buildArray(mass.getField(), adjointDimension);
        final T[] cartesianVariablesAndMass = formCartesianAndMassVector(state);

        // mass flow rate and control acceleration
        final T[] mainDerivativesIncrements = MathArrays.buildArray(mass.getField(), 7);
        final FieldVector3D<T> thrustAccelerationVector = getCost().getFieldThrustAccelerationVector(adjointVariables, mass);
        mainDerivativesIncrements[3] = thrustAccelerationVector.getX();
        mainDerivativesIncrements[4] = thrustAccelerationVector.getY();
        mainDerivativesIncrements[5] = thrustAccelerationVector.getZ();
        final T thrustAccelerationNorm = thrustAccelerationVector.getNorm();
        if (thrustAccelerationVector.getNorm().getReal() != 0.) {
            final T thrustForceMagnitude = thrustAccelerationNorm.multiply(mass);
            mainDerivativesIncrements[6] = thrustForceMagnitude.multiply(getCost().getMassFlowRateFactor().negate());
        }

        // Cartesian position adjoint
        additionalDerivatives[3] = adjointVariables[0].negate();
        additionalDerivatives[4] = adjointVariables[1].negate();
        additionalDerivatives[5] = adjointVariables[2].negate();

        // Cartesian velocity adjoint
        final FieldAbsoluteDate<T> date = state.getDate();
        final Frame propagationFrame = state.getFrame();
        for (final CartesianAdjointEquationTerm equationTerm: adjointEquationTerms) {
            final T[] contribution = equationTerm.getFieldRatesContribution(date, cartesianVariablesAndMass, adjointVariables,
                    propagationFrame);
            for (int i = 0; i < FastMath.min(adjointDimension, contribution.length); i++) {
                additionalDerivatives[i] = additionalDerivatives[i].add(contribution[i]);
            }
        }

        // other
        getCost().updateFieldAdjointDerivatives(adjointVariables, mass, additionalDerivatives);

        return new FieldCombinedDerivatives<>(additionalDerivatives, mainDerivativesIncrements);
    }

    /**
     * Gather Cartesian variables and mass in same vector.
     * @param state propagation state
     * @return Cartesian variables and mass
     */
    private T[] formCartesianAndMassVector(final FieldSpacecraftState<T> state) {
        final T mass = state.getMass();
        final T[] cartesianVariablesAndMass = MathArrays.buildArray(mass.getField(), 7);
        final FieldPVCoordinates<T> pvCoordinates = state.getPVCoordinates();
        System.arraycopy(pvCoordinates.getPosition().toArray(), 0, cartesianVariablesAndMass, 0, 3);
        System.arraycopy(pvCoordinates.getVelocity().toArray(), 0, cartesianVariablesAndMass, 3, 3);
        cartesianVariablesAndMass[6] = mass;
        return cartesianVariablesAndMass;
    }

    /**
     * Evaluate the Hamiltonian from Pontryagin's Maximum Principle.
     * @param state state assumed to hold the adjoint variables
     * @return Hamiltonian
     */
    public T evaluateHamiltonian(final FieldSpacecraftState<T> state) {
        final T[] cartesianAndMassVector = formCartesianAndMassVector(state);
        final T[] adjointVariables = state.getAdditionalState(getName());
        T hamiltonian = adjointVariables[0].multiply(cartesianAndMassVector[3]).add(adjointVariables[1].multiply(cartesianAndMassVector[4]))
                .add(adjointVariables[2].multiply(cartesianAndMassVector[5]));
        final FieldAbsoluteDate<T> date = state.getDate();
        final Frame propagationFrame = state.getFrame();
        for (final CartesianAdjointEquationTerm adjointEquationTerm : adjointEquationTerms) {
            final T contribution = adjointEquationTerm.getFieldHamiltonianContribution(date, cartesianAndMassVector,
                adjointVariables, propagationFrame);
            hamiltonian = hamiltonian.add(contribution);
        }
        if (adjointVariables.length != 6) {
            final T mass = state.getMass();
            final T thrustAccelerationNorm = getCost().getFieldThrustAccelerationVector(adjointVariables, mass).getNorm();
            final T thrustForceNorm = thrustAccelerationNorm.multiply(mass);
            hamiltonian = hamiltonian.subtract(adjointVariables[6].multiply(getCost().getMassFlowRateFactor()).multiply(thrustForceNorm));
        }
        hamiltonian = hamiltonian.add(getCost().getFieldHamiltonianContribution(adjointVariables, state.getMass()));
        return hamiltonian;
    }
}
