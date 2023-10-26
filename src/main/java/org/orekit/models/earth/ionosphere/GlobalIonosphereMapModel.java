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
package org.orekit.models.earth.ionosphere;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.analysis.interpolation.BilinearInterpolatingFunction;
import org.hipparchus.exception.DummyLocalizable;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.data.DataContext;
import org.orekit.data.DataLoader;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.TopocentricFrame;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeSpanMap;

/**
 * Global Ionosphere Map (GIM) model.
 * The ionospheric delay is computed according to the formulas:
 * <pre>
 *           40.3
 *    δ =  --------  *  STEC      with, STEC = VTEC * F(elevation)
 *            f²
 * </pre>
 * With:
 * <ul>
 * <li>f: The frequency of the signal in Hz.</li>
 * <li>STEC: The Slant Total Electron Content in TECUnits.</li>
 * <li>VTEC: The Vertical Total Electron Content in TECUnits.</li>
 * <li>F(elevation): A mapping function which depends on satellite elevation.</li>
 * </ul>
 * The VTEC is read from a IONEX file. A stream contains, for a given day, the values of the TEC for each hour of the day.
 * Values are given on a global 2.5° x 5.0° (latitude x longitude) grid.
 * <p>
 * A bilinear interpolation is performed the case of the user initialize the latitude and the
 * longitude with values that are not contained in the stream.
 * </p><p>
 * A temporal interpolation is also performed to compute the VTEC at the desired date.
 * </p><p>
 * IONEX files are obtained from
 * <a href="ftp://cddis.nasa.gov/gnss/products/ionex/"> The Crustal Dynamics Data Information System</a>.
 * </p><p>
 * The files have to be extracted to UTF-8 text files before being read by this loader.
 * </p><p>
 * Example of file:
 * </p>
 * <pre>
 *      1.0            IONOSPHERE MAPS     GPS                 IONEX VERSION / TYPE
 * BIMINX V5.3         AIUB                16-JAN-19 07:26     PGM / RUN BY / DATE
 * BROADCAST IONOSPHERE MODEL FOR DAY 015, 2019                COMMENT
 *   2019     1    15     0     0     0                        EPOCH OF FIRST MAP
 *   2019     1    16     0     0     0                        EPOCH OF LAST MAP
 *   3600                                                      INTERVAL
 *     25                                                      # OF MAPS IN FILE
 *   NONE                                                      MAPPING FUNCTION
 *      0.0                                                    ELEVATION CUTOFF
 *                                                             OBSERVABLES USED
 *   6371.0                                                    BASE RADIUS
 *      2                                                      MAP DIMENSION
 *    350.0 350.0   0.0                                        HGT1 / HGT2 / DHGT
 *     87.5 -87.5  -2.5                                        LAT1 / LAT2 / DLAT
 *   -180.0 180.0   5.0                                        LON1 / LON2 / DLON
 *     -1                                                      EXPONENT
 * TEC/RMS values in 0.1 TECU; 9999, if no value available     COMMENT
 *                                                             END OF HEADER
 *      1                                                      START OF TEC MAP
 *   2019     1    15     0     0     0                        EPOCH OF CURRENT MAP
 *     87.5-180.0 180.0   5.0 350.0                            LAT/LON1/LON2/DLON/H
 *    92   92   92   92   92   92   92   92   92   92   92   92   92   92   92   92
 *    92   92   92   92   92   92   92   92   92   92   92   92   92   92   92   92
 *    92   92   92   92   92   92   92   92   92   92   92   92   92   92   92   92
 *    92   92   92   92   92   92   92   92   92   92   92   92   92   92   92   92
 *    92   92   92   92   92   92   92   92   92
 *    ...
 * </pre>
 *
 * @see "Schaer, S., W. Gurtner, and J. Feltens, 1998, IONEX: The IONosphere Map EXchange
 *       Format Version 1, February 25, 1998, Proceedings of the IGS AC Workshop
 *       Darmstadt, Germany, February 9–11, 1998"
 *
 * @author Bryan Cazabonne
 *
 */
public class GlobalIonosphereMapModel implements IonosphericModel {

    /** Pattern for delimiting regular expressions. */
    private static final Pattern SEPARATOR = Pattern.compile("\\s+");

    /** Map of interpolable TEC. */
    private TimeSpanMap<TECMapPair> tecMap;

    /** UTC time scale. */
    private final TimeScale utc;

    /** Loaded IONEX files.
     * @since 12.0
     */
    private String names;

    /**
     * Constructor with supported names given by user. This constructor uses the {@link
     * DataContext#getDefault() default data context}.
     *
     * @param supportedNames regular expression that matches the names of the IONEX files
     *                       to be loaded. See {@link DataProvidersManager#feed(String,
     *                       DataLoader)}.
     * @see #GlobalIonosphereMapModel(String, DataProvidersManager, TimeScale)
     */
    @DefaultDataContext
    public GlobalIonosphereMapModel(final String supportedNames) {
        this(supportedNames,
                DataContext.getDefault().getDataProvidersManager(),
                DataContext.getDefault().getTimeScales().getUTC());
    }

    /**
     * Constructor that uses user defined supported names and data context.
     *
     * @param supportedNames       regular expression that matches the names of the IONEX
     *                             files to be loaded. See {@link DataProvidersManager#feed(String,
     *                             DataLoader)}.
     * @param dataProvidersManager provides access to auxiliary data files.
     * @param utc                  UTC time scale.
     * @since 10.1
     */
    public GlobalIonosphereMapModel(final String supportedNames,
                                    final DataProvidersManager dataProvidersManager,
                                    final TimeScale utc) {
        this.utc    = utc;
        this.tecMap = new TimeSpanMap<>(null);
        this.names  = "";

        // Read files
        dataProvidersManager.feed(supportedNames, new Parser());

    }

    /**
     * Constructor that uses user defined data sources.
     *
     * @param utc   UTC time scale.
     * @param ionex sources for the IONEX files
     * @since 12.0
     */
    public GlobalIonosphereMapModel(final TimeScale utc,
                                    final DataSource... ionex) {
        try {
            this.utc    = utc;
            this.tecMap = new TimeSpanMap<>(null);
            this.names  = "";
            final Parser parser = new Parser();
            for (final DataSource source : ionex) {
                try (InputStream is  = source.getOpener().openStreamOnce();
                    BufferedInputStream bis = new BufferedInputStream(is)) {
                    parser.loadData(bis, source.getName());
                }
            }
        } catch (IOException ioe) {
            throw new OrekitException(ioe, new DummyLocalizable(ioe.getMessage()));
        }
    }

    /**
     * Calculates the ionospheric path delay for the signal path from a ground
     * station to a satellite traversing ionosphere single layer at some pierce point.
     * <p>
     * The path delay can be computed for any elevation angle.
     * </p>
     * @param date current date
     * @param piercePoint ionospheric pierce point
     * @param elevation elevation of the satellite from receiver point in radians
     * @param frequency frequency of the signal in Hz
     * @return the path delay due to the ionosphere in m
     */
    private double pathDelayAtIPP(final AbsoluteDate date, final GeodeticPoint piercePoint,
                                  final double elevation, final double frequency) {
        // TEC in TECUnits
        final TECMapPair pair = getPairAtDate(date);
        final double tec = pair.getTEC(date, piercePoint);
        // Square of the frequency
        final double freq2 = frequency * frequency;
        // "Slant" Total Electron Content
        final double stec;
        // Check if a mapping factor is needed
        if (pair.mapping) {
            stec = tec;
        } else {
            // Mapping factor
            final double fz = mappingFunction(elevation, pair.r0, pair.h);
            stec = tec * fz;
        }
        // Delay computation
        final double alpha  = 40.3e16 / freq2;
        return alpha * stec;
    }

    @Override
    public double pathDelay(final SpacecraftState state, final TopocentricFrame baseFrame,
                            final double frequency, final double[] parameters) {

        // Satellite position in body frame
        final Frame    bodyFrame = baseFrame.getParentShape().getBodyFrame();
        final Vector3D satPoint  = state.getPosition(bodyFrame);

        // Elevation in radians
        final double   elevation = bodyFrame.
                                   getStaticTransformTo(baseFrame, state.getDate()).
                                   transformPosition(satPoint).
                                   getDelta();

        // Only consider measures above the horizon
        if (elevation > 0.0) {
            // Normalized Line Of Sight in body frame
            final Vector3D los = satPoint.subtract(baseFrame.getCartesianPoint()).normalize();
            // Ionosphere Pierce Point
            final GeodeticPoint ipp = piercePoint(state.getDate(), baseFrame.getCartesianPoint(), los, baseFrame.getParentShape());
            if (ipp != null) {
                // Delay
                return pathDelayAtIPP(state.getDate(), ipp, elevation, frequency);
            }
        }

        return 0.0;

    }

    /**
     * Calculates the ionospheric path delay for the signal path from a ground
     * station to a satellite traversing ionosphere single layer at some pierce point.
     * <p>
     * The path delay can be computed for any elevation angle.
     * </p>
     * @param <T> type of the elements
     * @param date current date
     * @param piercePoint ionospheric pierce point
     * @param elevation elevation of the satellite from receiver point in radians
     * @param frequency frequency of the signal in Hz
     * @return the path delay due to the ionosphere in m
     */
    private <T extends CalculusFieldElement<T>> T pathDelayAtIPP(final FieldAbsoluteDate<T> date,
                                                                 final GeodeticPoint piercePoint,
                                                                 final T elevation, final double frequency) {
        // TEC in TECUnits
        final TECMapPair pair = getPairAtDate(date.toAbsoluteDate());
        final T tec = pair.getTEC(date, piercePoint);
        // Square of the frequency
        final double freq2 = frequency * frequency;
        // "Slant" Total Electron Content
        final T stec;
        // Check if a mapping factor is needed
        if (pair.mapping) {
            stec = tec;
        } else {
            // Mapping factor
            final T fz = mappingFunction(elevation, pair.r0, pair.h);
            stec = tec.multiply(fz);
        }
        // Delay computation
        final double alpha  = 40.3e16 / freq2;
        return stec.multiply(alpha);
    }

    @Override
    public <T extends CalculusFieldElement<T>> T pathDelay(final FieldSpacecraftState<T> state, final TopocentricFrame baseFrame,
                                                           final double frequency, final T[] parameters) {

        // Satellite position in body frame
        final Frame            bodyFrame = baseFrame.getParentShape().getBodyFrame();
        final FieldVector3D<T> satPoint  = state.getPosition(bodyFrame);

        // Elevation in radians
        final T                elevation = bodyFrame.
                                           getStaticTransformTo(baseFrame, state.getDate()).
                                           transformPosition(satPoint).
                                           getDelta();

        // Only consider measures above the horizon
        if (elevation.getReal() > 0.0) {
            // Normalized Line Of Sight in body frame
            final Vector3D los = satPoint.toVector3D().subtract(baseFrame.getCartesianPoint()).normalize();
            // Ionosphere Pierce Point
            final GeodeticPoint ipp = piercePoint(state.getDate().toAbsoluteDate(), baseFrame.getCartesianPoint(), los, baseFrame.getParentShape());
            if (ipp != null) {
                // Delay
                return pathDelayAtIPP(state.getDate(), ipp, elevation, frequency);
            }
        }

        return elevation.getField().getZero();

    }

    /** Get the pair valid at date.
     * @param date computation date
     * @return pair valid at date
     * @since 12.0
     */
    private TECMapPair getPairAtDate(final AbsoluteDate date) {
        final TECMapPair pair = tecMap.get(date);
        if (pair == null) {
            throw new OrekitException(OrekitMessages.NO_TEC_DATA_IN_FILES_FOR_DATE,
                                      names, date);
        }
        return pair;
    }

    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.emptyList();
    }

    /**
     * Computes the ionospheric mapping function.
     * @param elevation the elevation of the satellite in radians
     * @param r0 mean Earth radius
     * @param h height of the ionospheric layer
     * @return the mapping function
     */
    private double mappingFunction(final double elevation,
                                   final double r0, final double h) {
        // Calculate the zenith angle from the elevation
        final double z = FastMath.abs(0.5 * FastMath.PI - elevation);
        // Distance ratio
        final double ratio = r0 / (r0 + h);
        // Mapping function
        final double coef = FastMath.sin(z) * ratio;
        final double fz = 1.0 / FastMath.sqrt(1.0 - coef * coef);
        return fz;
    }

    /**
     * Computes the ionospheric mapping function.
     * @param <T> type of the elements
     * @param elevation the elevation of the satellite in radians
     * @param r0 mean Earth radius
     * @param h height of the ionospheric layer
     * @return the mapping function
     */
    private <T extends CalculusFieldElement<T>> T mappingFunction(final T elevation,
                                                                  final double r0, final double h) {
        // Calculate the zenith angle from the elevation
        final T z = FastMath.abs(elevation.getPi().multiply(0.5).subtract(elevation));
        // Distance ratio
        final double ratio = r0 / (r0 + h);
        // Mapping function
        final T coef = FastMath.sin(z).multiply(ratio);
        final T fz = FastMath.sqrt(coef.multiply(coef).negate().add(1.0)).reciprocal();
        return fz;
    }

    /** Compute Ionospheric Pierce Point.
     * <p>
     * The pierce point is computed assuming a spherical ionospheric shell above mean Earth radius.
     * </p>
     * @param date computation date
     * @param recPoint point at receiver station in body frame
     * @param los normalized line of sight in body frame
     * @param bodyShape shape of the body
     * @return pierce point, or null if recPoint is above ionosphere single layer
     * @since 12.0
     */
    private GeodeticPoint piercePoint(final AbsoluteDate date,
                                      final Vector3D recPoint, final Vector3D los,
                                      final BodyShape bodyShape) {

        final TECMapPair pair = getPairAtDate(date);
        final double     r    = pair.r0 + pair.h;
        final double     r2   = r * r;
        final double     p2   = recPoint.getNormSq();
        if (p2 >= r2) {
            // we are above ionosphere single layer
            return null;
        }

        // compute positive k such that recPoint + k los is on the spherical shell at radius r
        final double dot = Vector3D.dotProduct(recPoint, los);
        final double k   = FastMath.sqrt(dot * dot + r2 - p2) - dot;

        // Ionosphere Pierce Point in body frame
        final Vector3D ipp = new Vector3D(1, recPoint, k, los);

        return bodyShape.transform(ipp, bodyShape.getBodyFrame(), null);

    }

    /** Parser for IONEX files. */
    private class Parser implements DataLoader {

        /** String for the end of a TEC map. */
        private static final String END = "END OF TEC MAP";

        /** String for the epoch of a TEC map. */
        private static final String EPOCH = "EPOCH OF CURRENT MAP";

        /** Index of label in data lines. */
        private static final int LABEL_START = 60;

        /** Kilometers to meters conversion factor. */
        private static final double KM_TO_M = 1000.0;

        /** Header of the IONEX file. */
        private IONEXHeader header;

        /** List of TEC Maps. */
        private List<TECMap> maps;

        @Override
        public boolean stillAcceptsData() {
            return true;
        }

        @Override
        public void loadData(final InputStream input, final String name)
            throws IOException {

            maps = new ArrayList<>();

            // Open stream and parse data
            int   lineNumber = 0;
            String line      = null;
            try (InputStreamReader isr = new InputStreamReader(input, StandardCharsets.UTF_8);
                 BufferedReader    br  = new BufferedReader(isr)) {

                // Placeholders for parsed data
                int               nbOfMaps    = 1;
                int               exponent    = -1;
                double            baseRadius  = Double.NaN;
                double            hIon        = Double.NaN;
                boolean           mappingF    = false;
                boolean           inTEC       = false;
                double[]          latitudes   = null;
                double[]          longitudes  = null;
                AbsoluteDate      firstEpoch  = null;
                AbsoluteDate      lastEpoch   = null;
                AbsoluteDate      epoch       = firstEpoch;
                ArrayList<Double> values      = new ArrayList<>();

                for (line = br.readLine(); line != null; line = br.readLine()) {
                    ++lineNumber;
                    if (line.length() > LABEL_START) {
                        switch (line.substring(LABEL_START).trim()) {
                            case "EPOCH OF FIRST MAP" :
                                firstEpoch = parseDate(line);
                                break;
                            case "EPOCH OF LAST MAP" :
                                lastEpoch = parseDate(line);
                                break;
                            case "INTERVAL" :
                                // ignored;
                                break;
                            case "# OF MAPS IN FILE" :
                                nbOfMaps = parseInt(line, 2, 4);
                                break;
                            case "BASE RADIUS" :
                                // Value is in kilometers
                                baseRadius = parseDouble(line, 2, 6) * KM_TO_M;
                                break;
                            case "MAPPING FUNCTION" :
                                mappingF = !parseString(line, 2, 4).equals("NONE");
                                break;
                            case "EXPONENT" :
                                exponent = parseInt(line, 4, 2);
                                break;
                            case "HGT1 / HGT2 / DHGT" :
                                if (parseDouble(line, 17, 3) == 0.0) {
                                    // Value is in kilometers
                                    hIon = parseDouble(line, 3, 5) * KM_TO_M;
                                }
                                break;
                            case "LAT1 / LAT2 / DLAT" :
                                latitudes = parseCoordinate(line);
                                break;
                            case "LON1 / LON2 / DLON" :
                                longitudes = parseCoordinate(line);
                                break;
                            case "END OF HEADER" :
                                // Check that latitude and longitude bondaries were found
                                if (latitudes == null || longitudes == null) {
                                    throw new OrekitException(OrekitMessages.NO_LATITUDE_LONGITUDE_BONDARIES_IN_IONEX_HEADER, name);
                                }
                                // Check that first and last epochs were found
                                if (firstEpoch == null || lastEpoch == null) {
                                    throw new OrekitException(OrekitMessages.NO_EPOCH_IN_IONEX_HEADER, name);
                                }
                                // At the end of the header, we build the IONEXHeader object
                                header = new IONEXHeader(nbOfMaps, baseRadius, hIon, mappingF);
                                break;
                            case "START OF TEC MAP" :
                                inTEC = true;
                                break;
                            case END :
                                final BilinearInterpolatingFunction tec = interpolateTEC(values, exponent, latitudes, longitudes);
                                final TECMap map = new TECMap(epoch, tec);
                                maps.add(map);
                                // Reset parameters
                                inTEC  = false;
                                values = new ArrayList<>();
                                epoch  = null;
                                break;
                            default :
                                if (inTEC) {
                                    // Date
                                    if (line.endsWith(EPOCH)) {
                                        epoch = parseDate(line);
                                    }
                                    // Fill TEC values list
                                    if (!line.endsWith("LAT/LON1/LON2/DLON/H") &&
                                        !line.endsWith(END) &&
                                        !line.endsWith(EPOCH)) {
                                        line = line.trim();
                                        final String[] readLine = SEPARATOR.split(line);
                                        for (final String s : readLine) {
                                            values.add(Double.parseDouble(s));
                                        }
                                    }
                                }
                                break;
                        }
                    } else {
                        if (inTEC) {
                            // Here, we are parsing the last line of TEC data for a given latitude
                            // The size of this line is lower than 60.
                            line = line.trim();
                            final String[] readLine = SEPARATOR.split(line);
                            for (final String s : readLine) {
                                values.add(Double.parseDouble(s));
                            }
                        }
                    }

                }

                // Close the stream after reading
                input.close();

            } catch (NumberFormatException nfe) {
                throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                          lineNumber, name, line);
            }

            // TEC map
            if (maps.size() != header.getTECMapsNumer()) {
                throw new OrekitException(OrekitMessages.INCONSISTENT_NUMBER_OF_TEC_MAPS_IN_FILE,
                                          maps.size(), header.getTECMapsNumer());
            }
            TECMap previous = null;
            for (TECMap current : maps) {
                if (previous != null) {
                    tecMap.addValidBetween(new TECMapPair(previous, current,
                                                          header.getEarthRadius(), header.getHIon(), header.isMappingFunction()),
                                           previous.date, current.date);
                }
                previous = current;
            }

            names = names.isEmpty() ? name : (names + ", " + name);

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

        /** Extract an integer from a line.
         * @param line to parse
         * @param start start index of the integer
         * @param length length of the integer
         * @return parsed integer
         */
        private int parseInt(final String line, final int start, final int length) {
            return Integer.parseInt(parseString(line, start, length));
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

        /** Extract a date from a parsed line.
         * @param line to parse
         * @return an absolute date
         */
        private AbsoluteDate parseDate(final String line) {
            return new AbsoluteDate(parseInt(line, 0, 6),
                                    parseInt(line, 6, 6),
                                    parseInt(line, 12, 6),
                                    parseInt(line, 18, 6),
                                    parseInt(line, 24, 6),
                                    parseDouble(line, 30, 13),
                                    utc);
        }

        /** Build the coordinate array from a parsed line.
         * @param line to parse
         * @return an array of coordinates in radians
         */
        private double[] parseCoordinate(final String line) {
            final double a = parseDouble(line, 2, 6);
            final double b = parseDouble(line, 8, 6);
            final double c = parseDouble(line, 14, 6);
            final double[] coordinate = new double[((int) FastMath.abs((a - b) / c)) + 1];
            int i = 0;
            for (double cor = FastMath.min(a, b); cor <= FastMath.max(a, b); cor += FastMath.abs(c)) {
                coordinate[i] = FastMath.toRadians(cor);
                i++;
            }
            return coordinate;
        }

        /** Interpolate the TEC in latitude and longitude.
         * @param exponent exponent defining the unit of the values listed in the data blocks
         * @param values TEC values
         * @param latitudes array containing the different latitudes in radians
         * @param longitudes array containing the different latitudes in radians
         * @return the interpolating TEC functiopn in TECUnits
         */
        private BilinearInterpolatingFunction interpolateTEC(final ArrayList<Double> values, final double exponent,
                                                             final double[] latitudes, final double[] longitudes) {
            // Array dimensions
            final int dimLat = latitudes.length;
            final int dimLon = longitudes.length;

            // Build the array of TEC data
            final double factor = FastMath.pow(10.0, exponent);
            final double[][] fvalTEC = new double[dimLat][dimLon];
            int index = dimLon * dimLat;
            for (int x = 0; x < dimLat; x++) {
                for (int y = dimLon - 1; y >= 0; y--) {
                    index = index - 1;
                    fvalTEC[x][y] = values.get(index) * factor;
                }
            }

            // Build Bilinear Interpolation function
            return new BilinearInterpolatingFunction(latitudes, longitudes, fvalTEC);

        }

    }

    /**
     * Container for IONEX data.
     * <p>
     * The TEC contained in the map is previously interpolated
     * according to the latitude and the longitude given by the user.
     * </p>
     */
    private static class TECMap {

        /** Date of the TEC Map. */
        private AbsoluteDate date;

        /** Interpolated TEC [TECUnits]. */
        private BilinearInterpolatingFunction tec;

        /**
         * Constructor.
         * @param date date of the TEC map
         * @param tec interpolated tec
         */
        TECMap(final AbsoluteDate date, final BilinearInterpolatingFunction tec) {
            this.date = date;
            this.tec  = tec;
        }

    }

    /** Container for a consecutive pair of TEC maps.
     * @since 12.0
     */
    private static class TECMapPair {

        /** First snapshot. */
        private final TECMap first;

        /** Second snapshot. */
        private final TECMap second;

        /** Mean earth radius [m]. */
        private double r0;

        /** Height of the ionospheric single layer [m]. */
        private double h;

        /** Flag for mapping function computation. */
        private boolean mapping;

        /** Simple constructor.
         * @param first first snapshot
         * @param second second snapshot
         * @param r0 mean Earth radius
         * @param h height of the ionospheric layer
         * @param mapping flag for mapping computation
         */
        TECMapPair(final TECMap first, final TECMap second,
                   final double r0, final double h, final boolean mapping) {
            this.first   = first;
            this.second  = second;
            this.r0      = r0;
            this.h       = h;
            this.mapping = mapping;
        }

        /** Get TEC at pierce point.
         * @param date date
         * @param ipp Ionospheric Pierce Point
         * @return TEC
         */
        public double getTEC(final AbsoluteDate date, final GeodeticPoint ipp) {
            // Get the TEC values at the two closest dates
            final AbsoluteDate t1   = first.date;
            final double       tec1 = first.tec.value(ipp.getLatitude(), ipp.getLongitude());
            final AbsoluteDate t2   = second.date;
            final double       tec2 = second.tec.value(ipp.getLatitude(), ipp.getLongitude());
            final double       dt   = t2.durationFrom(t1);

            // Perform temporal interpolation (Ref, Eq. 2)
            return (t2.durationFrom(date) / dt) * tec1 + (date.durationFrom(t1) / dt) * tec2;

        }

        /** Get TEC at pierce point.
         * @param date date
         * @param ipp Ionospheric Pierce Point
         * @param <T> type of the field elements
         * @return TEC
         */
        public <T extends CalculusFieldElement<T>> T getTEC(final FieldAbsoluteDate<T> date, final GeodeticPoint ipp) {

            // Get the TEC values at the two closest dates
            final AbsoluteDate t1   = first.date;
            final double       tec1 = first.tec.value(ipp.getLatitude(), ipp.getLongitude());
            final AbsoluteDate t2   = second.date;
            final double       tec2 = second.tec.value(ipp.getLatitude(), ipp.getLongitude());
            final double       dt   = t2.durationFrom(t1);

            // Perform temporal interpolation (Ref, Eq. 2)
            return date.durationFrom(t2).negate().divide(dt).multiply(tec1).add(date.durationFrom(t1).divide(dt).multiply(tec2));

        }

    }

    /** Container for IONEX header. */
    private static class IONEXHeader {

        /** Number of maps contained in the IONEX file. */
        private int nbOfMaps;

        /** Mean earth radius [m]. */
        private double baseRadius;

        /** Height of the ionospheric single layer [m]. */
        private double hIon;

        /** Flag for mapping function adopted for TEC determination. */
        private boolean isMappingFunction;

        /**
         * Constructor.
         * @param nbOfMaps number of TEC maps contained in the file
         * @param baseRadius mean earth radius in meters
         * @param hIon height of the ionospheric single layer in meters
         * @param mappingFunction flag for mapping function adopted for TEC determination
         */
        IONEXHeader(final int nbOfMaps,
                    final double baseRadius, final double hIon,
                    final boolean mappingFunction) {
            this.nbOfMaps          = nbOfMaps;
            this.baseRadius        = baseRadius;
            this.hIon              = hIon;
            this.isMappingFunction = mappingFunction;
        }

        /**
         * Get the number of TEC maps contained in the file.
         * @return the number of TEC maps
         */
        public int getTECMapsNumer() {
            return nbOfMaps;
        }

        /**
         * Get the mean earth radius in meters.
         * @return the mean earth radius
         */
        public double getEarthRadius() {
            return baseRadius;
        }

        /**
         * Get the height of the ionospheric single layer in meters.
         * @return the height of the ionospheric single layer
         */
        public double getHIon() {
            return hIon;
        }

        /**
         * Get the mapping function flag.
         * @return false if mapping function computation is needed
         */
        public boolean isMappingFunction() {
            return isMappingFunction;
        }

    }

}
