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
 * 4-character mission-specific support identification code (SIC).
 * <p>
 * Valid values: 0000-9999.
 *
 * @author Nick LaFarge
 * @since 13.0
 */
public class SupportIdCodeTerm extends LongValuedIIRVTerm {

    /** The length of the IIRV term within the message. */
    public static final int SUPPORT_ID_TERM_LENGTH = 4;

    /** Regular expression that ensures the validity of string values for this term (0000-9999). */
    public static final String SUPPORT_ID_TERM_PATTERN = "\\d{4}";

    /**
     * Constructor.
     * <p>
     * See {@link LongValuedIIRVTerm#LongValuedIIRVTerm(String, String, int, boolean)}
     *
     * @param value value of the support ID term
     */
    public SupportIdCodeTerm(final String value) {
        super(SUPPORT_ID_TERM_PATTERN, value, SUPPORT_ID_TERM_LENGTH, false);
    }

    /**
     * Constructor.
     * <p>
     * See {@link LongValuedIIRVTerm#LongValuedIIRVTerm(String, long, int, boolean)}
     *
     * @param value value of the support ID term
     */
    public SupportIdCodeTerm(final long value) {
        super(SUPPORT_ID_TERM_PATTERN, value, SUPPORT_ID_TERM_LENGTH, false);
    }
}
