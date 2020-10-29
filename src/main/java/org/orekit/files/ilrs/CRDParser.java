/* Copyright 2002-2020 CS GROUP
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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.hipparchus.util.FastMath;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ilrs.CRDConfiguration.DetectorConfiguration;
import org.orekit.files.ilrs.CRDConfiguration.LaserConfiguration;
import org.orekit.files.ilrs.CRDConfiguration.MeteorologicalConfiguration;
import org.orekit.files.ilrs.CRDConfiguration.SoftwareConfiguration;
import org.orekit.files.ilrs.CRDConfiguration.SystemConfiguration;
import org.orekit.files.ilrs.CRDConfiguration.TimingSystemConfiguration;
import org.orekit.files.ilrs.CRDConfiguration.TransponderConfiguration;
import org.orekit.files.ilrs.CRDFile.AnglesMeasurement;
import org.orekit.files.ilrs.CRDFile.CRDDataBlock;
import org.orekit.files.ilrs.CRDFile.MeteorologicalMeasurement;
import org.orekit.files.ilrs.CRDFile.RangeMeasurement;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScale;

/**
 * A parser for the CRD data file format.
 * <p>
 * It supports both 1.0 and 2.0 versions
 * <p>
 * <b>Note</b>: Not all the records are read by the parser. Only the most significants are parsed.
 * Contributions are welcome to support more fields in the format.
 * @see <a href="https://ilrs.gsfc.nasa.gov/docs/2009/crd_v1.01.pdf">1.0 file format</a>
 * @see <a href="https://ilrs.gsfc.nasa.gov/docs/2019/crd_v2.01.pdf">2.0 file format</a>
 * @author Bryan Cazabonne
 * @since 10.3
 */
public class CRDParser {

    /** Default supported files name pattern for CRD files. */
    public static final String DEFAULT_CRD_SUPPORTED_NAMES = "^(?!0+$)\\w{1,12}\\_\\d{6,8}.\\w{3}$";

    /** Nanometers to meters converter. */
    private static final double NM_TO_M = 1.0e-9;

    /** Kilohertz to hertz converter. */
    private static final double KHZ_TO_HZ = 1.0e3;

    /** Microseconds to seconds converter. */
    private static final double US_TO_S = 1.0e-6;

    /** Milli to none converter. */
    private static final double MILLI_TO_NONE = 1.0e-3;

    /** File format. */
    private static final String FILE_FORMAT = "CRD";

    /** Pattern for delimiting regular expressions. */
    private static final Pattern SEPARATOR = Pattern.compile("\\s+");

    /** Pattern for delimiting expressions with comma. */
    private static final Pattern COMMA = Pattern.compile(",");

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
     * Parse a CRD file from an input stream using the UTF-8 charset.
     *
     * <p> This method creates a {@link BufferedReader} from the stream and as such this
     * method may read more data than necessary from {@code stream} and the additional
     * data will be lost. The other parse methods do not have this issue.
     *
     * @param stream to read the CRD file from.
     * @return a parsed CRD file.
     * @throws IOException if {@code stream} throws one.
     * @see #parse(String)
     * @see #parse(BufferedReader, String)
     */
    public CRDFile parse(final InputStream stream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            return parse(reader, stream.toString());
        }
    }

    /**
     * Parse an CRD file from a file on the local file system.
     * @param fileName path to the CRD file.
     * @return parsed CRD file.
     * @throws IOException if one is thrown while opening or reading from {@code fileName}
     */
    public CRDFile parse(final String fileName) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(fileName),
                                                             StandardCharsets.UTF_8)) {
            return parse(reader, fileName);
        }
    }

    /**
     * Parse a CRD file from a stream.
     * @param reader   containing the CRD file.
     * @param fileName to use in error messages.
     * @return a parsed CRD file.
     * @throws IOException if {@code reader} throws one.
     */
    public CRDFile parse(final BufferedReader reader,
                         final String fileName) throws IOException {

        // Initialize internal data structures
        final ParseInfo pi = new ParseInfo();

        int lineNumber = 0;
        Stream<LineParser> cdrParsers = Stream.of(LineParser.H1);
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            ++lineNumber;
            final String l = line;
            final Optional<LineParser> selected = cdrParsers.filter(p -> p.canHandle(l)).findFirst();
            if (selected.isPresent()) {
                try {
                    selected.get().parse(line, pi);
                } catch (StringIndexOutOfBoundsException | NumberFormatException e) {
                    throw new OrekitException(e,
                                              OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                              lineNumber, fileName, line);
                }
                cdrParsers = selected.get().allowedNext();
            }
            if (pi.done) {
                // Return file
                return pi.file;
            }
        }

        // We never reached the EOF marker
        throw new OrekitException(OrekitMessages.CRD_UNEXPECTED_END_OF_FILE, lineNumber);

    }

    /** Transient data used for parsing a CRD file. The data is kept in a
     * separate data structure to make the parser thread-safe.
     * <p><b>Note</b>: The class intentionally does not provide accessor
     * methods, as it is only used internally for parsing a CRD file.</p>
     */
    private class ParseInfo {

        /** The corresponding CDR file. */
        private CRDFile file;

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

        /** Current data block start epoch. */
        private DateComponents startEpoch;

        /** End Of File reached indicator. */
        private boolean done;

        /**
         * Constructor.
         */
        protected ParseInfo() {

            // Initialise default values
            this.done       = false;
            this.version    = 1;
            this.startEpoch = DateComponents.J2000_EPOCH;

            // Initialise empty object
            this.file                 = new CRDFile();
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
            public Stream<LineParser> allowedNext() {
                return Stream.of(H2);
            }

        },

        /** Format header. */
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
                }

            }

            /** {@inheritDoc} */
            @Override
            public Stream<LineParser> allowedNext() {
                return Stream.of(H3);
            }

        },

        /** Format header. */
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
                    pi.header.setTargetLocation(Integer.parseInt(values[7]));
                }

            }

            /** {@inheritDoc} */
            @Override
            public Stream<LineParser> allowedNext() {
                return Stream.of(H4);
            }

        },

        /** Format header. */
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

                pi.startEpoch = new DateComponents(yearS, monthS, dayS);

                pi.header.setStartEpoch(new AbsoluteDate(yearS, monthS, dayS,
                                                         hourS, minuteS, secondS,
                                                         pi.timeScale));

                // End epoch
                final int    yearE   = Integer.parseInt(values[8]);
                final int    monthE  = Integer.parseInt(values[9]);
                final int    dayE    = Integer.parseInt(values[10]);
                final int    hourE   = Integer.parseInt(values[11]);
                final int    minuteE = Integer.parseInt(values[12]);
                final double secondE = Integer.parseInt(values[13]);

                pi.header.setEndEpoch(new AbsoluteDate(yearE, monthE, dayE,
                                                       hourE, minuteE, secondE,
                                                       pi.timeScale));

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
            public Stream<LineParser> allowedNext() {
                return Stream.of(H5, C0);
            }

        },

        /** Format header. */
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
            public Stream<LineParser> allowedNext() {
                return Stream.of(C0);
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
                systemRecord.setWavelength(Double.parseDouble(values[2]) * NM_TO_M);

                // System ID
                systemRecord.setSystemId(values[3]);

                // Set the system configuration record
                pi.configurationRecords.setSystemRecord(systemRecord);

            }

            /** {@inheritDoc} */
            @Override
            public Stream<LineParser> allowedNext() {
                return Stream.of(C1, C2, C3, C4, C5, C6, C7, TEN, ELEVEN, METEO, ANGLES, CALIB, STAT, COMPATIBILITY);
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
                laserRecord.setPrimaryWavelength(Double.parseDouble(values[4]) * NM_TO_M);
                laserRecord.setNominalFireRate(Double.parseDouble(values[5]));
                laserRecord.setPulseEnergy(Double.parseDouble(values[6]));
                laserRecord.setPulseWidth(Double.parseDouble(values[7]));
                laserRecord.setBeamDivergence(Double.parseDouble(values[8]));
                laserRecord.setPulseInOutgoingSemiTrain(Integer.parseInt(values[9]));

                // Set the laser configuration record
                pi.configurationRecords.setLaserRecord(laserRecord);

            }

            /** {@inheritDoc} */
            @Override
            public Stream<LineParser> allowedNext() {
                return Stream.of(C2, C3, C4, C5, C6, C7, TEN, ELEVEN, METEO, ANGLES, CALIB, STAT, COMPATIBILITY);
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
                detectorRecord.setApplicableWavelength(Double.parseDouble(values[4]) * NM_TO_M);
                detectorRecord.setQuantumEfficiency(Double.parseDouble(values[5]));
                detectorRecord.setAppliedVoltage(Double.parseDouble(values[6]));
                detectorRecord.setDarkCount(Double.parseDouble(values[7]) * KHZ_TO_HZ);
                detectorRecord.setOutputPulseType(values[8]);
                detectorRecord.setOutputPulseWidth(Double.parseDouble(values[9]));
                detectorRecord.setSpectralFilter(Double.parseDouble(values[10]) * NM_TO_M);
                detectorRecord.setTransmissionOfSpectralFilter(Double.parseDouble(values[11]));
                detectorRecord.setSpatialFilter(Double.parseDouble(values[12]));
                detectorRecord.setExternalSignalProcessing(values[13]);

                // Check file version for additional data
                if (pi.version == 2) {
                    detectorRecord.setAmplifierGain(Double.parseDouble(values[14]));
                    detectorRecord.setAmplifierBandwidth(Double.parseDouble(values[15]) * KHZ_TO_HZ);
                    detectorRecord.setAmplifierInUse(values[16]);
                }

                // Set the detector configuration record
                pi.configurationRecords.setDetectorRecord(detectorRecord);

            }

            /** {@inheritDoc} */
            @Override
            public Stream<LineParser> allowedNext() {
                return Stream.of(C3, C4, C5, C6, C7, TEN, ELEVEN, METEO, ANGLES, CALIB, STAT, COMPATIBILITY);
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
                timingRecord.setTimerSerialNumber(values[6]);
                timingRecord.setEpochDelayCorrection(Double.parseDouble(values[7]) * US_TO_S);

                // Set the timing system configuration record
                pi.configurationRecords.setTimingRecord(timingRecord);

            }

            /** {@inheritDoc} */
            @Override
            public Stream<LineParser> allowedNext() {
                return Stream.of(C4, C5, C6, C7, TEN, ELEVEN, METEO, ANGLES, CALIB, STAT, COMPATIBILITY);
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
                transponderRecord.setStationUTCOffset(Double.parseDouble(values[3]) * NM_TO_M);
                transponderRecord.setStationOscDrift(Double.parseDouble(values[4]));
                transponderRecord.setTranspUTCOffset(Double.parseDouble(values[5]) * NM_TO_M);
                transponderRecord.setTranspOscDrift(Double.parseDouble(values[6]));

                // Transponder clock reference time
                transponderRecord.setTranspClkRefTime(Double.parseDouble(values[7]));

                // Clock and drift indicators
                transponderRecord.setStationClockAndDriftApplied(Integer.parseInt(values[8]));
                transponderRecord.setSpacecraftClockAndDriftApplied(Integer.parseInt(values[9]));

                // Spacecraft time simplified
                transponderRecord.setIsSpacecraftTimeSimplified(readBoolean(values[10]));

                // Set the transponder configuration record
                pi.configurationRecords.setTransponderRecord(transponderRecord);

            }

            /** {@inheritDoc} */
            @Override
            public Stream<LineParser> allowedNext() {
                return Stream.of(C5, C6, C7, TEN, ELEVEN, METEO, ANGLES, CALIB, STAT, COMPATIBILITY);
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

                // Set the software configuration record
                pi.configurationRecords.setSoftwareRecord(softwareRecord);

            }

            /** {@inheritDoc} */
            @Override
            public Stream<LineParser> allowedNext() {
                return Stream.of(C6, C7, TEN, ELEVEN, METEO, ANGLES, CALIB, STAT, COMPATIBILITY);
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

                // Set the meteorological configuration record
                pi.configurationRecords.setMeteorologicalRecord(meteoRecord);

            }

            /** {@inheritDoc} */
            @Override
            public Stream<LineParser> allowedNext() {
                return Stream.of(C7, TEN, ELEVEN, METEO, ANGLES, CALIB, STAT, COMPATIBILITY);
            }

        },

        /** Calibration Target configuration record. */
        C7("C7", "c7") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {
                // Not implemented yet
            }

            /** {@inheritDoc} */
            @Override
            public Stream<LineParser> allowedNext() {
                return Stream.of(TEN, ELEVEN, METEO, ANGLES, CALIB, STAT, COMPATIBILITY);
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
                final double secOfDay     = Double.parseDouble(values[1]);
                final double timeOfFlight = Double.parseDouble(values[2]);
                final int    epochEvent   = Integer.parseInt(values[4]);

                // Initialise a new Range measurement
                final AbsoluteDate epoch = new AbsoluteDate(pi.startEpoch, new TimeComponents(secOfDay), pi.timeScale);
                final RangeMeasurement range = new RangeMeasurement(epoch, timeOfFlight, epochEvent);
                pi.dataBlock.addRangeData(range);

            }

            /** {@inheritDoc} */
            @Override
            public Stream<LineParser> allowedNext() {
                return Stream.of(H8, TEN, TWELVE, METEO, ANGLES, CALIB, STAT, COMPATIBILITY);
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
                final double secOfDay     = Double.parseDouble(values[1]);
                final double timeOfFlight = Double.parseDouble(values[2]);
                final int    epochEvent   = Integer.parseInt(values[4]);
                final double snr          = (pi.version == 2) ? Double.parseDouble(values[13]) : Double.NaN;

                // Initialise a new Range measurement
                final AbsoluteDate epoch = new AbsoluteDate(pi.startEpoch, new TimeComponents(secOfDay), pi.timeScale);
                final RangeMeasurement range = new RangeMeasurement(epoch, timeOfFlight, epochEvent, snr);
                pi.dataBlock.addRangeData(range);

            }

            /** {@inheritDoc} */
            @Override
            public Stream<LineParser> allowedNext() {
                return Stream.of(H8, ELEVEN, TWELVE, METEO, ANGLES, CALIB, STAT, COMPATIBILITY);
            }

        },

        /** Range Supplement Record. */
        TWELVE("12") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {
                // Not implemented yet
            }

            /** {@inheritDoc} */
            @Override
            public Stream<LineParser> allowedNext() {
                return Stream.of(H8, TEN, ELEVEN, TWELVE, METEO, ANGLES, CALIB, STAT, COMPATIBILITY);
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
                final double secOfDay    = Double.parseDouble(values[1]);
                final double pressure    = Double.parseDouble(values[2]) * MILLI_TO_NONE;
                final double temperature = Double.parseDouble(values[3]);
                final double humidity    = Double.parseDouble(values[4]);

                // Initialise a new meteorological measurement
                final AbsoluteDate epoch = new AbsoluteDate(pi.startEpoch, new TimeComponents(secOfDay), pi.timeScale);
                final MeteorologicalMeasurement meteo = new MeteorologicalMeasurement(epoch, pressure,
                                                                                      temperature, humidity);
                pi.dataBlock.addMeteoData(meteo);

            }

            /** {@inheritDoc} */
            @Override
            public Stream<LineParser> allowedNext() {
                return Stream.of(H8, METEO, METEO_SUPP, TEN, ELEVEN, TWELVE, ANGLES, CALIB, STAT, COMPATIBILITY);
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
            public Stream<LineParser> allowedNext() {
                return Stream.of(H8, METEO, METEO_SUPP, TEN, ELEVEN, TWELVE, ANGLES, CALIB, STAT, COMPATIBILITY);
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
                    azimuthRate   = readDoubleWithNaN(values[7]);
                    elevationRate = readDoubleWithNaN(values[8]);
                }

                // Initialise a new angles measurement
                final AbsoluteDate epoch = new AbsoluteDate(pi.startEpoch, new TimeComponents(secOfDay), pi.timeScale);
                final AnglesMeasurement angles = new AnglesMeasurement(epoch, azmiuth, elevation,
                                                                       directionFlag, orginFlag,
                                                                       isRefractionCorrected,
                                                                       azimuthRate, elevationRate);
                pi.dataBlock.addAnglesData(angles);

            }

            /** {@inheritDoc} */
            @Override
            public Stream<LineParser> allowedNext() {
                return Stream.of(H8, METEO, TEN, ELEVEN, ANGLES, CALIB, STAT, COMPATIBILITY);
            }

        },

        /** Calibration Record. */
        CALIB("40") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {
                // Not implemented yet
            }

            /** {@inheritDoc} */
            @Override
            public Stream<LineParser> allowedNext() {
                return Stream.of(H8, METEO, CALIB, CALIB_DETAILS, CALIB_SHOT, TEN, ELEVEN, TWELVE, ANGLES, STAT, COMPATIBILITY);
            }

        },

        /** Calibration Details Record. */
        CALIB_DETAILS("41") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {
                // Not implemented yet
            }

            /** {@inheritDoc} */
            @Override
            public Stream<LineParser> allowedNext() {
                return Stream.of(H8, METEO, CALIB, CALIB_DETAILS, CALIB_SHOT, TEN, ELEVEN, TWELVE, ANGLES, STAT, COMPATIBILITY);
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
            public Stream<LineParser> allowedNext() {
                return Stream.of(H8, METEO, CALIB, CALIB_DETAILS, CALIB_SHOT, TEN, ELEVEN, TWELVE, ANGLES, STAT, COMPATIBILITY);
            }

        },

        /** Session (Pass) Statistics Record. */
        STAT("50") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {
                // Not implemented yet
            }

            /** {@inheritDoc} */
            @Override
            public Stream<LineParser> allowedNext() {
                return Stream.of(H8, METEO, CALIB, CALIB_DETAILS, CALIB_SHOT, TEN, ELEVEN, TWELVE, ANGLES, STAT, COMPATIBILITY, H8);
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
            public Stream<LineParser> allowedNext() {
                return Stream.of(H8, METEO, CALIB, CALIB_DETAILS, CALIB_SHOT, TEN, ELEVEN, TWELVE, ANGLES, STAT, COMPATIBILITY);
            }

        },

        /** Comments. */
        COMMENTS("00") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {

                // Comment
                final String comment = line.split(getFirstIdentifier())[1].trim();
                pi.file.getComments().add(comment);

            }

            /** {@inheritDoc} */
            @Override
            public Stream<LineParser> allowedNext() {
                return Stream.of(H1, H2, H3, H4, H5, H8, C0, C1, C2, C3, C4, C5, C6, C7);
            }

        },

        /** End of data block. */
        H8("H8", "h8") {

            /** {@inheritDoc} */
            @Override
            public void parse(final String line, final ParseInfo pi) {

                // Fill data block
                pi.dataBlock.setHeader(pi.header);
                pi.dataBlock.setConfigurationRecords(pi.configurationRecords);

                // Add the data block to the CRD file
                pi.file.addDataBlock(pi.dataBlock);

                // Initialize a new empty containers
                pi.startEpoch           = DateComponents.J2000_EPOCH;
                pi.header               = new CRDHeader();
                pi.configurationRecords = new CRDConfiguration();
                pi.dataBlock            = new CRDDataBlock();

            }

            /** {@inheritDoc} */
            @Override
            public Stream<LineParser> allowedNext() {
                return Stream.of(H1, H9);
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
            public Stream<LineParser> allowedNext() {
                return Stream.of(H9);
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

        /**
         * Get the regular expression for identifying line.
         * @return the regular expression for identifying line
         */
        public String getFirstIdentifier() {
            return identifiers[0];
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
         * Read a double value taking into consideration a possible "na".
         * @param value input string
         * @return the corresponding double value
         */
        private static double readDoubleWithNaN(final String value) {
            return "na".equals(value) ? Double.NaN : Double.parseDouble(value);
        }

    }

}
