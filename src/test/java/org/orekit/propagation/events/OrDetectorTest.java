/* Copyright 2002-2023 CS GROUP
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

import java.util.Collections;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;

/**
 * Unit tests for {@link BooleanDetector#orCombine(EventDetector...)}.
 *
 * @author Evan Ward
 */
public class OrDetectorTest {

    /** first operand. */
    private MockDetector a;
    /** second operand. */
    private MockDetector b;
    /** null */
    private SpacecraftState s;
    /** subject under test */
    private BooleanDetector or;

    /** create subject under test and dependencies. */
    @BeforeEach
    public void setUp() {
        a = new MockDetector();
        b = new MockDetector();
        s = null;
        or = BooleanDetector.orCombine(a, b);
    }

    /**
     * check {@link BooleanDetector#g(SpacecraftState)}.
     */
    @Test
    public void testG() {
        // test zero cases
        a.g = b.g = 0.0;
        Assertions.assertEquals(0.0, or.g(s), 0);
        a.g = -1;
        b.g = 0;
        Assertions.assertEquals(0.0, or.g(s), 0);
        a.g = 0;
        b.g = -1;
        Assertions.assertEquals(0.0, or.g(s), 0);

        // test negative cases
        a.g = -1;
        b.g = -1;
        Assertions.assertTrue(or.g(s) < 0, "negative");

        // test positive cases
        a.g = 0;
        b.g = 1;
        Assertions.assertTrue(or.g(s) > 0, "positive");
        a.g = 1;
        b.g = -1;
        Assertions.assertTrue(or.g(s) > 0, "positive");
        a.g = 1;
        b.g = 0;
        Assertions.assertTrue(or.g(s) > 0, "positive");
        a.g = -1;
        b.g = 1;
        Assertions.assertTrue(or.g(s) > 0, "positive");
        a.g = 1;
        b.g = 1;
        Assertions.assertTrue(or.g(s) > 0, "positive");

    }

    /**
     * check when there is numeric cancellation between the two g values.
     */
    @Test
    public void testCancellation() {
        a.g = -1e-10;
        b.g = -1e10;
        Assertions.assertTrue(or.g(s) < 0, "negative");
        a.g = -1e10;
        b.g = -1e-10;
        Assertions.assertTrue(or.g(s) < 0, "negative");
        a.g = -1e10;
        b.g = 1e-10;
        Assertions.assertTrue(or.g(s) > 0, "positive");
        a.g = 1e-10;
        b.g = -1e10;
        Assertions.assertTrue(or.g(s) > 0, "positive");
        a.g = 1e10;
        b.g = -1e-10;
        Assertions.assertTrue(or.g(s) > 0, "positive");
        a.g = -1e-10;
        b.g = 1e10;
        Assertions.assertTrue(or.g(s) > 0, "positive");
    }

    /**
     * Check wrapped detectors are initialized.
     */
    @Test
    public void testInit() {
        // setup
        EventDetector a = Mockito.mock(EventDetector.class);
        Mockito.when(a.getMaxCheckInterval()).thenReturn(s -> AbstractDetector.DEFAULT_MAXCHECK);
        Mockito.when(a.getThreshold()).thenReturn(AbstractDetector.DEFAULT_THRESHOLD);
        EventDetector b = Mockito.mock(EventDetector.class);
        Mockito.when(b.getMaxCheckInterval()).thenReturn(s -> AbstractDetector.DEFAULT_MAXCHECK);
        Mockito.when(b.getThreshold()).thenReturn(AbstractDetector.DEFAULT_THRESHOLD);
        EventHandler c = Mockito.mock(EventHandler.class);
        BooleanDetector or = BooleanDetector.orCombine(a, b).withHandler(c);
        AbsoluteDate t = AbsoluteDate.CCSDS_EPOCH;
        s = Mockito.mock(SpacecraftState.class);
        Mockito.when(s.getDate()).thenReturn(t.shiftedBy(60.0));

        // action
        or.init(s, t);

        // verify
        Assertions.assertEquals(2, or.getDetectors().size());
        Mockito.verify(a).init(s, t);
        Mockito.verify(b).init(s, t);
        Mockito.verify(c).init(s, t, or);
    }

    /** check when no operands are passed to the constructor. */
    @Test
    public void testZeroDetectors() {
        // action
        try {
            BooleanDetector.orCombine(Collections.emptyList());
            Assertions.fail("Expected Exception");
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
            return AbstractDetector.DEFAULT_THRESHOLD;
        }

        @Override
        public AdaptableInterval getMaxCheckInterval() {
            return s -> AbstractDetector.DEFAULT_MAXCHECK;
        }

        @Override
        public int getMaxIterationCount() {
            return 0;
        }

        @Override
        public EventHandler getHandler() {
            return (state, detector, increasing) -> null;
        }
    }
}
