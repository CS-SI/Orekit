/* Copyright 2022-2026 Thales Alenia Space
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

package org.orekit.gnss.metric.messages.rtcm.msm;

import org.orekit.gnss.metric.messages.rtcm.msm.headers.RtcmMsmSignalId;

/**
 * Container for RTCM MSM signal-specific data fields.
 * @author Nathan Schiffmacher
 * @since 14.0
 */
public class RtcmMsmSignalData {

    /** MSM signal identifier. */
    private RtcmMsmSignalId signalId;

    /** GNSS signal fine Pseudoranges (DF400, DF405). */
    private double finePseudorange;

    /** GNSS signal fine Phaserange data (DF401, DF406). */
    private double finePhaserange;

    /** GNSS Phaserange Lock Time Indicator (DF402, DF407). */
    private int lockTimeIndicator;

    /** Half-cycle ambiguity indicator (DF420). */
    private boolean halfCycleAmbiguityIndicator;

    /** GNSS signal CNRs (DF403, DF408). */
    private double cnr;

    /** GNSS signal fine Phaserange Rates (DF404). */
    private double finePhaserangeRate;

    /**
     * Get the MSM signal identifier.
     * @return MSM signal identifier
     */
    public RtcmMsmSignalId getSignalId() {
        return signalId;
    }

    /**
     * Set the MSM signal identifier.
     * @param signalId MSM signal identifier
     */
    public void setSignalId(final RtcmMsmSignalId signalId) {
        this.signalId = signalId;
    }

    /**
     * Get the fine pseudorange.
     * @return fine pseudorange in meters
     */
    public double getFinePseudorange() {
        return finePseudorange;
    }

    /**
     * Set the fine pseudorange.
     * @param finePseudorange fine pseudorange in meters
     */
    public void setFinePseudorange(final double finePseudorange) {
        this.finePseudorange = finePseudorange;
    }

    /**
     * Get the fine phaserange.
     * @return fine phaserange in meters
     */
    public double getFinePhaserange() {
        return finePhaserange;
    }

    /**
     * Set the fine phaserange.
     * @param finePhaserange fine phaserange in meters
     */
    public void setFinePhaserange(final double finePhaserange) {
        this.finePhaserange = finePhaserange;
    }

    /**
     * Get the phaserange lock time indicator.
     * @return lock time indicator value
     */
    public int getLockTimeIndicator() {
        return lockTimeIndicator;
    }

    /**
     * Set the phaserange lock time indicator.
     * @param lockTimeIndicator lock time indicator value
     */
    public void setLockTimeIndicator(final int lockTimeIndicator) {
        this.lockTimeIndicator = lockTimeIndicator;
    }

    /**
     * Get the half-cycle ambiguity indicator.
     * @return true if half-cycle ambiguity is present, false otherwise
     */
    public boolean getHalfCycleAmbiguityIndicator() {
        return halfCycleAmbiguityIndicator;
    }

    /**
     * Set the half-cycle ambiguity indicator.
     * @param halfCycleAmbiguityIndicator true if half-cycle ambiguity is present, false otherwise
     */
    public void setHalfCycleAmbiguityIndicator(final boolean halfCycleAmbiguityIndicator) {
        this.halfCycleAmbiguityIndicator = halfCycleAmbiguityIndicator;
    }

    /**
     * Get the carrier-to-noise ratio.
     * @return CNR in dB-Hz
     */
    public double getCnr() {
        return cnr;
    }

    /**
     * Set the carrier-to-noise ratio.
     * @param cnr CNR in dB-Hz
     */
    public void setCnr(final double cnr) {
        this.cnr = cnr;
    }

    /**
     * Get the fine phaserange rate.
     * @return fine phaserange rate in meters per second
     */
    public double getFinePhaserangeRate() {
        return finePhaserangeRate;
    }

    /**
     * Set the fine phaserange rate.
     * @param finePhaserangeRate fine phaserange rate in meters per second
     */
    public void setFinePhaserangeRate(final double finePhaserangeRate) {
        this.finePhaserangeRate = finePhaserangeRate;
    }
}
