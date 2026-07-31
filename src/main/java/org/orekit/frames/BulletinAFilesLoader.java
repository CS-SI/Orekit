/* Copyright 2002-2026 CS GROUP
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hipparchus.util.FastMath;
import org.hipparchus.util.RomanNumeral;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.Month;
import org.orekit.time.TimeScale;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.IERSConventions.NutationCorrectionConverter;
import org.orekit.utils.units.UnitsConverter;

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
 * so the average number of entries is 7 as the files are published on a weekly
 * basis.
 * </p>
 * <p>
 * There are no prediction data for pole offsets.
 * </p>
 * <p>
 * This loader reads both the rapid service, the prediction and the final
 * values parts. As successive files have overlaps between all these sections,
 * values extracted from latest files (according to the publication dates from header)
 * override values extracted from earlier files, regardless of the files
 * reading order. If numerous bulletins A covering more than one year are read,
 * one particular date will typically appear:
 * </p>
 * <ul>
 *     <li>in the prediction section of 52 or 53 files,</li>
 *     <li>then in the rapid data section of one file</li>
 *     <li>then it will be missing in a few files,</li>
 *     <li>and finally it will appear in the final values sections of a last file.</li>
 * </ul>
 * <p>
 * In this case, the value retained will be the one extracted from the
 * final values section in the last published file.
 * </p>
 * <p>
 * If only one bulletin A file is read and if it corresponds to the first bulletin
 * of a month, it will have a roughly one month wide hole between the
 * final data and the rapid data. This hole will trigger an error as EOP
 * continuity is checked by default for at most 5 days holes. In this case,
 * users should call something like {@link FramesFactory#setEOPContinuityThreshold(double)
 * FramesFactory.setEOPContinuityThreshold(Constants.JULIAN_YEAR)} to prevent
 * the error to be triggered.
 * </p>
 * <p>
 * The bulletin A files are recognized thanks to their base names, which match the pattern
 * {@code bulletina-xxx-###.txt}, (or the same ending with {@code .gz} for gzip-compressed
 * files). In this pattern, xxx is a roman numeral corresponding to the year minus 1987,
 * and ### is a decimal number corresponding to the number of the week.
 * </p>
 * <p>
 * The week number is generally the ISO week number (i.e. week 1 is the week that contains the
 * first Thursday of the year). This was not the case in 2009, as the bulletin A for week 1
 * was published on Thursday, January 8th (probably because Thursday, January 1st was holidays).
 * So for whole 2009 year, the week number was ISO week minus one. This discrepancy was not
 * reproduced for years 2015 and 2026, for both years the bulletin A for week 1 was published
 * on Thursday, January 1st.
 * </p>
 * <p>
 * Bulletin A in csv format must be read using {@link EopCsvFilesLoader} rather
 * than using this loader. Bulletin A in xml format must be read using {@link EopXmlLoader}
 * rather than using this loader.
 * </p>
 * <p>
 * This class is immutable and hence thread-safe
 * </p>
 * @author Luc Maisonobe
 * @since 7.0
 * @see EopCsvFilesLoader
 * @see EopXmlLoader
 */
class BulletinAFilesLoader extends AbstractEopLoader implements EopHistoryLoader {

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

    /** Regular expression matching a string field to store. */
    private static final String STORED_STRING_FIELD   = "\\p{Blank}+(\\p{Alpha}+)";

    /** Enum for files sections, in expected order.
     * <p>The bulletin A weekly data files contain several sections,
     * each introduced with some fixed header text and followed by tabular data.
     * </p>
     */
    private enum Section {

        /** Publication metadata. */
        //      **********************************************************************
        //      *                                                                    *
        //      *                   I E R S   B U L L E T I N - A                    *
        //      *                                                                    *
        //      *           Rapid Service/Prediction of Earth Orientation            *
        //      **********************************************************************
        //      5 January 2006                                        Vol. XIX No. 001
        //      ______________________________________________________________________
        PUBLICATION_METADATA("^ *\\* *I E R S   B U L L E T I N - A *\\* *$",
                             LINE_START_REGEXP +
                             STORED_INTEGER_FIELD + STORED_STRING_FIELD + STORED_INTEGER_FIELD +
                             "\\p{Blank}*Vol\\." + STORED_STRING_FIELD +
                             "\\p{Blank}*No\\." + STORED_INTEGER_FIELD +
                             LINE_END_REGEXP),

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
        POLE_OFFSETS_IAU_1980_FINAL_VALUES("^ *IERS Celestial Pole Offset Final Series *$",
                                           LINE_START_REGEXP +
                                           STORED_MJD_FIELD +
                                           STORED_REAL_FIELD +
                                           STORED_REAL_FIELD +
                                           LINE_END_REGEXP),

        /** Pole offsets, IAU-2000. */
        // the format for the IAU-2000 series is similar, but the meanings of the fields
        // are different
        //                       IAU2000A Celestial Pole Offset Series
        //                        MJD      dX     error     dY     error
        //                                      (msec. of arc)
        //                       56519   -0.246   0.052   -0.223   0.080
        //                       56520   -0.239   0.052   -0.248   0.080
        //                       56521   -0.224   0.076   -0.277   0.110
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

    }

    /** Build a loader for IERS bulletins A files.
     * @param supportedNames regular expression for supported files names
     * @param manager provides access to the bulletin A files.
     * @param utcSupplier UTC time scale.
     */
    BulletinAFilesLoader(final String supportedNames,
                         final DataProvidersManager manager,
                         final Supplier<TimeScale> utcSupplier) {
        super(supportedNames, manager, utcSupplier);
    }

    /** {@inheritDoc} */
    public void fillHistory(final IERSConventions.NutationCorrectionConverter converter,
                            final Collection<EOPEntry> history) {
        final ItrfVersionProvider itrfVersionProvider =
            new ITRFVersionLoader(ITRFVersionLoader.SUPPORTED_NAMES, getDataProvidersManager());
        final Parser parser = new Parser(converter, itrfVersionProvider, getUtc());
        final EopParserLoader loader = new EopParserLoader(parser);
        this.feed(loader);

        history.addAll(loader.getEop());

    }

    /** Internal class performing the parsing. */
    static class Parser extends AbstractEopParser {

        /** File name. */
        private String fileName;

        /** Current line number. */
        private int lineNumber;

        /** Current line. */
        private String line;

        /** Publication date.
         * @since 14.0
         */
        private DateComponents publicationDate;

        /** Earliest parsed data. */
        private int mjdMin;

        /** Latest parsed data. */
        private int mjdMax;

        /** Simple constructor.
         * @param converter           converter to use
         * @param itrfVersionProvider to use for determining the ITRF version of the EOP.
         * @param utc                 time scale for parsing dates.
         * @since 14.0
         */
        Parser(final NutationCorrectionConverter converter,
               final ItrfVersionProvider itrfVersionProvider,
               final TimeScale utc) {
            super(converter, itrfVersionProvider, utc);
            this.lineNumber      = 0;
            this.publicationDate = DateComponents.MODIFIED_JULIAN_EPOCH;
            this.mjdMin          = Integer.MAX_VALUE;
            this.mjdMax          = Integer.MIN_VALUE;
        }

        /** {@inheritDoc} */
        @Override
        public Collection<EOPEntry> parse(final DataSource source)
            throws IOException {

            // create a new list for entries parsed from this file
            final List<EOPEntry> eop = new ArrayList<>();

            this.fileName      = source.getName();

            // set up a reader for line-oriented bulletin A files
            try (BufferedReader reader = new BufferedReader(source.getOpener().openReaderOnce())) {
                lineNumber =  0;

                // loop over sections
                final List<Section> remaining = new ArrayList<>(Arrays.asList(Section.values()));
                for (Section section = nextSection(remaining, reader);
                     section != null;
                     section = nextSection(remaining, reader)) {

                    final EopDataType eopDataType = switch (section) {
                        case PUBLICATION_METADATA -> null;
                        case EOP_FINAL_VALUES, POLE_OFFSETS_IAU_1980_FINAL_VALUES, POLE_OFFSETS_IAU_2000_FINAL_VALUES -> EopDataType.FINAL;
                        case EOP_RAPID_SERVICE, POLE_OFFSETS_IAU_1980_RAPID_SERVICE,
                             POLE_OFFSETS_IAU_2000_RAPID_SERVICE -> EopDataType.RAPID;
                        case EOP_PREDICTION -> EopDataType.PREDICTED;
                    };

                    switch (section) {
                        case PUBLICATION_METADATA:
                            loadMetadata(section, reader);
                            break;
                        case EOP_RAPID_SERVICE :
                        case EOP_FINAL_VALUES  :
                        case EOP_PREDICTION    :
                            eop.addAll(loadXYDT(section, reader, eopDataType));
                            break;
                        case POLE_OFFSETS_IAU_1980_RAPID_SERVICE :
                        case POLE_OFFSETS_IAU_1980_FINAL_VALUES  :
                            eop.addAll(loadPoleOffsets(section, false, reader, eopDataType));
                            break;
                        case POLE_OFFSETS_IAU_2000_RAPID_SERVICE :
                        case POLE_OFFSETS_IAU_2000_FINAL_VALUES  :
                            eop.addAll(loadPoleOffsets(section, true, reader, eopDataType));
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
                    throw new OrekitException(OrekitMessages.NOT_A_SUPPORTED_IERS_DATA_FILE, source.getName());
                }

            }

            // return the parsed eop
            return eop;

        }

        /** Skip to next section header.
         * @param sections sections to check for
         * @param reader reader from where file content is obtained
         * @return the next section or null if no section is found until end of file
         * @exception IOException if data can't be read
         */
        private Section nextSection(final List<Section> sections,
                                    final BufferedReader reader)
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

        /** Read publication metadata.
         * @param section section to parse
         * @param reader reader from where file content is obtained
         * @exception IOException if data can't be read
         */
        private void loadMetadata(final Section section, final BufferedReader reader)
            throws IOException {
            for (line = reader.readLine(); line != null; line = reader.readLine()) {
                lineNumber++;
                final String[] fields = section.getFields(line);
                if (fields != null) {

                    // parse the date
                    publicationDate = new DateComponents(Integer.parseInt(fields[2]),
                                                         Month.parseMonth(fields[1]),
                                                         Integer.parseInt(fields[0]));

                    // check volume and week number
                    checkVolume(RomanNumeral.parse(fields[3].toUpperCase(Locale.ROOT)));
                    checkWeek(Integer.parseInt(fields[4]));

                    return;

                }
            }
        }

        /** Check the volume number against publication date.
         * @param parsed parsed volume
         */
        private void checkVolume(final int parsed) {
            final int computed = publicationDate.getYear() - 1987;
            if (computed != parsed) {
                throw new OrekitException(OrekitMessages.UNEXPECTED_DATA_AT_LINE_IN_FILE,
                                          lineNumber, fileName);
            }
        }

        /** Check the week number against publication date.
         * @param parsed parsed week
         */
        private void checkWeek(final int parsed) {

            final int computed = publicationDate.getCalendarWeek();

            if (computed != parsed) {

                // in 2009, January 1st was a Thursday, and the bulletin A numbered as week 1
                // was published on January 8th. Week numbers in bulletin A were therefore ISO
                // week number minus 1 throughout the 2009 year. This did not happen in 2015
                // or 2026 despite both years also started on a Thursday
                // we allow this special case here
                final boolean januaryFirstIsThrusday =
                    new DateComponents(publicationDate.getYear(), 1, 1).getDayOfWeek() == 4;
                if (januaryFirstIsThrusday && parsed == computed - 1) {
                    // we accept this sloppy week number
                    return;
                }

                throw new OrekitException(OrekitMessages.UNEXPECTED_DATA_AT_LINE_IN_FILE,
                                          lineNumber, fileName);

            }

        }

        /** Read X, Y, UT1-UTC.
         * @param section section to parse
         * @param reader reader from where file content is obtained
         * @param eopDataType EOP data type
         * @return parsed incomplete EOP
         * @exception IOException if data can't be read
         */
        private List<EOPEntry> loadXYDT(final Section section, final BufferedReader reader,
                                        final EopDataType eopDataType)
            throws IOException {

            final List<EOPEntry> eop = new ArrayList<>();

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
                                                  fileName, year, month, day, mjd);
                    }
                    mjdMin = FastMath.min(mjdMin, mjd);
                    mjdMax = FastMath.max(mjdMax, mjd);

                    eop.add(new EOPEntry(mjd,
                                         Double.parseDouble(fields[6]), Double.NaN,
                                         UnitsConverter.ARC_SECONDS_TO_RADIANS.convert(Double.parseDouble(fields[4])),
                                         UnitsConverter.ARC_SECONDS_TO_RADIANS.convert(Double.parseDouble(fields[5])),
                                         Double.NaN, Double.NaN,
                                         0.0, 0.0, 0.0, 0.0,
                                         getItrfVersionProvider().getConfiguration(fileName, mjd).getVersion(),
                                         AbsoluteDate.createMJDDate(mjd, 0, getUtc()), eopDataType,
                                         publicationDate.getMJD(), 0, 0));

                } else if (inValuesPart) {
                    // we leave the values part
                    return eop;
                }
            }

            throw new OrekitException(OrekitMessages.UNEXPECTED_END_OF_FILE_AFTER_LINE,
                                      fileName, lineNumber);

        }

        /** Read EOP data.
         * @param section section to parse
         * @param isNonRotatingOrigin if true, the file contain Non-Rotating Origin nutation corrections
         * @param reader reader from where file content is obtained
         * @param eopDataType EOP data type
         * @return parsed incomplete EOP
         * @exception IOException if data can't be read
         */
        private List<EOPEntry> loadPoleOffsets(final Section section, final boolean isNonRotatingOrigin,
                                               final BufferedReader reader, final EopDataType eopDataType)
            throws IOException {

            final List<EOPEntry> eop = new ArrayList<>();

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

                    if (isNonRotatingOrigin) {
                        eop.add(new EOPEntry(mjd,
                                             Double.NaN, Double.NaN,
                                             Double.NaN, Double.NaN,
                                             Double.NaN, Double.NaN,
                                             Double.NaN, Double.NaN,
                                             UnitsConverter.MILLI_ARC_SECONDS_TO_RADIANS.convert(Double.parseDouble(fields[1])),
                                             UnitsConverter.MILLI_ARC_SECONDS_TO_RADIANS.convert(Double.parseDouble(fields[2])),
                                             getItrfVersionProvider().getConfiguration(fileName, mjd).getVersion(),
                                             AbsoluteDate.createMJDDate(mjd, 0, getUtc()), eopDataType,
                                             0, 0, publicationDate.getMJD()));
                    } else {
                        eop.add(new EOPEntry(mjd,
                                             Double.NaN, Double.NaN,
                                             Double.NaN, Double.NaN,
                                             Double.NaN, Double.NaN,
                                             UnitsConverter.MILLI_ARC_SECONDS_TO_RADIANS.convert(Double.parseDouble(fields[1])),
                                             UnitsConverter.MILLI_ARC_SECONDS_TO_RADIANS.convert(Double.parseDouble(fields[2])),
                                             Double.NaN, Double.NaN,
                                             getItrfVersionProvider().getConfiguration(fileName, mjd).getVersion(),
                                             AbsoluteDate.createMJDDate(mjd, 0, getUtc()), eopDataType,
                                             0, publicationDate.getMJD(), 0));
                    }

                } else if (inValuesPart) {
                    // we leave the values part
                    return eop;
                }
            }

            throw new OrekitException(OrekitMessages.UNEXPECTED_END_OF_FILE_AFTER_LINE,
                                      fileName, lineNumber);

        }

    }

}
