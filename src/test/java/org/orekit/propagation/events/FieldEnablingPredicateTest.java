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
import org.junit.jupiter.api.Test;
import org.orekit.propagation.FieldSpacecraftState;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class FieldEnablingPredicateTest {

    @Test
    @SuppressWarnings("unchecked")
    void testOrCombine() {
        // GIVEN
        final FieldSpacecraftState<Binary64> mockedState = mock();
        final FieldEventDetector<Binary64> mockedDetector = mock();
        final FieldEnablingPredicate<Binary64> truePredicate = ((state, detector, g) -> true);
        final FieldEnablingPredicate<Binary64> falsePredicate = ((state, detector, g) -> false);
        // WHEN
        final FieldEnablingPredicate<Binary64> combined = FieldEnablingPredicate.orCombine(truePredicate, falsePredicate);
        // THEN
        final boolean actual = combined.eventIsEnabled(mockedState, mockedDetector, Binary64.ZERO);
        assertTrue(actual);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testAndCombine() {
        // GIVEN
        final FieldSpacecraftState<Binary64> mockedState = mock();
        final FieldEventDetector<Binary64> mockedDetector = mock();
        final FieldEnablingPredicate<Binary64> truePredicate = ((state, detector, g) -> true);
        final FieldEnablingPredicate<Binary64> falsePredicate = ((state, detector, g) -> false);
        // WHEN
        final FieldEnablingPredicate<Binary64> combined = FieldEnablingPredicate.andCombine(truePredicate, falsePredicate);
        // THEN
        final boolean actual = combined.eventIsEnabled(mockedState, mockedDetector, Binary64.ZERO);
        assertFalse(actual);
    }
}