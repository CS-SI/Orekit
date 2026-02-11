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

import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Test;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.handlers.ContinueOnEvent;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.propagation.events.handlers.FieldStopOnEvent;
import org.orekit.time.AbsoluteDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FieldSingleDateDetectorTest {

    @Test
    void testToEventDetector() {
        // GIVEN
        final FieldSingleDateDetector<Binary64> fieldDetector = new FieldSingleDateDetector<>(Binary64Field.getInstance(),
                AbsoluteDate.ARBITRARY_EPOCH);
        final EventHandler expectedHandler = new ContinueOnEvent();
        // WHEN
        final SingleDateDetector detector = fieldDetector.toEventDetector(expectedHandler);
        // THEN
        assertEquals(expectedHandler, detector.getHandler());
        assertEquals(fieldDetector.getDate(), detector.getDate());
    }

    @Test
    void testConstructor() {
        // GIVEN
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        // WHEN
        final FieldSingleDateDetector<Binary64> detector = new FieldSingleDateDetector<>(Binary64Field.getInstance(), date);
        // THEN
        assertEquals(date, detector.getDate());
        assertInstanceOf(FieldStopOnEvent.class, detector.getHandler());
        assertEquals(DateDetector.DEFAULT_THRESHOLD, detector.getThreshold().getReal());
        assertEquals(DateDetector.DEFAULT_MAX_ITER, detector.getMaxIterationCount());
    }

    @Test
    void testDependsOnTimeOnly() {
        // GIVEN
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        // WHEN
        final FieldSingleDateDetector<Binary64> detector = new FieldSingleDateDetector<>(Binary64Field.getInstance(), date);
        // THEN
        assertTrue(detector.getEventFunction().dependsOnTimeOnly());
    }

    @Test
    void testG() {
        // GIVEN
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final FieldSpacecraftState<Binary64> mockedState = mock();
        final Binary64 expectedG = Binary64.ONE;
        when(mockedState.durationFrom(date)).thenReturn(expectedG);
        // WHEN
        final FieldSingleDateDetector<Binary64> detector = new FieldSingleDateDetector<>(Binary64Field.getInstance(), date);
        // THEN
        assertEquals(expectedG, detector.g(mockedState));
    }

    @Test
    void testCreate() {
        // GIVEN
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final FieldSingleDateDetector<Binary64> detector = new FieldSingleDateDetector<>(Binary64Field.getInstance(), date);
        final FieldEventDetectionSettings<Binary64> detectionSettings = new FieldEventDetectionSettings<>(Binary64Field.getInstance(),
                EventDetectionSettings.getDefaultEventDetectionSettings());
        final FieldEventHandler<Binary64> handler = mock();
        // WHEN
        final FieldSingleDateDetector<?> newDetector = detector.create(detectionSettings, handler);
        // THEN
        assertEquals(date, newDetector.getDate());
        assertEquals(handler, newDetector.getHandler());
        assertEquals(detectionSettings, newDetector.getDetectionSettings());
    }

    @Test
    void testShiftedBy() {
        // GIVEN
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final FieldSingleDateDetector<Binary64> detector = new FieldSingleDateDetector<>(Binary64Field.getInstance(), date);
        final double shift = 2;
        final AbsoluteDate otherDate = date.shiftedBy(shift);
        // WHEN
        final FieldSingleDateDetector<Binary64> newDetector = detector.shiftedBy(otherDate);
        // THEN
        assertEquals(otherDate, newDetector.getDate());
        assertEquals(detector.getHandler(), newDetector.getHandler());
        assertEquals(detector.getDetectionSettings(), newDetector.getDetectionSettings());
    }
}
