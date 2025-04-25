/* Copyright 2022-2025 Romain Serra
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
package org.orekit.propagation.events;

import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Test;
import org.orekit.TestUtils;
import org.orekit.frames.Frame;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ExtendedPositionProvider;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FieldAngularSeparationDetectorTest {

    @Test
    void testGetter() {
        // GIVEN
        final ExtendedPositionProvider mockedBeacon = mock();
        final ExtendedPositionProvider mockedObserver = mock();
        final Binary64 expectedAngle = Binary64.ONE;
        // WHEN
        final FieldAngularSeparationDetector<Binary64> detector = new FieldAngularSeparationDetector<>(mockedBeacon,
                mockedObserver, expectedAngle);
        // THEN
        assertEquals(expectedAngle, detector.getProximityAngle());
        assertEquals(mockedBeacon, detector.getBeacon());
        assertEquals(mockedObserver, detector.getObserver());
    }

    @Test
    void testG() {
        // GIVEN
        final Binary64Field field = Binary64Field.getInstance();
        final SpacecraftState state = new SpacecraftState(TestUtils.getDefaultOrbit(AbsoluteDate.ARBITRARY_EPOCH));
        final FieldSpacecraftState<Binary64> fieldState = new FieldSpacecraftState<>(field, state);
        final Frame frame = fieldState.getFrame();
        final ExtendedPositionProvider mockedBeacon = mock();
        final FieldVector3D<Binary64> beaconPosition = FieldVector3D.getMinusJ(field);
        final FieldVector3D<Binary64> observerPosition = FieldVector3D.getMinusK(field);
        when(mockedBeacon.getPosition(fieldState.getDate(), frame)).thenReturn(beaconPosition);
        when(mockedBeacon.getPosition(state.getDate(), frame)).thenReturn(beaconPosition.toVector3D());
        final ExtendedPositionProvider mockedObserver = mock();
        when(mockedObserver.getPosition(fieldState.getDate(), frame)).thenReturn(observerPosition);
        when(mockedObserver.getPosition(state.getDate(), frame)).thenReturn(observerPosition.toVector3D());
        final Binary64 angle = Binary64.ONE;
        // WHEN
        final FieldAngularSeparationDetector<Binary64> fieldDetector = new FieldAngularSeparationDetector<>(mockedBeacon,
                mockedObserver, angle);
        // THEN
        final AngularSeparationDetector detector = new AngularSeparationDetector(mockedBeacon, mockedObserver,
                angle.getReal());
        final Binary64 actualG = fieldDetector.g(fieldState);
        assertEquals(detector.g(state), actualG.getReal());
    }
}
