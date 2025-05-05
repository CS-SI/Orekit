/* Copyright 2022-2025 Romain Serra
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
package org.orekit.time;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TimeIntervalTest {

    @Test
    void testOf() {
        // GIVEN
        final AbsoluteDate minDate = AbsoluteDate.ARBITRARY_EPOCH;
        final AbsoluteDate maxDate = minDate.shiftedBy(1);
        // WHEN
        final TimeInterval timeInterval = TimeInterval.of(maxDate, minDate);
        // THEN
        assertEquals(minDate, timeInterval.getStartDate());
        assertEquals(maxDate, timeInterval.getEndDate());
    }

    @Test
    void testContainsDate() {
        // GIVEN
        final AbsoluteDate minDate = AbsoluteDate.ARBITRARY_EPOCH;
        final AbsoluteDate maxDate = minDate.shiftedBy(1);
        final TimeInterval timeInterval = TimeInterval.of(maxDate, minDate);
        // WHEN & THEN
        assertFalse(timeInterval.contains(minDate.shiftedBy(-1)));
        assertTrue(timeInterval.contains(minDate));
        assertTrue(timeInterval.contains(minDate.shiftedBy(timeInterval.duration() / 2)));
        assertTrue(timeInterval.contains(maxDate));
        assertFalse(timeInterval.contains(maxDate.shiftedBy(2)));
    }

    @Test
    void testContainsInterval() {
        // GIVEN
        final AbsoluteDate minDate = AbsoluteDate.ARBITRARY_EPOCH;
        final AbsoluteDate maxDate = minDate.shiftedBy(1);
        final TimeInterval timeInterval = TimeInterval.of(maxDate, minDate);
        final TimeInterval allTimes = TimeInterval.of(AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY);
        // WHEN & THEN
        assertFalse(timeInterval.contains(TimeInterval.of(maxDate.shiftedBy(1), maxDate.shiftedBy(10))));
        assertFalse(timeInterval.contains(allTimes));
        assertFalse(timeInterval.contains(TimeInterval.of(AbsoluteDate.PAST_INFINITY, maxDate)));
        assertFalse(timeInterval.contains(TimeInterval.of(minDate, AbsoluteDate.FUTURE_INFINITY)));
        assertTrue(TimeInterval.of(AbsoluteDate.PAST_INFINITY, maxDate).contains(timeInterval));
        assertTrue(TimeInterval.of(minDate, AbsoluteDate.FUTURE_INFINITY).contains(timeInterval));
        assertTrue(allTimes.contains(timeInterval));
        assertTrue(timeInterval.contains(TimeInterval.of(minDate, minDate)));
        assertTrue(timeInterval.contains(TimeInterval.of(maxDate, maxDate)));
        assertTrue(timeInterval.contains(timeInterval));
        assertTrue(timeInterval.contains(TimeInterval.of(minDate.shiftedBy(0.5), maxDate.shiftedBy(-0.1))));
    }

    @Test
    void testIntersects() {
        // GIVEN
        final AbsoluteDate minDate = AbsoluteDate.ARBITRARY_EPOCH;
        final AbsoluteDate maxDate = minDate.shiftedBy(1);
        final TimeInterval timeInterval = TimeInterval.of(maxDate, minDate);
        final TimeInterval allTimes = TimeInterval.of(AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY);
        // WHEN & THEN
        assertFalse(timeInterval.intersects(TimeInterval.of(minDate.shiftedBy(-10), minDate.shiftedBy(-1))));
        assertFalse(timeInterval.intersects(TimeInterval.of(maxDate.shiftedBy(1), maxDate.shiftedBy(10))));
        assertTrue(timeInterval.intersects(allTimes));
        assertTrue(allTimes.intersects(timeInterval));
        assertTrue(timeInterval.intersects(timeInterval));
        assertTrue(timeInterval.intersects(TimeInterval.of(minDate, minDate)));
        assertTrue(timeInterval.intersects(TimeInterval.of(maxDate, maxDate)));
        assertTrue(timeInterval.intersects(TimeInterval.of(minDate.shiftedBy(0.1), maxDate.shiftedBy(-0.5))));
    }

    @Test
    void testDuration() {
        // GIVEN
        final AbsoluteDate minDate = AbsoluteDate.ARBITRARY_EPOCH;
        final double expectedDuration = 42;
        final AbsoluteDate maxDate = minDate.shiftedBy(expectedDuration);
        final TimeInterval timeInterval = TimeInterval.of(minDate, maxDate);
        // WHEN
        final double actualDuration = timeInterval.duration();
        // THEN
        assertEquals(expectedDuration, actualDuration);
    }
}
