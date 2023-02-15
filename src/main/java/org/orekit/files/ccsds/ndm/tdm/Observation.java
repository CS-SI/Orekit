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

import org.orekit.time.AbsoluteDate;


/** The Observation class contains the data from an observation line.
 * <p>
 * It is not an Orekit object yet. It is a simple container holding:
 * </p>
 * <ul>
 *  <li>a keyword, the type of the observation;</li>
 *  <li>a timetag, the epoch of the observation;</li>
 *  <li>a measurement, the value of the observation.</li>
 * </ul>
 * <p>
 * WARNING. The same class handles many different measurements
 * types (range, Doppler, clocks, pressure, power to noise ratio…).
 * Since Orekit 11.0, it uses only SI units, so angular measurements
 * have already been converted in radians, range has been converted
 * in meters (according to the {@link TdmMetadata#getRangeUnits()
 * range units}, Doppler has been converted to meters per second.
 * Up to Orekit 10.x, the measurements were raw measurements as read
 * in the TDM.
 * </p>
 * @author Maxime Journot
 */
public class Observation {

    /** Type of the observation. */
    private final ObservationType type;

    /** Epoch: the timetag of the observation. */
    private final AbsoluteDate epoch;

    /** Measurement: the value of the observation. */
    private final double measurement;

    /** Simple constructor.
     * @param type type of the observation
     * @param epoch the timetag
     * @param measurement the measurement (in SI units, converted from TDM)
     */
    public Observation(final ObservationType type, final AbsoluteDate epoch, final double measurement) {
        this.type        = type;
        this.epoch       = epoch;
        this.measurement = measurement;
    }

    /** Get the type of observation.
     * @return type of observation
     */
    public ObservationType getType() {
        return type;
    }

    /** Getter for the epoch.
     * @return the epoch
     */
    public AbsoluteDate getEpoch() {
        return epoch;
    }

    /** Getter for the measurement.
     * @return the measurement (in SI units, converted from TDM)
     */
    public double getMeasurement() {
        return measurement;
    }

}
