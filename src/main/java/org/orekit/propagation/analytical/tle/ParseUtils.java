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
package org.orekit.propagation.analytical.tle;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

/** Utility class for TLE parsing, including alpha-5 TLE satellites IDs handling.
 * <p>
 * Alpha-5 extends the range of existing 5 digits TLE satellite numbers
 * by allowing the first digit to be an upper case letter, ignoring 'I'
 * and 'O' to avoid confusion with numbers '1' and '0'.
 * </p>
 * @see <a href="https://www.space-track.org/documentation#tle-alpha5>TLE-alpha5</a>
 * @author Mark rutten
 */
class ParseUtils  {

    /** Letter-number map for satellite number. */
    private static final int MAX_NUMERIC_SATNUM = 99999;

    /** Letter-number map for satellite number. */
    private static final Map<Character, Integer> ALPHA5_NUMBERS;

    /** Number-letter map for satellite number. */
    private static final Map<Integer, Character> ALPHA5_LETTERS;

    /** Scaling factor for alpha5 numbers. */
    private static final int ALPHA5_SCALING = 10000;

    static {
        // Generate maps between TLE satellite alphabetic characters and integers.
        final List<Character> alpha5Letters =
                        Arrays.asList('A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J',
                                      'K', 'L', 'M', 'N', 'P', 'Q', 'R', 'S', 'T',
                                      'U', 'V', 'W', 'X', 'Y', 'Z');
        ALPHA5_NUMBERS = new HashMap<>(alpha5Letters.size());
        ALPHA5_LETTERS = new HashMap<>(alpha5Letters.size());
        for (int i = 0; i < alpha5Letters.size(); ++i) {
            ALPHA5_NUMBERS.put(alpha5Letters.get(i), i + 10);
            ALPHA5_LETTERS.put(i + 10, alpha5Letters.get(i));
        }
    }

    /** Private constructor for a utility class. */
    private ParseUtils() {
        // nothing to do
    }

    /** Build an alpha5 satellite number.
     * @param satelliteNumber satellite number, that may exceed the 99999 limit
     * @param name parameter name
     * @return satellite number in alpha5 representation
     */
    public static String buildSatelliteNumber(final int satelliteNumber, final String name) {
        if (satelliteNumber > MAX_NUMERIC_SATNUM) {
            final int highDigits = satelliteNumber / ALPHA5_SCALING;
            final int lowDigits = satelliteNumber - highDigits * ALPHA5_SCALING;

            final Character alpha = ALPHA5_LETTERS.get(highDigits);
            if (alpha == null) {
                throw new OrekitException(OrekitMessages.TLE_INVALID_PARAMETER,
                                          satelliteNumber, name, "null");
            }
            return alpha + addPadding(name, lowDigits, '0', 4, true, satelliteNumber);
        } else {
            return addPadding(name, satelliteNumber, '0', 5, true, satelliteNumber);
        }
    }

    /** Add padding characters before an integer.
     * @param name parameter name
     * @param k integer to pad
     * @param c padding character
     * @param size desired size
     * @param rightJustified if true, the resulting string is
     * right justified (i.e. space are added to the left)
     * @param satelliteNumber satellite number
     * @return padded string
     */
    public static String addPadding(final String name, final int k, final char c,
                                    final int size, final boolean rightJustified,
                                    final int satelliteNumber) {
        return addPadding(name, Integer.toString(k), c, size, rightJustified, satelliteNumber);
    }

    /** Add padding characters to a string.
     * @param name parameter name
     * @param string string to pad
     * @param c padding character
     * @param size desired size
     * @param rightJustified if true, the resulting string is
     * right justified (i.e. space are added to the left)
     * @param satelliteNumber satellite number
     * @return padded string
     */
    public static String addPadding(final String name, final String string, final char c,
                                    final int size, final boolean rightJustified,
                                    final int satelliteNumber) {

        if (string.length() > size) {
            throw new OrekitException(OrekitMessages.TLE_INVALID_PARAMETER,
                                      satelliteNumber, name, string);
        }

        final StringBuilder padding = new StringBuilder();
        for (int i = 0; i < size; ++i) {
            padding.append(c);
        }

        if (rightJustified) {
            final String concatenated = padding + string;
            final int l = concatenated.length();
            return concatenated.substring(l - size, l);
        }

        return (string + padding).substring(0, size);

    }

    /** Parse a double.
     * @param line line to parse
     * @param start start index of the first character
     * @param length length of the string
     * @return value of the double
     */
    public static double parseDouble(final String line, final int start, final int length) {
        final String string = line.substring(start, start + length).trim();
        return string.length() > 0 ? Double.parseDouble(string.replace(' ', '0')) : 0;
    }

    /** Parse an integer.
     * @param line line to parse
     * @param start start index of the first character
     * @param length length of the string
     * @return value of the integer
     */
    public static int parseInteger(final String line, final int start, final int length) {
        final String field = line.substring(start, start + length).trim();
        return field.length() > 0 ? Integer.parseInt(field.replace(' ', '0')) : 0;
    }

    /** Parse a satellite number.
     * @param line line to parse
     * @param start start index of the first character
     * @param length length of the string
     * @return value of the integer
     */
    public static int parseSatelliteNumber(final String line, final int start, final int length) {
        String field = line.substring(start, start + length);
        int satelliteNumber;

        final Integer alpha = ALPHA5_NUMBERS.get(field.charAt(0));
        if (alpha != null) {
            satelliteNumber = Integer.parseInt(field.substring(1));
            satelliteNumber += alpha * ALPHA5_SCALING;
        } else {
            field = field.trim();
            satelliteNumber = field.length() > 0 ? Integer.parseInt(field.replace(' ', '0')) : 0;
        }
        return satelliteNumber;
    }

    /** Parse a year written on 2 digits.
     * @param line line to parse
     * @param start start index of the first character
     * @return value of the year
     */
    public static int parseYear(final String line, final int start) {
        final int year = 2000 + parseInteger(line, start, 2);
        return (year > 2056) ? (year - 100) : year;
    }

}
