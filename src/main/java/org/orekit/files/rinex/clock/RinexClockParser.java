/* Copyright 2002-2025 CS GROUP
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
package org.orekit.files.rinex.clock;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.util.FastMath;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.rinex.AppliedDCBS;
import org.orekit.files.rinex.AppliedPCVS;
import org.orekit.files.rinex.section.CommonLabel;
import org.orekit.files.rinex.utils.ParsingUtils;
import org.orekit.frames.Frame;
import org.orekit.gnss.IGSUtils;
import org.orekit.gnss.ObservationType;
import org.orekit.gnss.PredefinedObservationType;
import org.orekit.gnss.SatInSystem;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.gnss.TimeSystem;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScales;
import org.orekit.utils.units.Unit;

/** A parser for the clock file from the IGS.
 * This parser handles versions 2.0 to 3.04 of the RINEX clock files.
 * <p> It is able to manage some mistakes in file writing and format compliance such as wrong date format,
 * misplaced header blocks or missing information. </p>
 * <p> A time system should be specified in the file. However, if it is not, default time system will be chosen
 * regarding the satellite system. If it is mixed or not specified, default time system will be UTC. </p>
 * <p> Caution, files with missing information in header can lead to wrong data dates and station positions.
 * It is advised to check the correctness and format compliance of the clock file to be parsed. </p>
 * @see <a href="https://files.igs.org/pub/data/format/rinex_clock300.txt"> 3.00 clock file format</a>
 * @see <a href="https://files.igs.org/pub/data/format/rinex_clock302.txt"> 3.02 clock file format</a>
 * @see <a href="https://files.igs.org/pub/data/format/rinex_clock304.txt"> 3.04 clock file format</a>
 *
 * @author Thomas Paulet
 * @since 11.0
 */
public class RinexClockParser {

    /** Millimeter unit. */
    private static final Unit MILLIMETER = Unit.parse("mm");

    /** Spaces delimiters. */
    private static final String SPACES = "\\s+";

    /** Mapping from frame identifier in the file to a {@link Frame}. */
    private final Function<? super String, ? extends Frame> frameBuilder;

    /** Mapper from string to the observation type.
     * @since 13.0
     */
    private final Function<? super String, ? extends ObservationType> typeBuilder;

    /** Set of time scales. */
    private final TimeScales timeScales;

    /** Create a clock file parser using default values.
     * <p>
     * This constructor uses the {@link DataContext#getDefault() default data context}
     * and {@link IGSUtils#guessFrame(String)}, it recognizes only {@link
     * PredefinedObservationType} and {@link SatelliteSystem} with non-null
     * {@link SatelliteSystem#getObservationTimeScale() time scales} (i.e., neither
     * user-defined, nor {@link SatelliteSystem#SBAS}, nor {@link SatelliteSystem#MIXED}).
     * </p>
     * @see #RinexClockParser(Function, Function, TimeScales)
     */
    @DefaultDataContext
    public RinexClockParser() {
        this(IGSUtils::guessFrame,
             PredefinedObservationType::valueOf,
             DataContext.getDefault().getTimeScales());
    }

    /** Constructor, build the IGS clock file parser.
     * @param frameBuilder is a function that can construct a frame from a clock file
     *                     coordinate system string. The coordinate system can be
     *                     any 5 characters string e.g., ITR92, IGb08.
     * @param typeBuilder  mapper from string to the observation type
     * @param timeScales   the set of time scales used for parsing dates.
     * @since 14.0
     */
    public RinexClockParser(final Function<? super String, ? extends Frame> frameBuilder,
                            final Function<? super String, ? extends ObservationType> typeBuilder,
                            final TimeScales timeScales) {
        this.frameBuilder = frameBuilder;
        this.typeBuilder  = typeBuilder;
        this.timeScales   = timeScales;
    }

    /** Parse an IGS clock file from a {@link DataSource}.
     * @param source source for clock file
     * @return a parsed IGS clock file
     * @since 12.1
     */
    public RinexClock parse(final DataSource source) {

        Iterable<LineParser> candidateParsers = Collections.singleton(LineParser.VERSION);

        // initialize internal data structures
        final ParseInfo parseInfo = new ParseInfo(source.getName());

        try (Reader reader = source.getOpener().openReaderOnce();
             BufferedReader br = new BufferedReader(reader)) {
            ++parseInfo.lineNumber;
            nextLine:
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                for (final LineParser candidate : candidateParsers) {
                    if (candidate.canHandle.apply(parseInfo.file.getHeader(), line)) {
                        try {
                            candidate.parsingMethod.parse(line, parseInfo);
                            ++parseInfo.lineNumber;
                            candidateParsers = candidate.allowedNextProvider.apply(parseInfo);
                            continue nextLine;
                        } catch (StringIndexOutOfBoundsException | NumberFormatException | InputMismatchException e) {
                            throw new OrekitException(e, OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                      parseInfo.lineNumber, source.getName(), line);
                        }
                    }
                }

                // no parsers found for this line
                throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                          parseInfo.lineNumber, source.getName(), line);

            }

        } catch (IOException ioe) {
            throw new OrekitException(ioe, LocalizedCoreFormats.SIMPLE_MESSAGE, ioe.getLocalizedMessage());
        }

        return parseInfo.file;

    }

    /** Transient data used for parsing a clock file. */
    private class ParseInfo {

        /** Name of the data source. */
        private final String name;

        /** Mapping from frame identifier in the file to a {@link Frame}.
         * @since 14.0
         */
        private final Function<? super String, ? extends Frame> frameBuilder;

        /** Mapper from string to the observation type.
         * @since 14.0
         */
        private final Function<? super String, ? extends ObservationType> typeBuilder;

        /** Set of time scales for parsing dates.
         * @since 14.0
         */
        private final TimeScales timeScales;

        /** Current line number of the navigation message. */
        private int lineNumber;

        /** The corresponding clock file object. */
        private final RinexClock file;

        /** Current satellite system for observation type parsing. */
        private SatelliteSystem currentSatelliteSystem;

        /** Remaining number of observation types. */
        private int remainingObsTypes;

        /** Indicator for completed header. */
        private boolean headerCompleted;

        /** Current start date for reference clocks. */
        private AbsoluteDate referenceClockStartDate;

        /** Current end date for reference clocks. */
        private AbsoluteDate referenceClockEndDate;

        /** Pending reference clocks list. */
        private List<ReferenceClock> pendingReferenceClocks;

        /** Current clock data type. */
        private ClockDataType currentDataType;

        /** Current receiver/satellite name. */
        private String currentName;

        /** Date if the clock data line. */
        private AbsoluteDate date;

        /** Total number of values to parse. */
        private int totalValues;

        /** Index of next value to parse. */
        private int valueIndex;

        /** Current data values. */
        private final double[] values;

        /** Constructor, build the ParseInfo object.
         * @param name name of the data source
         */
        ParseInfo(final String name) {
            this.name                   = name;
            this.frameBuilder           = RinexClockParser.this.frameBuilder;
            this.typeBuilder            = RinexClockParser.this.typeBuilder;
            this.timeScales             = RinexClockParser.this.timeScales;
            this.file                   = new RinexClock();
            this.lineNumber             = 0;
            this.pendingReferenceClocks = new ArrayList<>();
            this.values                 = new double[6];
        }

    }


    /** Parsers for specific lines. */
    private enum LineParser {

        /** Parser for version, file type and satellite system. */
        VERSION((header, line) -> header.matchFound(CommonLabel.VERSION, line),
                (line, parseInfo) ->  {
                    final RinexClockHeader header = parseInfo.file.getHeader();
                    header.parseVersionFileTypeSatelliteSystem(line, null, parseInfo.name,
                                                               2.00, 3.00, 3.01, 3.02, 3.04);
                    if (header.getFormatVersion() < 3.0) {
                        // before 3.0, only GPS system was used
                        header.setSatelliteSystem(SatelliteSystem.GPS);
                        header.setTimeSystem(TimeSystem.GPS);
                        header.setTimeScale(TimeSystem.GPS.getTimeScale(parseInfo.timeScales));
                    }
                },
                LineParser::headerNext),

        /** Parser for generating program and emiting agency. */
        PROGRAM((header, line) -> header.matchFound(CommonLabel.PROGRAM, line),
                (line, parseInfo) -> parseInfo.file.getHeader().parseProgramRunByDate(line, parseInfo.timeScales),
                LineParser::headerNext),

        /** Parser for comments. */
        COMMENT((header, line) -> header.matchFound(CommonLabel.COMMENT, line),
                (line, parseInfo) -> ParsingUtils.parseComment(parseInfo.lineNumber, line, parseInfo.file),
                LineParser::commentNext),

        /** Parser for satellite system and related observation types. */
        SYS_NB_TYPES_OF_OBSERV((header, line) -> header.matchFound(CommonLabel.SYS_NB_TYPES_OF_OBSERV, line),
                               (line, parseInfo) -> {
                                   final RinexClockHeader header = parseInfo.file.getHeader();
                                   if (parseInfo.remainingObsTypes == 0) {
                                       // we are starting a new satellite system
                                       parseInfo.currentSatelliteSystem =
                                           SatelliteSystem.parseSatelliteSystem(ParsingUtils.parseString(line, 0, 1));
                                       parseInfo.remainingObsTypes = ParsingUtils.parseInt(line, 3, 3);
                                   }
                                   for (int i = 0; i < 14 && parseInfo.remainingObsTypes > 0; ++i) {
                                       parseInfo.remainingObsTypes--;
                                       final String obsType = ParsingUtils.parseString(line, 8 + 4 * i, 3);
                                       header.addSystemObservationType(parseInfo.currentSatelliteSystem,
                                                                       parseInfo.typeBuilder.apply(obsType));
                                   }
                               },
                               LineParser::sysObsTypesNext),

        /** Parser for time system identifier. */
        TIME_SYSTEM_ID((header, line) -> header.matchFound(ClockLabel.TIME_SYSTEM_ID, line),
                       (line, parseInfo) -> {
                           final RinexClockHeader header = parseInfo.file.getHeader();
                           final TimeSystem timeSystem = TimeSystem.parseTimeSystem(ParsingUtils.parseString(line, 3, 3));
                           header.setTimeSystem(timeSystem);
                           header.setTimeScale(timeSystem.getTimeScale(parseInfo.timeScales));
                       },
                       LineParser::headerNext),

        /** Parser for leap seconds. */
        LEAP_SECONDS((header, line) -> header.matchFound(CommonLabel.LEAP_SECONDS, line),
                     ((line, parseInfo) -> parseInfo.file.getHeader().
                         setLeapSeconds(ParsingUtils.parseInt(line, 0, 6))),
                     LineParser::headerNext),

        /** Parser for leap seconds GNSS. */
        LEAP_SECONDS_GNSS((header, line) -> header.matchFound(ClockLabel.LEAP_SECONDS_GNSS, line),
                     ((line, parseInfo) -> parseInfo.file.getHeader().
                         setLeapSecondsGNSS(ParsingUtils.parseInt(line, 0, 6))),
                     LineParser::headerNext),

        /** Parser for differential code bias corrections. */
        SYS_DCBS_APPLIED((header, line) -> header.matchFound(CommonLabel.SYS_DCBS_APPLIED, line),
                         (line, parseInfo) -> {
                                 final RinexClockHeader header = parseInfo.file.getHeader();
                                 final SatelliteSystem satelliteSystem =
                                     SatelliteSystem.parseSatelliteSystem(ParsingUtils.parseString(line, 0, 1),
                                                                          header.getSatelliteSystem());
                                 header.addAppliedDCBS(new AppliedDCBS(satelliteSystem,
                                                                       ParsingUtils.parseString(line, 3, 17),
                                                                       ParsingUtils.parseString(line,
                                                                                                header.isBefore304() ? 20 : 22,
                                                                                                header.isBefore304() ? 40 : 43)));
                         },
                         LineParser::headerNext),

        /** Parser for phase center variations corrections. */
        SYS_PCVS_APPLIED((header, line) -> header.matchFound(CommonLabel.SYS_PCVS_APPLIED, line),
                         (line, parseInfo) -> {
                                 final RinexClockHeader header = parseInfo.file.getHeader();
                                 final SatelliteSystem satelliteSystem =
                                     SatelliteSystem.parseSatelliteSystem(ParsingUtils.parseString(line, 0, 1),
                                                                          header.getSatelliteSystem());
                                 header.addAppliedPCVS(new AppliedPCVS(satelliteSystem,
                                                                       ParsingUtils.parseString(line, 3, 17),
                                                                       ParsingUtils.parseString(line,
                                                                                                header.isBefore304() ? 20 : 22,
                                                                                                header.isBefore304() ? 40 : 43)));
                         },
                         LineParser::headerNext),

        /** Parser for the different clock data types that are stored in the file. */
        NB_TYPES_OF_DATA((header, line) -> header.matchFound(ClockLabel.NB_TYPES_OF_DATA, line),
                         (line, parseInfo) -> {
                             final int n = ParsingUtils.parseInt(line, 0, 6);
                             if (n < 1) {
                                 throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                           parseInfo.lineNumber, parseInfo.name, line);
                             }
                             for (int i = 0; i < n; i++) {
                                 final String type = ParsingUtils.parseString(line, 10 + i * 6, 4);
                                 try {
                                     parseInfo.file.getHeader().addClockDataType(ClockDataType.valueOf(type));
                                 } catch (IllegalArgumentException iae) {
                                     throw new OrekitException(OrekitMessages.UNKNOWN_CLOCK_DATA_TYPE, type);
                                 }
                             }
                         },
                         LineParser::headerNext),

        /** Parser for the station with reference clock. */
        STATION_NAME_NUM((header, line) -> header.matchFound(ClockLabel.STATION_NAME_NUM, line),
                         (line, parseInfo) -> {
                             // we use a Scanner here instead of relying on the character indices
                             // because some files do NOT respect the format
                             // the reference example in table A18 of the rinex 3.04 specification itself exhibits
                             // errors (the DOMES number field is shifted 5 characters to the left wrt. the
                             // specification, i.e. this line in table A18 is consistent with the rinex clock 3.02
                             // specification, not with the rinex clock 3.04 specification…).
                             // We fall back to a Scanner, which should handle all format versions properly
                             final RinexClockHeader header = parseInfo.file.getHeader();
                                 try (Scanner s1      = new Scanner(line.substring(0, header.getLabelIndex()));
                                      Scanner s2      = s1.useDelimiter(SPACES);
                                      Scanner scanner = s2.useLocale(Locale.US)) {
                                 header.setStationName(scanner.next());
                                 header.setStationIdentifier(scanner.next());
                             }
                         },
                         LineParser::headerNext),

        /** Parser for the reference clock in case of calibration data. */
        STATION_CLK_REF((header, line) -> header.matchFound(ClockLabel.STATION_CLK_REF, line),
                        (line, parseInfo) ->  {
                            final RinexClockHeader header = parseInfo.file.getHeader();
                            if (header.isBefore304()) {
                                header.setExternalClockReference(line.substring(0, 60).trim());
                            } else {
                                header.setExternalClockReference(line.substring(0, 65).trim());
                            }
                        },
                        LineParser::headerNext),

        /** Parser for the analysis center. */
        ANALYSIS_CENTER((header, line) -> header.matchFound(ClockLabel.ANALYSIS_CENTER, line),
                        (line, parseInfo) -> {
                            final RinexClockHeader header = parseInfo.file.getHeader();

                            // First element is IGS AC designator
                            header.setAnalysisCenterID(ParsingUtils.parseString(line, 0, 3));

                            // Then, the full name of the analysis center
                            if (header.isBefore304()) {
                                header.setAnalysisCenterName(ParsingUtils.parseString(line, 5, 55));
                            } else {
                                header.setAnalysisCenterName(ParsingUtils.parseString(line, 5, 60));
                            }
                        },
                        LineParser::headerNext),

        /** Parser for the number of reference clocks over a period. */
        NB_OF_CLK_REF((header, line) -> header.matchFound(ClockLabel.NB_OF_CLK_REF, line),
                      (line, parseInfo) -> {
                          final RinexClockHeader header = parseInfo.file.getHeader();
                          if (!parseInfo.pendingReferenceClocks.isEmpty()) {
                              // Modify time span map of the reference clocks to accept the pending reference clock
                              header.addReferenceClockList(parseInfo.pendingReferenceClocks,
                                                           parseInfo.referenceClockStartDate);
                              parseInfo.pendingReferenceClocks = new ArrayList<>();
                          }

                          final String startStop = line.substring(7, header.getLabelIndex()).trim();
                          if (startStop.isEmpty()) {
                              // no start/stop epoch the record applies to the whole file
                              parseInfo.referenceClockStartDate = AbsoluteDate.PAST_INFINITY;
                              parseInfo.referenceClockEndDate   = AbsoluteDate.FUTURE_INFINITY;
                          } else {
                              // we use a Scanner here instead of relying on the character indices
                              // because some files do NOT respect the format
                              // the reference example in table A17 of the rinex 3.04 specification itself exhibits
                              // errors (the seconds field in start date is shifted 1 character to the left wrt.
                              // the specification, the first few fields in end date are shifted 2 characters to the
                              // left wrt. the specification, and the seconds field in end date is shifted 3 characters
                              // to the left wrt. specification, i.e. this line in table A17 is consistent with
                              // the rinex clock 3.02 specification, not with the rinex clock 3.04 specification…).
                              // we were not able to find any real 3.04 files that have non-empty dates on clock ref lines.
                              // We fall back to a Scanner, which should handle all format versions properly
                              try (Scanner s1      = new Scanner(startStop);
                                   Scanner s2      = s1.useDelimiter(SPACES);
                                   Scanner scanner = s2.useLocale(Locale.US)) {
                                  final TimeScale ts = header.getFormatVersion() < 3.02 ?
                                                       parseInfo.timeScales.getGPS() : header.getTimeScale();
                                  parseInfo.referenceClockStartDate =
                                      new AbsoluteDate(scanner.nextInt(), scanner.nextInt(), scanner.nextInt(),
                                                       scanner.nextInt(), scanner.nextInt(), scanner.nextDouble(),
                                                       ts);
                                  parseInfo.referenceClockEndDate =
                                      new AbsoluteDate(scanner.nextInt(), scanner.nextInt(), scanner.nextInt(),
                                                       scanner.nextInt(), scanner.nextInt(), scanner.nextDouble(),
                                                       ts);
                              }
                          }
                      },
                      LineParser::headerNext),

        /** Parser for the reference clock over a period. */
        ANALYSIS_CLK_REF((header, line) -> header.matchFound(ClockLabel.ANALYSIS_CLK_REF, line),
                         (line, parseInfo) -> {

                             // First element is the name of the receiver/satellite embedding the reference clock
                             final int length = parseInfo.file.getHeader().isBefore304() ? 4 : 9;
                             final String referenceName = ParsingUtils.parseString(line, 0, length);

                             // Second element is the reference clock ID
                             final String clockID = ParsingUtils.parseString(line, length + 1, 20);

                             // Optionally, third element is an a priori clock constraint, by default equal to zero
                             double clockConstraint = ParsingUtils.parseDouble(line, length + 36, 19);
                             if (Double.isNaN(clockConstraint)) {
                                 clockConstraint = 0.0;
                             }

                             // Add reference clock to current reference clock list
                             final ReferenceClock referenceClock =
                                 new ReferenceClock(referenceName, clockID, clockConstraint,
                                                    parseInfo.referenceClockStartDate,
                                                    parseInfo.referenceClockEndDate);
                             parseInfo.pendingReferenceClocks.add(referenceClock);
                         },
                         LineParser::headerNext),

        /** Parser for the number of stations embedded in the file and the related frame. */
        NB_OF_SOLN_STA_TRF((header, line) -> header.matchFound(ClockLabel.NB_OF_SOLN_STA_TRF, line),
                           (line, parseInfo) -> {
                               final RinexClockHeader header = parseInfo.file.getHeader();
                               final String complete = ParsingUtils.parseString(line, 10, header.isBefore304() ? 50 : 55);
                               int first = 0;
                               while (first < complete.length() && complete.charAt(first) == ' ') {
                                   ++first;
                               }
                               int last = first;
                               while (last < complete.length() &&
                                      Character.isLetterOrDigit(complete.charAt(last))) {
                                   ++last;
                               }
                               final String frameName = complete.substring(first, last);
                               header.setFrameName(frameName);
                               header.setFrame(parseInfo.frameBuilder.apply(frameName));
                           },
                           LineParser::headerNext),

        /** Parser for the stations embedded in the file and the related positions. */
        SOLN_STA_NAME_NUM((header, line) -> header.matchFound(ClockLabel.SOLN_STA_NAME_NUM, line),
                          (line, parseInfo) -> {
                                 final int    length     = parseInfo.file.getHeader().isBefore304() ? 4 : 9;
                                 final String designator = ParsingUtils.parseString(line, 0, length);
                                 final String identifier = ParsingUtils.parseString(line, length + 1, 20);
                                 final double x          = MILLIMETER.toSI(ParsingUtils.parseLong(line, length + 21, 11));
                                 final double y          = MILLIMETER.toSI(ParsingUtils.parseLong(line, length + 33, 11));
                                 final double z          = MILLIMETER.toSI(ParsingUtils.parseLong(line, length + 45, 11));
                                 final Receiver receiver = new Receiver(designator, identifier, x, y, z);
                                 parseInfo.file.getHeader().addReceiver(receiver);
                             },
                          LineParser::headerNext),

        /** Parser for the number of satellites embedded in the file. */
        NB_OF_SOLN_SATS((header, line) -> header.matchFound(ClockLabel.NB_OF_SOLN_SATS, line),
                        (line, parseInfo) -> {}, // we ignore this record
                        LineParser::headerNext),

        /** Parser for the satellites embedded in the file. */
        PRN_LIST((header, line) -> header.matchFound(ClockLabel.PRN_LIST, line),
                 (line, parseInfo) -> {
                     final RinexClockHeader header = parseInfo.file.getHeader();
                     final int nMax = header.isBefore304() ? 15 : 16;
                     for (int i = 0; i < nMax; ++i) {
                         final String prn = ParsingUtils.parseString(line, 4 * i, 3);
                         if (prn.isEmpty()) {
                             break;
                         } else {
                             header.addSatellite(new SatInSystem(prn));
                         }
                     }
                 },
                 LineParser::headerNext),

        /** Parser for the end of header. */
        HEADER_END((header, line) -> header.matchFound(CommonLabel.END, line),
                   (line, parseInfo) -> {
                       final RinexClockHeader header = parseInfo.file.getHeader();
                       if (!parseInfo.pendingReferenceClocks.isEmpty()) {
                           // Modify time span map of the reference clocks to accept the pending reference clock
                           header.addReferenceClockList(parseInfo.pendingReferenceClocks, parseInfo.referenceClockStartDate);
                       }
                       if (header.getTimeSystem() == null) {
                           if (header.getMergedSystem() == null ||
                               header.getMergedSystem() == SatelliteSystem.MIXED) {
                               if (header.getFormatVersion() >= 3.0) {
                                   throw new OrekitException(OrekitMessages.MISSING_TIME_SYSTEM_DEFINITION, parseInfo.name);
                               }
                           } else {
                               // we force the systems using the satellite list
                               header.setTimeSystem(TimeSystem.parseOneLetterCode(String.valueOf(header.getMergedSystem().getKey())));
                               header.setTimeScale(header.getTimeSystem().getTimeScale(parseInfo.timeScales));
                           }
                       }
                       parseInfo.headerCompleted = true;
                   },
                   LineParser::headerEndNext),

        /** Parser for a clock data line. */
        CLOCK_DATA((header, line) -> line.charAt(0) != ' ',
                   (line, parseInfo) -> {

                       final RinexClockHeader header = parseInfo.file.getHeader();
                       try {
                           parseInfo.currentDataType = ClockDataType.valueOf(line.substring(0, 2));
                        } catch (IllegalArgumentException iae) {
                           throw new OrekitException(OrekitMessages.UNKNOWN_CLOCK_DATA_TYPE, line.substring(0, 2));
                       }

                       // Second element is receiver/satellite name
                       final int length = header.isBefore304() ? 4 : 9;
                       parseInfo.currentName = ParsingUtils.parseString(line, 3, length);

                       // Third element is data epoch
                       final int startI = header.isBefore304() ?  8 : 13;
                       final int startD = header.isBefore304() ? 24 : 29;
                       parseInfo.date =
                               new AbsoluteDate(ParsingUtils.parseInt(line, startI, 4),
                                                ParsingUtils.parseInt(line, startI +  4, 4),
                                                ParsingUtils.parseInt(line, startI +  7, 3),
                                                ParsingUtils.parseInt(line, startI + 10, 3),
                                                ParsingUtils.parseInt(line, startI + 13, 3),
                                                ParsingUtils.parseDouble(line, startD, 10),
                                                header.getTimeScale());

                       // Fourth element is number of data values
                       parseInfo.totalValues = ParsingUtils.parseInt(line, startD + 11, 2);
                       parseInfo.valueIndex  = 0;

                       // Get the values in this line
                       Arrays.fill(parseInfo.values, 0.0);
                       int start = header.isBefore304() ?  40 : 45;
                       while (parseInfo.valueIndex < FastMath.min(2, parseInfo.totalValues)) {
                           parseInfo.values[parseInfo.valueIndex++] = ParsingUtils.parseDouble(line, start, 19);
                           start += header.isBefore304() ? 20 : 21;
                       }

                       // Check if continuation line is required
                       if (parseInfo.valueIndex == parseInfo.totalValues) {
                           // No continuation line is required
                           parseInfo.file.addClockData(parseInfo.currentName,
                                                       new ClockDataLine(parseInfo.currentDataType,
                                                                         parseInfo.currentName,
                                                                         parseInfo.date,
                                                                         parseInfo.totalValues,
                                                                         parseInfo.values[0],
                                                                         parseInfo.values[1],
                                                                         parseInfo.values[2],
                                                                         parseInfo.values[3],
                                                                         parseInfo.values[4],
                                                                         parseInfo.values[5]));
                       }
                   },
                   LineParser::dataNext),

        /** Parser for a continuation clock data line. */
        CLOCK_DATA_CONTINUATION((header, line) -> true,
                                (line, parseInfo) -> {
                                    final RinexClockHeader header = parseInfo.file.getHeader();
                                    int start = header.isBefore304() ? 0 : 3;
                                    while (parseInfo.valueIndex < parseInfo.totalValues) {
                                        parseInfo.values[parseInfo.valueIndex++] =
                                            ParsingUtils.parseDouble(line, start, 19);
                                        start += header.isBefore304() ? 20 : 21;
                                    }
                                    parseInfo.file.addClockData(parseInfo.currentName,
                                                                new ClockDataLine(parseInfo.currentDataType,
                                                                                  parseInfo.currentName, parseInfo.date,
                                                                                  parseInfo.totalValues,
                                                                                  parseInfo.values[0],
                                                                                  parseInfo.values[1],
                                                                                  parseInfo.values[2],
                                                                                  parseInfo.values[3],
                                                                                  parseInfo.values[4],
                                                                                  parseInfo.values[5]));
                                },
                                LineParser::dataNext);

        /** Predicate for identifying lines that can be parsed. */
        private final BiFunction<RinexClockHeader, String, Boolean> canHandle;

        /** Parsing method. */
        private final ParsingMethod parsingMethod;

        /** Provider for next line parsers. */
        private final Function<ParseInfo, Iterable<LineParser>> allowedNextProvider;

        /** Simple constructor.
         * @param canHandle predicate for identifying lines that can be parsed
         * @param parsingMethod parsing method
         * @param allowedNextProvider supplier for allowed parsers for next line
         */
        LineParser(final BiFunction<RinexClockHeader, String, Boolean> canHandle,
                   final ParsingMethod parsingMethod,
                   final Function<ParseInfo, Iterable<LineParser>> allowedNextProvider) {
            this.canHandle           = canHandle;
            this.parsingMethod       = parsingMethod;
            this.allowedNextProvider = allowedNextProvider;
        }

        /** Get the allowed parsers for next lines while parsing Rinex header.
         * @param parseInfo holder for transient data
         * @return allowed parsers for next line
         */
        private static Iterable<LineParser> headerNext(final ParseInfo parseInfo) {
            if (parseInfo.file.getHeader().isBefore304()) {
                return Arrays.asList(PROGRAM, COMMENT, SYS_NB_TYPES_OF_OBSERV, TIME_SYSTEM_ID,
                                     LEAP_SECONDS, SYS_DCBS_APPLIED, SYS_PCVS_APPLIED,
                                     NB_TYPES_OF_DATA, STATION_NAME_NUM, STATION_CLK_REF,
                                     ANALYSIS_CENTER, NB_OF_CLK_REF, ANALYSIS_CLK_REF,
                                     NB_OF_SOLN_STA_TRF, SOLN_STA_NAME_NUM, NB_OF_SOLN_SATS,
                                     PRN_LIST, HEADER_END);
            } else {
                return Arrays.asList(PROGRAM, COMMENT, SYS_NB_TYPES_OF_OBSERV, TIME_SYSTEM_ID,
                                     LEAP_SECONDS, LEAP_SECONDS_GNSS, SYS_DCBS_APPLIED, SYS_PCVS_APPLIED,
                                     NB_TYPES_OF_DATA, STATION_NAME_NUM, STATION_CLK_REF,
                                     ANALYSIS_CENTER, NB_OF_CLK_REF, ANALYSIS_CLK_REF,
                                     NB_OF_SOLN_STA_TRF, SOLN_STA_NAME_NUM, NB_OF_SOLN_SATS,
                                     PRN_LIST, HEADER_END);
            }
        }

        /** Get the allowed parsers for next lines while parsing comments.
         * @param parseInfo holder for transient data
         * @return allowed parsers for next line
         */
        private static Iterable<LineParser> commentNext(final ParseInfo parseInfo) {
            return parseInfo.headerCompleted ? headerEndNext(parseInfo) : headerNext(parseInfo);
        }

        /** Get the allowed parsers for next lines while parsing types of observations.
         * @param parseInfo holder for transient data
         * @return allowed parsers for next line
         */
        private static Iterable<LineParser> sysObsTypesNext(final ParseInfo parseInfo) {
            return parseInfo.remainingObsTypes > 0 ?
                   Collections.singletonList(SYS_NB_TYPES_OF_OBSERV) :
                   headerNext(parseInfo);
        }


        /** Get the allowed parsers for next lines while parsing header end.
         * @param parseInfo holder for transient data
         * @return allowed parsers for next line
         */
        private static Iterable<LineParser> headerEndNext(final ParseInfo parseInfo) {
            return Collections.singleton(CLOCK_DATA);
        }

        /** Get the allowed parsers for next lines while parsing data.
         * @param parseInfo holder for transient data
         * @return allowed parsers for next line
         */
        private static Iterable<LineParser> dataNext(final ParseInfo parseInfo) {
            return parseInfo.valueIndex < parseInfo.totalValues ?
                   Collections.singleton(LineParser.CLOCK_DATA_CONTINUATION) :
                   Collections.singleton(LineParser.CLOCK_DATA);
        }

    }

    /** Parsing method. */
    @FunctionalInterface
    private interface ParsingMethod {
        /** Parse a line.
         * @param line line to parse
         * @param parseInfo holder for transient data
         */
        void parse(String line, ParseInfo parseInfo);
    }

}
