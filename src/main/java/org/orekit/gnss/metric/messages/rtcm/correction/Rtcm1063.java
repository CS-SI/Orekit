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

import java.util.List;

import org.orekit.gnss.SatelliteSystem;

/**
 * RTCM 1063 message: GLONASS Orbit Correction Message.
 * @author Bryan Cazabonne
 * @since 12.0
 */
public class Rtcm1063 extends RtcmCorrectionMessage<RtcmOrbitCorrectionHeader, RtcmOrbitCorrectionData> {

    /**
     * Constructor.
     * @param typeCode message number
     * @param header message header
     * @param data message data
     */
    public Rtcm1063(final int typeCode, final RtcmOrbitCorrectionHeader header,
                    final List<RtcmOrbitCorrectionData> data) {
        super(typeCode, SatelliteSystem.GLONASS, header, data);
    }

}
