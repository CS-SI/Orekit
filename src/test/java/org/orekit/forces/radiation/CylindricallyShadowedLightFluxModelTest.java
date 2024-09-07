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
package org.orekit.forces.radiation;

import org.hipparchus.complex.Complex;
import org.hipparchus.complex.ComplexField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.events.Action;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.frames.Frame;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.*;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.FieldStopOnEvent;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ExtendedPositionProvider;

import java.util.List;

class CylindricallyShadowedLightFluxModelTest {

    @Test
    void testGetLightingRatio() {
        // GIVEN
        final double occultingBodyRadius = 0.5;
        final Vector3D sunPosition = Vector3D.PLUS_I.scalarMultiply(10.);
        final ExtendedPositionProvider sun = mockProvider(sunPosition);
        final CylindricallyShadowedLightFluxModel model = new CylindricallyShadowedLightFluxModel(Double.NaN, sun, occultingBodyRadius);
        final CylindricalShadowEclipseDetector detector = new CylindricalShadowEclipseDetector(sun,
                model.getOccultingBodyRadius(), Mockito.mock(EventHandler.class));
        // WHEN & THEN
        final int size = 100;
        for (int i = 0; i < size; i++) {
            final double angle = i * FastMath.PI / size;
            final Vector3D position = new Vector3D(angle, 0.);
            final double ratio = model.getLightingRatio(position, sunPosition);
            final SpacecraftState state = mockState(position);
            final double g = detector.g(state);
            if (g < 0.) {
                Assertions.assertEquals(0., ratio);
            } else {
                Assertions.assertEquals(1., ratio);
            }
        }
    }

    private ExtendedPositionProvider mockProvider(final Vector3D sunPosition) {
        final ExtendedPositionProvider mockedProvider = Mockito.mock(ExtendedPositionProvider.class);
        Mockito.when(mockedProvider.getPosition(Mockito.any(AbsoluteDate.class), Mockito.any(Frame.class)))
                .thenReturn(sunPosition);
        return mockedProvider;
    }

    private SpacecraftState mockState(final Vector3D position) {
        final SpacecraftState mockedState = Mockito.mock(SpacecraftState.class);
        Mockito.when(mockedState.getPosition()).thenReturn(position);
        Mockito.when(mockedState.getDate()).thenReturn(AbsoluteDate.ARBITRARY_EPOCH);
        Mockito.when(mockedState.getFrame()).thenReturn(Mockito.mock(Frame.class));
        return mockedState;
    }

    @Test
    void testFieldGetLightingRatio() {
        // GIVEN
        final ComplexField field = ComplexField.getInstance();
        final double occultingBodyRadius = 0.5;
        final FieldVector3D<Complex> sunPosition = FieldVector3D.getPlusI(field).scalarMultiply(10.);
        final ExtendedPositionProvider sun = mockFieldProvider(sunPosition);
        final CylindricallyShadowedLightFluxModel model = new CylindricallyShadowedLightFluxModel(Double.NaN, sun, occultingBodyRadius);
        final FieldCylindricalShadowEclipseDetector<Complex> detector = new FieldCylindricalShadowEclipseDetector<>(sun,
                new Complex(model.getOccultingBodyRadius()), new FieldStopOnEvent<>());
        // WHEN & THEN
        final int size = 100;
        for (int i = 0; i < size; i++) {
            final double angle = i * FastMath.PI / size;
            final FieldVector3D<Complex> position = new FieldVector3D<>(field, new Vector3D(angle, 0.));
            final double ratio = model.getLightingRatio(position, sunPosition).getReal();
            final FieldSpacecraftState<Complex> state = mockFieldState(position);
            final double g = detector.g(state).getReal();
            if (g < 0.) {
                Assertions.assertEquals(0., ratio);
            } else {
                Assertions.assertEquals(1., ratio);
            }
        }
    }

    @Test
    void testGetUnoccultedFluxVector() {
        // GIVEN
        final ComplexField field = ComplexField.getInstance();
        final double occultingBodyRadius = 0.5;
        final FieldVector3D<Complex> sunPosition = FieldVector3D.getPlusI(field).scalarMultiply(10.);
        final ExtendedPositionProvider sun = mockFieldProvider(sunPosition);
        final CylindricallyShadowedLightFluxModel model = new CylindricallyShadowedLightFluxModel(Double.NaN, sun, occultingBodyRadius);
        final Vector3D position = new Vector3D(1., 1.);
        final FieldVector3D<Complex> fieldPosition = new FieldVector3D<>(field, position);
        // WHEN
        final FieldVector3D<Complex> fieldFlux = model.getUnoccultedFluxVector(fieldPosition);
        // THEN
        final Vector3D expectedFlux = model.getUnoccultedFluxVector(position);
        Assertions.assertEquals(expectedFlux, fieldFlux.toVector3D());
    }

    @SuppressWarnings("unchecked")
    private ExtendedPositionProvider mockFieldProvider(final FieldVector3D<Complex> sunPosition) {
        final ExtendedPositionProvider mockedProvider = Mockito.mock(ExtendedPositionProvider.class);
        Mockito.when(mockedProvider.getPosition(Mockito.any(FieldAbsoluteDate.class), Mockito.any(Frame.class)))
                .thenReturn(sunPosition);
        return mockedProvider;
    }

    @SuppressWarnings("unchecked")
    private FieldSpacecraftState<Complex> mockFieldState(final FieldVector3D<Complex> position) {
        final FieldSpacecraftState<Complex> mockedState = Mockito.mock(FieldSpacecraftState.class);
        Mockito.when(mockedState.getPosition()).thenReturn(position);
        Mockito.when(mockedState.getDate()).thenReturn(Mockito.mock(FieldAbsoluteDate.class));
        Mockito.when(mockedState.getFrame()).thenReturn(Mockito.mock(Frame.class));
        return mockedState;
    }

    @Test
    void testGetEclipseConditionsDetector() {
        // GIVEN
        final CylindricallyShadowedLightFluxModel model = Mockito.mock(CylindricallyShadowedLightFluxModel.class);
        Mockito.when(model.getOccultingBodyRadius()).thenReturn(1.);
        Mockito.when(model.getEclipseConditionsDetector()).thenCallRealMethod();
        Mockito.when(model.getEventDetectionSettings()).thenReturn(EventDetectionSettings.getDefaultEventDetectionSettings());
        // WHEN
        final List<EventDetector> detectors = model.getEclipseConditionsDetector();
        // THEN
        Assertions.assertEquals(1, detectors.size());
        final EventDetector detector = detectors.get(0);
        Assertions.assertInstanceOf(CylindricalShadowEclipseDetector.class, detector);
        final Action action = detector.getHandler().eventOccurred(Mockito.mock(SpacecraftState.class), detector, false);
        Assertions.assertEquals(Action.RESET_DERIVATIVES, action);
    }

    @Test
    void testGetFieldEclipseConditionsDetector() {
        // GIVEN
        final ComplexField field = ComplexField.getInstance();
        final CylindricallyShadowedLightFluxModel model = Mockito.mock(CylindricallyShadowedLightFluxModel.class);
        Mockito.when(model.getOccultingBodyRadius()).thenReturn(1.);
        Mockito.when(model.getEventDetectionSettings()).thenReturn(EventDetectionSettings.getDefaultEventDetectionSettings());
        Mockito.when(model.getFieldEclipseConditionsDetector(field)).thenCallRealMethod();
        Mockito.when(model.getEclipseConditionsDetector()).thenCallRealMethod();
        // WHEN
        final List<FieldEventDetector<Complex>> fieldEventDetectors = model.getFieldEclipseConditionsDetector(field);
        // THEN
        final List<EventDetector> eventDetectors = model.getEclipseConditionsDetector();
        Assertions.assertEquals(eventDetectors.size(), fieldEventDetectors.size());
        Assertions.assertInstanceOf(FieldCylindricalShadowEclipseDetector.class, fieldEventDetectors.get(0));
        final FieldCylindricalShadowEclipseDetector<Complex> fieldShadowDetector = (FieldCylindricalShadowEclipseDetector<Complex>) fieldEventDetectors.get(0);
        final CylindricalShadowEclipseDetector shadowDetector = (CylindricalShadowEclipseDetector) eventDetectors.get(0);
        Assertions.assertEquals(shadowDetector.getThreshold(), fieldShadowDetector.getThreshold().getReal());
        compareActions(shadowDetector, fieldShadowDetector);
    }

    @SuppressWarnings("unchecked")
    private void compareActions(final EventDetector eventDetector, final FieldEventDetector<Complex> fieldEventDetector) {
        final boolean isIncreasing = false;
        final Action expectedAction = eventDetector.getHandler().eventOccurred(Mockito.mock(SpacecraftState.class), eventDetector, isIncreasing);
        final Action actualAction = fieldEventDetector.getHandler().eventOccurred(Mockito.mock(FieldSpacecraftState.class), fieldEventDetector, isIncreasing);
        Assertions.assertEquals(expectedAction, actualAction);
    }

}
