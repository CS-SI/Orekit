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

package org.orekit.files.sinex;

import java.util.HashSet;
import java.util.HashMap;
import org.orekit.utils.TimeSpanMap;
import org.orekit.gnss.ObservationType;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.time.AbsoluteDate;

public class DCB {

    /** Satellite PRN identifier.*/
    private String satPRN;

    /** Station ID. */
    private String stationId;

    /** Ensemble of Observation Code pairs present for the satellite. */
    private HashSet< HashSet<ObservationType> > observationSets;

    /**
     * Ensemble of DCBCode object, identifiable by observation code pairs,
     * each containing the corresponding TimeSpanMap of biases (DCB).
     */
    private HashMap<HashSet<ObservationType>, DCBCode> DCBCodeMap;


    /**
     * Constructor taking the satellite's PRN identifier as a parameter.
     * @param satPRN
     * @param stationId
     */
    public DCB(final String satPRN, final String stationId) {
        this.satPRN = satPRN;
        this.stationId = stationId;
        this.observationSets = new HashSet< HashSet<ObservationType> >();
        this.DCBCodeMap = new HashMap<HashSet<ObservationType>, DCBCode>();
    }

    // Class to store the TimeSpanMap per DCB Observation Code set
    private class DCBCode {

        /** TimeSpanMap containing the DCB values. */
        private TimeSpanMap<Double> DCBMap;

        /**
         * Simple constructor.
         */
        DCBCode() {
            this.DCBMap = new TimeSpanMap<>(null);
        }

        /**
         * Getter for the TimeSpanMap.
         * @return DCBMap, a timespanmap containing DCB values, for the observation code pair
         * corresponding to this DCBCode object.
         */
        public TimeSpanMap<Double> getDCBTimeMap() {
            return DCBMap;
        }
    }

    /**
     * Method to add the content of a DCB line to the DCBSatellite object.
     * Check presence of Code pair in a map, and add values to the corresponding
     * TimeSpanMap.
     *
     * @param Obs1
     * @param Obs2
     * @param spanBegin
     * @param spanEnd
     * @param biasValue
     */
    public void addDCBLine(final String Obs1, final String Obs2, final AbsoluteDate spanBegin, final AbsoluteDate spanEnd, final double biasValue) {

        //final ObservationPair singleObservationPair = new ObservationPair(Obs1, Obs2);

        final ObservationType Observation1 = ObservationType.valueOf(Obs1);
        final ObservationType Observation2 = ObservationType.valueOf(Obs2);
        final HashSet<ObservationType> singleObservationPair = new HashSet<ObservationType>();
        singleObservationPair.add(Observation1);
        singleObservationPair.add(Observation2);

        if (observationSets.add(singleObservationPair)) {
            DCBCodeMap.put(singleObservationPair, new DCBCode());
            DCBCodeMap.get(singleObservationPair).getDCBTimeMap().addValidBetween(biasValue, spanBegin, spanEnd);
        } else {
            DCBCodeMap.get(singleObservationPair).getDCBTimeMap().addValidBetween(biasValue, spanBegin, spanEnd);
        }
    }

    /**
     * Return the value of the Differential Code Bias for a given Observation Code pair,
     * at a given date, for the satellite this object has been created for.
     *
     * @param Obs1 String corresponding to the first code used for the DCB computation.
     * @param Obs2 String corresponding to the second code used for the DCB computation.
     * @param date Date at which to obtain the DCB.
     * @return The value of the DCB for a given Observation Code pair, at a given date.
     */
    public double getDCB(final String Obs1, final String Obs2, final AbsoluteDate date) {
        return getTimeSpanMap(Obs1, Obs2).get(date);
    }

    /**
     * Return the TimeSpanMap object for a given Observation Code pair,
     * for further operation on the object directly.
     *
     * @param Obs1 String corresponding to the first code used for the DCB computation.
     * @param Obs2 String corresponding to the second code used for the DCB computation.
     * @return The TimeSpanMap for a given Observation Code pair.
     */
    public TimeSpanMap<Double> getTimeSpanMap(final String Obs1, final String Obs2) {
        final HashSet<ObservationType> ObservationPair = new HashSet<ObservationType>();
        final ObservationType ObsType1 = ObservationType.valueOf(Obs1);
        final ObservationType ObsType2 = ObservationType.valueOf(Obs2);
        ObservationPair.add(ObsType1);
        ObservationPair.add(ObsType2);
        return DCBCodeMap.get(ObservationPair).getDCBTimeMap();
    }

    /**
     * Getter to obtain all available observation code pairs for the satellite.
     * @return HashSet(HashSet(ObservationType)) Observation code pairs obtained.
     */
    public HashSet< HashSet<ObservationType> > getAvailableObservationPairs() {
        return observationSets;
    }
    /**
     * Return the SatelliteSytem object corresponding to the satellite.
     *
     * @return SatelliteSystem object corresponding to the satellite
     * from which the DCB are extracted.
     */
    public SatelliteSystem getSatelliteSytem() {
        return SatelliteSystem.parseSatelliteSystem(satPRN);
    }

    /**
     * Return the satellite PRN, as a String.
     *
     * @return String object corresponding to the PRN identifier of the satellite.
     */
    public String getPRN() {
        return satPRN;
    }

    /**
     * Return the station id, as a String.
     *
     * @return String object corresponding to the identifier of the station.
     */
    public String getStationId() {
        return stationId;
    }

    /**
     * Return the object id, as defined in the SinexLoader, as a String.
     *
     * @return String object corresponding to the DCBMap key, as defined in SinexLoader.
     */
    public String getId() {
        return (stationId.equals("")) ? satPRN : satPRN.concat(stationId);
    }

    public AbsoluteDate getMinDateObservationPair(final String Obs1, final String Obs2) {
        final TimeSpanMap<Double> timeSpanMap = this.getTimeSpanMap(Obs1, Obs2);
        final AbsoluteDate date =  timeSpanMap.getFirstTransition().getDate();
        return date;
    }

    public AbsoluteDate getMaxDateObservationPair(final String Obs1, final String Obs2) {
        final TimeSpanMap<Double> timeSpanMap = this.getTimeSpanMap(Obs1, Obs2);
        final AbsoluteDate date =  timeSpanMap.getLastTransition().getDate();
        return date;
    }
}

