/* Copyright 2002-2024 CS GROUP
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
import org.hipparchus.util.MathUtils;
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
public class FieldDSSTTesseralContext<T extends CalculusFieldElement<T>> extends FieldDSSTGravityContext<T> {

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

    /** Central body rotation angle θ. */
    private T theta;

    /** ecc². */
    private T e2;

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

        super(auxiliaryElements, centralBodyFrame, provider, parameters);

        // Get field and zero
        final Field<T> field = auxiliaryElements.getDate().getField();
        final T zero = field.getZero();

        // Keplerian period
        final T a = auxiliaryElements.getSma();
        period = (a.getReal() < 0) ? zero.newInstance(Double.POSITIVE_INFINITY) : getMeanMotion().reciprocal().multiply(MathUtils.TWO_PI);

        // Eccentricity square
        e2 = auxiliaryElements.getEcc().multiply(auxiliaryElements.getEcc());

        // Central body rotation angle from equation 2.7.1-(3)(4).
        final FieldStaticTransform<T> t = getBodyFixedToInertialTransform();
        final FieldVector3D<T> xB = t.transformVector(FieldVector3D.getPlusI(field));
        final FieldVector3D<T> yB = t.transformVector(FieldVector3D.getPlusJ(field));
        theta = FastMath.atan2(auxiliaryElements.getVectorF().dotProduct(yB).negate().add((auxiliaryElements.getVectorG().dotProduct(xB)).multiply(I)),
                               auxiliaryElements.getVectorF().dotProduct(xB).add(auxiliaryElements.getVectorG().dotProduct(yB).multiply(I)));

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

    /** Get μ / a .
     * @return moa
     * @deprecated since 12.2 Use getMuoa() instead
     */
    @Deprecated
    public T getMoa() {
        return getMuoa();
    }

    /** Get the Keplerian period.
     * <p>The Keplerian period is computed directly from semi major axis
     * and central acceleration constant.</p>
     * @return Keplerian period in seconds, or positive infinity for hyperbolic orbits
     */
    public T getOrbitPeriod() {
        return period;
    }

    /** Get the ratio of satellite period to central body rotation period.
     * @return ratio
     */
    public T getRatio() {
        return ratio;
    }
}
