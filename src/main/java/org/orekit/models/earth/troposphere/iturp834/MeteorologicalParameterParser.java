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

import org.hipparchus.analysis.interpolation.BilinearInterpolatingFunction;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/** Parser for one meteorological parameters grid file.
 * @author Luc Maisonobe
 * @see <a href="https://www.itu.int/rec/R-REC-P.834/en">P.834 : Effects of tropospheric refraction on radiowave propagation</>
 * @since 13.0
 */
class MeteorologicalParameterParser {

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

    /** Parse interpolating function from a file.
     * @param fileName name of the file to parse
     * @return parsec interpolating function
     */
    public BilinearInterpolatingFunction parse(final String fileName) {

        // create the interpolation points
        final double[] longitudes = interpolationPoints(MIN_LON, MAX_LON, STEP);
        final double[] latitudes  = interpolationPoints(MIN_LAT, MAX_LAT, STEP);

        // parse the file
        final double[][] values = new double[latitudes.length][longitudes.length];
        try (InputStream       is     = MeteorologicalParameterParser.class.getResourceAsStream(fileName);
             InputStreamReader isr    = new InputStreamReader(is, StandardCharsets.UTF_8);
             BufferedReader    reader = new BufferedReader(isr)) {
            for (int row = 0; row < latitudes.length; ++row) {

                final String   line   = reader.readLine();
                final String[] fields = SPLITTER.split(line.trim());
                if (fields.length != longitudes.length) {
                    throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                              row + 1, fileName, line);
                }

                // distribute points in longitude
                for (int col = 0; col < longitudes.length; ++col) {
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
                values[0][row] = values[longitudes.length - 1][row];

            }
        } catch (IOException ioe) {
            // this should never happen with the embedded data
            throw new OrekitException(OrekitMessages.INTERNAL_ERROR, ioe);
        }

        // build the interpolating function
        return new BilinearInterpolatingFunction(longitudes, latitudes, values);

    }

    /** Create interpolation points coordinates.
     * @param min min angle in degrees (included)
     * @param max max angle in degrees (included)
     * @param step step between points
     * @return interpolation points coordinates (in radians)
     */
    private double[] interpolationPoints(final double min, final double max, final double step) {
        final double[] points = new double[(int) FastMath.rint((max - min) / step) + 1];
        for (int i = 0; i < points.length; i++) {
            points[i] = FastMath.toRadians(min + i * step);
        }
        return points;
    }

}
