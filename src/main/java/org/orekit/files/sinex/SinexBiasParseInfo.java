/* Copyright 2002-2024 Luc Maisonobe
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

import org.orekit.gnss.TimeSystem;
import org.orekit.time.TimeScales;

import java.util.HashMap;
import java.util.Map;

/** Parse information for Solution INdependent EXchange (SINEX) bias files.
 * @author Luc Maisonobe
 * @since 13.0
 */
public class SinexBiasParseInfo extends ParseInfo<SinexBias> {

    /** DSB description. */
    private final BiasDescription description;

    /** DSB data. */
    private final Map<String, DsbStation> stations;

    /** DSB data. */
    private final Map<String, DsbSatellite> satellites;

    /** Simple constructor.
     * @param timeScales time scales
     */
    SinexBiasParseInfo(final TimeScales timeScales) {
        super(timeScales);
        this.description = new BiasDescription();
        this.stations    = new HashMap<>();
        this.satellites  = new HashMap<>();
    }

    /** Get description.
     * @return description
     */
    BiasDescription getDescription() {
        return description;
    }

    /** Get satellite DSB.
     * @return satellite DSB
     */
    DsbSatellite getSatelliteDcb(final String prn) {
        return satellites.computeIfAbsent(prn, DsbSatellite::new);
    }

    /** Get station DSB.
     * @return station DSB
     */
    DsbStation getStationDcb(final String siteCode) {
        return stations.computeIfAbsent(siteCode, DsbStation::new);
    }

    /** Set time system.
     * @param timeSystem time system
     */
    void setTimeSystem(final TimeSystem timeSystem) {
        getDescription().setTimeSystem(timeSystem);
        setTimeScale(timeSystem.getTimeScale(getTimeScales()));
    }

    /** {@inheritDoc} */
    @Override
    protected SinexBias build() {
        return new SinexBias(getTimeScales(), getCreationDate(), getStartDate(), getEndDate(),
                             description, stations, satellites);
    }

}
