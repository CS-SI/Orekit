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
import org.orekit.propagation.sampling.MultiSatStepHandler;
import org.orekit.propagation.sampling.OrekitStepHandler;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeStamped;

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
 * This class <em>will</em> create new threads for running the propagators
 * and it <em>will</em> override the underlying propagators step handlers.
 * The intent is anyway to manage the steps all at once using the global
 * {@link MultiSatStepHandler handler} set up at construction.
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
            propagators.get(0).setMasterMode(new SinglePropagatorHandler(globalHandler));
            return Collections.singletonList(propagators.get(0).propagate(start, target));
        }

        final double sign = FastMath.copySign(1.0, target.durationFrom(start));
        final int n = propagators.size();

        // set up queues for propagators synchronization
        // the main thread will let underlying propagators go forward
        // by consuming the step handling parameters they will put at each step
        final List<SynchronousQueue<SpacecraftState>>        initQueues = new ArrayList<>(n);
        final List<SynchronousQueue<StepHandlingParameters>> shpQueues  = new ArrayList<>(n);
        for (final Propagator propagator : propagators) {
            final SynchronousQueue<SpacecraftState>        initQueue = new SynchronousQueue<>();
            initQueues.add(initQueue);
            final SynchronousQueue<StepHandlingParameters> shpQueue  = new SynchronousQueue<>();
            shpQueues.add(shpQueue);
            propagator.setMasterMode(new MultiplePropagatorsHandler(initQueue, shpQueue));
        }

        // concurrently run all propagators
        final ExecutorService               executorService        = Executors.newFixedThreadPool(n);
        final List<Future<SpacecraftState>> futures                = new ArrayList<>(n);
        final List<SpacecraftState>         initialStates          = new ArrayList<>(n);
        final List<StepHandlingParameters>  stepHandlingParameters = new ArrayList<>(n);
        final List<OrekitStepInterpolator>  restricted             = new ArrayList<>(n);
        final List<SpacecraftState>         finalStates            = new ArrayList<>(n);
        for (int i = 0; i < n; ++i) {
            final Propagator propagator = propagators.get(i);
            final Future<SpacecraftState> future = executorService.submit(() -> propagator.propagate(start, target));
            futures.add(future);
            initialStates.add(getParameters(i, future, initQueues.get(i)));
            stepHandlingParameters.add(getParameters(i, future, shpQueues.get(i)));
            restricted.add(null);
            finalStates.add(null);
        }

        // main loop
        AbsoluteDate previousDate = start;
        globalHandler.init(initialStates, target);
        for (boolean isLast = false; !isLast;) {

            // select the earliest ending propagator, according to propagation direction
            int selected = -1;
            AbsoluteDate selectedStepEnd = null;
            for (int i = 0; i < n; ++i) {
                final AbsoluteDate stepEnd = stepHandlingParameters.get(i).getDate();
                if (selected < 0 || sign * selectedStepEnd.durationFrom(stepEnd) > 0) {
                    selected        = i;
                    selectedStepEnd = stepEnd;
                }
            }

            // restrict steps to a common time range
            for (int i = 0; i < n; ++i) {
                final OrekitStepInterpolator interpolator  = stepHandlingParameters.get(i).interpolator;
                final SpacecraftState        previousState = interpolator.getInterpolatedState(previousDate);
                final SpacecraftState        currentState  = interpolator.getInterpolatedState(selectedStepEnd);
                restricted.set(i, interpolator.restrictStep(previousState, currentState));
            }

            // will this be the last step?
            isLast = stepHandlingParameters.get(selected).isLast;

            // handle all states at once
            globalHandler.handleStep(restricted, isLast);

            if (!isLast) {
                // advance one step
                stepHandlingParameters.set(selected,
                                           getParameters(selected, futures.get(selected), shpQueues.get(selected)));
            }

            previousDate = selectedStepEnd;

        }

        // stop all remaining propagators
        executorService.shutdownNow();

        // extract the final states
        for (int i = 0; i < n; ++i) {
            try {
                finalStates.set(i, futures.get(i).get());
            } catch (InterruptedException | ExecutionException e) {

                // sort out if exception was intentional or not
                manageException(e);

                // this propagator was intentionally stopped,
                // we retrieve the final state from the last available interpolator
                finalStates.set(i, stepHandlingParameters.get(i).interpolator.getInterpolatedState(previousDate));

            }
        }

        return finalStates;

    }

    /** Retrieve parameters.
     * @param index index of the propagator
     * @param future propagation task
     * @param queue queue for transferring parameters
     * @param <T> type of the parameters
     * @return retrieved parameters
     */
    private <T> T getParameters(final int index,
                                final Future<SpacecraftState> future,
                                final SynchronousQueue<T> queue) {
        try {
            T params = null;
            while (params == null && !future.isDone()) {
                params = queue.poll(MAX_WAIT, TimeUnit.MILLISECONDS);
            }
            if (params == null) {
                // call Future.get just for the side effect of retrieving the exception
                // in case the propagator ended due to an exception
                future.get();
            }
            return params;
        } catch (InterruptedException | ExecutionException e) {
            manageException(e);
            return null;
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
        public void handleStep(final OrekitStepInterpolator interpolator, final boolean isLast) {
            globalHandler.handleStep(Collections.singletonList(interpolator), isLast);
        }

    }

    /** Local class for handling multiple propagator steps. */
    private static class MultiplePropagatorsHandler implements OrekitStepHandler {

        /** Queue for passing initial state. */
        private final SynchronousQueue<SpacecraftState> initQueue;

        /** Queue for passing step handling parameters. */
        private final SynchronousQueue<StepHandlingParameters> shpQueue;

        /** Simple constructor.
         * @param initQueue queuefor passing initial state
         * @param shpQueue queue for passing step handling parameters.
         */
        MultiplePropagatorsHandler(final SynchronousQueue<SpacecraftState> initQueue,
                                   final SynchronousQueue<StepHandlingParameters> shpQueue) {
            this.initQueue = initQueue;
            this.shpQueue  = shpQueue;
        }


        /** {@inheritDoc} */
        @Override
        public void init(final SpacecraftState s0, final AbsoluteDate t) {
            try {
                initQueue.put(s0);
            } catch (InterruptedException ie) {
                // use a dedicated exception to stop thread almost gracefully
                throw new PropagatorStoppingException(ie);
            }
        }

        /** {@inheritDoc} */
        @Override
        public void handleStep(final OrekitStepInterpolator interpolator, final boolean isLast) {
            try {
                shpQueue.put(new StepHandlingParameters(interpolator, isLast));
            } catch (InterruptedException ie) {
                // use a dedicated exception to stop thread almost gracefully
                throw new PropagatorStoppingException(ie);
            }
        }

    }

    /** Local class holding parameters for one step handling. */
    private static class StepHandlingParameters implements TimeStamped {

        /** Interpolator set up for the current step. */
        private final OrekitStepInterpolator interpolator;

        /** Indicator for last step. */
        private final boolean isLast;

        /** Simple constructor.
         * @param interpolator interpolator set up for the current step
         * @param isLast if true, this is the last integration step
         */
        StepHandlingParameters(final OrekitStepInterpolator interpolator, final boolean isLast) {
            this.interpolator = interpolator;
            this.isLast       = isLast;
        }

        /** {@inheritDoc} */
        @Override
        public AbsoluteDate getDate() {
            return interpolator.getCurrentState().getDate();
        }

    }

}
