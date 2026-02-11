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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.functions.SingleDateEventFunction;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.propagation.events.handlers.FieldStopOnEvent;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeShiftable;
import org.orekit.time.TimeStamped;

/** Finder for single date detection.
 * The event function is positive after, negative before.
 * @author Romain Serra
 * @since 14.0
 * @see SingleDateDetector
 * @see FieldDateDetector
 */
public class FieldSingleDateDetector<T extends CalculusFieldElement<T>>
        extends FieldAbstractDetector<FieldSingleDateDetector<T>, T>
        implements TimeStamped, TimeShiftable<FieldSingleDateDetector<T>> {

    /** Date to detect. */
    private final AbsoluteDate date;

    /** Full constructor.
     * @param eventFunction event function
     * @param detectionSettings event detection settings
     * @param eventHandler event handler
     */
    public FieldSingleDateDetector(final SingleDateEventFunction eventFunction,
                                   final FieldEventDetectionSettings<T> detectionSettings,
                                   final FieldEventHandler<T> eventHandler) {
        super(eventFunction, detectionSettings, eventHandler);
        this.date = eventFunction.getDate();
    }

    /** Build a new instance with default detection settings and handler (stop on event).
     * @param field field type
     * @param date event date
     */
    public FieldSingleDateDetector(final Field<T> field, final AbsoluteDate date) {
        this(new SingleDateEventFunction(date), new FieldEventDetectionSettings<>(FieldDateDetector.DEFAULT_MAX_CHECK,
                field.getZero().newInstance(FieldDateDetector.DEFAULT_THRESHOLD),
                DEFAULT_MAX_ITER), new FieldStopOnEvent<>());
    }

    /** {@inheritDoc} */
    @Override
    protected FieldSingleDateDetector<T> create(final FieldEventDetectionSettings<T> detectionSettings,
                                                final FieldEventHandler<T> newHandler) {
        return new FieldSingleDateDetector<>((SingleDateEventFunction) getEventFunction(), detectionSettings, newHandler);
    }

    /** {@inheritDoc} */
    @Override
    public T g(final FieldSpacecraftState<T> s) {
        return getEventFunction().value(s);
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getDate() {
        return date;
    }

    /** {@inheritDoc} */
    @Override
    public FieldSingleDateDetector<T> shiftedBy(final double dt) {
        return withDate(date.shiftedBy(dt));
    }

    /**
     * Create new instance with input date.
     * @param newDate new date to be detected
     * @return new detector
     */
    public FieldSingleDateDetector<T> withDate(final AbsoluteDate newDate) {
        return new FieldSingleDateDetector<>(new SingleDateEventFunction(newDate), getDetectionSettings(), getHandler());
    }

    @Override
    public SingleDateDetector toEventDetector(final EventHandler eventHandler) {
        return new SingleDateDetector((SingleDateEventFunction) getEventFunction(),
                getDetectionSettings().toEventDetectionSettings(), eventHandler);
    }
}
