/* Copyright 2002-2016 CS Systèmes d'Information
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
package org.orekit.data;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hipparchus.util.FastMath;

/**
 * Parser for polynomials in IERS tables.
 * <p>
 * IERS conventions tables display polynomial parts using several different formats,
 * like the following ones:
 * </p>
 * <ul>
 *   <li>125.04455501° − 6962890.5431″t + 7.4722″t² + 0.007702″t³ − 0.00005939″t⁴</li>
 *   <li>0.02438175 × t + 0.00000538691 × t²</li>
 *   <li>0''.014506 + 4612''.15739966t + 1''.39667721t^2 - 0''.00009344t^3 + 0''.00001882t^4</li>
 *   <li>-16616.99 + 2004191742.88 t - 427219.05 t^2 - 198620.54 t^3 - 46.05 t^4 + 5.98 t^5</li>
 * </ul>
 * <p>
 * This class parses all these formats and returns the coefficients.
 * </p>
 *
 * @author Luc Maisonobe
 * @see SeriesTerm
 * @see PoissonSeries
 * @see BodiesElements
 */
public class PolynomialParser {

    /** Unit for the coefficients. */
    public enum Unit {

        /** Radians angles. */
        RADIANS(1.0),

        /** Degrees angles. */
        DEGREES(FastMath.toRadians(1.0)),

        /** Arc-seconds angles. */
        ARC_SECONDS(FastMath.toRadians(1.0 / 3600.0)),

        /** Milli arc-seconds angles. */
        MILLI_ARC_SECONDS(FastMath.toRadians(1.0 / 3600000.0)),

        /** Micro arc-seconds angles. */
        MICRO_ARC_SECONDS(FastMath.toRadians(1.0 / 3600000000.0)),

        /** No units. */
        NO_UNITS(1.0);

        /** Multiplication factor to convert to corresponding SI unit. */
        private final double factor;

        /** Simple constructor.
         * @param factor multiplication factor to convert to corresponding SI unit.
         */
        Unit(final double factor) {
            this.factor = factor;
        }

        /** Convert value from instance unit to corresponding SI unit.
         * @param value value in instance unit
         * @return value in SI unit
         */
        public double toSI(final double value) {
            return value * factor;
        }

    }

    /** Constants for various characters that can be used as minus sign. */
    private static final String[] MINUS = new String[] {
        "-",      // unicode HYPHEN-MINUS
        "\u2212"  // unicode MINUS SIGN
    };

    /** Constants for various characters that can be used as plus sign. */
    private static final String[] PLUS = new String[] {
        "+",      // unicode PLUS SIGN
    };

    /** Constants for various characters that can be used as multiplication sign. */
    private static final String[] MULTIPLICATION = new String[] {
        "*",      // unicode ASTERISK
        "\u00d7"  // unicode MULTIPLICATION SIGN
    };

    /** Constants for various characters that can be used as degree unit. */
    private static final String[] DEGREES = new String[] {
        "\u00b0", // unicode DEGREE SIGN
        "\u25e6"  // unicode WHITE BULLET
    };

    /** Constants for various characters that can be used as arc-seconds unit. */
    private static final String[] ARC_SECONDS = new String[] {
        "\u2033", // unicode DOUBLE_PRIME
        "''",     // doubled unicode APOSTROPHE
        "\""      // unicode QUOTATION MARK
    };

    /** Constants for various characters that can be used as powers. */
    private static final String[] SUPERSCRIPTS = new String[] {
        "\u2070", // unicode SUPERSCRIPT ZERO
        "\u00b9", // unicode SUPERSCRIPT ONE
        "\u00b2", // unicode SUPERSCRIPT TWO
        "\u00b3", // unicode SUPERSCRIPT THREE
        "\u2074", // unicode SUPERSCRIPT FOUR
        "\u2075", // unicode SUPERSCRIPT FIVE
        "\u2076", // unicode SUPERSCRIPT SIX
        "\u2077", // unicode SUPERSCRIPT SEVEN
        "\u2078", // unicode SUPERSCRIPT EIGHT
        "\u2079", // unicode SUPERSCRIPT NINE
    };

    /** Constants for various characters that can be used as powers. */
    private static final String[] DIGITS = new String[] {
        "0", // unicode DIGIT ZERO
        "1", // unicode DIGIT ONE
        "2", // unicode DIGIT TWO
        "3", // unicode DIGIT THREE
        "4", // unicode DIGIT FOUR
        "5", // unicode DIGIT FIVE
        "6", // unicode DIGIT SIX
        "7", // unicode DIGIT SEVEN
        "8", // unicode DIGIT EIGHT
        "9", // unicode DIGIT NINE
    };

    /** Regular expression pattern for monomials. */
    private final Pattern pattern;

    /** Matcher for a definition. */
    private Matcher matcher;

    /** Start index for next search. */
    private int next;

    /** Last parsed coefficient. */
    private double parsedCoefficient;

    /** Last parsed power. */
    private int parsedPower;

    /** Unit to use if no unit found while parsing. */
    private final Unit defaultUnit;

    /** Simple constructor.
     * @param freeVariable name of the free variable
     * @param defaultUnit unit to use if no unit found while parsing
     */
    public PolynomialParser(final char freeVariable, final Unit defaultUnit) {

        this.defaultUnit = defaultUnit;

        final String space        = "\\p{Space}*";
        final String unit         = either(quote(merge(DEGREES, ARC_SECONDS)));
        final String sign         = either(quote(merge(MINUS, PLUS)));
        final String integer      = "\\p{Digit}+";
        final String exp          = "[eE]" + zeroOrOne(sign, false) + integer;
        final String fractional   = "\\.\\p{Digit}*" + zeroOrOne(exp, false);
        final String embeddedUnit = group(integer, true) +
                                    group(unit, true) +
                                    group(fractional, true);
        final String appendedUnit = group(either(group(integer + zeroOrOne(fractional, false), false),
                                                 group(fractional, false)),
                                          true) +
                                    zeroOrOne(unit, true);
        final String caretPower   = "\\^" + any(quote(DIGITS));
        final String superscripts = any(quote(SUPERSCRIPTS));
        final String power        = zeroOrOne(either(quote(MULTIPLICATION)), false) +
                                    space + freeVariable +
                                    either(caretPower, superscripts);

        // the capturing groups of the following pattern are:
        //   group  1: sign
        //
        //   when unit is embedded within the coefficient, as in 1''.39667721:
        //   group  2: integer part of the coefficient
        //   group  3: unit
        //   group  4: fractional part of the coefficient
        //
        //   when unit is appended after the coefficient, as in 125.04455501°
        //   group  5: complete coefficient
        //   group  6: unit
        //
        //   group  7: complete power, including free variable, for example "× τ^4" or "× τ⁴"
        //
        //   when caret and regular digits are used, for example τ^4
        //   group  8: only exponent part of the power
        //
        //   when superscripts are used, for example τ⁴
        //   group  9: only exponent part of the power
        pattern = Pattern.compile(space + zeroOrOne(sign, true) + space +
                                  either(group(embeddedUnit, false), group(appendedUnit, false)) +
                                  space + zeroOrOne(power, true));

    }

    /** Merge two lists of markers.
     * @param markers1 first list
     * @param markers2 second list
     * @return merged list
     */
    private String[] merge(final String[] markers1, final String[] markers2) {
        final String[] merged = new String[markers1.length + markers2.length];
        System.arraycopy(markers1, 0, merged, 0, markers1.length);
        System.arraycopy(markers2, 0, merged, markers1.length, markers2.length);
        return merged;
    }

    /** Quote a list of markers.
     * @param markers markers to quote
     * @return quoted markers
     */
    private String[] quote(final String ... markers) {
        final String[] quoted = new String[markers.length];
        for (int i = 0; i < markers.length; ++i) {
            quoted[i] = "\\Q" + markers[i] + "\\E";
        }
        return quoted;
    }

    /** Create a regular expression for a group.
     * @param r raw regular expression to group
     * @param capturing if true, the group is a capturing group
     * @return group expression
     */
    private String group(final CharSequence r, final boolean capturing) {
        return (capturing ? "(" : "(?:") + r + ")";
    }

    /** Create a regular expression for alternative markers.
     * @param markers allowed markers
     * @return regular expression recognizing one marker from the list
     * (the result is a non-capturing group)
     */
    private String either(final CharSequence ... markers) {
        final StringBuilder builder = new StringBuilder();
        for (final CharSequence marker : markers) {
            if (builder.length() > 0) {
                builder.append('|');
            }
            builder.append(marker);
        }
        return group(builder, false);
    }

    /** Create a regular expression for a repeatable part.
     * @param markers allowed markers
     * @return regular expression recognizing any number of markers from the list
     * (the result is a capturing group)
     */
    private String any(final CharSequence ... markers) {
        return group(either(markers) + "*", true);
    }

    /** Create a regular expression for an optional part.
     * @param r optional raw regular expression
     * @param capturing if true, wrap the optional part in a capturing group
     * @return group expression
     */
    private String zeroOrOne(final CharSequence r, final boolean capturing) {
        final String optional = group(r, false) + "?";
        return capturing ? group(optional, true) : optional;
    }

    /** Check if a substring starts with one marker from an array.
     * @param s string containing the substring to check
     * @param offset offset at which substring starts
     * @param markers markes to check for
     * @return index of the start marker, or negative if string does not start
     * with one of the markers
     */
    private int startMarker(final String s, final int offset, final String[] markers) {
        for (int i = 0; i < markers.length; ++i) {
            if (s.startsWith(markers[i], offset)) {
                return i;
            }
        }
        return -1;
    }

    /** Parse a polynomial expression.
     * @param expression polynomial expression to parse
     * @return polynomial coefficients array in increasing degree order, or
     * null if expression is not a recognized polynomial
     */
    public double[] parse(final String expression) {

        final Map<Integer, Double> coefficients = new HashMap<Integer, Double>();
        int maxDegree = -1;
        matcher = pattern.matcher(expression);
        next = 0;
        while (parseMonomial(expression)) {
            maxDegree = FastMath.max(maxDegree, parsedPower);
            coefficients.put(parsedPower, parsedCoefficient);
        }

        if (maxDegree < 0) {
            return null;
        }

        final double[] parsedPolynomial = new double[maxDegree + 1];
        for (Map.Entry<Integer, Double> entry : coefficients.entrySet()) {
            parsedPolynomial[entry.getKey()] = entry.getValue();
        }

        return parsedPolynomial;

    }

    /** Parse next monomial.
     * @param expression polynomial expression to parse
     * @return true if a monomial has been parsed
     */
    private boolean parseMonomial(final String expression) {

        // groups indices
        final int signGroup         = 1;
        final int coeffIntGroup     = 2;
        final int embeddedUnitGroup = 3;
        final int coeffFracGroup    = 4;
        final int coeffGroup        = 5;
        final int appendedUnitGroup = 6;
        final int powerGroup        = 7;
        final int caretGroup        = 8;
        final int superScriptGroup  = 9;

        // advance matcher
        matcher.region(next, matcher.regionEnd());

        if (matcher.lookingAt()) {

            // parse coefficient, with proper sign and unit
            final double sign = startMarker(expression, matcher.start(signGroup), MINUS) >= 0 ? -1 : 1;
            final String coeff;
            final Unit unit;
            if (matcher.start(embeddedUnitGroup) >= 0) {
                // the unit is embedded between coefficient integer and fractional parts
                coeff = matcher.group(coeffIntGroup) + matcher.group(coeffFracGroup);
                if (startMarker(expression, matcher.start(embeddedUnitGroup), DEGREES) >= 0) {
                    unit = Unit.DEGREES;
                } else {
                    // as we recognize only degrees and arc-seconds as explicit settings in the expression
                    // and as we know here the unit as been set, it must be arc seconds
                    unit = Unit.ARC_SECONDS;
                }
            } else {
                // the unit is either after the coefficient or not present at all
                coeff = matcher.group(coeffGroup);
                if (startMarker(expression, matcher.start(appendedUnitGroup), DEGREES) >= 0) {
                    unit = Unit.DEGREES;
                } else if (startMarker(expression, matcher.start(appendedUnitGroup), ARC_SECONDS) >= 0) {
                    unit = Unit.ARC_SECONDS;
                } else {
                    unit = defaultUnit;
                }
            }
            parsedCoefficient = sign * unit.toSI(Double.parseDouble(coeff));

            if (matcher.start(powerGroup) < matcher.end(powerGroup)) {
                // this a power 1 or more term

                if (matcher.start(caretGroup) < matcher.end(caretGroup)) {
                    // general case: x^1234
                    parsedPower = 0;
                    for (int index = matcher.start(caretGroup); index < matcher.end(caretGroup); ++index) {
                        parsedPower = parsedPower * 10 + startMarker(expression, index, DIGITS);
                    }
                } else if (matcher.start(superScriptGroup) < matcher.end(superScriptGroup)) {
                    // general case: x¹²³⁴
                    parsedPower = 0;
                    for (int index = matcher.start(superScriptGroup); index < matcher.end(superScriptGroup); ++index) {
                        parsedPower = parsedPower * 10 + startMarker(expression, index, SUPERSCRIPTS);
                    }
                } else {
                    // special case: x is the same term as either x^1 or x¹
                    parsedPower = 1;
                }

            } else {
                // this is a constant term
                parsedPower = 0;
            }

            next = matcher.end();
            return true;

        } else {

            parsedCoefficient = Double.NaN;
            parsedPower       = -1;
            return false;

        }

    }

}
