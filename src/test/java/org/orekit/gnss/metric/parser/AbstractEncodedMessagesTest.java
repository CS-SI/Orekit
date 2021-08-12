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

import java.util.Random;

import org.junit.Assert;
import org.junit.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

public abstract class AbstractEncodedMessagesTest {

    protected abstract EncodedMessage buildRawMessages(byte[] bytes);

    private EncodedMessage buildAndStart(byte[] bytes) {
        final EncodedMessage m = buildRawMessages(bytes);
        m.start();
        return m;
    }

    @Test
    public void testTooLargeDataType() {
        try {
            buildAndStart(new byte[] { 0, 1, 2, 3, 4, 5}).extractBits(64);
            Assert.fail("an exception should habe been thrown");
        } catch (OrekitException re) {
            Assert.assertEquals(OrekitMessages.TOO_LARGE_DATA_TYPE, re.getSpecifier());
            Assert.assertEquals(64, ((Integer) re.getParts()[0]).intValue());
        }
    }

    @Test
    public void testUnexpectedNoData() {
        try {
            buildAndStart(new byte[0]).extractBits(1);
            Assert.fail("an exception should habe been thrown");
        } catch (OrekitException re) {
            Assert.assertEquals(OrekitMessages.END_OF_ENCODED_MESSAGE, re.getSpecifier());
        }
    }

    @Test
    public void testZeroBits() {
        Assert.assertEquals(0, buildAndStart(new byte[0]).extractBits(0));
    }

    @Test
    public void testFirstByte() {
        final String s = "11111111";
        Assert.assertEquals(0x01, buildAndStart(byteArrayFromBinary(s)).extractBits(1));
        Assert.assertEquals(0x03, buildAndStart(byteArrayFromBinary(s)).extractBits(2));
        Assert.assertEquals(0x07, buildAndStart(byteArrayFromBinary(s)).extractBits(3));
        Assert.assertEquals(0x0F, buildAndStart(byteArrayFromBinary(s)).extractBits(4));
        Assert.assertEquals(0x1F, buildAndStart(byteArrayFromBinary(s)).extractBits(5));
        Assert.assertEquals(0x3F, buildAndStart(byteArrayFromBinary(s)).extractBits(6));
        Assert.assertEquals(0x7F, buildAndStart(byteArrayFromBinary(s)).extractBits(7));
        Assert.assertEquals(0xFF, buildAndStart(byteArrayFromBinary(s)).extractBits(8));
    }

    @Test
    public void testExhaustAfterInitialSuccess() {
        EncodedMessage m = buildAndStart(byteArrayFromBinary("10111011"));
        Assert.assertEquals(5, m.extractBits(3));
        Assert.assertEquals(3, m.extractBits(2));
        try {
            m.extractBits(4);
            Assert.fail("an exception should habe been thrown");
        } catch (OrekitException re) {
            Assert.assertEquals(OrekitMessages.END_OF_ENCODED_MESSAGE, re.getSpecifier());
        }
    }

    @Test
    public void testCrossingByte() {
        EncodedMessage m = buildAndStart(byteArrayFromBinary("0100110101101011"));
        Assert.assertEquals(0x09, m.extractBits(5));
        Assert.assertEquals(0x2B, m.extractBits(6));
        Assert.assertEquals(0x0B, m.extractBits(5));
    }

    @Test
    public void testLargeType() {
        EncodedMessage m = buildAndStart(byteArrayFromBinary("01001101011010110100110101101011"));
        Assert.assertEquals(0x4D6B4D6B, m.extractBits(32));
    }

    @Test
    public void testRandom() {
        Random random = new Random(0x9454c64b36d9b1b1l);
        for (int i = 0; i < 1000; ++i) {

            // generate a random byte array, and the corresponding bits array
            int nbBytes = 1 + random.nextInt(2000);
            byte[] bits = new byte[nbBytes * 8];
            for (int k = 0; k < bits.length; ++k) {
                bits[k] = (byte) (random.nextBoolean() ? 1 : 0);
            }
            byte[] array = new byte[nbBytes];
            for (int k = 0; k < array.length; ++k) {
                array[k] = (byte) ((bits[8 * k    ] << 7) |
                                   (bits[8 * k + 1] << 6) |
                                   (bits[8 * k + 2] << 5) |
                                   (bits[8 * k + 3] << 4) |
                                   (bits[8 * k + 4] << 3) |
                                   (bits[8 * k + 5] << 2) |
                                   (bits[8 * k + 6] << 1) |
                                   (bits[8 * k + 7]));
            }

            EncodedMessage m = buildAndStart(array);

            int index = 0;
            int size  = 0;
            for (int remaining = bits.length; remaining > 0; remaining -= size) {
                size = Math.min(remaining, 1 + random.nextInt(63));
                long ref = 0l;
                for (int k = 0; k < size; ++k) {
                    ref = (ref << 1) | bits[index++];
                }
                Assert.assertEquals(ref, m.extractBits(size));
            }
            try {
                m.extractBits(1);
                Assert.fail("an exception should have been thrown");
            } catch (OrekitException me) {
                Assert.assertEquals(OrekitMessages.END_OF_ENCODED_MESSAGE, me.getSpecifier());
            }

        }

    }

    private byte[] byteArrayFromBinary(String radix2Value) {
        final byte[] array = new byte[radix2Value.length() / 8];
        for (int i = 0; i < array.length; ++i) {
            for (int j = 0; j < 8; ++j) {
                if (radix2Value.charAt(8 * i + j) != '0') {
                    array[i] |= 0x1 << (7 - j);
                }
            }
        }
        return array;
    }

}
