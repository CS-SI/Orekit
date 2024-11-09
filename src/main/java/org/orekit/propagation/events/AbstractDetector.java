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

import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.intervals.AdaptableInterval;
import org.orekit.time.AbsoluteDate;

/** Common parts shared by several orbital events finders.
 * @param <T> type of the detector
 * @see org.orekit.propagation.Propagator#addEventDetector(EventDetector)
 * @author Luc Maisonobe
 */
public abstract class AbstractDetector<T extends AbstractDetector<T>> implements EventDetector {

    /** Default maximum checking interval (s). */
    public static final double DEFAULT_MAXCHECK = EventDetectionSettings.DEFAULT_MAXCHECK;

    /** Default convergence threshold (s). */
    public static final double DEFAULT_THRESHOLD = EventDetectionSettings.DEFAULT_THRESHOLD;

    /** Default maximum number of iterations in the event time search. */
    public static final int DEFAULT_MAX_ITER = EventDetectionSettings.DEFAULT_MAX_ITER;

    /** Detection settings. */
    private final EventDetectionSettings eventDetectionSettings;

    /** Default handler for event overrides. */
    private final EventHandler handler;

    /** Propagation direction. */
    private boolean forward;

    /** Build a new instance.
     * @param maxCheck maximum checking interval, must be strictly positive (s)
     * @param threshold convergence threshold (s)
     * @param maxIter maximum number of iterations in the event time search
     * @param handler event handler to call at event occurrences
     */
    protected AbstractDetector(final double maxCheck, final double threshold, final int maxIter,
                               final EventHandler handler) {
        this(new EventDetectionSettings(maxCheck, threshold, maxIter), handler);
    }

    /** Build a new instance.
     * @param detectionSettings event detection settings
     * @param handler event handler to call at event occurrences
     * @since 12.2
     */
    protected AbstractDetector(final EventDetectionSettings detectionSettings, final EventHandler handler) {
        checkStrictlyPositive(detectionSettings.getThreshold());
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

    /**
     * {@inheritDoc}
     *
     * <p> This implementation sets the direction of propagation and initializes the event
     * handler. If a subclass overrides this method it should call {@code
     * super.init(s0, t)}.
     */
    @Override
    public void init(final SpacecraftState s0,
                     final AbsoluteDate t) {
        forward = t.durationFrom(s0.getDate()) >= 0.0;
        getHandler().init(s0, t, this);
    }

    /** {@inheritDoc} */
    @Override
    public EventDetectionSettings getDetectionSettings() {
        return eventDetectionSettings;
    }

    /**
     * Set up the maximum checking interval.
     * <p>
     * This will override a maximum checking interval if it has been configured previously.
     * </p>
     * @param newMaxCheck maximum checking interval (s)
     * @return a new detector with updated configuration (the instance is not changed)
     * @since 6.1
     */
    public T withMaxCheck(final double newMaxCheck) {
        return withMaxCheck(AdaptableInterval.of(newMaxCheck));
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
    public T withMaxCheck(final AdaptableInterval newMaxCheck) {
        return create(new EventDetectionSettings(newMaxCheck, getThreshold(), getMaxIterationCount()), getHandler());
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
    public T withMaxIter(final int newMaxIter) {
        return create(new EventDetectionSettings(getMaxCheckInterval(), getThreshold(), newMaxIter), getHandler());
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
    public T withThreshold(final double newThreshold) {
        return create(new EventDetectionSettings(getMaxCheckInterval(), newThreshold, getMaxIterationCount()), getHandler());
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
    public T withDetectionSettings(final EventDetectionSettings newSettings) {
        return create(new EventDetectionSettings(newSettings.getMaxCheckInterval(), newSettings.getThreshold(), newSettings.getMaxIterationCount()),
                getHandler());
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
    public T withHandler(final EventHandler newHandler) {
        return create(getDetectionSettings(), newHandler);
    }

    /** {@inheritDoc} */
    @Override
    public EventHandler getHandler() {
        return handler;
    }

    /** Build a new instance.
     * @param detectionSettings detection settings
     * @param newHandler event handler to call at event occurrences
     * @return a new instance of the appropriate sub-type
     * @since 12.2
     */
    protected abstract T create(EventDetectionSettings detectionSettings, EventHandler newHandler);

    /** Check if the current propagation is forward or backward.
     * @return true if the current propagation is forward
     * @since 7.2
     */
    public boolean isForward() {
        return forward;
    }

}
