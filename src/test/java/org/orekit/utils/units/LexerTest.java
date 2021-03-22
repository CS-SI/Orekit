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

import org.junit.Assert;
import org.junit.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

/**
 * Unit tests for {@link Lexer}.
 *
 * @author Luc Maisonobe
 */
public class LexerTest {

    @Test
    public void testAllTypes() {
        final Lexer lexer = new Lexer("√kg*km**  (3/2) /(µs^2*Ω⁻⁷)");
        expect(lexer, "√",    TokenType.SQUARE_ROOT, null, null, 0);
        expect(lexer, "kg",   TokenType.PREFIXED_UNIT, null, Unit.KILOGRAM, 0);
        expect(lexer, "*",    TokenType.MULTIPLICATION, null, null, 0);
        expect(lexer, "km",   TokenType.PREFIXED_UNIT, Prefix.KILO, Unit.METRE, 0);
        expect(lexer, "**",   TokenType.POWER, null, null, 0);
        expect(lexer, "(",    TokenType.OPEN, null, null, 0);
        expect(lexer, "3",    TokenType.INTEGER, null, null, 3);
        expect(lexer, "/",    TokenType.DIVISION, null, null, 0);
        expect(lexer, "2",    TokenType.INTEGER, null, null, 2);
        expect(lexer, ")",    TokenType.CLOSE, null, null, 0);
        expect(lexer, "/",    TokenType.DIVISION, null, null, 0);
        expect(lexer, "(",    TokenType.OPEN, null, null, 0);
        expect(lexer, "µs",   TokenType.PREFIXED_UNIT, Prefix.MICRO, Unit.SECOND, 0);
        expect(lexer, "^",    TokenType.POWER, null, null, 0);
        expect(lexer, "2",    TokenType.INTEGER, null, null, 2);
        expect(lexer, "*",    TokenType.MULTIPLICATION, null, null, 0);
        expect(lexer, "Ω",    TokenType.PREFIXED_UNIT, null, Unit.OHM, 0);
        expect(lexer, "",     TokenType.POWER, null, null, 0);
        expect(lexer, "⁻⁷",   TokenType.INTEGER, null, null, -7);
        expect(lexer, ")",    TokenType.CLOSE, null, null, 0);
        Assert.assertNull(lexer.next());
    }

    @Test
    public void testRegularExponent() {
        final Lexer lexer = new Lexer("N^123450 × MHz^-98765");
        expect(lexer, "N",      TokenType.PREFIXED_UNIT, null, Unit.NEWTON, 0);
        expect(lexer, "^",      TokenType.POWER, null, null, 0);
        expect(lexer, "123450", TokenType.INTEGER, null, null, 123450);
        expect(lexer, "×",      TokenType.MULTIPLICATION, null, null, 0);
        expect(lexer, "MHz",    TokenType.PREFIXED_UNIT, Prefix.MEGA, Unit.HERTZ, 0);
        expect(lexer, "^",      TokenType.POWER, null, null, 0);
        expect(lexer, "-98765", TokenType.INTEGER, null, null, -98765);
        Assert.assertNull(lexer.next());
    }

    @Test
    public void testSuperscriptExponent() {
        final Lexer lexer = new Lexer("SFU⁺¹²³⁴⁵⁰ ⁄ mas⁻⁹⁸⁷⁶⁵");
        expect(lexer, "SFU",     TokenType.PREFIXED_UNIT, null, Unit.SOLAR_FLUX_UNIT, 0);
        expect(lexer, "",        TokenType.POWER, null, null, 0);
        expect(lexer, "⁺¹²³⁴⁵⁰", TokenType.INTEGER, null, null, 123450);
        expect(lexer, "⁄",       TokenType.DIVISION, null, null, 0);
        expect(lexer, "mas",     TokenType.PREFIXED_UNIT, Prefix.MILLI, Unit.ARC_SECOND, 0);
        expect(lexer, "",        TokenType.POWER, null, null, 0);
        expect(lexer, "⁻⁹⁸⁷⁶⁵",  TokenType.INTEGER, null, null, -98765);
        Assert.assertNull(lexer.next());
    }

    @Test
    public void testSignWithoutDigits() {
        final Lexer lexer = new Lexer("Pa⁻");
        expect(lexer, "Pa", TokenType.PREFIXED_UNIT, null, Unit.PASCAL, 0);
        expect(lexer, "",  TokenType.POWER, null, null, 0);
        expectFailure(lexer);
    }

    @Test
    public void testMultipleSigns() {
        final Lexer lexer = new Lexer("MJ⁻⁺²");
        expect(lexer, "MJ", TokenType.PREFIXED_UNIT, Prefix.MEGA, Unit.JOULE, 0);
        expect(lexer, "",  TokenType.POWER, null, null, 0);
        expectFailure(lexer);
    }

    @Test
    public void testUnknownCharacter() {
        final Lexer lexer = new Lexer("pW^2#");
        expect(lexer, "pW", TokenType.PREFIXED_UNIT, Prefix.PICO, Unit.WATT, 0);
        expect(lexer, "^",  TokenType.POWER, null, null, 0);
        expect(lexer, "2",  TokenType.INTEGER, null, null, 2);
        expectFailure(lexer); 
    }

    @Test
    public void testPercentageCharacter() {
        final Lexer lexer = new Lexer("%");
        expect(lexer, "%", TokenType.PREFIXED_UNIT, null, Unit.PERCENT, 0);
        Assert.assertNull(lexer.next());
    }

    @Test
    public void testUnknownUnit() {
        final Lexer lexer = new Lexer("K");
        expectFailure(lexer);   
    }

    @Test
    public void testUnknownPrefix() {
        final Lexer lexer = new Lexer("b°");
        expectFailure(lexer);   
    }

    @Test
    public void testStartWithSuperscript() {
        final Lexer lexer = new Lexer("³");
        expect(lexer, "³", TokenType.INTEGER, null, null, 3);
        Assert.assertNull(lexer.next());
    }

    @Test
    public void testCharacters() {
        final Lexer lexer = new Lexer("d*°*min*◦*deg*′*hh*'*ad*″*a*\"*µ''");
        expect(lexer, "d",   TokenType.PREFIXED_UNIT, null, Unit.DAY, 0);
        expect(lexer, "*",   TokenType.MULTIPLICATION, null, null, 0);
        expect(lexer, "°",   TokenType.PREFIXED_UNIT, null, Unit.DEGREE, 0);
        expect(lexer, "*",   TokenType.MULTIPLICATION, null, null, 0);
        expect(lexer, "min", TokenType.PREFIXED_UNIT, null, Unit.MINUTE, 0);
        expect(lexer, "*",   TokenType.MULTIPLICATION, null, null, 0);
        expect(lexer, "◦",   TokenType.PREFIXED_UNIT, null, Unit.DEGREE, 0);
        expect(lexer, "*",   TokenType.MULTIPLICATION, null, null, 0);
        expect(lexer, "deg", TokenType.PREFIXED_UNIT, null, Unit.DEGREE, 0);
        expect(lexer, "*",   TokenType.MULTIPLICATION, null, null, 0);
        expect(lexer, "′",   TokenType.PREFIXED_UNIT, null, Unit.ARC_MINUTE, 0);
        expect(lexer, "*",   TokenType.MULTIPLICATION, null, null, 0);
        expect(lexer, "hh",  TokenType.PREFIXED_UNIT, Prefix.HECTO, Unit.HOUR, 0);
        expect(lexer, "*",   TokenType.MULTIPLICATION, null, null, 0);
        expect(lexer, "'",   TokenType.PREFIXED_UNIT, null, Unit.ARC_MINUTE, 0);
        expect(lexer, "*",   TokenType.MULTIPLICATION, null, null, 0);
        expect(lexer, "ad",  TokenType.PREFIXED_UNIT, Prefix.ATTO, Unit.DAY, 0);
        expect(lexer, "*",   TokenType.MULTIPLICATION, null, null, 0);
        expect(lexer, "″",   TokenType.PREFIXED_UNIT, null, Unit.ARC_SECOND, 0);
        expect(lexer, "*",   TokenType.MULTIPLICATION, null, null, 0);
        expect(lexer, "a",   TokenType.PREFIXED_UNIT, null, Unit.YEAR, 0);
        expect(lexer, "*",   TokenType.MULTIPLICATION, null, null, 0);
        expect(lexer, "\"",  TokenType.PREFIXED_UNIT, null, Unit.ARC_SECOND, 0);
        expect(lexer, "*",   TokenType.MULTIPLICATION, null, null, 0);
        expect(lexer, "µ''", TokenType.PREFIXED_UNIT, Prefix.MICRO, Unit.ARC_SECOND, 0);
        Assert.assertNull(lexer.next());
    }

    @Test
    public void testPushBack() {
        final Lexer lexer = new Lexer("m/s");
        expect(lexer, "m",   TokenType.PREFIXED_UNIT, null, Unit.METRE, 0);
        expect(lexer, "/",   TokenType.DIVISION, null, null, 0);
        lexer.pushBack();
        expect(lexer, "/",   TokenType.DIVISION, null, null, 0);
        expect(lexer, "s",   TokenType.PREFIXED_UNIT, null, Unit.SECOND, 0);
    }

    @Test
    public void testPushBackBeforeVirtualExponent() {
        final Lexer lexer = new Lexer("m²/s");
        expect(lexer, "m",   TokenType.PREFIXED_UNIT, null, Unit.METRE, 0);
        lexer.pushBack();
        expect(lexer, "m",   TokenType.PREFIXED_UNIT, null, Unit.METRE, 0);
        expect(lexer, "",    TokenType.POWER, null, null, 0);
        expect(lexer, "²",   TokenType.INTEGER, null, null, 2);
        expect(lexer, "/",   TokenType.DIVISION, null, null, 0);
        expect(lexer, "s",   TokenType.PREFIXED_UNIT, null, Unit.SECOND, 0);
    }

    @Test
    public void testPushBackAtVirtualExponent() {
        final Lexer lexer = new Lexer("m²/s");
        expect(lexer, "m",   TokenType.PREFIXED_UNIT, null, Unit.METRE, 0);
        expect(lexer, "",    TokenType.POWER, null, null, 0);
        lexer.pushBack();
        expect(lexer, "",    TokenType.POWER, null, null, 0);
        expect(lexer, "²",   TokenType.INTEGER, null, null, 2);
        expect(lexer, "/",   TokenType.DIVISION, null, null, 0);
        expect(lexer, "s",   TokenType.PREFIXED_UNIT, null, Unit.SECOND, 0);
    }

    @Test
    public void testPushBackAfterVirtualExponent() {
        final Lexer lexer = new Lexer("m²/s");
        expect(lexer, "m",   TokenType.PREFIXED_UNIT, null, Unit.METRE, 0);
        expect(lexer, "",    TokenType.POWER, null, null, 0);
        expect(lexer, "²",   TokenType.INTEGER, null, null, 2);
        lexer.pushBack();
        expect(lexer, "²",   TokenType.INTEGER, null, null, 2);
        expect(lexer, "/",   TokenType.DIVISION, null, null, 0);
        expect(lexer, "s",   TokenType.PREFIXED_UNIT, null, Unit.SECOND, 0);
    }

    @Test
    public void testPushBackAtEnd() {
        final Lexer lexer = new Lexer("m²/s");
        expect(lexer, "m",   TokenType.PREFIXED_UNIT, null, Unit.METRE, 0);
        expect(lexer, "",    TokenType.POWER, null, null, 0);
        expect(lexer, "²",   TokenType.INTEGER, null, null, 2);
        expect(lexer, "/",   TokenType.DIVISION, null, null, 0);
        expect(lexer, "s",   TokenType.PREFIXED_UNIT, null, Unit.SECOND, 0);
        Assert.assertNull(lexer.next());
        lexer.pushBack();
        Assert.assertNull(lexer.next());
    }

    private void expect(Lexer lexer, String subString, TokenType type,
                        Prefix prefix, Unit unit, int value) {
        Token t = lexer.next();
        Assert.assertEquals(subString, t.getSubString());
        Assert.assertEquals(type,      t.getType());
        if (unit == null) {
            Assert.assertNull(t.getPrefixedUnit());
        } else {
            Assert.assertTrue(unit.sameDimension(t.getPrefixedUnit()));
            double factor = (prefix == null) ? 1.0 : prefix.getFactor();
            Assert.assertEquals(factor * unit.getScale(), t.getPrefixedUnit().getScale(), 1.0e-10);
        }
        Assert.assertEquals(value, t.getValue());
    }

    private void expectFailure(Lexer lexer) {
        try {
            lexer.next();
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNKNOWN_UNIT, oe.getSpecifier());
            Assert.assertEquals(lexer.getUnitSpecification(), oe.getParts()[0]);
        }
    }

}
