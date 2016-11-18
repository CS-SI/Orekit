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

import org.hipparchus.RealFieldElement;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterObserver;

/** This class represents the features of a simplified spacecraft.
 *
 * <p>This model uses the coefficients described in the collective
 * book edited by CNES in 1995: Spaceflight Dynamics (part I), in
 * section 5.2.2.1.3.1 (page 296 of the English edition). The absorption
 * coefficient is called α and the specular reflection coefficient is
 * called τ. A comment in section 5.2.2.1.3.2 of the same book reads:
 * <pre>
 * Some authors prefer to express thermo-optical properties for surfaces
 * using the following coefficients: Ka = α, Ks = (1-α)τ, Kd = (1-α)(1-τ)
 * </pre>
 * <p> Ka is the same absorption coefficient, and Ks is also called specular
 * reflection coefficient, which leads to a confusion. In fact, as the Ka,
 * Ks and Kd coefficients are the most frequently used ones (using the
 * names Ca, Cs and Cd), when speaking about reflection coefficients, it
 * is more often Cd that is considered rather than τ.
 *
 * <p>
 * The classical set of coefficients Ca, Cs, and Cd are implemented in the
 * sister class {@link IsotropicRadiationClassicalConvention}, which should
 * probably be preferred to this legacy class.
 * </p>
 *
 * @see org.orekit.forces.BoxAndSolarArraySpacecraft
 * @see org.orekit.forces.drag.IsotropicDrag
 * @see IsotropicRadiationClassicalConvention
 * @author Luc Maisonobe
 * @since 7.1
 */
public class IsotropicRadiationCNES95Convention implements RadiationSensitive {

    /** Parameters scaling factor.
     * <p>
     * We use a power of 2 to avoid numeric noise introduction
     * in the multiplications/divisions sequences.
     * </p>
     */
    private final double SCALE = FastMath.scalb(1.0, -3);

    /** Drivers for radiation pressure coefficient parameter. */
    private final ParameterDriver[] radiationParametersDrivers;

    /** Cross section (m²). */
    private final double crossSection;

    /** Absorption coefficient. */
    private double alpha;

    /** Specular reflection coefficient. */
    private double tau;

    /** Simple constructor.
     * @param crossSection Surface (m²)
     * @param alpha absorption coefficient α between 0.0 an 1.0
     * @param tau specular reflection coefficient τ between 0.0 an 1.0
     */
    public IsotropicRadiationCNES95Convention(final double crossSection, final double alpha, final double tau) {
        this.radiationParametersDrivers = new ParameterDriver[2];
        try {
            radiationParametersDrivers[0] = new ParameterDriver(RadiationSensitive.ABSORPTION_COEFFICIENT,
                                                                alpha, SCALE, 0.0, 1.0);
            radiationParametersDrivers[0].addObserver(new ParameterObserver() {
                /** {@inheritDoc} */
                @Override
                public void valueChanged(final double previousValue, final ParameterDriver driver) {
                    IsotropicRadiationCNES95Convention.this.alpha = driver.getValue();
                }
            });
            radiationParametersDrivers[1] = new ParameterDriver(RadiationSensitive.REFLECTION_COEFFICIENT,
                                                                tau, SCALE, 0.0, 1.0);
            radiationParametersDrivers[1].addObserver(new ParameterObserver() {
                /** {@inheritDoc} */
                @Override
                public void valueChanged(final double previousValue, final ParameterDriver driver) {
                /** {@inheritDoc} */
                    IsotropicRadiationCNES95Convention.this.tau = driver.getValue();
                }
            });
        } catch (OrekitException oe) {
            // this should never occur as valueChanged above never throws an exception
            throw new OrekitInternalError(oe);
        };
        this.crossSection = crossSection;
        this.alpha        = alpha;
        this.tau          = tau;
    }

    /** {@inheritDoc} */
    @Override
    public ParameterDriver[] getRadiationParametersDrivers() {
        return radiationParametersDrivers.clone();
    }

    /** {@inheritDoc} */
    public Vector3D radiationPressureAcceleration(final AbsoluteDate date, final Frame frame, final Vector3D position,
                                                  final Rotation rotation, final double mass, final Vector3D flux) {
        final double kP = crossSection * (1 + 4 * (1.0 - alpha) * (1.0 - tau) / 9.0);
        return new Vector3D(kP / mass, flux);
    }

    /** {@inheritDoc} */
    public FieldVector3D<DerivativeStructure> radiationPressureAcceleration(final AbsoluteDate date, final Frame frame, final FieldVector3D<DerivativeStructure> position,
                                                                            final FieldRotation<DerivativeStructure> rotation, final DerivativeStructure mass,
                                                                            final FieldVector3D<DerivativeStructure> flux) {
        final double kP = crossSection * (1 + 4 * (1.0 - alpha) * (1.0 - tau) / 9.0);
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
            absorptionCoeffDS         = new DerivativeStructure(1, 1, 0, alpha);
            specularReflectionCoeffDS = new DerivativeStructure(1, 1,    tau);
        } else if (REFLECTION_COEFFICIENT.equals(paramName)) {
            absorptionCoeffDS         = new DerivativeStructure(1, 1,    alpha);
            specularReflectionCoeffDS = new DerivativeStructure(1, 1, 0, tau);
        } else {
            throw new OrekitException(OrekitMessages.UNSUPPORTED_PARAMETER_NAME, paramName,
                                      ABSORPTION_COEFFICIENT + ", " + REFLECTION_COEFFICIENT);
        }

        final DerivativeStructure kP =
                absorptionCoeffDS.subtract(1).multiply(specularReflectionCoeffDS.subtract(1)).multiply(4.0 / 9.0).add(1).multiply(crossSection);
        return new FieldVector3D<DerivativeStructure>(kP.divide(mass), flux);

    }

    @Override
    public <T extends RealFieldElement<T>> FieldVector3D<T>
        radiationPressureAcceleration(final FieldAbsoluteDate<T> date, final Frame frame,
                                      final FieldVector3D<T> position,
                                      final FieldRotation<T> rotation, final T mass,
                                      final FieldVector3D<T> flux)
        throws OrekitException {
        final double kP = crossSection * (1 + 4 * (1.0 - alpha) * (1.0 - tau) / 9.0);
        return new FieldVector3D<T>(mass.reciprocal().multiply(kP), flux);
    }

}
