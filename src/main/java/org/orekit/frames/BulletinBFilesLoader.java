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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.math3.util.FastMath;
import org.orekit.data.DataLoader;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.Month;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

/** Loader for bulletin B files.
 * <p>Bulletin B files contain {@link EOPEntry
 * Earth Orientation Parameters} for a few months periods.
 * They correspond fo tinalized data, suitable for long term
 * a posteriori analysis.</p>
 * <p>The bulletin B files are recognized thanks to their base names,
 * which must match one of the patterns <code>bulletinb_IAU2000-###.txt</code>,
 * <code>bulletinb_IAU2000.###</code>, <code>bulletinb-###.txt</code> or
 * <code>bulletinb.###</code> (or the same ending with <code>.gz</code>
 * for gzip-compressed files) where # stands for a digit character.</p>
 * <p>
 * Starting with bulletin B 252 published in February 2009, buletins B are
 * written in a format containing nutation corrections for both the
 * new IAU2000 nutation model as dx, dy entries in its section 1 and nutation
 * corrections for the old IAU1976 nutation model as dPsi, dEpsilon entries in
 * its sections 2. These bulletins are available from IERS <a
 * href="ftp://ftp.iers.org/products/eop/bulletinb/format_2009/">
 *  FTP site</a>. They are also available with exactly the same content
 * (but a different naming convention) from <a
 * href="http://hpiers.obspm.fr/eoppc/bul/bulb_new/">Paris-Meudon
 * observatory site</a>.
 * </p>
 * <p>
 * Ending with bulletin B 263 published in January 2010, bulletins B were
 * written in a format containing only one type of nutation corrections in its
 * section 1, either for new IAU2000 nutation model as dx, dy entries or the old
 * IAU1976 nutation model as dPsi, dEpsilon entries, depending on the file (a pair of
 * files with different name was published each month between March 2003 and January 2010).
 * </p>
 * <p>
 * This class handles both the old and the new format.
 * </p>
 * <p>
 * This class is immutable and hence thread-safe
 * </p>
 * @author Luc Maisonobe
 */
class BulletinBFilesLoader implements EOPHistoryLoader {

    /** Conversion factor. */
    private static final double MILLI_ARC_SECONDS_TO_RADIANS = Constants.ARC_SECONDS_TO_RADIANS / 1000;

    /** Conversion factor. */
    private static final double MILLI_SECONDS_TO_SECONDS = 1.e-3;

    /** Section 1 header pattern. */
    private static final Pattern SECTION_1_HEADER;

    /** Section 2 header pattern for old format. */
    private static final Pattern SECTION_2_HEADER_OLD;

    /** Section 3 header pattern. */
    private static final Pattern SECTION_3_HEADER;

    /** Pattern for line introducing the final bulletin B values. */
    private static final Pattern FINAL_VALUES_START;

    /** Pattern for line introducing the bulletin B preliminary extension. */
    private static final Pattern FINAL_VALUES_END;

    /** Data line pattern in section 1 (old format). */
    private static final Pattern SECTION_1_DATA_OLD_FORMAT;

    /** Data line pattern in section 2. */
    private static final Pattern SECTION_2_DATA_OLD_FORMAT;

    /** Data line pattern in section 1 (new format). */
    private static final Pattern SECTION_1_DATA_NEW_FORMAT;

    /** Data line pattern in section 3 (new format). */
    private static final Pattern SECTION_3_DATA_NEW_FORMAT;

    static {

        // the section headers lines in the old bulletin B monthly data files have
        // the following form (the indentation discrepancy for section 6 is really
        // present in the available files):
        // 1 - EARTH ORIENTATION PARAMETERS (IERS evaluation).
        // either
        // 2 - SMOOTHED VALUES OF x, y, UT1, D, DPSI, DEPSILON (IERS EVALUATION)
        // or
        // 2 - SMOOTHED VALUES OF x, y, UT1, D, dX, dY (IERS EVALUATION)
        // 3 - NORMAL VALUES OF THE EARTH ORIENTATION PARAMETERS AT FIVE-DAY INTERVALS
        // 4 - DURATION OF THE DAY AND ANGULAR VELOCITY OF THE EARTH (IERS evaluation).
        // 5 - INFORMATION ON TIME SCALES
        //       6 - SUMMARY OF CONTRIBUTED EARTH ORIENTATION PARAMETERS SERIES
        //
        // the section headers lines in the new bulletin B monthly data files have
        // the following form:
        // 1 - DAILY FINAL VALUES OF  x, y, UT1-UTC, dX, dY
        // 2 - DAILY FINAL VALUES OF CELESTIAL POLE OFFSETS dPsi1980 & dEps1980
        // 3 - EARTH ANGULAR VELOCITY : DAILY FINAL VALUES OF LOD, OMEGA AT 0hUTC
        // 4 - INFORMATION ON TIME SCALES
        // 5 - SUMMARY OF CONTRIBUTED EARTH ORIENTATION PARAMETERS SERIES
        SECTION_1_HEADER     = Pattern.compile("^ +1 - (\\p{Upper}+) \\p{Upper}+ \\p{Upper}+.*");
        SECTION_2_HEADER_OLD = Pattern.compile("^ +2 - SMOOTHED \\p{Upper}+ \\p{Upper}+.*((?:DPSI, DEPSILON)|(?:dX, dY)).*");
        SECTION_3_HEADER     = Pattern.compile("^ +3 - \\p{Upper}+ \\p{Upper}+ \\p{Upper}+.*");

        // the markers bracketing the final values in section 1 in the old bulletin B
        // monthly data files have the following form:
        //
        //  Final Bulletin B values.
        //   ...
        //  Preliminary extension, to be updated weekly in Bulletin A and monthly
        //  in Bulletin B.
        //
        // the markers bracketing the final values in section 1 in the new bulletin B
        // monthly data files have the following form:
        //
        //  Final values
        //   ...
        //  Preliminary extension
        //
        FINAL_VALUES_START = Pattern.compile("^\\p{Blank}+Final( Bulletin B)? values.*");
        FINAL_VALUES_END   = Pattern.compile("^\\p{Blank}+Preliminary extension.*");

        // the data lines in the old bulletin B monthly data files have the following form:
        // in section 1:
        // AUG   1  55044  0.22176 0.49302  0.231416  -33.768584   -69.1    -8.9
        // AUG   6  55049  0.23202 0.48003  0.230263  -33.769737   -69.5    -8.5
        // in section 2:
        // AUG   1   55044  0.22176  0.49302  0.230581 -0.835  -0.310  -69.1   -8.9
        // AUG   2   55045  0.22395  0.49041  0.230928 -0.296  -0.328  -69.5   -8.9
        //
        // the data lines in the new bulletin B monthly data files have the following form:
        // in section 1:
        // 2009   8   2   55045  223.954  490.410  230.9277    0.214 -0.056    0.008    0.009    0.0641  0.048  0.121
        // 2009   8   3   55046  225.925  487.700  231.2186    0.300 -0.138    0.010    0.012    0.0466  0.099  0.248
        // 2009   8   4   55047  227.931  485.078  231.3929    0.347 -0.231    0.019    0.023    0.0360  0.099  0.249
        // 2009   8   5   55048  230.016  482.445  231.4601    0.321 -0.291    0.025    0.028    0.0441  0.095  0.240
        // 2009   8   6   55049  232.017  480.026  231.3619    0.267 -0.273    0.025    0.029    0.0477  0.038  0.095
        // in section 2:
        // 2009   8   2   55045   -69.474    -8.929     0.199     0.121
        // 2009   8   3   55046   -69.459    -9.016     0.250     0.248
        // 2009   8   4   55047   -69.401    -9.039     0.250     0.249
        // 2009   8   5   55048   -69.425    -8.864     0.247     0.240
        // 2009   8   6   55049   -69.510    -8.539     0.153     0.095
        // in section 3:
        // 2009   8   2   55045 -0.3284  0.0013  15.04106723584    0.00000000023
        // 2009   8   3   55046 -0.2438  0.0013  15.04106722111    0.00000000023
        // 2009   8   4   55047 -0.1233  0.0013  15.04106720014    0.00000000023
        // 2009   8   5   55048  0.0119  0.0013  15.04106717660    0.00000000023
        // 2009   8   6   55049  0.1914  0.0013  15.04106714535    0.00000000023
        final StringBuilder builder = new StringBuilder("^\\p{Blank}+(?:");
        for (final Month month : Month.values()) {
            builder.append(month.getUpperCaseAbbreviation());
            builder.append('|');
        }
        builder.delete(builder.length() - 1, builder.length());
        builder.append(")");
        final String integerPattern      = "[-+]?\\p{Digit}+";
        final String realPattern         = "[-+]?(?:(?:\\p{Digit}+(?:\\.\\p{Digit}*)?)|(?:\\.\\p{Digit}+))(?:[eE][-+]?\\p{Digit}+)?";
        final String monthNameField      = builder.toString();
        final String ignoredIntegerField = "\\p{Blank}*" + integerPattern;
        final String storedIntegerField  = "\\p{Blank}*(" + integerPattern + ")";
        final String mjdField            = "\\p{Blank}+(\\p{Digit}\\p{Digit}\\p{Digit}\\p{Digit}\\p{Digit})";
        final String storedRealField     = "\\p{Blank}+(" + realPattern + ")";
        final String ignoredRealField    = "\\p{Blank}+" + realPattern;
        final String finalBlanks         = "\\p{Blank}*$";
        SECTION_1_DATA_OLD_FORMAT = Pattern.compile(monthNameField + ignoredIntegerField + mjdField +
                                                    ignoredRealField + ignoredRealField + ignoredRealField +
                                                    ignoredRealField + ignoredRealField + ignoredRealField +
                                                    finalBlanks);
        SECTION_2_DATA_OLD_FORMAT = Pattern.compile(monthNameField + ignoredIntegerField + mjdField +
                                                    storedRealField  + storedRealField  + storedRealField +
                                                    ignoredRealField +
                                                    storedRealField + storedRealField + storedRealField +
                                                    finalBlanks);
        SECTION_1_DATA_NEW_FORMAT = Pattern.compile(storedIntegerField + storedIntegerField + storedIntegerField + mjdField +
                                                    storedRealField + storedRealField + storedRealField +
                                                    storedRealField + storedRealField + ignoredRealField + ignoredRealField +
                                                    ignoredRealField + ignoredRealField + ignoredRealField +
                                                    finalBlanks);
        SECTION_3_DATA_NEW_FORMAT = Pattern.compile(ignoredIntegerField + ignoredIntegerField + ignoredIntegerField + mjdField +
                                                    storedRealField +
                                                    ignoredRealField + ignoredRealField + ignoredRealField +
                                                    finalBlanks);

    }

    /** Regular expression for supported files names. */
    private final String supportedNames;

    /** Build a loader for IERS bulletins B files.
    * @param supportedNames regular expression for supported files names
    */
    BulletinBFilesLoader(final String supportedNames) {
        this.supportedNames = supportedNames;
    }

    /** {@inheritDoc} */
    public void fillHistory(final IERSConventions.NutationCorrectionConverter converter,
                            final SortedSet<EOPEntry> history)
        throws OrekitException {
        final Parser parser = new Parser(converter);
        DataProvidersManager.getInstance().feed(supportedNames, parser);
        history.addAll(parser.history);
    }

    /** Internal class performing the parsing. */
    private static class Parser implements DataLoader {

        /** Converter for nutation corrections. */
        private final IERSConventions.NutationCorrectionConverter converter;

        /** History entries. */
        private final List<EOPEntry> history;

        /** Map for fields read in different sections. */
        private final Map<Integer, double[]> fieldsMap;

        /** Current line number. */
        private int lineNumber;

        /** Current line. */
        private String line;

        /** Start of final data. */
        private int mjdMin;

        /** End of final data. */
        private int mjdMax;

        /** Simple constructor.
         * @param converter converter to use
         */
        Parser(final IERSConventions.NutationCorrectionConverter converter) {
            this.converter  = converter;
            this.history    = new ArrayList<EOPEntry>();
            this.fieldsMap  = new HashMap<Integer, double[]>();
            this.lineNumber = 0;
            this.mjdMin     = Integer.MAX_VALUE;
            this.mjdMax     = Integer.MIN_VALUE;
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
            fieldsMap.clear();
            lineNumber = 0;
            mjdMin     = Integer.MAX_VALUE;
            mjdMax     = Integer.MIN_VALUE;

            // skip header up to section 1 and check if we are parsing an old or new format file
            final Matcher section1Matcher = seekToLine(SECTION_1_HEADER, reader, name);
            final boolean isOldFormat = "EARTH".equals(section1Matcher.group(1));

            if (isOldFormat) {

                // extract MJD bounds for final data from section 1
                loadMJDBoundsOldFormat(reader, name);

                final Matcher section2Matcher = seekToLine(SECTION_2_HEADER_OLD, reader, name);
                final boolean isNonRotatingOrigin = section2Matcher.group(1).startsWith("dX");
                loadEOPOldFormat(isNonRotatingOrigin, reader, name);

            } else {

                // extract x, y, UT1-UTC, dx, dy from section 1
                loadXYDTDxDyNewFormat(reader, name);

                // skip to section 3
                seekToLine(SECTION_3_HEADER, reader, name);

                // extract LOD data from section 3
                loadLODNewFormat(reader, name);

                // set up the EOP entries
                for (Map.Entry<Integer, double[]> entry : fieldsMap.entrySet()) {
                    final int mjd = entry.getKey();
                    final double[] array = entry.getValue();
                    if (Double.isNaN(array[0]) || Double.isNaN(array[1]) ||
                            Double.isNaN(array[2]) || Double.isNaN(array[3]) ||
                            Double.isNaN(array[4]) || Double.isNaN(array[5])) {
                        throw notifyUnexpectedErrorEncountered(name);
                    }
                    final AbsoluteDate mjdDate =
                            new AbsoluteDate(new DateComponents(DateComponents.MODIFIED_JULIAN_EPOCH, mjd),
                                             TimeScalesFactory.getUTC());
                    final double[] equinox = converter.toEquinox(mjdDate, array[4], array[5]);
                    history.add(new EOPEntry(mjd, array[0], array[1], array[2], array[3],
                                             equinox[0], equinox[1], array[4], array[5]));
                }

            }

        }

        /** Read until a line matching a pattern is found.
         * @param pattern pattern to look for
         * @param reader reader from where file content is obtained
         * @param name name of the file (or zip entry)
         * @return the matching matcher for the line
         * @exception IOException if data can't be read
         * @exception OrekitException if end of file is reached before line has been found
         */
        private Matcher seekToLine(final Pattern pattern, final BufferedReader reader, final String name)
            throws IOException, OrekitException {

            for (line = reader.readLine(); line != null; line = reader.readLine()) {
                ++lineNumber;
                final Matcher matcher = pattern.matcher(line);
                if (matcher.matches()) {
                    return matcher;
                }
            }

            // we have reached end of file and not found a matching line
            throw new OrekitException(OrekitMessages.UNEXPECTED_END_OF_FILE_AFTER_LINE,
                                      name, lineNumber);

        }

        /** Read MJD bounds of the final data part from section 1 in the old bulletin B format.
         * @param reader reader from where file content is obtained
         * @param name name of the file (or zip entry)
         * @exception IOException if data can't be read
         * @exception OrekitException if some data is missing or if some loader specific error occurs
         */
        private void loadMJDBoundsOldFormat(final BufferedReader reader, final String name)
            throws OrekitException, IOException {

            boolean inFinalValuesPart = false;
            for (line = reader.readLine(); line != null; line = reader.readLine()) {
                lineNumber++;
                Matcher matcher = FINAL_VALUES_START.matcher(line);
                if (matcher.matches()) {
                    // we are entering final values part (in section 1)
                    inFinalValuesPart = true;
                } else if (inFinalValuesPart) {
                    matcher = SECTION_1_DATA_OLD_FORMAT.matcher(line);
                    if (matcher.matches()) {
                        // this is a data line, build an entry from the extracted fields
                        final int mjd = Integer.parseInt(matcher.group(1));
                        mjdMin = FastMath.min(mjdMin, mjd);
                        mjdMax = FastMath.max(mjdMax, mjd);
                    } else {
                        matcher = FINAL_VALUES_END.matcher(line);
                        if (matcher.matches()) {
                            // we leave final values part
                            return;
                        }
                    }
                }
            }

            throw new OrekitException(OrekitMessages.UNEXPECTED_END_OF_FILE_AFTER_LINE,
                                      name, lineNumber);

        }

        /** Read EOP data from section 2 in the old bulletin B format.
         * @param isNonRotatingOrigin if true, the file contain Non-Rotating Origin nutation corrections
         * @param reader reader from where file content is obtained
         * @param name name of the file (or zip entry)
         * @exception IOException if data can't be read
         * @exception OrekitException if some data is missing or if some loader specific error occurs
         */
        private void loadEOPOldFormat(final boolean isNonRotatingOrigin,
                                      final BufferedReader reader, final String name)
            throws OrekitException, IOException {

            // read the data lines in the final values part inside section 2
            for (line = reader.readLine(); line != null; line = reader.readLine()) {
                lineNumber++;
                final Matcher matcher = SECTION_2_DATA_OLD_FORMAT.matcher(line);
                if (matcher.matches()) {
                    // this is a data line, build an entry from the extracted fields
                    final int    mjd   = Integer.parseInt(matcher.group(1));
                    final double x     = Double.parseDouble(matcher.group(2)) * Constants.ARC_SECONDS_TO_RADIANS;
                    final double y     = Double.parseDouble(matcher.group(3)) * Constants.ARC_SECONDS_TO_RADIANS;
                    final double dtu1  = Double.parseDouble(matcher.group(4));
                    final double lod   = Double.parseDouble(matcher.group(5)) * MILLI_SECONDS_TO_SECONDS;
                    if (mjd >= mjdMin) {
                        final AbsoluteDate mjdDate =
                                new AbsoluteDate(new DateComponents(DateComponents.MODIFIED_JULIAN_EPOCH, mjd),
                                                 TimeScalesFactory.getUTC());
                        final double[] equinox;
                        final double[] nro;
                        if (isNonRotatingOrigin) {
                            nro = new double[] {
                                Double.parseDouble(matcher.group(6)) * MILLI_ARC_SECONDS_TO_RADIANS,
                                Double.parseDouble(matcher.group(7)) * MILLI_ARC_SECONDS_TO_RADIANS
                            };
                            equinox = converter.toEquinox(mjdDate, nro[0], nro[1]);
                        } else {
                            equinox = new double[] {
                                Double.parseDouble(matcher.group(6)) * MILLI_ARC_SECONDS_TO_RADIANS,
                                Double.parseDouble(matcher.group(7)) * MILLI_ARC_SECONDS_TO_RADIANS
                            };
                            nro = converter.toNonRotating(mjdDate, equinox[0], equinox[1]);
                        }
                        history.add(new EOPEntry(mjd, dtu1, lod, x, y, equinox[0], equinox[1], nro[0], nro[1]));
                        if (mjd >= mjdMax) {
                            // don't bother reading the rest of the file
                            return;
                        }
                    }
                }
            }

        }

        /** Read X, Y, UT1-UTC, dx, dy from section 1 in the new bulletin B format.
         * @param reader reader from where file content is obtained
         * @param name name of the file (or zip entry)
         * @exception IOException if data can't be read
         * @exception OrekitException if some data is missing or if some loader specific error occurs
         */
        private void loadXYDTDxDyNewFormat(final BufferedReader reader, final String name)
            throws OrekitException, IOException {

            boolean inFinalValuesPart = false;
            for (line = reader.readLine(); line != null; line = reader.readLine()) {
                lineNumber++;
                Matcher matcher = FINAL_VALUES_START.matcher(line);
                if (matcher.matches()) {
                    // we are entering final values part (in section 1)
                    inFinalValuesPart = true;
                } else if (inFinalValuesPart) {
                    matcher = SECTION_1_DATA_NEW_FORMAT.matcher(line);
                    if (matcher.matches()) {
                        // this is a data line, build an entry from the extracted fields
                        final int year  = Integer.parseInt(matcher.group(1));
                        final int month = Integer.parseInt(matcher.group(2));
                        final int day   = Integer.parseInt(matcher.group(3));
                        final int mjd   = Integer.parseInt(matcher.group(4));
                        if (new DateComponents(year, month, day).getMJD() != mjd) {
                            throw new OrekitException(OrekitMessages.INCONSISTENT_DATES_IN_IERS_FILE,
                                                      name, year, month, day, mjd);
                        }
                        mjdMin = FastMath.min(mjdMin, mjd);
                        mjdMax = FastMath.max(mjdMax, mjd);
                        final double x    = Double.parseDouble(matcher.group(5)) * MILLI_ARC_SECONDS_TO_RADIANS;
                        final double y    = Double.parseDouble(matcher.group(6)) * MILLI_ARC_SECONDS_TO_RADIANS;
                        final double dtu1 = Double.parseDouble(matcher.group(7)) * MILLI_SECONDS_TO_SECONDS;
                        final double dx   = Double.parseDouble(matcher.group(8)) * MILLI_ARC_SECONDS_TO_RADIANS;
                        final double dy   = Double.parseDouble(matcher.group(9)) * MILLI_ARC_SECONDS_TO_RADIANS;
                        fieldsMap.put(mjd,
                                      new double[] {
                                          dtu1, Double.NaN, x, y, dx, dy
                                      });
                    } else {
                        matcher = FINAL_VALUES_END.matcher(line);
                        if (matcher.matches()) {
                            // we leave final values part
                            return;
                        }
                    }
                }
            }
        }

        /** Read LOD from section 3 in the new bulletin B format.
         * @param reader reader from where file content is obtained
         * @param name name of the file (or zip entry)
         * @exception IOException if data can't be read
         * @exception OrekitException if some data is missing or if some loader specific error occurs
         */
        private void loadLODNewFormat(final BufferedReader reader, final String name)
            throws OrekitException, IOException {
            for (line = reader.readLine(); line != null; line = reader.readLine()) {
                lineNumber++;
                final Matcher matcher = SECTION_3_DATA_NEW_FORMAT.matcher(line);
                if (matcher.matches()) {
                    // this is a data line, build an entry from the extracted fields
                    final int    mjd = Integer.parseInt(matcher.group(1));
                    if (mjd >= mjdMin) {
                        final double lod = Double.parseDouble(matcher.group(2)) * MILLI_SECONDS_TO_SECONDS;
                        final double[] array = fieldsMap.get(mjd);
                        if (array == null) {
                            throw notifyUnexpectedErrorEncountered(name);
                        }
                        array[1] = lod;
                        if (mjd >= mjdMax) {
                            // don't bother reading the rest of the file
                            return;
                        }
                    }
                }
            }
        }

        /** Create an exception to be thrown.
         * @param name name of the file (or zip entry)
         * @return OrekitException always thrown to notify an unexpected error has been
         * encountered by the caller
         */
        private OrekitException notifyUnexpectedErrorEncountered(final String name) {
            String loaderName = BulletinBFilesLoader.class.getName();
            loaderName = loaderName.substring(loaderName.lastIndexOf('.') + 1);
            return new OrekitException(OrekitMessages.UNEXPECTED_FILE_FORMAT_ERROR_FOR_LOADER,
                                       name, loaderName);
        }

    }

}
