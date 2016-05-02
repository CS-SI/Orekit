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

import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.PVCoordinates;

public class EventsLoggerTest {

    private double               mu;
    private AbsoluteDate         iniDate;
    private SpacecraftState      initialState;
    private NumericalPropagator  propagator;
    private int                  count;
    private EventDetector        umbraDetector;
    private EventDetector        penumbraDetector;

    @Test
    public void testLogUmbra() throws OrekitException {
        EventsLogger logger = new EventsLogger();
        @SuppressWarnings("unchecked")
        EventDetector monitored = ((AbstractDetector<EventDetector>) logger.monitorDetector(umbraDetector)).
                withMaxIter(200);
        Assert.assertEquals(100, umbraDetector.getMaxIterationCount());
        Assert.assertEquals(200, monitored.getMaxIterationCount());
        propagator.addEventDetector(monitored);
        propagator.addEventDetector(penumbraDetector);
        count = 0;
        propagator.propagate(iniDate.shiftedBy(16215)).getDate();
        Assert.assertEquals(11, count);
        checkCounts(logger, 3, 3, 0, 0);
    }

    @Test
    public void testLogPenumbra() throws OrekitException {
        EventsLogger logger = new EventsLogger();
        propagator.addEventDetector(umbraDetector);
        propagator.addEventDetector(logger.monitorDetector(penumbraDetector));
        count = 0;
        propagator.propagate(iniDate.shiftedBy(16215)).getDate();
        Assert.assertEquals(11, count);
        checkCounts(logger, 0, 0, 2, 3);
    }

    @Test
    public void testLogAll() throws OrekitException {
        EventsLogger logger = new EventsLogger();
        propagator.addEventDetector(logger.monitorDetector(umbraDetector));
        propagator.addEventDetector(logger.monitorDetector(penumbraDetector));
        count = 0;
        propagator.propagate(iniDate.shiftedBy(16215));
        Assert.assertEquals(11, count);
        checkCounts(logger, 3, 3, 2, 3);
    }

    @Test
    public void testImmutableList() throws OrekitException {
        EventsLogger logger = new EventsLogger();
        propagator.addEventDetector(logger.monitorDetector(umbraDetector));
        propagator.addEventDetector(logger.monitorDetector(penumbraDetector));
        count = 0;
        propagator.propagate(iniDate.shiftedBy(16215));
        List<EventsLogger.LoggedEvent> firstList = logger.getLoggedEvents();
        Assert.assertEquals(11, firstList.size());
        propagator.propagate(iniDate.shiftedBy(30000));
        List<EventsLogger.LoggedEvent> secondList = logger.getLoggedEvents();
        Assert.assertEquals(11, firstList.size());
        Assert.assertEquals(20, secondList.size());
        for (int i = 0; i < firstList.size(); ++i) {

            EventsLogger.LoggedEvent e1 = firstList.get(i);
            EventsLogger.LoggedEvent e2 = secondList.get(i);
            PVCoordinates pv1 = e1.getState().getPVCoordinates();
            PVCoordinates pv2 = e2.getState().getPVCoordinates();

            Assert.assertTrue(e1.getEventDetector() == e2.getEventDetector());
            Assert.assertEquals(0, pv1.getPosition().subtract(pv2.getPosition()).getNorm(), 1.0e-10);
            Assert.assertEquals(0, pv1.getVelocity().subtract(pv2.getVelocity()).getNorm(), 1.0e-10);
            Assert.assertEquals(e1.isIncreasing(), e2.isIncreasing());

        }
    }

    @Test
    public void testClearLog() throws OrekitException {
        EventsLogger logger = new EventsLogger();
        propagator.addEventDetector(logger.monitorDetector(umbraDetector));
        propagator.addEventDetector(logger.monitorDetector(penumbraDetector));
        count = 0;
        propagator.propagate(iniDate.shiftedBy(16215));
        List<EventsLogger.LoggedEvent> firstList = logger.getLoggedEvents();
        Assert.assertEquals(11, firstList.size());
        logger.clearLoggedEvents();
        propagator.propagate(iniDate.shiftedBy(30000));
        List<EventsLogger.LoggedEvent> secondList = logger.getLoggedEvents();
        Assert.assertEquals(11, firstList.size());
        Assert.assertEquals( 9, secondList.size());
    }

    private void checkCounts(EventsLogger logger,
                             int expectedUmbraIncreasingCount, int expectedUmbraDecreasingCount,
                             int expectedPenumbraIncreasingCount, int expectedPenumbraDecreasingCount) {
        int umbraIncreasingCount = 0;
        int umbraDecreasingCount = 0;
        int penumbraIncreasingCount = 0;
        int penumbraDecreasingCount = 0;
        for (EventsLogger.LoggedEvent event : logger.getLoggedEvents()) {
            if (event.getEventDetector() == umbraDetector) {
                if (event.isIncreasing()) {
                    ++umbraIncreasingCount;
                } else {
                    ++umbraDecreasingCount;
                }
            }
            if (event.getEventDetector() == penumbraDetector) {
                if (event.isIncreasing()) {
                    ++penumbraIncreasingCount;
                } else {
                    ++penumbraDecreasingCount;
                }
            }
        }
        Assert.assertEquals(expectedUmbraIncreasingCount,    umbraIncreasingCount);
        Assert.assertEquals(expectedUmbraDecreasingCount,    umbraDecreasingCount);
        Assert.assertEquals(expectedPenumbraIncreasingCount, penumbraIncreasingCount);
        Assert.assertEquals(expectedPenumbraDecreasingCount, penumbraDecreasingCount);
    }

    private EventDetector buildDetector(final boolean totalEclipse) throws OrekitException {

        EclipseDetector detector =
                new EclipseDetector(60., 1.e-3, CelestialBodyFactory.getSun(), 696000000,
                                   CelestialBodyFactory.getEarth(), 6400000);

        if (totalEclipse) {
            detector = detector.withUmbra();
        } else {
            detector = detector.withPenumbra();
        }

        detector = detector.withHandler(new EventHandler<EclipseDetector>() {

            public Action eventOccurred(SpacecraftState s, EclipseDetector detector, boolean increasing) {
                ++count;
                return Action.CONTINUE;
            }

        });

        return detector;

    }

    @Before
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
            count = 0;
            umbraDetector = buildDetector(true);
            penumbraDetector = buildDetector(false);
        } catch (OrekitException oe) {
            Assert.fail(oe.getLocalizedMessage());
        }
    }

    @After
    public void tearDown() {
        iniDate = null;
        initialState = null;
        propagator = null;
        count = 0;
        umbraDetector = null;
        penumbraDetector = null;
    }

}

