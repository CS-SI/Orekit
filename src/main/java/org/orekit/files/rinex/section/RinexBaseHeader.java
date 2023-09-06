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
package org.orekit.files.rinex.section;

import org.orekit.files.rinex.utils.RinexFileType;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateTimeComponents;

/** Base container for Rinex headers.
 * @since 12.0
 */
public class RinexBaseHeader {

    /** File type . */
    private final RinexFileType fileType;

    /** Rinex format Version. */
    private double formatVersion;

    /** Satellite System of the Rinex file (G/R/S/E/M). */
    private SatelliteSystem satelliteSystem;

    /** Name of the program creating current file. */
    private String programName;

    /** Name of the creator of the current file. */
    private String runByName;

    /** Date of the file creation. */
    private DateTimeComponents creationDateComponents;

    /** Time zone of the file creation. */
    private String creationTimeZone;

    /** Creation date as absolute date. */
    private AbsoluteDate creationDate;

    /** Digital Object Identifier.
     * @since 12.0
     */
    private String doi;

    /** License of use.
     * @since 12.0
     */
    private String license;

    /** Station information.
     * @since 12.0
     */
    private String stationInformation;

    /** Simple constructor.
     * @param fileType file type
     */
    protected RinexBaseHeader(final RinexFileType fileType) {
        this.fileType      = fileType;
        this.formatVersion = Double.NaN;
    }

    /**
     * Get the file type.
     * @return file type
     */
    public RinexFileType getFileType() {
        return fileType;
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
     * Getter for the run/by name.
     * @return the run/by name
     */
    public String getRunByName() {
        return runByName;
    }

    /**
     * Setter for the run/by name.
     * @param runByName the run/by name to set
     */
    public void setRunByName(final String runByName) {
        this.runByName = runByName;
    }

    /**
     * Getter for the creation date of the file as a string.
     * @return the creation date
     */
    public DateTimeComponents getCreationDateComponents() {
        return creationDateComponents;
    }

    /**
     * Setter for the creation date as a string.
     * @param creationDateComponents the creation date to set
     */
    public void setCreationDateComponents(final DateTimeComponents creationDateComponents) {
        this.creationDateComponents = creationDateComponents;
    }

    /**
     * Getter for the creation time zone of the file as a string.
     * @return the creation time zone as a string
     */
    public String getCreationTimeZone() {
        return creationTimeZone;
    }

    /**
     * Setter for the creation time zone.
     * @param creationTimeZone the creation time zone to set
     */
    public void setCreationTimeZone(final String creationTimeZone) {
        this.creationTimeZone = creationTimeZone;
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
     *  Getter for the Digital Object Information.
     * @return the Digital Object Information
     * @since 12.0
     */
    public String getDoi() {
        return doi;
    }

    /**
     * Setter for the Digital Object Information.
     * @param doi the Digital Object Information to set
     * @since 12.0
     */
    public void setDoi(final String doi) {
        this.doi = doi;
    }

    /**
     *  Getter for the license of use.
     * @return the license of use
     * @since 12.0
     */
    public String getLicense() {
        return license;
    }

    /**
     * Setter for the license of use.
     * @param license the license of use
     * @since 12.0
     */
    public void setLicense(final String license) {
        this.license = license;
    }

    /**
     *  Getter for the station information.
     * @return the station information
     * @since 12.0
     */
    public String getStationInformation() {
        return stationInformation;
    }

    /**
     * Setter for the station information.
     * @param stationInformation the station information to set
     * @since 12.0
     */
    public void setStationInformation(final String stationInformation) {
        this.stationInformation = stationInformation;
    }

}
