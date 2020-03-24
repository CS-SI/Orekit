/* Copyright 2002-2019 CS Systèmes d'Information
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
package org.orekit.estimation.measurements.filtering;

import org.hipparchus.util.FastMath;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.propagation.SpacecraftState;

public class ResidualFilter<T extends ObservedMeasurement<T>> implements MeasurementFilter<T> {

    /** Elevation threshold under which the measurement will be rejected. */
    private final double threshold;

    /**
     * Contructor.
     * @param threshold maximum value between estimated and observed value in order to the measurement to be accepted
     */
    public ResidualFilter(final double threshold) {
        this.threshold  = threshold;
    }

    @Override
    public void filter(final ObservedMeasurement<T> measurement, final SpacecraftState state) {
        final SpacecraftState[] sc              = new SpacecraftState[] {state};
        final EstimatedMeasurement<?> estimated = measurement.estimate(0, 0, sc);
        final double[] observedValue            = measurement.getObservedValue();
        final double[] estimatedValue           = estimated.getEstimatedValue();
        final double[] sigma                    = measurement.getTheoreticalStandardDeviation();
        for (int i = 0; i < observedValue.length; i++) {
            if (FastMath.abs(observedValue[i] - estimatedValue[i]) > threshold * sigma[i]) {
                measurement.setEnabled(false);
            }
        }
    }
}
