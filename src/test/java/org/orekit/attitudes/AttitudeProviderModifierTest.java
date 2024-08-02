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
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinatesProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AttitudeProviderModifierTest {

    @Test
    void testGetFrozenAttitudeProvider() {
        // GIVEN
        final AttitudeProvider attitudeProvider = Mockito.mock(AttitudeProvider.class);
        final Rotation expectedRotation = new Rotation(Vector3D.MINUS_I, Vector3D.MINUS_K);
        final PVCoordinatesProvider mockedPVCoordinatesProvider = Mockito.mock(PVCoordinatesProvider.class);
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final Frame mockedFrame = Mockito.mock(Frame.class);
        Mockito.when(attitudeProvider.getAttitudeRotation(mockedPVCoordinatesProvider, date, mockedFrame)).thenReturn(expectedRotation);
        // WHEN
        final AttitudeProviderModifier frozenAttitudeProvider = AttitudeProviderModifier.getFrozenAttitudeProvider(attitudeProvider);
        final Attitude attitude = frozenAttitudeProvider.getAttitude(mockedPVCoordinatesProvider, date, mockedFrame);
        // THEN
        assertEquals(attitudeProvider, frozenAttitudeProvider.getUnderlyingAttitudeProvider());
        final Rotation actualRotation = attitude.getRotation();
        assertEquals(0., Rotation.distance(expectedRotation, actualRotation));
        assertEquals(Vector3D.ZERO, attitude.getRotationAcceleration());
        assertEquals(Vector3D.ZERO, attitude.getSpin());
    }

    @SuppressWarnings("unchecked")
    @Test
    void testGetFrozenAttitudeProviderField() {
        // GIVEN
        final AttitudeProvider attitudeProvider = Mockito.mock(AttitudeProvider.class);
        final Rotation expectedRotation = new Rotation(Vector3D.MINUS_I, Vector3D.MINUS_K);
        final ComplexField field = ComplexField.getInstance();
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
        assertEquals(0., Rotation.distance(expectedRotation, actualRotation));
        assertEquals(FieldVector3D.getZero(field), attitude.getRotationAcceleration());
        assertEquals(FieldVector3D.getZero(field), attitude.getSpin());
    }

}
