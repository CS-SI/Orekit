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
package org.orekit.gnss.metric.messages.common;

/**
 * Container for SSR clock correction data.
 * @author Bryan Cazabonne
 * @since 11.0
 */
public class ClockCorrection {

    /** Delta Clock C0. */
    private final double deltaClockC0;

    /** Delta Clock C1. */
    private final double deltaClockC1;

    /** Delta Clock C2. */
    private final double deltaClockC2;

    /**
     * Constructor.
     * @param c0 delta Clock C0
     * @param c1 delta Clock C1
     * @param c2 delta Clock C2
     */
    public ClockCorrection(final double c0, final double c1, final double c2) {
        this.deltaClockC0 = c0;
        this.deltaClockC1 = c1;
        this.deltaClockC2 = c2;
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
     * Get the delta clock C2.
     * <p>
     * The reference time t0 is SSR Epoch Time (IDF003) plus ½ SSR Update Interval.
     * </p>
     * @return the delta clock C2 in seconds
     */
    public double getDeltaClockC2() {
        return deltaClockC2;
    }

}
