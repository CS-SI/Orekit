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
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.hipparchus.util.FastMath;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.gnss.RinexUtils;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.gnss.navigation.RinexNavigation.TimeSystemCorrection;
import org.orekit.propagation.analytical.gnss.data.BeidouNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.GLONASSNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.GPSNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.GalileoNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.IRNSSNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.QZSSNavigationMessage;
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

        int lineNumber = 0;
        Stream<LineParser> candidateParsers = Stream.of(LineParser.HEADER_VERSION);
        try (Reader reader = source.getOpener().openReaderOnce();
             BufferedReader br = new BufferedReader(reader)) {
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                ++lineNumber;
                final String l = line;
                final Optional<LineParser> selected = candidateParsers.filter(p -> p.canHandle.test(l)).findFirst();
                if (selected.isPresent()) {
                    try {
                        selected.get().parsingMethod.parse(line, pi);
                    } catch (StringIndexOutOfBoundsException | NumberFormatException | InputMismatchException e) {
                        throw new OrekitException(e,
                                                  OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                  lineNumber, source.getName(), line);
                    }
                    candidateParsers = selected.get().allowedNextProvider.apply(pi);
                } else {
                    throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                              lineNumber, source.getName(), line);
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

        /** Current line number of the navigation message. */
        private int lineNumber;

        /** Container for GPS navigation message. */
        private GPSNavigationMessage gpsNav;

        /** Container for Galileo navigation message. */
        private GalileoNavigationMessage galileoNav;

        /** Container for Beidou navigation message. */
        private BeidouNavigationMessage beidouNav;

        /** Container for QZSS navigation message. */
        private QZSSNavigationMessage qzssNav;

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
            this.systemLineParser             = SatelliteSystemLineParser.GPS;
            this.lineNumber                   = 0;
            this.gpsNav                       = new GPSNavigationMessage();
            this.galileoNav                   = new GalileoNavigationMessage();
            this.beidouNav                    = new BeidouNavigationMessage();
            this.qzssNav                      = new QZSSNavigationMessage();
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
                                                                                    3.01, 3.02, 3.03, 3.04, 3.05),
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

        /** Parser for the end of header. */
        HEADER_END(line -> RinexUtils.matchesLabel(line, "END OF HEADER"),
                   (line, pi) -> {
                       // nothing to do
                   },
                   LineParser::navigationNext),

        /** Parser for navigation message first data line. */
        NAVIGATION_MESSAGE_FIRST(line -> "GRECIJS".indexOf(line.charAt(0)) >= 0,
                                 (line, pi) -> {

                                     // Set the line number to 0
                                     pi.lineNumber = 0;

                                     // Current satellite system
                                     final String key = RinexUtils.parseString(line, 0, 1);

                                     // Initialize parser
                                     pi.systemLineParser = SatelliteSystemLineParser.getSatelliteSystemLineParser(key);

                                     // Read first line
                                     pi.systemLineParser.parseFirstLine(line, pi);

                                 },
                                 LineParser::navigationNext),

        /** Parser for broadcast orbit. */
        NAVIGATION_BROADCAST_ORBIT(line -> line.startsWith("    "),
                                   (line, pi) -> {

                                       // Increment the line number
                                       pi.lineNumber++;

                                       // Read the corresponding line
                                       if (pi.lineNumber == 1) {
                                           // BROADCAST ORBIT – 1
                                           pi.systemLineParser.parseFirstBroadcastOrbit(line, pi);
                                       } else if (pi.lineNumber == 2) {
                                           // BROADCAST ORBIT – 2
                                           pi.systemLineParser.parseSecondBroadcastOrbit(line, pi);
                                       } else if (pi.lineNumber == 3) {
                                           // BROADCAST ORBIT – 3
                                           pi.systemLineParser.parseThirdBroadcastOrbit(line, pi);
                                       } else if (pi.lineNumber == 4) {
                                           // BROADCAST ORBIT – 4
                                           pi.systemLineParser.parseFourthBroadcastOrbit(line, pi);
                                       } else if (pi.lineNumber == 5) {
                                           // BROADCAST ORBIT – 5
                                           pi.systemLineParser.parseFifthBroadcastOrbit(line, pi);
                                       } else if (pi.lineNumber == 6) {
                                           // BROADCAST ORBIT – 6
                                           pi.systemLineParser.parseSixthBroadcastOrbit(line, pi);
                                       } else {
                                           // BROADCAST ORBIT – 7
                                           pi.systemLineParser.parseSeventhBroadcastOrbit(line, pi);
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
            return Stream.of(HEADER_COMMENT, HEADER_PROGRAM, HEADER_IONOSPHERIC, HEADER_LEAP_SECONDS,
                             HEADER_TIME, HEADER_END);
        }

        /** Get the allowed parsers for next lines while parsing navigation date.
         * @param parseInfo holder for transient data
         * @return allowed parsers for next line
         */
        private static Stream<LineParser> navigationNext(final ParseInfo parseInfo) {
            return Stream.of(NAVIGATION_MESSAGE_FIRST, NAVIGATION_BROADCAST_ORBIT);
        }

    }

    /** Parsers for satellite system specific lines. */
    private enum SatelliteSystemLineParser {

        /** GPS. */
        GPS("G") {

            /** {@inheritDoc} */
            @Override
            public void parseFirstLine(final String line, final ParseInfo pi) {
                // PRN
                pi.gpsNav.setPRN(RinexUtils.parseInt(line, 1, 2));

                // Toc
                final int gpsTocYear  = RinexUtils.parseInt(line, 4, 4);
                final int gpsTocMonth = RinexUtils.parseInt(line, 9, 2);
                final int gpsTocDay   = RinexUtils.parseInt(line, 12, 2);
                final int gpsTocHours = RinexUtils.parseInt(line, 15, 2);
                final int gpsTocMin   = RinexUtils.parseInt(line, 18, 2);
                final int gpsTocSec   = RinexUtils.parseInt(line, 21, 2);
                pi.gpsNav.setEpochToc(new AbsoluteDate(gpsTocYear, gpsTocMonth, gpsTocDay, gpsTocHours,
                                                       gpsTocMin, gpsTocSec, pi.timeScales.getGPS()));

                // Af0, Af1, and Af2
                pi.gpsNav.setAf0(RinexUtils.parseDouble(line, 23, 19));
                pi.gpsNav.setAf1(RinexUtils.parseDouble(line, 42, 19));
                pi.gpsNav.setAf2(RinexUtils.parseDouble(line, 61, 19));
            }

            /** {@inheritDoc} */
            @Override
            public void parseFirstBroadcastOrbit(final String line, final ParseInfo pi) {
                // IODE
                pi.gpsNav.setIODE(RinexUtils.parseDouble(line, 4, 19));
                // Crs
                pi.gpsNav.setCrs(RinexUtils.parseDouble(line, 23, 19));
                // Delta n
                pi.gpsNav.setDeltaN(RinexUtils.parseDouble(line, 42, 19));
                // M0
                pi.gpsNav.setM0(RinexUtils.parseDouble(line, 61, 19));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSecondBroadcastOrbit(final String line, final ParseInfo pi) {
                // Cuc
                pi.gpsNav.setCuc(RinexUtils.parseDouble(line, 4, 19));
                // e
                pi.gpsNav.setE(RinexUtils.parseDouble(line, 23, 19));
                // Cus
                pi.gpsNav.setCus(RinexUtils.parseDouble(line, 42, 19));
                // sqrt(A)
                pi.gpsNav.setSqrtA(RinexUtils.parseDouble(line, 61, 19));
            }

            /** {@inheritDoc} */
            @Override
            public void parseThirdBroadcastOrbit(final String line, final ParseInfo pi) {
                // Toe
                pi.gpsNav.setTime(RinexUtils.parseDouble(line, 4, 19));
                // Cic
                pi.gpsNav.setCic(RinexUtils.parseDouble(line, 23, 19));
                // Omega0
                pi.gpsNav.setOmega0(RinexUtils.parseDouble(line, 42, 19));
                // Cis
                pi.gpsNav.setCis(RinexUtils.parseDouble(line, 61, 19));
            }

            /** {@inheritDoc} */
            @Override
            public void parseFourthBroadcastOrbit(final String line, final ParseInfo pi) {
                // i0
                pi.gpsNav.setI0(RinexUtils.parseDouble(line, 4, 19));
                // Crc
                pi.gpsNav.setCrc(RinexUtils.parseDouble(line, 23, 19));
                // omega
                pi.gpsNav.setPa(RinexUtils.parseDouble(line, 42, 19));
                // OMEGA DOT
                pi.gpsNav.setOmegaDot(RinexUtils.parseDouble(line, 61, 19));
            }

            /** {@inheritDoc} */
            @Override
            public void parseFifthBroadcastOrbit(final String line, final ParseInfo pi) {
                // iDot
                pi.gpsNav.setIDot(RinexUtils.parseDouble(line, 4, 19));
                // Codes on L2 channel (ignored)
                // RinexUtils.parseDouble(line, 23, 19)
                // GPS week (to go with Toe)
                pi.gpsNav.setWeek((int) RinexUtils.parseDouble(line, 42, 19));
                pi.gpsNav.setDate(new GNSSDate(pi.gpsNav.getWeek(),
                                               pi.gpsNav.getTime(),
                                               SatelliteSystem.GPS,
                                               pi.timeScales).getDate());
            }

            /** {@inheritDoc} */
            @Override
            public void parseSixthBroadcastOrbit(final String line, final ParseInfo pi) {
                // SV accuracy
                pi.gpsNav.setSvAccuracy(RinexUtils.parseDouble(line, 4, 19));
                // Health
                pi.gpsNav.setSvHealth(RinexUtils.parseDouble(line, 23, 19));
                // TGD
                pi.gpsNav.setTGD(RinexUtils.parseDouble(line, 42, 19));
                // IODC
                pi.gpsNav.setIODC(RinexUtils.parseDouble(line, 61, 19));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSeventhBroadcastOrbit(final String line, final ParseInfo pi) {
                // Add the navigation message to the file
                pi.file.addGPSNavigationMessage(pi.gpsNav);
                // Reinitialized the container for navigation data
                pi.gpsNav = new GPSNavigationMessage();
            }

        },

        /** Galileo. */
        GALILEO("E") {

            /** {@inheritDoc} */
            @Override
            public void parseFirstLine(final String line, final ParseInfo pi) {
                // PRN
                pi.galileoNav.setPRN(RinexUtils.parseInt(line, 1, 2));

                // Toc
                final int galileoTocYear  = RinexUtils.parseInt(line, 4, 4);
                final int galileoTocMonth = RinexUtils.parseInt(line, 9, 2);
                final int galileoTocDay   = RinexUtils.parseInt(line, 12, 2);
                final int galileoTocHours = RinexUtils.parseInt(line, 15, 2);
                final int galileoTocMin   = RinexUtils.parseInt(line, 18, 2);
                final int galileoTocSec   = RinexUtils.parseInt(line, 21, 2);
                pi.galileoNav.setEpochToc(new AbsoluteDate(galileoTocYear, galileoTocMonth, galileoTocDay, galileoTocHours,
                                                       galileoTocMin, galileoTocSec, pi.timeScales.getGST()));

                // Af0, Af1, and Af2
                pi.galileoNav.setAf0(RinexUtils.parseDouble(line, 23, 19));
                pi.galileoNav.setAf1(RinexUtils.parseDouble(line, 42, 19));
                pi.galileoNav.setAf2(RinexUtils.parseDouble(line, 61, 19));
            }

            /** {@inheritDoc} */
            @Override
            public void parseFirstBroadcastOrbit(final String line, final ParseInfo pi) {
                // IODNav
                pi.galileoNav.setIODNav(RinexUtils.parseDouble(line, 4, 19));
                // Crs
                pi.galileoNav.setCrs(RinexUtils.parseDouble(line, 23, 19));
                // Delta n
                pi.galileoNav.setDeltaN(RinexUtils.parseDouble(line, 42, 19));
                // M0
                pi.galileoNav.setM0(RinexUtils.parseDouble(line, 61, 19));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSecondBroadcastOrbit(final String line, final ParseInfo pi) {
                // Cuc
                pi.galileoNav.setCuc(RinexUtils.parseDouble(line, 4, 19));
                // e
                pi.galileoNav.setE(RinexUtils.parseDouble(line, 23, 19));
                // Cus
                pi.galileoNav.setCus(RinexUtils.parseDouble(line, 42, 19));
                // sqrt(A)
                pi.galileoNav.setSqrtA(RinexUtils.parseDouble(line, 61, 19));
            }

            /** {@inheritDoc} */
            @Override
            public void parseThirdBroadcastOrbit(final String line, final ParseInfo pi) {
                // Toe
                pi.galileoNav.setTime(RinexUtils.parseDouble(line, 4, 19));
                // Cic
                pi.galileoNav.setCic(RinexUtils.parseDouble(line, 23, 19));
                // Omega0
                pi.galileoNav.setOmega0(RinexUtils.parseDouble(line, 42, 19));
                // Cis
                pi.galileoNav.setCis(RinexUtils.parseDouble(line, 61, 19));
            }

            /** {@inheritDoc} */
            @Override
            public void parseFourthBroadcastOrbit(final String line, final ParseInfo pi) {
                // i0
                pi.galileoNav.setI0(RinexUtils.parseDouble(line, 4, 19));
                // Crc
                pi.galileoNav.setCrc(RinexUtils.parseDouble(line, 23, 19));
                // omega
                pi.galileoNav.setPa(RinexUtils.parseDouble(line, 42, 19));
                // OMEGA DOT
                pi.galileoNav.setOmegaDot(RinexUtils.parseDouble(line, 61, 19));
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
                // SISA
                pi.galileoNav.setSisa(RinexUtils.parseDouble(line, 4, 19));
                // Health
                pi.galileoNav.setSvHealth(RinexUtils.parseDouble(line, 23, 19));
                // E5a/E1 BGD
                pi.galileoNav.setBGDE1E5a(RinexUtils.parseDouble(line, 42, 19));
                // E5b/E1 BGD
                pi.galileoNav.setBGDE5bE1(RinexUtils.parseDouble(line, 61, 19));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSeventhBroadcastOrbit(final String line, final ParseInfo pi) {
                // Add the navigation message to the file
                pi.file.addGalileoNavigationMessage(pi.galileoNav);
                // Reinitialized the container for navigation data
                pi.galileoNav = new GalileoNavigationMessage();
            }

        },

        /** Beidou. */
        BEIDOU("C") {

            /** {@inheritDoc} */
            @Override
            public void parseFirstLine(final String line, final ParseInfo pi) {
                // PRN
                pi.beidouNav.setPRN(RinexUtils.parseInt(line, 1, 2));

                // Toc
                final int beidouTocYear  = RinexUtils.parseInt(line, 4, 4);
                final int beidouTocMonth = RinexUtils.parseInt(line, 9, 2);
                final int beidouTocDay   = RinexUtils.parseInt(line, 12, 2);
                final int beidouTocHours = RinexUtils.parseInt(line, 15, 2);
                final int beidouTocMin   = RinexUtils.parseInt(line, 18, 2);
                final int beidouTocSec   = RinexUtils.parseInt(line, 21, 2);
                pi.beidouNav.setEpochToc(new AbsoluteDate(beidouTocYear, beidouTocMonth, beidouTocDay, beidouTocHours,
                                                       beidouTocMin, beidouTocSec, pi.timeScales.getBDT()));

                // Af0, Af1, and Af2
                pi.beidouNav.setAf0(RinexUtils.parseDouble(line, 23, 19));
                pi.beidouNav.setAf1(RinexUtils.parseDouble(line, 42, 19));
                pi.beidouNav.setAf2(RinexUtils.parseDouble(line, 61, 19));
            }

            /** {@inheritDoc} */
            @Override
            public void parseFirstBroadcastOrbit(final String line, final ParseInfo pi) {
                // AODE
                pi.beidouNav.setAODE(RinexUtils.parseDouble(line, 4, 19));
                // Crs
                pi.beidouNav.setCrs(RinexUtils.parseDouble(line, 23, 19));
                // Delta n
                pi.beidouNav.setDeltaN(RinexUtils.parseDouble(line, 42, 19));
                // M0
                pi.beidouNav.setM0(RinexUtils.parseDouble(line, 61, 19));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSecondBroadcastOrbit(final String line, final ParseInfo pi) {
                // Cuc
                pi.beidouNav.setCuc(RinexUtils.parseDouble(line, 4, 19));
                // e
                pi.beidouNav.setE(RinexUtils.parseDouble(line, 23, 19));
                // Cus
                pi.beidouNav.setCus(RinexUtils.parseDouble(line, 42, 19));
                // sqrt(A)
                pi.beidouNav.setSqrtA(RinexUtils.parseDouble(line, 61, 19));
            }

            /** {@inheritDoc} */
            @Override
            public void parseThirdBroadcastOrbit(final String line, final ParseInfo pi) {
                // Toe
                pi.beidouNav.setTime(RinexUtils.parseDouble(line, 4, 19));
                // Cic
                pi.beidouNav.setCic(RinexUtils.parseDouble(line, 23, 19));
                // Omega0
                pi.beidouNav.setOmega0(RinexUtils.parseDouble(line, 42, 19));
                // Cis
                pi.beidouNav.setCis(RinexUtils.parseDouble(line, 61, 19));
            }

            /** {@inheritDoc} */
            @Override
            public void parseFourthBroadcastOrbit(final String line, final ParseInfo pi) {
                // i0
                pi.beidouNav.setI0(RinexUtils.parseDouble(line, 4, 19));
                // Crc
                pi.beidouNav.setCrc(RinexUtils.parseDouble(line, 23, 19));
                // omega
                pi.beidouNav.setPa(RinexUtils.parseDouble(line, 42, 19));
                // OMEGA DOT
                pi.beidouNav.setOmegaDot(RinexUtils.parseDouble(line, 61, 19));
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
                // SV accuracy
                pi.beidouNav.setSvAccuracy(RinexUtils.parseDouble(line, 4, 19));
                // SatH1 (ignored)
                // RinexUtils.parseDouble(line, 23, 19)
                // TGD1
                pi.beidouNav.setTGD1(RinexUtils.parseDouble(line, 42, 19));
                // TGD2
                pi.beidouNav.setTGD2(RinexUtils.parseDouble(line, 61, 19));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSeventhBroadcastOrbit(final String line, final ParseInfo pi) {
                // Transmission time of message (ignored)
                // RinexUtils.parseDouble(line, 4, 19);
                // AODC
                pi.beidouNav.setAODC(RinexUtils.parseDouble(line, 23, 19));
                // Add the navigation message to the file
                pi.file.addBeidouNavigationMessage(pi.beidouNav);
                // Reinitialized the container for navigation data
                pi.beidouNav = new BeidouNavigationMessage();

            }

        },

        /** QZSS. */
        QZSS("J") {

            /** {@inheritDoc} */
            @Override
            public void parseFirstLine(final String line, final ParseInfo pi) {
                // PRN
                pi.qzssNav.setPRN(RinexUtils.parseInt(line, 1, 2));

                // Toc
                final int qzssTocYear  = RinexUtils.parseInt(line, 4, 4);
                final int qzssTocMonth = RinexUtils.parseInt(line, 9, 2);
                final int qzssTocDay   = RinexUtils.parseInt(line, 12, 2);
                final int qzssTocHours = RinexUtils.parseInt(line, 15, 2);
                final int qzssTocMin   = RinexUtils.parseInt(line, 18, 2);
                final int qzssTocSec   = RinexUtils.parseInt(line, 21, 2);
                pi.qzssNav.setEpochToc(new AbsoluteDate(qzssTocYear, qzssTocMonth, qzssTocDay, qzssTocHours,
                                                       qzssTocMin, qzssTocSec, pi.timeScales.getQZSS()));

                // Af0, Af1, and Af2
                pi.qzssNav.setAf0(RinexUtils.parseDouble(line, 23, 19));
                pi.qzssNav.setAf1(RinexUtils.parseDouble(line, 42, 19));
                pi.qzssNav.setAf2(RinexUtils.parseDouble(line, 61, 19));
            }

            /** {@inheritDoc} */
            @Override
            public void parseFirstBroadcastOrbit(final String line, final ParseInfo pi) {
                // IODE
                pi.qzssNav.setIODE(RinexUtils.parseDouble(line, 4, 19));
                // Crs
                pi.qzssNav.setCrs(RinexUtils.parseDouble(line, 23, 19));
                // Delta n
                pi.qzssNav.setDeltaN(RinexUtils.parseDouble(line, 42, 19));
                // M0
                pi.qzssNav.setM0(RinexUtils.parseDouble(line, 61, 19));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSecondBroadcastOrbit(final String line, final ParseInfo pi) {
                // Cuc
                pi.qzssNav.setCuc(RinexUtils.parseDouble(line, 4, 19));
                // e
                pi.qzssNav.setE(RinexUtils.parseDouble(line, 23, 19));
                // Cus
                pi.qzssNav.setCus(RinexUtils.parseDouble(line, 42, 19));
                // sqrt(A)
                pi.qzssNav.setSqrtA(RinexUtils.parseDouble(line, 61, 19));
            }

            /** {@inheritDoc} */
            @Override
            public void parseThirdBroadcastOrbit(final String line, final ParseInfo pi) {
                // Toe
                pi.qzssNav.setTime(RinexUtils.parseDouble(line, 4, 19));
                // Cic
                pi.qzssNav.setCic(RinexUtils.parseDouble(line, 23, 19));
                // Omega0
                pi.qzssNav.setOmega0(RinexUtils.parseDouble(line, 42, 19));
                // Cis
                pi.qzssNav.setCis(RinexUtils.parseDouble(line, 61, 19));
            }

            /** {@inheritDoc} */
            @Override
            public void parseFourthBroadcastOrbit(final String line, final ParseInfo pi) {
                // i0
                pi.qzssNav.setI0(RinexUtils.parseDouble(line, 4, 19));
                // Crc
                pi.qzssNav.setCrc(RinexUtils.parseDouble(line, 23, 19));
                // omega
                pi.qzssNav.setPa(RinexUtils.parseDouble(line, 42, 19));
                // OMEGA DOT
                pi.qzssNav.setOmegaDot(RinexUtils.parseDouble(line, 61, 19));
            }

            /** {@inheritDoc} */
            @Override
            public void parseFifthBroadcastOrbit(final String line, final ParseInfo pi) {
                // iDot
                pi.qzssNav.setIDot(RinexUtils.parseDouble(line, 4, 19));
                // Codes on L2 channel (ignored)
                // RinexUtils.parseDouble(line, 23, 19)
                // GPS week (to go with Toe)
                pi.qzssNav.setWeek((int) RinexUtils.parseDouble(line, 42, 19));
                pi.qzssNav.setDate(new GNSSDate(pi.qzssNav.getWeek(),
                                                pi.qzssNav.getTime(),
                                                SatelliteSystem.GPS, // in Rinex files, week number is aligned to GPS week!
                                                pi.timeScales).getDate());
            }

            /** {@inheritDoc} */
            @Override
            public void parseSixthBroadcastOrbit(final String line, final ParseInfo pi) {
                // SV accuracy
                pi.qzssNav.setSvAccuracy(RinexUtils.parseDouble(line, 4, 19));
                // Health
                pi.qzssNav.setSvHealth(RinexUtils.parseDouble(line, 23, 19));
                // TGD
                pi.qzssNav.setTGD(RinexUtils.parseDouble(line, 42, 19));
                // IODC
                pi.qzssNav.setIODC(RinexUtils.parseDouble(line, 61, 19));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSeventhBroadcastOrbit(final String line, final ParseInfo pi) {
                // Add the navigation message to the file
                pi.file.addQZSSNavigationMessage(pi.qzssNav);
                // Reinitialized the container for navigation data
                pi.qzssNav = new QZSSNavigationMessage();
            }

        },

        /** IRNSS. */
        IRNSS("I") {

            /** {@inheritDoc} */
            @Override
            public void parseFirstLine(final String line, final ParseInfo pi) {
                // PRN
                pi.irnssNav.setPRN(RinexUtils.parseInt(line, 1, 2));

                // Toc
                final int irnssTocYear  = RinexUtils.parseInt(line, 4, 4);
                final int irnssTocMonth = RinexUtils.parseInt(line, 9, 2);
                final int irnssTocDay   = RinexUtils.parseInt(line, 12, 2);
                final int irnssTocHours = RinexUtils.parseInt(line, 15, 2);
                final int irnssTocMin   = RinexUtils.parseInt(line, 18, 2);
                final int irnssTocSec   = RinexUtils.parseInt(line, 21, 2);
                pi.irnssNav.setEpochToc(new AbsoluteDate(irnssTocYear, irnssTocMonth, irnssTocDay, irnssTocHours,
                                                         irnssTocMin, irnssTocSec, pi.timeScales.getIRNSS()));

                // Af0, Af1, and Af2
                pi.irnssNav.setAf0(RinexUtils.parseDouble(line, 23, 19));
                pi.irnssNav.setAf1(RinexUtils.parseDouble(line, 42, 19));
                pi.irnssNav.setAf2(RinexUtils.parseDouble(line, 61, 19));
            }

            /** {@inheritDoc} */
            @Override
            public void parseFirstBroadcastOrbit(final String line, final ParseInfo pi) {
                // IODEC
                pi.irnssNav.setIODEC(RinexUtils.parseDouble(line, 4, 19));
                // Crs
                pi.irnssNav.setCrs(RinexUtils.parseDouble(line, 23, 19));
                // Delta n
                pi.irnssNav.setDeltaN(RinexUtils.parseDouble(line, 42, 19));
                // M0
                pi.irnssNav.setM0(RinexUtils.parseDouble(line, 61, 19));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSecondBroadcastOrbit(final String line, final ParseInfo pi) {
                // Cuc
                pi.irnssNav.setCuc(RinexUtils.parseDouble(line, 4, 19));
                // e
                pi.irnssNav.setE(RinexUtils.parseDouble(line, 23, 19));
                // Cus
                pi.irnssNav.setCus(RinexUtils.parseDouble(line, 42, 19));
                // sqrt(A)
                pi.irnssNav.setSqrtA(RinexUtils.parseDouble(line, 61, 19));
            }

            /** {@inheritDoc} */
            @Override
            public void parseThirdBroadcastOrbit(final String line, final ParseInfo pi) {
                // Toe
                pi.irnssNav.setTime(RinexUtils.parseDouble(line, 4, 19));
                // Cic
                pi.irnssNav.setCic(RinexUtils.parseDouble(line, 23, 19));
                // Omega0
                pi.irnssNav.setOmega0(RinexUtils.parseDouble(line, 42, 19));
                // Cis
                pi.irnssNav.setCis(RinexUtils.parseDouble(line, 61, 19));
            }

            /** {@inheritDoc} */
            @Override
            public void parseFourthBroadcastOrbit(final String line, final ParseInfo pi) {
                // i0
                pi.irnssNav.setI0(RinexUtils.parseDouble(line, 4, 19));
                // Crc
                pi.irnssNav.setCrc(RinexUtils.parseDouble(line, 23, 19));
                // omega
                pi.irnssNav.setPa(RinexUtils.parseDouble(line, 42, 19));
                // OMEGA DOT
                pi.irnssNav.setOmegaDot(RinexUtils.parseDouble(line, 61, 19));
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
                // SV accuracy
                pi.irnssNav.setURA(RinexUtils.parseDouble(line, 4, 19));
                // Health
                pi.irnssNav.setSvHealth(RinexUtils.parseDouble(line, 23, 19));
                // TGD
                pi.irnssNav.setTGD(RinexUtils.parseDouble(line, 42, 19));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSeventhBroadcastOrbit(final String line, final ParseInfo pi) {
                // Add the navigation message to the file
                pi.file.addIRNSSNavigationMessage(pi.irnssNav);
                // Reinitialized the container for navigation data
                pi.irnssNav = new IRNSSNavigationMessage();

            }

        },

        /** Glonass. */
        GLONASS("R") {

            /** {@inheritDoc} */
            @Override
            public void parseFirstLine(final String line, final ParseInfo pi) {
                // PRN
                pi.glonassNav.setPRN(RinexUtils.parseInt(line, 1, 2));

                // Toc
                final int glonassTocYear  = RinexUtils.parseInt(line, 4, 4);
                final int glonassTocMonth = RinexUtils.parseInt(line, 9, 2);
                final int glonassTocDay   = RinexUtils.parseInt(line, 12, 2);
                final int glonassTocHours = RinexUtils.parseInt(line, 15, 2);
                final int glonassTocMin   = RinexUtils.parseInt(line, 18, 2);
                final int glonassTocSec   = RinexUtils.parseInt(line, 21, 2);
                final AbsoluteDate date = new AbsoluteDate(glonassTocYear, glonassTocMonth, glonassTocDay, glonassTocHours, glonassTocMin, glonassTocSec, pi.timeScales.getUTC());

                pi.glonassNav.setEpochToc(date);

                // TauN (we read -TauN) and GammaN
                pi.glonassNav.setTauN(-RinexUtils.parseDouble(line, 23, 19));
                pi.glonassNav.setGammaN(RinexUtils.parseDouble(line, 42, 19));

                // Date
                pi.glonassNav.setDate(date.getDate());

                // Time
                pi.glonassNav.setTime(fmod(RinexUtils.parseDouble(line, 61, 19), Constants.JULIAN_DAY));

            }

            /** {@inheritDoc} */
            @Override
            public void parseFirstBroadcastOrbit(final String line, final ParseInfo pi) {
                // X
                pi.glonassNav.setX(KM.toSI(RinexUtils.parseDouble(line, 4, 19) ));
                // Vx
                pi.glonassNav.setXDot(KM_PER_S.toSI(RinexUtils.parseDouble(line, 23, 19) ));
                // Ax
                pi.glonassNav.setXDotDot(KM_PER_S2.toSI(RinexUtils.parseDouble(line, 42, 19) ));
                // Health
                pi.glonassNav.setHealth(RinexUtils.parseDouble(line, 61, 19));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSecondBroadcastOrbit(final String line, final ParseInfo pi) {
                // Y
                pi.glonassNav.setY(KM.toSI(RinexUtils.parseDouble(line, 4, 19) ));
                // Vy
                pi.glonassNav.setYDot(KM_PER_S.toSI(RinexUtils.parseDouble(line, 23, 19) ));
                // Ay
                pi.glonassNav.setYDotDot(KM_PER_S2.toSI(RinexUtils.parseDouble(line, 42, 19) ));
                // Frequency number
                pi.glonassNav.setFrequencyNumber(RinexUtils.parseDouble(line, 61, 19));
            }

            /** {@inheritDoc} */
            @Override
            public void parseThirdBroadcastOrbit(final String line, final ParseInfo pi) {
                // Z
                pi.glonassNav.setZ(KM.toSI(RinexUtils.parseDouble(line, 4, 19) ));
                // Vz
                pi.glonassNav.setZDot(KM_PER_S.toSI(RinexUtils.parseDouble(line, 23, 19) ));
                // Az
                pi.glonassNav.setZDotDot(KM_PER_S2.toSI(RinexUtils.parseDouble(line, 42, 19) ));

                if (pi.file.getHeader().getFormatVersion() < 3.045) {
                    // Add the navigation message to the file
                    pi.file.addGlonassNavigationMessage(pi.glonassNav);
                    // Reinitialized the container for navigation data
                    pi.glonassNav = new GLONASSNavigationMessage();
                }
            }

            /** {@inheritDoc} */
            @Override
            public void parseFourthBroadcastOrbit(final String line, final ParseInfo pi) {
                if (pi.file.getHeader().getFormatVersion() > 3.045) {
                    // this line has been introduced in 3.05
                    pi.glonassNav.setStatusFlags((int) FastMath.rint(RinexUtils.parseDouble(line, 4, 19)));
                    double diff = RinexUtils.parseDouble(line, 23, 19);
                    if (Double.isNaN(diff)) {
                        diff = 0.999999999999e+09;
                    }
                    pi.glonassNav.setGroupDelayDifference(diff);
                    pi.glonassNav.setURA(RinexUtils.parseDouble(line, 42, 19));
                    double healthStatus = RinexUtils.parseDouble(line, 61, 19);
                    if (Double.isNaN(healthStatus)) {
                        healthStatus = 15.0;
                    }
                    pi.glonassNav.setHealthFlags((int) FastMath.rint(healthStatus));
                    // Add the navigation message to the file
                    pi.file.addGlonassNavigationMessage(pi.glonassNav);
                    // Reinitialized the container for navigation data
                    pi.glonassNav = new GLONASSNavigationMessage();
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

        /** SBAS. */
        SBAS("S") {

            /** {@inheritDoc} */
            @Override
            public void parseFirstLine(final String line, final ParseInfo pi) {
                // PRN
                pi.sbasNav.setPRN(RinexUtils.parseInt(line, 1, 2));

                // Toc
                final int sbasTocYear  = RinexUtils.parseInt(line, 4, 4);
                final int sbasTocMonth = RinexUtils.parseInt(line, 9, 2);
                final int sbasTocDay   = RinexUtils.parseInt(line, 12, 2);
                final int sbasTocHours = RinexUtils.parseInt(line, 15, 2);
                final int sbasTocMin   = RinexUtils.parseInt(line, 18, 2);
                final int sbasTocSec   = RinexUtils.parseInt(line, 21, 2);
                // Time scale (UTC for Rinex 3.01 and GPS for other RINEX versions)
                final int version100 = (int) FastMath.rint(pi.file.getHeader().getFormatVersion() * 100);
                final TimeScale    timeScale = (version100 == 301) ? pi.timeScales.getUTC() : pi.timeScales.getGPS();
                final AbsoluteDate refEpoch   = new AbsoluteDate(sbasTocYear, sbasTocMonth, sbasTocDay, sbasTocHours,
                                                                 sbasTocMin, sbasTocSec, timeScale);
                pi.sbasNav.setEpochToc(refEpoch);

                // AGf0 and AGf1
                pi.sbasNav.setAGf0(RinexUtils.parseDouble(line, 23, 19));
                pi.sbasNav.setAGf1(RinexUtils.parseDouble(line, 42, 19));
                pi.sbasNav.setTime(RinexUtils.parseDouble(line, 61, 19));

                // Set the ephemeris epoch (same as time of clock epoch)
                pi.sbasNav.setDate(refEpoch);
            }

            /** {@inheritDoc} */
            @Override
            public void parseFirstBroadcastOrbit(final String line, final ParseInfo pi) {
                // X
                pi.sbasNav.setX(KM.toSI(RinexUtils.parseDouble(line, 4, 19) ));
                // Vx
                pi.sbasNav.setXDot(KM_PER_S.toSI(RinexUtils.parseDouble(line, 23, 19) ));
                // Ax
                pi.sbasNav.setXDotDot(KM_PER_S2.toSI(RinexUtils.parseDouble(line, 42, 19) ));
                // Health
                pi.sbasNav.setHealth(RinexUtils.parseDouble(line, 61, 19));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSecondBroadcastOrbit(final String line, final ParseInfo pi) {
                // Y
                pi.sbasNav.setY(KM.toSI(RinexUtils.parseDouble(line, 4, 19) ));
                // Vy
                pi.sbasNav.setYDot(KM_PER_S.toSI(RinexUtils.parseDouble(line, 23, 19) ));
                // Ay
                pi.sbasNav.setYDotDot(KM_PER_S2.toSI(RinexUtils.parseDouble(line, 42, 19) ));
                // URA
                pi.sbasNav.setURA(RinexUtils.parseDouble(line, 61, 19));
            }

            /** {@inheritDoc} */
            @Override
            public void parseThirdBroadcastOrbit(final String line, final ParseInfo pi) {
                // Z
                pi.sbasNav.setZ(KM.toSI(RinexUtils.parseDouble(line, 4, 19) ));
                // Vz
                pi.sbasNav.setZDot(KM_PER_S.toSI(RinexUtils.parseDouble(line, 23, 19) ));
                // Az
                pi.sbasNav.setZDotDot(KM_PER_S2.toSI(RinexUtils.parseDouble(line, 42, 19) ));
                // IODN
                pi.sbasNav.setIODN(RinexUtils.parseDouble(line, 61, 19));

                // Add the navigation message to the file
                pi.file.addSBASNavigationMessage(pi.sbasNav);

                // Reinitialized the container for navigation data
                pi.sbasNav = new SBASNavigationMessage();

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

        };

        /** Parsing map. */
        private static final Map<String, SatelliteSystemLineParser> KEYS_MAP = new HashMap<>();
        static {
            for (final SatelliteSystemLineParser satelliteSystem : values()) {
                KEYS_MAP.put(satelliteSystem.getKey(), satelliteSystem);
            }
        }

        /** Satellite system key. */
        private String key;

        /**
         * Constructor.
         * @param key satellite system key
         */
        SatelliteSystemLineParser(final String key) {
            this.key = key;
        }

        /**
         * Getter for the satellite system key.
         * @return the satellite system key
         */
        public String getKey() {
            return key;
        }

        /** Parse a string to get the satellite system.
         * <p>
         * The string first character must be the satellite system.
         * </p>
         * @param s string to parse
         * @return the satellite system
         */
        public static SatelliteSystemLineParser getSatelliteSystemLineParser(final String s) {
            return KEYS_MAP.get(s);
        }

        /**
         * Parse the first line of the navigation message.
         * @param line line to read
         * @param pi holder for transient data
         */
        public abstract void parseFirstLine(String line, ParseInfo pi);

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
        public abstract void parseFourthBroadcastOrbit(String line, ParseInfo pi);

        /**
         * Parse the "BROADCASTORBIT - 5" line.
         * @param line line to read
         * @param pi holder for transient data
         */
        public abstract void parseFifthBroadcastOrbit(String line, ParseInfo pi);

        /**
         * Parse the "BROADCASTORBIT - 6" line.
         * @param line line to read
         * @param pi holder for transient data
         */
        public abstract void parseSixthBroadcastOrbit(String line, ParseInfo pi);

        /**
         * Parse the "BROADCASTORBIT - 7" line.
         * @param line line to read
         * @param pi holder for transient data
         */
        public abstract void parseSeventhBroadcastOrbit(String line, ParseInfo pi);

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
