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
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

import java.io.IOException;

public class FastScientificFormatterTest {

    @Test
    public void testRegularPositive() {
        Assertions.assertEquals(" 0.0e+00",           new FastScientificFormatter(8).toString(0.0));
        Assertions.assertEquals(" 1.0e-03",           new FastScientificFormatter(8).toString(0.001));
        Assertions.assertEquals(" 1.23456e-53",       new FastScientificFormatter(12).toString(1.23456e-53));
        Assertions.assertEquals(" 1.23456789012e+06", new FastScientificFormatter(18).toString(1234567.8901234));
        Assertions.assertEquals(" 1.2345678901e+06",  new FastScientificFormatter(17).toString(1234567.8901234));
    }

    @Test
    public void testRegularNegative() {
        Assertions.assertEquals("-1.0e-03",           new FastScientificFormatter(8).toString(-0.001));
        Assertions.assertEquals("-1.23456e-53",       new FastScientificFormatter(12).toString(-1.23456e-53));
        Assertions.assertEquals("-1.23456789012e+06", new FastScientificFormatter(18).toString(-1234567.8901234));
        Assertions.assertEquals("-1.2345678901e+06",  new FastScientificFormatter(17).toString(-1234567.8901234));
    }

    @Test
    public void testSmallPositive() {
        Assertions.assertEquals(" 6.3e-02", new FastScientificFormatter(8).toString(0.0625));
        Assertions.assertEquals(" 6.3e-03", new FastScientificFormatter(8).toString(0.00625));
        Assertions.assertEquals(" 6.3e-04", new FastScientificFormatter(8).toString(0.000625));
        Assertions.assertEquals(" 6.3e-05", new FastScientificFormatter(8).toString(0.0000625));
        Assertions.assertEquals(" 6.3e-06", new FastScientificFormatter(8).toString(0.00000625));
    }

    @Test
    public void testSmallNegative() {
        Assertions.assertEquals("-6.3e-02", new FastScientificFormatter(8).toString(-0.0625));
        Assertions.assertEquals("-6.3e-03", new FastScientificFormatter(8).toString(-0.00625));
        Assertions.assertEquals("-6.3e-04", new FastScientificFormatter(8).toString(-0.000625));
        Assertions.assertEquals("-6.3e-05", new FastScientificFormatter(8).toString(-0.0000625));
        Assertions.assertEquals("-6.3e-06", new FastScientificFormatter(8).toString(-0.00000625));
    }

    @Test
    public void testTrailingZeroPositive() {
       Assertions.assertEquals(" 1.7e+01",       new FastScientificFormatter( 8).toString(17.0));
       Assertions.assertEquals(" 1.70e+01",      new FastScientificFormatter( 9).toString(17.0));
       Assertions.assertEquals(" 1.700e+01",     new FastScientificFormatter(10).toString(17.0));
       Assertions.assertEquals(" 1.7000e+01",    new FastScientificFormatter(11).toString(17.0));
       Assertions.assertEquals(" 1.70000e+01",   new FastScientificFormatter(12).toString(17.0));
       Assertions.assertEquals(" 1.700000e+01",  new FastScientificFormatter(13).toString(17.0));
       Assertions.assertEquals(" 1.7000000e+01", new FastScientificFormatter(14).toString(17.0));
    }

    @Test
    public void testTrailingZeroNegative() {
       Assertions.assertEquals("-1.7e+01",       new FastScientificFormatter( 8).toString(-17.0));
       Assertions.assertEquals("-1.70e+01",      new FastScientificFormatter( 9).toString(-17.0));
       Assertions.assertEquals("-1.700e+01",     new FastScientificFormatter(10).toString(-17.0));
       Assertions.assertEquals("-1.7000e+01",    new FastScientificFormatter(11).toString(-17.0));
       Assertions.assertEquals("-1.70000e+01",   new FastScientificFormatter(12).toString(-17.0));
       Assertions.assertEquals("-1.700000e+01",  new FastScientificFormatter(13).toString(-17.0));
       Assertions.assertEquals("-1.7000000e+01", new FastScientificFormatter(14).toString(-17.0));
    }

    @Test
    public void testRoundingPositive() {
        Assertions.assertEquals(" 1.25e+00", new FastScientificFormatter(9).toString(1.2450));
        Assertions.assertEquals(" 1.24e+00", new FastScientificFormatter(9).toString(1.2449));
        Assertions.assertEquals(" 1.24e+00", new FastScientificFormatter(9).toString(1.2399));
        Assertions.assertEquals(" 1.24e+00", new FastScientificFormatter(9).toString(1.2350));
        Assertions.assertEquals(" 1.23e+00", new FastScientificFormatter(9).toString(1.2349));
    }

    @Test
    public void testRoundingNegative() {
        Assertions.assertEquals("-1.25e+00", new FastScientificFormatter(9).toString(-1.2450));
        Assertions.assertEquals("-1.24e+00", new FastScientificFormatter(9).toString(-1.2449));
        Assertions.assertEquals("-1.24e+00", new FastScientificFormatter(9).toString(-1.2399));
        Assertions.assertEquals("-1.24e+00", new FastScientificFormatter(9).toString(-1.2350));
        Assertions.assertEquals("-1.23e+00", new FastScientificFormatter(9).toString(-1.2349));
    }

    @Test
    public void testCarryPositive() {
        Assertions.assertEquals(" 2.00e+10", new FastScientificFormatter(9).toString(20000000001.0));
        Assertions.assertEquals(" 2.00e+10", new FastScientificFormatter(9).toString(19999999999.0));
    }

    @Test
    public void testCarryNegative() {
        Assertions.assertEquals("-2.00e+10", new FastScientificFormatter(9).toString(-20000000001.0));
        Assertions.assertEquals("-2.00e+10", new FastScientificFormatter(9).toString(-19999999999.0));
    }

    @Test
    public void testSpecialNumbers() {
        Assertions.assertEquals("       NaN",  new FastScientificFormatter(10).toString(Double.NaN));
        Assertions.assertEquals("  Infinity",  new FastScientificFormatter(10).toString(Double.POSITIVE_INFINITY));
        Assertions.assertEquals(" -Infinity",  new FastScientificFormatter(10).toString(Double.NEGATIVE_INFINITY));
        Assertions.assertEquals("     NaN",    new FastScientificFormatter(8).toString(Double.NaN));
        Assertions.assertEquals("Infinity",    new FastScientificFormatter(8).toString(Double.POSITIVE_INFINITY));
        Assertions.assertEquals("-Infinity",   new FastScientificFormatter(8).toString(Double.NEGATIVE_INFINITY));
    }

    @Test
    public void testSubNormal() {
        Assertions.assertEquals(" 0.00000000e+00", new FastScientificFormatter(15).toString(0x1.0p-1074 / 2));
        Assertions.assertEquals(" 4.9406565e-324", new FastScientificFormatter(15).toString(0x1.0p-1074));
        Assertions.assertEquals(" 9.8813129e-324", new FastScientificFormatter(15).toString(0x1.0p-1073));
        Assertions.assertEquals(" 1.9762626e-323", new FastScientificFormatter(15).toString(0x1.0p-1072));
        Assertions.assertEquals(" 1.3262474e-315", new FastScientificFormatter(15).toString(0x1.0p-1046));
        Assertions.assertEquals(" 1.1125369e-308", new FastScientificFormatter(15).toString(0x1.0p-1023));
        Assertions.assertEquals(" 2.2250739e-308", new FastScientificFormatter(15).toString(0x1.0p-1022));
    }

    @Test
    public void testSignedZero() {
        Assertions.assertEquals(" 0.0e+00",  new FastScientificFormatter(8).toString(0.0));
        Assertions.assertEquals("-0.0e+00",  new FastScientificFormatter(8).toString(-0.0));
        Assertions.assertEquals(" 0.00e+00", new FastScientificFormatter(9).toString(0.0));
        Assertions.assertEquals("-0.00e+00", new FastScientificFormatter(9).toString(-0.0));
    }

    @Test
    public void testChainCalls() throws IOException {
        final StringBuilder builder = new StringBuilder();
        new FastScientificFormatter(10).appendTo(builder, 1.2);
        builder.append('|');
        new FastScientificFormatter(8).appendTo(builder, -0.00034);
        builder.append('|');
        new FastScientificFormatter(10).appendTo(builder, Double.NaN);
        Assertions.assertEquals(" 1.200e+00|-3.4e-04|       NaN", builder.toString());
    }

    @Test
    public void testGetter() {
        Assertions.assertEquals(10, new FastScientificFormatter(10).getWidth());
    }

    @Test
    public void testTooSmallWidthTwoDigitsExponents() {
        doTestTooSmallWidth(6, 4);
    }

    @Test
    public void testTooSmallWidthThreeDigitsExponents() {
        doTestTooSmallWidth(7, 5);
    }

    private void doTestTooSmallWidth(final int width, final int delta) {
        try {
            new FastScientificFormatter(width);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.INVALID_FORMAT, oe.getSpecifier());
            Assertions.assertEquals(width - delta,                 oe.getParts()[0]);
            Assertions.assertEquals(width - delta - 3,             oe.getParts()[1]);
        }
    }

    @Test
    public void testTooLargeWidth() {
        try {
            new FastScientificFormatter(40);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.INVALID_FORMAT, oe.getSpecifier());
            Assertions.assertEquals(36,                            oe.getParts()[0]);
            Assertions.assertEquals(33,                            oe.getParts()[1]);
        }
    }

}
