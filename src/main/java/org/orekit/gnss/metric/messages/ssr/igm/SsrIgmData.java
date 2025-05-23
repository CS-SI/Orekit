/* Copyright 2002-2025 CS GROUP
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

import org.orekit.gnss.metric.messages.ssr.SsrData;

/**
 * Container for common data in IGS Generic SSR Message type.
 * @author Bryan Cazabonne
 * @since 11.0
 */
public class SsrIgmData extends SsrData {

    /** GNSS satellite ID. */
    private int satelliteID;

    /** Constructor. */
    public SsrIgmData() {
        // Nothing to do ...
    }

    /**
     * Get the satellite ID.
     * @return the satellite ID
     */
    public int getSatelliteID() {
        return satelliteID;
    }

    /**
     * Set the satellite ID.
     * @param satelliteID the ID to set
     */
    public void setSatelliteID(final int satelliteID) {
        this.satelliteID = satelliteID;
    }

}
