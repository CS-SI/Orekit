/* Copyright 2002-2015 CS Systèmes d'Information
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
package org.orekit.frames;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.orekit.data.DataLoader;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.IERSConventions;

/** Loader for IERS rapid data and prediction files in columns format (finals file).
 * <p>Rapid data and prediction files contain {@link EOPEntry
 * Earth Orientation Parameters} for several years periods, in one file
 * only that is updated regularly.</p>
 * <p>
 * These files contain both the data from IERS Bulletin A and IERS bulletin B.
 * This class parses only the part from Bulletin A.
 * </p>
 * <p>The rapid data and prediction file is recognized thanks to its base name,
 * which must match one of the the patterns <code>finals.*</code> or
 * <code>finals2000A.*</code> (or the same ending with <code>.gz</code>
 * for gzip-compressed files) where * stands for a word like "all", "daily",
 * or "data". The file with 2000A in their name correspond to the
 * IAU-2000 precession-nutation model whereas the files without any identifier
 * correspond to the IAU-1980 precession-nutation model. The files with the all
 * suffix start from 1973-01-01, the file with the data suffix start
 * from 1992-01-01 and the files with the daily suffix.</p>
 * <p>
 * This class is immutable and hence thread-safe
 * </p>
 * @author Romain Di Costanzo
 * @see <a href="http://maia.usno.navy.mil/ser7/readme.finals2000A">finals2000A file format description at USNO</a>
 * @see <a href="http://maia.usno.navy.mil/ser7/readme.finals">finals file format description at USNO</a>
 */
class RapidDataAndPredictionColumnsLoader implements EOPHistoryLoader {

    /** Conversion factor. */
    private static final double  ARC_SECONDS_TO_RADIANS       = 2 * Math.PI / 1296000;

    /** Conversion factor. */
    private static final double  MILLI_ARC_SECONDS_TO_RADIANS = ARC_SECONDS_TO_RADIANS / 1000;

    /** Conversion factor. */
    private static final double  MILLI_SECONDS_TO_SECONDS     = 1.0e-3;

    /** Field for year, month and day parsing. */
    private static final String  INTEGER2_FIELD               = "((?:\\p{Blank}|\\p{Digit})\\p{Digit})";

    /** Field for modified Julian day parsing. */
    private static final String  MJD_FIELD                    = "\\p{Blank}+(\\p{Digit}+)(?:\\.00*)";

    /** Field for separator parsing. */
    private static final String  SEPARATOR                    = "\\p{Blank}*[IP]";

    /** Field for real parsing. */
    private static final String  REAL_FIELD                   = "\\p{Blank}*(-?\\p{Digit}*\\.\\p{Digit}*)";

    /** Start index of the date part of the line. */
    private static int DATE_START = 0;

    /** end index of the date part of the line. */
    private static int DATE_END   = 15;

    /** Pattern to match the date part of the line (always present). */
    private static final Pattern DATE_PATTERN = Pattern.compile(INTEGER2_FIELD + INTEGER2_FIELD + INTEGER2_FIELD + MJD_FIELD);

    /** Start index of the pole part of the line. */
    private static int POLE_START = 16;

    /** end index of the pole part of the line. */
    private static int POLE_END   = 55;

    /** Pattern to match the pole part of the line. */
    private static final Pattern POLE_PATTERN = Pattern.compile(SEPARATOR + REAL_FIELD + REAL_FIELD + REAL_FIELD + REAL_FIELD);

    /** Start index of the UT1-UTC part of the line. */
    private static int UT1_UTC_START = 57;

    /** end index of the UT1-UTC part of the line. */
    private static int UT1_UTC_END   = 78;

    /** Pattern to match the UT1-UTC part of the line. */
    private static final Pattern UT1_UTC_PATTERN = Pattern.compile(SEPARATOR + REAL_FIELD + REAL_FIELD);

    /** Start index of the LOD part of the line. */
    private static int LOD_START = 79;

    /** end index of the LOD part of the line. */
    private static int LOD_END   = 93;

    /** Pattern to match the LOD part of the line. */
    private static final Pattern LOD_PATTERN = Pattern.compile(REAL_FIELD + REAL_FIELD);

    /** Start index of the nutation part of the line. */
    private static int NUTATION_START = 95;

    /** end index of the nutation part of the line. */
    private static int NUTATION_END   = 134;

    /** Pattern to match the nutation part of the line. */
    private static final Pattern NUTATION_PATTERN = Pattern.compile(SEPARATOR + REAL_FIELD + REAL_FIELD + REAL_FIELD + REAL_FIELD);

    /** Type of nutation corrections. */
    private final boolean isNonRotatingOrigin;

    /** File supported name. */
    private final String  supportedNames;

    /** Build a loader for IERS bulletins B files.
     * @param isNonRotatingOrigin if true the supported files <em>must</em>
     * contain δX/δY nutation corrections, otherwise they
     * <em>must</em> contain δΔψ/δΔε nutation
     * corrections
     * @param supportedNames regular expression for supported files names
     */
    RapidDataAndPredictionColumnsLoader(final boolean isNonRotatingOrigin,
                                               final String supportedNames) {
        this.isNonRotatingOrigin = isNonRotatingOrigin;
        this.supportedNames      = supportedNames;
    }

    /** {@inheritDoc} */
    public void fillHistory(final IERSConventions.NutationCorrectionConverter converter,
                            final SortedSet<EOPEntry> history)
        throws OrekitException {
        final Parser parser = new Parser(converter, isNonRotatingOrigin);
        DataProvidersManager.getInstance().feed(supportedNames, parser);
        history.addAll(parser.history);
    }

    /** Internal class performing the parsing. */
    private static class Parser implements DataLoader {

        /** Converter for nutation corrections. */
        private final IERSConventions.NutationCorrectionConverter converter;

        /** Indicator for Non-Rotating Origin. */
        private final boolean isNonRotatingOrigin;

        /** History entries. */
        private final List<EOPEntry> history;

        /** Current line number. */
        private int lineNumber;

        /** Current line. */
        private String line;

        /** Simple constructor.
         * @param converter converter to use
         * @param isNonRotatingOrigin type of nutation correction
         */
        Parser(final IERSConventions.NutationCorrectionConverter converter,
                      final boolean isNonRotatingOrigin) {
            this.converter           = converter;
            this.isNonRotatingOrigin = isNonRotatingOrigin;
            this.history             = new ArrayList<EOPEntry>();
            this.lineNumber          = 0;
        }

        /** {@inheritDoc} */
        public boolean stillAcceptsData() {
            return true;
        }

        /** {@inheritDoc} */
        public void loadData(final InputStream input, final String name)
            throws OrekitException, IOException {

            // set up a reader for line-oriented bulletin B files
            final BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"));

            // reset parse info to start new file (do not clear history!)
            lineNumber = 0;

            for (line = reader.readLine(); line != null; line = reader.readLine()) {

                lineNumber++;

                // split the lines in its various columns (some of them can be blank)
                final String datePart      = (line.length() >= DATE_END)     ? line.substring(DATE_START,       DATE_END)     : "";
                final String polePart      = (line.length() >= POLE_END)     ? line.substring(POLE_START,       POLE_END)     : "";
                final String ut1utcPart    = (line.length() >= UT1_UTC_END ) ? line.substring(UT1_UTC_START,    UT1_UTC_END)  : "";
                final String lodPart       = (line.length() >= LOD_END)      ? line.substring(LOD_START,        LOD_END)      : "";
                final String nutationPart  = (line.length() >= NUTATION_END) ? line.substring(NUTATION_START,   NUTATION_END) : "";

                // parse the date part
                final Matcher dateMatcher = DATE_PATTERN.matcher(datePart);
                final int mjd;
                if (dateMatcher.matches()) {
                    final int yy = Integer.parseInt(dateMatcher.group(1).trim());
                    final int mm = Integer.parseInt(dateMatcher.group(2).trim());
                    final int dd = Integer.parseInt(dateMatcher.group(3).trim());
                    final int yyyy = (yy > 70) ? yy + 1900 : yy + 2000;
                    mjd = Integer.parseInt(dateMatcher.group(4).trim());
                    final int reconstructedDate = new DateComponents(yyyy, mm, dd).getMJD();
                    if (reconstructedDate != mjd) {
                        throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                  lineNumber, name, line);
                    }
                } else {
                    throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                              lineNumber, name, line);
                }

                // parse the pole part
                final double x;
                final double y;
                if (polePart.trim().length() == 0) {
                    // pole part is blank
                    x = 0;
                    y = 0;
                } else {
                    final Matcher poleMatcher = POLE_PATTERN.matcher(polePart);
                    if (poleMatcher.matches()) {
                        x = ARC_SECONDS_TO_RADIANS * Double.parseDouble(poleMatcher.group(1));
                        y = ARC_SECONDS_TO_RADIANS * Double.parseDouble(poleMatcher.group(3));
                    } else {
                        throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                  lineNumber, name, line);
                    }
                }

                // parse the UT1-UTC part
                final double dtu1;
                if (ut1utcPart.trim().length() == 0) {
                    // UT1-UTC part is blank
                    dtu1 = 0;
                } else {
                    final Matcher ut1utcMatcher = UT1_UTC_PATTERN.matcher(ut1utcPart);
                    if (ut1utcMatcher.matches()) {
                        dtu1 = Double.parseDouble(ut1utcMatcher.group(1));
                    } else {
                        throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                  lineNumber, name, line);
                    }
                }

                // parse the lod part
                final double lod;
                if (lodPart.trim().length() == 0) {
                    // lod part is blank
                    lod = 0;
                } else {
                    final Matcher lodMatcher = LOD_PATTERN.matcher(lodPart);
                    if (lodMatcher.matches()) {
                        lod = MILLI_SECONDS_TO_SECONDS * Double.parseDouble(lodMatcher.group(1));
                    } else {
                        throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                  lineNumber, name, line);
                    }
                }

                // parse the nutation part
                final double[] nro;
                final double[] equinox;
                if (nutationPart.trim().length() == 0) {
                    // nutation part is blank
                    nro     = new double[2];
                    equinox = new double[2];
                } else {
                    final Matcher nutationMatcher = NUTATION_PATTERN.matcher(nutationPart);
                    final AbsoluteDate mjdDate =
                            new AbsoluteDate(new DateComponents(DateComponents.MODIFIED_JULIAN_EPOCH, mjd),
                                             TimeScalesFactory.getUTC());
                    if (nutationMatcher.matches()) {
                        if (isNonRotatingOrigin) {
                            nro = new double[] {
                                MILLI_ARC_SECONDS_TO_RADIANS * Double.parseDouble(nutationMatcher.group(1)),
                                MILLI_ARC_SECONDS_TO_RADIANS * Double.parseDouble(nutationMatcher.group(3))
                            };
                            equinox = converter.toEquinox(mjdDate, nro[0], nro[1]);
                        } else {
                            equinox = new double[] {
                                MILLI_ARC_SECONDS_TO_RADIANS * Double.parseDouble(nutationMatcher.group(1)),
                                MILLI_ARC_SECONDS_TO_RADIANS * Double.parseDouble(nutationMatcher.group(3))
                            };
                            nro = converter.toNonRotating(mjdDate, equinox[0], equinox[1]);
                        }
                    } else {
                        throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                  lineNumber, name, line);
                    }
                }

                history.add(new EOPEntry(mjd, dtu1, lod, x, y, equinox[0], equinox[1], nro[0], nro[1]));

            }

        }

    }

}
