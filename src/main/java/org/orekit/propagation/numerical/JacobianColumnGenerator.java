/* Copyright 2002-2021 CS GROUP
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

import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.QRDecomposition;
import org.hipparchus.util.Precision;
import org.orekit.errors.OrekitException;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.AdditionalEquations;

/** Generator for one column of a Jacobian matrix.
 * @author Luc Maisonobe
 * @since 11.1
 */
class JacobianColumnGenerator implements AdditionalEquations, StateTransitionMatrixGenerator.PartialsObserver {

    /** Space dimension. */
    private static final int SPACE_DIMENSION = 3;

    /** State dimension. */
    private static final int STATE_DIMENSION = 2 * SPACE_DIMENSION;

    /** Threshold for matrix solving. */
    private static final double THRESHOLD = Precision.SAFE_MIN;

    /** Name of the state for State Transition Matrix. */
    private final String stmName;

    /** Name of the parameter corresponding to the column. */
    private final String columnName;

    /** Last value computed for the partial derivatives. */
    private final double[] pDot;

    /** Mast value computed for the
    /** Simple constructor.
     * <p>
     * The generator for State Transition Matrix <em>must</em> be registered as
     * an integrable generator to the same propagator as the instance, as it
     * must be scheduled to update the state before the instance
     * </p>
     * @param stmGenerator generator for State Transition Matrix
     * @param columnName name of the parameter corresponding to the column
     */
    JacobianColumnGenerator(final StateTransitionMatrixGenerator stmGenerator, final String columnName) {
        this.stmName    = stmGenerator.getName();
        this.columnName = columnName;
        this.pDot       = new double[STATE_DIMENSION];
        stmGenerator.addObserver(columnName, this);
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return columnName;
    }

    /** {@inheritDoc} */
    @Override
    public int getDimension() {
        return STATE_DIMENSION;
    }

    /** {@inheritDoc}
     * <p>
     * The column derivative can be computed only if the State Transition Matrix derivatives
     * are available, as it implies the STM generator has already been run.
     * </p>
     */
    @Override
    public boolean yield(final SpacecraftState state) {
        return !state.hasAdditionalStateDerivative(stmName);
    }

    /** Set the initial value of the column.
     * <p>
     * The returned state must be added to the propagator.
     * </p>
     * @param state initial state (must already contain the Cartesian State Transition Matrix)
     * @param dYdQ column of the Jacobian ∂Y/∂qₘ for the {@link #getName()} named parameter},
     * if null (which is the most frequent case), assumed to be 0
     * @param orbitType orbit type used for states Y and Y₀ in {@code dYdQ}
     * @param positionAngle position angle used states Y and Y₀ in {@code dYdQ}
     * @return state with Jacobian column (converted to Cartesian ∂C/∂Q) added
     */
    public SpacecraftState setInitialColumn(final SpacecraftState state, final double[] dYdQ,
                                            final OrbitType orbitType, final PositionAngle positionAngle) {

        final double[] column;
        if (dYdQ == null) {
            // initial Jacobian is null (this is the most frequent case)
            column = new double[STATE_DIMENSION];
        } else {

            if (dYdQ.length != STATE_DIMENSION) {
                throw new OrekitException(LocalizedCoreFormats.DIMENSIONS_MISMATCH,
                                          dYdQ.length, STATE_DIMENSION);
            }

            // convert to Cartesian Jacobian
            final double[][] dYdC = new double[STATE_DIMENSION][STATE_DIMENSION];
            orbitType.convertType(state.getOrbit()).getJacobianWrtCartesian(positionAngle, dYdC);
            column = new QRDecomposition(MatrixUtils.createRealMatrix(dYdC), THRESHOLD).
                     getSolver().
                     solve(MatrixUtils.createRealVector(dYdQ)).
                     toArray();

        }


        // set additional state
        return state.addAdditionalState(columnName, column);

    }

    /** {@inheritDoc} */
    @Override
    public void partialsComputed(final SpacecraftState state, final double[] factor, final double[] accelerationPartials) {
        // retrieve current Jacobian column
        final double[] p = state.getAdditionalState(getName());

        // compute time derivative of the Jacobian column
        StateTransitionMatrixGenerator.multiplyMatrix(factor, p, pDot, 1);
        pDot[3] += accelerationPartials[0];
        pDot[4] += accelerationPartials[1];
        pDot[5] += accelerationPartials[2];
    }

    /** {@inheritDoc} */
    @Override
    public double[] derivatives(final SpacecraftState s) {
        return pDot;
    }

}

