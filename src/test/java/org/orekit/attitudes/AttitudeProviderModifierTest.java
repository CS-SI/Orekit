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
package org.orekit.attitudes;

import org.hipparchus.complex.Complex;
import org.hipparchus.complex.ComplexField;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.ParameterDriver;

import java.util.ArrayList;
import java.util.List;

class AttitudeProviderModifierTest {

    @Test
    void testGetFrozenAttitudeProviderEventDetectors() {
        // GIVEN
        final AttitudeProvider attitudeProvider = Mockito.mock(AttitudeProvider.class);
        final List<ParameterDriver> drivers = new ArrayList<>();
        Mockito.when(attitudeProvider.getEventDetectors()).thenCallRealMethod();
        Mockito.when(attitudeProvider.getEventDetectors(drivers)).thenCallRealMethod();
        // WHEN
        final AttitudeProviderModifier frozenAttitudeProvider = AttitudeProviderModifier
                .getFrozenAttitudeProvider(attitudeProvider);
        // THEN
        Assertions.assertEquals(attitudeProvider.getEventDetectors().count(),
                frozenAttitudeProvider.getEventDetectors().count());
        Assertions.assertEquals(attitudeProvider.getEventDetectors(drivers).count(),
                frozenAttitudeProvider.getEventDetectors(drivers).count());
    }

    @Test
    void testGetFrozenAttitudeProvider() {
        // GIVEN
        final AttitudeProvider attitudeProvider = Mockito.mock(AttitudeProvider.class);
        Mockito.when(attitudeProvider.getEventDetectors()).thenCallRealMethod();
        final Rotation expectedRotation = new Rotation(Vector3D.MINUS_I, Vector3D.MINUS_K);
        final PVCoordinatesProvider mockedPVCoordinatesProvider = Mockito.mock(PVCoordinatesProvider.class);
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final Frame mockedFrame = Mockito.mock(Frame.class);
        Mockito.when(attitudeProvider.getAttitudeRotation(mockedPVCoordinatesProvider, date, mockedFrame)).thenReturn(expectedRotation);
        // WHEN
        final AttitudeProviderModifier frozenAttitudeProvider = AttitudeProviderModifier.getFrozenAttitudeProvider(attitudeProvider);
        final Attitude attitude = frozenAttitudeProvider.getAttitude(mockedPVCoordinatesProvider, date, mockedFrame);
        // THEN
        Assertions.assertEquals(attitudeProvider, frozenAttitudeProvider.getUnderlyingAttitudeProvider());
        final Rotation actualRotation = attitude.getRotation();
        Assertions.assertEquals(0., Rotation.distance(expectedRotation, actualRotation));
        Assertions.assertEquals(Vector3D.ZERO, attitude.getRotationAcceleration());
        Assertions.assertEquals(Vector3D.ZERO, attitude.getSpin());
    }

    @SuppressWarnings("unchecked")
    @Test
    void testGetFrozenAttitudeProviderField() {
        // GIVEN
        final AttitudeProvider attitudeProvider = Mockito.mock(AttitudeProvider.class);
        final ComplexField field = ComplexField.getInstance();
        final Rotation expectedRotation = new Rotation(Vector3D.MINUS_I, Vector3D.MINUS_K);
        final FieldRotation<Complex> fieldRotation = new FieldRotation<>(field, expectedRotation);
        final FieldPVCoordinatesProvider<Complex> mockedPVCoordinatesProvider = Mockito.mock(FieldPVCoordinatesProvider.class);
        final FieldAbsoluteDate<Complex> date = FieldAbsoluteDate.getArbitraryEpoch(field);
        final Frame mockedFrame = Mockito.mock(Frame.class);
        Mockito.when(attitudeProvider.getAttitudeRotation(mockedPVCoordinatesProvider, date, mockedFrame)).thenReturn(fieldRotation);
        // WHEN
        final AttitudeProvider frozenAttitudeProvider = AttitudeProviderModifier.getFrozenAttitudeProvider(attitudeProvider);
        final FieldAttitude<Complex> attitude = frozenAttitudeProvider.getAttitude(mockedPVCoordinatesProvider, date,
                mockedFrame);
        // THEN
        final Rotation actualRotation = attitude.getRotation().toRotation();
        Assertions.assertEquals(0., Rotation.distance(expectedRotation, actualRotation));
        Assertions.assertEquals(FieldVector3D.getZero(field), attitude.getRotationAcceleration());
        Assertions.assertEquals(FieldVector3D.getZero(field), attitude.getSpin());
    }

    @Test
    void testGetFrozenAttitudeProviderFieldEventDetectors() {
        // GIVEN
        final AttitudeProvider attitudeProvider = Mockito.mock(AttitudeProvider.class);
        final ComplexField field = ComplexField.getInstance();
        Mockito.when(attitudeProvider.getFieldEventDetectors(field)).thenCallRealMethod();
        final List<ParameterDriver> driverList = new ArrayList<>();
        Mockito.when(attitudeProvider.getFieldEventDetectors(field, driverList)).thenCallRealMethod();
        // WHEN
        final AttitudeProvider frozenAttitudeProvider = AttitudeProviderModifier.getFrozenAttitudeProvider(attitudeProvider);
        // THEN
        Assertions.assertEquals(attitudeProvider.getFieldEventDetectors(field).count(),
                frozenAttitudeProvider.getEventDetectors().count());
        Assertions.assertEquals(attitudeProvider.getFieldEventDetectors(field, driverList).count(),
                frozenAttitudeProvider.getFieldEventDetectors(field, driverList).count());
    }
}
