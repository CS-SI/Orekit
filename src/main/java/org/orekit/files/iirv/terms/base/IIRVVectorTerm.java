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

import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Defines a term within an IIRV Vector, parameterized by its underlying data type.
 *
 * @param <T> Type of object represented in the term (e.g. integer, long, double, String, ...)
 * @author Nick LaFarge
 * @since 13.0
 */
public abstract class IIRVVectorTerm<T> implements Comparable<IIRVVectorTerm<?>> {

    /**
     * Regular expression pattern that validates the encoded String representation of
     * {@link #value}, as computed by {@link #toEncodedString}.
     */
    private final String pattern;

    /**
     * Length of the term, measured in number characters contained in the encoded String representation of
     * {@link #value}, as computed by {@link #toEncodedString}.
     */
    private final int length;

    /** Value of the term. */
    private final T value;

    /**
     * Constructs an IIRVVectorTerm with a given regular expression pattern, value, and length.
     *
     * @param pattern Regular expression pattern that validates the term
     * @param value   Value of the term
     * @param length  Length of the term, measured in number of characters in the String representation
     */
    protected IIRVVectorTerm(final String pattern, final T value, final int length) {
        this.pattern = pattern;
        this.length = length;
        this.value = value;
    }

    /**
     * Convert an IIRV term value into the encoded String representation, as it would appear in the IIRV message.
     *
     * @param termValue Value of the term
     * @return Encoded String representing of the inputted IIRV term it appears in the IIRV message
     */
    public abstract String toEncodedString(T termValue);

    /**
     * Converts the stored {@link #value} of the IIRV term into the encoded String representation, as it would appear
     * in the IIRV message.
     *
     * @return Encoded String representing of the value of the stored vector term, as it would appear in the
     * IIRV message
     */
    public String toEncodedString() {
        return toEncodedString(value);
    }

    @Override
    public int compareTo(final IIRVVectorTerm<?> o) {
        return this.toEncodedString().compareTo(o.toEncodedString());
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IIRVVectorTerm)) {
            return false;
        }
        final IIRVVectorTerm<?> that = (IIRVVectorTerm<?>) o;
        return this.compareTo(that) == 0;  // Equals if the encoded strings are the same
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(pattern, toEncodedString());
    }

    /**
     * Validate a string value against the vector term, ensuring that it is the proper length and matches
     * the specified regular expression pattern.
     *
     * @param valueString String to validate against the regular expression pattern
     */
    protected void validateString(final String valueString) {
        // Check length of string (should be captured by the regex, but this is a more helpful error message)
        if (valueString.length() != length) {
            throw new OrekitIllegalArgumentException(OrekitMessages.INCONSISTENT_NUMBER_OF_ELEMENTS, length, valueString.length());
        }

        if (!Pattern.compile(this.pattern).matcher(valueString).matches()) { // Match the pattern
            throw new OrekitIllegalArgumentException(OrekitMessages.IIRV_INVALID_TERM_VALUE, valueString);
        }
    }

    /**
     * Gets the value of the term in the IIRV vector.
     *
     * @return value of the term in the IIRV vector
     */
    public T value() {
        return value;
    }

    /**
     * Gets the length of the term.
     * <p>
     * The length is measured in number characters contained in the encoded String representation of
     * {@link #value}, as computed by {@link #toEncodedString}.
     *
     * @return Length of the term
     */
    public int length() {
        return length;
    }
}
