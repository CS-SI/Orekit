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

import org.orekit.files.iirv.terms.base.LongValuedIIRVTerm;

/**
 * 13-character signed component of a position vector.
 * <p>
 * Units: m
 * <p>
 * Valid values:
 * <ul>
 * <li> Character 1: ' ' or '-'
 * <li> Character 2-12: Any integer 0-9
 * </ul>
 *
 * @author Nick LaFarge
 * @since 13.0
 */
public class PositionVectorComponentTerm extends LongValuedIIRVTerm {

    /** The length of the IIRV term within the message. */
    public static final int POSITION_VECTOR_COMPONENT_TERM_LENGTH = 13;

    /** Regular expression that ensures the validity of string values for this term. */
    public static final String POSITION_VECTOR_COMPONENT_TERM_PATTERN = "( |-)\\d{12}";

    /**
     * Constructor.
     * <p>
     * See {@link LongValuedIIRVTerm#LongValuedIIRVTerm(String, long, int, boolean)}
     *
     * @param value value of the position vector component
     */
    public PositionVectorComponentTerm(final long value) {
        super(POSITION_VECTOR_COMPONENT_TERM_PATTERN, value, POSITION_VECTOR_COMPONENT_TERM_LENGTH, true);
    }

    /**
     * Constructor.
     * <p>
     * See {@link LongValuedIIRVTerm#LongValuedIIRVTerm(String, String, int, boolean)}
     *
     * @param value value of the position vector component
     */
    public PositionVectorComponentTerm(final String value) {
        super(POSITION_VECTOR_COMPONENT_TERM_PATTERN, value, POSITION_VECTOR_COMPONENT_TERM_LENGTH, true);
    }

    /**
     * Initializes a PositionVectorComponentTerm by rounding a floating point number to the nearest integer.
     * See {@link LongValuedIIRVTerm#LongValuedIIRVTerm(String, long, int, boolean)}
     *
     * @param value value of the position vector component
     */
    public PositionVectorComponentTerm(final double value) {
        super(POSITION_VECTOR_COMPONENT_TERM_PATTERN, Math.round(value), POSITION_VECTOR_COMPONENT_TERM_LENGTH, true);
    }
}
