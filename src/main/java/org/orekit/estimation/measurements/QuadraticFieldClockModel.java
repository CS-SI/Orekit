/* Copyright 2022-2025 Thales Alenia Space
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
package org.orekit.estimation.measurements;

import org.hipparchus.CalculusFieldElement;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.FieldClockOffset;

/** Quadratic clock model.
 *
 * @param <T> type of the field elements
 * @author Luc Maisonobe
 * @since 12.1
 *
 */
public class QuadraticFieldClockModel<T extends CalculusFieldElement<T>> {

    /** Clock model reference date. */
    private final FieldAbsoluteDate<T> referenceDate;

    /** Constant term. */
    private final T a0;

    /** Linear term. */
    private final T a1;

    /** Quadratic term. */
    private final T a2;

    /** Simple constructor.
     * @param referenceDate reference date
     * @param a0 constant term
     * @param a1 linear term
     * @param a2 quadratic term
     */
    public QuadraticFieldClockModel(final FieldAbsoluteDate<T> referenceDate,
                                    final T a0, final T a1, final T a2) {
        this.referenceDate = referenceDate;
        this.a0            = a0;
        this.a1            = a1;
        this.a2            = a2;
    }

    /** Get the clock offset at date.
     * @param date date at which offset is requested
     * @return clock offset at specified date
     */
    public FieldClockOffset<T> getOffset(final FieldAbsoluteDate<T> date) {
        final T dt = date.durationFrom(referenceDate);
        return new FieldClockOffset<>(date,
                                      a2.multiply(dt).add(a1).multiply(dt).add(a0),
                                      a2.multiply(dt).multiply(2).add(a1),
                                      a2.multiply(2));
    }

}
