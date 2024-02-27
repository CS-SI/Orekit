/* Copyright 2002-2024 Thales Alenia Space
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
package org.orekit.gnss;

import org.orekit.time.AbsoluteDate;

/** Quadratic clock model.
 *
 * @author Luc Maisonobe
 * @since 12.1
 *
 */
public class QuadraticClockModel implements ClockModel {

    /** Clock model reference date. */
    private final AbsoluteDate referenceDate;

    /** Constant term. */
    private final double a0;

    /** Linear term. */
    private final double a1;

    /** Quadratic term. */
    private final double a2;

    /** Simple constructor.
     * @param referenceDate reference date
     * @param a0 constant term
     * @param a1 linear term
     * @param a2 quadratic term
     */
    public QuadraticClockModel(final AbsoluteDate referenceDate,
                               final double a0, final double a1, final double a2) {
        this.referenceDate = referenceDate;
        this.a0            = a0;
        this.a1            = a1;
        this.a2            = a2;
    }

    /** Get reference date.
     * @return reference date
     */
    public AbsoluteDate getReferenceDate() {
        return referenceDate;
    }

    /** Get constant term.
     * @return constant term
     */
    public double getA0() {
        return a0;
    }

    /** Get linear term.
     * @return linear term
     */
    public double getA1() {
        return a1;
    }

    /** Get quadratic term.
     * @return quadratic term
     */
    public double getA2() {
        return a2;
    }

    /**  {@inheritDoc} */
    @Override
    public double getOffset(final AbsoluteDate date) {
        final double dt = date.durationFrom(referenceDate);
        return (a2 * dt + a1) * dt + a0;
    }

    /**  {@inheritDoc} */
    @Override
    public double getRate(final AbsoluteDate date) {
        final double dt = date.durationFrom(referenceDate);
        return 2 * a2 * dt + a1;
    }

}
