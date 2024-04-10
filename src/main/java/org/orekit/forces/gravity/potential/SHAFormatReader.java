/* Contributed in the public domain.
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
package org.orekit.forces.gravity.potential;

import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Arrays;
import java.util.regex.Pattern;

/** Reader for the SHA gravity field format.
 *
 * <p> This format is used by some lunar gravity models distributed by
 * NASA's Planetary Geology, Geophysics and Geochemistry Laboratory such as
 * GRGM1200B and GRGM1200L. It is a simple ASCII format, described in
 * <a href="https://pgda.gsfc.nasa.gov/products/75"> the GRGM1200B model product site</a>.
 * The first line contains 4 constants: model GM, mean radius, maximum degree and maximum order.
 * All other lines contain 6 entries: degree, order, Clm, Slm, sigma Clm and sigma Slm
 * (formal errors of Clm and Slm).
 *
 * <p> The proper way to use this class is to call the {@link GravityFieldFactory}
 *  which will determine which reader to use with the selected gravity field file.</p>
 *
 * @see GravityFields
 * @author Rafael Ayala
 */
public class SHAFormatReader extends PotentialCoefficientsReader {


    /** Default "0" text value. */
    private static final String ZERO = "0.0";

    /** Default "1" text value. */
    private static final String ONE = "1.0";

    /** Expression for multiple spaces. */
    private static final String SPACES = "\\s+";

    /** Pattern for delimiting regular expressions. */
    private static final Pattern SEPARATOR = Pattern.compile(SPACES);

    /** Pattern for real numbers. */
    private static final Pattern REAL =  Pattern.compile("[-+]?\\d?\\.\\d+[eEdD][-+]\\d\\d");

    /** Pattern for header line. */
    private static final Pattern HEADER_LINE =  Pattern.compile("^\\s*" + REAL + SPACES + REAL + "\\s+\\d+\\s+\\d+\\s*$");

    /** Pattern for data lines. */
    private static final Pattern DATA_LINE = Pattern.compile("^\\s*\\d+\\s+\\d+\\s+" + REAL + SPACES + REAL + SPACES + REAL + SPACES + REAL + "\\s*$");

    /** Start degree and order for coefficients container. */
    private static final int START_DEGREE_ORDER = 120;

    /** Simple constructor.
     * @param supportedNames regular expression for supported files names
     * @param missingCoefficientsAllowed if true, allows missing coefficients in the input data
     * @since 12.2
     */
    public SHAFormatReader(final String supportedNames, final boolean missingCoefficientsAllowed) {
        super(supportedNames, missingCoefficientsAllowed, null);
    }

    /** {@inheritDoc} */
    public void loadData(final InputStream input, final String name) throws IOException, ParseException, OrekitException {

        // reset the indicator before loading any data
        setReadComplete(false);
        setTideSystem(TideSystem.UNKNOWN);
        int       lineNumber = 0;
        int       maxDegree;
        int       maxOrder;
        String    line = null;
        Container container = new Container(START_DEGREE_ORDER, START_DEGREE_ORDER,
                                            missingCoefficientsAllowed() ? 0.0 : Double.NaN);
        try (BufferedReader r = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            for (line = r.readLine(); line != null; line = r.readLine()) {
                ++lineNumber;
                if (lineNumber == 1) {
                    // match the pattern of the header line
                    if (!HEADER_LINE.matcher(line).matches()) {
                        throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                  lineNumber, name, line);
                    }
                    final String[] headerFields = SEPARATOR.split(line.trim());
                    setMu(Double.parseDouble(headerFields[0]));
                    setAe(Double.parseDouble(headerFields[1]));
                    maxDegree = Integer.parseInt(headerFields[2]);
                    maxOrder = Integer.parseInt(headerFields[3]);
                    container = container.resize(maxDegree, maxOrder);
                    parseCoefficient(ONE, container.flattener, container.c, 0, 0, "C", name);
                    parseCoefficient(ZERO, container.flattener, container.s, 0, 0, "S", name);
                    parseCoefficient(ZERO, container.flattener, container.s, 1, 0, "C", name);
                    parseCoefficient(ZERO, container.flattener, container.s, 1, 0, "S", name);
                    parseCoefficient(ZERO, container.flattener, container.s, 1, 1, "C", name);
                    parseCoefficient(ZERO, container.flattener, container.s, 1, 1, "S", name);
                } else if (lineNumber > 1) {
                    // match the pattern of the data lines
                    if (!DATA_LINE.matcher(line).matches()) {
                        throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                lineNumber, name, line);
                    }
                    final String[] dataFields = SEPARATOR.split(line.trim());
                    // we want to assign the values of the data fields to the corresponding variables
                    final int n = Integer.parseInt(dataFields[0]);
                    final int m = Integer.parseInt(dataFields[1]);
                    parseCoefficient(dataFields[2], container.flattener, container.c, n, m, "C", name);
                    parseCoefficient(dataFields[3], container.flattener, container.s, n, m, "S", name);
                }
            }
        } catch (NumberFormatException nfe) {
            throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                      lineNumber, name, line);
        }
        setRawCoefficients(true, container.flattener, container.c, container.s, name);
        setReadComplete(true);
    }

    /** Provider for read spherical harmonics coefficients.
     * Like EGM fields, SHA fields don't include time-dependent parts,
     * so this method returns directly a constant provider.
     * @param wantNormalized if true, the provider will provide normalized coefficients,
     * otherwise it will provide un-normalized coefficients
     * @param degree maximal degree
     * @param order maximal order
     * @return a new provider
     * @since 12.2
     */
    public RawSphericalHarmonicsProvider getProvider(final boolean wantNormalized,
                                                     final int degree, final int order) {
        return getBaseProvider(wantNormalized, degree, order);
    }

    /** Temporary container for reading coefficients.
     * @since 12.2
     */
    private static class Container {

        /** Converter from triangular to flat form. */
        private final Flattener flattener;

        /** Cosine coefficients. */
        private final double[] c;

        /** Sine coefficients. */
        private final double[] s;

        /** Initial value for coefficients. */
        private final double initialValue;

        /** Build a container with given degree and order.
         * @param degree degree of the container
         * @param order order of the container
         * @param initialValue initial value for coefficients
         */
        Container(final int degree, final int order, final double initialValue) {
            this.flattener    = new Flattener(degree, order);
            this.c            = new double[flattener.arraySize()];
            this.s            = new double[flattener.arraySize()];
            this.initialValue = initialValue;
            Arrays.fill(c, initialValue);
            Arrays.fill(s, initialValue);
        }

        /** Build a resized container.
         * @param degree degree of the resized container
         * @param order order of the resized container
         * @return resized container
         */
        Container resize(final int degree, final int order) {
            final Container resized = new Container(degree, order, initialValue);
            for (int n = 0; n <= degree; ++n) {
                for (int m = 0; m <= FastMath.min(n, order); ++m) {
                    if (flattener.withinRange(n, m)) {
                        final int rIndex = resized.flattener.index(n, m);
                        final int index  = flattener.index(n, m);
                        resized.c[rIndex] = c[index];
                        resized.s[rIndex] = s[index];
                    }
                }
            }
            return resized;
        }
    }
}
