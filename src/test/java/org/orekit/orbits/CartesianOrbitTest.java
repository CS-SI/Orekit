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
package org.orekit.orbits;

import org.hipparchus.analysis.UnivariateFunction;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.FiniteDifferencesDifferentiator;
import org.hipparchus.analysis.differentiation.UnivariateDifferentiableFunction;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrixPreservingVisitor;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Function;


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

        Assertions.assertEquals(p.getPosition().getX(), pvCoordinates.getPosition().getX(), Utils.epsilonTest * FastMath.abs(pvCoordinates.getPosition().getX()));
        Assertions.assertEquals(p.getPosition().getY(), pvCoordinates.getPosition().getY(), Utils.epsilonTest * FastMath.abs(pvCoordinates.getPosition().getY()));
        Assertions.assertEquals(p.getPosition().getZ(), pvCoordinates.getPosition().getZ(), Utils.epsilonTest * FastMath.abs(pvCoordinates.getPosition().getZ()));
        Assertions.assertEquals(p.getPVCoordinates().getVelocity().getX(), pvCoordinates.getVelocity().getX(), Utils.epsilonTest * FastMath.abs(pvCoordinates.getVelocity().getX()));
        Assertions.assertEquals(p.getPVCoordinates().getVelocity().getY(), pvCoordinates.getVelocity().getY(), Utils.epsilonTest * FastMath.abs(pvCoordinates.getVelocity().getY()));
        Assertions.assertEquals(p.getPVCoordinates().getVelocity().getZ(), pvCoordinates.getVelocity().getZ(), Utils.epsilonTest * FastMath.abs(pvCoordinates.getVelocity().getZ()));

        Method initPV = CartesianOrbit.class.getDeclaredMethod("initPVCoordinates", new Class[0]);
        initPV.setAccessible(true);
        Assertions.assertSame(p.getPVCoordinates(), initPV.invoke(p, new Object[0]));

    }

    @Test
    public void testCartesianToEquinoctial() {

        Vector3D position = new Vector3D(-29536113.0, 30329259.0, -100125.0);
        Vector3D velocity = new Vector3D(-2194.0, -2141.0, -8.0);
        PVCoordinates pvCoordinates = new PVCoordinates( position, velocity);

        CartesianOrbit p = new CartesianOrbit(pvCoordinates, FramesFactory.getEME2000(), date, mu);

        Assertions.assertEquals(42255170.0028257,  p.getA(), Utils.epsilonTest * p.getA());
        Assertions.assertEquals(0.592732497856475e-03,  p.getEquinoctialEx(), Utils.epsilonE * FastMath.abs(p.getE()));
        Assertions.assertEquals(-0.206274396964359e-02, p.getEquinoctialEy(), Utils.epsilonE * FastMath.abs(p.getE()));
        Assertions.assertEquals(FastMath.sqrt(FastMath.pow(0.592732497856475e-03, 2)+FastMath.pow(-0.206274396964359e-02, 2)), p.getE(), Utils.epsilonAngle * FastMath.abs(p.getE()));
        Assertions.assertEquals(MathUtils.normalizeAngle(2*FastMath.asin(FastMath.sqrt((FastMath.pow(0.128021863908325e-03, 2)+FastMath.pow(-0.352136186881817e-02, 2))/4.)), p.getI()), p.getI(), Utils.epsilonAngle * FastMath.abs(p.getI()));
        Assertions.assertEquals(MathUtils.normalizeAngle(0.234498139679291e+01, p.getLM()), p.getLM(), Utils.epsilonAngle * FastMath.abs(p.getLM()));

        // trigger a specific path in copy constructor
        CartesianOrbit q = new CartesianOrbit(p);

        Assertions.assertEquals(42255170.0028257,  q.getA(), Utils.epsilonTest * q.getA());
        Assertions.assertEquals(0.592732497856475e-03,  q.getEquinoctialEx(), Utils.epsilonE * FastMath.abs(q.getE()));
        Assertions.assertEquals(-0.206274396964359e-02, q.getEquinoctialEy(), Utils.epsilonE * FastMath.abs(q.getE()));
        Assertions.assertEquals(FastMath.sqrt(FastMath.pow(0.592732497856475e-03, 2)+FastMath.pow(-0.206274396964359e-02, 2)), q.getE(), Utils.epsilonAngle * FastMath.abs(q.getE()));
        Assertions.assertEquals(MathUtils.normalizeAngle(2*FastMath.asin(FastMath.sqrt((FastMath.pow(0.128021863908325e-03, 2)+FastMath.pow(-0.352136186881817e-02, 2))/4.)), q.getI()), q.getI(), Utils.epsilonAngle * FastMath.abs(q.getI()));
        Assertions.assertEquals(MathUtils.normalizeAngle(0.234498139679291e+01, q.getLM()), q.getLM(), Utils.epsilonAngle * FastMath.abs(q.getLM()));

        Assertions.assertTrue(Double.isNaN(q.getADot()));
        Assertions.assertTrue(Double.isNaN(q.getEquinoctialExDot()));
        Assertions.assertTrue(Double.isNaN(q.getEquinoctialEyDot()));
        Assertions.assertTrue(Double.isNaN(q.getHxDot()));
        Assertions.assertTrue(Double.isNaN(q.getHyDot()));
        Assertions.assertTrue(Double.isNaN(q.getLvDot()));
        Assertions.assertTrue(Double.isNaN(q.getEDot()));
        Assertions.assertTrue(Double.isNaN(q.getIDot()));

    }

    @Test
    public void testCartesianToKeplerian(){

        Vector3D position = new Vector3D(-26655470.0, 29881667.0, -113657.0);
        Vector3D velocity = new Vector3D(-1125.0, -1122.0, 195.0);
        PVCoordinates pvCoordinates = new PVCoordinates( position, velocity);
        double mu = 3.9860047e14;

        CartesianOrbit p = new CartesianOrbit(pvCoordinates, FramesFactory.getEME2000(), date, mu);
        KeplerianOrbit kep = new KeplerianOrbit(p);

        Assertions.assertEquals(22979265.3030773,  p.getA(), Utils.epsilonTest  * p.getA());
        Assertions.assertEquals(0.743502611664700, p.getE(), Utils.epsilonE     * FastMath.abs(p.getE()));
        Assertions.assertEquals(0.122182096220906, p.getI(), Utils.epsilonAngle * FastMath.abs(p.getI()));
        double pa = kep.getPerigeeArgument();
        Assertions.assertEquals(MathUtils.normalizeAngle(3.09909041016672, pa), pa,
                     Utils.epsilonAngle * FastMath.abs(pa));
        double raan = kep.getRightAscensionOfAscendingNode();
        Assertions.assertEquals(MathUtils.normalizeAngle(2.32231010979999, raan), raan,
                     Utils.epsilonAngle * FastMath.abs(raan));
        double m = kep.getMeanAnomaly();
        Assertions.assertEquals(MathUtils.normalizeAngle(3.22888977629034, m), m,
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
        Assertions.assertEquals(a * epsilon * epsilon / ksi,
                     p.getPosition().getNorm(),
                     Utils.epsilonTest * FastMath.abs(p.getPosition().getNorm()));

        // validation of: V = sqrt(mu.(1+2e.cos(v)+e2)/a.(1-e2) )
        Assertions.assertEquals(na * FastMath.sqrt(ksi * ksi + nu * nu) / epsilon,
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
                                          p.getHx(), p.getHy(), lv, PositionAngleType.TRUE, p.getFrame(), date, mu);
            position = p.getPosition();

            // test if the norm of the position is in the range [perigee radius, apogee radius]
            // Warning: these tests are without absolute value by choice
            Assertions.assertTrue((position.getNorm() - apogeeRadius)  <= (  apogeeRadius * Utils.epsilonTest));
            Assertions.assertTrue((position.getNorm() - perigeeRadius) >= (- perigeeRadius * Utils.epsilonTest));
            // Assertions.assertTrue(position.getNorm() <= apogeeRadius);
            // Assertions.assertTrue(position.getNorm() >= perigeeRadius);

            position= position.normalize();
            velocity = p.getPVCoordinates().getVelocity().normalize();

            // at this stage of computation, all the vectors (position, velocity and momemtum) are normalized here

            // test of orthogonality between position and momentum
            Assertions.assertTrue(FastMath.abs(Vector3D.dotProduct(position, momentum)) < Utils.epsilonTest);
            // test of orthogonality between velocity and momentum
            Assertions.assertTrue(FastMath.abs(Vector3D.dotProduct(velocity, momentum)) < Utils.epsilonTest);
        }
    }

    @Test
    public void testHyperbola1() {
        CartesianOrbit orbit = new CartesianOrbit(new KeplerianOrbit(-10000000.0, 2.5, 0.3, 0, 0, 0.0,
                                                                     PositionAngleType.TRUE,
                                                                     FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH,
                                                                     mu));
        Vector3D perigeeP  = orbit.getPosition();
        Vector3D u = perigeeP.normalize();
        Vector3D focus1 = Vector3D.ZERO;
        Vector3D focus2 = new Vector3D(-2 * orbit.getA() * orbit.getE(), u);
        for (double dt = -5000; dt < 5000; dt += 60) {
            PVCoordinates pv = orbit.shiftedBy(dt).getPVCoordinates();
            double d1 = Vector3D.distance(pv.getPosition(), focus1);
            double d2 = Vector3D.distance(pv.getPosition(), focus2);
            Assertions.assertEquals(-2 * orbit.getA(), FastMath.abs(d1 - d2), 1.0e-6);
            CartesianOrbit rebuilt =
                new CartesianOrbit(pv, orbit.getFrame(), orbit.getDate().shiftedBy(dt), mu);
            Assertions.assertEquals(-10000000.0, rebuilt.getA(), 1.0e-6);
            Assertions.assertEquals(2.5, rebuilt.getE(), 1.0e-13);
        }
    }

    @Test
    public void testHyperbola2() {
        CartesianOrbit orbit = new CartesianOrbit(new KeplerianOrbit(-10000000.0, 1.2, 0.3, 0, 0, -1.75,
                                                                     PositionAngleType.MEAN,
                                                                     FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH,
                                                                     mu));
        Vector3D perigeeP  = new KeplerianOrbit(-10000000.0, 1.2, 0.3, 0, 0, 0.0, PositionAngleType.TRUE, orbit.getFrame(),
                                                orbit.getDate(), orbit.getMu()).getPosition();
        Vector3D u = perigeeP.normalize();
        Vector3D focus1 = Vector3D.ZERO;
        Vector3D focus2 = new Vector3D(-2 * orbit.getA() * orbit.getE(), u);
        for (double dt = -5000; dt < 5000; dt += 60) {
            PVCoordinates pv = orbit.shiftedBy(dt).getPVCoordinates();
            double d1 = Vector3D.distance(pv.getPosition(), focus1);
            double d2 = Vector3D.distance(pv.getPosition(), focus2);
            Assertions.assertEquals(-2 * orbit.getA(), FastMath.abs(d1 - d2), 1.0e-6);
            CartesianOrbit rebuilt =
                new CartesianOrbit(pv, orbit.getFrame(), orbit.getDate().shiftedBy(dt), mu);
            Assertions.assertEquals(-10000000.0, rebuilt.getA(), 1.0e-6);
            Assertions.assertEquals(1.2, rebuilt.getE(), 1.0e-13);
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
        Assertions.assertEquals(0.0, orbit.getE(), 2.0e-14);
    }

    @Test
    public void testSerialization()
      throws IOException, ClassNotFoundException {
        Vector3D position = new Vector3D(-29536113.0, 30329259.0, -100125.0);
        Vector3D velocity = new Vector3D(-2194.0, -2141.0, -8.0);
        PVCoordinates pvCoordinates = new PVCoordinates(position, velocity);
        CartesianOrbit orbit = new CartesianOrbit(pvCoordinates, FramesFactory.getEME2000(), date, mu);
        Assertions.assertEquals(42255170.003, orbit.getA(), 1.0e-3);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream    oos = new ObjectOutputStream(bos);
        oos.writeObject(orbit);

        Assertions.assertTrue(bos.size() > 270);
        Assertions.assertTrue(bos.size() < 320);

        ByteArrayInputStream  bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream     ois = new ObjectInputStream(bis);
        CartesianOrbit deserialized  = (CartesianOrbit) ois.readObject();
        PVCoordinates dpv = new PVCoordinates(orbit.getPVCoordinates(), deserialized.getPVCoordinates());
        Assertions.assertEquals(0.0, dpv.getPosition().getNorm(), 1.0e-10);
        Assertions.assertEquals(0.0, dpv.getVelocity().getNorm(), 1.0e-10);
        Assertions.assertTrue(Double.isNaN(orbit.getADot()) && Double.isNaN(deserialized.getADot()));
        Assertions.assertTrue(Double.isNaN(orbit.getEDot()) && Double.isNaN(deserialized.getEDot()));
        Assertions.assertTrue(Double.isNaN(orbit.getIDot()) && Double.isNaN(deserialized.getIDot()));
        Assertions.assertTrue(Double.isNaN(orbit.getEquinoctialExDot()) && Double.isNaN(deserialized.getEquinoctialExDot()));
        Assertions.assertTrue(Double.isNaN(orbit.getEquinoctialEyDot()) && Double.isNaN(deserialized.getEquinoctialEyDot()));
        Assertions.assertTrue(Double.isNaN(orbit.getHxDot()) && Double.isNaN(deserialized.getHxDot()));
        Assertions.assertTrue(Double.isNaN(orbit.getHyDot()) && Double.isNaN(deserialized.getHyDot()));
        Assertions.assertTrue(Double.isNaN(orbit.getLvDot()) && Double.isNaN(deserialized.getLvDot()));
        Assertions.assertEquals(orbit.getDate(), deserialized.getDate());
        Assertions.assertEquals(orbit.getMu(), deserialized.getMu(), 1.0e-10);
        Assertions.assertEquals(orbit.getFrame().getName(), deserialized.getFrame().getName());

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
        Assertions.assertEquals(42255170.003, orbit.getA(), 1.0e-3);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream    oos = new ObjectOutputStream(bos);
        oos.writeObject(orbit);

        Assertions.assertTrue(bos.size() > 320);
        Assertions.assertTrue(bos.size() < 370);

        ByteArrayInputStream  bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream     ois = new ObjectInputStream(bis);
        CartesianOrbit deserialized  = (CartesianOrbit) ois.readObject();
        PVCoordinates dpv = new PVCoordinates(orbit.getPVCoordinates(), deserialized.getPVCoordinates());
        Assertions.assertEquals(0.0, dpv.getPosition().getNorm(), 1.0e-10);
        Assertions.assertEquals(0.0, dpv.getVelocity().getNorm(), 1.0e-10);
        Assertions.assertEquals(orbit.getADot(), deserialized.getADot(), 1.0e-10);
        Assertions.assertEquals(orbit.getEDot(), deserialized.getEDot(), 1.0e-10);
        Assertions.assertEquals(orbit.getIDot(), deserialized.getIDot(), 1.0e-10);
        Assertions.assertEquals(orbit.getEquinoctialExDot(), deserialized.getEquinoctialExDot(), 1.0e-10);
        Assertions.assertEquals(orbit.getEquinoctialEyDot(), deserialized.getEquinoctialEyDot(), 1.0e-10);
        Assertions.assertEquals(orbit.getHxDot(), deserialized.getHxDot(), 1.0e-10);
        Assertions.assertEquals(orbit.getHyDot(), deserialized.getHyDot(), 1.0e-10);
        Assertions.assertEquals(orbit.getLvDot(), deserialized.getLvDot(), 1.0e-10);
        Assertions.assertEquals(orbit.getDate(), deserialized.getDate());
        Assertions.assertEquals(orbit.getMu(), deserialized.getMu(), 1.0e-10);
        Assertions.assertEquals(orbit.getFrame().getName(), deserialized.getFrame().getName());

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
        Assertions.assertTrue(orbit.hasDerivatives());
        double r2 = position.getNormSq();
        double r  = FastMath.sqrt(r2);
        Vector3D keplerianAcceleration = new Vector3D(-orbit.getMu() / (r2 * r), position);
        Assertions.assertEquals(0.0101, Vector3D.distance(keplerianAcceleration, acceleration), 1.0e-4);

        for (OrbitType type : OrbitType.values()) {
            Orbit converted = type.convertType(orbit);
            Assertions.assertTrue(converted.hasDerivatives());
            CartesianOrbit rebuilt = (CartesianOrbit) OrbitType.CARTESIAN.convertType(converted);
            Assertions.assertTrue(rebuilt.hasDerivatives());
            Assertions.assertEquals(0, Vector3D.distance(rebuilt.getPosition(),     position),     2.0e-9);
            Assertions.assertEquals(0, Vector3D.distance(rebuilt.getPVCoordinates().getVelocity(),     velocity),     2.5e-12);
            Assertions.assertEquals(0, Vector3D.distance(rebuilt.getPVCoordinates().getAcceleration(), acceleration), 4.9e-15);
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
        Assertions.assertTrue(orbit.hasDerivatives());
        double r2 = position.getNormSq();
        double r  = FastMath.sqrt(r2);
        Vector3D keplerianAcceleration = new Vector3D(-orbit.getMu() / (r2 * r), position);
        Assertions.assertEquals(4.78e-4, Vector3D.distance(keplerianAcceleration, acceleration), 1.0e-6);

        OrbitType type = OrbitType.KEPLERIAN;
        Orbit converted = type.convertType(orbit);
        Assertions.assertTrue(converted.hasDerivatives());
        CartesianOrbit rebuilt = (CartesianOrbit) OrbitType.CARTESIAN.convertType(converted);
        Assertions.assertTrue(rebuilt.hasDerivatives());
        Assertions.assertEquals(0, Vector3D.distance(rebuilt.getPosition(),     position),     1.0e-15);
        Assertions.assertEquals(0, Vector3D.distance(rebuilt.getPVCoordinates().getVelocity(),     velocity),     1.0e-15);
        Assertions.assertEquals(0, Vector3D.distance(rebuilt.getPVCoordinates().getAcceleration(), acceleration), 1.0e-15);

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

    @Test
    public void testNumericalIssue1015() {
        Vector3D position = new Vector3D(-1466739.735988, 1586390.713569, 6812901.677773);
        Vector3D velocity = new Vector3D(-9532.812, -4321.894, -1409.018);
        PVCoordinates pvCoordinates = new PVCoordinates(position, velocity);
        CartesianOrbit orbit = new CartesianOrbit(pvCoordinates, FramesFactory.getEME2000(), date, 3.986004415E14);
        testShift(orbit, new KeplerianOrbit(orbit), 1.0e-10);
    }

    private void testShift(CartesianOrbit tested, Orbit reference, double threshold) {
        for (double dt = - 1000; dt < 1000; dt += 10.0) {

            PVCoordinates pvTested    = tested.shiftedBy(dt).getPVCoordinates();
            Vector3D      pTested     = pvTested.getPosition();
            Vector3D      vTested     = pvTested.getVelocity();

            PVCoordinates pvReference = reference.shiftedBy(dt).getPVCoordinates();
            Vector3D      pReference  = pvReference.getPosition();
            Vector3D      vReference  = pvReference.getVelocity();

            Assertions.assertEquals(0, pTested.subtract(pReference).getNorm(), threshold * pReference.getNorm());
            Assertions.assertEquals(0, vTested.subtract(vReference).getNorm(), threshold * vReference.getNorm());

        }
    }

    @Test
    public void testNonInertialFrame() throws IllegalArgumentException {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Vector3D position = new Vector3D(-26655470.0, 29881667.0, -113657.0);
            Vector3D velocity = new Vector3D(-1125.0, -1122.0, 195.0);
            PVCoordinates pvCoordinates = new PVCoordinates( position, velocity);
            double mu = 3.9860047e14;
            new CartesianOrbit(pvCoordinates,
                    new Frame(FramesFactory.getEME2000(), Transform.IDENTITY, "non-inertial", false),
                    date, mu);
        });
    }

    @Test
    public void testJacobianReference() {

        Vector3D position = new Vector3D(-29536113.0, 30329259.0, -100125.0);
        Vector3D velocity = new Vector3D(-2194.0, -2141.0, -8.0);
        PVCoordinates pvCoordinates = new PVCoordinates( position, velocity);
        CartesianOrbit orbit = new CartesianOrbit(pvCoordinates, FramesFactory.getEME2000(), date, mu);

        double[][] jacobian = new double[6][6];
        orbit.getJacobianWrtCartesian(PositionAngleType.MEAN, jacobian);

        for (int i = 0; i < jacobian.length; i++) {
            double[] row    = jacobian[i];
            for (int j = 0; j < row.length; j++) {
                Assertions.assertEquals((i == j) ? 1 : 0, row[j], 1.0e-15);
            }
        }

        double[][] invJacobian = new double[6][6];
        orbit.getJacobianWrtParameters(PositionAngleType.MEAN, invJacobian);
        MatrixUtils.createRealMatrix(jacobian).
                        multiply(MatrixUtils.createRealMatrix(invJacobian)).
        walkInRowOrder(new RealMatrixPreservingVisitor() {
            public void start(int rows, int columns,
                              int startRow, int endRow, int startColumn, int endColumn) {
            }

            public void visit(int row, int column, double value) {
                Assertions.assertEquals(row == column ? 1.0 : 0.0, value, 1.0e-15);
            }

            public double end() {
                return Double.NaN;
            }
        });

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

        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getA()),
                            orbit.getADot(),
                            4.3e-8);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getEquinoctialEx()),
                            orbit.getEquinoctialExDot(),
                            2.1e-15);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getEquinoctialEy()),
                            orbit.getEquinoctialEyDot(),
                            5.3e-16);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getHx()),
                            orbit.getHxDot(),
                            4.4e-15);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getHy()),
                            orbit.getHyDot(),
                            8.0e-16);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getLv()),
                            orbit.getLvDot(),
                            1.2e-15);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getLE()),
                            orbit.getLEDot(),
                            7.8e-16);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getLM()),
                            orbit.getLMDot(),
                            8.8e-16);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getE()),
                            orbit.getEDot(),
                            7.0e-16);
        Assertions.assertEquals(differentiate(pv, frame, mu, shifted -> shifted.getI()),
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
        Assertions.assertEquals(10637829.465, orbit.getA(), 1.0e-3);
        Assertions.assertEquals(-738.145, orbit.getADot(), 1.0e-3);
        Assertions.assertEquals(0.05995861, orbit.getE(), 1.0e-8);
        Assertions.assertEquals(-6.523e-5, orbit.getEDot(), 1.0e-8);
        Assertions.assertEquals(FastMath.PI, orbit.getI(), 1.0e-15);
        Assertions.assertTrue(Double.isNaN(orbit.getIDot()));
        Assertions.assertTrue(Double.isNaN(orbit.getHx()));
        Assertions.assertTrue(Double.isNaN(orbit.getHxDot()));
        Assertions.assertTrue(Double.isNaN(orbit.getHy()));
        Assertions.assertTrue(Double.isNaN(orbit.getHyDot()));
    }

    @Test
    public void testToString() {
        Vector3D position = new Vector3D(-29536113.0, 30329259.0, -100125.0);
        Vector3D velocity = new Vector3D(-2194.0, -2141.0, -8.0);
        PVCoordinates pvCoordinates = new PVCoordinates(position, velocity);
        CartesianOrbit orbit = new CartesianOrbit(pvCoordinates, FramesFactory.getEME2000(), date, mu);
        Assertions.assertEquals("Cartesian parameters: {P(-2.9536113E7, 3.0329259E7, -100125.0), V(-2194.0, -2141.0, -8.0)}",
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

        Assertions.assertEquals(0.0,
                            Vector3D.distance(shiftedOrbit.getPosition(),
                                              shiftedOrbitCopy.getPosition()),
                            1.0e-10);
        Assertions.assertEquals(0.0,
                            Vector3D.distance(shiftedOrbit.getPVCoordinates().getVelocity(),
                                              shiftedOrbitCopy.getPVCoordinates().getVelocity()),
                            1.0e-10);

    }

    @Test
    public void testNormalize() {
        final Vector3D position = new Vector3D(42164140, 0, 0);
        final PVCoordinates pv  = new PVCoordinates(position,
                                       new Vector3D(0, FastMath.sqrt(mu / position.getNorm()), 0));
        final Orbit orbit = new CartesianOrbit(pv, FramesFactory.getEME2000(), date, mu);
        Assertions.assertSame(orbit, orbit.getType().normalize(orbit, null));
    }

    @Test
    public void testIssue1139() {

        // Create
        Vector3D position = new Vector3D(-29536113.0, 30329259.0, -100125.0);
        Vector3D velocity = new Vector3D(-2194.0, -2141.0, -8.0);
        PVCoordinates pvCoordinates = new PVCoordinates( position, velocity);

        CartesianOrbit p = new CartesianOrbit(pvCoordinates, FramesFactory.getEME2000(), date, mu);

        double dt = 60.0;
        AbsoluteDate shiftedEpoch = date.shiftedBy(dt);

        CartesianOrbit p2 = new CartesianOrbit(pvCoordinates, FramesFactory.getEME2000(), shiftedEpoch, mu);

        // Verify
        Assertions.assertEquals(dt, shiftedEpoch.durationFrom(date));
        Assertions.assertEquals(dt, p2.durationFrom(p));
        Assertions.assertEquals(dt, p2.getDate().durationFrom(p));
        Assertions.assertEquals(dt, p2.durationFrom(p.getDate()));
        Assertions.assertEquals(dt, p2.getDate().durationFrom(p.getDate()));
        Assertions.assertEquals(-dt, p.durationFrom(p2));
        
    }

    @BeforeEach
    public void setUp() {

        Utils.setDataRoot("regular-data");

        // Computation date
        date = AbsoluteDate.J2000_EPOCH;

        // Body mu
        mu = 3.9860047e14;
    }

    @AfterEach
    public void tearDown() {
        date = null;
    }

}

