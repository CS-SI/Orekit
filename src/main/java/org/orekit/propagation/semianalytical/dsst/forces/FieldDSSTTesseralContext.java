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

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.FastMath;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.Frame;
import org.orekit.propagation.semianalytical.dsst.utilities.FieldAuxiliaryElements;

/**
 * This class is a container for the common "field" parameters used in {@link DSSTTesseral}.
 * <p>
 * It performs parameters initialization at each integration step for the Tesseral contribution
 * to the central body gravitational perturbation.
 * <p>
 * @author Bryan Cazabonne
 * @since 10.0
 */
class FieldDSSTTesseralContext<T extends RealFieldElement<T>> extends FieldForceModelContext<T> {

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
    private T A;

    // Common factors for potential computation
    /** &Chi; = 1 / sqrt(1 - e²) = 1 / B. */
    private T chi;

    /** &Chi;². */
    private T chi2;

    /** Central body rotation angle θ. */
    private T theta;

    // Common factors from equinoctial coefficients
    /** 2 * a / A .*/
    private T ax2oA;

    /** 1 / (A * B) .*/
    private T ooAB;

    /** B / A .*/
    private T BoA;

    /** B / (A * (1 + B)) .*/
    private T BoABpo;

    /** C / (2 * A * B) .*/
    private T Co2AB;

    /** μ / a .*/
    private T moa;

    /** R / a .*/
    private T roa;

    /** ecc². */
    private T e2;

    /** Keplerian mean motion. */
    private T n;

    /** Keplerian period. */
    private T period;

    /** Maximum power of the eccentricity to use in summation over s. */
    private int maxEccPow;

    /** Ratio of satellite period to central body rotation period. */
    private T ratio;

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
    FieldDSSTTesseralContext(final FieldAuxiliaryElements<T> auxiliaryElements,
                                    final Frame centralBodyFrame,
                                    final UnnormalizedSphericalHarmonicsProvider provider,
                                    final int maxFrequencyShortPeriodics,
                                    final double bodyPeriod,
                                    final T[] parameters) {

        super(auxiliaryElements);

        final Field<T> field = auxiliaryElements.getDate().getField();
        final T zero = field.getZero();

        this.maxEccPow = 0;
        this.resOrders = new ArrayList<Integer>();

        final T mu = parameters[0];

        // Keplerian mean motion
        final T absA = FastMath.abs(auxiliaryElements.getSma());
        n = FastMath.sqrt(mu.divide(absA)).divide(absA);

        // Keplerian period
        final T a = auxiliaryElements.getSma();
        period = (a.getReal() < 0) ? zero.add(Double.POSITIVE_INFINITY) : a.multiply(2.0 * FastMath.PI).multiply(a.divide(mu).sqrt());

        A = FastMath.sqrt(mu.multiply(auxiliaryElements.getSma()));

        // Eccentricity square
        e2 = auxiliaryElements.getEcc().multiply(auxiliaryElements.getEcc());

        // Central body rotation angle from equation 2.7.1-(3)(4).
        final FieldTransform<T> t = centralBodyFrame.getTransformTo(auxiliaryElements.getFrame(), auxiliaryElements.getDate());
        final FieldVector3D<T> xB = t.transformVector(FieldVector3D.getPlusI(field));
        final FieldVector3D<T> yB = t.transformVector(FieldVector3D.getPlusJ(field));
        theta = FastMath.atan2(auxiliaryElements.getVectorF().dotProduct(yB).negate().add((auxiliaryElements.getVectorG().dotProduct(xB)).multiply(I)),
                               auxiliaryElements.getVectorF().dotProduct(xB).add(auxiliaryElements.getVectorG().dotProduct(yB).multiply(I)));

        // Common factors from equinoctial coefficients
        // 2 * a / A
        ax2oA  = auxiliaryElements.getSma().divide(A).multiply(2.);
        // B / A
        BoA    = auxiliaryElements.getB().divide(A);
        // 1 / AB
        ooAB   = A.multiply(auxiliaryElements.getB()).reciprocal();
        // C / 2AB
        Co2AB  = auxiliaryElements.getC().multiply(ooAB).divide(2.);
        // B / (A * (1 + B))
        BoABpo = BoA.divide(auxiliaryElements.getB().add(1.));
        // &mu / a
        moa    = mu.divide(auxiliaryElements.getSma());
        // R / a
        roa    = auxiliaryElements.getSma().divide(provider.getAe()).reciprocal();

        // &Chi; = 1 / B
        chi  = auxiliaryElements.getB().reciprocal();
        chi2 = chi.multiply(chi);

        // Set the highest power of the eccentricity in the analytical power
        // series expansion for the averaged high order resonant central body
        // spherical harmonic perturbation
        final T e = auxiliaryElements.getEcc();
        if (e.getReal() <= 0.005) {
            maxEccPow = 3;
        } else if (e.getReal() <= 0.02) {
            maxEccPow = 4;
        } else if (e.getReal() <= 0.1) {
            maxEccPow = 7;
        } else if (e.getReal() <= 0.2) {
            maxEccPow = 10;
        } else if (e.getReal() <= 0.3) {
            maxEccPow = 12;
        } else if (e.getReal() <= 0.4) {
            maxEccPow = 15;
        } else {
            maxEccPow = 20;
        }

        // Ratio of satellite to central body periods to define resonant terms
        ratio = period.divide(bodyPeriod);

        // Compute natural resonant terms
        final T tolerance = FastMath.max(zero.add(MIN_PERIOD_IN_SAT_REV),
                                                   period.divide(MIN_PERIOD_IN_SECONDS).reciprocal()).reciprocal();

        // Search the resonant orders in the tesseral harmonic field
        resOrders.clear();
        for (int m = 1; m <= provider.getMaxOrder(); m++) {
            final T resonance = ratio.multiply(m);
            final int jComputedRes = (int) FastMath.round(resonance);
            if (jComputedRes > 0 && jComputedRes <= maxFrequencyShortPeriodics && FastMath.abs(resonance.subtract(jComputedRes)).getReal() <= tolerance.getReal()) {
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
    public T getE2() {
        return e2;
    }

    /** Get Central body rotation angle θ.
     * @return theta
     */
    public T getTheta() {
        return theta;
    }

    /** Get ax2oA = 2 * a / A .
     * @return ax2oA
     */
    public T getAx2oA() {
        return ax2oA;
    }

    /** Get &Chi; = 1 / sqrt(1 - e²) = 1 / B.
     * @return chi
     */
    public T getChi() {
        return chi;
    }

    /** Get &Chi;².
     * @return chi2
     */
    public T getChi2() {
        return chi2;
    }

    /** Get B / A.
     * @return BoA
     */
    public T getBoA() {
        return BoA;
    }

    /** Get ooAB = 1 / (A * B).
     * @return ooAB
     */
    public T getOoAB() {
        return ooAB;
    }

    /** Get Co2AB = C / 2AB.
     * @return Co2AB
     */
    public T getCo2AB() {
        return Co2AB;
    }

    /** Get BoABpo = B / A(1 + B).
     * @return BoABpo
     */
    public T getBoABpo() {
        return BoABpo;
    }

    /** Get μ / a .
     * @return moa
     */
    public T getMoa() {
        return moa;
    }

    /** Get roa = R / a.
     * @return roa
     */
    public T getRoa() {
        return roa;
    }

    /** Get the maximum power of the eccentricity to use in summation over s.
     * @return roa
     */
    public int getMaxEccPow() {
        return maxEccPow;
    }

    /** Get the Keplerian period.
     * <p>The Keplerian period is computed directly from semi major axis
     * and central acceleration constant.</p>
     * @return Keplerian period in seconds, or positive infinity for hyperbolic orbits
     */
    public T getOrbitPeriod() {
        return period;
    }

    /** Get the Keplerian mean motion.
     * <p>The Keplerian mean motion is computed directly from semi major axis
     * and central acceleration constant.</p>
     * @return Keplerian mean motion in radians per second
     */
    public T getMeanMotion() {
        return n;
    }

    /** Get the ratio of satellite period to central body rotation period.
     * @return ratio
     */
    public T getRatio() {
        return ratio;
    }

}
