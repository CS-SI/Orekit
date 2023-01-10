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
package org.orekit.utils;

import java.util.Locale;

import org.hipparchus.util.RyuDouble;

/** Formatter used to produce strings from data with high accuracy.
 * <p>
 * When producing test output from computed data, we want the shortest
 * decimal representation of a floating point number that maintains
 * round-trip safety. That is, a correct parser can recover the exact
 * original number.
 * </p>
 * <p>
 * For efficiency, this class uses the {@link RyuDouble Ryū} algorithm
 * for producing shortest string representation with round-trip safety.
 * </p>
 * @author Luc Maisonobe
 * @since 11.0
 */
public class AccurateFormatter {

    /**
     * Standardized locale to use, to ensure files can be exchanged without
     * internationalization issues.
     */
    public static final Locale STANDARDIZED_LOCALE = Locale.US;

    /** String format used for dates. **/
    private static final String DATE_FORMAT = "%04d-%02d-%02dT%02d:%02d:%s";

    /** Low switch level for exponential format in dates (will never be reached due to {@link #LOW_TRUNCATION}). */
    private static final int LOW_EXP = -18;

    /** Truncation level for seconds, to avoid scientific format). */
    private static final double LOW_TRUNCATION = 1.0e-15;

    /** Private constructor for a utility class.
     */
    private AccurateFormatter() {
        // nothing to do
    }

    /** Format a double number.
     * @param value number to format
     * @return number formatted to full accuracy
     */
    public static String format(final double value) {
        return RyuDouble.doubleToString(value);
    }

    /** Format a date.
     * @param year year
     * @param month month
     * @param day day
     * @param hour hour
     * @param minute minute
     * @param seconds seconds
     * @return date formatted to full accuracy
     */
    public static String format(final int year, final int month, final int day,
                                final int hour, final int minute, final double seconds) {
        final double truncated = seconds < LOW_TRUNCATION ? 0.0 : seconds;
        final String s = RyuDouble.doubleToString(truncated, LOW_EXP, RyuDouble.DEFAULT_HIGH_EXP);
        return String.format(STANDARDIZED_LOCALE, DATE_FORMAT,
                             year, month, day,
                             hour, minute, s.charAt(1) == '.' ? "0" + s : s);
    }

}
