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
package org.orekit.orbits;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

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
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.PVCoordinates;


public class CartesianParametersTest {

    // Computation date
    private AbsoluteDate date;

    // Body mu
    private double mu;

    @Test
    public void testCartesianToCartesian() {

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
        Assert.assertEquals(FastMath.sqrt(FastMath.pow(0.592732497856475e-03,2)+FastMath.pow(-0.206274396964359e-02,2)), p.getE(), Utils.epsilonAngle * FastMath.abs(p.getE()));
        Assert.assertEquals(MathUtils.normalizeAngle(2*FastMath.asin(FastMath.sqrt((FastMath.pow(0.128021863908325e-03,2)+FastMath.pow(-0.352136186881817e-02,2))/4.)),p.getI()), p.getI(), Utils.epsilonAngle * FastMath.abs(p.getI()));
        Assert.assertEquals(MathUtils.normalizeAngle(0.234498139679291e+01,p.getLM()), p.getLM(), Utils.epsilonAngle * FastMath.abs(p.getLM()));
    }

    @Test
    public void testCartesianToKeplerian(){

        Vector3D position = new Vector3D(-26655470.0, 29881667.0,-113657.0);
        Vector3D velocity = new Vector3D(-1125.0,-1122.0,195.0);
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
    public void testNumericalIssue25() throws OrekitException {
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
      throws IOException, ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        Vector3D position = new Vector3D(-29536113.0, 30329259.0, -100125.0);
        Vector3D velocity = new Vector3D(-2194.0, -2141.0, -8.0);
        PVCoordinates pvCoordinates = new PVCoordinates( position, velocity);
        CartesianOrbit orbit = new CartesianOrbit(pvCoordinates, FramesFactory.getEME2000(), date, mu);
        Assert.assertEquals(42255170.003, orbit.getA(), 1.0e-3);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream    oos = new ObjectOutputStream(bos);
        oos.writeObject(orbit);

        Assert.assertTrue(bos.size() > 250);
        Assert.assertTrue(bos.size() < 350);

        ByteArrayInputStream  bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream     ois = new ObjectInputStream(bis);
        CartesianOrbit deserialized  = (CartesianOrbit) ois.readObject();
        PVCoordinates dpv = new PVCoordinates(orbit.getPVCoordinates(), deserialized.getPVCoordinates());
        Assert.assertEquals(0.0, dpv.getPosition().getNorm(), 1.0e-10);
        Assert.assertEquals(0.0, dpv.getVelocity().getNorm(), 1.0e-10);
        Assert.assertEquals(orbit.getDate(), deserialized.getDate());
        Assert.assertEquals(orbit.getMu(), deserialized.getMu(), 1.0e-10);

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
    public void testShiftHyperbolic() {
        Vector3D position = new Vector3D(-29536113.0, 30329259.0, -100125.0);
        Vector3D velocity = new Vector3D(3 * FastMath.sqrt(mu / position.getNorm()), position.orthogonal());
        PVCoordinates pvCoordinates = new PVCoordinates(position, velocity);
        CartesianOrbit orbit = new CartesianOrbit(pvCoordinates, FramesFactory.getEME2000(), date, mu);
        testShift(orbit, new KeplerianOrbit(orbit), 1.0e-15);
    }

    @Test
    public void testNumericalIssue135() throws OrekitException {
        Vector3D position = new Vector3D(-6.7884943832e7, -2.1423006112e7, -3.1603915377e7);
        Vector3D velocity = new Vector3D(-4732.55, -2472.086, -3022.177);
        PVCoordinates pvCoordinates = new PVCoordinates(position, velocity);
        CartesianOrbit orbit = new CartesianOrbit(pvCoordinates, FramesFactory.getEME2000(), date,
                                                  324858598826460.);
        testShift(orbit, new KeplerianOrbit(orbit), 3.0e-15);
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

        Vector3D position = new Vector3D(-26655470.0, 29881667.0,-113657.0);
        Vector3D velocity = new Vector3D(-1125.0,-1122.0,195.0);
        PVCoordinates pvCoordinates = new PVCoordinates( position, velocity);
        double mu = 3.9860047e14;
        new CartesianOrbit(pvCoordinates,
                           new Frame(FramesFactory.getEME2000(), Transform.IDENTITY, "non-inertial", false),
                           date, mu);
    }

    @Test
    public void testJacobianReference() throws OrekitException {

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
    public void testInterpolation() throws OrekitException {

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
            sample.add(propagator.propagate(date.shiftedBy(dt)).getOrbit());
        }

        // well inside the sample, interpolation should be much better than Keplerian shift
        // this is bacause we take the full non-Keplerian acceleration into account in
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
        Assert.assertTrue(maxShiftPError         > 390.0);
        Assert.assertTrue(maxInterpolationPError < 3.0e-8);
        Assert.assertTrue(maxShiftVError         > 3.0);
        Assert.assertTrue(maxInterpolationVError < 2.0e-9);

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
        Assert.assertTrue(maxShiftPError         < 2500.0);
        Assert.assertTrue(maxInterpolationPError > 6000.0);
        Assert.assertTrue(maxShiftVError         <    7.0);
        Assert.assertTrue(maxInterpolationVError >  170.0);

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

