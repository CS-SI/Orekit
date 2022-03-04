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
import org.orekit.gnss.SatelliteSystem;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeScale;
import org.orekit.utils.Constants;
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
public class SinexLoader {

    /** 00:000:00000 epoch. */
    private static final String DEFAULT_EPOCH_TWO_DIGITS = "00:000:00000";

    /** 0000:000:00000 epoch. */
    private static final String DEFAULT_EPOCH_FOUR_DIGITS = "0000:000:00000";

    /** Pattern for delimiting regular expressions. */
    private static final Pattern SEPARATOR = Pattern.compile(":");

    /** Pattern for regular data. */
    private static final Pattern PATTERN_SPACE = Pattern.compile("\\s+");

    /** Pattern to check beginning of SINEX files.*/
    private static final Pattern PATTERN_BEGIN = Pattern.compile("(%=).*");


    /** Station data.
     * Key: Site code
     */
    private final Map<String, Station> stations;

    /** DCB data.
     * The DCB observations are stored by satellites, which stores pair of observation codes,
     * and the associated biases in a TimeSpanMap.
     *
     */
    private final Map<String, DCB> dcbMap;

    /**
     *
     */
    private final Map<String, List<String[]>> idMap;

    /** */
    private final DCBDescription dcbDescriptor;

    /** UTC time scale. */
    private final TimeScale utc;

    /** SINEX file creation date as extracted for the first line. */
    private AbsoluteDate creationDate;

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
        this.creationDate = AbsoluteDate.FUTURE_INFINITY;
        this.dcbDescriptor = new DCBDescription();
        dcbMap = new HashMap<>();
        stations = new HashMap<>();
        idMap = new HashMap<>();

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
            this.creationDate = AbsoluteDate.FUTURE_INFINITY;
            this.dcbDescriptor = null;
            dcbMap = new HashMap<>();
            stations = new HashMap<>();
            idMap = new HashMap<>();
            try (InputStream         is  = source.getOpener().openStreamOnce();
                 BufferedInputStream bis = new BufferedInputStream(is)) {
                new Parser().loadData(bis, source.getName());
            }
        } catch (IOException | ParseException ioe) {
            throw new OrekitException(ioe, new DummyLocalizable(ioe.getMessage()));
        }
    }

    /**
     *
     * @return sinex file creation date as an AbsoluteDate
     */
    public AbsoluteDate getCreationDate() {
        return creationDate;
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

    /**
     * Get the parsed dcb data, per satellite.
     *
     * @return unmodifiable view of parsed station data
     */
    public Map<String, DCB> getDCBMap() {
        return Collections.unmodifiableMap(dcbMap);
    }

    /**
     * Get the DCBSatellite object for a given satellite identified by its PRN.
     *
     * @param id
     * @return DCBSatellite object corresponding to the satPRN value.
     */
    public DCB getDCB(final String id) {
        return dcbMap.get(id);
    }

    /**
     *
     * @return a DCBDescription object containing the description data of the DCB file.
     */
    public DCBDescription getDCBDescription() {
        return dcbDescriptor;
    }

    /**
     * Add the DCBSatellite object to the dcbSatellites Map,
     * containing all dcb data.
     *
     * @param dcb
     * @param id
     */
    private void addDCB(final DCB dcb, final String id) {
        if (dcbMap.get(id) == null) {
            dcbMap.put(id, dcb);
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
            boolean inDcbDesc  = false;
            boolean inDcbSol  = false;
            boolean inId       = false;
            boolean inEcc      = false;
            boolean inEpoch    = false;
            boolean inEstimate = false;
            boolean firstEcc   = true;
            Vector3D position  = Vector3D.ZERO;
            Vector3D velocity  = Vector3D.ZERO;
            final TimeScale scale = utc;

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
                        creationDate = stringEpochToAbsoluteDate(splitFirstLine[3], scale);
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
                        case "+BIAS/DESCRIPTION" :
                            // Start of coordinates data
                            inDcbDesc = true;
                            break;
                        case "-BIAS/DESCRIPTION" :
                            // Start of coordinates data
                            inDcbDesc = false;
                            break;
                        case "+BIAS/SOLUTION" :
                            // Start of coordinates data
                            inDcbSol = true;
                            break;
                        case "-BIAS/SOLUTION" :
                            // Start of coordinates data
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
                                } else if (inEcc) {

                                    // read antenna eccentricities data
                                    final Station station = getStation(parseString(line, 1, 4));

                                    // check if it is the first eccentricity entry for this station
                                    if (station.getEccentricitiesTimeSpanMap().getSpansNumber() == 1) {
                                        // we are parsing eccentricity data for a new station
                                        firstEcc = true;
                                    }

                                    // start and end of validity for the current entry
                                    final AbsoluteDate start = stringEpochToAbsoluteDate(parseString(line, 16, 12), scale);
                                    final AbsoluteDate end   = stringEpochToAbsoluteDate(parseString(line, 29, 12), scale);

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
                                    station.setValidFrom(stringEpochToAbsoluteDate(parseString(line, 16, 12), scale));
                                    station.setValidUntil(stringEpochToAbsoluteDate(parseString(line, 29, 12), scale));
                                } else if (inEstimate) {
                                    final Station station = getStation(parseString(line, 14, 4));
                                    // check if this station exists
                                    if (station != null) {
                                        // switch on coordinates data
                                        switch (parseString(line, 7, 6)) {
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
                                                station.setEpoch(stringEpochToAbsoluteDate(parseString(line, 27, 12), scale));
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

                                } else if (inDcbDesc) {
                                    // Determining the data type for the DCBDescription object
                                    final String[] splitLine = PATTERN_SPACE.split(line);
                                    final String dataType = splitLine[1];
                                    switch (dataType) {
                                        case "OBSERVATION_SAMPLING":
                                            dcbDescriptor.setObservationSampling(Integer.parseInt(splitLine[2]));
                                            break;
                                        case "PARAMETER_SPACING":
                                            dcbDescriptor.setParameterSpacing(Integer.parseInt(splitLine[2]));
                                            break;
                                        case "DETERMINATION_METHOD":
                                            dcbDescriptor.setDeterminationMethod(splitLine[2]);
                                            break;
                                        case "BIAS_MODE":
                                            dcbDescriptor.setBiasMode(splitLine[2]);
                                            break;
                                        case "TIME_SYSTEM":
                                            final SatelliteSystem timeSystem =  SatelliteSystem.parseSatelliteSystem(splitLine[2]);
                                            dcbDescriptor.setTimeSystem(timeSystem);
                                            break;
                                        default:
                                            break;
                                    }


                                } else if (inDcbSol) {

                                    /**
                                     * Parsing the data present in a DCB file solution line.
                                     * Most fields are used in the files provided by CDDIS.
                                     * Station is empty for satellite measurements.
                                     * The separator between columns is composed of spaces.
                                     */

                                    final String satPRN = parseString(line, 11, 3);
                                    final String stationId = parseString(line, 15, 9);
                                    /**
                                     * Checking if a DCBSatellite object with the PRN key is present
                                     * in the HashMap storing the various satellites DCBs. If not, creating
                                     * such an object before assignment of the characteristics.
                                     */

                                    // Parsing the line data.
                                    // final String biasType = parseString(line, 1, 5);
                                    final String Obs1 = parseString(line, 25, 4);
                                    final String Obs2 = parseString(line, 30, 4);
                                    final AbsoluteDate beginDate = stringEpochToAbsoluteDate( parseString(line, 35, 14), scale);
                                    final AbsoluteDate endDate = stringEpochToAbsoluteDate( parseString(line, 50, 14), scale);
                                    final Unit unitDcb = Unit.parse(parseString(line, 65, 4));
                                    final double valueDcb = unitDcb.toSI(Double.parseDouble(parseString(line, 70, 21)));

                                    final String id = (stationId.equals("")) ? satPRN : satPRN.concat(stationId);
                                    final String objectId = (stationId.equals("")) ? satPRN : stationId;
                                    DCB dcb = getDCB(id);
                                    if (dcb == null) {
                                        dcb = new DCB(satPRN, stationId);
                                        final String[] listDcb = {satPRN.substring(0, 1), objectId, id};
                                        final List<String[]> listDcbId = idMap.get(objectId);
                                        if (listDcbId == null) {
                                            final ArrayList<String[]> newListDcb =  new ArrayList<String[]>();
                                            newListDcb.add(listDcb);
                                            idMap.put( objectId, newListDcb );
                                        } else {
                                            listDcbId.add(listDcb);
                                        }

                                    }

                                    dcb.addDCBLine(Obs1, Obs2, beginDate, endDate, valueDcb);
                                    // Adding the object to the HashMap if not present.
                                    addDCB(dcb, id);

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
     * @param scale TimeScale for the computation of the dates
     * @return the corresponding AbsoluteDate
     */
    private AbsoluteDate stringEpochToAbsoluteDate(final String stringDate, final TimeScale scale) {

        // Deal with 00:000:00000 epochs
        if (DEFAULT_EPOCH_TWO_DIGITS.equals(stringDate) || DEFAULT_EPOCH_FOUR_DIGITS.equals(stringDate)) {
            // Data is still available, return a dummy date at infinity in the future direction
            // FIXME : For Release 12.0 switch to return creationDate in order to follow Sinex
            // convention.
            return AbsoluteDate.FUTURE_INFINITY;
        }

        // Date components
        final String[] fields = SEPARATOR.split(stringDate);

        // Read fields
        final int DigitsYear = Integer.parseInt(fields[0]);
        final int day           = Integer.parseInt(fields[1]);
        final int secInDay      = Integer.parseInt(fields[2]);

        // Data year
        final int year;
        if (DigitsYear > 50 && DigitsYear < 100) {
            year = 1900 + DigitsYear;
        } else if (DigitsYear < 100) {
            year = 2000 + DigitsYear;
        } else {
            year = DigitsYear;
        }

        // Return an absolute date.
        // Initialize to 1st January of the given year because
        // sometimes day in equal to 0 in the file.
        return new AbsoluteDate(new DateComponents(year, 1, 1), scale).
                        shiftedBy(Constants.JULIAN_DAY * (day - 1)).
                        shiftedBy(secInDay);

    }

    /**
     *
     *
     * @param objectId
     * @return List of string arrays containing for a given satellite or station, the available satellite system,
     * satellite PRN or station id, and corresponding id for the DCB map.
     */
    public List<String[]> getAvailableSystems(final String objectId) {
        return idMap.get(objectId);
    }

    /**
     *
     * @return List of all satellite and stations systems
     */
    public List<List<String[]>> getAvailableSystems() {
        final List< List<String[]> > systemsList = new ArrayList<List<String[]>>();
        for (Entry<String, List<String[]>>  entry : idMap.entrySet()) {
            systemsList.add(entry.getValue());
        }
        return systemsList;
    }
}
