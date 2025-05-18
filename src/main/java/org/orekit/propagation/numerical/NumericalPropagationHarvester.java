/* Copyright 2002-2025 CS GROUP
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
package org.orekit.propagation.numerical;

import java.util.List;

import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.AbstractMatricesHarvester;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.DoubleArrayDictionary;

/** Harvester between two-dimensional Jacobian matrices and one-dimensional {@link
 * SpacecraftState#getAdditionalState(String) additional state arrays}.
 * @author Luc Maisonobe
 * @since 11.1
 */
class NumericalPropagationHarvester extends AbstractMatricesHarvester {

    /** Propagator bound to this harvester. */
    private final NumericalPropagator propagator;

    /** Columns names for parameters. */
    private List<String> columnsNames;

    /** Simple constructor.
     * <p>
     * The arguments for initial matrices <em>must</em> be compatible with the {@link org.orekit.orbits.OrbitType orbit type}
     * and {@link PositionAngleType position angle} that will be used by propagator
     * </p>
     * <p>
     * If the initial matrix is 7x7, it means that the mass is considered as being a state variable.
     * </p>
     * @param propagator propagator bound to this harvester
     * @param stmName State Transition Matrix state name
     * @param initialStm initial State Transition Matrix ∂Y/∂Y₀,
     * if null (which is the most frequent case), assumed to be 6x6 identity
     * @param initialJacobianColumns initial columns of the Jacobians matrix with respect to parameters,
     * if null or if some selected parameters are missing from the dictionary, the corresponding
     * initial column is assumed to be 0
     */
    NumericalPropagationHarvester(final NumericalPropagator propagator, final String stmName,
                                  final RealMatrix initialStm, final DoubleArrayDictionary initialJacobianColumns) {
        super(stmName, initialStm, initialJacobianColumns);
        this.propagator   = propagator;
        this.columnsNames = null;
    }

    /** {@inheritDoc} */
    @Override
    protected double[][] getConversionJacobian(final SpacecraftState state) {

        final double[][] identity = super.getConversionJacobian(state);

        if (state.isOrbitDefined() && state.getOrbit().getType() != OrbitType.CARTESIAN) {
            // make sure the state is in the desired orbit type
            final Orbit orbit = propagator.getOrbitType().convertType(state.getOrbit());

            // compute the Jacobian, taking the position angle type into account
            final double[][] dYdC = new double[identity.length][identity[0].length];
            orbit.getJacobianWrtCartesian(propagator.getPositionAngleType(), dYdC);
            return dYdC;
        } else {
            return identity;
        }

    }

    /** {@inheritDoc} */
    @Override
    public void freezeColumnsNames() {
        columnsNames = getJacobiansColumnsNames();
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getJacobiansColumnsNames() {
        return columnsNames == null ? propagator.getJacobiansColumnsNames() : columnsNames;
    }

    /** {@inheritDoc} */
    @Override
    public OrbitType getOrbitType() {
        return propagator.getOrbitType();
    }

    /** {@inheritDoc} */
    @Override
    public PositionAngleType getPositionAngleType() {
        return propagator.getPositionAngleType();
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix getStateTransitionMatrix(final SpacecraftState state) {

        if (!state.hasAdditionalData(getStmName())) {
            return null;
        }

        // extract the additional state
        final double[] p = state.getAdditionalState(getStmName());
        final RealMatrix  dCdY0 = toSquareMatrix(p);

        final RealMatrix  dYdY0;
        if (!state.isOrbitDefined() || state.getOrbit().getType() == OrbitType.CARTESIAN) {
            dYdY0 = dCdY0;
        } else {
            // get the conversion Jacobian
            final RealMatrix dYdC = MatrixUtils.createRealIdentityMatrix(getStateDimension());
            dYdC.setSubMatrix(getConversionJacobian(state), 0, 0);

            // compute dYdC * dCdY0
            dYdY0 = dYdC.multiply(dCdY0);
        }

        return dYdY0;

    }

}
