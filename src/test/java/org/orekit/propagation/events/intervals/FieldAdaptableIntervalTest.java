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
package org.orekit.propagation.events.intervals;

import org.hipparchus.util.Binary64;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.orekit.propagation.FieldSpacecraftState;

class FieldAdaptableIntervalTest {

    @SuppressWarnings("unchecked")
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testOfDouble(final boolean isForward) {
        // GIVEN
        final double expectedValue = 1.;
        // WHEN
        final FieldAdaptableInterval<?> adaptableInterval = FieldAdaptableInterval.of(expectedValue);
        // THEN
        final double actualValue = adaptableInterval.currentInterval(Mockito.mock(FieldSpacecraftState.class), isForward);
        Assertions.assertEquals(expectedValue, actualValue);
    }

    @SuppressWarnings("unchecked")
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testOf(final boolean isForward) {
        // GIVEN
        final double expectedValue = 1.;
        final AdaptableInterval adaptableInterval = AdaptableInterval.of(expectedValue);
        // WHEN
        final FieldAdaptableInterval<?> fieldAdaptableInterval = FieldAdaptableInterval.of(adaptableInterval);
        // THEN
        final double actualValue = fieldAdaptableInterval.currentInterval(Mockito.mock(FieldSpacecraftState.class), isForward);
        Assertions.assertEquals(expectedValue, actualValue);
    }

    @ParameterizedTest
    @SuppressWarnings("unchecked")
    @ValueSource(booleans = {true, false})
    void testAdaptableIntervalOf(final boolean isForward) {
        // GIVEN
        final double expectedValue = 1;
        final FieldAdaptableInterval<Binary64> interval1 = FieldAdaptableInterval.of(expectedValue);
        final FieldAdaptableInterval<Binary64> interval2 = FieldAdaptableInterval.of(expectedValue + 1);
        // WHEN
        final FieldAdaptableInterval<Binary64> adaptableInterval = FieldAdaptableInterval.of(Double.POSITIVE_INFINITY, interval1, interval2);
        // THEN
        final double actualValue = adaptableInterval.currentInterval(Mockito.mock(FieldSpacecraftState.class), isForward);
        Assertions.assertEquals(expectedValue, actualValue);
    }
}

