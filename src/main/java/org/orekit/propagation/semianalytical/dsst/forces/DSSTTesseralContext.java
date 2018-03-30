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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;

/** This class is a container for the attributes of
 * {@link org.orekit.propagation.semianalytical.dsst.forces.DSSTTesseral DSSTTesseral}.
 * <p>
 * It replaces the last version of the method
 * {@link  org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel#initializeStep(AuxiliaryElements) initializeStep(AuxiliaryElements)}.
 * </p>
 */
public class DSSTTesseralContext extends ForceModelContext {

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

    /** Simple constructor.
     * Performs initialization at each integration step for the current force model.
     * This method aims at being called before mean elements rates computation
     * @param auxiliaryElements auxiliary elements related to the current orbit
     * @param centralBodyFrame rotating body frame
     * @param provider provider for spherical harmonics
     * @throws OrekitException if some specific error occurs
     */
    public DSSTTesseralContext(final AuxiliaryElements auxiliaryElements,
                               final Frame centralBodyFrame,
                               final UnnormalizedSphericalHarmonicsProvider provider)
        throws OrekitException {

        super(auxiliaryElements);

        // Eccentricity square
        e2 = auxiliaryElements.getEcc() * auxiliaryElements.getEcc();

        // Central body rotation angle from equation 2.7.1-(3)(4).
        final Transform t = centralBodyFrame.getTransformTo(auxiliaryElements.getFrame(), auxiliaryElements.getDate());
        final Vector3D xB = t.transformVector(Vector3D.PLUS_I);
        final Vector3D yB = t.transformVector(Vector3D.PLUS_J);
        theta = FastMath.atan2(-auxiliaryElements.getVectorF().dotProduct(yB) + I * auxiliaryElements.getVectorG().dotProduct(xB),
                               auxiliaryElements.getVectorF().dotProduct(xB) + I * auxiliaryElements.getVectorG().dotProduct(yB));

        // Equinoctial coefficients
        // A = sqrt(μ * a)
        final double A = auxiliaryElements.getA();
        // B = sqrt(1 - h² - k²)
        final double B = auxiliaryElements.getB();
        // C = 1 + p² + q²
        final double C = auxiliaryElements.getC();
        // Common factors from equinoctial coefficients
        // 2 * a / A
        ax2oA  = 2. * auxiliaryElements.getSma() / A;
        // B / A
        BoA  = B / A;
        // 1 / AB
        ooAB = 1. / (A * B);
        // C / 2AB
        Co2AB = C * ooAB / 2.;
        // B / (A * (1 + B))
        BoABpo = BoA / (1. + B);
        // &mu / a
        moa = provider.getMu() / auxiliaryElements.getSma();
        // R / a
        roa = provider.getAe() / auxiliaryElements.getSma();

        // &Chi; = 1 / B
        chi = 1. / B;
        chi2 = chi * chi;

    }

    /** Get retrograde factor.
     *  @return I
     */
    public int getRetrogadeFactor() {
        return I;
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


}
