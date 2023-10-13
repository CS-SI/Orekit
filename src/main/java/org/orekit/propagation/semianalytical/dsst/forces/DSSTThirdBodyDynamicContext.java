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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.CelestialBody;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;

/**
 * This class is a container for the common parameters used in
 * {@link DSSTThirdBody}.
 * <p>
 * It performs parameters initialization at each integration step for the third
 * body attraction perturbation. These parameters change for each integration
 * step.
 * </p>
 * @author Bryan Cazabonne
 * @since 11.3.3
 */
public class DSSTThirdBodyDynamicContext extends ForceModelContext {

    /** Standard gravitational parameter μ for the body in m³/s². */
    private double gm;

    /** Distance from center of mass of the central body to the 3rd body. */
    private double R3;

    /** A = sqrt(μ * a). */
    private double A;

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

    /** b = 1 / (1 + sqrt(1 - e²)) = 1 / (1 + B). */
    private double b;

    /** h * &Chi;³. */
    private double hXXX;

    /** k * &Chi;³. */
    private double kXXX;

    /** Keplerian mean motion. */
    private double motion;

    /** Constructor.
     * @param aux auxiliary elements related to the current orbit
     * @param body body the 3rd body to consider
     * @param parameters values of the force model parameters
     */
    public DSSTThirdBodyDynamicContext(final AuxiliaryElements aux,
                                       final CelestialBody body,
                                       final double[] parameters) {
        super(aux);

        // Parameters related to force model drivers
        final double mu = parameters[1];
        A = FastMath.sqrt(mu * aux.getSma());
        this.gm = parameters[0];
        final double absA = FastMath.abs(aux.getSma());
        motion = FastMath.sqrt(mu / absA) / absA;

        // Distance from center of mass of the central body to the 3rd body
        final Vector3D bodyPos = body.getPosition(aux.getDate(), aux.getFrame());
        R3 = bodyPos.getNorm();

        // Direction cosines
        final Vector3D bodyDir = bodyPos.normalize();
        alpha = bodyDir.dotProduct(aux.getVectorF());
        beta = bodyDir.dotProduct(aux.getVectorG());
        gamma = bodyDir.dotProduct(aux.getVectorW());

        // &Chi;<sup>-2</sup>.
        BB = aux.getB() * aux.getB();
        // &Chi;<sup>-3</sup>.
        BBB = BB * aux.getB();

        // b = 1 / (1 + B)
        b = 1. / (1. + aux.getB());

        // &Chi;
        X = 1. / aux.getB();
        XX = X * X;
        XXX = X * XX;
        // -2 * a / A
        m2aoA = -2. * aux.getSma() / A;
        // B / A
        BoA = aux.getB() / A;
        // 1 / AB
        ooAB = 1. / (A * aux.getB());
        // -C / 2AB
        mCo2AB = -aux.getC() * ooAB / 2.;
        // B / A(1 + B)
        BoABpo = BoA / (1. + aux.getB());

        // mu3 / R3
        muoR3 = gm / R3;

        // h * &Chi;³
        hXXX = aux.getH() * XXX;
        // k * &Chi;³
        kXXX = aux.getK() * XXX;

    }

    /** Get A = sqrt(μ * a).
     * @return A
     */
    public double getA() {
        return A;
    }

    /** Get the distance from center of mass of the central body to the 3rd body.
     * @return the distance from center of mass of the central body to the 3rd body
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
     * @return &Chi;²
     */
    public double getXX() {
        return XX;
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

    /** Get the Keplerian mean motion.
     * <p>The Keplerian mean motion is computed directly from semi major axis
     * and central acceleration constant.</p>
     * @return Keplerian mean motion in radians per second
     */
    public double getMeanMotion() {
        return motion;
    }

}
