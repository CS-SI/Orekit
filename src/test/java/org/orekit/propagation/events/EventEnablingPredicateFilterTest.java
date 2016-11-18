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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FramesFactory;
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
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

public class EventEnablingPredicateFilterTest {

    private OneAxisEllipsoid earth;
    private GeodeticPoint gp;
    private Orbit orbit;

    @Test
    public void testForward0Degrees() throws OrekitException {
        doElevationTest(FastMath.toRadians(0.0),
               orbit.getDate(),
               orbit.getDate().shiftedBy(Constants.JULIAN_DAY),
               8, true);
    }

    @Test
    public void testForward5Degrees() throws OrekitException {
        doElevationTest(FastMath.toRadians(5.0),
               orbit.getDate(),
               orbit.getDate().shiftedBy(Constants.JULIAN_DAY),
               6, false);
    }

    @Test
    public void testForward5DegreesStartEnabled() throws OrekitException {
        doElevationTest(FastMath.toRadians(5.0),
               orbit.getDate().shiftedBy(12614.0),
               orbit.getDate().shiftedBy(Constants.JULIAN_DAY),
               6, false);
    }

    @Test
    public void testBackward0Degrees() throws OrekitException {
        doElevationTest(FastMath.toRadians(0.0),
               orbit.getDate().shiftedBy(Constants.JULIAN_DAY),
               orbit.getDate(),
               8, true);
    }

    @Test
    public void testBackward5Degrees() throws OrekitException {
        doElevationTest(FastMath.toRadians(5.0),
               orbit.getDate().shiftedBy(Constants.JULIAN_DAY),
               orbit.getDate(),
               6, false);
    }

    @Test
    public void testBackward5DegreesStartEnabled() throws OrekitException {
        doElevationTest(FastMath.toRadians(5.0),
               orbit.getDate().shiftedBy(73112.0),
               orbit.getDate(),
               6, true);
    }

    private void doElevationTest(final double minElevation,
                                 final AbsoluteDate start, final AbsoluteDate end,
                                 final int expectedEvents, final boolean sameSign) throws OrekitException {

        final ElevationExtremumDetector raw =
                new ElevationExtremumDetector(0.001, 1.e-6, new TopocentricFrame(earth, gp, "test")).
                withHandler(new ContinueOnEvent<ElevationExtremumDetector>());
        final EventEnablingPredicateFilter<ElevationExtremumDetector> aboveGroundElevationDetector =
                new EventEnablingPredicateFilter<ElevationExtremumDetector>(raw,
                                new EnablingPredicate<ElevationExtremumDetector>() {
                                    public boolean eventIsEnabled(final SpacecraftState state,
                                                                  final ElevationExtremumDetector eventDetector,
                                                                  final double g) throws OrekitException {
                                        return eventDetector.getElevation(state) > minElevation;
                                    }
                }).withMaxCheck(60.0);

        Assert.assertEquals(0.001, raw.getMaxCheckInterval(), 1.0e-15);
        Assert.assertEquals(60.0, aboveGroundElevationDetector.getMaxCheckInterval(), 1.0e-15);
        Assert.assertEquals(1.0e-6, aboveGroundElevationDetector.getThreshold(), 1.0e-15);
        Assert.assertEquals(AbstractDetector.DEFAULT_MAX_ITER, aboveGroundElevationDetector.getMaxIterationCount());


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
            Assert.assertTrue(e0 > eMinus);
            Assert.assertTrue(e0 > ePlus);
            Assert.assertTrue(e0 > minElevation);
        }
        Assert.assertEquals(expectedEvents, logger.getLoggedEvents().size());

        propagator.clearEventsDetectors();
        double g1Raw = raw.g(propagator.propagate(orbit.getDate().shiftedBy(18540.0)));
        double g2Raw = raw.g(propagator.propagate(orbit.getDate().shiftedBy(18624.0)));
        double g1 = aboveGroundElevationDetector.g(propagator.propagate(orbit.getDate().shiftedBy(18540.0)));
        double g2 = aboveGroundElevationDetector.g(propagator.propagate(orbit.getDate().shiftedBy(18624.0)));
        Assert.assertTrue(g1Raw > 0);
        Assert.assertTrue(g2Raw < 0);
        if (sameSign) {
            Assert.assertTrue(g1 > 0);
            Assert.assertTrue(g2 < 0);
        } else {
            Assert.assertTrue(g1 < 0);
            Assert.assertTrue(g2 > 0);
        }

    }

    @Test
    public void testResetState() throws OrekitException {
        final List<AbsoluteDate> reset = new ArrayList<AbsoluteDate>();
        DateDetector raw = new DateDetector(orbit.getDate().shiftedBy(3600.0)).
                        withMaxCheck(1000.0).
                        withHandler(new EventHandler<DateDetector>() {
                            public SpacecraftState resetState(DateDetector detector,
                                                              SpacecraftState oldState) {
                                reset.add(oldState.getDate());
                                return oldState;
                            }
                            public Action eventOccurred(SpacecraftState s,
                                                        DateDetector detector,
                                                        boolean increasing) {
                                return Action.RESET_STATE;
                            }
                        });
        for (int i = 2; i < 10; ++i) {
            raw.addEventDate(orbit.getDate().shiftedBy(i * 3600.0));
        }
        EventEnablingPredicateFilter<DateDetector> filtered =
                        new EventEnablingPredicateFilter<DateDetector>(raw, new EnablingPredicate<DateDetector>() {
                            public boolean eventIsEnabled(SpacecraftState state,
                                                          DateDetector eventDetector,
                                                          double g) {
                                return state.getDate().durationFrom(orbit.getDate()) > 20000.0;
                            }
                        });
        Propagator propagator = new KeplerianPropagator(orbit);
        EventsLogger logger = new EventsLogger();
        propagator.addEventDetector(logger.monitorDetector(filtered));
        propagator.propagate(orbit.getDate().shiftedBy(Constants.JULIAN_DAY));
        List<LoggedEvent> events = logger.getLoggedEvents();
        Assert.assertEquals(4, events.size());
        Assert.assertEquals(6 * 3600, events.get(0).getState().getDate().durationFrom(orbit.getDate()), 1.0e-6);
        Assert.assertEquals(7 * 3600, events.get(1).getState().getDate().durationFrom(orbit.getDate()), 1.0e-6);
        Assert.assertEquals(8 * 3600, events.get(2).getState().getDate().durationFrom(orbit.getDate()), 1.0e-6);
        Assert.assertEquals(9 * 3600, events.get(3).getState().getDate().durationFrom(orbit.getDate()), 1.0e-6);
        Assert.assertEquals(4, reset.size());
        Assert.assertEquals(6 * 3600, reset.get(0).durationFrom(orbit.getDate()), 1.0e-6);
        Assert.assertEquals(7 * 3600, reset.get(1).durationFrom(orbit.getDate()), 1.0e-6);
        Assert.assertEquals(8 * 3600, reset.get(2).durationFrom(orbit.getDate()), 1.0e-6);
        Assert.assertEquals(9 * 3600, reset.get(3).durationFrom(orbit.getDate()), 1.0e-6);

    }

    @Test
    public void testExceedHistoryForward() throws OrekitException, IOException {
        final double period = 900.0;

        // the raw detector should trigger one event at each 900s period
        final DateDetector raw = new DateDetector(orbit.getDate().shiftedBy(-0.5 * period)).
                                 withMaxCheck(period / 3).
                                 withHandler(new ContinueOnEvent<DateDetector>());
        for (int i = 0; i < 300; ++i) {
            raw.addEventDate(orbit.getDate().shiftedBy((i + 0.5) * period));
        }

        // in fact, we will filter out half of these events, so we get only one event every 2 periods
        final EventEnablingPredicateFilter<DateDetector> filtered =
                        new EventEnablingPredicateFilter<DateDetector>(raw, new EnablingPredicate<DateDetector>() {
                            public boolean eventIsEnabled(SpacecraftState state,
                                                          DateDetector eventDetector,
                                                          double g) {
                                double nbPeriod = state.getDate().durationFrom(orbit.getDate()) / period;
                                return ((int) FastMath.floor(nbPeriod)) % 2 == 1;
                            }
                        });
        Propagator propagator = new KeplerianPropagator(orbit);
        EventsLogger logger = new EventsLogger();
        propagator.addEventDetector(logger.monitorDetector(filtered));
        propagator.propagate(orbit.getDate().shiftedBy(301 * period));
        List<LoggedEvent> events = logger.getLoggedEvents();

        // 300 periods, 150 events as half of them are filtered out
        Assert.assertEquals(150, events.size());

        // as we have encountered a lot of enabling status changes, we exceeded the internal history
        // if we try to display again the filtered g function for dates far in the past,
        // we will not see the zero crossings anymore, they have been lost
        propagator.clearEventsDetectors();
        for (double dt = 5000.0; dt < 10000.0; dt += 3.0) {
            double filteredG = filtered.g(propagator.propagate(orbit.getDate().shiftedBy(dt)));
            Assert.assertTrue(filteredG < 0.0);
        }

        // on the other hand, if we try to display again the filtered g function for past dates
        // that are still inside the history, we still see the zero crossings
        for (double dt = 195400.0; dt < 196200.0; dt += 3.0) {
            double filteredG = filtered.g(propagator.propagate(orbit.getDate().shiftedBy(dt)));
            if (dt < 195750) {
                Assert.assertTrue(filteredG > 0.0);
            } else {
                Assert.assertTrue(filteredG < 0.0);
            }
        }

    }

    @Test
    public void testExceedHistoryBackward() throws OrekitException, IOException {
        final double period = 900.0;

        // the raw detector should trigger one event at each 900s period
        final DateDetector raw = new DateDetector(orbit.getDate().shiftedBy(+0.5 * period)).
                                 withMaxCheck(period / 3).
                                 withHandler(new ContinueOnEvent<DateDetector>());
        for (int i = 0; i < 300; ++i) {
            raw.addEventDate(orbit.getDate().shiftedBy(-(i + 0.5) * period));
        }

        // in fact, we will filter out half of these events, so we get only one event every 2 periods
        final EventEnablingPredicateFilter<DateDetector> filtered =
                        new EventEnablingPredicateFilter<DateDetector>(raw, new EnablingPredicate<DateDetector>() {
                            public boolean eventIsEnabled(SpacecraftState state,
                                                          DateDetector eventDetector,
                                                          double g) {
                                double nbPeriod = orbit.getDate().durationFrom(state.getDate()) / period;
                                return ((int) FastMath.floor(nbPeriod)) % 2 == 1;
                            }
                        });
        Propagator propagator = new KeplerianPropagator(orbit);
        EventsLogger logger = new EventsLogger();
        propagator.addEventDetector(logger.monitorDetector(filtered));
        propagator.propagate(orbit.getDate().shiftedBy(-301 * period));
        List<LoggedEvent> events = logger.getLoggedEvents();

        // 300 periods, 150 events as half of them are filtered out
        Assert.assertEquals(150, events.size());

        // as we have encountered a lot of enabling status changes, we exceeded the internal history
        // if we try to display again the filtered g function for dates far in the future,
        // we will not see the zero crossings anymore, they have been lost
        propagator.clearEventsDetectors();
        for (double dt = -5000.0; dt > -10000.0; dt -= 3.0) {
            double filteredG = filtered.g(propagator.propagate(orbit.getDate().shiftedBy(dt)));
            Assert.assertTrue(filteredG < 0.0);
        }

        // on the other hand, if we try to display again the filtered g function for future dates
        // that are still inside the history, we still see the zero crossings
        for (double dt = -195400.0; dt > -196200.0; dt -= 3.0) {
            double filteredG = filtered.g(propagator.propagate(orbit.getDate().shiftedBy(dt)));
            if (dt < -195750) {
                Assert.assertTrue(filteredG < 0.0);
            } else {
                Assert.assertTrue(filteredG > 0.0);
            }
        }

    }

    @Test
    public void testNonSerializable() throws IOException {
        try {
            final EventEnablingPredicateFilter<DateDetector> filter =
                            new EventEnablingPredicateFilter<DateDetector>(new DateDetector(AbsoluteDate.J2000_EPOCH),
                                                                           new  DummyNonSerializablePredicate());
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream    oos = new ObjectOutputStream(bos);
            oos.writeObject(filter);
            Assert.fail("an exception should habe been thrown");
        } catch (NotSerializableException nse) {
            Assert.assertTrue(nse.getMessage().contains("DummyNonSerializablePredicate"));
        }
    }

    private static class DummyNonSerializablePredicate implements EnablingPredicate<DateDetector> {

        @Override
        public boolean eventIsEnabled(final SpacecraftState state,
                                      final DateDetector eventDetector,
                                      final double g) {
            return true;
        }

    }

    @Test
    public void testSerializable()
        throws IOException, IllegalArgumentException, IllegalAccessException,
               ClassNotFoundException, NoSuchFieldException, SecurityException {
        final EventEnablingPredicateFilter<DateDetector> filter =
                        new EventEnablingPredicateFilter<DateDetector>(new DateDetector(AbsoluteDate.J2000_EPOCH),
                                                                       new  DummySerializablePredicate());
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream    oos = new ObjectOutputStream(bos);
        oos.writeObject(filter);

        Assert.assertTrue(bos.size() >  900);
        Assert.assertTrue(bos.size() < 1000);

        ByteArrayInputStream  bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream     ois = new ObjectInputStream(bis);
        @SuppressWarnings("unchecked")
        EventEnablingPredicateFilter<DateDetector> deserialized  =
                        (EventEnablingPredicateFilter<DateDetector>) ois.readObject();
        Field enabler = EventEnablingPredicateFilter.class.getDeclaredField("enabler");
        enabler.setAccessible(true);
        Assert.assertEquals(DummySerializablePredicate.class, enabler.get(deserialized).getClass());
    }

    private static class DummySerializablePredicate implements EnablingPredicate<DateDetector>, Serializable {

        private static final long serialVersionUID = 20160321L;

        @Override
        public boolean eventIsEnabled(final SpacecraftState state,
                                      final DateDetector eventDetector,
                                      final double g) {
            return true;
        }

    }

    @Before
    public void setUp() throws OrekitException {

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

    @After
    public void tearDown() {
        earth = null;
        gp    = null;
        orbit = null;
    }

}

