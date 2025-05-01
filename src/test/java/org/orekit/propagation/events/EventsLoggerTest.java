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
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.CountAndContinue;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EventsLoggerTest {

    private double               mu;
    private AbsoluteDate         iniDate;
    private SpacecraftState      initialState;
    private NumericalPropagator  propagator;
    private CountAndContinue countAndContinue;
    private AbstractDetector<EclipseDetector>        umbraDetector;
    private AbstractDetector<EclipseDetector>         penumbraDetector;

    @Test
    void testMonitorDetectorClass() {
        // GIVEN
        final DateDetector dateDetector = new DateDetector();
        final EventsLogger eventsLogger = new EventsLogger();
        // WHEN
        final EventDetector detector = eventsLogger.monitorDetector(dateDetector);
        // THEN
        Assertions.assertInstanceOf(DetectorModifier.class, detector);
        final DetectorModifier modifier = (DetectorModifier) detector;
        Assertions.assertEquals(dateDetector, modifier.getDetector());
    }

    @Test
    void testMonitorDetectorHandlerEventOccurred() {
        // GIVEN
        final CountAndContinue counterHandler = new CountAndContinue();
        final DateDetector dateDetector = new DateDetector().withHandler(counterHandler);
        final EventsLogger eventsLogger = new EventsLogger();
        final EventDetector detector = eventsLogger.monitorDetector(dateDetector);
        final EventHandler handler = detector.getHandler();
        final SpacecraftState mockedState = mock();
        when(mockedState.getDate()).thenReturn(AbsoluteDate.ARBITRARY_EPOCH);
        // WHEN
        final Action action = handler.eventOccurred(mockedState, dateDetector, true);
        // THEN
        Assertions.assertEquals(Action.CONTINUE, action);
        final List<EventsLogger.LoggedEvent> loggedEvents = eventsLogger.getLoggedEvents();
        Assertions.assertEquals(loggedEvents.size(), counterHandler.getCount());
        final EventsLogger.LoggedEvent event = loggedEvents.get(0);
        Assertions.assertEquals(mockedState, event.getState());
        Assertions.assertEquals(mockedState, event.getResetState());
    }

    @ParameterizedTest
    @EnumSource(Action.class)
    void testMonitorDetectorHandlerResetState(final Action action) {
        // GIVEN
        final DateDetector dateDetector = new DateDetector().withHandler(new Handler(action));
        final EventsLogger eventsLogger = new EventsLogger();
        final EventDetector detector = eventsLogger.monitorDetector(dateDetector);
        final EventHandler handler = detector.getHandler();
        final SpacecraftState mockedState = mock();
        when(mockedState.getDate()).thenReturn(AbsoluteDate.ARBITRARY_EPOCH);
        // WHEN
        final SpacecraftState state = handler.resetState(dateDetector, mockedState);
        // THEN
        if (action == Action.RESET_STATE) {
            Assertions.assertNotEquals(mockedState, state);
        } else {
            Assertions.assertEquals(mockedState, state);
        }
    }

    private static class Handler implements EventHandler {

        private final Action action;

        Handler(final Action action) {
            this.action = action;
        }

        @Override
        public Action eventOccurred(SpacecraftState s, EventDetector detector, boolean increasing) {
            return action;
        }

        @Override
        public SpacecraftState resetState(EventDetector detector, SpacecraftState oldState) {
            if (action == Action.RESET_STATE) {
                return oldState.withMass(oldState.getMass());
            }
            return oldState;
        }
    }

    @Test
    void testLogUmbra() {
        EventsLogger logger = new EventsLogger();
        EventDetector monitored = logger.monitorDetector(umbraDetector.withMaxIter(200));
        Assertions.assertEquals(100, umbraDetector.getMaxIterationCount());
        Assertions.assertEquals(200, monitored.getMaxIterationCount());
        propagator.addEventDetector(monitored);
        propagator.addEventDetector(penumbraDetector);
        countAndContinue.reset();
        propagator.propagate(iniDate.shiftedBy(16215)).getDate();
        Assertions.assertEquals(11, countAndContinue.getCount());
        checkCounts(logger, 3, 3, 0, 0);
    }

    @Test
    void testLogPenumbra() {
        EventsLogger logger = new EventsLogger();
        propagator.addEventDetector(umbraDetector);
        propagator.addEventDetector(logger.monitorDetector(penumbraDetector));
        countAndContinue.reset();
        propagator.propagate(iniDate.shiftedBy(16215)).getDate();
        Assertions.assertEquals(11, countAndContinue.getCount());
        checkCounts(logger, 0, 0, 2, 3);
    }

    @Test
    void testLogAll() {
        EventsLogger logger = new EventsLogger();
        propagator.addEventDetector(logger.monitorDetector(umbraDetector));
        propagator.addEventDetector(logger.monitorDetector(penumbraDetector));
        countAndContinue.reset();
        propagator.propagate(iniDate.shiftedBy(16215));
        Assertions.assertEquals(11, countAndContinue.getCount());
        checkCounts(logger, 3, 3, 2, 3);
    }

    @Test
    void testImmutableList() {
        EventsLogger logger = new EventsLogger();
        propagator.addEventDetector(logger.monitorDetector(umbraDetector));
        propagator.addEventDetector(logger.monitorDetector(penumbraDetector));
        countAndContinue.reset();
        propagator.propagate(iniDate.shiftedBy(16215));
        List<EventsLogger.LoggedEvent> firstList = logger.getLoggedEvents();
        Assertions.assertEquals(11, firstList.size());
        propagator.propagate(iniDate.shiftedBy(30000));
        List<EventsLogger.LoggedEvent> secondList = logger.getLoggedEvents();
        Assertions.assertEquals(11, firstList.size());
        Assertions.assertEquals(20, secondList.size());
        for (int i = 0; i < firstList.size(); ++i) {

            EventsLogger.LoggedEvent e1 = firstList.get(i);
            EventsLogger.LoggedEvent e2 = secondList.get(i);
            PVCoordinates pv1 = e1.getState().getPVCoordinates();
            PVCoordinates pv2 = e2.getState().getPVCoordinates();

            Assertions.assertTrue(e1.getEventDetector() == e2.getEventDetector());
            Assertions.assertEquals(0, pv1.getPosition().subtract(pv2.getPosition()).getNorm(), 1.0e-10);
            Assertions.assertEquals(0, pv1.getVelocity().subtract(pv2.getVelocity()).getNorm(), 1.0e-10);
            Assertions.assertEquals(e1.isIncreasing(), e2.isIncreasing());

        }
    }

    @Test
    void testClearLog() {
        EventsLogger logger = new EventsLogger();
        propagator.addEventDetector(logger.monitorDetector(umbraDetector));
        propagator.addEventDetector(logger.monitorDetector(penumbraDetector));
        countAndContinue.reset();
        propagator.propagate(iniDate.shiftedBy(16215));
        List<EventsLogger.LoggedEvent> firstList = logger.getLoggedEvents();
        Assertions.assertEquals(11, firstList.size());
        logger.clearLoggedEvents();
        propagator.propagate(iniDate.shiftedBy(30000));
        List<EventsLogger.LoggedEvent> secondList = logger.getLoggedEvents();
        Assertions.assertEquals(11, firstList.size());
        Assertions.assertEquals( 9, secondList.size());
    }

    private void checkCounts(EventsLogger logger,
                             int expectedUmbraIncreasingCount, int expectedUmbraDecreasingCount,
                             int expectedPenumbraIncreasingCount, int expectedPenumbraDecreasingCount) {
        int umbraIncreasingCount = 0;
        int umbraDecreasingCount = 0;
        int penumbraIncreasingCount = 0;
        int penumbraDecreasingCount = 0;
        for (EventsLogger.LoggedEvent event : logger.getLoggedEvents()) {
            final EclipseDetector eclipseDetector = (EclipseDetector) (event.getEventDetector());
            if (eclipseDetector.getTotalEclipse()) {
                if (event.isIncreasing()) {
                    ++umbraIncreasingCount;
                } else {
                    ++umbraDecreasingCount;
                }
            }
            else {
                if (event.isIncreasing()) {
                    ++penumbraIncreasingCount;
                } else {
                    ++penumbraDecreasingCount;
                }
            }
        }
        Assertions.assertEquals(expectedUmbraIncreasingCount,    umbraIncreasingCount);
        Assertions.assertEquals(expectedUmbraDecreasingCount,    umbraDecreasingCount);
        Assertions.assertEquals(expectedPenumbraIncreasingCount, penumbraIncreasingCount);
        Assertions.assertEquals(expectedPenumbraDecreasingCount, penumbraDecreasingCount);
    }

    private EclipseDetector buildDetector(final boolean totalEclipse) {

        EclipseDetector detector =
                new EclipseDetector(CelestialBodyFactory.getSun(), 696000000,
                                    new OneAxisEllipsoid(6400000,
                                                         0.0,
                                                         FramesFactory.getITRF(IERSConventions.IERS_2010, true))).
                withMaxCheck(60.0).
                withThreshold(1.0e-3);

        if (totalEclipse) {
            detector = detector.withUmbra();
        } else {
            detector = detector.withPenumbra();
        }

        detector = detector.withHandler(countAndContinue);

        return detector;

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
                                                     FramesFactory.getEME2000(), iniDate, mu);
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
            countAndContinue = new CountAndContinue();
            umbraDetector = buildDetector(true);
            penumbraDetector = buildDetector(false);
        } catch (OrekitException oe) {
            Assertions.fail(oe.getLocalizedMessage());
        }
    }

    @AfterEach
    public void tearDown() {
        iniDate = null;
        initialState = null;
        propagator = null;
        countAndContinue = null;
        umbraDetector = null;
        penumbraDetector = null;
    }

}

