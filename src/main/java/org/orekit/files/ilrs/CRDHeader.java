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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.orekit.annotation.DefaultDataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;

/**
 * Container for Consolidated laser ranging Data Format (CDR) header.
 * @author Bryan Cazabonne
 * @author Rongwang Li
 * @since 10.3
 */
public class CRDHeader extends ILRSHeader {

    /** String delimiter regex of datetime. */
    private static final String DATETIME_DELIMITER_REGEX = "[-:T]";

    /** Space. */
    private static final String SPACE = " ";

    /** Pattern of delimiter of datetime. */
    public static final Pattern PATTERN_DATETIME_DELIMITER_REGEX = Pattern.compile(DATETIME_DELIMITER_REGEX);

    /** Station name from official list. */
    private String stationName;

    /** System identifier: Crustal Dynamics Project (CDP) Pad Identifier for SLR. */
    private int systemIdentifier;

    /** System number: Crustal Dynamics Project (CDP) 2-digit system number for SLR. */
    private int systemNumber;

    /** System occupancy: Crustal Dynamics Project (CDP) 2-digit occupancy sequence number for SLR. */
    private int systemOccupancy;

    /** Station Epoch Time Scale. */
    private int epochIdentifier;

    /** Station network. */
    private String stationNetword;

    /** Spacecraft Epoch Time Scale (transponders only). */
    private int spacecraftEpochTimeScale;

    /** Data type. */
    private int dataType;

    /** A flag to indicate the data release. */
    private int dataReleaseFlag;

    /** Tropospheric refraction correction applied indicator. */
    private boolean isTroposphericRefractionApplied;

    /** Center of mass correction applied indicator. */
    private boolean isCenterOfMassCorrectionApplied;

    /** Receive amplitude correction applied indicator. */
    private boolean isReceiveAmplitudeCorrectionApplied;

    /** Station system delay applied indicator. */
    private boolean isStationSystemDelayApplied;

    /** Spacecraft system delay applied (transponders) indicator. */
    private boolean isTransponderDelayApplied;

    /** Range type. */
    private RangeType rangeType;

    /** Data quality indicator. */
    private int qualityIndicator;

    /** Prediction type (CPF or TLE). */
    private int predictionType;

    /** Year of century from CPF or TLE. */
    private int yearOfCentury;

    /**
     * Date and time.
     * CPF starting date and hour (MMDDHH) from CPF H2 record or
     * TLE epoch day/fractional day.
     */
    private String dateAndTime;

    /** Prediction provider (CPF provider in H1 record or TLE source). */
    private String predictionProvider;

    /** Empty constructor.
     * <p>
     * This constructor is not strictly necessary, but it prevents spurious
     * javadoc warnings with JDK 18 and later.
     * </p>
     * @since 12.0
     */
    public CRDHeader() {
        // nothing to do
    }

    /**
     * Get the station name from official list.
     * @return the station name from official list
     */
    public String getStationName() {
        return stationName;
    }

    /**
     * Set the station name from official list.
     * @param stationName the station name to set
     */
    public void setStationName(final String stationName) {
        this.stationName = stationName;
    }

    /**
     * Get the system identifier.
     * @return the system identifier
     */
    public int getSystemIdentifier() {
        return systemIdentifier;
    }

    /**
     * Set the system identifier.
     * @param systemIdentifier the system identifier to set
     */
    public void setSystemIdentifier(final int systemIdentifier) {
        this.systemIdentifier = systemIdentifier;
    }

    /**
     * Get the system number.
     * @return the system number
     */
    public int getSystemNumber() {
        return systemNumber;
    }

    /**
     * Set the system number.
     * @param systemNumber the system number to set
     */
    public void setSystemNumber(final int systemNumber) {
        this.systemNumber = systemNumber;
    }

    /**
     * Get the system occupancy.
     * @return the system occupancy
     */
    public int getSystemOccupancy() {
        return systemOccupancy;
    }

    /**
     * Set the system occupancy.
     * @param systemOccupancy the system occupancy to set
     */
    public void setSystemOccupancy(final int systemOccupancy) {
        this.systemOccupancy = systemOccupancy;
    }

    /**
     * Get the epoch identifier.
     * <p>
     * 3 = UTC (UNSO) ; 4 = UTC (GPS) ; 7 = UTC (BIPM) ; 10 = UTC (Station Time Scale)
     * </p>
     * @return the epoch identifier
     */
    public int getEpochIdentifier() {
        return epochIdentifier;
    }

    /**
     * Set the epoch identifier.
     * @param epochIdentifier the epoch identifier to set
     */
    public void setEpochIdentifier(final int epochIdentifier) {
        this.epochIdentifier = epochIdentifier;
    }

    /**
     * Get the station network.
     * @return the station network
     */
    public String getStationNetword() {
        return stationNetword;
    }

    /**
     * Set the station network.
     * @param stationNetword the station network to set
     */
    public void setStationNetword(final String stationNetword) {
        this.stationNetword = stationNetword;
    }

    /**
     * Get the spacecraft epoch time scale.
     * @return the spacecraft epoch time scale
     */
    public int getSpacecraftEpochTimeScale() {
        return spacecraftEpochTimeScale;
    }

    /**
     * Set the spacecraft epoch time scale.
     * @param spacecraftEpochTimeScale the spacecraft epoch time scale to set
     */
    public void setSpacecraftEpochTimeScale(final int spacecraftEpochTimeScale) {
        this.spacecraftEpochTimeScale = spacecraftEpochTimeScale;
    }

    /**
     * Get the data type.
     * <p>
     * 0 = full rate ; 1 = normal point ; 2 = sampled engineering
     * </p>
     * @return the data type
     */
    public int getDataType() {
        return dataType;
    }

    /**
     * Set the data type.
     * @param dataType the data type to set
     */
    public void setDataType(final int dataType) {
        this.dataType = dataType;
    }

    /**
     * Get the flag indicating the data release.
     * @return the flag indicating the data release
     */
    public int getDataReleaseFlag() {
        return dataReleaseFlag;
    }

    /**
     * Set the flag indicating the data release.
     * @param dataReleaseFlag the flag to set
     */
    public void setDataReleaseFlag(final int dataReleaseFlag) {
        this.dataReleaseFlag = dataReleaseFlag;
    }

    /**
     * Get the tropospheric refraction correction applied indicator.
     * @return true if tropospheric refraction correction is applied
     */
    public boolean isTroposphericRefractionApplied() {
        return isTroposphericRefractionApplied;
    }

    /**
     * Set the tropospheric refraction correction applied indicator.
     * @param isTroposphericRefractionApplied true if tropospheric refraction correction is applied
     */
    public void setIsTroposphericRefractionApplied(final boolean isTroposphericRefractionApplied) {
        this.isTroposphericRefractionApplied = isTroposphericRefractionApplied;
    }

    /**
     * Get the center of mass correction applied indicator.
     * @return true if center of mass correction is applied
     */
    public boolean isCenterOfMassCorrectionApplied() {
        return isCenterOfMassCorrectionApplied;
    }

    /**
     * Set the center of mass correction applied indicator.
     * @param isCenterOfMassCorrectionApplied true if center of mass correction is applied
     */
    public void setIsCenterOfMassCorrectionApplied(final boolean isCenterOfMassCorrectionApplied) {
        this.isCenterOfMassCorrectionApplied = isCenterOfMassCorrectionApplied;
    }

    /**
     * Get the receive amplitude correction applied indicator.
     * @return true if receive amplitude correction is applied
     */
    public boolean isReceiveAmplitudeCorrectionApplied() {
        return isReceiveAmplitudeCorrectionApplied;
    }

    /**
     * Set the receive amplitude correction applied indicator.
     * @param isReceiveAmplitudeCorrectionApplied true if receive amplitude correction is applied
     */
    public void setIsReceiveAmplitudeCorrectionApplied(final boolean isReceiveAmplitudeCorrectionApplied) {
        this.isReceiveAmplitudeCorrectionApplied = isReceiveAmplitudeCorrectionApplied;
    }

    /**
     * Get the station system delay applied indicator.
     * @return true if station system delay is applied
     */
    public boolean isStationSystemDelayApplied() {
        return isStationSystemDelayApplied;
    }

    /**
     * Set the station system delay applied indicator.
     * @param isStationSystemDelayApplied true if station system delay is applied
     */
    public void setIsStationSystemDelayApplied(final boolean isStationSystemDelayApplied) {
        this.isStationSystemDelayApplied = isStationSystemDelayApplied;
    }

    /**
     * Get the spacecraft system delay applied (transponders) indicator.
     * @return true if transponder delay is applied
     */
    public boolean isTransponderDelayApplied() {
        return isTransponderDelayApplied;
    }

    /**
     * Set the spacecraft system delay applied (transponders) indicator.
     * @param isTransponderDelayApplied true if transponder delay is applied
     */
    public void setIsTransponderDelayApplied(final boolean isTransponderDelayApplied) {
        this.isTransponderDelayApplied = isTransponderDelayApplied;
    }

    /**
     * Get the range type.
     * @return the range type
     */
    public RangeType getRangeType() {
        return rangeType;
    }

    /**
     * Set the range type indicator.
     * @param indicator range type indicator
     */
    public void setRangeType(final int indicator) {
        this.rangeType = RangeType.getRangeType(indicator);
    }

    /**
     * Get the data quality indicator.
     * @return the data quality indicator
     */
    public int getQualityIndicator() {
        return qualityIndicator;
    }

    /**
     * Set the data quality indicator.
     * @param qualityIndicator the indicator to set
     */
    public void setQualityIndicator(final int qualityIndicator) {
        this.qualityIndicator = qualityIndicator;
    }

    /**
     * Get the prediction type (CPF or TLE).
     * @return the prediction type
     */
    public int getPredictionType() {
        return predictionType;
    }

    /**
     * Set the prediction type.
     * @param predictionType the prediction type to set
     */
    public void setPredictionType(final int predictionType) {
        this.predictionType = predictionType;
    }

    /**
     * Get the year of century from CPF or TLE.
     * @return the year of century from CPF or TLE
     */
    public int getYearOfCentury() {
        return yearOfCentury;
    }

    /**
     * Set the year of century from CPF or TLE.
     * @param yearOfCentury the year of century to set
     */
    public void setYearOfCentury(final int yearOfCentury) {
        this.yearOfCentury = yearOfCentury;
    }


    /**
     * Get the date and time as the string value.
     * <p>
     * Depending the prediction type, this value can represent the
     * CPF starting date and hour (MMDDHH) from CPF H2 record or
     * TLE epoch day/fractional day
     * </p>
     * @return the date and time as the string value
     */
    public String getDateAndTime() {
        return dateAndTime;
    }

    /**
     * Set the string value of date and time.
     * @param dateAndTime the date and time to set
     */
    public void setDateAndTime(final String dateAndTime) {
        this.dateAndTime = dateAndTime;
    }

    /**
     * Get the prediction provider.
     * @return the preditction provider
     */
    public String getPredictionProvider() {
        return predictionProvider;
    }

    /**
     * Set the prediction provider.
     * @param predictionProvider the prediction provider to set
     */
    public void setPredictionProvider(final String predictionProvider) {
        this.predictionProvider = predictionProvider;
    }

    /**
     * Get a string representation of the H1 in the CRD format.
     * @return a string representation of the H1, in the CRD format.
     * @since 12.0
     */
    public String getH1CrdString() {
        final DateComponents dc = getProductionEpoch();
        return String.format("H1 %3s %2d %04d %02d %02d %02d", getFormat(),
                getVersion(), dc.getYear(), dc.getMonth(), dc.getDay(),
                getProductionHour());
    }

    /**
     * Get a string representation of the H2 in the CRD format.
     * @return a string representation of the H2, in the CRD format.
     * @since 12.0
     */
    public String getH2CrdString() {
        return String.format("H2 %s %4d %02d %02d %2d %s", stationName,
                systemIdentifier, systemNumber, systemOccupancy,
                epochIdentifier, stationNetword);
    }

    /**
     * Get a string representation of the H3 in the CRD format.
     * @return a string representation of the H3, in the CRD format.
     * @since 12.0
     */
    public String getH3CrdString() {
        final int targetLocation = getTargetLocation();
        return String.format("H3 %s %7s %4s %5s %1d %1d %2s", getName(),
                getIlrsSatelliteId(), getSic(), getNoradId(),
                getSpacecraftEpochTimeScale(), getTargetClass(),
                CRD.formatIntegerOrNaN(targetLocation, -1));
    }

    /**
     * Get a string representation of the H4 in the CRD format.
     * @return a string representation of the H4, in the CRD format.
     * @since 12.0
     */
    @DefaultDataContext
    public String getH4CrdString() {
        // "2006-11-13T15:23:52" -- > "2006 11 13 15 23 52"
        final TimeScale utc = TimeScalesFactory.getUTC();
        final String startEpoch = getStartEpoch().toStringWithoutUtcOffset(utc, 0);
        final String endEpoch = getEndEpoch().toStringWithoutUtcOffset(utc, 0);
        return String.format("H4 %2d %s %s %d %d %d %d %d %d %d %d", getDataType(),
                PATTERN_DATETIME_DELIMITER_REGEX.matcher(startEpoch).replaceAll(SPACE),
                PATTERN_DATETIME_DELIMITER_REGEX.matcher(endEpoch).replaceAll(SPACE),
                dataReleaseFlag, isTroposphericRefractionApplied ? 1 : 0,
                isCenterOfMassCorrectionApplied ? 1 : 0,
                isReceiveAmplitudeCorrectionApplied ? 1 : 0,
                isStationSystemDelayApplied ? 1 : 0,
                isTransponderDelayApplied ? 1 : 0, rangeType.getIndicator(),
                qualityIndicator);
    }

    /**
     * Get a string representation of the H5 in the CRD format.
     * @return a string representation of the H5, in the CRD format.
     * @since 12.0
     */
    public String getH5CrdString() {
        return String.format("H5 %2d %02d %s %3s %5d", getPredictionType(), getYearOfCentury(),
                getDateAndTime(), getPredictionProvider(), getSequenceNumber());
    }

    /** Range type for SLR data. */
    public enum RangeType {

        /** No ranges (i.e. transmit time only). */
        NO_RANGES(0),

        /** One-way ranging. */
        ONE_WAY(1),

        /** Two-way ranging. */
        TWO_WAY(2),

        /** Received times only. */
        RECEIVED_ONLY(3),

        /** Mixed. */
        MIXED(4);

        /** Codes map. */
        private static final Map<Integer, RangeType> CODES_MAP = new HashMap<>();
        static {
            for (final RangeType type : values()) {
                CODES_MAP.put(type.getIndicator(), type);
            }
        }

        /** range type indicator. */
        private final int indicator;

        /**
         * Constructor.
         * @param indicator range type indicator
         */
        RangeType(final int indicator) {
            this.indicator = indicator;
        }

        /**
         * Get the range type indicator.
         * @return the range type indicator
         */
        public int getIndicator() {
            return indicator;
        }

        /**
         * Get the range type for the given indicator.
         * @param id indicator
         * @return the range type corresponding to the indicator
         */
        public static RangeType getRangeType(final int id) {
            final RangeType type = CODES_MAP.get(id);
            if (type == null) {
               // Invalid value. An exception is thrown
                throw new OrekitException(OrekitMessages.INVALID_RANGE_INDICATOR_IN_CRD_FILE, id);
            }
            return type;
        }

    }

    /** Data type for CRD data.
     * @since 12.0
     */
    public enum DataType {

        /** Full rate. */
        FULL_RATE(0),

        /** Normal point. */
        NORMAL_POINT(1),

        /** Sampled engineering. */
        SAMPLED_ENGIEERING(2);

        /** Codes map. */
        private static final Map<Integer, DataType> CODES_MAP = new HashMap<>();
        static {
            for (final DataType type : values()) {
                CODES_MAP.put(type.getIndicator(), type);
            }
        }

        /** data type indicator. */
        private final int indicator;

        /**
         * Constructor.
         * @param indicator data type indicator
         */
        DataType(final int indicator) {
            this.indicator = indicator;
        }

        /**
         * Get the data type indicator.
         * @return the data type indicator
         */
        public int getIndicator() {
            return indicator;
        }

        /**
         * Get the data type for the given indicator.
         * @param id indicator
         * @return the data type corresponding to the indicator
         */
        public static DataType getDataType(final int id) {
            final DataType type = CODES_MAP.get(id);
            if (type == null) {
               // Invalid value. An exception is thrown
                throw new RuntimeException(String.format("Invalid data type indicator {0} in CRD file header", id));
            }
            return type;
        }

    }

}
