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

import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.ode.events.Action;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.FieldEquinoctialOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.propagation.FieldPropagator;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.analytical.FieldKeplerianPropagator;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.IERSConventions;

public class FieldEventSlopeFilterTest {

    private FieldAbsoluteDate<Binary64>     iniDate;
    private FieldPropagator<Binary64>       propagator;
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
        FieldEclipseDetector<Binary64> detector =
                new FieldEclipseDetector<>(Binary64Field.getInstance(),
                                           CelestialBodyFactory.getSun(), sunRadius,
                                           new OneAxisEllipsoid(earthRadius,
                                                          0.0,
                                                          FramesFactory.getITRF(IERSConventions.IERS_2010, true))).
                withMaxCheck(60.0).
                withThreshold(new Binary64(1.0e-3)).
                withPenumbra().withHandler(new Counter());
        final FieldEventSlopeFilter<FieldEclipseDetector<Binary64>, Binary64> filter =
                new FieldEventSlopeFilter<>(detector, FilterType.TRIGGER_ONLY_INCREASING_EVENTS).
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
        propagator.setStepHandler(new Binary64(10.0), currentState -> {
            // we exceed the events history in the past,
            // and in this example get stuck with Transformer.MAX
            // transformer, hence the g function is always positive
            // in the test range
            Assertions.assertTrue(filter.g(currentState).getReal() > 0);
        });
        propagator.propagate(iniDate.shiftedBy(-3600), iniDate.shiftedBy(Constants.JULIAN_DAY + 3600));
    }

    @Test
    public void testReplayBackward() {
        FieldEclipseDetector<Binary64> detector =
                        new FieldEclipseDetector<>(Binary64Field.getInstance(),
                                                   CelestialBodyFactory.getSun(), sunRadius,
                                                   new OneAxisEllipsoid(earthRadius,
                                                                        0.0,
                                                                       FramesFactory.getITRF(IERSConventions.IERS_2010, true))).
                       withMaxCheck(60.0).
                       withThreshold(new Binary64(1.0e-3)).
                       withPenumbra().
                       withHandler(new Counter());
        final FieldEventSlopeFilter<FieldEclipseDetector<Binary64>, Binary64> filter =
                new FieldEventSlopeFilter<>(detector, FilterType.TRIGGER_ONLY_DECREASING_EVENTS).
                withMaxIter(200);
        Assertions.assertEquals(200, filter.getMaxIterationCount());

        propagator.clearEventsDetectors();
        propagator.addEventDetector(filter);
        propagator.propagate(iniDate.shiftedBy(7 * Constants.JULIAN_DAY), iniDate);
        Assertions.assertEquals(  0, ((Counter) detector.getHandler()).getIncreasingCounter());
        Assertions.assertEquals(102, ((Counter) detector.getHandler()).getDecreasingCounter());
        ((Counter) detector.getHandler()).reset();

        propagator.clearEventsDetectors();
        propagator.setStepHandler(new Binary64(10.0), currentState -> {
                // we exceed the events history in the past,
                // and in this example get stuck with Transformer.MIN
                // transformer, hence the g function is always negative
                // in the test range
                Assertions.assertTrue(filter.g(currentState).getReal() < 0);
            });
        propagator.propagate(iniDate.shiftedBy(7 * Constants.JULIAN_DAY + 3600),
                             iniDate.shiftedBy(6 * Constants.JULIAN_DAY + 3600));
    }

    @Test
    public void testUmbra() {
        FieldEclipseDetector<Binary64> detector =
                        new FieldEclipseDetector<>(Binary64Field.getInstance(),
                                                   CelestialBodyFactory.getSun(), sunRadius,
                                                   new OneAxisEllipsoid(earthRadius,
                                                                        0.0,
                                                                       FramesFactory.getITRF(IERSConventions.IERS_2010, true))).
                       withMaxCheck(60.0).
                       withThreshold(new Binary64(1.0e-3)).
                       withPenumbra().
                       withHandler(new Counter());

        propagator.clearEventsDetectors();
        propagator.addEventDetector(detector);
        propagator.propagate(iniDate, iniDate.shiftedBy(Constants.JULIAN_DAY));
        Assertions.assertEquals(14, ((Counter) detector.getHandler()).getIncreasingCounter());
        Assertions.assertEquals(15, ((Counter) detector.getHandler()).getDecreasingCounter());
        ((Counter) detector.getHandler()).reset();

        propagator.clearEventsDetectors();
        propagator.addEventDetector(new FieldEventSlopeFilter<>(detector, FilterType.TRIGGER_ONLY_INCREASING_EVENTS));
        propagator.propagate(iniDate, iniDate.shiftedBy(Constants.JULIAN_DAY));
        Assertions.assertEquals(14, ((Counter) detector.getHandler()).getIncreasingCounter());
        Assertions.assertEquals( 0, ((Counter) detector.getHandler()).getDecreasingCounter());
        ((Counter) detector.getHandler()).reset();

        propagator.clearEventsDetectors();
        propagator.addEventDetector(new FieldEventSlopeFilter<>(detector, FilterType.TRIGGER_ONLY_DECREASING_EVENTS));
        propagator.propagate(iniDate, iniDate.shiftedBy(Constants.JULIAN_DAY));
        Assertions.assertEquals( 0, ((Counter) detector.getHandler()).getIncreasingCounter());
        Assertions.assertEquals(15, ((Counter) detector.getHandler()).getDecreasingCounter());

    }

    @Test
    public void testPenumbra() {
        FieldEclipseDetector<Binary64> detector =
                        new FieldEclipseDetector<>(Binary64Field.getInstance(),
                                                   CelestialBodyFactory.getSun(), sunRadius,
                                                   new OneAxisEllipsoid(earthRadius,
                                                                        0.0,
                                                                       FramesFactory.getITRF(IERSConventions.IERS_2010, true))).
                       withMaxCheck(60.0).
                       withThreshold(new Binary64(1.0e-3)).
                       withPenumbra().
                       withHandler(new Counter());

        propagator.clearEventsDetectors();
        propagator.addEventDetector(detector);
        propagator.propagate(iniDate, iniDate.shiftedBy(Constants.JULIAN_DAY));
        Assertions.assertEquals(14, ((Counter) detector.getHandler()).getIncreasingCounter());
        Assertions.assertEquals(15, ((Counter) detector.getHandler()).getDecreasingCounter());
        ((Counter) detector.getHandler()).reset();

        propagator.clearEventsDetectors();
        propagator.addEventDetector(new FieldEventSlopeFilter<>(detector, FilterType.TRIGGER_ONLY_INCREASING_EVENTS));
        propagator.propagate(iniDate, iniDate.shiftedBy(Constants.JULIAN_DAY));
        Assertions.assertEquals(14, ((Counter) detector.getHandler()).getIncreasingCounter());
        Assertions.assertEquals( 0, ((Counter) detector.getHandler()).getDecreasingCounter());
        ((Counter) detector.getHandler()).reset();

        propagator.clearEventsDetectors();
        propagator.addEventDetector(new FieldEventSlopeFilter<>(detector, FilterType.TRIGGER_ONLY_DECREASING_EVENTS));
        propagator.propagate(iniDate, iniDate.shiftedBy(Constants.JULIAN_DAY));
        Assertions.assertEquals( 0, ((Counter) detector.getHandler()).getIncreasingCounter());
        Assertions.assertEquals(15, ((Counter) detector.getHandler()).getDecreasingCounter());

    }

    @Test
    public void testForwardIncreasingStartPos() {

        FieldSpacecraftState<Binary64> s = propagator.getInitialState();
        Binary64 startLatitude = earth.transform(s.getPosition(),
                                                 s.getFrame(), s.getDate()).getLatitude();

        // at start time, the g function is positive
        doTestLatitude(75500.0, startLatitude.subtract(0.1), 12, FilterType.TRIGGER_ONLY_INCREASING_EVENTS);

    }

    @Test
    public void testForwardIncreasingStartZero() {

        FieldSpacecraftState<Binary64> s = propagator.getInitialState();
        Binary64 startLatitude = earth.transform(s.getPosition(),
                                                 s.getFrame(), s.getDate()).getLatitude();

        // at start time, the g function is exactly 0
        doTestLatitude(75500.0, startLatitude, 12, FilterType.TRIGGER_ONLY_INCREASING_EVENTS);

    }

    @Test
    public void testForwardIncreasingStartNeg() {

        FieldSpacecraftState<Binary64> s = propagator.getInitialState();
        Binary64 startLatitude = earth.transform(s.getPosition(),
                                                 s.getFrame(), s.getDate()).getLatitude();

        // at start time, the g function is negative
        doTestLatitude(75500.0, startLatitude.add(0.1), 13, FilterType.TRIGGER_ONLY_INCREASING_EVENTS);

    }

    @Test
    public void testForwardDecreasingStartPos() {

        FieldSpacecraftState<Binary64> s = propagator.getInitialState();
        Binary64 startLatitude = earth.transform(s.getPosition(),
                                                 s.getFrame(), s.getDate()).getLatitude();

        // at start time, the g function is positive
        doTestLatitude(75500.0, startLatitude.subtract(0.1), 13, FilterType.TRIGGER_ONLY_DECREASING_EVENTS);
    }

    @Test
    public void testForwardDecreasingStartZero() {

        FieldSpacecraftState<Binary64> s = propagator.getInitialState();
        Binary64 startLatitude = earth.transform(s.getPosition(),
                                                 s.getFrame(), s.getDate()).getLatitude();

        // at start time, the g function is exactly 0
        doTestLatitude(75500.0, startLatitude, 13, FilterType.TRIGGER_ONLY_DECREASING_EVENTS);
    }

    @Test
    public void testForwardDecreasingStartNeg() {

        FieldSpacecraftState<Binary64> s = propagator.getInitialState();
        Binary64 startLatitude = earth.transform(s.getPosition(),
                                                 s.getFrame(), s.getDate()).getLatitude();

        // at start time, the g function is negative
        doTestLatitude(75500.0, startLatitude.add(0.1), 13, FilterType.TRIGGER_ONLY_DECREASING_EVENTS);
    }

    @Test
    public void testBackwardIncreasingStartPos() {

        FieldSpacecraftState<Binary64> s = propagator.getInitialState();
        Binary64 startLatitude = earth.transform(s.getPosition(),
                                                 s.getFrame(), s.getDate()).getLatitude();

        // at start time, the g function is positive
        doTestLatitude(-75500.0, startLatitude.subtract(0.1), 13, FilterType.TRIGGER_ONLY_INCREASING_EVENTS);

    }

    @Test
    public void testBackwardIncreasingStartZero() {

        FieldSpacecraftState<Binary64> s = propagator.getInitialState();
        Binary64 startLatitude = earth.transform(s.getPosition(),
                                                 s.getFrame(), s.getDate()).getLatitude();

        // at start time, the g function is exactly 0
        doTestLatitude(-75500.0, startLatitude, 12, FilterType.TRIGGER_ONLY_INCREASING_EVENTS);

    }

    @Test
    public void testBackwardIncreasingStartNeg() {

        FieldSpacecraftState<Binary64> s = propagator.getInitialState();
        Binary64 startLatitude = earth.transform(s.getPosition(),
                                                 s.getFrame(), s.getDate()).getLatitude();

        // at start time, the g function is negative
        doTestLatitude(-75500.0, startLatitude.add(0.1), 12, FilterType.TRIGGER_ONLY_INCREASING_EVENTS);

    }

    @Test
    public void testBackwardDecreasingStartPos() {

        FieldSpacecraftState<Binary64> s = propagator.getInitialState();
        Binary64 startLatitude = earth.transform(s.getPosition(),
                                                 s.getFrame(), s.getDate()).getLatitude();

        // at start time, the g function is positive
        doTestLatitude(-75500.0, startLatitude.subtract(0.1), 13, FilterType.TRIGGER_ONLY_DECREASING_EVENTS);
    }

    @Test
    public void testBackwardDecreasingStartZero() {

        FieldSpacecraftState<Binary64> s = propagator.getInitialState();
        Binary64 startLatitude = earth.transform(s.getPosition(),
                                                 s.getFrame(), s.getDate()).getLatitude();

        // at start time, the g function is exactly 0
        doTestLatitude(-75500.0, startLatitude, 13, FilterType.TRIGGER_ONLY_DECREASING_EVENTS);
    }

    @Test
    public void testBackwardDecreasingStartNeg() {

        FieldSpacecraftState<Binary64> s = propagator.getInitialState();
        Binary64 startLatitude = earth.transform(s.getPosition(),
                                                 s.getFrame(), s.getDate()).getLatitude();

        // at start time, the g function is negative
        doTestLatitude(-75500.0, startLatitude.add(0.1), 13, FilterType.TRIGGER_ONLY_DECREASING_EVENTS);
    }

    private void doTestLatitude(final double dt, final Binary64 latitude, final int expected, final FilterType filter) {
        final int[] count = new int[2];
        FieldLatitudeCrossingDetector<Binary64> detector =
                new FieldLatitudeCrossingDetector<>(latitude.getField(), earth, latitude.getReal()).
                withMaxCheck(300.0).
                withMaxIter(100).
                withThreshold(new Binary64(1.0e-3)).
                withHandler(new FieldEventHandler<Binary64>() {

                    @Override
                    public Action eventOccurred(FieldSpacecraftState<Binary64> s, FieldEventDetector<Binary64> detector, boolean increasing) {
                        Assertions.assertEquals(filter.getTriggeredIncreasing(), increasing);
                        count[0]++;
                        return Action.RESET_STATE;
                    }

                    @Override
                    public FieldSpacecraftState<Binary64> resetState(FieldEventDetector<Binary64> detector, FieldSpacecraftState<Binary64> oldState) {
                        count[1]++;
                        return oldState;
                    }

                });
        Assertions.assertSame(earth, detector.getBody());
        propagator.addEventDetector(new FieldEventSlopeFilter<>(detector, filter));
        FieldAbsoluteDate<Binary64> target = propagator.getInitialState().getDate().shiftedBy(dt);
        FieldSpacecraftState<Binary64> finalState = propagator.propagate(target);
        Assertions.assertEquals(0.0, finalState.getDate().durationFrom(target).getReal(), 1.0e-10);
        Assertions.assertEquals(expected, count[0]);
        Assertions.assertEquals(expected, count[1]);
    }

    private static class Counter implements FieldEventHandler<Binary64> {

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
        public Action eventOccurred(FieldSpacecraftState<Binary64> s, FieldEventDetector<Binary64> ed, boolean increasing) {
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
            Binary64 mu  = new Binary64(3.9860047e14);
            final FieldVector3D<Binary64> position  = new FieldVector3D<>(new Binary64(-6142438.668),
                                                                          new Binary64(3492467.560),
                                                                          new Binary64(-25767.25680));
            final FieldVector3D<Binary64> velocity  = new FieldVector3D<>(new Binary64(505.8479685),
                                                                          new Binary64(942.7809215),
                                                                          new Binary64(7435.922231));
            iniDate = new FieldAbsoluteDate<>(Binary64Field.getInstance(),
                                              new AbsoluteDate(1969, 7, 28, 4, 0, 0.0, TimeScalesFactory.getTT()));
            final FieldOrbit<Binary64> orbit = new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(position,  velocity),
                                                                           FramesFactory.getGCRF(), iniDate, mu);
            propagator = new FieldKeplerianPropagator<>(orbit, Utils.defaultLaw(), mu);
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

