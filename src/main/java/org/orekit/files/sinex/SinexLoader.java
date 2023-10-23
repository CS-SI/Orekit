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
package org.orekit.files.sinex;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hipparchus.exception.DummyLocalizable;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.data.DataLoader;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.sinex.Station.ReferenceSystem;
import org.orekit.frames.EOPEntry;
import org.orekit.frames.EopHistoryLoader;
import org.orekit.frames.ITRFVersion;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.gnss.TimeSystem;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChronologicalComparator;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScales;
import org.orekit.time.TimeStamped;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.IERSConventions.NutationCorrectionConverter;
import org.orekit.utils.units.Unit;

/**
 * Loader for Solution INdependent EXchange (SINEX) files.
 * <p>
 * The loader can be used to load several data types contained in Sinex files.
 * The current supported data are: station coordinates, site eccentricities, EOP, and Difference Code Bias (DCB).
 * Several instances of Sinex loader must be created in order to parse different data types.
 * </p>
 * <p>
 * The parsing of EOP parameters for multiple files in different SinexLoader object, fed into the default DataContext
 * might pose a problem in case validity dates are overlapping. As Sinex daily solution files provide a single EOP entry,
 * the Sinex loader will add points at the limits of data dates (startDate, endDate) of the Sinex file, which in case of
 * overlap will lead to inconsistencies in the final EOPHistory object. Multiple files can be parsed using a single SinexLoader
 * with a regex to overcome this issue.
 * </p>
 * @author Bryan Cazabonne
 * @since 10.3
 */
public class SinexLoader implements EopHistoryLoader {

    /** Length of day. */
    private static final String LOD = "LOD";

    /** UT1-UTC. */
    private static final String UT = "UT";

    /** X polar motion. */
    private static final String XPO = "XPO";

    /** Y polar motion. */
    private static final String YPO = "YPO";

    /** Nutation correction in longitude. */
    private static final String NUT_LN = "NUT_LN";

    /** Nutation correction in obliquity. */
    private static final String NUT_OB = "NUT_OB";

    /** Nutation correction X. */
    private static final String NUT_X = "NUT_X";

    /** Nutation correction Y. */
    private static final String NUT_Y = "NUT_Y";

    /** 00:000:00000 epoch. */
    private static final String DEFAULT_EPOCH_TWO_DIGITS = "00:000:00000";

    /** 0000:000:00000 epoch. */
    private static final String DEFAULT_EPOCH_FOUR_DIGITS = "0000:000:00000";

    /** Pattern for delimiting regular expressions. */
    private static final Pattern SEPARATOR = Pattern.compile(":");

    /** Pattern for regular data. */
    private static final Pattern PATTERN_SPACE = Pattern.compile("\\s+");

    /** Pattern to check beginning of SINEX files.*/
    private static final Pattern PATTERN_BEGIN = Pattern.compile("%=(?:SNX|BIA) \\d\\.\\d\\d ..." +
                                                                 " (\\d{2,4}:\\d{3}:\\d{5}) ..." +
                                                                 " (\\d{2,4}:\\d{3}:\\d{5}) (\\d{2,4}:\\d{3}:\\d{5})" +
                                                                 " . .*");

    /** List of all EOP parameter types. */
    private static final List<String> EOP_TYPES = Arrays.asList(LOD, UT, XPO, YPO, NUT_LN, NUT_OB, NUT_X, NUT_Y);

    /** Start time of the data used in the Sinex solution.*/
    private AbsoluteDate startDate;

    /** End time of the data used in the Sinex solution.*/
    private AbsoluteDate endDate;

    /** SINEX file creation date as extracted for the first line. */
    private AbsoluteDate creationDate;

    /** Station data.
     * Key: Site code
     */
    private final Map<String, Station> stations;

    /**
     * DCB data.
     * Key: Site code
     */
    private final Map<String, DcbStation> dcbStations;

    /**
     * DCB data.
     * Key: Satellite PRN
     */
    private final Map<String, DcbSatellite> dcbSatellites;

    /** DCB description. */
    private final DcbDescription dcbDescription;

    /** Data set. */
    private Map<AbsoluteDate, SinexEopEntry> eop;

    /** ITRF Version used for EOP parsing. */
    private ITRFVersion itrfVersionEop;

    /** Time scales. */
    private final TimeScales scales;

    /** Simple constructor. This constructor uses the {@link DataContext#getDefault()
     * default data context}.
     * @param supportedNames regular expression for supported files names
     * @see #SinexLoader(String, DataProvidersManager, TimeScales)
     */
    @DefaultDataContext
    public SinexLoader(final String supportedNames) {
        this(supportedNames,
             DataContext.getDefault().getDataProvidersManager(),
             DataContext.getDefault().getTimeScales());
    }

    /**
     * Construct a loader by specifying the source of SINEX auxiliary data files.
     * <p>
     * For EOP loading, a default {@link ITRFVersion#ITRF_2014} is used. It is
     * possible to update the version using the {@link #setITRFVersion(int)}
     * method.
     * </p>
     * @param supportedNames regular expression for supported files names
     * @param dataProvidersManager provides access to auxiliary data.
     * @param scales time scales
     */
    public SinexLoader(final String supportedNames,
                       final DataProvidersManager dataProvidersManager,
                       final TimeScales scales) {
        // Common data
        this.scales         = scales;
        this.creationDate   = AbsoluteDate.FUTURE_INFINITY;
        // DCB parameters
        this.dcbDescription = new DcbDescription();
        this.dcbStations    = new HashMap<>();
        this.dcbSatellites  = new HashMap<>();
        // EOP parameters
        this.eop            = new HashMap<>();
        this.itrfVersionEop = ITRFVersion.ITRF_2014;
        // Station data
        this.stations       = new HashMap<>();

        // Read the file
        dataProvidersManager.feed(supportedNames, new Parser());
    }

    /**
     * Simple constructor. This constructor uses the {@link DataContext#getDefault()
     * default data context}.
     * <p>
     * For EOP loading, a default {@link ITRFVersion#ITRF_2014} is used. It is
     * possible to update the version using the {@link #setITRFVersion(int)}
     * method.
     * </p>
     * @param source source for the RINEX data
     * @see #SinexLoader(String, DataProvidersManager, TimeScales)
     */
    @DefaultDataContext
    public SinexLoader(final DataSource source) {
        this(source, DataContext.getDefault().getTimeScales());
    }

    /**
     * Loads SINEX from the given input stream using the specified auxiliary data.
     * <p>
     * For EOP loading, a default {@link ITRFVersion#ITRF_2014} is used. It is
     * possible to update the version using the {@link #setITRFVersion(int)}
     * method.
     * </p>
     * @param source source for the RINEX data
     * @param scales time scales
     */
    public SinexLoader(final DataSource source, final TimeScales scales) {
        try {
            // Common data
            this.scales         = scales;
            this.creationDate   = AbsoluteDate.FUTURE_INFINITY;
            // EOP data
            this.itrfVersionEop = ITRFVersion.ITRF_2014;
            this.eop            = new HashMap<>();
            // DCB data
            this.dcbStations    = new HashMap<>();
            this.dcbSatellites  = new HashMap<>();
            this.dcbDescription = new DcbDescription();
            // Station data
            this.stations       = new HashMap<>();

            // Read the file
            try (InputStream         is  = source.getOpener().openStreamOnce();
                 BufferedInputStream bis = new BufferedInputStream(is)) {
                new Parser().loadData(bis, source.getName());
            }
        } catch (IOException | ParseException ioe) {
            throw new OrekitException(ioe, new DummyLocalizable(ioe.getMessage()));
        }
    }

    /**
     * Set the ITRF version used in EOP entries processing.
     * @param year Year of the ITRF Version used for parsing EOP.
     * @since 11.2
     */
    public void setITRFVersion(final int year) {
        this.itrfVersionEop = ITRFVersion.getITRFVersion(year);
    }

    /**
     * Get the ITRF version used for the EOP entries processing.
     * @return the ITRF Version used for the EOP processing.
     * @since 11.2
     */
    public ITRFVersion getITRFVersion() {
        return itrfVersionEop;
    }

    /**
     * Get the creation date of the parsed SINEX file.
     * @return SINEX file creation date as an AbsoluteDate
     * @since 12.0
     */
    public AbsoluteDate getCreationDate() {
        return creationDate;
    }

    /**
     * Get the file epoch start time.
     * @return the file epoch start time
     * @since 12.0
     */
    public AbsoluteDate getFileEpochStartTime() {
        return startDate;
    }

    /**
     * Get the file epoch end time.
     * @return the file epoch end time
     * @since 12.0
     */
    public AbsoluteDate getFileEpochEndTime() {
        return endDate;
    }

    /**
     * Get the parsed station data.
     * @return unmodifiable view of parsed station data
     */
    public Map<String, Station> getStations() {
        return Collections.unmodifiableMap(stations);
    }

    /**
     * Get the parsed EOP data.
     * @return unmodifiable view of parsed station data
     * @since 11.2
     */
    public Map<AbsoluteDate, SinexEopEntry> getParsedEop() {
        return Collections.unmodifiableMap(eop);
    }

    /**
     * Get the station corresponding to the given site code.
     *
     * @param siteCode site code
     * @return the corresponding station
     */
    public Station getStation(final String siteCode) {
        return stations.get(siteCode);
    }

    /** {@inheritDoc} */
    @Override
    public void fillHistory(final NutationCorrectionConverter converter,
                            final SortedSet<EOPEntry> history) {
        // Fill the history set with the content of the parsed data
        // According to Sinex standard, data are given in UTC
        history.addAll(getEopList(converter, scales.getUTC()));
    }

    /**
     * Get the DCB data for a given station.
     * @param siteCode site code
     * @return DCB data for the station
     * @since 12.0
     */
    public DcbStation getDcbStation(final String siteCode) {
        return dcbStations.get(siteCode);
    }

    /**
     * Get the DCB data for a given satellite identified by its PRN.
     * @param prn the satellite PRN (e.g. "G01" for GPS 01)
     * @return the DCB data for the satellite
     * @since 12.0
     */
    public DcbSatellite getDcbSatellite(final String prn) {
        return dcbSatellites.get(prn);
    }

    /** Parser for SINEX files. */
    private class Parser implements DataLoader {

        /** Start character of a comment line. */
        private static final String COMMENT = "*";

        /** {@inheritDoc} */
        @Override
        public boolean stillAcceptsData() {
            // We load all SINEX files we can find
            return true;
        }

        /** {@inheritDoc} */
        @Override
        public void loadData(final InputStream input, final String name)
            throws IOException, ParseException {

            // Useful parameters
            int lineNumber            = 0;
            String line               = null;
            boolean inDcbDesc         = false;
            boolean inDcbSol          = false;
            boolean inId              = false;
            boolean inAntenna         = false;
            boolean inEcc             = false;
            boolean inEpoch           = false;
            boolean inEstimate        = false;
            Vector3D position         = Vector3D.ZERO;
            Vector3D velocity         = Vector3D.ZERO;
            String startDateString    = "";
            String endDateString      = "";
            String creationDateString = "";

            // According to Sinex standard, the epochs are given in UTC scale.
            // Except for DCB files for which a TIME_SYSTEM key is present.
            TimeScale scale    = scales.getUTC();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {

                // Loop on lines
                for (line = reader.readLine(); line != null; line = reader.readLine()) {
                    ++lineNumber;
                    // For now, only few keys are supported
                    // They represent the minimum set of parameters that are interesting to consider in a SINEX file
                    // Other keys can be added depending user needs

                    // The first line is parsed in order to get the creation, start and end dates of the file
                    if (lineNumber == 1) {
                        final Matcher matcher = PATTERN_BEGIN.matcher(line);
                        if (matcher.matches()) {

                            creationDateString = matcher.group(1);
                            startDateString    = matcher.group(2);
                            endDateString      = matcher.group(3);
                            creationDate       = stringEpochToAbsoluteDate(creationDateString, false, scale);

                            if (startDate == null) {
                                // First data loading, needs to initialize the start and end dates for EOP history
                                startDate = stringEpochToAbsoluteDate(startDateString, true,  scale);
                                endDate   = stringEpochToAbsoluteDate(endDateString,   false, scale);
                            }
                        } else {
                            throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                      lineNumber, name, line);
                        }
                    } else {
                        switch (line.trim()) {
                            case "+SITE/ID" :
                                // Start of site id. data
                                inId = true;
                                break;
                            case "-SITE/ID" :
                                // End of site id. data
                                inId = false;
                                break;
                            case "+SITE/ANTENNA" :
                                // Start of site antenna data
                                inAntenna = true;
                                break;
                            case "-SITE/ANTENNA" :
                                // End of site antenna data
                                inAntenna = false;
                                break;
                            case "+SITE/ECCENTRICITY" :
                                // Start of antenna eccentricities data
                                inEcc = true;
                                break;
                            case "-SITE/ECCENTRICITY" :
                                // End of antenna eccentricities data
                                inEcc = false;
                                break;
                            case "+SOLUTION/EPOCHS" :
                                // Start of epoch data
                                inEpoch = true;
                                break;
                            case "-SOLUTION/EPOCHS" :
                                // End of epoch data
                                inEpoch = false;
                                break;
                            case "+SOLUTION/ESTIMATE" :
                                // Start of coordinates data
                                inEstimate = true;
                                break;
                            case "-SOLUTION/ESTIMATE" :
                                // End of coordinates data
                                inEstimate = false;
                                break;
                            case "+BIAS/DESCRIPTION" :
                                // Start of Bias description block data
                                inDcbDesc = true;
                                break;
                            case "-BIAS/DESCRIPTION" :
                                // End of Bias description block data
                                inDcbDesc = false;
                                break;
                            case "+BIAS/SOLUTION" :
                                // Start of Bias solution block data
                                inDcbSol = true;
                                break;
                            case "-BIAS/SOLUTION" :
                                // End of Bias solution block data
                                inDcbSol = false;
                                break;
                            default:
                                if (line.startsWith(COMMENT)) {
                                    // ignore that line
                                } else {
                                    // parsing data
                                    if (inId) {
                                        // read site id. data
                                        final Station station = new Station();
                                        station.setSiteCode(parseString(line, 1, 4));
                                        station.setDomes(parseString(line, 9, 9));
                                        // add the station to the map
                                        addStation(station);
                                    } else if (inAntenna) {

                                        // read antenna type data
                                        final Station station = getStation(parseString(line, 1, 4));

                                        final AbsoluteDate start = stringEpochToAbsoluteDate(parseString(line, 16, 12), true, scale);
                                        final AbsoluteDate end   = stringEpochToAbsoluteDate(parseString(line, 29, 12), false, scale);

                                        // antenna type
                                        final String type = parseString(line, 42, 20);

                                        // special implementation for the first entry
                                        if (station.getAntennaTypeTimeSpanMap().getSpansNumber() == 1) {
                                            // we want null values outside validity limits of the station
                                            station.addAntennaTypeValidBefore(type, end);
                                            station.addAntennaTypeValidBefore(null, start);
                                        } else {
                                            station.addAntennaTypeValidBefore(type, end);
                                        }

                                    } else if (inEcc) {

                                        // read antenna eccentricities data
                                        final Station station = getStation(parseString(line, 1, 4));

                                        final AbsoluteDate start = stringEpochToAbsoluteDate(parseString(line, 16, 12), true, scale);
                                        final AbsoluteDate end   = stringEpochToAbsoluteDate(parseString(line, 29, 12), false, scale);

                                        // reference system UNE or XYZ
                                        station.setEccRefSystem(ReferenceSystem.getEccRefSystem(parseString(line, 42, 3)));

                                        // eccentricity vector
                                        final Vector3D eccStation = new Vector3D(parseDouble(line, 46, 8),
                                                                                 parseDouble(line, 55, 8),
                                                                                 parseDouble(line, 64, 8));

                                        // special implementation for the first entry
                                        if (station.getEccentricitiesTimeSpanMap().getSpansNumber() == 1) {
                                            // we want null values outside validity limits of the station
                                            station.addStationEccentricitiesValidBefore(eccStation, end);
                                            station.addStationEccentricitiesValidBefore(null,       start);
                                        } else {
                                            station.addStationEccentricitiesValidBefore(eccStation, end);
                                        }

                                    } else if (inEpoch) {
                                        // read epoch data
                                        final Station station = getStation(parseString(line, 1, 4));
                                        station.setValidFrom(stringEpochToAbsoluteDate(parseString(line, 16, 12), true, scale));
                                        station.setValidUntil(stringEpochToAbsoluteDate(parseString(line, 29, 12), false, scale));
                                    } else if (inEstimate) {
                                        final Station       station     = getStation(parseString(line, 14, 4));
                                        final AbsoluteDate  currentDate = stringEpochToAbsoluteDate(parseString(line, 27, 12), false, scale);
                                        final String        dataType    = parseString(line, 7, 6);
                                        // check if this station exists or if we are parsing EOP
                                        if (station != null || EOP_TYPES.contains(dataType)) {
                                            // switch on coordinates data
                                            switch (dataType) {
                                                case "STAX":
                                                    // station X coordinate
                                                    final double x = parseDouble(line, 47, 22);
                                                    position = new Vector3D(x, position.getY(), position.getZ());
                                                    station.setPosition(position);
                                                    break;
                                                case "STAY":
                                                    // station Y coordinate
                                                    final double y = parseDouble(line, 47, 22);
                                                    position = new Vector3D(position.getX(), y, position.getZ());
                                                    station.setPosition(position);
                                                    break;
                                                case "STAZ":
                                                    // station Z coordinate
                                                    final double z = parseDouble(line, 47, 22);
                                                    position = new Vector3D(position.getX(), position.getY(), z);
                                                    station.setPosition(position);
                                                    // set the reference epoch (identical for all coordinates)
                                                    station.setEpoch(currentDate);
                                                    // reset position vector
                                                    position = Vector3D.ZERO;
                                                    break;
                                                case "VELX":
                                                    // station X velocity (value is in m/y)
                                                    final double vx = parseDouble(line, 47, 22) / Constants.JULIAN_YEAR;
                                                    velocity = new Vector3D(vx, velocity.getY(), velocity.getZ());
                                                    station.setVelocity(velocity);
                                                    break;
                                                case "VELY":
                                                    // station Y velocity (value is in m/y)
                                                    final double vy = parseDouble(line, 47, 22) / Constants.JULIAN_YEAR;
                                                    velocity = new Vector3D(velocity.getX(), vy, velocity.getZ());
                                                    station.setVelocity(velocity);
                                                    break;
                                                case "VELZ":
                                                    // station Z velocity (value is in m/y)
                                                    final double vz = parseDouble(line, 47, 22) / Constants.JULIAN_YEAR;
                                                    velocity = new Vector3D(velocity.getX(), velocity.getY(), vz);
                                                    station.setVelocity(velocity);
                                                    // reset position vector
                                                    velocity = Vector3D.ZERO;
                                                    break;
                                                case XPO:
                                                    // X polar motion
                                                    final double xPo = parseDoubleWithUnit(line, 40, 4, 47, 21);
                                                    getSinexEopEntry(currentDate).setxPo(xPo);
                                                    break;
                                                case YPO:
                                                    // Y polar motion
                                                    final double yPo = parseDoubleWithUnit(line, 40, 4, 47, 21);
                                                    getSinexEopEntry(currentDate).setyPo(yPo);
                                                    break;
                                                case LOD:
                                                    // length of day
                                                    final double lod = parseDoubleWithUnit(line, 40, 4, 47, 21);
                                                    getSinexEopEntry(currentDate).setLod(lod);
                                                    break;
                                                case UT:
                                                    // delta time UT1-UTC
                                                    final double dt = parseDoubleWithUnit(line, 40, 4, 47, 21);
                                                    getSinexEopEntry(currentDate).setUt1MinusUtc(dt);
                                                    break;
                                                case NUT_LN:
                                                    // nutation correction in longitude
                                                    final double nutLn = parseDoubleWithUnit(line, 40, 4, 47, 21);
                                                    getSinexEopEntry(currentDate).setNutLn(nutLn);
                                                    break;
                                                case NUT_OB:
                                                    // nutation correction in obliquity
                                                    final double nutOb = parseDoubleWithUnit(line, 40, 4, 47, 21);
                                                    getSinexEopEntry(currentDate).setNutOb(nutOb);
                                                    break;
                                                case NUT_X:
                                                    // nutation correction X
                                                    final double nutX = parseDoubleWithUnit(line, 40, 4, 47, 21);
                                                    getSinexEopEntry(currentDate).setNutX(nutX);
                                                    break;
                                                case NUT_Y:
                                                    // nutation correction Y
                                                    final double nutY = parseDoubleWithUnit(line, 40, 4, 47, 21);
                                                    getSinexEopEntry(currentDate).setNutY(nutY);
                                                    break;
                                                default:
                                                    // ignore that field
                                                    break;
                                            }
                                        }
                                    } else if (inDcbDesc) {
                                        // Determining the data type for the DCBDescription object
                                        final String[] splitLine = PATTERN_SPACE.split(line.trim());
                                        final String dataType = splitLine[0];
                                        final String data = splitLine[1];
                                        switch (dataType) {
                                            case "OBSERVATION_SAMPLING":
                                                dcbDescription.setObservationSampling(Integer.parseInt(data));
                                                break;
                                            case "PARAMETER_SPACING":
                                                dcbDescription.setParameterSpacing(Integer.parseInt(data));
                                                break;
                                            case "DETERMINATION_METHOD":
                                                dcbDescription.setDeterminationMethod(data);
                                                break;
                                            case "BIAS_MODE":
                                                dcbDescription.setBiasMode(data);
                                                break;
                                            case "TIME_SYSTEM":
                                                if ("UTC".equals(data)) {
                                                    dcbDescription.setTimeSystem(TimeSystem.UTC);
                                                } else if ("TAI".equals(data)) {
                                                    dcbDescription.setTimeSystem(TimeSystem.TAI);
                                                } else {
                                                    dcbDescription.setTimeSystem(TimeSystem.parseOneLetterCode(data));
                                                }
                                                scale = dcbDescription.getTimeSystem().getTimeScale(scales);
                                                // A time scale has been parsed, update start, end, and creation dates
                                                // to take into account the time scale
                                                startDate    = stringEpochToAbsoluteDate(startDateString,    true,  scale);
                                                endDate      = stringEpochToAbsoluteDate(endDateString,      false, scale);
                                                creationDate = stringEpochToAbsoluteDate(creationDateString, false, scale);
                                                break;
                                            default:
                                                break;
                                        }
                                    } else if (inDcbSol) {

                                        // Parsing the data present in a DCB file solution line.
                                        // Most fields are used in the files provided by CDDIS.
                                        // Station is empty for satellite measurements.
                                        // The separator between columns is composed of spaces.

                                        final String satellitePrn = parseString(line, 11, 3);
                                        final String siteCode     = parseString(line, 15, 9);

                                        // Parsing the line data.
                                        final String obs1 = parseString(line, 25, 4);
                                        final String obs2 = parseString(line, 30, 4);
                                        final AbsoluteDate beginDate = stringEpochToAbsoluteDate(parseString(line, 35, 14), true, scale);
                                        final AbsoluteDate finalDate = stringEpochToAbsoluteDate(parseString(line, 50, 14), false, scale);
                                        final Unit unitDcb = Unit.parse(parseString(line, 65, 4));
                                        final double valueDcb = unitDcb.toSI(Double.parseDouble(parseString(line, 70, 21)));

                                        // Verifying if present
                                        if (siteCode.equals("")) {
                                            final String id = satellitePrn;
                                            DcbSatellite dcbSatellite = getDcbSatellite(id);
                                            if (dcbSatellite == null) {
                                                dcbSatellite = new DcbSatellite(id);
                                                dcbSatellite.setDescription(dcbDescription);
                                            }
                                            final Dcb dcb = dcbSatellite.getDcbData();
                                            // Add the data to the DCB object.
                                            dcb.addDcbLine(obs1, obs2, beginDate, finalDate, valueDcb);
                                            // Adding the object to the HashMap if not present.
                                            addDcbSatellite(dcbSatellite, id);
                                        } else {
                                            final String id = siteCode;
                                            DcbStation dcbStation = getDcbStation(id);
                                            if (dcbStation == null) {
                                                dcbStation = new DcbStation(id);
                                                dcbStation.setDescription(dcbDescription);
                                            }
                                            final SatelliteSystem satSystem = SatelliteSystem.parseSatelliteSystem(satellitePrn);
                                            // Add the data to the DCB object.
                                            final Dcb dcb = dcbStation.getDcbData(satSystem);
                                            if (dcb == null) {
                                                dcbStation.addDcb(satSystem, new Dcb());
                                            }
                                            dcbStation.getDcbData(satSystem).addDcbLine(obs1, obs2, beginDate, finalDate, valueDcb);
                                            // Adding the object to the HashMap if not present.
                                            addDcbStation(dcbStation, id);
                                        }

                                    } else {
                                        // not supported line, ignore it
                                    }
                                }
                                break;
                        }
                    }
                }

            } catch (NumberFormatException nfe) {
                throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                        lineNumber, name, line);
            }

        }

        /** Extract a string from a line.
         * @param line to parse
         * @param start start index of the string
         * @param length length of the string
         * @return parsed string
         */
        private String parseString(final String line, final int start, final int length) {
            return line.substring(start, FastMath.min(line.length(), start + length)).trim();
        }

        /** Extract a double from a line.
         * @param line to parse
         * @param start start index of the real
         * @param length length of the real
         * @return parsed real
         */
        private double parseDouble(final String line, final int start, final int length) {
            return Double.parseDouble(parseString(line, start, length));
        }

        /** Extract a double from a line and convert in SI unit.
         * @param line to parse
         * @param startUnit start index of the unit
         * @param lengthUnit length of the unit
         * @param startDouble start index of the real
         * @param lengthDouble length of the real
         * @return parsed double in SI unit
         */
        private double parseDoubleWithUnit(final String line, final int startUnit, final int lengthUnit,
                                           final int startDouble, final int lengthDouble) {
            final Unit unit = Unit.parse(parseString(line, startUnit, lengthUnit));
            return unit.toSI(parseDouble(line, startDouble, lengthDouble));
        }

    }

    /**
     * Transform a String epoch to an AbsoluteDate.
     * @param stringDate string epoch
     * @param isStart true if epoch is a start validity epoch
     * @param scale TimeScale for the computation of the dates
     * @return the corresponding AbsoluteDate
     */
    private AbsoluteDate stringEpochToAbsoluteDate(final String stringDate, final boolean isStart, final TimeScale scale) {

        // Deal with 00:000:00000 epochs
        if (DEFAULT_EPOCH_TWO_DIGITS.equals(stringDate) || DEFAULT_EPOCH_FOUR_DIGITS.equals(stringDate)) {
            // If its a start validity epoch, the file start date shall be used.
            // For end validity epoch, future infinity is acceptable.
            return isStart ? startDate : AbsoluteDate.FUTURE_INFINITY;
        }

        // Date components
        final String[] fields = SEPARATOR.split(stringDate);

        // Read fields
        final int digitsYear = Integer.parseInt(fields[0]);
        final int day        = Integer.parseInt(fields[1]);
        final int secInDay   = Integer.parseInt(fields[2]);

        // Data year
        final int year;
        if (digitsYear > 50 && digitsYear < 100) {
            year = 1900 + digitsYear;
        } else if (digitsYear < 100) {
            year = 2000 + digitsYear;
        } else {
            year = digitsYear;
        }

        // Return an absolute date.
        // Initialize to 1st January of the given year because
        // sometimes day in equal to 0 in the file.
        return new AbsoluteDate(new DateComponents(year, 1, 1), scale).
                shiftedBy(Constants.JULIAN_DAY * (day - 1)).
                shiftedBy(secInDay);

    }

    /**
     * Add a new entry to the map of stations.
     * @param station station entry to add
     */
    private void addStation(final Station station) {
        // Check if the station already exists
        if (stations.get(station.getSiteCode()) == null) {
            stations.put(station.getSiteCode(), station);
        }
    }

    /**
     * Add a new entry to the map of stations DCB.
     * @param dcb DCB entry
     * @param siteCode site code
     * @since 12.0
     */
    private void addDcbStation(final DcbStation dcb, final String siteCode) {
        // Check if the DCB for the current station already exists
        if (dcbStations.get(siteCode) == null) {
            dcbStations.put(siteCode, dcb);
        }
    }

    /**
     * Add a new entry to the map of satellites DCB.
     * @param dcb DCB entry
     * @param prn satellite PRN (e.g. "G01" for GPS 01)
     * @since 12.0
     */
    private void addDcbSatellite(final DcbSatellite dcb, final String prn) {
        if (dcbSatellites.get(prn) == null) {
            dcbSatellites.put(prn, dcb);
        }
    }

    /**
     * Get the EOP entry for the given epoch.
     * @param date epoch
     * @return the EOP entry corresponding to the epoch
     */
    private SinexEopEntry getSinexEopEntry(final AbsoluteDate date) {
        eop.putIfAbsent(date, new SinexEopEntry(date));
        return eop.get(date);
    }

    /**
     * Converts parsed EOP lines a list of EOP entries.
     * <p>
     * The first read chronological EOP entry is duplicated at the start
     * time of the data as read in the Sinex header. In addition, the last
     * read chronological data is duplicated at the end time of the data.
     * </p>
     * @param converter converter to use for nutation corrections
     * @param scale time scale of EOP entries
     * @return a list of EOP entries
     */
    private List<EOPEntry> getEopList(final IERSConventions.NutationCorrectionConverter converter,
                                      final TimeScale scale) {

        // Initialize the list
        final List<EOPEntry> eopEntries = new ArrayList<>();

        // Convert the map of parsed EOP data to a sorted set
        final SortedSet<SinexEopEntry> set = mapToSortedSet(eop);

        // Loop on set
        for (final SinexEopEntry entry : set) {
            // Add to the list
            eopEntries.add(entry.toEopEntry(converter, itrfVersionEop, scale));
        }

        // Add first entry to the start time of the data
        eopEntries.add(copyEopEntry(startDate, set.first()).toEopEntry(converter, itrfVersionEop, scale));

        // Add the last entry to the end time of the data
        eopEntries.add(copyEopEntry(endDate, set.last()).toEopEntry(converter, itrfVersionEop, scale));

        if (set.size() < 2) {
            // there is only one entry in the Sinex file
            // in order for interpolation to work, we need to add more dummy entries
            eopEntries.add(copyEopEntry(startDate.shiftedBy(+1.0), set.first()).toEopEntry(converter, itrfVersionEop, scale));
            eopEntries.add(copyEopEntry(endDate.shiftedBy(-1.0),   set.last()).toEopEntry(converter, itrfVersionEop, scale));
        }

        // Return
        eopEntries.sort(new ChronologicalComparator());
        return eopEntries;

    }

    /**
     * Convert a map of TimeStamped instances to a sorted set.
     * @param inputMap input map
     * @param <T> type of TimeStamped
     * @return corresponding sorted set, chronologically ordered
     */
    private <T extends TimeStamped> SortedSet<T> mapToSortedSet(final Map<AbsoluteDate, T> inputMap) {

        // Create a sorted set, chronologically ordered
        final SortedSet<T> set = new TreeSet<>(new ChronologicalComparator());

        // Fill the set
        for (final Map.Entry<AbsoluteDate, T> entry : inputMap.entrySet()) {
            set.add(entry.getValue());
        }

        // Return the filled list
        return set;

    }

    /**
     * Copy an EOP entry.
     * <p>
     * The data epoch is updated.
     * </p>
     * @param date new epoch
     * @param reference reference used for the data
     * @return a copy of the reference with new epoch
     */
    private SinexEopEntry copyEopEntry(final AbsoluteDate date, final SinexEopEntry reference) {

        // Initialize
        final SinexEopEntry newEntry = new SinexEopEntry(date);

        // Fill
        newEntry.setLod(reference.getLod());
        newEntry.setUt1MinusUtc(reference.getUt1MinusUtc());
        newEntry.setxPo(reference.getXPo());
        newEntry.setyPo(reference.getYPo());
        newEntry.setNutX(reference.getNutX());
        newEntry.setNutY(reference.getNutY());
        newEntry.setNutLn(reference.getNutLn());
        newEntry.setNutOb(reference.getNutOb());

        // Return
        return newEntry;

    }

}
