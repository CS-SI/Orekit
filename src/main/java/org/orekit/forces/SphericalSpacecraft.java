/* Copyright 2002-2013 CS Systèmes d'Information
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
package org.orekit.forces;

import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.apache.commons.math3.geometry.euclidean.threed.FieldRotation;
import org.apache.commons.math3.geometry.euclidean.threed.FieldVector3D;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.drag.DragSensitive;
import org.orekit.forces.radiation.RadiationSensitive;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;

/** This class represents the features of a simplified spacecraft.
 * <p>The model of this spacecraft is a simple spherical model, this
 * means that all coefficients are constant and do not depend of
 * the direction.</p>
 * <p>Instances of this class are guaranteed to be immutable.</p>
 *
 * @see BoxAndSolarArraySpacecraft
 * @author &Eacute;douard Delente
 * @author Fabien Maussion
 * @author Pascal Parraud
 */
public class SphericalSpacecraft implements RadiationSensitive, DragSensitive {

    /** Cross section (m<sup>2</sup>). */
    private final double crossSection;

    /** Drag coefficient. */
    private double dragCoeff;

    /** Absorption coefficient. */
    private double absorptionCoeff;

    /** Specular reflection coefficient. */
    private double specularReflectionCoeff;

    /** Simple constructor.
     * @param crossSection Surface (m<sup>2</sup>)
     * @param dragCoeff drag coefficient (used only for drag)
     * @param absorptionCoeff absorption coefficient between 0.0 an 1.0
     * (used only for radiation pressure)
     * @param reflectionCoeff specular reflection coefficient between 0.0 an 1.0
     * (used only for radiation pressure)
     */
    public SphericalSpacecraft(final double crossSection, final double dragCoeff,
                               final double absorptionCoeff, final double reflectionCoeff) {
        this.crossSection            = crossSection;
        this.dragCoeff               = dragCoeff;
        this.absorptionCoeff         = absorptionCoeff;
        this.specularReflectionCoeff = reflectionCoeff;
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
                                                               final double density, final FieldVector3D<DerivativeStructure> relativeVelocity) {
        return new FieldVector3D<DerivativeStructure>(relativeVelocity.getNorm().multiply(density * dragCoeff * crossSection / 2).divide(mass),
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
    public Vector3D radiationPressureAcceleration(final AbsoluteDate date, final Frame frame, final Vector3D position,
                                                  final Rotation rotation, final double mass, final Vector3D flux) {
        final double kP = crossSection * (1 + 4 * (1.0 - absorptionCoeff) * (1.0 - specularReflectionCoeff) / 9.0);
        return new Vector3D(kP / mass, flux);
    }

    /** {@inheritDoc} */
    public FieldVector3D<DerivativeStructure> radiationPressureAcceleration(final AbsoluteDate date, final Frame frame, final FieldVector3D<DerivativeStructure> position,
                                                    final FieldRotation<DerivativeStructure> rotation, final DerivativeStructure mass,
                                                    final FieldVector3D<DerivativeStructure> flux) {
        final double kP = crossSection * (1 + 4 * (1.0 - absorptionCoeff) * (1.0 - specularReflectionCoeff) / 9.0);
        return new FieldVector3D<DerivativeStructure>(mass.reciprocal().multiply(kP), flux);
    }

    /** {@inheritDoc} */
    public FieldVector3D<DerivativeStructure> radiationPressureAcceleration(final AbsoluteDate date, final Frame frame, final Vector3D position,
                                                    final Rotation rotation, final double mass,
                                                    final Vector3D flux, final String paramName)
        throws OrekitException {

        final DerivativeStructure absorptionCoeffDS;
        final DerivativeStructure specularReflectionCoeffDS;
        if (ABSORPTION_COEFFICIENT.equals(paramName)) {
            absorptionCoeffDS         = new DerivativeStructure(1, 1, 0, absorptionCoeff);
            specularReflectionCoeffDS = new DerivativeStructure(1, 1,    specularReflectionCoeff);
        } else if (REFLECTION_COEFFICIENT.equals(paramName)) {
            absorptionCoeffDS         = new DerivativeStructure(1, 1,    absorptionCoeff);
            specularReflectionCoeffDS = new DerivativeStructure(1, 1, 0, specularReflectionCoeff);
        } else {
            throw new OrekitException(OrekitMessages.UNSUPPORTED_PARAMETER_NAME, paramName,
                                      ABSORPTION_COEFFICIENT + ", " + REFLECTION_COEFFICIENT);
        }

        final DerivativeStructure kP =
                absorptionCoeffDS.subtract(1).multiply(specularReflectionCoeffDS.subtract(1)).multiply(4.0 / 9.0).add(1).multiply(crossSection);
        return new FieldVector3D<DerivativeStructure>(kP.divide(mass), flux);

    }

    /** {@inheritDoc} */
    public void setDragCoefficient(final double value) {
        dragCoeff = value;
    }

    /** {@inheritDoc} */
    public double getDragCoefficient() {
        return dragCoeff;
    }

    /** {@inheritDoc} */
    public void setAbsorptionCoefficient(final double value) {
        absorptionCoeff = value;
    }

    /** {@inheritDoc} */
    public double getAbsorptionCoefficient() {
        return absorptionCoeff;
    }

    /** {@inheritDoc} */
    public void setReflectionCoefficient(final double value) {
        specularReflectionCoeff = value;
    }

    /** {@inheritDoc} */
    public double getReflectionCoefficient() {
        return specularReflectionCoeff;
    }

}
