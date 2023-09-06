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
 * Handler to perform pseudo-range smoothing using single frequency measurements.
 *
 * @author Louis Aucouturier
 * @since 11.2
 */
public class SingleFrequencySmoother {

    /** Window size for the hatch filter. */
    private int N;

    /** Interval time between two measurements.*/
    private double integrationTime;

    /** Threshold for the difference between smoothed and measured values. */
    private double threshold;

    /** Type of the smoothing measurements. */
    private MeasurementType type;

    /**
     * Map storing the filters for each observation type.
     * Observation types should not overlap for a single RINEX file.
     */
    private HashMap<ObservationType, SingleFrequencyHatchFilter> mapFilters;

    /**
     * Map storing the filtered data for each pseudo range.
     * The data is stored in the form of a list of ObservationDataSetUpdate, which itself
     * stores a pseudo-range ObservationData object with the filtered value, and the initial ObservationDataSet,
     * needed for further processing.
     */
    private HashMap<ObservationType, List<SmoothedObservationDataSet>> mapFilteredData;

    /**
     * Simple constructor.
     * @param type            type of the smoothing measurements
     * @param threshold       threshold for loss of lock detection
     *                        (represents the maximum difference between smoothed
     *                        and measured values for loss of lock detection)
     * @param N               window size of the Hatch Filter
     * @param integrationTime time interval between two measurements (s)
     */
    public SingleFrequencySmoother(final MeasurementType type,
                                   final double threshold, final int N,
                                   final double integrationTime) {
        this.mapFilteredData = new HashMap<>();
        this.mapFilters      = new HashMap<>();
        this.type            = type;
        this.N               = N;
        this.integrationTime = integrationTime;
        this.threshold       = threshold;
    }

    /**
     * Creates an Hatch filter given initial data.
     * @param codeData      input code observation data
     * @param smoothingData input smoothing observation data
     * @param system        satellite system corresponding to the observations
     * @return an Hatch filter for the input data
     */
    public SingleFrequencyHatchFilter createFilter(final ObservationData codeData,
                                                   final ObservationData smoothingData,
                                                   final SatelliteSystem system) {
        // Wavelength in meters
        final double wavelength = smoothingData.getObservationType().getFrequency(system).getWavelength();
        // Return a Single Frequency Hatch Filter
        return new SingleFrequencyHatchFilter(codeData, smoothingData, type, wavelength, threshold, N, integrationTime);
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
    public final HashMap<ObservationType, SingleFrequencyHatchFilter> getMapFilters() {
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
     * Applies a Single Frequency Hatch filter to a list of {@link ObservationDataSet}.
     * @param listODS   input observation data sets
     * @param satSystem satellite System from which to filter the pseudo-range values
     * @param prnNumber PRN identifier to identify the satellite from which to filter the pseudo-range values
     * @param obsType   observation type to use for filtering
     */
    public void filterDataSet(final List<ObservationDataSet> listODS, final SatelliteSystem satSystem,
                              final int prnNumber, final ObservationType obsType) {

        // Sort the list in chronological way to ensure the filter work on time ordered data.
        final List<ObservationDataSet> sortedListODS = new ArrayList<>(listODS);
        sortedListODS.sort(new ChronologicalComparator());

        // For each data set, work on those corresponding to the PRN and Satellite system.
        for (ObservationDataSet obsSet : sortedListODS) {
            if (obsSet.getSatellite().getSystem() == satSystem  && obsSet.getSatellite().getPRN() == prnNumber) {
                // Get all observation data
                final List<ObservationData> listObsData = obsSet.getObservationData();
                // For each ObservationData check if usable (SNR and !(isNaN))
                for (final ObservationData obsData : listObsData) {
                    final double snr = obsData.getSignalStrength();
                    if (!Double.isNaN(obsData.getValue()) && (snr == 0 || snr >= 4)) {

                        // Check measurement type, and if range check for the chosen smooting measurement
                        final ObservationType obsTypeData = obsData.getObservationType();
                        if (obsTypeData.getMeasurementType() == MeasurementType.PSEUDO_RANGE) {

                            ObservationData obsDataSmoothing = null;

                            for (final ObservationData obsDataSmoothingCurr : listObsData) {

                                // Iterate to find the required smoothing measurement corresponding to the observationType.
                                // Then copy the observation data to store them.
                                final ObservationType obsTypeSmoothingCurr = obsDataSmoothingCurr.getObservationType();

                                if (!Double.isNaN(obsDataSmoothingCurr.getValue()) && obsTypeSmoothingCurr == obsType) {
                                    obsDataSmoothing = copyObservationData(obsDataSmoothingCurr);
                                }

                            }

                            // Check if the filter exist in the filter map
                            SingleFrequencyHatchFilter filterObject = mapFilters.get(obsTypeData);

                            // If the filter does not exist and the phase object are not null, initialize a new filter and
                            // store it in the map, initialize a new list of observationDataSetUpdate, and store it in the map.
                            if (filterObject == null && obsDataSmoothing != null) {
                                filterObject = createFilter(obsData, obsDataSmoothing, satSystem);
                                mapFilters.put(obsTypeData, filterObject);
                                final List<SmoothedObservationDataSet> odList = new ArrayList<>();
                                odList.add(new SmoothedObservationDataSet(obsData, obsSet));
                                mapFilteredData.put(obsTypeData, odList);
                            // If filter exist, check if a phase object is null, then reset the filter at the next step,
                            // else, filter the data.
                            } else if (filterObject != null) {
                                if (obsDataSmoothing == null ) {
                                    filterObject.resetFilterNext(obsData.getValue());
                                } else {
                                    final ObservationData filteredRange = filterObject.filterData(obsData, obsDataSmoothing);
                                    mapFilteredData.get(obsTypeData).add(new SmoothedObservationDataSet(filteredRange, obsSet));
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
