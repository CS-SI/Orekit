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

import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Locale;

public class FastDoubleFormatterTest {

    @Test
    public void testRegularPositive() {
        Assertions.assertEquals("  1",        new FastDoubleFormatter(3, 0).toString(1.2));
        Assertions.assertEquals("1.2",        new FastDoubleFormatter(3, 1).toString(1.2));
        Assertions.assertEquals(" 1.2",       new FastDoubleFormatter(4, 1).toString(1.2));
        Assertions.assertEquals("  1.2",      new FastDoubleFormatter(5, 1).toString(1.2));
        Assertions.assertEquals("  1.200",    new FastDoubleFormatter(7, 3).toString(1.2));
    }

    @Test
    public void testRegularNegative() {
        Assertions.assertEquals(" -1",         new FastDoubleFormatter(3, 0).toString(-1.2));
        Assertions.assertEquals("-1.2",        new FastDoubleFormatter(3, 1).toString(-1.2));
        Assertions.assertEquals("-1.2",        new FastDoubleFormatter(4, 1).toString(-1.2));
        Assertions.assertEquals(" -1.2",       new FastDoubleFormatter(5, 1).toString(-1.2));
        Assertions.assertEquals(" -1.200",     new FastDoubleFormatter(7, 3).toString(-1.2));
    }

    @Test
    public void testSmallNegative() {
        Assertions.assertEquals("  -0",        new FastDoubleFormatter(4, 0).toString(-0.0625));
        Assertions.assertEquals("-0.1",        new FastDoubleFormatter(4, 1).toString(-0.0625));
        Assertions.assertEquals("-0.06",       new FastDoubleFormatter(5, 2).toString(-0.0625));
        Assertions.assertEquals("-0.063",      new FastDoubleFormatter(6, 3).toString(-0.0625));
        Assertions.assertEquals("-0.0625",     new FastDoubleFormatter(7, 4).toString(-0.0625));
    }

    @Test
    public void testTrailingZeroPositive() {
        Assertions.assertEquals("17.0",       new FastDoubleFormatter(4, 1).toString(17.0));
        Assertions.assertEquals("17.00",      new FastDoubleFormatter(5, 2).toString(17.0));
        Assertions.assertEquals("17.000",     new FastDoubleFormatter(6, 3).toString(17.0));
        Assertions.assertEquals("17.0000",    new FastDoubleFormatter(7, 4).toString(17.0));
        Assertions.assertEquals("17.00000",   new FastDoubleFormatter(8, 5).toString(17.0));
        Assertions.assertEquals("17.000000",  new FastDoubleFormatter(9, 6).toString(17.0));
    }

    @Test
    public void testTrailingZeroNegative() {
        Assertions.assertEquals("-17.0",       new FastDoubleFormatter(4, 1).toString(-17.0));
        Assertions.assertEquals("-17.00",      new FastDoubleFormatter(5, 2).toString(-17.0));
        Assertions.assertEquals("-17.000",     new FastDoubleFormatter(6, 3).toString(-17.0));
        Assertions.assertEquals("-17.0000",    new FastDoubleFormatter(7, 4).toString(-17.0));
        Assertions.assertEquals("-17.00000",   new FastDoubleFormatter(8, 5).toString(-17.0));
        Assertions.assertEquals("-17.000000",  new FastDoubleFormatter(9, 6).toString(-17.0));
    }

    @Test
    public void testRoundingPositive() {
        Assertions.assertEquals(" 1.24",        new FastDoubleFormatter(5, 2).toString(1.2401));
        Assertions.assertEquals(" 1.24",        new FastDoubleFormatter(5, 2).toString(1.2399));
    }

    @Test
    public void testRoundingNegative() {
        Assertions.assertEquals("-1.24",        new FastDoubleFormatter(5, 2).toString(-1.2401));
        Assertions.assertEquals("-1.24",        new FastDoubleFormatter(5, 2).toString(-1.2399));
    }

    @Test
    public void testCarryPositive() {
        Assertions.assertEquals(" 2.00",        new FastDoubleFormatter(5, 2).toString(2.0001));
        Assertions.assertEquals(" 2.00",        new FastDoubleFormatter(5, 2).toString(1.9999));
    }

    @Test
    public void testCarryNegative() {
        Assertions.assertEquals("-2.00",        new FastDoubleFormatter(5, 2).toString(-2.0001));
        Assertions.assertEquals("-2.00",        new FastDoubleFormatter(5, 2).toString(-1.9999));
    }

    @Test
    public void testSpecialNumbers() {
        Assertions.assertEquals("       NaN", new FastDoubleFormatter(10, 1).toString(Double.NaN));
        Assertions.assertEquals("  Infinity", new FastDoubleFormatter(10, 1).toString(Double.POSITIVE_INFINITY));
        Assertions.assertEquals(" -Infinity", new FastDoubleFormatter(10, 1).toString(Double.NEGATIVE_INFINITY));
    }

    @Test
    public void testSignedZero() {
        Assertions.assertEquals("  0.0", new FastDoubleFormatter(5, 1).toString(0.0));
        Assertions.assertEquals(" -0.0", new FastDoubleFormatter(5, 1).toString(-0.0));
    }

    @Test
    public void testChainCalls() throws IOException {
        final StringBuilder builder = new StringBuilder();
        new FastDoubleFormatter(5, 1).appendTo(builder, 1.2);
        builder.append(' ');
        new FastDoubleFormatter(6, 3).appendTo(builder, -3.4);
        builder.append(' ');
        new FastDoubleFormatter(3, 0).appendTo(builder, Double.NaN);
        Assertions.assertEquals("  1.2 -3.400 NaN", builder.toString());
    }

    @Test
    public void testGetters() {
        Assertions.assertEquals(4, new FastDoubleFormatter(4, 1).getWidth());
        Assertions.assertEquals(1, new FastDoubleFormatter(4, 1).getPrecision());
    }

}
