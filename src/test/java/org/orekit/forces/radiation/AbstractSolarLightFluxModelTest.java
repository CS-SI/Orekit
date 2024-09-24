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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.complex.Complex;
import org.hipparchus.complex.ComplexField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.frames.Frame;
import org.orekit.propagation.events.*;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ExtendedPositionProvider;

import java.util.Collections;
import java.util.List;

class AbstractSolarLightFluxModelTest {

    @Test
    void testGetters() {
        // GIVEN
        final double expectedRadius = 0.5;
        final ExtendedPositionProvider sun = Mockito.mock(ExtendedPositionProvider.class);
        // WHEN
        final TestSolarFlux model = new TestSolarFlux(Double.NaN, sun, expectedRadius);
        // THEN
        Assertions.assertEquals(expectedRadius, model.getOccultingBodyRadius());
        Assertions.assertEquals(sun, model.getOccultedBody());
    }

    @Test
    void testGetUnoccultedFluxVector() {
        // GIVEN
        final ComplexField field = ComplexField.getInstance();
        final double occultingBodyRadius = 0.5;
        final FieldVector3D<Complex> sunPosition = FieldVector3D.getPlusI(field).scalarMultiply(10.);
        final ExtendedPositionProvider sun = mockFieldProvider(sunPosition);
        final TestSolarFlux model = new TestSolarFlux(Double.NaN, sun, occultingBodyRadius);
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

    private static class TestSolarFlux extends AbstractSolarLightFluxModel {

        protected TestSolarFlux(double kRef, ExtendedPositionProvider occultedBody, double occultingBodyRadius) {
            super(kRef, occultedBody, occultingBodyRadius, EventDetectionSettings.getDefaultEventDetectionSettings());
        }

        @Override
        protected double getLightingRatio(Vector3D position, Vector3D occultedBodyPosition) {
            return 0;
        }

        @Override
        protected <T extends CalculusFieldElement<T>> T getLightingRatio(FieldVector3D<T> position, FieldVector3D<T> occultedBodyPosition) {
            return null;
        }

        @Override
        public List<EventDetector> getEclipseConditionsDetector() {
            return Collections.emptyList();
        }

        @Override
        public <T extends CalculusFieldElement<T>> List<FieldEventDetector<T>> getFieldEclipseConditionsDetector(Field<T> field) {
            return Collections.emptyList();
        }
    }

}
