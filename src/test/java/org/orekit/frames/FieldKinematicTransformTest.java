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
package org.orekit.frames;

import org.hipparchus.Field;
import org.hipparchus.complex.Complex;
import org.hipparchus.complex.ComplexField;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

class FieldKinematicTransformTest {

    @Test
    void testOf() {
        // GIVEN
        final KinematicTransform kinematicTransform = createArbitraryKineticTransform();
        // WHEn
        final FieldKinematicTransform<Complex> fieldKinematicTransform = FieldKinematicTransform.of(ComplexField.getInstance(),
                kinematicTransform);
        // THEN
        Assertions.assertEquals(fieldKinematicTransform.getDate(), kinematicTransform.getDate());
        Assertions.assertEquals(fieldKinematicTransform.getTranslation().toVector3D(), kinematicTransform.getTranslation());
        Assertions.assertEquals(fieldKinematicTransform.getVelocity().toVector3D(), kinematicTransform.getVelocity());
        Assertions.assertEquals(0., Rotation.distance(fieldKinematicTransform.getRotation().toRotation(),
                kinematicTransform.getRotation()));
        Assertions.assertEquals(fieldKinematicTransform.getRotationRate().toVector3D(),
                kinematicTransform.getRotationRate());
    }

    private KinematicTransform createArbitraryKineticTransform() {
        return KinematicTransform.of(AbsoluteDate.ARBITRARY_EPOCH,
                new PVCoordinates(Vector3D.MINUS_I, Vector3D.PLUS_J),
                new Rotation(Vector3D.MINUS_K, Vector3D.PLUS_I),
                Vector3D.MINUS_J);
    }

    @Test
    void testGetInverse() {
        // GIVEN
        final FieldKinematicTransform<Complex> fieldKinematicTransform = createArbitraryFieldKineticTransform();
        // WHEn
        final FieldKinematicTransform<Complex> fieldInverse = fieldKinematicTransform.getInverse();
        // THEN
        final KinematicTransform kinematicTransform = createKinematicTransform(fieldKinematicTransform);
        final KinematicTransform inverse = kinematicTransform.getInverse();
        Assertions.assertEquals(inverse.getDate(), fieldInverse.getDate());
        Assertions.assertEquals(inverse.getTranslation(), fieldInverse.getTranslation().toVector3D());
        Assertions.assertEquals(inverse.getVelocity(), fieldInverse.getVelocity().toVector3D());
        Assertions.assertEquals(0., Rotation.distance(inverse.getRotation(),
                fieldInverse.getRotation().toRotation()));
        Assertions.assertEquals(inverse.getRotationRate(), fieldInverse.getRotationRate().toVector3D());
    }

    @Test
    void testCompositeVelocity() {
        // GIVEN
        final FieldKinematicTransform<Complex> fieldKinematicTransform = createArbitraryFieldKineticTransform();
        final Field<Complex> field = fieldKinematicTransform.getFieldDate().getField();
        // WHEN
        final FieldVector3D<Complex> actualCompositeVelocity = FieldKinematicTransform
                .compositeVelocity(fieldKinematicTransform, FieldKinematicTransform.getIdentity(field));
        // THEN
        final KinematicTransform kinematicTransform = createKinematicTransform(fieldKinematicTransform);
        final Vector3D expectedCompositeVelocity = kinematicTransform.getVelocity();
        Assertions.assertEquals(expectedCompositeVelocity, actualCompositeVelocity.toVector3D());
    }

    @Test
    void testCompositeRotationRate() {
        // GIVEN
        final FieldKinematicTransform<Complex> fieldKinematicTransform = createArbitraryFieldKineticTransform();
        final Field<Complex> field = fieldKinematicTransform.getFieldDate().getField();
        // WHEN
        final FieldVector3D<Complex> actualCompositeRotationRate = FieldKinematicTransform
                .compositeRotationRate(fieldKinematicTransform, FieldKinematicTransform.getIdentity(field));
        // THEN
        final KinematicTransform kinematicTransform = createKinematicTransform(fieldKinematicTransform);
        final Vector3D expectedCompositeRotationRate = kinematicTransform.getRotationRate();
        Assertions.assertEquals(expectedCompositeRotationRate, actualCompositeRotationRate.toVector3D());
    }

    @Test
    void testTransformPVOnly(){
        // GIVEN
        final FieldKinematicTransform<Complex> fieldKinematicTransform = createArbitraryFieldKineticTransform();
        final FieldPVCoordinates<Complex> pvCoordinates = createFieldPV();
        // WHEN
        final FieldPVCoordinates<Complex> convertedPV = fieldKinematicTransform.transformOnlyPV(pvCoordinates);
        // THEN
        final KinematicTransform kinematicTransform = createKinematicTransform(fieldKinematicTransform);
        final PVCoordinates expectedPV = kinematicTransform.transformOnlyPV(pvCoordinates
                .toPVCoordinates());
        comparePVCoordinates(expectedPV, convertedPV.toPVCoordinates());
    }

    @Test
    void testTransformTimeStampedPVCoordinatesWithoutA() {
        // GIVEN
        final FieldKinematicTransform<Complex> fieldKinematicTransform = createArbitraryFieldKineticTransform();
        final TimeStampedFieldPVCoordinates<Complex> pvCoordinates = new TimeStampedFieldPVCoordinates<>(AbsoluteDate.ARBITRARY_EPOCH,
                createFieldPV());
        // WHEN
        final TimeStampedFieldPVCoordinates<Complex> convertedPV = fieldKinematicTransform
                .transformOnlyPV(pvCoordinates);
        // THEN
        final KinematicTransform kinematicTransform = createKinematicTransform(fieldKinematicTransform);
        final PVCoordinates expectedPV = kinematicTransform.transformOnlyPV(pvCoordinates
                .toPVCoordinates());
        comparePVCoordinates(expectedPV, convertedPV.toPVCoordinates());
    }

    private FieldPVCoordinates<Complex> createFieldPV() {
        final PVCoordinates pvCoordinates = new PVCoordinates(new Vector3D(1, 2, 3), new Vector3D(4, 5, 6));
        return new FieldPVCoordinates<>(ComplexField.getInstance(), pvCoordinates);
    }

    private void comparePVCoordinates(final PVCoordinates pvCoordinates, final PVCoordinates reconvertedPV) {
        final double tolerance = 1e-10;
        final double[] expectedPosition = pvCoordinates.getPosition().toArray();
        final double[] actualPosition = reconvertedPV.getPosition().toArray();
        final double[] expectedVelocity = pvCoordinates.getVelocity().toArray();
        final double[] actualVelocity = reconvertedPV.getVelocity().toArray();
        for (int i = 0; i < 3; i++) {
            Assertions.assertEquals(expectedPosition[i], actualPosition[i], tolerance);
            Assertions.assertEquals(expectedVelocity[i], actualVelocity[i], tolerance);
        }
    }

    private FieldKinematicTransform<Complex> createArbitraryFieldKineticTransform() {
        final Field<Complex> complexField = ComplexField.getInstance();
        final FieldAbsoluteDate<Complex> fieldAbsoluteDate = new FieldAbsoluteDate<>(complexField,
                AbsoluteDate.ARBITRARY_EPOCH);
        return FieldKinematicTransform.of(fieldAbsoluteDate,
                new FieldPVCoordinates<>(complexField, new PVCoordinates(Vector3D.MINUS_I, Vector3D.PLUS_J)),
                new FieldRotation<>(complexField, new Rotation(Vector3D.MINUS_K, Vector3D.PLUS_I)),
                new FieldVector3D<>(complexField, Vector3D.MINUS_J));
    }

    private KinematicTransform createKinematicTransform(final FieldKinematicTransform<?> fieldKinematicTransform) {
        return KinematicTransform.of(fieldKinematicTransform.getDate(),
                new PVCoordinates(fieldKinematicTransform.getTranslation().toVector3D(),
                fieldKinematicTransform.getVelocity().toVector3D()),
                fieldKinematicTransform.getRotation().toRotation(),
                fieldKinematicTransform.getRotationRate().toVector3D());
    }

}
