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

import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;

/**
 * Container for Consolidated laser ranging Prediction File (CPF) header.
 * <p>
 * Note: Only the required fields are present.
 * </p>
 * @author Bryan Cazabonne
 * @since 10.3
 */
public class CPFHeader {

    /** File format. */
    private String format;

    /** File version. */
    private int version;

    /** Ephemeris source. */
    private String source;

    /** Date component of the ephemeris production. */
    private DateComponents productionEpoch;

    /** Hour of ephemeris production. */
    private int productionHour;

    /** Ephemeris Sequence number. */
    private int sequenceNumber;

    /** Sub-daily Ephemeris Sequence number. */
    private int subDailySequenceNumber;

    /** Target name from official ILRS list (e.g. lageos1). */
    private String name;

    /** ILRS Satellite ID. */
    private int ilrsSatelliteId;

    /** SIC (Provided by ILRS; set to “-1” for targets without SIC). */
    private int sic;

    /** NORAD ID. */
    private int noradId;

    /** Starting epoch (UTC). */
    private AbsoluteDate startEpoch;

    /** Ending epoch (UTC). */
    private AbsoluteDate endEpoch;

    /** Time between table entries (UTC). */
    private int step;

    /** Compatibility with TIVs. */
    private boolean isCompatibleWithTIVs;

    /** Target class. */
    private int targetClass;

    /** Reference frame. */
    private Frame refFrame;

    /** Rotational angle type. */
    private int rotationalAngleType;

    /** Center of mass correction. */
    private boolean isCenterOfMassCorrectionApplied;

    /** Target location (Earth orbit, Lunar orbit, Mars orbit, ...) .*/
    private int targetLocation;

    /** Pulse Repetition Frequency (PRF) [Hz]. */
    private double prf;

    /** Transponder transmit delay [s]. */
    private double transpTransmitDelay;

    /** Transponder UTC offset [s]. */
    private double transpUtcOffset;

    /** Transponder Oscillator Drift in parts. */
    private double transpOscDrift;

    /** Transponder Clock Reference Time . */
    private double transpClkRef;

    /** Approximate center of mass to reflector offset [m]. */
    private double centerOfMassOffset;

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
     * Get the ephemeris source.
     * @return the ephemeris source
     */
    public String getSource() {
        return source;
    }

    /**
     * Set the ephemeris source.
     * @param source the ephemeris source to set
     */
    public void setSource(final String source) {
        this.source = source;
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

    /**
     * Get the sub-daily ephemeris sequence number.
     * @return the sub-daily ephemeris sequence number
     */
    public int getSubDailySequenceNumber() {
        return subDailySequenceNumber;
    }

    /**
     * Set the sub-daily ephemeris sequence number.
     * @param subDailySequenceNumber the sub-daily ephemeris sequence number to set
     */
    public void setSubDailySequenceNumber(final int subDailySequenceNumber) {
        this.subDailySequenceNumber = subDailySequenceNumber;
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
    public int getIlrsSatelliteId() {
        return ilrsSatelliteId;
    }

    /**
     * Set the IRLS satellite ID (based on COSPAR ID).
     * @param ilrsSatelliteId the IRLS satellite ID to set
     */
    public void setIlrsSatelliteId(final int ilrsSatelliteId) {
        this.ilrsSatelliteId = ilrsSatelliteId;
    }

    /**
     * Get the SIC ID.
     * @return the SIC ID
     */
    public int getSic() {
        return sic;
    }

    /**
     * Set the SIC ID.
     * @param sic the SIC ID to set
     */
    public void setSic(final int sic) {
        this.sic = sic;
    }

    /**
     * Get the satellite NORAD ID (i.e. Satellite Catalog Number).
     * @return the satellite NORAD ID
     */
    public int getNoradId() {
        return noradId;
    }

    /**
     * Set the satellite NORAD ID.
     * @param noradId the NORAD ID to set
     */
    public void setNoradId(final int noradId) {
        this.noradId = noradId;
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
     * Get the time between table entries.
     * @return the time between table entries in seconds
     */
    public int getStep() {
        return step;
    }

    /**
     * Set the time between table entries.
     * @param step the time to set in seconds
     */
    public void setStep(final int step) {
        this.step = step;
    }

    /**
     * Get the flag for compatibility with TIVs.
     * @return true if compatible with TIVs
     */
    public boolean isCompatibleWithTIVs() {
        return isCompatibleWithTIVs;
    }

    /**
     * Set the flag for compatibility with TIVs.
     * @param isCompatibleWithTIVs true if compatible with TIVs
     */
    public void setIsCompatibleWithTIVs(final boolean isCompatibleWithTIVs) {
        this.isCompatibleWithTIVs = isCompatibleWithTIVs;
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
     * Get the reference frame.
     * @return the reference frame
     */
    public Frame getRefFrame() {
        return refFrame;
    }

    /**
     * Set the reference frame.
     * @param refFrame the reference frame to set
     */
    public void setRefFrame(final Frame refFrame) {
        this.refFrame = refFrame;
    }

    /**
     * Get the rotation angle type.
     * @return the rotation angle type
     */
    public int getRotationalAngleType() {
        return rotationalAngleType;
    }

    /**
     * Set the rotation angle type.
     * @param rotationalAngleType the rotation angle type to set
     */
    public void setRotationalAngleType(final int rotationalAngleType) {
        this.rotationalAngleType = rotationalAngleType;
    }

    /**
     * Get the flag telling if the center of mass correction is applied.
     * @return true if center of mass correction is applied
     */
    public boolean isCenterOfMassCorrectionApplied() {
        return isCenterOfMassCorrectionApplied;
    }

    /**
     * Set the flag telling if the center of mass correction is applied.
     * @param isCenterOfMassCorrectionApplied true if center of mass correction is applied
     */
    public void setIsCenterOfMassCorrectionApplied(final boolean isCenterOfMassCorrectionApplied) {
        this.isCenterOfMassCorrectionApplied = isCenterOfMassCorrectionApplied;
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
     * Get the Pulse Repetition Frequency (PRF).
     * @return the Pulse Repetition Frequency (PRF) in Hz
     */
    public double getPrf() {
        return prf;
    }

    /**
     * Set the Pulse Repetition Frequency (PRF).
     * @param prf the ulse Repetition Frequency (PRF) to set in Hz
     */
    public void setPrf(final double prf) {
        this.prf = prf;
    }

    /**
     * Get the transponder transmit delay.
     * @return the transponder transmit delay in seconds
     */
    public double getTranspTransmitDelay() {
        return transpTransmitDelay;
    }

    /**
     * Set the transponder transmit delay.
     * @param transpTransmitDelay the transponder transmit delay to set in seconds
     */
    public void setTranspTransmitDelay(final double transpTransmitDelay) {
        this.transpTransmitDelay = transpTransmitDelay;
    }

    /**
     * Get the transponder UTC offset.
     * @return the transponder UTC offset in seconds
     */
    public double getTranspUtcOffset() {
        return transpUtcOffset;
    }

    /**
     * Set the transponder UTC offset.
     * @param transpUtcOffset the UTC offset to set in seconds
     */
    public void setTranspUtcOffset(final double transpUtcOffset) {
        this.transpUtcOffset = transpUtcOffset;
    }

    /**
     * Get the transponder Oscillator Drift in parts in 10^15.
     * @return the transponder Oscillator Drift in parts.
     */
    public double getTranspOscDrift() {
        return transpOscDrift;
    }

    /**
     * Set the transponder Oscillator Drift in parts.
     * @param transpOscDrift the transponder Oscillator Drift in parts in 10^15 to set
     */
    public void setTranspOscDrift(final double transpOscDrift) {
        this.transpOscDrift = transpOscDrift;
    }

    /**
     * Get the transponder Clock Reference Time.
     * @return the transponder Clock Reference Time
     */
    public double getTranspClkRef() {
        return transpClkRef;
    }

    /**
     * Set the transponder Clock Reference Time.
     * @param transpClkRef the transponder Clock Reference Time to set
     */
    public void setTranspClkRef(final double transpClkRef) {
        this.transpClkRef = transpClkRef;
    }

    /**
     * Get the approximate center of mass to reflector offset.
     * @return the approximate center of mass to reflector offset in meters
     */
    public double getCenterOfMassOffset() {
        return centerOfMassOffset;
    }

    /**
     * Set the approximate center of mass to reflector offset.
     * @param centerOfMassOffset the offset to set in meters
     */
    public void setCenterOfMassOffset(final double centerOfMassOffset) {
        this.centerOfMassOffset = centerOfMassOffset;
    }

}
