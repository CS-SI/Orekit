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

    /** Parameters scaling factor.
     * <p>
     * We use a power of 2 to avoid numeric noise introduction
     * in the multiplications/divisions sequences.
     * </p>
     */
    private final double SCALE = FastMath.scalb(1.0, -3);

    /** Drivers for radiation coefficient. */
    private final List<ParameterDriver> radiationParametersDrivers;

    /** Cross section (m²). */
    private final double crossSection;

    /** Constructor with reflection coefficient min/max set to ±∞.
     * @param crossSection Surface (m²)
     * @param cr reflection coefficient
     */
    public IsotropicRadiationSingleCoefficient(final double crossSection, final double cr) {
        this(crossSection, cr, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    /** Constructor with reflection coefficient min/max set by user.
    * @param crossSection Surface (m²)
    * @param cr reflection coefficient
    * @param crMin Minimum value of reflection coefficient
    * @param crMax Maximum value of reflection coefficient
    */
    public IsotropicRadiationSingleCoefficient(final double crossSection, final double cr,
                                               final double crMin, final double crMax) {
        // in some corner cases (unknown spacecraft, fuel leaks, active piloting ...)
        // the single coefficient may be arbitrary, and even negative
        // the REFLECTION_COEFFICIENT parameter should be sufficient, but GLOBAL_RADIATION_FACTOR
        // was added as of 12.0 for consistency with BoxAndSolarArraySpacecraft
        // that only has a global multiplicatof factor, hence allowing this name
        // to be used for both models
        this.radiationParametersDrivers = new ArrayList<>(2);
        radiationParametersDrivers.add(new ParameterDriver(RadiationSensitive.GLOBAL_RADIATION_FACTOR,
                                                           1.0, SCALE,
                                                           0.0, Double.POSITIVE_INFINITY));
        radiationParametersDrivers.add(new ParameterDriver(RadiationSensitive.REFLECTION_COEFFICIENT,
                                                           cr, SCALE,
                                                           crMin, crMax));

        this.crossSection = crossSection;

    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getRadiationParametersDrivers() {
        return Collections.unmodifiableList(radiationParametersDrivers);
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D radiationPressureAcceleration(final SpacecraftState state, final Vector3D flux,
                                                  final double[] parameters) {
        final double cr = parameters[1];
        return new Vector3D(parameters[0] * crossSection * cr / state.getMass(), flux);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldVector3D<T>
        radiationPressureAcceleration(final FieldSpacecraftState<T> state,
                                      final FieldVector3D<T> flux,
                                      final T[] parameters) {
        final T cr = parameters[1];
        return new FieldVector3D<>(state.getMass().reciprocal().multiply(parameters[0]).multiply(crossSection).multiply(cr),
                                   flux);

    }
}
