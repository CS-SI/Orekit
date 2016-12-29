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
package org.orekit.propagation.events;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.AbstractPropagator;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
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

        Assert.assertEquals(5, Transformer.values().length);
        Assert.assertSame(Transformer.UNINITIALIZED, Transformer.valueOf("UNINITIALIZED"));
        Assert.assertSame(Transformer.PLUS,          Transformer.valueOf("PLUS"));
        Assert.assertSame(Transformer.MINUS,         Transformer.valueOf("MINUS"));
        Assert.assertSame(Transformer.MIN,           Transformer.valueOf("MIN"));
        Assert.assertSame(Transformer.MAX,           Transformer.valueOf("MAX"));

        Assert.assertEquals(2, FilterType.values().length);
        Assert.assertSame(FilterType.TRIGGER_ONLY_DECREASING_EVENTS,
                          FilterType.valueOf("TRIGGER_ONLY_DECREASING_EVENTS"));
        Assert.assertSame(FilterType.TRIGGER_ONLY_INCREASING_EVENTS,
                          FilterType.valueOf("TRIGGER_ONLY_INCREASING_EVENTS"));

    }

    @Test
    public void testReplayForward() throws OrekitException {
        EclipseDetector detector =
                new EclipseDetector(60., 1.e-3,
                                     CelestialBodyFactory.getSun(), sunRadius,
                                     CelestialBodyFactory.getEarth(), earthRadius).
                withPenumbra().withHandler(new Counter());
        final EventSlopeFilter<EclipseDetector> filter =
                new EventSlopeFilter<EclipseDetector>(detector, FilterType.TRIGGER_ONLY_INCREASING_EVENTS).
                withMaxIter(200);
        Assert.assertEquals(200, filter.getMaxIterationCount());

        propagator.clearEventsDetectors();
        propagator.addEventDetector(filter);
        propagator.propagate(iniDate, iniDate.shiftedBy(7 * Constants.JULIAN_DAY));
        Assert.assertEquals(102, ((Counter) detector.getHandler()).getIncreasingCounter());
        Assert.assertEquals( 0, ((Counter) detector.getHandler()).getDecreasingCounter());
        ((Counter) detector.getHandler()).reset();

        propagator.clearEventsDetectors();
        propagator.setMasterMode(10.0, (currentState, isLast) -> {
            // we exceed the events history in the past,
            // and in this example get stuck with Transformer.MAX
            // transformer, hence the g function is always positive
            // in the test range
            Assert.assertTrue(filter.g(currentState) > 0);
        });
        propagator.propagate(iniDate.shiftedBy(-3600), iniDate.shiftedBy(Constants.JULIAN_DAY + 3600));
    }

    @Test
    public void testReplayBackward() throws OrekitException {
        EclipseDetector detector =
                new EclipseDetector(60., 1.e-3,
                                     CelestialBodyFactory.getSun(), sunRadius,
                                     CelestialBodyFactory.getEarth(), earthRadius).
                withPenumbra().withHandler(new Counter());
        final EventSlopeFilter<EclipseDetector> filter =
                new EventSlopeFilter<EclipseDetector>(detector, FilterType.TRIGGER_ONLY_DECREASING_EVENTS).
                withMaxIter(200);
        Assert.assertEquals(200, filter.getMaxIterationCount());

        propagator.clearEventsDetectors();
        propagator.addEventDetector(filter);
        propagator.propagate(iniDate.shiftedBy(7 * Constants.JULIAN_DAY), iniDate);
        Assert.assertEquals(  0, ((Counter) detector.getHandler()).getIncreasingCounter());
        Assert.assertEquals(102, ((Counter) detector.getHandler()).getDecreasingCounter());
        ((Counter) detector.getHandler()).reset();

        propagator.clearEventsDetectors();
        propagator.setMasterMode(10.0, new OrekitFixedStepHandler() {

            @Override
            public void handleStep(SpacecraftState currentState, boolean isLast)
                throws OrekitException {
                // we exceed the events history in the past,
                // and in this example get stuck with Transformer.MIN
                // transformer, hence the g function is always negative
                // in the test range
                Assert.assertTrue(filter.g(currentState) < 0);
            }
        });
        propagator.propagate(iniDate.shiftedBy(7 * Constants.JULIAN_DAY + 3600),
                             iniDate.shiftedBy(6 * Constants.JULIAN_DAY + 3600));
    }

    @Test
    public void testUmbra() throws OrekitException {
        EclipseDetector detector =
                new EclipseDetector(60., 1.e-3,
                                     CelestialBodyFactory.getSun(), sunRadius,
                                     CelestialBodyFactory.getEarth(), earthRadius).
                withPenumbra().withHandler(new Counter());

        propagator.clearEventsDetectors();
        propagator.addEventDetector(detector);
        propagator.propagate(iniDate, iniDate.shiftedBy(Constants.JULIAN_DAY));
        Assert.assertEquals(14, ((Counter) detector.getHandler()).getIncreasingCounter());
        Assert.assertEquals(15, ((Counter) detector.getHandler()).getDecreasingCounter());
        ((Counter) detector.getHandler()).reset();

        propagator.clearEventsDetectors();
        propagator.addEventDetector(new EventSlopeFilter<EclipseDetector>(detector, FilterType.TRIGGER_ONLY_INCREASING_EVENTS));
        propagator.propagate(iniDate, iniDate.shiftedBy(Constants.JULIAN_DAY));
        Assert.assertEquals(14, ((Counter) detector.getHandler()).getIncreasingCounter());
        Assert.assertEquals( 0, ((Counter) detector.getHandler()).getDecreasingCounter());
        ((Counter) detector.getHandler()).reset();

        propagator.clearEventsDetectors();
        propagator.addEventDetector(new EventSlopeFilter<EclipseDetector>(detector, FilterType.TRIGGER_ONLY_DECREASING_EVENTS));
        propagator.propagate(iniDate, iniDate.shiftedBy(Constants.JULIAN_DAY));
        Assert.assertEquals( 0, ((Counter) detector.getHandler()).getIncreasingCounter());
        Assert.assertEquals(15, ((Counter) detector.getHandler()).getDecreasingCounter());

    }

    @Test
    public void testPenumbra() throws OrekitException {
        EclipseDetector detector =
                new EclipseDetector(60., 1.e-3,
                                    CelestialBodyFactory.getSun(), sunRadius,
                                    CelestialBodyFactory.getEarth(), earthRadius).
                withPenumbra().withHandler(new Counter());

        propagator.clearEventsDetectors();
        propagator.addEventDetector(detector);
        propagator.propagate(iniDate, iniDate.shiftedBy(Constants.JULIAN_DAY));
        Assert.assertEquals(14, ((Counter) detector.getHandler()).getIncreasingCounter());
        Assert.assertEquals(15, ((Counter) detector.getHandler()).getDecreasingCounter());
        ((Counter) detector.getHandler()).reset();

        propagator.clearEventsDetectors();
        propagator.addEventDetector(new EventSlopeFilter<EclipseDetector>(detector, FilterType.TRIGGER_ONLY_INCREASING_EVENTS));
        propagator.propagate(iniDate, iniDate.shiftedBy(Constants.JULIAN_DAY));
        Assert.assertEquals(14, ((Counter) detector.getHandler()).getIncreasingCounter());
        Assert.assertEquals( 0, ((Counter) detector.getHandler()).getDecreasingCounter());
        ((Counter) detector.getHandler()).reset();

        propagator.clearEventsDetectors();
        propagator.addEventDetector(new EventSlopeFilter<EclipseDetector>(detector, FilterType.TRIGGER_ONLY_DECREASING_EVENTS));
        propagator.propagate(iniDate, iniDate.shiftedBy(Constants.JULIAN_DAY));
        Assert.assertEquals( 0, ((Counter) detector.getHandler()).getIncreasingCounter());
        Assert.assertEquals(15, ((Counter) detector.getHandler()).getDecreasingCounter());

    }

    @Test
    public void testForwardIncreasingStartPos() throws OrekitException {

        SpacecraftState s = propagator.getInitialState();
        double startLatitude = earth.transform(s.getPVCoordinates().getPosition(),
                                              s.getFrame(), s.getDate()).getLatitude();

        // at start time, the g function is positive
        doTestLatitude(75500.0, startLatitude - 0.1, 12, FilterType.TRIGGER_ONLY_INCREASING_EVENTS);

    }

    @Test
    public void testForwardIncreasingStartZero() throws OrekitException {

        SpacecraftState s = propagator.getInitialState();
        double startLatitude = earth.transform(s.getPVCoordinates().getPosition(),
                                              s.getFrame(), s.getDate()).getLatitude();

        // at start time, the g function is exactly 0
        doTestLatitude(75500.0, startLatitude, 12, FilterType.TRIGGER_ONLY_INCREASING_EVENTS);

    }

    @Test
    public void testForwardIncreasingStartNeg() throws OrekitException {

        SpacecraftState s = propagator.getInitialState();
        double startLatitude = earth.transform(s.getPVCoordinates().getPosition(),
                                              s.getFrame(), s.getDate()).getLatitude();

        // at start time, the g function is negative
        doTestLatitude(75500.0, startLatitude + 0.1, 13, FilterType.TRIGGER_ONLY_INCREASING_EVENTS);

    }

    @Test
    public void testForwardDecreasingStartPos() throws OrekitException {

        SpacecraftState s = propagator.getInitialState();
        double startLatitude = earth.transform(s.getPVCoordinates().getPosition(),
                                              s.getFrame(), s.getDate()).getLatitude();

        // at start time, the g function is positive
        doTestLatitude(75500.0, startLatitude - 0.1, 13, FilterType.TRIGGER_ONLY_DECREASING_EVENTS);
    }

    @Test
    public void testForwardDecreasingStartZero() throws OrekitException {

        SpacecraftState s = propagator.getInitialState();
        double startLatitude = earth.transform(s.getPVCoordinates().getPosition(),
                                              s.getFrame(), s.getDate()).getLatitude();

        // at start time, the g function is exactly 0
        doTestLatitude(75500.0, startLatitude, 13, FilterType.TRIGGER_ONLY_DECREASING_EVENTS);
    }

    @Test
    public void testForwardDecreasingStartNeg() throws OrekitException {

        SpacecraftState s = propagator.getInitialState();
        double startLatitude = earth.transform(s.getPVCoordinates().getPosition(),
                                              s.getFrame(), s.getDate()).getLatitude();

        // at start time, the g function is negative
        doTestLatitude(75500.0, startLatitude + 0.1, 13, FilterType.TRIGGER_ONLY_DECREASING_EVENTS);
    }

    @Test
    public void testBackwardIncreasingStartPos() throws OrekitException {

        SpacecraftState s = propagator.getInitialState();
        double startLatitude = earth.transform(s.getPVCoordinates().getPosition(),
                                              s.getFrame(), s.getDate()).getLatitude();

        // at start time, the g function is positive
        doTestLatitude(-75500.0, startLatitude - 0.1, 13, FilterType.TRIGGER_ONLY_INCREASING_EVENTS);

    }

    @Test
    public void testBackwardIncreasingStartZero() throws OrekitException {

        SpacecraftState s = propagator.getInitialState();
        double startLatitude = earth.transform(s.getPVCoordinates().getPosition(),
                                              s.getFrame(), s.getDate()).getLatitude();

        // at start time, the g function is exactly 0
        doTestLatitude(-75500.0, startLatitude, 12, FilterType.TRIGGER_ONLY_INCREASING_EVENTS);

    }

    @Test
    public void testBackwardIncreasingStartNeg() throws OrekitException {

        SpacecraftState s = propagator.getInitialState();
        double startLatitude = earth.transform(s.getPVCoordinates().getPosition(),
                                              s.getFrame(), s.getDate()).getLatitude();

        // at start time, the g function is negative
        doTestLatitude(-75500.0, startLatitude + 0.1, 12, FilterType.TRIGGER_ONLY_INCREASING_EVENTS);

    }

    @Test
    public void testBackwardDecreasingStartPos() throws OrekitException {

        SpacecraftState s = propagator.getInitialState();
        double startLatitude = earth.transform(s.getPVCoordinates().getPosition(),
                                              s.getFrame(), s.getDate()).getLatitude();

        // at start time, the g function is positive
        doTestLatitude(-75500.0, startLatitude - 0.1, 13, FilterType.TRIGGER_ONLY_DECREASING_EVENTS);
    }

    @Test
    public void testBackwardDecreasingStartZero() throws OrekitException {

        SpacecraftState s = propagator.getInitialState();
        double startLatitude = earth.transform(s.getPVCoordinates().getPosition(),
                                              s.getFrame(), s.getDate()).getLatitude();

        // at start time, the g function is exactly 0
        doTestLatitude(-75500.0, startLatitude, 13, FilterType.TRIGGER_ONLY_DECREASING_EVENTS);
    }

    @Test
    public void testBackwardDecreasingStartNeg() throws OrekitException {

        SpacecraftState s = propagator.getInitialState();
        double startLatitude = earth.transform(s.getPVCoordinates().getPosition(),
                                              s.getFrame(), s.getDate()).getLatitude();

        // at start time, the g function is negative
        doTestLatitude(-75500.0, startLatitude + 0.1, 13, FilterType.TRIGGER_ONLY_DECREASING_EVENTS);
    }

    private void doTestLatitude(final double dt, final double latitude, final int expected, final FilterType filter)
        throws OrekitException {
        final int[] count = new int[2];
        LatitudeCrossingDetector detector =
                new LatitudeCrossingDetector(earth, latitude).
                withMaxCheck(300.0).
                withMaxIter(100).
                withThreshold(1.0e-3).
                withHandler(new EventHandler<LatitudeCrossingDetector>() {

                    @Override
                    public Action eventOccurred(SpacecraftState s,
                                                LatitudeCrossingDetector detector,
                                                boolean increasing) throws OrekitException {
                        Assert.assertEquals(filter.getTriggeredIncreasing(), increasing);
                        count[0]++;
                        return Action.RESET_STATE;
                    }

                    @Override
                    public SpacecraftState resetState(LatitudeCrossingDetector detector,
                                                      SpacecraftState oldState) {
                        count[1]++;
                        return oldState;
                    }

                });
        Assert.assertSame(earth, detector.getBody());
        propagator.addEventDetector(new EventSlopeFilter<EventDetector>(detector, filter));
        AbsoluteDate target = propagator.getInitialState().getDate().shiftedBy(dt);
        SpacecraftState finalState = propagator.propagate(target);
        Assert.assertEquals(0.0, finalState.getDate().durationFrom(target), 1.0e-10);
        Assert.assertEquals(expected, count[0]);
        Assert.assertEquals(expected, count[1]);
    }

    private static class Counter implements EventHandler<EclipseDetector> {

        private int increasingCounter;
        private int decreasingCounter;

        public Counter() {
            reset();
        }

        public void reset() {
            increasingCounter = 0;
            decreasingCounter = 0;
        }

        public Action eventOccurred(SpacecraftState s, EclipseDetector ed, boolean increasing) {
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

    @Before
    public void setUp() {
        try {
            Utils.setDataRoot("regular-data");
            double mu  = 3.9860047e14;
            final Vector3D position  = new Vector3D(-6142438.668, 3492467.560, -25767.25680);
            final Vector3D velocity  = new Vector3D(505.8479685, 942.7809215, 7435.922231);
            iniDate = new AbsoluteDate(1969, 7, 28, 4, 0, 0.0, TimeScalesFactory.getTT());
            final Orbit orbit = new EquinoctialOrbit(new PVCoordinates(position,  velocity),
                                                     FramesFactory.getGCRF(), iniDate, mu);
            propagator = new KeplerianPropagator(orbit, AbstractPropagator.DEFAULT_LAW, mu);
            earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                         Constants.WGS84_EARTH_FLATTENING,
                                         FramesFactory.getITRF(IERSConventions.IERS_2010, true));

        } catch (OrekitException oe) {
            Assert.fail(oe.getLocalizedMessage());
        }
    }

    @After
    public void tearDown() {
        iniDate    = null;
        propagator = null;
        earth      = null;
    }

}

