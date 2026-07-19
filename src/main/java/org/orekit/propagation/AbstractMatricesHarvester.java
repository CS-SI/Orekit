/* Copyright 2002-2026 CS GROUP
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

    /** Default state dimension, equivalent to position and velocity vectors. */
    public static final int DEFAULT_STATE_DIMENSION = 6;

    /** State Transition Matrix state name. */
    private String stmName;

    /** Initial State Transition Matrix. */
    private RealMatrix initialStm;

    /** Initial columns of the Jacobians matrix with respect to parameters. */
    private DoubleArrayDictionary initialJacobianColumns;

    /** Set the initial State Transition Matrix.
     * <p>
     * The arguments for initial matrices <em>must</em> be compatible with the
     * {@link org.orekit.orbits.OrbitType orbit type} and
     * {@link PositionAngleType position angle} that will be used by propagator,
     * which may be different from input and output
     * </p>
     * @param name State Transition Matrix state name
     * @param initialStm initial State Transition Matrix ∂Y/∂I₀
     *                   if null (which is the most frequent case), input and output
     *                   orbit types are assumed to be identical and the matrix is
     *                   assumed to be the 6x6 identity
     */
    protected void setInitialStm(final String name, final RealMatrix initialStm) {
        this.stmName    = name;
        this.initialStm = initialStm == null ?
                          MatrixUtils.createRealIdentityMatrix(DEFAULT_STATE_DIMENSION) :
                          initialStm;
    }

    /** Set the initial columns of the Jacobians matrix with respect to parameters.
     * <p>
     * The arguments for initial matrices <em>must</em> be compatible with the
     * {@link org.orekit.orbits.OrbitType orbit type} and
     * {@link PositionAngleType position angle} that will be used by propagator
     * </p>
     * @param initialJacobianColumns initial columns of the Jacobians matrix with respect to parameters,
     * if null or if some selected parameters are missing from the dictionary, the corresponding
     * initial column is assumed to be 0
     */
    protected void setInitialJacobianColumns(final DoubleArrayDictionary initialJacobianColumns) {
        this.initialJacobianColumns = initialJacobianColumns == null ?
                                      new DoubleArrayDictionary() :
                                      initialJacobianColumns;
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

    /** {@inheritDoc} */
    @Override
    public void setReferenceState(final SpacecraftState reference) {
        // nothing to do
    }

    /** Freeze the names of the Jacobian columns.
     * <p>
     * This method is called when propagation starts, i.e. when configuration is completed
     * </p>
     */
    public abstract void freezeColumnsNames();

}
