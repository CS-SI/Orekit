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
 * 8-character dimensionless solar reflectivity coefficient.
 * <p>
 * s = "-" for negative sign or blank for positive sign,
 * assumed decimal point is six places from the right. May contain all zeros if not used.
 * <p>
 * Units: dimensionless
 * <p>
 * Valid values
 * <ul>
 * <li>-99.99999 to 99.99999</li>
 * <li>"<code>sxxxxxxx</code>: <code>s</code>: ' ' (ASCII space) or '-', <code>x</code>: Any integer 0-9</li>
 * </ul>
 *
 * @author Nick LaFarge
 * @since 13.0
 */
public class SolarReflectivityCoefficientTerm extends DoubleValuedIIRVTerm {

    /** SolarReflectivityCoefficientTerm contains all zeros when not used. */
    public static final SolarReflectivityCoefficientTerm UNUSED = new SolarReflectivityCoefficientTerm(0);

    /** The length of the IIRV term within the message. */
    public static final int SOLAR_REFLECTIVITY_COEFFICIENT_TERM_LENGTH = 8;

    /** Regular expression that ensures the validity of string values for this term. */
    public static final String SOLAR_REFLECTIVITY_COEFFICIENT_TERM_PATTERN = "( |-)\\d{7}";

    /** Number of characters before the end of the string the decimal place occurs. */
    public static final int N_CHARS_AFTER_DECIMAL_PLACE = 6;

    /**
     * Constructor.
     * <p>
     * See {@link DoubleValuedIIRVTerm#DoubleValuedIIRVTerm(String, String, int, int, boolean)}
     *
     * @param value value of the solar reflectivity coefficient term (dimensionless)
     */
    public SolarReflectivityCoefficientTerm(final String value) {
        super(SOLAR_REFLECTIVITY_COEFFICIENT_TERM_PATTERN,
            value,
            SOLAR_REFLECTIVITY_COEFFICIENT_TERM_LENGTH,
            N_CHARS_AFTER_DECIMAL_PLACE,
            true);
    }

    /**
     * Constructor.
     * <p>
     * See {@link DoubleValuedIIRVTerm#DoubleValuedIIRVTerm(String, double, int, int, boolean)}
     *
     * @param value value of the solar reflectivity coefficient term (dimensionless).
     */
    public SolarReflectivityCoefficientTerm(final double value) {
        super(SOLAR_REFLECTIVITY_COEFFICIENT_TERM_PATTERN,
            value,
            SOLAR_REFLECTIVITY_COEFFICIENT_TERM_LENGTH,
            N_CHARS_AFTER_DECIMAL_PLACE,
            true);
    }
}
