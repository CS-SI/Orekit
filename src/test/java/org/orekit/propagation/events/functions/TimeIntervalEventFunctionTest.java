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
package org.orekit.propagation.events.functions;

import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Test;
import org.orekit.TestUtils;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeInterval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TimeIntervalEventFunctionTest {

    @Test
    void testValue() {
        // GIVEN
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final TimeInterval timeInterval = TimeInterval.of(date, date.shiftedBy(1));
        final SpacecraftState state = mock();
        when(state.getDate()).thenReturn(date);
        final TimeIntervalEventFunction eventFunction = new TimeIntervalEventFunction(timeInterval);
        // WHEN
        final double value = eventFunction.value(state);
        // THEN
        assertEquals(0., value, 1e-15);
    }

    @Test
    void testValueField() {
        // GIVEN
        final Binary64Field field = Binary64Field.getInstance();
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final Orbit orbit = TestUtils.getDefaultOrbit(date.shiftedBy(-1.));
        final SpacecraftState state = new SpacecraftState(orbit);
        final FieldSpacecraftState<Binary64> fieldState = new FieldSpacecraftState<>(field, state);
        final TimeInterval timeInterval = TimeInterval.of(date, date.shiftedBy(1));
        final TimeIntervalEventFunction eventFunction = new TimeIntervalEventFunction(timeInterval);
        // WHEN
        final Binary64 value = eventFunction.value(fieldState);
        // THEN
        assertEquals(eventFunction.value(state), value.getReal());
    }

    @Test
    void testDependsOnTimeOnly() {
        // GIVEN
        final TimeIntervalEventFunction eventFunction = new TimeIntervalEventFunction(mock(TimeInterval.class));
        // WHEN
        final boolean actual = eventFunction.dependsOnTimeOnly();
        // THEN
        assertTrue(actual);
    }
}
