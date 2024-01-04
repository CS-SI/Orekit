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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.frames.Frame;
import org.orekit.frames.StaticTransform;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;

/**
 * This class is a container for the common parameters used in {@link DSSTTesseral} and {@link DSSTZonal}.
 * <p>
 * It performs parameters initialization at each integration step for the Tesseral  and Zonal contribution
 * to the central body gravitational perturbation.
 * </p>
 * @author Maxime Journot
 * @since 12.1
 */
public class DSSTGravityContext extends ForceModelContext {

    /** Direction cosine α. */
    private final double alpha;

    /** Direction cosine β. */
    private final double beta;

    /** Direction cosine γ. */
    private final double gamma;

    /** Transform from central body frame to inertial frame. */
    private final StaticTransform centralBodyToInertialTransform;

    /**
     * Constructor.
     *
     * @param auxiliaryElements auxiliary elements related to the current orbit
     * @param centralBodyFrame  rotating body frame
     */
    DSSTGravityContext(final AuxiliaryElements auxiliaryElements,
                       final Frame centralBodyFrame) {

        super(auxiliaryElements);

        // If (centralBodyFrame == null), then centralBodyFrame = orbit frame (see DSSTZonal constructors for more on this).
        final Frame internalCentralBodyFrame = centralBodyFrame == null ? auxiliaryElements.getFrame() : centralBodyFrame;

        // Central body rotation angle from equation 2.7.1-(3)(4).
        centralBodyToInertialTransform = internalCentralBodyFrame.
                        getStaticTransformTo(auxiliaryElements.getFrame(), auxiliaryElements.getDate());

        final Vector3D zB = centralBodyToInertialTransform.transformVector(Vector3D.PLUS_K);

        alpha = Vector3D.dotProduct(zB, auxiliaryElements.getVectorF());
        beta  = Vector3D.dotProduct(zB, auxiliaryElements.getVectorG());
        gamma = Vector3D.dotProduct(zB, auxiliaryElements.getVectorW());
    }

    /** Getter for the alpha.
     * @return the alpha
     */
    public double getAlpha() {
        return alpha;
    }

    /** Getter for the beta.
     * @return the beta
     */
    public double getBeta() {
        return beta;
    }

    /** Getter for the gamma.
     * @return the gamma
     */
    public double getGamma() {
        return gamma;
    }

    /** Getter for the centralBodyToInertialTransform.
     * @return the centralBodyToInertialTransform
     */
    public StaticTransform getCentralBodyToInertialTransform() {
        return centralBodyToInertialTransform;
    }
}
