/* Copyright 2002-2025 CS GROUP
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

/** Container for differential signal bias for a single link endpoint
 * (either emitter or receiver).
 * <p>
 * This class is made to handle both station and satellite DSB data.
 * Bias values are stored in TimeSpanMaps associated with a given pair
 * of observation types. Those TimeSpanMaps are stored in a Map, which
 * associate a pair of observation types to a TimeSpanMap of double values.
 * </p>
 * @author Louis Aucouturier
 * @since 12.0
 */
public class DifferentialSignalBias {

    /** Set of observation type pairs available for the satellite. */
    private final HashSet<Pair<ObservationType, ObservationType>> observationSets;

    /** Set of biases, identifiable by observation type pairs,
     * each containing the corresponding TimeSpanMap of biases (DSB).
     */
    private final HashMap<Pair<ObservationType, ObservationType>, TimeSpanMap<Double>> biases;

    /** Simple constructor.
     */
    public DifferentialSignalBias() {
        this.observationSets = new HashSet<>();
        this.biases          = new HashMap<>();
    }

    /** Add a bias.
     * @param obs1 first observation used for the DSB computation
     * @param obs2 second observation used for the DSB computation
     * @param spanBegin beginning of the validity span for this bias value
     * @param spanEnd end of the validity span for this bias value
     * @param biasValue DSB bias value (meters for code and cycle for phase)
     */
    public void addBias(final ObservationType obs1, final ObservationType obs2,
                        final AbsoluteDate spanBegin, final AbsoluteDate spanEnd,
                        final double biasValue) {

        // Setting a pair of observation type.
        final Pair<ObservationType, ObservationType> observationPair = new Pair<>(obs1, obs2);

        // If not present add a new bias to the map, identified by the Observation Pair.
        // Then add the bias value and validity period.
        if (observationSets.add(observationPair)) {
            biases.put(observationPair, new TimeSpanMap<>(null));
        }

        biases.get(observationPair).addValidBetween(biasValue, spanBegin, spanEnd);

    }

    /** Get the value of the Differential Signal Bias for a given observation pair at a given date.
     * @param obs1 first observation type
     * @param obs2 second observation type
     * @param date date at which to obtain the DSB
     * @return the value of the DSB (meters for code and cycle for phase)
     */
    public double getBias(final ObservationType obs1, final ObservationType obs2, final AbsoluteDate date) {
        return getTimeSpanMap(obs1, obs2).get(date);
    }

    /** Get all available observation type pairs for the satellite.
     * @return observation type pairs obtained.
     */
    public HashSet<Pair<ObservationType, ObservationType>> getAvailableObservationPairs() {
        return observationSets;
    }

    /** Get the minimum valid date for a given observation pair.
     * @param obs1 first observation type
     * @param obs2 second observation type
     * @return minimum valid date for the observation pair
     */
    public AbsoluteDate getMinimumValidDateForObservationPair(final ObservationType obs1, final ObservationType obs2) {
        return getTimeSpanMap(obs1, obs2).getFirstTransition().getDate();
    }

    /** Get the maximum valid date for a given observation pair.
     * @param obs1 first observation type
     * @param obs2 second observation type
     * @return maximum valid date for the observation pair
     */
    public AbsoluteDate getMaximumValidDateForObservationPair(final ObservationType obs1, final ObservationType obs2) {
        return getTimeSpanMap(obs1, obs2).getLastTransition().getDate();
    }

    /** Get the TimeSpanMap object for a given observation type pair,
     * for further operation on the object directly.
     *
     * @param obs1 first observation type
     * @param obs2 second observation type
     * @return the time span map for a given observation type pair
     */
    private TimeSpanMap<Double> getTimeSpanMap(final ObservationType obs1, final ObservationType obs2) {
        return biases.get(new Pair<>(obs1, obs2));
    }

}
