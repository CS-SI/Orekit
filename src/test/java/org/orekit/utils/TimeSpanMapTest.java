/* Copyright 2002-2022 CS GROUP
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


import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeSpanMap.Transition;

public class TimeSpanMapTest {

    @Test
    public void testSingleEntry() {
        String single = "single";
        TimeSpanMap<String> map = new TimeSpanMap<>(single);
        Assert.assertSame(single, map.get(AbsoluteDate.CCSDS_EPOCH));
        Assert.assertSame(single, map.get(AbsoluteDate.FIFTIES_EPOCH));
        Assert.assertSame(single, map.get(AbsoluteDate.FUTURE_INFINITY));
        Assert.assertSame(single, map.get(AbsoluteDate.GALILEO_EPOCH));
        Assert.assertSame(single, map.get(AbsoluteDate.GPS_EPOCH));
        Assert.assertSame(single, map.get(AbsoluteDate.J2000_EPOCH));
        Assert.assertSame(single, map.get(AbsoluteDate.JAVA_EPOCH));
        Assert.assertSame(single, map.get(AbsoluteDate.JULIAN_EPOCH));
        Assert.assertSame(single, map.get(AbsoluteDate.MODIFIED_JULIAN_EPOCH));
        Assert.assertSame(single, map.get(AbsoluteDate.PAST_INFINITY));
        Assert.assertEquals(0, map.getTransitionsNumber());
    }

    @Test
    public void testForwardAdd() {
        final AbsoluteDate ref = AbsoluteDate.J2000_EPOCH;
        TimeSpanMap<Integer> map = new TimeSpanMap<>(Integer.valueOf(0));
        for (int i = 1; i < 100; ++i) {
            map.addValidAfter(Integer.valueOf(i), ref.shiftedBy(i), false);
        }
        Assert.assertEquals(0, map.get(ref.shiftedBy(-1000.0)).intValue());
        Assert.assertEquals(0, map.get(ref.shiftedBy( -100.0)).intValue());
        TimeSpanMap.Span<Integer> span = map.getSpan(ref.shiftedBy(-1000.0));
        Assert.assertEquals(0, span.getData().intValue());
        Assert.assertTrue(span.getStart().durationFrom(AbsoluteDate.J2000_EPOCH) < -Double.MAX_VALUE);
        Assert.assertEquals(1.0, span.getEnd().durationFrom(AbsoluteDate.J2000_EPOCH), 1.0e-15);
        for (int i = 0; i < 100; ++i) {
            Assert.assertEquals(i, map.get(ref.shiftedBy(i + 0.1)).intValue());
            Assert.assertEquals(i, map.get(ref.shiftedBy(i + 0.9)).intValue());
            span = map.getSpan(ref.shiftedBy(i + 0.1));
            Assert.assertEquals(i, span.getData().intValue());
            if (i == 0) {
                Assert.assertTrue(span.getStart().durationFrom(AbsoluteDate.J2000_EPOCH) < -Double.MAX_VALUE);
            } else {
                Assert.assertEquals(i, span.getStart().durationFrom(AbsoluteDate.J2000_EPOCH), 1.0e-15);
            } if (i == 99) {
                Assert.assertTrue(span.getEnd().durationFrom(AbsoluteDate.J2000_EPOCH) > Double.MAX_VALUE);
            } else {
                Assert.assertEquals(i + 1, span.getEnd().durationFrom(AbsoluteDate.J2000_EPOCH), 1.0e-15);
            }
        }
        Assert.assertEquals(99, map.get(ref.shiftedBy(  100.0)).intValue());
        Assert.assertEquals(99, map.get(ref.shiftedBy( 1000.0)).intValue());
    }

    @Test
    public void testBackwardAdd() {
        final AbsoluteDate ref = AbsoluteDate.J2000_EPOCH;
        TimeSpanMap<Integer> map = new TimeSpanMap<>(Integer.valueOf(0));
        for (int i = -1; i > -100; --i) {
            map.addValidBefore(Integer.valueOf(i), ref.shiftedBy(i), false);
        }
        Assert.assertEquals(0, map.get(ref.shiftedBy( 1000.0)).intValue());
        Assert.assertEquals(0, map.get(ref.shiftedBy(  100.0)).intValue());
        for (int i = 0; i > -100; --i) {
            Assert.assertEquals(i, map.get(ref.shiftedBy(i - 0.1)).intValue());
            Assert.assertEquals(i, map.get(ref.shiftedBy(i - 0.9)).intValue());
        }
        Assert.assertEquals(-99, map.get(ref.shiftedBy( -100.0)).intValue());
        Assert.assertEquals(-99, map.get(ref.shiftedBy(-1000.0)).intValue());
    }

    @Deprecated
    @Test
    public void testDeprecatedAddValid() {
        final AbsoluteDate ref = AbsoluteDate.J2000_EPOCH;
        TimeSpanMap<Integer> map = new TimeSpanMap<>(Integer.valueOf(0));
        map.addValidAfter(Integer.valueOf(10), ref.shiftedBy(10.0));
        map.addValidAfter(Integer.valueOf( 3), ref.shiftedBy( 2.0));
        map.addValidAfter(Integer.valueOf( 9), ref.shiftedBy( 5.0));
        map.addValidBefore(Integer.valueOf( 2), ref.shiftedBy( 3.0));
        map.addValidBefore(Integer.valueOf( 5), ref.shiftedBy( 9.0));
        Assert.assertEquals( 0, map.get(ref.shiftedBy( -1.0)).intValue());
        Assert.assertEquals( 0, map.get(ref.shiftedBy(  1.9)).intValue());
        Assert.assertEquals( 2, map.get(ref.shiftedBy(  2.1)).intValue());
        Assert.assertEquals( 2, map.get(ref.shiftedBy(  2.9)).intValue());
        Assert.assertEquals( 3, map.get(ref.shiftedBy(  3.1)).intValue());
        Assert.assertEquals( 3, map.get(ref.shiftedBy(  4.9)).intValue());
        Assert.assertEquals( 5, map.get(ref.shiftedBy(  5.1)).intValue());
        Assert.assertEquals( 5, map.get(ref.shiftedBy(  8.9)).intValue());
        Assert.assertEquals( 9, map.get(ref.shiftedBy(  9.1)).intValue());
        Assert.assertEquals( 9, map.get(ref.shiftedBy(  9.9)).intValue());
        Assert.assertEquals(10, map.get(ref.shiftedBy( 10.1)).intValue());
        Assert.assertEquals(10, map.get(ref.shiftedBy(100.0)).intValue());
    }

    @Deprecated
    @Test
    public void testDeprecatedNavigableMap() {
        TimeSpanMap<Integer> map = new TimeSpanMap<>(Integer.valueOf(0));
        Assert.assertEquals(0, map.getTransitionsNumber());
        Assert.assertEquals(1, map.getTransitions().size());
        Assert.assertSame(AbsoluteDate.ARBITRARY_EPOCH, map.getTransitions().first().getDate());
    }

    @Test
    public void testRandomAddNoErase() {
        final AbsoluteDate ref = AbsoluteDate.J2000_EPOCH;
        TimeSpanMap<Integer> map = new TimeSpanMap<>(Integer.valueOf(0));
        map.addValidAfter(Integer.valueOf(10), ref.shiftedBy(10.0), false);
        map.addValidAfter(Integer.valueOf( 3), ref.shiftedBy( 2.0), false);
        map.addValidAfter(Integer.valueOf( 9), ref.shiftedBy( 5.0), false);
        map.addValidBefore(Integer.valueOf( 2), ref.shiftedBy( 3.0), false);
        map.addValidBefore(Integer.valueOf( 5), ref.shiftedBy( 9.0), false);
        Assert.assertEquals( 0, map.get(ref.shiftedBy( -1.0)).intValue());
        Assert.assertEquals( 0, map.get(ref.shiftedBy(  1.9)).intValue());
        Assert.assertEquals( 2, map.get(ref.shiftedBy(  2.1)).intValue());
        Assert.assertEquals( 2, map.get(ref.shiftedBy(  2.9)).intValue());
        Assert.assertEquals( 3, map.get(ref.shiftedBy(  3.1)).intValue());
        Assert.assertEquals( 3, map.get(ref.shiftedBy(  4.9)).intValue());
        Assert.assertEquals( 5, map.get(ref.shiftedBy(  5.1)).intValue());
        Assert.assertEquals( 5, map.get(ref.shiftedBy(  8.9)).intValue());
        Assert.assertEquals( 9, map.get(ref.shiftedBy(  9.1)).intValue());
        Assert.assertEquals( 9, map.get(ref.shiftedBy(  9.9)).intValue());
        Assert.assertEquals(10, map.get(ref.shiftedBy( 10.1)).intValue());
        Assert.assertEquals(10, map.get(ref.shiftedBy(100.0)).intValue());
        final StringBuilder builder = new StringBuilder();
        map.forEach(i -> builder.append(' ').append(i));
        Assert.assertEquals(" 0 2 3 5 9 10", builder.toString());
        Assert.assertEquals(5, map.getTransitionsNumber());
    }

    @Test
    public void testRandomAddErase() {
        final AbsoluteDate ref = AbsoluteDate.J2000_EPOCH;
        TimeSpanMap<Integer> map = new TimeSpanMap<>(null);
        map.addValidAfter(Integer.valueOf(10), ref.shiftedBy(10.0), false);
        map.addValidAfter(Integer.valueOf( 3), ref.shiftedBy( 2.0), false);
        map.addValidBefore(Integer.valueOf( 5), ref.shiftedBy( 7.0), false);
        map.addValidAfter(null, ref.shiftedBy( 5.0), true);
        map.addValidAfter(Integer.valueOf( 1), ref.shiftedBy( 1.0), false);
        map.addValidBefore(null, ref.shiftedBy( 3.0), true);
        map.addValidBefore(Integer.valueOf( 7), ref.shiftedBy( 9.0), false);
        Assert.assertNull(map.get(ref.shiftedBy( -1.0)));
        Assert.assertNull(map.get(ref.shiftedBy(  1.9)));
        Assert.assertNull(map.get(ref.shiftedBy(  2.1)));
        Assert.assertNull(map.get(ref.shiftedBy(  2.9)));
        Assert.assertEquals( 5, map.get(ref.shiftedBy(  3.1)).intValue());
        Assert.assertEquals( 5, map.get(ref.shiftedBy(  4.9)).intValue());
        Assert.assertEquals( 7, map.get(ref.shiftedBy(  5.1)).intValue());
        Assert.assertEquals( 7, map.get(ref.shiftedBy(  6.9)).intValue());
        Assert.assertEquals( 7, map.get(ref.shiftedBy(  7.1)).intValue());
        Assert.assertEquals( 7, map.get(ref.shiftedBy(  8.9)).intValue());
        Assert.assertNull(map.get(ref.shiftedBy(  9.1)));
        Assert.assertNull(map.get(ref.shiftedBy(  9.9)));
        Assert.assertNull(map.get(ref.shiftedBy( 10.1)));
        Assert.assertNull(map.get(ref.shiftedBy(100.0)));
        final StringBuilder builder = new StringBuilder();
        map.forEach(i -> builder.append(' ').append(i));
        Assert.assertEquals(" 5 7", builder.toString());
        Assert.assertEquals(3, map.getTransitionsNumber());
    }

    @Test
    public void testExtractRangeInfinity() {
        final AbsoluteDate ref = AbsoluteDate.J2000_EPOCH;
        TimeSpanMap<Integer> map = new TimeSpanMap<>(Integer.valueOf(0));
        map.addValidAfter(Integer.valueOf(10), ref.shiftedBy(10.0), false);
        map.addValidAfter(Integer.valueOf( 3), ref.shiftedBy( 2.0), false);
        map.addValidAfter(Integer.valueOf( 9), ref.shiftedBy( 5.0), false);
        map.addValidBefore(Integer.valueOf( 2), ref.shiftedBy( 3.0), false);
        map.addValidBefore(Integer.valueOf( 5), ref.shiftedBy( 9.0), false);
        TimeSpanMap<Integer> range = map.extractRange(AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY);
        Assert.assertEquals(map.getTransitionsNumber(), range.getTransitionsNumber());
    }

    @Test
    public void testExtractRangeSingleEntry() {
        final AbsoluteDate ref = AbsoluteDate.J2000_EPOCH;
        TimeSpanMap<Integer> map = new TimeSpanMap<>(Integer.valueOf(0));
        map.addValidAfter(Integer.valueOf(10), ref.shiftedBy(10.0), false);
        map.addValidAfter(Integer.valueOf( 3), ref.shiftedBy( 2.0), false);
        map.addValidAfter(Integer.valueOf( 9), ref.shiftedBy( 5.0), false);
        map.addValidBefore(Integer.valueOf( 2), ref.shiftedBy( 3.0), false);
        map.addValidBefore(Integer.valueOf( 5), ref.shiftedBy( 9.0), false);
        TimeSpanMap<Integer> range = map.extractRange(ref.shiftedBy(6), ref.shiftedBy(8));
        Assert.assertEquals(0, range.getTransitionsNumber());
        Assert.assertEquals(5, range.get(ref.shiftedBy(-10000)).intValue());
        Assert.assertEquals(5, range.get(ref.shiftedBy(+10000)).intValue());
    }

    @Test
    public void testExtractFromPastInfinity() {
        final AbsoluteDate ref = AbsoluteDate.J2000_EPOCH;
        TimeSpanMap<Integer> map = new TimeSpanMap<>(Integer.valueOf(0));
        map.addValidAfter(Integer.valueOf(10), ref.shiftedBy(10.0), false);
        map.addValidAfter(Integer.valueOf( 3), ref.shiftedBy( 2.0), false);
        map.addValidAfter(Integer.valueOf( 9), ref.shiftedBy( 5.0), false);
        map.addValidBefore(Integer.valueOf( 2), ref.shiftedBy( 3.0), false);
        map.addValidBefore(Integer.valueOf( 5), ref.shiftedBy( 9.0), false);
        TimeSpanMap<Integer> range = map.extractRange(AbsoluteDate.PAST_INFINITY, ref.shiftedBy(8));
        Assert.assertEquals(3, range.getTransitionsNumber());
        Assert.assertEquals( 0, range.get(ref.shiftedBy( -1.0)).intValue());
        Assert.assertEquals( 0, range.get(ref.shiftedBy(  1.9)).intValue());
        Assert.assertEquals( 2, range.get(ref.shiftedBy(  2.1)).intValue());
        Assert.assertEquals( 2, range.get(ref.shiftedBy(  2.9)).intValue());
        Assert.assertEquals( 3, range.get(ref.shiftedBy(  3.1)).intValue());
        Assert.assertEquals( 3, range.get(ref.shiftedBy(  4.9)).intValue());
        Assert.assertEquals( 5, range.get(ref.shiftedBy(  5.1)).intValue());
        Assert.assertEquals( 5, range.get(ref.shiftedBy(  8.9)).intValue());
        Assert.assertEquals( 5, range.get(ref.shiftedBy(  9.1)).intValue());
        Assert.assertEquals( 5, range.get(ref.shiftedBy( 99.0)).intValue());
    }

    @Test
    public void testExtractToFutureInfinity() {
        final AbsoluteDate ref = AbsoluteDate.J2000_EPOCH;
        TimeSpanMap<Integer> map = new TimeSpanMap<>(Integer.valueOf(0));
        map.addValidAfter(Integer.valueOf(10), ref.shiftedBy(10.0), false);
        map.addValidAfter(Integer.valueOf( 3), ref.shiftedBy( 2.0), false);
        map.addValidAfter(Integer.valueOf( 9), ref.shiftedBy( 5.0), false);
        map.addValidBefore(Integer.valueOf( 2), ref.shiftedBy( 3.0), false);
        map.addValidBefore(Integer.valueOf( 5), ref.shiftedBy( 9.0), false);
        TimeSpanMap<Integer> range = map.extractRange(ref.shiftedBy(4), AbsoluteDate.FUTURE_INFINITY);
        Assert.assertEquals(3, range.getTransitionsNumber());
        Assert.assertEquals( 3, range.get(ref.shiftedBy(-99.0)).intValue());
        Assert.assertEquals( 3, range.get(ref.shiftedBy(  4.9)).intValue());
        Assert.assertEquals( 5, range.get(ref.shiftedBy(  5.1)).intValue());
        Assert.assertEquals( 5, range.get(ref.shiftedBy(  8.9)).intValue());
        Assert.assertEquals( 9, range.get(ref.shiftedBy(  9.1)).intValue());
        Assert.assertEquals( 9, range.get(ref.shiftedBy(  9.9)).intValue());
        Assert.assertEquals(10, range.get(ref.shiftedBy( 10.1)).intValue());
        Assert.assertEquals(10, range.get(ref.shiftedBy(100.0)).intValue());
    }

    @Test
    public void testExtractIntermediate() {
        final AbsoluteDate ref = AbsoluteDate.J2000_EPOCH;
        TimeSpanMap<Integer> map = new TimeSpanMap<>(Integer.valueOf(0));
        map.addValidAfter(Integer.valueOf(10), ref.shiftedBy(10.0), false);
        map.addValidAfter(Integer.valueOf( 3), ref.shiftedBy( 2.0), false);
        map.addValidAfter(Integer.valueOf( 9), ref.shiftedBy( 5.0), false);
        map.addValidBefore(Integer.valueOf( 2), ref.shiftedBy( 3.0), false);
        map.addValidBefore(Integer.valueOf( 5), ref.shiftedBy( 9.0), false);
        TimeSpanMap<Integer> range = map.extractRange(ref.shiftedBy(4), ref.shiftedBy(8));
        Assert.assertEquals(1, range.getTransitionsNumber());
        Assert.assertEquals( 3, range.get(ref.shiftedBy(-99.0)).intValue());
        Assert.assertEquals( 3, range.get(ref.shiftedBy(  4.9)).intValue());
        Assert.assertEquals( 5, range.get(ref.shiftedBy(  5.1)).intValue());
        Assert.assertEquals( 5, range.get(ref.shiftedBy(  8.9)).intValue());
        Assert.assertEquals( 5, range.get(ref.shiftedBy(  9.1)).intValue());
        Assert.assertEquals( 5, range.get(ref.shiftedBy(999.9)).intValue());
    }

    @Test
    public void testSpanToTransitionLinkEmpty() {
        TimeSpanMap.Span<Integer> span = new TimeSpanMap<>(1).getSpan(AbsoluteDate.ARBITRARY_EPOCH);
        Assert.assertEquals(1, span.getData().intValue());
        Assert.assertSame(AbsoluteDate.PAST_INFINITY, span.getStart());
        Assert.assertNull(span.getStartTransition());
        Assert.assertSame(AbsoluteDate.FUTURE_INFINITY, span.getEnd());
        Assert.assertNull(span.getEndTransition());
    }

    @Test
    public void testSpanToTransitionLink() {
        final AbsoluteDate ref = AbsoluteDate.ARBITRARY_EPOCH;
        TimeSpanMap<Integer> map = new TimeSpanMap<>(Integer.valueOf(0));
        map.addValidAfter(Integer.valueOf(10), ref.shiftedBy(10.0), false);
        map.addValidAfter(Integer.valueOf( 3), ref.shiftedBy( 2.0), false);
        map.addValidAfter(Integer.valueOf( 9), ref.shiftedBy( 5.0), false);
        map.addValidBefore(Integer.valueOf( 2), ref.shiftedBy( 3.0), false);
        map.addValidBefore(Integer.valueOf( 5), ref.shiftedBy( 9.0), false);

        TimeSpanMap.Span<Integer> first = map.getSpan(ref.shiftedBy(-99.0));
        Assert.assertEquals(0, first.getData().intValue());
        Assert.assertSame(AbsoluteDate.PAST_INFINITY, first.getStart());
        Assert.assertNull(first.getStartTransition());
        Assert.assertEquals(2.0, first.getEnd().durationFrom(ref), 1.0e-15);
        Assert.assertNotNull(first.getEndTransition());

        TimeSpanMap.Span<Integer> middle = map.getSpan(ref.shiftedBy(6.0));
        Assert.assertEquals(5, middle.getData().intValue());
        Assert.assertEquals(5.0, middle.getStart().durationFrom(ref), 1.0e-15);
        Assert.assertNotNull(middle.getStartTransition());
        Assert.assertEquals(9.0, middle.getEnd().durationFrom(ref), 1.0e-15);
        Assert.assertNotNull(middle.getEndTransition());
        Assert.assertSame(middle.getStartTransition().getAfter(), middle.getEndTransition().getBefore());
        Assert.assertEquals(3, middle.getStartTransition().getBefore().intValue());
        Assert.assertEquals(5, middle.getStartTransition().getAfter().intValue());
        Assert.assertEquals(5, middle.getEndTransition().getBefore().intValue());
        Assert.assertEquals(9, middle.getEndTransition().getAfter().intValue());

        TimeSpanMap.Span<Integer> last = map.getSpan(ref.shiftedBy(+99.0));
        Assert.assertEquals(10, last.getData().intValue());
        Assert.assertEquals(10.0, last.getStart().durationFrom(ref), 1.0e-15);
        Assert.assertNotNull(last.getStartTransition());
        Assert.assertSame(AbsoluteDate.FUTURE_INFINITY, last.getEnd());
        Assert.assertNull(last.getEndTransition());

    }

    @Test
    public void testTransitionToSpanLink() {
        final AbsoluteDate ref = AbsoluteDate.ARBITRARY_EPOCH;
        TimeSpanMap<Integer> map = new TimeSpanMap<>(Integer.valueOf(0));
        map.addValidAfter(Integer.valueOf(10), ref.shiftedBy(10.0), false);
        map.addValidAfter(Integer.valueOf( 3), ref.shiftedBy( 2.0), false);
        map.addValidAfter(Integer.valueOf( 9), ref.shiftedBy( 5.0), false);
        map.addValidBefore(Integer.valueOf( 2), ref.shiftedBy( 3.0), false);
        map.addValidBefore(Integer.valueOf( 5), ref.shiftedBy( 9.0), false);

        TimeSpanMap.Transition<Integer> first = map.getSpan(ref.shiftedBy(-99.0)).getEndTransition();
        Assert.assertEquals(2.0, first.getDate().durationFrom(ref), 1.0e-15);
        Assert.assertEquals(0, first.getBefore().intValue());
        Assert.assertEquals(2, first.getAfter().intValue());

        TimeSpanMap.Transition<Integer> middle = map.getSpan(ref.shiftedBy(6.0)).getStartTransition();
        Assert.assertEquals( 5.0, middle.getDate().durationFrom(ref), 1.0e-15);
        Assert.assertEquals( 3, middle.getBefore().intValue());
        Assert.assertEquals( 5, middle.getAfter().intValue());

        TimeSpanMap.Transition<Integer> last = map.getSpan(ref.shiftedBy(+99.0)).getStartTransition();
        Assert.assertEquals(10.0, last.getDate().durationFrom(ref), 1.0e-15);
        Assert.assertEquals( 9, last.getBefore().intValue());
        Assert.assertEquals(10, last.getAfter().intValue());

    }

    @Test
    public void tesFirstLastEmpty() {
        TimeSpanMap<Integer> map = new TimeSpanMap<>(Integer.valueOf(0));
        Assert.assertNull(map.getFirstTransition());
        Assert.assertNull(map.getLastTransition());
        Assert.assertSame(map.getFirstSpan(), map.getLastSpan());
        Assert.assertNull(map.getFirstSpan().getStartTransition());
        Assert.assertNull(map.getFirstSpan().getEndTransition());
        Assert.assertNull(map.getFirstSpan().previous());
        Assert.assertNull(map.getLastSpan().next());
    }

    @Test
    public void testSpansNavigation() {
        final AbsoluteDate ref = AbsoluteDate.ARBITRARY_EPOCH;
        TimeSpanMap<Integer> map = new TimeSpanMap<>(Integer.valueOf(0));
        map.addValidAfter(Integer.valueOf(10), ref.shiftedBy(10.0), false);
        map.addValidAfter(Integer.valueOf( 3), ref.shiftedBy( 2.0), false);
        map.addValidAfter(Integer.valueOf( 9), ref.shiftedBy( 5.0), false);
        map.addValidBefore(Integer.valueOf( 2), ref.shiftedBy( 3.0), false);
        map.addValidBefore(Integer.valueOf( 5), ref.shiftedBy( 9.0), false);
        Assert.assertNull(map.getFirstSpan().previous());
        Assert.assertNull(map.getLastSpan().next());

        TimeSpanMap.Span<Integer> span = map.getFirstSpan();
        Assert.assertEquals(0, span.getData().intValue());
        span = span.next();
        Assert.assertEquals(2, span.getData().intValue());
        span = span.next();
        Assert.assertEquals(3, span.getData().intValue());
        span = span.next();
        Assert.assertEquals(5, span.getData().intValue());
        span = span.next();
        Assert.assertEquals(9, span.getData().intValue());
        span = span.next();
        Assert.assertEquals(10, span.getData().intValue());
        Assert.assertNull(span.next());
        span = span.previous();
        Assert.assertEquals(9, span.getData().intValue());
        span = span.previous();
        Assert.assertEquals(5, span.getData().intValue());
        span = span.previous();
        Assert.assertEquals(3, span.getData().intValue());
        span = span.previous();
        Assert.assertEquals(2, span.getData().intValue());
        span = span.previous();
        Assert.assertEquals(0, span.getData().intValue());
        Assert.assertNull(span.previous());

    }

    @Test
    public void testTransitionsNavigation() {
        final AbsoluteDate ref = AbsoluteDate.ARBITRARY_EPOCH;
        TimeSpanMap<Integer> map = new TimeSpanMap<>(Integer.valueOf(0));
        map.addValidAfter(Integer.valueOf(10), ref.shiftedBy(10.0), false);
        map.addValidAfter(Integer.valueOf( 3), ref.shiftedBy( 2.0), false);
        map.addValidAfter(Integer.valueOf( 9), ref.shiftedBy( 5.0), false);
        map.addValidBefore(Integer.valueOf( 2), ref.shiftedBy( 3.0), false);
        map.addValidBefore(Integer.valueOf( 5), ref.shiftedBy( 9.0), false);

        Assert.assertEquals( 2.0, map.getFirstTransition().getDate().durationFrom(ref), 1.0e-15);
        Assert.assertEquals(10.0, map.getLastTransition().getDate().durationFrom(ref), 1.0e-15);

        Transition<Integer> transition = map.getLastTransition();
        Assert.assertEquals(10.0, transition.getDate().durationFrom(ref), 1.0e-15);
        transition = transition.previous();
        Assert.assertEquals( 9.0, transition.getDate().durationFrom(ref), 1.0e-15);
        transition = transition.previous();
        Assert.assertEquals( 5.0, transition.getDate().durationFrom(ref), 1.0e-15);
        transition = transition.previous();
        Assert.assertEquals( 3.0, transition.getDate().durationFrom(ref), 1.0e-15);
        transition = transition.previous();
        Assert.assertEquals( 2.0, transition.getDate().durationFrom(ref), 1.0e-15);
        Assert.assertNull(transition.previous());
        transition = transition.next();
        Assert.assertEquals( 3.0, transition.getDate().durationFrom(ref), 1.0e-15);
        transition = transition.next();
        Assert.assertEquals( 5.0, transition.getDate().durationFrom(ref), 1.0e-15);
        transition = transition.next();
        Assert.assertEquals( 9.0, transition.getDate().durationFrom(ref), 1.0e-15);
        transition = transition.next();
        Assert.assertEquals(10.0, transition.getDate().durationFrom(ref), 1.0e-15);
        Assert.assertNull(transition.next());

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
