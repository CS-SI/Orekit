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
package org.orekit.forces.radiation;

import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.apache.commons.math3.geometry.euclidean.threed.FieldRotation;
import org.apache.commons.math3.geometry.euclidean.threed.FieldVector3D;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.radiation.RadiationSensitive;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;

/** This class represents the features of a simplified spacecraft.
 * <p>This model uses a single coefficient cr, considered to be
 * a {@link RadiationSensitive#REFLECTION_COEFFICIENT}.
 * </p>
 *
 * @see org.orekit.forces.BoxAndSolarArraySpacecraft
 * @see org.orekit.forces.drag.IsotropicDrag
 * @see IsotropicRadiationCNES95Convention
 * @author Luc Maisonobe
 * @since 7.1
 */
public class IsotropicRadiationSingleCoefficient implements RadiationSensitive {

    /** Cross section (m²). */
    private final double crossSection;

    /** Reflection coefficient. */
    private double cr;

    /** Simple constructor.
     * @param crossSection Surface (m²)
     * @param cr reflection coefficient
     */
    public IsotropicRadiationSingleCoefficient(final double crossSection, final double cr) {
        this.crossSection = crossSection;
        this.cr           = cr;
    }

    /** {@inheritDoc} */
    public Vector3D radiationPressureAcceleration(final AbsoluteDate date, final Frame frame, final Vector3D position,
                                                  final Rotation rotation, final double mass, final Vector3D flux) {
        return new Vector3D(crossSection * cr / mass, flux);
    }

    /** {@inheritDoc} */
    public FieldVector3D<DerivativeStructure> radiationPressureAcceleration(final AbsoluteDate date, final Frame frame, final FieldVector3D<DerivativeStructure> position,
                                                                            final FieldRotation<DerivativeStructure> rotation, final DerivativeStructure mass,
                                                                            final FieldVector3D<DerivativeStructure> flux) {
        return new FieldVector3D<DerivativeStructure>(mass.reciprocal().multiply(crossSection * cr), flux);
    }

    /** {@inheritDoc} */
    public FieldVector3D<DerivativeStructure> radiationPressureAcceleration(final AbsoluteDate date, final Frame frame, final Vector3D position,
                                                                            final Rotation rotation, final double mass,
                                                                            final Vector3D flux, final String paramName)
        throws OrekitException {

        final DerivativeStructure crDS;
        if (REFLECTION_COEFFICIENT.equals(paramName)) {
            crDS = new DerivativeStructure(1, 1, 0, cr);
        } else {
            throw new OrekitException(OrekitMessages.UNSUPPORTED_PARAMETER_NAME, paramName,
                                      ABSORPTION_COEFFICIENT + ", " + REFLECTION_COEFFICIENT);
        }

        return new FieldVector3D<DerivativeStructure>(crDS.multiply(crossSection / mass), flux);

    }

    /** {@inheritDoc}
     * <p>
     * As there are no absorption coefficients, this method
     * throws an {@link UnsupportedOperationException}.
     * </p>
     */
    public void setAbsorptionCoefficient(final double value)
        throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc}
     * <p>
     * As there are no absorption coefficients, this method
     * always returns 0.0.
     * </p>
     */
    public double getAbsorptionCoefficient() {
        return 0;
    }

    /** {@inheritDoc} */
    public void setReflectionCoefficient(final double value) {
        cr = value;
    }

    /** {@inheritDoc} */
    public double getReflectionCoefficient() {
        return cr;
    }

}
