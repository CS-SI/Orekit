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

import java.util.SortedSet;
import java.util.TreeSet;

import org.hipparchus.util.FastMath;

/** Decorrelation/reduction engine for LAMBDA method.
 * <p>
 * This class implements PJG Teunissen Least Square Ambiguity Decorrelation
 * Adjustment (LAMBDA) method, as described in both the 1996 paper <a
 * href="https://www.researchgate.net/publication/2790708_The_LAMBDA_method_for_integer_ambiguity_estimation_implementation_aspects">
 * The LAMBDA method for integer ambiguity estimation: implementation aspects</a> by
 * Paul de Jonge and Christian Tiberius and on the 2005 paper <a
 * href="https://www.researchgate.net/publication/225518977_MLAMBDA_a_modified_LAMBDA_method_for_integer_least-squares_estimation">
 * A modified LAMBDA method for integer least-squares estimation</a> by X.-W Chang, X. Yang
 * and T. Zhou, Journal of Geodesy 79(9):552-565, DOI: 10.1007/s00190-005-0004-x
 * </p>
 * <p>
 * It slightly departs on the original LAMBDA method as it does implement
 * the following improvements proposed in the de Jonge and Tiberius 1996 paper
 * that vastly speed up the search:
 * </p>
 * <ul>
 *   <li>alternate search starting from the middle and expanding outwards</li>
 *   <li>automatic shrinking of ellipsoid during the search</li>
 * </ul>
 * @see AmbiguitySolver
 * @author Luc Maisonobe
 * @since 10.0
 */
public class LambdaMethod extends AbstractLambdaMethod {

    /** Margin factor to apply to estimated search limit parameter. */
    private static final double CHI2_MARGIN_FACTOR = 1.1;

    /** Empty constructor.
     * <p>
     * This constructor is not strictly necessary, but it prevents spurious
     * javadoc warnings with JDK 18 and later.
     * </p>
     * @since 12.0
     */
    public LambdaMethod() {
        // nothing to do
    }

    /** {@inheritDoc} */
    @Override
    protected void ltdlDecomposition() {

        // get references to the diagonal and lower triangular parts
        final double[] diag = getDiagReference();
        final double[] low  = getLowReference();

        // perform Lᵀ.D.L decomposition
        for (int k = diag.length - 1; k >= 0; --k) {
            final double inv = 1.0 / diag[k];
            for (int i = 0; i < k; ++i) {
                final double lki = low[lIndex(k, i)];
                for (int j = 0; j < i; ++j) {
                    low[lIndex(i, j)] -= lki * low[lIndex(k, j)] * inv;
                }
                diag[i] -= lki * lki * inv;
            }
            for (int j = 0; j < k; ++j) {
                low[lIndex(k, j)] *= inv;
            }
        }

    }

    /** {@inheritDoc} */
    @Override
    protected void reduction() {

        // get references to the diagonal and lower triangular parts
        final double[] diag = getDiagReference();
        final double[] low  = getLowReference();
        final int n = diag.length;

        int kSaved = n - 2;
        int k0 = kSaved;
        while (k0 >= 0) {
            final int k1 = k0 + 1;
            if (k0 <= kSaved) {
                for (int i = k0 + 1; i < n; ++i) {
                    integerGaussTransformation(i, k0);
                }
            }
            final double lk1k0 = low[lIndex(k1, k0)];
            final double delta = diag[k0] + lk1k0 * lk1k0 * diag[k1];
            if (delta < diag[k1]) {
                permutation(k0, delta);
                kSaved = k0;
                k0 = n - 2;
            } else {
                k0--;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void discreteSearch() {

        // get references to the diagonal part
        final double[] diag = getDiagReference();
        final int n = diag.length;

        // estimate search domain limit
        final long[]   fixed   = new long[n];
        final double[] offsets = new double[n];
        final double[] left    = new double[n];
        final double[] right   = new double[n];

        final int maxSolutions = getMaxSolution();
        final double[] decorrelated = getDecorrelatedReference();

        final AlternatingSampler[] samplers = new AlternatingSampler[n];

        // set up top level sampling for last ambiguity
        double chi2 = estimateChi2(fixed, offsets);
        right[n - 1] = chi2 / diag[n - 1];
        int index = n - 1;
        while (index < n) {
            if (index == -1) {

                // all ambiguities have been fixed
                final double squaredNorm = chi2 - diag[0] * (right[0] - left[0]);
                addSolution(fixed, squaredNorm);
                final int size = getSolutionsSize();
                if (size >= maxSolutions) {

                    // shrink the ellipsoid
                    chi2 = squaredNorm;
                    right[n - 1] = chi2 / diag[n - 1];
                    for (int i = n - 1; i > 0; --i) {
                        samplers[i].setRadius(FastMath.sqrt(right[i]));
                        right[i - 1] = diag[i] / diag[i - 1] * (right[i] - left[i]);
                    }
                    samplers[0].setRadius(FastMath.sqrt(right[0]));

                    if (size > maxSolutions) {
                        removeSolution();
                    }

                }

                ++index;

            } else {

                if (samplers[index] == null) {
                    // we start exploring a new ambiguity
                    samplers[index] = new AlternatingSampler(conditionalEstimate(index, offsets), FastMath.sqrt(right[index]));
                } else {
                    // continue exploring the same ambiguity
                    samplers[index].generateNext();
                }

                if (samplers[index].inRange()) {
                    fixed[index]       = samplers[index].getCurrent();
                    offsets[index]     = fixed[index] - decorrelated[index];
                    final double delta = fixed[index] - samplers[index].getMidPoint();
                    left[index]        = delta * delta;
                    if (index > 0) {
                        right[index - 1]   = diag[index] / diag[index - 1] * (right[index] - left[index]);
                    }

                    // go down one level
                    --index;

                } else {

                    // we have completed exploration of this ambiguity range
                    samplers[index] = null;

                    // go up one level
                    ++index;

                }

            }
        }

    }

    /** {@inheritDoc} */
    @Override
    protected void inverseDecomposition() {

        // get references to the diagonal and lower triangular parts
        final double[] diag = getDiagReference();
        final double[] low  = getLowReference();
        final int n = diag.length;

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

        // get references to the diagonal part
        final double[] diag = getDiagReference();
        final int n = diag.length;
        // maximum number of solutions seeked
        final int maxSolutions = getMaxSolution();
        // get references to the decorrelated ambiguities
        final double[] decorrelated = getDecorrelatedReference();


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
            final int remaining = FastMath.min(n, maxSolutions - squaredNorms.size());
            for (int i = 0; i < remaining; ++i) {
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
        // get references to the lower triangular part and the decorrelated ambiguities
        final double[] diag = getDiagReference();
        final double[] decorrelated = getDecorrelatedReference();
        double n2 = 0;
        for (int i = diag.length - 1; i >= 0; --i) {
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
        // get references to the diagonal and lower triangular parts
        final double[] diag = getDiagReference();
        final double[] low  = getLowReference();
        // get references to the decorrelated ambiguities
        final double[] decorrelated = getDecorrelatedReference();

        double conditional = decorrelated[i];
        for (int j = i + 1; j < diag.length; ++j) {
            conditional -= low[lIndex(j, i)] * offsets[j];
        }
        return conditional;
    }

}
