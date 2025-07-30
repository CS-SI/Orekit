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

import org.orekit.gnss.ObservationType;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeSpanMap;

import java.util.HashMap;
import java.util.HashSet;

/** Container for observation-specific signal bias for a single link endpoint
 * (either emitter or receiver).
 * <p>
 * This class is made to handle both station and satellite OSB data.
 * Bias values are stored in TimeSpanMaps associated with a given
 * observation type. Those TimeSpanMaps are stored in a Map, which
 * associate an observation code to a TimeSpanMap of double values.
 * </p>
 * @author Louis Aucouturier
 * @author Luc Maisonobe
 * @since 13.0
 */
public class ObservableSpecificSignalBias {

    /** Set of observation types available for the satellite. */
    private final HashSet<ObservationType> observationSets;

    /** Set of biases, identifiable by observation types,
     * each containing the corresponding TimeSpanMap of biases.
     */
    private final HashMap<ObservationType, TimeSpanMap<Double>> biases;

    /** Simple constructor.
     */
    public ObservableSpecificSignalBias() {
        this.observationSets = new HashSet<>();
        this.biases          = new HashMap<>();
    }

    /** Add a bias.
     * @param obs observation used for the OSB computation
     * @param spanBegin beginning of the validity span for this bias value
     * @param spanEnd end of the validity span for this bias value
     * @param biasValue Observable-specific Signal Bias value (meters for code and cycle for phase)
     */
    public void addBias(final ObservationType obs,
                        final AbsoluteDate spanBegin, final AbsoluteDate spanEnd,
                        final double biasValue) {

        // If not present add a new bias to the map, identified by the Observation type.
        // Then add the bias value and validity period.
        if (observationSets.add(obs)) {
            biases.put(obs, new TimeSpanMap<>(null));
        }

        biases.get(obs).addValidBetween(biasValue, spanBegin, spanEnd);
    }

    /** Get the value of the Observable-specific Signal Bias for a given observation type at a given date.
     * @param obs observation type
     * @param date date at which to obtain the Observable-specific Signal Bias
     * @return the value of the Observable-specific Signal Bias (meters for code and cycle for phase)
     */
    public double getBias(final ObservationType obs, final AbsoluteDate date) {
        return getTimeSpanMap(obs).get(date);
    }

    /** Get all available observation types for the satellite.
     * @return Observation types obtained.
     */
    public HashSet<ObservationType> getAvailableObservations() {
        return observationSets;
    }

    /** Get the minimum valid date for a given observation type.
     * @param obs observation type
     * @return minimum valid date for the observation pair
     */
    public AbsoluteDate getMinimumValidDateForObservation(final ObservationType obs) {
        return getTimeSpanMap(obs).getFirstTransition().getDate();
    }

    /** Get the maximum valid date for a given observation type.
     * @param obs observation type
     * @return maximum valid date for the observation pair
     */
    public AbsoluteDate getMaximumValidDateForObservation(final ObservationType obs) {
        return getTimeSpanMap(obs).getLastTransition().getDate();
    }

    /** Get the TimeSpanMap object for a given observation type,
     * for further operation on the object directly.
     *
     * @param obs observation type
     * @return the time span map for a given observation code pair
     */
    public TimeSpanMap<Double> getTimeSpanMap(final ObservationType obs) {
        return biases.get(obs);
    }

}
