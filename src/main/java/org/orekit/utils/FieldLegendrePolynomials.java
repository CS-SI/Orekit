/* Copyright 2002-2024 CS GROUP
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
package org.orekit.utils;

import org.hipparchus.Field;
import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.CombinatoricsUtils;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;

/**
 * Computes the P<sub>nm</sub>(t) coefficients.
 * <p>
 * The computation of the Legendre polynomials is performed following:
 * Heiskanen and Moritz, Physical Geodesy, 1967, eq. 1-62
 * </p>
 * @since 11.0
 * @author Bryan Cazabonne
 * @param <T> type of the field elements
 */
public class FieldLegendrePolynomials<T extends CalculusFieldElement<T>> {

    /** Array for the Legendre polynomials. */
    private T[][] pCoef;

    /** Create Legendre polynomials for the given degree and order.
     * @param degree degree of the spherical harmonics
     * @param order order of the spherical harmonics
     * @param  t argument for polynomials calculation
     */
    public FieldLegendrePolynomials(final int degree, final int order,
                                    final T t) {

        // Field
        final Field<T> field = t.getField();

        // Initialize array
        this.pCoef = MathArrays.buildArray(field, degree + 1, order + 1);

        final T t2 = t.square();

        for (int n = 0; n <= degree; n++) {

            // m shall be <= n (Heiskanen and Moritz, 1967, pp 21)
            for (int m = 0; m <= FastMath.min(n, order); m++) {

                // r = int((n - m) / 2)
                final int r = (int) (n - m) / 2;
                T sum = field.getZero();
                for (int k = 0; k <= r; k++) {
                    final T term = FastMath.pow(t, n - m - 2 * k).
                                   multiply(FastMath.pow(-1.0, k) * CombinatoricsUtils.factorialDouble(2 * n - 2 * k) /
                                                                           (CombinatoricsUtils.factorialDouble(k) * CombinatoricsUtils.factorialDouble(n - k) *
                                                                                           CombinatoricsUtils.factorialDouble(n - m - 2 * k)));
                    sum = sum.add(term);
                }

                pCoef[n][m] = FastMath.pow(t2.negate().add(1.0), 0.5 * m).multiply(FastMath.pow(2, -n)).multiply(sum);

            }

        }

    }

    /** Return the coefficient P<sub>nm</sub>.
     * @param n index
     * @param m index
     * @return The coefficient P<sub>nm</sub>
     */
    public T getPnm(final int n, final int m) {
        return pCoef[n][m];
    }

}
