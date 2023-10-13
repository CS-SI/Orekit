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
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.CelestialBody;
import org.orekit.propagation.semianalytical.dsst.utilities.FieldAuxiliaryElements;

/**
 * This class is a container for the common "field" parameters used in {@link DSSTThirdBody}.
 * <p>
 * It performs parameters initialization at each integration step for the third
 * body attraction perturbation. These parameters change for each integration
 * step.
 * </p>
 * @author Bryan Cazabonne
 * @since 12.0
 * @param <T> type of the field elements
 */
public class FieldDSSTThirdBodyDynamicContext<T extends CalculusFieldElement<T>> extends FieldForceModelContext<T> {

    /** Standard gravitational parameter μ for the body in m³/s². */
    private T gm;

    /** Distance from center of mass of the central body to the 3rd body. */
    private T R3;

    /** A = sqrt(μ * a). */
    private T A;

    /** α. */
    private T alpha;

    /** β. */
    private T beta;

    /** γ. */
    private T gamma;

    /** B². */
    private T BB;

    /** B³. */
    private T BBB;

    /** &Chi; = 1 / sqrt(1 - e²) = 1 / B. */
    private T X;

    /** &Chi;². */
    private T XX;

    /** &Chi;³. */
    private T XXX;

    /** -2 * a / A. */
    private T m2aoA;

    /** B / A. */
    private T BoA;

    /** 1 / (A * B). */
    private T ooAB;

    /** -C / (2 * A * B). */
    private T mCo2AB;

    /** B / A(1 + B). */
    private T BoABpo;

    /** mu3 / R3. */
    private T muoR3;

    /** b = 1 / (1 + sqrt(1 - e²)) = 1 / (1 + B).*/
    private T b;

    /** h * &Chi;³. */
    private T hXXX;

    /** k * &Chi;³. */
    private T kXXX;

    /** Keplerian mean motion. */
    private T motion;

    /** Constructor.
     * @param aux auxiliary elements related to the current orbit
     * @param body body the 3rd body to consider
     * @param parameters values of the force model parameters
     */
    public FieldDSSTThirdBodyDynamicContext(final FieldAuxiliaryElements<T> aux,
                                            final CelestialBody body,
                                            final T[] parameters) {

        super(aux);

        // Parameters related to force model drivers
        final T mu = parameters[1];
        A = FastMath.sqrt(mu.multiply(aux.getSma()));
        this.gm = parameters[0];
        final T absA = FastMath.abs(aux.getSma());
        motion = FastMath.sqrt(mu.divide(absA)).divide(absA);

        // Distance from center of mass of the central body to the 3rd body
        final FieldVector3D<T> bodyPos = body.getPVCoordinates(aux.getDate(), aux.getFrame()).getPosition();
        R3 = bodyPos.getNorm();

        // Direction cosines
        final FieldVector3D<T> bodyDir = bodyPos.normalize();
        alpha = bodyDir.dotProduct(aux.getVectorF());
        beta  = bodyDir.dotProduct(aux.getVectorG());
        gamma = bodyDir.dotProduct(aux.getVectorW());

        // &Chi;<sup>-2</sup>.
        BB = aux.getB().multiply(aux.getB());
        // &Chi;<sup>-3</sup>.
        BBB = BB.multiply(aux.getB());

        // b = 1 / (1 + B)
        b = aux.getB().add(1.).reciprocal();

        // &Chi;
        X = aux.getB().reciprocal();
        XX = X.multiply(X);
        XXX = X.multiply(XX);
        // -2 * a / A
        m2aoA = aux.getSma().multiply(-2.).divide(A);
        // B / A
        BoA = aux.getB().divide(A);
        // 1 / AB
        ooAB = (A.multiply(aux.getB())).reciprocal();
        // -C / 2AB
        mCo2AB = aux.getC().multiply(ooAB).divide(2.).negate();
        // B / A(1 + B)
        BoABpo = BoA.divide(aux.getB().add(1.));

        // mu3 / R3
        muoR3 = R3.divide(gm).reciprocal();

        // h * &Chi;³
        hXXX = XXX.multiply(aux.getH());
        // k * &Chi;³
        kXXX = XXX.multiply(aux.getK());

    }

    /** Get A = sqrt(μ * a).
     * @return A
     */
    public T getA() {
        return A;
    }

    /** Get the distance from center of mass of the central body to the 3rd body.
     * @return the distance from center of mass of the central body to the 3rd body
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
     * @return &Chi;²
     */
    public T getXX() {
        return XX;
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

    /** Get the Keplerian mean motion.
     * <p>The Keplerian mean motion is computed directly from semi major axis
     * and central acceleration constant.</p>
     * @return Keplerian mean motion in radians per second
     */
    public T getMeanMotion() {
        return motion;
    }

}
