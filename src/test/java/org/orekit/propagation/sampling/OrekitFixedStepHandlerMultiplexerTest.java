/* Copyright 2002-2020 CS GROUP
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

public class OrekitFixedStepHandlerMultiplexerTest {

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @Test
    public void testMultiplexer() {

        // init
        AbsoluteDate initDate = new AbsoluteDate(2020, 2, 28, 16, 15, 0.0, TimeScalesFactory.getUTC());

        InitCheckerHandler initHandler = new InitCheckerHandler(1.0);
        IncrementationHandler incrementationHandler = new IncrementationHandler();

        OrekitFixedStepHandlerMultiplexer handler = new OrekitFixedStepHandlerMultiplexer();
        handler.add(initHandler);
        handler.add(incrementationHandler);

        Orbit ic = new KeplerianOrbit(6378137 + 500e3, 1e-3, 0, 0, 0, 0,
                PositionAngle.TRUE, FramesFactory.getGCRF(), initDate, Constants.WGS84_EARTH_MU);
        Propagator propagator = new KeplerianPropagator(ic);
        propagator.setMasterMode(60, handler);

        Assert.assertFalse(initHandler.isInitialized());
        Assert.assertEquals(1.0, initHandler.getExpected(), Double.MIN_VALUE);

        propagator.propagate(initDate.shiftedBy(90.0));

        // verify
        Assert.assertTrue(initHandler.isInitialized());
        Assert.assertEquals(2.0, initHandler.getExpected(), Double.MIN_VALUE);
        Assert.assertEquals(3, incrementationHandler.getValue());
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
        public void handleStep(SpacecraftState currentState, boolean isLast) {
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
            // Do nothing
        }

        @Override
        public void init(SpacecraftState s0, AbsoluteDate t, double step) {
            this.value = 1;
        }

        @Override
        public void handleStep(SpacecraftState currentState, boolean isLast) {
            this.value++;
        }

        int getValue() {
            return value;
        }

    }
}
