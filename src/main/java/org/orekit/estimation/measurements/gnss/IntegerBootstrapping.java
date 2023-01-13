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

import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.QRDecomposer;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.special.Erf;
import org.hipparchus.util.FastMath;;

/** Bootstrapping engine for ILS problem solving.
 * This method is base on the following paper: <a
 * href="https://www.researchgate.net/publication/225773077_Success_probability_of_integer_GPS_ambiguity_rounding_and_bootstrapping">
 * Success probability of integer GPs ambiguity rounding and bootstrapping</a> by P. J. G. Teunissen 1998 and
 * <a
 * href="https://repository.tudelft.nl/islandora/object/uuid%3A1a5b8a6e-c9d6-45e3-8e75-48db6d27a523">
 * Influence of ambiguity precision on the success rate of GNSS integer ambiguity bootstrapping</a> by
 * P. J. G. Teunissen 2006.
 * <p>
 *  This method is really faster for integer ambiguity resolution than LAMBDA or MLAMBDA method but its success rate
 *  is really smaller. The method  extends LambdaMethod as it uses LDL' factorization  and reduction methods from LAMBDA method.
 *  The method is really different from LAMBDA as the solution found is not a least-square solution. It is a solution which asses
 *  a probability of success of the solution found. The probability increase with the does with LDL' factorization and reduction
 *  methods.
 *  </p> <p>
 *  If one want to use this method for integer ambiguity resolution, one just need to construct IntegerBootstrapping
 *  only with a double which is the minimal probability of success one wants.
 *  Then from it, one can call the solveILS method.
 *  @author David Soulard
 *  @since 10.2
 */

public class IntegerBootstrapping extends LambdaMethod {

    /** Minimum probability for acceptance. */
    private double minProb;

    /** Upperbound of the probability. */
    private boolean boostrapUse;

    /** Integer ambiguity solution from bootstrap method. */
    private long[]  a_B;

    /** Probability of success of the solution found.*/
    private double p_aB;

    /** Constructor for the bootstrapping ambiguity estimator.
     * @param prob minimum probability acceptance  for the bootstrap
     */
    public IntegerBootstrapping(final double prob) {
        this.minProb = prob;
    }

    /**
     * Compute the solution by the bootstrap method from equation (13) in
     * P.J.G. Teunissen November 2006. The solution is a solution in the
     * distorted space from LdL' and Z transformation.
     */
    @Override
    protected void discreteSearch() {
        //If the probability success upper bound is greater than the min probability, bootstrapUse = true, false otherwise
        this.boostrapUse = upperBoundProbabilitySuccess() > this.minProb;
        //Getter of the diagonal part and lower part of the covariance matrix
        final double[] diag = getDiagReference();
        final double[] low  = getLowReference();
        final int n = diag.length;
        if (boostrapUse) {
            final RealMatrix I = MatrixUtils.createRealIdentityMatrix(n);
            a_B = new long[n];
            final RealMatrix L = getSymmetricMatrixFromLowPart(low);
            final RealMatrix invL_I  = new QRDecomposer(1.0e-10).
                                decompose(L).getInverse().subtract(I);
            final double[] decorrelated = getDecorrelatedReference();
            a_B[0] = (long) FastMath.rint(decorrelated[0]);
            for (int i = 1; i < a_B.length; i++) {
                double a_b = 0;
                for (int j = 0; j < i; j++) {
                    a_b += invL_I.getEntry(i, j) * a_B[j];
                }
                a_B[i] = (long) FastMath.rint(decorrelated[i] + a_b);
            }
            // Compute the probability of correct integer estimation
            p_aB = bootstrappedSuccessRate(diag, a_B);
            if (p_aB > minProb) {
                this.boostrapUse = true;
            } else {
                this.boostrapUse = false;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    protected IntegerLeastSquareSolution[] recoverAmbiguities() {
        if (boostrapUse) {
            // get references to the diagonal and lower triangular parts
            final double[] diag = getDiagReference();
            final int n = diag.length;
            final int[] zInverseTransformation = getZInverseTransformationReference();
            final long[] a = new long[n];
            for (int i = 0; i < n; ++i) {
                // compute a = Z⁻ᵀ.s
                long ai = 0;
                int l = zIndex(0, i);
                for (int j = 0; j < n; ++j) {
                    ai += zInverseTransformation[l] * a_B[j];
                    l  += n;
                }
                a[i] = ai;
            }
            a_B = a;
            final IntegerLeastSquareSolution sol = new IntegerLeastSquareSolution(a_B, p_aB);
            return new IntegerLeastSquareSolution[] {sol};
        }
        else {
            // Return an empty array
            return new IntegerLeastSquareSolution[0];
        }
    }

    /** Return the matrix symmetric from its low triangular part (1 on the diagonal).

     * @param l lower triangular part of the matrix
     * @return matrix
     */
    private RealMatrix getSymmetricMatrixFromLowPart(final double[] l) {
        final double[] diag = getDiagReference();
        final int n = diag.length;
        final RealMatrix L = MatrixUtils.createRealMatrix(n, n);
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < i; j++) {
                L.setEntry(i, j, l[lIndex(i, j)]);
            }
            L.setEntry(i, i, 1.0);
        }
        return L;
    }

    /**Compute the success rate of a bootstraped ILS problem solution.
     * @param D diagonal of the covaraicne matrix
     * @param aB bootstrapped solution
     * @return probability of success
     */
    private double bootstrappedSuccessRate(final double[] D, final long[] aB) {
        double p = 2.0 * phi(1 / (2.0 * D[0]) - 1.0);
        for (int i = 1; i < D.length; i++) {
            p = p * (2.0 * phi(1.0 / (2.0 * D[i])) - 1.0);
        }
        return p;
    }

    /** Compute at point x the the value of phi function.
     * Where phi = 1/2 *(1 + Erf(x/sqrt(2))
     * @param x value at which we compute phi function
     * @return value of phi(x)
     */
    private double phi(final double x) {
        return 0.5 * (1.0 + Erf.erf(x / FastMath.sqrt(2.0)));
    }

    /** Compute the upper bound probability of the ILS problem.
     * @return upper bound probability of the ILS problem
     */
    private double upperBoundProbabilitySuccess() {
        //covariance matrix determinant
        double det = 1;
        final double[] diag = getDiagReference();
        final int n         = diag.length;
        for (double d: diag) {
            det *= d;
        }
        //ADOP: Ambiguity Dilution of Precision
        final double adop = FastMath.pow(det, 1.0 / ((double) 2.0 * n));
        return FastMath.pow(2.0 * phi(1.0 / (2.0 * adop)) - 1.0, n);
    }
}
