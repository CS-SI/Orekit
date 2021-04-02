/* Copyright 2002-2021 CS GROUP
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
package org.orekit.gnss.metric.parser;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

/** Encoded messages as a sequence of bytes.
 * @author Luc Maisonobe
 * @since 11.0
 */
public abstract class AbstractEncodedMessages implements EncodedMessage {

    /** Current byte (as an int). */
    private int current;

    /** Remaining bits in current byte. */
    private int remaining;

    /** {@inheritDoc} */
    @Override
    public void start() {
        this.remaining  = 0;
    }

    /** Fetch the next byte from the message.
     * @return next byte from the message, as a primitive integer,
     * or -1 if end of data has been reached
     */
    protected abstract int fetchByte();

    /** {@inheritDoc} */
    @Override
    public long extractBits(final int n) {

        // safety check
        if (n > 63) {
            throw new OrekitException(OrekitMessages.TOO_LARGE_DATA_TYPE, n);
        }

        // initialization
        long value = 0l;

        // bits gathering loop
        int needed = n;
        while (needed > 0) {

            if (remaining == 0) {
                // we need to fetch one more byte
                final int read = fetchByte();
                if (read == -1) {
                    // end was unexpected
                    throw new OrekitException(OrekitMessages.END_OF_ENCODED_MESSAGE);
                }
                current   = read & 0xFF;
                remaining = 8;
            }

            final int nbBits = Math.min(remaining, needed);
            value      = (value << nbBits) | (current >>> (8 - nbBits));
            current    = (current << nbBits) & 0xFF;
            remaining -= nbBits;
            needed    -= nbBits;

        }

        return value;

    }

}
