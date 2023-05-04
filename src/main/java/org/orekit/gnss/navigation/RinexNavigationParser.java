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
import java.util.Arrays;
import java.util.Collections;
import java.util.InputMismatchException;
import java.util.function.Function;
import java.util.function.Predicate;

import org.hipparchus.util.FastMath;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.gnss.Frequency;
import org.orekit.gnss.RegionCode;
import org.orekit.gnss.RinexUtils;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.gnss.SbasId;
import org.orekit.gnss.TimeSystem;
import org.orekit.gnss.UtcId;
import org.orekit.propagation.analytical.gnss.data.AbstractNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.BeidouCivilianNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.BeidouLegacyNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.BeidouSatelliteType;
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

    /** Converter for ΔUT₁ first derivative. */
    private static final Unit S_PER_DAY = Unit.parse("s/d");

    /** Converter for ΔUT₁ second derivative. */
    private static final Unit S_PER_DAY2 = Unit.parse("s/d²");

    /** Converter for square root of semi-major axis. */
    private static final Unit SQRT_M = Unit.parse("√m");

    /** Converter for angular rates. */
    private static final Unit RAD_PER_S = Unit.parse("rad/s");;

    /** Converter for angular accelerations. */
    private static final Unit RAD_PER_S2 = Unit.parse("rad/s²");;

    /** Converter for rates of small angle. */
    private static final Unit AS_PER_DAY = Unit.parse("as/d");;

    /** Converter for accelerations of small angles. */
    private static final Unit AS_PER_DAY2 = Unit.parse("as/d²");;

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
        final ParseInfo pi = new ParseInfo(source.getName());

        Iterable<LineParser> candidateParsers = Collections.singleton(LineParser.HEADER_VERSION);
        try (Reader reader = source.getOpener().openReaderOnce();
             BufferedReader br = new BufferedReader(reader)) {
            nextLine:
                for (String line = br.readLine(); line != null; line = br.readLine()) {
                    ++pi.lineNumber;
                    for (final LineParser candidate : candidateParsers) {
                        if (candidate.canHandle.test(line)) {
                            try {
                                candidate.parsingMethod.parse(line, pi);
                                candidateParsers = candidate.allowedNextProvider.apply(pi);
                                continue nextLine;
                            } catch (StringIndexOutOfBoundsException | NumberFormatException | InputMismatchException e) {
                                throw new OrekitException(e,
                                                          OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                          pi.lineNumber, source.getName(), line);
                            }
                        }
                    }
                    throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                              pi.lineNumber, source.getName(), line);
                }
        }

        pi.closePendingMessage();

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
        private BeidouLegacyNavigationMessage beidouLNav;

        /** Container for Beidou navigation message. */
        private BeidouCivilianNavigationMessage beidouCNav;

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

        /** Container for System Time Offset message. */
        private SystemTimeOffsetMessage sto;

        /** Container for Earth Orientation Parameter message. */
        private EarthOrientationParameterMessage eop;

        /** Container for ionosphere Klobuchar message. */
        private IonosphereKlobucharMessage klobuchar;

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
            this.systemLineParser             = SatelliteSystemLineParser.GPS_LNAV;
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
                       if (header.getRunByName() == null ||
                           version >= 4 && header.getNumberOfLeapSeconds() < 0) {
                           throw new OrekitException(OrekitMessages.INCOMPLETE_HEADER, pi.name);
                       }
                   },
                   LineParser::navigationNext),

        /** Parser for navigation message space vehicle epoch and clock. */
        NAVIGATION_SV_EPOCH_CLOCK(line -> INITIALS.indexOf(line.charAt(0)) >= 0,
                                  (line, pi) -> {

                                      // Set the line number to 0
                                      pi.messageLineNumber = 0;

                                      if (pi.file.getHeader().getFormatVersion() < 4) {
                                          // Current satellite system
                                          final SatelliteSystem system = SatelliteSystem.parseSatelliteSystem(RinexUtils.parseString(line, 0, 1));

                                          // Initialize parser
                                          pi.closePendingMessage();
                                          pi.systemLineParser = SatelliteSystemLineParser.getParser(system, null, pi, line);
                                      }

                                      // Read first line
                                      pi.systemLineParser.parseSvEpochSvClockLine(line, pi);

                                  },
                                  LineParser::navigationNext),

        /** Parser for navigation message type. */
        EPH_TYPE(line -> line.startsWith("> EPH"),
                 (line, pi) -> {
                     final SatelliteSystem system = SatelliteSystem.parseSatelliteSystem(RinexUtils.parseString(line, 6, 1));
                     final String          type   = RinexUtils.parseString(line, 10, 4);
                     pi.closePendingMessage();
                     pi.systemLineParser = SatelliteSystemLineParser.getParser(system, type, pi, line);
                 },
                 pi -> Collections.singleton(NAVIGATION_SV_EPOCH_CLOCK)),

        /** Parser for broadcast orbit. */
        BROADCAST_ORBIT(line -> line.startsWith("    "),
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
        STO_LINE_1(line -> true,
                   (line, pi) -> {
                       pi.sto.setTransmissionTime(Unit.SECOND.toSI(RinexUtils.parseDouble(line,  4, 19)));
                       pi.sto.setA0(Unit.SECOND.toSI(RinexUtils.parseDouble(line, 23, 19)));
                       pi.sto.setA1(S_PER_S.toSI(RinexUtils.parseDouble(line, 42, 19)));
                       pi.sto.setA2(S_PER_S2.toSI(RinexUtils.parseDouble(line, 61, 19)));
                       pi.file.addSystemTimeOffset(pi.sto);
                       pi.sto = null;
                   },
                   LineParser::navigationNext),

        /** Parser for system time offset message space vehicle epoch and clock. */
        STO_SV_EPOCH_CLOCK(line -> true,
                           (line, pi) -> {

                               pi.sto.setDefinedTimeSystem(TimeSystem.parseTwoLettersCode(RinexUtils.parseString(line, 24, 2)));
                               pi.sto.setReferenceTimeSystem(TimeSystem.parseTwoLettersCode(RinexUtils.parseString(line, 26, 2)));
                               final String sbas = RinexUtils.parseString(line, 43, 18);
                               pi.sto.setSbasId(sbas.length() > 0 ? SbasId.valueOf(sbas) : null);
                               final String utc = RinexUtils.parseString(line, 62, 18);
                               pi.sto.setUtcId(utc.length() > 0 ? UtcId.parseUtcId(utc) : null);

                               // TODO is the reference date relative to one or the other time scale?
                               final int year  = RinexUtils.parseInt(line, 4, 4);
                               final int month = RinexUtils.parseInt(line, 9, 2);
                               final int day   = RinexUtils.parseInt(line, 12, 2);
                               final int hours = RinexUtils.parseInt(line, 15, 2);
                               final int min   = RinexUtils.parseInt(line, 18, 2);
                               final int sec   = RinexUtils.parseInt(line, 21, 2);
                               pi.sto.setReferenceEpoch(new AbsoluteDate(year, month, day, hours, min, sec,
                                                                         pi.sto.getDefinedTimeSystem().getTimeScale(pi.timeScales)));

                           },
                           pi -> Collections.singleton(STO_LINE_1)),

        /** Parser for system time offset message type. */
        STO_TYPE(line -> line.startsWith("> STO"),
                 (line, pi) -> {
                     pi.closePendingMessage();
                     pi.sto = new SystemTimeOffsetMessage(SatelliteSystem.parseSatelliteSystem(RinexUtils.parseString(line, 6, 1)),
                                                          RinexUtils.parseInt(line, 7, 2),
                                                          RinexUtils.parseString(line, 10, 4));
                 },
                 pi -> Collections.singleton(STO_SV_EPOCH_CLOCK)),

        /** Parser for Earth orientation parameter message model. */
        EOP_LINE_2(line -> true,
                   (line, pi) -> {
                       pi.eop.setTransmissionTime(Unit.SECOND.toSI(RinexUtils.parseDouble(line,  4, 19)));
                       pi.eop.setDut1(Unit.SECOND.toSI(RinexUtils.parseDouble(line, 23, 19)));
                       pi.eop.setDut1Dot(S_PER_DAY.toSI(RinexUtils.parseDouble(line, 42, 19)));
                       pi.eop.setDut1DotDot(S_PER_DAY2.toSI(RinexUtils.parseDouble(line, 61, 19)));
                       pi.file.addEarthOrientationParameter(pi.eop);
                       pi.eop = null;
                   },
                   LineParser::navigationNext),

        /** Parser for Earth orientation parameter message model. */
        EOP_LINE_1(line -> true,
                   (line, pi) -> {
                       pi.eop.setYp(Unit.ARC_SECOND.toSI(RinexUtils.parseDouble(line, 23, 19)));
                       pi.eop.setYpDot(AS_PER_DAY.toSI(RinexUtils.parseDouble(line, 42, 19)));
                       pi.eop.setYpDotDot(AS_PER_DAY2.toSI(RinexUtils.parseDouble(line, 61, 19)));
                   },
                   pi -> Collections.singleton(EOP_LINE_2)),

        /** Parser for Earth orientation parameter message space vehicle epoch and clock. */
        EOP_SV_EPOCH_CLOCK(line -> true,
                           (line, pi) -> {
                               final int year  = RinexUtils.parseInt(line, 4, 4);
                               final int month = RinexUtils.parseInt(line, 9, 2);
                               final int day   = RinexUtils.parseInt(line, 12, 2);
                               final int hours = RinexUtils.parseInt(line, 15, 2);
                               final int min   = RinexUtils.parseInt(line, 18, 2);
                               final int sec   = RinexUtils.parseInt(line, 21, 2);
                               pi.eop.setReferenceEpoch(new AbsoluteDate(year, month, day, hours, min, sec,
                                                                         pi.eop.getSystem().getDefaultTimeSystem(pi.timeScales)));
                               pi.eop.setXp(Unit.ARC_SECOND.toSI(RinexUtils.parseDouble(line, 23, 19)));
                               pi.eop.setXpDot(AS_PER_DAY.toSI(RinexUtils.parseDouble(line, 42, 19)));
                               pi.eop.setXpDotDot(AS_PER_DAY2.toSI(RinexUtils.parseDouble(line, 61, 19)));
                           },
                           pi -> Collections.singleton(EOP_LINE_1)),

        /** Parser for Earth orientation parameter message type. */
        EOP_TYPE(line -> line.startsWith("> EOP"),
                 (line, pi) -> {
                     pi.closePendingMessage();
                     pi.eop = new EarthOrientationParameterMessage(SatelliteSystem.parseSatelliteSystem(RinexUtils.parseString(line, 6, 1)),
                                                                   RinexUtils.parseInt(line, 7, 2),
                                                                   RinexUtils.parseString(line, 10, 4));
                 },
                 pi -> Collections.singleton(EOP_SV_EPOCH_CLOCK)),

        /** Parser for ionosphere Klobuchar message model. */
        KLOBUCHAR_LINE_2(line -> true,
                         (line, pi) -> {
                             pi.klobuchar.setBetaI(3, IonosphereKlobucharMessage.S_PER_SC_N[3].toSI(RinexUtils.parseDouble(line,  4, 19)));
                             pi.klobuchar.setRegionCode(RinexUtils.parseDouble(line, 23, 19) < 0.5 ?
                                                        RegionCode.WIDE_AREA : RegionCode.JAPAN);
                             pi.file.addKlobucharMessage(pi.klobuchar);
                             pi.klobuchar = null;
                         },
                         LineParser::navigationNext),

        /** Parser for ionosphere Klobuchar message model. */
        KLOBUCHAR_LINE_1(line -> true,
                         (line, pi) -> {
                             pi.klobuchar.setAlphaI(3, IonosphereKlobucharMessage.S_PER_SC_N[3].toSI(RinexUtils.parseDouble(line,  4, 19)));
                             pi.klobuchar.setBetaI(0, IonosphereKlobucharMessage.S_PER_SC_N[0].toSI(RinexUtils.parseDouble(line, 23, 19)));
                             pi.klobuchar.setBetaI(1, IonosphereKlobucharMessage.S_PER_SC_N[1].toSI(RinexUtils.parseDouble(line, 42, 19)));
                             pi.klobuchar.setBetaI(2, IonosphereKlobucharMessage.S_PER_SC_N[2].toSI(RinexUtils.parseDouble(line, 61, 19)));
                         },
                         pi -> Collections.singleton(KLOBUCHAR_LINE_2)),

        /** Parser for ionosphere Klobuchar message model. */
        KLOBUCHAR_LINE_0(line -> true,
                         (line, pi) -> {
                             final int year  = RinexUtils.parseInt(line, 4, 4);
                             final int month = RinexUtils.parseInt(line, 9, 2);
                             final int day   = RinexUtils.parseInt(line, 12, 2);
                             final int hours = RinexUtils.parseInt(line, 15, 2);
                             final int min   = RinexUtils.parseInt(line, 18, 2);
                             final int sec   = RinexUtils.parseInt(line, 21, 2);
                             pi.klobuchar.setTransmitTime(new AbsoluteDate(year, month, day, hours, min, sec,
                                                                           pi.klobuchar.getSystem().getDefaultTimeSystem(pi.timeScales)));
                             pi.klobuchar.setAlphaI(0, IonosphereKlobucharMessage.S_PER_SC_N[0].toSI(RinexUtils.parseDouble(line, 23, 19)));
                             pi.klobuchar.setAlphaI(1, IonosphereKlobucharMessage.S_PER_SC_N[1].toSI(RinexUtils.parseDouble(line, 42, 19)));
                             pi.klobuchar.setAlphaI(2, IonosphereKlobucharMessage.S_PER_SC_N[2].toSI(RinexUtils.parseDouble(line, 61, 19)));
                         },
                         pi -> Collections.singleton(KLOBUCHAR_LINE_1)),

        /** Parser for ionosphere Nequick-G message model. */
        NEQUICK_LINE_1(line -> true,
                       (line, pi) -> {
                           pi.nequickG.setFlags((int) FastMath.rint(RinexUtils.parseDouble(line, 4, 19)));
                           pi.file.addNequickGMessage(pi.nequickG);
                           pi.nequickG = null;
                       },
                       LineParser::navigationNext),

        /** Parser for ionosphere Nequick-G message model. */
        NEQUICK_LINE_0(line -> true,
                       (line, pi) -> {
                           final int year  = RinexUtils.parseInt(line, 4, 4);
                           final int month = RinexUtils.parseInt(line, 9, 2);
                           final int day   = RinexUtils.parseInt(line, 12, 2);
                           final int hours = RinexUtils.parseInt(line, 15, 2);
                           final int min   = RinexUtils.parseInt(line, 18, 2);
                           final int sec   = RinexUtils.parseInt(line, 21, 2);
                           pi.nequickG.setTransmitTime(new AbsoluteDate(year, month, day, hours, min, sec,
                                                                        pi.nequickG.getSystem().getDefaultTimeSystem(pi.timeScales)));
                           pi.nequickG.setAi0(IonosphereNequickGMessage.SFU.toSI(RinexUtils.parseDouble(line, 23, 19)));
                           pi.nequickG.setAi1(IonosphereNequickGMessage.SFU_PER_DEG.toSI(RinexUtils.parseDouble(line, 42, 19)));
                           pi.nequickG.setAi2(IonosphereNequickGMessage.SFU_PER_DEG2.toSI(RinexUtils.parseDouble(line, 61, 19)));
                       },
                       pi -> Collections.singleton(NEQUICK_LINE_1)),

        /** Parser for ionosphere BDGIM message model. */
        BDGIM_LINE_2(line -> true,
                     (line, pi) -> {
                         pi.bdgim.setAlphaI(7, Unit.TOTAL_ELECTRON_CONTENT_UNIT.toSI(RinexUtils.parseDouble(line,  4, 19)));
                         pi.bdgim.setAlphaI(8, Unit.TOTAL_ELECTRON_CONTENT_UNIT.toSI(RinexUtils.parseDouble(line, 23, 19)));
                         pi.file.addBDGIMMessage(pi.bdgim);
                         pi.bdgim = null;
                     },
                     LineParser::navigationNext),

        /** Parser for ionosphere BDGIM message model. */
        BDGIM_LINE_1(line -> true,
                     (line, pi) -> {
                         pi.bdgim.setAlphaI(3, Unit.TOTAL_ELECTRON_CONTENT_UNIT.toSI(RinexUtils.parseDouble(line,  4, 19)));
                         pi.bdgim.setAlphaI(4, Unit.TOTAL_ELECTRON_CONTENT_UNIT.toSI(RinexUtils.parseDouble(line, 23, 19)));
                         pi.bdgim.setAlphaI(5, Unit.TOTAL_ELECTRON_CONTENT_UNIT.toSI(RinexUtils.parseDouble(line, 42, 19)));
                         pi.bdgim.setAlphaI(6, Unit.TOTAL_ELECTRON_CONTENT_UNIT.toSI(RinexUtils.parseDouble(line, 61, 19)));
                     },
                     pi -> Collections.singleton(BDGIM_LINE_2)),

        /** Parser for ionosphere BDGIM message model. */
        BDGIM_LINE_0(line -> true,
                     (line, pi) -> {
                         final int year  = RinexUtils.parseInt(line, 4, 4);
                         final int month = RinexUtils.parseInt(line, 9, 2);
                         final int day   = RinexUtils.parseInt(line, 12, 2);
                         final int hours = RinexUtils.parseInt(line, 15, 2);
                         final int min   = RinexUtils.parseInt(line, 18, 2);
                         final int sec   = RinexUtils.parseInt(line, 21, 2);
                         pi.bdgim.setTransmitTime(new AbsoluteDate(year, month, day, hours, min, sec,
                                                                   pi.bdgim.getSystem().getDefaultTimeSystem(pi.timeScales)));
                         pi.bdgim.setAlphaI(0, Unit.TOTAL_ELECTRON_CONTENT_UNIT.toSI(RinexUtils.parseDouble(line, 23, 19)));
                         pi.bdgim.setAlphaI(1, Unit.TOTAL_ELECTRON_CONTENT_UNIT.toSI(RinexUtils.parseDouble(line, 42, 19)));
                         pi.bdgim.setAlphaI(2, Unit.TOTAL_ELECTRON_CONTENT_UNIT.toSI(RinexUtils.parseDouble(line, 61, 19)));
                     },
                     pi -> Collections.singleton(BDGIM_LINE_1)),

        /** Parser for ionosphere message type. */
        IONO_TYPE(line -> line.startsWith("> ION"),
                  (line, pi) -> {
                      pi.closePendingMessage();
                      final SatelliteSystem system = SatelliteSystem.parseSatelliteSystem(RinexUtils.parseString(line, 6, 1));
                      final int             prn    = RinexUtils.parseInt(line, 7, 2);
                      final String          type   = RinexUtils.parseString(line, 10, 4);
                      if (system == SatelliteSystem.GALILEO) {
                          pi.nequickG = new IonosphereNequickGMessage(system, prn, type);
                      } else {
                          // in Rinex 4.00, tables A32 and A34 are ambiguous as both seem to apply
                          // to Beidou CNVX messages, we consider BDGIM is the proper model in this case
                          if (system == SatelliteSystem.BEIDOU && "CNVX".equals(type)) {
                              pi.bdgim = new IonosphereBDGIMMessage(system, prn, type);
                          } else {
                              pi.klobuchar = new IonosphereKlobucharMessage(system, prn, type);
                          }
                      }
                  },
                  pi -> Collections.singleton(pi.nequickG != null ? NEQUICK_LINE_0 : (pi.bdgim != null ? BDGIM_LINE_0 : KLOBUCHAR_LINE_0)));

        /** Predicate for identifying lines that can be parsed. */
        private final Predicate<String> canHandle;

        /** Parsing method. */
        private final ParsingMethod parsingMethod;

        /** Provider for next line parsers. */
        private final Function<ParseInfo, Iterable<LineParser>> allowedNextProvider;

        /** Simple constructor.
         * @param canHandle predicate for identifying lines that can be parsed
         * @param parsingMethod parsing method
         * @param allowedNextProvider supplier for allowed parsers for next line
         */
        LineParser(final Predicate<String> canHandle, final ParsingMethod parsingMethod,
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
            if (parseInfo.file.getHeader().getFormatVersion() < 4) {
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
                parseInfo.qzssCNav   != null || parseInfo.irnssNav   != null || parseInfo.sbasNav    != null) {
                return Collections.singleton(BROADCAST_ORBIT);
            } else if (parseInfo.glonassNav != null) {
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
            } else if (parseInfo.file.getHeader().getFormatVersion() < 4) {
                return Collections.singleton(NAVIGATION_SV_EPOCH_CLOCK);
            } else {
                return Arrays.asList(EPH_TYPE, STO_TYPE, EOP_TYPE, IONO_TYPE);
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
                parseSvEpochSvClockLine(line, pi.timeScales.getGPS(), pi.gpsLNav);
            }

            /** {@inheritDoc} */
            @Override
            public void parseFirstBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.gpsLNav.setIODE(parseBroadcastDouble1(line,   Unit.SECOND));
                pi.gpsLNav.setCrs(parseBroadcastDouble2(line,    Unit.METRE));
                pi.gpsLNav.setDeltaN(parseBroadcastDouble3(line, RAD_PER_S));
                pi.gpsLNav.setM0(parseBroadcastDouble4(line,     Unit.RADIAN));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSecondBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.gpsLNav.setCuc(parseBroadcastDouble1(line,   Unit.RADIAN));
                pi.gpsLNav.setE(parseBroadcastDouble2(line,     Unit.NONE));
                pi.gpsLNav.setCus(parseBroadcastDouble3(line,   Unit.RADIAN));
                pi.gpsLNav.setSqrtA(parseBroadcastDouble4(line, SQRT_M));
            }

            /** {@inheritDoc} */
            @Override
            public void parseThirdBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.gpsLNav.setTime(parseBroadcastDouble1(line,   Unit.SECOND));
                pi.gpsLNav.setCic(parseBroadcastDouble2(line,    Unit.RADIAN));
                pi.gpsLNav.setOmega0(parseBroadcastDouble3(line, Unit.RADIAN));
                pi.gpsLNav.setCis(parseBroadcastDouble4(line,    Unit.RADIAN));
            }

            /** {@inheritDoc} */
            @Override
            public void parseFourthBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.gpsLNav.setI0(parseBroadcastDouble1(line,       Unit.RADIAN));
                pi.gpsLNav.setCrc(parseBroadcastDouble2(line,      Unit.METRE));
                pi.gpsLNav.setPa(parseBroadcastDouble3(line,       Unit.RADIAN));
                pi.gpsLNav.setOmegaDot(parseBroadcastDouble4(line, RAD_PER_S));
            }

            /** {@inheritDoc} */
            @Override
            public void parseFifthBroadcastOrbit(final String line, final ParseInfo pi) {
                // iDot
                pi.gpsLNav.setIDot(parseBroadcastDouble1(line, RAD_PER_S));
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
                pi.gpsLNav.setSvAccuracy(parseBroadcastDouble1(line, Unit.METRE));
                pi.gpsLNav.setSvHealth(parseBroadcastInt2(line));
                pi.gpsLNav.setTGD(parseBroadcastDouble3(line,        Unit.SECOND));
                pi.gpsLNav.setIODC(parseBroadcastDouble4(line,       Unit.SECOND));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSeventhBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.gpsLNav.setTransmissionTime(parseBroadcastDouble1(line, Unit.SECOND));
                pi.gpsLNav.setFitInterval(parseBroadcastInt2(line));
                pi.closePendingMessage();
            }

            /** {@inheritDoc} */
            @Override
            public void closeMessage(final ParseInfo pi) {
                if (pi.gpsLNav != null) {
                    pi.file.addGPSLegacyNavigationMessage(pi.gpsLNav);
                    pi.gpsLNav = null;
                }
            }

        },

        /** GPS civilian.
         * @since 12.0
         */
        GPS_CNAV() {

            /** {@inheritDoc} */
            @Override
            public void parseSvEpochSvClockLine(final String line, final ParseInfo pi) {
                parseSvEpochSvClockLine(line, pi.timeScales.getGPS(), pi.gpsCNav);
            }

            /** {@inheritDoc} */
            @Override
            public void parseFirstBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.gpsCNav.setADot(parseBroadcastDouble1(line,   M_PER_S));
                pi.gpsCNav.setCrs(parseBroadcastDouble2(line,    Unit.METRE));
                pi.gpsCNav.setDeltaN(parseBroadcastDouble3(line, RAD_PER_S));
                pi.gpsCNav.setM0(parseBroadcastDouble4(line,     Unit.RADIAN));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSecondBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.gpsCNav.setCuc(parseBroadcastDouble1(line,   Unit.RADIAN));
                pi.gpsCNav.setE(parseBroadcastDouble2(line,     Unit.NONE));
                pi.gpsCNav.setCus(parseBroadcastDouble3(line,   Unit.RADIAN));
                pi.gpsCNav.setSqrtA(parseBroadcastDouble4(line, SQRT_M));
            }

            /** {@inheritDoc} */
            @Override
            public void parseThirdBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.gpsCNav.setTime(parseBroadcastDouble1(line,   Unit.SECOND));
                pi.gpsCNav.setCic(parseBroadcastDouble2(line,    Unit.RADIAN));
                pi.gpsCNav.setOmega0(parseBroadcastDouble3(line, Unit.RADIAN));
                pi.gpsCNav.setCis(parseBroadcastDouble4(line,    Unit.RADIAN));
            }

            /** {@inheritDoc} */
            @Override
            public void parseFourthBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.gpsCNav.setI0(parseBroadcastDouble1(line,       Unit.RADIAN));
                pi.gpsCNav.setCrc(parseBroadcastDouble2(line,      Unit.METRE));
                pi.gpsCNav.setPa(parseBroadcastDouble3(line,       Unit.RADIAN));
                pi.gpsCNav.setOmegaDot(parseBroadcastDouble4(line, RAD_PER_S));
            }

            /** {@inheritDoc} */
            @Override
            public void parseFifthBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.gpsCNav.setIDot(parseBroadcastDouble1(line,       RAD_PER_S));
                pi.gpsCNav.setDeltaN0Dot(parseBroadcastDouble2(line, RAD_PER_S2));
                pi.gpsCNav.setUraiNed0(parseBroadcastInt3(line));
                pi.gpsCNav.setUraiNed1(parseBroadcastInt4(line));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSixthBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.gpsCNav.setUraiEd(parseBroadcastInt1(line));
                pi.gpsCNav.setSvHealth(parseBroadcastInt2(line));
                pi.gpsCNav.setTGD(parseBroadcastDouble3(line, Unit.SECOND));
                pi.gpsCNav.setUraiNed2(parseBroadcastInt4(line));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSeventhBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.gpsCNav.setIscL1CA(parseBroadcastDouble1(line, Unit.SECOND));
                pi.gpsCNav.setIscL2C(parseBroadcastDouble2(line,  Unit.SECOND));
                pi.gpsCNav.setIscL5I5(parseBroadcastDouble3(line, Unit.SECOND));
                pi.gpsCNav.setIscL5Q5(parseBroadcastDouble4(line, Unit.SECOND));
            }

            /** {@inheritDoc} */
            @Override
            public void parseEighthBroadcastOrbit(final String line, final ParseInfo pi) {
                if (pi.gpsCNav.isCnv2()) {
                    // in CNAV2 messages, there is an additional line for L1 CD and L1 CP inter signal delay
                    pi.gpsCNav.setIscL1CD(parseBroadcastDouble1(line, Unit.SECOND));
                    pi.gpsCNav.setIscL1CP(parseBroadcastDouble2(line, Unit.SECOND));
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
                pi.gpsCNav.setTransmissionTime(parseBroadcastDouble1(line, Unit.SECOND));
                pi.closePendingMessage();
            }

            /** {@inheritDoc} */
            @Override
            public void closeMessage(final ParseInfo pi) {
                if (pi.gpsCNav != null) {
                    pi.file.addGPSLegacyNavigationMessage(pi.gpsCNav);
                    pi.gpsCNav = null;
                }
            }

        },

        /** Galileo. */
        GALILEO() {

            /** {@inheritDoc} */
            @Override
            public void parseSvEpochSvClockLine(final String line, final ParseInfo pi) {
                parseSvEpochSvClockLine(line, pi.timeScales.getGPS(), pi.galileoNav);
            }

            /** {@inheritDoc} */
            @Override
            public void parseFirstBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.galileoNav.setIODNav(parseBroadcastInt1(line));
                pi.galileoNav.setCrs(parseBroadcastDouble2(line,       Unit.METRE));
                pi.galileoNav.setDeltaN(parseBroadcastDouble3(line,    RAD_PER_S));
                pi.galileoNav.setM0(parseBroadcastDouble4(line,        Unit.RADIAN));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSecondBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.galileoNav.setCuc(parseBroadcastDouble1(line,   Unit.RADIAN));
                pi.galileoNav.setE(parseBroadcastDouble2(line,     Unit.NONE));
                pi.galileoNav.setCus(parseBroadcastDouble3(line,   Unit.RADIAN));
                pi.galileoNav.setSqrtA(parseBroadcastDouble4(line, SQRT_M));
            }

            /** {@inheritDoc} */
            @Override
            public void parseThirdBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.galileoNav.setTime(parseBroadcastDouble1(line,   Unit.SECOND));
                pi.galileoNav.setCic(parseBroadcastDouble2(line,    Unit.RADIAN));
                pi.galileoNav.setOmega0(parseBroadcastDouble3(line, Unit.RADIAN));
                pi.galileoNav.setCis(parseBroadcastDouble4(line,    Unit.RADIAN));
            }

            /** {@inheritDoc} */
            @Override
            public void parseFourthBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.galileoNav.setI0(parseBroadcastDouble1(line,       Unit.RADIAN));
                pi.galileoNav.setCrc(parseBroadcastDouble2(line,      Unit.METRE));
                pi.galileoNav.setPa(parseBroadcastDouble3(line,       Unit.RADIAN));
                pi.galileoNav.setOmegaDot(parseBroadcastDouble4(line, RAD_PER_S));
            }

            /** {@inheritDoc} */
            @Override
            public void parseFifthBroadcastOrbit(final String line, final ParseInfo pi) {
                // iDot
                pi.galileoNav.setIDot(parseBroadcastDouble1(line, RAD_PER_S));
                pi.galileoNav.setDataSource(parseBroadcastInt2(line));
                // GAL week (to go with Toe)
                pi.galileoNav.setWeek(parseBroadcastInt3(line));
                pi.galileoNav.setDate(new GNSSDate(pi.galileoNav.getWeek(),
                                                   pi.galileoNav.getTime(),
                                                   SatelliteSystem.GPS, // in Rinex files, week number is aligned to GPS week!
                                                   pi.timeScales).getDate());
            }

            /** {@inheritDoc} */
            @Override
            public void parseSixthBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.galileoNav.setSisa(parseBroadcastDouble1(line,     Unit.METRE));
                pi.galileoNav.setSvHealth(parseBroadcastDouble2(line, Unit.NONE));
                pi.galileoNav.setBGDE1E5a(parseBroadcastDouble3(line, Unit.SECOND));
                pi.galileoNav.setBGDE5bE1(parseBroadcastDouble4(line, Unit.SECOND));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSeventhBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.galileoNav.setTransmissionTime(parseBroadcastDouble1(line, Unit.SECOND));
                pi.closePendingMessage();
            }

            /** {@inheritDoc} */
            @Override
            public void closeMessage(final ParseInfo pi) {
                if (pi.galileoNav != null) {
                    pi.file.addGalileoNavigationMessage(pi.galileoNav);
                    pi.galileoNav = null;
                }
            }

        },

        /** Glonass. */
        GLONASS() {

            /** {@inheritDoc} */
            @Override
            public void parseSvEpochSvClockLine(final String line, final ParseInfo pi) {

                pi.glonassNav.setPRN(parsePrnSvEpochClock(line));

                // Toc
                pi.glonassNav.setEpochToc(parsePrnSvEpochClock(line, pi.timeScales.getUTC()));

                // clock (it's not really a broadcast orbit line, but the fields are at the same indices…)
                pi.glonassNav.setTauN(parseBroadcastDouble2(line, MINUS_SECONDS));
                pi.glonassNav.setGammaN(parseBroadcastDouble3(line, Unit.NONE));
                pi.glonassNav.setTime(fmod(parseBroadcastDouble4(line, Unit.NONE), Constants.JULIAN_DAY));

                // Set the ephemeris epoch (same as time of clock epoch)
                pi.glonassNav.setDate(pi.glonassNav.getEpochToc());

            }

            /** {@inheritDoc} */
            @Override
            public void parseFirstBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.glonassNav.setX(parseBroadcastDouble1(line,       KM));
                pi.glonassNav.setXDot(parseBroadcastDouble2(line,    KM_PER_S));
                pi.glonassNav.setXDotDot(parseBroadcastDouble3(line, KM_PER_S2));
                pi.glonassNav.setHealth(parseBroadcastDouble4(line,  Unit.NONE));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSecondBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.glonassNav.setY(parseBroadcastDouble1(line,               KM));
                pi.glonassNav.setYDot(parseBroadcastDouble2(line,            KM_PER_S));
                pi.glonassNav.setYDotDot(parseBroadcastDouble3(line,         KM_PER_S2));
                pi.glonassNav.setFrequencyNumber(parseBroadcastDouble4(line, Unit.NONE));
            }

            /** {@inheritDoc} */
            @Override
            public void parseThirdBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.glonassNav.setZ(parseBroadcastDouble1(line,       KM));
                pi.glonassNav.setZDot(parseBroadcastDouble2(line,    KM_PER_S));
                pi.glonassNav.setZDotDot(parseBroadcastDouble3(line, KM_PER_S2));
                if (pi.file.getHeader().getFormatVersion() < 3.045) {
                    pi.closePendingMessage();
                }
            }

            /** {@inheritDoc} */
            @Override
            public void parseFourthBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.glonassNav.setStatusFlags(parseBroadcastDouble1(line,          Unit.NONE));
                pi.glonassNav.setGroupDelayDifference(parseBroadcastDouble2(line, Unit.NONE));
                pi.glonassNav.setURA(parseBroadcastDouble3(line,                  Unit.NONE));
                pi.glonassNav.setHealthFlags(parseBroadcastDouble4(line,          Unit.NONE));
                pi.closePendingMessage();
            }

            /** {@inheritDoc} */
            @Override
            public void closeMessage(final ParseInfo pi) {
                if (pi.glonassNav != null) {
                    pi.file.addGlonassNavigationMessage(pi.glonassNav);
                    pi.glonassNav = null;
                }
            }

        },

        /** QZSS legacy. */
        QZSS_LNAV() {

            /** {@inheritDoc} */
            @Override
            public void parseSvEpochSvClockLine(final String line, final ParseInfo pi) {
                parseSvEpochSvClockLine(line, pi.timeScales.getGPS(), pi.qzssLNav);
            }

            /** {@inheritDoc} */
            @Override
            public void parseFirstBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.qzssLNav.setIODE(parseBroadcastDouble1(line,   Unit.SECOND));
                pi.qzssLNav.setCrs(parseBroadcastDouble2(line,    Unit.METRE));
                pi.qzssLNav.setDeltaN(parseBroadcastDouble3(line, RAD_PER_S));
                pi.qzssLNav.setM0(parseBroadcastDouble4(line,     Unit.RADIAN));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSecondBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.qzssLNav.setCuc(parseBroadcastDouble1(line,   Unit.RADIAN));
                pi.qzssLNav.setE(parseBroadcastDouble2(line,     Unit.NONE));
                pi.qzssLNav.setCus(parseBroadcastDouble3(line,   Unit.RADIAN));
                pi.qzssLNav.setSqrtA(parseBroadcastDouble4(line, SQRT_M));
            }

            /** {@inheritDoc} */
            @Override
            public void parseThirdBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.qzssLNav.setTime(parseBroadcastDouble1(line,   Unit.SECOND));
                pi.qzssLNav.setCic(parseBroadcastDouble2(line,    Unit.RADIAN));
                pi.qzssLNav.setOmega0(parseBroadcastDouble3(line, Unit.RADIAN));
                pi.qzssLNav.setCis(parseBroadcastDouble4(line,    Unit.RADIAN));
            }

            /** {@inheritDoc} */
            @Override
            public void parseFourthBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.qzssLNav.setI0(parseBroadcastDouble1(line,       Unit.RADIAN));
                pi.qzssLNav.setCrc(parseBroadcastDouble2(line,      Unit.METRE));
                pi.qzssLNav.setPa(parseBroadcastDouble3(line,       Unit.RADIAN));
                pi.qzssLNav.setOmegaDot(parseBroadcastDouble4(line, RAD_PER_S));
            }

            /** {@inheritDoc} */
            @Override
            public void parseFifthBroadcastOrbit(final String line, final ParseInfo pi) {
                // iDot
                pi.qzssLNav.setIDot(parseBroadcastDouble1(line, RAD_PER_S));
                // Codes on L2 channel (ignored)
                // RinexUtils.parseDouble(line, 23, 19)
                // GPS week (to go with Toe)
                pi.qzssLNav.setWeek(parseBroadcastInt3(line));
                pi.qzssLNav.setDate(new GNSSDate(pi.qzssLNav.getWeek(),
                                                 pi.qzssLNav.getTime(),
                                                 SatelliteSystem.GPS, // in Rinex files, week number is aligned to GPS week!
                                                 pi.timeScales).getDate());
            }

            /** {@inheritDoc} */
            @Override
            public void parseSixthBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.qzssLNav.setSvAccuracy(parseBroadcastDouble1(line,  Unit.METRE));
                pi.qzssLNav.setSvHealth(parseBroadcastInt2(line));
                pi.qzssLNav.setTGD(parseBroadcastDouble3(line,         Unit.SECOND));
                pi.qzssLNav.setIODC(parseBroadcastDouble4(line,        Unit.SECOND));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSeventhBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.qzssLNav.setTransmissionTime(parseBroadcastDouble1(line, Unit.SECOND));
                pi.qzssLNav.setFitInterval(parseBroadcastInt2(line));
                pi.closePendingMessage();
            }

            /** {@inheritDoc} */
            @Override
            public void closeMessage(final ParseInfo pi) {
                if (pi.qzssLNav != null) {
                    pi.file.addQZSSLegacyNavigationMessage(pi.qzssLNav);
                    pi.qzssLNav = null;
                }
            }

        },

        /** QZSS civilian.
         * @since 12.0
         */
        QZSS_CNAV() {

            /** {@inheritDoc} */
            @Override
            public void parseSvEpochSvClockLine(final String line, final ParseInfo pi) {
                parseSvEpochSvClockLine(line, pi.timeScales.getGPS(), pi.qzssCNav);
            }

            /** {@inheritDoc} */
            @Override
            public void parseFirstBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.qzssCNav.setADot(parseBroadcastDouble1(line,   M_PER_S));
                pi.qzssCNav.setCrs(parseBroadcastDouble2(line,    Unit.METRE));
                pi.qzssCNav.setDeltaN(parseBroadcastDouble3(line, RAD_PER_S));
                pi.qzssCNav.setM0(parseBroadcastDouble4(line,     Unit.RADIAN));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSecondBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.qzssCNav.setCuc(parseBroadcastDouble1(line,   Unit.RADIAN));
                pi.qzssCNav.setE(parseBroadcastDouble2(line,     Unit.NONE));
                pi.qzssCNav.setCus(parseBroadcastDouble3(line,   Unit.RADIAN));
                pi.qzssCNav.setSqrtA(parseBroadcastDouble4(line, SQRT_M));
            }

            /** {@inheritDoc} */
            @Override
            public void parseThirdBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.qzssCNav.setTime(parseBroadcastDouble1(line,   Unit.SECOND));
                pi.qzssCNav.setCic(parseBroadcastDouble2(line,    Unit.RADIAN));
                pi.qzssCNav.setOmega0(parseBroadcastDouble3(line, Unit.RADIAN));
                pi.qzssCNav.setCis(parseBroadcastDouble4(line,    Unit.RADIAN));
            }

            /** {@inheritDoc} */
            @Override
            public void parseFourthBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.qzssCNav.setI0(parseBroadcastDouble1(line,       Unit.RADIAN));
                pi.qzssCNav.setCrc(parseBroadcastDouble2(line,      Unit.METRE));
                pi.qzssCNav.setPa(parseBroadcastDouble3(line,       Unit.RADIAN));
                pi.qzssCNav.setOmegaDot(parseBroadcastDouble4(line, RAD_PER_S));
            }

            /** {@inheritDoc} */
            @Override
            public void parseFifthBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.qzssCNav.setIDot(parseBroadcastDouble1(line,       RAD_PER_S));
                pi.qzssCNav.setDeltaN0Dot(parseBroadcastDouble2(line, RAD_PER_S2));
                pi.qzssCNav.setUraiNed0(parseBroadcastInt3(line));
                pi.qzssCNav.setUraiNed1(parseBroadcastInt4(line));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSixthBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.qzssCNav.setUraiEd(parseBroadcastInt1(line));
                pi.qzssCNav.setSvHealth(parseBroadcastInt2(line));
                pi.qzssCNav.setTGD(parseBroadcastDouble3(line, Unit.SECOND));
                pi.qzssCNav.setUraiNed2(parseBroadcastInt4(line));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSeventhBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.qzssCNav.setIscL1CA(parseBroadcastDouble1(line, Unit.SECOND));
                pi.qzssCNav.setIscL2C(parseBroadcastDouble2(line,  Unit.SECOND));
                pi.qzssCNav.setIscL5I5(parseBroadcastDouble3(line, Unit.SECOND));
                pi.qzssCNav.setIscL5Q5(parseBroadcastDouble4(line, Unit.SECOND));
            }

            /** {@inheritDoc} */
            @Override
            public void parseEighthBroadcastOrbit(final String line, final ParseInfo pi) {
                if (pi.qzssCNav.isCnv2()) {
                    // in CNAV2 messages, there is an additional line for L1 CD and L1 CP inter signal delay
                    pi.qzssCNav.setIscL1CD(parseBroadcastDouble1(line, Unit.SECOND));
                    pi.qzssCNav.setIscL1CP(parseBroadcastDouble2(line, Unit.SECOND));
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
                pi.qzssCNav.setTransmissionTime(parseBroadcastDouble1(line, Unit.SECOND));
                pi.closePendingMessage();
            }

            /** {@inheritDoc} */
            @Override
            public void closeMessage(final ParseInfo pi) {
                if (pi.qzssCNav != null) {
                    pi.file.addQZSSCivilianNavigationMessage(pi.qzssCNav);
                    pi.qzssCNav = null;
                }
            }

        },

        /** Beidou legacy. */
        BEIDOU_D1_D2() {

            /** {@inheritDoc} */
            @Override
            public void parseSvEpochSvClockLine(final String line, final ParseInfo pi) {
                parseSvEpochSvClockLine(line, pi.timeScales.getBDT(), pi.beidouLNav);
            }

            /** {@inheritDoc} */
            @Override
            public void parseFirstBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.beidouLNav.setAODE(parseBroadcastDouble1(line,   Unit.SECOND));
                pi.beidouLNav.setCrs(parseBroadcastDouble2(line,    Unit.METRE));
                pi.beidouLNav.setDeltaN(parseBroadcastDouble3(line, RAD_PER_S));
                pi.beidouLNav.setM0(parseBroadcastDouble4(line,     Unit.RADIAN));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSecondBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.beidouLNav.setCuc(parseBroadcastDouble1(line,   Unit.RADIAN));
                pi.beidouLNav.setE(parseBroadcastDouble2(line,     Unit.NONE));
                pi.beidouLNav.setCus(parseBroadcastDouble3(line,   Unit.RADIAN));
                pi.beidouLNav.setSqrtA(parseBroadcastDouble4(line, SQRT_M));
            }

            /** {@inheritDoc} */
            @Override
            public void parseThirdBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.beidouLNav.setTime(parseBroadcastDouble1(line,   Unit.SECOND));
                pi.beidouLNav.setCic(parseBroadcastDouble2(line,    Unit.RADIAN));
                pi.beidouLNav.setOmega0(parseBroadcastDouble3(line, Unit.RADIAN));
                pi.beidouLNav.setCis(parseBroadcastDouble4(line,    Unit.RADIAN));
            }

            /** {@inheritDoc} */
            @Override
            public void parseFourthBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.beidouLNav.setI0(parseBroadcastDouble1(line,       Unit.RADIAN));
                pi.beidouLNav.setCrc(parseBroadcastDouble2(line,      Unit.METRE));
                pi.beidouLNav.setPa(parseBroadcastDouble3(line,       Unit.RADIAN));
                pi.beidouLNav.setOmegaDot(parseBroadcastDouble4(line, RAD_PER_S));
            }

            /** {@inheritDoc} */
            @Override
            public void parseFifthBroadcastOrbit(final String line, final ParseInfo pi) {
                // iDot
                pi.beidouLNav.setIDot(parseBroadcastDouble1(line, RAD_PER_S));
                // BDT week (to go with Toe)
                pi.beidouLNav.setWeek(parseBroadcastInt3(line));
                pi.beidouLNav.setDate(new GNSSDate(pi.beidouLNav.getWeek(),
                                                   pi.beidouLNav.getTime(),
                                                   SatelliteSystem.BEIDOU,
                                                   pi.timeScales).getDate());
            }

            /** {@inheritDoc} */
            @Override
            public void parseSixthBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.beidouLNav.setSvAccuracy(parseBroadcastDouble1(line, Unit.METRE));
                // TODO SatH1
                pi.beidouLNav.setTGD1(parseBroadcastDouble3(line,       Unit.SECOND));
                pi.beidouLNav.setTGD2(parseBroadcastDouble4(line,       Unit.SECOND));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSeventhBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.beidouLNav.setTransmissionTime(parseBroadcastDouble1(line, Unit.SECOND));
                pi.beidouLNav.setAODC(parseBroadcastDouble2(line,             Unit.SECOND));
                pi.closePendingMessage();
            }

            /** {@inheritDoc} */
            @Override
            public void closeMessage(final ParseInfo pi) {
                if (pi.beidouLNav != null) {
                    pi.file.addBeidouLegacyNavigationMessage(pi.beidouLNav);
                    pi.beidouLNav = null;
                }
            }

        },

        /** Beidou-3 CNAV. */
        BEIDOU_CNV_123() {

            /** {@inheritDoc} */
            @Override
            public void parseSvEpochSvClockLine(final String line, final ParseInfo pi) {
                parseSvEpochSvClockLine(line, pi.timeScales.getBDT(), pi.beidouCNav);
            }

            /** {@inheritDoc} */
            @Override
            public void parseFirstBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.beidouCNav.setADot(parseBroadcastDouble1(line,   M_PER_S));
                pi.beidouCNav.setCrs(parseBroadcastDouble2(line,    Unit.METRE));
                pi.beidouCNav.setDeltaN(parseBroadcastDouble3(line, RAD_PER_S));
                pi.beidouCNav.setM0(parseBroadcastDouble4(line,     Unit.RADIAN));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSecondBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.beidouCNav.setCuc(parseBroadcastDouble1(line,   Unit.RADIAN));
                pi.beidouCNav.setE(parseBroadcastDouble2(line,     Unit.NONE));
                pi.beidouCNav.setCus(parseBroadcastDouble3(line,   Unit.RADIAN));
                pi.beidouCNav.setSqrtA(parseBroadcastDouble4(line, SQRT_M));
            }

            /** {@inheritDoc} */
            @Override
            public void parseThirdBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.beidouCNav.setTime(parseBroadcastDouble1(line,   Unit.SECOND));
                pi.beidouCNav.setCic(parseBroadcastDouble2(line,    Unit.RADIAN));
                pi.beidouCNav.setOmega0(parseBroadcastDouble3(line, Unit.RADIAN));
                pi.beidouCNav.setCis(parseBroadcastDouble4(line,    Unit.RADIAN));
            }

            /** {@inheritDoc} */
            @Override
            public void parseFourthBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.beidouCNav.setI0(parseBroadcastDouble1(line,       Unit.RADIAN));
                pi.beidouCNav.setCrc(parseBroadcastDouble2(line,      Unit.METRE));
                pi.beidouCNav.setPa(parseBroadcastDouble3(line,       Unit.RADIAN));
                pi.beidouCNav.setOmegaDot(parseBroadcastDouble4(line, RAD_PER_S));
            }

            /** {@inheritDoc} */
            @Override
            public void parseFifthBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.beidouCNav.setIDot(parseBroadcastDouble1(line,       RAD_PER_S));
                pi.beidouCNav.setDeltaN0Dot(parseBroadcastDouble2(line, RAD_PER_S2));
                switch (parseBroadcastInt3(line)) {
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
                pi.beidouCNav.setTime(parseBroadcastDouble4(line,     Unit.SECOND));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSixthBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.beidouCNav.setSisaiOe(parseBroadcastInt1(line));
                pi.beidouCNav.setSisaiOcb(parseBroadcastInt2(line));
                pi.beidouCNav.setSisaiOc1(parseBroadcastInt3(line));
                pi.beidouCNav.setSisaiOc2(parseBroadcastInt4(line));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSeventhBroadcastOrbit(final String line, final ParseInfo pi) {
                if (pi.beidouCNav.getSignal() == Frequency.B1C) {
                    pi.beidouCNav.setIscB1CD(parseBroadcastDouble1(line, Unit.SECOND));
                    // field 2 is spare
                    pi.beidouCNav.setTgdB1Cp(parseBroadcastDouble3(line, Unit.SECOND));
                    pi.beidouCNav.setTgdB2ap(parseBroadcastDouble4(line, Unit.SECOND));
                } else if (pi.beidouCNav.getSignal() == Frequency.B2A) {
                    // field 1 is spare
                    pi.beidouCNav.setIscB2AD(parseBroadcastDouble2(line, Unit.SECOND));
                    pi.beidouCNav.setTgdB1Cp(parseBroadcastDouble3(line, Unit.SECOND));
                    pi.beidouCNav.setTgdB2ap(parseBroadcastDouble4(line, Unit.SECOND));
                } else {
                    parseSismaiHealthIntegrity(line, pi);
                }
            }

            /** {@inheritDoc} */
            @Override
            public void parseEighthBroadcastOrbit(final String line, final ParseInfo pi) {
                if (pi.beidouCNav.getSignal() == Frequency.B2B) {
                    pi.beidouCNav.setTransmissionTime(parseBroadcastDouble1(line, Unit.SECOND));
                    pi.file.addBeidouCivilianNavigationMessage(pi.beidouCNav);
                    pi.beidouCNav = null;
                } else {
                    parseSismaiHealthIntegrity(line, pi);
                }
            }

            /** {@inheritDoc} */
            @Override
            public void parseNinthBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.beidouCNav.setTransmissionTime(parseBroadcastDouble1(line,  Unit.SECOND));
                // field 2 is spare
                // field 3 is spare
                pi.beidouCNav.setIODE(parseBroadcastInt4(line));
                pi.closePendingMessage();
            }

            /** {@inheritDoc} */
            @Override
            public void closeMessage(final ParseInfo pi) {
                if (pi.beidouCNav != null) {
                    pi.file.addBeidouCivilianNavigationMessage(pi.beidouCNav);
                    pi.beidouCNav = null;
                }
            }

            /**
             * Parse the SISMAI/Health/integrity line.
             * @param line line to read
             * @param pi holder for transient data
             */
            private void parseSismaiHealthIntegrity(final String line, final ParseInfo pi) {
                pi.beidouCNav.setSismai(parseBroadcastInt1(line));
                pi.beidouCNav.setHealth(parseBroadcastInt2(line));
                pi.beidouCNav.setIntegrityFlags(parseBroadcastInt3(line));
                pi.beidouCNav.setIODC(parseBroadcastInt4(line));
            }

        },

        /** SBAS. */
        SBAS() {

            /** {@inheritDoc} */
            @Override
            public void parseSvEpochSvClockLine(final String line, final ParseInfo pi) {

                // parse PRN
                pi.sbasNav.setPRN(parsePrnSvEpochClock(line));

                // Time scale (UTC for Rinex 3.01 and GPS for other RINEX versions)
                final int       version100 = (int) FastMath.rint(pi.file.getHeader().getFormatVersion() * 100);
                final TimeScale timeScale  = (version100 == 301) ? pi.timeScales.getUTC() : pi.timeScales.getGPS();

                pi.sbasNav.setEpochToc(parsePrnSvEpochClock(line, timeScale));
                pi.sbasNav.setAGf0(parseBroadcastDouble2(line, Unit.SECOND));
                pi.sbasNav.setAGf1(parseBroadcastDouble3(line, S_PER_S));
                pi.sbasNav.setTime(parseBroadcastDouble4(line, Unit.SECOND));

                // Set the ephemeris epoch (same as time of clock epoch)
                pi.sbasNav.setDate(pi.sbasNav.getEpochToc());

            }

            /** {@inheritDoc} */
            @Override
            public void parseFirstBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.sbasNav.setX(parseBroadcastDouble1(line,       KM));
                pi.sbasNav.setXDot(parseBroadcastDouble2(line,    KM_PER_S));
                pi.sbasNav.setXDotDot(parseBroadcastDouble3(line, KM_PER_S2));
                pi.sbasNav.setHealth(parseBroadcastDouble4(line,  Unit.NONE));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSecondBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.sbasNav.setY(parseBroadcastDouble1(line,       KM));
                pi.sbasNav.setYDot(parseBroadcastDouble2(line,    KM_PER_S));
                pi.sbasNav.setYDotDot(parseBroadcastDouble3(line, KM_PER_S2));
                pi.sbasNav.setURA(parseBroadcastDouble4(line,     Unit.NONE));
            }

            /** {@inheritDoc} */
            @Override
            public void parseThirdBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.sbasNav.setZ(parseBroadcastDouble1(line,       KM));
                pi.sbasNav.setZDot(parseBroadcastDouble2(line,    KM_PER_S));
                pi.sbasNav.setZDotDot(parseBroadcastDouble3(line, KM_PER_S2));
                pi.sbasNav.setIODN(parseBroadcastDouble4(line,    Unit.NONE));
                pi.closePendingMessage();
            }

            /** {@inheritDoc} */
            @Override
            public void closeMessage(final ParseInfo pi) {
                if (pi.sbasNav != null) {
                    pi.file.addSBASNavigationMessage(pi.sbasNav);
                    pi.sbasNav = null;
                }
            }

        },

        /** IRNSS. */
        IRNSS() {

            /** {@inheritDoc} */
            @Override
            public void parseSvEpochSvClockLine(final String line, final ParseInfo pi) {
                parseSvEpochSvClockLine(line, pi.timeScales.getIRNSS(), pi.irnssNav);
            }

            /** {@inheritDoc} */
            @Override
            public void parseFirstBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.irnssNav.setIODEC(parseBroadcastDouble1(line,   Unit.SECOND));
                pi.irnssNav.setCrs(parseBroadcastDouble2(line,     Unit.METRE));
                pi.irnssNav.setDeltaN(parseBroadcastDouble3(line,  RAD_PER_S));
                pi.irnssNav.setM0(parseBroadcastDouble4(line,      Unit.RADIAN));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSecondBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.irnssNav.setCuc(parseBroadcastDouble1(line,   Unit.RADIAN));
                pi.irnssNav.setE(parseBroadcastDouble2(line,     Unit.NONE));
                pi.irnssNav.setCus(parseBroadcastDouble3(line,   Unit.RADIAN));
                pi.irnssNav.setSqrtA(parseBroadcastDouble4(line, SQRT_M));
            }

            /** {@inheritDoc} */
            @Override
            public void parseThirdBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.irnssNav.setTime(parseBroadcastDouble1(line,   Unit.SECOND));
                pi.irnssNav.setCic(parseBroadcastDouble2(line,    Unit.RADIAN));
                pi.irnssNav.setOmega0(parseBroadcastDouble3(line, Unit.RADIAN));
                pi.irnssNav.setCis(parseBroadcastDouble4(line,    Unit.RADIAN));
            }

            /** {@inheritDoc} */
            @Override
            public void parseFourthBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.irnssNav.setI0(parseBroadcastDouble1(line,       Unit.RADIAN));
                pi.irnssNav.setCrc(parseBroadcastDouble2(line,      Unit.METRE));
                pi.irnssNav.setPa(parseBroadcastDouble3(line,       Unit.RADIAN));
                pi.irnssNav.setOmegaDot(parseBroadcastDouble4(line, RAD_PER_S));
            }

            /** {@inheritDoc} */
            @Override
            public void parseFifthBroadcastOrbit(final String line, final ParseInfo pi) {
                // iDot
                pi.irnssNav.setIDot(parseBroadcastDouble1(line, RAD_PER_S));
                // IRNSS week (to go with Toe)
                pi.irnssNav.setWeek(parseBroadcastInt3(line));
                pi.irnssNav.setDate(new GNSSDate(pi.irnssNav.getWeek(),
                                                 pi.irnssNav.getTime(),
                                                 SatelliteSystem.GPS, // in Rinex files, week number is aligned to GPS week!
                                                 pi.timeScales).getDate());
            }

            /** {@inheritDoc} */
            @Override
            public void parseSixthBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.irnssNav.setURA(parseBroadcastDouble1(line,      Unit.METRE));
                pi.irnssNav.setSvHealth(parseBroadcastDouble2(line, Unit.NONE));
                pi.irnssNav.setTGD(parseBroadcastDouble3(line,      Unit.SECOND));
            }

            /** {@inheritDoc} */
            @Override
            public void parseSeventhBroadcastOrbit(final String line, final ParseInfo pi) {
                pi.irnssNav.setTransmissionTime(parseBroadcastDouble1(line, Unit.SECOND));
                pi.closePendingMessage();
            }

            /** {@inheritDoc} */
            @Override
            public void closeMessage(final ParseInfo pi) {
                if (pi.irnssNav != null) {
                    pi.file.addIRNSSNavigationMessage(pi.irnssNav);
                    pi.irnssNav = null;
                }
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
                    if (type == null || type.equals(LegacyNavigationMessage.LNAV)) {
                        parseInfo.gpsLNav = new GPSLegacyNavigationMessage();
                        return GPS_LNAV;
                    } else if (type.equals(CivilianNavigationMessage.CNAV)) {
                        parseInfo.gpsCNav = new GPSCivilianNavigationMessage(false);
                        return GPS_CNAV;
                    } else if (type.equals(CivilianNavigationMessage.CNV2)) {
                        parseInfo.gpsCNav = new GPSCivilianNavigationMessage(true);
                        return GPS_CNAV;
                    }
                    break;
                case GALILEO :
                    if (type == null || type.equals("INAV") || type.equals("FNAV")) {
                        parseInfo.galileoNav = new GalileoNavigationMessage();
                        return GALILEO;
                    }
                    break;
                case GLONASS :
                    if (type == null || type.equals("FDMA")) {
                        parseInfo.glonassNav = new GLONASSNavigationMessage();
                        return GLONASS;
                    }
                    break;
                case QZSS :
                    if (type == null || type.equals(LegacyNavigationMessage.LNAV)) {
                        parseInfo.qzssLNav = new QZSSLegacyNavigationMessage();
                        return QZSS_LNAV;
                    } else if (type.equals(CivilianNavigationMessage.CNAV)) {
                        parseInfo.qzssCNav = new QZSSCivilianNavigationMessage(false);
                        return QZSS_CNAV;
                    } else if (type.equals(CivilianNavigationMessage.CNV2)) {
                        parseInfo.qzssCNav = new QZSSCivilianNavigationMessage(true);
                        return QZSS_CNAV;
                    }
                    break;
                case BEIDOU :
                    if (type == null ||
                        type.equals(BeidouLegacyNavigationMessage.D1) ||
                        type.equals(BeidouLegacyNavigationMessage.D2)) {
                        parseInfo.beidouLNav = new BeidouLegacyNavigationMessage();
                        return BEIDOU_D1_D2;
                    } else if (type.equals(BeidouCivilianNavigationMessage.CNV1)) {
                        parseInfo.beidouCNav = new BeidouCivilianNavigationMessage(Frequency.B1C);
                        return BEIDOU_CNV_123;
                    } else if (type.equals(BeidouCivilianNavigationMessage.CNV2)) {
                        parseInfo.beidouCNav = new BeidouCivilianNavigationMessage(Frequency.B2A);
                        return BEIDOU_CNV_123;
                    } else if (type.equals(BeidouCivilianNavigationMessage.CNV3)) {
                        parseInfo.beidouCNav = new BeidouCivilianNavigationMessage(Frequency.B2B);
                        return BEIDOU_CNV_123;
                    }
                    break;
                case IRNSS :
                    if (type == null || type.equals("LNAV")) {
                        parseInfo.irnssNav = new IRNSSNavigationMessage();
                        return IRNSS;
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
        protected void parseSvEpochSvClockLine(final String line, final TimeScale timeScale,
                                               final AbstractNavigationMessage message) {
            // PRN
            message.setPRN(parsePrnSvEpochClock(line));

            // Toc
            message.setEpochToc(parsePrnSvEpochClock(line, timeScale));

            // clock (it's not really a broadcast orbit line, but the fields are at the same indices…)
            message.setAf0(parseBroadcastDouble2(line, Unit.SECOND));
            message.setAf1(parseBroadcastDouble3(line, S_PER_S));
            message.setAf2(parseBroadcastDouble4(line, S_PER_S2));

        }

        /** Parse PRN field of a Sv/epoch/cloak line.
         * @param line line to parse
         * @return parsed field
         */
        protected int parsePrnSvEpochClock(final String line) {
            return RinexUtils.parseInt(line, 1, 2);
        }

        /** Parse epoch field of a Sv/epoch/cloak line.
         * @param line line to parse
         * @param timeScale time scale to use
         * @return parsed field
         */
        protected AbsoluteDate parsePrnSvEpochClock(final String line, final TimeScale timeScale) {
            final int year  = RinexUtils.parseInt(line, 4, 4);
            final int month = RinexUtils.parseInt(line, 9, 2);
            final int day   = RinexUtils.parseInt(line, 12, 2);
            final int hours = RinexUtils.parseInt(line, 15, 2);
            final int min   = RinexUtils.parseInt(line, 18, 2);
            final int sec   = RinexUtils.parseInt(line, 21, 2);
            return new AbsoluteDate(year, month, day, hours, min, sec, timeScale);
        }

        /** Parse double field 1 of a broadcast orbit line.
         * @param line line to parse
         * @param unit unit to used for parsing the field
         * @return parsed field
         */
        protected double parseBroadcastDouble1(final String line, final Unit unit) {
            return unit.toSI(RinexUtils.parseDouble(line, 4, 19));
        }

        /** Parse integer field 1 of a broadcast orbit line.
         * @param line line to parse
         * @return parsed field
         */
        protected int parseBroadcastInt1(final String line) {
            return (int) FastMath.rint(RinexUtils.parseDouble(line, 4, 19));
        }

        /** Parse double field 2 of a broadcast orbit line.
         * @param line line to parse
         * @param unit unit to used for parsing the field
         * @return parsed field
         */
        protected double parseBroadcastDouble2(final String line, final Unit unit) {
            return unit.toSI(RinexUtils.parseDouble(line, 23, 19));
        }

        /** Parse integer field 2 of a broadcast orbit line.
         * @param line line to parse
         * @return parsed field
         */
        protected int parseBroadcastInt2(final String line) {
            return (int) FastMath.rint(RinexUtils.parseDouble(line, 23, 19));
        }

        /** Parse double field 3 of a broadcast orbit line.
         * @param line line to parse
         * @param unit unit to used for parsing the field
         * @return parsed field
         */
        protected double parseBroadcastDouble3(final String line, final Unit unit) {
            return unit.toSI(RinexUtils.parseDouble(line, 42, 19));
        }

        /** Parse integer field 3 of a broadcast orbit line.
         * @param line line to parse
         * @return parsed field
         */
        protected int parseBroadcastInt3(final String line) {
            return (int) FastMath.rint(RinexUtils.parseDouble(line, 42, 19));
        }

        /** Parse double field 4 of a broadcast orbit line.
         * @param line line to parse
         * @param unit unit to used for parsing the field
         * @return parsed field
         */
        protected double parseBroadcastDouble4(final String line, final Unit unit) {
            return unit.toSI(RinexUtils.parseDouble(line, 61, 19));
        }

        /** Parse integer field 4 of a broadcast orbit line.
         * @param line line to parse
         * @return parsed field
         */
        protected int parseBroadcastInt4(final String line) {
            return (int) FastMath.rint(RinexUtils.parseDouble(line, 61, 19));
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
