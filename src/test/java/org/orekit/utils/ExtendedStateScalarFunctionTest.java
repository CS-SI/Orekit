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
package org.orekit.utils;

import java.util.function.ToDoubleFunction;

import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Test;
import org.orekit.frames.FramesFactory;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExtendedStateScalarFunctionTest {

    @Test
    void testOf() {
        // GIVEN
        final ToDoubleFunction<SpacecraftState> function = SpacecraftState::getMass;
        final ExtendedStateScalarFunction scalarFunction = ExtendedStateScalarFunction.of(function);
        final SpacecraftState mockedState = mock();
        final double expectedValue = 2.;
        when(mockedState.getMass()).thenReturn(expectedValue);
        // WHEN
        final double actualValue = scalarFunction.value(mockedState);
        // THEN
        assertEquals(expectedValue, actualValue);
    }

    @Test
    void testFieldValue() {
        // GIVEN
        final ExtendedStateScalarFunction scalarFunction = SpacecraftState::getMass;
        final double expectedValue = 2.;
        final SpacecraftState state = new SpacecraftState(new AbsolutePVCoordinates(FramesFactory.getGCRF(),
                AbsoluteDate.ARBITRARY_EPOCH, new PVCoordinates())).withMass(expectedValue);
        final FieldSpacecraftState<Binary64> fieldState = new FieldSpacecraftState<>(Binary64Field.getInstance(), state);
        // WHEN
        final Binary64 actualValue = scalarFunction.value(fieldState);
        // THEN
        assertEquals(expectedValue, actualValue.getReal());
    }
}
