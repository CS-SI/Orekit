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

import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.Frame;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.propagation.semianalytical.dsst.utilities.FieldAuxiliaryElements;

/** This class is a container for the field attributes of
 * {@link org.orekit.propagation.semianalytical.dsst.forces.DSSTTesseral DSSTTesseral}.
 * <p>
 * It replaces the last version of the method
 * {@link  org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel#initializeStep(AuxiliaryElements) initializeStep(AuxiliaryElements)}.
 * </p>
 */
public class FieldDSSTTesseralContext<T extends RealFieldElement<T>> extends FieldForceModelContext<T> {

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
    private final int I = 1;

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

    /** Simple constructor.
     * Performs initialization at each integration step for the current force model.
     * This method aims at being called before mean elements rates computation
     * @param fieldAuxiliaryElements auxiliary elements related to the current orbit
     * @param centralBodyFrame rotating body frame
     * @param provider provider for spherical harmonics
     * @throws OrekitException if some specific error occurs
     */
    public FieldDSSTTesseralContext(final FieldAuxiliaryElements<T> fieldAuxiliaryElements,
                               final Frame centralBodyFrame,
                               final UnnormalizedSphericalHarmonicsProvider provider)
        throws OrekitException {

        super(fieldAuxiliaryElements);

        // Eccentricity square
        e2 = fieldAuxiliaryElements.getEcc().multiply(fieldAuxiliaryElements.getEcc());

        // Central body rotation angle from equation 2.7.1-(3)(4).
        final FieldTransform<T> t = centralBodyFrame.getTransformTo(fieldAuxiliaryElements.getFrame(), fieldAuxiliaryElements.getDate());
        final FieldVector3D<T> xB = t.transformVector(Vector3D.PLUS_I);
        final FieldVector3D<T> yB = t.transformVector(Vector3D.PLUS_J);
        theta = FastMath.atan2(((T) fieldAuxiliaryElements.getVectorF().dotProduct(yB).reciprocal()).add((T) fieldAuxiliaryElements.getVectorG().dotProduct(xB).multiply(I)),
                               ((T) fieldAuxiliaryElements.getVectorF().dotProduct(xB)).add((T) fieldAuxiliaryElements.getVectorG().dotProduct(yB).multiply(I)));

        // Common factors from equinoctial coefficients
        // 2 * a / A
        ax2oA  = fieldAuxiliaryElements.getSma().divide(fieldAuxiliaryElements.getA()).multiply(2.);
        // B / A
        BoA  = fieldAuxiliaryElements.getB().divide(fieldAuxiliaryElements.getA());
        // 1 / AB
        ooAB = fieldAuxiliaryElements.getA().multiply(fieldAuxiliaryElements.getB()).reciprocal();
        // C / 2AB
        Co2AB = fieldAuxiliaryElements.getC().multiply(ooAB).divide(2.);
        // B / (A * (1 + B))
        BoABpo = BoA.divide(fieldAuxiliaryElements.getB().add(1.));
        // &mu / a
        moa = fieldAuxiliaryElements.getSma().divide(provider.getMu()).reciprocal();
        // R / a
        roa = fieldAuxiliaryElements.getSma().divide(provider.getAe()).reciprocal();

        // &Chi; = 1 / B
        chi = fieldAuxiliaryElements.getB().reciprocal();
        chi2 = chi.multiply(chi);

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

}
