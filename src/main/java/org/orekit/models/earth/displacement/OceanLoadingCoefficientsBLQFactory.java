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
package org.orekit.models.earth.displacement;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.hipparchus.util.FastMath;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.data.AbstractSelfFeedingLoader;
import org.orekit.data.DataContext;
import org.orekit.data.DataLoader;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

/**
 * Factory for ocean loading coefficients, using Onsala Space Observatory files in BLQ format.
 * <p>
 * Files in BLQ format can be generated using the form at the
 * <a href="http://holt.oso.chalmers.se/loading/">Bos-Scherneck web site</a>,
 * selecting BLQ as the output format.
 * </p>
 * <p>
 * The sites names are extracted from the file content, not the file name, because the
 * file can contain more than one station. As we expect existing files may have been
 * stripped from headers and footers, we do not attempt to parse them. We only parse
 * the series of 7 lines blocks starting with the lines with the station names and their
 * coordinates and the 6 data lines that follows. Several such blocks may appear in the
 * file. Copy-pasting the entire mail received from OSO after completing the web site
 * form works, as intermediate lines between the 7 lines blocks are simply ignored.
 * </p>
 * @see OceanLoadingCoefficients
 * @see OceanLoading
 * @since 9.1
 * @author Luc Maisonobe
 */
public class OceanLoadingCoefficientsBLQFactory extends AbstractSelfFeedingLoader {

    /** Default supported files name pattern for Onsala Space Observatory files in BLQ format. */
    public static final String DEFAULT_BLQ_SUPPORTED_NAMES = "^.+\\.blq$";

    /** Pattern for fields with real type. */
    private static final String  REAL_TYPE_PATTERN = "[-+]?(?:(?:\\p{Digit}+(?:\\.\\p{Digit}*)?)|(?:\\.\\p{Digit}+))(?:[eE][-+]?\\p{Digit}+)?";

    /** Pattern for extracted real fields. */
    private static final String  REAL_FIELD_PATTERN = "\\p{Space}*(" + REAL_TYPE_PATTERN + ")";

    /** Pattern for end of line. */
    private static final String  END_OF_LINE_PATTERN = "\\p{Space}*$";

    /** Pattern for site name and coordinates lines. */
    private static final String  SITE_LINE_PATTERN = "^\\$\\$ *([^,]*),\\p{Space}*(?:RADI TANG)?\\p{Space}*lon/lat:" +
                                                     REAL_FIELD_PATTERN +
                                                     REAL_FIELD_PATTERN +
                                                     REAL_FIELD_PATTERN +
                                                     END_OF_LINE_PATTERN;

    /** Pattern for coefficients lines. */
    private static final String  DATA_LINE_PATTERN = "^" +
                                                     REAL_FIELD_PATTERN + // M₂ tide
                                                     REAL_FIELD_PATTERN + // S₂ tide
                                                     REAL_FIELD_PATTERN + // N₂ tide
                                                     REAL_FIELD_PATTERN + // K₂ tide
                                                     REAL_FIELD_PATTERN + // K₁ tide
                                                     REAL_FIELD_PATTERN + // O₁ tide
                                                     REAL_FIELD_PATTERN + // P₁ tide
                                                     REAL_FIELD_PATTERN + // Q₁ tide
                                                     REAL_FIELD_PATTERN + // Mf tide
                                                     REAL_FIELD_PATTERN + // Mm tide
                                                     REAL_FIELD_PATTERN + // Ssa tide
                                                     END_OF_LINE_PATTERN;

    /** Pattern for site name and coordinates lines. */
    private static final Pattern SITE_PATTERN = Pattern.compile(SITE_LINE_PATTERN);

    /** Pattern for coefficients lines. */
    private static final Pattern DATA_PATTERN = Pattern.compile(DATA_LINE_PATTERN);

    /** Main tides. */
    private static final Tide[][] TIDES = {
        {
            Tide.SSA, Tide.MM, Tide.MF
        }, {
            Tide.Q1,  Tide.O1, Tide.P1, Tide.K1
        }, {
            Tide.N2,  Tide.M2, Tide.S2, Tide.K2
        }
    };

    /** Species index for each column. */
    private static final int[] I = {
        2, 2, 2, 2, 1, 1, 1, 1, 0, 0, 0
    };

    /** Tides index for each column. */
    private static final int[] J = {
        1, 2, 0, 3, 3, 1, 2, 0, 2, 1, 0
    };

    /** Parsed coefficients. */
    private final List<OceanLoadingCoefficients> coefficients;

    /** Simple constructor. This constructor uses the {@link DataContext#getDefault()
     * default data context}.
     * <p>
     * Files in BLQ format can be generated using the form at the
     * <a href="http://holt.oso.chalmers.se/loading/">Bos-Scherneck web site</a>,
     * selecting BLQ as the output format.
     * </p>
     * @param supportedNames regular expression for supported files names
     * @see #DEFAULT_BLQ_SUPPORTED_NAMES
     * @see #OceanLoadingCoefficientsBLQFactory(String, DataProvidersManager)
     */
    @DefaultDataContext
    public OceanLoadingCoefficientsBLQFactory(final String supportedNames) {
        this(supportedNames, DataContext.getDefault().getDataProvidersManager());
    }

    /**
     * This constructor allows specification of the source of the BLQ auxiliary data
     * files.
     *
     * <p>
     * Files in BLQ format can be generated using the form at the
     * <a href="http://holt.oso.chalmers.se/loading/">Bos-Scherneck web site</a>,
     * selecting BLQ as the output format.
     * </p>
     * @param supportedNames regular expression for supported files names
     * @param dataProvidersManager provides access to auxiliary data files.
     * @see #DEFAULT_BLQ_SUPPORTED_NAMES
     * @since 10.1
     */
    public OceanLoadingCoefficientsBLQFactory(
            final String supportedNames,
            final DataProvidersManager dataProvidersManager) {
        super(supportedNames, dataProvidersManager);

        this.coefficients   = new ArrayList<>();

    }

    /** Lazy loading of coefficients.
     */
    private void loadsIfNeeded() {
        if (coefficients.isEmpty()) {
            feed(new BLQParser());
        }
    }

    /** Get the list of sites for which we have found coefficients, in lexicographic order ignoring case.
     * @return list of sites for which we have found coefficients, in lexicographic order ignoring case
     */
    public List<String> getSites() {

        loadsIfNeeded();

        // extract sites names from the map
        final List<String> sites = coefficients.stream()
                .map(OceanLoadingCoefficients::getSiteName)
                // sort to ensure we have a reproducible order
                .sorted(String::compareToIgnoreCase)
                .collect(Collectors.toList());

        return sites;

    }

    /** Get the coefficients for a given site.
     * @param site site name (as it appears in the Onsala Space Observatory files in BLQ format),
     * ignoring case
     * @return coefficients for the site
     */
    public OceanLoadingCoefficients getCoefficients(final String site) {

        loadsIfNeeded();

        final Optional<OceanLoadingCoefficients> optional =
                        coefficients.stream().filter(c -> c.getSiteName().equalsIgnoreCase(site)).findFirst();
        if (!optional.isPresent()) {
            throw new OrekitException(OrekitMessages.STATION_NOT_FOUND,
                                      site,
                                      String.join(", ", getSites()));
        }

        return optional.get();

    }

    /** Local parser for the Onsala Space Observatory BLQ files.
     * <p>
     * when completing the web site form, the email received as the following form:
     * </p>
     * <pre>{@literal
     * $$ Ocean loading displacement
     * $$
     * $$ Calculated on holt using olfg/olmpp of H.-G. Scherneck
     * $$
     * $$ COLUMN ORDER:  M2  S2  N2  K2  K1  O1  P1  Q1  MF  MM SSA
     * $$
     * $$ ROW ORDER:
     * $$ AMPLITUDES (m)
     * $$   RADIAL
     * $$   TANGENTL    EW
     * $$   TANGENTL    NS
     * $$ PHASES (degrees)
     * $$   RADIAL
     * $$   TANGENTL    EW
     * $$   TANGENTL    NS
     * $$
     * $$ Displacement is defined positive in upwards, South and West direction.
     * $$ The phase lag is relative to Greenwich and lags positive. The
     * $$ Gutenberg-Bullen Greens function is used. In the ocean tide model the
     * $$ deficit of tidal water mass has been corrected by subtracting a uniform
     * $$ layer of water with a certain phase lag globally.
     * $$
     * $$ Complete <model name> : No interpolation of ocean model was necessary
     * $$ <model name>_PP       : Ocean model has been interpolated near the station
     * $$                         (PP = Post-Processing)
     * $$
     * $$ Ocean tide model: CSR4.0, long-period tides from FES99
     * $$
     * $$ END HEADER
     * $$
     *   Goldstone
     * $$ Complete CSR4.0_f
     * $$ Computed by OLFG, H.-G. Scherneck, Onsala Space Observatory 2017-Sep-28
     * $$ Goldstone,                 RADI TANG  lon/lat:  243.1105   35.4259    0.000
     *   .00130 .00155 .00064 .00052 .01031 .00661 .00339 .00119 .00005 .00002 .00003
     *   .00136 .00020 .00024 .00004 .00322 .00202 .00106 .00036 .00007 .00003 .00001
     *   .00372 .00165 .00082 .00045 .00175 .00113 .00057 .00022 .00004 .00002 .00003
     *     -2.7 -106.3  -62.6 -106.8   41.6   27.3   40.4   24.0 -119.1 -123.2 -169.7
     *   -145.3  -88.4  178.5  -66.3 -130.5 -145.3 -131.7 -148.7  124.3  139.6   23.3
     *     90.7  111.1   74.1  111.3  176.9  165.3  175.8  164.0   48.9   25.3    4.5
     * $$
     *   ONSALA
     * $$ CSR4.0_f_PP ID: 2017-09-28 15:01:14
     * $$ Computed by OLMPP by H G Scherneck, Onsala Space Observatory, 2017
     * $$ Onsala,                    RADI TANG  lon/lat:   11.9264   57.3958    0.000
     *   .00344 .00121 .00078 .00031 .00189 .00116 .00064 .00004 .00090 .00048 .00041
     *   .00143 .00035 .00035 .00008 .00053 .00051 .00018 .00009 .00013 .00006 .00007
     *   .00086 .00023 .00023 .00006 .00029 .00025 .00010 .00008 .00003 .00001 .00000
     *    -64.6  -50.3  -95.0  -53.1  -58.8 -152.4  -65.5 -133.8    9.8    5.8    2.1
     *     85.4  115.2   56.7  114.7   99.5   15.9   94.2  -10.0 -166.3 -169.8 -177.7
     *    110.7  147.1   93.9  148.6   49.4  -56.5   34.8 -169.9  -35.3   -3.7   10.1
     * $$ END TABLE
     * Errors:
     * Warnings:
     * }</pre>
     * <p>
     * We only parse blocks 7 lines blocks starting with the lines with the station names
     * and their coordinates and the 6 data lines that follows. Several such blocks may
     * appear in the file.
     * </p>
     */
    private class BLQParser implements DataLoader {

        /** Simple constructor.
         */
        BLQParser() {
            // empty constructor
        }

        /** {@inheritDoc} */
        @Override
        public boolean stillAcceptsData() {
            // as data for different stations may be in different files
            // we always accept new data, even if we have already parsed
            // some files
            return true;
        }

        /** {@inheritDoc} */
        @Override
        public void loadData(final InputStream input, final String name)
            throws IOException, OrekitException {

            // temporary holders for parsed data
            String         siteName     = null;
            GeodeticPoint  siteLocation = null;
            final double[][][] data     = new double[6][3][];
            for (int i = 0; i < data.length; ++i) {
                data[i][0] = new double[3];
                data[i][1] = new double[4];
                data[i][2] = new double[4];
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {

                int     lineNumber = 0;
                int     dataLine   = -1;
                for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                    ++lineNumber;

                    if (dataLine < 0) {
                        // we are looking for a site line
                        final Matcher matcher = SITE_PATTERN.matcher(line);
                        if (matcher.matches()) {
                            // the current line is a site description line
                            siteName = matcher.group(1);
                            siteLocation = new GeodeticPoint(FastMath.toRadians(Double.parseDouble(matcher.group(3))),
                                                             FastMath.toRadians(Double.parseDouble(matcher.group(2))),
                                                             Double.parseDouble(matcher.group(4)));
                            // next line must be line 0 of the data
                            dataLine = 0;
                        }
                    } else {
                        // we are looking for a data line
                        final Matcher matcher = DATA_PATTERN.matcher(line);
                        if (!matcher.matches()) {
                            throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                      lineNumber, name, line);
                        }
                        for (int k = 0; k < I.length; ++k) {
                            if (dataLine < 3) {
                                // amplitude data
                                data[dataLine][I[k]][J[k]] = Double.parseDouble(matcher.group(k + 1));
                            } else {
                                // phase data (reversed to be negative for lags)
                                data[dataLine][I[k]][J[k]] = -FastMath.toRadians(Double.parseDouble(matcher.group(k + 1)));
                            }
                        }
                        if (dataLine < data.length - 1) {
                            // we need more data
                            ++dataLine;
                        } else {
                            // it was the last data line
                            coefficients.add(new OceanLoadingCoefficients(siteName, siteLocation, TIDES,
                                                                          data[0], data[3],
                                                                          data[1], data[4],
                                                                          data[2], data[5]));
                            dataLine = -1;
                        }
                    }
                }

                if (dataLine >= 0) {
                    // we were looking for a line that did not appear
                    throw new OrekitException(OrekitMessages.UNEXPECTED_END_OF_FILE_AFTER_LINE,
                                              name, lineNumber);
                }

            }
        }

    }

}
