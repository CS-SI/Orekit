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
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.utils.units.Unit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/** Container for grid data.
 * @author Luc Maisonobe
 * @since 13.0
 */
abstract class AbstractGrid {

    /** Pattern for splitting fields. */
    private static final Pattern SPLITTER = Pattern.compile("\\s+");

    /** Minimum latitude (degrees). */
    private static final double MIN_LAT = -90.0;

    /** Maximum latitude (degrees). */
    private static final double MAX_LAT =  90.0;

    /** Grid step in latitude (degrees). */
    private static final double STEP_LAT =   1.5;

    /** Grid step in latitude (radians). */
    private static final double STEP_LAT_RAD = FastMath.toRadians(STEP_LAT);

    /** Minimum longitude (degrees). */
    private static final double MIN_LON = -180.0;

    /** Maximum longitude (degrees). */
    private static final double MAX_LON =  180.0;

    /** Grid step in longitude (degrees). */
    private static final double STEP_LON =   1.5;

    /** Grid step in longitude (radians). */
    private static final double STEP_LON_RAD = FastMath.toRadians(STEP_LON);

    /** Latitude grid axis. */
    private final GridAxis latitudeAxis;

    /** Longitude grid axis. */
    private final GridAxis longitudeAxis;

    /** Simple constructor.
     */
    protected AbstractGrid() {
        latitudeAxis  = buildAxis(MIN_LAT, MAX_LAT, STEP_LAT);
        longitudeAxis = buildAxis(MIN_LON, MAX_LON, STEP_LON);
    }

    /** Get latitude axis.
     * @return latitude axis
     */
    public GridAxis getLatitudeAxis() {
        return latitudeAxis;
    }

    /** Get longitude axis.
     * @return longitude axis
     */
    public GridAxis getLongitudeAxis() {
        return longitudeAxis;
    }

    /** Get cell size in latitude.
     * @return cell size in latitude
     */
    public double getSizeLat() {
        return STEP_LAT_RAD;
    }

    /** Get cell size in longitude.
     * @return cell size in longitude
     */
    public double getSizeLon() {
        return STEP_LON_RAD;
    }

    /** Get one raw cell.
     * @param location point location on Earth
     * @param rawData raww grid data
     * @return raw cell
     */
    protected GridCell getRawCell(final GeodeticPoint location, final double[][] rawData) {

        // locate the point
        final int    southIndex    = latitudeAxis.interpolationIndex(location.getLatitude());
        final double southLatitude = latitudeAxis.node(southIndex);
        final int    westIndex     = longitudeAxis.interpolationIndex(location.getLongitude());
        final double westLongitude = longitudeAxis.node(westIndex);

        // build the cell
        return new GridCell(location.getLatitude() - southLatitude,
                            location.getLongitude() - westLongitude,
                            STEP_LAT_RAD, STEP_LON_RAD,
                            rawData[southIndex + 1][westIndex],
                            rawData[southIndex][westIndex],
                            rawData[southIndex][westIndex + 1],
                            rawData[southIndex + 1][westIndex + 1]);

    }

    /** Get one raw cell.
     * @param <T> type of the field elements
     * @param location point location on Earth
     * @param rawData raww grid data
     * @return raw cell
     */
    protected <T extends CalculusFieldElement<T>> FieldGridCell<T> getRawCell(final FieldGeodeticPoint<T> location,
                                                                              final double[][] rawData) {

        // locate the point
        final int    southIndex    = latitudeAxis.interpolationIndex(location.getLatitude().getReal());
        final double southLatitude = latitudeAxis.node(southIndex);
        final int    westIndex     = longitudeAxis.interpolationIndex(location.getLongitude().getReal());
        final double westLongitude = longitudeAxis.node(westIndex);

        // build the cell
        final T zero = location.getAltitude().getField().getZero();
        return new FieldGridCell<>(location.getLatitude().subtract(southLatitude),
                                   location.getLongitude().subtract(westLongitude),
                                   STEP_LAT_RAD, STEP_LON_RAD,
                                   zero.newInstance(rawData[southIndex + 1][westIndex]),
                                   zero.newInstance(rawData[southIndex][westIndex]),
                                   zero.newInstance(rawData[southIndex][westIndex + 1]),
                                   zero.newInstance(rawData[southIndex + 1][westIndex + 1]));

    }

    /** Get one cell.
     * @param location point location on Earth
     * @param dayOfYear day of year
     */
    public abstract GridCell getCell(GeodeticPoint location, double dayOfYear);

    /** Get one cell.
     * @param <T> type of the field elements
     * @param location point location on Earth
     * @param dayOfYear day of year
     */
    public abstract <T extends CalculusFieldElement<T>> FieldGridCell<T> getCell(FieldGeodeticPoint<T> location,
                                                                                 T dayOfYear);

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

    /** Parse interpolation table from a resource file.
     * @param unit unit of values in resource file
     * @param name name of the resource to parse
     * @return parsed interpolation function
     */
    protected double[][] parse(final Unit unit, final String name) {

        // parse the file
        final double[][] values = new double[latitudeAxis.size()][longitudeAxis.size()];
        try (InputStream       is     = ITURP834WeatherParameters.class.getResourceAsStream(name);
             InputStreamReader isr    = new InputStreamReader(is, StandardCharsets.UTF_8);
             BufferedReader    reader = new BufferedReader(isr)) {
            for (int row = 0; row < latitudeAxis.size(); ++row) {

                final String   line   = reader.readLine();
                final String[] fields = SPLITTER.split(line.trim());
                if (fields.length != longitudeAxis.size()) {
                    throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                            row + 1, name, line);
                }

                // distribute points in longitude
                for (int col = 0; col < longitudeAxis.size(); ++col) {
                    // files are between 0° and 360° in longitude, with last column (360°) equal to first column (0°)
                    // our tables, on the other hand, use canonical longitudes between -180° and +180°
                    // we have to redistribute indices
                    // col =   0 → base longitude =   0.0° → fixed longitude =    0.0° → longitudeIndex = 120
                    // col =   1 → base longitude =   1.5° → fixed longitude =    1.5° → longitudeIndex = 121
                    // …
                    // col = 119 → base longitude = 178.5° → fixed longitude =  178.5° → longitudeIndex = 239
                    // col = 120 → base longitude = 180.0° → fixed longitude =  180.0° → longitudeIndex = 240
                    // col = 121 → base longitude = 181.5° → fixed longitude = -178.5° → longitudeIndex =   1
                    // …
                    // col = 239 → base longitude = 358.5° → fixed longitude =   -1.5° → longitudeIndex = 119
                    // col = 240 → base longitude = 360.0° → fixed longitude =    0.0° → longitudeIndex = 120
                    final int longitudeIndex = col < 121 ? col + 120 : col - 120;
                    values[longitudeIndex][row] = unit.toSI(Double.parseDouble(fields[col]));
                }

                // the loop above stored longitude 180° at index 240, but longitude -180° is missing
                values[0][row] = values[longitudeAxis.size() - 1][row];

            }
        } catch (IOException ioe) {
            // this should never happen with the embedded data
            throw new OrekitException(OrekitMessages.INTERNAL_ERROR, ioe);
        }

        // build the interpolating function
        return values;

    }

}
