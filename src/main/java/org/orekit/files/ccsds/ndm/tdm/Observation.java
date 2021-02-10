/* Copyright 2002-2021 CS GROUP
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

import org.orekit.time.AbsoluteDate;


/** The Observation class contains the data from an observation line.<p>
 * It is not an Orekit object yet.<p>
 * It is a simple container holding:<p>
 *  - a keyword, the type of the observation;<p>
 *  - a timetag, the epoch of the observation;<p>
 *  - a measurement, the value of the observation.<p>
 * @author Maxime Journot
 */
public class Observation {

    /** CCSDS Keyword: the type of the observation. */
    private String keyword;

    /** Epoch: the timetag of the observation. */
    private AbsoluteDate epoch;

    /** Measurement: the value of the observation. */
    private double measurement;

    /** Simple constructor.
     * @param keyword the keyword
     * @param epoch the timetag
     * @param measurement the measurement
     */
    Observation(final String keyword, final AbsoluteDate epoch, final double measurement) {
        this.keyword = keyword;
        this.epoch = epoch;
        this.measurement = measurement;
    }

    /** Getter for the keyword.
     * @return the keyword
     */
    public String getKeyword() {
        return keyword;
    }

    /** Setter for the keyword.
     * @param keyword the keyword to set
     */
    public void setKeyword(final String keyword) {
        this.keyword = keyword;
    }

    /** Getter for the epoch.
     * @return the epoch
     */
    public AbsoluteDate getEpoch() {
        return epoch;
    }

    /** Setter for the epoch.
     * @param epoch the epoch to set
     */
    public void setEpoch(final AbsoluteDate epoch) {
        this.epoch = epoch;
    }

    /** Getter for the measurement.
     * @return the measurement
     */
    public double getMeasurement() {
        return measurement;
    }

    /** Setter for the measurement.
     * @param measurement the measurement to set
     */
    public void setMeasurement(final double measurement) {
        this.measurement = measurement;
    }

}
