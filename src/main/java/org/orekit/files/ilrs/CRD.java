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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.hipparchus.util.FastMath;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChronologicalComparator;
import org.orekit.time.TimeScalesFactory;
import org.orekit.time.TimeStamped;
import org.orekit.utils.ImmutableTimeStampedCache;

/**
 * This class stores all the information of the Consolidated laser ranging Data Format (CRD) parsed
 * by CRDParser. It contains the header and a list of data records.
 * @author Bryan Cazabonne
 * @author Rongwang Li
 * @since 10.3
 */
public class CRD {

    /** Value of 'not available' or 'not applicable' or 'no information'. */
    public static final String STR_VALUE_NOT_AVAILABLE = "na";

    /** String of "NaN". */
    public static final String STR_NAN = "NaN";

    /** Pattern of "NaN". */
    public static final Pattern PATTERN_NAN = Pattern.compile(STR_NAN);

    /** List of comments contained in the file. */
    private List<String> comments;

    /** List of data blocks contain in the CDR file. */
    private List<CRDDataBlock> dataBlocks;

    /**
     * Constructor.
     */
    public CRD() {
        // Initialise empty lists
        this.comments   = new ArrayList<>();
        this.dataBlocks = new ArrayList<>();
    }

    /**
     * Format the integer value as a string, or the string <code>VALUE_NOT_AVAILABLE</code>.
     * @param value the value
     * @param valueNotAvailable the value means not available
     * @return a string
     * @since 12.0
     */
    public static String formatIntegerOrNaN(final int value, final int valueNotAvailable) {
        return value == valueNotAvailable ? STR_VALUE_NOT_AVAILABLE : String.format("%d", value);
    }

    /**
     * Replace all " NaN" with " na".
     * @param crdString the original string
     * @return the string
     * @since 12.0
     */
    public static String handleNaN(final String crdString) {
        return PATTERN_NAN.matcher(crdString).replaceAll(STR_VALUE_NOT_AVAILABLE);
    }

    /**
     * Add a data block to the current list of data blocks.
     * @param dataBlock data block to add
     */
    public void addDataBlock(final CRDDataBlock dataBlock) {
        dataBlocks.add(dataBlock);
    }

    /**
     * Get the comments contained in the file.
     * @return the comments contained in the file
     */
    public List<String> getComments() {
        return comments;
    }

    /**
     * Get the data blocks contain in the file.
     * @return the data blocks contain in the file
     */
    public List<CRDDataBlock> getDataBlocks() {
        return Collections.unmodifiableList(dataBlocks);
    }

    /**
     * Data block containing a set of data contain in the CRD file.
     * <p>
     * A data block consists of a header, configuration data and
     * recorded data (range, angles, meteorological, etc.).
     * </p>
     */
    public static class CRDDataBlock {

        /** Data block header. */
        private CRDHeader header;

        /** Configuration record. */
        private CRDConfiguration configurationRecords;

        /** Range records. */
        private List<RangeMeasurement> rangeData;

        /** Meteorological records. */
        private final SortedSet<MeteorologicalMeasurement> meteoData;

        /** Pointing angles records. */
        private List<AnglesMeasurement> anglesData;

        /** RangeSupplement records. */
        private List<RangeSupplement> rangeSupplementData;

        /** Session statistics record(s). */
        private List<SessionStatistics> sessionStatisticsData;

        /** Calibration Record(s). */
        private List<Calibration> calibrationData;

        /** Calibration detail record(s). */
        private List<CalibrationDetail> calibrationDetailData;

        /**
         * Constructor.
         */
        public CRDDataBlock() {
            // Initialise empty lists
            this.rangeData  = new ArrayList<>();
            this.meteoData  = new TreeSet<>(new ChronologicalComparator());
            this.anglesData = new ArrayList<>();
            this.rangeSupplementData = new ArrayList<>();
            this.sessionStatisticsData = new ArrayList<>();
            this.calibrationData = new ArrayList<>();
            this.calibrationDetailData = new ArrayList<>();
        }

        /**
         * Get the header of the current data block.
         * @return the header of the current data block
         */
        public CRDHeader getHeader() {
            return header;
        }

        /**
         * Set the header for the current data block.
         * @param header the header to set
         */
        public void setHeader(final CRDHeader header) {
            this.header = header;
        }

        /**
         * Get the system configuration records.
         * @return the system configuration records
         */
        public CRDConfiguration getConfigurationRecords() {
            return configurationRecords;
        }

        /**
         * Set the configuration records for the current data block.
         * @param configurationRecords the configuration records to set
         */
        public void setConfigurationRecords(final CRDConfiguration configurationRecords) {
            this.configurationRecords = configurationRecords;
        }

        /**
         * Add an entry to the list of range data.
         * @param range entry to add
         */
        public void addRangeData(final RangeMeasurement range) {
            rangeData.add(range);
        }

        /**
         * Add an entry to the list of meteorological data.
         * @param meteorologicalMeasurement entry to add
         */
        public void addMeteoData(final MeteorologicalMeasurement meteorologicalMeasurement) {
            meteoData.add(meteorologicalMeasurement);
        }

        /**
         * Add an entry to the list of angles data.
         * @param angles entry to add
         */
        public void addAnglesData(final AnglesMeasurement angles) {
            anglesData.add(angles);
        }

        /**
         * Get the range data for the data block.
         * @return an unmodifiable list of range data
         */
        public List<RangeMeasurement> getRangeData() {
            return Collections.unmodifiableList(rangeData);
        }

        /**
         * Get the angles data for the data block.
         * @return an unmodifiable list of angles data
         */
        public List<AnglesMeasurement> getAnglesData() {
            return Collections.unmodifiableList(anglesData);
        }

        /**
         * Get the meteorological data for the data block.
         * @return an unmodifiable list of meteorological data
         */
        public Meteo getMeteoData() {
            return new Meteo(meteoData);
        }

        /**
         * Add an entry to the list of range supplement data.
         * @param rangeSupplement entry to add
         * @since 12.0
         */
        public void addRangeSupplementData(final RangeSupplement rangeSupplement) {
            rangeSupplementData.add(rangeSupplement);
        }

        /**
         * Get the range supplement data for the data block.
         * @return an unmodifiable list of range supplement data
         * @since 12.0
         */
        public List<RangeSupplement> getRangeSupplementData() {
            return Collections.unmodifiableList(rangeSupplementData);
        }

        /**
         * Add an entry to the list of session statistics data.
         * @param sessionStatistics entry to add
         * @since 12.0
         */
        public void addSessionStatisticsData(final SessionStatistics sessionStatistics) {
            sessionStatisticsData.add(sessionStatistics);
        }

        /**
         * Get the session statistics data for the data block.
         * @return an unmodifiable list of session statistics data
         * @since 12.0
         */
        public List<SessionStatistics> getSessionStatisticsData() {
            return Collections.unmodifiableList(sessionStatisticsData);
        }

        /**
         * Get the default (the first if there are many records) SessionStat record.
         * @return the default (the first if there are many records) session statistics record
         * @since 12.0
         */
        public SessionStatistics getSessionStatisticsRecord() {
            return getSessionStatisticsRecord(null);
        }

        /**
         * Get the session statistics record related to the systemConfigurationId.
         * @param systemConfigurationId system configuration ID
         * @return the session statistics record
         * @since 12.0
         */
        public SessionStatistics getSessionStatisticsRecord(final String systemConfigurationId) {
            if (sessionStatisticsData.isEmpty()) {
                return null;
            }

            if (systemConfigurationId == null) {
                // default (the first one)
                return sessionStatisticsData.get(0);
            }

            // Loop to find the appropriate one
            for (SessionStatistics sessionStatistics : sessionStatisticsData) {
                if (systemConfigurationId.equalsIgnoreCase(sessionStatistics.getSystemConfigurationId())) {
                    return sessionStatistics;
                }
            }

            return null;
        }

        /**
         * Add an entry to the list of calibration data.
         * @param cal entry to add
         * @since 12.0
         */
        public void addCalibrationData(final Calibration cal) {
            calibrationData.add(cal);
        }

        /**
         * Get the calibration data for the data block.
         * @return an unmodifiable list of calibration data
         * @since 12.0
         */
        public List<Calibration> getCalibrationData() {
            return Collections.unmodifiableList(calibrationData);
        }

        /**
         * Get the Calibration record(s) related to the default system configuration id.
         * @return the Calibration record(s) related to the default system configuration id
         * @since 12.0
         */
        public List<Calibration> getCalibrationRecords() {
            return getCalibrationRecords(null);
        }

        /**
         * Get the Calibration record(s) related to the given systemConfigurationId.
         * @param systemConfigurationId system configuration ID
         * @return the Calibration record(s)
         * @since 12.0
         */
        public List<Calibration> getCalibrationRecords(final String systemConfigurationId) {
            if (calibrationData.isEmpty()) {
                return null;
            }

            final String systemConfigId = systemConfigurationId == null ? getConfigurationRecords().getSystemRecord().getConfigurationId() : systemConfigurationId;

            final List<Calibration> list = new ArrayList<Calibration>();
            // Loop to find the appropriate one
            for (Calibration calibration : calibrationData) {
                if (systemConfigId.equalsIgnoreCase(calibration.getSystemConfigurationId())) {
                    list.add(calibration);
                }
            }

            return list;
        }

        /**
         * Add an entry to the list of calibration detail data.
         * @param cal entry to add
         * @since 12.0
         */
        public void addCalibrationDetailData(final CalibrationDetail cal) {
            calibrationDetailData.add(cal);
        }

        /**
         * Get the calibration detail data for the data block.
         * @return an unmodifiable list of calibration detail data
         * @since 12.0
         */
        public List<CalibrationDetail> getCalibrationDetailData() {
            return Collections.unmodifiableList(calibrationDetailData);
        }

        /**
         * Get the CalibrationDetail record(s) related to the default system configuration id.
         * @return the CalibrationDetail record(s) related to the default system configuration id
         * @since 12.0
         */
        public List<CalibrationDetail> getCalibrationDetailRecords() {
            return getCalibrationDetailRecords(null);
        }

        /**
         * Get the CalibrationDetail record(s) related to the given systemConfigurationId.
         * @param systemConfigurationId system configuration ID
         * @return the CalibrationDetail record(s)
         * @since 12.0
         */
        public List<CalibrationDetail> getCalibrationDetailRecords(final String systemConfigurationId) {
            if (calibrationDetailData.isEmpty()) {
                return null;
            }

            final String systemConfigId = systemConfigurationId == null ? getConfigurationRecords().getSystemRecord().getConfigurationId() : systemConfigurationId;

            final List<CalibrationDetail> list = new ArrayList<CalibrationDetail>();
            // Loop to find the appropriate one
            for (CalibrationDetail calibration : calibrationDetailData) {
                if (systemConfigId.equalsIgnoreCase(calibration.getSystemConfigurationId())) {
                    list.add(calibration);
                }
            }

            return list;
        }

        /**
         * Get the wavelength related to the given RangeMeasurement.
         *
         * @param range a RangeMeasurement
         * @return the wavelength related to the given RangeMeasurement.
         * @since 12.0
         */
        public double getWavelength(final RangeMeasurement range) {
            return getConfigurationRecords().getSystemRecord(range.getSystemConfigurationId()).getWavelength();
        }

    }

    /** Range record. */
    public static class RangeMeasurement implements TimeStamped {

        /** Data epoch. */
        private AbsoluteDate date;

        /** Time of flight [s]. */
        private final double timeOfFlight;

        /** System configuration ID. */
        private final String systemConfigurationId;

        /** Time event reference indicator.
         * 0 = ground receive time (at SRP) (two-way)
         * 1 = spacecraft bounce time (two-way)
         * 2 = ground transmit time (at SRP) (two-way)
         * 3 = spacecraft receive time (one-way)
         * 4 = spacecraft transmit time (one-way)
         * 5 = ground transmit time (at SRP) and spacecraft receive time (one-way)
         * 6 = spacecraft transmit time and ground receive time (at SRP) (one-way)
         * Currently, only 1 and 2 are used for laser ranging data.
         */
        private final int epochEvent;

        /** Signal to noise ration. */
        private final double snr;

        /**
         * Constructor.
         * @param date data epoch
         * @param timeOfFlight time of flight in seconds
         * @param epochEvent indicates the time event reference
         */
        public RangeMeasurement(final AbsoluteDate date,
                                final double timeOfFlight,
                                final int epochEvent) {
            this(date, timeOfFlight, epochEvent, Double.NaN);
        }

        /**
         * Constructor.
         * @param date data epoch
         * @param timeOfFlight time of flight in seconds
         * @param epochEvent indicates the time event reference
         * @param snr signal to noise ratio (can be Double.NaN if unkonwn)
         */
        public RangeMeasurement(final AbsoluteDate date,
                                final double timeOfFlight,
                                final int epochEvent, final double snr) {
            this(date, timeOfFlight, epochEvent, snr, null);
        }

        /**
         * Constructor.
         * @param date data epoch
         * @param timeOfFlight time of flight in seconds
         * @param epochEvent indicates the time event reference
         * @param snr signal to noise ratio (can be Double.NaN if unkonwn)
         * @param systemConfigurationId system configuration id
         * @since 12.0
         */
        public RangeMeasurement(final AbsoluteDate date,
                                final double timeOfFlight, final int epochEvent,
                                final double snr,
                                final String systemConfigurationId) {
            this.date                  = date;
            this.timeOfFlight          = timeOfFlight;
            this.epochEvent            = epochEvent;
            this.snr                   = snr;
            this.systemConfigurationId = systemConfigurationId;
        }

        /**
         * Get the time-of-flight.
         * @return the time-of-flight in seconds
         */
        public double getTimeOfFlight() {
            return timeOfFlight;
        }

        /**
         * Get the indicator for the time event reference.
         * <ul>
         * <li>0 = ground receive time (at SRP) (two-way)</li>
         * <li>1 = spacecraft bounce time (two-way)</li>
         * <li>2 = ground transmit time (at SRP) (two-way)</li>
         * <li>3 = spacecraft receive time (one-way)</li>
         * <li>4 = spacecraft transmit time (one-way)</li>
         * <li>5 = ground transmit time (at SRP) and spacecraft receive time (one-way)</li>
         * <li>6 = spacecraft transmit time and ground receive time (at SRP) (one-way)</li>
         * </ul>
         * Currently, only 1 and 2 are used for laser ranging data
         * @return the indicator for the time event reference
         */
        public int getEpochEvent() {
            return epochEvent;
        }

        /**
         * Get the signal to noise ratio.
         * @return the signal to noise ratio
         */
        public double getSnr() {
            return snr;
        }

        /** {@inheritDoc} */
        @Override
        public AbsoluteDate getDate() {
            return date;
        }

        /**
         * Get the system configuration id.
         * @return the system configuration id
         * @since 12.0
         */
        public String getSystemConfigurationId() {
            return systemConfigurationId;
        }

        /**
         * Get a string representation of the instance in the CRD format.
         * @return a string representation of the instance, in the CRD format.
         * @since 12.0
         */
        public String toCrdString() {
            return "00 not supported. use NptRangeMeasurement or FrRangeMeasurement instead.";
        }
    }

    /**
     * Range record -- Full rate, Sampled Engineering/Quicklook.
     * @since 12.0
     */
    public static class FrRangeMeasurement extends RangeMeasurement {

        /** Filter flag. **/
        private final int filterFlag;

        /** Detector channel. **/
        private final int detectorChannel;

        /** Stop number (in multiple-stop system). **/
        private final int stopNumber;

        /** Receive amplitude - a positive linear scale value. **/
        private final int receiveAmplitude;

        /** Transmit amplitude - a positive linear scale value. **/
        private final int transmitAmplitude;

        /**
         * Constructor.
         * @param date data epoch
         * @param timeOfFlight time of flight in seconds
         * @param epochEvent indicates the time event reference
         * @param systemConfigurationId system configuration id
         * @param filterFlag filter flag
         * @param detectorChannel detector channel
         * @param stopNumber stop number
         * @param receiveAmplitude receive amplitude
         * @param transmitAmplitude transmit amplitude
         */
        public FrRangeMeasurement(final AbsoluteDate date,
                                  final double timeOfFlight,
                                  final int epochEvent,
                                  final String systemConfigurationId,
                                  final int filterFlag,
                                  final int detectorChannel,
                                  final int stopNumber,
                                  final int receiveAmplitude,
                                  final int transmitAmplitude) {
            super(date, timeOfFlight, epochEvent, Double.NaN, systemConfigurationId);
            this.filterFlag        = filterFlag;
            this.detectorChannel   = detectorChannel;
            this.stopNumber        = stopNumber;
            this.receiveAmplitude  = receiveAmplitude;
            this.transmitAmplitude = transmitAmplitude;
        }

        /**
         * Get the filter flag.
         * @return the filter flag
         */
        public int getFilterFlag() {
            return filterFlag;
        }

        /**
         * Get the detector channel.
         * @return the detector channel
         */
        public int getDetectorChannel() {
            return detectorChannel;
        }

        /**
         * Get the stop number.
         * @return the stop number
         */
        public int getStopNumber() {
            return stopNumber;
        }

        /**
         * Get the receive amplitude.
         * @return the receive amplitude, -1 if not measured
         */
        public int getReceiveAmplitude() {
            return receiveAmplitude;
        }

        /**
         * Get the transmit amplitude.
         * @return the transmit amplitude, -1 if not measured
         */
        public int getTransmitAmplitude() {
            return transmitAmplitude;
        }

        /** {@inheritDoc} */
        @Override
        @DefaultDataContext
        public String toCrdString() {
            return String.format("10 %s", toString());
        }

        @Override
        @DefaultDataContext
        public String toString() {
            // CRD suggested format, excluding the record type
            // 'local' is already utc.
            // Seconds of day (sod) is typically to 1 milllisec precision.
            // receiveAmplitude, transmitAmplitude: -1 if not available
            final double sod = getDate()
                    .getComponents(TimeScalesFactory.getUTC()).getTime()
                    .getSecondsInLocalDay();

            final String str = String.format(
                    "%18.12f %18.12f %4s %1d %1d %1d %1d %5s %5s", sod,
                    getTimeOfFlight(), getSystemConfigurationId(),
                    getEpochEvent(), filterFlag, detectorChannel, stopNumber,
                    formatIntegerOrNaN(receiveAmplitude, -1),
                    formatIntegerOrNaN(transmitAmplitude, -1));
            return handleNaN(str).replace(',', '.');
        }

    }

    /**
     * Range record -- Normal Point.
     * @since 12.0
     */
    public static class NptRangeMeasurement extends RangeMeasurement {

        /** Normal point window length [s]. */
        private final double windowLength;

        /** Number of raw ranges (after editing) compressed into the normal point. */
        private final int numberOfRawRanges;

        /** Bin RMS from the mean of raw accepted time-of-flight values minus the trend function. */
        private final double binRms;

        /** Bin skew from the mean of raw accepted time-of-flight values minus the trend function. */
        private final double binSkew;

        /** Bin kurtosis from the mean of raw accepted time-of-flight values minus the trend function. */
        private final double binKurtosis;

        /** Bin peak - mean value. */
        private final double binPeakMinusMean;

        /** Return rate [%]. */
        private final double returnRate;

        /** Detector channel. */
        private final int detectorChannel;

        /**
         * Constructor.
         * @param date data epoch
         * @param timeOfFlight time of flight in seconds
         * @param epochEvent indicates the time event reference
         * @param snr signal to noise ratio (can be Double.NaN if unkonwn)
         * @param systemConfigurationId System configuration id
         */
        public NptRangeMeasurement(final AbsoluteDate date,
                                   final double timeOfFlight,
                                   final int epochEvent, final double snr,
                                   final String systemConfigurationId) {
            this(date, timeOfFlight, epochEvent, snr, systemConfigurationId, -1,
                    -1, Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                    Double.NaN, 0);
        }

        /**
         * Constructor.
         * @param date data epoch
         * @param timeOfFlight time of flight in seconds
         * @param epochEvent indicates the time event reference
         * @param snr signal to noise ratio (can be Double.NaN if unkonwn)
         * @param systemConfigurationId System configuration id
         * @param windowLength normal point window length
         * @param numberOfRawRanges number of raw ranges (after editing) compressed into the normal point
         * @param binRms Bin RMS from the mean of raw accepted time-of-flight values minus the trend function
         * @param binSkew Bin skew from the mean of raw accepted time-of-flight values minus the trend function
         * @param binKurtosis Bin kurtosis from the mean of raw accepted time-of-flight values minus the trend function
         * @param binPeakMinusMean Bin peak - mean value
         * @param returnRate Return rate [%]
         * @param detectorChannel detector channel
         */
        public NptRangeMeasurement(final AbsoluteDate date,
                                   final double timeOfFlight,
                                   final int epochEvent, final double snr,
                                   final String systemConfigurationId,
                                   final double windowLength,
                                   final int numberOfRawRanges,
                                   final double binRms, final double binSkew,
                                   final double binKurtosis,
                                   final double binPeakMinusMean,
                                   final double returnRate,
                                   final int detectorChannel) {
            super(date, timeOfFlight, epochEvent, snr, systemConfigurationId);

            this.windowLength      = windowLength;
            this.numberOfRawRanges = numberOfRawRanges;
            this.binSkew           = binSkew;
            this.binKurtosis       = binKurtosis;
            this.binPeakMinusMean  = binPeakMinusMean;
            this.detectorChannel   = detectorChannel;
            this.binRms            = binRms == -1.0e-12 ? Double.NaN : binRms; // -1=na, ps --> s
            this.returnRate        = returnRate == -1 ? Double.NaN : returnRate; // -1=na
        }

        /**
         * Get the normal point window length.
         * @return the normal point window length
         */
        public double getWindowLength() {
            return windowLength;
        }

        /**
         * Get the umber of raw ranges (after editing) compressed into the normal point.
         * @return the umber of raw ranges
         */
        public int getNumberOfRawRanges() {
            return numberOfRawRanges;
        }

        /**
         * Get the bin RMS from the mean of raw accepted time-of-flight values minus the trend function.
         * @return the bin RMS
         */
        public double getBinRms() {
            return binRms;
        }

        /**
         * Get the bin skew from the mean of raw accepted time-of-flight values minus the trend function.
         * @return the bin skew
         */
        public double getBinSkew() {
            return binSkew;
        }

        /**
         * Get the bin kurtosis from the mean of raw accepted time-of-flight values minus the trend function.
         * @return the bin kurtosis
         */
        public double getBinKurtosis() {
            return binKurtosis;
        }

        /**
         * Get the bin peak - mean value.
         * @return the bin peak - mean value
         */
        public double getBinPeakMinusMean() {
            return binPeakMinusMean;
        }

        /**
         * Get the return rate.
         * @return the return rate
         */
        public double getReturnRate() {
            return returnRate;
        }

        /**
         * Get the detector channel.
         * @return the detector channel
         */
        public int getDetectorChannel() {
            return detectorChannel;
        }

        /** {@inheritDoc} */
        @Override
        @DefaultDataContext
        public String toCrdString() {
            return String.format("11 %s", toString());
        }

        @Override
        @DefaultDataContext
        public String toString() {
            // CRD suggested format, excluding the record type
            // binRms, binPeakMinusMean: s --> ps
            // 'local' is already utc.
            // Seconds of day (sod) is typically to 1 milllisec precision.
            final double sod = getDate()
                    .getComponents(TimeScalesFactory.getUTC()).getTime()
                    .getSecondsInLocalDay();

            final String str = String.format(
                    "%18.12f %18.12f %4s %1d %6.1f %6d %9.1f %7.3f %7.3f %9.1f %5.2f %1d %5.1f",
                    sod, getTimeOfFlight(), getSystemConfigurationId(),
                    getEpochEvent(), windowLength, numberOfRawRanges,
                    binRms * 1e12, binSkew, binKurtosis,
                    binPeakMinusMean * 1e12, returnRate, detectorChannel,
                    getSnr());
            return handleNaN(str).replace(',', '.');
        }

    }

    /**
     * Range Supplement Record.
     * @since 12.0
     */
    public static class RangeSupplement implements TimeStamped {

        /** Data epoch. */
        private AbsoluteDate date;

        /** System configuration ID. */
        private final String systemConfigurationId;

        /** Tropospheric refraction correction (one-way). */
        private final double troposphericRefractionCorrection;

        /** Target center of mass correction (one-way). */
        private final double centerOfMassCorrection;

        /** Neutral density (ND) filter value. */
        private final double ndFilterValue;

        /** Time bias applied. */
        private final double timeBiasApplied;

        /** Range rate. */
        private final double rangeRate;

        /**
         * Constructor.
         * @param date data epoch
         * @param systemConfigurationId system configuration ID
         * @param troposphericRefractionCorrection tropospheric refraction correction (one-way)
         * @param centerOfMassCorrection target center of mass correction (one-way)
         * @param ndFilterValue Neutral density (ND) filter value
         * @param timeBiasApplied Time bias applied
         * @param rangeRate Range rate
         */
        public RangeSupplement(final AbsoluteDate date,
                               final String systemConfigurationId,
                               final double troposphericRefractionCorrection,
                               final double centerOfMassCorrection,
                               final double ndFilterValue,
                               final double timeBiasApplied,
                               final double rangeRate) {
            this.date                             = date;
            this.systemConfigurationId            = systemConfigurationId;
            this.troposphericRefractionCorrection = troposphericRefractionCorrection;
            this.centerOfMassCorrection           = centerOfMassCorrection;
            this.ndFilterValue                    = ndFilterValue;
            this.timeBiasApplied                  = timeBiasApplied;
            this.rangeRate                        = rangeRate;
        }

        @Override
        public AbsoluteDate getDate() {
            return date;
        }

        /**
         * Get the system configuration id.
         * @return the system configuration id
         */
        public String getSystemConfigurationId() {
            return systemConfigurationId;
        }

        /**
         * Get the tropospheric refraction correction.
         * @return the tropospheric refraction correction
         */
        public double getTroposphericRefractionCorrection() {
            return troposphericRefractionCorrection;
        }

        /**
         * Get the target center of mass.
         * @return the target center of mass
         */
        public double getCenterOfMassCorrection() {
            return centerOfMassCorrection;
        }

        /**
         * Get the neutral density (ND) filter value.
         * @return the neutral density (ND) filter value
         */
        public double getNdFilterValue() {
            return ndFilterValue;
        }

        /**
         * Get the time bias applied.
         * @return the time bias applied
         */
        public double getTimeBiasApplied() {
            return timeBiasApplied;
        }

        /**
         * Get the range rate.
         * @return the range rate
         */
        public double getRangeRate() {
            return rangeRate;
        }

        /**
         * Get a string representation of the instance in the CRD format.
         * @return a string representation of the instance, in the CRD format.
         */
        @DefaultDataContext
        public String toCrdString() {
            return String.format("12 %s", toString());
        }

        @Override
        @DefaultDataContext
        public String toString() {
            // CRD suggested format, excluding the record type
            // troposphericRefractionCorrection: s --> ps
            // 'local' is already utc.
            // Seconds of day (sod) is typically to 1 milllisec precision.
            final double sod = getDate()
                    .getComponents(TimeScalesFactory.getUTC()).getTime()
                    .getSecondsInLocalDay();

            final String str = String.format(
                    "%18.12f %4s %6.1f %6.4f %5.2f %8.4f %f", sod,
                    getSystemConfigurationId(),
                    troposphericRefractionCorrection * 1e12,
                    centerOfMassCorrection, ndFilterValue, timeBiasApplied,
                    rangeRate);
            return handleNaN(str).replace(',', '.');
        }

    }

    /** This data record contains a minimal set of meteorological data. */
    public static class MeteorologicalMeasurement implements TimeStamped {

        /** Data epoch. */
        private AbsoluteDate date;

        /** Surface pressure [bar]. */
        private final double pressure;

        /** Surface temperature [K]. */
        private final double temperature;

        /** Relative humidity at the surface [%]. */
        private final double humidity;

        /** Origin of values.
         * 0=measured values, 1=interpolated values
         */
        private final int originOfValues;

        /**
         * Constructor.
         * @param date data epoch
         * @param pressure the surface pressure in bars
         * @param temperature the surface temperature in degrees Kelvin
         * @param humidity the relative humidity at the surface in percents
         */
        public MeteorologicalMeasurement(final AbsoluteDate date,
                                         final double pressure, final double temperature,
                                         final double humidity) {
            this(date, pressure, temperature, humidity, 0);
        }

        /**
         * Constructor.
         * @param date data epoch
         * @param pressure the surface pressure in bars
         * @param temperature the surface temperature in degrees Kelvin
         * @param humidity the relative humidity at the surface in percents
         * @param originOfValues Origin of values
         */
        public MeteorologicalMeasurement(final AbsoluteDate date, final double pressure, final double temperature,
                                         final double humidity, final int originOfValues) {
            this.date           = date;
            this.pressure       = pressure;
            this.temperature    = temperature;
            this.humidity       = humidity;
            this.originOfValues = originOfValues;
        }

        /**
         * Get the surface pressure.
         * @return the surface pressure in bars
         */
        public double getPressure() {
            return pressure;
        }

        /**
         * Get the surface temperature.
         * @return the surface temperature in degrees Kelvin
         */
        public double getTemperature() {
            return temperature;
        }

        /**
         * Get the relative humidity at the surface.
         * @return the relative humidity at the surface in percents
         */
        public double getHumidity() {
            return humidity;
        }

        /** {@inheritDoc} */
        @Override
        public AbsoluteDate getDate() {
            return date;
        }

        /** Get the origin of values.
         * 0=measure values
         * 1=interpolated values
         * @return the origin of values
         * @since 12.0
         */
        public int getOriginOfValues() {
            return originOfValues;
        }

        /**
         * Get a string representation of the instance in the CRD format.
         * @return a string representation of the instance, in the CRD format.
         * @since 12.0
         */
        @DefaultDataContext
        public String toCrdString() {
            return String.format("20 %s", toString());
        }

        @Override
        @DefaultDataContext
        public String toString() {
            // CRD suggested format, excluding the record type
            // pressure: bar --> mbar
            // 'local' is already utc.
            // Seconds of day (sod) is typically to 1 milllisec precision.
            final double sod = getDate()
                    .getComponents(TimeScalesFactory.getUTC()).getTime()
                    .getSecondsInLocalDay();

            final String str = String.format("%9.3f %7.2f %6.2f %4.0f %1d", sod,
                    pressure * 1e3, temperature, humidity, originOfValues);
            return handleNaN(str).replace(',', '.');
        }
    }

    /** Pointing angles record. */
    public static class AnglesMeasurement implements TimeStamped {

        /** Data epoch. */
        private AbsoluteDate date;

        /** Azimuth [rad]. */
        private final double azimuth;

        /** Elevation [rad]. */
        private final double elevation;

        /** Direction flag (0 = transmit &#38; receive ; 1 = transmit ; 2 = receive). */
        private final int directionFlag;

        /** Angle origin indicator.
         * 0 = unknown
         * 1 = computed
         * 2 = commanded (from predictions)
         * 3 = measured (from encoders)
         */
        private final int originIndicator;

        /** Refraction corrected. */
        private final boolean refractionCorrected;

        /** Azimuth rate [rad/sec]. */
        private final double azimuthRate;

        /** Elevation rate [rad/sec]. */
        private final double elevationRate;

        /**
         * Constructor.
         * @param date data epoch
         * @param azimuth azimuth angle in radians
         * @param elevation elevation angle in radians
         * @param directionFlag direction flag
         * @param originIndicator angle origin indicator
         * @param refractionCorrected flag to indicate if the refraction is corrected
         * @param azimuthRate azimuth rate in radians per second (equal to Double.NaN if unknown)
         * @param elevationRate elevation rate in radians per second (equal to Double.NaN if unknown)
         */
        public AnglesMeasurement(final AbsoluteDate date, final double azimuth,
                                 final double elevation, final int directionFlag,
                                 final int originIndicator,
                                 final boolean refractionCorrected,
                                 final double azimuthRate, final double elevationRate) {
            this.date                = date;
            this.azimuth             = azimuth;
            this.elevation           = elevation;
            this.directionFlag       = directionFlag;
            this.originIndicator     = originIndicator;
            this.refractionCorrected = refractionCorrected;
            this.azimuthRate         = azimuthRate;
            this.elevationRate       = elevationRate;
        }

        /**
         * Get the azimuth angle.
         * @return the azimuth angle in radians
         */
        public double getAzimuth() {
            return azimuth;
        }

        /**
         * Get the elevation angle.
         * @return the elevation angle in radians
         */
        public double getElevation() {
            return elevation;
        }

        /**
         * Get the direction flag (0 = transmit &#38; receive ; 1 = transmit ; 2 = receive).
         * @return the direction flag
         */
        public int getDirectionFlag() {
            return directionFlag;
        }

        /**
         * Get the angle origin indicator.
         * <p>
         * 0 = unknown;
         * 1 = computed;
         * 2 = commanded (from predictions);
         * 3 = measured (from encoders)
         * </p>
         * @return the angle origin indicator
         */
        public int getOriginIndicator() {
            return originIndicator;
        }

        /**
         * Get the flag indicating if the refraction is corrected.
         * @return true if refraction is corrected
         */
        public boolean isRefractionCorrected() {
            return refractionCorrected;
        }

        /**
         * Get the azimuth rate.
         * <p>
         * Is equal to Double.NaN if the value is unknown.
         * </p>
         * @return the azimuth rate in radians per second
         */
        public double getAzimuthRate() {
            return azimuthRate;
        }

        /**
         * Get the elevation rate.
         * <p>
         * Is equal to Double.NaN if the value is unknown.
         * </p>
         * @return the elevation rate in radians per second
         */
        public double getElevationRate() {
            return elevationRate;
        }

        /** {@inheritDoc} */
        @Override
        public AbsoluteDate getDate() {
            return date;
        }

        /**
         * Get a string representation of the instance in the CRD format.
         * @return a string representation of the instance, in the CRD format.
         * @since 12.0
         */
        @DefaultDataContext
        public String toCrdString() {
            return String.format("30 %s", toString());
        }

        @Override
        @DefaultDataContext
        public String toString() {
            // CRD suggested format, excluding the record type
            // azimuth, elevation: rad --> deg
            // azimuthRate, elevationRate: rad/s --> deg/s
            // 'local' is already utc.
            // Seconds of day (sod) is typically to 1 milllisec precision.
            final double sod = getDate()
                    .getComponents(TimeScalesFactory.getUTC()).getTime()
                    .getSecondsInLocalDay();

            final String str = String.format(
                    "%9.3f %8.4f %8.4f %1d %1d %1d %10.7f %10.7f", sod,
                    FastMath.toDegrees(azimuth), FastMath.toDegrees(elevation),
                    directionFlag, originIndicator, refractionCorrected ? 1 : 0,
                    FastMath.toDegrees(azimuthRate),
                    FastMath.toDegrees(elevationRate));
            return handleNaN(str).replace(',', '.');
        }
    }

    /** Meteorological data. */
    public static class Meteo {

        /** Number of neighbors for meteo data interpolation. */
        private static final int N_NEIGHBORS = 2;

        /** First available date. */
        private final AbsoluteDate firstDate;

        /** Last available date. */
        private final AbsoluteDate lastDate;

        /** Previous set of meteorological parameters. */
        private transient MeteorologicalMeasurement previousParam;

        /** Next set of solar meteorological parameters. */
        private transient MeteorologicalMeasurement nextParam;

        /** List of meteo data. */
        private final transient ImmutableTimeStampedCache<MeteorologicalMeasurement> meteo;

        /**
         * Constructor.
         * @param meteoData list of meteo data
         */
        public Meteo(final SortedSet<MeteorologicalMeasurement> meteoData) {

            // Size
            final int neighborsSize = (meteoData.size() < 2) ? meteoData.size() : N_NEIGHBORS;

            // Check neighbors size
            if (neighborsSize == 0) {

                // Meteo data -> empty cache
                this.meteo = ImmutableTimeStampedCache.emptyCache();

                // Null epochs (will ne be used)
                this.firstDate = null;
                this.lastDate  = null;

            } else {

                // Meteo data
                this.meteo = new ImmutableTimeStampedCache<MeteorologicalMeasurement>(neighborsSize, meteoData);

                // Initialize first and last available dates
                this.firstDate = meteoData.first().getDate();
                this.lastDate  = meteoData.last().getDate();

            }

        }

        /** Get an unmodifiable view of the tabulated meteorological data.
         * @return unmodifiable view of the tabulated meteorological data
         * @since 11.0
         */
        public List<MeteorologicalMeasurement> getData() {
            return meteo.getAll();
        }

        /**
         * Get the meteorological parameters at a given date.
         * @param date date when user wants the meteorological parameters
         * @return the meteorological parameters at date (can be null if
         *         meteorological data are empty).
         */
        public MeteorologicalMeasurement getMeteo(final AbsoluteDate date) {

            // Check if meteorological data are available
            if (meteo.getMaxNeighborsSize() == 0) {
                return null;
            }

            // Interpolating two neighboring meteorological parameters
            bracketDate(date);
            if (date.durationFrom(firstDate) <= 0 || date.durationFrom(lastDate) > 0) {
                // Date is outside file range
                return previousParam;
            } else {
                // Perform interpolations
                final double pressure    = getLinearInterpolation(date, previousParam.getPressure(), nextParam.getPressure());
                final double temperature = getLinearInterpolation(date, previousParam.getTemperature(), nextParam.getTemperature());
                final double humidity    = getLinearInterpolation(date, previousParam.getHumidity(), nextParam.getHumidity());
                return new MeteorologicalMeasurement(date, pressure, temperature, humidity);
            }

        }

        /**
         * Find the data bracketing a specified date.
         * @param date date to bracket
         */
        private void bracketDate(final AbsoluteDate date) {

            // don't search if the cached selection is fine
            if (previousParam != null &&
                date.durationFrom(previousParam.getDate()) > 0 &&
                date.durationFrom(nextParam.getDate()) <= 0) {
                return;
            }

            // Initialize previous and next parameters
            if (date.durationFrom(firstDate) <= 0) {
                // Current date is before the first date
                previousParam = meteo.getEarliest();
                nextParam     = previousParam;
            } else if (date.durationFrom(lastDate) > 0) {
                // Current date is after the last date
                previousParam = meteo.getLatest();
                nextParam     = previousParam;
            } else {
                // Current date is between first and last date
                final List<MeteorologicalMeasurement> neighbors = meteo.getNeighbors(date).collect(Collectors.toList());
                previousParam = neighbors.get(0);
                nextParam     = neighbors.get(1);
            }

        }

        /**
         * Performs a linear interpolation between two values The weights are computed
         * from the time delta between previous date, current date, next date.
         * @param date the current date
         * @param previousValue the value at previous date
         * @param nextValue the value at next date
         * @return the value interpolated for the current date
         */
        private double getLinearInterpolation(final AbsoluteDate date,
                                              final double previousValue,
                                              final double nextValue) {
            // Perform a linear interpolation
            final AbsoluteDate previousDate = previousParam.getDate();
            final AbsoluteDate currentDate = nextParam.getDate();
            final double dt = currentDate.durationFrom(previousDate);
            final double previousWeight = currentDate.durationFrom(date) / dt;
            final double nextWeight = date.durationFrom(previousDate) / dt;

            // Returns the data interpolated at the date
            return previousValue * previousWeight + nextValue * nextWeight;
        }

    }

    /**
     * Calibration Record.
     * @since 12.0
     */
    public static class Calibration implements TimeStamped {

        /** Data epoch. */
        private final AbsoluteDate date;

        /**
         * Type of data.
         *
         * 0=station combined transmit and receive calibration (“normal” SLR/LLR)
         * 1=station transmit calibration (e.g., one-way ranging to transponders)
         * 2=station receive calibration
         * 3=target combined transmit and receive calibrations
         * 4=target transmit calibration
         * 5=target receive calibration
         */
        private final int typeOfData;

        /** System configuration ID. */
        private final String systemConfigurationId;

        /** Number of data points recorded. */
        private final int numberOfPointsRecorded;

        /** Number of data points used. */
        private final int numberOfPointsUsed;

        /** One-way target distance (meters, nominal). */
        private final double oneWayDistance;

        /** Calibration System Delay. */
        private final double systemDelay;

        /** Calibration Delay Shift - a measure of calibration stability. */
        private final double delayShift;

        /** RMS of raw system delay. */
        private final double rms;

        /** Skew of raw system delay values from the mean. */
        private final double skew;

        /** Kurtosis of raw system delay values from the mean. */
        private final double kurtosis;

        /** System delay peak – mean value. */
        private final double peakMinusMean;

        /**
         * Calibration Type Indicator.
         *
         * 0=not used or undefined
         * 1=nominal (from once off assessment)
         * 2=external calibrations
         * 3=internal calibrations – telescope
         * 4=internal calibrations – building
         * 5=burst calibrations
         * 6=other
         */
        private final int typeIndicator;

        /**
         * Calibration Shift Type Indicator.
         *
         * 0=not used or undefined
         * 1=nominal (from once off assessment)
         * 2=pre- to post- Shift
         * 3=minimum to maximum
         * 4=other
         */
        private final int shiftTypeIndicator;

        /** Detector Channel.
         *
         * 0=not applicable or “all”
         * 1-4 for quadrant
         * 1-n for many channels
         */
        private final int detectorChannel;

        /**
         * Calibration Span.
         *
         * 0 = not applicable (e.g. Calibration type indicator is “nominal”)
         * 1 = Pre-calibration only
         * 2 = Post-calibration only
         * 3 = Combined (pre- and post-calibrations or multiple)
         * 4 = Real-time calibration (data taken while ranging to a satellite)
         */
        private final int span;

        /** Return Rate (%). */
        private final double returnRate;

        /**
         * Constructor.
         * @param date data epoch
         * @param typeOfData type of data
         * @param systemConfigurationId system configuration id
         * @param numberOfPointsRecorded number of data points recorded
         * @param numberOfPointsUsed number of data points used
         * @param oneWayDistance one-way target distance (nominal)
         * @param systemDelay calibration system delay
         * @param delayShift calibration delay shift - a measure of calibration stability
         * @param rms RMS of raw system delay
         * @param skew skew of raw system delay values from the mean.
         * @param kurtosis kurtosis of raw system delay values from the mean.
         * @param peakMinusMean system delay peak – mean value
         * @param typeIndicator calibration type indicator
         * @param shiftTypeIndicator calibration shift type indicator
         * @param detectorChannel detector channel
         * @param span calibration span
         * @param returnRate return rate (%)
         */
        public Calibration(final AbsoluteDate date, final int typeOfData,
                           final String systemConfigurationId,
                           final int numberOfPointsRecorded,
                           final int numberOfPointsUsed,
                           final double oneWayDistance,
                           final double systemDelay, final double delayShift,
                           final double rms, final double skew,
                           final double kurtosis, final double peakMinusMean,
                           final int typeIndicator, final int shiftTypeIndicator,
                           final int detectorChannel, final int span,
                           final double returnRate) {
            this.date                   = date;
            this.typeOfData             = typeOfData;
            this.systemConfigurationId  = systemConfigurationId;
            this.numberOfPointsRecorded = numberOfPointsRecorded;
            this.numberOfPointsUsed     = numberOfPointsUsed;
            this.systemDelay            = systemDelay;
            this.delayShift             = delayShift;
            this.rms                    = rms;
            this.skew                   = skew;
            this.kurtosis               = kurtosis;
            this.peakMinusMean          = peakMinusMean;
            this.typeIndicator          = typeIndicator;
            this.shiftTypeIndicator     = shiftTypeIndicator;
            this.detectorChannel        = detectorChannel;
            this.span                   = span;
            this.returnRate             = returnRate;
            this.oneWayDistance         = oneWayDistance == -1 ? Double.NaN : oneWayDistance; // -1=na
        }

        @Override
        public AbsoluteDate getDate() {
            return date;
        }

        /**
         * Get the type of data.
         *
         * <ul>
         * <li>0=station combined transmit and receive calibration (“normal” SLR/LLR)
         * <li>1=station transmit calibration (e.g., one-way ranging to transponders)
         * <li>2=station receive calibration
         * <li>3=target combined transmit and receive calibrations
         * <li>4=target transmit calibration
         * <li>5=target receive calibration
         * </ul>
         * @return the type of data
         */
        public int getTypeOfData() {
            return typeOfData;
        }

        /**
         * Get the system configuration id.
         * @return the system configuration id
         */
        public String getSystemConfigurationId() {
            return systemConfigurationId;
        }

        /**
         * Get the number of data points recorded.
         * @return the number of data points recorded, -1 if no information
         */
        public int getNumberOfPointsRecorded() {
            return numberOfPointsRecorded;
        }

        /**
         * Get the number of data points used.
         * @return the number of data points used, -1 if no information
         */
        public int getNumberOfPointsUsed() {
            return numberOfPointsUsed;
        }

        /**
         * Get the one-way target distance (nominal).
         * @return the one-way target distance (nominal)
         */
        public double getOneWayDistance() {
            return oneWayDistance;
        }

        /**
         * Get the calibration system delay.
         * @return the calibration system delay
         */
        public double getSystemDelay() {
            return systemDelay;
        }

        /**
         * Get the calibration delay shift.
         * @return the calibration delay shift
         */
        public double getDelayShift() {
            return delayShift;
        }

        /**
         * Get the rms of raw system delay.
         * @return the rms of raw system delay
         */
        public double getRms() {
            return rms;
        }

        /**
         * Get the skew of raw system delay values from the mean.
         * @return the skew of raw system delay values from the mean.
         */
        public double getSkew() {
            return skew;
        }

        /**
         * Get the kurtosis of raw system delay values from the mean.
         * @return the kurtosis of raw system delay values from the mean.
         */
        public double getKurtosis() {
            return kurtosis;
        }

        /**
         * Get the system delay peak – mean value.
         * @return the system delay peak – mean value
         */
        public double getPeakMinusMean() {
            return peakMinusMean;
        }

        /**
         * Get the calibration type indicator.
         *
         * <ul>
         * <li>0=not used or undefined
         * <li>1=nominal (from once off assessment)
         * <li>2=external calibrations
         * <li>3=internal calibrations – telescope
         * <li>4=internal calibrations – building
         * <li>5=burst calibrations
         * <li>6=other
         * </ul>
         * @return the calibration type indicator
         */
        public int getTypeIndicator() {
            return typeIndicator;
        }

        /**
         * Get the calibration shift type indicator.
         *
         * <ul>
         * <li>0=not used or undefined
         * <li>1=nominal (from once off assessment)
         * <li>2=pre- to post- Shift
         * <li>3=minimum to maximum
         * <li>4=other
         * </ul>
         * @return the calibration shift type indicator
         */
        public int getShiftTypeIndicator() {
            return shiftTypeIndicator;
        }

        /**
         * Get the detector channel.
         *
         * <ul>
         * <li>0=not applicable or “all”
         * <li>1-4 for quadrant
         * <li>1-n for many channels
         * </ul>
         * @return the detector channel
         */
        public int getDetectorChannel() {
            return detectorChannel;
        }

        /**
         * Get the calibration span.
         *
         * <ul>
         * <li>0 = not applicable (e.g. Calibration type indicator is “nominal”)
         * <li>1 = Pre-calibration only
         * <li>2 = Post-calibration only
         * <li>3 = Combined (pre- and post-calibrations or multiple)
         * <li>4 = Real-time calibration (data taken while ranging to a satellite)
         * </ul>
         * @return the calibration span
         */
        public int getSpan() {
            return span;
        }

        /**
         * Get the return rate.
         * @return the return rate
         */
        public double getReturnRate() {
            return returnRate;
        }

        /**
         * Get a string representation of the instance in the CRD format.
         * @return a string representation of the instance, in the CRD format.
         */
        @DefaultDataContext
        public String toCrdString() {
            return String.format("40 %s", toString());
        }

        @Override
        @DefaultDataContext
        public String toString() {
            // CRD suggested format, excluding the record type
            // systemDelay, delayShift: s --> ps
            // rms, peakMinusMean: s --> ps
            // 'local' is already utc.
            // Seconds of day (sod) is typically to 1 milllisec precision.
            final double sod = getDate()
                    .getComponents(TimeScalesFactory.getUTC()).getTime()
                    .getSecondsInLocalDay();

            final String str = String.format(
                    "%18.12f %1d %4s %8s %8s %8.4f %10.1f %8.1f %6.1f %7.3f %7.3f %6.1f %1d %1d %1d %1d %5.1f",
                    sod, typeOfData, systemConfigurationId,
                    formatIntegerOrNaN(numberOfPointsRecorded, -1),
                    formatIntegerOrNaN(numberOfPointsUsed, -1), oneWayDistance,
                    systemDelay * 1e12, delayShift * 1e12, rms * 1e12, skew,
                    kurtosis, peakMinusMean * 1e12, typeIndicator,
                    shiftTypeIndicator, detectorChannel, span, returnRate);
            return handleNaN(str).replace(',', '.');
        }

    }

    /**
     * Calibration Detail Record.
     * @since 12.0
     */
    public static class CalibrationDetail extends Calibration {
        // same as Calibration record except that the record type is '41' rather than '40'.

        /**
         * Constructor.
         * @param date data epoch
         * @param typeOfData type of data
         * @param systemConfigurationId system configuration id
         * @param numberOfPointsRecorded number of data points recorded
         * @param numberOfPointsUsed number of data points used
         * @param oneWayDistance one-way target distance (nominal)
         * @param systemDelay calibration system delay
         * @param delayShift calibration delay shift - a measure of calibration stability
         * @param rms RMS of raw system delay
         * @param skew skew of raw system delay values from the mean.
         * @param kurtosis kurtosis of raw system delay values from the mean.
         * @param peakMinusMean system delay peak – mean value
         * @param typeIndicator calibration type indicator
         * @param shiftTypeIndicator calibration shift type indicator
         * @param detectorChannel detector channel
         * @param span calibration span
         * @param returnRate return rate (%)
         */
        public CalibrationDetail(final AbsoluteDate date, final int typeOfData,
                                 final String systemConfigurationId,
                                 final int numberOfPointsRecorded,
                                 final int numberOfPointsUsed, final double oneWayDistance,
                                 final double systemDelay, final double delayShift,
                                 final double rms, final double skew, final double kurtosis,
                                 final double peakMinusMean, final int typeIndicator,
                                 final int shiftTypeIndicator, final int detectorChannel,
                                 final int span, final double returnRate) {
            super(date, typeOfData, systemConfigurationId, numberOfPointsRecorded,
                    numberOfPointsUsed, oneWayDistance, systemDelay, delayShift, rms, skew,
                    kurtosis, peakMinusMean, typeIndicator, shiftTypeIndicator,
                    detectorChannel, span, returnRate);
        }

        /**
         * Get a string representation of the instance in the CRD format.
         * @return a string representation of the instance, in the CRD format.
         */
        @DefaultDataContext
        public String toCrdString() {
            return String.format("41 %s", toString());
        }

    }

    /**
     * Session (Pass) Statistics Record.
     * @since 12.0
     */
    public static class SessionStatistics {

        /** System configuration ID. */
        private final String systemConfigurationId;

        /** Session RMS from the mean of raw accepted time-of-flight values minus the trend function. */
        private final double rms;

        /** Session skewness from the mean of raw accepted time-of-flight values minus the trend function. */
        private final double skewness;

        /** Session kurtosis from the mean of raw accepted time-of-flight values minus the trend function. */
        private final double kurtosis;

        /** Session peak – mean value. */
        private final double peakMinusMean;

        /**
         * Data quality assessment indicator.
         * <ul>
         * <li>0=undefined or no comment</li>
         * <li>1=clear, easily filtered data, with little or no noise</li>
         * <li>2=clear data with some noise; filtering is slightly compromised by noise level</li>
         * <li>3=clear data with a significant amount of noise, or weak data with little noise. Data are certainly
         * present, but filtering is difficult.</li>
         * <li>4=unclear data; data appear marginally to be present, but are very difficult to separate from noise
         * during filtering. Signal to noise ratio can be less than 1:1.</li>
         * <li>5=no data apparent</li>
         * </ul>
         */
        private final int dataQulityIndicator;

        /**
         * Constructor.
         * @param systemConfigurationId system configuration ID
         * @param rms session RMS from the mean of raw accepted time-of-flight values minus the trend function
         * @param skewness session skewness from the mean of raw accepted time-of-flight values minus the trend function
         * @param kurtosis session kurtosis from the mean of raw accepted time-of-flight values minus the trend function
         * @param peakMinusMean session peak – mean value
         * @param dataQulityIndicator data quality assessment indicator
         */
        public SessionStatistics(final String systemConfigurationId,
                                 final double rms, final double skewness,
                                 final double kurtosis,
                                 final double peakMinusMean,
                                 final int dataQulityIndicator) {
            this.systemConfigurationId = systemConfigurationId;
            this.rms                   = rms;
            this.skewness              = skewness;
            this.kurtosis              = kurtosis;
            this.peakMinusMean         = peakMinusMean;
            this.dataQulityIndicator   = dataQulityIndicator;
        }

        /**
         * Get system configuration id.
         * @return the system configuration id
         */
        public String getSystemConfigurationId() {
            return systemConfigurationId;
        }

        /**
         * Get the session RMS from the mean of raw accepted time-of-flight values minus the trend function.
         * @return the session RMS
         */
        public double getRms() {
            return rms;
        }

        /**
         * Get the session skewness from the mean of raw accepted time-of-flight values minus the trend function.
         * @return the session skewness
         */
        public double getSkewness() {
            return skewness;
        }

        /**
         * Get the session kurtosis from the mean of raw accepted time-of-flight values minus the trend function.
         * @return the session kurtosis
         */
        public double getKurtosis() {
            return kurtosis;
        }

        /**
         * Get the session peak – mean value.
         * @return the session peak – mean value
         */
        public double getPeakMinusMean() {
            return peakMinusMean;
        }

        /**
         * Get the data quality assessment indicator
         * <ul>
         * <li>0=undefined or no comment</li>
         * <li>1=clear, easily filtered data, with little or no noise</li>
         * <li>2=clear data with some noise; filtering is slightly compromised by noise level</li>
         * <li>3=clear data with a significant amount of noise, or weak data with little noise. Data are certainly
         * present, but filtering is difficult.</li>
         * <li>4=unclear data; data appear marginally to be present, but are very difficult to separate from noise
         * during filtering. Signal to noise ratio can be less than 1:1.</li>
         * <li>5=no data apparent</li>
         * </ul>
         * @return the data quality assessment indicator
         */
        public int getDataQulityIndicator() {
            return dataQulityIndicator;
        }

        /**
         * Get a string representation of the instance in the CRD format.
         * @return a string representation of the instance, in the CRD format.
         */
        public String toCrdString() {
            return String.format("50 %s", toString());
        }

        @Override
        public String toString() {
            // CRD suggested format, excluding the record type
            // rms, peakMinusMean: s --> ps
            final String str = String.format("%4s %6.1f %7.3f %7.3f %6.1f %1d",
                    systemConfigurationId, rms * 1e12, skewness, kurtosis,
                    peakMinusMean * 1e12, dataQulityIndicator);
            return handleNaN(str).replace(',', '.');
        }

    }

}
