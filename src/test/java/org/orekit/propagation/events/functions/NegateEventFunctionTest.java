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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Test;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NegateEventFunctionTest {

    @Test
    void testDepends() {
        // GIVEN
        final NegateEventFunction<SingleDateEventFunction> negate = new NegateEventFunction<>(new SingleDateEventFunction(AbsoluteDate.ARBITRARY_EPOCH));
        // WHEN
        final boolean actual = negate.dependsOnTimeOnly();
        // THEN
        assertTrue(actual);
    }

    @Test
    void testValue() {
        // GIVEN
        final double expected = -1.;
        final EventFunction function = state -> -expected;
        final NegateEventFunction<EventFunction> negate = new NegateEventFunction<>(function);
        // WHEN
        final double actual = negate.value(mock(SpacecraftState.class));
        // THEN
        assertEquals(expected, actual);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testValueField() {
        // GIVEN
        final EventFunction function = new EventFunction() {
            @Override
            public double value(SpacecraftState state) {
                return 1.;
            }

            @Override
            public <T extends CalculusFieldElement<T>> T value(FieldSpacecraftState<T> fieldState) {
                return fieldState.getDate().getField().getOne();
            }
        };
        final NegateEventFunction<EventFunction> negate = new NegateEventFunction<>(function);
        final FieldSpacecraftState<Binary64> mockState = mock();
        when(mockState.getDate()).thenReturn(FieldAbsoluteDate.getArbitraryEpoch(Binary64Field.getInstance()));
        // WHEN
        final Binary64 actual = negate.value(mockState);
        // THEN
        assertEquals(Binary64.ONE.negate(), actual);
    }
}
