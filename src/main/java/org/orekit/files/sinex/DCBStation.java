/* Copyright 2002-2022 CS GROUP
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
import java.util.Set;

import org.orekit.gnss.SatelliteSystem;

/**
 * Class based on DCB, used to store the data parsed in SinexLoader,
 * for Differential Code Biases computed for stations.
 * Satellites and stations have differentiated classes as stations
 * might have multiple satellite systems.
 * The data is stored in a Map of DCB, identified by the SatelliteSystem.
 *
 * @author Louis Aucouturier
 * @since 11.2
 */
public class DCBStation {

    /** Station ID. */
    private String stationId;

    /** DCB description. */
    private DCBDescription dcbDescription;

    /** Map containing DCB objects as a function of the Satellite System. */
    private HashMap<SatelliteSystem, DCB> DCBMap;

    /**
     * Simple constructor.
     * @param stationId
     */
    public DCBStation(final String stationId) {
        this.stationId = stationId;
        this.dcbDescription = null;
        this.DCBMap = new HashMap<SatelliteSystem, DCB>();
    }
    /**
     * Return the station id, as a String.
     *
     * @return String object corresponding to the identifier of the station.
     */
    public String getStationId() {
        return stationId;
    }

    /**
     * Get the DCB description object.
     * @return a DCBDescription object containing data parsed from SinexLoader.
     */
    public DCBDescription getDcbDescription() {
        return dcbDescription;
    }

    /**
     * Set the DCB description object.
     *
     * @param dcbDesc
     */
    public void setDCBDescription(final DCBDescription dcbDesc) {
        this.dcbDescription = dcbDesc;
    }

    /**
     * Get the DCB object for a given Satellite system.
     *
     * @param satSystem
     * @return dcb
     */
    public DCB getDcbObject(final SatelliteSystem satSystem) {
        final DCB dcb = DCBMap.get(satSystem);
        if (dcb == null) {
            return null;
        } else {
            return dcb;
        }
    }

    /**
     * Add the DCB object corresponding to a SatelliteSystem, to the map containing all DCB
     * objects for a particular station.
     *
     * @param satSystem
     * @param dcb
     */
    public void addDcbObject(final SatelliteSystem satSystem, final DCB dcb) {
        DCBMap.put(satSystem, dcb);
    }

    /**
     * Get the available Satellite systems available for a particular station.
     *
     * @return Set containing all SatelliteSystems available for DCB computation.
     */
    public Set<SatelliteSystem> getAvailableSatelliteSystems() {
        return DCBMap.keySet();
    }

}
