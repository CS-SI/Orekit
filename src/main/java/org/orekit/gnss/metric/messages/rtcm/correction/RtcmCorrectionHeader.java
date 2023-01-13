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
package org.orekit.gnss.metric.messages.rtcm.correction;

import org.orekit.gnss.metric.messages.common.SsrUpdateInterval;

/**
 * Container for common data in RTCM Correction Message type header.
 * @author Bryan Cazabonne
 * @since 12.0
 */
public class RtcmCorrectionHeader {

    /** SSR Epoch Time 1s. */
    private double epochTime1s;

    /** SSR Update Interval. */
    private SsrUpdateInterval ssrUpdateInterval;

    /** Multiple Message Indicator. */
    private int multipleMessageIndicator;

    /** IOD SSR. */
    private int iodSsr;

    /** SSR Provider ID. */
    private int ssrProviderId;

    /** SSR Solution ID. */
    private int ssrSolutionId;

    /** Number of satellites. */
    private int numberOfSatellites;

    /** Constructor. */
    public RtcmCorrectionHeader() {
        // Nothing to do ...
    }

    /**
     * Get the Epoch Time 1s.
     * <p>
     * Full seconds since the beginning of the GNSS week for
     * or full seconds since the beginning of GLONASS day
     * </p>
     * @return the Epoch Time 1s in seconds
     */
    public double getEpochTime1s() {
        return epochTime1s;
    }

    /**
     * Set the Epoch Time 1s.
     * @param epochTime1s the Epoch Time 1s to set
     */
    public void setEpochTime1s(final double epochTime1s) {
        this.epochTime1s = epochTime1s;
    }

    /**
     * Get the SSR Update Interval.
     * @return the SSR Update Interval
     */
    public SsrUpdateInterval getSsrUpdateInterval() {
        return ssrUpdateInterval;
    }

    /**
     * Set the SSR Update Interval.
     * @param ssrUpdateInterval the SSR Update Interval to set
     */
    public void setSsrUpdateInterval(final SsrUpdateInterval ssrUpdateInterval) {
        this.ssrUpdateInterval = ssrUpdateInterval;
    }

    /**
     * Get the Multiple Message Indicator.
     * <p>
     * 0 - Last message of a sequence. 1 - Multiple message transmitted
     * </p>
     * @return the SSR Multiple Message Indicator
     */
    public int getMultipleMessageIndicator() {
        return multipleMessageIndicator;
    }

    /**
     * Set the Multiple Message Indicator.
     * @param multipleMessageIndicator the Multiple Message Indicator to set
     */
    public void setMultipleMessageIndicator(final int multipleMessageIndicator) {
        this.multipleMessageIndicator = multipleMessageIndicator;
    }

    /**
     * Get the IOD SSR.
     * <p>
     * A change of Issue of Data SSR is used to
     * indicate a change in the SSR generating configuration.
     * </p>
     * @return the IOD SSR
     */
    public int getIodSsr() {
        return iodSsr;
    }

    /**
     * Set the IOD SSR.
     * @param iodSsr the IOF SSR to set
     */
    public void setIodSsr(final int iodSsr) {
        this.iodSsr = iodSsr;
    }

    /**
     * Get the SSR Provider ID.
     * @return the SSR Provider ID
     */
    public int getSsrProviderId() {
        return ssrProviderId;
    }

    /**
     * Set the SSR Provider ID.
     * @param ssrProviderId the SSR Provider ID to set
     */
    public void setSsrProviderId(final int ssrProviderId) {
        this.ssrProviderId = ssrProviderId;
    }

    /**
     * Get the SSR Solution ID.
     * @return the SSR Solution ID
     */
    public int getSsrSolutionId() {
        return ssrSolutionId;
    }

    /**
     * Set the SSR Solution ID.
     * @param ssrSolutionId the SSR Solution ID to set
     */
    public void setSsrSolutionId(final int ssrSolutionId) {
        this.ssrSolutionId = ssrSolutionId;
    }

    /**
     * Get the number of satellites for the current IGM message.
     * @return the number of satellites for the current IGM message
     */
    public int getNumberOfSatellites() {
        return numberOfSatellites;
    }

    /**
     * Set the number of satellites for the current IGM message.
     * @param numberOfSatellites the number of satellites to set
     */
    public void setNumberOfSatellites(final int numberOfSatellites) {
        this.numberOfSatellites = numberOfSatellites;
    }

}
