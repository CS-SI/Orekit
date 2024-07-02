/* Copyright 2002-2024 CS GROUP
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
 * Container for SSR IGM04 data.
 * @author Bryan Cazabonne
 * @since 11.0
 */
public class SsrIgm04Data extends SsrIgmData {

    /** High Rate Clock correction to be added to the polynomial clock correction. */
    private double highRateClockCorrection;

    /** Constructor. */
    public SsrIgm04Data() {
        // Nothing to do ...
    }

    /**
     * Get the high rate clock correction to be added to the polynomial clock correction.
     * @return the high rate clock correction in seconds
     */
    public double getHighRateClockCorrection() {
        return highRateClockCorrection;
    }

    /**
     * Set the high rate clock correction to be added to the polynomial clock correction.
     * @param highRateClockCorrection the high rate clock correction to set in seconds
     */
    public void setHighRateClockCorrection(final double highRateClockCorrection) {
        this.highRateClockCorrection = highRateClockCorrection;
    }

}
