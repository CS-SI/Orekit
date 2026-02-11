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
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LongitudeValueCrossingFunctionTest {

    @Test
    void testGetter() {
        // GIVEN
        final BodyShape bodyShape = new OneAxisEllipsoid(1000, 0., FramesFactory.getEME2000());
        final double expectedLongitude = -1;
        final LongitudeValueCrossingFunction eventFunction = new LongitudeValueCrossingFunction(bodyShape, expectedLongitude);
        // WHEN
        final double actual = eventFunction.getCriticalLongitude();
        // THEN
        assertEquals(expectedLongitude, actual);
    }

    @Test
    void testValue() {
        // GIVEN
        final Orbit orbit = TestUtils.getDefaultOrbit(AbsoluteDate.ARBITRARY_EPOCH);
        final SpacecraftState state = new SpacecraftState(orbit);
        final BodyShape bodyShape = new OneAxisEllipsoid(1000, 0., FramesFactory.getEME2000());
        final LongitudeValueCrossingFunction eventFunction = new LongitudeValueCrossingFunction(bodyShape, 0.);
        // WHEN
        final double value = eventFunction.value(state);
        // THEN
        assertEquals(eventFunction.transformToGeodeticPoint(state).getLongitude(), value);
    }

    @Test
    void testValueField() {
        // GIVEN
        final Orbit orbit = TestUtils.getDefaultOrbit(AbsoluteDate.ARBITRARY_EPOCH);
        final SpacecraftState state = new SpacecraftState(orbit);
        final FieldSpacecraftState<Binary64> fieldState = new FieldSpacecraftState<>(Binary64Field.getInstance(), state);
        final BodyShape bodyShape = new OneAxisEllipsoid(1000, 0.1, FramesFactory.getEME2000());
        final LongitudeValueCrossingFunction eventFunction = new LongitudeValueCrossingFunction(bodyShape, 1.);
        // WHEN
        final Binary64 value = eventFunction.value(fieldState);
        // THEN
        assertEquals(eventFunction.value(state), value.getReal());
    }
}
