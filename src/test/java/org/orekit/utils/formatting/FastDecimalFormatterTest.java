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

import org.hipparchus.util.Precision;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

import java.io.IOException;

public class FastDecimalFormatterTest {

    @Test
    public void testRegularPositive() {
        Assertions.assertEquals("  1",        new FastDecimalFormatter(3, 0).toString(1.2));
        Assertions.assertEquals("1.2",        new FastDecimalFormatter(3, 1).toString(1.2));
        Assertions.assertEquals(" 1.2",       new FastDecimalFormatter(4, 1).toString(1.2));
        Assertions.assertEquals("  1.2",      new FastDecimalFormatter(5, 1).toString(1.2));
        Assertions.assertEquals("  1.200",    new FastDecimalFormatter(7, 3).toString(1.2));
    }

    @Test
    public void testRegularNegative() {
        Assertions.assertEquals(" -1",         new FastDecimalFormatter(3, 0).toString(-1.2));
        Assertions.assertEquals("-1.2",        new FastDecimalFormatter(3, 1).toString(-1.2));
        Assertions.assertEquals("-1.2",        new FastDecimalFormatter(4, 1).toString(-1.2));
        Assertions.assertEquals(" -1.2",       new FastDecimalFormatter(5, 1).toString(-1.2));
        Assertions.assertEquals(" -1.200",     new FastDecimalFormatter(7, 3).toString(-1.2));
    }

    @Test
    public void testSmallPositive() {
        Assertions.assertEquals("   0",        new FastDecimalFormatter(4, 0).toString(0.0625));
        Assertions.assertEquals(" 0.1",        new FastDecimalFormatter(4, 1).toString(0.0625));
        Assertions.assertEquals(" 0.06",       new FastDecimalFormatter(5, 2).toString(0.0625));
        Assertions.assertEquals(" 0.063",      new FastDecimalFormatter(6, 3).toString(0.0625));
        Assertions.assertEquals(" 0.0625",     new FastDecimalFormatter(7, 4).toString(0.0625));
    }

   @Test
    public void testSmallNegative() {
        Assertions.assertEquals("  -0",        new FastDecimalFormatter(4, 0).toString(-0.0625));
        Assertions.assertEquals("-0.1",        new FastDecimalFormatter(4, 1).toString(-0.0625));
        Assertions.assertEquals("-0.06",       new FastDecimalFormatter(5, 2).toString(-0.0625));
        Assertions.assertEquals("-0.063",      new FastDecimalFormatter(6, 3).toString(-0.0625));
        Assertions.assertEquals("-0.0625",     new FastDecimalFormatter(7, 4).toString(-0.0625));
    }

    @Test
    public void testTrailingZeroPositive() {
        Assertions.assertEquals("17.0",       new FastDecimalFormatter(4, 1).toString(17.0));
        Assertions.assertEquals("17.00",      new FastDecimalFormatter(5, 2).toString(17.0));
        Assertions.assertEquals("17.000",     new FastDecimalFormatter(6, 3).toString(17.0));
        Assertions.assertEquals("17.0000",    new FastDecimalFormatter(7, 4).toString(17.0));
        Assertions.assertEquals("17.00000",   new FastDecimalFormatter(8, 5).toString(17.0));
        Assertions.assertEquals("17.000000",  new FastDecimalFormatter(9, 6).toString(17.0));
    }

    @Test
    public void testTrailingZeroNegative() {
        Assertions.assertEquals("-17.0",       new FastDecimalFormatter(4, 1).toString(-17.0));
        Assertions.assertEquals("-17.00",      new FastDecimalFormatter(5, 2).toString(-17.0));
        Assertions.assertEquals("-17.000",     new FastDecimalFormatter(6, 3).toString(-17.0));
        Assertions.assertEquals("-17.0000",    new FastDecimalFormatter(7, 4).toString(-17.0));
        Assertions.assertEquals("-17.00000",   new FastDecimalFormatter(8, 5).toString(-17.0));
        Assertions.assertEquals("-17.000000",  new FastDecimalFormatter(9, 6).toString(-17.0));
    }

    @Test
    public void testRoundingPositive() {
        Assertions.assertEquals(" 1.24",        new FastDecimalFormatter(5, 2).toString(1.2401));
        Assertions.assertEquals(" 1.24",        new FastDecimalFormatter(5, 2).toString(1.2399));
    }

    @Test
    public void testRoundingNegative() {
        Assertions.assertEquals("-1.24",        new FastDecimalFormatter(5, 2).toString(-1.2401));
        Assertions.assertEquals("-1.24",        new FastDecimalFormatter(5, 2).toString(-1.2399));
    }

    @Test
    public void testCarryPositive() {
        Assertions.assertEquals(" 2.00",        new FastDecimalFormatter(5, 2).toString(2.0001));
        Assertions.assertEquals(" 2.00",        new FastDecimalFormatter(5, 2).toString(1.9999));
    }

    @Test
    public void testCarryNegative() {
        Assertions.assertEquals("-2.00",        new FastDecimalFormatter(5, 2).toString(-2.0001));
        Assertions.assertEquals("-2.00",        new FastDecimalFormatter(5, 2).toString(-1.9999));
    }

    @Test
    public void testSpecialNumbers() {
        Assertions.assertEquals("       NaN", new FastDecimalFormatter(10, 1).toString(Double.NaN));
        Assertions.assertEquals("  Infinity", new FastDecimalFormatter(10, 1).toString(Double.POSITIVE_INFINITY));
        Assertions.assertEquals(" -Infinity", new FastDecimalFormatter(10, 1).toString(Double.NEGATIVE_INFINITY));
        Assertions.assertEquals("0.000",      new FastDecimalFormatter(5, 3).toString(Precision.SAFE_MIN));
    }

    @Test
    public void testSignedZero() {
        Assertions.assertEquals("  0.0", new FastDecimalFormatter(5, 1).toString(0.0));
        Assertions.assertEquals(" -0.0", new FastDecimalFormatter(5, 1).toString(-0.0));
    }

    @Test
    public void testChainCalls() throws IOException {
        final StringBuilder builder = new StringBuilder();
        new FastDecimalFormatter(5, 1).appendTo(builder, 1.2);
        builder.append('|');
        new FastDecimalFormatter(6, 3).appendTo(builder, -3.4);
        builder.append('|');
        new FastDecimalFormatter(3, 0).appendTo(builder, Double.NaN);
        Assertions.assertEquals("  1.2|-3.400|NaN", builder.toString());
    }

    @Test
    public void testGetters() {
        Assertions.assertEquals(4, new FastDecimalFormatter(4, 1).getWidth());
        Assertions.assertEquals(1, new FastDecimalFormatter(4, 1).getPrecision());
    }

    @Test
    public void testNegativeWidth() {
        doTestError(-4, 1);
    }

    @Test
    public void testTooLargeWidth() {
        doTestError(40, 1);
    }

    @Test
    public void testNegativePrecision() {
        doTestError(6, -1);
    }

    @Test
    public void testTooLargePrecision() {
        doTestError(4, 5);
    }

    private void doTestError(final int width, final int precision) {
        try {
            new FastDecimalFormatter(width, precision);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.INVALID_FORMAT, oe.getSpecifier());
            Assertions.assertEquals(width,                         oe.getParts()[0]);
            Assertions.assertEquals(precision,                     oe.getParts()[1]);
        }
    }

}
