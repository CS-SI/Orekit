/* Copyright 2022-2026 Romain Serra
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

import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FieldKeplerianParametersConverterTest {

    @ParameterizedTest
    @ValueSource(doubles = {-5., -0.2, 0.1, 0.5, 1., 3., 10.})
    void testBackAndForthEccentric(final double vx) {
        // GIVEN
        final GradientField field = GradientField.getField(6);
        final FieldVector3D<Gradient> fieldPosition = new FieldVector3D<>(new Gradient(1., buildArray(0)),
                new Gradient(2., buildArray(1)), new Gradient(3., buildArray(2)));
        final FieldVector3D<Gradient> fieldVelocity = new FieldVector3D<>(new Gradient(vx, buildArray(3)),
                new Gradient(-0.2, buildArray(4)), new Gradient(0.3, buildArray(5)));
        final FieldPVCoordinates<Gradient> fieldPV = new FieldPVCoordinates<>(fieldPosition, fieldVelocity);
        final FieldKeplerianParametersConverter<Gradient> converter = new FieldKeplerianParametersConverter<>(field.getOne());
        // WHEN
        final FieldKeplerianParameters<Gradient> fieldElements = converter.toParameters(fieldPV, PositionAngleType.TRUE);
        final FieldPVCoordinates<Gradient> actualPV = converter.toCartesian(fieldElements);
        // THEN
        final FieldVector3D<Gradient> actualPosition = actualPV.getPosition();
        final FieldVector3D<Gradient> actualVelocity = actualPV.getVelocity();
        assertGradientEquals(fieldPV.getPosition().getX(), actualPosition.getX());
        assertGradientEquals(fieldPV.getPosition().getY(), actualPosition.getY());
        assertGradientEquals(fieldPV.getPosition().getZ(), actualPosition.getZ());
        assertGradientEquals(fieldPV.getVelocity().getX(), actualVelocity.getX());
        assertGradientEquals(fieldPV.getVelocity().getY(), actualVelocity.getY());
        assertGradientEquals(fieldPV.getVelocity().getZ(), actualVelocity.getZ());
    }

    private static double[] buildArray(final int index) {
        final double[] array = new double[6];
        array[index] = 1.;
        return array;
    }

    private static void assertGradientEquals(final Gradient expected, final Gradient actual) {
        assertEquals(expected.getReal(), actual.getReal(), 1e-6);
        assertArrayEquals(expected.getGradient(), actual.getGradient(), 1e-12);
    }

    @ParameterizedTest
    @EnumSource(PositionAngleType.class)
    void testToParametersEccentric(final PositionAngleType positionAngleType) {
        // GIVEN
        final Binary64Field field = Binary64Field.getInstance();
        final PVCoordinates pv = new PVCoordinates(new Vector3D(1, 2, 3), new Vector3D(0.1, -0.2, 0.3));
        final FieldPVCoordinates<Binary64> fieldPV = new FieldPVCoordinates<>(Binary64Field.getInstance(), pv);
        final Binary64 mu = field.getOne();
        final FieldKeplerianParametersConverter<Binary64> converter = new FieldKeplerianParametersConverter<>(mu);
        // WHEN
        final FieldKeplerianParameters<Binary64> fieldElements = converter.toParameters(fieldPV, positionAngleType);
        // THEN
        final KeplerianParameters elements = new KeplerianParametersConverter(mu.getReal()).toParameters(pv, positionAngleType);
        assertEquals(elements, fieldElements.toKeplerianElements());
    }

    @ParameterizedTest
    @EnumSource(PositionAngleType.class)
    void testToParametersHyperbolic(final PositionAngleType positionAngleType) {
        // GIVEN
        final Binary64Field field = Binary64Field.getInstance();
        final PVCoordinates pv = new PVCoordinates(new Vector3D(1, 2, 3), new Vector3D(4, 5, 6));
        final FieldPVCoordinates<Binary64> fieldPV = new FieldPVCoordinates<>(Binary64Field.getInstance(), pv);
        final Binary64 mu = field.getOne();
        final FieldKeplerianParametersConverter<Binary64> converter = new FieldKeplerianParametersConverter<>(mu);
        // WHEN
        final FieldKeplerianParameters<Binary64> fieldElements = converter.toParameters(fieldPV, positionAngleType);
        // THEN
        final KeplerianParameters elements = new KeplerianParametersConverter(mu.getReal()).toParameters(pv, positionAngleType);
        assertEquals(elements, fieldElements.toKeplerianElements());
    }

    @ParameterizedTest
    @EnumSource(PositionAngleType.class)
    void testToCartesianElliptic(final PositionAngleType positionAngleType) {
        // GIVEN
        final Binary64Field field = Binary64Field.getInstance();
        final KeplerianParameters elements = new KeplerianParameters(1., 0.1, 2, 3, 4, 5, positionAngleType);
        final FieldKeplerianParameters<Binary64> fieldElements = new FieldKeplerianParameters<>(field, elements);
        final Binary64 mu = field.getOne();
        final FieldKeplerianParametersConverter<Binary64> converter = new FieldKeplerianParametersConverter<>(mu);
        // WHEN
        final FieldPVCoordinates<Binary64> fieldPV = converter.toCartesian(fieldElements);
        // THEN
        final PVCoordinates pv = new KeplerianParametersConverter(mu.getReal()).toCartesian(elements);
        assertEquals(pv.getPosition(), fieldPV.getPosition().toVector3D());
        assertEquals(pv.getVelocity(), fieldPV.getVelocity().toVector3D());
    }

    @ParameterizedTest
    @EnumSource(PositionAngleType.class)
    void testToCartesianHyperbolic(final PositionAngleType positionAngleType) {
        // GIVEN
        final Binary64Field field = Binary64Field.getInstance();
        final KeplerianParameters elements = new KeplerianParameters(-1., 1.5, 2, 3, 4, 5, positionAngleType);
        final FieldKeplerianParameters<Binary64> fieldElements = new FieldKeplerianParameters<>(field, elements);
        final Binary64 mu = field.getOne();
        final FieldKeplerianParametersConverter<Binary64> converter = new FieldKeplerianParametersConverter<>(mu);
        // WHEN
        final FieldPVCoordinates<Binary64> fieldPV = converter.toCartesian(fieldElements);
        // THEN
        final PVCoordinates pv = new KeplerianParametersConverter(mu.getReal()).toCartesian(elements);
        assertEquals(pv.getPosition(), fieldPV.getPosition().toVector3D());
        assertEquals(pv.getVelocity(), fieldPV.getVelocity().toVector3D());
    }
}
