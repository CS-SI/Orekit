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

import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Test;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.propagation.events.intervals.AdaptableInterval;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeInterval;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FieldTimeIntervalDetectorTest {

    @Test
    void testGetter() {
        // GIVEN
        final AbsoluteDate startDate = AbsoluteDate.ARBITRARY_EPOCH;
        final TimeInterval interval = TimeInterval.of(startDate, startDate.shiftedBy(1.));
        final FieldTimeIntervalDetector<Binary64> detector = new FieldTimeIntervalDetector<>(Binary64Field.getInstance(),
                interval);
        // WHEN
        final TimeInterval actualInterval = detector.getTimeInterval();
        // THEN
        assertEquals(interval, actualInterval);
    }

    @Test
    void testGValue() {
        // GIVEN
        final AbsoluteDate startDate = AbsoluteDate.ARBITRARY_EPOCH;
        final TimeInterval interval = TimeInterval.of(startDate, startDate.shiftedBy(1.));
        final FieldTimeIntervalDetector<Binary64> detector = new FieldTimeIntervalDetector<>(Binary64Field.getInstance(),
                interval);
        // WHEN & THEN
        final Binary64 expectedG = Binary64.ZERO;
        assertEquals(expectedG, detector.g(mockState(interval.getStartDate())));
        assertEquals(expectedG.negate(), detector.g(mockState(interval.getEndDate())));
    }

    @Test
    void testGSign() {
        // GIVEN
        final AbsoluteDate startDate = AbsoluteDate.ARBITRARY_EPOCH;
        final double dt = 1;
        final TimeInterval interval = TimeInterval.of(startDate, startDate.shiftedBy(dt));
        final FieldTimeIntervalDetector<Binary64> detector = new FieldTimeIntervalDetector<>(Binary64Field.getInstance(),
                interval);
        // WHEN & THEN
        assertTrue(detector.g(mockState(interval.getStartDate().shiftedBy(-dt))).getReal() < 0.);
        assertTrue(detector.g(mockState(interval.getStartDate().shiftedBy(dt / 2))).getReal() > 0.);
        assertTrue(detector.g(mockState(interval.getEndDate().shiftedBy(dt))).getReal() < 0.);
    }

    @Test
    void testCreate() {
        // GIVEN
        final AbsoluteDate startDate = AbsoluteDate.ARBITRARY_EPOCH;
        final TimeInterval interval = TimeInterval.of(startDate, startDate.shiftedBy(1));
        final FieldTimeIntervalDetector<Binary64> detector = new FieldTimeIntervalDetector<>(Binary64Field.getInstance(),
                interval);
        final EventDetectionSettings detectionSettings = new EventDetectionSettings(mock(AdaptableInterval.class),
                1., 1);
        final FieldEventDetectionSettings<Binary64> expectedSettings = new FieldEventDetectionSettings<>(Binary64Field.getInstance(),
                detectionSettings);
        @SuppressWarnings("unchecked")
        final FieldEventHandler<Binary64> mockedHandler = mock();
        // WHEN
        final FieldTimeIntervalDetector<Binary64> createdDetector = detector.create(expectedSettings, mockedHandler);
        // THEN
        assertEquals(expectedSettings, createdDetector.getDetectionSettings());
        assertEquals(mockedHandler, createdDetector.getHandler());
    }

    @SuppressWarnings("unchecked")
    private static FieldSpacecraftState<Binary64> mockState(final AbsoluteDate date) {
        final FieldSpacecraftState<Binary64> state = mock();
        when(state.getDate()).thenReturn(new FieldAbsoluteDate<>(Binary64Field.getInstance(), date));
        return state;
    }
}
