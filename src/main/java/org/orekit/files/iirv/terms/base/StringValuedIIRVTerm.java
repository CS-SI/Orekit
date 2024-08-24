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
package org.orekit.files.iirv.terms.base;

/**
 * Non-numeric/mutable term in an IIRV Vector represented as a String.
 *
 * @author Nick LaFarge
 * @since 13.0
 */
public class StringValuedIIRVTerm extends IIRVVectorTerm<String> {
    /**
     * Constructs an IIRV Vector Term represented by a long. This representation is used for any numeric terms
     * in the IIRV Vector that do not contain a decimal point.
     *
     * @param pattern Regular expression pattern that validates the term
     * @param value   Value of the term
     * @param length  Length of the term, measured in number of characters in the String representation
     */
    public StringValuedIIRVTerm(final String pattern, final String value, final int length) {
        super(pattern, value, length);
        validateString(toEncodedString());
    }

    /** {@inheritDoc} */
    @Override
    public String toEncodedString(final String termValue) {
        return termValue;
    }
}
