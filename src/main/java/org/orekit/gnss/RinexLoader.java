/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hipparchus.exception.DummyLocalizable;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.geometry.euclidean.twod.Vector2D;
import org.hipparchus.util.FastMath;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.data.DataLoader;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScales;

/** Loader for Rinex measurements files.
 * <p>
 * Supported versions are: 2.00, 2.10, 2.11, 2.12 (unofficial), 2.20 (unofficial),
 * 3.00, 3.01, 3.02, 3.03, and 3.04.
 * </p>
 * @see <a href="ftp://igs.org/pub/data/format/rinex2.txt">rinex 2.0</a>
 * @see <a href="ftp://igs.org/pub/data/format/rinex210.txt">rinex 2.10</a>
 * @see <a href="ftp://igs.org/pub/data/format/rinex211.txt">rinex 2.11</a>
 * @see <a href="http://www.aiub.unibe.ch/download/rinex/rinex212.txt">unofficial rinex 2.12</a>
 * @see <a href="http://www.aiub.unibe.ch/download/rinex/rnx_leo.txt">unofficial rinex 2.20</a>
 * @see <a href="ftp://igs.org/pub/data/format/rinex300.pdf">rinex 3.00</a>
 * @see <a href="ftp://igs.org/pub/data/format/rinex301.pdf">rinex 3.01</a>
 * @see <a href="ftp://igs.org/pub/data/format/rinex302.pdf">rinex 3.02</a>
 * @see <a href="ftp://igs.org/pub/data/format/rinex303.pdf">rinex 3.03</a>
 * @see <a href="ftp://igs.org/pub/data/format/rinex304.pdf">rinex 3.04</a>
 * @since 9.2
 */
public class RinexLoader {

    /** Default supported files name pattern for rinex 2 observation files. */
    public static final String DEFAULT_RINEX_2_SUPPORTED_NAMES = "^\\w{4}\\d{3}[0a-x](?:\\d{2})?\\.\\d{2}[oO]$";

    /** Default supported files name pattern for rinex 3 observation files. */
    public static final String DEFAULT_RINEX_3_SUPPORTED_NAMES = "^\\w{9}_\\w{1}_\\d{11}_\\d{2}\\w_\\d{2}\\w{1}_\\w{2}\\.rnx$";

    // CHECKSTYLE: stop JavadocVariable check
    private static final String RINEX_VERSION_TYPE   = "RINEX VERSION / TYPE";
    private static final String COMMENT              = "COMMENT";
    private static final String PGM_RUN_BY_DATE      = "PGM / RUN BY / DATE";
    private static final String MARKER_NAME          = "MARKER NAME";
    private static final String MARKER_NUMBER        = "MARKER NUMBER";
    private static final String MARKER_TYPE          = "MARKER TYPE";
    private static final String OBSERVER_AGENCY      = "OBSERVER / AGENCY";
    private static final String REC_NB_TYPE_VERS     = "REC # / TYPE / VERS";
    private static final String ANT_NB_TYPE          = "ANT # / TYPE";
    private static final String APPROX_POSITION_XYZ  = "APPROX POSITION XYZ";
    private static final String ANTENNA_DELTA_H_E_N  = "ANTENNA: DELTA H/E/N";
    private static final String ANTENNA_DELTA_X_Y_Z  = "ANTENNA: DELTA X/Y/Z";
    private static final String ANTENNA_PHASECENTER  = "ANTENNA: PHASECENTER";
    private static final String ANTENNA_B_SIGHT_XYZ  = "ANTENNA: B.SIGHT XYZ";
    private static final String ANTENNA_ZERODIR_AZI  = "ANTENNA: ZERODIR AZI";
    private static final String ANTENNA_ZERODIR_XYZ  = "ANTENNA: ZERODIR XYZ";
    private static final String NB_OF_SATELLITES     = "# OF SATELLITES";
    private static final String WAVELENGTH_FACT_L1_2 = "WAVELENGTH FACT L1/2";
    private static final String RCV_CLOCK_OFFS_APPL  = "RCV CLOCK OFFS APPL";
    private static final String INTERVAL             = "INTERVAL";
    private static final String TIME_OF_FIRST_OBS    = "TIME OF FIRST OBS";
    private static final String TIME_OF_LAST_OBS     = "TIME OF LAST OBS";
    private static final String LEAP_SECONDS         = "LEAP SECONDS";
    private static final String PRN_NB_OF_OBS        = "PRN / # OF OBS";
    private static final String NB_TYPES_OF_OBSERV   = "# / TYPES OF OBSERV";
    private static final String END_OF_HEADER        = "END OF HEADER";
    private static final String CENTER_OF_MASS_XYZ   = "CENTER OF MASS: XYZ";
    private static final String SIGNAL_STRENGTH_UNIT = "SIGNAL STRENGTH UNIT";
    private static final String SYS_NB_OBS_TYPES     = "SYS / # / OBS TYPES";
    private static final String SYS_DCBS_APPLIED     = "SYS / DCBS APPLIED";
    private static final String SYS_PCVS_APPLIED     = "SYS / PCVS APPLIED";
    private static final String SYS_SCALE_FACTOR     = "SYS / SCALE FACTOR";
    private static final String SYS_PHASE_SHIFT      = "SYS / PHASE SHIFT";
    private static final String SYS_PHASE_SHIFTS     = "SYS / PHASE SHIFTS";
    private static final String GLONASS_SLOT_FRQ_NB  = "GLONASS SLOT / FRQ #";
    private static final String GLONASS_COD_PHS_BIS  = "GLONASS COD/PHS/BIS";
    private static final String OBS_SCALE_FACTOR     = "OBS SCALE FACTOR";

    private static final String GPS                  = "GPS";
    private static final String GAL                  = "GAL";
    private static final String GLO                  = "GLO";
    private static final String QZS                  = "QZS";
    private static final String BDT                  = "BDT";
    private static final String IRN                  = "IRN";
    // CHECKSTYLE: resume JavadocVariable check

    /** Rinex Observations. */
    private final List<ObservationDataSet> observationDataSets;

    /** Set of time scales. */
    private final TimeScales timeScales;

    /** Simple constructor.
     * <p>
     * This constructor is used when the rinex files are managed by the
     * global {@link DataContext#getDefault() default data context}.
     * </p>
     * @param supportedNames regular expression for supported files names
     * @see #RinexLoader(String, DataProvidersManager, TimeScales)
     */
    @DefaultDataContext
    public RinexLoader(final String supportedNames) {
        this(supportedNames, DataContext.getDefault().getDataProvidersManager(),
                DataContext.getDefault().getTimeScales());
    }

    /**
     * Create a RINEX loader/parser with the given source of RINEX auxiliary data files.
     *
     * <p>
     * This constructor is used when the rinex files are managed by the given
     * {@code dataProvidersManager}.
     * </p>
     * @param supportedNames regular expression for supported files names
     * @param dataProvidersManager provides access to auxiliary data.
     * @param timeScales the set of time scales to use when parsing dates.
     * @since 10.1
     */
    public RinexLoader(final String supportedNames,
                       final DataProvidersManager dataProvidersManager,
                       final TimeScales timeScales) {
        observationDataSets = new ArrayList<>();
        this.timeScales = timeScales;
        dataProvidersManager.feed(supportedNames, new Parser());
    }

    /** Simple constructor. This constructor uses the {@link DataContext#getDefault()
     * default data context}.
     *
     * @param input data input stream
     * @param name name of the file (or zip entry)
     * @see #RinexLoader(InputStream, String, TimeScales)
     */
    @DefaultDataContext
    public RinexLoader(final InputStream input, final String name) {
        this(input, name, DataContext.getDefault().getTimeScales());
    }

    /**
     * Loads RINEX from the given input stream using the specified auxiliary data.
     *
     * @param input data input stream
     * @param name name of the file (or zip entry)
     * @param timeScales the set of time scales to use when parsing dates.
     * @since 10.1
     */
    public RinexLoader(final InputStream input,
                       final String name,
                       final TimeScales timeScales) {
        try {
            this.timeScales = timeScales;
            observationDataSets = new ArrayList<>();
            new Parser().loadData(input, name);
        } catch (IOException ioe) {
            throw new OrekitException(ioe, new DummyLocalizable(ioe.getMessage()));
        }
    }

    /** Get parsed rinex observations data sets.
     * @return unmodifiable view of parsed rinex observations
     * @since 9.3
     */
    public List<ObservationDataSet> getObservationDataSets() {
        return Collections.unmodifiableList(observationDataSets);
    }

    /** Parser for rinex files.
     */
    public class Parser implements DataLoader {

        /** Index of label in data lines. */
        private static final int LABEL_START = 60;

        /** File type accepted (only Observation Data). */
        private static final String FILE_TYPE = "O"; //Only Observation Data files

        /** Name of the file. */
        private String name;

        /** Current line. */
        private String line;

        /** current line number. */
        private int lineNumber;

        /** {@inheritDoc} */
        @Override
        public boolean stillAcceptsData() {
            // we load all rinex files we can find
            return true;
        }

        /** {@inheritDoc} */
        @Override
        public void loadData(final InputStream input, final String fileName)
            throws IOException, OrekitException {

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {

                this.name       = fileName;
                this.line       = null;
                this.lineNumber = 0;

                // placeholders for parsed data
                SatelliteSystem                  satelliteSystem        = null;
                double                           formatVersion          = Double.NaN;
                boolean                          inRinexVersion         = false;
                SatelliteSystem                  obsTypesSystem         = null;
                String                           markerName             = null;
                String                           markerNumber           = null;
                String                           markerType             = null;
                String                           observerName           = null;
                String                           agencyName             = null;
                String                           receiverNumber         = null;
                String                           receiverType           = null;
                String                           receiverVersion        = null;
                String                           antennaNumber          = null;
                String                           antennaType            = null;
                Vector3D                         approxPos              = null;
                Vector3D                         antRefPoint            = null;
                String                           obsCode                = null;
                Vector3D                         antPhaseCenter         = null;
                Vector3D                         antBSight              = null;
                double                           antAzi                 = Double.NaN;
                Vector3D                         antZeroDir             = null;
                Vector3D                         centerMass             = null;
                double                           antHeight              = Double.NaN;
                Vector2D                         eccentricities         = Vector2D.ZERO;
                int                              clkOffset              = -1;
                int                              nbTypes                = -1;
                int                              nbSat                  = -1;
                double                           interval               = Double.NaN;
                AbsoluteDate                     tFirstObs              = AbsoluteDate.PAST_INFINITY;
                AbsoluteDate                     tLastObs               = AbsoluteDate.FUTURE_INFINITY;
                TimeScale                        timeScale              = null;
                String                           timeScaleStr           = null;
                int                              leapSeconds            = 0;
                AbsoluteDate                     tObs                   = AbsoluteDate.PAST_INFINITY;
                String[]                         satsObsList            = null;
                int                              eventFlag              = -1;
                int                              nbSatObs               = -1;
                int                              nbLinesSat             = -1;
                double                           rcvrClkOffset          = 0;
                boolean                          inRunBy                = false;
                boolean                          inMarkerName           = false;
                boolean                          inObserver             = false;
                boolean                          inRecType              = false;
                boolean                          inAntType              = false;
                boolean                          inAproxPos             = false;
                boolean                          inAntDelta             = false;
                boolean                          inTypesObs             = false;
                boolean                          inFirstObs             = false;
                boolean                          inPhaseShift           = false;
                boolean                          inGlonassSlot          = false;
                boolean                          inGlonassCOD           = false;
                RinexHeader                      rinexHeader            = null;
                int                               scaleFactor            = 1;
                int                               nbObsScaleFactor       = 0;
                final List<ScaleFactorCorrection> scaleFactorCorrections = new ArrayList<>();
                final Map<SatelliteSystem, List<ObservationType>> listTypeObs = new HashMap<>();

                //First line must  always contain Rinex Version, File Type and Satellite Systems Observed
                readLine(reader, true);
                if (line.length() < LABEL_START || !RINEX_VERSION_TYPE.equals(line.substring(LABEL_START).trim())) {
                    throw new OrekitException(OrekitMessages.UNSUPPORTED_FILE_FORMAT, name);
                }
                formatVersion = parseDouble(0, 9);
                final int format100 = (int) FastMath.rint(100 * formatVersion);

                if ((format100 != 200) && (format100 != 210) && (format100 != 211) &&
                    (format100 != 212) && (format100 != 220) && (format100 != 300) &&
                    (format100 != 301) && (format100 != 302) && (format100 != 303) &&
                    (format100 != 304)) {
                    throw new OrekitException(OrekitMessages.UNSUPPORTED_FILE_FORMAT, name);
                }

                //File Type must be Observation_Data
                if (!(parseString(20, 1)).equals(FILE_TYPE)) {
                    throw new OrekitException(OrekitMessages.UNSUPPORTED_FILE_FORMAT, name);
                }
                satelliteSystem = SatelliteSystem.parseSatelliteSystem(parseString(40, 1));
                inRinexVersion = true;

                switch (format100 / 100) {
                    case 2: {

                        final int                   MAX_OBS_TYPES_PER_LINE_RNX2 = 9;
                        final int                   MAX_N_SAT_OBSERVATION       = 12;
                        final int                   MAX_N_TYPES_OBSERVATION     = 5;
                        final int                   MAX_OBS_TYPES_SCALE_FACTOR  = 8;
                        final List<ObservationType> typesObs = new ArrayList<>();

                        while (readLine(reader, false)) {

                            if (rinexHeader == null) {
                                switch(line.substring(LABEL_START).trim()) {
                                    case COMMENT :
                                        // nothing to do
                                        break;
                                    case PGM_RUN_BY_DATE :
                                        inRunBy = true;
                                        break;
                                    case MARKER_NAME :
                                        markerName = parseString(0, 60);
                                        inMarkerName = true;
                                        break;
                                    case MARKER_NUMBER :
                                        markerNumber = parseString(0, 20);
                                        break;
                                    case MARKER_TYPE :
                                        markerType = parseString(0, 20);
                                        break;
                                    case OBSERVER_AGENCY :
                                        observerName = parseString(0, 20);
                                        agencyName   = parseString(20, 40);
                                        inObserver = true;
                                        break;
                                    case REC_NB_TYPE_VERS :
                                        receiverNumber  = parseString(0, 20);
                                        receiverType    = parseString(20, 20);
                                        receiverVersion = parseString(40, 20);
                                        inRecType = true;
                                        break;
                                    case ANT_NB_TYPE :
                                        antennaNumber = parseString(0, 20);
                                        antennaType   = parseString(20, 20);
                                        inAntType = true;
                                        break;
                                    case APPROX_POSITION_XYZ :
                                        approxPos = new Vector3D(parseDouble(0, 14), parseDouble(14, 14),
                                                                 parseDouble(28, 14));
                                        inAproxPos = true;
                                        break;
                                    case ANTENNA_DELTA_H_E_N :
                                        antHeight = parseDouble(0, 14);
                                        eccentricities = new Vector2D(parseDouble(14, 14), parseDouble(28, 14));
                                        inAntDelta = true;
                                        break;
                                    case ANTENNA_DELTA_X_Y_Z :
                                        antRefPoint = new Vector3D(parseDouble(0, 14),
                                                                   parseDouble(14, 14),
                                                                   parseDouble(28, 14));
                                        break;
                                    case ANTENNA_B_SIGHT_XYZ :
                                        antBSight = new Vector3D(parseDouble(0, 14),
                                                                 parseDouble(14, 14),
                                                                 parseDouble(28, 14));
                                        break;
                                    case CENTER_OF_MASS_XYZ :
                                        centerMass = new Vector3D(parseDouble(0, 14),
                                                                  parseDouble(14, 14),
                                                                  parseDouble(28, 14));
                                        break;
                                    case NB_OF_SATELLITES :
                                        nbSat = parseInt(0, 6);
                                        break;
                                    case WAVELENGTH_FACT_L1_2 :
                                        //Optional line in header
                                        //Not stored for now
                                        break;
                                    case RCV_CLOCK_OFFS_APPL :
                                        clkOffset = parseInt(0, 6);
                                        break;
                                    case INTERVAL :
                                        interval = parseDouble(0, 10);
                                        break;
                                    case TIME_OF_FIRST_OBS :
                                        switch (satelliteSystem) {
                                            case GPS:
                                                timeScale = timeScales.getGPS();
                                                break;
                                            case GALILEO:
                                                timeScale = timeScales.getGST();
                                                break;
                                            case GLONASS:
                                                timeScale = timeScales.getGLONASS();
                                                break;
                                            case MIXED:
                                                //in Case of Mixed data, Timescale must be specified in the Time of First line
                                                timeScaleStr = parseString(48, 3);

                                                if (timeScaleStr.equals(GPS)) {
                                                    timeScale = timeScales.getGPS();
                                                } else if (timeScaleStr.equals(GAL)) {
                                                    timeScale = timeScales.getGST();
                                                } else if (timeScaleStr.equals(GLO)) {
                                                    timeScale = timeScales.getGLONASS();
                                                } else {
                                                    throw new OrekitException(OrekitMessages.UNSUPPORTED_FILE_FORMAT, name);
                                                }
                                                break;
                                            default :
                                                throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                                          lineNumber, name, line);
                                        }

                                        tFirstObs = new AbsoluteDate(parseInt(0, 6),
                                                                     parseInt(6, 6),
                                                                     parseInt(12, 6),
                                                                     parseInt(18, 6),
                                                                     parseInt(24, 6),
                                                                     parseDouble(30, 13), timeScale);
                                        inFirstObs = true;
                                        break;
                                    case TIME_OF_LAST_OBS :
                                        tLastObs = new AbsoluteDate(parseInt(0, 6),
                                                                    parseInt(6, 6),
                                                                    parseInt(12, 6),
                                                                    parseInt(18, 6),
                                                                    parseInt(24, 6),
                                                                    parseDouble(30, 13), timeScale);
                                        break;
                                    case LEAP_SECONDS :
                                        leapSeconds = parseInt(0, 6);
                                        break;
                                    case PRN_NB_OF_OBS :
                                        //Optional line in header, indicates number of Observations par Satellite
                                        //Not stored for now
                                        break;
                                    case NB_TYPES_OF_OBSERV :
                                        nbTypes = parseInt(0, 6);
                                        final int nbLinesTypesObs = (nbTypes + MAX_OBS_TYPES_PER_LINE_RNX2 - 1 ) / MAX_OBS_TYPES_PER_LINE_RNX2;

                                        for (int j = 0; j < nbLinesTypesObs; j++) {
                                            if (j > 0) {
                                                readLine(reader, true);
                                            }
                                            final int iMax = FastMath.min(MAX_OBS_TYPES_PER_LINE_RNX2, nbTypes - typesObs.size());
                                            for (int i = 0; i < iMax; i++) {
                                                try {
                                                    typesObs.add(ObservationType.valueOf(parseString(10 + (6 * i), 2)));
                                                } catch (IllegalArgumentException iae) {
                                                    throw new OrekitException(iae, OrekitMessages.UNKNOWN_RINEX_FREQUENCY,
                                                                              parseString(10 + (6 * i), 2), name, lineNumber);
                                                }
                                            }
                                        }
                                        inTypesObs = true;
                                        break;
                                    case OBS_SCALE_FACTOR :
                                        scaleFactor      = FastMath.max(1, parseInt(0,  6));
                                        nbObsScaleFactor = parseInt(6, 6);
                                        if (nbObsScaleFactor > MAX_OBS_TYPES_SCALE_FACTOR) {
                                            throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                                      lineNumber, name, line);
                                        }
                                        final List<ObservationType> typesObsScaleFactor = new ArrayList<>(nbObsScaleFactor);
                                        for (int i = 0; i < nbObsScaleFactor; i++) {
                                            typesObsScaleFactor.add(ObservationType.valueOf(parseString(16 + (6 * i), 2)));
                                        }
                                        scaleFactorCorrections.add(new ScaleFactorCorrection(satelliteSystem,
                                                                                             scaleFactor, typesObsScaleFactor));
                                        break;
                                    case END_OF_HEADER :
                                        //We make sure that we have read all the mandatory fields inside the header of the Rinex
                                        if (!inRinexVersion || !inRunBy || !inMarkerName ||
                                            !inObserver || !inRecType || !inAntType ||
                                            (formatVersion < 2.20 && !inAproxPos) ||
                                            (formatVersion < 2.20 && !inAntDelta) ||
                                            !inTypesObs || !inFirstObs) {
                                            throw new OrekitException(OrekitMessages.INCOMPLETE_HEADER, name);
                                        }

                                        //Header information gathered
                                        rinexHeader = new RinexHeader(formatVersion, satelliteSystem,
                                                                      markerName, markerNumber, markerType, observerName,
                                                                      agencyName, receiverNumber, receiverType,
                                                                      receiverVersion, antennaNumber, antennaType,
                                                                      approxPos, antHeight, eccentricities,
                                                                      antRefPoint, antBSight, centerMass, interval,
                                                                      tFirstObs, tLastObs, clkOffset, leapSeconds);
                                        break;
                                    default :
                                        if (rinexHeader == null) {
                                            //There must be an error due to an unknown Label inside the Header
                                            throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                                      lineNumber, name, line);
                                        }
                                }
                            } else {

                                //Start of a new Observation
                                rcvrClkOffset     =  0;
                                nbLinesSat        = -1;
                                eventFlag         = -1;
                                nbSatObs          = -1;
                                satsObsList       = null;
                                tObs              = null;

                                eventFlag = parseInt(28, 1);
                                //If eventFlag>1, we skip the corresponding lines to the next observation
                                if (eventFlag > 1) {
                                    if (eventFlag == 6) {
                                        nbSatObs  = parseInt(29, 3);
                                        nbLinesSat = (nbSatObs + 12 - 1) / 12;
                                        final int nbLinesObs = (nbTypes + 5 - 1) / 5;
                                        final int nbLinesSkip = (nbLinesSat - 1) + nbSatObs * nbLinesObs;
                                        for (int i = 0; i < nbLinesSkip; i++) {
                                            readLine(reader, true);
                                        }
                                    } else {
                                        final int nbLinesSkip = parseInt(29, 3);
                                        for (int i = 0; i < nbLinesSkip; i++) {
                                            readLine(reader, true);
                                        }
                                    }
                                } else {

                                    int y = parseInt(0, 3);
                                    if (79 < y && y <= 99) {
                                        y += 1900;
                                    } else if (0 <= y && y <= 79) {
                                        y += 2000;
                                    }
                                    tObs = new AbsoluteDate(y,
                                                            parseInt(3, 3),
                                                            parseInt(6, 3),
                                                            parseInt(9, 3),
                                                            parseInt(12, 3),
                                                            parseDouble(15, 11), timeScale);

                                    nbSatObs  = parseInt(29, 3);
                                    satsObsList   = new String[nbSatObs];
                                    //If the total number of satellites was indicated in the Header
                                    if (nbSat != -1 && nbSatObs > nbSat) {
                                        //we check that the number of Sat in the observation is consistent
                                        throw new OrekitException(OrekitMessages.INCONSISTENT_NUMBER_OF_SATS,
                                                                  lineNumber, name, nbSatObs, nbSat);
                                    }

                                    nbLinesSat = (nbSatObs + MAX_N_SAT_OBSERVATION - 1) / MAX_N_SAT_OBSERVATION;
                                    for (int j = 0; j < nbLinesSat; j++) {
                                        if (j > 0) {
                                            readLine(reader, true);
                                        }
                                        final int iMax = FastMath.min(MAX_N_SAT_OBSERVATION, nbSatObs  - j * MAX_N_SAT_OBSERVATION);
                                        for (int i = 0; i < iMax; i++) {
                                            satsObsList[i + MAX_N_SAT_OBSERVATION * j] = parseString(32 + 3 * i, 3);
                                        }

                                        //Read the Receiver Clock offset, if present
                                        rcvrClkOffset = parseDouble(68, 12);
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
                                                double value = parseDouble(16 * i, 14);
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
                                                                                        parseInt(14 + 16 * i, 1),
                                                                                        parseInt(15 + 16 * i, 1)));
                                            }
                                        }

                                        //We check that the Satellite type is consistent with Satellite System in the top of the file
                                        final SatelliteSystem satelliteSystemSat;
                                        final int id;
                                        if (satsObsList[k].length() < 3) {
                                            // missing satellite system, we use the global one
                                            satelliteSystemSat = satelliteSystem;
                                            id                 = Integer.parseInt(satsObsList[k]);
                                        } else {
                                            satelliteSystemSat = SatelliteSystem.parseSatelliteSystem(satsObsList[k]);
                                            id                 = Integer.parseInt(satsObsList[k].substring(1, 3).trim());
                                        }
                                        if (!satelliteSystem.equals(SatelliteSystem.MIXED)) {
                                            if (!satelliteSystemSat.equals(satelliteSystem)) {
                                                throw new OrekitException(OrekitMessages.INCONSISTENT_SATELLITE_SYSTEM,
                                                                          lineNumber, name, satelliteSystem, satelliteSystemSat);
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

                                    }
                                }
                            }
                        }
                        break;
                    }
                    case 3: {

                        final int                   MAX_OBS_TYPES_PER_LINE_RNX3 = 13;
                        final int           MAX_OBS_TYPES_SCALE_FACTOR_PER_LINE = 12;
                        final int                    MAX_N_SAT_PHSHIFT_PER_LINE = 10;

                        final List<ObservationType>                       typeObs                = new ArrayList<>();
                        String                                            sigStrengthUnit        = null;
                        int                                               leapSecondsFuture      = 0;
                        int                                               leapSecondsWeekNum     = 0;
                        int                                               leapSecondsDayNum      = 0;
                        final List<AppliedDCBS>                           listAppliedDCBs        = new ArrayList<>();
                        final List<AppliedPCVS>                           listAppliedPCVS        = new ArrayList<>();
                        SatelliteSystem                                   satSystemScaleFactor   = null;
                        String[]                                          satsPhaseShift         = null;
                        int                                               nbSatPhaseShift        = 0;
                        SatelliteSystem                                   satSystemPhaseShift    = null;
                        double                                            corrPhaseShift         = 0.0;
                        final List<PhaseShiftCorrection>                  phaseShiftCorrections  = new ArrayList<>();
                        ObservationType                                   phaseShiftTypeObs      = null;


                        while (readLine(reader, false)) {
                            if (rinexHeader == null) {
                                switch(line.substring(LABEL_START).trim()) {
                                    case COMMENT :
                                        // nothing to do
                                        break;
                                    case PGM_RUN_BY_DATE :
                                        inRunBy = true;
                                        break;
                                    case MARKER_NAME :
                                        markerName = parseString(0, 60);
                                        inMarkerName = true;
                                        break;
                                    case MARKER_NUMBER :
                                        markerNumber = parseString(0, 20);
                                        break;
                                    case MARKER_TYPE :
                                        markerType = parseString(0, 20);
                                        //Could be done with an Enumeration
                                        break;
                                    case OBSERVER_AGENCY :
                                        observerName = parseString(0, 20);
                                        agencyName   = parseString(20, 40);
                                        inObserver = true;
                                        break;
                                    case REC_NB_TYPE_VERS :
                                        receiverNumber  = parseString(0, 20);
                                        receiverType    = parseString(20, 20);
                                        receiverVersion = parseString(40, 20);
                                        inRecType = true;
                                        break;
                                    case ANT_NB_TYPE :
                                        antennaNumber = parseString(0, 20);
                                        antennaType   = parseString(20, 20);
                                        inAntType = true;
                                        break;
                                    case APPROX_POSITION_XYZ :
                                        approxPos = new Vector3D(parseDouble(0, 14),
                                                                 parseDouble(14, 14),
                                                                 parseDouble(28, 14));
                                        inAproxPos = true;
                                        break;
                                    case ANTENNA_DELTA_H_E_N :
                                        antHeight = parseDouble(0, 14);
                                        eccentricities = new Vector2D(parseDouble(14, 14),
                                                                      parseDouble(28, 14));
                                        inAntDelta = true;
                                        break;
                                    case ANTENNA_DELTA_X_Y_Z :
                                        antRefPoint = new Vector3D(parseDouble(0, 14),
                                                                   parseDouble(14, 14),
                                                                   parseDouble(28, 14));
                                        break;
                                    case ANTENNA_PHASECENTER :
                                        obsCode = parseString(2, 3);
                                        antPhaseCenter = new Vector3D(parseDouble(5, 9),
                                                                      parseDouble(14, 14),
                                                                      parseDouble(28, 14));
                                        break;
                                    case ANTENNA_B_SIGHT_XYZ :
                                        antBSight = new Vector3D(parseDouble(0, 14),
                                                                 parseDouble(14, 14),
                                                                 parseDouble(28, 14));
                                        break;
                                    case ANTENNA_ZERODIR_AZI :
                                        antAzi = parseDouble(0, 14);
                                        break;
                                    case ANTENNA_ZERODIR_XYZ :
                                        antZeroDir = new Vector3D(parseDouble(0, 14),
                                                                  parseDouble(14, 14),
                                                                  parseDouble(28, 14));
                                        break;
                                    case CENTER_OF_MASS_XYZ :
                                        centerMass = new Vector3D(parseDouble(0, 14),
                                                                  parseDouble(14, 14),
                                                                  parseDouble(28, 14));
                                        break;
                                    case NB_OF_SATELLITES :
                                        nbSat = parseInt(0, 6);
                                        break;
                                    case RCV_CLOCK_OFFS_APPL :
                                        clkOffset = parseInt(0, 6);
                                        break;
                                    case INTERVAL :
                                        interval = parseDouble(0, 10);
                                        break;
                                    case TIME_OF_FIRST_OBS :
                                        switch(satelliteSystem) {
                                            case GPS:
                                                timeScale = timeScales.getGPS();
                                                break;
                                            case GALILEO:
                                                timeScale = timeScales.getGST();
                                                break;
                                            case GLONASS:
                                                timeScale = timeScales.getGLONASS();
                                                break;
                                            case QZSS:
                                                timeScale = timeScales.getQZSS();
                                                break;
                                            case BEIDOU:
                                                timeScale = timeScales.getBDT();
                                                break;
                                            case IRNSS:
                                                timeScale = timeScales.getIRNSS();
                                                break;
                                            case MIXED:
                                                //in Case of Mixed data, Timescale must be specified in the Time of First line
                                                timeScaleStr = parseString(48, 3);

                                                if (timeScaleStr.equals(GPS)) {
                                                    timeScale = timeScales.getGPS();
                                                } else if (timeScaleStr.equals(GAL)) {
                                                    timeScale = timeScales.getGST();
                                                } else if (timeScaleStr.equals(GLO)) {
                                                    timeScale = timeScales.getGLONASS();
                                                } else if (timeScaleStr.equals(QZS)) {
                                                    timeScale = timeScales.getQZSS();
                                                } else if (timeScaleStr.equals(BDT)) {
                                                    timeScale = timeScales.getBDT();
                                                } else if (timeScaleStr.equals(IRN)) {
                                                    timeScale = timeScales.getIRNSS();
                                                } else {
                                                    throw new OrekitException(OrekitMessages.UNSUPPORTED_FILE_FORMAT, name);
                                                }
                                                break;
                                            default :
                                                throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                                          lineNumber, name, line);
                                        }

                                        tFirstObs = new AbsoluteDate(parseInt(0, 6),
                                                                     parseInt(6, 6),
                                                                     parseInt(12, 6),
                                                                     parseInt(18, 6),
                                                                     parseInt(24, 6),
                                                                     parseDouble(30, 13), timeScale);
                                        inFirstObs = true;
                                        break;
                                    case TIME_OF_LAST_OBS :
                                        tLastObs = new AbsoluteDate(parseInt(0, 6),
                                                                    parseInt(6, 6),
                                                                    parseInt(12, 6),
                                                                    parseInt(18, 6),
                                                                    parseInt(24, 6),
                                                                    parseDouble(30, 13), timeScale);
                                        break;
                                    case LEAP_SECONDS :
                                        leapSeconds = parseInt(0, 6);
                                        leapSecondsFuture = parseInt(6, 6);
                                        leapSecondsWeekNum = parseInt(12, 6);
                                        leapSecondsDayNum = parseInt(18, 6);
                                        //Time System Identifier must be added, last A3 String
                                        break;
                                    case PRN_NB_OF_OBS :
                                        //Optional line in header, indicates number of Observations par Satellite
                                        //Not stored for now
                                        break;
                                    case SYS_NB_OBS_TYPES :
                                        obsTypesSystem = null;
                                        typeObs.clear();

                                        obsTypesSystem = SatelliteSystem.parseSatelliteSystem(parseString(0, 1));
                                        nbTypes = parseInt(3, 3);

                                        final int nbLinesTypesObs = (nbTypes + MAX_OBS_TYPES_PER_LINE_RNX3 - 1) / MAX_OBS_TYPES_PER_LINE_RNX3;
                                        for (int j = 0; j < nbLinesTypesObs; j++) {
                                            if (j > 0) {
                                                readLine(reader, true);
                                            }
                                            final int iMax = FastMath.min(MAX_OBS_TYPES_PER_LINE_RNX3, nbTypes - typeObs.size());
                                            for (int i = 0; i < iMax; i++) {
                                                try {
                                                    typeObs.add(ObservationType.valueOf(parseString(7 + (4 * i), 3)));
                                                } catch (IllegalArgumentException iae) {
                                                    throw new OrekitException(iae, OrekitMessages.UNKNOWN_RINEX_FREQUENCY,
                                                                              parseString(7 + (4 * i), 3), name, lineNumber);
                                                }
                                            }
                                        }
                                        listTypeObs.put(obsTypesSystem, new ArrayList<>(typeObs));
                                        inTypesObs = true;
                                        break;
                                    case SIGNAL_STRENGTH_UNIT :
                                        sigStrengthUnit = parseString(0, 20);
                                        break;
                                    case SYS_DCBS_APPLIED :

                                        listAppliedDCBs.add(new AppliedDCBS(SatelliteSystem.parseSatelliteSystem(parseString(0, 1)),
                                                                            parseString(2, 17), parseString(20, 40)));
                                        break;
                                    case SYS_PCVS_APPLIED :

                                        listAppliedPCVS.add(new AppliedPCVS(SatelliteSystem.parseSatelliteSystem(parseString(0, 1)),
                                                                            parseString(2, 17), parseString(20, 40)));
                                        break;
                                    case SYS_SCALE_FACTOR :
                                        satSystemScaleFactor  = null;
                                        scaleFactor           = 1;
                                        nbObsScaleFactor      = 0;

                                        satSystemScaleFactor = SatelliteSystem.parseSatelliteSystem(parseString(0, 1));
                                        scaleFactor          = parseInt(2, 4);
                                        nbObsScaleFactor     = parseInt(8, 2);
                                        final List<ObservationType> typesObsScaleFactor = new ArrayList<>(nbObsScaleFactor);

                                        if (nbObsScaleFactor == 0) {
                                            typesObsScaleFactor.addAll(listTypeObs.get(satSystemScaleFactor));
                                        } else {
                                            final int nbLinesTypesObsScaleFactor = (nbObsScaleFactor + MAX_OBS_TYPES_SCALE_FACTOR_PER_LINE - 1) /
                                                                                   MAX_OBS_TYPES_SCALE_FACTOR_PER_LINE;
                                            for (int j = 0; j < nbLinesTypesObsScaleFactor; j++) {
                                                if ( j > 0) {
                                                    readLine(reader, true);
                                                }
                                                final int iMax = FastMath.min(MAX_OBS_TYPES_SCALE_FACTOR_PER_LINE, nbObsScaleFactor - typesObsScaleFactor.size());
                                                for (int i = 0; i < iMax; i++) {
                                                    typesObsScaleFactor.add(ObservationType.valueOf(parseString(11 + (4 * i), 3)));
                                                }
                                            }
                                        }

                                        scaleFactorCorrections.add(new ScaleFactorCorrection(satSystemScaleFactor,
                                                                                             scaleFactor, typesObsScaleFactor));
                                        break;
                                    case SYS_PHASE_SHIFT  :
                                    case SYS_PHASE_SHIFTS : {

                                        nbSatPhaseShift     = 0;
                                        satsPhaseShift      = null;
                                        corrPhaseShift      = 0.0;
                                        phaseShiftTypeObs   = null;
                                        satSystemPhaseShift = null;

                                        satSystemPhaseShift = SatelliteSystem.parseSatelliteSystem(parseString(0, 1));
                                        final String to = parseString(2, 3);
                                        phaseShiftTypeObs = to.isEmpty() ?
                                                            null :
                                                            ObservationType.valueOf(to.length() < 3 ? "L" + to : to);
                                        nbSatPhaseShift = parseInt(16, 2);
                                        corrPhaseShift = parseDouble(6, 8);

                                        if (nbSatPhaseShift == 0) {
                                            //If nbSat with Phase Shift is not indicated: all the satellites are affected for this Obs Type
                                        } else {
                                            satsPhaseShift = new String[nbSatPhaseShift];
                                            final int nbLinesSatPhaseShift = (nbSatPhaseShift + MAX_N_SAT_PHSHIFT_PER_LINE - 1) / MAX_N_SAT_PHSHIFT_PER_LINE;
                                            for (int j = 0; j < nbLinesSatPhaseShift; j++) {
                                                if (j > 0) {
                                                    readLine(reader, true);
                                                }
                                                final int iMax = FastMath.min(MAX_N_SAT_PHSHIFT_PER_LINE, nbSatPhaseShift - j * MAX_N_SAT_PHSHIFT_PER_LINE);
                                                for (int i = 0; i < iMax; i++) {
                                                    satsPhaseShift[i + 10 * j] = parseString(19 + 4 * i, 3);
                                                }
                                            }
                                        }
                                        phaseShiftCorrections.add(new PhaseShiftCorrection(satSystemPhaseShift,
                                                                                           phaseShiftTypeObs,
                                                                                           corrPhaseShift,
                                                                                           satsPhaseShift));
                                        inPhaseShift = true;
                                        break;
                                    }
                                    case GLONASS_SLOT_FRQ_NB :
                                        //Not defined yet
                                        inGlonassSlot = true;
                                        break;
                                    case GLONASS_COD_PHS_BIS :
                                        //Not defined yet
                                        inGlonassCOD = true;
                                        break;
                                    case END_OF_HEADER :
                                        //We make sure that we have read all the mandatory fields inside the header of the Rinex
                                        if (!inRinexVersion || !inRunBy || !inMarkerName ||
                                            !inObserver || !inRecType || !inAntType ||
                                            !inAntDelta || !inTypesObs || !inFirstObs ||
                                            (formatVersion >= 3.01 && !inPhaseShift) ||
                                            (formatVersion >= 3.03 && (!inGlonassSlot || !inGlonassCOD))) {
                                            throw new OrekitException(OrekitMessages.INCOMPLETE_HEADER, name);
                                        }

                                        //Header information gathered
                                        rinexHeader = new RinexHeader(formatVersion, satelliteSystem,
                                                                      markerName, markerNumber, markerType,
                                                                      observerName, agencyName, receiverNumber,
                                                                      receiverType, receiverVersion, antennaNumber,
                                                                      antennaType, approxPos, antHeight, eccentricities,
                                                                      antRefPoint, obsCode, antPhaseCenter, antBSight,
                                                                      antAzi, antZeroDir, centerMass, sigStrengthUnit,
                                                                      interval, tFirstObs, tLastObs, clkOffset, listAppliedDCBs,
                                                                      listAppliedPCVS, phaseShiftCorrections, leapSeconds,
                                                                      leapSecondsFuture, leapSecondsWeekNum, leapSecondsDayNum);
                                        break;
                                    default :
                                        if (rinexHeader == null) {
                                            //There must be an error due to an unknown Label inside the Header
                                            throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                                      lineNumber, name, line);
                                        }
                                }
                            } else {
                                //If End of Header

                                //Start of a new Observation
                                rcvrClkOffset     =  0;
                                eventFlag         = -1;
                                nbSatObs          = -1;
                                tObs              = null;

                                //A line that starts with ">" correspond to a new observation epoch
                                if (parseString(0, 1).equals(">")) {

                                    eventFlag = parseInt(31, 1);
                                    //If eventFlag>1, we skip the corresponding lines to the next observation
                                    if (eventFlag != 0) {
                                        final int nbLinesSkip = parseInt(32, 3);
                                        for (int i = 0; i < nbLinesSkip; i++) {
                                            readLine(reader, true);
                                        }
                                    } else {

                                        tObs = new AbsoluteDate(parseInt(2, 4),
                                                                parseInt(6, 3),
                                                                parseInt(9, 3),
                                                                parseInt(12, 3),
                                                                parseInt(15, 3),
                                                                parseDouble(18, 11), timeScale);

                                        nbSatObs  = parseInt(32, 3);
                                        //If the total number of satellites was indicated in the Header
                                        if (nbSat != -1 && nbSatObs > nbSat) {
                                            //we check that the number of Sat in the observation is consistent
                                            throw new OrekitException(OrekitMessages.INCONSISTENT_NUMBER_OF_SATS,
                                                                      lineNumber, name, nbSatObs, nbSat);
                                        }
                                        //Read the Receiver Clock offset, if present
                                        rcvrClkOffset = parseDouble(41, 15);
                                        if (Double.isNaN(rcvrClkOffset)) {
                                            rcvrClkOffset = 0.0;
                                        }

                                        //For each one of the Satellites in this Observation
                                        for (int i = 0; i < nbSatObs; i++) {

                                            readLine(reader, true);

                                            //We check that the Satellite type is consistent with Satellite System in the top of the file
                                            final SatelliteSystem satelliteSystemSat = SatelliteSystem.parseSatelliteSystem(parseString(0, 1));
                                            if (!satelliteSystem.equals(SatelliteSystem.MIXED)) {
                                                if (!satelliteSystemSat.equals(satelliteSystem)) {
                                                    throw new OrekitException(OrekitMessages.INCONSISTENT_SATELLITE_SYSTEM,
                                                                              lineNumber, name, satelliteSystem, satelliteSystemSat);
                                                }
                                            }

                                            final int prn = parseInt(1, 2);
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
                                                double value = parseDouble(3 + j * 16, 14);
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
                                                                                        parseInt(17 + j * 16, 1),
                                                                                        parseInt(18 + j * 16, 1)));
                                            }
                                            observationDataSets.add(new ObservationDataSet(rinexHeader, satelliteSystemSat, prnNumber,
                                                                                           tObs, rcvrClkOffset, observationData));

                                        }
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
        }

        /** Read a new line.
         * @param reader reader from where to read line
         * @param complainIfEnd if true an exception should be thrown if end of file is encountered
         * @return true if a line has been read
         * @exception IOException if a read error occurs
         */
        private boolean readLine(final BufferedReader reader, final boolean complainIfEnd)
            throws IOException {
            line = reader.readLine();
            if (line == null && complainIfEnd) {
                throw new OrekitException(OrekitMessages.UNEXPECTED_END_OF_FILE, name);
            }
            lineNumber++;
            return line != null;
        }

        /** Extract a string from a line.
         * @param start start index of the string
         * @param length length of the string
         * @return parsed string
         */
        private String parseString(final int start, final int length) {
            if (line.length() > start) {
                return line.substring(start, FastMath.min(line.length(), start + length)).trim();
            } else {
                return null;
            }
        }

        /** Extract an integer from a line.
         * @param start start index of the integer
         * @param length length of the integer
         * @return parsed integer
         */
        private int parseInt(final int start, final int length) {
            if (line.length() > start && !parseString(start, length).isEmpty()) {
                return Integer.parseInt(parseString(start, length));
            } else {
                return 0;
            }
        }

        /** Extract a double from a line.
         * @param start start index of the real
         * @param length length of the real
         * @return parsed real, or {@code Double.NaN} if field was empty
         */
        private double parseDouble(final int start, final int length) {
            if (line.length() > start && !parseString(start, length).isEmpty()) {
                return Double.parseDouble(parseString(start, length));
            } else {
                return Double.NaN;
            }
        }

        /** Phase Shift corrections.
         * Contains the phase shift corrections used to
         * generate phases consistent with respect to cycle shifts.
         */
        public class PhaseShiftCorrection {

            /** Satellite System. */
            private final SatelliteSystem satSystemPhaseShift;
            /** Carrier Phase Observation Code (may be null). */
            private final ObservationType typeObsPhaseShift;
            /** Phase Shift Corrections (cycles). */
            private final double phaseShiftCorrection;
            /** List of satellites involved. */
            private final String[] satsPhaseShift;

            /** Simple constructor.
             * @param satSystemPhaseShift Satellite System
             * @param typeObsPhaseShift Carrier Phase Observation Code (may be null)
             * @param phaseShiftCorrection Phase Shift Corrections (cycles)
             * @param satsPhaseShift List of satellites involved
             */
            private PhaseShiftCorrection(final SatelliteSystem satSystemPhaseShift,
                                         final ObservationType typeObsPhaseShift,
                                         final double phaseShiftCorrection, final String[] satsPhaseShift) {
                this.satSystemPhaseShift = satSystemPhaseShift;
                this.typeObsPhaseShift = typeObsPhaseShift;
                this.phaseShiftCorrection = phaseShiftCorrection;
                this.satsPhaseShift = satsPhaseShift;
            }

            /** Get the Satellite System.
             * @return Satellite System.
             */
            public SatelliteSystem getSatelliteSystem() {
                return satSystemPhaseShift;
            }
            /** Get the Carrier Phase Observation Code.
             * <p>
             * The observation code may be null for the uncorrected reference
             * signal group
             * </p>
             * @return Carrier Phase Observation Code.
             */
            public ObservationType getTypeObs() {
                return typeObsPhaseShift;
            }
            /** Get the Phase Shift Corrections.
             * @return Phase Shift Corrections (cycles)
             */
            public double getCorrection() {
                return phaseShiftCorrection;
            }
            /** Get the list of satellites involved.
             * @return List of satellites involved (if null, all the sats are involved)
             */
            public String[] getSatsCorrected() {
                //If empty, all the satellites of this constellation are affected for this Observation type
                return satsPhaseShift == null ? null : satsPhaseShift.clone();
            }
        }

        /** Scale Factor to be applied.
         * Contains the scale factors of 10 applied to the data before
         * being stored into the RINEX file.
         */
        public class ScaleFactorCorrection {

            /** Satellite System. */
            private final SatelliteSystem satSystemScaleFactor;
            /** List of Observations types that have been scaled. */
            private final List<ObservationType> typesObsScaleFactor;
            /** Factor to divide stored observations with before use. */
            private final double scaleFactor;

            /** Simple constructor.
             * @param satSystemScaleFactor Satellite System
             * @param scaleFactor Factor to divide stored observations (1,10,100,1000)
             * @param typesObsScaleFactor List of Observations types that have been scaled
             */
            private ScaleFactorCorrection(final SatelliteSystem satSystemScaleFactor,
                                          final double scaleFactor,
                                          final List<ObservationType> typesObsScaleFactor) {
                this.satSystemScaleFactor = satSystemScaleFactor;
                this.scaleFactor = scaleFactor;
                this.typesObsScaleFactor = typesObsScaleFactor;
            }
            /** Get the Satellite System.
             * @return Satellite System
             */
            public SatelliteSystem getSatelliteSystem() {
                return satSystemScaleFactor;
            }
            /** Get the Scale Factor.
             * @return Scale Factor
             */
            public double getCorrection() {
                return scaleFactor;
            }
            /** Get the list of Observation Types scaled.
             * @return List of Observation types scaled
             */
            public List<ObservationType> getTypesObsScaled() {
                return typesObsScaleFactor;
            }
        }

        /** Corrections of Differential Code Biases (DCBs) applied.
         * Contains information on the programs used to correct the observations
         * in RINEX files for differential code biases.
         */
        public class AppliedDCBS {

            /** Satellite system. */
            private final SatelliteSystem satelliteSystem;

            /** Program name used to apply differential code bias corrections. */
            private final String progDCBS;

            /** Source of corrections (URL). */
            private final String sourceDCBS;

            /** Simple constructor.
             * @param satelliteSystem satellite system
             * @param progDCBS Program name used to apply DCBs
             * @param sourceDCBS Source of corrections (URL)
             */
            private AppliedDCBS(final SatelliteSystem satelliteSystem,
                                final String progDCBS, final String sourceDCBS) {
                this.satelliteSystem = satelliteSystem;
                this.progDCBS        = progDCBS;
                this.sourceDCBS      = sourceDCBS;
            }

            /** Get the satellite system.
             * @return satellite system
             */
            public SatelliteSystem getSatelliteSystem() {
                return satelliteSystem;
            }

            /** Get the program name used to apply DCBs.
             * @return  Program name used to apply DCBs
             */
            public String getProgDCBS() {
                return progDCBS;
            }

            /** Get the source of corrections.
             * @return Source of corrections (URL)
             */
            public String getSourceDCBS() {
                return sourceDCBS;
            }

        }

        /** Corrections of antenna phase center variations (PCVs) applied.
         * Contains information on the programs used to correct the observations
         * in RINEX files for antenna phase center variations.
         */
        public class AppliedPCVS {

            /** Satellite system. */
            private final SatelliteSystem satelliteSystem;

            /** Program name used to antenna center variation corrections. */
            private final String progPCVS;

            /** Source of corrections (URL). */
            private final String sourcePCVS;

            /** Simple constructor.
             * @param satelliteSystem satellite system
             * @param progPCVS Program name used for PCVs
             * @param sourcePCVS Source of corrections (URL)
             */
            private AppliedPCVS(final SatelliteSystem satelliteSystem,
                                final String progPCVS, final String sourcePCVS) {
                this.satelliteSystem = satelliteSystem;
                this.progPCVS        = progPCVS;
                this.sourcePCVS      = sourcePCVS;
            }

            /** Get the satellite system.
             * @return satellite system
             */
            public SatelliteSystem getSatelliteSystem() {
                return satelliteSystem;
            }

            /** Get the program name used to apply PCVs.
             * @return  Program name used to apply PCVs
             */
            public String getProgPCVS() {
                return progPCVS;
            }

            /** Get the source of corrections.
             * @return Source of corrections (URL)
             */
            public String getSourcePCVS() {
                return sourcePCVS;
            }

        }
    }

}
