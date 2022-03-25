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

/**Handler to perform pseudo-range smoothing using Carrier-Phase measurements taken at a single frequency.
 *
 * Processes a list of ObservationDataSet, to produce smoothed pseudo-range measurements,
 * stored in a list of ObservationDataSetUpdate.
 *
 * @author Louis Aucouturier
 *
 */
public class PseudoRangeSingleFrequencySmoother {

    /** Window size for the hatch filter. */
    private int N;

    /** Maximum difference value between original and smoothed code value, above which
     * the filter is reset.*/
    private double threshold;

    /** Map storing the filters for each observation type. Observation types should not overlap
     * for a single RINEX file.*/
    private HashMap<ObservationType, CarrierPhaseHatchFilterSingleFrequency> mapFilters;

    /** Map storing the filtered data for each observation type of pseudo range.
     * The data is stored in the form of a list of ObservationDataSetUpdate, which itself
     * stores a pseudo-range ObservationData object with the filtered value, and the initial ObservationDataSet,
     * needed for further processing. */
    private HashMap<ObservationType, List<ObservationDataSetUpdate>> mapFilteredData;


    /**
     * Simple constructor.
     * @param N : Window size for the hatch filter as an integer.
     */
    public PseudoRangeSingleFrequencySmoother(final int N) {
        this(N, 100);
    }

    /**
     * Simple constructor.
     * @param N : Window size for the hatch filter as an integer.
     * @param threshold
     */
    public PseudoRangeSingleFrequencySmoother(final int N, final double threshold) {
        this.mapFilteredData = new HashMap<ObservationType, List<ObservationDataSetUpdate>>();
        this.mapFilters = new HashMap<ObservationType, CarrierPhaseHatchFilterSingleFrequency>();
        this.N = N;
        this.threshold = threshold;
    }

    /**
     * Method used to create a Hatch filter given initial data.
     *
     * @param codeData : pseudo-range ObservationData object to initialize the filter.
     * @param phaseDataF1 : phase ObservationData object for the first selected frequency
     * @param satSystem : SatelliteSystem object.
     * @param N_input : Window size for the Hatch filter as an integer.
     * @return Corresponding CarrierPhaseHatchFilterDualFrequency object.
     */
    public CarrierPhaseHatchFilterSingleFrequency initiateCarrierPhaseSmoother(final ObservationData codeData,
            final ObservationData phaseDataF1,
            final SatelliteSystem satSystem,
            final int N_input) {
        return new CarrierPhaseHatchFilterSingleFrequency(codeData, phaseDataF1, satSystem, N_input, threshold);
    }


    /**
     * Getter to obtain map of the filtered data.
     * @return HashMap of List of ObservationDataSetUpdate, with ObservationType as key.
     */
    public HashMap<ObservationType, List<ObservationDataSetUpdate>> getFilteredDataMap() {
        return mapFilteredData;
    }

    /**
     * @return the mapFilters
     */
    public final HashMap<ObservationType, CarrierPhaseHatchFilterSingleFrequency> getMapFilters() {
        return mapFilters;
    }


    /**
     * Method to copy an object, as a shallow copy.
     *
     * @param obsData
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
     * @param obsTypeF1 : Phase ObservationType to be used as the first frequency for filtering.
     */
    public void filterDataSet(final List<ObservationDataSet> listODS, final SatelliteSystem satSystem, final int prnNumber,
            final ObservationType obsTypeF1) {

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

                        // Check measurement type, and if range check for a phase carrier measurement corresponding of the chosen observationType
                        final ObservationType obsTypeRange = obsData.getObservationType();
                        if (obsTypeRange.getMeasurementType() == MeasurementType.PSEUDO_RANGE) {

                            ObservationData obsDataPhaseF1 = null;

                            for (ObservationData obsDataPhase : listObsData) {

                                // Iterate to find the required carrier phases corresponding to the observation types.
                                // Then copy the observation data to store them.
                                final ObservationType obsTypePhase = obsDataPhase.getObservationType();

                                if (!Double.isNaN(obsDataPhase.getValue()) && obsTypePhase == obsTypeF1) {
                                    obsDataPhaseF1 = copyObservationData(obsDataPhase);
                                }

                            }

                            // Check if the filter exist in the filter map
                            CarrierPhaseHatchFilterSingleFrequency filterObject = mapFilters.get(obsTypeRange);

                            // If the filter does not exist and the phase object are not null, initialize a new filter and
                            // store it in the map, initialize a new list of observationDataSetUpdate, and store it in the map.
                            if (filterObject == null && obsDataPhaseF1 != null) {
                                filterObject = initiateCarrierPhaseSmoother(obsData, obsDataPhaseF1, satSystem, N);
                                mapFilters.put(obsTypeRange, filterObject);
                                final List<ObservationDataSetUpdate> odList = new ArrayList<ObservationDataSetUpdate>();
                                odList.add(new ObservationDataSetUpdate(obsData, obsSet));
                                mapFilteredData.put(obsTypeRange, odList);
                            // If filter exist, check if a phase object is null, then reset the filter at the next step,
                            // else, filter the data.
                            } else if (filterObject != null) {
                                if (obsDataPhaseF1 == null ) {
                                    filterObject.resetFilterNext(obsData.getValue());
                                } else {
                                    final ObservationData filteredRange = filterObject.filterData(obsData, obsDataPhaseF1);
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
