/* Copyright 2002-2024 Thales Alenia Space
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

import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.EstimationModifier;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.utils.Constants;
import org.orekit.utils.ParameterDriver;

import java.util.Collections;
import java.util.List;

/** Class modifying theoretical range-rate measurement with relativistic frequency deviation.
 * <p>
 * Relativistic clock correction is caused by the motion of the satellite as well as
 * the change in the gravitational potential
 * </p>
 * @param <T> type of the measurement
 * @author Luc Maisonobe
 * @since 12.1
 *
 * @see "Teunissen, Peter, and Oliver Montenbruck, eds. Springer handbook of global navigation
 * satellite systems. Chapter 19.2. Springer, 2017."
 */
public abstract class AbstractRelativisticClockOnBoardRangeRateModifier<T extends ObservedMeasurement<T>>
    extends AbstractRelativisticClockModifier implements EstimationModifier<T> {

    /** Gravitational constant. */
    private final double gm;

    /** Simple constructor.
     * @param gm gravitational constant for main body in signal path vicinity.
     */
    public AbstractRelativisticClockOnBoardRangeRateModifier(final double gm) {
        super();
        this.gm = gm;
    }

    /** {@inheritDoc} */
    @Override
    public String getEffectName() {
        return "clock relativity";
    }

    /** Get gravitational constant for main body in signal path vicinity.
     * @return gravitational constant for main body in signal path vicinity
     */
    protected double getGm() {
        return gm;
    }

    /** {@inheritDoc} */
    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.emptyList();
    }

    /** Apply relativistic frequency deviation.
     * @param estimated estimated measurement to modify
     * @param aLocal semi major axis or local (receiver) satellite
     * @param rLocal distance of local (receiver) satellite to central body center
     * @param aRemote semi major axis or remote (transmitter) satellite
     * @param rRemote distance of remote (transmitter) satellite to central body center
     */
    protected void modifyWithoutDerivatives(final EstimatedMeasurementBase<T> estimated,
                                            final double aLocal,  final double rLocal,
                                            final double aRemote, final double rRemote) {

        // compute relativistic frequency deviation
        final double factor   = -gm * getScaleFactor();
        final double dfLocal  = factor * (1.0 / aLocal  - 1.0 / rLocal);
        final double dfRemote = factor * (1.0 / aRemote - 1.0 / rRemote);

        // Update estimated value taking into account the relativistic effect.
        final double[] newValue = estimated.getEstimatedValue().clone();
        newValue[0] = newValue[0] + (dfLocal - dfRemote) * Constants.SPEED_OF_LIGHT;
        estimated.modifyEstimatedValue(this, newValue);

    }

}
