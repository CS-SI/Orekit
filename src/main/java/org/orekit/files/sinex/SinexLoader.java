/* Copyright 2002-2022 CS GROUP
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
import org.orekit.frames.EOPHistoryLoader;
import org.orekit.frames.ITRFVersion;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChronologicalComparator;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeStamped;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.IERSConventions.NutationCorrectionConverter;
import org.orekit.utils.units.Unit;

/**
 * Loader for Solution INdependent EXchange (SINEX) files.
 * <p>
 * For now only few keys are supported: SITE/ID, SITE/ECCENTRICITY, SOLUTION/EPOCHS and SOLUTION/ESTIMATE.
 * They represent the minimum set of parameters that are interesting to consider in a SINEX file.
 * </p>
 * <p>
 * The parsing of EOP parameters for multiple files in different SinexLoader object, fed into the default DataContext
 * might pose a problem in case validity dates are overlapping. As Sinex daily solution files provide a single EOP entry,
 * the Sinex loader will add points at the limits of data dates (startDate, endDate) of the Sinex file, which in case of
 * overlap will lead to inconsistencies in the final EOPHistory object. Multiple files can be parsed using a single SinexLoader
 * with a regex to overcome this issue.
 * </p>
 *
 * @author Bryan Cazabonne
 * @since 10.3
 */
public class SinexLoader implements EOPHistoryLoader {

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
    private static final String DEFAULT_EPOCH = "00:000:00000";

    /** Pattern for delimiting regular expressions. */
    private static final Pattern SEPARATOR = Pattern.compile(":");

    /** Pattern for regular data. */
    private static final Pattern PATTERN_SPACE = Pattern.compile("\\s+");

    /** Pattern to check beginning of SINEX files.*/
    private static final Pattern PATTERN_BEGIN = Pattern.compile("(%=).*");

    /** List of all EOP parameter types. */
    private static final List<String> EOP_TYPES = Arrays.asList(LOD, UT, XPO, YPO, NUT_LN, NUT_OB, NUT_X, NUT_Y);

    /** Start time of the data used in the Sinex solution.*/
    private AbsoluteDate startDate;

    /** End time of the data used in the Sinex solution.*/
    private AbsoluteDate endDate;

    /** Station data.
     * Key: Site code
     */
    private final Map<String, Station> stations;

    /** Data set. */
    private Map<AbsoluteDate, SinexEopEntry> map;

    /** ITRF Version used for EOP parsing. */
    private ITRFVersion itrfVersionEop;

    /** UTC time scale. */
    private final TimeScale utc;

    /** Simple constructor. This constructor uses the {@link DataContext#getDefault()
     * default data context}.
     * @param supportedNames regular expression for supported files names
     * @see #SinexLoader(String, DataProvidersManager, TimeScale)
     */
    @DefaultDataContext
    public SinexLoader(final String supportedNames) {
        this(supportedNames,
             DataContext.getDefault().getDataProvidersManager(),
             DataContext.getDefault().getTimeScales().getUTC());
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
     * @param utc UTC time scale
     */
    public SinexLoader(final String supportedNames,
                       final DataProvidersManager dataProvidersManager,
                       final TimeScale utc) {
        this.utc            = utc;
        this.stations       = new HashMap<>();
        this.itrfVersionEop = ITRFVersion.ITRF_2014;
        this.map            = new HashMap<>();
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
     * @see #SinexLoader(String, DataProvidersManager, TimeScale)
     */
    @DefaultDataContext
    public SinexLoader(final DataSource source) {
        this(source, DataContext.getDefault().getTimeScales().getUTC());
    }

    /**
     * Loads SINEX from the given input stream using the specified auxiliary data.
     * <p>
     * For EOP loading, a default {@link ITRFVersion#ITRF_2014} is used. It is
     * possible to update the version using the {@link #setITRFVersion(int)}
     * method.
     * </p>
     * @param source source for the RINEX data
     * @param utc UTC time scale
     */
    public SinexLoader(final DataSource source, final TimeScale utc) {
        try {
            this.utc            = utc;
            this.stations       = new HashMap<>();
            this.itrfVersionEop = ITRFVersion.ITRF_2014;
            this.map            = new HashMap<>();
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
        return Collections.unmodifiableMap(map);
    }

    /**
     * Get the station corresponding to the given site code.
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
        history.addAll(getEopList(converter));
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
            int lineNumber     = 0;
            String line        = null;
            boolean inId       = false;
            boolean inEcc      = false;
            boolean inEpoch    = false;
            boolean inEstimate = false;
            boolean firstEcc   = true;
            Vector3D position  = Vector3D.ZERO;
            Vector3D velocity  = Vector3D.ZERO;

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {

                // Loop on lines
                for (line = reader.readLine(); line != null; line = reader.readLine()) {
                    ++lineNumber;
                    // For now, only few keys are supported
                    // They represent the minimum set of parameters that are interesting to consider in a SINEX file
                    // Other keys can be added depending user needs

                    // The first line is parsed in order to get the creation date of the file, which might be used
                    // in the case of an absent date as the final date of the data.
                    // Its position is fixed in the file, at the first line, in the 4th column.
                    if (lineNumber == 1 && PATTERN_BEGIN.matcher(line).matches()) {
                        final String[]     splitFirstLine = PATTERN_SPACE.split(line);
                        final AbsoluteDate fileStartDate  = stringEpochToAbsoluteDate(splitFirstLine[5]);
                        final AbsoluteDate fileEndDate    = stringEpochToAbsoluteDate(splitFirstLine[6]);
                        if (startDate == null) {
                            // First data loading, needs to initialize the start and end dates for EOP history
                            startDate = fileStartDate;
                            endDate   = fileEndDate;
                        }
                        // In case of multiple files used for EOP history, the start and end dates can be updated
                        startDate = fileStartDate.isBefore(startDate) ? fileStartDate : startDate;
                        endDate   = fileEndDate.isAfter(endDate) ? fileEndDate : endDate;
                    }

                    switch (line.trim()) {
                        case "+SITE/ID" :
                            // Start of site id. data
                            inId = true;
                            break;
                        case "-SITE/ID" :
                            // End of site id. data
                            inId = false;
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
                            // Start of coordinates data
                            inEstimate = false;
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
                                } else if (inEcc) {

                                    // read antenna eccentricities data
                                    final Station station = getStation(parseString(line, 1, 4));

                                    // check if it is the first eccentricity entry for this station
                                    if (station.getEccentricitiesTimeSpanMap().getSpansNumber() == 1) {
                                        // we are parsing eccentricity data for a new station
                                        firstEcc = true;
                                    }

                                    // start and end of validity for the current entry
                                    final AbsoluteDate start = stringEpochToAbsoluteDate(parseString(line, 16, 12));
                                    final AbsoluteDate end   = stringEpochToAbsoluteDate(parseString(line, 29, 12));

                                    // reference system UNE or XYZ
                                    station.setEccRefSystem(ReferenceSystem.getEccRefSystem(parseString(line, 42, 3)));

                                    // eccentricity vector
                                    final Vector3D eccStation = new Vector3D(parseDouble(line, 46, 8),
                                                                             parseDouble(line, 55, 8),
                                                                             parseDouble(line, 64, 8));

                                    // special implementation for the first entry
                                    if (firstEcc) {
                                        // we want null values outside validity limits of the station
                                        station.addStationEccentricitiesValidBefore(eccStation, end);
                                        station.addStationEccentricitiesValidBefore(null,       start);
                                        // we parsed the first entry, set the flag to false
                                        firstEcc = false;
                                    } else {
                                        station.addStationEccentricitiesValidBefore(eccStation, end);
                                    }

                                    // update the last known eccentricities entry
                                    station.setEccentricities(eccStation);

                                } else if (inEpoch) {
                                    // read epoch data
                                    final Station station = getStation(parseString(line, 1, 4));
                                    station.setValidFrom(stringEpochToAbsoluteDate(parseString(line, 16, 12)));
                                    station.setValidUntil(stringEpochToAbsoluteDate(parseString(line, 29, 12)));
                                } else if (inEstimate) {
                                    final Station       station     = getStation(parseString(line, 14, 4));
                                    final AbsoluteDate  currentDate = stringEpochToAbsoluteDate(parseString(line, 27, 12));
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
                                } else {
                                    // not supported line, ignore it
                                }
                            }
                            break;
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
     * @return the corresponding AbsoluteDate
     */
    private AbsoluteDate stringEpochToAbsoluteDate(final String stringDate) {

        // Deal with 00:000:00000 epochs
        if (DEFAULT_EPOCH.equals(stringDate)) {
            // Data is still available, return a dummy date at infinity in the future direction
            return AbsoluteDate.FUTURE_INFINITY;
        }

        // Date components
        final String[] fields = SEPARATOR.split(stringDate);

        // Read fields
        final int twoDigitsYear = Integer.parseInt(fields[0]);
        final int day           = Integer.parseInt(fields[1]);
        final int secInDay      = Integer.parseInt(fields[2]);

        // Data year
        final int year;
        if (twoDigitsYear > 50) {
            year = 1900 + twoDigitsYear;
        } else {
            year = 2000 + twoDigitsYear;
        }

        // Return an absolute date.
        // Initialize to 1st January of the given year because
        // sometimes day in equal to 0 in the file.
        return new AbsoluteDate(new DateComponents(year, 1, 1), utc).
                shiftedBy(Constants.JULIAN_DAY * (day - 1)).
                shiftedBy(secInDay);

    }

    /**
     * Add a new entry to the map of stations.
     * @param station station entry to add
     */
    private void addStation(final Station station) {
        // Check if station already exists
        if (stations.get(station.getSiteCode()) == null) {
            stations.put(station.getSiteCode(), station);
        }
    }

    /**
     * Get the EOP entry for the given epoch.
     * @param date epoch
     * @return the EOP entry corresponding to the epoch
     */
    private SinexEopEntry getSinexEopEntry(final AbsoluteDate date) {
        map.putIfAbsent(date, new SinexEopEntry(date));
        return map.get(date);
    }

    /**
     * Converts parsed EOP lines a list of EOP entries.
     * <p>
     * The first read chronological EOP entry is duplicated at the start
     * time of the data as read in the Sinex header. In addition, the last
     * read chronological data is duplicated at the end time of the data.
     * </p>
     * @param converter converter to use for nutation corrections
     * @return a list of EOP entries
     */
    private List<EOPEntry> getEopList(final IERSConventions.NutationCorrectionConverter converter) {

        // Initialize the list
        final List<EOPEntry> eop = new ArrayList<>();

        // Convert the map of parsed EOP data to a sorted set
        final SortedSet<SinexEopEntry> set = mapToSortedSet(map);

        // Loop on set
        for (final SinexEopEntry entry : set) {
            // Add to the list
            eop.add(entry.toEopEntry(converter, itrfVersionEop, utc));
        }

        // Add first entry to the start time of the data
        eop.add(copyEopEntry(startDate, set.first()).toEopEntry(converter, itrfVersionEop, utc));

        // Add the last entry to the end time of the data
        eop.add(copyEopEntry(endDate, set.last()).toEopEntry(converter, itrfVersionEop, utc));

        // Return
        eop.sort(new ChronologicalComparator());
        return eop;

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
