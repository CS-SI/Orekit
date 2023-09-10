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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.ode.events.Action;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.orbits.FieldEquinoctialOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.propagation.FieldPropagator;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.analytical.FieldEcksteinHechlerPropagator;
import org.orekit.propagation.analytical.FieldKeplerianPropagator;
import org.orekit.propagation.events.FieldEventsLogger.FieldLoggedEvent;
import org.orekit.propagation.events.handlers.FieldContinueOnEvent;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.IERSConventions;

public class FieldEventEnablingPredicateFilterTest {

    private OneAxisEllipsoid earth;
    private GeodeticPoint gp;
    private FieldOrbit<Binary64> orbit;

    @Test
    public void testForward0Degrees() {
        doElevationTest(FastMath.toRadians(0.0),
               orbit.getDate(),
               orbit.getDate().shiftedBy(Constants.JULIAN_DAY),
               8, true);
    }

    @Test
    public void testForward5Degrees() {
        doElevationTest(FastMath.toRadians(5.0),
               orbit.getDate(),
               orbit.getDate().shiftedBy(Constants.JULIAN_DAY),
               6, false);
    }

    @Test
    public void testForward5DegreesStartEnabled() {
        doElevationTest(FastMath.toRadians(5.0),
               orbit.getDate().shiftedBy(12614.0),
               orbit.getDate().shiftedBy(Constants.JULIAN_DAY),
               6, false);
    }

    @Test
    public void testBackward0Degrees() {
        doElevationTest(FastMath.toRadians(0.0),
               orbit.getDate().shiftedBy(Constants.JULIAN_DAY),
               orbit.getDate(),
               8, true);
    }

    @Test
    public void testBackward5Degrees() {
        doElevationTest(FastMath.toRadians(5.0),
               orbit.getDate().shiftedBy(Constants.JULIAN_DAY),
               orbit.getDate(),
               6, false);
    }

    @Test
    public void testBackward5DegreesStartEnabled() {
        doElevationTest(FastMath.toRadians(5.0),
               orbit.getDate().shiftedBy(73112.0),
               orbit.getDate(),
               6, true);
    }

    private void doElevationTest(final double minElevation,
                                 final FieldAbsoluteDate<Binary64> start, final FieldAbsoluteDate<Binary64> end,
                                 final int expectedEvents, final boolean sameSign) {

        final FieldElevationExtremumDetector<Binary64> raw =
                new FieldElevationExtremumDetector<>(new Binary64(0.001), new Binary64(1.e-6), new TopocentricFrame(earth, gp, "test")).
                withHandler(new FieldContinueOnEvent<>());
        final FieldEventEnablingPredicateFilter<Binary64> aboveGroundElevationDetector =
                new FieldEventEnablingPredicateFilter<>(raw,
                                                        new FieldEnablingPredicate<Binary64>() {
                    public boolean eventIsEnabled(final FieldSpacecraftState<Binary64> state,
                                                  final FieldEventDetector<Binary64> eventDetector,
                                                  final Binary64 g) {
                        return ((FieldElevationExtremumDetector<Binary64>) eventDetector).getElevation(state).getReal() > minElevation;
                    }
                }).withMaxCheck(60.0);

        Assertions.assertSame(raw, aboveGroundElevationDetector.getDetector());
        Assertions.assertEquals(0.001, raw.getMaxCheckInterval().currentInterval(null), 1.0e-15);
        Assertions.assertEquals(60.0, aboveGroundElevationDetector.getMaxCheckInterval().currentInterval(null), 1.0e-15);
        Assertions.assertEquals(1.0e-6, aboveGroundElevationDetector.getThreshold().getReal(), 1.0e-15);
        Assertions.assertEquals(AbstractDetector.DEFAULT_MAX_ITER, aboveGroundElevationDetector.getMaxIterationCount());


        FieldPropagator<Binary64> propagator =
            new FieldEcksteinHechlerPropagator<>(orbit,
                                                 Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS,
                                                 new Binary64(Constants.EIGEN5C_EARTH_MU),
                                                 Constants.EIGEN5C_EARTH_C20,
                                                 Constants.EIGEN5C_EARTH_C30,
                                                 Constants.EIGEN5C_EARTH_C40,
                                                 Constants.EIGEN5C_EARTH_C50,
                                                 Constants.EIGEN5C_EARTH_C60);

        FieldEventsLogger<Binary64> logger = new FieldEventsLogger<>();
        propagator.addEventDetector(logger.monitorDetector(aboveGroundElevationDetector));

        propagator.propagate(start, end);
        for (FieldLoggedEvent<Binary64> e : logger.getLoggedEvents()) {
            final double eMinus = raw.getElevation(e.getState().shiftedBy(-10.0)).getReal();
            final double e0     = raw.getElevation(e.getState()).getReal();
            final double ePlus  = raw.getElevation(e.getState().shiftedBy(+10.0)).getReal();
            Assertions.assertTrue(e0 > eMinus);
            Assertions.assertTrue(e0 > ePlus);
            Assertions.assertTrue(e0 > minElevation);
        }
        Assertions.assertEquals(expectedEvents, logger.getLoggedEvents().size());

        propagator.clearEventsDetectors();
        double g1Raw = raw.g(propagator.propagate(orbit.getDate().shiftedBy(18540.0))).getReal();
        double g2Raw = raw.g(propagator.propagate(orbit.getDate().shiftedBy(18624.0))).getReal();
        double g1 = aboveGroundElevationDetector.g(propagator.propagate(orbit.getDate().shiftedBy(18540.0))).getReal();
        double g2 = aboveGroundElevationDetector.g(propagator.propagate(orbit.getDate().shiftedBy(18624.0))).getReal();
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
    public void testResetState() {
        final List<FieldAbsoluteDate<Binary64>> reset = new ArrayList<>();
        @SuppressWarnings("unchecked")
        FieldDateDetector<Binary64> raw = new FieldDateDetector<>(Binary64Field.getInstance(), orbit.getDate().shiftedBy(3600.0)).
                        withMaxCheck(1000.0).
                        withHandler(new FieldEventHandler<Binary64>() {
                            public FieldSpacecraftState<Binary64> resetState(FieldEventDetector<Binary64> detector,
                                                                             FieldSpacecraftState<Binary64> oldState) {
                                reset.add(oldState.getDate());
                                return oldState;
                            }
                            public Action eventOccurred(FieldSpacecraftState<Binary64> s,
                                                        FieldEventDetector<Binary64> detector,
                                                        boolean increasing) {
                                return Action.RESET_STATE;
                            }
                        });
        for (int i = 2; i < 10; ++i) {
            raw.addEventDate(orbit.getDate().shiftedBy(i * 3600.0));
        }
        FieldEventEnablingPredicateFilter<Binary64> filtered =
                        new FieldEventEnablingPredicateFilter<Binary64>(raw, new FieldEnablingPredicate<Binary64>() {
                            public boolean eventIsEnabled(FieldSpacecraftState<Binary64> state,
                                                          FieldEventDetector<Binary64> eventDetector,
                                                          Binary64 g) {
                                return state.getDate().durationFrom(orbit.getDate()).getReal() > 20000.0;
                            }
                        });
        FieldPropagator<Binary64> propagator = new FieldKeplerianPropagator<>(orbit);
        FieldEventsLogger<Binary64> logger = new FieldEventsLogger<>();
        propagator.addEventDetector(logger.monitorDetector(filtered));
        propagator.propagate(orbit.getDate().shiftedBy(Constants.JULIAN_DAY));
        List<FieldLoggedEvent<Binary64>> events = logger.getLoggedEvents();
        Assertions.assertEquals(4, events.size());
        Assertions.assertEquals(6 * 3600, events.get(0).getState().getDate().durationFrom(orbit.getDate()).getReal(), 1.0e-6);
        Assertions.assertEquals(7 * 3600, events.get(1).getState().getDate().durationFrom(orbit.getDate()).getReal(), 1.0e-6);
        Assertions.assertEquals(8 * 3600, events.get(2).getState().getDate().durationFrom(orbit.getDate()).getReal(), 1.0e-6);
        Assertions.assertEquals(9 * 3600, events.get(3).getState().getDate().durationFrom(orbit.getDate()).getReal(), 1.0e-6);
        Assertions.assertEquals(4, reset.size());
        Assertions.assertEquals(6 * 3600, reset.get(0).durationFrom(orbit.getDate()).getReal(), 1.0e-6);
        Assertions.assertEquals(7 * 3600, reset.get(1).durationFrom(orbit.getDate()).getReal(), 1.0e-6);
        Assertions.assertEquals(8 * 3600, reset.get(2).durationFrom(orbit.getDate()).getReal(), 1.0e-6);
        Assertions.assertEquals(9 * 3600, reset.get(3).durationFrom(orbit.getDate()).getReal(), 1.0e-6);

    }

    @Test
    public void testExceedHistoryForward() throws IOException {
        final double period = 900.0;

        // the raw detector should trigger one event at each 900s period
        @SuppressWarnings("unchecked")
        final FieldDateDetector<Binary64> raw = new FieldDateDetector<>(Binary64Field.getInstance(),
                                                                        orbit.getDate().shiftedBy(-0.5 * period)).
                                                withMaxCheck(period / 3).
                                                withHandler(new FieldContinueOnEvent<>());
        for (int i = 0; i < 300; ++i) {
            raw.addEventDate(orbit.getDate().shiftedBy((i + 0.5) * period));
        }

        // in fact, we will filter out half of these events, so we get only one event every 2 periods
        final FieldEventEnablingPredicateFilter<Binary64> filtered =
                        new FieldEventEnablingPredicateFilter<>(raw, new FieldEnablingPredicate<Binary64>() {
                            public boolean eventIsEnabled(FieldSpacecraftState<Binary64> state,
                                                          FieldEventDetector<Binary64> eventDetector,
                                                          Binary64 g) {
                                double nbPeriod = state.getDate().durationFrom(orbit.getDate()).getReal() / period;
                                return ((int) FastMath.floor(nbPeriod)) % 2 == 1;
                            }
                        });
        FieldPropagator<Binary64> propagator = new FieldKeplerianPropagator<>(orbit);
        FieldEventsLogger<Binary64> logger = new FieldEventsLogger<>();
        propagator.addEventDetector(logger.monitorDetector(filtered));
        propagator.propagate(orbit.getDate().shiftedBy(301 * period));
        List<FieldLoggedEvent<Binary64>> events = logger.getLoggedEvents();

        // 300 periods, 150 events as half of them are filtered out
        Assertions.assertEquals(150, events.size());

        // as we have encountered a lot of enabling status changes, we exceeded the internal history
        // if we try to display again the filtered g function for dates far in the past,
        // we will not see the zero crossings anymore, they have been lost
        propagator.clearEventsDetectors();
        for (double dt = 5000.0; dt < 10000.0; dt += 3.0) {
            double filteredG = filtered.g(propagator.propagate(orbit.getDate().shiftedBy(dt))).getReal();
            Assertions.assertTrue(filteredG < 0.0);
        }

        // on the other hand, if we try to display again the filtered g function for past dates
        // that are still inside the history, we still see the zero crossings
        for (double dt = 195400.0; dt < 196200.0; dt += 3.0) {
            double filteredG = filtered.g(propagator.propagate(orbit.getDate().shiftedBy(dt))).getReal();
            if (dt < 195750) {
                Assertions.assertTrue(filteredG > 0.0);
            } else {
                Assertions.assertTrue(filteredG < 0.0);
            }
        }

    }

    @Test
    public void testExceedHistoryBackward() throws IOException {
        final double period = 900.0;

        // the raw detector should trigger one event at each 900s period
        @SuppressWarnings("unchecked")
        final FieldDateDetector<Binary64> raw = new FieldDateDetector<>(Binary64Field.getInstance(),
                                                                        orbit.getDate().shiftedBy(+0.5 * period)).
                                                withMaxCheck(period / 3).
                                                withHandler(new FieldContinueOnEvent<>());
        for (int i = 0; i < 300; ++i) {
            raw.addEventDate(orbit.getDate().shiftedBy(-(i + 0.5) * period));
        }

        // in fact, we will filter out half of these events, so we get only one event every 2 periods
        final FieldEventEnablingPredicateFilter<Binary64> filtered =
                        new FieldEventEnablingPredicateFilter<>(raw, new FieldEnablingPredicate<Binary64>() {
                            public boolean eventIsEnabled(FieldSpacecraftState<Binary64> state,
                                                          FieldEventDetector<Binary64> eventDetector,
                                                          Binary64 g) {
                                double nbPeriod = orbit.getDate().durationFrom(state.getDate()).getReal() / period;
                                return ((int) FastMath.floor(nbPeriod)) % 2 == 1;
                            }
                        });
        FieldPropagator<Binary64> propagator = new FieldKeplerianPropagator<>(orbit);
        FieldEventsLogger<Binary64> logger = new FieldEventsLogger<>();
        propagator.addEventDetector(logger.monitorDetector(filtered));
        propagator.propagate(orbit.getDate().shiftedBy(-301 * period));
        List<FieldLoggedEvent<Binary64>> events = logger.getLoggedEvents();

        // 300 periods, 150 events as half of them are filtered out
        Assertions.assertEquals(150, events.size());

        // as we have encountered a lot of enabling status changes, we exceeded the internal history
        // if we try to display again the filtered g function for dates far in the future,
        // we will not see the zero crossings anymore, they have been lost
        propagator.clearEventsDetectors();
        for (double dt = -5000.0; dt > -10000.0; dt -= 3.0) {
            double filteredG = filtered.g(propagator.propagate(orbit.getDate().shiftedBy(dt))).getReal();
            Assertions.assertTrue(filteredG < 0.0);
        }

        // on the other hand, if we try to display again the filtered g function for future dates
        // that are still inside the history, we still see the zero crossings
        for (double dt = -195400.0; dt > -196200.0; dt -= 3.0) {
            double filteredG = filtered.g(propagator.propagate(orbit.getDate().shiftedBy(dt))).getReal();
            if (dt < -195750) {
                Assertions.assertTrue(filteredG < 0.0);
            } else {
                Assertions.assertTrue(filteredG > 0.0);
            }
        }

    }

    @Test
    public void testGenerics() {
        // setup
        @SuppressWarnings("unchecked")
        FieldDateDetector<Binary64> detector = new FieldDateDetector<>(orbit.getDate().getField(), orbit.getDate());
        FieldEnablingPredicate<Binary64> predicate = (state, eventDetector, g) -> true;

        // action + verify. Just make sure it compiles with generics
        new FieldEventEnablingPredicateFilter<>(detector, predicate);
    }

    @BeforeEach
    public void setUp() {

        Utils.setDataRoot("regular-data");
        earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                     Constants.WGS84_EARTH_FLATTENING,
                                     FramesFactory.getITRF(IERSConventions.IERS_2010, true));

        gp = new GeodeticPoint(FastMath.toRadians(51.0), FastMath.toRadians(66.6), 300.0);
        final TimeScale utc = TimeScalesFactory.getUTC();
        final FieldVector3D<Binary64> position = new FieldVector3D<>(new Binary64(-6142438.668),
                                                                     new Binary64(3492467.56),
                                                                     new Binary64(-25767.257));
        final FieldVector3D<Binary64> velocity = new FieldVector3D<>(new Binary64(505.848),
                                                                     new Binary64(942.781),
                                                                     new Binary64(7435.922));
        final FieldAbsoluteDate<Binary64> date = new FieldAbsoluteDate<>(Binary64Field.getInstance(),
                                                                         new AbsoluteDate(2003, 9, 16, utc));
        orbit = new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(position,  velocity),
                                            FramesFactory.getEME2000(), date,
                                            new Binary64(Constants.EIGEN5C_EARTH_MU));

    }

    @AfterEach
    public void tearDown() {
        earth = null;
        gp    = null;
        orbit = null;
    }

}

