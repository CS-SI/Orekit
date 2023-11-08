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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.orekit.gnss.SatelliteSystem;
import org.orekit.gnss.metric.messages.rtcm.RtcmMessage;

/**
 * The RTCM Correction Message types provide elements
 * to calculate GNSS satellite corrections.
 * Corrections are orbit and clock corrections.
 *
 * @author Bryan Cazabonne
 * @since 12.0
 * @param <H> type of the header
 * @param <D> type of the data
 *
 */
public class RtcmCorrectionMessage<H extends RtcmCorrectionHeader, D extends RtcmCorrectionData> extends RtcmMessage<D> {

    /** Message header. */
    private final H header;

    /** Satellite system. */
    private final SatelliteSystem system;

    /**
     * Constructor.
     * @param system satellite system associated to the message
     * @param typeCode message number
     * @param header message header
     * @param data message data
     */
    public RtcmCorrectionMessage(final int typeCode, final SatelliteSystem system,
                                 final H header, final List<D> data) {
        super(typeCode, data);
        this.header = header;
        this.system = system;
    }

    /**
     * Get the header.
     * @return header
     */
    public H getHeader() {
        return header;
    }

    /**
     * Get the satellite system associated to the message.
     * @return the satellite system
     */
    public SatelliteSystem getSatelliteSystem() {
        return system;
    }

    /**
     * Get the all data parsed in the RTCM correction message.
     * <p>
     * Key: satellite identifier (e.g. "G01")
     * </p>
     * @return the all data for the parsed message
     */
    public Map<String, List<D>> getDataMap() {

        // Initialize map
        final Map<String, List<D>> data = new HashMap<>();

        // Loop on parsed data and fill map
        for (final D currentData : getData()) {
            final int satId = currentData.getSatelliteID();
            final String idString = satId < 10 ? "0" + String.valueOf(satId) : String.valueOf(satId);
            final String id = getSatelliteSystem().getKey() + idString;
            data.putIfAbsent(id, new ArrayList<>());
            data.get(id).add(currentData);
        }

        // Return an unmodifiable map of the parsed data
        return Collections.unmodifiableMap(data);

    }

}
