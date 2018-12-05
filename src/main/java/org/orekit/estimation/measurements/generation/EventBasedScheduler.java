/* Copyright 2002-2018 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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

import java.util.SortedSet;

import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.time.DatesSelector;


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

    /** Detector for checking measurements feasibility. */
    private final EventDetector detector;

    /** Semantic of the detector g function sign to use. */
    private final SignSemantic signSemantic;

    /** Simple constructor.
     * @param builder builder for individual measurements
     * @param selector selector for dates
     * @param detector detector for checking measurements feasibility
     * @param signSemantic semantic of the detector g function sign to use
     */
    public EventBasedScheduler(final MeasurementBuilder<T> builder, final DatesSelector selector,
                               final EventDetector detector, final SignSemantic signSemantic) {
        super(builder, selector);
        this.detector     = detector;
        this.signSemantic = signSemantic;
    }

    /** {@inheritDoc} */
    @Override
    public SortedSet<T> generate(final OrekitStepInterpolator... interpolators) {
        // TODO
        return null;
    }

}
