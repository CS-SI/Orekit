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
package org.orekit.files.sinex;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.hipparchus.exception.DummyLocalizable;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.data.DataLoader;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.sinex.Station.ReferenceSystem;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeScale;
import org.orekit.utils.Constants;

/**
 * Loader for Solution INdependent EXchange (SINEX) files.
 * <p>
 * For now only few keys are supported: SITE/ID, SITE/ECCENTRICITY, SOLUTION/EPOCHS and SOLUTION/ESTIMATE.
 * They represent the minimum set of parameters that are interesting to consider in a SINEX file.
 * </p>
 * @author Bryan Cazabonne
 * @since 10.3
 */
public class SINEXLoader {

    /** Pattern for delimiting regular expressions. */
    private static final Pattern SEPARATOR = Pattern.compile(":");

    /** Station data.
     * Key: Site code
     */
    private final Map<String, Station> stations;

    /** UTC time scale. */
    private final TimeScale utc;

    /** Simple constructor. This constructor uses the {@link DataContext#getDefault()
     * default data context}.
     * @param supportedNames regular expression for supported files names
     * @see #SINEXLoader(String, DataProvidersManager, TimeScale)
     */
    @DefaultDataContext
    public SINEXLoader(final String supportedNames) {
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
    public SINEXLoader(final String supportedNames,
                       final DataProvidersManager dataProvidersManager,
                       final TimeScale utc) {
        this.utc = utc;
        stations = new HashMap<>();
        dataProvidersManager.feed(supportedNames, new Parser());
    }

    /** Simple constructor. This constructor uses the {@link DataContext#getDefault()
     * default data context}.
     * @param input data input stream
     * @param name name of the file (or zip entry)
     * @see #SINEXLoader(InputStream, String, TimeScale)
     */
    @DefaultDataContext
    public SINEXLoader(final InputStream input, final String name) {
        this(input, name, DataContext.getDefault().getTimeScales().getUTC());
    }

    /**
     * Loads SINEX from the given input stream using the specified auxiliary data.
     * @param input data input stream
     * @param name name of the file (or zip entry)
     * @param utc UTC time scale
     */
    public SINEXLoader(final InputStream input,
                       final String name,
                       final TimeScale utc) {
        try {
            this.utc = utc;
            stations = new HashMap<>();
            new Parser().loadData(input, name);
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
            Vector3D position  = Vector3D.ZERO;
            Vector3D velocity  = Vector3D.ZERO;

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {

                // Loop on lines
                for (line = reader.readLine(); line != null; line = reader.readLine()) {
                    ++lineNumber;
                    // For now, only few keys are supported
                    // They represent the minimum set of parameters that are interesting to consider in a SINEX file
                    // Other keys can be added depending user needs
                    switch(line.trim()) {
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
                                    // check if start and end dates have been set
                                    if (station.getValidFrom() == null) {
                                        station.setValidFrom(stringEpochToAbsoluteDate(parseString(line, 16, 12)));
                                        station.setValidUntil(stringEpochToAbsoluteDate(parseString(line, 29, 12)));
                                    }
                                    station.setEccRefSystem(ReferenceSystem.getEccRefSystem(parseString(line, 42, 3)));
                                    station.setEccentricities(new Vector3D(parseDouble(line, 46, 8),
                                                                           parseDouble(line, 55, 8),
                                                                           parseDouble(line, 64, 8)));
                                } else if (inEpoch) {
                                    // read epoch data
                                    final Station station = getStation(parseString(line, 1, 4));
                                    station.setValidFrom(stringEpochToAbsoluteDate(parseString(line, 16, 12)));
                                    station.setValidUntil(stringEpochToAbsoluteDate(parseString(line, 29, 12)));
                                } else if (inEstimate) {
                                    final Station station = getStation(parseString(line, 14, 4));
                                    // check if this station exists
                                    if (station != null) {
                                        // switch on coordinates data
                                        switch(parseString(line, 7, 6)) {
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
                                                station.setEpoch(stringEpochToAbsoluteDate(parseString(line, 27, 12)));
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

}
