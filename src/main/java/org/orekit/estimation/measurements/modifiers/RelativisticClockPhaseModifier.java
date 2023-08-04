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
import org.orekit.estimation.measurements.gnss.Phase;
import org.orekit.utils.Constants;
import org.orekit.utils.ParameterDriver;

/** Class modifying theoretical phase measurement with relativistic clock correction.
 * <p>
 * Relativistic clock correction is caused by the motion of the satellite as well as
 * the change in the gravitational potential
 * </p>
 * @author Bryan Cazabonne
 * @since 10.3
 *
 * @see "Teunissen, Peter, and Oliver Montenbruck, eds. Springer handbook of global navigation
 * satellite systems. Chapter 19.2. Springer, 2017."
 */
public class RelativisticClockPhaseModifier extends AbstractRelativisticClockModifier implements EstimationModifier<Phase> {

    /** Simple constructor. */
    public RelativisticClockPhaseModifier() {
        super();
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    public void modifyWithoutDerivatives(final EstimatedMeasurementBase<Phase> estimated) {
        // Relativistic clock correction
        final double dtRel = relativisticCorrection(estimated);

        // Wavelength
        final double wavelength = estimated.getObservedMeasurement().getWavelength();

        // Update estimated value taking into account the relativistic effect.
        final double   cOverLambda = Constants.SPEED_OF_LIGHT / wavelength;
        final double[] newValue = estimated.getEstimatedValue().clone();
        newValue[0] = newValue[0] - dtRel * cOverLambda;
        estimated.setEstimatedValue(newValue);
    }

}
