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
package org.orekit.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeSpanMap.Span;
import org.orekit.utils.TimeSpanMap.Transition;

import java.util.function.Consumer;

public class TimeSpanMapTest {

    @Test
    public void testSingleEntry() {
        String single = "single";
        TimeSpanMap<String> map = new TimeSpanMap<>(single);
        checkCountConsistency(map);
        Assertions.assertSame(single, map.get(AbsoluteDate.CCSDS_EPOCH));
        Assertions.assertSame(single, map.get(AbsoluteDate.FIFTIES_EPOCH));
        Assertions.assertSame(single, map.get(AbsoluteDate.FUTURE_INFINITY));
        Assertions.assertSame(single, map.get(AbsoluteDate.GALILEO_EPOCH));
        Assertions.assertSame(single, map.get(AbsoluteDate.GPS_EPOCH));
        Assertions.assertSame(single, map.get(AbsoluteDate.J2000_EPOCH));
        Assertions.assertSame(single, map.get(AbsoluteDate.JAVA_EPOCH));
        Assertions.assertSame(single, map.get(AbsoluteDate.JULIAN_EPOCH));
        Assertions.assertSame(single, map.get(AbsoluteDate.MODIFIED_JULIAN_EPOCH));
        Assertions.assertSame(single, map.get(AbsoluteDate.PAST_INFINITY));
        Assertions.assertEquals(1, map.getSpansNumber());
        Assertions.assertSame(single, map.getFirstNonNullSpan().getData());
        Assertions.assertSame(single, map.getLastNonNullSpan().getData());
    }

    @Test
    public void testForwardAdd() {
        final AbsoluteDate ref = AbsoluteDate.J2000_EPOCH;
        TimeSpanMap<Integer> map = new TimeSpanMap<>(0);
        checkCountConsistency(map);
        for (int i = 1; i < 100; ++i) {
            Integer entry = i;
            TimeSpanMap.Span<Integer> span = map.addValidAfter(entry, ref.shiftedBy(i), false);
            Assertions.assertSame(entry, span.getData());
            checkCountConsistency(map);
        }
        Assertions.assertEquals(0, map.get(ref.shiftedBy(-1000.0)).intValue());
        Assertions.assertEquals(0, map.get(ref.shiftedBy( -100.0)).intValue());
        TimeSpanMap.Span<Integer> span = map.getSpan(ref.shiftedBy(-1000.0));
        Assertions.assertEquals(0, span.getData().intValue());
        Assertions.assertTrue(span.getStart().durationFrom(AbsoluteDate.J2000_EPOCH) < -Double.MAX_VALUE);
        Assertions.assertEquals(1.0, span.getEnd().durationFrom(AbsoluteDate.J2000_EPOCH), 1.0e-15);
        for (int i = 0; i < 100; ++i) {
            Assertions.assertEquals(i, map.get(ref.shiftedBy(i + 0.1)).intValue());
            Assertions.assertEquals(i, map.get(ref.shiftedBy(i + 0.9)).intValue());
            span = map.getSpan(ref.shiftedBy(i + 0.1));
            Assertions.assertEquals(i, span.getData().intValue());
            if (i == 0) {
                Assertions.assertTrue(span.getStart().durationFrom(AbsoluteDate.J2000_EPOCH) < -Double.MAX_VALUE);
            } else {
                Assertions.assertEquals(i, span.getStart().durationFrom(AbsoluteDate.J2000_EPOCH), 1.0e-15);
            } if (i == 99) {
                Assertions.assertTrue(span.getEnd().durationFrom(AbsoluteDate.J2000_EPOCH) > Double.MAX_VALUE);
            } else {
                Assertions.assertEquals(i + 1, span.getEnd().durationFrom(AbsoluteDate.J2000_EPOCH), 1.0e-15);
            }
        }
        Assertions.assertEquals(99, map.get(ref.shiftedBy(  100.0)).intValue());
        Assertions.assertEquals(99, map.get(ref.shiftedBy( 1000.0)).intValue());
        checkCountConsistency(map);
    }

    @Test
    public void testBackwardAdd() {
        final AbsoluteDate ref = AbsoluteDate.J2000_EPOCH;
        TimeSpanMap<Integer> map = new TimeSpanMap<>(0);
        checkCountConsistency(map);
        for (int i = -1; i > -100; --i) {
            Integer entry = i;
            TimeSpanMap.Span<Integer> span = map.addValidBefore(entry, ref.shiftedBy(i), false);
            Assertions.assertSame(entry, span.getData());
            checkCountConsistency(map);
        }
        Assertions.assertEquals(0, map.get(ref.shiftedBy( 1000.0)).intValue());
        Assertions.assertEquals(0, map.get(ref.shiftedBy(  100.0)).intValue());
        for (int i = 0; i > -100; --i) {
            Assertions.assertEquals(i, map.get(ref.shiftedBy(i - 0.1)).intValue());
            Assertions.assertEquals(i, map.get(ref.shiftedBy(i - 0.9)).intValue());
        }
        Assertions.assertEquals(-99, map.get(ref.shiftedBy( -100.0)).intValue());
        Assertions.assertEquals(-99, map.get(ref.shiftedBy(-1000.0)).intValue());
        checkCountConsistency(map);
    }

    @Test
    public void testRandomAddNoErase() {
        final AbsoluteDate ref = AbsoluteDate.J2000_EPOCH;
        TimeSpanMap<Integer> map = new TimeSpanMap<>(0);
        map.addValidAfter(10, ref.shiftedBy(10.0), false);
        map.addValidAfter( 3, ref.shiftedBy( 2.0), false);
        map.addValidAfter( 9, ref.shiftedBy( 5.0), false);
        map.addValidBefore( 2, ref.shiftedBy( 3.0), false);
        map.addValidBefore( 5, ref.shiftedBy( 9.0), false);
        Assertions.assertEquals( 0, map.get(ref.shiftedBy( -1.0)).intValue());
        Assertions.assertEquals( 0, map.get(ref.shiftedBy(  1.9)).intValue());
        Assertions.assertEquals( 2, map.get(ref.shiftedBy(  2.1)).intValue());
        Assertions.assertEquals( 2, map.get(ref.shiftedBy(  2.9)).intValue());
        Assertions.assertEquals( 3, map.get(ref.shiftedBy(  3.1)).intValue());
        Assertions.assertEquals( 3, map.get(ref.shiftedBy(  4.9)).intValue());
        Assertions.assertEquals( 5, map.get(ref.shiftedBy(  5.1)).intValue());
        Assertions.assertEquals( 5, map.get(ref.shiftedBy(  8.9)).intValue());
        Assertions.assertEquals( 9, map.get(ref.shiftedBy(  9.1)).intValue());
        Assertions.assertEquals( 9, map.get(ref.shiftedBy(  9.9)).intValue());
        Assertions.assertEquals(10, map.get(ref.shiftedBy( 10.1)).intValue());
        Assertions.assertEquals(10, map.get(ref.shiftedBy(100.0)).intValue());
        final StringBuilder builder = new StringBuilder();
        map.forEach(i -> builder.append(' ').append(i));
        Assertions.assertEquals(" 0 2 3 5 9 10", builder.toString());
        Assertions.assertEquals(6, map.getSpansNumber());
        checkCountConsistency(map);
    }

    @Test
    public void testRandomAddErase() {
        final AbsoluteDate ref = AbsoluteDate.J2000_EPOCH;
        TimeSpanMap<Integer> map = new TimeSpanMap<>(null);
        map.addValidAfter(10, ref.shiftedBy(10.0), false);
        map.addValidAfter( 3, ref.shiftedBy( 2.0), false);
        map.addValidBefore( 5, ref.shiftedBy( 7.0), false);
        map.addValidAfter(null, ref.shiftedBy( 5.0), true);
        map.addValidAfter( 1, ref.shiftedBy( 1.0), false);
        map.addValidBefore(null, ref.shiftedBy( 3.0), true);
        map.addValidBefore( 7, ref.shiftedBy( 9.0), false);
        Assertions.assertNull(map.get(ref.shiftedBy( -1.0)));
        Assertions.assertNull(map.get(ref.shiftedBy(  1.9)));
        Assertions.assertNull(map.get(ref.shiftedBy(  2.1)));
        Assertions.assertNull(map.get(ref.shiftedBy(  2.9)));
        Assertions.assertEquals( 5, map.get(ref.shiftedBy(  3.1)).intValue());
        Assertions.assertEquals( 5, map.get(ref.shiftedBy(  4.9)).intValue());
        Assertions.assertEquals( 7, map.get(ref.shiftedBy(  5.1)).intValue());
        Assertions.assertEquals( 7, map.get(ref.shiftedBy(  6.9)).intValue());
        Assertions.assertEquals( 7, map.get(ref.shiftedBy(  7.1)).intValue());
        Assertions.assertEquals( 7, map.get(ref.shiftedBy(  8.9)).intValue());
        Assertions.assertNull(map.get(ref.shiftedBy(  9.1)));
        Assertions.assertNull(map.get(ref.shiftedBy(  9.9)));
        Assertions.assertNull(map.get(ref.shiftedBy( 10.1)));
        Assertions.assertNull(map.get(ref.shiftedBy(100.0)));
        final StringBuilder builder = new StringBuilder();
        map.forEach(i -> builder.append(' ').append(i));
        Assertions.assertEquals(" 5 7", builder.toString());
        Assertions.assertEquals(4, map.getSpansNumber());
        checkCountConsistency(map);
    }

    @Test
    public void testAddBetweenEmpty() {
        TimeSpanMap<Integer> map = new TimeSpanMap<>(0);
        map.addValidBetween(1, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-2), AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(+2));
        Assertions.assertEquals(3, map.getSpansNumber());
        Assertions.assertEquals(0, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-3)).intValue());
        Assertions.assertEquals(1, map.get(AbsoluteDate.ARBITRARY_EPOCH).intValue());
        Assertions.assertEquals(0, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(+3)).intValue());
        checkCountConsistency(map);
    }

    @Test
    public void testAddBetweenBefore() {
        TimeSpanMap<Integer> map = new TimeSpanMap<>(0);
        map.addValidBefore(1, AbsoluteDate.ARBITRARY_EPOCH, false);
        map.addValidBetween(7, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-4), AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-2));
        Assertions.assertEquals(4, map.getSpansNumber());
        Assertions.assertEquals(1, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-5)).intValue());
        Assertions.assertEquals(7, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-3)).intValue());
        Assertions.assertEquals(1, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-1)).intValue());
        Assertions.assertEquals(0, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(+3)).intValue());
        checkCountConsistency(map);
    }

    @Test
    public void testAddBetweenAfter() {
        TimeSpanMap<Integer> map = new TimeSpanMap<>(0);
        map.addValidBefore(1, AbsoluteDate.ARBITRARY_EPOCH, false);
        map.addValidBetween(7, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(2), AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(4));
        Assertions.assertEquals(4, map.getSpansNumber());
        Assertions.assertEquals(1, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-3)).intValue());
        Assertions.assertEquals(0, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy( 1)).intValue());
        Assertions.assertEquals(7, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy( 3)).intValue());
        Assertions.assertEquals(0, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy( 5)).intValue());
        checkCountConsistency(map);
    }

    @Test
    public void testAddBetweenCoveringAll() {
        TimeSpanMap<Integer> map = new TimeSpanMap<>(0);
        map.addValidAfter(1, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(1), false);
        map.addValidAfter(2, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(2), false);
        map.addValidAfter(3, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(3), false);
        map.addValidAfter(4, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(4), false);
        map.addValidAfter(5, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(5), false);
        Assertions.assertEquals(6, map.getSpansNumber());
        map.addValidBetween(-1, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-1), AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(6));
        Assertions.assertEquals( 3, map.getSpansNumber());
        Assertions.assertEquals( 0, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-5)).intValue());
        Assertions.assertEquals(-1, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy( 2)).intValue());
        Assertions.assertEquals( 5, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(+7)).intValue());
        checkCountConsistency(map);
    }

    @Test
    public void testAddBetweenCoveringSome() {
        TimeSpanMap<Integer> map = new TimeSpanMap<>(0);
        map.addValidAfter(1, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(1), false);
        map.addValidAfter(2, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(2), false);
        map.addValidAfter(3, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(3), false);
        map.addValidAfter(4, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(4), false);
        map.addValidAfter(5, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(5), false);
        Assertions.assertEquals(6, map.getSpansNumber());
        Integer entry = -1;
        TimeSpanMap.Span<Integer> span = map.addValidBetween(entry, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(1.5), AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(4.5));
        Assertions.assertSame(entry, span.getData());
        Assertions.assertEquals(5, map.getSpansNumber());
        Assertions.assertEquals( 0, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(0.75)).intValue());
        Assertions.assertEquals( 1, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(1.25)).intValue());
        Assertions.assertEquals(-1, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(1.75)).intValue());
        Assertions.assertEquals(-1, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(4.25)).intValue());
        Assertions.assertEquals( 4, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(4.75)).intValue());
        Assertions.assertEquals( 5, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(5.25)).intValue());
        checkCountConsistency(map);
    }

    @Test
    public void testAddBetweenSplittingOneSpanOnly() {
        TimeSpanMap<Integer> map = new TimeSpanMap<>(0);
        map.addValidAfter(1, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(1), false);
        map.addValidAfter(2, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(2), false);
        map.addValidAfter(3, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(3), false);
        map.addValidAfter(4, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(4), false);
        map.addValidAfter(5, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(5), false);
        Assertions.assertEquals(6, map.getSpansNumber());
        map.addValidBetween(-1, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(2.25), AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(2.75));
        Assertions.assertEquals(8, map.getSpansNumber());
        Assertions.assertEquals( 0, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(0.75)).intValue());
        Assertions.assertEquals( 1, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(1.99)).intValue());
        Assertions.assertEquals( 2, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(2.01)).intValue());
        Assertions.assertEquals(-1, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(2.50)).intValue());
        Assertions.assertEquals( 2, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(2.99)).intValue());
        Assertions.assertEquals( 3, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(3.01)).intValue());
        Assertions.assertEquals( 4, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(4.25)).intValue());
        Assertions.assertEquals( 5, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(5.25)).intValue());
        checkCountConsistency(map);
    }

    @Test
    public void testAddBetweenExistingDates() {
        TimeSpanMap<Integer> map = new TimeSpanMap<>(0);
        map.addValidAfter(1, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(1), false);
        map.addValidAfter(2, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(2), false);
        map.addValidAfter(3, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(3), false);
        map.addValidAfter(4, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(4), false);
        map.addValidAfter(5, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(5), false);
        Assertions.assertEquals(6, map.getSpansNumber());
        map.addValidBetween(-1, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(2), AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(4));
        Assertions.assertEquals(5, map.getSpansNumber());
        Assertions.assertEquals( 0, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(0.99)).intValue());
        Assertions.assertEquals( 1, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(1.01)).intValue());
        Assertions.assertEquals( 1, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(1.99)).intValue());
        Assertions.assertEquals(-1, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(2.01)).intValue());
        Assertions.assertEquals(-1, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(3.99)).intValue());
        Assertions.assertEquals( 4, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(4.01)).intValue());
        Assertions.assertEquals( 4, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(4.99)).intValue());
        Assertions.assertEquals( 5, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(5.01)).intValue());
        checkCountConsistency(map);
    }

    @Test
    public void testExtractRangeInfinity() {
        final AbsoluteDate ref = AbsoluteDate.J2000_EPOCH;
        TimeSpanMap<Integer> map = new TimeSpanMap<>(0);
        map.addValidAfter(10, ref.shiftedBy(10.0), false);
        map.addValidAfter( 3, ref.shiftedBy( 2.0), false);
        map.addValidAfter( 9, ref.shiftedBy( 5.0), false);
        map.addValidBefore( 2, ref.shiftedBy( 3.0), false);
        map.addValidBefore( 5, ref.shiftedBy( 9.0), false);
        TimeSpanMap<Integer> range = map.extractRange(AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY);
        Assertions.assertEquals(map.getSpansNumber(), range.getSpansNumber());
        checkCountConsistency(map);
    }

    @Test
    public void testExtractRangeSingleEntry() {
        final AbsoluteDate ref = AbsoluteDate.J2000_EPOCH;
        TimeSpanMap<Integer> map = new TimeSpanMap<>(0);
        map.addValidAfter(10, ref.shiftedBy(10.0), false);
        map.addValidAfter( 3, ref.shiftedBy( 2.0), false);
        map.addValidAfter( 9, ref.shiftedBy( 5.0), false);
        map.addValidBefore( 2, ref.shiftedBy( 3.0), false);
        map.addValidBefore( 5, ref.shiftedBy( 9.0), false);
        TimeSpanMap<Integer> range = map.extractRange(ref.shiftedBy(6), ref.shiftedBy(8));
        Assertions.assertEquals(1, range.getSpansNumber());
        Assertions.assertEquals(5, range.get(ref.shiftedBy(-10000)).intValue());
        Assertions.assertEquals(5, range.get(ref.shiftedBy(+10000)).intValue());
        checkCountConsistency(map);
    }

    @Test
    public void testExtractFromPastInfinity() {
        final AbsoluteDate ref = AbsoluteDate.J2000_EPOCH;
        TimeSpanMap<Integer> map = new TimeSpanMap<>(0);
        map.addValidAfter(10, ref.shiftedBy(10.0), false);
        map.addValidAfter( 3, ref.shiftedBy( 2.0), false);
        map.addValidAfter( 9, ref.shiftedBy( 5.0), false);
        map.addValidBefore( 2, ref.shiftedBy( 3.0), false);
        map.addValidBefore( 5, ref.shiftedBy( 9.0), false);
        TimeSpanMap<Integer> range = map.extractRange(AbsoluteDate.PAST_INFINITY, ref.shiftedBy(8));
        Assertions.assertEquals(4, range.getSpansNumber());
        Assertions.assertEquals( 0, range.get(ref.shiftedBy( -1.0)).intValue());
        Assertions.assertEquals( 0, range.get(ref.shiftedBy(  1.9)).intValue());
        Assertions.assertEquals( 2, range.get(ref.shiftedBy(  2.1)).intValue());
        Assertions.assertEquals( 2, range.get(ref.shiftedBy(  2.9)).intValue());
        Assertions.assertEquals( 3, range.get(ref.shiftedBy(  3.1)).intValue());
        Assertions.assertEquals( 3, range.get(ref.shiftedBy(  4.9)).intValue());
        Assertions.assertEquals( 5, range.get(ref.shiftedBy(  5.1)).intValue());
        Assertions.assertEquals( 5, range.get(ref.shiftedBy(  8.9)).intValue());
        Assertions.assertEquals( 5, range.get(ref.shiftedBy(  9.1)).intValue());
        Assertions.assertEquals( 5, range.get(ref.shiftedBy( 99.0)).intValue());
        checkCountConsistency(map);
    }

    @Test
    public void testExtractToFutureInfinity() {
        final AbsoluteDate ref = AbsoluteDate.J2000_EPOCH;
        TimeSpanMap<Integer> map = new TimeSpanMap<>(0);
        map.addValidAfter(10, ref.shiftedBy(10.0), false);
        map.addValidAfter( 3, ref.shiftedBy( 2.0), false);
        map.addValidAfter( 9, ref.shiftedBy( 5.0), false);
        map.addValidBefore( 2, ref.shiftedBy( 3.0), false);
        map.addValidBefore( 5, ref.shiftedBy( 9.0), false);
        TimeSpanMap<Integer> range = map.extractRange(ref.shiftedBy(4), AbsoluteDate.FUTURE_INFINITY);
        Assertions.assertEquals(4, range.getSpansNumber());
        Assertions.assertEquals( 3, range.get(ref.shiftedBy(-99.0)).intValue());
        Assertions.assertEquals( 3, range.get(ref.shiftedBy(  4.9)).intValue());
        Assertions.assertEquals( 5, range.get(ref.shiftedBy(  5.1)).intValue());
        Assertions.assertEquals( 5, range.get(ref.shiftedBy(  8.9)).intValue());
        Assertions.assertEquals( 9, range.get(ref.shiftedBy(  9.1)).intValue());
        Assertions.assertEquals( 9, range.get(ref.shiftedBy(  9.9)).intValue());
        Assertions.assertEquals(10, range.get(ref.shiftedBy( 10.1)).intValue());
        Assertions.assertEquals(10, range.get(ref.shiftedBy(100.0)).intValue());
        checkCountConsistency(map);
    }

    @Test
    public void testExtractIntermediate() {
        final AbsoluteDate ref = AbsoluteDate.J2000_EPOCH;
        TimeSpanMap<Integer> map = new TimeSpanMap<>(0);
        map.addValidAfter(10, ref.shiftedBy(10.0), false);
        map.addValidAfter( 3, ref.shiftedBy( 2.0), false);
        map.addValidAfter( 9, ref.shiftedBy( 5.0), false);
        map.addValidBefore( 2, ref.shiftedBy( 3.0), false);
        map.addValidBefore( 5, ref.shiftedBy( 9.0), false);
        TimeSpanMap<Integer> range = map.extractRange(ref.shiftedBy(4), ref.shiftedBy(8));
        Assertions.assertEquals(2, range.getSpansNumber());
        Assertions.assertEquals( 3, range.get(ref.shiftedBy(-99.0)).intValue());
        Assertions.assertEquals( 3, range.get(ref.shiftedBy(  4.9)).intValue());
        Assertions.assertEquals( 5, range.get(ref.shiftedBy(  5.1)).intValue());
        Assertions.assertEquals( 5, range.get(ref.shiftedBy(  8.9)).intValue());
        Assertions.assertEquals( 5, range.get(ref.shiftedBy(  9.1)).intValue());
        Assertions.assertEquals( 5, range.get(ref.shiftedBy(999.9)).intValue());
        checkCountConsistency(map);
    }

    @Test
    public void testSpanToTransitionLinkEmpty() {
        TimeSpanMap.Span<Integer> span = new TimeSpanMap<>(1).getSpan(AbsoluteDate.ARBITRARY_EPOCH);
        Assertions.assertEquals(1, span.getData().intValue());
        Assertions.assertSame(AbsoluteDate.PAST_INFINITY, span.getStart());
        Assertions.assertNull(span.getStartTransition());
        Assertions.assertSame(AbsoluteDate.FUTURE_INFINITY, span.getEnd());
        Assertions.assertNull(span.getEndTransition());
    }

    @Test
    public void testSpanToTransitionLink() {
        final AbsoluteDate ref = AbsoluteDate.ARBITRARY_EPOCH;
        TimeSpanMap<Integer> map = new TimeSpanMap<>(0);
        map.addValidAfter(10, ref.shiftedBy(10.0), false);
        map.addValidAfter( 3, ref.shiftedBy( 2.0), false);
        map.addValidAfter( 9, ref.shiftedBy( 5.0), false);
        map.addValidBefore( 2, ref.shiftedBy( 3.0), false);
        map.addValidBefore( 5, ref.shiftedBy( 9.0), false);

        TimeSpanMap.Span<Integer> first = map.getSpan(ref.shiftedBy(-99.0));
        Assertions.assertEquals(0, first.getData().intValue());
        Assertions.assertSame(AbsoluteDate.PAST_INFINITY, first.getStart());
        Assertions.assertNull(first.getStartTransition());
        Assertions.assertEquals(2.0, first.getEnd().durationFrom(ref), 1.0e-15);
        Assertions.assertNotNull(first.getEndTransition());

        TimeSpanMap.Span<Integer> middle = map.getSpan(ref.shiftedBy(6.0));
        Assertions.assertEquals(5, middle.getData().intValue());
        Assertions.assertEquals(5.0, middle.getStart().durationFrom(ref), 1.0e-15);
        Assertions.assertNotNull(middle.getStartTransition());
        Assertions.assertEquals(9.0, middle.getEnd().durationFrom(ref), 1.0e-15);
        Assertions.assertNotNull(middle.getEndTransition());
        Assertions.assertSame(middle.getStartTransition().getAfter(), middle.getEndTransition().getBefore());
        Assertions.assertEquals(3, middle.getStartTransition().getBefore().intValue());
        Assertions.assertEquals(5, middle.getStartTransition().getAfter().intValue());
        Assertions.assertEquals(5, middle.getEndTransition().getBefore().intValue());
        Assertions.assertEquals(9, middle.getEndTransition().getAfter().intValue());

        TimeSpanMap.Span<Integer> last = map.getSpan(ref.shiftedBy(+99.0));
        Assertions.assertEquals(10, last.getData().intValue());
        Assertions.assertEquals(10.0, last.getStart().durationFrom(ref), 1.0e-15);
        Assertions.assertNotNull(last.getStartTransition());
        Assertions.assertSame(AbsoluteDate.FUTURE_INFINITY, last.getEnd());
        Assertions.assertNull(last.getEndTransition());

        checkCountConsistency(map);

    }

    @Test
    public void testTransitionToSpanLink() {
        final AbsoluteDate ref = AbsoluteDate.ARBITRARY_EPOCH;
        TimeSpanMap<Integer> map = new TimeSpanMap<>(0);
        map.addValidAfter(10, ref.shiftedBy(10.0), false);
        map.addValidAfter( 3, ref.shiftedBy( 2.0), false);
        map.addValidAfter( 9, ref.shiftedBy( 5.0), false);
        map.addValidBefore( 2, ref.shiftedBy( 3.0), false);
        map.addValidBefore( 5, ref.shiftedBy( 9.0), false);

        TimeSpanMap.Transition<Integer> first = map.getSpan(ref.shiftedBy(-99.0)).getEndTransition();
        Assertions.assertEquals(2.0, first.getDate().durationFrom(ref), 1.0e-15);
        Assertions.assertEquals(0, first.getBefore().intValue());
        Assertions.assertEquals(2, first.getAfter().intValue());

        TimeSpanMap.Transition<Integer> middle = map.getSpan(ref.shiftedBy(6.0)).getStartTransition();
        Assertions.assertEquals( 5.0, middle.getDate().durationFrom(ref), 1.0e-15);
        Assertions.assertEquals( 3, middle.getBefore().intValue());
        Assertions.assertEquals( 5, middle.getAfter().intValue());

        TimeSpanMap.Transition<Integer> last = map.getSpan(ref.shiftedBy(+99.0)).getStartTransition();
        Assertions.assertEquals(10.0, last.getDate().durationFrom(ref), 1.0e-15);
        Assertions.assertEquals( 9, last.getBefore().intValue());
        Assertions.assertEquals(10, last.getAfter().intValue());

        checkCountConsistency(map);

    }

    @Test
    public void tesFirstLastEmpty() {
        TimeSpanMap<Integer> map = new TimeSpanMap<>(0);
        Assertions.assertNull(map.getFirstTransition());
        Assertions.assertNull(map.getLastTransition());
        Assertions.assertSame(map.getFirstSpan(), map.getLastSpan());
        Assertions.assertNull(map.getFirstSpan().getStartTransition());
        Assertions.assertNull(map.getFirstSpan().getEndTransition());
        Assertions.assertNull(map.getFirstSpan().previous());
        Assertions.assertNull(map.getLastSpan().next());
        checkCountConsistency(map);
    }

    @Test
    public void testSpansNavigation() {
        final AbsoluteDate ref = AbsoluteDate.ARBITRARY_EPOCH;
        TimeSpanMap<Integer> map = new TimeSpanMap<>(0);
        map.addValidAfter(10, ref.shiftedBy(10.0), false);
        map.addValidAfter( 3, ref.shiftedBy( 2.0), false);
        map.addValidAfter( 9, ref.shiftedBy( 5.0), false);
        map.addValidBefore( 2, ref.shiftedBy( 3.0), false);
        map.addValidBefore( 5, ref.shiftedBy( 9.0), false);
        Assertions.assertNull(map.getFirstSpan().previous());
        Assertions.assertNull(map.getLastSpan().next());

        TimeSpanMap.Span<Integer> span = map.getFirstSpan();
        Assertions.assertEquals(0, span.getData().intValue());
        span = span.next();
        Assertions.assertEquals(2, span.getData().intValue());
        span = span.next();
        Assertions.assertEquals(3, span.getData().intValue());
        span = span.next();
        Assertions.assertEquals(5, span.getData().intValue());
        span = span.next();
        Assertions.assertEquals(9, span.getData().intValue());
        span = span.next();
        Assertions.assertEquals(10, span.getData().intValue());
        Assertions.assertNull(span.next());
        span = span.previous();
        Assertions.assertEquals(9, span.getData().intValue());
        span = span.previous();
        Assertions.assertEquals(5, span.getData().intValue());
        span = span.previous();
        Assertions.assertEquals(3, span.getData().intValue());
        span = span.previous();
        Assertions.assertEquals(2, span.getData().intValue());
        span = span.previous();
        Assertions.assertEquals(0, span.getData().intValue());
        Assertions.assertNull(span.previous());

        checkCountConsistency(map);

    }

    @Test
    public void testTransitionsNavigation() {
        final AbsoluteDate ref = AbsoluteDate.ARBITRARY_EPOCH;
        TimeSpanMap<Integer> map = new TimeSpanMap<>(0);
        map.addValidAfter(10, ref.shiftedBy(10.0), false);
        map.addValidAfter( 3, ref.shiftedBy( 2.0), false);
        map.addValidAfter( 9, ref.shiftedBy( 5.0), false);
        map.addValidBefore( 2, ref.shiftedBy( 3.0), false);
        map.addValidBefore( 5, ref.shiftedBy( 9.0), false);

        Assertions.assertEquals( 2.0, map.getFirstTransition().getDate().durationFrom(ref), 1.0e-15);
        Assertions.assertEquals(10.0, map.getLastTransition().getDate().durationFrom(ref), 1.0e-15);

        Transition<Integer> transition = map.getLastTransition();
        Assertions.assertEquals(10.0, transition.getDate().durationFrom(ref), 1.0e-15);
        transition = transition.previous();
        Assertions.assertEquals( 9.0, transition.getDate().durationFrom(ref), 1.0e-15);
        transition = transition.previous();
        Assertions.assertEquals( 5.0, transition.getDate().durationFrom(ref), 1.0e-15);
        transition = transition.previous();
        Assertions.assertEquals( 3.0, transition.getDate().durationFrom(ref), 1.0e-15);
        transition = transition.previous();
        Assertions.assertEquals( 2.0, transition.getDate().durationFrom(ref), 1.0e-15);
        Assertions.assertNull(transition.previous());
        transition = transition.next();
        Assertions.assertEquals( 3.0, transition.getDate().durationFrom(ref), 1.0e-15);
        transition = transition.next();
        Assertions.assertEquals( 5.0, transition.getDate().durationFrom(ref), 1.0e-15);
        transition = transition.next();
        Assertions.assertEquals( 9.0, transition.getDate().durationFrom(ref), 1.0e-15);
        transition = transition.next();
        Assertions.assertEquals(10.0, transition.getDate().durationFrom(ref), 1.0e-15);
        Assertions.assertNull(transition.next());

        checkCountConsistency(map);

    }

    @Test
    public void testDuplicatedBeforeAfterAtEnd() {
        TimeSpanMap<Integer> map = new TimeSpanMap<>(null);
        map.addValidBefore(-1, AbsoluteDate.ARBITRARY_EPOCH, false);
        map.addValidAfter(+1, AbsoluteDate.ARBITRARY_EPOCH, false);
        Assertions.assertEquals(2, map.getSpansNumber());
        Assertions.assertEquals(-1, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-1)).intValue());
        Assertions.assertEquals(+1, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(+1)).intValue());
        checkCountConsistency(map);
    }

    @Test
    public void testDuplicatedBeforeAfterMiddle() {
        TimeSpanMap<Integer> map = new TimeSpanMap<>(null);
        map.addValidBefore(-2, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-2), false);
        map.addValidAfter(+2, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(+2), false);
        map.addValidBefore(-1, AbsoluteDate.ARBITRARY_EPOCH, false);
        map.addValidAfter(+1, AbsoluteDate.ARBITRARY_EPOCH, false);
        Assertions.assertEquals(4, map.getSpansNumber());
        Assertions.assertEquals(-1, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-1)).intValue());
        Assertions.assertEquals(+1, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(+1)).intValue());
        checkCountConsistency(map);
    }

    @Test
    public void testDuplicatedBeforeBefore() {
        TimeSpanMap<Integer> map = new TimeSpanMap<>(null);
        map.addValidBefore(-2, AbsoluteDate.ARBITRARY_EPOCH, false); // first call at ARBITRARY_EPOCH
        map.addValidAfter(0, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-2), false);
        map.addValidBefore(-1, AbsoluteDate.ARBITRARY_EPOCH, false); // second call at ARBITRARY_EPOCH
        Assertions.assertEquals(3, map.getSpansNumber());
        Assertions.assertEquals(-2, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-10)).intValue());
        Assertions.assertEquals(-1, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-1)).intValue());
        Assertions.assertNull(map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(+1)));
        checkCountConsistency(map);
    }

    @Test
    public void testDuplicatedAfterBeforeAtEnd() {
        TimeSpanMap<Integer> map = new TimeSpanMap<>(null);
        map.addValidAfter(+1, AbsoluteDate.ARBITRARY_EPOCH, false);
        map.addValidBefore(-1, AbsoluteDate.ARBITRARY_EPOCH, false);
        Assertions.assertEquals(2, map.getSpansNumber());
        Assertions.assertEquals(-1, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-1)).intValue());
        Assertions.assertEquals(+1, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(+1)).intValue());
        checkCountConsistency(map);
    }

    @Test
    public void testDuplicatedAfterBeforeMiddle() {
        TimeSpanMap<Integer> map = new TimeSpanMap<>(null);
        map.addValidBefore(-2, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-2), false);
        map.addValidAfter(+2, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(+2), false);
        map.addValidAfter(+1, AbsoluteDate.ARBITRARY_EPOCH, false);
        map.addValidBefore(-1, AbsoluteDate.ARBITRARY_EPOCH, false);
        Assertions.assertEquals(4, map.getSpansNumber());
        Assertions.assertEquals(-1, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-1)).intValue());
        Assertions.assertEquals(+1, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(+1)).intValue());
        checkCountConsistency(map);
    }

    @Test
    public void testDuplicatedAfterAfter() {
        TimeSpanMap<Integer> map = new TimeSpanMap<>(null);
        map.addValidAfter(+2, AbsoluteDate.ARBITRARY_EPOCH, false); // first call at ARBITRARY_EPOCH
        map.addValidBefore(0, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(+2), false);
        map.addValidAfter(+1, AbsoluteDate.ARBITRARY_EPOCH, false); // second call at ARBITRARY_EPOCH
        Assertions.assertEquals(3, map.getSpansNumber());
        Assertions.assertNull(map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-1)));
        Assertions.assertEquals(+1, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(+1)).intValue());
        Assertions.assertEquals(+2, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(+10)).intValue());
        checkCountConsistency(map);
    }

    @Test
    public void testValidAllTime() {
        AbsoluteDate ref = AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(1);
        TimeSpanMap<Integer> map = new TimeSpanMap<>(0);

        // action
        map.addValidAfter(1, ref, false);
        map.addValidBefore(2, ref, false);

        // verify
        Assertions.assertEquals(1, (int) map.get(ref.shiftedBy(1)));
        Assertions.assertEquals(2, (int) map.get(ref.shiftedBy(-1)));
        Assertions.assertEquals(1, (int) map.get(ref));
    }

    @Test
    public void testBetweenPastInfinity() {
        TimeSpanMap<Integer> map = new TimeSpanMap<>(null);
        Assertions.assertEquals(1, map.getSpansNumber());
        map.addValidBetween(1, AbsoluteDate.PAST_INFINITY, AbsoluteDate.ARBITRARY_EPOCH);
        Assertions.assertEquals(2, map.getSpansNumber());
        Assertions.assertEquals(1, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-1)).intValue());
        Assertions.assertNull(map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(+1)));
    }

    @Test
    public void testBetweenFutureInfinity() {
        TimeSpanMap<Integer> map = new TimeSpanMap<>(null);
        Assertions.assertEquals(1, map.getSpansNumber());
        map.addValidBetween(1, AbsoluteDate.ARBITRARY_EPOCH, AbsoluteDate.FUTURE_INFINITY);
        Assertions.assertEquals(2, map.getSpansNumber());
        Assertions.assertNull(map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-1)));
        Assertions.assertEquals(1, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(+1)).intValue());
    }

    @Test
    public void testBetweenBothInfinity() {
        TimeSpanMap<Integer> map = new TimeSpanMap<>(null);
        Assertions.assertEquals(1, map.getSpansNumber());
        map.addValidBetween(1, AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY);
        Assertions.assertEquals(1, map.getSpansNumber());
        Assertions.assertEquals(1, map.get(AbsoluteDate.ARBITRARY_EPOCH).intValue());
    }

    @Test
    public void testFirstNonNull() {
        final TimeSpanMap<Integer> map = new TimeSpanMap<>(null);
        checkException(map, TimeSpanMap::getFirstNonNullSpan, OrekitMessages.NO_CACHED_ENTRIES);
        for (double dt = 0; dt < 10; dt += 0.25) {
            map.addValidAfter(null, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(dt), false);
        }
        checkException(map, TimeSpanMap::getFirstNonNullSpan, OrekitMessages.NO_CACHED_ENTRIES);
        map.addValidAfter(22, map.getLastTransition().getDate().shiftedBy( 60.0), false);
        map.addValidAfter(17, map.getLastTransition().getDate().shiftedBy(-20.0), false);
        Assertions.assertEquals(17, map.getFirstNonNullSpan().getData());
    }

    @Test
    public void testLastNonNull() {
        final TimeSpanMap<Integer> map = new TimeSpanMap<>(null);
        checkException(map, TimeSpanMap::getLastNonNullSpan, OrekitMessages.NO_CACHED_ENTRIES);
        for (double dt = 0; dt < 10; dt += 0.25) {
            map.addValidBefore(null, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-dt), false);
        }
        checkException(map, TimeSpanMap::getLastNonNullSpan, OrekitMessages.NO_CACHED_ENTRIES);
        map.addValidBefore(22, map.getLastTransition().getDate().shiftedBy(-60.0), false);
        map.addValidBefore(17, map.getLastTransition().getDate().shiftedBy( 20.0), false);
        Assertions.assertEquals(17, map.getLastNonNullSpan().getData());
    }

    @Test
    public void testMoveTowardsPastNoOverride() {
        final TimeSpanMap<Integer> map = new TimeSpanMap<>(null);
        for (int i = 0; i < 100; i +=10) {
            map.addValidAfter(i, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(i), false);
        }
        Assertions.assertEquals(11, map.getSpansNumber());
        TimeSpanMap.Transition<Integer> transition = map.getSpan(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(45)).getStartTransition();
        Assertions.assertEquals(40, transition.getDate().durationFrom(AbsoluteDate.ARBITRARY_EPOCH));
        Assertions.assertEquals(30, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(35.5)));
        Assertions.assertEquals(30, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(34.5)));
        transition.resetDate(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(35), true);
        Assertions.assertEquals(11, map.getSpansNumber());
        Assertions.assertEquals(35, transition.getDate().durationFrom(AbsoluteDate.ARBITRARY_EPOCH));
        Assertions.assertEquals(30, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(34.5)));
        Assertions.assertEquals(40, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(35.5)));
    }

    @Test
    public void testMoveTowardsPastOverride() {
        final TimeSpanMap<Integer> map = new TimeSpanMap<>(null);
        for (int i = 0; i < 100; i +=10) {
            map.addValidAfter(i, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(i), false);
        }
        Assertions.assertEquals(11, map.getSpansNumber());
        TimeSpanMap.Transition<Integer> transition = map.getSpan(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(45)).getStartTransition();
        Assertions.assertEquals(40, transition.getDate().durationFrom(AbsoluteDate.ARBITRARY_EPOCH));
        Assertions.assertEquals(10, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(15.5)));
        Assertions.assertEquals(10, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(14.5)));
        transition.resetDate(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(15), true);
        Assertions.assertEquals( 9, map.getSpansNumber());
        Assertions.assertEquals(15, transition.getDate().durationFrom(AbsoluteDate.ARBITRARY_EPOCH));
        Assertions.assertEquals(10, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(14.5)));
        Assertions.assertEquals(40, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(15.5)));
    }

    @Test
    public void testMoveTowardsPastOverrideAll() {
        final TimeSpanMap<Integer> map = new TimeSpanMap<>(null);
        for (int i = 0; i < 100; i +=10) {
            map.addValidBetween(i,
                                AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(i),
                                AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(i + 10));
        }
        Assertions.assertEquals(12, map.getSpansNumber());
        TimeSpanMap.Transition<Integer> transition = map.getSpan(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(45)).getStartTransition();
        Assertions.assertEquals(40, transition.getDate().durationFrom(AbsoluteDate.ARBITRARY_EPOCH));
        Assertions.assertNull(map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-10.5)));
        Assertions.assertNull(map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-9.5)));
        transition.resetDate(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-10), true);
        Assertions.assertEquals( 8, map.getSpansNumber());
        Assertions.assertEquals(-10, transition.getDate().durationFrom(AbsoluteDate.ARBITRARY_EPOCH));
        Assertions.assertEquals(40, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-9.5)));
        Assertions.assertNull(map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-10.5)));
    }

    @Test
    public void testMoveTowardsPastFirst() {
        final TimeSpanMap<Integer> map = new TimeSpanMap<>(null);
        for (int i = 0; i < 100; i +=10) {
            map.addValidBetween(i,
                                AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(i),
                                AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(i + 10));
        }
        Assertions.assertEquals(12, map.getSpansNumber());
        TimeSpanMap.Transition<Integer> transition = map.getFirstTransition();
        Assertions.assertEquals(0, transition.getDate().durationFrom(AbsoluteDate.ARBITRARY_EPOCH));
        Assertions.assertNull(map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-10.5)));
        Assertions.assertNull(map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-9.5)));
        transition.resetDate(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-10), true);
        Assertions.assertEquals(12, map.getSpansNumber());
        Assertions.assertEquals(-10, transition.getDate().durationFrom(AbsoluteDate.ARBITRARY_EPOCH));
        Assertions.assertEquals(0, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-9.5)));
        Assertions.assertNull(map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-10.5)));
    }

    @Test
    public void testMoveToPastInfinity() {
        final TimeSpanMap<Integer> map = new TimeSpanMap<>(null);
        for (int i = 0; i < 100; i +=10) {
            map.addValidBetween(i,
                                AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(i),
                                AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(i + 10));
        }
        Assertions.assertEquals(12, map.getSpansNumber());
        TimeSpanMap.Transition<Integer> transition = map.getSpan(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(55)).getStartTransition();
        Assertions.assertEquals(50, transition.getDate().durationFrom(AbsoluteDate.ARBITRARY_EPOCH));
        transition.resetDate(AbsoluteDate.PAST_INFINITY, true);
        Assertions.assertEquals( 6, map.getSpansNumber());
        Assertions.assertEquals(60, map.getFirstTransition().getDate().durationFrom(AbsoluteDate.ARBITRARY_EPOCH));
        Assertions.assertEquals(60, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(60.5)));
        Assertions.assertEquals(50, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-1000)));
    }

    @Test
    public void testMoveTransitionPastCollision() {
        final TimeSpanMap<Integer> map = new TimeSpanMap<>(null);
        for (int i = 0; i < 100; i +=10) {
            map.addValidBetween(i,
                                AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(i),
                                AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(i + 10));
        }
        TimeSpanMap.Transition<Integer> transition = map.getSpan(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(45)).getStartTransition();
        Assertions.assertEquals(40, transition.getDate().durationFrom(AbsoluteDate.ARBITRARY_EPOCH));
        try {
            transition.resetDate(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-3600), false);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.TRANSITION_DATES_COLLISION, oe.getSpecifier());
            Assertions.assertEquals(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(   40), oe.getParts()[0]);
            Assertions.assertEquals(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-3600), oe.getParts()[1]);
            Assertions.assertEquals(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(   30), oe.getParts()[2]);
        }
    }

    @Test
    public void testMoveTowardsFutureNoOverride() {
        final TimeSpanMap<Integer> map = new TimeSpanMap<>(null);
        for (int i = 0; i < 100; i +=10) {
            map.addValidBetween(i,
                                AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(i),
                                AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(i + 10));
        }
        Assertions.assertEquals(12, map.getSpansNumber());
        TimeSpanMap.Transition<Integer> transition = map.getSpan(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(45)).getStartTransition();
        Assertions.assertEquals(40, transition.getDate().durationFrom(AbsoluteDate.ARBITRARY_EPOCH));
        Assertions.assertEquals(40, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(45.5)));
        Assertions.assertEquals(40, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(44.5)));
        transition.resetDate(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(45), true);
        Assertions.assertEquals(12, map.getSpansNumber());
        Assertions.assertEquals(45, transition.getDate().durationFrom(AbsoluteDate.ARBITRARY_EPOCH));
        Assertions.assertEquals(30, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(44.5)));
        Assertions.assertEquals(40, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(45.5)));
    }

    @Test
    public void testMoveTowardsFutureOverride() {
        final TimeSpanMap<Integer> map = new TimeSpanMap<>(null);
        for (int i = 0; i < 100; i +=10) {
            map.addValidBetween(i,
                                AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(i),
                                AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(i + 10));
        }
        Assertions.assertEquals(12, map.getSpansNumber());
        TimeSpanMap.Transition<Integer> transition = map.getSpan(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(45)).getStartTransition();
        Assertions.assertEquals(40, transition.getDate().durationFrom(AbsoluteDate.ARBITRARY_EPOCH));
        Assertions.assertEquals(70, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(75.5)));
        Assertions.assertEquals(70, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(74.5)));
        transition.resetDate(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(75), true);
        Assertions.assertEquals( 9, map.getSpansNumber());
        Assertions.assertEquals(75, transition.getDate().durationFrom(AbsoluteDate.ARBITRARY_EPOCH));
        Assertions.assertEquals(30, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(74.5)));
        Assertions.assertEquals(70, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(75.5)));
    }

    @Test
    public void testMoveTowardsFutureOverrideAll() {
        final TimeSpanMap<Integer> map = new TimeSpanMap<>(null);
        for (int i = 0; i < 100; i +=10) {
            map.addValidBetween(i,
                                AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(i),
                                AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(i + 10));
        }
        Assertions.assertEquals(12, map.getSpansNumber());
        TimeSpanMap.Transition<Integer> transition = map.getSpan(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(45)).getStartTransition();
        Assertions.assertEquals(40, transition.getDate().durationFrom(AbsoluteDate.ARBITRARY_EPOCH));
        Assertions.assertNull(map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(110.5)));
        Assertions.assertNull(map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(109.5)));
        transition.resetDate(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(110), true);
        Assertions.assertEquals( 6, map.getSpansNumber());
        Assertions.assertEquals(110, transition.getDate().durationFrom(AbsoluteDate.ARBITRARY_EPOCH));
        Assertions.assertEquals(30, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(109.5)));
        Assertions.assertNull(map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(110.5)));
    }

    @Test
    public void testMoveTowardsFutureLast() {
        final TimeSpanMap<Integer> map = new TimeSpanMap<>(null);
        for (int i = 0; i < 100; i +=10) {
            map.addValidBetween(i,
                                AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(i),
                                AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(i + 10));
        }
        Assertions.assertEquals(12, map.getSpansNumber());
        TimeSpanMap.Transition<Integer> transition = map.getLastTransition();
        Assertions.assertEquals(100, transition.getDate().durationFrom(AbsoluteDate.ARBITRARY_EPOCH));
        Assertions.assertNull(map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(110.5)));
        Assertions.assertNull(map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(109.5)));
        transition.resetDate(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(110), true);
        Assertions.assertEquals(12, map.getSpansNumber());
        Assertions.assertEquals(110, transition.getDate().durationFrom(AbsoluteDate.ARBITRARY_EPOCH));
        Assertions.assertEquals(90,  map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(109.5)));
        Assertions.assertNull(map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(110.5)));
    }

    @Test
    public void testMoveToFutureInfinity() {
        final TimeSpanMap<Integer> map = new TimeSpanMap<>(null);
        for (int i = 0; i < 100; i +=10) {
            map.addValidBetween(i,
                                AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(i),
                                AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(i + 10));
        }
        Assertions.assertEquals(12, map.getSpansNumber());
        TimeSpanMap.Transition<Integer> transition = map.getSpan(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(45)).getStartTransition();
        Assertions.assertEquals(40, transition.getDate().durationFrom(AbsoluteDate.ARBITRARY_EPOCH));
        Assertions.assertEquals(70, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(75.5)));
        Assertions.assertEquals(70, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(74.5)));
        transition.resetDate(AbsoluteDate.FUTURE_INFINITY, true);
        Assertions.assertEquals( 5, map.getSpansNumber());
        Assertions.assertEquals(30, map.getLastTransition().getDate().durationFrom(AbsoluteDate.ARBITRARY_EPOCH));
        Assertions.assertEquals(30, map.get(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(1000)));
    }

    @Test
    public void testExpungeNumberEarliestForward() {
        doTestExpunge(5, Double.POSITIVE_INFINITY, ExpungePolicy.EXPUNGE_EARLIEST, true,
                      5, 50, 90, 25.0);
    }

    @Test
    public void testExpungeRangeEarliestForward() {
        doTestExpunge(Integer.MAX_VALUE, 55.0, ExpungePolicy.EXPUNGE_EARLIEST, true,
                      7, 30, 90, 25.0);
    }

    @Test
    public void testExpungeNumberEarliestBackward() {
        doTestExpunge(5, Double.POSITIVE_INFINITY, ExpungePolicy.EXPUNGE_EARLIEST, false,
                      5, 60, null, 25.0);
    }

    @Test
    public void testExpungeRangeEarliestBackward() {
        doTestExpunge(Integer.MAX_VALUE, 55.0, ExpungePolicy.EXPUNGE_EARLIEST, false,
                      7, 40, null, 25.0);
    }

    @Test
    public void testExpungeNumberLatestForward() {
        doTestExpunge(5, Double.POSITIVE_INFINITY, ExpungePolicy.EXPUNGE_LATEST, true,
                      5, null, 30, 75.0);
    }

    @Test
    public void testExpungeRangeLatestForward() {
        doTestExpunge(Integer.MAX_VALUE, 55.0, ExpungePolicy.EXPUNGE_LATEST, true,
                      7, null, 50, 75.0);
    }

    @Test
    public void testExpungeNumberLatestBackward() {
        doTestExpunge(5, Double.POSITIVE_INFINITY, ExpungePolicy.EXPUNGE_LATEST, false,
                      5, 0, 40, 75.0);
    }

    @Test
    public void testExpungeRangeLatestBackward() {
        doTestExpunge(Integer.MAX_VALUE, 55.0, ExpungePolicy.EXPUNGE_LATEST, false,
                      7, 0, 60, 75.0);
    }

    @Test
    public void testExpungeNumberFarthestForward() {
        doTestExpunge(5, Double.POSITIVE_INFINITY, ExpungePolicy.EXPUNGE_FARTHEST, true,
                      5, 50, 90, 25.0);
    }

    @Test
    public void testExpungeRangeFarthestForward() {
        doTestExpunge(Integer.MAX_VALUE, 55.0, ExpungePolicy.EXPUNGE_FARTHEST, true,
                      7, 30, 90, 25.0);
    }

    @Test
    public void testExpungeNumberFarthestBackward() {
        doTestExpunge(5, Double.POSITIVE_INFINITY, ExpungePolicy.EXPUNGE_FARTHEST, false,
                      5, 0, 40, 75.0);
    }

    @Test
    public void testExpungeRangeFarthestBackward() {
        doTestExpunge(Integer.MAX_VALUE, 55.0, ExpungePolicy.EXPUNGE_FARTHEST, false,
                      7, 0, 60, 75.0);
    }

    private void doTestExpunge(final int maxNbSpans, final double maxRange, final ExpungePolicy expungePolicy,
                               final boolean fillUpForward, final int expectedNbSpans,
                               final Integer expectedFirst, final Integer expectedLast,
                               final double invalidOffset) {
        final TimeSpanMap<Integer> map = new TimeSpanMap<>(null);
        map.configureExpunge(maxNbSpans, maxRange, expungePolicy);
        if (fillUpForward) {
            for (int i = 0; i < 100; i += 10) {
                map.addValidAfter(i, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(i), false);
            }
        } else {
            for (int i = 90; i >= 0; i -= 10) {
                map.addValidBefore(i,  AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(i + 10), false);
            }
        }
        Assertions.assertEquals(expectedNbSpans, map.getSpansNumber());
        Assertions.assertEquals(expectedFirst,   map.getFirstSpan().getData());
        Assertions.assertEquals(expectedLast,    map.getLastSpan().getData());
        try {
            map.getSpan(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(invalidOffset));
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.EXPUNGED_SPAN, oe.getSpecifier());
        }
    }

    @Test
    public void testLateExpungeConfiguration() {

        // initial setup
        final TimeSpanMap<Integer> map = new TimeSpanMap<>(null);
        for (int i = 0; i < 100; i += 10) {
            map.addValidAfter(i, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(i), false);
        }
        Assertions.assertEquals(  11, map.getSpansNumber());
        Assertions.assertNull(map.getFirstSpan().getData());
        Assertions.assertEquals(  90, map.getLastSpan().getData());

        // no changes just after configuration
        map.configureExpunge(5, Double.POSITIVE_INFINITY, ExpungePolicy.EXPUNGE_EARLIEST);
        Assertions.assertEquals(  11, map.getSpansNumber());
        Assertions.assertNull(map.getFirstSpan().getData());
        Assertions.assertEquals(  90, map.getLastSpan().getData());

        // changes applied after addition
        map.addValidAfter(100, AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(100), false);
        Assertions.assertEquals(  5, map.getSpansNumber());
        Assertions.assertEquals( 60, map.getFirstSpan().getData());
        Assertions.assertEquals(100, map.getLastSpan().getData());

    }

    @Test
    public void testMoveTransitionFutureCollision() {
        final TimeSpanMap<Integer> map = new TimeSpanMap<>(null);
        for (int i = 0; i < 100; i +=10) {
            map.addValidBetween(i,
                                AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(i),
                                AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(i + 10));
        }
        TimeSpanMap.Transition<Integer> transition = map.getSpan(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(45)).getStartTransition();
        Assertions.assertEquals(40, transition.getDate().durationFrom(AbsoluteDate.ARBITRARY_EPOCH));
        try {
            transition.resetDate(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(3600), false);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.TRANSITION_DATES_COLLISION, oe.getSpecifier());
            Assertions.assertEquals(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(  40), oe.getParts()[0]);
            Assertions.assertEquals(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(3600), oe.getParts()[1]);
            Assertions.assertEquals(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(  50), oe.getParts()[2]);
        }
    }

    private <T> void checkException(final TimeSpanMap<T> map,
                                    final Consumer<TimeSpanMap<T>> f,
                                    OrekitMessages expected) {
        try {
            f.accept(map);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(expected, oe.getSpecifier());
        }
    }

    private <T> void checkCountConsistency(final TimeSpanMap<T> map) {
        final int count1 = map.getSpansNumber();
        int count2 = 0;
        for (Span<T> span = map.getFirstSpan(); span != null; span = span.next()) {
            ++count2;
        }
        Assertions.assertEquals(count1, count2);
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
