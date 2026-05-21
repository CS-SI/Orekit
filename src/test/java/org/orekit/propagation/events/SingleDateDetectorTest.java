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

import org.junit.jupiter.api.Test;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.StopOnEvent;
import org.orekit.time.AbsoluteDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SingleDateDetectorTest {

    @Test
    void testConstructor() {
        // GIVEN
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        // WHEN
        final SingleDateDetector detector = new SingleDateDetector(date);
        // THEN
        assertEquals(date, detector.getDate());
        assertInstanceOf(StopOnEvent.class, detector.getHandler());
        assertEquals(DateDetector.DEFAULT_THRESHOLD, detector.getThreshold());
        assertEquals(DateDetector.DEFAULT_MAX_ITER, detector.getMaxIterationCount());
    }

    @Test
    void testDependsOnTimeOnly() {
        // GIVEN
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        // WHEN
        final SingleDateDetector detector = new SingleDateDetector(date);
        // THEN
        assertTrue(detector.getEventFunction().dependsOnTimeOnly());
    }

    @Test
    void testG() {
        // GIVEN
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final SpacecraftState mockedState = mock();
        final double expectedG = 1.;
        when(mockedState.durationFrom(date)).thenReturn(expectedG);
        // WHEN
        final SingleDateDetector detector = new SingleDateDetector(date);
        // THEN
        assertEquals(expectedG, detector.g(mockedState));
    }

    @Test
    void testCreate() {
        // GIVEN
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final SingleDateDetector detector = new SingleDateDetector(date);
        final EventDetectionSettings detectionSettings = EventDetectionSettings.getDefaultEventDetectionSettings();
        final EventHandler handler = mock();
        // WHEN
        final SingleDateDetector newDetector = detector.create(detectionSettings, handler);
        // THEN
        assertEquals(date, newDetector.getDate());
        assertEquals(handler, newDetector.getHandler());
        assertEquals(detectionSettings, newDetector.getDetectionSettings());
    }

    @Test
    void testWithDate() {
        // GIVEN
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final EventHandler handler = mock();
        final SingleDateDetector detector = new SingleDateDetector(handler, date);
        final AbsoluteDate otherDate = date.shiftedBy(1.);
        // WHEN
        final SingleDateDetector newDetector = detector.withDate(otherDate);
        // THEN
        assertEquals(otherDate, newDetector.getDate());
        assertEquals(handler, newDetector.getHandler());
        assertEquals(detector.getDetectionSettings(), newDetector.getDetectionSettings());
    }

    @Test
    void testShiftedBy() {
        // GIVEN
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final EventHandler handler = mock();
        final SingleDateDetector detector = new SingleDateDetector(handler, date);
        final double shift = -1;
        final AbsoluteDate otherDate = date.shiftedBy(shift);
        // WHEN
        final SingleDateDetector newDetector = detector.shiftedBy(shift);
        // THEN
        assertEquals(otherDate, newDetector.getDate());
        assertEquals(handler, newDetector.getHandler());
        assertEquals(detector.getDetectionSettings(), newDetector.getDetectionSettings());
    }
}
