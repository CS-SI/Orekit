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

/** Unit token.
 * @author Luc Maisonobe
 * @since 11.0
 */
class Token {

    /** String value. */
    private final CharSequence subString;

    /** Token type. */
    private final TokenType type;

    /** Integer value. */
    private final int integer;

    /** Fraction value. */
    private final Fraction fraction;

    /** Build a token.
     * @param subString substring corresponding to the token
     * @param type token type
     * @param integer integer value
     * @param fraction fraction value
     */
    Token(final CharSequence subString, final TokenType type, final int integer, final Fraction fraction) {
        this.subString = subString;
        this.type      = type;
        this.integer   = integer;
        this.fraction  = fraction;
    }

    /** Get the substring corresponding to the token.
     * @return substring corresponding to the token
     */
    public CharSequence getSubString() {
        return subString;
    }

    /** Get the token type.
     * @return token type
     */
    public TokenType getType() {
        return type;
    }

    /** Get the integer value (numerator in case of fraction).
     * @return integer value
     */
    public int getInt() {
        return integer;
    }

    /** Get the fraction value.
     * @return fraction value
     */
    public Fraction getFraction() {
        return fraction;
    }

}
