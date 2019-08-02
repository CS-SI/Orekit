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

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.gnss.CombinedObservationData;
import org.orekit.gnss.Frequency;
import org.orekit.gnss.MeasurementType;
import org.orekit.gnss.ObservationData;
import org.orekit.gnss.ObservationType;
import org.orekit.gnss.SatelliteSystem;

/**
 * Geometry-free combination.
 * <p>
 * This combination removes the geometry part of the measurement.
 * It can be used to estimate the ionospheric electron content or to detect
 * cycle slips in the carrier phase, as well.
 * </p>
 * <pre>
 *    mGF =  m2 - m1
 * </pre>
 * With:
 * <ul>
 * <li>mGF: Geometry-free measurement.</li>
 * <li>m1 : First measurement.</li>
 * <li>m1 : Second measurement.</li>
 * </ul>
 * <p>
 * Geometry-Free combination is a dual frequency combination.
 * The two measurements shall have different frequencies but they must have the same {@link MeasurementType}.
 * </p>
 * @author Bryan Cazabonne
 * @since 10.1
 */
public class GeometryFreeCombination implements DualFrequencyMeasurementCombination {

    /** Satellite system for wich the combination is applied. */
    private final SatelliteSystem system;

    /**
     * Package private constructor for the factory.
     * @param system satellite system for wich the combination is applied
     */
    GeometryFreeCombination(final SatelliteSystem system) {
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

        // Geometry-Free combination
        final double valueGF = od2.getValue() - od1.getValue();

        //Combined observation data
        return new CombinedObservationData(CombinationType.GEOMETRY_FREE, measType1, valueGF, Double.NaN);

    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return CombinationType.GEOMETRY_FREE.getName();
    }

}
