/* Copyright 2022-2025 Luc Maisonobe
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

import org.orekit.gnss.ObservationType;
import org.orekit.gnss.SatInSystem;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.gnss.TimeSystem;
import org.orekit.time.TimeScales;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

/** Parse information for Solution INdependent EXchange (SINEX) bias files.
 * @author Luc Maisonobe
 * @since 13.0
 */
public class SinexBiasParseInfo extends ParseInfo<SinexBias> {

    /** Mapper from satellite system and string to observation type. */
    private final BiFunction<? super SatelliteSystem, ? super String, ? extends ObservationType> typeBuilder;

    /** DSB description. */
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
     * @param typeBuilder mapper from string to observation type
     */
    SinexBiasParseInfo(final TimeScales timeScales,
                       final BiFunction<? super SatelliteSystem, ? super String, ? extends ObservationType> typeBuilder) {
        super(timeScales);
        this.description   = new BiasDescription();
        this.stationsDsb   = new HashMap<>();
        this.satellitesDsb = new HashMap<>();
        this.stationsOsb   = new HashMap<>();
        this.satellitesOsb = new HashMap<>();
        this.typeBuilder   = typeBuilder;
    }

    /** Get description.
     * @return description
     */
    BiasDescription getDescription() {
        return description;
    }

    /** Get satellite DSB.
     * @param satellite satellite identifier
     * @return satellite DSB
     */
    SatelliteDifferentialSignalBias getSatelliteDsb(final SatInSystem satellite) {
        return satellitesDsb.computeIfAbsent(satellite, SatelliteDifferentialSignalBias::new);
    }

    /** Get station DSB.
     * @param siteCode station site code
     * @return station DSB
     */
    StationDifferentialSignalBias getStationDsb(final String siteCode) {
        return stationsDsb.computeIfAbsent(siteCode, StationDifferentialSignalBias::new);
    }

    /** Get satellite OSB.
     * @param satellite satellite identifier
     * @return satellite OSB
     */
    SatelliteObservableSpecificSignalBias getSatelliteOsb(final SatInSystem satellite) {
        return satellitesOsb.computeIfAbsent(satellite, SatelliteObservableSpecificSignalBias::new);
    }

    /** Get station OSB.
     * @param siteCode station site code
     * @return station OSB
     */
    StationObservableSpecificSignalBias getStationOsb(final String siteCode) {
        return stationsOsb.computeIfAbsent(siteCode, StationObservableSpecificSignalBias::new);
    }

    /** Set time system.
     * @param timeSystem time system
     */
    void setTimeSystem(final TimeSystem timeSystem) {
        getDescription().setTimeSystem(timeSystem);
        setTimeScale(timeSystem.getTimeScale(getTimeScales()));
    }

    /** Extract an observation type from current line.
     * @param system satellite system
     * @param start  start index of the string
     * @param length length of the string
     * @return parsed observation type (null if field is empty)
     */
    protected ObservationType parseObservationType(final SatelliteSystem system, final int start, final int length) {
        final String type = parseString(start, length);
        return type.isEmpty() ? null : typeBuilder.apply(system, type);
    }

    /** {@inheritDoc} */
    @Override
    protected SinexBias build() {
        return new SinexBias(getTimeScales(), getCreationDate(), getStartDate(), getEndDate(),
                             description, stationsDsb, satellitesDsb, stationsOsb, satellitesOsb);
    }

}
