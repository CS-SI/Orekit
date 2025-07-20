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
package org.orekit.files.rinex.navigation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collections;
import java.util.InputMismatchException;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hipparchus.util.FastMath;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.rinex.navigation.parsers.GlonassFdmaParser;
import org.orekit.files.rinex.navigation.parsers.MessageLineParser;
import org.orekit.files.rinex.navigation.parsers.ParseInfo;
import org.orekit.files.rinex.section.CommonLabel;
import org.orekit.files.rinex.utils.ParsingUtils;
import org.orekit.gnss.PredefinedTimeSystem;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.propagation.analytical.gnss.data.NavICL1NvNavigationMessage;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.GNSSDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScales;
import org.orekit.utils.units.Unit;

/**
 * Parser for RINEX navigation messages files.
 * <p>
 * This parser handles RINEX version from 2 to 4.02.
 * </p>
 * @see <a href="https://files.igs.org/pub/data/format/rinex2.txt">rinex 2.0</a>
 * @see <a href="https://files.igs.org/pub/data/format/rinex210.txt">rinex 2.10</a>
 * @see <a href="https://files.igs.org/pub/data/format/rinex211.pdf">rinex 2.11</a>
 * @see <a href="https://files.igs.org/pub/data/format/rinex301.pdf"> 3.01 navigation messages file format</a>
 * @see <a href="https://files.igs.org/pub/data/format/rinex302.pdf"> 3.02 navigation messages file format</a>
 * @see <a href="https://files.igs.org/pub/data/format/rinex303.pdf"> 3.03 navigation messages file format</a>
 * @see <a href="https://files.igs.org/pub/data/format/rinex304.pdf"> 3.04 navigation messages file format</a>
 * @see <a href="https://files.igs.org/pub/data/format/rinex305.pdf"> 3.05 navigation messages file format</a>
 * @see <a href="https://files.igs.org/pub/data/format/rinex_4.00.pdf"> 4.00 navigation messages file format</a>
 * @see <a href="https://files.igs.org/pub/data/format/rinex_4.01.pdf"> 4.01 navigation messages file format</a>
 * @see <a href="https://files.igs.org/pub/data/format/rinex_4.02.pdf"> 4.02 navigation messages file format</a>
 *
 * @author Bryan Cazabonne
 * @since 11.0
 *
 */
public class RinexNavigationParser {

    /** Converter for positions. */
    public static final Unit KM = Unit.KILOMETRE;

    /** Converter for velocities. */
    public static final Unit KM_PER_S = Unit.parse("km/s");

    /** Converter for accelerations. */
    public static final Unit KM_PER_S2 = Unit.parse("km/s²");

    /** Converter for velocities. */
    public static final Unit M_PER_S = Unit.parse("m/s");

    /** Converter for clock drift. */
    public static final Unit S_PER_S = Unit.parse("s/s");

    /** Converter for clock drift rate. */
    public static final Unit S_PER_S2 = Unit.parse("s/s²");

    /** Converter for ΔUT₁ first derivative. */
    public static final Unit S_PER_DAY = Unit.parse("s/d");

    /** Converter for ΔUT₁ second derivative. */
    public static final Unit S_PER_DAY2 = Unit.parse("s/d²");

    /** Converter for square root of semi-major axis. */
    public static final Unit SQRT_M = Unit.parse("√m");

    /** Converter for angular rates. */
    public static final Unit RAD_PER_S = Unit.parse("rad/s");

    /** Converter for angular accelerations. */
    public static final Unit RAD_PER_S2 = Unit.parse("rad/s²");

    /** Converter for rates of small angle. */
    public static final Unit AS_PER_DAY = Unit.parse("as/d");

    /** Converter for accelerations of small angles. */
    public static final Unit AS_PER_DAY2 = Unit.parse("as/d²");

    /** Total Electron Content. */
    public static final Unit TEC = Unit.TOTAL_ELECTRON_CONTENT_UNIT;

    /** System initials. */
    private static final String INITIALS = "GRECIJS";

    /** Set of time scales. */
    private final TimeScales timeScales;

    /**
     * Constructor.
     * <p>This constructor uses the {@link DataContext#getDefault() default data context}.</p>
     * @see #RinexNavigationParser(TimeScales)
     *
     */
    @DefaultDataContext
    public RinexNavigationParser() {
        this(DataContext.getDefault().getTimeScales());
    }

    /**
     * Constructor.
     * @param timeScales the set of time scales used for parsing dates.
     */
    public RinexNavigationParser(final TimeScales timeScales) {
        this.timeScales = timeScales;
    }

    /**
     * Parse RINEX navigation messages.
     * @param source source providing the data to parse
     * @return a parsed  RINEX navigation messages file
     * @throws IOException if {@code reader} throws one
     */
    public RinexNavigation parse(final DataSource source) throws IOException {

        // initialize internal data structures
        final ParseInfo parseInfo = new ParseInfo(source.getName(), timeScales);

        Iterable<LineParser> candidateParsers = Collections.singleton(LineParser.HEADER_VERSION);
        try (Reader reader = source.getOpener().openReaderOnce();
             BufferedReader br = new BufferedReader(reader)) {
            nextLine:
                for (String line = br.readLine(); line != null; line = br.readLine()) {
                    parseInfo.setLine(line);
                    for (final LineParser candidate : candidateParsers) {
                        if (candidate.canHandle.apply(parseInfo.getHeader(), line)) {
                            try {
                                candidate.parsingMethod.accept(parseInfo);
                                candidateParsers = candidate.allowedNextProvider.apply(parseInfo);
                                continue nextLine;
                            } catch (StringIndexOutOfBoundsException | NumberFormatException | InputMismatchException e) {
                                throw new OrekitException(e,
                                                          OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                          parseInfo.getLineNumber(), source.getName(), line);
                            }
                        }
                    }
                    throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                              parseInfo.getLineNumber(), source.getName(), line);
                }
        }

        return parseInfo.getCompletedFile();

    }

    /** Parsers for specific lines. */
    private enum LineParser {

        /** Parser for version, file type and satellite system. */
        HEADER_VERSION((header, line) -> header.matchFound(CommonLabel.VERSION, line),
                       pi -> {
                           pi.getHeader().parseVersionFileTypeSatelliteSystem(pi.getLine(), SatelliteSystem.GPS,
                                                                              pi.getName(),
                                                                              2.0, 2.01, 2.10, 2.11,
                                                                              3.01, 3.02, 3.03, 3.04, 3.05,
                                                                              4.00, 4.01, 4.02);
                           pi.setInitialSpaces(pi.getHeader().getFormatVersion() < 3.0 ? 3 : 4);
                       },
                       LineParser::headerNext),

        /** Parser for generating program and emitting agency. */
        HEADER_PROGRAM((header, line) -> header.matchFound(CommonLabel.PROGRAM, line),
                       pi -> pi.getHeader().parseProgramRunByDate(pi.getLine(), pi.getTimeScales()),
                       LineParser::headerNext),

        /** Parser for comments. */
        HEADER_COMMENT((header, line) -> header.matchFound(CommonLabel.COMMENT, line),
                       ParseInfo::parseComment,
                       LineParser::headerNext),

        /** Parser for ionospheric correction parameters. */
        HEADER_ION_ALPHA((header, line) -> header.matchFound(NavigationLabel.ION_ALPHA, line),
                         pi -> {

                             pi.getHeader().setIonosphericCorrectionType(IonosphericCorrectionType.GPS);

                             // Read coefficients
                             final double[] parameters = new double[4];
                             parameters[0] = ParsingUtils.parseDouble(pi.getLine(), 2, 12);
                             parameters[1] = ParsingUtils.parseDouble(pi.getLine(), 14, 12);
                             parameters[2] = ParsingUtils.parseDouble(pi.getLine(), 26, 12);
                             parameters[3] = ParsingUtils.parseDouble(pi.getLine(), 38, 12);
                             pi.setKlobucharAlpha(parameters);

                         },
                         LineParser::headerNext),

        /** Parser for ionospheric correction parameters. */
        HEADER_ION_BETA((header, line) -> header.matchFound(NavigationLabel.ION_BETA, line),
                        pi -> {

                            pi.getHeader().setIonosphericCorrectionType(IonosphericCorrectionType.GPS);

                            // Read coefficients
                            final double[] parameters = new double[4];
                            parameters[0] = ParsingUtils.parseDouble(pi.getLine(), 2, 12);
                            parameters[1] = ParsingUtils.parseDouble(pi.getLine(), 14, 12);
                            parameters[2] = ParsingUtils.parseDouble(pi.getLine(), 26, 12);
                            parameters[3] = ParsingUtils.parseDouble(pi.getLine(), 38, 12);
                            pi.setKlobucharBeta(parameters);

                        },
                        LineParser::headerNext),

        /** Parser for ionospheric correction parameters. */
        HEADER_IONOSPHERIC((header, line) -> header.matchFound(NavigationLabel.IONOSPHERIC_CORR, line),
                           pi -> {

                               // ionospheric correction type
                               final IonosphericCorrectionType ionoType =
                                               IonosphericCorrectionType.valueOf(ParsingUtils.parseString(pi.getLine(), 0, 3));
                               pi.getHeader().setIonosphericCorrectionType(ionoType);

                               // Read coefficients
                               final double[] parameters = new double[4];
                               parameters[0] = ParsingUtils.parseDouble(pi.getLine(), 5, 12);
                               parameters[1] = ParsingUtils.parseDouble(pi.getLine(), 17, 12);
                               parameters[2] = ParsingUtils.parseDouble(pi.getLine(), 29, 12);
                               parameters[3] = ParsingUtils.parseDouble(pi.getLine(), 41, 12);

                               // Verify if we are parsing Galileo ionospheric parameters
                               if (ionoType == IonosphericCorrectionType.GAL) {

                                   // We are parsing Galileo ionospheric parameters
                                   pi.setNeQuickAlpha(parameters);

                               } else {
                                   // We are parsing Klobuchar ionospheric parameters

                                   // Verify if we are parsing "alpha" or "beta" ionospheric parameters
                                   if (pi.isIonosphereAlphaInitialized()) {

                                       // Ionospheric "beta" parameters
                                       pi.setKlobucharBeta(parameters);

                                   } else {

                                       // Ionospheric "alpha" parameters
                                       pi.setKlobucharAlpha(parameters);

                                   }

                               }

                           },
                           LineParser::headerNext),

        /** Parser for corrections to transform the system time to UTC or to other time systems. */
        HEADER_DELTA_UTC((header, line) -> header.matchFound(NavigationLabel.DELTA_UTC, line),
                         pi -> {
                             // Read fields
                             final double a0      = ParsingUtils.parseDouble(pi.getLine(), 3, 19);
                             final double a1      = ParsingUtils.parseDouble(pi.getLine(), 22, 19);
                             final int    refTime = ParsingUtils.parseInt(pi.getLine(), 41, 9);
                             final int    refWeek = ParsingUtils.parseInt(pi.getLine(), 50, 9);

                             // convert date
                             final SatelliteSystem satSystem = pi.getHeader().getSatelliteSystem();
                             final AbsoluteDate    date      = new GNSSDate(refWeek, refTime, satSystem, pi.getTimeScales()).getDate();

                             // Add to the list
                             final TimeSystemCorrection tsc = new TimeSystemCorrection("GPUT", date, a0, a1);
                             pi.getHeader().addTimeSystemCorrections(tsc);
                         },
                         LineParser::headerNext),

        /** Parser for corrections to transform the GLONASS system time to UTC or to other time systems. */
        HEADER_CORR_SYSTEM_TIME((header, line) -> header.matchFound(NavigationLabel.CORR_TO_SYSTEM_TIME, line),
                         pi -> {
                             // Read fields
                             final int year        = ParsingUtils.parseInt(pi.getLine(), 0, 6);
                             final int month       = ParsingUtils.parseInt(pi.getLine(), 6, 6);
                             final int day         = ParsingUtils.parseInt(pi.getLine(), 12, 6);
                             final double minusTau = ParsingUtils.parseDouble(pi.getLine(), 21, 19);

                             // convert date
                             final SatelliteSystem satSystem = pi.getHeader().getSatelliteSystem();
                             final TimeScale       timeScale = satSystem.getObservationTimeScale().getTimeScale(pi.getTimeScales());
                             final AbsoluteDate    date      = new AbsoluteDate(year, month, day, timeScale);

                             // Add to the list
                             final TimeSystemCorrection tsc = new TimeSystemCorrection("GLUT", date, minusTau, 0.0);
                             pi.getHeader().addTimeSystemCorrections(tsc);

                         },
                         LineParser::headerNext),

        /** Parser for corrections to transform the system time to UTC or to other time systems. */
        HEADER_TIME((header, line) -> header.matchFound(NavigationLabel.TIME_SYSTEM_CORR, line),
                    pi -> {

                        // Read fields
                        final String type    = ParsingUtils.parseString(pi.getLine(), 0, 4);
                        final double a0      = ParsingUtils.parseDouble(pi.getLine(), 5, 17);
                        final double a1      = ParsingUtils.parseDouble(pi.getLine(), 22, 16);
                        final int    refTime = ParsingUtils.parseInt(pi.getLine(), 38, 7);
                        final int    refWeek = ParsingUtils.parseInt(pi.getLine(), 46, 5);

                        // convert date
                        final SatelliteSystem satSystem = pi.getHeader().getSatelliteSystem();
                        final AbsoluteDate    date;
                        if (satSystem == SatelliteSystem.GLONASS) {
                            date = null;
                        } else if (satSystem == SatelliteSystem.BEIDOU) {
                            date = new GNSSDate(refWeek, refTime, satSystem, pi.getTimeScales()).getDate();
                        } else {
                            // all other systems are converted to GPS week in Rinex files!
                            date = new GNSSDate(refWeek, refTime, SatelliteSystem.GPS, pi.getTimeScales()).getDate();
                        }

                        // Add to the list
                        final TimeSystemCorrection tsc = new TimeSystemCorrection(type, date, a0, a1);
                        pi.getHeader().addTimeSystemCorrections(tsc);

                    },
                    LineParser::headerNext),

        /** Parser for leap seconds. */
        HEADER_LEAP_SECONDS((header, line) -> header.matchFound(CommonLabel.LEAP_SECONDS, line),
                            pi -> {
                                pi.getHeader().setLeapSecondsGNSS(ParsingUtils.parseInt(pi.getLine(), 0, 6));
                                pi.getHeader().setLeapSecondsFuture(ParsingUtils.parseInt(pi.getLine(), 6, 6));
                                pi.getHeader().setLeapSecondsWeekNum(ParsingUtils.parseInt(pi.getLine(), 12, 6));
                                pi.getHeader().setLeapSecondsDayNum(ParsingUtils.parseInt(pi.getLine(), 18, 6));
                            },
                            LineParser::headerNext),

        /** Parser for DOI.
         * @since 12.0
         */
        HEADER_DOI((header, line) -> header.matchFound(CommonLabel.DOI, line),
                   pi -> pi.getHeader().
                       setDoi(ParsingUtils.parseString(pi.getLine(), 0, pi.getHeader().getLabelIndex())),
                   LineParser::headerNext),

        /** Parser for license.
         * @since 12.0
         */
        HEADER_LICENSE((header, line) -> header.matchFound(CommonLabel.LICENSE, line),
                       pi -> pi.getHeader().
                           setLicense(ParsingUtils.parseString(pi.getLine(), 0, pi.getHeader().getLabelIndex())),
                       LineParser::headerNext),

        /** Parser for stationInformation.
         * @since 12.0
         */
        HEADER_STATION_INFORMATION((header, line) -> header.matchFound(CommonLabel.STATION_INFORMATION, line),
                                   pi -> pi.getHeader().
                                       setStationInformation(ParsingUtils.parseString(pi.getLine(), 0, pi.getHeader().getLabelIndex())),
                                   LineParser::headerNext),

        /** Parser for merged files.
         * @since 12.0
         */
        HEADER_MERGED_FILE((header, line) -> header.matchFound(NavigationLabel.MERGED_FILE, line),
                           pi -> pi.getHeader().setMergedFiles(ParsingUtils.parseInt(pi.getLine(), 0, 9)),
                           LineParser::headerNext),

       /** Parser for the end of header. */
        HEADER_END((header, line) -> header.matchFound(CommonLabel.END, line),
                   pi -> {
                       // get rinex format version
                       final RinexNavigationHeader header = pi.getHeader();
                       final double version = header.getFormatVersion();

                       // check mandatory header fields
                       if (header.getRunByName() == null ||
                           version >= 4 && header.getLeapSecondsGNSS() < 0) {
                           throw new OrekitException(OrekitMessages.INCOMPLETE_HEADER, pi.getName());
                       }

                       pi.setHeaderParsed(true);

                   },
                   LineParser::navigationNext),

        /** Parser for navigation message space vehicle epoch and clock. */
        NAVIGATION_SV_EPOCH_CLOCK_RINEX_2((header, line) -> true,
                                          pi -> {
                                              pi.setSystemLineParser(pi.getHeader().getSatelliteSystem(), null);
                                              pi.getMessageLineParser().parseLine00();
                                          },
                                          LineParser::navigationNext),

        /** Parser for navigation message space vehicle epoch and clock. */
        NAVIGATION_SV_EPOCH_CLOCK((header, line) -> INITIALS.indexOf(line.charAt(0)) >= 0,
                                  pi -> {
                                      if (pi.getHeader().getFormatVersion() < 4) {
                                          final SatelliteSystem system =
                                              SatelliteSystem.parseSatelliteSystem(ParsingUtils.parseString(pi.getLine(), 0, 1));
                                          pi.setSystemLineParser(system, null);
                                      }

                                      // Read first line
                                      pi.getMessageLineParser().parseLine00();

                                  },
                                  LineParser::navigationNext),

        /** Parser for navigation message type. */
        EPH_TYPE((header, line) -> MessageType.EPH.matches(line),
                 pi -> {
                     final SatelliteSystem system =
                         SatelliteSystem.parseSatelliteSystem(ParsingUtils.parseString(pi.getLine(), 6, 1));
                     final String          type   = ParsingUtils.parseString(pi.getLine(), 10, 4);
                     pi.setSystemLineParser(system, type);
                 },
                 pi -> Collections.singleton(NAVIGATION_SV_EPOCH_CLOCK)),

        /** Parser for message lines. */
        MESSAGE_LINE((header, line) -> MessageType.ORBIT.matches(line),
                     ParseInfo::parseMessageLine,
                     LineParser::navigationNext),

        /** Parser for system time offset message model. */
        STO_LINE_1((header, line) -> true,
                   pi -> {
                       pi.getSto().setTransmissionTime(pi.parseDouble1(Unit.SECOND));
                       pi.getSto().setA0(pi.parseDouble2(Unit.SECOND));
                       pi.getSto().setA1(pi.parseDouble3(S_PER_S));
                       pi.getSto().setA2(pi.parseDouble4(S_PER_S2));
                       pi.finishSto();
                   },
                   LineParser::navigationNext),

        /** Parser for system time offset message space vehicle epoch and clock. */
        STO_SV_EPOCH_CLOCK((header, line) -> true,
                           pi -> {

                               pi.getSto().setDefinedTimeSystem(PredefinedTimeSystem.
                                                           parseTwoLettersCode(ParsingUtils.parseString(pi.getLine(), 24, 2)));
                               pi.getSto().setReferenceTimeSystem(PredefinedTimeSystem.
                                                             parseTwoLettersCode(ParsingUtils.parseString(pi.getLine(), 26, 2)));
                               final String sbas = ParsingUtils.parseString(pi.getLine(), 43, 18);
                               pi.getSto().setSbasId(sbas == null || sbas.isEmpty() ? null : SbasId.valueOf(sbas));
                               final String utc = ParsingUtils.parseString(pi.getLine(), 62, 18);
                               pi.getSto().setUtcId(utc == null || utc.isEmpty() ? null : UtcId.parseUtcId(utc));
                               pi.getSto().setReferenceEpoch(pi.parseDate(pi.getLine(), pi.getSto().getSystem()));

                           },
                           pi -> Collections.singleton(STO_LINE_1)),

        /** Parser for system time offset message type. */
        STO_TYPE((header, line) -> MessageType.STO.matches(line),
                 pi -> {
                     pi.setSto(new SystemTimeOffsetMessage(SatelliteSystem.
                                                           parseSatelliteSystem(ParsingUtils.parseString(pi.getLine(), 6, 1)),
                                                           ParsingUtils.parseInt(pi.getLine(), 7, 2),
                                                           ParsingUtils.parseString(pi.getLine(), 10, 4),
                                                           ParsingUtils.parseString(pi.getLine(), 15, 4)));
                 },
                 pi -> Collections.singleton(STO_SV_EPOCH_CLOCK)),

        /** Parser for Earth orientation parameter message model. */
        EOP_LINE_2((header, line) -> true,
                   pi -> {
                       pi.getEop().setTransmissionTime(pi.parseDouble1(Unit.SECOND));
                       pi.getEop().setDut1(pi.parseDouble2(Unit.SECOND));
                       pi.getEop().setDut1Dot(pi.parseDouble3(S_PER_DAY));
                       pi.getEop().setDut1DotDot(pi.parseDouble4(S_PER_DAY2));
                       pi.finishEop();
                   },
                   LineParser::navigationNext),

        /** Parser for Earth orientation parameter message model. */
        EOP_LINE_1((header, line) -> true,
                   pi -> {
                       pi.getEop().setYp(pi.parseDouble2(Unit.ARC_SECOND));
                       pi.getEop().setYpDot(pi.parseDouble3(AS_PER_DAY));
                       pi.getEop().setYpDotDot(pi.parseDouble4(AS_PER_DAY2));
                   },
                   pi -> Collections.singleton(EOP_LINE_2)),

        /** Parser for Earth orientation parameter message model. */
        EOP_LINE_0((header, line) -> true,
                   pi -> {
                       pi.getEop().setReferenceEpoch(pi.parseDate(pi.getLine(), pi.getEop().getSystem()));
                       pi.getEop().setXp(pi.parseDouble2(Unit.ARC_SECOND));
                       pi.getEop().setXpDot(pi.parseDouble3(AS_PER_DAY));
                       pi.getEop().setXpDotDot(pi.parseDouble4(AS_PER_DAY2));
                   },
                   pi -> Collections.singleton(EOP_LINE_1)),

        /** Parser for Earth orientation parameter message type. */
        EOP_TYPE((header, line) -> MessageType.EOP.matches(line),
                 pi -> {
                     pi.setEop(new EarthOrientationParameterMessage(SatelliteSystem.
                                                                    parseSatelliteSystem(ParsingUtils.parseString(pi.getLine(), 6, 1)),
                                                                    ParsingUtils.parseInt(pi.getLine(), 7, 2),
                                                                    ParsingUtils.parseString(pi.getLine(), 10, 4),
                                                                    ParsingUtils.parseString(pi.getLine(), 15, 4)));
                 },
                 pi -> Collections.singleton(EOP_LINE_0)),

        /** Parser for ionosphere Klobuchar message model. */
        KLOBUCHAR_LINE_2((header, line) -> true,
                         pi -> {
                             pi.getKlobuchar().setBetaI(3, pi.parseDouble1(IonosphereBaseMessage.S_PER_SC_N3));
                             pi.getKlobuchar().setRegionCode(pi.parseDouble2(Unit.ONE) < 0.5 ?
                                                        RegionCode.WIDE_AREA : RegionCode.JAPAN);
                             pi.finishKlobuchar();
                         },
                         LineParser::navigationNext),

        /** Parser for ionosphere Klobuchar message model. */
        KLOBUCHAR_LINE_1((header, line) -> true,
                         pi -> {
                             pi.getKlobuchar().setAlphaI(3, pi.parseDouble1(IonosphereBaseMessage.S_PER_SC_N3));
                             pi.getKlobuchar().setBetaI(0, pi.parseDouble2(IonosphereBaseMessage.S_PER_SC_N0));
                             pi.getKlobuchar().setBetaI(1, pi.parseDouble3(IonosphereBaseMessage.S_PER_SC_N1));
                             pi.getKlobuchar().setBetaI(2, pi.parseDouble4(IonosphereBaseMessage.S_PER_SC_N2));
                         },
                         pi -> Collections.singleton(KLOBUCHAR_LINE_2)),

        /** Parser for ionosphere Klobuchar message model. */
        KLOBUCHAR_LINE_0((header, line) -> true,
                         pi -> {
                             pi.getKlobuchar().setTransmitTime(pi.parseDate(pi.getLine(), pi.getKlobuchar().getSystem()));
                             pi.getKlobuchar().setAlphaI(0, pi.parseDouble2(IonosphereBaseMessage.S_PER_SC_N0));
                             pi.getKlobuchar().setAlphaI(1, pi.parseDouble3(IonosphereBaseMessage.S_PER_SC_N1));
                             pi.getKlobuchar().setAlphaI(2, pi.parseDouble4(IonosphereBaseMessage.S_PER_SC_N2));
                         },
                         pi -> Collections.singleton(KLOBUCHAR_LINE_1)),

        /** Parser for NacIV Klobuchar message model.
         * @since 14.0
         */
        NAVIC_KLOBUCHAR_LINE_3((header, line) -> true,
                               pi -> {
                                   pi.getNavICKlobuchar().setLonMin(pi.parseDouble1(Unit.DEGREE));
                                   pi.getNavICKlobuchar().setLonMax(pi.parseDouble2(Unit.DEGREE));
                                   pi.getNavICKlobuchar().setModipMin(pi.parseDouble3(Unit.DEGREE));
                                   pi.getNavICKlobuchar().setModipMax(pi.parseDouble4(Unit.DEGREE));
                                   pi.finishNavICKlobuchar();
                               },
                               LineParser::navigationNext),

        /** Parser for NavIC Klobuchar message model.
         * @since 14.0
         */
        NAVIC_KLOBUCHAR_LINE_2((header, line) -> true,
                               pi -> {
                                   pi.getNavICKlobuchar().setBetaI(0, pi.parseDouble1(IonosphereBaseMessage.S_PER_SC_N0));
                                   pi.getNavICKlobuchar().setBetaI(1, pi.parseDouble2(IonosphereBaseMessage.S_PER_SC_N1));
                                   pi.getNavICKlobuchar().setBetaI(2, pi.parseDouble3(IonosphereBaseMessage.S_PER_SC_N2));
                                   pi.getNavICKlobuchar().setBetaI(3, pi.parseDouble4(IonosphereBaseMessage.S_PER_SC_N3));
                               },
                               pi -> Collections.singleton(NAVIC_KLOBUCHAR_LINE_3)),

        /** Parser for NavIC Klobuchar message model.
         * @since 14.0
         */
        NAVIC_KLOBUCHAR_LINE_1((header, line) -> true,
                               pi -> {
                                   pi.getNavICKlobuchar().setAlphaI(0, pi.parseDouble1(IonosphereBaseMessage.S_PER_SC_N0));
                                   pi.getNavICKlobuchar().setAlphaI(1, pi.parseDouble2(IonosphereBaseMessage.S_PER_SC_N1));
                                   pi.getNavICKlobuchar().setAlphaI(2, pi.parseDouble3(IonosphereBaseMessage.S_PER_SC_N2));
                                   pi.getNavICKlobuchar().setAlphaI(3, pi.parseDouble4(IonosphereBaseMessage.S_PER_SC_N3));
                               },
                               pi -> Collections.singleton(NAVIC_KLOBUCHAR_LINE_2)),

        /** Parser for NavIC Klobuchar message model. */
        NAVIC_KLOBUCHAR_LINE_0((header, line) -> true,
                               pi -> {
                                   pi.getNavICKlobuchar().setTransmitTime(pi.parseDate(pi.getLine(), pi.getNavICKlobuchar().getSystem()));
                                   pi.getNavICKlobuchar().setIOD(pi.parseDouble2(Unit.ONE));
                               },
                               pi -> Collections.singleton(NAVIC_KLOBUCHAR_LINE_1)),

        /** Parser for NacIV NeQuick N message model.
         * @since 14.0
         */
        NAVIC_NEQUICK_N_LINE_6((header, line) -> true,
                               pi -> {
                                   pi.getNavICNeQuickN().getRegion3().setLonMin(pi.parseDouble1(Unit.DEGREE));
                                   pi.getNavICNeQuickN().getRegion3().setLonMax(pi.parseDouble2(Unit.DEGREE));
                                   pi.getNavICNeQuickN().getRegion3().setModipMin(pi.parseDouble3(Unit.DEGREE));
                                   pi.getNavICNeQuickN().getRegion3().setModipMax(pi.parseDouble4(Unit.DEGREE));
                                   pi.finishNavICNeQuickN();
                               },
                               LineParser::navigationNext),

        /** Parser for NacIV NeQuick N message model.
         * @since 14.0
         */
        NAVIC_NEQUICK_N_LINE_5((header, line) -> true,
                               pi -> {
                                   pi.getNavICNeQuickN().getRegion3().setAi0(pi.parseDouble1(IonosphereAij.SFU));
                                   pi.getNavICNeQuickN().getRegion3().setAi1(pi.parseDouble2(IonosphereAij.SFU_PER_DEG));
                                   pi.getNavICNeQuickN().getRegion3().setAi2(pi.parseDouble3(IonosphereAij.SFU_PER_DEG2));
                                   pi.getNavICNeQuickN().getRegion3().setIDF(pi.parseDouble4(Unit.ONE));
                               },
                               pi -> Collections.singleton(NAVIC_NEQUICK_N_LINE_6)),

        /** Parser for NacIV NeQuick N message model.
         * @since 14.0
         */
        NAVIC_NEQUICK_N_LINE_4((header, line) -> true,
                               pi -> {
                                   pi.getNavICNeQuickN().getRegion2().setLonMin(pi.parseDouble1(Unit.DEGREE));
                                   pi.getNavICNeQuickN().getRegion2().setLonMax(pi.parseDouble2(Unit.DEGREE));
                                   pi.getNavICNeQuickN().getRegion2().setModipMin(pi.parseDouble3(Unit.DEGREE));
                                   pi.getNavICNeQuickN().getRegion2().setModipMax(pi.parseDouble4(Unit.DEGREE));
                               },
                               pi -> Collections.singleton(NAVIC_NEQUICK_N_LINE_5)),

        /** Parser for NacIV NeQuick N message model.
         * @since 14.0
         */
        NAVIC_NEQUICK_N_LINE_3((header, line) -> true,
                               pi -> {
                                   pi.getNavICNeQuickN().getRegion2().setAi0(pi.parseDouble1(IonosphereAij.SFU));
                                   pi.getNavICNeQuickN().getRegion2().setAi1(pi.parseDouble2(IonosphereAij.SFU_PER_DEG));
                                   pi.getNavICNeQuickN().getRegion2().setAi2(pi.parseDouble3(IonosphereAij.SFU_PER_DEG2));
                                   pi.getNavICNeQuickN().getRegion2().setIDF(pi.parseDouble4(Unit.ONE));
                               },
                               pi -> Collections.singleton(NAVIC_NEQUICK_N_LINE_4)),

        /** Parser for NacIV NeQuick N message model.
         * @since 14.0
         */
        NAVIC_NEQUICK_N_LINE_2((header, line) -> true,
                               pi -> {
                                   pi.getNavICNeQuickN().getRegion1().setLonMin(pi.parseDouble1(Unit.DEGREE));
                                   pi.getNavICNeQuickN().getRegion1().setLonMax(pi.parseDouble2(Unit.DEGREE));
                                   pi.getNavICNeQuickN().getRegion1().setModipMin(pi.parseDouble3(Unit.DEGREE));
                                   pi.getNavICNeQuickN().getRegion1().setModipMax(pi.parseDouble4(Unit.DEGREE));
                               },
                               pi -> Collections.singleton(NAVIC_NEQUICK_N_LINE_3)),

        /** Parser for NacIV NeQuick N message model.
         * @since 14.0
         */
        NAVIC_NEQUICK_N_LINE_1((header, line) -> true,
                               pi -> {
                                   pi.getNavICNeQuickN().getRegion1().setAi0(pi.parseDouble1(IonosphereAij.SFU));
                                   pi.getNavICNeQuickN().getRegion1().setAi1(pi.parseDouble2(IonosphereAij.SFU_PER_DEG));
                                   pi.getNavICNeQuickN().getRegion1().setAi2(pi.parseDouble3(IonosphereAij.SFU_PER_DEG2));
                                   pi.getNavICNeQuickN().getRegion1().setIDF(pi.parseDouble4(Unit.ONE));
                               },
                               pi -> Collections.singleton(NAVIC_NEQUICK_N_LINE_2)),

        /** Parser for NacIV NeQuick N message model.
         * @since 14.0
         */
        NAVIC_NEQUICK_N_LINE_0((header, line) -> true,
                               pi -> {
                                   pi.getNavICNeQuickN().setTransmitTime(pi.parseDate(pi.getLine(), pi.getNavICNeQuickN().getSystem()));
                                   pi.getNavICNeQuickN().setIOD(pi.parseDouble2(Unit.ONE));
                               },
                               pi -> Collections.singleton(NAVIC_NEQUICK_N_LINE_1)),

        /** Parser for GLONASS CDMS message model.
         * @since 14.0
         */
        GLONASS_CDMS_LINE_0((header, line) -> true,
                            pi -> {
                                pi.getGlonassCdms().setTransmitTime(pi.parseDate(pi.getLine(), pi.getGlonassCdms().getSystem()));
                                pi.getGlonassCdms().setCA(pi.parseDouble2(   Unit.ONE));
                                pi.getGlonassCdms().setCF107(pi.parseDouble3(Unit.ONE));
                                pi.getGlonassCdms().setCAP(pi.parseDouble4(  Unit.ONE));
                                pi.finishGlonassCdms();
                            },
                            LineParser::navigationNext),

        /** Parser for ionosphere Nequick-G message model. */
        NEQUICK_G_LINE_1((header, line) -> true,
                         pi -> {
                             pi.getNeQuickG().setFlags((int) FastMath.rint(ParsingUtils.parseDouble(pi.getLine(), 4, 19)));
                             pi.finishNequickG();
                         },
                         LineParser::navigationNext),

        /** Parser for ionosphere Nequick-G message model. */
        NEQUICK_G_LINE_0((header, line) -> true,
                         pi -> {
                             pi.getNeQuickG().setTransmitTime(pi.parseDate(pi.getLine(), pi.getNeQuickG().getSystem()));
                             pi.getNeQuickG().getAij().setAi0(pi.parseDouble2(IonosphereAij.SFU));
                             pi.getNeQuickG().getAij().setAi1(pi.parseDouble3(IonosphereAij.SFU_PER_DEG));
                             pi.getNeQuickG().getAij().setAi2(pi.parseDouble4(IonosphereAij.SFU_PER_DEG2));
                         },
                         pi -> Collections.singleton(NEQUICK_G_LINE_1)),

        /** Parser for ionosphere BDGIM message model. */
        BDGIM_LINE_2((header, line) -> true,
                     pi -> {
                         pi.getBdgim().setAlphaI(7, pi.parseDouble1(TEC));
                         pi.getBdgim().setAlphaI(8, pi.parseDouble2(TEC));
                         pi.finishBdgim();
                     },
                     LineParser::navigationNext),

        /** Parser for ionosphere BDGIM message model. */
        BDGIM_LINE_1((header, line) -> true,
                     pi -> {
                         pi.getBdgim().setAlphaI(3, pi.parseDouble1(TEC));
                         pi.getBdgim().setAlphaI(4, pi.parseDouble2(TEC));
                         pi.getBdgim().setAlphaI(5, pi.parseDouble3(TEC));
                         pi.getBdgim().setAlphaI(6, pi.parseDouble4(TEC));
                     },
                     pi -> Collections.singleton(BDGIM_LINE_2)),

        /** Parser for ionosphere BDGIM message model. */
        BDGIM_LINE_0((header, line) -> true,
                     pi -> {
                         pi.getBdgim().setTransmitTime(pi.parseDate(pi.getLine(), pi.getBdgim().getSystem()));
                         pi.getBdgim().setAlphaI(0, pi.parseDouble2(TEC));
                         pi.getBdgim().setAlphaI(1, pi.parseDouble3(TEC));
                         pi.getBdgim().setAlphaI(2, pi.parseDouble4(TEC));
                     },
                     pi -> Collections.singleton(BDGIM_LINE_1)),

        /** Parser for ionosphere message type. */
        IONO_TYPE((header, line) -> line.startsWith("> ION"),
                  pi -> {
                      final SatelliteSystem system = SatelliteSystem.parseSatelliteSystem(
                          ParsingUtils.parseString(pi.getLine(), 6, 1));
                      final int             prn     = ParsingUtils.parseInt(pi.getLine(), 7, 2);
                      final String          type    = ParsingUtils.parseString(pi.getLine(), 10, 4);
                      final String          subtype = ParsingUtils.parseString(pi.getLine(), 15, 4);
                      if (system == SatelliteSystem.GALILEO) {
                          pi.setNeQuickG(new IonosphereNequickGMessage(system, prn, type, subtype));
                      } else if (system == SatelliteSystem.BEIDOU && "CNVX".equals(type)) {
                          // in Rinex 4.00, tables A32 and A34 (A35 and A37 in Rinex 4.02) are ambiguous
                          // as both seem to apply to Beidou CNVX messages; we consider BDGIM is the
                          // proper model in this case
                          pi.setBdgim(new IonosphereBDGIMMessage(system, prn, type, subtype));
                      } else if (system == SatelliteSystem.NAVIC &&
                                 NavICL1NvNavigationMessage.L1NV.equals(type) &&
                                 "KLOB".equals(subtype)) {
                          pi.setNavICKlobuchar(new IonosphereNavICKlobucharMessage(system, prn, type, subtype));
                      } else if (system == SatelliteSystem.NAVIC &&
                                 NavICL1NvNavigationMessage.L1NV.equals(type) &&
                                 "NEQN".equals(subtype)) {
                          pi.setNavICNeQuickN(new IonosphereNavICNeQuickNMessage(system, prn, type, subtype));
                      } else if (system == SatelliteSystem.GLONASS) {
                          pi.setGlonassCdms(new IonosphereGlonassCdmsMessage(system, prn, type, subtype));
                      } else  {
                          pi.setKlobuchar(new IonosphereKlobucharMessage(system, prn, type, subtype));
                      }
                  },
                  LineParser::ionosphereNext);

        /** Predicate for identifying lines that can be parsed. */
        private final BiFunction<RinexNavigationHeader, String, Boolean> canHandle;

        /** Parsing method. */
        private final Consumer<ParseInfo> parsingMethod;

        /** Provider for next line parsers. */
        private final Function<ParseInfo, Iterable<LineParser>> allowedNextProvider;

        /** Simple constructor.
         * @param canHandle predicate for identifying lines that can be parsed
         * @param parsingMethod parsing method
         * @param allowedNextProvider supplier for allowed parsers for next line
         */
        LineParser(final BiFunction<RinexNavigationHeader, String, Boolean> canHandle,
                   final Consumer<ParseInfo> parsingMethod,
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
            if (parseInfo.getHeader().getFormatVersion() < 3) {
                // Rinex 2.x header entries
                return Arrays.asList(HEADER_COMMENT, HEADER_PROGRAM,
                                     HEADER_ION_ALPHA, HEADER_ION_BETA,
                                     HEADER_DELTA_UTC, HEADER_CORR_SYSTEM_TIME,
                                     HEADER_LEAP_SECONDS, HEADER_END);
            } else if (parseInfo.getHeader().getFormatVersion() < 4) {
                // Rinex 3.x header entries
                return Arrays.asList(HEADER_COMMENT, HEADER_PROGRAM,
                                     HEADER_IONOSPHERIC, HEADER_TIME,
                                     HEADER_LEAP_SECONDS, HEADER_END);
            } else {
                // Rinex 4.x header entries
                return Arrays.asList(HEADER_COMMENT, HEADER_PROGRAM,
                                     HEADER_DOI, HEADER_LICENSE, HEADER_STATION_INFORMATION, HEADER_MERGED_FILE,
                                     HEADER_LEAP_SECONDS, HEADER_END);
            }
        }

        /** Get the allowed parsers for next lines while parsing navigation date.
         * @param parseInfo holder for transient data
         * @return allowed parsers for next line
         */
        private static Iterable<LineParser> navigationNext(final ParseInfo parseInfo) {
            final MessageLineParser mlp = parseInfo.getMessageLineParser();
            if (mlp != null && mlp.getType() == MessageType.ORBIT) {
                if (mlp instanceof GlonassFdmaParser) {
                    // workaround for some invalid files that should nevertheless be parsed
                    // we have encountered in the wild merged files that claimed to be in 3.05 version
                    // and hence needed at least 4 broadcast GLONASS orbit lines (the fourth line was
                    // introduced in 3.05), but in fact only had 3 broadcast lines. We think they were
                    // merged from files in 3.04 or earlier format. In order to parse these files,
                    // we accept after the third line either another broadcast orbit line or a new message
                    if (parseInfo.getMessageLineNumber() < 3) {
                        return Collections.singleton(MESSAGE_LINE);
                    } else {
                        if (parseInfo.getHeader().getFormatVersion() < 4) {
                            return Arrays.asList(MESSAGE_LINE, NAVIGATION_SV_EPOCH_CLOCK);
                        } else {
                            return Arrays.asList(MESSAGE_LINE, EPH_TYPE, STO_TYPE, EOP_TYPE, IONO_TYPE);
                        }
                    }
                } else {
                    return Collections.singleton(MESSAGE_LINE);
                }
            } else if (parseInfo.getHeader().getFormatVersion() < 3) {
                return Collections.singleton(NAVIGATION_SV_EPOCH_CLOCK_RINEX_2);
            } else if (parseInfo.getHeader().getFormatVersion() < 4) {
                return Collections.singleton(NAVIGATION_SV_EPOCH_CLOCK);
            } else {
                return Arrays.asList(EPH_TYPE, STO_TYPE, EOP_TYPE, IONO_TYPE);
            }
        }

        /** Get the allowed parsers for next lines while parsing ionospheric model date.
         * @param parseInfo holder for transient data
         * @return allowed parsers for next line
         */
        private static Iterable<LineParser> ionosphereNext(final ParseInfo parseInfo) {
            if (parseInfo.getNeQuickG() != null) {
                return Collections.singleton(NEQUICK_G_LINE_0);
            } else if (parseInfo.getBdgim() != null) {
                return Collections.singleton(BDGIM_LINE_0);
            } else if (parseInfo.getKlobuchar() != null) {
                return Collections.singleton(KLOBUCHAR_LINE_0);
            } else if (parseInfo.getNavICKlobuchar() != null) {
                return Collections.singleton(NAVIC_KLOBUCHAR_LINE_0);
            } else if (parseInfo.getNavICNeQuickN() != null) {
                return Collections.singleton(NAVIC_NEQUICK_N_LINE_0);
            } else if (parseInfo.getGlonassCdms() != null) {
                return Collections.singleton(GLONASS_CDMS_LINE_0);
            } else {
                return Collections.emptyList();
            }
        }

    }

}
