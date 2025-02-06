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

import org.orekit.files.iirv.terms.base.DoubleValuedIIRVTerm;

/**
 * 13-character signed component of a velocity vector.
 * <p>
 * Units: m/s
 * <p>
 * Assumed decimal places is three places from the right
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
public class VelocityVectorComponentTerm extends DoubleValuedIIRVTerm {

    /** The length of the IIRV term within the message. */
    public static final int VELOCITY_VECTOR_COMPONENT_TERM_LENGTH = 13;

    /** Regular expression that ensures the validity of string values for this term. */
    public static final String VELOCITY_VECTOR_COMPONENT_TERM_PATTERN = "( |-)\\d{12}";

    /** Number of characters before the end of the string the decimal place occurs. */
    public static final int N_CHARS_AFTER_DECIMAL_PLACE = 3;

    /**
     * Constructor.
     * <p>
     * See {@link DoubleValuedIIRVTerm#DoubleValuedIIRVTerm(String, String, int, int, boolean)}
     *
     * @param value value of component of the velocity vector [m/s]
     */
    public VelocityVectorComponentTerm(final String value) {
        super(VELOCITY_VECTOR_COMPONENT_TERM_PATTERN,
            value,
            VELOCITY_VECTOR_COMPONENT_TERM_LENGTH,
            N_CHARS_AFTER_DECIMAL_PLACE,
            true);
    }

    /**
     * Constructor.
     * <p>
     * See {@link DoubleValuedIIRVTerm#DoubleValuedIIRVTerm(String, double, int, int, boolean)}
     *
     * @param value value of component of the velocity vector [m/s]
     */
    public VelocityVectorComponentTerm(final double value) {
        super(VELOCITY_VECTOR_COMPONENT_TERM_PATTERN,
            value,
            VELOCITY_VECTOR_COMPONENT_TERM_LENGTH,
            N_CHARS_AFTER_DECIMAL_PLACE,
            true);
    }

}
