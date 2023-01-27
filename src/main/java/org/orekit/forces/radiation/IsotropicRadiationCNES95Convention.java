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
package org.orekit.forces.radiation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.ParameterDriver;

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

    /** Drivers for absorption and specular reflection coefficients. */
    private final List<ParameterDriver> parameterDrivers;

    /** Cross section (m²). */
    private final double crossSection;

    /** Simple constructor.
     * @param crossSection Surface (m²)
     * @param alpha absorption coefficient α between 0.0 an 1.0
     * @param tau specular reflection coefficient τ between 0.0 an 1.0
     */
    public IsotropicRadiationCNES95Convention(final double crossSection, final double alpha, final double tau) {
        this.parameterDrivers = new ArrayList<>(3);
        parameterDrivers.add(new ParameterDriver(RadiationSensitive.GLOBAL_RADIATION_FACTOR, 1.0, SCALE, 0.0, Double.POSITIVE_INFINITY));
        parameterDrivers.add(new ParameterDriver(RadiationSensitive.ABSORPTION_COEFFICIENT, alpha, SCALE, 0.0, 1.0));
        parameterDrivers.add(new ParameterDriver(RadiationSensitive.REFLECTION_COEFFICIENT, tau, SCALE, 0.0, 1.0));
        this.crossSection = crossSection;
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getRadiationParametersDrivers() {
        return Collections.unmodifiableList(parameterDrivers);
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D radiationPressureAcceleration(final SpacecraftState state, final Vector3D flux,
                                                  final double[] parameters) {
        final double alpha = parameters[1];
        final double tau   = parameters[2];
        final double kP = parameters[0] * crossSection * (1 + 4 * (1.0 - alpha) * (1.0 - tau) / 9.0);
        return new Vector3D(kP / state.getMass(), flux);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldVector3D<T>
        radiationPressureAcceleration(final FieldSpacecraftState<T> state,
                                      final FieldVector3D<T> flux,
                                      final T[] parameters) {
        final T alpha = parameters[1];
        final T tau   = parameters[2];
        final T kP    = alpha.negate().add(1).multiply(tau.negate().add(1)).multiply(4.0 / 9.0).add(1).
                        multiply(parameters[0]).multiply(crossSection);
        return new FieldVector3D<>(state.getMass().reciprocal().multiply(kP), flux);
    }
}
