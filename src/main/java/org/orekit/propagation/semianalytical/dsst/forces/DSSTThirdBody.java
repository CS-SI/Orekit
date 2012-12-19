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

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;
import org.orekit.bodies.CelestialBody;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.propagation.semianalytical.dsst.utilities.CoefficientsFactory;
import org.orekit.propagation.semianalytical.dsst.utilities.CoefficientsFactory.NSKey;
import org.orekit.propagation.semianalytical.dsst.utilities.UpperBounds;
import org.orekit.time.AbsoluteDate;

/** Third body attraction perturbation to the
 *  {@link org.orekit.propagation.semianalytical.dsst.DSSTPropagator DSSTPropagator}.
 *
 *  @author Romain Di Costanzo
 *  @author Pascal Parraud
 */
public class DSSTThirdBody  implements DSSTForceModel {

    /** Max power for summation. */
    private static final int       MAX_POWER = 22;

    /** Truncation tolerance for big, eccentric  orbits. */
    private static final double    BIG_TRUNCATION_TOLERANCE = 1.e-1;

    /** Truncation tolerance for small orbits. */
    private static final double    SMALL_TRUNCATION_TOLERANCE = 1.e-4;

    /** The 3rd body to consider. */
    private final CelestialBody    body;

    /** Standard gravitational parameter &mu; for the body in m<sup>3</sup>/s<sup>2</sup>. */
    private final double           gm;

    /** Factorial. */
    private final double[]         fact;

    /** V<sub>ns</sub> coefficients. */
    private TreeMap<NSKey, Double> Vns;

    /** Distance from center of mass of the central body to the 3rd body. */
    private double R3;

    // Equinoctial elements (according to DSST notation)
    /** a. */
    private double a;
    /** e<sub>x</sub>. */
    private double k;
    /** e<sub>y</sub>. */
    private double h;
    /** h<sub>x</sub>. */
    private double q;
    /** h<sub>y</sub>. */
    private double p;

    /** Eccentricity. */
    private double ecc;

    // Direction cosines of the symmetry axis
    /** &alpha;. */
    private double alpha;
    /** &beta;. */
    private double beta;
    /** &gamma;. */
    private double gamma;

    // Common factors for potential computation
    /** &Chi; = 1 / sqrt(1 - e<sup>2</sup>) = 1 / B. */
    private double X;
    /** &Chi;<sup>2</sup>. */
    private double XX;
    /** &Chi;<sup>3</sup>. */
    private double XXX;
    /** -2 * a / A. */
    private double m2aoA;
    /** B / A. */
    private double BoA;
    /** 1 / (A * B). */
    private double ooAB;
    /** -C / (2 * A * B). */
    private double mCo2AB;
    /** B / A(1 + B). */
    private double BoABpo;

    /** Retrograde factor. */
    private int    I;

    /** Maw power for a/R3 in the serie expansion. */
    private int    maxAR3Pow;

    /** Maw power for e in the serie expansion. */
    private int    maxECCPow;

    /** Complete constructor.
     *  @param body the 3rd body to consider
     *  @see org.orekit.bodies.CelestialBodyFactory
     */
    public DSSTThirdBody(final CelestialBody body) {
        this.body = body;
        this.gm   = body.getGM();

        this.maxAR3Pow = Integer.MIN_VALUE;
        this.maxECCPow = Integer.MIN_VALUE;

        this.Vns = CoefficientsFactory.computeVnsCoefficient(MAX_POWER);

        // Factorials computation
        final int dim = 2 * MAX_POWER;
        this.fact = new double[dim];
        fact[0] = 1.;
        for (int i = 1; i < dim; i++) {
            fact[i] = i * fact[i - 1];
        }
    }

    /** Get third body.
     *  @return third body
     */
    public final CelestialBody getBody() {
        return body;
    }

    /** {@inheritDoc} */
    public void initialize(final AuxiliaryElements aux) throws OrekitException {

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

        // Distance from center of mass of the central body to the 3rd body
        final Vector3D bodyPos = body.getPVCoordinates(aux.getDate(), aux.getFrame()).getPosition();
        R3 = bodyPos.getNorm();

        // Direction cosines
        final Vector3D bodyDir = bodyPos.normalize();
        alpha = bodyDir.dotProduct(aux.getVectorF());
        beta  = bodyDir.dotProduct(aux.getVectorG());
        gamma = bodyDir.dotProduct(aux.getVectorW());

        // Equinoctial coefficients
        final double A = aux.getA();
        final double B = aux.getB();
        final double C = aux.getC();

        // &Chi;
        X = 1. / B;
        XX = X * X;
        XXX = X * XX;
        // -2 * a / A
        m2aoA = a / A;
        // B / A
        BoA = B / A;
        // 1 / AB
        ooAB = 1. / (A * B);
        // -C / 2AB
        mCo2AB = -C * ooAB / 2.;
        // B / A(1 + B)
        BoABpo = BoA / (1. + B);

        if (maxAR3Pow == Integer.MIN_VALUE) {
            // Set the highest powers of e and a/R3 in the analytical power
            // series expansion for 3rd body potential derivatives
            truncation();
        }
    }

    /** {@inheritDoc} */
    public double[] getMeanElementRate(final SpacecraftState currentState) throws OrekitException {

        // Compute potential U derivatives
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
                                               final double[] meanElements) throws OrekitException {
        // TODO: not implemented yet, Short Periodic Variations are set to null
        return new double[] {0., 0., 0., 0., 0., 0.};
    }

    /** Computes the highest power of the eccentricity and the highest power
     *  of a/R3 to appear in the truncated analytical power series expansion.
     *  <p>
     *  This method computes the upper value for the 3rd body potential and
     *  determines the maximal values from wich upper values give potential
     *  terms inferior to a defined tolerance.
     *  </p>
     */
    private void truncation() {
        // Truncation tolerance.
        final double aor = a / R3;
        final double tol = ( aor > .3 || (aor > .15  && ecc > .25) ) ? BIG_TRUNCATION_TOLERANCE : SMALL_TRUNCATION_TOLERANCE;

        // Utilities for truncation
        // Set a lower bound for eccentricity
        final double eo2  = FastMath.max(0.0025, ecc / 2.);
        final double x2o2 = XX / 2.;
        final double[] eccPwr = new double[MAX_POWER];
        final double[] chiPwr = new double[MAX_POWER];
        eccPwr[0] = 1.;
        chiPwr[0] = X;
        for (int i = 1; i < MAX_POWER; i++) {
            eccPwr[i] = eccPwr[i - 1] * eo2;
            chiPwr[i] = chiPwr[i - 1] * x2o2;
        }

        // Auxiliary quantities.
        final double ao2rxx = aor / (2. * XX);
        double xmuarn       = ao2rxx * ao2rxx * gm / (X * R3);
        double term         = 0.;

        // Compute max power for a/R3 and e.
        maxAR3Pow = 2;
        maxECCPow = 0;
        int n     = 2;
        int m     = 2;
        int nsmd2 = 0;

        do {
            // Upper bound for Tnm.
            term =  xmuarn *
                   (fact[n + m] / (fact[nsmd2] * fact[nsmd2 + m])) *
                   (fact[n + m + 1] / (fact[m] * fact[n + 1])) *
                   (fact[n - m + 1] / fact[n + 1]) *
                   eccPwr[m] * UpperBounds.getDnl(XX, chiPwr[m], n + 2, m);

            if (term < tol) {
                if (m == 0) {
                    break;
                } else if (m < 2) {
                    xmuarn *= ao2rxx;
                    m = 0;
                    n++;
                    nsmd2++;
                } else {
                    m -= 2;
                    nsmd2++;
                }
            } else {
                maxAR3Pow = n;
                maxECCPow = (maxECCPow < m) ? m : maxECCPow;
                xmuarn *= ao2rxx;
                m++;
                n++;
            }
        } while (n < MAX_POWER);

        maxECCPow = (maxECCPow > maxAR3Pow) ? maxAR3Pow : maxECCPow;
    }

    /** Compute potential derivatives.
     *  @return derivatives of the potential with respect to orbital parameters
     *  @throws OrekitException if Hansen coefficients cannot be computed
     */
    private double[] computeUDerivatives() throws OrekitException {

        // Hansen coefficients
        final HansenThirdBody hansen = new HansenThirdBody();
        // Gs coefficients
        final double[][] GsHs = CoefficientsFactory.computeGsHs(k, h, alpha, beta, maxECCPow);
        // Qns coefficients
        final double[][] Qns = CoefficientsFactory.computeQnsCoefficient(gamma, maxAR3Pow + 1);
        // mu3 / R3
        final double muoR3 = gm / R3;
        // a / R3
        final double aoR3  = a / R3;

        // Potential derivatives
        double dUda  = 0.;
        double dUdk  = 0.;
        double dUdh  = 0.;
        double dUdAl = 0.;
        double dUdBe = 0.;
        double dUdGa = 0.;

        for (int s = 0; s <= maxECCPow; s++) {
            // Get the current Gs and Hs coefficient
            final double gs = GsHs[0][s];

            // Compute partial derivatives of Gs from 3.1-(9)
            // First get the G(s-1) and the H(s-1) coefficient : SET TO 0 IF < 0
            final double sxgsm1 = s > 0 ? s * GsHs[0][s - 1] : 0.;
            final double sxhsm1 = s > 0 ? s * GsHs[1][s - 1] : 0.;
            // Get derivatives
            final double dGsdh  = beta  * sxgsm1 - alpha * sxhsm1;
            final double dGsdk  = alpha * sxgsm1 + beta  * sxhsm1;
            final double dGsdAl = k * sxgsm1 - h * sxhsm1;
            final double dGsdBe = h * sxgsm1 + k * sxhsm1;

            // Kronecker symbol (2 - delta(0,s))
            final double delta0s = (s == 0) ? 1. : 2.;

            for (int n = FastMath.max(2, s); n <= maxAR3Pow; n++) {
                // (n - s) must be even
                if ((n - s) % 2 == 0) {
                    // Extract data from previous computation :
                    final double vns   = Vns.get(new NSKey(n, s));
                    final double kns   = hansen.getValue(n, s);
                    final double qns   = Qns[n][s];
                    final double aoR3n = FastMath.pow(aoR3, n);
                    final double dkns  = hansen.getDerivative(n, s);
                    final double coef0 = delta0s * aoR3n * vns;
                    final double coef1 = coef0 * qns;
                    // dQns/dGamma = Q(n, s + 1) from Equation 3.1-(8)
                    // for n = s, Q(n, n + 1) = 0. (Cefola & Broucke, 1975)
                    final double dqns = (n == s) ? 0. : Qns[n][s + 1];

                    // Compute dU / da :
                    dUda += coef1 * n * kns * gs;
                    // Compute dU / dh
                    dUdh += coef1 * (kns * dGsdh + h * XXX * gs * dkns);
                    // Compute dU / dk
                    dUdk += coef1 * (kns * dGsdk + k * XXX * gs * dkns);
                    // Compute dU / dAlpha
                    dUdAl += coef1 * kns * dGsdAl;
                    // Compute dU / dBeta
                    dUdBe += coef1 * kns * dGsdBe;
                    // Compute dU / dGamma with dQns/dGamma = Q(n, s + 1)
                    dUdGa += coef0 * kns * dqns * gs;
                }
            }
        }

        dUda  *= muoR3 / a;
        dUdk  *= muoR3;
        dUdh  *= muoR3;
        dUdAl *= muoR3;
        dUdBe *= muoR3;
        dUdGa *= muoR3;

        return new double[] {dUda, dUdk, dUdh, dUdAl, dUdBe, dUdGa};

    }

    /** Hansen coefficients for 3rd body force model.
     *  <p>
     *  Hansen coefficients are functions of the eccentricity.
     *  For a given eccentricity, the computed elements are stored in a map.
     *  </p>
     */
    private class HansenThirdBody {

        /** Map to store every Hansen coefficient computed. */
        private TreeMap<NSKey, Double> coefficients;

        /** Map to store every Hansen coefficient derivative computed. */
        private TreeMap<NSKey, Double> derivatives;

        /** Simple constructor. */
        public HansenThirdBody() {
            coefficients = new TreeMap<CoefficientsFactory.NSKey, Double>();
            derivatives  = new TreeMap<CoefficientsFactory.NSKey, Double>();
            initialize();
        }

        /** Get the K<sub>0</sub><sup>n,s</sup> coefficient value.
         *  @param n n value
         *  @param s s value
         *  @return K<sub>0</sub><sup>n,s</sup>
         */
        public final double getValue(final int n, final int s) {
            if (coefficients.containsKey(new NSKey(n, s))) {
                return coefficients.get(new NSKey(n, s));
            } else {
                // Compute the K0(n,s) coefficients
                return computeValue(n, s);
            }
        }

        /** Get the dK<sub>0</sub><sup>n,s</sup> / d&x; coefficient derivative.
         *  @param n n value
         *  @param s s value
         *  @return dK<sub>j</sub><sup>n,s</sup> / d&x;
         */
        public final double getDerivative(final int n, final int s) {
            if (derivatives.containsKey(new NSKey(n, s))) {
                return derivatives.get(new NSKey(n, s));
            } else {
                // Compute the dK0(n,s) / dX derivative
                return computeDerivative(n, s);
            }
        }

        /** Initialization. */
        private void initialize() {
            final double ec2 = ecc * ecc;
            final double oX3 = 1. / XXX;
            coefficients.put(new NSKey(0, 0),  1.);
            coefficients.put(new NSKey(0, 1), -1.);
            coefficients.put(new NSKey(1, 0),  1. + 0.5 * ec2);
            coefficients.put(new NSKey(1, 1), -1.5);
            coefficients.put(new NSKey(2, 0),  1. + 1.5 * ec2);
            coefficients.put(new NSKey(2, 1), -2. - 0.5 * ec2);
            derivatives.put(new NSKey(0, 0),  0.);
            derivatives.put(new NSKey(1, 0),  oX3);
            derivatives.put(new NSKey(2, 0),  3. * oX3);
            derivatives.put(new NSKey(2, 1), -oX3);
        }

        /** Compute K<sub>0</sub><sup>n,s</sup> from Equation 2.7.3-(7)(8).
         *  @param n n value
         *  @param s s value
         *  @return K<sub>0</sub><sup>n,s</sup>
         */
        private double computeValue(final int n, final int s) {
            // Initialize return value
            double kns = 0.;

            if (n == (s - 1)) {

                final NSKey key = new NSKey(s - 2, s - 1);
                if (coefficients.containsKey(key)) {
                    kns = coefficients.get(key);
                } else {
                    kns = computeValue(s - 2, s - 1);
                }

                kns *= -(2. * s - 1.) / s;

            } else if (n == s) {

                final NSKey key = new NSKey(s - 1, s);
                if (coefficients.containsKey(key)) {
                    kns = coefficients.get(key);
                } else {
                    kns = computeValue(s - 1, s);
                }

                kns *= (2. * s + 1.) / (s + 1.);

            } else if (n > s) {

                final NSKey key1 = new NSKey(n - 1, s);
                double knM1 = 0.;
                if (coefficients.containsKey(key1)) {
                    knM1 = coefficients.get(key1);
                } else {
                    knM1 = computeValue(n - 1, s);
                }

                final NSKey key2 = new NSKey(n - 2, s);
                double knM2 = 0.;
                if (coefficients.containsKey(key2)) {
                    knM2 = coefficients.get(key2);
                } else {
                    knM2 = computeValue(n - 2, s);
                }

                final double val1 = (2. * n + 1.) / (n + 1.);
                final double val2 = (n + s) * (n - s) / (n * (n + 1.) * XX);

                kns = val1 * knM1 - val2 * knM2;
            }

            coefficients.put(new NSKey(n, s), kns);
            return kns;
        }

        /** Compute dK<sub>0</sub><sup>n,s</sup> / d&x; from Equation 3.2-(3).
         *  @param n n value
         *  @param s s value
         *  @return dK<sub>0</sub><sup>n,s</sup> / d&x;
         */
        private double computeDerivative(final int n, final int s) {
            // Initialize return value
            double dknsdx = 0.;

            if (n > s) {

                final NSKey keyNm1 = new NSKey(n - 1, s);
                double dKnM1 = 0.;
                if (derivatives.containsKey(keyNm1)) {
                    dKnM1 = derivatives.get(keyNm1);
                } else {
                    dKnM1 = computeDerivative(n - 1, s);
                }

                final NSKey keyNm2 = new NSKey(n - 2, s);
                double dKnM2 = 0.;
                if (derivatives.containsKey(keyNm2)) {
                    dKnM2 = derivatives.get(keyNm2);
                } else {
                    dKnM2 = computeDerivative(n - 2, s);
                }

                final double knM2 = getValue(n - 2, s);

                final double val1 = (2. * n + 1.) / (n + 1.);
                final double coef = (n + s) * (n - s) / (n * (n + 1.));
                final double val2 = coef / XX;
                final double val3 = 2. * coef / XXX;

                dknsdx = val1 * dKnM1 - val2 * dKnM2 + val3 * knM2;
            }

            derivatives.put(new NSKey(n, s), dknsdx);
            return dknsdx;
        }

    }

}
