/* Copyright 2002-2024 CS GROUP
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
package org.orekit.propagation.semianalytical.dsst;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.ode.ODEIntegrator;
import org.hipparchus.ode.events.Action;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince54Integrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.hipparchus.util.MathUtils;
import org.hipparchus.util.Precision;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.OrekitMatchers;
import org.orekit.TestUtils;
import org.orekit.Utils;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.LofOffset;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.forces.BoxAndSolarArraySpacecraft;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.potential.GRGSFormatReader;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.ICGEMFormatReader;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.TideSystem;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.forces.maneuvers.ImpulseManeuver;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.models.earth.atmosphere.Atmosphere;
import org.orekit.models.earth.atmosphere.HarrisPriester;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.EphemerisGenerator;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.AltitudeDetector;
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.events.LatitudeCrossingDetector;
import org.orekit.propagation.events.NodeDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.semianalytical.dsst.forces.AbstractGaussianContribution;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTAtmosphericDrag;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTSolarRadiationPressure;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTTesseral;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTThirdBody;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTZonal;
import org.orekit.propagation.semianalytical.dsst.forces.FieldShortPeriodTerms;
import org.orekit.propagation.semianalytical.dsst.forces.ShortPeriodTerms;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.propagation.semianalytical.dsst.utilities.FieldAuxiliaryElements;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeSpanMap;
import org.orekit.utils.TimeStampedPVCoordinates;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class DSSTPropagatorTest {

    private DSSTPropagator dsstProp;

    /**
     * Test issue #1029 about DSST short period terms computation.
     * Issue #1029 is a regression introduced in version 10.0
     * Test case built from Christophe Le Bris example: https://gitlab.orekit.org/orekit/orekit/-/issues/1029
     */
    @Test
    public void testIssue1029() {

        Utils.setDataRoot("regular-data:potential/grgs-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));

        // initial state
        final AbsoluteDate orbitEpoch = new AbsoluteDate(2023, 2, 18, TimeScalesFactory.getUTC());
        final Frame inertial = FramesFactory.getCIRF(IERSConventions.IERS_2010, true);
        final KeplerianOrbit orbit = new KeplerianOrbit(42166000.0, 0.00028, FastMath.toRadians(0.05), FastMath.toRadians(66.0),
                                                        FastMath.toRadians(270.0), FastMath.toRadians(11.94), PositionAngleType.MEAN,
                                                        inertial, orbitEpoch, Constants.WGS84_EARTH_MU);
        final EquinoctialOrbit equinoctial = new EquinoctialOrbit(orbit);

        // create propagator
        final double[][] tol = DSSTPropagator.tolerances(0.001, equinoctial);
        final AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(3600.0, 86400.0, tol[0], tol[1]);
        final DSSTPropagator propagator = new DSSTPropagator(integrator, PropagationType.OSCULATING);

        // add force models
        final Frame ecefFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        final UnnormalizedSphericalHarmonicsProvider gravityProvider = GravityFieldFactory.getUnnormalizedProvider(8, 8);
        propagator.addForceModel(new DSSTZonal(gravityProvider));
        propagator.addForceModel(new DSSTTesseral(ecefFrame, Constants.WGS84_EARTH_ANGULAR_VELOCITY, gravityProvider));
        propagator.addForceModel(new DSSTThirdBody(CelestialBodyFactory.getSun(), gravityProvider.getMu()));
        propagator.addForceModel(new DSSTThirdBody(CelestialBodyFactory.getMoon(), gravityProvider.getMu()));

        // propagate
        propagator.setInitialState(new SpacecraftState(equinoctial, 6000.0), PropagationType.MEAN);
        SpacecraftState propagated = propagator.propagate(orbitEpoch.shiftedBy(20.0 * Constants.JULIAN_DAY));

        // The purpose is not verifying propagated values, but to check that no exception occurred
        Assertions.assertEquals(0.0, propagated.getDate().durationFrom(orbitEpoch.shiftedBy(20.0 * Constants.JULIAN_DAY)), Double.MIN_VALUE);
        Assertions.assertEquals(4.216464862956647E7, propagated.getA(), Double.MIN_VALUE);

    }

    @Test
    public void testIssue363() {
        Utils.setDataRoot("regular-data");
        AbsoluteDate date = new AbsoluteDate("2003-06-18T00:00:00.000", TimeScalesFactory.getUTC());
        CircularOrbit orbit = new CircularOrbit(7389068.5, 1.0e-15, 1.0e-15, 1.709573, 1.308398, 0, PositionAngleType.MEAN,
                                                FramesFactory.getTOD(IERSConventions.IERS_2010, false),
                                                date, Constants.WGS84_EARTH_MU);
        SpacecraftState osculatingState = new SpacecraftState(orbit, 1116.2829);

        List<DSSTForceModel> dsstForceModels = new ArrayList<DSSTForceModel>();

        dsstForceModels.add(new DSSTThirdBody(CelestialBodyFactory.getMoon(), orbit.getMu()));
        dsstForceModels.add(new DSSTThirdBody(CelestialBodyFactory.getSun(), orbit.getMu()));

        SpacecraftState meanState = DSSTPropagator.computeMeanState(osculatingState, null, dsstForceModels);
        Assertions.assertEquals( 0.421,   osculatingState.getA()             - meanState.getA(),             1.0e-3);
        Assertions.assertEquals(-5.23e-8, osculatingState.getEquinoctialEx() - meanState.getEquinoctialEx(), 1.0e-10);
        Assertions.assertEquals(15.22e-8, osculatingState.getEquinoctialEy() - meanState.getEquinoctialEy(), 1.0e-10);
        Assertions.assertEquals(-3.15e-8, osculatingState.getHx()            - meanState.getHx(),            1.0e-10);
        Assertions.assertEquals( 2.83e-8, osculatingState.getHy()            - meanState.getHy(),            1.0e-10);
        Assertions.assertEquals(15.96e-8, osculatingState.getLM()            - meanState.getLM(),            1.0e-10);

    }

    @Test
    public void testIssue364() {
        Utils.setDataRoot("regular-data");
        AbsoluteDate date = new AbsoluteDate("2003-06-18T00:00:00.000", TimeScalesFactory.getUTC());
        CircularOrbit orbit = new CircularOrbit(7389068.5, 0.0, 0.0, 1.709573, 1.308398, 0, PositionAngleType.MEAN,
                                                FramesFactory.getTOD(IERSConventions.IERS_2010, false),
                                                date, Constants.WGS84_EARTH_MU);
        SpacecraftState osculatingState = new SpacecraftState(orbit, 1116.2829);

        List<DSSTForceModel> dsstForceModels = new ArrayList<DSSTForceModel>();

        dsstForceModels.add(new DSSTThirdBody(CelestialBodyFactory.getMoon(), orbit.getMu()));
        dsstForceModels.add(new DSSTThirdBody(CelestialBodyFactory.getSun(), orbit.getMu()));

        SpacecraftState meanState = DSSTPropagator.computeMeanState(osculatingState, null, dsstForceModels);
        Assertions.assertEquals( 0.421,   osculatingState.getA()             - meanState.getA(),             1.0e-3);
        Assertions.assertEquals(-5.23e-8, osculatingState.getEquinoctialEx() - meanState.getEquinoctialEx(), 1.0e-10);
        Assertions.assertEquals(15.22e-8, osculatingState.getEquinoctialEy() - meanState.getEquinoctialEy(), 1.0e-10);
        Assertions.assertEquals(-3.15e-8, osculatingState.getHx()            - meanState.getHx(),            1.0e-10);
        Assertions.assertEquals( 2.83e-8, osculatingState.getHy()            - meanState.getHy(),            1.0e-10);
        Assertions.assertEquals(15.96e-8, osculatingState.getLM()            - meanState.getLM(),            1.0e-10);

    }

    @Test
    public void testHighDegreesSetting() {

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
                        new DSSTTesseral(earthFrame, Constants.WGS84_EARTH_ANGULAR_VELOCITY, provider,
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
                                                  PositionAngleType.TRUE, eci, initialDate, Constants.EIGEN5C_EARTH_MU);
        SpacecraftState oscuState = DSSTPropagator.computeOsculatingState(new SpacecraftState(orbit), null, forces);
        Assertions.assertEquals(7119927.097122, oscuState.getA(), 0.001);
    }

    @Test
    public void testEphemerisDates() {
        //setup
        TimeScale tai = TimeScalesFactory.getTAI();
        AbsoluteDate initialDate = new AbsoluteDate("2015-07-01", tai);
        AbsoluteDate startDate = new AbsoluteDate("2015-07-03", tai).shiftedBy(-0.1);
        AbsoluteDate endDate = new AbsoluteDate("2015-07-04", tai);
        Frame eci = FramesFactory.getGCRF();
        KeplerianOrbit orbit = new KeplerianOrbit(
                                                  600e3 + Constants.WGS84_EARTH_EQUATORIAL_RADIUS, 0, 0, 0, 0, 0,
                                                  PositionAngleType.TRUE, eci, initialDate, Constants.EIGEN5C_EARTH_MU);
        double[][] tol = DSSTPropagator
                        .tolerances(1, orbit);
        Propagator prop = new DSSTPropagator(
                                             new DormandPrince853Integrator(0.1, 500, tol[0], tol[1]));
        prop.resetInitialState(new SpacecraftState(new CartesianOrbit(orbit)));

        //action
        EphemerisGenerator generator = prop.getEphemerisGenerator();
        prop.propagate(startDate, endDate);
        BoundedPropagator ephemeris = generator.getGeneratedEphemeris();

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
        Assertions.assertEquals(
                                ephemeris.propagate(date).getDate().durationFrom(date), 0, 0);
    }

    @Test
    public void testNoExtrapolation() {
        SpacecraftState state = getLEOState();
        setDSSTProp(state);

        // Propagation of the initial state at the initial date
        final SpacecraftState finalState = dsstProp.propagate(state.getDate());

        // Initial orbit definition
        final Vector3D initialPosition = state.getPosition();
        final Vector3D initialVelocity = state.getPVCoordinates().getVelocity();

        // Final orbit definition
        final Vector3D finalPosition = finalState.getPosition();
        final Vector3D finalVelocity = finalState.getPVCoordinates().getVelocity();

        // Check results
        Assertions.assertEquals(initialPosition.getX(), finalPosition.getX(), 0.0);
        Assertions.assertEquals(initialPosition.getY(), finalPosition.getY(), 0.0);
        Assertions.assertEquals(initialPosition.getZ(), finalPosition.getZ(), 0.0);
        Assertions.assertEquals(initialVelocity.getX(), finalVelocity.getX(), 0.0);
        Assertions.assertEquals(initialVelocity.getY(), finalVelocity.getY(), 0.0);
        Assertions.assertEquals(initialVelocity.getZ(), finalVelocity.getZ(), 0.0);
    }

    @Test
    public void testKepler() {
        SpacecraftState state = getLEOState();
        setDSSTProp(state);

        Assertions.assertEquals(2, dsstProp.getSatelliteRevolution());
        dsstProp.setSatelliteRevolution(17);
        Assertions.assertEquals(17, dsstProp.getSatelliteRevolution());

        // Propagation of the initial state at t + dt
        final double dt = 3200.;
        final SpacecraftState finalState = dsstProp.propagate(state.getDate().shiftedBy(dt));

        // Check results
        final double n = FastMath.sqrt(state.getMu() / state.getA()) / state.getA();
        Assertions.assertEquals(state.getA(), finalState.getA(), 0.);
        Assertions.assertEquals(state.getEquinoctialEx(), finalState.getEquinoctialEx(), 0.);
        Assertions.assertEquals(state.getEquinoctialEy(), finalState.getEquinoctialEy(), 0.);
        Assertions.assertEquals(state.getHx(), finalState.getHx(), 0.);
        Assertions.assertEquals(state.getHy(), finalState.getHy(), 0.);
        Assertions.assertEquals(state.getLM() + n * dt, finalState.getLM(), 1.e-14);

    }

    @Test
    public void testEphemeris() {
        SpacecraftState state = getGEOState();
        setDSSTProp(state);

        // Set ephemeris mode
        EphemerisGenerator generator = dsstProp.getEphemerisGenerator();

        // Propagation of the initial state at t + 10 days
        final double dt = 2. * Constants.JULIAN_DAY;
        dsstProp.propagate(state.getDate().shiftedBy(5. * dt));

        // Get ephemeris
        BoundedPropagator ephem = generator.getGeneratedEphemeris();

        // Propagation of the initial state with ephemeris at t + 2 days
        final SpacecraftState s = ephem.propagate(state.getDate().shiftedBy(dt));

        // Check results
        final double n = FastMath.sqrt(state.getMu() / state.getA()) / state.getA();
        Assertions.assertEquals(state.getA(), s.getA(), 0.);
        Assertions.assertEquals(state.getEquinoctialEx(), s.getEquinoctialEx(), 0.);
        Assertions.assertEquals(state.getEquinoctialEy(), s.getEquinoctialEy(), 0.);
        Assertions.assertEquals(state.getHx(), s.getHx(), 0.);
        Assertions.assertEquals(state.getHy(), s.getHy(), 0.);
        Assertions.assertEquals(state.getLM() + n * dt, s.getLM(), 1.5e-14);

    }

    @Test
    public void testImpulseManeuver() {
        final Orbit initialOrbit = new KeplerianOrbit(24532000.0, 0.72, 0.3, FastMath.PI, 0.4, 2.0, PositionAngleType.MEAN, FramesFactory.getEME2000(), new AbsoluteDate(new DateComponents(2008, 06, 23), new TimeComponents(14, 18, 37), TimeScalesFactory.getUTC()), 3.986004415e14);
        final double a = initialOrbit.getA();
        final double e = initialOrbit.getE();
        final double i = initialOrbit.getI();
        final double mu = initialOrbit.getMu();
        final double vApo = FastMath.sqrt(mu * (1 - e) / (a * (1 + e)));
        double dv = 0.99 * FastMath.tan(i) * vApo;

        // Set propagator with state
        setDSSTProp(new SpacecraftState(initialOrbit));

        // Add impulse maneuver
        dsstProp.setAttitudeProvider(new LofOffset(initialOrbit.getFrame(), LOFType.LVLH_CCSDS));
        dsstProp.addEventDetector(new ImpulseManeuver(new NodeDetector(initialOrbit, FramesFactory.getEME2000()), new Vector3D(dv, Vector3D.PLUS_J), 400.0));
        SpacecraftState propagated = dsstProp.propagate(initialOrbit.getDate().shiftedBy(8000));

        Assertions.assertEquals(0.0028257, propagated.getI(), 1.0e-6);
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
                                               PositionAngleType.MEAN,
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
        Assertions.assertEquals(26559920.81, state.getA(), 1.e-1);
        Assertions.assertEquals(0.2731622444E-03, state.getEquinoctialEx(), 2.e-8);
        Assertions.assertEquals(0.4164167597E-02, state.getEquinoctialEy(), 2.e-8);
        Assertions.assertEquals(-0.3399607878, state.getHx(), 5.e-8);
        Assertions.assertEquals(0.3971568634, state.getHy(), 2.e-6);
        Assertions.assertEquals(140.6375352,
                                FastMath.toDegrees(MathUtils.normalizeAngle(state.getLM(), FastMath.PI)),
                                5.e-3);
    }

    @Test
    public void testPropagationWithThirdBody() throws IOException {

        // Central Body geopotential 2x0
        final UnnormalizedSphericalHarmonicsProvider provider =
                        GravityFieldFactory.getUnnormalizedProvider(2, 0);
        final Frame earthFrame = CelestialBodyFactory.getEarth().getBodyOrientedFrame();
        DSSTForceModel zonal    = new DSSTZonal(provider, 2, 1, 5);
        DSSTForceModel tesseral = new DSSTTesseral(earthFrame,
                                                   Constants.WGS84_EARTH_ANGULAR_VELOCITY,
                                                   provider, 2, 0, 0, 2, 2, 0, 0);

        // Third Bodies Force Model (Moon + Sun)
        DSSTForceModel moon = new DSSTThirdBody(CelestialBodyFactory.getMoon(), provider.getMu());
        DSSTForceModel sun  = new DSSTThirdBody(CelestialBodyFactory.getSun(), provider.getMu());

        // SIRIUS Orbit
        final AbsoluteDate initDate = new AbsoluteDate(2003, 7, 1, 0, 0, 00.000,
                                                       TimeScalesFactory.getUTC());
        final Orbit orbit = new KeplerianOrbit(42163393.,
                                               0.2684,
                                               FastMath.toRadians(63.435),
                                               FastMath.toRadians(270.0),
                                               FastMath.toRadians(285.0),
                                               FastMath.toRadians(344.0),
                                               PositionAngleType.MEAN,
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
        Assertions.assertEquals(42163393.0, state.getA(), 1.e-1);
        Assertions.assertEquals(-0.2592789733084587, state.getEquinoctialEx(), 5.e-7);
        Assertions.assertEquals(-0.06893353670734315, state.getEquinoctialEy(), 2.e-7);
        Assertions.assertEquals( 0.1595005111738418, state.getHx(), 2.e-7);
        Assertions.assertEquals(-0.5968524904937771, state.getHy(), 5.e-8);
        Assertions.assertEquals(183.9386620425922,
                                FastMath.toDegrees(MathUtils.normalizeAngle(state.getLM(), FastMath.PI)),
                                3.e-2);
    }

    @Test
    public void testTooSmallMaxDegree() {
        Assertions.assertThrows(OrekitException.class, () -> {
            new DSSTZonal(GravityFieldFactory.getUnnormalizedProvider(2, 0), 1, 0, 3);
        });
    }

    @Test
    public void testTooLargeMaxDegree() {
        Assertions.assertThrows(OrekitException.class, () -> {
            new DSSTZonal(GravityFieldFactory.getUnnormalizedProvider(2, 0), 8, 0, 8);
        });
    }

    @Test
    public void testWrongMaxPower() {
        Assertions.assertThrows(OrekitException.class, () -> {
            new DSSTZonal(GravityFieldFactory.getUnnormalizedProvider(8, 8), 4, 4, 4);
        });
    }

    @Test
    public void testPropagationWithDrag() {

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
        DSSTForceModel drag = new DSSTAtmosphericDrag(atm, cd, area, provider.getMu());

        // LEO Orbit
        final AbsoluteDate initDate = new AbsoluteDate(2003, 7, 1, 0, 0, 00.000,
                                                       TimeScalesFactory.getUTC());
        final Orbit orbit = new KeplerianOrbit(7204535.848109440,
                                               0.0012402238462686,
                                               FastMath.toRadians(98.74341600466740),
                                               FastMath.toRadians(111.1990175076630),
                                               FastMath.toRadians(43.32990110790340),
                                               FastMath.toRadians(68.66852509725620),
                                               PositionAngleType.MEAN,
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
        Assertions.assertEquals(7204521.657141485, state.getA(), 6.e-1);
        Assertions.assertEquals(-0.001016800430994036, state.getEquinoctialEx(), 5.e-8);
        Assertions.assertEquals(0.0007093755541595772, state.getEquinoctialEy(), 2.e-8);
        Assertions.assertEquals(0.7757573478894775, state.getHx(), 5.e-8);
        Assertions.assertEquals(0.8698955648709271, state.getHy(), 5.e-8);
        Assertions.assertEquals(193.0939742953394,
                                FastMath.toDegrees(MathUtils.normalizeAngle(state.getLM(), FastMath.PI)),
                                2.e-3);
        //Assertions.assertEquals(((DSSTAtmosphericDrag)drag).getCd(), cd, 1e-9);
        //Assertions.assertEquals(((DSSTAtmosphericDrag)drag).getArea(), area, 1e-9);
        Assertions.assertEquals(((DSSTAtmosphericDrag)drag).getAtmosphere(), atm);

        final double atmosphericMaxConstant = 1000000.0; //DSSTAtmosphericDrag.ATMOSPHERE_ALTITUDE_MAX
        Assertions.assertEquals(((DSSTAtmosphericDrag)drag).getRbar(), atmosphericMaxConstant + Constants.WGS84_EARTH_EQUATORIAL_RADIUS, 1e-9);
    }

    @Test
    public void testPropagationWithSolarRadiationPressure() {

        // Central Body geopotential 2x0
        final UnnormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getUnnormalizedProvider(2, 0);
        DSSTForceModel zonal    = new DSSTZonal(provider, 2, 1, 5);
        DSSTForceModel tesseral = new DSSTTesseral(CelestialBodyFactory.getEarth().getBodyOrientedFrame(),
                                                   Constants.WGS84_EARTH_ANGULAR_VELOCITY,
                                                   provider, 2, 0, 0, 2, 2, 0, 0);

        // SRP Force Model
        DSSTForceModel srp = new DSSTSolarRadiationPressure(1.2, 100., CelestialBodyFactory.getSun(),
                                                            new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                                 Constants.WGS84_EARTH_FLATTENING,
                                                                                 CelestialBodyFactory.getEarth().getBodyOrientedFrame()),
                                                            provider.getMu());

        // GEO Orbit
        final AbsoluteDate initDate = new AbsoluteDate(2003, 9, 16, 0, 0, 00.000,
                                                       TimeScalesFactory.getUTC());
        final Orbit orbit = new KeplerianOrbit(42166258.,
                                               0.0001,
                                               FastMath.toRadians(0.001),
                                               FastMath.toRadians(315.4985),
                                               FastMath.toRadians(130.7562),
                                               FastMath.toRadians(44.2377),
                                               PositionAngleType.MEAN,
                                               FramesFactory.getGCRF(),
                                               initDate,
                                               provider.getMu());

        // Set propagator with state and force model
        dsstProp = new DSSTPropagator(new ClassicalRungeKuttaIntegrator(86400.));
        dsstProp.setInitialState(new SpacecraftState(orbit), PropagationType.MEAN);
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
        Assertions.assertEquals(42166257.99807995, state.getA(), 0.9);
        Assertions.assertEquals(-0.1781865038201885e-05, state.getEquinoctialEx(), 3.e-7);
        Assertions.assertEquals(-0.1191876027555493e-03, state.getEquinoctialEy(), 4.e-6);
        Assertions.assertEquals(-0.5624363171289686e-05, state.getHx(), 4.e-9);
        Assertions.assertEquals( 0.6618387121369373e-05, state.getHy(), 3.e-10);
        Assertions.assertEquals(140.3496229467104,
                                FastMath.toDegrees(MathUtils.normalizeAngle(state.getLM(), FastMath.PI)),
                                2.e-4);
    }

    @Test
    public void testStopEvent() {
        SpacecraftState state = getLEOState();
        setDSSTProp(state);

        final AbsoluteDate stopDate = state.getDate().shiftedBy(1000);
        CheckingHandler checking = new CheckingHandler(Action.STOP);
        dsstProp.addEventDetector(new DateDetector(stopDate).withHandler(checking));
        checking.assertEvent(false);
        final SpacecraftState finalState = dsstProp.propagate(state.getDate().shiftedBy(3200));
        checking.assertEvent(true);
        Assertions.assertEquals(0, finalState.getDate().durationFrom(stopDate), 1.0e-10);
    }

    @Test
    public void testContinueEvent() {
        SpacecraftState state = getLEOState();
        setDSSTProp(state);

        final AbsoluteDate resetDate = state.getDate().shiftedBy(1000);
        CheckingHandler checking = new CheckingHandler(Action.CONTINUE);
        dsstProp.addEventDetector(new DateDetector(resetDate).withHandler(checking));
        final double dt = 3200;
        checking.assertEvent(false);
        final SpacecraftState finalState = dsstProp.propagate(state.getDate().shiftedBy(dt));
        checking.assertEvent(true);
        final double n = FastMath.sqrt(state.getMu() / state.getA()) / state.getA();
        Assertions.assertEquals(state.getA(), finalState.getA(), 1.0e-10);
        Assertions.assertEquals(state.getEquinoctialEx(), finalState.getEquinoctialEx(), 1.0e-10);
        Assertions.assertEquals(state.getEquinoctialEy(), finalState.getEquinoctialEy(), 1.0e-10);
        Assertions.assertEquals(state.getHx(), finalState.getHx(), 1.0e-10);
        Assertions.assertEquals(state.getHy(), finalState.getHy(), 1.0e-10);
        Assertions.assertEquals(state.getLM() + n * dt, finalState.getLM(), 6.0e-10);
    }

    @Test
    public void testIssue157() {
        Utils.setDataRoot("regular-data:potential/icgem-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("^eigen-6s-truncated$", false));
        UnnormalizedSphericalHarmonicsProvider nshp = GravityFieldFactory.getUnnormalizedProvider(8, 8);
        Orbit orbit = new KeplerianOrbit(13378000, 0.05, 0, 0, FastMath.PI, 0, PositionAngleType.MEAN,
                                         FramesFactory.getTOD(false),
                                         new AbsoluteDate(2003, 5, 6, TimeScalesFactory.getUTC()),
                                         nshp.getMu());
        double period = orbit.getKeplerianPeriod();
        double[][] tolerance = DSSTPropagator.tolerances(1.0, orbit);
        AdaptiveStepsizeIntegrator integrator =
                        new DormandPrince853Integrator(period / 100, period * 100, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(10 * period);
        DSSTPropagator propagator = new DSSTPropagator(integrator, PropagationType.MEAN);
        OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                      Constants.WGS84_EARTH_FLATTENING,
                                                      FramesFactory.getGTOD(false));
        CelestialBody sun = CelestialBodyFactory.getSun();
        CelestialBody moon = CelestialBodyFactory.getMoon();
        propagator.addForceModel(new DSSTZonal(nshp, 8, 7, 17));
        propagator.addForceModel(new DSSTTesseral(earth.getBodyFrame(),
                                                  Constants.WGS84_EARTH_ANGULAR_VELOCITY,
                                                  nshp, 8, 8, 4, 12, 8, 8, 4));
        propagator.addForceModel(new DSSTThirdBody(sun, nshp.getMu()));
        propagator.addForceModel(new DSSTThirdBody(moon, nshp.getMu()));
        propagator.addForceModel(new DSSTAtmosphericDrag(new HarrisPriester(sun, earth), 2.1, 180, nshp.getMu()));
        propagator.addForceModel(new DSSTSolarRadiationPressure(1.2, 180, sun, earth, nshp.getMu()));


        propagator.setInitialState(new SpacecraftState(orbit, 45.0), PropagationType.OSCULATING);
        SpacecraftState finalState = propagator.propagate(orbit.getDate().shiftedBy(30 * Constants.JULIAN_DAY));
        // the following comparison is in fact meaningless
        // the initial orbit is osculating the final orbit is a mean orbit
        // and they are not considered at the same epoch
        // we keep it only as is was an historical test
        Assertions.assertEquals(2187.2, orbit.getA() - finalState.getA(), 1.0);

        propagator.setInitialState(new SpacecraftState(orbit, 45.0), PropagationType.MEAN);
        finalState = propagator.propagate(orbit.getDate().shiftedBy(30 * Constants.JULIAN_DAY));
        // the following comparison is realistic
        // both the initial orbit and final orbit are mean orbits
        Assertions.assertEquals(1475.9, orbit.getA() - finalState.getA(), 1.0);

    }

    /**
     * Compare classical propagation with a fixed-step handler with ephemeris generation on the same points.
     */
    @Test
    public void testEphemerisGeneration() {

        // GIVEN
        // -----

        Utils.setDataRoot("regular-data:potential/icgem-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("^eigen-6s-truncated$", false));
        UnnormalizedSphericalHarmonicsProvider nshp = GravityFieldFactory.getUnnormalizedProvider(8, 8);
        Orbit orbit = new KeplerianOrbit(13378000, 0.05, 0, 0, FastMath.PI, 0, PositionAngleType.MEAN,
                                         FramesFactory.getTOD(false),
                                         new AbsoluteDate(2003, 5, 6, TimeScalesFactory.getUTC()),
                                         nshp.getMu());
        double period = orbit.getKeplerianPeriod();
        double[][] tolerance = DSSTPropagator.tolerances(1.0, orbit);
        AdaptiveStepsizeIntegrator integrator =
                        new DormandPrince853Integrator(period / 100, period * 100, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(10 * period);
        DSSTPropagator propagator = new DSSTPropagator(integrator, PropagationType.OSCULATING);
        OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                      Constants.WGS84_EARTH_FLATTENING,
                                                      FramesFactory.getGTOD(false));
        CelestialBody sun = CelestialBodyFactory.getSun();
        CelestialBody moon = CelestialBodyFactory.getMoon();
        propagator.addForceModel(new DSSTZonal(nshp, 8, 7, 17));
        propagator.addForceModel(new DSSTTesseral(earth.getBodyFrame(),
                                                  Constants.WGS84_EARTH_ANGULAR_VELOCITY,
                                                  nshp, 8, 8, 4, 12, 8, 8, 4));
        propagator.addForceModel(new DSSTThirdBody(sun, nshp.getMu()));
        propagator.addForceModel(new DSSTThirdBody(moon, nshp.getMu()));
        propagator.addForceModel(new DSSTAtmosphericDrag(new HarrisPriester(sun, earth), 2.1, 180, nshp.getMu()));
        propagator.addForceModel(new DSSTSolarRadiationPressure(1.2, 180, sun, earth, nshp.getMu()));
        propagator.setInterpolationGridToMaxTimeGap(0.5 * Constants.JULIAN_DAY);


        // WHEN
        // ----

        // Number of days of propagation
        // Was 30 days but was reduced for issue 1106
        final double nDays = 5.;

        // direct generation of states
        propagator.setInitialState(new SpacecraftState(orbit, 45.0), PropagationType.MEAN);
        final List<SpacecraftState> states = new ArrayList<SpacecraftState>();
        propagator.setStepHandler(600, currentState -> states.add(currentState));
        propagator.propagate(orbit.getDate().shiftedBy(nDays * Constants.JULIAN_DAY));

        // ephemeris generation
        propagator.setInitialState(new SpacecraftState(orbit, 45.0), PropagationType.MEAN);
        final EphemerisGenerator generator = propagator.getEphemerisGenerator();
        propagator.propagate(orbit.getDate().shiftedBy(nDays * Constants.JULIAN_DAY));
        BoundedPropagator ephemeris = generator.getGeneratedEphemeris();

        double maxError = 0;
        for (final SpacecraftState state : states) {
            final SpacecraftState fromEphemeris = ephemeris.propagate(state.getDate());
            final double error = Vector3D.distance(state.getPosition(),
                                                   fromEphemeris.getPosition());
            maxError = FastMath.max(maxError, error);
        }

        // THEN
        // ----

        // Check on orbits' distances was 1e-10 m but was reduced during issue 1106
        Assertions.assertEquals(0.0, maxError, Precision.SAFE_MIN);
    }

    @Test
    public void testGetInitialOsculatingState() throws IllegalArgumentException, OrekitException {
        final SpacecraftState initialState = getGEOState();

        // build integrator
        final double minStep = initialState.getKeplerianPeriod() * 0.1;
        final double maxStep = initialState.getKeplerianPeriod() * 10.0;
        final double[][] tol = DSSTPropagator.tolerances(0.1, initialState.getOrbit());
        AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(minStep, maxStep, tol[0], tol[1]);

        // build the propagator for the propagation of the mean elements
        DSSTPropagator prop = new DSSTPropagator(integrator, PropagationType.MEAN);

        final UnnormalizedSphericalHarmonicsProvider provider =
                        GravityFieldFactory.getUnnormalizedProvider(4, 0);
        final Frame earthFrame = CelestialBodyFactory.getEarth().getBodyOrientedFrame();
        DSSTForceModel zonal    = new DSSTZonal(provider, 4, 3, 9);
        DSSTForceModel tesseral = new DSSTTesseral(earthFrame,
                                                   Constants.WGS84_EARTH_ANGULAR_VELOCITY,
                                                   provider, 4, 0, 4, 8, 4, 0, 2);
        prop.addForceModel(zonal);
        prop.addForceModel(tesseral);

        // Set the initial state
        prop.setInitialState(initialState, PropagationType.MEAN);
        // Check the stored initial state is the osculating one
        Assertions.assertEquals(initialState, prop.getInitialState());
        // Check that no propagation, i.e. propagation to the initial date, provides the initial
        // osculating state although the propagator is configured to propagate mean elements !!!
        Assertions.assertEquals(initialState, prop.propagate(initialState.getDate()));
    }

    @Test
    public void testMeanToOsculatingState() throws IllegalArgumentException, OrekitException {
        final SpacecraftState meanState = getGEOState();

        final UnnormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getUnnormalizedProvider(2, 0);
        final Frame earthFrame = CelestialBodyFactory.getEarth().getBodyOrientedFrame();
        final DSSTForceModel zonal    = new DSSTZonal(provider, 2, 1, 5);
        final DSSTForceModel tesseral = new DSSTTesseral(earthFrame,
                                                         Constants.WGS84_EARTH_ANGULAR_VELOCITY,
                                                         provider, 2, 0, 0, 2, 2, 0, 0);

        final Collection<DSSTForceModel> forces = new ArrayList<DSSTForceModel>();
        forces.add(zonal);
        forces.add(tesseral);

        final SpacecraftState osculatingState = DSSTPropagator.computeOsculatingState(meanState, null, forces);
        Assertions.assertEquals(1559.1,
                                Vector3D.distance(meanState.getPosition(),
                                                  osculatingState.getPosition()),
                                1.0);
    }

    @Test
    public void testOsculatingToMeanState() throws IllegalArgumentException, OrekitException {
        final SpacecraftState meanState = getGEOState();

        final UnnormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getUnnormalizedProvider(2, 0);
        final Frame earthFrame = CelestialBodyFactory.getEarth().getBodyOrientedFrame();

        DSSTForceModel zonal    = new DSSTZonal(provider, 2, 1, 5);
        DSSTForceModel tesseral = new DSSTTesseral(earthFrame,
                                                   Constants.WGS84_EARTH_ANGULAR_VELOCITY,
                                                   provider, 2, 0, 0, 2, 2, 0, 0);

        final Collection<DSSTForceModel> forces = new ArrayList<DSSTForceModel>();
        forces.add(zonal);
        forces.add(tesseral);

        final SpacecraftState osculatingState = DSSTPropagator.computeOsculatingState(meanState, null, forces);

        // there are no Gaussian force models, we don't need an attitude provider
        final SpacecraftState computedMeanState = DSSTPropagator.computeMeanState(osculatingState, null, forces);

        Assertions.assertEquals(meanState.getA(), computedMeanState.getA(), 2.0e-8);
        Assertions.assertEquals(0.0,
                                Vector3D.distance(meanState.getPosition(),
                                                  computedMeanState.getPosition()),
                                2.0e-8);
    }

    @Test
    public void testShortPeriodCoefficients() {
        Utils.setDataRoot("regular-data:potential/icgem-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("^eigen-6s-truncated$", false));
        UnnormalizedSphericalHarmonicsProvider nshp = GravityFieldFactory.getUnnormalizedProvider(4, 4);
        Orbit orbit = new KeplerianOrbit(13378000, 0.05, 0, 0, FastMath.PI, 0, PositionAngleType.MEAN,
                                         FramesFactory.getTOD(false),
                                         new AbsoluteDate(2003, 5, 6, TimeScalesFactory.getUTC()),
                                         nshp.getMu());
        double period = orbit.getKeplerianPeriod();
        double[][] tolerance = DSSTPropagator.tolerances(1.0, orbit);
        AdaptiveStepsizeIntegrator integrator =
                        new DormandPrince853Integrator(period / 100, period * 100, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(10 * period);
        DSSTPropagator propagator = new DSSTPropagator(integrator, PropagationType.OSCULATING);
        OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                      Constants.WGS84_EARTH_FLATTENING,
                                                      FramesFactory.getGTOD(false));
        CelestialBody sun = CelestialBodyFactory.getSun();
        CelestialBody moon = CelestialBodyFactory.getMoon();
        propagator.addForceModel(new DSSTZonal(nshp, 4, 3, 9));
        propagator.addForceModel(new DSSTTesseral(earth.getBodyFrame(),
                                                  Constants.WGS84_EARTH_ANGULAR_VELOCITY,
                                                  nshp, 4, 4, 4, 8, 4, 4, 2));
        propagator.addForceModel(new DSSTThirdBody(sun, nshp.getMu()));
        propagator.addForceModel(new DSSTThirdBody(moon, nshp.getMu()));
        propagator.addForceModel(new DSSTAtmosphericDrag(new HarrisPriester(sun, earth), 2.1, 180, nshp.getMu()));
        propagator.addForceModel(new DSSTSolarRadiationPressure(1.2, 180, sun, earth, nshp.getMu()));

        final AbsoluteDate finalDate = orbit.getDate().shiftedBy(30 * Constants.JULIAN_DAY);
        propagator.resetInitialState(new SpacecraftState(orbit, 45.0));
        final SpacecraftState stateNoConfig = propagator.propagate(finalDate);
        Assertions.assertEquals(0, stateNoConfig.getAdditionalStatesValues().size());

        Assertions.assertNull(propagator.getSelectedCoefficients());
        propagator.setSelectedCoefficients(new HashSet<String>());
        Assertions.assertNotNull(propagator.getSelectedCoefficients());
        Assertions.assertTrue(propagator.getSelectedCoefficients().isEmpty());
        propagator.resetInitialState(new SpacecraftState(orbit, 45.0));
        final SpacecraftState stateConfigEmpty = propagator.propagate(finalDate);
        Assertions.assertEquals(234, stateConfigEmpty.getAdditionalStatesValues().size());

        final Set<String> selected = new HashSet<String>();
        selected.add("DSST-3rd-body-Moon-s[7]");
        selected.add("DSST-central-body-tesseral-c[-2][3]");
        propagator.setSelectedCoefficients(selected);
        Assertions.assertEquals(2, propagator.getSelectedCoefficients().size());
        propagator.resetInitialState(new SpacecraftState(orbit, 45.0));
        final SpacecraftState stateConfigeSelected = propagator.propagate(finalDate);
        Assertions.assertEquals(selected.size(), stateConfigeSelected.getAdditionalStatesValues().size());

        propagator.setSelectedCoefficients(null);
        propagator.resetInitialState(new SpacecraftState(orbit, 45.0));
        final SpacecraftState stateConfigNull = propagator.propagate(finalDate);
        Assertions.assertEquals(0, stateConfigNull.getAdditionalStatesValues().size());

    }

    @Test
    public void testIssueMeanInclination() {

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
        final Orbit meanOrb = DSSTPropagator.computeMeanState(ss, null, forces).getOrbit();
        Assertions.assertEquals(0.0164196, FastMath.toDegrees(orb.getI() - meanOrb.getI()), 1.0e-7);
    }

    @Test
    public void testIssue257() {
        final SpacecraftState meanState = getGEOState();

        // Third Bodies Force Model (Moon + Sun)
        final DSSTForceModel moon = new DSSTThirdBody(CelestialBodyFactory.getMoon(), meanState.getMu());
        final DSSTForceModel sun  = new DSSTThirdBody(CelestialBodyFactory.getSun(), meanState.getMu());

        final Collection<DSSTForceModel> forces = new ArrayList<DSSTForceModel>();
        forces.add(moon);
        forces.add(sun);

        final SpacecraftState osculatingState = DSSTPropagator.computeOsculatingState(meanState, null, forces);
        Assertions.assertEquals(734.3,
                                Vector3D.distance(meanState.getPosition(),
                                                  osculatingState.getPosition()),
                                1.0);

        final SpacecraftState computedMeanState = DSSTPropagator.computeMeanState(osculatingState, null, forces);
        Assertions.assertEquals(734.3,
                                Vector3D.distance(osculatingState.getPosition(),
                                                  computedMeanState.getPosition()),
                                1.0);

        Assertions.assertEquals(0.0,
                                Vector3D.distance(computedMeanState.getPosition(),
                                                  meanState.getPosition()),
                                5.0e-6);

    }

    @Test
    public void testIssue339() {

        final SpacecraftState osculatingState = getLEOState();

        final CelestialBody    sun   = CelestialBodyFactory.getSun();
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            Constants.WGS84_EARTH_FLATTENING,
                                                            FramesFactory.getITRF(IERSConventions.IERS_2010,
                                                                                  true));
        final BoxAndSolarArraySpacecraft boxAndWing = new BoxAndSolarArraySpacecraft(5.0, 2.0, 2.0,
                                                                                     sun,
                                                                                     50.0, Vector3D.PLUS_J,
                                                                                     2.0, 0.1,
                                                                                     0.2, 0.6);
        final Atmosphere atmosphere = new HarrisPriester(CelestialBodyFactory.getSun(), earth, 6);
        final AttitudeProvider attitudeProvider = new LofOffset(osculatingState.getFrame(),
                                                                LOFType.LVLH_CCSDS, RotationOrder.XYZ,
                                                                0.0, 0.0, 0.0);

        // Surface force models that require an attitude provider
        final Collection<DSSTForceModel> forces = new ArrayList<DSSTForceModel>();
        forces.add(new DSSTSolarRadiationPressure(sun, earth, boxAndWing, osculatingState.getMu()));
        forces.add(new DSSTAtmosphericDrag(atmosphere, boxAndWing, osculatingState.getMu()));

        final SpacecraftState meanState = DSSTPropagator.computeMeanState(osculatingState, attitudeProvider, forces);
        Assertions.assertEquals(0.522,
                                Vector3D.distance(osculatingState.getPosition(),
                                                  meanState.getPosition()),
                                0.001);

        final SpacecraftState computedOsculatingState = DSSTPropagator.computeOsculatingState(meanState, attitudeProvider, forces);
        Assertions.assertEquals(0.0,
                                Vector3D.distance(osculatingState.getPosition(),
                                                  computedOsculatingState.getPosition()),
                                5.0e-6);

    }

    @Test
    public void testIssue613() {
        // Spacecraft state
        final SpacecraftState state = getLEOState();

        // Body frame
        final Frame itrf = FramesFactory .getITRF(IERSConventions.IERS_2010, true);

        // Earth
        final UnnormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getUnnormalizedProvider(4, 4);
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            Constants.WGS84_EARTH_FLATTENING, itrf);
        // Detectors
        final List<EventDetector> events = new ArrayList<>();
        events.add(new AltitudeDetector(85.5, earth));
        events.add(new LatitudeCrossingDetector(earth, 0.0));

        // Force models
        final List<DSSTForceModel> forceModels = new ArrayList<>();
        forceModels.add(new DSSTZonal(provider));
        forceModels.add(new DSSTTesseral(itrf, Constants.WGS84_EARTH_ANGULAR_VELOCITY, provider));
        forceModels.add(new DSSTThirdBody(CelestialBodyFactory.getMoon(), provider.getMu()));
        forceModels.add(new DSSTThirdBody(CelestialBodyFactory.getSun(), provider.getMu()));

        // Set up DSST propagator
        final double[][] tol = DSSTPropagator.tolerances(10.0, state.getOrbit());
        final ODEIntegrator integrator = new DormandPrince54Integrator(60.0, 3600.0, tol[0], tol[1]);
        final DSSTPropagator propagator = new DSSTPropagator(integrator, PropagationType.OSCULATING);
        for (DSSTForceModel force : forceModels) {
            propagator.addForceModel(force);
        }
        for (EventDetector event : events) {
            propagator.addEventDetector(event);
        }
        propagator.setInitialState(state);

        // Propagation
        final SpacecraftState finalState = propagator.propagate(state.getDate().shiftedBy(86400.0));

        // Verify is the propagation is correctly performed
        Assertions.assertEquals(finalState.getMu(), 3.986004415E14, Double.MIN_VALUE);
    }

    @Test
    public void testIssue339WithAccelerations() {
        final SpacecraftState osculatingState = getLEOStatePropagatedBy30Minutes();
        final CelestialBody sun = CelestialBodyFactory.getSun();
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING,
                                                            FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        final BoxAndSolarArraySpacecraft boxAndWing = new BoxAndSolarArraySpacecraft(5.0, 2.0, 2.0, sun, 50.0, Vector3D.PLUS_J, 2.0, 0.1, 0.2, 0.6);
        final Atmosphere atmosphere = new HarrisPriester(CelestialBodyFactory.getSun(), earth, 6);
        final AttitudeProvider attitudeProvider = new LofOffset(osculatingState.getFrame(), LOFType.LVLH_CCSDS, RotationOrder.XYZ, 0.0, 0.0, 0.0);
        // Surface force models that require an attitude provider
        final Collection<DSSTForceModel> forces = new ArrayList<DSSTForceModel>();
        forces.add(new DSSTAtmosphericDrag(atmosphere, boxAndWing, osculatingState.getMu()));
        final SpacecraftState meanState = DSSTPropagator.computeMeanState(osculatingState, attitudeProvider, forces);
        final SpacecraftState computedOsculatingState = DSSTPropagator.computeOsculatingState(meanState, attitudeProvider, forces);
        Assertions.assertEquals(0.0,
                                Vector3D.distance(osculatingState.getPosition(), computedOsculatingState.getPosition()),
                                5.0e-6);
    }

    @Test
    public void testIssue704() {

        // Coordinates
        final Orbit         orbit = getLEOState().getOrbit();
        final PVCoordinates pv    = orbit.getPVCoordinates();

        // dP
        final double dP = 10.0;

        // Computes dV
        final double r2 = pv.getPosition().getNormSq();
        final double v  = pv.getVelocity().getNorm();
        final double dV = orbit.getMu() * dP / (v * r2);

        // Verify
        final double[][] tol1 = DSSTPropagator.tolerances(dP, orbit);
        final double[][] tol2 = DSSTPropagator.tolerances(dP, dV, orbit);
        for (int i = 0; i < tol1.length; i++) {
            Assertions.assertArrayEquals(tol1[i], tol2[i], Double.MIN_VALUE);
        }

    }

    /** This test is based on the example given by Orekit user kris06 in https://gitlab.orekit.org/orekit/orekit/-/issues/670. */
    @Test
    public void testIssue670() {

        final NumericalForce force     = new NumericalForce();
        final DSSTForce      dsstForce = new DSSTForce(force, Constants.WGS84_EARTH_MU);

        SpacecraftState state = getLEOState();
        setDSSTProp(state);
        dsstProp.addForceModel(dsstForce);

        // Verify flag are false
        Assertions.assertFalse(force.initialized);
        Assertions.assertFalse(force.accComputed);

        // Propagation of the initial state at t + dt
        final double dt = 3200.;
        final AbsoluteDate target = state.getDate().shiftedBy(dt);

        dsstProp.propagate(target);

        // Flag must be true
        Assertions.assertTrue(force.initialized);
        Assertions.assertTrue(force.accComputed);

    }

    /** Test issue 672:
     * DSST Propagator was crashing with tesseral harmonics of the gravity field
     * when the order is lower or equal to 3.
     */
    @Test
    public void testIssue672() {

        // GIVEN
        // -----

        // Test with a central Body geopotential of 3x3
        final UnnormalizedSphericalHarmonicsProvider provider =
                        GravityFieldFactory.getUnnormalizedProvider(3, 3);
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
                                               PositionAngleType.MEAN,
                                               FramesFactory.getEME2000(),
                                               initDate,
                                               provider.getMu());

        // Set propagator with state and force model
        final SpacecraftState initialState = new SpacecraftState(orbit);
        final double minStep = initialState.getKeplerianPeriod();
        final double maxStep = 100. * minStep;
        final double[][] tol = DSSTPropagator.tolerances(1.0, initialState.getOrbit());
        AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(minStep, maxStep, tol[0], tol[1]);
        final DSSTPropagator propagator = new DSSTPropagator(integrator, PropagationType.OSCULATING);
        propagator.setInitialState(initialState, PropagationType.MEAN);
        propagator.addForceModel(new DSSTZonal(provider));
        propagator.addForceModel(new DSSTTesseral(earthFrame,
                                                  Constants.WGS84_EARTH_ANGULAR_VELOCITY, provider));

        // WHEN
        // ----

        // 1 day propagation
        final SpacecraftState state = propagator.propagate(initDate.shiftedBy(1. * 86400.));

        // THEN
        // -----

        // Verify that no exception occurred
        Assertions.assertNotNull(state);
    }

    /**
     * Check that the DSST can include the derivatives of force model parameters in the
     * Jacobian. Uses a very basic force model for a quick test. Checks both mean and
     * osculating as well as zero, one, or two force model parameters.
     */
    @Test
    public void testJacobianWrtParametersIssue986() {
        // setup - all
        Frame eci = FramesFactory.getGCRF();
        AbsoluteDate epoch = AbsoluteDate.ARBITRARY_EPOCH;
        UnnormalizedSphericalHarmonicsProvider harmonics =
                GravityFieldFactory.getUnnormalizedProvider(4, 4);
        double mu = harmonics.getMu();
        // semi-major axis
        double a = Constants.WGS84_EARTH_EQUATORIAL_RADIUS + 500e3;
        // Short period derivatives are NaN when ecc is zero.
        // Due to FieldEquinoctialOrbit.getE() = sqrt(ex**2 + ey**2)
        // When ex = 0 and ey = 0 then e = 0 and the derivative is infinite,
        // but when composed with zero derivatives the result is NaN.
        // so use some small, ignorable e.
        double e = 1e-17;
        Orbit orbit = new EquinoctialOrbit(new KeplerianOrbit(
                a, e, 1, 0, 0, 0,
                PositionAngleType.MEAN, eci, epoch, mu));
        final SpacecraftState initialState = new SpacecraftState(orbit);
        final double[][] tols = DSSTPropagator.tolerances(1, orbit);
        ODEIntegrator integrator = new DormandPrince853Integrator(
                100 * 60,
                Constants.JULIAN_DAY,
                tols[0],
                tols[1]);
        final double dt = 2 * orbit.getKeplerianPeriod();
        AbsoluteDate end = epoch.shiftedBy(dt);
        final double n = orbit.getKeplerianMeanMotion();
        RealMatrix expectedStm = MatrixUtils.createRealIdentityMatrix(6);
        // Montenbruck & Gill Eq. 7.11
        final double dmda = -3.0 * n * dt / (2 * a);
        expectedStm.setEntry(5, 0, dmda);
        int ulps = 5;
        double absTol = 1e-18;
        final double[] expectedStmMdot = {0, 0, 0, 0, 0, dt};
        final Matcher<double[]> stmMdotMatcherExact = OrekitMatchers
                .doubleArrayContaining(expectedStmMdot, ulps);
        final Matcher<double[]> stmMdotMatcherClose = OrekitMatchers
                .doubleArrayContaining(expectedStmMdot, 1e-30, 15);
        // d (M = n * dt) /d (mu)
        final double dmdmu = dt / (2 * Math.sqrt(mu * a * a * a));
        final Matcher<double[]> stmMuMatcherClose = OrekitMatchers
                .doubleArrayContaining(new double[]{0, 0, 0, 0, 0, dmdmu}, 1e-27, 15);
        // Not sure why adding a paramter changes the STM, but it does
        double twoParameterAbsTol = 1e-12;
        // larger tolerance for osculating comparisons because I'm treating
        // the osculating variations as error. I.e. I do not check the correctness of the
        // osculating terms in the derivatives. I assume other tests do that.
        double osculatingAbsTol = 1e-12;

        // setup - mean no selected parameter
        DSSTPropagator dsst = new DSSTPropagator(integrator, PropagationType.MEAN);
        dsst.setInitialState(initialState, PropagationType.MEAN);
        // an intentionally negligible force model, but with a different parameter
        dsst.addForceModel(new DSSTThirdBody(CelestialBodyFactory.getPluto(), mu));

        // action - mean no selected parameter
        DSSTHarvester harvester = dsst.setupMatricesComputation("stm", null, null);
        SpacecraftState state = dsst.propagate(end);
        RealMatrix stm = harvester.getStateTransitionMatrix(state);
        RealMatrix stmParameters = harvester.getParametersJacobian(state);

        // verify - mean no selected parameter
        MatcherAssert.assertThat(harvester.getOrbitType(), is(OrbitType.EQUINOCTIAL));
        MatcherAssert.assertThat(
                harvester.getPositionAngleType(),
                is(PositionAngleType.MEAN));
        MatcherAssert.assertThat(stmParameters, nullValue());
        MatcherAssert.assertThat(stm, OrekitMatchers.matrixCloseTo(expectedStm, absTol));

        // setup - mean with parameter
        dsst = new DSSTPropagator(integrator, PropagationType.MEAN);
        dsst.setInitialState(initialState, PropagationType.MEAN);
        MDot mDot = new MDot();
        mDot.getParametersDrivers().get(0).setSelected(true);
        dsst.addForceModel(mDot);
        // an intentionally negligible force model, but with a different parameter
        dsst.addForceModel(new DSSTThirdBody(CelestialBodyFactory.getPluto(), mu));

        // action - mean with parameter
        harvester = dsst.setupMatricesComputation("stm", null, null);
        state = dsst.propagate(end);
        stm = harvester.getStateTransitionMatrix(state);
        stmParameters = harvester.getParametersJacobian(state);

        // verify - mean with parameter
        MatcherAssert.assertThat(harvester.getOrbitType(), is(OrbitType.EQUINOCTIAL));
        MatcherAssert.assertThat(
                harvester.getPositionAngleType(),
                is(PositionAngleType.MEAN));
        MatcherAssert.assertThat(stm, OrekitMatchers.matrixCloseTo(expectedStm, absTol));
        MatcherAssert.assertThat(harvester.getJacobiansColumnsNames(), contains("MDot"));
        MatcherAssert.assertThat(stmParameters.getColumn(0), stmMdotMatcherExact);
        MatcherAssert.assertThat(stmParameters.getColumnDimension(), is(1));
        MatcherAssert.assertThat(stmParameters.getRowDimension(), is(6));

        // setup - mean with two parameters
        dsst = new DSSTPropagator(integrator, PropagationType.MEAN);
        dsst.setInitialState(initialState, PropagationType.MEAN);
        mDot = new MDot();
        mDot.getParametersDrivers().get(0).setSelected(true);
        dsst.addForceModel(mDot);
        // an intentionally negligible force model, but with a different parameter
        DSSTThirdBody third = new DSSTThirdBody(CelestialBodyFactory.getPluto(), mu);
        third.getParametersDrivers().get(1).setSelected(true);
        dsst.addForceModel(third);

        // action - mean with two parameters
        harvester = dsst.setupMatricesComputation("stm", null, null);
        state = dsst.propagate(end);
        stm = harvester.getStateTransitionMatrix(state);
        stmParameters = harvester.getParametersJacobian(state);

        // verify - mean with two parameters
        MatcherAssert.assertThat(harvester.getOrbitType(), is(OrbitType.EQUINOCTIAL));
        MatcherAssert.assertThat(
                harvester.getPositionAngleType(),
                is(PositionAngleType.MEAN));
        MatcherAssert.assertThat(stm,
                OrekitMatchers.matrixCloseTo(expectedStm, twoParameterAbsTol));
        // "Spancentral" seems like an odd name, but that's what the code uses.
        MatcherAssert.assertThat(harvester.getJacobiansColumnsNames(),
                contains("MDot", "Spancentral attraction coefficient0"));
        MatcherAssert.assertThat(stmParameters.getColumn(0), stmMdotMatcherClose);
        MatcherAssert.assertThat(stmParameters.getColumn(1), stmMuMatcherClose);
        MatcherAssert.assertThat(stmParameters.getColumnDimension(), is(2));
        MatcherAssert.assertThat(stmParameters.getRowDimension(), is(6));

        // setup - osculating no selected parameter
        dsst = new DSSTPropagator(integrator, PropagationType.OSCULATING);
        dsst.setInitialState(initialState, PropagationType.MEAN);
        // an intentionally negligible force model, but with a different parameter
        dsst.addForceModel(new DSSTThirdBody(CelestialBodyFactory.getPluto(), mu));

        // action - osculating no selected parameter
        harvester = dsst.setupMatricesComputation("stm", null, null);
        harvester.initializeFieldShortPeriodTerms(initialState);
        harvester.updateFieldShortPeriodTerms(initialState);
        harvester.setReferenceState(initialState);
        state = dsst.propagate(end);
        stm = harvester.getStateTransitionMatrix(state);
        stmParameters = harvester.getParametersJacobian(state);

        // verify - osculating no selected parameter
        MatcherAssert.assertThat(harvester.getOrbitType(), is(OrbitType.EQUINOCTIAL));
        MatcherAssert.assertThat(harvester.getPositionAngleType(), is(PositionAngleType.MEAN));
        MatcherAssert.assertThat(stmParameters, nullValue());
        MatcherAssert.assertThat(stm,
                OrekitMatchers.matrixCloseTo(expectedStm, osculatingAbsTol));

        // setup - osculating with parameter
        dsst = new DSSTPropagator(integrator, PropagationType.OSCULATING);
        dsst.setInitialState(initialState, PropagationType.MEAN);
        mDot = new MDot();
        mDot.getParametersDrivers().get(0).setSelected(true);
        dsst.addForceModel(mDot);
        // an intentionally negligible force model, but with a different parameter
        dsst.addForceModel(new DSSTThirdBody(CelestialBodyFactory.getPluto(), mu));

        // action - osculating with parameter
        harvester = dsst.setupMatricesComputation("stm", null, null);
        harvester.initializeFieldShortPeriodTerms(initialState);
        harvester.updateFieldShortPeriodTerms(initialState);
        harvester.setReferenceState(initialState);
        state = dsst.propagate(end);
        stm = harvester.getStateTransitionMatrix(state);
        stmParameters = harvester.getParametersJacobian(state);

        // verify - osculating with parameter
        MatcherAssert.assertThat(harvester.getOrbitType(), is(OrbitType.EQUINOCTIAL));
        MatcherAssert.assertThat(harvester.getPositionAngleType(), is(PositionAngleType.MEAN));
        MatcherAssert.assertThat(stm,
                OrekitMatchers.matrixCloseTo(expectedStm, osculatingAbsTol));
        MatcherAssert.assertThat(harvester.getJacobiansColumnsNames(), contains("MDot"));
        MatcherAssert.assertThat(stmParameters.getColumn(0), stmMdotMatcherExact);
        MatcherAssert.assertThat(stmParameters.getColumnDimension(), is(1));
        MatcherAssert.assertThat(stmParameters.getRowDimension(), is(6));

        // setup - osculating with two parameters
        dsst = new DSSTPropagator(integrator, PropagationType.OSCULATING);
        dsst.setInitialState(initialState, PropagationType.MEAN);
        mDot = new MDot();
        mDot.getParametersDrivers().get(0).setSelected(true);
        dsst.addForceModel(mDot);
        // an intentionally negligible force model, but with a different parameter
        third = new DSSTThirdBody(CelestialBodyFactory.getPluto(), mu);
        third.getParametersDrivers().get(1).setSelected(true);
        dsst.addForceModel(third);

        // action - osculating with two parameters
        harvester = dsst.setupMatricesComputation("stm", null, null);
        harvester.initializeFieldShortPeriodTerms(initialState);
        harvester.updateFieldShortPeriodTerms(initialState);
        harvester.setReferenceState(initialState);
        state = dsst.propagate(end);
        stm = harvester.getStateTransitionMatrix(state);
        stmParameters = harvester.getParametersJacobian(state);

        // verify - osculating with two parameters
        MatcherAssert.assertThat(harvester.getOrbitType(), is(OrbitType.EQUINOCTIAL));
        MatcherAssert.assertThat(
                harvester.getPositionAngleType(),
                is(PositionAngleType.MEAN));
        MatcherAssert.assertThat(stm,
                OrekitMatchers.matrixCloseTo(expectedStm, twoParameterAbsTol));
        // "Spancentral" seems like an odd name, but that's what the code uses.
        MatcherAssert.assertThat(harvester.getJacobiansColumnsNames(),
                contains("MDot", "Spancentral attraction coefficient0"));
        MatcherAssert.assertThat(stmParameters.getColumn(0), stmMdotMatcherClose);
        MatcherAssert.assertThat(stmParameters.getColumn(1), stmMuMatcherClose);
        MatcherAssert.assertThat(stmParameters.getColumnDimension(), is(2));
        MatcherAssert.assertThat(stmParameters.getRowDimension(), is(6));
    }

    /** Attempt at the bare minimum force model with a parameter. */
    private static class MDot implements DSSTForceModel {

        /** Mean Anomaly Rate */
        private final ParameterDriver mDot = new ParameterDriver(
                "MDot",
                // this seems to be needed to ensure the name is "MDot"
                new TimeSpanMap<>("MDot"),
                new TimeSpanMap<>(0.0),
                0,
                1,
                Double.NEGATIVE_INFINITY,
                Double.POSITIVE_INFINITY);

        @Override
        public List<ParameterDriver> getParametersDrivers() {
            return Collections.singletonList(mDot);
        }

        @Override
        public double[] getMeanElementRate(SpacecraftState state,
                                           AuxiliaryElements auxiliaryElements,
                                           double[] parameters) {
            return new double[]{0, 0, 0, 0, 0, parameters[0]};
        }

        @Override
        public <T extends CalculusFieldElement<T>> T[] getMeanElementRate(
                FieldSpacecraftState<T> state,
                 FieldAuxiliaryElements<T> auxiliaryElements,
                  T[] parameters) {

            final Field<T> field = state.getA().getField();
            final T zero = field.getZero();
            final T[] rates = MathArrays.buildArray(field, 6);
            rates[0] = zero;
            rates[1] = zero;
            rates[2] = zero;
            rates[3] = zero;
            rates[4] = zero;
            rates[5] = parameters[0];
            return rates;
        }

        /* not used */

        @Override
        public List<ShortPeriodTerms> initializeShortPeriodTerms(
                AuxiliaryElements auxiliaryElements,
                PropagationType type,
                double[] parameters) {
            return Collections.emptyList();
        }

        @Override
        public <T extends CalculusFieldElement<T>> List<FieldShortPeriodTerms<T>>
        initializeShortPeriodTerms(FieldAuxiliaryElements<T> auxiliaryElements,
                                   PropagationType type,
                                   T[] parameters) {
            return Collections.emptyList();
        }

        @Override
        public void registerAttitudeProvider(AttitudeProvider provider) {
        }

        @Override
        public void updateShortPeriodTerms(double[] parameters,
                                           SpacecraftState... meanStates) {
        }

        @Override
        public <T extends CalculusFieldElement<T>> void updateShortPeriodTerms(
                T[] parameters, FieldSpacecraftState<T>... meanStates) {
        }

    }

    private SpacecraftState getGEOState() throws IllegalArgumentException, OrekitException {
        // No shadow at this date
        final AbsoluteDate initDate = new AbsoluteDate(new DateComponents(2003, 05, 21), new TimeComponents(1, 0, 0.),
                                                       TimeScalesFactory.getUTC());
        final Orbit orbit = new EquinoctialOrbit(42164000,
                                                 10e-3,
                                                 10e-3,
                                                 FastMath.tan(0.001745329) * FastMath.cos(2 * FastMath.PI / 3),
                                                 FastMath.tan(0.001745329) * FastMath.sin(2 * FastMath.PI / 3), 0.1,
                                                 PositionAngleType.TRUE,
                                                 FramesFactory.getEME2000(),
                                                 initDate,
                                                 3.986004415E14);
        return new SpacecraftState(orbit);
    }

    private SpacecraftState getLEOState() throws IllegalArgumentException, OrekitException {
        final Vector3D position = new Vector3D(-6142438.668, 3492467.560, -25767.25680);
        final Vector3D velocity = new Vector3D(505.8479685, 942.7809215, 7435.922231);
        // Spring equinoxe 21st mars 2003 1h00m
        final AbsoluteDate initDate = new AbsoluteDate(new DateComponents(2003, 03, 21), new TimeComponents(1, 0, 0.), TimeScalesFactory.getUTC());
        return new SpacecraftState(new EquinoctialOrbit(new PVCoordinates(position, velocity),
                                                        FramesFactory.getEME2000(),
                                                        initDate,
                                                        3.986004415E14));
    }

    private void setDSSTProp(SpacecraftState initialState) {
        initialState.getDate();
        final double minStep = initialState.getKeplerianPeriod();
        final double maxStep = 100. * minStep;
        final double[][] tol = DSSTPropagator.tolerances(1.0, initialState.getOrbit());
        AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(minStep, maxStep, tol[0], tol[1]);
        dsstProp = new DSSTPropagator(integrator);
        dsstProp.setInitialState(initialState, PropagationType.MEAN);
    }

    private static class CheckingHandler implements EventHandler {

        private final Action actionOnEvent;
        private boolean gotHere;

        public CheckingHandler(final Action actionOnEvent) {
            this.actionOnEvent = actionOnEvent;
            this.gotHere       = false;
        }

        public void assertEvent(boolean expected) {
            Assertions.assertEquals(expected, gotHere);
        }

        @Override
        public Action eventOccurred(SpacecraftState s, EventDetector detector, boolean increasing) {
            gotHere = true;
            return actionOnEvent;
        }

    }

    /** This class is based on the example given by Orekit user kris06 in https://gitlab.orekit.org/orekit/orekit/-/issues/670. */
    private class DSSTForce extends AbstractGaussianContribution {

        DSSTForce(ForceModel contribution, double mu) {
            super("DSST mock -", 6.0e-10, contribution, mu);
        }

        /** {@inheritDoc} */
        @Override
        protected List<ParameterDriver> getParametersDriversWithoutMu() {
            return Collections.emptyList();
        }

        /** {@inheritDoc} */
        @Override
        protected double[] getLLimits(SpacecraftState state,
                                      AuxiliaryElements auxiliaryElements) {
            return new double[] { -FastMath.PI + MathUtils.normalizeAngle(state.getLv(), 0),
                FastMath.PI + MathUtils.normalizeAngle(state.getLv(), 0) };
        }

        /** {@inheritDoc} */
        @Override
        protected <T extends CalculusFieldElement<T>> T[] getLLimits(FieldSpacecraftState<T> state,
                                                                     FieldAuxiliaryElements<T> auxiliaryElements) {
            final Field<T> field = state.getDate().getField();
            final T zero = field.getZero();
            final T[] tab = MathArrays.buildArray(field, 2);
            tab[0] = MathUtils.normalizeAngle(state.getLv(), zero).subtract(FastMath.PI);
            tab[1] = MathUtils.normalizeAngle(state.getLv(), zero).add(FastMath.PI);
            return tab;
        }

    }

    /** This class is based on the example given by Orekit user kris06 in https://gitlab.orekit.org/orekit/orekit/-/issues/670. */
    private class NumericalForce implements ForceModel {

        private boolean initialized;
        private boolean accComputed;

        NumericalForce() {
            this.initialized = false;
        }

        /** {@inheritDoc} */
        @Override
        public void init(final SpacecraftState s0, final AbsoluteDate t) {
            this.initialized = true;
            this.accComputed = false;
        }

        /** {@inheritDoc} */
        @Override
        public boolean dependsOnPositionOnly() {
            return false;
        }

        /** {@inheritDoc} */
        @Override
        public Vector3D acceleration(SpacecraftState s, double[] parameters) {
            this.accComputed = true;
            return Vector3D.ZERO;
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> FieldVector3D<T> acceleration(FieldSpacecraftState<T> s, T[] parameters) {
            return FieldVector3D.getZero(s.getDate().getField());
        }

        /** {@inheritDoc} */
        @Override
        public Stream<EventDetector> getEventDetectors() {
            return Stream.empty();
        }

        /** {@inheritDoc} */
        @Override
        public <T extends CalculusFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventDetectors(final Field<T> field) {
            return Stream.empty();
        }


        @Override
        public List<ParameterDriver> getParametersDrivers() {
            return Collections.emptyList();
        }

    }

    @BeforeEach
    public void setUp() throws IOException, ParseException {
        Utils.setDataRoot("regular-data:potential/shm-format");
    }

    @AfterEach
    public void tearDown() {
        dsstProp = null;
    }

    private SpacecraftState getLEOStatePropagatedBy30Minutes() throws IllegalArgumentException, OrekitException {

        final Vector3D position = new Vector3D(-6142438.668, 3492467.560, -25767.25680);
        final Vector3D velocity = new Vector3D(505.8479685, 942.7809215, 7435.922231);
        // Spring equinoxe 21st mars 2003 1h00m
        final AbsoluteDate initialDate = new AbsoluteDate(new DateComponents(2003, 03, 21), new TimeComponents(1, 0, 0.), TimeScalesFactory.getUTC());
        final CartesianOrbit osculatingOrbit = new CartesianOrbit(new PVCoordinates(position, velocity), FramesFactory.getTOD(IERSConventions.IERS_1996, false),
                                                                  initialDate, Constants.WGS84_EARTH_MU);
        // Adaptive step integrator
        // with a minimum step of 0.001 and a maximum step of 1000
        double minStep = 0.001;
        double maxstep = 1000.0;
        double positionTolerance = 10.0;
        OrbitType propagationType = OrbitType.EQUINOCTIAL;
        double[][] tolerances = NumericalPropagator.tolerances(positionTolerance, osculatingOrbit, propagationType);
        AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(minStep, maxstep, tolerances[0], tolerances[1]);
        NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.setOrbitType(propagationType);

        NormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getNormalizedProvider(5, 5);
        ForceModel holmesFeatherstone = new HolmesFeatherstoneAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010, true), provider);
        propagator.addForceModel(holmesFeatherstone);
        propagator.setInitialState(new SpacecraftState(osculatingOrbit));

        return propagator.propagate(new AbsoluteDate(initialDate, 1800.));
    }

}
