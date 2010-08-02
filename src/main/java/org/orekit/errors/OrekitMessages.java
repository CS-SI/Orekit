/* Copyright 2010 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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
package org.orekit.errors;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.apache.commons.math.exception.Localizable;

/**
 * Enumeration for localized messages formats.
 * <p>
 * The constants in this enumeration represent the available
 * formats as localized strings. These formats are intended to be
 * localized using simple properties files, using the constant
 * name as the key and the property value as the message format.
 * The source English format is provided in the constants themselves
 * to serve both as a reminder for developers to understand the parameters
 * needed by each format, as a basis for translators to create
 * localized properties files, and as a default format if some
 * translation is missing.
 * </p>
 * @since 2.1
 * @version $Revision$ $Date$
 */
public enum OrekitMessages implements Localizable {

    // CHECKSTYLE: stop MultipleVariableDeclarations
    // CHECKSTYLE: stop JavadocVariable

    INTERNAL_ERROR("internal error, contact maintenance at {0}"),
    ALTITUDE_BELOW_ALLOWED_THRESHOLD("altitude ({0} m) is below the {1} m allowed threshold"),
    TRAJECTORY_INSIDE_BRILLOUIN_SPHERE("trajectory inside the Brillouin sphere (r = {0})"),
    ALMOST_EQUATORIAL_ORBIT("almost equatorial orbit (i = {0} degrees)"),
    ALMOST_CRITICALLY_INCLINED_ORBIT("almost critically inclined orbit (i = {0} degrees)"),
    UNABLE_TO_COMPUTE_ECKSTEIN_HECHLER_MEAN_PARAMETERS("unable to compute Eckstein-Hechler mean parameters after {0} iterations"),
    NULL_PARENT_FOR_FRAME("null parent for frame {0}"),
    FRAME_ANCESTOR_OF_BOTH_FRAMES("frame {0} is an ancestor of both frames {1} and {2}"),
    FRAME_ANCESTOR_OF_NEITHER_FRAME("frame {0} is an ancestor of neither frame {1} nor {2}"),
    UNKONWN_FRAME_TYPE("unknown frame type {0}, known types: {1}, {2}, {3}, {4}, {5}, {6} and {7}"),
    UNSUPPORTED_LOCAL_ORBITAL_FRAME("unsupported local orbital frame, supported types: {0} and {1}"),
    NON_QUASI_INERTIAL_FRAME_NOT_SUITABLE_FOR_DEFINING_ORBITS("non quasi-inertial frame \"{0}\" is not suitable for defining orbits"),
    DATA_ROOT_DIRECTORY_DOESN_NOT_EXISTS("data root directory {0} does not exist"),
    NOT_A_DIRECTORY("{0} is not a directory"),
    NEITHER_DIRECTORY_NOR_ZIP_OR_JAR("{0} is neither a directory nor a zip/jar archive file"),
    NO_EARTH_ORIENTATION_PARAMETERS_LOADED("no Earth Orientation Parameters loaded"),
    MISSING_EARTH_ORIENTATION_PARAMETERS_BETWEEN_DATES("missing Earth Orientation Parameters between {0} and {1}"),
    NOT_A_SUPPORTED_IERS_DATA_FILE("file {0} is not a supported IERS data file"),
    UNEXPECTED_DATA_AFTER_LINE_IN_FILE("unexpected data after line {0} in file {1}: {2}"),
    NON_CHRONOLOGICAL_DATES_IN_FILE("non-chronological dates in file {0}, line {1}"),
    NO_IERS_UTC_TAI_HISTORY_DATA_LOADED("no IERS UTC-TAI history data loaded"),
    NO_ENTRIES_IN_IERS_UTC_TAI_HISTORY_FILE("no entries found in IERS UTC-TAI history file {0}"),
    MISSING_SERIE_J_IN_FILE("missing serie j = {0} in file {1} (line {2})"),
    UNEXPECTED_END_OF_FILE_AFTER_LINE("unexpected end of file {0} (after line {1})"),
    UNABLE_TO_PARSE_LINE_IN_FILE("unable to parse line {0} of file {1}:\n{2}"),
    UNABLE_TO_FIND_FILE("unable to find file {0}"),
    UNABLE_TO_FIND_RESOURCE("unable to find resource {0} in classpath"),
    NOT_POSITIVE_SPACECRAFT_MASS("spacecraft mass is not positive: {0} kg"),
    SPACECRAFT_MASS_BECOMES_NEGATIVE("spacecraft mass becomes negative: {0} kg"),
    ORBIT_BECOMES_HYPERBOLIC_UNABLE_TO_PROPAGATE_FURTHER("orbit becomes hyperbolic, unable to propagate it further (e: {0})"),
    POSITIVE_FLOW_RATE("positive flow rate (q: {0})"),
    NO_GRAVITY_FIELD_DATA_LOADED("no gravity field data loaded"),
    POTENTIAL_ARRAYS_SIZES_MISMATCH("potential arrays sizes mismatch (C: {0}x{1}, S: {2}x{3})"),
    POLAR_TRAJECTORY("polar trajectory (distance to polar axis: {0})"),
    UNEXPECTED_FILE_FORMAT_ERROR_FOR_LOADER("unexpect format error for file {0} with loader {1}"),
    TOO_LARGE_DEGREE_FOR_GRAVITY_FIELD("too large degree (n = {0}, potential maximal degree is {1})"),
    TOO_LARGE_ORDER_FOR_GRAVITY_FIELD("too large order (m = {0}, potential maximal order is {1})"),
    NO_TLE_DATA_AVAILABLE("no TLE data available"),
    NO_TLE_FOR_OBJECT("no TLE data available for object {0}"),
    NO_TLE_FOR_LAUNCH_YEAR_NUMBER_PIECE("no TLE data available for launch year {0}, launch number {1}, launch piece {2}"),
    NOT_TLE_LINES("lines {0} and {1} are not TLE lines:\n{0}: \"{2}\"\n{1}: \"{3}\""),
    MISSING_SECOND_TLE_LINE("expected a second TLE line after line {0}:\n{0}: \"{1}\""),
    TLE_LINES_DO_NOT_REFER_TO_SAME_OBJECT("TLE lines do not refer to the same object:\n{0}\n{1}"),
    TLE_CHECKSUM_ERROR("wrong checksum of TLE line {0}, expected {1} but got {2} ({3})"),
    TOO_LARGE_ECCENTRICITY_FOR_PROPAGATION_MODEL("too large eccentricity for propagation model: e = {0}"),
    NO_SOLAR_ACTIVITY_AT_DATE("no solar activity available at {0}, data available only in range [{1}, {2}]"),
    NON_EXISTENT_MONTH("non-existent month {0}"),
    NON_EXISTENT_YEAR_MONTH_DAY("non-existent date {0}-{1}-{2}"),
    NON_EXISTENT_WEEK_DATE("non-existent week date {0}-W{1}-{2}"),
    NON_EXISTENT_DATE("non-existent date {0}"),
    NON_EXISTENT_DAY_NUMBER_IN_YEAR("no day number {0} in year {1}"),
    NON_EXISTENT_HMS_TIME("non-existent time {0}:{1}:{2}"),
    NON_EXISTENT_TIME("non-existent time {0}"),
    OUT_OF_RANGE_SECONDS_NUMBER("out of range seconds number: {0}"),
    ANGLE_TYPE_NOT_SUPPORTED("angle type not supported, supported angles: {0}, {1} and {2}"),
    SATELLITE_COLLIDED_WITH_TARGET("satellite collided with target"),
    ATTITUDE_POINTING_LAW_DOES_NOT_POINT_TO_GROUND("attitude pointing law misses ground"),
    ORBIT_AND_ATTITUDE_DATES_MISMATCH("orbit date ({0}) does not match attitude date ({1})"),
    ORBIT_AND_ATTITUDE_FRAMES_MISMATCH("orbit reference frame ({0}) does not match attitude reference frame ({1})"),
    INITIAL_STATE_NOT_SPECIFIED_FOR_ORBIT_PROPAGATION("initial state not specified for orbit propagation"),
    ODE_INTEGRATOR_NOT_SET_FOR_ORBIT_PROPAGATION("ODE integrator not set for orbit propagation"),
    PROPAGATOR_NOT_IN_EPHEMERIS_GENERATION_MODE("propagator is not in ephemeris generation mode"),
    EVENT_DATE_TOO_CLOSE("event date {0}, greater than {1} minus {3} seconds and smaller than {2} plus {3} seconds, cannot be added"),
    UNABLE_TO_READ_JPL_HEADER("unable to read header record from JPL ephemerides binary file {0}"),
    INCONSISTENT_ASTRONOMICAL_UNIT_IN_FILES("inconsistent values of astronomical unit in JPL ephemerides files: ({0} and {1})"),
    INCONSISTENT_EARTH_MOON_RATIO_IN_FILES("inconsistent values of Earth/Moon mass ratio in JPL ephemerides files: ({0} and {1})"),
    NO_DATA_LOADED_FOR_CELESTIAL_BODY("no data loaded for celestial body {0}"),
    NOT_A_JPL_EPHEMERIDES_BINARY_FILE("file {0} is not a JPL ephemerides binary file"),
    NO_JPL_EPHEMERIDES_BINARY_FILES_FOUND("no JPL ephemerides binary files found"),
    OUT_OF_RANGE_BODY_EPHEMERIDES_DATE("out of range date for {0} ephemerides: {1}"),
    OUT_OF_RANGE_EPHEMERIDES_DATE("out of range date for ephemerides: {0}, [{1}, {2}]"),
    UNKNOWN_PARAMETER("unknown parameter {0}");

    // CHECKSTYLE: resume JavadocVariable
    // CHECKSTYLE: resume MultipleVariableDeclarations


    /** Source English format. */
    private final String sourceFormat;

    /** Simple constructor.
     * @param sourceFormat source English format to use when no
     * localized version is available
     */
    private OrekitMessages(final String sourceFormat) {
        this.sourceFormat = sourceFormat;
    }

    /** {@inheritDoc} */
    public String getSourceString() {
        return sourceFormat;
    }

    /** {@inheritDoc} */
    public String getLocalizedString(final Locale locale) {
        try {
            ResourceBundle bundle =
                    ResourceBundle.getBundle("META-INF/localization/OrekitMessages", locale);
            if (bundle.getLocale().getLanguage().equals(locale.getLanguage())) {
                // the value of the resource is the translated format
                return bundle.getString(toString());
            }

        } catch (MissingResourceException mre) {
            // do nothing here
        }

        // either the locale is not supported or the resource is unknown
        // don't translate and fall back to using the source format
        return sourceFormat;

    }

}
