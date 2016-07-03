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
package org.orekit.propagation;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hipparchus.analysis.polynomials.PolynomialFunction;
import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.exception.MathIllegalStateException;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.BodyCenterPointing;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;


public class SpacecraftStateTest {

    @Test
    public void testShiftError()
        throws ParseException, OrekitException {


        // polynomial models for interpolation error in position, velocity, acceleration and attitude
        // these models grow as follows
        //   interpolation time (s)    position error (m)   velocity error (m/s)   acceleration error (m/s²)  attitude error (°)
        //           60                       30                    1                     0.014               0.00002
        //          120                      100                    2                     0.013               0.00009
        //          300                      600                    4                     0.011               0.0009
        //          600                     2000                    6                     0.006               0.006
        //          900                     4000                    6                     0.002               0.02
        // the expected maximum residuals with respect to these models are about 0.2m, 0.8mm/s, 7.9μm/s² and 2.8e-7°
        PolynomialFunction pModel = new PolynomialFunction(new double[] {
            -0.16513714130703838,    0.008052836586593358,  0.007306374155052651,
            -1.9942719771217313E-6, -1.8063354449344768E-9, 9.042229494006868E-13,
        });
        PolynomialFunction vModel = new PolynomialFunction(new double[] {
            -6.205041765231733E-4,   0.014820651821078279,  -7.458097665912488E-6,
            -1.5914983761563204E-9, -1.7936013181449342E-12, 2.187916095307246E-15,
        });
        PolynomialFunction aModel = new PolynomialFunction(new double[] {
            0.014788789776214587,   -1.418490913753379E-5,  1.947898940800356E-9,
            2.6179181901457335E-12, -7.603278343088821E-15, 2.968153861123117E-18,
        });
        PolynomialFunction rModel = new PolynomialFunction(new double[] {
            -2.7689062182403017E-6, 1.7406542555507358E-7,  2.510979532481025E-9,
             2.039932266627844E-11, 9.912634888010535E-15, -3.5015638902258456E-18,
        });

        AbsoluteDate centerDate = orbit.getDate().shiftedBy(100.0);
        SpacecraftState centerState = propagator.propagate(centerDate);
        double maxResidualP = 0;
        double maxResidualV = 0;
        double maxResidualA = 0;
        double maxResidualR = 0;
        for (double dt = 0; dt < 900.0; dt += 5) {
            SpacecraftState shifted = centerState.shiftedBy(dt);
            SpacecraftState propagated = propagator.propagate(centerDate.shiftedBy(dt));
            PVCoordinates dpv = new PVCoordinates(propagated.getPVCoordinates(), shifted.getPVCoordinates());
            double residualP = pModel.value(dt) - dpv.getPosition().getNorm();
            double residualV = vModel.value(dt) - dpv.getVelocity().getNorm();
            double residualA = aModel.value(dt) - dpv.getAcceleration().getNorm();
            double residualR = rModel.value(dt) -
                               FastMath.toDegrees(Rotation.distance(shifted.getAttitude().getRotation(),
                                                                    propagated.getAttitude().getRotation()));
            maxResidualP = FastMath.max(maxResidualP, FastMath.abs(residualP));
            maxResidualV = FastMath.max(maxResidualV, FastMath.abs(residualV));
            maxResidualA = FastMath.max(maxResidualA, FastMath.abs(residualA));
            maxResidualR = FastMath.max(maxResidualR, FastMath.abs(residualR));
        }
        Assert.assertEquals(0.2,    maxResidualP, 0.1);
        Assert.assertEquals(0.0008, maxResidualV, 0.0001);
        Assert.assertEquals(7.9e-6, maxResidualA, 1.0e-7);
        Assert.assertEquals(2.8e-6, maxResidualR, 1.0e-1);
    }

    @Test
    public void testInterpolation()
        throws ParseException, OrekitException {
        checkInterpolationError( 2,  106.46533, 0.40709287, 169847806.33e-9, 0.0, 450 * 450);
        checkInterpolationError( 3,    0.00353, 0.00003250,    189886.01e-9, 0.0, 0.0);
        checkInterpolationError( 4,    0.00002, 0.00000023,       232.25e-9, 0.0, 0.0);
    }

    private void checkInterpolationError(int n, double expectedErrorP, double expectedErrorV,
                                         double expectedErrorA, double expectedErrorM, double expectedErrorQ)
        throws OrekitException {
        AbsoluteDate centerDate = orbit.getDate().shiftedBy(100.0);
        SpacecraftState centerState = propagator.propagate(centerDate).addAdditionalState("quadratic", 0);
        List<SpacecraftState> sample = new ArrayList<SpacecraftState>();
        for (int i = 0; i < n; ++i) {
            double dt = i * 900.0 / (n - 1);
            SpacecraftState state = propagator.propagate(centerDate.shiftedBy(dt));
            state = state.addAdditionalState("quadratic", dt * dt);
            sample.add(state);
        }
        double maxErrorP = 0;
        double maxErrorV = 0;
        double maxErrorA = 0;
        double maxErrorM = 0;
        double maxErrorQ = 0;
        for (double dt = 0; dt < 900.0; dt += 5) {
            SpacecraftState interpolated = centerState.interpolate(centerDate.shiftedBy(dt), sample);
            SpacecraftState propagated = propagator.propagate(centerDate.shiftedBy(dt));
            PVCoordinates dpv = new PVCoordinates(propagated.getPVCoordinates(), interpolated.getPVCoordinates());
            maxErrorP = FastMath.max(maxErrorP, dpv.getPosition().getNorm());
            maxErrorV = FastMath.max(maxErrorV, dpv.getVelocity().getNorm());
            maxErrorA = FastMath.max(maxErrorA, FastMath.toDegrees(Rotation.distance(interpolated.getAttitude().getRotation(),
                                                                                                  propagated.getAttitude().getRotation())));
            maxErrorM = FastMath.max(maxErrorM, FastMath.abs(interpolated.getMass() - propagated.getMass()));
            maxErrorQ = FastMath.max(maxErrorQ, FastMath.abs(interpolated.getAdditionalState("quadratic")[0] - dt * dt));
        }
        Assert.assertEquals(expectedErrorP, maxErrorP, 1.0e-3);
        Assert.assertEquals(expectedErrorV, maxErrorV, 1.0e-6);
        Assert.assertEquals(expectedErrorA, maxErrorA, 4.0e-10);
        Assert.assertEquals(expectedErrorM, maxErrorM, 1.0e-15);
        Assert.assertEquals(expectedErrorQ, maxErrorQ, 2.0e-10);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testDatesConsistency() throws OrekitException {
        new SpacecraftState(orbit, attitudeLaw.getAttitude(orbit.shiftedBy(10.0),
                                                           orbit.getDate().shiftedBy(10.0), orbit.getFrame()));
    }

    /**
     * Check orbit and attitude dates can be off by a few ulps. I see this when using
     * FixedRate attitude provider.
     */
    @Test
    public void testDateConsistencyClose() throws OrekitException {
        //setup
        Orbit orbit10Shifts = orbit;
        for (int i = 0; i < 10; i++) {
            orbit10Shifts = orbit10Shifts.shiftedBy(0.1);
        }
        final Orbit orbit1Shift = orbit.shiftedBy(1);
        Attitude shiftedAttitude = attitudeLaw
                .getAttitude(orbit1Shift, orbit1Shift.getDate(), orbit.getFrame());

        //verify dates are very close, but not equal
        Assert.assertNotEquals(shiftedAttitude.getDate(), orbit10Shifts.getDate());
        Assert.assertEquals(
                shiftedAttitude.getDate().durationFrom(orbit10Shifts.getDate()),
                0, Precision.EPSILON);

        //action + verify no exception is thrown
        new SpacecraftState(orbit10Shifts, shiftedAttitude);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testFramesConsistency() throws OrekitException {
        new SpacecraftState(orbit,
                            new Attitude(orbit.getDate(),
                                         FramesFactory.getGCRF(),
                                         Rotation.IDENTITY, Vector3D.ZERO, Vector3D.ZERO));
    }

    @Test
    public void testTransform()
        throws ParseException, OrekitException {

        double maxDP = 0;
        double maxDV = 0;
        double maxDA = 0;
        for (double t = 0; t < orbit.getKeplerianPeriod(); t += 60) {
            final SpacecraftState state = propagator.propagate(orbit.getDate().shiftedBy(t));
            final Transform transform = state.toTransform().getInverse();
            PVCoordinates pv = transform.transformPVCoordinates(PVCoordinates.ZERO);
            PVCoordinates dPV = new PVCoordinates(pv, state.getPVCoordinates());
            Vector3D mZDirection = transform.transformVector(Vector3D.MINUS_K);
            double alpha = Vector3D.angle(mZDirection, state.getPVCoordinates().getPosition());
            maxDP = FastMath.max(maxDP, dPV.getPosition().getNorm());
            maxDV = FastMath.max(maxDV, dPV.getVelocity().getNorm());
            maxDA = FastMath.max(maxDA, FastMath.toDegrees(alpha));
        }
        Assert.assertEquals(0.0, maxDP, 1.0e-6);
        Assert.assertEquals(0.0, maxDV, 1.0e-9);
        Assert.assertEquals(0.0, maxDA, 1.0e-12);

    }

    @Test
    public void testAdditionalStates() throws OrekitException {
        final SpacecraftState state = propagator.propagate(orbit.getDate().shiftedBy(60));
        final SpacecraftState extended =
                state.
                 addAdditionalState("test-1", new double[] { 1.0, 2.0 }).
                  addAdditionalState("test-2", 42.0);
        Assert.assertEquals(0, state.getAdditionalStates().size());
        Assert.assertFalse(state.hasAdditionalState("test-1"));
        try {
            state.getAdditionalState("test-1");
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(oe.getSpecifier(), OrekitMessages.UNKNOWN_ADDITIONAL_STATE);
            Assert.assertEquals(oe.getParts()[0], "test-1");
        }
        try {
            state.ensureCompatibleAdditionalStates(extended);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(oe.getSpecifier(), OrekitMessages.UNKNOWN_ADDITIONAL_STATE);
            Assert.assertTrue(oe.getParts()[0].toString().startsWith("test-"));
        }
        try {
            extended.ensureCompatibleAdditionalStates(state);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(oe.getSpecifier(), OrekitMessages.UNKNOWN_ADDITIONAL_STATE);
            Assert.assertTrue(oe.getParts()[0].toString().startsWith("test-"));
        }
        try {
            extended.ensureCompatibleAdditionalStates(extended.addAdditionalState("test-2", new double[7]));
            Assert.fail("an exception should have been thrown");
        } catch (MathIllegalStateException mise) {
            Assert.assertEquals(LocalizedCoreFormats.DIMENSIONS_MISMATCH, mise.getSpecifier());
            Assert.assertEquals(7, ((Integer) mise.getParts()[0]).intValue());
        }
        Assert.assertEquals(2, extended.getAdditionalStates().size());
        Assert.assertTrue(extended.hasAdditionalState("test-1"));
        Assert.assertTrue(extended.hasAdditionalState("test-2"));
        Assert.assertEquals( 1.0, extended.getAdditionalState("test-1")[0], 1.0e-15);
        Assert.assertEquals( 2.0, extended.getAdditionalState("test-1")[1], 1.0e-15);
        Assert.assertEquals(42.0, extended.getAdditionalState("test-2")[0], 1.0e-15);

        // test various constructors
        Map<String, double[]> map = new HashMap<String, double[]>();
        map.put("test-3", new double[] { -6.0 });
        SpacecraftState sO = new SpacecraftState(state.getOrbit(), map);
        Assert.assertEquals(-6.0, sO.getAdditionalState("test-3")[0], 1.0e-15);
        SpacecraftState sOA = new SpacecraftState(state.getOrbit(), state.getAttitude(), map);
        Assert.assertEquals(-6.0, sOA.getAdditionalState("test-3")[0], 1.0e-15);
        SpacecraftState sOM = new SpacecraftState(state.getOrbit(), state.getMass(), map);
        Assert.assertEquals(-6.0, sOM.getAdditionalState("test-3")[0], 1.0e-15);
        SpacecraftState sOAM = new SpacecraftState(state.getOrbit(), state.getAttitude(), state.getMass(), map);
        Assert.assertEquals(-6.0, sOAM.getAdditionalState("test-3")[0], 1.0e-15);

    }

    @Test
    public void testSerialization()
            throws IOException, ClassNotFoundException, NoSuchFieldException, IllegalAccessException, OrekitException {

        propagator.resetInitialState(propagator.getInitialState().
                                     addAdditionalState("p1", 12.25).
                                     addAdditionalState("p2", 1, 2, 3));
        SpacecraftState state = propagator.propagate(orbit.getDate().shiftedBy(123.456));

        Assert.assertEquals(2, state.getAdditionalStates().size());
        Assert.assertEquals(1, state.getAdditionalState("p1").length);
        Assert.assertEquals(12.25, state.getAdditionalState("p1")[0], 1.0e-15);
        Assert.assertEquals(3, state.getAdditionalState("p2").length);
        Assert.assertEquals(1.0, state.getAdditionalState("p2")[0], 1.0e-15);
        Assert.assertEquals(2.0, state.getAdditionalState("p2")[1], 1.0e-15);
        Assert.assertEquals(3.0, state.getAdditionalState("p2")[2], 1.0e-15);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream    oos = new ObjectOutputStream(bos);
        oos.writeObject(state);

        Assert.assertTrue(bos.size() > 700);
        Assert.assertTrue(bos.size() < 800);

        ByteArrayInputStream  bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream     ois = new ObjectInputStream(bis);
        SpacecraftState deserialized  = (SpacecraftState) ois.readObject();
        Assert.assertEquals(0.0,
                            Vector3D.distance(state.getPVCoordinates().getPosition(),
                                              deserialized.getPVCoordinates().getPosition()),
                            1.0e-10);
        Assert.assertEquals(0.0,
                            Vector3D.distance(state.getPVCoordinates().getVelocity(),
                                              deserialized.getPVCoordinates().getVelocity()),
                            1.0e-10);
        Assert.assertEquals(0.0,
                            Rotation.distance(state.getAttitude().getRotation(),
                                              deserialized.getAttitude().getRotation()),
                            1.0e-10);
        Assert.assertEquals(0.0,
                            Vector3D.distance(state.getAttitude().getSpin(),
                                              deserialized.getAttitude().getSpin()),
                            1.0e-10);
        Assert.assertEquals(state.getDate(), deserialized.getDate());
        Assert.assertEquals(state.getMu(), deserialized.getMu(), 1.0e-10);
        Assert.assertEquals(state.getFrame().getName(), deserialized.getFrame().getName());
        Assert.assertEquals(2, deserialized.getAdditionalStates().size());
        Assert.assertEquals(1, deserialized.getAdditionalState("p1").length);
        Assert.assertEquals(12.25, deserialized.getAdditionalState("p1")[0], 1.0e-15);
        Assert.assertEquals(3, deserialized.getAdditionalState("p2").length);
        Assert.assertEquals(1.0, deserialized.getAdditionalState("p2")[0], 1.0e-15);
        Assert.assertEquals(2.0, deserialized.getAdditionalState("p2")[1], 1.0e-15);
        Assert.assertEquals(3.0, deserialized.getAdditionalState("p2")[2], 1.0e-15);

    }

    @Before
    public void setUp() {
        try {
        Utils.setDataRoot("regular-data");
        double mu  = 3.9860047e14;
        double ae  = 6.378137e6;
        double c20 = -1.08263e-3;
        double c30 = 2.54e-6;
        double c40 = 1.62e-6;
        double c50 = 2.3e-7;
        double c60 = -5.5e-7;

        mass = 2500;
        double a = 7187990.1979844316;
        double e = 0.5e-4;
        double i = 1.7105407051081795;
        double omega = 1.9674147913622104;
        double OMEGA = FastMath.toRadians(261);
        double lv = 0;

        AbsoluteDate date = new AbsoluteDate(new DateComponents(2004, 01, 01),
                                                 TimeComponents.H00,
                                                 TimeScalesFactory.getUTC());
        orbit = new KeplerianOrbit(a, e, i, omega, OMEGA, lv, PositionAngle.TRUE,
                                   FramesFactory.getEME2000(), date, mu);
        OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                      Constants.WGS84_EARTH_FLATTENING,
                                                      FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        attitudeLaw = new BodyCenterPointing(orbit.getFrame(), earth);
        propagator =
            new EcksteinHechlerPropagator(orbit, attitudeLaw, mass,
                                          ae, mu, c20, c30, c40, c50, c60);

        } catch (OrekitException oe) {
            Assert.fail(oe.getLocalizedMessage());
        }
    }

    @After
    public void tearDown() {
        mass  = Double.NaN;
        orbit = null;
        attitudeLaw = null;
        propagator = null;
    }

    private double mass;
    private Orbit orbit;
    private AttitudeProvider attitudeLaw;
    private Propagator propagator;

}
