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
package org.orekit.gnss.metric.parser;

import java.io.IOException;
import java.io.InputStream;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

/** Encoded message from an input stream.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class InputStreamEncodedMessage extends AbstractEncodedMessage {

    /** Input stream providing the message. */
    private final InputStream stream;

    /** Simple constructor.
     * @param stream input stream providing the message
     */
    public InputStreamEncodedMessage(final InputStream stream) {
        this.stream = stream;
    }

    /** {@inheritDoc} */
    @Override
    protected int fetchByte() {
        try {
            return stream.read();
        } catch (IOException ioe) {
            throw new OrekitException(ioe, OrekitMessages.END_OF_ENCODED_MESSAGE);
        }
    }

}
