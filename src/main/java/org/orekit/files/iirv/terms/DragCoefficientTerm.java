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

import org.orekit.files.iirv.terms.base.DoubleValuedIIRVTerm;

/**
 * 4-character dimensionless drag coefficient.
 * <p>
 * Assumed decimal point is two places from the right. Must contain all  zeros if not used.
 * <p>
 * Units: dimensionless
 * <p>
 * Valid values:
 * <ul>
 * <li> 0 to 99.99</li>
 * <li>"<code>xxxx</code>", <code>x</code>: Any integer 0-9</li>
 * </ul>
 *
 * @author Nick LaFarge
 * @since 13.0
 */
public class DragCoefficientTerm extends DoubleValuedIIRVTerm {

    /** DragCoefficientTerm contains all zeros when not used. */
    public static final DragCoefficientTerm UNUSED = new DragCoefficientTerm(0);

    /** The length of the IIRV term within the message. */
    public static final int DRAG_COEFFICIENT_TERM_LENGTH = 4;

    /** Regular expression that ensures the validity of string values for this term. */
    public static final String DRAG_COEFFICIENT_TERM_PATTERN = "\\d{4}";

    /**
     * Constructor.
     * <p>
     * See {@link DoubleValuedIIRVTerm#DoubleValuedIIRVTerm(String, String, int, int, boolean)}
     *
     * @param value value of the drag coefficient term (dimensionless)
     */
    public DragCoefficientTerm(final String value) {
        super(DRAG_COEFFICIENT_TERM_PATTERN, value, DRAG_COEFFICIENT_TERM_LENGTH, 2, false);
    }

    /**
     * Constructor.
     * <p>
     * See {@link DoubleValuedIIRVTerm#DoubleValuedIIRVTerm(String, double, int, int, boolean)}
     *
     * @param value value of the drag coefficient term (dimensionless)
     */
    public DragCoefficientTerm(final double value) {
        super(DRAG_COEFFICIENT_TERM_PATTERN, value, DRAG_COEFFICIENT_TERM_LENGTH, 2, false);
    }
}
