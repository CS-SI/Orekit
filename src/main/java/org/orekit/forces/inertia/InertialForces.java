/* Copyright 2002-2017 CS Systèmes d'Information
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
package org.orekit.forces.inertia;

import java.util.stream.Stream;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.AbstractForceModel;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.numerical.FieldTimeDerivativesEquations;
import org.orekit.utils.ParameterDriver;

/** Inertial force model.
 *
 * @author Guillaume Obrecht
 *
 */
public class InertialForces extends AbstractForceModel  {

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
    public Vector3D acceleration(final SpacecraftState s, final double[] parameters)
        throws OrekitException {

        if (s.getFrame() == referenceInertialFrame) {
            return Vector3D.ZERO;
        } else {
            // TODO check and clean

//            // Method 1: use (private) method compositeAcceleration() from Transform)
//            final Transform noninertToInert = referenceInertialFrame.getTransformTo(s.getFrame(), s.getDate());
//            final Transform noninertToSat = s.toTransform();
//            Vector3D inertialAccelerations = Transform.compositeAcceleration(noninertToInert, noninertToSat);
//            inertialAccelerations = noninertToInert.transformVector(inertialAccelerations);

//            // Method 2: extract acceleration from Transform
//            final Transform noninertToInert = referenceInertialFrame.getTransformTo(s.getFrame(), s.getDate());
//            final Transform noninertToSat = s.toTransform();
//            final Transform combined = new Transform(s.getDate(), noninertToInert, noninertToSat);
//            Vector3D inertialAccelerations = combined.getAcceleration();
//            inertialAccelerations = noninertToInert.transformVector(combined.getAcceleration());

            // Method 3: Formulas for inertial accelerations
            final Transform noninertToInert = referenceInertialFrame.getTransformTo(s.getFrame(), s.getDate());
            final Vector3D rotationRate = noninertToInert.getRotationRate();
            final Vector3D rotationAcceleration = noninertToInert.getRotationAcceleration();
            final Vector3D satPosition = s.getPVCoordinates().getPosition();
            final Vector3D satVelocity = s.getPVCoordinates().getVelocity();

            // Relative acceleration
            final Vector3D aRelative = noninertToInert.getAcceleration();

            // Centrifugal acceleration
            final Vector3D aCentrifug = Vector3D.crossProduct(rotationRate, Vector3D.crossProduct(rotationRate, satPosition));

            // Euler acceleration
            final Vector3D aEuler = Vector3D.crossProduct(rotationAcceleration, satPosition);

            // Coriolis acceleration
            final Vector3D aCoriolis = Vector3D.crossProduct(rotationRate, satVelocity);

            // Total of inertial accelerations
            return new Vector3D(1, aRelative, -1, aCentrifug, -1, aEuler, -2, aCoriolis);

        }
    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> FieldVector3D<T> acceleration(final FieldSpacecraftState<T> s,
                                                                         final T[] parameters)
        throws OrekitException {

        if (s.getFrame() == referenceInertialFrame) {
            return FieldVector3D.getZero(s.getDate().getField());
        } else {
            // TODO check and clean

//            // Method 1: use (private) method compositeAcceleration() from Transform)
//            final Transform noninertToInert = referenceInertialFrame.getTransformTo(s.getFrame(), s.getDate());
//            final Transform noninertToSat = s.toTransform();
//            Vector3D inertialAccelerations = Transform.compositeAcceleration(noninertToInert, noninertToSat);
//            inertialAccelerations = noninertToInert.transformVector(inertialAccelerations);

//            // Method 2: extract acceleration from Transform
//            final Transform noninertToInert = referenceInertialFrame.getTransformTo(s.getFrame(), s.getDate());
//            final Transform noninertToSat = s.toTransform();
//            final Transform combined = new Transform(s.getDate(), noninertToInert, noninertToSat);
//            Vector3D inertialAccelerations = combined.getAcceleration();
//            inertialAccelerations = noninertToInert.transformVector(combined.getAcceleration());

            // Method 3: Formulas for inertial accelerations
            final FieldTransform<T> noninertToInert = referenceInertialFrame.getTransformTo(s.getFrame(), s.getDate());
            final FieldVector3D<T> rotationRate = noninertToInert.getRotationRate();
            final FieldVector3D<T> rotationAcceleration = noninertToInert.getRotationAcceleration();
            final FieldVector3D<T> satPosition = s.getPVCoordinates().getPosition();
            final FieldVector3D<T> satVelocity = s.getPVCoordinates().getVelocity();

            // Relative acceleration
            final FieldVector3D<T> aRelative = noninertToInert.getAcceleration();

            // Centrifugal acceleration
            final FieldVector3D<T> aCentrifug = FieldVector3D.crossProduct(rotationRate,
                                                                           FieldVector3D.crossProduct(rotationRate, satPosition));

            // Euler acceleration
            final FieldVector3D<T> aEuler = FieldVector3D.crossProduct(rotationAcceleration, satPosition);

            // Coriolis acceleration
            final FieldVector3D<T> aCoriolis = FieldVector3D.crossProduct(rotationRate, satVelocity);

            // Total of inertial accelerations
            return new FieldVector3D<T>(1, aRelative, -1, aCentrifug, -1, aEuler, -2, aCoriolis);

        }
    }

    /** {@inheritDoc} */
    @Override
    public Stream<EventDetector> getEventsDetectors() {
        return Stream.empty();
    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> void
        addContribution(final FieldSpacecraftState<T> s, final FieldTimeDerivativesEquations<T> adder)
            throws OrekitException {
        // TODO implement this method
    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> Stream<FieldEventDetector<T>>
        getFieldEventsDetectors(final Field<T> field) {
        return Stream.empty();
    }

    /** {@inheritDoc} */
    @Override
    public ParameterDriver[] getParametersDrivers() {
        return new ParameterDriver[0];
    }

    /** {@inheritDoc} */
    @Override
    public ParameterDriver getParameterDriver(final String name)
        throws OrekitException {
        throw new OrekitException(OrekitMessages.UNSUPPORTED_PARAMETER_NAME, "<none>");
    }

}
