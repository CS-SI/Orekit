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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.hipparchus.analysis.polynomials.PolynomialFunction;
import org.hipparchus.util.FastMath;

/**
 * Implementation of the Modified Newcomb Operators.
 *
 *  <p> From equations 2.7.3 - (12)(13) of the Danielson paper, those operators
 *  are defined as:
 *
 *  <p>
 *  4(ρ + σ)Y<sub>ρ,σ</sub><sup>n,s</sup> = <br>
 *     2(2s - n)Y<sub>ρ-1,σ</sub><sup>n,s+1</sup> + (s - n)Y<sub>ρ-2,σ</sub><sup>n,s+2</sup> <br>
 *   - 2(2s + n)Y<sub>ρ,σ-1</sub><sup>n,s-1</sup> - (s+n)Y<sub>ρ,σ-2</sub><sup>n,s-2</sup> <br>
 *   + 2(2ρ + 2σ + 2 + 3n)Y<sub>ρ-1,σ-1</sub><sup>n,s</sup>
 *
 *  <p> Initialization is given by : Y<sub>0,0</sub><sup>n,s</sup> = 1
 *
 *  <p> Internally, the Modified Newcomb Operators are stored as an array of
 *  {@link PolynomialFunction} :
 *
 *  <p> Y<sub>ρ,σ</sub><sup>n,s</sup> = P<sub>k₀</sub> + P<sub>k₁</sub>n + ... +
 *  P<sub>k<sub>j</sub></sub>n<sup>j</sup>
 *
 * <p> where the P<sub>k<sub>j</sub></sub> are given by
 *
 * <p> P<sub>k<sub>j</sub></sub> = ∑<sub>j=0;ρ</sub> a<sub>j</sub>s<sup>j</sup>
 *
 * @author Romain Di Costanzo
 * @author Pascal Parraud
 */
public class NewcombOperators {

    /** Storage map. */
    private static final Map<NewKey, Double> MAP = new TreeMap<NewKey, Double>();

    /** Private constructor as class is a utility.
     */
    private NewcombOperators() {
    }

    /** Get the Newcomb operator evaluated at n, s, ρ, σ.
     * <p>
     * This method is guaranteed to be thread-safe
     * </p>
     *  @param rho ρ index
     *  @param sigma σ index
     *  @param n n index
     *  @param s s index
     *  @return Y<sub>ρ,σ</sub><sup>n,s</sup>
     */
    public static double getValue(final int rho, final int sigma, final int n, final int s) {

        final NewKey key = new NewKey(n, s, rho, sigma);
        synchronized (MAP) {
            if (MAP.containsKey(key)) {
                return MAP.get(key);
            }
        }

        // Get the Newcomb polynomials for the given rho and sigma
        final List<PolynomialFunction> polynomials = PolynomialsGenerator.getPolynomials(rho, sigma);

        // Compute the value from the list of polynomials for the given n and s
        double nPower = 1.;
        double value = 0.0;
        for (final PolynomialFunction polynomial : polynomials) {
            value += polynomial.value(s) * nPower;
            nPower = n * nPower;
        }
        synchronized (MAP) {
            MAP.put(key, value);
        }

        return value;

    }

    /** Generator for Newcomb polynomials. */
    private static class PolynomialsGenerator {

        /** Polynomials storage. */
        private static final SortedMap<Couple, List<PolynomialFunction>> POLYNOMIALS =
                new TreeMap<Couple, List<PolynomialFunction>>();

        /** Private constructor as class is a utility.
         */
        private PolynomialsGenerator() {
        }

        /** Get the list of polynomials representing the Newcomb Operator for the (ρ,σ) couple.
         * <p>
         * This method is guaranteed to be thread-safe
         * </p>
         *  @param rho ρ value
         *  @param sigma σ value
         *  @return Polynomials representing the Newcomb Operator for the (ρ,σ) couple.
         */
        private static List<PolynomialFunction> getPolynomials(final int rho, final int sigma) {

            final Couple couple = new Couple(rho, sigma);

            synchronized (POLYNOMIALS) {

                if (POLYNOMIALS.isEmpty()) {
                    // Initialize lists
                    final List<PolynomialFunction> l00 = new ArrayList<PolynomialFunction>();
                    final List<PolynomialFunction> l01 = new ArrayList<PolynomialFunction>();
                    final List<PolynomialFunction> l10 = new ArrayList<PolynomialFunction>();
                    final List<PolynomialFunction> l11 = new ArrayList<PolynomialFunction>();

                    // Y(rho = 0, sigma = 0) = 1
                    l00.add(new PolynomialFunction(new double[] {
                        1.
                    }));
                    // Y(rho = 0, sigma = 1) =  -s - n/2
                    l01.add(new PolynomialFunction(new double[] {
                        0, -1.
                    }));
                    l01.add(new PolynomialFunction(new double[] {
                        -0.5
                    }));
                    // Y(rho = 1, sigma = 0) =  s - n/2
                    l10.add(new PolynomialFunction(new double[] {
                        0, 1.
                    }));
                    l10.add(new PolynomialFunction(new double[] {
                        -0.5
                    }));
                    // Y(rho = 1, sigma = 1) = 3/2 - s² + 5n/4 + n²/4
                    l11.add(new PolynomialFunction(new double[] {
                        1.5, 0., -1.
                    }));
                    l11.add(new PolynomialFunction(new double[] {
                        1.25
                    }));
                    l11.add(new PolynomialFunction(new double[] {
                        0.25
                    }));

                    // Initialize polynomials
                    POLYNOMIALS.put(new Couple(0, 0), l00);
                    POLYNOMIALS.put(new Couple(0, 1), l01);
                    POLYNOMIALS.put(new Couple(1, 0), l10);
                    POLYNOMIALS.put(new Couple(1, 1), l11);

                }

                // If order hasn't been computed yet, update the Newcomb polynomials
                if (!POLYNOMIALS.containsKey(couple)) {
                    PolynomialsGenerator.computeFor(rho, sigma);
                }

                return POLYNOMIALS.get(couple);

            }
        }

        /** Compute the Modified Newcomb Operators up to a given (ρ, σ) couple.
         *  <p>
         *  The recursive computation uses equation 2.7.3-(12) of the Danielson paper.
         *  </p>
         *  @param rho ρ value to reach
         *  @param sigma σ value to reach
         */
        private static void computeFor(final int rho, final int sigma) {

            // Initialize result :
            List<PolynomialFunction> result = new ArrayList<PolynomialFunction>();

            // Get the coefficient from the recurrence relation
            final Map<Integer, List<PolynomialFunction>> map = generateRecurrenceCoefficients(rho, sigma);

            // Compute (s - n) * Y[rho - 2, sigma][n, s + 2]
            if (rho >= 2) {
                final List<PolynomialFunction> poly = map.get(0);
                final List<PolynomialFunction> list = getPolynomials(rho - 2, sigma);
                result = multiplyPolynomialList(poly, shiftList(list, 2));
            }

            // Compute 2(2rho + 2sigma + 2 + 3n) * Y[rho - 1, sigma - 1][n, s]
            if (rho >= 1 && sigma >= 1) {
                final List<PolynomialFunction> poly = map.get(1);
                final List<PolynomialFunction> list = getPolynomials(rho - 1, sigma - 1);
                result = sumPolynomialList(result, multiplyPolynomialList(poly, list));
            }

            // Compute 2(2s - n) * Y[rho - 1, sigma][n, s + 1]
            if (rho >= 1) {
                final List<PolynomialFunction> poly = map.get(2);
                final List<PolynomialFunction> list = getPolynomials(rho - 1, sigma);
                result = sumPolynomialList(result, multiplyPolynomialList(poly, shiftList(list, 1)));
            }

            // Compute -(s + n) * Y[rho, sigma - 2][n, s - 2]
            if (sigma >= 2) {
                final List<PolynomialFunction> poly = map.get(3);
                final List<PolynomialFunction> list = getPolynomials(rho, sigma - 2);
                result = sumPolynomialList(result, multiplyPolynomialList(poly, shiftList(list, -2)));
            }

            // Compute -2(2s + n) * Y[rho, sigma - 1][n, s - 1]
            if (sigma >= 1) {
                final List<PolynomialFunction> poly = map.get(4);
                final List<PolynomialFunction> list = getPolynomials(rho, sigma - 1);
                result = sumPolynomialList(result, multiplyPolynomialList(poly, shiftList(list, -1)));
            }

            // Save polynomials for current (rho, sigma) couple
            final Couple couple = new Couple(rho, sigma);
            POLYNOMIALS.put(couple, result);
        }

        /** Multiply two lists of polynomials defined as the internal representation of the Newcomb Operator.
         *  <p>
         *  Let's call R<sub>s</sub>(n) the result returned by the method :
         *  <pre>
         *  R<sub>s</sub>(n) = (P<sub>s₀</sub> + P<sub>s₁</sub>n + ... + P<sub>s<sub>j</sub></sub>n<sup>j</sup>) *(Q<sub>s₀</sub> + Q<sub>s₁</sub>n + ... + Q<sub>s<sub>k</sub></sub>n<sup>k</sup>)
         *  </pre>
         *
         *  where the P<sub>s<sub>j</sub></sub> and Q<sub>s<sub>k</sub></sub> are polynomials in s,
         *  s being the index of the Y<sub>ρ,σ</sub><sup>n,s</sup> function
         *
         *  @param poly1 first list of polynomials
         *  @param poly2 second list of polynomials
         *  @return R<sub>s</sub>(n) as a list of {@link PolynomialFunction}
         */
        private static List<PolynomialFunction> multiplyPolynomialList(final List<PolynomialFunction> poly1,
                                                                       final List<PolynomialFunction> poly2) {
            // Initialize the result list of polynomial function
            final List<PolynomialFunction> result = new ArrayList<PolynomialFunction>();
            initializeListOfPolynomials(poly1.size() + poly2.size() - 1, result);

            int i = 0;
            // Iterate over first polynomial list
            for (PolynomialFunction f1 : poly1) {
                // Iterate over second polynomial list
                for (int j = i; j < poly2.size() + i; j++) {
                    final PolynomialFunction p2 = poly2.get(j - i);
                    // Get previous polynomial for current 'n' order
                    final PolynomialFunction previousP2 = result.get(j);
                    // Replace the current order by summing the product of both of the polynomials
                    result.set(j, previousP2.add(f1.multiply(p2)));
                }
                // shift polynomial order in 'n'
                i++;
            }
            return result;
        }

        /** Sum two lists of {@link PolynomialFunction}.
         *  @param poly1 first list
         *  @param poly2 second list
         *  @return the summed list
         */
        private static List<PolynomialFunction> sumPolynomialList(final List<PolynomialFunction> poly1,
                                                                  final List<PolynomialFunction> poly2) {
            // identify the lowest degree polynomial
            final int lowLength  = FastMath.min(poly1.size(), poly2.size());
            final int highLength = FastMath.max(poly1.size(), poly2.size());
            // Initialize the result list of polynomial function
            final List<PolynomialFunction> result = new ArrayList<PolynomialFunction>();
            initializeListOfPolynomials(highLength, result);

            for (int i = 0; i < lowLength; i++) {
                // Add polynomials by increasing order of 'n'
                result.set(i, poly1.get(i).add(poly2.get(i)));
            }
            // Complete the list if lists are of different size:
            for (int i = lowLength; i < highLength; i++) {
                if (poly1.size() < poly2.size()) {
                    result.set(i, poly2.get(i));
                } else {
                    result.set(i, poly1.get(i));
                }
            }
            return result;
        }

        /** Initialize an empty list of polynomials.
         *  @param i order
         *  @param result list into which polynomials should be added
         */
        private static void initializeListOfPolynomials(final int i,
                                                        final List<PolynomialFunction> result) {
            for (int k = 0; k < i; k++) {
                result.add(new PolynomialFunction(new double[] {0.}));
            }
        }

        /** Shift a list of {@link PolynomialFunction}.
         *
         *  @param polynomialList list of {@link PolynomialFunction}
         *  @param shift shift value
         *  @return new list of shifted {@link PolynomialFunction}
         */
        private static List<PolynomialFunction> shiftList(final List<PolynomialFunction> polynomialList,
                                                          final int shift) {
            final List<PolynomialFunction> shiftedList = new ArrayList<PolynomialFunction>();
            for (PolynomialFunction function : polynomialList) {
                shiftedList.add(new PolynomialFunction(shift(function.getCoefficients(), shift)));
            }
            return shiftedList;
        }

        /**
         * Compute the coefficients of the polynomial \(P_s(x)\)
         * whose values at point {@code x} will be the same as the those from the
         * original polynomial \(P(x)\) when computed at {@code x + shift}.
         * <p>
         * More precisely, let \(\Delta = \) {@code shift} and let
         * \(P_s(x) = P(x + \Delta)\).  The returned array
         * consists of the coefficients of \(P_s\).  So if \(a_0, ..., a_{n-1}\)
         * are the coefficients of \(P\), then the returned array
         * \(b_0, ..., b_{n-1}\) satisfies the identity
         * \(\sum_{i=0}^{n-1} b_i x^i = \sum_{i=0}^{n-1} a_i (x + \Delta)^i\) for all \(x\).
         * </p>
         * <p>
         * This method is a modified version of the method with the same name
         * in Hipparchus {@code PolynomialsUtils} class, simply changing
         * computation of binomial coefficients so degrees higher than 66 can be used.
         * </p>
         *
         * @param coefficients Coefficients of the original polynomial.
         * @param shift Shift value.
         * @return the coefficients \(b_i\) of the shifted
         * polynomial.
         */
        public static double[] shift(final double[] coefficients,
                                     final double shift) {
            final int dp1 = coefficients.length;
            final double[] newCoefficients = new double[dp1];

            // Pascal triangle.
            final double[][] coeff = new double[dp1][dp1];
            coeff[0][0] = 1;
            for (int i = 1; i < dp1; i++) {
                coeff[i][0] = 1;
                for (int j = 1; j < i; j++) {
                    coeff[i][j] = coeff[i - 1][j - 1] + coeff[i - 1][j];
                }
                coeff[i][i] = 1;
            }

            // First polynomial coefficient.
            double shiftI = 1;
            for (int i = 0; i < dp1; i++) {
                newCoefficients[0] += coefficients[i] * shiftI;
                shiftI *= shift;
            }

            // Superior order.
            final int d = dp1 - 1;
            for (int i = 0; i < d; i++) {
                double shiftJmI = 1;
                for (int j = i; j < d; j++) {
                    newCoefficients[i + 1] += coeff[j + 1][j - i] * coefficients[j + 1] * shiftJmI;
                    shiftJmI *= shift;
                }
            }

            return newCoefficients;
        }

        /** Generate recurrence coefficients.
         *
         *  @param rho ρ value
         *  @param sigma σ value
         *  @return recurrence coefficients
         */
        private static Map<Integer, List<PolynomialFunction>> generateRecurrenceCoefficients(final int rho, final int sigma) {
            final double den   = 1. / (4. * (rho + sigma));
            final double denx2 = 2. * den;
            final double denx4 = 4. * den;
            // Initialization :
            final Map<Integer, List<PolynomialFunction>> list = new TreeMap<Integer, List<PolynomialFunction>>();
            final List<PolynomialFunction> poly0 = new ArrayList<PolynomialFunction>();
            final List<PolynomialFunction> poly1 = new ArrayList<PolynomialFunction>();
            final List<PolynomialFunction> poly2 = new ArrayList<PolynomialFunction>();
            final List<PolynomialFunction> poly3 = new ArrayList<PolynomialFunction>();
            final List<PolynomialFunction> poly4 = new ArrayList<PolynomialFunction>();
            // (s - n)
            poly0.add(new PolynomialFunction(new double[] {0., den}));
            poly0.add(new PolynomialFunction(new double[] {-den}));
            // 2(2 * rho + 2 * sigma + 2 + 3*n)
            poly1.add(new PolynomialFunction(new double[] {1. + denx4}));
            poly1.add(new PolynomialFunction(new double[] {denx2 + denx4}));
            // 2(2s - n)
            poly2.add(new PolynomialFunction(new double[] {0., denx4}));
            poly2.add(new PolynomialFunction(new double[] {-denx2}));
            // - (s + n)
            poly3.add(new PolynomialFunction(new double[] {0., -den}));
            poly3.add(new PolynomialFunction(new double[] {-den}));
            // - 2(2s + n)
            poly4.add(new PolynomialFunction(new double[] {0., -denx4}));
            poly4.add(new PolynomialFunction(new double[] {-denx2}));

            // Fill the map :
            list.put(0, poly0);
            list.put(1, poly1);
            list.put(2, poly2);
            list.put(3, poly3);
            list.put(4, poly4);
            return list;
        }

    }

    /** Private class to define a couple of value. */
    private static class Couple implements Comparable<Couple> {

        /** first couple value. */
        private final int rho;

        /** second couple value. */
        private final int sigma;

        /** Constructor.
         * @param rho first couple value
         * @param sigma second couple value
         */
        private Couple(final int rho, final int sigma) {
            this.rho = rho;
            this.sigma = sigma;
        }

        /** {@inheritDoc} */
        public int compareTo(final Couple c) {
            int result = 1;
            if (rho == c.rho) {
                if (sigma < c.sigma) {
                    result = -1;
                } else if (sigma == c.sigma) {
                    result = 0;
                }
            } else if (rho < c.rho) {
                result = -1;
            }
            return result;
        }

        /** {@inheritDoc} */
        public boolean equals(final Object couple) {

            if (couple == this) {
                // first fast check
                return true;
            }

            if ((couple != null) && (couple instanceof Couple)) {
                return (rho == ((Couple) couple).rho) && (sigma == ((Couple) couple).sigma);
            }

            return false;

        }

        /** {@inheritDoc} */
        public int hashCode() {
            return 0x7ab17c0c ^ (rho << 8) ^ sigma;
        }

    }

    /** Newcomb operator's key. */
    private static class NewKey implements Comparable<NewKey> {

        /** n value. */
        private final int n;

        /** s value. */
        private final int s;

        /** ρ value. */
        private final int rho;

        /** σ value. */
        private final int sigma;

        /** Simpleconstructor.
         * @param n n
         * @param s s
         * @param rho ρ
         * @param sigma σ
         */
        NewKey(final int n, final int s, final int rho, final int sigma) {
            this.n = n;
            this.s = s;
            this.rho = rho;
            this.sigma = sigma;
        }

        /** {@inheritDoc} */
        public int compareTo(final NewKey key) {
            int result = 1;
            if (n == key.n) {
                if (s == key.s) {
                    if (rho == key.rho) {
                        if (sigma < key.sigma) {
                            result = -1;
                        } else if (sigma == key.sigma) {
                            result = 0;
                        } else {
                            result = 1;
                        }
                    } else if (rho < key.rho) {
                        result = -1;
                    } else {
                        result = 1;
                    }
                } else if (s < key.s) {
                    result = -1;
                } else {
                    result = 1;
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

            if ((key != null) && (key instanceof NewKey)) {
                return (n     == ((NewKey) key).n) &&
                       (s     == ((NewKey) key).s) &&
                       (rho   == ((NewKey) key).rho) &&
                       (sigma == ((NewKey) key).sigma);
            }

            return false;

        }

        /** {@inheritDoc} */
        public int hashCode() {
            return 0x25baa451 ^ (n << 24) ^ (s << 16) ^ (rho << 8) ^ sigma;
        }

    }

}
