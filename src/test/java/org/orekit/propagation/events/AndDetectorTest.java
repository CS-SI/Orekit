/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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

import java.util.Collections;
import java.util.NoSuchElementException;

import org.hipparchus.ode.events.Action;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;

/**
 * Unit tests for {@link BooleanDetector}.
 *
 * @author Evan Ward
 */
public class AndDetectorTest {

    /** first operand. */
    private MockDetector a;
    /** second operand. */
    private MockDetector b;
    /** null */
    private SpacecraftState s;
    /** subject under test */
    private BooleanDetector and;

    /** create subject under test and dependencies. */
    @Before
    public void setUp() {
        a = new MockDetector();
        b = new MockDetector();
        s = null;
        and = BooleanDetector.andCombine(a, b);
    }

    /**
     * check {@link BooleanDetector#g(SpacecraftState)}.
     */
    @Test
    public void testG() {
        // test both zero
        a.g = b.g = 0.0;
        Assert.assertEquals(0.0, and.g(s), 0);

        // test either zero
        a.g = 1;
        b.g = 0;
        Assert.assertEquals(0.0, and.g(s), 0);
        a.g = 0;
        b.g = 1;
        Assert.assertEquals(0.0, and.g(s), 0);

        // test either negative
        a.g = 0;
        b.g = -1;
        Assert.assertTrue("negative", and.g(s) < 0);
        a.g = 1;
        b.g = -1;
        Assert.assertTrue("negative", and.g(s) < 0);
        a.g = -1;
        b.g = 0;
        Assert.assertTrue("negative", and.g(s) < 0);
        a.g = -1;
        b.g = 1;
        Assert.assertTrue("negative", and.g(s) < 0);
        a.g = -1;
        b.g = -1;
        Assert.assertTrue("negative", and.g(s) < 0);

        // test both positive
        a.g = 1;
        b.g = 1;
        Assert.assertTrue("positive", and.g(s) > 0);

    }

    /**
     * check {@link BooleanDetector} for cancellation.
     */
    @Test
    public void testCancellation() {
        a.g = -1e-10;
        b.g = 1e10;
        Assert.assertTrue("negative", and.g(s) < 0);
        a.g = 1e10;
        b.g = -1e-10;
        Assert.assertTrue("negative", and.g(s) < 0);
        a.g = 1e10;
        b.g = 1e-10;
        Assert.assertTrue("positive", and.g(s) > 0);
        a.g = 1e-10;
        b.g = 1e10;
        Assert.assertTrue("positive", and.g(s) > 0);
    }

    /**
     * Check wrapped detectors are initialized.
     */
    @Test
    public void testInit() {
        // setup
        EventDetector a = Mockito.mock(EventDetector.class);
        EventDetector b = Mockito.mock(EventDetector.class);
        @SuppressWarnings("unchecked")
        EventHandler<EventDetector> c = Mockito.mock(EventHandler.class);
        BooleanDetector and = BooleanDetector.andCombine(a, b).withHandler(c);
        AbsoluteDate t = AbsoluteDate.CCSDS_EPOCH;
        s = Mockito.mock(SpacecraftState.class);
        Mockito.when(s.getDate()).thenReturn(t.shiftedBy(60.0));

        // action
        and.init(s, t);

        // verify
        Mockito.verify(a).init(s, t);
        Mockito.verify(b).init(s, t);
        Mockito.verify(c).init(s, t);
    }

    /** check when no operands are passed to the constructor. */
    @Test
    public void testZeroDetectors() {
        // action
        try {
            BooleanDetector.andCombine(Collections.emptyList());
            Assert.fail("Expected Exception");
        } catch (NoSuchElementException e) {
            // expected
        }
    }

    /** Mock detector to set the g function to arbitrary values. */
    private static class MockDetector implements EventDetector {

        /** value to return from {@link #g(SpacecraftState)}. */
        public double g = 0;

        @Override
        public void init(SpacecraftState s0, AbsoluteDate t) {

        }

        @Override
        public double g(SpacecraftState s) {
            return this.g;
        }

        @Override
        public double getThreshold() {
            return 0;
        }

        @Override
        public double getMaxCheckInterval() {
            return 0;
        }

        @Override
        public int getMaxIterationCount() {
            return 0;
        }

        @Override
        public Action eventOccurred(SpacecraftState s, boolean increasing) {
            return null;
        }

        @Override
        public SpacecraftState resetState(SpacecraftState oldState) {
            return null;
        }
    }
}
