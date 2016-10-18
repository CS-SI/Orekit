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
package org.orekit.propagation.semianalytical.dsst.forces;

import java.io.NotSerializableException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;

import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.bodies.CelestialBody;
import org.orekit.errors.OrekitException;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.propagation.semianalytical.dsst.utilities.CjSjCoefficient;
import org.orekit.propagation.semianalytical.dsst.utilities.CoefficientsFactory;
import org.orekit.propagation.semianalytical.dsst.utilities.CoefficientsFactory.NSKey;
import org.orekit.propagation.semianalytical.dsst.utilities.JacobiPolynomials;
import org.orekit.propagation.semianalytical.dsst.utilities.ShortPeriodicsInterpolatedCoefficient;
import org.orekit.propagation.semianalytical.dsst.utilities.UpperBounds;
import org.orekit.propagation.semianalytical.dsst.utilities.hansen.HansenThirdBodyLinear;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeSpanMap;

/** Third body attraction perturbation to the
 *  {@link org.orekit.propagation.semianalytical.dsst.DSSTPropagator DSSTPropagator}.
 *
 *  @author Romain Di Costanzo
 *  @author Pascal Parraud
 */
public class DSSTThirdBody implements DSSTForceModel {

    /** Max power for summation. */
    private static final int    MAX_POWER = 22;

    /** Truncation tolerance for big, eccentric  orbits. */
    private static final double BIG_TRUNCATION_TOLERANCE = 1.e-1;

    /** Truncation tolerance for small orbits. */
    private static final double SMALL_TRUNCATION_TOLERANCE = 1.9e-6;

    /** Number of points for interpolation. */
    private static final int    INTERPOLATION_POINTS = 3;

    /** Maximum power for eccentricity used in short periodic computation. */
    private static final int    MAX_ECCPOWER_SP = 4;

    /** Retrograde factor I.
     *  <p>
     *  DSST model needs equinoctial orbit as internal representation.
     *  Classical equinoctial elements have discontinuities when inclination
     *  is close to zero. In this representation, I = +1. <br>
     *  To avoid this discontinuity, another representation exists and equinoctial
     *  elements can be expressed in a different way, called "retrograde" orbit.
     *  This implies I = -1. <br>
     *  As Orekit doesn't implement the retrograde orbit, I is always set to +1.
     *  But for the sake of consistency with the theory, the retrograde factor
     *  has been kept in the formulas.
     *  </p>
     */
    private static final int    I = 1;

    /** The 3rd body to consider. */
    private final CelestialBody    body;

    /** Standard gravitational parameter μ for the body in m³/s². */
    private final double           gm;

    /** Factorial. */
    private final double[]         fact;

    /** V<sub>ns</sub> coefficients. */
    private final TreeMap<NSKey, Double> Vns;

    /** Distance from center of mass of the central body to the 3rd body. */
    private double R3;

    /** Short period terms. */
    private ThirdBodyShortPeriodicCoefficients shortPeriods;

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
    /** α. */
    private double alpha;
    /** β. */
    private double beta;
    /** γ. */
    private double gamma;

    // Common factors for potential computation
    /** A = n * a². */
    private double A;
    /** B = sqrt(1 - e²). */
    private double B;
    /** C = 1 + p² + q². */
    private double C;
    /** B². */
    private double BB;
    /** B³. */
    private double BBB;

    /** The mean motion (n). */
    private double meanMotion;

    /** &Chi; = 1 / sqrt(1 - e²) = 1 / B. */
    private double X;
    /** &Chi;². */
    private double XX;
    /** &Chi;³. */
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

    /** Max power for a/R3 in the serie expansion. */
    private int    maxAR3Pow;

    /** Max power for e in the serie expansion. */
    private int    maxEccPow;

    /** Max power for e in the serie expansion (for short periodics). */
    private int    maxEccPowShort;

    /** Max frequency of F. */
    private int    maxFreqF;

    /** An array that contains the objects needed to build the Hansen coefficients. <br/>
     * The index is s */
    private final HansenThirdBodyLinear[] hansenObjects;

    /** The current value of the U function. <br/>
     * Needed for the short periodic contribution */
    private double U;

    /** Qns coefficients. */
    private double[][] Qns;
    /** a / R3 up to power maxAR3Pow. */
    private double[] aoR3Pow;
    /** mu3 / R3. */
    private double muoR3;

    /** b = 1 / (1 + sqrt(1 - e²)) = 1 / (1 + B).*/
    private double b;

    /** h * &Chi;³. */
    private double hXXX;
    /** k * &Chi;³. */
    private double kXXX;

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
     *  @param meanOnly only mean elements will be used for the propagation
     *  @throws OrekitException if some specific error occurs
     */
    @Override
    public List<ShortPeriodTerms> initialize(final AuxiliaryElements aux, final boolean meanOnly)
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

        // allocate the array aoR3Pow
        aoR3Pow = new double[maxAR3Pow + 1];

        maxFreqF = maxAR3Pow + 1;
        maxEccPowShort = MAX_ECCPOWER_SP;

        Qns = CoefficientsFactory.computeQns(gamma, maxAR3Pow, FastMath.max(maxEccPow, maxEccPowShort));
        final int jMax = maxAR3Pow + 1;
        shortPeriods = new ThirdBodyShortPeriodicCoefficients(jMax, INTERPOLATION_POINTS,
                                                              maxFreqF, body.getName(),
                                                              new TimeSpanMap<Slot>(new Slot(jMax, INTERPOLATION_POINTS)));

        final List<ShortPeriodTerms> list = new ArrayList<ShortPeriodTerms>();
        list.add(shortPeriods);
        return list;

    }

    /** {@inheritDoc} */
    @Override
    public void initializeStep(final AuxiliaryElements aux) throws OrekitException {

        // Equinoctial elements
        a = aux.getSma();
        k = aux.getK();
        h = aux.getH();
        q = aux.getQ();
        p = aux.getP();

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
        A = aux.getA();
        B = aux.getB();
        C = aux.getC();
        meanMotion = aux.getMeanMotion();

        //&Chi;<sup>-2</sup>.
        BB = B * B;
        //&Chi;<sup>-3</sup>.
        BBB = BB * B;

        //b = 1 / (1 + B)
        b = 1. / (1. + B);

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

        // mu3 / R3
        muoR3 = gm / R3;

        //h * &Chi;³
        hXXX = h * XXX;
        //k * &Chi;³
        kXXX = k * XXX;
    }

    /** {@inheritDoc} */
    @Override
    public double[] getMeanElementRate(final SpacecraftState currentState) {

        // Qns coefficients
        Qns = CoefficientsFactory.computeQns(gamma, maxAR3Pow, maxEccPow);
        // a / R3 up to power maxAR3Pow
        final double aoR3 = a / R3;
        aoR3Pow[0] = 1.;
        for (int i = 1; i <= maxAR3Pow; i++) {
            aoR3Pow[i] = aoR3 * aoR3Pow[i - 1];
        }

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
    @Override
    public void updateShortPeriodTerms(final SpacecraftState ... meanStates)
        throws OrekitException {

        final Slot slot = shortPeriods.createSlot(meanStates);

        for (final SpacecraftState meanState : meanStates) {

            initializeStep(new AuxiliaryElements(meanState.getOrbit(), I));

            // a / R3 up to power maxAR3Pow
            final double aoR3 = a / R3;
            aoR3Pow[0] = 1.;
            for (int i = 1; i <= maxAR3Pow; i++) {
                aoR3Pow[i] = aoR3 * aoR3Pow[i - 1];
            }

            // Qns coefficients
            Qns = CoefficientsFactory.computeQns(gamma, maxAR3Pow, FastMath.max(maxEccPow, maxEccPowShort));
            final GeneratingFunctionCoefficients gfCoefs =
                            new GeneratingFunctionCoefficients(maxAR3Pow, MAX_ECCPOWER_SP, maxAR3Pow + 1);

            //Compute additional quantities
            // 2 * a / An
            final double ax2oAn  = -m2aoA / meanMotion;
            // B / An
            final double BoAn  = BoA / meanMotion;
            // 1 / ABn
            final double ooABn = ooAB / meanMotion;
            // C / 2ABn
            final double Co2ABn = -mCo2AB / meanMotion;
            // B / (A * (1 + B) * n)
            final double BoABpon = BoABpo / meanMotion;
            // -3 / n²a² = -3 / nA
            final double m3onA = -3 / (A * meanMotion);

            //Compute the C<sub>i</sub><sup>j</sup> and S<sub>i</sub><sup>j</sup> coefficients.
            for (int j = 1; j < slot.cij.length; j++) {
                // First compute the C<sub>i</sub><sup>j</sup> coefficients
                final double[] currentCij = new double[6];

                // Compute the cross derivatives operator :
                final double SAlphaGammaCj   = alpha * gfCoefs.getdSdgammaCj(j) - gamma * gfCoefs.getdSdalphaCj(j);
                final double SAlphaBetaCj    = alpha * gfCoefs.getdSdbetaCj(j)  - beta  * gfCoefs.getdSdalphaCj(j);
                final double SBetaGammaCj    =  beta * gfCoefs.getdSdgammaCj(j) - gamma * gfCoefs.getdSdbetaCj(j);
                final double ShkCj           =     h * gfCoefs.getdSdkCj(j)     -  k    * gfCoefs.getdSdhCj(j);
                final double pSagmIqSbgoABnCj = (p * SAlphaGammaCj - I * q * SBetaGammaCj) * ooABn;
                final double ShkmSabmdSdlCj  =  ShkCj - SAlphaBetaCj - gfCoefs.getdSdlambdaCj(j);

                currentCij[0] =  ax2oAn * gfCoefs.getdSdlambdaCj(j);
                currentCij[1] =  -(BoAn * gfCoefs.getdSdhCj(j) + h * pSagmIqSbgoABnCj + k * BoABpon * gfCoefs.getdSdlambdaCj(j));
                currentCij[2] =    BoAn * gfCoefs.getdSdkCj(j) + k * pSagmIqSbgoABnCj - h * BoABpon * gfCoefs.getdSdlambdaCj(j);
                currentCij[3] =  Co2ABn * (q * ShkmSabmdSdlCj - I * SAlphaGammaCj);
                currentCij[4] =  Co2ABn * (p * ShkmSabmdSdlCj - SBetaGammaCj);
                currentCij[5] = -ax2oAn * gfCoefs.getdSdaCj(j) + BoABpon * (h * gfCoefs.getdSdhCj(j) + k * gfCoefs.getdSdkCj(j)) + pSagmIqSbgoABnCj + m3onA * gfCoefs.getSCj(j);

                // add the computed coefficients to the interpolators
                slot.cij[j].addGridPoint(meanState.getDate(), currentCij);

                // Compute the S<sub>i</sub><sup>j</sup> coefficients
                final double[] currentSij = new double[6];

                // Compute the cross derivatives operator :
                final double SAlphaGammaSj   = alpha * gfCoefs.getdSdgammaSj(j) - gamma * gfCoefs.getdSdalphaSj(j);
                final double SAlphaBetaSj    = alpha * gfCoefs.getdSdbetaSj(j)  - beta  * gfCoefs.getdSdalphaSj(j);
                final double SBetaGammaSj    =  beta * gfCoefs.getdSdgammaSj(j) - gamma * gfCoefs.getdSdbetaSj(j);
                final double ShkSj           =     h * gfCoefs.getdSdkSj(j)     -  k    * gfCoefs.getdSdhSj(j);
                final double pSagmIqSbgoABnSj = (p * SAlphaGammaSj - I * q * SBetaGammaSj) * ooABn;
                final double ShkmSabmdSdlSj  =  ShkSj - SAlphaBetaSj - gfCoefs.getdSdlambdaSj(j);

                currentSij[0] =  ax2oAn * gfCoefs.getdSdlambdaSj(j);
                currentSij[1] =  -(BoAn * gfCoefs.getdSdhSj(j) + h * pSagmIqSbgoABnSj + k * BoABpon * gfCoefs.getdSdlambdaSj(j));
                currentSij[2] =    BoAn * gfCoefs.getdSdkSj(j) + k * pSagmIqSbgoABnSj - h * BoABpon * gfCoefs.getdSdlambdaSj(j);
                currentSij[3] =  Co2ABn * (q * ShkmSabmdSdlSj - I * SAlphaGammaSj);
                currentSij[4] =  Co2ABn * (p * ShkmSabmdSdlSj - SBetaGammaSj);
                currentSij[5] = -ax2oAn * gfCoefs.getdSdaSj(j) + BoABpon * (h * gfCoefs.getdSdhSj(j) + k * gfCoefs.getdSdkSj(j)) + pSagmIqSbgoABnSj + m3onA * gfCoefs.getSSj(j);

                // add the computed coefficients to the interpolators
                slot.sij[j].addGridPoint(meanState.getDate(), currentSij);

                if (j == 1) {
                    //Compute the C⁰ coefficients using Danielson 2.5.2-15a.
                    final double[] value = new double[6];
                    for (int i = 0; i < 6; ++i) {
                        value[i] = currentCij[i] * k / 2. + currentSij[i] * h / 2.;
                    }
                    slot.cij[0].addGridPoint(meanState.getDate(), value);
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public EventDetector[] getEventsDetectors() {
        return null;
    }

    /** Compute potential derivatives.
     *  @return derivatives of the potential with respect to orbital parameters
     */
    private double[] computeUDerivatives() {

        // Gs and Hs coefficients
        final double[][] GsHs = CoefficientsFactory.computeGsHs(k, h, alpha, beta, maxEccPow);

        // Initialise U.
        U = 0.;

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

                    //Compute U:
                    U += coef2 * gs;

                    // Compute dU / da :
                    dUda += coef2 * n * gs;
                    // Compute dU / dh
                    dUdh += coef1 * (kns * dGsdh + hXXX * gs * dkns);
                    // Compute dU / dk
                    dUdk += coef1 * (kns * dGsdk + kXXX * gs * dkns);
                    // Compute dU / dAlpha
                    dUdAl += coef2 * dGsdAl;
                    // Compute dU / dBeta
                    dUdBe += coef2 * dGsdBe;
                    // Compute dU / dGamma
                    dUdGa += coef0 * kns * dqns * gs;
                }
            }
        }

        // multiply by mu3 / R3
        U *= muoR3;

        return new double[] {
            dUda  * muoR3 / a,
            dUdk  * muoR3,
            dUdh  * muoR3,
            dUdAl * muoR3,
            dUdBe * muoR3,
            dUdGa * muoR3
        };

    }

    /** {@inheritDoc} */
    @Override
    public void registerAttitudeProvider(final AttitudeProvider provider) {
        //nothing is done since this contribution is not sensitive to attitude
    }

    /** Computes the C<sup>j</sup> and S<sup>j</sup> coefficients Danielson 4.2-(15,16)
     * and their derivatives.
     *  <p>
     *  CS Mathematical Report $3.5.3.2
     *  </p>
     */
    private class FourierCjSjCoefficients {

        /** The coefficients G<sub>n, s</sub> and their derivatives. */
        private final GnsCoefficients gns;

        /** the coefficients e<sup>-|j-s|</sup>*w<sub>j</sub><sup>n, s</sup> and their derivatives by h and k. */
        private final WnsjEtomjmsCoefficient wnsjEtomjmsCoefficient;

        /** The terms containing the coefficients C<sub>j</sub> and S<sub>j</sub> of (α, β) or (k, h). */
        private final CjSjAlphaBetaKH ABDECoefficients;

        /** The Fourier coefficients C<sup>j</sup> and their derivatives.
         * <p>
         * Each column of the matrix contains the following values: <br/>
         * - C<sup>j</sup> <br/>
         * - dC<sup>j</sup> / da <br/>
         * - dC<sup>j</sup> / dk <br/>
         * - dC<sup>j</sup> / dh <br/>
         * - dC<sup>j</sup> / dα <br/>
         * - dC<sup>j</sup> / dβ <br/>
         * - dC<sup>j</sup> / dγ <br/>
         * </p>
         */
        private final double[][] cj;

        /** The S<sup>j</sup> coefficients and their derivatives.
         * <p>
         * Each column of the matrix contains the following values: <br/>
         * - S<sup>j</sup> <br/>
         * - dS<sup>j</sup> / da <br/>
         * - dS<sup>j</sup> / dk <br/>
         * - dS<sup>j</sup> / dh <br/>
         * - dS<sup>j</sup> / dα <br/>
         * - dS<sup>j</sup> / dβ <br/>
         * - dS<sup>j</sup> / dγ <br/>
         * </p>
         */
        private final double[][] sj;

        /** The Coefficients C<sup>j</sup><sub>,λ</sub>.
         * <p>
         * See Danielson 4.2-21
         * </p>
         */
        private final double[] cjlambda;

        /** The Coefficients S<sup>j</sup><sub>,λ</sub>.
        * <p>
        * See Danielson 4.2-21
        * </p>
        */
        private final double[] sjlambda;

        /** Maximum value for n. */
        private final int nMax;

        /** Maximum value for s. */
        private final int sMax;

        /** Maximum value for j. */
        private final int jMax;

        /**
         * Private constructor.
         *
         * @param nMax maximum value for n index
         * @param sMax maximum value for s index
         * @param jMax maximum value for j index
         */
        FourierCjSjCoefficients(final int nMax, final int sMax, final int jMax) {
            //Save parameters
            this.nMax = nMax;
            this.sMax = sMax;
            this.jMax = jMax;

            //Create objects
            wnsjEtomjmsCoefficient = new WnsjEtomjmsCoefficient();
            ABDECoefficients = new CjSjAlphaBetaKH();
            gns = new GnsCoefficients(nMax, sMax);

            //create arays
            this.cj = new double[7][jMax + 1];
            this.sj = new double[7][jMax + 1];
            this.cjlambda = new double[jMax];
            this.sjlambda = new double[jMax];

            computeCoefficients();
        }

        /**
         * Compute all coefficients.
         */
        private void computeCoefficients() {

            for (int j = 1; j <= jMax; j++) {
                // initialise the coefficients
                for (int i = 0; i <= 6; i++) {
                    cj[i][j] = 0.;
                    sj[i][j] = 0.;
                }
                if (j < jMax) {
                    // initialise the C<sup>j</sup><sub>,λ</sub> and S<sup>j</sup><sub>,λ</sub> coefficients
                    cjlambda[j] = 0.;
                    sjlambda[j] = 0.;
                }
                for (int s = 0; s <= sMax; s++) {

                    // Compute the coefficients A<sub>j, s</sub>, B<sub>j, s</sub>, D<sub>j, s</sub> and E<sub>j, s</sub>
                    ABDECoefficients.computeCoefficients(j, s);

                    // compute starting value for n
                    final int minN = FastMath.max(2, FastMath.max(j - 1, s));

                    for (int n = minN; n <= nMax; n++) {
                        // check if n-s is even
                        if ((n - s) % 2 == 0) {
                            // compute the coefficient e<sup>-|j-s|</sup>*w<sub>j</sub><sup>n+1, s</sup> and its derivatives
                            final double[] wjnp1semjms = wnsjEtomjmsCoefficient.computeWjnsEmjmsAndDeriv(j, s, n + 1);

                            // compute the coefficient e<sup>-|j-s|</sup>*w<sub>-j</sub><sup>n+1, s</sup> and its derivatives
                            final double[] wmjnp1semjms = wnsjEtomjmsCoefficient.computeWjnsEmjmsAndDeriv(-j, s, n + 1);

                            // compute common factors
                            final double coef1 = -(wjnp1semjms[0] * ABDECoefficients.getCoefA() + wmjnp1semjms[0] * ABDECoefficients.getCoefB());
                            final double coef2 =   wjnp1semjms[0] * ABDECoefficients.getCoefD() + wmjnp1semjms[0] * ABDECoefficients.getCoefE();

                            //Compute C<sup>j</sup>
                            cj[0][j] += gns.getGns(n, s) * coef1;
                            //Compute dC<sup>j</sup> / da
                            cj[1][j] += gns.getdGnsda(n, s) * coef1;
                            //Compute dC<sup>j</sup> / dk
                            cj[2][j] += -gns.getGns(n, s) *
                                        (
                                            wjnp1semjms[1] * ABDECoefficients.getCoefA() +
                                            wjnp1semjms[0] * ABDECoefficients.getdCoefAdk() +
                                            wmjnp1semjms[1] * ABDECoefficients.getCoefB() +
                                            wmjnp1semjms[0] * ABDECoefficients.getdCoefBdk()
                                         );
                            //Compute dC<sup>j</sup> / dh
                            cj[3][j] += -gns.getGns(n, s) *
                                        (
                                            wjnp1semjms[2] * ABDECoefficients.getCoefA() +
                                            wjnp1semjms[0] * ABDECoefficients.getdCoefAdh() +
                                            wmjnp1semjms[2] * ABDECoefficients.getCoefB() +
                                            wmjnp1semjms[0] * ABDECoefficients.getdCoefBdh()
                                         );
                            //Compute dC<sup>j</sup> / dα
                            cj[4][j] += -gns.getGns(n, s) *
                                        (
                                            wjnp1semjms[0] * ABDECoefficients.getdCoefAdalpha() +
                                            wmjnp1semjms[0] * ABDECoefficients.getdCoefBdalpha()
                                        );
                            //Compute dC<sup>j</sup> / dβ
                            cj[5][j] += -gns.getGns(n, s) *
                                        (
                                            wjnp1semjms[0] * ABDECoefficients.getdCoefAdbeta() +
                                            wmjnp1semjms[0] * ABDECoefficients.getdCoefBdbeta()
                                        );
                            //Compute dC<sup>j</sup> / dγ
                            cj[6][j] += gns.getdGnsdgamma(n, s) * coef1;

                            //Compute S<sup>j</sup>
                            sj[0][j] += gns.getGns(n, s) * coef2;
                            //Compute dS<sup>j</sup> / da
                            sj[1][j] += gns.getdGnsda(n, s) * coef2;
                            //Compute dS<sup>j</sup> / dk
                            sj[2][j] += gns.getGns(n, s) *
                                        (
                                            wjnp1semjms[1] * ABDECoefficients.getCoefD() +
                                            wjnp1semjms[0] * ABDECoefficients.getdCoefDdk() +
                                            wmjnp1semjms[1] * ABDECoefficients.getCoefE() +
                                            wmjnp1semjms[0] * ABDECoefficients.getdCoefEdk()
                                         );
                            //Compute dS<sup>j</sup> / dh
                            sj[3][j] += gns.getGns(n, s) *
                                        (
                                            wjnp1semjms[2] * ABDECoefficients.getCoefD() +
                                            wjnp1semjms[0] * ABDECoefficients.getdCoefDdh() +
                                            wmjnp1semjms[2] * ABDECoefficients.getCoefE() +
                                            wmjnp1semjms[0] * ABDECoefficients.getdCoefEdh()
                                         );
                            //Compute dS<sup>j</sup> / dα
                            sj[4][j] += gns.getGns(n, s) *
                                        (
                                            wjnp1semjms[0] * ABDECoefficients.getdCoefDdalpha() +
                                            wmjnp1semjms[0] * ABDECoefficients.getdCoefEdalpha()
                                        );
                            //Compute dS<sup>j</sup> / dβ
                            sj[5][j] += gns.getGns(n, s) *
                                        (
                                            wjnp1semjms[0] * ABDECoefficients.getdCoefDdbeta() +
                                            wmjnp1semjms[0] * ABDECoefficients.getdCoefEdbeta()
                                        );
                            //Compute dS<sup>j</sup> / dγ
                            sj[6][j] += gns.getdGnsdgamma(n, s) * coef2;

                            //Check if n is greater or equal to j and j is at most jMax-1
                            if (n >= j && j < jMax) {
                                // compute the coefficient e<sup>-|j-s|</sup>*w<sub>j</sub><sup>n, s</sup> and its derivatives
                                final double[] wjnsemjms = wnsjEtomjmsCoefficient.computeWjnsEmjmsAndDeriv(j, s, n);

                                // compute the coefficient e<sup>-|j-s|</sup>*w<sub>-j</sub><sup>n, s</sup> and its derivatives
                                final double[] wmjnsemjms = wnsjEtomjmsCoefficient.computeWjnsEmjmsAndDeriv(-j, s, n);

                                //Compute C<sup>j</sup><sub>,λ</sub>
                                cjlambda[j] += gns.getGns(n, s) * (wjnsemjms[0] * ABDECoefficients.getCoefD() + wmjnsemjms[0] * ABDECoefficients.getCoefE());
                                //Compute S<sup>j</sup><sub>,λ</sub>
                                sjlambda[j] += gns.getGns(n, s) * (wjnsemjms[0] * ABDECoefficients.getCoefA() + wmjnsemjms[0] * ABDECoefficients.getCoefB());
                            }
                        }
                    }
                }
                // Divide by j
                for (int i = 0; i <= 6; i++) {
                    cj[i][j] /= j;
                    sj[i][j] /= j;
                }
            }
            //The C⁰ coefficients are not computed here.
            //They are evaluated at the final point.

            //C⁰<sub>,λ</sub>
            cjlambda[0] = k * cjlambda[1] / 2. + h * sjlambda[1] / 2.;
        }

        /** Get the Fourier coefficient C<sup>j</sup>.
         * @param j j index
         * @return C<sup>j</sup>
         */
        public double getCj(final int j) {
            return cj[0][j];
        }

        /** Get the derivative dC<sup>j</sup>/da.
         * @param j j index
         * @return dC<sup>j</sup>/da
         */
        public double getdCjda(final int j) {
            return cj[1][j];
        }

        /** Get the derivative dC<sup>j</sup>/dk.
         * @param j j index
         * @return dC<sup>j</sup>/dk
         */
        public double getdCjdk(final int j) {
            return cj[2][j];
        }

        /** Get the derivative dC<sup>j</sup>/dh.
         * @param j j index
         * @return dC<sup>j</sup>/dh
         */
        public double getdCjdh(final int j) {
            return cj[3][j];
        }

        /** Get the derivative dC<sup>j</sup>/dα.
         * @param j j index
         * @return dC<sup>j</sup>/dα
         */
        public double getdCjdalpha(final int j) {
            return cj[4][j];
        }

        /** Get the derivative dC<sup>j</sup>/dβ.
         * @param j j index
         * @return dC<sup>j</sup>/dβ
         */
        public double getdCjdbeta(final int j) {
            return cj[5][j];
        }

        /** Get the derivative dC<sup>j</sup>/dγ.
         * @param j j index
         * @return dC<sup>j</sup>/dγ
         */
        public double getdCjdgamma(final int j) {
            return cj[6][j];
        }

        /** Get the Fourier coefficient S<sup>j</sup>.
         * @param j j index
         * @return S<sup>j</sup>
         */
        public double getSj(final int j) {
            return sj[0][j];
        }

        /** Get the derivative dS<sup>j</sup>/da.
         * @param j j index
         * @return dS<sup>j</sup>/da
         */
        public double getdSjda(final int j) {
            return sj[1][j];
        }

        /** Get the derivative dS<sup>j</sup>/dk.
         * @param j j index
         * @return dS<sup>j</sup>/dk
         */
        public double getdSjdk(final int j) {
            return sj[2][j];
        }

        /** Get the derivative dS<sup>j</sup>/dh.
         * @param j j index
         * @return dS<sup>j</sup>/dh
         */
        public double getdSjdh(final int j) {
            return sj[3][j];
        }

        /** Get the derivative dS<sup>j</sup>/dα.
         * @param j j index
         * @return dS<sup>j</sup>/dα
         */
        public double getdSjdalpha(final int j) {
            return sj[4][j];
        }

        /** Get the derivative dS<sup>j</sup>/dβ.
         * @param j j index
         * @return dS<sup>j</sup>/dβ
         */
        public double getdSjdbeta(final int j) {
            return sj[5][j];
        }

        /** Get the derivative dS<sup>j</sup>/dγ.
         * @param j j index
         * @return dS<sup>j</sup>/dγ
         */
        public double getdSjdgamma(final int j) {
            return sj[6][j];
        }

        /** Get the coefficient C⁰<sub>,λ</sub>.
         * @return C⁰<sub>,λ</sub>
         */
        public double getC0Lambda() {
            return cjlambda[0];
        }

        /** Get the coefficient C<sup>j</sup><sub>,λ</sub>.
         * @param j j index
         * @return C<sup>j</sup><sub>,λ</sub>
         */
        public double getCjLambda(final int j) {
            if (j < 1 || j >= jMax) {
                return 0.;
            }
            return cjlambda[j];
        }

        /** Get the coefficient S<sup>j</sup><sub>,λ</sub>.
         * @param j j index
         * @return S<sup>j</sup><sub>,λ</sub>
         */
        public double getSjLambda(final int j) {
            if (j < 1 || j >= jMax) {
                return 0.;
            }
            return sjlambda[j];
        }
    }

    /** This class covers the coefficients e<sup>-|j-s|</sup>*w<sub>j</sub><sup>n, s</sup> and their derivatives by h and k.
     *
     * <p>
     * Starting from Danielson 4.2-9,10,11 and taking into account that fact that: <br />
     * c = e / (1 + (1 - e²)<sup>1/2</sup>) = e / (1 + B) = e * b <br/>
     * the expression e<sup>-|j-s|</sup>*w<sub>j</sub><sup>n, s</sup>
     * can be written as: <br/ >
     * - for |s| > |j| <br />
     * e<sup>-|j-s|</sup>*w<sub>j</sub><sup>n, s</sup> =
     *          (((n + s)!(n - s)!)/((n + j)!(n - j)!)) *
     *          (-b)<sup>|j-s|</sup> *
     *          ((1 - c²)<sup>n-|s|</sup>/(1 + c²)<sup>n</sup>) *
     *          P<sub>n-|s|</sub><sup>|j-s|, |j+s|</sup>(χ) <br />
     * <br />
     * - for |s| <= |j| <br />
     * e<sup>-|j-s|</sup>*w<sub>j</sub><sup>n, s</sup> =
     *          (-b)<sup>|j-s|</sup> *
     *          ((1 - c²)<sup>n-|j|</sup>/(1 + c²)<sup>n</sup>) *
     *          P<sub>n-|j|</sub><sup>|j-s|, |j+s|</sup>(χ)
     * </p>
     *
     * @author Lucian Barbulescu
     */
    private class WnsjEtomjmsCoefficient {

        /** The value c.
         * <p>
         *  c = e / (1 + (1 - e²)<sup>1/2</sup>) = e / (1 + B) = e * b <br/>
         * </p>
         *  */
        private final double c;

        /** c².*/
        private final double c2;

        /** db / dh. */
        private final double dbdh;

        /** db / dk. */
        private final double dbdk;

        /** de / dh. */
        private final double dedh;

        /** de / dk. */
        private final double dedk;

        /** dc / dh = e * db/dh + b * de/dh. */
        private final double dcdh;

        /** dc / dk = e * db/dk + b * de/dk. */
        private final double dcdk;

        /** The values (1 - c²)<sup>n</sup>. <br />
         * The maximum possible value for the power is N + 1 */
        private final double[] omc2tn;

        /** The values (1 + c²)<sup>n</sup>. <br />
         * The maximum possible value for the power is N + 1 */
        private final double[] opc2tn;

        /** The values b<sup>|j-s|</sup>. */
        private final double[] btjms;

        /**
         * Standard constructor.
         */
        WnsjEtomjmsCoefficient() {
            //initialise fields
            c = ecc * b;
            c2 = c * c;

            //b² * χ
            final double b2Chi = b * b * X;
            //Compute derivatives of b
            dbdh = h * b2Chi;
            dbdk = k * b2Chi;

            //Compute derivatives of e
            dedh = h / ecc;
            dedk = k / ecc;

            //Compute derivatives of c
            dcdh = ecc * dbdh + b * dedh;
            dcdk = ecc * dbdk + b * dedk;

            //Compute the powers (1 - c²)<sup>n</sup> and (1 + c²)<sup>n</sup>
            omc2tn = new double[maxAR3Pow + maxFreqF + 2];
            opc2tn = new double[maxAR3Pow + maxFreqF + 2];
            final double omc2 = 1. - c2;
            final double opc2 = 1. + c2;
            omc2tn[0] = 1.;
            opc2tn[0] = 1.;
            for (int i = 1; i <= maxAR3Pow + maxFreqF + 1; i++) {
                omc2tn[i] = omc2tn[i - 1] * omc2;
                opc2tn[i] = opc2tn[i - 1] * opc2;
            }

            //Compute the powers of b
            btjms = new double[maxAR3Pow + maxFreqF + 1];
            btjms[0] = 1.;
            for (int i = 1; i <= maxAR3Pow + maxFreqF; i++) {
                btjms[i] = btjms[i - 1] * b;
            }
        }

        /** Compute the value of the coefficient e<sup>-|j-s|</sup>*w<sub>j</sub><sup>n, s</sup> and its derivatives by h and k. <br />
         *
         * @param j j index
         * @param s s index
         * @param n n index
         * @return an array containing the value of the coefficient at index 0, the derivative by k at index 1 and the derivative by h at index 2
         */
        public double[] computeWjnsEmjmsAndDeriv(final int j, final int s, final int n) {
            final double[] wjnsemjms = new double[] {0., 0., 0.};

            // |j|
            final int absJ = FastMath.abs(j);
            // |s|
            final int absS = FastMath.abs(s);
            // |j - s|
            final int absJmS = FastMath.abs(j - s);
            // |j + s|
            final int absJpS = FastMath.abs(j + s);

            //The lower index of P. Also the power of (1 - c²)
            final int l;
            // the factorial ratio coefficient or 1. if |s| <= |j|
            final double factCoef;
            if (absS > absJ) {
                factCoef = (fact[n + s] / fact[n + j]) * (fact[n - s] / fact[n - j]);
                l = n - absS;
            } else {
                factCoef = 1.;
                l = n - absJ;
            }

            // (-1)<sup>|j-s|</sup>
            final double sign = absJmS % 2 != 0 ? -1. : 1.;
            //(1 - c²)<sup>n-|s|</sup> / (1 + c²)<sup>n</sup>
            final double coef1 = omc2tn[l] / opc2tn[n];
            //-b<sup>|j-s|</sup>
            final double coef2 = sign * btjms[absJmS];
            // P<sub>l</sub><sup>|j-s|, |j+s|</sup>(χ)
            final DerivativeStructure jac =
                    JacobiPolynomials.getValue(l, absJmS, absJpS, new DerivativeStructure(1, 1, 0, X));

            // the derivative of coef1 by c
            final double dcoef1dc = -coef1 * 2. * c * (((double) n) / opc2tn[1] + ((double) l) / omc2tn[1]);
            // the derivative of coef1 by h
            final double dcoef1dh = dcoef1dc * dcdh;
            // the derivative of coef1 by k
            final double dcoef1dk = dcoef1dc * dcdk;

            // the derivative of coef2 by b
            final double dcoef2db = absJmS == 0 ? 0 : sign * (double) absJmS * btjms[absJmS - 1];
            // the derivative of coef2 by h
            final double dcoef2dh = dcoef2db * dbdh;
            // the derivative of coef2 by k
            final double dcoef2dk = dcoef2db * dbdk;

            // the jacobi polinomial value
            final double jacobi = jac.getValue();
            // the derivative of the Jacobi polynomial by h
            final double djacobidh = jac.getPartialDerivative(1) * hXXX;
            // the derivative of the Jacobi polynomial by k
            final double djacobidk = jac.getPartialDerivative(1) * kXXX;

            //group the above coefficients to limit the mathematical operations
            final double term1 = factCoef * coef1 * coef2;
            final double term2 = factCoef * coef1 * jacobi;
            final double term3 = factCoef * coef2 * jacobi;

            //compute e<sup>-|j-s|</sup>*w<sub>j</sub><sup>n, s</sup> and its derivatives by k and h
            wjnsemjms[0] = term1 * jacobi;
            wjnsemjms[1] = dcoef1dk * term3 + dcoef2dk * term2 + djacobidk * term1;
            wjnsemjms[2] = dcoef1dh * term3 + dcoef2dh * term2 + djacobidh * term1;

            return wjnsemjms;
        }
    }

    /** The G<sub>n,s</sub> coefficients and their derivatives.
     * <p>
     * See Danielson, 4.2-17
     *
     * @author Lucian Barbulescu
     */
    private class GnsCoefficients {

        /** Maximum value for n index. */
        private final int nMax;

        /** Maximum value for s index. */
        private final int sMax;

        /** The coefficients G<sub>n,s</sub>. */
        private final double gns[][];

        /** The derivatives of the coefficients G<sub>n,s</sub> by a. */
        private final double dgnsda[][];

        /** The derivatives of the coefficients G<sub>n,s</sub> by γ. */
        private final double dgnsdgamma[][];

        /** Standard constructor.
         *
         * @param nMax maximum value for n indes
         * @param sMax maximum value for s index
         */
        GnsCoefficients(final int nMax, final int sMax) {
            this.nMax = nMax;
            this.sMax = sMax;

            final int rows    = nMax + 1;
            final int columns = sMax + 1;
            this.gns          = new double[rows][columns];
            this.dgnsda       = new double[rows][columns];
            this.dgnsdgamma   = new double[rows][columns];

            // Generate the coefficients
            generateCoefficients();
        }
        /**
         * Compute the coefficient G<sub>n,s</sub> and its derivatives.
         * <p>
         * Only the derivatives by a and γ are computed as all others are 0
         * </p>
         */
        private void generateCoefficients() {
            for (int s = 0; s <= sMax; s++) {
                // The n index is always at least the maximum between 2 and s
                final int minN = FastMath.max(2, s);
                for (int n = minN; n <= nMax; n++) {
                    // compute the coefficients only if (n - s) % 2 == 0
                    if ( (n - s) % 2 == 0 ) {
                        // Kronecker symbol (2 - delta(0,s))
                        final double delta0s = (s == 0) ? 1. : 2.;
                        final double vns   = Vns.get(new NSKey(n, s));
                        final double coef0 = delta0s * aoR3Pow[n] * vns * muoR3;
                        final double coef1 = coef0 * Qns[n][s];
                        // dQns/dGamma = Q(n, s + 1) from Equation 3.1-(8)
                        // for n = s, Q(n, n + 1) = 0. (Cefola & Broucke, 1975)
                        final double dqns = (n == s) ? 0. : Qns[n][s + 1];

                        //Compute the coefficient and its derivatives.
                        this.gns[n][s] = coef1;
                        this.dgnsda[n][s] = coef1 * n / a;
                        this.dgnsdgamma[n][s] = coef0 * dqns;
                    } else {
                        // the coefficient and its derivatives is 0
                        this.gns[n][s] = 0.;
                        this.dgnsda[n][s] = 0.;
                        this.dgnsdgamma[n][s] = 0.;
                    }
                }
            }
        }

        /** Get the coefficient G<sub>n,s</sub>.
         *
         * @param n n index
         * @param s s index
         * @return the coefficient G<sub>n,s</sub>
         */
        public double getGns(final int n, final int s) {
            return this.gns[n][s];
        }

        /** Get the derivative dG<sub>n,s</sub> / da.
         *
         * @param n n index
         * @param s s index
         * @return the derivative dG<sub>n,s</sub> / da
         */
        public double getdGnsda(final int n, final int s) {
            return this.dgnsda[n][s];
        }

        /** Get the derivative dG<sub>n,s</sub> / dγ.
         *
         * @param n n index
         * @param s s index
         * @return the derivative dG<sub>n,s</sub> / dγ
         */
        public double getdGnsdgamma(final int n, final int s) {
            return this.dgnsdgamma[n][s];
        }
    }

    /** This class computes the terms containing the coefficients C<sub>j</sub> and S<sub>j</sub> of (α, β) or (k, h).
     *
     * <p>
     * The following terms and their derivatives by k, h, alpha and beta are considered: <br/ >
     * - sign(j-s) * C<sub>s</sub>(α, β) * S<sub>|j-s|</sub>(k, h) + S<sub>s</sub>(α, β) * C<sub>|j-s|</sub>(k, h) <br />
     * - C<sub>s</sub>(α, β) * S<sub>j+s</sub>(k, h) - S<sub>s</sub>(α, β) * C<sub>j+s</sub>(k, h) <br />
     * - C<sub>s</sub>(α, β) * C<sub>|j-s|</sub>(k, h) - sign(j-s) * S<sub>s</sub>(α, β) * S<sub>|j-s|</sub>(k, h) <br />
     * - C<sub>s</sub>(α, β) * C<sub>j+s</sub>(k, h) + S<sub>s</sub>(α, β) * S<sub>j+s</sub>(k, h) <br />
     * For the ease of usage the above terms are renamed A<sub>js</sub>, B<sub>js</sub>, D<sub>js</sub> and E<sub>js</sub> respectively <br />
     * See the CS Mathematical report $3.5.3.2 for more details
     * </p>
     * @author Lucian Barbulescu
     */
    private class CjSjAlphaBetaKH {

        /** The C<sub>j</sub>(k, h) and the S<sub>j</sub>(k, h) series. */
        private final CjSjCoefficient cjsjkh;

        /** The C<sub>j</sub>(α, β) and the S<sub>j</sub>(α, β) series. */
        private final CjSjCoefficient cjsjalbe;

        /** The coeficient sign(j-s) * C<sub>s</sub>(α, β) * S<sub>|j-s|</sub>(k, h) + S<sub>s</sub>(α, β) * C<sub>|j-s|</sub>(k, h)
         * and its derivative by k, h, α and β. */
        private final double coefAandDeriv[];

        /** The coeficient C<sub>s</sub>(α, β) * S<sub>j+s</sub>(k, h) - S<sub>s</sub>(α, β) * C<sub>j+s</sub>(k, h)
         * and its derivative by k, h, α and β. */
        private final double coefBandDeriv[];

        /** The coeficient C<sub>s</sub>(α, β) * C<sub>|j-s|</sub>(k, h) - sign(j-s) * S<sub>s</sub>(α, β) * S<sub>|j-s|</sub>(k, h)
         * and its derivative by k, h, α and β. */
        private final double coefDandDeriv[];

        /** The coeficient C<sub>s</sub>(α, β) * C<sub>j+s</sub>(k, h) + S<sub>s</sub>(α, β) * S<sub>j+s</sub>(k, h)
         * and its derivative by k, h, α and β. */
        private final double coefEandDeriv[];

        /**
         * Standard constructor.
         */
        CjSjAlphaBetaKH() {
            cjsjkh = new CjSjCoefficient(k, h);
            cjsjalbe = new CjSjCoefficient(alpha, beta);

            coefAandDeriv = new double[5];
            coefBandDeriv = new double[5];
            coefDandDeriv = new double[5];
            coefEandDeriv = new double[5];
        }

        /** Compute the coefficients and their derivatives for a given (j,s) pair.
         * @param j j index
         * @param s s index
         */
        public void computeCoefficients(final int j, final int s) {
            // sign of j-s
            final int sign = j < s ? -1 : 1;

            //|j-s|
            final int absJmS = FastMath.abs(j - s);

            //j+s
            final int jps = j + s;

            //Compute the coefficient A and its derivatives
            coefAandDeriv[0] = sign * cjsjalbe.getCj(s) * cjsjkh.getSj(absJmS) + cjsjalbe.getSj(s) * cjsjkh.getCj(absJmS);
            coefAandDeriv[1] = sign * cjsjalbe.getCj(s) * cjsjkh.getDsjDk(absJmS) + cjsjalbe.getSj(s) * cjsjkh.getDcjDk(absJmS);
            coefAandDeriv[2] = sign * cjsjalbe.getCj(s) * cjsjkh.getDsjDh(absJmS) + cjsjalbe.getSj(s) * cjsjkh.getDcjDh(absJmS);
            coefAandDeriv[3] = sign * cjsjalbe.getDcjDk(s) * cjsjkh.getSj(absJmS) + cjsjalbe.getDsjDk(s) * cjsjkh.getCj(absJmS);
            coefAandDeriv[4] = sign * cjsjalbe.getDcjDh(s) * cjsjkh.getSj(absJmS) + cjsjalbe.getDsjDh(s) * cjsjkh.getCj(absJmS);

            //Compute the coefficient B and its derivatives
            coefBandDeriv[0] = cjsjalbe.getCj(s) * cjsjkh.getSj(jps) - cjsjalbe.getSj(s) * cjsjkh.getCj(jps);
            coefBandDeriv[1] = cjsjalbe.getCj(s) * cjsjkh.getDsjDk(jps) - cjsjalbe.getSj(s) * cjsjkh.getDcjDk(jps);
            coefBandDeriv[2] = cjsjalbe.getCj(s) * cjsjkh.getDsjDh(jps) - cjsjalbe.getSj(s) * cjsjkh.getDcjDh(jps);
            coefBandDeriv[3] = cjsjalbe.getDcjDk(s) * cjsjkh.getSj(jps) - cjsjalbe.getDsjDk(s) * cjsjkh.getCj(jps);
            coefBandDeriv[4] = cjsjalbe.getDcjDh(s) * cjsjkh.getSj(jps) - cjsjalbe.getDsjDh(s) * cjsjkh.getCj(jps);

            //Compute the coefficient D and its derivatives
            coefDandDeriv[0] = cjsjalbe.getCj(s) * cjsjkh.getCj(absJmS) - sign * cjsjalbe.getSj(s) * cjsjkh.getSj(absJmS);
            coefDandDeriv[1] = cjsjalbe.getCj(s) * cjsjkh.getDcjDk(absJmS) - sign * cjsjalbe.getSj(s) * cjsjkh.getDsjDk(absJmS);
            coefDandDeriv[2] = cjsjalbe.getCj(s) * cjsjkh.getDcjDh(absJmS) - sign * cjsjalbe.getSj(s) * cjsjkh.getDsjDh(absJmS);
            coefDandDeriv[3] = cjsjalbe.getDcjDk(s) * cjsjkh.getCj(absJmS) - sign * cjsjalbe.getDsjDk(s) * cjsjkh.getSj(absJmS);
            coefDandDeriv[4] = cjsjalbe.getDcjDh(s) * cjsjkh.getCj(absJmS) - sign * cjsjalbe.getDsjDh(s) * cjsjkh.getSj(absJmS);

            //Compute the coefficient E and its derivatives
            coefEandDeriv[0] = cjsjalbe.getCj(s) * cjsjkh.getCj(jps) + cjsjalbe.getSj(s) * cjsjkh.getSj(jps);
            coefEandDeriv[1] = cjsjalbe.getCj(s) * cjsjkh.getDcjDk(jps) + cjsjalbe.getSj(s) * cjsjkh.getDsjDk(jps);
            coefEandDeriv[2] = cjsjalbe.getCj(s) * cjsjkh.getDcjDh(jps) + cjsjalbe.getSj(s) * cjsjkh.getDsjDh(jps);
            coefEandDeriv[3] = cjsjalbe.getDcjDk(s) * cjsjkh.getCj(jps) + cjsjalbe.getDsjDk(s) * cjsjkh.getSj(jps);
            coefEandDeriv[4] = cjsjalbe.getDcjDh(s) * cjsjkh.getCj(jps) + cjsjalbe.getDsjDh(s) * cjsjkh.getSj(jps);
        }

        /** Get the value of coefficient A<sub>j,s</sub>.
         *
         * @return the coefficient A<sub>j,s</sub>
         */
        public double getCoefA() {
            return coefAandDeriv[0];
        }

        /** Get the value of coefficient dA<sub>j,s</sub>/dk.
         *
         * @return the coefficient dA<sub>j,s</sub>/dk
         */
        public double getdCoefAdk() {
            return coefAandDeriv[1];
        }

        /** Get the value of coefficient dA<sub>j,s</sub>/dh.
         *
         * @return the coefficient dA<sub>j,s</sub>/dh
         */
        public double getdCoefAdh() {
            return coefAandDeriv[2];
        }

        /** Get the value of coefficient dA<sub>j,s</sub>/dα.
         *
         * @return the coefficient dA<sub>j,s</sub>/dα
         */
        public double getdCoefAdalpha() {
            return coefAandDeriv[3];
        }

        /** Get the value of coefficient dA<sub>j,s</sub>/dβ.
         *
         * @return the coefficient dA<sub>j,s</sub>/dβ
         */
        public double getdCoefAdbeta() {
            return coefAandDeriv[4];
        }

        /** Get the value of coefficient B<sub>j,s</sub>.
         *
         * @return the coefficient B<sub>j,s</sub>
         */
        public double getCoefB() {
            return coefBandDeriv[0];
        }

        /** Get the value of coefficient dB<sub>j,s</sub>/dk.
         *
         * @return the coefficient dB<sub>j,s</sub>/dk
         */
        public double getdCoefBdk() {
            return coefBandDeriv[1];
        }

        /** Get the value of coefficient dB<sub>j,s</sub>/dh.
         *
         * @return the coefficient dB<sub>j,s</sub>/dh
         */
        public double getdCoefBdh() {
            return coefBandDeriv[2];
        }

        /** Get the value of coefficient dB<sub>j,s</sub>/dα.
         *
         * @return the coefficient dB<sub>j,s</sub>/dα
         */
        public double getdCoefBdalpha() {
            return coefBandDeriv[3];
        }

        /** Get the value of coefficient dB<sub>j,s</sub>/dβ.
         *
         * @return the coefficient dB<sub>j,s</sub>/dβ
         */
        public double getdCoefBdbeta() {
            return coefBandDeriv[4];
        }

        /** Get the value of coefficient D<sub>j,s</sub>.
         *
         * @return the coefficient D<sub>j,s</sub>
         */
        public double getCoefD() {
            return coefDandDeriv[0];
        }

        /** Get the value of coefficient dD<sub>j,s</sub>/dk.
         *
         * @return the coefficient dD<sub>j,s</sub>/dk
         */
        public double getdCoefDdk() {
            return coefDandDeriv[1];
        }

        /** Get the value of coefficient dD<sub>j,s</sub>/dh.
         *
         * @return the coefficient dD<sub>j,s</sub>/dh
         */
        public double getdCoefDdh() {
            return coefDandDeriv[2];
        }

        /** Get the value of coefficient dD<sub>j,s</sub>/dα.
         *
         * @return the coefficient dD<sub>j,s</sub>/dα
         */
        public double getdCoefDdalpha() {
            return coefDandDeriv[3];
        }

        /** Get the value of coefficient dD<sub>j,s</sub>/dβ.
         *
         * @return the coefficient dD<sub>j,s</sub>/dβ
         */
        public double getdCoefDdbeta() {
            return coefDandDeriv[4];
        }

        /** Get the value of coefficient E<sub>j,s</sub>.
         *
         * @return the coefficient E<sub>j,s</sub>
         */
        public double getCoefE() {
            return coefEandDeriv[0];
        }

        /** Get the value of coefficient dE<sub>j,s</sub>/dk.
         *
         * @return the coefficient dE<sub>j,s</sub>/dk
         */
        public double getdCoefEdk() {
            return coefEandDeriv[1];
        }

        /** Get the value of coefficient dE<sub>j,s</sub>/dh.
         *
         * @return the coefficient dE<sub>j,s</sub>/dh
         */
        public double getdCoefEdh() {
            return coefEandDeriv[2];
        }

        /** Get the value of coefficient dE<sub>j,s</sub>/dα.
         *
         * @return the coefficient dE<sub>j,s</sub>/dα
         */
        public double getdCoefEdalpha() {
            return coefEandDeriv[3];
        }

        /** Get the value of coefficient dE<sub>j,s</sub>/dβ.
         *
         * @return the coefficient dE<sub>j,s</sub>/dβ
         */
        public double getdCoefEdbeta() {
            return coefEandDeriv[4];
        }
    }

    /** This class computes the coefficients for the generating function S and its derivatives.
     * <p>
     * The form of the generating functions is: <br>
     *  S = C⁰ + &Sigma;<sub>j=1</sub><sup>N+1</sup>(C<sup>j</sup> * cos(jF) + S<sup>j</sup> * sin(jF)) <br>
     *  The coefficients C⁰, C<sup>j</sup>, S<sup>j</sup> are the Fourrier coefficients
     *  presented in Danielson 4.2-14,15 except for the case j=1 where
     *  C¹ = C¹<sub>Fourier</sub> - hU and
     *  S¹ = S¹<sub>Fourier</sub> + kU <br>
     *  Also the coefficients of the derivatives of S by a, k, h, α, β, γ and λ
     *  are computed end expressed in a similar manner. The formulas used are 4.2-19, 20, 23, 24
     * </p>
     * @author Lucian Barbulescu
     */
    private class GeneratingFunctionCoefficients {

        /** The Fourier coefficients as presented in Danielson 4.2-14,15. */
        private final FourierCjSjCoefficients cjsjFourier;

        /** Maximum value of j index. */
        private final int jMax;

        /** The coefficients C<sup>j</sup> of the function S and its derivatives.
         * <p>
         * The index j belongs to the interval [0,jMax]. The coefficient C⁰ is the free coefficient.<br>
         * Each column of the matrix contains the coefficient corresponding to the following functions: <br/>
         * - S <br/>
         * - dS / da <br/>
         * - dS / dk <br/>
         * - dS / dh <br/>
         * - dS / dα <br/>
         * - dS / dβ <br/>
         * - dS / dγ <br/>
         * - dS / dλ
         * </p>
         */
        private final double[][] cjCoefs;

        /** The coefficients S<sup>j</sup> of the function S and its derivatives.
         * <p>
         * The index j belongs to the interval [0,jMax].<br>
         * Each column of the matrix contains the coefficient corresponding to the following functions: <br/>
         * - S <br/>
         * - dS / da <br/>
         * - dS / dk <br/>
         * - dS / dh <br/>
         * - dS / dα <br/>
         * - dS / dβ <br/>
         * - dS / dγ <br/>
         * - dS / dλ
         * </p>
         */
        private final double[][] sjCoefs;

        /**
         * Standard constructor.
         *
         * @param nMax maximum value of n index
         * @param sMax maximum value of s index
         * @param jMax maximum value of j index
         */
        GeneratingFunctionCoefficients(final int nMax, final int sMax, final int jMax) {
            this.jMax = jMax;
            this.cjsjFourier = new FourierCjSjCoefficients(nMax, sMax, jMax);
            this.cjCoefs = new double[8][jMax + 1];
            this.sjCoefs = new double[8][jMax + 1];

            computeGeneratingFunctionCoefficients();
        }

        /**
         * Compute the coefficients for the generating function S and its derivatives.
         */
        private void computeGeneratingFunctionCoefficients() {

            // Compute potential U and derivatives
            final double[] dU  = computeUDerivatives();

            //Compute the C<sup>j</sup> coefficients
            for (int j = 1; j <= jMax; j++) {
                //Compute the C<sup>j</sup> coefficients
                cjCoefs[0][j] = cjsjFourier.getCj(j);
                cjCoefs[1][j] = cjsjFourier.getdCjda(j);
                cjCoefs[2][j] = cjsjFourier.getdCjdk(j) - (cjsjFourier.getSjLambda(j - 1) - cjsjFourier.getSjLambda(j + 1)) / 2;
                cjCoefs[3][j] = cjsjFourier.getdCjdh(j) - (cjsjFourier.getCjLambda(j - 1) + cjsjFourier.getCjLambda(j + 1)) / 2;
                cjCoefs[4][j] = cjsjFourier.getdCjdalpha(j);
                cjCoefs[5][j] = cjsjFourier.getdCjdbeta(j);
                cjCoefs[6][j] = cjsjFourier.getdCjdgamma(j);
                cjCoefs[7][j] = cjsjFourier.getCjLambda(j);

                //Compute the S<sup>j</sup> coefficients
                sjCoefs[0][j] = cjsjFourier.getSj(j);
                sjCoefs[1][j] = cjsjFourier.getdSjda(j);
                sjCoefs[2][j] = cjsjFourier.getdSjdk(j) + (cjsjFourier.getCjLambda(j - 1) - cjsjFourier.getCjLambda(j + 1)) / 2;
                sjCoefs[3][j] = cjsjFourier.getdSjdh(j) - (cjsjFourier.getSjLambda(j - 1) + cjsjFourier.getSjLambda(j + 1)) / 2;
                sjCoefs[4][j] = cjsjFourier.getdSjdalpha(j);
                sjCoefs[5][j] = cjsjFourier.getdSjdbeta(j);
                sjCoefs[6][j] = cjsjFourier.getdSjdgamma(j);
                sjCoefs[7][j] = cjsjFourier.getSjLambda(j);

                //In the special case j == 1 there are some additional terms to be added
                if (j == 1) {
                    //Additional terms for C<sup>j</sup> coefficients
                    cjCoefs[0][j] += -h * U;
                    cjCoefs[1][j] += -h * dU[0];
                    cjCoefs[2][j] += -h * dU[1];
                    cjCoefs[3][j] += -(h * dU[2] + U + cjsjFourier.getC0Lambda());
                    cjCoefs[4][j] += -h * dU[3];
                    cjCoefs[5][j] += -h * dU[4];
                    cjCoefs[6][j] += -h * dU[5];

                    //Additional terms for S<sup>j</sup> coefficients
                    sjCoefs[0][j] += k * U;
                    sjCoefs[1][j] += k * dU[0];
                    sjCoefs[2][j] += k * dU[1] + U + cjsjFourier.getC0Lambda();
                    sjCoefs[3][j] += k * dU[2];
                    sjCoefs[4][j] += k * dU[3];
                    sjCoefs[5][j] += k * dU[4];
                    sjCoefs[6][j] += k * dU[5];
                }
            }
        }

        /** Get the coefficient C<sup>j</sup> for the function S.
         * <br>
         * Possible values for j are within the interval [0,jMax].
         * The value 0 is used to obtain the free coefficient C⁰
         * @param j j index
         * @return C<sup>j</sup> for the function S
         */
        public double getSCj(final int j) {
            return cjCoefs[0][j];
        }

        /** Get the coefficient S<sup>j</sup> for the function S.
         * <br>
         * Possible values for j are within the interval [1,jMax].
         * @param j j index
         * @return S<sup>j</sup> for the function S
         */
        public double getSSj(final int j) {
            return sjCoefs[0][j];
        }

        /** Get the coefficient C<sup>j</sup> for the derivative dS/da.
         * <br>
         * Possible values for j are within the interval [0,jMax].
         * The value 0 is used to obtain the free coefficient C⁰
         * @param j j index
         * @return C<sup>j</sup> for the function dS/da
         */
        public double getdSdaCj(final int j) {
            return cjCoefs[1][j];
        }

        /** Get the coefficient S<sup>j</sup> for the derivative dS/da.
         * <br>
         * Possible values for j are within the interval [1,jMax].
         * @param j j index
         * @return S<sup>j</sup> for the derivative dS/da
         */
        public double getdSdaSj(final int j) {
            return sjCoefs[1][j];
        }

        /** Get the coefficient C<sup>j</sup> for the derivative dS/dk
         * <br>
         * Possible values for j are within the interval [0,jMax].
         * The value 0 is used to obtain the free coefficient C⁰
         * @param j j index
         * @return C<sup>j</sup> for the function dS/dk
         */
        public double getdSdkCj(final int j) {
            return cjCoefs[2][j];
        }

        /** Get the coefficient S<sup>j</sup> for the derivative dS/dk.
         * <br>
         * Possible values for j are within the interval [1,jMax].
         * @param j j index
         * @return S<sup>j</sup> for the derivative dS/dk
         */
        public double getdSdkSj(final int j) {
            return sjCoefs[2][j];
        }

        /** Get the coefficient C<sup>j</sup> for the derivative dS/dh
         * <br>
         * Possible values for j are within the interval [0,jMax].
         * The value 0 is used to obtain the free coefficient C⁰
         * @param j j index
         * @return C<sup>j</sup> for the function dS/dh
         */
        public double getdSdhCj(final int j) {
            return cjCoefs[3][j];
        }

        /** Get the coefficient S<sup>j</sup> for the derivative dS/dh.
         * <br>
         * Possible values for j are within the interval [1,jMax].
         * @param j j index
         * @return S<sup>j</sup> for the derivative dS/dh
         */
        public double getdSdhSj(final int j) {
            return sjCoefs[3][j];
        }

        /** Get the coefficient C<sup>j</sup> for the derivative dS/dα
         * <br>
         * Possible values for j are within the interval [0,jMax].
         * The value 0 is used to obtain the free coefficient C⁰
         * @param j j index
         * @return C<sup>j</sup> for the function dS/dα
         */
        public double getdSdalphaCj(final int j) {
            return cjCoefs[4][j];
        }

        /** Get the coefficient S<sup>j</sup> for the derivative dS/dα.
         * <br>
         * Possible values for j are within the interval [1,jMax].
         * @param j j index
         * @return S<sup>j</sup> for the derivative dS/dα
         */
        public double getdSdalphaSj(final int j) {
            return sjCoefs[4][j];
        }

        /** Get the coefficient C<sup>j</sup> for the derivative dS/dβ
         * <br>
         * Possible values for j are within the interval [0,jMax].
         * The value 0 is used to obtain the free coefficient C⁰
         * @param j j index
         * @return C<sup>j</sup> for the function dS/dβ
         */
        public double getdSdbetaCj(final int j) {
            return cjCoefs[5][j];
        }

        /** Get the coefficient S<sup>j</sup> for the derivative dS/dβ.
         * <br>
         * Possible values for j are within the interval [1,jMax].
         * @param j j index
         * @return S<sup>j</sup> for the derivative dS/dβ
         */
        public double getdSdbetaSj(final int j) {
            return sjCoefs[5][j];
        }

        /** Get the coefficient C<sup>j</sup> for the derivative dS/dγ
         * <br>
         * Possible values for j are within the interval [0,jMax].
         * The value 0 is used to obtain the free coefficient C⁰
         * @param j j index
         * @return C<sup>j</sup> for the function dS/dγ
         */
        public double getdSdgammaCj(final int j) {
            return cjCoefs[6][j];
        }

        /** Get the coefficient S<sup>j</sup> for the derivative dS/dγ.
         * <br>
         * Possible values for j are within the interval [1,jMax].
         * @param j j index
         * @return S<sup>j</sup> for the derivative dS/dγ
         */
        public double getdSdgammaSj(final int j) {
            return sjCoefs[6][j];
        }

        /** Get the coefficient C<sup>j</sup> for the derivative dS/dλ
         * <br>
         * Possible values for j are within the interval [0,jMax].
         * The value 0 is used to obtain the free coefficient C⁰
         * @param j j index
         * @return C<sup>j</sup> for the function dS/dλ
         */
        public double getdSdlambdaCj(final int j) {
            return cjCoefs[7][j];
        }

        /** Get the coefficient S<sup>j</sup> for the derivative dS/dλ.
         * <br>
         * Possible values for j are within the interval [1,jMax].
         * @param j j index
         * @return S<sup>j</sup> for the derivative dS/dλ
         */
        public double getdSdlambdaSj(final int j) {
            return sjCoefs[7][j];
        }
    }

    /**
     * The coefficients used to compute the short periodic contribution for the Third body perturbation.
     * <p>
     * The short periodic contribution for the Third Body is expressed in Danielson 4.2-25.<br>
     * The coefficients C<sub>i</sub>⁰, C<sub>i</sub><sup>j</sup>, S<sub>i</sub><sup>j</sup>
     * are computed by replacing the corresponding values in formula 2.5.5-10.
     * </p>
     * @author Lucian Barbulescu
     */
    private static class ThirdBodyShortPeriodicCoefficients implements ShortPeriodTerms {

        /** Serializable UID. */
        private static final long serialVersionUID = 20151119L;

        /** Maximal value for j. */
        private final int jMax;

        /** Number of points used in the interpolation process. */
        private final int interpolationPoints;

        /** Max frequency of F. */
        private final int    maxFreqF;

        /** Coefficients prefix. */
        private final String prefix;

        /** All coefficients slots. */
        private final transient TimeSpanMap<Slot> slots;

        /**
         * Standard constructor.
         *  @param interpolationPoints number of points used in the interpolation process
         * @param jMax maximal value for j
         * @param maxFreqF Max frequency of F
         * @param bodyName third body name
         * @param slots all coefficients slots
         */
        ThirdBodyShortPeriodicCoefficients(final int jMax, final int interpolationPoints,
                                           final int maxFreqF, final String bodyName,
                                           final TimeSpanMap<Slot> slots) {
            this.jMax                = jMax;
            this.interpolationPoints = interpolationPoints;
            this.maxFreqF            = maxFreqF;
            this.prefix              = "DSST-3rd-body-" + bodyName + "-";
            this.slots               = slots;
        }

        /** Get the slot valid for some date.
         * @param meanStates mean states defining the slot
         * @return slot valid at the specified date
         */
        public Slot createSlot(final SpacecraftState ... meanStates) {
            final Slot         slot  = new Slot(jMax, interpolationPoints);
            final AbsoluteDate first = meanStates[0].getDate();
            final AbsoluteDate last  = meanStates[meanStates.length - 1].getDate();
            if (first.compareTo(last) <= 0) {
                slots.addValidAfter(slot, first);
            } else {
                slots.addValidBefore(slot, first);
            }
            return slot;
        }

        /** {@inheritDoc} */
        @Override
        public double[] value(final Orbit meanOrbit) {

            // select the coefficients slot
            final Slot slot = slots.get(meanOrbit.getDate());

            // the current eccentric longitude
            final double F = meanOrbit.getLE();

            //initialize the short periodic contribution with the corresponding C⁰ coeficient
            final double[] shortPeriodic = slot.cij[0].value(meanOrbit.getDate());

            // Add the cos and sin dependent terms
            for (int j = 1; j <= maxFreqF; j++) {
                //compute cos and sin
                final double cosjF = FastMath.cos(j * F);
                final double sinjF = FastMath.sin(j * F);

                final double[] c = slot.cij[j].value(meanOrbit.getDate());
                final double[] s = slot.sij[j].value(meanOrbit.getDate());
                for (int i = 0; i < 6; i++) {
                    shortPeriodic[i] += c[i] * cosjF + s[i] * sinjF;
                }
            }

            return shortPeriodic;

        }

        /** {@inheritDoc} */
        @Override
        public String getCoefficientsKeyPrefix() {
            return prefix;
        }

        /** {@inheritDoc}
         * <p>
         * For third body attraction forces,there are maxFreqF + 1 cj coefficients,
         * maxFreqF sj coefficients where maxFreqF depends on the orbit.
         * The j index is the integer multiplier for the eccentric longitude argument
         * in the cj and sj coefficients.
         * </p>
         */
        @Override
        public Map<String, double[]> getCoefficients(final AbsoluteDate date, final Set<String> selected)
            throws OrekitException {

            // select the coefficients slot
            final Slot slot = slots.get(date);

            final Map<String, double[]> coefficients = new HashMap<String, double[]>(2 * maxFreqF + 1);
            storeIfSelected(coefficients, selected, slot.cij[0].value(date), "c", 0);
            for (int j = 1; j <= maxFreqF; j++) {
                storeIfSelected(coefficients, selected, slot.cij[j].value(date), "c", j);
                storeIfSelected(coefficients, selected, slot.sij[j].value(date), "s", j);
            }
            return coefficients;

        }

        /** Put a coefficient in a map if selected.
         * @param map map to populate
         * @param selected set of coefficients that should be put in the map
         * (empty set means all coefficients are selected)
         * @param value coefficient value
         * @param id coefficient identifier
         * @param indices list of coefficient indices
         */
        private void storeIfSelected(final Map<String, double[]> map, final Set<String> selected,
                                     final double[] value, final String id, final int ... indices) {
            final StringBuilder keyBuilder = new StringBuilder(getCoefficientsKeyPrefix());
            keyBuilder.append(id);
            for (int index : indices) {
                keyBuilder.append('[').append(index).append(']');
            }
            final String key = keyBuilder.toString();
            if (selected.isEmpty() || selected.contains(key)) {
                map.put(key, value);
            }
        }

        /** Replace the instance with a data transfer object for serialization.
         * @return data transfer object that will be serialized
         * @exception NotSerializableException if an additional state provider is not serializable
         */
        private Object writeReplace() throws NotSerializableException {

            // slots transitions
            final SortedSet<TimeSpanMap.Transition<Slot>> transitions     = slots.getTransitions();
            final AbsoluteDate[]                          transitionDates = new AbsoluteDate[transitions.size()];
            final Slot[]                                  allSlots        = new Slot[transitions.size() + 1];
            int i = 0;
            for (final TimeSpanMap.Transition<Slot> transition : transitions) {
                if (i == 0) {
                    // slot before the first transition
                    allSlots[i] = transition.getBefore();
                }
                if (i < transitionDates.length) {
                    transitionDates[i] = transition.getDate();
                    allSlots[++i]      = transition.getAfter();
                }
            }

            return new DataTransferObject(jMax, interpolationPoints, maxFreqF, prefix,
                                          transitionDates, allSlots);

        }


        /** Internal class used only for serialization. */
        private static class DataTransferObject implements Serializable {

            /** Serializable UID. */
            private static final long serialVersionUID = 20160319L;

            /** Maximum value for j index. */
            private final int jMax;

            /** Number of points used in the interpolation process. */
            private final int interpolationPoints;

            /** Max frequency of F. */
            private final int    maxFreqF;

            /** Coefficients prefix. */
            private final String prefix;

            /** Transitions dates. */
            private final AbsoluteDate[] transitionDates;

            /** All slots. */
            private final Slot[] allSlots;

            /** Simple constructor.
             * @param jMax maximum value for j index
             * @param interpolationPoints number of points used in the interpolation process
             * @param maxFreqF max frequency of F
             * @param prefix prefix for coefficients keys
             * @param transitionDates transitions dates
             * @param allSlots all slots
             */
            DataTransferObject(final int jMax, final int interpolationPoints,
                               final int maxFreqF, final String prefix,
                               final AbsoluteDate[] transitionDates, final Slot[] allSlots) {
                this.jMax                  = jMax;
                this.interpolationPoints   = interpolationPoints;
                this.maxFreqF              = maxFreqF;
                this.prefix                = prefix;
                this.transitionDates       = transitionDates;
                this.allSlots              = allSlots;
            }

            /** Replace the deserialized data transfer object with a {@link ThirdBodyShortPeriodicCoefficients}.
             * @return replacement {@link ThirdBodyShortPeriodicCoefficients}
             */
            private Object readResolve() {

                final TimeSpanMap<Slot> slots = new TimeSpanMap<Slot>(allSlots[0]);
                for (int i = 0; i < transitionDates.length; ++i) {
                    slots.addValidAfter(allSlots[i + 1], transitionDates[i]);
                }

                return new ThirdBodyShortPeriodicCoefficients(jMax, interpolationPoints, maxFreqF, prefix, slots);

            }

        }

    }

    /** Coefficients valid for one time slot. */
    private static class Slot implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 20160319L;

        /** The coefficients C<sub>i</sub><sup>j</sup>.
         * <p>
         * The index order is cij[j][i] <br/>
         * i corresponds to the equinoctial element, as follows: <br/>
         * - i=0 for a <br/>
         * - i=1 for k <br/>
         * - i=2 for h <br/>
         * - i=3 for q <br/>
         * - i=4 for p <br/>
         * - i=5 for λ <br/>
         * </p>
         */
        private final ShortPeriodicsInterpolatedCoefficient[] cij;

        /** The coefficients S<sub>i</sub><sup>j</sup>.
         * <p>
         * The index order is sij[j][i] <br/>
         * i corresponds to the equinoctial element, as follows: <br/>
         * - i=0 for a <br/>
         * - i=1 for k <br/>
         * - i=2 for h <br/>
         * - i=3 for q <br/>
         * - i=4 for p <br/>
         * - i=5 for λ <br/>
         * </p>
         */
        private final ShortPeriodicsInterpolatedCoefficient[] sij;

        /** Simple constructor.
         *  @param jMax maximum value for j index
         *  @param interpolationPoints number of points used in the interpolation process
         */
        Slot(final int jMax, final int interpolationPoints) {
            // allocate the coefficients arrays
            cij = new ShortPeriodicsInterpolatedCoefficient[jMax + 1];
            sij = new ShortPeriodicsInterpolatedCoefficient[jMax + 1];
            for (int j = 0; j <= jMax; j++) {
                cij[j] = new ShortPeriodicsInterpolatedCoefficient(interpolationPoints);
                sij[j] = new ShortPeriodicsInterpolatedCoefficient(interpolationPoints);
            }


        }
    }

}
