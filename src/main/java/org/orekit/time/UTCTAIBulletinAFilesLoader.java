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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hipparchus.util.FastMath;
import org.orekit.data.DataLoader;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

/** Loader for UTC-TAI extracted from bulletin A files.
 * <p>This class is a modified version of {@code BulletinAFileLoader}
 * that only parses the TAI-UTC header line and checks the UT1-UTC column
 * for discontinuities.
 * </p>
 * <p>
 * Note that extracting UTC-TAI from bulletin A files is <em>NOT</em>
 * recommended. There are known issues in some past bulletin A
 * (for example bulletina-xix-001.txt from 2006-01-05 has a wrong year
 * for last leap second and bulletina-xxi-053.txt from 2008-12-31 has an
 * off by one value for TAI-UTC on MJD 54832). This is a known problem,
 * and the Earth Orientation Department at USNO told us this TAI-UTC
 * data was only provided as a convenience and this data should rather
 * be sourced from other official files. As the bulletin A files are
 * a record of past publications, they cannot modify archived bulletins,
 * hence the errors above will remain forever. This UTC-TAI loader should
 * therefore be used with great care.
 * </p>
 * <p>
 * This class is immutable and hence thread-safe
 * </p>
 * @author Luc Maisonobe
 * @since 7.1
 */
public class UTCTAIBulletinAFilesLoader implements UTCTAIOffsetsLoader {

    /** Regular expression for supported files names. */
    private final String supportedNames;

    /** Build a loader for IERS bulletins A files.
    * @param supportedNames regular expression for supported files names
    */
    public UTCTAIBulletinAFilesLoader(final String supportedNames) {
        this.supportedNames = supportedNames;
    }

    /** {@inheritDoc} */
    @Override
    public List<OffsetModel> loadOffsets() throws OrekitException {

        final Parser parser = new Parser();
        DataProvidersManager.getInstance().feed(supportedNames, parser);
        final SortedMap<Integer, Integer> taiUtc = parser.getTaiUtc();
        final SortedMap<Integer, Double>  ut1Utc = parser.getUt1Utc();

        // identify UT1-UTC discontinuities
        final List<Integer> leapDays = new ArrayList<Integer>();
        Map.Entry<Integer, Double> previous = null;
        for (final Map.Entry<Integer, Double> entry : ut1Utc.entrySet()) {
            if (previous != null) {
                final double delta = entry.getValue() - previous.getValue();
                if (FastMath.abs(delta) > 0.5) {
                    // discontinuity found between previous and current entry, a leap second has occurred
                    leapDays.add(entry.getKey());
                }
            }
            previous = entry;
        }

        final List<OffsetModel> offsets = new ArrayList<OffsetModel>();

        if (!taiUtc.isEmpty()) {

            // find the start offset, before the first UT1-UTC entry
            final Map.Entry<Integer, Integer> firstTaiMUtc = taiUtc.entrySet().iterator().next();
            int offset = firstTaiMUtc.getValue();
            final int refMJD = firstTaiMUtc.getKey();
            for (final int leapMJD : leapDays) {
                if (leapMJD > refMJD) {
                    break;
                }
                --offset;
            }

            // set all known time steps
            for (final int leapMJD : leapDays) {
                offsets.add(new OffsetModel(new DateComponents(DateComponents.MODIFIED_JULIAN_EPOCH, leapMJD),
                                            ++offset));
            }

            // check for missing time steps
            for (final Map.Entry<Integer, Integer> refTaiMUtc : taiUtc.entrySet()) {
                final DateComponents refDC = new DateComponents(DateComponents.MODIFIED_JULIAN_EPOCH,
                                                                refTaiMUtc.getKey() + 1);
                OffsetModel before = null;
                for (final OffsetModel o : offsets) {
                    if (o.getStart().compareTo(refDC) < 0) {
                        before = o;
                    }
                }
                if (before != null) {
                    if (refTaiMUtc.getValue() != (int) FastMath.rint(before.getOffset())) {
                        throw new OrekitException(OrekitMessages.MISSING_EARTH_ORIENTATION_PARAMETERS_BETWEEN_DATES,
                                                  before.getStart(), refDC);
                    }
                }
            }

            // make sure we stop the linear drift that was used before 1972
            if (offsets.isEmpty()) {
                offsets.add(0, new OffsetModel(new DateComponents(1972, 1, 1), taiUtc.get(taiUtc.firstKey())));
            } else {
                if (offsets.get(0).getStart().getYear() > 1972) {
                    offsets.add(0, new OffsetModel(new DateComponents(1972, 1, 1),
                                                ((int) FastMath.rint(offsets.get(0).getOffset())) - 1));
                }
            }

        }

        return offsets;

    }

    /** Internal class performing the parsing. */
    private static class Parser implements DataLoader {

        /** Regular expression matching blanks at start of line. */
        private static final String LINE_START_REGEXP     = "^\\p{Blank}+";

        /** Regular expression matching blanks at end of line. */
        private static final String LINE_END_REGEXP       = "\\p{Blank}*$";

        /** Regular expression matching integers. */
        private static final String INTEGER_REGEXP        = "[-+]?\\p{Digit}+";

        /** Regular expression matching real numbers. */
        private static final String REAL_REGEXP           = "[-+]?(?:(?:\\p{Digit}+(?:\\.\\p{Digit}*)?)|(?:\\.\\p{Digit}+))(?:[eE][-+]?\\p{Digit}+)?";

        /** Regular expression matching an integer field to store. */
        private static final String STORED_INTEGER_FIELD  = "\\p{Blank}*(" + INTEGER_REGEXP + ")";

        /** regular expression matching a Modified Julian Day field to store. */
        private static final String STORED_MJD_FIELD      = "\\p{Blank}+(\\p{Digit}\\p{Digit}\\p{Digit}\\p{Digit}\\p{Digit})";

        /** Regular expression matching a real field to store. */
        private static final String STORED_REAL_FIELD     = "\\p{Blank}+(" + REAL_REGEXP + ")";

        /** Regular expression matching a real field to ignore. */
        private static final String IGNORED_REAL_FIELD    = "\\p{Blank}+" + REAL_REGEXP;

        /** Enum for files sections, in expected order.
         * <p>The bulletin A weekly data files contain several sections,
         * each introduced with some fixed header text and followed by tabular data.
         * </p>
         */
        private enum Section {

            /** Earth Orientation Parameters rapid service. */
            // section 2 always contain rapid service data including error fields
            //      COMBINED EARTH ORIENTATION PARAMETERS:
            //
            //                              IERS Rapid Service
            //              MJD      x    error     y    error   UT1-UTC   error
            //                       "      "       "      "        s        s
            //   13  8 30  56534 0.16762 .00009 0.32705 .00009  0.038697 0.000019
            //   13  8 31  56535 0.16669 .00010 0.32564 .00010  0.038471 0.000019
            //   13  9  1  56536 0.16592 .00009 0.32410 .00010  0.038206 0.000024
            //   13  9  2  56537 0.16557 .00009 0.32270 .00009  0.037834 0.000024
            //   13  9  3  56538 0.16532 .00009 0.32147 .00010  0.037351 0.000024
            //   13  9  4  56539 0.16488 .00009 0.32044 .00010  0.036756 0.000023
            //   13  9  5  56540 0.16435 .00009 0.31948 .00009  0.036036 0.000024
            EOP_RAPID_SERVICE("^ *COMBINED EARTH ORIENTATION PARAMETERS: *$",
                              LINE_START_REGEXP +
                              STORED_INTEGER_FIELD + STORED_INTEGER_FIELD + STORED_INTEGER_FIELD +
                              STORED_MJD_FIELD +
                              IGNORED_REAL_FIELD + IGNORED_REAL_FIELD +
                              IGNORED_REAL_FIELD + IGNORED_REAL_FIELD +
                              STORED_REAL_FIELD  + IGNORED_REAL_FIELD +
                              LINE_END_REGEXP),

           /** Earth Orientation Parameters final values. */
           // the first bulletin A of each month also includes final values for the
           // period covering from day 2 of month m-2 to day 1 of month m-1.
           //                                IERS Final Values
           //                                 MJD        x        y      UT1-UTC
           //                                            "        "         s
           //             13  7  2           56475    0.1441   0.3901   0.05717
           //             13  7  3           56476    0.1457   0.3895   0.05716
           //             13  7  4           56477    0.1467   0.3887   0.05728
           //             13  7  5           56478    0.1477   0.3875   0.05755
           //             13  7  6           56479    0.1490   0.3862   0.05793
           //             13  7  7           56480    0.1504   0.3849   0.05832
           //             13  7  8           56481    0.1516   0.3835   0.05858
           //             13  7  9           56482    0.1530   0.3822   0.05877
           EOP_FINAL_VALUES("^ *IERS Final Values *$",
                            LINE_START_REGEXP +
                            STORED_INTEGER_FIELD + STORED_INTEGER_FIELD + STORED_INTEGER_FIELD +
                            STORED_MJD_FIELD +
                            IGNORED_REAL_FIELD +
                            IGNORED_REAL_FIELD +
                            STORED_REAL_FIELD +
                            LINE_END_REGEXP),

           /** TAI-UTC part of the Earth Orientation Parameters prediction.. */
           // section 3 always contain prediction data without error fields
           //
           //         PREDICTIONS:
           //         The following formulas will not reproduce the predictions given below,
           //         but may be used to extend the predictions beyond the end of this table.
           //
           //         x =  0.0969 + 0.1110 cos A - 0.0103 sin A - 0.0435 cos C - 0.0171 sin C
           //         y =  0.3457 - 0.0061 cos A - 0.1001 sin A - 0.0171 cos C + 0.0435 sin C
           //            UT1-UTC = -0.0052 - 0.00104 (MJD - 56548) - (UT2-UT1)
           //
           //         where A = 2*pi*(MJD-56540)/365.25 and C = 2*pi*(MJD-56540)/435.
           //
           //            TAI-UTC(MJD 56541) = 35.0
           //         The accuracy may be estimated from the expressions:
           //         S x,y = 0.00068 (MJD-56540)**0.80   S t = 0.00025 (MJD-56540)**0.75
           //         Estimated accuracies are:  Predictions     10 d   20 d   30 d   40 d
           //                                    Polar coord's  0.004  0.007  0.010  0.013
           //                                    UT1-UTC        0.0014 0.0024 0.0032 0.0040
           //
           //                       MJD      x(arcsec)   y(arcsec)   UT1-UTC(sec)
           //          2013  9  6  56541       0.1638      0.3185      0.03517
           //          2013  9  7  56542       0.1633      0.3175      0.03420
           //          2013  9  8  56543       0.1628      0.3164      0.03322
           //          2013  9  9  56544       0.1623      0.3153      0.03229
           //          2013  9 10  56545       0.1618      0.3142      0.03144
           //          2013  9 11  56546       0.1612      0.3131      0.03071
           //          2013  9 12  56547       0.1607      0.3119      0.03008
           TAI_UTC("^ *PREDICTIONS: *$",
                    LINE_START_REGEXP +
                    "TAI-UTC\\(MJD *" +
                    STORED_MJD_FIELD +
                    "\\) *= *" +
                    STORED_INTEGER_FIELD + "(?:\\.0*)?" +
                    LINE_END_REGEXP),

            /** Earth Orientation Parameters prediction. */
            // section 3 always contain prediction data without error fields
            //
            //         PREDICTIONS:
            //         The following formulas will not reproduce the predictions given below,
            //         but may be used to extend the predictions beyond the end of this table.
            //
            //         x =  0.0969 + 0.1110 cos A - 0.0103 sin A - 0.0435 cos C - 0.0171 sin C
            //         y =  0.3457 - 0.0061 cos A - 0.1001 sin A - 0.0171 cos C + 0.0435 sin C
            //            UT1-UTC = -0.0052 - 0.00104 (MJD - 56548) - (UT2-UT1)
            //
            //         where A = 2*pi*(MJD-56540)/365.25 and C = 2*pi*(MJD-56540)/435.
            //
            //            TAI-UTC(MJD 56541) = 35.0
            //         The accuracy may be estimated from the expressions:
            //         S x,y = 0.00068 (MJD-56540)**0.80   S t = 0.00025 (MJD-56540)**0.75
            //         Estimated accuracies are:  Predictions     10 d   20 d   30 d   40 d
            //                                    Polar coord's  0.004  0.007  0.010  0.013
            //                                    UT1-UTC        0.0014 0.0024 0.0032 0.0040
            //
            //                       MJD      x(arcsec)   y(arcsec)   UT1-UTC(sec)
            //          2013  9  6  56541       0.1638      0.3185      0.03517
            //          2013  9  7  56542       0.1633      0.3175      0.03420
            //          2013  9  8  56543       0.1628      0.3164      0.03322
            //          2013  9  9  56544       0.1623      0.3153      0.03229
            //          2013  9 10  56545       0.1618      0.3142      0.03144
            //          2013  9 11  56546       0.1612      0.3131      0.03071
            //          2013  9 12  56547       0.1607      0.3119      0.03008
            EOP_PREDICTION("^ *MJD *x\\(arcsec\\) *y\\(arcsec\\) *UT1-UTC\\(sec\\) *$",
                           LINE_START_REGEXP +
                           STORED_INTEGER_FIELD + STORED_INTEGER_FIELD + STORED_INTEGER_FIELD +
                           STORED_MJD_FIELD +
                           IGNORED_REAL_FIELD +
                           IGNORED_REAL_FIELD +
                           STORED_REAL_FIELD +
                           LINE_END_REGEXP);

            /** Header pattern. */
            private final Pattern header;

            /** Data pattern. */
            private final Pattern data;

            /** Simple constructor.
             * @param headerRegExp regular expression for header
             * @param dataRegExp regular expression for data
             */
            Section(final String headerRegExp, final String dataRegExp) {
                this.header = Pattern.compile(headerRegExp);
                this.data   = Pattern.compile(dataRegExp);
            }

            /** Check if a line matches the section header.
             * @param l line to check
             * @return true if the line matches the header
             */
            public boolean matchesHeader(final String l) {
                return header.matcher(l).matches();
            }

            /** Get the data fields from a line.
             * @param l line to parse
             * @return extracted fields, or null if line does not match data format
             */
            public String[] getFields(final String l) {
                final Matcher matcher = data.matcher(l);
                if (matcher.matches()) {
                    final String[] fields = new String[matcher.groupCount()];
                    for (int i = 0; i < fields.length; ++i) {
                        fields[i] = matcher.group(i + 1);
                    }
                    return fields;
                } else {
                    return null;
                }
            }

        };

        /** TAI-UTC history. */
        private final SortedMap<Integer, Integer> taiUtc;

        /** UT1-UTC history. */
        private final SortedMap<Integer, Double> ut1Utc;

        /** Current line number. */
        private int lineNumber;

        /** Current line. */
        private String line;

        /** Simple constructor.
         */
        Parser() {
            this.taiUtc     = new TreeMap<Integer, Integer>();
            this.ut1Utc     = new TreeMap<Integer, Double>();
            this.lineNumber = 0;
        }

        /** Get TAI-UTC history.
         * @return TAI-UTC history
         */
        public SortedMap<Integer, Integer> getTaiUtc() {
            return taiUtc;
        }

        /** Get UT1-UTC history.
         * @return UT1-UTC history
         */
        public SortedMap<Integer, Double> getUt1Utc() {
            return ut1Utc;
        }

        /** {@inheritDoc} */
        @Override
        public boolean stillAcceptsData() {
            return true;
        }

        /** {@inheritDoc} */
        @Override
        public void loadData(final InputStream input, final String name)
            throws OrekitException, IOException {

            // set up a reader for line-oriented bulletin A files
            final BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"));
            lineNumber =  0;

            // loop over sections
            final List<Section> remaining = new ArrayList<Section>();
            remaining.addAll(Arrays.asList(Section.values()));
            for (Section section = nextSection(remaining, reader, name);
                    section != null;
                    section = nextSection(remaining, reader, name)) {

                if (section == Section.TAI_UTC) {
                    loadTaiUtc(section, reader, name);
                } else {
                    // load the values
                    loadTimeSteps(section, reader, name);
                }

                // remove the already parsed section from the list
                remaining.remove(section);

            }

            // check that the mandatory sections have been parsed
            if (remaining.contains(Section.EOP_RAPID_SERVICE) || remaining.contains(Section.EOP_PREDICTION)) {
                throw new OrekitException(OrekitMessages.NOT_A_SUPPORTED_IERS_DATA_FILE, name);
            }

        }

        /** Skip to next section header.
         * @param sections sections to check for
         * @param reader reader from where file content is obtained
         * @param name name of the file (or zip entry)
         * @return the next section or null if no section is found until end of file
         * @exception IOException if data can't be read
         */
        private Section nextSection(final List<Section> sections, final BufferedReader reader, final String name)
            throws IOException {

            for (line = reader.readLine(); line != null; line = reader.readLine()) {
                ++lineNumber;
                for (Section section : sections) {
                    if (section.matchesHeader(line)) {
                        return section;
                    }
                }
            }

            // we have reached end of file and not found a matching section header
            return null;

        }

        /** Read TAI-UTC.
         * @param section section to parse
         * @param reader reader from where file content is obtained
         * @param name name of the file (or zip entry)
         * @exception IOException if data can't be read
         * @exception OrekitException if some data is missing or if some loader specific error occurs
         */
        private void loadTaiUtc(final Section section, final BufferedReader reader, final String name)
            throws OrekitException, IOException {

            for (line = reader.readLine(); line != null; line = reader.readLine()) {
                lineNumber++;
                final String[] fields = section.getFields(line);
                if (fields != null) {
                    // we have found the single line we are looking for
                    final int mjd    = Integer.parseInt(fields[0]);
                    final int offset = Integer.parseInt(fields[1]);
                    taiUtc.put(mjd, offset);
                    return;
                }
            }

            throw new OrekitException(OrekitMessages.UNEXPECTED_END_OF_FILE_AFTER_LINE,
                                      name, lineNumber);

        }

        /** Read UT1-UTC.
         * @param section section to parse
         * @param reader reader from where file content is obtained
         * @param name name of the file (or zip entry)
         * @exception IOException if data can't be read
         * @exception OrekitException if some data is missing or if some loader specific error occurs
         */
        private void loadTimeSteps(final Section section, final BufferedReader reader, final String name)
            throws OrekitException, IOException {

            boolean inValuesPart = false;
            for (line = reader.readLine(); line != null; line = reader.readLine()) {
                lineNumber++;
                final String[] fields = section.getFields(line);
                if (fields != null) {

                    // we are within the values part
                    inValuesPart = true;

                    // this is a data line, build an entry from the extracted fields
                    final int year  = Integer.parseInt(fields[0]);
                    final int month = Integer.parseInt(fields[1]);
                    final int day   = Integer.parseInt(fields[2]);
                    final int mjd   = Integer.parseInt(fields[3]);
                    final DateComponents dc = new DateComponents(DateComponents.MODIFIED_JULIAN_EPOCH, mjd);
                    if ((dc.getYear() % 100) != (year % 100) ||
                            dc.getMonth() != month ||
                            dc.getDay() != day) {
                        throw new OrekitException(OrekitMessages.INCONSISTENT_DATES_IN_IERS_FILE,
                                                  name, year, month, day, mjd);
                    }

                    final double offset = Double.parseDouble(fields[4]);
                    ut1Utc.put(mjd, offset);

                } else if (inValuesPart) {
                    // we leave values part
                    return;
                }
            }

            throw new OrekitException(OrekitMessages.UNEXPECTED_END_OF_FILE_AFTER_LINE,
                                      name, lineNumber);

        }

    }

}
