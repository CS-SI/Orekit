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
package org.orekit.gnss.metric.messages.ssr.igm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.orekit.gnss.SatelliteSystem;

/**
 * GNSS SSR Orbit Correction Message.
 * @author Bryan Cazabonne
 * @since 11.0
 */
public class SsrIgm01 extends SsrIgmMessage<SsrIgm01Header, SsrIgm01Data> {

    /**
     * Constructor.
     * @param typeCode message number
     * @param system satellite system
     * @param header message header
     * @param data message data
     */
    public SsrIgm01(final int typeCode, final SatelliteSystem system,
                    final SsrIgm01Header header, final List<SsrIgm01Data> data) {
        super(typeCode, system, header, data);
    }

    /**
     * Get the SSR IGM01 data parsed in the SSR message.
     * @return the SSR IGM01 data for the parsed message
     */
    public Map<String, List<SsrIgm01Data>> getSsrIgm01Data() {

        // Initialize map
        final Map<String, List<SsrIgm01Data>> ssrIgm01Data = new HashMap<>();

        // Loop on parsed data and fill map
        for (final SsrIgm01Data currentData : getData()) {
            final int ssrIgm01Id = currentData.getSatelliteID();
            final String ssrIgm01IdString = ssrIgm01Id < 10 ? "0" + String.valueOf(ssrIgm01Id) : String.valueOf(ssrIgm01Id);
            final String id = getSatelliteSystem().getKey() + ssrIgm01IdString;
            ssrIgm01Data.putIfAbsent(id, new ArrayList<>());
            ssrIgm01Data.get(id).add(currentData);
        }

        // Return an unmodifiable map of the parsed data
        return Collections.unmodifiableMap(ssrIgm01Data);

    }

}
