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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.ode.events.Action;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.FieldPropagator;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.propagation.events.handlers.FieldRecordAndContinue;
import org.orekit.propagation.events.handlers.FieldRecordAndContinue.Event;
import org.orekit.propagation.events.handlers.FieldStopOnEvent;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;

/**
 * Check events are detected correctly when the event times are close.
 *
 * @author Evan Ward
 */
public abstract class FieldCloseEventsAbstractTest<T extends RealFieldElement<T>>{

    public static final double mu = Constants.EIGEN5C_EARTH_MU;
    public static final Frame eci = FramesFactory.getGCRF();
    public static final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING, eci);

    public final Field<T> field;
    public final FieldAbsoluteDate<T> epoch;
    public final FieldKeplerianOrbit<T> initialOrbit;

    @BeforeClass
    public static void setUpBefore() {
        Utils.setDataRoot("regular-data");
    }

    public FieldCloseEventsAbstractTest(final Field<T> field) {
        this.field = field;
        this.epoch = new FieldAbsoluteDate<>(field, AbsoluteDate.J2000_EPOCH);
        this.initialOrbit = new FieldKeplerianOrbit<>(
            v(6378137 + 500e3), v(0), v(0), v(0), v(0), v(0), PositionAngle.TRUE,
            eci, epoch, v(mu));
    }

    /**
     * Create a propagator using the {@link #initialOrbit}.
     *
     * @param stepSize   required minimum step of integrator.
     * @return a usable propagator.
     */
    public abstract FieldPropagator<T> getPropagator(double stepSize);

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
        FieldPropagator<T> propagator = getPropagator(10.0);

        double t1 = 49, t2 = t1 + 1e-15, t3 = t1 + 4.9;
        List<Event<FieldEventDetector<T>, T>> events = new ArrayList<>();
        TimeDetector detector1 = new TimeDetector(t1)
                .withHandler(new Handler<>(events, Action.RESET_DERIVATIVES))
                .withMaxCheck(v(10))
                .withThreshold(v(1e-9));
        propagator.addEventDetector(detector1);
        TimeDetector detector2 = new TimeDetector(t2, t3)
                .withHandler(new FieldRecordAndContinue<>(events))
                .withMaxCheck(v(11))
                .withThreshold(v(1e-9));
        propagator.addEventDetector(detector2);

        // action
        propagator.propagate(epoch.shiftedBy(60));

        // verify
        Assert.assertEquals(1, events.size());
        Assert.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch).getReal(), 0.0);
        Assert.assertEquals(detector1, events.get(0).getDetector());
    }

    @Test
    public void testCloseEvents() {
        // setup
        double tolerance = 1;
        FieldPropagator<T> propagator = getPropagator(10);

        FieldRecordAndContinue<FieldEventDetector<T>, T> handler = new FieldRecordAndContinue<>();
        TimeDetector detector1 = new TimeDetector(5)
                .withHandler(handler)
                .withMaxCheck(v(10))
                .withThreshold(v(tolerance));
        propagator.addEventDetector(detector1);
        TimeDetector detector2 = new TimeDetector(5.5)
                .withHandler(handler)
                .withMaxCheck(v(10))
                .withThreshold(v(tolerance));
        propagator.addEventDetector(detector2);

        // action
        propagator.propagate(epoch.shiftedBy(20));

        // verify
        List<Event<FieldEventDetector<T>, T>> events = handler.getEvents();
        Assert.assertEquals(2, events.size());
        Assert.assertEquals(5, events.get(0).getState().getDate().durationFrom(epoch).getReal(), tolerance);
        Assert.assertEquals(detector1, events.get(0).getDetector());
        Assert.assertEquals(5.5, events.get(1).getState().getDate().durationFrom(epoch).getReal(), tolerance);
        Assert.assertEquals(detector2, events.get(1).getDetector());
    }

    @Test
    public void testSimultaneousEvents() {
        // setup
        FieldPropagator<T> propagator = getPropagator(10);

        FieldRecordAndContinue<FieldEventDetector<T>, T> handler1 = new FieldRecordAndContinue<>();
        TimeDetector detector1 = new TimeDetector(5)
                .withHandler(handler1)
                .withMaxCheck(v(10))
                .withThreshold(v(1));
        propagator.addEventDetector(detector1);
        FieldRecordAndContinue<FieldEventDetector<T>, T> handler2 = new FieldRecordAndContinue<>();
        TimeDetector detector2 = new TimeDetector(5)
                .withHandler(handler2)
                .withMaxCheck(v(10))
                .withThreshold(v(1));
        propagator.addEventDetector(detector2);

        // action
        propagator.propagate(epoch.shiftedBy(20));

        // verify
        List<Event<FieldEventDetector<T>, T>> events1 = handler1.getEvents();
        Assert.assertEquals(1, events1.size());
        Assert.assertEquals(5, events1.get(0).getState().getDate().durationFrom(epoch).getReal(), 0.0);
        List<Event<FieldEventDetector<T>, T>> events2 = handler2.getEvents();
        Assert.assertEquals(1, events2.size());
        Assert.assertEquals(5, events2.get(0).getState().getDate().durationFrom(epoch).getReal(), 0.0);
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
        FieldPropagator<T> propagator = getPropagator(10);

        FieldRecordAndContinue<FieldEventDetector<T>, T> handler = new FieldRecordAndContinue<>();
        TimeDetector detector1 = new TimeDetector(9.9, 10.1, 12)
                .withHandler(handler)
                .withMaxCheck(v(10))
                .withThreshold(v(0.2));
        propagator.addEventDetector(detector1);

        // action
        propagator.propagate(epoch.shiftedBy(20));

        //verify
        // finds one or three events. Not 2.
        List<Event<FieldEventDetector<T>, T>> events1 = handler.getEvents();
        Assert.assertEquals(1, events1.size());
        Assert.assertEquals(9.9, events1.get(0).getState().getDate().durationFrom(epoch).getReal(), 0.1);
        Assert.assertEquals(true, events1.get(0).isIncreasing());
    }

    /** "A Tricky Problem" from bug #239. */
    @Test
    public void testTrickyCaseLower() {
        // setup
        double maxCheck = 10;
        double tolerance = 1e-6;
        double t1 = 1.0, t2 = 15, t3 = 16, t4 = 17, t5 = 18;
        // shared event list so we know the order in which they occurred
        List<Event<FieldEventDetector<T>, T>> events = new ArrayList<>();
        TimeDetector detectorA = new TimeDetector(t3)
                .withHandler(new FieldRecordAndContinue<>(events))
                .withMaxCheck(v(maxCheck))
                .withThreshold(v(tolerance));
        TimeDetector detectorB = new TimeDetector(-10, t1, t2, t5)
                .withHandler(new FieldRecordAndContinue<>(events))
                .withMaxCheck(v(maxCheck))
                .withThreshold(v(tolerance));
        TimeDetector detectorC = new TimeDetector(t4)
                .withHandler(new Handler<>(events, Action.RESET_DERIVATIVES))
                .withMaxCheck(v(maxCheck))
                .withThreshold(v(tolerance));

        FieldPropagator<T> propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);
        propagator.addEventDetector(detectorB);
        propagator.addEventDetector(detectorC);

        // action
        propagator.propagate(epoch.shiftedBy(30));

        //verify
        // really we only care that the Rules of Event Handling are not violated,
        // but I only know one way to do that in this case.
        Assert.assertEquals(5, events.size());
        Assert.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch).getReal(), tolerance);
        Assert.assertEquals(false, events.get(0).isIncreasing());
        Assert.assertEquals(t2, events.get(1).getState().getDate().durationFrom(epoch).getReal(), tolerance);
        Assert.assertEquals(true, events.get(1).isIncreasing());
        Assert.assertEquals(t3, events.get(2).getState().getDate().durationFrom(epoch).getReal(), tolerance);
        Assert.assertEquals(true, events.get(2).isIncreasing());
        Assert.assertEquals(t4, events.get(3).getState().getDate().durationFrom(epoch).getReal(), tolerance);
        Assert.assertEquals(true, events.get(3).isIncreasing());
        Assert.assertEquals(t5, events.get(4).getState().getDate().durationFrom(epoch).getReal(), tolerance);
        Assert.assertEquals(false, events.get(4).isIncreasing());
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
        List<Event<FieldEventDetector<T>, T>> events = new ArrayList<>();
        TimeDetector detectorA = new TimeDetector(t2)
                .withHandler(new FieldRecordAndContinue<>(events))
                .withMaxCheck(v(maxCheck))
                .withThreshold(v(1e-6));
        FlatDetector detectorB = new FlatDetector(t3)
                .withHandler(new FieldRecordAndContinue<>(events))
                .withMaxCheck(v(maxCheck))
                .withThreshold(v(0.5));
        FieldPropagator<T> propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);
        propagator.addEventDetector(detectorB);

        // action
        propagator.propagate(epoch.shiftedBy(30));

        // verify
        // if these fail the event finding did its job,
        // but this test isn't testing what it is supposed to be
        Assert.assertSame(detectorB, events.get(0).getDetector());
        Assert.assertSame(detectorA, events.get(1).getDetector());
        Assert.assertTrue(events.get(0).getState().getDate().compareTo(
                events.get(1).getState().getDate()) < 0);

        // check event detection worked
        Assert.assertEquals(2, events.size());
        Assert.assertEquals(t3, events.get(0).getState().getDate().durationFrom(epoch).getReal(), 0.5);
        Assert.assertEquals(true, events.get(0).isIncreasing());
        Assert.assertEquals(t2, events.get(1).getState().getDate().durationFrom(epoch).getReal(), 1e-6);
        Assert.assertEquals(true, events.get(1).isIncreasing());
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
        List<Event<FieldEventDetector<T>, T>> events = new ArrayList<>();
        TimeDetector detectorA = new TimeDetector(t2)
                .withHandler(new FieldRecordAndContinue<>(events))
                .withMaxCheck(v(maxCheck))
                .withThreshold(v(1e-6));
        TimeDetector detectorB = new TimeDetector(t1, t3)
                .withHandler(new FieldRecordAndContinue<>(events))
                .withMaxCheck(v(maxCheck))
                .withThreshold(v(toleranceB));
        FieldPropagator<T> propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);
        propagator.addEventDetector(detectorB);

        // action
        propagator.propagate(epoch.shiftedBy(30));

        // verify
        // we only care that the rules are satisfied, there are other solutions
        Assert.assertEquals(3, events.size());
        Assert.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch).getReal(), toleranceB);
        Assert.assertEquals(true, events.get(0).isIncreasing());
        Assert.assertSame(detectorB, events.get(0).getDetector());
        // events at t2 and t3 may occur in either order since toleranceB > (t3 - t2)
        try {
            Assert.assertEquals(t2, events.get(1).getState().getDate().durationFrom(epoch).getReal(), tolerance);
            Assert.assertEquals(true, events.get(1).isIncreasing());
            Assert.assertSame(detectorA, events.get(1).getDetector());
            Assert.assertEquals(t3, events.get(2).getState().getDate().durationFrom(epoch).getReal(), toleranceB);
            Assert.assertEquals(false, events.get(2).isIncreasing());
            Assert.assertSame(detectorB, events.get(2).getDetector());
        } catch (AssertionError e) {
            Assert.assertEquals(t3, events.get(1).getState().getDate().durationFrom(epoch).getReal(), toleranceB);
            Assert.assertEquals(false, events.get(1).isIncreasing());
            Assert.assertSame(detectorB, events.get(1).getDetector());
            Assert.assertEquals(t2, events.get(2).getState().getDate().durationFrom(epoch).getReal(), tolerance);
            Assert.assertEquals(true, events.get(2).isIncreasing());
            Assert.assertSame(detectorA, events.get(2).getDetector());
        }
        // chronological
        for (int i = 1; i < events.size(); i++) {
            Assert.assertTrue(events.get(i).getState().getDate().compareTo(
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
        List<Event<FieldEventDetector<T>, T>> events = new ArrayList<>();
        FlatDetector detectorB = new FlatDetector(t1, t2, t3)
                .withHandler(new FieldRecordAndContinue<>(events))
                .withMaxCheck(v(maxCheck))
                .withThreshold(v(tolerance));
        FieldPropagator<T> propagator = getPropagator(10);
        propagator.addEventDetector(detectorB);

        // action
        propagator.propagate(epoch.shiftedBy(30));

        // verify
        // allowed to find t1 or t3.
        Assert.assertEquals(1, events.size());
        Assert.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch).getReal(), tolerance);
        Assert.assertEquals(true, events.get(0).isIncreasing());
        Assert.assertSame(detectorB, events.get(0).getDetector());
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
        List<Event<FieldEventDetector<T>, T>> events = new ArrayList<>();
        TimeDetector detectorA = new TimeDetector(t1)
                .withHandler(new FieldRecordAndContinue<>(events))
                .withMaxCheck(v(maxCheck))
                .withThreshold(v(tolerance));
        TimeDetector detectorB = new TimeDetector(t1, t1)
                .withHandler(new FieldRecordAndContinue<>(events))
                .withMaxCheck(v(maxCheck))
                .withThreshold(v(tolerance));
        FieldPropagator<T> propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);
        propagator.addEventDetector(detectorB);

        // action
        propagator.propagate(epoch.shiftedBy(30));

        // verify
        Assert.assertEquals(1, events.size());
        Assert.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch).getReal(), 0.0);
        Assert.assertEquals(true, events.get(0).isIncreasing());
        Assert.assertSame(detectorA, events.get(0).getDetector());
        // detector worked correctly
        Assert.assertTrue(detectorB.g(state(t1)).getReal() == 0.0);
        Assert.assertTrue(detectorB.g(state(t1 - 1e-6)).getReal() < 0);
        Assert.assertTrue(detectorB.g(state(t1 + 1e-6)).getReal() < 0);
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
        List<Event<FieldEventDetector<T>, T>> events = new ArrayList<>();
        TimeDetector detectorA = new TimeDetector(t1)
                .withHandler(new FieldRecordAndContinue<>(events))
                .withMaxCheck(v(maxCheck))
                .withThreshold(v(tolerance));
        ContinuousDetector detectorB = new ContinuousDetector(-20, t1, t1)
                .withHandler(new FieldRecordAndContinue<>(events))
                .withMaxCheck(v(maxCheck))
                .withThreshold(v(tolerance));
        FieldPropagator<T> propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);
        propagator.addEventDetector(detectorB);

        // action
        propagator.propagate(epoch.shiftedBy(30));

        // verify
        Assert.assertEquals(1, events.size());
        Assert.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch).getReal(), 0.0);
        Assert.assertEquals(true, events.get(0).isIncreasing());
        Assert.assertSame(detectorA, events.get(0).getDetector());
        // detector worked correctly
        Assert.assertEquals(0.0, detectorB.g(state(t1)).getReal(), 0.0);
        Assert.assertTrue(detectorB.g(state(t1 - 1e-6)).getReal() > 0);
        Assert.assertTrue(detectorB.g(state(t1 + 1e-6)).getReal() > 0);
    }

    /** check root finding when zero at both ends. */
    @Test
    public void testZeroAtBeginningAndEndOfInterval() {
        // setup
        double maxCheck = 10;
        double tolerance = 1e-6;
        double t1 = 10, t2 = 20;
        // shared event list so we know the order in which they occurred
        List<Event<FieldEventDetector<T>, T>> events = new ArrayList<>();
        ContinuousDetector detectorA = new ContinuousDetector(t1, t2)
                .withHandler(new FieldRecordAndContinue<>(events))
                .withMaxCheck(v(maxCheck))
                .withThreshold(v(tolerance));
        FieldPropagator<T> propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);
        // action
        propagator.propagate(epoch.shiftedBy(30));

        // verify
        Assert.assertEquals(2, events.size());
        Assert.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch).getReal(), tolerance);
        Assert.assertEquals(true, events.get(0).isIncreasing());
        Assert.assertSame(detectorA, events.get(0).getDetector());
        Assert.assertEquals(t2, events.get(1).getState().getDate().durationFrom(epoch).getReal(), tolerance);
        Assert.assertEquals(false, events.get(1).isIncreasing());
        Assert.assertSame(detectorA, events.get(1).getDetector());
    }

    /** check root finding when zero at both ends. */
    @Test
    public void testZeroAtBeginningAndEndOfIntervalOppositeSign() {
        // setup
        double maxCheck = 10;
        double tolerance = 1e-6;
        double t1 = 10, t2 = 20;
        // shared event list so we know the order in which they occurred
        List<Event<FieldEventDetector<T>, T>> events = new ArrayList<>();
        ContinuousDetector detectorA = new ContinuousDetector(-10, t1, t2)
                .withHandler(new FieldRecordAndContinue<>(events))
                .withMaxCheck(v(maxCheck))
                .withThreshold(v(tolerance));
        FieldPropagator<T> propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);

        // action
        propagator.propagate(epoch.shiftedBy(30));

        // verify
        Assert.assertEquals(2, events.size());
        Assert.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch).getReal(), tolerance);
        Assert.assertEquals(false, events.get(0).isIncreasing());
        Assert.assertSame(detectorA, events.get(0).getDetector());
        Assert.assertEquals(t2, events.get(1).getState().getDate().durationFrom(epoch).getReal(), tolerance);
        Assert.assertEquals(true, events.get(1).isIncreasing());
        Assert.assertSame(detectorA, events.get(1).getDetector());
    }

    /** Test where an event detector has to back up multiple times. */
    @Test
    public void testMultipleBackups() {
        // setup
        double maxCheck = 10;
        double tolerance = 1e-6;
        double t1 = 1.0, t2 = 2, t3 = 3, t4 = 4, t5 = 5, t6 = 6.5, t7 = 7;
        // shared event list so we know the order in which they occurred
        List<Event<FieldEventDetector<T>, T>> events = new ArrayList<>();
        ContinuousDetector detectorA = new ContinuousDetector(t6)
                .withHandler(new FieldRecordAndContinue<>(events))
                .withMaxCheck(v(maxCheck))
                .withThreshold(v(tolerance));
        FlatDetector detectorB = new FlatDetector(t1, t3, t4, t7)
                .withHandler(new FieldRecordAndContinue<>(events))
                .withMaxCheck(v(maxCheck))
                .withThreshold(v(tolerance));
        ContinuousDetector detectorC = new ContinuousDetector(t2, t5)
                .withHandler(new FieldRecordAndContinue<>(events))
                .withMaxCheck(v(maxCheck))
                .withThreshold(v(tolerance));

        FieldPropagator<T> propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);
        propagator.addEventDetector(detectorB);
        propagator.addEventDetector(detectorC);

        // action
        propagator.propagate(epoch.shiftedBy(30));

        //verify
        // need at least 5 events to check that multiple backups occurred
        Assert.assertEquals(5, events.size());
        Assert.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch).getReal(), tolerance);
        Assert.assertEquals(true, events.get(0).isIncreasing());
        Assert.assertEquals(detectorB, events.get(0).getDetector());
        Assert.assertEquals(t2, events.get(1).getState().getDate().durationFrom(epoch).getReal(), tolerance);
        Assert.assertEquals(true, events.get(1).isIncreasing());
        Assert.assertEquals(detectorC, events.get(1).getDetector());
        // reporting t3 and t4 is optional, seeing them is not.
        // we know a root was found at t3 because events are reported at t2 and t5.
        /*
        Assert.assertEquals(t3, events.get(2).getT(), tolerance);
        Assert.assertEquals(false, events.get(2).isIncreasing());
        Assert.assertEquals(detectorB, events.get(2).getHandler());
        Assert.assertEquals(t4, events.get(3).getT(), tolerance);
        Assert.assertEquals(true, events.get(3).isIncreasing());
        Assert.assertEquals(detectorB, events.get(3).getHandler());
        */
        Assert.assertEquals(t5, events.get(2).getState().getDate().durationFrom(epoch).getReal(), tolerance);
        Assert.assertEquals(false, events.get(2).isIncreasing());
        Assert.assertEquals(detectorC, events.get(2).getDetector());
        Assert.assertEquals(t6, events.get(3).getState().getDate().durationFrom(epoch).getReal(), tolerance);
        Assert.assertEquals(true, events.get(3).isIncreasing());
        Assert.assertEquals(detectorA, events.get(3).getDetector());
        Assert.assertEquals(t7, events.get(4).getState().getDate().durationFrom(epoch).getReal(), tolerance);
        Assert.assertEquals(false, events.get(4).isIncreasing());
        Assert.assertEquals(detectorB, events.get(4).getDetector());
    }

    /** Test a reset event triggering another event at the same time. */
    @Test
    public void testEventCausedByStateReset() {
        // setup
        double maxCheck = 10;
        double tolerance = 1e-6;
        double t1 = 15.0;
        FieldSpacecraftState<T> newState = new FieldSpacecraftState<>(new FieldKeplerianOrbit<>(
                v(6378137 + 500e3), v(0), v(FastMath.PI / 2), v(0), v(0),
                v(FastMath.PI / 2), PositionAngle.TRUE, eci, epoch.shiftedBy(t1), v(mu)));
        // shared event list so we know the order in which they occurred
        List<Event<FieldEventDetector<T>, T>> events = new ArrayList<>();
        TimeDetector detectorA = new TimeDetector(t1)
                .withHandler(new Handler<FieldEventDetector<T>>(events, Action.RESET_STATE) {
                    @Override
                    public FieldSpacecraftState<T> resetState(FieldEventDetector<T> detector,
                                                      FieldSpacecraftState<T> oldState) {
                        return newState;
                    }
                })
                .withMaxCheck(v(maxCheck))
                .withThreshold(v(tolerance));
        FieldLatitudeCrossingDetector<T> detectorB =
                new FieldLatitudeCrossingDetector<T>(field, earth, FastMath.toRadians(80))
                        .withHandler(new FieldRecordAndContinue<>(events))
                        .withMaxCheck(v(maxCheck))
                        .withThreshold(v(tolerance));

        FieldPropagator<T> propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);
        propagator.addEventDetector(detectorB);

        // action
        propagator.propagate(epoch.shiftedBy(40.0));

        //verify
        // really we only care that the Rules of Event Handling are not violated,
        Assert.assertEquals(2, events.size());
        Assert.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch).getReal(), tolerance);
        Assert.assertEquals(true, events.get(0).isIncreasing());
        Assert.assertEquals(detectorA, events.get(0).getDetector());
        Assert.assertEquals(t1, events.get(1).getState().getDate().durationFrom(epoch).getReal(), tolerance);
        Assert.assertEquals(true, events.get(1).isIncreasing());
        Assert.assertEquals(detectorB, events.get(1).getDetector());
    }

    /** check when t + tolerance == t. */
    @Test
    public void testConvergenceTooTight() {
        // setup
        double maxCheck = 10;
        double tolerance = 1e-18;
        double t1 = 15;
        // shared event list so we know the order in which they occurred
        List<Event<FieldEventDetector<T>, T>> events = new ArrayList<>();
        ContinuousDetector detectorA = new ContinuousDetector(t1)
                .withHandler(new FieldRecordAndContinue<>(events))
                .withMaxCheck(v(maxCheck))
                .withThreshold(v(tolerance));
        FieldPropagator<T> propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);

        // action
        propagator.propagate(epoch.shiftedBy(30));

        // verify
        Assert.assertEquals(1, events.size());
        Assert.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch).getReal(), 0.0);
        Assert.assertEquals(true, events.get(0).isIncreasing());
        Assert.assertSame(detectorA, events.get(0).getDetector());
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
        List<Event<FieldEventDetector<T>, T>> events = new ArrayList<>();
        // mutable boolean
        boolean[] swap = new boolean[1];
        final ContinuousDetector detectorA = new ContinuousDetector(t1)
                .withThreshold(v(maxCheck))
                .withThreshold(v(tolerance))
                .withHandler(new FieldRecordAndContinue<FieldEventDetector<T>, T>(events) {
                    @Override
                    public Action eventOccurred(FieldSpacecraftState<T> s,
                                                FieldEventDetector<T> detector,
                                                boolean increasing) {
                        swap[0] = true;
                        return super.eventOccurred(s, detector, increasing);
                    }
                });
        ContinuousDetector detectorB = new ContinuousDetector(t2);
        FieldEventDetector<T> detectorC = new FieldAbstractDetector<FieldEventDetector<T>, T>
                (v(maxCheck), v(tolerance), 100, new FieldRecordAndContinue<>(events)) {

            @Override
            public T g(FieldSpacecraftState<T> s) {
                if (swap[0]) {
                    return detectorB.g(s);
                } else {
                    return v(-1);
                }
            }

            @Override
            protected FieldEventDetector<T> create(
                    T newMaxCheck,
                    T newThreshold,
                    int newMaxIter,
                    FieldEventHandler<? super FieldEventDetector<T>, T> newHandler) {
                return null;
            }
        };
        FieldPropagator<T> propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);
        propagator.addEventDetector(detectorC);

        // action
        propagator.propagate(epoch.shiftedBy(30));

        // verify
        Assert.assertEquals(2, events.size());
        Assert.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch).getReal(), tolerance);
        Assert.assertEquals(true, events.get(0).isIncreasing());
        Assert.assertSame(detectorA, events.get(0).getDetector());
        Assert.assertEquals(t2, events.get(1).getState().getDate().durationFrom(epoch).getReal(), tolerance);
        Assert.assertEquals(true, events.get(1).isIncreasing());
        Assert.assertSame(detectorC, events.get(1).getDetector());
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
        List<Event<FieldEventDetector<T>, T>> events = new ArrayList<>();
        // mutable boolean
        boolean[] swap = new boolean[1];
        final ContinuousDetector detectorA = new ContinuousDetector(t1)
                .withMaxCheck(v(maxCheck))
                .withThreshold(v(tolerance))
                .withHandler(new FieldRecordAndContinue<FieldEventDetector<T>, T>(events) {
                    @Override
                    public Action eventOccurred(FieldSpacecraftState<T> state,
                                                FieldEventDetector<T> detector,
                                                boolean increasing) {
                        swap[0] = true;
                        super.eventOccurred(state, detector, increasing);
                        return Action.RESET_EVENTS;
                    }
                });
        final ContinuousDetector detectorB = new ContinuousDetector(t2);
        FieldEventDetector<T> detectorC = new FieldAbstractDetector<FieldEventDetector<T>, T>
                (v(maxCheck), v(tolerance), 100, new FieldRecordAndContinue<>(events)) {

            @Override
            public T g(FieldSpacecraftState<T> state) {
                if (!swap[0]) {
                    return detectorB.g(state);
                } else {
                    return v(-1);
                }
            }

            @Override
            protected FieldEventDetector<T> create(
                    T newMaxCheck,
                    T newThreshold,
                    int newMaxIter,
                    FieldEventHandler<? super FieldEventDetector<T>, T> newHandler) {
                return null;
            }

        };
        FieldPropagator<T> propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);
        propagator.addEventDetector(detectorC);

        // action
        propagator.propagate(epoch.shiftedBy(30));

        // verify
        Assert.assertEquals(1, events.size());
        Assert.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch).getReal(), tolerance);
        Assert.assertEquals(true, events.get(0).isIncreasing());
        Assert.assertSame(detectorA, events.get(0).getDetector());
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
        List<Event<FieldEventDetector<T>, T>> events = new ArrayList<>();
        // mutable boolean
        boolean[] swap = new boolean[1];
        final ContinuousDetector detectorA = new ContinuousDetector(t1)
                .withMaxCheck(v(maxCheck))
                .withThreshold(v(tolerance))
                .withHandler(new FieldRecordAndContinue<FieldEventDetector<T>, T>(events) {
                    @Override
                    public Action eventOccurred(FieldSpacecraftState<T> state,
                                                FieldEventDetector<T> detector,
                                                boolean increasing) {
                        swap[0] = true;
                        super.eventOccurred(state, detector, increasing);
                        return Action.RESET_EVENTS;
                    }
                });
        final ContinuousDetector detectorB = new ContinuousDetector(t2);
        final ContinuousDetector detectorD = new ContinuousDetector(t3);
        FieldEventDetector<T> detectorC = new FieldAbstractDetector<FieldEventDetector<T>, T>
                (v(maxCheck), v(tolerance), 100, new FieldRecordAndContinue<>(events)) {

            @Override
            public T g(FieldSpacecraftState<T> state) {
                if (!swap[0]) {
                    return detectorB.g(state);
                } else {
                    return detectorD.g(state);
                }
            }

            @Override
            protected FieldEventDetector<T> create(
                    T newMaxCheck,
                    T newThreshold,
                    int newMaxIter,
                    FieldEventHandler<? super FieldEventDetector<T>, T> newHandler) {
                return null;
            }

        };
        FieldPropagator<T> propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);
        propagator.addEventDetector(detectorC);

        // action
        propagator.propagate(epoch.shiftedBy(30));

        // verify
        Assert.assertEquals(2, events.size());
        Assert.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch).getReal(), tolerance);
        Assert.assertEquals(true, events.get(0).isIncreasing());
        Assert.assertSame(detectorA, events.get(0).getDetector());
        Assert.assertEquals(t3, events.get(1).getState().getDate().durationFrom(epoch).getReal(), tolerance);
        Assert.assertEquals(true, events.get(1).isIncreasing());
        Assert.assertSame(detectorC, events.get(1).getDetector());
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
        List<Event<FieldEventDetector<T>, T>> events = new ArrayList<>();
        // mutable boolean
        boolean[] swap = new boolean[1];
        final ContinuousDetector detectorA = new ContinuousDetector(t1)
                .withMaxCheck(v(maxCheck))
                .withThreshold(v(tolerance))
                .withHandler(new FieldRecordAndContinue<FieldEventDetector<T>, T>(events) {
                    @Override
                    public Action eventOccurred(FieldSpacecraftState<T> state,
                                                FieldEventDetector<T> detector,
                                                boolean increasing) {
                        swap[0] = true;
                        super.eventOccurred(state, detector, increasing);
                        return Action.RESET_EVENTS;
                    }
                });
        final ContinuousDetector detectorB = new ContinuousDetector(t2);
        final ContinuousDetector detectorD = new ContinuousDetector(t3);
        FieldEventDetector<T> detectorC = new FieldAbstractDetector<FieldEventDetector<T>, T>
                (v(maxCheck), v(tolerance), 100, new FieldRecordAndContinue<>(events)) {

            @Override
            public T g(FieldSpacecraftState<T> state) {
                if (swap[0]) {
                    return detectorB.g(state);
                } else {
                    return detectorD.g(state);
                }
            }

            @Override
            protected FieldEventDetector<T> create(
                    T newMaxCheck,
                    T newThreshold,
                    int newMaxIter,
                    FieldEventHandler<? super FieldEventDetector<T>, T> newHandler) {
                return null;
            }

        };
        FieldPropagator<T> propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);
        propagator.addEventDetector(detectorC);

        // action
        propagator.propagate(epoch.shiftedBy(30));

        // verify
        Assert.assertEquals(2, events.size());
        Assert.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch).getReal(), tolerance);
        Assert.assertEquals(true, events.get(0).isIncreasing());
        Assert.assertSame(detectorA, events.get(0).getDetector());
        Assert.assertEquals(t2, events.get(1).getState().getDate().durationFrom(epoch).getReal(), tolerance);
        Assert.assertEquals(true, events.get(1).isIncreasing());
        Assert.assertSame(detectorC, events.get(1).getDetector());
    }

    /** check when root finding tolerance > event finding tolerance. */
    @Test
    public void testToleranceStop() {
        // setup
        double maxCheck = 10;
        double tolerance = 1e-18; // less than 1 ulp
        double t1 = 15.1;
        // shared event list so we know the order in which they occurred
        List<Event<FieldEventDetector<T>, T>> events = new ArrayList<>();
        FlatDetector detectorA = new FlatDetector(t1)
                .withHandler(new Handler<>(events, Action.STOP))
                .withMaxCheck(v(maxCheck))
                .withThreshold(v(tolerance));
        FieldPropagator<T> propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);

        // action
        FieldSpacecraftState<T> finalState = propagator.propagate(epoch.shiftedBy(30));

        // verify
        Assert.assertEquals(1, events.size());
        // use root finder tolerance instead of event finder tolerance.
        Assert.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch).getReal(), tolerance);
        Assert.assertEquals(true, events.get(0).isIncreasing());
        Assert.assertSame(detectorA, events.get(0).getDetector());
        Assert.assertEquals(t1, finalState.getDate().durationFrom(epoch).getReal(), tolerance);

        // try to resume propagation
        finalState = propagator.propagate(epoch.shiftedBy(30));

        // verify it got to the end
        Assert.assertEquals(30.0, finalState.getDate().durationFrom(epoch).getReal(), 0.0);
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
        List<Event<FieldEventDetector<T>, T>> events = new ArrayList<>();
        // never zero so there is no easy way out
        FieldEventDetector<T> detectorA = new FieldAbstractDetector<FieldEventDetector<T>, T>
                (v(maxCheck), v(tolerance), 100, new FieldRecordAndContinue<>(events)) {

            @Override
            public T g(FieldSpacecraftState<T> state) {
                final FieldAbsoluteDate<T> t = state.getDate();
                if (t.compareTo(epoch.shiftedBy(t1)) < 0) {
                    return v(-1);
                } else if (t.compareTo(epoch.shiftedBy(t2)) < 0) {
                    return v(1);
                } else {
                    return v(-1);
                }
            }

            @Override
            protected FieldEventDetector<T> create(
                    T newMaxCheck,
                    T newThreshold,
                    int newMaxIter,
                    FieldEventHandler<? super FieldEventDetector<T>, T> newHandler) {
                return null;
            }
        };
        TimeDetector detectorB = new TimeDetector(t1)
                .withHandler(new FieldRecordAndContinue<>(events))
                .withMaxCheck(v(maxCheck))
                .withThreshold(v(tolerance));
        FieldPropagator<T> propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);
        propagator.addEventDetector(detectorB);

        // action
        propagator.propagate(epoch.shiftedBy(30.0));

        // verify
        Assert.assertEquals(3, events.size());
        Assert.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch).getReal(), tolerance);
        Assert.assertEquals(true, events.get(0).isIncreasing());
        Assert.assertSame(detectorA, events.get(0).getDetector());
        Assert.assertEquals(t1, events.get(1).getState().getDate().durationFrom(epoch).getReal(), tolerance);
        Assert.assertEquals(true, events.get(1).isIncreasing());
        Assert.assertSame(detectorB, events.get(1).getDetector());
        Assert.assertEquals(t2, events.get(2).getState().getDate().durationFrom(epoch).getReal(), tolerance);
        Assert.assertEquals(false, events.get(2).isIncreasing());
        Assert.assertSame(detectorA, events.get(2).getDetector());
    }

    /** check when root finding tolerance > event finding tolerance. */
    @Test
    public void testToleranceMaxIterations() {
        // setup
        double maxCheck = 10;
        double tolerance = 1e-18; // less than 1 ulp
        FieldAbsoluteDate<T> t1 = epoch.shiftedBy(15).shiftedBy(FastMath.ulp(15.0) / 8);
        // shared event list so we know the order in which they occurred
        List<Event<FieldEventDetector<T>, T>> events = new ArrayList<>();
        FlatDetector detectorA = new FlatDetector(t1)
                .withHandler(new FieldRecordAndContinue<>(events))
                .withMaxCheck(v(maxCheck))
                .withThreshold(v(tolerance));
        FieldPropagator<T> propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);

        // action
        propagator.propagate(epoch.shiftedBy(30));

        // verify
        Assert.assertEquals(1, events.size());
        // use root finder tolerance instead of event finder tolerance.
        Assert.assertEquals(t1.durationFrom(epoch).getReal(),
                events.get(0).getState().getDate().durationFrom(epoch).getReal(), tolerance);
        Assert.assertEquals(true, events.get(0).isIncreasing());
        Assert.assertSame(detectorA, events.get(0).getDetector());
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
        FieldPropagator<T> propagator = getPropagator(10.0);

        // switched for 9 to 1 to be close to the start of the step
        double t1 = -1;
        Handler<FieldEventDetector<T>> handler1 = new Handler<>(Action.RESET_DERIVATIVES);
        TimeDetector detector1 = new TimeDetector(t1)
                .withHandler(handler1)
                .withMaxCheck(v(10))
                .withThreshold(v(1e-9));
        propagator.addEventDetector(detector1);
        FieldRecordAndContinue<FieldEventDetector<T>, T> handler2 = new FieldRecordAndContinue<>();
        TimeDetector detector2 = new TimeDetector(t1 - 1e-15, t1 - 4.9)
                .withHandler(handler2)
                .withMaxCheck(v(11))
                .withThreshold(v(1e-9));
        propagator.addEventDetector(detector2);

        // action
        propagator.propagate(epoch.shiftedBy(-20));

        // verify
        List<Event<FieldEventDetector<T>, T>> events1 = handler1.getEvents();
        Assert.assertEquals(1, events1.size());
        Assert.assertEquals(t1, events1.get(0).getState().getDate().durationFrom(epoch).getReal(), 0.0);
        List<Event<FieldEventDetector<T>, T>> events2 = handler2.getEvents();
        Assert.assertEquals(0, events2.size());
    }

    @Test
    public void testCloseEventsReverse() {
        // setup
        double tolerance = 1;
        FieldPropagator<T> propagator = getPropagator(10);

        FieldRecordAndContinue<FieldEventDetector<T>, T> handler1 = new FieldRecordAndContinue<>();
        TimeDetector detector1 = new TimeDetector(-5)
                .withHandler(handler1)
                .withMaxCheck(v(10))
                .withThreshold(v(tolerance));
        propagator.addEventDetector(detector1);
        FieldRecordAndContinue<FieldEventDetector<T>, T> handler2 = new FieldRecordAndContinue<>();
        TimeDetector detector2 = new TimeDetector(-5.5)
                .withHandler(handler2)
                .withMaxCheck(v(10))
                .withThreshold(v(tolerance));
        propagator.addEventDetector(detector2);

        // action
        propagator.propagate(epoch.shiftedBy(-20));

        // verify
        List<Event<FieldEventDetector<T>, T>> events1 = handler1.getEvents();
        Assert.assertEquals(1, events1.size());
        Assert.assertEquals(-5, events1.get(0).getState().getDate().durationFrom(epoch).getReal(), tolerance);
        List<Event<FieldEventDetector<T>, T>> events2 = handler2.getEvents();
        Assert.assertEquals(1, events2.size());
        Assert.assertEquals(-5.5, events2.get(0).getState().getDate().durationFrom(epoch).getReal(), tolerance);
    }

    @Test
    public void testSimultaneousEventsReverse() {
        // setup
        FieldPropagator<T> propagator = getPropagator(10);

        FieldRecordAndContinue<FieldEventDetector<T>, T> handler1 = new FieldRecordAndContinue<>();
        TimeDetector detector1 = new TimeDetector(-5)
                .withHandler(handler1)
                .withMaxCheck(v(10))
                .withThreshold(v(1));
        propagator.addEventDetector(detector1);
        FieldRecordAndContinue<FieldEventDetector<T>, T> handler2 = new FieldRecordAndContinue<>();
        TimeDetector detector2 = new TimeDetector(-5)
                .withHandler(handler2)
                .withMaxCheck(v(10))
                .withThreshold(v(1));
        propagator.addEventDetector(detector2);

        // action
        propagator.propagate(epoch.shiftedBy(-20));

        // verify
        List<Event<FieldEventDetector<T>, T>> events1 = handler1.getEvents();
        Assert.assertEquals(1, events1.size());
        Assert.assertEquals(-5, events1.get(0).getState().getDate().durationFrom(epoch).getReal(), 0.0);
        List<Event<FieldEventDetector<T>, T>> events2 = handler2.getEvents();
        Assert.assertEquals(1, events2.size());
        Assert.assertEquals(-5, events2.get(0).getState().getDate().durationFrom(epoch).getReal(), 0.0);
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
        FieldPropagator<T> propagator = getPropagator(10);

        List<Event<FieldEventDetector<T>, T>> events = new ArrayList<>();
        TimeDetector detector1 = new TimeDetector(-9.9, -10.1, -12)
                .withHandler(new FieldRecordAndContinue<>(events))
                .withMaxCheck(v(10))
                .withThreshold(v(0.2));
        propagator.addEventDetector(detector1);

        // action
        propagator.propagate(epoch.shiftedBy(-20));

        //verify
        // finds one or three events. Not 2.
        Assert.assertEquals(1, events.size());
        Assert.assertEquals(-9.9, events.get(0).getState().getDate().durationFrom(epoch).getReal(), 0.2);
        Assert.assertEquals(true, events.get(0).isIncreasing());
    }

    /** "A Tricky Problem" from bug #239. */
    @Test
    public void testTrickyCaseLowerReverse() {
        // setup
        double maxCheck = 10;
        double tolerance = 1e-6;
        double t1 = -1.0, t2 = -15, t3 = -16, t4 = -17, t5 = -18;
        // shared event list so we know the order in which they occurred
        List<Event<FieldEventDetector<T>, T>> events = new ArrayList<>();
        TimeDetector detectorA = new TimeDetector(t3)
                .withHandler(new FieldRecordAndContinue<>(events))
                .withMaxCheck(v(maxCheck))
                .withThreshold(v(tolerance));
        TimeDetector detectorB = new TimeDetector(-50, t1, t2, t5)
                .withHandler(new FieldRecordAndContinue<>(events))
                .withMaxCheck(v(maxCheck))
                .withThreshold(v(tolerance));
        TimeDetector detectorC = new TimeDetector(t4)
                .withHandler(new Handler<>(events, Action.RESET_DERIVATIVES))
                .withMaxCheck(v(maxCheck))
                .withThreshold(v(tolerance));

        FieldPropagator<T> propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);
        propagator.addEventDetector(detectorB);
        propagator.addEventDetector(detectorC);

        // action
        propagator.propagate(epoch.shiftedBy(-30));

        //verify
        // really we only care that the Rules of Event Handling are not violated,
        // but I only know one way to do that in this case.
        Assert.assertEquals(5, events.size());
        Assert.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch).getReal(), tolerance);
        Assert.assertEquals(false, events.get(0).isIncreasing());
        Assert.assertEquals(t2, events.get(1).getState().getDate().durationFrom(epoch).getReal(), tolerance);
        Assert.assertEquals(true, events.get(1).isIncreasing());
        Assert.assertEquals(t3, events.get(2).getState().getDate().durationFrom(epoch).getReal(), tolerance);
        Assert.assertEquals(true, events.get(2).isIncreasing());
        Assert.assertEquals(t4, events.get(3).getState().getDate().durationFrom(epoch).getReal(), tolerance);
        Assert.assertEquals(true, events.get(3).isIncreasing());
        Assert.assertEquals(t5, events.get(4).getState().getDate().durationFrom(epoch).getReal(), tolerance);
        Assert.assertEquals(false, events.get(4).isIncreasing());
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
        List<Event<FieldEventDetector<T>, T>> events = new ArrayList<>();
        TimeDetector detectorA = new TimeDetector(t2)
                .withHandler(new FieldRecordAndContinue<>(events))
                .withMaxCheck(v(maxCheck))
                .withThreshold(v(1e-6));
        FlatDetector detectorB = new FlatDetector(t3)
                .withHandler(new FieldRecordAndContinue<>(events))
                .withMaxCheck(v(maxCheck))
                .withThreshold(v(0.5));
        FieldPropagator<T> propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);
        propagator.addEventDetector(detectorB);

        // action
        propagator.propagate(epoch.shiftedBy(-30.0));

        // verify
        // if these fail the event finding did its job,
        // but this test isn't testing what it is supposed to be
        Assert.assertSame(detectorB, events.get(0).getDetector());
        Assert.assertSame(detectorA, events.get(1).getDetector());
        Assert.assertTrue(events.get(0).getState().getDate().compareTo(
                events.get(1).getState().getDate()) > 0);

        // check event detection worked
        Assert.assertEquals(2, events.size());
        Assert.assertEquals(t3, events.get(0).getState().getDate().durationFrom(epoch).getReal(), 0.5);
        Assert.assertEquals(true, events.get(0).isIncreasing());
        Assert.assertEquals(t2, events.get(1).getState().getDate().durationFrom(epoch).getReal(), 1e-6);
        Assert.assertEquals(true, events.get(1).isIncreasing());
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
        List<Event<FieldEventDetector<T>, T>> events = new ArrayList<>();
        TimeDetector detectorA = new TimeDetector(t2)
                .withHandler(new FieldRecordAndContinue<>(events))
                .withMaxCheck(v(maxCheck))
                .withThreshold(v(tolerance));
        TimeDetector detectorB = new TimeDetector(-50, t1, t3)
                .withHandler(new FieldRecordAndContinue<>(events))
                .withMaxCheck(v(maxCheck))
                .withThreshold(v(toleranceB));
        FieldPropagator<T> propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);
        propagator.addEventDetector(detectorB);

        // action
        propagator.propagate(epoch.shiftedBy(-30.0));

        // verify
        // we only care that the rules are satisfied. There are multiple solutions.
        Assert.assertEquals(3, events.size());
        Assert.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch).getReal(), toleranceB);
        Assert.assertEquals(true, events.get(0).isIncreasing());
        Assert.assertSame(detectorB, events.get(0).getDetector());
        Assert.assertEquals(t3, events.get(1).getState().getDate().durationFrom(epoch).getReal(), toleranceB);
        Assert.assertEquals(false, events.get(1).isIncreasing());
        Assert.assertSame(detectorB, events.get(1).getDetector());
        Assert.assertEquals(t2, events.get(2).getState().getDate().durationFrom(epoch).getReal(), tolerance);
        Assert.assertEquals(true, events.get(2).isIncreasing());
        Assert.assertSame(detectorA, events.get(2).getDetector());
        // ascending order
        Assert.assertTrue(events.get(0).getState().getDate().compareTo(
                events.get(1).getState().getDate()) >= 0);
        Assert.assertTrue(events.get(1).getState().getDate().compareTo(
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
        List<Event<FieldEventDetector<T>, T>> events = new ArrayList<>();
        FlatDetector detectorB = new FlatDetector(t1, t2, t3)
                .withHandler(new FieldRecordAndContinue<>(events))
                .withMaxCheck(v(maxCheck))
                .withThreshold(v(tolerance));
        FieldPropagator<T> propagator = getPropagator(10);
        propagator.addEventDetector(detectorB);

        // action
        propagator.propagate(epoch.shiftedBy(-30.0));

        // verify
        // allowed to report t1 or t3.
        Assert.assertEquals(1, events.size());
        Assert.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch).getReal(), tolerance);
        Assert.assertEquals(true, events.get(0).isIncreasing());
        Assert.assertSame(detectorB, events.get(0).getDetector());
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
        List<Event<FieldEventDetector<T>, T>> events = new ArrayList<>();
        TimeDetector detectorA = new TimeDetector(t1)
                .withHandler(new FieldRecordAndContinue<>(events))
                .withMaxCheck(v(maxCheck))
                .withThreshold(v(tolerance));
        TimeDetector detectorB = new TimeDetector(t1, t1)
                .withHandler(new FieldRecordAndContinue<>(events))
                .withMaxCheck(v(maxCheck))
                .withThreshold(v(tolerance));
        FieldPropagator<T> propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);
        propagator.addEventDetector(detectorB);

        // action
        propagator.propagate(epoch.shiftedBy(-30.0));

        // verify
        Assert.assertEquals(1, events.size());
        Assert.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch).getReal(), 0.0);
        Assert.assertEquals(true, events.get(0).isIncreasing());
        Assert.assertSame(detectorA, events.get(0).getDetector());
        // detector worked correctly
        Assert.assertTrue(detectorB.g(state(t1)).getReal() == 0.0);
        Assert.assertTrue(detectorB.g(state(t1 + 1e-6)).getReal() < 0);
        Assert.assertTrue(detectorB.g(state(t1 - 1e-6)).getReal() < 0);
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
        List<Event<FieldEventDetector<T>, T>> events = new ArrayList<>();
        TimeDetector detectorA = new TimeDetector(t1)
                .withHandler(new FieldRecordAndContinue<>(events))
                .withMaxCheck(v(maxCheck))
                .withThreshold(v(tolerance));
        ContinuousDetector detectorB = new ContinuousDetector(-50, t1, t1)
                .withHandler(new FieldRecordAndContinue<>(events))
                .withMaxCheck(v(maxCheck))
                .withThreshold(v(tolerance));
        detectorB.g(state(t1));
        FieldPropagator<T> propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);
        propagator.addEventDetector(detectorB);

        // action
        propagator.propagate(epoch.shiftedBy(-30.0));

        // verify
        Assert.assertEquals(1, events.size());
        Assert.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch).getReal(), 0.0);
        Assert.assertEquals(true, events.get(0).isIncreasing());
        Assert.assertSame(detectorA, events.get(0).getDetector());
        // detector worked correctly
        Assert.assertEquals(0.0, detectorB.g(state(t1)).getReal(), 0.0);
        Assert.assertTrue(detectorB.g(state(t1 + 1e-6)).getReal() > 0);
        Assert.assertTrue(detectorB.g(state(t1 - 1e-6)).getReal() > 0);
    }

    /** check root finding when zero at both ends. */
    @Test
    public void testZeroAtBeginningAndEndOfIntervalReverse() {
        // setup
        double maxCheck = 10;
        double tolerance = 1e-6;
        double t1 = -10, t2 = -20;
        // shared event list so we know the order in which they occurred
        List<Event<FieldEventDetector<T>, T>> events = new ArrayList<>();
        ContinuousDetector detectorA = new ContinuousDetector(-50, t1, t2)
                .withHandler(new FieldRecordAndContinue<>(events))
                .withMaxCheck(v(maxCheck))
                .withThreshold(v(tolerance));
        FieldPropagator<T> propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);

        // action
        propagator.propagate(epoch.shiftedBy(-30.0));

        // verify
        Assert.assertEquals(2, events.size());
        Assert.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch).getReal(), tolerance);
        Assert.assertEquals(true, events.get(0).isIncreasing());
        Assert.assertSame(detectorA, events.get(0).getDetector());
        Assert.assertEquals(t2, events.get(1).getState().getDate().durationFrom(epoch).getReal(), tolerance);
        Assert.assertEquals(false, events.get(1).isIncreasing());
        Assert.assertSame(detectorA, events.get(1).getDetector());
    }

    /** check root finding when zero at both ends. */
    @Test
    public void testZeroAtBeginningAndEndOfIntervalOppositeSignReverse() {
        // setup
        double maxCheck = 10;
        double tolerance = 1e-6;
        double t1 = -10, t2 = -20;
        // shared event list so we know the order in which they occurred
        List<Event<FieldEventDetector<T>, T>> events = new ArrayList<>();
        ContinuousDetector detectorA = new ContinuousDetector(t1, t2)
                .withHandler(new FieldRecordAndContinue<>(events))
                .withMaxCheck(v(maxCheck))
                .withThreshold(v(tolerance));
        FieldPropagator<T> propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);

        // action
        propagator.propagate(epoch.shiftedBy(-30));

        // verify
        Assert.assertEquals(2, events.size());
        Assert.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch).getReal(), 0.0);
        Assert.assertEquals(false, events.get(0).isIncreasing());
        Assert.assertSame(detectorA, events.get(0).getDetector());
        Assert.assertEquals(t2, events.get(1).getState().getDate().durationFrom(epoch).getReal(), 0.0);
        Assert.assertEquals(true, events.get(1).isIncreasing());
        Assert.assertSame(detectorA, events.get(1).getDetector());
    }

    /** Test where an event detector has to back up multiple times. */
    @Test
    public void testMultipleBackupsReverse() {
        // setup
        double maxCheck = 10;
        double tolerance = 1e-6;
        double t1 = -1.0, t2 = -2, t3 = -3, t4 = -4, t5 = -5, t6 = -6.5, t7 = -7;
        // shared event list so we know the order in which they occurred
        List<Event<FieldEventDetector<T>, T>> events = new ArrayList<>();
        ContinuousDetector detectorA = new ContinuousDetector(t6)
                .withHandler(new FieldRecordAndContinue<>(events))
                .withMaxCheck(v(maxCheck))
                .withThreshold(v(tolerance));
        ContinuousDetector detectorB = new ContinuousDetector(-50, t1, t3, t4, t7)
                .withHandler(new FieldRecordAndContinue<>(events))
                .withMaxCheck(v(maxCheck))
                .withThreshold(v(tolerance));
        ContinuousDetector detectorC = new ContinuousDetector(-50, t2, t5)
                .withHandler(new FieldRecordAndContinue<>(events))
                .withMaxCheck(v(maxCheck))
                .withThreshold(v(tolerance));

        FieldPropagator<T> propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);
        propagator.addEventDetector(detectorB);
        propagator.addEventDetector(detectorC);

        // action
        propagator.propagate(epoch.shiftedBy(-30.0));

        //verify
        // really we only care that the Rules of Event Handling are not violated,
        Assert.assertEquals(5, events.size());
        Assert.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch).getReal(), tolerance);
        Assert.assertEquals(true, events.get(0).isIncreasing());
        Assert.assertEquals(detectorB, events.get(0).getDetector());
        Assert.assertEquals(t2, events.get(1).getState().getDate().durationFrom(epoch).getReal(), tolerance);
        Assert.assertEquals(true, events.get(1).isIncreasing());
        Assert.assertEquals(detectorC, events.get(1).getDetector());
        // reporting t3 and t4 is optional, seeing them is not.
        // we know a root was found at t3 because events are reported at t2 and t5.
        /*
        Assert.assertEquals(t3, events.get(2).getT(), tolerance);
        Assert.assertEquals(false, events.get(2).isIncreasing());
        Assert.assertEquals(detectorB, events.get(2).getHandler());
        Assert.assertEquals(t4, events.get(3).getT(), tolerance);
        Assert.assertEquals(true, events.get(3).isIncreasing());
        Assert.assertEquals(detectorB, events.get(3).getHandler());
        */
        Assert.assertEquals(t5, events.get(2).getState().getDate().durationFrom(epoch).getReal(), tolerance);
        Assert.assertEquals(false, events.get(2).isIncreasing());
        Assert.assertEquals(detectorC, events.get(2).getDetector());
        Assert.assertEquals(t6, events.get(3).getState().getDate().durationFrom(epoch).getReal(), tolerance);
        Assert.assertEquals(true, events.get(3).isIncreasing());
        Assert.assertEquals(detectorA, events.get(3).getDetector());
        Assert.assertEquals(t7, events.get(4).getState().getDate().durationFrom(epoch).getReal(), tolerance);
        Assert.assertEquals(false, events.get(4).isIncreasing());
        Assert.assertEquals(detectorB, events.get(4).getDetector());
    }

    /** Test a reset event triggering another event at the same time. */
    @Test
    public void testEventCausedByStateResetReverse() {
        // setup
        double maxCheck = 10;
        double tolerance = 1e-6;
        double t1 = -15.0;
        FieldSpacecraftState<T> newState = new FieldSpacecraftState<T>(new FieldKeplerianOrbit<T>(
                v(6378137 + 500e3), v(0), v(FastMath.PI / 2), v(0), v(0),
                v(FastMath.PI / 2), PositionAngle.TRUE, eci, epoch.shiftedBy(t1), v(mu)));
        // shared event list so we know the order in which they occurred
        List<Event<FieldEventDetector<T>, T>> events = new ArrayList<>();
        TimeDetector detectorA = new TimeDetector(t1)
                .withHandler(new Handler<FieldEventDetector<T>>(events, Action.RESET_STATE) {
                    @Override
                    public FieldSpacecraftState<T> resetState(FieldEventDetector<T> detector,
                                                      FieldSpacecraftState<T> oldState) {
                        return newState;
                    }
                })
                .withMaxCheck(v(maxCheck))
                .withThreshold(v(tolerance));
        FieldLatitudeCrossingDetector<T> detectorB =
                new FieldLatitudeCrossingDetector<T>(field, earth, FastMath.toRadians(80))
                        .withHandler(new FieldRecordAndContinue<>(events))
                        .withMaxCheck(v(maxCheck))
                        .withThreshold(v(tolerance));

        FieldPropagator<T> propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);
        propagator.addEventDetector(detectorB);

        // action
        propagator.propagate(epoch.shiftedBy(-40.0));

        //verify
        // really we only care that the Rules of Event Handling are not violated,
        Assert.assertEquals(2, events.size());
        Assert.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch).getReal(), tolerance);
        Assert.assertEquals(true, events.get(0).isIncreasing());
        Assert.assertEquals(detectorA, events.get(0).getDetector());
        Assert.assertEquals(t1, events.get(1).getState().getDate().durationFrom(epoch).getReal(), tolerance);
        Assert.assertEquals(false, events.get(1).isIncreasing());
        Assert.assertEquals(detectorB, events.get(1).getDetector());
    }

    /** check when t + tolerance == t. */
    @Test
    public void testConvergenceTooTightReverse() {
        // setup
        double maxCheck = 10;
        double tolerance = 1e-18;
        double t1 = -15;
        // shared event list so we know the order in which they occurred
        List<Event<FieldEventDetector<T>, T>> events = new ArrayList<>();
        ContinuousDetector detectorA = new ContinuousDetector(t1)
                .withHandler(new FieldRecordAndContinue<>(events))
                .withMaxCheck(v(maxCheck))
                .withThreshold(v(tolerance));
        FieldPropagator<T> propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);

        // action
        propagator.propagate(epoch.shiftedBy(-30.0));

        // verify
        Assert.assertEquals(1, events.size());
        Assert.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch).getReal(), 0.0);
        Assert.assertEquals(true, events.get(0).isIncreasing());
        Assert.assertSame(detectorA, events.get(0).getDetector());
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
        List<Event<FieldEventDetector<T>, T>> events = new ArrayList<>();
        // mutable boolean
        boolean[] swap = new boolean[1];
        ContinuousDetector detectorA = new ContinuousDetector(t1)
                .withMaxCheck(v(maxCheck))
                .withThreshold(v(tolerance))
                .withHandler(new FieldRecordAndContinue<FieldEventDetector<T>, T>(events) {
                    @Override
                    public Action eventOccurred(FieldSpacecraftState<T> s,
                                                FieldEventDetector<T> detector,
                                                boolean increasing) {
                        swap[0] = true;
                        return super.eventOccurred(s, detector, increasing);
                    }
                });
        ContinuousDetector detectorB = new ContinuousDetector(t2);
        FieldEventDetector<T> detectorC = new FieldAbstractDetector<FieldEventDetector<T>, T>
                (v(maxCheck), v(tolerance), 100, new FieldRecordAndContinue<>(events)) {

            @Override
            public T g(FieldSpacecraftState<T> state) {
                if (swap[0]) {
                    return detectorB.g(state);
                } else {
                    return v(1);
                }
            }

            @Override
            protected FieldEventDetector<T> create(
                    T newMaxCheck,
                    T newThreshold,
                    int newMaxIter,
                    FieldEventHandler<? super FieldEventDetector<T>, T> newHandler) {
                return null;
            }

        };
        FieldPropagator<T> propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);
        propagator.addEventDetector(detectorC);

        // action
        propagator.propagate(epoch.shiftedBy(-30.0));

        // verify
        Assert.assertEquals(2, events.size());
        Assert.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch).getReal(), tolerance);
        Assert.assertEquals(true, events.get(0).isIncreasing());
        Assert.assertSame(detectorA, events.get(0).getDetector());
        Assert.assertEquals(t2, events.get(1).getState().getDate().durationFrom(epoch).getReal(), tolerance);
        Assert.assertEquals(true, events.get(1).isIncreasing());
        Assert.assertSame(detectorC, events.get(1).getDetector());
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
        List<Event<FieldEventDetector<T>, T>> events = new ArrayList<>();
        // mutable boolean
        boolean[] swap = new boolean[1];
        final ContinuousDetector detectorA = new ContinuousDetector(t1)
                .withMaxCheck(v(maxCheck))
                .withThreshold(v(tolerance))
                .withHandler(new FieldRecordAndContinue<FieldEventDetector<T>, T>(events) {
                    @Override
                    public Action eventOccurred(FieldSpacecraftState<T> state,
                                                FieldEventDetector<T> detector,
                                                boolean increasing) {
                        swap[0] = true;
                        super.eventOccurred(state, detector, increasing);
                        return Action.RESET_EVENTS;
                    }
                });
        final ContinuousDetector detectorB = new ContinuousDetector(t2);
        FieldEventDetector<T> detectorC = new FieldAbstractDetector<FieldEventDetector<T>, T>
                (v(maxCheck), v(tolerance), 100, new FieldRecordAndContinue<>(events)) {

            @Override
            public T g(FieldSpacecraftState<T> state) {
                if (!swap[0]) {
                    return detectorB.g(state);
                } else {
                    return v(1);
                }
            }

            @Override
            protected FieldEventDetector<T> create(
                    T newMaxCheck,
                    T newThreshold,
                    int newMaxIter,
                    FieldEventHandler<? super FieldEventDetector<T>, T> newHandler) {
                return null;
            }

        };
        FieldPropagator<T> propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);
        propagator.addEventDetector(detectorC);

        // action
        propagator.propagate(epoch.shiftedBy(-30));

        // verify
        Assert.assertEquals(1, events.size());
        Assert.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch).getReal(), tolerance);
        Assert.assertEquals(true, events.get(0).isIncreasing());
        Assert.assertSame(detectorA, events.get(0).getDetector());
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
        List<Event<FieldEventDetector<T>, T>> events = new ArrayList<>();
        // mutable boolean
        boolean[] swap = new boolean[1];
        final ContinuousDetector detectorA = new ContinuousDetector(t1)
                .withMaxCheck(v(maxCheck))
                .withThreshold(v(tolerance))
                .withHandler(new FieldRecordAndContinue<FieldEventDetector<T>, T>(events) {
                    @Override
                    public Action eventOccurred(FieldSpacecraftState<T> state,
                                                FieldEventDetector<T> detector,
                                                boolean increasing) {
                        swap[0] = true;
                        super.eventOccurred(state, detector, increasing);
                        return Action.RESET_EVENTS;
                    }
                });
        final ContinuousDetector detectorB = new ContinuousDetector(t2);
        final ContinuousDetector detectorD = new ContinuousDetector(t3);
        FieldEventDetector<T> detectorC = new FieldAbstractDetector<FieldEventDetector<T>, T>
                (v(maxCheck), v(tolerance), 100, new FieldRecordAndContinue<>(events)) {

            @Override
            public T g(FieldSpacecraftState<T> state) {
                if (!swap[0]) {
                    return detectorB.g(state);
                } else {
                    return detectorD.g(state);
                }
            }

            @Override
            protected FieldEventDetector<T> create(
                    T newMaxCheck,
                    T newThreshold,
                    int newMaxIter,
                    FieldEventHandler<? super FieldEventDetector<T>, T> newHandler) {
                return null;
            }

        };
        FieldPropagator<T> propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);
        propagator.addEventDetector(detectorC);

        // action
        propagator.propagate(epoch.shiftedBy(-30));

        // verify
        Assert.assertEquals(2, events.size());
        Assert.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch).getReal(), tolerance);
        Assert.assertEquals(true, events.get(0).isIncreasing());
        Assert.assertSame(detectorA, events.get(0).getDetector());
        Assert.assertEquals(t3, events.get(1).getState().getDate().durationFrom(epoch).getReal(), tolerance);
        Assert.assertEquals(true, events.get(1).isIncreasing());
        Assert.assertSame(detectorC, events.get(1).getDetector());
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
        List<Event<FieldEventDetector<T>, T>> events = new ArrayList<>();
        // mutable boolean
        boolean[] swap = new boolean[1];
        final ContinuousDetector detectorA = new ContinuousDetector(t1)
                .withMaxCheck(v(maxCheck))
                .withThreshold(v(tolerance))
                .withHandler(new FieldRecordAndContinue<FieldEventDetector<T>, T>(events) {
                    @Override
                    public Action eventOccurred(FieldSpacecraftState<T> state,
                                                FieldEventDetector<T> detector,
                                                boolean increasing) {
                        swap[0] = true;
                        super.eventOccurred(state, detector, increasing);
                        return Action.RESET_EVENTS;
                    }
                });
        final ContinuousDetector detectorB = new ContinuousDetector(t2);
        final ContinuousDetector detectorD = new ContinuousDetector(t3);
        FieldEventDetector<T> detectorC = new FieldAbstractDetector<FieldEventDetector<T>, T>
                (v(maxCheck), v(tolerance), 100, new FieldRecordAndContinue<>(events)) {

            @Override
            public T g(FieldSpacecraftState<T> state) {
                if (swap[0]) {
                    return detectorB.g(state);
                } else {
                    return detectorD.g(state);
                }
            }

            @Override
            protected FieldEventDetector<T> create(
                    T newMaxCheck,
                    T newThreshold,
                    int newMaxIter,
                    FieldEventHandler<? super FieldEventDetector<T>, T> newHandler) {
                return null;
            }

        };
        FieldPropagator<T> propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);
        propagator.addEventDetector(detectorC);

        // action
        propagator.propagate(epoch.shiftedBy(-30));

        // verify
        Assert.assertEquals(2, events.size());
        Assert.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch).getReal(), tolerance);
        Assert.assertEquals(true, events.get(0).isIncreasing());
        Assert.assertSame(detectorA, events.get(0).getDetector());
        Assert.assertEquals(t2, events.get(1).getState().getDate().durationFrom(epoch).getReal(), tolerance);
        Assert.assertEquals(true, events.get(1).isIncreasing());
        Assert.assertSame(detectorC, events.get(1).getDetector());
    }

    /** check when root finding tolerance > event finding tolerance. */
    @Test
    public void testToleranceStopReverse() {
        // setup
        double maxCheck = 10;
        double tolerance = 1e-18; // less than 1 ulp
        double t1 = -15.1;
        // shared event list so we know the order in which they occurred
        List<Event<FieldEventDetector<T>, T>> events = new ArrayList<>();
        FlatDetector detectorA = new FlatDetector(t1)
                .withHandler(new Handler<>(events, Action.STOP))
                .withMaxCheck(v(maxCheck))
                .withThreshold(v(tolerance));
        FieldPropagator<T> propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);

        // action
        FieldSpacecraftState<T> finalState = propagator.propagate(epoch.shiftedBy(-30.0));

        // verify
        Assert.assertEquals(1, events.size());
        // use root finder tolerance instead of event finder tolerance.
        Assert.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch).getReal(), tolerance);
        Assert.assertEquals(true, events.get(0).isIncreasing());
        Assert.assertSame(detectorA, events.get(0).getDetector());
        Assert.assertEquals(t1, finalState.getDate().durationFrom(epoch).getReal(), tolerance);

        // try to resume propagation
        finalState = propagator.propagate(epoch.shiftedBy(-30.0));

        // verify it got to the end
        Assert.assertEquals(-30.0, finalState.getDate().durationFrom(epoch).getReal(), 0.0);
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
        List<Event<FieldEventDetector<T>, T>> events = new ArrayList<>();
        // never zero so there is no easy way out
        FieldEventDetector<T> detectorA = new FieldAbstractDetector<FieldEventDetector<T>, T>
                (v(maxCheck), v(tolerance), 100, new FieldRecordAndContinue<>(events)) {

            @Override
            public T g(FieldSpacecraftState<T> state) {
                final FieldAbsoluteDate<T> t = state.getDate();
                if (t.compareTo(epoch.shiftedBy(t1)) > 0) {
                    return v(-1);
                } else if (t.compareTo(epoch.shiftedBy(t2)) > 0) {
                    return v(1);
                } else {
                    return v(-1);
                }
            }

            @Override
            protected FieldEventDetector<T> create(
                    T newMaxCheck,
                    T newThreshold,
                    int newMaxIter,
                    FieldEventHandler<? super FieldEventDetector<T>, T> newHandler) {
                return null;
            }
        };
        TimeDetector detectorB = new TimeDetector(t1)
                .withHandler(new FieldRecordAndContinue<>(events))
                .withMaxCheck(v(maxCheck))
                .withThreshold(v(tolerance));
        FieldPropagator<T> propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);
        propagator.addEventDetector(detectorB);

        // action
        propagator.propagate(epoch.shiftedBy(-30.0));

        // verify
        Assert.assertEquals(3, events.size());
        Assert.assertEquals(t1, events.get(0).getState().getDate().durationFrom(epoch).getReal(), tolerance);
        Assert.assertEquals(false, events.get(0).isIncreasing());
        Assert.assertSame(detectorA, events.get(0).getDetector());
        Assert.assertEquals(t1, events.get(1).getState().getDate().durationFrom(epoch).getReal(), tolerance);
        Assert.assertEquals(true, events.get(1).isIncreasing());
        Assert.assertSame(detectorB, events.get(1).getDetector());
        Assert.assertEquals(t2, events.get(2).getState().getDate().durationFrom(epoch).getReal(), tolerance);
        Assert.assertEquals(true, events.get(2).isIncreasing());
        Assert.assertSame(detectorA, events.get(2).getDetector());
    }

    /** check when root finding tolerance > event finding tolerance. */
    @Test
    public void testToleranceMaxIterationsReverse() {
        // setup
        double maxCheck = 10;
        double tolerance = 1e-18; // less than 1 ulp
        FieldAbsoluteDate<T> t1 = epoch.shiftedBy(-15).shiftedBy(FastMath.ulp(-15.0) / 8);
        // shared event list so we know the order in which they occurred
        List<Event<FieldEventDetector<T>, T>> events = new ArrayList<>();
        FlatDetector detectorA = new FlatDetector(t1)
                .withHandler(new FieldRecordAndContinue<>(events))
                .withMaxCheck(v(maxCheck))
                .withThreshold(v(tolerance));
        FieldPropagator<T> propagator = getPropagator(10);
        propagator.addEventDetector(detectorA);

        // action
        propagator.propagate(epoch.shiftedBy(-30));

        // verify
        Assert.assertEquals(1, events.size());
        // use root finder tolerance instead of event finder tolerance.
        Assert.assertEquals(t1.durationFrom(epoch).getReal(),
                events.get(0).getState().getDate().durationFrom(epoch).getReal(),
                FastMath.ulp(-15.0));
        Assert.assertEquals(true, events.get(0).isIncreasing());
        Assert.assertSame(detectorA, events.get(0).getDetector());
    }



    /* utility classes and methods */

    /**
     * Create a state at a time.
     *
     * @param t time of state.
     * @return new state.
     */
    private FieldSpacecraftState<T> state(double t) {
        return new FieldSpacecraftState<>(new FieldKeplerianOrbit<>(
                v(6378137 + 500e3), v(0), v(0), v(0), v(0), v(0),
                PositionAngle.TRUE, eci, epoch.shiftedBy(t),
                v(mu)));
    }

    private List<FieldAbsoluteDate<T>> toDates(double[] eventTs) {
        Arrays.sort(eventTs);
        final List<FieldAbsoluteDate<T>> ret = new ArrayList<>();
        for (double eventT : eventTs) {
            ret.add(epoch.shiftedBy(eventT));
        }
        return ret;
    }

    /**
     * Value of.
     * @param value to wrap
     * @return {@code value} as type T.
     */
    public T v(double value) {
        return field.getZero().add(value);
    }

    /** Trigger an event at a particular time. */
    private class TimeDetector extends FieldAbstractDetector<TimeDetector, T> {

        /** time of the event to trigger. */
        private final List<FieldAbsoluteDate<T>> eventTs;

        /**
         * Create a detector that finds events at specific times.
         *
         * @param eventTs event times past epoch.
         */
        public TimeDetector(double... eventTs) {
            this(v(DEFAULT_MAXCHECK), v(DEFAULT_THRESHOLD), DEFAULT_MAX_ITER,
                    new FieldStopOnEvent<>(), toDates(eventTs));
        }

        /**
         * Create a detector that finds events at specific times.
         *
         * @param eventTs event times past epoch.
         */
        @SafeVarargs
        public TimeDetector(FieldAbsoluteDate<T>... eventTs) {
            this(v(DEFAULT_MAXCHECK), v(DEFAULT_THRESHOLD), DEFAULT_MAX_ITER,
                    new FieldStopOnEvent<>(), Arrays.asList(eventTs));
        }

        private TimeDetector(T newMaxCheck,
                             T newThreshold,
                             int newMaxIter,
                             FieldEventHandler<? super TimeDetector, T> newHandler,
                             List<FieldAbsoluteDate<T>> dates) {
            super(newMaxCheck, newThreshold, newMaxIter, newHandler);
            this.eventTs = dates;
        }

        @Override
        public T g(FieldSpacecraftState<T> s) {
            final FieldAbsoluteDate<T> t = s.getDate();
            int i = 0;
            while (i < eventTs.size() && t.compareTo(eventTs.get(i)) > 0) {
                i++;
            }
            i--;
            if (i < 0) {
                return t.durationFrom(eventTs.get(0));
            } else {
                int sign = (i % 2) * 2 - 1;
                return (t.durationFrom(eventTs.get(i))).multiply(-sign);
            }
        }

        @Override
        protected TimeDetector create(T newMaxCheck,
                                      T newThreshold,
                                      int newMaxIter,
                                      FieldEventHandler<? super TimeDetector, T> newHandler) {
            return new TimeDetector(
                    newMaxCheck, newThreshold, newMaxIter, newHandler, eventTs);
        }

    }

    /**
     * Same as {@link TimeDetector} except that it has a very flat g function which makes
     * root finding hard.
     */
    private class FlatDetector extends FieldAbstractDetector<FlatDetector, T> {

        private final FieldEventDetector<T> g;

        public FlatDetector(double... eventTs) {
            this(v(DEFAULT_MAXCHECK), v(DEFAULT_THRESHOLD), DEFAULT_MAX_ITER,
                    new FieldStopOnEvent<>(), new TimeDetector(eventTs));
        }

        @SafeVarargs
        public FlatDetector(FieldAbsoluteDate<T>... eventTs) {
            this(v(DEFAULT_MAXCHECK), v(DEFAULT_THRESHOLD), DEFAULT_MAX_ITER,
                    new FieldStopOnEvent<>(), new TimeDetector(eventTs));
        }

        private FlatDetector(T newMaxCheck,
                             T newThreshold,
                             int newMaxIter,
                             FieldEventHandler<? super FlatDetector, T> newHandler,
                             FieldEventDetector<T> g) {
            super(newMaxCheck, newThreshold, newMaxIter, newHandler);
            this.g = g;
        }

        @Override
        public T g(FieldSpacecraftState<T> s) {
            return FastMath.signum(g.g(s));
        }

        @Override
        protected FlatDetector create(T newMaxCheck,
                                      T newThreshold,
                                      int newMaxIter,
                                      FieldEventHandler<? super FlatDetector, T> newHandler) {
            return new FlatDetector(newMaxCheck, newThreshold, newMaxIter, newHandler, g);
        }

    }

    /** quadratic. */
    private class ContinuousDetector extends FieldAbstractDetector<ContinuousDetector, T> {

        /** time of the event to trigger. */
        private final List<FieldAbsoluteDate<T>> eventTs;

        public ContinuousDetector(double... eventTs) {
            this(v(DEFAULT_MAXCHECK), v(DEFAULT_THRESHOLD), DEFAULT_MAX_ITER,
                    new FieldStopOnEvent<>(), toDates(eventTs));
        }

        private ContinuousDetector(T newMaxCheck,
                                   T newThreshold,
                                   int newMaxIter,
                                   FieldEventHandler<? super ContinuousDetector, T> newHandler,
                                   List<FieldAbsoluteDate<T>> eventDates) {
            super(newMaxCheck, newThreshold, newMaxIter, newHandler);
            this.eventTs = eventDates;
        }

        @Override
        public T g(FieldSpacecraftState<T> s) {
            final FieldAbsoluteDate<T> t = s.getDate();
            int i = 0;
            while (i < eventTs.size() && t.compareTo(eventTs.get(i)) > 0) {
                i++;
            }
            i--;
            if (i < 0) {
                return t.durationFrom(eventTs.get(0));
            } else if (i < eventTs.size() - 1) {
                int sign = (i % 2) * 2 - 1;
                return (t.durationFrom(eventTs.get(i)))
                        .multiply(eventTs.get(i + 1).durationFrom(t)).multiply(-sign);
            } else {
                int sign = (i % 2) * 2 - 1;
                return (t.durationFrom(eventTs.get(i))).multiply(-sign);
            }
        }

        @Override
        protected ContinuousDetector create(
                T newMaxCheck,
                T newThreshold,
                int newMaxIter,
                FieldEventHandler<? super ContinuousDetector, T> newHandler) {
            return new ContinuousDetector(
                    newMaxCheck, newThreshold, newMaxIter, newHandler, eventTs);
        }

    }

    private class Handler<D extends FieldEventDetector<T>>
            extends FieldRecordAndContinue<D, T> {

        private final Action action;

        public Handler(Action action) {
            this.action = action;
        }

        public Handler(List<Event<D, T>> events, Action action) {
            super(events);
            this.action = action;
        }

        @Override
        public Action eventOccurred(FieldSpacecraftState<T> s,
                                    D detector,
                                    boolean increasing) {
            super.eventOccurred(s, detector, increasing);
            return this.action;
        }

    }

}
