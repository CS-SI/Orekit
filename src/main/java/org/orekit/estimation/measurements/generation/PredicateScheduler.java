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
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.time.AbsoluteDate;


/** {@link Scheduler} based on {@link SchedulingPredicate} for generating measurements sequences.
 * <p>
 * Predicate-based schedulers generate measurements following a repetitive pattern when
 * a {@link SchedulingPredicate predicate} provided at construction has checked measurements
 * are feasible with current spacecraft states.
 * </p>
 * <p>
 * The repetitive pattern can be either a continuous stream of measurements separated by
 * a constant step (for example one measurement every 60s), or several sequences of measurements
 * at high rate up to a maximum number, with a rest period between sequences (for example
 * up to 256 measurements every 100ms with 300s between each sequence).
 * </p>
 * @param <T> the type of the measurement
 * @author Luc Maisonobe
 * @since 9.3
 */
public class PredicateScheduler<T extends ObservedMeasurement<T>> implements Scheduler<T> {

    /** Builder for individual measurements. */
    private final MeasurementBuilder<T> builder;

    /** Predicate for checking measurements feasibility. */
    private final SchedulingPredicate predicate;

    /** Maximum number of measurements in a sequence. */
    private final int maxMeasurementsInSequence;

    /** Step between two consecutive meaurements within a sequence. */
    private final double stepWithinSequence;

    /** Step between sequences. */
    private final double stepBetweenSequences;

    /** Date of the previous measurement. */
    private AbsoluteDate previousDate;

    /** Simple constructor.
     * @param builder builder for individual measurements
     * @param predicate predicate for checking measurements feasibility
     * @param step step between two consecutive meaurements (s)
     */
    public PredicateScheduler(final MeasurementBuilder<T> builder,
                              final SchedulingPredicate predicate,
                              final double step) {
        this(builder, predicate, Integer.MAX_VALUE, step, step);
    }

    /** Simple constructor.
     * @param builder builder for individual measurements
     * @param predicate predicate for checking measurements feasibility
     * @param maxMeasurementsInSequence maximum number of measurements in a sequence
     * @param stepWithinSequence step between two consecutive meaurements within a sequence (s)
     * @param stepBetweenSequences step between the last measurement of a sequence and the first
     * measurement of the next sequence (s)
     */
    public PredicateScheduler(final MeasurementBuilder<T> builder,
                              final SchedulingPredicate predicate,
                              final int maxMeasurementsInSequence,
                              final double stepWithinSequence,
                              final double stepBetweenSequences) {
        this.builder                   = builder;
        this.predicate                 = predicate;
        this.maxMeasurementsInSequence = maxMeasurementsInSequence;
        this.stepWithinSequence        = stepWithinSequence;
        this.stepBetweenSequences      = stepBetweenSequences;
        this.previousDate = AbsoluteDate.PAST_INFINITY;
    }

    /** {@inheritDoc} */
    @Override
    public SortedSet<T> generate(final OrekitStepInterpolator... interpolators) {
        // TODO
        return null;
    }

}
