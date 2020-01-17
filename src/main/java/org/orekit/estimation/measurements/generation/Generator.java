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

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.PropagatorsParallelizer;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.sampling.MultiSatStepHandler;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.time.AbsoluteDate;


/** Main generator for {@link ObservedMeasurement observed measurements}.
 * @author Luc Maisonobe
 * @since 9.3
 */
public class Generator {

    /** Propagators. */
    private final List<Propagator> propagators;

    /** Sequences generators. */
    private final List<Scheduler<?>> schedulers;

    /** Build a generator with no sequences generator.
     */
    public Generator() {
        this.propagators = new ArrayList<>();
        this.schedulers  = new ArrayList<>();
    }

    /** Add a propagator.
     * @param propagator to add
     * @return satellite satellite propagated by the propagator
     */
    public ObservableSatellite addPropagator(final Propagator propagator) {
        propagators.add(propagator);
        return new ObservableSatellite(propagators.size() - 1);
    }

    /** Get a registered propagator.
     * @param satellite satellite propagated by the propagator {@link #addPropagator(Propagator)}
     * @return propagator corresponding to satellite
     */
    public Propagator getPropagator(final ObservableSatellite satellite) {
        return propagators.get(satellite.getPropagatorIndex());
    }

    /** Add a sequences generator for a specific measurement type.
     * @param scheduler sequences generator to add
     * @param <T> the type of the measurement
     */
    public <T extends ObservedMeasurement<T>> void addScheduler(final Scheduler<T> scheduler) {
        schedulers.add(scheduler);
    }

    /** Generate measurements.
     * @param start start of the measurements time span
     * @param end end of the measurements time span
     * @return generated measurements
     */
    public SortedSet<ObservedMeasurement<?>> generate(final AbsoluteDate start, final AbsoluteDate end) {

        // initialize schedulers
        for (final Scheduler<?> scheduler : schedulers) {
            scheduler.init(start, end);
        }

        // set up parallelized propagators
        final GeneratorHandler handler = new GeneratorHandler(schedulers);
        final PropagatorsParallelizer parallelizer = new PropagatorsParallelizer(propagators, handler);

        // generate the measurements
        parallelizer.propagate(start, end);

        return handler.getMeasurements();

    }

    /** Handler for measurements generation steps. */
    private static class GeneratorHandler implements MultiSatStepHandler {

        /** Sequences generators. */
        private final List<Scheduler<?>> schedulers;

        /** Set for holding measurements. */
        private final SortedSet<ObservedMeasurement<?>> measurements;

        /** Simple constructor.
         * @param schedulers sequences generators
         */
        GeneratorHandler(final List<Scheduler<?>> schedulers) {
            this.schedulers   = schedulers;
            this.measurements = new TreeSet<>();
        }

        /** {@inheritDoc} */
        @Override
        public void init(final List<SpacecraftState> states0, final AbsoluteDate t) {
            for (final Scheduler<?> scheduler : schedulers) {
                scheduler.init(states0.get(0).getDate(), t);
            }
        }

        /** {@inheritDoc} */
        @Override
        public void handleStep(final List<OrekitStepInterpolator> interpolators, final boolean isLast) {
            for (final Scheduler<?> scheduler : schedulers) {
                measurements.addAll(scheduler.generate(interpolators));
            }
        }

        /** Get the generated measurements.
         * @return generated measurements
         */
        public SortedSet<ObservedMeasurement<?>> getMeasurements() {
            return measurements;
        }

    }

}
