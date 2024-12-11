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

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Parser for Modified Dip Latitude (MODIP) grid file.
 * <p>
 * The MODIP grid allows to estimate MODIP μ [deg] at a given point (φ,λ) by interpolation of the relevant values
 * contained in the support file.
 * </p> <p>
 * The file contains the values of MODIP (expressed in degrees) on a geocentric grid from 90°S to 90°N with a 5-degree
 * step in latitude and from 180°W to 180°E with a 10-degree in longitude.
 * </p>
 * @author Bryan Cazabonne
 * @since 13.0
 */
class MODIPLoader {

    /** Supported name for MODIP grid. */
    private static final String SUPPORTED_NAME = NeQuickModel.NEQUICK_BASE + "modipNeQG_wrapped.asc";

    /** Pattern for delimiting regular expressions. */
    private static final Pattern SEPARATOR = Pattern.compile("\\s+");

    /** MODIP grid. */
    private double[][] grid;

    /**
     * Build a new instance.
     */
    MODIPLoader() {
        this.grid = null;
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
     * Load the data using supported names.
     */
    public void loadMODIPGrid() {
        try (InputStream in = MODIPLoader.class.getResourceAsStream(SUPPORTED_NAME)) {
            loadData(in, SUPPORTED_NAME);
        } catch (IOException e) {
            throw new OrekitException(OrekitMessages.INTERNAL_ERROR, e);
        }

        // Throw an exception if MODIP grid was not loaded properly
        if (grid == null) {
            throw new OrekitException(OrekitMessages.MODIP_GRID_NOT_LOADED, SUPPORTED_NAME);
        }
    }

    /**
     * Load data from a stream.
     *
     * @param input input stream
     * @param name  name of the file
     * @exception IOException if data can't be read
     */
    private void loadData(final InputStream input, final String name) throws IOException {

        // Grid size
        final int size = 39;

        // Initialize array
        final double[][] array = new double[size][size];

        // Open stream and parse data
        int lineNumber = 0;
        String line = null;
        try (InputStreamReader isr = new InputStreamReader(input, StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(isr)) {

            for (line = br.readLine(); line != null; line = br.readLine()) {
                ++lineNumber;
                line = line.trim();

                // Read grid data
                if (!line.isEmpty()) {
                    final String[] modip_line = SEPARATOR.split(line);
                    for (int column = 0; column < modip_line.length; column++) {
                        array[lineNumber - 1][column] = Double.parseDouble(modip_line[column]);
                    }
                }

            }

        } catch (NumberFormatException nfe) {
            throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, lineNumber, name, line);
        }

        // Clone parsed grid
        grid = array.clone();

    }
}
