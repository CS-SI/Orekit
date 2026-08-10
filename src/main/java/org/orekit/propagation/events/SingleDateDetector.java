/* Copyright 2022-2026 Romain Serra
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

import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.functions.SingleDateEventFunction;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.StopOnEvent;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeShiftable;
import org.orekit.time.TimeStamped;

/** Finder for single date detection.
 * The event function is positive after, negative before.
 * @author Romain Serra
 * @since 14.0
 * @see DateDetector
 */
public class SingleDateDetector extends AbstractDetector<SingleDateDetector> implements TimeStamped, TimeShiftable<SingleDateDetector> {

    /** Date to detect. */
    private final AbsoluteDate date;

    /** Full constructor.
     * @param eventFunction event function
     * @param detectionSettings event detection settings
     * @param eventHandler event handler
     */
    public SingleDateDetector(final SingleDateEventFunction eventFunction,
                              final EventDetectionSettings detectionSettings, final EventHandler eventHandler) {
        super(eventFunction, detectionSettings, eventHandler);
        this.date = eventFunction.getDate();
    }

    /** Build a new instance with default detection settings.
     * @param eventHandler event handler
     * @param date event date
     */
    public SingleDateDetector(final EventHandler eventHandler, final AbsoluteDate date) {
        this(new SingleDateEventFunction(date),
                new EventDetectionSettings(DateDetector.DEFAULT_MAX_CHECK, DateDetector.DEFAULT_THRESHOLD, DEFAULT_MAX_ITER),
                eventHandler);
    }

    /** Build a new instance with default detection settings and event handler (stop on event).
     * @param date event date
     */
    public SingleDateDetector(final AbsoluteDate date) {
        this(new StopOnEvent(), date);
    }

    /** {@inheritDoc} */
    @Override
    protected SingleDateDetector create(final EventDetectionSettings detectionSettings, final EventHandler newHandler) {
        return new SingleDateDetector((SingleDateEventFunction) getEventFunction(), detectionSettings, newHandler);
    }

    /** {@inheritDoc} */
    @Override
    public double g(final SpacecraftState s) {
        return getEventFunction().value(s);
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getDate() {
        return date;
    }

    /** {@inheritDoc} */
    @Override
    public SingleDateDetector shiftedBy(final double dt) {
        return withDate(date.shiftedBy(dt));
    }

    /**
     * Create new instance with input date.
     * @param newDate new date to be detected
     * @return new detector
     */
    public SingleDateDetector withDate(final AbsoluteDate newDate) {
        return new SingleDateDetector(new SingleDateEventFunction(newDate), getDetectionSettings(), getHandler());
    }
}
