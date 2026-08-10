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
import org.orekit.Utils;
import org.orekit.bodies.AnalyticalSolarPositionProvider;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.models.earth.ReferenceEllipsoid;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ExtendedPositionProvider;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GroundAtNightEventFunctionTest {

    @BeforeEach
    void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testField() {
        // GIVEN
        final ExtendedPositionProvider positionProvider = new AnalyticalSolarPositionProvider();
        final TopocentricFrame topocentricFrame = new TopocentricFrame(ReferenceEllipsoid.getWgs84(FramesFactory.getGTOD(true)),
                new GeodeticPoint(0, 0, 0), "");
        final GroundAtNightEventFunction eventFunction = new GroundAtNightEventFunction(topocentricFrame, positionProvider,
                0., null);
        final Frame frame = FramesFactory.getGCRF();
        final FieldSpacecraftState<Binary64> fieldState = mock();
        when(fieldState.getDate()).thenReturn(FieldAbsoluteDate.getArbitraryEpoch(Binary64Field.getInstance()));
        when(fieldState.getFrame()).thenReturn(frame);
        final SpacecraftState state = mock();
        when(state.getFrame()).thenReturn(frame);
        when(state.getDate()).thenReturn(AbsoluteDate.ARBITRARY_EPOCH);
        // WHEN
        final Binary64 value = eventFunction.value(fieldState);
        // THEN
        assertEquals(eventFunction.value(state), value.getReal());
    }

    @Test
    void testDependsOnTimeOnly() {
        // GIVEN
        final ExtendedPositionProvider positionProvider = new AnalyticalSolarPositionProvider();
        final TopocentricFrame topocentricFrame = mock();
        final GroundAtNightEventFunction eventFunction = new GroundAtNightEventFunction(topocentricFrame, positionProvider,
                0., null);
        // WHEN
        assertTrue(eventFunction.dependsOnTimeOnly());
    }
}
