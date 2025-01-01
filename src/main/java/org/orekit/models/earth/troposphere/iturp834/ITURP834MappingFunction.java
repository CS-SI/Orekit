/* Copyright 2022-2025 Thales Alenia Space
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
import org.hipparchus.util.FieldSinCos;
import org.hipparchus.util.MathArrays;
import org.hipparchus.util.MathUtils;
import org.hipparchus.util.SinCos;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.models.earth.troposphere.TroposphereMappingFunction;
import org.orekit.models.earth.weather.FieldPressureTemperatureHumidity;
import org.orekit.models.earth.weather.PressureTemperatureHumidity;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.utils.FieldTrackingCoordinates;
import org.orekit.utils.TrackingCoordinates;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/** ITU-R P.834 mapping function.
 * @see ITURP834PathDelay
 * @see ITURP834WeatherParametersProvider
 * @author Luc Maisonobe
 * @see <a href="https://www.itu.int/rec/R-REC-P.834/en">P.834 : Effects of tropospheric refraction on radiowave propagation</a>
 * @since 13.0
 */
public class ITURP834MappingFunction implements TroposphereMappingFunction {

    /** Splitter for fields in lines. */
    private static final Pattern SPLITTER = Pattern.compile("\\s+");

    /** Name of data file. */
    private static final String MAPPING_FUNCTION_NAME = "/assets/org/orekit/ITU-R-P.834/p834_mf_coeff_v1.txt";

    /** Minimum latitude, including extra margin for dealing with boundaries (degrees). */
    private static final double MIN_LAT = -92.5;

    /** Maximum latitude, including extra margin for dealing with boundaries (degrees). */
    private static final double MAX_LAT =  92.5;

    /** Grid step in latitude (degrees). */
    private static final double STEP_LAT =  5.0;

    /** Minimum longitude, including extra margin for dealing with boundaries (degrees). */
    private static final double MIN_LON = -182.5;

    /** Maximum longitude, including extra margin for dealing with boundaries (degrees). */
    private static final double MAX_LON =  182.5;

    /** Grid step in longitude (degrees). */
    private static final double STEP_LON =  5.0;

    /** Second coefficient for hydrostatic component. */
    private static final double BH = 0.0029;

    /** Constants for third coefficient for hydrostatic component, Northern hemisphere. */
    private static final double[] CH_NORTH = { 0.062, 0.001, 0.005, 0.0 };

    /** Constants for third coefficient for hydrostatic component, Southern hemisphere. */
    private static final double[] CH_SOUTH = { 0.062, 0.002, 0.007, FastMath.PI };

    /** Reference day of year for third coefficient for hydrostatic component. */
    private static final double REF_DOY = 28;

    /** Year length (in days). */
    private static final double YEAR = 365.25;

    /** Second coefficient for wet component. */
    private static final double BW = 0.00146;

    /** Third coefficient for wet component. */
    private static final double CW = 0.04391;

    /** Global factor to apply to first coefficient. */
    private static final double FACTOR = 1.0e-3;

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
        final double[]   longitudes = interpolationPoints(MIN_LON, MAX_LON, STEP_LON);
        final double[]   latitudes  = interpolationPoints(MIN_LAT, MAX_LAT, STEP_LAT);
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

        try (InputStream       is     = ITURP834MappingFunction.class.getResourceAsStream(MAPPING_FUNCTION_NAME);
             InputStreamReader isr    = is  == null ? null : new InputStreamReader(is, StandardCharsets.UTF_8);
             BufferedReader    reader = isr == null ? null : new BufferedReader(isr)) {
            if (reader == null) {
                // this should never happen with embedded data
                throw new OrekitException(OrekitMessages.UNABLE_TO_FIND_FILE, MAPPING_FUNCTION_NAME);
            }
            int lineNumber = 0;
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                ++lineNumber;
                final String[] fields = SPLITTER.split(line.trim());
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
                final int longitudeIndex = (int) FastMath.rint((numericFields[1] - MIN_LON) / STEP_LON);
                final int latitudeIndex  = (int) FastMath.rint((numericFields[0] - MIN_LAT) / STEP_LAT);

                // fill-in tables
                a0h[longitudeIndex][latitudeIndex] = FACTOR * numericFields[ 2];
                a1h[longitudeIndex][latitudeIndex] = FACTOR * numericFields[ 3];
                b1h[longitudeIndex][latitudeIndex] = FACTOR * numericFields[ 4];
                a2h[longitudeIndex][latitudeIndex] = FACTOR * numericFields[ 5];
                b2h[longitudeIndex][latitudeIndex] = FACTOR * numericFields[ 6];
                a0w[longitudeIndex][latitudeIndex] = FACTOR * numericFields[ 7];
                a1w[longitudeIndex][latitudeIndex] = FACTOR * numericFields[ 8];
                b1w[longitudeIndex][latitudeIndex] = FACTOR * numericFields[ 9];
                a2w[longitudeIndex][latitudeIndex] = FACTOR * numericFields[10];
                b2w[longitudeIndex][latitudeIndex] = FACTOR * numericFields[11];

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

    /** UTC time scale. */
    private final TimeScale utc;

    /** Simple constructor.
     * @param utc UTC time scale
     */
    public ITURP834MappingFunction(final TimeScale utc) {
        this.utc = utc;
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

        final double doy = date.getDayOfYear(utc);

        // compute third dry coefficient
        // equation 28c in ITU-R P.834 recommendation
        final double[] c    = point.getLatitude() >= 0.0 ? CH_NORTH : CH_SOUTH;
        final double   cosL = FastMath.cos(point.getLatitude());
        final double   cosD = FastMath.cos((doy - REF_DOY) * MathUtils.TWO_PI / YEAR + c[3]);
        final double   ch   = c[0] + ((cosD + 1) * c[2] * 0.5 + c[1]) * (1 - cosL);

        // compute first coefficient
        final SinCos sc1 = FastMath.sinCos(MathUtils.TWO_PI * doy / YEAR);
        final SinCos sc2 = SinCos.sum(sc1, sc1);

        // equation 28d in ITU-R P.834 recommendation
        final double ah  = A0H.value(point.getLongitude(), point.getLatitude()) +
                           A1H.value(point.getLongitude(), point.getLatitude()) * sc1.cos() +
                           B1H.value(point.getLongitude(), point.getLatitude()) * sc1.sin() +
                           A2H.value(point.getLongitude(), point.getLatitude()) * sc2.cos() +
                           B2H.value(point.getLongitude(), point.getLatitude()) * sc2.sin();

        // equation 28e in ITU-R P.834 recommendation
        final double aw  = A0W.value(point.getLongitude(), point.getLatitude()) +
                           A1W.value(point.getLongitude(), point.getLatitude()) * sc1.cos() +
                           B1W.value(point.getLongitude(), point.getLatitude()) * sc1.sin() +
                           A2W.value(point.getLongitude(), point.getLatitude()) * sc2.cos() +
                           B2W.value(point.getLongitude(), point.getLatitude()) * sc2.sin();

        // mapping function, equations 28a and 28b in ITU-R P.834 recommendation
        final double sinTheta = FastMath.sin(trackingCoordinates.getElevation());
        final double mh = (1 + ah / (1 + BH / (1 + ch))) /
                          (sinTheta + ah / (sinTheta + BH / (sinTheta + ch)));
        final double mw = (1 + aw / (1 + BW / (1 + CW))) /
                          (sinTheta + aw / (sinTheta + BW / (sinTheta + CW)));

        return new double[] {
            mh, mw
        };

    }

    @Override
    public <T extends CalculusFieldElement<T>> T[] mappingFactors(final FieldTrackingCoordinates<T> trackingCoordinates,
                                                                  final FieldGeodeticPoint<T> point,
                                                                  final FieldPressureTemperatureHumidity<T> weather,
                                                                  final FieldAbsoluteDate<T> date) {

        final T doy = date.getDayOfYear(utc);

        // compute third dry coefficient
        // equation 28c in ITU-R P.834 recommendation
        final double[] c    = point.getLatitude().getReal() >= 0.0 ? CH_NORTH : CH_SOUTH;
        final T        cosL = FastMath.cos(point.getLatitude());
        final T        cosD = FastMath.cos(doy.subtract(REF_DOY).multiply(MathUtils.TWO_PI / YEAR).add(c[3]));
        final T        ch   = cosD.add(1).multiply(c[2] * 0.5).add(c[1]).multiply(cosL.subtract(1).negate()).add(c[0]);

        // compute first coefficient
        final FieldSinCos<T> sc1 = FastMath.sinCos(doy.multiply(MathUtils.TWO_PI / YEAR));
        final FieldSinCos<T> sc2 = FieldSinCos.sum(sc1, sc1);

        // equation 28d in ITU-R P.834 recommendation
        final T ah  =     A0H.value(point.getLongitude(), point.getLatitude()).
                      add(A1H.value(point.getLongitude(), point.getLatitude()).multiply(sc1.cos())).
                      add(B1H.value(point.getLongitude(), point.getLatitude()).multiply(sc1.sin())).
                      add(A2H.value(point.getLongitude(), point.getLatitude()).multiply(sc2.cos())).
                      add(B2H.value(point.getLongitude(), point.getLatitude()).multiply(sc2.sin()));

        // equation 28e in ITU-R P.834 recommendation
        final T aw  =     A0W.value(point.getLongitude(), point.getLatitude()).
                      add(A1W.value(point.getLongitude(), point.getLatitude()).multiply(sc1.cos())).
                      add(B1W.value(point.getLongitude(), point.getLatitude()).multiply(sc1.sin())).
                      add(A2W.value(point.getLongitude(), point.getLatitude()).multiply(sc2.cos())).
                      add(B2W.value(point.getLongitude(), point.getLatitude()).multiply(sc2.sin()));

        // mapping function, equations 28a and 28b in ITU-R P.834 recommendation
        final T sinTheta = FastMath.sin(trackingCoordinates.getElevation());
        final T mh = ch.add(1).reciprocal().multiply(BH).add(1).reciprocal().multiply(ah).add(1).
                     divide(ch.add(sinTheta).reciprocal().multiply(BH).add(sinTheta).reciprocal().multiply(ah).add(sinTheta));
        final T mw = aw.divide(1 + BW / (1 + CW)).add(1).
                     divide(sinTheta.add(CW).reciprocal().multiply(BW).add(sinTheta).reciprocal().multiply(aw).add(sinTheta));

        final T[] m = MathArrays.buildArray(date.getField(), 2);
        m[0] = mh;
        m[1] = mw;
        return m;

    }

}
