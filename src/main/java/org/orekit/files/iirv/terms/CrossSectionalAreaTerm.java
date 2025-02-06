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
 * 5-character average satellite cross-sectional area in square meters with a resolution to the nearest hundredth of a
 * square meter.
 * <p>
 * Assumed decimal point is two places from the right. Must contain all zeros if not used.
 * <p>
 * Units: m^2
 * <p>
 * Valid values: <br>
 * <ul>
 * <li>0 to 999.99</li>
 * <li>[String]: Any integer 0-9 for characters 1-5 </li>
 * </ul>
 *
 * @author Nick LaFarge
 * @since 13.0
 */
public class CrossSectionalAreaTerm extends DoubleValuedIIRVTerm {

    /** CrossSectionalAreaTerm contains all zeros when not used. */
    public static final CrossSectionalAreaTerm UNUSED = new CrossSectionalAreaTerm(0);

    /** The length of the IIRV term within the message. */
    public static final int CROSS_SECTIONAL_AREA_TERM_LENGTH = 5;

    /** Regular expression that ensures the validity of string values for this term. */
    public static final String CROSS_SECTIONAL_AREA_TERM_PATTERN = "\\d{5}";

    /**
     * Constructor.
     * <p>
     * See {@link DoubleValuedIIRVTerm#DoubleValuedIIRVTerm(String, String, int, int, boolean)}
     *
     * @param value value of the cross-sectional area (m^2)
     */
    public CrossSectionalAreaTerm(final String value) {
        super(CROSS_SECTIONAL_AREA_TERM_PATTERN, value, CROSS_SECTIONAL_AREA_TERM_LENGTH, 2, false);
    }

    /**
     * Constructor.
     * <p>
     * See {@link DoubleValuedIIRVTerm#DoubleValuedIIRVTerm(String, double, int, int, boolean)}
     *
     * @param value value of the cross-sectional area (m^2)
     */
    public CrossSectionalAreaTerm(final double value) {
        super(CROSS_SECTIONAL_AREA_TERM_PATTERN, value, CROSS_SECTIONAL_AREA_TERM_LENGTH, 2, false);
    }
}
