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
import org.orekit.files.iirv.terms.IIRVTermUtils;

import java.util.regex.Pattern;

/**
 * Term in an IIRV Vector representing a double value.
 *
 * @author Nick LaFarge
 * @since 13.0
 */
public class DoubleValuedIIRVTerm extends IIRVVectorTerm<Double> {

    /** Space pattern. */
    private static final Pattern SPACE_PATTERN = Pattern.compile(" ");

    /**
     * Number of characters before the end of the encoded String the decimal place is assumed to occur.
     * <p>
     * For example, for {@code nCharsAfterDecimalPlace=2} and {@code length=4}, then 12.34
     * is encoded to "1234".
     */
    private final int nCharsAfterDecimalPlace;

    /** True if negative values are permitted, false if the value is positive. */
    private final boolean isSigned;

    /**
     * Constructs an IIRV Vector Term represented by a double. This representation is used for any numeric terms
     * in the IIRV Vector that contain a decimal point.
     *
     * @param pattern                 Regular expression pattern that validates the term
     * @param value                   Value of the term, expressed as a double
     * @param length                  Length of the term, measured in number of characters in the String representation
     * @param nCharsAfterDecimalPlace Number of characters before the end of the encoded String the decimal place is
     *                                assumed to occur.
     * @param isSigned                True if negative values are permitted, false if the value is positive
     */
    public DoubleValuedIIRVTerm(final String pattern, final double value, final int length, final int nCharsAfterDecimalPlace, final boolean isSigned) {
        super(pattern, value, length);
        this.nCharsAfterDecimalPlace = nCharsAfterDecimalPlace;
        this.isSigned = isSigned;

        // Validate input data
        validateString(toEncodedString(value));
        validateOverflow(this.value());
    }

    /**
     * Constructs an IIRV Vector Term represented by a double from a given String. This representation is used for any
     * numeric terms in the IIRV Vector that contain a decimal point.
     *
     * @param pattern                 Regular expression pattern that validates the term
     * @param value                   Value of the term, expressed as a String
     * @param length                  Length of the term, measured in number of characters in the String representation
     * @param nCharsAfterDecimalPlace Number of characters before the end of {@code value} the decimal place is
     *                                assumed to occur.
     * @param isSigned                True if negative values are permitted, false if the value is positive
     */
    public DoubleValuedIIRVTerm(final String pattern, final String value, final int length, final int nCharsAfterDecimalPlace, final boolean isSigned) {
        super(pattern, DoubleValuedIIRVTerm.computeValueFromString(value, nCharsAfterDecimalPlace), length);
        this.nCharsAfterDecimalPlace = nCharsAfterDecimalPlace;
        this.isSigned = isSigned;

        // Validate input data
        validateString(value);
        validateOverflow(this.value());
    }

    /**
     * Compute the double value of the term from a given String.
     *
     * @param value                   String value to convert to a double
     * @param nCharsAfterDecimalPlace Number of characters before the end of {@code value} the decimal place is
     *                                assumed to occur.
     * @return Double value corresponding to the {@code value} String argument
     */
    public static double computeValueFromString(final String value, final int nCharsAfterDecimalPlace) {
        try {
            String intStr = value;

            // Remove spaces (for positive values)
            intStr = SPACE_PATTERN.matcher(intStr).replaceAll("");

            // Return if there are no characters after the decimal place
            if (nCharsAfterDecimalPlace == 0) {
                return Integer.parseInt(intStr);
            }

            // Get the sign: negative if the first character is '-'
            final int sign = intStr.charAt(0) == '-' ? -1 : 1;

            // Get value before/after the decimal place
            final int beforeDecimalPlace = Integer.parseInt(intStr.substring(0, intStr.length() - nCharsAfterDecimalPlace));
            final int afterDecimalPlace = Integer.parseInt(intStr.substring(intStr.length() - nCharsAfterDecimalPlace));

            // Turn into a double by dividing the n numbers that appear after the decimal places by 10^n
            final double unsignedValue = FastMath.abs(beforeDecimalPlace) + afterDecimalPlace / FastMath.pow(10.0, nCharsAfterDecimalPlace);

            // Return the resulting double with the correct sign
            return sign * unsignedValue;
        } catch (NumberFormatException e) {
            throw new OrekitIllegalArgumentException(OrekitMessages.IIRV_INVALID_TERM_VALUE, value);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toEncodedString(final Double termValue) {
        // Reserve one character for the sign (if applicable)
        final int signAdjustedStringLength = isSigned ? length() - 1 : length();

        // Round the number to the specified number of decimal places
        final double roundedNum = FastMath.round(termValue * FastMath.pow(10, nCharsAfterDecimalPlace)) / FastMath.pow(10, nCharsAfterDecimalPlace);

        // Format the absolute value of the rounded number with specified integer and decimal lengths
        String formattedStr = String.format("%0" + signAdjustedStringLength + "." + nCharsAfterDecimalPlace + "f", FastMath.abs(roundedNum));

        // Remove the decimal point
        formattedStr = formattedStr.replace(".", "");

        // Add leading zeros for cases where the number has less than the max number of integer digits
        if (formattedStr.length() < signAdjustedStringLength) {
            formattedStr = IIRVTermUtils.addPadding(formattedStr, '0', signAdjustedStringLength, true);
        }

        // If the resulting String is all zero, then always use a positive sign to avoid encoding negative zero
        final int numNonzeroCharacters = formattedStr.replace("0", "").length();

        // Sign character ("" for unsigned number)
        String signCharacter = "";
        if (isSigned) {
            if (termValue >= 0 || numNonzeroCharacters == 0) {
                signCharacter = " ";
            } else {
                signCharacter = "-";
            }
        }

        return signCharacter + formattedStr;
    }

    /**
     * Validate that there is a sufficient number of characters available in the encoded String representation to
     * represent the value of the given double value.
     *
     * @param value Double value to check against the String encoding parameters
     */
    void validateOverflow(final double value) {
        // Compute the number of characters, excluding the sign character and all characters after the decimal place
        int n = length() - nCharsAfterDecimalPlace;
        if (isSigned) {
            n--;
        }

        // If the value is greater than the maximum possible value, throw an error
        final double maxPossibleValue = FastMath.pow(10, n);
        if (FastMath.abs(value) >= maxPossibleValue) {
            throw new OrekitIllegalArgumentException(OrekitMessages.IIRV_VALUE_TOO_LARGE, FastMath.abs(value), maxPossibleValue);
        }
    }
}
