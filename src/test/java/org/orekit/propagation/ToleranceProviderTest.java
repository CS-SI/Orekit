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
package org.orekit.propagation;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.*;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.util.Arrays;

class ToleranceProviderTest {

    @Test
    void testGetDefaultProvider() {
        // GIVEN
        final double dP = 1.;
        // WHEN
        final ToleranceProvider toleranceProvider = ToleranceProvider.getDefaultToleranceProvider(dP);
        final double[][] actualTolerances = toleranceProvider.getTolerances(Vector3D.PLUS_I, Vector3D.MINUS_K);
        // THEN
        Assertions.assertEquals(2, actualTolerances.length);
        for (int i = 0; i < 6; i++) {
            Assertions.assertEquals(dP, actualTolerances[0][i]);
        }
    }

    @Test
    void testOfConstantsCartesian() {
        // GIVEN
        final double expectedAbsolute = 1.;
        final double expectedRelative = 2.;
        // WHEN
        final ToleranceProvider toleranceProvider = ToleranceProvider.of(expectedAbsolute, expectedRelative);
        final double[][] actualTolerances = toleranceProvider.getTolerances(Vector3D.ZERO, Vector3D.ZERO);
        // THEN
        Assertions.assertEquals(2, actualTolerances.length);
        Assertions.assertEquals(7, actualTolerances[0].length);
        Assertions.assertEquals(actualTolerances[1].length, actualTolerances[0].length);
        for (int i = 0; i < 7; i++) {
            Assertions.assertEquals(expectedAbsolute, actualTolerances[0][i]);
            Assertions.assertEquals(expectedRelative, actualTolerances[1][i]);
        }
    }

    @ParameterizedTest
    @EnumSource(OrbitType.class)
    void testOfConstantsOrbit(final OrbitType orbitType) {
        // GIVEN
        final double expectedAbsolute = 1.;
        final double expectedRelative = 2.;
        // WHEN
        final ToleranceProvider toleranceProvider = ToleranceProvider.of(expectedAbsolute, expectedRelative);
        final double[][] actualTolerances = toleranceProvider.getTolerances(Mockito.mock(Orbit.class), orbitType,
                PositionAngleType.MEAN);
        // THEN
        Assertions.assertEquals(2, actualTolerances.length);
        Assertions.assertEquals(7, actualTolerances[0].length);
        Assertions.assertEquals(actualTolerances[1].length, actualTolerances[0].length);
        for (int i = 0; i < 7; i++) {
            Assertions.assertEquals(expectedAbsolute, actualTolerances[0][i]);
            Assertions.assertEquals(expectedRelative, actualTolerances[1][i]);
        }
    }

    @ParameterizedTest
    @EnumSource(PositionAngleType.class)
    void testOfConstantsOrbit(final PositionAngleType positionAngleType) {
        // GIVEN
        final double expectedAbsolute = 1.;
        final double expectedRelative = 2.;
        // WHEN
        final ToleranceProvider toleranceProvider = ToleranceProvider.of(expectedAbsolute, expectedRelative);
        final double[][] actualTolerances = toleranceProvider.getTolerances(Mockito.mock(Orbit.class), OrbitType.EQUINOCTIAL,
                positionAngleType);
        // THEN
        Assertions.assertEquals(2, actualTolerances.length);
        Assertions.assertEquals(7, actualTolerances[0].length);
        Assertions.assertEquals(actualTolerances[1].length, actualTolerances[0].length);
        for (int i = 0; i < 7; i++) {
            Assertions.assertEquals(expectedAbsolute, actualTolerances[0][i]);
            Assertions.assertEquals(expectedRelative, actualTolerances[1][i]);
        }
    }

    @Test
    void testOfCartesianProviderVectors() {
        // GIVEN
        final double[] absoluteTolerances = new double[7];
        Arrays.fill(absoluteTolerances, 1.);
        final double[] relativeTolerances = new double[7];
        Arrays.fill(relativeTolerances, 2.);
        final Vector3D vector = Vector3D.ZERO;
        final CartesianToleranceProvider mockedProvider = Mockito.mock(CartesianToleranceProvider.class);
        Mockito.when(mockedProvider.getTolerances(vector, vector))
                .thenReturn(new double[][] {absoluteTolerances, relativeTolerances});
        // WHEN
        final ToleranceProvider toleranceProvider = ToleranceProvider.of(mockedProvider);
        final double[][] actualTolerances = toleranceProvider.getTolerances(vector, vector);
        // THEN
        Assertions.assertArrayEquals(absoluteTolerances, actualTolerances[0]);
        Assertions.assertArrayEquals(relativeTolerances, actualTolerances[1]);
    }

    @Test
    void testOfCartesianProviderCartesianOrbit() {
        // GIVEN
        final double[] absoluteTolerances = new double[7];
        Arrays.fill(absoluteTolerances, 1.);
        final double[] relativeTolerances = new double[7];
        Arrays.fill(relativeTolerances, 2.);
        final CartesianOrbit mockedOrbit = Mockito.mock(CartesianOrbit.class);
        final Vector3D vector = Vector3D.ZERO;
        final CartesianToleranceProvider mockedProvider = Mockito.mock(CartesianToleranceProvider.class);
        Mockito.when(mockedOrbit.getPosition()).thenReturn(vector);
        Mockito.when(mockedOrbit.getVelocity()).thenReturn(vector);
        Mockito.when(mockedOrbit.getPVCoordinates()).thenReturn(new TimeStampedPVCoordinates(AbsoluteDate.ARBITRARY_EPOCH, vector, vector));
        Mockito.when(mockedProvider.getTolerances(vector, vector))
                .thenReturn(new double[][] {absoluteTolerances, relativeTolerances});
        // WHEN
        final ToleranceProvider toleranceProvider = ToleranceProvider.of(mockedProvider);
        final double[][] actualTolerances = toleranceProvider.getTolerances(mockedOrbit, OrbitType.CARTESIAN, null);
        // THEN
        Assertions.assertArrayEquals(absoluteTolerances, actualTolerances[0]);
        Assertions.assertArrayEquals(relativeTolerances, actualTolerances[1]);
    }

    @ParameterizedTest
    @EnumSource(value=OrbitType.class, names = {"KEPLERIAN", "EQUINOCTIAL"})
    void testOfCartesianProviderOrbit(final OrbitType orbitType) {
        // GIVEN
        final double[] absoluteTolerances = new double[7];
        Arrays.fill(absoluteTolerances, 1.);
        final double[] relativeTolerances = new double[7];
        Arrays.fill(relativeTolerances, 2.);
        final Orbit orbit = getOrbit();
        final CartesianToleranceProvider mockedProvider = Mockito.mock(CartesianToleranceProvider.class);
        Mockito.when(mockedProvider.getTolerances(Mockito.any(Vector3D.class), Mockito.any(Vector3D.class)))
                .thenReturn(new double[][] {absoluteTolerances, relativeTolerances});
        final PositionAngleType angleType = PositionAngleType.TRUE;
        // WHEN
        final ToleranceProvider toleranceProvider = ToleranceProvider.of(mockedProvider);
        final double[][] actualTolerances = toleranceProvider.getTolerances(orbit, orbitType, angleType);
        // THEN
        final double[][] cartesianTolerances = toleranceProvider.getTolerances((CartesianOrbit) OrbitType.CARTESIAN.convertType(orbit));
        final double[] cartAbsTol = cartesianTolerances[0];
        final Orbit converted = orbitType.convertType(orbit);
        final double[][] jacobian = new double[6][6];
        converted.getJacobianWrtCartesian(angleType, jacobian);
        for (int i = 0; i < jacobian.length; ++i) {
            final double[] row = jacobian[i];
            final double expected = FastMath.abs(row[0]) * cartAbsTol[0] +
                    FastMath.abs(row[1]) * cartAbsTol[1] +
                    FastMath.abs(row[2]) * cartAbsTol[2] +
                    FastMath.abs(row[3]) * cartAbsTol[3] +
                    FastMath.abs(row[4]) * cartAbsTol[4] +
                    FastMath.abs(row[5]) * cartAbsTol[5];
            Assertions.assertEquals(expected, actualTolerances[0][i]);
        }
    }

    private static Orbit getOrbit() {
        return new KeplerianOrbit(1e7, 0.001, 1, 2, 3, 4, PositionAngleType.ECCENTRIC, FramesFactory.getGCRF(),
                AbsoluteDate.ARBITRARY_EPOCH, Constants.EGM96_EARTH_MU);
    }

}
