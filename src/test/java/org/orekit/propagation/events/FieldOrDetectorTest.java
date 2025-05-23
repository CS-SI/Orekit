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
import org.orekit.propagation.events.intervals.FieldAdaptableInterval;
import org.orekit.time.FieldAbsoluteDate;

/**
 * Unit tests for {@link FieldBooleanDetector#orCombine(FieldEventDetector...)}.
 *
 * @author Evan Ward
 */
public class FieldOrDetectorTest {

    /** first operand. */
    private MockDetector a;
    /** second operand. */
    private MockDetector b;
    /** null (except for date) */
    private FieldSpacecraftState<Binary64> s;
    /** subject under test */
    private FieldBooleanDetector<Binary64> or;

    /** create subject under test and dependencies. */
    @SuppressWarnings("unchecked")
    @BeforeEach
    public void setUp() {
        a = new MockDetector();
        b = new MockDetector();
        s = Mockito.mock(FieldSpacecraftState.class);
        Mockito.when(s.getDate()).thenReturn(FieldAbsoluteDate.getArbitraryEpoch(Binary64Field.getInstance()));
        or = FieldBooleanDetector.orCombine(a, b);
    }

    /**
     * check {@link BooleanDetector#g(SpacecraftState)}.
     */
    @Test
    void testG() {
        // test zero cases
        a.g = b.g = new Binary64(0.0);
        Assertions.assertEquals(0.0, or.g(s).getReal(), 0);
        a.g = new Binary64(-1);
        b.g = new Binary64(0);
        Assertions.assertEquals(0.0, or.g(s).getReal(), 0);
        a.g = new Binary64(0);
        b.g = new Binary64(-1);
        Assertions.assertEquals(0.0, or.g(s).getReal(), 0);

        // test negative cases
        a.g = new Binary64(-1);
        b.g = new Binary64(-1);
        Assertions.assertTrue(or.g(s).getReal() < 0, "negative");

        // test positive cases
        a.g = new Binary64(0);
        b.g = new Binary64(1);
        Assertions.assertTrue(or.g(s).getReal() > 0, "positive");
        a.g = new Binary64(1);
        b.g = new Binary64(-1);
        Assertions.assertTrue(or.g(s).getReal() > 0, "positive");
        a.g = new Binary64(1);
        b.g = new Binary64(0);
        Assertions.assertTrue(or.g(s).getReal() > 0, "positive");
        a.g = new Binary64(-1);
        b.g = new Binary64(1);
        Assertions.assertTrue(or.g(s).getReal() > 0, "positive");
        a.g = new Binary64(1);
        b.g = new Binary64(1);
        Assertions.assertTrue(or.g(s).getReal() > 0, "positive");

    }

    /**
     * check when there is numeric cancellation between the two g values.
     */
    @Test
    void testCancellation() {
        a.g = new Binary64(-1e-10);
        b.g = new Binary64(-1e10);
        Assertions.assertTrue(or.g(s).getReal() < 0, "negative");
        a.g = new Binary64(-1e10);
        b.g = new Binary64(-1e-10);
        Assertions.assertTrue(or.g(s).getReal() < 0, "negative");
        a.g = new Binary64(-1e10);
        b.g = new Binary64(1e-10);
        Assertions.assertTrue(or.g(s).getReal() > 0, "positive");
        a.g = new Binary64(1e-10);
        b.g = new Binary64(-1e10);
        Assertions.assertTrue(or.g(s).getReal() > 0, "positive");
        a.g = new Binary64(1e10);
        b.g = new Binary64(-1e-10);
        Assertions.assertTrue(or.g(s).getReal() > 0, "positive");
        a.g = new Binary64(-1e-10);
        b.g = new Binary64(1e10);
        Assertions.assertTrue(or.g(s).getReal() > 0, "positive");
    }

    /**
     * Check wrapped detectors are initialized.
     */
    @SuppressWarnings("unchecked")
    @Test
    void testInit() {
        // setup
        FieldEventDetector<Binary64> a = Mockito.mock(FieldEventDetector.class);
        Mockito.when(a.getMaxCheckInterval()).thenReturn(FieldAdaptableInterval.of(AbstractDetector.DEFAULT_MAX_CHECK));
        Mockito.when(a.getThreshold()).thenReturn(new Binary64(AbstractDetector.DEFAULT_THRESHOLD));
        FieldEventDetector<Binary64> b = Mockito.mock(FieldEventDetector.class);
        Mockito.when(b.getMaxCheckInterval()).thenReturn(FieldAdaptableInterval.of(AbstractDetector.DEFAULT_MAX_CHECK));
        Mockito.when(b.getThreshold()).thenReturn(new Binary64(AbstractDetector.DEFAULT_THRESHOLD));
        FieldEventHandler<Binary64> c = Mockito.mock(FieldEventHandler.class);
        FieldBooleanDetector<Binary64> or = FieldBooleanDetector.orCombine(a, b).withHandler(c);
        FieldAbsoluteDate<Binary64> t = FieldAbsoluteDate.getCCSDSEpoch(Binary64Field.getInstance());
        s = Mockito.mock(FieldSpacecraftState.class);
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
    void testZeroDetectors() {
        // action
        try {
            BooleanDetector.orCombine(Collections.emptyList());
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
        public Binary64 g(FieldSpacecraftState<Binary64> s) {
            return this.g;
        }

        @Override
        public FieldEventDetectionSettings<Binary64> getDetectionSettings() {
            return new FieldEventDetectionSettings<>(Binary64Field.getInstance(), EventDetectionSettings.getDefaultEventDetectionSettings());
        }

        @Override
        public FieldEventHandler<Binary64> getHandler() {
            return (state, detector, increasing) -> null;
        }
    }
}
