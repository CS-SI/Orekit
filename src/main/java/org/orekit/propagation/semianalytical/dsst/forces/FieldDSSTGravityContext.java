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
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.frames.FieldStaticTransform;
import org.orekit.frames.Frame;
import org.orekit.propagation.semianalytical.dsst.utilities.FieldAuxiliaryElements;

/**
 * This class is a container for the common parameters used in {@link DSSTTesseral} and {@link DSSTZonal}.
 * <p>
 * It performs parameters initialization at each integration step for the Tesseral and Zonal contribution
 * to the central body gravitational perturbation.
 * </p>
 * @author Bryan Cazabonne
 * @author Maxime Journot
 * @since 12.2
 */
public class FieldDSSTGravityContext<T extends CalculusFieldElement<T>> extends FieldForceModelContext<T> {

    /** A = sqrt(μ * a). */
    private final T A;

    /** &Chi; = 1 / sqrt(1 - e²) = 1 / B. */
    private final T chi;

    /** &Chi;². */
    private final T chi2;

    // Common factors from equinoctial coefficients
    /** 2 * a / A . */
    private final T ax2oA;

    /** 1 / (A * B) . */
    private final T ooAB;

    /** B / A . */
    private final T BoA;

    /** B / (A * (1 + B)) . */
    private final T BoABpo;

    /** C / (2 * A * B) . */
    private final T Co2AB;

    /** μ / a . */
    private final T muoa;

    /** R / a . */
    private final T roa;

    /** Keplerian mean motion. */
    private final T n;

    /** Direction cosine α. */
    private final T alpha;

    /** Direction cosine β. */
    private final T beta;

    /** Direction cosine γ. */
    private final T gamma;

    /** Transform from body-fixed frame to inertial frame. */
    private final FieldStaticTransform<T> bodyFixedToInertialTransform;

    /**
     * Constructor.
     *
     * @param auxiliaryElements auxiliary elements related to the current orbit
     * @param centralBodyFixedFrame  rotating body frame
     * @param provider   provider for spherical harmonics
     * @param parameters values of the force model parameters
     */
    FieldDSSTGravityContext(final FieldAuxiliaryElements<T> auxiliaryElements,
                            final Frame centralBodyFixedFrame,
                            final UnnormalizedSphericalHarmonicsProvider provider,
                            final T[] parameters) {

        super(auxiliaryElements);

        // µ
        final T mu = parameters[0];

        // Semi-major axis
        final T a = auxiliaryElements.getSma();

        // Keplerian Mean Motion
        final T absA = FastMath.abs(a);
        this.n = FastMath.sqrt(mu.divide(absA)).divide(absA);

        // A = sqrt(µ * |a|)
        this.A = FastMath.sqrt(mu.multiply(absA));

        // &Chi; = 1 / B
        final T B = auxiliaryElements.getB();
        this.chi = auxiliaryElements.getB().reciprocal();
        this.chi2 = chi.multiply(chi);

        // Common factors from equinoctial coefficients
        // 2 * a / A
        this.ax2oA = a.divide(A).multiply(2.);
        // B / A
        this.BoA = B.divide(A);;
        // 1 / AB
        this.ooAB = A.multiply(B).reciprocal();;
        // C / 2AB
        this.Co2AB =  auxiliaryElements.getC().multiply(ooAB).divide(2.);;
        // B / (A * (1 + B))
        this.BoABpo = BoA.divide(B.add(1.));
        // &mu / a
        this.muoa = mu.divide(a);
        // R / a
        this.roa = a.divide(provider.getAe()).reciprocal();


        // If (centralBodyFrame == null), then centralBodyFrame = orbit frame (see DSSTZonal constructors for more on this).
        final Frame internalCentralBodyFrame = centralBodyFixedFrame == null ? auxiliaryElements.getFrame() : centralBodyFixedFrame;

        // Transform from body-fixed frame (typically ITRF) to inertial frame
        bodyFixedToInertialTransform = internalCentralBodyFrame.
                        getStaticTransformTo(auxiliaryElements.getFrame(), auxiliaryElements.getDate());

        final FieldVector3D<T> zB = bodyFixedToInertialTransform.transformVector(Vector3D.PLUS_K);

        // Direction cosines for central body [Eq. 2.1.9-(1)]
        alpha = FieldVector3D.dotProduct(zB, auxiliaryElements.getVectorF());
        beta  = FieldVector3D.dotProduct(zB, auxiliaryElements.getVectorG());
        gamma = FieldVector3D.dotProduct(zB, auxiliaryElements.getVectorW());
    }

    /** A = sqrt(μ * a).
     * @return A
     */
    public T getA() {
        return A;
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

    /** Getter for the ax2oA.
     * @return the ax2oA
     */
    public T getAx2oA() {
        return ax2oA;
    }

    /** Get ooAB = 1 / (A * B).
     * @return ooAB
     */
    public T getOoAB() {
        return ooAB;
    }

    /** Get B / A.
     * @return BoA
     */
    public T getBoA() {
        return BoA;
    }

    /** Get BoABpo = B / A(1 + B).
     * @return BoABpo
     */
    public T getBoABpo() {
        return BoABpo;
    }

    /** Get Co2AB = C / 2AB.
     * @return Co2AB
     */
    public T getCo2AB() {
        return Co2AB;
    }

    /** Get muoa = μ / a.
     * @return the muoa
     */
    public T getMuoa() {
        return muoa;
    }

    /** Get roa = R / a.
     * @return roa
     */
    public T getRoa() {
        return roa;
    }

    /** Get the Keplerian mean motion.
     * <p>The Keplerian mean motion is computed directly from semi major axis
     * and central acceleration constant.</p>
     * @return Keplerian mean motion in radians per second
     */
    public T getMeanMotion() {
        return n;
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
     * @return the γ
     */
    public T getGamma() {
        return gamma;
    }

    /** Getter for the bodyFixedToInertialTransform.
     * @return the bodyFixedToInertialTransform
     */
    public FieldStaticTransform<T> getBodyFixedToInertialTransform() {
        return bodyFixedToInertialTransform;
    }
}
