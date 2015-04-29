/* Copyright 2011-2012 Space Applications Services
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
package org.orekit.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StreamTokenizer;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;

import org.orekit.data.DataLoader;
import org.orekit.errors.OrekitException;

/** Used to read an interpolation table from a data file.
 * @author Thomas Neidhart
 */
public class InterpolationTableLoader implements DataLoader {

    /** Abscissa grid for the bi-variate interpolation function read from the file. */
    private double[] xArr;

    /** Ordinate grid for the bi-variate interpolation function read from the file. */
    private double[] yArr;

    /** Values samples for the bi-variate interpolation function read from the file. */
    private double[][] fArr;

    /** Returns a copy of the abscissa grid for the interpolation function.
     * @return the abscissa grid for the interpolation function,
     *         or <code>null</code> if the file could not be read
     */
    public double[] getAbscissaGrid() {
        return xArr.clone();
    }

    /** Returns a copy of the ordinate grid for the interpolation function.
     * @return the ordinate grid for the interpolation function,
     *         or <code>null</code> if the file could not be read
     */
    public double[] getOrdinateGrid() {
        return yArr.clone();
    }

    /** Returns a copy of the values samples for the interpolation function.
     * @return the values samples for the interpolation function,
     *         or <code>null</code> if the file could not be read
     */
    public double[][] getValuesSamples() {
        return fArr.clone();
    }

    /** {@inheritDoc} */
    public boolean stillAcceptsData() {
        return xArr == null;
    }

    /** Loads an bi-variate interpolation table from the given {@link InputStream}.
     * The format of the table is as follows (number of rows/columns can be extended):
     * <pre>
     *  Table: tableName
     *
     *      | 0.0 |  60.0 |  66.0
     *  -------------------------
     *    0 | 0.0 | 0.003 | 0.006
     *  500 | 0.0 | 0.003 | 0.006
     * </pre>
     * @param input the input stream to read data from
     * @param name  the name of the input file
     * @exception IOException if data can't be read
     * @exception ParseException if data can't be parsed
     * @exception OrekitException if some data is missing or unexpected
     *                            data is encountered
     */
    public void loadData(final InputStream input, final String name)
        throws IOException, ParseException, OrekitException {

        final List<Double> xValues = new LinkedList<Double>();
        final List<Double> yValues = new LinkedList<Double>();
        final LinkedList<List<Double>> cellValues = new LinkedList<List<Double>>();

        final StreamTokenizer tokenizer =
            new StreamTokenizer(new BufferedReader(new InputStreamReader(input, "UTF-8")));

        // ignore comments starting with a #
        tokenizer.commentChar('#');
        tokenizer.eolIsSignificant(true);

        int tokenCount = 0;
        boolean headerRow = false;
        boolean done = false;

        do {
            switch (tokenizer.nextToken()) {

                case StreamTokenizer.TT_EOF:
                    done = true;
                    break;

                case StreamTokenizer.TT_EOL:
                    // end of header row
                    if (yValues.size() > 0) {
                        headerRow = false;
                    }
                    tokenCount = 0;
                    break;

                case StreamTokenizer.TT_NUMBER:
                    if (headerRow) {
                        yValues.add(tokenizer.nval);
                    } else {
                        if (tokenCount == 0) {
                            xValues.add(tokenizer.nval);
                            cellValues.add(new LinkedList<Double>());
                        } else {
                            cellValues.getLast().add(tokenizer.nval);
                        }
                    }
                    tokenCount++;
                    break;

                case StreamTokenizer.TT_WORD:
                    // we are in the header row now
                    if (tokenizer.sval.startsWith("Table")) {
                        headerRow = true;
                    }
                    break;

                default:
                    break;
            }

        } while (!done);

        xArr = toPrimitiveArray(xValues);
        yArr = toPrimitiveArray(yValues);
        fArr = new double[cellValues.size()][];
        int idx = 0;

        for (List<Double> row : cellValues) {
            fArr[idx++] = toPrimitiveArray(row);
        }

    }

    /** Converts a list of {@link Double} objects into an array of double primitives.
     * @param list the list of {@link Double} objects
     * @return the double array containing the list elements
     */
    private double[] toPrimitiveArray(final List<Double> list) {
        final double[] result = new double[list.size()];
        int idx = 0;
        for (Double element : list) {
            result[idx++] = element;
        }
        return result;
    }
}
