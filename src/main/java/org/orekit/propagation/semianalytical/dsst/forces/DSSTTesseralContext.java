/* Copyright 2002-2018 CS Systèmes d'Information
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

import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.propagation.semianalytical.dsst.utilities.hansen.HansenTesseralLinear;

/** This class is a container for the attributes of
 * {@link org.orekit.propagation.semianalytical.dsst.forces.DSSTTesseral DSSTTesseral}.
 * <p>
 * It replaces the last version of the method
 * {@link  org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel#initializeStep(AuxiliaryElements) initializeStep(AuxiliaryElements)}.
 * </p>
 */
class DSSTTesseralContext extends ForceModelContext {

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
    private static final int I = 1;

    /** Minimum period for analytically averaged high-order resonant
     *  central body spherical harmonics in seconds.
     */
    private static final double MIN_PERIOD_IN_SECONDS = 864000.;

    /** Minimum period for analytically averaged high-order resonant
     *  central body spherical harmonics in satellite revolutions.
     */
    private static final double MIN_PERIOD_IN_SAT_REV = 10.;

    /** A = sqrt(μ * a). */
    private final double A;

    // Common factors for potential computation
    /** &Chi; = 1 / sqrt(1 - e²) = 1 / B. */
    private double chi;

    /** &Chi;². */
    private double chi2;

    /** Central body rotation angle θ. */
    private double theta;

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

    /** Maximum power of the eccentricity to use in summation over s. */
    private int maxEccPow;

    /** Maximum power of the eccentricity to use in Hansen coefficient Kernel expansion. */
    private int maxHansen;

    /** Keplerian period. */
    private double orbitPeriod;

    /** Maximal degree to consider for harmonics potential. */
    private final int maxDegree;

    /** Maximal order to consider for harmonics potential. */
    private final int maxOrder;

    /** A two dimensional array that contains the objects needed to build the Hansen coefficients. <br/>
     * The indexes are s + maxDegree and j */
    private HansenTesseralLinear[][] hansenObjects;

    /** Ratio of satellite period to central body rotation period. */
    private double ratio;

    /** Factory for the DerivativeStructure instances. */
    private final DSFactory factory;

    /** List of resonant orders. */
    private final List<Integer> resOrders;

    /** Simple constructor.
     * Performs initialization at each integration step for the current force model.
     * This method aims at being called before mean elements rates computation
     * @param auxiliaryElements auxiliary elements related to the current orbit
     * @param meanOnly create only the objects required for the mean contribution
     * @param centralBodyFrame rotating body frame
     * @param provider provider for spherical harmonics
     * @param maxFrequencyShortPeriodics maximum value for j
     * @param bodyPeriod central body rotation period (seconds)
     * @param parameters values of the force model parameters
     * @throws OrekitException if some specific error occurs
     */
    DSSTTesseralContext(final AuxiliaryElements auxiliaryElements,
                        final boolean meanOnly,
                        final Frame centralBodyFrame,
                        final UnnormalizedSphericalHarmonicsProvider provider,
                        final int maxFrequencyShortPeriodics,
                        final double bodyPeriod,
                        final double[] parameters)
        throws OrekitException {

        super(auxiliaryElements);

        this.maxEccPow = 0;
        this.maxHansen = 0;
        this.maxDegree = provider.getMaxDegree();
        this.maxOrder  = provider.getMaxOrder();
        this.factory   = new DSFactory(1, 1);
        this.resOrders = new ArrayList<Integer>();

        final double mu = parameters[0];

        A = FastMath.sqrt(mu * auxiliaryElements.getSma());

        // Eccentricity square
        e2 = auxiliaryElements.getEcc() * auxiliaryElements.getEcc();

        // Central body rotation angle from equation 2.7.1-(3)(4).
        final Transform t = centralBodyFrame.getTransformTo(auxiliaryElements.getFrame(), auxiliaryElements.getDate());
        final Vector3D xB = t.transformVector(Vector3D.PLUS_I);
        final Vector3D yB = t.transformVector(Vector3D.PLUS_J);
        theta = FastMath.atan2(-auxiliaryElements.getVectorF().dotProduct(yB) + I * auxiliaryElements.getVectorG().dotProduct(xB),
                               auxiliaryElements.getVectorF().dotProduct(xB) + I * auxiliaryElements.getVectorG().dotProduct(yB));

        // Common factors from equinoctial coefficients
        // 2 * a / A
        ax2oA  = 2. * auxiliaryElements.getSma() / A;
        // B / A
        BoA    = auxiliaryElements.getB() / A;
        // 1 / AB
        ooAB   = 1. / (A * auxiliaryElements.getB());
        // C / 2AB
        Co2AB  = auxiliaryElements.getC() * ooAB / 2.;
        // B / (A * (1 + B))
        BoABpo = BoA / (1. + auxiliaryElements.getB());
        // &mu / a
        moa    = mu / auxiliaryElements.getSma();
        // R / a
        roa    = provider.getAe() / auxiliaryElements.getSma();

        // &Chi; = 1 / B
        chi  = 1. / auxiliaryElements.getB();
        chi2 = chi * chi;

        // Set the highest power of the eccentricity in the analytical power
        // series expansion for the averaged high order resonant central body
        // spherical harmonic perturbation
        final double e = auxiliaryElements.getEcc();
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

        // Keplerian period
        orbitPeriod = auxiliaryElements.getKeplerianPeriod();

        // Ratio of satellite to central body periods to define resonant terms
        ratio = orbitPeriod / bodyPeriod;

        // Compute natural resonant terms
        final double tolerance = 1. / FastMath.max(MIN_PERIOD_IN_SAT_REV,
                                                   MIN_PERIOD_IN_SECONDS / orbitPeriod);

        // Search the resonant orders in the tesseral harmonic field
        resOrders.clear();
        for (int m = 1; m <= maxOrder; m++) {
            final double resonance = ratio * m;
            final int jComputedRes = (int) FastMath.round(resonance);
            if (jComputedRes > 0 && jComputedRes <= maxFrequencyShortPeriodics && FastMath.abs(resonance - jComputedRes) <= tolerance) {
                // Store each resonant index and order
                this.resOrders.add(m);
            }
        }

        //Allocate the two dimensional array
        final int rows     = 2 * maxDegree + 1;
        final int columns  = maxFrequencyShortPeriodics + 1;
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

                    //Create the object for the pair j, s
                    //context.getHansenObjects()[s + maxDegree][j] = new HansenTesseralLinear(maxDegree, s, j, n0, context.getMaxHansen());
                    this.hansenObjects[s + maxDegree][j] = new HansenTesseralLinear(maxDegree, s, j, n0, maxHansen);

                    if (s > 0 && s <= sMin) {
                        //Also create the object for the pair j, -s
                        //context.getHansenObjects()[maxDegree - s][j] = new HansenTesseralLinear(maxDegree, -s, j, n0, context.getMaxHansen());
                        this.hansenObjects[maxDegree - s][j] =  new HansenTesseralLinear(maxDegree, -s, j, n0, maxHansen);
                    }
                }
            }
        } else {
            // create all objects
            for (int j = 0; j <= maxFrequencyShortPeriodics; j++) {
                for (int s = -maxDegree; s <= maxDegree; s++) {
                    //Compute the n0 value
                    final int n0 = FastMath.max(2, FastMath.abs(s));

                    //context.getHansenObjects()[s + maxDegree][j] = new HansenTesseralLinear(maxDegree, s, j, n0, context.getMaxHansen());
                    this.hansenObjects[s + maxDegree][j] = new HansenTesseralLinear(maxDegree, s, j, n0, maxHansen);
                }
            }
        }

    }

    /** Compute init values for hansen objects.
     * @param rows number of rows of the hansen matrix
     * @param columns columns number of columns of the hansen matrix
     */
    public void computeHansenObjectsInitValues(final int rows, final int columns) {
        hansenObjects[rows][columns].computeInitValues(e2, chi, chi2);
    }

    /** Get hansen object.
     * @return hansenObjects
     */
    public HansenTesseralLinear[][] getHansenObjects() {
        return hansenObjects;
    }

    /** Get A = sqrt(μ * a).
     * @return A
     */
    public double getA() {
        return A;
    }

    /** Get the list of resonant orders.
     * @return resOrders
     */
    public List<Integer> getResOrders() {
        return resOrders;
    }

    /** Get ecc².
     * @return e2
     */
    public double getE2() {
        return e2;
    }

    /** Get Central body rotation angle θ.
     * @return theta
     */
    public double getTheta() {
        return theta;
    }

    /** Get ax2oA = 2 * a / A .
     * @return ax2oA
     */
    public double getAx2oA() {
        return ax2oA;
    }

    /** Get &Chi; = 1 / sqrt(1 - e²) = 1 / B.
     * @return chi
     */
    public double getChi() {
        return chi;
    }

    /** Get &Chi;².
     * @return chi2
     */
    public double getChi2() {
        return chi2;
    }

    /** Get B / A.
     * @return BoA
     */
    public double getBoA() {
        return BoA;
    }

    /** Get ooAB = 1 / (A * B).
     * @return ooAB
     */
    public double getOoAB() {
        return ooAB;
    }

    /** Get Co2AB = C / 2AB.
     * @return Co2AB
     */
    public double getCo2AB() {
        return Co2AB;
    }

    /** Get BoABpo = B / A(1 + B).
     * @return BoABpo
     */
    public double getBoABpo() {
        return BoABpo;
    }

    /** Get μ / a .
     * @return moa
     */
    public double getMoa() {
        return moa;
    }

    /** Get roa = R / a.
     * @return roa
     */
    public double getRoa() {
        return roa;
    }

    /** Get the maximum power of the eccentricity to use in summation over s.
     * @return maxEccPow
     */
    public int getMaxEccPow() {
        return maxEccPow;
    }

    /** Get the maximum power of the eccentricity to use in Hansen coefficient Kernel expansion.
     * @return maxHansen
     */
    public int getMaxHansen() {
        return maxHansen;
    }

    /** Get keplerian period.
     * @return orbitPeriod
     */
    public double getOrbitPeriod() {
        return orbitPeriod;
    }

    /** Get the maximal degree to consider for harmonics potential.
     * @return maxDegree
     */
    public int getMaxDegree() {
        return maxDegree;
    }

    /** Factory for the DerivativeStructure instances.
     * @return factory
     */
    public DSFactory getFactory() {
        return factory;
    }

    /** Get the ratio of satellite period to central body rotation period.
     * @return ratio
     */
    public double getRatio() {
        return ratio;
    }

}
