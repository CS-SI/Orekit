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
import java.util.List;

import org.hipparchus.util.FastMath;
import org.orekit.files.rinex.observation.ObservationData;
import org.orekit.files.rinex.observation.ObservationDataSet;
import org.orekit.gnss.MeasurementType;
import org.orekit.gnss.SatelliteSystem;

/**
 * Melbourne-Wübbena combination.
 * <p>
 * This combination allows, thanks to the wide-lane combination, a larger wavelength
 * than each signal individually. Moreover, the measurement noise is reduced by the
 * narrow-lane combination of code measurements.
 * </p>
 * <pre>
 *    mMW =  ΦWL- RNL
 *    mMW =  λWL * NWL+ b + ε
 * </pre>
 * With:
 * <ul>
 * <li>mMW : Melbourne-Wübbena measurement.</li>
 * <li>ΦWL : Wide-Lane phase measurement.</li>
 * <li>RNL : Narrow-Lane code measurement.</li>
 * <li>λWL : Wide-Lane wavelength.</li>
 * <li>NWL : Wide-Lane ambiguity (Nf1 - Nf2).</li>
 * <li>b   : Satellite and receiver instrumental delays.</li>
 * <li>ε   : Measurement noise.</li>
 * </ul>
 * <p>
 * {@link NarrowLaneCombination Narrow-Lane} and {@link WideLaneCombination Wide-Lane}
 * combinations shall be performed with the same pair of frequencies.
 * </p>
 *
 * @see "Detector based in code and carrier phase data: The Melbourne-Wübbena combination,
 *       J. Sanz Subirana, J.M. Juan Zornoza and M. Hernández-Pajares, 2011"
 *
 * @author Bryan Cazabonne
 * @since 10.1
 */
public class MelbourneWubbenaCombination implements MeasurementCombination {

    /** Threshold for frequency comparison. */
    private static final double THRESHOLD = 1.0e-10;

    /** Satellite system used for the combination. */
    private final SatelliteSystem system;

    /**
     * Package private constructor for the factory.
     * @param system satellite system for which the combination is applied
     */
    MelbourneWubbenaCombination(final SatelliteSystem system) {
        this.system = system;
    }

    /** {@inheritDoc} */
    @Override
    public CombinedObservationDataSet combine(final ObservationDataSet observations) {

        // Wide-Lane combination
        final WideLaneCombination        wideLane   = MeasurementCombinationFactory.getWideLaneCombination(system);
        final CombinedObservationDataSet combinedWL = wideLane.combine(observations);

        // Narrow-Lane combination
        final NarrowLaneCombination      narrowLane = MeasurementCombinationFactory.getNarrowLaneCombination(system);
        final CombinedObservationDataSet combinedNL = narrowLane.combine(observations);

        // Initialize list of combined observation data
        final List<CombinedObservationData> combined = new ArrayList<>();

        // Loop on Wide-Lane measurements
        for (CombinedObservationData odWL : combinedWL.getObservationData()) {
            // Only consider combined phase measurements
            if (odWL.getMeasurementType() == MeasurementType.CARRIER_PHASE) {
                // Loop on Narrow-Lane measurements
                for (CombinedObservationData odNL : combinedNL.getObservationData()) {
                    // Only consider combined range measurements
                    if (odNL.getMeasurementType() == MeasurementType.PSEUDO_RANGE) {
                        // Verify if the combinations have used the same frequencies
                        final boolean isCombinationPossible = isCombinationPossible(odWL, odNL);
                        if (isCombinationPossible) {
                            // Combined value and frequency
                            final double combinedValue     = odWL.getValue() - odNL.getValue();
                            final double combinedFrequency = odWL.getCombinedMHzFrequency();
                            // Used observation data to build the Melbourn-Wübbena measurement
                            final List<ObservationData> usedData = new ArrayList<ObservationData>(4);
                            usedData.add(0, odWL.getUsedObservationData().get(0));
                            usedData.add(1, odWL.getUsedObservationData().get(1));
                            usedData.add(2, odNL.getUsedObservationData().get(0));
                            usedData.add(3, odNL.getUsedObservationData().get(1));
                            // Update the combined observation data list
                            combined.add(new CombinedObservationData(CombinationType.MELBOURNE_WUBBENA,
                                                                     MeasurementType.COMBINED_RANGE_PHASE,
                                                                     combinedValue, combinedFrequency, usedData));
                        }
                    }
                }
            }
        }

        return new CombinedObservationDataSet(observations.getSatellite().getSystem(),
                                              observations.getSatellite().getPRN(),
                                              observations.getDate(),
                                              observations.getRcvrClkOffset(), combined);
    }

    /**
     * Verifies if the Melbourne-Wübbena combination is possible between both combined observation data.
     * <p>
     * This method compares the frequencies of the combined measurement to decide
     * if the combination of measurements is possible.
     * The combination is possible if :
     * <pre>
     *    abs(f1<sub>WL</sub> - f2<sub>WL</sub>) = abs(f1<sub>NL</sub> - f2<sub>NL</sub>)
     * </pre>
     * </p>
     * @param odWL Wide-Lane measurement
     * @param odNL Narrow-Lane measurement
     * @return true if the Melbourne-Wübbena combination is possible
     */
    private boolean isCombinationPossible(final CombinedObservationData odWL, final CombinedObservationData odNL) {
        // Frequencies
        final double[] frequency = new double[4];
        int j = 0;
        for (int i = 0; i < odWL.getUsedObservationData().size(); i++) {
            frequency[j++] = odWL.getUsedObservationData().get(i).getObservationType().getFrequency(system).getMHzFrequency();
            frequency[j++] = odNL.getUsedObservationData().get(i).getObservationType().getFrequency(system).getMHzFrequency();
        }
        // Verify if used frequencies are the same.
        // Possible numerical error is taken into account by using a threshold of acceptance
        return (FastMath.abs(frequency[0] - frequency[2]) - FastMath.abs(frequency[1] - frequency[3])) < THRESHOLD;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return CombinationType.MELBOURNE_WUBBENA.getName();
    }

}
