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
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.analysis.polynomials.PolynomialsUtils;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.MathUtils;
import org.orekit.errors.OrekitException;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.propagation.semianalytical.dsst.utilities.CoefficientsFactory;
import org.orekit.propagation.semianalytical.dsst.utilities.CoefficientsFactory.MNSKey;
import org.orekit.propagation.semianalytical.dsst.utilities.GHmsjPolynomials;
import org.orekit.propagation.semianalytical.dsst.utilities.GammaMnsFunction;
import org.orekit.propagation.semianalytical.dsst.utilities.NewcombOperators;
import org.orekit.propagation.semianalytical.dsst.utilities.ResonantCouple;
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

    /** Set of resonant tesseral harmonic couples. */
    private final List<ResonantCouple> resCouple;

    /** Factorial. */
    private final double[] fact;

    /** Maximum power of the eccentricity to use in Hansen coefficient Kernel expansion. */
    private int maxEc2Pow;

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
        this.resCouple = new ArrayList<ResonantCouple>();
        this.maxEc2Pow = 0;

        // Factorials computation
        final int maxFact = maxDegree + provider.getMaxOrder() + 1;
        this.fact = new double[maxFact];
        fact[0] = 1;
        for (int i = 1; i < maxFact; i++) {
            fact[i] = i * fact[i - 1];
        }

    }

    /** Set the resonant couples (n, m).
     *  <p>
     *  If the set is null or empty, the resonant couples will be automatically computed.
     *  If it is not null nor empty, only these resonant couples will be taken in account.
     *  </p>
     *  @param resonantTesseral Set of resonant couples
     */
    public void setResonantCouples(final Set<ResonantCouple> resonantTesseral) {
        if (resonantTesseral != null && !resonantTesseral.isEmpty()) {
            for (final ResonantCouple couple : resonantTesseral) {
                resCouple.add(couple);
            }
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
        int maxEccPow = 0;
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
        maxEc2Pow = maxEccPow / 2;

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
        final double[] dU  = computeUDerivatives(provider.getOffset(spacecraftState.getDate()));
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

    /** Get the resonant tesseral terms in the central body spherical harmonic field.
     */
    private void getResonantTerms() {

        if (resCouple.isEmpty()) {
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
                    // Store each resonant couple for a given order
                    for (int n = m; n <= maxDegree; n++) {
                        resCouple.add(new ResonantCouple(n, m));
                    }
                }
            }
        } else {
            // Get user defined resonant terms
            for (ResonantCouple couple : resCouple) {
                resOrders.add(couple.getM());
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
     *  @param dateOffset offset between current date and gravity field reference date
     *  @return potential derivatives
     *  @throws OrekitException if an error occurs
     */
    private double[] computeUDerivatives(final double dateOffset) throws OrekitException {

        // Gmsj and Hmsj polynomials
        final GHmsjPolynomials ghMSJ = new GHmsjPolynomials(k, h, alpha, beta, I);

        // GAMMAmns function
        final GammaMnsFunction gammaMNS = new GammaMnsFunction(fact, gamma, I);

        // Hansen coefficients
        final HansenTesseral hansen = new HansenTesseral(ecc, maxEc2Pow);

        // R / a up to power degree
        final double[] roaPow = new double[maxDegree + 1];
        roaPow[0] = 1.;
        for (int i = 1; i <= maxDegree; i++) {
            roaPow[i] = roa * roaPow[i - 1];
        }

        // Potential derivatives
        double dUda  = 0.;
        double dUdh  = 0.;
        double dUdk  = 0.;
        double dUdl  = 0.;
        double dUdAl = 0.;
        double dUdBe = 0.;
        double dUdGa = 0.;

        // SUM over resonant terms {j,m}
        for (int m : resOrders) {
            final int j = FastMath.max(1, (int) FastMath.round(ratio * m));
            final double[] potential = computeUsnDerivatives(dateOffset, j, m, roaPow, ghMSJ, gammaMNS, hansen);
            dUda  += potential[0];
            dUdh  += potential[1];
            dUdk  += potential[2];
            dUdl  += potential[3];
            dUdAl += potential[4];
            dUdBe += potential[5];
            dUdGa += potential[6];
        }

        dUda  *= -moa / a;
        dUdh  *=  moa;
        dUdk  *=  moa;
        dUdl  *=  moa;
        dUdAl *=  moa;
        dUdBe *=  moa;
        dUdGa *=  moa;

        return new double[] {dUda, dUdh, dUdk, dUdl, dUdAl, dUdBe, dUdGa};
    }

    /** Compute potential derivatives for each resonant tesseral term.
     *  @param dateOffset offset between current date and gravity field reference date
     *  @param j resonant index <i>j</i>
     *  @param m resonant order <i>m</i>
     *  @param roaPow powers of R/a up to degree
     *  @param ghMSJ G<sup>j</sup><sub>m,s</sub> and H<sup>j</sup><sub>m,s</sub> polynomials
     *  @param gammaMNS &Gamma;<sup>m</sup><sub>n,s</sub>(&gamma;) function
     *  @param hansen Hansen coefficients
     *  @return U<sub>s,n</sub> derivatives
     * @throws OrekitException if some error occurred
     */
    private double[] computeUsnDerivatives(final double dateOffset,
                                           final int j, final int m,
                                           final double[] roaPow,
                                           final GHmsjPolynomials ghMSJ,
                                           final GammaMnsFunction gammaMNS,
                                           final HansenTesseral hansen) throws OrekitException {

        // Initialize potential derivatives
        double dUda  = 0.;
        double dUdh  = 0.;
        double dUdk  = 0.;
        double dUdl  = 0.;
        double dUdAl = 0.;
        double dUdBe = 0.;
        double dUdGa = 0.;

        // Resonance angle
        final double jlMmt  = j * lm - m * theta;
        final double sinPhi = FastMath.sin(jlMmt);
        final double cosPhi = FastMath.cos(jlMmt);

        // I^m
        final int Im = I > 0 ? 1 : (m % 2 == 0 ? 1 : -1);

        // s-SUM from -N to N
        for (int s = -maxDegree; s <= maxDegree; s++) {

            // jacobi v, w, indices from 2.7.1-(15)
            final int v = FastMath.abs(m - s);
            final int w = FastMath.abs(m + s);

            // n-SUM from (Max(2, m, |s|)) to N
            final int nmin = FastMath.max(FastMath.max(2, m), FastMath.abs(s));
            for (int n = nmin; n <= maxDegree; n++) {
                // If (n - s) is odd, the contribution is null because of Vmns
                if ((n - s) % 2 == 0) {
                    final double fns    = fact[n + FastMath.abs(s)];
                    final double vMNS   = CoefficientsFactory.getVmns(m, n, s, fns, fact[n - m]);

                    final double gaMNS  = gammaMNS.getValue(m, n, s);
                    final double dGaMNS = gammaMNS.getDerivative(m, n, s);

                    final double kJNS   = hansen.getValue(j, -n - 1, s);
                    final double dkJNS  = hansen.getDerivative(j, -n - 1, s);

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
                    final PolynomialFunction jacobiPoly = PolynomialsUtils.createJacobiPolynomial(l, v, w);
                    final double jacobi  = jacobiPoly.value(gamma);
                    final double dJacobi = jacobiPoly.derivative().value(gamma);

                    final double cnm = provider.getUnnormalizedCnm(dateOffset, n, m);
                    final double snm = provider.getUnnormalizedSnm(dateOffset, n, m);

                    // Common factors
                    final double cf_0 = roaPow[n] * Im * vMNS;
                    final double cf_1 = cf_0 * gaMNS * jacobi;
                    final double cf_2 = cf_1 * kJNS;
                    final double gcPhs = gMSJ * cnm + hMSJ * snm;
                    final double gsMhc = gMSJ * snm - hMSJ * cnm;
                    final double dKgcPhsx2 = 2. * dkJNS * gcPhs;
                    final double dKgsMhcx2 = 2. * dkJNS * gsMhc;

                    // Compute dU / da from expansion of equation (4-a)
                    double realCosFactor = gcPhs * cosPhi;
                    double realSinFactor = gsMhc * sinPhi;
                    dUda += (n + 1) * cf_2 * (realCosFactor + realSinFactor);

                    // Compute dU / dh from expansion of equation (4-b)
                    realCosFactor = (kJNS * (cnm * dGdh + snm * dHdh) + h * dKgcPhsx2) * cosPhi;
                    realSinFactor = (kJNS * (snm * dGdh - cnm * dHdh) + h * dKgsMhcx2) * sinPhi;
                    dUdh += cf_1 * (realCosFactor + realSinFactor);

                    // Compute dU / dk from expansion of equation (4-c)
                    realCosFactor = (kJNS * (cnm * dGdk + snm * dHdk) + k * dKgcPhsx2) * cosPhi;
                    realSinFactor = (kJNS * (snm * dGdk - cnm * dHdk) + k * dKgsMhcx2) * sinPhi;
                    dUdk += cf_1 * (realCosFactor + realSinFactor);

                    // Compute dU / dLambda from expansion of equation (4-d)
                    realCosFactor = gsMhc * cosPhi;
                    realSinFactor = gcPhs * sinPhi;
                    dUdl += j * cf_2 * (realCosFactor - realSinFactor);

                    // Compute dU / alpha from expansion of equation (4-e)
                    realCosFactor = (dGdA * cnm + dHdA * snm) * cosPhi;
                    realSinFactor = (dGdA * snm - dHdA * cnm) * sinPhi;
                    dUdAl += cf_2 * (realCosFactor + realSinFactor);

                    // Compute dU / dBeta from expansion of equation (4-f)
                    realCosFactor = (dGdB * cnm + dHdB * snm) * cosPhi;
                    realSinFactor = (dGdB * snm - dHdB * cnm) * sinPhi;
                    dUdBe += cf_2 * (realCosFactor + realSinFactor);

                    // Compute dU / dGamma from expansion of equation (4-g)
                    realCosFactor = gcPhs * cosPhi;
                    realSinFactor = gsMhc * sinPhi;
                    dUdGa += cf_0 * kJNS * (jacobi * dGaMNS + gaMNS * dJacobi) * (realCosFactor + realSinFactor);
                }
            }
        }

        return new double[] {dUda, dUdh, dUdk, dUdl, dUdAl, dUdBe, dUdGa};
    }

    /** Hansen coefficients for tesseral contribution to central body force model.
     *  <p>
     *  Hansen coefficients are functions of the eccentricity.
     *  For a given eccentricity, the computed elements are stored in a map.
     *  </p>
     */
    private static class HansenTesseral {

        /** Map to store every Hansen coefficient computed. */
        private TreeMap<MNSKey, Double> coefficients;

        /** Map to store every Hansen coefficient derivatives computed. */
        private TreeMap<MNSKey, Double> derivatives;

        /** Eccentricity. */
        private final double e2;

        /** 1 - e<sup>2</sup>. */
        private final double ome2;

        /** &chi; = 1 / sqrt(1- e<sup>2</sup>). */
        private final double chi;

        /** &chi;<sup>2</sup>. */
        private final double chi2;

        /** Maximum power of e<sup>2</sup> to use in series expansion for the Hansen coefficient when
         * using modified Newcomb operator.
         */
        private final int    maxE2Power;

        /** Simple constructor.
         *  @param ecc eccentricity
         *  @param maxE2Power maximum power of e<sup>2</sup> to use in series expansion for the Hansen coefficient
         */
        public HansenTesseral(final double ecc, final int maxE2Power) {
            this.coefficients = new TreeMap<CoefficientsFactory.MNSKey, Double>();
            this.derivatives  = new TreeMap<CoefficientsFactory.MNSKey, Double>();
            this.maxE2Power   = maxE2Power;
            this.e2   = ecc * ecc;
            this.ome2 = 1. - e2;
            this.chi  = 1. / FastMath.sqrt(ome2);
            this.chi2 = chi * chi;
        }

        /** Get the K<sub>j</sub><sup>-n-1,s</sup> coefficient value.
         * @param j j value
         * @param mnm1 -n-1 value
         * @param s s value
         * @return K<sub>j</sub><sup>-n-1,s</sup>
         */
        public double getValue(final int j, final int mnm1, final int s) {
            if (coefficients.containsKey(new MNSKey(j, mnm1, s))) {
                // Get Kj,-n-1,s
                return coefficients.get(new MNSKey(j, mnm1, s));
            } else {
                // Compute Kj,-n-1,s
                return computeValue(j, mnm1, s);
            }
        }

        /** Get the dK<sub>j</sub><sup>-n-1,s</sup> / de<sup>2</sup> coefficient derivative.
         *  @param j j value
         *  @param mnm1 -n-1 value
         *  @param s s value
         *  @return dK<sub>j</sub><sup>-n-1,s</sup> / de<sup>2</sup>
         */
        public double getDerivative(final int j, final int mnm1, final int s) {
            if (derivatives.containsKey(new MNSKey(j, mnm1, s))) {
                // Get dKj,-n-1,s/de2
                return derivatives.get(new MNSKey(j, mnm1, s));
            } else {
                // Compute dKj,-n-1,s/de2
                return computeDerivative(j, mnm1, s);
            }
        }

        /** Compute the K<sub>j</sub><sup>-n-1,s</sup> coefficient from equation 2.7.3-(9).
         *
         *  @param j j value
         *  @param mnm1 -n-1 value
         *  @param s s value
         *  @return K<sub>j</sub><sup>-n-1,s</sup>
         */
        private double computeValue(final int j, final int mnm1, final int s) {
            final int n = -mnm1 - 1;
            double result = 0;
            if ((n == 3) || (n == s + 1) || (n == 1 - s)) {
                // Direct computation from Newcomb operator
                result = valueFromNewcomb(j, mnm1, s);
            } else {
                // Recursive computation
                double kmn = 0.;
                if (coefficients.containsKey(new MNSKey(j, -n, s))) {
                    kmn = coefficients.get(new MNSKey(j, -n, s));
                } else {
                    kmn = valueFromNewcomb(j, -n, s);
                    coefficients.put(new MNSKey(j, -n, s), kmn);
                }
                double kmnp1 = 0.;
                if (coefficients.containsKey(new MNSKey(j, -n + 1, s))) {
                    kmnp1 = coefficients.get(new MNSKey(j, -n + 1, s));
                } else {
                    kmnp1 = valueFromNewcomb(j, -n + 1, s);
                    coefficients.put(new MNSKey(j, -n + 1, s), kmnp1);
                }
                double kmnp3 = 0.;
                if (coefficients.containsKey(new MNSKey(j, -n + 3, s))) {
                    kmnp3 = coefficients.get(new MNSKey(j, -n + 3, s));
                } else {
                    kmnp3 = valueFromNewcomb(j, -n + 3, s);
                    coefficients.put(new MNSKey(j, -n + 3, s), kmnp3);
                }

                final double den    = (3 - n) * (1 - n + s) * (1 - n - s);
                final double ck     = chi2 / den;
                final double ckmn   = (3 - n) * (1 - n) * (3 - 2 * n);
                final double ckmnp1 = (2 - n) * ((3 - n) * (1 - n) + (2 * j * s) / chi);
                final double ckmnp3 = j * j * (1 - n);
                result = ck * (ckmn * kmn - ckmnp1 * kmnp1 + ckmnp3 * kmnp3);
            }
            coefficients.put(new MNSKey(j, mnm1, s), result);
            return result;
        }

        /** Compute dK<sub>j</sub><sup>-n-1,s</sup>/de<sup>2</sup>.
         *
         *  @param j j value
         *  @param mnm1 -n-1 value
         *  @param s s value
         *  @return dK<sub>j</sub><sup>-n-1,s</sup>/de<sup>2</sup>
         */
        private double computeDerivative(final int j, final int mnm1, final int s) {
            final int n = -mnm1 - 1;
            double result = 0;
//            if ((n == 3) || (n == s + 1) || (n == 1 - s)) {
                // Direct computation from Newcomb operator
            result = derivativeFromNewcomb(j, -n - 1, s);
//            } else {
//                // Recursive computation
//                final double kmnm1 = getValue(j, -n - 1, s);
//                final double kmnp1 = getValue(j, -n + 1, s);
//
//                double dkmn = 0.;
//                if (derivatives.containsKey(new MNSKey(j, -n, s))) {
//                    dkmn = derivatives.get(new MNSKey(j, -n, s));
//                } else {
//                    dkmn = derivativeFromNewcomb(j, -n, s);
//                    derivatives.put(new MNSKey(j, -n, s), dkmn);
//                }
//                double dkmnp1 = 0.;
//                if (derivatives.containsKey(new MNSKey(j, -n + 1, s))) {
//                    dkmnp1 = derivatives.get(new MNSKey(j, -n + 1, s));
//                } else {
//                    dkmnp1 = derivativeFromNewcomb(j, -n + 1, s);
//                    derivatives.put(new MNSKey(j, -n + 1, s), dkmnp1);
//                }
//                double dkmnp3 = 0.;
//                if (derivatives.containsKey(new MNSKey(j, -n + 3, s))) {
//                    dkmnp3 = derivatives.get(new MNSKey(j, -n + 3, s));
//                } else {
//                    dkmnp3 = derivativeFromNewcomb(j, -n + 3, s);
//                    derivatives.put(new MNSKey(j, -n + 3, s), dkmnp3);
//                }
//
//                final double den     = (3 - n) * (1 - n + s) * (1 - n - s);
//                final double cdk     = chi2 / den;
//                final double cdkmn   = (3 - n) * (1 - n) * (3 - 2 * n);
//                final double cdkmnp1 = (2 - n) * ((3 - n) * (1 - n) + (2 * j * s) / chi);
//                final double cdkmnp3 = j * j * (1 - n);
//                result = cdk * (cdkmn * dkmn - cdkmnp1 * dkmnp1 + cdkmnp3 * dkmnp3)
//                        + 0.5 * chi * (kmnm1 + (2 - n) * 2 * j * s * kmnp1 / den);
//            }
            derivatives.put(new MNSKey(j, mnm1, s), result);
            return result;
        }

        /** Compute the K<sub>j</sub><sup>-n-1,s</sup> from equation 2.7.3-(10).<br>
         *  <p>
         *  The coefficient value is evaluated from the {@link NewcombOperators} elements.
         *  </p>
         *  @param j j value
         *  @param mnm1 -n-1 value
         *  @param s s value
         *  @return K<sub>j</sub><sup>-n-1,s</sup>
         */
        private double valueFromNewcomb(final int j, final int mnm1, final int s) {
            // Initialization
            final int a = FastMath.max(j - s, 0);
            final int b = FastMath.max(s - j, 0);

            // Expansion until the maximum power in e^2 is reached
            // e^2*alpha
            double eP2a = 1.;
            double sum  = 0.;
            for (int alpha = 0; alpha <= maxE2Power; alpha++) {
                final double newcomb = NewcombOperators.getValue(alpha + a, alpha + b, mnm1, s);
                sum += newcomb * eP2a;
                eP2a *= e2;
            }

            return FastMath.pow(ome2, mnm1 + 1.5) * sum;
        }

        /** Compute dK<sub>j</sub><sup>-n-1,s</sup>/de<sup>2</sup> from equation 3.3-(5).
         *  <p>
         *  The derivative value is evaluated from the {@link NewcombOperators} elements.
         *  </p>
         *  @param j j value
         *  @param mnm1 -n-1 value
         *  @param s s value
         *  @return dK<sub>j</sub><sup>-n-1,s</sup>/de<sup>2</sup>
         */
        private double derivativeFromNewcomb(final int j, final int mnm1, final int s) {
            // Initialization
            final int a = FastMath.max(j - s, 0);
            final int b = FastMath.max(s - j, 0);

            // Expansion until the maximum power in e^2 is reached
            // e^2(alpha-1)
            double e2Pam1 = 1.;
            double sum    = 0.;
            for (int alpha = 1; alpha <= maxE2Power; alpha++) {
                final double newcomb = NewcombOperators.getValue(alpha + a, alpha + b, mnm1, s);
                sum += alpha * newcomb * e2Pam1;
                e2Pam1 *= e2;
            }

            final double coef = mnm1 + 1.5;
            final double Kjns = getValue(j, mnm1, s);

            return FastMath.pow(ome2, coef) * sum - coef * chi2 * Kjns;
        }
    }
}
