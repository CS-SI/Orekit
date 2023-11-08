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
package org.orekit.propagation.events;

import org.hipparchus.exception.MathRuntimeException;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.LocalizedODEFormats;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventsLogger.LoggedEvent;
import org.orekit.propagation.events.handlers.ContinueOnEvent;
import org.orekit.propagation.events.handlers.StopOnDecreasing;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.util.List;

public class EclipseDetectorTest {

    private double               mu;
    private AbsoluteDate         iniDate;
    private SpacecraftState      initialState;
    private NumericalPropagator  propagator;

    private CelestialBody        sun;
    private OneAxisEllipsoid     earth;
    private double               sunRadius;

    @Test
    public void testPolar() {
        final KeplerianOrbit original = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(initialState.getOrbit());
        final KeplerianOrbit polar    = new KeplerianOrbit(original.getA(), original.getE(),
                                                           0.5 * FastMath.PI, original.getPerigeeArgument(),
                                                           original.getRightAscensionOfAscendingNode(),
                                                           original.getTrueAnomaly(), PositionAngleType.TRUE,
                                                           original.getFrame(), original.getDate(),
                                                           original.getMu());
        propagator.resetInitialState(new SpacecraftState(polar));
        EventsLogger logger = new EventsLogger();
        OneAxisEllipsoid sphericalEarth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                               0.0,
                                                               FramesFactory.getITRF(IERSConventions.IERS_2010,
                                                                                     true));
        EclipseDetector withoutFlattening = new EclipseDetector(sun, sunRadius, sphericalEarth).
                                            withMaxCheck(60.0).
                                            withThreshold(1.0e-3).
                                            withHandler(new ContinueOnEvent()).
                                            withUmbra();
        OneAxisEllipsoid obateEarth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                           Constants.WGS84_EARTH_FLATTENING,
                                                           FramesFactory.getITRF(IERSConventions.IERS_2010,
                                                                                 true));
        EclipseDetector withFlattening    = new EclipseDetector(sun, sunRadius, obateEarth).
                                            withMaxCheck(60.0).
                                            withThreshold(1.0e-3).
                                            withHandler(new ContinueOnEvent()).
                                            withUmbra();
        propagator.addEventDetector(logger.monitorDetector(withoutFlattening));
        propagator.addEventDetector(logger.monitorDetector(withFlattening));
        double duration = 15000.0;
        final SpacecraftState finalState = propagator.propagate(iniDate.shiftedBy(duration));
        Assertions.assertEquals(duration, finalState.getDate().durationFrom(iniDate), 1.0e-3);
        final List<LoggedEvent> events = logger.getLoggedEvents();
        Assertions.assertEquals(10, events.size());
        Assertions.assertTrue(events.get(0).getEventDetector() == withoutFlattening);
        Assertions.assertFalse(events.get(0).isIncreasing());
        Assertions.assertEquals( 2274.702, events.get(0).getState().getDate().durationFrom(iniDate), 1.0e-3);
        Assertions.assertTrue(events.get(1).getEventDetector() == withFlattening);
        Assertions.assertFalse(events.get(1).isIncreasing());
        Assertions.assertEquals( 2280.427, events.get(1).getState().getDate().durationFrom(iniDate), 1.0e-3);
        Assertions.assertTrue(events.get(2).getEventDetector() == withFlattening);
        Assertions.assertTrue(events.get(2).isIncreasing());
        Assertions.assertEquals( 4310.742, events.get(2).getState().getDate().durationFrom(iniDate), 1.0e-3);
        Assertions.assertTrue(events.get(3).getEventDetector() == withoutFlattening);
        Assertions.assertTrue(events.get(3).isIncreasing());
        Assertions.assertEquals( 4317.155, events.get(3).getState().getDate().durationFrom(iniDate), 1.6e-3);
        Assertions.assertTrue(events.get(4).getEventDetector() == withoutFlattening);
        Assertions.assertFalse(events.get(4).isIncreasing());
        Assertions.assertEquals( 8189.250, events.get(4).getState().getDate().durationFrom(iniDate), 1.0e-3);
        Assertions.assertTrue(events.get(5).getEventDetector() == withFlattening);
        Assertions.assertFalse(events.get(5).isIncreasing());
        Assertions.assertEquals( 8194.978, events.get(5).getState().getDate().durationFrom(iniDate), 1.0e-3);
        Assertions.assertTrue(events.get(6).getEventDetector() == withFlattening);
        Assertions.assertTrue(events.get(6).isIncreasing());
        Assertions.assertEquals(10225.704, events.get(6).getState().getDate().durationFrom(iniDate), 1.0e-3);
        Assertions.assertTrue(events.get(7).getEventDetector() == withoutFlattening);
        Assertions.assertTrue(events.get(7).isIncreasing());
        Assertions.assertEquals(10232.115, events.get(7).getState().getDate().durationFrom(iniDate), 1.0e-3);
        Assertions.assertTrue(events.get(8).getEventDetector() == withoutFlattening);
        Assertions.assertFalse(events.get(8).isIncreasing());
        Assertions.assertEquals(14103.800, events.get(8).getState().getDate().durationFrom(iniDate), 1.0e-3);
        Assertions.assertTrue(events.get(9).getEventDetector() == withFlattening);
        Assertions.assertFalse(events.get(9).isIncreasing());
        Assertions.assertEquals(14109.530, events.get(9).getState().getDate().durationFrom(iniDate), 1.0e-3);

    }

    @Test
    public void testEclipse() {
        EclipseDetector e = new EclipseDetector(sun, sunRadius, earth).
                            withMaxCheck(60.0).
                            withThreshold(1.0e-3).
                            withHandler(new StopOnDecreasing()).
                            withUmbra();
        Assertions.assertEquals(60.0, e.getMaxCheckInterval().currentInterval(null), 1.0e-15);
        Assertions.assertEquals(1.0e-3, e.getThreshold(), 1.0e-15);
        Assertions.assertEquals(AbstractDetector.DEFAULT_MAX_ITER, e.getMaxIterationCount());
        Assertions.assertEquals(0.0, e.getMargin(), 1.0e-15);
        Assertions.assertTrue(e.getTotalEclipse());
        propagator.addEventDetector(e);
        final SpacecraftState finalState = propagator.propagate(iniDate.shiftedBy(6000));
        Assertions.assertEquals(2303.1835, finalState.getDate().durationFrom(iniDate), 1.0e-3);
    }

    @Test
    public void testPenumbra() {
        EclipseDetector e = new EclipseDetector(sun, sunRadius, earth).
                            withMaxCheck(60.0).
                            withThreshold(1.0e-3).
                            withPenumbra();
        Assertions.assertFalse(e.getTotalEclipse());
        propagator.addEventDetector(e);
        final SpacecraftState finalState = propagator.propagate(iniDate.shiftedBy(6000));
        Assertions.assertEquals(4388.155852, finalState.getDate().durationFrom(iniDate), 2.0e-6);
    }

    @Test
    public void testWithMethods() {
        EclipseDetector e = new EclipseDetector(sun, sunRadius, earth).
                             withHandler(new StopOnDecreasing()).
                             withMaxCheck(120.0).
                             withThreshold(1.0e-4).
                             withMaxIter(12).
                             withMargin(0.001);
        Assertions.assertEquals(120.0, e.getMaxCheckInterval().currentInterval(null), 1.0e-15);
        Assertions.assertEquals(1.0e-4, e.getThreshold(), 1.0e-15);
        Assertions.assertEquals(12, e.getMaxIterationCount());
        propagator.addEventDetector(e);
        final SpacecraftState finalState = propagator.propagate(iniDate.shiftedBy(6000));
        Assertions.assertEquals(2304.188978, finalState.getDate().durationFrom(iniDate), 1.0e-4);

    }

    @Test
    public void testInsideOcculting() {
        EclipseDetector e = new EclipseDetector(sun, sunRadius, earth);
        SpacecraftState s = new SpacecraftState(new CartesianOrbit(new TimeStampedPVCoordinates(AbsoluteDate.J2000_EPOCH,
                                                                                                new Vector3D(1e6, 2e6, 3e6),
                                                                                                new Vector3D(1000, 0, 0)),
                                                                   FramesFactory.getGCRF(),
                                                                   mu));
        try {
            e.g(s);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.POINT_INSIDE_ELLIPSOID, oe.getSpecifier());
        }
    }

    @Test
    public void testInsideOcculted() {
        EclipseDetector e = new EclipseDetector(sun, sunRadius, earth);
        Vector3D p = sun.getPosition(AbsoluteDate.J2000_EPOCH, FramesFactory.getGCRF());
        SpacecraftState s = new SpacecraftState(new CartesianOrbit(new TimeStampedPVCoordinates(AbsoluteDate.J2000_EPOCH,
                                                                                                p.add(Vector3D.PLUS_I),
                                                                                                Vector3D.PLUS_K),
                                                                   FramesFactory.getGCRF(),
                                                                   mu));
        Assertions.assertEquals(FastMath.PI, e.g(s), 1.0e-15);
    }

    @Test
    public void testTooSmallMaxIterationCount() {
        int n = 5;
        EclipseDetector e = new EclipseDetector(sun, sunRadius, earth).
                             withHandler(new StopOnDecreasing()).
                             withMaxCheck(120.0).
                             withThreshold(1.0e-4).
                             withMaxIter(n);
       propagator.addEventDetector(e);
        try {
            propagator.propagate(iniDate.shiftedBy(6000));
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(LocalizedODEFormats.FIND_ROOT,
                                    ((MathRuntimeException) oe.getCause()).getSpecifier());
        }
    }

    @BeforeEach
    public void setUp() {
        try {
            Utils.setDataRoot("regular-data");
            mu  = 3.9860047e14;
            final Vector3D position  = new Vector3D(-6142438.668, 3492467.560, -25767.25680);
            final Vector3D velocity  = new Vector3D(505.8479685, 942.7809215, 7435.922231);
            iniDate = new AbsoluteDate(1969, 7, 28, 4, 0, 0.0, TimeScalesFactory.getTT());
            final Orbit orbit = new EquinoctialOrbit(new PVCoordinates(position,  velocity),
                                                     FramesFactory.getGCRF(), iniDate, mu);
            initialState = new SpacecraftState(orbit);
            double[] absTolerance = {
                0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6, 0.001
            };
            double[] relTolerance = {
                1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7, 1.0e-7
            };
            AdaptiveStepsizeIntegrator integrator =
                new DormandPrince853Integrator(0.001, 1000, absTolerance, relTolerance);
            integrator.setInitialStepSize(60);
            propagator = new NumericalPropagator(integrator);
            propagator.setInitialState(initialState);
            sun = CelestialBodyFactory.getSun();
            earth = new OneAxisEllipsoid(6400000., 0.0, FramesFactory.getITRF(IERSConventions.IERS_2010, true));
            sunRadius = 696000000.;
        } catch (OrekitException oe) {
            Assertions.fail(oe.getLocalizedMessage());
        }
    }

    @AfterEach
    public void tearDown() {
        iniDate = null;
        initialState = null;
        propagator = null;
    }

}

