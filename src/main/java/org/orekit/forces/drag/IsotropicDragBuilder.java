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
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeSpanMap;

/** Builder for {@link IsotropicDrag} allowing to set coefficients on different time spans.
 * @author Luc Maisonobe
 * @since 14.0
 */
public class IsotropicDragBuilder {

    /** Prefix for coefficients limited to one time span. */
    public static final String SPAN_PREFIX = "Span";

    /** Parameters scaling factor.
     * <p>
     * We use a power of 2 to avoid numeric noise introduction
     * in the multiplications/divisions sequences.
     * </p>
     */
    private final double SCALE = FastMath.scalb(1.0, -3);

    /** Drag coefficients. */
    private final TimeSpanMap<Double> spans;

    /** Cross section (m²). */
    private final double crossSection;

    /** Minimum value of drag coefficient. */
    private final double dragCoeffMin;

    /** Maximum value of drag coefficient. */
    private final double dragCoeffMax;

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
        this.crossSection = crossSection;
        this.dragCoeffMin = dragCoeffMin;
        this.dragCoeffMax = dragCoeffMax;
        this.spans        = new TimeSpanMap<>(null);
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
        return addDragCoeff(dragCoeff, AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY);
    }

    /** Add a coefficient valid for a time span.
     * @param dragCoeff drag coefficient
     * @param earliestValidityDate date after which the coefficient is valid
     * @param latestValidityDate date before which the coefficient is valid
     * @return the instance itself, allowing use of the fluent interface pattern
     */
    public IsotropicDragBuilder addDragCoeff(final double dragCoeff,
                             final AbsoluteDate earliestValidityDate, final AbsoluteDate latestValidityDate) {
        spans.addValidBetween(dragCoeff, earliestValidityDate, latestValidityDate);
        return this;
    }

    /** Build a drag model.
     * <p>
     * If only one coefficient has been set, its name will be {@link DragSensitive#DRAG_COEFFICIENT}.
     * If several coefficients have been set, their names will be built by prepending
     * {@link #SPAN_PREFIX} to {@link DragSensitive#DRAG_COEFFICIENT} and then appending an
     * index counting from 0.
     * </p>
     * @return built model
     */
    public IsotropicDrag build() {

        TimeSpanMap.Span<Double> current;
        try {
            current = spans.getFirstNonNullSpan();
        } catch (OrekitException oe) {
            // user did not call addDragCoeff
            throw new OrekitException(oe, OrekitMessages.DRAG_COEFFICIENT_NOT_SET);
        }

        // check if there is one or several coefficients
        final boolean onlyOneCoeff = current == spans.getLastNonNullSpan();

        final TimeSpanMap<ParameterDriver> drivers = new TimeSpanMap<>(null);

        // build the drivers
        int index = 1;
        while (current != null) {

            // safety check
            if (current.getData() == null) {
                throw new OrekitException(OrekitMessages.MISSING_DRAG_COEFFICIENT,
                                          current.getStart(), current.getEnd());
            }

            // build the name of the driver, using a chronological index if needed
            final String name = onlyOneCoeff ?
                                DragSensitive.DRAG_COEFFICIENT :
                                SPAN_PREFIX + DragSensitive.DRAG_COEFFICIENT + index++;

            // create the driver
            final ParameterDriver driver =
                new ParameterDriver(name, current.getData(), SCALE, dragCoeffMin, dragCoeffMax);

            // add it to the map
            drivers.addValidBetween(driver, current.getStart(), current.getEnd());

            // prepare handling of next coefficient
            current = current.next();

        }

        return new IsotropicDrag(crossSection, drivers);

    }

}
