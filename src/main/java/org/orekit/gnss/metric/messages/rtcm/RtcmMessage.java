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
package org.orekit.gnss.metric.messages.rtcm;

import java.util.Collections;
import java.util.List;

import org.orekit.gnss.metric.messages.ParsedMessage;

/**
 * Base class for RTCM messages.
 *
 * @author Bryan Cazabonne
 * @since 11.0
 *
 * @param <D> type of the data
 */
public class RtcmMessage<D extends RtcmData> extends ParsedMessage {

    /** Message data. */
    private final List<D> rtcmData;

    /**
     * Constructor.
     * @param typeCode message number
     * @param rtcmData message data
     */
    public RtcmMessage(final int typeCode, final List<D> rtcmData) {
        super(typeCode);
        this.rtcmData = rtcmData;
    }

    /**
     * Get the data.
     * @return data
     */
    public List<D> getData() {
        return Collections.unmodifiableList(rtcmData);
    }

}
