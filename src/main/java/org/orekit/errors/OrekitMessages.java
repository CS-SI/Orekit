/* Copyright 2002-2023 CS GROUP
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
package org.orekit.errors;

import org.hipparchus.exception.Localizable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

/**
 * Enumeration for localized messages formats.
 * <p>
 * The constants in this enumeration represent the available formats as
 * localized strings. These formats are intended to be localized using simple
 * properties files, using the constant name as the key and the property value
 * as the message format. The source English format is provided in the constants
 * themselves to serve both as a reminder for developers to understand the
 * parameters needed by each format, as a basis for translators to create
 * localized properties files, and as a default format if some translation is
 * missing.
 * </p>
 * @since 2.1
 */
public enum OrekitMessages implements Localizable {

    /** INTERNAL_ERROR. */
    INTERNAL_ERROR("internal error, please notify development team by creating a new topic at {0}"),

    /** ALTITUDE_BELOW_ALLOWED_THRESHOLD. */
    ALTITUDE_BELOW_ALLOWED_THRESHOLD("altitude ({0} m) is below the {1} m allowed threshold"),

    /** POINT_INSIDE_ELLIPSOID. */
    POINT_INSIDE_ELLIPSOID("point is inside ellipsoid"),

    /** TRAJECTORY_INSIDE_BRILLOUIN_SPHERE. */
    TRAJECTORY_INSIDE_BRILLOUIN_SPHERE("trajectory inside the Brillouin sphere (r = {0})"),

    /** ALMOST_EQUATORIAL_ORBIT. */
    ALMOST_EQUATORIAL_ORBIT("almost equatorial orbit (i = {0} degrees)"),

    /** ALMOST_CRITICALLY_INCLINED_ORBIT. */
    ALMOST_CRITICALLY_INCLINED_ORBIT("almost critically inclined orbit (i = {0} degrees)"),

    /** UNABLE_TO_COMPUTE_ECKSTEIN_HECHLER_MEAN_PARAMETERS. */
    UNABLE_TO_COMPUTE_ECKSTEIN_HECHLER_MEAN_PARAMETERS("unable to compute Eckstein-Hechler mean parameters after {0} iterations"),

    /** UNABLE_TO_COMPUTE_BROUWER_LYDDANE_MEAN_PARAMETERS. */
    UNABLE_TO_COMPUTE_BROUWER_LYDDANE_MEAN_PARAMETERS("unable to compute Brouwer-Lyddane mean parameters after {0} iterations"),

    /** UNABLE_TO_COMPUTE_TLE. */
    UNABLE_TO_COMPUTE_TLE("unable to compute TLE after {0} iterations"),

    /** NULL_PARENT_FOR_FRAME. */
    NULL_PARENT_FOR_FRAME("null parent for frame {0}"),

    /** FRAME_ALREADY_ATTACHED. */
    FRAME_ALREADY_ATTACHED("frame {0} is already attached to frame {1}"),

    /** FRAME_NOT_ATTACHED. */
    FRAME_NOT_ATTACHED("frame {0} is not attached to the main frames tree"),

    /** FRAME_ANCESTOR_OF_BOTH_FRAMES. */
    FRAME_ANCESTOR_OF_BOTH_FRAMES("frame {0} is an ancestor of both frames {1} and {2}"),

    /** FRAME_ANCESTOR_OF_NEITHER_FRAME. */
    FRAME_ANCESTOR_OF_NEITHER_FRAME("frame {0} is an ancestor of neither frame {1} nor {2}"),

    /** FRAME_NO_NTH_ANCESTOR. */
    FRAME_NO_NTH_ANCESTOR("frame {0} has depth {1}, it cannot have an ancestor {2} levels above"),

    /** NO_SUCH_ITRF_FRAME. */
    NO_SUCH_ITRF_FRAME("ITRF frame {0} not found"),

    /** UNSUPPORTED_LOCAL_ORBITAL_FRAME. */
    UNSUPPORTED_LOCAL_ORBITAL_FRAME("unsupported local orbital frame {0}"),

    /** NON_PSEUDO_INERTIAL_FRAME. */
    NON_PSEUDO_INERTIAL_FRAME("non pseudo-inertial frame \"{0}\""),

    /** DATA_ROOT_DIRECTORY_DOES_NOT_EXIST. */
    DATA_ROOT_DIRECTORY_DOES_NOT_EXIST("data root directory {0} does not exist"),

    /** NOT_A_DIRECTORY. */
    NOT_A_DIRECTORY("{0} is not a directory"),

    /** NEITHER_DIRECTORY_NOR_ZIP_OR_JAR. */
    NEITHER_DIRECTORY_NOR_ZIP_OR_JAR("{0} is neither a directory nor a zip/jar archive file"),

    /** UNABLE_TO_FIND_RESOURCE. */
    UNABLE_TO_FIND_RESOURCE("unable to find resource {0} in classpath"),

    /** NO_EARTH_ORIENTATION_PARAMETERS_LOADED. */
    NO_EARTH_ORIENTATION_PARAMETERS_LOADED("no Earth Orientation Parameters loaded"),

    /** MISSING_EARTH_ORIENTATION_PARAMETERS_BETWEEN_DATES. */
    MISSING_EARTH_ORIENTATION_PARAMETERS_BETWEEN_DATES("missing Earth Orientation Parameters between {0} and {1}"),

    /** MISSING_EARTH_ORIENTATION_PARAMETERS_BETWEEN_DATES_GAP. */
    MISSING_EARTH_ORIENTATION_PARAMETERS_BETWEEN_DATES_GAP("missing Earth Orientation Parameters between {0} and {1}, gap is {2,number,0.0##############E0} s"),

    /** NO_EARTH_ORIENTATION_PARAMETERS. */
    NO_EARTH_ORIENTATION_PARAMETERS("missing Earth Orientation Parameters"),

    /** NOT_A_SUPPORTED_IERS_DATA_FILE. */
    NOT_A_SUPPORTED_IERS_DATA_FILE("file {0} is not a supported IERS data file"),

    /** INCONSISTENT_DATES_IN_IERS_FILE. */
    INCONSISTENT_DATES_IN_IERS_FILE("inconsistent dates in IERS file {0}: {1}-{2}-{3} and MJD {4}"),

    /** UNEXPECTED_DATA_AFTER_LINE_IN_FILE. */
    UNEXPECTED_DATA_AFTER_LINE_IN_FILE("unexpected data after line {0} in file {1}: {2}"),

    /** UNEXPECTED_DATA_AT_LINE_IN_FILE. */
    UNEXPECTED_DATA_AT_LINE_IN_FILE("unexpected data at line {0} in file {1}"),

    /** NON_CHRONOLOGICAL_DATES_IN_FILE. */
    NON_CHRONOLOGICAL_DATES_IN_FILE("non-chronological dates in file {0}, line {1}"),

    /** NO_IERS_UTC_TAI_HISTORY_DATA_LOADED. */
    NO_IERS_UTC_TAI_HISTORY_DATA_LOADED("no IERS UTC-TAI history data loaded"),

    /** NO_ENTRIES_IN_IERS_UTC_TAI_HISTORY_FILE. */
    NO_ENTRIES_IN_IERS_UTC_TAI_HISTORY_FILE("no entries found in IERS UTC-TAI history file {0}"),

    /** MISSING_SERIE_J_IN_FILE. */
    MISSING_SERIE_J_IN_FILE("missing serie j = {0} in file {1} (line {2})"),

    /** CANNOT_PARSE_BOTH_TAU_AND_GAMMA. */
    CANNOT_PARSE_BOTH_TAU_AND_GAMMA("cannot parse both τ and γ from the same Poissons series file"),

    /** UNEXPECTED_END_OF_FILE_AFTER_LINE. */
    UNEXPECTED_END_OF_FILE_AFTER_LINE("unexpected end of file {0} (after line {1})"),

    /** UNABLE_TO_PARSE_LINE_IN_FILE. */
    UNABLE_TO_PARSE_LINE_IN_FILE("unable to parse line {0} of file {1}:\n{2}"),

    /** UNABLE_TO_PARSE_ELEMENT_IN_FILE. */
    UNABLE_TO_PARSE_ELEMENT_IN_FILE("unable to parse element {0} at line {1}, file {2}"),

    /** UNABLE_TO_FIND_FILE. */
    UNABLE_TO_FIND_FILE("unable to find file {0}"),

    /** POSITIVE_FLOW_RATE. */
    POSITIVE_FLOW_RATE("positive flow rate (q: {0})"),

    /** NO_GRAVITY_FIELD_DATA_LOADED. */
    NO_GRAVITY_FIELD_DATA_LOADED("no gravity field data loaded"),

    /** GRAVITY_FIELD_NORMALIZATION_UNDERFLOW. */
    GRAVITY_FIELD_NORMALIZATION_UNDERFLOW("gravity field normalization underflow for degree {0} and order {1}"),

    /** NO_OCEAN_TIDE_DATA_LOADED. */
    NO_OCEAN_TIDE_DATA_LOADED("no ocean tide data loaded"),

    /** OCEAN_TIDE_DATA_DEGREE_ORDER_LIMITS. */
    OCEAN_TIDE_DATA_DEGREE_ORDER_LIMITS("ocean tide data file {0} limited to degree {1} and order {2}"),

    /** OCEAN_TIDE_LOAD_DEFORMATION_LIMITS. */
    OCEAN_TIDE_LOAD_DEFORMATION_LIMITS("load deformation coefficients limited to degree {0}, cannot parse degree {1} term from file {2}"),

    /** POLAR_TRAJECTORY. */
    POLAR_TRAJECTORY("polar trajectory (distance to polar axis: {0})"),

    /** UNEXPECTED_FILE_FORMAT_ERROR_FOR_LOADER. */
    UNEXPECTED_FILE_FORMAT_ERROR_FOR_LOADER("unexpected format error for file {0} with loader {1}"),

    /** DUPLICATED_GRAVITY_FIELD_COEFFICIENT_IN_FILE. */
    DUPLICATED_GRAVITY_FIELD_COEFFICIENT_IN_FILE("duplicated gravity field coefficient {0}({1}, {2}) in file {3}"),

    /** MISSING_GRAVITY_FIELD_COEFFICIENT_IN_FILE. */
    MISSING_GRAVITY_FIELD_COEFFICIENT_IN_FILE("missing gravity field coefficient {0}({1}, {2}) in file {3}"),

    /** TOO_LARGE_DEGREE_FOR_GRAVITY_FIELD. */
    TOO_LARGE_DEGREE_FOR_GRAVITY_FIELD("too large degree (n = {0}, potential maximal degree is {1})"),

    /** TOO_LARGE_ORDER_FOR_GRAVITY_FIELD. */
    TOO_LARGE_ORDER_FOR_GRAVITY_FIELD("too large order (m = {0}, potential maximal order is {1})"),

    /** WRONG_DEGREE_OR_ORDER. */
    WRONG_DEGREE_OR_ORDER("no term ({0}, {1}) in a {2}x{3} spherical harmonics decomposition"),

    /** SEVERAL_REFERENCE_DATES_IN_GRAVITY_FIELD. */
    SEVERAL_REFERENCE_DATES_IN_GRAVITY_FIELD("several reference dates ({0} and {1} differ by {3,number,0.0##############E0} s) found in gravity field file {2}"),

    /** NO_TLE_FOR_OBJECT. */
    NO_TLE_FOR_OBJECT("no TLE data available for object {0}"),

    /** NO_TLE_FOR_LAUNCH_YEAR_NUMBER_PIECE. */
    NO_TLE_FOR_LAUNCH_YEAR_NUMBER_PIECE("no TLE data available for launch year {0}, launch number {1}, launch piece {2}"),

    /** NOT_TLE_LINES. */
    NOT_TLE_LINES("lines {0} and {1} are not TLE lines:\n{0}: \"{2}\"\n{1}: \"{3}\""),

    /** MISSING_SECOND_TLE_LINE. */
    MISSING_SECOND_TLE_LINE("expected a second TLE line after line {0}:\n{0}: \"{1}\""),

    /** TLE_LINES_DO_NOT_REFER_TO_SAME_OBJECT. */
    TLE_LINES_DO_NOT_REFER_TO_SAME_OBJECT("TLE lines do not refer to the same object:\n{0}\n{1}"),

    /** TLE_INVALID_PARAMETER. */
    TLE_INVALID_PARAMETER("invalid TLE parameter for object {0}: {1} = {2}"),

    /** TLE_CHECKSUM_ERROR. */
    TLE_CHECKSUM_ERROR("wrong checksum of TLE line {0}, expected {1} but got {2} ({3})"),

    /** NO_TLE_DATA_AVAILABLE. */
    NO_TLE_DATA_AVAILABLE("no TLE data available"),

    /** NOT_POSITIVE_SPACECRAFT_MASS. */
    NOT_POSITIVE_SPACECRAFT_MASS("spacecraft mass is not positive: {0} kg"),

    /** TOO_LARGE_ECCENTRICITY_FOR_PROPAGATION_MODEL. */
    TOO_LARGE_ECCENTRICITY_FOR_PROPAGATION_MODEL("too large eccentricity for propagation model: e = {0}"),

    /** NO_SOLAR_ACTIVITY_AT_DATE. */
    NO_SOLAR_ACTIVITY_AT_DATE("no solar activity available at {0}, data available only in range [{1}, {2}]"),

    /** NON_EXISTENT_MONTH. */
    NON_EXISTENT_MONTH("non-existent month {0}"),

    /** NON_EXISTENT_YEAR_MONTH_DAY. */
    NON_EXISTENT_YEAR_MONTH_DAY("non-existent date {0}-{1}-{2}"),

    /** NON_EXISTENT_WEEK_DATE. */
    NON_EXISTENT_WEEK_DATE("non-existent week date {0}-W{1}-{2}"),

    /** NON_EXISTENT_DATE. */
    NON_EXISTENT_DATE("non-existent date {0}"),

    /** NON_EXISTENT_DAY_NUMBER_IN_YEAR. */
    NON_EXISTENT_DAY_NUMBER_IN_YEAR("no day number {0} in year {1}"),

    /** NON_EXISTENT_HMS_TIME. */
    NON_EXISTENT_HMS_TIME("non-existent time {0}:{1}:{2}"),

    /** NON_EXISTENT_TIME. */
    NON_EXISTENT_TIME("non-existent time {0}"),

    /** OUT_OF_RANGE_SECONDS_NUMBER. */
    OUT_OF_RANGE_SECONDS_NUMBER("out of range seconds number: {0}"),

    /** OUT_OF_RANGE_SECONDS_NUMBER_DETAIL. */
    OUT_OF_RANGE_SECONDS_NUMBER_DETAIL("out of range seconds number: {0} is not in [{1}, {2}]"),

    /** ANGLE_TYPE_NOT_SUPPORTED. */
    ANGLE_TYPE_NOT_SUPPORTED("angle type not supported, supported angles: {0}, {1} and {2}"),

    /** SATELLITE_COLLIDED_WITH_TARGET. */
    SATELLITE_COLLIDED_WITH_TARGET("satellite collided with target"),

    /** ATTITUDE_POINTING_LAW_DOES_NOT_POINT_TO_GROUND. */
    ATTITUDE_POINTING_LAW_DOES_NOT_POINT_TO_GROUND("attitude pointing law misses ground"),

    /** TOO_SHORT_TRANSITION_TIME_FOR_ATTITUDES_SWITCH. */
    TOO_SHORT_TRANSITION_TIME_FOR_ATTITUDES_SWITCH("{0} seconds transition time for attitudes switch is too short, should be longer than {1} seconds"),

    /** ORBIT_AND_ATTITUDE_DATES_MISMATCH. */
    ORBIT_AND_ATTITUDE_DATES_MISMATCH("orbit date ({0}) does not match attitude date ({1})"),

    /** FRAMES_MISMATCH. */
    FRAMES_MISMATCH("frame {0} does not match frame {1}"),

    /** INITIAL_STATE_NOT_SPECIFIED_FOR_ORBIT_PROPAGATION. */
    INITIAL_STATE_NOT_SPECIFIED_FOR_ORBIT_PROPAGATION("initial state not specified for orbit propagation"),

    /** EVENT_DATE_TOO_CLOSE. */
    EVENT_DATE_TOO_CLOSE("target event date must be before {1} by {3,number,0.0##############E0} seconds or after {2} by {3,number,0.0##############E0} seconds, but target event date {0} is {4,number,0.0##############E0} seconds before {1} and {5,number,0.0##############E0} seconds after {2} so it cannot be added"),

    /** UNABLE_TO_READ_JPL_HEADER. */
    UNABLE_TO_READ_JPL_HEADER("unable to read header record from JPL ephemerides binary file {0}"),

    /** INCONSISTENT_ASTRONOMICAL_UNIT_IN_FILES. */
    INCONSISTENT_ASTRONOMICAL_UNIT_IN_FILES("inconsistent values of astronomical unit in JPL ephemerides files: ({0} and {1})"),

    /** INCONSISTENT_EARTH_MOON_RATIO_IN_FILES. */
    INCONSISTENT_EARTH_MOON_RATIO_IN_FILES("inconsistent values of Earth/Moon mass ratio in JPL ephemerides files: ({0} and {1})"),

    /** NO_DATA_LOADED_FOR_CELESTIAL_BODY. */
    NO_DATA_LOADED_FOR_CELESTIAL_BODY("no data loaded for celestial body {0}"),

    /** NOT_A_JPL_EPHEMERIDES_BINARY_FILE. */
    NOT_A_JPL_EPHEMERIDES_BINARY_FILE("file {0} is not a JPL ephemerides binary file"),

    /** NOT_A_MARSHALL_SOLAR_ACTIVITY_FUTURE_ESTIMATION_FILE. */
    NOT_A_MARSHALL_SOLAR_ACTIVITY_FUTURE_ESTIMATION_FILE("file {0} is not a Marshall Solar Activity Future Estimation (MSAFE) file"),

    /** NO_JPL_EPHEMERIDES_BINARY_FILES_FOUND. */
    NO_JPL_EPHEMERIDES_BINARY_FILES_FOUND("no JPL ephemerides binary files found"),

    /** OUT_OF_RANGE_BODY_EPHEMERIDES_DATE. */
    OUT_OF_RANGE_BODY_EPHEMERIDES_DATE("out of range date for {0} ephemerides: {1}"),

    /** OUT_OF_RANGE_DATE. */
    OUT_OF_RANGE_DATE("out of range date: {0}, [{1}, {2}]"),

    /** OUT_OF_RANGE_EPHEMERIDES_DATE_BEFORE. */
    OUT_OF_RANGE_EPHEMERIDES_DATE_BEFORE("out of range date for ephemerides: {0} is {3,number,0.0##############E0} s before [{1}, {2}]"),

    /** OUT_OF_RANGE_EPHEMERIDES_DATE_AFTER. */
    OUT_OF_RANGE_EPHEMERIDES_DATE_AFTER("out of range date for ephemerides: {0} is {3,number,0.0##############E0} s after [{1}, {2}]"),

    /** UNEXPECTED_TWO_ELEVATION_VALUES_FOR_ONE_AZIMUTH. */
    UNEXPECTED_TWO_ELEVATION_VALUES_FOR_ONE_AZIMUTH("unexpected two elevation values: {0} and {1}, for one azimuth: {2}"),

    /** UNSUPPORTED_PARAMETER_NAME. */
    UNSUPPORTED_PARAMETER_NAME("unsupported parameter name {0}, supported names: {1}"),

    /** PARAMETER_WITH_SEVERAL_ESTIMATED_VALUES. */
    PARAMETER_WITH_SEVERAL_ESTIMATED_VALUES("{0} parameter contains several span in its value TimeSpanMap, the {1} method must be called"),

    /** PARAMETER_PERIODS_HAS_ALREADY_BEEN_SET. */
    PARAMETER_PERIODS_HAS_ALREADY_BEEN_SET("setPeriod was already called once on {0} parameter, another parameter should be created if the periods have to be changed"),

    /** TOO_SMALL_SCALE_FOR_PARAMETER. */
    TOO_SMALL_SCALE_FOR_PARAMETER("scale factor for parameter {0} is too small: {1}"),

    /** UNKNOWN_ADDITIONAL_STATE. */
    UNKNOWN_ADDITIONAL_STATE("unknown additional state \"{0}\""),

    /** UNKNOWN_MONTH. */
    UNKNOWN_MONTH("unknown month \"{0}\""),

    /** SINGULAR_JACOBIAN_FOR_ORBIT_TYPE. */
    SINGULAR_JACOBIAN_FOR_ORBIT_TYPE("Jacobian matrix for type {0} is singular with current orbit"),

    /** STATE_JACOBIAN_NOT_INITIALIZED. */
    STATE_JACOBIAN_NOT_INITIALIZED("state Jacobian has not been initialized yet"),

    /** STATE_JACOBIAN_NOT_6X6. */
    STATE_JACOBIAN_NOT_6X6("state Jacobian is a {0}x{1} matrix, it should be a 6x6 matrix"),

    /** STATE_AND_PARAMETERS_JACOBIANS_ROWS_MISMATCH. */
    STATE_AND_PARAMETERS_JACOBIANS_ROWS_MISMATCH("state Jacobian has {0} rows but parameters Jacobian has {1} rows"),

    /** INITIAL_MATRIX_AND_PARAMETERS_NUMBER_MISMATCH. */
    INITIAL_MATRIX_AND_PARAMETERS_NUMBER_MISMATCH("initial Jacobian matrix has {0} columns, but {1} parameters have been selected"),

    /** ORBIT_A_E_MISMATCH_WITH_CONIC_TYPE. */
    ORBIT_A_E_MISMATCH_WITH_CONIC_TYPE("orbit should be either elliptic with a > 0 and e < 1 or hyperbolic with a < 0 and e > 1, a = {0}, e = {1}"),

    /** ORBIT_ANOMALY_OUT_OF_HYPERBOLIC_RANGE. */
    ORBIT_ANOMALY_OUT_OF_HYPERBOLIC_RANGE("true anomaly {0} out of hyperbolic range (e = {1}, {2} < v < {3})"),

    /** HYPERBOLIC_ORBIT_NOT_HANDLED_AS. */
    HYPERBOLIC_ORBIT_NOT_HANDLED_AS("hyperbolic orbits cannot be handled as {0} instances"),

    /** CCSDS_DATE_INVALID_PREAMBLE_FIELD. */
    CCSDS_DATE_INVALID_PREAMBLE_FIELD("invalid preamble field in CCSDS date: {0}"),

    /** CCSDS_DATE_INVALID_LENGTH_TIME_FIELD. */
    CCSDS_DATE_INVALID_LENGTH_TIME_FIELD("invalid time field length in CCSDS date: {0}, expected {1}"),

    /** CCSDS_DATE_MISSING_AGENCY_EPOCH. */
    CCSDS_DATE_MISSING_AGENCY_EPOCH("missing agency epoch in CCSDS date"),

    /** CCSDS_MISSING_KEYWORD. */
    CCSDS_MISSING_KEYWORD("missing mandatory key {0} in CCSDS file {1}"),

    /** CCSDS_KEYWORD_NOT_ALLOWED_IN_VERSION. */
    CCSDS_KEYWORD_NOT_ALLOWED_IN_VERSION("key {0} is not allowed in format version {1}"),

    /** CCSDS_UNEXPECTED_KEYWORD. */
    CCSDS_UNEXPECTED_KEYWORD("unexpected keyword in CCSDS line number {0} of file {1}:\n{2}"),

    /** CCSDS_UNKNOWN_GM. */
    CCSDS_UNKNOWN_GM("the central body gravitational coefficient cannot be retrieved from the ODM"),

    /** CCSDS_UNKNOWN_SPACECRAFT_MASS. */
    CCSDS_UNKNOWN_SPACECRAFT_MASS("there is no spacecraft mass associated with this ODM file"),

    /** CCSDS_UNKNOWN_CONVENTIONS. */
    CCSDS_UNKNOWN_CONVENTIONS("no IERS conventions have been set before parsing"),

    /** CCSDS_INVALID_FRAME. */
    CCSDS_INVALID_FRAME("frame {0} is not valid in this CCSDS file context"),

    /** CCSDS_DIFFERENT_LVLH_DEFINITION. */
    CCSDS_DIFFERENT_LVLH_DEFINITION("this LVLH local orbital frame uses a different definition, please use LVLH_CCSDS instead"),

    /** CCSDS_INCONSISTENT_TIME_SYSTEMS. */
    CCSDS_INCONSISTENT_TIME_SYSTEMS("inconsistent time systems: {0} ≠ {1}"),

    /** CCSDS_TIME_SYSTEM_NOT_IMPLEMENTED. */
    CCSDS_TIME_SYSTEM_NOT_IMPLEMENTED("use of time system {0} in CCSDS files requires an additional ICD and is not implemented in Orekit"),

    /** CCSDS_TDM_KEYWORD_NOT_FOUND. */
    CCSDS_TDM_KEYWORD_NOT_FOUND("No CCSDS TDM keyword was found at line {0} of file {1}:\n{2}"),

    /** CCSDS_TDM_MISSING_RANGE_UNITS_CONVERTER. */
    CCSDS_TDM_MISSING_RANGE_UNITS_CONVERTER("no Range Units converter configured for parsing Tracking Data Message"),

    /** CCSDS_TIME_SYSTEM_NOT_READ_YET. */
    CCSDS_TIME_SYSTEM_NOT_READ_YET("Time system should have already been set before line {0} of file {1}"),

    /** CCSDS_UNKNOWN_ATTITUDE_TYPE. */
    CCSDS_UNKNOWN_ATTITUDE_TYPE("unknown attitude type {0}"),

    /** CCSDS_INCOMPLETE_DATA. */
    CCSDS_INCOMPLETE_DATA("incomplete data"),

    /** CCSDS_INVALID_ROTATION_SEQUENCE. */
    CCSDS_INVALID_ROTATION_SEQUENCE("invalid rotation sequence {0} at line {1} of file {2}"),

    /** CCSDS_UNSUPPORTED_ELEMENT_SET_TYPE. */
    CCSDS_UNSUPPORTED_ELEMENT_SET_TYPE("element set type {0} ({1}) is not supported yet"),

    /** CCSDS_UNSUPPORTED_RETROGRADE_EQUINOCTIAL. */
    CCSDS_UNSUPPORTED_RETROGRADE_EQUINOCTIAL("retrograde factor not supported in element set {0}"),

    /** CCSDS_MANEUVER_UNITS_WRONG_NB_COMPONENTS. */
    CCSDS_MANEUVER_UNITS_WRONG_NB_COMPONENTS("wrong number of units for maneuver {0}"),

    /** CCSDS_MANEUVER_MISSING_TIME. */
    CCSDS_MANEUVER_MISSING_TIME("missing time field for maneuver {0}"),

    /** CCSDS_INCONSISTENT_NUMBER_OF_ATTITUDE_STATES. */
    CCSDS_INCONSISTENT_NUMBER_OF_ATTITUDE_STATES("attitude type {0} and rate type {1} calls for {2} states, got {3}"),

    /** CCSDS_INCOMPATIBLE_KEYS_BOTH_USED. */
    CCSDS_INCOMPATIBLE_KEYS_BOTH_USED("incompatible keys {0} and {1} should not both be used"),

    /** CCSDS_SENSOR_INDEX_ALREADY_USED. */
    CCSDS_SENSOR_INDEX_ALREADY_USED("sensor index {0} is already used"),

    /** CCSDS_MISSING_SENSOR_INDEX. */
    CCSDS_MISSING_SENSOR_INDEX("missing sensor index {0}"),

    /** INCONSISTENT_NUMBER_OF_ELEMENTS. */
    INCONSISTENT_NUMBER_OF_ELEMENTS("inconsistent number of elements: expected {0}, got {1}"),

    /** CANNOT_ESTIMATE_PRECESSION_WITHOUT_PROPER_DERIVATIVES. */
    CANNOT_ESTIMATE_PRECESSION_WITHOUT_PROPER_DERIVATIVES("cannot estimate precession without proper derivatives"),

    /** ADDITIONAL_STATE_NAME_ALREADY_IN_USE. */
    ADDITIONAL_STATE_NAME_ALREADY_IN_USE("name \"{0}\" is already used for an additional state"),

    /** NON_RESETABLE_STATE. */
    NON_RESETABLE_STATE("reset state not allowed"),

    /** DSST_NEWCOMB_OPERATORS_COMPUTATION. */
    DSST_NEWCOMB_OPERATORS_COMPUTATION("Cannot compute Newcomb operators for sigma > rho ({0} > {1})"),

    /** DSST_VMNS_COEFFICIENT_ERROR_MS. */
    DSST_VMNS_COEFFICIENT_ERROR_MS("Cannot compute the Vmns coefficient with m > n ({0} > {1})"),

    /** DSST_SPR_SHADOW_INCONSISTENT. */
    DSST_SPR_SHADOW_INCONSISTENT("inconsistent shadow computation: entry = {0} whereas exit = {1}"),

    /** DSST_ECC_NO_NUMERICAL_AVERAGING_METHOD. */
    DSST_ECC_NO_NUMERICAL_AVERAGING_METHOD("The current orbit has an eccentricity ({0} > 0.5). DSST needs an unimplemented time dependent numerical method to compute the averaged rates"),

    /** SP3_UNSUPPORTED_VERSION. */
    SP3_UNSUPPORTED_VERSION("unsupported sp3 file version \"{0}\""),

    /** SP3_INVALID_HEADER_ENTRY. */
    SP3_INVALID_HEADER_ENTRY("invalid header entry {0} \"{1}\" in file {2} (format version {3})"),

    /** SP3_TOO_MANY_SATELLITES_FOR_VERSION. */
    SP3_TOO_MANY_SATELLITES_FOR_VERSION("version \"{0}\" supports only up to {1} satellites, found {2} in file {3}"),

    /** SP3_NUMBER_OF_EPOCH_MISMATCH. */
    SP3_NUMBER_OF_EPOCH_MISMATCH("found {0} epochs in file {1}, expected {2}"),

    /** SP3_INCOMPATIBLE_FILE_METADATA. */
    SP3_INCOMPATIBLE_FILE_METADATA("cannot splice sp3 files with incompatible metadata"),

    /** SP3_INCOMPATIBLE_SATELLITE_MEDATADA. */
    SP3_INCOMPATIBLE_SATELLITE_MEDATADA("cannot splice sp3 files with incompatible satellite metadata for satellite {0}"),

    /** STK_INVALID_OR_UNSUPPORTED_COORDINATE_SYSTEM. */
    STK_INVALID_OR_UNSUPPORTED_COORDINATE_SYSTEM("STK coordinate system \"{0}\" is invalid or not yet supported"),

    /** STK_UNMAPPED_COORDINATE_SYSTEM. */
    STK_UNMAPPED_COORDINATE_SYSTEM("STK coordinate system \"{0}\" has not been mapped to an Orekit frame"),

    /** STK_UNEXPECTED_END_OF_FILE. */
    STK_UNEXPECTED_END_OF_FILE("unexpected end of STK file (after line {0})"),

    /** CLOCK_FILE_UNSUPPORTED_VERSION. */
    CLOCK_FILE_UNSUPPORTED_VERSION("unsupported clock file version {0}"),

    /** UNSUPPORTED_FILE_FORMAT_VERSION. */
    UNSUPPORTED_FILE_FORMAT_VERSION("version {0} from file {1} is not supported, supported version: {2}"),

    /** NON_EXISTENT_GEOMAGNETIC_MODEL. */
    NON_EXISTENT_GEOMAGNETIC_MODEL("non-existent geomagnetic model {0} for year {1}"),

    /** UNSUPPORTED_TIME_TRANSFORM. */
    UNSUPPORTED_TIME_TRANSFORM("geomagnetic model {0} with epoch {1} does not support time transformation, no secular variation coefficients defined"),

    /** OUT_OF_RANGE_TIME_TRANSFORM. */
    OUT_OF_RANGE_TIME_TRANSFORM("time transformation of geomagnetic model {0} with epoch {1} is outside its validity range: {2} != [{3}, {4}]"),

    /** NOT_ENOUGH_DATA. */
    NOT_ENOUGH_DATA("not enough data (sample size = {0})"),

    /** NOT_ENOUGH_CACHED_NEIGHBORS. */
    NOT_ENOUGH_CACHED_NEIGHBORS("too small number of cached neighbors: {0} (must be at least {1})"),

    /** NO_CACHED_ENTRIES. */
    NO_CACHED_ENTRIES("no cached entries"),

    /** NON_CHRONOLOGICALLY_SORTED_ENTRIES. */
    NON_CHRONOLOGICALLY_SORTED_ENTRIES("generated entries not sorted: {0} > {1} by {2,number,0.0##############E0} s"),

    /** NO_DATA_GENERATED. */
    NO_DATA_GENERATED("no data generated around date: {0}"),

    /** UNABLE_TO_GENERATE_NEW_DATA_BEFORE. */
    UNABLE_TO_GENERATE_NEW_DATA_BEFORE("unable to generate new data before {0}, but data is requested for {1} which is {2,number,0.0##############E0} s before"),

    /** UNABLE_TO_GENERATE_NEW_DATA_AFTER. */
    UNABLE_TO_GENERATE_NEW_DATA_AFTER("unable to generate new data after {0}, but data is requested for {1} which is {2,number,0.0##############E0} s after"),

    /** UNABLE_TO_COMPUTE_HYPERBOLIC_ECCENTRIC_ANOMALY. */
    UNABLE_TO_COMPUTE_HYPERBOLIC_ECCENTRIC_ANOMALY("unable to compute hyperbolic eccentric anomaly from the mean anomaly after {0} iterations"),

    /** UNABLE_TO_COMPUTE_DSST_MEAN_PARAMETERS. */
    UNABLE_TO_COMPUTE_DSST_MEAN_PARAMETERS("unable to compute mean orbit from osculating orbit after {0} iterations"),

    /** OUT_OF_RANGE_DERIVATION_ORDER. */
    OUT_OF_RANGE_DERIVATION_ORDER("derivation order {0} is out of range"),

    /** ORBIT_TYPE_NOT_ALLOWED. */
    ORBIT_TYPE_NOT_ALLOWED("orbit type {0} not allowed here, allowed types: {1}"),

    /** NON_PSEUDO_INERTIAL_FRAME_NOT_SUITABLE_AS_REFERENCE_FOR_INERTIAL_FORCES. */
    NON_PSEUDO_INERTIAL_FRAME_NOT_SUITABLE_AS_REFERENCE_FOR_INERTIAL_FORCES("non pseudo-inertial frame {0} is not suitable as reference for inertial forces"),

    /** METHOD_NOT_AVAILABLE_WITHOUT_CENTRAL_BODY. */
    METHOD_NOT_AVAILABLE_WITHOUT_CENTRAL_BODY("method not available in the absence of a central body"),

    /** INCOMPATIBLE_FRAMES. */
    INCOMPATIBLE_FRAMES("operation not available between frames {0} and {1}"),

    /** UNDEFINED_ORBIT. */
    UNDEFINED_ORBIT("orbit not defined, state rather contains an absolute position-velocity-acceleration"),

    /** UNDEFINED_ABSOLUTE_PVCOORDINATES. */
    UNDEFINED_ABSOLUTE_PVCOORDINATES("absolute position-velocity-acceleration not defined, state rather contains an orbit"),

    /** INERTIAL_FORCE_MODEL_MISSING. */
    INERTIAL_FORCE_MODEL_MISSING("an inertial force model has to be used when propagating in non-inertial frame {0}"),

    /** NO_SEM_ALMANAC_AVAILABLE. */
    NO_SEM_ALMANAC_AVAILABLE("no SEM almanac file found"),

    /** NOT_A_SUPPORTED_SEM_ALMANAC_FILE. */
    NOT_A_SUPPORTED_SEM_ALMANAC_FILE("file {0} is not a supported SEM almanac file"),

    /** NO_YUMA_ALMANAC_AVAILABLE. */
    NO_YUMA_ALMANAC_AVAILABLE("no Yuma almanac file found"),

    /** NOT_A_SUPPORTED_YUMA_ALMANAC_FILE. */
    NOT_A_SUPPORTED_YUMA_ALMANAC_FILE("file {0} is not a supported Yuma almanac file"),

    /** NOT_ENOUGH_GNSS_FOR_DOP. */
    NOT_ENOUGH_GNSS_FOR_DOP("only {0} GNSS orbits are provided while {1} are needed to compute the DOP"),

    /** NOT_ENOUGH_PROPAGATORS. */
    NOT_ENOUGH_PROPAGATORS("Creating an aggregate propagator requires at least one constituent propagator, but none were provided."),

    /** NOT_ENOUGH_ATTITUDE_PROVIDERS. */
    NOT_ENOUGH_ATTITUDE_PROVIDERS("Creating an aggregate attitude provider requires at least one constituent attitude provider, but none were provided."),

    /** NULL_ARGUMENT. */
    NULL_ARGUMENT("argument {0} cannot be null"),

    /** VALUE_NOT_FOUND. */
    VALUE_NOT_FOUND("value {0} not found in {1}"),

    /** KLOBUCHAR_ALPHA_BETA_NOT_LOADED. */
    KLOBUCHAR_ALPHA_BETA_NOT_LOADED("Klobuchar coefficients α or β could not be loaded from {0}"),

    /** KLOBUCHAR_ALPHA_BETA_NOT_AVAILABLE_FOR_DATE. */
    KLOBUCHAR_ALPHA_BETA_NOT_AVAILABLE_FOR_DATE("Klobuchar coefficients α or β not available for date {0}"),

    /** NO_KLOBUCHAR_ALPHA_BETA_IN_FILE. */
    NO_KLOBUCHAR_ALPHA_BETA_IN_FILE("file {0} does not contain Klobuchar coefficients α or β"),

    /** NO_REFERENCE_DATE_FOR_PARAMETER. */
    NO_REFERENCE_DATE_FOR_PARAMETER("no reference date set for parameter {0}"),

    /** STATION_NOT_FOUND. */
    STATION_NOT_FOUND("station {0} not found, known stations: {1}"),

    /** UNKNOWN_SATELLITE_SYSTEM. */
    UNKNOWN_SATELLITE_SYSTEM("unknown satellite system {0}"),

    /** UNKNOWN_TIME_SYSTEM. */
    UNKNOWN_TIME_SYSTEM("unknown time system {0}"),

    /** UNKNOWN_UTC_ID. */
    UNKNOWN_UTC_ID("unknown UTC Id {0}"),

    /** UNKNOWN_CLOCK_DATA_TYPE. */
    UNKNOWN_CLOCK_DATA_TYPE("unknown clock data type {0}"),

    /** UNKNOWN_SATELLITE_ANTENNA_CODE. */
    UNKNOWN_SATELLITE_ANTENNA_CODE("unknown satellite antenna code {0}"),

    /** UNSUPPORTED_FREQUENCY_FOR_ANTENNA. */
    UNSUPPORTED_FREQUENCY_FOR_ANTENNA("frequency {0} is not supported by antenna {1}"),

    /** CANNOT_FIND_SATELLITE_IN_SYSTEM. */
    CANNOT_FIND_SATELLITE_IN_SYSTEM("cannot find satellite {0} in satellite system {1}"),

    /** UNKNOWN_RINEX_FREQUENCY. */
    UNKNOWN_RINEX_FREQUENCY("unknown RINEX frequency {0} in file {1}, line {2}"),

    /** MISMATCHED_FREQUENCIES. */
    MISMATCHED_FREQUENCIES("mismatched frequencies in file {0}, line {1} (expected {2}, got {3})"),

    /** WRONG_PARSING_TYPE. */
    WRONG_PARSING_TYPE("wrong parsing type for file {0}"),

    /** WRONG_COLUMNS_NUMBER. */
    WRONG_COLUMNS_NUMBER("wrong number of columns in file {0}, line {1} (expected {2} columns, got {3} columns)"),

    /** UNSUPPORTED_FILE_FORMAT. */
    UNSUPPORTED_FILE_FORMAT("unsupported format for file {0}"),

    /** INCOMPLETE_HEADER. */
    INCOMPLETE_HEADER("incomplete header in file {0}"),

    /** INCONSISTENT_NUMBER_OF_SATS. */
    INCONSISTENT_NUMBER_OF_SATS("inconsistent number of satellites in line {0}, file {1}: observation with {2} satellites and number of max satellites is {3}"),

    /** INCONSISTENT_SATELLITE_SYSTEM. */
    INCONSISTENT_SATELLITE_SYSTEM("the satellite system {3} from line {0}, file {1} is not consistent with the Rinex Satellite System {2} in header"),

    /** NO_PROPAGATOR_CONFIGURED. */
    NO_PROPAGATOR_CONFIGURED("no propagator configured"),

    /** DIMENSION_INCONSISTENT_WITH_PARAMETERS. */
    DIMENSION_INCONSISTENT_WITH_PARAMETERS("dimension {0} is inconsistent with parameters list: {1}"),

    /** NOT_A_SUPPORTED_UNIX_COMPRESSED_FILE. */
    NOT_A_SUPPORTED_UNIX_COMPRESSED_FILE("file {0} is not a supported Unix-compressed file"),

    /** UNEXPECTED_END_OF_FILE. */
    UNEXPECTED_END_OF_FILE("unexpected end of file {0}"),

    /** CORRUPTED_FILE. */
    CORRUPTED_FILE("file {0} is corrupted"),

    /** VIENNA_ACOEF_OR_ZENITH_DELAY_NOT_LOADED. */
    VIENNA_ACOEF_OR_ZENITH_DELAY_NOT_LOADED("Vienna coefficients ah or aw or zh or zw could not be loaded from {0}"),

    /** VIENNA_ACOEF_OR_ZENITH_DELAY_NOT_AVAILABLE_FOR_DATE. */
    VIENNA_ACOEF_OR_ZENITH_DELAY_NOT_AVAILABLE_FOR_DATE("Vienna coefficients ah or aw or zh or zw not available for date {0}"),

    /** NO_VIENNA_ACOEF_OR_ZENITH_DELAY_IN_FILE. */
    NO_VIENNA_ACOEF_OR_ZENITH_DELAY_IN_FILE("file {0} does not contain Vienna coefficients ah, aw, zh or zw"),

    /** IRREGULAR_OR_INCOMPLETE_GRID. */
    IRREGULAR_OR_INCOMPLETE_GRID("irregular or incomplete grid in file {0}"),

    /** INVALID_SATELLITE_SYSTEM. */
    INVALID_SATELLITE_SYSTEM("invalid satellite system {0}"),

    /** NO_TEC_DATA_IN_FILES_FOR_DATE. */
    NO_TEC_DATA_IN_FILES_FOR_DATE("IONEX files {0} does not contain TEC data for date {1}"),

    /** INCONSISTENT_NUMBER_OF_TEC_MAPS_IN_FILE. */
    INCONSISTENT_NUMBER_OF_TEC_MAPS_IN_FILE("number of maps {0} is inconsistent with header specification: {1}"),

    /** NO_LATITUDE_LONGITUDE_BONDARIES_IN_IONEX_HEADER. */
    NO_LATITUDE_LONGITUDE_BONDARIES_IN_IONEX_HEADER("file {0} does not contain latitude or longitude bondaries in its header section"),

    /** NO_EPOCH_IN_IONEX_HEADER. */
    NO_EPOCH_IN_IONEX_HEADER("file {0} does not contain epoch of first or last map in its header section"),

    /** ITRF_VERSIONS_PREFIX_ONLY. */
    ITRF_VERSIONS_PREFIX_ONLY("The first column of itrf-versions.conf is a plain prefix that is matched against the name of each loaded file. It should not contain any regular expression syntax or directory components, i.e. \"/\" or \"\\\". Actual value: \"{0}\"."),

    /** CANNOT_COMPUTE_AIMING_AT_SINGULAR_POINT. */
    CANNOT_COMPUTE_AIMING_AT_SINGULAR_POINT("cannot compute aiming direction at singular point: latitude = {0}, longitude = {1}"),

    /** STEC_INTEGRATION_DID_NOT_CONVERGE. */
    STEC_INTEGRATION_DID_NOT_CONVERGE("STEC integration did not converge"),

    /** MODIP_GRID_NOT_LOADED. */
    MODIP_GRID_NOT_LOADED("MODIP grid not be loaded from {0}"),

    /** NEQUICK_F2_FM3_NOT_LOADED. */
    NEQUICK_F2_FM3_NOT_LOADED("NeQuick coefficient f2 or fm3 not be loaded from {0}"),

    /** NOT_A_SUPPORTED_HATANAKA_COMPRESSED_FILE. */
    NOT_A_SUPPORTED_HATANAKA_COMPRESSED_FILE("file {0} is not a supported Hatanaka-compressed file"),

    /** CANNOT_COMPUTE_LAGRANGIAN. */
    CANNOT_COMPUTE_LAGRANGIAN("Cannot compute around {0}"),

    /** TRAJECTORY_NOT_CROSSING_XZPLANE. */
    TRAJECTORY_NOT_CROSSING_XZPLANE("The trajectory does not cross XZ Plane, it will not result in a Halo Orbit"),

    /** MULTIPLE_SHOOTING_UNDERCONSTRAINED. */
    MULTIPLE_SHOOTING_UNDERCONSTRAINED("The multiple shooting problem is underconstrained : {0} free variables, {1} constraints"),

    /** INVALID_MEASUREMENT_TYPES_FOR_COMBINATION_OF_MEASUREMENTS. */
    INVALID_MEASUREMENT_TYPES_FOR_COMBINATION_OF_MEASUREMENTS("invalid measurement types {0} and {1} for the combination of measurements {2}"),

    /** INCOMPATIBLE_FREQUENCIES_FOR_COMBINATION_OF_MEASUREMENTS. */
    INCOMPATIBLE_FREQUENCIES_FOR_COMBINATION_OF_MEASUREMENTS("frequencies {0} and {1} are incompatibles for the {2} combination"),

    /** NON_CHRONOLOGICAL_DATES_FOR_OBSERVATIONS. */
    NON_CHRONOLOGICAL_DATES_FOR_OBSERVATIONS("observations are not in chronological order: {0} is {2,number,0.0##############E0} s after {1}"),

    /** INCONSISTENT_SAMPLING_DATE. */
    INCONSISTENT_SAMPLING_DATE("inconsistent sampling date: expected {0} but got {1}"),

    /** EXCEPTIONAL_DATA_CONTEXT. */
    EXCEPTIONAL_DATA_CONTEXT("Use of the ExceptionalDataContext detected. This is typically used to detect developer errors."),

    /** NON_DIFFERENT_DATES_FOR_OBSERVATIONS. */
    NON_DIFFERENT_DATES_FOR_OBSERVATIONS("Observations must have different dates: {0}, {1} ({3,number,0.0##############E0} s from first observation), and {2} ({4,number,0.0##############E0} s from first observation, {5,number,0.0##############E0} s from second observation)"),

    /** NON_COPLANAR_POINTS. */
    NON_COPLANAR_POINTS("observations are not in the same plane"),

    /** INVALID_PARAMETER_RANGE. */
    INVALID_PARAMETER_RANGE("invalid parameter {0}: {1} not in range [{2}, {3}]"),

    /** PARAMETER_NOT_SET. */
    PARAMETER_NOT_SET("The parameter {0} should not be null in {1}"),

    /** FUNCTION_NOT_IMPLEMENTED. */
    FUNCTION_NOT_IMPLEMENTED("{0} is not implemented"),

    /** INVALID_TYPE_FOR_FUNCTION. */
    INVALID_TYPE_FOR_FUNCTION("Impossible to execute {0} with {1} set to {2}"),

    /** NO_DATA_IN_FILE. */
    NO_DATA_IN_FILE("No data could be parsed from file {0}"),

    /** CPF_UNEXPECTED_END_OF_FILE. */
    CPF_UNEXPECTED_END_OF_FILE("Unexpected end of CPF file (after line {0})"),

    /** UNEXPECTED_FORMAT_FOR_ILRS_FILE. */
    UNEXPECTED_FORMAT_FOR_ILRS_FILE("Unexpected file format. Must be {0} but is {1}"),

    /** CRD_UNEXPECTED_END_OF_FILE. */
    CRD_UNEXPECTED_END_OF_FILE("Unexpected end of CRD file (after line {0})"),

    /** INVALID_RANGE_INDICATOR_IN_CRD_FILE. */
    INVALID_RANGE_INDICATOR_IN_CRD_FILE("Invalid range indicator {0} in CRD file header"),

    /** END_OF_ENCODED_MESSAGE. */
    END_OF_ENCODED_MESSAGE("end of encoded message reached"),

    /** TOO_LARGE_DATA_TYPE. */
    TOO_LARGE_DATA_TYPE("too large data type ({0} bits)"),

    /** UNKNOWN_ENCODED_MESSAGE_NUMBER. */
    UNKNOWN_ENCODED_MESSAGE_NUMBER("unknown encoded message number {0}"),

    /** UNKNOWN_AUTHENTICATION_METHOD. */
    UNKNOWN_AUTHENTICATION_METHOD("unknown authentication method: {0}"),

    /** UNKNOWN_CARRIER_PHASE_CODE. */
    UNKNOWN_CARRIER_PHASE_CODE("unknown carrier phase code: {0}"),

    /** UNKNOWN_DATA_FORMAT. */
    UNKNOWN_DATA_FORMAT("unknown data format: {0}"),

    /** UNKNOWN_NAVIGATION_SYSTEM. */
    UNKNOWN_NAVIGATION_SYSTEM("unknown navigation system: {0}"),

    /** STREAM_REQUIRES_NMEA_FIX. */
    STREAM_REQUIRES_NMEA_FIX("data stream {0} requires a NMEA fix data"),

    /** FAILED_AUTHENTICATION. */
    FAILED_AUTHENTICATION("failed authentication for mountpoint {0}"),

    /** CONNECTION_ERROR. */
    CONNECTION_ERROR("error connecting to {0}: {1}"),

    /** UNEXPECTED_CONTENT_TYPE. */
    UNEXPECTED_CONTENT_TYPE("unexpected content type {0}"),

    /** CANNOT_PARSE_GNSS_DATA. */
    CANNOT_PARSE_GNSS_DATA("cannot parse GNSS data from {0}"),

    /** INVALID_GNSS_DATA. */
    INVALID_GNSS_DATA("invalid GNSS data: {0}"),

    /** GNSS_PARITY_ERROR. */
    GNSS_PARITY_ERROR("GNSS parity error on word {0}"),

    /** UNKNOWN_HOST. */
    UNKNOWN_HOST("unknown host {0}"),

    /** SOURCETABLE_PARSE_ERROR. */
    SOURCETABLE_PARSE_ERROR("error parsing sourcetable line {0} from {1}: {2}"),

    /** CANNOT_PARSE_SOURCETABLE. */
    CANNOT_PARSE_SOURCETABLE("cannot parse sourcetable from {0}"),

    /** MOUNPOINT_ALREADY_CONNECTED. */
    MOUNPOINT_ALREADY_CONNECTED("mount point {0} is already connected"),

    /** MISSING_HEADER. */
    MISSING_HEADER("missing header from {0}: {1}"),

    /** NOT_VALID_INTERNATIONAL_DESIGNATOR. */
    NOT_VALID_INTERNATIONAL_DESIGNATOR("{0} is not a valid international designator"),

    /** UNINITIALIZED_VALUE_FOR_KEY. */
    UNINITIALIZED_VALUE_FOR_KEY("value for key {0} has not been initialized"),

    /** UNKNOWN_UNIT. */
    UNKNOWN_UNIT("unknown unit {0}"),

    /** INCOMPATIBLE_UNITS. */
    INCOMPATIBLE_UNITS("units {0} and {1} are not compatible"),

    /** MISSING_VELOCITY. */
    MISSING_VELOCITY("missing velocity data"),

    /** ATTEMPT_TO_GENERATE_MALFORMED_FILE. */
    ATTEMPT_TO_GENERATE_MALFORMED_FILE("attempt to generate file {0} with a formatting error"),

    /** FIND_ROOT. */
    FIND_ROOT("{0} failed to find root between {1} (g={2,number,0.0##############E0}) and {3} (g={4,number,0.0##############E0})\nLast iteration at {5} (g={6,number,0.0##############E0})"),

    /** MISSING_STATION_DATA_FOR_EPOCH. */
    MISSING_STATION_DATA_FOR_EPOCH("missing station data for epoch {0}"),

    /** INCONSISTENT_SELECTION. */
    INCONSISTENT_SELECTION("inconsistent parameters selection between pairs {0}/{1} and {2}/{3}"),

    /** NO_UNSCENTED_TRANSFORM_CONFIGURED. */
    NO_UNSCENTED_TRANSFORM_CONFIGURED("no unscented transform configured"),

    /** NOT_STRICTLY_POSITIVE. */
    NOT_STRICTLY_POSITIVE("value is not strictly positive: {0}"),

    /** UNSUPPORTED_TRANSFORM. */
    UNSUPPORTED_TRANSFORM("transform from {0} to {1} is not implemented"),

    /** WRONG_ORBIT_PARAMETERS_TYPE. */
    WRONG_ORBIT_PARAMETERS_TYPE("orbital parameters type: {0} is different from expected orbital type : {1}"),

    /** WRONG_NB_COMPONENTS. */
    WRONG_NB_COMPONENTS("{0} expects {1} elements, got {2}"),

    /** CANNOT_CHANGE_COVARIANCE_TYPE_IF_DEFINED_IN_LOF. */
    CANNOT_CHANGE_COVARIANCE_TYPE_IF_DEFINED_IN_LOF("cannot change covariance type if defined in a local orbital frame"),

    /** CANNOT_CHANGE_COVARIANCE_TYPE_IF_DEFINED_IN_NON_INERTIAL_FRAME. */
    CANNOT_CHANGE_COVARIANCE_TYPE_IF_DEFINED_IN_NON_INERTIAL_FRAME("cannot change covariance type if defined in a non pseudo-inertial reference frame"),

    /** DIFFERENT_TIME_OF_CLOSEST_APPROACH. */
    DIFFERENT_TIME_OF_CLOSEST_APPROACH("primary collision object time of closest approach is different from the secondary collision object's one"),

    /** DATES_MISMATCH. */
    DATES_MISMATCH("first date {0} does not match second date {1}"),

    /** ORBITS_MUS_MISMATCH. */
    ORBITS_MUS_MISMATCH("first orbit mu {0} does not match second orbit mu {1}"),

    /** DIFFERENT_STATE_DEFINITION. */
    DIFFERENT_STATE_DEFINITION("one state is defined using an orbit while the other is defined using an absolute position-velocity-acceleration"),

    /** STATE_AND_COVARIANCE_DATES_MISMATCH. */
    STATE_AND_COVARIANCE_DATES_MISMATCH("state date {0} does not match its covariance date {1}"),

    /** NO_INTERPOLATOR_FOR_STATE_DEFINITION. */
    NO_INTERPOLATOR_FOR_STATE_DEFINITION("creating a spacecraft state interpolator requires at least one orbit interpolator or an absolute position-velocity-acceleration interpolator"),

    /** WRONG_INTERPOLATOR_DEFINED_FOR_STATE_INTERPOLATION. */
    WRONG_INTERPOLATOR_DEFINED_FOR_STATE_INTERPOLATION("wrong interpolator defined for this spacecraft state type (orbit or absolute PV)"),

    /** MULTIPLE_INTERPOLATOR_USED. */
    MULTIPLE_INTERPOLATOR_USED("multiple interpolators are used so they may use different numbers of interpolation points"),

    /** HEADER_NOT_WRITTEN. */
    HEADER_NOT_WRITTEN("header for file {0} has not been written yet"),

    /** HEADER_ALREADY_WRITTEN. */
    HEADER_ALREADY_WRITTEN("header for file {0} has already been written"),

    /** CANNOT_START_PROPAGATION_FROM_INFINITY. */
    CANNOT_START_PROPAGATION_FROM_INFINITY("Cannot start the propagation from an infinitely far date"),

    /** INVALID_SATELLITE_ID. */
    INVALID_SATELLITE_ID("invalid satellite id {0}"),

    /** WRONG_EOP_INTERPOLATION_DEGREE. */
    WRONG_EOP_INTERPOLATION_DEGREE("EOP interpolation degree must be of the form 4k-1, got {0}");

    /** Base name of the resource bundle in classpath. */
    private static final String RESOURCE_BASE_NAME = "assets/org/orekit/localization/OrekitMessages";

    /** Source English format. */
    private final String sourceFormat;

    /**
     * Simple constructor.
     * @param sourceFormat source English format to use when no localized version is
     *                     available
     */
    OrekitMessages(final String sourceFormat) {
        this.sourceFormat = sourceFormat;
    }

    /** {@inheritDoc} */
    public String getSourceString() {
        return sourceFormat;
    }

    /** {@inheritDoc} */
    public String getLocalizedString(final Locale locale) {
        try {
            final ResourceBundle bundle = ResourceBundle.getBundle(RESOURCE_BASE_NAME, locale, new UTF8Control());
            if (bundle.getLocale().getLanguage().equals(locale.getLanguage())) {
                final String translated = bundle.getString(name());
                if (translated != null && translated.length() > 0 &&
                        !translated.toLowerCase().contains("missing translation")) {
                    // the value of the resource is the translated format
                    return translated;
                }
            }

        } catch (MissingResourceException mre) {
            // do nothing here
        }

        // either the locale is not supported or the resource is not translated or
        // it is unknown: don't translate and fall back to using the source format
        return sourceFormat;

    }

    /**
     * Control class loading properties in UTF-8 encoding.
     * <p>
     * This class has been very slightly adapted from BalusC answer to question:
     * <a href=
     * "http://stackoverflow.com/questions/4659929/how-to-use-utf-8-in-resource-properties-with-resourcebundle">
     * How to use UTF-8 in resource properties with ResourceBundle</a>.
     * </p>
     * @since 6.0
     */
    public static class UTF8Control extends ResourceBundle.Control {

        /** Empty constructor.
         * <p>
         * This constructor is not strictly necessary, but it prevents spurious
         * javadoc warnings with JDK 18 and later.
         * </p>
         * @since 12.0
         */
        public UTF8Control() {
            // nothing to do
        }

        /** {@inheritDoc} */
        @Override
        public ResourceBundle newBundle(final String baseName, final Locale locale, final String format,
                final ClassLoader loader, final boolean reload)
                throws IllegalAccessException, InstantiationException, IOException {
            // The below is a copy of the default implementation.
            final String bundleName = toBundleName(baseName, locale);
            final String resourceName = toResourceName(bundleName, "utf8");
            ResourceBundle bundle = null;
            InputStream stream = null;
            if (reload) {
                final URL url = loader.getResource(resourceName);
                if (url != null) {
                    final URLConnection connection = url.openConnection();
                    if (connection != null) {
                        connection.setUseCaches(false);
                        stream = connection.getInputStream();
                    }
                }
            } else {
                stream = loader.getResourceAsStream(resourceName);
            }
            if (stream != null) {
                try (InputStreamReader inputStreamReader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    // Only this line is changed to make it to read properties files as UTF-8.
                    bundle = new PropertyResourceBundle(inputStreamReader);
                }
            }
            return bundle;
        }
    }
}
