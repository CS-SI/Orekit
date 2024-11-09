/* Copyright 2002-2024 CS GROUP
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
import org.orekit.propagation.events.intervals.FieldAdaptableInterval;
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
    public static final double DEFAULT_MAXCHECK = FieldEventDetectionSettings.DEFAULT_MAXCHECK;

    /** Default convergence threshold (s). */
    public static final double DEFAULT_THRESHOLD = FieldEventDetectionSettings.DEFAULT_THRESHOLD;

    /** Default maximum number of iterations in the event time search. */
    public static final int DEFAULT_MAX_ITER = FieldEventDetectionSettings.DEFAULT_MAX_ITER;

    /** Detection settings. */
    private final FieldEventDetectionSettings<T> eventDetectionSettings;

    /** Default handler for event overrides. */
    private final FieldEventHandler<T> handler;

    /** Propagation direction. */
    private boolean forward;

    /** Build a new instance.
     * @param detectionSettings event detection settings
     * @param handler event handler to call at event occurrences
     * @since 12.2
     */
    protected FieldAbstractDetector(final FieldEventDetectionSettings<T> detectionSettings,
                                    final FieldEventHandler<T> handler) {
        checkStrictlyPositive(detectionSettings.getThreshold().getReal());
        this.eventDetectionSettings = detectionSettings;
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
    @Override
    public void init(final FieldSpacecraftState<T> s0,
                     final FieldAbsoluteDate<T> t) {
        forward = t.durationFrom(s0.getDate()).getReal() >= 0.0;
        getHandler().init(s0, t, this);
    }

    /** {@inheritDoc} */
    @Override
    public FieldEventDetectionSettings<T> getDetectionSettings() {
        return eventDetectionSettings;
    }

    /**
     * Set up the maximum checking interval.
     * <p>
     * This will override a maximum checking interval if it has been configured previously.
     * </p>
     * @param newMaxCheck maximum checking interval (s)
     * @return a new detector with updated configuration (the instance is not changed)
     * @since 12.0
     */
    public D withMaxCheck(final double newMaxCheck) {
        return withMaxCheck(FieldAdaptableInterval.of(newMaxCheck));
    }

    /**
     * Set up the maximum checking interval.
     * <p>
     * This will override a maximum checking interval if it has been configured previously.
     * </p>
     * @param newMaxCheck maximum checking interval (s)
     * @return a new detector with updated configuration (the instance is not changed)
     * @since 12.0
     */
    public D withMaxCheck(final FieldAdaptableInterval<T> newMaxCheck) {
        return create(new FieldEventDetectionSettings<>(newMaxCheck, getThreshold(), getMaxIterationCount()), getHandler());
    }

    /**
     * Set up the maximum number of iterations in the event time search.
     * <p>
     * This will override a number of iterations if it has been configured previously.
     * </p>
     * @param newMaxIter maximum number of iterations in the event time search
     * @return a new detector with updated configuration (the instance is not changed)
     * @since 6.1
     */
    public D withMaxIter(final int newMaxIter) {
        return create(new FieldEventDetectionSettings<>(getMaxCheckInterval(), getThreshold(), newMaxIter), getHandler());
    }

    /**
     * Set up the convergence threshold.
     * <p>
     * This will override a convergence threshold if it has been configured previously.
     * </p>
     * @param newThreshold convergence threshold (s)
     * @return a new detector with updated configuration (the instance is not changed)
     * @since 6.1
     */
    public D withThreshold(final T newThreshold) {
        return create(new FieldEventDetectionSettings<>(getMaxCheckInterval(), newThreshold, getMaxIterationCount()), getHandler());
    }

    /**
     * Set up the event detection settings.
     * <p>
     * This will override settings previously configured.
     * </p>
     * @param newSettings new event detection settings
     * @return a new detector with updated configuration (the instance is not changed)
     * @since 12.2
     */
    public D withDetectionSettings(final FieldEventDetectionSettings<T> newSettings) {
        return create(newSettings, getHandler());
    }

    /**
     * Set up the event handler to call at event occurrences.
     * <p>
     * This will override a handler if it has been configured previously.
     * </p>
     * @param newHandler event handler to call at event occurrences
     * @return a new detector with updated configuration (the instance is not changed)
     * @since 6.1
     */
    public D withHandler(final FieldEventHandler<T> newHandler) {
        return create(getDetectionSettings(), newHandler);
    }

    /** {@inheritDoc} */
    public FieldEventHandler<T> getHandler() {
        return handler;
    }

    /** Build a new instance.
     * @param detectionSettings detection settings
     * @param newHandler event handler to call at event occurrences
     * @return a new instance of the appropriate sub-type
     * @since 12.2
     */
    protected abstract D create(FieldEventDetectionSettings<T> detectionSettings, FieldEventHandler<T> newHandler);

    /** Check if the current propagation is forward or backward.
     * @return true if the current propagation is forward
     * @since 7.2
     */
    public boolean isForward() {
        return forward;
    }

}
