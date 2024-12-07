/* Copyright 2022-2024 Romain Serra
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

import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.propagation.events.handlers.FieldStopOnEvent;
import org.orekit.time.FieldAbsoluteDate;

class FieldAdapterDetectorTest {

    @Test
    @SuppressWarnings("unchecked")
    void testGetDetectionSettings() {
        // GIVEN
        final FieldEventDetector<?> detector = Mockito.mock(FieldEventDetector.class);
        final FieldEventDetectionSettings detectionSettings = Mockito.mock(FieldEventDetectionSettings.class);
        Mockito.when(detector.getDetectionSettings()).thenReturn(detectionSettings);
        final FieldAdapterDetector<?> adapterDetector = new FieldAdapterDetector<>(detector);
        // WHEN
        final FieldEventDetectionSettings<?> actualSettings = adapterDetector.getDetectionSettings();
        // THEN
        Assertions.assertEquals(detectionSettings, actualSettings);
    }

    @Test
    @SuppressWarnings("unchecked")

    void testGetHandler() {
        // GIVEN
        final FieldEventDetector<?> detector = Mockito.mock(FieldEventDetector.class);
        final FieldEventHandler handler = Mockito.mock(FieldEventHandler.class);
        Mockito.when(detector.getHandler()).thenReturn(handler);
        final FieldAdapterDetector<?> adapterDetector = new FieldAdapterDetector<>(detector);
        // WHEN
        final FieldEventHandler<?> actualHandler = adapterDetector.getHandler();
        // THEN
        Assertions.assertEquals(handler, actualHandler);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testInit() {
        // GIVEN
        final FieldEventDetector<?> detector = Mockito.mock(FieldEventDetector.class);
        Mockito.when(detector.getHandler()).thenReturn(new FieldStopOnEvent<>());
        final FieldSpacecraftState mockedState = Mockito.mock(FieldSpacecraftState.class);
        final FieldAbsoluteDate mockedDate = Mockito.mock(FieldAbsoluteDate.class);
        final FieldAdapterDetector<?> adapterDetector = new FieldAdapterDetector<>(detector);
        // WHEN
        adapterDetector.init(mockedState, mockedDate);
        // THEN
        Mockito.verify(detector).init(mockedState, mockedDate);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testG() {
        // GIVEN
        final Binary64Field field = Binary64Field.getInstance();
        final FieldAbsoluteDate<Binary64> date = FieldAbsoluteDate.getArbitraryEpoch(field);
        final FieldEventDetector<Binary64> detector = new FieldDateDetector<>(date);
        final FieldSpacecraftState<Binary64> mockedState = Mockito.mock(FieldSpacecraftState.class);
        Mockito.when(mockedState.getDate()).thenReturn(date);
        final FieldAdapterDetector<Binary64> adapterDetector = new FieldAdapterDetector<>(detector);
        // WHEN
        final double actualG = adapterDetector.g(mockedState).getReal();
        // THEN
        Assertions.assertEquals(detector.g(mockedState).getReal(), actualG);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testFinish() {
        // GIVEN
        final FieldEventDetector<?> detector = Mockito.mock(FieldEventDetector.class);
        Mockito.when(detector.getHandler()).thenReturn(new FieldStopOnEvent<>());
        final FieldSpacecraftState mockedState = Mockito.mock(FieldSpacecraftState.class);
        final FieldAdapterDetector<?> adapterDetector = new FieldAdapterDetector<>(detector);
        // WHEN
        adapterDetector.finish(mockedState);
        // THEN
        Mockito.verify(detector).finish(mockedState);
    }
}
