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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hipparchus.fraction.Fraction;

/** Parser for units.
 * <p>
 * This fairly basic parser uses recursive descent with the following grammar,
 * where '*' can in fact be either '*', '×' or '.', '/' can be either '/' or '⁄'
 * and '^' can be either '^', "**" or implicit with switch to superscripts,
 * and fraction are either unicode fractions like ½ or ⅞ or the decimal value 0.5.
 * The special cases "n/a" returns a null list. It is intended to manage the
 * special unit {@link Unit.NONE}. The special case "1" returns a singleton with
 * the base term set to "1" and the exponent set to 1. It is intended to manage the
 * special unit {@link Unit.ONE}. This is the only case were a number can appear
 * in a unit, it cannot be combined with other units (i.e. m.1/s is not allowed).
 * </p>
 * <pre>
 *   unit         ::=  "n/a" | "1"  | chain
 *   chain        ::=  operand { ('*' | '/') operand }
 *   operand      ::=  '√' base     | base power
 *   power        ::=  '^' exponent | ε
 *   exponent     ::=  'fraction'   | integer | '(' integer denominator ')'
 *   denominator  ::=  '/' integer  | ε
 *   base         ::=  identifier   | '(' chain ')'
 * </pre>
 * <p>
 * This parses correctly units like MHz, km/√d, kg.m.s⁻¹, µas^⅖/(h**(2)×m)³, km/√(kg.s),
 * √kg*km** (3/2) /(µs^2*Ω⁻⁷), km**0.5/s.
 * </p>
 * <p>
 * Note that we don't accept both square root and power on the same operand, so km/√d³ is
 * refused (but km/√(d³) is accepted).
 * </p>
 * @author Luc Maisonobe
 * @since 11.0
 */
public class Parser {

    /** Private constructor for a utility class.
     */
    private Parser() {
    }

    /** Build the power terms list.
     * @param unitSpecification unit specification to parse
     * @return list of power terms
     */
    public static List<PowerTerm> buildList(final String unitSpecification) {
        if (Unit.NONE.getName().equals(unitSpecification)) {
            // special case for no units
            return null;
        } else if (Unit.ONE.getName().equals(unitSpecification)) {
            // special case for dimensionless unit
            return Collections.singletonList(new PowerTerm(unitSpecification, Fraction.ONE));
        } else {
            final Lexer lexer = new Lexer(unitSpecification);
            final List<PowerTerm> chain = chain(lexer);
            if (lexer.next() != null) {
                throw lexer.generateException();
            }
            return chain;
        }
    }

    /** Parse a chain unit.
     * @param lexer lexer providing tokens
     * @return chain unit
     */
    private static List<PowerTerm> chain(final Lexer lexer) {
        final List<PowerTerm> chain = new ArrayList<>();
        chain.addAll(operand(lexer));
        for (Token token = lexer.next(); token != null; token = lexer.next()) {
            if (checkType(token, TokenType.MULTIPLICATION)) {
                chain.addAll(operand(lexer));
            } else if (checkType(token, TokenType.DIVISION)) {
                chain.addAll(reciprocate(operand(lexer)));
            } else {
                lexer.pushBack();
                break;
            }
        }
        return chain;
    }

    /** Parse an operand.
     * @param lexer lexer providing tokens
     * @return operand term
     */
    private static List<PowerTerm> operand(final Lexer lexer) {
        final Token token = lexer.next();
        if (token == null) {
            throw lexer.generateException();
        }
        if (token.getType() == TokenType.SQUARE_ROOT) {
            return applyExponent(base(lexer), Fraction.ONE_HALF);
        } else {
            lexer.pushBack();
            return applyExponent(base(lexer), power(lexer));
        }
    }

    /** Apply an exponent to a base term.
     * @param base base term
     * @param exponent exponent (may be null)
     * @return term with exponent applied (same as {@code if exponent is null)
     */
    private static List<PowerTerm> applyExponent(final List<PowerTerm> base, final Fraction exponent) {

        if (exponent == null) {
            // no exponent at all, return the base term itself
            return base;
        }

        // combine exponent with existing ones, for example to handles compounds units like m/(kg.s²)³
        final List<PowerTerm> powered = new ArrayList<>(base.size());
        for (final PowerTerm term : base) {
            powered.add(new PowerTerm(term.getBase(), exponent.multiply(term.getExponent())));
        }

        return powered;

    }

    /** Compute the reciprocal a base term.
     * @param base base term
     * @return reciprocal of base term
     */
    private static List<PowerTerm> reciprocate(final List<PowerTerm> base) {

        // reciprocate individual terms
        final List<PowerTerm> reciprocal = new ArrayList<>(base.size());
        for (final PowerTerm term : base) {
            reciprocal.add(new PowerTerm(term.getBase(), term.getExponent().negate()));
        }

        return reciprocal;

    }

    /** Parse a power operation.
     * @param lexer lexer providing tokens
     * @return exponent, or null if no exponent
     */
    private static Fraction power(final Lexer lexer) {
        final Token token = lexer.next();
        if (checkType(token, TokenType.POWER)) {
            return exponent(lexer);
        } else {
            lexer.pushBack();
            return null;
        }
    }

    /** Parse an exponent.
     * @param lexer lexer providing tokens
     * @return exponent
     */
    private static Fraction exponent(final Lexer lexer) {
        final Token token = lexer.next();
        if (checkType(token, TokenType.FRACTION)) {
            return token.getFraction();
        } else if (checkType(token, TokenType.INTEGER)) {
            return new Fraction(token.getInt());
        } else {
            lexer.pushBack();
            accept(lexer, TokenType.OPEN);
            final int num = accept(lexer, TokenType.INTEGER).getInt();
            final int den = denominator(lexer);
            accept(lexer, TokenType.CLOSE);
            return new Fraction(num, den);
        }
    }

    /** Parse a denominator.
     * @param lexer lexer providing tokens
     * @return denominatior
     */
    private static int denominator(final Lexer lexer) {
        final Token token = lexer.next();
        if (checkType(token, TokenType.DIVISION)) {
            return accept(lexer, TokenType.INTEGER).getInt();
        } else  {
            lexer.pushBack();
            return 1;
        }
    }

    /** Parse a base term.
     * @param lexer lexer providing tokens
     * @return base term
     */
    private static List<PowerTerm> base(final Lexer lexer) {
        final Token token = lexer.next();
        if (checkType(token, TokenType.IDENTIFIER)) {
            return Collections.singletonList(new PowerTerm(token.getSubString(), Fraction.ONE));
        } else {
            lexer.pushBack();
            accept(lexer, TokenType.OPEN);
            final List<PowerTerm> chain = chain(lexer);
            accept(lexer, TokenType.CLOSE);
            return chain;
        }
    }

    /** Accept a token.
     * @param lexer lexer providing tokens
     * @param expected expected token type
     * @return accepted token
     */
    private static Token accept(final Lexer lexer, final TokenType expected) {
        final Token token = lexer.next();
        if (!checkType(token, expected)) {
            throw lexer.generateException();
        }
        return token;
    }

    /** Check a token exists and has proper type.
     * @param token token to check
     * @param expected expected token type
     * @return true if token exists and has proper type
     */
    private static boolean checkType(final Token token, final TokenType expected) {
        return token != null && token.getType() == expected;
    }

}
