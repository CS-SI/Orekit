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

import java.util.HashMap;
import java.util.Map;

import org.hipparchus.util.FastMath;
import org.orekit.files.rinex.observation.ObservationData;
import org.orekit.gnss.MeasurementType;

/**
 * Single frequency Hatch filter.
 * <p>
 * The single frequency Hatch Filter is used to smooth the
 * pseudo-range measurement using either a Doppler measurement
 * or a carrier phase measurement.
 * </p>
 * @see "Subirana, J. S., Hernandez-Pajares, M., and José Miguel Juan Zornoza. (2013).
 *       GNSS Data Processing: Fundamentals and Algorithms. European Space Agency."
 *
 * @see "Zhou, Z., and Li, B. (2017). Optimal Doppler-aided smoothing strategy for GNSS navigation.
 *       GPS solutions, 21(1), 197-210."
 *
 * @author Louis Aucouturier
 * @since 11.2
 */
public class SingleFrequencyHatchFilter extends HatchFilter {

    /** Interval time between two measurements. */
    private final double integrationTime;

    /** Wavelength of the Doppler measurements. */
    private final double wavelength;

    /** Type of the smoothing measurement. */
    private final SmoothingMeasurement smoothing;

    /**
     * Constructor for the Single Frequency Hatch Filter.
     * <p>
     * The threshold parameter corresponds to the maximum difference between
     * non-smoothed and smoothed pseudo range value, above which the filter
     * is reset.
     * </p>
     * @param initCode        initial code measurement
     * @param initSmoothing   initial smoothing measurement
     * @param type            type of the smoothing measurement (CARRIER_PHASE or DOPPLER)
     * @param wavelength      measurement value wavelength (m)
     * @param threshold       threshold for loss of lock detection
     *                        (represents the maximum difference between smoothed
     *                        and measured values for loss of lock detection)
     * @param N               window size of the Hatch Filter
     * @param integrationTime time interval between two measurements (s)
     */
    public SingleFrequencyHatchFilter(final ObservationData initCode, final ObservationData initSmoothing,
                                      final MeasurementType type, final double wavelength,
                                      final double threshold, final int N,
                                      final double integrationTime) {
        super(threshold, N);
        this.wavelength      = wavelength;
        this.integrationTime = integrationTime;
        this.smoothing       = SmoothingMeasurement.getSmoothingMeasurement(type);
        updatePreviousSmoothedCode(initCode.getValue());
        updatePreviousSmoothingValue(smoothing.getSmoothingValue(wavelength, integrationTime, initSmoothing.getValue()));
        addToSmoothedCodeHistory(initCode.getValue());
        addToCodeHistory(initCode.getValue());
    }

    /**
     * This method filters the provided data given the state of the filter.
     * @param codeData      input code observation data
     * @param smoothingData input smoothing observation data
     * @return the smoothed observation data
     * */
    public ObservationData filterData(final ObservationData codeData, final ObservationData smoothingData) {

        // Current code value
        final double code = codeData.getValue();
        addToCodeHistory(code);

        // Current smoothing value
        // smoothed_code = w * code + (1 - w) * (previous_smoothed_code + (smoothing_value - previous_smoothing_value))
        final double smoothingValue = smoothing.getSmoothingValue(wavelength, integrationTime, smoothingData.getValue());

        // Check for carrier phase cycle slip
        final boolean cycleSlip = FastMath.floorMod(smoothingData.getLossOfLockIndicator(), 2) != 0;

        // Computes the smoothed code value and store the smoothing
        // value for the next iteration (will be used as "previous_smoothing_value")
        double smoothedValue = smoothedCode(code, smoothingValue);
        updatePreviousSmoothingValue(smoothingValue * smoothing.getSign());

        // Check the smoothed value for loss or lock or filter resetting
        smoothedValue = checkValidData(code, smoothedValue, cycleSlip);
        addToSmoothedCodeHistory(smoothedValue);
        updatePreviousSmoothedCode(smoothedValue);

        // Return the smoothed observed data
        return new ObservationData(codeData.getObservationType(), smoothedValue,
                                   codeData.getLossOfLockIndicator(), codeData.getSignalStrength());

    }

    /** Smoothing measurement. */
    private enum SmoothingMeasurement {

        /** Carrier-phase measurement. */
        CARRIER_PHASE(MeasurementType.CARRIER_PHASE) {

            /** {@inheritDoc} */
            @Override
            public double getSmoothingValue(final double lambda, final double time, final double value) {
                return lambda * value;
            }

            /** {@inheritDoc} */
            @Override
            public int getSign() {
                return 1;
            }

        },

        /** Doppler measurement. */
        DOPPLER(MeasurementType.DOPPLER) {

            /** {@inheritDoc} */
            @Override
            public double getSmoothingValue(final  double lambda, final  double time, final double value) {
                return 0.5 * time * lambda * value;
            }

            /** {@inheritDoc} */
            @Override
            public int getSign() {
                return -1;
            }

        };

        /** Parsing map. */
        private static final Map<MeasurementType, SmoothingMeasurement> KEYS_MAP = new HashMap<>();
        static {
            for (final SmoothingMeasurement measurement : values()) {
                KEYS_MAP.put(measurement.getType(), measurement);
            }
        }

        /** Measurement type. */
        private final MeasurementType type;

        /**
         * Constructor.
         * @param type measurement type
         */
        SmoothingMeasurement(final MeasurementType type) {
            this.type = type;
        }

        /**
         * Get the smoothing value of the measurement.
         * @param lambda measurement wavelength (m)
         * @param time time interval between two measurements (s)
         * @param value measurement value (not the smoothing value!)
         * @return the smoothing value used to smooth the pseudo-range measurement
         */
        public abstract double getSmoothingValue(double lambda, double time, double value);

        /**
         * Get the sign for the "previous_smoothing_value".
         * @return the sign for the "previous_smoothing_value"
         */
        public abstract int getSign();

        /**
         * Get the smoothing measurement corresponding to the input type.
         * @param type measurment type
         * @return the corresponding smoothing measurement
         */
        public static SmoothingMeasurement getSmoothingMeasurement(final MeasurementType type) {
            return KEYS_MAP.get(type);
        }

        /**
         * Get the measurement type.
         * @return the measurement type
         */
        public MeasurementType getType() {
            return type;
        }

    }

}
