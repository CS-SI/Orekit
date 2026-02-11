/* Copyright 2022-2026 Romain Serra.
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
import org.hipparchus.util.Binary64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.time.FieldAbsoluteDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class FieldEventHandlerModifierTest {

    @Test
    void testInit() {
        // GIVEN
        final TestFieldHandler handler = new TestFieldHandler(Action.CONTINUE);
        final FieldEventHandlerModifier<Binary64> modifier = () -> handler;
        // WHEN
        modifier.init(null, null, null);
        // THEN
        assertTrue(handler.isInitialized);
    }

    @ParameterizedTest
    @EnumSource(Action.class)
    void testEventOccurred(final Action expectedAction) {
        // GIVEN
        final TestFieldHandler handler = new TestFieldHandler(expectedAction);
        final FieldEventHandlerModifier<Binary64> modifier = () -> handler;
        // WHEN
        final Action actualAction = modifier.eventOccurred(null, null, true);
        // THEN
        assertEquals(expectedAction, actualAction);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testResetState() {
        // GIVEN
        final TestFieldHandler handler = new TestFieldHandler(Action.RESET_STATE);
        final FieldEventHandlerModifier<Binary64> modifier = () -> handler;
        final FieldSpacecraftState<Binary64> state = mock();
        // WHEN
        final FieldSpacecraftState<Binary64> resetState = modifier.resetState(mock(), state);
        // THEN
        assertEquals(state, resetState);
    }

    @Test
    void testFinish() {
        // GIVEN
        final TestFieldHandler handler = new TestFieldHandler(Action.STOP);
        final FieldEventHandlerModifier<Binary64> modifier = () -> handler;
        // WHEN
        modifier.finish(null, null);
        // THEN
        assertTrue(handler.isFinished);
    }

    private static class TestFieldHandler implements FieldEventHandler<Binary64> {

        private final Action action;
        boolean isInitialized = false;
        boolean isFinished = false;

        TestFieldHandler(final Action action) {
            this.action = action;
        }

        @Override
        public void init(FieldSpacecraftState<Binary64> initialState, FieldAbsoluteDate<Binary64> target, FieldEventDetector<Binary64> detector) {
            FieldEventHandler.super.init(initialState, target, detector);
            isInitialized = true;
        }

        @Override
        public Action eventOccurred(FieldSpacecraftState<Binary64> s, FieldEventDetector<Binary64> detector, boolean increasing) {
            return action;
        }

        @Override
        public void finish(FieldSpacecraftState<Binary64> finalState, FieldEventDetector<Binary64> detector) {
            FieldEventHandler.super.finish(finalState, detector);
            isFinished = true;
        }
    }
}
