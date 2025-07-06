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
package org.orekit.files.rinex.navigation;

import org.orekit.files.rinex.section.Label;

/** Label for Rinex files.
 * @author Luc Maisonobe
 * @since 14.0
 */
public enum NavigationLabel implements Label {

    /** Ionosphere parameters A0-A3 (Rinex 2). */
    ION_ALPHA("ION ALPHA"),

    /** Ionosphere parameters B0-B3 (Rinex 2). */
    ION_BETA("ION BETA"),

    /** Almanac parameters to compute time in UTC (Rinex 2). */
    DELTA_UTC("DELTA-UTC: A0,A1,T,W"),

    /** Time of reference for system time corr (Rinex 2). */
    CORR_TO_SYSTEM_TIME("CORR TO SYSTEM TIME"),

    /** Ionospheric correction parameters (Rinex 3). */
    IONOSPHERIC_CORR("IONOSPHERIC CORR"),

    /** Difference between GNSS system time and UTC or other time systems (Rinex 3). */
    TIME_SYSTEM_CORR("TIME SYSTEM CORR"),

    /** Merged file. */
    MERGED_FILE("MERGED FILE");

    /** Label. */
    private final String[] labels;

    /** Simple constructor.
     * <p>
     * There may be several labels allowed, as some receiver generate files with typos…
     * Only the first label is considered official
     * </p>
     * @param labels labels
     */
    NavigationLabel(final String... labels) {
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

    /** Get the first label.
     * @return first label
     */
    public String getLabel() {
        return labels[0];
    }

}
