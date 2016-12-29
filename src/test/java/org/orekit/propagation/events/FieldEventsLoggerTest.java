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

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeFieldIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853FieldIntegrator;
import org.hipparchus.util.Decimal64Field;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.FieldEquinoctialOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.propagation.numerical.FieldNumericalPropagator;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.FieldPVCoordinates;

public class FieldEventsLoggerTest {

    private double               mu;
//    private FieldAbsoluteDate<T>         iniDate;
//    private FieldSpacecraftState<T>      initialState;
    private int                  count;
//    private FieldEventDetector<T>        umbraDetector;
//    private FieldEventDetector<T>        penumbraDetector;


    @Before
    public void setUp() {
            Utils.setDataRoot("regular-data");
            mu  = 3.9860047e14;
    }


    @Test
    public void testLogUmbra() throws OrekitException{
        doTestLogUmbra(Decimal64Field.getInstance());
    }
    @Test
    public void testLogPenumbra() throws OrekitException{
        doTestLogPenumbra(Decimal64Field.getInstance());
    }
    @Test
    public void testLogAll() throws OrekitException{
        doTestLogAll(Decimal64Field.getInstance());
    }
    @Test
    public void testImmutableList() throws OrekitException{
        doTestImmutableList(Decimal64Field.getInstance());
    }
    @Test
    public void testClearLog() throws OrekitException{
        doTestClearLog(Decimal64Field.getInstance());
    }

    private <T extends RealFieldElement<T>> void doTestLogUmbra(Field<T> field) throws OrekitException {

        T zero = field.getZero();


        final FieldVector3D<T> position  = new FieldVector3D<T>(zero.add(-6142438.668), zero.add(3492467.560), zero.add(-25767.25680));
        final FieldVector3D<T> velocity  = new FieldVector3D<T>(zero.add(505.8479685) , zero.add(942.7809215), zero.add(7435.922231));
        FieldAbsoluteDate<T> iniDate = new FieldAbsoluteDate<T>(field, 1969, 7, 28, 4, 0, 0.0, TimeScalesFactory.getTT());
        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<T>(new FieldPVCoordinates<T>(position,  velocity),
                                                 FramesFactory.getEME2000(), iniDate, mu);
        FieldSpacecraftState<T> initialState = new FieldSpacecraftState<T>(orbit);
        double[] absTolerance = {
            0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6, 0.001
        };
        double[] relTolerance = {
            1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7, 1.0e-7
        };
        AdaptiveStepsizeFieldIntegrator<T> integrator =
            new DormandPrince853FieldIntegrator<T>(field, 0.001, 1000, absTolerance, relTolerance);
        integrator.setInitialStepSize(field.getZero().add(60));
        FieldNumericalPropagator<T> propagator = new FieldNumericalPropagator<T>(field, integrator);
        propagator.setInitialState(initialState);
        count = 0;
        FieldEventDetector<T> umbraDetector = buildDetector(field, true);
        FieldEventDetector<T> penumbraDetector = buildDetector(field, false);



        FieldEventsLogger<T> logger = new FieldEventsLogger<T>();
        @SuppressWarnings("unchecked")
        FieldEventDetector<T> monitored = ((FieldAbstractDetector<FieldEventDetector<T>, T>) logger.monitorDetector(umbraDetector)).
                withMaxIter(200);
        Assert.assertEquals(100, umbraDetector.getMaxIterationCount());
        Assert.assertEquals(200, monitored.getMaxIterationCount());
        
        propagator.addEventDetector(monitored);
        propagator.addEventDetector(penumbraDetector);
        count = 0;
        propagator.propagate(iniDate.shiftedBy(16215)).getDate();
        Assert.assertEquals(11, count);
        checkCounts(logger, 3, 3, 0, 0, umbraDetector, penumbraDetector);
    }

    private <T extends RealFieldElement<T>> void doTestLogPenumbra(final Field<T> field) throws OrekitException {
        

        T zero = field.getZero();


        final FieldVector3D<T> position  = new FieldVector3D<T>(zero.add(-6142438.668), zero.add(3492467.560), zero.add(-25767.25680));
        final FieldVector3D<T> velocity  = new FieldVector3D<T>(zero.add(505.8479685) , zero.add(942.7809215), zero.add(7435.922231));
        FieldAbsoluteDate<T> iniDate = new FieldAbsoluteDate<T>(field, 1969, 7, 28, 4, 0, 0.0, TimeScalesFactory.getTT());
        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<T>(new FieldPVCoordinates<T>(position,  velocity),
                                                 FramesFactory.getEME2000(), iniDate, mu);
        FieldSpacecraftState<T> initialState = new FieldSpacecraftState<T>(orbit);
        double[] absTolerance = {
            0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6, 0.001
        };
        double[] relTolerance = {
            1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7, 1.0e-7
        };
        AdaptiveStepsizeFieldIntegrator<T> integrator =
            new DormandPrince853FieldIntegrator<T>(field, 0.001, 1000, absTolerance, relTolerance);
        integrator.setInitialStepSize(field.getZero().add(60));
        FieldNumericalPropagator<T> propagator = new FieldNumericalPropagator<T>(field, integrator);
        propagator.setInitialState(initialState);
        count = 0;
        FieldEventDetector<T> umbraDetector = buildDetector(field, true);
        FieldEventDetector<T> penumbraDetector = buildDetector(field, false);

        FieldEventsLogger<T> logger = new FieldEventsLogger<T>();
        propagator.addEventDetector(umbraDetector);
        propagator.addEventDetector(logger.monitorDetector(penumbraDetector));
        count = 0;
        propagator.propagate(iniDate.shiftedBy(16215)).getDate();
        Assert.assertEquals(11, count);
        checkCounts(logger, 0, 0, 2, 3, umbraDetector, penumbraDetector);
    }

    private <T extends RealFieldElement<T>> void doTestLogAll(final Field<T> field) throws OrekitException {

        T zero = field.getZero();


        final FieldVector3D<T> position  = new FieldVector3D<T>(zero.add(-6142438.668), zero.add(3492467.560), zero.add(-25767.25680));
        final FieldVector3D<T> velocity  = new FieldVector3D<T>(zero.add(505.8479685) , zero.add(942.7809215), zero.add(7435.922231));
        FieldAbsoluteDate<T> iniDate = new FieldAbsoluteDate<T>(field, 1969, 7, 28, 4, 0, 0.0, TimeScalesFactory.getTT());
        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<T>(new FieldPVCoordinates<T>(position,  velocity),
                                                 FramesFactory.getEME2000(), iniDate, mu);
        FieldSpacecraftState<T> initialState = new FieldSpacecraftState<T>(orbit);
        double[] absTolerance = {
            0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6, 0.001
        };
        double[] relTolerance = {
            1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7, 1.0e-7
        };
        AdaptiveStepsizeFieldIntegrator<T> integrator =
            new DormandPrince853FieldIntegrator<T>(field, 0.001, 1000, absTolerance, relTolerance);
        integrator.setInitialStepSize(field.getZero().add(60));
        FieldNumericalPropagator<T> propagator = new FieldNumericalPropagator<T>(field, integrator);
        propagator.setInitialState(initialState);
        count = 0;
        FieldEventDetector<T> umbraDetector = buildDetector(field, true);
        FieldEventDetector<T> penumbraDetector = buildDetector(field, false);


        
        
        
        
        
        
        FieldEventsLogger<T> logger = new FieldEventsLogger<T>();
        propagator.addEventDetector(logger.monitorDetector(umbraDetector));
        propagator.addEventDetector(logger.monitorDetector(penumbraDetector));
        count = 0;
        propagator.propagate(iniDate.shiftedBy(16215));
        Assert.assertEquals(11, count);
        checkCounts(logger, 3, 3, 2, 3, umbraDetector, penumbraDetector);
    }

    private <T extends RealFieldElement<T>> void doTestImmutableList(final Field<T> field) throws OrekitException {
        

        T zero = field.getZero();


        final FieldVector3D<T> position  = new FieldVector3D<T>(zero.add(-6142438.668), zero.add(3492467.560), zero.add(-25767.25680));
        final FieldVector3D<T> velocity  = new FieldVector3D<T>(zero.add(505.8479685) , zero.add(942.7809215), zero.add(7435.922231));
        FieldAbsoluteDate<T> iniDate = new FieldAbsoluteDate<T>(field, 1969, 7, 28, 4, 0, 0.0, TimeScalesFactory.getTT());
        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<T>(new FieldPVCoordinates<T>(position,  velocity),
                                                 FramesFactory.getEME2000(), iniDate, mu);
        FieldSpacecraftState<T> initialState = new FieldSpacecraftState<T>(orbit);
        double[] absTolerance = {
            0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6, 0.001
        };
        double[] relTolerance = {
            1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7, 1.0e-7
        };
        AdaptiveStepsizeFieldIntegrator<T> integrator =
            new DormandPrince853FieldIntegrator<T>(field, 0.001, 1000, absTolerance, relTolerance);
        integrator.setInitialStepSize(field.getZero().add(60));
        FieldNumericalPropagator<T> propagator = new FieldNumericalPropagator<T>(field, integrator);
        propagator.setInitialState(initialState);
        count = 0;
        FieldEventDetector<T> umbraDetector = buildDetector(field, true);
        FieldEventDetector<T> penumbraDetector = buildDetector(field, false);
        
        FieldEventsLogger<T> logger = new FieldEventsLogger<T>();
        propagator.addEventDetector(logger.monitorDetector(umbraDetector));
        propagator.addEventDetector(logger.monitorDetector(penumbraDetector));
        count = 0;
        propagator.propagate(iniDate.shiftedBy(16215));
        List<FieldEventsLogger.FieldLoggedEvent<T>> firstList = logger.getLoggedEvents();
        Assert.assertEquals(11, firstList.size());
        propagator.propagate(iniDate.shiftedBy(30000));
        List<FieldEventsLogger.FieldLoggedEvent<T>> secondList = logger.getLoggedEvents();
        Assert.assertEquals(11, firstList.size());
        Assert.assertEquals(20, secondList.size());
        for (int i = 0; i < firstList.size(); ++i) {

            FieldEventsLogger.FieldLoggedEvent<T> e1 = firstList.get(i);
            FieldEventsLogger.FieldLoggedEvent<T> e2 = secondList.get(i);
            FieldPVCoordinates<T> pv1 = e1.getState().getPVCoordinates();
            FieldPVCoordinates<T> pv2 = e2.getState().getPVCoordinates();

            Assert.assertTrue(e1.getEventDetector() == e2.getEventDetector());
            Assert.assertEquals(0, pv1.getPosition().subtract(pv2.getPosition()).getNorm().getReal(), 1.0e-10);
            Assert.assertEquals(0, pv1.getVelocity().subtract(pv2.getVelocity()).getNorm().getReal(), 1.0e-10);
            Assert.assertEquals(e1.isIncreasing(), e2.isIncreasing());

        }
    }

    private <T extends RealFieldElement<T>> void doTestClearLog(final Field<T> field) throws OrekitException {
        
        
        

        T zero = field.getZero();


        final FieldVector3D<T> position  = new FieldVector3D<T>(zero.add(-6142438.668), zero.add(3492467.560), zero.add(-25767.25680));
        final FieldVector3D<T> velocity  = new FieldVector3D<T>(zero.add(505.8479685) , zero.add(942.7809215), zero.add(7435.922231));
        FieldAbsoluteDate<T> iniDate = new FieldAbsoluteDate<T>(field, 1969, 7, 28, 4, 0, 0.0, TimeScalesFactory.getTT());
        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<T>(new FieldPVCoordinates<T>(position,  velocity),
                                                 FramesFactory.getEME2000(), iniDate, mu);
        FieldSpacecraftState<T> initialState = new FieldSpacecraftState<T>(orbit);
        double[] absTolerance = {
            0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6, 0.001
        };
        double[] relTolerance = {
            1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7, 1.0e-7
        };
        AdaptiveStepsizeFieldIntegrator<T> integrator =
            new DormandPrince853FieldIntegrator<T>(field, 0.001, 1000, absTolerance, relTolerance);
        integrator.setInitialStepSize(field.getZero().add(60));
        FieldNumericalPropagator<T> propagator = new FieldNumericalPropagator<T>(field, integrator);
        propagator.setInitialState(initialState);
        count = 0;
        FieldEventDetector<T> umbraDetector = buildDetector(field, true);
        FieldEventDetector<T> penumbraDetector = buildDetector(field, false);


        
        FieldEventsLogger<T> logger = new FieldEventsLogger<T>();
        propagator.addEventDetector(logger.monitorDetector(umbraDetector));
        propagator.addEventDetector(logger.monitorDetector(penumbraDetector));
        count = 0;
        propagator.propagate(iniDate.shiftedBy(16215));
        List<FieldEventsLogger.FieldLoggedEvent<T>> firstList = logger.getLoggedEvents();
        Assert.assertEquals(11, firstList.size());
        logger.clearLoggedEvents();
        propagator.propagate(iniDate.shiftedBy(30000));
        List<FieldEventsLogger.FieldLoggedEvent<T>> secondList = logger.getLoggedEvents();
        Assert.assertEquals(11, firstList.size());
        Assert.assertEquals( 9, secondList.size());
    }

    private <T extends RealFieldElement<T>> void checkCounts(FieldEventsLogger<T> logger,
                             int expectedUmbraIncreasingCount, int expectedUmbraDecreasingCount,
                             int expectedPenumbraIncreasingCount, int expectedPenumbraDecreasingCount, 
                             FieldEventDetector<T> umbraDetector, FieldEventDetector<T> penumbraDetector) {
        int umbraIncreasingCount = 0;
        int umbraDecreasingCount = 0;
        int penumbraIncreasingCount = 0;
        int penumbraDecreasingCount = 0;
        for (FieldEventsLogger.FieldLoggedEvent<T> event : logger.getLoggedEvents()) {
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

    private <T extends RealFieldElement<T>> FieldEventDetector<T> buildDetector(Field<T> field, final boolean totalEclipse) throws OrekitException {

        FieldEclipseDetector<T> detector =
                new FieldEclipseDetector<T>(field.getZero().add(60.), field.getZero().add(1.e-3), CelestialBodyFactory.getSun(), 696000000,
                                   CelestialBodyFactory.getEarth(), 6400000);

        if (totalEclipse) {
            detector = detector.withUmbra();
        } else {
            detector = detector.withPenumbra();
        }

        detector = detector.withHandler(new FieldEventHandler<FieldEclipseDetector<T>, T>() {

            public Action eventOccurred(FieldSpacecraftState<T> s, FieldEclipseDetector<T> detector, boolean increasing) {
                ++count;
                return Action.CONTINUE;
            }

        } );

        return detector;

    }

    @After
    public void tearDown() {
        count = 0;
    }

}

