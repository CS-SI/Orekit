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
package org.orekit.files.rinex.section;

/** Label for Rinex files.
 * @author Luc Maisonobe
 * @since 14.0
 */
public enum CommonLabel
    implements Label {

    /** Version, file type and satellite system. */
    VERSION("RINEX VERSION / TYPE"),

    /** Generating program and emiting agency. */
    PROGRAM("PGM / RUN BY / DATE"),

    /** Comments. */
    COMMENT("COMMENT"),

    /** DOI. */
    DOI("DOI"),

    /** Llicense. */
    LICENSE("LICENSE OF USE"),

    /** Station information.*/
    STATION_INFORMATION("STATION INFORMATION"),

    /** Number and types of observations. */
    SYS_NB_TYPES_OF_OBSERV("SYS / # / OBS TYPES"),

    /** Differential code bias corrections. */
    SYS_DCBS_APPLIED("SYS / DCBS APPLIED"),

    /** Phase center variations corrections. */
    SYS_PCVS_APPLIED("SYS / PCVS APPLIED"),

    /** Leap seconds separating UTC and TAI. */
    LEAP_SECONDS("LEAP SECONDS"),

    /** End of header. */
    END("END OF HEADER");

    /** Label. */
    private final String[] labels;

    /** Simple constructor.
     * <p>
     * There may be several labels allowed, as some receiver generate files with typos…
     * Only the first label is considered official
     * </p>
     * @param labels labels
     */
    CommonLabel(final String... labels) {
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
