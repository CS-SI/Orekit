/* Copyright 2002-2023 CS GROUP
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

import java.util.ArrayList;
import java.util.Comparator;
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

    /** Subscribers for generated measurements events.
     * @since 12.0
     */
    private final List<GeneratedMeasurementSubscriber> subscribers;

    /** Build a generator with no sequences generator.
     */
    public Generator() {
        this.propagators = new ArrayList<>();
        this.schedulers  = new ArrayList<>();
        this.subscribers = new ArrayList<>();
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

    /** Add a subscriber.
     * @param subscriber to add
     * @see GatheringSubscriber
     * @since 12.0
     */
    public void addSubscriber(final GeneratedMeasurementSubscriber subscriber) {
        subscribers.add(subscriber);
    }

    /** Generate measurements.
     * @param start start of the measurements time span
     * @param end end of the measurements time span
     */
    public void generate(final AbsoluteDate start, final AbsoluteDate end) {

        // set up parallelized propagators
        final GeneratorHandler        handler      = new GeneratorHandler(schedulers, subscribers);
        final PropagatorsParallelizer parallelizer = new PropagatorsParallelizer(propagators, handler);

        // generate the measurements
        parallelizer.propagate(start, end);

    }

    /** Handler for measurements generation steps. */
    private static class GeneratorHandler implements MultiSatStepHandler {

        /** Sequences generators. */
        private final List<Scheduler<?>> schedulers;

        /** Subscribers for generated measurements events.
         * @since 12.0
         */
        private final List<GeneratedMeasurementSubscriber> subscribers;

        /** Storage for sorted measurements within one step.
         * @since 12.0
         */
        private SortedSet<ObservedMeasurement<?>> generated;

        /** Simple constructor.
         * @param schedulers sequences generators
         * @param subscribers subscribers for generated measurements events
         * @since 12.0
         */
        GeneratorHandler(final List<Scheduler<?>> schedulers, final List<GeneratedMeasurementSubscriber> subscribers) {
            this.schedulers  = schedulers;
            this.subscribers = subscribers;
        }

        /** {@inheritDoc} */
        @Override
        public void init(final List<SpacecraftState> states0, final AbsoluteDate t) {

            final AbsoluteDate start = states0.get(0).getDate();
            final Comparator<ObservedMeasurement<?>> comparator = t.isAfterOrEqualTo(start) ?
                                                                  Comparator.naturalOrder() :
                                                                  Comparator.reverseOrder();
            generated = new TreeSet<>(comparator);

            // initialize schedulers
            for (final Scheduler<?> scheduler : schedulers) {
                scheduler.init(start, t);
            }

            // initialize subscribers
            for (final GeneratedMeasurementSubscriber subscriber : subscribers) {
                subscriber.init(start, t);
            }

        }

        /** {@inheritDoc} */
        @Override
        public void handleStep(final List<OrekitStepInterpolator> interpolators) {

            // prepare the sorted set for measurements generated during this step
            generated.clear();

            // generate measurements, looping over schedulers
            for (final Scheduler<?> scheduler : schedulers) {
                generated.addAll(scheduler.generate(interpolators));
            }

            // now that we have all measurements properly sorted, we can feed them to subscribers
            for (final ObservedMeasurement<?> measurement : generated) {
                for (final GeneratedMeasurementSubscriber subscriber : subscribers) {
                    subscriber.handleGeneratedMeasurement(measurement);
                }
            }

        }

    }

}
