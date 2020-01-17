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

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DatesSelector;


/** {@link Scheduler} generating measurements sequences continuously.
 * <p>
 * Continuous schedulers continuously generate measurements following a repetitive pattern.
 * The repetitive pattern can be either a continuous stream of measurements separated by
 * a constant step (for example one measurement every 60s), or several sequences of measurements
 * at high rate up to a maximum number, with a rest period between sequences (for example
 * sequences of up to 256 measurements every 100ms with 300s between each sequence).
 * </p>
 * @param <T> the type of the measurement
 * @author Luc Maisonobe
 * @since 9.3
 */
public class ContinuousScheduler<T extends ObservedMeasurement<T>> extends AbstractScheduler<T> {

    /** Simple constructor.
     * <p>
     * BEWARE! Dates selectors often store internally the last selected dates, so they are not
     * reusable across several {@link EventBasedScheduler instances}. A separate selector
     * should be used for each scheduler.
     * </p>
     * @param builder builder for individual measurements
     * @param selector selector for dates (beware that selectors are generally not
     * reusable across several {@link EventBasedScheduler instances}, each selector should
     * be dedicated to one scheduler
     */
    public ContinuousScheduler(final MeasurementBuilder<T> builder, final DatesSelector selector) {
        super(builder, selector);
    }

    /** {@inheritDoc} */
    @Override
    public SortedSet<T> generate(final List<OrekitStepInterpolator> interpolators) {

        // select dates in the current step, using arbitrarily interpolator 0
        // as all interpolators cover the same range
        final List<AbsoluteDate> dates = getSelector().selectDates(interpolators.get(0).getPreviousState().getDate(),
                                                                   interpolators.get(0).getCurrentState().getDate());

        // generate measurements when feasible
        final SortedSet<T> measurements = new TreeSet<>();
        for (final AbsoluteDate date : dates) {

            // interpolate states at measurement date
            final SpacecraftState[] states = new SpacecraftState[interpolators.size()];
            for (int i = 0; i < states.length; ++i) {
                states[i] = interpolators.get(i).getInterpolatedState(date);
            }

            // generate measurement
            measurements.add(getBuilder().build(states));

        }

        return measurements;

    }

}
