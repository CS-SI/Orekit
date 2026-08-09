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

import java.util.Map;

/** Orbit Exchange Format (ORBEX) files.
 * @see <a href="https://acc.igs.org/misc/ORBEX009.pdf">ORBEX - The orbit Exchange format - Draft version 0.09</a>
 * @author Luc Maisonobe
 * @since 14.0
 */
public class Orbex extends AbstractSinex {

    /** Description. */
    private final Description description;

    /** Ephemeris data. */
    private final Map<SatInSystem, Data> data;

    /** Simple constructor.
     * @param version      version number
     * @param timeScales   time scales
     * @param creationDate ORBEX file creation date
     * @param startDate    start time of the data
     * @param endDate      end time of the data
     * @param description  description
     * @param data         ephemeris data
     */
    Orbex(final double version, final TimeScales timeScales,
          final AbsoluteDate creationDate,
          final AbsoluteDate startDate, final AbsoluteDate endDate,
          final Description description, final Map<SatInSystem, Data> data) {
        super(version, timeScales, creationDate, startDate, endDate);
        this.description = description;
        this.data        = data;
    }

    /** Get the file description.
     * @return file description
     */
    public Description getDescription() {
        return description;
    }

    /** Get ephemeris data.
     * @return ephemeris data
     */
    public Map<SatInSystem, Data> getData() {
        return data;
    }

}
