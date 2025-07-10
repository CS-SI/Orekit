/* Copyright 2022-2025 Luc Maisonobe
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
package org.orekit.files.rinex.utils;

import org.hipparchus.util.FastMath;
import org.orekit.files.rinex.RinexFile;
import org.orekit.files.rinex.section.RinexComment;

/** Utilities for RINEX various messages files.
 * @author Luc Maisonobe
 * @since 12.0
 *
 */
public class ParsingUtils {

    /** Private constructor.
     * <p>This class is a utility class, it should neither have a public
     * nor a default constructor. This private constructor prevents
     * the compiler from generating one automatically.</p>
     */
    private ParsingUtils() {
    }

    /** Parse a comment.
     * @param lineNumber line number
     * @param line line to parse
     * @param rinexFile rinex file
     */
    public static void parseComment(final int lineNumber, final String line, final RinexFile<?> rinexFile) {
        rinexFile.addComment(new RinexComment(lineNumber, parseString(line, 0, 60)));
    }

    /**
     * Parse a double value.
     * @param line line to parse
     * @param startIndex start index
     * @param size size of the value
     * @return the parsed value
     */
    public static double parseDouble(final String line, final int startIndex, final int size) {
        final String subString = parseString(line, startIndex, size);
        if (subString == null || subString.isEmpty()) {
            return Double.NaN;
        } else {
            return Double.parseDouble(subString.replace('D', 'E').trim());
        }
    }

    /**
     * Parse an integer value.
     * @param line line to parse
     * @param startIndex start index
     * @param size size of the value
     * @return the parsed value
     */
    public static int parseInt(final String line, final int startIndex, final int size) {
        final String subString = parseString(line, startIndex, size);
        if (subString == null || subString.isEmpty()) {
            return 0;
        } else {
            return Integer.parseInt(subString.trim());
        }
    }

    /**
     * Parse a long integer value.
     * @param line line to parse
     * @param startIndex start index
     * @param size size of the value
     * @return the parsed value
     * @since 14.0
     */
    public static long parseLong(final String line, final int startIndex, final int size) {
        final String subString = parseString(line, startIndex, size);
        if (subString == null || subString.isEmpty()) {
            return 0;
        } else {
            return Long.parseLong(subString.trim());
        }
    }

    /**
     * Parse a string value.
     * @param line line to parse
     * @param startIndex start index
     * @param size size of the value
     * @return the parsed value
     */
    public static String parseString(final String line, final int startIndex, final int size) {
        if (line.length() > startIndex) {
            return line.substring(startIndex, FastMath.min(line.length(), startIndex + size)).trim();
        } else {
            return null;
        }
    }

    /** Convert a 2 digits year to a complete year.
     * @param yy year between 0 and 99
     * @return complete year
     * @since 12.0
     */
    public static int convert2DigitsYear(final int yy) {
        return yy >= 80 ? (yy + 1900) : (yy + 2000);
    }

}
