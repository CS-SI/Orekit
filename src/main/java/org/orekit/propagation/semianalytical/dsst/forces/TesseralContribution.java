/* Copyright 2002-2013 CS Systèmes d'Information
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

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.MathUtils;
import org.orekit.errors.OrekitException;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider.UnnormalizedSphericalHarmonics;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.propagation.semianalytical.dsst.utilities.CoefficientsFactory;
import org.orekit.propagation.semianalytical.dsst.utilities.CoefficientsFactory.MNSKey;
import org.orekit.propagation.semianalytical.dsst.utilities.GHmsjPolynomials;
import org.orekit.propagation.semianalytical.dsst.utilities.GammaMnsFunction;
import org.orekit.propagation.semianalytical.dsst.utilities.JacobiPolynomials;
import org.orekit.propagation.semianalytical.dsst.utilities.NewcombOperators;
import org.orekit.time.AbsoluteDate;

/** Tesseral contribution to the {@link DSSTCentralBody central body gravitational
 *  perturbation}.
 *  <p>
 *  Only resonant tesserals are considered.
 *  </p>
 *
 *  @author Romain Di Costanzo
 *  @author Pascal Parraud
 */
class TesseralContribution implements DSSTForceModel {

    /** Minimum period for analytically averaged high-order resonant
     *  central body spherical harmonics in seconds.
     */
    private static final double MIN_PERIOD_IN_SECONDS = 864000.;

    /** Minimum period for analytically averaged high-order resonant
     *  central body spherical harmonics in satellite revolutions.
     */
    private static final double MIN_PERIOD_IN_SAT_REV = 10.;

    /** Newcomb operators. */
    private final NewcombOperators newcomb;

    /** Jacobi polynomials. */
    private final JacobiPolynomials jacobiPoly;

    /** Provider for spherical harmonics. */
    private final UnnormalizedSphericalHarmonicsProvider provider;

    /** Central body rotating frame. */
    private final Frame bodyFrame;

    /** Central body rotation period (seconds). */
    private final double bodyPeriod;

    /** Maximal degree to consider for harmonics potential. */
    private final int maxDegree;

    /** List of resonant orders. */
    private final List<Integer> resOrders;

    /** Factorial. */
    private final double[] fact;

    /** Maximum power of the eccentricity to use in summation over s. */
    private int maxEccPow;

    /** Maximum power of the eccentricity to use in Hansen coefficient Kernel expansion. */
    private int maxHansen;

    /** Keplerian period. */
    private double orbitPeriod;

    /** Ratio of satellite period to central body rotation period. */
    private double ratio;

    /** Retrograde factor. */
    private int    I;

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
    /** lm. */
    private double lm;

    /** Eccentricity. */
    private double ecc;

    // Equinoctial reference frame vectors (according to DSST notation)
    /** Equinoctial frame f vector. */
    private Vector3D f;
    /** Equinoctial frame g vector. */
    private Vector3D g;

    /** Central body rotation angle &theta;. */
    private double theta;

    /** Direction cosine &alpha;. */
    private double alpha;
    /** Direction cosine &beta;. */
    private double beta;
    /** Direction cosine &gamma;. */
    private double gamma;

    // Common factors from equinoctial coefficients
    /** 2 * a / A .*/
    private double ax2oA;
    /** 1 / (A * B) .*/
    private double ooAB;
    /** B / A .*/
    private double BoA;
    /** B / (A * (1 + B)) .*/
    private double BoABpo;
    /** C / (2 * A * B) .*/
    private double Co2AB;
    /** &mu; / a .*/
    private double moa;
    /** R / a .*/
    private double roa;

    /** Single constructor.
     *  @param centralBodyFrame rotating body frame
     *  @param centralBodyRotationRate central body rotation rate (rad/s)
     *  @param provider provider for spherical harmonics
     */
    public TesseralContribution(final Frame centralBodyFrame,
                                final double centralBodyRotationRate,
                                final UnnormalizedSphericalHarmonicsProvider provider) {

        // Central body rotating frame
        this.bodyFrame = centralBodyFrame;

        // Central body rotation period in seconds
        this.bodyPeriod = MathUtils.TWO_PI / centralBodyRotationRate;

        // Provider for spherical harmonics
        this.provider  = provider;
        this.maxDegree = provider.getMaxDegree();

        // Initialize default values
        this.resOrders = new ArrayList<Integer>();
        this.maxEccPow = 0;
        this.maxHansen = 0;

        // Provider for Newcomb operators
        this.newcomb  = new NewcombOperators();

        // Provider for Jacobi polynomials
        this.jacobiPoly = new JacobiPolynomials();

       // Factorials computation
        final int maxFact = 2 * maxDegree + 1;
        this.fact = new double[maxFact];
        fact[0] = 1;
        for (int i = 1; i < maxFact; i++) {
            fact[i] = i * fact[i - 1];
        }

    }

    /** {@inheritDoc} */
    public void initialize(final AuxiliaryElements aux)
        throws OrekitException {

        // Keplerian period
        orbitPeriod = aux.getKeplerianPeriod();

        // Ratio of satellite to central body periods to define resonant terms
        ratio = orbitPeriod / bodyPeriod;

        // Compute the resonant tesseral harmonic terms if not set by the user
        getResonantTerms();

        // Set the highest power of the eccentricity in the analytical power
        // series expansion for the averaged high order resonant central body
        // spherical harmonic perturbation
        final double e = aux.getEcc();
        if (e <= 0.005) {
            maxEccPow = 3;
        } else if (e <= 0.02) {
            maxEccPow = 4;
        } else if (e <= 0.1) {
            maxEccPow = 7;
        } else if (e <= 0.2) {
            maxEccPow = 10;
        } else if (e <= 0.3) {
            maxEccPow = 12;
        } else if (e <= 0.4) {
            maxEccPow = 15;
        } else {
            maxEccPow = 20;
        }

        // Set the maximum power of the eccentricity to use in Hansen coefficient Kernel expansion.
        maxHansen = maxEccPow / 2;

    }

    /** {@inheritDoc} */
    public void initializeStep(final AuxiliaryElements aux) throws OrekitException {

        // Equinoctial elements
        a  = aux.getSma();
        k  = aux.getK();
        h  = aux.getH();
        q  = aux.getQ();
        p  = aux.getP();
        lm = aux.getLM();

        // Eccentricity
        ecc = aux.getEcc();

        // Retrograde factor
        I = aux.getRetrogradeFactor();

        // Equinoctial frame vectors
        f = aux.getVectorF();
        g = aux.getVectorG();

        // Central body rotation angle from equation 2.7.1-(3)(4).
        final Transform t = bodyFrame.getTransformTo(aux.getFrame(), aux.getDate());
        final Vector3D xB = t.transformVector(Vector3D.PLUS_I);
        final Vector3D yB = t.transformVector(Vector3D.PLUS_J);
        theta = FastMath.atan2(-f.dotProduct(yB) + I * g.dotProduct(xB),
                                f.dotProduct(xB) + I * g.dotProduct(yB));

        // Direction cosines
        alpha = aux.getAlpha();
        beta  = aux.getBeta();
        gamma = aux.getGamma();

        // Equinoctial coefficients
        // A = sqrt(&mu; * a)
        final double A = aux.getA();
        // B = sqrt(1 - h<sup>2</sup> - k<sup>2</sup>)
        final double B = aux.getB();
        // C = 1 + p<sup>2</sup> + q<sup>2</sup>
        final double C = aux.getC();
        // Common factors from equinoctial coefficients
        // a / A
        ax2oA  = 2. * a / A;
        // B / A
        BoA  = B / A;
        // 1 / AB
        ooAB = 1. / (A * B);
        // C / 2AB
        Co2AB = C * ooAB / 2.;
        // B / (A * (1 + B))
        BoABpo = BoA / (1. + B);
        // &mu / a
        moa = provider.getMu() / a;
        // R / a
        roa = provider.getAe() / a;

    }

    /** {@inheritDoc} */
    public double[] getMeanElementRate(final SpacecraftState spacecraftState) throws OrekitException {

        // Compute potential derivatives
        final double[] dU  = computeUDerivatives(spacecraftState.getDate());
        final double dUda  = dU[0];
        final double dUdh  = dU[1];
        final double dUdk  = dU[2];
        final double dUdl  = dU[3];
        final double dUdAl = dU[4];
        final double dUdBe = dU[5];
        final double dUdGa = dU[6];

        // Compute the cross derivative operator :
        final double UAlphaGamma   = alpha * dUdGa - gamma * dUdAl;
        final double UAlphaBeta    = alpha * dUdBe - beta  * dUdAl;
        final double UBetaGamma    =  beta * dUdGa - gamma * dUdBe;
        final double Uhk           =     h * dUdk  -     k * dUdh;
        final double pUagmIqUbgoAB = (p * UAlphaGamma - I * q * UBetaGamma) * ooAB;
        final double UhkmUabmdUdl  =  Uhk - UAlphaBeta - dUdl;

        final double da =  ax2oA * dUdl;
        final double dh =    BoA * dUdk + k * pUagmIqUbgoAB - h * BoABpo * dUdl;
        final double dk =  -(BoA * dUdh + h * pUagmIqUbgoAB + k * BoABpo * dUdl);
        final double dp =  Co2AB * (p * UhkmUabmdUdl - UBetaGamma);
        final double dq =  Co2AB * (q * UhkmUabmdUdl - I * UAlphaGamma);
        final double dM = -ax2oA * dUda + BoABpo * (h * dUdh + k * dUdk) + pUagmIqUbgoAB;

        return new double[] {da, dk, dh, dq, dp, dM};
    }

    /** {@inheritDoc} */
    public double[] getShortPeriodicVariations(final AbsoluteDate date,
                                               final double[] meanElements)
        throws OrekitException {
        // TODO: not implemented yet, Short Periodic Variations are set to null
        return new double[] {0., 0., 0., 0., 0., 0.};
    }

    /** {@inheritDoc} */
    public EventDetector[] getEventsDetectors() {
        return null;
    }

    /** Get the resonant tesseral terms in the central body spherical harmonic field.
     */
    private void getResonantTerms() {

        // Compute natural resonant terms
        final double tolerance = 1. / FastMath.max(MIN_PERIOD_IN_SAT_REV,
                                                   MIN_PERIOD_IN_SECONDS / orbitPeriod);

        // Search the resonant orders in the tesseral harmonic field
        final int maxOrder = provider.getMaxOrder();
        for (int m = 1; m <= maxOrder; m++) {
            final double resonance = ratio * m;
            final int j = (int) FastMath.round(resonance);
            if (j > 0 && FastMath.abs(resonance - j) <= tolerance) {
                // Store each resonant index and order
                resOrders.add(m);
            }
        }
    }

    /** Computes the potential U derivatives.
     *  <p>The following elements are computed from expression 3.3 - (4).
     *  <pre>
     *  dU / da
     *  dU / dh
     *  dU / dk
     *  dU / d&lambda;
     *  dU / d&alpha;
     *  dU / d&beta;
     *  dU / d&gamma;
     *  </pre>
     *  </p>
     *
     *  @param date current date
     *  @return potential derivatives
     *  @throws OrekitException if an error occurs
     */
    private double[] computeUDerivatives(final AbsoluteDate date) throws OrekitException {

        // Potential derivatives
        double dUda  = 0.;
        double dUdh  = 0.;
        double dUdk  = 0.;
        double dUdl  = 0.;
        double dUdAl = 0.;
        double dUdBe = 0.;
        double dUdGa = 0.;

        // Compute only if there is at least one resonant tesseral
        if (!resOrders.isEmpty()) {
            // Gmsj and Hmsj polynomials
            final GHmsjPolynomials ghMSJ = new GHmsjPolynomials(k, h, alpha, beta, I);

            // GAMMAmns function
            final GammaMnsFunction gammaMNS = new GammaMnsFunction(fact, gamma, I);

            // Hansen coefficients
            final HansenTesseral hansen = new HansenTesseral(ecc, maxHansen);

            // R / a up to power degree
            final double[] roaPow = new double[maxDegree + 1];
            roaPow[0] = 1.;
            for (int i = 1; i <= maxDegree; i++) {
                roaPow[i] = roa * roaPow[i - 1];
            }

            // SUM over resonant terms {j,m}
            for (int m : resOrders) {

                // Resonant index for the current resonant order
                final int j = FastMath.max(1, (int) FastMath.round(ratio * m));

                // Phase angle
                final double jlMmt  = j * lm - m * theta;
                final double sinPhi = FastMath.sin(jlMmt);
                final double cosPhi = FastMath.cos(jlMmt);

                // Potential derivatives components for a given resonant pair {j,m}
                double dUdaCos  = 0.;
                double dUdaSin  = 0.;
                double dUdhCos  = 0.;
                double dUdhSin  = 0.;
                double dUdkCos  = 0.;
                double dUdkSin  = 0.;
                double dUdlCos  = 0.;
                double dUdlSin  = 0.;
                double dUdAlCos = 0.;
                double dUdAlSin = 0.;
                double dUdBeCos = 0.;
                double dUdBeSin = 0.;
                double dUdGaCos = 0.;
                double dUdGaSin = 0.;

                // s-SUM from -sMin to sMax
                final int sMin = FastMath.min(maxEccPow - j, maxDegree);
                final int sMax = FastMath.min(maxEccPow + j, maxDegree);
                for (int s = 0; s <= sMax; s++) {

                    // n-SUM for s positive
                    final double[][] nSumSpos = computeNSum(date, j, m, s,
                                                            roaPow, ghMSJ, gammaMNS, hansen);
                    dUdaCos  += nSumSpos[0][0];
                    dUdaSin  += nSumSpos[0][1];
                    dUdhCos  += nSumSpos[1][0];
                    dUdhSin  += nSumSpos[1][1];
                    dUdkCos  += nSumSpos[2][0];
                    dUdkSin  += nSumSpos[2][1];
                    dUdlCos  += nSumSpos[3][0];
                    dUdlSin  += nSumSpos[3][1];
                    dUdAlCos += nSumSpos[4][0];
                    dUdAlSin += nSumSpos[4][1];
                    dUdBeCos += nSumSpos[5][0];
                    dUdBeSin += nSumSpos[5][1];
                    dUdGaCos += nSumSpos[6][0];
                    dUdGaSin += nSumSpos[6][1];

                    // n-SUM for s negative
                    if (s > 0 && s <= sMin) {
                        final double[][] nSumSneg = computeNSum(date, j, m, -s,
                                                                roaPow, ghMSJ, gammaMNS, hansen);
                        dUdaCos  += nSumSneg[0][0];
                        dUdaSin  += nSumSneg[0][1];
                        dUdhCos  += nSumSneg[1][0];
                        dUdhSin  += nSumSneg[1][1];
                        dUdkCos  += nSumSneg[2][0];
                        dUdkSin  += nSumSneg[2][1];
                        dUdlCos  += nSumSneg[3][0];
                        dUdlSin  += nSumSneg[3][1];
                        dUdAlCos += nSumSneg[4][0];
                        dUdAlSin += nSumSneg[4][1];
                        dUdBeCos += nSumSneg[5][0];
                        dUdBeSin += nSumSneg[5][1];
                        dUdGaCos += nSumSneg[6][0];
                        dUdGaSin += nSumSneg[6][1];
                    }
                }

                // Assembly of potential derivatives componants
                dUda  += cosPhi * dUdaCos  + sinPhi * dUdaSin;
                dUdh  += cosPhi * dUdhCos  + sinPhi * dUdhSin;
                dUdk  += cosPhi * dUdkCos  + sinPhi * dUdkSin;
                dUdl  += cosPhi * dUdlCos  + sinPhi * dUdlSin;
                dUdAl += cosPhi * dUdAlCos + sinPhi * dUdAlSin;
                dUdBe += cosPhi * dUdBeCos + sinPhi * dUdBeSin;
                dUdGa += cosPhi * dUdGaCos + sinPhi * dUdGaSin;
            }

            dUda  *= -moa / a;
            dUdh  *=  moa;
            dUdk  *=  moa;
            dUdl  *=  moa;
            dUdAl *=  moa;
            dUdBe *=  moa;
            dUdGa *=  moa;
        }

        return new double[] {dUda, dUdh, dUdk, dUdl, dUdAl, dUdBe, dUdGa};
    }

    /** Compute the n-SUM for potential derivatives components.
     *  @param date current date
     *  @param j resonant index <i>j</i>
     *  @param m resonant order <i>m</i>
     *  @param s d'Alembert characteristic <i>s</i>
     *  @param roaPow powers of R/a up to degree <i>n</i>
     *  @param ghMSJ G<sup>j</sup><sub>m,s</sub> and H<sup>j</sup><sub>m,s</sub> polynomials
     *  @param gammaMNS &Gamma;<sup>m</sup><sub>n,s</sub>(&gamma;) function
     *  @param hansen Hansen coefficients
     *  @return Components of U<sub>n</sub> derivatives for fixed j, m, s
     * @throws OrekitException if some error occurred
     */
    private double[][] computeNSum(final AbsoluteDate date,
                                             final int j, final int m, final int s,
                                             final double[] roaPow,
                                             final GHmsjPolynomials ghMSJ,
                                             final GammaMnsFunction gammaMNS,
                                             final HansenTesseral hansen) throws OrekitException {

        //spherical harmonics
        final UnnormalizedSphericalHarmonics harmonics = provider.onDate(date);

        // Potential derivatives components
        double dUdaCos  = 0.;
        double dUdaSin  = 0.;
        double dUdhCos  = 0.;
        double dUdhSin  = 0.;
        double dUdkCos  = 0.;
        double dUdkSin  = 0.;
        double dUdlCos  = 0.;
        double dUdlSin  = 0.;
        double dUdAlCos = 0.;
        double dUdAlSin = 0.;
        double dUdBeCos = 0.;
        double dUdBeSin = 0.;
        double dUdGaCos = 0.;
        double dUdGaSin = 0.;

        // I^m
        final int Im = I > 0 ? 1 : (m % 2 == 0 ? 1 : -1);

        // jacobi v, w, indices from 2.7.1-(15)
        final int v = FastMath.abs(m - s);
        final int w = FastMath.abs(m + s);

        // Initialise lower degree nmin = (Max(2, m, |s|)) for summation over n
        final int nmin = FastMath.max(FastMath.max(2, m), FastMath.abs(s));
        // n max value for computing Hansen kernel from Newcomb operators
        final int nmax = nmin + 3;

        // n-SUM from nmin to N
        for (int n = nmin; n <= maxDegree; n++) {

            // Compute Hansen kernel values and derivatives
            if (n <= nmax) {
                // from Newcomb operators
                hansen.valueFromNewcomb(j, -n - 1, s);
                hansen.derivFromNewcomb(j, -n - 1, s);
            } else {
                // from recurrence relations
                hansen.valueFromRecurrence(j, -n - 1, s);
                hansen.derivFromRecurrence(j, -n - 1, s);
            }

            // If (n - s) is odd, the contribution is null because of Vmns
            if ((n - s) % 2 == 0) {

                // Vmns coefficient
                final double fns    = fact[n + FastMath.abs(s)];
                final double vMNS   = CoefficientsFactory.getVmns(m, n, s, fns, fact[n - m]);

                // Inclination function Gamma and derivative
                final double gaMNS  = gammaMNS.getValue(m, n, s);
                final double dGaMNS = gammaMNS.getDerivative(m, n, s);

                // Hansen kernel value and derivative
                final double kJNS   = hansen.getValue(j, -n - 1, s);
                final double dkJNS  = hansen.getDeriv(j, -n - 1, s);

                // Gjms, Hjms polynomials and derivatives
                final double gMSJ   = ghMSJ.getGmsj(m, s, j);
                final double hMSJ   = ghMSJ.getHmsj(m, s, j);
                final double dGdh   = ghMSJ.getdGmsdh(m, s, j);
                final double dGdk   = ghMSJ.getdGmsdk(m, s, j);
                final double dGdA   = ghMSJ.getdGmsdAlpha(m, s, j);
                final double dGdB   = ghMSJ.getdGmsdBeta(m, s, j);
                final double dHdh   = ghMSJ.getdHmsdh(m, s, j);
                final double dHdk   = ghMSJ.getdHmsdk(m, s, j);
                final double dHdA   = ghMSJ.getdHmsdAlpha(m, s, j);
                final double dHdB   = ghMSJ.getdHmsdBeta(m, s, j);

                // Jacobi l-index from 2.7.1-(15)
                final int l = FastMath.min(n - m, n - FastMath.abs(s));
                // Jacobi polynomial and derivative
                final double jacobi  = jacobiPoly.getValue(l, v , w, gamma);
                final double dJacobi = jacobiPoly.getDerivative(l, v, w, gamma);

                // Geopotential coefficients
                final double cnm = harmonics.getUnnormalizedCnm(n, m);
                final double snm = harmonics.getUnnormalizedSnm(n, m);

                // Common factors from expansion of equations 3.3-4
                final double cf_0      = roaPow[n] * Im * vMNS;
                final double cf_1      = cf_0 * gaMNS * jacobi;
                final double cf_2      = cf_1 * kJNS;
                final double gcPhs     = gMSJ * cnm + hMSJ * snm;
                final double gsMhc     = gMSJ * snm - hMSJ * cnm;
                final double dKgcPhsx2 = 2. * dkJNS * gcPhs;
                final double dKgsMhcx2 = 2. * dkJNS * gsMhc;
                final double dUdaCoef  = (n + 1) * cf_2;
                final double dUdlCoef  = j * cf_2;
                final double dUdGaCoef = cf_0 * kJNS * (jacobi * dGaMNS + gaMNS * dJacobi);

                // dU / da components
                dUdaCos  += dUdaCoef * gcPhs;
                dUdaSin  += dUdaCoef * gsMhc;

                // dU / dh components
                dUdhCos  += cf_1 * (kJNS * (cnm * dGdh + snm * dHdh) + h * dKgcPhsx2);
                dUdhSin  += cf_1 * (kJNS * (snm * dGdh - cnm * dHdh) + h * dKgsMhcx2);

                // dU / dk components
                dUdkCos  += cf_1 * (kJNS * (cnm * dGdk + snm * dHdk) + k * dKgcPhsx2);
                dUdkSin  += cf_1 * (kJNS * (snm * dGdk - cnm * dHdk) + k * dKgsMhcx2);

                // dU / dLambda components
                dUdlCos  +=  dUdlCoef * gsMhc;
                dUdlSin  += -dUdlCoef * gcPhs;

                // dU / alpha components
                dUdAlCos += cf_2 * (dGdA * cnm + dHdA * snm);
                dUdAlSin += cf_2 * (dGdA * snm - dHdA * cnm);

                // dU / dBeta components
                dUdBeCos += cf_2 * (dGdB * cnm + dHdB * snm);
                dUdBeSin += cf_2 * (dGdB * snm - dHdB * cnm);

                // dU / dGamma components
                dUdGaCos += dUdGaCoef * gcPhs;
                dUdGaSin += dUdGaCoef * gsMhc;
            }
        }

        return new double[][] {{dUdaCos,  dUdaSin},
                               {dUdhCos,  dUdhSin},
                               {dUdkCos,  dUdkSin},
                               {dUdlCos,  dUdlSin},
                               {dUdAlCos, dUdAlSin},
                               {dUdBeCos, dUdBeSin},
                               {dUdGaCos, dUdGaSin}};
    }

    /** Hansen coefficients for tesseral contribution to central body force model.
     *  <p>
     *  Hansen coefficients are functions of the eccentricity.
     *  For a given eccentricity, the computed elements are stored in a map.
     *  </p>
     */
    private class HansenTesseral {

        /** Map to store every Hansen kernel value computed. */
        private TreeMap<MNSKey, Double> values;

        /** Map to store every Hansen kernel derivative computed. */
        private TreeMap<MNSKey, Double> derivatives;

        /** Eccentricity. */
        private final double e2;

        /** 1 - e<sup>2</sup>. */
        private final double ome2;

        /** &chi; = 1 / sqrt(1- e<sup>2</sup>). */
        private final double chi;

        /** &chi;<sup>2</sup> = 1 / (1- e<sup>2</sup>). */
        private final double chi2;

        /** d&chi; / de<sup>2</sup> = &chi;<sup>3</sup> / 2. */
        private final double dchi;

        /** d&chi;<sup>2</sup> / de<sup>2</sup> = &chi;<sup>4</sup>. */
        private final double dchi2;

        /** Max power of e<sup>2</sup> in serie expansion
         *  using Newcomb operator for Hansen kernel computation.
         */
        private final int    maxNewcomb;

        /** Simple constructor.
         *  @param ecc eccentricity
         *  @param maxHansen maximum power of e<sup>2</sup> to use in series expansion for the Hansen coefficient
         */
        public HansenTesseral(final double ecc, final int maxHansen) {
            this.values      = new TreeMap<CoefficientsFactory.MNSKey, Double>();
            this.derivatives = new TreeMap<CoefficientsFactory.MNSKey, Double>();
            this.maxNewcomb  = maxHansen;
            this.e2    = ecc * ecc;
            this.ome2  = 1. - e2;
            this.chi   = 1. / FastMath.sqrt(ome2);
            this.chi2  = chi * chi;
            this.dchi  = 0.5 * chi * chi2;
            this.dchi2 = chi2 * chi2;
        }

        /** Get the K<sub>j</sub><sup>-n-1,s</sup> value.
         * @param j j value
         * @param mnm1 -n-1 value
         * @param s s value
         * @return K<sub>j</sub><sup>-n-1,s</sup>
         */
        public double getValue(final int j, final int mnm1, final int s) {
            return values.get(new MNSKey(j, mnm1, s));
        }

        /** Get the dK<sub>j</sub><sup>-n-1,s</sup> / de<sup>2</sup> derivative.
         *  @param j j value
         *  @param mnm1 -n-1 value
         *  @param s s value
         *  @return dK<sub>j</sub><sup>-n-1,s</sup> / de<sup>2</sup>
         */
        public double getDeriv(final int j, final int mnm1, final int s) {
            return derivatives.get(new MNSKey(j, mnm1, s));
        }

        /** Compute the K<sub>j</sub><sup>-n-1,s</sup> from equation 2.7.3-(10).<br>
         *  <p>
         *  The coefficient value is evaluated from the {@link NewcombOperators} elements.
         *  </p>
         *  @param j j value
         *  @param mnm1 -n-1 value
         *  @param s s value
         */
        public void valueFromNewcomb(final int j, final int mnm1, final int s) {
            // Initialization
            final int aHT = FastMath.max(j - s, 0);
            final int bHT = FastMath.max(s - j, 0);
            // Expansion until maxNewcomb, the maximum power in e^2 for the Kernel value
            double sum = newcomb.getValue(maxNewcomb + aHT, maxNewcomb + bHT, mnm1, s);
            for (int alphaHT = maxNewcomb - 1; alphaHT >= 0; alphaHT--) {
                sum *= e2;
                sum += newcomb.getValue(alphaHT + aHT, alphaHT + bHT, mnm1, s);
            }
            // Kernel value from equation 2.7.3-(10)
            final double value = FastMath.pow(chi2, -mnm1 - 1) * sum / chi;
            // Storage of the Kernel value in the map
            values.put(new MNSKey(j, mnm1, s), value);
        }

        /** Compute dK<sub>j</sub><sup>-n-1,s</sup>/de<sup>2</sup> from equation 3.3-(5).
         *  <p>
         *  The derivative value is evaluated from the {@link NewcombOperators} elements.
         *  </p>
         *  @param j j value
         *  @param mnm1 -n-1 value
         *  @param s s value
         */
        public void derivFromNewcomb(final int j, final int mnm1, final int s) {
            // Initialization
            final int aHT = FastMath.max(j - s, 0);
            final int bHT = FastMath.max(s - j, 0);
            // Expansion until maxNewcomb-1, the maximum power in e^2 for the Kernel derivative
            double sum = maxNewcomb * newcomb.getValue(maxNewcomb + aHT, maxNewcomb + bHT, mnm1, s);
            for (int alphaHT = maxNewcomb - 1; alphaHT >= 1; alphaHT--) {
                sum *= e2;
                sum += alphaHT * newcomb.getValue(alphaHT + aHT, alphaHT + bHT, mnm1, s);
            }
            // Kernel derivative from equation 3.3-(5)
            final MNSKey key  = new MNSKey(j, mnm1, s);
            final double coef = -(mnm1 + 1.5);
            final double Kjns = values.get(key);
            final double derivative = coef * chi2 * Kjns + FastMath.pow(chi2, -mnm1 - 1) * sum / chi;
            // Storage of the Kernel derivative in the map
            derivatives.put(new MNSKey(j, mnm1, s), derivative);
        }

        /** Compute the K<sub>j</sub><sup>-n-1,s</sup> coefficient from equation 2.7.3-(9).
         *
         *  @param j j value
         *  @param mnm1 -n-1 value
         *  @param s s value
         */
        public void valueFromRecurrence(final int j, final int mnm1, final int s) {
            final int n = -mnm1 - 1;
            final double kmn    = values.get(new MNSKey(j, -n, s));
            final double kmnp1  = values.get(new MNSKey(j, -n + 1, s));
            final double kmnp3  = values.get(new MNSKey(j, -n + 3, s));
            final double den    = (3 - n) * (1 - n + s) * (1 - n - s);
            final double ck     = chi2 / den;
            final double ckmn   = (3 - n) * (1 - n) * (3 - 2 * n);
            final double ckmnp1 = (2 - n) * ((3 - n) * (1 - n) + (2 * j * s) / chi);
            final double ckmnp3 = j * j * (1 - n);
            final double value  = ck * (ckmn * kmn - ckmnp1 * kmnp1 + ckmnp3 * kmnp3);
            // Store value
            values.put(new MNSKey(j, mnm1, s), value);
        }

        /** Compute dK<sub>j</sub><sup>-n-1,s</sup>/de<sup>2</sup> from derivation of equation 2.7.3-(9).
         *
         *  @param j j value
         *  @param mnm1 -n-1 value
         *  @param s s value
         */
        public void derivFromRecurrence(final int j, final int mnm1, final int s) {
            final int n = -mnm1 - 1;
            final MNSKey keymn    = new MNSKey(j, -n, s);
            final MNSKey keymnp1  = new MNSKey(j, -n + 1, s);
            final MNSKey keymnp3  = new MNSKey(j, -n + 3, s);
            final double kmn      = values.get(keymn);
            final double kmnp1    = values.get(keymnp1);
            final double kmnp3    = values.get(keymnp3);
            final double dkmn     = derivatives.get(keymn);
            final double dkmnp1   = derivatives.get(keymnp1);
            final double dkmnp3   = derivatives.get(keymnp3);
            final double den      = (3 - n) * (1 - n + s) * (1 - n - s);
            final double cdkmn    = (3 - n) * (1 - n) * (3 - 2 * n);
            final double c0dkmnp1 = (n - 2) * (3 - n) * (1 - n);
            final double c1dkmnp1 = (n - 2) * (2 * j * s);
            final double cdkmnp3  = j * j * (1 - n);
            final double deriv    = chi2  * (cdkmn * dkmn + c0dkmnp1 * dkmnp1 + cdkmnp3 * dkmnp3) +
                                    chi   * c1dkmnp1 * dkmnp1 +
                                    dchi2 * (cdkmn * kmn + c0dkmnp1 * kmnp1  + cdkmnp3 * kmnp3)  +
                                    dchi  * c1dkmnp1 * kmnp1;
            // Store derivative
            derivatives.put(new MNSKey(j, mnm1, s), deriv / den);
        }
    }
}
