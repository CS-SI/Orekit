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
import org.orekit.bodies.CelestialBody;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.propagation.semianalytical.dsst.utilities.FieldAuxiliaryElements;

/** This class is a container for the field attributes of
 * {@link org.orekit.propagation.semianalytical.dsst.forces.DSSTThirdBody DSSTThirdBody}.
 * <p>
 * It replaces the last version of the method
 * {@link  org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel#initializeStep(AuxiliaryElements)
 * initializeStep(AuxiliaryElements)}.
 * </p>
 */
public class FieldDSSTThirdBodyContext<T extends RealFieldElement <T>> extends FieldForceModelContext<T> {

    /** Standard gravitational parameter μ for the body in m³/s². */
    private final double           gm;

    /** Distance from center of mass of the central body to the 3rd body. */
    private T R3;

    // Direction cosines of the symmetry axis
    /** α. */
    private final T alpha;
    /** β. */
    private final T beta;
    /** γ. */
    private final T gamma;

    /** B². */
    private final T BB;
    /** B³. */
    private final T BBB;

    /** &Chi; = 1 / sqrt(1 - e²) = 1 / B. */
    private final T X;
    /** &Chi;². */
    private final T XX;
    /** &Chi;³. */
    private final T XXX;
    /** -2 * a / A. */
    private final T m2aoA;
    /** B / A. */
    private final T BoA;
    /** 1 / (A * B). */
    private final T ooAB;
    /** -C / (2 * A * B). */
    private final T mCo2AB;
    /** B / A(1 + B). */
    private final T BoABpo;

    /** mu3 / R3. */
    private final T muoR3;

    /** b = 1 / (1 + sqrt(1 - e²)) = 1 / (1 + B).*/
    private final T b;

    /** h * &Chi;³. */
    private final T hXXX;
    /** k * &Chi;³. */
    private final T kXXX;

    /** Simple constructor.
     * Performs initialization at each integration step for the current force model.
     * This method aims at being called before mean elements rates computation
     * @param fieldAuxiliaryElements auxiliary elements related to the current orbit
     * @param thirdBody body the 3rd body to consider
     * @throws OrekitException if some specific error occurs
     */
    public FieldDSSTThirdBodyContext(final FieldAuxiliaryElements<T> fieldAuxiliaryElements, final CelestialBody thirdBody) throws OrekitException {

        super(fieldAuxiliaryElements);

        this.gm = thirdBody.getGM();
        // Distance from center of mass of the central body to the 3rd body
        final FieldVector3D<T> bodyPos = thirdBody.getPVCoordinates(fieldAuxiliaryElements.getDate(), fieldAuxiliaryElements.getFrame()).getPosition();
        R3 = bodyPos.getNorm();

        // Direction cosines
        final FieldVector3D<T> bodyDir = bodyPos.normalize();
        alpha = (T) bodyDir.dotProduct(fieldAuxiliaryElements.getVectorF());
        beta  = (T) bodyDir.dotProduct(fieldAuxiliaryElements.getVectorG());
        gamma = (T) bodyDir.dotProduct(fieldAuxiliaryElements.getVectorW());

        //&Chi;<sup>-2</sup>.
        BB = fieldAuxiliaryElements.getB().multiply(fieldAuxiliaryElements.getB());
        //&Chi;<sup>-3</sup>.
        BBB = BB.multiply(fieldAuxiliaryElements.getB());

        //b = 1 / (1 + B)
        b = fieldAuxiliaryElements.getB().add(1.).reciprocal();

        // &Chi;
        X = fieldAuxiliaryElements.getB().reciprocal();
        XX = X.multiply(X);
        XXX = X.multiply(XX);

        // -2 * a / A
        m2aoA = fieldAuxiliaryElements.getSma().multiply(-2.).divide(fieldAuxiliaryElements.getA());
        // B / A
        BoA = fieldAuxiliaryElements.getB().divide(fieldAuxiliaryElements.getA());
        // 1 / AB
        ooAB = (fieldAuxiliaryElements.getA().multiply(fieldAuxiliaryElements.getB())).reciprocal();
        // -C / 2AB
        mCo2AB = fieldAuxiliaryElements.getC().multiply(ooAB).divide(2.).negate();
        // B / A(1 + B)
        BoABpo = BoA.divide(fieldAuxiliaryElements.getB().add(1.));
        // mu3 / R3
        muoR3 = R3.divide(gm).reciprocal();
        //h * &Chi;³
        hXXX = XXX.multiply(fieldAuxiliaryElements.getH());
        //k * &Chi;³
        kXXX = XXX.multiply(fieldAuxiliaryElements.getK());

    }

    /** Get distance from center of mass of the central body to the 3rd body.
     * @return R3
     */
    public T getR3() {
        return R3;
    }

    /** Get direction cosine α for central body.
     * @return α
     */
    public T getAlpha() {
        return alpha;
    }

    /** Get direction cosine β for central body.
     * @return β
     */
    public T getBeta() {
        return beta;
    }

    /** Get direction cosine γ for central body.
     * @return γ
     */
    public T getGamma() {
        return gamma;
    }

    /** Get B².
     * @return B²
     */
    public T getBB() {
        return BB;
    }

    /** Get B³.
     * @return B³
     */
    public T getBBB() {
        return BBB;
    }

    /** Get b = 1 / (1 + sqrt(1 - e²)) = 1 / (1 + B).
     * @return b
     */
    public T getb() {
        return b;
    }

    /** Get &Chi; = 1 / sqrt(1 - e²) = 1 / B.
     * @return &Chi;
     */
    public T getX() {
        return X;
    }

    /** Get &Chi;².
     * @return &Chi;².
     */
    public T getXX() {
        return XX;
    }

    /** Get &Chi;³.
     * @return &Chi;³
     */
    public T getXXX() {
        return XXX;
    }

    /** Get m2aoA = -2 * a / A.
     * @return m2aoA
     */
    public T getM2aoA() {
        return m2aoA;
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

    /** Get mCo2AB = -C / 2AB.
     * @return mCo2AB
     */
    public T getMCo2AB() {
        return mCo2AB;
    }

    /** Get BoABpo = B / A(1 + B).
     * @return BoABpo
     */
    public T getBoABpo() {
        return BoABpo;
    }

    /** Get muoR3 = mu3 / R3.
     * @return muoR3
     */
    public T getMuoR3() {
        return muoR3;
    }

    /** Get hXXX = h * &Chi;³.
     * @return hXXX
     */
    public T getHXXX() {
        return hXXX;
    }

    /** Get kXXX = h * &Chi;³.
     * @return kXXX
     */
    public T getKXXX() {
        return kXXX;
    }

    /** Get standard gravitational parameter μ for the body in m³/s².
     *  @return gm
     */
    public double getGM() {
        return gm;
    }

}
