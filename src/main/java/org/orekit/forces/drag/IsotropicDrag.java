/* Copyright 2002-2016 CS Systèmes d'Information
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
package org.orekit.forces.drag;

import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.apache.commons.math3.geometry.euclidean.threed.FieldRotation;
import org.apache.commons.math3.geometry.euclidean.threed.FieldVector3D;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.drag.DragSensitive;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;

/** This class models isotropic drag effects.
 * <p>The model of this spacecraft is a simple spherical model, this
 * means that all coefficients are constant and do not depend of
 * the direction.</p>
 *
 * @see org.orekit.forces.BoxAndSolarArraySpacecraft
 * @see org.orekit.forces.radiation.IsotropicRadiationCNES95Convention
 * @author Luc Maisonobe
 * @since 7.1
 */
public class IsotropicDrag implements DragSensitive {

    /** Cross section (m²). */
    private final double crossSection;

    /** Drag coefficient. */
    private double dragCoeff;

    /** Simple constructor.
     * @param crossSection Surface (m²)
     * @param dragCoeff drag coefficient
     */
    public IsotropicDrag(final double crossSection, final double dragCoeff) {
        this.crossSection = crossSection;
        this.dragCoeff    = dragCoeff;
    }

    /** {@inheritDoc} */
    public Vector3D dragAcceleration(final AbsoluteDate date, final Frame frame, final Vector3D position,
                                     final Rotation rotation, final double mass,
                                     final double density, final Vector3D relativeVelocity) {
        return new Vector3D(relativeVelocity.getNorm() * density * dragCoeff * crossSection / (2 * mass),
                            relativeVelocity);
    }

    /** {@inheritDoc} */
    public FieldVector3D<DerivativeStructure> dragAcceleration(final AbsoluteDate date, final Frame frame, final FieldVector3D<DerivativeStructure> position,
                                                               final FieldRotation<DerivativeStructure> rotation, final DerivativeStructure mass,
                                                               final DerivativeStructure density, final FieldVector3D<DerivativeStructure> relativeVelocity) {
        return new FieldVector3D<DerivativeStructure>(relativeVelocity.getNorm().multiply(density.multiply(dragCoeff * crossSection / 2)).divide(mass),
                              relativeVelocity);
    }

    /** {@inheritDoc} */
    public FieldVector3D<DerivativeStructure> dragAcceleration(final AbsoluteDate date, final Frame frame, final Vector3D position,
                                                               final Rotation rotation, final double mass,
                                                               final  double density, final Vector3D relativeVelocity,
                                                               final String paramName)
        throws OrekitException {

        if (!DRAG_COEFFICIENT.equals(paramName)) {
            throw new OrekitException(OrekitMessages.UNSUPPORTED_PARAMETER_NAME, paramName, DRAG_COEFFICIENT);
        }

        final DerivativeStructure dragCoeffDS = new DerivativeStructure(1, 1, 0, dragCoeff);

        return new FieldVector3D<DerivativeStructure>(dragCoeffDS.multiply(relativeVelocity.getNorm() * density * crossSection / (2 * mass)),
                              relativeVelocity);

    }

    /** {@inheritDoc} */
    public void setDragCoefficient(final double value) {
        dragCoeff = value;
    }

    /** {@inheritDoc} */
    public double getDragCoefficient() {
        return dragCoeff;
    }

}
