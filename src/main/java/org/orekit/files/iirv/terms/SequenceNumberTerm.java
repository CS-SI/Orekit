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
import org.orekit.files.iirv.terms.base.LongValuedIIRVTerm;

/**
 * 3-character sequence number counter that is incremented for each vector in a set of vector data on a per-station per
 * transmission basis.
 * <p>
 * Valid values: 000-999.
 *
 * @author Nick LaFarge
 * @since 13.0
 */
public class SequenceNumberTerm extends LongValuedIIRVTerm {

    /** The length of the IIRV term within the message. */
    public static final int SEQUENCE_NUMBER_TERM_LENGTH = 3;

    /** Maximum value of an IIRV sequence number. */
    public static final int MAX_SEQUENCE_NUMBER = 999;

    /** Regular expression that ensures the validity of string values for this term (integer 000-999). */
    public static final String SEQUENCE_NUMBER_TERM_PATTERN = "\\d{3}";

    /**
     * Constructor.
     * <p>
     * See {@link LongValuedIIRVTerm#LongValuedIIRVTerm(String, String, int, boolean)}
     *
     * @param value value of the sequence number
     */
    public SequenceNumberTerm(final String value) {
        super(SEQUENCE_NUMBER_TERM_PATTERN, value, SEQUENCE_NUMBER_TERM_LENGTH, false);
    }

    /**
     * Constructor.
     * <p>
     * See {@link LongValuedIIRVTerm#LongValuedIIRVTerm(String, long, int, boolean)}
     *
     * @param value value of the sequence number
     */
    public SequenceNumberTerm(final long value) {
        super(SEQUENCE_NUMBER_TERM_PATTERN, value, SEQUENCE_NUMBER_TERM_LENGTH, false);

        if (value > MAX_SEQUENCE_NUMBER) {
            throw new OrekitIllegalArgumentException(OrekitMessages.IIRV_EXCEEDS_MAX_VECTORS, value);
        }
    }
}
