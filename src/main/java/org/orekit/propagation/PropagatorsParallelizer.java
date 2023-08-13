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
package org.orekit.propagation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.sampling.MultiSatFixedStepHandler;
import org.orekit.propagation.sampling.MultiSatStepHandler;
import org.orekit.propagation.sampling.MultisatStepNormalizer;
import org.orekit.propagation.sampling.OrekitStepHandler;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.propagation.sampling.StepHandlerMultiplexer;
import org.orekit.time.AbsoluteDate;

/** This class provides a way to propagate simultaneously several orbits.
 *
 * <p>
 * Multi-satellites propagation is based on multi-threading. Therefore,
 * care must be taken so that all propagators can be run in a multi-thread
 * context. This implies that all propagators are built independently and
 * that they rely on force models that are also built independently. An
 * obvious mistake would be to reuse a maneuver force model, as these models
 * need to cache the firing/not-firing status. Objects used by force models
 * like atmosphere models for drag force or others may also cache intermediate
 * variables, so separate instances for each propagator must be set up.
 * </p>
 * <p>
 * This class <em>will</em> create new threads for running the propagators.
 * It adds a new {@link MultiSatStepHandler global step handler} to manage
 * the steps all at once, in addition to the existing individual step
 * handlers that are preserved.
 * </p>
 * <p>
 * All propagators remain independent of each other (they don't even know
 * they are managed by the parallelizer) and advance their simulation
 * time following their own algorithm. The parallelizer will block them
 * at the end of each step and allow them to continue in order to maintain
 * synchronization. The {@link MultiSatStepHandler global handler} will
 * experience perfectly synchronized steps, but some propagators may already
 * be slightly ahead of time as depicted in the following rendering; were
 * simulation times flows from left to right:
 * </p>
 * <pre>
 *    propagator 1   : -------------[++++current step++++]&gt;
 *                                  |
 *    propagator 2   : ----[++++current step++++]---------&gt;
 *                                  |           |
 *    ...                           |           |
 *    propagator n   : ---------[++++current step++++]----&gt;
 *                                  |           |
 *                                  V           V
 *    global handler : -------------[global step]---------&gt;
 * </pre>
 * <p>
 * The previous sketch shows that propagator 1 has already computed states
 * up to the end of the propagation, but propagators 2 up to n are still late.
 * The global step seen by the handler will be the common part between all
 * propagators steps. Once this global step has been handled, the parallelizer
 * will let the more late propagator (here propagator 2) to go one step further
 * and a new global step will be computed and handled, until all propagators
 * reach the end.
 * </p>
 * <p>
 * This class does <em>not</em> provide multi-satellite events. As events
 * may truncate steps and even reset state, all events (including multi-satellite
 * events) are handled at a very low level within each propagators and cannot be
 * managed from outside by the parallelizer. For accurate handling of multi-satellite
 * events, the event detector should be registered <em>within</em> the propagator
 * of one satellite and have access to an independent propagator (typically an
 * analytical propagator or an ephemeris) of the other satellite. As the embedded
 * propagator will be called by the detector which itself is called by the first
 * propagator, it should really be a dedicated propagator and should not also
 * appear as one of the parallelized propagators, otherwise conflicts will appear here.
 * </p>
 * @author Luc Maisonobe
 * @since 9.0
 */

public class PropagatorsParallelizer {

    /** Waiting time to avoid getting stuck waiting for interrupted threads (ms). */
    private static long MAX_WAIT = 10;

    /** Underlying propagators. */
    private final List<Propagator> propagators;

    /** Global step handler. */
    private final MultiSatStepHandler globalHandler;

    /** Simple constructor.
     * @param propagators list of propagators to use
     * @param globalHandler global handler for managing all spacecrafts
     * simultaneously
     */
    public PropagatorsParallelizer(final List<Propagator> propagators,
                                   final MultiSatStepHandler globalHandler) {
        this.propagators = propagators;
        this.globalHandler = globalHandler;
    }

    /** Simple constructor.
     * @param propagators list of propagators to use
     * @param h fixed time step (sign is not used)
     * @param globalHandler global handler for managing all spacecrafts
     * simultaneously
     * @since 12.0
     */
    public PropagatorsParallelizer(final List<Propagator> propagators,
                                   final double h,
                                   final MultiSatFixedStepHandler globalHandler) {
        this.propagators   = propagators;
        this.globalHandler = new MultisatStepNormalizer(h, globalHandler);
    }

    /** Get an unmodifiable list of the underlying mono-satellite propagators.
     * @return unmodifiable list of the underlying mono-satellite propagators
     */
    public List<Propagator> getPropagators() {
        return Collections.unmodifiableList(propagators);
    }

    /** Propagate from a start date towards a target date.
     * @param start start date from which orbit state should be propagated
     * @param target target date to which orbit state should be propagated
     * @return propagated states
     */
    public List<SpacecraftState> propagate(final AbsoluteDate start, final AbsoluteDate target) {

        if (propagators.size() == 1) {
            // special handling when only one propagator is used
            propagators.get(0).getMultiplexer().add(new SinglePropagatorHandler(globalHandler));
            return Collections.singletonList(propagators.get(0).propagate(start, target));
        }

        final double sign = FastMath.copySign(1.0, target.durationFrom(start));

        // start all propagators in concurrent threads
        final ExecutorService            executorService = Executors.newFixedThreadPool(propagators.size());
        final List<PropagatorMonitoring> monitors        = new ArrayList<>(propagators.size());
        for (final Propagator propagator : propagators) {
            final PropagatorMonitoring monitor = new PropagatorMonitoring(propagator, start, target, executorService);
            monitor.waitFirstStepCompletion();
            monitors.add(monitor);
        }

        // main loop
        AbsoluteDate previousDate = start;
        final List<SpacecraftState> initialStates = new ArrayList<>(monitors.size());
        for (final PropagatorMonitoring monitor : monitors) {
            initialStates.add(monitor.parameters.initialState);
        }
        globalHandler.init(initialStates, target);
        for (boolean isLast = false; !isLast;) {

            // select the earliest ending propagator, according to propagation direction
            PropagatorMonitoring selected = null;
            AbsoluteDate selectedStepEnd  = null;
            for (PropagatorMonitoring monitor : monitors) {
                final AbsoluteDate stepEnd = monitor.parameters.interpolator.getCurrentState().getDate();
                if (selected == null || sign * selectedStepEnd.durationFrom(stepEnd) > 0) {
                    selected        = monitor;
                    selectedStepEnd = stepEnd;
                }
            }

            // restrict steps to a common time range
            for (PropagatorMonitoring monitor : monitors) {
                final OrekitStepInterpolator interpolator  = monitor.parameters.interpolator;
                final SpacecraftState        previousState = interpolator.getInterpolatedState(previousDate);
                final SpacecraftState        currentState  = interpolator.getInterpolatedState(selectedStepEnd);
                monitor.restricted                         = interpolator.restrictStep(previousState, currentState);
            }

            // handle all states at once
            final List<OrekitStepInterpolator> interpolators = new ArrayList<>(monitors.size());
            for (final PropagatorMonitoring monitor : monitors) {
                interpolators.add(monitor.restricted);
            }
            globalHandler.handleStep(interpolators);

            if (selected.parameters.finalState == null) {
                // step handler can still provide new results
                // this will wait until either handleStep or finish are called
                selected.retrieveNextParameters();
            } else {
                // this was the last step
                isLast = true;
                /* For NumericalPropagators :
                 * After reaching the finalState with the selected monitor,
                 * we need to do the step with all remaining monitors to reach the target time.
                 * This also triggers the StoringStepHandler, producing ephemeris.
                 */
                for (PropagatorMonitoring monitor : monitors) {
                    if (monitor != selected) {
                        monitor.retrieveNextParameters();
                    }
                }
            }

            previousDate = selectedStepEnd;

        }

        // stop all remaining propagators
        executorService.shutdownNow();

        // extract the final states
        final List<SpacecraftState> finalStates = new ArrayList<>(monitors.size());
        for (PropagatorMonitoring monitor : monitors) {
            try {
                finalStates.add(monitor.future.get());
            } catch (InterruptedException | ExecutionException e) {

                // sort out if exception was intentional or not
                monitor.manageException(e);

                // this propagator was intentionally stopped,
                // we retrieve the final state from the last available interpolator
                finalStates.add(monitor.parameters.interpolator.getInterpolatedState(previousDate));

            }
        }

        globalHandler.finish(finalStates);

        return finalStates;

    }

    /** Local exception to stop propagators. */
    private static class PropagatorStoppingException extends OrekitException {

        /** Serializable UID.*/
        private static final long serialVersionUID = 20170629L;

        /** Simple constructor.
         * @param ie interruption exception
         */
        PropagatorStoppingException(final InterruptedException ie) {
            super(ie, LocalizedCoreFormats.SIMPLE_MESSAGE, ie.getLocalizedMessage());
        }

    }

    /** Local class for handling single propagator steps. */
    private static class SinglePropagatorHandler implements OrekitStepHandler {

        /** Global handler. */
        private final MultiSatStepHandler globalHandler;

        /** Simple constructor.
         * @param globalHandler global handler to call
         */
        SinglePropagatorHandler(final MultiSatStepHandler globalHandler) {
            this.globalHandler = globalHandler;
        }


        /** {@inheritDoc} */
        @Override
        public void init(final SpacecraftState s0, final AbsoluteDate t) {
            globalHandler.init(Collections.singletonList(s0), t);
        }

        /** {@inheritDoc} */
        @Override
        public void handleStep(final OrekitStepInterpolator interpolator) {
            globalHandler.handleStep(Collections.singletonList(interpolator));
        }

        /** {@inheritDoc} */
        @Override
        public void finish(final SpacecraftState finalState) {
            globalHandler.finish(Collections.singletonList(finalState));
        }

    }

    /** Local class for handling multiple propagator steps. */
    private static class MultiplePropagatorsHandler implements OrekitStepHandler {

        /** Previous container handed off. */
        private ParametersContainer previous;

        /** Queue for passing step handling parameters. */
        private final SynchronousQueue<ParametersContainer> queue;

        /** Simple constructor.
         * @param queue queue for passing step handling parameters
         */
        MultiplePropagatorsHandler(final SynchronousQueue<ParametersContainer> queue) {
            this.previous = new ParametersContainer(null, null, null);
            this.queue    = queue;
        }

        /** Hand off container to parallelizer.
         * @param container parameters container to hand-off
         */
        private void handOff(final ParametersContainer container) {
            try {
                previous = container;
                queue.put(previous);
            } catch (InterruptedException ie) {
                // use a dedicated exception to stop thread almost gracefully
                throw new PropagatorStoppingException(ie);
            }
        }

        /** {@inheritDoc} */
        @Override
        public void init(final SpacecraftState s0, final AbsoluteDate t) {
            handOff(new ParametersContainer(s0, null, null));
        }

        /** {@inheritDoc} */
        @Override
        public void handleStep(final OrekitStepInterpolator interpolator) {
            handOff(new ParametersContainer(previous.initialState, interpolator, null));
        }

        /** {@inheritDoc} */
        @Override
        public void finish(final SpacecraftState finalState) {
            handOff(new ParametersContainer(previous.initialState, previous.interpolator, finalState));
        }

    }

    /** Container for parameters passed by propagators to step handlers. */
    private static class ParametersContainer {

        /** Initial state. */
        private final SpacecraftState initialState;

        /** Interpolator set up for last seen step. */
        private final OrekitStepInterpolator interpolator;

        /** Final state. */
        private final SpacecraftState finalState;

        /** Simple constructor.
         * @param initialState initial state
         * @param interpolator interpolator set up for last seen step
         * @param finalState final state
         */
        ParametersContainer(final SpacecraftState initialState,
                            final OrekitStepInterpolator interpolator,
                            final SpacecraftState finalState) {
            this.initialState = initialState;
            this.interpolator = interpolator;
            this.finalState   = finalState;
        }

    }

    /** Container for propagator monitoring. */
    private static class PropagatorMonitoring {

        /** Queue for handing off step handler parameters. */
        private final SynchronousQueue<ParametersContainer> queue;

        /** Future for retrieving propagation return value. */
        private final Future<SpacecraftState> future;

        /** Last step handler parameters received. */
        private ParametersContainer parameters;

        /** Interpolator restricted to time range shared with other propagators. */
        private OrekitStepInterpolator restricted;

        /** Simple constructor.
         * @param propagator managed propagator
         * @param start start date from which orbit state should be propagated
         * @param target target date to which orbit state should be propagated
         * @param executorService service for running propagator
         */
        PropagatorMonitoring(final Propagator propagator, final AbsoluteDate start, final AbsoluteDate target,
                             final ExecutorService executorService) {

            // set up queue for handing off step handler parameters synchronization
            // the main thread will let underlying propagators go forward
            // by consuming the step handling parameters they will put at each step
            queue = new SynchronousQueue<>();

            // Remove former instances of "MultiplePropagatorsHandler" from step handlers multiplexer
            clearMultiplePropagatorsHandler(propagator);

            // Add MultiplePropagatorsHandler step handler
            propagator.getMultiplexer().add(new MultiplePropagatorsHandler(queue));

            // start the propagator
            future = executorService.submit(() -> propagator.propagate(start, target));

        }

        /** Wait completion of first step.
         */
        public void waitFirstStepCompletion() {

            // wait until both the init method and the handleStep method
            // of the current propagator step handler have been called,
            // thus ensuring we have one step available to compare propagators
            // progress with each other
            while (parameters == null || parameters.initialState == null || parameters.interpolator == null) {
                retrieveNextParameters();
            }

        }

        /** Retrieve next step handling parameters.
         */
        public void retrieveNextParameters() {
            try {
                ParametersContainer params = null;
                while (params == null && !future.isDone()) {
                    params = queue.poll(MAX_WAIT, TimeUnit.MILLISECONDS);
                    // Check to avoid loop on future not done, in the case of reached finalState.
                    if (parameters != null) {
                        if (parameters.finalState != null) {
                            break;
                        }
                    }
                }
                if (params == null) {
                    // call Future.get just for the side effect of retrieving the exception
                    // in case the propagator ended due to an exception
                    future.get();
                }
                parameters = params;
            } catch (InterruptedException | ExecutionException e) {
                manageException(e);
                parameters = null;
            }
        }

        /** Convert exceptions.
         * @param exception exception caught
         */
        private void manageException(final Exception exception) {
            if (exception.getCause() instanceof PropagatorStoppingException) {
                // this was an expected exception, we deliberately shut down the propagators
                // we therefore explicitly ignore this exception
                return;
            } else if (exception.getCause() instanceof OrekitException) {
                // unwrap the original exception
                throw (OrekitException) exception.getCause();
            } else {
                throw new OrekitException(exception.getCause(),
                                          LocalizedCoreFormats.SIMPLE_MESSAGE, exception.getLocalizedMessage());
            }
        }

        /** Clear existing instances of MultiplePropagatorsHandler in a monitored propagator.
         * <p>
         * Removes former instances of "MultiplePropagatorsHandler" from step handlers multiplexer.
         * <p>
         * This is done to avoid propagation getting stuck after several calls to PropagatorsParallelizer.propagate(...)
         * <p>
         * See issue <a href="https://gitlab.orekit.org/orekit/orekit/-/issues/1105">1105</a>.
         * @param propagator monitored propagator whose MultiplePropagatorsHandlers must be cleared
         */
        private void clearMultiplePropagatorsHandler(final Propagator propagator) {

            // First, list instances of MultiplePropagatorsHandler in the propagator multiplexer
            final StepHandlerMultiplexer multiplexer = propagator.getMultiplexer();
            final List<OrekitStepHandler> existingMultiplePropagatorsHandler = new ArrayList<>();
            for (final OrekitStepHandler handler : multiplexer.getHandlers()) {
                if (handler instanceof MultiplePropagatorsHandler) {
                    existingMultiplePropagatorsHandler.add(handler);
                }
            }
            // Then, clear all MultiplePropagatorsHandler instances from multiplexer.
            // This is done in two steps because method "StepHandlerMultiplexer.remove(...)" already loops on the OrekitStepHandlers,
            // leading to a ConcurrentModificationException if attempting to do everything in a single loop
            for (final OrekitStepHandler handler : existingMultiplePropagatorsHandler) {
                multiplexer.remove(handler);
            }
        }
    }

}
