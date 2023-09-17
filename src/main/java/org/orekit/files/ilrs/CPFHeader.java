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

import org.orekit.frames.Frame;

/**
 * Container for Consolidated laser ranging Prediction File (CPF) header.
 * <p>
 * Note: Only the required fields are present.
 * </p>
 * @author Bryan Cazabonne
 * @since 10.3
 */
public class CPFHeader extends ILRSHeader {

    /** Ephemeris source. */
    private String source;

    /** Sub-daily Ephemeris Sequence number. */
    private int subDailySequenceNumber;

    /** Time between table entries (UTC). */
    private int step;

    /** Compatibility with TIVs. */
    private boolean isCompatibleWithTIVs;

    /** Reference frame. */
    private Frame refFrame;

    /** Reference frame identifier. */
    private int refFrameId;

    /** Rotational angle type. */
    private int rotationalAngleType;

    /** Center of mass correction. */
    private boolean isCenterOfMassCorrectionApplied;

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

    /** Empty constructor.
     * <p>
     * This constructor is not strictly necessary, but it prevents spurious
     * javadoc warnings with JDK 18 and later.
     * </p>
     * @since 12.0
     */
    public CPFHeader() {
        // nothing to do
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
     * Get the reference frame identifier.
     * @return the reference frame
     */
    public int getRefFrameId() {
        return refFrameId;
    }

    /**
     * Set the reference frame identifier.
     * @param refFrameId the reference frame identifier to set
     */
    public void setRefFrameId(final int refFrameId) {
        this.refFrameId = refFrameId;
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
