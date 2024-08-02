/* Copyright 2002-2024 CS GROUP
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
package org.orekit.attitudes;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Test;
import org.orekit.frames.FramesFactory;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldAngularCoordinates;

import static org.junit.jupiter.api.Assertions.assertEquals;


class FieldAttitudeTest {

    @Test
    void testShift() {
        doTestShift(Binary64Field.getInstance());
    }

    @Test
    void testSpin() {
        doTestSpin(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestShift(final Field<T> field){
        T zero = field.getZero();
        T one  = field.getOne();
        T rate = one.multiply(2 * FastMath.PI / (12 * 60));
        FieldAttitude<T> attitude = new FieldAttitude<>(new FieldAbsoluteDate<>(field), FramesFactory.getEME2000(),
                        new FieldRotation<>(one, zero, zero, zero, false),
                                            new FieldVector3D<>(rate, new FieldVector3D<>(zero, zero, one)), new FieldVector3D<>(zero, zero, zero));
        assertEquals(rate.getReal(), attitude.getSpin().getNorm().getReal(), 1.0e-10);
        double dt_R = 10.0;
        T dt = zero.add(dt_R);

        T alpha = rate.multiply(dt);
        FieldAttitude<T> shifted = attitude.shiftedBy(dt);
        assertEquals(rate.getReal(), shifted.getSpin().getNorm().getReal(), 1.0e-10);
        assertEquals(alpha.getReal(), FieldRotation.distance(attitude.getRotation(), shifted.getRotation()).getReal(), 1.0e-10);

        FieldVector3D<T> xSat = shifted.getRotation().applyInverseTo(Vector3D.PLUS_I);
        assertEquals(0.0, xSat.subtract(new FieldVector3D<>(alpha.cos(), alpha.sin(), zero)).getNorm().getReal(), 1.0e-10);
        FieldVector3D<T> ySat = shifted.getRotation().applyInverseTo(Vector3D.PLUS_J);
        assertEquals(0.0, ySat.subtract(new FieldVector3D<>(alpha.sin().multiply(-1), alpha.cos(), zero)).getNorm().getReal(), 1.0e-10);
        FieldVector3D<T> zSat = shifted.getRotation().applyInverseTo(Vector3D.PLUS_K);
        assertEquals(0.0, zSat.subtract(Vector3D.PLUS_K).getNorm().getReal(), 1.0e-10);

    }


    private <T extends CalculusFieldElement<T>> void doTestSpin(final Field<T> field) {
        T zero = field.getZero();
        T rate = zero.add(2 * FastMath.PI / (12 * 60));
        FieldAttitude<T> attitude = new FieldAttitude<>(new FieldAbsoluteDate<>(field), FramesFactory.getEME2000(),
                                                        new FieldRotation<>(zero.add(0.48), zero.add(0.64), zero.add(0.36), zero.add(0.48), false),
                                                        new FieldVector3D<>(rate, FieldVector3D.getPlusK(field)), FieldVector3D.getZero(field));
        assertEquals(rate.getReal(), attitude.getSpin().getNorm().getReal(), 1.0e-10);
        T dt = zero.add(10.0);
        FieldAttitude<T> shifted = attitude.shiftedBy(dt);
        assertEquals(rate.getReal(), shifted.getSpin().getNorm().getReal(), 1.0e-10);

        FieldVector3D<T> shiftedX  = shifted.getRotation().applyInverseTo(Vector3D.PLUS_I);
        FieldVector3D<T> shiftedY  = shifted.getRotation().applyInverseTo(Vector3D.PLUS_J);
        FieldVector3D<T> shiftedZ  = shifted.getRotation().applyInverseTo(Vector3D.PLUS_K);
        FieldVector3D<T> originalX = attitude.getRotation().applyInverseTo(Vector3D.PLUS_I);
        FieldVector3D<T> originalY = attitude.getRotation().applyInverseTo(Vector3D.PLUS_J);
        FieldVector3D<T> originalZ = attitude.getRotation().applyInverseTo(Vector3D.PLUS_K);
        assertEquals( FastMath.cos(rate.getReal() * dt.getReal()), FieldVector3D.dotProduct(shiftedX, originalX).getReal(), 1.0e-10);
        assertEquals( FastMath.sin(rate.getReal() * dt.getReal()), FieldVector3D.dotProduct(shiftedX, originalY).getReal(), 1.0e-10);
        assertEquals( 0.0,                 FieldVector3D.dotProduct(shiftedX, originalZ).getReal(), 1.0e-10);
        assertEquals(-FastMath.sin(rate.getReal() * dt.getReal()), FieldVector3D.dotProduct(shiftedY, originalX).getReal(), 1.0e-10);
        assertEquals( FastMath.cos(rate.getReal() * dt.getReal()), FieldVector3D.dotProduct(shiftedY, originalY).getReal(), 1.0e-10);
        assertEquals( 0.0,                 FieldVector3D.dotProduct(shiftedY, originalZ).getReal(), 1.0e-10);
        assertEquals( 0.0,                 FieldVector3D.dotProduct(shiftedZ, originalX).getReal(), 1.0e-10);
        assertEquals( 0.0,                 FieldVector3D.dotProduct(shiftedZ, originalY).getReal(), 1.0e-10);
        assertEquals( 1.0,                 FieldVector3D.dotProduct(shiftedZ, originalZ).getReal(), 1.0e-10);

        FieldVector3D<T> forward = FieldAngularCoordinates.estimateRate(attitude.getRotation(), shifted.getRotation(), dt);
        assertEquals(0.0, forward.subtract(attitude.getSpin()).getNorm().getReal(), 1.0e-10);

        FieldVector3D<T> reversed = FieldAngularCoordinates.estimateRate(shifted.getRotation(), attitude.getRotation(), dt);
        assertEquals(0.0, reversed.add(attitude.getSpin()).getNorm().getReal(), 1.0e-10);

    }

}

