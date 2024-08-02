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
package org.orekit.utils.units;

import java.util.Arrays;
import java.util.Collections;

import org.hipparchus.fraction.Fraction;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.utils.Constants;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Unit tests for {@link Lexer}.
 *
 * @author Luc Maisonobe
 */
class UnitTest {

    @Test
    void testTime() {
        assertEquals(       1.0, Unit.SECOND.toSI(1.0), 1.0e-10);
        assertEquals(      60.0, Unit.MINUTE.toSI(1.0), 1.0e-10);
        assertEquals(    3600.0, Unit.HOUR.toSI(1.0),   1.0e-10);
        assertEquals(   86400.0, Unit.DAY.toSI(1.0),    1.0e-10);
        assertEquals(31557600.0, Unit.YEAR.toSI(1.0),   1.0e-10);
        assertEquals(31557600.0, Unit.YEAR.toSI(Double.valueOf(1.0)),   1.0e-10);
        assertEquals(31557600.0, Unit.YEAR.toSI(new Binary64(1.0)).getReal(),   1.0e-10);
        assertEquals(1.0,        Unit.SECOND.fromSI(     1.0), 1.0e-10);
        assertEquals(1.0,        Unit.MINUTE.fromSI(    60.0), 1.0e-10);
        assertEquals(1.0,        Unit.HOUR.fromSI(    3600.0), 1.0e-10);
        assertEquals(1.0,        Unit.DAY.fromSI(    86400.0), 1.0e-10);
        assertEquals(1.0,        Unit.YEAR.fromSI(31557600.0), 1.0e-10);
        assertEquals(1.0,        Unit.YEAR.fromSI(Double.valueOf(31557600.0)), 1.0e-10);
        assertEquals(1.0,        Unit.YEAR.fromSI(new Binary64(31557600.0)).getReal(), 1.0e-10);
        assertEquals(365.25,     Unit.DAY.fromSI(Unit.YEAR.toSI(1.0)), 1.0e-10);
    }

    @Test
    void testSI() {
        assertTrue(Unit.PERCENT.sameDimensionSI().sameDimension(Unit.ONE));
        assertEquals("1", Unit.PERCENT.sameDimensionSI().getName());
        assertEquals("m³.s⁻²", Unit.parse("km**3/s**2").sameDimensionSI().getName());
        assertEquals("m⁻³.s⁻⁶.rad^(2/5)", Unit.parse("µas^(2/5)/(h**(2)×m)³").sameDimensionSI().getName());

    }

    @Test
    void testEquals() {
        final Unit u1 = Unit.parse("kg/m³");
        final Unit u2 = Unit.parse("kg/m^3");
        assertNotSame(u1, u2);
        assertEquals(u1, u2);
        assertNotEquals(u1.getName(), u2.getName());
        assertNotEquals(u1, Unit.parse("A").alias(u1.getName()));
        assertNotEquals(null, u1);
        assertNotEquals(u1, u1.getName());
        assertEquals(19160943, u1.hashCode());
    }

    @Test
    void testReference() {
        checkReference(Unit.NONE,                         "n/a",                     1.0,  0,  0,  0,  0, 0);
        checkReference(Unit.ONE,                            "1",                     1.0,  0,  0,  0,  0, 0);
        checkReference(Unit.PERCENT,                        "%",                    0.01,  0,  0,  0,  0, 0);
        checkReference(Unit.SECOND,                         "s",                     1.0,  0,  0,  1,  0, 0);
        checkReference(Unit.MINUTE,                       "min",                    60.0,  0,  0,  1,  0, 0);
        checkReference(Unit.HOUR,                           "h",                  3600.0,  0,  0,  1,  0, 0);
        checkReference(Unit.DAY,                            "d",                 86400.0,  0,  0,  1,  0, 0);
        checkReference(Unit.YEAR,                           "a",              31557600.0,  0,  0,  1,  0, 0);
        checkReference(Unit.HERTZ,                         "Hz",                     1.0,  0,  0, -1,  0, 0);
        checkReference(Unit.METRE,                          "m",                     1.0,  0,  1,  0,  0, 0);
        checkReference(Unit.KILOMETRE,                     "km",                  1000.0,  0,  1,  0,  0, 0);
        checkReference(Unit.KILOGRAM,                      "kg",                     1.0,  1,  0,  0,  0, 0);
        checkReference(Unit.GRAM,                           "g",                   0.001,  1,  0,  0,  0, 0);
        checkReference(Unit.AMPERE,                         "A",                     1.0,  0,  0,  0,  1, 0);
        checkReference(Unit.RADIAN,                       "rad",                     1.0,  0,  0,  0,  0, 1);
        checkReference(Unit.DEGREE,                         "°",  FastMath.PI /    180.0,  0,  0,  0,  0, 1);
        checkReference(Unit.ARC_MINUTE,                     "′",  FastMath.PI /  10800.0,  0,  0,  0,  0, 1);
        checkReference(Unit.ARC_SECOND,                     "″",  FastMath.PI / 648000.0,  0,  0,  0,  0, 1);
        checkReference(Unit.REVOLUTION,                   "rev",       2.0 * FastMath.PI,  0,  0,  0,  0, 1);
        checkReference(Unit.NEWTON,                         "N",                     1.0,  1,  1, -2,  0, 0);
        checkReference(Unit.PASCAL,                        "Pa",                     1.0,  1, -1, -2,  0, 0);
        checkReference(Unit.BAR,                          "bar",                100000.0,  1, -1, -2,  0, 0);
        checkReference(Unit.JOULE,                          "J",                     1.0,  1,  2, -2,  0, 0);
        checkReference(Unit.WATT,                           "W",                     1.0,  1,  2, -3,  0, 0);
        checkReference(Unit.COULOMB,                        "C",                     1.0,  0,  0,  1,  1, 0);
        checkReference(Unit.VOLT,                           "V",                     1.0,  1,  2, -3, -1, 0);
        checkReference(Unit.OHM,                            "Ω",                     1.0,  1,  2, -3, -2, 0);
        checkReference(Unit.TESLA,                          "T",                     1.0,  1,  0, -2, -1, 0);
        checkReference(Unit.SOLAR_FLUX_UNIT,              "SFU",                 1.0e-22,  1,  0, -2,  0, 0);
        checkReference(Unit.TOTAL_ELECTRON_CONTENT_UNIT, "TECU",                  1.0e16,  0, -2,  0,  0, 0);

    }

    private void checkReference(final Unit unit, final String name, final double scale,
                                final int mass, final int length, final int time,
                                final int current, final int angle) {
        assertEquals(name, unit.toString());
        assertEquals(scale, unit.getScale(), 1.0e-10);
        assertEquals(new Fraction(mass),     unit.getMass());
        assertEquals(new Fraction(length),   unit.getLength());
        assertEquals(new Fraction(time),     unit.getTime());
        assertEquals(new Fraction(current),  unit.getCurrent());
        assertEquals(new Fraction(angle),    unit.getAngle());
    }

    @Test
    void testNotAUnit() {
        assertSame(Unit.NONE, Unit.parse("n/a"));
    }

    @Test
    void testOneUnit() {
        checkReference("1",
                       1.0,
                       Fraction.ZERO, Fraction.ZERO, Fraction.ZERO, Fraction.ZERO);
    }

    @Test
    void testND() {
        // nd does not mean "not defined", but nano-day…
        checkReference("nd",
                       Prefix.NANO.getFactor() * Constants.JULIAN_DAY,
                       Fraction.ZERO, Fraction.ZERO, Fraction.ONE, Fraction.ZERO);
    }

    @Test
    void testPredefinedUnit() {
        checkReference("MHz",
                       1.0e6,
                       Fraction.ZERO, Fraction.ZERO, Fraction.MINUS_ONE, Fraction.ZERO);
    }

    @Test
    void testSquareRoot() {
        checkReference("km/√d",
                       1000.0 / FastMath.sqrt(Constants.JULIAN_DAY),
                       Fraction.ZERO, Fraction.ONE, new Fraction(-1, 2), Fraction.ZERO);
    }

    @Test
    void testChain() {
        checkReference("kg.m^(3/4).s⁻¹",
                       1.0,
                       Fraction.ONE, new Fraction(3, 4), Fraction.MINUS_ONE, Fraction.ZERO);
    }

    @Test
    void testExponents() {
        checkReference("µas^⅖/(h**(2)×m)³",
                       FastMath.pow(FastMath.toRadians(1.0 / 3.6e9), 0.4) / FastMath.pow(3600, 6),
                       Fraction.ZERO, new Fraction(-3, 1), new Fraction(-6, 1), new Fraction(2, 5));
    }

    @Test
    void testCompoundInSquareRoot() {
        checkReference("km/√(kg.s)",
                       1000.0,
                       new Fraction(-1, 2), Fraction.ONE, new Fraction(-1, 2), Fraction.ZERO);
    }

    @Test
    void testLeftAssociativity() {
        checkReference("(kg/m)/s²",
                       1.0,
                       Fraction.ONE, Fraction.MINUS_ONE, new Fraction(-2), Fraction.ZERO);
        checkReference("kg/(m/s²)",
                       1.0,
                       Fraction.ONE, Fraction.MINUS_ONE, Fraction.TWO, Fraction.ZERO);
        checkReference("kg/m/s²",
                       1.0,
                       Fraction.ONE, Fraction.MINUS_ONE, new Fraction(-2), Fraction.ZERO);
    }

    @Test
    void testCcsdsRoot() {
        checkReference("km**0.5/s",
                       FastMath.sqrt(1000.0),
                       Fraction.ZERO, Fraction.ONE_HALF, Fraction.MINUS_ONE, Fraction.ZERO);
        checkReference("km/s**0.5",
                       1000.0,
                       Fraction.ZERO, Fraction.ONE, new Fraction(-1, 2), Fraction.ZERO);
    }

    @Test
    void testNumber() {
        checkReference("#/yr",
                       1.0 / Constants.JULIAN_YEAR,
                       Fraction.ZERO, Fraction.ZERO, Fraction.MINUS_ONE, Fraction.ZERO);
    }

    @Test
    void testReciprocal() {
        checkReference("1/s",
                       1.0,
                       Fraction.ZERO, Fraction.ZERO, Fraction.MINUS_ONE, Fraction.ZERO);
    }

    @Test
    void testSeveralMicro() {
        checkReference("µs", // here we use U+00B5, MICRO SIGN
                       1.0e-6,
                       Fraction.ZERO, Fraction.ZERO, Fraction.ONE, Fraction.ZERO);
        checkReference("μs", // here we use U+03BC, GREEK SMALL LETTER MU
                       1.0e-6,
                       Fraction.ZERO, Fraction.ZERO, Fraction.ONE, Fraction.ZERO);
    }

    @Test
    void testEmpty() {
        expectFailure("");
    }

    @Test
    void testIncompleteExponent1() {
        expectFailure("m.g^(2/)");
    }

    @Test
    void testIncompleteExponent2() {
        expectFailure("m.g^(2m)");
    }

    @Test
    void testMissingClosingParenthesis() {
        expectFailure("m.(W");
    }

    @Test
    void testGarbageOnInput() {
        expectFailure("kg+s");
    }

    @Test
    void testFactor() {
        checkReference("kg/3s",
                       1.0 / 3.0,
                       Fraction.ONE, Fraction.ZERO, Fraction.MINUS_ONE, Fraction.ZERO);
    }

    @Test
    void testMissingUnit() {
        expectFailure("km/√");
    }

    @Test
    void testRootAndPower() {
        expectFailure("km/√d³");
    }

    @Test
    void testRootAndParenthesisedPower() {
        checkReference("km/√(d³)",
                       1000.0 / (Constants.JULIAN_DAY * FastMath.sqrt(Constants.JULIAN_DAY)),
                       Fraction.ZERO, Fraction.ONE, new Fraction(-3, 2), Fraction.ZERO);
    }

    @Test
    void checkWrongNumber() {
        try {
            Unit.ensureCompatible("some description",
                                    Arrays.asList(Unit.METRE, Unit.METRE, Unit.METRE),
                                    false,
                                    Collections.singletonList(Unit.KILOMETRE));
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.WRONG_NB_COMPONENTS, oe.getSpecifier());
            assertEquals("some description", oe.getParts()[0]);
            assertEquals(3, ((Integer) oe.getParts()[1]).intValue());
            assertEquals(1, ((Integer) oe.getParts()[2]).intValue());
        }
    }

    @Test
    void checkWrongUnitsDimension() {
        try {
            Unit.ensureCompatible(null,
                                    Arrays.asList(Unit.METRE, Unit.METRE, Unit.METRE),
                                    false,
                                    Arrays.asList(Unit.METRE, Unit.SECOND, Unit.METRE));
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.INCOMPATIBLE_UNITS, oe.getSpecifier());
            assertEquals("m", oe.getParts()[0]);
            assertEquals("s", oe.getParts()[1]);
        }
    }

    @Test
    void checkWrongUnitsScale() {
        try {
            Unit.ensureCompatible(null,
                                    Arrays.asList(Unit.METRE, Unit.METRE, Unit.METRE),
                                    false,
                                    Arrays.asList(Unit.METRE, Unit.KILOMETRE, Unit.METRE));
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.INCOMPATIBLE_UNITS, oe.getSpecifier());
            assertEquals("m",  oe.getParts()[0]);
            assertEquals("km", oe.getParts()[1]);
        }
    }

    @Test
    void checkCorrectUnitsScale() {
        Unit.ensureCompatible(null,
                                Arrays.asList(Unit.METRE, Unit.METRE, Unit.METRE),
                                true,
                                Arrays.asList(Unit.METRE, Unit.KILOMETRE, Unit.METRE));
    }

    private void checkReference(final String unitSpecification, final double scale,
                                final Fraction mass, final Fraction length,
                                final Fraction time, final Fraction angle) {
        final Unit unit = Unit.parse(unitSpecification);
        assertEquals(unitSpecification, unit.toString());
        assertEquals(scale,  unit.getScale(), 1.0e-10 * scale);
        assertEquals(mass,   unit.getMass());
        assertEquals(length, unit.getLength());
        assertEquals(time,   unit.getTime());
        assertEquals(angle,  unit.getAngle());
    }

    private void expectFailure(final String unitSpecification) {
        try {
            Unit.parse(unitSpecification);
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(OrekitMessages.UNKNOWN_UNIT, oe.getSpecifier());
            assertEquals(unitSpecification, oe.getParts()[0]);
        }
    }

}
