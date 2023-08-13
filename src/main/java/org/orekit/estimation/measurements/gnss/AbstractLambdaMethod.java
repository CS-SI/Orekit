/* Copyright 2002-2023 CS GROUP
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
package org.orekit.estimation.measurements.gnss;

import java.util.Comparator;
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
public abstract class AbstractLambdaMethod implements IntegerLeastSquareSolver {

    /** Number of ambiguities. */
    private int n;

    /** Decorrelated ambiguities. */
    private double[] decorrelated;

    /** Lower triangular matrix with unit diagonal, in row order (unit diagonal not stored). */
    private double[] low;

    /** Diagonal matrix. */
    private double[] diag;

    /** Z⁻¹ transformation matrix, in row order. */
    private int[] zInverseTransformation;

    /** Maximum number of solutions seeked. */
    private int maxSolutions;

    /** Placeholder for solutions found. */
    private SortedSet<IntegerLeastSquareSolution> solutions;

    /** Comparator for integer least square solutions. */
    private Comparator<IntegerLeastSquareSolution> comparator;

    /** Constructor.
     * <p>
     * By default a {@link IntegerLeastSquareComparator} is used
     * to compare integer least square solutions
     * </p>
     */
    protected AbstractLambdaMethod() {
        this.comparator = new IntegerLeastSquareComparator();
    }

    /** {@inheritDoc} */
    @Override
    public IntegerLeastSquareSolution[] solveILS(final int nbSol, final double[] floatAmbiguities,
                                                 final int[] indirection, final RealMatrix covariance) {

        // initialize the ILS problem search
        initializeProblem(floatAmbiguities, indirection, covariance, nbSol);

        // perform initial Lᵀ.D.L = Q decomposition of covariance
        ltdlDecomposition();

        // perform decorrelation/reduction of covariances
        reduction();

        // transform the Lᵀ.D.L = Q decomposition of covariance into
        // the L⁻¹.D⁻¹.L⁻ᵀ = Q⁻¹ decomposition of the inverse of covariance.
        inverseDecomposition();

        // perform discrete search of Integer Least Square problem
        discreteSearch();

        return recoverAmbiguities();

    }

    /** Set a custom comparator for integer least square solutions comparison.
     * <p>
     * Calling this method overrides any comparator that could have been set
     * beforehand. It also overrides the default {@link IntegerLeastSquareComparator}.
     * </p>
     * @param newCompartor new comparator to use
     * @since 11.0
     */
    public void setComparator(final Comparator<IntegerLeastSquareSolution> newCompartor) {
        this.comparator = newCompartor;
    }

    /** Initialize ILS problem.
     * @param floatAmbiguities float estimates of ambiguities
     * @param indirection indirection array to extract ambiguity covariances from global covariance matrix
     * @param globalCovariance global covariance matrix (includes ambiguities among other parameters)
     * @param nbSol number of solutions to search for
     */
    private void initializeProblem(final double[] floatAmbiguities, final int[] indirection,
                                   final RealMatrix globalCovariance, final int nbSol) {

        this.n                      = floatAmbiguities.length;
        this.decorrelated           = floatAmbiguities.clone();
        this.low                    = new double[(n * (n - 1)) / 2];
        this.diag                   = new double[n];
        this.zInverseTransformation = new int[n * n];
        this.maxSolutions           = nbSol;
        this.solutions              = new TreeSet<>(comparator);

        // initialize decomposition matrices
        for (int i = 0; i < n; ++i) {
            for (int j = 0; j < i; ++j) {
                low[lIndex(i, j)] = globalCovariance.getEntry(indirection[i], indirection[j]);
            }
            diag[i] = globalCovariance.getEntry(indirection[i], indirection[i]);
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

    /** Get the reference decorrelated ambiguities.
     * @return reference to the decorrelated ambiguities.
     **/
    protected double[] getDecorrelatedReference() {
        return decorrelated;
    }

    /** Get the maximum number of solutions seeked.
     * @return the maximum number of solutions seeked
     */
    protected int getMaxSolution() {
        return maxSolutions;
    }

    /** Add a new solution.
     * @param fixed solution array
     * @param squaredNorm squared distance to the corresponding float solution
     */
    protected void addSolution(final long[] fixed, final double squaredNorm) {
        solutions.add(new IntegerLeastSquareSolution(fixed, squaredNorm));
    }

    /** Remove spurious solution.
     */
    protected void removeSolution() {
        solutions.remove(solutions.last());
    }

    /** Get the number of solutions found.
     * @return the number of solutions found
     */
    protected int getSolutionsSize() {
        return solutions.size();
    }

    /**Get the maximum of distance among the solutions found.
     * getting last of solutions as they are sorted in SortesSet
     * @return greatest distance of the solutions
     * @since 10.2
     */
    protected double getMaxDistance() {
        return solutions.last().getSquaredDistance();
    }

    /** Get a reference to the Z  inverse transformation matrix.
     * <p>
     * BEWARE: the returned value is a reference to an internal array,
     * it is <em>only</em> intended for subclasses use (hence the
     * method is protected and not public).
     * BEWARE: for the MODIFIED LAMBDA METHOD, the returned matrix Z is such that
     * Q = Z'L'DLZ where Q is the covariance matrix and ' refers to the transposition operation
     * @return array of integer corresponding to Z matrix
     * @since 10.2
     */
    protected int[] getZInverseTransformationReference() {
        return zInverseTransformation;
    }

    /** Get the size of the problem. In the ILS problem, the integer returned
     * is the size of the covariance matrix.
     * @return the size of the ILS problem
     * @since 10.2
     */
    protected int getSize() {
        return n;
    }

    /** Perform Lᵀ.D.L = Q decomposition of the covariance matrix.
     */
    protected abstract void ltdlDecomposition();

    /** Perform LAMBDA reduction.
     */
    protected abstract void reduction();

    /** Find the best solutions to the Integer Least Square problem.
     */
    protected abstract void discreteSearch();

    /** Inverse the decomposition.
     * <p>
     * This method transforms the Lᵀ.D.L = Q decomposition of covariance into
     * the L⁻¹.D⁻¹.L⁻ᵀ = Q⁻¹ decomposition of the inverse of covariance.
     * </p>
     */
    protected abstract void inverseDecomposition();

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

            // update Z⁻¹ transformation matrix
            for (int i = 0; i < n; ++i) {
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

        // update Z⁻¹ transformation matrix
        for (int i = 0; i < n; ++i) {

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

    /** Recover ambiguities prior to the Z-transformation.
     * @return recovered ambiguities
     */
    protected IntegerLeastSquareSolution[] recoverAmbiguities() {

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
