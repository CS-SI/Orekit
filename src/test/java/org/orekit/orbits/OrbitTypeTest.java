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
package org.orekit.orbits;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.complex.Complex;
import org.hipparchus.complex.ComplexField;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.TimeStampedPVCoordinates;


class OrbitTypeTest {

    @BeforeAll
    public static void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    void testCartesianIsPositionAngleBased() {
        // GIVEN
        final OrbitType orbitType = OrbitType.CARTESIAN;
        // WHEN
        final boolean actualIsPositionAngleBased = orbitType.isPositionAngleBased();
        // THEN
        Assertions.assertFalse(actualIsPositionAngleBased);
    }

    @Test
    void testEquinoctialIsPositionAngleBased() {
        // GIVEN
        final OrbitType orbitType = OrbitType.EQUINOCTIAL;
        // WHEN
        final boolean actualIsPositionAngleBased = orbitType.isPositionAngleBased();
        // THEN
        Assertions.assertTrue(actualIsPositionAngleBased);
    }

    @Test
    void testKeplerianIsPositionAngleBased() {
        // GIVEN
        final OrbitType orbitType = OrbitType.KEPLERIAN;
        // WHEN
        final boolean actualIsPositionAngleBased = orbitType.isPositionAngleBased();
        // THEN
        Assertions.assertTrue(actualIsPositionAngleBased);
    }

    @Test
    void testCircularIsPositionAngleBased() {
        // GIVEN
        final OrbitType orbitType = OrbitType.CIRCULAR;
        // WHEN
        final boolean actualIsPositionAngleBased = orbitType.isPositionAngleBased();
        // THEN
        Assertions.assertTrue(actualIsPositionAngleBased);
    }

    @Test
    void testConvertToFieldOrbitCartesian() {
        // GIVEN
        final ComplexField field = ComplexField.getInstance();
        final CartesianOrbit cartesianOrbit = createCartesianOrbit();
        // WHEN
        final FieldCartesianOrbit<Complex> actualFieldOrbit = (FieldCartesianOrbit<Complex>) OrbitType.CARTESIAN
                .convertToFieldOrbit(field, cartesianOrbit);
        // THEN
        final FieldCartesianOrbit<Complex> expectedFieldOrbit = new FieldCartesianOrbit<>(field, cartesianOrbit);
        compareFieldCartesian(expectedFieldOrbit, actualFieldOrbit);
    }

    @Test
    void testConvertToFieldOrbitAndConvertTypeEquinoctial() {
        templateTestConvertToFieldOrbitAndConvertType(OrbitType.EQUINOCTIAL);
    }

    @Test
    void testConvertToFieldOrbitAndConvertTypeCircular() {
        templateTestConvertToFieldOrbitAndConvertType(OrbitType.CIRCULAR);
    }

    @Test
    void testConvertToFieldOrbitAndConvertTypeKeplerian() {
        templateTestConvertToFieldOrbitAndConvertType(OrbitType.KEPLERIAN);
    }

    void templateTestConvertToFieldOrbitAndConvertType(final OrbitType orbitType) {
        // GIVEN
        final ComplexField field = ComplexField.getInstance();
        final CartesianOrbit cartesianOrbit = createCartesianOrbit();
        // WHEN
        final Orbit orbit = orbitType.convertType(cartesianOrbit);
        final FieldOrbit<Complex> fieldOrbit = orbitType.convertToFieldOrbit(field, orbit);
        final FieldCartesianOrbit<Complex> actualFieldOrbit = (FieldCartesianOrbit<Complex>) OrbitType.CARTESIAN
                .convertType(fieldOrbit);
        // THEN
        final FieldCartesianOrbit<Complex> expectedFieldOrbit = new FieldCartesianOrbit<>(field, orbit);
        compareFieldCartesian(expectedFieldOrbit, actualFieldOrbit);
    }

    private CartesianOrbit createCartesianOrbit() {
        final Vector3D position = Vector3D.MINUS_I.scalarMultiply(1e6);
        final Vector3D velocity = Vector3D.PLUS_K.scalarMultiply(1e3);
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        final Frame frame = FramesFactory.getEME2000();
        final double mu = Constants.EGM96_EARTH_MU;
        final TimeStampedPVCoordinates pv = new TimeStampedPVCoordinates(date, position, velocity);
        return new CartesianOrbit(pv, frame, mu);
    }

    static <T extends CalculusFieldElement<T>> void compareFieldCartesian(final FieldCartesianOrbit<T> expectedFieldOrbit,
                                                                          final FieldCartesianOrbit<T> actualFieldOrbit) {
        Assertions.assertEquals(expectedFieldOrbit.hasDerivatives(), actualFieldOrbit.hasDerivatives());
        Assertions.assertEquals(expectedFieldOrbit.getFrame(), actualFieldOrbit.getFrame());
        Assertions.assertEquals(expectedFieldOrbit.getMu(), actualFieldOrbit.getMu());
        Assertions.assertEquals(expectedFieldOrbit.getDate(), actualFieldOrbit.getDate());
        final double tolerance = 1e-6;
        Assertions.assertArrayEquals(expectedFieldOrbit.getPosition().toVector3D().toArray(),
                actualFieldOrbit.getPosition().toVector3D().toArray(), tolerance);
        Assertions.assertArrayEquals(expectedFieldOrbit.getPVCoordinates().getVelocity().toVector3D().toArray(),
                actualFieldOrbit.getPVCoordinates().getVelocity().toVector3D().toArray(), tolerance);
        Assertions.assertArrayEquals(expectedFieldOrbit.getPVCoordinates().getAcceleration().toVector3D().toArray(),
                actualFieldOrbit.getPVCoordinates().getAcceleration().toVector3D().toArray(), tolerance);
    }

}