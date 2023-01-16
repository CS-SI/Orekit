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

/** SSR Update interval.
 * <p>
 * Using the indicator parsed in the RTCM message, this
 * class provides the SSR update interval in seconds.
 * </p>
 * @author Bryan Cazabonne
 * @since 12.0
 */
public class SsrUpdateInterval {

    /** SSR update interval indicator. */
    private final int indicator;

    /**
     * Constructor.
     * @param indicator indicator read in the RTCM message
     */
    public SsrUpdateInterval(final int indicator) {
        this.indicator = indicator;
    }

    /**
     * Get the update interval.
     * @return the update interval in seconds
     */
    public double getUpdateInterval() {
        switch (indicator) {
            case 0  : return 1.0;
            case 1  : return 2.0;
            case 2  : return 5.0;
            case 3  : return 10.0;
            case 4  : return 15.0;
            case 5  : return 30.0;
            case 6  : return 60.0;
            case 7  : return 120.0;
            case 8  : return 240.0;
            case 9  : return 300.0;
            case 10 : return 600.0;
            case 11 : return 900.0;
            case 12 : return 1800.0;
            case 13 : return 3600.0;
            case 14 : return 7200.0;
            default : return 10800.0;
        }
    }

}
