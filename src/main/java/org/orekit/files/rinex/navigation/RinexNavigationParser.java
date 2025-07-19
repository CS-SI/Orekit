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
import java.util.function.Function;

import org.hipparchus.util.FastMath;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.rinex.section.CommonLabel;
import org.orekit.files.rinex.utils.ParsingUtils;
import org.orekit.gnss.PredefinedGnssSignal;
import org.orekit.gnss.PredefinedTimeSystem;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.propagation.analytical.gnss.data.AbstractNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.BeidouCivilianNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.BeidouLegacyNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.BeidouSatelliteType;
import org.orekit.propagation.analytical.gnss.data.GLONASSFdmaNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.GPSCivilianNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.GPSLegacyNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.GalileoNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.NavICL1NVNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.NavICLegacyNavigationMessage;
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

    /** URA index to URA mapping (table 23 of NavIC ICD). */
    // CHECKSTYLE: stop Indentation check
    private static final double[] NAVIC_URA = {
           2.40,    3.40,    4.85,   6.85,
           9.65,   13.65,   24.00,  48.00,
          96.00,  192.00,  384.00, 768.00,
        1536.00, 3072.00, 6144.00, Double.NaN
    };
    // CHECKSTYLE: resume Indentation check

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
        final ParseInfo parseInfo = new ParseInfo(source.getName());

        Iterable<LineParser> candidateParsers = Collections.singleton(LineParser.HEADER_VERSION);
        try (Reader reader = source.getOpener().openReaderOnce();
             BufferedReader br = new BufferedReader(reader)) {
            nextLine:
                for (String line = br.readLine(); line != null; line = br.readLine()) {
                    ++parseInfo.lineNumber;
                    for (final LineParser candidate : candidateParsers) {
                        if (candidate.canHandle.apply(parseInfo.file.getHeader(), line)) {
                            try {
                                candidate.parsingMethod.parse(line, parseInfo);
                                candidateParsers = candidate.allowedNextProvider.apply(parseInfo);
                                continue nextLine;
                            } catch (StringIndexOutOfBoundsException | NumberFormatException | InputMismatchException e) {
                                throw new OrekitException(e,
                                                          OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                          parseInfo.lineNumber, source.getName(), line);
                            }
                        }
                    }
                    throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                              parseInfo.lineNumber, source.getName(), line);
                }
        }

        if (!parseInfo.headerParsed) {
            throw new OrekitException(OrekitMessages.UNEXPECTED_END_OF_FILE, source.getName());
        }

        parseInfo.closePendingMessage();

        return parseInfo.file;

    }

    /** Transient data used for parsing a RINEX navigation messages file. */
    private class ParseInfo {

        /** Name of the data source. */
        private final String name;

        /** Set of time scales for parsing dates. */
        private final TimeScales timeScales;

        /** The corresponding navigation messages file object. */
        private final RinexNavigation file;

        /** Number of initial spaces in broadcast orbits lines. */
        private int initialSpaces;

        /** Flag indicating header has been completely parsed. */
        private boolean headerParsed;

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
        private BeidouLegacyNavigationMessage beidouLNav;

        /** Container for Beidou navigation message. */
        private BeidouCivilianNavigationMessage beidouCNav;

        /** Container for QZSS navigation message. */
        private QZSSLegacyNavigationMessage qzssLNav;

        /** Container for QZSS navigation message. */
        private QZSSCivilianNavigationMessage qzssCNav;

        /** Container for NavIC navigation message. */
        private NavICLegacyNavigationMessage navicLNav;

        /** Container for NavIC navigation message.
         * @since 13.0
         */
        private NavICL1NVNavigationMessage navicL1NV;

        /** Container for GLONASS navigation message. */
        private GLONASSFdmaNavigationMessage glonassFdmaNav;

        /** Container for SBAS navigation message. */
        private SBASNavigationMessage sbasNav;

        /** Container for System Time Offset message. */
        private SystemTimeOffsetMessage sto;

        /** Container for Earth Orientation Parameter message. */
        private EarthOrientationParameterMessage eop;

        /** Container for ionosphere Klobuchar message. */
        private IonosphereKlobucharMessage klobuchar;

        /** Container for NacIV Klobuchar message.
         * @since 14.0
         */
        private IonosphereNavICKlobucharMessage navICKlobuchar;

        /** Container for NacIV NeQuick N message.
         * @since 14.0
         */
        private IonosphereNavICNeQuickNMessage navICNeQuickN;

        /** Container for GLONASS CDMS message.
         * @since 14.0
         */
        private IonosphereGlonassCdmsMessage glonassCdms;

        /** Container for ionosphere Nequick-G message. */
        private IonosphereNequickGMessage nequickG;

        /** Container for ionosphere BDGIM message. */
        private IonosphereBDGIMMessage bdgim;

        /** Constructor, build the ParseInfo object.
         * @param name name of the data source
         */
        ParseInfo(final String name) {
            // Initialize default values for fields
            this.name                         = name;
            this.timeScales                   = RinexNavigationParser.this.timeScales;
            this.isIonosphereAlphaInitialized = false;
            this.file                         = new RinexNavigation();

            // reset the default values set by header constructor
            this.file.getHeader().setProgramName(null);
            this.file.getHeader().setRunByName(null);
            this.file.getHeader().setCreationDateComponents(null);

        }

        /** Parse a date.
         * @param line line to parse
         * @param system satellite system
         * @return parsed date
         * @since 14.0
         */
        private AbsoluteDate parseDate(final String line, final SatelliteSystem system) {
            return parseDate(line, system.getObservationTimeScale().getTimeScale(timeScales));
        }

        /** Parse a date.
         * @param line line to parse
         * @param timeScale time scale
         * @return parsed date
         * @since 14.0
         */
        private AbsoluteDate parseDate(final String line, final TimeScale timeScale) {
            final int year  = ParsingUtils.parseInt(line, 4, 4);
            final int month = ParsingUtils.parseInt(line, 9, 2);
            final int day   = ParsingUtils.parseInt(line, 12, 2);
            final int hours = ParsingUtils.parseInt(line, 15, 2);
            final int min   = ParsingUtils.parseInt(line, 18, 2);
            final int sec   = ParsingUtils.parseInt(line, 21, 2);
            return new AbsoluteDate(year, month, day, hours, min, sec, timeScale);
        }

        /** Parse field 1 of a message line.
         * @param line line to parse
         * @param unit unit to apply
         * @return parsed field
         * @since 14.0
         */
        private double parseDouble1(final String line, final Unit unit) {
            return parseDouble(line, unit, initialSpaces);
        }

        /** Parse field 1 of a message line.
         * @param line line to parse
         * @return parsed field
         * @since 14.0
         */
        private int parseInt1(final String line) {
            return parseInt(line, initialSpaces);
        }

        /** Parse field 2 of a message line.
         * @param line line to parse
         * @param unit unit to apply
         * @return parsed field
         * @since 14.0
         */
        private double parseDouble2(final String line, final Unit unit) {
            return parseDouble(line, unit, initialSpaces + 19);
        }

        /** Parse field 2 of a message line.
         * @param line line to parse
         * @return parsed field
         * @since 14.0
         */
        private int parseInt2(final String line) {
            return parseInt(line, initialSpaces + 19);
        }

        /** Parse field 3 of a message line.
         * @param line line to parse
         * @param unit unit to apply
         * @return parsed field
         * @since 14.0
         */
        private double parseDouble3(final String line, final Unit unit) {
            return parseDouble(line, unit, initialSpaces + 38);
        }

        /** Parse field 3 of a message line.
         * @param line line to parse
         * @return parsed field
         * @since 14.0
         */
        private int parseInt3(final String line) {
            return parseInt(line, initialSpaces + 38);
        }

        /** Parse field 4 of a message line.
         * @param line line to parse
         * @param unit unit to apply
         * @return parsed field
         * @since 14.0
         */
        private double parseDouble4(final String line, final Unit unit) {
            return parseDouble(line, unit, initialSpaces + 57);
        }

        /** Parse field 4 of a message line.
         * @param line line to parse
         * @return parsed field
         * @since 14.0
         */
        private int parseInt4(final String line) {
            return parseInt(line, initialSpaces + 57);
        }

        /** Parse field n of a message line.
         * @param line line to parse
         * @param unit unit to apply
         * @param index index of first field character
         * @return parsed field
         */
        private double parseDouble(final String line, final Unit unit, final int index) {
            return unit.toSI(ParsingUtils.parseDouble(line, index, 19));
        }

        /** Parse field n of a message line.
         * @param line line to parse
         * @param index index of first field character
         * @return parsed field
         */
        private int parseInt(final String line, final int index) {
            return (int) FastMath.rint(ParsingUtils.parseDouble(line, index, 19));
        }

        /** Ensure navigation message has been closed.
         */
        void closePendingMessage() {
            if (systemLineParser != null) {
                systemLineParser.closeMessage(this);
                systemLineParser = null;
            }
        }

    }

    /** Parsers for specific lines. */
    private enum LineParser {

        /** Parser for version, file type and satellite system. */
        HEADER_VERSION((header, line) -> header.matchFound(CommonLabel.VERSION, line),
                       (line, pi) -> {
                           pi.file.getHeader().parseVersionFileTypeSatelliteSystem(line, SatelliteSystem.GPS,
                                                                                   pi.name,
                                                                                   2.0, 2.01, 2.10, 2.11,
                                                                                   3.01, 3.02, 3.03, 3.04, 3.05,
                                                                                   4.00, 4.01, 4.02);
                           pi.initialSpaces = pi.file.getHeader().getFormatVersion() < 3.0 ? 3 : 4;
                       },
                       LineParser::headerNext),

        /** Parser for generating program and emitting agency. */
        HEADER_PROGRAM((header, line) -> header.matchFound(CommonLabel.PROGRAM, line),
                       (line, pi) -> pi.file.getHeader().parseProgramRunByDate(line, pi.timeScales),
                       LineParser::headerNext),

        /** Parser for comments. */
        HEADER_COMMENT((header, line) -> header.matchFound(CommonLabel.COMMENT, line),
                       (line, pi) -> ParsingUtils.parseComment(pi.lineNumber, line, pi.file),
                       LineParser::headerNext),

        /** Parser for ionospheric correction parameters. */
        HEADER_ION_ALPHA((header, line) -> header.matchFound(NavigationLabel.ION_ALPHA, line),
                         (line, pi) -> {

                             pi.file.getHeader().setIonosphericCorrectionType(IonosphericCorrectionType.GPS);

                             // Read coefficients
                             final double[] parameters = new double[4];
                             parameters[0] = ParsingUtils.parseDouble(line, 2, 12);
                             parameters[1] = ParsingUtils.parseDouble(line, 14, 12);
                             parameters[2] = ParsingUtils.parseDouble(line, 26, 12);
                             parameters[3] = ParsingUtils.parseDouble(line, 38, 12);
                             pi.file.setKlobucharAlpha(parameters);
                             pi.isIonosphereAlphaInitialized = true;

                         },
                         LineParser::headerNext),

        /** Parser for ionospheric correction parameters. */
        HEADER_ION_BETA((header, line) -> header.matchFound(NavigationLabel.ION_BETA, line),
                        (line, pi) -> {

                            pi.file.getHeader().setIonosphericCorrectionType(IonosphericCorrectionType.GPS);

                            // Read coefficients
                            final double[] parameters = new double[4];
                            parameters[0] = ParsingUtils.parseDouble(line, 2, 12);
                            parameters[1] = ParsingUtils.parseDouble(line, 14, 12);
                            parameters[2] = ParsingUtils.parseDouble(line, 26, 12);
                            parameters[3] = ParsingUtils.parseDouble(line, 38, 12);
                            pi.file.setKlobucharBeta(parameters);

                        },
                        LineParser::headerNext),

        /** Parser for ionospheric correction parameters. */
        HEADER_IONOSPHERIC((header, line) -> header.matchFound(NavigationLabel.IONOSPHERIC_CORR, line),
                           (line, pi) -> {

                               // ionospheric correction type
                               final IonosphericCorrectionType ionoType =
                                               IonosphericCorrectionType.valueOf(ParsingUtils.parseString(line, 0, 3));
                               pi.file.getHeader().setIonosphericCorrectionType(ionoType);

                               // Read coefficients
                               final double[] parameters = new double[4];
                               parameters[0] = ParsingUtils.parseDouble(line, 5, 12);
                               parameters[1] = ParsingUtils.parseDouble(line, 17, 12);
                               parameters[2] = ParsingUtils.parseDouble(line, 29, 12);
                               parameters[3] = ParsingUtils.parseDouble(line, 41, 12);

                               // Verify if we are parsing Galileo ionospheric parameters
                               if (ionoType == IonosphericCorrectionType.GAL) {

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
        HEADER_DELTA_UTC((header, line) -> header.matchFound(NavigationLabel.DELTA_UTC, line),
                         (line, pi) -> {
                             // Read fields
                             final double a0      = ParsingUtils.parseDouble(line, 3, 19);
                             final double a1      = ParsingUtils.parseDouble(line, 22, 19);
                             final int    refTime = ParsingUtils.parseInt(line, 41, 9);
                             final int    refWeek = ParsingUtils.parseInt(line, 50, 9);

                             // convert date
                             final SatelliteSystem satSystem = pi.file.getHeader().getSatelliteSystem();
                             final AbsoluteDate    date      = new GNSSDate(refWeek, refTime, satSystem, pi.timeScales).getDate();

                             // Add to the list
                             final TimeSystemCorrection tsc = new TimeSystemCorrection("GPUT", date, a0, a1);
                             pi.file.getHeader().addTimeSystemCorrections(tsc);
                         },
                         LineParser::headerNext),

        /** Parser for corrections to transform the GLONASS system time to UTC or to other time systems. */
        HEADER_CORR_SYSTEM_TIME((header, line) -> header.matchFound(NavigationLabel.CORR_TO_SYSTEM_TIME, line),
                         (line, pi) -> {
                             // Read fields
                             final int year        = ParsingUtils.parseInt(line, 0, 6);
                             final int month       = ParsingUtils.parseInt(line, 6, 6);
                             final int day         = ParsingUtils.parseInt(line, 12, 6);
                             final double minusTau = ParsingUtils.parseDouble(line, 21, 19);

                             // convert date
                             final SatelliteSystem satSystem = pi.file.getHeader().getSatelliteSystem();
                             final TimeScale       timeScale = satSystem.getObservationTimeScale().getTimeScale(pi.timeScales);
                             final AbsoluteDate    date      = new AbsoluteDate(year, month, day, timeScale);

                             // Add to the list
                             final TimeSystemCorrection tsc = new TimeSystemCorrection("GLUT", date, minusTau, 0.0);
                             pi.file.getHeader().addTimeSystemCorrections(tsc);

                         },
                         LineParser::headerNext),

        /** Parser for corrections to transform the system time to UTC or to other time systems. */
        HEADER_TIME((header, line) -> header.matchFound(NavigationLabel.TIME_SYSTEM_CORR, line),
                    (line, pi) -> {

                        // Read fields
                        final String type    = ParsingUtils.parseString(line, 0, 4);
                        final double a0      = ParsingUtils.parseDouble(line, 5, 17);
                        final double a1      = ParsingUtils.parseDouble(line, 22, 16);
                        final int    refTime = ParsingUtils.parseInt(line, 38, 7);
                        final int    refWeek = ParsingUtils.parseInt(line, 46, 5);

                        // convert date
                        final SatelliteSystem satSystem = pi.file.getHeader().getSatelliteSystem();
                        final AbsoluteDate    date;
                        if (satSystem == SatelliteSystem.GLONASS) {
                            date = null;
                        } else if (satSystem == SatelliteSystem.BEIDOU) {
                            date = new GNSSDate(refWeek, refTime, satSystem, pi.timeScales).getDate();
                        } else {
                            // all other systems are converted to GPS week in Rinex files!
                            date = new GNSSDate(refWeek, refTime, SatelliteSystem.GPS, pi.timeScales).getDate();
                        }

                        // Add to the list
                        final TimeSystemCorrection tsc = new TimeSystemCorrection(type, date, a0, a1);
                        pi.file.getHeader().addTimeSystemCorrections(tsc);

                    },
                    LineParser::headerNext),

        /** Parser for leap seconds. */
        HEADER_LEAP_SECONDS((header, line) -> header.matchFound(CommonLabel.LEAP_SECONDS, line),
                            (line, pi) -> {
                                pi.file.getHeader().setLeapSecondsGNSS(ParsingUtils.parseInt(line, 0, 6));
                                pi.file.getHeader().setLeapSecondsFuture(ParsingUtils.parseInt(line, 6, 6));
                                pi.file.getHeader().setLeapSecondsWeekNum(ParsingUtils.parseInt(line, 12, 6));
                                pi.file.getHeader().setLeapSecondsDayNum(ParsingUtils.parseInt(line, 18, 6));
                            },
                            LineParser::headerNext),

        /** Parser for DOI.
         * @since 12.0
         */
        HEADER_DOI((header, line) -> header.matchFound(CommonLabel.DOI, line),
                   (line, pi) -> pi.file.getHeader().
                       setDoi(ParsingUtils.parseString(line, 0, pi.file.getHeader().getLabelIndex())),
                   LineParser::headerNext),

        /** Parser for license.
         * @since 12.0
         */
        HEADER_LICENSE((header, line) -> header.matchFound(CommonLabel.LICENSE, line),
                       (line, pi) -> pi.file.getHeader().
                           setLicense(ParsingUtils.parseString(line, 0, pi.file.getHeader().getLabelIndex())),
                       LineParser::headerNext),

        /** Parser for stationInformation.
         * @since 12.0
         */
        HEADER_STATION_INFORMATION((header, line) -> header.matchFound(CommonLabel.STATION_INFORMATION, line),
                                   (line, pi) -> pi.file.getHeader().
                                       setStationInformation(ParsingUtils.parseString(line, 0, pi.file.getHeader().getLabelIndex())),
                                   LineParser::headerNext),

        /** Parser for merged files.
         * @since 12.0
         */
        HEADER_MERGED_FILE((header, line) -> header.matchFound(NavigationLabel.MERGED_FILE, line),
                           (line, pi) -> pi.file.getHeader().setMergedFiles(ParsingUtils.parseInt(line, 0, 9)),
                           LineParser::headerNext),

       /** Parser for the end of header. */
        HEADER_END((header, line) -> header.matchFound(CommonLabel.END, line),
                   (line, pi) -> {
                       // get rinex format version
                       final RinexNavigationHeader header = pi.file.getHeader();
                       final double version = header.getFormatVersion();

                       // check mandatory header fields
                       if (header.getRunByName() == null ||
                           version >= 4 && header.getLeapSecondsGNSS() < 0) {
                           throw new OrekitException(OrekitMessages.INCOMPLETE_HEADER, pi.name);
                       }

                       pi.headerParsed = true;

                   },
                   LineParser::navigationNext),

        /** Parser for navigation message space vehicle epoch and clock. */
        NAVIGATION_SV_EPOCH_CLOCK_RINEX_2((header, line) -> true,
                                          (line, pi) -> {

                                              // Set the line number to 0
                                              pi.messageLineNumber = 0;

                                              // Initialize parser
                                              pi.closePendingMessage();
                                              pi.systemLineParser = SatelliteSystemLineParser.getParser(pi.file.getHeader().getSatelliteSystem(),
                                                                                                        null, pi, line);

                                              pi.systemLineParser.parseSvEpochSvClockLine(line, pi);

                                          },
                                          LineParser::navigationNext),

        /** Parser for navigation message space vehicle epoch and clock. */
        NAVIGATION_SV_EPOCH_CLOCK((header, line) -> INITIALS.indexOf(line.charAt(0)) >= 0,
                                  (line, pi) -> {

                                      // Set the line number to 0
                                      pi.messageLineNumber = 0;

                                      if (pi.file.getHeader().getFormatVersion() < 4) {
                                          // Current satellite system
                                          final SatelliteSystem system = SatelliteSystem.parseSatelliteSystem(
                                              ParsingUtils.parseString(line, 0, 1));

                                          // Initialize parser
                                          pi.closePendingMessage();
                                          pi.systemLineParser = SatelliteSystemLineParser.getParser(system, null, pi, line);
                                      }

                                      // Read first line
                                      pi.systemLineParser.parseSvEpochSvClockLine(line, pi);

                                  },
                                  LineParser::navigationNext),

        /** Parser for navigation message type. */
        EPH_TYPE((header, line) -> MessageType.EPH.matches(line),
                 (line, pi) -> {
                     final SatelliteSystem system =
                         SatelliteSystem.parseSatelliteSystem(ParsingUtils.parseString(line, 6, 1));
                     final String          type   = ParsingUtils.parseString(line, 10, 4);
                     pi.closePendingMessage();
                     pi.systemLineParser = SatelliteSystemLineParser.getParser(system, type, pi, line);
                 },
                 pi -> Collections.singleton(NAVIGATION_SV_EPOCH_CLOCK)),

        /** Parser for broadcast orbit. */
        BROADCAST_ORBIT((header, line) -> MessageType.ORBIT.matches(line),
                        (line, pi) -> {
                            switch (++pi.messageLineNumber) {
                                case 1: pi.systemLineParser.parseFirstBroadcastOrbit(line, pi);
                                break;
                                case 2: pi.systemLineParser.parseSecondBroadcastOrbit(line, pi);
                                break;
                                case 3: pi.systemLineParser.parseThirdBroadcastOrbit(line, pi);
                                break;
                                case 4: pi.systemLineParser.parseFourthBroadcastOrbit(line, pi);
                                break;
                                case 5: pi.systemLineParser.parseFifthBroadcastOrbit(line, pi);
                                break;
                                case 6: pi.systemLineParser.parseSixthBroadcastOrbit(line, pi);
                                break;
                                case 7: pi.systemLineParser.parseSeventhBroadcastOrbit(line, pi);
                                break;
                                case 8: pi.systemLineParser.parseEighthBroadcastOrbit(line, pi);
                                break;
                                case 9: pi.systemLineParser.parseNinthBroadcastOrbit(line, pi);
                                break;
                                default:
                                    // this should never happen
                                    throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                              pi.lineNumber, pi.name, line);
                            }

                        },
                        LineParser::navigationNext),

        /** Parser for system time offset message model. */
        STO_LINE_1((header, line) -> true,
                   (line, pi) -> {
                       pi.sto.setTransmissionTime(pi.parseDouble1(line, Unit.SECOND));
                       pi.sto.setA0(pi.parseDouble2(line, Unit.SECOND));
                       pi.sto.setA1(pi.parseDouble3(line, S_PER_S));
                       pi.sto.setA2(pi.parseDouble4(line, S_PER_S2));
                       pi.file.addSystemTimeOffset(pi.sto);
                       pi.sto = null;
                   },
                   LineParser::navigationNext),

        /** Parser for system time offset message space vehicle epoch and clock. */
        STO_SV_EPOCH_CLOCK((header, line) -> true,
                           (line, pi) -> {

                               pi.sto.setDefinedTimeSystem(PredefinedTimeSystem.
                                                           parseTwoLettersCode(ParsingUtils.parseString(line, 24, 2)));
                               pi.sto.setReferenceTimeSystem(PredefinedTimeSystem.
                                                             parseTwoLettersCode(ParsingUtils.parseString(line, 26, 2)));
                               final String sbas = ParsingUtils.parseString(line, 43, 18);
                               pi.sto.setSbasId(sbas == null || sbas.isEmpty() ? null : SbasId.valueOf(sbas));
                               final String utc = ParsingUtils.parseString(line, 62, 18);
                               pi.sto.setUtcId(utc == null || utc.isEmpty() ? null : UtcId.parseUtcId(utc));
                               pi.sto.setReferenceEpoch(pi.parseDate(line, pi.sto.getSystem()));

                           },
                           pi -> Collections.singleton(STO_LINE_1)),

        /** Parser for system time offset message type. */
        STO_TYPE((header, line) -> MessageType.STO.matches(line),
                 (line, pi) -> {
                     pi.closePendingMessage();
                     pi.sto = new SystemTimeOffsetMessage(SatelliteSystem.
                                                          parseSatelliteSystem(ParsingUtils.parseString(line, 6, 1)),
                                                          ParsingUtils.parseInt(line, 7, 2),
                                                          ParsingUtils.parseString(line, 10, 4),
                                                          ParsingUtils.parseString(line, 15, 4));
                 },
                 pi -> Collections.singleton(STO_SV_EPOCH_CLOCK)),

        /** Parser for Earth orientation parameter message model. */
        EOP_LINE_2((header, line) -> true,
                   (line, pi) -> {
                       pi.eop.setTransmissionTime(pi.parseDouble1(line, Unit.SECOND));
                       pi.eop.setDut1(pi.parseDouble2(line, Unit.SECOND));
                       pi.eop.setDut1Dot(pi.parseDouble3(line, S_PER_DAY));
                       pi.eop.setDut1DotDot(pi.parseDouble4(line, S_PER_DAY2));
                       pi.file.addEarthOrientationParameter(pi.eop);
                       pi.eop = null;
                   },
                   LineParser::navigationNext),

        /** Parser for Earth orientation parameter message model. */
        EOP_LINE_1((header, line) -> true,
                   (line, pi) -> {
                       pi.eop.setYp(pi.parseDouble2(line, Unit.ARC_SECOND));
                       pi.eop.setYpDot(pi.parseDouble3(line, AS_PER_DAY));
                       pi.eop.setYpDotDot(pi.parseDouble4(line, AS_PER_DAY2));
                   },
                   pi -> Collections.singleton(EOP_LINE_2)),

        /** Parser for Earth orientation parameter message model. */
        EOP_LINE_0((header, line) -> true,
                   (line, pi) -> {
                       pi.eop.setReferenceEpoch(pi.parseDate(line, pi.eop.getSystem()));
                       pi.eop.setXp(pi.parseDouble2(line, Unit.ARC_SECOND));
                       pi.eop.setXpDot(pi.parseDouble3(line, AS_PER_DAY));
                       pi.eop.setXpDotDot(pi.parseDouble4(line, AS_PER_DAY2));
                   },
                   pi -> Collections.singleton(EOP_LINE_1)),

        /** Parser for Earth orientation parameter message type. */
        EOP_TYPE((header, line) -> MessageType.EOP.matches(line),
                 (line, pi) -> {
                     pi.closePendingMessage();
                     pi.eop =
                         new EarthOrientationParameterMessage(SatelliteSystem.
                                                              parseSatelliteSystem(ParsingUtils.parseString(line, 6, 1)),
                                                              ParsingUtils.parseInt(line, 7, 2),
                                                              ParsingUtils.parseString(line, 10, 4),
                                                              ParsingUtils.parseString(line, 15, 4));
                 },
                 pi -> Collections.singleton(EOP_LINE_0)),

        /** Parser for ionosphere Klobuchar message model. */
        KLOBUCHAR_LINE_2((header, line) -> true,
                         (line, pi) -> {
                             pi.klobuchar.setBetaI(3, pi.parseDouble1(line, IonosphereBaseMessage.S_PER_SC_N3));
                             pi.klobuchar.setRegionCode(pi.parseDouble2(line, Unit.ONE) < 0.5 ?
                                                        RegionCode.WIDE_AREA : RegionCode.JAPAN);
                             pi.file.addKlobucharMessage(pi.klobuchar);
                             pi.klobuchar = null;
                         },
                         LineParser::navigationNext),

        /** Parser for ionosphere Klobuchar message model. */
        KLOBUCHAR_LINE_1((header, line) -> true,
                         (line, pi) -> {
                             pi.klobuchar.setAlphaI(3, pi.parseDouble1(line, IonosphereBaseMessage.S_PER_SC_N3));
                             pi.klobuchar.setBetaI(0, pi.parseDouble2(line, IonosphereBaseMessage.S_PER_SC_N0));
                             pi.klobuchar.setBetaI(1, pi.parseDouble3(line, IonosphereBaseMessage.S_PER_SC_N1));
                             pi.klobuchar.setBetaI(2, pi.parseDouble4(line, IonosphereBaseMessage.S_PER_SC_N2));
                         },
                         pi -> Collections.singleton(KLOBUCHAR_LINE_2)),

        /** Parser for ionosphere Klobuchar message model. */
        KLOBUCHAR_LINE_0((header, line) -> true,
                         (line, pi) -> {
                             pi.klobuchar.setTransmitTime(pi.parseDate(line, pi.klobuchar.getSystem()));
                             pi.klobuchar.setAlphaI(0, pi.parseDouble2(line, IonosphereBaseMessage.S_PER_SC_N0));
                             pi.klobuchar.setAlphaI(1, pi.parseDouble3(line, IonosphereBaseMessage.S_PER_SC_N1));
                             pi.klobuchar.setAlphaI(2, pi.parseDouble4(line, IonosphereBaseMessage.S_PER_SC_N2));
                         },
                         pi -> Collections.singleton(KLOBUCHAR_LINE_1)),

        /** Parser for NacIV Klobuchar message model.
         * @since 14.0
         */
        NAVIC_KLOBUCHAR_LINE_3((header, line) -> true,
                               (line, pi) -> {
                                   pi.navICKlobuchar.setLonMin(pi.parseDouble1(line, Unit.DEGREE));
                                   pi.navICKlobuchar.setLonMax(pi.parseDouble2(line, Unit.DEGREE));
                                   pi.navICKlobuchar.setModipMin(pi.parseDouble3(line, Unit.DEGREE));
                                   pi.navICKlobuchar.setModipMax(pi.parseDouble4(line, Unit.DEGREE));
                                   pi.file.addNavICKlobucharMessage(pi.navICKlobuchar);
                                   pi.navICKlobuchar = null;
                               },
                               LineParser::navigationNext),

        /** Parser for NavIC Klobuchar message model.
         * @since 14.0
         */
        NAVIC_KLOBUCHAR_LINE_2((header, line) -> true,
                               (line, pi) -> {
                                   pi.navICKlobuchar.setBetaI(0, pi.parseDouble1(line, IonosphereBaseMessage.S_PER_SC_N0));
                                   pi.navICKlobuchar.setBetaI(1, pi.parseDouble2(line, IonosphereBaseMessage.S_PER_SC_N1));
                                   pi.navICKlobuchar.setBetaI(2, pi.parseDouble3(line, IonosphereBaseMessage.S_PER_SC_N2));
                                   pi.navICKlobuchar.setBetaI(3, pi.parseDouble4(line, IonosphereBaseMessage.S_PER_SC_N3));
                               },
                               pi -> Collections.singleton(NAVIC_KLOBUCHAR_LINE_3)),

        /** Parser for NavIC Klobuchar message model.
         * @since 14.0
         */
        NAVIC_KLOBUCHAR_LINE_1((header, line) -> true,
                               (line, pi) -> {
                                   pi.navICKlobuchar.setAlphaI(0, pi.parseDouble1(line, IonosphereBaseMessage.S_PER_SC_N0));
                                   pi.navICKlobuchar.setAlphaI(1, pi.parseDouble2(line, IonosphereBaseMessage.S_PER_SC_N1));
                                   pi.navICKlobuchar.setAlphaI(2, pi.parseDouble3(line, IonosphereBaseMessage.S_PER_SC_N2));
                                   pi.navICKlobuchar.setAlphaI(3, pi.parseDouble4(line, IonosphereBaseMessage.S_PER_SC_N3));
                               },
                               pi -> Collections.singleton(NAVIC_KLOBUCHAR_LINE_2)),

        /** Parser for NavIC Klobuchar message model. */
        NAVIC_KLOBUCHAR_LINE_0((header, line) -> true,
                               (line, pi) -> {
                                   pi.navICKlobuchar.setTransmitTime(pi.parseDate(line, pi.navICKlobuchar.getSystem()));
                                   pi.navICKlobuchar.setIOD(pi.parseDouble2(line, Unit.ONE));
                               },
                               pi -> Collections.singleton(NAVIC_KLOBUCHAR_LINE_1)),

        /** Parser for NacIV NeQuick N message model.
         * @since 14.0
         */
        NAVIC_NEQUICK_N_LINE_6((header, line) -> true,
                               (line, pi) -> {
                                   pi.navICNeQuickN.getRegion3().setLonMin(pi.parseDouble1(line, Unit.DEGREE));
                                   pi.navICNeQuickN.getRegion3().setLonMax(pi.parseDouble2(line, Unit.DEGREE));
                                   pi.navICNeQuickN.getRegion3().setModipMin(pi.parseDouble3(line, Unit.DEGREE));
                                   pi.navICNeQuickN.getRegion3().setModipMax(pi.parseDouble4(line, Unit.DEGREE));
                                   pi.file.addNavICNeQuickNMessage(pi.navICNeQuickN);
                                   pi.navICNeQuickN = null;
                               },
                               LineParser::navigationNext),

        /** Parser for NacIV NeQuick N message model.
         * @since 14.0
         */
        NAVIC_NEQUICK_N_LINE_5((header, line) -> true,
                               (line, pi) -> {
                                   pi.navICNeQuickN.getRegion3().setAi0(pi.parseDouble1(line, IonosphereAij.SFU));
                                   pi.navICNeQuickN.getRegion3().setAi1(pi.parseDouble2(line, IonosphereAij.SFU_PER_DEG));
                                   pi.navICNeQuickN.getRegion3().setAi2(pi.parseDouble3(line, IonosphereAij.SFU_PER_DEG2));
                                   pi.navICNeQuickN.getRegion3().setIDF(pi.parseDouble4(line, Unit.ONE));
                               },
                               pi -> Collections.singleton(NAVIC_NEQUICK_N_LINE_6)),

        /** Parser for NacIV NeQuick N message model.
         * @since 14.0
         */
        NAVIC_NEQUICK_N_LINE_4((header, line) -> true,
                               (line, pi) -> {
                                   pi.navICNeQuickN.getRegion2().setLonMin(pi.parseDouble1(line, Unit.DEGREE));
                                   pi.navICNeQuickN.getRegion2().setLonMax(pi.parseDouble2(line, Unit.DEGREE));
                                   pi.navICNeQuickN.getRegion2().setModipMin(pi.parseDouble3(line, Unit.DEGREE));
                                   pi.navICNeQuickN.getRegion2().setModipMax(pi.parseDouble4(line, Unit.DEGREE));
                               },
                               pi -> Collections.singleton(NAVIC_NEQUICK_N_LINE_5)),

        /** Parser for NacIV NeQuick N message model.
         * @since 14.0
         */
        NAVIC_NEQUICK_N_LINE_3((header, line) -> true,
                               (line, pi) -> {
                                   pi.navICNeQuickN.getRegion2().setAi0(pi.parseDouble1(line, IonosphereAij.SFU));
                                   pi.navICNeQuickN.getRegion2().setAi1(pi.parseDouble2(line, IonosphereAij.SFU_PER_DEG));
                                   pi.navICNeQuickN.getRegion2().setAi2(pi.parseDouble3(line, IonosphereAij.SFU_PER_DEG2));
                                   pi.navICNeQuickN.getRegion2().setIDF(pi.parseDouble4(line, Unit.ONE));
                               },
                               pi -> Collections.singleton(NAVIC_NEQUICK_N_LINE_4)),

        /** Parser for NacIV NeQuick N message model.
         * @since 14.0
         */
        NAVIC_NEQUICK_N_LINE_2((header, line) -> true,
                               (line, pi) -> {
                                   pi.navICNeQuickN.getRegion1().setLonMin(pi.parseDouble1(line, Unit.DEGREE));
                                   pi.navICNeQuickN.getRegion1().setLonMax(pi.parseDouble2(line, Unit.DEGREE));
                                   pi.navICNeQuickN.getRegion1().setModipMin(pi.parseDouble3(line, Unit.DEGREE));
                                   pi.navICNeQuickN.getRegion1().setModipMax(pi.parseDouble4(line, Unit.DEGREE));
                               },
                               pi -> Collections.singleton(NAVIC_NEQUICK_N_LINE_3)),

        /** Parser for NacIV NeQuick N message model.
         * @since 14.0
         */
        NAVIC_NEQUICK_N_LINE_1((header, line) -> true,
                               (line, pi) -> {
                                   pi.navICNeQuickN.getRegion1().setAi0(pi.parseDouble1(line, IonosphereAij.SFU));
                                   pi.navICNeQuickN.getRegion1().setAi1(pi.parseDouble2(line, IonosphereAij.SFU_PER_DEG));
                                   pi.navICNeQuickN.getRegion1().setAi2(pi.parseDouble3(line, IonosphereAij.SFU_PER_DEG2));
                                   pi.navICNeQuickN.getRegion1().setIDF(pi.parseDouble4(line, Unit.ONE));
                               },
                               pi -> Collections.singleton(NAVIC_NEQUICK_N_LINE_2)),

        /** Parser for NacIV NeQuick N message model.
         * @since 14.0
         */
        NAVIC_NEQUICK_N_LINE_0((header, line) -> true,
                               (line, pi) -> {
                                   pi.navICNeQuickN.setTransmitTime(pi.parseDate(line, pi.navICNeQuickN.getSystem()));
                                   pi.navICNeQuickN.setIOD(pi.parseDouble2(line, Unit.ONE));
                               },
                               pi -> Collections.singleton(NAVIC_NEQUICK_N_LINE_1)),

        /** Parser for GLONASS CDMS message model.
         * @since 14.0
         */
        GLONASS_CDMS_LINE_0((header, line) -> true,
                            (line, pi) -> {
                                pi.glonassCdms.setTransmitTime(pi.parseDate(line, pi.glonassCdms.getSystem()));
                                pi.glonassCdms.setCA(pi.parseDouble2(line, Unit.ONE));
                                pi.glonassCdms.setCF107(pi.parseDouble3(line, Unit.ONE));
                                pi.glonassCdms.setCAP(pi.parseDouble4(line, Unit.ONE));
                                pi.file.addGlonassCDMSMessage(pi.glonassCdms);
                                pi.glonassCdms = null;
                            },
                            LineParser::navigationNext),

        /** Parser for ionosphere Nequick-G message model. */
        NEQUICK_G_LINE_1((header, line) -> true,
                         (line, pi) -> {
                             pi.nequickG.setFlags((int) FastMath.rint(ParsingUtils.parseDouble(line, 4, 19)));
                             pi.file.addNequickGMessage(pi.nequickG);
                             pi.nequickG = null;
                         },
                         LineParser::navigationNext),

        /** Parser for ionosphere Nequick-G message model. */
        NEQUICK_G_LINE_0((header, line) -> true,
                         (line, pi) -> {
                             pi.nequickG.setTransmitTime(pi.parseDate(line, pi.nequickG.getSystem()));
                             pi.nequickG.getAij().setAi0(pi.parseDouble2(line, IonosphereAij.SFU));
                             pi.nequickG.getAij().setAi1(pi.parseDouble3(line, IonosphereAij.SFU_PER_DEG));
                             pi.nequickG.getAij().setAi2(pi.parseDouble4(line, IonosphereAij.SFU_PER_DEG2));
                         },
                         pi -> Collections.singleton(NEQUICK_G_LINE_1)),

        /** Parser for ionosphere BDGIM message model. */
        BDGIM_LINE_2((header, line) -> true,
                     (line, pi) -> {
                         pi.bdgim.setAlphaI(7, pi.parseDouble1(line, TEC));
                         pi.bdgim.setAlphaI(8, pi.parseDouble2(line, TEC));
                         pi.file.addBDGIMMessage(pi.bdgim);
                         pi.bdgim = null;
                     },
                     LineParser::navigationNext),

        /** Parser for ionosphere BDGIM message model. */
        BDGIM_LINE_1((header, line) -> true,
                     (line, pi) -> {
                         pi.bdgim.setAlphaI(3, pi.parseDouble1(line, TEC));
                         pi.bdgim.setAlphaI(4, pi.parseDouble2(line, TEC));
                         pi.bdgim.setAlphaI(5, pi.parseDouble3(line, TEC));
                         pi.bdgim.setAlphaI(6, pi.parseDouble4(line, TEC));
                     },
                     pi -> Collections.singleton(BDGIM_LINE_2)),

        /** Parser for ionosphere BDGIM message model. */
        BDGIM_LINE_0((header, line) -> true,
                     (line, pi) -> {
                         pi.bdgim.setTransmitTime(pi.parseDate(line, pi.bdgim.getSystem()));
                         pi.bdgim.setAlphaI(0, pi.parseDouble2(line, TEC));
                         pi.bdgim.setAlphaI(1, pi.parseDouble3(line, TEC));
                         pi.bdgim.setAlphaI(2, pi.parseDouble4(line, TEC));
                     },
                     pi -> Collections.singleton(BDGIM_LINE_1)),

        /** Parser for ionosphere message type. */
        IONO_TYPE((header, line) -> line.startsWith("> ION"),
                  (line, pi) -> {
                      pi.closePendingMessage();
                      final SatelliteSystem system = SatelliteSystem.parseSatelliteSystem(
                          ParsingUtils.parseString(line, 6, 1));
                      final int             prn     = ParsingUtils.parseInt(line, 7, 2);
                      final String          type    = ParsingUtils.parseString(line, 10, 4);
                      final String          subtype = ParsingUtils.parseString(line, 15, 4);
                      if (system == SatelliteSystem.GALILEO) {
                          pi.nequickG = new IonosphereNequickGMessage(system, prn, type, subtype);
                      } else if (system == SatelliteSystem.BEIDOU && "CNVX".equals(type)) {
                          // in Rinex 4.00, tables A32 and A34 (A35 and A37 in Rinex 4.02) are ambiguous
                          // as both seem to apply to Beidou CNVX messages; we consider BDGIM is the
                          // proper model in this case
                          pi.bdgim = new IonosphereBDGIMMessage(system, prn, type, subtype);
                      } else if (system == SatelliteSystem.NAVIC &&
                                 NavICL1NVNavigationMessage.L1NV.equals(type) &&
                                 "KLOB".equals(subtype)) {
                          pi.navICKlobuchar = new IonosphereNavICKlobucharMessage(system, prn, type, subtype);
                      } else if (system == SatelliteSystem.NAVIC &&
                                 NavICL1NVNavigationMessage.L1NV.equals(type) &&
                                 "NEQN".equals(subtype)) {
                          pi.navICNeQuickN = new IonosphereNavICNeQuickNMessage(system, prn, type, subtype);
                      } else if (system == SatelliteSystem.GLONASS) {
                          pi.glonassCdms = new IonosphereGlonassCdmsMessage(system, prn, type, subtype);
                      } else  {
                          pi.klobuchar = new IonosphereKlobucharMessage(system, prn, type, subtype);
                      }
                  },
                  LineParser::ionosphereNext);

        /** Predicate for identifying lines that can be parsed. */
        private final BiFunction<RinexNavigationHeader, String, Boolean> canHandle;

        /** Parsing method. */
        private final ParsingMethod parsingMethod;

        /** Provider for next line parsers. */
        private final Function<ParseInfo, Iterable<LineParser>> allowedNextProvider;

        /** Simple constructor.
         * @param canHandle predicate for identifying lines that can be parsed
         * @param parsingMethod parsing method
         * @param allowedNextProvider supplier for allowed parsers for next line
         */
        LineParser(final BiFunction<RinexNavigationHeader, String, Boolean> canHandle,
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
            if (parseInfo.file.getHeader().getFormatVersion() < 3) {
                // Rinex 2.x header entries
                return Arrays.asList(HEADER_COMMENT, HEADER_PROGRAM,
                                     HEADER_ION_ALPHA, HEADER_ION_BETA,
                                     HEADER_DELTA_UTC, HEADER_CORR_SYSTEM_TIME,
                                     HEADER_LEAP_SECONDS, HEADER_END);
            } else if (parseInfo.file.getHeader().getFormatVersion() < 4) {
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
            if (parseInfo.gpsLNav    != null || parseInfo.gpsCNav    != null || parseInfo.galileoNav != null ||
                parseInfo.beidouLNav != null || parseInfo.beidouCNav != null || parseInfo.qzssLNav   != null ||
                parseInfo.qzssCNav   != null || parseInfo.navicLNav  != null || parseInfo.navicL1NV  != null ||
                parseInfo.sbasNav    != null) {
                return Collections.singleton(BROADCAST_ORBIT);
            } else if (parseInfo.glonassFdmaNav != null) {
                if (parseInfo.messageLineNumber < 3) {
                    return Collections.singleton(BROADCAST_ORBIT);
                } else {
                    // workaround for some invalid files that should nevertheless be parsed
                    // we have encountered in the wild merged files that claimed to be in 3.05 version
                    // and hence needed at least 4 broadcast GLONASS orbit lines (the fourth line was
                    // introduced in 3.05), but in fact only had 3 broadcast lines. We think they were
                    // merged from files in 3.04 or earlier format. In order to parse these files,
                    // we accept after the third line either another broadcast orbit line or a new message
                    if (parseInfo.file.getHeader().getFormatVersion() < 4) {
                        return Arrays.asList(BROADCAST_ORBIT, NAVIGATION_SV_EPOCH_CLOCK);
                    } else {
                        return Arrays.asList(BROADCAST_ORBIT, EPH_TYPE, STO_TYPE, EOP_TYPE, IONO_TYPE);
                    }
                }
            } else if (parseInfo.file.getHeader().getFormatVersion() < 3) {
                return Collections.singleton(NAVIGATION_SV_EPOCH_CLOCK_RINEX_2);
            } else if (parseInfo.file.getHeader().getFormatVersion() < 4) {
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
            if (parseInfo.nequickG != null) {
                return Collections.singleton(NEQUICK_G_LINE_0);
            } else if (parseInfo.bdgim != null) {
                return Collections.singleton(BDGIM_LINE_0);
            } else if (parseInfo.klobuchar != null) {
                return Collections.singleton(KLOBUCHAR_LINE_0);
            } else if (parseInfo.navICKlobuchar != null) {
                return Collections.singleton(NAVIC_KLOBUCHAR_LINE_0);
            } else if (parseInfo.navICNeQuickN != null) {
                return Collections.singleton(NAVIC_NEQUICK_N_LINE_0);
            } else if (parseInfo.glonassCdms != null) {
                return Collections.singleton(GLONASS_CDMS_LINE_0);
            } else {
                return Collections.emptyList();
            }
        }

    }

    /** Parsers for satellite system specific lines. */
    private enum SatelliteSystemLineParser {

        /** GPS legacy. */
        GPS_LNAV() {

            /** {@inheritDoc} */
            @Override
            public void parseSvEpochSvClockLine(final String line, final ParseInfo pi) {
                if (pi.file.getHeader().getFormatVersion() < 3.0) {
                    parseSvEpochSvClockLineRinex2(line, pi.timeScales.getGPS(), pi.gpsLNav);
                } else {
                    parseSvEpochSvClockLine(line, pi.timeScales.getGPS(), pi, pi.gpsLNav);
                }
            }

            /** {@inheritDoc} */
            @Override
            public void parseFirstBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.gpsLNav.setIODE(pi.parseDouble1(line, Unit.SECOND));
                pi.gpsLNav.setCrs(pi.parseDouble2(line, Unit.METRE));
                pi.gpsLNav.setDeltaN0(pi.parseDouble3(line, RAD_PER_S));
                pi.gpsLNav.setM0(pi.parseDouble4(line, Unit.RADIAN));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSecondBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.gpsLNav.setCuc(pi.parseDouble1(line, Unit.RADIAN));
                pi.gpsLNav.setE(pi.parseDouble2(line, Unit.NONE));
                pi.gpsLNav.setCus(pi.parseDouble3(line, Unit.RADIAN));
                pi.gpsLNav.setSqrtA(pi.parseDouble4(line, SQRT_M));
            }

            /** {@inheritDoc} */
            @Override
            public void parseThirdBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.gpsLNav.setTime(pi.parseDouble1(line, Unit.SECOND));
                pi.gpsLNav.setCic(pi.parseDouble2(line, Unit.RADIAN));
                pi.gpsLNav.setOmega0(pi.parseDouble3(line, Unit.RADIAN));
                pi.gpsLNav.setCis(pi.parseDouble4(line, Unit.RADIAN));
            }

            /** {@inheritDoc} */
            @Override
            public void parseFourthBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.gpsLNav.setI0(pi.parseDouble1(line, Unit.RADIAN));
                pi.gpsLNav.setCrc(pi.parseDouble2(line, Unit.METRE));
                pi.gpsLNav.setPa(pi.parseDouble3(line, Unit.RADIAN));
                pi.gpsLNav.setOmegaDot(pi.parseDouble4(line, RAD_PER_S));
            }

            /** {@inheritDoc} */
            @Override
            public void parseFifthBroadcastOrbit(final String line, final ParseInfo pi) {
                // iDot
                pi.gpsLNav.setIDot(pi.parseDouble1(line, RAD_PER_S));
                pi.gpsLNav.setL2Codes(pi.parseInt2(line));
                pi.gpsLNav.setWeek(pi.parseInt3(line));
                pi.gpsLNav.setL2PFlags(pi.parseInt4(line));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSixthBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.gpsLNav.setSvAccuracy(pi.parseDouble1(line, Unit.METRE));
                pi.gpsLNav.setSvHealth(pi.parseInt2(line));
                pi.gpsLNav.setTGD(pi.parseDouble3(line, Unit.SECOND));
                pi.gpsLNav.setIODC(pi.parseInt4(line));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSeventhBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.gpsLNav.setTransmissionTime(pi.parseDouble1(line, Unit.SECOND));
                pi.gpsLNav.setFitInterval(pi.parseInt2(line));
                pi.closePendingMessage();
            }

            /** {@inheritDoc} */
            @Override
            public void closeMessage(final ParseInfo pi) {
                pi.file.addGPSLegacyNavigationMessage(pi.gpsLNav);
                pi.gpsLNav = null;
            }

        },

        /** GPS civilian.
         * @since 12.0
         */
        GPS_CNAV() {

            /** {@inheritDoc} */
            @Override
            public void parseSvEpochSvClockLine(final String line, final ParseInfo pi) {
                parseSvEpochSvClockLine(line, pi.timeScales.getGPS(), pi, pi.gpsCNav);
            }

            /** {@inheritDoc} */
            @Override
            public void parseFirstBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.gpsCNav.setADot(pi.parseDouble1(line, M_PER_S));
                pi.gpsCNav.setCrs(pi.parseDouble2(line, Unit.METRE));
                pi.gpsCNav.setDeltaN0(pi.parseDouble3(line, RAD_PER_S));
                pi.gpsCNav.setM0(pi.parseDouble4(line, Unit.RADIAN));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSecondBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.gpsCNav.setCuc(pi.parseDouble1(line, Unit.RADIAN));
                pi.gpsCNav.setE(pi.parseDouble2(line, Unit.NONE));
                pi.gpsCNav.setCus(pi.parseDouble3(line, Unit.RADIAN));
                pi.gpsCNav.setSqrtA(pi.parseDouble4(line, SQRT_M));
            }

            /** {@inheritDoc} */
            @Override
            public void parseThirdBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.gpsCNav.setTime(pi.parseDouble1(line, Unit.SECOND));
                pi.gpsCNav.setCic(pi.parseDouble2(line, Unit.RADIAN));
                pi.gpsCNav.setOmega0(pi.parseDouble3(line, Unit.RADIAN));
                pi.gpsCNav.setCis(pi.parseDouble4(line, Unit.RADIAN));
            }

            /** {@inheritDoc} */
            @Override
            public void parseFourthBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.gpsCNav.setI0(pi.parseDouble1(line, Unit.RADIAN));
                pi.gpsCNav.setCrc(pi.parseDouble2(line, Unit.METRE));
                pi.gpsCNav.setPa(pi.parseDouble3(line, Unit.RADIAN));
                pi.gpsCNav.setOmegaDot(pi.parseDouble4(line, RAD_PER_S));
            }

            /** {@inheritDoc} */
            @Override
            public void parseFifthBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.gpsCNav.setIDot(pi.parseDouble1(line, RAD_PER_S));
                pi.gpsCNav.setDeltaN0Dot(pi.parseDouble2(line, RAD_PER_S2));
                pi.gpsCNav.setUraiNed0(pi.parseInt3(line));
                pi.gpsCNav.setUraiNed1(pi.parseInt4(line));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSixthBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.gpsCNav.setUraiEd(pi.parseInt1(line));
                pi.gpsCNav.setSvHealth(pi.parseInt2(line));
                pi.gpsCNav.setTGD(pi.parseDouble3(line, Unit.SECOND));
                pi.gpsCNav.setUraiNed2(pi.parseInt4(line));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSeventhBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.gpsCNav.setIscL1CA(pi.parseDouble1(line, Unit.SECOND));
                pi.gpsCNav.setIscL2C(pi.parseDouble2(line, Unit.SECOND));
                pi.gpsCNav.setIscL5I5(pi.parseDouble3(line, Unit.SECOND));
                pi.gpsCNav.setIscL5Q5(pi.parseDouble4(line, Unit.SECOND));
            }

            /** {@inheritDoc} */
            @Override
            public void parseEighthBroadcastOrbit(final String line, final ParseInfo pi) {
                if (pi.gpsCNav.isCnv2()) {
                    // in CNAV2 messages, there is an additional line for L1 CD and L1 CP inter signal delay
                    pi.gpsCNav.setIscL1CD(pi.parseDouble1(line, Unit.SECOND));
                    pi.gpsCNav.setIscL1CP(pi.parseDouble2(line, Unit.SECOND));
                } else {
                    parseTransmissionTimeLine(line, pi);
                }
            }

            /** {@inheritDoc} */
            @Override
            public void parseNinthBroadcastOrbit(final String line, final ParseInfo pi) {
                parseTransmissionTimeLine(line, pi);
            }

            /** Parse transmission time line.
             * @param line line to parse
             * @param pi holder for transient data
             */
            private void parseTransmissionTimeLine(final String line, final ParseInfo pi) {
                pi.gpsCNav.setTransmissionTime(pi.parseDouble1(line, Unit.SECOND));
                pi.gpsCNav.setWeek(pi.parseInt2(line));
                pi.gpsCNav.setFlags(pi.parseInt3(line));
                pi.closePendingMessage();
            }

            /** {@inheritDoc} */
            @Override
            public void closeMessage(final ParseInfo pi) {
                pi.file.addGPSCivilianNavigationMessage(pi.gpsCNav);
                pi.gpsCNav = null;
            }

        },

        /** Galileo. */
        GALILEO() {

            /** {@inheritDoc} */
            @Override
            public void parseSvEpochSvClockLine(final String line, final ParseInfo pi) {
                parseSvEpochSvClockLine(line, pi.timeScales.getGPS(), pi, pi.galileoNav);
            }

            /** {@inheritDoc} */
            @Override
            public void parseFirstBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.galileoNav.setIODNav(pi.parseInt1(line));
                pi.galileoNav.setCrs(pi.parseDouble2(line, Unit.METRE));
                pi.galileoNav.setDeltaN0(pi.parseDouble3(line, RAD_PER_S));
                pi.galileoNav.setM0(pi.parseDouble4(line, Unit.RADIAN));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSecondBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.galileoNav.setCuc(pi.parseDouble1(line, Unit.RADIAN));
                pi.galileoNav.setE(pi.parseDouble2(line, Unit.NONE));
                pi.galileoNav.setCus(pi.parseDouble3(line, Unit.RADIAN));
                pi.galileoNav.setSqrtA(pi.parseDouble4(line, SQRT_M));
            }

            /** {@inheritDoc} */
            @Override
            public void parseThirdBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.galileoNav.setTime(pi.parseDouble1(line, Unit.SECOND));
                pi.galileoNav.setCic(pi.parseDouble2(line, Unit.RADIAN));
                pi.galileoNav.setOmega0(pi.parseDouble3(line, Unit.RADIAN));
                pi.galileoNav.setCis(pi.parseDouble4(line, Unit.RADIAN));
            }

            /** {@inheritDoc} */
            @Override
            public void parseFourthBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.galileoNav.setI0(pi.parseDouble1(line, Unit.RADIAN));
                pi.galileoNav.setCrc(pi.parseDouble2(line, Unit.METRE));
                pi.galileoNav.setPa(pi.parseDouble3(line, Unit.RADIAN));
                pi.galileoNav.setOmegaDot(pi.parseDouble4(line, RAD_PER_S));
            }

            /** {@inheritDoc} */
            @Override
            public void parseFifthBroadcastOrbit(final String line, final ParseInfo pi) {
                // iDot
                pi.galileoNav.setIDot(pi.parseDouble1(line, RAD_PER_S));
                pi.galileoNav.setDataSource(pi.parseInt2(line));
                // GAL week (to go with Toe)
                pi.galileoNav.setWeek(pi.parseInt3(line));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSixthBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.galileoNav.setSisa(pi.parseDouble1(line, Unit.METRE));
                pi.galileoNav.setSvHealth(pi.parseDouble2(line, Unit.NONE));
                pi.galileoNav.setBGDE1E5a(pi.parseDouble3(line, Unit.SECOND));
                pi.galileoNav.setBGDE5bE1(pi.parseDouble4(line, Unit.SECOND));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSeventhBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.galileoNav.setTransmissionTime(pi.parseDouble1(line, Unit.SECOND));
                pi.closePendingMessage();
            }

            /** {@inheritDoc} */
            @Override
            public void closeMessage(final ParseInfo pi) {
                pi.file.addGalileoNavigationMessage(pi.galileoNav);
                pi.galileoNav = null;
            }

        },

        /** Glonass FDMA. */
        GLONASS_FDMA() {

            /** {@inheritDoc} */
            @Override
            public void parseSvEpochSvClockLine(final String line, final ParseInfo pi) {

                if (pi.file.getHeader().getFormatVersion() < 3.0) {

                    pi.glonassFdmaNav.setPRN(ParsingUtils.parseInt(line, 0, 2));

                    // Toc
                    final int    year  = ParsingUtils.convert2DigitsYear(ParsingUtils.parseInt(line, 3, 2));
                    final int    month = ParsingUtils.parseInt(line, 6, 2);
                    final int    day   = ParsingUtils.parseInt(line, 9, 2);
                    final int    hours = ParsingUtils.parseInt(line, 12, 2);
                    final int    min   = ParsingUtils.parseInt(line, 15, 2);
                    final double sec   = ParsingUtils.parseDouble(line, 17, 5);
                    pi.glonassFdmaNav.setEpochToc(new AbsoluteDate(year, month, day, hours, min, sec,
                                                                   pi.timeScales.getUTC()));

                    // clock
                    pi.glonassFdmaNav.setTauN(Unit.SECOND.toSI(-ParsingUtils.parseDouble(line, 22, 19)));
                    pi.glonassFdmaNav.setGammaN(Unit.NONE.toSI(ParsingUtils.parseDouble(line, 41, 19)));

                    // Set the ephemeris epoch (same as time of clock epoch)
                    pi.glonassFdmaNav.setDate(pi.glonassFdmaNav.getEpochToc());

                } else {
                    pi.glonassFdmaNav.setPRN(ParsingUtils.parseInt(line, 1, 2));

                    // Toc
                    pi.glonassFdmaNav.setEpochToc(pi.parseDate(line, pi.timeScales.getUTC()));

                    // clock
                    pi.glonassFdmaNav.setTauN(-pi.parseDouble2(line, Unit.ONE));
                    pi.glonassFdmaNav.setGammaN(pi.parseDouble3(line, Unit.ONE));
                    pi.glonassFdmaNav.setTime(fmod(pi.parseDouble4(line, Unit.ONE), Constants.JULIAN_DAY));

                    // Set the ephemeris epoch (same as time of clock epoch)
                    pi.glonassFdmaNav.setDate(pi.glonassFdmaNav.getEpochToc());
                }

            }

            /** {@inheritDoc} */
            @Override
            public void parseFirstBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.glonassFdmaNav.setX(pi.parseDouble1(line, KM));
                pi.glonassFdmaNav.setXDot(pi.parseDouble2(line, KM_PER_S));
                pi.glonassFdmaNav.setXDotDot(pi.parseDouble3(line, KM_PER_S2));
                pi.glonassFdmaNav.setHealth(pi.parseDouble4(line, Unit.NONE));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSecondBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.glonassFdmaNav.setY(pi.parseDouble1(line, KM));
                pi.glonassFdmaNav.setYDot(pi.parseDouble2(line, KM_PER_S));
                pi.glonassFdmaNav.setYDotDot(pi.parseDouble3(line, KM_PER_S2));
                pi.glonassFdmaNav.setFrequencyNumber(pi.parseDouble4(line, Unit.NONE));
            }

            /** {@inheritDoc} */
            @Override
            public void parseThirdBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.glonassFdmaNav.setZ(pi.parseDouble1(line, KM));
                pi.glonassFdmaNav.setZDot(pi.parseDouble2(line, KM_PER_S));
                pi.glonassFdmaNav.setZDotDot(pi.parseDouble3(line, KM_PER_S2));
                if (pi.file.getHeader().getFormatVersion() < 3.045) {
                    pi.closePendingMessage();
                }
            }

            /** {@inheritDoc} */
            @Override
            public void parseFourthBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.glonassFdmaNav.setStatusFlags(pi.parseDouble1(line, Unit.NONE));
                pi.glonassFdmaNav.setGroupDelayDifference(pi.parseDouble2(line, Unit.NONE));
                pi.glonassFdmaNav.setURA(pi.parseDouble3(line, Unit.NONE));
                pi.glonassFdmaNav.setHealthFlags(pi.parseDouble4(line, Unit.NONE));
                pi.closePendingMessage();
            }

            /** {@inheritDoc} */
            @Override
            public void closeMessage(final ParseInfo pi) {
                pi.file.addGlonassNavigationMessage(pi.glonassFdmaNav);
                pi.glonassFdmaNav = null;
            }

        },

        /** Glonass CDMA. */
        GLONASS_CDMA_CURRENTLY_IGNORED() {

            /** {@inheritDoc} */
            @Override
            public void parseSvEpochSvClockLine(final String line, final ParseInfo pi) {
            }

            /** {@inheritDoc} */
            @Override
            public void parseFirstBroadcastOrbit(final String line, final ParseInfo pi) {
            }

            /** {@inheritDoc} */
            @Override
            public void parseSecondBroadcastOrbit(final String line, final ParseInfo pi) {
            }

            /** {@inheritDoc} */
            @Override
            public void parseThirdBroadcastOrbit(final String line, final ParseInfo pi) {
            }

            /** {@inheritDoc} */
            @Override
            public void parseFourthBroadcastOrbit(final String line, final ParseInfo pi) {
            }

            /** {@inheritDoc} */
            @Override
            public void parseFifthBroadcastOrbit(final String line, final ParseInfo pi) {
            }

            /** {@inheritDoc} */
            @Override
            public void parseSixthBroadcastOrbit(final String line, final ParseInfo pi) {
            }

            /** {@inheritDoc} */
            @Override
            public void parseSeventhBroadcastOrbit(final String line, final ParseInfo pi) {
            }

            /** {@inheritDoc} */
            @Override
            public void parseEighthBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.closePendingMessage();
            }

            /** {@inheritDoc} */
            @Override
            public void closeMessage(final ParseInfo pi) {
            }

        },

        /** QZSS legacy. */
        QZSS_LNAV() {

            /** {@inheritDoc} */
            @Override
            public void parseSvEpochSvClockLine(final String line, final ParseInfo pi) {
                parseSvEpochSvClockLine(line, pi.timeScales.getGPS(), pi, pi.qzssLNav);
            }

            /** {@inheritDoc} */
            @Override
            public void parseFirstBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.qzssLNav.setIODE(pi.parseDouble1(line, Unit.SECOND));
                pi.qzssLNav.setCrs(pi.parseDouble2(line, Unit.METRE));
                pi.qzssLNav.setDeltaN0(pi.parseDouble3(line, RAD_PER_S));
                pi.qzssLNav.setM0(pi.parseDouble4(line, Unit.RADIAN));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSecondBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.qzssLNav.setCuc(pi.parseDouble1(line, Unit.RADIAN));
                pi.qzssLNav.setE(pi.parseDouble2(line, Unit.NONE));
                pi.qzssLNav.setCus(pi.parseDouble3(line, Unit.RADIAN));
                pi.qzssLNav.setSqrtA(pi.parseDouble4(line, SQRT_M));
            }

            /** {@inheritDoc} */
            @Override
            public void parseThirdBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.qzssLNav.setTime(pi.parseDouble1(line, Unit.SECOND));
                pi.qzssLNav.setCic(pi.parseDouble2(line, Unit.RADIAN));
                pi.qzssLNav.setOmega0(pi.parseDouble3(line, Unit.RADIAN));
                pi.qzssLNav.setCis(pi.parseDouble4(line, Unit.RADIAN));
            }

            /** {@inheritDoc} */
            @Override
            public void parseFourthBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.qzssLNav.setI0(pi.parseDouble1(line, Unit.RADIAN));
                pi.qzssLNav.setCrc(pi.parseDouble2(line, Unit.METRE));
                pi.qzssLNav.setPa(pi.parseDouble3(line, Unit.RADIAN));
                pi.qzssLNav.setOmegaDot(pi.parseDouble4(line, RAD_PER_S));
            }

            /** {@inheritDoc} */
            @Override
            public void parseFifthBroadcastOrbit(final String line, final ParseInfo pi) {
                // iDot
                pi.qzssLNav.setIDot(pi.parseDouble1(line, RAD_PER_S));
                pi.qzssLNav.setL2Codes(pi.parseInt2(line));
                pi.qzssLNav.setWeek(pi.parseInt3(line));
                pi.qzssLNav.setL2PFlags(pi.parseInt4(line));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSixthBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.qzssLNav.setSvAccuracy(pi.parseDouble1(line, Unit.METRE));
                pi.qzssLNav.setSvHealth(pi.parseInt2(line));
                pi.qzssLNav.setTGD(pi.parseDouble3(line, Unit.SECOND));
                pi.qzssLNav.setIODC(pi.parseInt4(line));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSeventhBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.qzssLNav.setTransmissionTime(pi.parseDouble1(line, Unit.SECOND));
                pi.qzssLNav.setFitInterval(pi.parseInt2(line));
                pi.closePendingMessage();
            }

            /** {@inheritDoc} */
            @Override
            public void closeMessage(final ParseInfo pi) {
                pi.file.addQZSSLegacyNavigationMessage(pi.qzssLNav);
                pi.qzssLNav = null;
            }

        },

        /** QZSS civilian.
         * @since 12.0
         */
        QZSS_CNAV() {

            /** {@inheritDoc} */
            @Override
            public void parseSvEpochSvClockLine(final String line, final ParseInfo pi) {
                parseSvEpochSvClockLine(line, pi.timeScales.getGPS(), pi, pi.qzssCNav);
            }

            /** {@inheritDoc} */
            @Override
            public void parseFirstBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.qzssCNav.setADot(pi.parseDouble1(line, M_PER_S));
                pi.qzssCNav.setCrs(pi.parseDouble2(line, Unit.METRE));
                pi.qzssCNav.setDeltaN0(pi.parseDouble3(line, RAD_PER_S));
                pi.qzssCNav.setM0(pi.parseDouble4(line, Unit.RADIAN));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSecondBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.qzssCNav.setCuc(pi.parseDouble1(line, Unit.RADIAN));
                pi.qzssCNav.setE(pi.parseDouble2(line, Unit.NONE));
                pi.qzssCNav.setCus(pi.parseDouble3(line, Unit.RADIAN));
                pi.qzssCNav.setSqrtA(pi.parseDouble4(line, SQRT_M));
            }

            /** {@inheritDoc} */
            @Override
            public void parseThirdBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.qzssCNav.setTime(pi.parseDouble1(line, Unit.SECOND));
                pi.qzssCNav.setCic(pi.parseDouble2(line, Unit.RADIAN));
                pi.qzssCNav.setOmega0(pi.parseDouble3(line, Unit.RADIAN));
                pi.qzssCNav.setCis(pi.parseDouble4(line, Unit.RADIAN));
            }

            /** {@inheritDoc} */
            @Override
            public void parseFourthBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.qzssCNav.setI0(pi.parseDouble1(line, Unit.RADIAN));
                pi.qzssCNav.setCrc(pi.parseDouble2(line, Unit.METRE));
                pi.qzssCNav.setPa(pi.parseDouble3(line, Unit.RADIAN));
                pi.qzssCNav.setOmegaDot(pi.parseDouble4(line, RAD_PER_S));
            }

            /** {@inheritDoc} */
            @Override
            public void parseFifthBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.qzssCNav.setIDot(pi.parseDouble1(line, RAD_PER_S));
                pi.qzssCNav.setDeltaN0Dot(pi.parseDouble2(line, RAD_PER_S2));
                pi.qzssCNav.setUraiNed0(pi.parseInt3(line));
                pi.qzssCNav.setUraiNed1(pi.parseInt4(line));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSixthBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.qzssCNav.setUraiEd(pi.parseInt1(line));
                pi.qzssCNav.setSvHealth(pi.parseInt2(line));
                pi.qzssCNav.setTGD(pi.parseDouble3(line, Unit.SECOND));
                pi.qzssCNav.setUraiNed2(pi.parseInt4(line));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSeventhBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.qzssCNav.setIscL1CA(pi.parseDouble1(line, Unit.SECOND));
                pi.qzssCNav.setIscL2C(pi.parseDouble2(line, Unit.SECOND));
                pi.qzssCNav.setIscL5I5(pi.parseDouble3(line, Unit.SECOND));
                pi.qzssCNav.setIscL5Q5(pi.parseDouble4(line, Unit.SECOND));
            }

            /** {@inheritDoc} */
            @Override
            public void parseEighthBroadcastOrbit(final String line, final ParseInfo pi) {
                if (pi.qzssCNav.isCnv2()) {
                    // in CNAV2 messages, there is an additional line for L1 CD and L1 CP inter signal delay
                    pi.qzssCNav.setIscL1CD(pi.parseDouble1(line, Unit.SECOND));
                    pi.qzssCNav.setIscL1CP(pi.parseDouble2(line, Unit.SECOND));
                } else {
                    parseTransmissionTimeLine(line, pi);
                }
            }

            /** {@inheritDoc} */
            @Override
            public void parseNinthBroadcastOrbit(final String line, final ParseInfo pi) {
                parseTransmissionTimeLine(line, pi);
            }

            /** Parse transmission time line.
             * @param line line to parse
             * @param pi holder for transient data
             */
            private void parseTransmissionTimeLine(final String line, final ParseInfo pi) {
                pi.qzssCNav.setTransmissionTime(pi.parseDouble1(line, Unit.SECOND));
                pi.qzssCNav.setWeek(pi.parseInt2(line));
                pi.closePendingMessage();
            }

            /** {@inheritDoc} */
            @Override
            public void closeMessage(final ParseInfo pi) {
                pi.file.addQZSSCivilianNavigationMessage(pi.qzssCNav);
                pi.qzssCNav = null;
            }

        },

        /** Beidou legacy. */
        BEIDOU_D1_D2() {

            /** {@inheritDoc} */
            @Override
            public void parseSvEpochSvClockLine(final String line, final ParseInfo pi) {
                parseSvEpochSvClockLine(line, pi.timeScales.getBDT(), pi, pi.beidouLNav);
            }

            /** {@inheritDoc} */
            @Override
            public void parseFirstBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.beidouLNav.setAODE(pi.parseDouble1(line, Unit.SECOND));
                pi.beidouLNav.setCrs(pi.parseDouble2(line, Unit.METRE));
                pi.beidouLNav.setDeltaN0(pi.parseDouble3(line, RAD_PER_S));
                pi.beidouLNav.setM0(pi.parseDouble4(line, Unit.RADIAN));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSecondBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.beidouLNav.setCuc(pi.parseDouble1(line, Unit.RADIAN));
                pi.beidouLNav.setE(pi.parseDouble2(line, Unit.NONE));
                pi.beidouLNav.setCus(pi.parseDouble3(line, Unit.RADIAN));
                pi.beidouLNav.setSqrtA(pi.parseDouble4(line, SQRT_M));
            }

            /** {@inheritDoc} */
            @Override
            public void parseThirdBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.beidouLNav.setTime(pi.parseDouble1(line, Unit.SECOND));
                pi.beidouLNav.setCic(pi.parseDouble2(line, Unit.RADIAN));
                pi.beidouLNav.setOmega0(pi.parseDouble3(line, Unit.RADIAN));
                pi.beidouLNav.setCis(pi.parseDouble4(line, Unit.RADIAN));
            }

            /** {@inheritDoc} */
            @Override
            public void parseFourthBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.beidouLNav.setI0(pi.parseDouble1(line, Unit.RADIAN));
                pi.beidouLNav.setCrc(pi.parseDouble2(line, Unit.METRE));
                pi.beidouLNav.setPa(pi.parseDouble3(line, Unit.RADIAN));
                pi.beidouLNav.setOmegaDot(pi.parseDouble4(line, RAD_PER_S));
            }

            /** {@inheritDoc} */
            @Override
            public void parseFifthBroadcastOrbit(final String line, final ParseInfo pi) {
                // iDot
                pi.beidouLNav.setIDot(pi.parseDouble1(line, RAD_PER_S));
                // BDT week (to go with Toe)
                pi.beidouLNav.setWeek(pi.parseInt3(line));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSixthBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.beidouLNav.setSvAccuracy(pi.parseDouble1(line, Unit.METRE));
                pi.beidouLNav.setSatH1(pi.parseInt2(line));
                pi.beidouLNav.setTGD1(pi.parseDouble3(line, Unit.SECOND));
                pi.beidouLNav.setTGD2(pi.parseDouble4(line, Unit.SECOND));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSeventhBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.beidouLNav.setTransmissionTime(pi.parseDouble1(line, Unit.SECOND));
                pi.beidouLNav.setAODC(pi.parseDouble2(line, Unit.SECOND));
                pi.closePendingMessage();
            }

            /** {@inheritDoc} */
            @Override
            public void closeMessage(final ParseInfo pi) {
                pi.file.addBeidouLegacyNavigationMessage(pi.beidouLNav);
                pi.beidouLNav = null;
            }

        },

        /** Beidou-3 CNAV. */
        BEIDOU_CNV_123() {

            /** {@inheritDoc} */
            @Override
            public void parseSvEpochSvClockLine(final String line, final ParseInfo pi) {
                parseSvEpochSvClockLine(line, pi.timeScales.getBDT(), pi, pi.beidouCNav);
            }

            /** {@inheritDoc} */
            @Override
            public void parseFirstBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.beidouCNav.setADot(pi.parseDouble1(line, M_PER_S));
                pi.beidouCNav.setCrs(pi.parseDouble2(line, Unit.METRE));
                pi.beidouCNav.setDeltaN0(pi.parseDouble3(line, RAD_PER_S));
                pi.beidouCNav.setM0(pi.parseDouble4(line, Unit.RADIAN));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSecondBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.beidouCNav.setCuc(pi.parseDouble1(line, Unit.RADIAN));
                pi.beidouCNav.setE(pi.parseDouble2(line, Unit.NONE));
                pi.beidouCNav.setCus(pi.parseDouble3(line, Unit.RADIAN));
                pi.beidouCNav.setSqrtA(pi.parseDouble4(line, SQRT_M));
            }

            /** {@inheritDoc} */
            @Override
            public void parseThirdBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.beidouCNav.setTime(pi.parseDouble1(line, Unit.SECOND));
                pi.beidouCNav.setCic(pi.parseDouble2(line, Unit.RADIAN));
                pi.beidouCNav.setOmega0(pi.parseDouble3(line, Unit.RADIAN));
                pi.beidouCNav.setCis(pi.parseDouble4(line, Unit.RADIAN));
            }

            /** {@inheritDoc} */
            @Override
            public void parseFourthBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.beidouCNav.setI0(pi.parseDouble1(line, Unit.RADIAN));
                pi.beidouCNav.setCrc(pi.parseDouble2(line, Unit.METRE));
                pi.beidouCNav.setPa(pi.parseDouble3(line, Unit.RADIAN));
                pi.beidouCNav.setOmegaDot(pi.parseDouble4(line, RAD_PER_S));
            }

            /** {@inheritDoc} */
            @Override
            public void parseFifthBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.beidouCNav.setIDot(pi.parseDouble1(line, RAD_PER_S));
                pi.beidouCNav.setDeltaN0Dot(pi.parseDouble2(line, RAD_PER_S2));
                switch (pi.parseInt3(line)) {
                    case 0 :
                        pi.beidouCNav.setSatelliteType(BeidouSatelliteType.RESERVED);
                        break;
                    case 1 :
                        pi.beidouCNav.setSatelliteType(BeidouSatelliteType.GEO);
                        break;
                    case 2 :
                        pi.beidouCNav.setSatelliteType(BeidouSatelliteType.IGSO);
                        break;
                    case 3 :
                        pi.beidouCNav.setSatelliteType(BeidouSatelliteType.MEO);
                        break;
                    default:
                        throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                  pi.lineNumber, pi.name, line);
                }
                pi.beidouCNav.setTime(pi.parseDouble4(line, Unit.SECOND));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSixthBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.beidouCNav.setSisaiOe(pi.parseInt1(line));
                pi.beidouCNav.setSisaiOcb(pi.parseInt2(line));
                pi.beidouCNav.setSisaiOc1(pi.parseInt3(line));
                pi.beidouCNav.setSisaiOc2(pi.parseInt4(line));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSeventhBroadcastOrbit(final String line, final ParseInfo pi) {
                if (pi.beidouCNav.getRadioWave().closeTo(PredefinedGnssSignal.B1C)) {
                    pi.beidouCNav.setIscB1CD(pi.parseDouble1(line, Unit.SECOND));
                    // field 2 is spare
                    pi.beidouCNav.setTgdB1Cp(pi.parseDouble3(line, Unit.SECOND));
                    pi.beidouCNav.setTgdB2ap(pi.parseDouble4(line, Unit.SECOND));
                } else if (pi.beidouCNav.getRadioWave().closeTo(PredefinedGnssSignal.B2A)) {
                    // field 1 is spare
                    pi.beidouCNav.setIscB2AD(pi.parseDouble2(line, Unit.SECOND));
                    pi.beidouCNav.setTgdB1Cp(pi.parseDouble3(line, Unit.SECOND));
                    pi.beidouCNav.setTgdB2ap(pi.parseDouble4(line, Unit.SECOND));
                } else {
                    parseSismaiHealthIntegrity(line, pi);
                }
            }

            /** {@inheritDoc} */
            @Override
            public void parseEighthBroadcastOrbit(final String line, final ParseInfo pi) {
                if (pi.beidouCNav.getRadioWave().closeTo(PredefinedGnssSignal.B2B)) {
                    pi.beidouCNav.setTransmissionTime(pi.parseDouble1(line, Unit.SECOND));
                    pi.closePendingMessage();
                } else {
                    parseSismaiHealthIntegrity(line, pi);
                }
            }

            /** {@inheritDoc} */
            @Override
            public void parseNinthBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.beidouCNav.setTransmissionTime(pi.parseDouble1(line, Unit.SECOND));
                // field 2 is spare
                // field 3 is spare
                pi.beidouCNav.setIODE(pi.parseInt4(line));
                pi.closePendingMessage();
            }

            /** {@inheritDoc} */
            @Override
            public void closeMessage(final ParseInfo pi) {
                pi.file.addBeidouCivilianNavigationMessage(pi.beidouCNav);
                pi.beidouCNav = null;
            }

            /**
             * Parse the SISMAI/Health/integrity line.
             * @param line line to read
             * @param pi holder for transient data
             */
            private void parseSismaiHealthIntegrity(final String line, final ParseInfo pi) {
                pi.beidouCNav.setSismai(pi.parseInt1(line));
                pi.beidouCNav.setHealth(pi.parseInt2(line));
                pi.beidouCNav.setIntegrityFlags(pi.parseInt3(line));
                pi.beidouCNav.setIODC(pi.parseInt4(line));
            }

        },

        /** SBAS. */
        SBAS() {

            /** {@inheritDoc} */
            @Override
            public void parseSvEpochSvClockLine(final String line, final ParseInfo pi) {

                // parse PRN
                pi.sbasNav.setPRN(ParsingUtils.parseInt(line, 1, 2));

                // Time scale (UTC for Rinex 3.01 and GPS for other RINEX versions)
                final int       version100 = (int) FastMath.rint(pi.file.getHeader().getFormatVersion() * 100);
                final TimeScale timeScale  = (version100 == 301) ? pi.timeScales.getUTC() : pi.timeScales.getGPS();

                pi.sbasNav.setEpochToc(pi.parseDate(line, timeScale));
                pi.sbasNav.setAGf0(pi.parseDouble2(line, Unit.SECOND));
                pi.sbasNav.setAGf1(pi.parseDouble3(line, S_PER_S));
                pi.sbasNav.setTime(pi.parseDouble4(line, Unit.SECOND));

                // Set the ephemeris epoch (same as time of clock epoch)
                pi.sbasNav.setDate(pi.sbasNav.getEpochToc());

            }

            /** {@inheritDoc} */
            @Override
            public void parseFirstBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.sbasNav.setX(pi.parseDouble1(line, KM));
                pi.sbasNav.setXDot(pi.parseDouble2(line, KM_PER_S));
                pi.sbasNav.setXDotDot(pi.parseDouble3(line, KM_PER_S2));
                pi.sbasNav.setHealth(pi.parseDouble4(line, Unit.NONE));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSecondBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.sbasNav.setY(pi.parseDouble1(line, KM));
                pi.sbasNav.setYDot(pi.parseDouble2(line, KM_PER_S));
                pi.sbasNav.setYDotDot(pi.parseDouble3(line, KM_PER_S2));
                pi.sbasNav.setURA(pi.parseDouble4(line, Unit.NONE));
            }

            /** {@inheritDoc} */
            @Override
            public void parseThirdBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.sbasNav.setZ(pi.parseDouble1(line, KM));
                pi.sbasNav.setZDot(pi.parseDouble2(line, KM_PER_S));
                pi.sbasNav.setZDotDot(pi.parseDouble3(line, KM_PER_S2));
                pi.sbasNav.setIODN(pi.parseDouble4(line, Unit.NONE));
                pi.closePendingMessage();
            }

            /** {@inheritDoc} */
            @Override
            public void closeMessage(final ParseInfo pi) {
                pi.file.addSBASNavigationMessage(pi.sbasNav);
                pi.sbasNav = null;
            }

        },

        /** NavIC. */
        NAVIC_LNAV() {

            /** {@inheritDoc} */
            @Override
            public void parseSvEpochSvClockLine(final String line, final ParseInfo pi) {
                parseSvEpochSvClockLine(line, pi.timeScales.getNavIC(), pi, pi.navicLNav);
            }

            /** {@inheritDoc} */
            @Override
            public void parseFirstBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.navicLNav.setIODE(pi.parseInt1(line));
                pi.navicLNav.setIODC(pi.navicLNav.getIODE());
                pi.navicLNav.setCrs(pi.parseDouble2(line, Unit.METRE));
                pi.navicLNav.setDeltaN0(pi.parseDouble3(line, RAD_PER_S));
                pi.navicLNav.setM0(pi.parseDouble4(line, Unit.RADIAN));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSecondBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.navicLNav.setCuc(pi.parseDouble1(line, Unit.RADIAN));
                pi.navicLNav.setE(pi.parseDouble2(line, Unit.NONE));
                pi.navicLNav.setCus(pi.parseDouble3(line, Unit.RADIAN));
                pi.navicLNav.setSqrtA(pi.parseDouble4(line, SQRT_M));
            }

            /** {@inheritDoc} */
            @Override
            public void parseThirdBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.navicLNav.setTime(pi.parseDouble1(line, Unit.SECOND));
                pi.navicLNav.setCic(pi.parseDouble2(line, Unit.RADIAN));
                pi.navicLNav.setOmega0(pi.parseDouble3(line, Unit.RADIAN));
                pi.navicLNav.setCis(pi.parseDouble4(line, Unit.RADIAN));
            }

            /** {@inheritDoc} */
            @Override
            public void parseFourthBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.navicLNav.setI0(pi.parseDouble1(line, Unit.RADIAN));
                pi.navicLNav.setCrc(pi.parseDouble2(line, Unit.METRE));
                pi.navicLNav.setPa(pi.parseDouble3(line, Unit.RADIAN));
                pi.navicLNav.setOmegaDot(pi.parseDouble4(line, RAD_PER_S));
            }

            /** {@inheritDoc} */
            @Override
            public void parseFifthBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.navicLNav.setIDot(pi.parseDouble1(line, RAD_PER_S));
                pi.navicLNav.setL2Codes(pi.parseInt2(line));
                pi.navicLNav.setWeek(pi.parseInt3(line));
                pi.navicLNav.setL2PFlags(pi.parseInt4(line));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSixthBroadcastOrbit(final String line, final ParseInfo pi) {
                final int uraIndex = pi.parseInt1(line);
                pi.navicLNav.setSvAccuracy(NAVIC_URA[FastMath.min(uraIndex, NAVIC_URA.length - 1)]);
                pi.navicLNav.setSvHealth(pi.parseInt2(line));
                pi.navicLNav.setTGD(pi.parseDouble3(line, Unit.SECOND));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSeventhBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.navicLNav.setTransmissionTime(pi.parseDouble1(line, Unit.SECOND));
                pi.closePendingMessage();
            }

            /** {@inheritDoc} */
            @Override
            public void closeMessage(final ParseInfo pi) {
                pi.file.addNavICLegacyNavigationMessage(pi.navicLNav);
                pi.navicLNav = null;
            }

        },

        /** NavIC L1NV.
         * @since 12.0
         */
        NAVIC_L1NV() {

            /** {@inheritDoc} */
            @Override
            public void parseSvEpochSvClockLine(final String line, final ParseInfo pi) {
                parseSvEpochSvClockLine(line, pi.timeScales.getGPS(), pi, pi.navicL1NV);
            }

            /** {@inheritDoc} */
            @Override
            public void parseFirstBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.navicL1NV.setADot(pi.parseDouble1(line, M_PER_S));
                pi.navicL1NV.setCrs(pi.parseDouble2(line, Unit.METRE));
                pi.navicL1NV.setDeltaN0(pi.parseDouble3(line, RAD_PER_S));
                pi.navicL1NV.setM0(pi.parseDouble4(line, Unit.RADIAN));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSecondBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.navicL1NV.setCuc(pi.parseDouble1(line, Unit.RADIAN));
                pi.navicL1NV.setE(pi.parseDouble2(line, Unit.NONE));
                pi.navicL1NV.setCus(pi.parseDouble3(line, Unit.RADIAN));
                pi.navicL1NV.setSqrtA(pi.parseDouble4(line, SQRT_M));
            }

            /** {@inheritDoc} */
            @Override
            public void parseThirdBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.navicL1NV.setTime(pi.parseDouble1(line, Unit.SECOND));
                pi.navicL1NV.setCic(pi.parseDouble2(line, Unit.RADIAN));
                pi.navicL1NV.setOmega0(pi.parseDouble3(line, Unit.RADIAN));
                pi.navicL1NV.setCis(pi.parseDouble4(line, Unit.RADIAN));
            }

            /** {@inheritDoc} */
            @Override
            public void parseFourthBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.navicL1NV.setI0(pi.parseDouble1(line, Unit.RADIAN));
                pi.navicL1NV.setCrc(pi.parseDouble2(line, Unit.METRE));
                pi.navicL1NV.setPa(pi.parseDouble3(line, Unit.RADIAN));
                pi.navicL1NV.setOmegaDot(pi.parseDouble4(line, RAD_PER_S));
            }

            /** {@inheritDoc} */
            @Override
            public void parseFifthBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.navicL1NV.setIDot(pi.parseDouble1(line, RAD_PER_S));
                pi.navicL1NV.setDeltaN0Dot(pi.parseDouble2(line, RAD_PER_S2));
                pi.navicL1NV.setReferenceSignalFlag(pi.parseInt4(line));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSixthBroadcastOrbit(final String line, final ParseInfo pi) {
                final int uraIndex = pi.parseInt1(line);
                pi.navicL1NV.setSvAccuracy(NAVIC_URA[FastMath.min(uraIndex, NAVIC_URA.length - 1)]);
                pi.navicL1NV.setSvHealth(pi.parseInt2(line));
                pi.navicL1NV.setTGD(pi.parseDouble3(line, Unit.SECOND));
                pi.navicL1NV.setTGDSL5(pi.parseDouble4(line, Unit.SECOND));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSeventhBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.navicL1NV.setIscSL1P(pi.parseDouble1(line, Unit.SECOND));
                pi.navicL1NV.setIscL1DL1P(pi.parseDouble2(line, Unit.SECOND));
                pi.navicL1NV.setIscL1PS(pi.parseDouble3(line, Unit.SECOND));
                pi.navicL1NV.setIscL1DS(pi.parseDouble4(line, Unit.SECOND));
            }

            /** {@inheritDoc} */
            @Override
            public void parseEighthBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.navicL1NV.setTransmissionTime(pi.parseDouble1(line, Unit.SECOND));
                pi.navicL1NV.setWeek(pi.parseInt2(line));
                pi.closePendingMessage();
            }

            /** {@inheritDoc} */
            @Override
            public void closeMessage(final ParseInfo pi) {
                pi.file.addNavICL1NVNavigationMessage(pi.navicL1NV);
                pi.navicL1NV = null;
            }

        };

        /** Get the parse for navigation message.
         * @param system satellite system
         * @param type message type (null for Rinex 3.x)
         * @param parseInfo container for transient data
         * @param line line being parsed
         * @return the satellite system line parser
         */
        private static SatelliteSystemLineParser getParser(final SatelliteSystem system, final String type,
                                                           final ParseInfo parseInfo, final String line) {
            switch (system) {
                case GPS :
                    if (type == null || type.equals(GPSLegacyNavigationMessage.LNAV)) {
                        // in Rinex, week number is aligned to GPS week!
                        parseInfo.gpsLNav = new GPSLegacyNavigationMessage(parseInfo.timeScales,
                                                                           SatelliteSystem.GPS,
                                                                           GPSLegacyNavigationMessage.LNAV);
                        return GPS_LNAV;
                    } else if (type.equals(GPSCivilianNavigationMessage.CNAV)) {
                        // in Rinex, week number is aligned to GPS week!
                        parseInfo.gpsCNav = new GPSCivilianNavigationMessage(false,
                                                                             parseInfo.timeScales,
                                                                             SatelliteSystem.GPS,
                                                                             GPSCivilianNavigationMessage.CNAV);
                        return GPS_CNAV;
                    } else if (type.equals(GPSCivilianNavigationMessage.CNV2)) {
                        // in Rinex, week number is aligned to GPS week!
                        parseInfo.gpsCNav = new GPSCivilianNavigationMessage(true,
                                                                             parseInfo.timeScales,
                                                                             SatelliteSystem.GPS,
                                                                             GPSCivilianNavigationMessage.CNV2);
                        return GPS_CNAV;
                    }
                    break;
                case GALILEO :
                    if (type == null ||
                        type.equals(GalileoNavigationMessage.INAV) ||
                        type.equals(GalileoNavigationMessage.FNAV)) {
                        // in Rinex, week number is aligned to GPS week!
                        parseInfo.galileoNav = new GalileoNavigationMessage(parseInfo.timeScales,
                                                                            SatelliteSystem.GPS,
                                                                            type);
                        return GALILEO;
                    }
                    break;
                case GLONASS :
                    if (type == null || type.equals("FDMA")) {
                        parseInfo.glonassFdmaNav = new GLONASSFdmaNavigationMessage();
                        return GLONASS_FDMA;
                    } else if (type.equals("L1OC") || type.equals("L3OC")) {
                        return GLONASS_CDMA_CURRENTLY_IGNORED;
                    }
                    break;
                case QZSS :
                    if (type == null || type.equals(QZSSLegacyNavigationMessage.LNAV)) {
                        // in Rinex, week number is aligned to GPS week!
                        parseInfo.qzssLNav = new QZSSLegacyNavigationMessage(parseInfo.timeScales,
                                                                             SatelliteSystem.GPS,
                                                                             QZSSLegacyNavigationMessage.LNAV);
                        return QZSS_LNAV;
                    } else if (type.equals(QZSSCivilianNavigationMessage.CNAV)) {
                        // in Rinex, week number is aligned to GPS week!
                        parseInfo.qzssCNav = new QZSSCivilianNavigationMessage(false,
                                                                               parseInfo.timeScales,
                                                                               SatelliteSystem.GPS,
                                                                               QZSSCivilianNavigationMessage.CNAV);
                        return QZSS_CNAV;
                    } else if (type.equals(QZSSCivilianNavigationMessage.CNV2)) {
                        // in Rinex, week number is aligned to GPS week!
                        parseInfo.qzssCNav = new QZSSCivilianNavigationMessage(true,
                                                                               parseInfo.timeScales,
                                                                               SatelliteSystem.GPS,
                                                                               QZSSCivilianNavigationMessage.CNV2);
                        return QZSS_CNAV;
                    }
                    break;
                case BEIDOU :
                    if (type == null ||
                        type.equals(BeidouLegacyNavigationMessage.D1) ||
                        type.equals(BeidouLegacyNavigationMessage.D2)) {
                        // in Rinex, week number for Beidou is really aligned to Beidou week!
                        parseInfo.beidouLNav = new BeidouLegacyNavigationMessage(parseInfo.timeScales,
                                                                                 SatelliteSystem.BEIDOU,
                                                                                 type);
                        return BEIDOU_D1_D2;
                    } else if (type.equals(BeidouCivilianNavigationMessage.CNV1)) {
                        // in Rinex, week number for Beidou is really aligned to Beidou week!
                        parseInfo.beidouCNav = new BeidouCivilianNavigationMessage(PredefinedGnssSignal.B1C,
                                                                                   parseInfo.timeScales,
                                                                                   SatelliteSystem.BEIDOU,
                                                                                   BeidouCivilianNavigationMessage.CNV1);
                        return BEIDOU_CNV_123;
                    } else if (type.equals(BeidouCivilianNavigationMessage.CNV2)) {
                        // in Rinex, week number for Beidou is really aligned to Beidou week!
                        parseInfo.beidouCNav = new BeidouCivilianNavigationMessage(PredefinedGnssSignal.B2A,
                                                                                   parseInfo.timeScales,
                                                                                   SatelliteSystem.BEIDOU,
                                                                                   BeidouCivilianNavigationMessage.CNV2);
                        return BEIDOU_CNV_123;
                    } else if (type.equals(BeidouCivilianNavigationMessage.CNV3)) {
                        // in Rinex, week number for Beidou is really aligned to Beidou week!
                        parseInfo.beidouCNav = new BeidouCivilianNavigationMessage(PredefinedGnssSignal.B2B,
                                                                                   parseInfo.timeScales,
                                                                                   SatelliteSystem.BEIDOU,
                                                                                   BeidouCivilianNavigationMessage.CNV3);
                        return BEIDOU_CNV_123;
                    }
                    break;
                case NAVIC:
                    if (type == null || type.equals(NavICLegacyNavigationMessage.LNAV)) {
                        // in Rinex, week number is aligned to GPS week!
                        parseInfo.navicLNav = new NavICLegacyNavigationMessage(parseInfo.timeScales,
                                                                               SatelliteSystem.GPS,
                                                                               NavICLegacyNavigationMessage.LNAV);
                        return NAVIC_LNAV;
                    } else if (type.equals(NavICL1NVNavigationMessage.L1NV)) {
                        // in Rinex, week number is aligned to GPS week!
                        parseInfo.navicL1NV = new NavICL1NVNavigationMessage(parseInfo.timeScales,
                                                                             SatelliteSystem.GPS,
                                                                             NavICL1NVNavigationMessage.L1NV);
                        return NAVIC_L1NV;
                    }
                    break;
                case SBAS :
                    if (type == null || type.equals("SBAS")) {
                        parseInfo.sbasNav = new SBASNavigationMessage();
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
         * @param timeScale time scale to use
         * @param message navigation message
         */
        protected void parseSvEpochSvClockLineRinex2(final String line, final TimeScale timeScale,
                                                     final AbstractNavigationMessage<?> message) {
            // PRN
            message.setPRN(ParsingUtils.parseInt(line, 0, 2));

            // Toc
            final int    year  = ParsingUtils.convert2DigitsYear(ParsingUtils.parseInt(line, 2, 3));
            final int    month = ParsingUtils.parseInt(line, 5, 3);
            final int    day   = ParsingUtils.parseInt(line, 8, 3);
            final int    hours = ParsingUtils.parseInt(line, 11, 3);
            final int    min   = ParsingUtils.parseInt(line, 14, 3);
            final double sec   = ParsingUtils.parseDouble(line, 17, 5);
            message.setEpochToc(new AbsoluteDate( year, month, day, hours, min, sec, timeScale));

            // clock
            message.setAf0(ParsingUtils.parseDouble(line, 22, 19));
            message.setAf1(ParsingUtils.parseDouble(line, 41, 19));
            message.setAf2(ParsingUtils.parseDouble(line, 60, 19));

        }

        /**
         * Parse the SV/Epoch/Sv clock of the navigation message.
         * @param line line to read
         * @param timeScale time scale to use
         * @param parseInfo container for parsing info
         * @param message navigation message
         */
        protected void parseSvEpochSvClockLine(final String line, final TimeScale timeScale,
                                               final ParseInfo parseInfo, final AbstractNavigationMessage<?> message) {
            // PRN
            message.setPRN(ParsingUtils.parseInt(line, 1, 2));

            // Toc
            message.setEpochToc(parseInfo.parseDate(line, timeScale));

            // clock
            message.setAf0(parseInfo.parseDouble2(line, Unit.SECOND));
            message.setAf1(parseInfo.parseDouble3(line, S_PER_S));
            message.setAf2(parseInfo.parseDouble4(line, S_PER_S2));

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
            // this should never be called (except by some tests that use reflection)
            throw new OrekitInternalError(null);
        }

        /**
         * Parse the "BROADCASTORBIT - 5" line.
         * @param line line to read
         * @param pi holder for transient data
         */
        public void parseFifthBroadcastOrbit(final String line, final ParseInfo pi) {
            // this should never be called (except by some tests that use reflection)
            throw new OrekitInternalError(null);
        }

        /**
         * Parse the "BROADCASTORBIT - 6" line.
         * @param line line to read
         * @param pi holder for transient data
         */
        public void parseSixthBroadcastOrbit(final String line, final ParseInfo pi) {
            // this should never be called (except by some tests that use reflection)
            throw new OrekitInternalError(null);
        }

        /**
         * Parse the "BROADCASTORBIT - 7" line.
         * @param line line to read
         * @param pi holder for transient data
         */
        public void parseSeventhBroadcastOrbit(final String line, final ParseInfo pi) {
            // this should never be called (except by some tests that use reflection)
            throw new OrekitInternalError(null);
        }

        /**
         * Parse the "BROADCASTORBIT - 8" line.
         * @param line line to read
         * @param pi holder for transient data
         */
        public void parseEighthBroadcastOrbit(final String line, final ParseInfo pi) {
            // this should never be called (except by some tests that use reflection)
            throw new OrekitInternalError(null);
        }

        /**
         * Parse the "BROADCASTORBIT - 9" line.
         * @param line line to read
         * @param pi holder for transient data
         */
        public void parseNinthBroadcastOrbit(final String line, final ParseInfo pi) {
            // this should never be called (except by some tests that use reflection)
            throw new OrekitInternalError(null);
        }

        /**
         * Close a message as last line was parsed.
         * @param pi holder for transient data
         */
        public abstract void closeMessage(ParseInfo pi);

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
