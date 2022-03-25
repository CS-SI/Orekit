/* Copyright 2002-2022 CS GROUP
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

import org.orekit.gnss.MeasurementType;
import org.orekit.gnss.ObservationData;
import org.orekit.gnss.ObservationDataSet;
import org.orekit.gnss.ObservationType;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.time.ChronologicalComparator;

/**
 *
 * @author Louis Aucouturier
 *
 */
public class PseudoRangeDopplerSmoother {

    /** Window size for the hatch filter. */
    private int N;

    /** Interval time between two measurements.*/
    private double integrationTime;

    /** Maximum difference value between original and smoothed code value, above which
     * the filter is reset.
     */
    private double threshold;

    /** Map storing the filters for each observation type. Observation types should not overlap
     * for a single RINEX file.*/
    private HashMap<ObservationType, DopplerHatchFilter> mapFilters;

    /** Map storing the filtered data for each observation type of pseudo range.
     * The data is stored in the form of a list of ObservationDataSetUpdate, which itself
     * stores a pseudo-range ObservationData object with the filtered value, and the initial ObservationDataSet,
     * needed for further processing. */
    private HashMap<ObservationType, List<ObservationDataSetUpdate>> mapFilteredData;


    /**
     * Simple constructor.
     * @param integrationTime
     * @param N : Window size for the hatch filter as an integer.
     */
    public PseudoRangeDopplerSmoother(final double integrationTime, final int N) {
        this(integrationTime, N, 100);
    }

    /**
     * Simple constructor.
     *
     * @param integrationTime
     * @param N : Window size for the hatch filter as an integer.
     * @param threshold
     */
    public PseudoRangeDopplerSmoother(final double integrationTime, final int N, final double threshold) {
        this.mapFilteredData = new HashMap<ObservationType, List<ObservationDataSetUpdate>>();
        this.mapFilters = new HashMap<ObservationType, DopplerHatchFilter>();
        this.N = N;
        this.integrationTime = integrationTime;
        this.threshold = threshold;
    }

    /**
     * Method used to create a Hatch filter given initial data.
     *
     * @param codeData : pseudo-range ObservationData object to initialize the filter.
     * @param dopplerData : phase ObservationData object for the first selected frequency
     * @param satSystem : SatelliteSystem object.
     * @param integrationTime_input
     * @param N_input : Window size for the Hatch filter as an integer.
     * @return Corresponding CarrierPhaseHatchFilterDualFrequency object.
     */
    public DopplerHatchFilter initiateDopplerSmoother(final ObservationData codeData,
            final ObservationData dopplerData,
            final SatelliteSystem satSystem,
            final double integrationTime_input,
            final int N_input) {
        return new DopplerHatchFilter(codeData, dopplerData, satSystem, integrationTime_input, N_input, threshold);
    }


    /**
     * Getter to obtain map of the filtered data.
     * @return HashMap of List of ObservationDataSetUpdate, with ObservationType as key.
     */
    public HashMap<ObservationType, List<ObservationDataSetUpdate>> getFilteredDataMap() {
        return mapFilteredData;
    }

    /**
     * Method to copy an ObservationData object.
     *
     * @param obsData : ObservationData to copy
     * @return Copy of obsData.
     */
    public ObservationData copyObservationData(final ObservationData obsData) {
        return new ObservationData(obsData.getObservationType(),
                obsData.getValue(),
                obsData.getLossOfLockIndicator(),
                obsData.getSignalStrength());
    }

    /**
     * Method used to apply a Divergence-Free Hatch filter to a list of ObservationDataSet.
     *
     * @param listODS : List of ObservationDataSet
     * @param satSystem : Satellite System from which to filter the pseudo-range values.
     * @param prnNumber : PRN identifier to identify the satellite from which to filter the pseudo-range values.
     * @param obsTypeDoppler : Phase ObservationType to be used as the first frequency for filtering.
     */
    public void filterDataSet(final List<ObservationDataSet> listODS, final SatelliteSystem satSystem, final int prnNumber,
            final ObservationType obsTypeDoppler) {

        // Sort the list in chronological way to ensure the filter work on time ordered data.
        final List<ObservationDataSet> sortedListODS = new ArrayList<>(listODS);
        sortedListODS.sort(new ChronologicalComparator());

        // For each data set, work on those corresponding to the PRN and Satellite system.
        for (ObservationDataSet obsSet : sortedListODS) {
            if (obsSet.getSatelliteSystem() == satSystem    &&
                    obsSet.getPrnNumber() == prnNumber) {
                // Get all observation data
                final List<ObservationData> listObsData = obsSet.getObservationData();
                // For each ObservationData check if usable (SNR and !(isNaN))
                for (ObservationData obsData : listObsData) {
                    final double snr = obsData.getSignalStrength();
                    if (!Double.isNaN(obsData.getValue()) && (snr == 0 || snr >= 4)) {

                        // Check measurement type, and if range check for a phase carrier measurement at the same frequency
                        final ObservationType obsTypeRange = obsData.getObservationType();
                        if (obsTypeRange.getMeasurementType() == MeasurementType.PSEUDO_RANGE) {

                            ObservationData obsDataDoppler = null;

                            for (ObservationData obsDataDopplerCurr : listObsData) {

                                // Iterate to find the required carrier phases corresponding to the observation types.
                                // Then copy the observation data to store them.
                                final ObservationType obsTypePhase = obsDataDopplerCurr.getObservationType();

                                if (!Double.isNaN(obsDataDopplerCurr.getValue()) && obsTypePhase == obsTypeDoppler) {
                                    obsDataDoppler = copyObservationData(obsDataDopplerCurr);
                                }

                            }

                            // Check if the filter exist in the filter map
                            DopplerHatchFilter filterObject = mapFilters.get(obsTypeRange);

                            // If the filter does not exist and the phase object are not null, initialize a new filter and
                            // store it in the map, initialize a new list of observationDataSetUpdate, and store it in the map.
                            if (filterObject == null && obsDataDoppler != null) {
                                filterObject = initiateDopplerSmoother(obsData, obsDataDoppler, satSystem, integrationTime, N);
                                mapFilters.put(obsTypeRange, filterObject);
                                final List<ObservationDataSetUpdate> odList = new ArrayList<ObservationDataSetUpdate>();
                                odList.add(new ObservationDataSetUpdate(obsData, obsSet));
                                mapFilteredData.put(obsTypeRange, odList);
                            // If filter exist, check if a phase object is null, then reset the filter at the next step,
                            // else, filter the data.
                            } else if (filterObject != null) {
                                if (obsDataDoppler == null ) {
                                    filterObject.resetFilterNext(obsData.getValue());
                                } else {
                                    final ObservationData filteredRange = filterObject.filterData(obsData, obsDataDoppler);
                                    mapFilteredData.get(obsTypeRange).add(new ObservationDataSetUpdate(filteredRange, obsSet));
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

