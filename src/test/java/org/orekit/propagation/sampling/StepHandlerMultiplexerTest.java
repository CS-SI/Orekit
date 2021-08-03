/* Copyright 2002-2021 CS GROUP
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

import org.hipparchus.ode.events.Action;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.events.DateDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;

public class StepHandlerMultiplexerTest {

    AbsoluteDate initDate;
    Propagator propagator;

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
        initDate = new AbsoluteDate(2020, 2, 28, 16, 15, 0.0, TimeScalesFactory.getUTC());
        Orbit ic = new KeplerianOrbit(6378137 + 500e3, 1e-3, 0, 0, 0, 0,
                                      PositionAngle.TRUE, FramesFactory.getGCRF(), initDate, Constants.WGS84_EARTH_MU);
        propagator = new KeplerianPropagator(ic);
    }

    @After
    public void tearDown() {
        initDate   = null;
        propagator = null;
    }

    @Test
    public void testMixedSteps() {

        StepHandlerMultiplexer multiplexer = propagator.getMultiplexer();

        InitCheckerHandler initHandler = new InitCheckerHandler(1.0);
        FixedCounter    counter60  = new FixedCounter();
        VariableCounter counterVar = new VariableCounter();
        FixedCounter    counter10  = new FixedCounter();

        multiplexer.add(60, initHandler);
        multiplexer.add(60, counter60);
        multiplexer.add(counterVar);
        multiplexer.add(10, counter10);
        Assert.assertEquals(4, multiplexer.getHandlers().size());

        Assert.assertFalse(initHandler.isInitialized());
        Assert.assertEquals(1.0, initHandler.getExpected(), Double.MIN_VALUE);

        propagator.propagate(initDate.shiftedBy(90.0));

        // verify
        Assert.assertTrue(initHandler.isInitialized());
        Assert.assertEquals(2.0, initHandler.getExpected(), Double.MIN_VALUE);

        Assert.assertEquals( 1,  counter60.initCount);
        Assert.assertEquals( 2,  counter60.handleCount);
        Assert.assertEquals( 1,  counter60.finishCount);

        Assert.assertEquals( 1,  counterVar.initCount);
        Assert.assertEquals( 2,  counterVar.handleCount);
        Assert.assertEquals( 1,  counterVar.finishCount);

        Assert.assertEquals( 1,  counter10.initCount);
        Assert.assertEquals(10,  counter10.handleCount);
        Assert.assertEquals( 1,  counter10.finishCount);

    }

    @Test
    public void testRemove() {

        StepHandlerMultiplexer multiplexer = propagator.getMultiplexer();

        FixedCounter    counter60  = new FixedCounter();
        VariableCounter counterVar = new VariableCounter();
        FixedCounter    counter10  = new FixedCounter();

        multiplexer.add(60, counter60);
        multiplexer.add(counterVar);
        multiplexer.add(10, counter10);
        Assert.assertEquals(3, multiplexer.getHandlers().size());
        Assert.assertTrue(((OrekitStepNormalizer) multiplexer.getHandlers().get(0)).getFixedStepHandler() instanceof FixedCounter);
        Assert.assertEquals(60.0, ((OrekitStepNormalizer) multiplexer.getHandlers().get(0)).getFixedTimeStep(), 1.0e-15);
        Assert.assertTrue(((OrekitStepNormalizer) multiplexer.getHandlers().get(2)).getFixedStepHandler() instanceof FixedCounter);
        Assert.assertEquals(10.0, ((OrekitStepNormalizer) multiplexer.getHandlers().get(2)).getFixedTimeStep(), 1.0e-15);

        // first run with all handlers
        propagator.propagate(initDate.shiftedBy(90.0));
        Assert.assertEquals( 1,    counter60.initCount);
        Assert.assertEquals( 2,    counter60.handleCount);
        Assert.assertEquals( 1,    counter60.finishCount);
        Assert.assertEquals(  0.0, counter60.start, 1.0e-15);
        Assert.assertEquals( 90.0, counter60.stop, 1.0e-15);
        Assert.assertEquals( 1,    counterVar.initCount);
        Assert.assertEquals( 2,    counterVar.handleCount);
        Assert.assertEquals( 1,    counterVar.finishCount);
        Assert.assertEquals(  0.0, counterVar.start, 1.0e-15);
        Assert.assertEquals( 90.0, counterVar.stop, 1.0e-15);
        Assert.assertEquals( 1,    counter10.initCount);
        Assert.assertEquals(10,    counter10.handleCount);
        Assert.assertEquals( 1,    counter10.finishCount);
        Assert.assertEquals(  0.0, counter10.start, 1.0e-15);
        Assert.assertEquals( 90.0, counter10.stop, 1.0e-15);

        // removing the handler at 10 seconds
        multiplexer.remove(counter10);
        Assert.assertEquals(2, multiplexer.getHandlers().size());
        propagator.propagate(initDate.shiftedBy(100.0), initDate.shiftedBy(190.0));
        Assert.assertEquals( 2,    counter60.initCount);
        Assert.assertEquals( 4,    counter60.handleCount);
        Assert.assertEquals( 2,    counter60.initCount);
        Assert.assertEquals(100.0, counter60.start, 1.0e-15);
        Assert.assertEquals(190.0, counter60.stop, 1.0e-15);
        Assert.assertEquals( 2,    counterVar.initCount);
        Assert.assertEquals( 4,    counterVar.handleCount);
        Assert.assertEquals( 2,    counterVar.finishCount);
        Assert.assertEquals(100.0, counterVar.start, 1.0e-15);
        Assert.assertEquals(190.0, counterVar.stop, 1.0e-15);
        Assert.assertEquals( 1,    counter10.initCount);
        Assert.assertEquals(10,    counter10.handleCount);
        Assert.assertEquals( 1,    counter10.initCount);
        Assert.assertEquals(  0.0, counter10.start, 1.0e-15);
        Assert.assertEquals( 90.0, counter10.stop, 1.0e-15);

        // attempting to remove a handler already removed
        multiplexer.remove(counter10);
        Assert.assertEquals(2, multiplexer.getHandlers().size());
        propagator.propagate(initDate.shiftedBy(200.0), initDate.shiftedBy(290.0));
        Assert.assertEquals( 3,    counter60.initCount);
        Assert.assertEquals( 6,    counter60.handleCount);
        Assert.assertEquals( 3,    counter60.finishCount);
        Assert.assertEquals(200.0, counter60.start, 1.0e-15);
        Assert.assertEquals(290.0, counter60.stop, 1.0e-15);
        Assert.assertEquals( 3,    counterVar.initCount);
        Assert.assertEquals( 6,    counterVar.handleCount);
        Assert.assertEquals( 3,    counterVar.finishCount);
        Assert.assertEquals(200.0, counterVar.start, 1.0e-15);
        Assert.assertEquals(290.0, counterVar.stop, 1.0e-15);
        Assert.assertEquals( 1,    counter10.initCount);
        Assert.assertEquals(10,    counter10.handleCount);
        Assert.assertEquals( 1,    counter10.finishCount);
        Assert.assertEquals(  0.0, counter10.start, 1.0e-15);
        Assert.assertEquals( 90.0, counter10.stop, 1.0e-15);
        
        // removing the handler with variable stepsize
        multiplexer.remove(counterVar);
        Assert.assertEquals(1, multiplexer.getHandlers().size());
        propagator.propagate(initDate.shiftedBy(300.0), initDate.shiftedBy(390.0));
        Assert.assertEquals( 4,    counter60.initCount);
        Assert.assertEquals( 8,    counter60.handleCount);
        Assert.assertEquals( 4,    counter60.initCount);
        Assert.assertEquals(300.0, counter60.start, 1.0e-15);
        Assert.assertEquals(390.0, counter60.stop, 1.0e-15);
        Assert.assertEquals( 3,    counterVar.initCount);
        Assert.assertEquals( 6,    counterVar.handleCount);
        Assert.assertEquals( 3,    counterVar.finishCount);
        Assert.assertEquals(200.0, counterVar.start, 1.0e-15);
        Assert.assertEquals(290.0, counterVar.stop, 1.0e-15);
        Assert.assertEquals( 1,    counter10.initCount);
        Assert.assertEquals(10,    counter10.handleCount);
        Assert.assertEquals( 1,    counter10.initCount);
        Assert.assertEquals(  0.0, counter10.start, 1.0e-15);
        Assert.assertEquals( 90.0, counter10.stop, 1.0e-15);

        // attempting to remove a handler already removed
        multiplexer.remove(counterVar);
        Assert.assertEquals(1, multiplexer.getHandlers().size());
        propagator.propagate(initDate.shiftedBy(400.0), initDate.shiftedBy(490.0));
        Assert.assertEquals( 5,    counter60.initCount);
        Assert.assertEquals(10,    counter60.handleCount);
        Assert.assertEquals( 5,    counter60.finishCount);
        Assert.assertEquals(400.0, counter60.start, 1.0e-15);
        Assert.assertEquals(490.0, counter60.stop, 1.0e-15);
        Assert.assertEquals( 3,    counterVar.initCount);
        Assert.assertEquals( 6,    counterVar.handleCount);
        Assert.assertEquals( 3,    counterVar.finishCount);
        Assert.assertEquals(200.0, counterVar.start, 1.0e-15);
        Assert.assertEquals(290.0, counterVar.stop, 1.0e-15);
        Assert.assertEquals( 1,    counter10.initCount);
        Assert.assertEquals(10,    counter10.handleCount);
        Assert.assertEquals( 1,    counter10.finishCount);
        Assert.assertEquals(  0.0, counter10.start, 1.0e-15);
        Assert.assertEquals( 90.0, counter10.stop, 1.0e-15);
        
        // removing everything
        multiplexer.clear();
        Assert.assertEquals(0, multiplexer.getHandlers().size());
        propagator.propagate(initDate.shiftedBy(500.0), initDate.shiftedBy(590.0));
        Assert.assertEquals( 5,    counter60.initCount);
        Assert.assertEquals(10,    counter60.handleCount);
        Assert.assertEquals( 5,    counter60.finishCount);
        Assert.assertEquals(400.0, counter60.start, 1.0e-15);
        Assert.assertEquals(490.0, counter60.stop, 1.0e-15);
        Assert.assertEquals( 3,    counterVar.initCount);
        Assert.assertEquals( 6,    counterVar.handleCount);
        Assert.assertEquals( 3,    counterVar.finishCount);
        Assert.assertEquals(200.0, counterVar.start, 1.0e-15);
        Assert.assertEquals(290.0, counterVar.stop, 1.0e-15);
        Assert.assertEquals( 1,    counter10.initCount);
        Assert.assertEquals(10,    counter10.handleCount);
        Assert.assertEquals( 1,    counter10.finishCount);
        Assert.assertEquals(  0.0, counter10.start, 1.0e-15);
        Assert.assertEquals( 90.0, counter10.stop, 1.0e-15);
        
    }

    @Test
    public void testOnTheFlyChanges() {

        StepHandlerMultiplexer multiplexer = propagator.getMultiplexer();

        double          add60      =  3.0;
        double          rem60      = 78.0;
        FixedCounter    counter60  = new FixedCounter();
        propagator.addEventDetector(new DateDetector(initDate.shiftedBy(add60)).
                                    withHandler((s, d, i) -> {
                                        multiplexer.add(60.0, counter60);
                                        return Action.CONTINUE;
                                    }));
        propagator.addEventDetector(new DateDetector(initDate.shiftedBy(rem60)).
                                    withHandler((s, d, i) -> {
                                        multiplexer.remove(counter60);
                                        return Action.CONTINUE;
                                    }));

        double          addVar     =  5.0;
        double          remVar     =  7.0;
        VariableCounter counterVar = new VariableCounter();
        propagator.addEventDetector(new DateDetector(initDate.shiftedBy(addVar)).
                                    withHandler((s, d, i) -> {
                                        multiplexer.add(counterVar);
                                        return Action.CONTINUE;
                                    }));
        propagator.addEventDetector(new DateDetector(initDate.shiftedBy(remVar)).
                                    withHandler((s, d, i) -> {
                                        multiplexer.remove(counterVar);
                                        return Action.CONTINUE;
                                    }));

        double          add10      =  6.0;
        double          rem10      = 82.0;
        FixedCounter    counter10  = new FixedCounter();
        propagator.addEventDetector(new DateDetector(initDate.shiftedBy(add10)).
                                    withHandler((s, d, i) -> {
                                        multiplexer.add(10.0, counter10);
                                        return Action.CONTINUE;
                                    }));
        propagator.addEventDetector(new DateDetector(initDate.shiftedBy(rem10)).
                                    withHandler((s, d, i) -> {
                                        multiplexer.clear();
                                        return Action.CONTINUE;
                                    }));

        // full run, which will add and remove step handlers on the fly
        propagator.propagate(initDate.shiftedBy(90.0));
        Assert.assertEquals( 1,     counter60.initCount);
        Assert.assertEquals( 2,     counter60.handleCount);
        Assert.assertEquals( 1,     counter60.finishCount);
        Assert.assertEquals(add60,  counter60.start, 1.0e-15);
        Assert.assertEquals(rem60,  counter60.stop, 1.0e-15);
        Assert.assertEquals( 1,     counterVar.initCount);
        Assert.assertEquals( 2,     counterVar.handleCount); // event at add10 splits the variable step in two parts
        Assert.assertEquals( 1,     counterVar.finishCount);
        Assert.assertEquals(addVar, counterVar.start, 1.0e-15);
        Assert.assertEquals(remVar, counterVar.stop, 1.0e-15);
        Assert.assertEquals( 1,     counter10.initCount);
        Assert.assertEquals( 8,     counter10.handleCount);
        Assert.assertEquals( 1,     counter10.finishCount);
        Assert.assertEquals(add10,  counter10.start, 1.0e-15);
        Assert.assertEquals(rem10,  counter10.stop, 1.0e-15);

    }

    private class InitCheckerHandler implements OrekitFixedStepHandler {

        private double expected;
        private boolean initialized;

        public InitCheckerHandler(final double expected) {
            this.expected    = expected;
            this.initialized = false;
        }

        @Override
        public void init(SpacecraftState s0, AbsoluteDate t, double step) {
            initialized = true;
        }

        @Override
        public void handleStep(SpacecraftState currentState) {
            this.expected = 2.0;
        }

        boolean isInitialized() {
            return initialized;
        }

        double getExpected() {
            return expected;
        }

    }

    private class FixedCounter implements OrekitFixedStepHandler {

        private int    initCount;
        private int    handleCount;
        private int    finishCount;
        private double start;
        private double stop;

        @Override
        public void init(SpacecraftState s0, AbsoluteDate t, double step) {
            ++initCount;
            start = s0.getDate().durationFrom(initDate);
        }

        @Override
        public void handleStep(SpacecraftState currentState) {
            ++handleCount;
        }

        @Override
        public void finish(SpacecraftState finalState) {
            ++finishCount;
            stop = finalState.getDate().durationFrom(initDate);
        }

    }

    private class VariableCounter implements OrekitStepHandler {

        private int    initCount;
        private int    handleCount;
        private int    finishCount;
        private double start;
        private double stop;

        @Override
        public void init(SpacecraftState s0, AbsoluteDate t) {
            ++initCount;
            start = s0.getDate().durationFrom(initDate);
        }

        @Override
        public void handleStep(OrekitStepInterpolator interpolator) {
            ++handleCount;
        }

        @Override
        public void finish(SpacecraftState finalState) {
            ++finishCount;
            stop = finalState.getDate().durationFrom(initDate);
        }

    }

}
