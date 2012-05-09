/* Copyright 2002-2012 Space Applications Services
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
package org.orekit.files.general;

import java.io.Serializable;

/** Contains general information about a satellite as contained
 * in an orbit file.
 * @author Thomas Neidhart
 */
public class SatelliteInformation implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = -8329507360193822277L;

    /** Id of the satellite as used in the orbit file. */
    private String satelliteId;

    /** One standard deviation of the orbit entries in m. */
    private int accuracy;

    /** Create a new {@link SatelliteInformation} object with a given
     * satellite id.
     * @param satId the satellite id
     */
    public SatelliteInformation(final String satId) {
        this.satelliteId = satId;
        this.accuracy = 0;
    }

    /** Returns the id for this satellite object.
     * @return the satellite id
     */
    public String getSatelliteId() {
        return satelliteId;
    }

    /** Set the id of this satellite.
     * @param satId the satellite id to be set
     */
    public void setSatelliteId(final String satId) {
        this.satelliteId = satId;
    }

    /** Returns the estimated accuracy of the orbit entries for this
     * satellite (in m).
     * @return the accuracy in m (one standard deviation)
     */
    public int getAccuracy() {
        return accuracy;
    }

    /** Set the accuracy for this satellite.
     * @param accuracy the accuracy in m (one standard deviation)
     */
    public void setAccuracy(final int accuracy) {
        this.accuracy = accuracy;
    }
}
