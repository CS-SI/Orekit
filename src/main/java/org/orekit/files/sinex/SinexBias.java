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

import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScales;

import java.util.Map;

/**
 * Container for Solution INdependent EXchange (SINEX) files.
 * @author Bryan Cazabonne
 * @author Luc Maisonobe
 * @since 13.0
 */
public class SinexBias extends AbstractSinex {

    /** Bias description. */
    private final BiasDescription description;

    /** DSB data. */
    private final Map<String, DsbStation> dcbStations;

    /** DSB data. */
    private final Map<String, DsbSatellite> dcbSatellites;

    /** Simple constructor.
     * @param timeScales time scales
     * @param creationDate SINEX file creation date
     * @param startDate start time of the data used in the Sinex solution
     * @param endDate end time of the data used in the Sinex solution
     * @param description bias description
     * @param dcbStations DSB data for stations
     * @param dcbSatellites DSB data for satellites
     */
    public SinexBias(final TimeScales timeScales, final AbsoluteDate creationDate,
                     final AbsoluteDate startDate, final AbsoluteDate endDate,
                     final BiasDescription description,
                     final Map<String, DsbStation> dcbStations, final Map<String, DsbSatellite> dcbSatellites) {
        super(timeScales, creationDate, startDate, endDate);
        this.description   = description;
        this.dcbStations   = dcbStations;
        this.dcbSatellites = dcbSatellites;
    }

    /** Get the bias description.
     * @return bias description
     */
    public BiasDescription getDescription() {
        return description;
    }

    /** Get the DSB data for a given station.
     * @param siteCode site code
     * @return DSB data for the station
     */
    public DsbStation getDsbStation(final String siteCode) {
        return dcbStations.get(siteCode);
    }

    /** Get the DSB data for a given satellite identified by its PRN.
     * @param prn the satellite PRN (e.g. "G01" for GPS 01)
     * @return the DSB data for the satellite
     */
    public DsbSatellite getDsbSatellite(final String prn) {
        return dcbSatellites.get(prn);
    }

}
