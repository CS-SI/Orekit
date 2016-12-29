/* Copyright 2002-2016 CS Systèmes d'Information
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
package org.orekit.propagation.events;

import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;

/** Common parts shared by several orbital events finders.
 * @see org.orekit.propagation.Propagator#addEventDetector(EventDetector)
 * @author Luc Maisonobe
 */
public abstract class AbstractDetector<T extends EventDetector> implements EventDetector {

    /** Default maximum checking interval (s). */
    public static final double DEFAULT_MAXCHECK = 600;

    /** Default convergence threshold (s). */
    public static final double DEFAULT_THRESHOLD = 1.e-6;

    /** Default cmaximum number of iterations in the event time search. */
    public static final int DEFAULT_MAX_ITER = 100;

    /** Serializable UID. */
    private static final long serialVersionUID = 20131202l;

    /** Max check interval. */
    private final double maxCheck;

    /** Convergence threshold. */
    private final double threshold;

    /** Maximum number of iterations in the event time search. */
    private final int maxIter;

    /** Default handler for event overrides. */
    private final EventHandler<? super T> handler;

    /** Propagation direction. */
    private boolean forward;

    /** Build a new instance.
     * @param maxCheck maximum checking interval (s)
     * @param threshold convergence threshold (s)
     * @param maxIter maximum number of iterations in the event time search
     * @param handler event handler to call at event occurrences
     */
    protected AbstractDetector(final double maxCheck, final double threshold, final int maxIter,
                               final EventHandler<? super T> handler) {
        this.maxCheck  = maxCheck;
        this.threshold = threshold;
        this.maxIter   = maxIter;
        this.handler   = handler;
        this.forward   = true;
    }

    /** {@inheritDoc} */
    public void init(final SpacecraftState s0, final AbsoluteDate t) {
        forward = t.durationFrom(s0.getDate()) >= 0.0;
    }

    /** {@inheritDoc} */
    public abstract double g(SpacecraftState s) throws OrekitException;

    /** {@inheritDoc} */
    public double getMaxCheckInterval() {
        return maxCheck;
    }

    /** {@inheritDoc} */
    public int getMaxIterationCount() {
        return maxIter;
    }

    /** {@inheritDoc} */
    public double getThreshold() {
        return threshold;
    }

    /**
     * Setup the maximum checking interval.
     * <p>
     * This will override a maximum checking interval if it has been configured previously.
     * </p>
     * @param newMaxCheck maximum checking interval (s)
     * @return a new detector with updated configuration (the instance is not changed)
     * @since 6.1
     */
    public T withMaxCheck(final double newMaxCheck) {
        return create(newMaxCheck, getThreshold(), getMaxIterationCount(), getHandler());
    }

    /**
     * Setup the maximum number of iterations in the event time search.
     * <p>
     * This will override a number of iterations if it has been configured previously.
     * </p>
     * @param newMaxIter maximum number of iterations in the event time search
     * @return a new detector with updated configuration (the instance is not changed)
     * @since 6.1
     */
    public T withMaxIter(final int newMaxIter) {
        return create(getMaxCheckInterval(), getThreshold(), newMaxIter,  getHandler());
    }

    /**
     * Setup the convergence threshold.
     * <p>
     * This will override a convergence threshold if it has been configured previously.
     * </p>
     * @param newThreshold convergence threshold (s)
     * @return a new detector with updated configuration (the instance is not changed)
     * @since 6.1
     */
    public T withThreshold(final double newThreshold) {
        return create(getMaxCheckInterval(), newThreshold, getMaxIterationCount(),  getHandler());
    }

    /**
     * Setup the event handler to call at event occurrences.
     * <p>
     * This will override a handler if it has been configured previously.
     * </p>
     * @param newHandler event handler to call at event occurrences
     * @return a new detector with updated configuration (the instance is not changed)
     * @since 6.1
     */
    public T withHandler(final EventHandler<? super T> newHandler) {
        return create(getMaxCheckInterval(), getThreshold(), getMaxIterationCount(), newHandler);
    }

    /** Get the handler.
     * @return event handler to call at event occurrences
     */
    public EventHandler<? super T> getHandler() {
        return handler;
    }

    /** {@inheritDoc} */
    public EventHandler.Action eventOccurred(final SpacecraftState s, final boolean increasing)
        throws OrekitException {
        @SuppressWarnings("unchecked")
        final EventHandler.Action whatNext = getHandler().eventOccurred(s, (T) this, increasing);
        return whatNext;
    }

    /** {@inheritDoc} */
    public SpacecraftState resetState(final SpacecraftState oldState) throws OrekitException {
        @SuppressWarnings("unchecked")
        final SpacecraftState newState = getHandler().resetState((T) this, oldState);
        return newState;
    }

    /** Build a new instance.
     * @param newMaxCheck maximum checking interval (s)
     * @param newThreshold convergence threshold (s)
     * @param newMaxIter maximum number of iterations in the event time search
     * @param newHandler event handler to call at event occurrences
     * @return a new instance of the appropriate sub-type
     */
    protected abstract T create(double newMaxCheck, double newThreshold,
                                int newMaxIter, EventHandler<? super T> newHandler);

    /** Check if the current propagation is forward or backward.
     * @return true if the current propagation is forward
     * @since 7.2
     */
    public boolean isForward() {
        return forward;
    }

}
