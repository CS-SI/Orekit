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
package org.orekit.propagation;

import java.util.List;

import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.orekit.orbits.PositionAngleType;
import org.orekit.utils.DoubleArrayDictionary;

/** Base harvester between two-dimensional Jacobian matrices and one-dimensional {@link
 * SpacecraftState#getAdditionalState(String) additional state arrays}.
 * @author Luc Maisonobe
 * @since 11.1
 */
public abstract class AbstractMatricesHarvester implements MatricesHarvester {

    /** State dimension, fixed to 6.
     * @deprecated as of 13.1, use DEFAULT_STATE_DIMENSION */
    @Deprecated
    public static final int STATE_DIMENSION = 6;

    /** Default state dimension, equivalent to position and velocity vectors. */
    public static final int DEFAULT_STATE_DIMENSION = 6;

    /** Identity conversion matrix for Cartesian-like coordinates. */
    private static final double[][] IDENTITY6 = {
        { 1.0, 0.0, 0.0, 0.0, 0.0, 0.0 },
        { 0.0, 1.0, 0.0, 0.0, 0.0, 0.0 },
        { 0.0, 0.0, 1.0, 0.0, 0.0, 0.0 },
        { 0.0, 0.0, 0.0, 1.0, 0.0, 0.0 },
        { 0.0, 0.0, 0.0, 0.0, 1.0, 0.0 },
        { 0.0, 0.0, 0.0, 0.0, 0.0, 1.0 }
    };

    /** Initial State Transition Matrix. */
    private final RealMatrix initialStm;

    /** Initial columns of the Jacobians matrix with respect to parameters. */
    private final DoubleArrayDictionary initialJacobianColumns;

    /** State Transition Matrix state name. */
    private final String stmName;

    /** Simple constructor.
     * <p>
     * The arguments for initial matrices <em>must</em> be compatible with the {@link org.orekit.orbits.OrbitType orbit type}
     * and {@link PositionAngleType position angle} that will be used by propagator
     * </p>
     * @param stmName State Transition Matrix state name
     * @param initialStm initial State Transition Matrix ∂Y/∂Y₀,
     * if null (which is the most frequent case), assumed to be 6x6 identity
     * @param initialJacobianColumns initial columns of the Jacobians matrix with respect to parameters,
     * if null or if some selected parameters are missing from the dictionary, the corresponding
     * initial column is assumed to be 0
     */
    protected AbstractMatricesHarvester(final String stmName, final RealMatrix initialStm, final DoubleArrayDictionary initialJacobianColumns) {
        this.stmName                = stmName;
        this.initialStm             = initialStm == null ? MatrixUtils.createRealIdentityMatrix(DEFAULT_STATE_DIMENSION) : initialStm;
        this.initialJacobianColumns = initialJacobianColumns == null ? new DoubleArrayDictionary() : initialJacobianColumns;
    }

    /**
     * Getter for the state dimension.
     * @return state dimension
     * @since 13.1
     */
    public int getStateDimension() {
        return initialStm.getColumnDimension();
    }

    /** Get the State Transition Matrix state name.
     * @return State Transition Matrix state name
     */
    public String getStmName() {
        return stmName;
    }

    /** Get the initial State Transition Matrix.
     * @return initial State Transition Matrix
     */
    public RealMatrix getInitialStateTransitionMatrix() {
        return initialStm;
    }

    /** Get the initial column of Jacobian matrix with respect to named parameter.
     * @param columnName name of the column
     * @return initial column of the Jacobian matrix
     */
    public double[] getInitialJacobianColumn(final String columnName) {
        final DoubleArrayDictionary.Entry entry = initialJacobianColumns.getEntry(columnName);
        return entry == null ? new double[getStateDimension()] : entry.getValue();
    }

    /** Convert a flattened array to a square matrix.
     * @param array input array
     * @return the corresponding matrix
     * @since 13.1
     */
    public RealMatrix toSquareMatrix(final double[] array) {
        final int stateDimension = getStateDimension();
        final RealMatrix matrix = MatrixUtils.createRealMatrix(stateDimension, stateDimension);
        int index = 0;
        for (int i = 0; i < stateDimension; ++i) {
            for (int j = 0; j < stateDimension; ++j) {
                matrix.setEntry(i, j, array[index++]);
            }
        }
        return matrix;
    }

    /** Set the STM data into an array.
     * @param matrix STM matrix
     * @return an array containing the STM data
     * @since 13.1
     */
    public double[] toArray(final double[][] matrix) {
        final int stateDimension = matrix.length;
        final double[] array = new double[stateDimension * stateDimension];
        int index = 0;
        for (final double[] row : matrix) {
            for (int j = 0; j < stateDimension; ++j) {
                array[index++] = row[j];
            }
        }
        return array;
    }

    /** Get the conversion Jacobian between state parameters and parameters used for derivatives.
     * <p>
     * The base implementation returns identity, which is suitable for DSST and TLE propagators,
     * as state parameters and parameters used for derivatives are the same.
     * </p>
     * <p>
     * For Numerical propagator, parameters used for derivatives are Cartesian
     * and they can be different from state parameters because the numerical propagator can accept different type
     * of orbits, so the method is overridden in derived classes.
     * </p>
     * @param state spacecraft state
     * @return conversion Jacobian
     */
    protected double[][] getConversionJacobian(final SpacecraftState state) {
        return IDENTITY6;
    }

    /** {@inheritDoc} */
    @Override
    public void setReferenceState(final SpacecraftState reference) {
        // nothing to do
    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix getStateTransitionMatrix(final SpacecraftState state) {

        if (!state.hasAdditionalData(stmName)) {
            return null;
        }

        // get the conversion Jacobian
        final double[][] dYdC = getConversionJacobian(state);

        // extract the additional state
        final double[] p = state.getAdditionalState(stmName);

        // compute dYdY0 = dYdC * dCdY0
        final int stateDimension = getStateDimension();
        final RealMatrix  dYdY0 = MatrixUtils.createRealMatrix(stateDimension, stateDimension);
        for (int i = 0; i < stateDimension; i++) {
            final double[] rowC = dYdC[i];
            for (int j = 0; j < stateDimension; ++j) {
                double sum = 0;
                int pIndex = j;
                for (int k = 0; k < stateDimension; ++k) {
                    sum += rowC[k] * p[pIndex];
                    pIndex += stateDimension;
                }
                dYdY0.setEntry(i, j, sum);
            }
        }

        return dYdY0;

    }

    /** {@inheritDoc} */
    @Override
    public RealMatrix getParametersJacobian(final SpacecraftState state) {

        final List<String> columnsNames = getJacobiansColumnsNames();

        if (columnsNames == null || columnsNames.isEmpty()) {
            return null;
        }

        // get the conversion Jacobian
        final RealMatrix dYdC = MatrixUtils.createRealIdentityMatrix(getStateDimension());
        dYdC.setSubMatrix(getConversionJacobian(state), 0, 0);

        // compute dYdP = dYdC * dCdP
        final RealMatrix dYdP = MatrixUtils.createRealMatrix(getStateDimension(), columnsNames.size());
        for (int j = 0; j < columnsNames.size(); j++) {
            final double[] p = state.getAdditionalState(columnsNames.get(j));
            for (int i = 0; i < getStateDimension(); ++i) {
                final double[] dYdCi = dYdC.getRow(i);
                double sum = 0;
                for (int k = 0; k < getStateDimension(); ++k) {
                    sum += dYdCi[k] * p[k];
                }
                dYdP.setEntry(i, j, sum);
            }
        }

        return dYdP;

    }

    /** Freeze the names of the Jacobian columns.
     * <p>
     * This method is called when propagation starts, i.e. when configuration is completed
     * </p>
     */
    public abstract void freezeColumnsNames();

}
