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

import org.hipparchus.CalculusFieldElement;
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
import org.orekit.utils.TimeStampedAngularCoordinates;
import org.orekit.utils.TimeStampedFieldAngularCoordinates;

import java.util.ArrayList;
import java.util.List;

class AttitudeProviderModifierTest {

    @Test
    void testGetAttitude() {
        // GIVEN
        final Rotation expectedRotation = new Rotation(Vector3D.MINUS_I, Vector3D.MINUS_K);
        final AttitudeProvider attitudeProvider = new TestProvider(expectedRotation);
        final AttitudeProviderModifier mockedModifier = Mockito.mock(AttitudeProviderModifier.class);
        Mockito.when(mockedModifier.getUnderlyingAttitudeProvider()).thenReturn(attitudeProvider);
        final PVCoordinatesProvider mockedPVCoordinatesProvider = Mockito.mock(PVCoordinatesProvider.class);
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final Frame mockedFrame = Mockito.mock(Frame.class);
        Mockito.when(mockedModifier.getAttitude(mockedPVCoordinatesProvider, date, mockedFrame)).thenCallRealMethod();
        // WHEN
        final Attitude actualAttitude = mockedModifier.getAttitude(mockedPVCoordinatesProvider, date, mockedFrame);
        // THEN
        final Attitude expectedAttitude = attitudeProvider.getAttitude(mockedPVCoordinatesProvider, date, mockedFrame);
        Assertions.assertEquals(0, Rotation.distance(expectedRotation, actualAttitude.getRotation()));
        Assertions.assertEquals(expectedAttitude.getSpin(), actualAttitude.getSpin());
        Assertions.assertEquals(expectedAttitude.getRotationAcceleration(), actualAttitude.getRotationAcceleration());
    }

    @SuppressWarnings("unchecked")
    @Test
    void testGetFieldAttitude() {
        // GIVEN
        final Rotation expectedRotation = new Rotation(Vector3D.MINUS_I, Vector3D.MINUS_K);
        final AttitudeProvider attitudeProvider = new TestProvider(expectedRotation);
        final ComplexField field = ComplexField.getInstance();
        final FieldPVCoordinatesProvider<Complex> mockedPVCoordinatesProvider = Mockito.mock(FieldPVCoordinatesProvider.class);
        final FieldAbsoluteDate<Complex> date = FieldAbsoluteDate.getArbitraryEpoch(field);
        final Frame mockedFrame = Mockito.mock(Frame.class);
        final AttitudeProviderModifier mockedModifier = Mockito.mock(AttitudeProviderModifier.class);
        Mockito.when(mockedModifier.getUnderlyingAttitudeProvider()).thenReturn(attitudeProvider);
        Mockito.when(mockedModifier.getAttitude(mockedPVCoordinatesProvider, date, mockedFrame)).thenCallRealMethod();
        // WHEN
        final FieldAttitude<Complex> attitude = mockedModifier.getAttitude(mockedPVCoordinatesProvider, date,
                mockedFrame);
        // THEN
        final Rotation actualRotation = attitude.getRotation().toRotation();
        Assertions.assertEquals(0., Rotation.distance(expectedRotation, actualRotation));
        Assertions.assertEquals(FieldVector3D.getZero(field), attitude.getRotationAcceleration());
        Assertions.assertEquals(FieldVector3D.getZero(field), attitude.getSpin());
    }

    @Test
    void testGetFrozenAttitudeProviderEventDetectors() {
        // GIVEN
        final AttitudeProvider attitudeProvider = new TestProvider(Rotation.IDENTITY);
        final List<ParameterDriver> drivers = new ArrayList<>();
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
        final Rotation expectedRotation = new Rotation(Vector3D.MINUS_I, Vector3D.MINUS_K);
        final AttitudeProvider attitudeProvider = new TestProvider(expectedRotation);
        final PVCoordinatesProvider mockedPVCoordinatesProvider = Mockito.mock(PVCoordinatesProvider.class);
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final Frame mockedFrame = Mockito.mock(Frame.class);
        // WHEN
        final AttitudeProviderModifier frozenAttitudeProvider = AttitudeProviderModifier.getFrozenAttitudeProvider(attitudeProvider);
        final Attitude attitude = frozenAttitudeProvider.getAttitude(mockedPVCoordinatesProvider, date, mockedFrame);
        // THEN
        Assertions.assertEquals(attitudeProvider, frozenAttitudeProvider.getUnderlyingAttitudeProvider());
        final Rotation actualRotation = attitude.getRotation();
        Assertions.assertEquals(0., Rotation.distance(expectedRotation, actualRotation));
        Assertions.assertEquals(Vector3D.ZERO, attitude.getRotationAcceleration());
        Assertions.assertEquals(Vector3D.ZERO, attitude.getSpin());
        final Rotation rotation = frozenAttitudeProvider.getAttitudeRotation(mockedPVCoordinatesProvider, date, mockedFrame);
        Assertions.assertEquals(0., Rotation.distance(rotation, actualRotation));
    }

    @SuppressWarnings("unchecked")
    @Test
    void testGetFrozenAttitudeProviderField() {
        // GIVEN
        final Rotation expectedRotation = new Rotation(Vector3D.MINUS_I, Vector3D.MINUS_K);
        final AttitudeProvider attitudeProvider = new TestProvider(expectedRotation);
        final ComplexField field = ComplexField.getInstance();
        final FieldPVCoordinatesProvider<Complex> mockedPVCoordinatesProvider = Mockito.mock(FieldPVCoordinatesProvider.class);
        final FieldAbsoluteDate<Complex> date = FieldAbsoluteDate.getArbitraryEpoch(field);
        final Frame mockedFrame = Mockito.mock(Frame.class);
        // WHEN
        final AttitudeProvider frozenAttitudeProvider = AttitudeProviderModifier.getFrozenAttitudeProvider(attitudeProvider);
        final FieldAttitude<Complex> attitude = frozenAttitudeProvider.getAttitude(mockedPVCoordinatesProvider, date,
                mockedFrame);
        // THEN
        final Rotation actualRotation = attitude.getRotation().toRotation();
        Assertions.assertEquals(0., Rotation.distance(expectedRotation, actualRotation));
        Assertions.assertEquals(FieldVector3D.getZero(field), attitude.getRotationAcceleration());
        Assertions.assertEquals(FieldVector3D.getZero(field), attitude.getSpin());
        final Rotation rotation = frozenAttitudeProvider.getAttitudeRotation(mockedPVCoordinatesProvider, date,
                mockedFrame).toRotation();
        Assertions.assertEquals(0., Rotation.distance(rotation, actualRotation));
    }

    @Test
    void testGetFrozenAttitudeProviderFieldEventDetectors() {
        // GIVEN
        final AttitudeProvider attitudeProvider = new TestProvider(Rotation.IDENTITY);
        final ComplexField field = ComplexField.getInstance();
        final List<ParameterDriver> driverList = new ArrayList<>();
        // WHEN
        final AttitudeProvider frozenAttitudeProvider = AttitudeProviderModifier.getFrozenAttitudeProvider(attitudeProvider);
        // THEN
        Assertions.assertEquals(attitudeProvider.getFieldEventDetectors(field).count(),
                frozenAttitudeProvider.getEventDetectors().count());
        Assertions.assertEquals(attitudeProvider.getFieldEventDetectors(field, driverList).count(),
                frozenAttitudeProvider.getFieldEventDetectors(field, driverList).count());
    }

    @Test
    void testGetParametersDrivers() {
        // GIVEN
        final AttitudeProvider mockedProvider = Mockito.mock(AttitudeProvider.class);
        final List<ParameterDriver> expectedDrivers = new ArrayList<>();
        expectedDrivers.add(Mockito.mock(ParameterDriver.class));
        Mockito.when(mockedProvider.getParametersDrivers()).thenReturn(expectedDrivers);
        final AttitudeProviderModifier mockedProviderModifier = Mockito.mock(AttitudeProviderModifier.class);
        Mockito.when(mockedProviderModifier.getUnderlyingAttitudeProvider()).thenReturn(mockedProvider);
        Mockito.when(mockedProviderModifier.getParametersDrivers()).thenCallRealMethod();
        // WHEN
        final List<ParameterDriver> actualDrivers = mockedProviderModifier.getParametersDrivers();
        // THEN
        Assertions.assertEquals(expectedDrivers.size(), actualDrivers.size());
    }

    @Test
    void testGetFrozenAttitudeProviderGetParametersDrivers() {
        // GIVEN
        final AttitudeProvider attitudeProvider = new TestProvider(Rotation.IDENTITY);
        final AttitudeProviderModifier frozenAttitudeProvider = AttitudeProviderModifier.getFrozenAttitudeProvider(attitudeProvider);
        // WHEN
        final List<ParameterDriver> drivers = frozenAttitudeProvider.getParametersDrivers();
        // THEN
        Assertions.assertEquals(attitudeProvider.getParametersDrivers().size(), drivers.size());
    }

    private static class TestProvider implements AttitudeProvider {

        private final Rotation r;
        TestProvider(final Rotation r) {
            this.r = r;
        }

        @Override
        public Attitude getAttitude(final PVCoordinatesProvider pvProv, final AbsoluteDate date, final Frame frame) {
            return new Attitude(frame, new TimeStampedAngularCoordinates(date, r, Vector3D.ZERO, Vector3D.ZERO));
        }

        @Override
        public <T extends CalculusFieldElement<T>> FieldAttitude<T> getAttitude(
            final FieldPVCoordinatesProvider<T> pvProv, final FieldAbsoluteDate<T> date, final Frame frame) {
            return new FieldAttitude<>(frame,
                                       new TimeStampedFieldAngularCoordinates<>(date,
                                                                                new FieldRotation<>(date.getField(), r),
                                                                                FieldVector3D.getZero(date.getField()),
                                                                                FieldVector3D.getZero(date.getField())));
        }

    }

}
