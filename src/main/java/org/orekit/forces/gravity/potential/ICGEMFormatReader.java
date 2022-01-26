/* Copyright 2002-2022 CS GROUP
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.errors.OrekitParseException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScale;
import org.orekit.utils.Constants;
import org.orekit.utils.TimeSpanMap;

/** Reader for the ICGEM gravity field format.
 *
 * <p>This format is used to describe the gravity field of EIGEN models
 * published by the GFZ Potsdam since 2004. It is described in Franz
 * Barthelmes and Christoph F&ouml;rste paper: "the ICGEM-format".
 * The 2006-02-28 version of this paper can be found <a
 * href="http://op.gfz-potsdam.de/grace/results/grav/g005_ICGEM-Format.pdf">here</a>
 * and the 2011-06-07 version of this paper can be found <a
 * href="http://icgem.gfz-potsdam.de/ICGEM-Format-2011.pdf">here</a>.
 * These versions differ in time-dependent coefficients, which are linear-only prior
 * to 2011 (up to eigen-5 model) and have also harmonic effects after that date
 * (starting with eigen-6 model). A third (undocumented as of 2018-05-14) version
 * of the file format also adds a time-span for time-dependent coefficients, allowing
 * for piecewise models. All three versions are supported by the class.</p>
 * <p>
 * This reader uses relaxed check on the gravity constant key so any key ending
 * in gravity_constant is accepted and not only earth_gravity_constant as specified
 * in the previous documents. This allows to read also non Earth gravity fields
 * as found in <a href="http://icgem.gfz-potsdam.de/tom_celestial">ICGEM
 * - Gravity Field Models of other Celestial Bodies</a> page to be read.
 * </p>
 *
 * <p> The proper way to use this class is to call the {@link GravityFieldFactory}
 *  which will determine which reader to use with the selected gravity field file.</p>
 *
 * @see GravityFields
 * @author Luc Maisonobe
 */
public class ICGEMFormatReader extends PotentialCoefficientsReader {

    /** Format. */
    private static final String FORMAT                  = "format";

    /** Supported formats. */
    private static final String SUPPORTED_FORMAT        = "icgem(\\d+\\.\\d+)";

    /** Maximum supported formats. */
    private static final double MAX_FORMAT              = 2.0;

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

    /** Pattern for delimiting regular expressions. */
    private static final Pattern SEPARATOR = Pattern.compile("\\s+");

    /** Indicator for normalized coefficients. */
    private boolean normalized;

    /** Time map of the harmonics. */
    private TimeSpanMap<PiecewiseSphericalHarmonics> map;

    /** Simple constructor.
     *
     * <p>This constructor uses the {@link DataContext#getDefault() default data context}.
     *
     * @param supportedNames regular expression for supported files names
     * @param missingCoefficientsAllowed if true, allows missing coefficients in the input data
     * @see #ICGEMFormatReader(String, boolean, TimeScale)
     */
    @DefaultDataContext
    public ICGEMFormatReader(final String supportedNames, final boolean missingCoefficientsAllowed) {
        this(supportedNames, missingCoefficientsAllowed,
                DataContext.getDefault().getTimeScales().getTT());
    }

    /**
     * Simple constructor.
     *
     * @param supportedNames             regular expression for supported files names
     * @param missingCoefficientsAllowed if true, allows missing coefficients in the input
     *                                   data
     * @param timeScale                  to use when parsing dates.
     * @since 10.1
     */
    public ICGEMFormatReader(final String supportedNames,
                             final boolean missingCoefficientsAllowed,
                             final TimeScale timeScale) {
        super(supportedNames, missingCoefficientsAllowed, timeScale);
    }

    /** {@inheritDoc} */
    public void loadData(final InputStream input, final String name)
        throws IOException, ParseException, OrekitException {

        // reset the indicator before loading any data
        setReadComplete(false);
        map = new TimeSpanMap<>(null);

        // by default, the field is normalized with unknown tide system
        // (will be overridden later if non-default)
        normalized = true;
        TideSystem tideSystem = TideSystem.UNKNOWN;

        double version   = 1.0;
        boolean inHeader = true;
        int lineNumber   = 0;
        String line      = null;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            for (line = r.readLine(); line != null; line = r.readLine()) {
                boolean parseError = false;
                ++lineNumber;
                line = line.trim();
                if (line.length() == 0) {
                    continue;
                }
                final String[] tab = SEPARATOR.split(line);
                if (inHeader) {
                    if (tab.length == 2 && FORMAT.equals(tab[0])) {
                        final Matcher matcher = Pattern.compile(SUPPORTED_FORMAT).matcher(tab[1]);
                        if (matcher.matches()) {
                            version = Double.parseDouble(matcher.group(1));
                            if (version > MAX_FORMAT) {
                                parseError = true;
                            }
                        } else {
                            parseError = true;
                        }
                    } else if (tab.length == 2 && PRODUCT_TYPE.equals(tab[0])) {
                        parseError = !GRAVITY_FIELD.equals(tab[1]);
                    } else if (tab.length == 2 && tab[0].endsWith(GRAVITY_CONSTANT)) {
                        setMu(parseDouble(tab[1]));
                    } else if (tab.length == 2 && REFERENCE_RADIUS.equals(tab[0])) {
                        setAe(parseDouble(tab[1]));
                    } else if (tab.length == 2 && MAX_DEGREE.equals(tab[0])) {

                        final int degree = FastMath.min(getMaxParseDegree(), Integer.parseInt(tab[1]));
                        final int order  = FastMath.min(getMaxParseOrder(), degree);
                        c = buildTriangularArray(degree, order, missingCoefficientsAllowed() ? 0.0 : Double.NaN);
                        s = buildTriangularArray(degree, order, missingCoefficientsAllowed() ? 0.0 : Double.NaN);

                    } else if (tab.length == 2 && TIDE_SYSTEM_INDICATOR.equals(tab[0])) {
                        if (ZERO_TIDE.equals(tab[1])) {
                            tideSystem = TideSystem.ZERO_TIDE;
                        } else if (TIDE_FREE.equals(tab[1])) {
                            tideSystem = TideSystem.TIDE_FREE;
                        } else if (TIDE_UNKNOWN.equals(tab[1])) {
                            tideSystem = TideSystem.UNKNOWN;
                        } else {
                            parseError = true;
                        }
                    } else if (tab.length == 2 && NORMALIZATION_INDICATOR.equals(tab[0])) {
                        if (NORMALIZED.equals(tab[1])) {
                            normalized = true;
                        } else if (UNNORMALIZED.equals(tab[1])) {
                            normalized = false;
                        } else {
                            parseError = true;
                        }
                    } else if (tab.length == 2 && END_OF_HEADER.equals(tab[0])) {
                        inHeader = false;
                    }
                    if (parseError) {
                        throw new OrekitParseException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                       lineNumber, name, line);
                    }
                } else {
                    if (tab.length == 7 && GFC.equals(tab[0]) || (tab.length == 8 || tab.length == 9) && GFCT.equals(tab[0])) {

                        final int i = Integer.parseInt(tab[1]);
                        final int j = Integer.parseInt(tab[2]);
                        if (i < c.length && j < c[i].length) {

                            parseCoefficient(tab[3], c, i, j, "C", name);
                            parseCoefficient(tab[4], s, i, j, "S", name);
                            okCoeffs = true;

                            if (tab.length > 8) {
                                if (version < 2.0) {
                                    // before version 2.0, there is only one reference date
                                    if (tab.length != 8) {
                                        parseError = true;
                                    } else {
                                        final AbsoluteDate lineDate = parseDate(tab[7]);
                                        if (referenceDate == null) {
                                            // first reference found, store it
                                            referenceDate = lineDate;
                                        } else if (!referenceDate.equals(lineDate)) {
                                            // we already know the reference date, check this lines does not define a new one
                                            throw new OrekitException(OrekitMessages.SEVERAL_REFERENCE_DATES_IN_GRAVITY_FIELD,
                                                                      referenceDate, lineDate, name,
                                                                      lineDate.durationFrom(referenceDate));
                                        }
                                    }
                                } else {
                                    // starting with version 2.0, two reference dates define validity intervals
                                    if (tab.length != 9) {
                                        parseError = true;
                                    } else {
                                        // TODO
                                    }
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
                                cCos.put(period, new ArrayList<>());
                                cSin.put(period, new ArrayList<>());
                                sCos.put(period, new ArrayList<>());
                                sSin.put(period, new ArrayList<>());
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
                        parseError = true;
                    }

                    if (parseError) {
                        throw new OrekitParseException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                       lineNumber, name, line);
                    }

                }

            }
        } catch (NumberFormatException nfe) {
            throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                      lineNumber, name, line);
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
     * @since 6.0
     */
    public RawSphericalHarmonicsProvider getProvider(final boolean wantNormalized,
                                                     final int degree, final int order) {

        return new RawSphericalHarmonicsProvider() {

            /** {@inheritDoc} */
            @Override
            public int getMaxDegree() {
                return map.getFirstSpan().getData().getConstant().getMaxDegree();
            }

            /** {@inheritDoc} */
            @Override
            public int getMaxOrder() {
                return map.getFirstSpan().getData().getConstant().getMaxOrder();
            }

            /** {@inheritDoc} */
            @Override
            public double getMu() {
                return map.getFirstSpan().getData().getConstant().getMu();
            }

            /** {@inheritDoc} */
            @Override
            public double getAe() {
                return map.getFirstSpan().getData().getConstant().getAe();
            }

            /** {@inheritDoc} */
            @Override
            public AbsoluteDate getReferenceDate() {
                return map.getFirstSpan().getData().getConstant().getReferenceDate();
            }

            /** {@inheritDoc} */
            @Override
            public TideSystem getTideSystem() {
                return map.getFirstSpan().getData().getConstant().getTideSystem();
            }

            /** {@inheritDoc} */
            @Override
            public double getOffset(final AbsoluteDate date) {
                return map.get(date).getConstant().getOffset(date);
            }

            /** {@inheritDoc} */
            @Override
            public RawSphericalHarmonics onDate(final AbsoluteDate date) {
                return map.get(date).onDate(date);
            }

        };

    }

    /** Parse a reference date.
     * <p>
     * The reference dates have either the format yyyymmdd (for 2011 format)
     * or the format yyyymmdd.xxxx (for format version 2.0). The 2.0 format
     * is not described anywhere (at least I did not find any references),
     * but the .xxxx fractional part seems to be hours and minutes chosen
     * close to some strong earthquakes looking at the dates in Eigen 6S4 file
     * with non-zero fractional part and the corresponding earthquakes hours
     * (19850109.1751 vs. 1985-01-09T19:28:21, but it was not really a big quake,
     * maybe there is a confusion with the 1985 Mexico earthquake at 1985-09-19T13:17:47,
     * 20020815.0817 vs 2002-08-15:05:30:26, 20041226.0060 vs. 2004-12-26T00:58:53,
     * 20100227.0735 vs. 2010-02-27T06:34:11, and 20110311.0515 vs. 2011-03-11T05:46:24).
     * We guess the .0060 fractional part for the 2004 Sumatra-Andaman islands
     * earthquake results from sloppy rounding when writing the file.
     * </p>
     * @param field text field containing the date
     * @return parsed date
     * @since 11.1
     */
    private AbsoluteDate parseDate(final String field) {

        // check the date part (format yyyymmdd)
        final DateComponents dc = new DateComponents(Integer.parseInt(field.substring(0, 4)),
                                                     Integer.parseInt(field.substring(4, 6)),
                                                     Integer.parseInt(field.substring(6, 8)));

        // check the hour part (format .hhmm, working around checks on minutes)
        final TimeComponents tc;
        if (field.length() > 8) {
            // we convert from hours and minutes here in order to allow
            // the strange special case found in Eigen 6S4 file with date 20041226.0060
            tc = new TimeComponents(Integer.parseInt(field.substring(9, 11)) / 24.0 +
                                    Integer.parseInt(field.substring(11, 13)) / 3600.0);
        } else {
            tc = TimeComponents.H00;
        }

        return toDate(dc, tc);

    }

}
