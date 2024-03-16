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
package org.orekit.gnss.metric.messages.rtcm.correction;

import org.orekit.gnss.metric.messages.common.OrbitCorrection;

/**
 * Container for common data in RTCM orbit correction message type.
 * @author Bryan Cazabonne
 * @since 12.0
 */
public class RtcmOrbitCorrectionData extends RtcmCorrectionData {

    /** GNSS IOD. */
    private int gnssIod;

    /** Container for SSR orbit correction data. */
    private OrbitCorrection orbitCorrection;

    /** Constructor. */
    public RtcmOrbitCorrectionData() {
        // Nothing to do ...
    }

    /**
     * Get the GNSS IOD.
     * <p>
     * Users have to interpret the IOD value depending the
     * satellite system of the current message.
     * </p>
     * @return the GNSS IOD
     */
    public int getGnssIod() {
        return gnssIod;
    }

    /**
     * Set the GNSS IOD.
     * @param gnssIod the GNSS IOD to set
     */
    public void setGnssIod(final int gnssIod) {
        this.gnssIod = gnssIod;
    }

    /**
     * Get the orbit correction data.
     * @return the orbit correction data
     */
    public OrbitCorrection getOrbitCorrection() {
        return orbitCorrection;
    }

    /**
     * Set the orbit correction data.
     * @param orbitCorrection the data to set
     */
    public void setOrbitCorrection(final OrbitCorrection orbitCorrection) {
        this.orbitCorrection = orbitCorrection;
    }

}
