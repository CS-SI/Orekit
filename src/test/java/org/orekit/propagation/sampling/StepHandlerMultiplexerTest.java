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

    @Test
    public void testFixedStep() {

        StepHandlerMultiplexer multiplexer = new StepHandlerMultiplexer();
        propagator.setMasterMode(multiplexer);

        InitCheckerHandler initHandler = new InitCheckerHandler(1.0);
        IncrementationHandler incrementation60Handler = new IncrementationHandler();
        IncrementationHandler incrementation10Handler = new IncrementationHandler();

        multiplexer.add(60, initHandler);
        multiplexer.add(60, incrementation60Handler);
        multiplexer.add(10, incrementation10Handler);

        Assert.assertFalse(initHandler.isInitialized());
        Assert.assertEquals(1.0, initHandler.getExpected(), Double.MIN_VALUE);

        propagator.propagate(initDate.shiftedBy(90.0));

        // verify
        Assert.assertTrue(initHandler.isInitialized());
        Assert.assertEquals(2.0, initHandler.getExpected(), Double.MIN_VALUE);

        // init called once
        // handleStep called at t₀, t₀ + 60
        // finish called at t₁
        Assert.assertEquals( 4,  incrementation60Handler.getValue());

        // init called once
        // handleStep called at t₀, t₀ + 10, t₀ + 20, t₀ + 30, t₀ + 40, t₀ + 50, t₀ + 60, t₀ + 70, t₀ + 80, t₀ + 90
        // finish called at t₁
        Assert.assertEquals(12,  incrementation10Handler.getValue());

    }

    @Test
    public void testRemove() {

        StepHandlerMultiplexer multiplexer = new StepHandlerMultiplexer();
        propagator.setMasterMode(multiplexer);

        IncrementationHandler incrementation60Handler = new IncrementationHandler();
        IncrementationHandler incrementation10Handler = new IncrementationHandler();

        multiplexer.add(60, incrementation60Handler);
        multiplexer.add(10, incrementation10Handler);

        // first run with both handlers
        propagator.propagate(initDate.shiftedBy(90.0));
        Assert.assertEquals( 4,  incrementation60Handler.getValue());
        Assert.assertEquals(12,  incrementation10Handler.getValue());

        // removing the handler at 10 seconds
        multiplexer.remove(incrementation10Handler);
        propagator.propagate(initDate, initDate.shiftedBy(90.0));
        Assert.assertEquals( 8,  incrementation60Handler.getValue());
        Assert.assertEquals(12,  incrementation10Handler.getValue());

        // attempting to remove a handler already removed
        multiplexer.remove(incrementation10Handler);
        propagator.propagate(initDate, initDate.shiftedBy(90.0));
        Assert.assertEquals(12,  incrementation60Handler.getValue());
        Assert.assertEquals(12,  incrementation10Handler.getValue());
        
        // removing everything
        multiplexer.clear();
        propagator.propagate(initDate, initDate.shiftedBy(90.0));
        Assert.assertEquals(12,  incrementation60Handler.getValue());
        Assert.assertEquals(12,  incrementation10Handler.getValue());
        
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

    private class IncrementationHandler implements OrekitFixedStepHandler {

        private int value;

        public IncrementationHandler() {
            this.value = 0;
        }

        @Override
        public void init(SpacecraftState s0, AbsoluteDate t, double step) {
            this.value++;
        }

        @Override
        public void handleStep(SpacecraftState currentState) {
            this.value++;
        }

        @Override
        public void finish(SpacecraftState finalState) {
            this.value++;
        }

        int getValue() {
            return value;
        }

    }

}
