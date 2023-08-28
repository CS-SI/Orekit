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

package org.orekit.files.sinex;

import java.util.HashMap;
import java.util.HashSet;

import org.hipparchus.util.Pair;
import org.orekit.gnss.ObservationType;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeSpanMap;

/**
 * Class to store DCB Solution data parsed in the SinexLoader.
 * <p>
 * This class is made to handle both station and satellite DCB data.
 * Bias values are stored in TimeSpanMaps associated with a given pair
 * of observation codes. Those TimeSpanMaps are stored in a Map, which
 * associate a pair of observation code (as a HashSet of ObservationType)
 * to a TimeSpanMap,  encapsulated in a DCBCode object.
 * </p>
 * @author Louis Aucouturier
 * @since 12.0
 */
public class Dcb {

    /** Ensemble of observation code pairs available for the satellite. */
    private HashSet<Pair<ObservationType, ObservationType>> observationSets;

    /**
     * Ensemble of DCBCode object, identifiable by observation code pairs,
     * each containing the corresponding TimeSpanMap of biases (DCB).
     */
    private HashMap<Pair<ObservationType, ObservationType>, DcbCode> dcbCodeMap;

    /** Simple constructor. */
    public Dcb() {
        this.observationSets = new HashSet<>();
        this.dcbCodeMap      = new HashMap<>();
    }

    // Class to store the TimeSpanMap per DCB Observation Code set
    private static class DcbCode {

        /** TimeSpanMap containing the DCB bias values. */
        private TimeSpanMap<Double> dcbMap;

        /**
         * Simple constructor.
         */
        DcbCode() {
            this.dcbMap = new TimeSpanMap<>(null);
        }

        /**
         * Getter for the TimeSpanMap of DCB values.
         *
         * @return a time span map containing DCB values, for the observation code pair
         * corresponding to this DCBCode object
         */
        public TimeSpanMap<Double> getDcbTimeMap() {
            return dcbMap;
        }

    }

    /**
     * Add the content of a DCB line to the DCBSatellite object.
     * <p>
     * The method check the presence of a Code pair in a map, and add
     * values to the corresponding TimeSpanMap.
     * </p>
     * @param obs1 String corresponding to the first code used for the DCB computation
     * @param obs2 String corresponding to the second code used for the DCB computation
     * @param spanBegin Absolute Date corresponding to the beginning of the validity span for this bias value
     * @param spanEnd Absolute Date corresponding to the end of the validity span for this bias value
     * @param biasValue DCB bias value expressed in S.I. units
     */
    public void addDcbLine(final String obs1, final String obs2,
                           final AbsoluteDate spanBegin, final AbsoluteDate spanEnd,
                           final double biasValue) {

        // Setting a pair of observation type.
        final Pair<ObservationType, ObservationType> observationPair = new Pair<>(ObservationType.valueOf(obs1), ObservationType.valueOf(obs2));

        // If not present add a new DCBCode to the map, identified by the Observation Pair.
        // Then add the bias value and validity period.
        if (observationSets.add(observationPair)) {
            dcbCodeMap.put(observationPair, new DcbCode());
        }

        dcbCodeMap.get(observationPair).getDcbTimeMap().addValidBetween(biasValue, spanBegin, spanEnd);
    }

    /**
     * Get the value of the Differential Code Bias for a given observation pair
     * and a at a given date.
     *
     * @param obs1 string corresponding to the first code used for the DCB computation
     * @param obs2 string corresponding to the second code used for the DCB computation
     * @param date date at which to obtain the DCB
     * @return the value of the DCB in S.I. units
     */
    public double getDcb(final String obs1, final String obs2, final AbsoluteDate date) {
        return getDcb(ObservationType.valueOf(obs1), ObservationType.valueOf(obs2), date);
    }

    /**
     * Get the value of the Differential Code Bias for a given observation pair
     * and a at a given date.
     *
     * @param obs1 first observation type
     * @param obs2 second observation type
     * @param date date at which to obtain the DCB
     * @return the value of the DCB in S.I. units
     */
    public double getDcb(final ObservationType obs1, final ObservationType obs2, final AbsoluteDate date) {
        return getTimeSpanMap(obs1, obs2).get(date);
    }

    /**
     * Get all available observation code pairs for the satellite.
     *
     * @return HashSet(HashSet(ObservationType)) Observation code pairs obtained.
     */
    public HashSet<Pair<ObservationType, ObservationType>> getAvailableObservationPairs() {
        return observationSets;
    }

    /**
     * Get the minimum valid date for a given observation pair.
     *
     * @param obs1 sString corresponding to the first code used for the DCB computation
     * @param obs2 string corresponding to the second code used for the DCB computation
     * @return minimum valid date for the observation pair
     */
    public AbsoluteDate getMinimumValidDateForObservationPair(final String obs1, final String obs2) {
        return getMinimumValidDateForObservationPair(ObservationType.valueOf(obs1), ObservationType.valueOf(obs2));
    }

    /**
     * Get the minimum valid date for a given observation pair.
     *
     * @param obs1 first observation type
     * @param obs2 second observation type
     * @return minimum valid date for the observation pair
     */
    public AbsoluteDate getMinimumValidDateForObservationPair(final ObservationType obs1, final ObservationType obs2) {
        return getTimeSpanMap(obs1, obs2).getFirstTransition().getDate();
    }

    /**
     * Get the maximum valid date for a given observation pair.
     *
     * @param obs1 string corresponding to the first code used for the DCB computation
     * @param obs2 string corresponding to the second code used for the DCB computation
     * @return maximum valid date for the observation pair
     */
    public AbsoluteDate getMaximumValidDateForObservationPair(final String obs1, final String obs2) {
        return getMaximumValidDateForObservationPair(ObservationType.valueOf(obs1), ObservationType.valueOf(obs2));
    }

    /**
     * Get the maximum valid date for a given observation pair.
     *
     * @param obs1 first observation type
     * @param obs2 second observation type
     * @return maximum valid date for the observation pair
     */
    public AbsoluteDate getMaximumValidDateForObservationPair(final ObservationType obs1, final ObservationType obs2) {
        return getTimeSpanMap(obs1, obs2).getLastTransition().getDate();
    }

    /**
     * Return the TimeSpanMap object for a given observation code pair,
     * for further operation on the object directly.
     *
     * @param obs1 first observation type
     * @param obs2 second observation type
     * @return the time span map for a given observation code pair
     */
    private TimeSpanMap<Double> getTimeSpanMap(final ObservationType obs1, final ObservationType obs2) {
        return dcbCodeMap.get(new Pair<>(obs1, obs2)).getDcbTimeMap();
    }

}

