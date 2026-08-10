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
package org.orekit.propagation.events.intervals;

import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PeriodBasedAdaptableIntervalTest {

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testCurrentIntervalNotOrbit(final boolean isForward) {
        // GIVEN
        final SpacecraftState mockedState = mock();
        when(mockedState.isOrbitDefined()).thenReturn(false);
        final double expected = 10.;
        final PeriodBasedAdaptableInterval interval = new PeriodBasedAdaptableInterval(1., expected);
        // WHEN
        final double actual = interval.currentInterval(mockedState, isForward);
        // THEN
        assertEquals(expected, actual);
    }

    @Test
    void testCurrentIntervalNotElliptical() {
        // GIVEN
        final SpacecraftState mockedState = mock();
        when(mockedState.isOrbitDefined()).thenReturn(true);
        final Orbit mockedOrbit = mock();
        when(mockedOrbit.isElliptical()).thenReturn(false);
        when(mockedState.getOrbit()).thenReturn(mockedOrbit);
        final double expected = 10.;
        final PeriodBasedAdaptableInterval interval = new PeriodBasedAdaptableInterval(2., expected);
        // WHEN
        final double actual = interval.currentInterval(mockedState, false);
        // THEN
        assertEquals(expected, actual);
    }

    @ParameterizedTest
    @ValueSource(doubles = {1., 10.})
    void testCurrentIntervalElliptical(final double defaultMaxCheck) {
        // GIVEN
        final SpacecraftState mockedState = mock();
        when(mockedState.isOrbitDefined()).thenReturn(true);
        final Orbit mockedOrbit = mock();
        when(mockedOrbit.isElliptical()).thenReturn(true);
        final double period = 3.;
        when(mockedOrbit.getKeplerianPeriod()).thenReturn(period);
        when(mockedState.getOrbit()).thenReturn(mockedOrbit);
        final double factor = 2;
        final double expected = FastMath.min(defaultMaxCheck, period * factor);
        final PeriodBasedAdaptableInterval interval = new PeriodBasedAdaptableInterval(factor, defaultMaxCheck);
        // WHEN
        final double actual = interval.currentInterval(mockedState, true);
        // THEN
        assertEquals(expected, actual);
    }
}
