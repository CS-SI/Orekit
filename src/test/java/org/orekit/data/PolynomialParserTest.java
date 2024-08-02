/* Copyright 2002-2024 CS GROUP
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
package org.orekit.data;

import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Method;
import java.util.Arrays;

class PolynomialParserTest {

    @Test
    void testEmptyData() {
        double[] coefficients =
                new PolynomialParser('x', PolynomialParser.Unit.NO_UNITS).parse("");
        assertNull(coefficients);
    }

    @Test
    void testConstant() {
        double[] coefficients =
                new PolynomialParser('x', PolynomialParser.Unit.NO_UNITS).parse("3");
        assertEquals(1, coefficients.length);
        assertEquals(3, coefficients[0], 1.0e-14);
    }

    @Test
    void testHighDegreeMonomialSuperscripts() {
        double[] coefficients =
                new PolynomialParser('\u03c4', PolynomialParser.Unit.NO_UNITS).parse(" −1234.567″ × τ⁴⁵");
        assertEquals(46, coefficients.length);
        assertEquals(FastMath.toRadians(-1234.567 / 3600), coefficients[45], 1.0e-14);
        for (int i = 0; i < coefficients.length - 1; ++i) {
            assertEquals(0.0, coefficients[i], 1.0e-14);
        }
    }

    @Test
    void testHighDegreeMonomialCaret() {
        double[] coefficients =
                new PolynomialParser('\u03c4', PolynomialParser.Unit.NO_UNITS).parse(" −1234.567″ × τ^45");
        assertEquals(46, coefficients.length);
        assertEquals(FastMath.toRadians(-1234.567 / 3600), coefficients[45], 1.0e-14);
        for (int i = 0; i < coefficients.length - 1; ++i) {
            assertEquals(0.0, coefficients[i], 1.0e-14);
        }
    }

    @Test
    void testDefaultRadians() {
        double[] coefficients =
                new PolynomialParser('a', PolynomialParser.Unit.RADIANS).parse("1 − 1 × a");
        assertEquals(2, coefficients.length);
        assertEquals(+1.0, coefficients[0], 1.0e-14);
        assertEquals(-1.0, coefficients[1], 1.0e-14);
    }

    @Test
    void testDefaultDegrees() {
        double[] coefficients =
                new PolynomialParser('a', PolynomialParser.Unit.DEGREES).parse("1 − 1 × a");
        assertEquals(2, coefficients.length);
        assertEquals(FastMath.toRadians(+1.0), coefficients[0], 1.0e-14);
        assertEquals(FastMath.toRadians(-1.0), coefficients[1], 1.0e-14);
    }

    @Test
    void testDefaultArcSeconds() {
        double[] coefficients =
                new PolynomialParser('a', PolynomialParser.Unit.ARC_SECONDS).parse("1 − 1 × a");
        assertEquals(2, coefficients.length);
        assertEquals(FastMath.toRadians(+1.0 / 3600.0), coefficients[0], 1.0e-14);
        assertEquals(FastMath.toRadians(-1.0 / 3600.0), coefficients[1], 1.0e-14);
    }

    @Test
    void testDefaultMilliArcSeconds() {
        double[] coefficients =
                new PolynomialParser('a', PolynomialParser.Unit.MILLI_ARC_SECONDS).parse("1 − 1 × a");
        assertEquals(2, coefficients.length);
        assertEquals(FastMath.toRadians(+1.0 / 3600000.0), coefficients[0], 1.0e-14);
        assertEquals(FastMath.toRadians(-1.0 / 3600000.0), coefficients[1], 1.0e-14);
    }

    @Test
    void testDefaultMicroArcSeconds() {
        double[] coefficients =
                new PolynomialParser('a', PolynomialParser.Unit.MICRO_ARC_SECONDS).parse("1 − 1 × a");
        assertEquals(2, coefficients.length);
        assertEquals(FastMath.toRadians(+1.0 / 3600000000.0), coefficients[0], 1.0e-14);
        assertEquals(FastMath.toRadians(-1.0 / 3600000000.0), coefficients[1], 1.0e-14);
    }

    @Test
    void testDefaultNoUnits() {
        double[] coefficients =
                new PolynomialParser('a', PolynomialParser.Unit.NO_UNITS).parse("1 − 1 × a");
        assertEquals(2, coefficients.length);
        assertEquals(+1.0, coefficients[0], 1.0e-14);
        assertEquals(-1.0, coefficients[1], 1.0e-14);
    }

    @Test
    void testExplicitEmbeddedUnits() {
        for (final PolynomialParser.Unit defaultUnit : PolynomialParser.Unit.values()) {
            double[] coefficients = new PolynomialParser('a', defaultUnit).parse("1°.25 − 1″.2 × a");
            assertEquals(2, coefficients.length);
            assertEquals(FastMath.toRadians(+1.25), coefficients[0], 1.0e-14);
            assertEquals(FastMath.toRadians(-1.2 / 3600.0), coefficients[1], 1.0e-14);
        }
    }

    @Test
    void testExplicitAppendedUnits() {
        for (final PolynomialParser.Unit defaultUnit : PolynomialParser.Unit.values()) {
            double[] coefficients = new PolynomialParser('a', defaultUnit).parse("1° − 1″ × a");
            assertEquals(2, coefficients.length);
            assertEquals(FastMath.toRadians(+1.0), coefficients[0], 1.0e-14);
            assertEquals(FastMath.toRadians(-1.0 / 3600.0), coefficients[1], 1.0e-14);
        }
    }

    @Test
    void testIERSData1() {
        PolynomialParser parser = new PolynomialParser('t', PolynomialParser.Unit.MICRO_ARC_SECONDS);
        double[] coefficients = parser.parse("125.04455501° − 6962890.5431″t + 7.4722″t² + 0.007702″t³ − 0.00005939″t⁴");
        assertEquals(5, coefficients.length);
        assertEquals(FastMath.toRadians(125.04455501), coefficients[0], 1.0e-14);
        assertEquals(FastMath.toRadians(-6962890.5431 / 3600), coefficients[1], 1.0e-14);
        assertEquals(FastMath.toRadians(7.4722 / 3600), coefficients[2], 1.0e-14);
        assertEquals(FastMath.toRadians(0.007702 / 3600), coefficients[3], 1.0e-14);
        assertEquals(FastMath.toRadians(-0.00005939 / 3600.0), coefficients[4], 1.0e-14);
    }

    @Test
    void testIERSData2() {
        PolynomialParser parser = new PolynomialParser('t', PolynomialParser.Unit.ARC_SECONDS);
        double[] coefficients = parser.parse("0.02438175 × t + 0.00000538691 × t²");
        assertEquals(3, coefficients.length);
        assertEquals(FastMath.toRadians(0), coefficients[0], 1.0e-14);
        assertEquals(FastMath.toRadians(0.02438175 / 3600), coefficients[1], 1.0e-14);
        assertEquals(FastMath.toRadians(0.00000538691 / 3600.0), coefficients[2], 1.0e-14);
    }

    @Test
    void testIERSData3() {
        PolynomialParser parser = new PolynomialParser('t', PolynomialParser.Unit.MICRO_ARC_SECONDS);
        double[] coefficients = parser.parse("0''.014506 + 4612''.15739966t + 1''.39667721t^2 - 0''.00009344t^3 + 0''.00001882t^4");
        assertEquals(5, coefficients.length);
        assertEquals(FastMath.toRadians(0.014506 / 3600), coefficients[0], 1.0e-14);
        assertEquals(FastMath.toRadians(4612.15739966 / 3600), coefficients[1], 1.0e-14);
        assertEquals(FastMath.toRadians(1.39667721 / 3600), coefficients[2], 1.0e-14);
        assertEquals(FastMath.toRadians(-0.00009344 / 3600), coefficients[3], 1.0e-14);
        assertEquals(FastMath.toRadians(0.00001882 / 3600.0), coefficients[4], 1.0e-14);
    }

    @Test
    void testIERSData4() {
        PolynomialParser parser = new PolynomialParser('t', PolynomialParser.Unit.MICRO_ARC_SECONDS);
        double[] coefficients = parser.parse("-16616.99 + 2004191742.88 t - 427219.05 t^2 - 198620.54 t^3 - 46.05 t^4 + 5.98 t^5");
        assertEquals(6, coefficients.length);
        assertEquals(FastMath.toRadians(-16616.99 / 3600000000.0), coefficients[0], 1.0e-14);
        assertEquals(FastMath.toRadians(2004191742.88 / 3600000000.0), coefficients[1], 1.0e-14);
        assertEquals(FastMath.toRadians(-427219.05 / 3600000000.0), coefficients[2], 1.0e-14);
        assertEquals(FastMath.toRadians(-198620.54 / 3600000000.0), coefficients[3], 1.0e-14);
        assertEquals(FastMath.toRadians(-46.05 / 3600000000.0), coefficients[4], 1.0e-14);
        assertEquals(FastMath.toRadians(5.98 / 3600000000.0), coefficients[5], 1.0e-14);
    }

    @Test
    void testEnum() throws NoSuchMethodException, SecurityException,
            IllegalAccessException, IllegalArgumentException,
            InvocationTargetException {
        Class<?> e = null;
        for (final Class<?> c : PolynomialParser.class.getDeclaredClasses()) {
            if (c.getName().endsWith("Unit")) {
                e = c;
            }
        }
        Method m = e.getDeclaredMethod("valueOf", String.class);
        m.setAccessible(true);
        for (String n : Arrays.asList("RADIANS", "DEGREES", "ARC_SECONDS",
                                      "MILLI_ARC_SECONDS", "MICRO_ARC_SECONDS", "NO_UNITS")) {
            assertEquals(n, m.invoke(null, n).toString());
        }
        try {
            m.invoke(null, "inexistent");
            fail("an exception should have been thrown");
        } catch (InvocationTargetException ite) {
            assertTrue(ite.getCause() instanceof IllegalArgumentException);
        }
    }

}
