/* Copyright 2002-2025 CS GROUP
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
package org.orekit.models.earth.ionosphere.nequick;

import org.hipparchus.util.FastMath;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.DateComponents;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.regex.Pattern;

/**
 * Parser for CCIR files.
 * <p>
 * Numerical grid maps which describe the regular variation of the ionosphere. They are used to derive other variables
 * such as critical frequencies and transmission factors.
 * </p> <p>
 * The coefficients correspond to low and high solar activity conditions.
 * </p> <p>
 * The CCIR file naming convention is ccirXX.asc where each XX means month + 10.
 * </p> <p>
 * Coefficients are store into tow arrays, F2 and Fm3. F2 coefficients are used for the computation of the F2 layer
 * critical frequency. Fm3 for the computation of the F2 layer maximum usable frequency factor. The size of these two
 * arrays is fixed and discussed into the section 2.5.3.2 of the reference document.
 * </p>
 * @author Bryan Cazabonne
 * @since 13.0
 */
class CCIRLoader {

    /** Total number of F2 coefficients contained in the file. */
    private static final int NUMBER_F2_COEFFICIENTS = 1976;

    /** Pattern for delimiting regular expressions. */
    private static final Pattern SEPARATOR = Pattern.compile("\\s+");

    /** Rows number for F2 and Fm3 arrays. */
    private static final int ROWS = 2;

    /** Columns number for F2 array. */
    private static final int TOTAL_COLUMNS_F2 = 76;

    /** Columns number for Fm3 array. */
    private static final int TOTAL_COLUMNS_FM3 = 49;

    /** Depth of F2 array. */
    private static final int DEPTH_F2 = 13;

    /** Depth of Fm3 array. */
    private static final int DEPTH_FM3 = 9;

    /** F2 coefficients used for the computation of the F2 layer critical frequency. */
    private double[][][] parsedF2;

    /** Fm3 coefficients used for the computation of the F2 layer maximum usable frequency factor. */
    private double[][][] parsedFm3;

    /**
     * Build a new instance.
     */
    CCIRLoader() {
        this.parsedF2  = new double[ROWS][TOTAL_COLUMNS_F2][DEPTH_F2];
        this.parsedFm3 = new double[ROWS][TOTAL_COLUMNS_FM3][DEPTH_FM3];
    }

    /**
     * Get the F2 coefficients used for the computation of the F2 layer critical frequency.
     *
     * @return the F2 coefficients
     */
    public double[][][] getF2() {
        return parsedF2.clone();
    }

    /**
     * Get the Fm3 coefficients used for the computation of the F2 layer maximum usable frequency factor.
     *
     * @return the F2 coefficients
     */
    public double[][][] getFm3() {
        return parsedFm3.clone();
    }

    /**
     * Load the data for a given month.
     *
     * @param dateComponents month given but its DateComponents
     */
    public void loadCCIRCoefficients(final DateComponents dateComponents) {

        // The files are named ccirXX.asc where XX substitute the month of the year + 10
        final int currentMonth = dateComponents.getMonth();
        final String fileName = String.format("/assets/org/orekit/nequick/ccir%02d.asc", currentMonth + 10);
        loadData(new DataSource(fileName, () -> CCIRLoader.class.getResourceAsStream(fileName)));

    }

    /** Load data.
     * @param dataSource data source
     */
    public void loadData(final DataSource dataSource) {

        // Placeholders for parsed data
        int    lineNumber       = 0;
        int    index            = 0;
        int    currentRowF2     = 0;
        int    currentColumnF2  = 0;
        int    currentDepthF2   = 0;
        int    currentRowFm3    = 0;
        int    currentColumnFm3 = 0;
        int    currentDepthFm3  = 0;
        String line             = null;

        try (Reader         r  = dataSource.getOpener().openReaderOnce();
             BufferedReader br = new BufferedReader(r)) {

            for (line = br.readLine(); line != null; line = br.readLine()) {
                ++lineNumber;
                line = line.trim();

                // Read grid data
                if (!line.isEmpty()) {
                    final String[] ccir_line = SEPARATOR.split(line);
                    for (final String field : ccir_line) {

                        if (index < NUMBER_F2_COEFFICIENTS) {
                            // Parse F2 coefficients
                            if (currentDepthF2 >= DEPTH_F2 && currentColumnF2 < (TOTAL_COLUMNS_F2 - 1)) {
                                currentDepthF2 = 0;
                                currentColumnF2++;
                            } else if (currentDepthF2 >= DEPTH_F2 && currentColumnF2 >= (TOTAL_COLUMNS_F2 - 1)) {
                                currentDepthF2 = 0;
                                currentColumnF2 = 0;
                                currentRowF2++;
                            }
                            parsedF2[currentRowF2][currentColumnF2][currentDepthF2++] = Double.parseDouble(field);
                            index++;
                        } else {
                            // Parse Fm3 coefficients
                            if (currentDepthFm3 >= DEPTH_FM3 && currentColumnFm3 < (TOTAL_COLUMNS_FM3 - 1)) {
                                currentDepthFm3 = 0;
                                currentColumnFm3++;
                            } else if (currentDepthFm3 >= DEPTH_FM3 && currentColumnFm3 >= (TOTAL_COLUMNS_FM3 - 1)) {
                                currentDepthFm3 = 0;
                                currentColumnFm3 = 0;
                                currentRowFm3++;
                            }
                            parsedFm3[currentRowFm3][currentColumnFm3][currentDepthFm3++] = Double.parseDouble(field);
                            index++;
                        }

                    }
                }

            }

        } catch (IOException ioe) {
            throw new OrekitException(ioe, OrekitMessages.NEQUICK_F2_FM3_NOT_LOADED, dataSource.getName());
        } catch (NumberFormatException nfe) {
            throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                      lineNumber, dataSource.getName(), line);
        }

        checkDimensions(currentRowF2,  currentColumnF2,  currentDepthF2,  parsedF2,  dataSource.getName());
        checkDimensions(currentRowFm3, currentColumnFm3, currentDepthFm3, parsedFm3, dataSource.getName());

    }

    /** Check dimensions.
     * @param currentRow current row index
     * @param currentColumn current column index
     * @param currentDepth current depth index
     * @param array storage array
     * @param name data source name
     */
    private void checkDimensions(final int currentRow, final int currentColumn, final int currentDepth,
                                 final double[][][] array, final String name) {
        // just three equality tests
        // written in a way test coverage doesn't complain about missing cases…
        if (FastMath.max(FastMath.max(FastMath.abs(currentRow - (array.length - 1)),
                                      FastMath.abs(currentColumn - (array[0].length - 1))),
                         FastMath.abs(currentDepth - array[0][0].length)) != 0) {
            throw new OrekitException(OrekitMessages.NEQUICK_F2_FM3_NOT_LOADED, name);
        }
    }

}
