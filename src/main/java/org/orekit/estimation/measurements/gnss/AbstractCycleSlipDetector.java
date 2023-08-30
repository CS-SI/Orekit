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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.orekit.files.rinex.observation.ObservationDataSet;
import org.orekit.gnss.Frequency;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.time.AbsoluteDate;

/**
 * Base class for cycle-slip detectors.
 * @author David Soulard
 * @since 10.2
 */
public abstract class AbstractCycleSlipDetector implements CycleSlipDetectors {

    /** Separator for satellite name. */
    private static final String SEPARATOR = " - ";

    /** Minimum number of measurement needed before being able to figure out cycle-slip occurrence.*/
    private int minMeasurementNumber;

    /** Maximum time lapse between two measurements without considering a cycle-slip occurred [s]. */
    private final double dt;

    /** List which contains all the info regarding the cycle slip. */
    private List<CycleSlipDetectorResults> data;

    /** List of all the things use for cycle-slip detections. */
    private List<Map<Frequency, DataForDetection>> stuff;

    /**
     * Cycle-slip detector Abstract Constructor.
     * @param dt time gap between two consecutive measurements in seconds
     *        (if time between two consecutive measurement is greater than dt, a cycle slip is declared)
     * @param n number of measures needed before starting test if a cycle-slip occurs
     */
    AbstractCycleSlipDetector(final double dt, final int n) {
        this.minMeasurementNumber = n;
        this.dt                   = dt;
        this.data                 = new ArrayList<>();
        this.stuff                = new ArrayList<>();
    }

    /** {@inheritDoc} */
    @Override
    public List<CycleSlipDetectorResults> detect(final List<ObservationDataSet> observations) {
        // Loop on observation data set
        for (ObservationDataSet observation: observations) {
            // Manage data
            manageData(observation);
        }
        // Return the results of the cycle-slip detection
        return getResults();
    }

    /**
     * The method is in charge of collecting the measurements, manage them, and call the detection method.
     * @param observation observation data set
     */
    protected abstract void manageData(ObservationDataSet observation);

    /**
     * Get the minimum number of measurement needed before being able to figure out cycle-slip occurrence.
     * @return the minimum number of measurement needed before being able to figure out cycle-slip occurrence.
     */
    protected int getMinMeasurementNumber() {
        return minMeasurementNumber;
    }

    /**
     * Get the maximum time lapse between 2 measurements without considering a cycle-slip has occurring between both.
     * @return the maximum time lapse between 2 measurements
     */
    protected double getMaxTimeBeetween2Measurement() {
        return dt;
    }

    /**
     * Get on all the results computed by the detector (e.g.: dates of cycle-slip).
     * @return  all the results computed by the detector (e.g.: dates of cycle-slip).
     */
    protected List<CycleSlipDetectorResults> getResults() {
        return data;
    }

    /**
     * Get the stuff (all the things needed for, the detector).
     * @return return stuff
     */
    protected List<Map<Frequency, DataForDetection>> getStuffReference() {
        return stuff;
    }

    /** Set the data: collect data at the current Date, at the current frequency, for a given satellite, add it within the attributes data and stuff.
     * @param nameSat name of the satellite (e.g. "GPS - 7")
     * @param date date of the measurement
     * @param value measurement at the current date
     * @param freq frequency used
     */
    protected void cycleSlipDataSet(final String nameSat, final AbsoluteDate date,
                                    final double value, final Frequency freq)  {
        // Check if cycle-slip data are empty
        if (data.isEmpty()) {
            data.add(new CycleSlipDetectorResults(nameSat, date, freq));
            final Map<Frequency, DataForDetection> newMap = new HashMap<>();
            newMap.put(freq, new DataForDetection(value, date));
            stuff.add(newMap);
        } else {
            if (!alreadyExist(nameSat, freq)) {
                // As the couple satellite-frequency, first possibility is that the satellite already exist within the data but not at this frequency
                for (CycleSlipDetectorResults r: data) {
                    if (r.getSatelliteName().compareTo(nameSat) == 0) {
                        r.addAtOtherFrequency(freq, date);
                        final Map<Frequency, DataForDetection> newMap = stuff.get(data.indexOf(r));
                        newMap.put(freq, new DataForDetection(value, date));
                        stuff.set(data.indexOf(r), newMap);
                        return;
                    }
                }
                //If w've reach this point is because the name does not exist, in this case another element in the two list should be added
                data.add(new CycleSlipDetectorResults(nameSat, date, freq));
                final Map<Frequency, DataForDetection> newMap = new HashMap<>();
                newMap.put(freq, new DataForDetection(value, date));
                stuff.add(newMap);
            } else {
                // We add the value of the combination of measurements
                addValue(nameSat, date, value, freq);
            }
        }

    }

    /**
     * Create the name of a satellite from its PRN number and satellite System it belongs to.
     * @param numSat satellite PRN number
     * @param sys Satellite System of the satellite
     * @return the satellite name on a specified format (e.g.: "GPS - 7")
     */
    protected String setName(final int numSat, final SatelliteSystem sys) {
        return sys.name() + SEPARATOR + numSat;
    }

    /**
     * Return true if the link (defined by a frequency and a satellite) has been already built.
     * @param nameSat name of the satellite (e.g.: GPS - 07 for satelite 7 of GPS constellation).
     * @param freq frequency used in the link
     * @return true if it already exists within attribute data
     */
    private boolean alreadyExist(final String nameSat, final Frequency freq) {
        if (data != null) {
            for (CycleSlipDetectorResults result: data) {
                if (result.getSatelliteName().compareTo(nameSat) == 0) {
                    return result.getCycleSlipMap().containsKey(freq);
                }
            }
        }
        return false;
    }

    /**
     * Add a value the data.
     * @param nameSat name of the satellite (satellite system - PRN)
     * @param date date of the measurement
     * @param value phase measurement minus code measurement
     * @param frequency frequency use
     */
    private void addValue(final String nameSat, final AbsoluteDate date,
                          final double value, final Frequency frequency) {
        // Loop on cycle-slip data
        for (CycleSlipDetectorResults result: data) {
            // Find the good position to add the data
            if (result.getSatelliteName().compareTo(nameSat) == 0 && result.getCycleSlipMap().containsKey(frequency)) {
                // The date is not to far away from the last one
                final Map<Frequency, DataForDetection> valuesMap = stuff.get(data.indexOf(result));
                final DataForDetection detect = valuesMap.get(frequency);
                detect.write                  = (detect.write + 1) % minMeasurementNumber;
                detect.figures[detect.write]  = new SlipComputationData(value, date);
                result.setDate(frequency, date);
                detect.canBeComputed++;
                break;
            }
        }
    }

    /**
     * Container for computed if cycle-slip occurs.
     * @author David Soulard
     */
    static class SlipComputationData {

        /** Value of the measurement. */
        private double value;

        /** Date of measurement. */
        private AbsoluteDate date;

        /**
         * Simple constructor.
         * @param value value of the measurement
         * @param date date of the measurement
         */
        SlipComputationData(final double value, final AbsoluteDate date) {
            this.value  = value;
            this.date   = date;
        }

        /**
         * Get the value of the measurement.
         * @return value of the measurement
         */
        protected double getValue() {
            return value;
        }

        /**
         * Get the date of measurement saved within this.
         * @return date of measurement saved within this
         */
        protected AbsoluteDate getDate() {
            return date;
        }
    }

    /**
     * Container for all the data need for doing cycle-slip detection.
     * @author David Soulard
     */
    class DataForDetection {

        /** Array used to compute cycle slip. */
        private SlipComputationData[] figures;

        /** Integer to make the array above circular. */
        private int write;

        /** Integer to know how many data have been added since last cycle-slip. */
        private int canBeComputed;

        /**
         * Constructor.
         * @param value measurement
         * @param date date at which measurements are taken.
         */
        DataForDetection(final double value, final AbsoluteDate date) {
            this.figures       = new SlipComputationData[minMeasurementNumber];
            this.figures[0]    = new SlipComputationData(value, date);
            this.canBeComputed = 1;
            this.write         = 0;
        }

        /**
         * Get the array of values used for computation of cycle-slip detectors.
         * @return SlipComputationDatat array
         */
        protected SlipComputationData[] getFiguresReference() {
            return figures;
        }

        /**
         * Get the reference of the counter of position into the array.
         * @return the position on which writing should occur within the circular array figures.
         */
        protected int getWrite() {
            return write;
        }

        /**
         * Get the counter on the number of measurement which have been saved up to the current date.
         * @return the number of measurement which have been saved up to the current date
         */
        protected int getCanBeComputed() {
            return canBeComputed;
        }

        /**
         * Reset this to the initial value when a cycle slip occurs.
         * The first element is already setting with a value and a date
         * @param newF new SlipComputationData[] to be used within the detector
         * @param value to be added in the first element of the array
         * @param date at which the value is given.
         */
        protected void resetFigures(final SlipComputationData[] newF, final double value, final AbsoluteDate date) {
            this.figures        = newF;
            this.figures[0]     = new SlipComputationData(value, date);
            this.write          = 0;
            this.canBeComputed  = 1;
        }

    }
}
