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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.intervals.AdaptableInterval;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeInterval;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TimeIntervalDetectorTest {

    @Test
    void testGetter() {
        // GIVEN
        final AbsoluteDate startDate = AbsoluteDate.ARBITRARY_EPOCH;
        final TimeInterval interval = TimeInterval.of(startDate, startDate.shiftedBy(1.));
        final TimeIntervalDetector detector = new TimeIntervalDetector(mock(EventHandler.class), interval);
        // WHEN
        final TimeInterval actualInterval = detector.getTimeInterval();
        // THEN
        assertEquals(interval, actualInterval);
    }

    @Test
    void testDependsOnlyOnTime() {
        // GIVEN
        final AbsoluteDate startDate = AbsoluteDate.ARBITRARY_EPOCH;
        final TimeInterval interval = TimeInterval.of(startDate, startDate.shiftedBy(1.));
        final TimeIntervalDetector detector = new TimeIntervalDetector(mock(EventHandler.class), interval);
        // WHEN
        final boolean value = detector.dependsOnTimeOnly();
        // THEN
        Assertions.assertTrue(value);
    }

    @Test
    void testGValue() {
        // GIVEN
        final AbsoluteDate startDate = AbsoluteDate.ARBITRARY_EPOCH;
        final TimeInterval interval = TimeInterval.of(startDate, startDate.shiftedBy(1.));
        final TimeIntervalDetector detector = new TimeIntervalDetector(mock(EventHandler.class), interval);
        // WHEN & THEN
        final double expectedG = 0.;
        assertEquals(expectedG, detector.g(mockState(interval.getStartDate())));
        assertEquals(expectedG, detector.g(mockState(interval.getEndDate())));
    }

    @Test
    void testGSign() {
        // GIVEN
        final AbsoluteDate startDate = AbsoluteDate.ARBITRARY_EPOCH;
        final double dt = 1;
        final TimeInterval interval = TimeInterval.of(startDate, startDate.shiftedBy(dt));
        final TimeIntervalDetector detector = new TimeIntervalDetector(mock(EventHandler.class), interval);
        // WHEN & THEN
        assertTrue(detector.g(mockState(interval.getStartDate().shiftedBy(-dt))) < 0.);
        assertTrue(detector.g(mockState(interval.getStartDate().shiftedBy(dt / 2))) > 0.);
        assertTrue(detector.g(mockState(interval.getEndDate().shiftedBy(dt))) < 0.);
    }

    @Test
    void testCreate() {
        // GIVEN
        final AbsoluteDate startDate = AbsoluteDate.ARBITRARY_EPOCH;
        final TimeInterval interval = TimeInterval.of(startDate, startDate.shiftedBy(1));
        final TimeIntervalDetector detector = new TimeIntervalDetector(mock(EventHandler.class), interval);
        final EventDetectionSettings detectionSettings = new EventDetectionSettings(mock(AdaptableInterval.class),
                1., 1);
        final EventHandler mockedHandler = mock();
        // WHEN
        final TimeIntervalDetector createdDetector = detector.create(detectionSettings, mockedHandler);
        // THEN
        assertEquals(detectionSettings, createdDetector.getDetectionSettings());
        assertEquals(mockedHandler, createdDetector.getHandler());
    }

    private static SpacecraftState mockState(final AbsoluteDate date) {
        final SpacecraftState state = mock();
        when(state.getDate()).thenReturn(date);
        return state;
    }
}
