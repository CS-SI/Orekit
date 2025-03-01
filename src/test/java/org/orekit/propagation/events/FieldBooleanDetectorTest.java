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
import org.mockito.Mockito;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.handlers.FieldContinueOnEvent;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.time.FieldAbsoluteDate;

import static org.junit.jupiter.api.Assertions.*;

class FieldBooleanDetectorTest {

    @Test
    @SuppressWarnings("unchecked")
    void testInit() {
        final TestDetector detector = new TestDetector();
        final FieldBooleanDetector<Binary64> booleanDetector = FieldBooleanDetector.orCombine(detector);
        final FieldAbsoluteDate<Binary64> date = FieldAbsoluteDate.getArbitraryEpoch(Binary64Field.getInstance());
        final FieldSpacecraftState<Binary64> mockedState = Mockito.mock(FieldSpacecraftState.class);
        Mockito.when(mockedState.getDate()).thenReturn(date);
        // WHEN
        booleanDetector.init(mockedState, date);
        // THEN
        assertTrue(detector.initialized);
        assertFalse(detector.finished);
        assertFalse(detector.resetted);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testReset() {
        final TestDetector detector = new TestDetector();
        final FieldBooleanDetector<Binary64> booleanDetector = FieldBooleanDetector.orCombine(detector);
        final FieldAbsoluteDate<Binary64> date = FieldAbsoluteDate.getArbitraryEpoch(Binary64Field.getInstance());
        final FieldSpacecraftState<Binary64> mockedState = Mockito.mock(FieldSpacecraftState.class);
        Mockito.when(mockedState.getDate()).thenReturn(date);
        // WHEN
        booleanDetector.reset(mockedState, date);
        // THEN
        assertTrue(detector.resetted);
        assertFalse(detector.finished);
        assertFalse(detector.initialized);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testFinish() {
        final TestDetector detector = new TestDetector();
        final FieldBooleanDetector<Binary64> booleanDetector = FieldBooleanDetector.orCombine(detector);
        final FieldAbsoluteDate<Binary64> date = FieldAbsoluteDate.getArbitraryEpoch(Binary64Field.getInstance());
        final FieldSpacecraftState<Binary64> mockedState = Mockito.mock(FieldSpacecraftState.class);
        Mockito.when(mockedState.getDate()).thenReturn(date);
        // WHEN
        booleanDetector.finish(mockedState);
        // THEN
        assertTrue(detector.finished);
        assertFalse(detector.initialized);
        assertFalse(detector.resetted);
    }

    private static class TestDetector implements FieldEventDetector<Binary64> {
        boolean initialized = false;
        boolean resetted = false;
        boolean finished = false;

        @Override
        public void init(FieldSpacecraftState<Binary64> s0, FieldAbsoluteDate<Binary64> t) {
            FieldEventDetector.super.init(s0, t);
            initialized = true;
        }

        @Override
        public void reset(FieldSpacecraftState<Binary64> state, FieldAbsoluteDate<Binary64> target) {
            FieldEventDetector.super.reset(state, target);
            resetted = true;
        }

        @Override
        public void finish(FieldSpacecraftState<Binary64> state) {
            FieldEventDetector.super.finish(state);
            finished = true;
        }

        @Override
        public Binary64 g(FieldSpacecraftState<Binary64> s) {
            return Binary64.ONE;
        }

        @Override
        public FieldEventHandler<Binary64> getHandler() {
            return new FieldContinueOnEvent<>();
        }

        @Override
        public FieldEventDetectionSettings<Binary64> getDetectionSettings() {
            return new FieldEventDetectionSettings<>(Binary64Field.getInstance(),
                    EventDetectionSettings.getDefaultEventDetectionSettings());
        }
    }
}
