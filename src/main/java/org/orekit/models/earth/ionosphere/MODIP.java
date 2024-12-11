/* Copyright 2002-2024 CS GROUP
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

import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/** Modified Dip Latitude (MODIP) grid.
 * <p>
 * The MODIP grid allows to estimate MODIP μ [deg] at a given point (φ,λ) by interpolation of the relevant values
 * contained in the support file.
 * </p> <p>
 * The file contains the values of MODIP (expressed in degrees) on a geocentric grid from 90°S to 90°N with a 5-degree
 * step in latitude and from 180°W to 180°E with a 10-degree in longitude.
 * </p>
 * @author Bryan Cazabonne
 * @author Luc Maisonobe
 * @since 13.0
 */
class MODIP {

    /** Pattern for delimiting regular expressions. */
    private static final Pattern SEPARATOR = Pattern.compile("\\s+");

    /** Longitude step. */
    private final double lonStep;

    /** Latitude step. */
    private final double latStep;

    /** MODIP grid. */
    private final double[][] grid;

    /** Build a new MODIP grid.
     * @param version version of NeQuick model
     */
    MODIP(final NeQuickVersion version) {
        lonStep = MathUtils.TWO_PI / version.getcellsLon();
        latStep = FastMath.PI      / version.getcellsLat();
        grid    = new double[version.getcellsLat() + 2][version.getcellsLon() + 3];
        final String fileName = String.format("%s/%s", NeQuickModel.NEQUICK_BASE, version.getResourceName());
        try (InputStream in = MODIP.class.getResourceAsStream(fileName)) {
            loadData(in, fileName, !version.isWrappingAlreadyIncluded());
        } catch (IOException e) {
            throw new OrekitException(OrekitMessages.INTERNAL_ERROR, e);
        }

    }

    /**
     * Returns the MODIP grid array.
     *
     * @return the MODIP grid array
     */
    public double[][] getMODIPGrid() {
        return grid.clone();
    }

    /**
     * Load data from a stream.
     *
     * @param input input stream
     * @param name  name of the file
     * @param addWrapping if true, wrapping should be added to loaded data
     * @exception IOException if data can't be read
     */
    private void loadData(final InputStream input, final String name, final boolean addWrapping) throws IOException {

        // if we must add wrapping, we must keep some empty rows and columns that will be filled later on
        int first = addWrapping ? 1 : 0;

        // Open stream and parse data
        int lineNumber = 0;
        String line = null;
        int row = first;
        try (InputStreamReader isr = new InputStreamReader(input, StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(isr)) {

            for (line = br.readLine(); line != null; line = br.readLine()) {
                ++lineNumber;
                line = line.trim();

                // Read grid data
                final String[] fields = SEPARATOR.split(line);
                if (fields.length == (addWrapping ? grid[row].length : grid[row].length - 2)) {
                    // this should be a regular data line (i.e. not a header line)
                    for (int i = 0; i < fields.length; i++) {
                        grid[row][first + i] = FastMath.toRadians(Double.parseDouble(fields[i]));
                    }
                    ++row;
                }

            }

        } catch (NumberFormatException nfe) {
            throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, lineNumber, name, line);
        }

        if (addWrapping) {

            // we must copy columns to wrap around Earth in longitude
            // the first three columns must be identical to the last three columns
            // the columns 1 and gi.length - 2 are already identical in the original resource file
            for (int i = 1; i < grid.length - 1; ++i) {
                final double[] gi = grid[i];
                gi[0]             = gi[gi.length - 3];
                gi[gi.length - 1] = gi[2];
            }

            // we must add rows phased 180° in longitude after poles, for the sake of interpolation
            // TODO

        }

        // Throw an exception if MODIP grid was not loaded properly
        if (row != grid.length) {
            throw new OrekitException(OrekitMessages.MODIP_GRID_NOT_LOADED, name);
        }

    }

}
