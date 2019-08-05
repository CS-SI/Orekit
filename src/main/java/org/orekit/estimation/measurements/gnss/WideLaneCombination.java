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
package org.orekit.estimation.measurements.gnss;

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.gnss.CombinedObservationData;
import org.orekit.gnss.CombinedObservationDataSet;
import org.orekit.gnss.Frequency;
import org.orekit.gnss.MeasurementType;
import org.orekit.gnss.ObservationData;
import org.orekit.gnss.ObservationDataSet;
import org.orekit.gnss.ObservationType;
import org.orekit.gnss.SatelliteSystem;

/**
 * Wide-Lane combination.
 * <p>
 * This combination are used to create a signal
 * with a significantly wide wavelength.
 * This longer wavelength is useful for cycle-slips
 * detection and ambiguity fixing
 * </p>
 * <pre>
 *              f1 * m1 - f2 * m2
 *    mWL =  -----------------------
 *                   f1 - f2
 * </pre>
 * With:
 * <ul>
 * <li>mWL: Wide-laning measurement.</li>
 * <li>f1 : Frequency of the first measurement.</li>
 * <li>m1 : First measurement.</li>
 * <li>f2 : Frequency of the second measurement.</li>
 * <li>m1 : Second measurement.</li>
 * </ul>
 * <p>
 * Wide-Lane combination is a dual frequency combination.
 * The two measurements shall have different frequencies but they must have the same {@link MeasurementType}.
 * </p>
 * @author Bryan Cazabonne
 * @since 10.1
 */
public class WideLaneCombination implements DualFrequencyMeasurementCombination {

    /** Satellite system for wich the combination is applied. */
    private final SatelliteSystem system;

    /**
     * Package private constructor for the factory.
     * @param system satellite system for wich the combination is applied
     */
    WideLaneCombination(final SatelliteSystem system) {
        this.system = system;
    }

    /** {@inheritDoc} */
    @Override
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

        // Measurement values
        final double value1 = od1.getValue();
        final double value2 = od2.getValue();
        // Frequency values
        final double f1   = freq1.getMHzFrequency();
        final double f2   = freq2.getMHzFrequency();
        // Wide-Lane combination
        final double valueWL = MathArrays.linearCombination(f1, value1, -f2, value2) / (f1 - f2);
        final double freqWL  = FastMath.abs(f1 - f2);

        //Combined observation data
        return new CombinedObservationData(CombinationType.WIDE_LANE, measType1, valueWL, freqWL);
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
                if (i != j) {
                    combined.add(combine(pseudoRanges.get(i), pseudoRanges.get(j)));
                }
            }
        }
        // Combine carrier-phases
        for (int i = 0; i < phases.size() - 1; i++) {
            for (int j = 1; j < phases.size(); j++) {
                if (i != j) {
                    combined.add(combine(phases.get(i), phases.get(j)));
                }
            }
        }

        return new CombinedObservationDataSet(observations.getHeader(), observations.getSatelliteSystem(),
                                              observations.getPrnNumber(), observations.getDate(),
                                              observations.getRcvrClkOffset(), combined);
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return CombinationType.WIDE_LANE.getName();
    }

}
