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
package org.orekit.gnss.metric.messages.ssr.subtype;

import java.util.List;

import org.orekit.gnss.metric.messages.ssr.SsrMessage;
import org.orekit.models.earth.ionosphere.SsrVtecIonosphericModel;

/**
 * SSR Ionosphere VTEC Spherical Harmonics Message.
 * @author Bryan Cazabonne
 * @since 11.0
 */
public class SsrIm201 extends SsrMessage<SsrIm201Header, SsrIm201Data> {

    /**
     * Constructor.
     * @param typeCode message number
     * @param header message header
     * @param data message data
     */
    public SsrIm201(final int typeCode, final SsrIm201Header header,
                    final List<SsrIm201Data> data) {
        super(typeCode, header, data);
    }

    /**
     * Get the ionospheric model adapted to the current IM201 message.
     * @return the ionospheric model
     */
    public SsrVtecIonosphericModel getIonosphericModel() {
        return new SsrVtecIonosphericModel(this);
    }

}
