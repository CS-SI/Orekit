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
package org.orekit.utils;

import java.util.Locale;

import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;

/** Formatter used to produce strings from data with high accuracy.
 * <p>
 * When producing test output from coputed data, we don't want to loose
 * internal accuracy when writing doubles so when we parse the output
 * again in another application we recover similar data. We also don't
 * want to have awkward representations like STEP = 1.25000000000000000.
 * </p>
 * <p>
 * This class attempts do this by using adaptive format with the fewer
 * numbers of decimals that allows to preserve the full accuracy (down
 * to one ULP).
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
        return format(value, 1, "%22.15e");
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
        return String.format(STANDARDIZED_LOCALE, DATE_FORMAT,
                             year, month, day, hour, minute,
                             format(seconds, 2, "%012.9f"));
    }

    /** Format a double to string value with high precision.
     * @param value value to format
     * @param minDigitsBeforeSeparator minimum number of digits before decimal separator
     * @param fallbackFormat format to use if simple formats don't work
     * @return formatted value, with all original value accuracy preserved, or null
     * if value is {@code Double.NaN}
     */
    private static String format(final double value, final int minDigitsBeforeSeparator,
                                 final String fallbackFormat) {

        // first try decimal formats with increasing number of digits
        int scale = 1;
        for (int n = 1; n < 15; ++n) {
            scale *= 10;
            final double scaled  = value * scale;
            final long   rounded = (long) FastMath.rint(scaled);
            if (Precision.equals(scaled, rounded, 1)) {
                // the current number of digits is well suited for the value
                final int firstDigit = rounded < 0 ? 1 : 0;
                final StringBuilder builder = new StringBuilder();
                builder.append(rounded);
                while (builder.length() < n + firstDigit + minDigitsBeforeSeparator) {
                    builder.insert(firstDigit, '0');
                }
                builder.insert(builder.length() - n, '.');
                return builder.toString();
            }
        }

        // none of the simple formats worked, fallback to specified format
        return String.format(STANDARDIZED_LOCALE, fallbackFormat, value);

    }

}
