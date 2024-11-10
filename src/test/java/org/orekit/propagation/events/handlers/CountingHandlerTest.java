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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;

class CountingHandlerTest {

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testEventOccurred(final boolean countAll) {
        // GIVEN
        final CountingHandler handler = new TestHandler(countAll, Action.CONTINUE);
        final SpacecraftState mockedState = Mockito.mock(SpacecraftState.class);
        final EventDetector mockedDetector = Mockito.mock(EventDetector.class);
        final int calls = 42;
        // WHEN
        for (int i = 0; i < calls; i++) {
            handler.eventOccurred(mockedState, mockedDetector, true);
        }
        // THEN
        Assertions.assertEquals(countAll ? calls : 0, handler.getCount());
    }

    @ParameterizedTest
    @EnumSource(Action.class)
    void testEventOccurred(final Action expectedAction) {
        // GIVEN
        final CountingHandler handler = new TestHandler(true, expectedAction);
        final SpacecraftState mockedState = Mockito.mock(SpacecraftState.class);
        final EventDetector mockedDetector = Mockito.mock(EventDetector.class);
        // WHEN
        final Action actualAction = handler.eventOccurred(mockedState, mockedDetector, true);
        // THEN
        Assertions.assertEquals(expectedAction, actualAction);
    }

    private static class TestHandler extends CountingHandler {

        private final boolean countAll;

        TestHandler(final boolean countAll, final Action action) {
            super(0, action);
            this.countAll = countAll;
        }

        @Override
        protected boolean doesCount(SpacecraftState state, EventDetector detector, boolean increasing) {
            return countAll;
        }
    }

}
