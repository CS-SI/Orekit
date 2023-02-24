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
package org.orekit.gnss;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.geometry.euclidean.twod.Vector2D;
import org.hipparchus.util.FastMath;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScales;

/** Loader for Rinex measurements files.
 * <p>
 * Supported versions are: 2.00, 2.10, 2.11, 2.12 (unofficial), 2.20 (unofficial),
 * 3.00, 3.01, 3.02, 3.03, 3.04, and 3.05.
 * </p>
 * @see <a href="https://files.igs.org/pub/data/format/rinex2.pdf">rinex 2.0</a>
 * @see <a href="https://files.igs.org/pub/data/format/rinex210.pdf">rinex 2.10</a>
 * @see <a href="https://files.igs.org/pub/data/format/rinex211.pdf">rinex 2.11</a>
 * @see <a href="http://www.aiub.unibe.ch/download/rinex/rinex212.txt">unofficial rinex 2.12</a>
 * @see <a href="http://www.aiub.unibe.ch/download/rinex/rnx_leo.txt">unofficial rinex 2.20</a>
 * @see <a href="https://files.igs.org/pub/data/format/rinex300.pdf">rinex 3.00</a>
 * @see <a href="https://files.igs.org/pub/data/format/rinex301.pdf">rinex 3.01</a>
 * @see <a href="https://files.igs.org/pub/data/format/rinex302.pdf">rinex 3.02</a>
 * @see <a href="https://files.igs.org/pub/data/format/rinex303.pdf">rinex 3.03</a>
 * @see <a href="https://files.igs.org/pub/data/format/rinex304.pdf">rinex 3.04</a>
 * @see <a href="https://files.igs.org/pub/data/format/rinex305.pdf">rinex 3.05</a>
 * @since 9.2
 */
public class RinexObservationLoader {

    /** Default name pattern for rinex 2 observation files. */
    public static final String DEFAULT_RINEX_2_NAMES = "^\\w{4}\\d{3}[0a-x](?:\\d{2})?\\.\\d{2}[oO]$";

    /** Default name pattern for rinex 3 observation files. */
    public static final String DEFAULT_RINEX_3_NAMES = "^\\w{9}_\\w{1}_\\d{11}_\\d{2}\\w_\\d{2}\\w{1}_\\w{2}\\.rnx$";

    /** GPS time scale. */
    private static final String GPS = "GPS";

    /** Galileo time scale. */
    private static final String GAL = "GAL";

    /** GLONASS time scale. */
    private static final String GLO = "GLO";

    /** QZSS time scale. */
    private static final String QZS = "QZS";

    /** Beidou time scale. */
    private static final String BDT = "BDT";

    /** IRNSS time scale. */
    private static final String IRN = "IRN";

    /** Maximum number of satellites per line in Rinex 2 format . */
    private static final int MAX_SAT_PER_RINEX_2_LINE = 12;

    /** Maximum number of observations per line in Rinex 2 format. */
    private static final int MAX_OBS_PER_RINEX_2_LINE = 5;

    /** Set of time scales. */
    private final TimeScales timeScales;

    /** Simple constructor.
     * <p>
     * <p>This constructor uses the {@link DataContext#getDefault() default data context}.</p>
     * </p>
     */
    @DefaultDataContext
    public RinexObservationLoader() {
        this(DataContext.getDefault().getTimeScales());
    }

    /**
     * Create a RINEX loader/parser with the given source of RINEX auxiliary data files.
     * @param timeScales the set of time scales to use when parsing dates.
     * @since 12.0
     */
    public RinexObservationLoader(final TimeScales timeScales) {
        this.timeScales = timeScales;
    }

    /**
     * Parse RINEX observations messages.
     * @param source source providing the data to parse
     * @return parsed observations
     */
    public List<ObservationDataSet> parse(final DataSource source) {

        Stream<LineParser> candidateParsers = Stream.of(LineParser.HEADER_VERSION);

        // placeholders for parsed data
        final ParseInfo parseInfo = new ParseInfo(source.getName());

        try (Reader reader = source.getOpener().openReaderOnce();
             BufferedReader br = new BufferedReader(reader)) {
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                ++parseInfo.lineNumber;
                final String l = line;
                final Optional<LineParser> selected = candidateParsers.filter(p -> p.canHandle.test(l)).findFirst();
                if (selected.isPresent()) {
                    try {
                        selected.get().parsingMethod.parse(line, parseInfo);
                    } catch (StringIndexOutOfBoundsException | NumberFormatException | InputMismatchException e) {
                        throw new OrekitException(e,
                                                  OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                  parseInfo.lineNumber, source.getName(), line);
                    }
                    candidateParsers = selected.get().allowedNextProvider.apply(parseInfo);
                } else {
                    throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                              parseInfo.lineNumber, source.getName(), line);
                }
            }
        } catch (IOException ioe) {
            throw new OrekitException(ioe, LocalizedCoreFormats.SIMPLE_MESSAGE, ioe.getLocalizedMessage());
        }

        return parseInfo.observationDataSets;

    }

    private void f() {
        //First line must  always contain Rinex Version, File Type and Satellite Systems Observed
        readLine(reader, true);
        if (line.length() < LABEL_START || !RINEX_VERSION_TYPE.equals(line.substring(LABEL_START).trim())) {
            throw new OrekitException(OrekitMessages.UNSUPPORTED_FILE_FORMAT, name);
        }
        RinexUtils.parseVersionFileTypeSatelliteSystem(line, fileName, rinexHeader,
                                                       2.0, 2.1, 2.11, 2.12, 2.20, 3.0, 3.01, 3.02, 3.03, 3.04);
        inRinexVersion = true;

        switch ((int) FastMath.floor(rinexHeader.getFormatVersion())) {
            case 2: {

                final List<ObservationType> typesObs = new ArrayList<>();

                while (readLine(reader, false)) {

                    for (int j = 0; j < nbLinesSat; j++) {
                        if (j > 0) {
                            readLine(reader, true);
                        }
                        final int iMax = FastMath.min(MAX_N_SAT_OBSERVATION, nbSatObs  - j * MAX_N_SAT_OBSERVATION);
                        for (int i = 0; i < iMax; i++) {
                            satsObsList[i + MAX_N_SAT_OBSERVATION * j] = RinexUtils.parseString(line, 32 + 3 * i, 3);
                        }

                        //Read the Receiver Clock offset, if present
                        rcvrClkOffset = RinexUtils.parseDouble(line, 68, 12);
                        if (Double.isNaN(rcvrClkOffset)) {
                            rcvrClkOffset = 0.0;
                        }

                    }

                    //For each one of the Satellites in this observation
                    final int nbLinesObs = (nbTypes + MAX_N_TYPES_OBSERVATION - 1) / MAX_N_TYPES_OBSERVATION;
                    for (int k = 0; k < nbSatObs; k++) {


                        //Once the Date and Satellites list is read:
                        //  - to read the Data for each satellite
                        //  - 5 Observations per line
                        final List<ObservationData> observationData = new ArrayList<>(nbSatObs);
                        for (int j = 0; j < nbLinesObs; j++) {
                            readLine(reader, true);
                            final int iMax = FastMath.min(MAX_N_TYPES_OBSERVATION, nbTypes - observationData.size());
                            for (int i = 0; i < iMax; i++) {
                                final ObservationType type = typesObs.get(observationData.size());
                                double value = RinexUtils.parseDouble(line, 16 * i, 14);
                                boolean scaleFactorFound = false;
                                //We look for the lines of ScaledFactorCorrections
                                for (int l = 0; l < scaleFactorCorrections.size() && !scaleFactorFound; ++l) {
                                    //We check if the next Observation Type to read needs to be scaled
                                    if (scaleFactorCorrections.get(l).getTypesObsScaled().contains(type)) {
                                        value /= scaleFactorCorrections.get(l).getCorrection();
                                        scaleFactorFound = true;
                                    }
                                }
                                observationData.add(new ObservationData(type,
                                                                        value,
                                                                        RinexUtils.parseInt(line, 14 + 16 * i, 1),
                                                                        RinexUtils.parseInt(line, 15 + 16 * i, 1)));
                            }
                        }

                        //We check that the Satellite type is consistent with Satellite System in the top of the file
                        final SatelliteSystem satelliteSystemSat;
                        final int id;
                        if (satsObsList[k].length() < 3) {
                            // missing satellite system, we use the global one
                            satelliteSystemSat = rinexHeader.getSatelliteSystem();
                            id                 = Integer.parseInt(satsObsList[k]);
                        } else {
                            satelliteSystemSat = SatelliteSystem.parseSatelliteSystem(satsObsList[k]);
                            id                 = Integer.parseInt(satsObsList[k].substring(1, 3).trim());
                        }
                        if (rinexHeader.getSatelliteSystem() != SatelliteSystem.MIXED) {
                            if (satelliteSystemSat != rinexHeader.getSatelliteSystem()) {
                                throw new OrekitException(OrekitMessages.INCONSISTENT_SATELLITE_SYSTEM,
                                                          lineNumber, name,
                                                          rinexHeader.getSatelliteSystem(),
                                                          satelliteSystemSat);
                            }
                        }

                        final int prnNumber;
                        switch (satelliteSystemSat) {
                            case GPS:
                            case GLONASS:
                            case GALILEO:
                                prnNumber = id;
                                break;
                            case SBAS:
                                prnNumber = id + 100;
                                break;
                            default:
                                // MIXED satellite system is not allowed here
                                throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                          lineNumber, name, line);
                        }

                        observationDataSets.add(new ObservationDataSet(rinexHeader, satelliteSystemSat, prnNumber,
                                                                       tObs, rcvrClkOffset, observationData));

                        break;
                    }
                }
            }
            case 3: {

                while (readLine(reader, false)) {
                        //If End of Header

                        //Start of a new Observation
                        rcvrClkOffset     =  0;
                        eventFlag         = -1;
                        nbSatObs          = -1;
                        tObs              = null;

                        //A line that starts with ">" correspond to a new observation epoch
                        if (RinexUtils.parseString(line, 0, 1).equals(">")) {

                            eventFlag = RinexUtils.parseInt(line, 31, 1);
                            //If eventFlag>1, we skip the corresponding lines to the next observation
                            if (eventFlag != 0) {
                                final int nbLinesSkip = RinexUtils.parseInt(line, 32, 3);
                                for (int i = 0; i < nbLinesSkip; i++) {
                                    readLine(reader, true);
                                }
                            } else {

                                tObs = new AbsoluteDate(RinexUtils.parseInt(line, 2, 4),
                                                        RinexUtils.parseInt(line, 6, 3),
                                                        RinexUtils.parseInt(line, 9, 3),
                                                        RinexUtils.parseInt(line, 12, 3),
                                                        RinexUtils.parseInt(line, 15, 3),
                                                        RinexUtils.parseDouble(line, 18, 11), timeScale);

                                nbSatObs  = RinexUtils.parseInt(line, 32, 3);
                                //If the total number of satellites was indicated in the Header
                                if (nbSat != -1 && nbSatObs > nbSat) {
                                    //we check that the number of Sat in the observation is consistent
                                    throw new OrekitException(OrekitMessages.INCONSISTENT_NUMBER_OF_SATS,
                                                              lineNumber, name, nbSatObs, nbSat);
                                }
                                //Read the Receiver Clock offset, if present
                                rcvrClkOffset = RinexUtils.parseDouble(line, 41, 15);
                                if (Double.isNaN(rcvrClkOffset)) {
                                    rcvrClkOffset = 0.0;
                                }

                                //For each one of the Satellites in this Observation
                                for (int i = 0; i < nbSatObs; i++) {

                                    readLine(reader, true);

                                    //We check that the Satellite type is consistent with Satellite System in the top of the file
                                    final SatelliteSystem satelliteSystemSat = SatelliteSystem.parseSatelliteSystem(RinexUtils.parseString(line, 0, 1));
                                    if (rinexHeader.getSatelliteSystem() != SatelliteSystem.MIXED) {
                                        if (satelliteSystemSat != rinexHeader.getSatelliteSystem()) {
                                            throw new OrekitException(OrekitMessages.INCONSISTENT_SATELLITE_SYSTEM,
                                                                      lineNumber, name,
                                                                      rinexHeader.getSatelliteSystem(),
                                                                      satelliteSystemSat);
                                        }
                                    }

                                    final int prn = RinexUtils.parseInt(line, 1, 2);
                                    final int prnNumber;
                                    switch (satelliteSystemSat) {
                                        case GPS:
                                        case GLONASS:
                                        case GALILEO:
                                        case BEIDOU:
                                        case IRNSS:
                                            prnNumber = prn;
                                            break;
                                        case QZSS:
                                            prnNumber = prn + 192;
                                            break;
                                        case SBAS:
                                            prnNumber = prn + 100;
                                            break;
                                        default:
                                            // MIXED satellite system is not allowed here
                                            throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                                      lineNumber, name, line);
                                    }
                                    final List<ObservationData> observationData = new ArrayList<>(nbSatObs);
                                    for (int j = 0; j < listTypeObs.get(satelliteSystemSat).size(); j++) {
                                        final ObservationType rf = listTypeObs.get(satelliteSystemSat).get(j);
                                        boolean scaleFactorFound = false;
                                        //We look for the lines of ScaledFactorCorrections that correspond to this SatSystem
                                        int k = 0;
                                        double value = RinexUtils.parseDouble(line, 3 + j * 16, 14);
                                        while (k < scaleFactorCorrections.size() && !scaleFactorFound) {
                                            if (scaleFactorCorrections.get(k).getSatelliteSystem().equals(satelliteSystemSat)) {
                                                //We check if the next Observation Type to read needs to be scaled
                                                if (scaleFactorCorrections.get(k).getTypesObsScaled().contains(rf)) {
                                                    value /= scaleFactorCorrections.get(k).getCorrection();
                                                    scaleFactorFound = true;
                                                }
                                            }
                                            k++;
                                        }
                                        observationData.add(new ObservationData(rf,
                                                                                value,
                                                                                RinexUtils.parseInt(line, 17 + j * 16, 1),
                                                                                RinexUtils.parseInt(line, 18 + j * 16, 1)));
                                    }
                                    observationDataSets.add(new ObservationDataSet(rinexHeader, satelliteSystemSat, prnNumber,
                                                                                   tObs, rcvrClkOffset, observationData));

                                }
                            }
                        }
                }
                break;
            }
            default:
                //If RINEX Version is neither 2 nor 3
                throw new OrekitException(OrekitMessages.UNSUPPORTED_FILE_FORMAT, name);
        }
    }

    /** Transient data used for parsing a RINEX observation messages file.
     * @since 12.0
     */
    private class ParseInfo {

        /** Name of the data source. */
        private final String name;

        /** Set of time scales for parsing dates. */
        private final TimeScales timeScales;

        /** Current line number of the navigation message. */
        private int lineNumber;

        /** Rinex header associated with this data set. */
        private final RinexObservationHeader header;

        /** List of Observation data sets. */
        private final List<ObservationDataSet> observationDataSets;

        /** Satellite System. */
        private SatelliteSystem satelliteSystem;

        /** PRN Number of the satellite observed. */
        private int prnNumber;

        /** Date of the observation. */
        private AbsoluteDate tObs;

        /** Receiver clock offset (seconds). */
        private double rcvrClkOffset;

        private TimeScale                         timeScale;
        private String                            timeScaleStr;
        private String[]                          satsObsList;
        private int                               eventFlag;
        private int                               nbTypes;
        private int                               nbSat;
        private int                               nbSatObs;
        private int                               nbLinesSat;
        private int                               scaleFactor;
        private int                               nbObsScaleFactor;
        private int                               nextObsLineNumber;
        private SatelliteSystem                   currentSystem;
        private int                               phaseShiftNbSat;
        private ObservationType                   phaseShiftTypeObs;
        private double                            corrPhaseShift;
        private final List<ScaleFactorCorrection> scaleFactorCorrections;
        private final Map<SatelliteSystem, List<ObservationType>> listTypeObs;
        private final List<ObservationType>                       typesObs;
        private final List<String>                                satPhaseShift;
        private final List<ObservationType>                       typesObsScaleFactor;
        private final List<String>                                satObs;

        /** Constructor, build the ParseInfo object.
         * @param name name of the data source
         */
        ParseInfo(final String name) {
            // Initialize default values for fields
            this.name                   = name;
            this.timeScales             = RinexObservationLoader.this.timeScales;
            this.header                 = new RinexObservationHeader();
            this.observationDataSets    = new ArrayList<>();
            this.lineNumber             = 0;
            this.tObs                   = AbsoluteDate.PAST_INFINITY;
            this.timeScale              = null;
            this.timeScaleStr           = null;
            this.satsObsList            = null;
            this.eventFlag              = -1;
            this.nbTypes                = -1;
            this.nbSat                  = -1;
            this.nbSatObs               = -1;
            this.nbLinesSat             = -1;
            this.nbObsScaleFactor       = -1;
            this.scaleFactorCorrections = new ArrayList<>();
            this.listTypeObs            = new HashMap<>();
            this.typesObs               = new ArrayList<>();
            this.satPhaseShift          = new ArrayList<>();
            this.typesObsScaleFactor    = new ArrayList<>();
            this.satObs                 = new ArrayList<>();
        }

    }

    /** Parsers for specific lines. */
    private enum LineParser {

        /** Parser for version, file type and satellite system. */
        HEADER_VERSION(line -> RinexUtils.matchesLabel(line, "RINEX VERSION / TYPE"),
                       (line, parseInfo) ->  RinexUtils.parseVersionFileTypeSatelliteSystem(line, parseInfo.name, parseInfo.header,
                                                                                            2.00, 2.10, 2.11, 2.12, 2.20,
                                                                                            3.00, 3.01, 3.02, 3.03, 3.04, 3.05),
                       LineParser::headerNext),

        /** Parser for generating program and emiting agency. */
        HEADER_PROGRAM(line -> RinexUtils.matchesLabel(line, "PGM / RUN BY / DATE"),
                       (line, parseInfo) -> RinexUtils.parseProgramRunByDate(line, parseInfo.lineNumber, parseInfo.name,
                                                                             parseInfo.timeScales, parseInfo.header),
                       LineParser::headerNext),

        /** Parser for comments. */
        HEADER_COMMENT(line -> RinexUtils.matchesLabel(line, "COMMENT"),
                       (line, parseInfo) -> RinexUtils.parseComment(line, parseInfo.header),
                       LineParser::headerNext),

        /** Parser for marker name. */
        MARKER_NAME(line -> RinexUtils.matchesLabel(line, "MARKER NAME"),
                    (line, parseInfo) ->  parseInfo.header.setMarkerName(RinexUtils.parseString(line, 0, 60)),
                    LineParser::headerNext),

        /** Parser for marker number. */
        MARKER_NUMBER(line -> RinexUtils.matchesLabel(line, "MARKER NUMBER"),
                      (line, parseInfo) -> parseInfo.header.setMarkerNumber(RinexUtils.parseString(line, 0, 20)),
                      LineParser::headerNext),

        /** Parser for marker type. */
        MARKER_TYPE(line -> RinexUtils.matchesLabel(line, "MARKER TYPE"),
                    (line, parseInfo) -> parseInfo.header.setMarkerType(RinexUtils.parseString(line, 0, 20)),
                    LineParser::headerNext),

        /** Parser for observer agency. */
        OBSERVER_AGENCY(line -> RinexUtils.matchesLabel(line, "OBSERVER / AGENCY"),
                        (line, parseInfo) -> {
                            parseInfo.header.setObserverName(RinexUtils.parseString(line, 0, 20));
                            parseInfo.header.setAgencyName(RinexUtils.parseString(line, 20, 40));
                        },
                        LineParser::headerNext),

        /** Parser for receiver tnumber, type and version. */
        REC_NB_TYPE_VERS(line -> RinexUtils.matchesLabel(line, "REC # / TYPE / VERS"),
                         (line, parseInfo) -> {
                             parseInfo.header.setReceiverNumber(RinexUtils.parseString(line, 0, 20));
                             parseInfo.header.setReceiverType(RinexUtils.parseString(line, 20, 20));
                             parseInfo.header.setReceiverVersion(RinexUtils.parseString(line, 40, 20));
                         },
                         LineParser::headerNext),

        /** Parser for antenna number and type. */
        ANT_NB_TYPE(line -> RinexUtils.matchesLabel(line, "ANT # / TYPE"),
                    (line, parseInfo) -> {
                        parseInfo.header.setAntennaNumber(RinexUtils.parseString(line, 0, 20));
                        parseInfo.header.setAntennaType(RinexUtils.parseString(line, 20, 20));
                    },
                    LineParser::headerNext),

        /** Parser for approximative position. */
        APPROX_POSITION_XYZ(line -> RinexUtils.matchesLabel(line, "APPROX POSITION XYZ"),
                            (line, parseInfo) -> {
                                parseInfo.header.setApproxPos(new Vector3D(RinexUtils.parseDouble(line, 0, 14),
                                                                           RinexUtils.parseDouble(line, 14, 14),
                                                                           RinexUtils.parseDouble(line, 28, 14)));
                            },
                            LineParser::headerNext),

        /** Parser for antenna reference point. */
        ANTENNA_DELTA_H_E_N(line -> RinexUtils.matchesLabel(line, "ANTENNA: DELTA H/E/N"),
                            (line, parseInfo) -> {
                                parseInfo.header.setAntennaHeight(RinexUtils.parseDouble(line, 0, 14));
                                parseInfo.header.setEccentricities(new Vector2D(RinexUtils.parseDouble(line, 14, 14),
                                                                                RinexUtils.parseDouble(line, 28, 14)));
                            },
                            LineParser::headerNext),

        /** Parser for antenna reference point. */
        ANTENNA_DELTA_X_Y_Z(line -> RinexUtils.matchesLabel(line, "ANTENNA: DELTA X/Y/Z"),
                            (line, parseInfo) -> {
                                parseInfo.header.setAntennaReferencePoint(new Vector3D(RinexUtils.parseDouble(line,  0, 14),
                                                                                       RinexUtils.parseDouble(line, 14, 14),
                                                                                       RinexUtils.parseDouble(line, 28, 14)));
                            },
                            LineParser::headerNext),

        /** Parser for antenna phase center. */
        ANTENNA_PHASECENTER(line -> RinexUtils.matchesLabel(line, "ANTENNA: PHASECENTER"),
                            (line, parseInfo) -> {
                                parseInfo.header.setObservationCode(RinexUtils.parseString(line, 2, 3));
                                parseInfo.header.setAntennaPhaseCenter(new Vector3D(RinexUtils.parseDouble(line, 5, 9),
                                                                                    RinexUtils.parseDouble(line, 14, 14),
                                                                                    RinexUtils.parseDouble(line, 28, 14)));
                            },
                            LineParser::headerNext),

        /** Parser for antenna bore sight. */
        ANTENNA_B_SIGHT_XYZ(line -> RinexUtils.matchesLabel(line, "ANTENNA: B.SIGHT XYZ"),
                            (line, parseInfo) -> {
                                parseInfo.header.setAntennaBSight(new Vector3D(RinexUtils.parseDouble(line,  0, 14),
                                                                               RinexUtils.parseDouble(line, 14, 14),
                                                                               RinexUtils.parseDouble(line, 28, 14)));
                            },
                            LineParser::headerNext),

        /** Parser for antenna zero direction. */
        ANTENNA_ZERODIR_AZI(line -> RinexUtils.matchesLabel(line, "ANTENNA: ZERODIR AZI"),
                            (line, parseInfo) -> parseInfo.header.setAntennaAzimuth(RinexUtils.parseDouble(line, 0, 14)),
                            LineParser::headerNext),

        /** Parser for antenna zero direction. */
        ANTENNA_ZERODIR_XYZ(line -> RinexUtils.matchesLabel(line, "ANTENNA: ZERODIR XYZ"),
                            (line, parseInfo) -> parseInfo.header.setAntennaZeroDirection(new Vector3D(RinexUtils.parseDouble(line, 0, 14),
                                                                                                       RinexUtils.parseDouble(line, 14, 14),
                                                                                                       RinexUtils.parseDouble(line, 28, 14))),
                            LineParser::headerNext),

        /** Parser for wavelength factors. */
        WAVELENGTH_FACT_L1_2(line -> RinexUtils.matchesLabel(line, "WAVELENGTH FACT L1/2"),
                             (line, parseInfo) -> {
                                 // optional line in Rinex 2 header, not stored for now
                             },
                             LineParser::headerNext),

        /** Parser for indicator of receiver clock offset application. */
        RCV_CLOCK_OFFS_APPL(line -> RinexUtils.matchesLabel(line, "RCV CLOCK OFFS APPL"),
                            (line, parseInfo) -> parseInfo.header.setClkOffset(RinexUtils.parseInt(line, 0, 6)),
                            LineParser::headerNext),

        /** Parser for observation interval. */
        INTERVAL(line -> RinexUtils.matchesLabel(line, "INTERVAL"),
                 (line, parseInfo) -> parseInfo.header.setInterval(RinexUtils.parseDouble(line, 0, 10)),
                 LineParser::headerNext),

        /** Parser for time of first observation. */
        TIME_OF_FIRST_OBS(line -> RinexUtils.matchesLabel(line, "TIME OF FIRST OBS"),
                          (line, parseInfo) -> {
                              switch (parseInfo.header.getSatelliteSystem()) {
                                  case GPS:
                                      parseInfo.timeScale = parseInfo.timeScales.getGPS();
                                      break;
                                  case GALILEO:
                                      parseInfo.timeScale = parseInfo.timeScales.getGST();
                                      break;
                                  case GLONASS:
                                      parseInfo.timeScale = parseInfo.timeScales.getGLONASS();
                                      break;
                                  case QZSS:
                                      parseInfo.timeScale = parseInfo.timeScales.getQZSS();
                                      break;
                                  case BEIDOU:
                                      parseInfo.timeScale = parseInfo.timeScales.getBDT();
                                      break;
                                  case IRNSS:
                                      parseInfo.timeScale = parseInfo.timeScales.getIRNSS();
                                      break;
                                  case MIXED: {
                                      // in case of mixed data, time scale must be specified in the Time of First line
                                      final String timeScaleStr = RinexUtils.parseString(line, 48, 3);

                                      if (timeScaleStr.equals(GPS)) {
                                          parseInfo.timeScale = parseInfo.timeScales.getGPS();
                                      } else if (timeScaleStr.equals(GAL)) {
                                          parseInfo.timeScale = parseInfo.timeScales.getGST();
                                      } else if (timeScaleStr.equals(GLO)) {
                                          parseInfo.timeScale = parseInfo.timeScales.getGLONASS();
                                      } else if (timeScaleStr.equals(QZS)) {
                                          parseInfo.timeScale = parseInfo.timeScales.getQZSS();
                                      } else if (timeScaleStr.equals(BDT)) {
                                          parseInfo.timeScale = parseInfo.timeScales.getBDT();
                                      } else if (timeScaleStr.equals(IRN)) {
                                          parseInfo.timeScale = parseInfo.timeScales.getIRNSS();
                                      } else {
                                          throw new OrekitException(OrekitMessages.UNSUPPORTED_FILE_FORMAT, parseInfo.name);
                                      }
                                      break;
                                  }
                                  default :
                                      throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                                parseInfo.lineNumber, parseInfo.name, line);
                              }
                              parseInfo.header.setTFirstObs(new AbsoluteDate(RinexUtils.parseInt(line, 0, 6),
                                                                             RinexUtils.parseInt(line, 6, 6),
                                                                             RinexUtils.parseInt(line, 12, 6),
                                                                             RinexUtils.parseInt(line, 18, 6),
                                                                             RinexUtils.parseInt(line, 24, 6),
                                                                             RinexUtils.parseDouble(line, 30, 13),
                                                                             parseInfo.timeScale));
                          },
                          LineParser::headerNext),

        /** Parser for time of last observation. */
        TIME_OF_LAST_OBS(line -> RinexUtils.matchesLabel(line, "TIME OF LAST OBS"),
                         (line, parseInfo) -> {
                             parseInfo.header.setTLastObs(new AbsoluteDate(RinexUtils.parseInt(line, 0, 6),
                                                                           RinexUtils.parseInt(line, 6, 6),
                                                                           RinexUtils.parseInt(line, 12, 6),
                                                                           RinexUtils.parseInt(line, 18, 6),
                                                                           RinexUtils.parseInt(line, 24, 6),
                                                                           RinexUtils.parseDouble(line, 30, 13),
                                                                           parseInfo.timeScale));
                         },
                         LineParser::headerNext),

        /** Parser for leap seconds. */
        LEAP_SECONDS(line -> RinexUtils.matchesLabel(line, "LEAP SECONDS"),
                     (line, parseInfo) -> {
                         parseInfo.header.setLeapSeconds(RinexUtils.parseInt(line, 0, 6));
                         if (parseInfo.header.getFormatVersion() >= 3.0) {
                             parseInfo.header.setLeapSecondsFuture(RinexUtils.parseInt(line, 6, 6));
                             parseInfo.header.setLeapSecondsWeekNum(RinexUtils.parseInt(line, 12, 6));
                             parseInfo.header.setLeapSecondsDayNum(RinexUtils.parseInt(line, 18, 6));
                         }
                     },
                     LineParser::headerNext),

        /** Parser for PRN and number of observations . */
        PRN_NB_OF_OBS(line -> RinexUtils.matchesLabel(line, "PRN / # OF OBS"),
                      (line, parseInfo) ->  {
                          // optional line, not stored for now
                      },
                      LineParser::headerNext),

        /** Parser for number of satellites. */
        NB_OF_SATELLITES(line -> RinexUtils.matchesLabel(line, "# OF SATELLITES"),
                         (line, parseInfo) -> parseInfo.nbSat = RinexUtils.parseInt(line, 0, 6),
                         parseInfo -> Stream.of(PRN_NB_OF_OBS)),

        /** Parser for number and types of observations. */
        TYPES_OF_OBSERV(line -> RinexUtils.matchesLabel(line, "# / TYPES OF OBSERV") ||
                                RinexUtils.matchesLabel(line, "SYS / # / OBS TYPES"),
                           (line, parseInfo) -> {
                               final double version = parseInfo.header.getFormatVersion();
                               if (parseInfo.nbTypes < 0) {
                                   // first line of types of observations
                                   if (version < 3) {
                                       // Rinex 2 has only one system
                                       parseInfo.currentSystem = parseInfo.header.getSatelliteSystem();
                                       parseInfo.nbTypes       = RinexUtils.parseInt(line, 0, 6);
                                   } else {
                                       // Rinex 3 and above allows mixed systems
                                       parseInfo.currentSystem = SatelliteSystem.parseSatelliteSystem(RinexUtils.parseString(line, 0, 1));
                                       parseInfo.nbTypes       = RinexUtils.parseInt(line, 3, 3);
                                   }
                               }

                               final int firstIndex = version < 3 ? 10 : 7;
                               final int increment  = version < 3 ?  6 : 4;
                               final int size       = version < 3 ?  2 : 3;
                               for (int i = firstIndex; i < 60 && parseInfo.typesObs.size() < parseInfo.nbTypes; i += increment) {
                                   final String type = RinexUtils.parseString(line, i, size);
                                   try {
                                       parseInfo.typesObs.add(ObservationType.valueOf(type));
                                   } catch (IllegalArgumentException iae) {
                                       throw new OrekitException(iae, OrekitMessages.UNKNOWN_RINEX_FREQUENCY,
                                                                 type, parseInfo.name, parseInfo.lineNumber);
                                   }
                               }

                               if (parseInfo.typesObs.size() == parseInfo.nbTypes) {
                                   // we have completed the list
                                   parseInfo.listTypeObs.put(parseInfo.currentSystem, new ArrayList<>(parseInfo.typesObs));
                                   parseInfo.nbTypes = -1;
                                   parseInfo.typesObs.clear();
                               }

                           },
                           LineParser::headerNbTypesObs),

        /** Parser for center of mass. */
        CENTER_OF_MASS_XYZ(line -> RinexUtils.matchesLabel(line, "CENTER OF MASS: XYZ"),
                           (line, parseInfo) -> {
                               parseInfo.header.setCenterMass(new Vector3D(RinexUtils.parseDouble(line,  0, 14),
                                                                           RinexUtils.parseDouble(line, 14, 14),
                                                                           RinexUtils.parseDouble(line, 28, 14)));
                           },
                           LineParser::headerNext),

        /** Parser for unit of signal strength. */
        SIGNAL_STRENGTH_UNIT(line -> RinexUtils.matchesLabel(line, "SIGNAL STRENGTH UNIT"),
                             (line, parseInfo) -> parseInfo.header.setSignalStrengthUnit(RinexUtils.parseString(line, 0, 20)),
                             LineParser::headerNext),

        /** Parser for differential code bias corrections. */
        SYS_DCBS_APPLIED(line -> RinexUtils.matchesLabel(line, "SYS / DCBS APPLIED"),
                         (line, parseInfo) -> parseInfo.header.addAppliedDCBS(new AppliedDCBS(SatelliteSystem.parseSatelliteSystem(RinexUtils.parseString(line, 0, 1)),
                                                                                              RinexUtils.parseString(line, 2, 17),
                                                                                              RinexUtils.parseString(line, 20, 40))),
                         LineParser::headerNext),

        /** Parser for phase center variations corrections. */
        SYS_PCVS_APPLIED(line -> RinexUtils.matchesLabel(line, "SYS / PCVS APPLIED"),
                         (line, parseInfo) -> parseInfo.header.addAppliedPCVS(new AppliedPCVS(SatelliteSystem.parseSatelliteSystem(RinexUtils.parseString(line, 0, 1)),
                                                                                              RinexUtils.parseString(line, 2, 17),
                                                                                              RinexUtils.parseString(line, 20, 40))),
                         LineParser::headerNext),

        /** Parser for scale factor. */
        SYS_SCALE_FACTOR(line -> RinexUtils.matchesLabel(line, "SYS / SCALE FACTOR"),
                         (line, parseInfo) -> {

                             if (parseInfo.nbObsScaleFactor < 0) {
                                 // first line of scale factor
                                 parseInfo.currentSystem    = SatelliteSystem.parseSatelliteSystem(RinexUtils.parseString(line, 0, 1));
                                 parseInfo.scaleFactor      = RinexUtils.parseInt(line, 2, 4);
                                 parseInfo.nbObsScaleFactor = RinexUtils.parseInt(line, 8, 2);
                             }

                             if (parseInfo.nbObsScaleFactor == 0) {
                                 parseInfo.typesObsScaleFactor.addAll(parseInfo.listTypeObs.get(parseInfo.currentSystem));
                             } else {
                                 for (int i = 11; i < 60 && parseInfo.typesObsScaleFactor.size() < parseInfo.nbObsScaleFactor; i += 4) {
                                     parseInfo.typesObsScaleFactor.add(ObservationType.valueOf(RinexUtils.parseString(line, i, 3)));
                                 }
                             }

                             if (parseInfo.typesObsScaleFactor.size() >= parseInfo.nbObsScaleFactor) {
                                 // we have completed the list
                                 parseInfo.header.addScaleFactorCorrection(new ScaleFactorCorrection(parseInfo.currentSystem,
                                                                                                     parseInfo.scaleFactor,
                                                                                                     parseInfo.typesObsScaleFactor));
                                 parseInfo.phaseShiftNbSat = -1;
                                 parseInfo.satPhaseShift.clear();
                             }

                         },
                         LineParser::headerNext),

        /** Parser for phase shift. */
        SYS_PHASE_SHIFT(line -> RinexUtils.matchesLabel(line, "SYS / PHASE SHIFT") ||
                                RinexUtils.matchesLabel(line, "SYS / PHASE SHIFTS"),
                        (line, parseInfo) -> {

                            if (parseInfo.phaseShiftNbSat < 0) {
                                // first line of phase shift
                                parseInfo.currentSystem     = SatelliteSystem.parseSatelliteSystem(RinexUtils.parseString(line, 0, 1));
                                final String to             = RinexUtils.parseString(line, 2, 3);
                                parseInfo.phaseShiftTypeObs = to.isEmpty() ? null : ObservationType.valueOf(to.length() < 3 ? "L" + to : to);
                                parseInfo.corrPhaseShift    = RinexUtils.parseDouble(line, 6, 8);
                                parseInfo.phaseShiftNbSat   = RinexUtils.parseInt(line, 16, 2);
                            }

                            for (int i = 19; i < 60 && parseInfo.satPhaseShift.size() < parseInfo.phaseShiftNbSat; i += 4) {
                                parseInfo.satPhaseShift.add(RinexUtils.parseString(line, i, 3));
                            }

                            if (parseInfo.satPhaseShift.size() == parseInfo.phaseShiftNbSat) {
                                // we have completed the list
                                parseInfo.header.addPhaseShiftCorrection(new PhaseShiftCorrection(parseInfo.currentSystem,
                                                                                                  parseInfo.phaseShiftTypeObs,
                                                                                                  parseInfo.corrPhaseShift,
                                                                                                  (String[]) parseInfo.satPhaseShift.toArray()));
                                parseInfo.phaseShiftNbSat = -1;
                                parseInfo.satPhaseShift.clear();
                            }

                        },
                        LineParser::headerPhaseShift),

        /** Parser for GLONASS slot and frequency number. */
        GLONASS_SLOT_FRQ_NB(line -> RinexUtils.matchesLabel(line, "GLONASS SLOT / FRQ #"),
                            (line, parseInfo) -> {
                                // TODO
                                throw new OrekitInternalError(null);
                            },
                            LineParser::headerNext),

        /** Parser for GLONASS phase bias corrections. */
        GLONASS_COD_PHS_BIS(line -> RinexUtils.matchesLabel(line, "GLONASS COD/PHS/BIS"),
                            (line, parseInfo) -> {
                                // TODO
                                throw new OrekitInternalError(null);
                            },
                            LineParser::headerNext),

        /** Parser for observations scale factor. */
        OBS_SCALE_FACTOR(line -> RinexUtils.matchesLabel(line, "OBS SCALE FACTOR"),
                         (line, parseInfo) -> {
                             final int scaleFactor      = FastMath.max(1, RinexUtils.parseInt(line, 0,  6));
                             final int nbObsScaleFactor = RinexUtils.parseInt(line, 6, 6);
                             final List<ObservationType> typesObsScaleFactor = new ArrayList<>(nbObsScaleFactor);
                             for (int i = 0; i < nbObsScaleFactor; i++) {
                                 typesObsScaleFactor.add(ObservationType.valueOf(RinexUtils.parseString(line, 16 + (6 * i), 2)));
                             }
                             parseInfo.scaleFactorCorrections.add(new ScaleFactorCorrection(parseInfo.header.getSatelliteSystem(),
                                                                                            scaleFactor, typesObsScaleFactor));
                         },
                         LineParser::headerNext),

        /** Parser for Rinex 2 observation line. */
        RINEX_2_OBSERVATION(line -> false,
                            (line, parseInfo) -> {
                                // TODO
                            },
                            LineParser::observation2),

        /** Parser for Rinex 2 special record. */
        RINEX_2_IGNORED_SPECIAL_RECORD(line -> false,
                           (line, parseInfo) -> {
                               // TODO
                           },
                           LineParser::observation2),

        /** Parser for Rinex 2 data first line. */
        RINEX_2_DATA_FIRST(line -> false,
                           (line, parseInfo) -> {
                               parseInfo.eventFlag = RinexUtils.parseInt(line, 28, 1);
                               final int n  = RinexUtils.parseInt(line, 29, 3);
                               if (parseInfo.eventFlag > 1) {
                                   if (parseInfo.eventFlag < 6) {
                                       // moving antenna / new site occupation / header information / external event
                                       parseInfo.nextObsLineNumber = parseInfo.lineNumber + n;
                                   } else {
                                       // cycle slip
                                       final int nbLinesSat  = (n + MAX_SAT_PER_RINEX_2_LINE - 1) / MAX_SAT_PER_RINEX_2_LINE;
                                       final int nbLinesObs  = (parseInfo.nbTypes + 5 - 1) / 5;
                                       final int nbLinesSkip = (nbLinesSat - 1) + n * nbLinesObs;
                                       parseInfo.nextObsLineNumber = parseInfo.lineNumber + nbLinesSkip;
                                   }
                               } else {
                                   // regular observation
                                   parseInfo.nbSatObs = n;
                                   if (parseInfo.nbSat != -1 && parseInfo.nbSatObs > parseInfo.nbSat) {
                                       // we check that the number of Sat in the observation is consistent
                                       throw new OrekitException(OrekitMessages.INCONSISTENT_NUMBER_OF_SATS,
                                                                 parseInfo.lineNumber, parseInfo.name,
                                                                 parseInfo.nbSatObs, parseInfo.nbSat);
                                   }
                                   final int nbLines = (parseInfo.nbSatObs + MAX_OBS_PER_RINEX_2_LINE - 1) / MAX_OBS_PER_RINEX_2_LINE;
                                   parseInfo.nextObsLineNumber = parseInfo.lineNumber + nbLines;

                                   // observations epoch
                                   final int yy = RinexUtils.parseInt(line, 0, 3);
                                   parseInfo.tObs = new AbsoluteDate(yy >= 80 ? (yy + 1900) : (yy + 2000),
                                                                     RinexUtils.parseInt(line, 3, 3),
                                                                     RinexUtils.parseInt(line, 6, 3),
                                                                     RinexUtils.parseInt(line, 9, 3),
                                                                     RinexUtils.parseInt(line, 12, 3),
                                                                     RinexUtils.parseDouble(line, 15, 11),
                                                                     parseInfo.timeScale);

                                   // read the Receiver Clock offset, if present
                                   parseInfo.rcvrClkOffset = RinexUtils.parseDouble(line, 68, 12);
                                   if (Double.isNaN(parseInfo.rcvrClkOffset)) {
                                       parseInfo.rcvrClkOffset = 0.0;
                                   }

                               }

                           },
                           parseInfo -> Stream.of(parseInfo.eventFlag > 1 ?
                                                  RINEX_2_IGNORED_SPECIAL_RECORD  :
                                                  RINEX_2_OBSERVATION)),

        /** Parser for Rinex 3 observation line. */
        RINEX_3_OBSERVATION(line -> false,
                            (line, parseInfo) -> {
                                // TODO
                            },
                            LineParser::observation3),

        /** Parser for Rinex 3 data first line. */
        RINEX_3_DATA_FIRST(line -> false,
                           (line, parseInfo) -> {
                               // TODO
                           },
                           parseInfo -> Stream.of(RINEX_3_OBSERVATION)),

        /** Parser for the end of header. */
        HEADER_END(line -> RinexUtils.matchesLabel(line, "END OF HEADER"),
                   (line, parseInfo) -> {

                       // get rinex format version
                       final double version = parseInfo.header.getFormatVersion();

                       // check mandatory header fields
                       if (version < 3) {
                           if (parseInfo.header.getMarkerName()                             != null ||
                               parseInfo.header.getObserverName()                           != null ||
                               parseInfo.header.getReceiverNumber()                         != null ||
                               parseInfo.header.getAntennaNumber()                          != null ||
                               version < 2.20 && parseInfo.header.getApproxPos()            != null ||
                               version < 2.20 && !Double.isNaN(parseInfo.header.getAntennaHeight()) ||
                               parseInfo.listTypeObs.isEmpty()) {
                               throw new OrekitException(OrekitMessages.INCOMPLETE_HEADER, parseInfo.name);
                           }

                       }
                       if (parseInfo.header.getMarkerName()           != null ||
                           parseInfo.header.getObserverName()         != null ||
                           parseInfo.header.getReceiverNumber()       != null ||
                           parseInfo.header.getAntennaNumber()        != null ||
                           !Double.isNaN(parseInfo.header.getAntennaHeight()) ||
                           parseInfo.listTypeObs.isEmpty()                    ||
                           version >= 3.01 && parseInfo.header.getPhaseShiftCorrections().isEmpty()) {
                           throw new OrekitException(OrekitMessages.INCOMPLETE_HEADER, parseInfo.name);
                       }
                   },
                   parseInfo -> Stream.of(parseInfo.header.getFormatVersion() < 2 ?
                                          RINEX_2_DATA_FIRST :
                                          RINEX_3_DATA_FIRST));


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
            if (parseInfo.header.getFormatVersion() < 2) {
                return Stream.of(HEADER_COMMENT, HEADER_PROGRAM, MARKER_NAME, MARKER_NUMBER, MARKER_TYPE, OBSERVER_AGENCY,
                                 REC_NB_TYPE_VERS, ANT_NB_TYPE, APPROX_POSITION_XYZ, ANTENNA_DELTA_H_E_N,
                                 ANTENNA_DELTA_X_Y_Z, ANTENNA_B_SIGHT_XYZ, CENTER_OF_MASS_XYZ, NB_OF_SATELLITES,
                                 WAVELENGTH_FACT_L1_2, RCV_CLOCK_OFFS_APPL, INTERVAL, TIME_OF_FIRST_OBS, TIME_OF_LAST_OBS,
                                 LEAP_SECONDS, PRN_NB_OF_OBS, TYPES_OF_OBSERV, OBS_SCALE_FACTOR, HEADER_END);
            } else {
                return Stream.of(HEADER_COMMENT, HEADER_PROGRAM, MARKER_NAME, MARKER_NUMBER, MARKER_TYPE, OBSERVER_AGENCY,
                                 REC_NB_TYPE_VERS, ANT_NB_TYPE, APPROX_POSITION_XYZ, ANTENNA_DELTA_H_E_N,
                                 ANTENNA_DELTA_X_Y_Z, ANTENNA_PHASECENTER, ANTENNA_B_SIGHT_XYZ, ANTENNA_ZERODIR_AZI,
                                 ANTENNA_ZERODIR_XYZ, CENTER_OF_MASS_XYZ, NB_OF_SATELLITES, RCV_CLOCK_OFFS_APPL,
                                 INTERVAL, TIME_OF_FIRST_OBS, TIME_OF_LAST_OBS, LEAP_SECONDS, PRN_NB_OF_OBS,
                                 TYPES_OF_OBSERV, SIGNAL_STRENGTH_UNIT, SYS_DCBS_APPLIED,
                                 SYS_PCVS_APPLIED, SYS_SCALE_FACTOR, SYS_PHASE_SHIFT,
                                 GLONASS_SLOT_FRQ_NB, GLONASS_COD_PHS_BIS, HEADER_END);
            }
        }

        /** Get the allowed parsers for next lines while parsing types of observations.
         * @param parseInfo holder for transient data
         * @return allowed parsers for next line
         */
        private static Stream<LineParser> headerNbTypesObs(final ParseInfo parseInfo) {
            if (parseInfo.typesObs.size() < parseInfo.nbTypes) {
                return Stream.of(TYPES_OF_OBSERV);
            } else {
                return headerNext(parseInfo);
            }
        }

        /** Get the allowed parsers for next lines while parsing phase shifts.
         * @param parseInfo holder for transient data
         * @return allowed parsers for next line
         */
        private static Stream<LineParser> headerPhaseShift(final ParseInfo parseInfo) {
            if (parseInfo.satPhaseShift.size() < parseInfo.phaseShiftNbSat) {
                return Stream.of(SYS_PHASE_SHIFT);
            } else {
                return headerNext(parseInfo);
            }
        }

        /** Get the allowed parsers for next lines while parsing Rinex 2 observations.
         * @param parseInfo holder for transient data
         * @return allowed parsers for next line
         */
        private static Stream<LineParser> observation2(final ParseInfo parseInfo) {
            // TODO: return a singleton based on the number of observations
            return Stream.of(RINEX_2_DATA_FIRST, RINEX_2_OBSERVATION);
        }

        /** Get the allowed parsers for next lines while parsing Rinex 3 observations.
         * @param parseInfo holder for transient data
         * @return allowed parsers for next line
         */
        private static Stream<LineParser> observation3(final ParseInfo parseInfo) {
            // TODO: return a singleton based on the number of observations
            return Stream.of(RINEX_3_DATA_FIRST, RINEX_3_OBSERVATION);
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
