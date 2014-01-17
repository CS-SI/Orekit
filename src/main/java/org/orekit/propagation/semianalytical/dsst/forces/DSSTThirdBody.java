/* Copyright 2002-2014 CS Systèmes d'Information
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
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.propagation.semianalytical.dsst.utilities.CoefficientsFactory;
import org.orekit.propagation.semianalytical.dsst.utilities.CoefficientsFactory.NSKey;
import org.orekit.propagation.semianalytical.dsst.utilities.UpperBounds;
import org.orekit.propagation.semianalytical.dsst.utilities.hansen.HansenThirdBodyLinear;
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
    /** B = sqrt(1 - e<sup>2</sup>). */
    private double B;
    /** B<sup>2</sup>. */
    private double BB;
    /** B<sup>3</sup>. */
    private double BBB;

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
    private int    maxEccPow;

    /** An array that contains the objects needed to build the Hansen coefficients. <br/>
     * The index is s */
    private HansenThirdBodyLinear[] hansenObjects;

    /** Complete constructor.
     *  @param body the 3rd body to consider
     *  @see org.orekit.bodies.CelestialBodyFactory
     */
    public DSSTThirdBody(final CelestialBody body) {
        this.body = body;
        this.gm   = body.getGM();

        this.maxAR3Pow = Integer.MIN_VALUE;
        this.maxEccPow = Integer.MIN_VALUE;

        this.Vns = CoefficientsFactory.computeVns(MAX_POWER);

        // Factorials computation
        final int dim = 2 * MAX_POWER;
        this.fact = new double[dim];
        fact[0] = 1.;
        for (int i = 1; i < dim; i++) {
            fact[i] = i * fact[i - 1];
        }

        //Initialise the HansenCoefficient generator
        this.hansenObjects = new HansenThirdBodyLinear[MAX_POWER + 1];
        for (int s = 0; s <= MAX_POWER; s++) {
            this.hansenObjects[s] = new HansenThirdBodyLinear(MAX_POWER, s);
        }

    }

    /** Get third body.
     *  @return third body
     */
    public CelestialBody getBody() {
        return body;
    }

    /** Computes the highest power of the eccentricity and the highest power
     *  of a/R3 to appear in the truncated analytical power series expansion.
     *  <p>
     *  This method computes the upper value for the 3rd body potential and
     *  determines the maximal powers for the eccentricity and a/R3 producing
     *  potential terms bigger than a defined tolerance.
     *  </p>
     *  @param aux auxiliary elements related to the current orbit
     *  @throws OrekitException if some specific error occurs
     */
    public void initialize(final AuxiliaryElements aux)
        throws OrekitException {

        // Initializes specific parameters.
        initializeStep(aux);

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
        maxEccPow = 0;
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
                maxEccPow = FastMath.max(m, maxEccPow);
                xmuarn *= ao2rxx;
                m++;
                n++;
            }
        } while (n < MAX_POWER);

        maxEccPow = FastMath.min(maxAR3Pow, maxEccPow);

    }

    /** {@inheritDoc} */
    public void initializeStep(final AuxiliaryElements aux) throws OrekitException {

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
        B = aux.getB();
        final double C = aux.getC();

        //&Chi;<sup>-2</sup>.
        BB = B * B;
        //&Chi;<sup>-3</sup>.
        BBB = BB * B;

        // &Chi;
        X = 1. / B;
        XX = X * X;
        XXX = X * XX;
        // -2 * a / A
        m2aoA = -2. * a / A;
        // B / A
        BoA = B / A;
        // 1 / AB
        ooAB = 1. / (A * B);
        // -C / 2AB
        mCo2AB = -C * ooAB / 2.;
        // B / A(1 + B)
        BoABpo = BoA / (1. + B);

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

    /** {@inheritDoc} */
    public EventDetector[] getEventsDetectors() {
        return null;
    }

    /** Compute potential derivatives.
     *  @return derivatives of the potential with respect to orbital parameters
     *  @throws OrekitException if Hansen coefficients cannot be computed
     */
    private double[] computeUDerivatives() throws OrekitException {

        // Gs and Hs coefficients
        final double[][] GsHs = CoefficientsFactory.computeGsHs(k, h, alpha, beta, maxEccPow);
        // Qns coefficients
        final double[][] Qns = CoefficientsFactory.computeQns(gamma, maxAR3Pow, maxEccPow);
        // a / R3 up to power maxAR3Pow
        final double aoR3 = a / R3;
        final double[] aoR3Pow = new double[maxAR3Pow + 1];
        aoR3Pow[0] = 1.;
        for (int i = 1; i <= maxAR3Pow; i++) {
            aoR3Pow[i] = aoR3 * aoR3Pow[i - 1];
        }

        // Potential derivatives
        double dUda  = 0.;
        double dUdk  = 0.;
        double dUdh  = 0.;
        double dUdAl = 0.;
        double dUdBe = 0.;
        double dUdGa = 0.;

        for (int s = 0; s <= maxEccPow; s++) {
            // initialise the Hansen roots
            this.hansenObjects[s].computeInitValues(B, BB, BBB);

            // Get the current Gs coefficient
            final double gs = GsHs[0][s];

            // Compute Gs partial derivatives from 3.1-(9)
            double dGsdh  = 0.;
            double dGsdk  = 0.;
            double dGsdAl = 0.;
            double dGsdBe = 0.;
            if (s > 0) {
                // First get the G(s-1) and the H(s-1) coefficients
                final double sxGsm1 = s * GsHs[0][s - 1];
                final double sxHsm1 = s * GsHs[1][s - 1];
                // Then compute derivatives
                dGsdh  = beta  * sxGsm1 - alpha * sxHsm1;
                dGsdk  = alpha * sxGsm1 + beta  * sxHsm1;
                dGsdAl = k * sxGsm1 - h * sxHsm1;
                dGsdBe = h * sxGsm1 + k * sxHsm1;
            }

            // Kronecker symbol (2 - delta(0,s))
            final double delta0s = (s == 0) ? 1. : 2.;

            for (int n = FastMath.max(2, s); n <= maxAR3Pow; n++) {
                // (n - s) must be even
                if ((n - s) % 2 == 0) {
                    // Extract data from previous computation :
                    final double kns   = this.hansenObjects[s].getValue(n, B);
                    final double dkns  = this.hansenObjects[s].getDerivative(n, B);

                    final double vns   = Vns.get(new NSKey(n, s));
                    final double coef0 = delta0s * aoR3Pow[n] * vns;
                    final double coef1 = coef0 * Qns[n][s];
                    final double coef2 = coef1 * kns;
                    // dQns/dGamma = Q(n, s + 1) from Equation 3.1-(8)
                    // for n = s, Q(n, n + 1) = 0. (Cefola & Broucke, 1975)
                    final double dqns = (n == s) ? 0. : Qns[n][s + 1];

                    // Compute dU / da :
                    dUda += coef2 * n * gs;
                    // Compute dU / dh
                    dUdh += coef1 * (kns * dGsdh + h * XXX * gs * dkns);
                    // Compute dU / dk
                    dUdk += coef1 * (kns * dGsdk + k * XXX * gs * dkns);
                    // Compute dU / dAlpha
                    dUdAl += coef2 * dGsdAl;
                    // Compute dU / dBeta
                    dUdBe += coef2 * dGsdBe;
                    // Compute dU / dGamma
                    dUdGa += coef0 * kns * dqns * gs;
                }
            }
        }

        // mu3 / R3
        final double muoR3 = gm / R3;

        return new double[] {
            dUda  * muoR3 / a,
            dUdk  * muoR3,
            dUdh  * muoR3,
            dUdAl * muoR3,
            dUdBe * muoR3,
            dUdGa * muoR3
        };

    }
}
