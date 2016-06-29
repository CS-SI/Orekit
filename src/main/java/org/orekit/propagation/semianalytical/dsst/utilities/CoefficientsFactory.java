/* Copyright 2002-2016 CS Systèmes d'Information
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

import org.hipparchus.util.CombinatoricsUtils;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

/**
 * This class is designed to provide coefficient from the DSST theory.
 *
 * @author Romain Di Costanzo
 */
public class CoefficientsFactory {

    /** Internal storage of the polynomial values. Reused for further computation. */
    private static TreeMap<NSKey, Double> VNS = new TreeMap<NSKey, Double>();

    /** Last computed order for V<sub>ns</sub> coefficients. */
    private static int         LAST_VNS_ORDER = 2;

    /** Static initialization for the V<sub>ns</sub> coefficient. */
    static {
        // Initialization
        VNS.put(new NSKey(0, 0), 1.);
        VNS.put(new NSKey(1, 0), 0.);
        VNS.put(new NSKey(1, 1), 0.5);
    }

    /** Private constructor as the class is a utility class.
     */
    private CoefficientsFactory() {
    }

    /** Compute the Q<sub>n,s</sub> coefficients evaluated at γ from the recurrence formula 2.8.3-(2).
     *  <p>
     *  Q<sub>n,s</sub> coefficients are computed for n = 0 to nMax
     *  and s = 0 to sMax + 1 in order to also get the derivative dQ<sub>n,s</sub>/dγ = Q(n, s + 1)
     *  </p>
     *  @param gamma γ angle
     *  @param nMax n max value
     *  @param sMax s max value
     *  @return Q<sub>n,s</sub> coefficients array
     */
    public static double[][] computeQns(final double gamma, final int nMax, final int sMax) {

        // Initialization
        final int sDim = FastMath.min(sMax + 1, nMax) + 1;
        final int rows = nMax + 1;
        final double[][] Qns = new double[rows][];
        for (int i = 0; i <= nMax; i++) {
            final int snDim = FastMath.min(i + 1, sDim);
            Qns[i] = new double[snDim];
        }

        // first element
        Qns[0][0] = 1;

        for (int n = 1; n <= nMax; n++) {
            final int snDim = FastMath.min(n + 1, sDim);
            for (int s = 0; s < snDim; s++) {
                if (n == s) {
                    Qns[n][s] = (2. * s - 1.) * Qns[s - 1][s - 1];
                } else if (n == (s + 1)) {
                    Qns[n][s] = (2. * s + 1.) * gamma * Qns[s][s];
                } else {
                    Qns[n][s] = (2. * n - 1.) * gamma * Qns[n - 1][s] - (n + s - 1.) * Qns[n - 2][s];
                    Qns[n][s] /= n - s;
                }
            }
        }

        return Qns;
    }

    /** Compute recursively G<sub>s</sub> and H<sub>s</sub> polynomials from equation 3.1-(5).
     *  @param k x-component of the eccentricity vector
     *  @param h y-component of the eccentricity vector
     *  @param alpha 1st direction cosine
     *  @param beta 2nd direction cosine
     *  @param order development order
     *  @return Array of G<sub>s</sub> and H<sub>s</sub> polynomials for s from 0 to order.<br>
     *          The 1st column contains the G<sub>s</sub> values.
     *          The 2nd column contains the H<sub>s</sub> values.
     */
    public static double[][] computeGsHs(final double k, final double h,
                                         final double alpha, final double beta,
                                         final int order) {
        // Constant terms
        final double hamkb = h * alpha - k * beta;
        final double kaphb = k * alpha + h * beta;
        // Initialization
        final double[][] GsHs = new double[2][order + 1];
        GsHs[0][0] = 1.;
        GsHs[1][0] = 0.;

        for (int s = 1; s <= order; s++) {
            // Gs coefficient
            GsHs[0][s] = kaphb * GsHs[0][s - 1] - hamkb * GsHs[1][s - 1];
            // Hs coefficient
            GsHs[1][s] = hamkb * GsHs[0][s - 1] + kaphb * GsHs[1][s - 1];
        }

        return GsHs;
    }

    /** Compute the V<sub>n,s</sub> coefficients from 2.8.2-(1)(2).
     * @param order Order of the computation. Computation will be done from 0 to order -1
     * @return Map of the V<sub>n, s</sub> coefficients
     */
    public static TreeMap<NSKey, Double> computeVns(final int order) {

        if (order > LAST_VNS_ORDER) {
            // Compute coefficient
            // Need previous computation as recurrence relation is done at s + 1 and n + 2
            final int min = (LAST_VNS_ORDER - 2 < 0) ? 0 : (LAST_VNS_ORDER - 2);
            for (int n = min; n < order; n++) {
                for (int s = 0; s < n + 1; s++) {
                    if ((n - s) % 2 != 0) {
                        VNS.put(new NSKey(n, s), 0.);
                    } else {
                        // s = n
                        if (n == s && (s + 1) < order) {
                            VNS.put(new NSKey(s + 1, s + 1), VNS.get(new NSKey(s, s)) / (2 * s + 2.));
                        }
                        // otherwise
                        if ((n + 2) < order) {
                            VNS.put(new NSKey(n + 2, s), VNS.get(new NSKey(n, s)) * (-n + s - 1.) / (n + s + 2.));
                        }
                    }
                }
            }
            LAST_VNS_ORDER = order;
        }
        return VNS;
    }

    /** Get the V<sub>n,s</sub><sup>m</sup> coefficient from V<sub>n,s</sub>.
     *  <br>See § 2.8.2 in Danielson paper.
     * @param m m
     * @param n n
     * @param s s
     * @return The V<sub>n, s</sub> <sup>m</sup> coefficient
     * @throws OrekitException if m &gt; n
     */
    public static double getVmns(final int m, final int n, final int s)
        throws OrekitException {
        if (m > n) {
            throw new OrekitException(OrekitMessages.DSST_VMNS_COEFFICIENT_ERROR_MS, m, n);
        }
        final double fns = CombinatoricsUtils.factorialDouble(n + FastMath.abs(s));
        final double fnm = CombinatoricsUtils.factorialDouble(n  - m);

        double result = 0;
        // If (n - s) is odd, the Vmsn coefficient is null
        if ((n - s) % 2 == 0) {
            // Update the Vns coefficient
            if ((n + 1) > LAST_VNS_ORDER) {
                computeVns(n + 1);
            }
            if (s >= 0) {
                result = fns  * VNS.get(new NSKey(n, s)) / fnm;
            } else {
                // If s < 0 : Vmn-s = (-1)^(-s) Vmns
                final int mops = (s % 2 == 0) ? 1 : -1;
                result = mops * fns * VNS.get(new NSKey(n, -s)) / fnm;
            }
        }
        return result;
    }

    /** Key formed by two integer values. */
    public static class NSKey implements Comparable<NSKey> {

        /** n value. */
        private final int n;

        /** s value. */
        private final int s;

        /** Simple constructor.
         * @param n n
         * @param s s
         */
        public NSKey(final int n, final int s) {
            this.n = n;
            this.s = s;
        }

        /** Get n.
         * @return n
         */
        public int getN() {
            return n;
        }

        /** Get s.
         * @return s
         */
        public int getS() {
            return s;
        }

        /** {@inheritDoc} */
        public int compareTo(final NSKey key) {
            int result = 1;
            if (n == key.n) {
                if (s < key.s) {
                    result = -1;
                } else if (s == key.s) {
                    result = 0;
                }
            } else if (n < key.n) {
                result = -1;
            }
            return result;
        }

        /** {@inheritDoc} */
        public boolean equals(final Object key) {

            if (key == this) {
                // first fast check
                return true;
            }

            if ((key != null) && (key instanceof NSKey)) {
                return (n == ((NSKey) key).n) && (s == ((NSKey) key).s);
            }

            return false;

        }

        /** {@inheritDoc} */
        public int hashCode() {
            return 0x998493a6 ^ (n << 8) ^ s;
        }

    }

}
