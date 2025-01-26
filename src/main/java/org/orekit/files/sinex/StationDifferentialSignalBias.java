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

import java.util.Collection;
import java.util.HashMap;

import org.orekit.gnss.SatelliteSystem;

/** Container for {@link DifferentialSignalBias Differential Signal Biases} associated to one station.
 * @author Louis Aucouturier
 * @since 12.0
 */
public class StationDifferentialSignalBias {

    /** Site code. */
    private final String siteCode;

    /** Map containing DSB objects as a function of the satellite system. */
    private final HashMap<SatelliteSystem, DifferentialSignalBias> dsbMap;

    /** Simple constructor.
     * @param siteCode the site code (station identifier)
     */
    public StationDifferentialSignalBias(final String siteCode) {
        this.siteCode = siteCode;
        this.dsbMap   = new HashMap<>();
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
    public DifferentialSignalBias getDsb(final SatelliteSystem satelliteSystem) {
        return dsbMap.computeIfAbsent(satelliteSystem, s -> new DifferentialSignalBias());
    }

    /** Get the satellite systems available for the station.
     * @return a Set containing all SatelliteSystems available for DSB computation.
     */
    public Collection<SatelliteSystem> getAvailableSatelliteSystems() {
        return dsbMap.keySet();
    }

}
