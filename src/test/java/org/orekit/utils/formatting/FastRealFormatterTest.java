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

/** This class implements a formatter for real numbers with low overhead.
 * <p>
 * This class is intended to be used when formatting large amounts of data
 * with fixed formats, like, for example, large ephemeris or measurement files.
 * Building the formatter is done once, and the formatter can be called
 * hundreds of thousands of times, without incurring the overhead that would
 * happen with {@code String.format()}.
 * </p>
 * @author Luc Maisonobe
 * @since 13.0.3
 */
public class FastRealFormatterTest {

    @Test
    public void testRegularPositive() {
        Assertions.assertEquals("  1",        new FastRealFormatter( 3,  0).toString( 1.2));
        Assertions.assertEquals("1.2",        new FastRealFormatter( 3,  1).toString( 1.2));
        Assertions.assertEquals(" 1.2",       new FastRealFormatter( 4,  1).toString( 1.2));
        Assertions.assertEquals("  1.2",      new FastRealFormatter( 5,  1).toString( 1.2));
        Assertions.assertEquals("  1.200",    new FastRealFormatter( 7,  3).toString( 1.2));
    }

    @Test
    public void testRegularNegative() {
        Assertions.assertEquals(" -1",         new FastRealFormatter(3, 0).toString(-1.2));
        Assertions.assertEquals("-1.2",        new FastRealFormatter(3, 1).toString(-1.2));
        Assertions.assertEquals("-1.2",        new FastRealFormatter(4, 1).toString(-1.2));
        Assertions.assertEquals(" -1.2",       new FastRealFormatter(5, 1).toString(-1.2));
        Assertions.assertEquals(" -1.200",     new FastRealFormatter(7, 3).toString(-1.2));
    }

    @Test
    public void testTrailingZeroPositive() {
        Assertions.assertEquals("17.0",       new FastRealFormatter( 4,  1).toString(17.0));
        Assertions.assertEquals("17.00",      new FastRealFormatter( 5,  2).toString(17.0));
        Assertions.assertEquals("17.000",     new FastRealFormatter( 6,  3).toString(17.0));
        Assertions.assertEquals("17.0000",    new FastRealFormatter( 7,  4).toString(17.0));
        Assertions.assertEquals("17.00000",   new FastRealFormatter( 8,  5).toString(17.0));
        Assertions.assertEquals("17.000000",  new FastRealFormatter( 9,  6).toString(17.0));
    }

    @Test
    public void testTrailingZeroNegative() {
        Assertions.assertEquals("-17.0",       new FastRealFormatter( 4,  1).toString(-17.0));
        Assertions.assertEquals("-17.00",      new FastRealFormatter( 5,  2).toString(-17.0));
        Assertions.assertEquals("-17.000",     new FastRealFormatter( 6,  3).toString(-17.0));
        Assertions.assertEquals("-17.0000",    new FastRealFormatter( 7,  4).toString(-17.0));
        Assertions.assertEquals("-17.00000",   new FastRealFormatter( 8,  5).toString(-17.0));
        Assertions.assertEquals("-17.000000",  new FastRealFormatter( 9,  6).toString(-17.0));
    }

    @Test
    public void testRoundingPositive() {
        Assertions.assertEquals(" 1.24",        new FastRealFormatter( 5,  2).toString( 1.2401));
        Assertions.assertEquals(" 1.24",        new FastRealFormatter( 5,  2).toString( 1.2399));
    }

    @Test
    public void testRoundingNegative() {
        Assertions.assertEquals("-1.24",        new FastRealFormatter( 5,  2).toString(-1.2401));
        Assertions.assertEquals("-1.24",        new FastRealFormatter( 5,  2).toString(-1.2399));
    }

    @Test
    public void testCarryPositive() {
        Assertions.assertEquals(" 2.00",        new FastRealFormatter( 5,  2).toString( 2.0001));
        Assertions.assertEquals(" 2.00",        new FastRealFormatter( 5,  2).toString( 1.9999));
    }

    @Test
    public void testCarryNegative() {
        Assertions.assertEquals("-2.00",        new FastRealFormatter( 5,  2).toString(-2.0001));
        Assertions.assertEquals("-2.00",        new FastRealFormatter( 5,  2).toString(-1.9999));
    }

    @Test
    public void testSpecialNumbers() {
        Assertions.assertEquals("       NaN", new FastRealFormatter(10,  1).toString(Double.NaN));
        Assertions.assertEquals("  Infinity", new FastRealFormatter(10,  1).toString(Double.POSITIVE_INFINITY));
        Assertions.assertEquals(" -Infinity", new FastRealFormatter(10,  1).toString(Double.NEGATIVE_INFINITY));
    }

    @Test
    public void testSignedZero() {
        Assertions.assertEquals(" 0.0", new FastRealFormatter(4,  1).toString(0.0));
        Assertions.assertEquals("-0.0", new FastRealFormatter(4,  1).toString(-0.0));
    }

}
