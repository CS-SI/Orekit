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
package org.orekit.gnss.metric.messages.ssr;

/**
 * Container for common data in SSR messages header.
 * @author Bryan Cazabonne
 * @since 11.0
 */
public class SsrHeader {

    /** SSR Epoch Time 1s. */
    private double ssrEpoch1s;

    /** SSR Update Interval. */
    private int ssrUpdateInterval;

    /** SSR Multiple Message Indicator. */
    private int ssrMultipleMessageIndicator;

    /** IOD SSR. */
    private int iodSsr;

    /** SSR Provider ID. */
    private int ssrProviderId;

    /** SSR Solution ID. */
    private int ssrSolutionId;

    /** Empty constructor.
     * <p>
     * This constructor is not strictly necessary, but it prevents spurious
     * javadoc warnings with JDK 18 and later.
     * </p>
     * @since 12.0
     */
    public SsrHeader() {
        // nothing to do
    }

    /**
     * Get the SSR Epoch Time 1s.
     * <p>
     * Full seconds since the beginning of the week of continuous time scale
     * with no offset from GPS, Galileo, QZSS, SBAS,
     * UTC leap seconds from GLONASS,
     * -14 s offset from BDS
     * </p>
     * @return the SSR Epoch Time 1s in seconds
     */
    public double getSsrEpoch1s() {
        return ssrEpoch1s;
    }

    /**
     * Set the SSR Epoch Time 1s.
     * @param ssrEpoch1s the SSR Epoch Time 1s to set
     */
    public void setSsrEpoch1s(final double ssrEpoch1s) {
        this.ssrEpoch1s = ssrEpoch1s;
    }

    /**
     * Get the SSR Update Interval.
     * @return the SSR Update Interval in seconds
     */
    public int getSsrUpdateInterval() {
        return ssrUpdateInterval;
    }

    /**
     * Set the SSR Update Interval.
     * @param ssrUpdateInterval the SSR Update Interval to set
     */
    public void setSsrUpdateInterval(final int ssrUpdateInterval) {
        this.ssrUpdateInterval = ssrUpdateInterval;
    }

    /**
     * Get the SSR Multiple Message Indicator.
     * <p>
     * 0 - Last message of a sequence. 1 - Multiple message transmitted
     * </p>
     * @return the SSR Multiple Message Indicator
     */
    public int getSsrMultipleMessageIndicator() {
        return ssrMultipleMessageIndicator;
    }

    /**
     * Set the SSR Multiple Message Indicator.
     * @param ssrMultipleMessageIndicator the SSR Multiple Message Indicator to set
     */
    public void setSsrMultipleMessageIndicator(final int ssrMultipleMessageIndicator) {
        this.ssrMultipleMessageIndicator = ssrMultipleMessageIndicator;
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

}
