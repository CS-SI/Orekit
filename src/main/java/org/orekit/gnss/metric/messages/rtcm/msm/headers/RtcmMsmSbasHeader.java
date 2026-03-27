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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.orekit.gnss.SatInSystem;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.time.GNSSDate;

/**
 * RTCM MSM header for SBAS observations.
 * @author Nathan Schiffmacher
 * @since 14.0
 */
public class RtcmMsmSbasHeader extends RtcmMsmHeader {
    /** Epoch time within the GPS week, in seconds. */
    private double epochTime;

    /** Mapping of RTCM MSM signal identifiers to SBAS MSM signal IDs. */
    private static final Map<Integer, RtcmMsmSignalId> SIGNAL_ID_MAP = new HashMap<>();
    static {
        // Reserved IDs are not mapped
        SIGNAL_ID_MAP.put(2,  RtcmMsmSignalId.SBAS_1C);
        SIGNAL_ID_MAP.put(22,  RtcmMsmSignalId.SBAS_5I);
        SIGNAL_ID_MAP.put(23,  RtcmMsmSignalId.SBAS_5Q);
        SIGNAL_ID_MAP.put(24,  RtcmMsmSignalId.SBAS_5X);
    }

    /**
     * Get the SBAS epoch time (which is GPS epoch time)
     * @return epoch time within the GPS week, in seconds
     */
    public double getEpochTime() {
        return this.epochTime;
    }

    /**
     * Set the SBAS epoch time (which is GPS epoch time)
     * @param epochTime epoch time within the GPS week, in seconds
     */
    public void setEpochTime(final double epochTime) {
        this.epochTime = epochTime;
    }

    /** {@inheritDoc} */
    @Override
    public List<SatInSystem> convertSatellitesMask() {
        final List<SatInSystem> satellites = new ArrayList<>();
        for (int satId = 1; satId <= 64; satId ++) { 
            if ((this.getSatellitesMask() >> (64 - satId) & 1) == 1) {
                // IDs 40-64 are reserved
                // PRN is SatID + 119
                satellites.add(new SatInSystem(SatelliteSystem.SBAS, satId < 40 ? 119 + satId : -1));
            }
        }
        return satellites;
    }

    /** {@inheritDoc} */
    @Override
    public List<RtcmMsmSignalId> convertSignalsMask() {
        final List<RtcmMsmSignalId> signals = new ArrayList<>();
        for (int signalId = 1; signalId <= 32; signalId++) {
            if ((this.getSignalsMask() >> (32 - signalId) & 1) == 1) {
                signals.add(SIGNAL_ID_MAP.getOrDefault(signalId, RtcmMsmSignalId.RESERVED));
            }
        }
        return signals;
    }
    
    /**
     * Build the SBAS epoch (which is GPS epoch) from week number and seconds of week.
     * @param weekNumber SBAS (ie. GPS) week number
     * @return GNSS date corresponding to the epoch
     */
    public GNSSDate getEpoch(int weekNumber) {
        return new GNSSDate(weekNumber, this.getEpochTime(), SatelliteSystem.SBAS);
    }
}
