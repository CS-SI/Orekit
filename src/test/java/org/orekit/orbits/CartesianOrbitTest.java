/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.hipparchus.analysis.UnivariateFunction;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.FiniteDifferencesDifferentiator;
import org.hipparchus.analysis.differentiation.UnivariateDifferentiableFunction;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrixPreservingVisitor;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;


public class CartesianOrbitTest {

    // Computation date
    private AbsoluteDate date;

    // Body mu
    private double mu;

    @Test
    public void testCartesianToCartesian()
        throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        Vector3D position = new Vector3D(-29536113.0, 30329259.0, -100125.0);
        Vector3D velocity = new Vector3D(-2194.0, -2141.0, -8.0);
        PVCoordinates pvCoordinates = new PVCoordinates( position, velocity);
        double mu = 3.9860047e14;

        CartesianOrbit p = new CartesianOrbit(pvCoordinates, FramesFactory.getEME2000(), date, mu);

        Assert.assertEquals(p.getPVCoordinates().getPosition().getX(), pvCoordinates.getPosition().getX(), Utils.epsilonTest * FastMath.abs(pvCoordinates.getPosition().getX()));
        Assert.assertEquals(p.getPVCoordinates().getPosition().getY(), pvCoordinates.getPosition().getY(), Utils.epsilonTest * FastMath.abs(pvCoordinates.getPosition().getY()));
        Assert.assertEquals(p.getPVCoordinates().getPosition().getZ(), pvCoordinates.getPosition().getZ(), Utils.epsilonTest * FastMath.abs(pvCoordinates.getPosition().getZ()));
        Assert.assertEquals(p.getPVCoordinates().getVelocity().getX(), pvCoordinates.getVelocity().getX(), Utils.epsilonTest * FastMath.abs(pvCoordinates.getVelocity().getX()));
        Assert.assertEquals(p.getPVCoordinates().getVelocity().getY(), pvCoordinates.getVelocity().getY(), Utils.epsilonTest * FastMath.abs(pvCoordinates.getVelocity().getY()));
        Assert.assertEquals(p.getPVCoordinates().getVelocity().getZ(), pvCoordinates.getVelocity().getZ(), Utils.epsilonTest * FastMath.abs(pvCoordinates.getVelocity().getZ()));

        Method initPV = CartesianOrbit.class.getDeclaredMethod("initPVCoordinates", new Class[0]);
        initPV.setAccessible(true);
        Assert.assertSame(p.getPVCoordinates(), initPV.invoke(p, new Object[0]));

    }

    @Test
    public void testCartesianToEquinoctial() {

        Vector3D position = new Vector3D(-29536113.0, 30329259.0, -100125.0);
        Vector3D velocity = new Vector3D(-2194.0, -2141.0, -8.0);
        PVCoordinates pvCoordinates = new PVCoordinates( position, velocity);

        CartesianOrbit p = new CartesianOrbit(pvCoordinates, FramesFactory.getEME2000(), date, mu);

        Assert.assertEquals(42255170.0028257,  p.getA(), Utils.epsilonTest * p.getA());
        Assert.assertEquals(0.592732497856475e-03,  p.getEquinoctialEx(), Utils.epsilonE * FastMath.abs(p.getE()));
        Assert.assertEquals(-0.206274396964359e-02, p.getEquinoctialEy(), Utils.epsilonE * FastMath.abs(p.getE()));
        Assert.assertEquals(FastMath.sqrt(FastMath.pow(0.592732497856475e-03, 2)+FastMath.pow(-0.206274396964359e-02, 2)), p.getE(), Utils.epsilonAngle * FastMath.abs(p.getE()));
        Assert.assertEquals(MathUtils.normalizeAngle(2*FastMath.asin(FastMath.sqrt((FastMath.pow(0.128021863908325e-03, 2)+FastMath.pow(-0.352136186881817e-02, 2))/4.)), p.getI()), p.getI(), Utils.epsilonAngle * FastMath.abs(p.getI()));
        Assert.assertEquals(MathUtils.normalizeAngle(0.234498139679291e+01, p.getLM()), p.getLM(), Utils.epsilonAngle * FastMath.abs(p.getLM()));

        // trigger a specific path in copy constructor
        CartesianOrbit q = new CartesianOrbit(p);

        Assert.assertEquals(42255170.0028257,  q.getA(), Utils.epsilonTest * q.getA());
        Assert.assertEquals(0.592732497856475e-03,  q.getEquinoctialEx(), Utils.epsilonE * FastMath.abs(q.getE()));
        Assert.assertEquals(-0.206274396964359e-02, q.getEquinoctialEy(), Utils.epsilonE * FastMath.abs(q.getE()));
        Assert.assertEquals(FastMath.sqrt(FastMath.pow(0.592732497856475e-03, 2)+FastMath.pow(-0.206274396964359e-02, 2)), q.getE(), Utils.epsilonAngle * FastMath.abs(q.getE()));
        Assert.assertEquals(MathUtils.normalizeAngle(2*FastMath.asin(FastMath.sqrt((FastMath.pow(0.128021863908325e-03, 2)+FastMath.pow(-0.352136186881817e-02, 2))/4.)), q.getI()), q.getI(), Utils.epsilonAngle * FastMath.abs(q.getI()));
        Assert.assertEquals(MathUtils.normalizeAngle(0.234498139679291e+01, q.getLM()), q.getLM(), Utils.epsilonAngle * FastMath.abs(q.getLM()));

        Assert.assertTrue(Double.isNaN(q.getADot()));
        Assert.assertTrue(Double.isNaN(q.getEquinoctialExDot()));
        Assert.assertTrue(Double.isNaN(q.getEquinoctialEyDot()));
        Assert.assertTrue(Double.isNaN(q.getHxDot()));
        Assert.assertTrue(Double.isNaN(q.getHyDot()));
        Assert.assertTrue(Double.isNaN(q.getLvDot()));
        Assert.assertTrue(Double.isNaN(q.getEDot()));
        Assert.assertTrue(Double.isNaN(q.getIDot()));

    }

    @Test
    public void testCartesianToKeplerian(){

        Vector3D position = new Vector3D(-26655470.0, 29881667.0, -113657.0);
        Vector3D velocity = new Vector3D(-1125.0, -1122.0, 195.0);
        PVCoordinates pvCoordinates = new PVCoordinates( position, velocity);
        double mu = 3.9860047e14;

        CartesianOrbit p = new CartesianOrbit(pvCoordinates, FramesFactory.getEME2000(), date, mu);
        KeplerianOrbit kep = new KeplerianOrbit(p);

        Assert.assertEquals(22979265.3030773,  p.getA(), Utils.epsilonTest  * p.getA());
        Assert.assertEquals(0.743502611664700, p.getE(), Utils.epsilonE     * FastMath.abs(p.getE()));
        Assert.assertEquals(0.122182096220906, p.getI(), Utils.epsilonAngle * FastMath.abs(p.getI()));
        double pa = kep.getPerigeeArgument();
        Assert.assertEquals(MathUtils.normalizeAngle(3.09909041016672, pa), pa,
                     Utils.epsilonAngle * FastMath.abs(pa));
        double raan = kep.getRightAscensionOfAscendingNode();
        Assert.assertEquals(MathUtils.normalizeAngle(2.32231010979999, raan), raan,
                     Utils.epsilonAngle * FastMath.abs(raan));
        double m = kep.getMeanAnomaly();
        Assert.assertEquals(MathUtils.normalizeAngle(3.22888977629034, m), m,
                     Utils.epsilonAngle * FastMath.abs(FastMath.abs(m)));
    }

    @Test
    public void testPositionVelocityNorms(){

        Vector3D position = new Vector3D(-29536113.0, 30329259.0, -100125.0);
        Vector3D velocity = new Vector3D(-2194.0, -2141.0, -8.0);
        PVCoordinates pvCoordinates = new PVCoordinates( position, velocity);

        CartesianOrbit p = new CartesianOrbit(pvCoordinates, FramesFactory.getEME2000(), date, mu);

        double e       = p.getE();
        double v       = new KeplerianOrbit(p).getTrueAnomaly();
        double ksi     = 1 + e * FastMath.cos(v);
        double nu      = e * FastMath.sin(v);
        double epsilon = FastMath.sqrt((1 - e) * (1 + e));

        double a  = p.getA();
        double na = FastMath.sqrt(mu / a);

        // validation of: r = a .(1 - e2) / (1 + e.cos(v))
        Assert.assertEquals(a * epsilon * epsilon / ksi,
                     p.getPVCoordinates().getPosition().getNorm(),
                     Utils.epsilonTest * FastMath.abs(p.getPVCoordinates().getPosition().getNorm()));

        // validation of: V = sqrt(mu.(1+2e.cos(v)+e2)/a.(1-e2) )
        Assert.assertEquals(na * FastMath.sqrt(ksi * ksi + nu * nu) / epsilon,
                     p.getPVCoordinates().getVelocity().getNorm(),
                     Utils.epsilonTest * FastMath.abs(p.getPVCoordinates().getVelocity().getNorm()));

    }

    @Test
    public void testGeometry() {

        Vector3D position = new Vector3D(-29536113.0, 30329259.0, -100125.0);
        Vector3D velocity = new Vector3D(-2194.0, -2141.0, -8.0);
        PVCoordinates pvCoordinates = new PVCoordinates( position, velocity);

        Vector3D momentum = pvCoordinates.getMomentum().normalize();

        EquinoctialOrbit p = new EquinoctialOrbit(pvCoordinates, FramesFactory.getEME2000(), date, mu);

        double apogeeRadius  = p.getA() * (1 + p.getE());
        double perigeeRadius = p.getA() * (1 - p.getE());

        for (double lv = 0; lv <= 2 * FastMath.PI; lv += 2 * FastMath.PI/100.) {
            p = new EquinoctialOrbit(p.getA(), p.getEquinoctialEx(), p.getEquinoctialEy(),
                                          p.getHx(), p.getHy(), lv, PositionAngle.TRUE, p.getFrame(), date, mu);
            position = p.getPVCoordinates().getPosition();

            // test if the norm of the position is in the range [perigee radius, apogee radius]
            // Warning: these tests are without absolute value by choice
            Assert.assertTrue((position.getNorm() - apogeeRadius)  <= (  apogeeRadius * Utils.epsilonTest));
            Assert.assertTrue((position.getNorm() - perigeeRadius) >= (- perigeeRadius * Utils.epsilonTest));
            // Assert.assertTrue(position.getNorm() <= apogeeRadius);
            // Assert.assertTrue(position.getNorm() >= perigeeRadius);

            position= position.normalize();
            velocity = p.getPVCoordinates().getVelocity().normalize();

            // at this stage of computation, all the vectors (position, velocity and momemtum) are normalized here

            // test of orthogonality between position and momentum
            Assert.assertTrue(FastMath.abs(Vector3D.dotProduct(position, momentum)) < Utils.epsilonTest);
            // test of orthogonality between velocity and momentum
            Assert.assertTrue(FastMath.abs(Vector3D.dotProduct(velocity, momentum)) < Utils.epsilonTest);
        }
    }

    @Test
    public void testHyperbola1() {
        CartesianOrbit orbit = new CartesianOrbit(new KeplerianOrbit(-10000000.0, 2.5, 0.3, 0, 0, 0.0,
                                                                     PositionAngle.TRUE,
                                                                     FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH,
                                                                     mu));
        Vector3D perigeeP  = orbit.getPVCoordinates().getPosition();
        Vector3D u = perigeeP.normalize();
        Vector3D focus1 = Vector3D.ZERO;
        Vector3D focus2 = new Vector3D(-2 * orbit.getA() * orbit.getE(), u);
        for (double dt = -5000; dt < 5000; dt += 60) {
            PVCoordinates pv = orbit.shiftedBy(dt).getPVCoordinates();
            double d1 = Vector3D.distance(pv.getPosition(), focus1);
            double d2 = Vector3D.distance(pv.getPosition(), focus2);
            Assert.assertEquals(-2 * orbit.getA(), FastMath.abs(d1 - d2), 1.0e-6);
            CartesianOrbit rebuilt =
                new CartesianOrbit(pv, orbit.getFrame(), orbit.getDate().shiftedBy(dt), mu);
            Assert.assertEquals(-10000000.0, rebuilt.getA(), 1.0e-6);
            Assert.assertEquals(2.5, rebuilt.getE(), 1.0e-13);
        }
    }

    @Test
    public void testHyperbola2() {
        CartesianOrbit orbit = new CartesianOrbit(new KeplerianOrbit(-10000000.0, 1.2, 0.3, 0, 0, -1.75,
                                                                     PositionAngle.MEAN,
                                                                     FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH,
                                                                     mu));
        Vector3D perigeeP  = new KeplerianOrbit(-10000000.0, 1.2, 0.3, 0, 0, 0.0, PositionAngle.TRUE, orbit.getFrame(),
                                                orbit.getDate(), orbit.getMu()).getPVCoordinates().getPosition();
        Vector3D u = perigeeP.normalize();
        Vector3D focus1 = Vector3D.ZERO;
        Vector3D focus2 = new Vector3D(-2 * orbit.getA() * orbit.getE(), u);
        for (double dt = -5000; dt < 5000; dt += 60) {
            PVCoordinates pv = orbit.shiftedBy(dt).getPVCoordinates();
            double d1 = Vector3D.distance(pv.getPosition(), focus1);
            double d2 = Vector3D.distance(pv.getPosition(), focus2);
            Assert.assertEquals(-2 * orbit.getA(), FastMath.abs(d1 - d2), 1.0e-6);
            CartesianOrbit rebuilt =
                new CartesianOrbit(pv, orbit.getFrame(), orbit.getDate().shiftedBy(dt), mu);
            Assert.assertEquals(-10000000.0, rebuilt.getA(), 1.0e-6);
            Assert.assertEquals(1.2, rebuilt.getE(), 1.0e-13);
        }
    }

    @Test
    public void testNumericalIssue25() {
        Vector3D position = new Vector3D(3782116.14107698, 416663.11924914, 5875541.62103057);
        Vector3D velocity = new Vector3D(-6349.7848910501, 288.4061811651, 4066.9366759691);
        CartesianOrbit orbit = new CartesianOrbit(new PVCoordinates(position, velocity),
                                                  FramesFactory.getEME2000(),
                                                  new AbsoluteDate("2004-01-01T23:00:00.000",
                                                                   TimeScalesFactory.getUTC()),
                                                                   3.986004415E14);
        Assert.assertEquals(0.0, orbit.getE(), 2.0e-14);
    }

    @Test
    public void testSerialization()
      throws IOException, ClassNotFoundException {
        Vector3D position = new Vector3D(-29536113.0, 30329259.0, -100125.0);
        Vector3D velocity = new Vector3D(-2194.0, -2141.0, -8.0);
        PVCoordinates pvCoordinates = new PVCoordinates(position, velocity);
        CartesianOrbit orbit = new CartesianOrbit(pvCoordinates, FramesFactory.getEME2000(), date, mu);
        Assert.assertEquals(42255170.003, orbit.getA(), 1.0e-3);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream    oos = new ObjectOutputStream(bos);
        oos.writeObject(orbit);

        Assert.assertTrue(bos.size() > 270);
        Assert.assertTrue(bos.size() < 320);

        ByteArrayInputStream  bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream     ois = new ObjectInputStream(bis);
        CartesianOrbit deserialized  = (CartesianOrbit) ois.readObject();
        PVCoordinates dpv = new PVCoordinates(orbit.getPVCoordinates(), deserialized.getPVCoordinates());
        Assert.assertEquals(0.0, dpv.getPosition().getNorm(), 1.0e-10);
        Assert.assertEquals(0.0, dpv.getVelocity().getNorm(), 1.0e-10);
        Assert.assertTrue(Double.isNaN(orbit.getADot()) && Double.isNaN(deserialized.getADot()));
        Assert.assertTrue(Double.isNaN(orbit.getEDot()) && Double.isNaN(deserialized.getEDot()));
        Assert.assertTrue(Double.isNaN(orbit.getIDot()) && Double.isNaN(deserialized.getIDot()));
        Assert.assertTrue(Double.isNaN(orbit.getEquinoctialExDot()) && Double.isNaN(deserialized.getEquinoctialExDot()));
        Assert.assertTrue(Double.isNaN(orbit.getEquinoctialEyDot()) && Double.isNaN(deserialized.getEquinoctialEyDot()));
        Assert.assertTrue(Double.isNaN(orbit.getHxDot()) && Double.isNaN(deserialized.getHxDot()));
        Assert.assertTrue(Double.isNaN(orbit.getHyDot()) && Double.isNaN(deserialized.getHyDot()));
        Assert.assertTrue(Double.isNaN(orbit.getLvDot()) && Double.isNaN(deserialized.getLvDot()));
        Assert.assertEquals(orbit.getDate(), deserialized.getDate());
        Assert.assertEquals(orbit.getMu(), deserialized.getMu(), 1.0e-10);
        Assert.assertEquals(orbit.getFrame().getName(), deserialized.getFrame().getName());

    }

    @Test
    public void testSerializationWithDerivatives()
      throws IOException, ClassNotFoundException {
        Vector3D position = new Vector3D(-29536113.0, 30329259.0, -100125.0);
        Vector3D velocity = new Vector3D(-2194.0, -2141.0, -8.0);
        double r2 = position.getNormSq();
        double r  = FastMath.sqrt(r2);
        Vector3D acceleration = new Vector3D(-mu / (r * r2), position,
                                             1, new Vector3D(-0.1, 0.2, 0.3));
        PVCoordinates pvCoordinates = new PVCoordinates(position, velocity, acceleration);
        CartesianOrbit orbit = new CartesianOrbit(pvCoordinates, FramesFactory.getEME2000(), date, mu);
        Assert.assertEquals(42255170.003, orbit.getA(), 1.0e-3);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream    oos = new ObjectOutputStream(bos);
        oos.writeObject(orbit);

        Assert.assertTrue(bos.size() > 320);
        Assert.assertTrue(bos.size() < 370);

        ByteArrayInputStream  bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream     ois = new ObjectInputStream(bis);
        CartesianOrbit deserialized  = (CartesianOrbit) ois.readObject();
        PVCoordinates dpv = new PVCoordinates(orbit.getPVCoordinates(), deserialized.getPVCoordinates());
        Assert.assertEquals(0.0, dpv.getPosition().getNorm(), 1.0e-10);
        Assert.assertEquals(0.0, dpv.getVelocity().getNorm(), 1.0e-10);
        Assert.assertEquals(orbit.getADot(), deserialized.getADot(), 1.0e-10);
        Assert.assertEquals(orbit.getEDot(), deserialized.getEDot(), 1.0e-10);
        Assert.assertEquals(orbit.getIDot(), deserialized.getIDot(), 1.0e-10);
        Assert.assertEquals(orbit.getEquinoctialExDot(), deserialized.getEquinoctialExDot(), 1.0e-10);
        Assert.assertEquals(orbit.getEquinoctialEyDot(), deserialized.getEquinoctialEyDot(), 1.0e-10);
        Assert.assertEquals(orbit.getHxDot(), deserialized.getHxDot(), 1.0e-10);
        Assert.assertEquals(orbit.getHyDot(), deserialized.getHyDot(), 1.0e-10);
        Assert.assertEquals(orbit.getLvDot(), deserialized.getLvDot(), 1.0e-10);
        Assert.assertEquals(orbit.getDate(), deserialized.getDate());
        Assert.assertEquals(orbit.getMu(), deserialized.getMu(), 1.0e-10);
        Assert.assertEquals(orbit.getFrame().getName(), deserialized.getFrame().getName());

    }

    @Test
    public void testDerivativesConversionSymmetry() {
        final AbsoluteDate date = new AbsoluteDate("2003-05-01T00:01:20.000", TimeScalesFactory.getUTC());
        Vector3D position     = new Vector3D(6893443.400234382, 1886406.1073757345, -589265.1150359757);
        Vector3D velocity     = new Vector3D(-281.1261461082365, -1231.6165642450928, -7348.756363469432);
        Vector3D acceleration = new Vector3D(-7.460341170581685, -2.0415957334584527, 0.6393322823627762);
        PVCoordinates pvCoordinates = new PVCoordinates( position, velocity, acceleration);
        CartesianOrbit orbit = new CartesianOrbit(pvCoordinates, FramesFactory.getEME2000(),
                                                  date, Constants.EIGEN5C_EARTH_MU);
        Assert.assertTrue(orbit.hasDerivatives());
        double r2 = position.getNormSq();
        double r  = FastMath.sqrt(r2);
        Vector3D keplerianAcceleration = new Vector3D(-orbit.getMu() / (r2 * r), position);
        Assert.assertEquals(0.0101, Vector3D.distance(keplerianAcceleration, acceleration), 1.0e-4);

        for (OrbitType type : OrbitType.values()) {
            Orbit converted = type.convertType(orbit);
            Assert.assertTrue(converted.hasDerivatives());
            CartesianOrbit rebuilt = (CartesianOrbit) OrbitType.CARTESIAN.convertType(converted);
            Assert.assertTrue(rebuilt.hasDerivatives());
            Assert.assertEquals(0, Vector3D.distance(rebuilt.getPVCoordinates().getPosition(),     position),     2.0e-9);
            Assert.assertEquals(0, Vector3D.distance(rebuilt.getPVCoordinates().getVelocity(),     velocity),     2.5e-12);
            Assert.assertEquals(0, Vector3D.distance(rebuilt.getPVCoordinates().getAcceleration(), acceleration), 4.9e-15);
        }

    }

    @Test
    public void testDerivativesConversionSymmetryHyperbolic() {
        final AbsoluteDate date         = new AbsoluteDate("2003-05-01T00:00:20.000", TimeScalesFactory.getUTC());
        final Vector3D     position     = new Vector3D(224267911.905821, 290251613.109399, 45534292.777492);
        final Vector3D     velocity     = new Vector3D(-1494.068165293, 1124.771027677, 526.915286134);
        final Vector3D     acceleration = new Vector3D(-0.001295920501, -0.002233045187, -0.000349906292);
        PVCoordinates pvCoordinates = new PVCoordinates( position, velocity, acceleration);
        CartesianOrbit orbit = new CartesianOrbit(pvCoordinates, FramesFactory.getEME2000(),
                                                  date, Constants.EIGEN5C_EARTH_MU);
        Assert.assertTrue(orbit.hasDerivatives());
        double r2 = position.getNormSq();
        double r  = FastMath.sqrt(r2);
        Vector3D keplerianAcceleration = new Vector3D(-orbit.getMu() / (r2 * r), position);
        Assert.assertEquals(4.78e-4, Vector3D.distance(keplerianAcceleration, acceleration), 1.0e-6);

        OrbitType type = OrbitType.KEPLERIAN;
        Orbit converted = type.convertType(orbit);
        Assert.assertTrue(converted.hasDerivatives());
        CartesianOrbit rebuilt = (CartesianOrbit) OrbitType.CARTESIAN.convertType(converted);
        Assert.assertTrue(rebuilt.hasDerivatives());
        Assert.assertEquals(0, Vector3D.distance(rebuilt.getPVCoordinates().getPosition(),     position),     1.0e-15);
        Assert.assertEquals(0, Vector3D.distance(rebuilt.getPVCoordinates().getVelocity(),     velocity),     1.0e-15);
        Assert.assertEquals(0, Vector3D.distance(rebuilt.getPVCoordinates().getAcceleration(), acceleration), 1.0e-15);

    }

    @Test
    public void testShiftElliptic() {
        Vector3D position = new Vector3D(-29536113.0, 30329259.0, -100125.0);
        Vector3D velocity = new Vector3D(-2194.0, -2141.0, -8.0);
        PVCoordinates pvCoordinates = new PVCoordinates( position, velocity);
        CartesianOrbit orbit = new CartesianOrbit(pvCoordinates, FramesFactory.getEME2000(), date, mu);
        testShift(orbit, new KeplerianOrbit(orbit), 1.0e-13);
    }

    @Test
    public void testShiftCircular() {
        Vector3D position = new Vector3D(-29536113.0, 30329259.0, -100125.0);
        Vector3D velocity = new Vector3D(FastMath.sqrt(mu / position.getNorm()), position.orthogonal());
        PVCoordinates pvCoordinates = new PVCoordinates( position, velocity);
        CartesianOrbit orbit = new CartesianOrbit(pvCoordinates, FramesFactory.getEME2000(), date, mu);
        testShift(orbit, new CircularOrbit(orbit), 1.0e-15);
    }

    @Test
    public void testShiftEquinoctial() {
        Vector3D position = new Vector3D(-29536113.0, 30329259.0, -100125.0);
        Vector3D velocity = new Vector3D(FastMath.sqrt(mu / position.getNorm()), position.orthogonal());
        PVCoordinates pvCoordinates = new PVCoordinates( position, velocity);
        CartesianOrbit orbit = new CartesianOrbit(pvCoordinates, FramesFactory.getEME2000(), date, mu);
        testShift(orbit, new EquinoctialOrbit(orbit), 5.0e-14);
    }

    @Test
    public void testShiftHyperbolic() {
        Vector3D position = new Vector3D(-29536113.0, 30329259.0, -100125.0);
        Vector3D velocity = new Vector3D(3 * FastMath.sqrt(mu / position.getNorm()), position.orthogonal());
        PVCoordinates pvCoordinates = new PVCoordinates(position, velocity);
        CartesianOrbit orbit = new CartesianOrbit(pvCoordinates, FramesFactory.getEME2000(), date, mu);
        testShift(orbit, new KeplerianOrbit(orbit), 2.0e-15);
    }

    @Test
    public void testNumericalIssue135() {
        Vector3D position = new Vector3D(-6.7884943832e7, -2.1423006112e7, -3.1603915377e7);
        Vector3D velocity = new Vector3D(-4732.55, -2472.086, -3022.177);
        PVCoordinates pvCoordinates = new PVCoordinates(position, velocity);
        CartesianOrbit orbit = new CartesianOrbit(pvCoordinates, FramesFactory.getEME2000(), date,
                                                  324858598826460.);
        testShift(orbit, new KeplerianOrbit(orbit), 6.0e-15);
    }

    private void testShift(CartesianOrbit tested, Orbit reference, double threshold) {
        for (double dt = - 1000; dt < 1000; dt += 10.0) {

            PVCoordinates pvTested    = tested.shiftedBy(dt).getPVCoordinates();
            Vector3D      pTested     = pvTested.getPosition();
            Vector3D      vTested     = pvTested.getVelocity();

            PVCoordinates pvReference = reference.shiftedBy(dt).getPVCoordinates();
            Vector3D      pReference  = pvReference.getPosition();
            Vector3D      vReference  = pvReference.getVelocity();

            Assert.assertEquals(0, pTested.subtract(pReference).getNorm(), threshold * pReference.getNorm());
            Assert.assertEquals(0, vTested.subtract(vReference).getNorm(), threshold * vReference.getNorm());

        }
    }

    @Test(expected=IllegalArgumentException.class)
    public void testNonInertialFrame() throws IllegalArgumentException {

        Vector3D position = new Vector3D(-26655470.0, 29881667.0, -113657.0);
        Vector3D velocity = new Vector3D(-1125.0, -1122.0, 195.0);
        PVCoordinates pvCoordinates = new PVCoordinates( position, velocity);
        double mu = 3.9860047e14;
        new CartesianOrbit(pvCoordinates,
                           new Frame(FramesFactory.getEME2000(), Transform.IDENTITY, "non-inertial", false),
                           date, mu);
    }

    @Test
    public void testJacobianReference() {

        Vector3D position = new Vector3D(-29536113.0, 30329259.0, -100125.0);
        Vector3D velocity = new Vector3D(-2194.0, -2141.0, -8.0);
        PVCoordinates pvCoordinates = new PVCoordinates( position, velocity);
        CartesianOrbit orbit = new CartesianOrbit(pvCoordinates, FramesFactory.getEME2000(), date, mu);

        double[][] jacobian = new double[6][6];
        orbit.getJacobianWrtCartesian(PositionAngle.MEAN, jacobian);

        for (int i = 0; i < jacobian.length; i++) {
            double[] row    = jacobian[i];
            for (int j = 0; j < row.length; j++) {
                Assert.assertEquals((i == j) ? 1 : 0, row[j], 1.0e-15);
            }
        }

        double[][] invJacobian = new double[6][6];
        orbit.getJacobianWrtParameters(PositionAngle.MEAN, invJacobian);
        MatrixUtils.createRealMatrix(jacobian).
                        multiply(MatrixUtils.createRealMatrix(invJacobian)).
        walkInRowOrder(new RealMatrixPreservingVisitor() {
            public void start(int rows, int columns,
                              int startRow, int endRow, int startColumn, int endColumn) {
            }

            public void visit(int row, int column, double value) {
                Assert.assertEquals(row == column ? 1.0 : 0.0, value, 1.0e-15);
            }

            public double end() {
                return Double.NaN;
            }
        });

    }

    @Test
    public void testInterpolationWithDerivatives() {
        doTestInterpolation(true,
                            394, 2.15e-8, 3.21, 1.39e-9,
                            2474, 6842, 6.55, 186);
    }

    @Test
    public void testInterpolationWithoutDerivatives() {
        doTestInterpolation(false,
                            394, 2.61, 3.21, 0.154,
                            2474, 2.28e12, 6.55, 6.22e10);
    }

    private void doTestInterpolation(boolean useDerivatives,
                                     double shiftPositionErrorWithin, double interpolationPositionErrorWithin,
                                     double shiftVelocityErrorWithin, double interpolationVelocityErrorWithin,
                                     double shiftPositionErrorFarPast, double interpolationPositionErrorFarPast,
                                     double shiftVelocityErrorFarPast, double interpolationVelocityErrorFarPast)
        {

        final double ehMu  = 3.9860047e14;
        final double ae  = 6.378137e6;
        final double c20 = -1.08263e-3;
        final double c30 = 2.54e-6;
        final double c40 = 1.62e-6;
        final double c50 = 2.3e-7;
        final double c60 = -5.5e-7;

        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        final Vector3D position = new Vector3D(3220103., 69623., 6449822.);
        final Vector3D velocity = new Vector3D(6414.7, -2006., -3180.);
        final CartesianOrbit initialOrbit = new CartesianOrbit(new PVCoordinates(position, velocity),
                                                               FramesFactory.getEME2000(), date, ehMu);

        EcksteinHechlerPropagator propagator =
                new EcksteinHechlerPropagator(initialOrbit, ae, ehMu, c20, c30, c40, c50, c60);

        // set up a 5 points sample
        List<Orbit> sample = new ArrayList<Orbit>();
        for (double dt = 0; dt < 251.0; dt += 60.0) {
            Orbit orbit = propagator.propagate(date.shiftedBy(dt)).getOrbit();
            if (!useDerivatives) {
                // remove derivatives
                double[] stateVector = new double[6];
                orbit.getType().mapOrbitToArray(orbit, PositionAngle.TRUE, stateVector, null);
                orbit = orbit.getType().mapArrayToOrbit(stateVector, null, PositionAngle.TRUE,
                                                        orbit.getDate(), orbit.getMu(), orbit.getFrame());
            }
            sample.add(orbit);
        }

        // well inside the sample, interpolation should be much better than Keplerian shift
        // this is because we take the full non-Keplerian acceleration into account in
        // the Cartesian parameters, which in this case is preserved by the
        // Eckstein-Hechler propagator
        double maxShiftPError = 0;
        double maxInterpolationPError = 0;
        double maxShiftVError = 0;
        double maxInterpolationVError = 0;
        for (double dt = 0; dt < 240.0; dt += 1.0) {
            AbsoluteDate t                   = initialOrbit.getDate().shiftedBy(dt);
            PVCoordinates propagated         = propagator.propagate(t).getPVCoordinates();
            PVCoordinates shiftError         = new PVCoordinates(propagated,
                                                                 initialOrbit.shiftedBy(dt).getPVCoordinates());
            PVCoordinates interpolationError = new PVCoordinates(propagated,
                                                                 initialOrbit.interpolate(t, sample).getPVCoordinates());
            maxShiftPError                   = FastMath.max(maxShiftPError,
                                                            shiftError.getPosition().getNorm());
            maxInterpolationPError           = FastMath.max(maxInterpolationPError,
                                                            interpolationError.getPosition().getNorm());
            maxShiftVError                   = FastMath.max(maxShiftVError,
                                                            shiftError.getVelocity().getNorm());
            maxInterpolationVError           = FastMath.max(maxInterpolationVError,
                                                            interpolationError.getVelocity().getNorm());
        }
        Assert.assertEquals(shiftPositionErrorWithin,         maxShiftPError,         0.01 * shiftPositionErrorWithin);
        Assert.assertEquals(interpolationPositionErrorWithin, maxInterpolationPError, 0.01 * interpolationPositionErrorWithin);
        Assert.assertEquals(shiftVelocityErrorWithin,         maxShiftVError,         0.01 * shiftVelocityErrorWithin);
        Assert.assertEquals(interpolationVelocityErrorWithin, maxInterpolationVError, 0.01 * interpolationVelocityErrorWithin);

        // if we go far past sample end, interpolation becomes worse than Keplerian shift
        maxShiftPError = 0;
        maxInterpolationPError = 0;
        maxShiftVError = 0;
        maxInterpolationVError = 0;
        for (double dt = 500.0; dt < 650.0; dt += 1.0) {
            AbsoluteDate t                   = initialOrbit.getDate().shiftedBy(dt);
            PVCoordinates propagated         = propagator.propagate(t).getPVCoordinates();
            PVCoordinates shiftError         = new PVCoordinates(propagated,
                                                                 initialOrbit.shiftedBy(dt).getPVCoordinates());
            PVCoordinates interpolationError = new PVCoordinates(propagated,
                                                                 initialOrbit.interpolate(t, sample).getPVCoordinates());
            maxShiftPError                   = FastMath.max(maxShiftPError,
                                                            shiftError.getPosition().getNorm());
            maxInterpolationPError           = FastMath.max(maxInterpolationPError,
                                                            interpolationError.getPosition().getNorm());
            maxShiftVError                   = FastMath.max(maxShiftVError,
                                                            shiftError.getVelocity().getNorm());
            maxInterpolationVError           = FastMath.max(maxInterpolationVError,
                                                            interpolationError.getVelocity().getNorm());
        }
        Assert.assertEquals(shiftPositionErrorFarPast,         maxShiftPError,         0.01 * shiftPositionErrorFarPast);
        Assert.assertEquals(interpolationPositionErrorFarPast, maxInterpolationPError, 0.01 * interpolationPositionErrorFarPast);
        Assert.assertEquals(shiftVelocityErrorFarPast,         maxShiftVError,         0.01 * shiftVelocityErrorFarPast);
        Assert.assertEquals(interpolationVelocityErrorFarPast, maxInterpolationVError, 0.01 * interpolationVelocityErrorFarPast);

    }

    @Test
    public void testNonKeplerianDerivatives() {
        final AbsoluteDate date         = new AbsoluteDate("2003-05-01T00:00:20.000", TimeScalesFactory.getUTC());
        final Vector3D     position     = new Vector3D(6896874.444705,  1956581.072644,  -147476.245054);
        final Vector3D     velocity     = new Vector3D(166.816407662, -1106.783301861, -7372.745712770);
        final Vector3D     acceleration = new Vector3D(-7.466182457944, -2.118153357345,  0.160004048437);
        final TimeStampedPVCoordinates pv = new TimeStampedPVCoordinates(date, position, velocity, acceleration);
        final Frame frame = FramesFactory.getEME2000();
        final double mu   = Constants.EIGEN5C_EARTH_MU;
        final CartesianOrbit orbit = new CartesianOrbit(pv, frame, mu);

        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getA()),
                            orbit.getADot(),
                            4.3e-8);
        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getEquinoctialEx()),
                            orbit.getEquinoctialExDot(),
                            2.1e-15);
        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getEquinoctialEy()),
                            orbit.getEquinoctialEyDot(),
                            5.3e-16);
        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getHx()),
                            orbit.getHxDot(),
                            4.4e-15);
        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getHy()),
                            orbit.getHyDot(),
                            8.0e-16);
        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getLv()),
                            orbit.getLvDot(),
                            1.2e-15);
        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getLE()),
                            orbit.getLEDot(),
                            7.8e-16);
        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getLM()),
                            orbit.getLMDot(),
                            8.8e-16);
        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getE()),
                            orbit.getEDot(),
                            7.0e-16);
        Assert.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getI()),
                            orbit.getIDot(),
                            5.7e-16);

    }

    private <S extends Function<CartesianOrbit, Double>>
    double differentiate(TimeStampedPVCoordinates pv, Frame frame, double mu, S picker) {
        final DSFactory factory = new DSFactory(1, 1);
        FiniteDifferencesDifferentiator differentiator = new FiniteDifferencesDifferentiator(8, 0.1);
        UnivariateDifferentiableFunction diff = differentiator.differentiate(new UnivariateFunction() {
            public double value(double dt) {
                return picker.apply(new CartesianOrbit(pv.shiftedBy(dt), frame, mu));
            }
        });
        return diff.value(factory.variable(0, 0.0)).getPartialDerivative(1);
    }

    @Test
    public void testEquatorialRetrograde() {
        Vector3D position = new Vector3D(10000000.0, 0.0, 0.0);
        Vector3D velocity = new Vector3D(0.0, -6500.0, 0.0);
        double r2 = position.getNormSq();
        double r  = FastMath.sqrt(r2);
        Vector3D acceleration = new Vector3D(-mu / (r * r2), position,
                                             1, new Vector3D(-0.1, 0.2, 0.3));
        PVCoordinates pvCoordinates = new PVCoordinates(position, velocity, acceleration);
        CartesianOrbit orbit = new CartesianOrbit(pvCoordinates, FramesFactory.getEME2000(), date, mu);
        Assert.assertEquals(10637829.465, orbit.getA(), 1.0e-3);
        Assert.assertEquals(-738.145, orbit.getADot(), 1.0e-3);
        Assert.assertEquals(0.05995861, orbit.getE(), 1.0e-8);
        Assert.assertEquals(-6.523e-5, orbit.getEDot(), 1.0e-8);
        Assert.assertEquals(FastMath.PI, orbit.getI(), 1.0e-15);
        Assert.assertTrue(Double.isNaN(orbit.getIDot()));
        Assert.assertTrue(Double.isNaN(orbit.getHx()));
        Assert.assertTrue(Double.isNaN(orbit.getHxDot()));
        Assert.assertTrue(Double.isNaN(orbit.getHy()));
        Assert.assertTrue(Double.isNaN(orbit.getHyDot()));
    }

    @Test
    public void testToString() {
        Vector3D position = new Vector3D(-29536113.0, 30329259.0, -100125.0);
        Vector3D velocity = new Vector3D(-2194.0, -2141.0, -8.0);
        PVCoordinates pvCoordinates = new PVCoordinates(position, velocity);
        CartesianOrbit orbit = new CartesianOrbit(pvCoordinates, FramesFactory.getEME2000(), date, mu);
        Assert.assertEquals("Cartesian parameters: {P(-2.9536113E7, 3.0329259E7, -100125.0), V(-2194.0, -2141.0, -8.0)}",
                            orbit.toString());
    }

    @Test
    public void testCopyNonKeplerianAcceleration() {

        final Frame eme2000     = FramesFactory.getEME2000();

        // Define GEO satellite position
        final Vector3D position = new Vector3D(42164140, 0, 0);
        // Build PVCoodrinates starting from its position and computing the corresponding circular velocity
        final PVCoordinates pv  = new PVCoordinates(position,
                                       new Vector3D(0, FastMath.sqrt(mu / position.getNorm()), 0));
        // Build a KeplerianOrbit in eme2000
        final Orbit orbit = new CartesianOrbit(pv, eme2000, date, mu);

        // Build another KeplerianOrbit as a copy of the first one
        final Orbit orbitCopy = new CartesianOrbit(orbit);

        // Shift the orbit of a time-interval
        final Orbit shiftedOrbit = orbit.shiftedBy(10); // This works good
        final Orbit shiftedOrbitCopy = orbitCopy.shiftedBy(10); // This does not work

        Assert.assertEquals(0.0,
                            Vector3D.distance(shiftedOrbit.getPVCoordinates().getPosition(),
                                              shiftedOrbitCopy.getPVCoordinates().getPosition()),
                            1.0e-10);
        Assert.assertEquals(0.0,
                            Vector3D.distance(shiftedOrbit.getPVCoordinates().getVelocity(),
                                              shiftedOrbitCopy.getPVCoordinates().getVelocity()),
                            1.0e-10);

    }

    @Before
    public void setUp() {

        Utils.setDataRoot("regular-data");

        // Computation date
        date = AbsoluteDate.J2000_EPOCH;

        // Body mu
        mu = 3.9860047e14;
    }

    @After
    public void tearDown() {
        date = null;
    }

}

