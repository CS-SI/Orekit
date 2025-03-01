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

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.ContinueOnEvent;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;

import static org.junit.jupiter.api.Assertions.*;

class BooleanDetectorTest {

    @Test
    void testInit() {
        // GIVEN
        final TestDetector detector = new TestDetector();
        final BooleanDetector booleanDetector = BooleanDetector.andCombine(detector);
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final SpacecraftState mockedState = Mockito.mock(SpacecraftState.class);
        Mockito.when(mockedState.getDate()).thenReturn(date);
        // WHEN
        booleanDetector.init(mockedState, date);
        // THEN
        assertFalse(detector.finished);
        assertFalse(detector.resetted);
        assertTrue(detector.initialized);
    }

    @Test
    void testReset() {
        // GIVEN
        final TestDetector detector = new TestDetector();
        final BooleanDetector booleanDetector = BooleanDetector.orCombine(detector);
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final SpacecraftState mockedState = Mockito.mock(SpacecraftState.class);
        Mockito.when(mockedState.getDate()).thenReturn(date);
        // WHEN
        booleanDetector.reset(mockedState, date);
        // THEN
        assertTrue(detector.resetted);
        assertFalse(detector.finished);
        assertFalse(detector.initialized);
    }

    @Test
    void testFinish() {
        // GIVEN
        final TestDetector detector = new TestDetector();
        final BooleanDetector booleanDetector = BooleanDetector.orCombine(detector);
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final SpacecraftState mockedState = Mockito.mock(SpacecraftState.class);
        Mockito.when(mockedState.getDate()).thenReturn(date);
        // WHEN
        booleanDetector.finish(mockedState);
        // THEN
        assertTrue(detector.finished);
        assertFalse(detector.resetted);
        assertFalse(detector.initialized);
    }

    private static class TestDetector implements EventDetector {

        boolean initialized = false;
        boolean resetted = false;
        boolean finished = false;

        @Override
        public double g(SpacecraftState s) {
            return 1;
        }

        @Override
        public void init(SpacecraftState s0, AbsoluteDate t) {
            EventDetector.super.init(s0, t);
            initialized = true;
        }

        @Override
        public void reset(SpacecraftState state, AbsoluteDate target) {
            EventDetector.super.reset(state, target);
            resetted = true;
        }

        @Override
        public void finish(SpacecraftState state) {
            EventDetector.super.finish(state);
            finished = true;
        }

        @Override
        public EventHandler getHandler() {
            return new ContinueOnEvent();
        }
    }
}
