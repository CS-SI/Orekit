/* Copyright 2002-2025 CS GROUP
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

package org.orekit.propagation.analytical;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.stat.descriptive.StorelessUnivariateStatistic;
import org.hipparchus.stat.descriptive.rank.Max;
import org.hipparchus.stat.descriptive.rank.Min;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.forces.ForceModel;
import org.orekit.forces.drag.DragForce;
import org.orekit.forces.drag.IsotropicDrag;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.TideSystem;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider.UnnormalizedSphericalHarmonics;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.models.earth.atmosphere.DTM2000;
import org.orekit.models.earth.atmosphere.data.MarshallSolarActivityFutureEstimation;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.ToleranceProvider;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

public class BrouwerLyddanePropagatorTest {

    private static final AttitudeProvider DEFAULT_LAW = Utils.defaultLaw();

    @Test
    public void testIssue947ComputeMeanOrbit() {
        // Error case from gitlab issue#947: negative eccentricity when calculating mean orbit
        TLE tleOrbit = new TLE("1 43196U 18015E   21055.59816856  .00000894  00000-0  38966-4 0  9996",
                               "2 43196  97.4662 188.8169 0016935 299.6845  60.2706 15.24746686170319");
        Propagator propagator = TLEPropagator.selectExtrapolator(tleOrbit);

        //Get state at initial date and 3 days before
        SpacecraftState tleState = propagator.getInitialState();
        SpacecraftState tleStateAtDate = propagator.propagate(propagator.getInitialState().getDate().shiftedBy(3,  TimeUnit.DAYS));
            
        //BL mean orbit
        double epsilon = 1.0e-12;
        int maxIter = 500;
        UnnormalizedSphericalHarmonicsProvider provider = GravityFieldFactory.getUnnormalizedProvider(5, 0);

        Assertions.assertDoesNotThrow(() -> {
            BrouwerLyddanePropagator.computeMeanOrbit(tleState.getOrbit(), provider,
                                                      provider.onDate(tleState.getDate()),
                                                      BrouwerLyddanePropagator.M2,
                                                      epsilon, maxIter);
        });

        Assertions.assertDoesNotThrow(() -> {
            BrouwerLyddanePropagator.computeMeanOrbit(tleStateAtDate.getOrbit(), provider,
                                                      provider.onDate(tleStateAtDate.getDate()),
                                                      BrouwerLyddanePropagator.M2,
                                                      epsilon, maxIter);
        });
    }

    @Test
    public void testIssue947Propagate() {
        // Error case from forum thread (jbrunell): negative eccentricity when propagating
        final double x1  = -1772.619869591273 * 1000;
        final double y1  = -3908.6521138424428 * 1000;
        final double z1  =  5266.68093513367 * 1000;
        final double dx1 =  6.35969327821623 * 1000;
        final double dy1 = -4.165238186695803 * 1000;
        final double dz1 = -0.9458311825913897 * 1000;

        final Vector3D pos = new Vector3D(x1, y1, z1);
        final Vector3D vel = new Vector3D(dx1, dy1, dz1);
        final TimeScale tai = TimeScalesFactory.getTAI();
        final AbsoluteDate date = new AbsoluteDate(2023, 3, 1, 14, 6, 29.639584, tai);
        final CircularOrbit orbit = new CircularOrbit(new PVCoordinates(pos, vel),
                                                      FramesFactory.getGCRF(), date,
                                                      Constants.EGM96_EARTH_MU);

        final BrouwerLyddanePropagator blPropagator = new BrouwerLyddanePropagator(orbit, 1000.,
                Constants.IERS2010_EARTH_EQUATORIAL_RADIUS, Constants.IERS2010_EARTH_MU,
                Constants.EIGEN5C_EARTH_C20, Constants.EIGEN5C_EARTH_C30,
                Constants.EIGEN5C_EARTH_C40, Constants.EIGEN5C_EARTH_C50,
                BrouwerLyddanePropagator.M2);
        final List<SpacecraftState> states = new ArrayList<>();
        blPropagator.setStepHandler(10, states::add);
        final AbsoluteDate startDate = new AbsoluteDate(2023, 3, 1, 14, 6, 29.639584, tai);
        final AbsoluteDate endDate = new AbsoluteDate(2023, 3, 1, 15, 6, 29.639584, tai);
        Assertions.assertDoesNotThrow(() -> blPropagator.propagate(startDate, endDate));
    }

    @Test
    public void testIssue947Constructor() {
        // Error case from forum thread (nicksakva) : negative eccentricity when constructing
        final Frame eme2000 = FramesFactory.getEME2000();

        final AbsoluteDate t = new AbsoluteDate(2023,06,26,12,0,0.0,TimeScalesFactory.getUTC());
        final double x = 6313554.48504233;
        final double y = 2775620.8433687;
        final double z = -2111774.8221765;
        final double vx = 1575.31305905025;
        final double vy = 1786.43351611741;
        final double vz = 7042.05214468662;

        final PVCoordinates pv = new PVCoordinates(new Vector3D(x, y, z), new Vector3D(vx, vy, vz));
        final CartesianOrbit orb = new CartesianOrbit(pv, eme2000, t, Constants.EGM96_EARTH_MU);

        Assertions.assertDoesNotThrow(() -> {
            new BrouwerLyddanePropagator(orb, Constants.EGM96_EARTH_EQUATORIAL_RADIUS,
                                              Constants.EGM96_EARTH_MU,
                                              Constants.EGM96_EARTH_C20, Constants.EGM96_EARTH_C30,
                                              Constants.EGM96_EARTH_C40, Constants.EGM96_EARTH_C50,
                                              BrouwerLyddanePropagator.M2);
        });
    }

    @Test
    public void sameDateCartesian() {

        // Definition of initial conditions with position and velocity
        // ------------------------------------------------------------
        // e = 0.04152500499523033   and   i = 1.705015527659039

        AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        Vector3D position = new Vector3D(3220103., 69623., 6149822.);
        Vector3D velocity = new Vector3D(6414.7, -2006., -3180.);

        Orbit initialOrbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
                                                  FramesFactory.getEME2000(), initDate, provider.getMu());

        // Extrapolation at the initial date
        // ---------------------------------
        BrouwerLyddanePropagator extrapolator =
                new BrouwerLyddanePropagator(initialOrbit, GravityFieldFactory.getUnnormalizedProvider(provider), BrouwerLyddanePropagator.M2);
        SpacecraftState finalOrbit = extrapolator.propagate(initDate);

        // position, velocity and semi-major axis match almost perfectly
        Assertions.assertEquals(0.0,
                                Vector3D.distance(initialOrbit.getPosition(),
                                                  finalOrbit.getPosition()),
                                4.7e-7);
        Assertions.assertEquals(0.0,
                                Vector3D.distance(initialOrbit.getVelocity(),
                                                  finalOrbit.getVelocity()),
                                2.8e-10);
        Assertions.assertEquals(0.0, finalOrbit.getOrbit().getA() - initialOrbit.getA(), 1.3e-8);

    }

    @Test
    public void sameDateKeplerian() {

        // Definition of initial conditions with position and velocity
        // ------------------------------------------------------------
        AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        Orbit initialOrbit = new KeplerianOrbit(6767924.41, .0005,  1.7, 2.1, 2.9,
                6.2, PositionAngleType.TRUE,
                FramesFactory.getEME2000(), initDate, provider.getMu());

        BrouwerLyddanePropagator extrapolator =
                new BrouwerLyddanePropagator(initialOrbit, DEFAULT_LAW, GravityFieldFactory.getUnnormalizedProvider(provider), BrouwerLyddanePropagator.M2);

        SpacecraftState finalOrbit = extrapolator.propagate(initDate);

        // positions  velocity and semi major axis match perfectly
        Assertions.assertEquals(0.0,
                            Vector3D.distance(initialOrbit.getPosition(),
                                              finalOrbit.getPosition()),
                            4.0e-7);
        Assertions.assertEquals(0.0,
                            Vector3D.distance(initialOrbit.getVelocity(),
                                              finalOrbit.getVelocity()),
                            2.9e-10);
        Assertions.assertEquals(0.0, finalOrbit.getOrbit().getA() - initialOrbit.getA(), 3.8e-9);
    }


    @Test
    public void almostSphericalBody() {

        // Definition of initial conditions
        // ---------------------------------
        // with e around e = 1.4e-4 and i = 1.7 rad
        Vector3D position = new Vector3D(3220103., 69623., 8449822.);
        Vector3D velocity = new Vector3D(6414.7, -2006., -3180.);

        AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        Orbit initialOrbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
                                                  FramesFactory.getEME2000(), initDate, provider.getMu());

        // Initialisation to simulate a Keplerian extrapolation
        // To be noticed: in order to simulate a Keplerian extrapolation with the
        // analytical
        // extrapolator, one should put the zonal coefficients to 0. But due to
        // numerical pbs
        // one must put a non 0 value.
        UnnormalizedSphericalHarmonicsProvider kepProvider =
                GravityFieldFactory.getUnnormalizedProvider(6.378137e6, 3.9860047e14,
                                                            TideSystem.UNKNOWN,
                                                            new double[][] {
                                                                { 0 }, { 0 }, { 0.1e-10 }, { 0.1e-13 }, { 0.1e-13 }, { 0.1e-14 }, { 0.1e-14 }
                                                            }, new double[][] {
                                                                { 0 }, { 0 },  { 0 }, { 0 }, { 0 }, { 0 }, { 0 }
                                                            });

        // Extrapolators definitions
        // -------------------------
        BrouwerLyddanePropagator extrapolatorAna =
            new BrouwerLyddanePropagator(initialOrbit, 1000.0, kepProvider, BrouwerLyddanePropagator.M2);
        KeplerianPropagator extrapolatorKep = new KeplerianPropagator(initialOrbit);

        // Extrapolation at a final date different from initial date
        // ---------------------------------------------------------
        double delta_t = 100.0; // extrapolation duration in seconds
        AbsoluteDate extrapDate = initDate.shiftedBy(delta_t);

        Orbit finalOrbitAna = extrapolatorAna.propagate(extrapDate).getOrbit();
        Orbit finalOrbitKep = extrapolatorKep.propagate(extrapDate).getOrbit();

        Assertions.assertEquals(0.0, finalOrbitAna.getDate().durationFrom(extrapDate), 0.0);
        // comparison of each orbital parameters
        Assertions.assertEquals(finalOrbitAna.getA(), finalOrbitKep.getA(), 10
                     * Utils.epsilonTest * finalOrbitKep.getA());
        Assertions.assertEquals(finalOrbitAna.getEquinoctialEx(), finalOrbitKep.getEquinoctialEx(), Utils.epsilonE
                     * finalOrbitKep.getE());
        Assertions.assertEquals(finalOrbitAna.getEquinoctialEy(), finalOrbitKep.getEquinoctialEy(), Utils.epsilonE
                     * finalOrbitKep.getE());
        Assertions.assertEquals(MathUtils.normalizeAngle(finalOrbitAna.getHx(), finalOrbitKep.getHx()),
                     finalOrbitKep.getHx(), Utils.epsilonAngle
                     * FastMath.abs(finalOrbitKep.getI()));
        Assertions.assertEquals(MathUtils.normalizeAngle(finalOrbitAna.getHy(), finalOrbitKep.getHy()),
                     finalOrbitKep.getHy(), Utils.epsilonAngle
                     * FastMath.abs(finalOrbitKep.getI()));
        Assertions.assertEquals(MathUtils.normalizeAngle(finalOrbitAna.getLv(), finalOrbitKep.getLv()),
                     finalOrbitKep.getLv(), Utils.epsilonAngle
                     * FastMath.abs(finalOrbitKep.getLv()));
        Assertions.assertEquals(MathUtils.normalizeAngle(finalOrbitAna.getLE(), finalOrbitKep.getLE()),
                     finalOrbitKep.getLE(), Utils.epsilonAngle
                     * FastMath.abs(finalOrbitKep.getLE()));
        Assertions.assertEquals(MathUtils.normalizeAngle(finalOrbitAna.getLM(), finalOrbitKep.getLM()),
                     finalOrbitKep.getLM(), Utils.epsilonAngle
                     * FastMath.abs(finalOrbitKep.getLM()));
    }

    @Test
    public void compareToNumericalPropagation() {

        final Frame inertialFrame = FramesFactory.getEME2000();
        AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        double timeshift = 60000. ;

        // Initial orbit
        final double a = 24396159; // semi major axis in meters
        final double e = 0.01; // eccentricity
        final double i = FastMath.toRadians(7); // inclination
        final double omega = FastMath.toRadians(180); // perigee argument
        final double raan = FastMath.toRadians(261); // right ascention of ascending node
        final double lM = 0; // mean anomaly
        final Orbit initialOrbit = new KeplerianOrbit(a, e, i, omega, raan, lM, PositionAngleType.TRUE,
                                                      inertialFrame, initDate, provider.getMu());
        // Initial state definition
        final SpacecraftState initialState = new SpacecraftState(initialOrbit);

        //_______________________________________________________________________________________________
        // SET UP A REFERENCE NUMERICAL PROPAGATION
        //_______________________________________________________________________________________________

        // Adaptive step integrator with a minimum step of 0.001 and a maximum step of 1000
        final double minStep = 0.001;
        final double maxstep = 1000.0;
        final double positionTolerance = 10.0;
        final OrbitType propagationType = OrbitType.KEPLERIAN;
        final double[][] tolerances =
                ToleranceProvider.getDefaultToleranceProvider(positionTolerance).getTolerances(initialOrbit, propagationType);
        final AdaptiveStepsizeIntegrator integrator =
                new DormandPrince853Integrator(minStep, maxstep, tolerances[0], tolerances[1]);

        // Numerical Propagator
        final NumericalPropagator NumPropagator = new NumericalPropagator(integrator);
        NumPropagator.setOrbitType(propagationType);

        final ForceModel holmesFeatherstone =
                new HolmesFeatherstoneAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010, true), provider);
        NumPropagator.addForceModel(holmesFeatherstone);

        // Set up initial state in the propagator
        NumPropagator.setInitialState(initialState);

        // Extrapolate from the initial to the final date
        final SpacecraftState NumFinalState = NumPropagator.propagate(initDate.shiftedBy(timeshift));
        final KeplerianOrbit NumOrbit = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(NumFinalState.getOrbit());

        //_______________________________________________________________________________________________
        // SET UP A BROUWER LYDDANE PROPAGATION
        //_______________________________________________________________________________________________

        BrouwerLyddanePropagator BLextrapolator =
                new BrouwerLyddanePropagator(initialOrbit, GravityFieldFactory.getUnnormalizedProvider(provider), BrouwerLyddanePropagator.M2);

        SpacecraftState BLFinalState = BLextrapolator.propagate(initDate.shiftedBy(timeshift));
        final KeplerianOrbit BLOrbit = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(BLFinalState.getOrbit());

        Assertions.assertEquals(NumOrbit.getA(), BLOrbit.getA(), 0.175);
        Assertions.assertEquals(NumOrbit.getE(), BLOrbit.getE(), 3.2e-6);
        Assertions.assertEquals(NumOrbit.getI(), BLOrbit.getI(), 6.9e-8);
        Assertions.assertEquals(MathUtils.normalizeAngle(NumOrbit.getPerigeeArgument(), FastMath.PI),
                MathUtils.normalizeAngle(BLOrbit.getPerigeeArgument(), FastMath.PI), 0.0053);
        Assertions.assertEquals(MathUtils.normalizeAngle(NumOrbit.getRightAscensionOfAscendingNode(), FastMath.PI),
                MathUtils.normalizeAngle(BLOrbit.getRightAscensionOfAscendingNode(), FastMath.PI), 1.2e-6);
        Assertions.assertEquals(MathUtils.normalizeAngle(NumOrbit.getTrueAnomaly(), FastMath.PI),
                MathUtils.normalizeAngle(BLOrbit.getTrueAnomaly(), FastMath.PI), 0.0052);
    }

    @Test
    public void compareToNumericalPropagationWithDrag() {

        final Frame inertialFrame = FramesFactory.getEME2000();
        final TimeScale utc = TimeScalesFactory.getUTC();
        final AbsoluteDate initDate = new AbsoluteDate(2003, 1, 1, 00, 00, 00.000, utc);
        double timeshift = 60000. ;

        // Initial orbit
        final double a = Constants.WGS84_EARTH_EQUATORIAL_RADIUS + 400e3; // semi major axis in meters
        final double e = 0.01; // eccentricity
        final double i = FastMath.toRadians(7); // inclination
        final double omega = FastMath.toRadians(180); // perigee argument
        final double raan = FastMath.toRadians(261); // right ascention of ascending node
        final double lM = 0; // mean anomaly
        final Orbit initialOrbit = new KeplerianOrbit(a, e, i, omega, raan, lM, PositionAngleType.TRUE,
                                                      inertialFrame, initDate, provider.getMu());
        // Initial state definition
        final SpacecraftState initialState = new SpacecraftState(initialOrbit);

        //_______________________________________________________________________________________________
        // SET UP A REFERENCE NUMERICAL PROPAGATION
        //_______________________________________________________________________________________________

        // Adaptive step integrator with a minimum step of 0.001 and a maximum step of 1000
        final double minStep = 0.001;
        final double maxstep = 1000.0;
        final double positionTolerance = 10.0;
        final OrbitType propagationType = OrbitType.KEPLERIAN;
        final double[][] tolerances =
                ToleranceProvider.getDefaultToleranceProvider(positionTolerance).getTolerances(initialOrbit, propagationType);
        final AdaptiveStepsizeIntegrator integrator =
                new DormandPrince853Integrator(minStep, maxstep, tolerances[0], tolerances[1]);

        // Numerical Propagator
        final NumericalPropagator NumPropagator = new NumericalPropagator(integrator);
        NumPropagator.setOrbitType(propagationType);

        // Atmosphere
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING,
                                                            FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        MarshallSolarActivityFutureEstimation msafe =
                        new MarshallSolarActivityFutureEstimation("Jan2000F10-edited-data\\.txt",
                                                                  MarshallSolarActivityFutureEstimation.StrengthLevel.AVERAGE);
        DTM2000 atmosphere = new DTM2000(msafe, CelestialBodyFactory.getSun(), earth);

        // Force model
        final ForceModel holmesFeatherstone =
                new HolmesFeatherstoneAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010, true), provider);
        final ForceModel drag = new DragForce(atmosphere, new IsotropicDrag(1.0, 1.0));
        NumPropagator.addForceModel(holmesFeatherstone);
        NumPropagator.addForceModel(drag);

        // Set up initial state in the propagator
        NumPropagator.setInitialState(initialState);

        // Extrapolate from the initial to the final date
        final SpacecraftState NumFinalState = NumPropagator.propagate(initDate.shiftedBy(timeshift));
        final KeplerianOrbit NumOrbit = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(NumFinalState.getOrbit());

        //_______________________________________________________________________________________________
        // SET UP A BROUWER LYDDANE PROPAGATION WITHOUT DRAG
        //_______________________________________________________________________________________________

        BrouwerLyddanePropagator BLextrapolator =
                        new BrouwerLyddanePropagator(initialOrbit, GravityFieldFactory.getUnnormalizedProvider(provider), BrouwerLyddanePropagator.M2);

        SpacecraftState BLFinalState = BLextrapolator.propagate(initDate.shiftedBy(timeshift));
        KeplerianOrbit BLOrbit = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(BLFinalState.getOrbit());

        // Verify a and e differences without the drag effect on Brouwer-Lyddane
        final double deltaSmaBefore = 15.81;
        final double deltaEccBefore = 9.2681e-5;
        Assertions.assertEquals(NumOrbit.getA(), BLOrbit.getA(), deltaSmaBefore);
        Assertions.assertEquals(NumOrbit.getE(), BLOrbit.getE(), deltaEccBefore);

        //_______________________________________________________________________________________________
        // SET UP A BROUWER LYDDANE PROPAGATION WITH DRAG
        //_______________________________________________________________________________________________

        double M2 = 1.0e-14;
        BLextrapolator = new BrouwerLyddanePropagator(initialOrbit, GravityFieldFactory.getUnnormalizedProvider(provider), M2);
        BLFinalState = BLextrapolator.propagate(initDate.shiftedBy(timeshift));
        BLOrbit = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(BLFinalState.getOrbit());

        // Verify a and e differences without the drag effect on Brouwer-Lyddane
        final double deltaSmaAfter = 11.04;
        final double deltaEccAfter = 9.2639e-5;
        Assertions.assertEquals(NumOrbit.getA(), BLOrbit.getA(), deltaSmaAfter);
        Assertions.assertEquals(NumOrbit.getE(), BLOrbit.getE(), deltaEccAfter);

        Assertions.assertTrue(deltaSmaAfter < deltaSmaBefore);
        Assertions.assertTrue(deltaEccAfter < deltaEccBefore);
    }


    @Test
    public void compareToNumericalPropagationMeanInitialOrbit() {

        final Frame inertialFrame = FramesFactory.getEME2000();
        AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        double timeshift = 60000. ;

        // Initial orbit
        final double a = 24396159; // semi major axis in meters
        final double e = 0.01; // eccentricity
        final double i = FastMath.toRadians(7); // inclination
        final double omega = FastMath.toRadians(180); // perigee argument
        final double raan = FastMath.toRadians(261); // right ascention of ascending node
        final double lM = 0; // mean anomaly
        final Orbit initialOrbit = new KeplerianOrbit(a, e, i, omega, raan, lM, PositionAngleType.TRUE,
                                                      inertialFrame, initDate, provider.getMu());

        BrouwerLyddanePropagator BLextrapolator =
                new BrouwerLyddanePropagator(initialOrbit, GravityFieldFactory.getUnnormalizedProvider(provider),
                                             PropagationType.MEAN, BrouwerLyddanePropagator.M2);
        SpacecraftState initialOsculatingState = BLextrapolator.propagate(initDate);
        final KeplerianOrbit InitOrbit = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(initialOsculatingState.getOrbit());

        SpacecraftState BLFinalState = BLextrapolator.propagate(initDate.shiftedBy(timeshift));
        final KeplerianOrbit BLOrbit = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(BLFinalState.getOrbit());

        //_______________________________________________________________________________________________
        // SET UP A REFERENCE NUMERICAL PROPAGATION
        //_______________________________________________________________________________________________

        // Adaptive step integrator with a minimum step of 0.001 and a maximum step of 1000
        final double minStep = 0.001;
        final double maxstep = 1000.0;
        final double positionTolerance = 10.0;
        final OrbitType propagationType = OrbitType.KEPLERIAN;
        final double[][] tolerances =
                ToleranceProvider.getDefaultToleranceProvider(positionTolerance).getTolerances(InitOrbit, propagationType);
        final AdaptiveStepsizeIntegrator integrator =
                new DormandPrince853Integrator(minStep, maxstep, tolerances[0], tolerances[1]);

        // Numerical Propagator
        final NumericalPropagator NumPropagator = new NumericalPropagator(integrator);
        NumPropagator.setOrbitType(propagationType);

        final ForceModel holmesFeatherstone =
                new HolmesFeatherstoneAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010, true), provider);
        NumPropagator.addForceModel(holmesFeatherstone);

        // Set up initial state in the propagator
        NumPropagator.setInitialState(initialOsculatingState);

        // Extrapolate from the initial to the final date
        final SpacecraftState NumFinalState = NumPropagator.propagate(initDate.shiftedBy(timeshift));
        final KeplerianOrbit NumOrbit = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(NumFinalState.getOrbit());

        // Asserts
        Assertions.assertEquals(NumOrbit.getA(), BLOrbit.getA(), 0.174);
        Assertions.assertEquals(NumOrbit.getE(), BLOrbit.getE(), 3.2e-6);
        Assertions.assertEquals(NumOrbit.getI(), BLOrbit.getI(), 6.9e-8);
        Assertions.assertEquals(MathUtils.normalizeAngle(NumOrbit.getPerigeeArgument(), FastMath.PI),
                MathUtils.normalizeAngle(BLOrbit.getPerigeeArgument(), FastMath.PI), 0.0053);
        Assertions.assertEquals(MathUtils.normalizeAngle(NumOrbit.getRightAscensionOfAscendingNode(), FastMath.PI),
                MathUtils.normalizeAngle(BLOrbit.getRightAscensionOfAscendingNode(), FastMath.PI), 1.2e-6);
        Assertions.assertEquals(MathUtils.normalizeAngle(NumOrbit.getTrueAnomaly(), FastMath.PI),
                MathUtils.normalizeAngle(BLOrbit.getTrueAnomaly(), FastMath.PI), 0.0052);
    }

    @Test
    public void compareToNumericalPropagationResetInitialIntermediate() {

        final Frame inertialFrame = FramesFactory.getEME2000();
        AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        double timeshift = 60000.;

        // Initial orbit
        final double a = 24396159; // semi major axis in meters
        final double e = 0.01; // eccentricity
        final double i = FastMath.toRadians(7); // inclination
        final double omega = FastMath.toRadians(180); // perigee argument
        final double raan = FastMath.toRadians(261); // right ascention of ascending node
        final double lM = 0; // mean anomaly
        final Orbit initialOrbit = new KeplerianOrbit(a, e, i, omega, raan, lM, PositionAngleType.TRUE,
                                                      inertialFrame, initDate, provider.getMu());
        // Initial state definition
        final SpacecraftState initialState = new SpacecraftState(initialOrbit);

        //_______________________________________________________________________________________________
        // SET UP A BROUWER LYDDANE PROPAGATOR
        //_______________________________________________________________________________________________

        BrouwerLyddanePropagator BLextrapolator1 =
                new BrouwerLyddanePropagator(initialOrbit, DEFAULT_LAW, Propagator.DEFAULT_MASS, GravityFieldFactory.getUnnormalizedProvider(provider),
                                             PropagationType.OSCULATING, BrouwerLyddanePropagator.M2);
        //_______________________________________________________________________________________________
        // SET UP ANOTHER BROUWER LYDDANE PROPAGATOR
        //_______________________________________________________________________________________________

        BrouwerLyddanePropagator BLextrapolator2 =
                new BrouwerLyddanePropagator( new KeplerianOrbit(a + 3000, e + 0.001, i - FastMath.toRadians(12.0), omega, raan, lM, PositionAngleType.TRUE,
                        inertialFrame, initDate, provider.getMu()),DEFAULT_LAW, Propagator.DEFAULT_MASS, GravityFieldFactory.getUnnormalizedProvider(provider), BrouwerLyddanePropagator.M2);
        // Reset BL2 with BL1 initial state
        BLextrapolator2.resetInitialState(initialState);

        SpacecraftState BLFinalState1 = BLextrapolator1.propagate(initDate.shiftedBy(timeshift));
        final KeplerianOrbit BLOrbit1 = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(BLFinalState1.getOrbit());
        SpacecraftState BLFinalState2 = BLextrapolator2.propagate(initDate.shiftedBy(timeshift));
        BLextrapolator2.resetIntermediateState(BLFinalState1, true);
        final KeplerianOrbit BLOrbit2 = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(BLFinalState2.getOrbit());

        // Asserts
        Assertions.assertEquals(BLOrbit1.getA(), BLOrbit2.getA(), 0.0);
        Assertions.assertEquals(BLOrbit1.getE(), BLOrbit2.getE(), 0.0);
        Assertions.assertEquals(BLOrbit1.getI(), BLOrbit2.getI(), 0.0);
        Assertions.assertEquals(MathUtils.normalizeAngle(BLOrbit1.getPerigeeArgument(), FastMath.PI),
                MathUtils.normalizeAngle(BLOrbit2.getPerigeeArgument(), FastMath.PI), 0.0);
        Assertions.assertEquals(MathUtils.normalizeAngle(BLOrbit1.getRightAscensionOfAscendingNode(), FastMath.PI),
                MathUtils.normalizeAngle(BLOrbit2.getRightAscensionOfAscendingNode(), FastMath.PI), 0.0);
        Assertions.assertEquals(MathUtils.normalizeAngle(BLOrbit1.getTrueAnomaly(), FastMath.PI),
                MathUtils.normalizeAngle(BLOrbit2.getTrueAnomaly(), FastMath.PI), 0.0);
    }

    @Test
    public void compareConstructors() {

        final Frame inertialFrame = FramesFactory.getEME2000();
        AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        double timeshift = 600. ;

        // Initial orbit
        final double a = 24396159; // semi major axis in meters
        final double e = 0.01; // eccentricity
        final double i = FastMath.toRadians(7); // inclination
        final double omega = FastMath.toRadians(180); // perigee argument
        final double raan = FastMath.toRadians(261); // right ascention of ascending node
        final double lV = 0; // true anomaly
        final Orbit initialOrbit = new KeplerianOrbit(a, e, i, omega, raan, lV, PositionAngleType.TRUE,
                                                      inertialFrame, initDate, provider.getMu());

        BrouwerLyddanePropagator BLPropagator1 = new BrouwerLyddanePropagator(initialOrbit, DEFAULT_LAW,
                provider.getAe(), provider.getMu(), -1.08263e-3, 2.54e-6, 1.62e-6, 2.3e-7, BrouwerLyddanePropagator.M2);
        BrouwerLyddanePropagator BLPropagator2 = new BrouwerLyddanePropagator(initialOrbit,
                provider.getAe(), provider.getMu(), -1.08263e-3, 2.54e-6, 1.62e-6, 2.3e-7, BrouwerLyddanePropagator.M2);
        BrouwerLyddanePropagator BLPropagator3 = new BrouwerLyddanePropagator(initialOrbit, Propagator.DEFAULT_MASS,
                provider.getAe(), provider.getMu(), -1.08263e-3, 2.54e-6, 1.62e-6, 2.3e-7, BrouwerLyddanePropagator.M2);

        SpacecraftState BLFinalState1 = BLPropagator1.propagate(initDate.shiftedBy(timeshift));
        final KeplerianOrbit BLOrbit1 = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(BLFinalState1.getOrbit());
        SpacecraftState BLFinalState2 = BLPropagator2.propagate(initDate.shiftedBy(timeshift));
        final KeplerianOrbit BLOrbit2 = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(BLFinalState2.getOrbit());
        SpacecraftState BLFinalState3 = BLPropagator3.propagate(initDate.shiftedBy(timeshift));
        final KeplerianOrbit BLOrbit3 = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(BLFinalState3.getOrbit());

        // Asserts
        Assertions.assertEquals(BLOrbit1.getA(), BLOrbit2.getA(), 0.0);
        Assertions.assertEquals(BLOrbit1.getE(), BLOrbit2.getE(), 0.0);
        Assertions.assertEquals(BLOrbit1.getI(), BLOrbit2.getI(), 0.0);
        Assertions.assertEquals(MathUtils.normalizeAngle(BLOrbit1.getPerigeeArgument(), FastMath.PI),
                MathUtils.normalizeAngle(BLOrbit2.getPerigeeArgument(), FastMath.PI), 0.0);
        Assertions.assertEquals(MathUtils.normalizeAngle(BLOrbit1.getRightAscensionOfAscendingNode(), FastMath.PI),
                MathUtils.normalizeAngle(BLOrbit2.getRightAscensionOfAscendingNode(), FastMath.PI), 0.0);
        Assertions.assertEquals(MathUtils.normalizeAngle(BLOrbit1.getTrueAnomaly(), FastMath.PI),
                MathUtils.normalizeAngle(BLOrbit2.getTrueAnomaly(), FastMath.PI), 0.0);

        Assertions.assertEquals(BLOrbit1.getA(), BLOrbit3.getA(), 0.0);
        Assertions.assertEquals(BLOrbit1.getE(), BLOrbit3.getE(), 0.0);
        Assertions.assertEquals(BLOrbit1.getI(), BLOrbit3.getI(), 0.0);
        Assertions.assertEquals(MathUtils.normalizeAngle(BLOrbit1.getPerigeeArgument(), FastMath.PI),
                MathUtils.normalizeAngle(BLOrbit3.getPerigeeArgument(), FastMath.PI), 0.0);
        Assertions.assertEquals(MathUtils.normalizeAngle(BLOrbit1.getRightAscensionOfAscendingNode(), FastMath.PI),
                MathUtils.normalizeAngle(BLOrbit3.getRightAscensionOfAscendingNode(), FastMath.PI), 0.0);
        Assertions.assertEquals(MathUtils.normalizeAngle(BLOrbit1.getTrueAnomaly(), FastMath.PI),
                MathUtils.normalizeAngle(BLOrbit3.getTrueAnomaly(), FastMath.PI), 0.0);
    }

    @Test
    public void undergroundOrbit() {
        // for a semi major axis < equatorial radius
        Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
        Vector3D velocity = new Vector3D(-500.0, 800.0, 100.0);
        AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH;
        Orbit initialOrbit = new EquinoctialOrbit(new PVCoordinates(position, velocity),
                FramesFactory.getEME2000(), initDate, provider.getMu());
        // Extrapolator definition
        // -----------------------
        OrekitException oe = Assertions.assertThrows(OrekitException.class, () -> {
                new BrouwerLyddanePropagator(initialOrbit, DEFAULT_LAW, provider.getAe(), provider.getMu(),
                        -1.08263e-3, 2.54e-6, 1.62e-6, 2.3e-7, BrouwerLyddanePropagator.M2);
        });
        Assertions.assertTrue(oe.getMessage().contains("trajectory inside the Brillouin sphere (r ="));
    }


    @Test
    public void tooEllipticalOrbit() {
        // for an eccentricity too big for the model
        AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH;
        Orbit initialOrbit = new KeplerianOrbit(67679244.0, 1.0, 1.85850, 2.1, 2.9,
                6.2, PositionAngleType.TRUE, FramesFactory.getEME2000(),
                initDate, provider.getMu());
        // Extrapolator definition
        // -----------------------
        OrekitException oe = Assertions.assertThrows(OrekitException.class, () -> {
                new BrouwerLyddanePropagator(initialOrbit, provider.getAe(), provider.getMu(),
                        -1.08263e-3, 2.54e-6, 1.62e-6, 2.3e-7, BrouwerLyddanePropagator.M2);
              });
        Assertions.assertTrue(oe.getMessage().contains("too large eccentricity for propagation model: e ="));
    }


    @Test
    public void criticalInclination() {

        final Frame inertialFrame = FramesFactory.getEME2000();

        final double a = 24396159; // semi major axis in meters
        final double e = 0.01; // eccentricity
        final double i = FastMath.acos(1.0 / FastMath.sqrt(5.0)); // critical inclination
        final double omega = FastMath.toRadians(180); // perigee argument
        final double raan = FastMath.toRadians(261); // right ascention of ascending node
        final double lV = 0; // true anomaly

        AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH;
        final Orbit initialOrbit = new KeplerianOrbit(a, e, i, omega, raan, lV, PositionAngleType.TRUE,
                                                      inertialFrame, initDate, provider.getMu());

        // Extrapolator definition
        // -----------------------
        BrouwerLyddanePropagator extrapolator =
            new BrouwerLyddanePropagator(initialOrbit, GravityFieldFactory.getUnnormalizedProvider(provider), BrouwerLyddanePropagator.M2);

        // Extrapolation at the initial date
        // ---------------------------------
        SpacecraftState finalOrbit = extrapolator.propagate(initDate);

        // Asserts
        // -------
        Assertions.assertEquals(0.0,
                                Vector3D.distance(initialOrbit.getPosition(),
                                                  finalOrbit.getPosition()),
                                1.5e-8);
        Assertions.assertEquals(0.0,
                                Vector3D.distance(initialOrbit.getVelocity(),
                                                  finalOrbit.getVelocity()),
                                2.7e-12);
        Assertions.assertEquals(0.0, finalOrbit.getOrbit().getA() - initialOrbit.getA(), 7.5e-9);
    }

    @Test
    public void insideEarth() {
        // for an eccentricity too big for the model
        final AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH;
        final Orbit initialOrbit = new KeplerianOrbit(provider.getAe()-1000.0, 0.01,
                                                      FastMath.toRadians(10.0), 2.1, 2.9,
                                                      6.2, PositionAngleType.TRUE,
                                                      FramesFactory.getEME2000(),
                                                      initDate, provider.getMu());
        // Extrapolator definition
        // -----------------------
        OrekitIllegalArgumentException oe = Assertions.assertThrows(OrekitIllegalArgumentException.class, () -> {
                new BrouwerLyddanePropagator(initialOrbit, Propagator.DEFAULT_MASS, provider.getAe(), provider.getMu(),
                        -1.08263e-3, 2.54e-6, 1.62e-6, 2.3e-7);
        });
        Assertions.assertTrue(oe.getMessage().contains("orbit should be either elliptic with a > 0 and e < 1 or hyperbolic with a < 0 and e > 1"));
    }

    @Test
    public void testUnableToComputeBLMeanParameters() {
        final Frame eci = FramesFactory.getEME2000();
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);

        // Initial orbit
        final double a = 24396159; // semi major axis in meters
        final double e = 0.9; // eccentricity
        final double i = FastMath.toRadians(7); // inclination
        final double pa = FastMath.toRadians(180); // perigee argument
        final double raan = FastMath.toRadians(261); // right ascention of ascending node
        final double lM = FastMath.toRadians(0); // mean anomaly
        final Orbit orbit = new KeplerianOrbit(a, e, i, pa, raan, lM, PositionAngleType.MEAN,
                                               eci, date, provider.getMu());
        // Extrapolator definition
        // -----------------------
        final UnnormalizedSphericalHarmonicsProvider up = GravityFieldFactory.getUnnormalizedProvider(provider);
        final UnnormalizedSphericalHarmonics uh = up.onDate(date);
        final double eps  = 1.e-13;
        final int maxIter = 10;
        OrekitException oe = Assertions.assertThrows(OrekitException.class, () -> {
            BrouwerLyddanePropagator.computeMeanOrbit(orbit, up, uh, BrouwerLyddanePropagator.M2,
                                                      eps, maxIter);
        });
        Assertions.assertTrue(oe.getMessage().contains("unable to compute Brouwer-Lyddane mean parameters after"));
    }

    @Test
    public void testMeanOrbit() {
        final KeplerianOrbit initialOsculating =
                        new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngleType.TRUE,
                                           FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH,
                                           provider.getMu());
        final UnnormalizedSphericalHarmonicsProvider ushp = GravityFieldFactory.getUnnormalizedProvider(provider);
        final UnnormalizedSphericalHarmonics ush = ushp.onDate(initialOsculating.getDate());

        // set up a reference numerical propagator starting for the specified start orbit
        // using the same force models (i.e. the first few zonal terms)
        double[][] tol = ToleranceProvider.getDefaultToleranceProvider(0.1).getTolerances(initialOsculating, OrbitType.KEPLERIAN);
        AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(0.001, 1000, tol[0], tol[1]);
        integrator.setInitialStepSize(60);
        NumericalPropagator num = new NumericalPropagator(integrator);
        Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        num.addForceModel(new HolmesFeatherstoneAttractionModel(itrf, provider));
        num.setInitialState(new SpacecraftState(initialOsculating));
        num.setOrbitType(OrbitType.KEPLERIAN);
        num.setPositionAngleType(initialOsculating.getCachedPositionAngleType());
        final StorelessUnivariateStatistic oscMin  = new Min();
        final StorelessUnivariateStatistic oscMax  = new Max();
        final StorelessUnivariateStatistic meanMin = new Min();
        final StorelessUnivariateStatistic meanMax = new Max();
        num.getMultiplexer().add(60, state -> {
            final Orbit osc = state.getOrbit();
            oscMin.increment(osc.getA());
            oscMax.increment(osc.getA());
            // compute mean orbit at current date (this is what we test)
            final Orbit mean = BrouwerLyddanePropagator.computeMeanOrbit(state.getOrbit(), ushp, ush, BrouwerLyddanePropagator.M2);
            meanMin.increment(mean.getA());
            meanMax.increment(mean.getA());
        });
        num.propagate(initialOsculating.getDate().shiftedBy(Constants.JULIAN_DAY));

        // Asserts
        Assertions.assertEquals(3188.347, oscMax.getResult()  - oscMin.getResult(),  1.0e-3);
        Assertions.assertEquals(  25.794, meanMax.getResult() - meanMin.getResult(), 1.0e-3);

    }

    @Test
    public void testGeostationaryOrbit() {

        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Frame eci = FramesFactory.getEME2000();
        final UnnormalizedSphericalHarmonicsProvider ushp = GravityFieldFactory.getUnnormalizedProvider(provider);

        // geostationary orbit
        final double sma = FastMath.cbrt(Constants.IERS2010_EARTH_MU /
                                         Constants.IERS2010_EARTH_ANGULAR_VELOCITY /
                                         Constants.IERS2010_EARTH_ANGULAR_VELOCITY);
        final double ecc  = 0.0;
        final double inc  = 0.0;
        final double pa   = 0.0;
        final double raan = 0.0;
        final double lV   = 0.0;
        final Orbit orbit = new KeplerianOrbit(sma, ecc, inc, pa, raan, lV,
                                               PositionAngleType.TRUE,
                                               eci, date, provider.getMu());

        // set up a BL propagator from mean orbit
        final BrouwerLyddanePropagator bl = new BrouwerLyddanePropagator(orbit, ushp, PropagationType.MEAN,
                                                                         BrouwerLyddanePropagator.M2);

        // propagate
        final SpacecraftState state = bl.propagate(date.shiftedBy(Constants.JULIAN_DAY));
        final KeplerianOrbit orbOsc = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(state.getOrbit());

        // Check all elements are defined, i.e. no singularity
        Assertions.assertTrue(Double.isFinite(orbOsc.getA()));
        Assertions.assertTrue(Double.isFinite(orbOsc.getE()));
        Assertions.assertTrue(Double.isFinite(orbOsc.getI()));
        Assertions.assertTrue(Double.isFinite(orbOsc.getPerigeeArgument()));
        Assertions.assertTrue(Double.isFinite(orbOsc.getRightAscensionOfAscendingNode()));
        Assertions.assertTrue(Double.isFinite(orbOsc.getTrueAnomaly()));

        // set up a BL propagator from osculating orbit
        final BrouwerLyddanePropagator bl2 = new BrouwerLyddanePropagator(orbit, ushp, PropagationType.OSCULATING,
                                                                         BrouwerLyddanePropagator.M2);

        // propagate
        final SpacecraftState state2 = bl2.propagate(date.shiftedBy(Constants.JULIAN_DAY));
        final KeplerianOrbit orbOsc2 = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(state2.getOrbit());

        // Check all elements are defined, i.e. no singularity
        Assertions.assertTrue(Double.isFinite(orbOsc2.getA()));
        Assertions.assertTrue(Double.isFinite(orbOsc2.getE()));
        Assertions.assertTrue(Double.isFinite(orbOsc2.getI()));
        Assertions.assertTrue(Double.isFinite(orbOsc2.getPerigeeArgument()));
        Assertions.assertTrue(Double.isFinite(orbOsc2.getRightAscensionOfAscendingNode()));
        Assertions.assertTrue(Double.isFinite(orbOsc2.getTrueAnomaly()));
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data:atmosphere:potential/icgem-format");
        provider = GravityFieldFactory.getNormalizedProvider(5, 0);
    }

    @AfterEach
    public void tearDown() {
        provider = null;
    }

    private NormalizedSphericalHarmonicsProvider provider;

}
