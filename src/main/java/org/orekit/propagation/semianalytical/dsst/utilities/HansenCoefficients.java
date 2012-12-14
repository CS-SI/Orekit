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

import java.util.TreeMap;

import org.apache.commons.math3.util.ArithmeticUtils;
import org.apache.commons.math3.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.semianalytical.dsst.utilities.DSSTCoefficientFactory.MNSKey;

/** Hansen coefficients for tesseral contribution to central body force model.
 *  <p>
 *  Hansen coefficient are eccentricity function representation.
 *  For a given eccentricity, every computed element is stored in a map.
 *  </p>
 *
 *  @author Romain Di Costanzo
 */
public class HansenCoefficients {

    /** Map to store every Hansen coefficient computed. */
    private TreeMap<MNSKey, Double> coefficients;

    /** Map to store every Hansen coefficient derivatives computed. */
    private TreeMap<MNSKey, Double> derivatives;

    /** Eccentricity. */
    private final double ecc;

    /** 1 - e<sup>2</sup>. */
    private final double ome2;

    /** &chi; = 1 / sqrt(1- e<sup>2</sup>). */
    private final double chi;

    /** &chi;<sup>2</sup>. */
    private final double chi2;

    /** Maximum power of e<sup>2</sup> to use in series expansion for the Hansen coefficient when
     * using modified Newcomb operator.
     */
    private final int    maxEccPower;

    /** Simple constructor.
     *  @param ecc eccentricity
     *  @param maxEccPower maximum power of e<sup>2</sup> to use in series expansion for the Hansen coefficient
     */
    public HansenCoefficients(final double ecc, final int maxEccPower) {
        coefficients = new TreeMap<DSSTCoefficientFactory.MNSKey, Double>();
        derivatives = new TreeMap<DSSTCoefficientFactory.MNSKey, Double>();
        this.ecc = ecc;
        this.ome2 = 1. - ecc * ecc;
        this.chi = 1. / FastMath.sqrt(ome2);
        this.chi2 = chi * chi;
        this.maxEccPower = maxEccPower;
        initialize();
    }

    /** Get the K<sub>j</sub><sup>n,s</sup> coefficient value for any (j, n, s).
     * @param j j value
     * @param n n value
     * @param s s value
     * @return K<sub>j</sub><sup>n,s</sup>
     * @throws OrekitException if some error occurred
     */
    public final double getValue(final int j, final int n, final int s)
        throws OrekitException {
        if (coefficients.containsKey(new MNSKey(j, n, s))) {
            return coefficients.get(new MNSKey(j, n, s));
        } else {
            // Compute the general Kj(-n-1, s) with n >= 0
            return computeValue(j, -n - 1, s);
        }
    }

    /** Get the dK<sub>j</sub><sup>n,s</sup> / d&chi; coefficient derivative for any (j, n, s).
     *  @param j j value
     *  @param n n value
     *  @param s s value
     *  @return dK<sub>j</sub><sup>n,s</sup> / d&chi;
     *  @throws OrekitException if some error occurred
     */
    public final double getDerivative(final int j, final int n, final int s)
        throws OrekitException {
        if (derivatives.containsKey(new MNSKey(j, n, s))) {
            return derivatives.get(new MNSKey(j, n, s));
        } else {
            // Compute the general dKj(-n-1,s) / dX derivative with n >= 0
            return computeDerivative(j, -n - 1, s);
        }
    }

    /** Kernels initialization. */
    private void initialize() {
        coefficients.put(new MNSKey(0, 0, 0), 1.);
        coefficients.put(new MNSKey(0, 0, 1), -1.);
        coefficients.put(new MNSKey(0, 1, 0), 1. + 0.5 * ecc * ecc);
        coefficients.put(new MNSKey(0, 1, 1), -1.5);
        coefficients.put(new MNSKey(0, 2, 0), 1. + 1.5 * ecc * ecc);
        coefficients.put(new MNSKey(0, 2, 1), -2. - 0.5 * ecc * ecc);
        coefficients.put(new MNSKey(0, -1, 0), 0.);
        coefficients.put(new MNSKey(0, -1, 1), 0.);
        coefficients.put(new MNSKey(0, -2, 0), chi);
        coefficients.put(new MNSKey(0, -2, 1), 0.);
        coefficients.put(new MNSKey(0, -3, 0), chi * chi2);
        coefficients.put(new MNSKey(0, -3, 1), 0.5 * chi * chi2);
        derivatives.put(new MNSKey(0, 0, 0), 0.);
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
    private double computeValue(final int j, final int n, final int s)
        throws OrekitException {
        double result = 0;
        if ((n == 3) || (n == s + 1) || (n == 1 - s)) {
            result = computValueFromNewcomb(j, -n - 1, s);
        } else {
            final double kmN = computValueFromNewcomb(j, -n, s);
            coefficients.put(new MNSKey(j, -n, s), kmN);
            final double kmNp1 = computValueFromNewcomb(j, -n + 1, s);
            coefficients.put(new MNSKey(j, -n + 1, s), kmNp1);
            final double kmNp3 = computValueFromNewcomb(j, -n + 3, s);
            coefficients.put(new MNSKey(j, -n + 3, s), kmNp3);

            final double factor = chi2 / ((3. - n) * (1. - n + s) * (1. - n - s));
            final double factmN = (3. - n) * (1. - n) * (3. - 2. * n);
            final double factmNp1 = (2. - n) * ((3. - n) * (1. - n) + (2. * j * s) / chi);
            final double factmNp3 = j * j * (1. - n);
            result = factor * (factmN * kmN - factmNp1 * kmNp1 + factmNp3 * kmNp3);
            coefficients.put(new MNSKey(j, -(n + 1), s), result);
        }
        return result;
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
    private double computeDerivative(final int j, final int n, final int s)
        throws OrekitException {
        // Initialization
        final int nn = -(n + 1);
        final double Kjns = computValueFromNewcomb(j, nn, s);
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
    public final double computValueFromNewcomb(final int j, final int n, final int s)
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
                final double product = pochhammer(n + j + k + 2, s - j - k);
                final double jk = FastMath.pow(j, k);
                final double den = DSSTFactorial.fact(k) * DSSTFactorial.fact(s - j - k);
                result += product * jk / den;
            }
        } else {
            commonFact = FastMath.pow(-0.5, j - s);
            for (int k = 0; k <= (j - s); k++) {
                final double product = pochhammer(n - j + k + 2, j - s - k);
                final double jk = FastMath.pow(-j, k);
                final double den = DSSTFactorial.fact(k) * DSSTFactorial.fact(j - s - k);
                result += product * jk / den;
            }
        }
        return commonFact * result;
    }

    /** Compute the Pochhammer product.
     *  <pre>
     *  (&alpha;)<sub>k</sub> = (&alpha;)(&alpha; + 1)(&alpha; + 2)...(&alpha; + k - 1)
     *  </pre>
     *
     *  @param alpha &alpha;
     *  @param k k
     *  @return pochhammer product
     */
    private static double pochhammer(final int alpha, final int k) {

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
