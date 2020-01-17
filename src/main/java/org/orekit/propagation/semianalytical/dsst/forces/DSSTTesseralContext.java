/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;

/**
 * This class is a container for the common parameters used in {@link DSSTTesseral}.
 * <p>
 * It performs parameters initialization at each integration step for the Tesseral contribution
 * to the central body gravitational perturbation.
 * <p>
 * @author Bryan Cazabonne
 * @since 10.0
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
    private double A;

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

    /** Keplerian mean motion. */
    private double n;

    /** Keplerian period. */
    private double period;

    /** Ratio of satellite period to central body rotation period. */
    private double ratio;

    /** List of resonant orders. */
    private final List<Integer> resOrders;

    /**
     * Simple constructor.
     *
     * @param auxiliaryElements auxiliary elements related to the current orbit
     * @param centralBodyFrame rotating body frame
     * @param provider provider for spherical harmonics
     * @param maxFrequencyShortPeriodics maximum value for j
     * @param bodyPeriod central body rotation period (seconds)
     * @param parameters values of the force model parameters
     */
    DSSTTesseralContext(final AuxiliaryElements auxiliaryElements,
                        final Frame centralBodyFrame,
                        final UnnormalizedSphericalHarmonicsProvider provider,
                        final int maxFrequencyShortPeriodics,
                        final double bodyPeriod,
                        final double[] parameters) {

        super(auxiliaryElements);

        this.maxEccPow = 0;
        this.resOrders = new ArrayList<Integer>();

        final double mu = parameters[0];

        // Keplerian Mean Motion
        final double absA = FastMath.abs(auxiliaryElements.getSma());
        n = FastMath.sqrt(mu / absA) / absA;

        // Keplerian period
        final double a = auxiliaryElements.getSma();
        period = (a < 0) ? Double.POSITIVE_INFINITY : 2.0 * FastMath.PI * a * FastMath.sqrt(a / mu);

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

        // Ratio of satellite to central body periods to define resonant terms
        ratio = period / bodyPeriod;

        // Compute natural resonant terms
        final double tolerance = 1. / FastMath.max(MIN_PERIOD_IN_SAT_REV,
                                                   MIN_PERIOD_IN_SECONDS / period);

        // Search the resonant orders in the tesseral harmonic field
        resOrders.clear();
        for (int m = 1; m <= provider.getMaxOrder(); m++) {
            final double resonance = ratio * m;
            final int jComputedRes = (int) FastMath.round(resonance);
            if (jComputedRes > 0 && jComputedRes <= maxFrequencyShortPeriodics && FastMath.abs(resonance - jComputedRes) <= tolerance) {
                // Store each resonant index and order
                this.resOrders.add(m);
            }
        }

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

    /** Get the Keplerian period.
     * <p>The Keplerian period is computed directly from semi major axis
     * and central acceleration constant.</p>
     * @return Keplerian period in seconds, or positive infinity for hyperbolic orbits
     */
    public double getOrbitPeriod() {
        return period;
    }

    /** Get the Keplerian mean motion.
     * <p>The Keplerian mean motion is computed directly from semi major axis
     * and central acceleration constant.</p>
     * @return Keplerian mean motion in radians per second
     */
    public double getMeanMotion() {
        return n;
    }

    /** Get the ratio of satellite period to central body rotation period.
     * @return ratio
     */
    public double getRatio() {
        return ratio;
    }

}
