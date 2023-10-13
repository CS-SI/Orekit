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

import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;

/**
 * Container for common data contains in International Laser Ranging Service (ILRS) files header.
 * @see CPFHeader
 * @see CRDHeader
 * @author Bryan Cazabonne
 * @since 10.3
 */
public abstract class ILRSHeader {

    /** File format. */
    private String format;

    /** File version. */
    private int version;

    /** Date component of the ephemeris production. */
    private DateComponents productionEpoch;

    /** Hour of ephemeris production. */
    private int productionHour;

    /** Target name from official ILRS list (e.g. lageos1). */
    private String name;

    /** ILRS Satellite ID. */
    private String ilrsSatelliteId;

    /** SIC (Provided by ILRS; set to “-1” for targets without SIC). */
    private String sic;

    /** NORAD ID. */
    private String noradId;

    /** Target class. */
    private int targetClass;

    /** Target location (Earth orbit, Lunar orbit, Mars orbit, ...) .*/
    private int targetLocation;

    /** Starting epoch (UTC). */
    private AbsoluteDate startEpoch;

    /** Ending epoch (UTC). */
    private AbsoluteDate endEpoch;

    /** Sequence number. */
    private int sequenceNumber;

    /** Empty constructor.
     * <p>
     * This constructor is not strictly necessary, but it prevents spurious
     * javadoc warnings with JDK 18 and later.
     * </p>
     * @since 12.0
     */
    public ILRSHeader() {
        // nothing to do
    }

    /**
     * Get the file format.
     * @return the file format
     */
    public String getFormat() {
        return format;
    }

    /**
     * Set the file format.
     * @param format the format to set
     */
    public void setFormat(final String format) {
        this.format = format;
    }

    /**
     * Get the format version.
     * @return the format version
     */
    public int getVersion() {
        return version;
    }

    /**
     * Set the format version.
     * @param version the version to set
     */
    public void setVersion(final int version) {
        this.version = version;
    }

    /**
     * Get the date component of the ephemeris production.
     * @return the date component of the ephemeris production
     */
    public DateComponents getProductionEpoch() {
        return productionEpoch;
    }

    /**
     * Set the date component of the ephemeris production.
     * @param productionEpoch the date component to set
     */
    public void setProductionEpoch(final DateComponents productionEpoch) {
        this.productionEpoch = productionEpoch;
    }

    /**
     * Get the hour of ephemeris production (UTC).
     * @return the hour of ephemeris production
     */
    public int getProductionHour() {
        return productionHour;
    }

    /**
     * Set the hour of ephemeris production.
     * @param productionHour the hour of ephemeris production to set
     */
    public void setProductionHour(final int productionHour) {
        this.productionHour = productionHour;
    }

    /**
     * Get the satellite target name.
     * @return the satellite target name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the satellite target name.
     * @param name the satellite target name to set
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Get the IRLS satellite ID (based on COSPAR ID).
     * @return the IRLS satellite ID
     */
    public String getIlrsSatelliteId() {
        return ilrsSatelliteId;
    }

    /**
     * Set the IRLS satellite ID (based on COSPAR ID).
     * @param ilrsSatelliteId the IRLS satellite ID to set
     */
    public void setIlrsSatelliteId(final String ilrsSatelliteId) {
        this.ilrsSatelliteId = ilrsSatelliteId;
    }

    /**
     * Get the SIC ID.
     * @return the SIC ID
     */
    public String getSic() {
        return sic;
    }

    /**
     * Set the SIC ID.
     * @param sic the SIC ID to set
     */
    public void setSic(final String sic) {
        this.sic = sic;
    }

    /**
     * Get the satellite NORAD ID (i.e. Satellite Catalog Number).
     * @return the satellite NORAD ID
     */
    public String getNoradId() {
        return noradId;
    }

    /**
     * Set the satellite NORAD ID.
     * @param noradId the NORAD ID to set
     */
    public void setNoradId(final String noradId) {
        this.noradId = noradId;
    }

    /**
     * Get the target class.
     * <p>
     * 0 = no retroreflector; 1 = passive retroreflector; ...
     * </p>
     * @return the target class
     */
    public int getTargetClass() {
        return targetClass;
    }

    /**
     * Set the target class.
     * <p>
     * 0 = no retroreflector; 1 = passive retroreflector; ...
     * </p>
     * @param targetClass the target class to set
     */
    public void setTargetClass(final int targetClass) {
        this.targetClass = targetClass;
    }

    /**
     * Get the target location.
     * <p>
     * 1 = Earth orbit; 2 = Lunar orbit; ...
     * </p>
     * @return the target location
     */
    public int getTargetLocation() {
        return targetLocation;
    }

    /**
     * Set the target location.
     * <p>
     * 1 = Earth orbit; 2 = Lunar orbit; ...
     * </p>
     * @param targetLocation the target location to set
     */
    public void setTargetLocation(final int targetLocation) {
        this.targetLocation = targetLocation;
    }

    /**
     * Get the starting epoch (UTC).
     * @return the starting epoch
     */
    public AbsoluteDate getStartEpoch() {
        return startEpoch;
    }

    /**
     * Set the staring epoch (UTC).
     * @param startEpoch the starting epoch to set
     */
    public void setStartEpoch(final AbsoluteDate startEpoch) {
        this.startEpoch = startEpoch;
    }

    /**
     * Get the ending epoch (UTC).
     * @return the ending epoch
     */
    public AbsoluteDate getEndEpoch() {
        return endEpoch;
    }

    /**
     * Set the ending epoch (UTC).
     * @param endEpoch the ending epoch to set
     */
    public void setEndEpoch(final AbsoluteDate endEpoch) {
        this.endEpoch = endEpoch;
    }

    /**
     * Get the ephemeris sequence number.
     * @return the ephemeris sequence number
     */
    public int getSequenceNumber() {
        return sequenceNumber;
    }

    /**
     * Set the ephemeris sequence number.
     * @param sequenceNumber the ephemeris sequence number to set
     */
    public void setSequenceNumber(final int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

}
