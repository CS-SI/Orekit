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
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

/** Lexer for units.
 * @author Luc Maisonobe
 * @since 11.0
 */
class Lexer {

    /** Unit specification to tokenize. */
    private final CharSequence unitSpecification;

    /** End index. */
    private final int end;

    /** Start index for next token. */
    private int start;

    /** Next to last token emitted. */
    private Token nextToLast;

    /** Last token emitted. */
    private Token last;

    /** Upcoming token (which was pushed back). */
    private Token upcoming;

    /** Build a lexer for a unit specification.
     * @param unitSpecification unit specification to tokenize
     */
    Lexer(final CharSequence unitSpecification) {
        this.unitSpecification = unitSpecification;
        this.end               = unitSpecification.length();
        this.start             = 0;
        this.last              = null;
    }

    /** Get the complete unit specification.
     * @return complete unit specification
     */
    public String getUnitSpecification() {
        return unitSpecification.toString();
    }

    /** Push back last returned token.
     * <p>
     * This can be called only once
     * </p>
     */
    public void pushBack() {
        upcoming = last;
        last     = nextToLast;
    }

    /** Get next token.
     * @return next token, or null if there are no more tokens
     */
    public Token next() {

        if (upcoming != null) {
            nextToLast = last;
            last       = upcoming;
            upcoming   = null;
            return last;
        }

        // skip whitespace
        while (start < end && Character.isWhitespace(unitSpecification.charAt(start))) {
            ++start;
        }

        if (start >= end) {
            // no more characters to analyze
            nextToLast = last;
            last       = null;
            return null;
        }

        // look for prefixed units
        int current = start;
        while (current < end &&
               (Character.isLowerCase(unitSpecification.charAt(current)) ||
                Character.isUpperCase(unitSpecification.charAt(current)) ||
                unitSpecification.charAt(current) == '°'  ||
                unitSpecification.charAt(current) == '◦'  ||
                unitSpecification.charAt(current) == '′'  ||
                unitSpecification.charAt(current) == '\'' ||
                unitSpecification.charAt(current) == '″'  ||
                unitSpecification.charAt(current) == '"'  ||
                unitSpecification.charAt(current) == '%'  ||
                unitSpecification.charAt(current) == '#')) {
            ++current;
        }
        if (current > start) {
            return emit(current, TokenType.IDENTIFIER, 0, 1);
        }

        // look for power
        if (start < end - 1 &&
            unitSpecification.charAt(start)     == '*' &&
            unitSpecification.charAt(start + 1) == '*') {
            // power indicator as **
            return emit(start + 2, TokenType.POWER, 0, 1);
        } else if (unitSpecification.charAt(start) == '^') {
            // power indicator as ^
            return emit(start + 1, TokenType.POWER, 0, 1);
        } else if (convertSuperscript(start) != ' ' &&
                   last != null &&
                   last.getType() != TokenType.POWER) {
            // virtual power indicator as we switch to superscript characters
            return emit(start, TokenType.POWER, 0, 1);
        }

        // look for one character tokens
        if (unitSpecification.charAt(start) == '*') {
            return emit(start + 1, TokenType.MULTIPLICATION, 0, 1);
        } else if (unitSpecification.charAt(start) == '×') {
            return emit(start + 1, TokenType.MULTIPLICATION, 0, 1);
        } else if (unitSpecification.charAt(start) == '.') {
            return emit(start + 1, TokenType.MULTIPLICATION, 0, 1);
        } else if (unitSpecification.charAt(start) == '·') {
            return emit(start + 1, TokenType.MULTIPLICATION, 0, 1);
        } else if (unitSpecification.charAt(start) == '/') {
            return emit(start + 1, TokenType.DIVISION, 0, 1);
        } else if (unitSpecification.charAt(start) == '⁄') {
            return emit(start + 1, TokenType.DIVISION, 0, 1);
        } else if (unitSpecification.charAt(start) == '(') {
            return emit(start + 1, TokenType.OPEN, 0, 1);
        } else if (unitSpecification.charAt(start) == ')') {
            return emit(start + 1, TokenType.CLOSE, 0, 1);
        } else if (unitSpecification.charAt(start) == '√') {
            return emit(start + 1, TokenType.SQUARE_ROOT, 0, 1);
        }

        // look for special case "0.5" (used by CCSDS for square roots)
        if (start < end - 2 &&
             unitSpecification.charAt(start)     == '0' &&
             unitSpecification.charAt(start + 1) == '.' &&
             unitSpecification.charAt(start + 2) == '5') {
            // ½ written as decimal number
            return emit(start + 3, TokenType.FRACTION, 1, 2);
        }

        // look for special case "1.5" (used by CCSDS for power 3/2)
        if (start < end - 2 &&
             unitSpecification.charAt(start)     == '1' &&
             unitSpecification.charAt(start + 1) == '.' &&
             unitSpecification.charAt(start + 2) == '5') {
            // 3/2 written as decimal number
            return emit(start + 3, TokenType.FRACTION, 3, 2);
        }

        // look for unicode fractions
        if (unitSpecification.charAt(start) == '¼') {
            return emit(start + 1, TokenType.FRACTION, 1, 4);
        } else if (unitSpecification.charAt(start) == '½') {
            return emit(start + 1, TokenType.FRACTION, 1, 2);
        } else if (unitSpecification.charAt(start) == '¾') {
            return emit(start + 1, TokenType.FRACTION, 3, 4);
        } else if (unitSpecification.charAt(start) == '⅐') {
            return emit(start + 1, TokenType.FRACTION, 1, 7);
        } else if (unitSpecification.charAt(start) == '⅑') {
            return emit(start + 1, TokenType.FRACTION, 1, 9);
        } else if (unitSpecification.charAt(start) == '⅒') {
            return emit(start + 1, TokenType.FRACTION, 1, 10);
        } else if (unitSpecification.charAt(start) == '⅓') {
            return emit(start + 1, TokenType.FRACTION, 1, 3);
        } else if (unitSpecification.charAt(start) == '⅔') {
            return emit(start + 1, TokenType.FRACTION, 2, 3);
        } else if (unitSpecification.charAt(start) == '⅕') {
            return emit(start + 1, TokenType.FRACTION, 1, 5);
        } else if (unitSpecification.charAt(start) == '⅖') {
            return emit(start + 1, TokenType.FRACTION, 2, 5);
        } else if (unitSpecification.charAt(start) == '⅗') {
            return emit(start + 1, TokenType.FRACTION, 3, 5);
        } else if (unitSpecification.charAt(start) == '⅘') {
            return emit(start + 1, TokenType.FRACTION, 4, 5);
        } else if (unitSpecification.charAt(start) == '⅙') {
            return emit(start + 1, TokenType.FRACTION, 1, 6);
        } else if (unitSpecification.charAt(start) == '⅚') {
            return emit(start + 1, TokenType.FRACTION, 5, 6);
        } else if (unitSpecification.charAt(start) == '⅛') {
            return emit(start + 1, TokenType.FRACTION, 1, 8);
        } else if (unitSpecification.charAt(start) == '⅜') {
            return emit(start + 1, TokenType.FRACTION, 3, 8);
        } else if (unitSpecification.charAt(start) == '⅝') {
            return emit(start + 1, TokenType.FRACTION, 5, 8);
        } else if (unitSpecification.charAt(start) == '⅞') {
            return emit(start + 1, TokenType.FRACTION, 7, 8);
        }

        // it must be an integer, either as regular character or as superscript
        final Converter converter = (convertSuperscript(start) == ' ') ?
                                    this::noConvert :
                                    this::convertSuperscript;

        // manage sign, taking care of counting characters properly
        final int sign;
        final int numberStart;
        if (converter.convert(start) == '+') {
            sign        = +1;
            numberStart = start + 1;
        } else if (converter.convert(start) == '-') {
            sign        = -1;
            numberStart = start + 1;
        } else {
            sign        = 1;
            numberStart = start;
        }
        current = numberStart;

        int value = 0;
        while (current < end) {
            final int c = converter.convert(current);
            if (c >= '0' && c <= '9') {
                value = value * 10 + (c - '0');
                ++current;
            } else {
                break;
            }
        }
        if (current > numberStart) {
            // there were some digits
            return emit(current, TokenType.INTEGER, sign * value, 1);
        }

        throw generateException();

    }

    /** Generate an exception.
     * @return generated exception
     */
    public OrekitException generateException() {
        return new OrekitException(OrekitMessages.UNKNOWN_UNIT, unitSpecification);
    }

    /** Emit one token.
     * @param after index after token
     * @param type token type
     * @param numerator value of the token numerator
     * @param denominator value of the token denominator
     * @return new token
     */
    private Token emit(final int after, final TokenType type, final int numerator, final int denominator) {
        final CharSequence subString = unitSpecification.subSequence(start, after);
        start      = after;
        nextToLast = last;
        last       = new Token(subString, type, numerator,
                               denominator == 1 ? null : new Fraction(numerator, denominator));
        return last;
    }

    /** Convert a superscript character to regular digit or sign character.
     * @param index character index
     * @return regular digit or sign character, or ' ' if character is not a superscript
     */
    private char convertSuperscript(final int index) {
        // we can't do fancy stuff with code points
        // superscripts for 1, 2 and 3 are not in the same range as others
        switch (unitSpecification.charAt(index)) {
            case '⁰' :
                return '0';
            case '¹' :
                return '1';
            case '²' :
                return '2';
            case '³' :
                return '3';
            case '⁴' :
                return '4';
            case '⁵' :
                return '5';
            case '⁶' :
                return '6';
            case '⁷' :
                return '7';
            case '⁸' :
                return '8';
            case '⁹' :
                return '9';
            case '⁺' :
                return '+';
            case '⁻' :
                return '-';
            default :
                return ' ';
        }

    }

    /** No-op converter.
     * @param index character index
     * @return character at index
     */
    private char noConvert(final int index) {
        return unitSpecification.charAt(index);
    }

    /** Character converter. */
    private interface Converter {
        /** Convert a character.
         * @param index character index
         * @return converted character
         */
        char convert(int index);
    }

}
