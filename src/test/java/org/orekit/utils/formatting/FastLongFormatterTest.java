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
package org.orekit.utils.formatting;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

public class FastLongFormatterTest {

    @Test
    public void testRegularPositive() {
        Assertions.assertEquals(   "3", new FastLongFormatter(1, false, false).toString(3));
        Assertions.assertEquals(   "3", new FastLongFormatter(1, true, false).toString(3));
        Assertions.assertEquals(  " 3", new FastLongFormatter(2, false, false).toString(3));
        Assertions.assertEquals(  "03", new FastLongFormatter(2, true, false).toString(3));
        Assertions.assertEquals( "  3", new FastLongFormatter(3, false, false).toString(3));
        Assertions.assertEquals( "003", new FastLongFormatter(3, true, false).toString(3));
        Assertions.assertEquals("   3", new FastLongFormatter(4, false, false).toString(3));
        Assertions.assertEquals("0003", new FastLongFormatter(4, true, false).toString(3));
    }

    @Test
    public void testRegularNegative() {
        Assertions.assertEquals(  "-3", new FastLongFormatter(1, false, false).toString(-3));
        Assertions.assertEquals(  "-3", new FastLongFormatter(1, true, false).toString(-3));
        Assertions.assertEquals(  "-3", new FastLongFormatter(2, false, false).toString(-3));
        Assertions.assertEquals(  "-3", new FastLongFormatter(2, true, false).toString(-3));
        Assertions.assertEquals( " -3", new FastLongFormatter(3, false, false).toString(-3));
        Assertions.assertEquals( "-03", new FastLongFormatter(3, true, false).toString(-3));
        Assertions.assertEquals("  -3", new FastLongFormatter(4, false, false).toString(-3));
        Assertions.assertEquals("-003", new FastLongFormatter(4, true, false).toString(-3));
    }

    @Test
    public void testSpecialNumbers() {
        Assertions.assertEquals(                   "0", new FastLongFormatter(1, false, false).toString(0));
        Assertions.assertEquals(                 "000", new FastLongFormatter(3, true, false).toString(0));
        Assertions.assertEquals(                 "  0", new FastLongFormatter(3, false, false).toString(0));
        Assertions.assertEquals("-9223372036854775808", new FastLongFormatter(1, false, false).toString(Long.MIN_VALUE));
        Assertions.assertEquals( "9223372036854775807", new FastLongFormatter(1, false, false).toString(Long.MAX_VALUE));
    }

    @Test
    public void testChainCalls() {
        Assertions.assertEquals(" 1 -02 04",
                                new StringBuilder().
                                        append(new FastLongFormatter(2, false, false).toString(1)).
                                        append(' ').
                                        append(new FastLongFormatter(3, true, false).toString(-2)).
                                        append(' ').
                                        append(new FastLongFormatter(2, true, false).toString(4)).
                                        toString());
    }

    @Test
    public void testExceedWidth() {
        try {
            new FastLongFormatter(3, true, true).toString(-123);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException e) {
            Assertions.assertEquals(OrekitMessages.WIDTH_EXCEEDED, e.getSpecifier());
            Assertions.assertEquals("-123", e.getParts()[0]);
            Assertions.assertEquals(3,      (Integer) e.getParts()[1]);
        }
    }

    @Test
    public void testGetters() {
        Assertions.assertEquals(4, new FastLongFormatter(4, true, false).getWidth());
        Assertions.assertTrue(new FastLongFormatter(4, true, false).hasZeroPadding());
        Assertions.assertFalse(new FastLongFormatter(4, false, false).hasZeroPadding());
    }

}
