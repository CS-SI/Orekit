/* Copyright 2002-2021 CS GROUP
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
package org.orekit.gnss.metric.messages.ssr.igm;

/**
 * Container for SSR IGM02 data.
 * @author Bryan Cazabonne
 * @since 11.0
 */
public class SsrIgm02Data extends SsrIgmData {

    /** Delta Clock C0. */
    private double deltaClockC0;

    /** Delta Clock C1. */
    private double deltaClockC1;

    /** Delta Clock C2. */
    private double deltaClockC2;

    /** Constructor. */
    public SsrIgm02Data() {
        // Nothing to do ...
    }

    /**
     * Get the delta clock C0.
     * <p>
     * The reference time t0 is SSR Epoch Time (IDF003) plus ½ SSR Update Interval.
     * </p>
     * @return the delta clock C0 in seconds
     */
    public double getDeltaClockC0() {
        return deltaClockC0;
    }

    /**
     * Set the delta clock C0.
     * @param deltaClockC0 the delta clock C0 to set in seconds
     */
    public void setDeltaClockC0(final double deltaClockC0) {
        this.deltaClockC0 = deltaClockC0;
    }

    /**
     * Get the delta clock C1.
     * <p>
     * The reference time t0 is SSR Epoch Time (IDF003) plus ½ SSR Update Interval.
     * </p>
     * @return the delta clock C1 in seconds
     */
    public double getDeltaClockC1() {
        return deltaClockC1;
    }

    /**
     * Set the delta clock C1.
     * @param deltaClockC1 the delta clock C1 to set in seconds
     */
    public void setDeltaClockC1(final double deltaClockC1) {
        this.deltaClockC1 = deltaClockC1;
    }

    /**
     * Get the delta clock C2.
     * <p>
     * The reference time t0 is SSR Epoch Time (IDF003) plus ½ SSR Update Interval.
     * </p>
     * @return the delta clock C2 in seconds
     */
    public double getDeltaClockC2() {
        return deltaClockC2;
    }

    /**
     * Set the delta clock C2.
     * @param deltaClockC2 the delta clock C2 to set in seconds
     */
    public void setDeltaClockC2(final double deltaClockC2) {
        this.deltaClockC2 = deltaClockC2;
    }


}
