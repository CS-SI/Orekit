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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.hipparchus.util.Precision;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
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

    /** Errors indicator. */
    private static final String ERRORS                  = "errors";

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

    /** Name of base coefficients. */
    private static final String BASE_NAMES              = "C/S";

    /** Pattern for delimiting regular expressions. */
    private static final Pattern SEPARATOR = Pattern.compile("\\s+");

    /** Pattern for supported formats. */
    private static final Pattern SUPPORTED_FORMAT = Pattern.compile("icgem(\\d+\\.\\d+)");

    /** Flag for Gravitational coefficient. */
    private static final int MU = 0x1;

    /** Flag for scaling radius. */
    private static final int AE = 0x2;

    /** Flag for degree/order. */
    private static final int LIMITS = 0x4;

    /** Flag for errors. */
    private static final int ERR = 0x8;

    /** Flag for coefficients. */
    private static final int COEFFS = 0x10;

    /** Indicator for normalized coefficients. */
    private boolean normalized;

    /** Reference dates. */
    private List<AbsoluteDate> referenceDates;

    /** Pulsations. */
    private List<Double>       pulsations;

    /** Time map of the harmonics. */
    private TimeSpanMap<Container> containers;

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
        containers     = null;
        referenceDates = new ArrayList<>();
        pulsations     = new ArrayList<>();

        // by default, the field is normalized with unknown tide system
        // (will be overridden later if non-default)
        normalized            = true;
        TideSystem tideSystem = TideSystem.UNKNOWN;
        Errors     errors     = Errors.NO;

        double    version        = 1.0;
        boolean   inHeader       = true;
        Flattener flattener      = null;
        int       flags          = 0;
        double[]  c0             = null;
        double[]  s0             = null;
        int       lineNumber     = 0;
        String    line           = null;
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
                    if (tab.length >= 2 && FORMAT.equals(tab[0])) {
                        final Matcher matcher = SUPPORTED_FORMAT.matcher(tab[1]);
                        if (matcher.matches()) {
                            version = Double.parseDouble(matcher.group(1));
                            if (version > MAX_FORMAT) {
                                parseError = true;
                            }
                        } else {
                            parseError = true;
                        }
                    } else if (tab.length >= 2 && PRODUCT_TYPE.equals(tab[0])) {
                        parseError = !GRAVITY_FIELD.equals(tab[1]);
                    } else if (tab.length >= 2 && tab[0].endsWith(GRAVITY_CONSTANT)) {
                        setMu(parseDouble(tab[1]));
                        flags |= MU;
                    } else if (tab.length >= 2 && REFERENCE_RADIUS.equals(tab[0])) {
                        setAe(parseDouble(tab[1]));
                        flags |= AE;
                    } else if (tab.length >= 2 && MAX_DEGREE.equals(tab[0])) {

                        final int degree = FastMath.min(getMaxParseDegree(), Integer.parseInt(tab[1]));
                        final int order  = FastMath.min(getMaxParseOrder(), degree);
                        flattener  = new Flattener(degree, order);
                        c0         = buildFlatArray(flattener, missingCoefficientsAllowed() ? 0.0 : Double.NaN);
                        s0         = buildFlatArray(flattener, missingCoefficientsAllowed() ? 0.0 : Double.NaN);
                        flags     |= LIMITS;

                    } else if (tab.length >= 2 && ERRORS.equals(tab[0])) {
                        try {
                            errors = Errors.valueOf(tab[1].toUpperCase(Locale.US));
                            flags |= ERR;
                        } catch (IllegalArgumentException iae) {
                            parseError = true;
                        }
                    } else if (tab.length >= 2 && TIDE_SYSTEM_INDICATOR.equals(tab[0])) {
                        if (ZERO_TIDE.equals(tab[1])) {
                            tideSystem = TideSystem.ZERO_TIDE;
                        } else if (TIDE_FREE.equals(tab[1])) {
                            tideSystem = TideSystem.TIDE_FREE;
                        } else if (TIDE_UNKNOWN.equals(tab[1])) {
                            tideSystem = TideSystem.UNKNOWN;
                        } else {
                            parseError = true;
                        }
                    } else if (tab.length >= 2 && NORMALIZATION_INDICATOR.equals(tab[0])) {
                        if (NORMALIZED.equals(tab[1])) {
                            normalized = true;
                        } else if (UNNORMALIZED.equals(tab[1])) {
                            normalized = false;
                        } else {
                            parseError = true;
                        }
                    } else if (tab.length >= 1 && END_OF_HEADER.equals(tab[0])) {
                        inHeader   = false;
                    }
                    if (parseError) {
                        throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                  lineNumber, name, line);
                    }
                } else if (dataKeyKnown(tab) && tab.length > 2) {

                    final int n = Integer.parseInt(tab[1]);
                    final int m = Integer.parseInt(tab[2]);
                    flags |= COEFFS;
                    if (!flattener.withinRange(n, m)) {
                        // just ignore coefficients we don't need
                        continue;
                    }

                    if (tab.length > 4 && GFC.equals(tab[0])) {
                        // fixed coefficient

                        parseCoefficient(tab[3], flattener, c0, n, m, "C", name);
                        parseCoefficient(tab[4], flattener, s0, n, m, "S", name);

                    } else if (version < 2.0 && tab.length > 5 + errors.fields && GFCT.equals(tab[0])) {
                        // base of linear coefficient with infinite time range

                        if (containers == null) {
                            // prepare the single container (it will be populated when next lines are parsed)
                            containers = new TimeSpanMap<>(new Container(flattener));
                        }

                        // set the constant coefficients to 0 as they will be managed by the piecewise model
                        final int globalIndex = flattener.index(n, m);
                        c0[globalIndex]       = 0.0;
                        s0[globalIndex]       = 0.0;

                        // store the single reference date valid for the whole field
                        final AbsoluteDate lineDate = parseDate(tab[5 + errors.fields]);
                        final int referenceIndex    = referenceDateIndex(referenceDates, lineDate);
                        if (referenceIndex != 0) {
                            // we already know the reference date, check this lines does not define a new one
                            throw new OrekitException(OrekitMessages.SEVERAL_REFERENCE_DATES_IN_GRAVITY_FIELD,
                                                      referenceDates.get(0), lineDate, name,
                                                      lineDate.durationFrom(referenceDates.get(0)));
                        }

                        final Container single = containers.getFirstSpan().getData();
                        final int       index  = single.flattener.index(n, m);
                        if (single.components[index] != null) {
                            throw new OrekitException(OrekitMessages.DUPLICATED_GRAVITY_FIELD_COEFFICIENT_IN_FILE,
                                                      BASE_NAMES, n, m, name);
                        }
                        single.components[index] = new TimeDependentHarmonic(referenceIndex, parseDouble(tab[3]), parseDouble(tab[4]));


                    } else if (version >= 2.0 && tab.length > 6 + errors.fields && GFCT.equals(tab[0])) {
                        // base of linear coefficient with limited time range

                        if (containers == null) {
                            // prepare empty map to hold containers as they are parsed
                            containers = new TimeSpanMap<>(null);
                        }

                        // set the constant coefficients to 0 as they will be managed by the piecewise model
                        final int globalIndex = flattener.index(n, m);
                        c0[globalIndex]       = 0.0;
                        s0[globalIndex]       = 0.0;

                        final AbsoluteDate t0 = parseDate(tab[5 + errors.fields]);
                        final AbsoluteDate t1 = parseDate(tab[6 + errors.fields]);

                        // get the containers active for the specified time range
                        final List<TimeSpanMap.Span<Container>> active = getActive(flattener, t0, t1);
                        for (final TimeSpanMap.Span<Container> span : active) {
                            final Container             container = span.getData();
                            final int                   index     = container.flattener.index(n, m);
                            if (container.components[index] != null) {
                                throw new OrekitException(OrekitMessages.DUPLICATED_GRAVITY_FIELD_COEFFICIENT_IN_FILE,
                                                          BASE_NAMES, n, m, name);
                            }
                            container.components[index] = new TimeDependentHarmonic(referenceDateIndex(referenceDates, t0),
                                                                                    parseDouble(tab[3]), parseDouble(tab[4]));
                        }

                    } else if (version < 2.0 && tab.length > 4 && (DOT.equals(tab[0]) || TRND.equals(tab[0]))) {
                        // slope of linear coefficient with infinite range

                        // store the secular trend coefficients
                        final Container single = containers.getFirstSpan().getData();
                        final TimeDependentHarmonic harmonic = single.components[single.flattener.index(n, m)];
                        if (harmonic == null) {
                            parseError = true;
                        } else {
                            harmonic.setTrend(parseDouble(tab[3]) / Constants.JULIAN_YEAR,
                                              parseDouble(tab[4]) / Constants.JULIAN_YEAR);
                        }

                    } else if (version >= 2.0 && tab.length > 6 + errors.fields && TRND.equals(tab[0])) {
                        // slope of linear coefficient with limited range

                        final AbsoluteDate t0 = parseDate(tab[5 + errors.fields]);
                        final AbsoluteDate t1 = parseDate(tab[6 + errors.fields]);

                        // get the containers active for the specified time range
                        final List<TimeSpanMap.Span<Container>> active = getActive(flattener, t0, t1);
                        for (final TimeSpanMap.Span<Container> span : active) {
                            final Container             container = span.getData();
                            final int                   index     = container.flattener.index(n, m);
                            if (container.components[index] == null) {
                                parseError = true;
                                break;
                            } else {
                                container.components[index].setTrend(parseDouble(tab[3]) / Constants.JULIAN_YEAR,
                                                                     parseDouble(tab[4]) / Constants.JULIAN_YEAR);
                            }
                        }

                    } else if (version < 2.0 && tab.length > 5 + errors.fields && (ASIN.equals(tab[0]) || ACOS.equals(tab[0]))) {
                        // periodic coefficient with infinite range

                        final double period = parseDouble(tab[5 + errors.fields]) * Constants.JULIAN_YEAR;
                        final int    pIndex = pulsationIndex(pulsations, MathUtils.TWO_PI / period);

                        // store the periodic effects coefficients
                        final Container single = containers.getFirstSpan().getData();
                        final TimeDependentHarmonic harmonic = single.components[single.flattener.index(n, m)];
                        if (harmonic == null) {
                            parseError = true;
                        } else {
                            if (ACOS.equals(tab[0])) {
                                harmonic.addCosine(-1, pIndex, parseDouble(tab[3]), parseDouble(tab[4]));
                            } else {
                                harmonic.addSine(-1, pIndex, parseDouble(tab[3]), parseDouble(tab[4]));
                            }
                        }

                    } else if (version >= 2.0 && tab.length > 7 + errors.fields && (ASIN.equals(tab[0]) || ACOS.equals(tab[0]))) {
                        // periodic coefficient with limited range

                        final AbsoluteDate t0      = parseDate(tab[5 + errors.fields]);
                        final AbsoluteDate t1      = parseDate(tab[6 + errors.fields]);
                        final int          tIndex  = referenceDateIndex(referenceDates, t0);
                        final double       period  = parseDouble(tab[7 + errors.fields]) * Constants.JULIAN_YEAR;
                        final int          pIndex  = pulsationIndex(pulsations, MathUtils.TWO_PI / period);

                        // get the containers active for the specified time range
                        final List<TimeSpanMap.Span<Container>> active = getActive(flattener, t0, t1);
                        for (final TimeSpanMap.Span<Container> span : active) {
                            final Container             container = span.getData();
                            final int                   index     = container.flattener.index(n, m);
                            if (container.components[index] == null) {
                                parseError = true;
                                break;
                            } else {
                                if (ASIN.equals(tab[0])) {
                                    container.components[index].addSine(tIndex, pIndex,
                                                                        parseDouble(tab[3]), parseDouble(tab[4]));
                                } else {
                                    container.components[index].addCosine(tIndex, pIndex,
                                                                          parseDouble(tab[3]), parseDouble(tab[4]));
                                }
                            }
                        }

                    } else {
                        parseError = true;
                    }

                } else if (dataKeyKnown(tab)) {
                    // this was an expected data key, but the line is truncated
                    parseError = true;
                }

                if (parseError) {
                    throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                              lineNumber, name, line);
                }

            }

        } catch (NumberFormatException nfe) {
            throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                      lineNumber, name, line);
        }

        if (flags != (MU | AE | LIMITS | ERR | COEFFS)) {
            String loaderName = getClass().getName();
            loaderName = loaderName.substring(loaderName.lastIndexOf('.') + 1);
            throw new OrekitException(OrekitMessages.UNEXPECTED_FILE_FORMAT_ERROR_FOR_LOADER,
                                      name, loaderName);
        }

        if (missingCoefficientsAllowed()) {
            // ensure at least the (0, 0) element is properly set
            if (Precision.equals(c0[flattener.index(0, 0)], 0.0, 0)) {
                c0[flattener.index(0, 0)] = 1.0;
            }
        }

        setRawCoefficients(normalized, flattener, c0, s0, name);
        setTideSystem(tideSystem);
        setReadComplete(true);

    }

    /** Check if a line starts with a known data key.
     * @param tab line fields
     * @return true if line starts with a known data key
     * @since 11.1
     */
    private boolean dataKeyKnown(final String[] tab) {
        return tab.length > 0 &&
               (GFC.equals(tab[0])  || GFCT.equals(tab[0]) ||
                DOT.equals(tab[0])  || TRND.equals(tab[0]) ||
                ASIN.equals(tab[0]) || ACOS.equals(tab[0]));
    }

    /** Get the spans with containers active over a time range.
     * @param flattener converter from triangular form to flat form
     * @param t0 start of time span
     * @param t1 end of time span
     * @return span active from {@code t0} to {@code t1}
     * @since 11.1
     */
    private List<TimeSpanMap.Span<Container>> getActive(final Flattener flattener,
                                                        final AbsoluteDate t0, final AbsoluteDate t1) {

        final List<TimeSpanMap.Span<Container>> active = new ArrayList<>();

        TimeSpanMap.Span<Container> span = containers.getSpan(t0);
        if (span.getStart().isBefore(t0)) {
            if (span.getEnd().isAfterOrEqualTo(t1)) {
                if (span.getData() == null) {
                    // the specified time range lies on an empty range
                    span = containers.addValidBetween(new Container(flattener), t0, t1);
                } else {
                    // the specified time range splits an existing container in three parts
                    containers.addValidAfter(copyContainer(span.getData(), flattener), t1, false);
                    span = containers.addValidAfter(copyContainer(span.getData(), flattener), t0, false);
                }
            } else {
                span = containers.addValidAfter(copyContainer(span.getData(), flattener), t0, false);
            }
        }

        while (span.getStart().isBefore(t1)) {
            if (span.getEnd().isAfter(t1)) {
                // this span extends past t1, we must split it
                span = containers.addValidBefore(copyContainer(span.getData(), flattener), t1, false);
            }
            active.add(span);
            span = span.next();
        }

        return active;

    }

    /** Copy a container.
     * @param original time span to copy (may be null)
     * @param flattener converter between triangular and flat forms
     * @return fresh copy
     */
    private Container copyContainer(final Container original, final Flattener flattener) {
        return original == null ?
               new Container(flattener) :
               original.resize(flattener.getDegree(), flattener.getOrder());
    }

    /** Get the index of a reference date, adding it if needed.
     * @param known known reference dates
     * @param referenceDate reference date to select
     * @return index of the reference date in the {@code known} list
     * @since 11.1
     */
    private int referenceDateIndex(final List<AbsoluteDate> known, final AbsoluteDate referenceDate) {
        for (int i = 0; i < known.size(); ++i) {
            if (known.get(i).equals(referenceDate)) {
                return i;
            }
        }
        known.add(referenceDate);
        return known.size() - 1;
    }

    /** Get the index of a pulsation, adding it if needed.
     * @param known known pulsations
     * @param pulsation pulsation to select
     * @return index of the pulsation in the {@code known} list
     * @since 11.1
     */
    private int pulsationIndex(final List<Double> known, final double pulsation) {
        for (int i = 0; i < known.size(); ++i) {
            if (Precision.equals(known.get(i), pulsation, 1)) {
                return i;
            }
        }
        known.add(pulsation);
        return known.size() - 1;
    }

    /** {@inheritDoc} */
    public RawSphericalHarmonicsProvider getProvider(final boolean wantNormalized,
                                                     final int degree, final int order) {

        // get the constant part of the field
        final ConstantSphericalHarmonics constant = getBaseProvider(wantNormalized, degree, order);
        if (containers == null) {
            // there are no time-dependent parts in the field
            return constant;
        }

        // create the shared parts of the model
        final AbsoluteDate[] dates = new AbsoluteDate[referenceDates.size()];
        for (int i = 0; i < dates.length; ++i) {
            dates[i] = referenceDates.get(i);
        }
        final double[] puls = new double[pulsations.size()];
        for (int i = 0; i < puls.length; ++i) {
            puls[i] = pulsations.get(i);
        }

        // convert the mutable containers to piecewise parts with desired normalization
        final TimeSpanMap<PiecewisePart> pieces = new TimeSpanMap<>(null);
        for (TimeSpanMap.Span<Container> span = containers.getFirstSpan(); span != null; span = span.next()) {
            if (span.getData() != null) {
                final Flattener spanFlattener = span.getData().flattener;
                final Flattener rescaledFlattener = new Flattener(FastMath.min(degree, spanFlattener.getDegree()),
                                                                  FastMath.min(order, spanFlattener.getOrder()));
                pieces.addValidBetween(new PiecewisePart(rescaledFlattener,
                                                         rescale(wantNormalized, rescaledFlattener, span.getData().flattener,
                                                                 span.getData().components)),
                                       span.getStart(), span.getEnd());
            }
        }

        return new PiecewiseSphericalHarmonics(constant, dates, puls, pieces);

    }

    /** Parse a reference date.
     * <p>
     * The reference dates have either the format yyyymmdd (for 2011 format)
     * or the format yyyymmdd.xxxx (for format version 2.0).
     * </p>
     * <p>
     * The documentation for 2011 format does not specify the time scales,
     * but on of the example reads "The reference time t0 is: t0 = 2005.0 y"
     * when the corresponding field in the data section reads "20050101",
     * so we assume the dates are consistent with astronomical conventions
     * and 2005.0 is 2005-01-01T12:00:00 (i.e. noon).
     * </p>
     * <p>
     * The 2.0 format is not described anywhere (at least I did not find any
     * references), but the .xxxx fractional part seems to be hours and minutes chosen
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
            tc = new TimeComponents(Integer.parseInt(field.substring(9, 11)) * 3600 +
                                    Integer.parseInt(field.substring(11, 13)) * 60);
        } else {
            // assume astronomical convention for hour
            tc = TimeComponents.H12;
        }

        return toDate(dc, tc);

    }

    /** Temporary container for reading coefficients.
     * @since 11.1
     */
    private static class Container {

        /** Converter between (degree, order) indices and flatten array. */
        private final Flattener flattener;

        /** Components of the spherical harmonics. */
        private final TimeDependentHarmonic[] components;

        /** Build a container with given degree and order.
         * @param flattener converter between (degree, order) indices and flatten array
         */
        Container(final Flattener flattener) {
            this.flattener  = flattener;
            this.components = new TimeDependentHarmonic[flattener.arraySize()];
        }

        /** Build a resized container.
         * @param degree degree of the container
         * @param order order of the container
         * @return resized container
         */
        Container resize(final int degree, final int order) {

            // create new instance
            final Container resized = new Container(new Flattener(degree, order));

            // copy harmonics
            for (int n = 0; n <= degree; ++n) {
                for (int m = 0; m <= FastMath.min(n, order); ++m) {
                    resized.components[resized.flattener.index(n, m)] = components[flattener.index(n, m)];
                }
            }

            return resized;

        }

    }

    /** Errors present in the gravity field.
     * @since 11.1
     */
    private enum Errors {

        /** No errors. */
        NO(0),

        /** Calibrated errors. */
        CALIBRATED(2),

        /** Formal errors. */
        FORMAL(2),

        /** Both calibrated and formal. */
        CALIBRATED_AND_FORMAL(4);

        /** Number of error fields in data lines. */
        private final int fields;

        /** Simple constructor.
         * @param fields umber of error fields in data lines
         */
        Errors(final int fields) {
            this.fields = fields;
        }

    }

}
