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

/** Parser for units.
 * <p>
 * This fairly basic parser uses recursive descent with the following grammar,
 * where '*' can in fact be either '*', '×' or '.', '/' can be either '/' or '⁄'
 * and '^' can be either '^', "**" or implicit with switch to superscripts.
 * The special case "n/a" corresponds to {@link PredefinedUnit#NONE}.
 * </p>
 * <pre>
 *   unit         → "n/a"        | chain
 *   chain        → operand operation
 *   operand      → '√' simple   | simple power
 *   operation    → '*' chain    | '/' chain    | ε
 *   power        → '^' exponent | ε
 *   exponent     → integer      | '(' integer denominator ')'
 *   denominator  → '/' integer  | ε
 *   simple       → predefined   | '(' chain ')'
 * </pre>
 * <p>
 * This parses correctly units like MHz, km/√d, kg.m.s⁻¹, µas^(2/5)/(h**(2)×m)³, km/√(kg.s), √kg*km**  (3/2) /(µs^2*Ω⁻⁷).
 * Note that we don't accept both square root and power on the same operand, so km/√d³ is refused.
 * </p>
 * @author Luc Maisonobe
 * @since 11.0
 */
class Parser {

    /** Private constructor for a utility class.
     */
    private Parser() {
    }

    /** Parse a string.
     * @param unitSpecification unit specification to parse
     * @return parsed unit
     */
    public static Unit parse(final String unitSpecification) {
        if (Unit.NONE.getName().equals(unitSpecification)) {
            // special case
            return Unit.NONE;
        } else {
            final Lexer lexer = new Lexer(unitSpecification);
            final Unit parsed = chain(lexer);
            if (lexer.next() != null) {
                throw lexer.generateException();
            }
            return parsed.alias(unitSpecification);
        }
    }

    /** Parse a chain unit.
     * @param lexer lexer providing tokens
     * @return chain unit
     */
    private static Unit chain(final Lexer lexer) {
        return operation(operand(lexer), lexer);
    }

    /** Parse an operand.
     * @param lexer lexer providing tokens
     * @return operand unit
     */
    private static Unit operand(final Lexer lexer) {
        final Token token = lexer.next();
        if (token == null) {
            throw lexer.generateException();
        }
        if (token.getType() == TokenType.SQUARE_ROOT) {
            return simple(lexer).power(null, Fraction.ONE_HALF);
        } else {
            lexer.pushBack();
            return simple(lexer).power(null, power(lexer));
        }
    }

    /** Parse an operation.
     * @param lhs left hand side unit
     * @param lexer lexer providing tokens
     * @return simple unit
     */
    private static Unit operation(final Unit lhs, final Lexer lexer) {
        final Token token = lexer.next();
        if (checkType(token, TokenType.MULTIPLICATION)) {
            return lhs.multiply(null, chain(lexer));
        } else if (token != null && token.getType() == TokenType.DIVISION) {
            return lhs.divide(null, chain(lexer));
        } else {
            lexer.pushBack();
            return lhs;
        }
    }

    /** Parse a power operation.
     * @param lexer lexer providing tokens
     * @return exponent
     */
    private static Fraction power(final Lexer lexer) {
        final Token token = lexer.next();
        if (checkType(token, TokenType.POWER)) {
            return exponent(lexer);
        } else {
            lexer.pushBack();
            return Fraction.ONE;
        }
    }

    /** Parse an exponent.
     * @param lexer lexer providing tokens
     * @return exponent
     */
    private static Fraction exponent(final Lexer lexer) {
        final Token token = lexer.next();
        if (checkType(token, TokenType.INTEGER)) {
            return new Fraction(token.getValue());
        } else {
            lexer.pushBack();
            accept(lexer, TokenType.OPEN);
            final int num = accept(lexer, TokenType.INTEGER).getValue();
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
            return accept(lexer, TokenType.INTEGER).getValue();
        } else  {
            lexer.pushBack();
            return 1;
        }
    }

    /** Parse a simple unit.
     * @param lexer lexer providing tokens
     * @return simple unit
     */
    private static Unit simple(final Lexer lexer) {
        final Token token = lexer.next();
        if (checkType(token, TokenType.PREFIXED_UNIT)) {
            return token.getPrefixedUnit();
        } else {
            lexer.pushBack();
            accept(lexer, TokenType.OPEN);
            final Unit chain = chain(lexer);
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
