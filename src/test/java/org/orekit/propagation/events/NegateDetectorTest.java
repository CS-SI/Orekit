/* Copyright 2002-2026 CS GROUP
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

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.functions.EventFunction;
import org.orekit.time.AbsoluteDate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link NegateDetector}.
 *
 * @author Evan Ward
 */
class NegateDetectorTest {

    @Test
    void testInitBackward() {
        //setup
        final DateDetector dateDetector = new DateDetector();
        final NegateDetector negateDetector = new NegateDetector(dateDetector);
        final SpacecraftState state = mock(SpacecraftState.class);
        when(state.getDate()).thenReturn(AbsoluteDate.ARBITRARY_EPOCH);

        //action
        negateDetector.init(state, AbsoluteDate.PAST_INFINITY);

        //verify
        Assertions.assertEquals(dateDetector.isForward(), negateDetector.isForward());
    }

    @Test
    void testInitForward() {
        //setup
        final DateDetector dateDetector = new DateDetector();
        final NegateDetector negateDetector = new NegateDetector(dateDetector);
        final SpacecraftState state = mock(SpacecraftState.class);
        when(state.getDate()).thenReturn(AbsoluteDate.ARBITRARY_EPOCH);

        //action
        negateDetector.init(state, AbsoluteDate.FUTURE_INFINITY);

        //verify
        Assertions.assertEquals(dateDetector.isForward(), negateDetector.isForward());
    }

    @Test
    void testGetDetector() {
        //setup
        EventDetector expectedDetector = new DateDetector();

        //action
        NegateDetector detector = new NegateDetector(expectedDetector);

        //verify
        Assertions.assertEquals(expectedDetector, detector.getDetector());
        Assertions.assertEquals(expectedDetector, detector.getOriginal());
    }

    @Test
    void testGetEventFunction() {
        // GIVEN
        final EventDetector mockedDetector = mock();
        final EventFunction eventFunction = mock();
        final SpacecraftState state = mock();
        final double originalG = 1.;
        when(eventFunction.value(state)).thenReturn(originalG);
        when(mockedDetector.getEventFunction()).thenReturn(eventFunction);
        when(mockedDetector.getDetectionSettings()).thenReturn(EventDetectionSettings.getDefaultEventDetectionSettings());
        // WHEN
        final NegateDetector detector = new NegateDetector(mockedDetector);
        // THEN
        Assertions.assertEquals(eventFunction, detector.getEventFunction().getBaseFunction());
        Assertions.assertEquals(-originalG, detector.g(state));
    }

    /** Check a with___ method. */
    @Test
    void testCreate() {
        //setup
        EventDetector a = mock(EventDetector.class);
        when(a.getDetectionSettings()).thenReturn(EventDetectionSettings.getDefaultEventDetectionSettings());
        NegateDetector detector = new NegateDetector(a);

        // action
        NegateDetector actual = detector.withMaxCheck(100);

        //verify
        MatcherAssert.assertThat(actual.getMaxCheckInterval().currentInterval(null, true), CoreMatchers.is(100.0));
        Assertions.assertSame(actual.getOriginal(), a);
    }
}
