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
import org.hipparchus.analysis.interpolation.BilinearInterpolatingFunction;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.models.earth.troposphere.TroposphereMappingFunction;
import org.orekit.models.earth.weather.FieldPressureTemperatureHumidity;
import org.orekit.models.earth.weather.PressureTemperatureHumidity;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldTrackingCoordinates;
import org.orekit.utils.TrackingCoordinates;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/** ITU-R P.834 mapping function.
 * @author Luc Maisonobe
 * @see <a href="https://www.itu.int/rec/R-REC-P.834/en">P.834 : Effects of tropospheric refraction on radiowave propagation</>
 * @since 13.0
 */
public class ITU834MappingFunction implements TroposphereMappingFunction {

    /** Name of data file. */
    private static final String MAPPING_FUNCTION_NAME = "/assets/org/orekit/ITU-R-P.834/p834_mf_coeff_v1.txt";

    /** Minimum longitude, including extra margin for dealing with boundaries (degrees). */
    private static final double MIN_LON = -182.5;

    /** Maximum longitude, including extra margin for dealing with boundaries (degrees). */
    private static final double MAX_LON =  182.5;

    /** Minimum latitude, including extra margin for dealing with boundaries (degrees). */
    private static final double MIN_LAT = -92.5;

    /** Maximum latitude, including extra margin for dealing with boundaries (degrees). */
    private static final double MAX_LAT =  92.5;

    /** Grid step (degrees). */
    private static final double STEP    =   5.0;

    /** Interpolator for constant hydrostatic coefficient. */
    private static final BilinearInterpolatingFunction A0H;

    /** Interpolator for annual cosine hydrostatic coefficient. */
    private static final BilinearInterpolatingFunction A1H;

    /** Interpolator for annual sine hydrostatic coefficient. */
    private static final BilinearInterpolatingFunction B1H;

    /** Interpolator for semi-annual cosine hydrostatic coefficient. */
    private static final BilinearInterpolatingFunction A2H;

    /** Interpolator for semin-annual sine hydrostatic coefficient. */
    private static final BilinearInterpolatingFunction B2H;

    /** Interpolator for constant wet coefficient. */
    private static final BilinearInterpolatingFunction A0W;

    /** Interpolator for annual cosine wet coefficient. */
    private static final BilinearInterpolatingFunction A1W;

    /** Interpolator for annual sine wet coefficient. */
    private static final BilinearInterpolatingFunction B1W;

    /** Interpolator for semi-annual cosine wet coefficient. */
    private static final BilinearInterpolatingFunction A2W;

    /** Interpolator for semin-annual sine wet coefficient. */
    private static final BilinearInterpolatingFunction B2W;

    // load model data
    static {

        // create the various tables
        // we add extra lines and columns to the official files, for dealing with boundaries
        final double[]   longitudes = interpolationPoints(MIN_LON, MAX_LON, STEP);
        final double[]   latitudes  = interpolationPoints(MIN_LAT, MAX_LAT, STEP);
        final double[][] a0h        = new double[longitudes.length][latitudes.length];
        final double[][] a1h        = new double[longitudes.length][latitudes.length];
        final double[][] b1h        = new double[longitudes.length][latitudes.length];
        final double[][] a2h        = new double[longitudes.length][latitudes.length];
        final double[][] b2h        = new double[longitudes.length][latitudes.length];
        final double[][] a0w        = new double[longitudes.length][latitudes.length];
        final double[][] a1w        = new double[longitudes.length][latitudes.length];
        final double[][] b1w        = new double[longitudes.length][latitudes.length];
        final double[][] a2w        = new double[longitudes.length][latitudes.length];
        final double[][] b2w        = new double[longitudes.length][latitudes.length];

        final Pattern splitter = Pattern.compile("\\s+");
        try (InputStream       is     = ITU834MappingFunction.class.getResourceAsStream(MAPPING_FUNCTION_NAME);
             InputStreamReader isr    = new InputStreamReader(is, StandardCharsets.UTF_8);
             BufferedReader    reader = new BufferedReader(isr)) {
            int lineNumber = 0;
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                ++lineNumber;
                final String[] fields = splitter.split(line.trim());
                if (fields.length != 12) {
                    // this should never happen with the embedded data
                    throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                              lineNumber, MAPPING_FUNCTION_NAME, line);
                }

                // parse the fields
                final double[] numericFields = new double[fields.length];
                for (int i = 0; i < fields.length; ++i) {
                    numericFields[i] = Double.parseDouble(fields[i]);
                }

                // find indices in our extended grid
                final int longitudeIndex = (int) FastMath.rint((numericFields[1] - MIN_LON) / STEP);
                final int latitudeIndex  = (int) FastMath.rint((numericFields[0] - MIN_LAT) / STEP);

                // fill-in tables
                a0h[longitudeIndex][latitudeIndex] = numericFields[ 2];
                a1h[longitudeIndex][latitudeIndex] = numericFields[ 3];
                b1h[longitudeIndex][latitudeIndex] = numericFields[ 4];
                a2h[longitudeIndex][latitudeIndex] = numericFields[ 5];
                b2h[longitudeIndex][latitudeIndex] = numericFields[ 6];
                a0w[longitudeIndex][latitudeIndex] = numericFields[ 7];
                a1w[longitudeIndex][latitudeIndex] = numericFields[ 8];
                b1w[longitudeIndex][latitudeIndex] = numericFields[ 9];
                a2w[longitudeIndex][latitudeIndex] = numericFields[10];
                b2w[longitudeIndex][latitudeIndex] = numericFields[11];

            }

            // extend tables in latitude to cover poles
            for (int i = 1; i < longitudes.length - 1; ++i) {
                a0h[i][0]                    = a0h[i][1];
                a0h[i][latitudes.length - 1] = a0h[i][latitudes.length - 2];
                a1h[i][0]                    = a1h[i][1];
                a1h[i][latitudes.length - 1] = a1h[i][latitudes.length - 2];
                b1h[i][0]                    = b1h[i][1];
                b1h[i][latitudes.length - 1] = b1h[i][latitudes.length - 2];
                a2h[i][0]                    = a2h[i][1];
                a2h[i][latitudes.length - 1] = a2h[i][latitudes.length - 2];
                b2h[i][0]                    = b2h[i][1];
                b2h[i][latitudes.length - 1] = b2h[i][latitudes.length - 2];
                a0w[i][0]                    = a0w[i][1];
                a0w[i][latitudes.length - 1] = a0w[i][latitudes.length - 2];
                a1w[i][0]                    = a1w[i][1];
                a1w[i][latitudes.length - 1] = a1w[i][latitudes.length - 2];
                b1w[i][0]                    = b1w[i][1];
                b1w[i][latitudes.length - 1] = b1w[i][latitudes.length - 2];
                a2w[i][0]                    = a2w[i][1];
                a2w[i][latitudes.length - 1] = a2w[i][latitudes.length - 2];
                b2w[i][0]                    = b2w[i][1];
                b2w[i][latitudes.length - 1] = b2w[i][latitudes.length - 2];
            }

            // extend tables in longitude to cover anti-meridian
            for (int j = 0; j < latitudes.length; ++j) {
                a0h[0][j]                     = a0h[longitudes.length - 2][j];
                a0h[longitudes.length - 1][j] = a0h[1][j];
                a1h[0][j]                     = a1h[longitudes.length - 2][j];
                a1h[longitudes.length - 1][j] = a1h[1][j];
                b1h[0][j]                     = b1h[longitudes.length - 2][j];
                b1h[longitudes.length - 1][j] = b1h[1][j];
                a2h[0][j]                     = a2h[longitudes.length - 2][j];
                a2h[longitudes.length - 1][j] = a2h[1][j];
                b2h[0][j]                     = b2h[longitudes.length - 2][j];
                b2h[longitudes.length - 1][j] = b2h[1][j];
                a0w[0][j]                     = a0w[longitudes.length - 2][j];
                a0w[longitudes.length - 1][j] = a0w[1][j];
                a1w[0][j]                     = a1w[longitudes.length - 2][j];
                a1w[longitudes.length - 1][j] = a1w[1][j];
                b1w[0][j]                     = b1w[longitudes.length - 2][j];
                b1w[longitudes.length - 1][j] = b1w[1][j];
                a2w[0][j]                     = a2w[longitudes.length - 2][j];
                a2w[longitudes.length - 1][j] = a2w[1][j];
                b2w[0][j]                     = b2w[longitudes.length - 2][j];
                b2w[longitudes.length - 1][j] = b2w[1][j];
            }

            // build interpolators
            A0H = new BilinearInterpolatingFunction(longitudes, latitudes, a0h);
            A1H = new BilinearInterpolatingFunction(longitudes, latitudes, a1h);
            B1H = new BilinearInterpolatingFunction(longitudes, latitudes, b1h);
            A2H = new BilinearInterpolatingFunction(longitudes, latitudes, a2h);
            B2H = new BilinearInterpolatingFunction(longitudes, latitudes, b2h);
            A0W = new BilinearInterpolatingFunction(longitudes, latitudes, a0w);
            A1W = new BilinearInterpolatingFunction(longitudes, latitudes, a1w);
            B1W = new BilinearInterpolatingFunction(longitudes, latitudes, b1w);
            A2W = new BilinearInterpolatingFunction(longitudes, latitudes, a2w);
            B2W = new BilinearInterpolatingFunction(longitudes, latitudes, b2w);

        } catch (IOException ioe) {
            // this should never happen with the embedded data
            throw new OrekitException(OrekitMessages.INTERNAL_ERROR, ioe);
        }
    }

    /** Create interpolation points coordinates.
     * @param min min angle in degrees (included)
     * @param max max angle in degrees (included)
     * @param step step between points
     * @return interpolation points coordinates (in radians)
     */
    private static double[] interpolationPoints(final double min, final double max, final double step) {
        final double[] points = new double[(int) FastMath.rint((max - min) / step) + 1];
        for (int i = 0; i < points.length; i++) {
            points[i] = FastMath.toRadians(min + i * step);
        }
        return points;
    }

    @Override
    public double[] mappingFactors(final TrackingCoordinates trackingCoordinates, final GeodeticPoint point,
                                   final PressureTemperatureHumidity weather, final AbsoluteDate date) {
        // TODO
        return new double[0];
    }

    @Override
    public <T extends CalculusFieldElement<T>> T[] mappingFactors(final FieldTrackingCoordinates<T> trackingCoordinates,
                                                                  final FieldGeodeticPoint<T> point,
                                                                  final FieldPressureTemperatureHumidity<T> weather,
                                                                  final FieldAbsoluteDate<T> date) {
        // TODO
        return null;
    }

}
