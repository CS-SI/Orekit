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

/**
 * RTCM MSM header for GPS observations.
 * @author Nathan Schiffmacher
 * @since 14.0
 */
public class RtcmMsmGpsHeader extends RtcmMsmHeader {

    /** Mapping of RTCM MSM signal identifiers to GPS MSM signal IDs. */
    private static final Map<Integer, RtcmMsmSignalId> SIGNAL_ID_MAP = new HashMap<>();
    static {
        // Reserved IDs are not mapped
        SIGNAL_ID_MAP.put(2,  RtcmMsmSignalId.GPS_1C);
        SIGNAL_ID_MAP.put(3,  RtcmMsmSignalId.GPS_1P);
        SIGNAL_ID_MAP.put(4,  RtcmMsmSignalId.GPS_1W);
        SIGNAL_ID_MAP.put(8,  RtcmMsmSignalId.GPS_2C);
        SIGNAL_ID_MAP.put(9,  RtcmMsmSignalId.GPS_2P);
        SIGNAL_ID_MAP.put(10, RtcmMsmSignalId.GPS_2W);
        SIGNAL_ID_MAP.put(15, RtcmMsmSignalId.GPS_2S);
        SIGNAL_ID_MAP.put(16, RtcmMsmSignalId.GPS_2L);
        SIGNAL_ID_MAP.put(17, RtcmMsmSignalId.GPS_2X);
        SIGNAL_ID_MAP.put(22, RtcmMsmSignalId.GPS_5I);
        SIGNAL_ID_MAP.put(23, RtcmMsmSignalId.GPS_5Q);
        SIGNAL_ID_MAP.put(24, RtcmMsmSignalId.GPS_5X);
        SIGNAL_ID_MAP.put(30, RtcmMsmSignalId.GPS_1S);
        SIGNAL_ID_MAP.put(31, RtcmMsmSignalId.GPS_1L);
        SIGNAL_ID_MAP.put(32, RtcmMsmSignalId.GPS_1X);
    }

    /** {@inheritDoc} */
    @Override
    public List<SatInSystem> convertSatellitesMask() {
        final List<SatInSystem> satellites = new ArrayList<>();
        for (int satId = 1; satId <= 64; satId++) {
            if ((this.getSatellitesMask() >> (64 - satId) & 1) == 1) {
                // id 64 is reserved
                satellites.add(new SatInSystem(SatelliteSystem.GPS, satId < 64 ? satId : -1));
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

}
