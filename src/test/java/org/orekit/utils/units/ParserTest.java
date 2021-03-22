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
package org.orekit.utils.units;

import org.hipparchus.fraction.Fraction;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.utils.Constants;

/**
 * Unit tests for {@link Lexer}.
 *
 * @author Luc Maisonobe
 */
public class ParserTest {

    @Test
    public void testNotAUnit() {
        Assert.assertSame(Unit.NONE, Parser.parse("n/a"));
    }

    @Test
    public void testPredefinedUnit() {
        checkReference("MHz",
                       1.0e6,
                       Fraction.ZERO, Fraction.ZERO, Fraction.MINUS_ONE, Fraction.ZERO);
    }

    @Test
    public void testSquareRoot() {
        checkReference("km/√d",
                       1000.0 / FastMath.sqrt(Constants.JULIAN_DAY),
                       Fraction.ZERO, Fraction.ONE, new Fraction(-1, 2), Fraction.ZERO);
    }

    @Test
    public void testChain() {
        checkReference("kg.m.s⁻¹",
                       1.0,
                       Fraction.ONE, Fraction.ONE, Fraction.MINUS_ONE, Fraction.ZERO);
    }

    @Test
    public void testExponents() {
        checkReference("µas^(2/5)/(h**(2)×m)³",
                       FastMath.pow(FastMath.toRadians(1.0 / 3.6e9), 0.4) / FastMath.pow(3600, 6),
                       Fraction.ZERO, new Fraction(-3, 1), new Fraction(-6, 1), new Fraction(2, 5));
    }

    @Test
    public void testCompoundInSquareRoot() {
        checkReference("km/√(kg.s)",
                       1000.0,
                       new Fraction(-1, 2), Fraction.ONE, new Fraction(-1, 2), Fraction.ZERO);
    }

    @Test
    public void testEmpty() {
        expectFailure("");
    }

    @Test
    public void testIncompleteExponent1() {
        expectFailure("m.g^(2/)");
    }

    @Test
    public void testIncompleteExponent2() {
        expectFailure("m.g^(2m)");
    }

    @Test
    public void testMissingClosingParenthesis() {
        expectFailure("m.(W");
    }

    @Test
    public void testGarbageOnInput() {
        expectFailure("kg+s");
    }

    @Test
    public void testSpuriousFactor() {
        expectFailure("kg/3s");
    }

    @Test
    public void testMissingUnit() {
        expectFailure("km/√");
    }

    @Test
    public void testRootAndPower() {
        expectFailure("km/√d³");
    }

    private void checkReference(final String unitSpecification, final double scale,
                                final Fraction mass, final Fraction length,
                                final Fraction time, final Fraction angle) {
        final Unit unit = Parser.parse(unitSpecification);
        Assert.assertEquals(unitSpecification, unit.toString());
        Assert.assertEquals(scale,  unit.getScale(), 1.0e-10 * scale);
        Assert.assertEquals(mass,   unit.getMass());
        Assert.assertEquals(length, unit.getLength());
        Assert.assertEquals(time,   unit.getTime());
        Assert.assertEquals(angle,  unit.getAngle());
    }

    private void expectFailure(final String unitSpecification) {
        try {
            Parser.parse(unitSpecification);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNKNOWN_UNIT, oe.getSpecifier());
            Assert.assertEquals(unitSpecification, oe.getParts()[0]);
        }
    }

}
