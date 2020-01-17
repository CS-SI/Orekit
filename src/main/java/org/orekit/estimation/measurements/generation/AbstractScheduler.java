/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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
package org.orekit.estimation.measurements.generation;

import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DatesSelector;


/** Base implementation of {@link Scheduler} managing {@link DatesSelector dates selection}.
 * @param <T> the type of the measurement
 * @author Luc Maisonobe
 * @since 9.3
 */
public abstract class AbstractScheduler<T extends ObservedMeasurement<T>> implements Scheduler<T> {

    /** Builder for individual measurements. */
    private final MeasurementBuilder<T> builder;

    /** Selector for dates. */
    private final DatesSelector selector;

    /** Simple constructor.
     * @param builder builder for individual measurements
     * @param selector selector for dates
     */
    protected AbstractScheduler(final MeasurementBuilder<T> builder,
                                final DatesSelector selector) {
        this.builder  = builder;
        this.selector = selector;
    }

    /** {@inheritDoc}
     * <p>
     * This implementation initialize the measurement builder.
     * </p>
     */
    @Override
    public void init(final AbsoluteDate start, final AbsoluteDate end) {
        builder.init(start, end);
    }

    /** Get the measurements builder.
     * @return measurements builder
     */
    public MeasurementBuilder<T> getBuilder() {
        return builder;
    }

    /** Get the dates selector.
     * @return dates selector
     */
    public DatesSelector getSelector() {
        return selector;
    }

}
