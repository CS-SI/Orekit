/* Copyright 2002-2024 CS GROUP
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
package org.orekit.gnss.metric.messages.ssr;

import java.util.Collections;
import java.util.List;

import org.orekit.gnss.metric.messages.ParsedMessage;

/**
 * Base class for SSR messages.
 *
 * @author Bryan Cazabonne
 * @since 11.0
 * @param <H> type of the header
 * @param <D> type of the data
 */
public class SsrMessage<H extends SsrHeader, D extends SsrData> extends ParsedMessage {

    /** Message header. */
    private final H header;

    /** Message data. */
    private final List<D> data;

    /**
     * Constructor.
     * @param typeCode message number
     * @param header message header
     * @param data message data
     */
    public SsrMessage(final int typeCode, final H header, final List<D> data) {
        super(typeCode);
        this.header = header;
        this.data   = data;
    }

    /**
     * Get the header.
     * @return header
     */
    public H getHeader() {
        return header;
    }

    /**
     * Get the data.
     * @return data
     */
    public List<D> getData() {
        return Collections.unmodifiableList(data);
    }

}
