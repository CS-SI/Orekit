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
package org.orekit.models.earth.atmosphere;

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
import org.orekit.utils.ExtendedPositionProvider;

class AbstractSunInfluencedAtmosphereTest {

    @Test
    void testGetSunPosition() {
        // GIVEN
        final ExtendedPositionProvider provider = new TestProvider();
        final TestAtmosphere testAtmosphere = new TestAtmosphere(provider);
        final Frame mockedFrame = Mockito.mock(Frame.class);
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final FieldAbsoluteDate<Complex> fieldDate = new FieldAbsoluteDate<>(ComplexField.getInstance(), date).shiftedBy(Complex.I);
        // WHEN
        final FieldVector3D<Complex> fieldSunPosition = testAtmosphere.getSunPosition(fieldDate, mockedFrame);
        // THEN
        final Vector3D sunPosition = testAtmosphere.getSunPosition(date, mockedFrame);
        Assertions.assertEquals(sunPosition, fieldSunPosition.toVector3D());
        Assertions.assertNotEquals(0., fieldSunPosition.getX().getImaginary());
    }

    @Test
    void testGetSun() {
        // GIVEN
        final ExtendedPositionProvider mockedProvider = Mockito.mock(ExtendedPositionProvider.class);
        final TestAtmosphere testAtmosphere = new TestAtmosphere(mockedProvider);
        // WHEN
        final ExtendedPositionProvider sun = testAtmosphere.getSun();
        // THEN
        Assertions.assertEquals(mockedProvider, sun);
    }

    private static class TestProvider implements ExtendedPositionProvider {

        @Override
        public <T extends CalculusFieldElement<T>> FieldVector3D<T> getPosition(FieldAbsoluteDate<T> date, Frame frame) {
            final T zero = date.getField().getZero();
            return new FieldVector3D<>(date.durationFrom(AbsoluteDate.ARBITRARY_EPOCH), zero, zero);
        }
    }

    private static class TestAtmosphere extends AbstractSunInfluencedAtmosphere {

        public TestAtmosphere(final ExtendedPositionProvider provider) {
            super(provider);
        }

        @Override
        public Frame getFrame() {
            return null;
        }

        @Override
        public double getDensity(AbsoluteDate date, Vector3D position, Frame frame) {
            return 0;
        }

        @Override
        public <T extends CalculusFieldElement<T>> T getDensity(FieldAbsoluteDate<T> date, FieldVector3D<T> position, Frame frame) {
            return null;
        }
    }
}
