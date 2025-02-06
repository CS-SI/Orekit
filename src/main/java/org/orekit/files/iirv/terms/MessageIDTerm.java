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

import org.orekit.files.iirv.terms.base.LongValuedIIRVTerm;

/**
 * A unique 7-character number used to reference the IIRV message.
 * <p>
 * Valid values: 0000000 to 9999999
 *
 * @author Nick LaFarge
 * @since 13.0
 */
public class MessageIDTerm extends LongValuedIIRVTerm {

    /** The length of the IIRV term within the message. */
    public static final int MESSAGE_ID_TERM_LENGTH = 7;

    /** Regular expression that ensures the validity of string values for this term. */
    public static final String MESSAGE_ID_TERM_PATTERN = "\\d{7}";

    /**
     * Constructor.
     * <p>
     * See {@link LongValuedIIRVTerm#LongValuedIIRVTerm(String, String, int, boolean)}
     *
     * @param value value of the message ID term
     */
    public MessageIDTerm(final String value) {
        super(MESSAGE_ID_TERM_PATTERN, value, MESSAGE_ID_TERM_LENGTH, false);
    }

    /**
     * Constructor.
     * <p>
     * See {@link LongValuedIIRVTerm#LongValuedIIRVTerm(String, long, int, boolean)}
     *
     * @param value value of the message ID term
     */
    public MessageIDTerm(final long value) {
        super(MESSAGE_ID_TERM_PATTERN, value, MESSAGE_ID_TERM_LENGTH, false);
    }
}
