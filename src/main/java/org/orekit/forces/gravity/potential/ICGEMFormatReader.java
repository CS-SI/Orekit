/* Copyright 2002-2016 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.errors.OrekitParseException;
import org.orekit.time.DateComponents;
import org.orekit.utils.Constants;

/** Reader for the ICGEM gravity field format.
 *
 * <p>This format is used to describe the gravity field of EIGEN models
 * published by the GFZ Potsdam since 2004. It is described in Franz
 * Barthelmes and Christoph F&ouml;rste paper: "the ICGEM-format".
 * The 2006-02-28 version of this paper can be found <a
 * href="http://op.gfz-potsdam.de/grace/results/grav/g005_ICGEM-Format.pdf">here</a>
 * and the 2011-06-07 version of this paper can be found <a
 * href="http://icgem.gfz-potsdam.de/ICGEM/documents/ICGEM-Format-2011.pdf">here</a>.
 * These versions differ in time-dependent coefficients, which are linear-only prior
 * to 2011 (up to eigen-5 model) and have also harmonic effects after that date
 * (starting with eigen-6 model). Both versions are supported by the class.</p>
 * <p>
 * This reader uses relaxed check on the gravity constant key so any key ending
 * in gravity_constant is accepted and not only earth_gravity_constant as specified
 * in the previous documents. This allows to read also non Earth gravity fields
 * as found in <a href="http://icgem.gfz-potsdam.de/ICGEM/ModelstabBodies.html">ICGEM
 * - Gravity Field Models of other Celestial Bodies</a> page to be read.
 * </p>
 * <p>
 * In order to simplify implementation, some design choices have been made: the
 * reference date and the periods of harmonic pulsations are stored globally and
 * not on a per-coefficient basis. This has the following implications:
 * </p>
 * <ul>
 *   <li>
 *     all time-stamped coefficients must share the same reference date, otherwise
 *     an error will be triggered during parsing,
 *   </li>
 *   <li>
 *     in order to avoid too large memory and CPU consumption, only a few different
 *     periods should appear in the file,
 *   </li>
 *   <li>
 *     only one occurrence of each coefficient may appear in the file, otherwise
 *     an error will be triggered during parsing. Multiple occurrences with different
 *     time stamps are forbidden (both because they correspond to a duplicated entry
 *     and because they define two different reference dates as per previous design
 *     choice).
 *   </li>
 * </ul>
 *
 * <p> The proper way to use this class is to call the {@link GravityFieldFactory}
 *  which will determine which reader to use with the selected gravity field file.</p>
 *
 * @see GravityFieldFactory
 * @author Luc Maisonobe
 */
public class ICGEMFormatReader extends PotentialCoefficientsReader {

    /** Product type. */
    private static final String PRODUCT_TYPE            = "product_type";

    /** Gravity field product type. */
    private static final String GRAVITY_FIELD           = "gravity_field";

    /** Gravity constant marker. */
    private static final String GRAVITY_CONSTANT        = "gravity_constant";

    /** Reference radius. */
    private static final String REFERENCE_RADIUS        = "radius";

    /** Max degree. */
    private static final String MAX_DEGREE              = "max_degree";

    /** Tide system indicator. */
    private static final String TIDE_SYSTEM_INDICATOR   = "tide_system";

    /** Indicator value for zero-tide system. */
    private static final String ZERO_TIDE               = "zero_tide";

    /** Indicator value for tide-free system. */
    private static final String TIDE_FREE               = "tide_free";

    /** Indicator value for unknown tide system. */
    private static final String TIDE_UNKNOWN            = "unknown";

    /** Normalization indicator. */
    private static final String NORMALIZATION_INDICATOR = "norm";

    /** Indicator value for normalized coefficients. */
    private static final String NORMALIZED              = "fully_normalized";

    /** Indicator value for un-normalized coefficients. */
    private static final String UNNORMALIZED            = "unnormalized";

    /** End of header marker. */
    private static final String END_OF_HEADER           = "end_of_head";

    /** Gravity field coefficient. */
    private static final String GFC                     = "gfc";

    /** Time stamped gravity field coefficient. */
    private static final String GFCT                    = "gfct";

    /** Gravity field coefficient first time derivative. */
    private static final String DOT                     = "dot";

    /** Gravity field coefficient trend. */
    private static final String TRND                    = "trnd";

    /** Gravity field coefficient sine amplitude. */
    private static final String ASIN                    = "asin";

    /** Gravity field coefficient cosine amplitude. */
    private static final String ACOS                    = "acos";

    /** Tide system. */
    private TideSystem tideSystem;

    /** Indicator for normalized coefficients. */
    private boolean normalized;

    /** Reference date. */
    private DateComponents referenceDate;

    /** Secular trend of the cosine coefficients. */
    private final List<List<Double>> cTrend;

    /** Secular trend of the sine coefficients. */
    private final List<List<Double>> sTrend;

    /** Cosine part of the cosine coefficients pulsation. */
    private final Map<Double, List<List<Double>>> cCos;

    /** Sine part of the cosine coefficients pulsation. */
    private final Map<Double, List<List<Double>>> cSin;

    /** Cosine part of the sine coefficients pulsation. */
    private final Map<Double, List<List<Double>>> sCos;

    /** Sine part of the sine coefficients pulsation. */
    private final Map<Double, List<List<Double>>> sSin;

    /** Simple constructor.
     * @param supportedNames regular expression for supported files names
     * @param missingCoefficientsAllowed if true, allows missing coefficients in the input data
     */
    public ICGEMFormatReader(final String supportedNames, final boolean missingCoefficientsAllowed) {
        super(supportedNames, missingCoefficientsAllowed);
        referenceDate = null;
        cTrend = new ArrayList<List<Double>>();
        sTrend = new ArrayList<List<Double>>();
        cCos   = new HashMap<Double, List<List<Double>>>();
        cSin   = new HashMap<Double, List<List<Double>>>();
        sCos   = new HashMap<Double, List<List<Double>>>();
        sSin   = new HashMap<Double, List<List<Double>>>();
    }

    /** {@inheritDoc} */
    public void loadData(final InputStream input, final String name)
        throws IOException, ParseException, OrekitException {

        // reset the indicator before loading any data
        setReadComplete(false);
        referenceDate = null;
        cTrend.clear();
        sTrend.clear();
        cCos.clear();
        cSin.clear();
        sCos.clear();
        sSin.clear();

        // by default, the field is normalized with unknown tide system
        // (will be overridden later if non-default)
        normalized = true;
        tideSystem = TideSystem.UNKNOWN;

        final BufferedReader r = new BufferedReader(new InputStreamReader(input, "UTF-8"));
        boolean inHeader = true;
        double[][] c               = null;
        double[][] s               = null;
        boolean okCoeffs           = false;
        int lineNumber   = 0;
        for (String line = r.readLine(); line != null; line = r.readLine()) {
            try {
                ++lineNumber;
                if (line.trim().length() == 0) {
                    continue;
                }
                final String[] tab = line.split("\\s+");
                if (inHeader) {
                    if ((tab.length == 2) && PRODUCT_TYPE.equals(tab[0])) {
                        if (!GRAVITY_FIELD.equals(tab[1])) {
                            throw new OrekitParseException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                           lineNumber, name, line);
                        }
                    } else if ((tab.length == 2) && tab[0].endsWith(GRAVITY_CONSTANT)) {
                        setMu(parseDouble(tab[1]));
                    } else if ((tab.length == 2) && REFERENCE_RADIUS.equals(tab[0])) {
                        setAe(parseDouble(tab[1]));
                    } else if ((tab.length == 2) && MAX_DEGREE.equals(tab[0])) {

                        final int degree = FastMath.min(getMaxParseDegree(), Integer.parseInt(tab[1]));
                        final int order  = FastMath.min(getMaxParseOrder(), degree);
                        c = buildTriangularArray(degree, order, missingCoefficientsAllowed() ? 0.0 : Double.NaN);
                        s = buildTriangularArray(degree, order, missingCoefficientsAllowed() ? 0.0 : Double.NaN);

                    } else if ((tab.length == 2) && TIDE_SYSTEM_INDICATOR.equals(tab[0])) {
                        if (ZERO_TIDE.equals(tab[1])) {
                            tideSystem = TideSystem.ZERO_TIDE;
                        } else if (TIDE_FREE.equals(tab[1])) {
                            tideSystem = TideSystem.TIDE_FREE;
                        } else if (TIDE_UNKNOWN.equals(tab[1])) {
                            tideSystem = TideSystem.UNKNOWN;
                        } else {
                            throw new OrekitParseException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                           lineNumber, name, line);
                        }
                    } else if ((tab.length == 2) && NORMALIZATION_INDICATOR.equals(tab[0])) {
                        if (NORMALIZED.equals(tab[1])) {
                            normalized = true;
                        } else if (UNNORMALIZED.equals(tab[1])) {
                            normalized = false;
                        } else {
                            throw new OrekitParseException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                           lineNumber, name, line);
                        }
                    } else if ((tab.length == 2) && END_OF_HEADER.equals(tab[0])) {
                        inHeader = false;
                    }
                } else {
                    if ((tab.length == 7 && GFC.equals(tab[0])) || (tab.length == 8 && GFCT.equals(tab[0]))) {

                        final int i = Integer.parseInt(tab[1]);
                        final int j = Integer.parseInt(tab[2]);
                        if (i < c.length && j < c[i].length) {

                            parseCoefficient(tab[3], c, i, j, "C", name);
                            parseCoefficient(tab[4], s, i, j, "S", name);
                            okCoeffs = true;

                            if (tab.length == 8) {
                                // check the reference date (format yyyymmdd)
                                final DateComponents localRef = new DateComponents(Integer.parseInt(tab[7].substring(0, 4)),
                                                                                   Integer.parseInt(tab[7].substring(4, 6)),
                                                                                   Integer.parseInt(tab[7].substring(6, 8)));
                                if (referenceDate == null) {
                                    // first reference found, store it
                                    referenceDate = localRef;
                                } else if (!referenceDate.equals(localRef)) {
                                    throw new OrekitException(OrekitMessages.SEVERAL_REFERENCE_DATES_IN_GRAVITY_FIELD,
                                                              referenceDate, localRef, name);
                                }
                            }

                        }
                    } else if (tab.length == 7 && (DOT.equals(tab[0]) || TRND.equals(tab[0]))) {

                        final int i = Integer.parseInt(tab[1]);
                        final int j = Integer.parseInt(tab[2]);
                        if (i < c.length && j < c[i].length) {

                            // store the secular trend coefficients
                            extendListOfLists(cTrend, i, j, 0.0);
                            extendListOfLists(sTrend, i, j, 0.0);
                            parseCoefficient(tab[3], cTrend, i, j, "Ctrend", name);
                            parseCoefficient(tab[4], sTrend, i, j, "Strend", name);

                        }

                    } else if (tab.length == 8 && (ASIN.equals(tab[0]) || ACOS.equals(tab[0]))) {

                        final int i = Integer.parseInt(tab[1]);
                        final int j = Integer.parseInt(tab[2]);
                        if (i < c.length && j < c[i].length) {

                            // extract arrays corresponding to period
                            final Double period = Double.valueOf(tab[7]);
                            if (!cCos.containsKey(period)) {
                                cCos.put(period, new ArrayList<List<Double>>());
                                cSin.put(period, new ArrayList<List<Double>>());
                                sCos.put(period, new ArrayList<List<Double>>());
                                sSin.put(period, new ArrayList<List<Double>>());
                            }
                            final List<List<Double>> cCosPeriod = cCos.get(period);
                            final List<List<Double>> cSinPeriod = cSin.get(period);
                            final List<List<Double>> sCosPeriod = sCos.get(period);
                            final List<List<Double>> sSinPeriod = sSin.get(period);

                            // store the pulsation coefficients
                            extendListOfLists(cCosPeriod, i, j, 0.0);
                            extendListOfLists(cSinPeriod, i, j, 0.0);
                            extendListOfLists(sCosPeriod, i, j, 0.0);
                            extendListOfLists(sSinPeriod, i, j, 0.0);
                            if (ACOS.equals(tab[0])) {
                                parseCoefficient(tab[3], cCosPeriod, i, j, "Ccos", name);
                                parseCoefficient(tab[4], sCosPeriod, i, j, "SCos", name);
                            } else {
                                parseCoefficient(tab[3], cSinPeriod, i, j, "Csin", name);
                                parseCoefficient(tab[4], sSinPeriod, i, j, "Ssin", name);
                            }

                        }

                    } else {
                        throw new OrekitParseException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                       lineNumber, name, line);
                    }
                }
            } catch (NumberFormatException nfe) {
                final OrekitParseException pe = new OrekitParseException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                                         lineNumber, name, line);
                pe.initCause(nfe);
                throw pe;
            }
        }

        if (missingCoefficientsAllowed() && c.length > 0 && c[0].length > 0) {
            // ensure at least the (0, 0) element is properly set
            if (Precision.equals(c[0][0], 0.0, 0)) {
                c[0][0] = 1.0;
            }
        }

        if (Double.isNaN(getAe()) || Double.isNaN(getMu()) || !okCoeffs) {
            String loaderName = getClass().getName();
            loaderName = loaderName.substring(loaderName.lastIndexOf('.') + 1);
            throw new OrekitException(OrekitMessages.UNEXPECTED_FILE_FORMAT_ERROR_FOR_LOADER,
                                      name, loaderName);
        }

        setRawCoefficients(normalized, c, s, name);
        setTideSystem(tideSystem);
        setReadComplete(true);

    }

    /** Get a provider for read spherical harmonics coefficients.
     * <p>
     * ICGEM fields do include time-dependent parts which are taken into account
     * in the returned provider.
     * </p>
     * @param wantNormalized if true, the provider will provide normalized coefficients,
     * otherwise it will provide un-normalized coefficients
     * @param degree maximal degree
     * @param order maximal order
     * @return a new provider
     * @exception OrekitException if the requested maximal degree or order exceeds the
     * available degree or order or if no gravity field has read yet
     * @since 6.0
     */
    public RawSphericalHarmonicsProvider getProvider(final boolean wantNormalized,
                                                     final int degree, final int order)
        throws OrekitException {

        RawSphericalHarmonicsProvider provider = getConstantProvider(wantNormalized, degree, order);
        if (cTrend.isEmpty() && cCos.isEmpty()) {
            // there are no time-dependent coefficients
            return provider;
        }

        if (!cTrend.isEmpty()) {

            // add the secular trend layer
            final double[][] cArrayTrend = toArray(cTrend);
            final double[][] sArrayTrend = toArray(sTrend);
            rescale(1.0 / Constants.JULIAN_YEAR, normalized, cArrayTrend, sArrayTrend, wantNormalized, cArrayTrend, sArrayTrend);
            provider = new SecularTrendSphericalHarmonics(provider, referenceDate, cArrayTrend, sArrayTrend);

        }

        for (final Map.Entry<Double, List<List<Double>>> entry : cCos.entrySet()) {

            final double period = entry.getKey();

            // add the pulsating layer for the current period
            final double[][] cArrayCos = toArray(cCos.get(period));
            final double[][] sArrayCos = toArray(sCos.get(period));
            final double[][] cArraySin = toArray(cSin.get(period));
            final double[][] sArraySin = toArray(sSin.get(period));
            rescale(1.0, normalized, cArrayCos, sArrayCos, wantNormalized, cArrayCos, sArrayCos);
            rescale(1.0, normalized, cArraySin, sArraySin, wantNormalized, cArraySin, sArraySin);
            provider = new PulsatingSphericalHarmonics(provider, period * Constants.JULIAN_YEAR,
                                                       cArrayCos, cArraySin, sArrayCos, sArraySin);

        }

        return provider;

    }

}
