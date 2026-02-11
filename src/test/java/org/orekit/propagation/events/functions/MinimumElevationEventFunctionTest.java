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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.TestUtils;
import org.orekit.Utils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.models.AtmosphericRefractionModel;
import org.orekit.models.earth.ITURP834AtmosphericRefraction;
import org.orekit.models.earth.ReferenceEllipsoid;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;

import static org.junit.jupiter.api.Assertions.*;

class MinimumElevationEventFunctionTest {

    @BeforeEach
    void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    void testValueField() {
        // GIVEN
        final AtmosphericRefractionModel model = new ITURP834AtmosphericRefraction(100e3);
        final TopocentricFrame frame = new TopocentricFrame(ReferenceEllipsoid.getWgs84(FramesFactory.getGTOD(true)),
                new GeodeticPoint(1., 2., 0.), "42");
        final MinimumElevationEventFunction eventFunction = new MinimumElevationEventFunction(model, frame, 0.1);
        final SpacecraftState state = new SpacecraftState(TestUtils.getDefaultOrbit(AbsoluteDate.ARBITRARY_EPOCH));
        final FieldSpacecraftState<Binary64> fieldState = new FieldSpacecraftState<>(Binary64Field.getInstance(), state);
        // WHEN
        final Binary64 actual = eventFunction.value(fieldState);
        // THEN
        assertEquals(eventFunction.value(state), actual.getReal());
    }

    @Test
    void testValueFieldNoRefraction() {
        // GIVEN
        final TopocentricFrame frame = new TopocentricFrame(ReferenceEllipsoid.getWgs84(FramesFactory.getGTOD(true)),
                new GeodeticPoint(1., 2., 0.), "42");
        final MinimumElevationEventFunction eventFunction = new MinimumElevationEventFunction(null, frame, 0.1);
        final SpacecraftState state = new SpacecraftState(TestUtils.getDefaultOrbit(AbsoluteDate.ARBITRARY_EPOCH));
        final FieldSpacecraftState<Binary64> fieldState = new FieldSpacecraftState<>(Binary64Field.getInstance(), state);
        // WHEN
        final Binary64 actual = eventFunction.value(fieldState);
        // THEN
        assertEquals(eventFunction.value(state), actual.getReal());
    }
}
