/* Copyright 2022-2025 Romain Serra
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
import org.orekit.propagation.events.handlers.FieldContinueOnEvent;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.propagation.events.intervals.DateDetectionAdaptableIntervalFactory;
import org.orekit.propagation.events.intervals.FieldAdaptableInterval;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeInterval;


/**
 * Detector for time intervals. Positive whenever the date is inside, negative otherwise.
 *
 * @author Romain Serra
 * @since 13.1
 * @see TimeInterval
 * @see TimeIntervalDetector
 */
public class FieldTimeIntervalDetector<T extends CalculusFieldElement<T>>
        extends FieldAbstractDetector<FieldTimeIntervalDetector<T>, T> {

    /** Time interval for detection. */
    private final TimeInterval timeInterval;

    /**
     * Constructor with default detection settings and handler.
     * @param field field
     * @param timeInterval time interval
     */
    public FieldTimeIntervalDetector(final Field<T> field, final TimeInterval timeInterval) {
        this(getDefaultDetectionSettings(field, timeInterval), new FieldContinueOnEvent<>(), timeInterval);
    }

    /**
     * Constructor.
     * @param detectionSettings event detection settings
     * @param handler event handler
     * @param timeInterval time interval
     */
    public FieldTimeIntervalDetector(final FieldEventDetectionSettings<T> detectionSettings,
                                     final FieldEventHandler<T> handler,
                                     final TimeInterval timeInterval) {
        super(detectionSettings, handler);
        this.timeInterval = timeInterval;
    }

    /**
     * Get the default detection settings.
     * @param field field
     * @param timeInterval time interval
     * @return default detection settings
     * @param <W> field type
     */
    private static <W extends CalculusFieldElement<W>> FieldEventDetectionSettings<W> getDefaultDetectionSettings(final Field<W> field,
                                                                                                                  final TimeInterval timeInterval) {
        final FieldAbsoluteDate<W> fieldStartDate = new FieldAbsoluteDate<>(field, timeInterval.getStartDate());
        final FieldAbsoluteDate<W> fieldEndDate = new FieldAbsoluteDate<>(field, timeInterval.getEndDate());
        final FieldAdaptableInterval<W> adaptableInterval = DateDetectionAdaptableIntervalFactory
                .getDatesDetectionFieldInterval(fieldStartDate, fieldEndDate);
        final W fieldThreshold = field.getZero().newInstance(DateDetector.DEFAULT_THRESHOLD);
        return new FieldEventDetectionSettings<>(adaptableInterval, fieldThreshold, DEFAULT_MAX_ITER);
    }

    /**
     * Getter for the time interval.
     * @return interval
     */
    public TimeInterval getTimeInterval() {
        return timeInterval;
    }

    @Override
    protected FieldTimeIntervalDetector<T> create(final FieldEventDetectionSettings<T> detectionSettings,
                                                 final FieldEventHandler<T> newHandler) {
        return new FieldTimeIntervalDetector<>(detectionSettings, newHandler, timeInterval);
    }

    @Override
    public T g(final FieldSpacecraftState<T> s) {
        final FieldAbsoluteDate<T> date = s.getDate();
        return (date.durationFrom(timeInterval.getStartDate())).multiply(date.durationFrom(timeInterval.getEndDate())).negate();
    }
}
