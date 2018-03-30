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

import org.orekit.bodies.CelestialBody;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;

/** This class is a container for the attributes of
 * {@link org.orekit.propagation.semianalytical.dsst.forces.DSSTThirdBody DSSTThirdBody}.
 * <p>
 * It replaces the last version of the method
 * {@link  org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel#initializeStep(AuxiliaryElements)
 * initializeStep(AuxiliaryElements)}.
 * </p>
 */
public class DSSTThirdBodyContext extends ForceModelContext {

    /** Distance from center of mass of the central body to the 3rd body. */
    private double R3;

    // Direction cosines of the symmetry axis
    /** α. */
    private double alpha;
    /** β. */
    private double beta;
    /** γ. */
    private double gamma;

    /** B². */
    private double BB;
    /** B³. */
    private double BBB;

    /** &Chi; = 1 / sqrt(1 - e²) = 1 / B. */
    private double X;
    /** &Chi;². */
    private double XX;
    /** &Chi;³. */
    private double XXX;
    /** -2 * a / A. */
    private double m2aoA;
    /** B / A. */
    private double BoA;
    /** 1 / (A * B). */
    private double ooAB;
    /** -C / (2 * A * B). */
    private double mCo2AB;
    /** B / A(1 + B). */
    private double BoABpo;

    /** mu3 / R3. */
    private double muoR3;

    /** b = 1 / (1 + sqrt(1 - e²)) = 1 / (1 + B).*/
    private double b;

    /** h * &Chi;³. */
    private double hXXX;
    /** k * &Chi;³. */
    private double kXXX;

    /** Simple constructor.
     * Performs initialization at each integration step for the current force model.
     * This method aims at being called before mean elements rates computation
     * @param auxiliaryElements auxiliary elements related to the current orbit
     * @param thirdBody body the 3rd body to consider
     * @throws OrekitException if some specific error occurs
     */
    public DSSTThirdBodyContext(final AuxiliaryElements auxiliaryElements, final CelestialBody thirdBody) throws OrekitException {

        super(auxiliaryElements);

        // Distance from center of mass of the central body to the 3rd body
        final Vector3D bodyPos = thirdBody.getPVCoordinates(auxiliaryElements.getDate(), auxiliaryElements.getFrame()).getPosition();
        R3 = bodyPos.getNorm();

        // Direction cosines
        final Vector3D bodyDir = bodyPos.normalize();
        alpha = bodyDir.dotProduct(auxiliaryElements.getVectorF());
        beta  = bodyDir.dotProduct(auxiliaryElements.getVectorG());
        gamma = bodyDir.dotProduct(auxiliaryElements.getVectorW());

        //&Chi;<sup>-2</sup>.
        BB = auxiliaryElements.getB() * auxiliaryElements.getB();
        //&Chi;<sup>-3</sup>.
        BBB = BB * auxiliaryElements.getB();

        //b = 1 / (1 + B)
        b = 1. / (1. + auxiliaryElements.getB());

        // &Chi;
        X = 1. / auxiliaryElements.getB();
        XX = X * X;
        XXX = X * XX;
        // -2 * a / A
        m2aoA = -2. * auxiliaryElements.getSma() / auxiliaryElements.getA();
        // B / A
        BoA = auxiliaryElements.getB() / auxiliaryElements.getA();
        // 1 / AB
        ooAB = 1. / (auxiliaryElements.getA() * auxiliaryElements.getB());
        // -C / 2AB
        mCo2AB = -auxiliaryElements.getC() * ooAB / 2.;
        // B / A(1 + B)
        BoABpo = BoA / (1. + auxiliaryElements.getB());

        // mu3 / R3
        muoR3 = thirdBody.getGM() / R3;

        //h * &Chi;³
        hXXX = auxiliaryElements.getH() * XXX;
        //k * &Chi;³
        kXXX = auxiliaryElements.getK() * XXX;
    }

    /** Get distance from center of mass of the central body to the 3rd body.
     * @return R3
     */
    public double getR3() {
        return R3;
    }

    /** Get direction cosine α for central body.
     * @return α
     */
    public double getAlpha() {
        return alpha;
    }

    /** Get direction cosine β for central body.
     * @return β
     */
    public double getBeta() {
        return beta;
    }

    /** Get direction cosine γ for central body.
     * @return γ
     */
    public double getGamma() {
        return gamma;
    }

    /** Get B².
     * @return B²
     */
    public double getBB() {
        return BB;
    }

    /** Get B³.
     * @return B³
     */
    public double getBBB() {
        return BBB;
    }

    /** Get b = 1 / (1 + sqrt(1 - e²)) = 1 / (1 + B).
     * @return b
     */
    public double getb() {
        return b;
    }

    /** Get &Chi; = 1 / sqrt(1 - e²) = 1 / B.
     * @return &Chi;
     */
    public double getX() {
        return X;
    }

    /** Get &Chi;².
     * @return &Chi;².
     */
    public double getXX() {
        return XX;
    }

    /** Get &Chi;³.
     * @return &Chi;³
     */
    public double getXXX() {
        return XXX;
    }

    /** Get m2aoA = -2 * a / A.
     * @return m2aoA
     */
    public double getM2aoA() {
        return m2aoA;
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

    /** Get mCo2AB = -C / 2AB.
     * @return mCo2AB
     */
    public double getMCo2AB() {
        return mCo2AB;
    }

    /** Get BoABpo = B / A(1 + B).
     * @return BoABpo
     */
    public double getBoABpo() {
        return BoABpo;
    }

    /** Get muoR3 = mu3 / R3.
     * @return muoR3
     */
    public double getMuoR3() {
        return muoR3;
    }

    /** Get hXXX = h * &Chi;³.
     * @return hXXX
     */
    public double getHXXX() {
        return hXXX;
    }

    /** Get kXXX = h * &Chi;³.
     * @return kXXX
     */
    public double getKXXX() {
        return kXXX;
    }

}
