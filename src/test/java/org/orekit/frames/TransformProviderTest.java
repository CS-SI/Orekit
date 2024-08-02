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
package org.orekit.frames;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.complex.Complex;
import org.hipparchus.complex.ComplexField;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Test;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TransformProviderTest {

    @Test
    void testGetStaticTransform() {
        // GIVEN
        final TestTransformProvider transformProvider = new TestTransformProvider();
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        // WHEN
        final StaticTransform staticTransform = transformProvider.getStaticTransform(date);
        // THEN
        final Transform transform = transformProvider.getTransform(date);
        assertEquals(date, staticTransform.getDate());
        assertEquals(transform.getCartesian().getPosition(), staticTransform.getTranslation());
        assertEquals(0., Rotation.distance(transform.getRotation(), staticTransform.getRotation()));
    }

    @Test
    void testGetKinematicTransform() {
        // GIVEN
        final TestTransformProvider transformProvider = new TestTransformProvider();
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        // WHEN
        final KinematicTransform kinematicTransform = transformProvider.getKinematicTransform(date);
        // THEN
        final Transform transform = transformProvider.getTransform(date);
        assertEquals(date, kinematicTransform.getDate());
        assertEquals(transform.getCartesian().getPosition(), kinematicTransform.getTranslation());
        assertEquals(transform.getCartesian().getVelocity(), kinematicTransform.getVelocity());
        assertEquals(0., Rotation.distance(transform.getRotation(), kinematicTransform.getRotation()));
        assertEquals(transform.getRotationRate(), kinematicTransform.getRotationRate());
    }

    @Test
    void testFieldGetKinematicTransform() {
        // GIVEN
        final TestTransformProvider transformProvider = new TestTransformProvider();
        final ComplexField complexField = ComplexField.getInstance();
        final FieldAbsoluteDate<Complex> fieldDate = FieldAbsoluteDate.getArbitraryEpoch(complexField);
        // WHEN
        final FieldKinematicTransform<Complex> fieldKinematicTransform = transformProvider
                .getKinematicTransform(fieldDate);
        // THEN
        final KinematicTransform kinematicTransform = transformProvider.getKinematicTransform(fieldDate.toAbsoluteDate());
        assertEquals(kinematicTransform.getDate(), fieldKinematicTransform.getDate());
        assertEquals(kinematicTransform.getTranslation(), fieldKinematicTransform.getTranslation().toVector3D());
        assertEquals(kinematicTransform.getVelocity(), fieldKinematicTransform.getVelocity().toVector3D());
        assertEquals(0., Rotation.distance(kinematicTransform.getRotation(),
                fieldKinematicTransform.getRotation().toRotation()));
        assertEquals(kinematicTransform.getRotationRate(),
                fieldKinematicTransform.getRotationRate().toVector3D());
    }

    @Test
    void testFieldGetStaticTransform() {
        // GIVEN
        final TestTransformProvider transformProvider = new TestTransformProvider();
        final ComplexField complexField = ComplexField.getInstance();
        final FieldAbsoluteDate<Complex> fieldDate = FieldAbsoluteDate.getArbitraryEpoch(complexField);
        // WHEN
        final FieldStaticTransform<Complex> fieldStaticTransform = transformProvider
                .getStaticTransform(fieldDate);
        // THEN
        final StaticTransform staticTransform = transformProvider.getStaticTransform(fieldDate.toAbsoluteDate());
        assertEquals(staticTransform.getDate(), fieldStaticTransform.getDate());
        assertEquals(staticTransform.getTranslation(), fieldStaticTransform.getTranslation().toVector3D());
        assertEquals(0., Rotation.distance(staticTransform.getRotation(),
                fieldStaticTransform.getRotation().toRotation()));
    }

    private static class TestTransformProvider implements TransformProvider {

        @Override
        public Transform getTransform(AbsoluteDate date) {
            return new Transform(date, new Rotation(Vector3D.PLUS_I, 1., RotationConvention.FRAME_TRANSFORM),
                    Vector3D.MINUS_J);
        }

        @Override
        public <T extends CalculusFieldElement<T>> FieldTransform<T> getTransform(FieldAbsoluteDate<T> date) {
            final Transform transform = getTransform(date.toAbsoluteDate());
            final Field<T> field = date.getField();
            final FieldRotation<T> fieldRotation = new FieldRotation<>(field, transform.getRotation());
            final FieldVector3D<T> fieldRotationRate = new FieldVector3D<>(field, transform.getRotationRate());
            return new FieldTransform<>(date, fieldRotation, fieldRotationRate);
        }
    }

}
