/* Copyright 2002-2025 CS GROUP
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
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.StaticTransform;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;

/**
 * This class is a container for the common parameters used in {@link DSSTTesseral} and {@link DSSTZonal}.
 * <p>
 * It performs parameters initialization at each integration step for the Tesseral  and Zonal contribution
 * to the central body gravitational perturbation.
 * </p>
 * @author Bryan Cazabonne
 * @author Maxime Journot
 * @since 12.2
 */
public class DSSTGravityContext extends ForceModelContext {

    /** A = sqrt(μ * a). */
    private final double A;

    /** &Chi; = 1 / sqrt(1 - e²) = 1 / B. */
    private final double chi;

    /** &Chi;². */
    private final double chi2;

    // Common factors from equinoctial coefficients
    /** 2 * a / A . */
    private final double ax2oA;

    /** 1 / (A * B) . */
    private final double ooAB;

    /** B / A . */
    private final double BoA;

    /** B / (A * (1 + B)) . */
    private final double BoABpo;

    /** C / (2 * A * B) . */
    private final double Co2AB;

    /** μ / a . */
    private final double muoa;

    /** R / a . */
    private final double roa;

    /** Keplerian mean motion. */
    private final double n;

    /** Direction cosine α. */
    private final double alpha;

    /** Direction cosine β. */
    private final double beta;

    /** Direction cosine γ. */
    private final double gamma;

    /** Transform from body-fixed frame to inertial frame. */
    private final StaticTransform bodyFixedToInertialTransform;

    /**
     * Constructor.
     *
     * @param auxiliaryElements auxiliary elements related to the current orbit
     * @param bodyFixedFrame  rotating body frame
     * @param provider   provider for spherical harmonics
     * @param parameters values of the force model parameters
     */
    DSSTGravityContext(final AuxiliaryElements auxiliaryElements,
                       final Frame bodyFixedFrame,
                       final UnnormalizedSphericalHarmonicsProvider provider,
                       final double[] parameters) {

        super(auxiliaryElements);

        // µ
        final double mu = parameters[0];

        // Semi-major axis
        final double a = auxiliaryElements.getSma();

        // Keplerian Mean Motion
        final double absA = FastMath.abs(a);
        this.n = FastMath.sqrt(mu / absA) / absA;

        // A = sqrt(µ * |a|)
        this.A = FastMath.sqrt(mu * absA);

        // &Chi; = 1 / B
        final double B = auxiliaryElements.getB();
        this.chi = 1. / B;
        this.chi2 = chi * chi;

        // Common factors from equinoctial coefficients
        // 2 * a / A
        this.ax2oA = 2. * a / A;
        // B / A
        this.BoA = B / A;
        // 1 / AB
        this.ooAB = 1. / (A * B);
        // C / 2AB
        this.Co2AB = auxiliaryElements.getC() * ooAB / 2.;
        // B / (A * (1 + B))
        this.BoABpo = BoA / (1. + B);
        // &mu / a
        this.muoa = mu / a;
        // R / a
        this.roa = provider.getAe() / a;

        // If (centralBodyFrame == null), then centralBodyFrame = orbit frame (see DSSTZonal constructors for more on this).
        final Frame internalBodyFixedFrame = bodyFixedFrame == null ? auxiliaryElements.getFrame() : bodyFixedFrame;

        // Transform from body-fixed frame (typically ITRF) to inertial frame
        this.bodyFixedToInertialTransform = internalBodyFixedFrame.
                        getStaticTransformTo(auxiliaryElements.getFrame(), auxiliaryElements.getDate());

        final Vector3D zB = bodyFixedToInertialTransform.transformVector(Vector3D.PLUS_K);

        // Direction cosines for central body [Eq. 2.1.9-(1)]
        this.alpha = Vector3D.dotProduct(zB, auxiliaryElements.getVectorF());
        this.beta  = Vector3D.dotProduct(zB, auxiliaryElements.getVectorG());
        this.gamma = Vector3D.dotProduct(zB, auxiliaryElements.getVectorW());
    }

    /** Getter for the a.
     * @return the a
     */
    public double getA() {
        return A;
    }

    /** Getter for the chi.
     * @return the chi
     */
    public double getChi() {
        return chi;
    }

    /** Getter for the chi2.
     * @return the chi2
     */
    public double getChi2() {
        return chi2;
    }

    /** Getter for the ax2oA.
     * @return the ax2oA
     */
    public double getAx2oA() {
        return ax2oA;
    }

    /** ooAB = 1 / (A * B).
     * @return the ooAB
     */
    public double getOoAB() {
        return ooAB;
    }

    /** Get B / A.
     * @return the boA
     */
    public double getBoA() {
        return BoA;
    }

    /** Get BoABpo = B / A(1 + B).
     * @return the boABpo
     */
    public double getBoABpo() {
        return BoABpo;
    }

    /** Get Co2AB = C / 2AB.
     * @return the co2AB
     */
    public double getCo2AB() {
        return Co2AB;
    }

    /** Get μ / a.
     * @return the muoa
     */
    public double getMuoa() {
        return muoa;
    }

    /** Get roa = R / a.
     * @return the roa
     */
    public double getRoa() {
        return roa;
    }

    /**
     * Get the Keplerian mean motion.
     * <p>
     * The Keplerian mean motion is computed directly from semi major axis and
     * central acceleration constant.
     * </p>
     * @return Keplerian mean motion in radians per second
     */
    public double getMeanMotion() {
        return n;
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

    /** Getter for the bodyFixedToInertialTransform.
     * @return the bodyFixedToInertialTransform
     */
    public StaticTransform getBodyFixedToInertialTransform() {
        return bodyFixedToInertialTransform;
    }
}
