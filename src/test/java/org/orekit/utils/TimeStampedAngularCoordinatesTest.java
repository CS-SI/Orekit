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


import java.util.ArrayList;
import java.util.List;

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
import org.junit.Assert;
import org.junit.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;

public class TimeStampedAngularCoordinatesTest {

    @Test
    public void testZeroRate() throws OrekitException {
        TimeStampedAngularCoordinates ac =
                new TimeStampedAngularCoordinates(AbsoluteDate.J2000_EPOCH,
                                                  new Rotation(0.48, 0.64, 0.36, 0.48, false),
                                                  Vector3D.ZERO, Vector3D.ZERO);
        Assert.assertEquals(Vector3D.ZERO, ac.getRotationRate());
        double dt = 10.0;
        TimeStampedAngularCoordinates shifted = ac.shiftedBy(dt);
        Assert.assertEquals(Vector3D.ZERO, shifted.getRotationAcceleration());
        Assert.assertEquals(Vector3D.ZERO, shifted.getRotationRate());
        Assert.assertEquals(0.0, Rotation.distance(ac.getRotation(), shifted.getRotation()), 1.0e-15);
    }

    @Test
    public void testTwoPairs() throws OrekitException, java.io.IOException {
        RandomGenerator random = new Well1024a(0x976ad943966c9f00l);

        for (int i = 0; i < 20; ++i) {

            Rotation r = randomRotation(random);
            Vector3D o = randomVector(random, 1.0e-2);
            Vector3D a = randomVector(random, 1.0e-2);
            TimeStampedAngularCoordinates reference = new TimeStampedAngularCoordinates(AbsoluteDate.J2000_EPOCH, r, o, a);

            PVCoordinates u1 = randomPVCoordinates(random, 1000, 1.0, 0.001);
            PVCoordinates u2 = randomPVCoordinates(random, 1000, 1.0, 0.001);
            PVCoordinates v1 = reference.applyTo(u1);
            PVCoordinates v2 = reference.applyTo(u2);
            TimeStampedAngularCoordinates ac =
                    new TimeStampedAngularCoordinates(AbsoluteDate.J2000_EPOCH, u1, u2, v1, v2, 1.0e-9);

            Assert.assertEquals(0, Vector3D.distance(v1.getPosition().normalize(), ac.applyTo(u1).getPosition().normalize()), 1.0e-14);
            Assert.assertEquals(0, Vector3D.distance(v1.getVelocity().normalize(), ac.applyTo(u1).getVelocity().normalize()), 1.0e-14);
            Assert.assertEquals(0, Vector3D.distance(v1.getAcceleration().normalize(), ac.applyTo(u1).getAcceleration().normalize()), 1.0e-14);
            Assert.assertEquals(0, Vector3D.distance(v2.getPosition().normalize(), ac.applyTo(u2).getPosition().normalize()), 1.0e-14);
            Assert.assertEquals(0, Vector3D.distance(v2.getVelocity().normalize(), ac.applyTo(u2).getVelocity().normalize()), 1.0e-14);
            Assert.assertEquals(0, Vector3D.distance(v2.getAcceleration().normalize(), ac.applyTo(u2).getAcceleration().normalize()), 1.0e-14);

        }

    }

    @Test
    public void testDerivativesStructures2() throws OrekitException {
        RandomGenerator random = new Well1024a(0x75fbebbdbf127b3dl);

        Rotation r    = randomRotation(random);
        Vector3D o    = randomVector(random, 1.0e-2);
        Vector3D oDot = randomVector(random, 1.0e-2);
        TimeStampedAngularCoordinates ac = new TimeStampedAngularCoordinates(AbsoluteDate.J2000_EPOCH,
                                                                             r, o, oDot);
        TimeStampedAngularCoordinates rebuilt =
                new TimeStampedAngularCoordinates(AbsoluteDate.J2000_EPOCH,
                                                  ac.toDerivativeStructureRotation(2));
        Assert.assertEquals(0.0, Rotation.distance(ac.getRotation(), rebuilt.getRotation()), 1.0e-15);
        Assert.assertEquals(0.0, Vector3D.distance(ac.getRotationRate(), rebuilt.getRotationRate()), 1.0e-15);
        Assert.assertEquals(0.0, Vector3D.distance(ac.getRotationAcceleration(), rebuilt.getRotationAcceleration()), 1.0e-15);
    }

    @Test
    public void testShift() throws OrekitException {
        double rate = 2 * FastMath.PI / (12 * 60);
        TimeStampedAngularCoordinates ac =
                new TimeStampedAngularCoordinates(AbsoluteDate.J2000_EPOCH,
                                                  Rotation.IDENTITY,
                                                  new Vector3D(rate, Vector3D.PLUS_K), Vector3D.ZERO);
        Assert.assertEquals(rate, ac.getRotationRate().getNorm(), 1.0e-10);
        double dt = 10.0;
        double alpha = rate * dt;
        TimeStampedAngularCoordinates shifted = ac.shiftedBy(dt);
        Assert.assertEquals(rate, shifted.getRotationRate().getNorm(), 1.0e-10);
        Assert.assertEquals(alpha, Rotation.distance(ac.getRotation(), shifted.getRotation()), 1.0e-10);

        Vector3D xSat = shifted.getRotation().applyInverseTo(Vector3D.PLUS_I);
        Assert.assertEquals(0.0, xSat.subtract(new Vector3D(FastMath.cos(alpha), FastMath.sin(alpha), 0)).getNorm(), 1.0e-10);
        Vector3D ySat = shifted.getRotation().applyInverseTo(Vector3D.PLUS_J);
        Assert.assertEquals(0.0, ySat.subtract(new Vector3D(-FastMath.sin(alpha), FastMath.cos(alpha), 0)).getNorm(), 1.0e-10);
        Vector3D zSat = shifted.getRotation().applyInverseTo(Vector3D.PLUS_K);
        Assert.assertEquals(0.0, zSat.subtract(Vector3D.PLUS_K).getNorm(), 1.0e-10);

    }

    @Test
    public void testSpin() throws OrekitException {
        double rate = 2 * FastMath.PI / (12 * 60);
        TimeStampedAngularCoordinates ac =
                new TimeStampedAngularCoordinates(AbsoluteDate.J2000_EPOCH,
                                                  new Rotation(0.48, 0.64, 0.36, 0.48, false),
                                                  new Vector3D(rate, Vector3D.PLUS_K), Vector3D.ZERO);
        Assert.assertEquals(rate, ac.getRotationRate().getNorm(), 1.0e-10);
        double dt = 10.0;
        TimeStampedAngularCoordinates shifted = ac.shiftedBy(dt);
        Assert.assertEquals(rate, shifted.getRotationRate().getNorm(), 1.0e-10);
        Assert.assertEquals(rate * dt, Rotation.distance(ac.getRotation(), shifted.getRotation()), 1.0e-10);

        Vector3D shiftedX  = shifted.getRotation().applyInverseTo(Vector3D.PLUS_I);
        Vector3D shiftedY  = shifted.getRotation().applyInverseTo(Vector3D.PLUS_J);
        Vector3D shiftedZ  = shifted.getRotation().applyInverseTo(Vector3D.PLUS_K);
        Vector3D originalX = ac.getRotation().applyInverseTo(Vector3D.PLUS_I);
        Vector3D originalY = ac.getRotation().applyInverseTo(Vector3D.PLUS_J);
        Vector3D originalZ = ac.getRotation().applyInverseTo(Vector3D.PLUS_K);
        Assert.assertEquals( FastMath.cos(rate * dt), Vector3D.dotProduct(shiftedX, originalX), 1.0e-10);
        Assert.assertEquals( FastMath.sin(rate * dt), Vector3D.dotProduct(shiftedX, originalY), 1.0e-10);
        Assert.assertEquals( 0.0,                 Vector3D.dotProduct(shiftedX, originalZ), 1.0e-10);
        Assert.assertEquals(-FastMath.sin(rate * dt), Vector3D.dotProduct(shiftedY, originalX), 1.0e-10);
        Assert.assertEquals( FastMath.cos(rate * dt), Vector3D.dotProduct(shiftedY, originalY), 1.0e-10);
        Assert.assertEquals( 0.0,                 Vector3D.dotProduct(shiftedY, originalZ), 1.0e-10);
        Assert.assertEquals( 0.0,                 Vector3D.dotProduct(shiftedZ, originalX), 1.0e-10);
        Assert.assertEquals( 0.0,                 Vector3D.dotProduct(shiftedZ, originalY), 1.0e-10);
        Assert.assertEquals( 1.0,                 Vector3D.dotProduct(shiftedZ, originalZ), 1.0e-10);

        Vector3D forward = TimeStampedAngularCoordinates.estimateRate(ac.getRotation(), shifted.getRotation(), dt);
        Assert.assertEquals(0.0, forward.subtract(ac.getRotationRate()).getNorm(), 1.0e-10);

        Vector3D reversed = TimeStampedAngularCoordinates.estimateRate(shifted.getRotation(), ac.getRotation(), dt);
        Assert.assertEquals(0.0, reversed.add(ac.getRotationRate()).getNorm(), 1.0e-10);

    }

    @Test
    public void testReverseOffset() {
        RandomGenerator random = new Well1024a(0x4ecca9d57a8f1611l);
        for (int i = 0; i < 100; ++i) {
            Rotation r = randomRotation(random);
            Vector3D o = randomVector(random, 1.0e-3);
            Vector3D a = randomVector(random, 1.0e-3);
            TimeStampedAngularCoordinates ac = new TimeStampedAngularCoordinates(AbsoluteDate.J2000_EPOCH, r, o, a);
            TimeStampedAngularCoordinates sum = ac.addOffset(ac.revert());
            Assert.assertEquals(0.0, sum.getRotation().getAngle(), 1.0e-15);
            Assert.assertEquals(0.0, sum.getRotationRate().getNorm(), 1.0e-15);
            Assert.assertEquals(0.0, sum.getRotationAcceleration().getNorm(), 1.0e-15);
        }
    }

    @Test
    public void testNoCommute() {
        TimeStampedAngularCoordinates ac1 =
	    new TimeStampedAngularCoordinates(AbsoluteDate.J2000_EPOCH, new Rotation(0.48,  0.64, 0.36, 0.48, false), Vector3D.ZERO, Vector3D.ZERO);
        TimeStampedAngularCoordinates ac2 =
	    new TimeStampedAngularCoordinates(AbsoluteDate.J2000_EPOCH, new Rotation(0.36, -0.48, 0.48, 0.64, false), Vector3D.ZERO, Vector3D.ZERO);

        TimeStampedAngularCoordinates add12 = ac1.addOffset(ac2);
        TimeStampedAngularCoordinates add21 = ac2.addOffset(ac1);

        // the rotations are really different from each other
        Assert.assertEquals(2.574, Rotation.distance(add12.getRotation(), add21.getRotation()), 1.0e-3);

    }

    @Test
    public void testRoundTripNoOp() {
        RandomGenerator random = new Well1024a(0x1e610cfe89306669l);
        for (int i = 0; i < 100; ++i) {

            Rotation r1 = randomRotation(random);
            Vector3D o1 = randomVector(random, 1.0e-2);
            Vector3D a1 = randomVector(random, 1.0e-2);
            TimeStampedAngularCoordinates ac1 = new TimeStampedAngularCoordinates(AbsoluteDate.J2000_EPOCH, r1, o1, a1);
            Rotation r2 = randomRotation(random);
            Vector3D o2 = randomVector(random, 1.0e-2);
            Vector3D a2 = randomVector(random, 1.0e-2);

            TimeStampedAngularCoordinates ac2 = new TimeStampedAngularCoordinates(AbsoluteDate.J2000_EPOCH, r2, o2, a2);
            TimeStampedAngularCoordinates roundTripSA = ac1.subtractOffset(ac2).addOffset(ac2);
            Assert.assertEquals(0.0, Rotation.distance(ac1.getRotation(), roundTripSA.getRotation()), 4.0e-16);
            Assert.assertEquals(0.0, Vector3D.distance(ac1.getRotationRate(), roundTripSA.getRotationRate()), 2.0e-17);
            Assert.assertEquals(0.0, Vector3D.distance(ac1.getRotationAcceleration(), roundTripSA.getRotationAcceleration()), 1.0e-17);

            TimeStampedAngularCoordinates roundTripAS = ac1.addOffset(ac2).subtractOffset(ac2);
            Assert.assertEquals(0.0, Rotation.distance(ac1.getRotation(), roundTripAS.getRotation()), 6.0e-16);
            Assert.assertEquals(0.0, Vector3D.distance(ac1.getRotationRate(), roundTripAS.getRotationRate()), 2.0e-17);
            Assert.assertEquals(0.0, Vector3D.distance(ac1.getRotationAcceleration(), roundTripAS.getRotationAcceleration()), 2.0e-17);

        }
    }

    @Test
    public void testInterpolationAroundPI() throws OrekitException {

        List<TimeStampedAngularCoordinates> sample = new ArrayList<TimeStampedAngularCoordinates>();

        // add angular coordinates at t0: 179.999 degrees rotation along X axis
        AbsoluteDate t0 = new AbsoluteDate("2012-01-01T00:00:00.000", TimeScalesFactory.getTAI());
        TimeStampedAngularCoordinates ac0 = new TimeStampedAngularCoordinates(t0,
                                                                              new Rotation(Vector3D.PLUS_I,
                                                                                           FastMath.toRadians(179.999),
                                                                                           RotationConvention.VECTOR_OPERATOR),
                                                                              new Vector3D(FastMath.toRadians(0), 0, 0),
                                                                              Vector3D.ZERO);
        sample.add(ac0);

        // add angular coordinates at t1: -179.999 degrees rotation (= 180.001 degrees) along X axis
        AbsoluteDate t1 = new AbsoluteDate("2012-01-01T00:00:02.000", TimeScalesFactory.getTAI());
        TimeStampedAngularCoordinates ac1 = new TimeStampedAngularCoordinates(t1,
                                                                              new Rotation(Vector3D.PLUS_I,
                                                                                           FastMath.toRadians(-179.999),
                                                                                           RotationConvention.VECTOR_OPERATOR),
                                                                              new Vector3D(FastMath.toRadians(0), 0, 0),
                                                                              Vector3D.ZERO);
        sample.add(ac1);

        // get interpolated angular coordinates at mid time between t0 and t1
        AbsoluteDate t = new AbsoluteDate("2012-01-01T00:00:01.000", TimeScalesFactory.getTAI());
        TimeStampedAngularCoordinates interpolated =
                TimeStampedAngularCoordinates.interpolate(t, AngularDerivativesFilter.USE_R, sample);

        Assert.assertEquals(FastMath.toRadians(180), interpolated.getRotation().getAngle(), 1.0e-12);

    }

    @Test
    public void testInterpolationWithoutAcceleration() throws OrekitException {
        AbsoluteDate date = AbsoluteDate.GALILEO_EPOCH;
        double alpha0 = 0.5   * FastMath.PI;
        double omega  = 0.05  * FastMath.PI;
        final TimeStampedAngularCoordinates reference =
                new TimeStampedAngularCoordinates(date,
                                                  new Rotation(Vector3D.PLUS_K, alpha0,
                                                               RotationConvention.VECTOR_OPERATOR),
                                                  new Vector3D(omega, Vector3D.MINUS_K),
                                                  Vector3D.ZERO);
        double[] errors = interpolationErrors(reference, 1.0);
        Assert.assertEquals(0.0, errors[0], 1.4e-15);
        Assert.assertEquals(0.0, errors[1], 3.0e-15);
        Assert.assertEquals(0.0, errors[2], 3.0e-14);
    }

    @Test
    public void testInterpolationWithAcceleration() throws OrekitException {
        AbsoluteDate date = AbsoluteDate.GALILEO_EPOCH;
        double alpha0 = 0.5   * FastMath.PI;
        double omega  = 0.05  * FastMath.PI;
        double eta    = 0.005 * FastMath.PI;
        final TimeStampedAngularCoordinates reference =
                new TimeStampedAngularCoordinates(date,
                                                  new Rotation(Vector3D.PLUS_K, alpha0,
                                                               RotationConvention.VECTOR_OPERATOR),
                                                  new Vector3D(omega, Vector3D.MINUS_K),
                                                  new Vector3D(eta,   Vector3D.PLUS_J));
        double[] errors = interpolationErrors(reference, 1.0);
        Assert.assertEquals(0.0, errors[0], 3.0e-5);
        Assert.assertEquals(0.0, errors[1], 2.0e-4);
        Assert.assertEquals(0.0, errors[2], 4.6e-3);
    }

    private double[] interpolationErrors(final TimeStampedAngularCoordinates reference, double dt)
        throws OrekitException {

        final OrdinaryDifferentialEquation ode = new OrdinaryDifferentialEquation() {
            public int getDimension() {
                return 4;
            }
            public double[] computeDerivatives(final double t, final double[] q) {
                final double omegaX = reference.getRotationRate().getX() + t * reference.getRotationAcceleration().getX();
                final double omegaY = reference.getRotationRate().getY() + t * reference.getRotationAcceleration().getY();
                final double omegaZ = reference.getRotationRate().getZ() + t * reference.getRotationAcceleration().getZ();
                return new double[] {
                    0.5 * MathArrays.linearCombination(-q[1], omegaX, -q[2], omegaY, -q[3], omegaZ),
                    0.5 * MathArrays.linearCombination( q[0], omegaX, -q[3], omegaY,  q[2], omegaZ),
                    0.5 * MathArrays.linearCombination( q[3], omegaX,  q[0], omegaY, -q[1], omegaZ),
                    0.5 * MathArrays.linearCombination(-q[2], omegaX,  q[1], omegaY,  q[0], omegaZ)
                };
            }
        };
        final List<TimeStampedAngularCoordinates> complete = new ArrayList<TimeStampedAngularCoordinates>();
        ODEIntegrator integrator = new DormandPrince853Integrator(1.0e-6, 1.0, 1.0e-12, 1.0e-12);
        integrator.addStepHandler(new StepNormalizer(dt / 2000, new ODEFixedStepHandler() {
            public void handleStep(ODEStateAndDerivative state, boolean isLast) {
                final double   t = state.getTime();
                final double[] q = state.getPrimaryState();
                complete.add(new TimeStampedAngularCoordinates(reference.getDate().shiftedBy(t),
                                                               new Rotation(q[0], q[1], q[2], q[3], true),
                                                               new Vector3D(1, reference.getRotationRate(),
                                                                            t, reference.getRotationAcceleration()),
                                                               reference.getRotationAcceleration()));
            }
        }));

        double[] y = new double[] {
            reference.getRotation().getQ0(),
            reference.getRotation().getQ1(),
            reference.getRotation().getQ2(),
            reference.getRotation().getQ3()
        };
        integrator.integrate(ode, new ODEState(0, y), dt);

        List<TimeStampedAngularCoordinates> sample = new ArrayList<TimeStampedAngularCoordinates>();
        sample.add(complete.get(0));
        sample.add(complete.get(complete.size() / 2));
        sample.add(complete.get(complete.size() - 1));

        double maxRotationError     = 0;
        double maxRateError         = 0;
        double maxAccelerationError = 0;
        for (TimeStampedAngularCoordinates acRef : complete) {
            TimeStampedAngularCoordinates interpolated =
                    TimeStampedAngularCoordinates.interpolate(acRef.getDate(), AngularDerivativesFilter.USE_RRA, sample);
            double rotationError     = Rotation.distance(acRef.getRotation(), interpolated.getRotation());
            double rateError         = Vector3D.distance(acRef.getRotationRate(), interpolated.getRotationRate());
            double accelerationError = Vector3D.distance(acRef.getRotationAcceleration(), interpolated.getRotationAcceleration());
            maxRotationError         = FastMath.max(maxRotationError,     rotationError);
            maxRateError             = FastMath.max(maxRateError,         rateError);
            maxAccelerationError     = FastMath.max(maxAccelerationError, accelerationError);
        }

        return new double[] {
          maxRotationError, maxRateError, maxAccelerationError
        };

    }

    @Test
    public void testInterpolationNeedOffsetWrongRate() throws OrekitException {
        AbsoluteDate date = AbsoluteDate.GALILEO_EPOCH;
        double omega  = 2.0 * FastMath.PI;
        TimeStampedAngularCoordinates reference =
                new TimeStampedAngularCoordinates(date,
                                                  Rotation.IDENTITY,
                                                  new Vector3D(omega, Vector3D.MINUS_K),
                                                  Vector3D.ZERO);

        List<TimeStampedAngularCoordinates> sample = new ArrayList<TimeStampedAngularCoordinates>();
        for (double dt : new double[] { 0.0, 0.25, 0.5, 0.75, 1.0 }) {
            TimeStampedAngularCoordinates shifted = reference.shiftedBy(dt);
            sample.add(new TimeStampedAngularCoordinates(shifted.getDate(),
                                                         shifted.getRotation(),
                                                         Vector3D.ZERO, Vector3D.ZERO));
        }

        for (TimeStampedAngularCoordinates s : sample) {
            TimeStampedAngularCoordinates interpolated =
                    TimeStampedAngularCoordinates.interpolate(s.getDate(), AngularDerivativesFilter.USE_RR, sample);
            Rotation r    = interpolated.getRotation();
            Vector3D rate = interpolated.getRotationRate();
            Assert.assertEquals(0.0, Rotation.distance(s.getRotation(), r), 2.0e-14);
            Assert.assertEquals(0.0, Vector3D.distance(s.getRotationRate(), rate), 2.0e-13);
        }

    }

    @Test
    public void testInterpolationRotationOnly() throws OrekitException {
        AbsoluteDate date = AbsoluteDate.GALILEO_EPOCH;
        double alpha0 = 0.5 * FastMath.PI;
        double omega  = 0.5 * FastMath.PI;
        TimeStampedAngularCoordinates reference =
                new TimeStampedAngularCoordinates(date,
                                                  new Rotation(Vector3D.PLUS_K, alpha0,
                                                               RotationConvention.VECTOR_OPERATOR),
                                                  new Vector3D(omega, Vector3D.MINUS_K),
                                                  Vector3D.ZERO);

        List<TimeStampedAngularCoordinates> sample = new ArrayList<TimeStampedAngularCoordinates>();
        for (double dt : new double[] { 0.0, 0.2, 0.4, 0.6, 0.8, 1.0 }) {
            Rotation r = reference.shiftedBy(dt).getRotation();
            sample.add(new TimeStampedAngularCoordinates(date.shiftedBy(dt), r, Vector3D.ZERO, Vector3D.ZERO));
        }

        for (double dt = 0; dt < 1.0; dt += 0.001) {
            TimeStampedAngularCoordinates interpolated =
                    TimeStampedAngularCoordinates.interpolate(date.shiftedBy(dt), AngularDerivativesFilter.USE_R, sample);
            Rotation r    = interpolated.getRotation();
            Vector3D rate = interpolated.getRotationRate();
            Assert.assertEquals(0.0, Rotation.distance(reference.shiftedBy(dt).getRotation(), r), 3.0e-4);
            Assert.assertEquals(0.0, Vector3D.distance(reference.shiftedBy(dt).getRotationRate(), rate), 1.0e-2);
        }

    }

    @Test
    public void testInterpolationTooSmallSample() throws OrekitException {
        AbsoluteDate date = AbsoluteDate.GALILEO_EPOCH;
        double alpha0 = 0.5 * FastMath.PI;
        double omega  = 0.5 * FastMath.PI;
        TimeStampedAngularCoordinates reference =
                new TimeStampedAngularCoordinates(date,
                                                  new Rotation(Vector3D.PLUS_K, alpha0,
                                                               RotationConvention.VECTOR_OPERATOR),
                                                  new Vector3D(omega, Vector3D.MINUS_K),
                                                  Vector3D.ZERO);

        List<TimeStampedAngularCoordinates> sample = new ArrayList<TimeStampedAngularCoordinates>();
        Rotation r = reference.shiftedBy(0.2).getRotation();
        sample.add(new TimeStampedAngularCoordinates(date.shiftedBy(0.2), r, Vector3D.ZERO, Vector3D.ZERO));

        try {
            TimeStampedAngularCoordinates.interpolate(date.shiftedBy(0.3), AngularDerivativesFilter.USE_R, sample);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.NOT_ENOUGH_DATA_FOR_INTERPOLATION, oe.getSpecifier());
            Assert.assertEquals(1, ((Integer) oe.getParts()[0]).intValue());
        }

    }

    @Test
    public void testInterpolationGTODIssue() throws OrekitException {
        AbsoluteDate t0 = new AbsoluteDate("2004-04-06T19:59:28.000", TimeScalesFactory.getTAI());
        double[][] params = new double[][] {
            { 0.0, -0.3802356750911964, -0.9248896320037013, 7.292115030462892e-5 },
            { 4.0,  0.1345716955788532, -0.990903859488413,  7.292115033301528e-5 },
            { 8.0, -0.613127541102373,   0.7899839354960061, 7.292115037371062e-5 }
        };
        List<TimeStampedAngularCoordinates> sample = new ArrayList<TimeStampedAngularCoordinates>();
        for (double[] row : params) {
            AbsoluteDate t = t0.shiftedBy(row[0] * 3600.0);
            Rotation     r = new Rotation(row[1], 0.0, 0.0, row[2], false);
            Vector3D     o = new Vector3D(row[3], Vector3D.PLUS_K);
            sample.add(new TimeStampedAngularCoordinates(t, r, o, Vector3D.ZERO));
        }
        for (double dt = 0; dt < 29000; dt += 120) {
            TimeStampedAngularCoordinates shifted      = sample.get(0).shiftedBy(dt);
            TimeStampedAngularCoordinates interpolated =
                    TimeStampedAngularCoordinates.interpolate(t0.shiftedBy(dt), AngularDerivativesFilter.USE_RR, sample);
            Assert.assertEquals(0.0,
                                Rotation.distance(shifted.getRotation(), interpolated.getRotation()),
                                1.3e-7);
            Assert.assertEquals(0.0,
                                Vector3D.distance(shifted.getRotationRate(), interpolated.getRotationRate()),
                                1.0e-11);
        }

    }

    private Vector3D randomVector(RandomGenerator random, double norm) {
        double n = random.nextDouble() * norm;
        double x = 2 * random.nextDouble() - 1;
        double y = 2 * random.nextDouble() - 1;
        double z = 2 * random.nextDouble() - 1;
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

