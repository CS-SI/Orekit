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
package org.orekit.utils;

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;

/** Builder for {@link ParameterDriversSequence}.
 * @author Luc Maisonobe
 * @since 14.0
 */
public class ParameterDriversSequenceBuilder {

    /** Prefix for drivers limited to one time span. */
    public static final String SPAN_PREFIX = "span-";

    /** Base name of the parameters. */
    private final String baseName;

    /** Parameters scaling factor. */
    private final double scale;

    /** Minimum value. */
    private final double minValue;

    /** Maximum value. */
    private final double maxValue;

    /** Reference values. */
    private final TimeSpanMap<Double> spans;

    /** Constructor for an initially empty builder.
     * <p>
     * The builder {@link #build() build()} method cannot be called right after construction.
     * Some reference values must be set before, either by calling the
     * {@link #addReferenceValue(double) addReferenceValue(referenceValue)} method once, or by
     * calling the {@link #addReferenceValue(double, AbsoluteDate, AbsoluteDate)
     * addReferenceValue(referenceValue, earliestValidityDate, latestValidityDate)} method as many times
     * as needed to cover the usage range before the {@link #build() build()} method can be called.
     * </p>
     * @param baseName base name of the parameters
     * @param scale    scaling factor to convert the parameters value to
     *                 non-dimensional (typically set to the expected standard deviation
     *                 of the parameter), it must be non-zero
     * @param minValue minimum value allowed
     * @param maxValue maximum value allowed
     */
    public ParameterDriversSequenceBuilder(final String baseName, final double scale,
                                           final double minValue, final double maxValue) {
        this.baseName = baseName;
        this.scale    = scale;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.spans    = new TimeSpanMap<>(null);
    }

    /** Add a reference value throughout timeline.
     * <p>
     * Calling this method is equivalent to call
     * {@code addReferenceValue(referenceValue, AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY)}.
     * </p>
     * @param referenceValue reference value
     * @return the instance itself, allowing use of the fluent interface pattern
     */
    public ParameterDriversSequenceBuilder addReferenceValue(final double referenceValue) {
        return addReferenceValue(referenceValue, AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY);
    }

    /** Add a reference value valid for a time span.
     * @param referenceValue       reference value
     * @param earliestValidityDate date after which the coefficient is valid
     * @param latestValidityDate   date before which the coefficient is valid
     * @return the instance itself, allowing use of the fluent interface pattern
     */
    public ParameterDriversSequenceBuilder addReferenceValue(final double referenceValue,
                                                             final AbsoluteDate earliestValidityDate,
                                                             final AbsoluteDate latestValidityDate) {
        spans.addValidBetween(referenceValue, earliestValidityDate, latestValidityDate);
        return this;
    }

    /** Build a {@link ParameterDriversSequence}.
     * <p>
     * If only one reference value has been set, its name will be the base name set at construction.
     * If several reference values have been set, their names will be built by concatenating together
     * {@link #SPAN_PREFIX}, followed by the base name set at construction, followed by a single "-",
     * and finally appending the index of the parameter driver, counting from 0.
     * </p>
     * @return built sequence
     */
    public ParameterDriversSequence build() {

        TimeSpanMap.Span<Double> current;
        try {
            current = spans.getFirstNonNullSpan();
        } catch (OrekitException oe) {
            // user did not call addReferenceValue
            throw new OrekitException(oe, OrekitMessages.NO_REFERENCE_VALUES_SET);
        }

        // check if there is one or several reference values
        final boolean oneValueOnly = current == spans.getLastNonNullSpan();

        final TimeSpanMap<ParameterDriver> drivers = new TimeSpanMap<>(null);

        // build the drivers
        int index = 0;
        while (current != null) {

            // safety check
            if (current.getData() == null) {
                if (current.next() == null) {
                    // null data after the sequence is OK
                    break;
                } else {
                    // null date in the middle of the sequence
                    throw new OrekitException(OrekitMessages.MISSING_REFERENCE_VALUE,
                                              current.getStart(), current.getEnd());
                }
            }

            // build the name of the driver, using a chronological index if needed
            final String name = oneValueOnly ? baseName : SPAN_PREFIX + baseName + "-" + index++;

            // create the driver
            final ParameterDriver driver = new ParameterDriver(name, current.getData(), scale, minValue, maxValue,
                                                               current.getStart(), current.getEnd());

            // add it to the map
            drivers.addValidBetween(driver, current.getStart(), current.getEnd());

            // prepare handling of next reference value
            current = current.next();

        }

        return new ParameterDriversSequence(drivers);

    }

}
