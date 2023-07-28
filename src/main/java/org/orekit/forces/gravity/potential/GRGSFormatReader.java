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
import org.orekit.time.TimeScale;
import org.orekit.utils.Constants;

/** Reader for the GRGS gravity field format.
 *
 * <p> This format was used to describe various gravity fields at GRGS (Toulouse).
 *
 * <p> The proper way to use this class is to call the {@link GravityFieldFactory}
 *  which will determine which reader to use with the selected gravity field file.</p>
 *
 * @see GravityFields
 * @author Luc Maisonobe
 */
public class GRGSFormatReader extends PotentialCoefficientsReader {

    /** Patterns for lines (the last pattern is repeated for all data lines). */
    private static final Pattern[] LINES;

    /** Flag for Earth data. */
    private static final int EARTH = 0x1;

    /** Flag for degree/order. */
    private static final int LIMITS = 0x2;

    /** Flag for coefficients. */
    private static final int COEFFS = 0x4;

    /** Reference date. */
    private AbsoluteDate referenceDate;

    /** Converter from triangular to flat form. */
    private Flattener dotFlattener;

    /** Secular drift of the cosine coefficients. */
    private double[] cDot;

    /** Secular drift of the sine coefficients. */
    private double[] sDot;

    static {

        // sub-patterns
        final String real = "[-+]?\\d?\\.\\d+[eEdD][-+]\\d\\d";
        final String sep = ")\\s*(";

        // regular expression for header lines
        final String[] header = {
            "^\\s*FIELD - .*$",
            "^\\s+AE\\s+1/F\\s+GM\\s+OMEGA\\s*$",
            "^\\s*(" + real + sep + real + sep + real + sep + real + ")\\s*$",
            "^\\s*REFERENCE\\s+DATE\\s+:\\s+(\\d+)\\.0+\\s*$",
            "^\\s*MAXIMAL\\s+DEGREE\\s+:\\s+(\\d+)\\s.*$",
            "^\\s*L\\s+M\\s+DOT\\s+CBAR\\s+SBAR\\s+SIGMA C\\s+SIGMA S(\\s+LIB)?\\s*$"
        };

        // regular expression for data lines
        final String data = "^([ 0-9]{3})([ 0-9]{3})(   |DOT)\\s*(" +
                            real + sep + real + sep + real + sep + real +
                            ")(\\s+[0-9]+)?\\s*$";

        // compile the regular expressions
        LINES = new Pattern[header.length + 1];
        for (int i = 0; i < header.length; ++i) {
            LINES[i] = Pattern.compile(header[i]);
        }
        LINES[LINES.length - 1] = Pattern.compile(data);

    }

    /** Simple constructor.
     *
     * <p>This constructor uses the {@link DataContext#getDefault() default data context}.
     *
     * @param supportedNames regular expression for supported files names
     * @param missingCoefficientsAllowed if true, allows missing coefficients in the input data
     * @see #GRGSFormatReader(String, boolean, TimeScale)
     */
    @DefaultDataContext
    public GRGSFormatReader(final String supportedNames, final boolean missingCoefficientsAllowed) {
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
    public GRGSFormatReader(final String supportedNames,
                            final boolean missingCoefficientsAllowed,
                            final TimeScale timeScale) {
        super(supportedNames, missingCoefficientsAllowed, timeScale);
        reset();
    }

    /** {@inheritDoc} */
    public void loadData(final InputStream input, final String name)
        throws IOException, ParseException, OrekitException {

        // reset the indicator before loading any data
        reset();

        //        FIELD - GRIM5, VERSION : C1, november 1999
        //        AE                  1/F                 GM                 OMEGA
        //0.63781364600000E+070.29825765000000E+030.39860044150000E+150.72921150000000E-04
        //REFERENCE DATE : 1997.00
        //MAXIMAL DEGREE : 120     Sigmas calibration factor : .5000E+01 (applied)
        //L  M DOT         CBAR                SBAR             SIGMA C      SIGMA S
        // 2  0DOT 0.13637590952454E-10 0.00000000000000E+00  .143968E-11  .000000E+00
        // 3  0DOT 0.28175700027753E-11 0.00000000000000E+00  .496704E-12  .000000E+00
        // 4  0DOT 0.12249148508277E-10 0.00000000000000E+00  .129977E-11  .000000E+00
        // 0  0     .99999999988600E+00  .00000000000000E+00  .153900E-09  .000000E+00
        // 2  0   -0.48416511550920E-03 0.00000000000000E+00  .204904E-10  .000000E+00

        Flattener flattener  = null;
        int       dotDegree  = -1;
        int       dotOrder   = -1;
        int       flags      = 0;
        int       lineNumber = 0;
        double[]  c0         = null;
        double[]  s0         = null;
        double[]  c1         = null;
        double[]  s1         = null;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            for (String line = r.readLine(); line != null; line = r.readLine()) {

                ++lineNumber;

                // match current header or data line
                final Matcher matcher = LINES[FastMath.min(LINES.length, lineNumber) - 1].matcher(line);
                if (!matcher.matches()) {
                    throw new OrekitParseException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                   lineNumber, name, line);
                }

                if (lineNumber == 3) {
                    // header line defining ae, 1/f, GM and Omega
                    setAe(parseDouble(matcher.group(1)));
                    setMu(parseDouble(matcher.group(3)));
                    flags |= EARTH;
                } else if (lineNumber == 4) {
                    // header line containing the reference date
                    referenceDate  = toDate(
                            new DateComponents(Integer.parseInt(matcher.group(1)), 1, 1));
                } else if (lineNumber == 5) {
                    // header line defining max degree
                    final int degree = FastMath.min(getMaxParseDegree(), Integer.parseInt(matcher.group(1)));
                    final int order  = FastMath.min(getMaxParseOrder(), degree);
                    flattener = new Flattener(degree, order);
                    c0 = buildFlatArray(flattener, missingCoefficientsAllowed() ? 0.0 : Double.NaN);
                    s0 = buildFlatArray(flattener, missingCoefficientsAllowed() ? 0.0 : Double.NaN);
                    c1 = buildFlatArray(flattener, 0.0);
                    s1 = buildFlatArray(flattener, 0.0);
                    flags |= LIMITS;
                } else if (lineNumber > 6) {
                    // data line
                    final int i = Integer.parseInt(matcher.group(1).trim());
                    final int j = Integer.parseInt(matcher.group(2).trim());
                    if (flattener.withinRange(i, j)) {
                        if ("DOT".equals(matcher.group(3).trim())) {

                            // store the secular drift coefficients
                            parseCoefficient(matcher.group(4), flattener, c1, i, j, "Cdot", name);
                            parseCoefficient(matcher.group(5), flattener, s1, i, j, "Sdot", name);
                            dotDegree = FastMath.max(dotDegree, i);
                            dotOrder  = FastMath.max(dotOrder,  j);

                        } else {

                            // store the constant coefficients
                            parseCoefficient(matcher.group(4), flattener, c0, i, j, "C", name);
                            parseCoefficient(matcher.group(5), flattener, s0, i, j, "S", name);

                        }
                    }
                    flags |= COEFFS;
                }

            }
        }

        if (flags != (EARTH | LIMITS | COEFFS)) {
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

        // resize secular drift arrays
        if (dotDegree >= 0) {
            dotFlattener = new Flattener(dotDegree, dotOrder);
            cDot         = new double[dotFlattener.arraySize()];
            sDot         = new double[dotFlattener.arraySize()];
            for (int n = 0; n <= dotDegree; ++n) {
                for (int m = 0; m <= FastMath.min(n, dotOrder); ++m) {
                    cDot[dotFlattener.index(n, m)] = c1[flattener.index(n, m)];
                    sDot[dotFlattener.index(n, m)] = s1[flattener.index(n, m)];
                }
            }
        }

        setRawCoefficients(true, flattener, c0, s0, name);
        setTideSystem(TideSystem.UNKNOWN);
        setReadComplete(true);

    }

    /** Reset instance before read.
     * @since 11.1
     */
    private void reset() {
        setReadComplete(false);
        referenceDate = null;
        dotFlattener  = null;
        cDot          = null;
        sDot          = null;
    }

    /** {@inheritDoc}
     * <p>
     * GRGS fields may include time-dependent parts which are taken into account
     * in the returned provider.
     * </p>
     */
    public RawSphericalHarmonicsProvider getProvider(final boolean wantNormalized,
                                                     final int degree, final int order) {

        // get the constant part
        RawSphericalHarmonicsProvider provider = getBaseProvider(wantNormalized, degree, order);

        if (dotFlattener != null) {

            // add the secular trend layer
            final double scale = 1.0 / Constants.JULIAN_YEAR;
            final Flattener rescaledFlattener = new Flattener(FastMath.min(degree, dotFlattener.getDegree()),
                                                              FastMath.min(order, dotFlattener.getOrder()));
            provider = new SecularTrendSphericalHarmonics(provider, referenceDate, rescaledFlattener,
                                                          rescale(scale, wantNormalized, rescaledFlattener, dotFlattener, cDot),
                                                          rescale(scale, wantNormalized, rescaledFlattener, dotFlattener, sDot));

        }

        return provider;

    }

}
