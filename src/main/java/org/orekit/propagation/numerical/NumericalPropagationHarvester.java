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

import java.util.Collections;
import java.util.List;

import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.MatricesHarvester;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.DoubleArrayDictionary;

/** Harvester between two-dimensional Jacobian matrices and one-dimensional {@link
 * SpacecraftState#getAdditionalState(String) additional state arrays}.
 * @author Luc Maisonobe
 * @since 11.1
 */
class NumericalPropagationHarvester implements MatricesHarvester {

    /** State dimension, fixed to 6. */
    public static final int STATE_DIMENSION = 6;

    /** Initial State Transition Matrix. */
    private final RealMatrix initialStm;

    /** Initial columns of the Jacobians matrix with respect to parameters. */
    private final DoubleArrayDictionary initialJacobianColumns;

    /** State Transition Matrix state name. */
    private final String stmName;

    /** Columns names for parameters. */
    private List<String> columnsNames;

    /** Orbit type. */
    private OrbitType orbitType;

    /** Position angle type. */
    private PositionAngle positionAngleType;

    /** Simple constructor.
     * <p>
     * The arguments for initial matrices <em>must</em> be compatible with the {@link #setOrbitType(OrbitType) orbit type}
     * and {@link #setPositionAngleType(PositionAngle) position angle} that will be ultimately
     * selected when propagation starts
     * </p>
     * @param stmName State Transition Matrix state name
     * @param initialStm initial State Transition Matrix ∂Y/∂Y₀,
     * if null (which is the most frequent case), assumed to be 6x6 identity
     * @param initialJacobianColumns initial columns of the Jacobians matrix with respect to parameters,
     * if null or if some selected parameters are missing from the dictionary, the corresponding
     * initial column is assumed to be 0
     */
    NumericalPropagationHarvester(final String stmName, final RealMatrix initialStm,
                                  final DoubleArrayDictionary initialJacobianColumns) {
        this.stmName                = stmName;
        this.initialStm             = initialStm == null ? MatrixUtils.createRealIdentityMatrix(STATE_DIMENSION) : initialStm;
        this.initialJacobianColumns = initialJacobianColumns == null ? new DoubleArrayDictionary() : initialJacobianColumns;
        this.columnsNames           = Collections.emptyList();
    }

    /** Get the State Transition Matrix state name.
     * @return State Transition Matrix state name
     */
    String getStmName() {
        return stmName;
    }

    /** Set Jacobian matrix columns names.
     * @param columnsNames names of the parameters for Jacobian columns, in desired matrix order
     */
    void setColumnsNames(final List<String> columnsNames) {
        this.columnsNames = columnsNames;
    }

    /** Set orbit type.
     * @param orbitType orbit type
     */
    void setOrbitType(final OrbitType orbitType) {
        this.orbitType = orbitType;
    }

    /** Set position angle type.
     * @param positionAngleType angle type
     */
    void setPositionAngleType(final PositionAngle positionAngleType) {
        this.positionAngleType = positionAngleType;
    }

    /** Get the initial State Transition Matrix.
     * @return initial State Transition Matrix
     */
    RealMatrix getInitialStateTransitionMatrix() {
        return initialStm;
    }

    /** Get the initial column of Jacobian matrix with respect to named parameter.
     * @param columnName name of the column
     * @return initial column of the Jacobian matrix
     */
    double[] getInitialJacobianColumn(final String columnName) {
        final DoubleArrayDictionary.Entry entry = initialJacobianColumns.getEntry(columnName);
        return entry == null ? new double[STATE_DIMENSION] : entry.getValue();
    }

    /** Get the conversion Jacobian between state parameters and parameters used for derivatives.
     * <p>
     * For DSST and TLE propagators, state parameters and parameters used for derivatives are the same,
     * so the Jocabian is simply the identity.
     * </p>
     * <p>
     * For Numerical propagator, parameters used for derivatives are cartesian
     * and they can be different from state parameters because the numerical propagator can accept different type
     * of orbits.
     * </p>
     * @param state spacecraft state
     * @return conversion Jacobian
     */
    private double[][] getConversionJacobian(final SpacecraftState state) {

        final double[][] dYdC = new double[STATE_DIMENSION][STATE_DIMENSION];

        if (state.isOrbitDefined()) {
            // make sure the state is in the desired orbit type
            final Orbit orbit = orbitType.convertType(state.getOrbit());

            // compute the Jacobian, taking the position angle type into account
            orbit.getJacobianWrtCartesian(positionAngleType, dYdC);
        } else {
            // for absolute PVA, parameters are already Cartesian
            for (int i = 0; i < STATE_DIMENSION; ++i) {
                dYdC[i][i] = 1.0;
            }
        }

        return dYdC;

    }

    /** {@inheritDoc} */
    @Override
    public void setReferenceState(final SpacecraftState reference) {
        // nothing to do
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix getStateTransitionMatrix(final SpacecraftState state) {

        if (stmName == null || !state.hasAdditionalState(stmName)) {
            return null;
        }

        // get the conversion Jacobian
        final double[][] dYdC = getConversionJacobian(state);

        // extract the additional state
        final double[] p = state.getAdditionalState(stmName);

        // compute dYdY0 = dYdC * dCdY0
        final RealMatrix  dYdY0 = MatrixUtils.createRealMatrix(STATE_DIMENSION, STATE_DIMENSION);
        for (int i = 0; i < STATE_DIMENSION; i++) {
            final double[] rowC = dYdC[i];
            for (int j = 0; j < STATE_DIMENSION; ++j) {
                double sum = 0;
                int pIndex = j;
                for (int k = 0; k < STATE_DIMENSION; ++k) {
                    sum += rowC[k] * p[pIndex];
                    pIndex += STATE_DIMENSION;
                }
                dYdY0.setEntry(i, j, sum);
            }
        }

        return dYdY0;

    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix getParametersJacobian(final SpacecraftState state) {

        if (columnsNames.isEmpty()) {
            return null;
        }

        // get the conversion Jacobian
        final double[][] dYdC = getConversionJacobian(state);

        // compute dYdP = dYdC * dCdP
        final RealMatrix dYdP = MatrixUtils.createRealMatrix(STATE_DIMENSION, columnsNames.size());
        for (int j = 0; j < columnsNames.size(); j++) {
            final double[] p = state.getAdditionalState(columnsNames.get(j));
            for (int i = 0; i < STATE_DIMENSION; ++i) {
                final double[] dYdCi = dYdC[i];
                double sum = 0;
                for (int k = 0; k < STATE_DIMENSION; ++k) {
                    sum += dYdCi[k] * p[k];
                }
                dYdP.setEntry(i, j, sum);
            }
        }

        return dYdP;

    }

    /** {@inheritDoc} */
    @Override
    public List<String> getJacobiansColumnsNames() {
        return Collections.unmodifiableList(columnsNames);
    }

}
