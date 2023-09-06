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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.events.Action;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

public class EventSlopeFilterTest {

    private AbsoluteDate     iniDate;
    private Propagator       propagator;
    private OneAxisEllipsoid earth;

    private double sunRadius = 696000000.;
    private double earthRadius = 6400000.;

    @Test
    public void testEnums() {
        // this test is here only for test coverage ...

        Assertions.assertEquals(5, Transformer.values().length);
        Assertions.assertSame(Transformer.UNINITIALIZED, Transformer.valueOf("UNINITIALIZED"));
        Assertions.assertSame(Transformer.PLUS,          Transformer.valueOf("PLUS"));
        Assertions.assertSame(Transformer.MINUS,         Transformer.valueOf("MINUS"));
        Assertions.assertSame(Transformer.MIN,           Transformer.valueOf("MIN"));
        Assertions.assertSame(Transformer.MAX,           Transformer.valueOf("MAX"));

        Assertions.assertEquals(2, FilterType.values().length);
        Assertions.assertSame(FilterType.TRIGGER_ONLY_DECREASING_EVENTS,
                          FilterType.valueOf("TRIGGER_ONLY_DECREASING_EVENTS"));
        Assertions.assertSame(FilterType.TRIGGER_ONLY_INCREASING_EVENTS,
                          FilterType.valueOf("TRIGGER_ONLY_INCREASING_EVENTS"));

    }

    @Test
    public void testReplayForward() {
        EclipseDetector detector =
                new EclipseDetector(CelestialBodyFactory.getSun(), sunRadius,
                                     new OneAxisEllipsoid(earthRadius,
                                                          0.0,
                                                          FramesFactory.getITRF(IERSConventions.IERS_2010, true))).
                withMaxCheck(60.0).
                withThreshold(1.0e-3).
                withPenumbra().withHandler(new Counter());
        final EventSlopeFilter<EclipseDetector> filter =
                new EventSlopeFilter<EclipseDetector>(detector, FilterType.TRIGGER_ONLY_INCREASING_EVENTS).
                withMaxIter(200);
        Assertions.assertSame(detector, filter.getDetector());
        Assertions.assertEquals(200, filter.getMaxIterationCount());

        propagator.clearEventsDetectors();
        propagator.addEventDetector(filter);
        propagator.propagate(iniDate, iniDate.shiftedBy(7 * Constants.JULIAN_DAY));
        Assertions.assertEquals(102, ((Counter) detector.getHandler()).getIncreasingCounter());
        Assertions.assertEquals( 0, ((Counter) detector.getHandler()).getDecreasingCounter());
        ((Counter) detector.getHandler()).reset();

        propagator.clearEventsDetectors();
        propagator.setStepHandler(10.0, currentState -> {
            // we exceed the events history in the past,
            // and in this example get stuck with Transformer.MAX
            // transformer, hence the g function is always positive
            // in the test range
            Assertions.assertTrue(filter.g(currentState) > 0);
        });
        propagator.propagate(iniDate.shiftedBy(-3600), iniDate.shiftedBy(Constants.JULIAN_DAY + 3600));
    }

    @Test
    public void testReplayBackward() {
        EclipseDetector detector =
                        new EclipseDetector(CelestialBodyFactory.getSun(), sunRadius,
                                            new OneAxisEllipsoid(earthRadius,
                                                                 0.0,
                                                                 FramesFactory.getITRF(IERSConventions.IERS_2010, true))).
                       withMaxCheck(60.0).
                       withThreshold(1.0e-3).
                       withPenumbra().
                       withHandler(new Counter());
        final EventSlopeFilter<EclipseDetector> filter =
                new EventSlopeFilter<EclipseDetector>(detector, FilterType.TRIGGER_ONLY_DECREASING_EVENTS).
                withMaxIter(200);
        Assertions.assertEquals(200, filter.getMaxIterationCount());

        propagator.clearEventsDetectors();
        propagator.addEventDetector(filter);
        propagator.propagate(iniDate.shiftedBy(7 * Constants.JULIAN_DAY), iniDate);
        Assertions.assertEquals(  0, ((Counter) detector.getHandler()).getIncreasingCounter());
        Assertions.assertEquals(102, ((Counter) detector.getHandler()).getDecreasingCounter());
        ((Counter) detector.getHandler()).reset();

        propagator.clearEventsDetectors();
        propagator.setStepHandler(10.0, currentState -> {
                // we exceed the events history in the past,
                // and in this example get stuck with Transformer.MIN
                // transformer, hence the g function is always negative
                // in the test range
                Assertions.assertTrue(filter.g(currentState) < 0);
            });
        propagator.propagate(iniDate.shiftedBy(7 * Constants.JULIAN_DAY + 3600),
                             iniDate.shiftedBy(6 * Constants.JULIAN_DAY + 3600));
    }

    @Test
    public void testUmbra() {
        EclipseDetector detector =
                        new EclipseDetector(CelestialBodyFactory.getSun(), sunRadius,
                                            new OneAxisEllipsoid(earthRadius,
                                                                 0.0,
                                                                 FramesFactory.getITRF(IERSConventions.IERS_2010, true))).
                       withMaxCheck(60.0).
                       withThreshold(1.0e-3).
                       withPenumbra().
                       withHandler(new Counter());

        propagator.clearEventsDetectors();
        propagator.addEventDetector(detector);
        propagator.propagate(iniDate, iniDate.shiftedBy(Constants.JULIAN_DAY));
        Assertions.assertEquals(14, ((Counter) detector.getHandler()).getIncreasingCounter());
        Assertions.assertEquals(15, ((Counter) detector.getHandler()).getDecreasingCounter());
        ((Counter) detector.getHandler()).reset();

        propagator.clearEventsDetectors();
        propagator.addEventDetector(new EventSlopeFilter<EclipseDetector>(detector, FilterType.TRIGGER_ONLY_INCREASING_EVENTS));
        propagator.propagate(iniDate, iniDate.shiftedBy(Constants.JULIAN_DAY));
        Assertions.assertEquals(14, ((Counter) detector.getHandler()).getIncreasingCounter());
        Assertions.assertEquals( 0, ((Counter) detector.getHandler()).getDecreasingCounter());
        ((Counter) detector.getHandler()).reset();

        propagator.clearEventsDetectors();
        propagator.addEventDetector(new EventSlopeFilter<EclipseDetector>(detector, FilterType.TRIGGER_ONLY_DECREASING_EVENTS));
        propagator.propagate(iniDate, iniDate.shiftedBy(Constants.JULIAN_DAY));
        Assertions.assertEquals( 0, ((Counter) detector.getHandler()).getIncreasingCounter());
        Assertions.assertEquals(15, ((Counter) detector.getHandler()).getDecreasingCounter());

    }

    @Test
    public void testPenumbra() {
        EclipseDetector detector =
                        new EclipseDetector(CelestialBodyFactory.getSun(), sunRadius,
                                            new OneAxisEllipsoid(earthRadius,
                                                                 0.0,
                                                                 FramesFactory.getITRF(IERSConventions.IERS_2010, true))).
                       withMaxCheck(60.0).
                       withThreshold(1.0e-3).
                       withPenumbra().
                       withHandler(new Counter());

        propagator.clearEventsDetectors();
        propagator.addEventDetector(detector);
        propagator.propagate(iniDate, iniDate.shiftedBy(Constants.JULIAN_DAY));
        Assertions.assertEquals(14, ((Counter) detector.getHandler()).getIncreasingCounter());
        Assertions.assertEquals(15, ((Counter) detector.getHandler()).getDecreasingCounter());
        ((Counter) detector.getHandler()).reset();

        propagator.clearEventsDetectors();
        final EventSlopeFilter<EclipseDetector> outOfEclipseDetector =
              new EventSlopeFilter<>(detector, FilterType.TRIGGER_ONLY_INCREASING_EVENTS);
        propagator.addEventDetector(outOfEclipseDetector);
        propagator.propagate(iniDate, iniDate.shiftedBy(Constants.JULIAN_DAY));
        Assertions.assertEquals(14, ((Counter) detector.getHandler()).getIncreasingCounter());
        Assertions.assertEquals(0, ((Counter) detector.getHandler()).getDecreasingCounter());
        Assertions.assertEquals(FilterType.TRIGGER_ONLY_INCREASING_EVENTS, outOfEclipseDetector.getFilter());
        ((Counter) detector.getHandler()).reset();

        propagator.clearEventsDetectors();
        final EventSlopeFilter<EclipseDetector> enteringEclipseDetector =
              new EventSlopeFilter<>(detector, FilterType.TRIGGER_ONLY_DECREASING_EVENTS);
        propagator.addEventDetector(enteringEclipseDetector);
        propagator.propagate(iniDate, iniDate.shiftedBy(Constants.JULIAN_DAY));
        Assertions.assertEquals(0, ((Counter) detector.getHandler()).getIncreasingCounter());
        Assertions.assertEquals(15, ((Counter) detector.getHandler()).getDecreasingCounter());
        Assertions.assertEquals(FilterType.TRIGGER_ONLY_DECREASING_EVENTS, enteringEclipseDetector.getFilter());

    }

    @Test
    public void testForwardIncreasingStartPos() {

        SpacecraftState s = propagator.getInitialState();
        double startLatitude = earth.transform(s.getPosition(),
                                              s.getFrame(), s.getDate()).getLatitude();

        // at start time, the g function is positive
        doTestLatitude(75500.0, startLatitude - 0.1, 12, FilterType.TRIGGER_ONLY_INCREASING_EVENTS);

    }

    @Test
    public void testForwardIncreasingStartZero() {

        SpacecraftState s = propagator.getInitialState();
        double startLatitude = earth.transform(s.getPosition(),
                                              s.getFrame(), s.getDate()).getLatitude();

        // at start time, the g function is exactly 0
        doTestLatitude(75500.0, startLatitude, 12, FilterType.TRIGGER_ONLY_INCREASING_EVENTS);

    }

    @Test
    public void testForwardIncreasingStartNeg() {

        SpacecraftState s = propagator.getInitialState();
        double startLatitude = earth.transform(s.getPosition(),
                                              s.getFrame(), s.getDate()).getLatitude();

        // at start time, the g function is negative
        doTestLatitude(75500.0, startLatitude + 0.1, 13, FilterType.TRIGGER_ONLY_INCREASING_EVENTS);

    }

    @Test
    public void testForwardDecreasingStartPos() {

        SpacecraftState s = propagator.getInitialState();
        double startLatitude = earth.transform(s.getPosition(),
                                              s.getFrame(), s.getDate()).getLatitude();

        // at start time, the g function is positive
        doTestLatitude(75500.0, startLatitude - 0.1, 13, FilterType.TRIGGER_ONLY_DECREASING_EVENTS);
    }

    @Test
    public void testForwardDecreasingStartZero() {

        SpacecraftState s = propagator.getInitialState();
        double startLatitude = earth.transform(s.getPosition(),
                                              s.getFrame(), s.getDate()).getLatitude();

        // at start time, the g function is exactly 0
        doTestLatitude(75500.0, startLatitude, 13, FilterType.TRIGGER_ONLY_DECREASING_EVENTS);
    }

    @Test
    public void testForwardDecreasingStartNeg() {

        SpacecraftState s = propagator.getInitialState();
        double startLatitude = earth.transform(s.getPosition(),
                                              s.getFrame(), s.getDate()).getLatitude();

        // at start time, the g function is negative
        doTestLatitude(75500.0, startLatitude + 0.1, 13, FilterType.TRIGGER_ONLY_DECREASING_EVENTS);
    }

    @Test
    public void testBackwardIncreasingStartPos() {

        SpacecraftState s = propagator.getInitialState();
        double startLatitude = earth.transform(s.getPosition(),
                                              s.getFrame(), s.getDate()).getLatitude();

        // at start time, the g function is positive
        doTestLatitude(-75500.0, startLatitude - 0.1, 13, FilterType.TRIGGER_ONLY_INCREASING_EVENTS);

    }

    @Test
    public void testBackwardIncreasingStartZero() {

        SpacecraftState s = propagator.getInitialState();
        double startLatitude = earth.transform(s.getPosition(),
                                              s.getFrame(), s.getDate()).getLatitude();

        // at start time, the g function is exactly 0
        doTestLatitude(-75500.0, startLatitude, 12, FilterType.TRIGGER_ONLY_INCREASING_EVENTS);

    }

    @Test
    public void testBackwardIncreasingStartNeg() {

        SpacecraftState s = propagator.getInitialState();
        double startLatitude = earth.transform(s.getPosition(),
                                              s.getFrame(), s.getDate()).getLatitude();

        // at start time, the g function is negative
        doTestLatitude(-75500.0, startLatitude + 0.1, 12, FilterType.TRIGGER_ONLY_INCREASING_EVENTS);

    }

    @Test
    public void testBackwardDecreasingStartPos() {

        SpacecraftState s = propagator.getInitialState();
        double startLatitude = earth.transform(s.getPosition(),
                                              s.getFrame(), s.getDate()).getLatitude();

        // at start time, the g function is positive
        doTestLatitude(-75500.0, startLatitude - 0.1, 13, FilterType.TRIGGER_ONLY_DECREASING_EVENTS);
    }

    @Test
    public void testBackwardDecreasingStartZero() {

        SpacecraftState s = propagator.getInitialState();
        double startLatitude = earth.transform(s.getPosition(),
                                              s.getFrame(), s.getDate()).getLatitude();

        // at start time, the g function is exactly 0
        doTestLatitude(-75500.0, startLatitude, 13, FilterType.TRIGGER_ONLY_DECREASING_EVENTS);
    }

    @Test
    public void testBackwardDecreasingStartNeg() {

        SpacecraftState s = propagator.getInitialState();
        double startLatitude = earth.transform(s.getPosition(),
                                              s.getFrame(), s.getDate()).getLatitude();

        // at start time, the g function is negative
        doTestLatitude(-75500.0, startLatitude + 0.1, 13, FilterType.TRIGGER_ONLY_DECREASING_EVENTS);
    }

    private void doTestLatitude(final double dt, final double latitude, final int expected, final FilterType filter)
        {
        final int[] count = new int[2];
        LatitudeCrossingDetector detector =
                new LatitudeCrossingDetector(earth, latitude).
                withMaxCheck(300.0).
                withMaxIter(100).
                withThreshold(1.0e-3).
                withHandler(new EventHandler() {

                    @Override
                    public Action eventOccurred(SpacecraftState s, EventDetector detector, boolean increasing) {
                        Assertions.assertEquals(filter.getTriggeredIncreasing(), increasing);
                        count[0]++;
                        return Action.RESET_STATE;
                    }

                    @Override
                    public SpacecraftState resetState(EventDetector detector, SpacecraftState oldState) {
                        count[1]++;
                        return oldState;
                    }

                });
        Assertions.assertSame(earth, detector.getBody());
        propagator.addEventDetector(new EventSlopeFilter<EventDetector>(detector, filter));
        AbsoluteDate target = propagator.getInitialState().getDate().shiftedBy(dt);
        SpacecraftState finalState = propagator.propagate(target);
        Assertions.assertEquals(0.0, finalState.getDate().durationFrom(target), 1.0e-10);
        Assertions.assertEquals(expected, count[0]);
        Assertions.assertEquals(expected, count[1]);
    }

    private static class Counter implements EventHandler {

        private int increasingCounter;
        private int decreasingCounter;

        public Counter() {
            reset();
        }

        public void reset() {
            increasingCounter = 0;
            decreasingCounter = 0;
        }

        @Override
        public Action eventOccurred(SpacecraftState s, EventDetector ed, boolean increasing) {
            if (increasing) {
                increasingCounter++;
            } else {
                decreasingCounter++;
            }
            return Action.CONTINUE;
        }

        public int getIncreasingCounter() {
            return increasingCounter;
        }

        public int getDecreasingCounter() {
            return decreasingCounter;
        }

    }

    @BeforeEach
    public void setUp() {
        try {
            Utils.setDataRoot("regular-data");
            double mu  = 3.9860047e14;
            final Vector3D position  = new Vector3D(-6142438.668, 3492467.560, -25767.25680);
            final Vector3D velocity  = new Vector3D(505.8479685, 942.7809215, 7435.922231);
            iniDate = new AbsoluteDate(1969, 7, 28, 4, 0, 0.0, TimeScalesFactory.getTT());
            final Orbit orbit = new EquinoctialOrbit(new PVCoordinates(position,  velocity),
                                                     FramesFactory.getGCRF(), iniDate, mu);
            propagator = new KeplerianPropagator(orbit, Utils.defaultLaw(), mu);
            earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                         Constants.WGS84_EARTH_FLATTENING,
                                         FramesFactory.getITRF(IERSConventions.IERS_2010, true));

        } catch (OrekitException oe) {
            Assertions.fail(oe.getLocalizedMessage());
        }
    }

    @AfterEach
    public void tearDown() {
        iniDate    = null;
        propagator = null;
        earth      = null;
    }

}

