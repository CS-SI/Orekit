/* Copyright 2022-2026 Luc Maisonobe
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
package org.orekit.files.sinex.orbex;

import org.orekit.files.sinex.AbstractSinex;
import org.orekit.gnss.SatInSystem;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScales;
import org.orekit.time.clocks.ClockOffset;
import org.orekit.utils.AngularCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Orbit Exchange Format (ORBEX) files.
 * @author Luc Maisonobe
 * @since 14.0
 */
public class Orbex extends AbstractSinex {

    /** Ephemeris data. */
    private final Map<SatInSystem, Data> data;

    /** Simple constructor.
     * @param timeScales time scales
     * @param creationDate SINEX file creation date
     * @param startDate start time of the data used in the Sinex solution
     * @param endDate end time of the data used in the Sinex solution
     * @param data ephemeris data
     */
    Orbex(final TimeScales timeScales, final AbsoluteDate creationDate,
          final AbsoluteDate startDate, final AbsoluteDate endDate,
          final Map<SatInSystem, Data> data) {
        super(timeScales, creationDate, startDate, endDate);
        this.data = data;
    }

    /** Get ephemeris data.
     * @return ephemeris data
     */
    public Map<SatInSystem, Data> getData() {
        return data;
    }

    /** Container for one satellite data in Orbex files.
     * @param satId       satellite id
     * @param description satellite description
     * @param orbit       orbit ephemeris
     * @param clock       clock ephemeris
     * @param attitude    attitude ephemeris
     */
    public record Data(SatInSystem satId, String description,
                       List<TimeStampedPVCoordinates> orbit,
                       List<ClockOffset> clock,
                       List<AngularCoordinates> attitude) {

        /** Constructor from id and description only.
         * @param satId       satellite id
         * @param description satellite description
         */
        public Data(final SatInSystem satId, final String description) {
            this(satId, description, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }

    }

}
