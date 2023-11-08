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
package org.orekit.estimation.measurements.gnss;

import java.util.List;

import org.orekit.files.rinex.observation.ObservationData;
import org.orekit.gnss.MeasurementType;

/**
 * Combined observation data.
 * @author Bryan Cazabonne
 * @since 10.1
 */
public class CombinedObservationData {

    /** Type of the combination of measurements. */
    private final CombinationType combinationType;

    /** Measurement type. */
    private final MeasurementType measurementType;

    /** Combined observed value. */
    private double value;

    /** Frequency of the combined observation data [MHz]. */
    private final double combinedFrequency;

    /** Observation data used to perform the combination of measurements. */
    private final List<ObservationData> usedData;

    /**
     * Constructor.
     * @param combinationType combination of measurements used to build the combined observation data
     * @param measurementType measurement type used for the combination of measurement
     * @param combinedValue combined observed value
     * (may be {@code Double.NaN} if combined observation not available)
     * @param combinedFrequency frequency of the combined observation data in MHz
     * (may be {@code Double.NaN} if combined frequency is not available)
     * @param usedData observation data used to perform the combination of measurements
     */
    public CombinedObservationData(final CombinationType combinationType, final MeasurementType measurementType,
                                   final double combinedValue, final double combinedFrequency,
                                   final List<ObservationData> usedData) {
        this.combinationType   = combinationType;
        this.measurementType   = measurementType;
        this.value             = combinedValue;
        this.combinedFrequency = combinedFrequency;
        this.usedData          = usedData;
    }

    /** Get the combined observed value.
     * @return observed value (may be {@code Double.NaN} if observation not available)
     */
    public double getValue() {
        return value;
    }

    /** Get the value of the combined frequency in MHz.
     * <p>
     * For the single frequency combinations, this method returns
     * the common frequency of both measurements.
     * </p>
     * @return value of the combined frequency in MHz
     */
    public double getCombinedMHzFrequency() {
        return combinedFrequency;
    }

    /** Get the type of the combination of measurements used to build the instance.
     * @return the combination of measurements type
     */
    public CombinationType getCombinationType() {
        return combinationType;
    }

    /** Get the measurement type.
     * @return measurement type
     */
    public MeasurementType getMeasurementType() {
        return measurementType;
    }

    /**
     * Get the list of observation data used to perform the combination of measurements.
     * @return a list of observation data
     */
    public List<ObservationData> getUsedObservationData() {
        return usedData;
    }

}
