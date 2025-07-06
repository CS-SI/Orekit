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
import java.util.function.BiFunction;
import java.util.function.Function;

import org.hipparchus.exception.LocalizedCoreFormats;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.rinex.AppliedDCBS;
import org.orekit.files.rinex.AppliedPCVS;
import org.orekit.files.rinex.section.CommonLabel;
import org.orekit.files.rinex.utils.parsing.RinexUtils;
import org.orekit.frames.Frame;
import org.orekit.gnss.IGSUtils;
import org.orekit.gnss.ObservationType;
import org.orekit.gnss.PredefinedObservationType;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.gnss.TimeSystem;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
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

    /** Mapping from frame identifier in the file to a {@link Frame}. */
    private final Function<? super String, ? extends Frame> frameBuilder;

    /** Mapper from string to the observation type.
     * @since 13.0
     */
    private final Function<? super String, ? extends ObservationType> typeBuilder;

    /** Mapper from satellite system to time scales.
     * @since 14.0
     */
    private final BiFunction<SatelliteSystem, TimeScales, ? extends TimeScale> timeScaleBuilder;

    /** Set of time scales. */
    private final TimeScales timeScales;

    /** Create a clock file parser using default values.
     * <p>
     * This constructor uses the {@link DataContext#getDefault() default data context}
     * and {@link IGSUtils#guessFrame(String)}, it recognizes only {@link
     * PredefinedObservationType} and {@link SatelliteSystem}bwith non-null
     * {@link SatelliteSystem#getObservationTimeScale() time scales} (i.e., neither
     * user-defined, nor {@link SatelliteSystem#SBAS}, nor {@link SatelliteSystem#MIXED}).
     * </p>
     * @see #RinexClockParser(Function, Function, BiFunction, TimeScales)
     */
    @DefaultDataContext
    public RinexClockParser() {
        this(IGSUtils::guessFrame,
             PredefinedObservationType::valueOf,
             (system, ts) -> system.getObservationTimeScale() == null ?
                             null :
                             system.getObservationTimeScale().getTimeScale(ts),
             DataContext.getDefault().getTimeScales());
    }

    /** Constructor, build the IGS clock file parser.
     * @param frameBuilder     is a function that can construct a frame from a clock file
     *                         coordinate system string. The coordinate system can be
     *                         any 5 characters string e.g., ITR92, IGb08.
     * @param typeBuilder      mapper from string to the observation type
     * @param timeScaleBuilder mapper from satellite system to time scales (useful for user-defined satellite systems)
     * @param timeScales       the set of time scales used for parsing dates.
     * @since 14.0
     */
    public RinexClockParser(final Function<? super String, ? extends Frame> frameBuilder,
                            final Function<? super String, ? extends ObservationType> typeBuilder,
                            final BiFunction<SatelliteSystem, TimeScales, ? extends TimeScale> timeScaleBuilder,
                            final TimeScales timeScales) {
        this.frameBuilder     = frameBuilder;
        this.typeBuilder      = typeBuilder;
        this.timeScaleBuilder = timeScaleBuilder;
        this.timeScales       = timeScales;
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

        /** Mapper from satellite system to time scales.
         * @since 13.0
         */
        private final BiFunction<SatelliteSystem, TimeScales, ? extends TimeScale> timeScaleBuilder;

        /** Set of time scales for parsing dates. */
        private final TimeScales timeScales;

        /** Current line number of the navigation message. */
        private int lineNumber;

        /** The corresponding clock file object. */
        private final RinexClock file;

        /** Indicator for format before 3.04. */
        private boolean before304;

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

        /** Current data date components. */
        private DateComponents currentDateComponents;

        /** Current data time components. */
        private TimeComponents currentTimeComponents;

        /** Current data number of data values to follow. */
        private int currentNumberOfValues;

        /** Current data values. */
        private double[] currentDataValues;

        /** Constructor, build the ParseInfo object.
         * @param name name of the data source
         */
        ParseInfo(final String name) {
            this.name                   = name;
            this.timeScales             = RinexClockParser.this.timeScales;
            this.timeScaleBuilder       = RinexClockParser.this.timeScaleBuilder;
            this.file                   = new RinexClock(frameBuilder);
            this.lineNumber             = 0;
            this.pendingReferenceClocks = new ArrayList<>();
        }

        /** Build an observation type.
         * @param type observation type
         * @return built type
         */
        ObservationType buildType(final String type) {
            return RinexClockParser.this.typeBuilder.apply(type);
        }

    }


    /** Parsers for specific lines. */
    private enum LineParser {

        /** Parser for version, file type and satellite system. */
        VERSION((header, line) -> header.matchFound(CommonLabel.VERSION, line),
                (line, parseInfo) ->  {
                    final RinexClockHeader header = parseInfo.file.getHeader();
                    RinexUtils.parseVersionFileTypeSatelliteSystem(line, parseInfo.name, header,
                                                                   2.00, 3.00, 3.01, 3.02, 3.04);
                    parseInfo.before304 = header.getFormatVersion() < 3.04;
                },
                LineParser::headerNext),

        /** Parser for generating program and emiting agency. */
        PROGRAM((header, line) -> header.matchFound(CommonLabel.PROGRAM, line),
                (line, parseInfo) -> RinexUtils.parseProgramRunByDate(line, parseInfo.lineNumber, parseInfo.name,
                                                                      parseInfo.timeScales, parseInfo.file.getHeader()),
                LineParser::headerNext),

        /** Parser for comments. */
        COMMENT((header, line) -> header.matchFound(CommonLabel.COMMENT, line),
                (line, parseInfo) -> RinexUtils.parseComment(parseInfo.lineNumber, line, parseInfo.file),
                LineParser::commentNext),

        /** Parser for satellite system and related observation types. */
        SYS_NB_TYPES_OF_OBSERV((header, line) -> header.matchFound(CommonLabel.SYS_NB_TYPES_OF_OBSERV, line),
                               (line, parseInfo) -> {
                                   if (parseInfo.remainingObsTypes == 0) {
                                       // we are starting a new satellite system
                                       parseInfo.currentSatelliteSystem =
                                           SatelliteSystem.parseSatelliteSystem(RinexUtils.parseString(line, 0, 1));
                                       parseInfo.remainingObsTypes = RinexUtils.parseInt(line, 3, 3);
                                   }
                                   for (int i = 0; i < 14 && parseInfo.remainingObsTypes > 0; ++i) {
                                       parseInfo.remainingObsTypes--;
                                       final String obsType = RinexUtils.parseString(line, 8 + 4 * i, 3);
                                       parseInfo.file.getHeader()
                                           .addSystemObservationType(parseInfo.currentSatelliteSystem,
                                                                     parseInfo.buildType(obsType));
                                   }
                               },
                               LineParser::sysObsTypesNext),

        /** Parser for time system identifier. */
        TIME_SYSTEM_ID((header, line) -> header.matchFound(ClockLabel.TIME_SYSTEM_ID, line),
                       (line, parseInfo) -> parseInfo.file.getHeader().
                           setTimeSystem(TimeSystem.parseTimeSystem(RinexUtils.parseString(line, 3, 3))),
                       LineParser::headerNext),

        /** Parser for leap seconds. */
        LEAP_SECONDS((header, line) -> header.matchFound(CommonLabel.LEAP_SECONDS, line),
                     ((line, parseInfo) -> parseInfo.file.getHeader().
                         setLeapSeconds(RinexUtils.parseInt(line, 0, 6))),
                     LineParser::headerNext),

        /** Parser for leap seconds GNSS. */
        LEAP_SECONDS_GNSS((header, line) -> header.matchFound(ClockLabel.LEAP_SECONDS_GNSS, line),
                     ((line, parseInfo) -> parseInfo.file.getHeader().
                         setLeapSecondsGNSS(RinexUtils.parseInt(line, 0, 6))),
                     LineParser::headerNext),

        /** Parser for differential code bias corrections. */
        SYS_DCBS_APPLIED((header, line) -> header.matchFound(CommonLabel.SYS_DCBS_APPLIED, line),
                         (line, parseInfo) -> parseInfo.file.getHeader().
                             addAppliedDCBS(new AppliedDCBS(SatelliteSystem.parseSatelliteSystem(RinexUtils.parseString(line, 0, 1)),
                                                            RinexUtils.parseString(line, 3, 17),
                                                            RinexUtils.parseString(line, 22, 43))),
                         LineParser::headerNext),

        /** Parser for phase center variations corrections. */
        SYS_PCVS_APPLIED((header, line) -> header.matchFound(CommonLabel.SYS_PCVS_APPLIED, line),
                         (line, parseInfo) -> parseInfo.file.getHeader().
                             addAppliedPCVS(new AppliedPCVS(SatelliteSystem.parseSatelliteSystem(RinexUtils.parseString(line, 0, 1)),
                                                            RinexUtils.parseString(line, 3, 17),
                                                            RinexUtils.parseString(line, 22, 43))),
                         LineParser::headerNext),

        /** Parser for the different clock data types that are stored in the file. */
        NB_TYPES_OF_DATA((header, line) -> header.matchFound(ClockLabel.NB_TYPES_OF_DATA, line),
                         (line, parseInfo) -> {
                             final int n = RinexUtils.parseInt(line, 0, 6);
                             for (int i = 0; i < n; i++) {
                                 final String type = RinexUtils.parseString(line, 6 + i * 6, 4);
                                 parseInfo.file.getHeader().addClockDataType(ClockDataType.parseClockDataType(type));
                             }
                         },
                         LineParser::headerNext),

        /** Parser for the station with reference clock. */
        STATION_NAME_NUM((header, line) -> header.matchFound(ClockLabel.STATION_NAME_NUM, line),
                         (line, parseInfo) -> {
                             final RinexClockHeader header = parseInfo.file.getHeader();
                             header.setStationName(RinexUtils.parseString(line, 0, 9));
                             header.setStationIdentifier(RinexUtils.parseString(line, 10, 20));
                         },
                         LineParser::headerNext),

        /** Parser for the reference clock in case of calibration data. */
        STATION_CLK_REF((header, line) -> header.matchFound(ClockLabel.STATION_CLK_REF, line),
                        (line, parseInfo) ->  {
                            final RinexClockHeader header = parseInfo.file.getHeader();
                            if (parseInfo.before304) {
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
                            header.setAnalysisCenterID(RinexUtils.parseString(line, 0, 3));

                            // Then, the full name of the analysis center
                            if (parseInfo.before304) {
                                header.setAnalysisCenterName(RinexUtils.parseString(line, 5, 55));
                            } else {
                                header.setAnalysisCenterName(RinexUtils.parseString(line, 5, 60));
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
                          final int startI = 7;
                          final int startD = parseInfo.before304 ? 23 : 24;
                          final int endI   = parseInfo.before304 ? 34 : 36;
                          final int endD   = parseInfo.before304 ? 50 : 53;
                              if (RinexUtils.parseString(line, startI, endD + 10 - startI).trim().isEmpty()) {
                              // no start/stop epoch the record applies to the whole file
                              parseInfo.referenceClockStartDate = AbsoluteDate.PAST_INFINITY;
                              parseInfo.referenceClockEndDate   = AbsoluteDate.FUTURE_INFINITY;
                          } else {
                              parseInfo.referenceClockStartDate =
                                  new AbsoluteDate(RinexUtils.parseInt(line,    startI,       4),
                                                   RinexUtils.parseInt(line,    startI +  5,  2),
                                                   RinexUtils.parseInt(line,    startI +  8,  2),
                                                   RinexUtils.parseInt(line,    startI + 11,  2),
                                                   RinexUtils.parseInt(line,    startI + 14,  2),
                                                   RinexUtils.parseDouble(line, startD,      10),
                                                   parseInfo.timeScaleBuilder.apply(header.getSatelliteSystem(),
                                                                                    parseInfo.timeScales));
                              parseInfo.referenceClockEndDate =
                                  new AbsoluteDate(RinexUtils.parseInt(line,    endI,       4),
                                                   RinexUtils.parseInt(line,    endI +  5,  2),
                                                   RinexUtils.parseInt(line,    endI +  8,  2),
                                                   RinexUtils.parseInt(line,    endI + 11,  2),
                                                   RinexUtils.parseInt(line,    endI + 14,  2),
                                                   RinexUtils.parseDouble(line, endD,      10),
                                                   parseInfo.timeScaleBuilder.apply(header.getSatelliteSystem(),
                                                                                    parseInfo.timeScales));
                          }
                      },
                      LineParser::headerNext),

        /** Parser for the reference clock over a period. */
        ANALYSIS_CLK_REF((header, line) -> header.matchFound(ClockLabel.ANALYSIS_CLK_REF, line),
                         (line, parseInfo) -> {

                             // First element is the name of the receiver/satellite embedding the reference clock
                             final int length = parseInfo.before304 ? 4 : 9;
                             final String referenceName = RinexUtils.parseString(line, 0, length);

                             // Second element is the reference clock ID
                             final String clockID = RinexUtils.parseString(line, length + 1, 20);

                             // Optionally, third element is an a priori clock constraint, by default equal to zero
                             double clockConstraint = RinexUtils.parseDouble(line, length + 36, 19);
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
                           (line, parseInfo) ->
                               parseInfo.file.getHeader().
                                   setFrameName(RinexUtils.parseString(line, 10, parseInfo.before304 ? 50 : 55)),
                           LineParser::headerNext),

        /** Parser for the stations embedded in the file and the related positions. */
        SOLN_STA_NAME_NUM((header, line) -> header.matchFound(ClockLabel.SOLN_STA_NAME_NUM, line),
                          (line, parseInfo) -> {
                                 final int    length     = parseInfo.before304 ? 4 : 9;
                                 final String designator = RinexUtils.parseString(line, 0, length);
                                 final String identifier = RinexUtils.parseString(line, length + 1, 20);
                                 final double x          = MILLIMETER.toSI(RinexUtils.parseInt(line, length + 21, 11));
                                 final double y          = MILLIMETER.toSI(RinexUtils.parseInt(line, length + 33, 11));
                                 final double z          = MILLIMETER.toSI(RinexUtils.parseInt(line, length + 45, 11));
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
                     final int nMax = parseInfo.before304 ? 15 : 16;
                     for (int i = 0; i < nMax; ++i) {
                         final String prn = RinexUtils.parseString(line, 4 * i, 3);
                         if (prn.isEmpty()) {
                             break;
                         } else {
                             header.addSatellite(prn);
                         }
                     }
                 },
                 LineParser::headerNext),

        /** Parser for the end of header. */
        HEADER_END((header, line) -> header.matchFound(CommonLabel.END, line),
                   (line, parseInfo) -> {
                       if (!parseInfo.pendingReferenceClocks.isEmpty()) {
                           // Modify time span map of the reference clocks to accept the pending reference clock
                           parseInfo.file.getHeader().addReferenceClockList(parseInfo.pendingReferenceClocks, parseInfo.referenceClockStartDate);
                       };
                       parseInfo.headerCompleted = true;
                   },
                   LineParser::headerEndNext),

        /** Parser for a clock data line. */
        CLOCK_DATA((header, line) -> line.charAt(0) != ' ',
                   (line, parseInfo) -> {

                       try {
                           parseInfo.currentDataType = ClockDataType.valueOf(line.substring(0, 2));
                        } catch (IllegalArgumentException iae) {
                           throw new OrekitException(OrekitMessages.UNKNOWN_CLOCK_DATA_TYPE, line.substring(0, 2));
                       }

                       // Initialise current values
                       parseInfo.currentDataValues = new double[6];

                       // Second element is receiver/satellite name
                       final int length = parseInfo.before304 ? 4 : 9;
                       parseInfo.currentName = RinexUtils.parseString(line, 3, length);

                       // Third element is data epoch
                       final int year = scanner.nextInt();
                       final int month = scanner.nextInt();
                       final int day = scanner.nextInt();
                       final int hour = scanner.nextInt();
                       final int min = scanner.nextInt();
                       final double sec = scanner.nextDouble();
                       parseInfo.currentDateComponents = new DateComponents(year, month, day);
                       parseInfo.currentTimeComponents = new TimeComponents(hour, min, sec);

                       // Fourth element is number of data values
                       parseInfo.currentNumberOfValues = scanner.nextInt();

                       // Get the values in this line, there are at most 2.
                       // Some entries claim less values than there actually are.
                       // All values are added to the set, regardless of their claimed number.
                       int i = 0;
                       while (scanner.hasNextDouble()) {
                           parseInfo.currentDataValues[i++] = scanner.nextDouble();
                       }

                       // Check if continuation line is required
                       if (parseInfo.currentNumberOfValues <= 2) {
                           // No continuation line is required
                           parseInfo.file.addClockData(parseInfo.currentName,
                                                       new ClockDataLine(parseInfo.currentDataType,
                                                                         parseInfo.currentName,
                                                                         parseInfo.currentDateComponents,
                                                                         parseInfo.currentTimeComponents,
                                                                         parseInfo.currentNumberOfValues,
                                                                         parseInfo.currentDataValues[0],
                                                                         parseInfo.currentDataValues[1], 0.0, 0.0, 0.0,
                                                                         0.0));
                       }
                   },
                   LineParser::dataNext),

        /** Parser for a continuation clock data line. */
        CLOCK_DATA_CONTINUATION((header, line) -> line.charAt(0) == ' ',
                                (line, parseInfo) -> {

                                    // Get the values in this continuation line.
                                    // Some entries claim less values than there actually are.
                                    // All values are added to the set, regardless of their claimed number.
                                    int i = 2;
                                    while (scanner.hasNextDouble()) {
                                        parseInfo.currentDataValues[i++] = scanner.nextDouble();
                                    }

                                    // Add clock data line
                                    parseInfo.file.addClockData(parseInfo.currentName,
                                                                new ClockDataLine(parseInfo.currentDataType,
                                                                                  parseInfo.currentName,
                                                                                  parseInfo.currentDateComponents,
                                                                                  parseInfo.currentTimeComponents,
                                                                                  parseInfo.currentNumberOfValues,
                                                                                  parseInfo.currentDataValues[0],
                                                                                  parseInfo.currentDataValues[1],
                                                                                  parseInfo.currentDataValues[2],
                                                                                  parseInfo.currentDataValues[3],
                                                                                  parseInfo.currentDataValues[4],
                                                                                  parseInfo.currentDataValues[5]));

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
            return Arrays.asList(PROGRAM, COMMENT, SYS_NB_TYPES_OF_OBSERV, TIME_SYSTEM_ID,
                                 LEAP_SECONDS, ANALYSIS_CENTER, NB_OF_CLK_REF, ANALYSIS_CLK_REF,
                                 NB_OF_SOLN_STA_TRF, SOLN_STA_NAME_NUM, NB_OF_SOLN_SATS, PRN_LIST, HEADER_END);
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
            return Arrays.asList(LineParser.CLOCK_DATA, LineParser.CLOCK_DATA_CONTINUATION);
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
