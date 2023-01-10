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
package org.orekit.models.earth.weather;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.ToDoubleFunction;
import java.util.regex.Pattern;

import org.hipparchus.analysis.interpolation.BilinearInterpolatingFunction;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.hipparchus.util.SinCos;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.data.DataLoader;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.models.earth.Geoid;
import org.orekit.models.earth.troposphere.ViennaOneModel;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.utils.Constants;

/** The Global Pressure and Temperature 2 (GPT2) model.
 * This model is an empirical model that provides the temperature, the pressure and the water vapor pressure
 * of a site depending its latitude and  longitude. This model also provides the a<sub>h</sub>
 * and a<sub>w</sub> coefficients used for the {@link ViennaOneModel Vienna 1} model.
 * <p>
 * The requisite coefficients for the computation of the weather parameters are provided by the
 * Department of Geodesy and Geoinformation of the Vienna University. They are based on an
 * external grid file like "gpt2_1.grd" (1° x 1°) or "gpt2_5.grd" (5° x 5°) available at:
 * <a href="http://vmf.geo.tuwien.ac.at/codes/"> link</a>
 * </p>
 * <p>
 * A bilinear interpolation is performed in order to obtained the correct values of the weather parameters.
 * </p>
 * <p>
 * The format is always the same, with and example shown below for the pressure and the temperature.
 * <p>
 * Example:
 * </p>
 * <pre>
 * %  lat    lon   p:a0    A1   B1   A2   B2  T:a0    A1   B1   A2   B2
 *   87.5    2.5 101421    21  409 -217 -122 259.2 -13.2 -6.1  2.6  0.3
 *   87.5    7.5 101416    21  411 -213 -120 259.3 -13.1 -6.1  2.6  0.3
 *   87.5   12.5 101411    22  413 -209 -118 259.3 -13.1 -6.1  2.6  0.3
 *   87.5   17.5 101407    23  415 -205 -116 259.4 -13.0 -6.1  2.6  0.3
 *   ...
 * </pre>
 *
 * @see "K. Lagler, M. Schindelegger, J. Böhm, H. Krasna, T. Nilsson (2013),
 * GPT2: empirical slant delay model for radio space geodetic techniques. Geophys
 * Res Lett 40(6):1069–1073. doi:10.1002/grl.50288"
 *
 * @author Bryan Cazabonne
 *
 */
public class GlobalPressureTemperature2Model implements WeatherModel {

    /** Default supported files name pattern. */
    public static final String DEFAULT_SUPPORTED_NAMES = "gpt2_\\d+.grd";

    /** Pattern for delimiting regular expressions. */
    private static final Pattern SEPARATOR = Pattern.compile("\\s+");

    /** Standard gravity constant [m/s²]. */
    private static final double G = Constants.G0_STANDARD_GRAVITY;

    /** Ideal gas constant for dry air [J/kg/K]. */
    private static final double R = 287.0;

    /** Conversion factor from degrees to mill arcseconds. */
    private static final int DEG_TO_MAS = 3600000;

    /** Shared lazily loaded grid. */
    private static final AtomicReference<Grid> SHARED_GRID = new AtomicReference<>(null);

    /** South-West grid entry. */
    private final GridEntry southWest;

    /** South-East grid entry. */
    private final GridEntry southEast;

    /** North-West grid entry. */
    private final GridEntry northWest;

    /** North-East grid entry. */
    private final GridEntry northEast;

    /** The hydrostatic and wet a coefficients loaded. */
    private double[] coefficientsA;

    /** Geodetic site latitude, radians.*/
    private double latitude;

    /** Geodetic site longitude, radians.*/
    private double longitude;

    /** Temperature site, in kelvins. */
    private double temperature;

    /** Pressure site, in hPa. */
    private double pressure;

    /** water vapour pressure, in hPa. */
    private double e0;

    /** Geoid used to compute the undulations. */
    private final Geoid geoid;

    /** UTC time scale. */
    private final TimeScale utc;

    /**
     * Constructor with supported names given by user. This constructor uses the {@link
     * DataContext#getDefault() default data context}.
     *
     * @param supportedNames supported names
     * @param latitude geodetic latitude of the station, in radians
     * @param longitude longitude geodetic longitude of the station, in radians
     * @param geoid level surface of the gravity potential of a body
     * @see #GlobalPressureTemperature2Model(String, double, double, Geoid,
     * DataProvidersManager, TimeScale)
     */
    @DefaultDataContext
    public GlobalPressureTemperature2Model(final String supportedNames, final double latitude,
                                           final double longitude, final Geoid geoid) {
        this(supportedNames, latitude, longitude, geoid,
                DataContext.getDefault().getDataProvidersManager(),
                DataContext.getDefault().getTimeScales().getUTC());
    }

    /**
     * Constructor with supported names and source of GPT2 auxiliary data given by user.
     *
     * @param supportedNames supported names
     * @param latitude geodetic latitude of the station, in radians
     * @param longitude longitude geodetic longitude of the station, in radians
     * @param geoid level surface of the gravity potential of a body
     * @param dataProvidersManager provides access to auxiliary data.
     * @param utc UTC time scale.
     * @since 10.1
     */
    public GlobalPressureTemperature2Model(final String supportedNames,
                                           final double latitude,
                                           final double longitude,
                                           final Geoid geoid,
                                           final DataProvidersManager dataProvidersManager,
                                           final TimeScale utc) {
        this.coefficientsA = null;
        this.temperature   = Double.NaN;
        this.pressure      = Double.NaN;
        this.e0            = Double.NaN;
        this.geoid         = geoid;
        this.latitude      = latitude;
        this.utc = utc;

        // get the lazily loaded shared grid
        Grid grid = SHARED_GRID.get();
        if (grid == null) {
            // this is the first instance we create, we need to load the grid data
            final Parser parser = new Parser();
            dataProvidersManager.feed(supportedNames, parser);
            SHARED_GRID.compareAndSet(null, parser.grid);
            grid = parser.grid;
        }

        // Normalize longitude according to the grid
        this.longitude = MathUtils.normalizeAngle(longitude, grid.entries[0][0].longitude + FastMath.PI);

        final int southIndex = grid.getSouthIndex(this.latitude);
        final int westIndex  = grid.getWestIndex(this.longitude);
        this.southWest = grid.entries[southIndex    ][westIndex    ];
        this.southEast = grid.entries[southIndex    ][westIndex + 1];
        this.northWest = grid.entries[southIndex + 1][westIndex    ];
        this.northEast = grid.entries[southIndex + 1][westIndex + 1];

    }

    /**
     * Constructor with default supported names. This constructor uses the {@link
     * DataContext#getDefault() default data context}.
     *
     * @param latitude geodetic latitude of the station, in radians
     * @param longitude geodetic latitude of the station, in radians
     * @param geoid level surface of the gravity potential of a body
     * @see #GlobalPressureTemperature2Model(String, double, double, Geoid,
     * DataProvidersManager, TimeScale)
     */
    @DefaultDataContext
    public GlobalPressureTemperature2Model(final double latitude, final double longitude, final Geoid geoid) {
        this(DEFAULT_SUPPORTED_NAMES, latitude, longitude, geoid);
    }

    /** Returns the a coefficients array.
     * <ul>
     * <li>double[0] = a<sub>h</sub>
     * <li>double[1] = a<sub>w</sub>
     * </ul>
     * @return the a coefficients array
     */
    public double[] getA() {
        return coefficientsA.clone();
    }

    /** Returns the temperature at the station [K].
     * @return the temperature at the station [K]
     */
    public double getTemperature() {
        return temperature;
    }

    /** Returns the pressure at the station [hPa].
     * @return the pressure at the station [hPa]
     */
    public double getPressure() {
        return pressure;
    }

    /** Returns the water vapor pressure at the station [hPa].
     * @return the water vapor pressure at the station [hPa]
     */
    public double getWaterVaporPressure() {
        return e0;
    }

    @Override
    public void weatherParameters(final double stationHeight, final AbsoluteDate currentDate) {

        final int dayOfYear = currentDate.getComponents(utc).getDate().getDayOfYear();

        // ah and aw coefficients
        coefficientsA = new double[] {
            interpolate(e -> evaluate(dayOfYear, e.ah)) * 0.001,
            interpolate(e -> evaluate(dayOfYear, e.aw)) * 0.001
        };

        // Corrected height (can be negative)
        final double undu            = geoid.getUndulation(latitude, longitude, currentDate);
        final double correctedheight = stationHeight - undu - interpolate(e -> e.hS);

        // Temperature gradient [K/m]
        final double dTdH = interpolate(e -> evaluate(dayOfYear, e.dT)) * 0.001;

        // Specific humidity
        final double qv = interpolate(e -> evaluate(dayOfYear, e.qv0)) * 0.001;

        // For the computation of the temperature and the pressure, we use
        // the standard ICAO atmosphere formulas.

        // Temperature [K]
        final double t0 = interpolate(e -> evaluate(dayOfYear, e.temperature0));
        this.temperature = t0 + dTdH * correctedheight;

        // Pressure [hPa]
        final double p0 = interpolate(e -> evaluate(dayOfYear, e.pressure0));
        final double exponent = G / (dTdH * R);
        this.pressure = p0 * FastMath.pow(1 - (dTdH / t0) * correctedheight, exponent) * 0.01;

        // Water vapor pressure [hPa]
        this.e0 = qv * pressure / (0.622 + 0.378 * qv);

    }

    /** Interpolate a grid function.
     * @param gridGetter getter for the grid function
     * @return interpolated function"
     */
    private double interpolate(final ToDoubleFunction<GridEntry> gridGetter) {

        // cell surrounding the point
        final double[] xVal = new double[] {
            southWest.longitude, southEast.longitude
        };
        final double[] yVal = new double[] {
            southWest.latitude, northWest.latitude
        };

        // evaluate grid points at specified day
        final double[][] fval = new double[][] {
            {
                gridGetter.applyAsDouble(southWest),
                gridGetter.applyAsDouble(northWest)
            }, {
                gridGetter.applyAsDouble(southEast),
                gridGetter.applyAsDouble(northEast)
            }
        };

        // perform interpolation in the grid
        return new BilinearInterpolatingFunction(xVal, yVal, fval).value(longitude, latitude);

    }

    /** Evaluate a model for some day.
     * @param dayOfYear day to evaluate
     * @param model model array
     * @return model value at specified day
     */
    private double evaluate(final int dayOfYear, final double[] model) {

        final double coef = (dayOfYear / 365.25) * 2 * FastMath.PI;
        final SinCos sc1  = FastMath.sinCos(coef);
        final SinCos sc2  = FastMath.sinCos(2.0 * coef);

        return model[0] +
               model[1] * sc1.cos() + model[2] * sc1.sin() +
               model[3] * sc2.cos() + model[4] * sc2.sin();

    }

    /** Parser for GPT2 grid files. */
    private static class Parser implements DataLoader {

        /** Grid entries. */
        private Grid grid;

        @Override
        public boolean stillAcceptsData() {
            return grid == null;
        }

        @Override
        public void loadData(final InputStream input, final String name)
            throws IOException, ParseException {

            final SortedSet<Integer> latSample = new TreeSet<>();
            final SortedSet<Integer> lonSample = new TreeSet<>();
            final List<GridEntry>    entries   = new ArrayList<>();

            // Open stream and parse data
            int   lineNumber = 0;
            String line      = null;
            try (InputStreamReader isr = new InputStreamReader(input, StandardCharsets.UTF_8);
                 BufferedReader    br = new BufferedReader(isr)) {

                for (line = br.readLine(); line != null; line = br.readLine()) {
                    ++lineNumber;
                    line = line.trim();

                    // read grid data
                    if (line.length() > 0 && !line.startsWith("%")) {
                        final GridEntry entry = new GridEntry(SEPARATOR.split(line));
                        latSample.add(entry.latKey);
                        lonSample.add(entry.lonKey);
                        entries.add(entry);
                    }

                }
            } catch (NumberFormatException nfe) {
                throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                          lineNumber, name, line);
            }

            // organize entries in a grid that wraps arouns Earth in longitude
            grid = new Grid(latSample, lonSample, entries, name);

        }

    }

    /** Container for complete grid. */
    private static class Grid {

        /** Latitude sample. */
        private final SortedSet<Integer> latitudeSample;

        /** Longitude sample. */
        private final SortedSet<Integer> longitudeSample;

        /** Grid entries. */
        private final GridEntry[][] entries;

        /** Simple constructor.
         * @param latitudeSample latitude sample
         * @param longitudeSample longitude sample
         * @param loadedEntries loaded entries, organized as a simple list
         * @param name file name
         */
        Grid(final SortedSet<Integer> latitudeSample, final SortedSet<Integer> longitudeSample,
             final List<GridEntry> loadedEntries, final String name) {

            final int nA         = latitudeSample.size();
            final int nO         = longitudeSample.size() + 1; // we add one here for wrapping the grid
            this.entries         = new GridEntry[nA][nO];
            this.latitudeSample  = latitudeSample;
            this.longitudeSample = longitudeSample;

            // organize entries in the regular grid
            for (final GridEntry entry : loadedEntries) {
                final int latitudeIndex  = latitudeSample.headSet(entry.latKey + 1).size() - 1;
                final int longitudeIndex = longitudeSample.headSet(entry.lonKey + 1).size() - 1;
                entries[latitudeIndex][longitudeIndex] = entry;
            }

            // finalize the grid
            for (final GridEntry[] row : entries) {

                // check for missing entries
                for (int longitudeIndex = 0; longitudeIndex < nO - 1; ++longitudeIndex) {
                    if (row[longitudeIndex] == null) {
                        throw new OrekitException(OrekitMessages.IRREGULAR_OR_INCOMPLETE_GRID, name);
                    }
                }

                // wrap the grid around the Earth in longitude
                row[nO - 1] = new GridEntry(row[0].latitude, row[0].latKey,
                                            row[0].longitude + 2 * FastMath.PI,
                                            row[0].lonKey + DEG_TO_MAS * 360,
                                            row[0].hS, row[0].pressure0, row[0].temperature0,
                                            row[0].qv0, row[0].dT, row[0].ah, row[0].aw);

            }

        }

        /** Get index of South entries in the grid.
         * @param latitude latitude to locate (radians)
         * @return index of South entries in the grid
         */
        public int getSouthIndex(final double latitude) {

            final int latKey = (int) FastMath.rint(FastMath.toDegrees(latitude) * DEG_TO_MAS);
            final int index  = latitudeSample.headSet(latKey + 1).size() - 1;

            // make sure we have at least one point remaining on North by clipping to size - 2
            return FastMath.min(index, latitudeSample.size() - 2);

        }

        /** Get index of West entries in the grid.
         * @param longitude longitude to locate (radians)
         * @return index of West entries in the grid
         */
        public int getWestIndex(final double longitude) {

            final int lonKey = (int) FastMath.rint(FastMath.toDegrees(longitude) * DEG_TO_MAS);
            final int index  = longitudeSample.headSet(lonKey + 1).size() - 1;

            // we don't do clipping in longitude because we have added a row to wrap around the Earth
            return index;

        }

    }

    /** Container for grid entries. */
    private static class GridEntry {

        /** Latitude (radian). */
        private final double latitude;

        /** Latitude key (mas). */
        private final int latKey;

        /** Longitude (radian). */
        private final double longitude;

        /** Longitude key (mas). */
        private final int lonKey;

        /** Height correction. */
        private final double hS;

        /** Pressure model. */
        private final double[] pressure0;

        /** Temperature model. */
        private final double[] temperature0;

        /** Specific humidity model. */
        private final double[] qv0;

        /** Temperature gradient model. */
        private final double[] dT;

        /** ah coefficient model. */
        private final double[] ah;

        /** aw coefficient model. */
        private final double[] aw;

        /** Build an entry from a parsed line.
         * @param fields line fields
         */
        GridEntry(final String[] fields) {

            final double latDegree = Double.parseDouble(fields[0]);
            final double lonDegree = Double.parseDouble(fields[1]);
            latitude     = FastMath.toRadians(latDegree);
            longitude    = FastMath.toRadians(lonDegree);
            latKey       = (int) FastMath.rint(latDegree * DEG_TO_MAS);
            lonKey       = (int) FastMath.rint(lonDegree * DEG_TO_MAS);

            hS           = Double.parseDouble(fields[23]);

            pressure0    = createModel(fields, 2);
            temperature0 = createModel(fields, 7);
            qv0          = createModel(fields, 12);
            dT           = createModel(fields, 17);
            ah           = createModel(fields, 24);
            aw           = createModel(fields, 29);

        }

        /** Build an entry from its components.
         * @param latitude latitude (radian)
         * @param latKey latitude key (mas)
         * @param longitude longitude (radian)
         * @param lonKey longitude key (mas)
         * @param hS height correction
         * @param pressure0 pressure model
         * @param temperature0 temperature model
         * @param qv0 specific humidity model
         * @param dT temperature gradient model
         * @param ah ah coefficient model
         * @param aw aw coefficient model
         */
        GridEntry(final double latitude, final int latKey, final double longitude, final int lonKey,
                  final double hS, final double[] pressure0, final double[] temperature0,
                  final double[] qv0, final double[] dT, final double[] ah, final double[] aw) {

            this.latitude     = latitude;
            this.latKey       = latKey;
            this.longitude    = longitude;
            this.lonKey       = lonKey;
            this.hS           = hS;
            this.pressure0    = pressure0.clone();
            this.temperature0 = temperature0.clone();
            this.qv0          = qv0.clone();
            this.dT           = dT.clone();
            this.ah           = ah.clone();
            this.aw           = aw.clone();
        }

        /** Create a time model array.
         * @param fields line fields
         * @param first index of the first component of the model
         * @return time model array
         */
        private double[] createModel(final String[] fields, final int first) {
            return new double[] {
                Double.parseDouble(fields[first    ]),
                Double.parseDouble(fields[first + 1]),
                Double.parseDouble(fields[first + 2]),
                Double.parseDouble(fields[first + 3]),
                Double.parseDouble(fields[first + 4])
            };
        }

    }

}
