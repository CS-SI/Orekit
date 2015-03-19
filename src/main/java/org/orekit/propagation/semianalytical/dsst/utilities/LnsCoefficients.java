/* Copyright 2002-2015 CS Systèmes d'Information
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
package org.orekit.propagation.semianalytical.dsst.utilities;

import java.util.TreeMap;

import org.orekit.propagation.semianalytical.dsst.utilities.CoefficientsFactory.NSKey;

/** Compute the L<sub>n</sub><sup>s</sup>(γ).
 *  <p>
 *  The fomula used is: <br/>
 *  L<sub>n</sub><sup>s</sup>(γ) = ( R / a )<sup>n</sup>V<sub>ns</sub>Q<sup>ns</sup>(γ)
 *  </p>
 *  @author Lucian Barbulescu
 */
public class LnsCoefficients {

    /** The coefficients L<sub>n</sub><sup>s</sup>(γ). */
    private final double[][] lns;

    /** The coefficients dL<sub>n</sub><sup>s</sup>(γ) / dγ. */
    private final double[][] dlns;

    /** Create a set of L<sub>n</sub><sup>s</sup>(γ) coefficients.
     *
     * @param nMax maximum value for n
     * @param sMax maximum value for s
     * @param Qns the Q<sup>ns</sup>(γ) coefficients
     * @param Vns the V<sub>ns</sub> coefficients
     * @param roa (R / a)
     */
    public LnsCoefficients(final int nMax, final int sMax,
            final double[][] Qns, final TreeMap<NSKey, Double> Vns, final double roa) {
        this.lns = new double[nMax + 1][sMax + 1];
        this.dlns = new double[nMax + 1][sMax + 1];

        final double[] roaPow = new double[nMax + 1];
        roaPow[0] = 1.;
        for (int i = 1; i <= nMax; i++) {
            roaPow[i] = roa * roaPow[i - 1];
        }
        for (int s = 0; s <= sMax; s++) {
            for (int n = s; n <= nMax; n++) {
                // if (n - s) is not even L<sub>n</sub><sup>s</sup>(γ) is 0
                if ((n - s) % 2 == 0) {
                    final double coef = roaPow[n] * Vns.get(new NSKey(n, s));
                    lns[n][s] = coef * Qns[n][s];
                    if ( n == s) {
                        // if n == s the derivative is 0 because Q[n][s+1] == Q[n][n+1] is 0
                        dlns[n][s] = 0;
                    } else {
                        dlns[n][s] = coef * Qns[n][s + 1];
                    }
                } else {
                    lns[n][s] = 0.;
                    dlns[n][s] = 0;
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
    public double getLns(final int n, final int s) {
        return lns[n][s];
    }

    /**Get the value of dL<sub>n</sub><sup>s</sup> / dγ (γ).
     *
     * @param n n index
     * @param s s index
     * @return L<sub>n</sub><sup>s</sup>(γ)
     */
    public double getdLnsdGamma(final int n, final int s) {
        return dlns[n][s];
    }
}
