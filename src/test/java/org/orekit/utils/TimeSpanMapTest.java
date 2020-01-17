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
package org.orekit.utils;


import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.time.AbsoluteDate;

public class TimeSpanMapTest {

    @Test
    public void testSingleEntry() {
        String single = "single";
        TimeSpanMap<String> map = new TimeSpanMap<String>(single);
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
    }

    @Test
    public void testForwardAdd() {
        final AbsoluteDate ref = AbsoluteDate.J2000_EPOCH;
        TimeSpanMap<Integer> map = new TimeSpanMap<Integer>(Integer.valueOf(0));
        for (int i = 1; i < 100; ++i) {
            map.addValidAfter(Integer.valueOf(i), ref.shiftedBy(i));
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
        TimeSpanMap<Integer> map = new TimeSpanMap<Integer>(Integer.valueOf(0));
        for (int i = -1; i > -100; --i) {
            map.addValidBefore(Integer.valueOf(i), ref.shiftedBy(i));
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

    @Test
    public void testRandomAdd() {
        final AbsoluteDate ref = AbsoluteDate.J2000_EPOCH;
        TimeSpanMap<Integer> map = new TimeSpanMap<Integer>(Integer.valueOf(0));
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

    @Test
    public void testExtractRangeInfinity() {
        final AbsoluteDate ref = AbsoluteDate.J2000_EPOCH;
        TimeSpanMap<Integer> map = new TimeSpanMap<Integer>(Integer.valueOf(0));        
        map.addValidAfter(Integer.valueOf(10), ref.shiftedBy(10.0));
        map.addValidAfter(Integer.valueOf( 3), ref.shiftedBy( 2.0));
        map.addValidAfter(Integer.valueOf( 9), ref.shiftedBy( 5.0));
        map.addValidBefore(Integer.valueOf( 2), ref.shiftedBy( 3.0));
        map.addValidBefore(Integer.valueOf( 5), ref.shiftedBy( 9.0));
        TimeSpanMap<Integer> range = map.extractRange(AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY);
        Assert.assertEquals(map.getTransitions().size(), range.getTransitions().size());
    }

    @Test
    public void testExtractRangeSingleEntry() {
        final AbsoluteDate ref = AbsoluteDate.J2000_EPOCH;
        TimeSpanMap<Integer> map = new TimeSpanMap<Integer>(Integer.valueOf(0));        
        map.addValidAfter(Integer.valueOf(10), ref.shiftedBy(10.0));
        map.addValidAfter(Integer.valueOf( 3), ref.shiftedBy( 2.0));
        map.addValidAfter(Integer.valueOf( 9), ref.shiftedBy( 5.0));
        map.addValidBefore(Integer.valueOf( 2), ref.shiftedBy( 3.0));
        map.addValidBefore(Integer.valueOf( 5), ref.shiftedBy( 9.0));
        TimeSpanMap<Integer> range = map.extractRange(ref.shiftedBy(6), ref.shiftedBy(8));
        Assert.assertEquals(1, range.getTransitions().size());
        Assert.assertEquals(5, range.get(ref.shiftedBy(-10000)).intValue());
        Assert.assertEquals(5, range.get(ref.shiftedBy(+10000)).intValue());
    }

    @Test
    public void testExtractFromPastInfinity() {
        final AbsoluteDate ref = AbsoluteDate.J2000_EPOCH;
        TimeSpanMap<Integer> map = new TimeSpanMap<Integer>(Integer.valueOf(0));        
        map.addValidAfter(Integer.valueOf(10), ref.shiftedBy(10.0));
        map.addValidAfter(Integer.valueOf( 3), ref.shiftedBy( 2.0));
        map.addValidAfter(Integer.valueOf( 9), ref.shiftedBy( 5.0));
        map.addValidBefore(Integer.valueOf( 2), ref.shiftedBy( 3.0));
        map.addValidBefore(Integer.valueOf( 5), ref.shiftedBy( 9.0));
        TimeSpanMap<Integer> range = map.extractRange(AbsoluteDate.PAST_INFINITY, ref.shiftedBy(8));
        Assert.assertEquals(3, range.getTransitions().size());
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
        TimeSpanMap<Integer> map = new TimeSpanMap<Integer>(Integer.valueOf(0));        
        map.addValidAfter(Integer.valueOf(10), ref.shiftedBy(10.0));
        map.addValidAfter(Integer.valueOf( 3), ref.shiftedBy( 2.0));
        map.addValidAfter(Integer.valueOf( 9), ref.shiftedBy( 5.0));
        map.addValidBefore(Integer.valueOf( 2), ref.shiftedBy( 3.0));
        map.addValidBefore(Integer.valueOf( 5), ref.shiftedBy( 9.0));
        TimeSpanMap<Integer> range = map.extractRange(ref.shiftedBy(4), AbsoluteDate.FUTURE_INFINITY);
        Assert.assertEquals(3, range.getTransitions().size());
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
        TimeSpanMap<Integer> map = new TimeSpanMap<Integer>(Integer.valueOf(0));        
        map.addValidAfter(Integer.valueOf(10), ref.shiftedBy(10.0));
        map.addValidAfter(Integer.valueOf( 3), ref.shiftedBy( 2.0));
        map.addValidAfter(Integer.valueOf( 9), ref.shiftedBy( 5.0));
        map.addValidBefore(Integer.valueOf( 2), ref.shiftedBy( 3.0));
        map.addValidBefore(Integer.valueOf( 5), ref.shiftedBy( 9.0));
        TimeSpanMap<Integer> range = map.extractRange(ref.shiftedBy(4), ref.shiftedBy(8));
        Assert.assertEquals(1, range.getTransitions().size());
        Assert.assertEquals( 3, range.get(ref.shiftedBy(-99.0)).intValue());
        Assert.assertEquals( 3, range.get(ref.shiftedBy(  4.9)).intValue());
        Assert.assertEquals( 5, range.get(ref.shiftedBy(  5.1)).intValue());
        Assert.assertEquals( 5, range.get(ref.shiftedBy(  8.9)).intValue());
        Assert.assertEquals( 5, range.get(ref.shiftedBy(  9.1)).intValue());
        Assert.assertEquals( 5, range.get(ref.shiftedBy(999.9)).intValue());
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
