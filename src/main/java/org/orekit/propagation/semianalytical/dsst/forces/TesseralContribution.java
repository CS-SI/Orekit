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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.analysis.polynomials.PolynomialsUtils;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.MathUtils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.semianalytical.dsst.coefficients.CjSjCoefficient;
import org.orekit.propagation.semianalytical.dsst.coefficients.DSSTCoefficientFactory;
import org.orekit.propagation.semianalytical.dsst.coefficients.GHmsjPolynomials;
import org.orekit.propagation.semianalytical.dsst.coefficients.GammaMsnCoefficients;
import org.orekit.propagation.semianalytical.dsst.coefficients.HansenCoefficients;
import org.orekit.time.AbsoluteDate;

/** Tesseral contribution to the {@link DSSTCentralBody central body gravitational perturbation}
 *
 *   @author Romain Di Costanzo
 *   @author Pascal Parraud
 */
class TesseralContribution implements DSSTForceModel {

    /** Truncation tolerance for orbits always in vacuum. */
    private static final double TRUNCATION_TOLERANCE = 1e-10;

    /** Maximum resonant order. */
    private int maxResonantOrder;

    /** Maximum resonant degree. */
    private int maxResonantDegree;

    /** List of resonant tesseral harmonic couple. */
    private List<ResonantCouple>   resonantTesseralHarmonic;

    /**
     * Minimum period for analytically averaged high-order resonant central body spherical harmonics
     * in seconds. This value is set to 10 days, but can be overrides by using the
     * {@link #setResonantMinPeriodInSec(double)} method.
     */
    private double resonantMinPeriodInSec;

    /**
     * Minimum period for analytically averaged high-order resonant central body spherical harmonics
     * in satellite revolutions. This value is set to 10 satellite revolutions, but can be overrides
     * by using the {@link #setResonantMinPeriodInSatRev(double)} method.
     */
    private double resonantMinPeriodInSatRev;

    /**
     * Highest power of the eccentricity to appear in the truncated analytical power series
     * expansion for the averaged central-body resonant Tesseral harmonic potential. The user can
     * set this value by using the {@link #setTesseralMaximumEccentricityPower(double)} method. If
     * he doesn't, the software will compute a default value itself, through the
     * {@link #computeTesseralMaxEccentricityPower()} method.
     */
    private int                    tesseralMaxEccentricityPower;

    /** Minimal integer value for the s index truncation in tesseral harmonic expansion. */
    private int                    tessMinS;

    /** Maximal integer value for the s index truncation in tesseral harmonic expansion. */
    private int                    tessMaxS;

    /** Minimal integer value for the N index truncation in tesseral harmonic expansion. */
    private int                    tessMaxN;

    /**
     * Tesseral trucation tolerance. This value is used by the
     * {@link DSSTCentralBody#tesseralTruncation(SpacecraftState)} method which determines the upper
     * bound for geopotential summation.
     */
    private double                 tesseralTruncationTolerance;

    /** Maximum power of the eccentricity in the Hansen coefficient kernels. */
    private int                    maximumHansen;

    /** Hansen coefficient. */
    private HansenCoefficients     hansen;

    /** Central-body rotation period (seconds) */
    private final double bodyPeriod;
    
    /** Equatorial radius of the Central Body */
    private final double ae;

    /** Central body attraction coefficient (m<sup>3</sup>/s<sup>2</sup>) */
    private final double mu;

    /** First normalized potential coefficients array */
    private final double[][] Cnm;

    /** Second normalized potential coefficients array */
    private final double[][] Snm;

    /** Degree <i>n</i> of potential */
    private final int   degree;

    /** Order <i>m</i> of potential */
    private final int   order;

    /** Retrograde factor */
    private int    I;

    // Equinoctial elements (according to DSST notation)
    /** a */
    private double a;
    /** ex */
    private double k;
    /** ey */
    private double h;
    /** hx */
    private double q;
    /** hy */
    private double p;
    /** lm */
    private double lm;

    /** Keplerian period */
    private double orbitPeriod;

    /** Eccentricity */
    private double ecc;

    // Equinoctial reference frame vectors (according to DSST notation)
    /** f */
    private Vector3D f;
    /** g */
    private Vector3D g;

    /** Direction cosine &alpha */
    private double alpha;
    /** Direction cosine &beta */
    private double beta;
    /** Direction cosine &gamma */
    private double gamma;

    // Common factors from equinoctial coefficients
    /** a / A */
    private double aoA;
    /** 1 / (A * B) */
    private double ooAB;
    /** B / A */
    private double BoA;
    /** B / (A * (1 + B)) */
    private double BoABpo;
    /** C / (2 * A * B) */
    private double Co2AB;
    /** &mu; / a */
    private double moa;

    /** &Gamma;<sub>n, s</sub> <sup>m</sup> (&gamma;) coefficient from equations 2.7.1 - (13). */
    private GammaMsnCoefficients gammaMNS;

    /** Single constructor.
    * @param centralBodyRotationRate central body rotation rate (rad/s)
    * @param equatorialRadius equatorial radius of the central body (m)
    * @param mu central body attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
    * @param Cnm un-normalized coefficients array of the spherical harmonics (cosine part)
    * @param Snm un-normalized coefficients array of the spherical harmonics (sine part)
    */
    public TesseralContribution(final double centralBodyRotationRate,
                                final double equatorialRadius,
                                final double mu,
                                final double[][] Cnm,
                                final double[][] Snm) {

        // Set the central-body rotation period
        this.bodyPeriod = MathUtils.TWO_PI / centralBodyRotationRate;
        this.ae         = equatorialRadius;
        this.mu         = mu;
        this.Cnm        = Cnm.clone();
        this.Snm        = Snm.clone();
        this.degree     = Cnm.length - 1;
        this.order      = Cnm[degree].length - 1;

        // Initialize default values
        this.resonantMinPeriodInSec = 864000d;
        this.resonantMinPeriodInSatRev = 10d;
        this.tesseralMaxEccentricityPower = Integer.MIN_VALUE;
        this.tesseralTruncationTolerance = Double.NEGATIVE_INFINITY;

        // Initialize default values
        this.maximumHansen = Integer.MIN_VALUE;

        // Store local variables
        resonantTesseralHarmonic = new ArrayList<ResonantCouple>();
        // Set to a default undefined value
        maxResonantOrder = Integer.MIN_VALUE;
        maxResonantDegree = Integer.MIN_VALUE;

    }

    /** {@inheritDoc} */
    public final void initialize(final AuxiliaryElements aux) throws OrekitException {
        
        // Equinoctial elements
        a  = aux.getSma();
        k  = aux.getK();
        h  = aux.getH();
        q  = aux.getQ();
        p  = aux.getP();
        lm = aux.getLM();
        
        // Eccentricity
        ecc = aux.getEcc();
        
        // Keplerian period
        orbitPeriod = aux.getKeplerianPeriod();
    
        // Retrograde factor
        I = aux.getRetrogradeFactor();

        // Equinoctial frame vectors
        f = aux.getVectorF();
        g = aux.getVectorG();
        
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
        aoA  = a / A;
        // B / A
        BoA  = B / A;
        // 1 / AB
        ooAB = 1. / (A * B);
        // C / 2AB
        Co2AB = C * ooAB / 2.;
        // B / (A * (1 + B))
        BoABpo = BoA / (1. + B);
        // &mu / a
        moa  = mu / a;

        // Compute  &Gamma;<sub>n, s</sub> <sup>m</sup> (&gamma;) coefficient
        gammaMNS = new GammaMsnCoefficients(gamma, I);

        // Get the maximum power of E to use in Hansen coefficient Kernel expansion
        computeHansenMaximumEccentricity();

        // Initialize hansen coefficient
        hansen   = new HansenCoefficients(ecc, maximumHansen);

        // Compute the central body resonant tesseral harmonic terms
        computeResonantTesseral();
    
        // Set the highest power of the eccentricity in the analytical power series expansion for
        // the averaged high order resonant central body spherical harmonic perturbation
        computeResonantTesseralMaxEccPower();
    
        // TODO Not sure about this part :
        // Truncation of the central body tesseral harmonic :
        if (resonantTesseralHarmonic.size() > 0) {
//            tesseralTruncation();
            tessMaxN = Collections.max(resonantTesseralHarmonic).getN();
        } else {
            tessMaxN = degree;
        }
        tessMinS = tesseralMaxEccentricityPower;
        tessMaxS = tesseralMaxEccentricityPower;

    }

    /** {@inheritDoc} */
    public double[] getMeanElementRate(final SpacecraftState spacecraftState) throws OrekitException {

        // Compute potential derivatives
        final double[] dU  = computeUDerivatives();
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

        final double aDot =  2 * aoA * dudl;
        final double hDot =   BoA * dudk + k * pUAGmIqUBGoAB - h * BoABpo * dudl;
        final double kDot = -(BoA * dudh + h * pUAGmIqUBGoAB + k * BoABpo * dudl);
        final double pDot = Co2AB * (p * UhkmUabmdudl - UBetaGamma);
        final double qDot = Co2AB * (q * UhkmUabmdudl - I * UAlphaGamma);
        final double lDot = -2 * aoA * duda + BoABpo * (h * dudh + k * dudk) + pUAGmIqUBGoAB;

        return new double[] {aDot, hDot, kDot, pDot, qDot, lDot};

    }

    /** {@inheritDoc} */
    public double[] getShortPeriodicVariations(final AbsoluteDate date,
                                               final double[] meanElements)
        throws OrekitException {
        // TODO: not implemented yet, Short Periodic Variations are set to null
        return new double[] {0., 0., 0., 0., 0., 0.};
    }

    /** Set the resonant Tesseral harmonic couple term.
     *  This parameter can be set to null or be an empty list.
     *  If so, the program will automatically determine the resonant couple to
     *  take in account. If not, only the resonant couple given by the user will
     *  be taken in account.
     *  
     * @param resonantTesseral Resonant Tesseral harmonic couple term
     */
    public final void setResonantTesseral(final List<ResonantCouple> resonantTesseral) {

        // Store local variables
        if (resonantTesseral != null) {
            resonantTesseralHarmonic = resonantTesseral;
            if (resonantTesseralHarmonic.size() > 0) {
                // Get the maximal resonant order
                final ResonantCouple maxCouple = Collections.max(resonantTesseral);
                maxResonantOrder  = maxCouple.getM();
                maxResonantDegree = maxCouple.getN();
            }
        } else {
            resonantTesseralHarmonic = new ArrayList<ResonantCouple>();
            // Set to a default undefined value
            maxResonantOrder  = Integer.MIN_VALUE;
            maxResonantDegree = Integer.MIN_VALUE;
        }
    }

    /** Set the minimum period for analytically averaged high-order resonant
     *  central body spherical harmonics in seconds.
     *  <p>
     *  Set to 10 days by default.
     *  </p>
     * @param resonantMinPeriodInSec minimum period in seconds
     */
    public final void setResonantMinPeriodInSec(final double resonantMinPeriodInSec) {
        this.resonantMinPeriodInSec = resonantMinPeriodInSec;
    }

    /** Set the minimum period for analytically averaged high-order resonant
     *  central body spherical harmonics in number of satellite revolutions.
     *  <p>
     *  Set to 10 days by default.
     *  </p>
     * @param resonantMinPeriodInSatRev minimum period in satellite revolutions
     */
    public final void setResonantMinPeriodInSatRev(final double resonantMinPeriodInSatRev) {
        this.resonantMinPeriodInSatRev = resonantMinPeriodInSatRev;
    }

    /** Set the highest power of the eccentricity to appear in the truncated analytical
     *  power series expansion for the averaged central-body tesseral harmonic potential.
     *
     * @param tesseralMaxEccPower highest power of the eccentricity
     */
    public final void setTesseralMaximumEccentricityPower(final int tesseralMaxEccPower) {
        this.tesseralMaxEccentricityPower = tesseralMaxEccPower;
    }

    /** Compute the maximum power of the eccentricity to use in Hansen coefficient Kernel expansion.
     */
    private void computeHansenMaximumEccentricity() {
        if (maximumHansen != Integer.MIN_VALUE) {
            // Set the maximum value to tesseralMaxEccentricityPower / 2
            maximumHansen = Math.min(maximumHansen, 10);
        } else {
            maximumHansen = (int) tesseralMaxEccentricityPower / 2;
        }
    }

    /**
     * This subroutine finds the resonant tesseral terms in the central body spherical harmonic
     * field. The routine computes the repetition period of the perturbation caused by each central
     * body sectoral and tesseral harmonic term and compares the period to a predetermined
     * tolerance, the minimum period considered to be resonant.
     */
    private void computeResonantTesseral() {

        // Compute ration of satellite period to central body rotation period
        final double ratio = orbitPeriod / bodyPeriod;
    
        // If user didn't define a maximum resonant order, use the maximum central-body's
        // order
        if (maxResonantOrder == Integer.MIN_VALUE) {
            maxResonantOrder = order;
        }
    
        if (maxResonantDegree == Integer.MIN_VALUE) {
            maxResonantDegree = degree;
        }
    
        // Has the user requested a specific set of resonant tesseral harmonic terms ?
        if (resonantTesseralHarmonic.size() == 0) {
    
            final int maxResonantOrderTmp = maxResonantOrder;
            // Reinitialize the maxResonantOrder parameter :
            maxResonantOrder = 0;
    
            double tolerance = resonantMinPeriodInSec / orbitPeriod;
            if (tolerance < resonantMinPeriodInSatRev) {
                tolerance = resonantMinPeriodInSatRev;
            }
            tolerance = 1d / tolerance;
    
            // Now find the order of the resonant tesseral harmonic field
            for (int m = 1; m <= maxResonantOrderTmp; m++) {
                final double resonance = ratio * m;
                final int j = (int) FastMath.ceil(resonance);
                if (FastMath.abs(resonance - j) <= tolerance && j > 0d) {
                    // Update the maximum resonant order found
                    maxResonantOrder = m;
    
                    // Store each resonant couple for a given order
                    int n = m;
                    // insert resonant terms into the resonant field until either 10 terms or all of
                    // the resonant terms are chosen
                    while (n <= degree && resonantTesseralHarmonic.size() < 10) {
                        resonantTesseralHarmonic.add(new ResonantCouple(n, m));
                        n++;
                    }
                }
            }
        }
    }

    /** Computes the highest power of the eccentricity to appear in the truncated
     *  analytical power series expansion for the averaged central-body resonant tesseral harmonic
     *  potential.
     *  Analytical averaging should not be used for resonant harmonics if the eccentricity is greater
     *  than 0.5.
     *
     * @throws OrekitException if eccentricity > 0.5
     */
    private void computeResonantTesseralMaxEccPower() throws OrekitException {
        // Is the maximum d'Alenbert characteristic given by the user ?
        if (tesseralMaxEccentricityPower != Integer.MIN_VALUE) {
            // Set the maximum possible power expansion to 20
            tesseralMaxEccentricityPower = Math.min(tesseralMaxEccentricityPower, 20);
        } else {
            // Set the correct d'Alembert characteristic from the satellite eccentricity
            if (ecc < 5E-3) {
                tesseralMaxEccentricityPower = 3;
            } else if (ecc <= 0.02) {
                tesseralMaxEccentricityPower = 4;
            } else if (ecc <= 0.1) {
                tesseralMaxEccentricityPower = 7;
            } else if (ecc <= 0.2) {
                tesseralMaxEccentricityPower = 10;
            } else if (ecc <= 0.3) {
                tesseralMaxEccentricityPower = 12;
            } else if (ecc <= 0.4) {
                tesseralMaxEccentricityPower = 15;
            } else if (ecc <= 0.5) {
                tesseralMaxEccentricityPower = 20;
            } else {
                throw new OrekitException(OrekitMessages.DSST_ECC_NO_NUMERICAL_AVERAGING_METHOD, ecc);
            }
        }
    }

    /** The expansion of the central body tesseral harmonics has four truncatable indices.
     *  The algorithm determines the maximum value of each of those indices by computing the upper
     *  |R<sub>jmsn</sub>| perturbation function value for every indices. <br>
     *  Algorithm description can be found in the D.A Danielson paper at paragraph 6.3
     *
     *  @throws OrekitException if an error occurs when computing Hansen upper bound
     */
    private void truncation() throws OrekitException {
    
        // Check if a value has been entered by the user :
        if (tesseralTruncationTolerance == Double.NEGATIVE_INFINITY) {
            tesseralTruncationTolerance = TRUNCATION_TOLERANCE;
        }
    
        // Temporary variables :
        int sMin = Integer.MAX_VALUE;
        int sMax = Integer.MIN_VALUE;
        int n;
        boolean sLoop = true;
    
        // J-loop j = 0, +-1, +-2 ...
        // Resonant term identified :
        final Iterator<ResonantCouple> iterator = resonantTesseralHarmonic.iterator();
        // Iterative process :
    
        while (iterator.hasNext()) {
            final ResonantCouple resonantTesseralCouple = iterator.next();
            final int j = resonantTesseralCouple.getN();
            int m = resonantTesseralCouple.getM();
            int sbis = 0;
            // S-loop : s = j, j+-1, j+-2 ...
            int s = j;
            while (sLoop) {
                final int signS = (int) FastMath.pow(-1, s);
                sbis += s * signS;
                sMin = FastMath.min(sMin, sbis);
                sMax = FastMath.max(sMax, sbis);
    
                // N-loop : n = Max(2, m, |s|), n-m even and n < N. N being the maximum
                // potential degree
                n = FastMath.max(FastMath.max(2, m), FastMath.abs(sbis));
    
                if (n > degree) {
                    break;
                }
    
                if ((n - sbis) % 2 == 0) {
    
                    // Compute the perturbation function upper bound :
                    final double hansenUp = HansenCoefficients.computeUpperBound(ecc, j, -n - 1, sbis);
    
                    // Compute Jacobi polynomials upper bound :
                    final int l = (sbis <= m) ? (n - m) : n - sbis;
                    final int v = FastMath.abs(m - sbis);
                    final int w = FastMath.abs(m + sbis);
    
                    final PolynomialFunction jacobi = PolynomialsUtils.createJacobiPolynomial(l, v, w);
                    final double jacDer = jacobi.derivative().value(gamma);
                    final double jacDer2 = jacDer * jacDer;
                    final double jacGam = jacobi.value(gamma);
                    final double jacGam2 = jacGam * jacGam;
                    final double jacFact = (1 - gamma * gamma) / (l * (v + w + l + 1));
                    final double jacobiUp = FastMath.sqrt(jacGam2 + jacFact * jacDer2);
    
                    // Upper bound for |Cnm - iSnm|
                    final double cnm = Cnm[n][m];
                    final double cnm2 = cnm * cnm;
                    final double snm = Snm[n][m];
                    final double snm2 = snm * snm;
                    final double csnmUp = FastMath.sqrt(cnm2 + snm2);
    
                    // Upper bound for the |Gmsj + iHmsj|
                    final double maxE = FastMath.pow(ecc, FastMath.abs(sbis - j));
                    final int p = FastMath.abs(sbis - I * m) / 2;
                    final double maxG = FastMath.pow(1 - gamma * gamma, p);
                    final double ghmsUp = maxE * maxG;
    
                    // Upper bound for Vmns
                    final double vmnsUp = FastMath.abs(DSSTCoefficientFactory.getVmns(m, n, sbis));
                    // Upper bound for Gammamsn
                    final GammaMsnCoefficients gmns = new GammaMsnCoefficients(gamma, I);
                    final double gmnsUp = FastMath.abs(gmns.getGammaMsn(n, sbis, m));
    
                    // Upper perturbation function value
                    final double common = moa * FastMath.pow(ae / a, n);
                    final double upperValue = common * vmnsUp * gmnsUp * hansenUp * jacobiUp * csnmUp * ghmsUp;
    
                    if (upperValue <= tesseralTruncationTolerance) {
                        // Store values :
                        tessMinS = FastMath.abs(sMin);
                        tessMaxS = sMax;
                        tessMaxN = n;
    
                        // Force loop to stop :
                        sLoop = false;
                        m = order;
                        n = degree;
                    }
                }
                s++;
            }
        }
    }

    /**
     * Compute the following elements from expression 3.3 - (4).
     * If tesseral harmonic have been identified (automatically or set by user),
     * they are the only one to be taken in account.
     * If no resonant term have been found, we compute non resonant tesseral term from those
     * found by the {@link tesseralTruncation(SpacecraftState)} method.
     *
     * <pre>
     * dU / da
     * dU / dh
     * dU / dk
     * dU / d&lambda;
     * dU / d&alpha;
     * dU / d&beta;
     * dU / d&gamma;
     *
     * </pre>
     *
     * @return potential derivatives
     * @throws OrekitException if an error occurs in Hansen computation
     */
    private double[] computeUDerivatives() throws OrekitException {
        // Result initialization
        double duda  = 0d;
        double dudh  = 0d;
        double dudk  = 0d;
        double dudl  = 0d;
        double dudal = 0d;
        double dudbe = 0d;
        double dudga = 0d;

        // Resonant term identified :
        final Iterator<ResonantCouple> iterator = resonantTesseralHarmonic.iterator();
        // Iterative process :

        while (iterator.hasNext()) {
            final ResonantCouple resonantTesseralCouple = iterator.next();
            final int j = resonantTesseralCouple.getN();
            final int m = resonantTesseralCouple.getM();

            final double[] potential = tesseralPotentialComputation(j, m);
            duda  += potential[0];
            dudh  += potential[1];
            dudk  += potential[2];
            dudl  += potential[3];
            dudal += potential[4];
            dudbe += potential[5];
            dudga += potential[6];
        }

        duda  *= -moa / a;
        dudh  *=  moa;
        dudk  *=  moa;
        dudl  *=  moa;
        dudal *=  moa;
        dudbe *=  moa;
        dudga *=  moa;

        return new double[] {duda, dudh, dudk, dudl, dudal, dudbe, dudga};
    }

    /** Compute potential for tesseral harmonic terms.
     * @param j j-index
     * @param m m-index
     * @return potential derivatives
     * @throws OrekitException if an error occurs in Hansen computation
     */
    private double[] tesseralPotentialComputation(final int j, final int m)
        throws OrekitException {

        // Result initialization
        double duda  = 0d;
        double dudh  = 0d;
        double dudk  = 0d;
        double dudl  = 0d;
        double dudal = 0d;
        double dudbe = 0d;
        double dudga = 0d;

        final double ra = ae / a;

        // The &theta; angle is the central body rotation angle defined
        // from the equinoctial reference frame from equation 2.7.1-(3)(4).
        final double theta  = FastMath.atan2(-f.getY() + I * g.getX(), f.getX() + I * g.getY());
        final CjSjCoefficient cisiKH = new CjSjCoefficient(k, h);
        final CjSjCoefficient cisiAB = new CjSjCoefficient(alpha, beta);
        final GHmsjPolynomials GHms  = new GHmsjPolynomials(cisiKH, cisiAB, I);

        // Jacobi indices
        final double jlMmt  = j * lm - m * theta;
        final double sinPhi = FastMath.sin(jlMmt);
        final double cosPhi = FastMath.cos(jlMmt);

        final int Im = (int) FastMath.pow(I, m);
        // Sum(-N, N)
        for (int s = -tessMinS; s <= tessMaxS; s++) {
            // Sum(Max(2, m, |s|))
            final int nmin = Math.max(Math.max(2, m), Math.abs(s));

            // jacobi v, w, indices : see 2.7.1 - (15)
            final int v = FastMath.abs(m - s);
            final int w = FastMath.abs(m + s);
            for (int n = nmin; n <= tessMaxN; n++) {
                // (R / a)^n
                final double ran = FastMath.pow(ra, n);
                final double vmsn = DSSTCoefficientFactory.getVmns(m, n, s);
                final double gamMsn = gammaMNS.getGammaMsn(n, s, m);
                final double dGamma = gammaMNS.getDGammaMsn(n, s, m);
                final double kjn_1 = hansen.getHansenKernelValue(j, -n - 1, s);
                // kjn_1 = hansen.computHKVfromNewcomb(j, -n - 1, s);
                final double dkjn_1 = hansen.getHansenKernelDerivative(j, -n - 1, s);
                final double dGdh = GHms.getdGmsdh(m, s, j);
                final double dGdk = GHms.getdGmsdk(m, s, j);
                final double dGdA = GHms.getdGmsdAlpha(m, s, j);
                final double dGdB = GHms.getdGmsdBeta(m, s, j);
                final double dHdh = GHms.getdHmsdh(m, s, j);
                final double dHdk = GHms.getdHmsdk(m, s, j);
                final double dHdA = GHms.getdHmsdAlpha(m, s, j);
                final double dHdB = GHms.getdHmsdBeta(m, s, j);

                // Jacobi l-indices : see 2.7.1 - (15)
                final int l = FastMath.abs(s) <= m ? (n - m) : n - FastMath.abs(s);
                final PolynomialFunction jacobiPoly = PolynomialsUtils.createJacobiPolynomial(l, v, w);
                final double jacobi = jacobiPoly.value(gamma);
                final double dJacobi = jacobiPoly.derivative().value(gamma);
                final double gms = GHms.getGmsj(m, s, j);
                final double hms = GHms.getHmsj(m, s, j);
                final double cnm = Cnm[n][m];
                final double snm = Snm[n][m];

                // Compute dU / da from expansion of equation (4-a)
                double realCosFactor = (gms * cnm + hms * snm) * cosPhi;
                double realSinFactor = (gms * snm - hms * cnm) * sinPhi;
                duda += (n + 1) * ran * Im * vmsn * gamMsn * kjn_1 * jacobi * (realCosFactor + realSinFactor);

                // Compute dU / dh from expansion of equation (4-b)
                realCosFactor = (cnm * kjn_1 * dGdh + 2 * cnm * h * (gms + hms) * dkjn_1 + snm * kjn_1 * dHdh) * cosPhi;
                realSinFactor = (-cnm * kjn_1 * dHdh + 2 * snm * h * (gms + hms) * dkjn_1 + snm * kjn_1 * dGdh) * sinPhi;
                dudh += ran * Im * vmsn * gamMsn * jacobi * (realCosFactor + realSinFactor);

                // Compute dU / dk from expansion of equation (4-c)
                realCosFactor = (cnm * kjn_1 * dGdk + 2 * cnm * k * (gms + hms) * dkjn_1 + snm * kjn_1 * dHdk) * cosPhi;
                realSinFactor = (-cnm * kjn_1 * dHdk + 2 * snm * k * (gms + hms) * dkjn_1 + snm * kjn_1 * dGdk) * sinPhi;
                dudk += ran * Im * vmsn * gamMsn * jacobi * (realCosFactor + realSinFactor);

                // Compute dU / dLambda from expansion of equation (4-d)
                realCosFactor = (snm * gms - hms * cnm) * cosPhi;
                realSinFactor = (snm * hms + gms * cnm) * sinPhi;
                dudl += j * ran * Im * vmsn * kjn_1 * jacobi * (realCosFactor - realSinFactor);

                // Compute dU / alpha from expansion of equation (4-e)
                realCosFactor = (dGdA * cnm + dHdA * snm) * cosPhi;
                realSinFactor = (dGdA * snm - dHdA * cnm) * sinPhi;
                dudal += ran * Im * vmsn * gamMsn * kjn_1 * jacobi * (realCosFactor + realSinFactor);

                // Compute dU / dBeta from expansion of equation (4-f)
                realCosFactor = (dGdB * cnm + dHdB * snm) * cosPhi;
                realSinFactor = (dGdB * snm - dHdB * cnm) * sinPhi;
                dudbe += ran * Im * vmsn * gamMsn * kjn_1 * jacobi * (realCosFactor + realSinFactor);

                // Compute dU / dGamma from expansion of equation (4-g)
                realCosFactor = (gms * cnm + hms * snm) * cosPhi;
                realSinFactor = (gms * snm - hms * cnm) * sinPhi;
                dudga += ran * Im * vmsn * kjn_1 * (jacobi * dGamma + gamMsn * dJacobi) * (realCosFactor + realSinFactor);
            }
        }

        return new double[] {duda, dudh, dudk, dudl, dudal, dudbe, dudga};
    }


}
