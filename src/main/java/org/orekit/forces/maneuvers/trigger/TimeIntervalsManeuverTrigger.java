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
package org.orekit.forces.maneuvers.trigger;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.orekit.propagation.events.BooleanDetector;
import org.orekit.propagation.events.FieldBooleanDetector;
import org.orekit.propagation.events.FieldEventDetectionSettings;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.events.FieldTimeIntervalDetector;
import org.orekit.propagation.events.TimeIntervalDetector;
import org.orekit.propagation.events.handlers.ContinueOnEvent;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.FieldContinueOnEvent;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.propagation.events.intervals.AdaptableInterval;
import org.orekit.propagation.events.intervals.DateDetectionAdaptableIntervalFactory;
import org.orekit.time.TimeInterval;
import org.orekit.time.TimeStamped;
import org.orekit.utils.ParameterDriver;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Maneuver trigger based on time intervals.
 *
 * @author Romain Serra
 * @since 13.1
 * @see TimeInterval
 * @see IntervalEventTrigger
 */
public class TimeIntervalsManeuverTrigger extends IntervalEventTrigger<BooleanDetector> {

    /**
     * Private constructor.
     * @param booleanDetector prototype detector
     */
    private TimeIntervalsManeuverTrigger(final BooleanDetector booleanDetector) {
        super(booleanDetector);
    }

    /**
     * Build an instance based on the input time intervals. Detectors are created with default settings.
     * @param timeIntervals intervals
     * @return maneuver trigger
     */
    public static TimeIntervalsManeuverTrigger of(final TimeInterval... timeIntervals) {
        final EventHandler arbitraryHandler = new ContinueOnEvent();
        final BooleanDetector detector = BooleanDetector.orCombine(Arrays.stream(timeIntervals)
                .map(timeInterval -> new TimeIntervalDetector(arbitraryHandler, timeInterval))
                .collect(Collectors.toList()));
        final Set<TimeStamped> dates = new HashSet<>();
        Arrays.stream(timeIntervals).forEach(interval -> dates.add(interval.getStartDate()));
        Arrays.stream(timeIntervals).forEach(interval -> dates.add(interval.getEndDate()));
        final AdaptableInterval maxCheck = DateDetectionAdaptableIntervalFactory
                .getDatesDetectionInterval(dates.toArray(new TimeStamped[0]));
        return new TimeIntervalsManeuverTrigger(detector.withMaxCheck(maxCheck));
    }

    /**
     * Build an instance based on the input time interval detectors.
     * @param timeIntervalDetectors detectors
     * @return maneuver trigger
     */
    public static TimeIntervalsManeuverTrigger of(final TimeIntervalDetector... timeIntervalDetectors) {
        final BooleanDetector detector = BooleanDetector.orCombine(timeIntervalDetectors);
        return new TimeIntervalsManeuverTrigger(detector);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <D extends FieldEventDetector<S>, S extends CalculusFieldElement<S>> D convertIntervalDetector(final Field<S> field,
                                                                                                             final BooleanDetector detector) {
        final FieldEventHandler<S> arbitraryHandler = new FieldContinueOnEvent<>();
        return (D) FieldBooleanDetector.orCombine(detector.getDetectors().stream()
                .map(TimeIntervalDetector.class::cast)
                .map(intervalDetector -> new FieldTimeIntervalDetector<>(new FieldEventDetectionSettings<>(field,
                        intervalDetector.getDetectionSettings()), arbitraryHandler, intervalDetector.getTimeInterval()))
                .collect(Collectors.toList()));
    }

    @Override
    public List<ParameterDriver> getParametersDrivers() {
        return Collections.emptyList();
    }
}
