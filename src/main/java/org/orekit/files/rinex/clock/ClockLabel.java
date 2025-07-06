/* Copyright 2022-2025 Thales Alenia Space
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
package org.orekit.files.rinex.clock;

import org.orekit.files.rinex.section.Label;

/** Label for Rinex files.
 * @author Luc Maisonobe
 * @since 14.0
 */
public enum ClockLabel implements Label {

    /** Time system used for time tags. */
    TIME_SYSTEM_ID("TIME SYSTEM ID"),

    /** Leap seconds separating UTC and GNSS system times. */
    LEAP_SECONDS_GNSS("LEAP SECONDS GNSS"),

    /** Number of different clock data types. */
    NB_TYPES_OF_DATA("# / TYPES OF DATA"),

    /** 4-character or 9-character site ID. */
    STATION_NAME_NUM("STATION NAME / NUM"),

    /** Unique identifier for external reference clock. */
    STATION_CLK_REF("STATION CLK REF"),

    /** Name of Analysis Center. */
    ANALYSIS_CENTER("ANALYSIS CENTER"),

    /** Number of analysis clock references. */
    NB_OF_CLK_REF("# OF CLK REF"),

    /** List of the analysis clock references. */
    ANALYSIS_CLK_REF("ANALYSIS CLK REF"),

    /** Number of receivers included in the clock data records. */
    NB_OF_SOLN_STA_TRF("# OF SOLN STA / TRF"),

    /** Solution station data. */
    SOLN_STA_NAME_NUM("SOLN STA NAME / NUM"),

    /** Number of different satellites in the clock data records. */
    NB_OF_SOLN_SATS("# OF SOLN SATS"),

    /** List of all satellites reported in this file. */
    PRN_LIST("PRN LIST");

    /** Label. */
    private final String[] labels;

    /** Simple constructor.
     * <p>
     * There may be several labels allowed, as some receiver generate files with typos…
     * Only the first label is considered official
     * </p>
     * @param labels labels
     */
    ClockLabel(final String... labels) {
        this.labels = labels.clone();
    }

    /** {@inheritDoc} */
    @Override
    public boolean matches(final String label) {
        for (String allowed : labels) {
            if (allowed.equals(label)) {
                return true;
            }
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public String getLabel() {
        return labels[0];
    }

}
