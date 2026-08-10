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

import org.hipparchus.complex.Complex;
import org.hipparchus.complex.ComplexField;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Test;
import org.orekit.TestUtils;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EventFunctionTest {

    @Test
    void testOf() {
        // GIVEN
        final Binary64 expected = Binary64.ONE;
        final Function<FieldSpacecraftState<Binary64>, Binary64> function = state -> expected;
        // WHEN
        final EventFunction eventFunction = EventFunction.of(Binary64Field.getInstance(), function);
        // THEN
        final SpacecraftState state = new SpacecraftState(TestUtils.getDefaultOrbit(AbsoluteDate.ARBITRARY_EPOCH));
        assertEquals(1., eventFunction.value(state));
        assertEquals(expected, eventFunction.value(new FieldSpacecraftState<>(Binary64Field.getInstance(), state)));
        assertEquals(Complex.ONE, eventFunction.value(new FieldSpacecraftState<>(ComplexField.getInstance(), state)));
        assertFalse(eventFunction.dependsOnTimeOnly());
        assertTrue(eventFunction.dependsOnMainVariablesOnly());
    }

    @Test
    void testDependsOnMainVariablesOnly() {
        // GIVEN
        final EventFunction eventFunction = state -> 0.;
        // WHEN
        final boolean actual = eventFunction.dependsOnMainVariablesOnly();
        // THEN
        assertTrue(actual);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testValueField() {
        // GIVEN
        final double expectedValue = -1.;
        final EventFunction eventFunction = state -> expectedValue;
        final FieldSpacecraftState<Binary64> mockedState = mock();
        when(mockedState.getDate()).thenReturn(FieldAbsoluteDate.getArbitraryEpoch(Binary64Field.getInstance()));
        // WHEN
        final Binary64 actual = eventFunction.value(mockedState);
        // THEN
        assertEquals(expectedValue, actual.getReal());
    }
}
