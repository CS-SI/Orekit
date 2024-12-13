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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.regex.Pattern;

/** Modified Dip Latitude grid.
 * <p>
 * The ModipGrid grid allows to estimate modip μ [deg] at a given point (φ,λ)
 * by interpolation of the relevant values contained in the support file.
 * </p>
 * <p>
 * The data is parsed from files sampling Earth in latitude and longitude. In
 * the case of Galileo-specific modip file, sampling step is 5° in latitude and
 * 10° in longitude, and extra rows/columns have been added to wrap around
 * anti-meridian and longitude and also "past" pole (adding extra points with
 * 180° longitude phasing offset) in latitude. In the case of ITU original
 * NeQuick 2 model, sampling step is 1° in latitude and 2° in longitude, without
 * any extra rows/columns. We therefore add the extra rows/columns upon parsing
 * the ITU file.
 * </p>
 * @author Bryan Cazabonne
 * @author Luc Maisonobe
 * @since 13.0
 */
class ModipGrid {

    /** Pattern for delimiting regular expressions. */
    private static final Pattern SEPARATOR = Pattern.compile("\\s+");

    /** Number of cells of ModipGrid grid in longitude (without wrapping). */
    private final int nbCellsLon;

    /** Cell size in longitude. */
    private final double sizeLon;

    /** Number of cells of ModipGrid grid in latitude (without wrapping). */
    private final int nbCellsLat;

    /** Cell size in latitude. */
    private final double sizeLat;

    /** Modip grid. */
    private final double[][] grid;

    /** Build a new modip grid.
     * @param nbCellsLon number of cells of ModipGrid grid in longitude (without wrapping)
     * @param nbCellsLat number of cells of ModipGrid grid in latitude (without wrapping)
     * @param source source of the grid file
     * @param wrappingAlreadyIncluded indicator for already included ModipGrid grid wrapping in resource file
     */
    ModipGrid(final int nbCellsLon, final int nbCellsLat,
              final DataSource source, final boolean wrappingAlreadyIncluded) {
        this.nbCellsLon = nbCellsLon;
        this.sizeLon    = MathUtils.TWO_PI / nbCellsLon;
        this.nbCellsLat = nbCellsLat;
        this.sizeLat    = FastMath.PI / nbCellsLat;
        this.grid       = new double[nbCellsLat + 3][nbCellsLon + 3];
        loadData(source, !wrappingAlreadyIncluded);
    }

    /** Compute modip for a location.
     * @param latitude latitude
     * @param longitude longitude
     * @return modip at specified location, in degrees
     */
    public double computeMODIP(final double latitude, final double longitude) {

        // index in latitude (note that Galileo document uses x for interpolation in latitude)
        // ensuring we have two points before (or at) index and two points after index for 3rd order interpolation
        double x  = (latitude + MathUtils.SEMI_PI) / sizeLat + 1;
        if (x < 1) {
            return -90;
        }
        if (x >= nbCellsLat + 1) {
            return 90;
        }
        final int    i      = (int) FastMath.floor(x);
        final double deltaX = x - i;

        // index in longitude (note that Galileo document uses y for interpolation in longitude)
        // ensuring we have two points before (or at) index and two points after index for 3rd order interpolation
        double y  = (longitude + FastMath.PI) / sizeLon + 1;
        while (y < 1) {
            y += nbCellsLon;
        }
        while (y >= nbCellsLon + 1) {
            y -= nbCellsLon;
        }
        final int    j      = (int) FastMath.floor(y);
        final double deltaY = y - j;

        // zi coefficients (Eq. 12 and 13)
        final double z1 = interpolate(grid[i - 1][j - 1], grid[i][j - 1], grid[i + 1][j - 1], grid[i + 2][j - 1], deltaX);
        final double z2 = interpolate(grid[i - 1][j],     grid[i][j],     grid[i + 1][j],     grid[i + 2][j],     deltaX);
        final double z3 = interpolate(grid[i - 1][j + 1], grid[i][j + 1], grid[i + 1][j + 1], grid[i + 2][j + 1], deltaX);
        final double z4 = interpolate(grid[i - 1][j + 2], grid[i][j + 2], grid[i + 1][j + 2], grid[i + 2][j + 2], deltaX);

        // Modip (Ref Eq. 16)
        return interpolate(z1, z2, z3, z4, deltaY);

    }

    /**
     * This method provides a third order interpolation function
     * as recommended in the reference document (Ref Eq. 128 to Eq. 138)
     *
     * @param z1 z1 coefficient
     * @param z2 z2 coefficient
     * @param z3 z3 coefficient
     * @param z4 z4 coefficient
     * @param x position
     * @return a third order interpolation
     */
    private double interpolate(final double z1, final double z2, final double z3, final double z4,
                               final double x) {

        if (x < 5e-11) {
            return z2;
        }

        final double delta = 2.0 * x - 1.0;
        final double g1 = z3 + z2;
        final double g2 = z3 - z2;
        final double g3 = z4 + z1;
        final double g4 = (z4 - z1) / 3.0;
        final double a0 = 9.0 * g1 - g3;
        final double a1 = 9.0 * g2 - g4;
        final double a2 = g3 - g1;
        final double a3 = g4 - g2;
        return 0.0625 * (a0 + delta * (a1 + delta * (a2 + delta * a3)));

    }

    /** Compute modip for a location.
     * @param latitude latitude
     * @param longitude longitude
     * @return modip at specified location
     */
    public <T extends CalculusFieldElement<T>> T computeMODIP(final T latitude, final T longitude) {

        // index in latitude (note that Galileo document uses x for interpolation in latitude)
        // ensuring we have two points before (or at) index and two points after index for 3rd order interpolation
        T x  = latitude.add(MathUtils.SEMI_PI).divide(sizeLat).add(1);
        if (x.getReal() < 1) {
            return latitude.newInstance(-90);
        }
        if (x.getReal() >= nbCellsLat + 1) {
            return latitude.newInstance(90);
        }
        final int i      = (int) FastMath.floor(x.getReal());
        final T   deltaX = x.subtract(i);

        // index in longitude (note that Galileo document uses y for interpolation in longitude)
        // ensuring we have two points before (or at) index and two points after index for 3rd order interpolation
        T y  = longitude.add(FastMath.PI).divide(sizeLon).add(1);
        while (y.getReal() < 1) {
            y = y.add(nbCellsLon);
        }
        while (y.getReal() >= nbCellsLon + 1) {
            y = y.subtract(nbCellsLon);
        }
        final int j      = (int) FastMath.floor(y.getReal());
        final T   deltaY = y.subtract(j);

        // zi coefficients (Eq. 12 and 13)
        final T z1 = interpolate(x.newInstance(grid[i - 1][j - 1]), x.newInstance(grid[i][j - 1]),
                                 x.newInstance(grid[i + 1][j - 1]), x.newInstance(grid[i + 2][j - 1]),
                                 deltaX);
        final T z2 = interpolate(x.newInstance(grid[i - 1][j]),     x.newInstance(grid[i][j]),
                                 x.newInstance(grid[i + 1][j]),     x.newInstance(grid[i + 2][j]),
                                 deltaX);
        final T z3 = interpolate(x.newInstance(grid[i - 1][j + 1]), x.newInstance(grid[i][j + 1]),
                                 x.newInstance(grid[i + 1][j + 1]), x.newInstance(grid[i + 2][j + 1]),
                                 deltaX);
        final T z4 = interpolate(x.newInstance(grid[i - 1][j + 2]), x.newInstance(grid[i][j + 2]),
                                 x.newInstance(grid[i + 1][j + 2]), x.newInstance(grid[i + 2][j + 2]),
                                 deltaX);

        // Modip (Ref Eq. 16)
        return interpolate(z1, z2, z3, z4, deltaY);

    }

    /**
     * This method provides a third order interpolation function
     * as recommended in the reference document (Ref Eq. 128 to Eq. 138)
     *
     * @param <T> type of the field elements
     * @param z1 z1 coefficient
     * @param z2 z2 coefficient
     * @param z3 z3 coefficient
     * @param z4 z4 coefficient
     * @param x position
     * @return a third order interpolation
     */
    private <T extends CalculusFieldElement<T>> T interpolate(final T z1, final T z2, final T z3, final T z4,
                                                              final T x) {

        if (x.getReal() < 5e-11) {
            return z2;
        }

        final T delta = x.multiply(2.0).subtract(1.0);
        final T g1    = z3.add(z2);
        final T g2    = z3.subtract(z2);
        final T g3    = z4.add(z1);
        final T g4    = z4.subtract(z1).divide(3.0);
        final T a0    = g1.multiply(9.0).subtract(g3);
        final T a1    = g2.multiply(9.0).subtract(g4);
        final T a2    = g3.subtract(g1);
        final T a3    = g4.subtract(g2);
        return delta.multiply(a3).add(a2).multiply(delta).add(a1).multiply(delta).add(a0).multiply(0.0625);

    }

    /**
     * Load data from a stream.
     *
     * @param source grid source
     * @param addWrapping if true, wrapping should be added to loaded data
     */
    private void loadData(final DataSource source, final boolean addWrapping) {
        // if we must add wrapping, we must keep some empty rows and columns that will be filled later on
        int first = addWrapping ? 1 : 0;

        // Open stream and parse data
        int lineNumber = 0;
        String line = null;
        int row = first;
        try (Reader         r  = source.getOpener().openReaderOnce();
             BufferedReader br = new BufferedReader(r)) {

            for (line = br.readLine(); line != null; line = br.readLine()) {
                ++lineNumber;

                // Read grid data
                final String[] fields = SEPARATOR.split(line.trim());
                if (fields.length == (addWrapping ? grid[row].length - 2 : grid[row].length)) {
                    // this should be a regular data line (i.e. not a header line)
                    for (int i = 0; i < fields.length; i++) {
                        grid[row][first + i] = Double.parseDouble(fields[i]);
                    }
                    ++row;
                }

            }

        } catch (IOException | NumberFormatException e) {
            throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, lineNumber, source.getName(), line);
        }

        if (addWrapping) {

            // we must add rows phased 180° in longitude after poles, for the sake of interpolation
            final double[] extraNorth = grid[0];
            final double[] refNorth   = grid[2];
            final double[] extraSouth = grid[grid.length - 1];
            final double[] refSouth   = grid[grid.length - 3];
            final int      deltaPhase = nbCellsLon / 2;
            for (int j = 1; j <= deltaPhase; j++) {
                extraNorth[j]              = refNorth[j + deltaPhase];
                extraNorth[j + deltaPhase] = refNorth[j];
                extraSouth[j]              = refSouth[j + deltaPhase];
                extraSouth[j + deltaPhase] = refSouth[j];
            }
            extraNorth[nbCellsLon + 1] = extraNorth[1];
            extraSouth[nbCellsLon + 1] = extraSouth[1];

            // we must copy columns to wrap around Earth in longitude
            // the first three columns must be identical to the last three columns
            // the columns 1 and gi.length - 2 (i.e. anti-meridian) are already
            // identical in the original resource file
            for (final double[] gi : grid) {
                gi[0] = gi[gi.length - 3];
                gi[gi.length - 1] = gi[2];
            }

            row++;

        }

        // Throw an exception if ModipGrid grid was not loaded properly
        if (row != grid.length) {
            throw new OrekitException(OrekitMessages.MODIP_GRID_NOT_LOADED, source.getName());
        }

    }

}
