/* Copyright 2024-2025 The Johns Hopkins University Applied Physics Laboratory
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
package org.orekit.files.iirv.terms.base;

import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;

import java.util.regex.Pattern;

/**
 * Term in an IIRV Vector representing a Long (or integer) value.
 *
 * @author Nick LaFarge
 * @since 13.0
 */
public class LongValuedIIRVTerm extends IIRVVectorTerm<Long> {

    /** Space pattern. */
    private static final Pattern SPACE_PATTERN = Pattern.compile(" ");

    /** True if negative values are permitted, false if the value is positive. */
    private final boolean isSigned;

    /**
     * Constructs an IIRV Vector Term represented by a long. This representation is used for any numeric terms
     * in the IIRV Vector that do not contain a decimal point.
     *
     * @param pattern  Regular expression pattern that validates the term
     * @param value    Value of the term, expressed as a long
     * @param length   LengthC of the term, measured in number of characters in the String representation
     * @param isSigned True if negative values are permitted, false if the value is positive
     */
    public LongValuedIIRVTerm(final String pattern, final long value, final int length, final boolean isSigned) {
        super(pattern, value, length);
        this.isSigned = isSigned;
        validateString(toEncodedString(value));
        validateNumericValue(this.value());
    }

    /**
     * Constructs an IIRV Vector Term represented by a Long. This representation is used for any numeric terms
     * in the IIRV Vector that do not contain a decimal point.
     *
     * @param pattern  Regular expression pattern that validates the term
     * @param value    Value of the term, expressed as a String
     * @param length   Length of the term, measured in number of characters in the String representation
     * @param isSigned True if negative values are permitted, false if the value is positive
     */
    public LongValuedIIRVTerm(final String pattern, final String value, final int length, final boolean isSigned) {
        super(pattern, LongValuedIIRVTerm.computeValueFromString(value), length);
        this.isSigned = isSigned;
        validateString(value);
        validateNumericValue(this.value());
    }

    /**
     * Parses a string as a long, removing any leading spaces.
     *
     * @param value String value of the term.
     * @return the long represented by the argument
     */
    public static long computeValueFromString(final String value) {
        try {
            // Remove spaces (for positive values)
            final String integerString = SPACE_PATTERN.matcher(value).replaceAll("");

            // Cast String to integer
            return Long.parseLong(integerString);
        } catch (NumberFormatException nfe) {
            throw new OrekitIllegalArgumentException(OrekitMessages.IIRV_INVALID_TERM_VALUE, value);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toEncodedString(final Long termValue) {

        // Reserve one character for the sign (if applicable)
        final int signAdjustedStringLength = isSigned ? length() - 1 : length();

        String signCharacter = "";  // Sign character ('' for unsigned number)
        if (isSigned) {
            signCharacter = termValue > 0 ? " " : "-";
        }

        // Pad each string with zeros to reach the desired length
        final String integerString = String.format("%0" + signAdjustedStringLength + "d", FastMath.abs(termValue));

        return signCharacter + integerString;
    }

    /**
     * Convert the underlying {@link #value()} from long to int.
     *
     * @return The value of the term as an int
     */
    public int toInt() {
        return FastMath.toIntExact(value());
    }

    /**
     * Validate a given numerical value to ensure it is not greater than the maximum possible accuracy of this term,
     * and that it does not contain a negative value for a positive term (when {@link #isSigned} is false).
     *
     * @param value long value of this term
     */
    protected void validateNumericValue(final long value) {
        // Compute the number of characters excluding the sign character
        final int n = isSigned ? length() - 1 : length();

        // If the value is greater than the maximum possible value, throw an error
        final double maxPossibleValue = FastMath.pow(10, n);
        if (FastMath.abs(value) >= maxPossibleValue) {
            throw new OrekitIllegalArgumentException(OrekitMessages.IIRV_VALUE_TOO_LARGE, FastMath.abs(value), maxPossibleValue);
        }

        // Throw an error if the signs don't match up
        if (!isSigned && value < 0) {
            throw new OrekitIllegalArgumentException(OrekitMessages.NOT_POSITIVE, value);
        }
    }
}
