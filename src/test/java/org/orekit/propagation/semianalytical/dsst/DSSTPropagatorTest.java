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
package org.orekit.propagation.semianalytical.dsst;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.hamcrest.MatcherAssert;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.OrekitMatchers;
import org.orekit.Utils;
import org.orekit.attitudes.LofOffset;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.forces.drag.atmosphere.Atmosphere;
import org.orekit.forces.drag.atmosphere.HarrisPriester;
import org.orekit.forces.gravity.potential.GRGSFormatReader;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.ICGEMFormatReader;
import org.orekit.forces.gravity.potential.TideSystem;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.forces.maneuvers.ImpulseManeuver;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.NodeDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.EventHandler.Action;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTAtmosphericDrag;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTSolarRadiationPressure;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTTesseral;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTThirdBody;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTZonal;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

public class DSSTPropagatorTest {

    private DSSTPropagator dsstProp;

    @Test
    public void testHighDegreesSetting() throws OrekitException {

        Utils.setDataRoot("regular-data:potential/grgs-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));
        int earthDegree = 36;
        int earthOrder  = 36;
        int eccPower    = 4;
        final UnnormalizedSphericalHarmonicsProvider provider =
                GravityFieldFactory.getUnnormalizedProvider(earthDegree, earthOrder);
        final org.orekit.frames.Frame earthFrame =
                FramesFactory.getITRF(IERSConventions.IERS_2010, true); // terrestrial frame
        final DSSTForceModel force =
                new DSSTTesseral(earthFrame,Constants.WGS84_EARTH_ANGULAR_VELOCITY, provider,
                                 earthDegree, earthOrder, eccPower, earthDegree + eccPower,
                                 earthDegree, earthOrder, eccPower);
        final Collection<DSSTForceModel> forces = new ArrayList<DSSTForceModel>();
        forces.add(force);
        TimeScale tai = TimeScalesFactory.getTAI();
        AbsoluteDate initialDate = new AbsoluteDate("2015-07-01", tai);
        Frame eci = FramesFactory.getGCRF();
        KeplerianOrbit orbit = new KeplerianOrbit(
                7120000.0, 1.0e-3, FastMath.toRadians(60.0),
                FastMath.toRadians(120.0), FastMath.toRadians(47.0),
                FastMath.toRadians(12.0),
                PositionAngle.TRUE, eci, initialDate, Constants.EIGEN5C_EARTH_MU);
        SpacecraftState oscuState = DSSTPropagator.computeOsculatingState(new SpacecraftState(orbit), forces);
        Assert.assertEquals(7119927.097122, oscuState.getA(), 0.001);
    }

    @Test
    public void testEphemerisDates() throws OrekitException {
        //setup
        TimeScale tai = TimeScalesFactory.getTAI();
        AbsoluteDate initialDate = new AbsoluteDate("2015-07-01", tai);
        AbsoluteDate startDate = new AbsoluteDate("2015-07-03", tai).shiftedBy(-0.1);
        AbsoluteDate endDate = new AbsoluteDate("2015-07-04", tai);
        Frame eci = FramesFactory.getGCRF();
        KeplerianOrbit orbit = new KeplerianOrbit(
                600e3 + Constants.WGS84_EARTH_EQUATORIAL_RADIUS, 0, 0, 0, 0, 0,
                PositionAngle.TRUE, eci, initialDate, Constants.EIGEN5C_EARTH_MU);
        double[][] tol = DSSTPropagator
                .tolerances(1, orbit);
        Propagator prop = new DSSTPropagator(
                new DormandPrince853Integrator(0.1, 500, tol[0], tol[1]));
        prop.resetInitialState(new SpacecraftState(new CartesianOrbit(orbit)));

        //action
        prop.setEphemerisMode();
        prop.propagate(startDate, endDate);
        BoundedPropagator ephemeris = prop.getGeneratedEphemeris();

        //verify
        TimeStampedPVCoordinates actualPV = ephemeris.getPVCoordinates(startDate, eci);
        TimeStampedPVCoordinates expectedPV = orbit.getPVCoordinates(startDate, eci);
        MatcherAssert.assertThat(actualPV.getPosition(),
                OrekitMatchers.vectorCloseTo(expectedPV.getPosition(), 1.0));
        MatcherAssert.assertThat(actualPV.getVelocity(),
                OrekitMatchers.vectorCloseTo(expectedPV.getVelocity(), 1.0));
        MatcherAssert.assertThat(ephemeris.getMinDate().durationFrom(startDate),
                OrekitMatchers.closeTo(0, 0));
        MatcherAssert.assertThat(ephemeris.getMaxDate().durationFrom(endDate),
                OrekitMatchers.closeTo(0, 0));
        //test date
        AbsoluteDate date = endDate.shiftedBy(-0.11);
        Assert.assertEquals(
                ephemeris.propagate(date).getDate().durationFrom(date), 0, 0);
    }

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
        dsstProp.addForceModel(new DSSTZonal(provider, 4, 3, 9));
        dsstProp.addForceModel(new DSSTTesseral(earthFrame,
                                                Constants.WGS84_EARTH_ANGULAR_VELOCITY, provider,
                                                4, 4, 4, 8, 4, 4, 2));

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
        DSSTForceModel zonal    = new DSSTZonal(provider, 2, 1, 5);
        DSSTForceModel tesseral = new DSSTTesseral(earthFrame,
                                                   Constants.WGS84_EARTH_ANGULAR_VELOCITY,
                                                   provider, 2, 0, 0, 2, 2, 0, 0);

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
        dsstProp.addForceModel(zonal);
        dsstProp.addForceModel(tesseral);
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

    @Test(expected=OrekitException.class)
    public void testTooSmallMaxDegree() throws OrekitException {
        new DSSTZonal(GravityFieldFactory.getUnnormalizedProvider(2, 0), 1, 0, 3);
    }

    @Test(expected=OrekitException.class)
    public void testTooLargeMaxDegree() throws OrekitException {
        new DSSTZonal(GravityFieldFactory.getUnnormalizedProvider(2, 0), 8, 0, 8);
    }

    @Test(expected=OrekitException.class)
    public void testWrongMaxPower() throws OrekitException {
        new DSSTZonal(GravityFieldFactory.getUnnormalizedProvider(8, 8), 4, 4, 4);
    }

    @Test
    public void testPropagationWithDrag() throws OrekitException {

        // Central Body geopotential 2x0
        final UnnormalizedSphericalHarmonicsProvider provider =
                GravityFieldFactory.getUnnormalizedProvider(2, 0);
        final Frame earthFrame = CelestialBodyFactory.getEarth().getBodyOrientedFrame();
        DSSTForceModel zonal    = new DSSTZonal(provider, 2, 0, 5);
        DSSTForceModel tesseral = new DSSTTesseral(earthFrame,
                                                   Constants.WGS84_EARTH_ANGULAR_VELOCITY,
                                                   provider, 2, 0, 0, 2, 2, 0, 0);

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
        dsstProp.addForceModel(zonal);
        dsstProp.addForceModel(tesseral);
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
        DSSTForceModel zonal    = new DSSTZonal(provider, 2, 1, 5);
        DSSTForceModel tesseral = new DSSTTesseral(CelestialBodyFactory.getEarth().getBodyOrientedFrame(),
                                                   Constants.WGS84_EARTH_ANGULAR_VELOCITY,
                                                   provider, 2, 0, 0, 2, 2, 0, 0);

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
        dsstProp.addForceModel(zonal);
        dsstProp.addForceModel(tesseral);
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
        Assert.assertEquals(42166257.99807995, state.getA(), 0.8);
        Assert.assertEquals(-0.1781865038201885e-05, state.getEquinoctialEx(), 3.e-7);
        Assert.assertEquals(-0.1191876027555493e-03, state.getEquinoctialEy(), 4.e-6);
        Assert.assertEquals(-0.5624363171289686e-05, state.getHx(), 4.e-9);
        Assert.assertEquals( 0.6618387121369373e-05, state.getHy(), 3.e-10);
        Assert.assertEquals(140.3496229467104,
                            FastMath.toDegrees(MathUtils.normalizeAngle(state.getLM(), FastMath.PI)),
                            2.e-4);
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
        propagator.addForceModel(new DSSTZonal(nshp, 8, 7, 17));
        propagator.addForceModel(new DSSTTesseral(earth.getBodyFrame(),
                                                  Constants.WGS84_EARTH_ANGULAR_VELOCITY,
                                                  nshp, 8, 8, 4, 12, 8, 8, 4));
        propagator.addForceModel(new DSSTThirdBody(sun));
        propagator.addForceModel(new DSSTThirdBody(moon));
        propagator.addForceModel(new DSSTAtmosphericDrag(new HarrisPriester(sun, earth), 2.1, 180));
        propagator.addForceModel(new DSSTSolarRadiationPressure(1.2, 180, sun, earth.getEquatorialRadius()));


        propagator.setInitialState(new SpacecraftState(orbit, 45.0), true);
        SpacecraftState finalState = propagator.propagate(orbit.getDate().shiftedBy(30 * Constants.JULIAN_DAY));
        // the following comparison is in fact meaningless
        // the initial orbit is osculating the final orbit is a mean orbit
        // and they are not considered at the same epoch
        // we keep it only as is was an historical test
        Assert.assertEquals(2189.4, orbit.getA() - finalState.getA(), 1.0);

        propagator.setInitialState(new SpacecraftState(orbit, 45.0), false);
        finalState = propagator.propagate(orbit.getDate().shiftedBy(30 * Constants.JULIAN_DAY));
        // the following comparison is realistic
        // both the initial orbit and final orbit are mean orbits
        Assert.assertEquals(1478.05, orbit.getA() - finalState.getA(), 1.0);

    }

    @Test
    public void testEphemerisGeneration() throws OrekitException {
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
        DSSTPropagator propagator = new DSSTPropagator(integrator, false);
        OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                      Constants.WGS84_EARTH_FLATTENING,
                                                      FramesFactory.getGTOD(false));
        CelestialBody sun = CelestialBodyFactory.getSun();
        CelestialBody moon = CelestialBodyFactory.getMoon();
        propagator.addForceModel(new DSSTZonal(nshp, 8, 7, 17));
        propagator.addForceModel(new DSSTTesseral(earth.getBodyFrame(),
                                                  Constants.WGS84_EARTH_ANGULAR_VELOCITY,
                                                  nshp, 8, 8, 4, 12, 8, 8, 4));
        propagator.addForceModel(new DSSTThirdBody(sun));
        propagator.addForceModel(new DSSTThirdBody(moon));
        propagator.addForceModel(new DSSTAtmosphericDrag(new HarrisPriester(sun, earth), 2.1, 180));
        propagator.addForceModel(new DSSTSolarRadiationPressure(1.2, 180, sun, earth.getEquatorialRadius()));
        propagator.setInterpolationGridToMaxTimeGap(0.5 * Constants.JULIAN_DAY);

        // direct generation of states
        propagator.setInitialState(new SpacecraftState(orbit, 45.0), false);
        final List<SpacecraftState> states = new ArrayList<SpacecraftState>();
        propagator.setMasterMode(
                600,
                (currentState, isLast) -> states.add(currentState));
        propagator.propagate(orbit.getDate().shiftedBy(30 * Constants.JULIAN_DAY));

        // ephemeris generation
        propagator.setInitialState(new SpacecraftState(orbit, 45.0), false);
        propagator.setEphemerisMode();
        propagator.propagate(orbit.getDate().shiftedBy(30 * Constants.JULIAN_DAY));
        BoundedPropagator ephemeris = propagator.getGeneratedEphemeris();

        double maxError = 0;
        for (final SpacecraftState state : states) {
            final SpacecraftState fromEphemeris = ephemeris.propagate(state.getDate());
            final double error = Vector3D.distance(state.getPVCoordinates().getPosition(),
                                                   fromEphemeris.getPVCoordinates().getPosition());
            maxError = FastMath.max(maxError, error);
        }
        Assert.assertEquals(0.0, maxError, 1.0e-10);
    }

    @Test
    public void testGetInitialOsculatingState() throws IllegalArgumentException, OrekitException {
        final SpacecraftState initialState = getGEOrbit();

        // build integrator
        final double minStep = initialState.getKeplerianPeriod() * 0.1;
        final double maxStep = initialState.getKeplerianPeriod() * 10.0;
        final double[][] tol = DSSTPropagator.tolerances(0.1, initialState.getOrbit());
        AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(minStep, maxStep, tol[0], tol[1]);

        // build the propagator for the propagation of the mean elements
        DSSTPropagator prop = new DSSTPropagator(integrator, true);

        final UnnormalizedSphericalHarmonicsProvider provider =
                GravityFieldFactory.getUnnormalizedProvider(4, 0);
        final Frame earthFrame = CelestialBodyFactory.getEarth().getBodyOrientedFrame();
        DSSTForceModel zonal    = new DSSTZonal(provider, 4, 3, 9);
        DSSTForceModel tesseral = new DSSTTesseral(earthFrame,
                                                   Constants.WGS84_EARTH_ANGULAR_VELOCITY,
                                                   provider, 4, 0, 4, 8, 4, 0, 2);
        prop.addForceModel(zonal);
        prop.addForceModel(tesseral);

        // Set the initial state as osculating
        prop.setInitialState(initialState, false);
        // Check the stored initial state is the osculating one
        Assert.assertEquals(initialState, prop.getInitialState());
        // Check that no propagation, i.e. propagation to the initial date, provides the initial
        // osculating state although the propagator is configured to propagate mean elements !!!
        Assert.assertEquals(initialState, prop.propagate(initialState.getDate()));
    }

    @Test
    public void testMeanToOsculatingState() throws IllegalArgumentException, OrekitException {
        final SpacecraftState meanState = getGEOrbit();

        final UnnormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getUnnormalizedProvider(2, 0);
        final Frame earthFrame = CelestialBodyFactory.getEarth().getBodyOrientedFrame();
        final DSSTForceModel zonal    = new DSSTZonal(provider, 2, 1, 5);
        final DSSTForceModel tesseral = new DSSTTesseral(earthFrame,
                                                         Constants.WGS84_EARTH_ANGULAR_VELOCITY,
                                                         provider, 2, 0, 0, 2, 2, 0, 0);

        final Collection<DSSTForceModel> forces = new ArrayList<DSSTForceModel>();
        forces.add(zonal);
        forces.add(tesseral);

        final SpacecraftState osculatingState = DSSTPropagator.computeOsculatingState(meanState, forces);
        Assert.assertEquals(1559.1,
                            Vector3D.distance(meanState.getPVCoordinates().getPosition(),
                                              osculatingState.getPVCoordinates().getPosition()),
                            1.0);
    }

    @Test
    public void testOsculatingToMeanState() throws IllegalArgumentException, OrekitException {
        final SpacecraftState meanState = getGEOrbit();

        final UnnormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getUnnormalizedProvider(2, 0);
        final Frame earthFrame = CelestialBodyFactory.getEarth().getBodyOrientedFrame();

        DSSTForceModel zonal    = new DSSTZonal(provider, 2, 1, 5);
        DSSTForceModel tesseral = new DSSTTesseral(earthFrame,
                                                   Constants.WGS84_EARTH_ANGULAR_VELOCITY,
                                                   provider, 2, 0, 0, 2, 2, 0, 0);

        final Collection<DSSTForceModel> forces = new ArrayList<DSSTForceModel>();
        forces.add(zonal);
        forces.add(tesseral);

        final SpacecraftState osculatingState = DSSTPropagator.computeOsculatingState(meanState, forces);

        final SpacecraftState computedMeanState = DSSTPropagator.computeMeanState(osculatingState, forces);

        Assert.assertEquals(meanState.getA(), computedMeanState.getA(), 2.0e-8);
        Assert.assertEquals(0.0,
                            Vector3D.distance(meanState.getPVCoordinates().getPosition(),
                                             computedMeanState.getPVCoordinates().getPosition()),
                            2.0e-8);
    }

    @Test
    public void testShortPeriodCoefficients() throws OrekitException {
        Utils.setDataRoot("regular-data:potential/icgem-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("^eigen-6s-truncated$", false));
        UnnormalizedSphericalHarmonicsProvider nshp = GravityFieldFactory.getUnnormalizedProvider(4, 4);
        Orbit orbit = new KeplerianOrbit(13378000, 0.05, 0, 0, FastMath.PI, 0, PositionAngle.MEAN,
                                         FramesFactory.getTOD(false),
                                         new AbsoluteDate(2003, 5, 6, TimeScalesFactory.getUTC()),
                                         nshp.getMu());
        double period = orbit.getKeplerianPeriod();
        double[][] tolerance = DSSTPropagator.tolerances(1.0, orbit);
        AdaptiveStepsizeIntegrator integrator =
                new DormandPrince853Integrator(period / 100, period * 100, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(10 * period);
        DSSTPropagator propagator = new DSSTPropagator(integrator, false);
        OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                      Constants.WGS84_EARTH_FLATTENING,
                                                      FramesFactory.getGTOD(false));
        CelestialBody sun = CelestialBodyFactory.getSun();
        CelestialBody moon = CelestialBodyFactory.getMoon();
        propagator.addForceModel(new DSSTZonal(nshp, 4, 3, 9));
        propagator.addForceModel(new DSSTTesseral(earth.getBodyFrame(),
                                                  Constants.WGS84_EARTH_ANGULAR_VELOCITY,
                                                  nshp, 4, 4, 4, 8, 4, 4, 2));
        propagator.addForceModel(new DSSTThirdBody(sun));
        propagator.addForceModel(new DSSTThirdBody(moon));
        propagator.addForceModel(new DSSTAtmosphericDrag(new HarrisPriester(sun, earth), 2.1, 180));
        propagator.addForceModel(new DSSTSolarRadiationPressure(1.2, 180, sun, earth.getEquatorialRadius()));

        final AbsoluteDate finalDate = orbit.getDate().shiftedBy(30 * Constants.JULIAN_DAY);
        propagator.resetInitialState(new SpacecraftState(orbit, 45.0));
        final SpacecraftState stateNoConfig = propagator.propagate(finalDate);
        Assert.assertEquals(0, stateNoConfig.getAdditionalStates().size());

        propagator.setSelectedCoefficients(new HashSet<String>());
        propagator.resetInitialState(new SpacecraftState(orbit, 45.0));
        final SpacecraftState stateConfigEmpty = propagator.propagate(finalDate);
        Assert.assertEquals(234, stateConfigEmpty.getAdditionalStates().size());

        final Set<String> selected = new HashSet<String>();
        selected.add("DSST-3rd-body-Moon-s[7]");
        selected.add("DSST-central-body-tesseral-c[-2][3]");
        propagator.setSelectedCoefficients(selected);
        propagator.resetInitialState(new SpacecraftState(orbit, 45.0));
        final SpacecraftState stateConfigeSelected = propagator.propagate(finalDate);
        Assert.assertEquals(selected.size(), stateConfigeSelected.getAdditionalStates().size());

        propagator.setSelectedCoefficients(null);
        propagator.resetInitialState(new SpacecraftState(orbit, 45.0));
        final SpacecraftState stateConfigNull = propagator.propagate(finalDate);
        Assert.assertEquals(0, stateConfigNull.getAdditionalStates().size());

    }

    @Test
    public void testIssueMeanInclination() throws OrekitException {

        final double earthAe = 6378137.0;
        final double earthMu = 3.9860044E14;
        final double earthJ2 = 0.0010826;

        // Initialize the DSST propagator with only J2 perturbation
        Orbit orb = new KeplerianOrbit(new TimeStampedPVCoordinates(new AbsoluteDate("1992-10-08T15:20:38.821",
                                                                                     TimeScalesFactory.getUTC()),
                                                                    new Vector3D(5392808.809823, -4187618.3357927715, -44206.638015847195),
                                                                    new Vector3D(2337.4472786270794, 2474.0146611860464, 6778.507766114648)),
                                       FramesFactory.getTOD(false), earthMu);
        final SpacecraftState ss = new SpacecraftState(orb);
        final UnnormalizedSphericalHarmonicsProvider provider =
              GravityFieldFactory.getUnnormalizedProvider(earthAe, earthMu, TideSystem.UNKNOWN,
                                                          new double[][] { { 0.0 }, { 0.0 }, { -earthJ2 } },
                                                          new double[][] { { 0.0 }, { 0.0 }, { 0.0 } });
        final Frame earthFrame = CelestialBodyFactory.getEarth().getBodyOrientedFrame();
        DSSTForceModel zonal    = new DSSTZonal(provider, 2, 1, 5);
        DSSTForceModel tesseral = new DSSTTesseral(earthFrame,
                                                   Constants.WGS84_EARTH_ANGULAR_VELOCITY,
                                                   provider, 2, 0, 0, 2, 2, 0, 0);
        final Collection<DSSTForceModel> forces = new ArrayList<DSSTForceModel>();
        forces.add(zonal);
        forces.add(tesseral);
        // Computes J2 mean elements using the DSST osculating to mean converter
        final Orbit meanOrb = DSSTPropagator.computeMeanState(ss, forces).getOrbit();
        Assert.assertEquals(0.0164196, FastMath.toDegrees(orb.getI() - meanOrb.getI()), 1.0e-7);
    }

    @Test
    public void testIssue257() throws OrekitException {
        final SpacecraftState meanState = getGEOrbit();

        // Third Bodies Force Model (Moon + Sun) */
        final DSSTForceModel moon = new DSSTThirdBody(CelestialBodyFactory.getMoon());
        final DSSTForceModel sun  = new DSSTThirdBody(CelestialBodyFactory.getSun());

        final Collection<DSSTForceModel> forces = new ArrayList<DSSTForceModel>();
        forces.add(moon);
        forces.add(sun);

        final SpacecraftState osculatingState = DSSTPropagator.computeOsculatingState(meanState, forces);
        Assert.assertEquals(734.3,
                            Vector3D.distance(meanState.getPVCoordinates().getPosition(),
                                              osculatingState.getPVCoordinates().getPosition()),
                            1.0);

        final SpacecraftState computedMeanState = DSSTPropagator.computeMeanState(osculatingState, forces);
        Assert.assertEquals(734.3,
                            Vector3D.distance(osculatingState.getPVCoordinates().getPosition(),
                                              computedMeanState.getPVCoordinates().getPosition()),
                            1.0);

        Assert.assertEquals(0.0,
                            Vector3D.distance(computedMeanState.getPVCoordinates().getPosition(),
                                              meanState.getPVCoordinates().getPosition()),
                            5.0e-6);

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
