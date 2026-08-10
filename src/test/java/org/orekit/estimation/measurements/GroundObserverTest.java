/* Copyright 2022-2026 Romain Serra
 * Licensed to CS Group (CS) under one or more
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
package org.orekit.estimation.measurements;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.TestUtils;
import org.orekit.Utils;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.models.earth.ReferenceEllipsoid;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldTrackingCoordinates;
import org.orekit.utils.TrackingCoordinates;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GroundObserverTest {

    @BeforeEach
    void setUp() {
        Utils.setDataRoot("regular-data:gnss");
    }

    @Test
    void testGetTrackingCoordinates() {
        // GIVEN
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final GroundObserver groundObserver = mock();
        final ReferenceEllipsoid ellipsoid = ReferenceEllipsoid.getWgs84(FramesFactory.getGTOD(true));
        when(groundObserver.getParentShape()).thenReturn(ellipsoid);
        final GeodeticPoint point = new GeodeticPoint(1., 2., 3.);
        when(groundObserver.getOffsetGeodeticPoint(date)).thenReturn(point);
        final SpacecraftState state = new SpacecraftState(TestUtils.getDefaultOrbit(date));
        when(groundObserver.getTrackingCoordinates(state)).thenCallRealMethod();
        // WHEN
        final TrackingCoordinates coordinates = groundObserver.getTrackingCoordinates(state);
        //
        final TopocentricFrame topocentricFrame = new TopocentricFrame(ellipsoid, point, "");
        final Vector3D position = state.getPosition(topocentricFrame);
        final TrackingCoordinates expected = topocentricFrame.getTrackingCoordinates(position, topocentricFrame, state.getDate());
        assertEquals(expected.getElevation(), coordinates.getElevation());
        assertEquals(expected.getAzimuth(), coordinates.getAzimuth());
        assertEquals(expected.getRange(), coordinates.getRange());
    }

    @Test
    void testFieldGetTrackingCoordinates() {
        // GIVEN
        final FieldAbsoluteDate<Binary64> date = FieldAbsoluteDate.getArbitraryEpoch(Binary64Field.getInstance());
        final GroundObserver groundObserver = mock();
        final ReferenceEllipsoid ellipsoid = ReferenceEllipsoid.getWgs84(FramesFactory.getGTOD(true));
        when(groundObserver.getParentShape()).thenReturn(ellipsoid);
        final FieldGeodeticPoint<Binary64> point = new FieldGeodeticPoint<>(Binary64Field.getInstance(), new GeodeticPoint(1., 2., 3.));
        when(groundObserver.getOffsetGeodeticPoint(date)).thenReturn(point);
        when(groundObserver.getOffsetGeodeticPoint(date.toAbsoluteDate())).thenReturn(point.toGeodeticPoint());
        final SpacecraftState state = new SpacecraftState(TestUtils.getDefaultOrbit(date.toAbsoluteDate()));
        final FieldSpacecraftState<Binary64> fieldState = new FieldSpacecraftState<>(Binary64Field.getInstance(), state);
        when(groundObserver.getTrackingCoordinates(fieldState)).thenCallRealMethod();
        when(groundObserver.getTrackingCoordinates(state)).thenCallRealMethod();
        // WHEN
        final FieldTrackingCoordinates<Binary64> coordinates = groundObserver.getTrackingCoordinates(fieldState);
        //
        final TrackingCoordinates expected = groundObserver.getTrackingCoordinates(state);
        assertEquals(expected.getAzimuth(), coordinates.getAzimuth().getReal(), 1e-15);
        assertEquals(expected.getElevation(), coordinates.getElevation().getReal(), 1e-15);
        assertEquals(expected.getRange(), coordinates.getRange().getReal(), 1e-15);
    }
}
