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

import org.orekit.gnss.SatelliteSystem;

import java.util.Collection;
import java.util.HashMap;

/**
 * Class based on OSB, used to store the data parsed in {@link SinexBiasParser}
 * for Observation Signal Biases computed for stations.
 * <p>
 * Satellites and stations have differentiated classes as stations might have multiple satellite systems.
 * The data are stored in a Map of OSB, identified by the {@link SatelliteSystem}
 * </p>
 * @author Louis Aucouturier
 * @author Luc Maisonobe
 * @since 13.0
 */
public class StationObservableSpecificSignalBias {

    /** Site code. */
    private final String siteCode;

    /** Map containing OSB objects as a function of the satellite system. */
    private final HashMap<SatelliteSystem, ObservableSpecificSignalBias> osbMap;

    /** Simple constructor.
     * @param siteCode the site code (station identifier)
     */
    public StationObservableSpecificSignalBias(final String siteCode) {
        this.siteCode = siteCode;
        this.osbMap   = new HashMap<>();
    }

    /** Get the site code (station identifier).
     * @return the site code
     */
    public String getSiteCode() {
        return siteCode;
    }

    /** Get the OSB data for a given satellite system.
     * @param satelliteSystem satellite system
     * @return the OSB data corresponding to the satellite system
     */
    public ObservableSpecificSignalBias getOsb(final SatelliteSystem satelliteSystem) {
        return osbMap.computeIfAbsent(satelliteSystem, s -> new ObservableSpecificSignalBias());
    }

    /** Get the satellite systems available for the station.
     * @return a Set containing all SatelliteSystems available for DSB computation.
     */
    public Collection<SatelliteSystem> getAvailableSatelliteSystems() {
        return osbMap.keySet();
    }

}
