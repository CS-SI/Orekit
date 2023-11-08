/*
 * Licensed to the Hipparchus project under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.orekit.propagation.events;

import org.hipparchus.ode.events.Action;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.ContinueOnEvent;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.RecordAndContinue;
import org.orekit.propagation.events.handlers.RecordAndContinue.Event;
import org.orekit.propagation.events.handlers.StopOnEvent;
import org.orekit.propagation.sampling.OrekitStepHandler;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Check events are detected correctly when the event times are close.
 *
 * @author Evan Ward
 */
public abstract class CloseEventsAbstractTest {

    public static final AbsoluteDate epoch = AbsoluteDate.J2000_EPOCH;
    public static final double mu = Constants.EIGEN5C_EARTH_MU;
    public static final Frame eci = FramesFactory.getGCRF();
    public static final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING, eci);
    public static final KeplerianOrbit initialOrbit = new KeplerianOrbit(
            6378137 + 500e3, 0, 0, 0, 0, 0, PositionAngleType.TRUE,
            eci, epoch, mu);

    @BeforeAll
    public static void setUpBefore() {
        Utils.setDataRoot("regular-data");
    }

    /**
     * Create a propagator using the {@link #initialOrbit}.
     *
     * @param stepSize   required minimum step of integrator.
     * @return a usable propagator.
     */
    public abstract Propagator getPropagator(double stepSize);

    @Test
    public void testCloseEventsFirstOneIsReset() {
        // setup
        // a fairly rare state to reproduce this bug. Two dates, d1 < d2, that
        // are very close. Event triggers on d1 will reset state to break out of
        // event handling loop in AbstractIntegrator.acceptStep(). At this point
        // detector2 has g0Positive == true but the event time is set to just
        // before the event so g(t0) is negative. Now on processing the
        // next step the root solver checks the sign of the start, midpoint,
        // and end of the interval so we need another event less than half a max
        // check interval after d2 so that the g function will be negative at
        // all three times. Then we get a non bracketing exception.
        Propagator propagator = getPropagator(10.0);

        double t1 = 49, t2 = FastMath.nextUp(t1), t3 = t1 + 4.9;
        List<Event> events = new ArrayList<>();
        TimeDetector detector1 = new TimeDetector(t1)
                .withHandler(new Handler(events, Action.RESET_DERIVATIVES))
                .withMaxCheck(10)
                .withThreshold(1e-9);
        propagator.addEventDetector(detector1);
        TimeDetector detector2 = new TimeDetector(t2, t3)
                .withHandler(new RecordAndContinue(events))
                .withMaxCheck(11)
                .withThreshold(1e-9);
        propagator.addEventDetector(detector2);

        // action
        propagator.propagate(epoch.shiftedBy(60));

        // verify
        Assertions.assertEquals(1, events.size());
        Assertions.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch), 0.0);
        Assertions.assertEquals(detector1, events.get(0).getDetector());
    }

    @Test
    public void testCloseEvents() {
        // setup
        double tolerance = 1;
        Propagator propagator = getPropagator(10);

        RecordAndContinue handler = new RecordAndContinue();
        TimeDetector detector1 = new TimeDetector(5)
                .withHandler(handler)
                .withMaxCheck(10)
                .withThreshold(tolerance);
        propagator.addEventDetector(detector1);
        TimeDetector detector2 = new TimeDetector(5.5)
                .withHandler(handler)
                .withMaxCheck(10)
                .withThreshold(tolerance);
        propagator.addEventDetector(detector2);

        // action
        propagator.propagate(epoch.shiftedBy(20));

        // verify
        List<Event> events = handler.getEvents();
        Assertions.assertEquals(2, events.size());
        Assertions.assertEquals(5, events.get(0).getState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(detector1, events.get(0).getDetector());
        Assertions.assertEquals(5.5, events.get(1).getState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(detector2, events.get(1).getDetector());
    }

    @Test
    public void testSimultaneousEvents() {
        // setup
        Propagator propagator = getPropagator(10);

        RecordAndContinue handler1 = new RecordAndContinue();
        TimeDetector detector1 = new TimeDetector(5)
                .withHandler(handler1)
                .withMaxCheck(10)
                .withThreshold(1);
        propagator.addEventDetector(detector1);
        RecordAndContinue handler2 = new RecordAndContinue();
        TimeDetector detector2 = new TimeDetector(5)
                .withHandler(handler2)
                .withMaxCheck(10)
                .withThreshold(1);
        propagator.addEventDetector(detector2);

        // action
        propagator.propagate(epoch.shiftedBy(20));

        // verify
        List<Event> events1 = handler1.getEvents();
        Assertions.assertEquals(1, events1.size());
        Assertions.assertEquals(5, events1.get(0).getState().getDate().durationFrom(epoch), 0.0);
        List<Event> events2 = handler2.getEvents();
        Assertions.assertEquals(1, events2.size());
        Assertions.assertEquals(5, events2.get(0).getState().getDate().durationFrom(epoch), 0.0);
    }

    /**
     * Previously there were some branches when tryAdvance() returned false but did not
     * set {@code t0 = t}. This allowed the order of events to not be chronological and to
     * detect events that should not have occurred, both of which are problems.
     */
    @Test
    public void testSimultaneousEventsReset() {
        // setup
        double tol = 1e-10;
        Propagator propagator = getPropagator(10);
        boolean[] firstEventOccurred = {false};
        List<Event> events = new ArrayList<>();

        TimeDetector detector1 = new TimeDetector(5)
                .withMaxCheck(10)
                .withThreshold(tol)
                .withHandler(new Handler(events, Action.RESET_STATE) {
                    @Override
                    public Action eventOccurred(SpacecraftState s, EventDetector detector, boolean increasing) {
                        firstEventOccurred[0] = true;
                        return super.eventOccurred(s, detector, increasing);
                    }

                    @Override
                    public SpacecraftState resetState(final EventDetector detector, final SpacecraftState oldState) {
                        return oldState;
                    }
                });
        propagator.addEventDetector(detector1);
        // this detector changes it's g function definition when detector1 fires
        FunctionalDetector detector2 = new FunctionalDetector()
                .withMaxCheck(1)
                .withThreshold(tol)
                .withHandler(new RecordAndContinue(events))
                .withFunction(state -> {
                            if (firstEventOccurred[0]) {
                                return new TimeDetector(1, 3, 5).g(state);
                            }
                            return new TimeDetector(5).g(state);
                        }
                );
        propagator.addEventDetector(detector2);

        // action
        propagator.propagate(epoch.shiftedBy(20));

        // verify
        // order is important to make sure the test checks what it is supposed to
        Assertions.assertEquals(5, events.get(0).getState().getDate().durationFrom(epoch), 0.0);
        Assertions.assertTrue(events.get(0).isIncreasing());
        Assertions.assertEquals(detector1, events.get(0).getDetector());
        Assertions.assertEquals(5, events.get(1).getState().getDate().durationFrom(epoch), 0.0);
        Assertions.assertTrue(events.get(1).isIncreasing());
        Assertions.assertEquals(detector2, events.get(1).getDetector());
        Assertions.assertEquals(2, events.size());
    }

    /**
     * When two event detectors have a discontinuous event caused by a {@link
     * Action#RESET_STATE} or {@link Action#RESET_DERIVATIVES}. The two event detectors
     * would each say they had an event that had to be handled before the other one, but
     * neither would actually back up at all. For #684.
     */
    @Test
    public void testSimultaneousDiscontinuousEventsAfterReset() {
        // setup
        double t = FastMath.PI;
        double tol = 1e-10;
        Propagator propagator = getPropagator(10);
        List<Event> events = new ArrayList<>();
        SpacecraftState newState = new SpacecraftState(new KeplerianOrbit(
                42e6, 0, 0, 0, 0, 0, PositionAngleType.TRUE, eci, epoch.shiftedBy(t), mu));

        TimeDetector resetDetector = new TimeDetector(t)
                .withHandler(new ResetHandler(events, newState))
                .withMaxCheck(10)
                .withThreshold(tol);
        propagator.addEventDetector(resetDetector);
        List<EventDetector> detectors = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            FunctionalDetector detector1 = new FunctionalDetector()
                    .withFunction(s -> s.getA() - 10e6)
                    .withThreshold(tol)
                    .withMaxCheck(10)
                    .withHandler(new RecordAndContinue(events));
            propagator.addEventDetector(detector1);
            detectors.add(detector1);
        }

        // action
        propagator.propagate(epoch.shiftedBy(10));

        // verify
        Assertions.assertEquals(t, events.get(0).getState().getDate().durationFrom(epoch), tol);
        Assertions.assertTrue(events.get(0).isIncreasing());
        Assertions.assertEquals(resetDetector, events.get(0).getDetector());
        // next two events can occur in either order
        Assertions.assertEquals(t, events.get(1).getState().getDate().durationFrom(epoch), tol);
        Assertions.assertTrue(events.get(1).isIncreasing());
        Assertions.assertEquals(detectors.get(0), events.get(1).getDetector());
        Assertions.assertEquals(t, events.get(2).getState().getDate().durationFrom(epoch), tol);
        Assertions.assertTrue(events.get(2).isIncreasing());
        Assertions.assertEquals(detectors.get(1), events.get(2).getDetector());
        Assertions.assertEquals(events.size(), 3);
    }

    /**
     * test the g function switching with a period shorter than the tolerance. We don't
     * need to find any of the events, but we do need to not crash. And we need to
     * preserve the alternating increasing / decreasing sequence.
     */
    @Test
    public void testFastSwitching() {
        // setup
        // step size of 10 to land in between two events we would otherwise miss
        Propagator propagator = getPropagator(10);

        RecordAndContinue handler = new RecordAndContinue();
        TimeDetector detector1 = new TimeDetector(9.9, 10.1, 12)
                .withHandler(handler)
                .withMaxCheck(10)
                .withThreshold(0.2);
        propagator.addEventDetector(detector1);

        // action
        propagator.propagate(epoch.shiftedBy(20));

        //verify
        // finds one or three events. Not 2.
        List<Event> events1 = handler.getEvents();
        Assertions.assertEquals(1, events1.size());
        Assertions.assertEquals(9.9, events1.get(0).getState().getDate().durationFrom(epoch), 0.1);
        Assertions.assertEquals(true, events1.get(0).isIncreasing());
    }

    /** "A Tricky Problem" from bug #239. */
    @Test
    public void testTrickyCaseLower() {
        // setup
        double maxCheck = 10;
        double tolerance = 1e-6;
        double t1 = 1.0, t2 = 15, t3 = 16, t4 = 17, t5 = 18;
        // shared event list so we know the order in which they occurred
        List<Event> events = new ArrayList<>();
        TimeDetector detectorA = new TimeDetector(t3)
                .withHandler(new RecordAndContinue(events))
                .withMaxCheck(maxCheck)
                .withThreshold(tolerance);
        TimeDetector detectorB = new TimeDetector(-10, t1, t2, t5)
                .withHandler(new RecordAndContinue(events))
                .withMaxCheck(maxCheck)
                .withThreshold(tolerance);
        TimeDetector detectorC = new TimeDetector(t4)
                .withHandler(new Handler(events, Action.RESET_DERIVATIVES))
                .withMaxCheck(maxCheck)
                .withThreshold(tolerance);

        Propagator propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);
        propagator.addEventDetector(detectorB);
        propagator.addEventDetector(detectorC);

        // action
        propagator.propagate(epoch.shiftedBy(30));

        //verify
        // really we only care that the Rules of Event Handling are not violated,
        // but I only know one way to do that in this case.
        Assertions.assertEquals(5, events.size());
        Assertions.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(false, events.get(0).isIncreasing());
        Assertions.assertEquals(t2, events.get(1).getState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(true, events.get(1).isIncreasing());
        Assertions.assertEquals(t3, events.get(2).getState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(true, events.get(2).isIncreasing());
        Assertions.assertEquals(t4, events.get(3).getState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(true, events.get(3).isIncreasing());
        Assertions.assertEquals(t5, events.get(4).getState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(false, events.get(4).isIncreasing());
    }

    /**
     * Test case for two event detectors. DetectorA has event at t2, DetectorB at t3, but
     * due to the root finding tolerance DetectorB's event occurs at t1. With t1 < t2 <
     * t3.
     */
    @Test
    public void testRootFindingTolerance() {
        //setup
        double maxCheck = 10;
        double t2 = 11, t3 = t2 + 1e-5;
        List<Event> events = new ArrayList<>();
        TimeDetector detectorA = new TimeDetector(t2)
                .withHandler(new RecordAndContinue(events))
                .withMaxCheck(maxCheck)
                .withThreshold(1e-6);
        FlatDetector detectorB = new FlatDetector(t3)
                .withHandler(new RecordAndContinue(events))
                .withMaxCheck(maxCheck)
                .withThreshold(0.5);
        Propagator propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);
        propagator.addEventDetector(detectorB);

        // action
        propagator.propagate(epoch.shiftedBy(30));

        // verify
        // if these fail the event finding did its job,
        // but this test isn't testing what it is supposed to be
        Assertions.assertSame(detectorB, events.get(0).getDetector());
        Assertions.assertSame(detectorA, events.get(1).getDetector());
        Assertions.assertTrue(events.get(0).getState().getDate().compareTo(
                events.get(1).getState().getDate()) < 0);

        // check event detection worked
        Assertions.assertEquals(2, events.size());
        Assertions.assertEquals(t3, events.get(0).getState().getDate().durationFrom(epoch), 0.5);
        Assertions.assertEquals(true, events.get(0).isIncreasing());
        Assertions.assertEquals(t2, events.get(1).getState().getDate().durationFrom(epoch), 1e-6);
        Assertions.assertEquals(true, events.get(1).isIncreasing());
    }

    /** check when g(t < root) < 0,  g(root + convergence) < 0. */
    @Test
    public void testRootPlusToleranceHasWrongSign() {
        // setup
        double maxCheck = 10;
        double tolerance = 1e-6;
        final double toleranceB = 0.3;
        double t1 = 11, t2 = 11.1, t3 = 11.2;
        // shared event list so we know the order in which they occurred
        List<Event> events = new ArrayList<>();
        TimeDetector detectorA = new TimeDetector(t2)
                .withHandler(new RecordAndContinue(events))
                .withMaxCheck(maxCheck)
                .withThreshold(1e-6);
        TimeDetector detectorB = new TimeDetector(t1, t3)
                .withHandler(new RecordAndContinue(events))
                .withMaxCheck(maxCheck)
                .withThreshold(toleranceB);
        Propagator propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);
        propagator.addEventDetector(detectorB);

        // action
        propagator.propagate(epoch.shiftedBy(30));

        // verify
        // we only care that the rules are satisfied, there are other solutions
        Assertions.assertEquals(3, events.size());
        Assertions.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch), toleranceB);
        Assertions.assertEquals(true, events.get(0).isIncreasing());
        Assertions.assertSame(detectorB, events.get(0).getDetector());
        Assertions.assertEquals(t3, events.get(1).getState().getDate().durationFrom(epoch), toleranceB);
        Assertions.assertEquals(false, events.get(1).isIncreasing());
        Assertions.assertSame(detectorB, events.get(1).getDetector());
        Assertions.assertEquals(t2, events.get(2).getState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(true, events.get(2).isIncreasing());
        Assertions.assertSame(detectorA, events.get(2).getDetector());
        // chronological
        for (int i = 1; i < events.size(); i++) {
            Assertions.assertTrue(events.get(i).getState().getDate().compareTo(
                    events.get(i - 1).getState().getDate()) >= 0);
        }
    }

    /** check when g(t < root) < 0,  g(root + convergence) < 0. */
    @Test
    public void testRootPlusToleranceHasWrongSignAndLessThanTb() {
        // setup
        // test is fragile w.r.t. implementation and these parameters
        double maxCheck = 10;
        double tolerance = 0.5;
        double t1 = 11, t2 = 11.4, t3 = 12.0;
        // shared event list so we know the order in which they occurred
        List<Event> events = new ArrayList<>();
        FlatDetector detectorB = new FlatDetector(t1, t2, t3)
                .withHandler(new RecordAndContinue(events))
                .withMaxCheck(maxCheck)
                .withThreshold(tolerance);
        Propagator propagator = getPropagator(10);
        propagator.addEventDetector(detectorB);

        // action
        propagator.propagate(epoch.shiftedBy(30));

        // verify
        // allowed to find t1 or t3.
        Assertions.assertEquals(1, events.size());
        Assertions.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(true, events.get(0).isIncreasing());
        Assertions.assertSame(detectorB, events.get(0).getDetector());
    }

    /**
     * Check when g(t) has a multiple root. e.g. g(t < root) < 0, g(root) = 0, g(t > root)
     * < 0.
     */
    @Test
    public void testDoubleRoot() {
        // setup
        double maxCheck = 10;
        double tolerance = 1e-6;
        double t1 = 11;
        // shared event list so we know the order in which they occurred
        List<Event> events = new ArrayList<>();
        TimeDetector detectorA = new TimeDetector(t1)
                .withHandler(new RecordAndContinue(events))
                .withMaxCheck(maxCheck)
                .withThreshold(tolerance);
        TimeDetector detectorB = new TimeDetector(t1, t1)
                .withHandler(new RecordAndContinue(events))
                .withMaxCheck(maxCheck)
                .withThreshold(tolerance);
        Propagator propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);
        propagator.addEventDetector(detectorB);

        // action
        propagator.propagate(epoch.shiftedBy(30));

        // verify
        Assertions.assertEquals(1, events.size());
        Assertions.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch), 0.0);
        Assertions.assertEquals(true, events.get(0).isIncreasing());
        Assertions.assertSame(detectorA, events.get(0).getDetector());
        // detector worked correctly
        Assertions.assertTrue(detectorB.g(state(t1)) == 0.0);
        Assertions.assertTrue(detectorB.g(state(t1 - 1e-6)) < 0);
        Assertions.assertTrue(detectorB.g(state(t1 + 1e-6)) < 0);
    }

    /**
     * Check when g(t) has a multiple root. e.g. g(t < root) > 0, g(root) = 0, g(t > root)
     * > 0.
     */
    @Test
    public void testDoubleRootOppositeSign() {
        // setup
        double maxCheck = 10;
        double tolerance = 1e-6;
        double t1 = 11;
        // shared event list so we know the order in which they occurred
        List<Event> events = new ArrayList<>();
        TimeDetector detectorA = new TimeDetector(t1)
                .withHandler(new RecordAndContinue(events))
                .withMaxCheck(maxCheck)
                .withThreshold(tolerance);
        ContinuousDetector detectorB = new ContinuousDetector(-20, t1, t1)
                .withHandler(new RecordAndContinue(events))
                .withMaxCheck(maxCheck)
                .withThreshold(tolerance);
        Propagator propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);
        propagator.addEventDetector(detectorB);

        // action
        propagator.propagate(epoch.shiftedBy(30));

        // verify
        Assertions.assertEquals(1, events.size());
        Assertions.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch), 0.0);
        Assertions.assertEquals(true, events.get(0).isIncreasing());
        Assertions.assertSame(detectorA, events.get(0).getDetector());
        // detector worked correctly
        Assertions.assertEquals(0.0, detectorB.g(state(t1)), 0.0);
        Assertions.assertTrue(detectorB.g(state(t1 - 1e-6)) > 0);
        Assertions.assertTrue(detectorB.g(state(t1 + 1e-6)) > 0);
    }

    /** check root finding when zero at both ends. */
    @Test
    public void testZeroAtBeginningAndEndOfInterval() {
        // setup
        double maxCheck = 10;
        double tolerance = 1e-6;
        double t1 = 10, t2 = 20;
        // shared event list so we know the order in which they occurred
        List<Event> events = new ArrayList<>();
        ContinuousDetector detectorA = new ContinuousDetector(t1, t2)
                .withHandler(new RecordAndContinue(events))
                .withMaxCheck(maxCheck)
                .withThreshold(tolerance);
        Propagator propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);
        // action
        propagator.propagate(epoch.shiftedBy(30));

        // verify
        Assertions.assertEquals(2, events.size());
        Assertions.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(true, events.get(0).isIncreasing());
        Assertions.assertSame(detectorA, events.get(0).getDetector());
        Assertions.assertEquals(t2, events.get(1).getState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(false, events.get(1).isIncreasing());
        Assertions.assertSame(detectorA, events.get(1).getDetector());
    }

    /** check root finding when zero at both ends. */
    @Test
    public void testZeroAtBeginningAndEndOfIntervalOppositeSign() {
        // setup
        double maxCheck = 10;
        double tolerance = 1e-6;
        double t1 = 10, t2 = 20;
        // shared event list so we know the order in which they occurred
        List<Event> events = new ArrayList<>();
        ContinuousDetector detectorA = new ContinuousDetector(-10, t1, t2)
                .withHandler(new RecordAndContinue(events))
                .withMaxCheck(maxCheck)
                .withThreshold(tolerance);
        Propagator propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);

        // action
        propagator.propagate(epoch.shiftedBy(30));

        // verify
        Assertions.assertEquals(2, events.size());
        Assertions.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(false, events.get(0).isIncreasing());
        Assertions.assertSame(detectorA, events.get(0).getDetector());
        Assertions.assertEquals(t2, events.get(1).getState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(true, events.get(1).isIncreasing());
        Assertions.assertSame(detectorA, events.get(1).getDetector());
    }

    /** Test where an event detector has to back up multiple times. */
    @Test
    public void testMultipleBackups() {
        // setup
        double maxCheck = 10;
        double tolerance = 1e-6;
        double t1 = 1.0, t2 = 2, t3 = 3, t4 = 4, t5 = 5, t6 = 6.5, t7 = 7;
        // shared event list so we know the order in which they occurred
        List<Event> events = new ArrayList<>();
        ContinuousDetector detectorA = new ContinuousDetector(t6)
                .withHandler(new RecordAndContinue(events))
                .withMaxCheck(maxCheck)
                .withThreshold(tolerance);
        FlatDetector detectorB = new FlatDetector(t1, t3, t4, t7)
                .withHandler(new RecordAndContinue(events))
                .withMaxCheck(maxCheck)
                .withThreshold(tolerance);
        ContinuousDetector detectorC = new ContinuousDetector(t2, t5)
                .withHandler(new RecordAndContinue(events))
                .withMaxCheck(maxCheck)
                .withThreshold(tolerance);

        Propagator propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);
        propagator.addEventDetector(detectorB);
        propagator.addEventDetector(detectorC);

        // action
        propagator.propagate(epoch.shiftedBy(30));

        //verify
        // need at least 5 events to check that multiple backups occurred
        Assertions.assertEquals(5, events.size());
        Assertions.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(true, events.get(0).isIncreasing());
        Assertions.assertEquals(detectorB, events.get(0).getDetector());
        Assertions.assertEquals(t2, events.get(1).getState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(true, events.get(1).isIncreasing());
        Assertions.assertEquals(detectorC, events.get(1).getDetector());
        // reporting t3 and t4 is optional, seeing them is not.
        // we know a root was found at t3 because events are reported at t2 and t5.
        /*
        Assertions.assertEquals(t3, events.get(2).getT(), tolerance);
        Assertions.assertEquals(false, events.get(2).isIncreasing());
        Assertions.assertEquals(detectorB, events.get(2).getHandler());
        Assertions.assertEquals(t4, events.get(3).getT(), tolerance);
        Assertions.assertEquals(true, events.get(3).isIncreasing());
        Assertions.assertEquals(detectorB, events.get(3).getHandler());
        */
        Assertions.assertEquals(t5, events.get(2).getState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(false, events.get(2).isIncreasing());
        Assertions.assertEquals(detectorC, events.get(2).getDetector());
        Assertions.assertEquals(t6, events.get(3).getState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(true, events.get(3).isIncreasing());
        Assertions.assertEquals(detectorA, events.get(3).getDetector());
        Assertions.assertEquals(t7, events.get(4).getState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(false, events.get(4).isIncreasing());
        Assertions.assertEquals(detectorB, events.get(4).getDetector());
    }

    /** Test a reset event triggering another event at the same time. */
    @Test
    public void testEventCausedByStateReset() {
        // setup
        double maxCheck = 10;
        double tolerance = 1e-6;
        double t1 = 15.0;
        SpacecraftState newState = new SpacecraftState(new KeplerianOrbit(
                6378137 + 500e3, 0, FastMath.PI / 2, 0, 0,
                FastMath.PI / 2, PositionAngleType.TRUE, eci, epoch.shiftedBy(t1), mu));
        // shared event list so we know the order in which they occurred
        List<Event> events = new ArrayList<>();
        TimeDetector detectorA = new TimeDetector(t1)
                .withHandler(new Handler(events, Action.RESET_STATE) {
                    @Override
                    public SpacecraftState resetState(EventDetector detector,
                                                      SpacecraftState oldState) {
                        return newState;
                    }
                })
                .withMaxCheck(maxCheck)
                .withThreshold(tolerance);
        LatitudeCrossingDetector detectorB =
                new LatitudeCrossingDetector(earth, FastMath.toRadians(80))
                        .withHandler(new RecordAndContinue(events))
                        .withMaxCheck(maxCheck)
                        .withThreshold(tolerance);

        Propagator propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);
        propagator.addEventDetector(detectorB);

        // action
        propagator.propagate(epoch.shiftedBy(40.0));

        //verify
        // really we only care that the Rules of Event Handling are not violated,
        Assertions.assertEquals(2, events.size());
        Assertions.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(true, events.get(0).isIncreasing());
        Assertions.assertEquals(detectorA, events.get(0).getDetector());
        Assertions.assertEquals(t1, events.get(1).getState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(true, events.get(1).isIncreasing());
        Assertions.assertEquals(detectorB, events.get(1).getDetector());
    }

    /** check when t + tolerance == t. */
    @Test
    public void testConvergenceTooTight() {
        // setup
        double maxCheck = 10;
        double tolerance = 1e-18;
        double t1 = 15;
        // shared event list so we know the order in which they occurred
        List<Event> events = new ArrayList<>();
        ContinuousDetector detectorA = new ContinuousDetector(t1)
                .withHandler(new RecordAndContinue(events))
                .withMaxCheck(maxCheck)
                .withThreshold(tolerance);
        Propagator propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);

        // action
        propagator.propagate(epoch.shiftedBy(30));

        // verify
        Assertions.assertEquals(1, events.size());
        Assertions.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch), 0.0);
        Assertions.assertEquals(true, events.get(0).isIncreasing());
        Assertions.assertSame(detectorA, events.get(0).getDetector());
    }

    /**
     * test when one event detector changes the definition of another's g function before
     * the end of the step as a result of a continue action. Not sure if this should be
     * officially supported, but it is used in Orekit's DateDetector, it's useful, and not
     * too hard to implement.
     */
    @Test
    public void testEventChangesGFunctionDefinition() {
        // setup
        double maxCheck = 5;
        double tolerance = 1e-6;
        double t1 = 11, t2 = 19;
        // shared event list so we know the order in which they occurred
        List<Event> events = new ArrayList<>();
        // mutable boolean
        boolean[] swap = new boolean[1];
        final ContinuousDetector detectorA = new ContinuousDetector(t1)
                .withThreshold(maxCheck)
                .withThreshold(tolerance)
                .withHandler(new RecordAndContinue(events) {
                    @Override
                    public Action eventOccurred(SpacecraftState s,
                                                EventDetector detector,
                                                boolean increasing) {
                        swap[0] = true;
                        return super.eventOccurred(s, detector, increasing);
                    }
                });
        ContinuousDetector detectorB = new ContinuousDetector(t2);
        EventDetector detectorC = new FunctionalDetector()
                .withMaxCheck(maxCheck)
                .withThreshold(tolerance)
                .withMaxIter(100)
                .withHandler(new RecordAndContinue(events))
                .withFunction(s -> swap[0] ? detectorB.g(s) : -1);
        Propagator propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);
        propagator.addEventDetector(detectorC);

        // action
        propagator.propagate(epoch.shiftedBy(30));

        // verify
        Assertions.assertEquals(2, events.size());
        Assertions.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(true, events.get(0).isIncreasing());
        Assertions.assertSame(detectorA, events.get(0).getDetector());
        Assertions.assertEquals(t2, events.get(1).getState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(true, events.get(1).isIncreasing());
        Assertions.assertSame(detectorC, events.get(1).getDetector());
    }

    /**
     * test when one event detector changes the definition of another's g function before
     * the end of the step as a result of an event occurring. In this case the change
     * cancels the occurrence of the event.
     */
    @Test
    public void testEventChangesGFunctionDefinitionCancel() {
        // setup
        double maxCheck = 5;
        double tolerance = 1e-6;
        double t1 = 11, t2 = 11.1;
        // shared event list so we know the order in which they occurred
        List<Event> events = new ArrayList<>();
        // mutable boolean
        boolean[] swap = new boolean[1];
        final ContinuousDetector detectorA = new ContinuousDetector(t1)
                .withMaxCheck(maxCheck)
                .withThreshold(tolerance)
                .withHandler(new RecordAndContinue(events) {
                    @Override
                    public Action eventOccurred(SpacecraftState state,
                                                EventDetector detector,
                                                boolean increasing) {
                        swap[0] = true;
                        super.eventOccurred(state, detector, increasing);
                        return Action.RESET_EVENTS;
                    }
                });
        final ContinuousDetector detectorB = new ContinuousDetector(t2);
        EventDetector detectorC = new FunctionalDetector()
                .withMaxCheck(maxCheck)
                .withThreshold(tolerance)
                .withMaxIter(100)
                .withHandler(new RecordAndContinue(events))
                .withFunction(state -> swap[0] ? -1 : detectorB.g(state));
        Propagator propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);
        propagator.addEventDetector(detectorC);

        // action
        propagator.propagate(epoch.shiftedBy(30));

        // verify
        Assertions.assertEquals(1, events.size());
        Assertions.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(true, events.get(0).isIncreasing());
        Assertions.assertSame(detectorA, events.get(0).getDetector());
    }

    /**
     * test when one event detector changes the definition of another's g function before
     * the end of the step as a result of an event occurring. In this case the change
     * delays the occurrence of the event.
     */
    @Test
    public void testEventChangesGFunctionDefinitionDelay() {
        // setup
        double maxCheck = 5;
        double tolerance = 1e-6;
        double t1 = 11, t2 = 11.1, t3 = 11.2;
        // shared event list so we know the order in which they occurred
        List<Event> events = new ArrayList<>();
        // mutable boolean
        boolean[] swap = new boolean[1];
        final ContinuousDetector detectorA = new ContinuousDetector(t1)
                .withMaxCheck(maxCheck)
                .withThreshold(tolerance)
                .withHandler(new RecordAndContinue(events) {
                    @Override
                    public Action eventOccurred(SpacecraftState state,
                                                EventDetector detector,
                                                boolean increasing) {
                        swap[0] = true;
                        super.eventOccurred(state, detector, increasing);
                        return Action.RESET_EVENTS;
                    }
                });
        final ContinuousDetector detectorB = new ContinuousDetector(t2);
        final ContinuousDetector detectorD = new ContinuousDetector(t3);
        EventDetector detectorC = new FunctionalDetector()
                .withMaxCheck(maxCheck)
                .withThreshold(tolerance)
                .withMaxIter(100)
                .withHandler(new RecordAndContinue(events))
                .withFunction(state -> swap[0] ? detectorD.g(state) : detectorB.g(state));
        Propagator propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);
        propagator.addEventDetector(detectorC);

        // action
        propagator.propagate(epoch.shiftedBy(30));

        // verify
        Assertions.assertEquals(2, events.size());
        Assertions.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(true, events.get(0).isIncreasing());
        Assertions.assertSame(detectorA, events.get(0).getDetector());
        Assertions.assertEquals(t3, events.get(1).getState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(true, events.get(1).isIncreasing());
        Assertions.assertSame(detectorC, events.get(1).getDetector());
    }

    /**
     * test when one event detector changes the definition of another's g function before
     * the end of the step as a result of an event occurring. In this case the change
     * causes the event to happen sooner than originally expected.
     */
    @Test
    public void testEventChangesGFunctionDefinitionAccelerate() {
        // setup
        double maxCheck = 5;
        double tolerance = 1e-6;
        double t1 = 11, t2 = 11.1, t3 = 11.2;
        // shared event list so we know the order in which they occurred
        List<Event> events = new ArrayList<>();
        // mutable boolean
        boolean[] swap = new boolean[1];
        final ContinuousDetector detectorA = new ContinuousDetector(t1)
                .withMaxCheck(maxCheck)
                .withThreshold(tolerance)
                .withHandler(new RecordAndContinue(events) {
                    @Override
                    public Action eventOccurred(SpacecraftState state,
                                                EventDetector detector,
                                                boolean increasing) {
                        swap[0] = true;
                        super.eventOccurred(state, detector, increasing);
                        return Action.RESET_EVENTS;
                    }
                });
        final ContinuousDetector detectorB = new ContinuousDetector(t2);
        final ContinuousDetector detectorD = new ContinuousDetector(t3);
        EventDetector detectorC = new FunctionalDetector()
                .withMaxCheck(maxCheck)
                .withThreshold(tolerance)
                .withMaxIter(100)
                .withHandler(new RecordAndContinue(events))
                .withFunction(state -> swap[0] ? detectorB.g(state) : detectorD.g(state));
        Propagator propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);
        propagator.addEventDetector(detectorC);

        // action
        propagator.propagate(epoch.shiftedBy(30));

        // verify
        Assertions.assertEquals(2, events.size());
        Assertions.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(true, events.get(0).isIncreasing());
        Assertions.assertSame(detectorA, events.get(0).getDetector());
        Assertions.assertEquals(t2, events.get(1).getState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(true, events.get(1).isIncreasing());
        Assertions.assertSame(detectorC, events.get(1).getDetector());
    }

    /** check when root finding tolerance > event finding tolerance. */
    @Test
    public void testToleranceStop() {
        // setup
        double maxCheck = 10;
        double tolerance = 1e-18; // less than 1 ulp
        double t1 = 15.1;
        // shared event list so we know the order in which they occurred
        List<Event> events = new ArrayList<>();
        FlatDetector detectorA = new FlatDetector(t1)
                .withHandler(new Handler(events, Action.STOP))
                .withMaxCheck(maxCheck)
                .withThreshold(tolerance);
        Propagator propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);

        // action
        SpacecraftState finalState = propagator.propagate(epoch.shiftedBy(30));

        // verify
        Assertions.assertEquals(1, events.size());
        // use root finder tolerance instead of event finder tolerance.
        Assertions.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(true, events.get(0).isIncreasing());
        Assertions.assertSame(detectorA, events.get(0).getDetector());
        Assertions.assertEquals(t1, finalState.getDate().durationFrom(epoch), tolerance);

        // try to resume propagation
        finalState = propagator.propagate(epoch.shiftedBy(30));

        // verify it got to the end
        Assertions.assertEquals(30.0, finalState.getDate().durationFrom(epoch), 0.0);
    }

    /**
     * The root finder requires the start point to be in the interval (a, b) which is hard
     * when there aren't many numbers between a and b. This test uses a second event
     * detector to force a very small window for the first event detector.
     */
    @Test
    public void testShortBracketingInterval() {
        // setup
        double maxCheck = 10;
        double tolerance = 1e-6;
        final double t1 = FastMath.nextUp(10.0), t2 = 10.5;
        // shared event list so we know the order in which they occurred
        List<Event> events = new ArrayList<>();
        // never zero so there is no easy way out
        EventDetector detectorA = new FunctionalDetector()
                .withMaxCheck(maxCheck)
                .withThreshold(tolerance)
                .withMaxIter(100)
                .withHandler(new RecordAndContinue(events))
                .withFunction(state -> {
                    final AbsoluteDate t = state.getDate();
                    if (t.compareTo(epoch.shiftedBy(t1)) < 0) {
                        return -1;
                    } else if (t.compareTo(epoch.shiftedBy(t2)) < 0) {
                        return 1;
                    } else {
                        return -1;
                    }
                });
        TimeDetector detectorB = new TimeDetector(t1)
                .withHandler(new RecordAndContinue(events))
                .withMaxCheck(maxCheck)
                .withThreshold(tolerance);
        Propagator propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);
        propagator.addEventDetector(detectorB);

        // action
        propagator.propagate(epoch.shiftedBy(30.0));

        // verify
        Assertions.assertEquals(3, events.size());
        Assertions.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(true, events.get(0).isIncreasing());
        Assertions.assertSame(detectorA, events.get(0).getDetector());
        Assertions.assertEquals(t1, events.get(1).getState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(true, events.get(1).isIncreasing());
        Assertions.assertSame(detectorB, events.get(1).getDetector());
        Assertions.assertEquals(t2, events.get(2).getState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(false, events.get(2).isIncreasing());
        Assertions.assertSame(detectorA, events.get(2).getDetector());
    }

    /** check when root finding tolerance > event finding tolerance. */
    @Test
    public void testToleranceMaxIterations() {
        // setup
        double maxCheck = 10;
        double tolerance = 1e-18; // less than 1 ulp
        AbsoluteDate t1 = epoch.shiftedBy(15).shiftedBy(FastMath.ulp(15.0) / 8);
        // shared event list so we know the order in which they occurred
        List<Event> events = new ArrayList<>();
        FlatDetector detectorA = new FlatDetector(t1)
                .withHandler(new RecordAndContinue(events))
                .withMaxCheck(maxCheck)
                .withThreshold(tolerance);
        Propagator propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);

        // action
        propagator.propagate(epoch.shiftedBy(30));

        // verify
        Assertions.assertEquals(1, events.size());
        // use root finder tolerance instead of event finder tolerance.
        Assertions.assertEquals(t1.durationFrom(epoch),
                events.get(0).getState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(true, events.get(0).isIncreasing());
        Assertions.assertSame(detectorA, events.get(0).getDetector());
    }

    /** Check that steps are restricted correctly with a continue event. */
    @Test
    public void testEventStepHandler() {
        // setup
        double tolerance = 1e-18;
        Propagator propagator = getPropagator(10);
        propagator.addEventDetector(new TimeDetector(5)
                .withHandler(new Handler(Action.CONTINUE))
                .withThreshold(tolerance));
        StepHandler stepHandler = new StepHandler();
        propagator.setStepHandler(stepHandler);
        AbsoluteDate target = epoch.shiftedBy(10);

        // action
        SpacecraftState finalState = propagator.propagate(target);

        // verify
        Assertions.assertEquals(10.0, finalState.getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(0.0,
                stepHandler.initialState.getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(10.0, stepHandler.targetDate.durationFrom(epoch), tolerance);
        Assertions.assertEquals(10.0,
                stepHandler.finalState.getDate().durationFrom(epoch), tolerance);
        OrekitStepInterpolator interpolator = stepHandler.interpolators.get(0);
        Assertions.assertEquals(0.0,
                interpolator.getPreviousState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(5.0,
                interpolator.getCurrentState().getDate().durationFrom(epoch), tolerance);
        interpolator = stepHandler.interpolators.get(1);
        Assertions.assertEquals(5.0,
                interpolator.getPreviousState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(10.0,
                interpolator.getCurrentState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(2, stepHandler.interpolators.size());
    }

    /**
     * Test {@link EventHandler#resetState(EventDetector, SpacecraftState)} returns {@code
     * null}.
     */
    @Test
    public void testEventCausedByDerivativesReset() {
        // setup
        TimeDetector detectorA = new TimeDetector(15.0)
                .withHandler(new Handler(Action.RESET_STATE){
                    @Override
                    public SpacecraftState resetState(EventDetector d, SpacecraftState s) {
                        return null;
                    }
                })
                .withMaxCheck(10)
                .withThreshold(1e-6);
        Propagator propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);

        try {
            // action
            propagator.propagate(epoch.shiftedBy(20.0));
            Assertions.fail("Expected Exception");
        } catch (NullPointerException e) {
            // expected
        }
    }


    /* The following tests are copies of the above tests, except that they propagate in
     * the reverse direction and all the signs on the time values are negated.
     */


    @Test
    public void testCloseEventsFirstOneIsResetReverse() {
        // setup
        // a fairly rare state to reproduce this bug. Two dates, d1 < d2, that
        // are very close. Event triggers on d1 will reset state to break out of
        // event handling loop in AbstractIntegrator.acceptStep(). At this point
        // detector2 has g0Positive == true but the event time is set to just
        // before the event so g(t0) is negative. Now on processing the
        // next step the root solver checks the sign of the start, midpoint,
        // and end of the interval so we need another event less than half a max
        // check interval after d2 so that the g function will be negative at
        // all three times. Then we get a non bracketing exception.
        Propagator propagator = getPropagator(10.0);

        // switched for 9 to 1 to be close to the start of the step
        double t1 = -1;
        Handler handler1 = new Handler(Action.RESET_DERIVATIVES);
        TimeDetector detector1 = new TimeDetector(t1)
                .withHandler(handler1)
                .withMaxCheck(10)
                .withThreshold(1e-9);
        propagator.addEventDetector(detector1);
        RecordAndContinue handler2 = new RecordAndContinue();
        TimeDetector detector2 = new TimeDetector(t1 - 1e-15, t1 - 4.9)
                .withHandler(handler2)
                .withMaxCheck(11)
                .withThreshold(1e-9);
        propagator.addEventDetector(detector2);

        // action
        propagator.propagate(epoch.shiftedBy(-20));

        // verify
        List<Event> events1 = handler1.getEvents();
        Assertions.assertEquals(1, events1.size());
        Assertions.assertEquals(t1, events1.get(0).getState().getDate().durationFrom(epoch), 0.0);
        List<Event> events2 = handler2.getEvents();
        Assertions.assertEquals(0, events2.size());
    }

    @Test
    public void testCloseEventsReverse() {
        // setup
        double tolerance = 1;
        Propagator propagator = getPropagator(10);

        RecordAndContinue handler1 = new RecordAndContinue();
        TimeDetector detector1 = new TimeDetector(-5)
                .withHandler(handler1)
                .withMaxCheck(10)
                .withThreshold(tolerance);
        propagator.addEventDetector(detector1);
        RecordAndContinue handler2 = new RecordAndContinue();
        TimeDetector detector2 = new TimeDetector(-5.5)
                .withHandler(handler2)
                .withMaxCheck(10)
                .withThreshold(tolerance);
        propagator.addEventDetector(detector2);

        // action
        propagator.propagate(epoch.shiftedBy(-20));

        // verify
        List<Event> events1 = handler1.getEvents();
        Assertions.assertEquals(1, events1.size());
        Assertions.assertEquals(-5, events1.get(0).getState().getDate().durationFrom(epoch), tolerance);
        List<Event> events2 = handler2.getEvents();
        Assertions.assertEquals(1, events2.size());
        Assertions.assertEquals(-5.5, events2.get(0).getState().getDate().durationFrom(epoch), tolerance);
    }

    @Test
    public void testSimultaneousEventsReverse() {
        // setup
        Propagator propagator = getPropagator(10);

        RecordAndContinue handler1 = new RecordAndContinue();
        TimeDetector detector1 = new TimeDetector(-5)
                .withHandler(handler1)
                .withMaxCheck(10)
                .withThreshold(1);
        propagator.addEventDetector(detector1);
        RecordAndContinue handler2 = new RecordAndContinue();
        TimeDetector detector2 = new TimeDetector(-5)
                .withHandler(handler2)
                .withMaxCheck(10)
                .withThreshold(1);
        propagator.addEventDetector(detector2);

        // action
        propagator.propagate(epoch.shiftedBy(-20));

        // verify
        List<Event> events1 = handler1.getEvents();
        Assertions.assertEquals(1, events1.size());
        Assertions.assertEquals(-5, events1.get(0).getState().getDate().durationFrom(epoch), 0.0);
        List<Event> events2 = handler2.getEvents();
        Assertions.assertEquals(1, events2.size());
        Assertions.assertEquals(-5, events2.get(0).getState().getDate().durationFrom(epoch), 0.0);
    }

    /**
     * Previously there were some branches when tryAdvance() returned false but did not
     * set {@code t0 = t}. This allowed the order of events to not be chronological and to
     * detect events that should not have occurred, both of which are problems.
     */
    @Test
    public void testSimultaneousEventsResetReverse() {
        // setup
        double tol = 1e-10;
        Propagator propagator = getPropagator(10);
        boolean[] firstEventOccurred = {false};
        List<Event> events = new ArrayList<>();

        TimeDetector detector1 = new TimeDetector(-5)
                .withMaxCheck(10)
                .withThreshold(tol)
                .withHandler(new Handler(events, Action.RESET_STATE) {
                    @Override
                    public Action eventOccurred(SpacecraftState s, EventDetector detector, boolean increasing) {
                        firstEventOccurred[0] = true;
                        return super.eventOccurred(s, detector, increasing);
                    }

                    @Override
                    public SpacecraftState resetState(final EventDetector detector, final SpacecraftState oldState) {
                        return oldState;
                    }
                });
        propagator.addEventDetector(detector1);
        // this detector changes it's g function definition when detector1 fires
        FunctionalDetector detector2 = new FunctionalDetector()
                .withMaxCheck(1)
                .withThreshold(tol)
                .withHandler(new RecordAndContinue(events))
                .withFunction(state -> {
                            if (firstEventOccurred[0]) {
                                return new TimeDetector(-1, -3, -5).g(state);
                            }
                            return new TimeDetector(-5).g(state);
                        }
                );
        propagator.addEventDetector(detector2);

        // action
        propagator.propagate(epoch.shiftedBy(-20));

        // verify
        // order is important to make sure the test checks what it is supposed to
        Assertions.assertEquals(-5, events.get(0).getState().getDate().durationFrom(epoch), 0.0);
        Assertions.assertTrue(events.get(0).isIncreasing());
        Assertions.assertEquals(detector1, events.get(0).getDetector());
        Assertions.assertEquals(-5, events.get(1).getState().getDate().durationFrom(epoch), 0.0);
        Assertions.assertTrue(events.get(1).isIncreasing());
        Assertions.assertEquals(detector2, events.get(1).getDetector());
        Assertions.assertEquals(2, events.size());
    }

    /**
     * test the g function switching with a period shorter than the tolerance. We don't
     * need to find any of the events, but we do need to not crash. And we need to
     * preserve the alternating increasing / decreasing sequence.
     */
    @Test
    public void testFastSwitchingReverse() {
        // setup
        // step size of 10 to land in between two events we would otherwise miss
        Propagator propagator = getPropagator(10);

        List<Event> events = new ArrayList<>();
        TimeDetector detector1 = new TimeDetector(-9.9, -10.1, -12)
                .withHandler(new RecordAndContinue(events))
                .withMaxCheck(10)
                .withThreshold(0.2);
        propagator.addEventDetector(detector1);

        // action
        propagator.propagate(epoch.shiftedBy(-20));

        //verify
        // finds one or three events. Not 2.
        Assertions.assertEquals(1, events.size());
        Assertions.assertEquals(-9.9, events.get(0).getState().getDate().durationFrom(epoch), 0.2);
        Assertions.assertEquals(true, events.get(0).isIncreasing());
    }

    /** "A Tricky Problem" from bug #239. */
    @Test
    public void testTrickyCaseLowerReverse() {
        // setup
        double maxCheck = 10;
        double tolerance = 1e-6;
        double t1 = -1.0, t2 = -15, t3 = -16, t4 = -17, t5 = -18;
        // shared event list so we know the order in which they occurred
        List<Event> events = new ArrayList<>();
        TimeDetector detectorA = new TimeDetector(t3)
                .withHandler(new RecordAndContinue(events))
                .withMaxCheck(maxCheck)
                .withThreshold(tolerance);
        TimeDetector detectorB = new TimeDetector(-50, t1, t2, t5)
                .withHandler(new RecordAndContinue(events))
                .withMaxCheck(maxCheck)
                .withThreshold(tolerance);
        TimeDetector detectorC = new TimeDetector(t4)
                .withHandler(new Handler(events, Action.RESET_DERIVATIVES))
                .withMaxCheck(maxCheck)
                .withThreshold(tolerance);

        Propagator propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);
        propagator.addEventDetector(detectorB);
        propagator.addEventDetector(detectorC);

        // action
        propagator.propagate(epoch.shiftedBy(-30));

        //verify
        // really we only care that the Rules of Event Handling are not violated,
        // but I only know one way to do that in this case.
        Assertions.assertEquals(5, events.size());
        Assertions.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(false, events.get(0).isIncreasing());
        Assertions.assertEquals(t2, events.get(1).getState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(true, events.get(1).isIncreasing());
        Assertions.assertEquals(t3, events.get(2).getState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(true, events.get(2).isIncreasing());
        Assertions.assertEquals(t4, events.get(3).getState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(true, events.get(3).isIncreasing());
        Assertions.assertEquals(t5, events.get(4).getState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(false, events.get(4).isIncreasing());
    }

    /**
     * Test case for two event detectors. DetectorA has event at t2, DetectorB at t3, but
     * due to the root finding tolerance DetectorB's event occurs at t1. With t1 < t2 <
     * t3.
     */
    @Test
    public void testRootFindingToleranceReverse() {
        //setup
        double maxCheck = 10;
        double t2 = -11, t3 = t2 - 1e-5;
        List<Event> events = new ArrayList<>();
        TimeDetector detectorA = new TimeDetector(t2)
                .withHandler(new RecordAndContinue(events))
                .withMaxCheck(maxCheck)
                .withThreshold(1e-6);
        FlatDetector detectorB = new FlatDetector(t3)
                .withHandler(new RecordAndContinue(events))
                .withMaxCheck(maxCheck)
                .withThreshold(0.5);
        Propagator propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);
        propagator.addEventDetector(detectorB);

        // action
        propagator.propagate(epoch.shiftedBy(-30.0));

        // verify
        // if these fail the event finding did its job,
        // but this test isn't testing what it is supposed to be
        Assertions.assertSame(detectorB, events.get(0).getDetector());
        Assertions.assertSame(detectorA, events.get(1).getDetector());
        Assertions.assertTrue(events.get(0).getState().getDate().compareTo(
                events.get(1).getState().getDate()) > 0);

        // check event detection worked
        Assertions.assertEquals(2, events.size());
        Assertions.assertEquals(t3, events.get(0).getState().getDate().durationFrom(epoch), 0.5);
        Assertions.assertEquals(true, events.get(0).isIncreasing());
        Assertions.assertEquals(t2, events.get(1).getState().getDate().durationFrom(epoch), 1e-6);
        Assertions.assertEquals(true, events.get(1).isIncreasing());
    }

    /** check when g(t < root) < 0,  g(root + convergence) < 0. */
    @Test
    public void testRootPlusToleranceHasWrongSignReverse() {
        // setup
        double maxCheck = 10;
        double tolerance = 1e-6;
        final double toleranceB = 0.3;
        double t1 = -11, t2 = -11.1, t3 = -11.2;
        // shared event list so we know the order in which they occurred
        List<Event> events = new ArrayList<>();
        TimeDetector detectorA = new TimeDetector(t2)
                .withHandler(new RecordAndContinue(events))
                .withMaxCheck(maxCheck)
                .withThreshold(tolerance);
        TimeDetector detectorB = new TimeDetector(-50, t1, t3)
                .withHandler(new RecordAndContinue(events))
                .withMaxCheck(maxCheck)
                .withThreshold(toleranceB);
        Propagator propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);
        propagator.addEventDetector(detectorB);

        // action
        propagator.propagate(epoch.shiftedBy(-30.0));

        // verify
        // we only care that the rules are satisfied. There are multiple solutions.
        Assertions.assertEquals(3, events.size());
        Assertions.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch), toleranceB);
        Assertions.assertEquals(true, events.get(0).isIncreasing());
        Assertions.assertSame(detectorB, events.get(0).getDetector());
        Assertions.assertEquals(t3, events.get(1).getState().getDate().durationFrom(epoch), toleranceB);
        Assertions.assertEquals(false, events.get(1).isIncreasing());
        Assertions.assertSame(detectorB, events.get(1).getDetector());
        Assertions.assertEquals(t2, events.get(2).getState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(true, events.get(2).isIncreasing());
        Assertions.assertSame(detectorA, events.get(2).getDetector());
        // ascending order
        Assertions.assertTrue(events.get(0).getState().getDate().compareTo(
                events.get(1).getState().getDate()) >= 0);
        Assertions.assertTrue(events.get(1).getState().getDate().compareTo(
                events.get(2).getState().getDate()) >= 0);
    }

    /** check when g(t < root) < 0,  g(root + convergence) < 0. */
    @Test
    public void testRootPlusToleranceHasWrongSignAndLessThanTbReverse() {
        // setup
        // test is fragile w.r.t. implementation and these parameters
        double maxCheck = 10;
        double tolerance = 0.5;
        double t1 = -11, t2 = -11.4, t3 = -12.0;
        // shared event list so we know the order in which they occurred
        List<Event> events = new ArrayList<>();
        FlatDetector detectorB = new FlatDetector(t1, t2, t3)
                .withHandler(new RecordAndContinue(events))
                .withMaxCheck(maxCheck)
                .withThreshold(tolerance);
        Propagator propagator = getPropagator(10);
        propagator.addEventDetector(detectorB);

        // action
        propagator.propagate(epoch.shiftedBy(-30.0));

        // verify
        // allowed to report t1 or t3.
        Assertions.assertEquals(1, events.size());
        Assertions.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(true, events.get(0).isIncreasing());
        Assertions.assertSame(detectorB, events.get(0).getDetector());
    }

    /**
     * Check when g(t) has a multiple root. e.g. g(t < root) < 0, g(root) = 0, g(t > root)
     * < 0.
     */
    @Test
    public void testDoubleRootReverse() {
        // setup
        double maxCheck = 10;
        double tolerance = 1e-6;
        double t1 = -11;
        // shared event list so we know the order in which they occurred
        List<Event> events = new ArrayList<>();
        TimeDetector detectorA = new TimeDetector(t1)
                .withHandler(new RecordAndContinue(events))
                .withMaxCheck(maxCheck)
                .withThreshold(tolerance);
        TimeDetector detectorB = new TimeDetector(t1, t1)
                .withHandler(new RecordAndContinue(events))
                .withMaxCheck(maxCheck)
                .withThreshold(tolerance);
        Propagator propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);
        propagator.addEventDetector(detectorB);

        // action
        propagator.propagate(epoch.shiftedBy(-30.0));

        // verify
        Assertions.assertEquals(1, events.size());
        Assertions.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch), 0.0);
        Assertions.assertEquals(true, events.get(0).isIncreasing());
        Assertions.assertSame(detectorA, events.get(0).getDetector());
        // detector worked correctly
        Assertions.assertTrue(detectorB.g(state(t1)) == 0.0);
        Assertions.assertTrue(detectorB.g(state(t1 + 1e-6)) < 0);
        Assertions.assertTrue(detectorB.g(state(t1 - 1e-6)) < 0);
    }

    /**
     * Check when g(t) has a multiple root. e.g. g(t < root) > 0, g(root) = 0, g(t > root)
     * > 0.
     */
    @Test
    public void testDoubleRootOppositeSignReverse() {
        // setup
        double maxCheck = 10;
        double tolerance = 1e-6;
        double t1 = -11;
        // shared event list so we know the order in which they occurred
        List<Event> events = new ArrayList<>();
        TimeDetector detectorA = new TimeDetector(t1)
                .withHandler(new RecordAndContinue(events))
                .withMaxCheck(maxCheck)
                .withThreshold(tolerance);
        ContinuousDetector detectorB = new ContinuousDetector(-50, t1, t1)
                .withHandler(new RecordAndContinue(events))
                .withMaxCheck(maxCheck)
                .withThreshold(tolerance);
        detectorB.g(state(t1));
        Propagator propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);
        propagator.addEventDetector(detectorB);

        // action
        propagator.propagate(epoch.shiftedBy(-30.0));

        // verify
        Assertions.assertEquals(1, events.size());
        Assertions.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch), 0.0);
        Assertions.assertEquals(true, events.get(0).isIncreasing());
        Assertions.assertSame(detectorA, events.get(0).getDetector());
        // detector worked correctly
        Assertions.assertEquals(0.0, detectorB.g(state(t1)), 0.0);
        Assertions.assertTrue(detectorB.g(state(t1 + 1e-6)) > 0);
        Assertions.assertTrue(detectorB.g(state(t1 - 1e-6)) > 0);
    }

    /** check root finding when zero at both ends. */
    @Test
    public void testZeroAtBeginningAndEndOfIntervalReverse() {
        // setup
        double maxCheck = 10;
        double tolerance = 1e-6;
        double t1 = -10, t2 = -20;
        // shared event list so we know the order in which they occurred
        List<Event> events = new ArrayList<>();
        ContinuousDetector detectorA = new ContinuousDetector(-50, t1, t2)
                .withHandler(new RecordAndContinue(events))
                .withMaxCheck(maxCheck)
                .withThreshold(tolerance);
        Propagator propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);

        // action
        propagator.propagate(epoch.shiftedBy(-30.0));

        // verify
        Assertions.assertEquals(2, events.size());
        Assertions.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(true, events.get(0).isIncreasing());
        Assertions.assertSame(detectorA, events.get(0).getDetector());
        Assertions.assertEquals(t2, events.get(1).getState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(false, events.get(1).isIncreasing());
        Assertions.assertSame(detectorA, events.get(1).getDetector());
    }

    /** check root finding when zero at both ends. */
    @Test
    public void testZeroAtBeginningAndEndOfIntervalOppositeSignReverse() {
        // setup
        double maxCheck = 10;
        double tolerance = 1e-6;
        double t1 = -10, t2 = -20;
        // shared event list so we know the order in which they occurred
        List<Event> events = new ArrayList<>();
        ContinuousDetector detectorA = new ContinuousDetector(t1, t2)
                .withHandler(new RecordAndContinue(events))
                .withMaxCheck(maxCheck)
                .withThreshold(tolerance);
        Propagator propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);

        // action
        propagator.propagate(epoch.shiftedBy(-30));

        // verify
        Assertions.assertEquals(2, events.size());
        Assertions.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch), 0.0);
        Assertions.assertEquals(false, events.get(0).isIncreasing());
        Assertions.assertSame(detectorA, events.get(0).getDetector());
        Assertions.assertEquals(t2, events.get(1).getState().getDate().durationFrom(epoch), 0.0);
        Assertions.assertEquals(true, events.get(1).isIncreasing());
        Assertions.assertSame(detectorA, events.get(1).getDetector());
    }

    /** Test where an event detector has to back up multiple times. */
    @Test
    public void testMultipleBackupsReverse() {
        // setup
        double maxCheck = 10;
        double tolerance = 1e-6;
        double t1 = -1.0, t2 = -2, t3 = -3, t4 = -4, t5 = -5, t6 = -6.5, t7 = -7;
        // shared event list so we know the order in which they occurred
        List<Event> events = new ArrayList<>();
        ContinuousDetector detectorA = new ContinuousDetector(t6)
                .withHandler(new RecordAndContinue(events))
                .withMaxCheck(maxCheck)
                .withThreshold(tolerance);
        ContinuousDetector detectorB = new ContinuousDetector(-50, t1, t3, t4, t7)
                .withHandler(new RecordAndContinue(events))
                .withMaxCheck(maxCheck)
                .withThreshold(tolerance);
        ContinuousDetector detectorC = new ContinuousDetector(-50, t2, t5)
                .withHandler(new RecordAndContinue(events))
                .withMaxCheck(maxCheck)
                .withThreshold(tolerance);

        Propagator propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);
        propagator.addEventDetector(detectorB);
        propagator.addEventDetector(detectorC);

        // action
        propagator.propagate(epoch.shiftedBy(-30.0));

        //verify
        // really we only care that the Rules of Event Handling are not violated,
        Assertions.assertEquals(5, events.size());
        Assertions.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(true, events.get(0).isIncreasing());
        Assertions.assertEquals(detectorB, events.get(0).getDetector());
        Assertions.assertEquals(t2, events.get(1).getState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(true, events.get(1).isIncreasing());
        Assertions.assertEquals(detectorC, events.get(1).getDetector());
        // reporting t3 and t4 is optional, seeing them is not.
        // we know a root was found at t3 because events are reported at t2 and t5.
        /*
        Assertions.assertEquals(t3, events.get(2).getT(), tolerance);
        Assertions.assertEquals(false, events.get(2).isIncreasing());
        Assertions.assertEquals(detectorB, events.get(2).getHandler());
        Assertions.assertEquals(t4, events.get(3).getT(), tolerance);
        Assertions.assertEquals(true, events.get(3).isIncreasing());
        Assertions.assertEquals(detectorB, events.get(3).getHandler());
        */
        Assertions.assertEquals(t5, events.get(2).getState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(false, events.get(2).isIncreasing());
        Assertions.assertEquals(detectorC, events.get(2).getDetector());
        Assertions.assertEquals(t6, events.get(3).getState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(true, events.get(3).isIncreasing());
        Assertions.assertEquals(detectorA, events.get(3).getDetector());
        Assertions.assertEquals(t7, events.get(4).getState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(false, events.get(4).isIncreasing());
        Assertions.assertEquals(detectorB, events.get(4).getDetector());
    }

    /** Test a reset event triggering another event at the same time. */
    @Test
    public void testEventCausedByStateResetReverse() {
        // setup
        double maxCheck = 10;
        double tolerance = 1e-6;
        double t1 = -15.0;
        SpacecraftState newState = new SpacecraftState(new KeplerianOrbit(
                6378137 + 500e3, 0, FastMath.PI / 2, 0, 0,
                FastMath.PI / 2, PositionAngleType.TRUE, eci, epoch.shiftedBy(t1), mu));
        // shared event list so we know the order in which they occurred
        List<Event> events = new ArrayList<>();
        TimeDetector detectorA = new TimeDetector(t1)
                .withHandler(new Handler(events, Action.RESET_STATE) {
                    @Override
                    public SpacecraftState resetState(EventDetector detector,
                                                      SpacecraftState oldState) {
                        return newState;
                    }
                })
                .withMaxCheck(maxCheck)
                .withThreshold(tolerance);
        LatitudeCrossingDetector detectorB =
                new LatitudeCrossingDetector(earth, FastMath.toRadians(80))
                        .withHandler(new RecordAndContinue(events))
                        .withMaxCheck(maxCheck)
                        .withThreshold(tolerance);

        Propagator propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);
        propagator.addEventDetector(detectorB);

        // action
        propagator.propagate(epoch.shiftedBy(-40.0));

        //verify
        // really we only care that the Rules of Event Handling are not violated,
        Assertions.assertEquals(2, events.size());
        Assertions.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(true, events.get(0).isIncreasing());
        Assertions.assertEquals(detectorA, events.get(0).getDetector());
        Assertions.assertEquals(t1, events.get(1).getState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(false, events.get(1).isIncreasing());
        Assertions.assertEquals(detectorB, events.get(1).getDetector());
    }

    /** check when t + tolerance == t. */
    @Test
    public void testConvergenceTooTightReverse() {
        // setup
        double maxCheck = 10;
        double tolerance = 1e-18;
        double t1 = -15;
        // shared event list so we know the order in which they occurred
        List<Event> events = new ArrayList<>();
        ContinuousDetector detectorA = new ContinuousDetector(t1)
                .withHandler(new RecordAndContinue(events))
                .withMaxCheck(maxCheck)
                .withThreshold(tolerance);
        Propagator propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);

        // action
        propagator.propagate(epoch.shiftedBy(-30.0));

        // verify
        Assertions.assertEquals(1, events.size());
        Assertions.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch), 0.0);
        Assertions.assertEquals(true, events.get(0).isIncreasing());
        Assertions.assertSame(detectorA, events.get(0).getDetector());
    }

    /**
     * test when one event detector changes the definition of another's g function before
     * the end of the step as a result of a continue action. Not sure if this should be
     * officially supported, but it is used in Orekit's DateDetector, it's useful, and not
     * too hard to implement.
     */
    @Test
    public void testEventChangesGFunctionDefinitionReverse() {
        // setup
        double maxCheck = 5;
        double tolerance = 1e-6;
        double t1 = -11, t2 = -19;
        // shared event list so we know the order in which they occurred
        List<Event> events = new ArrayList<>();
        // mutable boolean
        boolean[] swap = new boolean[1];
        ContinuousDetector detectorA = new ContinuousDetector(t1)
                .withMaxCheck(maxCheck)
                .withThreshold(tolerance)
                .withHandler(new RecordAndContinue(events) {
                    @Override
                    public Action eventOccurred(SpacecraftState s,
                                                EventDetector detector,
                                                boolean increasing) {
                        swap[0] = true;
                        return super.eventOccurred(s, detector, increasing);
                    }
                });
        ContinuousDetector detectorB = new ContinuousDetector(t2);
        EventDetector detectorC = new FunctionalDetector()
                .withMaxCheck(maxCheck)
                .withThreshold(tolerance)
                .withMaxIter(100)
                .withHandler(new RecordAndContinue(events))
                .withFunction(state -> swap[0] ? detectorB.g(state) : 1);
        Propagator propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);
        propagator.addEventDetector(detectorC);

        // action
        propagator.propagate(epoch.shiftedBy(-30.0));

        // verify
        Assertions.assertEquals(2, events.size());
        Assertions.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(true, events.get(0).isIncreasing());
        Assertions.assertSame(detectorA, events.get(0).getDetector());
        Assertions.assertEquals(t2, events.get(1).getState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(true, events.get(1).isIncreasing());
        Assertions.assertSame(detectorC, events.get(1).getDetector());
    }

    /**
     * test when one event detector changes the definition of another's g function before
     * the end of the step as a result of an event occurring. In this case the change
     * cancels the occurrence of the event.
     */
    @Test
    public void testEventChangesGFunctionDefinitionCancelReverse() {
        // setup
        double maxCheck = 5;
        double tolerance = 1e-6;
        double t1 = -11, t2 = -11.1;
        // shared event list so we know the order in which they occurred
        List<Event> events = new ArrayList<>();
        // mutable boolean
        boolean[] swap = new boolean[1];
        final ContinuousDetector detectorA = new ContinuousDetector(t1)
                .withMaxCheck(maxCheck)
                .withThreshold(tolerance)
                .withHandler(new RecordAndContinue(events) {
                    @Override
                    public Action eventOccurred(SpacecraftState state,
                                                EventDetector detector,
                                                boolean increasing) {
                        swap[0] = true;
                        super.eventOccurred(state, detector, increasing);
                        return Action.RESET_EVENTS;
                    }
                });
        final ContinuousDetector detectorB = new ContinuousDetector(t2);
        EventDetector detectorC = new FunctionalDetector()
        .withMaxCheck(maxCheck)
                .withThreshold(tolerance)
                .withMaxIter(100)
                .withHandler(new RecordAndContinue(events))
                .withFunction(state -> swap[0] ? 1 : detectorB.g(state));
        Propagator propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);
        propagator.addEventDetector(detectorC);

        // action
        propagator.propagate(epoch.shiftedBy(-30));

        // verify
        Assertions.assertEquals(1, events.size());
        Assertions.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(true, events.get(0).isIncreasing());
        Assertions.assertSame(detectorA, events.get(0).getDetector());
    }

    /**
     * test when one event detector changes the definition of another's g function before
     * the end of the step as a result of an event occurring. In this case the change
     * delays the occurrence of the event.
     */
    @Test
    public void testEventChangesGFunctionDefinitionDelayReverse() {
        // setup
        double maxCheck = 5;
        double tolerance = 1e-6;
        double t1 = -11, t2 = -11.1, t3 = -11.2;
        // shared event list so we know the order in which they occurred
        List<Event> events = new ArrayList<>();
        // mutable boolean
        boolean[] swap = new boolean[1];
        final ContinuousDetector detectorA = new ContinuousDetector(t1)
                .withMaxCheck(maxCheck)
                .withThreshold(tolerance)
                .withHandler(new RecordAndContinue(events) {
                    @Override
                    public Action eventOccurred(SpacecraftState state,
                                                EventDetector detector,
                                                boolean increasing) {
                        swap[0] = true;
                        super.eventOccurred(state, detector, increasing);
                        return Action.RESET_EVENTS;
                    }
                });
        final ContinuousDetector detectorB = new ContinuousDetector(t2);
        final ContinuousDetector detectorD = new ContinuousDetector(t3);
        EventDetector detectorC = new FunctionalDetector()
                .withMaxCheck(maxCheck)
                .withThreshold(tolerance)
                .withMaxIter(100)
                .withHandler(new RecordAndContinue(events))
                .withFunction(state -> swap[0] ? detectorD.g(state) : detectorB.g(state));
        Propagator propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);
        propagator.addEventDetector(detectorC);

        // action
        propagator.propagate(epoch.shiftedBy(-30));

        // verify
        Assertions.assertEquals(2, events.size());
        Assertions.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(true, events.get(0).isIncreasing());
        Assertions.assertSame(detectorA, events.get(0).getDetector());
        Assertions.assertEquals(t3, events.get(1).getState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(true, events.get(1).isIncreasing());
        Assertions.assertSame(detectorC, events.get(1).getDetector());
    }

    /**
     * test when one event detector changes the definition of another's g function before
     * the end of the step as a result of an event occurring. In this case the change
     * causes the event to happen sooner than originally expected.
     */
    @Test
    public void testEventChangesGFunctionDefinitionAccelerateReverse() {
        // setup
        double maxCheck = 5;
        double tolerance = 1e-6;
        double t1 = -11, t2 = -11.1, t3 = -11.2;
        // shared event list so we know the order in which they occurred
        List<Event> events = new ArrayList<>();
        // mutable boolean
        boolean[] swap = new boolean[1];
        final ContinuousDetector detectorA = new ContinuousDetector(t1)
                .withMaxCheck(maxCheck)
                .withThreshold(tolerance)
                .withHandler(new RecordAndContinue(events) {
                    @Override
                    public Action eventOccurred(SpacecraftState state,
                                                EventDetector detector,
                                                boolean increasing) {
                        swap[0] = true;
                        super.eventOccurred(state, detector, increasing);
                        return Action.RESET_EVENTS;
                    }
                });
        final ContinuousDetector detectorB = new ContinuousDetector(t2);
        final ContinuousDetector detectorD = new ContinuousDetector(t3);
        EventDetector detectorC = new FunctionalDetector()
                .withMaxCheck(maxCheck)
                .withThreshold(tolerance)
                .withMaxIter(100)
                .withHandler(new RecordAndContinue(events))
                .withFunction(state -> swap[0] ? detectorB.g(state) : detectorD.g(state));
        Propagator propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);
        propagator.addEventDetector(detectorC);

        // action
        propagator.propagate(epoch.shiftedBy(-30));

        // verify
        Assertions.assertEquals(2, events.size());
        Assertions.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(true, events.get(0).isIncreasing());
        Assertions.assertSame(detectorA, events.get(0).getDetector());
        Assertions.assertEquals(t2, events.get(1).getState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(true, events.get(1).isIncreasing());
        Assertions.assertSame(detectorC, events.get(1).getDetector());
    }

    /** check when root finding tolerance > event finding tolerance. */
    @Test
    public void testToleranceStopReverse() {
        // setup
        double maxCheck = 10;
        double tolerance = 1e-18; // less than 1 ulp
        double t1 = -15.1;
        // shared event list so we know the order in which they occurred
        List<Event> events = new ArrayList<>();
        FlatDetector detectorA = new FlatDetector(t1)
                .withHandler(new Handler(events, Action.STOP))
                .withMaxCheck(maxCheck)
                .withThreshold(tolerance);
        Propagator propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);

        // action
        SpacecraftState finalState = propagator.propagate(epoch.shiftedBy(-30.0));

        // verify
        Assertions.assertEquals(1, events.size());
        // use root finder tolerance instead of event finder tolerance.
        Assertions.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(true, events.get(0).isIncreasing());
        Assertions.assertSame(detectorA, events.get(0).getDetector());
        Assertions.assertEquals(t1, finalState.getDate().durationFrom(epoch), tolerance);

        // try to resume propagation
        finalState = propagator.propagate(epoch.shiftedBy(-30.0));

        // verify it got to the end
        Assertions.assertEquals(-30.0, finalState.getDate().durationFrom(epoch), 0.0);
    }

    /**
     * The root finder requires the start point to be in the interval (a, b) which is hard
     * when there aren't many numbers between a and b. This test uses a second event
     * detector to force a very small window for the first event detector.
     */
    @Test
    public void testShortBracketingIntervalReverse() {
        // setup
        double maxCheck = 10;
        double tolerance = 1e-6;
        final double t1 = FastMath.nextDown(-10.0), t2 = -10.5;
        // shared event list so we know the order in which they occurred
        List<Event> events = new ArrayList<>();
        // never zero so there is no easy way out
        EventDetector detectorA = new FunctionalDetector()
                .withMaxCheck(maxCheck)
                .withThreshold(tolerance)
                .withMaxIter(100)
                .withHandler(new RecordAndContinue(events))
                .withFunction(state -> {
                    final AbsoluteDate t = state.getDate();
                    if (t.compareTo(epoch.shiftedBy(t1)) > 0) {
                        return -1;
                    } else if (t.compareTo(epoch.shiftedBy(t2)) > 0) {
                        return 1;
                    } else {
                        return -1;
                    }
                });
        TimeDetector detectorB = new TimeDetector(t1)
                .withHandler(new RecordAndContinue(events))
                .withMaxCheck(maxCheck)
                .withThreshold(tolerance);
        Propagator propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);
        propagator.addEventDetector(detectorB);

        // action
        propagator.propagate(epoch.shiftedBy(-30.0));

        // verify
        Assertions.assertEquals(3, events.size());
        Assertions.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(false, events.get(0).isIncreasing());
        Assertions.assertSame(detectorA, events.get(0).getDetector());
        Assertions.assertEquals(t1, events.get(1).getState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(true, events.get(1).isIncreasing());
        Assertions.assertSame(detectorB, events.get(1).getDetector());
        Assertions.assertEquals(t2, events.get(2).getState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(true, events.get(2).isIncreasing());
        Assertions.assertSame(detectorA, events.get(2).getDetector());
    }

    /** check when root finding tolerance > event finding tolerance. */
    @Test
    public void testToleranceMaxIterationsReverse() {
        // setup
        double maxCheck = 10;
        double tolerance = 1e-18; // less than 1 ulp
        AbsoluteDate t1 = epoch.shiftedBy(-15).shiftedBy(FastMath.ulp(-15.0) / 8);
        // shared event list so we know the order in which they occurred
        List<Event> events = new ArrayList<>();
        FlatDetector detectorA = new FlatDetector(t1)
                .withHandler(new RecordAndContinue(events))
                .withMaxCheck(maxCheck)
                .withThreshold(tolerance);
        Propagator propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);

        // action
        propagator.propagate(epoch.shiftedBy(-30));

        // verify
        Assertions.assertEquals(1, events.size());
        // use root finder tolerance instead of event finder tolerance.
        Assertions.assertEquals(t1.durationFrom(epoch),
                events.get(0).getState().getDate().durationFrom(epoch),
                FastMath.ulp(-15.0));
        Assertions.assertEquals(true, events.get(0).isIncreasing());
        Assertions.assertSame(detectorA, events.get(0).getDetector());
    }

    /** Check that steps are restricted correctly with a continue event. */
    @Test
    public void testEventStepHandlerReverse() {
        // setup
        double tolerance = 1e-18;
        Propagator propagator = getPropagator(10);
        propagator.addEventDetector(new TimeDetector(-5)
                .withHandler(new Handler(Action.CONTINUE))
                .withThreshold(tolerance));
        StepHandler stepHandler = new StepHandler();
        propagator.setStepHandler(stepHandler);

        // action
        SpacecraftState finalState = propagator.propagate(epoch.shiftedBy(-10));

        // verify
        Assertions.assertEquals(-10.0, finalState.getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(0.0,
                stepHandler.initialState.getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(-10.0, stepHandler.targetDate.durationFrom(epoch), tolerance);
        Assertions.assertEquals(-10.0,
                stepHandler.finalState.getDate().durationFrom(epoch), tolerance);
        OrekitStepInterpolator interpolator = stepHandler.interpolators.get(0);
        Assertions.assertEquals(0.0,
                interpolator.getPreviousState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(-5.0,
                interpolator.getCurrentState().getDate().durationFrom(epoch), tolerance);
        interpolator = stepHandler.interpolators.get(1);
        Assertions.assertEquals(-5.0,
                interpolator.getPreviousState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(-10.0,
                interpolator.getCurrentState().getDate().durationFrom(epoch), tolerance);
        Assertions.assertEquals(2, stepHandler.interpolators.size());
    }

    /**
     * Test {@link EventHandler#resetState(EventDetector, SpacecraftState)} returns {@code
     * null}.
     */
    @Test
    public void testEventCausedByDerivativesResetReverse() {
        // setup
        TimeDetector detectorA = new TimeDetector(-15.0)
                .withHandler(new Handler(Action.RESET_STATE){
                    @Override
                    public SpacecraftState resetState(EventDetector d, SpacecraftState s) {
                        return null;
                    }
                })
                .withMaxCheck(10)
                .withThreshold(1e-6);
        Propagator propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);

        try {
            // action
            propagator.propagate(epoch.shiftedBy(-20.0));
            Assertions.fail("Expected Exception");
        } catch (NullPointerException e) {
            // expected
        }
    }

    @Test
    public void testResetChangesSign() {
        Propagator propagator = getPropagator(2.5);
        AbsoluteDate t0 = propagator.getInitialState().getDate();
        final double small = 1.25e-11;
        ResetChangesSignGenerator eventsGenerator = new ResetChangesSignGenerator(t0, 0.75, 1.125, -0.5 * small).
                                                    withMaxCheck(1).
                                                    withThreshold(small).
                                                    withMaxIter(1000);
        propagator.addEventDetector(eventsGenerator);
        final SpacecraftState end = propagator.propagate(propagator.getInitialState().getDate().shiftedBy(12.5));
        Assertions.assertEquals(2,                 eventsGenerator.getCount());
        Assertions.assertEquals(1.125 + 0.5 * small, end.getDate().durationFrom(t0), 1.0e-12);
    }

    /* utility classes and methods */

    /**
     * Create a state at a time.
     *
     * @param t time of state.
     * @return new state.
     */
    private SpacecraftState state(double t) {
        return new SpacecraftState(
                new KeplerianOrbit(6378137 + 500e3, 0, 0, 0, 0, 0,
                        PositionAngleType.TRUE, eci, epoch.shiftedBy(t),
                        mu));
    }

    private static List<AbsoluteDate> toDates(double[] eventTs) {
        Arrays.sort(eventTs);
        final List<AbsoluteDate> ret = new ArrayList<>();
        for (double eventT : eventTs) {
            ret.add(epoch.shiftedBy(eventT));
        }
        return ret;
    }

    /** Trigger an event at a particular time. */
    protected static class TimeDetector extends AbstractDetector<TimeDetector> {

        /** time of the event to trigger. */
        private final List<AbsoluteDate> eventTs;

        /**
         * Create a detector that finds events at specific times.
         *
         * @param eventTs event times past epoch.
         */
        public TimeDetector(double... eventTs) {
            this(s -> DEFAULT_MAXCHECK, DEFAULT_THRESHOLD, DEFAULT_MAX_ITER,
                 new StopOnEvent(), toDates(eventTs));
        }

        /**
         * Create a detector that finds events at specific times.
         *
         * @param eventTs event times past epoch.
         */
        public TimeDetector(AbsoluteDate... eventTs) {
            this(s -> DEFAULT_MAXCHECK, DEFAULT_THRESHOLD, DEFAULT_MAX_ITER,
                 new StopOnEvent(), Arrays.asList(eventTs));
        }

        private TimeDetector(AdaptableInterval newMaxCheck,
                             double newThreshold,
                             int newMaxIter,
                             EventHandler newHandler,
                             List<AbsoluteDate> dates) {
            super(newMaxCheck, newThreshold, newMaxIter, newHandler);
            this.eventTs = dates;
        }

        @Override
        public double g(SpacecraftState s) {
            final AbsoluteDate t = s.getDate();
            int i = 0;
            while (i < eventTs.size() && t.compareTo(eventTs.get(i)) > 0) {
                i++;
            }
            i--;
            if (i < 0) {
                return t.durationFrom(eventTs.get(0));
            } else {
                int sign = (i % 2) * 2 - 1;
                return -sign * (t.durationFrom(eventTs.get(i)));
            }
        }

        @Override
        protected TimeDetector create(AdaptableInterval newMaxCheck,
                                      double newThreshold,
                                      int newMaxIter,
                                      EventHandler newHandler) {
            return new TimeDetector(
                    newMaxCheck, newThreshold, newMaxIter, newHandler, eventTs);
        }

    }

    /**
     * Same as {@link TimeDetector} except that it has a very flat g function which makes
     * root finding hard.
     */
    private static class FlatDetector extends AbstractDetector<FlatDetector> {

        private final EventDetector g;

        public FlatDetector(double... eventTs) {
            this(s -> DEFAULT_MAXCHECK, DEFAULT_THRESHOLD, DEFAULT_MAX_ITER,
                 new StopOnEvent(), new TimeDetector(eventTs));
        }

        public FlatDetector(AbsoluteDate... eventTs) {
            this(s -> DEFAULT_MAXCHECK, DEFAULT_THRESHOLD, DEFAULT_MAX_ITER,
                 new StopOnEvent(), new TimeDetector(eventTs));
        }

        private FlatDetector(AdaptableInterval newMaxCheck,
                             double newThreshold,
                             int newMaxIter,
                             EventHandler newHandler,
                             EventDetector g) {
            super(newMaxCheck, newThreshold, newMaxIter, newHandler);
            this.g = g;
        }

        @Override
        public double g(SpacecraftState s) {
            return FastMath.signum(g.g(s));
        }

        @Override
        protected FlatDetector create(AdaptableInterval newMaxCheck,
                                      double newThreshold,
                                      int newMaxIter,
                                      EventHandler newHandler) {
            return new FlatDetector(newMaxCheck, newThreshold, newMaxIter, newHandler, g);
        }

    }

    /** quadratic. */
    private static class ContinuousDetector extends AbstractDetector<ContinuousDetector> {

        /** time of the event to trigger. */
        private final List<AbsoluteDate> eventTs;

        public ContinuousDetector(double... eventTs) {
            this(s -> DEFAULT_MAXCHECK, DEFAULT_THRESHOLD, DEFAULT_MAX_ITER,
                 new StopOnEvent(), toDates(eventTs));
        }

        private ContinuousDetector(AdaptableInterval newMaxCheck,
                                   double newThreshold,
                                   int newMaxIter,
                                   EventHandler newHandler,
                                   List<AbsoluteDate> eventDates) {
            super(newMaxCheck, newThreshold, newMaxIter, newHandler);
            this.eventTs = eventDates;
        }

        @Override
        public double g(SpacecraftState s) {
            final AbsoluteDate t = s.getDate();
            int i = 0;
            while (i < eventTs.size() && t.compareTo(eventTs.get(i)) > 0) {
                i++;
            }
            i--;
            if (i < 0) {
                return t.durationFrom(eventTs.get(0));
            } else if (i < eventTs.size() - 1) {
                int sign = (i % 2) * 2 - 1;
                return -sign * (t.durationFrom(eventTs.get(i)))
                        * (eventTs.get(i + 1).durationFrom(t));
            } else {
                int sign = (i % 2) * 2 - 1;
                return -sign * (t.durationFrom(eventTs.get(i)));
            }
        }

        @Override
        protected ContinuousDetector create(
                AdaptableInterval newMaxCheck,
                double newThreshold,
                int newMaxIter,
                EventHandler newHandler) {
            return new ContinuousDetector(
                    newMaxCheck, newThreshold, newMaxIter, newHandler, eventTs);
        }

    }

    private static class Handler extends RecordAndContinue {

        private final Action action;

        public Handler(Action action) {
            this.action = action;
        }

        public Handler(List<Event> events, Action action) {
            super(events);
            this.action = action;
        }

        @Override
        public Action eventOccurred(SpacecraftState s, EventDetector detector, boolean increasing) {
            super.eventOccurred(s, detector, increasing);
            return this.action;
        }

    }

    private static class ResetHandler extends Handler {

        private final SpacecraftState newState;
        private final int times;
        private long i = 0;

        public ResetHandler(List<Event> events, SpacecraftState newState) {
            this(events, newState, Integer.MAX_VALUE);
        }

        public ResetHandler(List<Event> events, SpacecraftState newState, int times) {
            super(events, Action.RESET_STATE);
            this.newState = newState;
            this.times = times;
        }

        @Override
        public Action eventOccurred(final SpacecraftState s, final EventDetector detector, final boolean increasing) {
            super.eventOccurred(s, detector, increasing);
            if (i++ < times) {
                return Action.RESET_STATE;
            }
            return Action.CONTINUE;
        }

        @Override
        public SpacecraftState resetState(EventDetector detector, SpacecraftState oldState) {
            Assertions.assertEquals(0, newState.getDate().durationFrom(oldState.getDate()), 0);
            return newState;
        }
    }

    private static class StepHandler implements OrekitStepHandler {

        private SpacecraftState initialState;
        private AbsoluteDate targetDate;
        private List<OrekitStepInterpolator> interpolators = new ArrayList<>();
        private SpacecraftState finalState;

        @Override
        public void init(SpacecraftState s0, AbsoluteDate t) {
            initialState = s0;
            targetDate = t;
        }

        @Override
        public void handleStep(OrekitStepInterpolator interpolator) {
            interpolators.add(interpolator);
        }

        @Override
        public void finish(SpacecraftState finalState) {
            this.finalState = finalState;
        }
    }

    private class ResetChangesSignGenerator extends AbstractDetector<ResetChangesSignGenerator> {

        final AbsoluteDate t0;
        final double y1;
        final double y2;
        final double change;
        double delta;
        int count;

        public ResetChangesSignGenerator(final AbsoluteDate t0, final double y1, final double y2, final double change) {
            this(s -> DEFAULT_MAXCHECK, DEFAULT_THRESHOLD, DEFAULT_MAX_ITER,
                 new ContinueOnEvent(), t0, y1, y2, change);
        }

        private ResetChangesSignGenerator(final AdaptableInterval newMaxCheck, final double newThreshold, final int newMaxIter,
                                          final EventHandler newHandler,
                                          final AbsoluteDate t0, final double y1, final double y2, final double change ) {
            super(newMaxCheck, newThreshold, newMaxIter, newHandler);
            this.t0     = t0;
            this.y1     = y1;
            this.y2     = y2;
            this.change = change;
            this.delta  = 0;
            this.count  = 0;
        }

        protected ResetChangesSignGenerator create(final AdaptableInterval newMaxCheck, final double newThreshold,
                                                   final int newMaxIter,
                                                   final EventHandler newHandler) {
            return new ResetChangesSignGenerator(newMaxCheck, newThreshold, newMaxIter, newHandler,
                                                 t0, y1, y2, change);
        }

        public double g(SpacecraftState s) {
            double dt = s.getDate().durationFrom(t0) + delta;
            return (dt - y1) * (dt - y2);
        }

        public EventHandler getHandler() {
            return new EventHandler() {
                public Action eventOccurred(SpacecraftState s, EventDetector detector, boolean increasing) {
                    return ++count < 2 ? Action.RESET_STATE : Action.STOP;
                }

                public SpacecraftState resetState(EventDetector detector, SpacecraftState s) {
                    delta = change;
                    return s;
                }
            };
        }

        public int getCount() {
            return count;
        }

    }

}
