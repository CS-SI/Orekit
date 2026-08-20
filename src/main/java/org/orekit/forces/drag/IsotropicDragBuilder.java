/* Copyright 2022-2026 Luc Maisonobe
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
package org.orekit.forces.drag;

import org.hipparchus.util.FastMath;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriversSequenceBuilder;

/** Builder for {@link IsotropicDrag} allowing to set coefficients on different time spans.
 * @author Luc Maisonobe
 * @since 14.0
 */
public class IsotropicDragBuilder {

   /** Parameters scaling factor.
     * <p>
     * We use a power of 2 to avoid numeric noise introduction
     * in the multiplications/divisions sequences.
     * </p>
     */
    private static final double SCALE = FastMath.scalb(1.0, -3);

    /** Underlying builder. */
    private final ParameterDriversSequenceBuilder builder;

    /** Cross section (m²). */
    private final double crossSection;

    /** Constructor for an initially empty builder, with min/max set to ±∞.
     * @param crossSection Surface (m²)
     */
    public IsotropicDragBuilder(final double crossSection) {
        this(crossSection, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    /** Constructor for an initially empty builder.
     * <p>
     * The builder {@link #build() build()} method cannot be called right after construction.
     * Some drag coefficients must be set before, either by calling the
     * {@link #addDragCoeff(double) addDragCoeff(dragCoeff)} method once, or by calling the
     * {@link #addDragCoeff(double, AbsoluteDate, AbsoluteDate)
     * addDragCoeff(dragCoeff, earliestValidityDate, latestValidityDate)} method as many times
     * as needed to cover the usage range before the {@link #build() build()} method can be called.
     * </p>
     * @param crossSection Surface (m²)
     * @param dragCoeffMin minimum value of drag coefficient
     * @param dragCoeffMax maximum value of drag coefficient
     */
    public IsotropicDragBuilder(final double crossSection, final double dragCoeffMin, final double dragCoeffMax) {
        this.builder      = new ParameterDriversSequenceBuilder(DragSensitive.DRAG_COEFFICIENT,
                                                                SCALE, dragCoeffMin, dragCoeffMax);
        this.crossSection = crossSection;
    }

    /** Add a coefficient throughout timeline.
     * <p>
     * Calling this method is equivalent to call
     * {@code addDragCoeff(dragCoeff, AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY)}.
     * </p>
     * @param dragCoeff drag coefficient
     * @return the instance itself, allowing use of the fluent interface pattern
     */
    public IsotropicDragBuilder addDragCoeff(final double dragCoeff) {
        builder.addReferenceValue(dragCoeff);
        return this;
    }

    /** Add a coefficient valid for a time span.
     * @param dragCoeff drag coefficient
     * @param earliestValidityDate date after which the coefficient is valid
     * @param latestValidityDate date before which the coefficient is valid
     * @return the instance itself, allowing use of the fluent interface pattern
     */
    public IsotropicDragBuilder addDragCoeff(final double dragCoeff,
                                             final AbsoluteDate earliestValidityDate,
                                             final AbsoluteDate latestValidityDate) {
        builder.addReferenceValue(dragCoeff, earliestValidityDate, latestValidityDate);
        return this;
    }

    /** Build a drag model.
     * <p>
     * If only one coefficient has been set, its name will be {@link DragSensitive#DRAG_COEFFICIENT}.
     * If several coefficients have been set, their names will be built by prepending
     * {@link ParameterDriversSequenceBuilder#SPAN_PREFIX} to {@link DragSensitive#DRAG_COEFFICIENT}
     * and then appending an index counting from 1.
     * </p>
     * @return built model
     */
    public IsotropicDrag build() {
        return new IsotropicDrag(crossSection, builder.build());
    }

}
