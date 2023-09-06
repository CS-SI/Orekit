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
package org.orekit.estimation.measurements.filtering;

import org.hipparchus.util.FastMath;
import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.propagation.SpacecraftState;

/**
 * Residual pre-processing filter.
 * <p>
 * The measurement residual is defined by the difference between
 * the observed value and the estimated value of the measurement.
 * </p>
 * @param <T> the type of the measurement
 * @author Bryan Cazabonne
 * @author David Soulard
 * @since 10.2
 */
public class ResidualFilter<T extends ObservedMeasurement<T>> implements MeasurementFilter<T> {

    /** Threshold over which the measurement will be rejected. */
    private final double threshold;

    /**
     * Constructor.
     * @param threshold maximum value for the measurement residual
     */
    public ResidualFilter(final double threshold) {
        this.threshold  = threshold;
    }

    /** {@inheritDoc} */
    @Override
    public void filter(final ObservedMeasurement<T> measurement, final SpacecraftState state) {

        // Computation of the estimated value of the measurement
        final SpacecraftState[]           sc             = new SpacecraftState[] {state};
        final EstimatedMeasurementBase<T> estimated      = measurement.estimateWithoutDerivatives(0, 0, sc);
        final double[]                    estimatedValue = estimated.getEstimatedValue();

        // Observed parameters (i.e. value and standard deviation)
        final double[] observedValue = measurement.getObservedValue();
        final double[] sigma         = measurement.getTheoreticalStandardDeviation();

        // Check if observed value is not too far from estimation
        for (int i = 0; i < observedValue.length; i++) {
            if (FastMath.abs(observedValue[i] - estimatedValue[i]) > threshold * sigma[i]) {
                // Observed value is too far, measurement is disabled
                measurement.setEnabled(false);
            }
        }

    }

}
