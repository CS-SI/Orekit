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
package org.orekit.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeSpanMap.Span;
import org.orekit.utils.TimeSpanMap.Transition;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class TimeSpanMapTest {

    @Test
    void testSingleEntry() {
        String single = "single";
        TimeSpanMap<String> map = new TimeSpanMap<>(single);
        checkCountConsistency(map);
        assertSame(single, map.get(AbsoluteDate.CCSDS_EPOCH));
        assertSame(single, map.get(AbsoluteDate.FIFTIES_EPOCH));
        assertSame(single, map.get(AbsoluteDate.FUTURE_INFINITY));
        assertSame(single, map.get(AbsoluteDate.GALILEO_EPOCH));
        assertSame(single, map.get(AbsoluteDate.GPS_EPOCH));
        assertSame(single, map.get(AbsoluteDate.J2000_EPOCH));
        assertSame(single, map.get(AbsoluteDate.JAVA_EPOCH));
        assertSame(single, map.get(AbsoluteDate.JULIAN_EPOCH));
        assertSame(single, map.get(AbsoluteDate.MODIFIED_JULIAN_EPOCH));
        assertSame(single, map.get(AbsoluteDate.PAST_INFINITY));
        assertEquals(1, map.getSpansNumber());
        assertSame(single, map.getFirstNonNullSpan().getData());
        assertSame(single, map.getLastNonNullSpan().getData());
    }

    @Test
    void testForwardAdd() {
        final AbsoluteDate ref = AbsoluteDate.J2000_EPOCH;
        TimeSpanMap<Integer> map = new TimeSpanMap<>(Integer.valueOf(0));
        checkCountConsistency(map);
        for (int i = 1; i < 100; ++i) {
            Integer entry = Integer.valueOf(i);
            TimeSpanMap.Span<Integer> span = map.addValidAfter(entry, ref.shiftedBy(i), false);
            assertSame(entry, span.getData());
            checkCountConsistency(map);
        }
        assertEquals(0, map.get(ref.shiftedBy(-1000.0)).intValue());
        assertEquals(0, map.get(ref.shiftedBy( -100.0)).intValue());
        TimeSpanMap.Span<Integer> span = map.getSpan(ref.shiftedBy(-1000.0));
        assertEquals(0, span.getData().intValue());
        assertTrue(span.getStart().durationFrom(AbsoluteDate.J2000_EPOCH) < -Double.MAX_VALUE);
        assertEquals(1.0, span.getEnd().durationFrom(AbsoluteDate.J2000_EPOCH), 1.0e-15);
        for (int i = 0; i < 100; ++i) {
            assertEquals(i, map.get(ref.shiftedBy(i + 0.1)).intValue());
            assertEquals(i, map.get(ref.shiftedBy(i + 0.9)).intValue());
            span = map.getSpan(ref.shiftedBy(i + 0.1));
            assertEquals(i, span.getData().intValue());
            if (i == 0) {
                assertTrue(span.getStart().durationFrom(AbsoluteDate.J2000_EPOCH) < -Double.MAX_VALUE);
            } else {
                assertEquals(i, span.getStart().durationFrom(AbsoluteDate.J2000_EPOCH), 1.0e-15);
            } if (i == 99) {
                assertTrue(span.getEnd().durationFrom(AbsoluteDate.J2000_EPOCH) > Double.MAX_VALUE);
            } else {
                assertEquals(i + 1, span.getEnd().durationFrom(AbsoluteDate.J2000_EPOCH), 1.0e-15);
            }
        }
        assertEquals(99, map.get(ref.shiftedBy(  100.0)).intValue());
        assertEquals(99, map.get(ref.shiftedBy( 1000.0)).intValue());
        checkCountConsistency(map);
    }

    @Test
    void testBackwardAdd() {
        final AbsoluteDate ref = AbsoluteDate.J2000_EPOCH;
        TimeSpanMap<Integer> map = new TimeSpanMap<>(Integer.valueOf(0));
        checkCountConsistency(map);
        for (int i = -1; i > -100; --i) {
            Integer entry = Integer.valueOf(i);
            TimeSpanMap.Span<Integer> span = map.addValidBefore(entry, ref.shiftedBy(i), false);
            assertSame(entry, span.getData());
            checkCountConsistency(map);
        }
        assertEquals(0, map.get(ref.shiftedBy( 1000.0)).intValue());
        assertEquals(0, map.get(ref.shiftedBy(  100.0)).intValue());
        for (int i = 0; i > -100; --i) {
            assertEquals(i, map.get(ref.shiftedBy(i - 0.1)).intValue());
            assertEquals(i, map.get(ref.shiftedBy(i - 0.9)).intValue());
        }
        assertEquals(-99, map.get(ref.shiftedBy( -100.0)).intValue());
        assertEquals(-99, map.get(ref.shiftedBy(-1000.0)).intValue());
        checkCountConsistency(map);
    }

    @Test
    void testRandomAddNoErase() {
        final AbsoluteDate ref = AbsoluteDate.J2000_EPOCH;
        TimeSpanMap<Integer> map = new TimeSpanMap<>(Integer.valueOf(0));
        map.addValidAfter(Integer.valueOf(10), ref.shiftedBy(10.0), false);
        map.addValidAfter(Integer.valueOf( 3), ref.shiftedBy( 2.0), false);
        map.addValidAfter(Integer.valueOf( 9), ref.shiftedBy( 5.0), false);
        map.addValidBefore(Integer.valueOf( 2), ref.shiftedBy( 3.0), false);
        map.addValidBefore(Integer.valueOf( 5), ref.shiftedBy( 9.0), false);
        assertEquals( 0, map.get(ref.shiftedBy( -1.0)).intValue());
        assertEquals( 0, map.get(ref.shiftedBy(  1.9)).intValue());
        assertEquals( 2, map.get(ref.shiftedBy(  2.1)).intValue());
        assertEquals( 2, map.get(ref.shiftedBy(  2.9)).intValue());
        assertEquals( 3, map.get(ref.shiftedBy(  3.1)).intValue());
        assertEquals( 3, map.get(ref.shiftedBy(  4.9)).intValue());
        assertEquals( 5, map.get(ref.shiftedBy(  5.1)).intValue());
        assertEquals( 5, map.get(ref.shiftedBy(  8.9)).intValue());
        assertEquals( 9, map.get(ref.shiftedBy(  9.1)).intValue());
        assertEquals( 9, map.get(ref.shiftedBy(  9.9)).intValue());
        assertEquals(10, map.get(ref.shiftedBy( 10.1)).intValue());
        assertEquals(10, map.get(ref.shiftedBy(100.0)).intValue());
        final StringBuilder builder = new StringBuilder();
        map.forEach(i -> builder.append(' ').append(i));
        assertEquals(" 0 2 3 5 9 10", builder.toString());
        assertEquals(6, map.getSpansNumber());
        checkCountConsistency(map);
    }

    @Test
    void testRandomAddErase() {
        final AbsoluteDate ref = AbsoluteDate.J2000_EPOCH;
        TimeSpanMap<Integer> map = new TimeSpanMap<>(null);
        map.addValidAfter(Integer.valueOf(10), ref.shiftedBy(10.0), false);
        map.addValidAfter(Integer.valueOf( 3), ref.shiftedBy( 2.0), false);
        map.addValidBefore(Integer.valueOf( 5), ref.shiftedBy( 7.0), false);
        map.addValidAfter(null, ref.shiftedBy( 5.0), true);
        map.addValidAfter(Integer.valueOf( 1), ref.shiftedBy( 1.0), false);
        map.addValidBefore(null, ref.shiftedBy( 3.0), true);
        map.addValidBefore(Integer.valueOf( 7), ref.shiftedBy( 9.0), false);
        assertNull(map.get(ref.shiftedBy( -1.0)));
        assertNull(map.get(ref.shiftedBy(  1.9)));
        assertNull(map.get(ref.shiftedBy(  2.1)));
        assertNull(map.get(ref.shiftedBy(  2.9)));
        assertEquals( 5, map.get(ref.shiftedBy(  3.1)).intValue());
        assertEquals( 5, map.get(ref.shiftedBy(  4.9)).intValue());
        assertEquals( 7, map.get(ref.shiftedBy(  5.1)).intValue());
        assertEquals( 7, map.get(ref.shiftedBy(  6.9)).intValue());
        assertEquals( 7, map.get(ref.shiftedBy(  7.1)).intValue());
        assertEquals( 7, map.get(ref.shiftedBy(  8.9)).intValue());
        assertNull(map.get(ref.shiftedBy(  9.1)));
        assertNull(map.get(ref.shiftedBy(  9.9)));
        assertNull(map.get(ref.shiftedBy( 10.1)));
        assertNull(map.get(ref.shiftedBy(100.0)));
        final StringBuilder builder = new StringBuilder();
        map.forEach(i -> builder.append(' ').append(i));
        assertEquals(" 5 7", builder.toString());
        assertEquals(4, map.getSpansNumber());
        checkCountConsistency(map);
    }

    @Test
    void testAddBetweenEmpty() {
        TimeSpanMap<Integer> map = new TimeSpanMap<>(Integer.valueOf(0));
        map.addValidBetween(1, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-2), AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(+2));
        assertEquals(3, map.getSpansNumber());
        assertEquals(0, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-3)).intValue());
        assertEquals(1, map.get(AbsoluteDate.ARBITRARY_EPOCH).intValue());
        assertEquals(0, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(+3)).intValue());
        checkCountConsistency(map);
    }

    @Test
    void testAddBetweenBefore() {
        TimeSpanMap<Integer> map = new TimeSpanMap<>(Integer.valueOf(0));
        map.addValidBefore(1, AbsoluteDate.ARBITRARY_EPOCH, false);
        map.addValidBetween(7, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-4), AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-2));
        assertEquals(4, map.getSpansNumber());
        assertEquals(1, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-5)).intValue());
        assertEquals(7, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-3)).intValue());
        assertEquals(1, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-1)).intValue());
        assertEquals(0, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(+3)).intValue());
        checkCountConsistency(map);
    }

    @Test
    void testAddBetweenAfter() {
        TimeSpanMap<Integer> map = new TimeSpanMap<>(Integer.valueOf(0));
        map.addValidBefore(1, AbsoluteDate.ARBITRARY_EPOCH, false);
        map.addValidBetween(7, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(2), AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(4));
        assertEquals(4, map.getSpansNumber());
        assertEquals(1, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-3)).intValue());
        assertEquals(0, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy( 1)).intValue());
        assertEquals(7, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy( 3)).intValue());
        assertEquals(0, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy( 5)).intValue());
        checkCountConsistency(map);
    }

    @Test
    void testAddBetweenCoveringAll() {
        TimeSpanMap<Integer> map = new TimeSpanMap<>(Integer.valueOf(0));
        map.addValidAfter(1, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(1), false);
        map.addValidAfter(2, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(2), false);
        map.addValidAfter(3, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(3), false);
        map.addValidAfter(4, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(4), false);
        map.addValidAfter(5, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(5), false);
        assertEquals(6, map.getSpansNumber());
        map.addValidBetween(-1, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-1), AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(6));
        assertEquals( 3, map.getSpansNumber());
        assertEquals( 0, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-5)).intValue());
        assertEquals(-1, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy( 2)).intValue());
        assertEquals( 5, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(+7)).intValue());
        checkCountConsistency(map);
    }

    @Test
    void testAddBetweenCoveringSome() {
        TimeSpanMap<Integer> map = new TimeSpanMap<>(Integer.valueOf(0));
        map.addValidAfter(1, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(1), false);
        map.addValidAfter(2, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(2), false);
        map.addValidAfter(3, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(3), false);
        map.addValidAfter(4, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(4), false);
        map.addValidAfter(5, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(5), false);
        assertEquals(6, map.getSpansNumber());
        Integer entry = Integer.valueOf(-1);
        TimeSpanMap.Span<Integer> span = map.addValidBetween(entry, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(1.5), AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(4.5));
        assertSame(entry, span.getData());
        assertEquals(5, map.getSpansNumber());
        assertEquals( 0, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(0.75)).intValue());
        assertEquals( 1, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(1.25)).intValue());
        assertEquals(-1, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(1.75)).intValue());
        assertEquals(-1, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(4.25)).intValue());
        assertEquals( 4, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(4.75)).intValue());
        assertEquals( 5, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(5.25)).intValue());
        checkCountConsistency(map);
    }

    @Test
    void testAddBetweenSplittingOneSpanOnly() {
        TimeSpanMap<Integer> map = new TimeSpanMap<>(Integer.valueOf(0));
        map.addValidAfter(1, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(1), false);
        map.addValidAfter(2, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(2), false);
        map.addValidAfter(3, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(3), false);
        map.addValidAfter(4, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(4), false);
        map.addValidAfter(5, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(5), false);
        assertEquals(6, map.getSpansNumber());
        map.addValidBetween(-1, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(2.25), AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(2.75));
        assertEquals(8, map.getSpansNumber());
        assertEquals( 0, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(0.75)).intValue());
        assertEquals( 1, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(1.99)).intValue());
        assertEquals( 2, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(2.01)).intValue());
        assertEquals(-1, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(2.50)).intValue());
        assertEquals( 2, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(2.99)).intValue());
        assertEquals( 3, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(3.01)).intValue());
        assertEquals( 4, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(4.25)).intValue());
        assertEquals( 5, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(5.25)).intValue());
        checkCountConsistency(map);
    }

    @Test
    void testAddBetweenExistingDates() {
        TimeSpanMap<Integer> map = new TimeSpanMap<>(Integer.valueOf(0));
        map.addValidAfter(1, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(1), false);
        map.addValidAfter(2, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(2), false);
        map.addValidAfter(3, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(3), false);
        map.addValidAfter(4, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(4), false);
        map.addValidAfter(5, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(5), false);
        assertEquals(6, map.getSpansNumber());
        map.addValidBetween(-1, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(2), AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(4));
        assertEquals(5, map.getSpansNumber());
        assertEquals( 0, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(0.99)).intValue());
        assertEquals( 1, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(1.01)).intValue());
        assertEquals( 1, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(1.99)).intValue());
        assertEquals(-1, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(2.01)).intValue());
        assertEquals(-1, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(3.99)).intValue());
        assertEquals( 4, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(4.01)).intValue());
        assertEquals( 4, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(4.99)).intValue());
        assertEquals( 5, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(5.01)).intValue());
        checkCountConsistency(map);
    }

    @Test
    void testExtractRangeInfinity() {
        final AbsoluteDate ref = AbsoluteDate.J2000_EPOCH;
        TimeSpanMap<Integer> map = new TimeSpanMap<>(Integer.valueOf(0));
        map.addValidAfter(Integer.valueOf(10), ref.shiftedBy(10.0), false);
        map.addValidAfter(Integer.valueOf( 3), ref.shiftedBy( 2.0), false);
        map.addValidAfter(Integer.valueOf( 9), ref.shiftedBy( 5.0), false);
        map.addValidBefore(Integer.valueOf( 2), ref.shiftedBy( 3.0), false);
        map.addValidBefore(Integer.valueOf( 5), ref.shiftedBy( 9.0), false);
        TimeSpanMap<Integer> range = map.extractRange(AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY);
        assertEquals(map.getSpansNumber(), range.getSpansNumber());
        checkCountConsistency(map);
    }

    @Test
    void testExtractRangeSingleEntry() {
        final AbsoluteDate ref = AbsoluteDate.J2000_EPOCH;
        TimeSpanMap<Integer> map = new TimeSpanMap<>(Integer.valueOf(0));
        map.addValidAfter(Integer.valueOf(10), ref.shiftedBy(10.0), false);
        map.addValidAfter(Integer.valueOf( 3), ref.shiftedBy( 2.0), false);
        map.addValidAfter(Integer.valueOf( 9), ref.shiftedBy( 5.0), false);
        map.addValidBefore(Integer.valueOf( 2), ref.shiftedBy( 3.0), false);
        map.addValidBefore(Integer.valueOf( 5), ref.shiftedBy( 9.0), false);
        TimeSpanMap<Integer> range = map.extractRange(ref.shiftedBy(6), ref.shiftedBy(8));
        assertEquals(1, range.getSpansNumber());
        assertEquals(5, range.get(ref.shiftedBy(-10000)).intValue());
        assertEquals(5, range.get(ref.shiftedBy(+10000)).intValue());
        checkCountConsistency(map);
    }

    @Test
    void testExtractFromPastInfinity() {
        final AbsoluteDate ref = AbsoluteDate.J2000_EPOCH;
        TimeSpanMap<Integer> map = new TimeSpanMap<>(Integer.valueOf(0));
        map.addValidAfter(Integer.valueOf(10), ref.shiftedBy(10.0), false);
        map.addValidAfter(Integer.valueOf( 3), ref.shiftedBy( 2.0), false);
        map.addValidAfter(Integer.valueOf( 9), ref.shiftedBy( 5.0), false);
        map.addValidBefore(Integer.valueOf( 2), ref.shiftedBy( 3.0), false);
        map.addValidBefore(Integer.valueOf( 5), ref.shiftedBy( 9.0), false);
        TimeSpanMap<Integer> range = map.extractRange(AbsoluteDate.PAST_INFINITY, ref.shiftedBy(8));
        assertEquals(4, range.getSpansNumber());
        assertEquals( 0, range.get(ref.shiftedBy( -1.0)).intValue());
        assertEquals( 0, range.get(ref.shiftedBy(  1.9)).intValue());
        assertEquals( 2, range.get(ref.shiftedBy(  2.1)).intValue());
        assertEquals( 2, range.get(ref.shiftedBy(  2.9)).intValue());
        assertEquals( 3, range.get(ref.shiftedBy(  3.1)).intValue());
        assertEquals( 3, range.get(ref.shiftedBy(  4.9)).intValue());
        assertEquals( 5, range.get(ref.shiftedBy(  5.1)).intValue());
        assertEquals( 5, range.get(ref.shiftedBy(  8.9)).intValue());
        assertEquals( 5, range.get(ref.shiftedBy(  9.1)).intValue());
        assertEquals( 5, range.get(ref.shiftedBy( 99.0)).intValue());
        checkCountConsistency(map);
    }

    @Test
    void testExtractToFutureInfinity() {
        final AbsoluteDate ref = AbsoluteDate.J2000_EPOCH;
        TimeSpanMap<Integer> map = new TimeSpanMap<>(Integer.valueOf(0));
        map.addValidAfter(Integer.valueOf(10), ref.shiftedBy(10.0), false);
        map.addValidAfter(Integer.valueOf( 3), ref.shiftedBy( 2.0), false);
        map.addValidAfter(Integer.valueOf( 9), ref.shiftedBy( 5.0), false);
        map.addValidBefore(Integer.valueOf( 2), ref.shiftedBy( 3.0), false);
        map.addValidBefore(Integer.valueOf( 5), ref.shiftedBy( 9.0), false);
        TimeSpanMap<Integer> range = map.extractRange(ref.shiftedBy(4), AbsoluteDate.FUTURE_INFINITY);
        assertEquals(4, range.getSpansNumber());
        assertEquals( 3, range.get(ref.shiftedBy(-99.0)).intValue());
        assertEquals( 3, range.get(ref.shiftedBy(  4.9)).intValue());
        assertEquals( 5, range.get(ref.shiftedBy(  5.1)).intValue());
        assertEquals( 5, range.get(ref.shiftedBy(  8.9)).intValue());
        assertEquals( 9, range.get(ref.shiftedBy(  9.1)).intValue());
        assertEquals( 9, range.get(ref.shiftedBy(  9.9)).intValue());
        assertEquals(10, range.get(ref.shiftedBy( 10.1)).intValue());
        assertEquals(10, range.get(ref.shiftedBy(100.0)).intValue());
        checkCountConsistency(map);
    }

    @Test
    void testExtractIntermediate() {
        final AbsoluteDate ref = AbsoluteDate.J2000_EPOCH;
        TimeSpanMap<Integer> map = new TimeSpanMap<>(Integer.valueOf(0));
        map.addValidAfter(Integer.valueOf(10), ref.shiftedBy(10.0), false);
        map.addValidAfter(Integer.valueOf( 3), ref.shiftedBy( 2.0), false);
        map.addValidAfter(Integer.valueOf( 9), ref.shiftedBy( 5.0), false);
        map.addValidBefore(Integer.valueOf( 2), ref.shiftedBy( 3.0), false);
        map.addValidBefore(Integer.valueOf( 5), ref.shiftedBy( 9.0), false);
        TimeSpanMap<Integer> range = map.extractRange(ref.shiftedBy(4), ref.shiftedBy(8));
        assertEquals(2, range.getSpansNumber());
        assertEquals( 3, range.get(ref.shiftedBy(-99.0)).intValue());
        assertEquals( 3, range.get(ref.shiftedBy(  4.9)).intValue());
        assertEquals( 5, range.get(ref.shiftedBy(  5.1)).intValue());
        assertEquals( 5, range.get(ref.shiftedBy(  8.9)).intValue());
        assertEquals( 5, range.get(ref.shiftedBy(  9.1)).intValue());
        assertEquals( 5, range.get(ref.shiftedBy(999.9)).intValue());
        checkCountConsistency(map);
    }

    @Test
    void testSpanToTransitionLinkEmpty() {
        TimeSpanMap.Span<Integer> span = new TimeSpanMap<>(1).getSpan(AbsoluteDate.ARBITRARY_EPOCH);
        assertEquals(1, span.getData().intValue());
        assertSame(AbsoluteDate.PAST_INFINITY, span.getStart());
        assertNull(span.getStartTransition());
        assertSame(AbsoluteDate.FUTURE_INFINITY, span.getEnd());
        assertNull(span.getEndTransition());
    }

    @Test
    void testSpanToTransitionLink() {
        final AbsoluteDate ref = AbsoluteDate.ARBITRARY_EPOCH;
        TimeSpanMap<Integer> map = new TimeSpanMap<>(Integer.valueOf(0));
        map.addValidAfter(Integer.valueOf(10), ref.shiftedBy(10.0), false);
        map.addValidAfter(Integer.valueOf( 3), ref.shiftedBy( 2.0), false);
        map.addValidAfter(Integer.valueOf( 9), ref.shiftedBy( 5.0), false);
        map.addValidBefore(Integer.valueOf( 2), ref.shiftedBy( 3.0), false);
        map.addValidBefore(Integer.valueOf( 5), ref.shiftedBy( 9.0), false);

        TimeSpanMap.Span<Integer> first = map.getSpan(ref.shiftedBy(-99.0));
        assertEquals(0, first.getData().intValue());
        assertSame(AbsoluteDate.PAST_INFINITY, first.getStart());
        assertNull(first.getStartTransition());
        assertEquals(2.0, first.getEnd().durationFrom(ref), 1.0e-15);
        assertNotNull(first.getEndTransition());

        TimeSpanMap.Span<Integer> middle = map.getSpan(ref.shiftedBy(6.0));
        assertEquals(5, middle.getData().intValue());
        assertEquals(5.0, middle.getStart().durationFrom(ref), 1.0e-15);
        assertNotNull(middle.getStartTransition());
        assertEquals(9.0, middle.getEnd().durationFrom(ref), 1.0e-15);
        assertNotNull(middle.getEndTransition());
        assertSame(middle.getStartTransition().getAfter(), middle.getEndTransition().getBefore());
        assertEquals(3, middle.getStartTransition().getBefore().intValue());
        assertEquals(5, middle.getStartTransition().getAfter().intValue());
        assertEquals(5, middle.getEndTransition().getBefore().intValue());
        assertEquals(9, middle.getEndTransition().getAfter().intValue());

        TimeSpanMap.Span<Integer> last = map.getSpan(ref.shiftedBy(+99.0));
        assertEquals(10, last.getData().intValue());
        assertEquals(10.0, last.getStart().durationFrom(ref), 1.0e-15);
        assertNotNull(last.getStartTransition());
        assertSame(AbsoluteDate.FUTURE_INFINITY, last.getEnd());
        assertNull(last.getEndTransition());

        checkCountConsistency(map);

    }

    @Test
    void testTransitionToSpanLink() {
        final AbsoluteDate ref = AbsoluteDate.ARBITRARY_EPOCH;
        TimeSpanMap<Integer> map = new TimeSpanMap<>(Integer.valueOf(0));
        map.addValidAfter(Integer.valueOf(10), ref.shiftedBy(10.0), false);
        map.addValidAfter(Integer.valueOf( 3), ref.shiftedBy( 2.0), false);
        map.addValidAfter(Integer.valueOf( 9), ref.shiftedBy( 5.0), false);
        map.addValidBefore(Integer.valueOf( 2), ref.shiftedBy( 3.0), false);
        map.addValidBefore(Integer.valueOf( 5), ref.shiftedBy( 9.0), false);

        TimeSpanMap.Transition<Integer> first = map.getSpan(ref.shiftedBy(-99.0)).getEndTransition();
        assertEquals(2.0, first.getDate().durationFrom(ref), 1.0e-15);
        assertEquals(0, first.getBefore().intValue());
        assertEquals(2, first.getAfter().intValue());

        TimeSpanMap.Transition<Integer> middle = map.getSpan(ref.shiftedBy(6.0)).getStartTransition();
        assertEquals( 5.0, middle.getDate().durationFrom(ref), 1.0e-15);
        assertEquals( 3, middle.getBefore().intValue());
        assertEquals( 5, middle.getAfter().intValue());

        TimeSpanMap.Transition<Integer> last = map.getSpan(ref.shiftedBy(+99.0)).getStartTransition();
        assertEquals(10.0, last.getDate().durationFrom(ref), 1.0e-15);
        assertEquals( 9, last.getBefore().intValue());
        assertEquals(10, last.getAfter().intValue());

        checkCountConsistency(map);

    }

    @Test
    void tesFirstLastEmpty() {
        TimeSpanMap<Integer> map = new TimeSpanMap<>(Integer.valueOf(0));
        assertNull(map.getFirstTransition());
        assertNull(map.getLastTransition());
        assertSame(map.getFirstSpan(), map.getLastSpan());
        assertNull(map.getFirstSpan().getStartTransition());
        assertNull(map.getFirstSpan().getEndTransition());
        assertNull(map.getFirstSpan().previous());
        assertNull(map.getLastSpan().next());
        checkCountConsistency(map);
    }

    @Test
    void testSpansNavigation() {
        final AbsoluteDate ref = AbsoluteDate.ARBITRARY_EPOCH;
        TimeSpanMap<Integer> map = new TimeSpanMap<>(Integer.valueOf(0));
        map.addValidAfter(Integer.valueOf(10), ref.shiftedBy(10.0), false);
        map.addValidAfter(Integer.valueOf( 3), ref.shiftedBy( 2.0), false);
        map.addValidAfter(Integer.valueOf( 9), ref.shiftedBy( 5.0), false);
        map.addValidBefore(Integer.valueOf( 2), ref.shiftedBy( 3.0), false);
        map.addValidBefore(Integer.valueOf( 5), ref.shiftedBy( 9.0), false);
        assertNull(map.getFirstSpan().previous());
        assertNull(map.getLastSpan().next());

        TimeSpanMap.Span<Integer> span = map.getFirstSpan();
        assertEquals(0, span.getData().intValue());
        span = span.next();
        assertEquals(2, span.getData().intValue());
        span = span.next();
        assertEquals(3, span.getData().intValue());
        span = span.next();
        assertEquals(5, span.getData().intValue());
        span = span.next();
        assertEquals(9, span.getData().intValue());
        span = span.next();
        assertEquals(10, span.getData().intValue());
        assertNull(span.next());
        span = span.previous();
        assertEquals(9, span.getData().intValue());
        span = span.previous();
        assertEquals(5, span.getData().intValue());
        span = span.previous();
        assertEquals(3, span.getData().intValue());
        span = span.previous();
        assertEquals(2, span.getData().intValue());
        span = span.previous();
        assertEquals(0, span.getData().intValue());
        assertNull(span.previous());

        checkCountConsistency(map);

    }

    @Test
    void testTransitionsNavigation() {
        final AbsoluteDate ref = AbsoluteDate.ARBITRARY_EPOCH;
        TimeSpanMap<Integer> map = new TimeSpanMap<>(Integer.valueOf(0));
        map.addValidAfter(Integer.valueOf(10), ref.shiftedBy(10.0), false);
        map.addValidAfter(Integer.valueOf( 3), ref.shiftedBy( 2.0), false);
        map.addValidAfter(Integer.valueOf( 9), ref.shiftedBy( 5.0), false);
        map.addValidBefore(Integer.valueOf( 2), ref.shiftedBy( 3.0), false);
        map.addValidBefore(Integer.valueOf( 5), ref.shiftedBy( 9.0), false);

        assertEquals( 2.0, map.getFirstTransition().getDate().durationFrom(ref), 1.0e-15);
        assertEquals(10.0, map.getLastTransition().getDate().durationFrom(ref), 1.0e-15);

        Transition<Integer> transition = map.getLastTransition();
        assertEquals(10.0, transition.getDate().durationFrom(ref), 1.0e-15);
        transition = transition.previous();
        assertEquals( 9.0, transition.getDate().durationFrom(ref), 1.0e-15);
        transition = transition.previous();
        assertEquals( 5.0, transition.getDate().durationFrom(ref), 1.0e-15);
        transition = transition.previous();
        assertEquals( 3.0, transition.getDate().durationFrom(ref), 1.0e-15);
        transition = transition.previous();
        assertEquals( 2.0, transition.getDate().durationFrom(ref), 1.0e-15);
        assertNull(transition.previous());
        transition = transition.next();
        assertEquals( 3.0, transition.getDate().durationFrom(ref), 1.0e-15);
        transition = transition.next();
        assertEquals( 5.0, transition.getDate().durationFrom(ref), 1.0e-15);
        transition = transition.next();
        assertEquals( 9.0, transition.getDate().durationFrom(ref), 1.0e-15);
        transition = transition.next();
        assertEquals(10.0, transition.getDate().durationFrom(ref), 1.0e-15);
        assertNull(transition.next());

        checkCountConsistency(map);

    }

    @Test
    void testDuplicatedBeforeAfterAtEnd() {
        TimeSpanMap<Integer> map = new TimeSpanMap<>(null);
        map.addValidBefore(-1, AbsoluteDate.ARBITRARY_EPOCH, false);
        map.addValidAfter(+1, AbsoluteDate.ARBITRARY_EPOCH, false);
        assertEquals(2, map.getSpansNumber());
        assertEquals(-1, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-1)).intValue());
        assertEquals(+1, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(+1)).intValue());
        checkCountConsistency(map);
    }

    @Test
    void testDuplicatedBeforeAfterMiddle() {
        TimeSpanMap<Integer> map = new TimeSpanMap<>(null);
        map.addValidBefore(-2, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-2), false);
        map.addValidAfter(+2, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(+2), false);
        map.addValidBefore(-1, AbsoluteDate.ARBITRARY_EPOCH, false);
        map.addValidAfter(+1, AbsoluteDate.ARBITRARY_EPOCH, false);
        assertEquals(4, map.getSpansNumber());
        assertEquals(-1, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-1)).intValue());
        assertEquals(+1, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(+1)).intValue());
        checkCountConsistency(map);
    }

    @Test
    void testDuplicatedBeforeBefore() {
        TimeSpanMap<Integer> map = new TimeSpanMap<>(null);
        map.addValidBefore(-2, AbsoluteDate.ARBITRARY_EPOCH, false); // first call at ARBITRARY_EPOCH
        map.addValidAfter(0, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-2), false);
        map.addValidBefore(-1, AbsoluteDate.ARBITRARY_EPOCH, false); // second call at ARBITRARY_EPOCH
        assertEquals(3, map.getSpansNumber());
        assertEquals(-2, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-10)).intValue());
        assertEquals(-1, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-1)).intValue());
        assertNull(map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(+1)));
        checkCountConsistency(map);
    }

    @Test
    void testDuplicatedAfterBeforeAtEnd() {
        TimeSpanMap<Integer> map = new TimeSpanMap<>(null);
        map.addValidAfter(+1, AbsoluteDate.ARBITRARY_EPOCH, false);
        map.addValidBefore(-1, AbsoluteDate.ARBITRARY_EPOCH, false);
        assertEquals(2, map.getSpansNumber());
        assertEquals(-1, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-1)).intValue());
        assertEquals(+1, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(+1)).intValue());
        checkCountConsistency(map);
    }

    @Test
    void testDuplicatedAfterBeforeMiddle() {
        TimeSpanMap<Integer> map = new TimeSpanMap<>(null);
        map.addValidBefore(-2, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-2), false);
        map.addValidAfter(+2, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(+2), false);
        map.addValidAfter(+1, AbsoluteDate.ARBITRARY_EPOCH, false);
        map.addValidBefore(-1, AbsoluteDate.ARBITRARY_EPOCH, false);
        assertEquals(4, map.getSpansNumber());
        assertEquals(-1, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-1)).intValue());
        assertEquals(+1, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(+1)).intValue());
        checkCountConsistency(map);
    }

    @Test
    void testDuplicatedAfterAfter() {
        TimeSpanMap<Integer> map = new TimeSpanMap<>(null);
        map.addValidAfter(+2, AbsoluteDate.ARBITRARY_EPOCH, false); // first call at ARBITRARY_EPOCH
        map.addValidBefore(0, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(+2), false);
        map.addValidAfter(+1, AbsoluteDate.ARBITRARY_EPOCH, false); // second call at ARBITRARY_EPOCH
        assertEquals(3, map.getSpansNumber());
        assertNull(map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-1)));
        assertEquals(+1, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(+1)).intValue());
        assertEquals(+2, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(+10)).intValue());
        checkCountConsistency(map);
    }

    @Test
    void testValidAllTime() {
        AbsoluteDate ref = AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(1);
        TimeSpanMap<Integer> map = new TimeSpanMap<>(0);

        // action
        map.addValidAfter(1, ref, false);
        map.addValidBefore(2, ref, false);

        // verify
        assertEquals(1, (int) map.get(ref.shiftedBy(1)));
        assertEquals(2, (int) map.get(ref.shiftedBy(-1)));
        assertEquals(1, (int) map.get(ref));
    }

    @Test
    void testBetweenPastInfinity() {
        TimeSpanMap<Integer> map = new TimeSpanMap<>(null);
        assertEquals(1, map.getSpansNumber());
        map.addValidBetween(1, AbsoluteDate.PAST_INFINITY, AbsoluteDate.ARBITRARY_EPOCH);
        assertEquals(2, map.getSpansNumber());
        assertEquals(1, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-1)).intValue());
        assertNull(map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(+1)));
    }

    @Test
    void testBetweenFutureInfinity() {
        TimeSpanMap<Integer> map = new TimeSpanMap<>(null);
        assertEquals(1, map.getSpansNumber());
        map.addValidBetween(1, AbsoluteDate.ARBITRARY_EPOCH, AbsoluteDate.FUTURE_INFINITY);
        assertEquals(2, map.getSpansNumber());
        assertNull(map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-1)));
        assertEquals(1, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(+1)).intValue());
    }

    @Test
    void testBetweenBothInfinity() {
        TimeSpanMap<Integer> map = new TimeSpanMap<>(null);
        assertEquals(1, map.getSpansNumber());
        map.addValidBetween(1, AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY);
        assertEquals(1, map.getSpansNumber());
        assertEquals(1, map.get(AbsoluteDate.ARBITRARY_EPOCH).intValue());
    }

    @Test
    void testFirstNonNull() {
        final TimeSpanMap<Integer> map = new TimeSpanMap<>(null);
        checkException(map, TimeSpanMap::getFirstNonNullSpan, OrekitMessages.NO_CACHED_ENTRIES);
        for (double dt = 0; dt < 10; dt += 0.25) {
            map.addValidAfter(null, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(dt), false);
        }
        checkException(map, TimeSpanMap::getFirstNonNullSpan, OrekitMessages.NO_CACHED_ENTRIES);
        map.addValidAfter(22, map.getLastTransition().getDate().shiftedBy( 60.0), false);
        map.addValidAfter(17, map.getLastTransition().getDate().shiftedBy(-20.0), false);
        assertEquals(17, map.getFirstNonNullSpan().getData());
    }

    @Test
    void testLastNonNull() {
        final TimeSpanMap<Integer> map = new TimeSpanMap<>(null);
        checkException(map, TimeSpanMap::getLastNonNullSpan, OrekitMessages.NO_CACHED_ENTRIES);
        for (double dt = 0; dt < 10; dt += 0.25) {
            map.addValidBefore(null, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-dt), false);
        }
        checkException(map, TimeSpanMap::getLastNonNullSpan, OrekitMessages.NO_CACHED_ENTRIES);
        map.addValidBefore(22, map.getLastTransition().getDate().shiftedBy(-60.0), false);
        map.addValidBefore(17, map.getLastTransition().getDate().shiftedBy( 20.0), false);
        assertEquals(17, map.getLastNonNullSpan().getData());
    }

    private <T> void checkException(final TimeSpanMap<T> map,
                                    final Consumer<TimeSpanMap<T>> f,
                                    OrekitMessages expected) {
        try {
            f.accept(map);
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            assertEquals(expected, oe.getSpecifier());
        }
    }

    private <T> void checkCountConsistency(final TimeSpanMap<T> map) {
        final int count1 = map.getSpansNumber();
        int count2 = 0;
        for (Span<T> span = map.getFirstSpan(); span != null; span = span.next()) {
            ++count2;
        }
        assertEquals(count1, count2);
    }

    @BeforeEach
    void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
