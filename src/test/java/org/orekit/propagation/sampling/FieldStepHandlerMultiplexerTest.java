/* Copyright 2002-2024 CS GROUP
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

package org.orekit.propagation.sampling;

import org.hipparchus.Field;
import org.hipparchus.ode.events.Action;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldPropagator;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.analytical.FieldKeplerianPropagator;
import org.orekit.propagation.events.FieldDateDetector;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FieldStepHandlerMultiplexerTest {

    FieldAbsoluteDate<Binary64> initDate;
    FieldPropagator<Binary64> propagator;

    @BeforeEach
    void setUp() {
        Utils.setDataRoot("regular-data");
        Field<Binary64> field = Binary64Field.getInstance();
        Binary64        zero  = field.getZero();
        initDate = new FieldAbsoluteDate<>(field, 2020, 2, 28, 16, 15, 0.0, TimeScalesFactory.getUTC());
        FieldOrbit<Binary64> ic = new FieldKeplerianOrbit<>(zero.add(6378137 + 500e3), zero.add(1e-3), zero, zero, zero, zero,
                                                             PositionAngleType.TRUE, FramesFactory.getGCRF(), initDate,
                                                             zero.add(Constants.WGS84_EARTH_MU));
        propagator = new FieldKeplerianPropagator<>(ic);
    }

    @AfterEach
    void tearDown() {
        initDate   = null;
        propagator = null;
    }

    @Test
    void testMixedSteps() {

        Field<Binary64> field = Binary64Field.getInstance();
        Binary64        zero  = field.getZero();

        FieldStepHandlerMultiplexer<Binary64> multiplexer = propagator.getMultiplexer();

        FieldInitCheckerHandler initHandler = new FieldInitCheckerHandler(1.0);
        FieldFixedCounter    counter60  = new FieldFixedCounter();
        FieldVariableCounter counterVar = new FieldVariableCounter();
        FieldFixedCounter    counter10  = new FieldFixedCounter();

        multiplexer.add(zero.newInstance(60.0), initHandler);
        multiplexer.add(zero.newInstance(60.0), counter60);
        multiplexer.add(counterVar);
        multiplexer.add(zero.newInstance(10.0), counter10);
        assertEquals(4, multiplexer.getHandlers().size());

        assertFalse(initHandler.isInitialized());
        assertEquals(1.0, initHandler.getExpected(), Double.MIN_VALUE);

        propagator.propagate(initDate.shiftedBy(90.0));

        // verify
        assertTrue(initHandler.isInitialized());
        assertEquals(2.0, initHandler.getExpected(), Double.MIN_VALUE);

        assertEquals( 1,  counter60.initCount);
        assertEquals( 2,  counter60.handleCount);
        assertEquals( 1,  counter60.finishCount);

        assertEquals( 1,  counterVar.initCount);
        assertEquals( 1,  counterVar.handleCount);
        assertEquals( 1,  counterVar.finishCount);

        assertEquals( 1,  counter10.initCount);
        assertEquals(10,  counter10.handleCount);
        assertEquals( 1,  counter10.finishCount);

    }

    @Test
    void testRemove() {

        FieldStepHandlerMultiplexer<Binary64> multiplexer = propagator.getMultiplexer();

        Field<Binary64> field = Binary64Field.getInstance();
        Binary64        zero  = field.getZero();

        FieldFixedCounter    counter60  = new FieldFixedCounter();
        FieldVariableCounter counterVar = new FieldVariableCounter();
        FieldFixedCounter    counter10  = new FieldFixedCounter();

        multiplexer.add(zero.newInstance(60.0), counter60);
        multiplexer.add(counterVar);
        multiplexer.add(zero.newInstance(10.0), counter10);
        assertEquals(3, multiplexer.getHandlers().size());
        assertInstanceOf(FieldFixedCounter.class, ((FieldOrekitStepNormalizer<Binary64>) multiplexer.getHandlers().get(0)).getFixedStepHandler());
        assertEquals(60.0, ((FieldOrekitStepNormalizer<Binary64>) multiplexer.getHandlers().get(0)).getFixedTimeStep().getReal(), 1.0e-15);
        assertInstanceOf(FieldFixedCounter.class, ((FieldOrekitStepNormalizer<Binary64>) multiplexer.getHandlers().get(2)).getFixedStepHandler());
        assertEquals(10.0, ((FieldOrekitStepNormalizer<Binary64>) multiplexer.getHandlers().get(2)).getFixedTimeStep().getReal(), 1.0e-15);

        // first run with all handlers
        propagator.propagate(initDate.shiftedBy(90.0));
        assertEquals( 1,    counter60.initCount);
        assertEquals( 2,    counter60.handleCount);
        assertEquals( 1,    counter60.finishCount);
        assertEquals(  0.0, counter60.start, 1.0e-15);
        assertEquals( 90.0, counter60.stop, 1.0e-15);
        assertEquals( 1,    counterVar.initCount);
        assertEquals( 1,    counterVar.handleCount);
        assertEquals( 1,    counterVar.finishCount);
        assertEquals(  0.0, counterVar.start, 1.0e-15);
        assertEquals( 90.0, counterVar.stop, 1.0e-15);
        assertEquals( 1,    counter10.initCount);
        assertEquals(10,    counter10.handleCount);
        assertEquals( 1,    counter10.finishCount);
        assertEquals(  0.0, counter10.start, 1.0e-15);
        assertEquals( 90.0, counter10.stop, 1.0e-15);

        // removing the handler at 10 seconds
        multiplexer.remove(counter10);
        assertEquals(2, multiplexer.getHandlers().size());
        propagator.propagate(initDate.shiftedBy(100.0), initDate.shiftedBy(190.0));
        assertEquals( 2,    counter60.initCount);
        assertEquals( 4,    counter60.handleCount);
        assertEquals( 2,    counter60.initCount);
        assertEquals(100.0, counter60.start, 1.0e-15);
        assertEquals(190.0, counter60.stop, 1.0e-15);
        assertEquals( 2,    counterVar.initCount);
        assertEquals( 2,    counterVar.handleCount);
        assertEquals( 2,    counterVar.finishCount);
        assertEquals(100.0, counterVar.start, 1.0e-15);
        assertEquals(190.0, counterVar.stop, 1.0e-15);
        assertEquals( 1,    counter10.initCount);
        assertEquals(10,    counter10.handleCount);
        assertEquals( 1,    counter10.initCount);
        assertEquals(  0.0, counter10.start, 1.0e-15);
        assertEquals( 90.0, counter10.stop, 1.0e-15);

        // attempting to remove a handler already removed
        multiplexer.remove(counter10);
        assertEquals(2, multiplexer.getHandlers().size());
        propagator.propagate(initDate.shiftedBy(200.0), initDate.shiftedBy(290.0));
        assertEquals( 3,    counter60.initCount);
        assertEquals( 6,    counter60.handleCount);
        assertEquals( 3,    counter60.finishCount);
        assertEquals(200.0, counter60.start, 1.0e-15);
        assertEquals(290.0, counter60.stop, 1.0e-15);
        assertEquals( 3,    counterVar.initCount);
        assertEquals( 3,    counterVar.handleCount);
        assertEquals( 3,    counterVar.finishCount);
        assertEquals(200.0, counterVar.start, 1.0e-15);
        assertEquals(290.0, counterVar.stop, 1.0e-15);
        assertEquals( 1,    counter10.initCount);
        assertEquals(10,    counter10.handleCount);
        assertEquals( 1,    counter10.finishCount);
        assertEquals(  0.0, counter10.start, 1.0e-15);
        assertEquals( 90.0, counter10.stop, 1.0e-15);

        // removing the handler with variable stepsize
        multiplexer.remove(counterVar);
        assertEquals(1, multiplexer.getHandlers().size());
        propagator.propagate(initDate.shiftedBy(300.0), initDate.shiftedBy(390.0));
        assertEquals( 4,    counter60.initCount);
        assertEquals( 8,    counter60.handleCount);
        assertEquals( 4,    counter60.initCount);
        assertEquals(300.0, counter60.start, 1.0e-15);
        assertEquals(390.0, counter60.stop, 1.0e-15);
        assertEquals( 3,    counterVar.initCount);
        assertEquals( 3,    counterVar.handleCount);
        assertEquals( 3,    counterVar.finishCount);
        assertEquals(200.0, counterVar.start, 1.0e-15);
        assertEquals(290.0, counterVar.stop, 1.0e-15);
        assertEquals( 1,    counter10.initCount);
        assertEquals(10,    counter10.handleCount);
        assertEquals( 1,    counter10.initCount);
        assertEquals(  0.0, counter10.start, 1.0e-15);
        assertEquals( 90.0, counter10.stop, 1.0e-15);

        // attempting to remove a handler already removed
        multiplexer.remove(counterVar);
        assertEquals(1, multiplexer.getHandlers().size());
        propagator.propagate(initDate.shiftedBy(400.0), initDate.shiftedBy(490.0));
        assertEquals( 5,    counter60.initCount);
        assertEquals(10,    counter60.handleCount);
        assertEquals( 5,    counter60.finishCount);
        assertEquals(400.0, counter60.start, 1.0e-15);
        assertEquals(490.0, counter60.stop, 1.0e-15);
        assertEquals( 3,    counterVar.initCount);
        assertEquals( 3,    counterVar.handleCount);
        assertEquals( 3,    counterVar.finishCount);
        assertEquals(200.0, counterVar.start, 1.0e-15);
        assertEquals(290.0, counterVar.stop, 1.0e-15);
        assertEquals( 1,    counter10.initCount);
        assertEquals(10,    counter10.handleCount);
        assertEquals( 1,    counter10.finishCount);
        assertEquals(  0.0, counter10.start, 1.0e-15);
        assertEquals( 90.0, counter10.stop, 1.0e-15);

        // removing everything
        multiplexer.clear();
        assertEquals(0, multiplexer.getHandlers().size());
        propagator.propagate(initDate.shiftedBy(500.0), initDate.shiftedBy(590.0));
        assertEquals( 5,    counter60.initCount);
        assertEquals(10,    counter60.handleCount);
        assertEquals( 5,    counter60.finishCount);
        assertEquals(400.0, counter60.start, 1.0e-15);
        assertEquals(490.0, counter60.stop, 1.0e-15);
        assertEquals( 3,    counterVar.initCount);
        assertEquals( 3,    counterVar.handleCount);
        assertEquals( 3,    counterVar.finishCount);
        assertEquals(200.0, counterVar.start, 1.0e-15);
        assertEquals(290.0, counterVar.stop, 1.0e-15);
        assertEquals( 1,    counter10.initCount);
        assertEquals(10,    counter10.handleCount);
        assertEquals( 1,    counter10.finishCount);
        assertEquals(  0.0, counter10.start, 1.0e-15);
        assertEquals( 90.0, counter10.stop, 1.0e-15);

    }

    @Test
    void testOnTheFlyChanges() {

        final FieldStepHandlerMultiplexer<Binary64> multiplexer = propagator.getMultiplexer();

        Field<Binary64> field = Binary64Field.getInstance();
        Binary64        zero  = field.getZero();

        double               add60      =  3.0;
        double               rem60      = 78.0;
        FieldFixedCounter    counter60  = new FieldFixedCounter();
        FieldDateDetector<Binary64> d1 = new FieldDateDetector<>(field, initDate.shiftedBy(add60)).
                                         withHandler((s, d, i) -> {
                                             multiplexer.add(zero.newInstance(60.0), counter60);
                                            return Action.CONTINUE;
                                         });
        propagator.addEventDetector(d1);
        FieldDateDetector<Binary64> d2 = new FieldDateDetector<>(field, initDate.shiftedBy(rem60)).
                                         withHandler((s, d, i) -> {
                                             multiplexer.remove(counter60);
                                             return Action.CONTINUE;
                                         });
        propagator.addEventDetector(d2);

        double               addVar     =  5.0;
        double               remVar     =  7.0;
        FieldVariableCounter counterVar = new FieldVariableCounter();
        FieldDateDetector<Binary64> d3 = new FieldDateDetector<>(field, initDate.shiftedBy(addVar)).
                                         withHandler((s, d, i) -> {
                                             multiplexer.add(counterVar);
                                             return Action.CONTINUE;
                                         });
        propagator.addEventDetector(d3);
        FieldDateDetector<Binary64> d4 = new FieldDateDetector<>(field, initDate.shiftedBy(remVar)).
                                         withHandler((s, d, i) -> {
                                             multiplexer.remove(counterVar);
                                             return Action.CONTINUE;
                                         });
        propagator.addEventDetector(d4);

        double               add10      =  6.0;
        double               rem10      = 82.0;
        FieldFixedCounter    counter10  = new FieldFixedCounter();
        FieldDateDetector<Binary64> d5 = new FieldDateDetector<>(field, initDate.shiftedBy(add10)).
                                         withHandler((s, d, i) -> {
                                             multiplexer.add(zero.newInstance(10.0), counter10);
                                             return Action.CONTINUE;
                                         });
        propagator.addEventDetector(d5);
        FieldDateDetector<Binary64> d6 = new FieldDateDetector<>(field, initDate.shiftedBy(rem10)).
                                         withHandler((s, d, i) -> {
                                             multiplexer.clear();
                                             return Action.CONTINUE;
                                         });
        propagator.addEventDetector(d6);

        // full run, which will add and remove step handlers on the fly
        propagator.propagate(initDate.shiftedBy(90.0));
        assertEquals( 1,     counter60.initCount);
        assertEquals( 2,     counter60.handleCount);
        assertEquals( 1,     counter60.finishCount);
        assertEquals(add60,  counter60.start, 1.0e-15);
        assertEquals(rem60,  counter60.stop, 1.0e-15);
        assertEquals( 1,     counterVar.initCount);
        assertEquals( 2,     counterVar.handleCount); // event at add10 splits the variable step in two parts
        assertEquals( 1,     counterVar.finishCount);
        assertEquals(addVar, counterVar.start, 1.0e-15);
        assertEquals(remVar, counterVar.stop, 1.0e-15);
        assertEquals( 1,     counter10.initCount);
        assertEquals( 8,     counter10.handleCount);
        assertEquals( 1,     counter10.finishCount);
        assertEquals(add10,  counter10.start, 1.0e-15);
        assertEquals(rem10,  counter10.stop, 1.0e-15);

    }

    private class FieldInitCheckerHandler implements FieldOrekitFixedStepHandler<Binary64> {

        private double expected;
        private boolean initialized;

        public FieldInitCheckerHandler(final double expected) {
            this.expected    = expected;
            this.initialized = false;
        }

        @Override
        public void init(FieldSpacecraftState<Binary64> s0, FieldAbsoluteDate<Binary64> t, Binary64 step) {
            initialized = true;
        }

        @Override
        public void handleStep(FieldSpacecraftState<Binary64> currentState) {
            this.expected = 2.0;
        }

        boolean isInitialized() {
            return initialized;
        }

        double getExpected() {
            return expected;
        }

    }

    private class FieldFixedCounter implements FieldOrekitFixedStepHandler<Binary64> {

        private int    initCount;
        private int    handleCount;
        private int    finishCount;
        private double start;
        private double stop;

        @Override
        public void init(FieldSpacecraftState<Binary64> s0, FieldAbsoluteDate<Binary64> t, Binary64 step) {
            ++initCount;
            start = s0.getDate().durationFrom(initDate).getReal();
        }

        @Override
        public void handleStep(FieldSpacecraftState<Binary64> currentState) {
            ++handleCount;
        }

        @Override
        public void finish(FieldSpacecraftState<Binary64> finalState) {
            ++finishCount;
            stop = finalState.getDate().durationFrom(initDate).getReal();
        }

    }

    private class FieldVariableCounter implements FieldOrekitStepHandler<Binary64> {

        private int    initCount;
        private int    handleCount;
        private int    finishCount;
        private double start;
        private double stop;

        @Override
        public void init(FieldSpacecraftState<Binary64> s0, FieldAbsoluteDate<Binary64> t) {
            ++initCount;
            start = s0.getDate().durationFrom(initDate).getReal();
        }

        @Override
        public void handleStep(FieldOrekitStepInterpolator<Binary64> interpolator) {
            assertNotNull(interpolator.getPosition(interpolator.getCurrentState().getDate(),
                                                              interpolator.getCurrentState().getFrame()));
            ++handleCount;
        }

        @Override
        public void finish(FieldSpacecraftState<Binary64> finalState) {
            ++finishCount;
            stop = finalState.getDate().durationFrom(initDate).getReal();
        }

    }

}
