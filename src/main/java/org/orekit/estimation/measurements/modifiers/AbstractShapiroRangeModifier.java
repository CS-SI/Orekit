/* Copyright 2022-2026 Romain Serra
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
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Class modifying theoretical range measurement with Shapiro time delay.
 * <p>
 * Shapiro time delay is a relativistic effect due to gravity.
 * </p>
 * @author Romain Serra
 * @since 14.0
 * @param <T> type of the measurements
 */
public abstract class AbstractShapiroRangeModifier<T extends ObservedMeasurement<T>> extends AbstractShapiroBaseModifier<T> {

    /**
     * Simple constructor.
     *
     * @param shapiroModel Shapiro delay computer
     */
    protected AbstractShapiroRangeModifier(final ShapiroModel shapiroModel) {
        super(shapiroModel);
    }

    /** {@inheritDoc} */
    @Override
    public void modifyWithoutDerivatives(final EstimatedMeasurementBase<T> estimated) {
        // compute correction, for one way or two way measurements
        final TimeStampedPVCoordinates[] pv = estimated.getParticipants();
        final double commonPart = shapiroCorrection(pv[0].getPosition(), pv[1].getPosition());
        final double correction = pv.length < 3 ? commonPart :
                0.5 * (commonPart + shapiroCorrection(pv[1].getPosition(), pv[2].getPosition()));

        // update estimated value taking into account the Shapiro time delay.
        final double[] newValue = estimated.getEstimatedValue().clone();
        newValue[0] = newValue[0] + correction;
        estimated.modifyEstimatedValue(this, newValue);

    }

}
