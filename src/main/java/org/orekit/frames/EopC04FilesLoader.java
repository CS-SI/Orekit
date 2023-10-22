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
package org.orekit.frames;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScale;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.IERSConventions.NutationCorrectionConverter;

/** Loader for EOP C04 files.
 * <p>EOP C04 files contain {@link EOPEntry
 * Earth Orientation Parameters} consistent with ITRF20xx for one year periods, with various
 * xx (05, 08, 14, 20) depending on the data source.</p>
 * <p>The EOP C04 files retrieved from the old ftp site
 * <a href="ftp://ftp.iers.org/products/eop/long-term/">ftp://ftp.iers.org/products/eop/long-term/</a>
 * were recognized thanks to their base names, which must match one of the patterns
 * {@code eopc04_##_IAU2000.##} or {@code eopc04_##.##} (or the same ending with <code>.gz</code> for
 * gzip-compressed files) where # stands for a digit character. As of early 2023, this ftp site
 * seems not to be accessible anymore.</p>
 * <p>
 * The official source for these files is now the web site
 * <a href="https://hpiers.obspm.fr/eoppc/eop/">https://hpiers.obspm.fr/eoppc/eop/</a>. These
 * files do <em>not</em> follow the old naming convention that was used in the older ftp site.
 * They lack the _05, _08 or _14 markers in the file names. The ITRF year appears only in the URL
 * (with directories eopc04_05, eop04_c08…). The directory for the current data is named eopc04
 * without any suffix. So before 2023-02-14 the eopc04 directory would contain files compatible with
 * ITRF2014 and after 2023-02-14 it would contain files compatible with ITRF2020. In each directory,
 * the files don't have any marker, hence users downloading eopc04.99 file from eopc04_05 would get
 * a file compatible with ITRF2005 whereas users downloading a file with the exact same name eopc04.99
 * but from eop04_c08 would get a file compatible with ITRF2008.
 * </p>
 * <p>
 * Starting with Orekit version 12.0, the ITRF year is retrieved by analyzing the file header, it is
 * not linked to file name anymore, hence it is compatible with any IERS site layout.
 * </p>
 * <p>
 * This class is immutable and hence thread-safe
 * </p>
 * @author Luc Maisonobe
 */
class EopC04FilesLoader extends AbstractEopLoader implements EopHistoryLoader {

    /** Build a loader for IERS EOP C04 files.
     * @param supportedNames regular expression for supported files names
     * @param manager provides access to the EOP C04 files.
     * @param utcSupplier UTC time scale.
     */
    EopC04FilesLoader(final String supportedNames,
                      final DataProvidersManager manager,
                      final Supplier<TimeScale> utcSupplier) {
        super(supportedNames, manager, utcSupplier);
    }

    /** {@inheritDoc} */
    public void fillHistory(final IERSConventions.NutationCorrectionConverter converter,
                            final SortedSet<EOPEntry> history) {
        final Parser parser = new Parser(converter, getUtc());
        final EopParserLoader loader = new EopParserLoader(parser);
        this.feed(loader);
        history.addAll(loader.getEop());
    }

    /** Internal class performing the parsing. */
    static class Parser extends AbstractEopParser {

        /** Simple constructor.
         * @param converter converter to use
         * @param utc       time scale for parsing dates.
         */
        Parser(final NutationCorrectionConverter converter,
               final TimeScale utc) {
            super(converter, null, utc);
        }

        /** {@inheritDoc} */
        public Collection<EOPEntry> parse(final InputStream input, final String name)
            throws IOException, OrekitException {

            final List<EOPEntry> history = new ArrayList<>();

            // set up a reader for line-oriented EOP C04 files
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                // reset parse info to start new file (do not clear history!)
                int lineNumber   = 0;
                boolean inHeader = true;
                final LineParser[] tentativeParsers = new LineParser[] {
                    new LineWithoutRatesParser(name),
                    new LineWithRatesParser(name)
                };
                LineParser selectedParser = null;

                // read all file
                for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                    ++lineNumber;
                    boolean parsed = false;

                    if (inHeader) {
                        // maybe it's an header line
                        for (final LineParser parser : tentativeParsers) {
                            if (parser.parseHeaderLine(line)) {
                                // we recognized one EOP C04 format
                                selectedParser = parser;
                                break;
                            }
                        }
                    }

                    if (selectedParser != null) {
                        // maybe it's a data line
                        final EOPEntry entry = selectedParser.parseDataLine(line);
                        if (entry != null) {

                            // this is a data line, build an entry from the extracted fields
                            history.add(entry);
                            parsed = true;

                            // we know we have already finished header
                            inHeader = false;

                        }
                    }

                    if (!(inHeader || parsed)) {
                        throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                lineNumber, name, line);
                    }
                }

                // check if we have read something
                if (inHeader) {
                    throw new OrekitException(OrekitMessages.NOT_A_SUPPORTED_IERS_DATA_FILE, name);
                }
            }

            return history;
        }

        /** Base parser for EOP C04 lines.
         * @since 12.0
         */
        private abstract class LineParser {

            /** Pattern for ITRF version. */
            private final Pattern itrfVersionPattern;

            /** Pattern for columns header. */
            private final Pattern columnHeaderPattern;

            /** Pattern for data lines. */
            private final Pattern dataPattern;

            /** Year group. */
            private final int yearGroup;

            /** Month group. */
            private final int monthGroup;

            /** Day group. */
            private final int dayGroup;

            /** MJD group. */
            private final int mjdGroup;

            /** Name of the stream for error messages. */
            private final String name;

            /** ITRF version. */
            private ITRFVersion itrfVersion;

            /** Simple constructor.
             * @param itrfVersionRegexp regular expression for ITRF version
             * @param columnsHeaderRegexp regular expression for columns header
             * @param dataRegexp regular expression for data lines
             * @param yearGroup year group
             * @param monthGroup month group
             * @param dayGroup day group
             * @param mjdGroup MJD group
             * @param name  of the stream for error messages.
             */
            protected LineParser(final String itrfVersionRegexp, final String columnsHeaderRegexp,
                                 final String dataRegexp,
                                 final int yearGroup, final int monthGroup, final int dayGroup,
                                 final int mjdGroup, final String name) {
                this.itrfVersionPattern  = Pattern.compile(itrfVersionRegexp);
                this.columnHeaderPattern = Pattern.compile(columnsHeaderRegexp);
                this.dataPattern         = Pattern.compile(dataRegexp);
                this.yearGroup           = yearGroup;
                this.monthGroup          = monthGroup;
                this.dayGroup            = dayGroup;
                this.mjdGroup            = mjdGroup;
                this.name                = name;
            }

            /** Get the ITRF version for this EOP C04 file.
             * @return ITRF version
             */
            protected ITRFVersion getItrfVersion() {
                return itrfVersion;
            }

            /** Parse a header line.
             * @param line line to parse
             * @return true if line was recognized (either ITRF version or columns header)
             */
            public boolean parseHeaderLine(final String line) {
                final Matcher itrfVersionMatcher = itrfVersionPattern.matcher(line);
                if (itrfVersionMatcher.matches()) {
                    switch (Integer.parseInt(itrfVersionMatcher.group(1))) {
                        case 5 :
                            itrfVersion = ITRFVersion.ITRF_2005;
                            break;
                        case 8 :
                            itrfVersion = ITRFVersion.ITRF_2008;
                            break;
                        case 14 :
                            itrfVersion = ITRFVersion.ITRF_2014;
                            break;
                        case 20 :
                            itrfVersion = ITRFVersion.ITRF_2020;
                            break;
                        default :
                            throw new OrekitException(OrekitMessages.NO_SUCH_ITRF_FRAME, itrfVersionMatcher.group(1));
                    }
                    return true;
                } else {
                    final Matcher columnHeaderMatcher = columnHeaderPattern.matcher(line);
                    if (columnHeaderMatcher.matches()) {
                        parseColumnsHeaderLine(columnHeaderMatcher);
                        return true;
                    }
                    return false;
                }
            }

            /** Parse a data line.
             * @param line line to parse
             * @return EOP entry for the line, or null if line does not match expected regular expression
             */
            public EOPEntry parseDataLine(final String line) {

                final Matcher matcher = dataPattern.matcher(line);
                if (!matcher.matches()) {
                    // this is not a data line
                    return null;
                }

                // check date
                final DateComponents dc = new DateComponents(Integer.parseInt(matcher.group(yearGroup)),
                                                             Integer.parseInt(matcher.group(monthGroup)),
                                                             Integer.parseInt(matcher.group(dayGroup)));
                final int    mjd   = Integer.parseInt(matcher.group(mjdGroup));
                if (dc.getMJD() != mjd) {
                    throw new OrekitException(OrekitMessages.INCONSISTENT_DATES_IN_IERS_FILE,
                                              name, dc.getYear(), dc.getMonth(), dc.getDay(), mjd);
                }

                return parseDataLine(matcher, dc);

            }

            /** Parse a columns header line.
             * @param matcher matcher for line
             */
            protected abstract void parseColumnsHeaderLine(Matcher matcher);

            /** Parse a data line.
             * @param matcher matcher for line
             * @param dc date components already extracted from the line
             * @return EOP entry for the line
             */
            protected abstract EOPEntry parseDataLine(Matcher matcher, DateComponents dc);

        }

        /** Parser for data lines without pole rates.
         * <p>
         * ITRF markers have either the following form:
         * </p>
         * <pre>
         *                           EOP (IERS) 05 C04
         * </pre>
         * <p>
         * or the following form:
         * </p>
         * <pre>
         *                           EOP (IERS) 14 C04 TIME SERIES
         * </pre>
         * <p>
         * Header have either the following form:
         * </p>
         * <pre>
         *       Date      MJD      x          y        UT1-UTC       LOD         dPsi      dEps       x Err     y Err   UT1-UTC Err  LOD Err    dPsi Err   dEpsilon Err
         *                          "          "           s           s            "         "        "          "          s           s            "         "
         *      (0h UTC)
         * </pre>
         * <p>
         * or the following form:
         * </p>
         * <pre>
         *       Date      MJD      x          y        UT1-UTC       LOD         dX        dY        x Err     y Err   UT1-UTC Err  LOD Err     dX Err       dY Err
         *                          "          "           s           s          "         "           "          "          s         s            "           "
         *      (0h UTC)
         * </pre>
         * <p>
         * The data lines in the EOP C04 yearly data files have either the following fixed form:
         * </p>
         * <pre>
         * year month day MJD …12 floating values fields in decimal format...
         * 2000   1   1  51544   0.043242   0.377915   0.3554777   …
         * 2000   1   2  51545   0.043515   0.377753   0.3546065   …
         * 2000   1   3  51546   0.043623   0.377452   0.3538444   …
         * </pre>
         * @since 12.0
         */
        private class LineWithoutRatesParser extends LineParser {

            /** Nutation header group. */
            private static final int NUTATION_HEADER_GROUP = 1;

            /** Year group. */
            private static final int YEAR_GROUP = 1;

            /** Month group. */
            private static final int MONTH_GROUP = 2;

            /** Day group. */
            private static final int DAY_GROUP = 3;

            /** MJD group. */
            private static final int MJD_GROUP = 4;

            /** X component of pole motion group. */
            private static final int POLE_X_GROUP = 5;

            /** Y component of pole motion group. */
            private static final int POLE_Y_GROUP = 6;

            /** UT1-UTC group. */
            private static final int UT1_UTC_GROUP = 7;

            /** LoD group. */
            private static final int LOD_GROUP = 8;

            /** Correction for nutation first field (either dX or dPsi). */
            private static final int NUT_0_GROUP = 9;

            /** Correction for nutation second field (either dY or dEps). */
            private static final int NUT_1_GROUP = 10;

            /** Indicator for non-rotating origin. */
            private boolean isNonRotatingOrigin;

            /** Simple constructor.
             * @param name  of the stream for error messages.
             */
            LineWithoutRatesParser(final String name) {
                super("^ +EOP +\\(IERS\\) +([0-9][0-9]) +C04.*",
                      "^ *Date +MJD +x +y +UT1-UTC +LOD +((?:dPsi +dEps)|(?:dX +dY)) .*",
                      "^(\\d+) +(\\d+) +(\\d+) +(\\d+) +(-?\\d+\\.\\d+) +(-?\\d+\\.\\d+) +(-?\\d+\\.\\d+) +(-?\\d+\\.\\d+) +(-?\\d+\\.\\d+) +(-?\\d+\\.\\d+)(?: +(-?\\d+\\.\\d+)){6}$",
                      YEAR_GROUP, MONTH_GROUP, DAY_GROUP, MJD_GROUP,
                      name);
            }

            /** {@inheritDoc} */
            @Override
            protected void parseColumnsHeaderLine(final Matcher matcher) {
                isNonRotatingOrigin = matcher.group(NUTATION_HEADER_GROUP).startsWith("dX");
            }

            /** {@inheritDoc} */
            @Override
            protected EOPEntry parseDataLine(final Matcher matcher, final DateComponents dc) {

                final AbsoluteDate date = new AbsoluteDate(dc, getUtc());

                final double x     = Double.parseDouble(matcher.group(POLE_X_GROUP)) * Constants.ARC_SECONDS_TO_RADIANS;
                final double y     = Double.parseDouble(matcher.group(POLE_Y_GROUP)) * Constants.ARC_SECONDS_TO_RADIANS;
                final double dtu1  = Double.parseDouble(matcher.group(UT1_UTC_GROUP));
                final double lod   = Double.parseDouble(matcher.group(LOD_GROUP));
                final double[] equinox;
                final double[] nro;
                if (isNonRotatingOrigin) {
                    nro = new double[] {
                        Double.parseDouble(matcher.group(NUT_0_GROUP)) * Constants.ARC_SECONDS_TO_RADIANS,
                        Double.parseDouble(matcher.group(NUT_1_GROUP)) * Constants.ARC_SECONDS_TO_RADIANS
                    };
                    equinox = getConverter().toEquinox(date, nro[0], nro[1]);
                } else {
                    equinox = new double[] {
                        Double.parseDouble(matcher.group(NUT_0_GROUP)) * Constants.ARC_SECONDS_TO_RADIANS,
                        Double.parseDouble(matcher.group(NUT_1_GROUP)) * Constants.ARC_SECONDS_TO_RADIANS
                    };
                    nro = getConverter().toNonRotating(date, equinox[0], equinox[1]);
                }

                return new EOPEntry(dc.getMJD(), dtu1, lod, x, y, Double.NaN, Double.NaN,
                                    equinox[0], equinox[1], nro[0], nro[1],
                                    getItrfVersion(), date);

            }
        }

        /** Parser for data lines with pole rates.
         * <p>
         * ITRF markers have either the following form:
         * </p>
         * <pre>
         * # EOP (IERS) 20 C04 TIME SERIES  consistent with ITRF 2020 - sampled at 0h UTC
         * </pre>
         * <p>
         * Header have either the following form:
         * </p>
         * <pre>
         * # YR  MM  DD  HH       MJD        x(")        y(")  UT1-UTC(s)       dX(")      dY(")       xrt(")      yrt(")      LOD(s)        x Er        y Er  UT1-UTC Er      dX Er       dY Er       xrt Er      yrt Er      LOD Er
         * </pre>
         * <p>
         * The data lines in the EOP C04 yearly data files have either the following fixed form:
         * </p>
         * <pre>
         * year month day hour MJD (in floating format) …16 floating values fields in decimal format...
         * 2015   1   1  12  57023.50    0.030148    0.281014   …
         * 2015   1   2  12  57024.50    0.029219    0.281441   …
         * 2015   1   3  12  57025.50    0.028777    0.281824   …
         * </pre>
         * @since 12.0
         */
        private class LineWithRatesParser extends LineParser {

            /** Year group. */
            private static final int YEAR_GROUP = 1;

            /** Month group. */
            private static final int MONTH_GROUP = 2;

            /** Day group. */
            private static final int DAY_GROUP = 3;

            /** Hour group. */
            private static final int HOUR_GROUP = 4;

            /** MJD group. */
            private static final int MJD_GROUP = 5;

            /** X component of pole motion group. */
            private static final int POLE_X_GROUP = 6;

            /** Y component of pole motion group. */
            private static final int POLE_Y_GROUP = 7;

            /** UT1-UTC group. */
            private static final int UT1_UTC_GROUP = 8;

            /** Correction for nutation first field. */
            private static final int NUT_DX_GROUP = 9;

            /** Correction for nutation second field. */
            private static final int NUT_DY_GROUP = 10;

            /** X rate component of pole motion group.
             * @since 12.0
             */
            private static final int POLE_X_RATE_GROUP = 11;

            /** Y rate component of pole motion group.
             * @since 12.0
             */
            private static final int POLE_Y_RATE_GROUP = 12;

            /** LoD group. */
            private static final int LOD_GROUP = 13;

            /** Simple constructor.
             * @param name  of the stream for error messages.
             */
            LineWithRatesParser(final String name) {
                super("^# +EOP +\\(IERS\\) +([0-9][0-9]) +C04.*",
                      "^# +YR +MM +DD +H +MJD +x\\(\"\\) +y\\(\"\\) +UT1-UTC\\(s\\) +dX\\(\"\\) +dY\\(\"\\) +xrt\\(\"\\) +yrt\\'\"\\) +.*",
                      "^(\\d+) +(\\d+) +(\\d+) +(\\d+) +(\\d+)\\.\\d+ +(-?\\d+\\.\\d+) +(-?\\d+\\.\\d+) +(-?\\d+\\.\\d+) +(-?\\d+\\.\\d+) +(-?\\d+\\.\\d+) +(-?\\d+\\.\\d+) +(-?\\d+\\.\\d+) +(-?\\d+\\.\\d+)(?: +(-?\\d+\\.\\d+)){8}$", // we intentionally ignore MJD fractional part
                      YEAR_GROUP, MONTH_GROUP, DAY_GROUP, MJD_GROUP,
                      name);
            }

            /** {@inheritDoc} */
            @Override
            protected void parseColumnsHeaderLine(final Matcher matcher) {
                // nothing to do here
            }

            /** {@inheritDoc} */
            @Override
            protected EOPEntry parseDataLine(final Matcher matcher, final DateComponents dc) {

                final TimeComponents tc = new TimeComponents(Integer.parseInt(matcher.group(HOUR_GROUP)), 0, 0.0);
                final AbsoluteDate date = new AbsoluteDate(dc, tc, getUtc());

                final double x     = Double.parseDouble(matcher.group(POLE_X_GROUP)) * Constants.ARC_SECONDS_TO_RADIANS;
                final double y     = Double.parseDouble(matcher.group(POLE_Y_GROUP)) * Constants.ARC_SECONDS_TO_RADIANS;
                final double xRate = Double.parseDouble(matcher.group(POLE_X_RATE_GROUP)) *
                                     Constants.ARC_SECONDS_TO_RADIANS / Constants.JULIAN_DAY;
                final double yRate = Double.parseDouble(matcher.group(POLE_Y_RATE_GROUP)) *
                                     Constants.ARC_SECONDS_TO_RADIANS / Constants.JULIAN_DAY;
                final double dtu1  = Double.parseDouble(matcher.group(UT1_UTC_GROUP));
                final double lod   = Double.parseDouble(matcher.group(LOD_GROUP));
                final double[] nro = new double[] {
                    Double.parseDouble(matcher.group(NUT_DX_GROUP)) * Constants.ARC_SECONDS_TO_RADIANS,
                    Double.parseDouble(matcher.group(NUT_DY_GROUP)) * Constants.ARC_SECONDS_TO_RADIANS
                };
                final double[] equinox = getConverter().toEquinox(date, nro[0], nro[1]);

                return new EOPEntry(dc.getMJD(), dtu1, lod, x, y, xRate, yRate,
                                    equinox[0], equinox[1], nro[0], nro[1],
                                    getItrfVersion(), date);

            }
        }

    }

}
