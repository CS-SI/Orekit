/* Copyright 2002-2026 CS GROUP
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

import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class RtcmDataFieldTest {

    @ParameterizedTest
    @MethodSource("df106Cases")
    void testDF106(final String bits, final int expectedSeconds) {
        ByteArrayEncodedMessage message = new ByteArrayEncodedMessage(byteArrayFromBinary(bits));
        message.start();
        Assertions.assertEquals(expectedSeconds, RtcmDataField.DF106.intValue(message));
    }

    static Stream<Arguments> df106Cases() {
        return Stream.of(
            Arguments.of("00000000",    0),
            Arguments.of("01000000", 1800),
            Arguments.of("10000000", 2700),
            Arguments.of("11000000", 3600)
        );
    }

    private byte[] byteArrayFromBinary(String radix2Value) {
        final byte[] array = new byte[radix2Value.length() / 8];
        for (int i = 0; i < array.length; ++i) {
            for (int j = 0; j < 8; ++j) {
                if (radix2Value.charAt(8 * i + j) != '0') {
                    array[i] |= (byte) (0x1 << (7 - j));
                }
            }
        }
        return array;
    }

}
