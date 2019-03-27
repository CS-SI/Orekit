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

/** Decorrelation/reduction engine for {@link LambdaSolver LAMBDA method}.
 * <p>
 * This class implements PJG Teunissen Least Square Ambiguity Decorrelation
 * Adjustment (LAMBDA) method, as described in the 2005 paper <a
 * href="https://www.researchgate.net/publication/225518977_MLAMBDA_a_modified_LAMBDA_method_for_integer_least-squares_estimation">
 * A modified LAMBDA method for integer least-squares estimation</a> by X.-W Chang, X. Yang
 * and T. Zhou, Journal of Geodesy 79(9):552-565, DOI: 10.1007/s00190-005-0004-x
 * </p>
 * @see AmbiguitySolver
 * @author Luc Maisonobe
 * @since 10.0
 */
class LambdaMethod extends AbstractLambdaMethod {

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

}
