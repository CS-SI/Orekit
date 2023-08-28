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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.orekit.files.rinex.observation.ObservationData;
import org.orekit.files.rinex.observation.ObservationDataSet;
import org.orekit.gnss.MeasurementType;
import org.orekit.gnss.ObservationType;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.time.ChronologicalComparator;

/**
 * Handler to perform pseudo-range smoothing using Divergence-Free phase combinations.
 *
 * @author Louis Aucouturier
 * @since 11.2
 */
public class DualFrequencySmoother {

    /** Window size for the hatch filter. */
    private int N;

    /** Threshold for the difference between smoothed and measured values. */
    private double threshold;

    /**
     * Map storing the filters for each observation type.
     * Observation types should not overlap for a single RINEX file.
     */
    private HashMap<ObservationType, DualFrequencyHatchFilter> mapFilters;


    /**
     * Map storing the filtered data for each observation type of pseudo range.
     * The data is stored in the form of a list of ObservationDataSetUpdate, which itself
     * stores a pseudo-range ObservationData object with the filtered value, and the initial ObservationDataSet,
     * needed for further processing.
     */
    private HashMap<ObservationType, List<SmoothedObservationDataSet>> mapFilteredData;

    /**
     * Simple constructor.
     * @param threshold threshold for loss of lock detection
     *                  (represents the maximum difference between smoothed
     *                  and measured values for loss of lock detection)
     * @param N         window size of the Hatch Filter
     */
    public DualFrequencySmoother(final double threshold, final int N) {
        this.mapFilteredData = new HashMap<>();
        this.mapFilters      = new HashMap<>();
        this.N               = N;
        this.threshold       = threshold;
    }

    /**
     * Creates an Hatch filter given initial data.
     *
     * @param codeData    input code observation data
     * @param phaseDataF1 input phase observation data for the first frequency
     * @param phaseDataF2 input phase observation data for the second frequency
     * @param satSystem   satellite system corresponding to the observations
     * @return an Hatch filter for the input data
     */
    public DualFrequencyHatchFilter createFilter(final ObservationData codeData,
                                                 final ObservationData phaseDataF1,
                                                 final ObservationData phaseDataF2,
                                                 final SatelliteSystem satSystem) {
        // Wavelengths in meters
        final double wavelengthF1 = phaseDataF1.getObservationType().getFrequency(satSystem).getWavelength();
        final double wavelengthF2 = phaseDataF2.getObservationType().getFrequency(satSystem).getWavelength();
        // Return a Dual Frequency Hatch Filter
        return new DualFrequencyHatchFilter(codeData, phaseDataF1, phaseDataF2, wavelengthF1, wavelengthF2, threshold, N);
    }

    /**
     * Get the map of the filtered data.
     * @return a map containing the filtered data.
     */
    public HashMap<ObservationType, List<SmoothedObservationDataSet>> getFilteredDataMap() {
        return mapFilteredData;
    }

    /**
     * Get the map storing the filters for each observation type.
     * @return the map storing the filters for each observation type
     */
    public final HashMap<ObservationType, DualFrequencyHatchFilter> getMapFilters() {
        return mapFilters;
    }

    /**
     * Copy an ObservationData object.
     * @param obsData observation data to copy
     * @return a copy of the input observation data
     */
    public ObservationData copyObservationData(final ObservationData obsData) {
        return new ObservationData(obsData.getObservationType(), obsData.getValue(),
                                   obsData.getLossOfLockIndicator(), obsData.getSignalStrength());
    }

    /**
     * Applies a Dual Frequency Hatch filter to a list of {@link ObservationDataSet}.
     *
     * @param listODS input observation data sets
     * @param satSystem satellite System from which to filter the pseudo-range values
     * @param prnNumber PRN identifier to identify the satellite from which to filter the pseudo-range values
     * @param obsTypeF1 observation type to be used as the first frequency for filtering
     * @param obsTypeF2 observation type to be used as the second frequency for filtering
     */
    public void filterDataSet(final List<ObservationDataSet> listODS, final SatelliteSystem satSystem, final int prnNumber,
                              final ObservationType obsTypeF1, final ObservationType obsTypeF2) {

        // Sort the list in chronological way to ensure the filter work on time ordered data.
        final List<ObservationDataSet> sortedListODS = new ArrayList<>(listODS);
        sortedListODS.sort(new ChronologicalComparator());

        // For each data set, work on those corresping to the PRN and Satellite system.
        for (final ObservationDataSet obsSet : sortedListODS) {
            if (obsSet.getSatellite().getSystem() == satSystem && obsSet.getSatellite().getPRN() == prnNumber) {
                // Get all observation data
                final List<ObservationData> listObsData = obsSet.getObservationData();
                // For each ObservationData check if usable (SNR and !(isNaN))
                for (ObservationData obsData : listObsData) {
                    final double snr = obsData.getSignalStrength();
                    if (!Double.isNaN(obsData.getValue()) && (snr == 0 || snr >= 4)) {

                        // Check measurement type, and if range check for a phase carrier measurement of the chosen observationTypes
                        final ObservationType obsTypeRange = obsData.getObservationType();
                        if (obsTypeRange.getMeasurementType() == MeasurementType.PSEUDO_RANGE) {

                            ObservationData obsDataPhaseF1 = null;
                            ObservationData obsDataPhaseF2 = null;

                            for (ObservationData obsDataPhase : listObsData) {

                                // Iterate to find the required carrier phases corresponding to the observation types.
                                // Then copy the observation data to store them.
                                final ObservationType obsTypePhase = obsDataPhase.getObservationType();

                                if (!Double.isNaN(obsDataPhase.getValue()) && obsTypePhase == obsTypeF1) {
                                    obsDataPhaseF1 = copyObservationData(obsDataPhase);
                                }

                                if (!Double.isNaN(obsDataPhase.getValue()) && obsTypePhase == obsTypeF2) {
                                    obsDataPhaseF2 = copyObservationData(obsDataPhase);
                                }
                            }

                            // Check if the filter exist in the filter map
                            DualFrequencyHatchFilter filterObject = mapFilters.get(obsTypeRange);

                            // If the filter does not exist and the phase object are not null, initialize a new filter and
                            // store it in the map, initialize a new list of observationDataSetUpdate, and store it in the map.
                            if (filterObject == null && obsDataPhaseF1 != null && obsDataPhaseF2 != null) {
                                filterObject = createFilter(obsData, obsDataPhaseF1, obsDataPhaseF2, satSystem);
                                mapFilters.put(obsTypeRange, filterObject);
                                final List<SmoothedObservationDataSet> odList = new ArrayList<SmoothedObservationDataSet>();
                                odList.add(new SmoothedObservationDataSet(obsData, obsSet));
                                mapFilteredData.put(obsTypeRange, odList);
                            // If filter exist, check if a phase object is null, then reset the filter at the next step,
                            // else, filter the data.
                            } else if (filterObject != null) {
                                if (obsDataPhaseF1 == null || obsDataPhaseF2 == null) {
                                    filterObject.resetFilterNext(obsData.getValue());
                                } else {
                                    final ObservationData filteredRange = filterObject.filterData(obsData, obsDataPhaseF1, obsDataPhaseF2);
                                    mapFilteredData.get(obsTypeRange).add(new SmoothedObservationDataSet(filteredRange, obsSet));
                                }
                            } else {
                                // IF the filter does not exist and one of the phase is equal to NaN or absent
                                // just skip to the next ObservationDataSet.
                            }


                        }
                    }
                }
            }

        }
    }
}

