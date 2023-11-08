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
package org.orekit.propagation.semianalytical.dsst.utilities;

import java.util.Arrays;

import org.hipparchus.Field;
import org.hipparchus.CalculusFieldElement;
import org.hipparchus.fraction.BigFraction;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;

/** Compute the &Gamma;<sup>m</sup><sub>n,s</sub>(γ) function from equation 2.7.1-(13).
 * @param <T> type of the field elements
 */
public class FieldGammaMnsFunction <T extends CalculusFieldElement<T>> {

    /** Factorial ratios. */
    private static double[] PRECOMPUTED_RATIOS;

    /** Field element. */
    private final Field<T> field;

    /** Factorial ratios. */
    private final double[] ratios;

    /** Storage array. */
    private final T[] values;

    /** 1 + I * γ. */
    private final T opIg;

    /** I = +1 for a prograde orbit, -1 otherwise. */
    private final int    I;

    /** Simple constructor.
     *  @param nMax max value for n
     *  @param gamma γ
     *  @param I retrograde factor
     *  @param field field element
     */
    public FieldGammaMnsFunction(final int nMax, final T gamma, final int I, final Field<T> field) {
        this.field = field;
        final T zero = field.getZero();
        final int size = (nMax + 1) * (nMax + 2) * (4 * nMax + 3) / 6;
        this.values = MathArrays.buildArray(field, size);
        this.ratios = getRatios(nMax, size);
        Arrays.fill(values, zero.add(Double.NaN));
        this.opIg   = gamma.multiply(I).add(1.);
        this.I      = I;
    }

    /** Compute the array index.
     *  @param m m
     *  @param n n
     *  @param s s
     *  @return index for element m, n, s
     */
    private static int index(final int m, final int n, final int s) {
        return n * (n + 1) * (4 * n - 1) / 6 + // index for 0, n, 0
               m * (2 * n + 1) +               // index for m, n, 0
               s + n;                          // index for m, n, s
    }

    /** Get the ratios for the given size.
     * @param nMax max value for n
     * @param size ratio size array
     * @return factorial ratios
     */
    private static double[] getRatios(final int nMax, final int size) {
        synchronized (GammaMnsFunction.class) {
            if (PRECOMPUTED_RATIOS == null || PRECOMPUTED_RATIOS.length < size) {
                // we need to compute a larger reference array

                final BigFraction[] bF = new BigFraction[size];
                for (int n = 0; n <= nMax; ++n) {

                    // populate ratios for s = 0
                    bF[index(0, n, 0)] = BigFraction.ONE;
                    for (int m = 1; m <= n; ++m) {
                        bF[index(m, n, 0)] = bF[index(m - 1, n, 0)].multiply(n + m).divide(n - (m - 1));
                    }

                    // populate ratios for s != 0
                    for (int absS = 1; absS <= n; ++absS) {
                        for (int m = 0; m <= n; ++m) {
                            bF[index(m, n, +absS)] = bF[index(m, n, absS - 1)].divide(n + absS).multiply(n - (absS - 1));
                            bF[index(m, n, -absS)] = bF[index(m, n, absS)];
                        }
                    }

                }

                // convert to double
                PRECOMPUTED_RATIOS = new double[size];
                for (int i = 0; i < bF.length; ++i) {
                    PRECOMPUTED_RATIOS[i] = bF[i].doubleValue();
                }

            }
            return PRECOMPUTED_RATIOS;
        }
    }

    /** Get &Gamma; function value.
     *  @param m m
     *  @param n n
     *  @param s s
     *  @return &Gamma;<sup>m</sup><sub>n, s</sub>(γ)
     */
    public T getValue(final int m, final int n, final int s) {
        final int i = index(m, n, s);
        if (Double.isNaN(values[i].getReal())) {
            if (s <= -m) {
                values[i] = FastMath.scalb(FastMath.pow(opIg, -I * m), s).multiply(((m - s) & 0x1) == 0 ? +1 : -1);
            } else if (s <= m) {
                values[i] = FastMath.scalb(FastMath.pow(opIg, I * s), -m).multiply(ratios[i]).multiply(((m - s) & 0x1) == 0 ? +1 : -1);
            } else {
                values[i] = FastMath.scalb(FastMath.pow(opIg, I * m), -s);
            }
        }
        return values[i];
    }

    /** Get &Gamma; function derivative.
     * @param m m
     * @param n n
     * @param s s
     * @return d&Gamma;<sup>m</sup><sub>n,s</sub>(γ)/dγ
     */
    public T getDerivative(final int m, final int n, final int s) {
        final T zero = field.getZero();
        T res = zero;
        if (s <= -m) {
            res = getValue(m, n, s).multiply(I).multiply(-m).divide(opIg);
        } else if (s >= m) {
            res =  getValue(m, n, s).multiply(I).multiply(m).divide(opIg);;
        } else {
            res =  getValue(m, n, s).multiply(I).multiply(s).divide(opIg);;
        }
        return res;
    }

}
