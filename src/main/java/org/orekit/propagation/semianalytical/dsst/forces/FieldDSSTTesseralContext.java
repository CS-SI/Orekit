/* Copyright 2002-2023 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.FastMath;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.frames.FieldStaticTransform;
import org.orekit.frames.Frame;
import org.orekit.propagation.semianalytical.dsst.utilities.FieldAuxiliaryElements;
import org.orekit.time.AbsoluteDate;

/**
 * This class is a container for the common "field" parameters used in {@link DSSTTesseral}.
 * <p>
 * It performs parameters initialization at each integration step for the Tesseral contribution
 * to the central body gravitational perturbation.
 * </p>
 * @author Bryan Cazabonne
 * @since 10.0
 * @param <T> type of the field elements
 */
public class FieldDSSTTesseralContext<T extends CalculusFieldElement<T>> extends FieldForceModelContext<T> {

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

    /** Ratio of satellite period to central body rotation period. */
    private T ratio;

    /**
     * Simple constructor.
     *
     * @param auxiliaryElements auxiliary elements related to the current orbit
     * @param centralBodyFrame rotating body frame
     * @param provider provider for spherical harmonics
     * @param maxFrequencyShortPeriodics maximum value for j
     * @param bodyPeriod central body rotation period (seconds)
     * @param parameters values of the force model parameters (only 1 values
     * for each parameters corresponding to state date) obtained by calling
     * the extract parameter method {@link #extractParameters(double[], AbsoluteDate)}
     * to selected the right value for state date or by getting the parameters for a specific date
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

        final T mu = parameters[0];

        // Keplerian mean motion
        final T absA = FastMath.abs(auxiliaryElements.getSma());
        n = FastMath.sqrt(mu.divide(absA)).divide(absA);

        // Keplerian period
        final T a = auxiliaryElements.getSma();
        period = (a.getReal() < 0) ? zero.add(Double.POSITIVE_INFINITY) : a.multiply(a.getPi().multiply(2.0)).multiply(a.divide(mu).sqrt());

        A = FastMath.sqrt(mu.multiply(auxiliaryElements.getSma()));

        // Eccentricity square
        e2 = auxiliaryElements.getEcc().multiply(auxiliaryElements.getEcc());

        // Central body rotation angle from equation 2.7.1-(3)(4).
        final FieldStaticTransform<T> t = centralBodyFrame.getStaticTransformTo(auxiliaryElements.getFrame(), auxiliaryElements.getDate());
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

        // Ratio of satellite to central body periods to define resonant terms
        ratio = period.divide(bodyPeriod);

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
