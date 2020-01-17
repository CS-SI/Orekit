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
package org.orekit.propagation.events;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.ICGEMFormatReader;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.propagation.events.EventsLogger.LoggedEvent;
import org.orekit.propagation.events.handlers.ContinueOnEvent;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

public class PositionAngleDetectorTest {

    @Test
    public void testCartesian() {
        try {
            new PositionAngleDetector(OrbitType.CARTESIAN, PositionAngle.TRUE, 0.0).
            withMaxCheck(600.0).
            withThreshold(1.0e-6);
            Assert.fail("an exception should habe been thrown");
        } catch (OrekitIllegalArgumentException oiae) {
            Assert.assertEquals(OrekitMessages.ORBIT_TYPE_NOT_ALLOWED, oiae.getSpecifier());
            Assert.assertEquals(OrbitType.CARTESIAN, oiae.getParts()[0]);
        }
    }

    @Test
    public void testTrueAnomalyForward() {
        doTest(OrbitType.KEPLERIAN, PositionAngle.TRUE, FastMath.toRadians(10.0), Constants.JULIAN_DAY, 15);
    }

    @Test
    public void testTrueAnomalyBackward() {
        doTest(OrbitType.KEPLERIAN, PositionAngle.TRUE, FastMath.toRadians(10.0), -Constants.JULIAN_DAY, 14);
    }

    @Test
    public void testMeanAnomalyForward() {
        doTest(OrbitType.KEPLERIAN, PositionAngle.MEAN, FastMath.toRadians(10.0), Constants.JULIAN_DAY, 15);
    }

    @Test
    public void testMeanAnomalyBackward() {
        doTest(OrbitType.KEPLERIAN, PositionAngle.MEAN, FastMath.toRadians(10.0), -Constants.JULIAN_DAY, 14);
    }

    @Test
    public void testEccentricAnomalyForward() {
        doTest(OrbitType.KEPLERIAN, PositionAngle.ECCENTRIC, FastMath.toRadians(10.0), Constants.JULIAN_DAY, 15);
    }

    @Test
    public void testEccentricAnomalyBackward() {
        doTest(OrbitType.KEPLERIAN, PositionAngle.ECCENTRIC, FastMath.toRadians(10.0), -Constants.JULIAN_DAY, 14);
    }

    @Test
    public void testTrueLatitudeArgumentForward() {
        doTest(OrbitType.CIRCULAR, PositionAngle.TRUE, FastMath.toRadians(730.0), Constants.JULIAN_DAY, 15);
    }

    @Test
    public void testTrueLatitudeArgumentBackward() {
        doTest(OrbitType.CIRCULAR, PositionAngle.TRUE, FastMath.toRadians(730.0), -Constants.JULIAN_DAY, 14);
    }

    @Test
    public void testMeanLatitudeArgumentForward() {
        doTest(OrbitType.CIRCULAR, PositionAngle.MEAN, FastMath.toRadians(730.0), Constants.JULIAN_DAY, 15);
    }

    @Test
    public void testMeanLatitudeArgumentBackward() {
        doTest(OrbitType.CIRCULAR, PositionAngle.MEAN, FastMath.toRadians(730.0), -Constants.JULIAN_DAY, 14);
    }

    @Test
    public void testEccentricLatitudeArgumentForward() {
        doTest(OrbitType.CIRCULAR, PositionAngle.ECCENTRIC, FastMath.toRadians(730.0), Constants.JULIAN_DAY, 15);
    }

    @Test
    public void testEccentricLatitudeArgumentBackward() {
        doTest(OrbitType.CIRCULAR, PositionAngle.ECCENTRIC, FastMath.toRadians(730.0), -Constants.JULIAN_DAY, 14);
    }

    @Test
    public void testTrueLongitudeArgumentForward() {
        doTest(OrbitType.EQUINOCTIAL, PositionAngle.TRUE, FastMath.toRadians(-45.0), Constants.JULIAN_DAY, 15);
    }

    @Test
    public void testTrueLongitudeArgumentBackward() {
        doTest(OrbitType.EQUINOCTIAL, PositionAngle.TRUE, FastMath.toRadians(-45.0), -Constants.JULIAN_DAY, 14);
    }

    @Test
    public void testMeanLongitudeArgumentForward() {
        doTest(OrbitType.EQUINOCTIAL, PositionAngle.MEAN, FastMath.toRadians(-45.0), Constants.JULIAN_DAY, 15);
    }

    @Test
    public void testMeanLongitudeArgumentBackward() {
        doTest(OrbitType.EQUINOCTIAL, PositionAngle.MEAN, FastMath.toRadians(-45.0), -Constants.JULIAN_DAY, 14);
    }

    @Test
    public void testEccentricLongitudeArgumentForward() {
        doTest(OrbitType.EQUINOCTIAL, PositionAngle.ECCENTRIC, FastMath.toRadians(-45.0), Constants.JULIAN_DAY, 15);
    }

    @Test
    public void testEccentricLongitudeArgumentBackward() {
        doTest(OrbitType.EQUINOCTIAL, PositionAngle.ECCENTRIC, FastMath.toRadians(-45.0), -Constants.JULIAN_DAY, 14);
    }

    @Test
    public void testIssue493() {

        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("eigen-6s-truncated", false));
        NormalizedSphericalHarmonicsProvider provider =
                        GravityFieldFactory.getNormalizedProvider(10, 10);

        Frame inertialFrame = FramesFactory.getEME2000();

        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate initialDate = new AbsoluteDate(2004, 01, 01, 23, 30, 00.000, utc);

        double mu =  provider.getMu();

        double a = 24396159;                 // semi major axis in meters
        double e = 0.72831215;               // eccentricity
        double i = FastMath.toRadians(7);        // inclination
        double omega = FastMath.toRadians(180);  // perigee argument
        double raan = FastMath.toRadians(261);   // right ascension of ascending node
        double lM = 0;                       // mean anomaly

        Orbit initialOrbit = new KeplerianOrbit(a, e, i, omega, raan, lM, PositionAngle.MEAN,
                                                inertialFrame, initialDate, mu);

        // Initial state definition
        SpacecraftState initialState = new SpacecraftState(initialOrbit);

        // Adaptive step integrator
        // with a minimum step of 0.001 and a maximum step of 1000
        double minStep = 0.001;
        double maxstep = 1000.0;
        double positionTolerance = 10.0;
        OrbitType propagationType = OrbitType.KEPLERIAN;
        double[][] tolerances =
                        NumericalPropagator.tolerances(positionTolerance, initialOrbit, propagationType);
        AdaptiveStepsizeIntegrator integrator =
                        new DormandPrince853Integrator(minStep, maxstep, tolerances[0], tolerances[1]);

        // Propagator in Keplerian mode
        NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.setOrbitType(propagationType);

        // Simple gravity field force model
        ForceModel holmesFeatherstone =
                        new HolmesFeatherstoneAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010,true),
                                                              provider);

        propagator.addForceModel(holmesFeatherstone);

        final double maxCheck  = 600.0;
        final double threshold = 1.0e-6;
        PositionAngleDetector detector01 = new PositionAngleDetector(maxCheck,
                                                                     threshold,
                                                                     propagationType,
                                                                     PositionAngle.TRUE,
                                                                     FastMath.toRadians(01.0)).
                                           withHandler(new ContinueOnEvent<>());
        PositionAngleDetector detector90 = new PositionAngleDetector(maxCheck,
                                                                     threshold,
                                                                     propagationType,
                                                                     PositionAngle.TRUE,
                                                                     FastMath.toRadians(90.0)).
                                           withHandler(new ContinueOnEvent<>());

        // detect events with numerical propagator (and generate ephemeris)
        propagator.setEphemerisMode();
        propagator.setInitialState(initialState);
        EventsLogger logger1 = new EventsLogger();
        propagator.addEventDetector(logger1.monitorDetector(detector01));
        propagator.addEventDetector(logger1.monitorDetector(detector90));
        final AbsoluteDate finalDate = propagator.propagate(new AbsoluteDate(initialDate, Constants.JULIAN_DAY)).getDate();
        final BoundedPropagator ephemeris = propagator.getGeneratedEphemeris();
        Assert.assertEquals(6, logger1.getLoggedEvents().size());

        // detect events with generated ephemeris
        EventsLogger logger2 = new EventsLogger();
        ephemeris.addEventDetector(logger2.monitorDetector(detector01));
        ephemeris.addEventDetector(logger2.monitorDetector(detector90));
        ephemeris.propagate(initialDate, finalDate);
        Assert.assertEquals(logger1.getLoggedEvents().size(), logger2.getLoggedEvents().size());
        for (int k = 0; k < logger1.getLoggedEvents().size(); ++k) {
            AbsoluteDate date1 = logger1.getLoggedEvents().get(k).getState().getDate();
            AbsoluteDate date2 = logger2.getLoggedEvents().get(k).getState().getDate();
            Assert.assertEquals(0.0, date2.durationFrom(date1), threshold);
        }

    }

    private void doTest(final OrbitType orbitType, final PositionAngle positionAngle,
                        final double angle, final double deltaT, final int expectedCrossings) {

        PositionAngleDetector d =
                new PositionAngleDetector(orbitType, positionAngle, angle).
                withMaxCheck(60).
                withThreshold(1.e-10).
                withHandler(new ContinueOnEvent<PositionAngleDetector>());

        Assert.assertEquals(60.0, d.getMaxCheckInterval(), 1.0e-15);
        Assert.assertEquals(1.0e-10, d.getThreshold(), 1.0e-15);
        Assert.assertEquals(orbitType, d.getOrbitType());
        Assert.assertEquals(positionAngle, d.getPositionAngle());
        Assert.assertEquals(angle, d.getAngle(), 1.0e-14);
        Assert.assertEquals(AbstractDetector.DEFAULT_MAX_ITER, d.getMaxIterationCount());

        final TimeScale utc = TimeScalesFactory.getUTC();
        final Vector3D position = new Vector3D(-6142438.668, 3492467.56, -25767.257);
        final Vector3D velocity = new Vector3D(506.0, 943.0, 7450);
        final AbsoluteDate date = new AbsoluteDate(2003, 9, 16, utc);
        final Orbit orbit = new CartesianOrbit(new PVCoordinates(position,  velocity),
                                               FramesFactory.getEME2000(), date,
                                               Constants.EIGEN5C_EARTH_MU);

        Propagator propagator =
            new EcksteinHechlerPropagator(orbit,
                                          Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS,
                                          Constants.EIGEN5C_EARTH_MU,
                                          Constants.EIGEN5C_EARTH_C20,
                                          Constants.EIGEN5C_EARTH_C30,
                                          Constants.EIGEN5C_EARTH_C40,
                                          Constants.EIGEN5C_EARTH_C50,
                                          Constants.EIGEN5C_EARTH_C60);

        EventsLogger logger = new EventsLogger();
        propagator.addEventDetector(logger.monitorDetector(d));

        propagator.propagate(date.shiftedBy(deltaT));

        double[] array = new double[6];
        for (LoggedEvent e : logger.getLoggedEvents()) {
            SpacecraftState state = e.getState();
            orbitType.mapOrbitToArray(state.getOrbit(), positionAngle, array, null);
            Assert.assertEquals(angle, MathUtils.normalizeAngle(array[5], angle), 1.0e-10);
        }
        Assert.assertEquals(expectedCrossings, logger.getLoggedEvents().size());

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data:potential");
    }

}

