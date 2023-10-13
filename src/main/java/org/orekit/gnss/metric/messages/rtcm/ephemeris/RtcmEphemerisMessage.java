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
package org.orekit.gnss.metric.messages.rtcm.ephemeris;

import java.util.Collections;

import org.orekit.gnss.metric.messages.rtcm.RtcmMessage;

/**
 * Base class for RTCM ephemeris messages.
 * @author Bryan Cazabonne
 * @since 11.0
 * @param <D> type of the data
 */
public class RtcmEphemerisMessage<D extends RtcmEphemerisData> extends RtcmMessage<D> {

    /**
     * Constructor.
     * @param typeCode message number
     * @param rtcmData message data
     */
    public RtcmEphemerisMessage(final int typeCode, final D rtcmData) {
        // Ephemeris messages contain only one entry
        super(typeCode, Collections.singletonList(rtcmData));
    }

    /**
     * Get the ephemeris data contain in the ephemeris message.
     * @return the ephemeris data contain in the ephemeris message
     */
    public D getEphemerisData() {
        // Ephemeris data message contain only one entry
        return getData().get(0);
    }

}
