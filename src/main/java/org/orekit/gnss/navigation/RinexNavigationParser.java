/* Copyright 2002-2022 CS GROUP
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.hipparchus.util.FastMath;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.gnss.TimeSystem;
import org.orekit.gnss.navigation.RinexNavigation.TimeSystemCorrection;
import org.orekit.propagation.analytical.gnss.data.BeidouNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.GLONASSNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.GPSNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.GalileoNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.IRNSSNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.QZSSNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.SBASNavigationMessage;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.GNSSDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScales;
import org.orekit.utils.Constants;

/**
 * Parser for RINEX navigation messages files.
 * <p>
 * This parser handles RINEX version from 3.01 to 3.05. It is not adapted for RINEX 2.10 and 2.11 versions.
 * </p>
 * @see <a href="https://files.igs.org/pub/data/format/rinex301.pdf"> 3.01 navigation messages file format</a>
 * @see <a href="https://files.igs.org/pub/data/format/rinex302.pdf"> 3.02 navigation messages file format</a>
 * @see <a href="https://files.igs.org/pub/data/format/rinex303.pdf"> 3.03 navigation messages file format</a>
 * @see <a href="https://files.igs.org/pub/data/format/rinex304.pdf"> 3.04 navigation messages file format</a>
 * @see <a href="https://files.igs.org/pub/data/format/rinex305.pdf"> 3.05 navigation messages file format</a>
 *
 * @author Bryan Cazabonne
 * @since 11.0
 *
 */
public class RinexNavigationParser {

    /** Handled clock file format versions. */
    private static final List<Double> HANDLED_VERSIONS = Arrays.asList(3.01, 3.02, 3.03, 3.04, 3.05);

    /** File Type. */
    private static final String FILE_TYPE = "N";

    /** Seconds to milliseconds converter. */
    private static final Double SEC_TO_MILLI = 1000.0;

    /** Kilometer to meters converter. */
    private static final Double KM_TO_M = 1000.0;

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
        final ParseInfo pi = new ParseInfo();

        int lineNumber = 0;
        Stream<LineParser> candidateParsers = Stream.of(LineParser.HEADER_VERSION);
        try (Reader reader = source.getOpener().openReaderOnce();
             BufferedReader br = new BufferedReader(reader)) {
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                ++lineNumber;
                final String l = line;
                final Optional<LineParser> selected = candidateParsers.filter(p -> p.canHandle(l)).findFirst();
                if (selected.isPresent()) {
                    try {
                        selected.get().parse(line, pi);
                    } catch (StringIndexOutOfBoundsException | NumberFormatException | InputMismatchException e) {
                        throw new OrekitException(e,
                                                  OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                  lineNumber, source.getName(), line);
                    }
                    candidateParsers = selected.get().allowedNext();
                } else {
                    throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                              lineNumber, source.getName(), line);
                }
            }
        }

        return pi.file;

    }

    /**
     * Parse a double value.
     * @param line line to parse
     * @param startIndex start index
     * @param size size of the value
     * @return the parsed value
     */
    private static double parseDouble(final String line, final int startIndex, final int size) {
        return Double.parseDouble(line.substring(startIndex, startIndex + size).replace('D', 'E').trim());
    }

    /**
     * Parse an integer value.
     * @param line line to parse
     * @param startIndex start index
     * @param size size of the value
     * @return the parsed value
     */
    private static int parseInt(final String line, final int startIndex, final int size) {
        return Integer.parseInt(line.substring(startIndex, startIndex + size).trim());
    }

    /**
     * Parse a string value.
     * @param line line to parse
     * @param startIndex start index
     * @param size size of the value
     * @return the parsed value
     */
    private static String parseString(final String line, final int startIndex, final int size) {
        return line.substring(startIndex, startIndex + size).trim();
    }

    /** Transient data used for parsing a RINEX navigation messages file. */
    private class ParseInfo {

        /** Set of time scales for parsing dates. */
        private final TimeScales timeScales;

        /** The corresponding navigation messages file object. */
        private RinexNavigation file;

        /** The version of the navigation file. */
        private double version;

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

        /** Constructor, build the ParseInfo object. */
        ParseInfo() {
            // Initialize default values for fields
            this.timeScales                   = RinexNavigationParser.this.timeScales;
            this.version                      = 1.0;
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
        HEADER_VERSION("^.+RINEX VERSION / TYPE( )*$") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {

                // Rinex version
                pi.version = parseDouble(line, 0, 9);

                // Throw exception if format version is not handled
                if (!HANDLED_VERSIONS.contains(pi.version)) {
                    throw new OrekitException(OrekitMessages.NAVIGATION_FILE_UNSUPPORTED_VERSION, pi.version);
                }
                pi.file.setFormatVersion(pi.version);

                // File type
                pi.file.setFileType(FILE_TYPE);

                // Satellite system
                final SatelliteSystem system = SatelliteSystem.parseSatelliteSystem(parseString(line, 40, 1));
                pi.file.setSatelliteSystem(system);

            }

            /** {@inheritDoc} */
            @Override
            public Stream<LineParser> allowedNext() {
                return Stream.of(HEADER_PROGRAM);
            }

        },

        /** Parser for generating program and emiting agency. */
        HEADER_PROGRAM("^.+PGM / RUN BY / DATE( )*$") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {

                // Name of the generating program
                final String programName = parseString(line, 0, 20);
                pi.file.setProgramName(programName);

                // Name of the emiting agency
                final String agencyName = parseString(line, 20, 20);
                pi.file.setAgencyName(agencyName);

                // Date and time of file creation
                final String date     = parseString(line, 40, 8);
                final String time     = parseString(line, 49, 6);
                final String timeZone = parseString(line, 56, 4);
                pi.file.setCreationDateString(date);
                pi.file.setCreationTimeString(time);
                pi.file.setCreationTimeZoneString(timeZone);

                // Convert date and time to an Orekit absolute date
                final DateComponents dateComponents = new DateComponents(parseInt(date, 0, 4),
                                                                         parseInt(date, 4, 2),
                                                                         parseInt(date, 6, 2));
                final TimeComponents timeComponents = new TimeComponents(parseInt(time, 0, 2),
                                                                         parseInt(time, 2, 2),
                                                                         parseInt(time, 4, 2));
                pi.file.setCreationDate(new AbsoluteDate(dateComponents,
                                                         timeComponents,
                                                         TimeSystem.parseTimeSystem(timeZone).getTimeScale(pi.timeScales)));

            }

            /** {@inheritDoc} */
            @Override
            public Stream<LineParser> allowedNext() {
                return Stream.of(HEADER_COMMENT, HEADER_IONOSPHERIC, HEADER_TIME, HEADER_LEAP_SECONDS, HEADER_END);
            }

        },

        /** Parser for comments. */
        HEADER_COMMENT("^.+COMMENT( )*$") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {
                pi.file.addComment(parseString(line, 0, 60));
            }

            /** {@inheritDoc} */
            @Override
            public Stream<LineParser> allowedNext() {
                return Stream.of(HEADER_COMMENT, HEADER_IONOSPHERIC, HEADER_TIME, HEADER_LEAP_SECONDS, HEADER_END);
            }

        },

        /** Parser for ionospheric correction parameters. */
        HEADER_IONOSPHERIC("^.+IONOSPHERIC CORR( )*$") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {

                // Satellite system
                final String ionoType = parseString(line, 0, 3);
                pi.file.setIonosphericCorrectionType(ionoType);

                // Read coefficients
                final double[] parameters = new double[4];
                parameters[0] = parseDouble(line, 5,  12);
                parameters[1] = parseDouble(line, 17, 12);
                parameters[2] = parseDouble(line, 29, 12);
                parameters[3] = parseDouble(line, 41, 12);

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

            }

            /** {@inheritDoc} */
            @Override
            public Stream<LineParser> allowedNext() {
                return Stream.of(HEADER_COMMENT, HEADER_IONOSPHERIC, HEADER_TIME, HEADER_LEAP_SECONDS, HEADER_END);
            }

        },

        /** Parser for corrections to transform the system time to UTC or to other time systems. */
        HEADER_TIME("^.+TIME SYSTEM CORR( )*$") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {

                // Read fields
                final String type    = parseString(line, 0,  4);
                final double a0      = parseDouble(line, 5,  17);
                final double a1      = parseDouble(line, 22, 16);
                final int    refTime = parseInt(line, 38, 7);
                final int    refWeek = parseInt(line, 46, 5);

                // Add to the list
                final TimeSystemCorrection tsc = new TimeSystemCorrection(type, a0, a1, refTime, refWeek);
                pi.file.addTimeSystemCorrections(tsc);

            }

            /** {@inheritDoc} */
            @Override
            public Stream<LineParser> allowedNext() {
                return Stream.of(HEADER_COMMENT, HEADER_TIME, HEADER_LEAP_SECONDS, HEADER_END);
            }

        },

        /** Parser for leap seconds. */
        HEADER_LEAP_SECONDS("^.+LEAP SECONDS( )*$") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {
                // Current number of leap seconds
                pi.file.setNumberOfLeapSeconds(parseInt(line, 0, 6));
            }

            /** {@inheritDoc} */
            @Override
            public Stream<LineParser> allowedNext() {
                return Stream.of(HEADER_COMMENT, HEADER_IONOSPHERIC, HEADER_TIME, HEADER_END);
            }

        },

        /** Parser for the end of header. */
        HEADER_END("^.+END OF HEADER( )*$") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {
                // do nothing...
            }

            /** {@inheritDoc} */
            @Override
            public Stream<LineParser> allowedNext() {
                return Stream.of(NAVIGATION_MESSAGE_FIRST);
            }

        },

        /** Parser for navigation message first data line. */
        NAVIGATION_MESSAGE_FIRST("(^G|^R|^E|^C|^I|^J|^S).+$") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {

                // Set the line number to 0
                pi.lineNumber = 0;

                // Current satellite system
                final String key = parseString(line, 0, 1);

                // Initialize parser
                pi.systemLineParser = SatelliteSystemLineParser.getSatelliteSystemLineParser(key);

                // Read first line
                pi.systemLineParser.parseFirstLine(line, pi);

            }

            /** {@inheritDoc} */
            @Override
            public Stream<LineParser> allowedNext() {
                return Stream.of(NAVIGATION_BROADCAST_ORBIT);
            }

        },

        /** Parser for broadcast orbit. */
        NAVIGATION_BROADCAST_ORBIT("^    .+") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {

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

            }

            /** {@inheritDoc} */
            @Override
            public Stream<LineParser> allowedNext() {
                return Stream.of(NAVIGATION_MESSAGE_FIRST, NAVIGATION_BROADCAST_ORBIT);
            }

        };

        /** Pattern for identifying line. */
        private final Pattern pattern;

        /** Simple constructor.
         * @param lineRegexp regular expression for identifying line
         */
        LineParser(final String lineRegexp) {
            pattern = Pattern.compile(lineRegexp);
        }

        /** Parse a line.
         * @param line line to parse
         * @param pi holder for transient data
         */
        public abstract void parse(String line, ParseInfo pi);

        /** Get the allowed parsers for next line.
         * @return allowed parsers for next line
         */
        public abstract Stream<LineParser> allowedNext();

        /** Check if parser can handle line.
         * @param line line to parse
         * @return true if parser can handle the specified line
         */
        public boolean canHandle(final String line) {
            return pattern.matcher(line).matches();
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
                pi.gpsNav.setPRN(parseInt(line, 1, 2));

                // Toc
                final int gpsTocYear  = parseInt(line, 4, 4);
                final int gpsTocMonth = parseInt(line, 9, 2);
                final int gpsTocDay   = parseInt(line, 12, 2);
                final int gpsTocHours = parseInt(line, 15, 2);
                final int gpsTocMin   = parseInt(line, 18, 2);
                final int gpsTocSec   = parseInt(line, 21, 2);
                pi.gpsNav.setEpochToc(new AbsoluteDate(gpsTocYear, gpsTocMonth, gpsTocDay, gpsTocHours,
                                                       gpsTocMin, gpsTocSec, pi.timeScales.getGPS()));

                // Af0, Af1, and Af2
                pi.gpsNav.setAf0(parseDouble(line, 23, 19));
                pi.gpsNav.setAf1(parseDouble(line, 42, 19));
                pi.gpsNav.setAf2(parseDouble(line, 61, 19));
            }

            /** {@inheritDoc} */
            @Override
            public void parseFirstBroadcastOrbit(final String line, final ParseInfo pi) {
                // IODE
                pi.gpsNav.setIODE(parseDouble(line, 4, 19));
                // Crs
                pi.gpsNav.setCrs(parseDouble(line, 23, 19));
                // Delta n
                pi.gpsNav.setDeltaN(parseDouble(line, 42, 19));
                // M0
                pi.gpsNav.setM0(parseDouble(line, 61, 19));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSecondBroadcastOrbit(final String line, final ParseInfo pi) {
                // Cuc
                pi.gpsNav.setCuc(parseDouble(line, 4, 19));
                // e
                pi.gpsNav.setE(parseDouble(line, 23, 19));
                // Cus
                pi.gpsNav.setCus(parseDouble(line, 42, 19));
                // sqrt(A)
                pi.gpsNav.setSqrtA(parseDouble(line, 61, 19));
            }

            /** {@inheritDoc} */
            @Override
            public void parseThirdBroadcastOrbit(final String line, final ParseInfo pi) {
                // Toe
                pi.gpsNav.setTime(parseDouble(line, 4, 19));
                // Cic
                pi.gpsNav.setCic(parseDouble(line, 23, 19));
                // Omega0
                pi.gpsNav.setOmega0(parseDouble(line, 42, 19));
                // Cis
                pi.gpsNav.setCis(parseDouble(line, 61, 19));
            }

            /** {@inheritDoc} */
            @Override
            public void parseFourthBroadcastOrbit(final String line, final ParseInfo pi) {
                // i0
                pi.gpsNav.setI0(parseDouble(line, 4, 19));
                // Crc
                pi.gpsNav.setCrc(parseDouble(line, 23, 19));
                // omega
                pi.gpsNav.setPa(parseDouble(line, 42, 19));
                // OMEGA DOT
                pi.gpsNav.setOmegaDot(parseDouble(line, 61, 19));
            }

            /** {@inheritDoc} */
            @Override
            public void parseFifthBroadcastOrbit(final String line, final ParseInfo pi) {
                // iDot
                pi.gpsNav.setIDot(parseDouble(line, 4, 19));
                // Codes on L2 channel (ignored)
                // parseDouble(line, 23, 19)
                // GPS week (to go with Toe)
                pi.gpsNav.setWeek((int) parseDouble(line, 42, 19));
                pi.gpsNav.setDate(new GNSSDate(pi.gpsNav.getWeek(),
                                               SEC_TO_MILLI * pi.gpsNav.getTime(),
                                               SatelliteSystem.GPS,
                                               pi.timeScales).getDate());
            }

            /** {@inheritDoc} */
            @Override
            public void parseSixthBroadcastOrbit(final String line, final ParseInfo pi) {
                // SV accuracy
                pi.gpsNav.setSvAccuracy(parseDouble(line, 4, 19));
                // Health
                pi.gpsNav.setSvHealth(parseDouble(line, 23, 19));
                // TGD
                pi.gpsNav.setTGD(parseDouble(line, 42, 19));
                // IODC
                pi.gpsNav.setIODC(parseDouble(line, 61, 19));
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
                pi.galileoNav.setPRN(parseInt(line, 1, 2));

                // Toc
                final int galileoTocYear  = parseInt(line, 4, 4);
                final int galileoTocMonth = parseInt(line, 9, 2);
                final int galileoTocDay   = parseInt(line, 12, 2);
                final int galileoTocHours = parseInt(line, 15, 2);
                final int galileoTocMin   = parseInt(line, 18, 2);
                final int galileoTocSec   = parseInt(line, 21, 2);
                pi.galileoNav.setEpochToc(new AbsoluteDate(galileoTocYear, galileoTocMonth, galileoTocDay, galileoTocHours,
                                                       galileoTocMin, galileoTocSec, pi.timeScales.getGST()));

                // Af0, Af1, and Af2
                pi.galileoNav.setAf0(parseDouble(line, 23, 19));
                pi.galileoNav.setAf1(parseDouble(line, 42, 19));
                pi.galileoNav.setAf2(parseDouble(line, 61, 19));
            }

            /** {@inheritDoc} */
            @Override
            public void parseFirstBroadcastOrbit(final String line, final ParseInfo pi) {
                // IODNav
                pi.galileoNav.setIODNav(parseDouble(line, 4, 19));
                // Crs
                pi.galileoNav.setCrs(parseDouble(line, 23, 19));
                // Delta n
                pi.galileoNav.setDeltaN(parseDouble(line, 42, 19));
                // M0
                pi.galileoNav.setM0(parseDouble(line, 61, 19));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSecondBroadcastOrbit(final String line, final ParseInfo pi) {
                // Cuc
                pi.galileoNav.setCuc(parseDouble(line, 4, 19));
                // e
                pi.galileoNav.setE(parseDouble(line, 23, 19));
                // Cus
                pi.galileoNav.setCus(parseDouble(line, 42, 19));
                // sqrt(A)
                pi.galileoNav.setSqrtA(parseDouble(line, 61, 19));
            }

            /** {@inheritDoc} */
            @Override
            public void parseThirdBroadcastOrbit(final String line, final ParseInfo pi) {
                // Toe
                pi.galileoNav.setTime(parseDouble(line, 4, 19));
                // Cic
                pi.galileoNav.setCic(parseDouble(line, 23, 19));
                // Omega0
                pi.galileoNav.setOmega0(parseDouble(line, 42, 19));
                // Cis
                pi.galileoNav.setCis(parseDouble(line, 61, 19));
            }

            /** {@inheritDoc} */
            @Override
            public void parseFourthBroadcastOrbit(final String line, final ParseInfo pi) {
                // i0
                pi.galileoNav.setI0(parseDouble(line, 4, 19));
                // Crc
                pi.galileoNav.setCrc(parseDouble(line, 23, 19));
                // omega
                pi.galileoNav.setPa(parseDouble(line, 42, 19));
                // OMEGA DOT
                pi.galileoNav.setOmegaDot(parseDouble(line, 61, 19));
            }

            /** {@inheritDoc} */
            @Override
            public void parseFifthBroadcastOrbit(final String line, final ParseInfo pi) {
                // iDot
                pi.galileoNav.setIDot(parseDouble(line, 4, 19));
                // Data sources (ignored)
                // parseDouble(line, 23, 19)
                // GAL week (to go with Toe)
                pi.galileoNav.setWeek((int) parseDouble(line, 42, 19));
                pi.galileoNav.setDate(new GNSSDate(pi.galileoNav.getWeek(),
                                                   SEC_TO_MILLI * pi.galileoNav.getTime(),
                                                   SatelliteSystem.GPS, // in Rinex files, week number is aligned to GPS week!
                                                   pi.timeScales).getDate());
            }

            /** {@inheritDoc} */
            @Override
            public void parseSixthBroadcastOrbit(final String line, final ParseInfo pi) {
                // SISA
                pi.galileoNav.setSisa(parseDouble(line, 4, 19));
                // Health
                pi.galileoNav.setSvHealth(parseDouble(line, 23, 19));
                // E5a/E1 BGD
                pi.galileoNav.setBGDE1E5a(parseDouble(line, 42, 19));
                // E5b/E1 BGD
                pi.galileoNav.setBGDE5bE1(parseDouble(line, 61, 19));
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
                pi.beidouNav.setPRN(parseInt(line, 1, 2));

                // Toc
                final int beidouTocYear  = parseInt(line, 4, 4);
                final int beidouTocMonth = parseInt(line, 9, 2);
                final int beidouTocDay   = parseInt(line, 12, 2);
                final int beidouTocHours = parseInt(line, 15, 2);
                final int beidouTocMin   = parseInt(line, 18, 2);
                final int beidouTocSec   = parseInt(line, 21, 2);
                pi.beidouNav.setEpochToc(new AbsoluteDate(beidouTocYear, beidouTocMonth, beidouTocDay, beidouTocHours,
                                                       beidouTocMin, beidouTocSec, pi.timeScales.getBDT()));

                // Af0, Af1, and Af2
                pi.beidouNav.setAf0(parseDouble(line, 23, 19));
                pi.beidouNav.setAf1(parseDouble(line, 42, 19));
                pi.beidouNav.setAf2(parseDouble(line, 61, 19));
            }

            /** {@inheritDoc} */
            @Override
            public void parseFirstBroadcastOrbit(final String line, final ParseInfo pi) {
                // AODE
                pi.beidouNav.setAODE(parseDouble(line, 4, 19));
                // Crs
                pi.beidouNav.setCrs(parseDouble(line, 23, 19));
                // Delta n
                pi.beidouNav.setDeltaN(parseDouble(line, 42, 19));
                // M0
                pi.beidouNav.setM0(parseDouble(line, 61, 19));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSecondBroadcastOrbit(final String line, final ParseInfo pi) {
                // Cuc
                pi.beidouNav.setCuc(parseDouble(line, 4, 19));
                // e
                pi.beidouNav.setE(parseDouble(line, 23, 19));
                // Cus
                pi.beidouNav.setCus(parseDouble(line, 42, 19));
                // sqrt(A)
                pi.beidouNav.setSqrtA(parseDouble(line, 61, 19));
            }

            /** {@inheritDoc} */
            @Override
            public void parseThirdBroadcastOrbit(final String line, final ParseInfo pi) {
                // Toe
                pi.beidouNav.setTime(parseDouble(line, 4, 19));
                // Cic
                pi.beidouNav.setCic(parseDouble(line, 23, 19));
                // Omega0
                pi.beidouNav.setOmega0(parseDouble(line, 42, 19));
                // Cis
                pi.beidouNav.setCis(parseDouble(line, 61, 19));
            }

            /** {@inheritDoc} */
            @Override
            public void parseFourthBroadcastOrbit(final String line, final ParseInfo pi) {
                // i0
                pi.beidouNav.setI0(parseDouble(line, 4, 19));
                // Crc
                pi.beidouNav.setCrc(parseDouble(line, 23, 19));
                // omega
                pi.beidouNav.setPa(parseDouble(line, 42, 19));
                // OMEGA DOT
                pi.beidouNav.setOmegaDot(parseDouble(line, 61, 19));
            }

            /** {@inheritDoc} */
            @Override
            public void parseFifthBroadcastOrbit(final String line, final ParseInfo pi) {
                // iDot
                pi.beidouNav.setIDot(parseDouble(line, 4, 19));
                // BDT week (to go with Toe)
                pi.beidouNav.setWeek((int) parseDouble(line, 42, 19));
                pi.beidouNav.setDate(new GNSSDate(pi.beidouNav.getWeek(),
                                                  SEC_TO_MILLI * pi.beidouNav.getTime(),
                                                  SatelliteSystem.BEIDOU,
                                                  pi.timeScales).getDate());
            }

            /** {@inheritDoc} */
            @Override
            public void parseSixthBroadcastOrbit(final String line, final ParseInfo pi) {
                // SV accuracy
                pi.beidouNav.setSvAccuracy(parseDouble(line, 4, 19));
                // SatH1 (ignored)
                // parseDouble(line, 23, 19)
                // TGD1
                pi.beidouNav.setTGD1(parseDouble(line, 42, 19));
                // TGD2
                pi.beidouNav.setTGD2(parseDouble(line, 61, 19));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSeventhBroadcastOrbit(final String line, final ParseInfo pi) {
                // Transmission time of message (ignored)
                // parseDouble(line, 4, 19);
                // AODC
                pi.beidouNav.setAODC(parseDouble(line, 23, 19));
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
                pi.qzssNav.setPRN(parseInt(line, 1, 2));

                // Toc
                final int qzssTocYear  = parseInt(line, 4, 4);
                final int qzssTocMonth = parseInt(line, 9, 2);
                final int qzssTocDay   = parseInt(line, 12, 2);
                final int qzssTocHours = parseInt(line, 15, 2);
                final int qzssTocMin   = parseInt(line, 18, 2);
                final int qzssTocSec   = parseInt(line, 21, 2);
                pi.qzssNav.setEpochToc(new AbsoluteDate(qzssTocYear, qzssTocMonth, qzssTocDay, qzssTocHours,
                                                       qzssTocMin, qzssTocSec, pi.timeScales.getQZSS()));

                // Af0, Af1, and Af2
                pi.qzssNav.setAf0(parseDouble(line, 23, 19));
                pi.qzssNav.setAf1(parseDouble(line, 42, 19));
                pi.qzssNav.setAf2(parseDouble(line, 61, 19));
            }

            /** {@inheritDoc} */
            @Override
            public void parseFirstBroadcastOrbit(final String line, final ParseInfo pi) {
                // IODE
                pi.qzssNav.setIODE(parseDouble(line, 4, 19));
                // Crs
                pi.qzssNav.setCrs(parseDouble(line, 23, 19));
                // Delta n
                pi.qzssNav.setDeltaN(parseDouble(line, 42, 19));
                // M0
                pi.qzssNav.setM0(parseDouble(line, 61, 19));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSecondBroadcastOrbit(final String line, final ParseInfo pi) {
                // Cuc
                pi.qzssNav.setCuc(parseDouble(line, 4, 19));
                // e
                pi.qzssNav.setE(parseDouble(line, 23, 19));
                // Cus
                pi.qzssNav.setCus(parseDouble(line, 42, 19));
                // sqrt(A)
                pi.qzssNav.setSqrtA(parseDouble(line, 61, 19));
            }

            /** {@inheritDoc} */
            @Override
            public void parseThirdBroadcastOrbit(final String line, final ParseInfo pi) {
                // Toe
                pi.qzssNav.setTime(parseDouble(line, 4, 19));
                // Cic
                pi.qzssNav.setCic(parseDouble(line, 23, 19));
                // Omega0
                pi.qzssNav.setOmega0(parseDouble(line, 42, 19));
                // Cis
                pi.qzssNav.setCis(parseDouble(line, 61, 19));
            }

            /** {@inheritDoc} */
            @Override
            public void parseFourthBroadcastOrbit(final String line, final ParseInfo pi) {
                // i0
                pi.qzssNav.setI0(parseDouble(line, 4, 19));
                // Crc
                pi.qzssNav.setCrc(parseDouble(line, 23, 19));
                // omega
                pi.qzssNav.setPa(parseDouble(line, 42, 19));
                // OMEGA DOT
                pi.qzssNav.setOmegaDot(parseDouble(line, 61, 19));
            }

            /** {@inheritDoc} */
            @Override
            public void parseFifthBroadcastOrbit(final String line, final ParseInfo pi) {
                // iDot
                pi.qzssNav.setIDot(parseDouble(line, 4, 19));
                // Codes on L2 channel (ignored)
                // parseDouble(line, 23, 19)
                // GPS week (to go with Toe)
                pi.qzssNav.setWeek((int) parseDouble(line, 42, 19));
                pi.qzssNav.setDate(new GNSSDate(pi.qzssNav.getWeek(),
                                                SEC_TO_MILLI * pi.qzssNav.getTime(),
                                                SatelliteSystem.GPS, // in Rinex files, week number is aligned to GPS week!
                                                pi.timeScales).getDate());
            }

            /** {@inheritDoc} */
            @Override
            public void parseSixthBroadcastOrbit(final String line, final ParseInfo pi) {
                // SV accuracy
                pi.qzssNav.setSvAccuracy(parseDouble(line, 4, 19));
                // Health
                pi.qzssNav.setSvHealth(parseDouble(line, 23, 19));
                // TGD
                pi.qzssNav.setTGD(parseDouble(line, 42, 19));
                // IODC
                pi.qzssNav.setIODC(parseDouble(line, 61, 19));
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
                pi.irnssNav.setPRN(parseInt(line, 1, 2));

                // Toc
                final int irnssTocYear  = parseInt(line, 4, 4);
                final int irnssTocMonth = parseInt(line, 9, 2);
                final int irnssTocDay   = parseInt(line, 12, 2);
                final int irnssTocHours = parseInt(line, 15, 2);
                final int irnssTocMin   = parseInt(line, 18, 2);
                final int irnssTocSec   = parseInt(line, 21, 2);
                pi.irnssNav.setEpochToc(new AbsoluteDate(irnssTocYear, irnssTocMonth, irnssTocDay, irnssTocHours,
                                                         irnssTocMin, irnssTocSec, pi.timeScales.getIRNSS()));

                // Af0, Af1, and Af2
                pi.irnssNav.setAf0(parseDouble(line, 23, 19));
                pi.irnssNav.setAf1(parseDouble(line, 42, 19));
                pi.irnssNav.setAf2(parseDouble(line, 61, 19));
            }

            /** {@inheritDoc} */
            @Override
            public void parseFirstBroadcastOrbit(final String line, final ParseInfo pi) {
                // IODEC
                pi.irnssNav.setIODEC(parseDouble(line, 4, 19));
                // Crs
                pi.irnssNav.setCrs(parseDouble(line, 23, 19));
                // Delta n
                pi.irnssNav.setDeltaN(parseDouble(line, 42, 19));
                // M0
                pi.irnssNav.setM0(parseDouble(line, 61, 19));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSecondBroadcastOrbit(final String line, final ParseInfo pi) {
                // Cuc
                pi.irnssNav.setCuc(parseDouble(line, 4, 19));
                // e
                pi.irnssNav.setE(parseDouble(line, 23, 19));
                // Cus
                pi.irnssNav.setCus(parseDouble(line, 42, 19));
                // sqrt(A)
                pi.irnssNav.setSqrtA(parseDouble(line, 61, 19));
            }

            /** {@inheritDoc} */
            @Override
            public void parseThirdBroadcastOrbit(final String line, final ParseInfo pi) {
                // Toe
                pi.irnssNav.setTime(parseDouble(line, 4, 19));
                // Cic
                pi.irnssNav.setCic(parseDouble(line, 23, 19));
                // Omega0
                pi.irnssNav.setOmega0(parseDouble(line, 42, 19));
                // Cis
                pi.irnssNav.setCis(parseDouble(line, 61, 19));
            }

            /** {@inheritDoc} */
            @Override
            public void parseFourthBroadcastOrbit(final String line, final ParseInfo pi) {
                // i0
                pi.irnssNav.setI0(parseDouble(line, 4, 19));
                // Crc
                pi.irnssNav.setCrc(parseDouble(line, 23, 19));
                // omega
                pi.irnssNav.setPa(parseDouble(line, 42, 19));
                // OMEGA DOT
                pi.irnssNav.setOmegaDot(parseDouble(line, 61, 19));
            }

            /** {@inheritDoc} */
            @Override
            public void parseFifthBroadcastOrbit(final String line, final ParseInfo pi) {
                // iDot
                pi.irnssNav.setIDot(parseDouble(line, 4, 19));
                // IRNSS week (to go with Toe)
                pi.irnssNav.setWeek((int) parseDouble(line, 42, 19));
                pi.irnssNav.setDate(new GNSSDate(pi.irnssNav.getWeek(),
                                                 SEC_TO_MILLI * pi.irnssNav.getTime(),
                                                 SatelliteSystem.GPS, // in Rinex files, week number is aligned to GPS week!
                                                 pi.timeScales).getDate());
            }

            /** {@inheritDoc} */
            @Override
            public void parseSixthBroadcastOrbit(final String line, final ParseInfo pi) {
                // SV accuracy
                pi.irnssNav.setURA(parseDouble(line, 4, 19));
                // Health
                pi.irnssNav.setSvHealth(parseDouble(line, 23, 19));
                // TGD
                pi.irnssNav.setTGD(parseDouble(line, 42, 19));
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
                pi.glonassNav.setPRN(parseInt(line, 1, 2));

                // Toc
                final int glonassTocYear  = parseInt(line, 4, 4);
                final int glonassTocMonth = parseInt(line, 9, 2);
                final int glonassTocDay   = parseInt(line, 12, 2);
                final int glonassTocHours = parseInt(line, 15, 2);
                final int glonassTocMin   = parseInt(line, 18, 2);
                final int glonassTocSec   = parseInt(line, 21, 2);
                final AbsoluteDate date = new AbsoluteDate(glonassTocYear, glonassTocMonth, glonassTocDay, glonassTocHours,
                                                           glonassTocMin, glonassTocSec, pi.timeScales.getUTC());

                // Build a GPS date
                final GNSSDate gpsEpoch = new GNSSDate(date, SatelliteSystem.GPS, pi.timeScales);

                // Toc rounded by 15 min in UTC
                final double secInWeek = FastMath.floor((0.001 * gpsEpoch.getMilliInWeek() + 450.0) / 900.0) * 900.0;
                final AbsoluteDate rounded = new GNSSDate(gpsEpoch.getWeekNumber(),
                                                          SEC_TO_MILLI * secInWeek,
                                                          SatelliteSystem.GPS, pi.timeScales).getDate();

                pi.glonassNav.setEpochToc(rounded);

                // TauN (we read -TauN) and GammaN
                pi.glonassNav.setTauN(-parseDouble(line, 23, 19));
                pi.glonassNav.setGammaN(parseDouble(line, 42, 19));

                // Date
                pi.glonassNav.setDate(rounded);

                // Time
                pi.glonassNav.setTime(fmod(parseDouble(line, 61, 19), Constants.JULIAN_DAY));

            }

            /** {@inheritDoc} */
            @Override
            public void parseFirstBroadcastOrbit(final String line, final ParseInfo pi) {
                // X
                pi.glonassNav.setX(parseDouble(line, 4, 19) * KM_TO_M);
                // Vx
                pi.glonassNav.setXDot(parseDouble(line, 23, 19) * KM_TO_M);
                // Ax
                pi.glonassNav.setXDotDot(parseDouble(line, 42, 19) * KM_TO_M);
                // Health
                pi.glonassNav.setHealth(parseDouble(line, 61, 19));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSecondBroadcastOrbit(final String line, final ParseInfo pi) {
                // Y
                pi.glonassNav.setY(parseDouble(line, 4, 19) * KM_TO_M);
                // Vy
                pi.glonassNav.setYDot(parseDouble(line, 23, 19) * KM_TO_M);
                // Ay
                pi.glonassNav.setYDotDot(parseDouble(line, 42, 19) * KM_TO_M);
                // Frequency number
                pi.glonassNav.setFrequencyNumber(parseDouble(line, 61, 19));
            }

            /** {@inheritDoc} */
            @Override
            public void parseThirdBroadcastOrbit(final String line, final ParseInfo pi) {
                // Z
                pi.glonassNav.setZ(parseDouble(line, 4, 19) * KM_TO_M);
                // Vz
                pi.glonassNav.setZDot(parseDouble(line, 23, 19) * KM_TO_M);
                // Az
                pi.glonassNav.setZDotDot(parseDouble(line, 42, 19) * KM_TO_M);

                // Add the navigation message to the file
                pi.file.addGlonassNavigationMessage(pi.glonassNav);
                // Reinitialized the container for navigation data
                pi.glonassNav = new GLONASSNavigationMessage();
            }

            /** {@inheritDoc} */
            @Override
            public void parseFourthBroadcastOrbit(final String line, final ParseInfo pi) {
                // Nothing to do for GLONASS
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
                pi.sbasNav.setPRN(parseInt(line, 1, 2));

                // Toc
                final int sbasTocYear  = parseInt(line, 4, 4);
                final int sbasTocMonth = parseInt(line, 9, 2);
                final int sbasTocDay   = parseInt(line, 12, 2);
                final int sbasTocHours = parseInt(line, 15, 2);
                final int sbasTocMin   = parseInt(line, 18, 2);
                final int sbasTocSec   = parseInt(line, 21, 2);
                // Time scale (UTC for Rinex 3.01 and GPS for other RINEX versions)
                final TimeScale    timeScale = ((int) pi.version * 100 == 301) ? pi.timeScales.getUTC() : pi.timeScales.getGPS();
                final AbsoluteDate refEpoch   = new AbsoluteDate(sbasTocYear, sbasTocMonth, sbasTocDay, sbasTocHours,
                                                                 sbasTocMin, sbasTocSec, timeScale);
                pi.sbasNav.setEpochToc(refEpoch);

                // AGf0 and AGf1
                pi.sbasNav.setAGf0(parseDouble(line, 23, 19));
                pi.sbasNav.setAGf1(parseDouble(line, 42, 19));
                pi.sbasNav.setTime(parseDouble(line, 61, 19));

                // Set the ephemeris epoch (same as time of clock epoch)
                pi.sbasNav.setDate(refEpoch);
            }

            /** {@inheritDoc} */
            @Override
            public void parseFirstBroadcastOrbit(final String line, final ParseInfo pi) {
                // X
                pi.sbasNav.setX(parseDouble(line, 4, 19) * KM_TO_M);
                // Vx
                pi.sbasNav.setXDot(parseDouble(line, 23, 19) * KM_TO_M);
                // Ax
                pi.sbasNav.setXDotDot(parseDouble(line, 42, 19) * KM_TO_M);
                // Health
                pi.sbasNav.setHealth(parseDouble(line, 61, 19));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSecondBroadcastOrbit(final String line, final ParseInfo pi) {
                // Y
                pi.sbasNav.setY(parseDouble(line, 4, 19) * KM_TO_M);
                // Vy
                pi.sbasNav.setYDot(parseDouble(line, 23, 19) * KM_TO_M);
                // Ay
                pi.sbasNav.setYDotDot(parseDouble(line, 42, 19) * KM_TO_M);
                // URA
                pi.sbasNav.setURA(parseDouble(line, 61, 19));
            }

            /** {@inheritDoc} */
            @Override
            public void parseThirdBroadcastOrbit(final String line, final ParseInfo pi) {
                // Z
                pi.sbasNav.setZ(parseDouble(line, 4, 19) * KM_TO_M);
                // Vz
                pi.sbasNav.setZDot(parseDouble(line, 23, 19) * KM_TO_M);
                // Az
                pi.sbasNav.setZDotDot(parseDouble(line, 42, 19) * KM_TO_M);
                // IODN
                pi.sbasNav.setIODN(parseDouble(line, 61, 19));

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

}
