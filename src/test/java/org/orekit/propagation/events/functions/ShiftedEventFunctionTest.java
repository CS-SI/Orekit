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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Test;
import org.orekit.frames.FramesFactory;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.PVCoordinates;

import static org.junit.jupiter.api.Assertions.*;

class ShiftedEventFunctionTest {

    @Test
    void testDependsOn() {
        // GIVEN
        final SingleDateEventFunction dateEventFunction = new SingleDateEventFunction(AbsoluteDate.ARBITRARY_EPOCH);
        final ShiftedEventFunction shiftedEventFunction = new ShiftedEventFunction(dateEventFunction, 0.);
        // WHEN
        assertEquals(dateEventFunction.dependsOnTimeOnly(), shiftedEventFunction.dependsOnTimeOnly());
        assertEquals(dateEventFunction.dependsOnMainVariablesOnly(), shiftedEventFunction.dependsOnMainVariablesOnly());
    }

    @Test
    void testValue() {
        // GIVEN
        final ApsideEventFunction apsideEventFunction = new ApsideEventFunction();
        final ShiftedEventFunction shiftedEventFunction = new ShiftedEventFunction(apsideEventFunction, 1.);
        final Vector3D position = new Vector3D(1.0, 2.0, 3.0);
        final Vector3D velocity = new Vector3D(4.0, 5.0, 6.0);
        final AbsolutePVCoordinates coordinates = new AbsolutePVCoordinates(FramesFactory.getGCRF(), AbsoluteDate.ARBITRARY_EPOCH,
                new PVCoordinates(position, velocity));
        final SpacecraftState state = new SpacecraftState(coordinates);
        // WHEN
        final double value = shiftedEventFunction.value(state);
        // THEN
        final double expected = apsideEventFunction.value(state.shiftedBy(shiftedEventFunction.getTimeShift()));
        assertEquals(expected, value);
    }

    @Test
    void testValueField() {
        // GIVEN
        final ApsideEventFunction apsideEventFunction = new ApsideEventFunction();
        final ShiftedEventFunction shiftedEventFunction = new ShiftedEventFunction(apsideEventFunction, 1.);
        final Vector3D position = new Vector3D(1.0, 2.0, 3.0);
        final Vector3D velocity = new Vector3D(4.0, 5.0, 6.0);
        final AbsolutePVCoordinates coordinates = new AbsolutePVCoordinates(FramesFactory.getGCRF(), AbsoluteDate.ARBITRARY_EPOCH,
                new PVCoordinates(position, velocity));
        final SpacecraftState state = new SpacecraftState(coordinates);
        final FieldSpacecraftState<Binary64> fieldState = new FieldSpacecraftState<>(Binary64Field.getInstance(), state);
        // WHEN
        final Binary64 value = shiftedEventFunction.value(fieldState);
        // THEN
        final Binary64 expected = apsideEventFunction.value(fieldState.shiftedBy(shiftedEventFunction.getTimeShift()));
        assertEquals(expected, value);
    }
}
