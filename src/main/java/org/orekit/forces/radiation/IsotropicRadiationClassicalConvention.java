/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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

import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ParameterDriver;

/** This class represents the features of a simplified spacecraft.
 * <p>This model uses the classical thermo-optical coefficients
 * Ca for absorption, Cs for specular reflection and Kd for diffuse
 * reflection. The equation Ca + Cs + Cd = 1 always holds.
 * </p>
 * <p>
 * A less standard set of coefficients α = Ca for absorption and
 * τ = Cs/(1-Ca) for specular reflection is implemented in the sister
 * class {@link IsotropicRadiationCNES95Convention}.
 * </p>
 *
 * @see org.orekit.forces.BoxAndSolarArraySpacecraft
 * @see org.orekit.forces.drag.IsotropicDrag
 * @see IsotropicRadiationCNES95Convention
 * @author Luc Maisonobe
 * @since 7.1
 */
public class IsotropicRadiationClassicalConvention implements RadiationSensitive {

    /** Parameters scaling factor.
     * <p>
     * We use a power of 2 to avoid numeric noise introduction
     * in the multiplications/divisions sequences.
     * </p>
     */
    private final double SCALE = FastMath.scalb(1.0, -3);

    /** Driver for absorption coefficient. */
    private final ParameterDriver absorptionParameterDriver;

    /** Driver for specular reflection coefficient. */
    private final ParameterDriver reflectionParameterDriver;

    /** Cross section (m²). */
    private final double crossSection;

    /** Simple constructor.
     * @param crossSection Surface (m²)
     * @param ca absorption coefficient Ca between 0.0 an 1.0
     * @param cs specular reflection coefficient Cs between 0.0 an 1.0
     */
    public IsotropicRadiationClassicalConvention(final double crossSection, final double ca, final double cs) {
        try {
            absorptionParameterDriver = new ParameterDriver(RadiationSensitive.ABSORPTION_COEFFICIENT,
                                                            ca, SCALE, 0.0, 1.0);
            reflectionParameterDriver = new ParameterDriver(RadiationSensitive.REFLECTION_COEFFICIENT,
                                                            cs, SCALE, 0.0, 1.0);
        } catch (OrekitException oe) {
            // this should never occur as valueChanged above never throws an exception
            throw new OrekitInternalError(oe);
        }
        this.crossSection = crossSection;
    }

    /** {@inheritDoc} */
    @Override
    public ParameterDriver[] getRadiationParametersDrivers() {
        return new ParameterDriver[] {
            absorptionParameterDriver, reflectionParameterDriver
        };
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D radiationPressureAcceleration(final AbsoluteDate date, final Frame frame, final Vector3D position,
                                                  final Rotation rotation, final double mass, final Vector3D flux,
                                                  final double[] parameters) {
        final double ca = parameters[0];
        final double cs = parameters[1];
        final double kP = crossSection * (1 + 4 * (1.0 - ca - cs) / 9.0);
        return new Vector3D(kP / mass, flux);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends RealFieldElement<T>> FieldVector3D<T>
        radiationPressureAcceleration(final FieldAbsoluteDate<T> date, final Frame frame,
                                      final FieldVector3D<T> position,
                                      final FieldRotation<T> rotation, final T mass,
                                      final FieldVector3D<T> flux,
                                      final T[] parameters) {
        final T ca = parameters[0];
        final T cs = parameters[1];
        final T kP = ca.add(cs).negate().add(1).multiply(4.0 / 9.0).add(1).multiply(crossSection);
        return new FieldVector3D<>(mass.reciprocal().multiply(kP), flux);
    }
}
