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
 * 8-character mass of the satellite in kilograms with a resolution to the nearest tenth of a kilogram; assumed decimal
 * point is one place from the right. Must contain all zeros if not used.
 * <p>
 * Units: kg
 * <p>
 * Valid values:<br>
 * <ul>
 * <li>0 to 999.99</li>
 * <li>[String]: Any integer 0-9 for characters 1-8 </li>
 * </ul>
 *
 * @author Nick LaFarge
 * @since 13.0
 */
public class MassTerm extends DoubleValuedIIRVTerm {

    /** MassTerm contains all zeros when not used. */
    public static final MassTerm UNUSED = new MassTerm(0);

    /** The length of the IIRV term within the message. */
    public static final int MASS_TERM_LENGTH = 8;

    /** Regular expression that ensures the validity of string values for this term. */
    public static final String MASS_TERM_PATTERN = "\\d{8}";

    /**
     * Constructor.
     * <p>
     * See {@link DoubleValuedIIRVTerm#DoubleValuedIIRVTerm(String, String, int, int, boolean)}
     *
     * @param value mass value [kg]
     */
    public MassTerm(final String value) {
        super(MASS_TERM_PATTERN, value, MASS_TERM_LENGTH, 1, false);
    }

    /**
     * Constructor.
     * <p>
     * See {@link DoubleValuedIIRVTerm#DoubleValuedIIRVTerm(String, double, int, int, boolean)}
     *
     * @param value mass value [kg]
     */
    public MassTerm(final double value) {
        super(MASS_TERM_PATTERN, value, MASS_TERM_LENGTH, 1, false);
    }
}
