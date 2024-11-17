/* Copyright 2002-2024 Thales Alenia Space
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
package org.orekit.models.earth.troposphere.iturp834;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.analysis.interpolation.GridAxis;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.models.earth.weather.FieldPressureTemperatureHumidity;
import org.orekit.models.earth.weather.PressureTemperatureHumidity;
import org.orekit.models.earth.weather.PressureTemperatureHumidityProvider;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScale;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/** The ITU-R P.834 weather parameters.
 * <p>
 * This class implements the weather parameters part of the model,
 * i.e. equations 26a to 26 f in section 6 of the recommendation.
 * </p>
 * @author Luc Maisonobe
 * @see <a href="https://www.itu.int/rec/R-REC-P.834/en">P.834 : Effects of tropospheric refraction on radiowave propagation</>
 * @since 13.0
 */
public class ITURP834WeatherParameters implements PressureTemperatureHumidityProvider {

    /** ITU-R P.834 data resources directory. */
    private static final String ITU_R_P_834 = "/assets/org/orekit/ITU-R-P.834/";

    /** Pattern for splitting fields. */
    private static final Pattern SPLITTER = Pattern.compile("\\s+");

    /** Minimum longitude (degrees). */
    private static final double MIN_LON = -180.0;

    /** Maximum longitude (degrees). */
    private static final double MAX_LON =  180.0;

    /** Minimum latitude (degrees). */
    private static final double MIN_LAT = -90.0;

    /** Maximum latitude (degrees). */
    private static final double MAX_LAT =  90.0;

    /** Grid step (degrees). */
    private static final double STEP    =   1.5;

    /** Latitude grid axis. */
    private static final GridAxis LATITUDE_AXIS;

    /** Longitude grid axis. */
    private static final GridAxis LONGITUDE_AXIS;

    /** Annual pulsation. */
    private static final double OMEGA = MathUtils.TWO_PI / 365.25;

    /** Gravity factor for equation 27g. */
    private static final double G_27G = 9.806;

    /** Gravity latitude correction factor for equation 27g. */
    private static final double GL_27G = 0.002637;

    /** Gravity altitude correction factor for equation 27g (rescaled for altitudes in meters). */
    private static final double GH_27G = 3.1e-7;

    /** Gravity factor for equation 27j. */
    private static final double G_27J = 9.784;

    /** Gravity latitude correction factor for equation 27j. */
    private static final double GL_27J = 0.00266;

    /** Gravity altitude correction factor for equation 27j (rescaled for altitudes in meters). */
    private static final double GH_27J = 2.8e-7;

    /** Molar gas constant (J/mol K). */
    private static final double R = 8.314;

    /** Dry air molar mass (kg/mol). */
    private static final double MD = 0.0289644;

    /** R'd factor. **/
    private static final double R_PRIME_D = R / (1000 * MD);

    /** Average of air total pressure at the Earth surface. */
    private static final double[][] AIR_TOTAL_PRESSURE_AVERAGE;

    /** Seasonal fluctuation of air total pressure at the Earth surface. */
    private static final double[][] AIR_TOTAL_PRESSURE_SEASONAL;

    /** Day of minimum of air total pressure at the Earth surface. */
    private static final double[][] AIR_TOTAL_PRESSURE_MINIMUM;

    /** Average of water vapour partial pressure at the Earth surface. */
    private static final double[][] WATER_VAPOUR_PARTIAL_PRESSURE_AVERAGE;

    /** Seasonal fluctuation of water vapour partial pressure at the Earth surface. */
    private static final double[][] WATER_VAPOUR_PARTIAL_PRESSURE_SEASONAL;

    /** Day of minimum of water vapour partial pressure at the Earth surface. */
    private static final double[][] WATER_VAPOUR_PARTIAL_PRESSURE_MINIMUM;

    /** Average of mean temperature of the water vapour column above the surface. */
    private static final double[][] MEAN_TEMPERATURE_AVERAGE;

    /** Seasonal fluctuation of mean temperature of the water vapour column above the surface. */
    private static final double[][] MEAN_TEMPERATURE_SEASONAL;

    /** Day of minimum of mean temperature of the water vapour column above the surface. */
    private static final double[][] MEAN_TEMPERATURE_MINIMUM;

    /** Average of vapour pressure decrease factor. */
    private static final double[][] VAPOUR_PRESSURE_DECREASE_FACTOR_AVERAGE;

    /** Seasonal fluctuation of vapour pressure decrease factor. */
    private static final double[][] VAPOUR_PRESSURE_DECREASE_FACTOR_SEASONAL;

    /** Day of minimum of vapour pressure decrease factor. */
    private static final double[][] VAPOUR_PRESSURE_DECREASE_FACTOR_MINIMUM;

    /** Average of lapse rate of mean temperature of water vapour from Earth surface. */
    private static final double[][] LAPSE_RATE_MEAN_TEMPERATURE_AVERAGE;

    /** Seasonal fluctuation of lapse rate of mean temperature of water vapour from Earth surface. */
    private static final double[][] LAPSE_RATE_MEAN_TEMPERATURE_SEASONAL;

    /** Day of minimum of lapse rate of mean temperature of water vapour from Earth surface. */
    private static final double[][] LAPSE_RATE_MEAN_TEMPERATURE_MINIMUM;

    /** Average height of reference level with respect to mean seal level. */
    private static final double[][] AVERAGE_HEIGHT_REFERENCE_LEVEL;

    /** Gravity at Earth surface (from equaiton 27g). */
    private static final double[][] G;

   /** Gravity at Earth surface (from equation 27j). */
    private static final double[][] GM;

    /** UTC time scale to evaluate time-dependent tables. */
    private final TimeScale utc;

    // load all model data files
    static {

        // build axes
        LATITUDE_AXIS  = buildAxis(MIN_LAT, MAX_LAT, STEP);
        LONGITUDE_AXIS = buildAxis(MIN_LON, MAX_LON, STEP);

        // load data files
        AIR_TOTAL_PRESSURE_AVERAGE               = parse(MeteorologicalParameter.AIR_TOTAL_PRESSURE.averageValue());
        AIR_TOTAL_PRESSURE_SEASONAL              = parse(MeteorologicalParameter.AIR_TOTAL_PRESSURE.seasonalFluctuation());
        AIR_TOTAL_PRESSURE_MINIMUM               = parse(MeteorologicalParameter.AIR_TOTAL_PRESSURE.dayMinimum());
        WATER_VAPOUR_PARTIAL_PRESSURE_AVERAGE    = parse(MeteorologicalParameter.WATER_VAPOUR_PARTIAL_PRESSURE.averageValue());
        WATER_VAPOUR_PARTIAL_PRESSURE_SEASONAL   = parse(MeteorologicalParameter.WATER_VAPOUR_PARTIAL_PRESSURE.seasonalFluctuation());
        WATER_VAPOUR_PARTIAL_PRESSURE_MINIMUM    = parse(MeteorologicalParameter.WATER_VAPOUR_PARTIAL_PRESSURE.dayMinimum());
        MEAN_TEMPERATURE_AVERAGE                 = parse(MeteorologicalParameter.MEAN_TEMPERATURE.averageValue());
        MEAN_TEMPERATURE_SEASONAL                = parse(MeteorologicalParameter.MEAN_TEMPERATURE.seasonalFluctuation());
        MEAN_TEMPERATURE_MINIMUM                 = parse(MeteorologicalParameter.MEAN_TEMPERATURE.dayMinimum());
        VAPOUR_PRESSURE_DECREASE_FACTOR_AVERAGE  = parse(MeteorologicalParameter.VAPOUR_PRESSURE_DECREASE_FACTOR.averageValue());
        VAPOUR_PRESSURE_DECREASE_FACTOR_SEASONAL = parse(MeteorologicalParameter.VAPOUR_PRESSURE_DECREASE_FACTOR.seasonalFluctuation());
        VAPOUR_PRESSURE_DECREASE_FACTOR_MINIMUM  = parse(MeteorologicalParameter.VAPOUR_PRESSURE_DECREASE_FACTOR.dayMinimum());
        LAPSE_RATE_MEAN_TEMPERATURE_AVERAGE      = parse(MeteorologicalParameter.LAPSE_RATE_MEAN_TEMPERATURE.averageValue());
        LAPSE_RATE_MEAN_TEMPERATURE_SEASONAL     = parse(MeteorologicalParameter.LAPSE_RATE_MEAN_TEMPERATURE.seasonalFluctuation());
        LAPSE_RATE_MEAN_TEMPERATURE_MINIMUM      = parse(MeteorologicalParameter.LAPSE_RATE_MEAN_TEMPERATURE.dayMinimum());
        AVERAGE_HEIGHT_REFERENCE_LEVEL           = parse("hreflev.dat");

        // precompute gravity at Earth surface throughout the grid
        G  = new double[LATITUDE_AXIS.size()][LONGITUDE_AXIS.size()];
        GM = new double[LATITUDE_AXIS.size()][LONGITUDE_AXIS.size()];
        for (int i = 0; i < LATITUDE_AXIS.size(); i++) {
            for (int j = 0; j < LONGITUDE_AXIS.size(); j++) {
                final double cos = FastMath.cos(2 * LATITUDE_AXIS.node(i));
                final double h   = AVERAGE_HEIGHT_REFERENCE_LEVEL[i][j];
                // equation 27g
                G[i][j]  = G_27G * ((1 - GL_27G * cos) - GH_27G * h);
                // equation 27j
                GM[i][j] = G_27J * ((1 - GL_27J * cos) - GH_27J * h);
            }
        }
    }

    /** Simple constructor.
     * @param utc UTC time scale to evaluate time-dependent tables
     */
    public ITURP834WeatherParameters(final TimeScale utc) {
        this.utc = utc;
    }

    /** {@inheritDoc} */
    @Override
    public PressureTemperatureHumidity getWeatherParameters(final GeodeticPoint location, final AbsoluteDate date) {

        // locate the point in the tables
        final int    southIndex = LATITUDE_AXIS.interpolationIndex(location.getLatitude());
        final int    westIndex  = LONGITUDE_AXIS.interpolationIndex(location.getLongitude());

        // evaluate grid points for current date at reference height
        final double doy        = date.getDayOfYear(utc);
        final Cell   pHRef      = new Cell(AIR_TOTAL_PRESSURE_AVERAGE,
                                           AIR_TOTAL_PRESSURE_SEASONAL,
                                           AIR_TOTAL_PRESSURE_MINIMUM,
                                           southIndex, westIndex, doy);
        final Cell   eHRef      = new Cell(WATER_VAPOUR_PARTIAL_PRESSURE_AVERAGE,
                                           WATER_VAPOUR_PARTIAL_PRESSURE_SEASONAL,
                                           WATER_VAPOUR_PARTIAL_PRESSURE_MINIMUM,
                                           southIndex, westIndex, doy);
        final Cell   tmHRef     = new Cell(MEAN_TEMPERATURE_AVERAGE,
                                           MEAN_TEMPERATURE_SEASONAL,
                                           MEAN_TEMPERATURE_MINIMUM,
                                           southIndex, westIndex, doy);
        final Cell   lambdaHRef = new Cell(VAPOUR_PRESSURE_DECREASE_FACTOR_AVERAGE,
                                           VAPOUR_PRESSURE_DECREASE_FACTOR_SEASONAL,
                                           VAPOUR_PRESSURE_DECREASE_FACTOR_MINIMUM,
                                           southIndex, westIndex, doy);
        final Cell   alphaHRef  = new Cell(LAPSE_RATE_MEAN_TEMPERATURE_AVERAGE,
                                           LAPSE_RATE_MEAN_TEMPERATURE_SEASONAL,
                                           LAPSE_RATE_MEAN_TEMPERATURE_MINIMUM,
                                           southIndex, westIndex, doy);
        // reference height
        final Cell   hRef  = new Cell(AVERAGE_HEIGHT_REFERENCE_LEVEL, southIndex, westIndex);

        // exponent
        final Cell   g     = new Cell(G, southIndex, westIndex);

        // mean temperature at current height, equation 27b
        final Cell dh              = new Cell(location.getAltitude() - hRef.nw,
                                              location.getAltitude() - hRef.sw,
                                              location.getAltitude() - hRef.se,
                                              location.getAltitude() - hRef.ne);
        final Cell meanTemperature = new Cell(tmHRef.nw - alphaHRef.nw * dh.nw,
                                              tmHRef.sw - alphaHRef.sw * dh.sw,
                                              tmHRef.se - alphaHRef.se * dh.se,
                                              tmHRef.ne - alphaHRef.ne * dh.ne);

        // lapse rate, equation 27f
        final Cell lgRd     = new Cell((lambdaHRef.nw + 1) * g.nw / R_PRIME_D,
                                       (lambdaHRef.sw + 1) * g.sw / R_PRIME_D,
                                       (lambdaHRef.se + 1) * g.se / R_PRIME_D,
                                       (lambdaHRef.ne + 1) * g.ne / R_PRIME_D);
        final Cell alpha    = new Cell(0.5 * (lgRd.nw - FastMath.sqrt(lgRd.nw * (lgRd.nw - 4 * alphaHRef.nw))),
                                       0.5 * (lgRd.sw - FastMath.sqrt(lgRd.sw * (lgRd.sw - 4 * alphaHRef.sw))),
                                       0.5 * (lgRd.se - FastMath.sqrt(lgRd.se * (lgRd.se - 4 * alphaHRef.se))),
                                       0.5 * (lgRd.ne - FastMath.sqrt(lgRd.ne * (lgRd.ne - 4 * alphaHRef.ne))));

        // temperature, equation 27e
        final Cell t        = new Cell(tmHRef.nw / (1 - alpha.nw / lgRd.nw),
                                       tmHRef.sw / (1 - alpha.sw / lgRd.sw),
                                       tmHRef.se / (1 - alpha.se / lgRd.se),
                                       tmHRef.ne / (1 - alpha.ne / lgRd.ne));

        // pressure at current height, equation 27c
        final Cell pressure = new Cell(pHRef.nw * FastMath.pow(1 - alpha.nw * dh.nw / t.nw, g.nw / (alpha.nw * R_PRIME_D)),
                                       pHRef.sw * FastMath.pow(1 - alpha.sw * dh.sw / t.sw, g.sw / (alpha.sw * R_PRIME_D)),
                                       pHRef.se * FastMath.pow(1 - alpha.se * dh.se / t.se, g.se / (alpha.se * R_PRIME_D)),
                                       pHRef.ne * FastMath.pow(1 - alpha.ne * dh.ne / t.ne, g.ne / (alpha.ne * R_PRIME_D)));

        // perform interpolation
        final double dLatS = location.getLatitude()             - LATITUDE_AXIS.node(southIndex);
        final double dLatN = LATITUDE_AXIS.node(southIndex + 1) - location.getLatitude();
        final double dLonE = LONGITUDE_AXIS.node(westIndex + 1) - location.getLongitude();
        final double dLonW = location.getLongitude()            - LONGITUDE_AXIS.node(westIndex);
        return new PressureTemperatureHumidity(location.getAltitude(),
                                               pressure.interpolate(dLatS, dLatN, dLonE, dLonW),
                                               t.interpolate(dLatS, dLatN, dLonE, dLonW),
                                               eHRef.interpolate(dLatS, dLatN, dLonE, dLonW),
                                               meanTemperature.interpolate(dLatS, dLatN, dLonE, dLonW),
                                               lambdaHRef.interpolate(dLatS, dLatN, dLonE, dLonW));

    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldPressureTemperatureHumidity<T>
    getWeatherParameters(final FieldGeodeticPoint<T> location, final FieldAbsoluteDate<T> date) {
        // TODO
        return null;
    }

    /** Parse interpolating table from a file.
     * @param fileName name of the file to parse
     * @return parsec interpolating function
     */
    public static double[][] parse(final String fileName) {

        // parse the file
        final double[][] values = new double[LATITUDE_AXIS.size()][LONGITUDE_AXIS.size()];
        try (InputStream is     = ITURP834WeatherParameters.class.getResourceAsStream(ITU_R_P_834 + fileName);
             InputStreamReader isr    = new InputStreamReader(is, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(isr)) {
            for (int row = 0; row < LATITUDE_AXIS.size(); ++row) {

                final String   line   = reader.readLine();
                final String[] fields = SPLITTER.split(line.trim());
                if (fields.length != LONGITUDE_AXIS.size()) {
                    throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                              row + 1, fileName, line);
                }

                // distribute points in longitude
                for (int col = 0; col < LONGITUDE_AXIS.size(); ++col) {
                    // files are between 0° and 360° in longitude,  with last column (360°) equal to first column (0°)
                    // our tables, on the other hand use canonical longitudes between -180° and +180°
                    // col =   0 → base longitude =   0.0° → fixed longitude =    0.0° → longitudeIndex = 120
                    // col =   1 → base longitude =   1.5° → fixed longitude =    1.5° → longitudeIndex = 121
                    // …
                    // col = 119 → base longitude = 178.5° → fixed longitude =  178.5° → longitudeIndex = 239
                    // col = 120 → base longitude = 180.0° → fixed longitude =  180.0° → longitudeIndex = 240
                    // col = 121 → base longitude = 181.5° → fixed longitude = -178.5° → longitudeIndex =   1
                    // …
                    // col = 239 → base longitude = 358.5° → fixed longitude =   -1.5° → longitudeIndex = 119
                    // col = 240 → base longitude = 360.0° → fixed longitude =    0.0° → longitudeIndex = 120
                    final double baseLongitude  = col * STEP;
                    final double fixedLongitude = baseLongitude > 180 ? baseLongitude - 360 : baseLongitude;
                    final int    longitudeIndex = (int) FastMath.rint((fixedLongitude - MIN_LON) / STEP);
                    values[longitudeIndex][row] = Double.parseDouble(fields[col]);
                }

                // the loop above stored longitude 180° at index 240, but longitude -180° is missing
                values[0][row] = values[LONGITUDE_AXIS.size() - 1][row];

            }
        } catch (IOException ioe) {
            // this should never happen with the embedded data
            throw new OrekitException(OrekitMessages.INTERNAL_ERROR, ioe);
        }

        // build the interpolating function
        return values;

    }

    /** Build a grid axis for interpolating within a table.
     * @param min min angle in degrees (included)
     * @param max max angle in degrees (included)
     * @param step step between points
     * @return grid axis
     */
    private static GridAxis buildAxis(final double min, final double max, final double step) {
        final double[] grid = new double[(int) FastMath.rint((max - min) / step) + 1];
        for (int i = 0; i < grid.length; i++) {
            grid[i] = FastMath.toRadians(min + i * step);
        }
        return new GridAxis(grid, 2);
    }

    /** Holder for one cell. */
    private static class Cell {

        /** North-West value. */
        private final double nw;

        /** South-West value. */
        private final double sw;

        /** South-East value. */
        private final double se;

        /** North-East value. */
        private final double ne;

        /** Simple constructor.
         * @param average average value table
         * @param seasonal seasonal fluctuation table
         * @param min day of minimum table
         * @param southIndex latitude index of South points
         * @param westIndex longitude index of West points
         * @param doy day of year
         */
        Cell(final double[][] average, final double[][] seasonal, final double[][] min,
             final int southIndex, final int westIndex, final double doy) {
            this.nw = evaluate(average, seasonal, min, southIndex + 1, westIndex,     doy);
            this.sw = evaluate(average, seasonal, min, southIndex,     westIndex,     doy);
            this.se = evaluate(average, seasonal, min, southIndex,     westIndex + 1, doy);
            this.ne = evaluate(average, seasonal, min, southIndex + 1, westIndex + 1, doy);
        }

        /** Simple constructor.
         * @param constant constant value table
         * @param southIndex latitude index of South points
         * @param westIndex longitude index of West points
         */
        Cell(final double[][] constant, final int southIndex, final int westIndex) {
            this.nw = constant[southIndex + 1][westIndex];
            this.sw = constant[southIndex    ][westIndex];
            this.se = constant[southIndex    ][westIndex + 1];
            this.ne = constant[southIndex + 1][westIndex + 1];
        }

        /** Simple constructor.
         * @param nw North-West value
         * @param sw South-West value
         * @param se South-East value
         * @param ne North-East value
         */
        Cell(final double nw, final double sw, final double se, final double ne) {
            this.nw = nw;
            this.sw = sw;
            this.se = se;
            this.ne = ne;
        }

        /** Perform bi-linear interpolation.
         * @param dLatS latitude - latitude South
         * @param dLatN latitude North - latitude
         * @param dLonE longitude East - longitude
         * @param dLonW longitude - longitude West
         */
        public double interpolate(final double dLatS, final double dLatN, final double dLonE, final double dLonW) {
            // TODO: check signs
            return (dLatS * (dLonW * ne + dLonE * nw) + dLatN * (dLonW * se + dLonE * sw)) /
                   ((dLatN - dLatS) * (dLonE - dLonW));
        }

        /** Evaluate one table node for a given day.
         * @param average average value table
         * @param seasonal seasonal fluctuation table
         * @param min day of minimum table
         * @param latitudeIndex latitude index
         * @param longitudeIndex longitude index
         * @param doy day of year
         * @return value of the tabulated function at the node
         */
        private static double evaluate(final double[][] average, final double[][] seasonal, final double[][] min,
                                       final int latitudeIndex, final int longitudeIndex, final double doy) {
            // equation 27a
            return average[latitudeIndex][longitudeIndex] -
                   seasonal[latitudeIndex][longitudeIndex] *
                       FastMath.toRadians(OMEGA * (doy - min[latitudeIndex][longitudeIndex]));
        }

    }

}
