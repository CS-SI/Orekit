///* Copyright 2002-2016 CS Systèmes d'Information
// * Licensed to CS Systèmes d'Information (CS) under one or more
// * contributor license agreements.  See the NOTICE file distributed with
// * this work for additional information regarding copyright ownership.
// * CS licenses this file to You under the Apache License, Version 2.0
// * (the "License"); you may not use this file except in compliance with
// * the License.  You may obtain a copy of the License at
// *
// *   http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package org.orekit.propagation.events;
//
//import java.util.List;
//
//import org.hipparchus.Field;
//import org.hipparchus.RealFieldElement;
//import org.hipparchus.geometry.euclidean.threed.FieldVector3D<T>;
//import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
//import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
//import org.junit.After;
//import org.junit.Assert;
//import org.junit.Before;
//import org.junit.Test;
//import org.orekit.Utils;
//import org.orekit.bodies.CelestialBodyFactory;
//import org.orekit.errors.OrekitException;
//import org.orekit.frames.FramesFactory;
//import org.orekit.orbits.FieldEquinoctialOrbit;
//import org.orekit.orbits.FieldOrbit;
//import org.orekit.propagation.FieldSpacecraftState;
//import org.orekit.propagation.events.handlers.FieldEventHandler;
//import org.orekit.propagation.numerical.NumericalPropagator;
//import org.orekit.time.FieldAbsoluteDate;
//import org.orekit.time.TimeScalesFactory;
//import org.orekit.utils.FieldPVCoordinates;
//
//public class FieldEventsLoggerTest {
//
//    private double               mu;
////    private FieldAbsoluteDate<T>         iniDate;
////    private FieldSpacecraftState<T>      initialState;
//    private NumericalPropagator  propagator;
//    private int                  count;
////    private FieldEventDetector<T>        umbraDetector;
////    private FieldEventDetector<T>        penumbraDetector;
//
//
//    @Before
//    public void setUp() {
//            Utils.setDataRoot("regular-data");
//            mu  = 3.9860047e14;
//    }
//
//
//    @Test
//    public void test(){
//
//    }
//
//    public <T extends RealFieldElement<T>> void testLogUmbra(Field<T> field) throws OrekitException {
//
//        T zero = field.getZero();
//
//
//        final FieldVector3D<T> position  = new FieldVector3D<T>(zero.add(-6142438.668), zero.add(3492467.560), zero.add(-25767.25680));
//        final FieldVector3D<T> velocity  = new FieldVector3D<T>(zero.add(505.8479685) , zero.add(942.7809215), zero.add(7435.922231));
//        FieldAbsoluteDate<T> iniDate = new FieldAbsoluteDate<T>(field, 1969, 7, 28, 4, 0, 0.0, TimeScalesFactory.getTT());
//        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<T>(new FieldPVCoordinates<T>(position,  velocity),
//                                                 FramesFactory.getEME2000(), iniDate, mu);
//        FieldSpacecraftState<T> initialState = new FieldSpacecraftState<T>(orbit);
//        double[] absTolerance = {
//            0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6, 0.001
//        };
//        double[] relTolerance = {
//            1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7, 1.0e-7
//        };
//        AdaptiveStepsizeIntegrator integrator =
//            new DormandPrince853Integrator(0.001, 1000, absTolerance, relTolerance);
//        integrator.setInitialStepSize(60);
//        propagator = new NumericalPropagator(integrator);
//        propagator.setInitialState(initialState.toSpacecraftState());
//        count = 0;
//        FieldEventDetector<T> umbraDetector = buildDetector(true);
//        FieldEventDetector<T> penumbraDetector = buildDetector(false);
//
//
//
//        EventsLogger logger = new EventsLogger();
//        @SuppressWarnings("unchecked")
//        FieldEventDetector<T> monitored = ((AbstractDetector<FieldEventDetector<T>>) logger.monitorDetector(umbraDetector)).
//                withMaxIter(200);
//        Assert.assertEquals(100, umbraDetector.getMaxIterationCount());
//        Assert.assertEquals(200, monitored.getMaxIterationCount());
//        propagator.addFieldEventDetector<T>(monitored);
//        propagator.addFieldEventDetector<T>(penumbraDetector);
//        count = 0;
//        propagator.propagate(iniDate.shiftedBy(16215)).getDate();
//        Assert.assertEquals(11, count);
//        checkCounts(logger, 3, 3, 0, 0);
//    }
//
//    @Test
//    public void testLogPenumbra() throws OrekitException {
//        EventsLogger logger = new EventsLogger();
//        propagator.addFieldEventDetector<T>(umbraDetector);
//        propagator.addFieldEventDetector<T>(logger.monitorDetector(penumbraDetector));
//        count = 0;
//        propagator.propagate(iniDate.shiftedBy(16215)).getDate();
//        Assert.assertEquals(11, count);
//        checkCounts(logger, 0, 0, 2, 3);
//    }
//
//    @Test
//    public void testLogAll() throws OrekitException {
//        EventsLogger logger = new EventsLogger();
//        propagator.addFieldEventDetector<T>(logger.monitorDetector(umbraDetector));
//        propagator.addFieldEventDetector<T>(logger.monitorDetector(penumbraDetector));
//        count = 0;
//        propagator.propagate(iniDate.shiftedBy(16215));
//        Assert.assertEquals(11, count);
//        checkCounts(logger, 3, 3, 2, 3);
//    }
//
//    @Test
//    public void testImmutableList() throws OrekitException {
//        EventsLogger logger = new EventsLogger();
//        propagator.addFieldEventDetector<T>(logger.monitorDetector(umbraDetector));
//        propagator.addFieldEventDetector<T>(logger.monitorDetector(penumbraDetector));
//        count = 0;
//        propagator.propagate(iniDate.shiftedBy(16215));
//        List<EventsLogger.LoggedEvent> firstList = logger.getLoggedEvents();
//        Assert.assertEquals(11, firstList.size());
//        propagator.propagate(iniDate.shiftedBy(30000));
//        List<EventsLogger.LoggedEvent> secondList = logger.getLoggedEvents();
//        Assert.assertEquals(11, firstList.size());
//        Assert.assertEquals(20, secondList.size());
//        for (int i = 0; i < firstList.size(); ++i) {
//
//            EventsLogger.LoggedEvent e1 = firstList.get(i);
//            EventsLogger.LoggedEvent e2 = secondList.get(i);
//            FieldPVCoordinates<T> pv1 = e1.getState().getFieldPVCoordinates<T>();
//            FieldPVCoordinates<T> pv2 = e2.getState().getFieldPVCoordinates<T>();
//
//            Assert.assertTrue(e1.getFieldEventDetector<T>() == e2.getFieldEventDetector<T>());
//            Assert.assertEquals(0, pv1.getPosition().subtract(pv2.getPosition()).getNorm(), 1.0e-10);
//            Assert.assertEquals(0, pv1.getVelocity().subtract(pv2.getVelocity()).getNorm(), 1.0e-10);
//            Assert.assertEquals(e1.isIncreasing(), e2.isIncreasing());
//
//        }
//    }
//
//    @Test
//    public void testClearLog() throws OrekitException {
//        EventsLogger logger = new EventsLogger();
//        propagator.addFieldEventDetector<T>(logger.monitorDetector(umbraDetector));
//        propagator.addFieldEventDetector<T>(logger.monitorDetector(penumbraDetector));
//        count = 0;
//        propagator.propagate(iniDate.shiftedBy(16215));
//        List<EventsLogger.LoggedEvent> firstList = logger.getLoggedEvents();
//        Assert.assertEquals(11, firstList.size());
//        logger.clearLoggedEvents();
//        propagator.propagate(iniDate.shiftedBy(30000));
//        List<EventsLogger.LoggedEvent> secondList = logger.getLoggedEvents();
//        Assert.assertEquals(11, firstList.size());
//        Assert.assertEquals( 9, secondList.size());
//    }
//
//    private void checkCounts(EventsLogger logger,
//                             int expectedUmbraIncreasingCount, int expectedUmbraDecreasingCount,
//                             int expectedPenumbraIncreasingCount, int expectedPenumbraDecreasingCount) {
//        int umbraIncreasingCount = 0;
//        int umbraDecreasingCount = 0;
//        int penumbraIncreasingCount = 0;
//        int penumbraDecreasingCount = 0;
//        for (EventsLogger.LoggedEvent event : logger.getLoggedEvents()) {
//            if (event.getFieldEventDetector<T>() == umbraDetector) {
//                if (event.isIncreasing()) {
//                    ++umbraIncreasingCount;
//                } else {
//                    ++umbraDecreasingCount;
//                }
//            }
//            if (event.getFieldEventDetector<T>() == penumbraDetector) {
//                if (event.isIncreasing()) {
//                    ++penumbraIncreasingCount;
//                } else {
//                    ++penumbraDecreasingCount;
//                }
//            }
//        }
//        Assert.assertEquals(expectedUmbraIncreasingCount,    umbraIncreasingCount);
//        Assert.assertEquals(expectedUmbraDecreasingCount,    umbraDecreasingCount);
//        Assert.assertEquals(expectedPenumbraIncreasingCount, penumbraIncreasingCount);
//        Assert.assertEquals(expectedPenumbraDecreasingCount, penumbraDecreasingCount);
//    }
//
//    private <T extends RealFieldElement<T>> FieldEventDetector<T> buildDetector(final boolean totalEclipse) throws OrekitException {
//
//        FieldEclipseDetector<T> detector =
//                new FieldEclipseDetector<T>(60., 1.e-3, CelestialBodyFactory.getSun(), 696000000,
//                                   CelestialBodyFactory.getEarth(), 6400000);
//
//        if (totalEclipse) {
//            detector = detector.withUmbra();
//        } else {
//            detector = detector.withPenumbra();
//        }
//
//        detector = detector.withHandler(new FieldEventHandler<FieldEclipseDetector<T>, T>() {
//
//            public Action eventOccurred(FieldSpacecraftState<T> s, EclipseDetector detector, boolean increasing) {
//                ++count;
//                return Action.CONTINUE;
//            }
//
//        } );
//
//        return detector;
//
//    }
//
//    @After
//    public void tearDown() {
//        propagator = null;
//        count = 0;
//    }
//
//}
//
