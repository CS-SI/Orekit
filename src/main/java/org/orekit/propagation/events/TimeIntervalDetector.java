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

import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.intervals.DateDetectionAdaptableIntervalFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeInterval;


/**
 * Detector for time intervals. Positive whenever the date is inside, negative otherwise.
 *
 * @author Romain Serra
 * @since 13.1
 * @see TimeInterval
 */
public class TimeIntervalDetector extends AbstractDetector<TimeIntervalDetector> {

    /** Time interval for detection. */
    private final TimeInterval timeInterval;

    /**
     * Constructor with default detection settings.
     * @param handler event handler
     * @param timeInterval time interval
     */
    public TimeIntervalDetector(final EventHandler handler, final TimeInterval timeInterval) {
        this(new EventDetectionSettings(DateDetectionAdaptableIntervalFactory.getDatesDetectionInterval(timeInterval.getStartDate(), timeInterval.getEndDate()),
                DateDetector.DEFAULT_THRESHOLD, DEFAULT_MAX_ITER), handler, timeInterval);
    }

    /**
     * Constructor.
     * @param detectionSettings event detection settings
     * @param handler event handler
     * @param timeInterval time interval
     */
    public TimeIntervalDetector(final EventDetectionSettings detectionSettings, final EventHandler handler,
                                final TimeInterval timeInterval) {
        super(detectionSettings, handler);
        this.timeInterval = timeInterval;
    }

    /**
     * Getter for the time interval.
     * @return interval
     */
    public TimeInterval getTimeInterval() {
        return timeInterval;
    }

    @Override
    protected TimeIntervalDetector create(final EventDetectionSettings detectionSettings, final EventHandler newHandler) {
        return new TimeIntervalDetector(detectionSettings, newHandler, timeInterval);
    }

    @Override
    public double g(final SpacecraftState s) {
        final AbsoluteDate date = s.getDate();
        return (date.durationFrom(timeInterval.getStartDate())) * (timeInterval.getEndDate().durationFrom(date));
    }
}
