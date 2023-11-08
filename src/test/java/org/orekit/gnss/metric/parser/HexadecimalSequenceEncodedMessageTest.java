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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

public class HexadecimalSequenceEncodedMessageTest extends AbstractEncodedMessageTest {

    protected EncodedMessage buildRawMessages(byte[] bytes) {
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < bytes.length; ++i) {
            int d = bytes[i] & 0xFF;
            builder.append(Character.forDigit(d >>> 4 & 0xF, 16));
            builder.append(Character.forDigit(d       & 0xF, 16));
        }
        return new HexadecimalSequenceEncodedMessage(builder);
    }

    @Test
    public void testString() {
        final EncodedMessage message = new HexadecimalSequenceEncodedMessage("0BFF3480AA");
        message.start();
        Assertions.assertEquals(0x0b,   message.extractBits(8));
        Assertions.assertEquals(0xff34, message.extractBits(16));
        Assertions.assertEquals(0x8,    message.extractBits(4));
        Assertions.assertEquals(0x0,    message.extractBits(4));
        Assertions.assertEquals(0xaa,   message.extractBits(8));
        try {
            message.extractBits(1);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.END_OF_ENCODED_MESSAGE, oe.getSpecifier());
        }
    }

}
