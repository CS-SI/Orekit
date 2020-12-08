/* Copyright 2002-2020 CS GROUP
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

import org.hipparchus.exception.Localizable;

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

    // CHECKSTYLE: stop JavadocVariable check

    INTERNAL_ERROR("internal error, please notify development team by creating an issue at {0}"),
    ALTITUDE_BELOW_ALLOWED_THRESHOLD("altitude ({0} m) is below the {1} m allowed threshold"),
    POINT_INSIDE_ELLIPSOID("point is inside ellipsoid"),
    TRAJECTORY_INSIDE_BRILLOUIN_SPHERE("trajectory inside the Brillouin sphere (r = {0})"),
    ALMOST_EQUATORIAL_ORBIT("almost equatorial orbit (i = {0} degrees)"),
    ALMOST_CRITICALLY_INCLINED_ORBIT("almost critically inclined orbit (i = {0} degrees)"),
    UNABLE_TO_COMPUTE_ECKSTEIN_HECHLER_MEAN_PARAMETERS(
            "unable to compute Eckstein-Hechler mean parameters after {0} iterations"),
    NULL_PARENT_FOR_FRAME("null parent for frame {0}"),
    FRAME_ALREADY_ATTACHED("frame {0} is already attached to frame {1}"),
    FRAME_NOT_ATTACHED("frame {0} is not attached to the main frames tree"),
    FRAME_ANCESTOR_OF_BOTH_FRAMES("frame {0} is an ancestor of both frames {1} and {2}"),
    FRAME_ANCESTOR_OF_NEITHER_FRAME("frame {0} is an ancestor of neither frame {1} nor {2}"),
    FRAME_NO_NTH_ANCESTOR("frame {0} has depth {1}, it cannot have an ancestor {2} levels above"),
    NO_SUCH_ITRF_FRAME("ITRF frame {0} not found"),
    UNSUPPORTED_LOCAL_ORBITAL_FRAME("unsupported local orbital frame, supported types: {0} and {1}"),
    NON_PSEUDO_INERTIAL_FRAME("non pseudo-inertial frame \"{0}\""),
    DATA_ROOT_DIRECTORY_DOES_NOT_EXIST("data root directory {0} does not exist"),
    NOT_A_DIRECTORY("{0} is not a directory"),
    NEITHER_DIRECTORY_NOR_ZIP_OR_JAR("{0} is neither a directory nor a zip/jar archive file"),
    UNABLE_TO_FIND_RESOURCE("unable to find resource {0} in classpath"),
    NO_EARTH_ORIENTATION_PARAMETERS_LOADED("no Earth Orientation Parameters loaded"),
    MISSING_EARTH_ORIENTATION_PARAMETERS_BETWEEN_DATES("missing Earth Orientation Parameters between {0} and {1}"),
    NO_EARTH_ORIENTATION_PARAMETERS("missing Earth Orientation Parameters"),
    NOT_A_SUPPORTED_IERS_DATA_FILE("file {0} is not a supported IERS data file"),
    INCONSISTENT_DATES_IN_IERS_FILE("inconsistent dates in IERS file {0}: {1}-{2}-{3} and MJD {4}"),
    UNEXPECTED_DATA_AFTER_LINE_IN_FILE("unexpected data after line {0} in file {1}: {2}"),
    NON_CHRONOLOGICAL_DATES_IN_FILE("non-chronological dates in file {0}, line {1}"),
    NO_IERS_UTC_TAI_HISTORY_DATA_LOADED("no IERS UTC-TAI history data loaded"),
    NO_ENTRIES_IN_IERS_UTC_TAI_HISTORY_FILE("no entries found in IERS UTC-TAI history file {0}"),
    MISSING_SERIE_J_IN_FILE("missing serie j = {0} in file {1} (line {2})"),
    CANNOT_PARSE_BOTH_TAU_AND_GAMMA("cannot parse both τ and γ from the same Poissons series file"),
    UNEXPECTED_END_OF_FILE_AFTER_LINE("unexpected end of file {0} (after line {1})"),
    UNABLE_TO_PARSE_LINE_IN_FILE("unable to parse line {0} of file {1}:\n{2}"),
    UNABLE_TO_FIND_FILE("unable to find file {0}"),
    SPACECRAFT_MASS_BECOMES_NEGATIVE("spacecraft mass becomes negative: {0} kg"),
    POSITIVE_FLOW_RATE("positive flow rate (q: {0})"), NO_GRAVITY_FIELD_DATA_LOADED("no gravity field data loaded"),
    GRAVITY_FIELD_NORMALIZATION_UNDERFLOW("gravity field normalization underflow for degree {0} and order {1}"),
    NO_OCEAN_TIDE_DATA_LOADED("no ocean tide data loaded"),
    OCEAN_TIDE_DATA_DEGREE_ORDER_LIMITS("ocean tide data file {0} limited to degree {1} and order {2}"),
    OCEAN_TIDE_LOAD_DEFORMATION_LIMITS(
            "load deformation coefficients limited to degree {0}, cannot parse degree {1} term from file {2}"),
    POLAR_TRAJECTORY("polar trajectory (distance to polar axis: {0})"),
    UNEXPECTED_FILE_FORMAT_ERROR_FOR_LOADER("unexpected format error for file {0} with loader {1}"),
    DUPLICATED_GRAVITY_FIELD_COEFFICIENT_IN_FILE("duplicated gravity field coefficient {0}({1}, {2}) in file {3}"),
    MISSING_GRAVITY_FIELD_COEFFICIENT_IN_FILE("missing gravity field coefficient {0}({1}, {2}) in file {3}"),
    TOO_LARGE_DEGREE_FOR_GRAVITY_FIELD("too large degree (n = {0}, potential maximal degree is {1})"),
    TOO_LARGE_ORDER_FOR_GRAVITY_FIELD("too large order (m = {0}, potential maximal order is {1})"),
    SEVERAL_REFERENCE_DATES_IN_GRAVITY_FIELD("several reference dates ({0} and {1}) found in gravity field file {2}"),
    NO_TLE_FOR_OBJECT("no TLE data available for object {0}"),
    NO_TLE_FOR_LAUNCH_YEAR_NUMBER_PIECE(
            "no TLE data available for launch year {0}, launch number {1}, launch piece {2}"),
    NOT_TLE_LINES("lines {0} and {1} are not TLE lines:\n{0}: \"{2}\"\n{1}: \"{3}\""),
    MISSING_SECOND_TLE_LINE("expected a second TLE line after line {0}:\n{0}: \"{1}\""),
    TLE_LINES_DO_NOT_REFER_TO_SAME_OBJECT("TLE lines do not refer to the same object:\n{0}\n{1}"),
    TLE_INVALID_PARAMETER("invalid TLE parameter for object {0}: {1} = {2}"),
    TLE_INVALID_PARAMETER_RANGE("invalid TLE parameter {0}: {1} not in range [{2}, {3}]"),
    TLE_CHECKSUM_ERROR("wrong checksum of TLE line {0}, expected {1} but got {2} ({3})"),
    NO_TLE_DATA_AVAILABLE("no TLE data available"),
    NOT_POSITIVE_SPACECRAFT_MASS("spacecraft mass is not positive: {0} kg"),
    TOO_LARGE_ECCENTRICITY_FOR_PROPAGATION_MODEL("too large eccentricity for propagation model: e = {0}"),
    NO_SOLAR_ACTIVITY_AT_DATE("no solar activity available at {0}, data available only in range [{1}, {2}]"),
    NON_EXISTENT_MONTH("non-existent month {0}"), NON_EXISTENT_YEAR_MONTH_DAY("non-existent date {0}-{1}-{2}"),
    NON_EXISTENT_WEEK_DATE("non-existent week date {0}-W{1}-{2}"), NON_EXISTENT_DATE("non-existent date {0}"),
    NON_EXISTENT_DAY_NUMBER_IN_YEAR("no day number {0} in year {1}"),
    NON_EXISTENT_HMS_TIME("non-existent time {0}:{1}:{2}"), NON_EXISTENT_TIME("non-existent time {0}"),
    OUT_OF_RANGE_SECONDS_NUMBER("out of range seconds number: {0}"),
    OUT_OF_RANGE_SECONDS_NUMBER_DETAIL("out of range seconds number: {0} is not in [{1}, {2})"),
    ANGLE_TYPE_NOT_SUPPORTED("angle type not supported, supported angles: {0}, {1} and {2}"),
    SATELLITE_COLLIDED_WITH_TARGET("satellite collided with target"),
    ATTITUDE_POINTING_LAW_DOES_NOT_POINT_TO_GROUND("attitude pointing law misses ground"),
    TOO_SHORT_TRANSITION_TIME_FOR_ATTITUDES_SWITCH(
            "{0} seconds transition time for attitudes switch is too short, should be longer than {1} seconds"),
    ORBIT_AND_ATTITUDE_DATES_MISMATCH("orbit date ({0}) does not match attitude date ({1})"),
    FRAMES_MISMATCH("frame {0} does not match frame {1}"),
    INITIAL_STATE_NOT_SPECIFIED_FOR_ORBIT_PROPAGATION("initial state not specified for orbit propagation"),
    PROPAGATOR_NOT_IN_EPHEMERIS_GENERATION_MODE("propagator is not in ephemeris generation mode"),
    EVENT_DATE_TOO_CLOSE(
            "event date {0}, greater than {1} minus {3} seconds and smaller than {2} plus {3} seconds, cannot be added"),
    UNABLE_TO_READ_JPL_HEADER("unable to read header record from JPL ephemerides binary file {0}"),
    INCONSISTENT_ASTRONOMICAL_UNIT_IN_FILES(
            "inconsistent values of astronomical unit in JPL ephemerides files: ({0} and {1})"),
    INCONSISTENT_EARTH_MOON_RATIO_IN_FILES(
            "inconsistent values of Earth/Moon mass ratio in JPL ephemerides files: ({0} and {1})"),
    NO_DATA_LOADED_FOR_CELESTIAL_BODY("no data loaded for celestial body {0}"),
    NOT_A_JPL_EPHEMERIDES_BINARY_FILE("file {0} is not a JPL ephemerides binary file"),
    NOT_A_MARSHALL_SOLAR_ACTIVITY_FUTURE_ESTIMATION_FILE(
            "file {0} is not a Marshall Solar Activity Future Estimation (MSAFE) file"),
    NO_JPL_EPHEMERIDES_BINARY_FILES_FOUND("no JPL ephemerides binary files found"),
    OUT_OF_RANGE_BODY_EPHEMERIDES_DATE("out of range date for {0} ephemerides: {1}"),
    OUT_OF_RANGE_EPHEMERIDES_DATE("out of range date for ephemerides: {0}, [{1}, {2}]"),
    UNEXPECTED_TWO_ELEVATION_VALUES_FOR_ONE_AZIMUTH(
            "unexpected two elevation values: {0} and {1}, for one azimuth: {2}"),
    UNSUPPORTED_PARAMETER_NAME("unsupported parameter name {0}, supported names: {1}"),
    TOO_SMALL_SCALE_FOR_PARAMETER("scale factor for parameter {0} is too small: {1}"),
    UNKNOWN_ADDITIONAL_STATE("unknown additional state \"{0}\""), UNKNOWN_MONTH("unknown month \"{0}\""),
    SINGULAR_JACOBIAN_FOR_ORBIT_TYPE("Jacobian matrix for type {0} is singular with current orbit"),
    STATE_JACOBIAN_NOT_INITIALIZED("state Jacobian has not been initialized yet"),
    STATE_JACOBIAN_NOT_6X6("state Jacobian is a {0}x{1} matrix, it should be a 6x6 matrix"),
    STATE_AND_PARAMETERS_JACOBIANS_ROWS_MISMATCH("state Jacobian has {0} rows but parameters Jacobian has {1} rows"),
    INITIAL_MATRIX_AND_PARAMETERS_NUMBER_MISMATCH(
            "initial Jacobian matrix has {0} columns, but {1} parameters have been selected"),
    ORBIT_A_E_MISMATCH_WITH_CONIC_TYPE(
            "orbit should be either elliptic with a > 0 and e < 1 or hyperbolic with a < 0 and e > 1, a = {0}, e = {1}"),
    ORBIT_ANOMALY_OUT_OF_HYPERBOLIC_RANGE("true anomaly {0} out of hyperbolic range (e = {1}, {2} < v < {3})"),
    HYPERBOLIC_ORBIT_NOT_HANDLED_AS("hyperbolic orbits cannot be handled as {0} instances"),
    CCSDS_DATE_INVALID_PREAMBLE_FIELD("invalid preamble field in CCSDS date: {0}"),
    CCSDS_DATE_INVALID_LENGTH_TIME_FIELD("invalid time field length in CCSDS date: {0}, expected {1}"),
    CCSDS_DATE_MISSING_AGENCY_EPOCH("missing agency epoch in CCSDS date"),
    CCSDS_UNEXPECTED_KEYWORD("unexpected keyword in CCSDS line number {0} of file {1}:\n{2}"),
    CCSDS_UNKNOWN_GM("the central body gravitational coefficient cannot be retrieved from the ODM"),
    CCSDS_UNKNOWN_SPACECRAFT_MASS("there is no spacecraft mass associated with this ODM file"),
    CCSDS_UNKNOWN_CONVENTIONS("no IERS conventions have been set before parsing"),
    CCSDS_INVALID_FRAME("frame {0} is not valid in this ODM file context"),
    CCSDS_OEM_INCONSISTENT_TIME_SYSTEMS("inconsistent time systems in the ephemeris blocks: {0} ≠ {1}"),
    CCSDS_TIME_SYSTEM_NOT_IMPLEMENTED(
            "use of time system {0} in CCSDS files requires an additional ICD and is not implemented in Orekit"),
    CCSDS_NO_CORRESPONDING_TIME_SCALE("the CCSDS time system {0} has no corresponding Orekit TimeScale."),
    CCSDS_TDM_INCONSISTENT_TIME_SYSTEMS("Inconsistent time systems in the observations blocks: {0} ≠ {1}"),
    CCSDS_TDM_INCONSISTENT_DATA_LINE("Inconsistent data line in TDM file at line {0} of file {1}.\n" +
            "A TDM data line should be as follows \"keyword = epoch value\".\n" + "Whereas read data line is: {2}"),
    CCSDS_TDM_XML_INCONSISTENT_DATA_BLOCK("Inconsistent XML observation block at line {0} of TDM file {1}.\n" +
            "A TDM observation block should be as follows\n\t<observation>\n\t\t<EPOCH>epoch</EPOCH>\n" +
            "\t\t<KEYWORD>value</KEYWORD>\n\t</observation>"),
    CCSDS_TDM_KEYWORD_NOT_FOUND("No CCSDS TDM keyword was found at line {0} of file {1}:\n{2}"),
    CCSDS_TIME_SYSTEM_NOT_READ_YET("Parameter {0} needs a time system to be interpreted"),
    CCSDS_TDM_UNKNOWN_FORMAT("TDM file {0} format is unknown. Please specify a file format: KEYVALUE or XML"),
    CCSDS_AEM_INCONSISTENT_TIME_SYSTEMS("inconsistent time systems in the attitude blocks: {0} ≠ {1}"),
    CCSDS_AEM_NULL_ATTITUDE_TYPE("invalid attitude type {0}"),
    CCSDS_AEM_ATTITUDE_TYPE_NOT_IMPLEMENTED("attitude type {0} in CCSDS AEM files is not implemented in Orekit"),
    CCSDS_AEM_INVALID_ROTATION_SEQUENCE("the rotation sequence of the Euler angles {0} is invalid"),
    ADDITIONAL_STATE_NAME_ALREADY_IN_USE("name \"{0}\" is already used for an additional state"),
    NON_RESETABLE_STATE("reset state not allowed"),
    DSST_NEWCOMB_OPERATORS_COMPUTATION("Cannot compute Newcomb operators for sigma > rho ({0} > {1})"),
    DSST_VMNS_COEFFICIENT_ERROR_MS("Cannot compute the Vmns coefficient with m > n ({0} > {1})"),
    DSST_SPR_SHADOW_INCONSISTENT("inconsistent shadow computation: entry = {0} whereas exit = {1}"),
    DSST_ECC_NO_NUMERICAL_AVERAGING_METHOD(
            "The current orbit has an eccentricity ({0} > 0.5). DSST needs an unimplemented time dependent numerical method to compute the averaged rates"),
    SP3_UNSUPPORTED_VERSION("unsupported sp3 file version {0}"),
    SP3_NUMBER_OF_EPOCH_MISMATCH("found {0} epochs in file {1}, expected {2}"),
    SP3_UNEXPECTED_END_OF_FILE("unexpected end of sp3 file (after line {0})"),
    NON_EXISTENT_GEOMAGNETIC_MODEL("non-existent geomagnetic model {0} for year {1}"),
    UNSUPPORTED_TIME_TRANSFORM(
            "geomagnetic model {0} with epoch {1} does not support time transformation, no secular variation coefficients defined"),
    OUT_OF_RANGE_TIME_TRANSFORM(
            "time transformation of geomagnetic model {0} with epoch {1} is outside its validity range: {2} != [{3}, {4}]"),
    NOT_ENOUGH_DATA_FOR_INTERPOLATION("not enough data for interpolation (sample size = {0})"),
    NOT_ENOUGH_CACHED_NEIGHBORS("too small number of cached neighbors: {0} (must be at least {1})"),
    NO_CACHED_ENTRIES("no cached entries"),
    NON_CHRONOLOGICALLY_SORTED_ENTRIES("generated entries not sorted: {0} > {1}"),
    NO_DATA_GENERATED("no data generated around date: {0}"),
    UNABLE_TO_GENERATE_NEW_DATA_BEFORE("unable to generate new data before {0}, data requested for {1}"),
    UNABLE_TO_GENERATE_NEW_DATA_AFTER("unable to generate new data after {0}, data requested for {1}"),
    UNABLE_TO_COMPUTE_HYPERBOLIC_ECCENTRIC_ANOMALY(
            "unable to compute hyperbolic eccentric anomaly from the mean anomaly after {0} iterations"),
    UNABLE_TO_COMPUTE_DSST_MEAN_PARAMETERS("unable to compute mean orbit from osculating orbit after {0} iterations"),
    OUT_OF_RANGE_DERIVATION_ORDER("derivation order {0} is out of range"),
    ORBIT_TYPE_NOT_ALLOWED("orbit type {0} not allowed here, allowed types: {1}"),
    NON_PSEUDO_INERTIAL_FRAME_NOT_SUITABLE_AS_REFERENCE_FOR_INERTIAL_FORCES(
            "non pseudo-inertial frame {0} is not suitable as reference for inertial forces"),
    METHOD_NOT_AVAILABLE_WITHOUT_CENTRAL_BODY("method not available in the absence of a central body"),
    INCOMPATIBLE_FRAMES("operation not available between frames {0} and {1}"),
    UNDEFINED_ORBIT("orbit not defined, state rather contains an absolute position-velocity-acceleration"),
    UNDEFINED_ABSOLUTE_PVCOORDINATES(
            "absolute position-velocity-acceleration not defined, state rather contains an orbit"),
    INERTIAL_FORCE_MODEL_MISSING("an inertial force model has to be used when propagating in non-inertial frame {0}"),
    NO_SEM_ALMANAC_AVAILABLE("no SEM almanac file found"),
    NOT_A_SUPPORTED_SEM_ALMANAC_FILE("file {0} is not a supported SEM almanac file"),
    NO_YUMA_ALMANAC_AVAILABLE("no Yuma almanac file found"),
    NOT_A_SUPPORTED_YUMA_ALMANAC_FILE("file {0} is not a supported Yuma almanac file"),
    NOT_ENOUGH_GNSS_FOR_DOP("only {0} GNSS orbits are provided while {1} are needed to compute the DOP"),
    NOT_ENOUGH_PROPAGATORS(
            "Creating an aggregate propagator requires at least one constituent propagator, but none were provided."),
    NOT_ENOUGH_ATTITUDE_PROVIDERS(
                    "Creating an aggregate attitude provider requires at least one constituent attitude provider, but none were provided."),
    NULL_ARGUMENT("argument {0} cannot be null"), VALUE_NOT_FOUND("value {0} not found in {1}"),
    EPHEMERIS_FILE_NO_MULTI_SUPPORT("Ephemeris file format does not support multiple space objects"),
    KLOBUCHAR_ALPHA_BETA_NOT_LOADED("Klobuchar coefficients α or β could not be loaded from {0}"),
    KLOBUCHAR_ALPHA_BETA_NOT_AVAILABLE_FOR_DATE("Klobuchar coefficients α or β not available for date {0}"),
    NO_KLOBUCHAR_ALPHA_BETA_IN_FILE("file {0} does not contain Klobuchar coefficients α or β"),
    NO_REFERENCE_DATE_FOR_PARAMETER("no reference date set for parameter {0}"),
    STATION_NOT_FOUND("station {0} not found, known stations: {1}"),
    UNKNOWN_SATELLITE_SYSTEM("unknown satellite system {0}"),
    UNKNOWN_SATELLITE_ANTENNA_CODE("unknown satellite antenna code {0}"),
    UNSUPPORTED_FREQUENCY_FOR_ANTENNA("frequency {0} is not supported by antenna {1}"),
    CANNOT_FIND_SATELLITE_IN_SYSTEM("cannot find satellite {0} in satellite system {1}"),
    UNKNOWN_RINEX_FREQUENCY("unknown RINEX frequency {0} in file {1}, line {2}"),
    MISMATCHED_FREQUENCIES("mismatched frequencies in file {0}, line {1} (expected {2}, got {3})"),
    WRONG_COLUMNS_NUMBER("wrong number of columns in file {0}, line {1} (expected {2} columns, got {3} columns)"),
    UNSUPPORTED_FILE_FORMAT("unsupported format for file {0}"), INCOMPLETE_HEADER("incomplete header in file {0}"),
    INCONSISTENT_NUMBER_OF_SATS(
            "inconsistent number of satellites in line {0}, file {1}: observation with {2} satellites and number of max satellites is {3}"),
    INCONSISTENT_SATELLITE_SYSTEM(
            "the satellite system {3} from line {0}, file {1} is not consistent with the Rinex Satellite System {2} in header"),
    NO_PROPAGATOR_CONFIGURED("no propagator configured"),
    DIMENSION_INCONSISTENT_WITH_PARAMETERS("dimension {0} is inconsistent with parameters list: {1}"),
    NOT_A_SUPPORTED_UNIX_COMPRESSED_FILE("file {0} is not a supported Unix-compressed file"),
    UNEXPECTED_END_OF_FILE("unexpected end of file {0}"), CORRUPTED_FILE("file {0} is corrupted"),
    VIENNA_ACOEF_OR_ZENITH_DELAY_NOT_LOADED("Vienna coefficients ah or aw or zh or zw could not be loaded from {0}"),
    VIENNA_ACOEF_OR_ZENITH_DELAY_NOT_AVAILABLE_FOR_DATE(
            "Vienna coefficients ah or aw or zh or zw not available for date {0}"),
    NO_VIENNA_ACOEF_OR_ZENITH_DELAY_IN_FILE("file {0} does not contain Vienna coefficients ah, aw, zh or zw"),
    IRREGULAR_OR_INCOMPLETE_GRID("irregular or incomplete grid in file {0}"),
    INVALID_SATELLITE_SYSTEM("invalid satellite system {0}"),
    NO_TEC_DATA_IN_FILE_FOR_DATE("IONEX file {0} does not contain TEC data for date {1}"),
    INCONSISTENT_NUMBER_OF_TEC_MAPS_IN_FILE("number of maps {0} is inconsistent with header specification: {1}"),
    NO_LATITUDE_LONGITUDE_BONDARIES_IN_IONEX_HEADER(
            "file {0} does not contain latitude or longitude bondaries in its header section"),
    NO_EPOCH_IN_IONEX_HEADER("file {0} does not contain epoch of first or last map in its header section"),
    ITRF_VERSIONS_PREFIX_ONLY("The first column of itrf-versions.conf is a plain " +
            "prefix that is matched against the name of each loaded file. It should " +
            "not contain any regular expression syntax or directory components, i.e. " +
            "\"/\" or \"\\\". Actual value: \"{0}\"."),
    CANNOT_COMPUTE_AIMING_AT_SINGULAR_POINT(
            "cannot compute aiming direction at singular point: latitude = {0}, longitude = {1}"),
    STEC_INTEGRATION_DID_NOT_CONVERGE("STEC integration did not converge"),
    MODIP_GRID_NOT_LOADED("MODIP grid not be loaded from {0}"),
    NEQUICK_F2_FM3_NOT_LOADED("NeQuick coefficient f2 or fm3 not be loaded from {0}"),
    NOT_A_SUPPORTED_HATANAKA_COMPRESSED_FILE("file {0} is not a supported Hatanaka-compressed file"),
    CANNOT_COMPUTE_LAGRANGIAN("Cannot compute around {0}"),
    TRAJECTORY_NOT_CROSSING_XZPLANE("The trajectory does not cross XZ Plane, it will not result in a Halo Orbit"),
    MULTIPLE_SHOOTING_UNDERCONSTRAINED(
            "The multiple shooting problem is underconstrained : {0} free variables, {1} constraints"),
    INVALID_MEASUREMENT_TYPES_FOR_COMBINATION_OF_MEASUREMENTS(
            "invalid measurement types {0} and {1} for the combination of measurements {2}"),
    INCOMPATIBLE_FREQUENCIES_FOR_COMBINATION_OF_MEASUREMENTS(
            "frequencies {0} and {1} are incompatibles for the {2} combination"),
    NON_CHRONOLOGICAL_DATES_FOR_OBSERVATIONS("observations {0} and {1} are not in chronological dates"),
    EXCEPTIONAL_DATA_CONTEXT(
            "Use of the ExceptionalDataContext detected. This is typically used to detect developer errors."),
    NON_DIFFERENT_DATES_FOR_OBSERVATIONS("observations {0}, {1} and {2} must have different dates"),
    NON_COPLANAR_POINTS("observations are not in the same plane"),
    INVALID_PARAMETER_RANGE("invalid parameter {0}: {1} not in range [{2}, {3}]"),
    PARAMETER_NOT_SET("The parameter {0} should not be null in {1}"),
    FUNCTION_NOT_IMPLEMENTED("{0} is not implemented"),
    INVALID_TYPE_FOR_FUNCTION("Impossible to execute {0} with {1} set to {2}"),
    NO_DATA_IN_FILE("No data could be parsed from file {0}"),
    CPF_UNEXPECTED_END_OF_FILE("Unexpected end of CPF file (after line {0})"),
    UNEXPECTED_FORMAT_FOR_ILRS_FILE("Unexpected file format. Must be {0} but is {1}"),
    CRD_UNEXPECTED_END_OF_FILE("Unexpected end of CRD file (after line {0})"),
    INVALID_RANGE_INDICATOR_IN_CRD_FILE("Invalid range indicator {0} in CRD file header");

    // CHECKSTYLE: resume JavadocVariable check

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
                if ((translated != null) && (translated.length() > 0) &&
                        (!translated.toLowerCase().contains("missing translation"))) {
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
