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
package org.orekit.propagation.events;

import org.hipparchus.CalculusFieldElement;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.time.FieldAbsoluteDate;

/** Common parts shared by several orbital events finders.
 * @param <D> type of the detector
 * @param <T> type of the field element
 * @see org.orekit.propagation.Propagator#addEventDetector(EventDetector)
 * @author Luc Maisonobe
 */
public abstract class FieldAbstractDetector<D extends FieldAbstractDetector<D, T>, T extends CalculusFieldElement<T>>
    implements FieldEventDetector<T> {

    /** Default maximum checking interval (s). */
    public static final double DEFAULT_MAXCHECK = 600;

    /** Default convergence threshold (s). */
    public static final double DEFAULT_THRESHOLD = 1.e-6;

    /** Default cmaximum number of iterations in the event time search. */
    public static final int DEFAULT_MAX_ITER = 100;

    /** Max check interval. */
    private final FieldAdaptableInterval<T> maxCheck;

    /** Convergence threshold. */
    private final T threshold;

    /** Maximum number of iterations in the event time search. */
    private final int maxIter;

    /** Default handler for event overrides. */
    private final FieldEventHandler<T> handler;

    /** Propagation direction. */
    private boolean forward;

    /** Build a new instance.
     * @param maxCheck maximum checking interval
     * @param threshold convergence threshold (s)
     * @param maxIter maximum number of iterations in the event time search
     * @param handler event handler to call at event occurrences
     */
    protected FieldAbstractDetector(final FieldAdaptableInterval<T> maxCheck, final T threshold, final int maxIter,
                                    final FieldEventHandler<T> handler) {
        checkStrictlyPositive(threshold.getReal());
        this.maxCheck  = maxCheck;
        this.threshold = threshold;
        this.maxIter   = maxIter;
        this.handler   = handler;
        this.forward   = true;
    }

    /** Check value is strictly positive.
     * @param value value to check
     * @exception OrekitException if value is not strictly positive
     * @since 11.2
     */
    private void checkStrictlyPositive(final double value) throws OrekitException {
        if (value <= 0.0) {
            throw new OrekitException(OrekitMessages.NOT_STRICTLY_POSITIVE, value);
        }
    }

    /** {@inheritDoc} */
    public void init(final FieldSpacecraftState<T> s0,
                     final FieldAbsoluteDate<T> t) {
        forward = t.durationFrom(s0.getDate()).getReal() >= 0.0;
        getHandler().init(s0, t, this);
    }

    /** {@inheritDoc} */
    public abstract T g(FieldSpacecraftState<T> s);

    /** {@inheritDoc} */
    public FieldAdaptableInterval<T> getMaxCheckInterval() {
        return maxCheck;
    }

    /** {@inheritDoc} */
    public int getMaxIterationCount() {
        return maxIter;
    }

    /** {@inheritDoc} */
    public T getThreshold() {
        return threshold;
    }

    /**
     * Setup the maximum checking interval.
     * <p>
     * This will override a maximum checking interval if it has been configured previously.
     * </p>
     * @param newMaxCheck maximum checking interval (s)
     * @return a new detector with updated configuration (the instance is not changed)
     * @since 12.0
     */
    public D withMaxCheck(final double newMaxCheck) {
        return withMaxCheck(s -> newMaxCheck);
    }

    /**
     * Setup the maximum checking interval.
     * <p>
     * This will override a maximum checking interval if it has been configured previously.
     * </p>
     * @param newMaxCheck maximum checking interval (s)
     * @return a new detector with updated configuration (the instance is not changed)
     * @since 12.0
     */
    public D withMaxCheck(final FieldAdaptableInterval<T> newMaxCheck) {
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
    public D withMaxIter(final int newMaxIter) {
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
    public D withThreshold(final T newThreshold) {
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
    public D withHandler(final FieldEventHandler<T> newHandler) {
        return create(getMaxCheckInterval(), getThreshold(), getMaxIterationCount(), newHandler);
    }

    /** {@inheritDoc} */
    public FieldEventHandler<T> getHandler() {
        return handler;
    }

    /** Build a new instance.
     * @param newMaxCheck maximum checking interval
     * @param newThreshold convergence threshold (s)
     * @param newMaxIter maximum number of iterations in the event time search
     * @param newHandler event handler to call at event occurrences
     * @return a new instance of the appropriate sub-type
     */
    protected abstract D create(FieldAdaptableInterval<T> newMaxCheck, T newThreshold,
                                int newMaxIter, FieldEventHandler<T> newHandler);

    /** Check if the current propagation is forward or backward.
     * @return true if the current propagation is forward
     * @since 7.2
     */
    public boolean isForward() {
        return forward;
    }

}
