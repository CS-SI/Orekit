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
package org.orekit.forces.inertia;

import java.util.Collections;
import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.ForceModel;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.ParameterDriver;

/** Inertial force model.
 * <p>
 * This force model adds the pseudo-forces due to inertia between the
 * integrating frame and a reference inertial frame from which
 * this force model is built.
 * </p>
 * <p>
 * Two typical use-cases are propagating {@link AbsolutePVCoordinates} in either:
 * </p>
 * <ul>
 *   <li>a non-inertial frame (for example propagating in the rotating {@link
 *       org.orekit.frames.FramesFactory#getITRF(org.orekit.utils.IERSConventions, boolean) ITRF}
 *       frame),</li>
 *   <li>an inertial frame that is not related to the main attracting body (for example
 *       propagating in {@link org.orekit.frames.FramesFactory#getEME2000() EME2000} frame a
 *       trajectory about the Sun and Jupiter).</li>
 * </ul>
 * <p>
 * In the second used case above, the attraction from the two main bodies, i.e. the Sun and
 * Jupiter, should be represented by {@link org.orekit.forces.gravity.SingleBodyAbsoluteAttraction}
 * instances.
 * </p>
 * @see org.orekit.forces.gravity.SingleBodyAbsoluteAttraction
 * @author Guillaume Obrecht
 * @author Luc Maisonobe
 */
public class InertialForces implements ForceModel {

    /** Reference inertial frame to use to compute inertial forces. */
    private Frame referenceInertialFrame;

    /** Simple constructor.
     * @param referenceInertialFrame the pseudo-inertial frame to use as reference for the inertial forces
     * @exception OrekitIllegalArgumentException if frame is not a {@link
     * Frame#isPseudoInertial pseudo-inertial frame}
     */
    public InertialForces(final Frame referenceInertialFrame)
        throws OrekitIllegalArgumentException {
        if (!referenceInertialFrame.isPseudoInertial()) {
            throw new OrekitIllegalArgumentException(OrekitMessages.NON_PSEUDO_INERTIAL_FRAME_NOT_SUITABLE_AS_REFERENCE_FOR_INERTIAL_FORCES,
                                                     referenceInertialFrame.getName());
        }
        this.referenceInertialFrame = referenceInertialFrame;
    }

    /** {@inheritDoc} */
    @Override
    public boolean dependsOnPositionOnly() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D acceleration(final SpacecraftState s, final double[] parameters) {

        final Transform inertToStateFrame = referenceInertialFrame.getTransformTo(s.getFrame(), s.getDate());
        final Vector3D  a1                = inertToStateFrame.getCartesian().getAcceleration();
        final Rotation  r1                = inertToStateFrame.getAngular().getRotation();
        final Vector3D  o1                = inertToStateFrame.getAngular().getRotationRate();
        final Vector3D  oDot1             = inertToStateFrame.getAngular().getRotationAcceleration();

        final Vector3D  p2                = s.getPosition();
        final Vector3D  v2                = s.getPVCoordinates().getVelocity();

        final Vector3D crossCrossP        = Vector3D.crossProduct(o1,    Vector3D.crossProduct(o1, p2));
        final Vector3D crossV             = Vector3D.crossProduct(o1,    v2);
        final Vector3D crossDotP          = Vector3D.crossProduct(oDot1, p2);

        // we intentionally DON'T include s.getPVCoordinates().getAcceleration()
        // because we want only the coupling effect of the frames transforms
        return r1.applyTo(a1).subtract(new Vector3D(2, crossV, 1, crossCrossP, 1, crossDotP));

    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldVector3D<T> acceleration(final FieldSpacecraftState<T> s,
                                                                         final T[] parameters) {

        final FieldTransform<T> inertToStateFrame = referenceInertialFrame.getTransformTo(s.getFrame(), s.getDate());
        final FieldVector3D<T>  a1                = inertToStateFrame.getCartesian().getAcceleration();
        final FieldRotation<T>  r1                = inertToStateFrame.getAngular().getRotation();
        final FieldVector3D<T>  o1                = inertToStateFrame.getAngular().getRotationRate();
        final FieldVector3D<T>  oDot1             = inertToStateFrame.getAngular().getRotationAcceleration();

        final FieldVector3D<T>  p2                = s.getPosition();
        final FieldVector3D<T>  v2                = s.getPVCoordinates().getVelocity();

        final FieldVector3D<T> crossCrossP        = FieldVector3D.crossProduct(o1,    FieldVector3D.crossProduct(o1, p2));
        final FieldVector3D<T> crossV             = FieldVector3D.crossProduct(o1,    v2);
        final FieldVector3D<T> crossDotP          = FieldVector3D.crossProduct(oDot1, p2);

        // we intentionally DON'T include s.getPVCoordinates().getAcceleration()
        // because we want only the coupling effect of the frames transforms
        return r1.applyTo(a1).subtract(new FieldVector3D<>(2, crossV, 1, crossCrossP, 1, crossDotP));

    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.emptyList();
    }
}
