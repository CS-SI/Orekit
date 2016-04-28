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
package org.orekit.time;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hipparchus.util.FastMath;
import org.orekit.data.DataLoader;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

/** Loader for UTC-TAI extracted from tai-utc.dat file from USNO.
 * <p>
 * This class is immutable and hence thread-safe
 * </p>
 * @author Luc Maisonobe
 * @since 7.1
 */
public class TAIUTCDatFilesLoader implements UTCTAIOffsetsLoader {

    /** Default supported files name pattern. */
    public static final String DEFAULT_SUPPORTED_NAMES = "^tai-utc\\.dat$";

    /** Regular expression for supported files names. */
    private final String supportedNames;

    /** Build a loader for tai-utc.dat file from USNO.
     * @param supportedNames regular expression for supported files names
     */
    public TAIUTCDatFilesLoader(final String supportedNames) {
        this.supportedNames = supportedNames;
    }

    /** {@inheritDoc} */
    @Override
    public List<OffsetModel> loadOffsets() throws OrekitException {
        final Parser parser = new Parser();
        DataProvidersManager.getInstance().feed(supportedNames, parser);
        return parser.getOffsets();
    }

    /** Internal class performing the parsing. */
    private static class Parser implements DataLoader {

        /** Regular expression for optional blanks. */
        private static final String BLANKS               = "\\p{Blank}*";

        /** Regular expression for storage start. */
        private static final String STORAGE_START        = "(";

        /** Regular expression for storage end. */
        private static final String STORAGE_END          = ")";

        /** Regular expression for alternative. */
        private static final String ALTERNATIVE          = "|";

        /** Regular expression matching blanks at start of line. */
        private static final String LINE_START_REGEXP     = "^" + BLANKS;

        /** Regular expression matching blanks at end of line. */
        private static final String LINE_END_REGEXP       = BLANKS + "$";

        /** Regular expression matching integers. */
        private static final String INTEGER_REGEXP        = "[-+]?\\p{Digit}+";

        /** Regular expression matching real numbers. */
        private static final String REAL_REGEXP           = "[-+]?(?:(?:\\p{Digit}+(?:\\.\\p{Digit}*)?)|(?:\\.\\p{Digit}+))(?:[eE][-+]?\\p{Digit}+)?";

        /** Regular expression matching an integer field to store. */
        private static final String STORED_INTEGER_FIELD  = BLANKS + STORAGE_START + INTEGER_REGEXP + STORAGE_END;

        /** Regular expression matching a real field to store. */
        private static final String STORED_REAL_FIELD     = BLANKS + STORAGE_START + REAL_REGEXP + STORAGE_END;

        /** Data lines pattern. */
        private Pattern dataPattern;

        /** UTC-TAI offsets. */
        private List<OffsetModel> offsets;

        /** Simple constructor.
         */
        Parser() {

            // data lines read:
            // 1965 SEP  1 =JD 2439004.5  TAI-UTC=   3.8401300 S + (MJD - 38761.) X 0.001296 S
            // 1966 JAN  1 =JD 2439126.5  TAI-UTC=   4.3131700 S + (MJD - 39126.) X 0.002592 S
            // 1968 FEB  1 =JD 2439887.5  TAI-UTC=   4.2131700 S + (MJD - 39126.) X 0.002592 S
            // 1972 JAN  1 =JD 2441317.5  TAI-UTC=  10.0       S + (MJD - 41317.) X 0.0      S
            // 1972 JUL  1 =JD 2441499.5  TAI-UTC=  11.0       S + (MJD - 41317.) X 0.0      S
            // 1973 JAN  1 =JD 2441683.5  TAI-UTC=  12.0       S + (MJD - 41317.) X 0.0      S
            // 1974 JAN  1 =JD 2442048.5  TAI-UTC=  13.0       S + (MJD - 41317.) X 0.0      S

            // month as a three letters upper case abbreviation
            final StringBuilder builder = new StringBuilder(BLANKS + STORAGE_START);
            for (final Month month : Month.values()) {
                builder.append(month.getUpperCaseAbbreviation());
                builder.append(ALTERNATIVE);
            }
            builder.delete(builder.length() - 1, builder.length());
            builder.append(STORAGE_END);
            final String monthField = builder.toString();

            dataPattern = Pattern.compile(LINE_START_REGEXP +
                                          STORED_INTEGER_FIELD + monthField + STORED_INTEGER_FIELD +
                                          "\\p{Blank}+=JD" + STORED_REAL_FIELD +
                                          "\\p{Blank}+TAI-UTC=" + STORED_REAL_FIELD +
                                          "\\p{Blank}+S\\p{Blank}+\\+\\p{Blank}+\\(MJD\\p{Blank}+-" + STORED_REAL_FIELD +
                                          "\\p{Blank}*\\)\\p{Blank}+X" + STORED_REAL_FIELD +
                                          "\\p{Blank}*S" + LINE_END_REGEXP);

            offsets = new ArrayList<OffsetModel>();

        }

        /** Get the parsed offsets.
         * @return parsed offsets
         */
        public List<OffsetModel> getOffsets() {
            return offsets;
        }

        /** {@inheritDoc} */
        public boolean stillAcceptsData() {
            return offsets.isEmpty();
        }

        /** Load UTC-TAI offsets entries read from some file.
         * <p>The time steps are extracted from some {@code tai-utc.dat} file.
         * Since entries are stored in a {@link java.util.SortedMap SortedMap},
         * they are chronologically sorted and only one entry remains for a given date.</p>
         * @param input data input stream
         * @param name name of the file (or zip entry)
         * @exception IOException if data can't be read
         * @exception ParseException if data can't be parsed
         * @exception OrekitException if some data is missing
         * or if some loader specific error occurs
         */
        public void loadData(final InputStream input, final String name)
            throws OrekitException, IOException, ParseException {

            offsets.clear();

            // set up a reader for line-oriented file
            final BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"));

            // read all file, ignoring not recognized lines
            int lineNumber = 0;
            DateComponents lastDate = null;
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                ++lineNumber;

                // check matching for data lines
                final Matcher matcher = dataPattern.matcher(line);
                if (matcher.matches()) {

                    try {
                        // build an entry from the extracted fields
                        final DateComponents dc1 = new DateComponents(Integer.parseInt(matcher.group(1)),
                                                                      Month.parseMonth(matcher.group(2)),
                                                                      Integer.parseInt(matcher.group(3)));
                        final DateComponents dc2 = new DateComponents(DateComponents.JULIAN_EPOCH,
                                                                      (int) FastMath.ceil(Double.parseDouble(matcher.group(4))));
                        if (!dc1.equals(dc2)) {
                            throw new OrekitException(OrekitMessages.INCONSISTENT_DATES_IN_IERS_FILE,
                                                      name, dc1.getYear(), dc1.getMonth(), dc1.getDay(), dc2.getMJD());
                        }

                        if ((lastDate != null) && dc1.compareTo(lastDate) <= 0) {
                            throw new OrekitException(OrekitMessages.NON_CHRONOLOGICAL_DATES_IN_FILE,
                                                      name, lineNumber);
                        }
                        lastDate = dc1;

                        final double offset = Double.parseDouble(matcher.group(5));
                        final double mjdRef = Double.parseDouble(matcher.group(6));
                        final double slope  = Double.parseDouble(matcher.group(7));
                        offsets.add(new OffsetModel(dc1, (int) FastMath.rint(mjdRef), offset, slope));

                    } catch (NumberFormatException nfe) {
                        throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                  lineNumber, name, line);
                    }
                }
            }

            if (offsets.isEmpty()) {
                throw new OrekitException(OrekitMessages.NO_ENTRIES_IN_IERS_UTC_TAI_HISTORY_FILE, name);
            }

        }

    }

}
