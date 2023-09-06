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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.rinex.observation.ObservationData;
import org.orekit.files.rinex.observation.ObservationDataSet;
import org.orekit.gnss.Frequency;
import org.orekit.gnss.MeasurementType;
import org.orekit.gnss.ObservationType;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.utils.Constants;

/** Base class for dual frequency combination of measurements.
 * @author Bryan Cazabonne
 * @since 10.1
 */
public abstract class AbstractDualFrequencyCombination implements MeasurementCombination {

    /** Mega Hertz to Hertz converter. */
    public static final double MHZ_TO_HZ = 1.0e6;

    /** Type of combination of measurements. */
    private final CombinationType type;

    /** Satellite system used for the combination. */
    private final SatelliteSystem system;

    /**
     * Constructor.
     * @param type combination of measurements type
     * @param system satellite system
     */
    protected AbstractDualFrequencyCombination(final CombinationType type, final SatelliteSystem system) {
        this.type   = type;
        this.system = system;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return type.getName();
    }

    /**
     * Combines observation data using a dual frequency combination of measurements.
     * @param od1 first observation data to combined
     * @param od2 second observation data to combined
     * @return a combined observation data
     */
    public CombinedObservationData combine(final ObservationData od1, final ObservationData od2) {

        // Observation types
        final ObservationType obsType1 = od1.getObservationType();
        final ObservationType obsType2 = od2.getObservationType();

        // Frequencies
        final Frequency freq1 = obsType1.getFrequency(system);
        final Frequency freq2 = obsType2.getFrequency(system);
        // Check if the combination of measurements if performed for two different frequencies
        if (freq1 == freq2) {
            throw new OrekitException(OrekitMessages.INCOMPATIBLE_FREQUENCIES_FOR_COMBINATION_OF_MEASUREMENTS,
                                      freq1, freq2, getName());
        }

        // Measurements types
        final MeasurementType measType1 = obsType1.getMeasurementType();
        final MeasurementType measType2 = obsType2.getMeasurementType();

        // Check if measurement types are the same
        if (measType1 != measType2) {
            // If the measurement types are differents, an exception is thrown
            throw new OrekitException(OrekitMessages.INVALID_MEASUREMENT_TYPES_FOR_COMBINATION_OF_MEASUREMENTS,
                                      measType1, measType2, getName());
        }

        // Combined frequency
        final double combinedFrequency = getCombinedFrequency(freq1, freq2);

        // Combined value
        final double combinedValue;
        if (obsType1.getMeasurementType() == MeasurementType.CARRIER_PHASE && !Double.isNaN(combinedFrequency)) {
            // Transform from cycle to meters measurements
            final double obs1Meters = od1.getValue() * Constants.SPEED_OF_LIGHT / (freq1.getMHzFrequency() * MHZ_TO_HZ);
            final double obs2Meters = od2.getValue() * Constants.SPEED_OF_LIGHT / (freq2.getMHzFrequency() * MHZ_TO_HZ);

            // Calculate the combined value and convert it in cycles using the combined frequency
            combinedValue = getCombinedValue(obs1Meters, freq1, obs2Meters, freq2) * (combinedFrequency * MHZ_TO_HZ) / Constants.SPEED_OF_LIGHT;
        } else {
            combinedValue = getCombinedValue(od1.getValue(), freq1, od2.getValue(), freq2);
        }

        // Combined observation data
        return new CombinedObservationData(type, measType1, combinedValue, combinedFrequency, Arrays.asList(od1, od2));

    }

    /** {@inheritDoc} */
    @Override
    public CombinedObservationDataSet combine(final ObservationDataSet observations) {

        // Initialize list of measurements
        final List<ObservationData> pseudoRanges = new ArrayList<>();
        final List<ObservationData> phases       = new ArrayList<>();

        // Loop on observation data to fill lists
        for (final ObservationData od : observations.getObservationData()) {
            if (!Double.isNaN(od.getValue())) {
                if (od.getObservationType().getMeasurementType() == MeasurementType.PSEUDO_RANGE) {
                    pseudoRanges.add(od);
                } else if (od.getObservationType().getMeasurementType() == MeasurementType.CARRIER_PHASE) {
                    phases.add(od);
                }
            }
        }

        // Initialize list of combined observation data
        final List<CombinedObservationData> combined = new ArrayList<>();
        // Combine pseudo-ranges
        for (int i = 0; i < pseudoRanges.size() - 1; i++) {
            for (int j = 1; j < pseudoRanges.size(); j++) {
                final boolean combine = isCombinationPossible(pseudoRanges.get(i), pseudoRanges.get(j));
                if (combine) {
                    combined.add(combine(pseudoRanges.get(i), pseudoRanges.get(j)));
                }
            }
        }
        // Combine carrier-phases
        for (int i = 0; i < phases.size() - 1; i++) {
            for (int j = 1; j < phases.size(); j++) {
                final boolean combine = isCombinationPossible(phases.get(i), phases.get(j));
                if (combine) {
                    combined.add(combine(phases.get(i), phases.get(j)));
                }
            }
        }

        return new CombinedObservationDataSet(observations.getSatellite().getSystem(),
                                              observations.getSatellite().getPRN(),
                                              observations.getDate(),
                                              observations.getRcvrClkOffset(), combined);
    }

    /**
     * Get the combined observed value of two measurements.
     * @param obs1 observed value of the first measurement
     * @param f1 frequency of the first measurement
     * @param obs2 observed value of the second measurement
     * @param f2 frequency of the second measurement
     * @return combined observed value
     */
    protected abstract double getCombinedValue(double obs1, Frequency f1, double obs2, Frequency f2);

    /**
     * Get the combined frequency of two measurements.
     * @param f1 frequency of the first measurement
     * @param f2 frequency of the second measurement
     * @return combined frequency in MHz
     */
    protected abstract double getCombinedFrequency(Frequency f1, Frequency f2);

    /**
     * Verifies if two observation data can be combine.
     * @param data1 first observation data
     * @param data2 second observation data
     * @return true if observation data can be combined
     */
    private boolean isCombinationPossible(final ObservationData data1, final ObservationData data2) {
        // Observation types
        final ObservationType obsType1 = data1.getObservationType();
        final ObservationType obsType2 = data2.getObservationType();
        // Dual-frequency combination is possible only if observation code is the same and data frequencies are different
        return obsType1.getFrequency(system) != obsType2.getFrequency(system) &&
                        obsType1.getSignalCode() == obsType2.getSignalCode();
    }

}
