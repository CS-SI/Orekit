/* Copyright 2002-2024 Luc Maisonobe
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

import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

public class SplitOffsetTest {

    @Test
    public void testLongsConstructor() {
        final SplitOffset so01 = new SplitOffset(1234L, 5678L);
        Assertions.assertEquals(1234L,               so01.getSeconds());
        Assertions.assertEquals(5678L,               so01.getAttoSeconds());
        Assertions.assertTrue(so01.isFinite());
        final SplitOffset so02 = new SplitOffset(1234L, -5678L);
        Assertions.assertEquals(1233L,               so02.getSeconds());
        Assertions.assertEquals(999999999999994322L, so02.getAttoSeconds());
        Assertions.assertTrue(so02.isFinite());
        final SplitOffset so03 = new SplitOffset(1234L, -1L);
        Assertions.assertEquals(1233L,               so03.getSeconds());
        Assertions.assertEquals(999999999999999999L, so03.getAttoSeconds());
        Assertions.assertTrue(so03.isFinite());
        final SplitOffset so04 = new SplitOffset(1234L, -9223372036854775808L);
        Assertions.assertEquals(1224L,               so04.getSeconds());
        Assertions.assertEquals(776627963145224192L, so04.getAttoSeconds());
        Assertions.assertTrue(so04.isFinite());
        final SplitOffset so05 = new SplitOffset(1234L, 9223372036854775807L);
        Assertions.assertEquals(1243L,               so05.getSeconds());
        Assertions.assertEquals(223372036854775807L, so05.getAttoSeconds());
        Assertions.assertTrue(so05.isFinite());
        final SplitOffset so06 = new SplitOffset(1234L, 0L);
        Assertions.assertEquals(1234L,               so06.getSeconds());
        Assertions.assertEquals(0L,                  so06.getAttoSeconds());
        Assertions.assertTrue(so06.isFinite());
        final SplitOffset so07 = new SplitOffset(-1234L, 5678L);
        Assertions.assertEquals(-1234L,              so07.getSeconds());
        Assertions.assertEquals(5678L,               so07.getAttoSeconds());
        Assertions.assertTrue(so07.isFinite());
        final SplitOffset so08 = new SplitOffset(-1234L, -5678L);
        Assertions.assertEquals(-1235L,              so08.getSeconds());
        Assertions.assertEquals(999999999999994322L, so08.getAttoSeconds());
        Assertions.assertTrue(so08.isFinite());
        final SplitOffset so09 = new SplitOffset(-1234L, -1L);
        Assertions.assertEquals(-1235L,              so09.getSeconds());
        Assertions.assertEquals(999999999999999999L, so09.getAttoSeconds());
        Assertions.assertTrue(so09.isFinite());
        final SplitOffset so10 = new SplitOffset(-1234L, -9223372036854775808L);
        Assertions.assertEquals(-1244L,              so10.getSeconds());
        Assertions.assertEquals(776627963145224192L, so10.getAttoSeconds());
        Assertions.assertTrue(so10.isFinite());
        final SplitOffset so11 = new SplitOffset(-1234L, 9223372036854775807L);
        Assertions.assertEquals(-1225L,              so11.getSeconds());
        Assertions.assertEquals(223372036854775807L, so11.getAttoSeconds());
        Assertions.assertTrue(so11.isFinite());
        final SplitOffset so12 = new SplitOffset(-1234L, 0L);
        Assertions.assertEquals(-1234L,              so12.getSeconds());
        Assertions.assertEquals(0L,                  so12.getAttoSeconds());
        Assertions.assertTrue(so12.isFinite());
    }

    @Test
    public void testOverflow() {
        Assertions.assertTrue(new SplitOffset(Long.MAX_VALUE,   999999999999999999L).isFinite());
        Assertions.assertTrue(new SplitOffset(Long.MAX_VALUE,  1000000000000000000L).isPositiveInfinity());
        Assertions.assertTrue(new SplitOffset(Long.MIN_VALUE,  0L).isFinite());
        Assertions.assertTrue(new SplitOffset(Long.MIN_VALUE, -1L).isNegativeInfinity());
    }

    @Test
    public void testPositiveDoubleContructor() {
        final double d = 123.4567890123456789;
        final SplitOffset so = new SplitOffset(d);
        Assertions.assertEquals(123L, so.getSeconds());
        // error is 1676 attosecond because the primitive double is not accurate enough
        Assertions.assertEquals(456789012345680576L, so.getAttoSeconds());
        Assertions.assertTrue(so.isFinite());
        Assertions.assertEquals(d, so.toDouble(), 1.0e-16);
    }

    @Test
    public void testNegativeDoubleContructor() {
        final double d = -123.4567890123456789;
        final SplitOffset so = new SplitOffset(d);
        Assertions.assertEquals(-124L, so.getSeconds());
        // error is 1676 attosecond because the primitive double is not accurate enough
        Assertions.assertEquals(543210987654319424L, so.getAttoSeconds());
        Assertions.assertTrue(so.isFinite());
        Assertions.assertEquals(d, so.toDouble(), 1.0e-16);
    }

    @Test
    public void testSmallNegativeDoubleContructor() {
        final SplitOffset so = new SplitOffset(-1.0e-17);
        Assertions.assertEquals(-1L, so.getSeconds());
        Assertions.assertEquals(999999999999999990L, so.getAttoSeconds());
        Assertions.assertTrue(so.isFinite());
    }

    @Test
    public void testNaNDouble() {

        final SplitOffset nan = new SplitOffset(Double.NaN);
        Assertions.assertEquals(0L, nan.getSeconds());
        Assertions.assertTrue(nan.isNaN());
        Assertions.assertFalse(nan.isInfinite());
        Assertions.assertEquals(Long.MAX_VALUE, nan.getRoundedOffset(TimeUnit.DAYS));
        Assertions.assertTrue(Double.isNaN(nan.toDouble()));

        Assertions.assertEquals(0L, SplitOffset.NaN.getSeconds());
        Assertions.assertTrue(SplitOffset.NaN.isNaN());
        Assertions.assertFalse(SplitOffset.NaN.isInfinite());
        Assertions.assertEquals(Long.MAX_VALUE, SplitOffset.NaN.getRoundedOffset(TimeUnit.DAYS));
        Assertions.assertTrue(Double.isNaN(SplitOffset.NaN.toDouble()));

    }

    @Test
    public void testPositiveInfinity() {

        final SplitOffset pos = new SplitOffset(Double.POSITIVE_INFINITY);
        Assertions.assertEquals(Long.MAX_VALUE, pos.getSeconds());
        Assertions.assertFalse(pos.isNaN());
        Assertions.assertTrue(pos.isInfinite());
        Assertions.assertFalse(pos.isNegativeInfinity());
        Assertions.assertTrue(pos.isPositiveInfinity());
        Assertions.assertEquals(Long.MAX_VALUE, pos.getRoundedOffset(TimeUnit.DAYS));
        Assertions.assertTrue(Double.isInfinite(pos.toDouble()));
        Assertions.assertTrue(pos.toDouble() > 0);

        Assertions.assertEquals(Long.MAX_VALUE, SplitOffset.POSITIVE_INFINITY.getSeconds());
        Assertions.assertFalse(SplitOffset.POSITIVE_INFINITY.isNaN());
        Assertions.assertTrue(SplitOffset.POSITIVE_INFINITY.isInfinite());
        Assertions.assertFalse(SplitOffset.POSITIVE_INFINITY.isNegativeInfinity());
        Assertions.assertTrue(SplitOffset.POSITIVE_INFINITY.isPositiveInfinity());
        Assertions.assertEquals(Long.MAX_VALUE, SplitOffset.POSITIVE_INFINITY.getRoundedOffset(TimeUnit.DAYS));
        Assertions.assertTrue(Double.isInfinite(SplitOffset.POSITIVE_INFINITY.toDouble()));
        Assertions.assertTrue(SplitOffset.POSITIVE_INFINITY.toDouble() > 0);

    }

    @Test
    public void testNegativeInfinity() {

        final SplitOffset neg = new SplitOffset(Double.NEGATIVE_INFINITY);
        Assertions.assertEquals(Long.MIN_VALUE, neg.getSeconds());
        Assertions.assertFalse(neg.isNaN());
        Assertions.assertTrue(neg.isInfinite());
        Assertions.assertTrue(neg.isNegativeInfinity());
        Assertions.assertFalse(neg.isPositiveInfinity());
        Assertions.assertEquals(Long.MIN_VALUE, neg.getRoundedOffset(TimeUnit.DAYS));
        Assertions.assertTrue(Double.isInfinite(neg.toDouble()));
        Assertions.assertTrue(neg.toDouble() < 0);

        Assertions.assertEquals(Long.MIN_VALUE, SplitOffset.NEGATIVE_INFINITY.getSeconds());
        Assertions.assertFalse(SplitOffset.NEGATIVE_INFINITY.isNaN());
        Assertions.assertTrue(SplitOffset.NEGATIVE_INFINITY.isInfinite());
        Assertions.assertTrue(SplitOffset.NEGATIVE_INFINITY.isNegativeInfinity());
        Assertions.assertFalse(SplitOffset.NEGATIVE_INFINITY.isPositiveInfinity());
        Assertions.assertEquals(Long.MIN_VALUE, SplitOffset.NEGATIVE_INFINITY.getRoundedOffset(TimeUnit.DAYS));
        Assertions.assertTrue(Double.isInfinite(SplitOffset.NEGATIVE_INFINITY.toDouble()));
        Assertions.assertTrue(SplitOffset.NEGATIVE_INFINITY.toDouble() < 0);

    }

    @Test
    public void testOutOfRangeDouble() {

        final double limit = (double) Long.MAX_VALUE;

        final SplitOffset plus = new SplitOffset( limit);
        Assertions.assertEquals( limit, plus.getRoundedOffset(TimeUnit.SECONDS), 1.0e-15 * limit);
        Assertions.assertFalse(plus.isNaN());
        Assertions.assertFalse(plus.isInfinite());

        final SplitOffset minus = new SplitOffset(-limit);
        Assertions.assertEquals(-limit, minus.getRoundedOffset(TimeUnit.SECONDS), 1.0e-15 * limit);
        Assertions.assertFalse(minus.isNaN());
        Assertions.assertFalse(minus.isInfinite());

        final SplitOffset afterPlus = new SplitOffset(FastMath.nextAfter( limit, Double.POSITIVE_INFINITY));
        Assertions.assertFalse(afterPlus.isNaN());
        Assertions.assertTrue(afterPlus.isInfinite());
        Assertions.assertTrue(afterPlus.getSeconds() > 0L);
        Assertions.assertFalse(SplitOffset.POSITIVE_INFINITY.isNaN());
        Assertions.assertTrue(SplitOffset.POSITIVE_INFINITY.isInfinite());
        Assertions.assertTrue(SplitOffset.POSITIVE_INFINITY.getSeconds() > 0L);

        final SplitOffset beforeMinus = new SplitOffset(FastMath.nextAfter(-limit, Double.NEGATIVE_INFINITY));
        Assertions.assertFalse(beforeMinus.isNaN());
        Assertions.assertTrue(beforeMinus.isInfinite());
        Assertions.assertTrue(beforeMinus.getSeconds() < 0L);
        Assertions.assertFalse(beforeMinus.isNaN());
        Assertions.assertTrue(beforeMinus.isInfinite());
        Assertions.assertFalse(SplitOffset.NEGATIVE_INFINITY.isNaN());
        Assertions.assertTrue(SplitOffset.NEGATIVE_INFINITY.isInfinite());
        Assertions.assertTrue(SplitOffset.NEGATIVE_INFINITY.getSeconds() < 0L);

    }

    @Test
    public void testSumConstructor() {
        final SplitOffset so = new SplitOffset(new SplitOffset( 1L,  2L),
                                               new SplitOffset( 3L,  4L),
                                               new SplitOffset( 5L,  6L),
                                               new SplitOffset( 7L,  8L),
                                               new SplitOffset( 9L, 10L),
                                               new SplitOffset(11L, 12L));
        Assertions.assertEquals(36L, so.getSeconds());
        Assertions.assertEquals(42L, so.getAttoSeconds());
    }

    @Test
    public void testDaysTimeUnit() {

        final SplitOffset days = new SplitOffset(2, TimeUnit.DAYS);
        Assertions.assertEquals(172800L, days.getSeconds());
        Assertions.assertEquals(0L,      days.getAttoSeconds());
        Assertions.assertTrue(days.isFinite());

    }

    @Test
    public void testOutOfRangeDays() {
        doTestOutOfRange(106751991167300L, TimeUnit.DAYS);
    }

    @Test
    public void testHoursTimeUnit() {

        final SplitOffset hours = new SplitOffset(2, TimeUnit.HOURS);
        Assertions.assertEquals(7200L, hours.getSeconds());
        Assertions.assertEquals(0L,    hours.getAttoSeconds());
        Assertions.assertTrue(hours.isFinite());

    }

    @Test
    public void testOutOfRangeHours() {
        doTestOutOfRange(2562047788015215L, TimeUnit.HOURS);
    }

    @Test
    public void testMinutesTimeUnit() {

        final SplitOffset minutes = new SplitOffset(2, TimeUnit.MINUTES);
        Assertions.assertEquals(120L, minutes.getSeconds());
        Assertions.assertEquals(0L,   minutes.getAttoSeconds());
        Assertions.assertTrue(minutes.isFinite());

    }

    @Test
    public void testOutOfRangeMinutes() {
        doTestOutOfRange(153722867280912929L, TimeUnit.MINUTES);
    }

    @Test
    public void testSecondsTimeUnit() {

        final SplitOffset seconds = new SplitOffset(2, TimeUnit.SECONDS);
        Assertions.assertEquals(2L, seconds.getSeconds());
        Assertions.assertEquals(0L, seconds.getAttoSeconds());
        Assertions.assertTrue(seconds.isFinite());

    }

    @Test
    public void testMilliSecondsTimeUnit() {

        final SplitOffset milliSeconds = new SplitOffset(2, TimeUnit.MILLISECONDS);
        Assertions.assertEquals(0L,                milliSeconds.getSeconds());
        Assertions.assertEquals(2000000000000000L, milliSeconds.getAttoSeconds());
        Assertions.assertTrue(milliSeconds.isFinite());

    }

    @Test
    public void testMicroSecondsTimeUnit() {

        final SplitOffset microSeconds = new SplitOffset(2, TimeUnit.MICROSECONDS);
        Assertions.assertEquals(0L,             microSeconds.getSeconds());
        Assertions.assertEquals(2000000000000L, microSeconds.getAttoSeconds());
        Assertions.assertTrue(microSeconds.isFinite());

    }

    @Test
    public void testNanoTimeUnit() {

        final SplitOffset nanoSeconds = new SplitOffset(2, TimeUnit.NANOSECONDS);
        Assertions.assertEquals(0L,          nanoSeconds.getSeconds());
        Assertions.assertEquals(2000000000L, nanoSeconds.getAttoSeconds());
        Assertions.assertTrue(nanoSeconds.isFinite());

    }

    private void doTestOutOfRange(final long offset, final TimeUnit unit) {

        final SplitOffset plus = new SplitOffset( offset, unit);
        Assertions.assertEquals( offset, plus.getRoundedOffset(unit));
        Assertions.assertTrue(plus.isFinite());
        Assertions.assertFalse(plus.isNaN());
        Assertions.assertFalse(plus.isInfinite());

        final SplitOffset minus = new SplitOffset(-offset, unit);
        Assertions.assertEquals(-offset, minus.getRoundedOffset(unit));
        Assertions.assertTrue(minus.isFinite());
        Assertions.assertFalse(minus.isNaN());
        Assertions.assertFalse(minus.isInfinite());

        final SplitOffset afterPlus = new SplitOffset(offset + 1L, unit);
        Assertions.assertEquals(Long.MAX_VALUE, afterPlus.getRoundedOffset(unit));
        Assertions.assertFalse(afterPlus.isFinite());
        Assertions.assertFalse(afterPlus.isNaN());
        Assertions.assertTrue(afterPlus.isInfinite());

        final SplitOffset beforeMinus = new SplitOffset(-offset - 1L, unit);
        Assertions.assertEquals(Long.MIN_VALUE, beforeMinus.getRoundedOffset(unit));
        Assertions.assertFalse(beforeMinus.isFinite());
        Assertions.assertFalse(beforeMinus.isNaN());
        Assertions.assertTrue(beforeMinus.isInfinite());

    }

    @Test
    public void testOffsetSymmetry() {

         for (long offset : new long[] {
            Long.MIN_VALUE,
            -4000000000000000001L, -4000000000000000000L, -3999999999999999999L,
            -3000000000000000001L, -3000000000000000000L, -2999999999999999999L,
            -2000000000000000001L, -2000000000000000000L, -1999999999999999999L,
            -153722867280912930L, -153722867280912929L,
            -2562047788015216L, -2562047788015215L,
            -106751991167301L, -106751991167300L,
            -86401L, -86400L, -86399L, -43201L, -43200L, -43199L,
            -3601L, -3600L, -3599L, -1801L, -1800L, -1799L,
            -61L, -60L, -59L, -31L, -30L, -29L,
            -1L, 0L, 1L,
            29L, 30L, 31L, 59L, 60L, 61L,
            1799L, 1800L, 1801L, 3599L, 3600L, 3601L,
            43199L, 43200L, 43201L, 86399L, 86400L, 86401L,
            106751991167300L, 106751991167301L,
            2562047788015215L, 2562047788015216L,
            153722867280912929L, 153722867280912930L,
            1999999999999999999L, 2000000000000000000L, 2000000000000000001L,
            2999999999999999999L, 3000000000000000000L, 3000000000000000001L,
            3999999999999999999L, 4000000000000000000L, 4000000000000000001L,
            Long.MAX_VALUE
        }) {
            for (final TimeUnit tu : TimeUnit.values()) {
                final SplitOffset splitOffset = new SplitOffset(offset, tu);
                final long rebuilt = splitOffset.getRoundedOffset(tu);
                if (splitOffset.isInfinite()) {
                    Assertions.assertEquals(offset < 0 ? Long.MIN_VALUE : Long.MAX_VALUE, rebuilt);
                } else {
                    Assertions.assertEquals(offset, rebuilt);
                }
            }
        }
    }

    @Test
    public void testAdd() {
        final SplitOffset add1 = SplitOffset.add(new SplitOffset(1234L, 5678L),
                                                new SplitOffset(76L, -876L));
        Assertions.assertEquals(1310L, add1.getSeconds());
        Assertions.assertEquals(4802L, add1.getAttoSeconds());
        Assertions.assertTrue(add1.isFinite());

        final SplitOffset add2 = SplitOffset.add(new SplitOffset(1L, 500000000000000000L),
                                                new SplitOffset(2L, 500000000000000001L));
        Assertions.assertEquals(4L, add2.getSeconds());
        Assertions.assertEquals(1L, add2.getAttoSeconds());
        Assertions.assertTrue(add2.isFinite());

        final SplitOffset add3 = SplitOffset.add(new SplitOffset(1234L, 23L),
                                                new SplitOffset(76L, -876L));
        Assertions.assertEquals(1309L,               add3.getSeconds());
        Assertions.assertEquals(999999999999999147L, add3.getAttoSeconds());
        Assertions.assertTrue(add3.isFinite());
    }

    @Test
    public void testAddSpecialValues() {

        Assertions.assertTrue(SplitOffset.add(SplitOffset.NaN,               SplitOffset.HOUR).isNaN());
        Assertions.assertTrue(SplitOffset.add(SplitOffset.HOUR,              SplitOffset.NaN).isNaN());
        Assertions.assertTrue(SplitOffset.add(SplitOffset.POSITIVE_INFINITY, SplitOffset.HOUR).isPositiveInfinity());
        Assertions.assertTrue(SplitOffset.add(SplitOffset.HOUR,              SplitOffset.POSITIVE_INFINITY).isPositiveInfinity());
        Assertions.assertTrue(SplitOffset.add(SplitOffset.NEGATIVE_INFINITY, SplitOffset.HOUR).isNegativeInfinity());
        Assertions.assertTrue(SplitOffset.add(SplitOffset.HOUR,              SplitOffset.NEGATIVE_INFINITY).isNegativeInfinity());

        Assertions.assertTrue(SplitOffset.add(SplitOffset.NaN,               SplitOffset.NaN).isNaN());
        Assertions.assertTrue(SplitOffset.add(SplitOffset.NaN,               SplitOffset.NaN).isNaN());
        Assertions.assertTrue(SplitOffset.add(SplitOffset.POSITIVE_INFINITY, SplitOffset.NaN).isNaN());
        Assertions.assertTrue(SplitOffset.add(SplitOffset.NaN,               SplitOffset.POSITIVE_INFINITY).isNaN());
        Assertions.assertTrue(SplitOffset.add(SplitOffset.NEGATIVE_INFINITY, SplitOffset.NaN).isNaN());
        Assertions.assertTrue(SplitOffset.add(SplitOffset.NaN,               SplitOffset.NEGATIVE_INFINITY).isNaN());

        Assertions.assertTrue(SplitOffset.add(SplitOffset.NaN,               SplitOffset.POSITIVE_INFINITY).isNaN());
        Assertions.assertTrue(SplitOffset.add(SplitOffset.POSITIVE_INFINITY, SplitOffset.NaN).isNaN());
        Assertions.assertTrue(SplitOffset.add(SplitOffset.POSITIVE_INFINITY, SplitOffset.POSITIVE_INFINITY).isPositiveInfinity());
        Assertions.assertTrue(SplitOffset.add(SplitOffset.NEGATIVE_INFINITY, SplitOffset.POSITIVE_INFINITY).isNaN());
        Assertions.assertTrue(SplitOffset.add(SplitOffset.POSITIVE_INFINITY, SplitOffset.NEGATIVE_INFINITY).isNaN());

        Assertions.assertTrue(SplitOffset.add(SplitOffset.NaN,               SplitOffset.NEGATIVE_INFINITY).isNaN());
        Assertions.assertTrue(SplitOffset.add(SplitOffset.NEGATIVE_INFINITY, SplitOffset.NaN).isNaN());
        Assertions.assertTrue(SplitOffset.add(SplitOffset.POSITIVE_INFINITY, SplitOffset.NEGATIVE_INFINITY).isNaN());
        Assertions.assertTrue(SplitOffset.add(SplitOffset.NEGATIVE_INFINITY, SplitOffset.POSITIVE_INFINITY).isNaN());
        Assertions.assertTrue(SplitOffset.add(SplitOffset.NEGATIVE_INFINITY, SplitOffset.NEGATIVE_INFINITY).isNegativeInfinity());

    }

    @Test
    public void testSubtract() {
        final SplitOffset sub1 = SplitOffset.subtract(new SplitOffset(1234L, 5678L),
                                                      new SplitOffset(76L, 876L));
        Assertions.assertEquals(1158L, sub1.getSeconds());
        Assertions.assertEquals(4802L, sub1.getAttoSeconds());
        Assertions.assertTrue(sub1.isFinite());

        final SplitOffset sub2 = SplitOffset.subtract(new SplitOffset(1L, 0L),
                                                      new SplitOffset(2L, 1L));
        Assertions.assertEquals(-2L, sub2.getSeconds());
        Assertions.assertEquals(999999999999999999L, sub2.getAttoSeconds());
        Assertions.assertTrue(sub2.isFinite());

        final SplitOffset sub3 = SplitOffset.subtract(new SplitOffset(1234L, 999999999999999999L),
                                                      new SplitOffset(76L, 123456L));
        Assertions.assertEquals(1158L,               sub3.getSeconds());
        Assertions.assertEquals(999999999999876543L, sub3.getAttoSeconds());
        Assertions.assertTrue(sub3.isFinite());
    }

    @Test
    public void testSubtractSpecialValues() {

        Assertions.assertTrue(SplitOffset.subtract(SplitOffset.NaN,               SplitOffset.HOUR).isNaN());
        Assertions.assertTrue(SplitOffset.subtract(SplitOffset.HOUR,              SplitOffset.NaN).isNaN());
        Assertions.assertTrue(SplitOffset.subtract(SplitOffset.POSITIVE_INFINITY, SplitOffset.HOUR).isPositiveInfinity());
        Assertions.assertTrue(SplitOffset.subtract(SplitOffset.HOUR,              SplitOffset.POSITIVE_INFINITY).isNegativeInfinity());
        Assertions.assertTrue(SplitOffset.subtract(SplitOffset.NEGATIVE_INFINITY, SplitOffset.HOUR).isNegativeInfinity());
        Assertions.assertTrue(SplitOffset.subtract(SplitOffset.HOUR,              SplitOffset.NEGATIVE_INFINITY).isPositiveInfinity());

        Assertions.assertTrue(SplitOffset.subtract(SplitOffset.NaN,               SplitOffset.NaN).isNaN());
        Assertions.assertTrue(SplitOffset.subtract(SplitOffset.NaN,               SplitOffset.NaN).isNaN());
        Assertions.assertTrue(SplitOffset.subtract(SplitOffset.POSITIVE_INFINITY, SplitOffset.NaN).isNaN());
        Assertions.assertTrue(SplitOffset.subtract(SplitOffset.NaN,               SplitOffset.POSITIVE_INFINITY).isNaN());
        Assertions.assertTrue(SplitOffset.subtract(SplitOffset.NEGATIVE_INFINITY, SplitOffset.NaN).isNaN());
        Assertions.assertTrue(SplitOffset.subtract(SplitOffset.NaN,               SplitOffset.NEGATIVE_INFINITY).isNaN());

        Assertions.assertTrue(SplitOffset.subtract(SplitOffset.NaN,               SplitOffset.POSITIVE_INFINITY).isNaN());
        Assertions.assertTrue(SplitOffset.subtract(SplitOffset.POSITIVE_INFINITY, SplitOffset.NaN).isNaN());
        Assertions.assertTrue(SplitOffset.subtract(SplitOffset.POSITIVE_INFINITY, SplitOffset.POSITIVE_INFINITY).isNaN());
        Assertions.assertTrue(SplitOffset.subtract(SplitOffset.NEGATIVE_INFINITY, SplitOffset.POSITIVE_INFINITY).isNegativeInfinity());
        Assertions.assertTrue(SplitOffset.subtract(SplitOffset.POSITIVE_INFINITY, SplitOffset.NEGATIVE_INFINITY).isPositiveInfinity());

        Assertions.assertTrue(SplitOffset.subtract(SplitOffset.NaN,               SplitOffset.NEGATIVE_INFINITY).isNaN());
        Assertions.assertTrue(SplitOffset.subtract(SplitOffset.NEGATIVE_INFINITY, SplitOffset.NaN).isNaN());
        Assertions.assertTrue(SplitOffset.subtract(SplitOffset.POSITIVE_INFINITY, SplitOffset.NEGATIVE_INFINITY).isPositiveInfinity());
        Assertions.assertTrue(SplitOffset.subtract(SplitOffset.NEGATIVE_INFINITY, SplitOffset.POSITIVE_INFINITY).isNegativeInfinity());
        Assertions.assertTrue(SplitOffset.subtract(SplitOffset.NEGATIVE_INFINITY, SplitOffset.NEGATIVE_INFINITY).isNaN());

    }

    @Test
    public void testCompareFinite() {
        Assertions.assertTrue(SplitOffset.ZERO.compareTo(new SplitOffset(0L, 0L)) == 0);
        Assertions.assertTrue(new SplitOffset(1L, 1L).compareTo(new SplitOffset(1L, 1L)) == 0);
        Assertions.assertTrue(SplitOffset.MICROSECOND.compareTo(SplitOffset.MILLISECOND) < 0);
        Assertions.assertTrue(SplitOffset.MILLISECOND.compareTo(SplitOffset.MICROSECOND) > 0);
    }

    @Test
    public void testCompareSpecialCases() {

        Assertions.assertTrue(SplitOffset.NaN.compareTo(SplitOffset.NaN)               == 0);
        Assertions.assertTrue(SplitOffset.NaN.compareTo(SplitOffset.ATTOSECOND)        >  0);
        Assertions.assertTrue(SplitOffset.NaN.compareTo(SplitOffset.POSITIVE_INFINITY) >  0);
        Assertions.assertTrue(SplitOffset.NaN.compareTo(SplitOffset.NEGATIVE_INFINITY) >  0);

        Assertions.assertTrue(SplitOffset.ATTOSECOND.compareTo(SplitOffset.NaN)               <  0);
        Assertions.assertTrue(SplitOffset.ATTOSECOND.compareTo(SplitOffset.ATTOSECOND)        == 0);
        Assertions.assertTrue(SplitOffset.ATTOSECOND.compareTo(SplitOffset.POSITIVE_INFINITY) <  0);
        Assertions.assertTrue(SplitOffset.ATTOSECOND.compareTo(SplitOffset.NEGATIVE_INFINITY) >  0);

        Assertions.assertTrue(SplitOffset.POSITIVE_INFINITY.compareTo(SplitOffset.NaN)               <  0);
        Assertions.assertTrue(SplitOffset.POSITIVE_INFINITY.compareTo(SplitOffset.ATTOSECOND)        >  0);
        Assertions.assertTrue(SplitOffset.POSITIVE_INFINITY.compareTo(SplitOffset.POSITIVE_INFINITY) == 0);
        Assertions.assertTrue(SplitOffset.POSITIVE_INFINITY.compareTo(SplitOffset.NEGATIVE_INFINITY) >  0);

        Assertions.assertTrue(SplitOffset.NEGATIVE_INFINITY.compareTo(SplitOffset.NaN)               <  0);
        Assertions.assertTrue(SplitOffset.NEGATIVE_INFINITY.compareTo(SplitOffset.ATTOSECOND)        <  0);
        Assertions.assertTrue(SplitOffset.NEGATIVE_INFINITY.compareTo(SplitOffset.POSITIVE_INFINITY) <  0);
        Assertions.assertTrue(SplitOffset.NEGATIVE_INFINITY.compareTo(SplitOffset.NEGATIVE_INFINITY) == 0);

    }

    @Test
    public void testMultiples() {
        for (int n = 0; n < 100; ++n) {
            checkMultiple(n, SplitOffset.ZERO, SplitOffset.ZERO);
        }
        checkMultiple(1000, SplitOffset.ATTOSECOND,  SplitOffset.FEMTOSECOND);
        checkMultiple(1000, SplitOffset.FEMTOSECOND, SplitOffset.PICOSECOND);
        checkMultiple(1000, SplitOffset.PICOSECOND,  SplitOffset.NANOSECOND);
        checkMultiple(1000, SplitOffset.NANOSECOND,  SplitOffset.MICROSECOND);
        checkMultiple(1000, SplitOffset.MICROSECOND, SplitOffset.MILLISECOND);
        checkMultiple(1000, SplitOffset.MILLISECOND, SplitOffset.SECOND);
        checkMultiple(  60, SplitOffset.SECOND,      SplitOffset.MINUTE);
        checkMultiple(  60, SplitOffset.MINUTE,      SplitOffset.HOUR);
        checkMultiple(  24, SplitOffset.HOUR,        SplitOffset.DAY);
    }

    private void checkMultiple(final int n, final SplitOffset small, final SplitOffset large) {
        SplitOffset sum = SplitOffset.ZERO;
        for (int i = 0; i < n; ++i) {
            sum = SplitOffset.add(sum, small);
        }
        Assertions.assertTrue(SplitOffset.subtract(sum, large).isZero());
    }

}
