/* Copyright 2002-2023 CS GROUP
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
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
        expect(lexer, "√",    TokenType.SQUARE_ROOT, 0, 1);
        expect(lexer, "kg",   TokenType.IDENTIFIER, 0, 1);
        expect(lexer, "*",    TokenType.MULTIPLICATION, 0, 1);
        expect(lexer, "km",   TokenType.IDENTIFIER, 0, 1);
        expect(lexer, "**",   TokenType.POWER, 0, 1);
        expect(lexer, "(",    TokenType.OPEN, 0, 1);
        expect(lexer, "3",    TokenType.INTEGER, 3, 1);
        expect(lexer, "/",    TokenType.DIVISION, 0, 1);
        expect(lexer, "2",    TokenType.INTEGER, 2, 1);
        expect(lexer, ")",    TokenType.CLOSE, 0, 1);
        expect(lexer, "/",    TokenType.DIVISION, 0, 1);
        expect(lexer, "(",    TokenType.OPEN, 0, 1);
        expect(lexer, "µs",   TokenType.IDENTIFIER, 0, 1);
        expect(lexer, "^",    TokenType.POWER, 0, 1);
        expect(lexer, "2",    TokenType.INTEGER, 2, 1);
        expect(lexer, "*",    TokenType.MULTIPLICATION, 0, 1);
        expect(lexer, "Ω",    TokenType.IDENTIFIER, 0, 1);
        expect(lexer, "",     TokenType.POWER, 0, 1);
        expect(lexer, "⁻⁷",   TokenType.INTEGER, -7, 1);
        expect(lexer, ")",    TokenType.CLOSE, 0, 1);
        Assertions.assertNull(lexer.next());
    }

    @Test
    public void testRegularExponent() {
        final Lexer lexer = new Lexer("N^123450 × MHz^-98765");
        expect(lexer, "N",      TokenType.IDENTIFIER, 0, 1);
        expect(lexer, "^",      TokenType.POWER, 0, 1);
        expect(lexer, "123450", TokenType.INTEGER, 123450, 1);
        expect(lexer, "×",      TokenType.MULTIPLICATION, 0, 1);
        expect(lexer, "MHz",    TokenType.IDENTIFIER, 0, 1);
        expect(lexer, "^",      TokenType.POWER, 0, 1);
        expect(lexer, "-98765", TokenType.INTEGER, -98765, 1);
        Assertions.assertNull(lexer.next());
    }

    @Test
    public void testSuperscriptExponent() {
        final Lexer lexer = new Lexer("SFU⁺¹²³⁴⁵⁰ ⁄ mas⁻⁹⁸⁷⁶⁵");
        expect(lexer, "SFU",     TokenType.IDENTIFIER, 0, 1);
        expect(lexer, "",        TokenType.POWER, 0, 1);
        expect(lexer, "⁺¹²³⁴⁵⁰", TokenType.INTEGER, 123450, 1);
        expect(lexer, "⁄",       TokenType.DIVISION, 0, 1);
        expect(lexer, "mas",     TokenType.IDENTIFIER, 0, 1);
        expect(lexer, "",        TokenType.POWER, 0, 1);
        expect(lexer, "⁻⁹⁸⁷⁶⁵",  TokenType.INTEGER, -98765, 1);
        Assertions.assertNull(lexer.next());
    }

    @Test
    public void testSignWithoutDigits() {
        final Lexer lexer = new Lexer("Pa⁻");
        expect(lexer, "Pa", TokenType.IDENTIFIER, 0, 1);
        expect(lexer, "",  TokenType.POWER, 0, 1);
        expectFailure(lexer);
    }

    @Test
    public void testMultipleSigns() {
        final Lexer lexer = new Lexer("MJ⁻⁺²");
        expect(lexer, "MJ", TokenType.IDENTIFIER, 0, 1);
        expect(lexer, "",  TokenType.POWER, 0, 1);
        expectFailure(lexer);
    }

    @Test
    public void testUnknownCharacter() {
        final Lexer lexer = new Lexer("pW^2∇");
        expect(lexer, "pW", TokenType.IDENTIFIER, 0, 1);
        expect(lexer, "^",  TokenType.POWER, 0, 1);
        expect(lexer, "2",  TokenType.INTEGER, 2, 1);
        expectFailure(lexer);
    }

    @Test
    public void testPercentageCharacter() {
        final Lexer lexer = new Lexer("%");
        expect(lexer, "%", TokenType.IDENTIFIER, 0, 1);
        Assertions.assertNull(lexer.next());
    }

    @Test
    public void testStartWithSuperscript() {
        final Lexer lexer = new Lexer("³");
        expect(lexer, "³", TokenType.INTEGER, 3, 1);
        Assertions.assertNull(lexer.next());
    }

    @Test
    public void testCharacters() {
        final Lexer lexer = new Lexer("d*°*min*◦*deg*′*hh*'*ad*″*a*\"*µ''");
        expect(lexer, "d",   TokenType.IDENTIFIER, 0, 1);
        expect(lexer, "*",   TokenType.MULTIPLICATION, 0, 1);
        expect(lexer, "°",   TokenType.IDENTIFIER, 0, 1);
        expect(lexer, "*",   TokenType.MULTIPLICATION, 0, 1);
        expect(lexer, "min", TokenType.IDENTIFIER, 0, 1);
        expect(lexer, "*",   TokenType.MULTIPLICATION, 0, 1);
        expect(lexer, "◦",   TokenType.IDENTIFIER, 0, 1);
        expect(lexer, "*",   TokenType.MULTIPLICATION, 0, 1);
        expect(lexer, "deg", TokenType.IDENTIFIER, 0, 1);
        expect(lexer, "*",   TokenType.MULTIPLICATION, 0, 1);
        expect(lexer, "′",   TokenType.IDENTIFIER, 0, 1);
        expect(lexer, "*",   TokenType.MULTIPLICATION, 0, 1);
        expect(lexer, "hh",  TokenType.IDENTIFIER, 0, 1);
        expect(lexer, "*",   TokenType.MULTIPLICATION, 0, 1);
        expect(lexer, "'",   TokenType.IDENTIFIER, 0, 1);
        expect(lexer, "*",   TokenType.MULTIPLICATION, 0, 1);
        expect(lexer, "ad",  TokenType.IDENTIFIER, 0, 1);
        expect(lexer, "*",   TokenType.MULTIPLICATION, 0, 1);
        expect(lexer, "″",   TokenType.IDENTIFIER, 0, 1);
        expect(lexer, "*",   TokenType.MULTIPLICATION, 0, 1);
        expect(lexer, "a",   TokenType.IDENTIFIER, 0, 1);
        expect(lexer, "*",   TokenType.MULTIPLICATION, 0, 1);
        expect(lexer, "\"",  TokenType.IDENTIFIER, 0, 1);
        expect(lexer, "*",   TokenType.MULTIPLICATION, 0, 1);
        expect(lexer, "µ''", TokenType.IDENTIFIER, 0, 1);
        Assertions.assertNull(lexer.next());
    }

    @Test
    public void testIdentifierUnicodeLetter() {
        final String s = "αβγDEFghi";
        final Lexer lexer = new Lexer(s);
        expect(lexer, s, TokenType.IDENTIFIER, 0, 1);
        Assertions.assertNull(lexer.next());
    }

    @Test
    public void testOneHalfAsDecimal() {
        final Lexer lexer = new Lexer("0.5");
        expect(lexer, "0.5", TokenType.FRACTION, 1, 2);
        Assertions.assertNull(lexer.next());
    }

    @Test
    public void testThreeHalfAsDecimal() {
        final Lexer lexer = new Lexer("1.5");
        expect(lexer, "1.5", TokenType.FRACTION, 3, 2);
        Assertions.assertNull(lexer.next());
    }

    @Test
    public void testUnicodeFractions() {
        final Lexer lexer = new Lexer("¼½¾⅐⅑⅒⅓⅔⅕⅖⅗⅘⅙⅚⅛⅜⅝⅞");
        expect(lexer, "¼", TokenType.FRACTION, 1, 4);
        expect(lexer, "½", TokenType.FRACTION, 1, 2);
        expect(lexer, "¾", TokenType.FRACTION, 3, 4);
        expect(lexer, "⅐", TokenType.FRACTION, 1, 7);
        expect(lexer, "⅑", TokenType.FRACTION, 1, 9);
        expect(lexer, "⅒", TokenType.FRACTION, 1, 10);
        expect(lexer, "⅓", TokenType.FRACTION, 1, 3);
        expect(lexer, "⅔", TokenType.FRACTION, 2, 3);
        expect(lexer, "⅕", TokenType.FRACTION, 1, 5);
        expect(lexer, "⅖", TokenType.FRACTION, 2, 5);
        expect(lexer, "⅗", TokenType.FRACTION, 3, 5);
        expect(lexer, "⅘", TokenType.FRACTION, 4, 5);
        expect(lexer, "⅙", TokenType.FRACTION, 1, 6);
        expect(lexer, "⅚", TokenType.FRACTION, 5, 6);
        expect(lexer, "⅛", TokenType.FRACTION, 1, 8);
        expect(lexer, "⅜", TokenType.FRACTION, 3, 8);
        expect(lexer, "⅝", TokenType.FRACTION, 5, 8);
        expect(lexer, "⅞", TokenType.FRACTION, 7, 8);
        Assertions.assertNull(lexer.next());
    }

    @Test
    public void testPushBack() {
        final Lexer lexer = new Lexer("m/s");
        expect(lexer, "m",   TokenType.IDENTIFIER, 0, 1);
        expect(lexer, "/",   TokenType.DIVISION, 0, 1);
        lexer.pushBack();
        expect(lexer, "/",   TokenType.DIVISION, 0, 1);
        expect(lexer, "s",   TokenType.IDENTIFIER, 0, 1);
    }

    @Test
    public void testPushBackBeforeVirtualExponent() {
        final Lexer lexer = new Lexer("m²/s");
        expect(lexer, "m",   TokenType.IDENTIFIER, 0, 1);
        lexer.pushBack();
        expect(lexer, "m",   TokenType.IDENTIFIER, 0, 1);
        expect(lexer, "",    TokenType.POWER, 0, 1);
        expect(lexer, "²",   TokenType.INTEGER, 2, 1);
        expect(lexer, "/",   TokenType.DIVISION, 0, 1);
        expect(lexer, "s",   TokenType.IDENTIFIER, 0, 1);
    }

    @Test
    public void testPushBackAtVirtualExponent() {
        final Lexer lexer = new Lexer("m²/s");
       expect(lexer, "m",   TokenType.IDENTIFIER, 0, 1);
        expect(lexer, "",    TokenType.POWER, 0, 1);
        lexer.pushBack();
        expect(lexer, "",    TokenType.POWER, 0, 1);
        expect(lexer, "²",   TokenType.INTEGER, 2, 1);
        expect(lexer, "/",   TokenType.DIVISION, 0, 1);
        expect(lexer, "s",   TokenType.IDENTIFIER, 0, 1);
    }

    @Test
    public void testPushBackAfterVirtualExponent() {
        final Lexer lexer = new Lexer("m²/s");
        expect(lexer, "m",   TokenType.IDENTIFIER, 0, 1);
        expect(lexer, "",    TokenType.POWER, 0, 1);
        expect(lexer, "²",   TokenType.INTEGER, 2, 1);
        lexer.pushBack();
        expect(lexer, "²",   TokenType.INTEGER, 2, 1);
        expect(lexer, "/",   TokenType.DIVISION, 0, 1);
        expect(lexer, "s",   TokenType.IDENTIFIER, 0, 1);
    }

    @Test
    public void testPushBackAtEnd() {
        final Lexer lexer = new Lexer("m²/s");
        expect(lexer, "m",   TokenType.IDENTIFIER, 0, 1);
        expect(lexer, "",    TokenType.POWER, 0, 1);
        expect(lexer, "²",   TokenType.INTEGER, 2, 1);
        expect(lexer, "/",   TokenType.DIVISION, 0, 1);
        expect(lexer, "s",   TokenType.IDENTIFIER, 0, 1);
        Assertions.assertNull(lexer.next());
        lexer.pushBack();
        Assertions.assertNull(lexer.next());
    }

    private void expect(Lexer lexer, String subString, TokenType type,
                        int numerator, int denominator) {
        Token t = lexer.next();
        Assertions.assertEquals(subString, t.getSubString());
        Assertions.assertEquals(type,      t.getType());
        Assertions.assertEquals(numerator, t.getInt());
        Assertions.assertEquals(type == TokenType.FRACTION ? new Fraction(numerator, denominator) : null,
                            t.getFraction());
    }

    private void expectFailure(Lexer lexer) {
        try {
            lexer.next();
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNKNOWN_UNIT, oe.getSpecifier());
            Assertions.assertEquals(lexer.getUnitSpecification(), oe.getParts()[0]);
        }
    }

}
