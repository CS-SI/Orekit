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
package org.orekit.propagation.events.intervals;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.orekit.propagation.SpacecraftState;

class AdaptableIntervalTest {

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testOf(final boolean isForward) {
        // GIVEN
        final double expectedValue = 1.;
        // WHEN
        final AdaptableInterval adaptableInterval = AdaptableInterval.of(expectedValue);
        // THEN
        final double actualValue = adaptableInterval.currentInterval(Mockito.mock(SpacecraftState.class), isForward);
        Assertions.assertEquals(expectedValue, actualValue);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testAdaptableIntervalOf(final boolean isForward) {
        // GIVEN
        final double expectedValue = 1;
        final AdaptableInterval interval1 = AdaptableInterval.of(expectedValue);
        final AdaptableInterval interval2 = AdaptableInterval.of(expectedValue + 1);
        // WHEN
        final AdaptableInterval adaptableInterval = AdaptableInterval.of(Double.POSITIVE_INFINITY, interval1, interval2);
        // THEN
        final double actualValue = adaptableInterval.currentInterval(Mockito.mock(SpacecraftState.class), isForward);
        Assertions.assertEquals(expectedValue, actualValue);
    }

}