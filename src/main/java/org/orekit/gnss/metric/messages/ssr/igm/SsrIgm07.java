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
 * GNSS SSR SSR URA Message.
 * @author Bryan Cazabonne
 * @since 11.0
 */
public class SsrIgm07 extends SsrIgmMessage<SsrIgm07Header, SsrIgm07Data> {

    /**
     * Constructor.
     * @param typeCode message number
     * @param system satellite system
     * @param header message header
     * @param data message data
     */
    public SsrIgm07(final int typeCode, final SatelliteSystem system,
                    final SsrIgm07Header header, final List<SsrIgm07Data> data) {
        super(typeCode, system, header, data);
    }

    /**
     * Get the SSR IGM07 data parsed in the SSR message.
     * @return the SSR IGM07 data for the parsed message
     */
    public Map<String, List<SsrIgm07Data>> getSsrIgm07Data() {

        // Initialize map
        final Map<String, List<SsrIgm07Data>> ssrIgm07Data = new HashMap<>();

        // Loop on parsed data and fill map
        for (final SsrIgm07Data currentData : getData()) {
            final int ssrIgm07Id = currentData.getSatelliteID();
            final String ssrIgm07IdString = ssrIgm07Id < 10 ? "0" + String.valueOf(ssrIgm07Id) : String.valueOf(ssrIgm07Id);
            final String id = getSatelliteSystem().getKey() + ssrIgm07IdString;
            ssrIgm07Data.putIfAbsent(id, new ArrayList<>());
            ssrIgm07Data.get(id).add(currentData);
        }

        // Return an unmodifiable map of the parsed data
        return Collections.unmodifiableMap(ssrIgm07Data);

    }

}

