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
package org.orekit.files.ilrs;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.util.FastMath;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ilrs.CRD.AnglesMeasurement;
import org.orekit.files.ilrs.CRD.CRDDataBlock;
import org.orekit.files.ilrs.CRD.Calibration;
import org.orekit.files.ilrs.CRD.CalibrationDetail;
import org.orekit.files.ilrs.CRD.FrRangeMeasurement;
import org.orekit.files.ilrs.CRD.MeteorologicalMeasurement;
import org.orekit.files.ilrs.CRD.NptRangeMeasurement;
import org.orekit.files.ilrs.CRD.RangeMeasurement;
import org.orekit.files.ilrs.CRD.RangeSupplement;
import org.orekit.files.ilrs.CRD.SessionStatistics;
import org.orekit.files.ilrs.CRDConfiguration.CalibrationTargetConfiguration;
import org.orekit.files.ilrs.CRDConfiguration.DetectorConfiguration;
import org.orekit.files.ilrs.CRDConfiguration.LaserConfiguration;
import org.orekit.files.ilrs.CRDConfiguration.MeteorologicalConfiguration;
import org.orekit.files.ilrs.CRDConfiguration.SoftwareConfiguration;
import org.orekit.files.ilrs.CRDConfiguration.SystemConfiguration;
import org.orekit.files.ilrs.CRDConfiguration.TimingSystemConfiguration;
import org.orekit.files.ilrs.CRDConfiguration.TransponderConfiguration;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScale;
import org.orekit.utils.Constants;
import org.orekit.utils.units.Unit;
import org.orekit.utils.units.UnitsConverter;

/**
 * A parser for the CRD data file format.
 * <p>
 * It supports both 1.0 and 2.0 versions
 * <p>
 * <b>Note</b>: Not all the records are read by the parser. Only the most significants are parsed.
 * Contributions are welcome to support more fields in the format.
 * @see <a href="https://ilrs.gsfc.nasa.gov/docs/2009/crd_v1.01.pdf">1.0 file format</a>
 * @see <a href="https://ilrs.gsfc.nasa.gov/docs/2021/crd_v2.01e2.pdf">2.0 file format</a>
 * @author Bryan Cazabonne
 * @author Rongwang Li
 * @since 10.3
 */
public class CRDParser {

    /** Default supported files name pattern for CRD files. */
    public static final String DEFAULT_CRD_SUPPORTED_NAMES = "^(?!0+$)\\w{1,12}\\_\\d{6,8}.\\w{3}$";

    /** Nanometers units. */
    private static final Unit NM = Unit.parse("nm");

    /** Kilohertz units. */
    private static final Unit KHZ = Unit.parse("kHz");

    /** Microseconds units. */
    private static final Unit US = Unit.parse("µs");

    /** Nanoseconds units. */
    private static final Unit NS = Unit.parse("ns");

    /** Picoseconds units. */
    private static final Unit PS = Unit.parse("ps");

    /** mbar to bar converter. */
    private static final UnitsConverter MBAR_TO_BAR = new UnitsConverter(Unit.parse("mbar"), Unit.parse("bar"));

    /** File format. */
    private static final String FILE_FORMAT = "CRD";

    /** Pattern for delimiting regular expressions. */
    private static final Pattern SEPARATOR = Pattern.compile("\\s+");

    /** Pattern for delimiting expressions with comma. */
    private static final Pattern COMMA = Pattern.compile(",");

    /** Identifier of comment record. */
    private static final String COMMENTS_IDENTIFIER = "00";

    /** Pattern of " [-]?(na)". */
    private static final Pattern PATTERN_NA = Pattern.compile(" [-]?(na)");

    /** Time scale used to define epochs in CPF file. */
    private final TimeScale timeScale;

    /**
     * Default constructor.
     * <p>
     * This constructor uses the {@link DataContext#getDefault() default data context}.
     */
    @DefaultDataContext
    public CRDParser() {
        this(DataContext.getDefault().getTimeScales().getUTC());
    }

    /**
     * Constructor.
     * @param utc utc time scale to read epochs
     */
    public CRDParser(final TimeScale utc) {
        this.timeScale = utc;
    }

    /**
     * Get the time scale used to read the file.
     * @return the time scale used to read the file
     */
    public TimeScale getTimeScale() {
        return timeScale;
    }

    /**
     * Parse a CRD file.
     * @param source data source containing the CRD file.
     * @return a parsed CRD file.
     * @throws IOException if {@code reader} throws one.
     */
    public CRD parse(final DataSource source) throws IOException {

        // Initialize internal data structures
        final ParseInfo pi = new ParseInfo();

        int lineNumber = 0;
        Iterable<LineParser> crdParsers = Collections.singleton(LineParser.H1);
        try (BufferedReader reader = new BufferedReader(source.getOpener().openReaderOnce())) {
            nextLine:
                for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                    ++lineNumber;

                    if (line.startsWith(COMMENTS_IDENTIFIER)) {
                        // Comment is in the beginning of the file.
                        crdParsers = Arrays.asList(LineParser.COMMENTS);
                    }

                    for (final LineParser candidate : crdParsers) {
                        if (candidate.canHandle(line)) {
                            try {

                                // Note: since crd v2.01.
                                // The literal “na” is used instead of “-1” for fields that are not applicable or not avaiable.
                                // And there may be "-na".
                                // note: "analog" --> "aNaNlog"
                                line = PATTERN_NA.matcher(line).replaceAll(" " + CRD.STR_NAN);

                                candidate.parse(line, pi);
                                if (pi.done) {
                                    // Return file
                                    return pi.file;
                                }
                                crdParsers = candidate.allowedNext();
                                continue nextLine;
                            } catch (StringIndexOutOfBoundsException | NumberFormatException e) {
                                throw new OrekitException(e,
                                                          OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                          lineNumber, source.getName(), line);
                            }
                        }
                    }
                }

            // We never reached the EOF marker
            throw new OrekitException(OrekitMessages.CRD_UNEXPECTED_END_OF_FILE, lineNumber);

        } catch (IOException ioe) {
            throw new OrekitException(ioe, LocalizedCoreFormats.SIMPLE_MESSAGE, ioe.getLocalizedMessage());
        }

    }

    /**
     * Make sure the epoch is 'right' by doing a day shift if it is required by comparing the current and session start epoch.
     * According to the CRD document, the duration of a session must be less than one day.
     * @param epoch current epoch
     * @param startEpoch start epoch of session
     * @return epoch with rollover is handled.
     */
    private static AbsoluteDate checkRollover(final AbsoluteDate epoch, final AbsoluteDate startEpoch) {
        // If the current epoch is before the start epoch of a session, the epoch should be shifted by 1 day.
        // For METEO(20) data, the epoch may be a 'little' (10 hours?) before the session start epoch.
        // And also for CALIB(40) and CALIB_DETAILS(41)
        return epoch.durationFrom(startEpoch) < -36000 ? epoch.shiftedBy(Constants.JULIAN_DAY) : epoch;
    }

    /** Transient data used for parsing a CRD file. The data is kept in a
     * separate data structure to make the parser thread-safe.
     * <p><b>Note</b>: The class intentionally does not provide accessor
     * methods, as it is only used internally for parsing a CRD file.</p>
     */
    private class ParseInfo {

        /** The corresponding CDR file. */
        private CRD file;

        /** Version. */
        private int version;

        /** The current data block. */
        private CRDDataBlock dataBlock;

        /** Data block header. */
        private CRDHeader header;

        /** Cofiguration records. */
        private CRDConfiguration configurationRecords;

        /** Time scale. */
        private TimeScale timeScale;

        /** Current data block start epoch, DateComponents only. */
        private DateComponents startEpochDateComponents;

        /** End Of File reached indicator. */
        private boolean done;

        /**
         * Constructor.
         */
        protected ParseInfo() {

            // Initialise default values
            this.done       = false;
            this.version    = 1;
            this.startEpochDateComponents = DateComponents.J2000_EPOCH;

            // Initialise empty object
            this.file                 = new CRD();
            this.header               = new CRDHeader();
            this.configurationRecords = new CRDConfiguration();
            this.dataBlock            = new CRDDataBlock();

            // Time scale
            this.timeScale = CRDParser.this.timeScale;

        }

    }

    /** Parsers for specific lines. */
    private enum LineParser {

        /** Format header. */
        H1("H1", "h1") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {

                // Data contained in the line
                final String[] values = SEPARATOR.split(line);

                // Format and version
                final String format = values[1];
                pi.version = Integer.parseInt(values[2]);

                // Throw an exception if format is not equal to "CRD"
                if (!format.equalsIgnoreCase(FILE_FORMAT)) {
                    throw new OrekitException(OrekitMessages.UNEXPECTED_FORMAT_FOR_ILRS_FILE, FILE_FORMAT, format);
                }

                // Fill first elements
                pi.header.setFormat(format);
                pi.header.setVersion(pi.version);

                // Epoch of ephemeris production
                final int year  = Integer.parseInt(values[3]);
                final int month = Integer.parseInt(values[4]);
                final int day   = Integer.parseInt(values[5]);
                pi.header.setProductionEpoch(new DateComponents(year, month, day));

                // Hour of ephemeris production
                pi.header.setProductionHour(Integer.parseInt(values[6]));

            }

            /** {@inheritDoc} */
            @Override
            public Iterable<LineParser> allowedNext() {
                return Arrays.asList(H2, COMMENTS);
            }

        },

        /** Station header. */
        H2("H2", "h2") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {

                // Data contained in the line
                final String[] values = SEPARATOR.split(line);

                // Station name
                pi.header.setStationName(values[1]);

                // Crustal Dynamics Project keys
                pi.header.setSystemIdentifier(Integer.parseInt(values[2]));
                pi.header.setSystemNumber(Integer.parseInt(values[3]));
                pi.header.setSystemOccupancy(Integer.parseInt(values[4]));

                // Station epoch time scale
                pi.header.setEpochIdentifier(Integer.parseInt(values[5]));

                // Station network
                if (pi.version == 2) {
                    pi.header.setStationNetword(values[6]);
                } else {
                    pi.header.setStationNetword(CRD.STR_VALUE_NOT_AVAILABLE);
                }

            }

            /** {@inheritDoc} */
            @Override
            public Iterable<LineParser> allowedNext() {
                return Arrays.asList(H3, C0, C1, C2, C3, C4, C5, C6, C7, COMMENTS);
            }

        },

        /** Target header. */
        H3("H3", "h3") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {

                // Data contained in the line
                final String[] values = SEPARATOR.split(line);

                // Target name
                pi.header.setName(values[1]);

                // Identifiers
                pi.header.setIlrsSatelliteId(values[2]);
                pi.header.setSic(values[3]);
                pi.header.setNoradId(values[4]);

                // Spacecraft Epoch Time Scale
                pi.header.setSpacecraftEpochTimeScale(Integer.parseInt(values[5]));

                // Target class and location (if needed)
                pi.header.setTargetClass(Integer.parseInt(values[6]));
                if (pi.version == 2) {
                    // na=unknown (for use when tracking a transponder using a Version 1 CPF)
                    // treated it as -1
                    pi.header.setTargetLocation(readIntegerWithNaN(values[7], -1));
                }

            }

            /** {@inheritDoc} */
            @Override
            public Iterable<LineParser> allowedNext() {
                return Arrays.asList(H4, C0, C1, C2, C3, C4, C5, C6, C7, COMMENTS);
            }

        },

        /** Session (Pass/Pass segment) header. */
        H4("H4", "h4") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {

                // Data contained in the line
                final String[] values = SEPARATOR.split(line);

                // Data type
                pi.header.setDataType(Integer.parseInt(values[1]));

                // Start epoch
                final int    yearS   = Integer.parseInt(values[2]);
                final int    monthS  = Integer.parseInt(values[3]);
                final int    dayS    = Integer.parseInt(values[4]);
                final int    hourS   = Integer.parseInt(values[5]);
                final int    minuteS = Integer.parseInt(values[6]);
                final double secondS = Integer.parseInt(values[7]);

                pi.startEpochDateComponents = new DateComponents(yearS, monthS, dayS);

                pi.header.setStartEpoch(new AbsoluteDate(yearS, monthS, dayS,
                        hourS, minuteS, secondS,
                        pi.timeScale));

                // End epoch
                // since crd v2.01
                // Set the ending date and time fields to “na” if not available.
                if (pi.version == 2 && values[8].equalsIgnoreCase("")) {
                    pi.header.setEndEpoch(null);
                } else {
                    final int yearE = Integer.parseInt(values[8]);
                    final int monthE = Integer.parseInt(values[9]);
                    final int dayE = Integer.parseInt(values[10]);
                    final int hourE = Integer.parseInt(values[11]);
                    final int minuteE = Integer.parseInt(values[12]);
                    final double secondE = Integer.parseInt(values[13]);

                    // fixed 2022-12-12
                    // if yearE or monthE is -1.
                    if (monthE == -1) {
                        pi.header.setEndEpoch(null);
                    } else {
                        pi.header.setEndEpoch(new AbsoluteDate(yearE, monthE, dayE, hourE, minuteE, secondE, pi.timeScale));
                    }
                }

                // Data release
                pi.header.setDataReleaseFlag(Integer.parseInt(values[14]));

                // Correction flags
                pi.header.setIsTroposphericRefractionApplied(readBoolean(values[15]));
                pi.header.setIsCenterOfMassCorrectionApplied(readBoolean(values[16]));
                pi.header.setIsReceiveAmplitudeCorrectionApplied(readBoolean(values[17]));
                pi.header.setIsStationSystemDelayApplied(readBoolean(values[18]));
                pi.header.setIsTransponderDelayApplied(readBoolean(values[19]));

                // Range type indicator
                pi.header.setRangeType(Integer.parseInt(values[20]));

                // Data quality indicator
                pi.header.setQualityIndicator(Integer.parseInt(values[21]));

            }

            /** {@inheritDoc} */
            @Override
            public Iterable<LineParser> allowedNext() {
                return Arrays.asList(H5, C0, C1, C2, C3, C4, C5, C6, C7, TEN, ELEVEN, TWELVE, METEO, METEO_SUPP, ANGLES,
                                     CALIB, CALIB_DETAILS, CALIB_SHOT, STAT, COMPATIBILITY, COMMENTS, CUSTOM);
            }

        },

        /** Prediction header. */
        H5("H5", "h5") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {

                // Data contained in the line
                final String[] values = SEPARATOR.split(line);

                // Fill data
                pi.header.setPredictionType(Integer.parseInt(values[1]));
                pi.header.setYearOfCentury(Integer.parseInt(values[2]));
                pi.header.setDateAndTime(values[3]);
                pi.header.setPredictionProvider(values[4]);
                pi.header.setSequenceNumber(Integer.parseInt(values[5]));

            }

            /** {@inheritDoc} */
            @Override
            public Iterable<LineParser> allowedNext() {
                return Arrays.asList(C0, C1, C2, C3, C4, C5, C6, C7, TEN, ELEVEN, TWELVE, METEO, METEO_SUPP, ANGLES, CALIB,
                                     CALIB_DETAILS, CALIB_SHOT, STAT, COMPATIBILITY, COMMENTS, CUSTOM);
            }

        },

        /** System configuration record. */
        C0("C0", "c0") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {

                // Initialise an empty system configuration record
                final SystemConfiguration systemRecord = new SystemConfiguration();

                // Data contained in the line
                final String[] values = SEPARATOR.split(line);

                // Wavelength
                systemRecord.setWavelength(NM.toSI(Double.parseDouble(values[2])));

                // System ID
                systemRecord.setSystemId(values[3]);

                // Components, A B C D E F G
                systemRecord.setComponents(Arrays.copyOfRange(values, 4, values.length));

                // Add the system configuration record
                pi.configurationRecords.addConfigurationRecord(systemRecord);

            }

            /** {@inheritDoc} */
            @Override
            public Iterable<LineParser> allowedNext() {
                return Arrays.asList(H3, H4, H5, C0, C1, C2, C3, C4, C5, C6, C7, TEN, ELEVEN, TWELVE, METEO, METEO_SUPP,
                                     ANGLES, CALIB, CALIB_DETAILS, CALIB_SHOT, STAT, COMPATIBILITY, COMMENTS, CUSTOM);
            }

        },


        /** Laser configuration record. */
        C1("C1", "c1") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {

                // Initialise an empty laser configuration record
                final LaserConfiguration laserRecord = new LaserConfiguration();

                // Data contained in the line
                final String[] values = SEPARATOR.split(line);

                // Fill values
                laserRecord.setLaserId(values[2]);
                laserRecord.setLaserType(values[3]);
                laserRecord.setPrimaryWavelength(NM.toSI(Double.parseDouble(values[4])));
                laserRecord.setNominalFireRate(Double.parseDouble(values[5]));
                laserRecord.setPulseEnergy(Double.parseDouble(values[6]));
                laserRecord.setPulseWidth(Double.parseDouble(values[7]));
                laserRecord.setBeamDivergence(Double.parseDouble(values[8]));
                laserRecord.setPulseInOutgoingSemiTrain(readIntegerWithNaN(values[9], 1));

                // Add the laser configuration record
                pi.configurationRecords.addConfigurationRecord(laserRecord);

            }

            /** {@inheritDoc} */
            @Override
            public Iterable<LineParser> allowedNext() {
                return Arrays.asList(C2, C3, C4, C5, C6, C7, TEN, ELEVEN, METEO, ANGLES, CALIB, STAT, COMPATIBILITY, COMMENTS);
            }

        },

        /** Detector configuration record. */
        C2("C2", "c2") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {

                // Initialise an empty detector configuration record
                final DetectorConfiguration detectorRecord = new DetectorConfiguration();

                // Data contained in the line
                final String[] values = SEPARATOR.split(line);

                // Fill values
                detectorRecord.setDetectorId(values[2]);
                detectorRecord.setDetectorType(values[3]);
                detectorRecord.setApplicableWavelength(NM.toSI(Double.parseDouble(values[4])));
                detectorRecord.setQuantumEfficiency(Double.parseDouble(values[5]));
                detectorRecord.setAppliedVoltage(Double.parseDouble(values[6]));
                detectorRecord.setDarkCount(KHZ.toSI(Double.parseDouble(values[7])));
                detectorRecord.setOutputPulseType(values[8]);
                detectorRecord.setOutputPulseWidth(Double.parseDouble(values[9]));
                detectorRecord.setSpectralFilter(NM.toSI(Double.parseDouble(values[10])));
                detectorRecord.setTransmissionOfSpectralFilter(Double.parseDouble(values[11]));
                detectorRecord.setSpatialFilter(Double.parseDouble(values[12]));
                detectorRecord.setExternalSignalProcessing(values[13]);

                // Check file version for additional data
                if (pi.version == 2) {
                    detectorRecord.setAmplifierGain(Double.parseDouble(values[14]));
                    detectorRecord.setAmplifierBandwidth(KHZ.toSI(Double.parseDouble(values[15])));
                    detectorRecord.setAmplifierInUse(values[16]);
                } else {
                    detectorRecord.setAmplifierGain(Double.NaN);
                    detectorRecord.setAmplifierBandwidth(Double.NaN);
                    detectorRecord.setAmplifierInUse(CRD.STR_VALUE_NOT_AVAILABLE);
                }

                // Add the detector configuration record
                pi.configurationRecords.addConfigurationRecord(detectorRecord);

            }

            /** {@inheritDoc} */
            @Override
            public Iterable<LineParser> allowedNext() {
                return Arrays.asList(H3, H4, H5, C0, C1, C2, C3, C4, C5, C6, C7, TEN, ELEVEN, TWELVE, METEO, METEO_SUPP,
                                     ANGLES, CALIB, CALIB_DETAILS, CALIB_SHOT, STAT, COMPATIBILITY, COMMENTS, CUSTOM);
            }

        },

        /** Timing system configuration record. */
        C3("C3", "c3") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {

                // Initialise an empty timing system configuration record
                final TimingSystemConfiguration timingRecord = new TimingSystemConfiguration();

                // Data contained in the line
                final String[] values = SEPARATOR.split(line);

                // Fill values
                timingRecord.setLocalTimingId(values[2]);
                timingRecord.setTimeSource(values[3]);
                timingRecord.setFrequencySource(values[4]);
                timingRecord.setTimer(values[5]);
                final String timerSerialNumber = values[6];
                if (CRD.STR_NAN.equalsIgnoreCase(timerSerialNumber)) {
                    // The timer serial number may be "na"
                    timingRecord.setTimerSerialNumber(CRD.STR_VALUE_NOT_AVAILABLE);
                } else {
                    timingRecord.setTimerSerialNumber(timerSerialNumber);
                }
                timingRecord.setEpochDelayCorrection(US.toSI(Double.parseDouble(values[7])));

                // Add the timing system configuration record
                pi.configurationRecords.addConfigurationRecord(timingRecord);

            }

            /** {@inheritDoc} */
            @Override
            public Iterable<LineParser> allowedNext() {
                return Arrays.asList(H3, H4, H5, C0, C1, C2, C3, C4, C5, C6, C7, TEN, ELEVEN, TWELVE, METEO, METEO_SUPP,
                                     ANGLES, CALIB, CALIB_DETAILS, CALIB_SHOT, STAT, COMPATIBILITY, COMMENTS, CUSTOM);
            }

        },

        /** Transponder configuration record. */
        C4("C4", "c4") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {

                // Initialise an empty transponder configuration record
                final TransponderConfiguration transponderRecord = new TransponderConfiguration();

                // Data contained in the line
                final String[] values = SEPARATOR.split(line);

                // Estimated offsets and drifts
                transponderRecord.setTransponderId(values[2]);
                transponderRecord.setStationUTCOffset(NS.toSI(Double.parseDouble(values[3])));
                transponderRecord.setStationOscDrift(Double.parseDouble(values[4]));
                transponderRecord.setTranspUTCOffset(NS.toSI(Double.parseDouble(values[5])));
                transponderRecord.setTranspOscDrift(Double.parseDouble(values[6]));

                // Transponder clock reference time
                transponderRecord.setTranspClkRefTime(Double.parseDouble(values[7]));

                // Clock and drift indicators
                transponderRecord.setStationClockAndDriftApplied(Integer.parseInt(values[8]));
                transponderRecord.setSpacecraftClockAndDriftApplied(Integer.parseInt(values[9]));

                // Spacecraft time simplified
                transponderRecord.setIsSpacecraftTimeSimplified(readBoolean(values[10]));

                // Add the transponder configuration record
                pi.configurationRecords.addConfigurationRecord(transponderRecord);

            }

            /** {@inheritDoc} */
            @Override
            public Iterable<LineParser> allowedNext() {
                return Arrays.asList(H3, H4, H5, C0, C1, C2, C3, C4, C5, C6, C7, TEN, ELEVEN, TWELVE, METEO, METEO_SUPP,
                                     ANGLES, CALIB, CALIB_DETAILS, CALIB_SHOT, STAT, COMPATIBILITY, COMMENTS, CUSTOM);
            }

        },

        /** Software configuration record. */
        C5("C5", "c5") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {

                // Initialise an empty software configuration record
                final SoftwareConfiguration softwareRecord = new SoftwareConfiguration();

                // Data contained in the line
                final String[] values = SEPARATOR.split(line);

                // Fill values
                softwareRecord.setSoftwareId(values[2]);
                softwareRecord.setTrackingSoftwares(COMMA.split(values[3]));
                softwareRecord.setTrackingSoftwareVersions(COMMA.split(values[4]));
                softwareRecord.setProcessingSoftwares(COMMA.split(values[5]));
                softwareRecord.setProcessingSoftwareVersions(COMMA.split(values[6]));

                // Add the software configuration record
                pi.configurationRecords.addConfigurationRecord(softwareRecord);

            }

            /** {@inheritDoc} */
            @Override
            public Iterable<LineParser> allowedNext() {
                return Arrays.asList(H3, H4, H5, C0, C1, C2, C3, C4, C5, C6, C7, TEN, ELEVEN, TWELVE, METEO, METEO_SUPP,
                                     ANGLES, CALIB, CALIB_DETAILS, CALIB_SHOT, STAT, COMPATIBILITY, COMMENTS, CUSTOM);
            }

        },

        /** Meteorological instrumentation configuration record. */
        C6("C6", "c6") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {

                // Initialise an empty meteorological configuration record
                final MeteorologicalConfiguration meteoRecord = new MeteorologicalConfiguration();

                // Data contained in the line
                final String[] values = SEPARATOR.split(line);

                // Fill values
                meteoRecord.setMeteorologicalId(values[2]);
                meteoRecord.setPressSensorManufacturer(values[3]);
                meteoRecord.setPressSensorModel(values[4]);
                meteoRecord.setPressSensorSerialNumber(values[5]);
                meteoRecord.setTempSensorManufacturer(values[6]);
                meteoRecord.setTempSensorModel(values[7]);
                meteoRecord.setTempSensorSerialNumber(values[8]);
                meteoRecord.setHumiSensorManufacturer(values[9]);
                meteoRecord.setHumiSensorModel(values[10]);
                meteoRecord.setHumiSensorSerialNumber(values[11]);

                // Add the meteorological configuration record
                pi.configurationRecords.addConfigurationRecord(meteoRecord);

            }

            /** {@inheritDoc} */
            @Override
            public Iterable<LineParser> allowedNext() {
                return Arrays.asList(H3, H4, H5, C0, C1, C2, C3, C4, C5, C6, C7, TEN, ELEVEN, TWELVE, METEO, METEO_SUPP,
                                     ANGLES, CALIB, CALIB_DETAILS, CALIB_SHOT, STAT, COMPATIBILITY, COMMENTS, CUSTOM);
            }

        },

        /** Calibration Target configuration record. */
        C7("C7", "c7") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {

                // Initialise an empty calibration target configuration record
                final CalibrationTargetConfiguration calibRecord = new CalibrationTargetConfiguration();

                // Data contained in the line
                final String[] values = SEPARATOR.split(line);

                // Fill values
                calibRecord.setConfigurationId(values[2]);
                calibRecord.setTargetName(values[3]);
                calibRecord.setSurveyedTargetDistance(Double.parseDouble(values[4]));
                calibRecord.setSurveyError(Double.parseDouble(values[5]) * 1e-3);  // mm --> m
                calibRecord.setSumOfAllConstantDelays(Double.parseDouble(values[6]));
                calibRecord.setPulseEnergy(Double.parseDouble(values[7]));
                calibRecord.setProcessingSoftwareName(values[8]);
                calibRecord.setProcessingSoftwareVersion(values[9]);

                // Add the calibration target configuration record
                pi.configurationRecords.addConfigurationRecord(calibRecord);
            }

            /** {@inheritDoc} */
            @Override
            public Iterable<LineParser> allowedNext() {
                return Arrays.asList(H3, H4, H5, C0, C1, C2, C3, C4, C5, C6, C7, TEN, ELEVEN, TWELVE, METEO, METEO_SUPP,
                                     ANGLES, CALIB, CALIB_DETAILS, CALIB_SHOT, STAT, COMPATIBILITY, COMMENTS, CUSTOM);
            }

        },

        /** Range Record (Full rate, Sampled Engineering/Quicklook). */
        TEN("10") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {

                // Data contained in the line
                final String[] values = SEPARATOR.split(line);

                // Read data
                final double secOfDay         = Double.parseDouble(values[1]);
                final double timeOfFlight     = Double.parseDouble(values[2]);
                final String systemConfigId   = values[3];
                final int    epochEvent       = Integer.parseInt(values[4]);
                final int    filterFlag       = Integer.parseInt(values[5]);
                final int    detectorChannel  = Integer.parseInt(values[6]);
                final int    stopNumber       = Integer.parseInt(values[7]);
                final int    receiveAmplitude = readIntegerWithNaN(values[8], -1);

                int transmitAmplitude = -1;
                if (pi.version == 2) {
                    transmitAmplitude = readIntegerWithNaN(values[9], -1);
                }

                // Initialise a new Range measurement
                AbsoluteDate epoch = new AbsoluteDate(pi.startEpochDateComponents, new TimeComponents(secOfDay), pi.timeScale);
                // Check rollover
                epoch = checkRollover(epoch, pi.header.getStartEpoch());
                final RangeMeasurement range = new FrRangeMeasurement(epoch, timeOfFlight, epochEvent, systemConfigId,
                        filterFlag, detectorChannel, stopNumber, receiveAmplitude, transmitAmplitude);
                pi.dataBlock.addRangeData(range);

            }

            /** {@inheritDoc} */
            @Override
            public Iterable<LineParser> allowedNext() {
                return Arrays.asList(H8, TEN, TWELVE, METEO, METEO_SUPP, ANGLES, CALIB, CALIB_DETAILS, CALIB_SHOT, STAT,
                                     COMPATIBILITY, COMMENTS, CUSTOM);
            }

        },

        /** Range Record (Normal point). */
        ELEVEN("11") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {

                // Data contained in the line
                final String[] values = SEPARATOR.split(line);

                // Read data
                final double   secOfDay          = Double.parseDouble(values[1]);
                final double   timeOfFlight      = Double.parseDouble(values[2]);
                final String   systemConfigId    = values[3];
                final int      epochEvent        = Integer.parseInt(values[4]);
                final double   windowLength      = Double.parseDouble(values[5]);
                final int      numberOfRawRanges = Integer.parseInt(values[6]);
                final double   binRms            = PS.toSI(Double.parseDouble(values[7]));
                final double   binSkew           = Double.parseDouble(values[8]);
                final double   binKurtosis       = Double.parseDouble(values[9]);
                final double   binPeakMinusMean  = PS.toSI(Double.parseDouble(values[10]));
                final double   returnRate        = Double.parseDouble(values[11]);
                final int      detectorChannel   = Integer.parseInt(values[12]);

                double snr = Double.NaN;
                if (pi.version == 2) {
                    snr    = Double.parseDouble(values[13]);
                }

                // Initialise a new Range measurement
                AbsoluteDate epoch = new AbsoluteDate(pi.startEpochDateComponents, new TimeComponents(secOfDay), pi.timeScale);
                // Check rollover
                epoch = checkRollover(epoch, pi.header.getStartEpoch());
                final RangeMeasurement range = new NptRangeMeasurement(epoch, timeOfFlight, epochEvent, snr,
                        systemConfigId, windowLength, numberOfRawRanges, binRms, binSkew, binKurtosis, binPeakMinusMean,
                        returnRate, detectorChannel);
                pi.dataBlock.addRangeData(range);

            }

            /** {@inheritDoc} */
            @Override
            public Iterable<LineParser> allowedNext() {
                return Arrays.asList(H8, ELEVEN, TWELVE, METEO, METEO_SUPP, ANGLES, CALIB, CALIB_DETAILS, CALIB_SHOT, STAT,
                                     COMPATIBILITY, COMMENTS, CUSTOM);
            }

        },

        /** Range Supplement Record. */
        TWELVE("12") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {

                // Data contained in the line
                final String[] values = SEPARATOR.split(line);

                // Read data
                final double   secOfDay                   = Double.parseDouble(values[1]);
                final String   systemConfigId             = values[2];
                final double   troposphericRefractionCorr = PS.toSI(Double.parseDouble(values[3]));
                final double   centerOfMassCorr           = Double.parseDouble(values[4]);
                final double   ndFilterValue              = Double.parseDouble(values[5]);
                final double   timeBiasApplied            = Double.parseDouble(values[6]);

                double rangeRate = Double.NaN;
                if (pi.version == 2) {
                    rangeRate    = Double.parseDouble(values[7]);
                }

                // Initialise a new Range measurement
                AbsoluteDate epoch = new AbsoluteDate(pi.startEpochDateComponents, new TimeComponents(secOfDay), pi.timeScale);
                // Check rollover
                epoch = checkRollover(epoch, pi.header.getStartEpoch());
                final RangeSupplement rangeSup = new RangeSupplement(epoch, systemConfigId, troposphericRefractionCorr,
                        centerOfMassCorr, ndFilterValue, timeBiasApplied, rangeRate);
                pi.dataBlock.addRangeSupplementData(rangeSup);

            }

            /** {@inheritDoc} */
            @Override
            public Iterable<LineParser> allowedNext() {
                return Arrays.asList(H8, TEN, ELEVEN, TWELVE, METEO, ANGLES, CALIB, STAT, COMPATIBILITY, COMMENTS);
            }

        },

        /** Meteorological record. */
        METEO("20") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {

                // Data contained in the line
                final String[] values = SEPARATOR.split(line);

                // Read data
                final double   secOfDay       = Double.parseDouble(values[1]);
                final double   pressure       = MBAR_TO_BAR.convert(Double.parseDouble(values[2]));
                final double   temperature    = Double.parseDouble(values[3]);
                final double   humidity       = Double.parseDouble(values[4]);
                final int      originOfValues = Integer.parseInt(values[5]);

                // Initialise a new Range measurement
                AbsoluteDate epoch = new AbsoluteDate(pi.startEpochDateComponents, new TimeComponents(secOfDay), pi.timeScale);
                // Check rollover
                epoch = checkRollover(epoch, pi.header.getStartEpoch());
                final MeteorologicalMeasurement meteo = new MeteorologicalMeasurement(epoch, pressure, temperature,
                        humidity, originOfValues);
                pi.dataBlock.addMeteoData(meteo);

            }

            /** {@inheritDoc} */
            @Override
            public Iterable<LineParser> allowedNext() {
                return Arrays.asList(H8, TEN, ELEVEN, TWELVE, METEO, METEO_SUPP, ANGLES, CALIB, CALIB_DETAILS, CALIB_SHOT,
                                     STAT, COMPATIBILITY, COMMENTS, CUSTOM);
            }

        },

        /** Meteorological Supplement record. */
        METEO_SUPP("21") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {
                // Not implemented yet
            }

            /** {@inheritDoc} */
            @Override
            public Iterable<LineParser> allowedNext() {
                return Arrays.asList(H8, TEN, ELEVEN, TWELVE, METEO, METEO_SUPP, ANGLES, CALIB, CALIB_DETAILS, CALIB_SHOT,
                                     STAT, COMPATIBILITY, COMMENTS, CUSTOM);
            }

        },

        /** Pointing Angle Record. */
        ANGLES("30") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {

                // Data contained in the line
                final String[] values = SEPARATOR.split(line);

                // Read data
                final double  secOfDay              = Double.parseDouble(values[1]);
                final double  azmiuth               = FastMath.toRadians(Double.parseDouble(values[2]));
                final double  elevation             = FastMath.toRadians(Double.parseDouble(values[3]));
                final int     directionFlag         = Integer.parseInt(values[4]);
                final int     orginFlag             = Integer.parseInt(values[5]);
                final boolean isRefractionCorrected = readBoolean(values[6]);


                // Angles rates
                double azimuthRate   = Double.NaN;
                double elevationRate = Double.NaN;
                if (pi.version == 2) {
                    // degrees/second ==> rad/s
                    azimuthRate   = FastMath.toRadians(Double.parseDouble(values[7]));
                    elevationRate = FastMath.toRadians(Double.parseDouble(values[8]));
                }

                // Initialise a new angles measurement
                AbsoluteDate epoch = new AbsoluteDate(pi.startEpochDateComponents, new TimeComponents(secOfDay), pi.timeScale);
                // Check rollover
                epoch = checkRollover(epoch, pi.header.getStartEpoch());
                final AnglesMeasurement angles = new AnglesMeasurement(epoch, azmiuth, elevation,
                        directionFlag, orginFlag,
                        isRefractionCorrected,
                        azimuthRate, elevationRate);
                pi.dataBlock.addAnglesData(angles);

            }

            /** {@inheritDoc} */
            @Override
            public Iterable<LineParser> allowedNext() {
                return Arrays.asList(H8, TEN, ELEVEN, TWELVE, METEO, METEO_SUPP, ANGLES, CALIB, CALIB_DETAILS, CALIB_SHOT,
                                     STAT, COMPATIBILITY, COMMENTS, CUSTOM);
            }

        },

        /** Calibration Record. */
        CALIB("40") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {

                // Data contained in the line
                final String[] values = SEPARATOR.split(line);

                // Read data
                final double   secOfDay               = Double.parseDouble(values[1]);
                final int      typeOfData             = Integer.parseInt(values[2]);
                final String   systemConfigId         = values[3];
                final int      numberOfPointsRecorded = readIntegerWithNaN(values[4], -1);
                final int      numberOfPointsUsed     = readIntegerWithNaN(values[5], -1);
                final double   oneWayDistance         = Double.parseDouble(values[6]);
                final double   systemDelay            = PS.toSI(Double.parseDouble(values[7]));
                final double   delayShift             = PS.toSI(Double.parseDouble(values[8]));
                final double   rms                    = PS.toSI(Double.parseDouble(values[9]));
                final double   skew                   = Double.parseDouble(values[10]);
                final double   kurtosis               = Double.parseDouble(values[11]);
                final double   peakMinusMean          = PS.toSI(Double.parseDouble(values[12]));
                final int      typeIndicator          = Integer.parseInt(values[13]);
                final int      shiftTypeIndicator     = Integer.parseInt(values[14]);
                final int      detectorChannel        = Integer.parseInt(values[15]);

                // Check file version for additional data
                int    span       = 0;
                double returnRate = Double.NaN;
                if (pi.version == 2) {
                    // fixed 20230321
                    // the span may be "na"
                    span       = readIntegerWithNaN(values[16], -1);
                    returnRate = Double.parseDouble(values[17]);
                }

                // Initialise a new angles measurement
                AbsoluteDate epoch = new AbsoluteDate(pi.startEpochDateComponents, new TimeComponents(secOfDay), pi.timeScale);
                // Check rollover
                epoch = checkRollover(epoch, pi.header.getStartEpoch());
                final Calibration cal = new Calibration(epoch, typeOfData, systemConfigId, numberOfPointsRecorded,
                        numberOfPointsUsed, oneWayDistance, systemDelay, delayShift, rms, skew, kurtosis, peakMinusMean,
                        typeIndicator, shiftTypeIndicator, detectorChannel, span, returnRate);
                pi.dataBlock.addCalibrationData(cal);

            }

            /** {@inheritDoc} */
            @Override
            public Iterable<LineParser> allowedNext() {
                return Arrays.asList(H8, TEN, ELEVEN, TWELVE, METEO, METEO_SUPP, ANGLES, CALIB, CALIB_DETAILS, CALIB_SHOT,
                                     STAT, COMPATIBILITY, COMMENTS, CUSTOM);
            }

        },

        /** Calibration Details Record. */
        CALIB_DETAILS("41") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {

                // Data contained in the line
                final String[] values = SEPARATOR.split(line);

                // Read data
                final double   secOfDay               = Double.parseDouble(values[1]);
                final int      typeOfData             = Integer.parseInt(values[2]);
                final String   systemConfigId         = values[3];
                final int      numberOfPointsRecorded = readIntegerWithNaN(values[4], -1);
                final int      numberOfPointsUsed     = readIntegerWithNaN(values[5], -1);
                final double   oneWayDistance         = Double.parseDouble(values[6]);
                final double   systemDelay            = PS.toSI(Double.parseDouble(values[7]));
                final double   delayShift             = PS.toSI(Double.parseDouble(values[8]));
                final double   rms                    = PS.toSI(Double.parseDouble(values[9]));
                final double   skew                   = Double.parseDouble(values[10]);
                final double   kurtosis               = Double.parseDouble(values[11]);
                final double   peakMinusMean          = PS.toSI(Double.parseDouble(values[12]));
                final int      typeIndicator          = Integer.parseInt(values[13]);
                final int      shiftTypeIndicator     = Integer.parseInt(values[14]);
                final int      detectorChannel        = Integer.parseInt(values[15]);

                // Check file version for additional data
                int    span       = 0;
                double returnRate = Double.NaN;
                if (pi.version == 2) {
                    span       = Integer.parseInt(values[16]);
                    returnRate = Double.parseDouble(values[17]);
                }

                // Initialise a new angles measurement
                AbsoluteDate epoch = new AbsoluteDate(pi.startEpochDateComponents, new TimeComponents(secOfDay), pi.timeScale);
                // Check rollover
                epoch = checkRollover(epoch, pi.header.getStartEpoch());
                final CalibrationDetail cal = new CalibrationDetail(epoch, typeOfData, systemConfigId,
                        numberOfPointsRecorded, numberOfPointsUsed, oneWayDistance, systemDelay, delayShift, rms, skew,
                        kurtosis, peakMinusMean, typeIndicator, shiftTypeIndicator, detectorChannel, span, returnRate);
                pi.dataBlock.addCalibrationDetailData(cal);

            }

            /** {@inheritDoc} */
            @Override
            public Iterable<LineParser> allowedNext() {
                return Arrays.asList(H8, TEN, ELEVEN, TWELVE, METEO, METEO_SUPP, ANGLES, CALIB, CALIB_DETAILS, CALIB_SHOT,
                                     STAT, COMPATIBILITY, COMMENTS, CUSTOM);
            }

        },

        /** Calibration "Shot" Record. */
        CALIB_SHOT("42") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {
                // Not implemented yet
            }

            /** {@inheritDoc} */
            @Override
            public Iterable<LineParser> allowedNext() {
                return Arrays.asList(H8, TEN, ELEVEN, TWELVE, METEO, METEO_SUPP, ANGLES, CALIB, CALIB_DETAILS, CALIB_SHOT,
                                     STAT, COMPATIBILITY, COMMENTS, CUSTOM);
            }

        },

        /** Session (Pass) Statistics Record. */
        STAT("50") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {

                // Data contained in the line
                final String[] values = SEPARATOR.split(line);

                // Read data
                final String systemConfigId    = values[1];
                final double rms               = PS.toSI(Double.parseDouble(values[2]));
                final double skewness          = Double.parseDouble(values[3]);
                final double kurtosis          = Double.parseDouble(values[4]);
                //
                // The peak minus mean may be "*"
                // 50 shao     35.0  -0.509   2.221 ****** 0
                final double peakMinusMean = values[5].contains("*") ? Double.NaN : PS.toSI(Double.parseDouble(values[5]));

                final int dataQualityIndicator = Integer.parseInt(values[6]);

                final SessionStatistics stat = new SessionStatistics(systemConfigId, rms, skewness, kurtosis, peakMinusMean,
                        dataQualityIndicator);
                pi.dataBlock.addSessionStatisticsData(stat);

            }

            /** {@inheritDoc} */
            @Override
            public Iterable<LineParser> allowedNext() {
                return Arrays.asList(H8, TEN, ELEVEN, TWELVE, METEO, METEO_SUPP, ANGLES, CALIB, CALIB_DETAILS, CALIB_SHOT,
                                     STAT, COMPATIBILITY, COMMENTS, CUSTOM);
            }

        },

        /** Compatibility record. */
        COMPATIBILITY("60") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {
                // Not implemented yet
            }

            /** {@inheritDoc} */
            @Override
            public Iterable<LineParser> allowedNext() {
                return Arrays.asList(H8, TEN, ELEVEN, TWELVE, METEO, METEO_SUPP, ANGLES, CALIB, CALIB_DETAILS, CALIB_SHOT,
                                     STAT, COMPATIBILITY, COMMENTS, CUSTOM);
            }

        },

        /** Comments. */
        COMMENTS(COMMENTS_IDENTIFIER) {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {

                // Comment
                final String comment = line.substring(2).trim();
                pi.file.getComments().add(comment);

            }

            /** {@inheritDoc} */
            @Override
            public Iterable<LineParser> allowedNext() {
                return Arrays.asList(H1, H2, H3, H4, H5, H8, H9, C0, C1, C2, C3, C4, C5, C6, C7, TEN, ELEVEN, TWELVE, METEO,
                        METEO_SUPP, ANGLES, CALIB, CALIB_DETAILS, CALIB_SHOT, STAT, COMPATIBILITY, COMMENTS, CUSTOM);

            }

        },

        /** Custom. */
        CUSTOM("9\\d") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {
                // Not implemented yet
            }

            /** {@inheritDoc} */
            public Iterable<LineParser> allowedNext() {
                return Arrays.asList(H8, TEN, ELEVEN, TWELVE, METEO, METEO_SUPP, ANGLES, CALIB, CALIB_DETAILS, CALIB_SHOT,
                                     STAT, COMPATIBILITY, COMMENTS, CUSTOM);

            }

        },

        /** End of data block. */
        H8("H8", "h8") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {

                // fixed 2022-12-12
                // For the case of monthE is -1.
                // Use the date of the last range data as the end epoch.
                if (pi.header.getEndEpoch() == null) {
                    final List<RangeMeasurement> rangeData =  pi.dataBlock.getRangeData();
                    pi.header.setEndEpoch(rangeData.get(rangeData.size() - 1).getDate());
                }

                // Fill data block
                pi.dataBlock.setHeader(pi.header);
                pi.dataBlock.setConfigurationRecords(pi.configurationRecords);

                // Add the data block to the CRD file
                pi.file.addDataBlock(pi.dataBlock);

                // Initialize a new empty containers
                pi.startEpochDateComponents           = DateComponents.J2000_EPOCH;
                final CRDHeader lastHeader  = pi.header;
                pi.header               = new CRDHeader();
                pi.configurationRecords = new CRDConfiguration();
                pi.dataBlock            = new CRDDataBlock();

                // fill header with H1 H2 H3 if the file is for many targets, single system
                // configuration (see P31 in crd201)
                pi.header.setFormat(lastHeader.getFormat());
                pi.header.setVersion(lastHeader.getVersion());
                pi.header.setProductionEpoch(lastHeader.getProductionEpoch());
                pi.header.setProductionHour(lastHeader.getProductionHour());

                pi.header.setStationName(lastHeader.getStationName());
                pi.header.setSystemIdentifier(lastHeader.getSystemIdentifier());
                pi.header.setSystemNumber(lastHeader.getSystemNumber());
                pi.header.setSystemOccupancy(lastHeader.getSystemOccupancy());
                pi.header.setEpochIdentifier(lastHeader.getEpochIdentifier());
                pi.header.setStationNetword(lastHeader.getStationNetword());

                pi.header.setName(lastHeader.getName());
                pi.header.setIlrsSatelliteId(lastHeader.getIlrsSatelliteId());
                pi.header.setSic(lastHeader.getSic());
                pi.header.setNoradId(lastHeader.getNoradId());
                pi.header.setSpacecraftEpochTimeScale(lastHeader.getSpacecraftEpochTimeScale());
                pi.header.setTargetClass(lastHeader.getTargetClass());
                pi.header.setTargetLocation(lastHeader.getTargetLocation());

            }

            /** {@inheritDoc} */
            @Override
            public Iterable<LineParser> allowedNext() {
                return Arrays.asList(H1, H4, H9, COMMENTS);
            }

        },

        /** Last record in file. */
        H9("H9", "h9") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {
                pi.done = true;
            }

            /** {@inheritDoc} */
            @Override
            public Iterable<LineParser> allowedNext() {
                return Collections.singleton(H9);
            }

        };

        /** Patterns for identifying line. */
        private final Pattern[] patterns;

        /** Identifiers. */
        private final String[] identifiers;

        /** Simple constructor.
         * @param identifier regular expression for identifying line (i.e. first element)
         */
        LineParser(final String... identifier) {
            this.identifiers = identifier;
            // Initialise patterns
            this.patterns    = new Pattern[identifiers.length];
            for (int index = 0; index < patterns.length; index++) {
                patterns[index] = Pattern.compile(identifiers[index]);
            }
        }

        /** Parse a line.
         * @param line line to parse
         * @param pi holder for transient data
         */
        public abstract void parse(String line, ParseInfo pi);

        /** Get the allowed parsers for next line.
         * @return allowed parsers for next line
         */
        public abstract Iterable<LineParser> allowedNext();

        /** Check if parser can handle line.
         * @param line line to parse
         * @return true if parser can handle the specified line
         */
        public boolean canHandle(final String line) {
            // Line identifier
            final String lineId = SEPARATOR.split(line)[0];
            // Loop on patterns
            for (Pattern pattern : patterns) {
                if (pattern.matcher(lineId).matches()) {
                    return true;
                }
            }
            // No match
            return false;
        }

        /**
         * Read a boolean from a string value.
         * @param value input value
         * @return the correspondin boolean
         */
        private static boolean readBoolean(final String value) {
            return Integer.parseInt(value) == 1;
        }

        /**
         * Read an integer value taking into consideration a possible "NaN".
         * If the value is "NaN", the defaultValue is returned.
         * @param value input string
         * @param defaultValue the default value
         * @return the corresponding integer value
         */
        private static int readIntegerWithNaN(final String value, final int defaultValue) {
            return CRD.STR_NAN.equalsIgnoreCase(value) ? defaultValue : Integer.parseInt(value);
        }
    }

}
