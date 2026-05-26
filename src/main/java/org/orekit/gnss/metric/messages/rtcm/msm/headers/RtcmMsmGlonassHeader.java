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
import org.orekit.time.GLONASSDate;

/**
 * RTCM MSM header for GLONASS observations.
 * @author Nathan Schiffmacher
 * @since 14.0
 */
public class RtcmMsmGlonassHeader extends RtcmMsmHeader {
    /** GLONASS day of week. */
    private int dayOfWeek;
    /** Epoch time within the GLONASS day, in seconds. */
    private double epochTime;

    /** Mapping of RTCM MSM signal identifiers to GLONASS MSM signal IDs. */
    private static final Map<Integer, RtcmMsmSignalId> SIGNAL_ID_MAP = new HashMap<>();
    static {
        // Reserved IDs are not mapped
        SIGNAL_ID_MAP.put(2,  RtcmMsmSignalId.GLO_1C);
        SIGNAL_ID_MAP.put(3,  RtcmMsmSignalId.GLO_1P);
        SIGNAL_ID_MAP.put(8,  RtcmMsmSignalId.GLO_2C);
        SIGNAL_ID_MAP.put(9,  RtcmMsmSignalId.GLO_2P);
    }

    /**
     * Get the GLONASS day of week.
     * @return GLONASS day of week
     */
    public int getDayOfWeek() {
        return this.dayOfWeek;
    }

    /**
     * Set the GLONASS day of week.
     * @param dayOfWeek GLONASS day of week
     */
    public void setDayOfWeek(final int dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    /**
     * Get the GLONASS epoch time.
     * @return epoch time within the GLONASS day, in seconds
     */
    public double getEpochTime() {
        return this.epochTime;
    }

    /**
     * Set the GLONASS epoch time.
     * @param epochTime epoch time within the GLONASS day, in seconds
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
                // IDs 25-64 are reserved
                satellites.add(new SatInSystem(SatelliteSystem.GLONASS, satId < 25 ? satId : -1));
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
     * Build the GLONASS epoch from GLONASS-specific time parameters.
     * @param na GLONASS day number within four-year interval
     * @param n4 GLONASS four-year interval number
     * @return GLONASS date corresponding to the epoch
     */
    public GLONASSDate getEpoch(final int na, final int n4) {
        return new GLONASSDate(na, n4, this.getEpochTime());
    }
}
