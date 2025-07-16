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
package org.orekit.files.rinex.observation;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.geometry.euclidean.twod.Vector2D;
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
import org.orekit.gnss.ObservationTimeScale;
import org.orekit.gnss.ObservationType;
import org.orekit.gnss.PredefinedObservationType;
import org.orekit.gnss.SatInSystem;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScales;
import org.orekit.utils.units.Unit;

/** Parser for Rinex measurements files.
 * <p>
 * Supported versions are: 2.00, 2.10, 2.11, 2.12 (unofficial), 2.20 (unofficial),
 * 3.00, 3.01, 3.02, 3.03, 3.04, 3.05, 4.00, 4.01, and 4.02.
 * </p>
 * @see <a href="https://files.igs.org/pub/data/format/rinex2.txt">rinex 2.0</a>
 * @see <a href="https://files.igs.org/pub/data/format/rinex210.txt">rinex 2.10</a>
 * @see <a href="https://files.igs.org/pub/data/format/rinex211.pdf">rinex 2.11</a>
 * @see <a href="http://www.aiub.unibe.ch/download/rinex/rinex212.txt">unofficial rinex 2.12</a>
 * @see <a href="http://www.aiub.unibe.ch/download/rinex/rnx_leo.txt">unofficial rinex 2.20</a>
 * @see <a href="https://files.igs.org/pub/data/format/rinex300.pdf">rinex 3.00</a>
 * @see <a href="https://files.igs.org/pub/data/format/rinex301.pdf">rinex 3.01</a>
 * @see <a href="https://files.igs.org/pub/data/format/rinex302.pdf">rinex 3.02</a>
 * @see <a href="https://files.igs.org/pub/data/format/rinex303.pdf">rinex 3.03</a>
 * @see <a href="https://files.igs.org/pub/data/format/rinex304.pdf">rinex 3.04</a>
 * @see <a href="https://files.igs.org/pub/data/format/rinex305.pdf">rinex 3.05</a>
 * @see <a href="https://files.igs.org/pub/data/format/rinex_4.00.pdf">rinex 4.00</a>
 * @see <a href="https://files.igs.org/pub/data/format/rinex_4.01.pdf">rinex 4.01</a>
 * @see <a href="https://files.igs.org/pub/data/format/rinex_4.02.pdf">rinex 4.02</a>
 * @since 12.0
 */
public class RinexObservationParser {

    /** Default name pattern for rinex 2 observation files. */
    public static final String DEFAULT_RINEX_2_NAMES = "^\\w{4}\\d{3}[0a-x](?:\\d{2})?\\.\\d{2}[oO]$";

    /** Default name pattern for rinex 3 observation files. */
    public static final String DEFAULT_RINEX_3_NAMES = "^\\w{9}_\\w{1}_\\d{11}_\\d{2}\\w_\\d{2}\\w{1}_\\w{2}\\.rnx$";

    /** Maximum number of satellites per line in Rinex 2 format . */
    private static final int MAX_SAT_PER_RINEX_2_LINE = 12;

    /** Maximum number of observations per line in Rinex 2 format. */
    private static final int MAX_OBS_PER_RINEX_2_LINE = 5;

    /** Pico seconds. */
    private static final Unit PICO_SECOND = Unit.parse("ps");

    /** Set of time scales. */
    private final TimeScales timeScales;

    /** Mapper from string to observation type.
     * @since 13.0
     */
    private final Function<? super String, ? extends ObservationType> typeBuilder;

    /** Mapper from satellite system to time scales.
     * @since 13.0
     */
    private final BiFunction<SatelliteSystem, TimeScales, ? extends TimeScale> timeScaleBuilder;

    /** Simple constructor.
     * <p>
     * This constructor uses the {@link DataContext#getDefault() default data context}
     * and recognizes only {@link PredefinedObservationType} and {@link SatelliteSystem}
     * with non-null {@link SatelliteSystem#getObservationTimeScale() time scales}
     * (i.e. neither user-defined, nor {@link SatelliteSystem#SBAS}, nor {@link SatelliteSystem#MIXED}).
     * </p>
     * @see #RinexObservationParser(Function, BiFunction, TimeScales)
     */
    @DefaultDataContext
    public RinexObservationParser() {
        this(PredefinedObservationType::valueOf,
             (system, ts) -> system.getObservationTimeScale() == null ?
                             null :
                             system.getObservationTimeScale().getTimeScale(ts),
             DataContext.getDefault().getTimeScales());
    }

    /**
     * Create a RINEX loader/parser with the given source of RINEX auxiliary data files.
     * @param typeBuilder mapper from string to observation type
     * @param timeScaleBuilder mapper from satellite system to time scales (useful for user-defined satellite systems)
     * @param timeScales the set of time scales to use when parsing dates
     * @since 13.0
     */
    public RinexObservationParser(final Function<? super String, ? extends ObservationType> typeBuilder,
                                  final BiFunction<SatelliteSystem, TimeScales, ? extends TimeScale> timeScaleBuilder,
                                  final TimeScales timeScales) {
        this.typeBuilder      = typeBuilder;
        this.timeScaleBuilder = timeScaleBuilder;
        this.timeScales       = timeScales;
    }

    /**
     * Parse RINEX observations messages.
     * @param source source providing the data to parse
     * @return parsed observations file
     */
    public RinexObservation parse(final DataSource source) {

        Iterable<LineParser> candidateParsers = Collections.singleton(LineParser.VERSION);

        // placeholders for parsed data
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
                            } catch (StringIndexOutOfBoundsException | NumberFormatException e) {
                                throw new OrekitException(e,
                                                          OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
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

    /** Transient data used for parsing a RINEX observation messages file.
     * @since 12.0
     */
    private class ParseInfo {

        /** Name of the data source. */
        private final String name;

        /** Mapper from string to observation type.
         * @since 14.0
         */
        private final Function<? super String, ? extends ObservationType> typeBuilder;

        /** Mapper from satellite system to time scales.
         * @since 13.0
         */
        private final BiFunction<SatelliteSystem, TimeScales, ? extends TimeScale> timeScaleBuilder;

        /** Set of time scales for parsing dates. */
        private final TimeScales timeScales;

        /** Current line number of the navigation message. */
        private int lineNumber;

        /** Rinex file. */
        private final RinexObservation file;

        /** Date of the observation. */
        private AbsoluteDate tObs;

        /** Indicator that time of first observation was already fixed. */
        private boolean tFirstFixed;

        /** Indicator that time of last observation was already fixed. */
        private boolean tLastFixed;

        /** Receiver clock offset (seconds). */
        private double rcvrClkOffset;

        /** time scale for parsing dates. */
        private TimeScale timeScale;

        /** Number of observation types. */
        private int nbTypes;

        /** Number of satellites in the current observations block. */
        private int nbSatObs;

        /** Number of scaling factors. */
        private int nbObsScaleFactor;

        /** Index of satellite in current observation. */
        private int indexObsSat;

        /** Line number of start of next observation. */
        private int nextObsStartLineNumber;

        /** Current satellite system. */
        private SatelliteSystem currentSystem;

        /** Number of satellites affected by phase shifts. */
        private int phaseShiftNbSat;

        /** Number of GLONASS satellites. */
        private int nbGlonass;

        /** Satellites affected by phase shift. */
        private final List<SatInSystem> satPhaseShift;

        /** Type of observation affected by phase shift. */
        private ObservationType phaseShiftTypeObs;

        /** Phase shift correction. */
        private double corrPhaseShift;

        /** Indicator for completed header. */
        private boolean headerCompleted;

        /** Indicator for skipping special records (eventFlag from 2 to 5). */
        private boolean specialRecord;

        /** Indicator for skipping cyckle slip records (enventFlag == 6). */
        private boolean cycleSlip;

        /** Event flag. */
        private int eventFlag;

        /** Scaling factors. */
        private final List<ObservationType> typesObsScaleFactor;

        /** Types of observations. */
        private final List<ObservationType> typesObs;

        /** Observations. */
        private final List<ObservationData> observations;

        /** Satellites in current observation. */
        private final List<SatInSystem> satObs;

        /** Current satellite. */
        private SatInSystem currentSat;

        /** Constructor, build the ParseInfo object.
         * @param name name of the data source
         */
        ParseInfo(final String name) {
            // Initialize default values for fields
            this.name                   = name;
            this.typeBuilder            = RinexObservationParser.this.typeBuilder;
            this.timeScales             = RinexObservationParser.this.timeScales;
            this.timeScaleBuilder       = RinexObservationParser.this.timeScaleBuilder;
            this.file                   = new RinexObservation();
            this.lineNumber             = 0;
            this.tObs                   = AbsoluteDate.PAST_INFINITY;
            this.tFirstFixed            = false;
            this.tLastFixed             = false;
            this.timeScale              = null;
            this.nbTypes                = -1;
            this.nbSatObs               = -1;
            this.nbGlonass              = -1;
            this.phaseShiftNbSat        = -1;
            this.nbObsScaleFactor       = -1;
            this.nextObsStartLineNumber = -1;
            this.typesObs               = new ArrayList<>();
            this.observations           = new ArrayList<>();
            this.satPhaseShift          = new ArrayList<>();
            this.typesObsScaleFactor    = new ArrayList<>();
            this.satObs                 = new ArrayList<>();

            // reset the default values set by header constructor
            this.file.getHeader().setProgramName(null);
            this.file.getHeader().setRunByName(null);
            this.file.getHeader().setCreationDateComponents(null);

        }

        /** Set observation date, taking care of receiver/absolute time scales.
         * @param rawDate date as parsed, prior to any time scale modification
         */
        private void setTObs(final AbsoluteDate rawDate) {
            final RinexObservationHeader header = file.getHeader();
            if (header.getClockOffsetApplied()) {
                // date was already in an absolute time scale
                tObs = rawDate;
            } else {
                // the epoch was expressed in receiver clock
                // we need to convert it to absolute date
                if (FastMath.abs(rawDate.durationFrom(header.getTFirstObs())) < 1.0e-6 &&
                    !tFirstFixed) {
                    // we need to fix the first date in the header too
                    header.setTFirstObs(header.getTFirstObs().shiftedBy(-rcvrClkOffset));
                    tFirstFixed = true;
                }
                if (FastMath.abs(rawDate.durationFrom(header.getTLastObs())) < 1.0e-6 &&
                    !tLastFixed) {
                    // we need to fix the last date in the header too
                    header.setTLastObs(header.getTLastObs().shiftedBy(-rcvrClkOffset));
                    tLastFixed = true;
                }
                tObs = rawDate.shiftedBy(-rcvrClkOffset);
            }
        }

    }

    /** Parsers for specific lines. */
    private enum LineParser {

        /** Parser for version, file type and satellite system. */
        VERSION((header, line) -> header.matchFound(CommonLabel.VERSION, line),
                (line, parseInfo) ->  parseInfo.file.getHeader().
                    parseVersionFileTypeSatelliteSystem(line, SatelliteSystem.GPS, parseInfo.name,
                                                        2.00, 2.10, 2.11, 2.12, 2.20,
                                                        3.00, 3.01, 3.02, 3.03, 3.04, 3.05,
                                                        4.00, 4.01, 4.02),
                LineParser::headerNext),

        /** Parser for generating program and emitting agency. */
        PROGRAM((header, line) -> header.matchFound(CommonLabel.PROGRAM, line),
                (line, parseInfo) -> parseInfo.file.getHeader().parseProgramRunByDate(line, parseInfo.timeScales),
                LineParser::headerNext),

        /** Parser for comments. */
        COMMENT((header, line) -> header.matchFound(CommonLabel.COMMENT, line),
                       (line, parseInfo) -> ParsingUtils.parseComment(parseInfo.lineNumber, line, parseInfo.file),
                       LineParser::commentNext),

        /** Parser for marker name. */
        MARKER_NAME((header, line) -> header.matchFound(ObservationLabel.MARKER_NAME, line),
                    (line, parseInfo) ->  parseInfo.file.getHeader().
                        setMarkerName(ParsingUtils.parseString(line, 0, parseInfo.file.getHeader().getLabelIndex())),
                    LineParser::headerNext),

        /** Parser for marker number. */
        MARKER_NUMBER((header, line) -> header.matchFound(ObservationLabel.MARKER_NUMBER, line),
                      (line, parseInfo) -> parseInfo.file.getHeader().setMarkerNumber(
                          ParsingUtils.parseString(line, 0, 20)),
                      LineParser::headerNext),

        /** Parser for marker type. */
        MARKER_TYPE((header, line) -> header.matchFound(ObservationLabel.MARKER_TYPE, line),
                    (line, parseInfo) -> parseInfo.file.getHeader().setMarkerType(ParsingUtils.parseString(line, 0, 20)),
                    LineParser::headerNext),

        /** Parser for observer agency. */
        OBSERVER_AGENCY((header, line) -> header.matchFound(ObservationLabel.OBSERVER_AGENCY, line),
                        (line, parseInfo) -> {
                            parseInfo.file.getHeader().setObserverName(ParsingUtils.parseString(line, 0, 20));
                            parseInfo.file.getHeader().setAgencyName(ParsingUtils.parseString(line, 20, 40));
                        },
                        LineParser::headerNext),

        /** Parser for receiver number, type and version. */
        REC_NB_TYPE_VERS((header, line) -> header.matchFound(ObservationLabel.REC_NB_TYPE_VERS, line),
                         (line, parseInfo) -> {
                             parseInfo.file.getHeader().setReceiverNumber(ParsingUtils.parseString(line, 0, 20));
                             parseInfo.file.getHeader().setReceiverType(ParsingUtils.parseString(line, 20, 20));
                             parseInfo.file.getHeader().setReceiverVersion(ParsingUtils.parseString(line, 40, 20));
                         },
                         LineParser::headerNext),

        /** Parser for antenna number and type. */
        ANT_NB_TYPE((header, line) -> header.matchFound(ObservationLabel.ANT_NB_TYPE, line),
                    (line, parseInfo) -> {
                        parseInfo.file.getHeader().setAntennaNumber(ParsingUtils.parseString(line, 0, 20));
                        parseInfo.file.getHeader().setAntennaType(ParsingUtils.parseString(line, 20, 20));
                    },
                    LineParser::headerNext),

        /** Parser for approximative position. */
        APPROX_POSITION_XYZ((header, line) -> header.matchFound(ObservationLabel.APPROX_POSITION_XYZ, line),
                            (line, parseInfo) -> parseInfo.file.getHeader().setApproxPos(new Vector3D(
                                ParsingUtils.parseDouble(line, 0, 14),
                                ParsingUtils.parseDouble(line, 14, 14),
                                ParsingUtils.parseDouble(line, 28, 14))),
                            LineParser::headerNext),

        /** Parser for antenna reference point. */
        ANTENNA_DELTA_H_E_N((header, line) -> header.matchFound(ObservationLabel.ANTENNA_DELTA_H_E_N, line),
                            (line, parseInfo) -> {
                                parseInfo.file.getHeader().setAntennaHeight(ParsingUtils.parseDouble(line, 0, 14));
                                parseInfo.file.getHeader().setEccentricities(new Vector2D(
                                    ParsingUtils.parseDouble(line, 14, 14),
                                    ParsingUtils.parseDouble(line, 28, 14)));
                            },
                            LineParser::headerNext),

        /** Parser for antenna reference point. */
        ANTENNA_DELTA_X_Y_Z((header, line) -> header.matchFound(ObservationLabel.ANTENNA_DELTA_X_Y_Z, line),
                            (line, parseInfo) -> parseInfo.file.getHeader().setAntennaReferencePoint(new Vector3D(
                                ParsingUtils.parseDouble(line, 0, 14),
                                ParsingUtils.parseDouble(line, 14, 14),
                                ParsingUtils.parseDouble(line, 28, 14))),
                            LineParser::headerNext),

        /** Parser for antenna phase center. */
        ANTENNA_PHASE_CENTER((header, line) -> header.matchFound(ObservationLabel.ANTENNA_PHASE_CENTER, line),
                             (line, parseInfo) -> {
                                 parseInfo.file.getHeader().setPhaseCenterSystem(SatelliteSystem.parseSatelliteSystem(
                                     ParsingUtils.parseString(line, 0, 1)));
                                 parseInfo.file.getHeader().setObservationCode(ParsingUtils.parseString(line, 2, 3));
                                 parseInfo.file.getHeader().setAntennaPhaseCenter(new Vector3D(
                                     ParsingUtils.parseDouble(line, 5, 9),
                                     ParsingUtils.parseDouble(line, 14, 14),
                                     ParsingUtils.parseDouble(line, 28, 14)));
                             },
                             LineParser::headerNext),

        /** Parser for antenna bore sight. */
        ANTENNA_B_SIGHT_XYZ((header, line) -> header.matchFound(ObservationLabel.ANTENNA_B_SIGHT_XYZ, line),
                            (line, parseInfo) -> parseInfo.file.getHeader().setAntennaBSight(new Vector3D(
                                ParsingUtils.parseDouble(line, 0, 14),
                                ParsingUtils.parseDouble(line, 14, 14),
                                ParsingUtils.parseDouble(line, 28, 14))),
                            LineParser::headerNext),

        /** Parser for antenna zero direction. */
        ANTENNA_ZERODIR_AZI((header, line) -> header.matchFound(ObservationLabel.ANTENNA_ZERODIR_AZI, line),
                            (line, parseInfo) -> parseInfo.file.getHeader().setAntennaAzimuth(FastMath.toRadians(
                                ParsingUtils.parseDouble(line, 0, 14))),
                            LineParser::headerNext),

        /** Parser for antenna zero direction. */
        ANTENNA_ZERODIR_XYZ((header, line) -> header.matchFound(ObservationLabel.ANTENNA_ZERODIR_XYZ, line),
                            (line, parseInfo) -> parseInfo.file.getHeader().setAntennaZeroDirection(new Vector3D(
                                ParsingUtils.parseDouble(line, 0, 14),
                                ParsingUtils.parseDouble(line, 14, 14),
                                ParsingUtils.parseDouble(line, 28, 14))),
                            LineParser::headerNext),

        /** Parser for wavelength factors. */
        WAVELENGTH_FACT_L1_2((header, line) -> header.matchFound(ObservationLabel.WAVELENGTH_FACT_L1_2, line),
                             (line, parseInfo) -> {
                                 // optional line in Rinex 2 header, not stored for now
                             },
                             LineParser::headerNext),

        /** Parser for observations scale factor. */
        OBS_SCALE_FACTOR((header, line) -> header.matchFound(ObservationLabel.OBS_SCALE_FACTOR, line),
                         (line, parseInfo) -> {
                             final int scaleFactor      = FastMath.max(1, ParsingUtils.parseInt(line, 0, 6));
                             final int nbObsScaleFactor = ParsingUtils.parseInt(line, 6, 6);
                             final List<ObservationType> types = new ArrayList<>(nbObsScaleFactor);
                             for (int i = 0; i < nbObsScaleFactor; i++) {
                                 types.add(parseInfo.typeBuilder.apply(ParsingUtils.parseString(line, 16 + (6 * i), 2)));
                             }
                             parseInfo.file.getHeader().addScaleFactorCorrection(parseInfo.file.getHeader().getSatelliteSystem(),
                                                                                 new ScaleFactorCorrection(scaleFactor, types));
                         },
                         LineParser::headerNext),

        /** Parser for center of mass. */
        CENTER_OF_MASS_XYZ((header, line) -> header.matchFound(ObservationLabel.CENTER_OF_MASS_XYZ, line),
                           (line, parseInfo) -> parseInfo.file.getHeader().setCenterMass(new Vector3D(
                               ParsingUtils.parseDouble(line, 0, 14),
                               ParsingUtils.parseDouble(line, 14, 14),
                               ParsingUtils.parseDouble(line, 28, 14))),
                           LineParser::headerNext),

        /** Parser for DOI.
         * @since 12.0
         */
        DOI((header, line) -> header.matchFound(CommonLabel.DOI, line),
            (line, parseInfo) -> parseInfo.file.getHeader().
                setDoi(ParsingUtils.parseString(line, 0, parseInfo.file.getHeader().getLabelIndex())),
            LineParser::headerNext),

        /** Parser for license.
         * @since 12.0
         */
        LICENSE((header, line) -> header.matchFound(CommonLabel.LICENSE, line),
                (line, parseInfo) -> parseInfo.file.getHeader().
                    setLicense(ParsingUtils.parseString(line, 0, parseInfo.file.getHeader().getLabelIndex())),
                LineParser::headerNext),

        /** Parser for station information.
         * @since 12.0
         */
        STATION_INFORMATION((header, line) -> header.matchFound(CommonLabel.STATION_INFORMATION, line),
                            (line, parseInfo) -> parseInfo.file.getHeader().
                                setStationInformation(ParsingUtils.parseString(line, 0, parseInfo.file.getHeader().getLabelIndex())),
                            LineParser::headerNext),

        /** Parser for number and types of observations. */
        SYS_NB_TYPES_OF_OBSERV((header, line) -> header.matchFound(CommonLabel.SYS_NB_TYPES_OF_OBSERV, line) ||
                                       header.matchFound(ObservationLabel.NB_TYPES_OF_OBSERV, line),
                               (line, parseInfo) -> {
                                   final RinexObservationHeader header = parseInfo.file.getHeader();
                                   final double version = header.getFormatVersion();
                                   if (parseInfo.nbTypes < 0) {
                                       // first line of types of observations
                                       if (version < 3) {
                                           // Rinex 2 has only one system
                                           parseInfo.currentSystem = header.getSatelliteSystem();
                                           parseInfo.nbTypes       = ParsingUtils.parseInt(line, 0, 6);
                                       } else {
                                           // Rinex 3 and above allow mixed systems
                                           parseInfo.currentSystem = SatelliteSystem.parseSatelliteSystem(
                                               ParsingUtils.parseString(line, 0, 1));
                                           parseInfo.nbTypes       = ParsingUtils.parseInt(line, 3, 3);
                                           if (parseInfo.currentSystem != header.getSatelliteSystem() &&
                                               header.getSatelliteSystem() != SatelliteSystem.MIXED) {
                                               throw new OrekitException(OrekitMessages.INCONSISTENT_SATELLITE_SYSTEM,
                                                                         parseInfo.lineNumber, parseInfo.name,
                                                                         header.getSatelliteSystem(),
                                                                         parseInfo.currentSystem);
                                           }
                                       }
                                   }

                                   final int firstIndex = version < 3 ? 10 : 7;
                                   final int increment  = version < 3 ?  6 : 4;
                                   final int size       = version < 3 ?  2 : 3;
                                   for (int i = firstIndex;
                                                   (i + size) <= header.getLabelIndex() && parseInfo.typesObs.size() < parseInfo.nbTypes;
                                                   i += increment) {
                                       final String type = ParsingUtils.parseString(line, i, size);
                                       try {
                                           parseInfo.typesObs.add(parseInfo.typeBuilder.apply(type));
                                       } catch (IllegalArgumentException iae) {
                                           throw new OrekitException(iae, OrekitMessages.UNKNOWN_RINEX_FREQUENCY,
                                                                     type, parseInfo.name, parseInfo.lineNumber);
                                       }
                                   }

                                   if (parseInfo.typesObs.size() == parseInfo.nbTypes) {
                                       // we have completed the list
                                       header.setTypeObs(parseInfo.currentSystem, parseInfo.typesObs);
                                       parseInfo.typesObs.clear();
                                       parseInfo.nbTypes = -1;
                                   }

                               },
                               LineParser::headerNbTypesObs),

        /** Parser for unit of signal strength. */
        SIGNAL_STRENGTH_UNIT((header, line) -> header.matchFound(ObservationLabel.SIGNAL_STRENGTH_UNIT, line),
                             (line, parseInfo) -> parseInfo.file.getHeader().setSignalStrengthUnit(
                                 ParsingUtils.parseString(line, 0, 20)),
                             LineParser::headerNext),

        /** Parser for observation interval. */
        INTERVAL((header, line) -> header.matchFound(ObservationLabel.INTERVAL, line),
                 (line, parseInfo) -> parseInfo.file.getHeader().setInterval(ParsingUtils.parseDouble(line, 0, 10)),
                 LineParser::headerNext),

        /** Parser for time of first observation. */
        TIME_OF_FIRST_OBS((header, line) -> header.matchFound(ObservationLabel.TIME_OF_FIRST_OBS, line),
                          (line, parseInfo) -> {
                              try {
                                  // general case: TIME OF FIRST OBS specifies the time scale
                                  parseInfo.timeScale = ObservationTimeScale.
                                                        valueOf(ParsingUtils.parseString(line, 48, 3)).
                                                        getTimeScale(parseInfo.timeScales);
                              } catch (IllegalArgumentException iae) {
                                  if (parseInfo.file.getHeader().getSatelliteSystem() != SatelliteSystem.MIXED) {
                                      // use the default from time system header
                                      parseInfo.timeScale = parseInfo.timeScaleBuilder.apply(parseInfo.file.getHeader().getSatelliteSystem(),
                                                                                             parseInfo.timeScales);
                                      if (parseInfo.timeScale == null) {
                                          throw new OrekitException(iae, OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                                    parseInfo.lineNumber, parseInfo.name, line);
                                      }
                                  } else {
                                      // in case of mixed data, time scale must be specified in the Time of First Observation line
                                      throw new OrekitException(iae, OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                                parseInfo.lineNumber, parseInfo.name, line);
                                  }
                              }
                              parseInfo.file.getHeader().setTFirstObs(new AbsoluteDate(ParsingUtils.parseInt(line, 0, 6),
                                                                                       ParsingUtils.parseInt(line, 6, 6),
                                                                                       ParsingUtils.parseInt(line, 12, 6),
                                                                                       ParsingUtils.parseInt(line, 18, 6),
                                                                                       ParsingUtils.parseInt(line, 24, 6),
                                                                                       ParsingUtils.parseDouble(line, 30, 13),
                                                                                       parseInfo.timeScale));
                          },
                          LineParser::headerNext),

        /** Parser for time of last observation. */
        TIME_OF_LAST_OBS((header, line) -> header.matchFound(ObservationLabel.TIME_OF_LAST_OBS, line),
                         (line, parseInfo) -> parseInfo.file.getHeader().setTLastObs(new AbsoluteDate(
                             ParsingUtils.parseInt(line, 0, 6),
                             ParsingUtils.parseInt(line, 6, 6),
                             ParsingUtils.parseInt(line, 12, 6),
                             ParsingUtils.parseInt(line, 18, 6),
                             ParsingUtils.parseInt(line, 24, 6),
                             ParsingUtils.parseDouble(line, 30, 13),
                             parseInfo.timeScale)),
                         LineParser::headerNext),

        /** Parser for indicator of receiver clock offset application. */
        RCV_CLOCK_OFFS_APPL((header, line) -> header.matchFound(ObservationLabel.RCV_CLOCK_OFFS_APPL, line),
                            (line, parseInfo) -> parseInfo.file.getHeader().setClockOffsetApplied(
                                ParsingUtils.parseInt(line, 0, 6) > 0),
                            LineParser::headerNext),

        /** Parser for differential code bias corrections. */
        SYS_DCBS_APPLIED((header, line) -> header.matchFound(CommonLabel.SYS_DCBS_APPLIED, line),
                         (line, parseInfo) -> parseInfo.file.getHeader().addAppliedDCBS(new AppliedDCBS(SatelliteSystem.parseSatelliteSystem(
                             ParsingUtils.parseString(line, 0, 1)),
                                                                                                        ParsingUtils.parseString(line, 2, 17),
                                                                                                        ParsingUtils.parseString(line, 20, 40))),
                         LineParser::headerNext),

        /** Parser for phase center variations corrections. */
        SYS_PCVS_APPLIED((header, line) -> header.matchFound(CommonLabel.SYS_PCVS_APPLIED, line),
                         (line, parseInfo) -> parseInfo.file.getHeader().addAppliedPCVS(new AppliedPCVS(SatelliteSystem.parseSatelliteSystem(
                             ParsingUtils.parseString(line, 0, 1)),
                                                                                                        ParsingUtils.parseString(line, 2, 17),
                                                                                                        ParsingUtils.parseString(line, 20, 40))),
                         LineParser::headerNext),

        /** Parser for scale factor. */
        SYS_SCALE_FACTOR((header, line) -> header.matchFound(ObservationLabel.SYS_SCALE_FACTOR, line),
                         (line, parseInfo) -> {

                             final RinexObservationHeader header = parseInfo.file.getHeader();
                             int scaleFactor = 1;
                             if (parseInfo.nbObsScaleFactor < 0) {
                                 // first line of scale factor
                                 parseInfo.currentSystem    = SatelliteSystem.parseSatelliteSystem(
                                     ParsingUtils.parseString(line, 0, 1));
                                 scaleFactor                = ParsingUtils.parseInt(line, 2, 4);
                                 parseInfo.nbObsScaleFactor = ParsingUtils.parseInt(line, 8, 2);
                             }

                             if (parseInfo.nbObsScaleFactor == 0) {
                                 parseInfo.typesObsScaleFactor.addAll(header.getTypeObs().get(parseInfo.currentSystem));
                             } else {
                                 for (int i = 11;
                                      i < header.getLabelIndex() && parseInfo.typesObsScaleFactor.size() < parseInfo.nbObsScaleFactor;
                                      i += 4) {
                                     parseInfo.typesObsScaleFactor.add(parseInfo.typeBuilder.apply(
                                         ParsingUtils.parseString(line, i, 3)));
                                 }
                             }

                             if (parseInfo.typesObsScaleFactor.size() >= parseInfo.nbObsScaleFactor) {
                                 // we have completed the list
                                 header.addScaleFactorCorrection(parseInfo.currentSystem,
                                                                 new ScaleFactorCorrection(scaleFactor,
                                                                                           new ArrayList<>(parseInfo.typesObsScaleFactor)));
                                 parseInfo.nbObsScaleFactor = -1;
                                 parseInfo.typesObsScaleFactor.clear();
                             }

                         },
                         LineParser::headerNext),

        /** Parser for phase shift. */
        SYS_PHASE_SHIFT((header, line) -> header.matchFound(ObservationLabel.SYS_PHASE_SHIFT, line),
                        (line, parseInfo) -> {

                            final RinexObservationHeader header = parseInfo.file.getHeader();
                            if (parseInfo.phaseShiftNbSat < 0) {
                                // first line of phase shift
                                parseInfo.currentSystem     = SatelliteSystem.parseSatelliteSystem(
                                    ParsingUtils.parseString(line, 0, 1));
                                final String to             = ParsingUtils.parseString(line, 2, 3);
                                parseInfo.phaseShiftTypeObs = to.isEmpty() ?
                                                              null :
                                                              parseInfo.typeBuilder.apply(to.length() < 3 ? "L" + to : to);
                                parseInfo.corrPhaseShift    = ParsingUtils.parseDouble(line, 6, 8);
                                parseInfo.phaseShiftNbSat   = ParsingUtils.parseInt(line, 16, 2);
                            }

                            for (int i = 19;
                                 i + 3 < header.getLabelIndex() && parseInfo.satPhaseShift.size() < parseInfo.phaseShiftNbSat;
                                 i += 4) {
                                final String satSpec = line.charAt(i) == ' ' ?
                                                       parseInfo.currentSystem.getKey() + line.substring(i + 1, i + 3) :
                                                       line.substring(i, i + 3);
                                parseInfo.satPhaseShift.add(new SatInSystem(satSpec));
                            }

                            if (parseInfo.satPhaseShift.size() == parseInfo.phaseShiftNbSat) {
                                // we have completed the list
                                header.addPhaseShiftCorrection(new PhaseShiftCorrection(parseInfo.currentSystem,
                                                                                        parseInfo.phaseShiftTypeObs,
                                                                                        parseInfo.corrPhaseShift,
                                                                                        new ArrayList<>(parseInfo.satPhaseShift)));
                                parseInfo.phaseShiftNbSat = -1;
                                parseInfo.satPhaseShift.clear();
                            }

                        },
                        LineParser::headerPhaseShift),

        /** Parser for GLONASS slot and frequency number. */
        GLONASS_SLOT_FRQ_NB((header, line) -> header.matchFound(ObservationLabel.GLONASS_SLOT_FRQ_NB, line),
                            (line, parseInfo) -> {

                                final RinexObservationHeader header = parseInfo.file.getHeader();
                                if (parseInfo.nbGlonass < 0) {
                                    // first line of GLONASS satellite/frequency association
                                    parseInfo.nbGlonass = ParsingUtils.parseInt(line, 0, 3);
                                }

                                for (int i = 4;
                                     i < header.getLabelIndex() && header.getGlonassChannels().size() < parseInfo.nbGlonass;
                                     i += 7) {
                                    final int k = ParsingUtils.parseInt(line, i + 4, 2);
                                    header.addGlonassChannel(new GlonassSatelliteChannel(new SatInSystem(line.substring(i, i + 3)), k));
                                }

                            },
                            LineParser::headerNext),

        /** Parser for GLONASS phase bias corrections. */
        GLONASS_COD_PHS_BIS((header, line) -> header.matchFound(ObservationLabel.GLONASS_COD_PHS_BIS, line),
                            (line, parseInfo) -> {

                                // C1C signal
                                final String c1c = ParsingUtils.parseString(line, 1, 3);
                                if (!c1c.isEmpty()) {
                                    parseInfo.file.getHeader().setC1cCodePhaseBias(ParsingUtils.parseDouble(line, 5, 8));
                                }

                                // C1P signal
                                final String c1p = ParsingUtils.parseString(line, 14, 3);
                                if (!c1p.isEmpty()) {
                                    parseInfo.file.getHeader().setC1pCodePhaseBias(ParsingUtils.parseDouble(line, 18, 8));
                                }

                                // C2C signal
                                final String c2c = ParsingUtils.parseString(line, 27, 3);
                                if (!c2c.isEmpty()) {
                                    parseInfo.file.getHeader().setC2cCodePhaseBias(ParsingUtils.parseDouble(line, 31, 8));
                                }

                                // C2P signal
                                final String c2p = ParsingUtils.parseString(line, 40, 3);
                                if (!c2p.isEmpty()) {
                                    parseInfo.file.getHeader().setC2pCodePhaseBias(ParsingUtils.parseDouble(line, 44, 8));
                                }

                            },
                            LineParser::headerNext),

        /** Parser for leap seconds. */
        LEAP_SECONDS((header, line) -> header.matchFound(CommonLabel.LEAP_SECONDS, line),
                     (line, parseInfo) -> {
                         parseInfo.file.getHeader().setLeapSecondsGNSS(ParsingUtils.parseInt(line, 0, 6));
                         if (parseInfo.file.getHeader().getFormatVersion() > 3.0) {
                             // extra fields introduced in 3.01
                             parseInfo.file.getHeader().setLeapSecondsFuture(ParsingUtils.parseInt(line, 6, 6));
                             parseInfo.file.getHeader().setLeapSecondsWeekNum(ParsingUtils.parseInt(line, 12, 6));
                             parseInfo.file.getHeader().setLeapSecondsDayNum(ParsingUtils.parseInt(line, 18, 6));
                         }
                     },
                     LineParser::headerNext),

        /** Parser for number of satellites. */
        NB_OF_SATELLITES((header, line) -> header.matchFound(ObservationLabel.NB_OF_SATELLITES, line),
                         (line, parseInfo) -> parseInfo.file.getHeader().setNbSat(ParsingUtils.parseInt(line, 0, 6)),
                         LineParser::headerNext),

        /** Parser for PRN and number of observations . */
        PRN_NB_OF_OBS((header, line) -> header.matchFound(ObservationLabel.PRN_NB_OF_OBS, line),
                      (line, parseInfo) ->  {
                          final RinexObservationHeader header = parseInfo.file.getHeader();
                          final String systemName = ParsingUtils.parseString(line, 3, 1);
                          if (!systemName.isEmpty()) {
                              parseInfo.currentSat = new SatInSystem(line.substring(3, 6));
                              parseInfo.nbTypes    = 0;
                          }
                          final List<ObservationType> types = header.getTypeObs().get(parseInfo.currentSat.getSystem());

                          final int firstIndex = 6;
                          final int increment  = 6;
                          final int size       = 6;
                          for (int i = firstIndex;
                               (i + size) <= header.getLabelIndex() && parseInfo.nbTypes < types.size();
                               i += increment) {
                              final String nb = ParsingUtils.parseString(line, i, size);
                              if (!nb.isEmpty()) {
                                  header.setNbObsPerSatellite(parseInfo.currentSat, types.get(parseInfo.nbTypes),
                                                              ParsingUtils.parseInt(line, i, size));
                              }
                              ++parseInfo.nbTypes;
                          }

                      },
                      LineParser::headerNext),

        /** Parser for the end of header. */
        END((header, line) -> header.matchFound(CommonLabel.END, line),
            (line, parseInfo) -> {

                parseInfo.headerCompleted = true;

                // get rinex format version
                final RinexObservationHeader header = parseInfo.file.getHeader();
                final double version = header.getFormatVersion();

                // check mandatory header fields
                if (version < 3) {
                    if (header.getMarkerName()                  == null ||
                        header.getObserverName()                == null ||
                        header.getReceiverNumber()              == null ||
                        header.getAntennaNumber()               == null ||
                        header.getTFirstObs()                   == null ||
                        version < 2.20 && header.getApproxPos() == null ||
                        version < 2.20 && Double.isNaN(header.getAntennaHeight()) ||
                        header.getTypeObs().isEmpty()) {
                        throw new OrekitException(OrekitMessages.INCOMPLETE_HEADER, parseInfo.name);
                    }

                } else {
                    if (header.getMarkerName()           == null ||
                        header.getObserverName()         == null ||
                        header.getReceiverNumber()       == null ||
                        header.getAntennaNumber()        == null ||
                        Double.isNaN(header.getAntennaHeight()) &&
                        header.getAntennaReferencePoint() == null  ||
                        header.getTFirstObs()            == null ||
                        header.getTypeObs().isEmpty()) {
                        throw new OrekitException(OrekitMessages.INCOMPLETE_HEADER, parseInfo.name);
                    }
                }
            },
            LineParser::headerEndNext),

        /** Parser for Rinex 2 data list of satellites. */
        RINEX_2_DATA_SAT_LIST((header, line) -> true,
                              (line, parseInfo) -> {
                                  final RinexObservationHeader header = parseInfo.file.getHeader();
                                  for (int index = 32; parseInfo.satObs.size() < parseInfo.nbSatObs && index < 68; index += 3) {
                                      // add one PRN to the list of observed satellites
                                      final String satSpec =
                                              line.charAt(index) == ' ' ?
                                              header.getSatelliteSystem().getKey() + line.substring(index + 1, index + 3) :
                                              line.substring(index, index + 3);
                                      final SatInSystem satellite = new SatInSystem(satSpec);
                                      if (satellite.getSystem() != header.getSatelliteSystem() &&
                                          header.getSatelliteSystem() != SatelliteSystem.MIXED) {
                                          throw new OrekitException(OrekitMessages.INCONSISTENT_SATELLITE_SYSTEM,
                                                                    parseInfo.lineNumber, parseInfo.name,
                                                                    header.getSatelliteSystem(),
                                                                    satellite.getSystem());
                                      }
                                      parseInfo.satObs.add(satellite);
                                      // note that we *must* use header.getSatelliteSystem() as it was used to set up parseInfo.mapTypeObs
                                      // and it may be MIXED to be applied to all satellites systems
                                      final int nbObservables = header.getTypeObs().get(header.getSatelliteSystem()).size();
                                      final int nbLines       = (nbObservables + MAX_OBS_PER_RINEX_2_LINE - 1) / MAX_OBS_PER_RINEX_2_LINE;
                                      parseInfo.nextObsStartLineNumber += nbLines;
                                  }
                              },
                              LineParser::first2),

        /** Parser for Rinex 2 data first line. */
        RINEX_2_DATA_FIRST((header, line) -> true,
                           (line, parseInfo) -> {

                               // flag
                               parseInfo.eventFlag = ParsingUtils.parseInt(line, 28, 1);

                               // number of sats
                               parseInfo.nbSatObs   = ParsingUtils.parseInt(line, 29, 3);
                               final int nbLinesSat = (parseInfo.nbSatObs + MAX_SAT_PER_RINEX_2_LINE - 1) / MAX_SAT_PER_RINEX_2_LINE;

                               if (parseInfo.eventFlag < 2) {
                                   // regular observation
                                   parseInfo.specialRecord = false;
                                   parseInfo.cycleSlip     = false;
                                   final int nbSat         = parseInfo.file.getHeader().getNbSat();
                                   if (nbSat != -1 && parseInfo.nbSatObs > nbSat) {
                                       // we check that the number of Sat in the observation is consistent
                                       throw new OrekitException(OrekitMessages.INCONSISTENT_NUMBER_OF_SATS,
                                                                 parseInfo.lineNumber, parseInfo.name,
                                                                 parseInfo.nbSatObs, nbSat);
                                   }
                                   parseInfo.nextObsStartLineNumber = parseInfo.lineNumber + nbLinesSat;

                                   // read the Receiver Clock offset, if present
                                   parseInfo.rcvrClkOffset = ParsingUtils.parseDouble(line, 68, 12);
                                   if (Double.isNaN(parseInfo.rcvrClkOffset)) {
                                       parseInfo.rcvrClkOffset = 0.0;
                                   }

                               } else if (parseInfo.eventFlag < 6) {
                                   // moving antenna / new site occupation / header information / external event
                                   // here, number of sats means number of lines to skip
                                   parseInfo.specialRecord = true;
                                   parseInfo.cycleSlip     = false;
                                   parseInfo.nextObsStartLineNumber = parseInfo.lineNumber + parseInfo.nbSatObs + 1;
                               } else if (parseInfo.eventFlag == 6) {
                                   // cycle slip, we will ignore it during observations parsing
                                   parseInfo.specialRecord = false;
                                   parseInfo.cycleSlip     = true;
                                   parseInfo.nextObsStartLineNumber = parseInfo.lineNumber + nbLinesSat;
                               } else {
                                   // unknown event flag
                                   throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                             parseInfo.lineNumber, parseInfo.name, line);
                               }

                               // parse the list of satellites observed
                               parseInfo.satObs.clear();
                               if (!parseInfo.specialRecord) {

                                   // observations epoch
                                   parseInfo.setTObs(new AbsoluteDate(
                                       ParsingUtils.convert2DigitsYear(ParsingUtils.parseInt(line, 1, 2)),
                                       ParsingUtils.parseInt(line, 4, 2),
                                       ParsingUtils.parseInt(line, 7, 2),
                                       ParsingUtils.parseInt(line, 10, 2),
                                       ParsingUtils.parseInt(line, 13, 2),
                                       ParsingUtils.parseDouble(line, 15, 11),
                                       parseInfo.timeScale));

                                   // satellites list
                                   RINEX_2_DATA_SAT_LIST.parsingMethod.parse(line, parseInfo);

                               }

                               // prepare handling of observations for current epoch
                               parseInfo.indexObsSat = 0;
                               parseInfo.observations.clear();

                           },
                           LineParser::first2),

        /** Parser for Rinex 2 special record. */
        RINEX_2_IGNORED_SPECIAL_RECORD((header, line) -> true,
                           (line, parseInfo) -> {
                               // nothing to do
                           },
                           LineParser::ignore2),

        /** Parser for Rinex 2 observation line. */
        RINEX_2_OBSERVATION((header, line) -> true,
                            (line, parseInfo) -> {
                                final List<ObservationType> types = parseInfo.file.getHeader().getTypeObs().get(parseInfo.file.getHeader().getSatelliteSystem());
                                for (int index = 0;
                                     parseInfo.observations.size() < types.size() && index < 80;
                                     index += 16) {
                                    final ObservationData observationData;
                                    if (parseInfo.cycleSlip) {
                                        // we are in a cycle slip data block (eventFlag = 6), we just ignore everything
                                        observationData = null;
                                    } else {
                                        // this is a regular observation line
                                        final ObservationType type    = types.get(parseInfo.observations.size());
                                        final double          scaling = getScaling(parseInfo, type, parseInfo.currentSystem);
                                        observationData = new ObservationData(type,
                                                                              scaling * ParsingUtils.parseDouble(line, index, 14),
                                                                              ParsingUtils.parseInt(line, index + 14, 1),
                                                                              ParsingUtils.parseInt(line, index + 15, 1));
                                    }
                                    parseInfo.observations.add(observationData);
                                }

                                if (parseInfo.observations.size() == types.size()) {
                                    // we have finished handling observations/cycle slips for one satellite
                                    if (!parseInfo.cycleSlip) {
                                        parseInfo.file.addObservationDataSet(new ObservationDataSet(parseInfo.satObs.get(parseInfo.indexObsSat),
                                                                                                    parseInfo.tObs,
                                                                                                    parseInfo.eventFlag,
                                                                                                    parseInfo.rcvrClkOffset,
                                                                                                    new ArrayList<>(parseInfo.observations)));
                                    }
                                    parseInfo.indexObsSat++;
                                    parseInfo.observations.clear();
                                }

                            },
                            LineParser::observation2),

        /** Parser for Rinex 3 observation line. */
        RINEX_3_OBSERVATION((header, line) -> true,
                            (line, parseInfo) -> {
                                final SatInSystem sat = new SatInSystem(line.substring(0, 3));
                                final List<ObservationType> types = parseInfo.file.getHeader().getTypeObs().get(sat.getSystem());
                                for (int index = 3;
                                     parseInfo.observations.size() < types.size();
                                     index += 16) {
                                    final ObservationData observationData;
                                    if (parseInfo.specialRecord || parseInfo.cycleSlip) {
                                        // we are in a special record (eventFlag < 6) or in a cycle slip data block (eventFlag = 6), we just ignore everything
                                        observationData = null;
                                    } else {
                                        // this is a regular observation line
                                        final ObservationType type    = types.get(parseInfo.observations.size());
                                        final double          scaling = getScaling(parseInfo, type, sat.getSystem());
                                        observationData = new ObservationData(type,
                                                                              scaling * ParsingUtils.parseDouble(line, index, 14),
                                                                              ParsingUtils.parseInt(line, index + 14, 1),
                                                                              ParsingUtils.parseInt(line, index + 15, 1));
                                    }
                                    parseInfo.observations.add(observationData);
                                }

                                if (!(parseInfo.specialRecord || parseInfo.cycleSlip)) {
                                    parseInfo.file.addObservationDataSet(new ObservationDataSet(sat,
                                                                                                parseInfo.tObs,
                                                                                                parseInfo.eventFlag,
                                                                                                parseInfo.rcvrClkOffset,
                                                                                                new ArrayList<>(parseInfo.observations)));
                                }
                                parseInfo.observations.clear();

                            },
                            LineParser::observation3),

        /** Parser for Rinex 3 data first line. */
        RINEX_3_DATA_FIRST((header, line) -> line.startsWith(">"),
                           (line, parseInfo) -> {

                               // flag
                               parseInfo.eventFlag = ParsingUtils.parseInt(line, 31, 1);

                               // number of sats
                               parseInfo.nbSatObs   = ParsingUtils.parseInt(line, 32, 3);

                               if (parseInfo.eventFlag < 2) {
                                   // regular observation
                                   parseInfo.specialRecord = false;
                                   parseInfo.cycleSlip     = false;
                                   final int nbSat         = parseInfo.file.getHeader().getNbSat();
                                   if (nbSat != -1 && parseInfo.nbSatObs > nbSat) {
                                       // we check that the number of Sat in the observation is consistent
                                       throw new OrekitException(OrekitMessages.INCONSISTENT_NUMBER_OF_SATS,
                                                                 parseInfo.lineNumber, parseInfo.name,
                                                                 parseInfo.nbSatObs, nbSat);
                                   }
                                   parseInfo.nextObsStartLineNumber = parseInfo.lineNumber + parseInfo.nbSatObs + 1;

                                   // read the Receiver Clock offset, if present
                                   parseInfo.rcvrClkOffset = ParsingUtils.parseDouble(line, 41, 15);
                                   if (Double.isNaN(parseInfo.rcvrClkOffset)) {
                                       parseInfo.rcvrClkOffset = 0.0;
                                   }

                               } else if (parseInfo.eventFlag < 6) {
                                   // moving antenna / new site occupation / header information / external event
                                   // here, number of sats means number of lines to skip
                                   parseInfo.specialRecord = true;
                                   parseInfo.cycleSlip     = false;
                                   parseInfo.nextObsStartLineNumber = parseInfo.lineNumber + parseInfo.nbSatObs + 1;
                               } else if (parseInfo.eventFlag == 6) {
                                   // cycle slip, we will ignore it during observations parsing
                                   parseInfo.specialRecord = false;
                                   parseInfo.cycleSlip     = true;
                                   parseInfo.nextObsStartLineNumber = parseInfo.lineNumber + parseInfo.nbSatObs + 1;
                               } else {
                                   // unknown event flag
                                   throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                             parseInfo.lineNumber, parseInfo.name, line);
                               }

                               // parse the list of satellites observed
                               parseInfo.satObs.clear();
                               if (!parseInfo.specialRecord) {

                                   // observations epoch
                                   parseInfo.setTObs(new AbsoluteDate(ParsingUtils.parseInt(line, 2, 4),
                                                                      ParsingUtils.parseInt(line, 7, 2),
                                                                      ParsingUtils.parseInt(line, 10, 2),
                                                                      ParsingUtils.parseInt(line, 13, 2),
                                                                      ParsingUtils.parseInt(line, 16, 2),
                                                                      ParsingUtils.parseDouble(line, 18, 11) +
                                                                      PICO_SECOND.toSI(ParsingUtils.parseInt(line, 57, 5)),
                                                                      parseInfo.timeScale));

                               }

                               // prepare handling of observations for current epoch
                               parseInfo.observations.clear();

                           },
                           parseInfo -> Collections.singleton(RINEX_3_OBSERVATION));


        /** Predicate for identifying lines that can be parsed. */
        private final BiFunction<RinexObservationHeader, String, Boolean> canHandle;

        /** Parsing method. */
        private final ParsingMethod parsingMethod;

        /** Provider for next line parsers. */
        private final Function<ParseInfo, Iterable<LineParser>> allowedNextProvider;

        /** Simple constructor.
         * @param canHandle predicate for identifying lines that can be parsed
         * @param parsingMethod parsing method
         * @param allowedNextProvider supplier for allowed parsers for next line
         */
        LineParser(final BiFunction<RinexObservationHeader, String, Boolean> canHandle, final ParsingMethod parsingMethod,
                   final Function<ParseInfo, Iterable<LineParser>> allowedNextProvider) {
            this.canHandle           = canHandle;
            this.parsingMethod       = parsingMethod;
            this.allowedNextProvider = allowedNextProvider;
        }

        /** Get the allowed parsers for next lines while parsing comments.
         * @param parseInfo holder for transient data
         * @return allowed parsers for next line
         */
        private static Iterable<LineParser> commentNext(final ParseInfo parseInfo) {
            return parseInfo.headerCompleted ? headerEndNext(parseInfo) : headerNext(parseInfo);
        }

        /** Get the allowed parsers for next lines while parsing Rinex header.
         * @param parseInfo holder for transient data
         * @return allowed parsers for next line
         */
        private static Iterable<LineParser> headerNext(final ParseInfo parseInfo) {
            if (parseInfo.file.getHeader().getFormatVersion() < 3) {
                // Rinex 2.x header entries
                return Arrays.asList(PROGRAM, COMMENT, MARKER_NAME, MARKER_NUMBER, MARKER_TYPE, OBSERVER_AGENCY,
                                     REC_NB_TYPE_VERS, ANT_NB_TYPE, APPROX_POSITION_XYZ, ANTENNA_DELTA_H_E_N,
                                     ANTENNA_DELTA_X_Y_Z, ANTENNA_B_SIGHT_XYZ, WAVELENGTH_FACT_L1_2, OBS_SCALE_FACTOR,
                                     CENTER_OF_MASS_XYZ, SYS_NB_TYPES_OF_OBSERV, INTERVAL, TIME_OF_FIRST_OBS, TIME_OF_LAST_OBS,
                                     RCV_CLOCK_OFFS_APPL, LEAP_SECONDS, NB_OF_SATELLITES, PRN_NB_OF_OBS, END);
            } else if (parseInfo.file.getHeader().getFormatVersion() < 4) {
                // Rinex 3.x header entries
                return Arrays.asList(PROGRAM, COMMENT, MARKER_NAME, MARKER_NUMBER, MARKER_TYPE, OBSERVER_AGENCY,
                                     REC_NB_TYPE_VERS, ANT_NB_TYPE, APPROX_POSITION_XYZ, ANTENNA_DELTA_H_E_N,
                                     ANTENNA_DELTA_X_Y_Z, ANTENNA_PHASE_CENTER, ANTENNA_B_SIGHT_XYZ, ANTENNA_ZERODIR_AZI,
                                     ANTENNA_ZERODIR_XYZ, CENTER_OF_MASS_XYZ, SYS_NB_TYPES_OF_OBSERV, SIGNAL_STRENGTH_UNIT,
                                     INTERVAL, TIME_OF_FIRST_OBS, TIME_OF_LAST_OBS, RCV_CLOCK_OFFS_APPL,
                                     SYS_DCBS_APPLIED, SYS_PCVS_APPLIED, SYS_SCALE_FACTOR, SYS_PHASE_SHIFT,
                                     GLONASS_SLOT_FRQ_NB, GLONASS_COD_PHS_BIS, LEAP_SECONDS, NB_OF_SATELLITES,
                                     PRN_NB_OF_OBS, END);
            } else {
                // Rinex 4.x header entries
                return Arrays.asList(PROGRAM, COMMENT, MARKER_NAME, MARKER_NUMBER, MARKER_TYPE, OBSERVER_AGENCY,
                                     REC_NB_TYPE_VERS, ANT_NB_TYPE, APPROX_POSITION_XYZ, ANTENNA_DELTA_H_E_N,
                                     ANTENNA_DELTA_X_Y_Z, ANTENNA_PHASE_CENTER, ANTENNA_B_SIGHT_XYZ, ANTENNA_ZERODIR_AZI,
                                     ANTENNA_ZERODIR_XYZ, CENTER_OF_MASS_XYZ, DOI, LICENSE, STATION_INFORMATION,
                                     SYS_NB_TYPES_OF_OBSERV, SIGNAL_STRENGTH_UNIT, INTERVAL, TIME_OF_FIRST_OBS, TIME_OF_LAST_OBS,
                                     RCV_CLOCK_OFFS_APPL, SYS_DCBS_APPLIED, SYS_PCVS_APPLIED, SYS_SCALE_FACTOR, SYS_PHASE_SHIFT,
                                     GLONASS_SLOT_FRQ_NB, GLONASS_COD_PHS_BIS, LEAP_SECONDS, NB_OF_SATELLITES,
                                     PRN_NB_OF_OBS, END);
            }
        }

        /** Get the allowed parsers for next lines while parsing header end.
         * @param parseInfo holder for transient data
         * @return allowed parsers for next line
         */
        private static Iterable<LineParser> headerEndNext(final ParseInfo parseInfo) {
            return Collections.singleton(parseInfo.file.getHeader().getFormatVersion() < 3 ?
                                         RINEX_2_DATA_FIRST : RINEX_3_DATA_FIRST);
        }

        /** Get the allowed parsers for next lines while parsing types of observations.
         * @param parseInfo holder for transient data
         * @return allowed parsers for next line
         */
        private static Iterable<LineParser> headerNbTypesObs(final ParseInfo parseInfo) {
            if (parseInfo.typesObs.size() < parseInfo.nbTypes) {
                return Arrays.asList(COMMENT, SYS_NB_TYPES_OF_OBSERV);
            } else {
                return headerNext(parseInfo);
            }
        }

        /** Get the allowed parsers for next lines while parsing phase shifts.
         * @param parseInfo holder for transient data
         * @return allowed parsers for next line
         */
        private static Iterable<LineParser> headerPhaseShift(final ParseInfo parseInfo) {
            if (parseInfo.satPhaseShift.size() < parseInfo.phaseShiftNbSat) {
                return Arrays.asList(COMMENT, SYS_PHASE_SHIFT);
            } else {
                return headerNext(parseInfo);
            }
        }

        /** Get the allowed parsers for next lines while parsing Rinex 2 observations first lines.
         * @param parseInfo holder for transient data
         * @return allowed parsers for next line
         */
        private static Iterable<LineParser> first2(final ParseInfo parseInfo) {
            if (parseInfo.specialRecord) {
                return Collections.singleton(RINEX_2_IGNORED_SPECIAL_RECORD);
            } else if (parseInfo.satObs.size() < parseInfo.nbSatObs) {
                return Collections.singleton(RINEX_2_DATA_SAT_LIST);
            } else {
                return Collections.singleton(RINEX_2_OBSERVATION);
            }
        }

        /** Get the allowed parsers for next lines while parsing Rinex 2 ignored special records.
         * @param parseInfo holder for transient data
         * @return allowed parsers for next line
         */
        private static Iterable<LineParser> ignore2(final ParseInfo parseInfo) {
            if (parseInfo.lineNumber < parseInfo.nextObsStartLineNumber) {
                return Collections.singleton(RINEX_2_IGNORED_SPECIAL_RECORD);
            } else {
                return Arrays.asList(COMMENT, RINEX_2_DATA_FIRST);
            }
        }

        /** Get the allowed parsers for next lines while parsing Rinex 2 observations per se.
         * @param parseInfo holder for transient data
         * @return allowed parsers for next line
         */
        private static Iterable<LineParser> observation2(final ParseInfo parseInfo) {
            if (parseInfo.lineNumber < parseInfo.nextObsStartLineNumber) {
                return Collections.singleton(RINEX_2_OBSERVATION);
            } else {
                return Arrays.asList(COMMENT, RINEX_2_DATA_FIRST);
            }
        }

        /** Get the allowed parsers for next lines while parsing Rinex 3 observations.
         * @param parseInfo holder for transient data
         * @return allowed parsers for next line
         */
        private static Iterable<LineParser> observation3(final ParseInfo parseInfo) {
            if (parseInfo.lineNumber < parseInfo.nextObsStartLineNumber) {
                return Collections.singleton(RINEX_3_OBSERVATION);
            } else {
                return Arrays.asList(COMMENT, RINEX_3_DATA_FIRST);
            }
        }

        /** Get the scaling factor for an observation.
         * @param parseInfo holder for transient data
         * @param type type of observation
         * @param system satellite system for the observation
         * @return scaling factor
         */
        private static double getScaling(final ParseInfo parseInfo, final ObservationType type,
                                         final SatelliteSystem system) {

            for (final ScaleFactorCorrection scaleFactorCorrection :
                parseInfo.file.getHeader().getScaleFactorCorrections(system)) {
                // check if the next Observation Type to read needs to be scaled
                if (scaleFactorCorrection.getTypesObsScaled().contains(type)) {
                    return 1.0 / scaleFactorCorrection.getCorrection();
                }
            }

            // no scaling
            return 1.0;

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
