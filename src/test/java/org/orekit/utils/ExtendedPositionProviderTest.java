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

package org.orekit.utils;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.complex.Complex;
import org.hipparchus.complex.ComplexField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;

class ExtendedPositionProviderTest {

    @Test
    void testGetPVCoordinates() {
        // GIVEN
        final TestExtendedPositionProvider positionProvider = new TestExtendedPositionProvider();
        final Frame frame = Mockito.mock(Frame.class);
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        // WHEN
        final TimeStampedPVCoordinates pvCoordinates = positionProvider.getPVCoordinates(date, frame);
        // THEN
        final Vector3D expectedPosition = positionProvider.getPosition(date, frame);
        Assertions.assertEquals(expectedPosition, pvCoordinates.getPosition());
        Assertions.assertEquals(1., pvCoordinates.getVelocity().getX());
        Assertions.assertEquals(0., pvCoordinates.getAcceleration().getNorm());
    }

    @Test
    void testGetPVCoordinatesField() {
        // GIVEN
        final TestExtendedPositionProvider positionProvider = new TestExtendedPositionProvider();
        final Frame frame = Mockito.mock(Frame.class);
        final FieldAbsoluteDate<Complex> date = FieldAbsoluteDate.getJ2000Epoch(ComplexField.getInstance());
        // WHEN
        final TimeStampedFieldPVCoordinates<Complex> pvCoordinates = positionProvider.getPVCoordinates(date, frame);
        // THEN
        final FieldVector3D<Complex> expectedPosition = positionProvider.getPosition(date, frame);
        Assertions.assertEquals(expectedPosition, pvCoordinates.getPosition());
        Assertions.assertEquals(1., pvCoordinates.getVelocity().getX().getReal());
        Assertions.assertEquals(0., pvCoordinates.getAcceleration().getNorm().getReal());
    }

    @Test
    void testToFieldPVCoordinatesProvider() {
        // GIVEN
        final TestExtendedPositionProvider positionProvider = new TestExtendedPositionProvider();
        final Frame frame = Mockito.mock(Frame.class);
        final FieldAbsoluteDate<Complex> date = FieldAbsoluteDate.getJ2000Epoch(ComplexField.getInstance());
        // WHEN
        final FieldPVCoordinatesProvider<Complex> fieldPVCoordinatesProvider = positionProvider
            .toFieldPVCoordinatesProvider(ComplexField.getInstance());
        // THEN
        final FieldVector3D<Complex> expectedPosition = positionProvider.getPosition(date, frame);
        final FieldVector3D<Complex> actualPosition = fieldPVCoordinatesProvider.getPosition(date, frame);
        Assertions.assertEquals(expectedPosition, actualPosition);
        final FieldPVCoordinates<Complex> expectedPV = positionProvider.getPVCoordinates(date, frame);
        final FieldPVCoordinates<Complex> actualPV = fieldPVCoordinatesProvider.getPVCoordinates(date, frame);
        Assertions.assertEquals(expectedPV.getPosition(), actualPV.getPosition());
        Assertions.assertEquals(expectedPV.getVelocity(), actualPV.getVelocity());
        Assertions.assertEquals(expectedPV.getAcceleration(), actualPV.getAcceleration());
    }

    private static class TestExtendedPositionProvider implements ExtendedPositionProvider {

        private final AbsoluteDate referenceDate = AbsoluteDate.ARBITRARY_EPOCH;

        @Override
        public Vector3D getPosition(AbsoluteDate date, Frame frame) {
            final double shift = date.durationFrom(referenceDate);
            return new Vector3D(1. + shift, 2., 3.);
        }

        @Override
        public <T extends CalculusFieldElement<T>> FieldVector3D<T> getPosition(FieldAbsoluteDate<T> date, Frame frame) {
            final T zero = date.getField().getZero();
            final T shift = date.durationFrom(referenceDate);
            return new FieldVector3D<>(zero.newInstance(1.).add(shift), zero.newInstance(2.), zero.newInstance(3.));
        }
    }

}
