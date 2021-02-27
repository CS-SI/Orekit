/* Copyright 2002-2021 CS GROUP
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
               (Character.isAlphabetic(unitSpecification.charAt(current)) ||
                unitSpecification.charAt(current) == '°'  ||
                unitSpecification.charAt(current) == '◦'  ||
                unitSpecification.charAt(current) == '′'  ||
                unitSpecification.charAt(current) == '\'' ||
                unitSpecification.charAt(current) == '″'  ||
                unitSpecification.charAt(current) == '"'  ||
                unitSpecification.charAt(current) == 'µ')) {
            ++current;
        }
        if (current > start) {
            final String identifier = unitSpecification.subSequence(start, current).toString();
            return emit(current, TokenType.PREFIXED_UNIT, PrefixedUnit.valueOf(identifier), 0);
        }

        // look for power
        if ((start < end - 1) &&
            unitSpecification.charAt(start) == '*' &&
            unitSpecification.charAt(start + 1) == '*') {
            // power indicator as **
            return emit(start + 2, TokenType.POWER, null, 0);
        } else if (unitSpecification.charAt(start) == '^') {
            // power indicator as ^
            return emit(start + 1, TokenType.POWER, null, 0);
        } else if (convertSuperscript(start) != ' ' &&
                   last != null &&
                   last.getType() != TokenType.POWER) {
            // virtual power indicator as we switch to superscript characters
            return emit(start, TokenType.POWER, null, 0);
        }

        // look for one character tokens
        if (unitSpecification.charAt(start) == '*') {
            return emit(start + 1, TokenType.MULTIPLICATION, null, 0);
        } else if (unitSpecification.charAt(start) == '×') {
            return emit(start + 1, TokenType.MULTIPLICATION, null, 0);
        } else if (unitSpecification.charAt(start) == '.') {
            return emit(start + 1, TokenType.MULTIPLICATION, null, 0);
        } else if (unitSpecification.charAt(start) == '/') {
            return emit(start + 1, TokenType.DIVISION, null, 0);
        } else if (unitSpecification.charAt(start) == '⁄') {
            return emit(start + 1, TokenType.DIVISION, null, 0);
        } else if (unitSpecification.charAt(start) == '(') {
            return emit(start + 1, TokenType.OPEN, null, 0);
        } else if (unitSpecification.charAt(start) == ')') {
            return emit(start + 1, TokenType.CLOSE, null, 0);
        } else if (unitSpecification.charAt(start) == '√') {
            return emit(start + 1, TokenType.SQUARE_ROOT, null, 0);
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
            if ((c >= '0') && (c <= '9')) {
                value = value * 10 + (c - '0');
                ++current;
            } else {
                break;
            }
        }
        if (current > numberStart) {
            // there were some digits
            return emit(current, TokenType.INTEGER, null, sign * value);
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
     * @param unit prefixed unit value
     * @param value integer value
     * @return new token
     */
    private Token emit(final int after, final TokenType type,
                       final PrefixedUnit unit, final int value) {
        final CharSequence subString = unitSpecification.subSequence(start, after);
        start      = after;
        nextToLast = last;
        last       = new Token(subString, type, unit, value);
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
