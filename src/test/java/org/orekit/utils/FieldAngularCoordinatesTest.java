/* Copyright 2002-2016 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
package org.orekit.utils;


import java.util.Random;

import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.util.Decimal64;
import org.hipparchus.util.Decimal64Field;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.errors.OrekitException;

public class FieldAngularCoordinatesTest {

    @Test
    public void testZeroRate() throws OrekitException {
        FieldAngularCoordinates<DerivativeStructure> angularCoordinates =
                new FieldAngularCoordinates<DerivativeStructure>(createRotation(0.48, 0.64, 0.36, 0.48, false),
                                                                 createVector(0, 0, 0, 4), createVector(0, 0, 0, 4));
        Assert.assertEquals(createVector(0, 0, 0, 4), angularCoordinates.getRotationRate());
        double dt = 10.0;
        FieldAngularCoordinates<DerivativeStructure> shifted = angularCoordinates.shiftedBy(dt);
        Assert.assertEquals(0.0, shifted.getRotationAcceleration().getNorm().getReal(), 1.0e-15);
        Assert.assertEquals(0.0, shifted.getRotationRate().getNorm().getReal(), 1.0e-15);
        Assert.assertEquals(0.0, FieldRotation.distance(angularCoordinates.getRotation(), shifted.getRotation()).getReal(), 1.0e-15);
    }

    @Test
    public void testShift() throws OrekitException {
        double rate = 2 * FastMath.PI / (12 * 60);
        FieldAngularCoordinates<DerivativeStructure> angularCoordinates =
                new FieldAngularCoordinates<DerivativeStructure>(createRotation(1, 0, 0, 0, false),
                                       new FieldVector3D<DerivativeStructure>(rate, createVector(0, 0, 1, 4)), createVector(0, 0, 0, 4));
        Assert.assertEquals(rate, angularCoordinates.getRotationRate().getNorm().getReal(), 1.0e-10);
        double dt = 10.0;
        double alpha = rate * dt;
        FieldAngularCoordinates<DerivativeStructure> shifted = angularCoordinates.shiftedBy(dt);
        Assert.assertEquals(rate, shifted.getRotationRate().getNorm().getReal(), 1.0e-10);
        Assert.assertEquals(alpha, FieldRotation.distance(angularCoordinates.getRotation(), shifted.getRotation()).getReal(), 1.0e-10);

        FieldVector3D<DerivativeStructure> xSat = shifted.getRotation().applyInverseTo(createVector(1, 0, 0, 4));
        Assert.assertEquals(0.0, xSat.subtract(createVector(FastMath.cos(alpha), FastMath.sin(alpha), 0, 4)).getNorm().getReal(), 1.0e-10);
        FieldVector3D<DerivativeStructure> ySat = shifted.getRotation().applyInverseTo(createVector(0, 1, 0, 4));
        Assert.assertEquals(0.0, ySat.subtract(createVector(-FastMath.sin(alpha), FastMath.cos(alpha), 0, 4)).getNorm().getReal(), 1.0e-10);
        FieldVector3D<DerivativeStructure> zSat = shifted.getRotation().applyInverseTo(createVector(0, 0, 1, 4));
        Assert.assertEquals(0.0, zSat.subtract(createVector(0, 0, 1, 4)).getNorm().getReal(), 1.0e-10);

    }

    @Test
    public void testToAC() {
        Random random = new Random(0xc9b4cf6c371108e0l);
        for (int i = 0; i < 100; ++i) {
            FieldRotation<DerivativeStructure> r = randomRotation(random);
            FieldVector3D<DerivativeStructure> o = randomVector(random, 1.0e-3);
            FieldVector3D<DerivativeStructure> a = randomVector(random, 1.0e-3);
            FieldAngularCoordinates<DerivativeStructure> acds = new FieldAngularCoordinates<DerivativeStructure>(r, o, a);
            AngularCoordinates ac = acds.toAngularCoordinates();
            Assert.assertEquals(0, Rotation.distance(r.toRotation(), ac.getRotation()), 1.0e-15);
            Assert.assertEquals(0, FieldVector3D.distance(o, ac.getRotationRate()).getReal(), 1.0e-15);
        }
    }

    @Test
    public void testSpin() throws OrekitException {
        double rate = 2 * FastMath.PI / (12 * 60);
        FieldAngularCoordinates<DerivativeStructure> angularCoordinates =
                new FieldAngularCoordinates<DerivativeStructure>(createRotation(0.48, 0.64, 0.36, 0.48, false),
                                       new FieldVector3D<DerivativeStructure>(rate, createVector(0, 0, 1, 4)),
                                       createVector(0, 0, 0, 4));
        Assert.assertEquals(rate, angularCoordinates.getRotationRate().getNorm().getReal(), 1.0e-10);
        double dt = 10.0;
        FieldAngularCoordinates<DerivativeStructure> shifted = angularCoordinates.shiftedBy(dt);
        Assert.assertEquals(rate, shifted.getRotationRate().getNorm().getReal(), 1.0e-10);
        Assert.assertEquals(rate * dt, FieldRotation.distance(angularCoordinates.getRotation(), shifted.getRotation()).getReal(), 1.0e-10);

        FieldVector3D<DerivativeStructure> shiftedX  = shifted.getRotation().applyInverseTo(createVector(1, 0, 0, 4));
        FieldVector3D<DerivativeStructure> shiftedY  = shifted.getRotation().applyInverseTo(createVector(0, 1, 0, 4));
        FieldVector3D<DerivativeStructure> shiftedZ  = shifted.getRotation().applyInverseTo(createVector(0, 0, 1, 4));
        FieldVector3D<DerivativeStructure> originalX = angularCoordinates.getRotation().applyInverseTo(createVector(1, 0, 0, 4));
        FieldVector3D<DerivativeStructure> originalY = angularCoordinates.getRotation().applyInverseTo(createVector(0, 1, 0, 4));
        FieldVector3D<DerivativeStructure> originalZ = angularCoordinates.getRotation().applyInverseTo(createVector(0, 0, 1, 4));
        Assert.assertEquals( FastMath.cos(rate * dt), FieldVector3D.dotProduct(shiftedX, originalX).getReal(), 1.0e-10);
        Assert.assertEquals( FastMath.sin(rate * dt), FieldVector3D.dotProduct(shiftedX, originalY).getReal(), 1.0e-10);
        Assert.assertEquals( 0.0,                 FieldVector3D.dotProduct(shiftedX, originalZ).getReal(), 1.0e-10);
        Assert.assertEquals(-FastMath.sin(rate * dt), FieldVector3D.dotProduct(shiftedY, originalX).getReal(), 1.0e-10);
        Assert.assertEquals( FastMath.cos(rate * dt), FieldVector3D.dotProduct(shiftedY, originalY).getReal(), 1.0e-10);
        Assert.assertEquals( 0.0,                 FieldVector3D.dotProduct(shiftedY, originalZ).getReal(), 1.0e-10);
        Assert.assertEquals( 0.0,                 FieldVector3D.dotProduct(shiftedZ, originalX).getReal(), 1.0e-10);
        Assert.assertEquals( 0.0,                 FieldVector3D.dotProduct(shiftedZ, originalY).getReal(), 1.0e-10);
        Assert.assertEquals( 1.0,                 FieldVector3D.dotProduct(shiftedZ, originalZ).getReal(), 1.0e-10);

        FieldVector3D<DerivativeStructure> forward = FieldAngularCoordinates.estimateRate(angularCoordinates.getRotation(), shifted.getRotation(), dt);
        Assert.assertEquals(0.0, forward.subtract(angularCoordinates.getRotationRate()).getNorm().getReal(), 1.0e-10);

        FieldVector3D<DerivativeStructure> reversed = FieldAngularCoordinates.estimateRate(shifted.getRotation(), angularCoordinates.getRotation(), dt);
        Assert.assertEquals(0.0, reversed.add(angularCoordinates.getRotationRate()).getNorm().getReal(), 1.0e-10);

    }

    @Test
    public void testReverseOffset() {
        Random random = new Random(0x4ecca9d57a8f1611l);
        for (int i = 0; i < 100; ++i) {
            FieldRotation<DerivativeStructure> r = randomRotation(random);
            FieldVector3D<DerivativeStructure> o = randomVector(random, 1.0e-3);
            FieldVector3D<DerivativeStructure> a = randomVector(random, 1.0e-3);
            FieldAngularCoordinates<DerivativeStructure> ac = new FieldAngularCoordinates<DerivativeStructure>(r, o, a);
            FieldAngularCoordinates<DerivativeStructure> sum = ac.addOffset(ac.revert());
            Assert.assertEquals(0.0, sum.getRotation().getAngle().getReal(), 1.0e-15);
            Assert.assertEquals(0.0, sum.getRotationRate().getNorm().getReal(), 1.0e-15);
        }
    }

    @Test
    public void testNoCommute() {
        FieldAngularCoordinates<DerivativeStructure> ac1 =
                new FieldAngularCoordinates<DerivativeStructure>(createRotation(0.48,  0.64, 0.36, 0.48, false),
                                                                 createVector(0, 0, 0, 4), createVector(0, 0, 0, 4));
        FieldAngularCoordinates<DerivativeStructure> ac2 =
                new FieldAngularCoordinates<DerivativeStructure>(createRotation(0.36, -0.48, 0.48, 0.64, false),
                                                                 createVector(0, 0, 0, 4), createVector(0, 0, 0, 4));

        FieldAngularCoordinates<DerivativeStructure> add12 = ac1.addOffset(ac2);
        FieldAngularCoordinates<DerivativeStructure> add21 = ac2.addOffset(ac1);

        // the rotations are really different from each other
        Assert.assertEquals(2.574, FieldRotation.distance(add12.getRotation(), add21.getRotation()).getReal(), 1.0e-3);

    }

    @Test
    public void testRoundTripNoOp() {
        Random random = new Random(0x1e610cfe89306669l);
        for (int i = 0; i < 100; ++i) {

            FieldRotation<DerivativeStructure> r1 = randomRotation(random);
            FieldVector3D<DerivativeStructure> o1 = randomVector(random, 1.0e-2);
            FieldVector3D<DerivativeStructure> a1 = randomVector(random, 1.0e-2);
            FieldAngularCoordinates<DerivativeStructure> ac1 = new FieldAngularCoordinates<DerivativeStructure>(r1, o1, a1);

            FieldRotation<DerivativeStructure> r2 = randomRotation(random);
            FieldVector3D<DerivativeStructure> o2 = randomVector(random, 1.0e-2);
            FieldVector3D<DerivativeStructure> a2 = randomVector(random, 1.0e-2);
            FieldAngularCoordinates<DerivativeStructure> ac2 = new FieldAngularCoordinates<DerivativeStructure>(r2, o2, a2);

            FieldAngularCoordinates<DerivativeStructure> roundTripSA = ac1.subtractOffset(ac2).addOffset(ac2);
            Assert.assertEquals(0.0, FieldRotation.distance(ac1.getRotation(), roundTripSA.getRotation()).getReal(), 1.0e-15);
            Assert.assertEquals(0.0, FieldVector3D.distance(ac1.getRotationRate(), roundTripSA.getRotationRate()).getReal(), 2.0e-17);
            Assert.assertEquals(0.0, FieldVector3D.distance(ac1.getRotationAcceleration(), roundTripSA.getRotationAcceleration()).getReal(), 2.0e-17);

            FieldAngularCoordinates<DerivativeStructure> roundTripAS = ac1.addOffset(ac2).subtractOffset(ac2);
            Assert.assertEquals(0.0, FieldRotation.distance(ac1.getRotation(), roundTripAS.getRotation()).getReal(), 1.0e-15);
            Assert.assertEquals(0.0, FieldVector3D.distance(ac1.getRotationRate(), roundTripAS.getRotationRate()).getReal(), 2.0e-17);
            Assert.assertEquals(0.0, FieldVector3D.distance(ac1.getRotationAcceleration(), roundTripAS.getRotationAcceleration()).getReal(), 2.0e-17);
        }
    }
    
    @Test 
    public void testResultAngularCoordinates() throws OrekitException{
        Field<Decimal64> field = Decimal64Field.getInstance();
        Decimal64 zero = field.getZero();
        FieldVector3D<Decimal64> pos_B = new FieldVector3D<Decimal64> (zero.add(-0.23723922134606962    )  ,
                                                       zero.add(-0.9628700341496187     ),
                                                       zero.add(0.1288365211879871      ));
        FieldVector3D<Decimal64> vel_B = new FieldVector3D<Decimal64> (zero.add(2.6031808214929053E-7   ),
                                                       zero.add(-8.141147978260352E-8   ),
                                                       zero.add(-1.2908618653852553E-7  ));
        FieldVector3D<Decimal64> acc_B = new FieldVector3D<Decimal64>(zero.add( -1.395403347295246E-10  ),
                                                      zero.add( -2.7451871050415643E-12 ),
                                                      zero.add( -2.781723303703499E-10  ) );
       
        FieldPVCoordinates<Decimal64> B = new FieldPVCoordinates<Decimal64>(pos_B, vel_B, acc_B);
       
       
        FieldVector3D<Decimal64> pos_A = new FieldVector3D<Decimal64> (zero.add(-0.44665912825286425    ),
                                                       zero.add(-0.00965737694923173    ),
                                                       zero.add(-0.894652087807798      ));
        FieldVector3D<Decimal64> vel_A = new FieldVector3D<Decimal64> (zero.add(-8.897373390367405E-4   )    ,
                                                       zero.add(2.7825509772757976E-4   )  ,
                                                       zero.add(4.412017757970883E-4    )   );
        FieldVector3D<Decimal64> acc_A = new FieldVector3D<Decimal64>(zero.add( 4.743595125825107E-7    ),
                                                      zero.add( 1.01875177357042E-8     ),
                                                      zero.add( 9.520371766790574E-7    ) );

        FieldPVCoordinates<Decimal64> A = new FieldPVCoordinates<Decimal64>(pos_A, vel_A, acc_A);
       
        FieldPVCoordinates<Decimal64> PLUS_K = new FieldPVCoordinates<Decimal64>(new FieldVector3D<Decimal64>(field.getZero(), field.getZero(), field.getOne()),
                                                                 new FieldVector3D<Decimal64>(field.getZero(), field.getZero(), field.getZero()),
                                                                 new FieldVector3D<Decimal64>(field.getZero(), field.getZero(), field.getZero()));
       
        FieldPVCoordinates<Decimal64> PLUS_J = new FieldPVCoordinates<Decimal64>(new FieldVector3D<Decimal64>(field.getZero(), field.getOne(), field.getZero()),
                                                                 new FieldVector3D<Decimal64>(field.getZero(), field.getZero(), field.getZero()),
                                                                 new FieldVector3D<Decimal64>(field.getZero(), field.getZero(), field.getZero()));
       
       
        FieldAngularCoordinates<Decimal64> fac = new FieldAngularCoordinates<Decimal64>(A, B, PLUS_K, PLUS_J, 1.0e-6);
       
        AngularCoordinates ac = new AngularCoordinates(A.toPVCoordinates(), B.toPVCoordinates(), PLUS_K.toPVCoordinates(), PLUS_J.toPVCoordinates(), 1.0e-6);

        Assert.assertTrue( fac.getRotationRate().toVector3D().equals(ac.getRotationRate()));
        
    }

    private FieldVector3D<DerivativeStructure> randomVector(Random random, double norm) {
        double n = random.nextDouble() * norm;
        double x = random.nextDouble();
        double y = random.nextDouble();
        double z = random.nextDouble();
        return new FieldVector3D<DerivativeStructure>(n, createVector(x, y, z, 4).normalize());
    }

    private FieldRotation<DerivativeStructure> randomRotation(Random random) {
        double q0 = random.nextDouble() * 2 - 1;
        double q1 = random.nextDouble() * 2 - 1;
        double q2 = random.nextDouble() * 2 - 1;
        double q3 = random.nextDouble() * 2 - 1;
        double q  = FastMath.sqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3);
        return createRotation(q0 / q, q1 / q, q2 / q, q3 / q, false);
    }

    private FieldRotation<DerivativeStructure> createRotation(double q0, double q1, double q2, double q3,
                                      boolean needsNormalization) {
        return new FieldRotation<DerivativeStructure>(new DerivativeStructure(4, 1, 0, q0),
                              new DerivativeStructure(4, 1, 1, q1),
                              new DerivativeStructure(4, 1, 2, q2),
                              new DerivativeStructure(4, 1, 3, q3),
                              needsNormalization);
    }

    private FieldVector3D<DerivativeStructure> createVector(double x, double y, double z, int params) {
        return new FieldVector3D<DerivativeStructure>(new DerivativeStructure(params, 1, 0, x),
                              new DerivativeStructure(params, 1, 1, y),
                              new DerivativeStructure(params, 1, 2, z));
    }

}
