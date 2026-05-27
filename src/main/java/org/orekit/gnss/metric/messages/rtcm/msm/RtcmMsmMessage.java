/* Copyright 2022-2026 Thales Alenia Space
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

package org.orekit.gnss.metric.messages.rtcm.msm;

import java.util.List;

import org.orekit.gnss.metric.messages.rtcm.RtcmMessage;
import org.orekit.gnss.metric.messages.rtcm.msm.headers.RtcmMsmHeader;

/**
 * Generic container for RTCM MSM messages with a header and associated cell data.
 * @author Nathan Schiffmacher
 * @since 14.0
 */
public class RtcmMsmMessage<H extends RtcmMsmHeader> extends RtcmMessage<RtcmMsmCellData> {

    /** MSM message header. */
    private final H header;

    /**
     * Simple constructor.
     * @param typeCode RTCM message type code
     * @param header MSM message header
     * @param cells list of MSM cells associated with this message
     */
    public RtcmMsmMessage(final int typeCode, final H header, final List<RtcmMsmCellData> cells) {
        super(typeCode, cells);
        this.header = header;
    }

    /**
     * Get the MSM header.
     * @return header associated with this message
     */
    public H getHeader() {
        return this.header;
    }
}
