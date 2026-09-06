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
package org.orekit.utils.drivers;

import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeInterval;
import org.orekit.utils.TimeSpanMap;

/** Builder for {@link ParameterDriversSequence}.
 * @author Luc Maisonobe
 * @since 14.0
 */
public class ParameterDriversSequenceBuilder extends
    BaseParameterDriversSequenceBuilder<Double,
                                        ParameterDriver,
                                        ParameterObserver,
                                        ParameterDriversSequence,
                                        ParameterDriversSequenceBuilder> {

    /** Constructor for an initially empty builder.
     * <p>
     * The builder {@link #build() build()} method cannot be called right after construction.
     * Some reference values must be set before, either by calling the
     * {@link BaseParameterDriversSequenceBuilder#addReferenceValue(Object) addReferenceValue(referenceValue)}
     * method once, or by calling the {@link BaseParameterDriversSequenceBuilder#addReferenceValue(Object,
     * AbsoluteDate, AbsoluteDate) addReferenceValue(referenceValue, earliestValidityDate, latestValidityDate)}
     * method as many times as needed to cover the usage range before the {@link #build() build()} method can be
     * called.
     * </p>
     * @param baseName     base name of the parameters
     * @param scale        scaling factor to convert the parameters value to
     *                     non-dimensional (typically set to the expected standard deviation
     *                     of the parameter), it must be non-zero
     * @param minValue     minimum value allowed
     * @param maxValue     maximum value allowed
     * @param defaultValue default value valid throughout timeline
     */
    public ParameterDriversSequenceBuilder(final String baseName, final double scale,
                                           final double minValue, final double maxValue,
                                           final Double defaultValue) {
        super(baseName, scale, minValue, maxValue, defaultValue);
    }

    /** {@inheritDoc} */
    @Override
    protected ParameterDriver buildDriver(final String name, final Double referenceValue, final double scale,
                                          final double minValue, final double maxValue, final TimeInterval validity) {
        return new ParameterDriver(name, referenceValue, scale, minValue, maxValue, validity);
    }

    /** {@inheritDoc} */
    @Override
    protected ParameterDriversSequence buildSequence(final TimeSpanMap<ParameterDriver> drivers) {
        return new ParameterDriversSequence(drivers);
    }

}
