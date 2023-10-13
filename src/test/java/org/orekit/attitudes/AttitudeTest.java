/* Copyright 2002-2023 CS GROUP
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

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.AngularCoordinates;

public class AttitudeTest {

    @Test
    public void testZeroRate() {
        //        Utils.setDataRoot("regular-data");
        Attitude attitude = new Attitude(AbsoluteDate.J2000_EPOCH, FramesFactory.getEME2000(),
                                         new Rotation(0.48, 0.64, 0.36, 0.48, false),
                                         Vector3D.ZERO, Vector3D.ZERO);
        Assertions.assertEquals(Vector3D.ZERO, attitude.getSpin());
        double dt = 10.0;
        Attitude shifted = attitude.shiftedBy(dt);
        Assertions.assertEquals(Vector3D.ZERO, shifted.getRotationAcceleration());
        Assertions.assertEquals(Vector3D.ZERO, shifted.getSpin());
        Assertions.assertEquals(0.0, Rotation.distance(attitude.getRotation(), shifted.getRotation()), 1.0e-15);
    }

    @Test
    public void testShift() {
        //Utils.setDataRoot("regular-data");
        double rate = 2 * FastMath.PI / (12 * 60);
        Attitude attitude = new Attitude(AbsoluteDate.J2000_EPOCH, FramesFactory.getEME2000(),
                                         Rotation.IDENTITY,
                                         new Vector3D(rate, Vector3D.PLUS_K), Vector3D.ZERO);
        Assertions.assertEquals(rate, attitude.getSpin().getNorm(), 1.0e-10);
        double dt = 10.0;
        double alpha = rate * dt;
        Attitude shifted = attitude.shiftedBy(dt);
        Assertions.assertEquals(rate, shifted.getSpin().getNorm(), 1.0e-10);
        Assertions.assertEquals(alpha, Rotation.distance(attitude.getRotation(), shifted.getRotation()), 1.0e-10);

        Vector3D xSat = shifted.getRotation().applyInverseTo(Vector3D.PLUS_I);
        Assertions.assertEquals(0.0, xSat.subtract(new Vector3D(FastMath.cos(alpha), FastMath.sin(alpha), 0)).getNorm(), 1.0e-10);
        Vector3D ySat = shifted.getRotation().applyInverseTo(Vector3D.PLUS_J);
        Assertions.assertEquals(0.0, ySat.subtract(new Vector3D(-FastMath.sin(alpha), FastMath.cos(alpha), 0)).getNorm(), 1.0e-10);
        Vector3D zSat = shifted.getRotation().applyInverseTo(Vector3D.PLUS_K);
        Assertions.assertEquals(0.0, zSat.subtract(Vector3D.PLUS_K).getNorm(), 1.0e-10);

    }

    @Test
    public void testSpin() {
        //Utils.setDataRoot("regular-data");
        double rate = 2 * FastMath.PI / (12 * 60);
        Attitude attitude = new Attitude(AbsoluteDate.J2000_EPOCH, FramesFactory.getEME2000(),
                                         new Rotation(0.48, 0.64, 0.36, 0.48, false),
                                         new Vector3D(rate, Vector3D.PLUS_K), Vector3D.ZERO);
        Assertions.assertEquals(rate, attitude.getSpin().getNorm(), 1.0e-10);
        double dt = 10.0;
        Attitude shifted = attitude.shiftedBy(dt);
        Assertions.assertEquals(rate, shifted.getSpin().getNorm(), 1.0e-10);
        Assertions.assertEquals(rate * dt, Rotation.distance(attitude.getRotation(), shifted.getRotation()), 1.0e-10);

        Vector3D shiftedX  = shifted.getRotation().applyInverseTo(Vector3D.PLUS_I);
        Vector3D shiftedY  = shifted.getRotation().applyInverseTo(Vector3D.PLUS_J);
        Vector3D shiftedZ  = shifted.getRotation().applyInverseTo(Vector3D.PLUS_K);
        Vector3D originalX = attitude.getRotation().applyInverseTo(Vector3D.PLUS_I);
        Vector3D originalY = attitude.getRotation().applyInverseTo(Vector3D.PLUS_J);
        Vector3D originalZ = attitude.getRotation().applyInverseTo(Vector3D.PLUS_K);
        Assertions.assertEquals( FastMath.cos(rate * dt), Vector3D.dotProduct(shiftedX, originalX), 1.0e-10);
        Assertions.assertEquals( FastMath.sin(rate * dt), Vector3D.dotProduct(shiftedX, originalY), 1.0e-10);
        Assertions.assertEquals( 0.0,                 Vector3D.dotProduct(shiftedX, originalZ), 1.0e-10);
        Assertions.assertEquals(-FastMath.sin(rate * dt), Vector3D.dotProduct(shiftedY, originalX), 1.0e-10);
        Assertions.assertEquals( FastMath.cos(rate * dt), Vector3D.dotProduct(shiftedY, originalY), 1.0e-10);
        Assertions.assertEquals( 0.0,                 Vector3D.dotProduct(shiftedY, originalZ), 1.0e-10);
        Assertions.assertEquals( 0.0,                 Vector3D.dotProduct(shiftedZ, originalX), 1.0e-10);
        Assertions.assertEquals( 0.0,                 Vector3D.dotProduct(shiftedZ, originalY), 1.0e-10);
        Assertions.assertEquals( 1.0,                 Vector3D.dotProduct(shiftedZ, originalZ), 1.0e-10);

        Vector3D forward = AngularCoordinates.estimateRate(attitude.getRotation(), shifted.getRotation(), dt);
        Assertions.assertEquals(0.0, forward.subtract(attitude.getSpin()).getNorm(), 1.0e-10);

        Vector3D reversed = AngularCoordinates.estimateRate(shifted.getRotation(), attitude.getRotation(), dt);
        Assertions.assertEquals(0.0, reversed.add(attitude.getSpin()).getNorm(), 1.0e-10);

    }

}

