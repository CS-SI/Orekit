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

import org.orekit.gnss.SatInSystem;

/**
 * Container for RTCM MSM satellite-specific data fields.
 * @author Nathan Schiffmacher
 * @since 14.0
 */
public class RtcmMsmSatelliteData {
    /** Satellite the data refers to */
    private SatInSystem satellite;

    /** DF397: GNSS Satellite rough ranges (truncated to milliseconds), expressed in seconds. Null if not present. */
    private double intMillisRoughRange;

    /** DF398: GNSS Satellite rough ranges modulo 1 millisecond, expressed in seconds. Null if not present. */
    private double modMillisRoughRange;

    /** DF399: GNSS Satellite rough Phaserange Rates, expressed in meters per second. Null if not present. */
    private double roughPhaserangeRate;

    /** Extended satellite data, GNSS specific */
    private long extendedSatelliteData;

    /**
     * Get the satellite the MSM data refers to.
     * @return satellite identifier in its system
     */
    public SatInSystem getSatellite() {
        return satellite;
    }

    /**
     * Set the satellite the MSM data refers to.
     * @param satellite satellite identifier in its system
     */
    public void setSatellite(final SatInSystem satellite) {
        this.satellite = satellite;
    }

    /**
     * Get the rough range truncated to integer milliseconds.
     * @return rough range in seconds, truncated to milliseconds
     */
    public double getIntMillisRoughRange() {
        return intMillisRoughRange;
    }

    /**
     * Set the rough range truncated to integer milliseconds.
     * @param roughRangeMillis rough range in seconds, truncated to milliseconds
     */
    public void setIntMillisRoughRange(final double roughRangeMillis) {
        this.intMillisRoughRange = roughRangeMillis;
    }

    /**
     * Get the rough range modulo 1 millisecond.
     * @return rough range modulo 1 ms, in seconds
     */
    public double getModMillisRoughRange() {
        return modMillisRoughRange;
    }

    /**
     * Set the rough range modulo 1 millisecond.
     * @param roughRangeModMillis rough range modulo 1 ms, in seconds
     */
    public void setModMillisRoughRange(final double roughRangeModMillis) {
        this.modMillisRoughRange = roughRangeModMillis;
    }

    /**
     * Get the rough phaserange rate.
     * @return rough phaserange rate in meters per second
     */
    public double getRoughPhaserangeRate() {
        return roughPhaserangeRate;
    }

    /**
     * Set the rough phaserange rate.
     * @param phaserangeRate rough phaserange rate in meters per second
     */
    public void setRoughPhaserangeRate(final double phaserangeRate) {
        this.roughPhaserangeRate = phaserangeRate;
    }

    /**
     * Get the extended satellite data.
     * @return extended GNSS-specific satellite data
     */
    public long getExtendedSatelliteData() {
        return extendedSatelliteData;
    }

    /**
     * Set the extended satellite data.
     * @param extendedSatelliteData extended GNSS-specific satellite data
     */
    public void setExtendedSatelliteData(final long extendedSatelliteData) {
        this.extendedSatelliteData = extendedSatelliteData;
    }
}
