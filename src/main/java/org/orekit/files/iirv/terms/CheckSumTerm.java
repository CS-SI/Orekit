/* Copyright 2024 The Johns Hopkins University Applied Physics Laboratory
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
package org.orekit.files.iirv.terms;


import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.iirv.IIRVVector;
import org.orekit.files.iirv.terms.base.IIRVVectorTerm;
import org.orekit.files.iirv.terms.base.LongValuedIIRVTerm;

/**
 * Three-character checksum to validate message.
 * <p>
 * Calculated by summing the decimal equivalent of the preceding characters in the line, counting spaces as 0 and
 * negative signs as 1:
 * <ul>
 * <li> 0 through 9 = face value
 * <li> Minus (-)   = 1
 * <li> ASCII Space = 0
 * </ul>
 * <p>
 * Valid Values: 000-999
 *
 * @author Nick LaFarge
 * @since 13.0
 */
public class CheckSumTerm extends LongValuedIIRVTerm {

    /** The length of the IIRV term within the message. */
    public static final int CHECK_SUM_TERM_LENGTH = 3;

    /** Regular expression that ensures the validity of string values for this term. */
    public static final String CHECK_SUM_TERM_PATTERN = "\\d{3}";

    /**
     * Constructor.
     * <p>
     * See {@link LongValuedIIRVTerm#LongValuedIIRVTerm(String, String, int, boolean)}
     *
     * @param value value of the coordinate system term
     */
    public CheckSumTerm(final String value) {
        super(CHECK_SUM_TERM_PATTERN, value.replace(IIRVVector.LINE_SEPARATOR, ""), CHECK_SUM_TERM_LENGTH, false);
    }

    /**
     * Constructor.
     * <p>
     * See {@link LongValuedIIRVTerm#LongValuedIIRVTerm(String, long, int, boolean)}
     *
     * @param value value of the coordinate system term
     */
    public CheckSumTerm(final long value) {
        super(CHECK_SUM_TERM_PATTERN, value, CHECK_SUM_TERM_LENGTH, false);
    }

    /**
     * Constructs an IIRV checksum from a series of IIRVTerm instances.
     *
     * @param terms IIRVTerms to compute checksum
     * @return newly created CheckSum instance
     */
    public static CheckSumTerm fromIIRVTerms(final IIRVVectorTerm<?>... terms) {
        final String lineString = IIRVTermUtils.iirvTermsToLineString(terms);  // Compute the line string for the inputs
        final int checkSumValue = computeChecksum(lineString); // Compute the checksum value from the String
        return new CheckSumTerm(checkSumValue);  // Create the CheckSumTerm object
    }

    /**
     * Computes the sum of the decimal equivalent of characters in the line, counting spaces as 0 and
     * negative signs as 1.
     *
     * @param input input string to compute checksum from
     * @return computed checksum integer value
     */
    public static int computeChecksum(final String input) {
        // Compute the sum based on the characters
        int sum = 0;
        for (int i = 0; i < input.length(); i++) {
            final char c = input.charAt(i);
            final int valueToAdd;
            if (Character.isDigit(c)) {
                valueToAdd = Character.getNumericValue(c);  // Convert the digit character to its numeric value
            } else if (c == ' ') {
                valueToAdd = 0;  // Space counts as 0
            } else if (c == '-') {
                valueToAdd = 1;  // Sign character counts as 1
            } else {
                throw new OrekitIllegalArgumentException(OrekitMessages.IIRV_INVALID_TERM_VALUE, c);
            }
            sum += valueToAdd;  // Increment the sum
        }
        return sum;
    }

    /**
     * Validate a line's embedded checksum value.
     *
     * @param line string line of an IIRV message (including checksum as the final three characters)
     * @return true if the derived and embedded checksum values are equal
     */
    public static boolean validateLineCheckSum(final String line) {
        // Don't include carriage/line returns in checksum
        final String strippedLine = line.replace(IIRVVector.LINE_SEPARATOR, "");

        // Separate message from checksum
        final String message = strippedLine.substring(0, strippedLine.length() - 3);
        final String checkSum = strippedLine.substring(strippedLine.length() - 3);

        return CheckSumTerm.computeChecksum(message) == Integer.parseInt(checkSum);
    }

    /**
     * Validate the checksum from a line based on the object's checksum integer value.
     *
     * @param line string line of an IIRV message (including checksum as the final three characters)
     * @return true if the extracted checksum value matches this object's integer value
     */
    public boolean validateAgainstLineString(final String line) {
        final String strippedLine = line.replace(IIRVVector.LINE_SEPARATOR, "");
        final String message = strippedLine.substring(0, strippedLine.length() - 3);
        final int computedChecksum = computeChecksum(message);
        return value() == computedChecksum;
    }

}
