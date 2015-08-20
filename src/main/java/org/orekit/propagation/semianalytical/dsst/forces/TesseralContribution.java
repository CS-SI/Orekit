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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.MathUtils;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider.UnnormalizedSphericalHarmonics;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.propagation.semianalytical.dsst.utilities.CoefficientsFactory;
import org.orekit.propagation.semianalytical.dsst.utilities.GHmsjPolynomials;
import org.orekit.propagation.semianalytical.dsst.utilities.GammaMnsFunction;
import org.orekit.propagation.semianalytical.dsst.utilities.JacobiPolynomials;
import org.orekit.propagation.semianalytical.dsst.utilities.ShortPeriodicsInterpolatedCoefficient;
import org.orekit.propagation.semianalytical.dsst.utilities.hansen.HansenTesseralLinear;
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

    /** Number of points for interpolation. */
    private static final int INTERPOLATION_POINTS = 3;

    /** Maximum possible (absolute) value for j index. */
    private static final int MAXJ = 12;

    /** The maximum degree used for tesseral short periodics (without m-daily). */
    private static final int MAX_DEGREE_TESSERAL_SP = 8;

    /** The maximum degree used for m-daily tesseral short periodics. */
    private static final int MAX_DEGREE_MDAILY_TESSERAL_SP = 12;

    /** The maximum order used for tesseral short periodics (without m-daily). */
    private static final int MAX_ORDER_TESSERAL_SP = 8;

    /** The maximum order used for m-daily tesseral short periodics. */
    private static final int MAX_ORDER_MDAILY_TESSERAL_SP = 12;

    /** The maximum value for eccentricity power. */
    private static final int MAX_ECCPOWER_SP = 4;

    /** Provider for spherical harmonics. */
    private final UnnormalizedSphericalHarmonicsProvider provider;

    /** Central body rotating frame. */
    private final Frame bodyFrame;

    /** Central body rotation rate (rad/s). */
    private final double centralBodyRotationRate;

    /** Central body rotation period (seconds). */
    private final double bodyPeriod;

    /** Maximal degree to consider for harmonics potential. */
    private final int maxDegree;

    /** Maximal degree to consider for short periodics tesseral harmonics potential (without m-daily). */
    private final int maxDegreeTesseralSP;

    /** Maximal degree to consider for short periodics m-daily tesseral harmonics potential . */
    private final int maxDegreeMdailyTesseralSP;

    /** Maximal order to consider for harmonics potential. */
    private final int maxOrder;

    /** Maximal order to consider for short periodics tesseral harmonics potential (without m-daily). */
    private final int maxOrderTesseralSP;

    /** Maximal order to consider for short periodics m-daily tesseral harmonics potential . */
    private final int maxOrderMdailyTesseralSP;

    /** List of resonant orders. */
    private final List<Integer> resOrders;

    /** Factorial. */
    private final double[] fact;

    /** Maximum power of the eccentricity to use in summation over s. */
    private int maxEccPow;

    /** Maximum power of the eccentricity to use in summation over s for
     * short periodic tesseral harmonics (without m-daily). */
    private int maxEccPowTesseralSP;

    /** Maximum power of the eccentricity to use in summation over s for
     * m-daily tesseral harmonics. */
    private int maxEccPowMdailyTesseralSP;

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

    // Common factors for potential computation
    /** &Chi; = 1 / sqrt(1 - e²) = 1 / B. */
    private double chi;

    /** &Chi;². */
    private double chi2;

    // Equinoctial reference frame vectors (according to DSST notation)
    /** Equinoctial frame f vector. */
    private Vector3D f;

    /** Equinoctial frame g vector. */
    private Vector3D g;

    /** Central body rotation angle θ. */
    private double theta;

    /** Direction cosine α. */
    private double alpha;

    /** Direction cosine β. */
    private double beta;

    /** Direction cosine γ. */
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

    /** μ / a .*/
    private double moa;

    /** R / a .*/
    private double roa;

    /** ecc². */
    private double e2;

    /** The satellite mean motion. */
    private double meanMotion;

    /** Flag to take into account only M-dailies harmonic tesserals for short periodic perturbations.  */
    private final boolean mDailiesOnly;

    /** Maximum value for j.
     * <p>
     * jmax = maxDegreeTesseralSP + maxEccPowTesseralSP, no more than 12
     * </p>
     * */
    private int jMax;

    /** List of non resonant orders with j != 0. */
    private final SortedMap<Integer, List<Integer> > nonResOrders;

    /** A two dimensional array that contains the objects needed to build the Hansen coefficients. <br/>
     * The indexes are s + maxDegree and j */
    private HansenTesseralLinear[][] hansenObjects;

    /** The C<sub>i</sub><sup>j</sup><sup>m</sup> and S<sub>i</sub><sup>j</sup><sup>m</sup> coefficients
     * used to compute the short-periodic tesseral contribution. */
    private TesseralShortPeriodicCoefficients tesseralSPCoefs;

    /** The frame used to describe the orbits. */
    private Frame frame;

    /** Single constructor.
     *  @param centralBodyFrame rotating body frame
     *  @param centralBodyRotationRate central body rotation rate (rad/s)
     *  @param provider provider for spherical harmonics
     *  @param mDailiesOnly if true only M-dailies tesseral harmonics are taken into account for short periodics
     */
    TesseralContribution(final Frame centralBodyFrame,
                                final double centralBodyRotationRate,
                                final UnnormalizedSphericalHarmonicsProvider provider,
                                final boolean mDailiesOnly) {

        // Central body rotating frame
        this.bodyFrame = centralBodyFrame;

        //Save the rotation rate
        this.centralBodyRotationRate = centralBodyRotationRate;

        // Central body rotation period in seconds
        this.bodyPeriod = MathUtils.TWO_PI / centralBodyRotationRate;

        // Provider for spherical harmonics
        this.provider  = provider;
        this.maxDegree = provider.getMaxDegree();
        this.maxOrder  = provider.getMaxOrder();

        //set the maximum degree order for short periodics
        this.maxDegreeTesseralSP = FastMath.min(maxDegree, MAX_DEGREE_TESSERAL_SP);
        this.maxDegreeMdailyTesseralSP = FastMath.min(maxDegree, MAX_DEGREE_MDAILY_TESSERAL_SP);
        this.maxOrderTesseralSP = FastMath.min(maxOrder, MAX_ORDER_TESSERAL_SP);
        this.maxOrderMdailyTesseralSP = FastMath.min(maxOrder, MAX_ORDER_MDAILY_TESSERAL_SP);

        // set the maximum value for eccentricity power
        this.maxEccPowTesseralSP = MAX_ECCPOWER_SP;
        this.maxEccPowMdailyTesseralSP = FastMath.min(maxDegreeMdailyTesseralSP - 2, MAX_ECCPOWER_SP);
        this.jMax = FastMath.min(MAXJ, maxDegreeTesseralSP + maxEccPowTesseralSP);

        // m-daylies only
        this.mDailiesOnly = mDailiesOnly;

        // Initialize default values
        this.resOrders = new ArrayList<Integer>();
        this.nonResOrders = new TreeMap<Integer, List <Integer> >();
        this.maxEccPow = 0;
        this.maxHansen = 0;

       // Factorials computation
        final int maxFact = 2 * maxDegree + 1;
        this.fact = new double[maxFact];
        fact[0] = 1;
        for (int i = 1; i < maxFact; i++) {
            fact[i] = i * fact[i - 1];
        }
    }

    /** {@inheritDoc} */
    @Override
    public void initialize(final AuxiliaryElements aux, final boolean meanOnly)
        throws OrekitException {

        // Keplerian period
        orbitPeriod = aux.getKeplerianPeriod();

        // orbit frame
        frame = aux.getFrame();

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
        jMax = FastMath.min(MAXJ, maxDegree + maxEccPow);

        // Ratio of satellite to central body periods to define resonant terms
        ratio = orbitPeriod / bodyPeriod;

        // Compute the resonant tesseral harmonic terms if not set by the user
        getResonantAndNonResonantTerms(meanOnly);

        //initialize the HansenTesseralLinear objects needed
        createHansenObjects(meanOnly);

        if (!meanOnly) {
            //Initialize the Tesseral Short Periodics coefficient class
            tesseralSPCoefs = new TesseralShortPeriodicCoefficients(jMax,
                    FastMath.max(maxOrderTesseralSP, maxOrderMdailyTesseralSP), INTERPOLATION_POINTS);
        }
    }

    /** Create the objects needed for linear transformation.
     *
     * <p>
     * Each {@link org.orekit.propagation.semianalytical.dsst.utilities.hansenHansenTesseralLinear HansenTesseralLinear} uses
     * a fixed value for s and j. Since j varies from -maxJ to +maxJ and s varies from -maxDegree to +maxDegree,
     * a 2 * maxDegree + 1 x 2 * maxJ + 1 matrix of objects should be created. The size of this matrix can be reduced
     * by taking into account the expression (2.7.3-2). This means that is enough to create the objects for  positive
     * values of j and all values of s.
     * </p>
     *
     * @param meanOnly create only the objects required for the mean contribution
     */
    private void createHansenObjects(final boolean meanOnly) {
        //Allocate the two dimensional array
        final int rows     = 2 * maxDegree + 1;
        final int columns  = jMax + 1;
        this.hansenObjects = new HansenTesseralLinear[rows][columns];

        if (meanOnly) {
            // loop through the resonant orders
            for (int m : resOrders) {
                //Compute the corresponding j term
                final int j = FastMath.max(1, (int) FastMath.round(ratio * m));

                //Compute the sMin and sMax values
                final int sMin = FastMath.min(maxEccPow - j, maxDegree);
                final int sMax = FastMath.min(maxEccPow + j, maxDegree);

                //loop through the s values
                for (int s = 0; s <= sMax; s++) {
                    //Compute the n0 value
                    final int n0 = FastMath.max(FastMath.max(2, m), s);

                    //Create the object for the pair j,s
                    this.hansenObjects[s + maxDegree][j] = new HansenTesseralLinear(maxDegree, s, j, n0, maxHansen);

                    if (s > 0 && s <= sMin) {
                        //Also create the object for the pair j, -s
                        this.hansenObjects[maxDegree - s][j] =  new HansenTesseralLinear(maxDegree, -s, j, n0, maxHansen);
                    }
                }
            }
        } else {
            // create all objects
            for (int j = 0; j <= jMax; j++) {
                for (int s = -maxDegree; s <= maxDegree; s++) {
                    //Compute the n0 value
                    final int n0 = FastMath.max(2, FastMath.abs(s));

                    this.hansenObjects[s + maxDegree][j] = new HansenTesseralLinear(maxDegree, s, j, n0, maxHansen);
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
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
        e2 = ecc * ecc;

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
        // A = sqrt(μ * a)
        final double A = aux.getA();
        // B = sqrt(1 - h² - k²)
        final double B = aux.getB();
        // C = 1 + p² + q²
        final double C = aux.getC();
        // Common factors from equinoctial coefficients
        // 2 * a / A
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

        // &Chi; = 1 / B
        chi = 1. / B;
        chi2 = chi * chi;

        //mean motion n
        meanMotion = aux.getMeanMotion();
    }

    /** {@inheritDoc} */
    @Override
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
    @Override
    public double[] getShortPeriodicVariations(final AbsoluteDate date,
                                               final double[] meanElements)
        throws OrekitException {

        // Initialise the short periodic variations
        final double[] shortPeriodicVariation = new double[] {0., 0., 0., 0., 0., 0.};

        // Compute only if there is at least one non-resonant tesseral or
        // only the m-daily tesseral should be taken into account
        if (!nonResOrders.isEmpty() || mDailiesOnly) {

            // Build an Orbit object from the mean elements
            final Orbit meanOrbit = OrbitType.EQUINOCTIAL.mapArrayToOrbit(
                    meanElements, PositionAngle.MEAN, date, provider.getMu(), this.frame);

            //Build an auxiliary object
            final AuxiliaryElements aux = new AuxiliaryElements(meanOrbit, I);

            // Central body rotation angle from equation 2.7.1-(3)(4).
            final Transform t = bodyFrame.getTransformTo(aux.getFrame(), aux.getDate());
            final Vector3D xB = t.transformVector(Vector3D.PLUS_I);
            final Vector3D yB = t.transformVector(Vector3D.PLUS_J);
            final double currentTheta = FastMath.atan2(-f.dotProduct(yB) + I * g.dotProduct(xB),
                                                        f.dotProduct(xB) + I * g.dotProduct(yB));

            //Add the m-daily contribution
            for (int m = 1; m <= maxOrderMdailyTesseralSP; m++) {
                // Phase angle
                final double jlMmt  = -m * currentTheta;
                final double sinPhi = FastMath.sin(jlMmt);
                final double cosPhi = FastMath.cos(jlMmt);

                // compute contribution for each element
                for (int i = 0; i < 6; i++) {
                    shortPeriodicVariation[i] += tesseralSPCoefs.getCijm(i, 0, m, date) * cosPhi +
                                                 tesseralSPCoefs.getSijm(i, 0, m, date) * sinPhi;
                }
            }

            // loop through all non-resonant (j,m) pairs
            for (final Map.Entry<Integer, List<Integer>> entry : nonResOrders.entrySet()) {
                final int           m     = entry.getKey();
                final List<Integer> listJ = entry.getValue();

                for (int j : listJ) {
                    // Phase angle
                    final double jlMmt  = j * meanElements[5] - m * currentTheta;
                    final double sinPhi = FastMath.sin(jlMmt);
                    final double cosPhi = FastMath.cos(jlMmt);

                    // compute contribution for each element
                    for (int i = 0; i < 6; i++) {
                        shortPeriodicVariation[i] += tesseralSPCoefs.getCijm(i, j, m, date) * cosPhi +
                                                     tesseralSPCoefs.getSijm(i, j, m, date) * sinPhi;
                    }

                }
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
        // Initialise the Hansen coefficients
        for (int s = -maxDegree; s <= maxDegree; s++) {
            // coefficients with j == 0 are always needed
            this.hansenObjects[s + maxDegree][0].computeInitValues(e2, chi, chi2);
            if (!mDailiesOnly) {
                // initialize other objects only if required
                for (int j = 1; j <= jMax; j++) {
                    this.hansenObjects[s + maxDegree][j].computeInitValues(e2, chi, chi2);
                }
            }
        }

        // Compute coefficients
        tesseralSPCoefs.computeCoefficients(state.getDate());
    }

     /**
      * Get the resonant and non-resonant tesseral terms in the central body spherical harmonic field.
      *
      * @param resonantOnly extract only resonant terms
      */
    private void getResonantAndNonResonantTerms(final boolean resonantOnly) {

        // Compute natural resonant terms
        final double tolerance = 1. / FastMath.max(MIN_PERIOD_IN_SAT_REV,
                                                   MIN_PERIOD_IN_SECONDS / orbitPeriod);

        // Search the resonant orders in the tesseral harmonic field
        resOrders.clear();
        nonResOrders.clear();
        for (int m = 1; m <= maxOrder; m++) {
            final double resonance = ratio * m;
            int jRes = 0;
            final int jComputedRes = (int) FastMath.round(resonance);
            if (jComputedRes > 0 && jComputedRes <= jMax && FastMath.abs(resonance - jComputedRes) <= tolerance) {
                // Store each resonant index and order
                resOrders.add(m);
                jRes = jComputedRes;
            }

            if (!resonantOnly && !mDailiesOnly && m <= maxOrderTesseralSP) {
                //compute non resonant orders in the tesseral harmonic field
                final List<Integer> listJofM = new ArrayList<Integer>();
                //for the moment we take only the pairs (j,m) with |j| <= maxDegree + maxEccPow (from |s-j| <= maxEccPow and |s| <= maxDegree)
                for (int j = -jMax; j <= jMax; j++) {
                    if (j != 0 && j != jRes) {
                        listJofM.add(j);
                    }
                }

                nonResOrders.put(m, listJofM);
            }
        }
    }

    /** Computes the potential U derivatives.
     *  <p>The following elements are computed from expression 3.3 - (4).
     *  <pre>
     *  dU / da
     *  dU / dh
     *  dU / dk
     *  dU / dλ
     *  dU / dα
     *  dU / dβ
     *  dU / dγ
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

                    //Compute the initial values for Hansen coefficients using newComb operators
                    this.hansenObjects[s + maxDegree][j].computeInitValues(e2, chi, chi2);

                    // n-SUM for s positive
                    final double[][] nSumSpos = computeNSum(date, j, m, s, maxDegree,
                                                            roaPow, ghMSJ, gammaMNS);
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
                        //Compute the initial values for Hansen coefficients using newComb operators
                        this.hansenObjects[maxDegree - s][j].computeInitValues(e2, chi, chi2);

                        final double[][] nSumSneg = computeNSum(date, j, m, -s, maxDegree,
                                                                roaPow, ghMSJ, gammaMNS);
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
     *  @param maxN maximum possible value for <i>n</i> index
     *  @param roaPow powers of R/a up to degree <i>n</i>
     *  @param ghMSJ G<sup>j</sup><sub>m,s</sub> and H<sup>j</sup><sub>m,s</sub> polynomials
     *  @param gammaMNS &Gamma;<sup>m</sup><sub>n,s</sub>(γ) function
     *  @return Components of U<sub>n</sub> derivatives for fixed j, m, s
     * @throws OrekitException if some error occurred
     */
    private double[][] computeNSum(final AbsoluteDate date,
                                   final int j, final int m, final int s, final int maxN, final double[] roaPow,
                                   final GHmsjPolynomials ghMSJ, final GammaMnsFunction gammaMNS)
        throws OrekitException {

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

        //Get the corresponding Hansen object
        final int sIndex = maxDegree + (j < 0 ? -s : s);
        final int jIndex = FastMath.abs(j);
        final HansenTesseralLinear hans = this.hansenObjects[sIndex][jIndex];

        // n-SUM from nmin to N
        for (int n = nmin; n <= maxN; n++) {
            // If (n - s) is odd, the contribution is null because of Vmns
            if ((n - s) % 2 == 0) {

                // Vmns coefficient
                final double fns    = fact[n + FastMath.abs(s)];
                final double vMNS   = CoefficientsFactory.getVmns(m, n, s, fns, fact[n - m]);

                // Inclination function Gamma and derivative
                final double gaMNS  = gammaMNS.getValue(m, n, s);
                final double dGaMNS = gammaMNS.getDerivative(m, n, s);

                // Hansen kernel value and derivative
                final double kJNS   = hans.getValue(-n - 1, chi);
                final double dkJNS  = hans.getDerivative(-n - 1, chi);

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
                final DerivativeStructure jacobi =
                        JacobiPolynomials.getValue(l, v , w, new DerivativeStructure(1, 1, 0, gamma));

                // Geopotential coefficients
                final double cnm = harmonics.getUnnormalizedCnm(n, m);
                final double snm = harmonics.getUnnormalizedSnm(n, m);

                // Common factors from expansion of equations 3.3-4
                final double cf_0      = roaPow[n] * Im * vMNS;
                final double cf_1      = cf_0 * gaMNS * jacobi.getValue();
                final double cf_2      = cf_1 * kJNS;
                final double gcPhs     = gMSJ * cnm + hMSJ * snm;
                final double gsMhc     = gMSJ * snm - hMSJ * cnm;
                final double dKgcPhsx2 = 2. * dkJNS * gcPhs;
                final double dKgsMhcx2 = 2. * dkJNS * gsMhc;
                final double dUdaCoef  = (n + 1) * cf_2;
                final double dUdlCoef  = j * cf_2;
                final double dUdGaCoef = cf_0 * kJNS * (jacobi.getValue() * dGaMNS + gaMNS * jacobi.getPartialDerivative(1));

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

    /** {@inheritDoc} */
    @Override
    public void registerAttitudeProvider(final AttitudeProvider attitudeProvider) {
        //nothing is done since this contribution is not sensitive to attitude
    }

    /** {@inheritDoc} */
    @Override
    public void resetShortPeriodicsCoefficients() {
        if (tesseralSPCoefs != null) {
            tesseralSPCoefs.resetCoefficients();
        }
    }

    /** Compute the C<sup>j</sup> and the S<sup>j</sup> coefficients.
     *  <p>
     *  Those coefficients are given in Danielson paper by substituting the
     *  disturbing function (2.7.1-16) with m != 0 into (2.2-10)
     *  </p>
     */
    private class FourierCjSjCoefficients {

        /** Absolute limit for j ( -jMax <= j <= jMax ).  */
        private final int jMax;

        /** The C<sub>i</sub><sup>jm</sup> coefficients.
         * <p>
         * The index order is [m][j][i] <br/>
         * The i index corresponds to the C<sub>i</sub><sup>jm</sup> coefficients used to
         * compute the following: <br/>
         * - da/dt <br/>
         * - dk/dt <br/>
         * - dh/dt / dk <br/>
         * - dq/dt <br/>
         * - dp/dt / dα <br/>
         * - dλ/dt / dβ <br/>
         * </p>
         */
        private final double[][][] cCoef;

        /** The S<sub>i</sub><sup>jm</sup> coefficients.
         * <p>
         * The index order is [m][j][i] <br/>
         * The i index corresponds to the C<sub>i</sub><sup>jm</sup> coefficients used to
         * compute the following: <br/>
         * - da/dt <br/>
         * - dk/dt <br/>
         * - dh/dt / dk <br/>
         * - dq/dt <br/>
         * - dp/dt / dα <br/>
         * - dλ/dt / dβ <br/>
         * </p>
         */
        private final double[][][] sCoef;

        /** G<sub>ms</sub><sup>j</sup> and H<sub>ms</sub><sup>j</sup> polynomials. */
        private GHmsjPolynomials ghMSJ;

        /** &Gamma;<sub>ns</sub><sup>m</sup> function. */
        private GammaMnsFunction gammaMNS;

        /** R / a up to power degree. */
        private final double[] roaPow;

        /** Create a set of C<sub>i</sub><sup>jm</sup> and S<sub>i</sub><sup>jm</sup> coefficients.
         *  @param jMax absolute limit for j ( -jMax <= j <= jMax )
         *  @param mMax maximum value for m
         */
        FourierCjSjCoefficients(final int jMax, final int mMax) {
            // initialise fields
            final int rows    = mMax + 1;
            final int columns = 2 * jMax + 1;
            this.jMax         = jMax;
            this.cCoef        = new double[rows][columns][6];
            this.sCoef        = new double[rows][columns][6];
            this.roaPow       = new double[maxDegree + 1];
            roaPow[0] = 1.;
        }

        /**
         * Generate the coefficients.
         * @param date the current date
         * @throws OrekitException if an error occurs while generating the coefficients
         */
        public void generateCoefficients(final AbsoluteDate date) throws OrekitException {
            // Compute only if there is at least one non-resonant tesseral
            if (!nonResOrders.isEmpty() || mDailiesOnly) {
                // Gmsj and Hmsj polynomials
                ghMSJ = new GHmsjPolynomials(k, h, alpha, beta, I);

                // GAMMAmns function
                gammaMNS = new GammaMnsFunction(fact, gamma, I);

                final int maxRoaPower = FastMath.max(maxDegreeTesseralSP, maxDegreeMdailyTesseralSP);

                // R / a up to power degree
                for (int i = 1; i <= maxRoaPower; i++) {
                    roaPow[i] = roa * roaPow[i - 1];
                }

                //generate the m-daily coefficients
                for (int m = 1; m <= maxOrderMdailyTesseralSP; m++) {
                    buildFourierCoefficients(date, m, 0, maxDegreeMdailyTesseralSP);
                }

                // generate the other coefficients only if required
                if (!mDailiesOnly) {
                    for (int m: nonResOrders.keySet()) {
                        final List<Integer> listJ = nonResOrders.get(m);

                        for (int j: listJ) {

                            buildFourierCoefficients(date, m, j, maxDegreeTesseralSP);
                        }
                    }
                }
            }
        }

        /** Build a set of fourier coefficients for a given m and j.
         *
         * @param date the date of the coefficients
         * @param m m index
         * @param j j index
         * @param maxN  maximum value for n index
         * @throws OrekitException in case of Hansen kernel generation error
         */
        private void buildFourierCoefficients(final AbsoluteDate date,
               final int m, final int j, final int maxN) throws OrekitException {
            // Potential derivatives components for a given non-resonant pair {j,m}
            double dRdaCos  = 0.;
            double dRdaSin  = 0.;
            double dRdhCos  = 0.;
            double dRdhSin  = 0.;
            double dRdkCos  = 0.;
            double dRdkSin  = 0.;
            double dRdlCos  = 0.;
            double dRdlSin  = 0.;
            double dRdAlCos = 0.;
            double dRdAlSin = 0.;
            double dRdBeCos = 0.;
            double dRdBeSin = 0.;
            double dRdGaCos = 0.;
            double dRdGaSin = 0.;

            // s-SUM from -sMin to sMax
            final int sMin = j == 0 ? maxEccPowMdailyTesseralSP : maxEccPowTesseralSP;
            final int sMax = j == 0 ? maxEccPowMdailyTesseralSP : maxEccPowTesseralSP;
            for (int s = 0; s <= sMax; s++) {

                // n-SUM for s positive
                final double[][] nSumSpos = computeNSum(date, j, m, s, maxN,
                                                        roaPow, ghMSJ, gammaMNS);
                dRdaCos  += nSumSpos[0][0];
                dRdaSin  += nSumSpos[0][1];
                dRdhCos  += nSumSpos[1][0];
                dRdhSin  += nSumSpos[1][1];
                dRdkCos  += nSumSpos[2][0];
                dRdkSin  += nSumSpos[2][1];
                dRdlCos  += nSumSpos[3][0];
                dRdlSin  += nSumSpos[3][1];
                dRdAlCos += nSumSpos[4][0];
                dRdAlSin += nSumSpos[4][1];
                dRdBeCos += nSumSpos[5][0];
                dRdBeSin += nSumSpos[5][1];
                dRdGaCos += nSumSpos[6][0];
                dRdGaSin += nSumSpos[6][1];

                // n-SUM for s negative
                if (s > 0 && s <= sMin) {
                    final double[][] nSumSneg = computeNSum(date, j, m, -s, maxN,
                                                            roaPow, ghMSJ, gammaMNS);
                    dRdaCos  += nSumSneg[0][0];
                    dRdaSin  += nSumSneg[0][1];
                    dRdhCos  += nSumSneg[1][0];
                    dRdhSin  += nSumSneg[1][1];
                    dRdkCos  += nSumSneg[2][0];
                    dRdkSin  += nSumSneg[2][1];
                    dRdlCos  += nSumSneg[3][0];
                    dRdlSin  += nSumSneg[3][1];
                    dRdAlCos += nSumSneg[4][0];
                    dRdAlSin += nSumSneg[4][1];
                    dRdBeCos += nSumSneg[5][0];
                    dRdBeSin += nSumSneg[5][1];
                    dRdGaCos += nSumSneg[6][0];
                    dRdGaSin += nSumSneg[6][1];
                }
            }
            dRdaCos  *= -moa / a;
            dRdaSin  *= -moa / a;
            dRdhCos  *=  moa;
            dRdhSin  *=  moa;
            dRdkCos  *=  moa;
            dRdkSin *=  moa;
            dRdlCos *=  moa;
            dRdlSin *=  moa;
            dRdAlCos *=  moa;
            dRdAlSin *=  moa;
            dRdBeCos *=  moa;
            dRdBeSin *=  moa;
            dRdGaCos *=  moa;
            dRdGaSin *=  moa;

            // Compute the cross derivative operator :
            final double RAlphaGammaCos   = alpha * dRdGaCos - gamma * dRdAlCos;
            final double RAlphaGammaSin   = alpha * dRdGaSin - gamma * dRdAlSin;
            final double RAlphaBetaCos    = alpha * dRdBeCos - beta  * dRdAlCos;
            final double RAlphaBetaSin    = alpha * dRdBeSin - beta  * dRdAlSin;
            final double RBetaGammaCos    =  beta * dRdGaCos - gamma * dRdBeCos;
            final double RBetaGammaSin    =  beta * dRdGaSin - gamma * dRdBeSin;
            final double RhkCos           =     h * dRdkCos  -     k * dRdhCos;
            final double RhkSin           =     h * dRdkSin  -     k * dRdhSin;
            final double pRagmIqRbgoABCos = (p * RAlphaGammaCos - I * q * RBetaGammaCos) * ooAB;
            final double pRagmIqRbgoABSin = (p * RAlphaGammaSin - I * q * RBetaGammaSin) * ooAB;
            final double RhkmRabmdRdlCos  =  RhkCos - RAlphaBetaCos - dRdlCos;
            final double RhkmRabmdRdlSin  =  RhkSin - RAlphaBetaSin - dRdlSin;

            // da/dt
            cCoef[m][j + jMax][0] = ax2oA * dRdlCos;
            sCoef[m][j + jMax][0] = ax2oA * dRdlSin;

            // dk/dt
            cCoef[m][j + jMax][1] = -(BoA * dRdhCos + h * pRagmIqRbgoABCos + k * BoABpo * dRdlCos);
            sCoef[m][j + jMax][1] = -(BoA * dRdhSin + h * pRagmIqRbgoABSin + k * BoABpo * dRdlSin);

            // dh/dt
            cCoef[m][j + jMax][2] = BoA * dRdkCos + k * pRagmIqRbgoABCos - h * BoABpo * dRdlCos;
            sCoef[m][j + jMax][2] = BoA * dRdkSin + k * pRagmIqRbgoABSin - h * BoABpo * dRdlSin;

            // dq/dt
            cCoef[m][j + jMax][3] = Co2AB * (q * RhkmRabmdRdlCos - I * RAlphaGammaCos);
            sCoef[m][j + jMax][3] = Co2AB * (q * RhkmRabmdRdlSin - I * RAlphaGammaSin);

            // dp/dt
            cCoef[m][j + jMax][4] = Co2AB * (p * RhkmRabmdRdlCos - RBetaGammaCos);
            sCoef[m][j + jMax][4] = Co2AB * (p * RhkmRabmdRdlSin - RBetaGammaSin);

            // dλ/dt
            cCoef[m][j + jMax][5] = -ax2oA * dRdaCos + BoABpo * (h * dRdhCos + k * dRdkCos) + pRagmIqRbgoABCos;
            sCoef[m][j + jMax][5] = -ax2oA * dRdaSin + BoABpo * (h * dRdhSin + k * dRdkSin) + pRagmIqRbgoABSin;
        }

        /** Get the coefficient C<sub>i</sub><sup>jm</sup>.
         * @param i i index - corresponds to the required variation
         * @param j j index
         * @param m m index
         * @return the coefficient C<sub>i</sub><sup>jm</sup>
         */
        public double getCijm(final int i, final int j, final int m) {
            return cCoef[m][j + jMax][i];
        }

        /** Get the coefficient S<sub>i</sub><sup>jm</sup>.
         * @param i i index - corresponds to the required variation
         * @param j j index
         * @param m m index
         * @return the coefficient S<sub>i</sub><sup>jm</sup>
         */
        public double getSijm(final int i, final int j, final int m) {
            return sCoef[m][j + jMax][i];
        }
    }

    /** The C<sup>i</sup><sub>m</sub><sub>t</sub> and S<sup>i</sup><sub>m</sub><sub>t</sub> coefficients used to compute
     * the short-periodic zonal contribution.
     *   <p>
     *  Those coefficients are given by expression 2.5.4-5 from the Danielsno paper.
     *   </p>
     *
     * @author Sorin Scortan
     *
     */
    private class TesseralShortPeriodicCoefficients {

        /** The coefficients C<sub>i</sub><sup>j</sup><sup>m</sup>.
         * <p>
         * The index order is cijm[m][j][i] <br/>
         * i corresponds to the equinoctial element, as follows: <br/>
         * - i=0 for a <br/>
         * - i=1 for k <br/>
         * - i=2 for h <br/>
         * - i=3 for q <br/>
         * - i=4 for p <br/>
         * - i=5 for λ <br/>
         * </p>
         */
        private final ShortPeriodicsInterpolatedCoefficient[][][] cijm;

        /** The coefficients S<sub>i</sub><sup>j</sup><sup>m</sup>.
         * <p>
         * The index order is sijm[m][j][i] <br/>
         * i corresponds to the equinoctial element, as follows: <br/>
         * - i=0 for a <br/>
         * - i=1 for k <br/>
         * - i=2 for h <br/>
         * - i=3 for q <br/>
         * - i=4 for p <br/>
         * - i=5 for λ <br/>
         * </p>
         */
        private final ShortPeriodicsInterpolatedCoefficient[][][] sijm;

        /** Absolute limit for j ( -jMax <= j <= jMax ).  */
        private final int jMax;

        /** Maximum value for m.  */
        private final int mMax;

        /** The fourier coefficients. */
        private final FourierCjSjCoefficients cjsjFourier;

        /** 3n / 2a. */
        private double tnota;

        /** Constructor.
         * @param jMax absolute limit for j ( -jMax <= j <= jMax )
         * @param mMax maximum value for m
         * @param interpolationPoints number of points used in the interpolation process
         */
        TesseralShortPeriodicCoefficients(final int jMax, final int mMax, final int interpolationPoints) {

            // Initialize fields
            final int rows    = mMax + 1;
            final int columns = 2 * jMax + 1;
            this.jMax = jMax;
            this.mMax = mMax;
            this.cijm = new ShortPeriodicsInterpolatedCoefficient[rows][columns][6];
            this.sijm = new ShortPeriodicsInterpolatedCoefficient[rows][columns][6];
            this.cjsjFourier = new FourierCjSjCoefficients(jMax, mMax);

            for (int m = 1; m <= mMax; m++) {
                for (int j = -jMax; j <= jMax; j++) {
                    for (int i = 0; i < 6; i++) {
                        this.cijm[m][j + jMax][i] = new ShortPeriodicsInterpolatedCoefficient(interpolationPoints);
                        this.sijm[m][j + jMax][i] = new ShortPeriodicsInterpolatedCoefficient(interpolationPoints);
                    }
                }
            }
        }

        /** Compute the short periodic coefficients.
         *
         * @param date the current date
         * @throws OrekitException if an error occurs
         */
        public void computeCoefficients(final AbsoluteDate date)
            throws OrekitException {
            // Compute only if there is at least one non-resonant tesseral
            if (!nonResOrders.isEmpty() || mDailiesOnly) {
                // Generate the fourrier coefficients
                cjsjFourier.generateCoefficients(date);

                // the coefficient 3n / 2a
                tnota = 1.5 * meanMotion / a;

                // build the mDaily coefficients
                for (int m = 1; m <= maxOrderMdailyTesseralSP; m++) {
                    // build the coefficients
                    buildCoefficients(date, m, 0);
                }

                if (!mDailiesOnly) {
                    // generate the other coefficients, if required
                    for (int m: nonResOrders.keySet()) {
                        final List<Integer> listJ = nonResOrders.get(m);

                        for (int j: listJ) {
                            // build the coefficients
                            buildCoefficients(date, m, j);
                        }
                    }
                }
            }

        }

        /** Build a set of coefficients.
         *
         * @param date the current date
         * @param m m index
         * @param j j index
         */
        private void buildCoefficients(final AbsoluteDate date, final int m, final int j) {
            // Create local arrays
            final double[] currentCijm = new double[] {0., 0., 0., 0., 0., 0.};
            final double[] currentSijm = new double[] {0., 0., 0., 0., 0., 0.};

            // compute the term 1 / (jn - mθ<sup>.</sup>)
            final double oojnmt = 1. / (j * meanMotion - m * centralBodyRotationRate);

            // initialise the coeficients
            for (int i = 0; i < 6; i++) {
                currentCijm[i] = -cjsjFourier.getSijm(i, j, m);
                currentSijm[i] = cjsjFourier.getCijm(i, j, m);
            }
            // Add the separate part for δ<sub>6i</sub>
            currentCijm[5] += tnota * oojnmt * cjsjFourier.getCijm(0, j, m);
            currentSijm[5] += tnota * oojnmt * cjsjFourier.getSijm(0, j, m);

            //Multiply by 1 / (jn - mθ<sup>.</sup>)
            for (int i = 0; i < 6; i++) {
                currentCijm[i] *= oojnmt;
                currentSijm[i] *= oojnmt;
            }

            // Add the coefficients to the interpolation grid
            for (int i = 0; i < 6; i++) {
                cijm[m][j + jMax][i].addGridPoint(date, currentCijm[i]);
                sijm[m][j + jMax][i].addGridPoint(date, currentSijm[i]);
            }
        }

        /** Reset the coefficients.
         * <p>
         * For each coefficient, clear history of computed points
         * </p>
         */
        public void resetCoefficients() {
            for (int m = 1; m <= mMax; m++) {
                for (int j = -jMax; j <= jMax; j++) {
                    for (int i = 0; i < 6; i++) {
                        this.cijm[m][j + jMax][i].clearHistory();
                        this.sijm[m][j + jMax][i].clearHistory();
                    }
                }
            }
        }

        /** Get C<sub>i</sub><sup>j</sup><sup>m</sup>.
         *
         * @param i i index
         * @param j j index
         * @param m m index
         * @param date the date
         * @return C<sub>i</sub><sup>j</sup><sup>m</sup>
         */
        public double getCijm(final int i, final int j, final int m, final AbsoluteDate date) {
            return cijm[m][j + jMax][i].value(date);
        }

        /** Get S<sub>i</sub><sup>j</sup><sup>m</sup>.
         *
         * @param i i index
         * @param j j index
         * @param m m index
         * @param date the date
         * @return S<sub>i</sub><sup>j</sup><sup>m</sup>
         */
        public double getSijm(final int i, final int j, final int m, final AbsoluteDate date) {
            return sijm[m][j + jMax][i].value(date);
        }
    }
}
