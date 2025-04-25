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
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Test;
import org.orekit.TestUtils;
import org.orekit.frames.TopocentricFrame;
import org.orekit.models.AtmosphericRefractionModel;
import org.orekit.models.earth.ITURP834AtmosphericRefraction;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.propagation.events.handlers.FieldStopOnEvent;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.ExtendedPositionProvider;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FieldGroundAtNightDetectorTest {

    @Test
    void testGRefraction() {
        // GIVEN
        final Binary64Field field = Binary64Field.getInstance();
        final SpacecraftState state = new SpacecraftState(TestUtils.getDefaultOrbit(AbsoluteDate.ARBITRARY_EPOCH));
        final FieldSpacecraftState<Binary64> fieldState = new FieldSpacecraftState<>(field, state);
        final TopocentricFrame frame = mock();
        final Vector3D sunPosition = Vector3D.PLUS_J.scalarMultiply(Constants.JPL_SSD_ASTRONOMICAL_UNIT);
        final FieldVector3D<Binary64> fieldSunPosition = new FieldVector3D<>(field, sunPosition);
        when(frame.getElevation(fieldSunPosition, state.getFrame(), fieldState.getDate())).thenReturn(fieldSunPosition.getDelta());
        when(frame.getElevation(sunPosition, state.getFrame(), state.getDate())).thenReturn(sunPosition.getDelta());
        final ExtendedPositionProvider provider = mock();
        when(provider.getPosition(fieldState.getDate(), fieldState.getFrame())).thenReturn(fieldSunPosition);
        when(provider.getPosition(state.getDate(), state.getFrame())).thenReturn(sunPosition);
        final Binary64 duskDawnElevation = Binary64.ZERO;
        final AtmosphericRefractionModel model = new ITURP834AtmosphericRefraction(0.);
        final FieldGroundAtNightDetector<Binary64> fieldDetector = new FieldGroundAtNightDetector<>(frame, provider,
                duskDawnElevation,  model);
        // WHEN
        final Binary64 actualG = fieldDetector.g(fieldState);
        // THEN
        final GroundAtNightDetector detector = new GroundAtNightDetector(frame, provider, duskDawnElevation.getReal(), model);
        assertEquals(detector.g(state), actualG.getReal());
    }

    @Test
    void testG() {
        // GIVEN
        final Binary64Field field = Binary64Field.getInstance();
        final SpacecraftState state = new SpacecraftState(TestUtils.getDefaultOrbit(AbsoluteDate.ARBITRARY_EPOCH));
        final FieldSpacecraftState<Binary64> fieldState = new FieldSpacecraftState<>(field, state);
        final TopocentricFrame frame = mock();
        final Vector3D sunPosition = Vector3D.PLUS_J.scalarMultiply(Constants.JPL_SSD_ASTRONOMICAL_UNIT);
        final FieldVector3D<Binary64> fieldSunPosition = new FieldVector3D<>(field, sunPosition);
        when(frame.getElevation(fieldSunPosition, state.getFrame(), fieldState.getDate())).thenReturn(fieldSunPosition.getDelta());
        final ExtendedPositionProvider provider = mock();
        when(provider.getPosition(fieldState.getDate(), fieldState.getFrame())).thenReturn(fieldSunPosition);
        final Binary64 duskDawnElevation = Binary64.ONE;
        final FieldGroundAtNightDetector<Binary64> detector = new FieldGroundAtNightDetector<>(frame, provider,
                duskDawnElevation,  null);
        // WHEN
        final Binary64 actualG = detector.g(fieldState);
        // THEN
        assertEquals(fieldSunPosition.getDelta().subtract(duskDawnElevation).negate(), actualG);
    }

    @Test
    void testCreate() {
        // GIVEN
        final TopocentricFrame mockedFrame = mock();
        final ExtendedPositionProvider mockedProvider = mock();
        final FieldGroundAtNightDetector<Binary64> detector = new FieldGroundAtNightDetector<>(mockedFrame, mockedProvider,
                Binary64.ZERO,  null);
        final FieldEventDetectionSettings<Binary64> detectionSettings = new FieldEventDetectionSettings<>(Binary64Field.getInstance(),
                EventDetectionSettings.getDefaultEventDetectionSettings());
        final FieldEventHandler<Binary64> expectedHandler = new FieldStopOnEvent<>();
        // WHEN
        final FieldGroundAtNightDetector<Binary64> createdDetector = detector.create(detectionSettings, expectedHandler);
        // THEN
        assertEquals(detector.getTopocentricFrame(), createdDetector.getTopocentricFrame());
        assertEquals(detectionSettings, createdDetector.getDetectionSettings());
        assertEquals(expectedHandler, createdDetector.getHandler());
    }
}

