/* Copyright 2002-2012 CS Systèmes d'Information
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

import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.analysis.polynomials.PolynomialsUtils;
import org.apache.commons.math3.util.ArithmeticUtils;
import org.apache.commons.math3.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

/**
 * This class is designed to provide coefficient from the DSST theory.
 *
 * @author Romain Di Costanzo
 */
public class DSSTCoefficientFactory {

    /** Internal storage of the polynomial values. Reused for further computation. */
    private static TreeMap<NSKey, Double> VNS = new TreeMap<NSKey, Double>();

    /** Last computed order for V<sub>ns</sub> coefficients. */
    private static int         LAST_VNS_ORDER = 2;

    /** Map of the Qns derivatives, for each (n, s) couple. */
    private static Map<NSKey, PolynomialFunction> QNS_MAP        = new TreeMap<NSKey, PolynomialFunction>();

    /** Static initialization for the V<sub>ns</sub> coefficient. */
    static {
        // Initialization
        VNS.put(new NSKey(0, 0), 1.);
        VNS.put(new NSKey(1, 0), 0.);
        VNS.put(new NSKey(1, 1), 0.5);
    }

    /** Private constructor as the class is a utility class.
     */
    private DSSTCoefficientFactory() {
    }

    /** Get the Q<sub>ns</sub> value from 2.8.1-(4) evaluated in &gamma; This method is using the
     * Legendre polynomial to compute the Q<sub>ns</sub>'s one. This direct computation method
     * allows to store the polynomials value in a static map. If the Q<sub>ns</sub> had been
     * computed already, they just will be evaluated at &gamma;
     *
     * @param gamma &gamma; angle for which Q<sub>ns</sub> is evaluated
     * @param n n value
     * @param s s value
     * @return the polynomial value evaluated at &gamma;
     */
    public static double getQnsPolynomialValue(final double gamma, final int n, final int s) {
        PolynomialFunction derivative;
        if (QNS_MAP.containsKey(new NSKey(n, s))) {
            derivative = QNS_MAP.get(new NSKey(n, s));
        } else {
            final PolynomialFunction legendre = PolynomialsUtils.createLegendrePolynomial(n);
            derivative = legendre;
            for (int i = 0; i < s; i++) {
                derivative = (PolynomialFunction) derivative.derivative();
            }
            QNS_MAP.put(new NSKey(n, s), derivative);
        }
        return derivative.value(gamma);
    }

    /** Compute the Q<sub>ns</sub> array evaluated at &gamma; from the recurrence formula 2.8.3-(2).
     * As this method directly evaluate the polynomial at &gamma;, values aren't stored.
     * @param gamma &gamma; angle
     * @param order order N of computation
     * @return Q<sub>ns</sub> array
     */
    public static double[][] computeQnsCoefficient(final double gamma, final int order) {

        // Initialization
        final double[][] Qns = new double[order + 1][];
        for (int i = 0; i < order + 1; i++) {
            Qns[i] = new double[i + 1];
        }

        // first element
        Qns[0][0] = 1;

        for (int n = 1; n <= order; n++) {
            for (int s = 0; s <= n; s++) {
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
     * @param k x-component of the eccentricity vector
     * @param h y-component of the eccentricity vector
     * @param alpha 1st direction cosine
     * @param beta 2nd direction cosine
     * @param order development order
     * @return Array of G<sub>s</sub> and H<sub>s</sub> polynomials for s from 0 to order.<br>
     *         The 1st column contains the G<sub>s</sub> values.
     *         The 2nd column contains the H<sub>s</sub> values.
     */
    public static double[][] computeGsHs(final double k, final double h,
                                         final double alpha, final double beta,
                                         final int order) {
        // Constant terms
        final double hamkb = h * alpha - k * beta;
        final double kaphb = k * alpha + h * beta;
        // Initialization
        final double[][] GsHs = new double[2][order + 1];
        // 1st & 2nd Gs coefficients
        GsHs[0][0] = 1.;
        GsHs[0][1] = kaphb;
        // 1st & 2nd Hs coefficients
        GsHs[1][0] = 0.;
        GsHs[1][1] = hamkb;

        for (int s = 2; s <= order; s++) {
            // Gs coefficient
            GsHs[0][s] = kaphb * GsHs[0][s - 1] - hamkb * GsHs[1][s - 1];
            // Hs coefficient
            GsHs[1][s] = hamkb * GsHs[0][s - 1] + kaphb * GsHs[1][s - 1];
        }

        return GsHs;
    }

    /** Compute the V<sub>n, s</sub> coefficient from 2.8.2 - (1)(2).
     * @param order Order of the computation. Computation will be done from order 0 to order -1
     * @return Map of the V<sub>n, s</sub> coefficient
     */
    public static TreeMap<NSKey, Double> computeVnsCoefficient(final int order) {

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

    /** Initialize the V<sub>n, s</sub> <sup>m</sup> coefficient from the V<sub>n, s</sub>
     * <sup>m</sup> expression as function of the V<sub>n, s</sub> coefficients. See text in 2.8.2
     * @param m m
     * @param n n
     * @param s s
     * @return The V<sub>n, s</sub> <sup>m</sup> coefficient
     * @throws OrekitException if m > s
     */
    public static double getVmns(final int m, final int n, final int s)
        throws OrekitException {
        if (m > n) {
            throw new OrekitException(OrekitMessages.DSST_VMSN_COEFFICIENT_ERROR_MS, m, s);
        }

        if ((n + 1) > LAST_VNS_ORDER) {
            // Update the Vns coefficient
            computeVnsCoefficient(n + 1);
        }

        // If (n -s) is odd, the Vmsn coefficient is null
        double result = 0;
        if ((n - s) % 2 == 0) {
            if (s >= 0) {
                result = ArithmeticUtils.factorial(n + s)  * VNS.get(new NSKey(n, s)) / ArithmeticUtils.factorial(n - m);
            } else {
                // If s < 0 : Vmn-s = (-1)^(-s) Vmns
                result = FastMath.pow(-1, -s) * ArithmeticUtils.factorial(n - s) * VNS.get(new NSKey(n, -s)) /
                        ArithmeticUtils.factorial(n - m);
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

    /** MNS couple's key. */
    public static class MNSKey implements Comparable<MNSKey> {

        /** m value. */
        private final int m;

        /** n value. */
        private final int n;

        /** s value. */
        private final int s;

        /** Simpleconstructor.
         * @param m m
         * @param n n
         * @param s s
         */
        public MNSKey(final int m, final int n, final int s) {
            this.m = m;
            this.n = n;
            this.s = s;
        }

        /** {@inheritDoc} */
        public int compareTo(final MNSKey key) {
            int result = 1;
            if (m == key.m) {
                if (n == key.n) {
                    if (s < key.s) {
                        result = -1;
                    } else if (s == key.s) {
                        result = 0;
                    } else {
                        result = 1;
                    }
                } else if (n < key.n) {
                    result = -1;
                } else {
                    result = 1;
                }
            } else if (m < key.m) {
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

            if ((key != null) && (key instanceof MNSKey)) {
                return (m == ((MNSKey) key).m) &&
                       (n == ((MNSKey) key).n) &&
                       (s == ((MNSKey) key).s);
            }

            return false;

        }

        /** {@inheritDoc} */
        public int hashCode() {
            return 0x25baa451 ^ (m << 16) ^ (n << 8) ^ s;
        }

    }

}
