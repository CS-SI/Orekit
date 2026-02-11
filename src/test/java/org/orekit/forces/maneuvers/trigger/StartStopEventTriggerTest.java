/* Copyright 2002-2026 CS GROUP
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

import java.util.List;

import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.TestUtils;
import org.orekit.frames.FramesFactory;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.EventEnablingPredicateFilter;
import org.orekit.propagation.events.EventShifter;
import org.orekit.propagation.events.EventSlopeFilter;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.events.FieldEventEnablingPredicateFilter;
import org.orekit.propagation.events.FieldEventShifter;
import org.orekit.propagation.events.FieldEventSlopeFilter;
import org.orekit.propagation.events.FilterType;
import org.orekit.propagation.events.NodeDetector;
import org.orekit.propagation.events.handlers.StopOnEvent;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeStamped;


class StartStopEventTriggerTest extends AbstractManeuverTriggersTest<StartStopEventsTrigger<DateDetector, DateDetector>> {

    public static class StartStopDates extends StartStopEventsTrigger<DateDetector, DateDetector> {

        public StartStopDates(final AbsoluteDate start, final AbsoluteDate stop) {
            super(new DateDetector(start, stop.shiftedBy(10.0)).
                  withMaxCheck(5.0).
                  withThreshold(1.0e-10).
                  withHandler(new StopOnEvent()),
                  new DateDetector(stop, stop.shiftedBy(20.0)).
                  withMaxCheck(5.0).
                  withThreshold(1.0e-10).
                  withHandler(new StopOnEvent()));
        }
    }

    protected StartStopEventsTrigger<DateDetector, DateDetector> createTrigger(final AbsoluteDate start, final AbsoluteDate stop) {
        return new StartStopDates(start, stop);
    }

    @Test
    void testComponents() {
        StartStopEventsTrigger<DateDetector, DateDetector> trigger = createTrigger(AbsoluteDate.J2000_EPOCH,
                                                                                   AbsoluteDate.J2000_EPOCH.shiftedBy(100.0));
        final List<TimeStamped>    startDates = trigger.getStartDetector().getDates();
        final List<TimeStamped>    stopDates  = trigger.getStopDetector().getDates();
        Assertions.assertEquals(2,     trigger.getEventDetectors().count());
        Assertions.assertEquals(2,     trigger.getFieldEventDetectors(Binary64Field.getInstance()).count());
        Assertions.assertEquals(2,     startDates.size());
        Assertions.assertEquals(  0.0, startDates.get(0).getDate().durationFrom(AbsoluteDate.J2000_EPOCH), 1.0e-10);
        Assertions.assertEquals(110.0, startDates.get(1).getDate().durationFrom(AbsoluteDate.J2000_EPOCH), 1.0e-10);
        Assertions.assertEquals(2,     stopDates.size());
        Assertions.assertEquals(100.0, stopDates.get(0).getDate().durationFrom(AbsoluteDate.J2000_EPOCH), 1.0e-10);
        Assertions.assertEquals(120.0, stopDates.get(1).getDate().durationFrom(AbsoluteDate.J2000_EPOCH), 1.0e-10);
    }

    @Test
    void testConvertDetector() {
        // GIVEN
        final EventShifter startDetector = new EventShifter(new DateDetector(), true, 1., 2.);
        final EventSlopeFilter<EventDetector> slopeFilter = new EventSlopeFilter<>(new NodeDetector(FramesFactory.getGCRF()),
                FilterType.TRIGGER_ONLY_DECREASING_EVENTS);
        final EventEnablingPredicateFilter stopDetector = new EventEnablingPredicateFilter(slopeFilter, (state, detector, g) -> true);
        final StartStopEventsTrigger<EventShifter, EventDetector> trigger = new StartStopEventsTrigger<>(startDetector,
                stopDetector);
        final Binary64Field field = Binary64Field.getInstance();
        final SpacecraftState state = new SpacecraftState(TestUtils.getDefaultOrbit(AbsoluteDate.ARBITRARY_EPOCH));
        final FieldSpacecraftState<Binary64> fieldState = new FieldSpacecraftState<>(field, state);
        // WHEN
        final FieldEventDetector<Binary64> fieldStartDetector = trigger.convertDetector(field, startDetector);
        final FieldEventDetector<Binary64> fieldStopDetector = trigger.convertDetector(field, stopDetector);
        // THEN
        Assertions.assertInstanceOf(FieldEventShifter.class, fieldStartDetector);
        Assertions.assertEquals(startDetector.getThreshold(), fieldStartDetector.getThreshold().getReal());
        final FieldEventShifter<Binary64> fieldEventShifter = (FieldEventShifter<Binary64>) fieldStartDetector;
        Assertions.assertEquals(startDetector.isUseShiftedStates(), fieldEventShifter.isUseShiftedStates());
        Assertions.assertEquals(startDetector.getIncreasingTimeShift(), fieldEventShifter.getIncreasingTimeShift().getReal());
        Assertions.assertEquals(startDetector.getDecreasingTimeShift(), fieldEventShifter.getDecreasingTimeShift().getReal());
        Assertions.assertInstanceOf(FieldEventEnablingPredicateFilter.class, fieldStopDetector);
        Assertions.assertEquals(stopDetector.getThreshold(), fieldStopDetector.getThreshold().getReal());
        final FieldEventEnablingPredicateFilter<Binary64> fieldEventEnablingPredicateFilter = (FieldEventEnablingPredicateFilter<Binary64>) fieldStopDetector;
        Assertions.assertInstanceOf(FieldEventSlopeFilter.class, fieldEventEnablingPredicateFilter.getDetector());
        final FieldEventSlopeFilter<?, Binary64> fieldEventSlopeFilter = (FieldEventSlopeFilter<?, Binary64>) (fieldEventEnablingPredicateFilter.getDetector());
        Assertions.assertEquals(slopeFilter.getFilterType(), fieldEventSlopeFilter.getFilterType());
        Assertions.assertEquals(startDetector.g(state), fieldStartDetector.g(fieldState).getReal());
        stopDetector.init(state, AbsoluteDate.FUTURE_INFINITY);
        fieldStopDetector.init(fieldState, FieldAbsoluteDate.getFutureInfinity(field));
        Assertions.assertEquals(stopDetector.g(state), fieldStopDetector.g(fieldState).getReal());
    }

}
