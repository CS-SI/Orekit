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

package org.orekit.files.sinex;

import java.util.HashMap;

import org.orekit.gnss.SatelliteSystem;

/**
 * Class based on DCB, used to store the data parsed in {@link SinexLoader}
 * for Differential Code Biases computed for stations.
 * <p>
 * Satellites and stations have differentiated classes as stations might have multiple satellite systems.
 * The data are stored in a Map of DCB, identified by the {@link SatelliteSystem}
 * </p>
 * @author Louis Aucouturier
 * @since 12.0
 */
public class DcbStation {

    /** Site code. */
    private String siteCode;

    /** DCB description container. */
    private DcbDescription description;

    /** Map containing DCB objects as a function of the satellite system. */
    private HashMap<SatelliteSystem, Dcb> dcbMap;

    /**
     * Simple constructor.
     * @param siteCode the site code (station identifier)
     */
    public DcbStation(final String siteCode) {
        this.siteCode    = siteCode;
        this.description = null;
        this.dcbMap      = new HashMap<SatelliteSystem, Dcb>();
    }

    /**
     * Get the site code (station identifier).
     *
     * @return the site code
     */
    public String getSiteCode() {
        return siteCode;
    }

    /**
     * Get the data contained in "DCB/DESCRIPTION" block of the Sinex file.
     * <p>
     * This block gives important parameters from the analysis and defines
     * the fields in the block ’BIAS/SOLUTION’
     * </p>
     * @return the "DCB/DESCRIPTION" parameters.
     */
    public DcbDescription getDescription() {
        return description;
    }

    /**
     * Set the data contained in "DCB/DESCRIPTION" block of the Sinex file.
     *
     * @param description the "DCB/DESCRIPTION" parameters to set
     */
    public void setDescription(final DcbDescription description) {
        this.description = description;
    }

    /**
     * Get the DCB data for a given satellite system.
     *
     * @param satelliteSystem satellite system
     * @return the DCB data corresponding to the satellite system
     *         (can be null is no DCB available)
     */
    public Dcb getDcbData(final SatelliteSystem satelliteSystem) {
        return dcbMap.get(satelliteSystem);
    }

    /**
     * Add the DCB data corresponding to a satellite system.
     * <p>
     * If the instance previously contained DCB data for the satellite system, the old value is replaced.
     * </p>
     * @param satelliteSystem satellite system for which the DCB is added
     * @param dcb DCB data
     */
    public void addDcb(final SatelliteSystem satelliteSystem, final Dcb dcb) {
        dcbMap.put(satelliteSystem, dcb);
    }

    /**
     * Get the satellite systems available for the station.
     *
     * @return a Set containing all SatelliteSystems available for DCB computation.
     */
    public Iterable<SatelliteSystem> getAvailableSatelliteSystems() {
        return dcbMap.keySet();
    }

}
