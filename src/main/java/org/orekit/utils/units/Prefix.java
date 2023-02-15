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

/** Multiplicative prefixes.
 * @author Luc Maisonobe
 * @since 11.0
 */
enum Prefix {

    /** Septillion. */
    YOTTA("Y", 1.0e24),

    /** Sextillion. */
    ZETTA("Z", 1.0e21),

    /** Quintillion. */
    EXA("E", 1.0e18),

    /** Quadrillion. */
    PETA("P", 1.0e15),

    /** Trillion. */
    TERA("T", 1.0e12),

    /** Billion. */
    GIGA("G", 1.0e9),

    /** Million. */
    MEGA("M", 1.0e6),

    /** Thousand. */
    KILO("k", 1.0e3),

    /** Hundred. */
    HECTO("h", 1.0e2),

    /** Ten. */
    DECA("da", 1.0e1),

    /** Tenth. */
    DECI("d", 1.0e-1),

    /** Hundredth. */
    CENTI("c", 1.0e-2),

    /** Thousandth. */
    MILLI("m", 1.0e-3),

    /** Millionth.
     * <p>
     * The symbol used here is the standard SI one: µ (U+00B5, MICRO SIGN)
     * </p>
     */
    MICRO("µ", 1.0e-6),

    /** Millionth.
     * <p>
     * The symbol used here is an alternate one: μ (U+03BC, GREEK SMALL LETTER MU)
     * </p>
     */
    MICRO_ALTERNATE("μ", 1.0e-6),

    /** Billionth. */
    NANO("n", 1.0e-9),

    /** Trillionth. */
    PICO("p", 1.0e-12),

    /** Quadrillionth. */
    FEMTO("f", 1.0e-15),

    /** Quintillionth. */
    ATTO("a", 1.0e-18),

    /** Sextillionth. */
    ZEPTO("z", 1.0e-21),

    /** Septillionth. */
    YOCTO("y", 1.0e-24);

    /** Symbol. */
    private String symbol;

    /** Multiplication factor. */
    private double factor;

    /** Simple constructor.
     * @param symbol symbol
     * @param factor multiplication factor
     */
    Prefix(final String symbol, final double factor) {
        this.symbol = symbol;
        this.factor = factor;
    }

    /** Get the prefix symbol.
     * @return prefix symbol
     */
    public String getSymbol() {
        return symbol;
    }

    /** Get the prefix multiplication factor.
     * @return prefix multiplication factor
     */
    public double getFactor() {
        return factor;
    }

}
