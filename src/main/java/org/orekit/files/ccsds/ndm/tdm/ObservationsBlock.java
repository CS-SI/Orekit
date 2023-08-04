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

package org.orekit.files.ccsds.ndm.tdm;

import java.util.ArrayList;
import java.util.List;

import org.orekit.files.ccsds.section.CommentsContainer;
import org.orekit.files.ccsds.section.Data;
import org.orekit.time.AbsoluteDate;

/** The Observations Block class contain metadata and the list of observation data lines.<p>
 * The reason for which the observations have been separated into blocks is that the different
 * data blocks in a TDM file usually refers to different types of observations.<p>
 * An observation block is associated with a TDM metadata object and contains a list of observations.<p>
 * At this level, an observation is not an Orekit object, it is a custom object containing:<p>
 *  - a keyword, the type of the observation;<p>
 *  - a timetag, the date of the observation;<p>
 *  - a measurement, the value of the observation.
 * @author Maxime Journot
 */
public class ObservationsBlock extends CommentsContainer implements Data {

    /** Current observation epoch. */
    private AbsoluteDate currentObservationEpoch;

    /** List of observations data lines. */
    private List<Observation> observations;

    /** ObservationsBlock constructor. */
    public ObservationsBlock() {
        observations = new ArrayList<>();
    }

    /** Add the epoch of current observation.
     * @param epoch current observation epoch
     * @return alwaus return {@code true}
     */
    boolean addObservationEpoch(final AbsoluteDate epoch) {
        refuseFurtherComments();
        currentObservationEpoch = epoch;
        return true;
    }

    /** Get current observation epoch if set.
     * @return current observation epoch, or null if not set
     */
    AbsoluteDate getCurrentObservationEpoch() {
        return currentObservationEpoch;
    }

    /** Add the value of current observation.
     * @param type type of the observation
     * @param measurement measurement of the observation
     */
    void addObservationValue(final ObservationType type, final double measurement) {
        addObservation(type, currentObservationEpoch, measurement);
        currentObservationEpoch = null;
    }

    /** Get the list of Observations data lines.
     * @return a reference to the internal list of Observations data lines
     */
    public List<Observation> getObservations() {
        return this.observations;
    }

    /** Set the list of Observations Data Lines.
     * @param observations the list of Observations Data Lines to set
     */
    public void setObservations(final List<Observation> observations) {
        refuseFurtherComments();
        this.observations = new ArrayList<>(observations);
    }

    /** Adds an observation data line.
     * @param observation the observation to add to the list
     */
    public void addObservation(final Observation observation) {
        refuseFurtherComments();
        this.observations.add(observation);
    }

    /** Adds an observation data line.
     * @param type type of the observation
     * @param epoch the timetag
     * @param measurement the measurement
     */
    public void addObservation(final ObservationType type,
                               final AbsoluteDate epoch,
                               final double measurement) {
        this.addObservation(new Observation(type, epoch, measurement));
    }

}
