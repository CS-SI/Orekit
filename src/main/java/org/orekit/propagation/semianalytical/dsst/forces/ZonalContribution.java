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
package org.orekit.propagation.semianalytical.dsst.forces;

import java.util.TreeMap;

import org.apache.commons.math3.util.FastMath;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider.UnnormalizedSphericalHarmonics;
import org.orekit.frames.Frame;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.propagation.semianalytical.dsst.utilities.CjSjCoefficient;
import org.orekit.propagation.semianalytical.dsst.utilities.CoefficientsFactory;
import org.orekit.propagation.semianalytical.dsst.utilities.CoefficientsFactory.NSKey;
import org.orekit.propagation.semianalytical.dsst.utilities.GHIJjsPolynomials;
import org.orekit.propagation.semianalytical.dsst.utilities.LnsCoefficients;
import org.orekit.propagation.semianalytical.dsst.utilities.ShortPeriodicsInterpolatedCoefficient;
import org.orekit.propagation.semianalytical.dsst.utilities.UpperBounds;
import org.orekit.propagation.semianalytical.dsst.utilities.hansen.HansenZonalLinear;
import org.orekit.time.AbsoluteDate;

/** Zonal contribution to the {@link DSSTCentralBody central body gravitational perturbation}.
 *
 *   @author Romain Di Costanzo
 *   @author Pascal Parraud
 */
class ZonalContribution implements DSSTForceModel {

    /** Truncation tolerance. */
    private static final double TRUNCATION_TOLERANCE = 1e-4;

    /** Number of points for interpolation. */
    private static final int INTERPOLATION_POINTS = 3;

    /** Highest possible degree taken into account in short periodics computation. */
    private static final int SHORT_PERIODICS_MAX_DEGREE = 12;

    /** Provider for spherical harmonics. */
    private final UnnormalizedSphericalHarmonicsProvider provider;

    /** Maximal degree to consider for harmonics potential. */
    private final int maxDegree;

    /** Maximal degree to consider for harmonics potential. */
    private int maxDegreeShortPeriodics;

    /** Maximal degree to consider for harmonics potential in short periodic computations. */
    private final int maxOrder;

    /** Factorial. */
    private final double[] fact;

    /** Coefficient used to define the mean disturbing function V<sub>ns</sub> coefficient. */
    private final TreeMap<NSKey, Double> Vns;

    /** Highest power of the eccentricity to be used in series expansion. */
    private int maxEccPow;

    /** Highest power of the eccentricity to be used in mean elements computations. */
    private int maxEccPowMeanElements;

    /** Highest power of the eccentricity to be used in short periodic computations. */
    private int maxEccPowShortPeriodics;

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
    private int I;

    /** Eccentricity. */
    private double ecc;

    /** Direction cosine &alpha. */
    private double alpha;
    /** Direction cosine &beta. */
    private double beta;
    /** Direction cosine &gamma. */
    private double gamma;

    // Common factors for potential computation
    /** &Chi; = 1 / sqrt(1 - e²) = 1 / B. */
    private double X;
    /** &Chi;². */
    private double XX;
    /** &Chi;³. */
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
    /** μ / a .*/
    private double muoa;
    /** R / a .*/
    private double roa;

    /** An array that contains the objects needed to build the Hansen coefficients. <br/>
     * The index is s*/
    private HansenZonalLinear[] hansenObjects;

    /** The current value of the U function. <br/>
     * Needed when computed the short periodic contribution */
    private double U;

    /** A = sqrt( μ * a ) = n * a². */
    private double A;
    /** B = sqrt( 1 - k² - h² ). */
    private double B;
    /** C = 1 + p² + Q². */
    private double C;

    /** The mean motion (n). */
    private double meanMotion;

    /** The frame used to describe the orbits. */
    private Frame frame;

    /** The C<sub>i</sub><sup>j</sup> and S<sub>i</sub><sup>j</sup> coefficients used to compute
     * the short-periodic zonal contribution. */
    private ZonalShortPeriodicCoefficients zonalSPCoefs;

    /** Simple constructor.
     * @param provider provider for spherical harmonics
     */
    public ZonalContribution(final UnnormalizedSphericalHarmonicsProvider provider) {

        this.provider  = provider;
        this.maxDegree = provider.getMaxDegree();
        this.maxOrder  = provider.getMaxOrder();

        // Vns coefficients
        this.Vns = CoefficientsFactory.computeVns(maxDegree + 1);

        // Factorials computation
        final int maxFact = 2 * maxDegree + 1;
        this.fact = new double[maxFact];
        fact[0] = 1.;
        for (int i = 1; i < maxFact; i++) {
            fact[i] = i * fact[i - 1];
        }

        // Initialize default values
        this.maxEccPow = (maxDegree == 2) ? 0 : Integer.MIN_VALUE;
        this.maxEccPowMeanElements = maxEccPow;
        this.maxEccPowShortPeriodics = maxDegree - 1;

    }

    /** Get the spherical harmonics provider.
     *  @return the spherical harmonics provider
     */
    public UnnormalizedSphericalHarmonicsProvider getProvider() {
        return provider;
    }

    /** Computes the highest power of the eccentricity to appear in the truncated
     *  analytical power series expansion.
     *  <p>
     *  This method computes the upper value for the central body potential and
     *  determines the maximal power for the eccentricity producing potential
     *  terms bigger than a defined tolerance.
     *  </p>
     *  @param aux auxiliary elements related to the current orbit
     *  @param meanOnly only mean elements will be used during the propagation
     *  @throws OrekitException if some specific error occurs
     */
    @Override
    public void initialize(final AuxiliaryElements aux, final boolean meanOnly)
        throws OrekitException {

        computeMeanElementsTruncations(aux);

        if (!meanOnly) {
            computeShortPeriodicsTruncations();
            maxEccPow = FastMath.max(maxEccPowMeanElements, maxEccPowShortPeriodics);
        }
        else {
            maxEccPow = maxEccPowMeanElements;
        }

        //Initialize the HansenCoefficient generator
        this.hansenObjects = new HansenZonalLinear[maxEccPow + 1];

        for (int s = 0; s <= maxEccPow; s++) {
            this.hansenObjects[s] = new HansenZonalLinear(maxDegree, s);
        }

        if (!meanOnly) {
            //Initialize the Zonal Short Periodics coefficient class
            zonalSPCoefs = new ZonalShortPeriodicCoefficients(maxDegreeShortPeriodics,
                    maxEccPowShortPeriodics, INTERPOLATION_POINTS);
        }
    }

    /** Compute indices truncations for mean elements computations.
     * @param aux auxiliary elements
     * @throws OrekitException if an error occurs
     */
    private void computeMeanElementsTruncations(final AuxiliaryElements aux) throws OrekitException {
        // save the frame
        this.frame = aux.getFrame();

        //Compute the max eccentricity power for the mean element rate expansion
        if (maxDegree == 2) {
            maxEccPowMeanElements = 0;
        } else {
            // Initializes specific parameters.
            initializeStep(aux);
            final UnnormalizedSphericalHarmonics harmonics = provider.onDate(aux.getDate());

            // Utilities for truncation
            final double ax2or = 2. * a / provider.getAe();
            double xmuran = provider.getMu() / a;
            // Set a lower bound for eccentricity
            final double eo2  = FastMath.max(0.0025, ecc / 2.);
            final double x2o2 = XX / 2.;
            final double[] eccPwr = new double[maxDegree + 1];
            final double[] chiPwr = new double[maxDegree + 1];
            final double[] hafPwr = new double[maxDegree + 1];
            eccPwr[0] = 1.;
            chiPwr[0] = X;
            hafPwr[0] = 1.;
            for (int i = 1; i <= maxDegree; i++) {
                eccPwr[i] = eccPwr[i - 1] * eo2;
                chiPwr[i] = chiPwr[i - 1] * x2o2;
                hafPwr[i] = hafPwr[i - 1] * 0.5;
                xmuran  /= ax2or;
            }

            // Set highest power of e and degree of current spherical harmonic.
            maxEccPowMeanElements = 0;
            int n = maxDegree;
            // Loop over n
            do {
                // Set order of current spherical harmonic.
                int m = 0;
                // Loop over m
                do {
                    // Compute magnitude of current spherical harmonic coefficient.
                    final double cnm = harmonics.getUnnormalizedCnm(n, m);
                    final double snm = harmonics.getUnnormalizedSnm(n, m);
                    final double csnm = FastMath.hypot(cnm, snm);
                    if (csnm == 0.) break;
                    // Set magnitude of last spherical harmonic term.
                    double lastTerm = 0.;
                    // Set current power of e and related indices.
                    int nsld2 = (n - maxEccPowMeanElements - 1) / 2;
                    int l = n - 2 * nsld2;
                    // Loop over l
                    double term = 0.;
                    do {
                        // Compute magnitude of current spherical harmonic term.
                        if (m < l) {
                            term = csnm * xmuran *
                                    (fact[n - l] / (fact[n - m])) *
                                    (fact[n + l] / (fact[nsld2] * fact[nsld2 + l])) *
                                    eccPwr[l] * UpperBounds.getDnl(XX, chiPwr[l], n, l) *
                                    (UpperBounds.getRnml(gamma, n, l, m, 1, I) + UpperBounds.getRnml(gamma, n, l, m, -1, I));
                        } else {
                            term = csnm * xmuran *
                                    (fact[n + m] / (fact[nsld2] * fact[nsld2 + l])) *
                                    eccPwr[l] * hafPwr[m - l] * UpperBounds.getDnl(XX, chiPwr[l], n, l) *
                                    (UpperBounds.getRnml(gamma, n, m, l, 1, I) + UpperBounds.getRnml(gamma, n, m, l, -1, I));
                        }
                        // Is the current spherical harmonic term bigger than the truncation tolerance ?
                        if (term >= TRUNCATION_TOLERANCE) {
                            maxEccPowMeanElements = l;
                        } else {
                            // Is the current term smaller than the last term ?
                            if (term < lastTerm) {
                                break;
                            }
                        }
                        // Proceed to next power of e.
                        lastTerm = term;
                        l += 2;
                        nsld2--;
                    } while (l < n);
                    // Is the current spherical harmonic term bigger than the truncation tolerance ?
                    if (term >= TRUNCATION_TOLERANCE) {
                        maxEccPowMeanElements = FastMath.min(maxDegree - 2, maxEccPowMeanElements);
                        return;
                    }
                    // Proceed to next order.
                    m++;
                } while (m <= FastMath.min(n, maxOrder));
                // Procced to next degree.
                xmuran *= ax2or;
                n--;
            } while (n > maxEccPowMeanElements + 2);

            maxEccPowMeanElements = FastMath.min(maxDegree - 2, maxEccPowMeanElements);
        }
    }

    /** Compute indices truncations for short periodics computation.
     * <p>
     * Truncations are as follows:
     * </p><p>
     * maxDegree_SP = min(maxDegree, SHORT_PERIODICS_MAX_DEGREE)
     * </p><p>
     * maxEccPow_SP = min(maxDegree_SP - 1, 4)
     * </p>
     */
    private void computeShortPeriodicsTruncations() {
        maxDegreeShortPeriodics = FastMath.min(maxDegree, SHORT_PERIODICS_MAX_DEGREE);
        maxEccPowShortPeriodics = FastMath.min(maxDegreeShortPeriodics - 1, 4);
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

        // Retrograde factor
        I = aux.getRetrogradeFactor();

        // Eccentricity
        ecc = aux.getEcc();

        // Direction cosines
        alpha = aux.getAlpha();
        beta  = aux.getBeta();
        gamma = aux.getGamma();

        // Equinoctial coefficients
        A = aux.getA();
        B = aux.getB();
        C = aux.getC();

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
        // μ / a
        muoa = provider.getMu() / a;
        // R / a
        roa = provider.getAe() / a;

        // Mean motion
        meanMotion = aux.getMeanMotion();
    }

    /** {@inheritDoc} */
    @Override
    public double[] getMeanElementRate(final SpacecraftState spacecraftState) throws OrekitException {
        return computeMeanElementRates(spacecraftState.getDate());
    }

    /** {@inheritDoc} */
    @Override
    public double[] getShortPeriodicVariations(final AbsoluteDate date,
            final double[] meanElements) throws OrekitException {

        // Build an Orbit object from the mean elements
        final Orbit meanOrbit = OrbitType.EQUINOCTIAL.mapArrayToOrbit(
                meanElements, PositionAngle.MEAN, date, provider.getMu(), this.frame);

        // Get the True longitude L
        final double L = meanOrbit.getLv();

        // Define maxJ
        final int maxJ = 2 * maxDegreeShortPeriodics + 1;

        // Compute the center
        final double center = L - meanElements[5];

        // Initialize short periodic variations
        final double[] shortPeriodicVariation = new double[6];
        for (int i = 0; i < 6; i++) {
            shortPeriodicVariation[i] = zonalSPCoefs.getCij(i, 0, date) + center * zonalSPCoefs.getDi(i, date);
        }

        for (int j = 1; j <= maxJ; j++) {
            for (int i = 0; i < 6; i++) {
                // Get Cij and Sij
                final double cij = zonalSPCoefs.getCij(i, j, date);
                final double sij = zonalSPCoefs.getSij(i, j, date);
                // add corresponding term to the short periodic variation
                shortPeriodicVariation[i] += cij * FastMath.cos(j * L);
                shortPeriodicVariation[i] += sij * FastMath.sin(j * L);
            }
        }

        return shortPeriodicVariation;
    }

    /** {@inheritDoc} */
    @Override
    public EventDetector[] getEventsDetectors() {
        return null;
    }

    /** {@inheritDoc} */
    public void computeShortPeriodicsCoefficients(final SpacecraftState state) throws OrekitException {
        //Compute all coefficients
        zonalSPCoefs.computeCoefficients(state.getDate());
    }

    /** Compute the mean element rates.
     * @param date current date
     * @return the mean element rates
     * @throws OrekitException if an error occurs in hansen computation
     */
    private double[] computeMeanElementRates(final AbsoluteDate date) throws OrekitException {
        // Compute potential derivative
        final double[] dU  = computeUDerivatives(date);
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

    /** Compute the derivatives of the gravitational potential U [Eq. 3.1-(6)].
     *  <p>
     *  The result is the array
     *  [dU/da, dU/dk, dU/dh, dU/dα, dU/dβ, dU/dγ]
     *  </p>
     *  @param date current date
     *  @return potential derivatives
     *  @throws OrekitException if an error occurs in hansen computation
     */
    private double[] computeUDerivatives(final AbsoluteDate date) throws OrekitException {

        final UnnormalizedSphericalHarmonics harmonics = provider.onDate(date);

        //Reset U
        U = 0.;

        // Gs and Hs coefficients
        final double[][] GsHs = CoefficientsFactory.computeGsHs(k, h, alpha, beta, maxEccPowMeanElements);
        // Qns coefficients
        final double[][] Qns  = CoefficientsFactory.computeQns(gamma, maxDegree, maxEccPowMeanElements);

        final double[] roaPow = new double[maxDegree + 1];
        roaPow[0] = 1.;
        for (int i = 1; i <= maxDegree; i++) {
            roaPow[i] = roa * roaPow[i - 1];
        }

        // Potential derivatives
        double dUda  = 0.;
        double dUdk  = 0.;
        double dUdh  = 0.;
        double dUdAl = 0.;
        double dUdBe = 0.;
        double dUdGa = 0.;

        for (int s = 0; s <= maxEccPowMeanElements; s++) {
            //Initialise the Hansen roots
            this.hansenObjects[s].computeInitValues(X);

            // Get the current Gs coefficient
            final double gs = GsHs[0][s];

            // Compute Gs partial derivatives from 3.1-(9)
            double dGsdh  = 0.;
            double dGsdk  = 0.;
            double dGsdAl = 0.;
            double dGsdBe = 0.;
            if (s > 0) {
                // First get the G(s-1) and the H(s-1) coefficients
                final double sxgsm1 = s * GsHs[0][s - 1];
                final double sxhsm1 = s * GsHs[1][s - 1];
                // Then compute derivatives
                dGsdh  = beta  * sxgsm1 - alpha * sxhsm1;
                dGsdk  = alpha * sxgsm1 + beta  * sxhsm1;
                dGsdAl = k * sxgsm1 - h * sxhsm1;
                dGsdBe = h * sxgsm1 + k * sxhsm1;
            }

            // Kronecker symbol (2 - delta(0,s))
            final double d0s = (s == 0) ? 1 : 2;

            for (int n = s + 2; n <= maxDegree; n++) {
                // (n - s) must be even
                if ((n - s) % 2 == 0) {

                    //Extract data from previous computation :
                    final double kns   = this.hansenObjects[s].getValue(-n - 1, X);
                    final double dkns  = this.hansenObjects[s].getDerivative(-n - 1, X);

                    final double vns   = Vns.get(new NSKey(n, s));
                    final double coef0 = d0s * roaPow[n] * vns * -harmonics.getUnnormalizedCnm(n, 0);
                    final double coef1 = coef0 * Qns[n][s];
                    final double coef2 = coef1 * kns;
                    final double coef3 = coef2 * gs;
                    // dQns/dGamma = Q(n, s + 1) from Equation 3.1-(8)
                    final double dqns  = Qns[n][s + 1];

                    // Compute U
                    U += coef3;
                    // Compute dU / da :
                    dUda += coef3 * (n + 1);
                    // Compute dU / dEx
                    dUdk += coef1 * (kns * dGsdk + k * XXX * gs * dkns);
                    // Compute dU / dEy
                    dUdh += coef1 * (kns * dGsdh + h * XXX * gs * dkns);
                    // Compute dU / dAlpha
                    dUdAl += coef2 * dGsdAl;
                    // Compute dU / dBeta
                    dUdBe += coef2 * dGsdBe;
                    // Compute dU / dGamma
                    dUdGa += coef0 * kns * dqns * gs;

                }
            }
        }

        // Multiply by -(μ / a)
        U *= -muoa;

        return new double[] {
            dUda  *  muoa / a,
            dUdk  * -muoa,
            dUdh  * -muoa,
            dUdAl * -muoa,
            dUdBe * -muoa,
            dUdGa * -muoa
        };
    }

    /** {@inheritDoc} */
    @Override
    public void registerAttitudeProvider(final AttitudeProvider attitudeProvider) {
        //nothing is done since this contribution is not sensitive to attitude
    }

    /** {@inheritDoc} */
    @Override
    public void resetShortPeriodicsCoefficients() {
        if (zonalSPCoefs != null) {
            zonalSPCoefs.resetCoefficients();
        }
    }

    /** Check if an index is within the accepted interval.
     *
     * @param index the index to check
     * @param lowerBound the lower bound of the interval
     * @param upperBound the upper bound of the interval
     * @return true if the index is between the lower and upper bounds, false otherwise
     */
    private boolean isBetween(final int index, final int lowerBound, final int upperBound) {
        return index >= lowerBound && index <= upperBound;
    }

    /** The coefficients used to compute the short-periodic zonal contribution.
     *
     * <p>
     * Those coefficients are given in Danielson paper by expressions 4.1-(20) to 4.1.-(25)
     * </p>
     * <p>
     * The coefficients are: <br/>
     * - C<sub>i</sub><sup>j</sup> and S<sub>i</sub><sup>j</sup> <br/>
     * - ρ<sub>j</sub> and σ<sub>j</sub> <br/>
     * - C<sub>i</sub>⁰
     * </p>
     *
     * @author Lucian Barbulescu
     */
    private class ZonalShortPeriodicCoefficients
    {

        /**The coefficients D<sub>i</sub>.
         * <p>
         * i corresponds to the equinoctial element, as follows:
         * - i=0 for a <br/>
         * - i=1 for k <br/>
         * - i=2 for h <br/>
         * - i=3 for q <br/>
         * - i=4 for p <br/>
         * - i=5 for λ <br/>
         * </p>
         */
        private final ShortPeriodicsInterpolatedCoefficient[] di;

        /** The coefficients C<sub>i</sub><sup>j</sup>.
         * <p>
         * The constant term C<sub>i</sub>⁰ is also stored in this variable at index j = 0 <br>
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
        private final ShortPeriodicsInterpolatedCoefficient[][] cij;

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
        private final ShortPeriodicsInterpolatedCoefficient[][] sij;

        /** The coefficients ρ<sub>j</sub>. */
        private final double[] rhoj;

        /** The coefficients σ<sub>j</sub>. */
        private final double[] sigmaj;

        /** N maximum. */
        private final int nMax;

        /** S maximum. */
        private final int sMax;

        /** J maximum. */
        private final int jMax;

        /** The C<sup>j</sup> and the S<sup>j</sup> coefficients. */
        private FourierCjSjCoefficients cjsj;

        /** h * k. */
        private double hk;
        /** k² - h². */
        private double k2mh2;
        /** (k² - h²) / 2. */
        private double k2mh2o2;
        /** 1 / (n² * a²). */
        private double oon2a2;
        /** 1 / (n² * a) . */
        private double oon2a;
        /** χ³ / (n² * a). */
        private double x3on2a;
        /** χ / (n² * a²). */
        private double xon2a2;
        /** (C * χ) / ( 2 * n² * a² ). */
        private double cxo2n2a2;
        /** (χ²) / (n² * a² * (χ + 1 ) ). */
        private double x2on2a2xp1;
        /** B * B.*/
        private double BB;

        /**  Constructor.
         *  @param nMax maximum possible value for n
         *  @param maxEccPow maximum eccentricity power
         *  @param interpolationPoints number of points used in the interpolation process
         */
        public ZonalShortPeriodicCoefficients(final int nMax, final int maxEccPow, final int interpolationPoints) {

            // Save parameters
            this.nMax = nMax;
            this.sMax = maxEccPow;
            this.jMax = 2 * nMax + 1;

            this.di = new ShortPeriodicsInterpolatedCoefficient[6];

            this.cij = new ShortPeriodicsInterpolatedCoefficient[jMax + 1][6];
            this.sij = new ShortPeriodicsInterpolatedCoefficient[jMax + 1][6];

            this.rhoj = new double[jMax + 1];
            this.sigmaj  = new double[jMax + 1];

            //Initialise the arrays
            for (int j = 0; j <= jMax; j++) {
                for (int i = 0; i < 6; i++) {
                    this.cij[j][i] = new ShortPeriodicsInterpolatedCoefficient(interpolationPoints);
                    this.sij[j][i] = new ShortPeriodicsInterpolatedCoefficient(interpolationPoints);
                }
            }

            for (int i = 0; i < 6; i++) {
                this.di[i] = new ShortPeriodicsInterpolatedCoefficient(interpolationPoints);
            }
        }

        /** Reset the coefficients.
         * <p>
         * For each coefficient, clear history of computed points
         * </p>
         */
        public void resetCoefficients() {

            for (int j = 0; j <= jMax; j++) {
                this.rhoj[j] = 0.;
                this.sigmaj[j] = 0.;
                for (int i = 0; i < 6; i++) {
                    this.cij[j][i].clearHistory();
                    this.sij[j][i].clearHistory();
                }
            }

            for (int i = 0; i < 6; i++) {
                this.di[i].clearHistory();
            }
        }

        /** Compute the short periodic coefficients.
         *
         * @param date the current date
         * @throws OrekitException if an error occurs
         */
        public void computeCoefficients(final AbsoluteDate date)
            throws OrekitException {
            // h * k.
            this.hk = h * k;
            // k² - h².
            this.k2mh2 = k * k - h * h;
            // (k² - h²) / 2.
            this.k2mh2o2 = k2mh2 / 2.;
            // 1 / (n² * a²) = 1 / (n * A)
            this.oon2a2 = 1 / (A * meanMotion);
            // 1 / (n² * a) = a / (n * A)
            this.oon2a = a * oon2a2;
            // χ³ / (n² * a)
            this.x3on2a = XXX * oon2a;
            // χ / (n² * a²)
            this.xon2a2 = X * oon2a2;
            // (C * χ) / ( 2 * n² * a² )
            this.cxo2n2a2 = xon2a2 * C / 2;
            // (χ²) / (n² * a² * (χ + 1 ) )
            this.x2on2a2xp1 = xon2a2 * X / (X + 1);
            // B * B
            this.BB = B * B;

            // Create a new instance of Fourrier coefficients
            this.cjsj = new FourierCjSjCoefficients(date, nMax, sMax);

            // Compute rhoj and sigmaj
            computeRhoSigmaCoefficients(date);

            // Compute Di
            computeDiCoefficients(date);

            // generate the Cij and Sij coefficients
            computeCijSijCoefficients(date);
        }

        /** Generate the values for the D<sub>i</sub> coefficients.
         * @param date target date
         * @throws OrekitException if an error occurs during the coefficient computation
         */
        private void computeDiCoefficients(final AbsoluteDate date) throws OrekitException {
            final double[] meanElementRates = computeMeanElementRates(date);
            final double [] currentDi = new double[6];

            // Add the coefficients to the interpolation grid
            for (int i = 0; i < 6; i++) {
                currentDi[i] = meanElementRates[i] / meanMotion;

                if (i == 5) {
                    currentDi[i] += -1.5 * 2 * U * oon2a2;
                }

                di[i].addGridPoint(date, currentDi[i]);
            }
        }

        /**
         * Generate the values for the C<sub>i</sub><sup>j</sup> and the S<sub>i</sub><sup>j</sup> coefficients.
         * @param date date of computation
         */
        private void computeCijSijCoefficients(final AbsoluteDate date) {
            // The C<sub>i</sub>⁰ coefficients
            final double [] currentCi0 = new double[] {0., 0., 0., 0., 0., 0.};
            for (int j = 1; j <= jMax; j++) {

                // Create local arrays
                final double[] currentCij = new double[] {0., 0., 0., 0., 0., 0.};
                final double[] currentSij = new double[] {0., 0., 0., 0., 0., 0.};

                // j == 1
                if (j == 1) {
                    final double coef1 = 4 * k * U - hk * cjsj.getCj(1) + k2mh2o2 * cjsj.getSj(1);
                    final double coef2 = 4 * h * U + k2mh2o2 * cjsj.getCj(1) + hk * cjsj.getSj(1);
                    final double coef3 = (k * cjsj.getCj(1) + h * cjsj.getSj(1)) / 4.;
                    final double coef4 = (8 * U - h * cjsj.getCj(1) + k * cjsj.getSj(1)) / 4.;

                    //Coefficients for a
                    currentCij[0] += coef1;
                    currentSij[0] += coef2;

                    //Coefficients for k
                    currentCij[1] += coef4;
                    currentSij[1] += coef3;

                    //Coefficients for h
                    currentCij[2] -= coef3;
                    currentSij[2] += coef4;

                    //Coefficients for λ
                    currentCij[5] -= coef2 / 2;
                    currentSij[5] += coef1 / 2;
                }

                // j == 2
                if (j == 2) {
                    final double coef1 = k2mh2 * U;
                    final double coef2 = 2 * hk * U;
                    final double coef3 = h * U / 2;
                    final double coef4 = k * U / 2;

                    //Coefficients for a
                    currentCij[0] += coef1;
                    currentSij[0] += coef2;

                    //Coefficients for k
                    currentCij[1] += coef4;
                    currentSij[1] += coef3;

                    //Coefficients for h
                    currentCij[2] -= coef3;
                    currentSij[2] += coef4;

                    //Coefficients for λ
                    currentCij[5] -= coef2 / 2;
                    currentSij[5] += coef1 / 2;
                }

                // j between 1 and 2N-3
                if (isBetween(j, 1, 2 * nMax - 3)) {
                    final double coef1 = ( j + 2 ) * (-hk * cjsj.getCj(j + 2) + k2mh2o2 * cjsj.getSj(j + 2));
                    final double coef2 = ( j + 2 ) * (k2mh2o2 * cjsj.getCj(j + 2) + hk * cjsj.getSj(j + 2));
                    final double coef3 = ( j + 2 ) * (k * cjsj.getCj(j + 2) + h * cjsj.getSj(j + 2)) / 4;
                    final double coef4 = ( j + 2 ) * (h * cjsj.getCj(j + 2) - k * cjsj.getSj(j + 2)) / 4;

                    //Coefficients for a
                    currentCij[0] += coef1;
                    currentSij[0] -= coef2;

                    //Coefficients for k
                    currentCij[1] += -coef4;
                    currentSij[1] -= coef3;

                    //Coefficients for h
                    currentCij[2] -= coef3;
                    currentSij[2] += coef4;

                    //Coefficients for λ
                    currentCij[5] -= coef2 / 2;
                    currentSij[5] += -coef1 / 2;
                }

                // j between 1 and 2N-2
                if (isBetween(j, 1, 2 * nMax - 2)) {
                    final double coef1 = 2 * ( j + 1 ) * (-h * cjsj.getCj(j + 1) + k * cjsj.getSj(j + 1));
                    final double coef2 = 2 * ( j + 1 ) * (k * cjsj.getCj(j + 1) + h * cjsj.getSj(j + 1));
                    final double coef3 = ( j + 1 ) * cjsj.getCj(j + 1);
                    final double coef4 = ( j + 1 ) * cjsj.getSj(j + 1);

                    //Coefficients for a
                    currentCij[0] += coef1;
                    currentSij[0] -= coef2;

                    //Coefficients for k
                    currentCij[1] += coef4;
                    currentSij[1] -= coef3;

                    //Coefficients for h
                    currentCij[2] -= coef3;
                    currentSij[2] -= coef4;

                    //Coefficients for λ
                    currentCij[5] -= coef2 / 2;
                    currentSij[5] += -coef1 / 2;
                }

                // j between 2 and 2N
                if (isBetween(j, 2, 2 * nMax)) {
                    final double coef1 = 2 * ( j - 1 ) * (h * cjsj.getCj(j - 1) + k * cjsj.getSj(j - 1));
                    final double coef2 = 2 * ( j - 1 ) * (k * cjsj.getCj(j - 1) - h * cjsj.getSj(j - 1));
                    final double coef3 = ( j - 1 ) * cjsj.getCj(j - 1);
                    final double coef4 = ( j - 1 ) * cjsj.getSj(j - 1);

                    //Coefficients for a
                    currentCij[0] += coef1;
                    currentSij[0] -= coef2;

                    //Coefficients for k
                    currentCij[1] += coef4;
                    currentSij[1] -= coef3;

                    //Coefficients for h
                    currentCij[2] += coef3;
                    currentSij[2] += coef4;

                    //Coefficients for λ
                    currentCij[5] += coef2 / 2;
                    currentSij[5] += coef1 / 2;
                }

                // j between 3 and 2N + 1
                if (isBetween(j, 3, 2 * nMax + 1)) {
                    final double coef1 = ( j - 2 ) * (hk * cjsj.getCj(j - 2) + k2mh2o2 * cjsj.getSj(j - 2));
                    final double coef2 = ( j - 2 ) * (-k2mh2o2 * cjsj.getCj(j - 2) + hk * cjsj.getSj(j - 2));
                    final double coef3 = ( j - 2 ) * (k * cjsj.getCj(j - 2) - h * cjsj.getSj(j - 2)) / 4;
                    final double coef4 = ( j - 2 ) * (h * cjsj.getCj(j - 2) + k * cjsj.getSj(j - 2)) / 4;
                    final double coef5 = ( j - 2 ) * (k2mh2o2 * cjsj.getCj(j - 2) - hk * cjsj.getSj(j - 2));

                    //Coefficients for a
                    currentCij[0] += coef1;
                    currentSij[0] += coef2;

                    //Coefficients for k
                    currentCij[1] += coef4;
                    currentSij[1] += -coef3;

                    //Coefficients for h
                    currentCij[2] += coef3;
                    currentSij[2] += coef4;

                    //Coefficients for λ
                    currentCij[5] += coef5 / 2;
                    currentSij[5] += coef1 / 2;
                }

                //multiply by the common factor
                //for a (i == 0) -> χ³ / (n² * a)
                currentCij[0] *= this.x3on2a;
                currentSij[0] *= this.x3on2a;
                //for k (i == 1) -> χ / (n² * a²)
                currentCij[1] *= this.xon2a2;
                currentSij[1] *= this.xon2a2;
                //for h (i == 2) -> χ / (n² * a²)
                currentCij[2] *= this.xon2a2;
                currentSij[2] *= this.xon2a2;
                //for λ (i == 5) -> (χ²) / (n² * a² * (χ + 1 ) )
                currentCij[5] *= this.x2on2a2xp1;
                currentSij[5] *= this.x2on2a2xp1;

                // j is between 1 and 2 * N - 1
                if (isBetween(j, 1, 2 * nMax - 1)) {
                    // Compute cross derivatives
                    // Cj(alpha,gamma) = alpha * dC/dgamma - gamma * dC/dalpha
                    final double CjAlphaGamma   = alpha * cjsj.getdCjdGamma(j) - gamma * cjsj.getdCjdAlpha(j);
                    // Cj(alpha,beta) = alpha * dC/dbeta - beta * dC/dalpha
                    final double CjAlphaBeta   = alpha * cjsj.getdCjdBeta(j) - beta * cjsj.getdCjdAlpha(j);
                    // Cj(beta,gamma) = beta * dC/dgamma - gamma * dC/dbeta
                    final double CjBetaGamma    =  beta * cjsj.getdCjdGamma(j) - gamma * cjsj.getdCjdBeta(j);
                    // Cj(h,k) = h * dC/dk - k * dC/dh
                    final double CjHK   = h * cjsj.getdCjdK(j) - k * cjsj.getdCjdH(j);
                    // Sj(alpha,gamma) = alpha * dS/dgamma - gamma * dS/dalpha
                    final double SjAlphaGamma   = alpha * cjsj.getdSjdGamma(j) - gamma * cjsj.getdSjdAlpha(j);
                    // Sj(alpha,beta) = alpha * dS/dbeta - beta * dS/dalpha
                    final double SjAlphaBeta   = alpha * cjsj.getdSjdBeta(j) - beta * cjsj.getdSjdAlpha(j);
                    // Sj(beta,gamma) = beta * dS/dgamma - gamma * dS/dbeta
                    final double SjBetaGamma    =  beta * cjsj.getdSjdGamma(j) - gamma * cjsj.getdSjdBeta(j);
                    // Sj(h,k) = h * dS/dk - k * dS/dh
                    final double SjHK   = h * cjsj.getdSjdK(j) - k * cjsj.getdSjdH(j);

                    //Coefficients for a
                    final double coef1 = this.x3on2a * (3 - BB) * j;
                    currentCij[0] += coef1 * cjsj.getSj(j);
                    currentSij[0] -= coef1 * cjsj.getCj(j);

                    //Coefficients for k and h
                    final double coef2 = p * CjAlphaGamma - I * q * CjBetaGamma;
                    final double coef3 = p * SjAlphaGamma - I * q * SjBetaGamma;
                    currentCij[1] -= this.xon2a2 * (h * coef2 + BB * cjsj.getdCjdH(j) - 1.5 * k * j * cjsj.getSj(j));
                    currentSij[1] -= this.xon2a2 * (h * coef3 + BB * cjsj.getdSjdH(j) + 1.5 * k * j * cjsj.getCj(j));
                    currentCij[2] += this.xon2a2 * (k * coef2 + BB * cjsj.getdCjdK(j) + 1.5 * h * j * cjsj.getSj(j));
                    currentSij[2] += this.xon2a2 * (k * coef3 + BB * cjsj.getdSjdK(j) - 1.5 * h * j * cjsj.getCj(j));

                    //Coefficients for q and p
                    final double coef4 = CjHK - CjAlphaBeta - j * cjsj.getSj(j);
                    final double coef5 = SjHK - SjAlphaBeta + j * cjsj.getCj(j);
                    currentCij[3] = this.cxo2n2a2 * (-I * CjAlphaGamma + q * coef4);
                    currentSij[3] = this.cxo2n2a2 * (-I * SjAlphaGamma + q * coef5);
                    currentCij[4] = this.cxo2n2a2 * (-CjBetaGamma + p * coef4);
                    currentSij[4] = this.cxo2n2a2 * (-SjBetaGamma + p * coef5);

                    //Coefficients for λ
                    final double coef6 = h * cjsj.getdCjdH(j) + k * cjsj.getdCjdK(j);
                    final double coef7 = h * cjsj.getdSjdH(j) + k * cjsj.getdSjdK(j);
                    currentCij[5] += this.oon2a2 * (-2 * a * cjsj.getdCjdA(j) + coef6 / (X + 1) + X * coef2 - 3 * cjsj.getCj(j));
                    currentSij[5] += this.oon2a2 * (-2 * a * cjsj.getdSjdA(j) + coef7 / (X + 1) + X * coef3 - 3 * cjsj.getSj(j));
                }

                for (int i = 0; i < 6; i++) {
                    //Add the current coefficients contribution to C<sub>i</sub>⁰
                    currentCi0[i] -= currentCij[i] * rhoj[j] + currentSij[i] * sigmaj[j];

                    // Add the coefficients to the interpolation grid
                    cij[j][i].addGridPoint(date, currentCij[i]);
                    sij[j][i].addGridPoint(date, currentSij[i]);
                }
            }

            //Add C<sub>i</sub>⁰ to the interpolation grid
            for (int i = 0; i < 6; i++) {
                cij[0][i].addGridPoint(date, currentCi0[i]);
            }
        }

        /**
         * Compute the auxiliary quantities ρ<sub>j</sub> and σ<sub>j</sub>.
         * <p>
         * The expressions used are equations 2.5.3-(4) from the Danielson paper. <br/>
         *  ρ<sub>j</sub> = (1+jB)(-b)<sup>j</sup>C<sub>j</sub>(k, h) <br/>
         *  σ<sub>j</sub> = (1+jB)(-b)<sup>j</sup>S<sub>j</sub>(k, h) <br/>
         * </p>
         * @param date target date
         */
        private void computeRhoSigmaCoefficients(final AbsoluteDate date) {
            final CjSjCoefficient cjsjKH = new CjSjCoefficient(k, h);
            final double b = 1. / (1 + B);

            // (-b)<sup>j</sup>
            double mbtj = 1;

            for (int j = 1; j <= jMax; j++) {
                double rho;
                double sigma;

                //Compute current rho and sigma;
                mbtj *= -b;
                final double coef = (1 + j * B) * mbtj;
                rho = coef * cjsjKH.getCj(j);
                sigma = coef * cjsjKH.getSj(j);

                // Add the coefficients to the interpolation grid
                rhoj[j] = rho;
                sigmaj[j] = sigma;
            }
        }

        /** Get C<sub>i</sub><sup>j</sup>.
         *
         * @param i i index
         * @param j j index
         * @param date the date
         * @return C<sub>i</sub><sup>j</sup>
         */
        public double getCij(final int i, final int j, final AbsoluteDate date) {
            return cij[j][i].value(date);
        }

        /** Get S<sub>i</sub><sup>j</sup>.
         *
         * @param i i index
         * @param j j index
         * @param date the date
         * @return S<sub>i</sub><sup>j</sup>
         */
        public double getSij(final int i, final int j, final AbsoluteDate date) {
            return sij[j][i].value(date);
        }

        /** Get D<sub>i</sub>.
         * @param i i index
         * @param date target date
         * @return D<sub>i</sub>
         */
        public double getDi(final int i, final AbsoluteDate date) {
            return di[i].value(date);
        }
    }

    /** Compute the C<sup>j</sup> and the S<sup>j</sup> coefficients.
     *  <p>
     *  Those coefficients are given in Danielson paper by expressions 4.1-(13) to 4.1.-(16b)
     *  </p>
     */
    private class FourierCjSjCoefficients {

        /** The G<sub>js</sub>, H<sub>js</sub>, I<sub>js</sub> and J<sub>js</sub> polynomials. */
        private final GHIJjsPolynomials ghijCoef;

        /** L<sub>n</sub><sup>s</sup>(γ). */
        private final LnsCoefficients lnsCoef;

        /** Maximum possible value for n. */
        private final int nMax;

        /** Maximum possible value for s. */
        private final int sMax;

        /** Maximum possible value for j. */
        private final int jMax;

        /** The C<sup>j</sup> coefficients and their derivatives.
         * <p>
         * Each column of the matrix contains the following values: <br/>
         * - C<sup>j</sup> <br/>
         * - dC<sup>j</sup> / da <br/>
         * - dC<sup>j</sup> / dh <br/>
         * - dC<sup>j</sup> / dk <br/>
         * - dC<sup>j</sup> / dα <br/>
         * - dC<sup>j</sup> / dβ <br/>
         * - dC<sup>j</sup> / dγ <br/>
         * </p>
         */
        private final double[][] cCoef;

        /** The S<sup>j</sup> coefficients and their derivatives.
         * <p>
         * Each column of the matrix contains the following values: <br/>
         * - S<sup>j</sup> <br/>
         * - dS<sup>j</sup> / da <br/>
         * - dS<sup>j</sup> / dh <br/>
         * - dS<sup>j</sup> / dk <br/>
         * - dS<sup>j</sup> / dα <br/>
         * - dS<sup>j</sup> / dβ <br/>
         * - dS<sup>j</sup> / dγ <br/>
         * </p>
         */
        private final double[][] sCoef;

        /** h * &Chi;³. */
        private final double hXXX;
        /** k * &Chi;³. */
        private final double kXXX;

        /** Create a set of C<sup>j</sup> and the S<sup>j</sup> coefficients.
         *  @param date the current date
         *  @param nMax maximum possible value for n
         *  @param sMax maximum possible value for s
         * @throws OrekitException if an error occurs while generating the coefficients
         */
        public FourierCjSjCoefficients(final AbsoluteDate date, final int nMax, final int sMax)
            throws OrekitException {
            this.ghijCoef = new GHIJjsPolynomials(k, h, alpha, beta);
            // Qns coefficients
            final double[][] Qns  = CoefficientsFactory.computeQns(gamma, nMax, nMax);

            this.lnsCoef = new LnsCoefficients(nMax, nMax, Qns, Vns, roa);
            this.nMax = nMax;
            this.sMax = sMax;
            this.jMax = 2 * nMax - 1;

            // compute the common factors that depends on the mean elements
            this.hXXX = h * XXX;
            this.kXXX = k * XXX;

            this.cCoef = new double[7][jMax + 1];
            this.sCoef = new double[7][jMax + 1];

            for (int s = 0; s <= sMax; s++) {
                //Initialise the Hansen roots
                hansenObjects[s].computeInitValues(X);
            }
            generateCoefficients(date);
        }

        /** Generate all coefficients.
         * @param date the current date
         * @throws OrekitException if an error occurs while generating the coefficients
         */
        private void generateCoefficients(final AbsoluteDate date) throws OrekitException {
            final UnnormalizedSphericalHarmonics harmonics = provider.onDate(date);
            for (int j = 1; j <= jMax; j++) {

                //init arrays
                for (int i = 0; i <= 6; i++) {
                    cCoef[i][j] = 0.;
                    sCoef[i][j] = 0.;
                }

                if (isBetween(j, 1, nMax - 1)) {

                    //compute first double sum where s: j -> N-1 and n: s+1 -> N
                    for (int s = j; s <= FastMath.min(nMax - 1, sMax); s++) {
                        // j - s
                        final int jms = j - s;
                        // Kronecker symbols (2 - delta(0,s-j)) and (2 - delta(0,j-s))
                        final int d0smj = (s == j) ? 1 : 2;

                        for (int n = s + 1; n <= nMax; n++) {
                            // if n + (j-s) is odd, then the term is equal to zero due to the factor Vn,s-j
                            if ((n + jms) % 2 == 0) {
                                // (2 - delta(0,s-j)) * J<sub>n</sub> * K₀<sup>-n-1,s</sup> * L<sub>n</sub><sup>j-s</sup>
                                final double lns = lnsCoef.getLns(n, -jms);
                                final double dlns = lnsCoef.getdLnsdGamma(n, -jms);

                                final double hjs = ghijCoef.getHjs(s, -jms);
                                final double dHjsdh = ghijCoef.getdHjsdh(s, -jms);
                                final double dHjsdk = ghijCoef.getdHjsdk(s, -jms);
                                final double dHjsdAlpha = ghijCoef.getdHjsdAlpha(s, -jms);
                                final double dHjsdBeta = ghijCoef.getdHjsdBeta(s, -jms);

                                final double gjs = ghijCoef.getGjs(s, -jms);
                                final double dGjsdh = ghijCoef.getdGjsdh(s, -jms);
                                final double dGjsdk = ghijCoef.getdGjsdk(s, -jms);
                                final double dGjsdAlpha = ghijCoef.getdGjsdAlpha(s, -jms);
                                final double dGjsdBeta = ghijCoef.getdGjsdBeta(s, -jms);

                                // J<sub>n</sub>
                                final double jn = -harmonics.getUnnormalizedCnm(n, 0);

                                // K₀<sup>-n-1,s</sup>
                                final double kns   = hansenObjects[s].getValue(-n - 1, X);
                                final double dkns  = hansenObjects[s].getDerivative(-n - 1, X);

                                final double coef0 = d0smj * jn;
                                final double coef1 = coef0 * lns;
                                final double coef2 = coef1 * kns;
                                final double coef3 = coef2 * hjs;
                                final double coef4 = coef2 * gjs;

                                // Add the term to the coefficients
                                cCoef[0][j] += coef3;
                                cCoef[1][j] += coef3 * (n + 1);
                                cCoef[2][j] += coef1 * (kns * dHjsdh + hjs * hXXX * dkns);
                                cCoef[3][j] += coef1 * (kns * dHjsdk + hjs * kXXX * dkns);
                                cCoef[4][j] += coef2 * dHjsdAlpha;
                                cCoef[5][j] += coef2 * dHjsdBeta;
                                cCoef[6][j] += coef0 * dlns * kns * hjs;

                                sCoef[0][j] += coef4;
                                sCoef[1][j] += coef4 * (n + 1);
                                sCoef[2][j] += coef1 * (kns * dGjsdh + gjs * hXXX * dkns);
                                sCoef[3][j] += coef1 * (kns * dGjsdk + gjs * kXXX * dkns);
                                sCoef[4][j] += coef2 * dGjsdAlpha;
                                sCoef[5][j] += coef2 * dGjsdBeta;
                                sCoef[6][j] += coef0 * dlns * kns * gjs;
                            }
                        }
                    }

                    //compute second double sum where s: 0 -> N-j and n: max(j+s, j+1) -> N
                    for (int s = 0; s <= FastMath.min(nMax - j, sMax); s++) {
                        // j + s
                        final int jps = j + s;
                        // Kronecker symbols (2 - delta(0,j+s))
                        final double d0spj = (s == -j) ? 1 : 2;

                        for (int n = FastMath.max(j + s, j + 1); n <= nMax; n++) {
                            // if n + (j+s) is odd, then the term is equal to zero due to the factor Vn,s+j
                            if ((n + jps) % 2 == 0) {
                                // (2 - delta(0,s+j)) * J<sub>n</sub> * K₀<sup>-n-1,s</sup> * L<sub>n</sub><sup>j+s</sup>
                                final double lns = lnsCoef.getLns(n, jps);
                                final double dlns = lnsCoef.getdLnsdGamma(n, jps);

                                final double hjs = ghijCoef.getHjs(s, jps);
                                final double dHjsdh = ghijCoef.getdHjsdh(s, jps);
                                final double dHjsdk = ghijCoef.getdHjsdk(s, jps);
                                final double dHjsdAlpha = ghijCoef.getdHjsdAlpha(s, jps);
                                final double dHjsdBeta = ghijCoef.getdHjsdBeta(s, jps);

                                final double gjs = ghijCoef.getGjs(s, jps);
                                final double dGjsdh = ghijCoef.getdGjsdh(s, jps);
                                final double dGjsdk = ghijCoef.getdGjsdk(s, jps);
                                final double dGjsdAlpha = ghijCoef.getdGjsdAlpha(s, jps);
                                final double dGjsdBeta = ghijCoef.getdGjsdBeta(s, jps);

                                // J<sub>n</sub>
                                final double jn = -harmonics.getUnnormalizedCnm(n, 0);

                                // K₀<sup>-n-1,s</sup>
                                final double kns   = hansenObjects[s].getValue(-n - 1, X);
                                final double dkns  = hansenObjects[s].getDerivative(-n - 1, X);

                                final double coef0 = d0spj * jn;
                                final double coef1 = coef0 * lns;
                                final double coef2 = coef1 * kns;

                                final double coef3 = coef2 * hjs;
                                final double coef4 = coef2 * gjs;

                                // Add the term to the coefficients
                                cCoef[0][j] -= coef3;
                                cCoef[1][j] -= coef3 * (n + 1);
                                cCoef[2][j] -= coef1 * (kns * dHjsdh + hjs * hXXX * dkns);
                                cCoef[3][j] -= coef1 * (kns * dHjsdk + hjs * kXXX * dkns);
                                cCoef[4][j] -= coef2 * dHjsdAlpha;
                                cCoef[5][j] -= coef2 * dHjsdBeta;
                                cCoef[6][j] -= coef0 * dlns * kns * hjs;

                                sCoef[0][j] += coef4;
                                sCoef[1][j] += coef4 * (n + 1);
                                sCoef[2][j] += coef1 * (kns * dGjsdh + gjs * hXXX * dkns);
                                sCoef[3][j] += coef1 * (kns * dGjsdk + gjs * kXXX * dkns);
                                sCoef[4][j] += coef2 * dGjsdAlpha;
                                sCoef[5][j] += coef2 * dGjsdBeta;
                                sCoef[6][j] += coef0 * dlns * kns * gjs;
                            }
                        }
                    }

                    //compute third double sum where s: 1 -> j and  n: j+1 -> N
                    for (int s = 1; s <= FastMath.min(j, sMax); s++) {
                        // j - s
                        final int jms = j - s;
                        // Kronecker symbols (2 - delta(0,s-j)) and (2 - delta(0,j-s))
                        final int d0smj = (s == j) ? 1 : 2;

                        for (int n = j + 1; n <= nMax; n++) {
                            // if n + (j-s) is odd, then the term is equal to zero due to the factor Vn,s-j
                            if ((n + jms) % 2 == 0) {
                                // (2 - delta(0,j-s)) * J<sub>n</sub> * K₀<sup>-n-1,s</sup> * L<sub>n</sub><sup>j-s</sup>
                                final double lns = lnsCoef.getLns(n, jms);
                                final double dlns = lnsCoef.getdLnsdGamma(n, jms);

                                final double ijs = ghijCoef.getIjs(s, jms);
                                final double dIjsdh = ghijCoef.getdIjsdh(s, jms);
                                final double dIjsdk = ghijCoef.getdIjsdk(s, jms);
                                final double dIjsdAlpha = ghijCoef.getdIjsdAlpha(s, jms);
                                final double dIjsdBeta = ghijCoef.getdIjsdBeta(s, jms);

                                final double jjs = ghijCoef.getJjs(s, jms);
                                final double dJjsdh = ghijCoef.getdJjsdh(s, jms);
                                final double dJjsdk = ghijCoef.getdJjsdk(s, jms);
                                final double dJjsdAlpha = ghijCoef.getdJjsdAlpha(s, jms);
                                final double dJjsdBeta = ghijCoef.getdJjsdBeta(s, jms);

                                // J<sub>n</sub>
                                final double jn = -harmonics.getUnnormalizedCnm(n, 0);

                                // K₀<sup>-n-1,s</sup>
                                final double kns   = hansenObjects[s].getValue(-n - 1, X);
                                final double dkns  = hansenObjects[s].getDerivative(-n - 1, X);

                                final double coef0 = d0smj * jn;
                                final double coef1 = coef0 * lns;
                                final double coef2 = coef1 * kns;

                                final double coef3 = coef2 * ijs;
                                final double coef4 = coef2 * jjs;

                                // Add the term to the coefficients
                                cCoef[0][j] -= coef3;
                                cCoef[1][j] -= coef3 * (n + 1);
                                cCoef[2][j] -= coef1 * (kns * dIjsdh + ijs * hXXX * dkns);
                                cCoef[3][j] -= coef1 * (kns * dIjsdk + ijs * kXXX * dkns);
                                cCoef[4][j] -= coef2 * dIjsdAlpha;
                                cCoef[5][j] -= coef2 * dIjsdBeta;
                                cCoef[6][j] -= coef0 * dlns * kns * ijs;

                                sCoef[0][j] += coef4;
                                sCoef[1][j] += coef4 * (n + 1);
                                sCoef[2][j] += coef1 * (kns * dJjsdh + jjs * hXXX * dkns);
                                sCoef[3][j] += coef1 * (kns * dJjsdk + jjs * kXXX * dkns);
                                sCoef[4][j] += coef2 * dJjsdAlpha;
                                sCoef[5][j] += coef2 * dJjsdBeta;
                                sCoef[6][j] += coef0 * dlns * kns * jjs;
                            }
                        }
                    }
                }

                if (isBetween(j, 2, nMax)) {
                    //add first term
                    // J<sub>j</sub>
                    final double jj = -harmonics.getUnnormalizedCnm(j, 0);
                    double kns = hansenObjects[0].getValue(-j - 1, X);
                    double dkns = hansenObjects[0].getDerivative(-j - 1, X);

                    double lns = lnsCoef.getLns(j, j);
                    //dlns is 0 because n == s == j

                    final double hjs = ghijCoef.getHjs(0, j);
                    final double dHjsdh = ghijCoef.getdHjsdh(0, j);
                    final double dHjsdk = ghijCoef.getdHjsdk(0, j);
                    final double dHjsdAlpha = ghijCoef.getdHjsdAlpha(0, j);
                    final double dHjsdBeta = ghijCoef.getdHjsdBeta(0, j);

                    final double gjs = ghijCoef.getGjs(0, j);
                    final double dGjsdh = ghijCoef.getdGjsdh(0, j);
                    final double dGjsdk = ghijCoef.getdGjsdk(0, j);
                    final double dGjsdAlpha = ghijCoef.getdGjsdAlpha(0, j);
                    final double dGjsdBeta = ghijCoef.getdGjsdBeta(0, j);

                    // 2 * J<sub>j</sub> * K₀<sup>-j-1,0</sup> * L<sub>j</sub><sup>j</sup>
                    double coef0 = 2 * jj;
                    double coef1 = coef0 * lns;
                    double coef2 = coef1 * kns;

                    double coef3 = coef2 * hjs;
                    double coef4 = coef2 * gjs;

                    // Add the term to the coefficients
                    cCoef[0][j] -= coef3;
                    cCoef[1][j] -= coef3 * (j + 1);
                    cCoef[2][j] -= coef1 * (kns * dHjsdh + hjs * hXXX * dkns);
                    cCoef[3][j] -= coef1 * (kns * dHjsdk + hjs * kXXX * dkns);
                    cCoef[4][j] -= coef2 * dHjsdAlpha;
                    cCoef[5][j] -= coef2 * dHjsdBeta;
                    //no contribution to cCoef[6][j] because dlns is 0

                    sCoef[0][j] += coef4;
                    sCoef[1][j] += coef4 * (j + 1);
                    sCoef[2][j] += coef1 * (kns * dGjsdh + gjs * hXXX * dkns);
                    sCoef[3][j] += coef1 * (kns * dGjsdk + gjs * kXXX * dkns);
                    sCoef[4][j] += coef2 * dGjsdAlpha;
                    sCoef[5][j] += coef2 * dGjsdBeta;
                    //no contribution to sCoef[6][j] because dlns is 0

                    //compute simple sum where s: 1 -> j-1
                    for (int s = 1; s <= FastMath.min(j - 1, sMax); s++) {
                        // j - s
                        final int jms = j - s;
                        // Kronecker symbols (2 - delta(0,s-j)) and (2 - delta(0,j-s))
                        final int d0smj = (s == j) ? 1 : 2;

                        // if s is odd, then the term is equal to zero due to the factor Vj,s-j
                        if (s % 2 == 0) {
                            // (2 - delta(0,j-s)) * J<sub>j</sub> * K₀<sup>-j-1,s</sup> * L<sub>j</sub><sup>j-s</sup>
                            kns   = hansenObjects[s].getValue(-j - 1, X);
                            dkns  = hansenObjects[s].getDerivative(-j - 1, X);

                            lns = lnsCoef.getLns(j, jms);
                            final double dlns = lnsCoef.getdLnsdGamma(j, jms);

                            final double ijs = ghijCoef.getIjs(s, jms);
                            final double dIjsdh = ghijCoef.getdIjsdh(s, jms);
                            final double dIjsdk = ghijCoef.getdIjsdk(s, jms);
                            final double dIjsdAlpha = ghijCoef.getdIjsdAlpha(s, jms);
                            final double dIjsdBeta = ghijCoef.getdIjsdBeta(s, jms);

                            final double jjs = ghijCoef.getJjs(s, jms);
                            final double dJjsdh = ghijCoef.getdJjsdh(s, jms);
                            final double dJjsdk = ghijCoef.getdJjsdk(s, jms);
                            final double dJjsdAlpha = ghijCoef.getdJjsdAlpha(s, jms);
                            final double dJjsdBeta = ghijCoef.getdJjsdBeta(s, jms);

                            coef0 = d0smj * jj;
                            coef1 = coef0 * lns;
                            coef2 = coef1 * kns;

                            coef3 = coef2 * ijs;
                            coef4 = coef2 * jjs;

                            // Add the term to the coefficients
                            cCoef[0][j] -= coef3;
                            cCoef[1][j] -= coef3 * (j + 1);
                            cCoef[2][j] -= coef1 * (kns * dIjsdh + ijs * hXXX * dkns);
                            cCoef[3][j] -= coef1 * (kns * dIjsdk + ijs * kXXX * dkns);
                            cCoef[4][j] -= coef2 * dIjsdAlpha;
                            cCoef[5][j] -= coef2 * dIjsdBeta;
                            cCoef[6][j] -= coef0 * dlns * kns * ijs;

                            sCoef[0][j] += coef4;
                            sCoef[1][j] += coef4 * (j + 1);
                            sCoef[2][j] += coef1 * (kns * dJjsdh + jjs * hXXX * dkns);
                            sCoef[3][j] += coef1 * (kns * dJjsdk + jjs * kXXX * dkns);
                            sCoef[4][j] += coef2 * dJjsdAlpha;
                            sCoef[5][j] += coef2 * dJjsdBeta;
                            sCoef[6][j] += coef0 * dlns * kns * jjs;
                        }
                    }
                }

                if (isBetween(j, 3, 2 * nMax - 1)) {
                    //compute uppercase sigma expressions

                    //min(j-1,N)
                    final int minjm1on = FastMath.min(j - 1, nMax);

                    //if j is even
                    if (j % 2 == 0) {
                        //compute first double sum where s: j-min(j-1,N) -> j/2-1 and n: j-s -> min(j-1,N)
                        for (int s = j - minjm1on; s <= FastMath.min(j / 2 - 1, sMax); s++) {
                            // j - s
                            final int jms = j - s;
                            // Kronecker symbols (2 - delta(0,s-j)) and (2 - delta(0,j-s))
                            final int d0smj = (s == j) ? 1 : 2;

                            for (int n = j - s; n <= minjm1on; n++) {
                                // if n + (j-s) is odd, then the term is equal to zero due to the factor Vn,s-j
                                if ((n + jms) % 2 == 0) {
                                    // (2 - delta(0,j-s)) * J<sub>n</sub> * K₀<sup>-n-1,s</sup> * L<sub>n</sub><sup>j-s</sup>
                                    final double lns = lnsCoef.getLns(n, jms);
                                    final double dlns = lnsCoef.getdLnsdGamma(n, jms);

                                    final double ijs = ghijCoef.getIjs(s, jms);
                                    final double dIjsdh = ghijCoef.getdIjsdh(s, jms);
                                    final double dIjsdk = ghijCoef.getdIjsdk(s, jms);
                                    final double dIjsdAlpha = ghijCoef.getdIjsdAlpha(s, jms);
                                    final double dIjsdBeta = ghijCoef.getdIjsdBeta(s, jms);

                                    final double jjs = ghijCoef.getJjs(s, jms);
                                    final double dJjsdh = ghijCoef.getdJjsdh(s, jms);
                                    final double dJjsdk = ghijCoef.getdJjsdk(s, jms);
                                    final double dJjsdAlpha = ghijCoef.getdJjsdAlpha(s, jms);
                                    final double dJjsdBeta = ghijCoef.getdJjsdBeta(s, jms);

                                    // J<sub>n</sub>
                                    final double jn = -harmonics.getUnnormalizedCnm(n, 0);

                                    // K₀<sup>-n-1,s</sup>
                                    final double kns   = hansenObjects[s].getValue(-n - 1, X);
                                    final double dkns  = hansenObjects[s].getDerivative(-n - 1, X);

                                    final double coef0 = d0smj * jn;
                                    final double coef1 = coef0 * lns;
                                    final double coef2 = coef1 * kns;

                                    final double coef3 = coef2 * ijs;
                                    final double coef4 = coef2 * jjs;

                                    // Add the term to the coefficients
                                    cCoef[0][j] -= coef3;
                                    cCoef[1][j] -= coef3 * (n + 1);
                                    cCoef[2][j] -= coef1 * (kns * dIjsdh + ijs * hXXX * dkns);
                                    cCoef[3][j] -= coef1 * (kns * dIjsdk + ijs * kXXX * dkns);
                                    cCoef[4][j] -= coef2 * dIjsdAlpha;
                                    cCoef[5][j] -= coef2 * dIjsdBeta;
                                    cCoef[6][j] -= coef0 * dlns * kns * ijs;

                                    sCoef[0][j] += coef4;
                                    sCoef[1][j] += coef4 * (n + 1);
                                    sCoef[2][j] += coef1 * (kns * dJjsdh + jjs * hXXX * dkns);
                                    sCoef[3][j] += coef1 * (kns * dJjsdk + jjs * kXXX * dkns);
                                    sCoef[4][j] += coef2 * dJjsdAlpha;
                                    sCoef[5][j] += coef2 * dJjsdBeta;
                                    sCoef[6][j] += coef0 * dlns * kns * jjs;
                                }
                            }
                        }

                        //compute second double sum where s: j/2 -> min(j-1,N)-1 and n: s+1 -> min(j-1,N)
                        for (int s = j / 2; s <=  FastMath.min(minjm1on - 1, sMax); s++) {
                            // j - s
                            final int jms = j - s;
                            // Kronecker symbols (2 - delta(0,s-j)) and (2 - delta(0,j-s))
                            final int d0smj = (s == j) ? 1 : 2;

                            for (int n = s + 1; n <= minjm1on; n++) {
                                // if n + (j-s) is odd, then the term is equal to zero due to the factor Vn,s-j
                                if ((n + jms) % 2 == 0) {
                                    // (2 - delta(0,j-s)) * J<sub>n</sub> * K₀<sup>-n-1,s</sup> * L<sub>n</sub><sup>j-s</sup>
                                    final double lns = lnsCoef.getLns(n, jms);
                                    final double dlns = lnsCoef.getdLnsdGamma(n, jms);

                                    final double ijs = ghijCoef.getIjs(s, jms);
                                    final double dIjsdh = ghijCoef.getdIjsdh(s, jms);
                                    final double dIjsdk = ghijCoef.getdIjsdk(s, jms);
                                    final double dIjsdAlpha = ghijCoef.getdIjsdAlpha(s, jms);
                                    final double dIjsdBeta = ghijCoef.getdIjsdBeta(s, jms);

                                    final double jjs = ghijCoef.getJjs(s, jms);
                                    final double dJjsdh = ghijCoef.getdJjsdh(s, jms);
                                    final double dJjsdk = ghijCoef.getdJjsdk(s, jms);
                                    final double dJjsdAlpha = ghijCoef.getdJjsdAlpha(s, jms);
                                    final double dJjsdBeta = ghijCoef.getdJjsdBeta(s, jms);

                                    // J<sub>n</sub>
                                    final double jn = -harmonics.getUnnormalizedCnm(n, 0);

                                    // K₀<sup>-n-1,s</sup>
                                    final double kns   = hansenObjects[s].getValue(-n - 1, X);
                                    final double dkns  = hansenObjects[s].getDerivative(-n - 1, X);

                                    final double coef0 = d0smj * jn;
                                    final double coef1 = coef0 * lns;
                                    final double coef2 = coef1 * kns;

                                    final double coef3 = coef2 * ijs;
                                    final double coef4 = coef2 * jjs;

                                    // Add the term to the coefficients
                                    cCoef[0][j] -= coef3;
                                    cCoef[1][j] -= coef3 * (n + 1);
                                    cCoef[2][j] -= coef1 * (kns * dIjsdh + ijs * hXXX * dkns);
                                    cCoef[3][j] -= coef1 * (kns * dIjsdk + ijs * kXXX * dkns);
                                    cCoef[4][j] -= coef2 * dIjsdAlpha;
                                    cCoef[5][j] -= coef2 * dIjsdBeta;
                                    cCoef[6][j] -= coef0 * dlns * kns * ijs;

                                    sCoef[0][j] += coef4;
                                    sCoef[1][j] += coef4 * (n + 1);
                                    sCoef[2][j] += coef1 * (kns * dJjsdh + jjs * hXXX * dkns);
                                    sCoef[3][j] += coef1 * (kns * dJjsdk + jjs * kXXX * dkns);
                                    sCoef[4][j] += coef2 * dJjsdAlpha;
                                    sCoef[5][j] += coef2 * dJjsdBeta;
                                    sCoef[6][j] += coef0 * dlns * kns * jjs;
                                }
                            }
                        }
                    }

                    //if j is odd
                    else {
                        //compute first double sum where s: (j-1)/2 -> min(j-1,N)-1 and n: s+1 -> min(j-1,N)
                        for (int s = (j - 1) / 2; s <= FastMath.min(minjm1on - 1, sMax); s++) {
                            // j - s
                            final int jms = j - s;
                            // Kronecker symbols (2 - delta(0,s-j)) and (2 - delta(0,j-s))
                            final int d0smj = (s == j) ? 1 : 2;

                            for (int n = s + 1; n <= minjm1on; n++) {
                                // if n + (j-s) is odd, then the term is equal to zero due to the factor Vn,s-j
                                if ((n + jms) % 2 == 0) {
                                    // (2 - delta(0,j-s)) * J<sub>n</sub> * K₀<sup>-n-1,s</sup> * L<sub>n</sub><sup>j-s</sup>
                                    final double lns = lnsCoef.getLns(n, jms);
                                    final double dlns = lnsCoef.getdLnsdGamma(n, jms);

                                    final double ijs = ghijCoef.getIjs(s, jms);
                                    final double dIjsdh = ghijCoef.getdIjsdh(s, jms);
                                    final double dIjsdk = ghijCoef.getdIjsdk(s, jms);
                                    final double dIjsdAlpha = ghijCoef.getdIjsdAlpha(s, jms);
                                    final double dIjsdBeta = ghijCoef.getdIjsdBeta(s, jms);

                                    final double jjs = ghijCoef.getJjs(s, jms);
                                    final double dJjsdh = ghijCoef.getdJjsdh(s, jms);
                                    final double dJjsdk = ghijCoef.getdJjsdk(s, jms);
                                    final double dJjsdAlpha = ghijCoef.getdJjsdAlpha(s, jms);
                                    final double dJjsdBeta = ghijCoef.getdJjsdBeta(s, jms);

                                    // J<sub>n</sub>
                                    final double jn = -harmonics.getUnnormalizedCnm(n, 0);

                                    // K₀<sup>-n-1,s</sup>

                                    final double kns = hansenObjects[s].getValue(-n - 1, X);
                                    final double dkns  = hansenObjects[s].getDerivative(-n - 1, X);

                                    final double coef0 = d0smj * jn;
                                    final double coef1 = coef0 * lns;
                                    final double coef2 = coef1 * kns;

                                    final double coef3 = coef2 * ijs;
                                    final double coef4 = coef2 * jjs;

                                    // Add the term to the coefficients
                                    cCoef[0][j] -= coef3;
                                    cCoef[1][j] -= coef3 * (n + 1);
                                    cCoef[2][j] -= coef1 * (kns * dIjsdh + ijs * hXXX * dkns);
                                    cCoef[3][j] -= coef1 * (kns * dIjsdk + ijs * kXXX * dkns);
                                    cCoef[4][j] -= coef2 * dIjsdAlpha;
                                    cCoef[5][j] -= coef2 * dIjsdBeta;
                                    cCoef[6][j] -= coef0 * dlns * kns * ijs;

                                    sCoef[0][j] += coef4;
                                    sCoef[1][j] += coef4 * (n + 1);
                                    sCoef[2][j] += coef1 * (kns * dJjsdh + jjs * hXXX * dkns);
                                    sCoef[3][j] += coef1 * (kns * dJjsdk + jjs * kXXX * dkns);
                                    sCoef[4][j] += coef2 * dJjsdAlpha;
                                    sCoef[5][j] += coef2 * dJjsdBeta;
                                    sCoef[6][j] += coef0 * dlns * kns * jjs;
                                }
                            }
                        }

                        //the second double sum is added only if N >= 4 and j between 5 and 2*N-3
                        if (nMax >= 4 && isBetween(j, 5, 2 * nMax - 3)) {
                            //compute second double sum where s: j-min(j-1,N) -> (j-3)/2 and n: j-s -> min(j-1,N)
                            for (int s = j - minjm1on; s <= FastMath.min((j - 3) / 2, sMax); s++) {
                                // j - s
                                final int jms = j - s;
                                // Kronecker symbols (2 - delta(0,s-j)) and (2 - delta(0,j-s))
                                final int d0smj = (s == j) ? 1 : 2;

                                for (int n = j - s; n <= minjm1on; n++) {
                                    // if n + (j-s) is odd, then the term is equal to zero due to the factor Vn,s-j
                                    if ((n + jms) % 2 == 0) {
                                        // (2 - delta(0,j-s)) * J<sub>n</sub> * K₀<sup>-n-1,s</sup> * L<sub>n</sub><sup>j-s</sup>
                                        final double lns = lnsCoef.getLns(n, jms);
                                        final double dlns = lnsCoef.getdLnsdGamma(n, jms);

                                        final double ijs = ghijCoef.getIjs(s, jms);
                                        final double dIjsdh = ghijCoef.getdIjsdh(s, jms);
                                        final double dIjsdk = ghijCoef.getdIjsdk(s, jms);
                                        final double dIjsdAlpha = ghijCoef.getdIjsdAlpha(s, jms);
                                        final double dIjsdBeta = ghijCoef.getdIjsdBeta(s, jms);

                                        final double jjs = ghijCoef.getJjs(s, jms);
                                        final double dJjsdh = ghijCoef.getdJjsdh(s, jms);
                                        final double dJjsdk = ghijCoef.getdJjsdk(s, jms);
                                        final double dJjsdAlpha = ghijCoef.getdJjsdAlpha(s, jms);
                                        final double dJjsdBeta = ghijCoef.getdJjsdBeta(s, jms);

                                        // J<sub>n</sub>
                                        final double jn = -harmonics.getUnnormalizedCnm(n, 0);

                                        // K₀<sup>-n-1,s</sup>
                                        final double kns   = hansenObjects[s].getValue(-n - 1, X);
                                        final double dkns  = hansenObjects[s].getDerivative(-n - 1, X);

                                        final double coef0 = d0smj * jn;
                                        final double coef1 = coef0 * lns;
                                        final double coef2 = coef1 * kns;

                                        final double coef3 = coef2 * ijs;
                                        final double coef4 = coef2 * jjs;

                                        // Add the term to the coefficients
                                        cCoef[0][j] -= coef3;
                                        cCoef[1][j] -= coef3 * (n + 1);
                                        cCoef[2][j] -= coef1 * (kns * dIjsdh + ijs * hXXX * dkns);
                                        cCoef[3][j] -= coef1 * (kns * dIjsdk + ijs * kXXX * dkns);
                                        cCoef[4][j] -= coef2 * dIjsdAlpha;
                                        cCoef[5][j] -= coef2 * dIjsdBeta;
                                        cCoef[6][j] -= coef0 * dlns * kns * ijs;

                                        sCoef[0][j] += coef4;
                                        sCoef[1][j] += coef4 * (n + 1);
                                        sCoef[2][j] += coef1 * (kns * dJjsdh + jjs * hXXX * dkns);
                                        sCoef[3][j] += coef1 * (kns * dJjsdk + jjs * kXXX * dkns);
                                        sCoef[4][j] += coef2 * dJjsdAlpha;
                                        sCoef[5][j] += coef2 * dJjsdBeta;
                                        sCoef[6][j] += coef0 * dlns * kns * jjs;
                                    }
                                }
                            }
                        }
                    }
                }

                cCoef[0][j] *= -muoa / j;
                cCoef[1][j] *=  muoa / ( j * a );
                cCoef[2][j] *= -muoa / j;
                cCoef[3][j] *= -muoa / j;
                cCoef[4][j] *= -muoa / j;
                cCoef[5][j] *= -muoa / j;
                cCoef[6][j] *= -muoa / j;

                sCoef[0][j] *= -muoa / j;
                sCoef[1][j] *=  muoa / ( j * a );
                sCoef[2][j] *= -muoa / j;
                sCoef[3][j] *= -muoa / j;
                sCoef[4][j] *= -muoa / j;
                sCoef[5][j] *= -muoa / j;
                sCoef[6][j] *= -muoa / j;

            }
        }

        /** Check if an index is within the accepted interval.
         *
         * @param index the index to check
         * @param lowerBound the lower bound of the interval
         * @param upperBound the upper bound of the interval
         * @return true if the index is between the lower and upper bounds, false otherwise
         */
        private boolean isBetween(final int index, final int lowerBound, final int upperBound) {
            return index >= lowerBound && index <= upperBound;
        }

        /**Get the value of C<sup>j</sup>.
         *
         * @param j j index
         * @return C<sup>j</sup>
         */
        public double getCj(final int j) {
            return cCoef[0][j];
        }

        /**Get the value of dC<sup>j</sup> / da.
         *
         * @param j j index
         * @return dC<sup>j</sup> / da
         */
        public double getdCjdA(final int j) {
            return cCoef[1][j];
        }

        /**Get the value of dC<sup>j</sup> / dh.
         *
         * @param j j index
         * @return dC<sup>j</sup> / dh
         */
        public double getdCjdH(final int j) {
            return cCoef[2][j];
        }

        /**Get the value of dC<sup>j</sup> / dk.
         *
         * @param j j index
         * @return dC<sup>j</sup> / dk
         */
        public double getdCjdK(final int j) {
            return cCoef[3][j];
        }

        /**Get the value of dC<sup>j</sup> / dα.
         *
         * @param j j index
         * @return dC<sup>j</sup> / dα
         */
        public double getdCjdAlpha(final int j) {
            return cCoef[4][j];
        }

        /**Get the value of dC<sup>j</sup> / dβ.
         *
         * @param j j index
         * @return dC<sup>j</sup> / dβ
         */
        public double getdCjdBeta(final int j) {
            return cCoef[5][j];
        }

        /**Get the value of dC<sup>j</sup> / dγ.
         *
         * @param j j index
         * @return dC<sup>j</sup> / dγ
         */
        public double getdCjdGamma(final int j) {
            return cCoef[6][j];
        }

        /**Get the value of S<sup>j</sup>.
         *
         * @param j j index
         * @return S<sup>j</sup>
         */
        public double getSj(final int j) {
            return sCoef[0][j];
        }

        /**Get the value of dS<sup>j</sup> / da.
         *
         * @param j j index
         * @return dS<sup>j</sup> / da
         */
        public double getdSjdA(final int j) {
            return sCoef[1][j];
        }

        /**Get the value of dS<sup>j</sup> / dh.
         *
         * @param j j index
         * @return dS<sup>j</sup> / dh
         */
        public double getdSjdH(final int j) {
            return sCoef[2][j];
        }

        /**Get the value of dS<sup>j</sup> / dk.
         *
         * @param j j index
         * @return dS<sup>j</sup> / dk
         */
        public double getdSjdK(final int j) {
            return sCoef[3][j];
        }

        /**Get the value of dS<sup>j</sup> / dα.
         *
         * @param j j index
         * @return dS<sup>j</sup> / dα
         */
        public double getdSjdAlpha(final int j) {
            return sCoef[4][j];
        }

        /**Get the value of dS<sup>j</sup> / dβ.
         *
         * @param j j index
         * @return dS<sup>j</sup> / dβ
         */
        public double getdSjdBeta(final int j) {
            return sCoef[5][j];
        }

        /**Get the value of dS<sup>j</sup> /  dγ.
         *
         * @param j j index
         * @return dS<sup>j</sup> /  dγ
         */
        public double getdSjdGamma(final int j) {
            return sCoef[6][j];
        }
    }
}
