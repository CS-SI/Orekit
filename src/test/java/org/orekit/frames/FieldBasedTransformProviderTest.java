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
package org.orekit.frames;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FieldBasedTransformProviderTest {

    @BeforeEach
    void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    void testFieldTransform() {
        // GIVEN
        final TestProvider provider = new TestProvider();
        final FieldAbsoluteDate<Binary64> fieldAbsoluteDate = FieldAbsoluteDate.getArbitraryEpoch(Binary64Field.getInstance());
        // WHEN
        final FieldTransform<Binary64> fieldTransform = provider.getTransform(fieldAbsoluteDate);
        // THEN
        final Transform transform = provider.getTransform(fieldAbsoluteDate.toAbsoluteDate());
        assertEquals(fieldAbsoluteDate, fieldTransform.getFieldDate());
        assertEquals(transform.getCartesian().getPosition(), fieldTransform.getTranslation().toVector3D());
        assertEquals(transform.getCartesian().getVelocity(), fieldTransform.getVelocity().toVector3D());
        assertEquals(0., Rotation.distance(transform.getRotation(), fieldTransform.getRotation().toRotation()));
        assertEquals(transform.getRotationRate(), fieldTransform.getRotationRate().toVector3D());
    }

    @Test
    void testFieldKinematic() {
        // GIVEN
        final TestProvider provider = new TestProvider();
        final FieldAbsoluteDate<Binary64> fieldAbsoluteDate = FieldAbsoluteDate.getArbitraryEpoch(Binary64Field.getInstance());
        // WHEN
        final FieldKinematicTransform<Binary64> kinematicTransform = provider.getKinematicTransform(fieldAbsoluteDate);
        // THEN
        final FieldTransform<Binary64> transform = provider.getTransform(fieldAbsoluteDate);
        assertEquals(fieldAbsoluteDate, kinematicTransform.getFieldDate());
        assertEquals(transform.getCartesian().getPosition(), kinematicTransform.getTranslation());
        assertEquals(transform.getCartesian().getVelocity(), kinematicTransform.getVelocity());
        assertEquals(0., Rotation.distance(transform.getRotation().toRotation(), kinematicTransform.getRotation().toRotation()));
        assertEquals(transform.getRotationRate(), kinematicTransform.getRotationRate());
    }

    @Test
    void testFieldStatic() {
        // GIVEN
        final TestProvider provider = new TestProvider();
        final FieldAbsoluteDate<Binary64> fieldAbsoluteDate = FieldAbsoluteDate.getArbitraryEpoch(Binary64Field.getInstance());
        // WHEN
        final FieldStaticTransform<Binary64> staticTransform = provider.getStaticTransform(fieldAbsoluteDate);
        // THEN
        final FieldTransform<Binary64> transform = provider.getTransform(fieldAbsoluteDate);
        assertEquals(fieldAbsoluteDate, staticTransform.getFieldDate());
        assertEquals(transform.getCartesian().getPosition(), staticTransform.getTranslation());
        assertEquals(0., Rotation.distance(transform.getRotation().toRotation(), staticTransform.getRotation().toRotation()));
    }

    @Test
    void testKinematic() {
        // GIVEN
        final TestProvider provider = new TestProvider();
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        // WHEN
        final KinematicTransform kinematicTransform = provider.getKinematicTransform(date);
        // THEN
        final Transform transform = provider.getTransform(date);
        assertEquals(date, kinematicTransform.getDate());
        assertEquals(transform.getCartesian().getPosition(), kinematicTransform.getTranslation());
        assertEquals(transform.getCartesian().getVelocity(), kinematicTransform.getVelocity());
        assertEquals(0., Rotation.distance(transform.getRotation(), kinematicTransform.getRotation()));
        assertEquals(transform.getRotationRate(), kinematicTransform.getRotationRate());
    }

    @Test
    void testStatic() {
        // GIVEN
        final TestProvider provider = new TestProvider();
        final AbsoluteDate date = AbsoluteDate.ARBITRARY_EPOCH;
        // WHEN
        final StaticTransform staticTransform = provider.getStaticTransform(date);
        // THEN
        final Transform transform = provider.getTransform(date);
        assertEquals(date, staticTransform.getDate());
        assertEquals(transform.getCartesian().getPosition(), staticTransform.getTranslation());
        assertEquals(0., Rotation.distance(transform.getRotation(), staticTransform.getRotation()));
    }

    private static class TestProvider implements FieldBasedTransformProvider {

        @Override
        public <T extends CalculusFieldElement<T>> FieldRotation<T> getRotation(final FieldAbsoluteDate<T> date) {
            return new FieldRotation<>(FieldVector3D.getMinusI(date.getField()), date.getMJD(),
                    RotationConvention.VECTOR_OPERATOR);
        }
    }
}
