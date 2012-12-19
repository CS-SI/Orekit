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
package org.orekit.propagation.semianalytical.dsst.forces;

import java.util.TreeMap;

import org.apache.commons.math3.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.propagation.semianalytical.dsst.utilities.CoefficientsFactory;
import org.orekit.propagation.semianalytical.dsst.utilities.CoefficientsFactory.NSKey;
import org.orekit.time.AbsoluteDate;

/** Zonal contribution to the {@link DSSTCentralBody central body gravitational perturbation}.
 *
 *   @author Romain Di Costanzo
 *   @author Pascal Parraud
 */
class ZonalContribution implements DSSTForceModel {

    /** Truncation tolerance. */
    private static final double TRUNCATION_TOLERANCE = 1e-10;

    /** Equatorial radius of the central body. */
    private final double r;

    /** Central body attraction coefficient. */
    private final double mu;

    /** Degree <i>n</i> of potential. */
    private final int    degree;

    /** Geopotential coefficient Jn = -Cn0. */
    private final double[] Jn;

    /** Factorial. */
    private final double[] fact;

    /** Coefficient used to define the mean disturbing function V<sub>ns</sub> coefficient. */
    private final TreeMap<NSKey, Double> Vns;

    // Equinoctial elements (according to DSST notation)
    /** a. */
    private double a;
    /** ex. */
    private double k;
    /** ey. */
    private double h;
    /** hx. */
    private double q;
    /** hy. */
    private double p;
    /** Retrograde factor. */
    private int    I;

    /** Eccentricity. */
    private double ecc;

    /** Direction cosine &alpha. */
    private double alpha;
    /** Direction cosine &beta. */
    private double beta;
    /** Direction cosine &gamma. */
    private double gamma;

    // Common factors for potential computation
    /** &Chi; = 1 / sqrt(1 - e<sup>2</sup>) = 1 / B. */
    private double X;
    /** &Chi;<sup>2</sup>. */
    private double XX;
    /** &Chi;<sup>3</sup>. */
    private double XXX;
    /** 1 / (A * B) .*/
    private double ooAB;
    /** B / A .*/
    private double BoA;
    /** B / A(1 + B) .*/
    private double BoABpo;
    /** -C / (2 * A * B) .*/
    private double mCo2AB;
    /** -2 * a / A .*/
    private double m2aoA;
    /** &mu; / a .*/
    private double muoa;

    /** Highest power of the eccentricity to be used in series expansion. */
    private int    maxEccPow;

    /** Maximal degree of the potential to be used in series expansion. */
    private int    maxDegree;

    /** Hansen coefficient. */
    private HansenZonal hansen;

    /** Simple constructor.
     *  @param equatorialRadius equatorial radius of the central body (m)
     *  @param mu central body attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     *  @param jn zonal coefficients of the potential
     */
    public ZonalContribution(final double equatorialRadius,
                             final double mu,
                             final double[] jn) {

        this.r      = equatorialRadius;
        this.mu     = mu;
        this.Jn     = jn.clone();
        this.degree = jn.length - 1;

        this.Vns = CoefficientsFactory.computeVnsCoefficient(degree + 1);

        // Factorials computation
        this.fact = new double[degree + 1];
        fact[0] = 1.;
        for (int i = 1; i <= degree; i++) {
            fact[i] = i * fact[i - 1];
        }

        // Initialize default values
//        this.maxEccPow = (degree == 2) ? 0 : Integer.MIN_VALUE;
        this.maxDegree = degree;
        this.maxEccPow = degree - 2;
    }

    /** {@inheritDoc} */
    public final void initialize(final AuxiliaryElements aux) throws OrekitException {

        // Equinoctial elements
        a = aux.getSma();
        k = aux.getK();
        h = aux.getH();
        q = aux.getQ();
        p = aux.getP();

        // Retrograde factor
        I = aux.getRetrogradeFactor();

        // Eccentricity
        ecc = aux.getEcc();

        // Direction cosines
        alpha = aux.getAlpha();
        beta  = aux.getBeta();
        gamma = aux.getGamma();

        // Equinoctial coefficients
        final double A = aux.getA();
        final double B = aux.getB();
        final double C = aux.getC();

        // &Chi; = 1 / B
        X = 1. / B;
        XX = X * X;
        XXX = X * XX;
        // 1 / AB
        ooAB = 1. / (A * B);
        // B / A
        BoA = B / A;
        // -C / 2AB
        mCo2AB = -C * ooAB / 2.;
        // B / A(1 + B)
        BoABpo = BoA / (1. + B);
        // -2 * a / A
        m2aoA = -2 * a / A;
        // &mu; / a
        muoa = mu / a;

        // Hansen coefficients
        hansen = new HansenZonal();

        // Utilities for truncation
        final double eo2 = ecc / 2.;
        final double x2o2 = XX / 2.;
        final double[] eccPwr = new double[degree + 1];
        final double[] hafPwr = new double[degree + 1];
        final double[] chiPwr = new double[degree + 1];
        eccPwr[0] = 1.;
        hafPwr[0] = 1.;
        chiPwr[0] = X;
        for (int i = 1; i <= degree; i++) {
            eccPwr[i] = eccPwr[i - 1] * eo2;
            hafPwr[i] = hafPwr[i - 1] * 0.5;
            chiPwr[i] = chiPwr[i - 1] * x2o2;
        }

        // Set the highest power of the eccentricity in the analytical power
        // series expansion for the averaged low order zonal harmonic perturbation
        truncation();

    }

    /** {@inheritDoc} */
    public double[] getMeanElementRate(final SpacecraftState spacecraftState) throws OrekitException {

        // Compute potential derivative
        final double[] dU  = computeUDerivatives();
        final double dUda  = dU[0];
        final double dUdk  = dU[1];
        final double dUdh  = dU[2];
        final double dUdAl = dU[3];
        final double dUdBe = dU[4];
        final double dUdGa = dU[5];

        // Compute cross derivatives [Eq. 2.2-(8)]
        // U(alpha,gamma) = alpha * dU/dgamma - gamma * dU/dalpha
        final double UAlphaGamma   = alpha * dUdGa - gamma * dUdAl;
        // U(beta,gamma) = beta * dU/dgamma - gamma * dU/dbeta
        final double UBetaGamma    =  beta * dUdGa - gamma * dUdBe;
        // Common factor
        final double pUAGmIqUBGoAB = (p * UAlphaGamma - I * q * UBetaGamma) * ooAB;

        // Compute mean elements rates [Eq. 3.1-(1)]
        final double da =  0.;
        final double dh =  BoA * dUdk + k * pUAGmIqUBGoAB;
        final double dk = -BoA * dUdh - h * pUAGmIqUBGoAB;
        final double dp =  mCo2AB * UBetaGamma;
        final double dq =  mCo2AB * UAlphaGamma * I;
        final double dM =  m2aoA * dUda + BoABpo * (h * dUdh + k * dUdk) + pUAGmIqUBGoAB;

        return new double[] {da, dk, dh, dq, dp, dM};
    }

    /** {@inheritDoc} */
    public double[] getShortPeriodicVariations(final AbsoluteDate date,
                                               final double[] meanElements)
        throws OrekitException {
        // TODO: not implemented yet, Short Periodic Variations are set to null
        return new double[] {0., 0., 0., 0., 0., 0.};
    }

    /** Computes the highest power of the eccentricity and the maximal degree
     *  to appear in the truncated analytical power series expansion for the
     *  averaged central-body zonal harmonic potential.
     *  <p>
     *  This method is computing the upper value for the central body geopotential
     *  and then determine the maximal values from with upper values give geopotential
     *  terms inferior to a defined tolerance.
     *  </p>
     *  Algorithm description can be found in the D.A Danielson paper at paragraph 6.2.
     *
     * @throws OrekitException if an error occurs in Hansen coefficient computation
     */
    private void truncation() throws OrekitException {
        // Check if highest power of E has been given already set
        if (maxEccPow == Integer.MIN_VALUE) {
            // Did a maximum eccentricity power has been found
            boolean maxFound = false;
            // Maximal degree of the geopotential expansion
            int nMax = Integer.MIN_VALUE;
            // Maximal power of e
            int sMax = Integer.MIN_VALUE;

            // Auxiliary quantities
            final double omgg = 1. - gamma * gamma;
            final double ro2a = r / (2. * a);
            double x2MuRaN = 2. * muoa * ro2a;
            // Search for the highest power of E for which the computed value is greater than
            // the truncation tolerance in the power series
            // s-loop :
            for (int s = 0; s <= degree - 2; s++) {
                final int so2 = s / 2;
                final double omggpso2xeccps = FastMath.pow(omgg, so2) * FastMath.pow(ecc, s);
                // n-loop
                for (int n = s + 2; n <= degree; n++) {
                    // (n - s) must be even
                    if ((n - s) % 2 == 0) {
                        // Local values :
                        final double factor = fact[n - s] / (fact[(n + s) / 2] * fact[(n - s) / 2]);
                        final double k0 = hansen.getValue(-n - 1, s);
                        // Compute Qns
                        final double qns  = FastMath.abs(CoefficientsFactory.getQnsPolynomialValue(gamma, n, s));
                        // Compute dQns/dGamma
                        final double dQns = FastMath.abs(CoefficientsFactory.getQnsPolynomialValue(gamma, n, s + 1));
                        // Compute the Qns upper bound
                        final double coef = omgg / (n * (n + 1) - s * (s + 1));
                        final double qnsB = FastMath.sqrt(qns * qns + coef * dQns * dQns);

                        // Get the current potential upper bound for the current (n, s) couple.
                        final double term = x2MuRaN * ro2a * FastMath.abs(Jn[n]) * factor * k0 * qnsB *
                                            omggpso2xeccps / FastMath.pow(2, n);

                        // Compare result with the tolerance parameter :
                        if (term <= TRUNCATION_TOLERANCE) {
                            // Stop here
                            nMax = Math.max(nMax, n);
                            sMax = Math.max(sMax, s);
                            // truncature found
                            maxFound = true;
                            // Force a premature end loop
                            n = degree;
                            s = degree;
                        }
                    }
                }
                // Prepare next loop :
                x2MuRaN = 2 * muoa * FastMath.pow(ro2a, s + 1);
            }
            if (maxFound) {
                maxDegree = nMax;
                maxEccPow = sMax;
            } else {
                maxDegree = degree;
                maxEccPow = degree - 2;
            }
        }
    }

    /** Compute the derivatives of the gravitational potential U [Eq. 3.1-(6)].
     *  <p>
     *  The result is the array
     *  [dU/da, dU/dk, dU/dh, dU/d&alpha;, dU/d&beta;, dU/d&gamma;]
     *  </p>
     *  @return potential derivatives
     *  @throws OrekitException if an error occurs in hansen computation
     */
    private double[] computeUDerivatives()
        throws OrekitException {

        // Initialize data
        final double[][] GsHs = CoefficientsFactory.computeGsHs(k, h, alpha, beta, maxDegree + 1);
        final double[][] Qns  = CoefficientsFactory.computeQnsCoefficient(gamma, maxDegree + 1);

        final double roa = r / a;

        // Potential derivatives
        double dUda  = 0d;
        double dUdk  = 0d;
        double dUdh  = 0d;
        double dUdAl = 0d;
        double dUdBe = 0d;
        double dUdGa = 0d;

        for (int s = 0; s <= maxEccPow; s++) {
            // Get the current gs and hs coefficient :
            final double gs = GsHs[0][s];

            // Compute partial derivatives of Gs from 3.1-(9)
            // First get the G(s-1) and the H(s-1) coefficient : SET TO 0 IF < 0
            final double sxgsm1 = s > 0 ? s * GsHs[0][s - 1] : 0;
            final double sxhsm1 = s > 0 ? s * GsHs[1][s - 1] : 0;
            // Get derivatives
            final double dGsdh  = beta  * sxgsm1 - alpha * sxhsm1;
            final double dGsdk  = alpha * sxgsm1 + beta  * sxhsm1;
            final double dGsdAl = k * sxgsm1 - h * sxhsm1;
            final double dGsdBe = h * sxgsm1 + k * sxhsm1;

            // Kronecker symbol (2 - delta(0,s))
            final double delta0s = (s == 0) ? 1 : 2;

            for (int n = s + 2; n <= maxDegree; n++) {
                // (n - s) must be even
                if ((n - s) % 2 == 0) {
                    // Extract data from previous computation :
                    final double jn   = Jn[n];
                    final double vns  = Vns.get(new NSKey(n, s));
                    final double kns  = hansen.getValue(-n - 1, s);
                    final double qns  = Qns[n][s];
                    final double rapn = FastMath.pow(roa, n);
                    final double dkns = hansen.getDerivative(-n - 1, s);
                    final double coef = delta0s * rapn * jn * vns;

                    // Compute dU / da :
                    dUda += coef * kns * qns * (n + 1) * gs;
                    // Compute dU / dEx
                    dUdk += coef * qns * (kns * dGsdk + k * XXX * gs * dkns);
                    // Compute dU / dEy
                    dUdh += coef * qns * (kns * dGsdh + h * XXX * gs * dkns);
                    // Compute dU / dAlpha
                    dUdAl += coef * kns * qns * dGsdAl;
                    // Compute dU / dBeta
                    dUdBe += coef * kns * qns * dGsdBe;
                    // Compute dU / dGamma : here dQns/dGamma = Q(n, s + 1) from Equation 3.1 - (8)
                    dUdGa += coef * kns * Qns[n][s + 1] * gs;
                }
            }
        }

        dUda  *=  muoa / a;
        dUdk  *= -muoa;
        dUdh  *= -muoa;
        dUdAl *= -muoa;
        dUdBe *= -muoa;
        dUdGa *= -muoa;

        return new double[] {dUda, dUdk, dUdh, dUdAl, dUdBe, dUdGa};
    }

    /** Hansen coefficients for zonal contribution to central body force model.
     *  <p>
     *  Hansen coefficients are functions of the eccentricity.
     *  For a given eccentricity, the computed elements are stored in a map.
     *  </p>
     */
    private class HansenZonal {

        /** Map to store every Hansen coefficient computed. */
        private TreeMap<NSKey, Double> coefficients;

        /** Map to store every Hansen coefficient derivative computed. */
        private TreeMap<NSKey, Double> derivatives;

        /** Simple constructor. */
        public HansenZonal() {
            coefficients = new TreeMap<CoefficientsFactory.NSKey, Double>();
            derivatives  = new TreeMap<CoefficientsFactory.NSKey, Double>();
            initialize();
        }

        /** Get the K<sub>0</sub><sup>-n-1,s</sup> coefficient value.
         *  @param mnm1 -n-1 value
         *  @param s s value
         *  @return K<sub>0</sub><sup>-n-1,s</sup>
         */
        public final double getValue(final int mnm1, final int s) {
            if (coefficients.containsKey(new NSKey(mnm1, s))) {
                return coefficients.get(new NSKey(mnm1, s));
            } else {
                // Compute the K0(-n-1,s) coefficients
                return computeValue(-mnm1 - 1, s);
            }
        }

        /** Get the dK<sub>0</sub><sup>-n-1,s</sup> / d&chi; coefficient derivative.
         *  @param mnm1 -n-1 value
         *  @param s s value
         *  @return dK<sub>0</sub><sup>-n-1,s</sup> / d&chi;
         */
        public final double getDerivative(final int mnm1, final int s) {
            if (derivatives.containsKey(new NSKey(mnm1, s))) {
                return derivatives.get(new NSKey(mnm1, s));
            } else {
                // Compute the dK0(-n-1,s) / dX derivative
                return computeDerivative(-mnm1 - 1, s);
            }
        }

        /** Kernels initialization. */
        private void initialize() {
            coefficients.put(new NSKey(0, 0), 1.);
            coefficients.put(new NSKey(-1, 0), 0.);
            coefficients.put(new NSKey(-1, 1), 0.);
            coefficients.put(new NSKey(-2, 0), X);
            coefficients.put(new NSKey(-2, 1), 0.);
            coefficients.put(new NSKey(-3, 0), XXX);
            coefficients.put(new NSKey(-3, 1), 0.5 * XXX);
            derivatives.put(new NSKey(0, 0), 0.);
        }

        /** Compute the K<sub>0</sub><sup>-n-1,s</sup> coefficient from equation 2.7.3-(6).
         * @param n n  positive value. For a given 'n', the K<sub>0</sub><sup>-n-1,s</sup> is returned.
         * @param s s value
         * @return K<sub>0</sub><sup>-n-1,s</sup>
         */
        private double computeValue(final int n, final int s) {
            // Initialize return value
            double kns = 0.;

            final NSKey key = new NSKey(-n - 1, s);

            if (coefficients.containsKey(key)) {
                kns = coefficients.get(key);
            } else {
                if (n == s) {
                    kns = 0.;
                } else if (n == (s + 1)) {
                    kns = FastMath.pow(X, 1 + 2 * s) / FastMath.pow(2, s);
                } else {
                    final NSKey keymNS = new NSKey(-n, s);
                    double kmNS = 0.;
                    if (coefficients.containsKey(keymNS)) {
                        kmNS = coefficients.get(keymNS);
                    } else {
                        kmNS = computeValue(n - 1, s);
                    }

                    final NSKey keymNp1S = new NSKey(-(n - 1), s);
                    double kmNp1S = 0.;
                    if (coefficients.containsKey(keymNp1S)) {
                        kmNp1S = coefficients.get(keymNp1S);
                    } else {
                        kmNp1S = computeValue(n - 2, s);
                    }

                    kns = (n - 1.) * XX * ((2. * n - 3.) * kmNS - (n - 2.) * kmNp1S);
                    kns /= (n + s - 1.) * (n - s - 1.);
                }
                // Add K(-n-1, s)
                coefficients.put(key, kns);
            }
            return kns;
        }

        /** Compute dK<sub>0</sub><sup>-n-1,s</sup> / d&chi; from Equation 3.1-(7).
         *  @param n n positive value. For a given 'n', the dK<sub>0</sub><sup>-n-1,s</sup> / d&chi; is returned.
         *  @param s s value
         *  @return dK<sub>0</sub><sup>-n-1,s</sup> / d&chi;
         */
        private double computeDerivative(final int n, final int s) {
            // Initialize return value
            double dkdxns = 0.;

            final NSKey key = new NSKey(-n - 1, s);
            if (n == s) {
                derivatives.put(key, 0.);
            } else if (n == s + 1) {
                dkdxns = (1. + 2. * s) * FastMath.pow(X, 2 * s) / FastMath.pow(2, s);
            } else {
                final NSKey keymN = new NSKey(-n, s);
                double dkmN = 0.;
                if (derivatives.containsKey(keymN)) {
                    dkmN = derivatives.get(keymN);
                } else {
                    dkmN = computeDerivative(n - 1, s);
                }
                final NSKey keymNp1 = new NSKey(-n + 1, s);
                double dkmNp1 = 0.;
                if (derivatives.containsKey(keymNp1)) {
                    dkmNp1 = derivatives.get(keymNp1);
                } else {
                    dkmNp1 = computeDerivative(n - 2, s);
                }
                final double kns = getValue(-n - 1, s);
                dkdxns = (n - 1) * XX * ((2. * n - 3.) * dkmN - (n - 2.) * dkmNp1) / ((n + s - 1.) * (n - s - 1.));
                dkdxns += 2. * kns / X;
            }

            derivatives.put(key, dkdxns);
            return dkdxns;
        }

    }

}
