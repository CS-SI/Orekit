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

import java.util.List;

import org.orekit.gnss.SatelliteSystem;
import org.orekit.gnss.metric.messages.ssr.SsrMessage;

/**
 * The IGS Generic SSR Message types provide elements
 * to calculate GNSS satellite corrections.
 * Corrections are orbit and clock corrections, code and phase biases,
 * and the user range accuracy.
 *
 * @author Bryan Cazabonne
 * @since 11.0
 * @param <H> type of the header
 * @param <D> type of the data
 */
public class SsrIgmMessage<H extends SsrIgmHeader, D extends SsrIgmData> extends SsrMessage<H, D> {

    /** Satellite system. */
    private final SatelliteSystem system;

    /**
     * Constructor.
     * @param system satellite system associated to the message
     * @param typeCode message number
     * @param header message header
     * @param data message data
     */
    public SsrIgmMessage(final int typeCode, final SatelliteSystem system,
                         final H header, final List<D> data) {
        super(typeCode, header, data);
        this.system = system;
    }

    /**
     * Get the satellite system associated to the message.
     * @return the satellite system
     */
    public SatelliteSystem getSatelliteSystem() {
        return system;
    }

}
