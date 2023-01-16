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

public class EncodedMessagesTest {

    @Test
    public void testBasicUse() {

        // Initialization
        final MockEncodedMessages message = new MockEncodedMessages();
        message.start();

        // Verify
        Assertions.assertEquals(1, DataType.BIT_1.decode(message).intValue());
        Assertions.assertEquals(2, DataType.BIT_2.decode(message).intValue());
        Assertions.assertEquals(3, DataType.BIT_3.decode(message).intValue());
        Assertions.assertEquals(4, DataType.BIT_4.decode(message).intValue());
    }

    private class MockEncodedMessages implements EncodedMessage {

        @Override
        public long extractBits(int n) {
            return n;
        }

    }

}
