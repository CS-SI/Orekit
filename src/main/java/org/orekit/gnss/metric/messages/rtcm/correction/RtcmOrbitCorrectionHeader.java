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

/**
 * Container for common data in RTCM Orbit Correction Message type header.
 * @author Bryan Cazabonne
 * @since 12.0
 */
public class RtcmOrbitCorrectionHeader extends RtcmCorrectionHeader {

    /** Satellite reference datum. */
    private int satelliteReferenceDatum;

    /** Constructor. */
    public RtcmOrbitCorrectionHeader() {
        // Nothing to do ...
    }

    /**
     * Get the satellite reference datum.
     * <p>
     * Orbit corrections refer to Satellite Reference Datum:
     * 0 - ITRF. 1 - Regional
     * </p>
     * @return the indicator of the satellite reference datum
     */
    public int getSatelliteReferenceDatum() {
        return satelliteReferenceDatum;
    }

    /**
     * Set the satellite reference datum.
     * @param satelliteReferenceDatum the satellite reference datum to set
     */
    public void setSatelliteReferenceDatum(final int satelliteReferenceDatum) {
        this.satelliteReferenceDatum = satelliteReferenceDatum;
    }

}
