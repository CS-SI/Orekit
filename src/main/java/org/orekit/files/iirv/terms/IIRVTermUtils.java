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
package org.orekit.files.iirv.terms;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.iirv.IIRVVector;
import org.orekit.files.iirv.terms.base.IIRVVectorTerm;

/**
 * Utilities class for {@link IIRVVectorTerm} subclasses.
 *
 * @author Nick LaFarge
 * @since 13.0
 */
public final class IIRVTermUtils {

    /** Private constructor. */
    private IIRVTermUtils() {
    }

    /**
     * Add padding characters to a string.
     *
     * @param string           string to pad
     * @param c                padding character
     * @param size             desired size
     * @param addPaddingToLeft if true, the resulting string is right justified (i.e. the padding character is added to
     *                         the left of the string)
     * @return padded String
     */
    public static String addPadding(final String string,
                                    final char c,
                                    final int size,
                                    final boolean addPaddingToLeft) {

        if (size <= 0) {
            throw new OrekitException(OrekitMessages.NOT_STRICTLY_POSITIVE, size);
        }
        final StringBuilder padding = new StringBuilder();
        for (int i = 0; i < size; i++) {
            padding.append(c);
        }

        if (addPaddingToLeft) {
            final String concatenated = padding + string;
            final int l = concatenated.length();
            return concatenated.substring(l - size, l);
        }

        return (string + padding).substring(0, size);

    }

    /**
     * Converts a list of {@link IIRVVectorTerm} instances to a String for a single line of an {@link IIRVVector}.
     *
     * @param terms terms to parse/convert
     * @return String containing each of the inputted terms
     */
    public static String iirvTermsToLineString(final IIRVVectorTerm<?>... terms) {
        return iirvTermsToLineStringSplitByTerm("", terms);
    }


    /**
     * Converts a list of {@link IIRVVectorTerm} instances to a String for a single line of an {@link IIRVVector},
     * where each term in the line is split by a specified delimiter.
     * <p>
     * For real IIRV vector, the deliminator is always empty; it is only used when creating human-readable forms
     * to more readily identify specific terms within a given message.
     *
     * @param delimiter delimiter to insert between each IIRV vector term
     * @param terms     terms to parse/convert
     * @return String containing each of the inputted terms
     */
    public static String iirvTermsToLineStringSplitByTerm(final String delimiter, final IIRVVectorTerm<?>... terms) {
        final StringBuilder lineString = new StringBuilder();
        for (IIRVVectorTerm<?> term : terms) {
            lineString.append(term.toEncodedString());
            if (!delimiter.isEmpty()) {
                lineString.append(delimiter);
            }
        }
        return lineString.toString();
    }

}
