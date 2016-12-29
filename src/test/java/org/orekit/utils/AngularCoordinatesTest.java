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


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.hipparchus.exception.MathIllegalArgumentException;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.ODEIntegrator;
import org.hipparchus.ode.ODEState;
import org.hipparchus.ode.ODEStateAndDerivative;
import org.hipparchus.ode.OrdinaryDifferentialEquation;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.ode.sampling.ODEFixedStepHandler;
import org.hipparchus.ode.sampling.StepNormalizer;
import org.hipparchus.random.RandomGenerator;
import org.hipparchus.random.Well1024a;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.hipparchus.util.Precision;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

public class AngularCoordinatesTest {

    @Test
    public void testDefaultConstructor() throws OrekitException {
        AngularCoordinates ac = new AngularCoordinates();
        Assert.assertEquals(0.0, ac.getRotationAcceleration().getNorm(), 1.0e-15);
        Assert.assertEquals(0.0, ac.getRotationRate().getNorm(), 1.0e-15);
        Assert.assertEquals(0.0, Rotation.distance(ac.getRotation(), Rotation.IDENTITY), 1.0e-10);
    }

    @Test
    public void testDerivativesStructuresNeg() throws OrekitException {
        try {
            AngularCoordinates.IDENTITY.toDerivativeStructureRotation(-1);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.OUT_OF_RANGE_DERIVATION_ORDER, oe.getSpecifier());
            Assert.assertEquals(-1, ((Integer) (oe.getParts()[0])).intValue());
        }

    }

    @Test
    public void testDerivativesStructures3() throws OrekitException {
        try {
            AngularCoordinates.IDENTITY.toDerivativeStructureRotation(3);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.OUT_OF_RANGE_DERIVATION_ORDER, oe.getSpecifier());
            Assert.assertEquals(3, ((Integer) (oe.getParts()[0])).intValue());
        }

    }

    @Test
    public void testDerivativesStructures0() throws OrekitException {
        RandomGenerator random = new Well1024a(0x18a0a08fd63f047al);

        Rotation r    = randomRotation(random);
        Vector3D o    = randomVector(random, 1.0e-2);
        Vector3D oDot = randomVector(random, 1.0e-2);
        AngularCoordinates ac = new AngularCoordinates(r, o, oDot);
        AngularCoordinates rebuilt = new AngularCoordinates(ac.toDerivativeStructureRotation(0));
        Assert.assertEquals(0.0, Rotation.distance(ac.getRotation(), rebuilt.getRotation()), 1.0e-15);
        Assert.assertEquals(0.0, rebuilt.getRotationRate().getNorm(), 1.0e-15);
        Assert.assertEquals(0.0, rebuilt.getRotationAcceleration().getNorm(), 1.0e-15);
    }

    @Test
    public void testDerivativesStructures1() throws OrekitException {
        RandomGenerator random = new Well1024a(0x8f8fc6d27bbdc46dl);

        Rotation r    = randomRotation(random);
        Vector3D o    = randomVector(random, 1.0e-2);
        Vector3D oDot = randomVector(random, 1.0e-2);
        AngularCoordinates ac = new AngularCoordinates(r, o, oDot);
        AngularCoordinates rebuilt = new AngularCoordinates(ac.toDerivativeStructureRotation(1));
        Assert.assertEquals(0.0, Rotation.distance(ac.getRotation(), rebuilt.getRotation()), 1.0e-15);
        Assert.assertEquals(0.0, Vector3D.distance(ac.getRotationRate(), rebuilt.getRotationRate()), 1.0e-15);
        Assert.assertEquals(0.0, rebuilt.getRotationAcceleration().getNorm(), 1.0e-15);
    }

    @Test
    public void testDerivativesStructures2() throws OrekitException {
        RandomGenerator random = new Well1024a(0x1633878dddac047dl);

        Rotation r    = randomRotation(random);
        Vector3D o    = randomVector(random, 1.0e-2);
        Vector3D oDot = randomVector(random, 1.0e-2);
        AngularCoordinates ac = new AngularCoordinates(r, o, oDot);
        AngularCoordinates rebuilt = new AngularCoordinates(ac.toDerivativeStructureRotation(2));
        Assert.assertEquals(0.0, Rotation.distance(ac.getRotation(), rebuilt.getRotation()), 1.0e-15);
        Assert.assertEquals(0.0, Vector3D.distance(ac.getRotationRate(), rebuilt.getRotationRate()), 1.0e-15);
        Assert.assertEquals(0.0, Vector3D.distance(ac.getRotationAcceleration(), rebuilt.getRotationAcceleration()), 1.0e-15);
    }

    @Test
    public void testZeroRate() throws OrekitException {
        AngularCoordinates ac =
                new AngularCoordinates(new Rotation(0.48, 0.64, 0.36, 0.48, false), Vector3D.ZERO, Vector3D.ZERO);
        Assert.assertEquals(Vector3D.ZERO, ac.getRotationRate());
        double dt = 10.0;
        AngularCoordinates shifted = ac.shiftedBy(dt);
        Assert.assertEquals(Vector3D.ZERO, shifted.getRotationAcceleration());
        Assert.assertEquals(Vector3D.ZERO, shifted.getRotationRate());
        Assert.assertEquals(0.0, Rotation.distance(ac.getRotation(), shifted.getRotation()), 1.0e-15);
    }

    @Test
    public void testShiftWithoutAcceleration() throws OrekitException {
        double rate = 2 * FastMath.PI / (12 * 60);
        AngularCoordinates ac =
                new AngularCoordinates(Rotation.IDENTITY,
                                       new Vector3D(rate, Vector3D.PLUS_K),
                                       Vector3D.ZERO);
        Assert.assertEquals(rate, ac.getRotationRate().getNorm(), 1.0e-10);
        double dt = 10.0;
        double alpha = rate * dt;
        AngularCoordinates shifted = ac.shiftedBy(dt);
        Assert.assertEquals(rate, shifted.getRotationRate().getNorm(), 1.0e-10);
        Assert.assertEquals(alpha, Rotation.distance(ac.getRotation(), shifted.getRotation()), 1.0e-15);

        Vector3D xSat = shifted.getRotation().applyInverseTo(Vector3D.PLUS_I);
        Assert.assertEquals(0.0, xSat.subtract(new Vector3D(FastMath.cos(alpha), FastMath.sin(alpha), 0)).getNorm(), 1.0e-15);
        Vector3D ySat = shifted.getRotation().applyInverseTo(Vector3D.PLUS_J);
        Assert.assertEquals(0.0, ySat.subtract(new Vector3D(-FastMath.sin(alpha), FastMath.cos(alpha), 0)).getNorm(), 1.0e-15);
        Vector3D zSat = shifted.getRotation().applyInverseTo(Vector3D.PLUS_K);
        Assert.assertEquals(0.0, zSat.subtract(Vector3D.PLUS_K).getNorm(), 1.0e-15);

    }

    @Test
    public void testShiftWithAcceleration() throws OrekitException {
        double rate = 2 * FastMath.PI / (12 * 60);
        double acc  = 0.001;
        double dt   = 1.0;
        int    n    = 2000;
        final AngularCoordinates quadratic =
                new AngularCoordinates(Rotation.IDENTITY,
                                       new Vector3D(rate, Vector3D.PLUS_K),
                                       new Vector3D(acc,  Vector3D.PLUS_J));
        final AngularCoordinates linear =
                new AngularCoordinates(quadratic.getRotation(), quadratic.getRotationRate(), Vector3D.ZERO);

        final OrdinaryDifferentialEquation ode = new OrdinaryDifferentialEquation() {
            public int getDimension() {
                return 4;
            }
            public double[] computeDerivatives(final double t, final double[] q) {
                final double omegaX = quadratic.getRotationRate().getX() + t * quadratic.getRotationAcceleration().getX();
                final double omegaY = quadratic.getRotationRate().getY() + t * quadratic.getRotationAcceleration().getY();
                final double omegaZ = quadratic.getRotationRate().getZ() + t * quadratic.getRotationAcceleration().getZ();
                return new double[] {
                    0.5 * MathArrays.linearCombination(-q[1], omegaX, -q[2], omegaY, -q[3], omegaZ),
                    0.5 * MathArrays.linearCombination( q[0], omegaX, -q[3], omegaY,  q[2], omegaZ),
                    0.5 * MathArrays.linearCombination( q[3], omegaX,  q[0], omegaY, -q[1], omegaZ),
                    0.5 * MathArrays.linearCombination(-q[2], omegaX,  q[1], omegaY,  q[0], omegaZ)
                };
            }
        };
        ODEIntegrator integrator = new DormandPrince853Integrator(1.0e-6, 1.0, 1.0e-12, 1.0e-12);
        integrator.addStepHandler(new StepNormalizer(dt / n, new ODEFixedStepHandler() {
            public void handleStep(ODEStateAndDerivative s, boolean isLast) {
                final double   t = s.getTime();
                final double[] y = s.getPrimaryState();
                Rotation reference = new Rotation(y[0], y[1], y[2], y[3], true);

                // the error in shiftedBy taking acceleration into account is cubic
                double expectedCubicError     = 1.4544e-6 * t * t * t;
                Assert.assertEquals(expectedCubicError,
                                    Rotation.distance(reference, quadratic.shiftedBy(t).getRotation()),
                                    0.0001 * expectedCubicError);

                // the error in shiftedBy not taking acceleration into account is quadratic
                double expectedQuadraticError = 5.0e-4 * t * t;
                Assert.assertEquals(expectedQuadraticError,
                                    Rotation.distance(reference, linear.shiftedBy(t).getRotation()),
                                    0.00001 * expectedQuadraticError);

            }
        }));

        double[] y = new double[] {
            quadratic.getRotation().getQ0(),
            quadratic.getRotation().getQ1(),
            quadratic.getRotation().getQ2(),
            quadratic.getRotation().getQ3()
        };
        integrator.integrate(ode, new ODEState(0, y), dt);

    }

    @Test
    public void testSpin() throws OrekitException {
        double rate = 2 * FastMath.PI / (12 * 60);
        AngularCoordinates angularCoordinates =
                new AngularCoordinates(new Rotation(0.48, 0.64, 0.36, 0.48, false),
                                       new Vector3D(rate, Vector3D.PLUS_K));
        Assert.assertEquals(rate, angularCoordinates.getRotationRate().getNorm(), 1.0e-10);
        double dt = 10.0;
        AngularCoordinates shifted = angularCoordinates.shiftedBy(dt);
        Assert.assertEquals(rate, shifted.getRotationRate().getNorm(), 1.0e-10);
        Assert.assertEquals(rate * dt, Rotation.distance(angularCoordinates.getRotation(), shifted.getRotation()), 1.0e-10);

        Vector3D shiftedX  = shifted.getRotation().applyInverseTo(Vector3D.PLUS_I);
        Vector3D shiftedY  = shifted.getRotation().applyInverseTo(Vector3D.PLUS_J);
        Vector3D shiftedZ  = shifted.getRotation().applyInverseTo(Vector3D.PLUS_K);
        Vector3D originalX = angularCoordinates.getRotation().applyInverseTo(Vector3D.PLUS_I);
        Vector3D originalY = angularCoordinates.getRotation().applyInverseTo(Vector3D.PLUS_J);
        Vector3D originalZ = angularCoordinates.getRotation().applyInverseTo(Vector3D.PLUS_K);
        Assert.assertEquals( FastMath.cos(rate * dt), Vector3D.dotProduct(shiftedX, originalX), 1.0e-10);
        Assert.assertEquals( FastMath.sin(rate * dt), Vector3D.dotProduct(shiftedX, originalY), 1.0e-10);
        Assert.assertEquals( 0.0,                 Vector3D.dotProduct(shiftedX, originalZ), 1.0e-10);
        Assert.assertEquals(-FastMath.sin(rate * dt), Vector3D.dotProduct(shiftedY, originalX), 1.0e-10);
        Assert.assertEquals( FastMath.cos(rate * dt), Vector3D.dotProduct(shiftedY, originalY), 1.0e-10);
        Assert.assertEquals( 0.0,                 Vector3D.dotProduct(shiftedY, originalZ), 1.0e-10);
        Assert.assertEquals( 0.0,                 Vector3D.dotProduct(shiftedZ, originalX), 1.0e-10);
        Assert.assertEquals( 0.0,                 Vector3D.dotProduct(shiftedZ, originalY), 1.0e-10);
        Assert.assertEquals( 1.0,                 Vector3D.dotProduct(shiftedZ, originalZ), 1.0e-10);

        Vector3D forward = AngularCoordinates.estimateRate(angularCoordinates.getRotation(), shifted.getRotation(), dt);
        Assert.assertEquals(0.0, forward.subtract(angularCoordinates.getRotationRate()).getNorm(), 1.0e-10);

        Vector3D reversed = AngularCoordinates.estimateRate(shifted.getRotation(), angularCoordinates.getRotation(), dt);
        Assert.assertEquals(0.0, reversed.add(angularCoordinates.getRotationRate()).getNorm(), 1.0e-10);

    }

    @Test
    public void testReverseOffset() {
        RandomGenerator random = new Well1024a(0x4ecca9d57a8f1611l);
        for (int i = 0; i < 100; ++i) {
            Rotation r = randomRotation(random);
            Vector3D o = randomVector(random, 1.0e-3);
            AngularCoordinates ac = new AngularCoordinates(r, o);
            AngularCoordinates sum = ac.addOffset(ac.revert());
            Assert.assertEquals(0.0, sum.getRotation().getAngle(), 1.0e-15);
            Assert.assertEquals(0.0, sum.getRotationRate().getNorm(), 1.0e-15);
            Assert.assertEquals(0.0, sum.getRotationAcceleration().getNorm(), 1.0e-15);
        }
    }

    @Test
    public void testNoCommute() {
        AngularCoordinates ac1 =
                new AngularCoordinates(new Rotation(0.48,  0.64, 0.36, 0.48, false), Vector3D.ZERO);
        AngularCoordinates ac2 =
                new AngularCoordinates(new Rotation(0.36, -0.48, 0.48, 0.64, false), Vector3D.ZERO);

        AngularCoordinates add12 = ac1.addOffset(ac2);
        AngularCoordinates add21 = ac2.addOffset(ac1);

        // the rotations are really different from each other
        Assert.assertEquals(2.574, Rotation.distance(add12.getRotation(), add21.getRotation()), 1.0e-3);

    }

    @Test
    public void testRoundTripNoOp() {
        RandomGenerator random = new Well1024a(0x1e610cfe89306669l);
        for (int i = 0; i < 100; ++i) {

            Rotation r1    = randomRotation(random);
            Vector3D o1    = randomVector(random, 1.0e-2);
            Vector3D oDot1 = randomVector(random, 1.0e-2);
            AngularCoordinates ac1 = new AngularCoordinates(r1, o1, oDot1);

            Rotation r2    = randomRotation(random);
            Vector3D o2    = randomVector(random, 1.0e-2);
            Vector3D oDot2 = randomVector(random, 1.0e-2);
            AngularCoordinates ac2 = new AngularCoordinates(r2, o2, oDot2);

            AngularCoordinates roundTripSA = ac1.subtractOffset(ac2).addOffset(ac2);
            Assert.assertEquals(0.0, Rotation.distance(ac1.getRotation(), roundTripSA.getRotation()), 5.0e-16);
            Assert.assertEquals(0.0, Vector3D.distance(ac1.getRotationRate(), roundTripSA.getRotationRate()), 2.0e-17);
            Assert.assertEquals(0.0, Vector3D.distance(ac1.getRotationAcceleration(), roundTripSA.getRotationAcceleration()), 2.0e-17);

            AngularCoordinates roundTripAS = ac1.addOffset(ac2).subtractOffset(ac2);
            Assert.assertEquals(0.0, Rotation.distance(ac1.getRotation(), roundTripAS.getRotation()), 5.0e-16);
            Assert.assertEquals(0.0, Vector3D.distance(ac1.getRotationRate(), roundTripAS.getRotationRate()), 2.0e-17);
            Assert.assertEquals(0.0, Vector3D.distance(ac1.getRotationAcceleration(), roundTripAS.getRotationAcceleration()), 2.0e-17);

        }
    }

    @Test
    public void testRodriguesSymmetry() {

        // check the two-way conversion result in identity
        RandomGenerator random = new Well1024a(0xb1e615aaa8236b52l);
        for (int i = 0; i < 1000; ++i) {
            Rotation rotation             = randomRotation(random);
            Vector3D rotationRate         = randomVector(random, 0.01);
            Vector3D rotationAcceleration = randomVector(random, 0.01);
            AngularCoordinates ac         = new AngularCoordinates(rotation, rotationRate, rotationAcceleration);
            AngularCoordinates rebuilt    = AngularCoordinates.createFromModifiedRodrigues(ac.getModifiedRodrigues(1.0));
            Assert.assertEquals(0.0, Rotation.distance(rotation, rebuilt.getRotation()), 1.0e-14);
            Assert.assertEquals(0.0, Vector3D.distance(rotationRate, rebuilt.getRotationRate()), 1.0e-15);
            Assert.assertEquals(0.0, Vector3D.distance(rotationAcceleration, rebuilt.getRotationAcceleration()), 1.0e-15);
        }

    }

    @Test
    public void testRodriguesSpecialCases() {

        // identity
        double[][] identity = new AngularCoordinates(Rotation.IDENTITY, Vector3D.ZERO, Vector3D.ZERO).getModifiedRodrigues(1.0);
        for (double[] row : identity) {
            for (double element : row) {
                Assert.assertEquals(0.0, element, Precision.SAFE_MIN);
            }
        }
        AngularCoordinates acId = AngularCoordinates.createFromModifiedRodrigues(identity);
        Assert.assertEquals(0.0, acId.getRotation().getAngle(), Precision.SAFE_MIN);
        Assert.assertEquals(0.0, acId.getRotationRate().getNorm(), Precision.SAFE_MIN);

        // PI angle rotation (which is singular for non-modified Rodrigues vector)
        RandomGenerator random = new Well1024a(0x2158523e6accb859l);
        for (int i = 0; i < 100; ++i) {
            Vector3D axis = randomVector(random, 1.0);
            AngularCoordinates original = new AngularCoordinates(new Rotation(axis, FastMath.PI, RotationConvention.VECTOR_OPERATOR),
                                                                 Vector3D.ZERO, Vector3D.ZERO);
            AngularCoordinates rebuilt = AngularCoordinates.createFromModifiedRodrigues(original.getModifiedRodrigues(1.0));
            Assert.assertEquals(FastMath.PI, rebuilt.getRotation().getAngle(), 1.0e-15);
            Assert.assertEquals(0.0, FastMath.sin(Vector3D.angle(axis, rebuilt.getRotation().getAxis(RotationConvention.VECTOR_OPERATOR))), 1.0e-15);
            Assert.assertEquals(0.0, rebuilt.getRotationRate().getNorm(), 1.0e-16);
        }

    }

    @Test
    public void testInverseCrossProducts()
        throws OrekitException {
        checkInverse(Vector3D.PLUS_K, Vector3D.PLUS_I, Vector3D.PLUS_J);
        checkInverse(Vector3D.ZERO,   Vector3D.ZERO,   Vector3D.ZERO);
        checkInverse(Vector3D.ZERO,   Vector3D.ZERO,   Vector3D.PLUS_J);
        checkInverse(Vector3D.PLUS_K, Vector3D.PLUS_K, Vector3D.PLUS_J);
        checkInverse(Vector3D.ZERO,   Vector3D.PLUS_K, Vector3D.ZERO);
        checkInverse(Vector3D.PLUS_K, Vector3D.PLUS_I, Vector3D.PLUS_K);
        checkInverse(Vector3D.PLUS_K, Vector3D.PLUS_I, Vector3D.PLUS_I);
        checkInverse(Vector3D.PLUS_K, Vector3D.PLUS_I, new Vector3D(1, 0, -1).normalize());
        checkInverse(Vector3D.ZERO, Vector3D.PLUS_I, Vector3D.ZERO,   Vector3D.PLUS_J,  Vector3D.ZERO);
    }

    @Test
    public void testInverseCrossProductsFailures() {
        checkInverseFailure(Vector3D.PLUS_K, Vector3D.ZERO,   Vector3D.PLUS_J, Vector3D.PLUS_I,  Vector3D.PLUS_K);
        checkInverseFailure(Vector3D.PLUS_K, Vector3D.ZERO,   Vector3D.ZERO,   Vector3D.ZERO,    Vector3D.PLUS_K);
        checkInverseFailure(Vector3D.PLUS_I, Vector3D.PLUS_I, Vector3D.ZERO,   Vector3D.MINUS_I, Vector3D.PLUS_K);
        checkInverseFailure(Vector3D.PLUS_I, Vector3D.PLUS_I, Vector3D.ZERO,   Vector3D.PLUS_J,  Vector3D.PLUS_J);
        checkInverseFailure(Vector3D.PLUS_I, Vector3D.PLUS_I, Vector3D.PLUS_J, Vector3D.PLUS_J,  Vector3D.ZERO);
        checkInverseFailure(Vector3D.PLUS_I, Vector3D.PLUS_I, Vector3D.PLUS_J, Vector3D.ZERO,    Vector3D.PLUS_J);
    }

    @Test
    public void testRandomInverseCrossProducts() throws OrekitException {
        RandomGenerator generator = new Well1024a(0x52b29d8f6ac2d64bl);
        for (int i = 0; i < 10000; ++i) {
            Vector3D omega = randomVector(generator, 10 * generator.nextDouble() + 1.0);
            Vector3D v1    = randomVector(generator, 10 * generator.nextDouble() + 1.0);
            Vector3D v2    = randomVector(generator, 10 * generator.nextDouble() + 1.0);
            checkInverse(omega, v1, v2);
        }
    }

    private void checkInverse(Vector3D omega, Vector3D v1, Vector3D v2) throws OrekitException {
        checkInverse(omega,
                     v1, Vector3D.crossProduct(omega, v1),
                     v2, Vector3D.crossProduct(omega, v2));
    }

    private void checkInverseFailure(Vector3D omega, Vector3D v1, Vector3D c1, Vector3D v2, Vector3D c2) {
        try {
            checkInverse(omega, v1, c1, v2, c2);
            Assert.fail("an exception should have been thrown");
        } catch (MathIllegalArgumentException miae) {
            // expected
        }
    }

    private void checkInverse(Vector3D omega, Vector3D v1, Vector3D c1, Vector3D v2, Vector3D c2)
        throws MathIllegalArgumentException {
        try {
            Method inverse;
            inverse = AngularCoordinates.class.getDeclaredMethod("inverseCrossProducts",
                                                                 Vector3D.class, Vector3D.class,
                                                                 Vector3D.class, Vector3D.class,
                                                                 double.class);
            inverse.setAccessible(true);
            Vector3D rebuilt = (Vector3D) inverse.invoke(null, v1, c1, v2, c2, 1.0e-9);
            Assert.assertEquals(0.0, Vector3D.distance(omega, rebuilt), 5.0e-12 * omega.getNorm());
        } catch (NoSuchMethodException e) {
            Assert.fail(e.getLocalizedMessage());
        } catch (SecurityException e) {
            Assert.fail(e.getLocalizedMessage());
        } catch (IllegalAccessException e) {
            Assert.fail(e.getLocalizedMessage());
        } catch (IllegalArgumentException e) {
            Assert.fail(e.getLocalizedMessage());
        } catch (InvocationTargetException e) {
            throw (MathIllegalArgumentException) e.getCause();
        }
    }

    @Test
    public void testRandomPVCoordinates() throws OrekitException {
        RandomGenerator generator = new Well1024a(0x49eb5b92d1f94b89l);
        for (int i = 0; i < 100; ++i) {
            Rotation r           = randomRotation(generator);
            Vector3D omega       = randomVector(generator, 10    * generator.nextDouble() + 1.0);
            Vector3D omegaDot    = randomVector(generator, 0.1   * generator.nextDouble() + 0.01);
            AngularCoordinates ref = new AngularCoordinates(r, omega, omegaDot);
            AngularCoordinates inv = ref.revert();
            for (int j = 0; j < 100; ++j) {
                PVCoordinates v1 = randomPVCoordinates(generator, 1000, 1.0, 0.001);
                PVCoordinates v2 = randomPVCoordinates(generator, 1000, 1.0, 0.0010);
                PVCoordinates u1 = inv.applyTo(v1);
                PVCoordinates u2 = inv.applyTo(v2);
                AngularCoordinates rebuilt = new AngularCoordinates(u1, u2, v1, v2, 1.0e-9);
                Assert.assertEquals(0.0,
                                    Rotation.distance(r, rebuilt.getRotation()),
                                    4.0e-14);
                Assert.assertEquals(0.0,
                                    Vector3D.distance(omega, rebuilt.getRotationRate()),
                                    3.0e-12 * omega.getNorm());
                Assert.assertEquals(0.0,
                                    Vector3D.distance(omegaDot, rebuilt.getRotationAcceleration()),
                                    2.0e-6 * omegaDot.getNorm());
            }
        }
    }

    @Test
    public void testCancellingDerivatives() throws OrekitException {
        PVCoordinates u1 = new PVCoordinates(new Vector3D(-0.4466591282528639,   -0.009657376949231283,  -0.894652087807798),
                                             new Vector3D(-8.897296517803556E-4,  2.7825250920407674E-4,  4.411979658413134E-4),
                                             new Vector3D( 4.753127475302486E-7,  1.0209400376727623E-8,  9.515403756524403E-7));
        PVCoordinates u2 = new PVCoordinates(new Vector3D( 0.23723907259910096,   0.9628700806685033,    -0.1288364474275361),
                                             new Vector3D(-7.98741002062555E-24,  2.4979687659429984E-24, 3.9607863426704016E-24),
                                             new Vector3D(-3.150541868418562E-23, 9.856329862034835E-24,  1.5648124883326986E-23));
        PVCoordinates v1 = new PVCoordinates(Vector3D.PLUS_K, Vector3D.ZERO, Vector3D.ZERO);
        PVCoordinates v2 = new PVCoordinates(Vector3D.MINUS_J, Vector3D.ZERO, Vector3D.ZERO);
        AngularCoordinates ac = new AngularCoordinates(u1, u2, v1, v2, 1.0e-9);
        PVCoordinates v1Computed = ac.applyTo(u1);
        PVCoordinates v2Computed = ac.applyTo(u2);
        Assert.assertEquals(0, Vector3D.distance(v1.getPosition(),     v1Computed.getPosition()),     1.0e-15);
        Assert.assertEquals(0, Vector3D.distance(v2.getPosition(),     v2Computed.getPosition()),     1.0e-15);
        Assert.assertEquals(0, Vector3D.distance(v1.getVelocity(),     v1Computed.getVelocity()),     1.0e-15);
        Assert.assertEquals(0, Vector3D.distance(v2.getVelocity(),     v2Computed.getVelocity()),     1.0e-15);
        Assert.assertEquals(0, Vector3D.distance(v1.getAcceleration(), v1Computed.getAcceleration()), 1.0e-15);
        Assert.assertEquals(0, Vector3D.distance(v2.getAcceleration(), v2Computed.getAcceleration()), 1.0e-15);
    }

    private Vector3D randomVector(RandomGenerator random, double norm) {
        double n = random.nextDouble() * norm;
        double x = random.nextDouble();
        double y = random.nextDouble();
        double z = random.nextDouble();
        return new Vector3D(n, new Vector3D(x, y, z).normalize());
    }

    private PVCoordinates randomPVCoordinates(RandomGenerator random,
                                              double norm0, double norm1, double norm2) {
        Vector3D p0 = randomVector(random, norm0);
        Vector3D p1 = randomVector(random, norm1);
        Vector3D p2 = randomVector(random, norm2);
        return new PVCoordinates(p0, p1, p2);
    }

    private Rotation randomRotation(RandomGenerator random) {
        double q0 = random.nextDouble() * 2 - 1;
        double q1 = random.nextDouble() * 2 - 1;
        double q2 = random.nextDouble() * 2 - 1;
        double q3 = random.nextDouble() * 2 - 1;
        double q  = FastMath.sqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3);
        return new Rotation(q0 / q, q1 / q, q2 / q, q3 / q, false);
    }

}

