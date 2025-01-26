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

import org.orekit.gnss.SatInSystem;
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
    private final Map<String, StationDifferentialSignalBias> stationsDsb;

    /** DSB data. */
    private final Map<SatInSystem, SatelliteDifferentialSignalBias> satellitesDsb;

    /** OSB data. */
    private final Map<String, StationObservableSpecificSignalBias> stationsOsb;

    /** OSB data. */
    private final Map<SatInSystem, SatelliteObservableSpecificSignalBias> satellitesOsb;

    /** Simple constructor.
     * @param timeScales time scales
     * @param creationDate SINEX file creation date
     * @param startDate start time of the data used in the Sinex solution
     * @param endDate end time of the data used in the Sinex solution
     * @param description bias description
     * @param stationsDsb DSB data for stations
     * @param satellitesDsb DSB data for satellites
     * @param stationsOsb OSB data for stations
     * @param satellitesOsb OSB data for satellites
     */
    public SinexBias(final TimeScales timeScales, final AbsoluteDate creationDate,
                     final AbsoluteDate startDate, final AbsoluteDate endDate,
                     final BiasDescription description,
                     final Map<String, StationDifferentialSignalBias> stationsDsb,
                     final Map<SatInSystem, SatelliteDifferentialSignalBias> satellitesDsb,
                     final Map<String, StationObservableSpecificSignalBias> stationsOsb,
                     final Map<SatInSystem, SatelliteObservableSpecificSignalBias> satellitesOsb) {
        super(timeScales, creationDate, startDate, endDate);
        this.description   = description;
        this.stationsDsb   = stationsDsb;
        this.satellitesDsb = satellitesDsb;
        this.stationsOsb   = stationsOsb;
        this.satellitesOsb = satellitesOsb;
    }

    /** Get the bias description.
     * @return bias description
     */
    public BiasDescription getDescription() {
        return description;
    }

    /** Get the DSB data for stations.
     * @return DSB data for stations, indexed by station site code
     */
    public Map<String, StationDifferentialSignalBias> getStationsDsb() {
        return stationsDsb;
    }

    /** Get the DSB data for satellites.
     * @return DSB data for satellites
     */
    public Map<SatInSystem, SatelliteDifferentialSignalBias> getSatellitesDsb() {
        return satellitesDsb;
    }

    /** Get the OSB data for stations.
     * @return OSB data for stations, indexed by station site code
     */
    public Map<String, StationObservableSpecificSignalBias> getStationsOsb() {
        return stationsOsb;
    }

    /** Get the OSB data for satellites.
     * @return OSB data for satellites
     */
    public Map<SatInSystem, SatelliteObservableSpecificSignalBias> getSatellitesOsb() {
        return satellitesOsb;
    }

}
