/* Copyright 2023 Thales Alenia Space
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

/** Labels for Rinex files.
 * @author Luc Maisonobe
 * @since 12.0
 */
public enum RinexLabels {

    /** Version, file type and satellite system. */
    VERSION("RINEX VERSION / TYPE"),

    /** Generating program and emiting agency. */
    PROGRAM("PGM / RUN BY / DATE"),

    /** Comments. */
    COMMENT("COMMENT"),

    /** Marker name. */
    MARKER_NAME("MARKER NAME"),

    /** Marker number. */
    MARKER_NUMBER("MARKER NUMBER"),

    /** Marker type. */
    MARKER_TYPE("MARKER TYPE"),

    /** Observer agency. */
    OBSERVER_AGENCY("OBSERVER / AGENCY"),

    /** Receiver number, type and version. */
    REC_NB_TYPE_VERS("REC # / TYPE / VERS"),

    /** Antenna number and type. */
    ANT_NB_TYPE("ANT # / TYPE"),

    /** Approximative position. */
    APPROX_POSITION_XYZ("APPROX POSITION XYZ"),

    /** Antenna reference point. */
    ANTENNA_DELTA_H_E_N("ANTENNA: DELTA H/E/N"),

    /** Antenna reference point. */
    ANTENNA_DELTA_X_Y_Z("ANTENNA: DELTA X/Y/Z"),

    /** Antenna phase center. */
    ANTENNA_PHASE_CENTER("ANTENNA: PHASECENTER"),

    /** Antenna bore sight. */
    ANTENNA_B_SIGHT_XYZ("ANTENNA: B.SIGHT XYZ"),

    /** Antenna zero direction. */
    ANTENNA_ZERODIR_AZI("ANTENNA: ZERODIR AZI"),

    /** Antenna zero direction. */
    ANTENNA_ZERODIR_XYZ("ANTENNA: ZERODIR XYZ"),

    /** Wavelength factors. */
    WAVELENGTH_FACT_L1_2("WAVELENGTH FACT L1/2"),

    /** Observations scale factor. */
    OBS_SCALE_FACTOR("OBS SCALE FACTOR"),

    /** Center of mass. */
    CENTER_OF_MASS_XYZ("CENTER OF MASS: XYZ"),

    /** DOI. */
    DOI("DOI"),

    /** Llicense. */
    LICENSE("LICENSE OF USE"),

    /** Station information.*/
    STATION_INFORMATION("STATION INFORMATION"),

    /** Number and types of observations. */
    NB_TYPES_OF_OBSERV("# / TYPES OF OBSERV"),

    /** Number and types of observations. */
    SYS_NB_TYPES_OF_OBSERV("SYS / # / OBS TYPES"),

    /** Unit of signal strength. */
    SIGNAL_STRENGTH_UNIT("SIGNAL STRENGTH UNIT"),

    /** Observation interval. */
    INTERVAL("INTERVAL"),

    /** Time of first observation. */
    TIME_OF_FIRST_OBS("TIME OF FIRST OBS"),

    /** Time of last observation. */
    TIME_OF_LAST_OBS("TIME OF LAST OBS"),

    /** Indicator of receiver clock offset application. */
    RCV_CLOCK_OFFS_APPL("RCV CLOCK OFFS APPL"),

    /** Differential code bias corrections. */
    SYS_DCBS_APPLIED("SYS / DCBS APPLIED"),

    /** Phase center variations corrections. */
    SYS_PCVS_APPLIED("SYS / PCVS APPLIED"),

    /** Scale factor. */
    SYS_SCALE_FACTOR("SYS / SCALE FACTOR"),

    /** Phase shift. */
    SYS_PHASE_SHIFT("SYS / PHASE SHIFT", "SYS / PHASE SHIFTS"),

    /** GLONASS slot and frequency number. */
    GLONASS_SLOT_FRQ_NB("GLONASS SLOT / FRQ #"),

    /** GLONASS phase bias corrections. */
    GLONASS_COD_PHS_BIS("GLONASS COD/PHS/BIS"),

    /** Leap seconds. */
    LEAP_SECONDS("LEAP SECONDS"),

    /** Number of satellites. */
    NB_OF_SATELLITES("# OF SATELLITES"),

    /** PRN and number of observations . */
    PRN_NB_OF_OBS("PRN / # OF OBS"),

    /** End of header. */
    END("END OF HEADER");

    /** Labels. */
    private final String[] labels;

    /** Simple constructor.
     * <p>
     * There may be several labels allowed, as some receiver generate files with typos…
     * Only the first label is considered official
     * </p>
     * @param labels labels
     */
    RinexLabels(final String... labels) {
        this.labels = labels.clone();
    }

    /** Check if label matches.
     * @param label label to check
     * @return true if label matches one of the allowed label
     */
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
