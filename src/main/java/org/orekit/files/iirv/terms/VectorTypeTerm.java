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
 * 1-character type of vector specified in the message.
 * <p>
 * Valid values:
 * <ul>
 * <li> 1 = Free flight (routine on-orbit)
 * <li> 2 = Forced (special orbit update)
 * <li> 3 = Spare
 * <li> 4 = Maneuver ignition
 * <li> 5 = Maneuver cutoff
 * <li> 6 = Reentry
 * <li> 7 = Powered flight
 * <li> 8 = Stationary
 * <li> 9 = Spare
 * </ul>
 *
 * @author Nick LaFarge
 * @since 13.0
 */
public class VectorTypeTerm extends LongValuedIIRVTerm {

    /** Free flight (routine on-orbit) VectorType. */
    public static final VectorTypeTerm FREE_FLIGHT = new VectorTypeTerm("1");

    /** Forced VectorType. */
    public static final VectorTypeTerm FORCED = new VectorTypeTerm("2");

    /** Spare VectorType: 3. */
    public static final VectorTypeTerm SPARE3 = new VectorTypeTerm("3");

    /** Maneuver ignition VectorType. */
    public static final VectorTypeTerm MANEUVER_IGNITION = new VectorTypeTerm("4");

    /** Maneuver cutoff VectorType. */
    public static final VectorTypeTerm MANEUVER_CUTOFF = new VectorTypeTerm("5");

    /** Reentry VectorType. */
    public static final VectorTypeTerm REENTRY = new VectorTypeTerm("6");

    /** Powered flight VectorType. */
    public static final VectorTypeTerm POWERED_FLIGHT = new VectorTypeTerm("7");

    /** Stationary VectorType. */
    public static final VectorTypeTerm STATIONARY = new VectorTypeTerm("8");

    /** Spare VectorType: 9. */
    public static final VectorTypeTerm SPARE9 = new VectorTypeTerm("9");

    /** The length of the IIRV term within the message. */
    public static final int VECTOR_TYPE_TERM_LENGTH = 1;

    /** Regular expression that ensures the validity of string values for this term. */
    public static final String VECTOR_TYPE_TERM_PATTERN = "[1-9]";

    /**
     * Constructor.
     * <p>
     * See {@link LongValuedIIRVTerm#LongValuedIIRVTerm(String, String, int, boolean)}
     *
     * @param value value of the vector type term
     */
    public VectorTypeTerm(final String value) {
        super(VECTOR_TYPE_TERM_PATTERN, value, VECTOR_TYPE_TERM_LENGTH, false);
    }

    /**
     * Constructor.
     * <p>
     * See {@link LongValuedIIRVTerm#LongValuedIIRVTerm(String, long, int, boolean)}
     *
     * @param value value of the vector type term
     */
    public VectorTypeTerm(final long value) {
        super(VECTOR_TYPE_TERM_PATTERN, value, VECTOR_TYPE_TERM_LENGTH, false);
    }
}
