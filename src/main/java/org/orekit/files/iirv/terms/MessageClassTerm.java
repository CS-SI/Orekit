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
 * 2-character IIRV message class.
 * <p>
 * Valid values:
 * <ul>
 * <li> 10 = IIRV (nominal)
 * <li> 15 = IIRV (inflight update)
 * </ul>
 *
 * @author Nick LaFarge
 * @since 13.0
 */
public class MessageClassTerm extends LongValuedIIRVTerm {

    /** Nominal MessageClass. */
    public static final MessageClassTerm NOMINAL = new MessageClassTerm("10");

    /** Inflight update MessageClass. */
    public static final MessageClassTerm INFLIGHT_UPDATE = new MessageClassTerm("15");

    /** Length of the term (number of characters). */
    public static final int MESSAGE_CLASS_TERM_LENGTH = 2;

    /** Regular expression that ensures the validity of string values for this term. */
    public static final String MESSAGE_CLASS_TERM_PATTERN = "(10|15)";

    /**
     * Constructor.
     * <p>
     * See {@link LongValuedIIRVTerm#LongValuedIIRVTerm(String, String, int, boolean)}
     *
     * @param value value of the message class term
     */
    public MessageClassTerm(final String value) {
        super(MESSAGE_CLASS_TERM_PATTERN, value, MESSAGE_CLASS_TERM_LENGTH, false);
    }

    /**
     * Constructor.
     * <p>
     * See {@link LongValuedIIRVTerm#LongValuedIIRVTerm(String, long, int, boolean)}
     *
     * @param value value of the message class term
     */
    public MessageClassTerm(final long value) {
        super(MESSAGE_CLASS_TERM_PATTERN, value, MESSAGE_CLASS_TERM_LENGTH, false);
    }
}
