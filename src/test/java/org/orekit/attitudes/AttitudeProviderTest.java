/* Copyright 2022-2023 Romain Serra
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
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.AngularCoordinates;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinatesProvider;

class AttitudeProviderTest {

    private static final Frame REFERENCE_FRAME = FramesFactory.getGCRF();

    private static class TestAttitudeProvider implements AttitudeProvider {

        @Override
        public Attitude getAttitude(PVCoordinatesProvider pvProv, AbsoluteDate date, Frame frame) {
            return new Attitude(date, frame, new AngularCoordinates());
        }

        @Override
        public <T extends CalculusFieldElement<T>> FieldAttitude<T> getAttitude(FieldPVCoordinatesProvider<T> pvProv, FieldAbsoluteDate<T> date, Frame frame) {
            return new FieldAttitude<T>(date.getField(), new Attitude(date.toAbsoluteDate(), frame, new AngularCoordinates()));
        }
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