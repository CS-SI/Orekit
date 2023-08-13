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
import org.orekit.estimation.measurements.RangeRate;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.Constants;
import org.orekit.utils.ParameterDriver;

/** Class modifying theoretical range-rate measurement with relativistic frequency deviation.
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
public class RelativisticClockRangeRateModifier extends AbstractRelativisticClockModifier implements EstimationModifier<RangeRate> {

    /** Gravitational constant. */
    private final double gm;

    /** Simple constructor.
     * @param gm gravitational constant for main body in signal path vicinity.
     */
    public RelativisticClockRangeRateModifier(final double gm) {
        super();
        this.gm = gm;
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    public void modifyWithoutDerivatives(final EstimatedMeasurementBase<RangeRate> estimated) {
        // Spacecraft state
        final SpacecraftState state = estimated.getStates()[0];

        // Relativistic frequency deviation
        final double factor = -gm * getScaleFactor();
        final double dfRel = factor *
                        (reciprocal(state.getA()) - reciprocal(state.getPosition().getNorm()));

        // Update estimated value taking into account the relativistic effect.
        final double[] newValue = estimated.getEstimatedValue().clone();
        newValue[0] = newValue[0] + dfRel * Constants.SPEED_OF_LIGHT;
        estimated.setEstimatedValue(newValue);
    }

    /** Returns the inverse of the given value.
     * @param value value to inverse
     * @return the inverse of the value
     */
    private static double reciprocal(final double value) {
        return 1.0 / value;
    }

}
