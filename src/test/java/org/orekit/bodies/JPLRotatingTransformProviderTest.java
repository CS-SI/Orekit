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
package org.orekit.bodies;

import org.hipparchus.analysis.differentiation.UnivariateDerivative1;
import org.hipparchus.analysis.differentiation.UnivariateDerivative1Field;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.orekit.Utils;
import org.orekit.frames.FieldStaticTransform;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.StaticTransform;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.AngularCoordinates;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class JPLRotatingTransformProviderTest {

    private static IAUPole iauPole;

    @BeforeEach
    void setUp() {
        Utils.setDataRoot("regular-data");
        iauPole = PredefinedIAUPoles.getIAUPole(JPLEphemeridesLoader.EphemerisType.EARTH, TimeScalesFactory.getTimeScales());
    }

    @Test
    void testFieldTransform() {
        // GIVEN
        final JPLRotatingTransformProvider provider = new JPLRotatingTransformProvider(iauPole);
        final UnivariateDerivative1Field field = UnivariateDerivative1Field.getInstance();
        final FieldAbsoluteDate<UnivariateDerivative1> fieldAbsoluteDate = FieldAbsoluteDate.getArbitraryEpoch(field)
                .shiftedBy(new UnivariateDerivative1(0., 1.));
        // WHEN
        final FieldTransform<UnivariateDerivative1> fieldTransform = provider.getTransform(fieldAbsoluteDate);
        // THEN
        final Transform transform = provider.getTransform(fieldAbsoluteDate.toAbsoluteDate());
        assertNotEquals(0., fieldTransform.getRotation().getQ0().getFirstDerivative());
        assertEquals(fieldAbsoluteDate, fieldTransform.getFieldDate());
        assertEquals(transform.getCartesian().getPosition(), fieldTransform.getTranslation().toVector3D());
        assertEquals(transform.getCartesian().getVelocity(), fieldTransform.getVelocity().toVector3D());
        assertEquals(0., Rotation.distance(transform.getRotation(), fieldTransform.getRotation().toRotation()));
        assertEquals(transform.getRotationRate(), fieldTransform.getRotationRate().toVector3D());
    }

    @ParameterizedTest
    @EnumSource(value = JPLEphemeridesLoader.EphemerisType.class, names = {"SUN", "MOON", "JUPITER", "VENUS", "MARS"})
    void testFieldStatic(final JPLEphemeridesLoader.EphemerisType ephemerisType) {
        // GIVEN
        final JPLRotatingTransformProvider provider = new JPLRotatingTransformProvider(PredefinedIAUPoles.getIAUPole(ephemerisType,
                TimeScalesFactory.getTimeScales()));
        final FieldAbsoluteDate<Binary64> fieldAbsoluteDate = FieldAbsoluteDate.getArbitraryEpoch(Binary64Field.getInstance());
        // WHEN
        final FieldStaticTransform<Binary64> staticTransform = provider.getStaticTransform(fieldAbsoluteDate);
        // THEN
        final FieldTransform<Binary64> transform = provider.getTransform(fieldAbsoluteDate);
        assertEquals(fieldAbsoluteDate, staticTransform.getFieldDate());
        assertArrayEquals(transform.getTranslation().toVector3D().toArray(),
                staticTransform.getTranslation().toVector3D().toArray(), 1e-14);
        assertEquals(staticTransform.getRotation().getAngle(), transform.getRotation().getAngle());
        assertEquals(0., Rotation.distance(transform.getRotation().toRotation(), staticTransform.getRotation().toRotation()));
    }

    @Test
    void testStatic() {
        // GIVEN
        final JPLRotatingTransformProvider provider = new JPLRotatingTransformProvider(iauPole);
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        // WHEN
        final StaticTransform staticTransform = provider.getStaticTransform(date);
        // THEN
        final Transform transform = provider.getTransform(date);
        assertEquals(date, staticTransform.getDate());
        assertEquals(transform.getCartesian().getPosition(), staticTransform.getTranslation());
        assertEquals(0., Rotation.distance(transform.getRotation(), staticTransform.getRotation()), 1e-15);
    }

    @Test
    void testTransform() {
        // GIVEN
        final JPLRotatingTransformProvider provider = new JPLRotatingTransformProvider(iauPole);
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        // WHEN
        final Transform transform = provider.getTransform(date);
        // THEN
        assertEquals(date, transform.getDate());
        assertEquals(Vector3D.ZERO, transform.getTranslation());
        assertEquals(Vector3D.ZERO, transform.getVelocity());
        assertEquals(Vector3D.ZERO, transform.getAcceleration());
        final double w0 = iauPole.getPrimeMeridianAngle(date);
        final Rotation rotation = new Rotation(Vector3D.PLUS_K, w0, RotationConvention.FRAME_TRANSFORM);
        assertEquals(0., Rotation.distance(rotation, transform.getRotation()), 1e-15);
        final FieldAbsoluteDate<UnivariateDerivative1> fieldDate = new FieldAbsoluteDate<>(UnivariateDerivative1Field.getInstance(), date)
                .shiftedBy(new UnivariateDerivative1(0., 1.));
        final UnivariateDerivative1 fieldW0 = iauPole.getPrimeMeridianAngle(fieldDate);
        final FieldRotation<UnivariateDerivative1> fieldRotation = new FieldRotation<>(FieldVector3D.getPlusK(fieldDate.getField()),
                fieldW0, RotationConvention.FRAME_TRANSFORM);
        final AngularCoordinates coordinates = new AngularCoordinates(fieldRotation);
        assertArrayEquals(coordinates.getRotationRate().toArray(), transform.getRotationRate().toArray(), 1e-12);
    }
}
