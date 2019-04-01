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

import java.util.SortedSet;
import java.util.TreeSet;

import org.hipparchus.linear.RealMatrix;
import org.hipparchus.util.FastMath;

/** Base class for decorrelation/reduction engine for LAMBDA type methods.
 * <p>
 * This class is based on both the 1996 paper <a href="https://www.researchgate.net/publication/2790708_The_LAMBDA_method_for_integer_ambiguity_estimation_implementation_aspects">
 * The LAMBDA method for integer ambiguity estimation: implementation aspects</a> by
 * Paul de Jonge and Christian Tiberius and on the 2005 paper <a
 * href="https://www.researchgate.net/publication/225518977_MLAMBDA_a_modified_LAMBDA_method_for_integer_least-squares_estimation">
 * A modified LAMBDA method for integer least-squares estimation</a> by X.-W Chang, X. Yang
 * and T. Zhou, Journal of Geodesy 79(9):552-565, DOI: 10.1007/s00190-005-0004-x
 * </p>
 * @author Luc Maisonobe
 * @since 10.0
 */
abstract class AbstractLambdaMethod implements IntegerLeastSquareSolver {

    /** Margin factor to apply to estimated search limit parameter. */
    private static final double CHI2_MARGIN_FACTOR = 1.1;

    /** Number of ambiguities. */
    private int n;

    /** Decorrelated ambiguities. */
    private double[] decorrelated;

    /** Lower triangular matrix with unit diagonal, in row order (unit diagonal not stored). */
    private double[] low;

    /** Diagonal matrix. */
    private double[] diag;

    /** Z transformation matrix, in row order. */
    private int[] zTransformation;

    /** Z⁻¹ transformation matrix, in row order. */
    private int[] zInverseTransformation;

    /** Maximum number of solutions seeked. */
    private int maxSolutions;

    /** Placeholder for solutions found. */
    private SortedSet<IntegerLeastSquareSolution> solutions;

    /** {@inheritDoc} */
    @Override
    public IntegerLeastSquareSolution[] solveILS(final int nbSol, final double[] floatAmbiguities,
                                                 final int[] indirection, final RealMatrix covariance) {

        // initialize the ILS problem search
        initializeSearch(floatAmbiguities, indirection, covariance, nbSol);

        // perform initial Lᵀ.D.L = Q decomposition of covariance
        ltdlDecomposition();

        // perform decorrelation/reduction of covariances
        reduction();

        // transforms the Lᵀ.D.L = Q decomposition of covariance into
        // the L⁻¹.D⁻¹.L⁻ᵀ = Q⁻¹ decomposition of the inverse of covariance.
        inverseDecomposition();

        // perform discrete search of Integer Least Square problem
        solveILS();

        return recoverAmbiguities();

    }

    /** Initialize search.
     * @param floatAmbiguities float estimates of ambiguities
     * @param indirection indirection array to extract ambiguity covariances from global covariance matrix
     * @param globalCovariance global covariance matrix (includes ambiguities among other parameters)
     * @param nbSol number of solutions to search for
     */
    protected void initializeSearch(final double[] floatAmbiguities, final int[] indirection,
                                    final RealMatrix globalCovariance, final int nbSol) {

        this.n                      = floatAmbiguities.length;
        this.decorrelated           = floatAmbiguities.clone();
        this.low                    = new double[(n * (n - 1)) / 2];
        this.diag                   = new double[n];
        this.zTransformation        = new int[n * n];
        this.zInverseTransformation = new int[n * n];
        this.maxSolutions           = nbSol;
        this.solutions              = new TreeSet<>();

        // initialize decomposition matrices
        for (int i = 0; i < n; ++i) {
            for (int j = 0; j < i; ++j) {
                low[lIndex(i, j)] = globalCovariance.getEntry(indirection[i], indirection[j]);
            }
            diag[i] = globalCovariance.getEntry(indirection[i], indirection[i]);
            zTransformation[zIndex(i, i)] = 1;
            zInverseTransformation[zIndex(i, i)] = 1;
        }

    }

    /** Get a reference to the diagonal matrix of the decomposition.
     * <p>
     * BEWARE: the returned value is a reference to an internal array,
     * it is <em>only</em> intended for subclasses use (hence the
     * method is protected and not public).
     * </p>
     * @return reference to the diagonal matrix of the decomposition
     */
    protected double[] getDiagReference() {
        return diag;
    }

    /** Get a reference to the lower triangular matrix of the decomposition.
     * <p>
     * BEWARE: the returned value is a reference to an internal array,
     * it is <em>only</em> intended for subclasses use (hence the
     * method is protected and not public).
     * </p>
     * @return reference to the lower triangular matrix of the decomposition
     */
    protected double[] getLowReference() {
        return low;
    }

    /** Perform Lᵀ.D.L = Q decomposition of the covariance matrix.
     */
    protected abstract void ltdlDecomposition();

    /** Perform LAMBDA reduction.
     */
    protected abstract void reduction();

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
            low[lIndex(row, col)] -= mu;
            for (int i = row + 1; i < n; ++i) {
                low[lIndex(i, col)] -= mu * low[lIndex(i, row)];
            }

            // update Z and Z⁻¹ transformations matrices
            for (int i = 0; i < n; ++i) {
                // post-multiplying Z by Zᵢⱼ = I - μ eᵢ eⱼᵀ
                zTransformation[zIndex(i, col)]        -= mu * zTransformation[zIndex(i, row)];
                // pre-multiplying Z⁻¹ by Zᵢⱼ⁻¹ = I + μ eᵢ eⱼᵀ
                zInverseTransformation[zIndex(row, i)] += mu * zInverseTransformation[zIndex(col, i)];
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

        // update Z and Z⁻¹ transformations matrices
        for (int i = 0; i < n; ++i) {

            final int indexik0               = zIndex(i, k0);
            final int indexik1               = indexik0 + 1;
            final int tmp1                   = zTransformation[indexik0];
            zTransformation[indexik0]        = zTransformation[indexik1];
            zTransformation[indexik1]        = tmp1;

            final int indexk0i               = zIndex(k0, i);
            final int indexk1i               = indexk0i + n;
            final int tmp2                   = zInverseTransformation[indexk0i];
            zInverseTransformation[indexk0i] = zInverseTransformation[indexk1i];
            zInverseTransformation[indexk1i] = tmp2;

        }

        // update decorrelated ambiguities
        final double tmp = decorrelated[k0];
        decorrelated[k0] = decorrelated[k1];
        decorrelated[k1] = tmp;

    }

    /** Inverse the decomposition.
     * <p>
     * This method transforms the Lᵀ.D.L = Q decomposition of covariance into
     * the L⁻¹.D⁻¹.L⁻ᵀ = Q⁻¹ decomposition of the inverse of covariance.
     * </p>
     */
    private void inverseDecomposition() {

        // we rely on the following equation, where a low triangular
        // matrix L of dimension n is split into sub-matrices of dimensions
        // k, l and m with k + l + m = n
        //
        // [  A  |      |    ]  [        A⁻¹        |         |       ]   [  Iₖ  |      |     ]
        // [  B  |  Iₗ  |    ]  [       -BA⁻¹       |   Iₗ    |       ] = [      |  Iₗ  |     ]
        // [  C  |  D   |  E ]  [ E⁻¹ (DB - C) A⁻¹  | -E⁻¹D   |  E⁻¹  ]   [      |      |  Iₘ ]
        //
        // considering we have already computed A⁻¹ (i.e. inverted rows 0 to k-1
        // of L), and using l = 1 in the previous expression (i.e. the middle matrix
        // is only one row), we see that elements 0 to k-1 of row k are given by -BA⁻¹
        // and that element k is I₁ = 1. We can therefore invert L row by row and we
        // obtain an inverse matrix L⁻¹ which is a low triangular matrix with unit
        // diagonal. A⁻¹ is therefore also a low triangular matrix with unit diagonal.
        // This property is used in the loops below to speed up the computation of -BA⁻¹
        final double[] row = new double[n - 1];
        diag[0] = 1.0 / diag[0];
        for (int k = 1; k < n; ++k) {

            // compute row k of lower triangular part, by computing -BA⁻¹
            final int iK = lIndex(k, 0);
            System.arraycopy(low, iK, row, 0, k);
            for (int j = 0; j < k; ++j) {
                double sum = row[j];
                for (int l = j + 1; l < k; ++l) {
                    sum += row[l] * low[lIndex(l, j)];
                }
                low[iK + j] = -sum;
            }

            // diagonal part
            diag[k] = 1.0 / diag[k];

        }

    }

    /** Find the best solutions to the Integer Least Square problem.
     */
    private void solveILS() {

        // estimate search domain limit
        final long[]   fixed   = new long[n];
        final double[] offsets = new double[n];
        final double   chi2    = estimateChi2(fixed, offsets);

        // set up top level sampling for last ambiguity
        final double right = chi2 / diag[n - 1];
        final AlternatingSampler topSampler = new AlternatingSampler(decorrelated[n - 1], FastMath.sqrt(right));

        // try different candidates for last ambiguity
        while (topSampler.inRange()) {
            fixed[n - 1]      = topSampler.getCurrent();
            offsets[n - 1]    = fixed[n - 1] - decorrelated[n - 1];
            final double left = offsets[n - 1] * offsets[n - 1];

            // recursive search, assuming current candidate for last ambiguity
            recursiveSolve(n - 1, left, right, chi2, fixed, offsets);

            // prepare next candidate
            topSampler.generateNext();

        }

    }

    /** Recursive search of solutions to the Integer Least Square problem.
     * @param index index of first fixed ambiguity
     * @param left left hand side of search inequality (de Jonge and Tiberius 1996 paper, equation 4.6)
     * @param right right hand side of search inequality (de Jonge and Tiberius 1996 paper, equation 4.6)
     * @param chi2 search limit parameter χ²
     * @param fixed placeholder for test fixed ambiguities
     * @param offsets placeholder for offsets between fixed ambiguities and float ambiguities
     */
    private void recursiveSolve(final int index, final double left, final double right,
                                final double chi2, final long[] fixed, final double[] offsets) {

        if (index == 0) {
            // all ambiguities have been fixed
            final double squaredNorm = chi2 - diag[0] * (right - left);
            solutions.add(new IntegerLeastSquareSolution(fixed, squaredNorm));
            if (solutions.size() > maxSolutions) {
                solutions.remove(solutions.last());
            }
        } else {

            // we need to fix more ambiguities
            final int next = index - 1;

            // compute search bounds as per section 4 in de Jonge and Tiberius 1996
            final double             conditional = conditionalEstimate(next, offsets);
            final double             nextRight   = diag[index] / diag[next] * (right - left);
            final AlternatingSampler sampler     = new AlternatingSampler(conditional, FastMath.sqrt(nextRight));
            while (sampler.inRange()) {

                // candidate for next ambiguity from sampler
                fixed[next]           = sampler.getCurrent();
                offsets[next]         = fixed[next] - decorrelated[next];
                final double delta    = fixed[next] - conditional;
                final double nextLeft = delta * delta;

                // recursive search, assuming current candidate for next ambiguity
                recursiveSolve(next, nextLeft, nextRight, chi2, fixed, offsets);

                // prepare next candidate
                sampler.generateNext();

            }

        }

    }

    /** Recover ambiguities prior to the Z-transformation.
     * @return recovered ambiguities
     */
    private IntegerLeastSquareSolution[] recoverAmbiguities() {

        final IntegerLeastSquareSolution[] recovered = new IntegerLeastSquareSolution[solutions.size()];

        int k = 0;
        final long[] a = new long[n];
        for (final IntegerLeastSquareSolution solution : solutions) {
            final long[] s = solution.getSolution();
            for (int i = 0; i < n; ++i) {
                // compute a = Z⁻ᵀ.s
                long ai = 0;
                int l = zIndex(0, i);
                for (int j = 0; j < n; ++j) {
                    ai += zInverseTransformation[l] * s[j];
                    l  += n;
                }
                a[i] = ai;
            }
            recovered[k++] = new IntegerLeastSquareSolution(a, solution.getSquaredDistance());
        }

        return recovered;

    }

    /** Compute a safe estimate of search limit parameter χ².
     * <p>
     * The estimate is based on section 4.11 in de Jonge and Tiberius 1996,
     * computing χ² such that it includes at least a few preset grid points
     * </p>
     * @param fixed placeholder for test fixed ambiguities
     * @param offsets placeholder for offsets between fixed ambiguities and float ambiguities
     * @return safe estimate of search limit parameter χ²
     */
    private double estimateChi2(final long[] fixed, final double[] offsets) {

        // initialize test points, assuming ambiguities have been fully decorrelated
        final AlternatingSampler[] samplers = new AlternatingSampler[n];
        for (int i = 0; i < n; ++i) {
            samplers[i] = new AlternatingSampler(decorrelated[i], 0.0);
        }

        final SortedSet<Double> squaredNorms = new TreeSet<>();

        // first test point at center
        for (int i = 0; i < n; ++i) {
            fixed[i] = samplers[i].getCurrent();
        }
        squaredNorms.add(squaredNorm(fixed, offsets));

        while (squaredNorms.size() < maxSolutions) {
            // add a series of grid points, each shifted from center along a different axis
            for (int i = 0; i < n; ++i) {
                final long saved = fixed[i];
                samplers[i].generateNext();
                fixed[i] = samplers[i].getCurrent();
                squaredNorms.add(squaredNorm(fixed, offsets));
                fixed[i] = saved;
            }
        }

        // select a limit ensuring at least the needed number of grid points are in the search domain
        int count = 0;
        for (final double s : squaredNorms) {
            if (++count == maxSolutions) {
                return s * CHI2_MARGIN_FACTOR;
            }
        }

        // never reached
        return Double.NaN;

    }

    /** Compute squared norm of a set of fixed ambiguities.
     * @param fixed fixed ambiguities
     * @param offsets placeholder for offsets between fixed ambiguities and float ambiguities
     * @return squared norm of a set of fixed ambiguities
     */
    private double squaredNorm(final long[] fixed, final double[] offsets) {
        double n2 = 0;
        for (int i = n - 1; i >= 0; --i) {
            offsets[i] = fixed[i] - decorrelated[i];
            final double delta = fixed[i] - conditionalEstimate(i, offsets);
            n2 += diag[i] * delta * delta;
        }
        return n2;
    }

    /** Compute conditional estimate of an ambiguity.
     * <p>
     * This corresponds to equation 4.4 in de Jonge and Tiberius 1996
     * </p>
     * @param i index of the ambiguity
     * @param offsets offsets between already fixed ambiguities and float ambiguities
     * @return conditional estimate of ambiguity â<sub>i|i+1...n</sub>
     */
    private double conditionalEstimate(final int i, final double[] offsets) {
        double conditional = decorrelated[i];
        for (int j = i + 1; j < n; ++j) {
            conditional -= low[lIndex(j, i)] * offsets[j];
        }
        return conditional;
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
