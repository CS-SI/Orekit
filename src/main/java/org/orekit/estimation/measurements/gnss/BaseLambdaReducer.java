/* Copyright 2002-2019 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
package org.orekit.estimation.measurements.gnss;

import java.util.List;

import org.hipparchus.linear.DiagonalMatrix;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.util.FastMath;
import org.orekit.utils.ParameterDriver;

/** Base class for decorrelation/reduction engine for LAMBDA type methods.
 * <p>
 * This class is based on the 2005 paper <a
 * href="https://www.researchgate.net/publication/225518977_MLAMBDA_a_modified_LAMBDA_method_for_integer_least-squares_estimation">
 * A modified LAMBDA method for integer least-squares estimation</a> by X.-W Chang, X. Yang
 * and T. Zhou, Journal of Geodesy 79(9):552-565, DOI: 10.1007/s00190-005-0004-x
 * </p>
 * @author Luc Maisonobe
 * @since 10.0
 */
abstract class BaseLambdaReducer {

    /** Number of ambiguities. */
    private final int n;

    /** Drivers for ambiguities. */
    private final List<ParameterDriver> drivers;

    /** DEcorrelated ambiguities. */
    private final double[] decorrelated;

    /** Indirection array to extract ambiguity parameters. */
    private final int[] indirection;

    /** Lower triangular matrix with unit diagonal, in row order. */
    private final double[] low;

    /** Diagonal matrix. */
    private final double[] diag;

    /** Z transformation matrix, in row order. */
    private int[] zTransformation;

    /** Simple constructor.
     * @param drivers parameters drivers for ambiguities
     * @param indirection indirection array to extract ambiguity parameters
     * @param covariance full covariance matrix
     */
    protected BaseLambdaReducer(final List<ParameterDriver> drivers, final int[] indirection, final RealMatrix covariance) {

        this.n               = drivers.size();
        this.drivers         = drivers;
        this.decorrelated    = drivers.stream().mapToDouble(d -> d.getValue()).toArray();
        this.indirection     = indirection.clone();
        this.low             = new double[(n * (n - 1)) / 2];
        this.diag            = new double[n];
        this.zTransformation = new int[n * n];

        // initialize decomposition matrices
        for (int i = 0; i < n; ++i) {
            for (int j = 0; j < i; ++j) {
                low[lIndex(i, j)] = covariance.getEntry(indirection[i], indirection[j]);
            }
            diag[i] = covariance.getEntry(indirection[i], indirection[i]);
            zTransformation[zIndex(i, i)] = 1;
        }

    }

    /** Perform Lᵀ.D.L decomposition.
     */
    public void ltdlDecomposition() {
        doDecomposition(diag, low);
    }

    /** Perform Lᵀ.D.L decomposition.
     * @param d diagonal matrix of the decomposition
     * @param l lower triangular matrix of the decomposition
     */
    protected abstract void doDecomposition(double[] d, double[] l);

    /** Perform LAMBDA reduction.
     */
    public void reduction() {
        doReduction(diag, low);
    }

    /** Perform LAMBDA reduction.
     * @param d diagonal matrix of the decomposition
     * @param l lower triangular matrix of the decomposition
     */
    protected abstract void doReduction(double[] d, double[] l);

    /** Get the low triangular matrix of the Lᵀ.D.L decomposition.
     * @return low triangular matrix of the Lᵀ.D.L decomposition
     */
    public RealMatrix getLow() {
        final RealMatrix lowM = MatrixUtils.createRealMatrix(n, n);
        int k = 0;
        for (int i = 0; i < n; ++i) {
            for (int j = 0; j < i; ++j) {
                lowM.setEntry(i, j, low[k++]);
            }
            lowM.setEntry(i, i, 1.0);
        }
        return lowM;
    }

    /** Get the diagonal matrix of the Lᵀ.D.L decomposition.
     * @return diagonal matrix of the Lᵀ.D.L decomposition
     */
    public DiagonalMatrix getDiag() {
        return MatrixUtils.createRealDiagonalMatrix(diag);
    }

    /** Perform one integer Gauss transformation.
     * <p>
     * This method corresponds to algorithm 2.1 in X.-W Chang, X. Yang and T. Zhou paper.
     * </p>
     * @param row row index (counting from 0)
     * @param col column index (counting from 0)
     */
    protected void integerGaussTransformation(final int row, final int col) {
        final int mu = (int) FastMath.rint(low[lIndex(row, col)]);
        if (mu != 0) {

            // update low triangular matrix (post-multiplying L by Zᵢⱼ = I - μ eᵢ eⱼᵀ)
            for (int i = row; i < n; ++i) {
                low[lIndex(i, col)] -= mu * low[lIndex(i, row)];
            }

            // update Z transformation matrix (post-multiplying Z by Zᵢⱼ = I - μ eᵢ eⱼᵀ)
            for (int i = 0; i < n; ++i) {
                zTransformation[zIndex(i, col)] -= mu * zTransformation[zIndex(i, row)];
            }

            // update decorrelated ambiguities estimates (pre-multiplying a by  Zᵢⱼᵀ = I - μ eⱼ eᵢᵀ)
            decorrelated[col] -= mu * decorrelated[row];

        }
    }

    /** Perform one symmetric permutation involving rows/columns {@code k0} and {@code k0+1}.
     * <p>
     * This method corresponds to algorithm 2.2 in X.-W Chang, X. Yang and T. Zhou paper.
     * </p>
     * @param k0 diagonal index (counting from 0)
     * @param delta new value for diag[k0+1]
     */
    protected void permutation(final int k0, final double delta) {

        final int    k1        = k0 + 1;
        final int    k2        = k0 + 2;
        final int    indexk1k0 = lIndex(k1, k0);
        final double lk1k0     = low[indexk1k0];
        final double eta       = diag[k0] / delta;
        final double lambda    = diag[k1] * lk1k0 / delta;

        // update diagonal
        diag[k0] = eta * diag[k1];
        diag[k1] = delta;

        // update low triangular matrix
        for (int j = 0; j < k0; ++j) {
            final int indexk0j = lIndex(k0, j);
            final int indexk1j = lIndex(k1, j);
            final double lk0j  = low[indexk0j];
            final double lk1j  = low[indexk1j];
            low[indexk0j]      = lk1j          - lk1k0 * lk0j;
            low[indexk1j]      = lambda * lk1j + eta   * lk0j;
        }
        low[indexk1k0] = lambda;
        for (int i = k2; i < n; ++i) {
            final int indexik0 = lIndex(i, k0);
            final int indexik1 = indexik0 + 1;
            final double tmp   = low[indexik0];
            low[indexik0]      = low[indexik1];
            low[indexik1]      = tmp;
        }

        // update z transformation matrix
        for (int i = 0; i < n; ++i) {
            final int indexik0        = zIndex(i, k0);
            final int indexik1        = indexik0 + 1;
            final int tmp             = zTransformation[indexik0];
            zTransformation[indexik0] = zTransformation[indexik1];
            zTransformation[indexik1] = tmp;
        }

        // update decorrelated ambiguities
        final double tmp = decorrelated[k0];
        decorrelated[k0] = decorrelated[k1];
        decorrelated[k1] = tmp;

    }

    /** Get the index of an entry in the lower triangular matrix.
     * @param row row index (counting from 0)
     * @param col column index (counting from 0)
     * @return index in the single dimension array
     */
    protected int lIndex(final int row, final int col) {
        return (row * (row - 1)) / 2 + col;
    }

    /** Get the index of an entry in the Z transformation matrix.
     * @param row row index (counting from 0)
     * @param col column index (counting from 0)
     * @return index in the single dimension array
     */
    protected int zIndex(final int row, final int col) {
        return row * n + col;
    }

}
