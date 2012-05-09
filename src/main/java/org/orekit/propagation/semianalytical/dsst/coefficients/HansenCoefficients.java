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
package org.orekit.propagation.semianalytical.dsst.coefficients;

import java.util.TreeMap;

import org.apache.commons.math3.util.ArithmeticUtils;
import org.apache.commons.math3.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.semianalytical.dsst.coefficients.DSSTCoefficientFactory.MNSKey;

/** Hansen coefficient tool.
 * Hansen coefficient are eccentricity function representation.
 * For a given eccentricity, every computed element is stored in a map.
 *
 * @author Romain Di Costanzo
 */
public class HansenCoefficients {

    /**
     * Default maximum power of e<sup>2</sup> to use in series expansion for the Hansen coefficient.
     * Set a default e<sup>10</sup> as maximal power.
     */
    private static final int        DEFAULT_ECC2 = 10;

    /** Map to store every Hansen coefficient computed. */
    private TreeMap<MNSKey, Double> HANSEN_KERNEL;

    /** Map to store every Hansen coefficient derivatives computed. */
    private TreeMap<MNSKey, Double> HANSEN_KERNEL_DERIVATIVES;

    /** Eccentricity. */
    private final double            ecc;

    /** 1 - e<sup>2</sup>. */
    private final double            ome2;

    /** &chi; = 1 / sqrt(1- e<sup>2</sup>). */
    private final double            chi;

    /** &chi;<sup>2</sup>. */
    private final double            chi2;

    /** Maximum power of e<sup>2</sup> to use in series expansion for the Hansen coefficient when
     * using modified Newcomb operator.
     */
    private final int               maxEccPower;

    /** Simple constructor.
     * @param ecc eccentricity
     */
    public HansenCoefficients(final double ecc) {
        this(ecc, DEFAULT_ECC2);
    }

    /** Simple constructor.
     * @param ecc eccentricity
     * @param maxEccPower maximum power of e<sup>2</sup> to use in series expansion for the Hansen coefficient
     */
    public HansenCoefficients(final double ecc, final int maxEccPower) {
        HANSEN_KERNEL = new TreeMap<DSSTCoefficientFactory.MNSKey, Double>();
        HANSEN_KERNEL_DERIVATIVES = new TreeMap<DSSTCoefficientFactory.MNSKey, Double>();
        this.ecc = ecc;
        this.ome2 = 1. - ecc * ecc;
        this.chi = 1. / FastMath.sqrt(ome2);
        this.chi2 = chi * chi;
        this.maxEccPower = maxEccPower;
        initializeKernels();
    }

    /** Get the K<sub>j</sub><sup>n,s</sup> coefficient value for any (j, n, s).
     * @param j j value
     * @param n n value
     * @param s s value
     * @return K<sub>j</sub><sup>n,s</sup>
     * @throws OrekitException if some error occurred
     */
    public final double getHansenKernelValue(final int j, final int n, final int s)
        throws OrekitException {
        double result = 0d;
        if (HANSEN_KERNEL.containsKey(new MNSKey(j, n, s))) {
            result = HANSEN_KERNEL.get(new MNSKey(j, n, s));
        } else {
            if (j == 0) {
                if (n >= 0) {
                    // Compute the K0(n,s) coefficients
                    result = computeHKVJ0NPositive(n, s);
                } else {
                    // Compute the K0(-n-1,s) coefficients with n >= 0
                    result = computeHKVJ0NNegative(-(n + 1), s);
                }
            } else {
                // Compute the general Kj(-n-1, s) with n >= 0
                result = computeHKVNNegative(j, -(n + 1), s);
            }
        }
        return result;
    }

    /** Get the dK<sub>j</sub><sup>n,s</sup> / d&chi; coefficient derivative for any (j, n, s).
     *  @param j j value
     * @param n n value
     * @param s s value
     * @return dK<sub>j</sub><sup>n,s</sup> / d&chi;
     * @throws OrekitException if some error occurred
     */
    public final double getHansenKernelDerivative(final int j, final int n, final int s)
        throws OrekitException {
        if (HANSEN_KERNEL_DERIVATIVES.containsKey(new MNSKey(j, n, s))) {
            return HANSEN_KERNEL_DERIVATIVES.get(new MNSKey(j, n, s));
        } else {
            if (j == 0) {
                if (n >= 0) {
                    // Compute the dK0(n,s) / dX derivative
                    return computeHKDJ0NPositive(n, s);
                } else {
                    // Compute the dK0(-n-1,s) / dX derivative with n >= 0
                    return computeHKDJ0NNegative(-(n + 1), s);
                }
            } else {
                // Compute the general dKj(-n-1,s) / dX derivative with n >= 0
                return computeHKDNNegative(j, -(n + 1), s);
            }
        }
    }

    /** Kernels initialization. */
    private void initializeKernels() {
        HANSEN_KERNEL.put(new MNSKey(0, 0, 0), 1.);
        HANSEN_KERNEL.put(new MNSKey(0, 0, 1), -1.);
        HANSEN_KERNEL.put(new MNSKey(0, 1, 0), 1. + 0.5 * ecc * ecc);
        HANSEN_KERNEL.put(new MNSKey(0, 1, 1), -1.5);
        HANSEN_KERNEL.put(new MNSKey(0, 2, 0), 1. + 1.5 * ecc * ecc);
        HANSEN_KERNEL.put(new MNSKey(0, 2, 1), -2. - 0.5 * ecc * ecc);
        HANSEN_KERNEL.put(new MNSKey(0, -1, 0), 0.);
        HANSEN_KERNEL.put(new MNSKey(0, -1, 1), 0.);
        HANSEN_KERNEL.put(new MNSKey(0, -2, 0), chi);
        HANSEN_KERNEL.put(new MNSKey(0, -2, 1), 0.);
        HANSEN_KERNEL.put(new MNSKey(0, -3, 0), chi * chi2);
        HANSEN_KERNEL.put(new MNSKey(0, -3, 1), 0.5 * chi * chi2);
        HANSEN_KERNEL_DERIVATIVES.put(new MNSKey(0, 0, 0), 0.);
    }

    /** Compute K<sub>0</sub><sup>n,s</sup> from Equation 2.7.3-(7)(8).
     * @param n n value
     * @param s  s value
     * @return K<sub>0</sub><sup>n,s</sup>
     * @throws OrekitException if some error occurred
     */
    private double computeHKVJ0NPositive(final int n, final int s)
        throws OrekitException {
        double kns = 0.;

        if (n == (s - 1)) {
            final MNSKey key = new MNSKey(0, s - 2, s - 1);
            if (HANSEN_KERNEL.containsKey(key)) {
                kns = HANSEN_KERNEL.get(key);
            } else {
                kns = computeHKVJ0NPositive(s - 2, s - 1);
            }

            kns *= -(2. * s - 1.) / s;

        } else if (n == s) {
            final MNSKey key = new MNSKey(0, s - 1, s);
            if (HANSEN_KERNEL.containsKey(key)) {
                kns = HANSEN_KERNEL.get(key);
            } else {
                kns = computeHKVJ0NPositive(s - 1, s);
            }

            kns *= (2. * s + 1.) / (s + 1.);

        } else if (n > s) {
            final MNSKey key1 = new MNSKey(0, n - 1, s);
            double knM1 = 0.;
            if (HANSEN_KERNEL.containsKey(key1)) {
                knM1 = HANSEN_KERNEL.get(key1);
            } else {
                knM1 = computeHKVJ0NPositive(n - 1, s);
            }

            final MNSKey key2 = new MNSKey(0, n - 2, s);
            double knM2 = 0.;
            if (HANSEN_KERNEL.containsKey(key2)) {
                knM2 = HANSEN_KERNEL.get(key2);
            } else {
                knM2 = computeHKVJ0NPositive(n - 2, s);
            }

            final double val1 = (2. * n + 1.) / (n + 1.);
            final double val2 = (n + s) * (n - s) / (n * (n + 1.) * chi2);
            kns = val1 * knM1 - val2 * knM2;

        }

        HANSEN_KERNEL.put(new MNSKey(0, n, s), kns);
        return kns;
    }

    /** Compute the K<sub>0</sub><sup>-n-1,s</sup> coefficient from equation 2.7.3-(6).
     * @param n n value, must be positive. For a given 'n', the K<sub>0</sub><sup>-n-1,s</sup> will be returned
     * @param s s value
     * @return K<sub>0</sub><sup>-n-1,s</sup>
     * @throws OrekitException if some error occurred
     */
    private double computeHKVJ0NNegative(final int n, final int s)
        throws OrekitException {
        double kns = 0.;
        // Positive s value only for formula application. -s value equal to the
        // s value by
        // definition
        final int ss = (s < 0) ? -s : s;

        final MNSKey key1 = new MNSKey(0, -(n + 1), ss);
        final MNSKey key2 = new MNSKey(0, -(n + 1), -ss);

        if (HANSEN_KERNEL.containsKey(key1)) {
            kns = HANSEN_KERNEL.get(key1);
            HANSEN_KERNEL.put(key2, kns);
        } else if (HANSEN_KERNEL.containsKey(key2)) {
            kns = HANSEN_KERNEL.get(key2);
            HANSEN_KERNEL.put(key1, kns);
        } else {
            if (n == ss) {
                kns = 0.;
            } else if (n == (ss + 1)) {
                kns = FastMath.pow(chi, 1 + 2 * ss) / FastMath.pow(2, ss);
            } else {
                final MNSKey keymNS = new MNSKey(0, -n, ss);
                double kmNS = 0.;
                if (HANSEN_KERNEL.containsKey(keymNS)) {
                    kmNS = HANSEN_KERNEL.get(keymNS);
                } else {
                    kmNS = computeHKVJ0NNegative(n - 1, ss);
                }

                final MNSKey keymNp1S = new MNSKey(0, -(n - 1), ss);
                double kmNp1S = 0.;
                if (HANSEN_KERNEL.containsKey(keymNp1S)) {
                    kmNp1S = HANSEN_KERNEL.get(keymNp1S);
                } else {
                    kmNp1S = computeHKVJ0NNegative(n - 2, ss);
                }

                kns = (n - 1.) * chi2 * ((2. * n - 3.) * kmNS - (n - 2.) * kmNp1S);
                kns /= (n + ss - 1.) * (n - ss - 1.);
            }
            // Add K(n, s) and K(n, -s) as they are symmetric
            HANSEN_KERNEL.put(key1, kns);
            HANSEN_KERNEL.put(key2, kns);
        }
        return kns;
    }

    /** Compute the K<sub>j</sub><sup>-n-1,s</sup> coefficient from equation 2.7.3-(9).
     * Not to be used for j = 0 !
     *
     * @param j j value
     * @param n np value, must be positive. For a given 'np', the K<sub>j</sub><sup>-np-1,s</sup>
     *            will be returned
     * @param s s value
     * @return K<sub>j</sub><sup>-n-1,s</sup>
     * @throws OrekitException if some error occurred
     */
    private double computeHKVNNegative(final int j, final int n, final int s)
        throws OrekitException {
        double result = 0;
        if ((n == 3) || (n == s + 1) || (n == 1 - s)) {
            result = computHKVfromNewcomb(j, -n - 1, s);
        } else {
            final double kmN = computHKVfromNewcomb(j, -n, s);
            HANSEN_KERNEL.put(new MNSKey(j, -n, s), kmN);
            final double kmNp1 = computHKVfromNewcomb(j, -n + 1, s);
            HANSEN_KERNEL.put(new MNSKey(j, -n + 1, s), kmNp1);
            final double kmNp3 = computHKVfromNewcomb(j, -n + 3, s);
            HANSEN_KERNEL.put(new MNSKey(j, -n + 3, s), kmNp3);

            final double factor = chi2 / ((3. - n) * (1. - n + s) * (1. - n - s));
            final double factmN = (3. - n) * (1. - n) * (3. - 2. * n);
            final double factmNp1 = (2. - n) * ((3. - n) * (1. - n) + (2. * j * s) / chi);
            final double factmNp3 = j * j * (1. - n);
            result = factor * (factmN * kmN - factmNp1 * kmNp1 + factmNp3 * kmNp3);
            HANSEN_KERNEL.put(new MNSKey(j, -(n + 1), s), result);
        }
        return result;
    }

    /** Compute dK<sub>0</sub><sup>n,s</sup> / d&chi; from Equation 3.2-(3).
     * @param n n value
     * @param s s value
     * @return dK<sub>0</sub><sup>n,s</sup> / d&chi;
     * @throws OrekitException if some error occurred
     */
    private double computeHKDJ0NPositive(final int n, final int s) throws OrekitException {

        double dkdxns = 0.;

        final MNSKey keyNS = new MNSKey(0, n, s);
        if ((n == s - 1) || (n == s)) {
            HANSEN_KERNEL_DERIVATIVES.put(keyNS, 0.);

        } else {

            final MNSKey keyNm1 = new MNSKey(0, n - 1, s);
            double dKnM1 = 0.;
            if (HANSEN_KERNEL_DERIVATIVES.containsKey(keyNm1)) {
                dKnM1 = HANSEN_KERNEL_DERIVATIVES.get(keyNm1);
            } else {
                dKnM1 = computeHKDJ0NPositive(n - 1, s);
            }

            final MNSKey keyNm2 = new MNSKey(0, n - 2, s);
            double dKnM2 = 0.;
            if (HANSEN_KERNEL_DERIVATIVES.containsKey(keyNm2)) {
                dKnM2 = HANSEN_KERNEL_DERIVATIVES.get(keyNm2);
            } else {
                dKnM2 = computeHKDJ0NPositive(n - 2, s);
            }

            final double knM2 = getHansenKernelValue(0, n - 2, s);

            final double val1 = (2. * n + 1.) / (n + 1.);
            final double val2 = (n + s) * (n - s) / (n * (n + 1.) * chi2);
            final double val3 = 2. * (n + s) * (n - s) / (n * (n + 1.) * chi2 * chi);
            dkdxns = val1 * dKnM1 - val2 * dKnM2 + val3 * knM2;
        }

        HANSEN_KERNEL_DERIVATIVES.put(keyNS, dkdxns);
        return dkdxns;
    }

    /** Compute dK<sub>0</sub><sup>-n-1,s</sup> / d&chi; from Equation 3.1-(7).
     * @param n np value, must be positive. For a given 'np', the dK<sub>0</sub><sup>-np-1,s</sup>
     *            / d&chi; will be returned
     * @param s s value
     * @return dK<sub>0</sub><sup>-n-1,s</sup> / d&chi;
     * @throws OrekitException if some error occurred
     */
    private double computeHKDJ0NNegative(final int n, final int s) throws OrekitException {
        double dkdxns = 0.;
        final MNSKey key = new MNSKey(0, -(n + 1), s);

        if (n == FastMath.abs(s)) {
            HANSEN_KERNEL_DERIVATIVES.put(key, 0.);

        } else if (n == FastMath.abs(s) + 1) {
            dkdxns = (1. + 2. * s) * FastMath.pow(chi, 2 * s) / FastMath.pow(2, s);

        } else {

            final MNSKey keymN = new MNSKey(0, -n, s);
            double dkmN = 0.;
            if (HANSEN_KERNEL_DERIVATIVES.containsKey(keymN)) {
                dkmN = HANSEN_KERNEL_DERIVATIVES.get(keymN);
            } else {
                dkmN = computeHKDJ0NNegative(n - 1, s);
            }

            final MNSKey keymNp1 = new MNSKey(0, -n + 1, s);
            double dkmNp1 = 0.;
            if (HANSEN_KERNEL_DERIVATIVES.containsKey(keymNp1)) {
                dkmNp1 = HANSEN_KERNEL_DERIVATIVES.get(keymNp1);
            } else {
                dkmNp1 = computeHKDJ0NNegative(n - 2, s);
            }

            final double kns = getHansenKernelValue(0, -(n + 1), s);

            dkdxns = (n - 1) * chi2 * ((2. * n - 3.) * dkmN - (n - 2.) * dkmNp1) / ((n + s - 1.) * (n - s + 1.));
            dkdxns += 2. * kns / chi;
        }

        HANSEN_KERNEL_DERIVATIVES.put(key, dkdxns);
        return dkdxns;
    }

    /** Compute dK<sub>j</sub><sup>n,s</sup> / de<sup>2</sup>; from equation 3.3-(5).
     * This coefficient is always calculated for a negative n = -np-1 with np > 0
     *
     * @param j j value
     * @param n np value, must be positive. For a given 'np', the K<sub>j</sub><sup>-np-1,s</sup> will be returned
     * @param s s value
     * @return dK<sub>j</sub><sup>n,s</sup> / de<sup>2</sup>
     * @throws OrekitException if some error occurred
     */
    private double computeHKDNNegative(final int j, final int n, final int s)
        throws OrekitException {
        // Initialization
        final int nn = -(n + 1);
        final double Kjns = computHKVfromNewcomb(j, nn, s);
        final double KjnsTerm = -((nn + 1.5) / ome2) * Kjns;
        final int a = FastMath.max(j - s, 0);
        final int b = FastMath.max(s - j, 0);

        int i = 1;
        double res = 0.;
        final double e2 = ecc * ecc;
        // e^i
        double eExpI = 1;
        // Expansion until the maximum power in eccentricity is reached
        int k;
        for (k = 0; k < maxEccPower; k += 2) {
            final double newcomb = ModifiedNewcombOperators.getValue(i + a, i + b, nn, s);
            res += i * newcomb * eExpI;
            eExpI *= e2;
            i++;
        }
        return KjnsTerm + FastMath.pow(ome2, nn + 1.5) * res;
    }

    /** Compute the Hansen coefficient K<sub>j</sub><sup>ns</sup> from equation 2.7.3-(10).
     * The coefficient value is evaluated from the {@link ModifiedNewcombOperators} elements.
     * This coefficient is always calculated for a negative n = -np-1 with np > 0.
     *
     * @param j j value
     * @param n n value
     * @param s s value
     * @return K<sub>j</sub><sup>ns</sup>
     * @throws OrekitException if the Newcomb operator cannot be computed with the current indexes
     */
    public final double computHKVfromNewcomb(final int j, final int n, final int s)
        throws OrekitException {

        final int a = FastMath.max(j - s, 0);
        final int b = FastMath.max(s - j, 0);
        int i = 0;
        double res = 0.;
        final double e2 = ecc * ecc;
        // e^(2i)
        double eExp2I = 1;
        // Expansion until the maximum power in eccentricity is reached
        for (int k = 0; k < maxEccPower + 1; k += 2) {
            final double newcomb = ModifiedNewcombOperators.getValue(i + a, i + b, n, s);
            res += newcomb * eExp2I;
            eExp2I *= e2;
            i++;
        }
        return FastMath.pow(ome2, n + 1.5) * res;
    }

    /** Compute the upper bound for hansen coefficient of given indexes.
     *
     * <pre>
     *  |K<sub>j</sub><sup>-n-1, s</sup>|<sub>Bound</sub> = Max(|K<sub>j</sub><sup>-n-1, s</sup>(0)|, |K<sub>j</sub><sup>-n-1, s</sup>(1)|)
     * </pre>
     *
     * @param e eccentricity
     * @param j j
     * @param n n
     * @param s s
     * @return upper bound
     * @throws OrekitException If an error occurs in {@link ModifiedNewcombOperators} computation
     */
    public static double computeUpperBound(final double e, final int j, final int n, final int s)
        throws OrekitException {

        final double commonFactor = FastMath.pow(1 - e * e, n + 1.5);

        // Compute maximum value for e = 0
        final double kn0 = computeUpperBoundZeroE(j, n, s);

        // Compute maximal value for e = 1
        double kn1 = 0d;
        for (int k = 0; k <= (-n - 2); k++) {
            if (k >= FastMath.abs(s)) {
                final double binomialNK = ArithmeticUtils.binomialCoefficientDouble(-n - 2, k);
                final double binomialKS = ArithmeticUtils.binomialCoefficientDouble(k, (k - s) / 2);
                kn1 += binomialNK * (1 + FastMath.pow(-1, k + s)) * binomialKS;
            }
        }
        return FastMath.abs(commonFactor * FastMath.max(kn0, kn1));
    }

    /** Compute the hansen kernel enveloppe, i.e upper value for null eccentricity.
     * See equation in 6.3
     *
     * @param j j
     * @param n n
     * @param s s
     * @return The Hansen upper value for 0 eccentricity
     */
    private static double computeUpperBoundZeroE(final int j, final int n, final int s) {
        double result = 0d;
        double commonFact = 0d;
        if (s >= j) {
            commonFact = FastMath.pow(-0.5, s - j);
            for (int k = 0; k <= (s - j); k++) {
                final double product = pochhammerProduct(n + j + k + 2, s - j - k);
                final double jk = FastMath.pow(j, k);
                final double den = DSSTFactorial.fact(k).multiply(DSSTFactorial.fact(s - j - k)).doubleValue();
                result += product * jk / den;
            }
        } else {
            commonFact = FastMath.pow(-0.5, j - s);
            for (int k = 0; k <= (j - s); k++) {
                final double product = pochhammerProduct(n - j + k + 2, j - s - k);
                final double jk = FastMath.pow(-j, k);
                final double den = DSSTFactorial.fact(k).multiply(DSSTFactorial.fact(j - s - k)).doubleValue();
                result += product * jk / den;
            }
        }
        return commonFact * result;
    }

    /** Compute the pochhammer product.
     *
     * <pre>
     *  (&alpha;)<sub>k</sub> = (&alpha;)(&alpha; + 1)(&alpha; + 2)...(&alpha; + k - 1)
     * </pre>
     *
     * @param alpha &alpha;
     * @param k k
     * @return pochhammer product
     */
    private static double pochhammerProduct(final int alpha, final int k) {

        if (k == 0) {
            return 1;
        } else if ((alpha + k - 1) == 0) {
            return 0;
        }
        // Pochhammer product :
        double product = alpha;
        for (int sum = 0; sum <= (alpha + k - 1); sum++) {
            product *= alpha + sum;
        }
        return product;
    }

}
