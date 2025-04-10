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
package org.orekit.attitudes;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.UnivariateDerivative1;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

class AttitudeProviderTest {

    private static final Frame REFERENCE_FRAME = FramesFactory.getGCRF();

    private static class TestAttitudeProvider implements AttitudeProvider {
        private static final AngularCoordinates ANGULAR_COORDINATES = new AngularCoordinates(new Rotation(Vector3D.MINUS_I, Vector3D.MINUS_K));

        @Override
        public Attitude getAttitude(PVCoordinatesProvider pvProv, AbsoluteDate date, Frame frame) {
            return new Attitude(date, frame, ANGULAR_COORDINATES);
        }

        @Override
        public <T extends CalculusFieldElement<T>> FieldAttitude<T> getAttitude(FieldPVCoordinatesProvider<T> pvProv, FieldAbsoluteDate<T> date, Frame frame) {
            return new FieldAttitude<>(date, frame, new FieldAngularCoordinates<>(date.getField(), ANGULAR_COORDINATES));
        }
    }

    @Test
    void testGetAttitudeRotationModelField() {
        // GIVEN
        final TestAttitudeProvider attitudeProvider = new TestAttitudeProvider();
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final SpacecraftState state = new SpacecraftState(new CartesianOrbit(new PVCoordinates(Vector3D.MINUS_J, Vector3D.MINUS_K),
                FramesFactory.getEME2000(), date, 1.));
        final Binary64Field field = Binary64Field.getInstance();
        final FieldSpacecraftState<Binary64> fieldState = new FieldSpacecraftState<>(field, state);
        // WHEN
        final FieldRotation<Binary64> actualRotation = attitudeProvider.getAttitudeRotation(fieldState, null);
        // THEN
        final FieldRotation<Binary64> expectedRotation = attitudeProvider.getAttitudeRotation(fieldState.getOrbit(), fieldState.getDate(),
                state.getFrame());
        Assertions.assertEquals(0., Rotation.distance(expectedRotation.toRotation(), actualRotation.toRotation()));
    }

    @Test
    void testGetAttitudeRotationModelFieldAbsolutePV() {
        // GIVEN
        final TestAttitudeProvider attitudeProvider = new TestAttitudeProvider();
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final SpacecraftState state = new SpacecraftState(new AbsolutePVCoordinates(FramesFactory.getEME2000(), date,
                new PVCoordinates(Vector3D.MINUS_J, Vector3D.MINUS_K)));
        final Binary64Field field = Binary64Field.getInstance();
        final FieldSpacecraftState<Binary64> fieldState = new FieldSpacecraftState<>(field, state);
        // WHEN
        final FieldRotation<Binary64> actualRotation = attitudeProvider.getAttitudeRotation(fieldState, null);
        // THEN
        final FieldRotation<Binary64> expectedRotation = attitudeProvider.getAttitudeRotation(fieldState.getAbsPVA(), fieldState.getDate(),
                state.getFrame());
        Assertions.assertEquals(0., Rotation.distance(expectedRotation.toRotation(), actualRotation.toRotation()));
    }

    @Test
    void testGetAttitudeRotationModel() {
        // GIVEN
        final TestAttitudeProvider attitudeProvider = new TestAttitudeProvider();
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final SpacecraftState state = new SpacecraftState(new CartesianOrbit(new PVCoordinates(Vector3D.MINUS_J, Vector3D.MINUS_K),
                FramesFactory.getEME2000(), date, 1.));
        // WHEN
        final Rotation actualRotation = attitudeProvider.getAttitudeRotation(state, new double[0]);
        // THEN
        final Rotation expectedRotation = attitudeProvider.getAttitudeRotation(state.getOrbit(), date, state.getFrame());
        Assertions.assertEquals(0., Rotation.distance(expectedRotation, actualRotation));
    }

    @Test
    void testGetAttitudeRotationModelAbsolutePV() {
        // GIVEN
        final TestAttitudeProvider attitudeProvider = new TestAttitudeProvider();
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final SpacecraftState state = new SpacecraftState(new AbsolutePVCoordinates(FramesFactory.getEME2000(), date,
                new PVCoordinates(Vector3D.MINUS_J, Vector3D.MINUS_K)));
        // WHEN
        final Rotation actualRotation = attitudeProvider.getAttitudeRotation(state, new double[0]);
        // THEN
        final Rotation expectedRotation = attitudeProvider.getAttitudeRotation(state.getAbsPVA(), date, state.getFrame());
        Assertions.assertEquals(0., Rotation.distance(expectedRotation, actualRotation));
    }

    @Test
    void testGetAttitudeRotation() {
        // GIVEN
        final TestAttitudeProvider attitudeProvider = new TestAttitudeProvider();
        final PVCoordinatesProvider mockPvCoordinatesProvider = Mockito.mock(PVCoordinatesProvider.class);
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        // WHEN
        final Rotation actualRotation = attitudeProvider.getAttitudeRotation(mockPvCoordinatesProvider, date, REFERENCE_FRAME);
        // THEN
        final Rotation expectedRotation = attitudeProvider.getAttitude(mockPvCoordinatesProvider, date, REFERENCE_FRAME)
                .getRotation();
        Assertions.assertEquals(0., Rotation.distance(expectedRotation, actualRotation));
    }

    @Test
    void testGetEventDetectors() {
        // GIVEN
        final AttitudeProvider mockedProvider = Mockito.mock(AttitudeProvider.class);
        Mockito.when(mockedProvider.getParametersDrivers()).thenReturn(new ArrayList<>());
        // WHEN
        final Stream<EventDetector> detectorStream = mockedProvider.getEventDetectors();
        // THEN
        Assertions.assertEquals(0, detectorStream.count());
    }

    @Test
    void testGetParametersDrivers() {
        // GIVEN
        final AttitudeProvider mockedProvider = new TestAttitudeProvider();
        // WHEN
        final List<ParameterDriver> driverList = mockedProvider.getParametersDrivers();
        // THEN
        Assertions.assertTrue(driverList.isEmpty());
    }

    @Test
    void testGetAttitudeRotationFieldBinary64() {
        templateTestGetRotationField(Binary64Field.getInstance());
    }

    @Test
    void testGetAttitudeRotationFieldUnivariateDerivative1() {
        templateTestGetRotationField(new UnivariateDerivative1(0., 0.).getField());
    }

    <T extends CalculusFieldElement<T>> void templateTestGetRotationField(final Field<T> field) {
        // GIVEN
        final TestAttitudeProvider attitudeProvider = new TestAttitudeProvider();
        @SuppressWarnings("unchecked")
        final FieldPVCoordinatesProvider<T> mockPvCoordinatesProvider = Mockito.mock(FieldPVCoordinatesProvider.class);
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final FieldAbsoluteDate<T> fieldDate = new FieldAbsoluteDate<T>(field, date);
        // WHEN
        final FieldRotation<T> actualRotation = attitudeProvider.getAttitudeRotation(mockPvCoordinatesProvider, fieldDate, REFERENCE_FRAME);
        // THEN
        final FieldRotation<T> expectedRotation = attitudeProvider.getAttitude(mockPvCoordinatesProvider, fieldDate, REFERENCE_FRAME)
                .getRotation();
        Assertions.assertEquals(0., Rotation.distance(expectedRotation.toRotation(), actualRotation.toRotation()));
    }

}
