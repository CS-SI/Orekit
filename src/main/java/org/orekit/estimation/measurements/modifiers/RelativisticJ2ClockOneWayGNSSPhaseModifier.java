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

package org.orekit.estimation.measurements.modifiers;

import java.util.Collections;
import java.util.List;

import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.gnss.OneWayGNSSPhase;
import org.orekit.utils.Constants;
import org.orekit.utils.ParameterDriver;

/**
 * Class modifying theoretical one-way phase measurements with relativistic J2 clock correction.
 * <p>
 * Relativistic clock correction of the effects caused by the oblateness of Earth on
 * the gravity potential.
 * </p>
 * <p>
 * The time delay caused by this effect is computed based on the orbital parameters of the
 * emitter's orbit.
 * </p>
 *
 * @author Louis Aucouturier
 * @since 11.2
 *
 * @see "Teunissen, Peter, and Oliver Montenbruck, eds. Springer handbook of global navigation
 * satellite systems. Chapter 19.2. Equation 19.18 Springer, 2017."
 */

public class RelativisticJ2ClockOneWayGNSSPhaseModifier extends AbstractRelativisticJ2ClockModifier implements EstimationModifier<OneWayGNSSPhase> {

    /**
     * Modifier constructor.
     * @param gm Earth gravitational constant (mu) in m³/s².
     * @param c20 Earth un-normalized second zonal coefficient (Signed J2 constant, is negative) (Typical value -1.0826e-3).
     * @param equatorialRadius Earth equatorial radius in m.
     */
    public RelativisticJ2ClockOneWayGNSSPhaseModifier(final double gm,
                                                      final double c20,
                                                      final double equatorialRadius) {
        super(gm, c20, equatorialRadius);
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    public void modifyWithoutDerivatives(final EstimatedMeasurementBase<OneWayGNSSPhase> estimated) {
        // Relativistic clock correction
        final double dtJ2 = relativisticJ2Correction(estimated);

        // Wavelength
        final double wavelength = estimated.getObservedMeasurement().getWavelength();

        // Update estimated value taking into account the relativistic effect.
        final double   cOverLambda = Constants.SPEED_OF_LIGHT / wavelength;
        final double[] newValue = estimated.getEstimatedValue().clone();
        newValue[0] = newValue[0] - dtJ2 * cOverLambda;
        estimated.setEstimatedValue(newValue);
    }

}
