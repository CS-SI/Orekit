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
 * 2-character body number/vehicle identification code (VIC).
 * <p>
 * Valid values: 01-99.
 *
 * @author Nick LaFarge
 * @since 13.0
 */
public class VehicleIdCodeTerm extends LongValuedIIRVTerm {

    /** Default VIC set to 1. */
    public static final VehicleIdCodeTerm DEFAULT = new VehicleIdCodeTerm("01");

    /** The length of the IIRV term within the message. */
    public static final int VEHICLE_ID_TERM_LENGTH = 2;

    /** Regular expression to check that vehicle identification codes are 01-99 (00 is not a valid entry). */
    public static final String VEHICLE_ID_TERM_PATTERN = "(?:0[1-9]|[1-9]\\d)";

    /**
     * Constructor.
     * <p>
     * See {@link LongValuedIIRVTerm#LongValuedIIRVTerm(String, String, int, boolean)}
     *
     * @param value value of the vehicle ID code term
     */
    public VehicleIdCodeTerm(final String value) {
        super(VEHICLE_ID_TERM_PATTERN, value, VEHICLE_ID_TERM_LENGTH, false);
    }

    /**
     * Constructor.
     * <p>
     * See {@link LongValuedIIRVTerm#LongValuedIIRVTerm(String, long, int, boolean)}
     *
     * @param value value of the vehicle ID code term
     */
    public VehicleIdCodeTerm(final long value) {
        super(VEHICLE_ID_TERM_PATTERN, value, VEHICLE_ID_TERM_LENGTH, false);
    }
}
