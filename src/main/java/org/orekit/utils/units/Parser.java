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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hipparchus.fraction.Fraction;
import org.hipparchus.util.FastMath;

/** Parser for units.
 * <p>
 * This fairly basic parser uses recursive descent with the following grammar,
 * where '*' can in fact be either '*', '×', '.', or '·', '/' can be either
 * '/' or '⁄' and '^' can be either '^', "**" or implicit with switch to superscripts,
 * and fraction are either unicode fractions like ½ or ⅞ or the decimal value 0.5.
 * The special cases "n/a" returns a null list. It is intended to manage the
 * special unit {@link Unit#NONE}.
 * </p>
 * <pre>
 *   unit         ::=  "n/a" | chain
 *   chain        ::=  operand { ('*' | '/') operand }
 *   operand      ::=  integer | integer term | term
 *   term         ::=  '√' base | base power
 *   power        ::=  '^' exponent | ε
 *   exponent     ::=  'fraction'   | integer | '(' integer denominator ')'
 *   denominator  ::=  '/' integer  | ε
 *   base         ::=  identifier | '(' chain ')'
 * </pre>
 * <p>
 * This parses correctly units like MHz, km/√d, kg.m.s⁻¹, µas^⅖/(h**(2)×m)³, km/√(kg.s),
 * √kg*km** (3/2) /(µs^2*Ω⁻⁷), km**0.5/s, #/y, 2rev/d², 1/s.
 * </p>
 * <p>
 * Note that we don't accept combining square roots and power on the same operand; km/√d³
 * is refused (but km/√(d³) is accepted). We also accept a single integer prefix and
 * only at the start of the specification.
 * </p>
 * @author Luc Maisonobe
 * @since 11.0
 */
public class Parser {

    /** Private constructor for a utility class.
     */
    private Parser() {
    }

    /** Build the list of terms corresponding to a units specification.
     * @param unitsSpecification units specification to parse
     * @return parse tree
     */
    public static List<PowerTerm> buildTermsList(final String unitsSpecification) {
        if (Unit.NONE.getName().equals(unitsSpecification)) {
            // special case for no units
            return null;
        } else {
            final Lexer lexer = new Lexer(unitsSpecification);
            final List<PowerTerm> chain = chain(lexer);
            if (lexer.next() != null) {
                throw lexer.generateException();
            }
            return chain;
        }
    }

    /** Parse a units chain.
     * @param lexer lexer providing tokens
     * @return parsed units chain
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
     * @return parsed operand
     */
    private static List<PowerTerm> operand(final Lexer lexer) {
        final Token token1 = lexer.next();
        if (token1 == null) {
            throw lexer.generateException();
        }
        if (checkType(token1, TokenType.INTEGER)) {
            final int scale = token1.getInt();
            final Token token2 = lexer.next();
            lexer.pushBack();
            if (token2 == null ||
                checkType(token2, TokenType.MULTIPLICATION) ||
                checkType(token2, TokenType.DIVISION)) {
                return Collections.singletonList(new PowerTerm(scale, "1", Fraction.ONE));
            } else {
                return applyScale(term(lexer), scale);
            }
        } else {
            lexer.pushBack();
            return term(lexer);
        }
    }

    /** Parse a term.
     * @param lexer lexer providing tokens
     * @return parsed term
     */
    private static List<PowerTerm> term(final Lexer lexer) {
        final Token token = lexer.next();
        if (token.getType() == TokenType.SQUARE_ROOT) {
            return applyExponent(base(lexer), Fraction.ONE_HALF);
        } else {
            lexer.pushBack();
            return applyExponent(base(lexer), power(lexer));
        }
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
            return Collections.singletonList(new PowerTerm(1.0, token.getSubString(), Fraction.ONE));
        } else {
            lexer.pushBack();
            accept(lexer, TokenType.OPEN);
            final List<PowerTerm> chain = chain(lexer);
            accept(lexer, TokenType.CLOSE);
            return chain;
        }
    }

    /** Compute the reciprocal a base term.
     * @param base base term
     * @return reciprocal of base term
     */
    private static List<PowerTerm> reciprocate(final List<PowerTerm> base) {

        // reciprocate individual terms
        final List<PowerTerm> reciprocal = new ArrayList<>(base.size());
        for (final PowerTerm term : base) {
            reciprocal.add(new PowerTerm(1.0 / term.getScale(), term.getBase(), term.getExponent().negate()));
        }

        return reciprocal;

    }

    /** Apply a scaling factor to a base term.
     * @param base base term
     * @param scale scaling factor
     * @return term with scaling factor applied (same as {@code base} if {@code scale} is 1)
     */
    private static List<PowerTerm> applyScale(final List<PowerTerm> base, final int scale) {

        if (scale == 1) {
            // no scaling at all, return the base term itself
            return base;
        }

        // combine scaling factor with first term
        final List<PowerTerm> powered = new ArrayList<>(base.size());
        boolean first = true;
        for (final PowerTerm term : base) {
            if (first) {
                powered.add(new PowerTerm(scale * term.getScale(), term.getBase(), term.getExponent()));
                first = false;
            } else {
                powered.add(term);
            }
        }

        return powered;

    }

    /** Apply an exponent to a base term.
     * @param base base term
     * @param exponent exponent (may be null)
     * @return term with exponent applied (same as {@code base} if exponent is null)
     */
    private static List<PowerTerm> applyExponent(final List<PowerTerm> base, final Fraction exponent) {

        if (exponent == null || exponent.equals(Fraction.ONE)) {
            // return the base term itself
            return base;
        }

        // combine exponent with existing ones, for example to handles compounds units like m/(kg.s²)³
        final List<PowerTerm> powered = new ArrayList<>(base.size());
        for (final PowerTerm term : base) {
            final double poweredScale;
            if (exponent.isInteger()) {
                poweredScale = FastMath.pow(term.getScale(), exponent.getNumerator());
            } else if (Fraction.ONE_HALF.equals(exponent)) {
                poweredScale = FastMath.sqrt(term.getScale());
            } else {
                poweredScale = FastMath.pow(term.getScale(), exponent.doubleValue());
            }
            powered.add(new PowerTerm(poweredScale, term.getBase(), exponent.multiply(term.getExponent())));
        }

        return powered;

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
