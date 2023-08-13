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
 * GNSS SSR Phase Bias Message.
 * @author Bryan Cazabonne
 * @since 11.0
 */
public class SsrIgm06 extends SsrIgmMessage<SsrIgm06Header, SsrIgm06Data> {

    /**
     * Constructor.
     * @param typeCode message number
     * @param system satellite system
     * @param header message header
     * @param data message data
     */
    public SsrIgm06(final int typeCode, final SatelliteSystem system,
                    final SsrIgm06Header header, final List<SsrIgm06Data> data) {
        super(typeCode, system, header, data);
    }

    /**
     * Get the SSR IGM06 data parsed in the SSR message.
     * @return the SSR IGM06 data for the parsed message
     */
    public Map<String, List<SsrIgm06Data>> getSsrIgm06Data() {

        // Initialize map
        final Map<String, List<SsrIgm06Data>> ssrIgm06Data = new HashMap<>();

        // Loop on parsed data and fill map
        for (final SsrIgm06Data currentData : getData()) {
            final int ssrIgm06Id = currentData.getSatelliteID();
            final String ssrIgm06IdString = ssrIgm06Id < 10 ? "0" + String.valueOf(ssrIgm06Id) : String.valueOf(ssrIgm06Id);
            final String id = getSatelliteSystem().getKey() + ssrIgm06IdString;
            ssrIgm06Data.putIfAbsent(id, new ArrayList<>());
            ssrIgm06Data.get(id).add(currentData);
        }

        // Return an unmodifiable map of the parsed data
        return Collections.unmodifiableMap(ssrIgm06Data);

    }
}

