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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.time.AbsoluteDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class EventHandlerModifierTest {

    @Test
    void testInit() {
        // GIVEN
        final TestHandler handler = new TestHandler(Action.CONTINUE);
        final EventHandlerModifier modifier = getModifier(handler);
        // WHEN
        modifier.init(mock(), mock(), mock());
        // THEN
        assertTrue(handler.isInitialized);
    }

    @ParameterizedTest
    @EnumSource(Action.class)
    void testEventOccurred(final Action expectedAction) {
        // GIVEN
        final TestHandler handler = new TestHandler(expectedAction);
        final EventHandlerModifier modifier = getModifier(handler);
        // WHEN
        final Action actualAction = modifier.eventOccurred(mock(), mock(), true);
        // THEN
        assertEquals(expectedAction, actualAction);
    }

    @Test
    void testResetState() {
        // GIVEN
        final TestHandler handler = new TestHandler(Action.RESET_STATE);
        final EventHandlerModifier modifier = getModifier(handler);
        final SpacecraftState state = mock();
        // WHEN
        final SpacecraftState resetState = modifier.resetState(mock(), state);
        // THEN
        assertEquals(state, resetState);
    }

    @Test
    void testFinish() {
        // GIVEN
        final TestHandler handler = new TestHandler(Action.STOP);
        final EventHandlerModifier modifier = getModifier(handler);
        // WHEN
        modifier.finish(mock(), mock());
        // THEN
        assertTrue(handler.isFinished);
    }

    EventHandlerModifier getModifier(final TestHandler handler) {
        return () -> handler;
    }

    private static class TestHandler implements EventHandler {

        private final Action action;
        boolean isInitialized = false;
        boolean isFinished = false;

        TestHandler(final Action action) {
            this.action = action;
        }

        @Override
        public void init(SpacecraftState initialState, AbsoluteDate target, EventDetector detector) {
            EventHandler.super.init(initialState, target, detector);
            isInitialized = true;
        }

        @Override
        public Action eventOccurred(SpacecraftState s, EventDetector detector, boolean increasing) {
            return action;
        }

        @Override
        public void finish(SpacecraftState finalState, EventDetector detector) {
            EventHandler.super.finish(finalState, detector);
            isFinished = true;
        }
    }
}
