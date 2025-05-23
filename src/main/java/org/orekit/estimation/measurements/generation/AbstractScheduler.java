/* Copyright 2002-2025 CS GROUP
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
package org.orekit.estimation.measurements.generation;

import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Predicate;

import org.orekit.estimation.measurements.EstimatedMeasurementBase;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
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

    /** Predicate for a posteriori filtering of generated measurements.
     * @since 13.0
     */
    private final Predicate<EstimatedMeasurementBase<T>> filter;

    /** Simple constructor.
     * @param builder builder for individual measurements
     * @param selector selector for dates
     * @param filter predicate for a posteriori filtering of generated measurements
     *               (measurements are accepted if the predicates evaluates to {@code true})
     * @since 13.0
     */
    protected AbstractScheduler(final MeasurementBuilder<T> builder,
                                final DatesSelector selector,
                                final Predicate<EstimatedMeasurementBase<T>> filter) {
        this.builder  = builder;
        this.selector = selector;
        this.filter   = filter;
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

    /** {@inheritDoc} */
    @Override
    public MeasurementBuilder<T> getBuilder() {
        return builder;
    }

    /** Get the dates selector.
     * @return dates selector
     */
    public DatesSelector getSelector() {
        return selector;
    }

    /** {@inheritDoc} */
    @Override
    public SortedSet<EstimatedMeasurementBase<T>> generate(final Map<ObservableSatellite, OrekitStepInterpolator> interpolators) {

        // select dates in the current step, using arbitrarily first interpolator
        // as all interpolators cover the same range
        final Map.Entry<ObservableSatellite, OrekitStepInterpolator> first = interpolators.entrySet().iterator().next();
        final List<AbsoluteDate> dates = getSelector().selectDates(first.getValue().getPreviousState().getDate(),
                                                                   first.getValue().getCurrentState().getDate());

        // generate measurements when feasible
        final SortedSet<EstimatedMeasurementBase<T>> measurements = new TreeSet<>();
        for (final AbsoluteDate date : dates) {
            if (measurementIsFeasible(date)) {
                // a measurement is feasible at this date
                final EstimatedMeasurementBase<T> built = getBuilder().build(date, interpolators);
                if (filter.test(built)) {
                    // add the generated measurement is the filters accepts it
                    measurements.add(built);
                }
            }
        }

        return measurements;

    }

    /** Check if a measurement is feasible at some date.
     * @param date date to check
     * @return true if measurement if feasible
     * @since 12.0
     */
    protected abstract boolean measurementIsFeasible(AbsoluteDate date);

}
