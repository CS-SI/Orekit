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

import org.hipparchus.ode.events.Action;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.AdapterDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DatesSelector;
import org.orekit.utils.TimeSpanMap;


/** {@link Scheduler} based on {@link EventDetector} for generating measurements sequences.
 * <p>
 * Event-based schedulers generate measurements following a repetitive pattern when the
 * a {@link EventDetector detector} provided at construction is in a {@link SignSemantic
 * measurement feasible} state. It is important that the sign of the g function of the underlying
 * event detector is not arbitrary, but has a semantic meaning, e.g. in or out,
 * true or false. This class works well with event detectors that detect entry to or exit
 * from a region, e.g. {@link org.orekit.propagation.events.EclipseDetector EclipseDetector},
 * {@link org.orekit.propagation.events.ElevationDetector ElevationDetector}, {@link
 * org.orekit.propagation.events.LatitudeCrossingDetector LatitudeCrossingDetector}. Using this
 * scheduler with detectors that are not based on entry to or exit from a region, e.g. {@link
 * org.orekit.propagation.events.DateDetector DateDetector}, {@link
 * org.orekit.propagation.events.LongitudeCrossingDetector LongitudeCrossingDetector}, will likely
 * lead to unexpected results.
 * </p>
 * <p>
 * The repetitive pattern can be either a continuous stream of measurements separated by
 * a constant step (for example one measurement every 60s), or several sequences of measurements
 * at high rate up to a maximum number, with a rest period between sequences (for example
 * sequences of up to 256 measurements every 100ms with 300s between each sequence).
 * </p>
 * @param <T> the type of the measurement
 * @author Luc Maisonobe
 * @since 9.3
 */
public class EventBasedScheduler<T extends ObservedMeasurement<T>> extends AbstractScheduler<T> {

    /** Semantic of the detector g function sign to use. */
    private final SignSemantic signSemantic;

    /** Feasibility status. */
    private TimeSpanMap<Boolean> feasibility;

    /** Propagation direction. */
    private boolean forward;

    /** Simple constructor.
     * <p>
     * The event detector instance should <em>not</em> be already bound to the propagator.
     * It will be wrapped in an {@link AdapterDetector adapter} in order to manage time
     * ranges when measurements are feasible. The wrapping adapter will be automatically
     * {@link Propagator#addEventDetector(EventDetector) added} to the propagator by this
     * constructor.
     * </p>
     * <p>
     * BEWARE! Dates selectors often store internally the last selected dates, so they are not
     * reusable across several {@link EventBasedScheduler instances}. A separate selector
     * should be used for each scheduler.
     * </p>
     * @param builder builder for individual measurements
     * @param selector selector for dates (beware that selectors are generally not
     * reusable across several {@link EventBasedScheduler instances}, each selector should
     * be dedicated to one scheduler
     * @param propagator propagator associated with this scheduler
     * @param detector detector for checking measurements feasibility
     * @param signSemantic semantic of the detector g function sign to use
     */
    public EventBasedScheduler(final MeasurementBuilder<T> builder, final DatesSelector selector,
                               final Propagator propagator,
                               final EventDetector detector, final SignSemantic signSemantic) {
        super(builder, selector);
        this.signSemantic = signSemantic;
        this.feasibility  = new TimeSpanMap<Boolean>(Boolean.FALSE);
        this.forward      = true;
        propagator.addEventDetector(new FeasibilityAdapter(detector));
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
            if (feasibility.get(date)) {
                // a measurement is feasible at this date

                // interpolate states at measurement date
                final SpacecraftState[] states = new SpacecraftState[interpolators.size()];
                for (int i = 0; i < states.length; ++i) {
                    states[i] = interpolators.get(i).getInterpolatedState(date);
                }

                // generate measurement
                measurements.add(getBuilder().build(states));

            }
        }

        return measurements;

    }

    /** Adapter for managing feasibility status changes. */
    private class FeasibilityAdapter extends AdapterDetector {

        /** Build an adaptor wrapping an existing detector.
         * @param detector detector to wrap
         */
        FeasibilityAdapter(final EventDetector detector) {
            super(detector);
        }

        /** {@inheritDoc} */
        @Override
        public void init(final SpacecraftState s0, final AbsoluteDate t) {
            super.init(s0, t);
            forward     = t.compareTo(s0.getDate()) > 0;
            feasibility = new TimeSpanMap<Boolean>(signSemantic.measurementIsFeasible(g(s0)));
        }

        /** {@inheritDoc} */
        @Override
        public Action eventOccurred(final SpacecraftState s, final boolean increasing) {

            // find the feasibility status AFTER the current date
            final boolean statusAfter = signSemantic.measurementIsFeasible(increasing ? +1 : -1);

            // store either status or its opposite according to propagation direction
            if (forward) {
                // forward propagation
                feasibility.addValidAfter(statusAfter, s.getDate());
            } else {
                // backward propagation
                feasibility.addValidBefore(!statusAfter, s.getDate());
            }

            // delegate to wrapped detector
            return super.eventOccurred(s, increasing);

        }

    }

}
