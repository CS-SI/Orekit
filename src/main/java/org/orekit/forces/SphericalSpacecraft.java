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
package org.orekit.forces;

import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.apache.commons.math3.geometry.euclidean.threed.FieldRotation;
import org.apache.commons.math3.geometry.euclidean.threed.FieldVector3D;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.forces.drag.DragSensitive;
import org.orekit.forces.drag.IsotropicDrag;
import org.orekit.forces.radiation.RadiationSensitive;
import org.orekit.forces.radiation.IsotropicRadiationCNES95Convention;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;

/** This class represents the features of a simplified spacecraft.
 * <p>The model of this spacecraft is a simple spherical model, this
 * means that all coefficients are constant and do not depend of
 * the direction.</p>
 *
 * @see BoxAndSolarArraySpacecraft
 * @see IsotropicDrag
 * @see IsotropicRadiationCNES95Convention
 * @author &Eacute;douard Delente
 * @author Fabien Maussion
 * @author Pascal Parraud
 * @deprecated as of 3.1, replaced with {@link IsotropicDrag}, and either
 * {@link org.orekit.forces.radiation.IsotropicRadiationSingleCoefficient},
 * {@link org.orekit.forces.radiation.IsotropicRadiationClassicalConvention}
 * or {@link IsotropicRadiationCNES95Convention}
 */
@Deprecated
public class SphericalSpacecraft implements RadiationSensitive, DragSensitive {

    /** Drag part. */
    private IsotropicDrag drag;

    /** Radiation part. */
    private IsotropicRadiationCNES95Convention radiation;

    /** Simple constructor.
     * @param crossSection Surface (m²)
     * @param dragCoeff drag coefficient (used only for drag)
     * @param absorptionCoeff absorption coefficient between 0.0 an 1.0
     * (used only for radiation pressure)
     * @param reflectionCoeff specular reflection coefficient between 0.0 an 1.0
     * (used only for radiation pressure)
     */
    public SphericalSpacecraft(final double crossSection, final double dragCoeff,
                               final double absorptionCoeff, final double reflectionCoeff) {
        this.drag      = new IsotropicDrag(crossSection, dragCoeff);
        this.radiation = new IsotropicRadiationCNES95Convention(crossSection, absorptionCoeff, reflectionCoeff);
    }

    /** {@inheritDoc} */
    public Vector3D dragAcceleration(final AbsoluteDate date, final Frame frame, final Vector3D position,
                                     final Rotation rotation, final double mass,
                                     final double density, final Vector3D relativeVelocity) {
        return drag.dragAcceleration(date, frame, position, rotation, mass, density, relativeVelocity);
    }

    /** {@inheritDoc} */
    public FieldVector3D<DerivativeStructure> dragAcceleration(final AbsoluteDate date, final Frame frame, final FieldVector3D<DerivativeStructure> position,
                                                               final FieldRotation<DerivativeStructure> rotation, final DerivativeStructure mass,
                                                               final DerivativeStructure density, final FieldVector3D<DerivativeStructure> relativeVelocity) {
        return drag.dragAcceleration(date, frame, position, rotation, mass, density, relativeVelocity);
    }

    /** {@inheritDoc} */
    public FieldVector3D<DerivativeStructure> dragAcceleration(final AbsoluteDate date, final Frame frame, final Vector3D position,
                                                               final Rotation rotation, final double mass,
                                                               final  double density, final Vector3D relativeVelocity,
                                                               final String paramName)
        throws OrekitException {
        return drag.dragAcceleration(date, frame, position, rotation, mass, density, relativeVelocity, paramName);
    }

    /** {@inheritDoc} */
    public Vector3D radiationPressureAcceleration(final AbsoluteDate date, final Frame frame, final Vector3D position,
                                                  final Rotation rotation, final double mass, final Vector3D flux) {
        return radiation.radiationPressureAcceleration(date, frame, position, rotation, mass, flux);
    }

    /** {@inheritDoc} */
    public FieldVector3D<DerivativeStructure> radiationPressureAcceleration(final AbsoluteDate date, final Frame frame, final FieldVector3D<DerivativeStructure> position,
                                                                            final FieldRotation<DerivativeStructure> rotation, final DerivativeStructure mass,
                                                                            final FieldVector3D<DerivativeStructure> flux) {
        return radiation.radiationPressureAcceleration(date, frame, position, rotation, mass, flux);
    }

    /** {@inheritDoc} */
    public FieldVector3D<DerivativeStructure> radiationPressureAcceleration(final AbsoluteDate date, final Frame frame, final Vector3D position,
                                                                            final Rotation rotation, final double mass,
                                                                            final Vector3D flux, final String paramName)
        throws OrekitException {
        return radiation.radiationPressureAcceleration(date, frame, position, rotation, mass, flux, paramName);
    }

    /** {@inheritDoc} */
    public void setDragCoefficient(final double value) {
        drag.setDragCoefficient(value);
    }

    /** {@inheritDoc} */
    public double getDragCoefficient() {
        return drag.getDragCoefficient();
    }

    /** {@inheritDoc} */
    public void setAbsorptionCoefficient(final double value) {
        radiation.setAbsorptionCoefficient(value);
    }

    /** {@inheritDoc} */
    public double getAbsorptionCoefficient() {
        return radiation.getAbsorptionCoefficient();
    }

    /** {@inheritDoc} */
    public void setReflectionCoefficient(final double value) {
        radiation.setReflectionCoefficient(value);
    }

    /** {@inheritDoc} */
    public double getReflectionCoefficient() {
        return radiation.getReflectionCoefficient();
    }

}
