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
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.*;

class GroundPointingAttitudeModifierTest {

    @Test
    void testGetTargetPosition() {
        // GIVEN
        final GroundPointing mockedLaw = Mockito.mock(GroundPointing.class);
        final TestYawLaw law = new TestYawLaw(mockedLaw);
        final PVCoordinatesProvider mockedPVProvider = Mockito.mock(PVCoordinatesProvider.class);
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final Frame frame = Mockito.mock(Frame.class);
        final Vector3D expectedPosition = Vector3D.PLUS_J;
        Mockito.when(mockedLaw.getTargetPosition(mockedPVProvider, date, frame)).thenReturn(expectedPosition);
        // WHEN
        final Vector3D actualPosition = law.getTargetPosition(mockedPVProvider, date, frame);
        // THEN
        Assertions.assertEquals(expectedPosition, actualPosition);
    }

    @SuppressWarnings("unchecked")
    @Test
    void testGetTargetPositionField() {
        // GIVEN
        final GroundPointing mockedLaw = Mockito.mock(GroundPointing.class);
        final TestYawLaw law = new TestYawLaw(mockedLaw);
        final ComplexField field = ComplexField.getInstance();
        final FieldPVCoordinatesProvider<Complex> mockedPVProvider = Mockito.mock(FieldPVCoordinatesProvider.class);
        final FieldAbsoluteDate<Complex> date = new FieldAbsoluteDate<>(field, AbsoluteDate.ARBITRARY_EPOCH);
        final Frame frame = Mockito.mock(Frame.class);
        final FieldVector3D<Complex> expectedPosition = new FieldVector3D<>(field, Vector3D.PLUS_J);
        Mockito.when(mockedLaw.getTargetPosition(mockedPVProvider, date, frame)).thenReturn(expectedPosition);
        // WHEN
        final FieldVector3D<Complex> actualPosition = law.getTargetPosition(mockedPVProvider, date, frame);
        // THEN
        Assertions.assertEquals(expectedPosition, actualPosition);
    }

    @Test
    void testGetTargetPV() {
        // GIVEN
        final GroundPointing mockedLaw = Mockito.mock(GroundPointing.class);
        final TestYawLaw law = new TestYawLaw(mockedLaw);
        final PVCoordinatesProvider mockedPVProvider = Mockito.mock(PVCoordinatesProvider.class);
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final Frame frame = Mockito.mock(Frame.class);
        final TimeStampedPVCoordinates expectedPvCoordinates = new TimeStampedPVCoordinates(date, new PVCoordinates(Vector3D.MINUS_I,
                Vector3D.PLUS_J));
        Mockito.when(mockedLaw.getTargetPV(mockedPVProvider, date, frame)).thenReturn(expectedPvCoordinates);
        // WHEN
        final TimeStampedPVCoordinates actualPVCoordinates = law.getTargetPV(mockedPVProvider, date, frame);
        // THEN
        Assertions.assertEquals(expectedPvCoordinates.getPosition(), actualPVCoordinates.getPosition());
        Assertions.assertEquals(expectedPvCoordinates.getVelocity(), actualPVCoordinates.getVelocity());
    }

    @SuppressWarnings("unchecked")
    @Test
    void testGetTargetPVField() {
        // GIVEN
        final GroundPointing mockedLaw = Mockito.mock(GroundPointing.class);
        final TestYawLaw law = new TestYawLaw(mockedLaw);
        final ComplexField field = ComplexField.getInstance();
        final FieldPVCoordinatesProvider<Complex> mockedPVProvider = Mockito.mock(FieldPVCoordinatesProvider.class);
        final FieldAbsoluteDate<Complex> date = new FieldAbsoluteDate<>(field, AbsoluteDate.ARBITRARY_EPOCH);
        final Frame frame = Mockito.mock(Frame.class);
        final TimeStampedFieldPVCoordinates<Complex> expectedPV = new TimeStampedFieldPVCoordinates<>(field,
                new TimeStampedPVCoordinates(date.toAbsoluteDate(), Vector3D.MINUS_I, Vector3D.MINUS_K, Vector3D.ZERO));
        Mockito.when(mockedLaw.getTargetPV(mockedPVProvider, date, frame)).thenReturn(expectedPV);
        // WHEN
        final TimeStampedFieldPVCoordinates<Complex> actualPV = law.getTargetPV(mockedPVProvider, date, frame);
        // THEN
        Assertions.assertEquals(expectedPV.getPosition(), actualPV.getPosition());
        Assertions.assertEquals(expectedPV.getVelocity(), actualPV.getVelocity());
    }

    @Test
    void testGetBaseState() {
        // GIVEN
        final GroundPointing mockedLaw = Mockito.mock(GroundPointing.class);
        final TestYawLaw law = new TestYawLaw(mockedLaw);
        final PVCoordinatesProvider mockedPVProvider = Mockito.mock(PVCoordinatesProvider.class);
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final Frame frame = Mockito.mock(Frame.class);
        final Attitude mockedAttitude = Mockito.mock(Attitude.class);
        Mockito.when(mockedLaw.getAttitude(mockedPVProvider, date, frame)).thenReturn(mockedAttitude);
        // WHEN
        final Attitude attitude = law.getBaseState(mockedPVProvider, date, frame);
        // THEN
        Assertions.assertEquals(mockedAttitude, attitude);
    }

    @SuppressWarnings("unchecked")
    @Test
    void testGetBaseStateField() {
        // GIVEN
        final GroundPointing mockedLaw = Mockito.mock(GroundPointing.class);
        final TestYawLaw law = new TestYawLaw(mockedLaw);
        final ComplexField field = ComplexField.getInstance();
        final FieldPVCoordinatesProvider<Complex> mockedPVProvider = Mockito.mock(FieldPVCoordinatesProvider.class);
        final FieldAbsoluteDate<Complex> date = new FieldAbsoluteDate<>(field, AbsoluteDate.ARBITRARY_EPOCH);
        final Frame frame = Mockito.mock(Frame.class);
        final FieldAttitude<Complex> mockedAttitude = Mockito.mock(FieldAttitude.class);
        Mockito.when(mockedLaw.getAttitude(mockedPVProvider, date, frame)).thenReturn(mockedAttitude);
        // WHEN
        final FieldAttitude<Complex> attitude = law.getBaseState(mockedPVProvider, date, frame);
        // THEN
        Assertions.assertEquals(mockedAttitude, attitude);
    }

    private static class TestYawLaw extends GroundPointingAttitudeModifier {

        protected TestYawLaw(final GroundPointing groundPointingLaw) {
            super(FramesFactory.getEME2000(), Mockito.mock(Frame.class), groundPointingLaw);
        }

    }

}
