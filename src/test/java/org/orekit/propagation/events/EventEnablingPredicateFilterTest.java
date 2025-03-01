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
package org.orekit.propagation.events;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.events.Action;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.orekit.TestUtils;
import org.orekit.Utils;
import org.orekit.attitudes.LofOffset;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.forces.maneuvers.ImpulseManeuver;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.frames.TopocentricFrame;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.events.EventsLogger.LoggedEvent;
import org.orekit.propagation.events.handlers.ContinueOnEvent;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.RecordAndContinue;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

import java.util.ArrayList;
import java.util.List;

class EventEnablingPredicateFilterTest {

    private OneAxisEllipsoid earth;
    private GeodeticPoint gp;
    private Orbit orbit;

    @Test
    void testForward0Degrees() {
        doElevationTest(FastMath.toRadians(0.0),
               orbit.getDate(),
               orbit.getDate().shiftedBy(Constants.JULIAN_DAY),
               8, true);
    }

    @Test
    void testForward5Degrees() {
        doElevationTest(FastMath.toRadians(5.0),
               orbit.getDate(),
               orbit.getDate().shiftedBy(Constants.JULIAN_DAY),
               6, false);
    }

    @Test
    void testForward5DegreesStartEnabled() {
        doElevationTest(FastMath.toRadians(5.0),
               orbit.getDate().shiftedBy(12614.0),
               orbit.getDate().shiftedBy(Constants.JULIAN_DAY),
               6, false);
    }

    @Test
    void testBackward0Degrees() {
        doElevationTest(FastMath.toRadians(0.0),
               orbit.getDate().shiftedBy(Constants.JULIAN_DAY),
               orbit.getDate(),
               8, true);
    }

    @Test
    void testBackward5Degrees() {
        doElevationTest(FastMath.toRadians(5.0),
               orbit.getDate().shiftedBy(Constants.JULIAN_DAY),
               orbit.getDate(),
               6, false);
    }

    @Test
    void testBackward5DegreesStartEnabled() {
        doElevationTest(FastMath.toRadians(5.0),
               orbit.getDate().shiftedBy(73112.0),
               orbit.getDate(),
               6, true);
    }

    private void doElevationTest(final double minElevation,
                                 final AbsoluteDate start, final AbsoluteDate end,
                                 final int expectedEvents, final boolean sameSign) {

        final ElevationExtremumDetector raw =
                new ElevationExtremumDetector(0.001, 1.e-6, new TopocentricFrame(earth, gp, "test")).
                withHandler(new ContinueOnEvent());
        final EventEnablingPredicateFilter aboveGroundElevationDetector =
                new EventEnablingPredicateFilter(raw,
                        (state, eventDetector, g) -> ((ElevationExtremumDetector) eventDetector).getElevation(state) > minElevation).withMaxCheck(60.0);

        Assertions.assertSame(raw, aboveGroundElevationDetector.getDetector());
        Assertions.assertEquals(0.001, raw.getMaxCheckInterval().currentInterval(null, true), 1.0e-15);
        Assertions.assertEquals(60.0, aboveGroundElevationDetector.getMaxCheckInterval().currentInterval(null, true), 1.0e-15);
        Assertions.assertEquals(1.0e-6, aboveGroundElevationDetector.getThreshold(), 1.0e-15);
        Assertions.assertEquals(AbstractDetector.DEFAULT_MAX_ITER, aboveGroundElevationDetector.getMaxIterationCount());


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
        propagator.addEventDetector(logger.monitorDetector(aboveGroundElevationDetector));

        propagator.propagate(start, end);
        for (LoggedEvent e : logger.getLoggedEvents()) {
            final double eMinus = raw.getElevation(e.getState().shiftedBy(-10.0));
            final double e0     = raw.getElevation(e.getState());
            final double ePlus  = raw.getElevation(e.getState().shiftedBy(+10.0));
            Assertions.assertTrue(e0 > eMinus);
            Assertions.assertTrue(e0 > ePlus);
            Assertions.assertTrue(e0 > minElevation);
        }
        Assertions.assertEquals(expectedEvents, logger.getLoggedEvents().size());

        propagator.clearEventsDetectors();
        double g1Raw = raw.g(propagator.propagate(orbit.getDate().shiftedBy(18540.0)));
        double g2Raw = raw.g(propagator.propagate(orbit.getDate().shiftedBy(18624.0)));
        double g1 = aboveGroundElevationDetector.g(propagator.propagate(orbit.getDate().shiftedBy(18540.0)));
        double g2 = aboveGroundElevationDetector.g(propagator.propagate(orbit.getDate().shiftedBy(18624.0)));
        Assertions.assertTrue(g1Raw > 0);
        Assertions.assertTrue(g2Raw < 0);
        if (sameSign) {
            Assertions.assertTrue(g1 > 0);
            Assertions.assertTrue(g2 < 0);
        } else {
            Assertions.assertTrue(g1 < 0);
            Assertions.assertTrue(g2 > 0);
        }

    }

    @Test
    void testResetState() {
        final List<AbsoluteDate> reset = new ArrayList<>();
        DateDetector raw = new DateDetector(orbit.getDate().shiftedBy(3600.0)).
                        withMaxCheck(1000.0).
                        withHandler(new EventHandler() {
                            public SpacecraftState resetState(EventDetector detector, SpacecraftState oldState) {
                                reset.add(oldState.getDate());
                                return oldState;
                            }
                            public Action eventOccurred(SpacecraftState s, EventDetector detector, boolean increasing) {
                                return Action.RESET_STATE;
                            }
                        });
        for (int i = 2; i < 10; ++i) {
            raw.addEventDate(orbit.getDate().shiftedBy(i * 3600.0));
        }
        EventEnablingPredicateFilter filtered =
                        new EventEnablingPredicateFilter(raw, (state, eventDetector, g) -> state.getDate().durationFrom(orbit.getDate()) > 20000.0);
        Propagator propagator = new KeplerianPropagator(orbit);
        EventsLogger logger = new EventsLogger();
        propagator.addEventDetector(logger.monitorDetector(filtered));
        propagator.propagate(orbit.getDate().shiftedBy(Constants.JULIAN_DAY));
        List<LoggedEvent> events = logger.getLoggedEvents();
        Assertions.assertEquals(4, events.size());
        Assertions.assertEquals(6 * 3600, events.get(0).getState().getDate().durationFrom(orbit.getDate()), 1.0e-6);
        Assertions.assertEquals(7 * 3600, events.get(1).getState().getDate().durationFrom(orbit.getDate()), 1.0e-6);
        Assertions.assertEquals(8 * 3600, events.get(2).getState().getDate().durationFrom(orbit.getDate()), 1.0e-6);
        Assertions.assertEquals(9 * 3600, events.get(3).getState().getDate().durationFrom(orbit.getDate()), 1.0e-6);
        Assertions.assertEquals(4, reset.size());
        Assertions.assertEquals(6 * 3600, reset.get(0).durationFrom(orbit.getDate()), 1.0e-6);
        Assertions.assertEquals(7 * 3600, reset.get(1).durationFrom(orbit.getDate()), 1.0e-6);
        Assertions.assertEquals(8 * 3600, reset.get(2).durationFrom(orbit.getDate()), 1.0e-6);
        Assertions.assertEquals(9 * 3600, reset.get(3).durationFrom(orbit.getDate()), 1.0e-6);

    }

    @Test
    void testExceedHistoryForward() {
        final double period = 900.0;

        // the raw detector should trigger one event at each 900s period
        final DateDetector raw = new DateDetector(orbit.getDate().shiftedBy(-0.5 * period)).
                                 withMaxCheck(period / 3).
                                 withHandler(new ContinueOnEvent());
        for (int i = 0; i < 300; ++i) {
            raw.addEventDate(orbit.getDate().shiftedBy((i + 0.5) * period));
        }

        // in fact, we will filter out half of these events, so we get only one event every 2 periods
        final EventEnablingPredicateFilter filtered =
                        new EventEnablingPredicateFilter(raw, (state, eventDetector, g) -> {
                            double nbPeriod = state.getDate().durationFrom(orbit.getDate()) / period;
                            return ((int) FastMath.floor(nbPeriod)) % 2 == 1;
                        });
        Propagator propagator = new KeplerianPropagator(orbit);
        EventsLogger logger = new EventsLogger();
        propagator.addEventDetector(logger.monitorDetector(filtered));
        propagator.propagate(orbit.getDate().shiftedBy(301 * period));
        List<LoggedEvent> events = logger.getLoggedEvents();

        // 300 periods, 150 events as half of them are filtered out
        Assertions.assertEquals(150, events.size());

        // as we have encountered a lot of enabling status changes, we exceeded the internal history
        // if we try to display again the filtered g function for dates far in the past,
        // we will not see the zero crossings anymore, they have been lost
        propagator.clearEventsDetectors();
        for (double dt = 5000.0; dt < 10000.0; dt += 3.0) {
            double filteredG = filtered.g(propagator.propagate(orbit.getDate().shiftedBy(dt)));
            Assertions.assertTrue(filteredG < 0.0);
        }

        // on the other hand, if we try to display again the filtered g function for past dates
        // that are still inside the history, we still see the zero crossings
        for (double dt = 195400.0; dt < 196200.0; dt += 3.0) {
            double filteredG = filtered.g(propagator.propagate(orbit.getDate().shiftedBy(dt)));
            if (dt < 195750) {
                Assertions.assertTrue(filteredG > 0.0);
            } else {
                Assertions.assertTrue(filteredG < 0.0);
            }
        }

    }

    @Test
    void testExceedHistoryBackward() {
        final double period = 900.0;

        // the raw detector should trigger one event at each 900s period
        final DateDetector raw = new DateDetector(orbit.getDate().shiftedBy(+0.5 * period)).
                                 withMaxCheck(period / 3).
                                 withHandler(new ContinueOnEvent());
        for (int i = 0; i < 300; ++i) {
            raw.addEventDate(orbit.getDate().shiftedBy(-(i + 0.5) * period));
        }

        // in fact, we will filter out half of these events, so we get only one event every 2 periods
        final EventEnablingPredicateFilter filtered =
                        new EventEnablingPredicateFilter(raw, (state, eventDetector, g) -> {
                            double nbPeriod = orbit.getDate().durationFrom(state.getDate()) / period;
                            return ((int) FastMath.floor(nbPeriod)) % 2 == 1;
                        });
        Propagator propagator = new KeplerianPropagator(orbit);
        EventsLogger logger = new EventsLogger();
        propagator.addEventDetector(logger.monitorDetector(filtered));
        propagator.propagate(orbit.getDate().shiftedBy(-301 * period));
        List<LoggedEvent> events = logger.getLoggedEvents();

        // 300 periods, 150 events as half of them are filtered out
        Assertions.assertEquals(150, events.size());

        // as we have encountered a lot of enabling status changes, we exceeded the internal history
        // if we try to display again the filtered g function for dates far in the future,
        // we will not see the zero crossings anymore, they have been lost
        propagator.clearEventsDetectors();
        for (double dt = -5000.0; dt > -10000.0; dt -= 3.0) {
            double filteredG = filtered.g(propagator.propagate(orbit.getDate().shiftedBy(dt)));
            Assertions.assertTrue(filteredG < 0.0);
        }

        // on the other hand, if we try to display again the filtered g function for future dates
        // that are still inside the history, we still see the zero crossings
        for (double dt = -195400.0; dt > -196200.0; dt -= 3.0) {
            double filteredG = filtered.g(propagator.propagate(orbit.getDate().shiftedBy(dt)));
            if (dt < -195750) {
                Assertions.assertTrue(filteredG < 0.0);
            } else {
                Assertions.assertTrue(filteredG > 0.0);
            }
        }

    }

    @Test
    void testGenerics() {
        // setup
        DateDetector detector = new DateDetector(orbit.getDate());
        EnablingPredicate predicate = (state, eventDetector, g) -> true;

        // action + verify. Just make sure it compiles with generics
        new EventEnablingPredicateFilter(detector, predicate);
    }

    @Disabled("Awaiting fix")
    @ParameterizedTest
    @ValueSource(doubles = {1, 2.6, 3.5, 4.5, 5.2, 6})
    void testIssue1647Analytical(final double enablingFactor) {
        final Orbit initialOrbit = TestUtils.getDefaultOrbit(AbsoluteDate.ARBITRARY_EPOCH);
        final KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit);
        testTemplateWithStateResetter(2.8, enablingFactor, propagator);
    }

    private void testTemplateWithStateResetter(final double maneuverDelayFactor, final double enablingFactor,
                                               final Propagator propagator) {
        // GIVEN
        final Orbit initialOrbit = propagator.getInitialState().getOrbit();
        final AbsoluteDate epoch = initialOrbit.getDate();
        final double period = initialOrbit.getKeplerianPeriod();
        final ApsideDetector apsideDetector = new ApsideDetector(initialOrbit);
        final AbsoluteDate maneuverDate = epoch.shiftedBy(period * maneuverDelayFactor);
        final ImpulseManeuver maneuver = new ImpulseManeuver(new DateDetector(maneuverDate),
                new LofOffset(initialOrbit.getFrame(), LOFType.TNW), Vector3D.PLUS_I.scalarMultiply(100.), Double.POSITIVE_INFINITY);
        propagator.addEventDetector(maneuver);
        final RecordAndContinue recordAndContinue = new RecordAndContinue();
        final AbsoluteDate date = epoch.shiftedBy(maneuverDate.shiftedBy(period * enablingFactor));
        // WHEN
        propagator.addEventDetector(new EventEnablingPredicateFilter(apsideDetector.withHandler(recordAndContinue),
                ((state, detector, g) -> state.getDate().isAfterOrEqualTo(date))));
        propagator.propagate(epoch.shiftedBy(period * 10));
        // THEN
        final List<RecordAndContinue.Event> eventList = recordAndContinue.getEvents();
        Assertions.assertFalse(eventList.isEmpty());
        for (final RecordAndContinue.Event event: eventList) {
            final SpacecraftState state = event.getState();
            Assertions.assertEquals(0., apsideDetector.g(state), 1e-3);
        }
    }


    @BeforeEach
    void setUp() {

        Utils.setDataRoot("regular-data");
        earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                     Constants.WGS84_EARTH_FLATTENING,
                                     FramesFactory.getITRF(IERSConventions.IERS_2010, true));

        gp = new GeodeticPoint(FastMath.toRadians(51.0), FastMath.toRadians(66.6), 300.0);
        final TimeScale utc = TimeScalesFactory.getUTC();
        final Vector3D position = new Vector3D(-6142438.668, 3492467.56, -25767.257);
        final Vector3D velocity = new Vector3D(505.848, 942.781, 7435.922);
        final AbsoluteDate date = new AbsoluteDate(2003, 9, 16, utc);
        orbit = new EquinoctialOrbit(new PVCoordinates(position,  velocity),
                                     FramesFactory.getEME2000(), date, Constants.EIGEN5C_EARTH_MU);

    }

    @AfterEach
    void tearDown() {
        earth = null;
        gp    = null;
        orbit = null;
    }

}

