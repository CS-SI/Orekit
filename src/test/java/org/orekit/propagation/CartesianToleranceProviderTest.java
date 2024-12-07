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
package org.orekit.propagation;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.AbsolutePVCoordinates;
import org.orekit.utils.FieldAbsolutePVCoordinates;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.util.Arrays;

class CartesianToleranceProviderTest {

    @Test
    void testOfdP() {
        // GIVEN
        final double dP = 1.;
        final CartesianToleranceProvider toleranceProvider = CartesianToleranceProvider.of(dP);
        // WHEN
        final double[][] tolerances = toleranceProvider.getTolerances(Mockito.mock(Vector3D.class),
                Mockito.mock(Vector3D.class));
        // THEN
        Assertions.assertEquals(dP, tolerances[0][0]);
        Assertions.assertEquals(dP, tolerances[0][1]);
        Assertions.assertEquals(dP, tolerances[0][2]);
        Assertions.assertEquals(CartesianToleranceProvider.DEFAULT_ABSOLUTE_MASS_TOLERANCE, tolerances[0][6]);
        for (int i = 0; i < tolerances[0].length; ++i) {
            Assertions.assertTrue(tolerances[1][i] > 0.);
        }
    }

    @Test
    void testOfdPdVdM() {
        // GIVEN
        final double dP = 1.;
        final double dV = 2.;
        final double dM = 3.;
        final CartesianToleranceProvider toleranceProvider = CartesianToleranceProvider.of(dP, dV, dM);
        final Vector3D unitVector = Vector3D.MINUS_I;
        // WHEN
        final double[][] tolerances = toleranceProvider.getTolerances(unitVector, unitVector);
        // THEN
        Assertions.assertEquals(dP, tolerances[0][0]);
        Assertions.assertEquals(dP, tolerances[0][1]);
        Assertions.assertEquals(dP, tolerances[0][2]);
        Assertions.assertEquals(dV, tolerances[0][3]);
        Assertions.assertEquals(dV, tolerances[0][4]);
        Assertions.assertEquals(dV, tolerances[0][5]);
        Assertions.assertEquals(dM, tolerances[0][6]);
        for (int i = 0; i < 6; ++i) {
            Assertions.assertEquals(tolerances[1][i], tolerances[0][i]);
        }
        Assertions.assertTrue(tolerances[1][6] > 0.);
    }

    @Test
    void testGetTolerancesCartesianOrbit() {
        // GIVEN
        final double[] absoluteTolerances = new double[7];
        Arrays.fill(absoluteTolerances, 1.);
        final double[] relativeTolerances = new double[7];
        Arrays.fill(relativeTolerances, 2.);
        final CartesianToleranceProvider mockedProvider = new TestProvider(absoluteTolerances, relativeTolerances);
        final CartesianOrbit mockedOrbit = mockOrbit();
        // WHEN
        final double[][] actualTolerances = mockedProvider.getTolerances(mockedOrbit);
        // THEN
        Assertions.assertArrayEquals(absoluteTolerances, actualTolerances[0]);
        Assertions.assertArrayEquals(relativeTolerances, actualTolerances[1]);
    }

    @Test
    void testGetTolerancesFieldCartesianOrbit() {
        // GIVEN
        final double[] absoluteTolerances = new double[7];
        Arrays.fill(absoluteTolerances, 1.);
        final double[] relativeTolerances = new double[7];
        Arrays.fill(relativeTolerances, 2.);
        final CartesianToleranceProvider mockedProvider = new TestProvider(absoluteTolerances, relativeTolerances);
        final FieldCartesianOrbit<Binary64> mockedOrbit = mockFieldOrbit();
        // WHEN
        final double[][] actualTolerances = mockedProvider.getTolerances(mockedOrbit);
        // THEN
        Assertions.assertArrayEquals(absoluteTolerances, actualTolerances[0]);
        Assertions.assertArrayEquals(relativeTolerances, actualTolerances[1]);
    }

    private static CartesianOrbit mockOrbit() {
        final CartesianOrbit mockedOrbit = Mockito.mock(CartesianOrbit.class);
        Mockito.when(mockedOrbit.getPosition()).thenReturn(Mockito.mock(Vector3D.class));
        Mockito.when(mockedOrbit.getPVCoordinates()).thenReturn(new TimeStampedPVCoordinates(AbsoluteDate.ARBITRARY_EPOCH, new PVCoordinates()));
        return mockedOrbit;
    }

    @SuppressWarnings("unchecked")
    private static <T extends CalculusFieldElement<T>> FieldCartesianOrbit<T> mockFieldOrbit() {
        final FieldCartesianOrbit<T> mockedFieldOrbit = Mockito.mock(FieldCartesianOrbit.class);
        final CartesianOrbit mockedOrbit = mockOrbit();
        Mockito.when(mockedFieldOrbit.toOrbit()).thenReturn(mockedOrbit);
        return mockedFieldOrbit;
    }

    @Test
    void testGetTolerancesAbsolutePV() {
        // GIVEN
        final double[] absoluteTolerances = new double[7];
        Arrays.fill(absoluteTolerances, 1.);
        final double[] relativeTolerances = new double[7];
        Arrays.fill(relativeTolerances, 2.);
        final CartesianToleranceProvider mockedProvider = new TestProvider(absoluteTolerances, relativeTolerances);
        final AbsolutePVCoordinates mockedPV = mockPV();
        // WHEN
        final double[][] actualTolerances = mockedProvider.getTolerances(mockedPV);
        // THEN
        Assertions.assertArrayEquals(absoluteTolerances, actualTolerances[0]);
        Assertions.assertArrayEquals(relativeTolerances, actualTolerances[1]);
    }

    @Test
    void testGetTolerancesFieldPV() {
        // GIVEN
        final double[] absoluteTolerances = new double[7];
        Arrays.fill(absoluteTolerances, 1.);
        final double[] relativeTolerances = new double[7];
        Arrays.fill(relativeTolerances, 2.);
        final CartesianToleranceProvider mockedProvider = new TestProvider(absoluteTolerances, relativeTolerances);
        final FieldAbsolutePVCoordinates<Binary64> mockedFieldPV = mockFieldPV();
        // WHEN
        final double[][] actualTolerances = mockedProvider.getTolerances(mockedFieldPV);
        // THEN
        Assertions.assertArrayEquals(absoluteTolerances, actualTolerances[0]);
        Assertions.assertArrayEquals(relativeTolerances, actualTolerances[1]);
    }

    private static AbsolutePVCoordinates mockPV() {
        final AbsolutePVCoordinates mockedPV = Mockito.mock(AbsolutePVCoordinates.class);
        Mockito.when(mockedPV.getPosition()).thenReturn(Mockito.mock(Vector3D.class));
        Mockito.when(mockedPV.getPVCoordinates()).thenReturn(new TimeStampedPVCoordinates(AbsoluteDate.ARBITRARY_EPOCH, new PVCoordinates()));
        return mockedPV;
    }

    @SuppressWarnings("unchecked")
    private static <T extends CalculusFieldElement<T>> FieldAbsolutePVCoordinates<T> mockFieldPV() {
        final FieldAbsolutePVCoordinates<T> mockedFieldPV = Mockito.mock(FieldAbsolutePVCoordinates.class);
        final AbsolutePVCoordinates mockedPV = mockPV();
        Mockito.when(mockedFieldPV.toAbsolutePVCoordinates()).thenReturn(mockedPV);
        return mockedFieldPV;
    }

    private static class TestProvider implements CartesianToleranceProvider {
        private final double[] absoluteTolerances;
        private final double[] relativeTolerances;
        TestProvider(final double[] absoluteTolerances, final double[] relativeTolerances) {
            this.absoluteTolerances = absoluteTolerances;
            this.relativeTolerances = relativeTolerances;
        }
        @Override
        public double[][] getTolerances(final Vector3D position, final Vector3D velocity) {
            return new double[][] {
                absoluteTolerances, relativeTolerances
            };
        }
    }

}
