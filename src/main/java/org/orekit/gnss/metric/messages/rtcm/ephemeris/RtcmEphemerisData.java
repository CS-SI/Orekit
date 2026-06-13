/* Copyright 2002-2026 CS GROUP
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
package org.orekit.gnss.metric.messages.rtcm.ephemeris;

import org.orekit.gnss.metric.messages.common.AccuracyProvider;
import org.orekit.gnss.metric.messages.rtcm.RtcmData;

/**
 * Container for common data in RTCM ephemeris message type.
 * @author Bryan Cazabonne
 * @since 11.0
 */
public class RtcmEphemerisData extends RtcmData {

    /** Satellite ID. */
    private final int satelliteId;

    /** Accuracy provider. */
    private AccuracyProvider accuracyProvider;

    /** Constructor.
     * @param satelliteId satellite ID
     * @since 14.0
     */
    public RtcmEphemerisData(final int satelliteId) {
        this.satelliteId      = satelliteId;
    }

    /** Get the satellite ID.
     * @return the satellite ID
     */
    public int getSatelliteID() {
        return satelliteId;
    }

    /** Set the accuracy provider of the ephemeris message.
     * @param accuracyProvider accuracy provider
     */
    public void setAccuracyProvider(final AccuracyProvider accuracyProvider) {
        this.accuracyProvider = accuracyProvider;
    }

    /** Get the accuracy provider of the ephemeris message.
     * @return the accuracy provider
     */
    public AccuracyProvider getAccuracyProvider() {
        return accuracyProvider;
    }

}
