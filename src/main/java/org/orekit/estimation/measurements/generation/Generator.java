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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.PropagatorsParallelizer;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.sampling.MultiSatStepHandler;
import org.orekit.propagation.sampling.OrekitStepHandler;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.propagation.sampling.StepHandlerMultiplexer;
import org.orekit.time.AbsoluteDate;


/** Main generator for {@link ObservedMeasurement observed measurements}.
 * @author Luc Maisonobe
 * @since 9.3
 */
public class Generator {

    /** Observable satellites.
     * @since 12.0
     */
    private final List<ObservableSatellite> observableSatellites;

    /** Propagators. */
    private final List<Propagator> propagators;

    /** Schedulers for multiple satellites measurements. */
    private final List<Scheduler<? extends ObservedMeasurement<?>>> multiSatSchedulers;

    /** Schedulers for single satellite measurements. */
    private final Map<ObservableSatellite, List<Scheduler<? extends ObservedMeasurement<?>>>> singleSatSchedulers;

    /** Subscribers for generated measurements events.
     * @since 12.0
     */
    private final List<GeneratedMeasurementSubscriber> subscribers;

    /** Build a generator with no sequences generator.
     */
    public Generator() {
        this.observableSatellites = new ArrayList<>();
        this.propagators          = new ArrayList<>();
        this.multiSatSchedulers   = new ArrayList<>();
        this.singleSatSchedulers  = new HashMap<>();
        this.subscribers          = new ArrayList<>();
    }

    /** Add a propagator.
     * @param propagator to add
     * @return satellite satellite propagated by the propagator
     */
    public ObservableSatellite addPropagator(final Propagator propagator) {
        final ObservableSatellite os = new ObservableSatellite(propagators.size());
        observableSatellites.add(os);
        propagators.add(propagator);
        return os;
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
        final ObservableSatellite[] satellites = scheduler.getBuilder().getSatellites();
        if (satellites.length == 1) {
            // this scheduler manages only one satellite
            // we can let the individual propagator handle it
            List<Scheduler<? extends ObservedMeasurement<?>>> list = singleSatSchedulers.get(satellites[0]);
            if (list == null) {
                list = new ArrayList<>();
                singleSatSchedulers.put(satellites[0], list);
            }
            list.add(scheduler);
        } else {
            // this scheduler manages several satellites at once
            // we need to handle it at top level
            multiSatSchedulers.add(scheduler);
        }
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

        // set up top level handler
        final MultipleSatGeneratorHandler globalHandler =
                        new MultipleSatGeneratorHandler(multiSatSchedulers, subscribers,
                                                        observableSatellites, end.isAfterOrEqualTo(start));

        // set up low level handlers
        for (final Map.Entry<ObservableSatellite, List<Scheduler<? extends ObservedMeasurement<?>>>> entry : singleSatSchedulers.entrySet()) {
            final StepHandlerMultiplexer multiplexer = propagators.get(entry.getKey().getPropagatorIndex()).getMultiplexer();
            for (final Scheduler<?> scheduler : entry.getValue()) {
                multiplexer.add(new SingleSatGeneratorHandler<>(scheduler, globalHandler));
            }
        }

        // prepare parallelized generation
        final PropagatorsParallelizer parallelizer = new PropagatorsParallelizer(propagators, globalHandler);

        // generate the measurements
        parallelizer.propagate(start, end);

        // clean up low level handlers
        for (final Map.Entry<ObservableSatellite, List<Scheduler<? extends ObservedMeasurement<?>>>> entry : singleSatSchedulers.entrySet()) {
            // we need to clean up the step handlers in two loops to avoid concurrent modification exception
            final StepHandlerMultiplexer multiplexer = propagators.get(entry.getKey().getPropagatorIndex()).getMultiplexer();
            final List<OrekitStepHandler> toBeRemoved = new ArrayList<>();
            for (final OrekitStepHandler handler : multiplexer.getHandlers()) {
                if (handler instanceof SingleSatGeneratorHandler &&
                    ((SingleSatGeneratorHandler<?>) handler).globalHandler == globalHandler) {
                    toBeRemoved.add(handler);
                }
            }
            for (final OrekitStepHandler handler : toBeRemoved) {
                multiplexer.remove(handler);
            }
        }

    }

    /** Handler for measurements generation steps, single satellite case.
     * <p>
     * These handlers are called from the individual propagators threads.
     * This means they generate measurements in parallel.
     * </p>
     * @param <T> the type of the measurement
     * @since 12.0
     */
    private static class SingleSatGeneratorHandler<T extends ObservedMeasurement<T>> implements OrekitStepHandler {

        /** Scheduler. */
        private final Scheduler<T> scheduler;

        /** Satellite related to this scheduler. */
        private final ObservableSatellite satellite;

        /** Global handler. */
        private final MultipleSatGeneratorHandler globalHandler;

        /** Simple constructor.
         * @param scheduler scheduler
         * @param globalHandler global handler
         */
        SingleSatGeneratorHandler(final Scheduler<T> scheduler, final MultipleSatGeneratorHandler globalHandler) {
            this.scheduler     = scheduler;
            this.satellite     = scheduler.getBuilder().getSatellites()[0];
            this.globalHandler = globalHandler;
        }

        /** {@inheritDoc} */
        @Override
        public void init(final SpacecraftState state0, final AbsoluteDate t) {
            scheduler.init(state0.getDate(), t);
        }

        /** {@inheritDoc} */
        @Override
        public void handleStep(final OrekitStepInterpolator interpolator) {
            globalHandler.addMeasurements(scheduler.generate(Collections.singletonMap(satellite, interpolator)));
        }

    }

    /** Handler for measurements generation steps.
     * <p>
     * This handler is called from the propagator parallelizer thread.
     * The parallelizer thread is called after the individual propagators thread,
     * which may already have produced measurements ahead of time, so we must
     * take care than within each step we handle only the measurements that belong
     * to this step.
     * </p>
     */
    private static class MultipleSatGeneratorHandler implements MultiSatStepHandler {

        /** Sequences generators. */
        private final List<Scheduler<? extends ObservedMeasurement<?>>> schedulers;

        /** Subscribers for generated measurements events.
         * @since 12.0
         */
        private final List<GeneratedMeasurementSubscriber> subscribers;

        /** Observable satellites.
         * @since 12.0
         */
        private final List<ObservableSatellite> observableSatellites;

        /** Storage for sorted measurements within one step.
         * @since 12.0
         */
        private final SortedSet<ObservedMeasurement<?>> generated;

        /** Forward generation indicator.
         * @since 12.0
         */
        private final boolean forward;

        /** Simple constructor.
         * @param schedulers sequences generators
         * @param subscribers subscribers for generated measurements events
         * @param observableSatellites observable satellites
         * @param forward if true, generation is forward
         * @since 12.0
         */
        MultipleSatGeneratorHandler(final List<Scheduler<? extends ObservedMeasurement<?>>> schedulers,
                                    final List<GeneratedMeasurementSubscriber> subscribers,
                                    final List<ObservableSatellite> observableSatellites, final boolean forward) {

            // measurements comparator, consistent with generation direction
            final Comparator<ObservedMeasurement<?>> comparator = forward ? Comparator.naturalOrder() : Comparator.reverseOrder();

            this.schedulers           = schedulers;
            this.subscribers          = subscribers;
            this.observableSatellites = observableSatellites;
            this.generated            = new TreeSet<>(comparator);
            this.forward              = forward;

        }

        /** {@inheritDoc} */
        @Override
        public void init(final List<SpacecraftState> states0, final AbsoluteDate t) {

            final AbsoluteDate start = states0.get(0).getDate();

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

            // prepare interpolators map
            final Map<ObservableSatellite, OrekitStepInterpolator> interpolatorsMap =
                            new HashMap<>(interpolators.size());
            for (int i = 0; i < interpolators.size(); ++i) {
                interpolatorsMap.put(observableSatellites.get(i), interpolators.get(i));
            }
            final AbsoluteDate lastDate = interpolators.get(0).getCurrentState().getDate();

            synchronized (generated) {

                // generate measurements, looping over schedulers
                for (final Scheduler<? extends ObservedMeasurement<?>> scheduler : schedulers) {
                    generated.addAll(scheduler.generate(interpolatorsMap));
                }

                // now that we have all measurements properly sorted, we can feed them to subscribers
                for (final Iterator<ObservedMeasurement<?>> iterator = generated.iterator(); iterator.hasNext();) {
                    final ObservedMeasurement<?> measurement = iterator.next();
                    if (forward == lastDate.isAfterOrEqualTo(measurement)) {
                        // this measurement belongs to the current step
                        for (final GeneratedMeasurementSubscriber subscriber : subscribers) {
                            subscriber.handleGeneratedMeasurement(measurement);
                        }
                        iterator.remove();
                    } else {
                        // this measurement belongs to an upcoming step ; we don't handle it yet as more
                        // intermediate measurements may be produced by low level propagators threads
                        break;
                    }
                }

            }

        }

        /** {@inheritDoc} */
        public void finish(final List<SpacecraftState> finalStates) {
            synchronized (generated) {
                for (final ObservedMeasurement<?> measurement : generated) {
                    for (final GeneratedMeasurementSubscriber subscriber : subscribers) {
                        subscriber.handleGeneratedMeasurement(measurement);
                    }
                }
                generated.clear();
            }
        }

        /** Add measurements performed by a low level handler.
         * @param measurements measurements to add
         * @since 12.0
         */
        private void addMeasurements(final SortedSet<? extends ObservedMeasurement<?>> measurements) {
            synchronized (generated) {
                generated.addAll(measurements);
            }
        }

    }

}
