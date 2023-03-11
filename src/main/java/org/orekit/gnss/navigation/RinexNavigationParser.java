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
package org.orekit.gnss.navigation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.InputMismatchException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.hipparchus.util.FastMath;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.gnss.RinexUtils;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.gnss.navigation.RinexNavigation.TimeSystemCorrection;
import org.orekit.propagation.analytical.gnss.data.BeidouNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.CivilianNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.GLONASSNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.GPSCivilianNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.GPSLegacyNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.GalileoNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.IRNSSNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.LegacyNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.QZSSCivilianNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.QZSSLegacyNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.SBASNavigationMessage;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.GNSSDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScales;
import org.orekit.utils.Constants;
import org.orekit.utils.units.Unit;

/**
 * Parser for RINEX navigation messages files.
 * <p>
 * This parser handles RINEX version from 3.01 to 4.00. It is not adapted for RINEX 2.10 and 2.11 versions.
 * </p>
 * @see <a href="https://files.igs.org/pub/data/format/rinex301.pdf"> 3.01 navigation messages file format</a>
 * @see <a href="https://files.igs.org/pub/data/format/rinex302.pdf"> 3.02 navigation messages file format</a>
 * @see <a href="https://files.igs.org/pub/data/format/rinex303.pdf"> 3.03 navigation messages file format</a>
 * @see <a href="https://files.igs.org/pub/data/format/rinex304.pdf"> 3.04 navigation messages file format</a>
 * @see <a href="https://files.igs.org/pub/data/format/rinex305.pdf"> 3.05 navigation messages file format</a>
 * @see <a href="https://files.igs.org/pub/data/format/rinex_4.00.pdf"> 4.00 navigation messages file format</a>
 *
 * @author Bryan Cazabonne
 * @since 11.0
 *
 */
public class RinexNavigationParser {

    /** Converter for positions. */
    private static final Unit KM = Unit.KILOMETRE;

    /** Converter for velocities. */
    private static final Unit KM_PER_S = Unit.parse("km/s");

    /** Converter for accelerations. */
    private static final Unit KM_PER_S2 = Unit.parse("km/s²");;

    /** Converter for velocities. */
    private static final Unit M_PER_S = Unit.parse("m/s");

    /** Converter for GLONASS τₙ. */
    private static final Unit MINUS_SECONDS = Unit.parse("-1s");

    /** Converter for clock drift. */
    private static final Unit S_PER_S = Unit.parse("s/s");

    /** Converter for clock drift rate. */
    private static final Unit S_PER_S2 = Unit.parse("s/s²");

    /** Converter for square root of semi-major axis. */
    private static final Unit SQRT_M = Unit.parse("√m");

    /** Converter for angular rates. */
    private static final Unit RAD_PER_S = Unit.parse("rad/s");;

    /** Converter for angular accelerations. */
    private static final Unit RAD_PER_S2 = Unit.parse("rad/s²");;

    /** Indicator for CNV1 messages. */
    private static final String CNV1 = "CNV1";

    /** Indicator for CNV2 messages. */
    private static final String CNV2 = "CNV2";

    /** Indicator for CNV3 messages. */
    private static final String CNV3 = "CNV3";

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
        final ParseInfo pi = new ParseInfo(source.getName());

        Stream<LineParser> candidateParsers = Stream.of(LineParser.HEADER_VERSION);
        try (Reader reader = source.getOpener().openReaderOnce();
             BufferedReader br = new BufferedReader(reader)) {
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                ++pi.lineNumber;
                final String l = line;
                final Optional<LineParser> selected = candidateParsers.filter(p -> p.canHandle.test(l)).findFirst();
                if (selected.isPresent()) {
                    try {
                        selected.get().parsingMethod.parse(line, pi);
                    } catch (StringIndexOutOfBoundsException | NumberFormatException | InputMismatchException e) {
                        throw new OrekitException(e,
                                                  OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                  pi.lineNumber, source.getName(), line);
                    }
                    candidateParsers = selected.get().allowedNextProvider.apply(pi);
                } else {
                    throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                              pi.lineNumber, source.getName(), line);
                }
            }
        }

        return pi.file;

    }

    /** Transient data used for parsing a RINEX navigation messages file. */
    private class ParseInfo {

        /** Name of the data source. */
        private final String name;

        /** Set of time scales for parsing dates. */
        private final TimeScales timeScales;

        /** The corresponding navigation messages file object. */
        private RinexNavigation file;

        /** Flag indicating the distinction between "alpha" and "beta" ionospheric coefficients. */
        private boolean isIonosphereAlphaInitialized;

        /** Satellite system line parser. */
        private SatelliteSystemLineParser systemLineParser;

        /** Current global line number. */
        private int lineNumber;

        /** Current line number within the navigation message. */
        private int messageLineNumber;

        /** Container for GPS navigation message. */
        private GPSLegacyNavigationMessage gpsLNav;

        /** Container for GPS navigation message. */
        private GPSCivilianNavigationMessage gpsCNav;

        /** Container for Galileo navigation message. */
        private GalileoNavigationMessage galileoNav;

        /** Container for Beidou navigation message. */
        private BeidouNavigationMessage beidouNav;

        /** Container for QZSS navigation message. */
        private QZSSLegacyNavigationMessage qzssLNav;

        /** Container for QZSS navigation message. */
        private QZSSCivilianNavigationMessage qzssCNav;

        /** Container for IRNSS navigation message. */
        private IRNSSNavigationMessage irnssNav;

        /** Container for GLONASS navigation message. */
        private GLONASSNavigationMessage glonassNav;

        /** Container for SBAS navigation message. */
        private SBASNavigationMessage sbasNav;

        /** Constructor, build the ParseInfo object.
         * @param name name of the data source
         */
        ParseInfo(final String name) {
            // Initialize default values for fields
            this.name                         = name;
            this.timeScales                   = RinexNavigationParser.this.timeScales;
            this.isIonosphereAlphaInitialized = false;
            this.file                         = new RinexNavigation();
            this.systemLineParser             = SatelliteSystemLineParser.LNAV;
            this.lineNumber                   = 0;
            this.messageLineNumber            = 0;
            this.gpsLNav                      = new GPSLegacyNavigationMessage();
            this.gpsCNav                      = new GPSCivilianNavigationMessage();
            this.galileoNav                   = new GalileoNavigationMessage();
            this.beidouNav                    = new BeidouNavigationMessage();
            this.qzssLNav                     = new QZSSLegacyNavigationMessage();
            this.qzssCNav                     = new QZSSCivilianNavigationMessage();
            this.irnssNav                     = new IRNSSNavigationMessage();
            this.glonassNav                   = new GLONASSNavigationMessage();
            this.sbasNav                      = new SBASNavigationMessage();
        }

    }

    /** Parsers for specific lines. */
    private enum LineParser {

        /** Parser for version, file type and satellite system. */
        HEADER_VERSION(line -> RinexUtils.matchesLabel(line, "RINEX VERSION / TYPE"),
                       (line, pi) -> RinexUtils.parseVersionFileTypeSatelliteSystem(line, pi.name, pi.file.getHeader(),
                                                                                    3.01, 3.02, 3.03, 3.04, 3.05, 4.00),
                       LineParser::headerNext),

        /** Parser for generating program and emitting agency. */
        HEADER_PROGRAM(line -> RinexUtils.matchesLabel(line, "PGM / RUN BY / DATE"),
                       (line, pi) -> RinexUtils.parseProgramRunByDate(line, pi.lineNumber, pi.name, pi.timeScales, pi.file.getHeader()),
                       LineParser::headerNext),

        /** Parser for comments. */
        HEADER_COMMENT(line -> RinexUtils.matchesLabel(line, "COMMENT"),
                       (line, pi) -> RinexUtils.parseComment(line, pi.file.getHeader()),
                       LineParser::headerNext),

        /** Parser for ionospheric correction parameters. */
        HEADER_IONOSPHERIC(line -> RinexUtils.matchesLabel(line, "IONOSPHERIC CORR"),
                           (line, pi) -> {

                               // Satellite system
                               final String ionoType = RinexUtils.parseString(line, 0, 3);
                               pi.file.getHeader().setIonosphericCorrectionType(ionoType);

                               // Read coefficients
                               final double[] parameters = new double[4];
                               parameters[0] = RinexUtils.parseDouble(line, 5,  12);
                               parameters[1] = RinexUtils.parseDouble(line, 17, 12);
                               parameters[2] = RinexUtils.parseDouble(line, 29, 12);
                               parameters[3] = RinexUtils.parseDouble(line, 41, 12);

                               // Verify if we are parsing Galileo ionospheric parameters
                               if ("GAL".equals(ionoType)) {

                                   // We are parsing Galileo ionospheric parameters
                                   pi.file.setNeQuickAlpha(parameters);

                               } else {
                                   // We are parsing Klobuchar ionospheric parameters

                                   // Verify if we are parsing "alpha" or "beta" ionospheric parameters
                                   if (pi.isIonosphereAlphaInitialized) {

                                       // Ionospheric "beta" parameters
                                       pi.file.setKlobucharBeta(parameters);

                                   } else {

                                       // Ionospheric "alpha" parameters
                                       pi.file.setKlobucharAlpha(parameters);

                                       // Set the flag to true
                                       pi.isIonosphereAlphaInitialized = true;

                                   }

                               }

                           },
                           LineParser::headerNext),

        /** Parser for corrections to transform the system time to UTC or to other time systems. */
        HEADER_TIME(line -> RinexUtils.matchesLabel(line, "TIME SYSTEM CORR"),
                    (line, pi) -> {

                        // Read fields
                        final String type    = RinexUtils.parseString(line, 0,  4);
                        final double a0      = RinexUtils.parseDouble(line, 5,  17);
                        final double a1      = RinexUtils.parseDouble(line, 22, 16);
                        final int    refTime = RinexUtils.parseInt(line, 38, 7);
                        final int    refWeek = RinexUtils.parseInt(line, 46, 5);

                        // Add to the list
                        final TimeSystemCorrection tsc = new TimeSystemCorrection(type, a0, a1, refTime, refWeek);
                        pi.file.getHeader().addTimeSystemCorrections(tsc);

                    },
                    LineParser::headerNext),

        /** Parser for leap seconds. */
        HEADER_LEAP_SECONDS(line -> RinexUtils.matchesLabel(line, "LEAP SECONDS"),
                            (line, pi) -> pi.file.getHeader().setNumberOfLeapSeconds(RinexUtils.parseInt(line, 0, 6)),
                            LineParser::headerNext),

        /** Parser for DOI.
         * @since 12.0
         */
        HEADER_DOI(line -> RinexUtils.matchesLabel(line, "DOI"),
                            (line, pi) -> pi.file.getHeader().setDoi(RinexUtils.parseString(line, 0, RinexUtils.LABEL_INDEX)),
                            LineParser::headerNext),

        /** Parser for license.
         * @since 12.0
         */
        HEADER_LICENSE(line -> RinexUtils.matchesLabel(line, "LICENSE OF USE"),
                            (line, pi) -> pi.file.getHeader().setLicense(RinexUtils.parseString(line, 0, RinexUtils.LABEL_INDEX)),
                            LineParser::headerNext),

        /** Parser for stationInformation.
         * @since 12.0
         */
        HEADER_STATION_INFORMATION(line -> RinexUtils.matchesLabel(line, "STATION INFORMATION"),
                            (line, pi) -> pi.file.getHeader().setStationInformation(RinexUtils.parseString(line, 0, RinexUtils.LABEL_INDEX)),
                            LineParser::headerNext),

        /** Parser for merged files.
         * @since 12.0
         */
        HEADER_MERGED_FILE(line -> RinexUtils.matchesLabel(line, "MERGED FILE"),
                            (line, pi) -> pi.file.getHeader().setMergedFiles(RinexUtils.parseInt(line, 0, 9)),
                            LineParser::headerNext),

       /** Parser for the end of header. */
        HEADER_END(line -> RinexUtils.matchesLabel(line, "END OF HEADER"),
                   (line, pi) -> {
                       // get rinex format version
                       final RinexNavigationHeader header = pi.file.getHeader();
                       final double version = header.getFormatVersion();

                       // check mandatory header fields
                       if (version < 4) {
                           if (header.getRunByName() == null) {
                               throw new OrekitException(OrekitMessages.INCOMPLETE_HEADER, pi.name);
                           }
                       } else {
                           if (header.getRunByName() == null ||
                               header.getNumberOfLeapSeconds() < 0) {
                               throw new OrekitException(OrekitMessages.INCOMPLETE_HEADER, pi.name);
                           }
                       }
                   },
                   LineParser::navigationNext),

        /** Parser for navigation message space vehicle epoch and clock. */
        NAVIGATION_SV_EPOCH_CLOCK(line -> "GRECIJS".indexOf(line.charAt(0)) >= 0,
                                 (line, pi) -> {

                                     // Set the line number to 0
                                     pi.messageLineNumber = 0;

                                     if (pi.file.getHeader().getFormatVersion() < 4) {
                                         // Current satellite system
                                         final SatelliteSystem system = SatelliteSystem.parseSatelliteSystem(RinexUtils.parseString(line, 0, 1));

                                         // Initialize parser
                                         pi.systemLineParser = SatelliteSystemLineParser.getParser(system, null, pi, line);
                                     }

                                     // Read first line
                                     pi.systemLineParser.parseSvEpochSvClockLine(line, pi);

                                 },
                                 LineParser::navigationNext),

        /** Parser for navigation message type. */
        NAVIGATION_TYPE(line -> line.startsWith("> EPH"),
                        (line, pi) -> {
                            final SatelliteSystem system = SatelliteSystem.parseSatelliteSystem(RinexUtils.parseString(line, 6, 1));
                            final String          type   = RinexUtils.parseString(line, 10, 4);
                            pi.systemLineParser = SatelliteSystemLineParser.getParser(system, type, pi, line);
                        },
                        pi -> Stream.of(NAVIGATION_SV_EPOCH_CLOCK)),

        /** Parser for broadcast orbit. */
        NAVIGATION_BROADCAST_ORBIT(line -> line.startsWith("    "),
                                   (line, pi) -> {

                                       // Increment the line number
                                       pi.messageLineNumber++;

                                       // Read the corresponding line
                                       if (pi.messageLineNumber == 1) {
                                           // BROADCAST ORBIT – 1
                                           pi.systemLineParser.parseFirstBroadcastOrbit(line, pi);
                                       } else if (pi.messageLineNumber == 2) {
                                           // BROADCAST ORBIT – 2
                                           pi.systemLineParser.parseSecondBroadcastOrbit(line, pi);
                                       } else if (pi.messageLineNumber == 3) {
                                           // BROADCAST ORBIT – 3
                                           pi.systemLineParser.parseThirdBroadcastOrbit(line, pi);
                                       } else if (pi.messageLineNumber == 4) {
                                           // BROADCAST ORBIT – 4
                                           pi.systemLineParser.parseFourthBroadcastOrbit(line, pi);
                                       } else if (pi.messageLineNumber == 5) {
                                           // BROADCAST ORBIT – 5
                                           pi.systemLineParser.parseFifthBroadcastOrbit(line, pi);
                                       } else if (pi.messageLineNumber == 6) {
                                           // BROADCAST ORBIT – 6
                                           pi.systemLineParser.parseSixthBroadcastOrbit(line, pi);
                                       } else if (pi.messageLineNumber == 7) {
                                           // BROADCAST ORBIT – 7
                                           pi.systemLineParser.parseSeventhBroadcastOrbit(line, pi);
                                       } else if (pi.messageLineNumber == 8) {
                                           // BROADCAST ORBIT – 8
                                           pi.systemLineParser.parseEighthBroadcastOrbit(line, pi);
                                       } else {
                                           // BROADCAST ORBIT – 9
                                           pi.systemLineParser.parseNinththBroadcastOrbit(line, pi);
                                       }

                                   },
                                   LineParser::navigationNext);

        /** Predicate for identifying lines that can be parsed. */
        private final Predicate<String> canHandle;

        /** Parsing method. */
        private final ParsingMethod parsingMethod;

        /** Provider for next line parsers. */
        private final Function<ParseInfo, Stream<LineParser>> allowedNextProvider;

        /** Simple constructor.
         * @param canHandle predicate for identifying lines that can be parsed
         * @param parsingMethod parsing method
         * @param allowedNextProvider supplier for allowed parsers for next line
         */
        LineParser(final Predicate<String> canHandle, final ParsingMethod parsingMethod,
                   final Function<ParseInfo, Stream<LineParser>> allowedNextProvider) {
            this.canHandle           = canHandle;
            this.parsingMethod       = parsingMethod;
            this.allowedNextProvider = allowedNextProvider;
        }

        /** Get the allowed parsers for next lines while parsing Rinex header.
         * @param parseInfo holder for transient data
         * @return allowed parsers for next line
         */
        private static Stream<LineParser> headerNext(final ParseInfo parseInfo) {
            if (parseInfo.file.getHeader().getFormatVersion() < 4) {
                // Rinex 3.x header entries
                return Stream.of(HEADER_COMMENT, HEADER_PROGRAM,
                                 HEADER_IONOSPHERIC, HEADER_TIME,
                                 HEADER_LEAP_SECONDS, HEADER_END);
            } else {
                // Rinex 4.x header entries
                return Stream.of(HEADER_COMMENT, HEADER_PROGRAM,
                                 HEADER_DOI, HEADER_LICENSE, HEADER_STATION_INFORMATION, HEADER_MERGED_FILE,
                                 HEADER_LEAP_SECONDS, HEADER_END);
            }
        }

        /** Get the allowed parsers for next lines while parsing navigation date.
         * @param parseInfo holder for transient data
         * @return allowed parsers for next line
         */
        private static Stream<LineParser> navigationNext(final ParseInfo parseInfo) {
            if (parseInfo.file.getHeader().getFormatVersion() < 4) {
                return Stream.of(NAVIGATION_SV_EPOCH_CLOCK, NAVIGATION_BROADCAST_ORBIT);
            } else {
                return Stream.of(NAVIGATION_TYPE, NAVIGATION_SV_EPOCH_CLOCK, NAVIGATION_BROADCAST_ORBIT);
            }
        }

    }

    /** Parsers for satellite system specific lines. */
    private enum SatelliteSystemLineParser {

        /** GPS legacy. */
        LNAV() {

            /** {@inheritDoc} */
            @Override
            public void parseSvEpochSvClockLine(final String line, final ParseInfo pi) {
                parseSvEpochSvClockLine(line,
                                        pi.gpsLNav::setPRN,
                                        pi.gpsLNav::setEpochToc, pi.timeScales.getGPS(),
                                        pi.gpsLNav::setAf0, Unit.SECOND,
                                        pi.gpsLNav::setAf1, S_PER_S,
                                        pi.gpsLNav::setAf2, S_PER_S2);
            }

            /** {@inheritDoc} */
            @Override
            public void parseFirstBroadcastOrbit(final String line, final ParseInfo pi) {
                parseLine(line,
                          pi.gpsLNav::setIODE,   Unit.SECOND,
                          pi.gpsLNav::setCrs,    Unit.METRE,
                          pi.gpsLNav::setDeltaN, RAD_PER_S,
                          pi.gpsLNav::setM0,     Unit.RADIAN,
                          null);
            }

            /** {@inheritDoc} */
            @Override
            public void parseSecondBroadcastOrbit(final String line, final ParseInfo pi) {
                parseLine(line,
                          pi.gpsLNav::setCuc,   Unit.RADIAN,
                          pi.gpsLNav::setE,     Unit.NONE,
                          pi.gpsLNav::setCus,   Unit.RADIAN,
                          pi.gpsLNav::setSqrtA, SQRT_M,
                          null);
            }

            /** {@inheritDoc} */
            @Override
            public void parseThirdBroadcastOrbit(final String line, final ParseInfo pi) {
                parseLine(line,
                          pi.gpsLNav::setTime,   Unit.SECOND,
                          pi.gpsLNav::setCic,    Unit.RADIAN,
                          pi.gpsLNav::setOmega0, Unit.RADIAN,
                          pi.gpsLNav::setCis,    Unit.RADIAN,
                          null);
            }

            /** {@inheritDoc} */
            @Override
            public void parseFourthBroadcastOrbit(final String line, final ParseInfo pi) {
                parseLine(line,
                          pi.gpsLNav::setI0,       Unit.RADIAN,
                          pi.gpsLNav::setCrc,      Unit.METRE,
                          pi.gpsLNav::setPa,       Unit.RADIAN,
                          pi.gpsLNav::setOmegaDot, RAD_PER_S,
                          null);
            }

            /** {@inheritDoc} */
            @Override
            public void parseFifthBroadcastOrbit(final String line, final ParseInfo pi) {
                // iDot
                pi.gpsLNav.setIDot(RinexUtils.parseDouble(line, 4, 19));
                // Codes on L2 channel (ignored)
                // RinexUtils.parseDouble(line, 23, 19)
                // GPS week (to go with Toe)
                pi.gpsLNav.setWeek((int) RinexUtils.parseDouble(line, 42, 19));
                pi.gpsLNav.setDate(new GNSSDate(pi.gpsLNav.getWeek(),
                                               pi.gpsLNav.getTime(),
                                               SatelliteSystem.GPS,
                                               pi.timeScales).getDate());
            }

            /** {@inheritDoc} */
            @Override
            public void parseSixthBroadcastOrbit(final String line, final ParseInfo pi) {
                parseLine(line,
                          pi.gpsLNav::setSvAccuracy, Unit.METRE,
                          pi.gpsLNav::setSvHealth,   Unit.NONE,
                          pi.gpsLNav::setTGD,        Unit.SECOND,
                          pi.gpsLNav::setIODC,       Unit.SECOND,
                          null);
            }

            /** {@inheritDoc} */
            @Override
            public void parseSeventhBroadcastOrbit(final String line, final ParseInfo pi) {
                parseLine(line,
                          null, Unit.NONE, // TODO: transmission time
                          null, Unit.NONE,
                          null, Unit.NONE,
                          null, Unit.NONE,
                          () -> {
                              pi.file.addGPSNavigationMessage(pi.gpsLNav);
                              pi.gpsLNav = new GPSLegacyNavigationMessage();
                          });
            }

        },

        /** GPS civilian.
         * @since 12.0
         */
        CNAV() {

            /** {@inheritDoc} */
            @Override
            public void parseSvEpochSvClockLine(final String line, final ParseInfo pi) {
                parseSvEpochSvClockLine(line,
                                        pi.gpsCNav::setPRN,
                                        pi.gpsCNav::setEpochToc, pi.timeScales.getGPS(),
                                        pi.gpsCNav::setAf0, Unit.SECOND,
                                        pi.gpsCNav::setAf1, S_PER_S,
                                        pi.gpsCNav::setAf2, S_PER_S2);
            }

            /** {@inheritDoc} */
            @Override
            public void parseFirstBroadcastOrbit(final String line, final ParseInfo pi) {
                parseLine(line,
                          pi.gpsCNav::setADot,   M_PER_S,
                          pi.gpsCNav::setCrs,    Unit.METRE,
                          pi.gpsCNav::setDeltaN, RAD_PER_S,
                          pi.gpsCNav::setM0,     Unit.RADIAN,
                          null);
            }

            /** {@inheritDoc} */
            @Override
            public void parseSecondBroadcastOrbit(final String line, final ParseInfo pi) {
                parseLine(line,
                          pi.gpsCNav::setCuc,   Unit.RADIAN,
                          pi.gpsCNav::setE,     Unit.NONE,
                          pi.gpsCNav::setCus,   Unit.RADIAN,
                          pi.gpsCNav::setSqrtA, SQRT_M,
                          null);
            }

            /** {@inheritDoc} */
            @Override
            public void parseThirdBroadcastOrbit(final String line, final ParseInfo pi) {
                parseLine(line,
                          pi.gpsCNav::setTime,   Unit.SECOND,
                          pi.gpsCNav::setCic,    Unit.RADIAN,
                          pi.gpsCNav::setOmega0, Unit.RADIAN,
                          pi.gpsCNav::setCis,    Unit.RADIAN,
                          null);
            }

            /** {@inheritDoc} */
            @Override
            public void parseFourthBroadcastOrbit(final String line, final ParseInfo pi) {
                parseLine(line,
                          pi.gpsCNav::setI0,       Unit.RADIAN,
                          pi.gpsCNav::setCrc,      Unit.METRE,
                          pi.gpsCNav::setPa,       Unit.RADIAN,
                          pi.gpsCNav::setOmegaDot, RAD_PER_S,
                          null);
            }

            /** {@inheritDoc} */
            @Override
            public void parseFifthBroadcastOrbit(final String line, final ParseInfo pi) {
                parseLine(line,
                          pi.gpsCNav::setIDot,       RAD_PER_S,
                          pi.gpsCNav::setDeltaN0Dot, RAD_PER_S2,
                          pi.gpsCNav::setUraiNed0,   Unit.NONE,
                          pi.gpsCNav::setUraiNed1,   Unit.NONE,
                          null);
            }

            /** {@inheritDoc} */
            @Override
            public void parseSixthBroadcastOrbit(final String line, final ParseInfo pi) {
                parseLine(line,
                          pi.gpsCNav::setSvAccuracy, Unit.NONE,
                          pi.gpsCNav::setSvHealth,   Unit.NONE,
                          pi.gpsCNav::setTGD,        Unit.SECOND,
                          pi.gpsCNav::setUraiNed2,   Unit.NONE,
                          null);
            }

            /** {@inheritDoc} */
            @Override
            public void parseSeventhBroadcastOrbit(final String line, final ParseInfo pi) {
                parseLine(line,
                          pi.gpsCNav::setIscL1CA, Unit.SECOND,
                          pi.gpsCNav::setIscL2C,  Unit.SECOND,
                          pi.gpsCNav::setIscL5I5, Unit.SECOND,
                          pi.gpsCNav::setIscL5Q5, Unit.SECOND,
                          null);
            }

            /** {@inheritDoc} */
            @Override
            public void parseEighthBroadcastOrbit(final String line, final ParseInfo pi) {
                parseLine(line,
                          null, Unit.NONE, // TODO: transmission time
                          null, Unit.NONE,
                          null, Unit.NONE,
                          null, Unit.NONE,
                          () -> {
                              pi.file.addGPSNavigationMessage(pi.gpsCNav);
                              pi.gpsCNav = new GPSCivilianNavigationMessage();
                          });
            }

        },

        /** Galileo. */
        GALILEO() {

            /** {@inheritDoc} */
            @Override
            public void parseSvEpochSvClockLine(final String line, final ParseInfo pi) {
                parseSvEpochSvClockLine(line,
                                        pi.galileoNav::setPRN,
                                        pi.galileoNav::setEpochToc, pi.timeScales.getGPS(),
                                        pi.galileoNav::setAf0, Unit.SECOND,
                                        pi.galileoNav::setAf1, S_PER_S,
                                        pi.galileoNav::setAf2, S_PER_S2);
            }

            /** {@inheritDoc} */
            @Override
            public void parseFirstBroadcastOrbit(final String line, final ParseInfo pi) {
                parseLine(line,
                          pi.galileoNav::setIODNav, Unit.SECOND,
                          pi.galileoNav::setCrs,    Unit.METRE,
                          pi.galileoNav::setDeltaN, RAD_PER_S,
                          pi.galileoNav::setM0,     Unit.RADIAN,
                          null);
            }

            /** {@inheritDoc} */
            @Override
            public void parseSecondBroadcastOrbit(final String line, final ParseInfo pi) {
                parseLine(line,
                          pi.galileoNav::setCuc,   Unit.RADIAN,
                          pi.galileoNav::setE,     Unit.NONE,
                          pi.galileoNav::setCus,   Unit.RADIAN,
                          pi.galileoNav::setSqrtA, SQRT_M,
                          null);
            }

            /** {@inheritDoc} */
            @Override
            public void parseThirdBroadcastOrbit(final String line, final ParseInfo pi) {
                parseLine(line,
                          pi.galileoNav::setTime,   Unit.SECOND,
                          pi.galileoNav::setCic,    Unit.RADIAN,
                          pi.galileoNav::setOmega0, Unit.RADIAN,
                          pi.galileoNav::setCis,    Unit.RADIAN,
                          null);
            }

            /** {@inheritDoc} */
            @Override
            public void parseFourthBroadcastOrbit(final String line, final ParseInfo pi) {
                parseLine(line,
                          pi.galileoNav::setI0,       Unit.RADIAN,
                          pi.galileoNav::setCrc,      Unit.METRE,
                          pi.galileoNav::setPa,       Unit.RADIAN,
                          pi.galileoNav::setOmegaDot, RAD_PER_S,
                          null);
            }

            /** {@inheritDoc} */
            @Override
            public void parseFifthBroadcastOrbit(final String line, final ParseInfo pi) {
                // iDot
                pi.galileoNav.setIDot(RinexUtils.parseDouble(line, 4, 19));
                // Data sources (ignored)
                // RinexUtils.parseDouble(line, 23, 19)
                // GAL week (to go with Toe)
                pi.galileoNav.setWeek((int) RinexUtils.parseDouble(line, 42, 19));
                pi.galileoNav.setDate(new GNSSDate(pi.galileoNav.getWeek(),
                                                   pi.galileoNav.getTime(),
                                                   SatelliteSystem.GPS, // in Rinex files, week number is aligned to GPS week!
                                                   pi.timeScales).getDate());
            }

            /** {@inheritDoc} */
            @Override
            public void parseSixthBroadcastOrbit(final String line, final ParseInfo pi) {
                parseLine(line,
                          pi.galileoNav::setSisa,     Unit.METRE,
                          pi.galileoNav::setSvHealth, Unit.NONE,
                          pi.galileoNav::setBGDE1E5a, Unit.SECOND,
                          pi.galileoNav::setBGDE5bE1, Unit.SECOND,
                          null);
            }

            /** {@inheritDoc} */
            @Override
            public void parseSeventhBroadcastOrbit(final String line, final ParseInfo pi) {
                parseLine(line,
                          null, Unit.NONE, // TODO: transmission time
                          null, Unit.NONE,
                          null, Unit.NONE,
                          null, Unit.NONE,
                          () -> {
                              pi.file.addGalileoNavigationMessage(pi.galileoNav);
                              pi.galileoNav = new GalileoNavigationMessage();
                          });
            }

        },

        /** Glonass. */
        GLONASS() {

            /** {@inheritDoc} */
            @Override
            public void parseSvEpochSvClockLine(final String line, final ParseInfo pi) {

                parseSvEpochSvClockLine(line,
                                        pi.glonassNav::setPRN,
                                        pi.glonassNav::setEpochToc, pi.timeScales.getUTC(),
                                        pi.glonassNav::setTauN, MINUS_SECONDS,
                                        pi.glonassNav::setGammaN, Unit.NONE,
                                        d -> pi.glonassNav.setTime(fmod(d, Constants.JULIAN_DAY)), Unit.NONE);

                // Set the ephemeris epoch (same as time of clock epoch)
                pi.glonassNav.setDate(pi.glonassNav.getEpochToc());

            }

            /** {@inheritDoc} */
            @Override
            public void parseFirstBroadcastOrbit(final String line, final ParseInfo pi) {
                parseLine(line,
                          pi.glonassNav::setX,       KM,
                          pi.glonassNav::setXDot,    KM_PER_S,
                          pi.glonassNav::setXDotDot, KM_PER_S2,
                          pi.glonassNav::setHealth,  Unit.NONE,
                          null);
            }

            /** {@inheritDoc} */
            @Override
            public void parseSecondBroadcastOrbit(final String line, final ParseInfo pi) {
                parseLine(line,
                          pi.glonassNav::setY,               KM,
                          pi.glonassNav::setYDot,            KM_PER_S,
                          pi.glonassNav::setYDotDot,         KM_PER_S2,
                          pi.glonassNav::setFrequencyNumber, Unit.NONE,
                          null);
            }

            /** {@inheritDoc} */
            @Override
            public void parseThirdBroadcastOrbit(final String line, final ParseInfo pi) {
                parseLine(line,
                          pi.glonassNav::setZ,       KM,
                          pi.glonassNav::setZDot,    KM_PER_S,
                          pi.glonassNav::setZDotDot, KM_PER_S2,
                          null,                      Unit.NONE,
                          () -> {
                              if (pi.file.getHeader().getFormatVersion() < 3.045) {
                                  pi.file.addGlonassNavigationMessage(pi.glonassNav);
                                  pi.glonassNav = new GLONASSNavigationMessage();
                              }
                          });
            }

            /** {@inheritDoc} */
            @Override
            public void parseFourthBroadcastOrbit(final String line, final ParseInfo pi) {
                if (pi.file.getHeader().getFormatVersion() > 3.045) {
                    // this line has been introduced in 3.05
                    parseLine(line,
                              pi.glonassNav::setStatusFlags,          Unit.NONE,
                              pi.glonassNav::setGroupDelayDifference, Unit.NONE,
                              pi.glonassNav::setURA,                  Unit.NONE,
                              pi.glonassNav::setHealthFlags,          Unit.NONE,
                              () -> {
                                  pi.file.addGlonassNavigationMessage(pi.glonassNav);
                                  pi.glonassNav = new GLONASSNavigationMessage();
                              });
                }
            }

            /** {@inheritDoc} */
            @Override
            public void parseFifthBroadcastOrbit(final String line, final ParseInfo pi) {
                // Nothing to do for GLONASS
            }

            /** {@inheritDoc} */
            @Override
            public void parseSixthBroadcastOrbit(final String line, final ParseInfo pi) {
                // Nothing to do for GLONASS
            }

            /** {@inheritDoc} */
            @Override
            public void parseSeventhBroadcastOrbit(final String line, final ParseInfo pi) {
                // Nothing to do for GLONASS
            }

        },

        /** QZSS. */
        QZSS() {

            /** {@inheritDoc} */
            @Override
            public void parseSvEpochSvClockLine(final String line, final ParseInfo pi) {
                parseSvEpochSvClockLine(line,
                                        pi.qzssLNav::setPRN,
                                        pi.qzssLNav::setEpochToc, pi.timeScales.getGPS(),
                                        pi.qzssLNav::setAf0, Unit.SECOND,
                                        pi.qzssLNav::setAf1, S_PER_S,
                                        pi.qzssLNav::setAf2, S_PER_S2);
            }

            /** {@inheritDoc} */
            @Override
            public void parseFirstBroadcastOrbit(final String line, final ParseInfo pi) {
                parseLine(line,
                          pi.qzssLNav::setIODE,   Unit.SECOND,
                          pi.qzssLNav::setCrs,    Unit.METRE,
                          pi.qzssLNav::setDeltaN, RAD_PER_S,
                          pi.qzssLNav::setM0,     Unit.RADIAN,
                          null);
            }

            /** {@inheritDoc} */
            @Override
            public void parseSecondBroadcastOrbit(final String line, final ParseInfo pi) {
                parseLine(line,
                          pi.qzssLNav::setCuc,   Unit.RADIAN,
                          pi.qzssLNav::setE,     Unit.NONE,
                          pi.qzssLNav::setCus,   Unit.RADIAN,
                          pi.qzssLNav::setSqrtA, SQRT_M,
                          null);
            }

            /** {@inheritDoc} */
            @Override
            public void parseThirdBroadcastOrbit(final String line, final ParseInfo pi) {
                parseLine(line,
                          pi.qzssLNav::setTime,   Unit.SECOND,
                          pi.qzssLNav::setCic,    Unit.RADIAN,
                          pi.qzssLNav::setOmega0, Unit.RADIAN,
                          pi.qzssLNav::setCis,    Unit.RADIAN,
                          null);
            }

            /** {@inheritDoc} */
            @Override
            public void parseFourthBroadcastOrbit(final String line, final ParseInfo pi) {
                parseLine(line,
                          pi.qzssLNav::setI0,       Unit.RADIAN,
                          pi.qzssLNav::setCrc,      Unit.METRE,
                          pi.qzssLNav::setPa,       Unit.RADIAN,
                          pi.qzssLNav::setOmegaDot, RAD_PER_S,
                          null);
            }

            /** {@inheritDoc} */
            @Override
            public void parseFifthBroadcastOrbit(final String line, final ParseInfo pi) {
                // iDot
                pi.qzssLNav.setIDot(RinexUtils.parseDouble(line, 4, 19));
                // Codes on L2 channel (ignored)
                // RinexUtils.parseDouble(line, 23, 19)
                // GPS week (to go with Toe)
                pi.qzssLNav.setWeek((int) RinexUtils.parseDouble(line, 42, 19));
                pi.qzssLNav.setDate(new GNSSDate(pi.qzssLNav.getWeek(),
                                                 pi.qzssLNav.getTime(),
                                                 SatelliteSystem.GPS, // in Rinex files, week number is aligned to GPS week!
                                                 pi.timeScales).getDate());
            }

            /** {@inheritDoc} */
            @Override
            public void parseSixthBroadcastOrbit(final String line, final ParseInfo pi) {
                parseLine(line,
                          pi.qzssLNav::setSvAccuracy, Unit.METRE,
                          pi.qzssLNav::setSvHealth,   Unit.NONE,
                          pi.qzssLNav::setTGD,        Unit.SECOND,
                          pi.qzssLNav::setIODC,       Unit.SECOND,
                          null);
            }

            /** {@inheritDoc} */
            @Override
            public void parseSeventhBroadcastOrbit(final String line, final ParseInfo pi) {
                parseLine(line,
                          null, Unit.NONE, // TODO: transmission time
                          null, Unit.NONE, // TODO: fit interval
                          null, Unit.NONE,
                          null, Unit.NONE,
                          () -> {
                              pi.file.addQZSSNavigationMessage(pi.qzssLNav);
                              pi.qzssLNav = new QZSSLegacyNavigationMessage();
                          });
            }

        },

        /** Beidou. */
        BEIDOU() {

            /** {@inheritDoc} */
            @Override
            public void parseSvEpochSvClockLine(final String line, final ParseInfo pi) {
                parseSvEpochSvClockLine(line,
                                        pi.beidouNav::setPRN,
                                        pi.beidouNav::setEpochToc, pi.timeScales.getBDT(),
                                        pi.beidouNav::setAf0, Unit.SECOND,
                                        pi.beidouNav::setAf1, S_PER_S,
                                        pi.beidouNav::setAf2, S_PER_S2);
            }

            /** {@inheritDoc} */
            @Override
            public void parseFirstBroadcastOrbit(final String line, final ParseInfo pi) {
                parseLine(line,
                          pi.beidouNav::setAODE,   Unit.SECOND,
                          pi.beidouNav::setCrs,    Unit.METRE,
                          pi.beidouNav::setDeltaN, RAD_PER_S,
                          pi.beidouNav::setM0,     Unit.RADIAN,
                          null);
            }

            /** {@inheritDoc} */
            @Override
            public void parseSecondBroadcastOrbit(final String line, final ParseInfo pi) {
                parseLine(line,
                          pi.beidouNav::setCuc,   Unit.RADIAN,
                          pi.beidouNav::setE,     Unit.NONE,
                          pi.beidouNav::setCus,   Unit.RADIAN,
                          pi.beidouNav::setSqrtA, SQRT_M,
                          null);
            }

            /** {@inheritDoc} */
            @Override
            public void parseThirdBroadcastOrbit(final String line, final ParseInfo pi) {
                parseLine(line,
                          pi.beidouNav::setTime,   Unit.SECOND,
                          pi.beidouNav::setCic,    Unit.RADIAN,
                          pi.beidouNav::setOmega0, Unit.RADIAN,
                          pi.beidouNav::setCis,    Unit.RADIAN,
                          null);
            }

            /** {@inheritDoc} */
            @Override
            public void parseFourthBroadcastOrbit(final String line, final ParseInfo pi) {
                parseLine(line,
                          pi.beidouNav::setI0,       Unit.RADIAN,
                          pi.beidouNav::setCrc,      Unit.METRE,
                          pi.beidouNav::setPa,       Unit.RADIAN,
                          pi.beidouNav::setOmegaDot, RAD_PER_S,
                          null);
            }

            /** {@inheritDoc} */
            @Override
            public void parseFifthBroadcastOrbit(final String line, final ParseInfo pi) {
                // iDot
                pi.beidouNav.setIDot(RinexUtils.parseDouble(line, 4, 19));
                // BDT week (to go with Toe)
                pi.beidouNav.setWeek((int) RinexUtils.parseDouble(line, 42, 19));
                pi.beidouNav.setDate(new GNSSDate(pi.beidouNav.getWeek(),
                                                  pi.beidouNav.getTime(),
                                                  SatelliteSystem.BEIDOU,
                                                  pi.timeScales).getDate());
            }

            /** {@inheritDoc} */
            @Override
            public void parseSixthBroadcastOrbit(final String line, final ParseInfo pi) {
                parseLine(line,
                          pi.beidouNav::setSvAccuracy, Unit.METRE,
                          null,                        Unit.NONE,  // TODO: SatH1
                          pi.beidouNav::setTGD1,       Unit.SECOND,
                          pi.beidouNav::setTGD2,       Unit.SECOND,
                          null);
            }

            /** {@inheritDoc} */
            @Override
            public void parseSeventhBroadcastOrbit(final String line, final ParseInfo pi) {
                parseLine(line,
                          null,                  Unit.NONE, // TODO: transmission time
                          pi.beidouNav::setAODC, Unit.SECOND,
                          null,                  Unit.NONE,
                          null,                  Unit.NONE,
                          () -> {
                              pi.file.addBeidouNavigationMessage(pi.beidouNav);
                              pi.beidouNav = new BeidouNavigationMessage();
                          });
            }

        },

        /** SBAS. */
        SBAS() {

            /** {@inheritDoc} */
            @Override
            public void parseSvEpochSvClockLine(final String line, final ParseInfo pi) {

                // Time scale (UTC for Rinex 3.01 and GPS for other RINEX versions)
                final int version100 = (int) FastMath.rint(pi.file.getHeader().getFormatVersion() * 100);
                final TimeScale    timeScale = (version100 == 301) ? pi.timeScales.getUTC() : pi.timeScales.getGPS();

                parseSvEpochSvClockLine(line,
                                        pi.sbasNav::setPRN,
                                        pi.sbasNav::setEpochToc, timeScale,
                                        pi.sbasNav::setAGf0, Unit.SECOND,
                                        pi.sbasNav::setAGf1, S_PER_S,
                                        pi.sbasNav::setTime, Unit.SECOND);

                // Set the ephemeris epoch (same as time of clock epoch)
                pi.sbasNav.setDate(pi.sbasNav.getEpochToc());

            }

            /** {@inheritDoc} */
            @Override
            public void parseFirstBroadcastOrbit(final String line, final ParseInfo pi) {
                parseLine(line,
                          pi.sbasNav::setX,       KM,
                          pi.sbasNav::setXDot,    KM_PER_S,
                          pi.sbasNav::setXDotDot, KM_PER_S2,
                          pi.sbasNav::setHealth,  Unit.NONE,
                          null);
            }

            /** {@inheritDoc} */
            @Override
            public void parseSecondBroadcastOrbit(final String line, final ParseInfo pi) {
                parseLine(line,
                          pi.sbasNav::setY,       KM,
                          pi.sbasNav::setYDot,    KM_PER_S,
                          pi.sbasNav::setYDotDot, KM_PER_S2,
                          pi.sbasNav::setURA,     Unit.NONE,
                          null);
            }

            /** {@inheritDoc} */
            @Override
            public void parseThirdBroadcastOrbit(final String line, final ParseInfo pi) {
                parseLine(line,
                          pi.sbasNav::setZ,       KM,
                          pi.sbasNav::setZDot,    KM_PER_S,
                          pi.sbasNav::setZDotDot, KM_PER_S2,
                          pi.sbasNav::setIODN,    Unit.NONE,
                          () -> {
                              pi.file.addSBASNavigationMessage(pi.sbasNav);
                              pi.sbasNav = new SBASNavigationMessage();
                          });
            }

            /** {@inheritDoc} */
            @Override
            public void parseFourthBroadcastOrbit(final String line, final ParseInfo pi) {
                // Nothing to do for SBAS
            }

            /** {@inheritDoc} */
            @Override
            public void parseFifthBroadcastOrbit(final String line, final ParseInfo pi) {
                // Nothing to do for SBAS
            }

            /** {@inheritDoc} */
            @Override
            public void parseSixthBroadcastOrbit(final String line, final ParseInfo pi) {
                // Nothing to do for SBAS
            }

            /** {@inheritDoc} */
            @Override
            public void parseSeventhBroadcastOrbit(final String line, final ParseInfo pi) {
                // Nothing to do for SBAS
            }

        },

        /** IRNSS. */
        IRNSS() {

            /** {@inheritDoc} */
            @Override
            public void parseSvEpochSvClockLine(final String line, final ParseInfo pi) {
                parseSvEpochSvClockLine(line,
                                        pi.irnssNav::setPRN,
                                        pi.irnssNav::setEpochToc, pi.timeScales.getIRNSS(),
                                        pi.irnssNav::setAf0, Unit.SECOND,
                                        pi.irnssNav::setAf1, S_PER_S,
                                        pi.irnssNav::setAf2, S_PER_S2);
            }

            /** {@inheritDoc} */
            @Override
            public void parseFirstBroadcastOrbit(final String line, final ParseInfo pi) {
                parseLine(line,
                          pi.irnssNav::setIODEC,   Unit.SECOND,
                          pi.irnssNav::setCrs,     Unit.METRE,
                          pi.irnssNav::setDeltaN,  RAD_PER_S,
                          pi.irnssNav::setM0,      Unit.RADIAN,
                          null);
            }

            /** {@inheritDoc} */
            @Override
            public void parseSecondBroadcastOrbit(final String line, final ParseInfo pi) {
                parseLine(line,
                          pi.irnssNav::setCuc,   Unit.RADIAN,
                          pi.irnssNav::setE,     Unit.NONE,
                          pi.irnssNav::setCus,   Unit.RADIAN,
                          pi.irnssNav::setSqrtA, SQRT_M,
                          null);
            }

            /** {@inheritDoc} */
            @Override
            public void parseThirdBroadcastOrbit(final String line, final ParseInfo pi) {
                parseLine(line,
                          pi.irnssNav::setTime,   Unit.SECOND,
                          pi.irnssNav::setCic,    Unit.RADIAN,
                          pi.irnssNav::setOmega0, Unit.RADIAN,
                          pi.irnssNav::setCis,    Unit.RADIAN,
                          null);
            }

            /** {@inheritDoc} */
            @Override
            public void parseFourthBroadcastOrbit(final String line, final ParseInfo pi) {
                parseLine(line,
                          pi.irnssNav::setI0,       Unit.RADIAN,
                          pi.irnssNav::setCrc,      Unit.METRE,
                          pi.irnssNav::setPa,       Unit.RADIAN,
                          pi.irnssNav::setOmegaDot, RAD_PER_S,
                          null);
            }

            /** {@inheritDoc} */
            @Override
            public void parseFifthBroadcastOrbit(final String line, final ParseInfo pi) {
                // iDot
                pi.irnssNav.setIDot(RinexUtils.parseDouble(line, 4, 19));
                // IRNSS week (to go with Toe)
                pi.irnssNav.setWeek((int) RinexUtils.parseDouble(line, 42, 19));
                pi.irnssNav.setDate(new GNSSDate(pi.irnssNav.getWeek(),
                                                 pi.irnssNav.getTime(),
                                                 SatelliteSystem.GPS, // in Rinex files, week number is aligned to GPS week!
                                                 pi.timeScales).getDate());
            }

            /** {@inheritDoc} */
            @Override
            public void parseSixthBroadcastOrbit(final String line, final ParseInfo pi) {
                parseLine(line,
                          pi.irnssNav::setURA,      Unit.METRE,
                          pi.irnssNav::setSvHealth, Unit.NONE,
                          pi.irnssNav::setTGD,      Unit.SECOND,
                          null,                     Unit.NONE,
                          null);
            }

            /** {@inheritDoc} */
            @Override
            public void parseSeventhBroadcastOrbit(final String line, final ParseInfo pi) {
                parseLine(line,
                          null, Unit.NONE,
                          null, Unit.NONE,
                          null, Unit.NONE,
                          null, Unit.NONE,
                          () -> {
                              pi.file.addIRNSSNavigationMessage(pi.irnssNav);
                              pi.irnssNav = new IRNSSNavigationMessage();
                          });
            }

        };

        /** Get the parse for navigation message.
         * @param system satellite system
         * @param type message type (null for Rinex 3.x)
         * @param parseInfo container for transient data
         * @param line line being parsed
         * @return the satellite system
         */
        public static SatelliteSystemLineParser getParser(final SatelliteSystem system, final String type,
                                                          final ParseInfo parseInfo, final String line) {
            switch (system) {
                case GPS :
                    if (type == null || type.equals(LegacyNavigationMessage.LNAV)) {
                        return LNAV;
                    } else if (type.equals(CivilianNavigationMessage.CNAV)) {
                        return CNAV;
                    } else if (type.equals(CNV2)) {
                        // TODO
                        throw new OrekitInternalError(null);
                    }
                    break;
                case GALILEO :
                    if (type == null || type.equals("INAV") || type.equals("FNAV")) {
                        return GALILEO;
                    }
                    break;
                case GLONASS :
                    if (type == null || type.equals("FDMA")) {
                        return GLONASS;
                    }
                    break;
                case QZSS :
                    if (type == null || type.equals(LegacyNavigationMessage.LNAV)) {
                        return QZSS;
                    } else if (type.equals(CivilianNavigationMessage.CNAV)) {
                        // TODO
                        throw new OrekitInternalError(null);
                    } else if (type.equals(CNV2)) {
                        // TODO
                        throw new OrekitInternalError(null);
                    }
                    break;
                case BEIDOU :
                    if (type == null || type.equals("D1") || type.equals("D2")) {
                        return BEIDOU;
                    } else if (type.equals(CNV1)) {
                        // TODO
                        throw new OrekitInternalError(null);
                    } else if (type.equals(CNV2)) {
                        // TODO
                        throw new OrekitInternalError(null);
                    } else if (type.equals(CNV3)) {
                        // TODO
                        throw new OrekitInternalError(null);
                    }
                    break;
                case IRNSS :
                    if (type == null || type.equals("LNAV")) {
                        return IRNSS;
                    }
                    break;
                case SBAS :
                    if (type == null || type.equals("SBAS")) {
                        return SBAS;
                    }
                    break;
                default:
                    // do nothing, handle error after the switch
            }
            throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                      parseInfo.lineNumber, parseInfo.name, line);
        }

        /**
         * Parse the SV/Epoch/Sv clock of the navigation message.
         * @param line line to read
         * @param prnSetter setter for the PRN
         * @param tocSetter setter for the Tim:e Of Clock
         * @param timeScale time scale to use for parsing the Time Of Clock
         * @param setter1 setter for the first field
         * @param unit1 unit for the first field
         * @param setter2 setter for the second field
         * @param unit2 unit for the second field
         * @param setter3 setter for the third field
         * @param unit3 unit for the third field
         */
        protected void parseSvEpochSvClockLine(final String line,
                                               final IntConsumer prnSetter,
                                               final Consumer<AbsoluteDate> tocSetter, final TimeScale timeScale,
                                               final DoubleConsumer setter1, final Unit unit1,
                                               final DoubleConsumer setter2, final Unit unit2,
                                               final DoubleConsumer setter3, final Unit unit3) {
            // PRN
            prnSetter.accept(RinexUtils.parseInt(line, 1, 2));

            // Toc
            final int year  = RinexUtils.parseInt(line, 4, 4);
            final int month = RinexUtils.parseInt(line, 9, 2);
            final int day   = RinexUtils.parseInt(line, 12, 2);
            final int hours = RinexUtils.parseInt(line, 15, 2);
            final int min   = RinexUtils.parseInt(line, 18, 2);
            final int sec   = RinexUtils.parseInt(line, 21, 2);
            tocSetter.accept(new AbsoluteDate(year, month, day, hours, min, sec, timeScale));

            // clock
            setter1.accept(unit1.toSI(RinexUtils.parseDouble(line, 23, 19)));
            setter2.accept(unit2.toSI(RinexUtils.parseDouble(line, 42, 19)));
            setter3.accept(unit3.toSI(RinexUtils.parseDouble(line, 61, 19)));

        }

        /** Parse o broadcast orbit line.
         * <p>
         * All parameters (except line) may be null if a field should be ignored
         * </p>
         * @param line line to parse
         * @param setter1 setter for the first field
         * @param unit1 unit for the first field
         * @param setter2 setter for the second field
         * @param unit2 unit for the second field
         * @param setter3 setter for the third field
         * @param unit3 unit for the third field
         * @param setter4 setter for the fourth field
         * @param unit4 unit for the fourth field
         * @param finalizer finalizer, non-null only for last broadcast line
         */
        protected void parseLine(final String line,
                                 final DoubleConsumer setter1, final Unit unit1,
                                 final DoubleConsumer setter2, final Unit unit2,
                                 final DoubleConsumer setter3, final Unit unit3,
                                 final DoubleConsumer setter4, final Unit unit4,
                                 final Finalizer finalizer) {
            if (setter1 != null) {
                setter1.accept(unit1.toSI(RinexUtils.parseDouble(line, 4, 19)));
            }
            if (setter2 != null) {
                setter2.accept(unit2.toSI(RinexUtils.parseDouble(line, 23, 19)));
            }
            if (setter3 != null) {
                setter3.accept(unit3.toSI(RinexUtils.parseDouble(line, 42, 19)));
            }
            if (setter4 != null) {
                setter4.accept(unit4.toSI(RinexUtils.parseDouble(line, 61, 19)));
            }
            if (finalizer != null) {
                finalizer.finalize();
            }
        }

        /** Finalizer for last broadcast orbit line. */
        private interface Finalizer {
            /** Finalize broadcast orbit.
             */
            void finalize();
        }

        /**
         * Parse the SV/Epoch/Sv clock of the navigation message.
         * @param line line to read
         * @param pi holder for transient data
         */
        public abstract void parseSvEpochSvClockLine(String line, ParseInfo pi);

        /**
         * Parse the "BROADCASTORBIT - 1" line.
         * @param line line to read
         * @param pi holder for transient data
         */
        public abstract void parseFirstBroadcastOrbit(String line, ParseInfo pi);

        /**
         * Parse the "BROADCASTORBIT - 2" line.
         * @param line line to read
         * @param pi holder for transient data
         */
        public abstract void parseSecondBroadcastOrbit(String line, ParseInfo pi);

        /**
         * Parse the "BROADCASTORBIT - 3" line.
         * @param line line to read
         * @param pi holder for transient data
         */
        public abstract void parseThirdBroadcastOrbit(String line, ParseInfo pi);

        /**
         * Parse the "BROADCASTORBIT - 4" line.
         * @param line line to read
         * @param pi holder for transient data
         */
        public void parseFourthBroadcastOrbit(final String line, final ParseInfo pi) {
            // do nothing by default
        }

        /**
         * Parse the "BROADCASTORBIT - 5" line.
         * @param line line to read
         * @param pi holder for transient data
         */
        public void parseFifthBroadcastOrbit(final String line, final ParseInfo pi) {
            // do nothing by default
        }

        /**
         * Parse the "BROADCASTORBIT - 6" line.
         * @param line line to read
         * @param pi holder for transient data
         */
        public void parseSixthBroadcastOrbit(final String line, final ParseInfo pi) {
            // do nothing by default
        }

        /**
         * Parse the "BROADCASTORBIT - 7" line.
         * @param line line to read
         * @param pi holder for transient data
         */
        public void parseSeventhBroadcastOrbit(final String line, final ParseInfo pi) {
            // do nothing by default
        }

        /**
         * Parse the "BROADCASTORBIT - 8" line.
         * @param line line to read
         * @param pi holder for transient data
         */
        public void parseEighthBroadcastOrbit(final String line, final ParseInfo pi) {
            // do nothing by default
        }

        /**
         * Parse the "BROADCASTORBIT - 9" line.
         * @param line line to read
         * @param pi holder for transient data
         */
        public void parseNinththBroadcastOrbit(final String line, final ParseInfo pi) {
            // do nothing by default
        }

        /**
         * Calculates the floating-point remainder of a / b.
         * <p>
         * fmod = a - x * b
         * where x = (int) a / b
         * </p>
         * @param a numerator
         * @param b denominator
         * @return the floating-point remainder of a / b
         */
        private static double fmod(final double a, final double b) {
            final double x = (int) (a / b);
            return a - x * b;
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
