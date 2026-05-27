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

package org.orekit.gnss.metric.messages.rtcm.msm.headers;

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.util.Pair;
import org.orekit.gnss.SatInSystem;

/**
 * Base class for RTCM MSM message headers, holding common metadata and bit masks.
 * @author Nathan Schiffmacher
 * @since 14.0
 */
public abstract class RtcmMsmHeader {

    /** Reference Station ID. */
    private String referenceStation;

    /** Multiple Message Indicator. */
    private boolean multipleMessage;

    /** Issue of Data Station. */
    private int issueofDataStation;

    /** Clock Steering Indicator. */
    private int clockSteeringIndicator;

    /** External Clock Indicator. */
    private int externalClockIndicator;

    /** Divergence-free Smoothing Indicator. */
    private boolean divergenceFreeSmoothingIndicator;

    /** Smoothing Interval. */
    private int smoothingInterval;

    /** Satellites mask indicating which satellites are included in the MSM message. */
    private long satellitesMask;

    /** Signals mask indicating which signals are included in the MSM message. */
    private long signalsMask;

    /** Cells mask indicating which satellite/signal combinations are included in the MSM message. */
    private long cellsMask;

    /** Epoch time within the BeiDou week, in seconds. */
    private double epochTime;

    /** Constructor. */
    public RtcmMsmHeader() {
        // Nothing to do ...
    }

    /**
     * Get the epoch time.
     * @return epoch time within the GNSS week, in seconds
     */
    public double getEpochTime() {
        return this.epochTime;
    }

    /**
     * Set the epoch time.
     * @param epochTime epoch time within the GNSS week, in seconds
     */
    public void setEpochTime(final double epochTime) {
        this.epochTime = epochTime;
    }

    /**
     * Get the Reference Station ID.
     * @return the Reference Station ID
     */
    public String getReferenceStation() {
        return referenceStation;
    }

    /**
     * Set the Reference Station ID.
     * @param referenceStation the Reference Station ID to set
     */
    public void setReferenceStation(final String referenceStation) {
        this.referenceStation = referenceStation;
    }

    /**
     * Get the MSM Multiple Message Flag (DF393).
     * @return true if more MSM messages follow for the epoch, false otherwise
     */
    public boolean getMultipleMessageFlag() {
        return multipleMessage;
    }

    /**
     * Set the MSM Multiple Message Flag (DF393).
     * @param multipleMessageFlag true if more MSM messages follow for the epoch, false otherwise
     */
    public void setMultipleMessageFlag(final boolean multipleMessageFlag) {
        this.multipleMessage = multipleMessageFlag;
    }

    /**
     * Get the Issue of Data Station.
     * @return the Issue of Data Station
     */
    public int getIssueofDataStation() {
        return issueofDataStation;
    }

    /**
     * Set the Issue of Data Station.
     * @param issueofDataStation the Issue of Data Station to set
     */
    public void setIssueofDataStation(final int issueofDataStation) {
        this.issueofDataStation = issueofDataStation;
    }

    /**
     * Get the Clock Steering Indicator.
     * @return the Clock Steering Indicator
     */
    public int getClockSteeringIndicator() {
        return clockSteeringIndicator;
    }

    /**
     * Set the Clock Steering Indicator.
     * @param clockSteeringIndicator the Clock Steering Indicator to set
     */
    public void setClockSteeringIndicator(final int clockSteeringIndicator) {
        this.clockSteeringIndicator = clockSteeringIndicator;
    }

    /**
     * Get the External Clock Indicator.
     * @return the External Clock Indicator
     */
    public int getExternalClockIndicator() {
        return externalClockIndicator;
    }

    /**
     * Set the External Clock Indicator.
     * @param externalClockIndicator the External Clock Indicator to set
     */
    public void setExternalClockIndicator(final int externalClockIndicator) {
        this.externalClockIndicator = externalClockIndicator;
    }

    /**
     * Get the Divergence-free Smoothing Indicator.
     * @return true if divergence-free smoothing is used, false otherwise
     */
    public boolean getDivergenceFreeSmoothingIndicator() {
        return divergenceFreeSmoothingIndicator;
    }

    /**
     * Set the Divergence-free Smoothing Indicator.
     * @param divergenceFreeSmoothingIndicator the Divergence-free Smoothing Indicator to set
     */
    public void setDivergenceFreeSmoothingIndicator(final boolean divergenceFreeSmoothingIndicator) {
        this.divergenceFreeSmoothingIndicator = divergenceFreeSmoothingIndicator;
    }

    /**
     * Get the Smoothing Interval.
     * @return the Smoothing Interval
     */
    public int getSmoothingInterval() {
        return smoothingInterval;
    }

    /**
     * Set the Smoothing Interval.
     * @param smoothingInterval the Smoothing Interval to set
     */
    public void setSmoothingInterval(final int smoothingInterval) {
        this.smoothingInterval = smoothingInterval;
    }

    /**
     * Get the satellites mask.
     * @return bit mask indicating which satellites are present
     */
    public long getSatellitesMask() {
        return this.satellitesMask;
    }

    /**
     * Set the satellites mask.
     * @param satellitesMask bit mask indicating which satellites are present
     */
    public void setSatellitesMask(final long satellitesMask) {
        this.satellitesMask = satellitesMask;
    }

    /**
     * Get the number of satellites present in the mask.
     * @return number of satellites with their bit set in the mask
     */
    public int getNumberOfSatellites() {
        return Long.bitCount(this.satellitesMask);
    }

    /**
     * Get the signals mask.
     * @return bit mask indicating which signals are present
     */
    public long getSignalsMask() {
        return this.signalsMask;
    }

    /**
     * Set the signals mask.
     * @param signalsMask bit mask indicating which signals are present
     */
    public void setSignalsMask(final long signalsMask) {
        this.signalsMask = signalsMask;
    }

    /**
     * Get the number of signals present in the mask.
     * @return number of signals with their bit set in the mask
     */
    public int getNumberOfSignals() {
        return Long.bitCount(this.signalsMask);
    }

    /**
     * Get the cells mask.
     * @return bit mask indicating which satellite/signal cells are present
     */
    public long getCellsMask() {
        return this.cellsMask;
    }

    /**
     * Set the cells mask.
     * @param cellsMask bit mask indicating which satellite/signal cells are present
     */
    public void setCellsMask(final long cellsMask) {
        this.cellsMask = cellsMask;
    }

    /**
     * Get the number of cells present in the mask.
     * @return number of satellite/signal cells with their bit set in the mask
     */
    public int getNumberOfCells() {
        return Long.bitCount(this.cellsMask);
    }

    /**
     * Convert the cells mask to a list of satellite/signal pairs.
     * @return list of satellite/signal pairs corresponding to the active cells
     */
    public List<Pair<SatInSystem, RtcmMsmSignalId>> convertCellsMask() {
        final List<SatInSystem> satellites = this.convertSatellitesMask();
        final List<RtcmMsmSignalId> signals = this.convertSignalsMask();
        final List<Pair<SatInSystem, RtcmMsmSignalId>> cells = new ArrayList<>();

        // Compute the number of cells
        final int nSatellites = satellites.size();
        final int nSignals = signals.size();
        final int nCells = nSatellites * nSignals;

        // Use the cells mask to build the List of cells present
        for (int satIdx = 0; satIdx < nSatellites; satIdx++) {
            for (int sigIdx = 0; sigIdx < nSignals; sigIdx++) {
                final int cellIdx = satIdx * nSignals + sigIdx;
                if ((this.getCellsMask() >> (nCells - 1 - cellIdx) & 1) == 1) {
                    cells.add(new Pair<>(satellites.get(satIdx), signals.get(sigIdx)));
                }
            }
        }

        return cells;
    }

    /**
     * Convert the satellites mask to a list of satellites.
     * @return list of satellites present in the MSM message
     */
    public abstract List<SatInSystem> convertSatellitesMask();

    /**
     * Convert the signals mask to a list of signals.
     * @return list of signals present in the MSM message
     */
    public abstract List<RtcmMsmSignalId> convertSignalsMask();
}
