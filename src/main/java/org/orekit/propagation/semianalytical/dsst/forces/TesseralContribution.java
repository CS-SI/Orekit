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

import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.analysis.polynomials.PolynomialsUtils;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.MathUtils;
import org.orekit.errors.OrekitException;
import org.orekit.forces.gravity.potential.SphericalHarmonicsProvider;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.propagation.semianalytical.dsst.utilities.CoefficientsFactory;
import org.orekit.propagation.semianalytical.dsst.utilities.CoefficientsFactory.MNSKey;
import org.orekit.propagation.semianalytical.dsst.utilities.GHmsjPolynomials;
import org.orekit.propagation.semianalytical.dsst.utilities.GammaMnsFunction;
import org.orekit.propagation.semianalytical.dsst.utilities.ModifiedNewcombOperators;
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

    /** Provider for spherical harmonics. */
    private final SphericalHarmonicsProvider provider;

    /** Maximum resonant order. */
    /** Minimum period for analytically averaged high-order resonant
     *  central body spherical harmonics in satellite revolutions.
     */
    private static final double MIN_PERIOD_IN_SAT_REV = 10.;

    /** Set of resonant index. */
    private Set<Integer> resIndexSet;

    /** Set of resonant orders. */
    private Set<Integer> resOrderSet;

    /** Set of resonant tesseral harmonic couples. */
    private Set<ResonantCouple> resCoupleSet;

    /** Highest power of the eccentricity to appear in the truncated analytical
     *  power series expansion for the averaged central-body resonant tesseral
     *  harmonics potential.
     */
    private int maxEccPow;

    /** Maximum power of the eccentricity to use in Hansen coefficient Kernel expansion. */
    private int maxHansen;

    /** Maximal integer value for the s index truncation in tesseral harmonic expansion. */
    private int minS;

    /** Minimal integer value for the N index truncation in tesseral harmonic expansion. */
    private int maxS;

    /** Central body rotation period (seconds). */
    private final double bodyPeriod;

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

    /** Keplerian period. */
    private double orbitPeriod;

    /** Ratio of satellite period to central body rotation period. */
    private double ratio;

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
    /** r / a .*/
    private double roa;

    /** Single constructor.
     * @param centralBodyRotationRate central body rotation rate (rad/s)
     * @param provider provider for spherical harmonics
     */
    public TesseralContribution(final double centralBodyRotationRate,
                                final SphericalHarmonicsProvider provider) {

        // Set the central-body rotation period
        this.bodyPeriod = MathUtils.TWO_PI / centralBodyRotationRate;
        this.provider   = provider;

        // Initialize default values
        this.resIndexSet  = new TreeSet<Integer>();
        this.resOrderSet  = new TreeSet<Integer>();
        this.resCoupleSet = new TreeSet<ResonantCouple>();
        this.maxEccPow    = 0;
        this.maxHansen    = 0;

    }

    /** Set the resonant Tesseral harmonic couple term.
     *  <p>
     *  If the list is null or empty, the resonant couples will be automatically computed.
     *  If it is not null nor empty, only these resonant couples will be taken in account.
     *  </p>
     *  @param resonantTesseral List of resonant terms
     */
    public void setResonantTesseralTerms(final Set<ResonantCouple> resonantTesseral) {

        if (resonantTesseral != null && !resonantTesseral.isEmpty()) {
            resCoupleSet = resonantTesseral;
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

        // Set the boundary values for s index to use in potential series expansion.
        minS = provider.getMaxDegree();
        maxS = provider.getMaxDegree();

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
        theta = FastMath.atan2(-f.getY() + I * g.getX(), f.getX() + I * g.getY());

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
        moa  = provider.getMu() / a;
        roa = provider.getAe() / a;

    }

    /** {@inheritDoc} */
    public double[] getMeanElementRate(final SpacecraftState spacecraftState) throws OrekitException {

        // Compute potential derivatives
        final double[] dU  = computeUDerivatives(provider.getOffset(spacecraftState.getDate()));
        final double duda  = dU[0];
        final double dudh  = dU[1];
        final double dudk  = dU[2];
        final double dudl  = dU[3];
        final double dudal = dU[4];
        final double dudbe = dU[5];
        final double dudga = dU[6];

        // Compute the cross derivative operator :
        final double UAlphaGamma   = alpha * dudga - gamma * dudal;
        final double UAlphaBeta    = alpha * dudbe - beta  * dudal;
        final double UBetaGamma    =  beta * dudga - gamma * dudbe;
        final double Uhk           =     h * dudk  -     k * dudh;
        final double pUAGmIqUBGoAB = (p * UAlphaGamma - I * q * UBetaGamma) * ooAB;
        final double UhkmUabmdudl  =  Uhk - UAlphaBeta - dudl;

        final double aDot =  ax2oA * dudl;
        final double hDot =    BoA * dudk + k * pUAGmIqUBGoAB - h * BoABpo * dudl;
        final double kDot =  -(BoA * dudh + h * pUAGmIqUBGoAB + k * BoABpo * dudl);
        final double pDot =  Co2AB * (p * UhkmUabmdudl - UBetaGamma);
        final double qDot =  Co2AB * (q * UhkmUabmdudl - I * UAlphaGamma);
        final double lDot = -ax2oA * duda + BoABpo * (h * dudh + k * dudk) + pUAGmIqUBGoAB;

        return new double[] {aDot, hDot, kDot, pDot, qDot, lDot};

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

        if (resCoupleSet.isEmpty()) {
            // Compute natural resonant terms
            final double tolerance = 1. / FastMath.max(MIN_PERIOD_IN_SAT_REV,
                                                       MIN_PERIOD_IN_SECONDS / orbitPeriod);

            // Search the resonant orders in the tesseral harmonic field
            for (int m = 1; m <= provider.getMaxOrder(); m++) {
                final double resonance = ratio * m;
                final int j = (int) FastMath.round(resonance);
                if (j > 0 && FastMath.abs(resonance - j) <= tolerance) {
                    // Store each resonant index and order
                    resIndexSet.add(j);
                    resOrderSet.add(m);
                    // Store each resonant couple for a given order
                    for (int n = m; n <= provider.getMaxDegree(); n++) {
                        resCoupleSet.add(new ResonantCouple(n, m));
                    }
                }
            }
        } else {
            // Get user defined resonant terms
            for (ResonantCouple resCouple : resCoupleSet) {
                final int resOrder = resCouple.getM();
                final int resIndex = (int) FastMath.round(ratio * resOrder);
                resOrderSet.add(resOrder);
                resIndexSet.add(FastMath.max(1, resIndex));
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
     *  @throws OrekitException if an error occurs in Hansen computation
     */
    private double[] computeUDerivatives(final double dateOffset) throws OrekitException {

        // Gmsj and Hmsj polynomials
        final GHmsjPolynomials ghMSJ = new GHmsjPolynomials(k, h, alpha, beta, I);

        // GAMMAmns function
        final GammaMnsFunction gammaMNS = new GammaMnsFunction(gamma, I);

        // Hansen coefficients
        final HansenTesseral hansen = new HansenTesseral(ecc, maxHansen);

        // Potential derivatives
        double dUda  = 0.;
        double dUdh  = 0.;
        double dUdk  = 0.;
        double dUdl  = 0.;
        double dUdAl = 0.;
        double dUdBe = 0.;
        double dUdGa = 0.;

        // j-SUM over resonant indexes
        for (int j : resIndexSet) {
            // m-SUM over resonant orders
            for (int m : resOrderSet) {
                final double[] potential = computeUsnDerivatives(dateOffset, j, m, ghMSJ, gammaMNS, hansen);
                dUda  += potential[0];
                dUdh  += potential[1];
                dUdk  += potential[2];
                dUdl  += potential[3];
                dUdAl += potential[4];
                dUdBe += potential[5];
                dUdGa += potential[6];
            }
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
     *  @param ghMSJ G<sup>j</sup><sub>m,s</sub> and H<sup>j</sup><sub>m,s</sub> polynomials
     *  @param gammaMNS &Gamma;<sup>m</sup><sub>n,s</sub>(&gamma;) function
     *  @param hansen Hansen coefficients
     *  @return U<sub>s,n</sub> derivatives
     *  @throws OrekitException if an error occurs in Hansen computation
     */
    private double[] computeUsnDerivatives(final double dateOffset, final int j, final int m,
                                           final GHmsjPolynomials ghMSJ,
                                           final GammaMnsFunction gammaMNS,
                                           final HansenTesseral hansen)
        throws OrekitException {

        // Initialize potential derivatives
        double dUda  = 0.;
        double dUdh  = 0.;
        double dUdk  = 0.;
        double dUdl  = 0.;
        double dUdAl = 0.;
        double dUdBe = 0.;
        double dUdGa = 0.;

        // Jacobi indices
        final double jlMmt  = j * lm - m * theta;
        final double sinPhi = FastMath.sin(jlMmt);
        final double cosPhi = FastMath.cos(jlMmt);

        final int Im = (int) FastMath.pow(I, m);
        // s-SUM from -N to N
        for (int s = -minS; s <= maxS; s++) {

            // jacobi v, w, indices from 2.7.1-(15)
            final int v = FastMath.abs(m - s);
            final int w = FastMath.abs(m + s);
            // n-SUM from (Max(2, m, |s|)) to N
            final int nmin = Math.max(Math.max(2, m), Math.abs(s));
            for (int n = nmin; n <= provider.getMaxDegree(); n++) {
                // (R / a)^n
                final double ran    = FastMath.pow(roa, n);
                final double vMNS   = CoefficientsFactory.getVmns(m, n, s);
                final double gaMNS  = gammaMNS.getValue(m, n, s);
                final double dGaMNS = gammaMNS.getDerivative(m, n, s);
                final double kJNS   = hansen.getValue(j, -n - 1, s);
                final double dkJNS  = hansen.getDerivative(j, -n - 1, s);
                final double dGdh   = ghMSJ.getdGmsdh(m, s, j);
                final double dGdk   = ghMSJ.getdGmsdk(m, s, j);
                final double dGdA   = ghMSJ.getdGmsdAlpha(m, s, j);
                final double dGdB   = ghMSJ.getdGmsdBeta(m, s, j);
                final double dHdh   = ghMSJ.getdHmsdh(m, s, j);
                final double dHdk   = ghMSJ.getdHmsdk(m, s, j);
                final double dHdA   = ghMSJ.getdHmsdAlpha(m, s, j);
                final double dHdB   = ghMSJ.getdHmsdBeta(m, s, j);

                // Jacobi l-indices from 2.7.1-(15)
                final int l = FastMath.min(n - m, n - FastMath.abs(s));
                final PolynomialFunction jacobiPoly = PolynomialsUtils.createJacobiPolynomial(l, v, w);
                final double jacobi  = jacobiPoly.value(gamma);
                final double dJacobi = jacobiPoly.derivative().value(gamma);
                final double gms = ghMSJ.getGmsj(m, s, j);
                final double hms = ghMSJ.getHmsj(m, s, j);
                final double cnm = provider.getUnnormalizedCnm(dateOffset, n, m);
                final double snm = provider.getUnnormalizedCnm(dateOffset, n, m);

                // Compute dU / da from expansion of equation (4-a)
                double realCosFactor = (gms * cnm + hms * snm) * cosPhi;
                double realSinFactor = (gms * snm - hms * cnm) * sinPhi;
                dUda += (n + 1) * ran * Im * vMNS * gaMNS * kJNS * jacobi * (realCosFactor + realSinFactor);

                // Compute dU / dh from expansion of equation (4-b)
                realCosFactor = ( cnm * kJNS * dGdh + 2 * cnm * h * (gms + hms) * dkJNS + snm * kJNS * dHdh) * cosPhi;
                realSinFactor = (-cnm * kJNS * dHdh + 2 * snm * h * (gms + hms) * dkJNS + snm * kJNS * dGdh) * sinPhi;
                dUdh += ran * Im * vMNS * gaMNS * jacobi * (realCosFactor + realSinFactor);

                // Compute dU / dk from expansion of equation (4-c)
                realCosFactor = ( cnm * kJNS * dGdk + 2 * cnm * k * (gms + hms) * dkJNS + snm * kJNS * dHdk) * cosPhi;
                realSinFactor = (-cnm * kJNS * dHdk + 2 * snm * k * (gms + hms) * dkJNS + snm * kJNS * dGdk) * sinPhi;
                dUdk += ran * Im * vMNS * gaMNS * jacobi * (realCosFactor + realSinFactor);

                // Compute dU / dLambda from expansion of equation (4-d)
                realCosFactor = (snm * gms - hms * cnm) * cosPhi;
                realSinFactor = (snm * hms + gms * cnm) * sinPhi;
                dUdl += j * ran * Im * vMNS * kJNS * jacobi * (realCosFactor - realSinFactor);

                // Compute dU / alpha from expansion of equation (4-e)
                realCosFactor = (dGdA * cnm + dHdA * snm) * cosPhi;
                realSinFactor = (dGdA * snm - dHdA * cnm) * sinPhi;
                dUdAl += ran * Im * vMNS * gaMNS * kJNS * jacobi * (realCosFactor + realSinFactor);

                // Compute dU / dBeta from expansion of equation (4-f)
                realCosFactor = (dGdB * cnm + dHdB * snm) * cosPhi;
                realSinFactor = (dGdB * snm - dHdB * cnm) * sinPhi;
                dUdBe += ran * Im * vMNS * gaMNS * kJNS * jacobi * (realCosFactor + realSinFactor);

                // Compute dU / dGamma from expansion of equation (4-g)
                realCosFactor = (gms * cnm + hms * snm) * cosPhi;
                realSinFactor = (gms * snm - hms * cnm) * sinPhi;
                dUdGa += ran * Im * vMNS * kJNS * (jacobi * dGaMNS + gaMNS * dJacobi) * (realCosFactor + realSinFactor);
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
            initialize();
        }

        /** Get the K<sub>j</sub><sup>n,s</sup> coefficient value.
         * @param j j value
         * @param n n value
         * @param s s value
         * @return K<sub>j</sub><sup>n,s</sup>
         * @throws OrekitException if some error occurred
         */
        public double getValue(final int j, final int n, final int s)
            throws OrekitException {
            if (coefficients.containsKey(new MNSKey(j, n, s))) {
                return coefficients.get(new MNSKey(j, n, s));
            } else {
                // Compute the general Kj,-n-1,s with n >= 0
                return computeValue(j, -n - 1, s);
            }
        }

        /** Get the dK<sub>j</sub><sup>n,s</sup> / de<sup>2</sup> coefficient derivative.
         *  @param j j value
         *  @param n n value
         *  @param s s value
         *  @return dK<sub>j</sub><sup>n,s</sup> / d&chi;
         *  @throws OrekitException if some error occurred
         */
        public double getDerivative(final int j, final int n, final int s)
            throws OrekitException {
            if (derivatives.containsKey(new MNSKey(j, n, s))) {
                return derivatives.get(new MNSKey(j, n, s));
            } else {
                // Compute the general dKj,-n-1,s / de2 derivative with n >= 0
                return computeDerivative(j, -n - 1, s);
            }
        }

        /** Kernels initialization. */
        private void initialize() {
            coefficients.put(new MNSKey(0, 0, 0), 1.);
            coefficients.put(new MNSKey(0, 0, 1), -1.);
            coefficients.put(new MNSKey(0, 1, 0), 1. + 0.5 * e2);
            coefficients.put(new MNSKey(0, 1, 1), -1.5);
            coefficients.put(new MNSKey(0, 2, 0), 1. + 1.5 * e2);
            coefficients.put(new MNSKey(0, 2, 1), -2. - 0.5 * e2);
            coefficients.put(new MNSKey(0, -1, 0), 0.);
            coefficients.put(new MNSKey(0, -1, 1), 0.);
            coefficients.put(new MNSKey(0, -2, 0), chi);
            coefficients.put(new MNSKey(0, -2, 1), 0.);
            coefficients.put(new MNSKey(0, -3, 0), chi * chi2);
            coefficients.put(new MNSKey(0, -3, 1), 0.5 * chi * chi2);
            derivatives.put(new MNSKey(0, 0, 0), 0.);
        }

        /** Compute the K<sub>j</sub><sup>-n-1,s</sup> coefficient from equation 2.7.3-(9).
         *
         * @param j j value
         * @param n n value, must be positive.
         *          For a given 'n', the K<sub>j</sub><sup>-n-1,s</sup> will be returned.
         * @param s s value
         * @return K<sub>j</sub><sup>-n-1,s</sup>
         * @throws OrekitException if some error occurred
         */
        private double computeValue(final int j, final int n, final int s)
            throws OrekitException {
            double result = 0;
            if ((n == 3) || (n == s + 1) || (n == 1 - s)) {
                result = valueFromNewcomb(j, -n - 1, s);
            } else {
                final double kmN = valueFromNewcomb(j, -n, s);
                coefficients.put(new MNSKey(j, -n, s), kmN);
                final double kmNp1 = valueFromNewcomb(j, -n + 1, s);
                coefficients.put(new MNSKey(j, -n + 1, s), kmNp1);
                final double kmNp3 = valueFromNewcomb(j, -n + 3, s);
                coefficients.put(new MNSKey(j, -n + 3, s), kmNp3);

                final double factor = chi2 / ((3. - n) * (1. - n + s) * (1. - n - s));
                final double factmN = (3. - n) * (1. - n) * (3. - 2. * n);
                final double factmNp1 = (2. - n) * ((3. - n) * (1. - n) + (2. * j * s) / chi);
                final double factmNp3 = j * j * (1. - n);
                result = factor * (factmN * kmN - factmNp1 * kmNp1 + factmNp3 * kmNp3);
            }
            coefficients.put(new MNSKey(j, -n - 1, s), result);
            return result;
        }

        /** Compute dK<sub>j</sub><sup>n,s</sup>/de<sup>2</sup> from equation 3.3-(5).
         *  <p>
         *  This coefficient is always calculated for a negative n = -np-1 with np > 0
         *  </p>
         *  @param j j value
         *  @param n np value (> 0)
         *  @param s s value
         *  @return dK<sub>j</sub><sup>n,s</sup>/de<sup>2</sup>
         *  @throws OrekitException if some error occurred
         */
        private double computeDerivative(final int j, final int n, final int s)
            throws OrekitException {
            // Get Kjns value
            final double Kjns = getValue(j, n, s);

            final int a = FastMath.max(j - s, 0);
            final int b = FastMath.max(s - j, 0);

            // Expansion until the maximum power in e^2 is reached
            double sum     = 0.;
            // e^2(alpha-1)
            double e2Pam1  = 1.;
            for (int alpha = 1; alpha < maxE2Power; alpha++) {
                final double newcomb = ModifiedNewcombOperators.getValue(alpha + a, alpha + b, -n - 1, s);
                sum += alpha * newcomb * e2Pam1;
                e2Pam1 *= e2;
            }
            final double coef = -n + 0.5;
            return FastMath.pow(ome2, coef) * sum - coef * chi2 * Kjns;
        }

        /** Compute the Hansen coefficient K<sub>j</sub><sup>ns</sup> from equation 2.7.3-(10).<br>
         *  The coefficient value is evaluated from the {@link ModifiedNewcombOperators} elements.
         *  <p>
         *  This coefficient is always calculated for a negative n = -np-1 with np > 0
         *  </p>
         *
         *  @param j j value
         *  @param n n value (< 0)
         *  @param s s value
         *  @return K<sub>j</sub><sup>ns</sup>
         *  @throws OrekitException if the Newcomb operator cannot be computed with the current indexes
         */
        private double valueFromNewcomb(final int j, final int n, final int s)
            throws OrekitException {
            // Initialization
            final int a = FastMath.max(j - s, 0);
            final int b = FastMath.max(s - j, 0);

            // Expansion until the maximum power in e^2 is reached
            double sum  = 0.;
            // e2^alpha
            double e2Pa = 1.;
            for (int alpha = 0; alpha <= maxE2Power; alpha++) {
                final double newcomb = ModifiedNewcombOperators.getValue(alpha + a, alpha + b, n, s);
                sum += newcomb * e2Pa;
                e2Pa *= e2;
            }
            return FastMath.pow(ome2, n + 1.5) * sum;
        }
    }
}
