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

import org.orekit.gnss.SatelliteSystem;

/**
 * Class based on DSB, used to store the data parsed in {@link SinexLoader}
 * for Differential Code Biases computed for stations.
 * <p>
 * Satellites and stations have differentiated classes as stations might have multiple satellite systems.
 * The data are stored in a Map of DSB, identified by the {@link SatelliteSystem}
 * </p>
 * @author Louis Aucouturier
 * @since 12.0
 */
public class DsbStation {

    /** Site code. */
    private final String siteCode;

    /** Map containing DSB objects as a function of the satellite system. */
    private final HashMap<SatelliteSystem, Dsb> dcbMap;

    /** Simple constructor.
     * @param siteCode the site code (station identifier)
     */
    public DsbStation(final String siteCode) {
        this.siteCode = siteCode;
        this.dcbMap   = new HashMap<>();
    }

    /** Get the site code (station identifier).
     * @return the site code
     */
    public String getSiteCode() {
        return siteCode;
    }

    /** Get the DSB data for a given satellite system.
     * @param satelliteSystem satellite system
     * @return the DSB data corresponding to the satellite system
     */
    public Dsb getDcbData(final SatelliteSystem satelliteSystem) {
        return dcbMap.computeIfAbsent(satelliteSystem, s -> new Dsb());
    }

    /** Add the DSB data corresponding to a satellite system.
     * <p>
     * If the instance previously contained DSB data for the satellite system, the old value is replaced.
     * </p>
     * @param satelliteSystem satellite system for which the DSB is added
     * @param dsb DSB data
     */
    public void addDcb(final SatelliteSystem satelliteSystem, final Dsb dsb) {
        dcbMap.put(satelliteSystem, dsb);
    }

    /** Get the satellite systems available for the station.
     * @return a Set containing all SatelliteSystems available for DSB computation.
     */
    public Iterable<SatelliteSystem> getAvailableSatelliteSystems() {
        return dcbMap.keySet();
    }

}
