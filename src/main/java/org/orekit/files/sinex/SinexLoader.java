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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
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
 * @author Bryan Cazabonne
 * @since 10.3
 */
public class SinexLoader implements EOPHistoryLoader {

    /** 00:000:00000 epoch. */
    private static final String DEFAULT_EPOCH = "00:000:00000";

    /** Pattern for delimiting regular expressions. */
    private static final Pattern SEPARATOR = Pattern.compile(":");

    /** Pattern for regular data. */
    private static final Pattern PATTERN_SPACE = Pattern.compile("\\s+");

    /** Pattern to check beginning of SINEX files.*/
    private static final Pattern PATTERN_BEGIN = Pattern.compile("(%=).*");

    /** Start time of the data used in the Sinex solution.*/
    private AbsoluteDate startDate;

    /** End time of the data used in the Sinex solution.*/
    private AbsoluteDate endDate;

    /** Station data.
     * Key: Site code
     */
    private final Map<String, Station> stations;

    /** List of EOP Entries. */
    private final List<EOPEntry> eopList;

    /** Map containing the data stored for each parameter, as a Map, which key is an AbsoluteDate. */
    private final Map<AbsoluteDate, Map<String, Double>> mapEopHistory;

    /** List of all EOP parameter types. */
    private final List<String> eopTypes = List.of("LOD", "UT", "XPO", "YPO", "NUT_LN", "NUT_OB", "NUT_X", "NUT_Y");

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
     * @param supportedNames regular expression for supported files names
     * @param dataProvidersManager provides access to auxiliary data.
     * @param utc UTC time scale
     */
    public SinexLoader(final String supportedNames,
            final DataProvidersManager dataProvidersManager,
            final TimeScale utc) {
        this.utc = utc;
        stations = new HashMap<>();
        mapEopHistory = new HashMap<AbsoluteDate, Map<String, Double>>();
        this.itrfVersionEop = null;
        this.eopList = new ArrayList<EOPEntry>();
        dataProvidersManager.feed(supportedNames, new Parser());
    }

    /** Simple constructor. This constructor uses the {@link DataContext#getDefault()
     * default data context}.
     * @param source source for the RINEX data
     * @see #SinexLoader(String, DataProvidersManager, TimeScale)
     */
    @DefaultDataContext
    public SinexLoader(final DataSource source) {
        this(source, DataContext.getDefault().getTimeScales().getUTC());
    }

    /**
     * Loads SINEX from the given input stream using the specified auxiliary data.
     * @param source source for the RINEX data
     * @param utc UTC time scale
     */
    public SinexLoader(final DataSource source, final TimeScale utc) {
        try {
            this.utc = utc;
            stations = new HashMap<>();
            mapEopHistory = new HashMap<AbsoluteDate, Map<String, Double>>();
            this.eopList = new ArrayList<EOPEntry>();
            this.itrfVersionEop = null;
            try (InputStream         is  = source.getOpener().openStreamOnce();
                    BufferedInputStream bis = new BufferedInputStream(is)) {
                new Parser().loadData(bis, source.getName());
            }
        } catch (IOException | ParseException ioe) {
            throw new OrekitException(ioe, new DummyLocalizable(ioe.getMessage()));
        }
    }

    /**
     * Get the parsed station data.
     * @return unmodifiable view of parsed station data
     */
    public Map<String, Station> getStations() {
        return Collections.unmodifiableMap(stations);
    }

    /**
     * Get the station corresponding to the given site code.
     * @param siteCode site code
     * @return the corresponding station
     */
    public Station getStation(final String siteCode) {
        return stations.get(siteCode);
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

                    /**
                     *  The first line is parsed in order to get the creation date of the file, which might be used
                     *  in the case of an absent date as the final date of the data.
                     *  Its position is fixed in the file, at the first line, in the 4th column.
                     */
                    if (lineNumber == 1 && PATTERN_BEGIN.matcher(line).matches()) {
                        final String[] splitFirstLine = PATTERN_SPACE.split(line);
                        startDate = stringEpochToAbsoluteDate(splitFirstLine[5]);
                        endDate = stringEpochToAbsoluteDate(splitFirstLine[6]);
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
                                    final Station station = getStation(parseString(line, 14, 4));
                                    final AbsoluteDate currentDate = stringEpochToAbsoluteDate(parseString(line, 27, 12));
                                    final String dataType = parseString(line, 7, 6);
                                    // check if this station exists
                                    if (station != null) {
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
                                            default:
                                                // ignore that field
                                                break;
                                        }
                                    }
                                    // Check if map containing the eop data exists and if dataType is in eopType
                                    // if not build map to store and put it in the eop history map
                                    // else access it and add key to the eop map
                                    Map<String, Double> eopMap = mapEopHistory.get(currentDate);
                                    if ( eopMap == null && eopTypes.indexOf(dataType) != -1) {
                                        mapEopHistory.put(currentDate, new HashMap<String, Double>());
                                        eopMap = mapEopHistory.get(currentDate);
                                    }
                                    if ( eopMap != null && eopTypes.indexOf(dataType) != -1) {
                                        final Unit unitEop = Unit.parse(parseString(line, 40, 4));
                                        eopMap.put( dataType, unitEop.toSI( parseDouble(line, 47, 22) ) );
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
     * Check if a Double object is null, and returns either the value if not-null or 0.
     *
     * @param value Double object to check.
     * @return Double object value if not null or 0.
     */
    private double checkDouble(final Double value) {
        return (value == null) ? 0 : value.doubleValue();
    }

    /**
     * Method used to copy an EOPEntry object, with a shifted date and according Modified Julian Date.
     *
     * @param entry EOPEntry to shift in time.
     * @param newDate AbsoluteDate at which to shift the EOPEntry.
     * @return Date shifted EOPEntry object.
     */
    private EOPEntry shiftEopEntry(final EOPEntry entry, final AbsoluteDate newDate) {
        // Compute the MJD for the new date.
        final int mjd = newDate.getComponents(utc).getDate().getMJD();
        // Returns a new instance of the object with modified dates.
        return new EOPEntry(mjd, entry.getUT1MinusUTC(), entry.getLOD(),
                entry.getX(), entry.getY(), entry.getDdPsi(), entry.getDdEps(),
                entry.getDx(), entry.getDy(),
                entry.getITRFType(), newDate);
    }

    /**
     * Convert EOP lines into a single EOPEntry object for a given AbsoluteDate.
     * EOPEntry objects can be incomplete, with the absent values being set to zero.
     *
     * Takes a converter as a parameter being fed by the fillHistory method.
     *
     * @param converter
     */
    public void toEopEntries(final IERSConventions.NutationCorrectionConverter converter) {
        // Go through each item stored in the map, with the key being the date.
        for (Entry<AbsoluteDate, Map<String, Double>> mapEntry : mapEopHistory.entrySet()) {
            // Get and prepare date, mjd
            final AbsoluteDate date = mapEntry.getKey();
            final int mjd = mapEntry.getKey().getComponents(utc).getDate().getMJD();

            // Get values :  If key absent set to 0 through checkDouble
            final Map<String, Double> eopMap = mapEntry.getValue();

            final double ut = checkDouble(eopMap.get(eopTypes.get(1)));
            final double lod = checkDouble(eopMap.get(eopTypes.get(0)));
            final double xpo = checkDouble(eopMap.get(eopTypes.get(2)));
            final double ypo = checkDouble(eopMap.get(eopTypes.get(3)));
            final double nut_ln = checkDouble(eopMap.get(eopTypes.get(4)));
            final double nut_ob = checkDouble(eopMap.get(eopTypes.get(5)));
            final double nut_x = checkDouble(eopMap.get(eopTypes.get(6)));
            final double nut_y = checkDouble(eopMap.get(eopTypes.get(7)));

            // Prepare correction double arrays (equinox and non rotating origin)
            final double[] nro;
            final double[] equinox;

            // Obtain the correction values and put them into the arrays.
            if (nut_x != 0 && nut_y != 0 && nut_ln == 0 && nut_ob == 0) {
                nro = new double[] {nut_x, nut_y};
                equinox = converter.toEquinox(date, nut_x, nut_y);
            } else if (nut_x == 0 && nut_y == 0 && nut_ln != 0 && nut_ob != 0) {
                nro = converter.toNonRotating(date, nut_ln, nut_ob);
                equinox = new double[] {nut_ln, nut_ob};
            } else {
                nro = new double[] {nut_x, nut_y};
                equinox = new double[] {nut_ln, nut_ob};
            }

            // Create a new EOPEntry object storing the extracted data, then add it to the list of EOPEntries.
            final EOPEntry newEopEntry = new EOPEntry(mjd, ut, lod, xpo, ypo, equinox[0], equinox[1], nro[0], nro[1], itrfVersionEop, date);
            eopList.add(newEopEntry);

        }

        // Sort the list by date, before further operations.
        eopList.sort(new ChronologicalComparator());

        // Check for the creation of the EOPHistory object, that requires at least 4 interpolation points.
        // Handling the cases with less than 4 EOP entries.
        switch (eopList.size()) {
            case 1 :
                // Consider the value constant for the validity of the file.
                // If only a value, add the same entry 3 times to the end of data date defined in the SINEX file.
                final EOPEntry entry = eopList.get(0);
                if (entry.getDate().isBetween(startDate, endDate)) {
                    final double timeDifference = endDate.durationFrom(entry.getDate());
                    final double timeStep = timeDifference / 3;
                    eopList.add(shiftEopEntry(entry, entry.getDate().shiftedBy(timeStep)));
                    eopList.add(shiftEopEntry(entry, entry.getDate().shiftedBy(timeStep * 2)));
                    eopList.add(shiftEopEntry(entry, endDate));
                }
                break;
            default:
                break;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void fillHistory(final NutationCorrectionConverter converter,
            final SortedSet<EOPEntry> history) {
        // Call the toEopEntries method to fill the eopList variable.
        toEopEntries(converter);
        // Fill the history set with the content of the eopList list of EOPEntry objects.
        history.addAll(eopList);
    }

    /**
     * Setter for the ITRF version used in EOP entries processing.
     *
     * @param year Year of the ITRF Version used for the EOPEntry objects.
     */
    public void setITRFVersion(final int year) {
        this.itrfVersionEop = ITRFVersion.getITRFVersion(year);
    }

    /**
     *  Getter for the ITRF version used for the EOP entries processing.
     *
     * @return Year of the ITRF Version used for the EOPEntry objects.
     */
    public ITRFVersion getITRFVersion() {
        return itrfVersionEop;
    }

}
