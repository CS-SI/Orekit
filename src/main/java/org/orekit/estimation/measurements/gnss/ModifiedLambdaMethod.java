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

import org.hipparchus.util.FastMath;

/** Decorrelation/reduction engine for Modified LAMBDA method.
 * <p>
 * This class implements Modified Least Square Ambiguity Decorrelation
 * Adjustment (MLAMBDA) method, as described in <a
 * href="https://www.researchgate.net/publication/225518977_MLAMBDA_a_modified_LAMBDA_method_for_integer_least-squares_estimation">
 * A modified LAMBDA method for integer least-squares estimation</a> by X.-W Chang, X. Yang
 * and T. Zhou, Journal of Geodesy 79(9):552-565, DOI: 10.1007/s00190-005-0004-x
 * </p>
 *
 * @see AmbiguitySolver
 * @author David Soulard
 * @since 10.2
 */
public class ModifiedLambdaMethod extends AbstractLambdaMethod {

    /** Empty constructor.
     * <p>
     * This constructor is not strictly necessary, but it prevents spurious
     * javadoc warnings with JDK 18 and later.
     * </p>
     * @since 12.0
     */
    public ModifiedLambdaMethod() {
        // nothing to do
    }

    /** Compute the LᵀDL factorization with symmetric pivoting decomposition of Q
     * (symmetric definite positive matrix) with a minimum symmetric pivoting: Q = ZᵀLᵀDLZ.
     */
    @Override
    protected void ltdlDecomposition() {
        final double[] diag = getDiagReference();
        final double[] low  = getLowReference();
        final int[] Z = getZInverseTransformationReference();
        final double[] aNew = getDecorrelatedReference();
        for (int  i = 0; i < this.getSize(); i++) {
            Z[zIndex(i, i)] = 0;
        }
        final int n = diag.length;
        final int[] perm = new int[n];
        for (int i = 0; i < n; i++) {
            perm[i] = i;
        }
        for (int k = n - 1; k >= 0; k--) {
            final int q = posMin(diag, k);
            exchangeIntP1WithIntP2(perm, k, q);
            permRowThenCol(low, diag, k, q);
            if (k > 0) {
                final double Wkk = diag[k];
                divide(low, Wkk, k);
                set(low, diag, k);
            }
            exchangeDoubleP1WithDoubleP2(aNew, k, q);
        }
        for (int j = 0; j < n; j++) {
            Z[zIndex(j, perm[j])] = 1;
        }
    }

    /** Perform MLAMBDA reduction.
     */
    @Override
    protected void reduction() {
        final double[] diag = getDiagReference();
        final double[] low  = getLowReference();
        final int n = diag.length;
        int k = getSize() - 2;
        while ( k > -1) {
            final int kp1 = k + 1;
            double tmp = low[lIndex(kp1, k)];
            final double mu = FastMath.rint(tmp);
            if (Math.abs(mu) > 1e-9) {
                tmp -= mu;
            }
            final double delta = diag[k] + tmp * tmp * diag[kp1];
            if (delta < diag[kp1]) {
                integerGaussTransformation(kp1, k);
                if (mu > 0) {
                    for (int i = k + 2; i < n; i++) {
                        integerGaussTransformation(i, k);
                    }
                }
                permutation(k, delta);
                if (k < n - 2) {
                    k++;
                }
            } else {
                k--;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void discreteSearch() {
        //initialization
        final int n                 = getSize();
        final int maxSolutions      = getMaxSolution();
        final double[] diag         = getDiagReference();
        final double[] decorrelated = getDecorrelatedReference();
        final double[] low          = getLowReference();
        final long[] z              = new long[n];
        final double[] zb           = new double[n];
        final double[] y            = new double[n];
        final int[] path            = new int[n];

        for (int i = 0; i < n; i++) {
            path[i] = n - 1;
        }

        final double[] step    = new double[n];
        final double[] dist    = new double[n + 1];
        final double[] lS      = new double[(n * (n - 1)) / 2];
        final double[] dS      = new double[n];
        double maxDist         = Double.POSITIVE_INFINITY;
        int count              = 0;
        boolean endSearch      = false;
        int ulevel             = 0;

        // Determine which level to move to after z(0) is chosen at level 1.
        final int k0;
        if (maxSolutions == 1) {
            k0 = 1;
        }
        else {
            k0 = 0;
        }

        // Initialization at level n
        zb[n - 1] = decorrelated.clone()[n - 1];
        z[n -  1] = (long) FastMath.rint(zb[n - 1]);
        y[n - 1] = zb[n - 1] - z[n - 1];
        step[n - 1] =  sign(y[n - 1]);
        int k = n - 1;
        while (!endSearch) {
            for (int i = ulevel; i <= k - 1; i++) {
                path[i] = k;
            }
            for (int j = ulevel - 1; j >= 0; j--) {
                if (path[j] < k) {
                    path[j] = k;
                } else {
                    break;
                }
            }
            double newDist = dist[k] + y[k] * y[k] / diag[k];
            while (newDist < maxDist) {
                // move to level k-1
                if (k != 0) {
                    k--;
                    dist[k] = newDist;
                    for (int j = path[k]; j > k; j--) {
                        if (j - 1 == k) {
                            //Update diagonal
                            dS[k] = lS[lIndex(j, k)] - y[j] * low[lIndex(j, k)];
                        } else {
                            //Update low triangular part
                            lS[lIndex(j - 1, k)] = lS[lIndex(j, k)] - y[j] * low[lIndex(j, k)];
                        }
                    }

                    zb[k] = decorrelated[k] + dS[k];
                    z[k] =  (long) FastMath.rint(zb[k]);
                    y[k] = zb[k] - z[k];
                    step[k] =  sign(y[k]);
                } else {
                    //Save the value of one optimum z
                    if (count < (maxSolutions - 1)) {
                        addSolution(z, newDist);
                        count++;
                    //Save the value of one optimum z
                    } else if (count == (maxSolutions - 1)) {
                        addSolution(z, newDist);
                        maxDist = getMaxDistance();
                        count++;
                    //Replace the new solution with the one with the greatest distance
                    } else {
                        removeSolution();
                        addSolution(z, newDist);
                        maxDist = getMaxDistance();
                    }
                    k = k0;
                    z[k] = z[k] + (long) step[k];
                    y[k] = zb[k] - z[k];
                    step[k] = -step[k] -  sign(step[k]);
                }
                newDist = dist[k] + y[k] * y[k] / diag[k];
            }
            ulevel = k;
            //exit or move to level k+1
            while (newDist >= maxDist) {
                if (k == n - 1) {
                    endSearch = true;
                    break;
                }
                //move to level k+1
                k++;
                //next integer
                z[k] = z[k] + (long) step[k];
                y[k] = zb[k] - z[k];
                step[k] = -step[k] -  sign(step[k]);
                newDist = dist[k] + y[k] * y[k] / diag[k];
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void inverseDecomposition() {
        // nothing for M-LAMBDA method
    }

    /** Return the position of the smallest element in the diagonal of a matrix given in parameter.
     * @param k the position of the smallest diagonal element
     * @param D an array of double being the diagonal of the covariance matrix
     * @return the position of the smallest element of mat.
     */
    private int posMin(final double[] D, final int k) {
        int q = 0;
        double value = D[0];
        for (int i = 1; i <= k; i++) {
            if (value > D[i]) {
                value = D[i];
                q = i;
            }
        }
        return q;
    }

    /** Perform the following operation on the matrix W in parameters.
     * W(1:p-1,1:p-1) = W(1:p-1,1:p-1) - W(p,1:p-1)'*W(p,p)*W(p,1:p-1);
     * @param L array of double being a lower triangular part of the covariance matrix
     * @param D array of double being the diagonal of the covariance matrix
     * @param p integer at which the computation is done
     */
    private void set(final double[] L, final double[] D, final int p) {
        final double d = D[p];
        final double[] row = new double[p];
        for (int i = 0; i < p; i++) {
            row[i] = L[lIndex(p, i)];
        }
        for (int i = 0; i < p; i++) {
            for (int j = 0; j < i; j++) {
                L[lIndex(i, j)] = L[lIndex(i, j)] - row[i] * row[j] * d;
            }
            D[i] = D[i] - row[i] * row[i] * d;
        }
    }

    /** Perform a permutation between two row and then two column of the covariance matrix.
     * @param L array of double being the lower triangular part of the covariance matrix
     * @param D array of double being the diagonal of the covariance matrix
     * @param p1 integer: position of the permutation
     * @param p2 integer: position of the permutation
     */
    private void permRowThenCol(final double[] L, final double[] D, final int p1, final int p2) {
        final double[] rowP1 = getRow(L, D, p1);
        final double[] rowP2 = getRow(L, D, p2);
        if (p1 > p2) {
            //Update row P2
            for (int j = 0; j < p2; j++) {
                L[lIndex(p2, j)] = rowP1[j];
            }
            D[p2] = rowP1[p2];
            //Update row p1
            for (int j = 0; j < p1; j++) {
                L[lIndex(p1, j)] = rowP2[j];
            }
            D[p1] = rowP2[p1];
            final double[] colP1 = getColPmax(L, D, rowP1, p2, p1);
            //Update column P1
            for (int i = p1 + 1; i < D.length; i++) {
                L[lIndex(i, p1)] = L[lIndex(i, p2)];
            }
            D[p1] = L[lIndex(p1, p2)];
            //Update column P2
            for (int i = p2 + 1; i < D.length; i++) {
                L[lIndex(i, p2)] = colP1[i];
            }
            D[p2] = colP1[p2];
        } else {
            //Does nothing when p1 <= p2
        }
    }

    /**Get the row of the covariance matrix at the given position (count from 0).
     * @param L lower part of the covariance matrix
     * @param D diagonal part of the covariance matrix
     * @param pos wanted position
     * @return array of double being the row of covariance matrix at given position
     */
    private double[] getRow(final double[] L, final double[] D, final int pos) {
        final double[] row = new double[D.length];
        for (int j = 0; j < pos; j++) {
            row[j] = L[lIndex(pos, j)];
        }
        row[pos] = D[pos];
        for (int j = pos + 1; j < D.length; j++) {
            row[j] = L[lIndex(j, pos)];
        }
        return row;
    }

    /**Getter for column the greatest at the right side.
     * @param L double array being the lower triangular matrix
     * @param D double array being the diagonal matrix
     * @param row double array being the row of the matrix at given position
     * @param pmin position at which we get the column
     * @param pmax other positions
     * @return array of double
     */
    private double[] getColPmax(final double[] L, final double[] D, final double[] row, final int pmin, final int pmax) {
        final double[] col = new double[D.length];
        col[pmin] = row[pmax];
        for (int i = pmin + 1; i < pmax; i++) {
            col[i] = row[i];
        }
        col[pmax] = D[pmax];
        for (int i = pmax + 1; i < D.length; i++) {
            col[i] = L[lIndex(i, pmax)];
        }
        return col;
    }

    /** Perform the following operation:  W(k,1:k-1) = W(k,1:k-1)/W(k,k) where W is the covariance matrix.
     * @param mat lower triangular part of the covaraince matrix
     * @param diag double: value of the covaraicne matrix at psoition (k;k)
     * @param k integer needed
     */
    private void divide(final double[] mat, final double diag, final int k) {
        for (int j = 0; j < k; j++) {
            mat[lIndex(k, j)] = mat[lIndex(k, j)] / diag;
        }
    }

    /**Inverse the position of 2 element of the array in parameters.
     * @param mat array on which change should be done
     * @param p1 position of the first element to change
     * @param p2 position of the second element to change
     * @return
     */
    private void  exchangeIntP1WithIntP2(final int[] mat, final int p1, final int p2) {
        final int[] Z = mat.clone();
        mat[p1] = Z[p2];
        mat[p2] = Z[p1];
    }

    /** On the array of double mat exchange the element at position p1 with the one at position p2.
     * @param mat array on which the exchange is performed
     * @param p1 first position of exchange (0 is the first element)
     * @param p2 second position of exchange (0 is the first element)
     */
    private void exchangeDoubleP1WithDoubleP2(final double[] mat, final int p1, final int p2) {
        final double[] a = mat.clone();
        mat[p1] = a[p2];
        mat[p2] = a[p1];
    }

    /** Return the symbol of parameter a.
     * @param a the double for which we want the want the symbol
     * @return -1.0 if a is lower than or equal to 0 or 1.0 if a is greater than 0
     */
    protected double sign(final double a) {
        return (a <= 0.0) ? -1.0 : ((a > 0.0) ? 1.0 : a);
    }

}
