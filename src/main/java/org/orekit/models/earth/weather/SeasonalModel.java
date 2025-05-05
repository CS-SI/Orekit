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
package org.orekit.models.earth.weather;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.SinCos;
import org.hipparchus.util.FieldSinCos;

/** Seasonal model used in Global Pressure Temperature models.
 * @see "Landskron, D. & Böhm, J. J Geod (2018)
 *      VMF3/GPT3: refined discrete and empirical troposphere mapping functions
 *      92: 349. https://doi.org/10.1007/s00190-017-1066-2"
 * @author Luc Maisonobe
 * @since 12.1
 */
class SeasonalModel {

    /** Constant. */
    private final double a0;

    /** Annual cosine amplitude. */
    private final double a1;

    /** Annual sine amplitude. */
    private final double b1;

    /** Semi-annual cosine amplitude. */
    private final double a2;

    /** Semi-annual sine amplitude. */
    private final double b2;

    /** Simple constructor.
     * @param a0 constant
     * @param a1 annual cosine amplitude
     * @param b1 annual sine amplitude
     * @param a2 semi-annual cosine amplitude
     * @param b2 semi-annual sine amplitude
     */
    SeasonalModel(final double a0, final double a1, final double b1, final double a2, final double b2) {
        this.a0 = a0;
        this.a1 = a1;
        this.b1 = b1;
        this.a2 = a2;
        this.b2 = b2;
    }

    /** Evaluate a model for some day.
     * @param sc1 sine and cosine of yearly harmonic term
     * @param sc2 sine and cosine of bi-yearly harmonic term
     * @return model value at specified day
     * @since 13.0
     */
    public double evaluate(final SinCos sc1, final SinCos sc2) {
        return a0 + a1 * sc1.cos() + b1 * sc1.sin() + a2 * sc2.cos() + b2 * sc2.sin();
    }

    /** Evaluate a model for some day.
     * @param <T> type of the field elements
     * @param sc1 sine and cosine of yearly harmonic term
     * @param sc2 sine and cosine of bi-yearly harmonic term
     * @return model value at specified day
     * @since 13.0
     */
    public <T extends CalculusFieldElement<T>> T evaluate(final FieldSinCos<T> sc1, final FieldSinCos<T> sc2) {
        return sc1.cos().multiply(a1).
            add(sc1.sin().multiply(b1)).
            add(sc2.cos().multiply(a2)).
            add(sc2.sin().multiply(b2)).
            add(a0);
    }

}
