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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.ode.events.Action;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeFieldIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853FieldIntegrator;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.TestUtils;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.orbits.FieldEquinoctialOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.events.handlers.FieldCountAndContinue;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.propagation.numerical.FieldNumericalPropagator;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.FieldPVCoordinates;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FieldEventsLoggerTest {

    private double               mu;
    private int                  count;


    @BeforeEach
    void setUp() {
            Utils.setDataRoot("regular-data");
            mu  = 3.9860047e14;
    }

    @Test
    void testMonitorDetectorClass() {
        // GIVEN
        final FieldAbsoluteDate<Binary64> fieldDate = FieldAbsoluteDate.getArbitraryEpoch(Binary64Field.getInstance());
        final FieldDateDetector<Binary64> dateDetector = new FieldDateDetector<>(fieldDate);
        final FieldEventsLogger<Binary64> eventsLogger = new FieldEventsLogger<>();
        // WHEN
        final FieldEventDetector<Binary64> detector = eventsLogger.monitorDetector(dateDetector);
        // THEN
        Assertions.assertInstanceOf(FieldDetectorModifier.class, detector);
        final FieldDetectorModifier<Binary64> modifier = (FieldDetectorModifier<Binary64>) detector;
        Assertions.assertEquals(dateDetector, modifier.getDetector());
    }

    @Test
    void testMonitorDetectorHandlerEventOccurred() {
        // GIVEN
        final FieldAbsoluteDate<Binary64> fieldDate = FieldAbsoluteDate.getArbitraryEpoch(Binary64Field.getInstance());
        final FieldCountAndContinue<Binary64> counterHandler = new FieldCountAndContinue<>(0);
        final FieldDateDetector<Binary64> dateDetector = new FieldDateDetector<>(fieldDate).withHandler(counterHandler);
        final FieldEventsLogger<Binary64> eventsLogger = new FieldEventsLogger<>();
        final FieldEventDetector<Binary64> detector = eventsLogger.monitorDetector(dateDetector);
        final FieldEventHandler<Binary64> handler = detector.getHandler();
        @SuppressWarnings("unchecked")
        final FieldSpacecraftState<Binary64> mockedState = mock();
        when(mockedState.getDate()).thenReturn(fieldDate);
        // WHEN
        final Action action = handler.eventOccurred(mockedState, dateDetector, true);
        // THEN
        Assertions.assertEquals(Action.CONTINUE, action);
        final List<FieldEventsLogger.FieldLoggedEvent<Binary64>> loggedEvents = eventsLogger.getLoggedEvents();
        Assertions.assertEquals(loggedEvents.size(), counterHandler.getCount());
        final FieldEventsLogger.FieldLoggedEvent<Binary64> event = loggedEvents.get(0);
        Assertions.assertEquals(mockedState, event.getState());
        Assertions.assertEquals(mockedState, event.getResetState());
    }

    @Test
    void testMonitorDetectorHandlerResetState() {
        // GIVEN
        final FieldAbsoluteDate<Binary64> fieldDate = FieldAbsoluteDate.getArbitraryEpoch(Binary64Field.getInstance());
        final FieldCountAndContinue<Binary64> counterHandler = new FieldCountAndContinue<>(0);
        final FieldDateDetector<Binary64> dateDetector = new FieldDateDetector<>(fieldDate).withHandler(counterHandler);
        final FieldEventsLogger<Binary64> eventsLogger = new FieldEventsLogger<>();
        final FieldEventDetector<Binary64> detector = eventsLogger.monitorDetector(dateDetector);
        final FieldEventHandler<Binary64> handler = detector.getHandler();
        final FieldOrbit<Binary64> fieldOrbit = new FieldCartesianOrbit<>(Binary64Field.getInstance(),
                TestUtils.getDefaultOrbit(fieldDate.toAbsoluteDate()));
        final FieldSpacecraftState<Binary64> fieldState = new FieldSpacecraftState<>(fieldOrbit);
        // WHEN
        final Action action = handler.eventOccurred(fieldState, dateDetector, true);
        // THEN
        final FieldSpacecraftState<Binary64> state = handler.resetState(dateDetector, fieldState);
        // THEN
        if (action == Action.RESET_STATE) {
            Assertions.assertNotEquals(fieldState, state);
        } else {
            Assertions.assertEquals(fieldState, state);
        }
    }

    @Test
    void testLogUmbra() {
        doTestLogUmbra(Binary64Field.getInstance());
    }

    @Test
    void testLogPenumbra() {
        doTestLogPenumbra(Binary64Field.getInstance());
    }

    @Test
    void testLogAll() {
        doTestLogAll(Binary64Field.getInstance());
    }

    @Test
    void testImmutableList() {
        doTestImmutableList(Binary64Field.getInstance());
    }

    @Test
    void testClearLog() {
        doTestClearLog(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestLogUmbra(Field<T> field) {

        T zero = field.getZero();


        final FieldVector3D<T> position  = new FieldVector3D<>(zero.add(-6142438.668), zero.add(3492467.560), zero.add(-25767.25680));
        final FieldVector3D<T> velocity  = new FieldVector3D<>(zero.add(505.8479685) , zero.add(942.7809215), zero.add(7435.922231));
        FieldAbsoluteDate<T> iniDate = new FieldAbsoluteDate<>(field, 1969, 7, 28, 4, 0, 0.0, TimeScalesFactory.getTT());
        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(position,  velocity),
                                                                FramesFactory.getEME2000(), iniDate, zero.add(mu));
        FieldSpacecraftState<T> initialState = new FieldSpacecraftState<>(orbit);
        double[] absTolerance = {
            0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6, 0.001
        };
        double[] relTolerance = {
            1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7, 1.0e-7
        };
        AdaptiveStepsizeFieldIntegrator<T> integrator =
            new DormandPrince853FieldIntegrator<>(field, 0.001, 1000, absTolerance, relTolerance);
        integrator.setInitialStepSize(60);
        FieldNumericalPropagator<T> propagator = new FieldNumericalPropagator<>(field, integrator);
        propagator.setOrbitType(OrbitType.EQUINOCTIAL);
        propagator.setInitialState(initialState);
        count = 0;
        FieldEclipseDetector<T> umbraDetector = buildDetector(field, true);
        FieldEclipseDetector<T> penumbraDetector = buildDetector(field, false);



        FieldEventsLogger<T> logger = new FieldEventsLogger<>();
        FieldEventDetector<T> monitored = logger.monitorDetector(umbraDetector.withMaxIter(200));
        Assertions.assertEquals(100, umbraDetector.getMaxIterationCount());
        Assertions.assertEquals(200, monitored.getMaxIterationCount());

        propagator.addEventDetector(monitored);
        propagator.addEventDetector(penumbraDetector);
        count = 0;
        propagator.propagate(iniDate.shiftedBy(16215)).getDate();
        Assertions.assertEquals(11, count);
        checkCounts(logger, 3, 3, 0, 0);
    }

    private <T extends CalculusFieldElement<T>> void doTestLogPenumbra(final Field<T> field) {


        T zero = field.getZero();


        final FieldVector3D<T> position  = new FieldVector3D<>(zero.add(-6142438.668), zero.add(3492467.560), zero.add(-25767.25680));
        final FieldVector3D<T> velocity  = new FieldVector3D<>(zero.add(505.8479685) , zero.add(942.7809215), zero.add(7435.922231));
        FieldAbsoluteDate<T> iniDate = new FieldAbsoluteDate<>(field, 1969, 7, 28, 4, 0, 0.0, TimeScalesFactory.getTT());
        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(position,  velocity),
                                                                FramesFactory.getEME2000(), iniDate, zero.add(mu));
        FieldSpacecraftState<T> initialState = new FieldSpacecraftState<>(orbit);
        double[] absTolerance = {
            0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6, 0.001
        };
        double[] relTolerance = {
            1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7, 1.0e-7
        };
        AdaptiveStepsizeFieldIntegrator<T> integrator =
            new DormandPrince853FieldIntegrator<>(field, 0.001, 1000, absTolerance, relTolerance);
        integrator.setInitialStepSize(60);
        FieldNumericalPropagator<T> propagator = new FieldNumericalPropagator<>(field, integrator);
        propagator.setOrbitType(OrbitType.EQUINOCTIAL);
        propagator.setInitialState(initialState);
        count = 0;
        FieldEventDetector<T> umbraDetector = buildDetector(field, true);
        FieldEventDetector<T> penumbraDetector = buildDetector(field, false);

        FieldEventsLogger<T> logger = new FieldEventsLogger<>();
        propagator.addEventDetector(umbraDetector);
        propagator.addEventDetector(logger.monitorDetector(penumbraDetector));
        count = 0;
        propagator.propagate(iniDate.shiftedBy(16215)).getDate();
        Assertions.assertEquals(11, count);
        checkCounts(logger, 0, 0, 2, 3);
    }

    private <T extends CalculusFieldElement<T>> void doTestLogAll(final Field<T> field) {

        T zero = field.getZero();


        final FieldVector3D<T> position  = new FieldVector3D<>(zero.add(-6142438.668), zero.add(3492467.560), zero.add(-25767.25680));
        final FieldVector3D<T> velocity  = new FieldVector3D<>(zero.add(505.8479685) , zero.add(942.7809215), zero.add(7435.922231));
        FieldAbsoluteDate<T> iniDate = new FieldAbsoluteDate<>(field, 1969, 7, 28, 4, 0, 0.0, TimeScalesFactory.getTT());
        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(position,  velocity),
                                                                FramesFactory.getEME2000(), iniDate, zero.add(mu));
        FieldSpacecraftState<T> initialState = new FieldSpacecraftState<>(orbit);
        double[] absTolerance = {
            0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6, 0.001
        };
        double[] relTolerance = {
            1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7, 1.0e-7
        };
        AdaptiveStepsizeFieldIntegrator<T> integrator =
            new DormandPrince853FieldIntegrator<>(field, 0.001, 1000, absTolerance, relTolerance);
        integrator.setInitialStepSize(60);
        FieldNumericalPropagator<T> propagator = new FieldNumericalPropagator<>(field, integrator);
        propagator.setOrbitType(OrbitType.EQUINOCTIAL);
        propagator.setInitialState(initialState);
        count = 0;
        FieldEventDetector<T> umbraDetector = buildDetector(field, true);
        FieldEventDetector<T> penumbraDetector = buildDetector(field, false);








        FieldEventsLogger<T> logger = new FieldEventsLogger<>();
        propagator.addEventDetector(logger.monitorDetector(umbraDetector));
        propagator.addEventDetector(logger.monitorDetector(penumbraDetector));
        count = 0;
        propagator.propagate(iniDate.shiftedBy(16215));
        Assertions.assertEquals(11, count);
        checkCounts(logger, 3, 3, 2, 3);
    }

    private <T extends CalculusFieldElement<T>> void doTestImmutableList(final Field<T> field) {


        T zero = field.getZero();


        final FieldVector3D<T> position  = new FieldVector3D<>(zero.add(-6142438.668), zero.add(3492467.560), zero.add(-25767.25680));
        final FieldVector3D<T> velocity  = new FieldVector3D<>(zero.add(505.8479685) , zero.add(942.7809215), zero.add(7435.922231));
        FieldAbsoluteDate<T> iniDate = new FieldAbsoluteDate<>(field, 1969, 7, 28, 4, 0, 0.0, TimeScalesFactory.getTT());
        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(position,  velocity),
                                                                FramesFactory.getEME2000(), iniDate, zero.add(mu));
        FieldSpacecraftState<T> initialState = new FieldSpacecraftState<>(orbit);
        double[] absTolerance = {
            0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6, 0.001
        };
        double[] relTolerance = {
            1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7, 1.0e-7
        };
        AdaptiveStepsizeFieldIntegrator<T> integrator =
            new DormandPrince853FieldIntegrator<>(field, 0.001, 1000, absTolerance, relTolerance);
        integrator.setInitialStepSize(60);
        FieldNumericalPropagator<T> propagator = new FieldNumericalPropagator<>(field, integrator);
        propagator.setOrbitType(OrbitType.EQUINOCTIAL);
        propagator.setInitialState(initialState);
        count = 0;
        FieldEventDetector<T> umbraDetector = buildDetector(field, true);
        FieldEventDetector<T> penumbraDetector = buildDetector(field, false);

        FieldEventsLogger<T> logger = new FieldEventsLogger<>();
        propagator.addEventDetector(logger.monitorDetector(umbraDetector));
        propagator.addEventDetector(logger.monitorDetector(penumbraDetector));
        count = 0;
        propagator.propagate(iniDate.shiftedBy(16215));
        List<FieldEventsLogger.FieldLoggedEvent<T>> firstList = logger.getLoggedEvents();
        Assertions.assertEquals(11, firstList.size());
        propagator.propagate(iniDate.shiftedBy(30000));
        List<FieldEventsLogger.FieldLoggedEvent<T>> secondList = logger.getLoggedEvents();
        Assertions.assertEquals(11, firstList.size());
        Assertions.assertEquals(20, secondList.size());
        for (int i = 0; i < firstList.size(); ++i) {

            FieldEventsLogger.FieldLoggedEvent<T> e1 = firstList.get(i);
            FieldEventsLogger.FieldLoggedEvent<T> e2 = secondList.get(i);
            FieldPVCoordinates<T> pv1 = e1.getState().getPVCoordinates();
            FieldPVCoordinates<T> pv2 = e2.getState().getPVCoordinates();

            Assertions.assertSame(e1.getEventDetector(), e2.getEventDetector());
            Assertions.assertEquals(0, pv1.getPosition().subtract(pv2.getPosition()).getNorm().getReal(), 1.0e-10);
            Assertions.assertEquals(0, pv1.getVelocity().subtract(pv2.getVelocity()).getNorm().getReal(), 1.0e-10);
            Assertions.assertEquals(e1.isIncreasing(), e2.isIncreasing());

        }
    }

    private <T extends CalculusFieldElement<T>> void doTestClearLog(final Field<T> field) {




        T zero = field.getZero();


        final FieldVector3D<T> position  = new FieldVector3D<>(zero.add(-6142438.668), zero.add(3492467.560), zero.add(-25767.25680));
        final FieldVector3D<T> velocity  = new FieldVector3D<>(zero.add(505.8479685) , zero.add(942.7809215), zero.add(7435.922231));
        FieldAbsoluteDate<T> iniDate = new FieldAbsoluteDate<>(field, 1969, 7, 28, 4, 0, 0.0, TimeScalesFactory.getTT());
        final FieldOrbit<T> orbit = new FieldEquinoctialOrbit<>(new FieldPVCoordinates<>(position,  velocity),
                                                                FramesFactory.getEME2000(), iniDate, zero.add(mu));
        FieldSpacecraftState<T> initialState = new FieldSpacecraftState<>(orbit);
        double[] absTolerance = {
            0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6, 0.001
        };
        double[] relTolerance = {
            1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7, 1.0e-7
        };
        AdaptiveStepsizeFieldIntegrator<T> integrator =
            new DormandPrince853FieldIntegrator<>(field, 0.001, 1000, absTolerance, relTolerance);
        integrator.setInitialStepSize(60);
        FieldNumericalPropagator<T> propagator = new FieldNumericalPropagator<>(field, integrator);
        propagator.setOrbitType(OrbitType.EQUINOCTIAL);
        propagator.setInitialState(initialState);
        count = 0;
        FieldEventDetector<T> umbraDetector = buildDetector(field, true);
        FieldEventDetector<T> penumbraDetector = buildDetector(field, false);



        FieldEventsLogger<T> logger = new FieldEventsLogger<>();
        propagator.addEventDetector(logger.monitorDetector(umbraDetector));
        propagator.addEventDetector(logger.monitorDetector(penumbraDetector));
        count = 0;
        propagator.propagate(iniDate.shiftedBy(16215));
        List<FieldEventsLogger.FieldLoggedEvent<T>> firstList = logger.getLoggedEvents();
        Assertions.assertEquals(11, firstList.size());
        logger.clearLoggedEvents();
        propagator.propagate(iniDate.shiftedBy(30000));
        List<FieldEventsLogger.FieldLoggedEvent<T>> secondList = logger.getLoggedEvents();
        Assertions.assertEquals(11, firstList.size());
        Assertions.assertEquals( 9, secondList.size());
    }

    private <T extends CalculusFieldElement<T>> void checkCounts(FieldEventsLogger<T> logger,
                             int expectedUmbraIncreasingCount, int expectedUmbraDecreasingCount,
                             int expectedPenumbraIncreasingCount, int expectedPenumbraDecreasingCount) {
        int umbraIncreasingCount = 0;
        int umbraDecreasingCount = 0;
        int penumbraIncreasingCount = 0;
        int penumbraDecreasingCount = 0;
        for (FieldEventsLogger.FieldLoggedEvent<T> event : logger.getLoggedEvents()) {
            final FieldEclipseDetector<T> eclipseDetector = (FieldEclipseDetector<T>) (event.getEventDetector());
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

    private <T extends CalculusFieldElement<T>> FieldEclipseDetector<T> buildDetector(Field<T> field, final boolean totalEclipse) {

        FieldEclipseDetector<T> detector =
                new FieldEclipseDetector<>(field, CelestialBodyFactory.getSun(), 696000000,
                                           new OneAxisEllipsoid(6400000, 0.0, FramesFactory.getGCRF())).
                withMaxCheck(60.0).
                withThreshold(field.getZero().newInstance(1.0e-3));

        if (totalEclipse) {
            detector = detector.withUmbra();
        } else {
            detector = detector.withPenumbra();
        }

        detector = detector.withHandler((s, detector1, increasing) -> {
            ++count;
            return Action.CONTINUE;
        });

        return detector;

    }

    @AfterEach
    void tearDown() {
        count = 0;
    }

}

