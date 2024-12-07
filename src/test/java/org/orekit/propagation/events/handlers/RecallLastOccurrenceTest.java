/* Copyright 2022-2024 Romain Serra
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
package org.orekit.propagation.events.handlers;

import org.hipparchus.ode.events.Action;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.time.AbsoluteDate;

class RecallLastOccurrenceTest {

    private static final Action ACTION = Action.CONTINUE;

    @Test
    void testFinish() {
        // GIVEN
        final EventHandler mockedHandler = Mockito.mock(EventHandler.class);
        final RecallLastOccurrence wrappingHandler = new RecallLastOccurrence(mockedHandler);
        final SpacecraftState mockedState = Mockito.mock(SpacecraftState.class);
        // WHEN
        wrappingHandler.finish(mockedState, null);
        // THEN
        Mockito.verify(mockedHandler, Mockito.times(1)).finish(mockedState, null);
    }

    @Test
    void testEventOccurred() {
        // GIVEN
        final TestHandler testHandler = new TestHandler();
        final RecallLastOccurrence recallLastOccurrence = new RecallLastOccurrence(testHandler);
        final AbsoluteDate expectedDate = AbsoluteDate.J2000_EPOCH;
        final SpacecraftState mockedState = mockState(expectedDate);
        // WHEN
        final Action action = recallLastOccurrence.eventOccurred(mockedState, null, true);
        // THEN
        Assertions.assertEquals(expectedDate, recallLastOccurrence.getLastOccurrence());
        Assertions.assertEquals(ACTION, action);
    }

    @Test
    void testResetState() {
        // GIVEN
        final TestHandler testHandler = new TestHandler();
        final RecallLastOccurrence recallLastOccurrence = new RecallLastOccurrence(testHandler);
        final SpacecraftState mockedState = mockState(AbsoluteDate.ARBITRARY_EPOCH);
        // WHEN
        final SpacecraftState actualState = recallLastOccurrence.resetState(null, mockedState);
        // THEN
        Assertions.assertEquals(mockedState, actualState);
        Assertions.assertNull(recallLastOccurrence.getLastOccurrence());
    }

    @Test
    void testInitBackward() {
        // GIVEN
        final TestHandler testHandler = new TestHandler();
        final RecallLastOccurrence recallLastOccurrence = new RecallLastOccurrence(testHandler);
        final SpacecraftState mockedState = mockState(AbsoluteDate.FUTURE_INFINITY);
        // WHEN
        recallLastOccurrence.init(mockedState, AbsoluteDate.ARBITRARY_EPOCH, null);
        // THEN
        Assertions.assertTrue(testHandler.isInitialized);
        Assertions.assertEquals(AbsoluteDate.FUTURE_INFINITY, recallLastOccurrence.getLastOccurrence());
    }

    @Test
    void testInitForward() {
        // GIVEN
        final TestHandler testHandler = new TestHandler();
        final RecallLastOccurrence recallLastOccurrence = new RecallLastOccurrence(testHandler);
        final SpacecraftState mockedState = mockState(AbsoluteDate.PAST_INFINITY);
        // WHEN
        recallLastOccurrence.init(mockedState, AbsoluteDate.ARBITRARY_EPOCH, null);
        // THEN
        Assertions.assertTrue(testHandler.isInitialized);
        Assertions.assertEquals(AbsoluteDate.PAST_INFINITY, recallLastOccurrence.getLastOccurrence());
    }

    private SpacecraftState mockState(final AbsoluteDate date) {
        final SpacecraftState mockedState = Mockito.mock(SpacecraftState.class);
        Mockito.when(mockedState.getDate()).thenReturn(date);
        return mockedState;
    }

    private static class TestHandler implements EventHandler {

        boolean isInitialized = false;

        boolean isFinished = false;

        @Override
        public void init(SpacecraftState initialState, AbsoluteDate target, EventDetector detector) {
            isInitialized = true;
        }

        @Override
        public Action eventOccurred(SpacecraftState s, EventDetector detector, boolean increasing) {
            return ACTION;
        }

        @Override
        public void finish(SpacecraftState finalState, EventDetector detector) {
            isFinished = true;
        }
    }

}
