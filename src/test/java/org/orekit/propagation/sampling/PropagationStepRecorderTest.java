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
package org.orekit.propagation.sampling;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PropagationStepRecorderTest {

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testSetter(final boolean resetAutomatically) {
        // GIVEN
        final PropagationStepRecorder recorder = new PropagationStepRecorder();
        // WHEN
        recorder.setResetAutomatically(resetAutomatically);
        // THEN
        assertEquals(resetAutomatically, recorder.isResetAutomatically());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testGetter(final boolean resetAutomatically) {
        // GIVEN

        // WHEN
        final PropagationStepRecorder recorder = new PropagationStepRecorder(resetAutomatically);
        // THEN
        assertEquals(resetAutomatically, recorder.isResetAutomatically());
    }

    @Test
    void copyStatesAtConstructionTest() {
        // GIVEN
        final PropagationStepRecorder recorder = new PropagationStepRecorder();
        // WHEN
        final List<SpacecraftState> states = recorder.copyStates();
        // THEN
        assertEquals(0, states.size());
    }

    @Test
    void copyStatesTest() {
        // GIVEN
        final PropagationStepRecorder recorder = new PropagationStepRecorder();
        recorder.handleStep(mockInterpolator());
        // WHEN
        recorder.init(mockState(), AbsoluteDate.ARBITRARY_EPOCH);
        // THEN
        final List<SpacecraftState> states = recorder.copyStates();
        assertEquals(0, states.size());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void handleStepTest(final boolean resetAutomatically) {
        // GIVEN
        final PropagationStepRecorder recorder = new PropagationStepRecorder(resetAutomatically);
        final OrekitStepInterpolator mockedInterpolator = mockInterpolator();
        final int expectedSize = 10;
        // WHEN
        for (int i = 0; i < expectedSize; ++i) {
            recorder.handleStep(mockedInterpolator);
        }
        // WHEN
        final List<SpacecraftState> states = recorder.copyStates();
        assertEquals(expectedSize, states.size());
    }

    private static OrekitStepInterpolator mockInterpolator() {
        final SpacecraftState state = mockState();
        final OrekitStepInterpolator mockedInterpolator = Mockito.mock();
        Mockito.when(mockedInterpolator.getCurrentState()).thenReturn(state);
        return mockedInterpolator;
    }

    private static SpacecraftState mockState() {
        return Mockito.mock(SpacecraftState.class);
    }
}
