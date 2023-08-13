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
package org.orekit.time;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.AbstractSelfFeedingLoader;
import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;


/** Loader for UTC versus TAI history files.
 * <p>UTC versus TAI history files contain {@link UTCTAIOffset
 * leap seconds} data since.</p>
 * <p>The UTC versus TAI history files are recognized thanks to their
 * base names, which must match the pattern <code>UTC-TAI.history</code>
 * (or <code>UTC-TAI.history.gz</code> for gzip-compressed files)</p>
 * <p>Only one history file must be present in the IERS directories
 * hierarchy.</p>
 * @author Luc Maisonobe
 */
public class UTCTAIHistoryFilesLoader extends AbstractSelfFeedingLoader
        implements UTCTAIOffsetsLoader {

    /** Supported files name pattern. */
    private static final String SUPPORTED_NAMES = "^UTC-TAI\\.history$";

    /**
     * Build a loader for UTC-TAI history file. This constructor uses the {@link
     * DataContext#getDefault() default data context}.
     *
     * @see #UTCTAIHistoryFilesLoader(DataProvidersManager)
     */
    @DefaultDataContext
    public UTCTAIHistoryFilesLoader() {
        this(DataContext.getDefault().getDataProvidersManager());
    }

    /**
     * Build a loader for UTC-TAI history file.
     *
     * @param manager provides access to the {@code UTC-TAI.history} file.
     */
    public UTCTAIHistoryFilesLoader(final DataProvidersManager manager) {
        super(SUPPORTED_NAMES, manager);
    }

    /** {@inheritDoc} */
    @Override
    public List<OffsetModel> loadOffsets() {
        final UtcTaiOffsetLoader parser = new UtcTaiOffsetLoader(new Parser());
        this.feed(parser);
        return parser.getOffsets();
    }

    /** Internal class performing the parsing. */
    public static class Parser implements UTCTAIOffsetsLoader.Parser {

        /** Regular data lines pattern. */
        private Pattern regularPattern;

        /** Last line pattern pattern. */
        private Pattern lastPattern;

        /** Simple constructor.
         */
        public Parser() {

            // the data lines in the UTC time steps data files have the following form:
            // 1966  Jan.  1 - 1968  Feb.  1     4.313 170 0s + (MJD - 39 126) x 0.002 592s
            // 1968  Feb.  1 - 1972  Jan.  1     4.213 170 0s +        ""
            // 1972  Jan.  1 -       Jul.  1    10s
            //       Jul.  1 - 1973  Jan.  1    11s
            // 1973  Jan.  1 - 1974  Jan.  1    12s
            //  ...
            // 2006  Jan.  1.- 2009  Jan.  1    33s
            // 2009  Jan.  1.- 2012  Jul   1    34s
            // 2012  Jul   1 -                  35s

            // we ignore the non-constant and non integer offsets before 1972-01-01
            final String start = "^";

            // year group
            final String yearField = "\\p{Blank}*((?:\\p{Digit}\\p{Digit}\\p{Digit}\\p{Digit})|(?:    ))";

            // second group: month as a three letters capitalized abbreviation
            final StringBuilder builder = new StringBuilder("\\p{Blank}+(");
            for (final Month month : Month.values()) {
                builder.append(month.getCapitalizedAbbreviation());
                builder.append('|');
            }
            builder.delete(builder.length() - 1, builder.length());
            builder.append(")\\.?");
            final String monthField = builder.toString();

            // day group
            final String dayField = "\\p{Blank}+([ 0-9]+)\\.?";

            // offset group
            final String offsetField = "\\p{Blank}+(\\p{Digit}+)s";

            final String separator   = "\\p{Blank}*-\\p{Blank}+";
            final String finalBlanks = "\\p{Blank}*$";
            regularPattern = Pattern.compile(start + yearField + monthField + dayField +
                                             separator + yearField + monthField + dayField +
                                             offsetField + finalBlanks);
            lastPattern    = Pattern.compile(start + yearField + monthField + dayField +
                                             separator + offsetField + finalBlanks);


        }

        /** Load UTC-TAI offsets entries read from some file.
         *
         * {@inheritDoc}
         *
         * @param input data input stream
         * @param name name of the file (or zip entry)
         * @exception IOException if data can't be read
         */
        @Override
        public List<OffsetModel> parse(final InputStream input, final String name)
            throws IOException {

            final List<OffsetModel> offsets = new ArrayList<>();
            final String emptyYear = "    ";
            int lineNumber = 0;
            DateComponents lastDate = null;
            String line = null;
            int lastLine = 0;
            String previousYear = emptyYear;
            // set up a reader for line-oriented file
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {

                // read all file, ignoring not recognized lines
                for (line = reader.readLine(); line != null; line = reader.readLine()) {
                    ++lineNumber;

                    // check matching for regular lines and last line
                    Matcher matcher = regularPattern.matcher(line);
                    if (matcher.matches()) {
                        if (lastLine > 0) {
                            throw new OrekitException(OrekitMessages.UNEXPECTED_DATA_AFTER_LINE_IN_FILE,
                                                      lastLine, name, line);
                        }
                    } else {
                        matcher = lastPattern.matcher(line);
                        if (matcher.matches()) {
                            // this is the last line (there is a start date but no end date)
                            lastLine = lineNumber;
                        }
                    }

                    if (matcher.matches()) {

                        // build an entry from the extracted fields

                        String year = matcher.group(1);
                        if (emptyYear.equals(year)) {
                            year = previousYear;
                        }
                        if (lineNumber != lastLine) {
                            if (emptyYear.equals(matcher.group(4))) {
                                previousYear = year;
                            } else {
                                previousYear = matcher.group(4);
                            }
                        }
                        final DateComponents leapDay = new DateComponents(Integer.parseInt(year.trim()),
                                                                          Month.parseMonth(matcher.group(2)),
                                                                          Integer.parseInt(matcher.group(3).trim()));

                        final int offset = Integer.parseInt(matcher.group(matcher.groupCount()));
                        if (lastDate != null && leapDay.compareTo(lastDate) <= 0) {
                            throw new OrekitException(OrekitMessages.NON_CHRONOLOGICAL_DATES_IN_FILE,
                                                      name, lineNumber);
                        }
                        lastDate = leapDay;
                        offsets.add(new OffsetModel(leapDay, offset));

                    }
                }

            }  catch (NumberFormatException nfe) {
                throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                          lineNumber, name, line);
            }

            if (offsets.isEmpty()) {
                throw new OrekitException(OrekitMessages.NO_ENTRIES_IN_IERS_UTC_TAI_HISTORY_FILE, name);
            }

            return offsets;
        }

    }

}
