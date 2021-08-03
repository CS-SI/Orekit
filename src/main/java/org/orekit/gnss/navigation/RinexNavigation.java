/* Copyright 2002-2021 CS GROUP
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.orekit.gnss.SatelliteSystem;
import org.orekit.models.earth.ionosphere.KlobucharIonoModel;
import org.orekit.models.earth.ionosphere.NeQuickModel;
import org.orekit.propagation.analytical.gnss.data.BeidouNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.GLONASSNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.GPSNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.GalileoNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.IRNSSNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.QZSSNavigationMessage;
import org.orekit.propagation.analytical.gnss.data.SBASNavigationMessage;
import org.orekit.time.AbsoluteDate;

/**
 * Represents a parsed RINEX navigation messages files.
 * @author Bryan Cazabonne
 * @since 11.0
 */
public class RinexNavigation {

    /** Format version. */
    private double formatVersion;

    /** File type ('N' for navigation data). */
    private String fileType;

    /** Satellite system. */
    private SatelliteSystem satelliteSystem;

    /** Name of the program creating current file. */
    private String programName;

    /** Name of the agency creating the current file. */
    private String agencyName;

    /** Date of the file creation as a string. */
    private String creationDateString;

    /** Time of the file creation as a string. */
    private String creationTimeString;

    /** Time zone of the file creation as a string. */
    private String creationTimeZoneString;

    /** Creation date as absolute date. */
    private AbsoluteDate creationDate;

    /** Comments. */
    private String comments;

    /** Ionospheric correction type. */
    private String ionosphericCorrectionType;

    /** The 4 Klobuchar coefficients of a cubic equation representing the amplitude of the vertical delay. */
    private double[] klobucharAlpha;

    /** The 4 coefficients of a cubic equation representing the period of the model. */
    private double[] klobucharBeta;

    /** The three ionospheric coefficients broadcast in the Galileo navigation message. */
    private double[] neQuickAlpha;

    /** List of time system corrections. */
    private List<TimeSystemCorrection> timeSystemCorrections;

    /** Current number of leap seconds. */
    private int numberOfLeapSeconds;

    /** A map containing the GPS navigation messages. */
    private Map<String, List<GPSNavigationMessage>> gpsData;

    /** A map containing the Galileo navigation messages. */
    private Map<String, List<GalileoNavigationMessage>> galileoData;

    /** A map containing the Beidou navigation messages. */
    private Map<String, List<BeidouNavigationMessage>> beidouData;

    /** A map containing the QZSS navigation messages. */
    private Map<String, List<QZSSNavigationMessage>> qzssData;

    /** A map containing the IRNSS navigation messages. */
    private Map<String, List<IRNSSNavigationMessage>> irnssData;

    /** A map containing the GLONASS navigation messages. */
    private Map<String, List<GLONASSNavigationMessage>> glonassData;

    /** A map containing the SBAS navigation messages. */
    private Map<String, List<SBASNavigationMessage>> sbasData;

    /** Constructor. */
    public RinexNavigation() {
        this.comments              = "";
        this.timeSystemCorrections = new ArrayList<>();
        this.gpsData               = new HashMap<>();
        this.galileoData           = new HashMap<>();
        this.beidouData            = new HashMap<>();
        this.qzssData              = new HashMap<>();
        this.irnssData             = new HashMap<>();
        this.glonassData           = new HashMap<>();
        this.sbasData              = new HashMap<>();
    }

    /**
     * Getter for the format version.
     * @return the format version
     */
    public double getFormatVersion() {
        return formatVersion;
    }

    /**
     * Setter for the format version.
     * @param formatVersion the format version to set
     */
    public void setFormatVersion(final double formatVersion) {
        this.formatVersion = formatVersion;
    }

    /**
     * Get the file type.
     * @return 'N' for navigation data.
     */
    public String getFileType() {
        return fileType;
    }

    /**
     * Setter for the file type.
     * @param fileType must be 'N' for navigation data
     */
    public void setFileType(final String fileType) {
        this.fileType = fileType;
    }

    /**
     * Getter for the satellite system.
     * <p>
     * Not specified for RINEX 2.X versions (value is null).
     * </p>
     * @return the satellite system
     */
    public SatelliteSystem getSatelliteSystem() {
        return satelliteSystem;
    }

    /**
     * Setter for the satellite system.
     * @param satelliteSystem the satellite system to set
     */
    public void setSatelliteSystem(final SatelliteSystem satelliteSystem) {
        this.satelliteSystem = satelliteSystem;
    }

    /**
     * Getter for the program name.
     * @return the program name
     */
    public String getProgramName() {
        return programName;
    }

    /**
     * Setter for the program name.
     * @param programName the program name to set
     */
    public void setProgramName(final String programName) {
        this.programName = programName;
    }

    /**
     * Getter for the agency name.
     * @return the agencyName
     */
    public String getAgencyName() {
        return agencyName;
    }

    /**
     * Setter for the agency name.
     * @param agencyName the agency name to set
     */
    public void setAgencyName(final String agencyName) {
        this.agencyName = agencyName;
    }

    /**
     * Getter for the creation date of the file as a string.
     * @return the creation date as a string
     */
    public String getCreationDateString() {
        return creationDateString;
    }

    /**
     * Setter for the creation date as a string.
     * @param creationDateString the creation date as a string to set
     */
    public void setCreationDateString(final String creationDateString) {
        this.creationDateString = creationDateString;
    }

    /**
     * Getter for the creation time of the file as a string.
     * @return the creation time as a string
     */
    public String getCreationTimeString() {
        return creationTimeString;
    }

    /**
     * Setter for the creation time as a string.
     * @param creationTimeString the creation time as a string to set
     */
    public void setCreationTimeString(final String creationTimeString) {
        this.creationTimeString = creationTimeString;
    }

    /**
     * Getter for the creation time zone of the file as a string.
     * @return the creation time zone as a string
     */
    public String getCreationTimeZoneString() {
        return creationTimeZoneString;
    }

    /**
     * Setter for the creation time zone.
     * @param creationTimeZoneString the creation time zone as a string to set
     */
    public void setCreationTimeZoneString(final String creationTimeZoneString) {
        this.creationTimeZoneString = creationTimeZoneString;
    }

    /**
     * Getter for the creation date.
     * @return the creation date
     */
    public AbsoluteDate getCreationDate() {
        return creationDate;
    }

    /**
     * Setter for the creation date.
     * @param creationDate the creation date to set
     */
    public void setCreationDate(final AbsoluteDate creationDate) {
        this.creationDate = creationDate;
    }

    /**
     * Getter for the comments.
     * @return the comments
     */
    public String getComments() {
        return comments;
    }

    /**
     * Add a comment line.
     * @param comment the comment line to add
     */
    public void addComment(final String comment) {
        this.comments = comments.concat(comment);
    }

    /**
     * Getter for the ionospheric correction type.
     * <p>
     * Only the three first characters are given (e.g. GAL, GPS, QZS, BDS, or IRN)
     * </p>
     * @return the ionospheric correction type
     */
    public String getIonosphericCorrectionType() {
        return ionosphericCorrectionType;
    }

    /**
     * Setter for the ionospheric correction type.
     * @param ionosphericCorrectionType the ionospheric correction type to set
     */
    public void setIonosphericCorrectionType(final String ionosphericCorrectionType) {
        this.ionosphericCorrectionType = ionosphericCorrectionType;
    }

    /**
     * Get the "alpha" ionospheric parameters.
     * <p>
     * They are used to initialize the {@link KlobucharIonoModel}.
     * </p>
     * @return the "alpha" ionospheric parameters
     */
    public double[] getKlobucharAlpha() {
        return klobucharAlpha.clone();
    }

    /**
     * Set the "alpha" ionspheric parameters.
     * @param klobucharAlpha the "alpha" ionspheric parameters to set
     */
    public void setKlobucharAlpha(final double[] klobucharAlpha) {
        this.klobucharAlpha = klobucharAlpha.clone();
    }

    /**
     * Get the "beta" ionospheric parameters.
     * <p>
     * They are used to initialize the {@link KlobucharIonoModel}.
     * </p>
     * @return the "beta" ionospheric parameters
     */
    public double[] getKlobucharBeta() {
        return klobucharBeta.clone();
    }

    /**
     * Set the "beta" ionospheric parameters.
     * @param klobucharBeta the "beta" ionospheric parameters to set
     */
    public void setKlobucharBeta(final double[] klobucharBeta) {
        this.klobucharBeta = klobucharBeta.clone();
    }

    /**
     * Get the "alpha" ionospheric parameters.
     * <p>
     * They are used to initialize the {@link NeQuickModel}.
     * </p>
     * @return the "alpha" ionospheric parameters
     */
    public double[] getNeQuickAlpha() {
        return neQuickAlpha.clone();
    }

    /**
     * Set the "alpha" ionospheric parameters.
     * @param neQuickAlpha the "alpha" ionospheric parameters to set
     */
    public void setNeQuickAlpha(final double[] neQuickAlpha) {
        this.neQuickAlpha = neQuickAlpha.clone();
    }

    /**
     * Getter for the time system corrections contained in the file header.
     * <p>
     * Corrections to transform the system time to UTC or oter time system.
     * </p>
     * @return the list of time system corrections
     */
    public List<TimeSystemCorrection> getTimeSystemCorrections() {
        return timeSystemCorrections;
    }

    /**
     * Add a time system correction to the list.
     * @param timeSystemCorrection the element to add
     */
    public void addTimeSystemCorrections(final TimeSystemCorrection timeSystemCorrection) {
        this.timeSystemCorrections.add(timeSystemCorrection);
    }

    /**
     * Getter for the current number of leap seconds.
     * @return the current number of leap seconds
     */
    public int getNumberOfLeapSeconds() {
        return numberOfLeapSeconds;
    }

    /**
     * Setter for the current number of leap seconds.
     * @param numberOfLeapSeconds the number of leap seconds to set
     */
    public void setNumberOfLeapSeconds(final int numberOfLeapSeconds) {
        this.numberOfLeapSeconds = numberOfLeapSeconds;
    }

    /**
     * Get all the GPS navigation messages contained in the file.
     * @return an unmodifiable list of GPS navigation messages
     */
    public Map<String, List<GPSNavigationMessage>> getGPSNavigationMessages() {
        return Collections.unmodifiableMap(gpsData);
    }

    /**
     * Get the GPS navigation messages for the given satellite Id.
     * @param satId satellite Id (i.e. Satellite System (e.g. G) + satellite number)
     * @return an unmodifiable list of GPS navigation messages
     */
    public List<GPSNavigationMessage> getGPSNavigationMessages(final String satId) {
        return Collections.unmodifiableList(gpsData.get(satId));
    }

    /**
     * Add a GPS navigation message to the list.
     * @param message message to add
     */
    public void addGPSNavigationMessage(final GPSNavigationMessage message) {
        final int    gpsPRN = message.getPRN();
        final String prnString = gpsPRN < 10 ? "0" + String.valueOf(gpsPRN) : String.valueOf(gpsPRN);
        final String satId = SatelliteSystem.GPS.getKey() + prnString;
        gpsData.putIfAbsent(satId, new ArrayList<GPSNavigationMessage>());
        gpsData.get(satId).add(message);
    }

    /**
     * Get all the Galileo navigation messages contained in the file.
     * @return an unmodifiable list of Galileo navigation messages
     */
    public Map<String, List<GalileoNavigationMessage>> getGalileoNavigationMessages() {
        return Collections.unmodifiableMap(galileoData);
    }

    /**
     * Get the Galileo navigation messages for the given satellite Id.
     * @param satId satellite Id (i.e. Satellite System (e.g. E) + satellite number)
     * @return an unmodifiable list of Galileo navigation messages
     */
    public List<GalileoNavigationMessage> getGalileoNavigationMessages(final String satId) {
        return Collections.unmodifiableList(galileoData.get(satId));
    }

    /**
     * Add a Galileo navigation message to the list.
     * @param message message to add
     */
    public void addGalileoNavigationMessage(final GalileoNavigationMessage message) {
        final int    galPRN = message.getPRN();
        final String prnString = galPRN < 10 ? "0" + String.valueOf(galPRN) : String.valueOf(galPRN);
        final String satId = SatelliteSystem.GALILEO.getKey() + prnString;
        galileoData.putIfAbsent(satId, new ArrayList<GalileoNavigationMessage>());
        galileoData.get(satId).add(message);
    }

    /**
     * Get all the Beidou navigation messages contained in the file.
     * @return an unmodifiable list of Beidou navigation messages
     */
    public Map<String, List<BeidouNavigationMessage>> getBeidouNavigationMessages() {
        return Collections.unmodifiableMap(beidouData);
    }

    /**
     * Get the Beidou navigation messages for the given satellite Id.
     * @param satId satellite Id (i.e. Satellite System (e.g. C) + satellite number)
     * @return an unmodifiable list of Beidou navigation messages
     */
    public List<BeidouNavigationMessage> getBeidouNavigationMessages(final String satId) {
        return Collections.unmodifiableList(beidouData.get(satId));
    }

    /**
     * Add a Beidou navigation message to the list.
     * @param message message to add
     */
    public void addBeidouNavigationMessage(final BeidouNavigationMessage message) {
        final int    bdtPRN = message.getPRN();
        final String prnString = bdtPRN < 10 ? "0" + String.valueOf(bdtPRN) : String.valueOf(bdtPRN);
        final String satId = SatelliteSystem.BEIDOU.getKey() + prnString;
        beidouData.putIfAbsent(satId, new ArrayList<BeidouNavigationMessage>());
        beidouData.get(satId).add(message);
    }

    /**
     * Get all the QZSS navigation messages contained in the file.
     * @return an unmodifiable list of QZSS navigation messages
     */
    public Map<String, List<QZSSNavigationMessage>> getQZSSNavigationMessages() {
        return Collections.unmodifiableMap(qzssData);
    }

    /**
     * Get the QZSS navigation messages for the given satellite Id.
     * @param satId satellite Id (i.e. Satellite System (e.g. J) + satellite number)
     * @return an unmodifiable list of QZSS navigation messages
     */
    public List<QZSSNavigationMessage> getQZSSNavigationMessages(final String satId) {
        return Collections.unmodifiableList(qzssData.get(satId));
    }

    /**
     * Add a QZSS navigation message to the list.
     * @param message message to add
     */
    public void addQZSSNavigationMessage(final QZSSNavigationMessage message) {
        final int    qzsPRN = message.getPRN();
        final String prnString = qzsPRN < 10 ? "0" + String.valueOf(qzsPRN) : String.valueOf(qzsPRN);
        final String satId = SatelliteSystem.QZSS.getKey() + prnString;
        qzssData.putIfAbsent(satId, new ArrayList<QZSSNavigationMessage>());
        qzssData.get(satId).add(message);
    }

    /**
     * Get all the IRNSS navigation messages contained in the file.
     * @return an unmodifiable list of IRNSS navigation messages
     */
    public Map<String, List<IRNSSNavigationMessage>> getIRNSSNavigationMessages() {
        return Collections.unmodifiableMap(irnssData);
    }

    /**
     * Get the IRNSS navigation messages for the given satellite Id.
     * @param satId satellite Id (i.e. Satellite System (e.g. I) + satellite number)
     * @return an unmodifiable list of IRNSS navigation messages
     */
    public List<IRNSSNavigationMessage> getIRNSSNavigationMessages(final String satId) {
        return Collections.unmodifiableList(irnssData.get(satId));
    }

    /**
     * Add a IRNSS navigation message to the list.
     * @param message message to add
     */
    public void addIRNSSNavigationMessage(final IRNSSNavigationMessage message) {
        final int    irsPRN = message.getPRN();
        final String prnString = irsPRN < 10 ? "0" + String.valueOf(irsPRN) : String.valueOf(irsPRN);
        final String satId = SatelliteSystem.IRNSS.getKey() + prnString;
        irnssData.putIfAbsent(satId, new ArrayList<IRNSSNavigationMessage>());
        irnssData.get(satId).add(message);
    }

    /**
     * Get all the Glonass navigation messages contained in the file.
     * @return an unmodifiable list of Glonass navigation messages
     */
    public Map<String, List<GLONASSNavigationMessage>> getGlonassNavigationMessages() {
        return Collections.unmodifiableMap(glonassData);
    }

    /**
     * Get the Glonass navigation messages for the given satellite Id.
     * @param satId satellite Id (i.e. Satellite System (e.g. R) + satellite number)
     * @return an unmodifiable list of Glonass navigation messages
     */
    public List<GLONASSNavigationMessage> getGlonassNavigationMessages(final String satId) {
        return Collections.unmodifiableList(glonassData.get(satId));
    }

    /**
     * Add a Glonass navigation message to the list.
     * @param message message to add
     */
    public void addGlonassNavigationMessage(final GLONASSNavigationMessage message) {
        final int    gloPRN = message.getPRN();
        final String prnString = gloPRN < 10 ? "0" + String.valueOf(gloPRN) : String.valueOf(gloPRN);
        final String satId = SatelliteSystem.GLONASS.getKey() + prnString;
        glonassData.putIfAbsent(satId, new ArrayList<GLONASSNavigationMessage>());
        glonassData.get(satId).add(message);
    }

    /**
     * Get all the SBAS navigation messages contained in the file.
     * @return an unmodifiable list of SBAS navigation messages
     */
    public Map<String, List<SBASNavigationMessage>> getSBASNavigationMessages() {
        return Collections.unmodifiableMap(sbasData);
    }

    /**
     * Get the SBAS navigation messages for the given satellite Id.
     * @param satId satellite Id (i.e. Satellite System (e.g. S) + satellite number)
     * @return an unmodifiable list of SBAS navigation messages
     */
    public List<SBASNavigationMessage> getSBASNavigationMessages(final String satId) {
        return Collections.unmodifiableList(sbasData.get(satId));
    }

    /**
     * Add a SBAS navigation message to the list.
     * @param message message to add
     */
    public void addSBASNavigationMessage(final SBASNavigationMessage message) {
        final int    sbsPRN = message.getPRN();
        final String prnString = sbsPRN < 10 ? "0" + String.valueOf(sbsPRN) : String.valueOf(sbsPRN);
        final String satId = SatelliteSystem.SBAS.getKey() + prnString;
        sbasData.putIfAbsent(satId, new ArrayList<SBASNavigationMessage>());
        sbasData.get(satId).add(message);
    }

    /** Container for time system corrections. */
    public static class TimeSystemCorrection {

        /** Time system correction type. */
        private String timeSystemCorrectionType;

        /** A0 coefficient of linear polynomial for time system correction. */
        private double timeSystemCorrectionA0;

        /** A1 coefficient of linear polynomial for time system correction. */
        private double timeSystemCorrectionA1;

        /** Reference time for time system correction (seconds into GNSS week). */
        private int timeSystemCorrectionSecOfWeek;

        /** Reference week number for time system correction. */
        private int timeSystemCorrectionWeekNumber;

        /**
         * Constructor.
         * @param timeSystemCorrectionType       time system correction type
         * @param timeSystemCorrectionA0         A0 coefficient of linear polynomial for time system correction
         * @param timeSystemCorrectionA1         A1 coefficient of linear polynomial for time system correction
         * @param timeSystemCorrectionSecOfWeek  reference time for time system correction
         * @param timeSystemCorrectionWeekNumber reference week number for time system correction
         */
        public TimeSystemCorrection(final String timeSystemCorrectionType,
                                    final double timeSystemCorrectionA0,
                                    final double timeSystemCorrectionA1,
                                    final int timeSystemCorrectionSecOfWeek,
                                    final int timeSystemCorrectionWeekNumber) {
            this.timeSystemCorrectionType       = timeSystemCorrectionType;
            this.timeSystemCorrectionA0         = timeSystemCorrectionA0;
            this.timeSystemCorrectionA1         = timeSystemCorrectionA1;
            this.timeSystemCorrectionSecOfWeek  = timeSystemCorrectionSecOfWeek;
            this.timeSystemCorrectionWeekNumber = timeSystemCorrectionWeekNumber;
        }

        /**
         * Getter for the time system correction type.
         * @return the time system correction type
         */
        public String getTimeSystemCorrectionType() {
            return timeSystemCorrectionType;
        }

        /**
         * Getter for the A0 coefficient of the time system correction.
         * <p>
         * deltaT = {@link #getTimeSystemCorrectionA0() A0} +
         *          {@link #getTimeSystemCorrectionA1() A1} * (t - tref)
         * </p>
         * @return the A0 coefficient of the time system correction
         */
        public double getTimeSystemCorrectionA0() {
            return timeSystemCorrectionA0;
        }

        /**
         * Getter for the A1 coefficient of the time system correction.
         * <p>
         * deltaT = {@link #getTimeSystemCorrectionA0() A0} +
         *          {@link #getTimeSystemCorrectionA1() A1} * (t - tref)
         * </p>
         * @return the A1 coefficient of the time system correction
         */
        public double getTimeSystemCorrectionA1() {
            return timeSystemCorrectionA1;
        }

        /**
         * Getter for the reference time of the time system correction polynomial.
         * <p>
         * Seconds into GNSS week
         * </p>
         * @return the reference time of the time system correction polynomial
         */
        public int getTimeSystemCorrectionSecOfWeek() {
            return timeSystemCorrectionSecOfWeek;
        }

        /**
         * Getter for the reference week number of the time system correction polynomial.
         * <p>
         * Continuous number since the reference epoch of the corresponding GNSS constellation
         * </p>
         * @return the reference week number of the time system correction polynomial
         */
        public int getTimeSystemCorrectionWeekNumber() {
            return timeSystemCorrectionWeekNumber;
        }

    }

}
