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

import org.hipparchus.complex.Complex;
import org.hipparchus.complex.ComplexField;
import org.hipparchus.ode.events.Action;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;

class FieldRecallLastOccurrenceTest {

    private static final Action ACTION = Action.CONTINUE;

    @Test
    void testEventOccurred() {
        // GIVEN
        final TestHandler testHandler = new TestHandler();
        final FieldRecallLastOccurrence<Complex> recallLastOccurrence = new FieldRecallLastOccurrence<>(testHandler);
        final FieldAbsoluteDate<Complex> expectedDate = FieldAbsoluteDate.getArbitraryEpoch(ComplexField.getInstance());
        final FieldSpacecraftState<Complex> mockedState = mockState(expectedDate);
        // WHEN
        final Action action = recallLastOccurrence.eventOccurred(mockedState, null, true);
        // THEN
        Assertions.assertEquals(expectedDate, recallLastOccurrence.getLastOccurrence());
        Assertions.assertEquals(ACTION, action);
    }

    @Test
    void testFinish() {
        // GIVEN
        final TestHandler testHandler = new TestHandler();
        final FieldRecallLastOccurrence<Complex> recallLastOccurrence = new FieldRecallLastOccurrence<>(testHandler);
        final FieldAbsoluteDate<Complex> expectedDate = FieldAbsoluteDate.getArbitraryEpoch(ComplexField.getInstance());
        final FieldSpacecraftState<Complex> mockedState = mockState(expectedDate);
        // WHEN
        recallLastOccurrence.finish(mockedState, null);
        // THEN
        Assertions.assertTrue(testHandler.isFinished);
    }

    @Test
    void testResetState() {
        // GIVEN
        final TestHandler testHandler = new TestHandler();
        final FieldRecallLastOccurrence<Complex> recallLastOccurrence = new FieldRecallLastOccurrence<>(testHandler);
        final FieldSpacecraftState<Complex> mockedState = mockState(FieldAbsoluteDate.getArbitraryEpoch(ComplexField.getInstance()));
        // WHEN
        final FieldSpacecraftState<Complex> actualState = recallLastOccurrence.resetState(null, mockedState);
        // THEN
        Assertions.assertEquals(mockedState, actualState);
        Assertions.assertNull(recallLastOccurrence.getLastOccurrence());
    }

    @Test
    void testInit() {
        // GIVEN
        final TestHandler testHandler = new TestHandler();
        final FieldRecallLastOccurrence<Complex> recallLastOccurrence = new FieldRecallLastOccurrence<>(testHandler);
        final FieldSpacecraftState<Complex> mockedState = mockState(new FieldAbsoluteDate<>(ComplexField.getInstance(),
            AbsoluteDate.FUTURE_INFINITY));
        // WHEN
        recallLastOccurrence.init(mockedState, FieldAbsoluteDate.getArbitraryEpoch(ComplexField.getInstance()), null);
        // THEN
        Assertions.assertTrue(testHandler.isInitialized);
    }

    @SuppressWarnings("unchecked")
    private FieldSpacecraftState<Complex> mockState(final FieldAbsoluteDate<Complex> date) {
        final FieldSpacecraftState<Complex> mockedState = Mockito.mock(FieldSpacecraftState.class);
        Mockito.when(mockedState.getDate()).thenReturn(date);
        return mockedState;
    }

    private static class TestHandler implements FieldEventHandler<Complex> {

        boolean isInitialized = false;
        boolean isFinished = false;

        @Override
        public void init(FieldSpacecraftState<Complex> initialState, FieldAbsoluteDate<Complex> target, FieldEventDetector<Complex> detector) {
            isInitialized = true;
        }

        @Override
        public Action eventOccurred(FieldSpacecraftState<Complex> s, FieldEventDetector<Complex> detector, boolean increasing) {
            return ACTION;
        }

        @Override
        public void finish(FieldSpacecraftState<Complex> finalState, FieldEventDetector<Complex> detector) {
            isFinished = true;
        }
    }

}
