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

import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.handlers.FieldEventHandler;
import org.orekit.time.FieldAbsoluteDate;

/**
 * Unit tests for {@link BooleanDetector}.
 *
 * @author Evan Ward
 */
public class FieldAndDetectorTest {

    /** first operand. */
    private MockDetector a;
    /** second operand. */
    private MockDetector b;
    /** null (except for date) */
    private FieldSpacecraftState<Binary64> s;
    /** subject under test */
    private FieldBooleanDetector<Binary64> and;

    /** create subject under test and dependencies. */
    @SuppressWarnings("unchecked")
    @BeforeEach
    public void setUp() {
        a = new MockDetector();
        b = new MockDetector();
        s = Mockito.mock(FieldSpacecraftState.class);
        Mockito.when(s.getDate()).thenReturn(FieldAbsoluteDate.getArbitraryEpoch(Binary64Field.getInstance()));
        and = FieldBooleanDetector.andCombine(a, b);
    }

    /**
     * check {@link BooleanDetector#g(SpacecraftState)}.
     */
    @Test
    public void testG() {
        // test both zero
        a.g = b.g = new Binary64(0.0);
        Assertions.assertEquals(0.0, and.g(s).getReal(), 0);

        // test either zero
        a.g = new Binary64(1);
        b.g = new Binary64(0);
        Assertions.assertEquals(0.0, and.g(s).getReal(), 0);
        a.g = new Binary64(0);
        b.g = new Binary64(1);
        Assertions.assertEquals(0.0, and.g(s).getReal(), 0);

        // test either negative
        a.g = new Binary64(0);
        b.g = new Binary64(-1);
        Assertions.assertTrue(and.g(s).getReal() < 0, "negative");
        a.g = new Binary64(1);
        b.g = new Binary64(-1);
        Assertions.assertTrue(and.g(s).getReal() < 0, "negative");
        a.g = new Binary64(-1);
        b.g = new Binary64(0);
        Assertions.assertTrue(and.g(s).getReal() < 0, "negative");
        a.g = new Binary64(-1);
        b.g = new Binary64(1);
        Assertions.assertTrue(and.g(s).getReal() < 0, "negative");
        a.g = new Binary64(-1);
        b.g = new Binary64(-1);
        Assertions.assertTrue(and.g(s).getReal() < 0, "negative");

        // test both positive
        a.g = new Binary64(1);
        b.g = new Binary64(1);
        Assertions.assertTrue(and.g(s).getReal() > 0, "positive");

    }

    /**
     * check {@link BooleanDetector} for cancellation.
     */
    @Test
    public void testCancellation() {
        a.g = new Binary64(-1e-10);
        b.g = new Binary64(1e10);
        Assertions.assertTrue(and.g(s).getReal() < 0, "negative");
        a.g = new Binary64(1e10);
        b.g = new Binary64(-1e-10);
        Assertions.assertTrue(and.g(s).getReal() < 0, "negative");
        a.g = new Binary64(1e10);
        b.g = new Binary64(1e-10);
        Assertions.assertTrue(and.g(s).getReal() > 0, "positive");
        a.g = new Binary64(1e-10);
        b.g = new Binary64(1e10);
        Assertions.assertTrue(and.g(s).getReal() > 0, "positive");
    }

    /**
     * Check wrapped detectors are initialized.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testInit() {
        // setup
        FieldEventDetector<Binary64> a = Mockito.mock(FieldEventDetector.class);
        Mockito.when(a.getMaxCheckInterval()).thenReturn(s -> AbstractDetector.DEFAULT_MAXCHECK);
        Mockito.when(a.getThreshold()).thenReturn(new Binary64(AbstractDetector.DEFAULT_THRESHOLD));
        FieldEventDetector<Binary64> b = Mockito.mock(FieldEventDetector.class);
        Mockito.when(b.getMaxCheckInterval()).thenReturn(s -> AbstractDetector.DEFAULT_MAXCHECK);
        Mockito.when(b.getThreshold()).thenReturn(new Binary64(AbstractDetector.DEFAULT_THRESHOLD));
        FieldEventHandler<Binary64> c = Mockito.mock(FieldEventHandler.class);
        FieldBooleanDetector<Binary64> and = FieldBooleanDetector.andCombine(a, b).withHandler(c);
        FieldAbsoluteDate<Binary64> t = FieldAbsoluteDate.getCCSDSEpoch(Binary64Field.getInstance());
        s = Mockito.mock(FieldSpacecraftState.class);
        Mockito.when(s.getDate()).thenReturn(t.shiftedBy(60.0));

        // action
        and.init(s, t);

        // verify
        Assertions.assertEquals(2, and.getDetectors().size());
        Mockito.verify(a).init(s, t);
        Mockito.verify(b).init(s, t);
        Mockito.verify(c).init(s, t, and);
    }

    /** check when no operands are passed to the constructor. */
    @Test
    public void testZeroDetectors() {
        // action
        try {
            BooleanDetector.andCombine(Collections.emptyList());
            Assertions.fail("Expected Exception");
        } catch (NoSuchElementException e) {
            // expected
        }
    }

    /** Mock detector to set the g function to arbitrary values. */
    private static class MockDetector implements FieldEventDetector<Binary64> {

        /** value to return from {@link #g(SpacecraftState)}. */
        public Binary64 g = new Binary64(0);

        @Override
        public void init(FieldSpacecraftState<Binary64> s0, FieldAbsoluteDate<Binary64> t) {

        }

        @Override
        public Binary64 g(FieldSpacecraftState<Binary64> s) {
            return this.g;
        }

        @Override
        public Binary64 getThreshold() {
            return new Binary64(AbstractDetector.DEFAULT_THRESHOLD);
        }

        @Override
        public FieldAdaptableInterval<Binary64> getMaxCheckInterval() {
            return s -> AbstractDetector.DEFAULT_MAXCHECK;
        }

        @Override
        public int getMaxIterationCount() {
            return 0;
        }

        @Override
        public FieldEventHandler<Binary64> getHandler() {
            return (state, detector, increasing) -> null;
        }
    }
}
