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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class EnablingPredicateTest {

    @Test
    void TestDependsOnMainVariablesOnly() {
        // GIVEN
        final EnablingPredicate truePredicate = ((state, detector, g) -> true);
        // WHEN & THEN
        assertFalse(truePredicate.dependsOnMainVariablesOnly());
    }

    @Test
    void testOrCombine() {
        // GIVEN
        final SpacecraftState mockedState = mock();
        final EventDetector mockedDetector = mock();
        final EnablingPredicate truePredicate = ((state, detector, g) -> true);
        final EnablingPredicate falsePredicate = ((state, detector, g) -> false);
        // WHEN
        final EnablingPredicate combined = EnablingPredicate.orCombine(truePredicate, falsePredicate);
        // THEN
        final boolean actual = combined.eventIsEnabled(mockedState, mockedDetector, 0.);
        assertTrue(actual);
    }

    @Test
    void testAndCombine() {
        // GIVEN
        final SpacecraftState mockedState = mock();
        final EventDetector mockedDetector = mock();
        // WHEN
        final EnablingPredicate combined = EnablingPredicate.andCombine(new AlwaysTruePredicate(), new AlwaysFalsePredicate());
        // THEN
        final boolean actual = combined.eventIsEnabled(mockedState, mockedDetector, 0.);
        assertFalse(actual);
        assertTrue(combined.dependsOnMainVariablesOnly());
    }

    private static class AlwaysTruePredicate implements EnablingPredicate {
        @Override
        public boolean eventIsEnabled(SpacecraftState state, EventDetector detector, double g) {
            return true;
        }

        @Override
        public boolean dependsOnMainVariablesOnly() {
            return true;
        }
    }

    private static class AlwaysFalsePredicate implements EnablingPredicate {
        @Override
        public boolean eventIsEnabled(SpacecraftState state, EventDetector detector, double g) {
            return false;
        }

        @Override
        public boolean dependsOnMainVariablesOnly() {
            return true;
        }
    }
}
