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

import org.orekit.gnss.metric.messages.rtcm.RtcmData;

/**
 * Container for RTCM MSM cell data, grouping satellite and signal information.
 * @author Nathan Schiffmacher
 * @since 14.0
 */
public class RtcmMsmCellData extends RtcmData {

    /** Satellite-related MSM data. */
    private final RtcmMsmSatelliteData satelliteData;

    /** Signal-related MSM data. */
    private final RtcmMsmSignalData signalData;

    /**
     * Simple constructor.
     * @param satelliteData satellite-related MSM data
     * @param signalData signal-related MSM data
     */
    public RtcmMsmCellData(final RtcmMsmSatelliteData satelliteData, final RtcmMsmSignalData signalData) {
        this.satelliteData = satelliteData;
        this.signalData = signalData;
    }

    /**
     * Get satellite-related MSM data.
     * @return satellite data for this cell
     */
    public RtcmMsmSatelliteData getSatelliteData() {
        return this.satelliteData;
    }

    /**
     * Get signal-related MSM data.
     * @return signal data for this cell
     */
    public RtcmMsmSignalData getSignalData() {
        return this.signalData;
    }
}
