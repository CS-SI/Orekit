/* Copyright 2022-2025 Thales Alenia Space
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

public class FastLongFormatterTest {

    @Test
    public void testRegularPositive() {
        Assertions.assertEquals(   "3", new FastLongFormatter(1, false).toString(3));
        Assertions.assertEquals(   "3", new FastLongFormatter(1, true).toString(3));
        Assertions.assertEquals(  " 3", new FastLongFormatter(2, false).toString(3));
        Assertions.assertEquals(  "03", new FastLongFormatter(2, true).toString(3));
        Assertions.assertEquals( "  3", new FastLongFormatter(3, false).toString(3));
        Assertions.assertEquals( "003", new FastLongFormatter(3, true).toString(3));
        Assertions.assertEquals("   3", new FastLongFormatter(4, false).toString(3));
        Assertions.assertEquals("0003", new FastLongFormatter(4, true).toString(3));
    }

    @Test
    public void testRegularNegative() {
        Assertions.assertEquals(  "-3", new FastLongFormatter(1, false).toString(-3));
        Assertions.assertEquals(  "-3", new FastLongFormatter(1, true).toString(-3));
        Assertions.assertEquals(  "-3", new FastLongFormatter(2, false).toString(-3));
        Assertions.assertEquals(  "-3", new FastLongFormatter(2, true).toString(-3));
        Assertions.assertEquals( " -3", new FastLongFormatter(3, false).toString(-3));
        Assertions.assertEquals( "-03", new FastLongFormatter(3, true).toString(-3));
        Assertions.assertEquals("  -3", new FastLongFormatter(4, false).toString(-3));
        Assertions.assertEquals("-003", new FastLongFormatter(4, true).toString(-3));
    }

    @Test
    public void testSpecialNumbers() {
        Assertions.assertEquals(                   "0", new FastLongFormatter(1, false).toString(0));
        Assertions.assertEquals(                 "000", new FastLongFormatter(3, true).toString(0));
        Assertions.assertEquals(                 "  0", new FastLongFormatter(3, false).toString(0));
        Assertions.assertEquals("-9223372036854775808", new FastLongFormatter(1, false).toString(Long.MIN_VALUE));
        Assertions.assertEquals( "9223372036854775807", new FastLongFormatter(1, false).toString(Long.MAX_VALUE));
    }

    @Test
    public void testChainCalls() {
        Assertions.assertEquals(" 1 -02 04",
                                new StringBuilder().
                                        append(new FastLongFormatter(2, false).toString(1)).
                                        append(' ').
                                        append(new FastLongFormatter(3, true).toString(-2)).
                                        append(' ').
                                        append(new FastLongFormatter(2, true).toString(4)).
                                        toString());
    }

    @Test
    public void testGetters() {
        Assertions.assertEquals(4, new FastLongFormatter(4, true).getWidth());
        Assertions.assertTrue(new FastLongFormatter(4, true).hasZeroPadding());
        Assertions.assertFalse(new FastLongFormatter(4, false).hasZeroPadding());
    }

}
