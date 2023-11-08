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

import java.util.SortedMap;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.util.MathArrays;
import org.orekit.propagation.semianalytical.dsst.utilities.CoefficientsFactory.NSKey;

/** Compute the L<sub>n</sub><sup>s</sup>(γ).
 *  <p>
 *  The fomula used is: <br>
 *  L<sub>n</sub><sup>s</sup>(γ) = ( R / a )<sup>n</sup>V<sub>ns</sub>Q<sup>ns</sup>(γ)
 *  </p>
 *  @author Lucian Barbulescu
 * @param <T> type of the field elements
 */
public class FieldLnsCoefficients <T extends CalculusFieldElement<T>> {

    /** The coefficients L<sub>n</sub><sup>s</sup>(γ). */
    private final T[][] lns;

    /** The coefficients dL<sub>n</sub><sup>s</sup>(γ) / dγ. */
    private final T[][] dlns;

    /** Create a set of L<sub>n</sub><sup>s</sup>(γ) coefficients.
    *
    * @param nMax maximum value for n
    * @param sMax maximum value for s
    * @param Qns the Q<sup>ns</sup>(γ) coefficients
    * @param Vns the V<sub>ns</sub> coefficients
    * @param roa (R / a)
    * @param field field used by default
    */
    public FieldLnsCoefficients(final int nMax, final int sMax,
                                final T[][] Qns, final SortedMap<NSKey, Double> Vns, final T roa,
                                final Field<T> field) {
        final T zero      = field.getZero();
        final int rows    = nMax + 1;
        final int columns = sMax + 1;
        this.lns          = MathArrays.buildArray(field, rows, columns);
        this.dlns         = MathArrays.buildArray(field, rows, columns);

        final T[] roaPow = MathArrays.buildArray(field, rows);
        roaPow[0] = zero.add(1.);
        for (int i = 1; i <= nMax; i++) {
            roaPow[i] = roa.multiply(roaPow[i - 1]);
        }
        for (int s = 0; s <= sMax; s++) {
            for (int n = s; n <= nMax; n++) {
                // if (n - s) is not even L<sub>n</sub><sup>s</sup>(γ) is 0
                if ((n - s) % 2 == 0) {
                    final T coef = roaPow[n].multiply(Vns.get(new NSKey(n, s)));
                    lns[n][s] = coef.multiply(Qns[n][s]);
                    if ( n == s) {
                        // if n == s the derivative is 0 because Q[n][s+1] == Q[n][n+1] is 0
                        dlns[n][s] = zero;
                    } else {
                        dlns[n][s] = coef.multiply(Qns[n][s + 1]);
                    }
                } else {
                    lns[n][s]  = zero;
                    dlns[n][s] = zero;
                }
            }
        }

    }

   /**Get the value of L<sub>n</sub><sup>s</sup>(γ).
    *
    * @param n n index
    * @param s s index
    * @return L<sub>n</sub><sup>s</sup>(γ)
    */
    public T getLns(final int n, final int s) {
        return lns[n][s];
    }

   /**Get the value of dL<sub>n</sub><sup>s</sup> / dγ (γ).
    *
    * @param n n index
    * @param s s index
    * @return L<sub>n</sub><sup>s</sup>(γ)
    */
    public T getdLnsdGamma(final int n, final int s) {
        return dlns[n][s];
    }
}
