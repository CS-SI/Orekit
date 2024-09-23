/* Copyright 2002-2024 CS GROUP
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
import java.util.function.Function;

import org.hipparchus.util.Pair;
import org.orekit.gnss.ObservationType;
import org.orekit.gnss.PredefinedObservationType;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeSpanMap;

/**
 * Class to store DSB Solution data parsed in the SinexBiasParser.
 * <p>
 * This class is made to handle both station and satellite DSB data.
 * Bias values are stored in TimeSpanMaps associated with a given pair
 * of observation codes. Those TimeSpanMaps are stored in a Map, which
 * associate a pair of observation code (as a HashSet of ObservationType)
 * to a TimeSpanMap, encapsulated in a DSBCode object.
 * </p>
 * @author Louis Aucouturier
 * @since 12.0
 */
public class Dsb {

    /** Ensemble of observation code pairs available for the satellite. */
    private final HashSet<Pair<ObservationType, ObservationType>> observationSets;

    /**
     * Ensemble of DSBCode object, identifiable by observation code pairs,
     * each containing the corresponding TimeSpanMap of biases (DSB).
     */
    private final HashMap<Pair<ObservationType, ObservationType>, TimeSpanMap<Double>> dsbCodeMap;

    /** Mapper from string to observation type.
     * @since 13.0
     */
    private final Function<? super String, ? extends ObservationType> typeBuilder;

    /** Simple constructor.
     * <p>
     * This constructor just recognizes {@link PredefinedObservationType}.
     * </p>
     */
    public Dsb() {
        this(PredefinedObservationType::valueOf);
    }

    /** Simple constructor.
     * @param typeBuilder mapper from string to observation type
     * @since 13.0
     */
    public Dsb(final Function<? super String, ? extends ObservationType> typeBuilder) {
        this.observationSets = new HashSet<>();
        this.dsbCodeMap      = new HashMap<>();
        this.typeBuilder     = typeBuilder;
    }

    /**
     * Add the content of a DSB line to the DSBSatellite object.
     * <p>
     * The method check the presence of a Code pair in a map, and add
     * values to the corresponding TimeSpanMap.
     * </p>
     * @param obs1 String corresponding to the first code used for the DSB computation
     * @param obs2 String corresponding to the second code used for the DSB computation
     * @param spanBegin Absolute Date corresponding to the beginning of the validity span for this bias value
     * @param spanEnd Absolute Date corresponding to the end of the validity span for this bias value
     * @param biasValue DSB bias value expressed in S.I. units
     */
    public void addDsbLine(final String obs1, final String obs2,
                           final AbsoluteDate spanBegin, final AbsoluteDate spanEnd,
                           final double biasValue) {

        // Setting a pair of observation type.
        final Pair<ObservationType, ObservationType> observationPair =
            new Pair<>(typeBuilder.apply(obs1), typeBuilder.apply(obs2));

        // If not present add a new DSBCode to the map, identified by the Observation Pair.
        // Then add the bias value and validity period.
        if (observationSets.add(observationPair)) {
            dsbCodeMap.put(observationPair, new TimeSpanMap<>(null));
        }

        dsbCodeMap.get(observationPair).addValidBetween(biasValue, spanBegin, spanEnd);
    }

    /**
     * Get the value of the Differential Signal Bias for a given observation pair at a given date.
     *
     * @param obs1 string corresponding to the first code used for the DSB computation
     * @param obs2 string corresponding to the second code used for the DSB computation
     * @param date date at which to obtain the DSB
     * @return the value of the DSB in S.I. units
     */
    public double getDsb(final String obs1, final String obs2, final AbsoluteDate date) {
        return getDsb(typeBuilder.apply(obs1), typeBuilder.apply(obs2), date);
    }

    /**
     * Get the value of the Differential Signal Bias for a given observation pair at a given date.
     *
     * @param obs1 first observation type
     * @param obs2 second observation type
     * @param date date at which to obtain the DSB
     * @return the value of the DSB in S.I. units
     */
    public double getDsb(final ObservationType obs1, final ObservationType obs2, final AbsoluteDate date) {
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
     * @param obs1 sString corresponding to the first code used for the DSB computation
     * @param obs2 string corresponding to the second code used for the DSB computation
     * @return minimum valid date for the observation pair
     */
    public AbsoluteDate getMinimumValidDateForObservationPair(final String obs1, final String obs2) {
        return getMinimumValidDateForObservationPair(typeBuilder.apply(obs1), typeBuilder.apply(obs2));
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
     * @param obs1 string corresponding to the first code used for the DSB computation
     * @param obs2 string corresponding to the second code used for the DSB computation
     * @return maximum valid date for the observation pair
     */
    public AbsoluteDate getMaximumValidDateForObservationPair(final String obs1, final String obs2) {
        return getMaximumValidDateForObservationPair(typeBuilder.apply(obs1), typeBuilder.apply(obs2));
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
        return dsbCodeMap.get(new Pair<>(obs1, obs2));
    }

}
