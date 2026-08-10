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
import org.orekit.TestUtils;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlaneCrossingFunctionTest {

    @Test
    void testGetters() {
        // GIVEN
        final Frame frame = FramesFactory.getGCRF();
        final Vector3D normal = new Vector3D(1, 2, 3);
        // WHEN
        final PlaneCrossingFunction crossingFunction = new PlaneCrossingFunction(normal, frame);
        // THEN
        assertEquals(frame, crossingFunction.getPlaneFrame());
        assertEquals(normal.normalize(), crossingFunction.getPlaneNormal());
    }

    @Test
    void testValueVersusNode() {
        // GIVEN
        final Frame frame = FramesFactory.getGCRF();
        final Vector3D normal = Vector3D.PLUS_K;
        final PlaneCrossingFunction crossingFunction = new PlaneCrossingFunction(normal, frame);
        final SpacecraftState state = mock();
        final Vector3D position = new Vector3D(3000, 1000, -2000);
        when(state.getPosition(frame)).thenReturn(position);
        // WHEN
        final double actualValue = crossingFunction.value(state);
        // THEN
        final NodeEventFunction nodeEventFunction = new NodeEventFunction(frame);
        assertEquals(nodeEventFunction.value(state), actualValue);
    }

    @Test
    void testValue() {
        // GIVEN
        final Frame frame = FramesFactory.getGCRF();
        final Vector3D normal = new Vector3D(1, 2, 3);
        final PlaneCrossingFunction crossingFunction = new PlaneCrossingFunction(normal, frame);
        final SpacecraftState state = mock();
        final Vector3D position = new Vector3D(300, 100, -200);
        when(state.getPosition(frame)).thenReturn(position);
        // WHEN
        final double actualValue = crossingFunction.value(state);
        // THEN
        assertEquals(normal.normalize().dotProduct(position), actualValue);
    }

    @Test
    void testValueField() {
        // GIVEN
        final Frame frame = FramesFactory.getGCRF();
        final Vector3D normal = new Vector3D(1, 2, 3);
        final PlaneCrossingFunction crossingFunction = new PlaneCrossingFunction(normal, frame);
        final SpacecraftState state = new SpacecraftState(TestUtils.getDefaultOrbit(AbsoluteDate.ARBITRARY_EPOCH));
        final FieldSpacecraftState<Binary64> fieldState = new FieldSpacecraftState<>(Binary64Field.getInstance(), state);
        // WHEN
        final Binary64 actualValue = crossingFunction.value(fieldState);
        // THEN
        assertEquals(crossingFunction.value(state), actualValue.getReal());
    }
}
