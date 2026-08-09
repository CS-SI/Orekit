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

import org.orekit.gnss.SatInSystem;
import org.orekit.time.clocks.ClockOffset;
import org.orekit.utils.AngularCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.util.ArrayList;
import java.util.List;

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

    /**
     * Constructor from id and description only.
     *
     * @param satId       satellite id
     * @param description satellite description
     */
    public Data(final SatInSystem satId, final String description) {
        this(satId, description, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

}
