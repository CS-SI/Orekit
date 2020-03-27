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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hipparchus.fitting.PolynomialCurveFitter;
import org.hipparchus.fitting.WeightedObservedPoint;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.gnss.Frequency;
import org.orekit.gnss.MeasurementType;
import org.orekit.gnss.ObservationData;
import org.orekit.gnss.ObservationDataSet;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

public abstract class AbstractCycleSlipDetector  implements CycleSlipDetectors {

    /** Name of the station which define the detector. */
    private String station;

    /** Minimum number of measurement needed before being able to figure out cycle-slip occurrence.*/
    private int minMeasurementNumber;

    /** Maximum time lapse between two measurements without considering a cycle-slip occurred. */
    private final double dt;

    /** List which contains all the info regarding the cycle slip.*/
    private List<CycleSlipDetectorResults> data = new ArrayList<>();

    /** List of all the things use for cycle-slip detections. */
    private List<Map<Frequency, DataForDetection>> stuff = new ArrayList<>();

    /**
     * Cycle-slip detector Abstract Constructor.
     * @param obserDataSets observationDataSet list from a RINEX file
     * @param dt time gap threshold between two consecutive measurements (if time between two consecutive measurement is greater than dt, a cycle slip is declared)
     * @param threshold maximum discrepancy for the current value with respect to the predicted value above which a cycle-slip is detected
     * @param n  Number of measure needed before starting test if a cycle-slip occurs
     */
    public AbstractCycleSlipDetector(final List<ObservationDataSet> obserDataSets, final double dt, final double threshold, final int n) {
        this.minMeasurementNumber    = n;
        this.dt                 = dt;
    }

    /**
     * Get the name of the station which defined the detector.
     * @return the name of station which defined the detector
     */
    public String getStationName() {
        return station;
    }

    /**
     * Get the minimum number of measurement needed before being able to figure out cycle-slip occurrence.
     * @return the minimum number of measurement needed before being able to figure out cycle-slip occurrence.
     */
    public int getMinMeasurementNumber() {
        return minMeasurementNumber;
    }

    /**
     * Get the maximum time lapse between 2 measurements without considering a cycle-slip has occurring between both.
     * @return the maximum time lapse between 2 measurements
     */
    public double getMaxTimeBeetween2Measurement() {
        return dt;
    }

    /**Get on all the results computed by the detector (e.g.: dates of cycle-slip).
     * @return  all the results computed by the detector (e.g.: dates of cycle-slip).
     */
    public List<CycleSlipDetectorResults> getResults() {
        return data;
    }

    /**
     * Getter of the stuff (all the things needed for, the detector).
     * @return return stuff
     */
    protected List<Map<Frequency, DataForDetection>> getStuffReference() {
        return stuff;
    }

    /**
     * Set the name of the station which define the detector.
     * @param name of the station
     */
    protected void setStationName(final String name) {
        this.station = name;
    }

    /**
     * The method is in charge of collecting the measurements, manage them, and call the detection method.
     * @param obser ObservationDataSet coming from the RINEX file
     * @param threshold maximum discrepancy for the current value with respect to the predicted value above which a cycle-slip is detected
     * NOTICE: The default manageData method is the one for statistical
     */
    protected void manageData(final ObservationDataSet obser, final double threshold) {
        final int numSat = obser.getPrnNumber();
        String nameSat = "";
        final AbsoluteDate date = obser.getDate();
        final List<DataMeasure> dataPhase = new ArrayList<>();
        final List<DataMeasure> dataRange = new ArrayList<>();
        for (ObservationData od: obser.getObservationData()) {
            if (!Double.isNaN(od.getValue())) {
                //check if there is a cycle_slip at the current time
                if (od.getObservationType().getMeasurementType() == MeasurementType.CARRIER_PHASE) {
                    final Frequency f = od.getObservationType().getFrequency(obser.getSatelliteSystem());
                    final double value = od.getValue();
                    dataPhase.add(new DataMeasure(value, f));

                }
                if (od.getObservationType().getMeasurementType() == MeasurementType.PSEUDO_RANGE) {
                    final Frequency f = od.getObservationType().getFrequency(obser.getSatelliteSystem());
                    final double value = od.getValue();
                    dataRange.add(new DataMeasure(value, f));
                }
            }
        }
        for (int i = 0; i < dataPhase.size(); i++) {
            for (int j = 0; j < dataRange.size(); j++) {
                if (dataRange.get(j).getFrequency() == dataPhase.get(i).getFrequency()) {
                    //We have a both phase and code measuremnt at the same frequency.
                    final double f = dataPhase.get(i).GetFrequencyMHz();
                    final double value = (Constants.SPEED_OF_LIGHT / (f * 1e6)) * dataPhase.get(i).getValue() - dataRange.get(j).getValue();
                    nameSat = setName(numSat, obser.getSatelliteSystem());
                    final int slip = cycleSlipDetection(nameSat, date, value, dataPhase.get(i).getFrequency(),  threshold);
                    if (slip == 1) { cycleSlipDataSet(nameSat, date, value, dataPhase.get(i).getFrequency()); };
                }
            }
        }
    }

    /** Set the data: collect data at the current Date, at the current frequency, for a given satellite, add it within the attributes data and stuff.
     * @param nameSat name of the satellite (e.g.: "GPS - 7")
     * @param date date of the measurement
     * @param value measurement at the current date
     * @param freq frequency used
     */
    protected void cycleSlipDataSet(final String nameSat, final AbsoluteDate date, final double value, final Frequency freq)  {
        if (data == null) {
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
                //We add the value phi-R; the date; update the write indice
                addValue(nameSat, date, freq, value);
            }
        }

    }

    /** Compute if there is a cycle slip at a specific date.
     * @param nameSat name of the satellite, on the predefined format (e.g.: GPS - 07 for satellite 7 of GPS constellation)
     * @param currentDate the date at which we check if a cycle-slip occurs
     * @param value phase measurement minus code measurement
     * @param freq frequency used (expressed in MHz)
     * @param threshold maximum discrepancy for the current value with respect to the predicted value above which a cycle-slip is detected
     * @return 0 if a cycle slip has been detected, 1  otherwise.
     */
    protected int cycleSlipDetection(final String nameSat, final AbsoluteDate currentDate, final double value, final Frequency freq, final double threshold) {
        if (data != null) {
            for (CycleSlipDetectorResults d: data) {
                //found the right cycle data
                if (d.getSatelliteName().compareTo(nameSat) == 0 && d.getCycleSlipMap().containsKey(freq)) {
                    final Map<Frequency, DataForDetection> values = stuff.get(data.indexOf(d));
                    final DataForDetection v = values.get(freq);
                    //Check the time gap condition
                    final double deltaT = FastMath.abs(currentDate.durationFrom(v.figures[v.write].getDate()));
                    if (deltaT > dt) {
                        d.addCycleSlipDate(freq, currentDate);
                        v.figures       = new SlipComputationData[minMeasurementNumber];
                        v.figures[0]    = new SlipComputationData(value, currentDate);
                        v.canBeComputed = 1;
                        v.write         = 0;
                        d.setDate(freq, currentDate);
                        return 0;
                    }
                    //Compute the fitting polynamil if there are enough measurement since last cycle-slip
                    if (v.canBeComputed >= minMeasurementNumber) {
                        final List<WeightedObservedPoint> xy = new ArrayList<>();
                        for (int i = 0; i < minMeasurementNumber; i++) {
                            final SlipComputationData current = v.figures[i];

                            xy.add(new WeightedObservedPoint(1.0, current.getDate().durationFrom(currentDate),
                                                             current.getValue()));
                        }
                        final PolynomialCurveFitter fitting = PolynomialCurveFitter.create(2);
                        //Check if there is a cycle_slip
                        if (FastMath.abs(fitting.fit(xy)[0] - value) > threshold) {
                            d.addCycleSlipDate(freq, currentDate);
                            v.figures       = new SlipComputationData[minMeasurementNumber];
                            v.figures[0]    = new SlipComputationData(value, currentDate);
                            v.canBeComputed = 1;
                            v.write         = 0;
                            d.setDate(freq, currentDate);
                            return 0;
                        }
                    } else {
                        break;
                    }
                }
            }
        }
        return 1;
    }

    /**
     * Create the Name of a satellite from its PRN number and satellite System it belongs to.
     * @param numSat satellite PRN number
     * @param sys Satellite System of the satellite
     * @return the satellite name on a specified format (e.g.: "GPS - 7")
     */
    public String setName(final int numSat, final SatelliteSystem sys) {
        switch (sys) {
            case GPS:
                return "GPS - " + numSat;
            case GALILEO:
                return "GAL - " + numSat;
            case GLONASS:
                return "GLO - " + numSat;
            case BEIDOU:
                return "BEI - " + numSat;
            case QZSS:
                return "QZS - " + numSat;
            case IRNSS:
                return "IRN - " + numSat;
            case SBAS:
                return "SBA - " + numSat;
            case MIXED:
                return "MIX - " + numSat;
            default:
                throw new OrekitException(OrekitMessages.INVALID_SATELLITE_SYSTEM);
        }
    }

    /** Return true if the link (defined by a frequency and a satellite) have been already built.
     * @param nameSat name of the satellite (e.g.: GPS - 07 for satelite 7 of GPS constellation).
     * @param freq frequency used in the link
     * @return true if it already exist within attribute data
     */
    protected boolean alreadyExist(final String nameSat, final Frequency freq) {
        if (data != null) {
            for (CycleSlipDetectorResults result: data) {
                if (result.getSatelliteName().compareTo(nameSat) == 0) {
                    return result.getCycleSlipMap().containsKey(freq);
                }
            }
        }
        return false;
    }

    /** Add a value the data.
     * @param NameSat name of the satellite (satellite system - PRN)
     * @param date date of the measurement
     * @param frequency frequency use
     * @param value phase measurement minus code measurement
     */
    protected void addValue(final String NameSat, final AbsoluteDate date, final Frequency frequency, final double value) {
        for (CycleSlipDetectorResults result: data) {
            //find the good position to add the data
            if (result.getSatelliteName().compareTo(NameSat) == 0 && result.getCycleSlipMap().containsKey(frequency)) {
                //the date is not to far away from the last one
                final Map<Frequency, DataForDetection> valuesMap = stuff.get(data.indexOf(result));
                final DataForDetection detect = valuesMap.get(frequency);
                detect.write                        = (detect.write + 1) % minMeasurementNumber;
                detect.figures[detect.write]        = new SlipComputationData(value, date);
                result.setDate(frequency, date);
                detect.canBeComputed++;
                break;

            }
        }
    }

    /** Class containers for computed if cycle-slip occurs.
     * contains a value (phase measurement minus range measurement) and the date of measurements.
     * @author David Soulard
     *
     */
    protected class SlipComputationData {

        /**Value = phi - R where phi is the phase measurement and R the code measurement. */
        private double value;

        /**Date of measurement.*/
        private AbsoluteDate date;

        /** Simple constructor.
         * @param d phase measurement minus code measurementS
         * @param date date of the measurement
         */
        SlipComputationData(final double d, final AbsoluteDate date) {
            this.value  = d;
            this.date   = date;
        }

        /**
         * Get the measurement value saved within this.
         * @return the measurement value saved within this
         */
        protected double getValue() {
            return value;
        }

        /**
         * Getter on the date of measurement saved within this.
         * @return date of measurement saved within this
         */
        protected AbsoluteDate getDate() {
            return date;
        }
    }

    /** Class containers for stocking either phase or range measure on a given frequency.
     * Contains a value (phase or range) and the frequency of the measurements.
     * @author David Soulard
     */
    protected class DataMeasure {

        /** phase measurement minus code measurement. */
        private double value;

        /**Frequency use.*/
        private Frequency freq;

        /** Simple constructor.
         * @param value phase measurement minus code measurement
         * @param f frequency use
         */
        DataMeasure(final double value, final Frequency f) {
            this.value  = value;
            this.freq   = f;
        }

        /**
         * Get the value saved within the DataMeasure.
         * @return the value saved within the DataMeasure
         */
        protected double getValue() {
            return value;
        }

        /**
         * Get the frequency saved within this.
         * @return the frequency saved within this
         */
        protected Frequency getFrequency() {
            return freq;
        }

        /**
         * Get the frequency value saved in MHz.
         * @return the frequency in MHZ.
         */
        protected double GetFrequencyMHz() {
            return freq.getMHzFrequency();
        }
    }

    /**
     * Containers for all the data need for doing cycle-slip detection.
     * @author David Soulard
     *
     */
    protected class DataForDetection {
        /**Array used to compute cycle slip.*/
        private SlipComputationData[] figures;

        /**Integer to make the array above circular.*/
        private int write = 0;

        /** integer to know how many data have been added since last cycle-slip. */
        private int canBeComputed;

        /**
         * Constructor.
         * @param value measurement
         * @param date date at which measurements are taken.
         */
        DataForDetection(final double value, final AbsoluteDate date) {
            this.figures                = new SlipComputationData[minMeasurementNumber];
            this.figures[0]             = new SlipComputationData(value, date);
            this.canBeComputed          = 1;
        }

        /**
         * Get the array of values used for computation of cycle-slip detectors.
         * @return SlipComputationDatat array
         */
        protected SlipComputationData[] getFiguresReference() {
            return figures;
        }

        /** Get the reference of the counter of position into the array.
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
         * ReSet this to the initial value when a cycle slip occurs.
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

        /**
         * Setter when a new measurements is added (only used in the Statistical case).
         * All the parameter are adjusted with respect to the new measurement
         * @param value value of the measurement considered
         * @param currentDate  date of the measurement
         */
        protected void goOneStep(final double value, final AbsoluteDate currentDate) {
            this.write                    = (this.write + 1) % minMeasurementNumber;
            this.figures[this.write]    = new SlipComputationData(value, currentDate);
            this.canBeComputed++;
        }
    }
}
