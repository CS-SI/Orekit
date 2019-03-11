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
package org.orekit.estimation.measurements;

import java.util.List;

import org.hipparchus.linear.RealMatrix;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;

/** Lambda method for integer ambiguity solving.
 * @author Luc Maisonobe
 * @since 10.0
 */
public class LambdaSolver extends AmbiguitySolver {

    /** Simple constructor.
     * @param ambiguityDrivers drivers for ambiguity parameters
     */
    public LambdaSolver(final List<ParameterDriver> ambiguityDrivers) {
        super(ambiguityDrivers);
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> fixIntegerAmbiguities(final int startIndex,
                                                       final ParameterDriversList measurementsParametersDrivers,
                                                       final RealMatrix covariance) {

        // LT.D.L decomposition of ambiguity covariances
        final LTDL ltdl = new LTDL(getAmbiguityIndirection(startIndex, measurementsParametersDrivers), covariance);

        // TODO
        return null;

    }

    /** LT.D.L decomposition of a symmetric matrix. */
    private static class LTDL {

        /** Lower triangular matrix with unit diagonal, in row order. */
        private final double[] low;

        /** Diagonal matrix. */
        private final double[] diag;

        /** Simple constructor.
         * @param indirection indirection array to extract ambiguity parameters
         * @param covariance full covariance matrix
         */
        LTDL(final int[] indirection, final RealMatrix covariance) {

            final int n = indirection.length;
            low  = new double[(n * (n - 1)) / 2];
            diag = new double[n];

            // initialize decomposition matrices
            int kL = 0;
            int kD = 0;
            for (int i = 0; i < n; ++i) {
                for (int j = 0; j < i; ++j) {
                    low[kL++] = covariance.getEntry(indirection[i], indirection[j]);
                }
                diag[kD++] = covariance.getEntry(indirection[i], indirection[i]);
            }

            // perform decomposition
            for (int k = n - 1; k >= 0; --k) {
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

        /** Get the index of an entry in the lower triangular matrix.
         * @param row row index (counting from 0)
         * @param col column index (counting from 0)
         * @return index in the single dimension array
         */
        private int lIndex(final int row, final int col) {
            return (row * (row - 1)) / 2 + col;
        }

    }

}
