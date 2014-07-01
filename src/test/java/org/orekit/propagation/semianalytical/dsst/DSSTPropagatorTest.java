/* Copyright 2002-2014 CS Systèmes d'Information
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
package org.orekit.propagation.semianalytical.dsst;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.apache.commons.math3.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import org.apache.commons.math3.ode.nonstiff.DormandPrince853Integrator;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.MathUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.attitudes.LofOffset;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.forces.drag.Atmosphere;
import org.orekit.forces.drag.HarrisPriester;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.ICGEMFormatReader;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.forces.maneuvers.ImpulseManeuver;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.NodeDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.EventHandler.Action;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTAtmosphericDrag;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTCentralBody;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTSolarRadiationPressure;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTThirdBody;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;

public class DSSTPropagatorTest {

    private DSSTPropagator dsstProp;

    @Test
    public void testNoExtrapolation() throws OrekitException {
        SpacecraftState state = getLEOrbit();
        setDSSTProp(state);

        // Propagation of the initial state at the initial date
        final SpacecraftState finalState = dsstProp.propagate(state.getDate());

        // Initial orbit definition
        final Vector3D initialPosition = state.getPVCoordinates().getPosition();
        final Vector3D initialVelocity = state.getPVCoordinates().getVelocity();

        // Final orbit definition
        final Vector3D finalPosition = finalState.getPVCoordinates().getPosition();
        final Vector3D finalVelocity = finalState.getPVCoordinates().getVelocity();

        // Check results
        Assert.assertEquals(initialPosition.getX(), finalPosition.getX(), 0.0);
        Assert.assertEquals(initialPosition.getY(), finalPosition.getY(), 0.0);
        Assert.assertEquals(initialPosition.getZ(), finalPosition.getZ(), 0.0);
        Assert.assertEquals(initialVelocity.getX(), finalVelocity.getX(), 0.0);
        Assert.assertEquals(initialVelocity.getY(), finalVelocity.getY(), 0.0);
        Assert.assertEquals(initialVelocity.getZ(), finalVelocity.getZ(), 0.0);
    }

    @Test
    public void testKepler() throws OrekitException {
        SpacecraftState state = getLEOrbit();
        setDSSTProp(state);

        // Propagation of the initial state at t + dt
        final double dt = 3200.;
        final SpacecraftState finalState = dsstProp.propagate(state.getDate().shiftedBy(dt));

        // Check results
        final double n = FastMath.sqrt(state.getMu() / state.getA()) / state.getA();
        Assert.assertEquals(state.getA(), finalState.getA(), 0.);
        Assert.assertEquals(state.getEquinoctialEx(), finalState.getEquinoctialEx(), 0.);
        Assert.assertEquals(state.getEquinoctialEy(), finalState.getEquinoctialEy(), 0.);
        Assert.assertEquals(state.getHx(), finalState.getHx(), 0.);
        Assert.assertEquals(state.getHy(), finalState.getHy(), 0.);
        Assert.assertEquals(state.getLM() + n * dt, finalState.getLM(), 1.e-14);

    }

    @Test
    public void testEphemeris() throws OrekitException {
        SpacecraftState state = getGEOrbit();
        setDSSTProp(state);

        // Set ephemeris mode
        dsstProp.setEphemerisMode();

        // Propagation of the initial state at t + 10 days
        final double dt = 2. * Constants.JULIAN_DAY;
        dsstProp.propagate(state.getDate().shiftedBy(5. * dt));

        // Get ephemeris
        BoundedPropagator ephem = dsstProp.getGeneratedEphemeris();

        // Propagation of the initial state with ephemeris at t + 2 days
        final SpacecraftState s = ephem.propagate(state.getDate().shiftedBy(dt));

        // Check results
        final double n = FastMath.sqrt(state.getMu() / state.getA()) / state.getA();
        Assert.assertEquals(state.getA(), s.getA(), 0.);
        Assert.assertEquals(state.getEquinoctialEx(), s.getEquinoctialEx(), 0.);
        Assert.assertEquals(state.getEquinoctialEy(), s.getEquinoctialEy(), 0.);
        Assert.assertEquals(state.getHx(), s.getHx(), 0.);
        Assert.assertEquals(state.getHy(), s.getHy(), 0.);
        Assert.assertEquals(state.getLM() + n * dt, s.getLM(), 1.5e-14);

    }

    @Test
    public void testImpulseManeuver() throws OrekitException {
        final Orbit initialOrbit = new KeplerianOrbit(24532000.0, 0.72, 0.3, FastMath.PI, 0.4, 2.0, PositionAngle.MEAN, FramesFactory.getEME2000(), new AbsoluteDate(new DateComponents(2008, 06, 23), new TimeComponents(14, 18, 37), TimeScalesFactory.getUTC()), 3.986004415e14);
        final double a = initialOrbit.getA();
        final double e = initialOrbit.getE();
        final double i = initialOrbit.getI();
        final double mu = initialOrbit.getMu();
        final double vApo = FastMath.sqrt(mu * (1 - e) / (a * (1 + e)));
        double dv = 0.99 * FastMath.tan(i) * vApo;

        // Set propagator with state
        setDSSTProp(new SpacecraftState(initialOrbit));

        // Add impulse maneuver
        dsstProp.setAttitudeProvider(new LofOffset(initialOrbit.getFrame(), LOFType.VVLH));
        dsstProp.addEventDetector(new ImpulseManeuver<NodeDetector>(new NodeDetector(initialOrbit, FramesFactory.getEME2000()), new Vector3D(dv, Vector3D.PLUS_J), 400.0));
        SpacecraftState propagated = dsstProp.propagate(initialOrbit.getDate().shiftedBy(8000));

        Assert.assertEquals(0.0028257, propagated.getI(), 1.0e-6);
    }

    @Test
    public void testPropagationWithCentralBody() throws Exception {

        // Central Body geopotential 4x4
        final UnnormalizedSphericalHarmonicsProvider provider =
                GravityFieldFactory.getUnnormalizedProvider(4, 4);
        final Frame earthFrame = CelestialBodyFactory.getEarth().getBodyOrientedFrame();
        DSSTForceModel force = new DSSTCentralBody(earthFrame,
                                                   Constants.WGS84_EARTH_ANGULAR_VELOCITY, provider);

        // GPS Orbit
        final AbsoluteDate initDate = new AbsoluteDate(2007, 4, 16, 0, 46, 42.400,
                                                       TimeScalesFactory.getUTC());
        final Orbit orbit = new KeplerianOrbit(26559890.,
                                               0.0041632,
                                               FastMath.toRadians(55.2),
                                               FastMath.toRadians(315.4985),
                                               FastMath.toRadians(130.7562),
                                               FastMath.toRadians(44.2377),
                                               PositionAngle.MEAN,
                                               FramesFactory.getEME2000(),
                                               initDate,
                                               provider.getMu());

        // Set propagator with state and force model
        setDSSTProp(new SpacecraftState(orbit));
        dsstProp.addForceModel(force);

        // 5 days propagation
        final SpacecraftState state = dsstProp.propagate(initDate.shiftedBy(5. * 86400.));

        // Ref GTDS_DSST:
        // a    = 26559.92081 km
        // h/ey =   0.2731622444E-03
        // k/ex =   0.4164167597E-02
        // p/hy =  -0.3399607878
        // q/hx =   0.3971568634
        // lM   = 140.6375352°
        Assert.assertEquals(26559920.81, state.getA(), 1.e-1);
        Assert.assertEquals(0.2731622444E-03, state.getEquinoctialEx(), 2.e-8);
        Assert.assertEquals(0.4164167597E-02, state.getEquinoctialEy(), 2.e-8);
        Assert.assertEquals(-0.3399607878, state.getHx(), 5.e-8);
        Assert.assertEquals(0.3971568634, state.getHy(), 2.e-6);
        Assert.assertEquals(140.6375352,
                            FastMath.toDegrees(MathUtils.normalizeAngle(state.getLM(), FastMath.PI)),
                            5.e-3);
    }

    @Test
    public void testPropagationWithThirdBody() throws OrekitException, IOException {

        // Central Body geopotential 2x0
        final UnnormalizedSphericalHarmonicsProvider provider =
                GravityFieldFactory.getUnnormalizedProvider(2, 0);
        final Frame earthFrame = CelestialBodyFactory.getEarth().getBodyOrientedFrame();
        DSSTForceModel centralBody = new DSSTCentralBody(earthFrame,
                                                         Constants.WGS84_EARTH_ANGULAR_VELOCITY,
                                                         provider);

        // Third Bodies Force Model (Moon + Sun) */
        DSSTForceModel moon = new DSSTThirdBody(CelestialBodyFactory.getMoon());
        DSSTForceModel sun  = new DSSTThirdBody(CelestialBodyFactory.getSun());

        // SIRIUS Orbit
        final AbsoluteDate initDate = new AbsoluteDate(2003, 7, 1, 0, 0, 00.000,
                                                       TimeScalesFactory.getUTC());
        final Orbit orbit = new KeplerianOrbit(42163393.,
                                               0.2684,
                                               FastMath.toRadians(63.435),
                                               FastMath.toRadians(270.0),
                                               FastMath.toRadians(285.0),
                                               FastMath.toRadians(344.0),
                                               PositionAngle.MEAN,
                                               FramesFactory.getEME2000(),
                                               initDate,
                                               provider.getMu());

        // Set propagator with state and force model
        setDSSTProp(new SpacecraftState(orbit));
        dsstProp.addForceModel(centralBody);
        dsstProp.addForceModel(moon);
        dsstProp.addForceModel(sun);

        // 5 days propagation
        final SpacecraftState state = dsstProp.propagate(initDate.shiftedBy(5. * 86400.));

        // Ref Standalone_DSST:
        // a    = 42163393.0 m
        // h/ey =  -0.06893353670734315
        // k/ex =  -0.2592789733084587
        // p/hy =  -0.5968524904937771
        // q/hx =   0.1595005111738418
        // lM   = 183°9386620425922
        Assert.assertEquals(42163393.0, state.getA(), 1.e-1);
        Assert.assertEquals(-0.2592789733084587, state.getEquinoctialEx(), 5.e-7);
        Assert.assertEquals(-0.06893353670734315, state.getEquinoctialEy(), 2.e-7);
        Assert.assertEquals( 0.1595005111738418, state.getHx(), 2.e-7);
        Assert.assertEquals(-0.5968524904937771, state.getHy(), 5.e-8);
        Assert.assertEquals(183.9386620425922,
                            FastMath.toDegrees(MathUtils.normalizeAngle(state.getLM(), FastMath.PI)),
                            3.e-2);
    }

    @Test
    public void testPropagationWithDrag() throws OrekitException {

        // Central Body geopotential 2x0
        final UnnormalizedSphericalHarmonicsProvider provider =
                GravityFieldFactory.getUnnormalizedProvider(2, 0);
        final Frame earthFrame = CelestialBodyFactory.getEarth().getBodyOrientedFrame();
        DSSTForceModel centralBody = new DSSTCentralBody(earthFrame,
                                                         Constants.WGS84_EARTH_ANGULAR_VELOCITY,
                                                         provider);

        // Drag Force Model
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(provider.getAe(),
                                                            Constants.WGS84_EARTH_FLATTENING,
                                                            earthFrame);
        final Atmosphere atm = new HarrisPriester(CelestialBodyFactory.getSun(), earth, 6);

        final double cd = 2.0;
        final double area = 25.0;
        DSSTForceModel drag = new DSSTAtmosphericDrag(atm, cd, area);

        // LEO Orbit
        final AbsoluteDate initDate = new AbsoluteDate(2003, 7, 1, 0, 0, 00.000,
                                                       TimeScalesFactory.getUTC());
        final Orbit orbit = new KeplerianOrbit(7204535.848109440,
                                               0.0012402238462686,
                                               FastMath.toRadians(98.74341600466740),
                                               FastMath.toRadians(111.1990175076630),
                                               FastMath.toRadians(43.32990110790340),
                                               FastMath.toRadians(68.66852509725620),
                                               PositionAngle.MEAN,
                                               FramesFactory.getEME2000(),
                                               initDate,
                                               provider.getMu());

        // Set propagator with state and force model
        setDSSTProp(new SpacecraftState(orbit));
        dsstProp.addForceModel(centralBody);
        dsstProp.addForceModel(drag);

        // 5 days propagation
        final SpacecraftState state = dsstProp.propagate(initDate.shiftedBy(5. * 86400.));

        // Ref Standalone_DSST:
        // a    = 7204521.657141485 m
        // h/ey =  0.0007093755541595772
        // k/ex = -0.001016800430994036
        // p/hy =  0.8698955648709271
        // q/hx =  0.7757573478894775
        // lM   = 193°0939742953394
        Assert.assertEquals(7204521.657141485, state.getA(), 6.e-1);
        Assert.assertEquals(-0.001016800430994036, state.getEquinoctialEx(), 5.e-8);
        Assert.assertEquals(0.0007093755541595772, state.getEquinoctialEy(), 2.e-8);
        Assert.assertEquals(0.7757573478894775, state.getHx(), 5.e-8);
        Assert.assertEquals(0.8698955648709271, state.getHy(), 5.e-8);
        Assert.assertEquals(193.0939742953394,
                            FastMath.toDegrees(MathUtils.normalizeAngle(state.getLM(), FastMath.PI)),
                            2.e-3);
        //Assert.assertEquals(((DSSTAtmosphericDrag)drag).getCd(), cd, 1e-9);
        //Assert.assertEquals(((DSSTAtmosphericDrag)drag).getArea(), area, 1e-9);
        Assert.assertEquals(((DSSTAtmosphericDrag)drag).getAtmosphere(), atm);

        final double atmosphericMaxConstant = 1000000.0; //DSSTAtmosphericDrag.ATMOSPHERE_ALTITUDE_MAX
        Assert.assertEquals(((DSSTAtmosphericDrag)drag).getRbar(), atmosphericMaxConstant + Constants.WGS84_EARTH_EQUATORIAL_RADIUS,1e-9);
    }

    @Test
    public void testPropagationWithSolarRadiationPressure() throws OrekitException {

        // Central Body geopotential 2x0
        final UnnormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getUnnormalizedProvider(2, 0);
        DSSTForceModel centralBody = new DSSTCentralBody(CelestialBodyFactory.getEarth().getBodyOrientedFrame(),
                                                         Constants.WGS84_EARTH_ANGULAR_VELOCITY, provider);

        // SRP Force Model
        DSSTForceModel srp = new DSSTSolarRadiationPressure(1.2, 100., CelestialBodyFactory.getSun(),
                                                            Constants.WGS84_EARTH_EQUATORIAL_RADIUS);

        // GEO Orbit
        final AbsoluteDate initDate = new AbsoluteDate(2003, 9, 16, 0, 0, 00.000,
                                                       TimeScalesFactory.getUTC());
        final Orbit orbit = new KeplerianOrbit(42166258.,
                                               0.0001,
                                               FastMath.toRadians(0.001),
                                               FastMath.toRadians(315.4985),
                                               FastMath.toRadians(130.7562),
                                               FastMath.toRadians(44.2377),
                                               PositionAngle.MEAN,
                                               FramesFactory.getGCRF(),
                                               initDate,
                                               provider.getMu());

        // Set propagator with state and force model
        dsstProp = new DSSTPropagator(new ClassicalRungeKuttaIntegrator(86400.));
        dsstProp.setInitialState(new SpacecraftState(orbit), false);
        dsstProp.addForceModel(centralBody);
        dsstProp.addForceModel(srp);

        // 10 days propagation
        final SpacecraftState state = dsstProp.propagate(initDate.shiftedBy(10. * 86400.));

        // Ref Standalone_DSST:
        // a    = 42166257.99807995 m
        // h/ey = -0.1191876027555493D-03
        // k/ex = -0.1781865038201885D-05
        // p/hy =  0.6618387121369373D-05
        // q/hx = -0.5624363171289686D-05
        // lM   = 140°3496229467104
        Assert.assertEquals(42166257.99807995, state.getA(), 8.e-5);
        Assert.assertEquals(-0.1781865038201885e-05, state.getEquinoctialEx(), 3.e-7);
        Assert.assertEquals(-0.1191876027555493e-03, state.getEquinoctialEy(), 4.e-6);
        Assert.assertEquals(-0.5624363171289686e-05, state.getHx(), 4.e-11);
        Assert.assertEquals( 0.6618387121369373e-05, state.getHy(), 2.e-10);
        Assert.assertEquals(140.3496229467104,
                            FastMath.toDegrees(MathUtils.normalizeAngle(state.getLM(), FastMath.PI)),
                            9.e-7);
    }

    @Test
    public void testStopEvent() throws OrekitException {
        SpacecraftState state = getLEOrbit();
        setDSSTProp(state);

        final AbsoluteDate stopDate = state.getDate().shiftedBy(1000);
        CheckingHandler<DateDetector> checking = new CheckingHandler<DateDetector>(Action.STOP);
        dsstProp.addEventDetector(new DateDetector(stopDate).withHandler(checking));
        checking.assertEvent(false);
        final SpacecraftState finalState = dsstProp.propagate(state.getDate().shiftedBy(3200));
        checking.assertEvent(true);
        Assert.assertEquals(0, finalState.getDate().durationFrom(stopDate), 1.0e-10);
    }

    @Test
    public void testContinueEvent() throws OrekitException {
        SpacecraftState state = getLEOrbit();
        setDSSTProp(state);

        final AbsoluteDate resetDate = state.getDate().shiftedBy(1000);
        CheckingHandler<DateDetector> checking = new CheckingHandler<DateDetector>(Action.CONTINUE);
        dsstProp.addEventDetector(new DateDetector(resetDate).withHandler(checking));
        final double dt = 3200;
        checking.assertEvent(false);
        final SpacecraftState finalState = dsstProp.propagate(state.getDate().shiftedBy(dt));
        checking.assertEvent(true);
        final double n = FastMath.sqrt(state.getMu() / state.getA()) / state.getA();
        Assert.assertEquals(state.getA(), finalState.getA(), 1.0e-10);
        Assert.assertEquals(state.getEquinoctialEx(), finalState.getEquinoctialEx(), 1.0e-10);
        Assert.assertEquals(state.getEquinoctialEy(), finalState.getEquinoctialEy(), 1.0e-10);
        Assert.assertEquals(state.getHx(), finalState.getHx(), 1.0e-10);
        Assert.assertEquals(state.getHy(), finalState.getHy(), 1.0e-10);
        Assert.assertEquals(state.getLM() + n * dt, finalState.getLM(), 6.0e-10);
    }

    @Test
    public void testIssue157() throws OrekitException {
        Utils.setDataRoot("regular-data:potential/icgem-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("^eigen-6s-truncated$", false));
        UnnormalizedSphericalHarmonicsProvider nshp = GravityFieldFactory.getUnnormalizedProvider(8, 8);
        Orbit orbit = new KeplerianOrbit(13378000, 0.05, 0, 0, FastMath.PI, 0, PositionAngle.MEAN,
                                         FramesFactory.getTOD(false),
                                         new AbsoluteDate(2003, 5, 6, TimeScalesFactory.getUTC()),
                                         nshp.getMu());
        double period = orbit.getKeplerianPeriod();
        double[][] tolerance = DSSTPropagator.tolerances(1.0, orbit);
        AdaptiveStepsizeIntegrator integrator =
                new DormandPrince853Integrator(period / 100, period * 100, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(10 * period);
        DSSTPropagator propagator = new DSSTPropagator(integrator, true);
        OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                      Constants.WGS84_EARTH_FLATTENING,
                                                      FramesFactory.getGTOD(false));
        CelestialBody sun = CelestialBodyFactory.getSun();
        CelestialBody moon = CelestialBodyFactory.getMoon();
        propagator.addForceModel(new DSSTCentralBody(earth.getBodyFrame(), Constants.WGS84_EARTH_ANGULAR_VELOCITY, nshp));
        propagator.addForceModel(new DSSTThirdBody(sun));
        propagator.addForceModel(new DSSTThirdBody(moon));
        propagator.addForceModel(new DSSTAtmosphericDrag(new HarrisPriester(sun, earth), 2.1, 180));
        propagator.addForceModel(new DSSTSolarRadiationPressure(1.2, 180, sun, earth.getEquatorialRadius()));

        propagator.resetInitialState(new SpacecraftState(orbit, 45.0));
        final SpacecraftState finalState = propagator.propagate(orbit.getDate().shiftedBy(30 * Constants.JULIAN_DAY));
        Assert.assertEquals(8758.8, orbit.getA() - finalState.getA(), 10.0);

    }

    @Test
    public void testDSSTrestart() throws OrekitException {

        DSSTPropagator dsstProp;


        // build force model geopotential 8x8
        Utils.setDataRoot("regular-data:potential/icgem-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("^eigen-6s-truncated$", false));
        final UnnormalizedSphericalHarmonicsProvider gravityProvider = GravityFieldFactory.getUnnormalizedProvider(8, 8);
        final Frame rotatingFrame = CelestialBodyFactory.getEarth().getBodyOrientedFrame();
        DSSTForceModel gravityForceModel = new DSSTCentralBody(rotatingFrame, Constants.WGS84_EARTH_ANGULAR_VELOCITY, gravityProvider);

        // build initial state
        final AbsoluteDate epochDate = new AbsoluteDate(2014, 01, 01, 0, 0, 0, TimeScalesFactory.getUTC());
        final KeplerianOrbit initialOrbit = new KeplerianOrbit( 26562000.0, 0.72, FastMath.toRadians(63.435),
                FastMath.toRadians(270.0), 0.0, 0.0, PositionAngle.MEAN, FramesFactory.getEME2000(), epochDate, gravityProvider.getMu());
        final SpacecraftState initialState = new SpacecraftState(new EquinoctialOrbit(initialOrbit));

        // build integrator
        final double minStep = initialState.getKeplerianPeriod() * 0.1;
        final double maxStep = initialState.getKeplerianPeriod() * 10.0;
        final double[][] tol = DSSTPropagator.tolerances(0.1, initialState.getOrbit());
        AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(minStep, maxStep, tol[0], tol[1]);
        dsstProp = new DSSTPropagator(integrator);

        // add force model
        dsstProp.addForceModel(gravityForceModel);

        // DSST Propagation (first propagation without timing, for warm-up purposes)
        dsstProp.setInitialState(initialState, false);
        dsstProp.propagate(epochDate.shiftedBy(100.0 * 86400.0));
        double refExecTime = 0;

        for (int i = 0; i < 5; i++) {
            dsstProp.setInitialState(initialState, false);
            long propStart = System.currentTimeMillis();
            dsstProp.propagate(epochDate.shiftedBy(100.0 * 86400.0));
            long propEnd = System.currentTimeMillis();
            double execTime = 0.001 * (propEnd - propStart);

            if (refExecTime <= 0) {
                refExecTime = execTime;
            } else {
                Assert.assertTrue(execTime <= refExecTime * 1.1);
            }
        }
    }

    @Test
    public void testGetInitialOsculatingState() throws IllegalArgumentException, OrekitException {
        final SpacecraftState initialState = getGEOrbit();

        // build integrator
        final double minStep = initialState.getKeplerianPeriod() * 0.1;
        final double maxStep = initialState.getKeplerianPeriod() * 10.0;
        final double[][] tol = DSSTPropagator.tolerances(0.1, initialState.getOrbit());
        AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(minStep, maxStep, tol[0], tol[1]);

        DSSTPropagator prop = new DSSTPropagator(integrator, false);

        final UnnormalizedSphericalHarmonicsProvider provider =
                GravityFieldFactory.getUnnormalizedProvider(4, 0);
        final Frame earthFrame = CelestialBodyFactory.getEarth().getBodyOrientedFrame();
        DSSTForceModel force = new DSSTCentralBody(earthFrame,
                                                   Constants.WGS84_EARTH_ANGULAR_VELOCITY, provider);
        prop.addForceModel(force);

        prop.setInitialState(initialState, false);
        prop.getInitialState();
    }

    @Test
    public void testMeanToOsculatingState() throws IllegalArgumentException, OrekitException {
        final SpacecraftState leoMeanState = getLEOrbit();
        final SpacecraftState geoMeanState = getGEOrbit();

        final UnnormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getUnnormalizedProvider(2, 0);
        final Frame earthFrame = CelestialBodyFactory.getEarth().getBodyOrientedFrame();

        final DSSTForceModel force = new DSSTCentralBody(earthFrame,
                Constants.WGS84_EARTH_ANGULAR_VELOCITY, provider);

        final Collection<DSSTForceModel> forces = new ArrayList<DSSTForceModel>();
        forces.add(force);

        DSSTPropagator.computeOsculatingState(leoMeanState, forces);
        DSSTPropagator.computeOsculatingState(geoMeanState, forces);
    }

    @Test
    public void testOsculatingToMeanState() throws IllegalArgumentException, OrekitException {
        final SpacecraftState leoMeanState = getLEOrbit();

        final UnnormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getUnnormalizedProvider(2, 0);
        final Frame earthFrame = CelestialBodyFactory.getEarth().getBodyOrientedFrame();

        final DSSTForceModel force = new DSSTCentralBody(earthFrame,
                Constants.WGS84_EARTH_ANGULAR_VELOCITY, provider);

        final Collection<DSSTForceModel> forces = new ArrayList<DSSTForceModel>();
        forces.add(force);

        final EquinoctialOrbit osculatingLEOOrbit = (EquinoctialOrbit) DSSTPropagator.
                computeOsculatingState(leoMeanState, forces).getOrbit();
        final SpacecraftState leoOsculatingState = new SpacecraftState(osculatingLEOOrbit);

        final SpacecraftState leoComputedMeanState = DSSTPropagator.computeMeanState(leoOsculatingState, forces);

        Assert.assertEquals(leoMeanState.getA(), leoComputedMeanState.getA(), 1.);
    }

    private SpacecraftState getGEOrbit() throws IllegalArgumentException, OrekitException {
        // No shadow at this date
        final AbsoluteDate initDate = new AbsoluteDate(new DateComponents(2003, 05, 21), new TimeComponents(1, 0, 0.),
                                                       TimeScalesFactory.getUTC());
        final Orbit orbit = new EquinoctialOrbit(42164000,
                                                 10e-3,
                                                 10e-3,
                                                 FastMath.tan(0.001745329) * FastMath.cos(2 * FastMath.PI / 3),
                                                 FastMath.tan(0.001745329) * FastMath.sin(2 * FastMath.PI / 3), 0.1,
                                                 PositionAngle.TRUE,
                                                 FramesFactory.getEME2000(),
                                                 initDate,
                                                 3.986004415E14);
        return new SpacecraftState(orbit);
    }

    private SpacecraftState getLEOrbit() throws IllegalArgumentException, OrekitException {
        final Vector3D position = new Vector3D(-6142438.668, 3492467.560, -25767.25680);
        final Vector3D velocity = new Vector3D(505.8479685, 942.7809215, 7435.922231);
        // Spring equinoxe 21st mars 2003 1h00m
        final AbsoluteDate initDate = new AbsoluteDate(new DateComponents(2003, 03, 21), new TimeComponents(1, 0, 0.), TimeScalesFactory.getUTC());
        return new SpacecraftState(new EquinoctialOrbit(new PVCoordinates(position, velocity),
                                                        FramesFactory.getEME2000(),
                                                        initDate,
                                                        3.986004415E14));
    }

    private void setDSSTProp(SpacecraftState initialState) throws OrekitException {
        initialState.getDate();
        final double minStep = initialState.getKeplerianPeriod();
        final double maxStep = 100. * minStep;
        final double[][] tol = DSSTPropagator.tolerances(1.0, initialState.getOrbit());
        AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(minStep, maxStep, tol[0], tol[1]);
        dsstProp = new DSSTPropagator(integrator);
        dsstProp.setInitialState(initialState, false);

    }

    private static class CheckingHandler<T extends EventDetector> implements EventHandler<T> {

        private final Action actionOnEvent;
        private boolean gotHere;

        public CheckingHandler(final Action actionOnEvent) {
            this.actionOnEvent = actionOnEvent;
            this.gotHere       = false;
        }

        public void assertEvent(boolean expected) {
            Assert.assertEquals(expected, gotHere);
        }

        @Override
        public Action eventOccurred(SpacecraftState s, T detector, boolean increasing) {
            gotHere = true;
            return actionOnEvent;
        }

        @Override
        public SpacecraftState resetState(T detector, SpacecraftState oldState)
            throws OrekitException {
            return oldState;
        }

    }

    @Before
    public void setUp() throws OrekitException, IOException, ParseException {
        Utils.setDataRoot("regular-data:potential/shm-format");
    }

    @After
    public void tearDown() {
        dsstProp = null;
    }

}
