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
import java.util.Arrays;
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
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.DateComponents;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

/** Loader for bulletin A files.
 * <p>Bulletin A files contain {@link EOPEntry
 * Earth Orientation Parameters} for a few days periods, they
 * correspond to rapid data estimations, suitable for near-real time
 * and prediction purposes. Prediction series are only available for
 * pole motion xp, yp and UT1-UTC, they are not available for
 * pole offsets (Δδψ/Δδε and x/y).</p>
 * <p>A bulletin A published on Modified Julian Day mjd (nominally a
 * Thursday) will generally contain:
 * </p>
 * <ul>
 *   <li>rapid service xp, yp and UT1-UTC data from mjd-6 to mjd</li>
 *   <li>prediction xp, yp and UT1-UTC data from mjd+1 to mjd+365</li>
 *   <li>if it is first bulletin of month m, final values xp, yp and
 *       UT1-UTC data from day 2 of month m-2 to day 1 of month m-1</li>
 *   <li>rapid service pole offsets Δδψ/Δδε and x/y if available, for some
 *       varying period somewhere from mjd-30 to mjd-10 (see below)</li>
 *   <li>if it is first bulletin of month m, final values pole offsets
 *       Δδψ/Δδε and x/y data from day 2 of month m-2 to day 1 of month
 *       m-1</li>
 * </ul>
 * <p>
 * There are some discrepancies in the rapid service time range above,
 * mainly when the nominal publication Thursday corresponds to holidays.
 * In this case a bulletin may be published the day before and have a 6
 * days span only for rapid data, and a later bulletin will have an 8 days
 * span to recover the normal schedule. This occurred for bulletin A Vol.
 * XVIII No. 047, bulletin A Vol. XVIII No. 048, bulletin A Vol. XXI No.
 * 052 and bulletin A Vol. XXII No. 001.
 * </p>
 * <p>Rapid service for pole offsets appears irregular. As extreme examples
 * bulletin A Vol. XXVI No. 037 from 2013-09-12 contained 15 entries
 * for pole offsets, from mjd-22 to mjd-8, bulletin A Vol. XXVI No. 039
 * from 2013-09-26 contained only 3 entries for pole offsets, from mjd-15
 * to mjd-13, and bulletin A Vol. XXVI No. 040 from 2013-10-03 contained no
 * rapid service pole offsets at all, it contained only final values. Despite
 * this irregularity, rapid service data is continuous over consecutive files,
 * so the mean number of entries is 7 as the files are published on a weekly
 * basis.
 * </p>
 * <p>
 * There are no prediction data for pole offsets.
 * </p>
 * <p>
 * This loader reads both the rapid service, the prediction and the final
 * values parts. As successive files have overlaps between all these sections,
 * values extracted from latest files (with respect to the covered dates)
 * override values extracted from earlier files, regardless of the files
 * reading order. If numerous bulletins A covering more than one year are read,
 * one particular date will typically appear in the prediction section of
 * 52 or 53 files, then in the rapid data section of one file, then it will
 * be missing in a few files, and will finally appear a last time in the
 * final values sections of a last file. In this case, the value retained
 * will be the one extracted from the final values section in the more
 * recent file.
 * </p>
 * <p>
 * If only one bulletin A file is read and it correspond to the first bulletin
 * of a month, it will have a roughly one month wide hole between the
 * final data and the rapid data. This hole will trigger an error as EOP
 * continuity is checked by default for at most 5 days holes. In this case,
 * users should call something like {@link FramesFactory#setEOPContinuityThreshold(double)
 * FramesFactory.setEOPContinuityThreshold(Constants.JULIAN_YEAR)} to prevent
 * the error to be triggered.
 * </p>
 * <p>The bulletin A files are recognized thanks to their base names,
 * which must match the pattern <code>bulletina-xxxx-###.txt</code>,
 * (or the same ending with <code>.gz</code> for gzip-compressed files)
 * where x stands for a roman numeral character and # stands for a digit
 * character.</p>
 * <p>
 * This class is immutable and hence thread-safe
 * </p>
 * @author Luc Maisonobe
 * @since 7.0
 */
class BulletinAFilesLoader implements EOPHistoryLoader {

    /** Conversion factor. */
    private static final double MILLI_ARC_SECONDS_TO_RADIANS = Constants.ARC_SECONDS_TO_RADIANS / 1000;

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
                          STORED_REAL_FIELD + IGNORED_REAL_FIELD +
                          STORED_REAL_FIELD + IGNORED_REAL_FIELD +
                          STORED_REAL_FIELD + IGNORED_REAL_FIELD +
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
                        STORED_REAL_FIELD +
                        STORED_REAL_FIELD +
                        STORED_REAL_FIELD +
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
        EOP_PREDICTION("^ *PREDICTIONS: *$",
                       LINE_START_REGEXP +
                       STORED_INTEGER_FIELD + STORED_INTEGER_FIELD + STORED_INTEGER_FIELD +
                       STORED_MJD_FIELD +
                       STORED_REAL_FIELD +
                       STORED_REAL_FIELD +
                       STORED_REAL_FIELD +
                       LINE_END_REGEXP),

        /** Pole offsets, IAU-1980. */
        // section 4 may contain rapid service pole offset series including error fields
        //        CELESTIAL POLE OFFSET SERIES:
        //                             NEOS Celestial Pole Offset Series
        //                         MJD      dpsi    error     deps    error
        //                                          (msec. of arc)
        //                        56519   -87.47     0.13   -12.96     0.08
        //                        56520   -87.72     0.13   -13.20     0.08
        //                        56521   -87.79     0.19   -13.56     0.11
        POLE_OFFSETS_IAU_1980_RAPID_SERVICE("^ *NEOS Celestial Pole Offset Series *$",
                                            LINE_START_REGEXP +
                                            STORED_MJD_FIELD +
                                            STORED_REAL_FIELD + IGNORED_REAL_FIELD +
                                            STORED_REAL_FIELD + IGNORED_REAL_FIELD +
                                            LINE_END_REGEXP),

        /** Pole offsets, IAU-1980 final values. */
        // the format for the IAU-2000 series is similar, but the meanings of the fields
        // are different
        //                       IAU2000A Celestial Pole Offset Series
        //                        MJD      dX     error     dY     error
        //                                      (msec. of arc)
        //                       56519   -0.246   0.052   -0.223   0.080
        //                       56520   -0.239   0.052   -0.248   0.080
        //                       56521   -0.224   0.076   -0.277   0.110
        POLE_OFFSETS_IAU_1980_FINAL_VALUES("^ *IERS Celestial Pole Offset Final Series *$",
                                           LINE_START_REGEXP +
                                           STORED_MJD_FIELD +
                                           STORED_REAL_FIELD +
                                           STORED_REAL_FIELD +
                                           LINE_END_REGEXP),

        /** Pole offsets, IAU-2000. */
        // the first bulletin A of each month also includes final values for the
        // period covering from day 2 of month m-2 to day 1 of month m-1.
        //                    IERS Celestial Pole Offset Final Series
        //                          MJD          dpsi      deps
        //                                       (msec. of arc)
        //                         56475       -81.0     -13.3
        //                         56476       -81.2     -13.4
        //                         56477       -81.6     -13.4
        //                         56478       -82.2     -13.5
        //                         56479       -82.5     -13.6
        //                         56480       -82.5     -13.7
        POLE_OFFSETS_IAU_2000_RAPID_SERVICE("^ *IAU2000A Celestial Pole Offset Series *$",
                                            LINE_START_REGEXP +
                                            STORED_MJD_FIELD +
                                            STORED_REAL_FIELD + IGNORED_REAL_FIELD +
                                            STORED_REAL_FIELD + IGNORED_REAL_FIELD +
                                            LINE_END_REGEXP),

        /** Pole offsets, IAU-2000 final values. */
        // the format for the IAU-2000 series is similar, but the meanings of the fields
        // are different
        //                   IAU2000A Celestial Pole Offset Final Series
        //                            MJD     dX         dY
        //                            (msec. of arc)
        //                          56475     0.00      -0.28
        //                          56476    -0.06      -0.29
        //                          56477    -0.07      -0.27
        //                          56478    -0.12      -0.33
        //                          56479    -0.12      -0.33
        //                          56480    -0.13      -0.36
        POLE_OFFSETS_IAU_2000_FINAL_VALUES("^ *IAU2000A Celestial Pole Offset Final Series *$",
                                           LINE_START_REGEXP +
                                           STORED_MJD_FIELD +
                                           STORED_REAL_FIELD +
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
         * @param line line to check
         * @return true if the line matches the header
         */
        public boolean matchesHeader(final String line) {
            return header.matcher(line).matches();
        }

        /** Get the data fields from a line.
         * @param line line to parse
         * @return extracted fields, or null if line does not match data format
         */
        public String[] getFields(final String line) {
            final Matcher matcher = data.matcher(line);
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

    /** Regular expression for supported files names. */
    private final String supportedNames;

    /** Build a loader for IERS bulletins A files.
    * @param supportedNames regular expression for supported files names
    */
    BulletinAFilesLoader(final String supportedNames) {
        this.supportedNames = supportedNames;
    }

    /** {@inheritDoc} */
    public void fillHistory(final IERSConventions.NutationCorrectionConverter converter,
                            final SortedSet<EOPEntry> history)
        throws OrekitException {
        final Parser parser = new Parser();
        DataProvidersManager.getInstance().feed(supportedNames, parser);
        parser.fill(history);
    }

    /** Internal class performing the parsing. */
    private static class Parser implements DataLoader {

        /** Map for xp, yp, dut1 fields read in different sections. */
        private final Map<Integer, double[]> eopFieldsMap;

        /** Map for pole offsets fields read in different sections. */
        private final Map<Integer, double[]> poleOffsetsFieldsMap;

        /** Current line number. */
        private int lineNumber;

        /** Current line. */
        private String line;

        /** Earliest parsed data. */
        private int mjdMin;

        /** Latest parsed data. */
        private int mjdMax;

        /** First MJD parsed in current file. */
        private int firstMJD;

        /** Simple constructor.
         */
        Parser() {
            this.eopFieldsMap         = new HashMap<Integer, double[]>();
            this.poleOffsetsFieldsMap = new HashMap<Integer, double[]>();
            this.lineNumber           = 0;
            this.mjdMin               = Integer.MAX_VALUE;
            this.mjdMax               = Integer.MIN_VALUE;
            this.firstMJD             = -1;
        }

        /** {@inheritDoc} */
        public boolean stillAcceptsData() {
            return true;
        }

        /** {@inheritDoc} */
        public void loadData(final InputStream input, final String name)
            throws OrekitException, IOException {

            // set up a reader for line-oriented bulletin A files
            final BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"));
            lineNumber =  0;
            firstMJD   = -1;

            // loop over sections
            final List<Section> remaining = new ArrayList<Section>();
            remaining.addAll(Arrays.asList(Section.values()));
            for (Section section = nextSection(remaining, reader, name);
                 section != null;
                 section = nextSection(remaining, reader, name)) {

                switch (section) {
                    case EOP_RAPID_SERVICE :
                    case EOP_FINAL_VALUES  :
                    case EOP_PREDICTION    :
                        loadXYDT(section, reader, name);
                        break;
                    case POLE_OFFSETS_IAU_1980_RAPID_SERVICE :
                    case POLE_OFFSETS_IAU_1980_FINAL_VALUES  :
                        loadPoleOffsets(section, false, reader, name);
                        break;
                    case POLE_OFFSETS_IAU_2000_RAPID_SERVICE :
                    case POLE_OFFSETS_IAU_2000_FINAL_VALUES  :
                        loadPoleOffsets(section, true, reader, name);
                        break;
                    default :
                        // this should never happen
                        throw new OrekitInternalError(null);
                }

                // remove the already parsed section from the list
                remaining.remove(section);

            }

            // check that the mandatory sections have been parsed
            if (remaining.contains(Section.EOP_RAPID_SERVICE) ||
                remaining.contains(Section.EOP_PREDICTION) ||
                (remaining.contains(Section.POLE_OFFSETS_IAU_1980_RAPID_SERVICE) ^
                 remaining.contains(Section.POLE_OFFSETS_IAU_2000_RAPID_SERVICE)) ||
                (remaining.contains(Section.POLE_OFFSETS_IAU_1980_FINAL_VALUES) ^
                 remaining.contains(Section.POLE_OFFSETS_IAU_2000_FINAL_VALUES))) {
                throw new OrekitException(OrekitMessages.NOT_A_SUPPORTED_IERS_DATA_FILE, name);
            }

        }

        /** Fill EOP history obtained after reading several files.
         * @param history history to fill up
         * @exception OrekitException if UTC time scale cannot be retrieved
         */
        public void fill(final SortedSet<EOPEntry> history)
            throws OrekitException {

            double[] currentEOP = null;
            double[] nextEOP    = eopFieldsMap.get(mjdMin);
            for (int mjd = mjdMin; mjd <= mjdMax; ++mjd) {

                final double[] currentPole = poleOffsetsFieldsMap.get(mjd);

                final double[] previousEOP = currentEOP;
                currentEOP = nextEOP;
                nextEOP    = eopFieldsMap.get(mjd + 1);

                if (currentEOP == null) {
                    if (currentPole != null) {
                        // we have only pole offsets for this date
                        history.add(new EOPEntry(mjd,
                                                 0.0, 0.0, 0.0, 0.0,
                                                 currentPole[1] * MILLI_ARC_SECONDS_TO_RADIANS,
                                                 currentPole[2] * MILLI_ARC_SECONDS_TO_RADIANS,
                                                 currentPole[3] * MILLI_ARC_SECONDS_TO_RADIANS,
                                                 currentPole[4] * MILLI_ARC_SECONDS_TO_RADIANS));
                    }
                } else {

                    // compute LOD as the opposite of the time derivative of UT1-UTC
                    final double lod;
                    if (previousEOP == null) {
                        if (nextEOP == null) {
                            // isolated point
                            lod = 0;
                        } else {
                            // first entry, we use a forward difference
                            lod = currentEOP[3] - nextEOP[3];
                        }
                    } else {
                        if (nextEOP == null) {
                            // last entry, we use a backward difference
                            lod = previousEOP[3] - currentEOP[3];
                        } else {
                            // regular entry, we use a centered difference
                            lod = 0.5 * (previousEOP[3] - nextEOP[3]);
                        }
                    }

                    if (currentPole == null) {
                        // we have only EOP for this date
                        history.add(new EOPEntry(mjd,
                                                 currentEOP[3], lod,
                                                 currentEOP[1] * Constants.ARC_SECONDS_TO_RADIANS,
                                                 currentEOP[2] * Constants.ARC_SECONDS_TO_RADIANS,
                                                 0.0, 0.0, 0.0, 0.0));
                    } else {
                        // we have complete data
                        history.add(new EOPEntry(mjd,
                                                 currentEOP[3], lod,
                                                 currentEOP[1]  * Constants.ARC_SECONDS_TO_RADIANS,
                                                 currentEOP[2]  * Constants.ARC_SECONDS_TO_RADIANS,
                                                 currentPole[1] * MILLI_ARC_SECONDS_TO_RADIANS,
                                                 currentPole[2] * MILLI_ARC_SECONDS_TO_RADIANS,
                                                 currentPole[3] * MILLI_ARC_SECONDS_TO_RADIANS,
                                                 currentPole[4] * MILLI_ARC_SECONDS_TO_RADIANS));
                    }
                }

            }

        }

        /** Skip to next section header.
         * @param sections sections to check for
         * @param reader reader from where file content is obtained
         * @param name name of the file (or zip entry)
         * @return the next section or null if no section is found until end of file
         * @exception IOException if data can't be read
         */
        private Section nextSection(final List<Section> sections,
                                    final BufferedReader reader, final String name)
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

        /** Read X, Y, UT1-UTC.
         * @param section section to parse
         * @param reader reader from where file content is obtained
         * @param name name of the file (or zip entry)
         * @exception IOException if data can't be read
         * @exception OrekitException if some data is missing or if some loader specific error occurs
         */
        private void loadXYDT(final Section section, final BufferedReader reader, final String name)
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
                    mjdMin = FastMath.min(mjdMin, mjd);
                    mjdMax = FastMath.max(mjdMax, mjd);
                    if (firstMJD < 0) {
                        // store the first mjd parsed
                        firstMJD = mjd;
                    }

                    // get the entry at the same date if it was already parsed
                    final double[] eop;
                    if (eopFieldsMap.containsKey(mjd)) {
                        eop = eopFieldsMap.get(mjd);
                    } else {
                        eop = new double[4];
                        eopFieldsMap.put(mjd, eop);
                    }

                    if (eop[0] <= firstMJD) {
                        // either it is the first time we parse this date (eop[0] = 0),
                        // or the new parsed data is from a more recent file
                        // in both case, we should update the array
                        eop[0] = firstMJD;
                        eop[1] = Double.parseDouble(fields[4]);
                        eop[2] = Double.parseDouble(fields[5]);
                        eop[3] = Double.parseDouble(fields[6]);
                    }

                } else if (inValuesPart) {
                    // we leave values part
                    return;
                }
            }

            throw new OrekitException(OrekitMessages.UNEXPECTED_END_OF_FILE_AFTER_LINE,
                                      name, lineNumber);

        }

        /** Read EOP data.
         * @param section section to parse
         * @param isNonRotatingOrigin if true, the file contain Non-Rotating Origin nutation corrections
         * @param reader reader from where file content is obtained
         * @param name name of the file (or zip entry)
         * @exception IOException if data can't be read
         * @exception OrekitException if some data is missing or if some loader specific error occurs
         */
        private void loadPoleOffsets(final Section section, final boolean isNonRotatingOrigin,
                                     final BufferedReader reader, final String name)
            throws OrekitException, IOException {

            boolean inValuesPart = false;
            for (line = reader.readLine(); line != null; line = reader.readLine()) {
                lineNumber++;
                final String[] fields = section.getFields(line);
                if (fields != null) {

                    // we are within the values part
                    inValuesPart = true;

                    // this is a data line, build an entry from the extracted fields
                    final int mjd = Integer.parseInt(fields[0]);
                    mjdMin = FastMath.min(mjdMin, mjd);
                    mjdMax = FastMath.max(mjdMax, mjd);

                    // get the entry at the same date if it was already parsed
                    final double[] pole;
                    if (poleOffsetsFieldsMap.containsKey(mjd)) {
                        pole = poleOffsetsFieldsMap.get(mjd);
                    } else {
                        pole = new double[5];
                        poleOffsetsFieldsMap.put(mjd, pole);
                    }

                    if (pole[0] <= firstMJD) {
                        // either it is the first time we parse this date (pole[0] = 0),
                        // or the new parsed data is from a more recent file
                        // in both case, we should update the array
                        pole[0] = firstMJD;
                        if (isNonRotatingOrigin) {
                            pole[1] = Double.parseDouble(fields[1]);
                            pole[2] = Double.parseDouble(fields[2]);
                        } else {
                            pole[3] = Double.parseDouble(fields[1]);
                            pole[4] = Double.parseDouble(fields[2]);
                        }
                    }

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
