/* Copyright 2002-2023 CS GROUP
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;

import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.utils.Constants;

/**This reader is adapted to the EGM Format.
 *
 * <p> The proper way to use this class is to call the {@link GravityFieldFactory}
 *  which will determine which reader to use with the selected gravity field file.</p>
 *
 * @see GravityFields
 * @author Fabien Maussion
 */
public class EGMFormatReader extends PotentialCoefficientsReader {

    /** Pattern for delimiting regular expressions. */
    private static final Pattern SEPARATOR = Pattern.compile("\\s+");

    /** Start degree and order for coefficients container. */
    private static final int START_DEGREE_ORDER = 120;

    /** Flag for using WGS84 values for equatorial radius and central attraction coefficient. */
    private final boolean useWgs84Coefficients;

    /** Simple constructor.
     * @param supportedNames regular expression for supported files names
     * @param missingCoefficientsAllowed if true, allows missing coefficients in the input data
     */
    public EGMFormatReader(final String supportedNames, final boolean missingCoefficientsAllowed) {
        this(supportedNames, missingCoefficientsAllowed, false);
    }

    /**
     * Simple constructor that allows overriding 'standard' EGM96 ae and mu with
     * WGS84 variants.
     *
     * @param supportedNames regular expression for supported files names
     * @param missingCoefficientsAllowed if true, allows missing coefficients in the input data
     * @param useWgs84Coefficients if true, the WGS84 values will be used for equatorial radius
     * and central attraction coefficient
     */
    public EGMFormatReader(final String supportedNames, final boolean missingCoefficientsAllowed,
                           final boolean useWgs84Coefficients) {
        super(supportedNames, missingCoefficientsAllowed, null);
        this.useWgs84Coefficients = useWgs84Coefficients;
    }


    /** {@inheritDoc} */
    public void loadData(final InputStream input, final String name)
        throws IOException, ParseException, OrekitException {

        // reset the indicator before loading any data
        setReadComplete(false);

        // both EGM96 and EGM2008 use the same values for ae and mu
        // if a new EGM model changes them, we should have some selection logic
        // based on file name (a better way would be to have the data in the
        // file...)
        if (this.useWgs84Coefficients) {
            setAe(Constants.WGS84_EARTH_EQUATORIAL_RADIUS);
            setMu(Constants.WGS84_EARTH_MU);
        } else {
            setAe(Constants.EGM96_EARTH_EQUATORIAL_RADIUS);
            setMu(Constants.EGM96_EARTH_MU);
        }

        final String lowerCaseName = name.toLowerCase(Locale.US);
        if (lowerCaseName.contains("2008") || lowerCaseName.contains("zerotide")) {
            setTideSystem(TideSystem.ZERO_TIDE);
        } else {
            setTideSystem(TideSystem.TIDE_FREE);
        }

        Container container = new Container(START_DEGREE_ORDER, START_DEGREE_ORDER,
                                            missingCoefficientsAllowed() ? 0.0 : Double.NaN);
        boolean okFields = true;
        int       maxDegree  = -1;
        int       maxOrder   = -1;
        int lineNumber = 0;
        String line = null;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            for (line = r.readLine(); okFields && line != null; line = r.readLine()) {
                lineNumber++;
                if (line.length() >= 15) {

                    // get the fields defining the current potential terms
                    final String[] tab = SEPARATOR.split(line.trim());
                    if (tab.length != 6) {
                        okFields = false;
                    }

                    final int i = Integer.parseInt(tab[0]);
                    final int j = Integer.parseInt(tab[1]);
                    if (i < 0 || j < 0) {
                        throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                  lineNumber, name, line);
                    }

                    if (i <= getMaxParseDegree() && j <= getMaxParseOrder()) {

                        while (!container.flattener.withinRange(i, j)) {
                            // we need to resize the container
                            container = container.resize(container.flattener.getDegree() * 2,
                                                         container.flattener.getOrder() * 2);
                        }

                        parseCoefficient(tab[2], container.flattener, container.c, i, j, "C", name);
                        parseCoefficient(tab[3], container.flattener, container.s, i, j, "S", name);
                        maxDegree = FastMath.max(maxDegree, i);
                        maxOrder  = FastMath.max(maxOrder,  j);

                    }

                }
            }
        } catch (NumberFormatException nfe) {
            throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                      lineNumber, name, line);
        }

        if (missingCoefficientsAllowed() && getMaxParseDegree() > 0 && getMaxParseOrder() > 0) {
            // ensure at least the (0, 0) element is properly set
            if (Precision.equals(container.c[container.flattener.index(0, 0)], 0.0, 0)) {
                container.c[container.flattener.index(0, 0)] = 1.0;
            }
        }

        if (!(okFields && maxDegree >= 0)) {
            String loaderName = getClass().getName();
            loaderName = loaderName.substring(loaderName.lastIndexOf('.') + 1);
            throw new OrekitException(OrekitMessages.UNEXPECTED_FILE_FORMAT_ERROR_FOR_LOADER,
                                      name, loaderName);
        }

        container = container.resize(maxDegree, maxOrder);
        setRawCoefficients(true, container.flattener, container.c, container.s, name);
        setReadComplete(true);

    }

    /** Get a provider for read spherical harmonics coefficients.
     * <p>
     * EGM fields don't include time-dependent parts, so this method returns
     * directly a constant provider.
     * </p>
     * @param wantNormalized if true, the provider will provide normalized coefficients,
     * otherwise it will provide un-normalized coefficients
     * @param degree maximal degree
     * @param order maximal order
     * @return a new provider
     * @since 6.0
     */
    public RawSphericalHarmonicsProvider getProvider(final boolean wantNormalized,
                                                     final int degree, final int order) {
        return getBaseProvider(wantNormalized, degree, order);
    }

    /** Temporary container for reading coefficients.
     * @since 11.1
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
