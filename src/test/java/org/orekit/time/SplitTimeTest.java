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

public class SplitTimeTest {

    @Test
    public void testLongsConstructor() {
        final SplitTime so01 = new SplitTime(1234L, 5678L);
        Assertions.assertEquals(1234L,               so01.getSeconds());
        Assertions.assertEquals(5678L,               so01.getAttoSeconds());
        Assertions.assertTrue(so01.isFinite());
        final SplitTime so02 = new SplitTime(1234L, -5678L);
        Assertions.assertEquals(1233L,               so02.getSeconds());
        Assertions.assertEquals(999999999999994322L, so02.getAttoSeconds());
        Assertions.assertTrue(so02.isFinite());
        final SplitTime so03 = new SplitTime(1234L, -1L);
        Assertions.assertEquals(1233L,               so03.getSeconds());
        Assertions.assertEquals(999999999999999999L, so03.getAttoSeconds());
        Assertions.assertTrue(so03.isFinite());
        final SplitTime so04 = new SplitTime(1234L, -9223372036854775808L);
        Assertions.assertEquals(1224L,               so04.getSeconds());
        Assertions.assertEquals(776627963145224192L, so04.getAttoSeconds());
        Assertions.assertTrue(so04.isFinite());
        final SplitTime so05 = new SplitTime(1234L, 9223372036854775807L);
        Assertions.assertEquals(1243L,               so05.getSeconds());
        Assertions.assertEquals(223372036854775807L, so05.getAttoSeconds());
        Assertions.assertTrue(so05.isFinite());
        final SplitTime so06 = new SplitTime(1234L, 0L);
        Assertions.assertEquals(1234L,               so06.getSeconds());
        Assertions.assertEquals(0L,                  so06.getAttoSeconds());
        Assertions.assertTrue(so06.isFinite());
        final SplitTime so07 = new SplitTime(-1234L, 5678L);
        Assertions.assertEquals(-1234L,              so07.getSeconds());
        Assertions.assertEquals(5678L,               so07.getAttoSeconds());
        Assertions.assertTrue(so07.isFinite());
        final SplitTime so08 = new SplitTime(-1234L, -5678L);
        Assertions.assertEquals(-1235L,              so08.getSeconds());
        Assertions.assertEquals(999999999999994322L, so08.getAttoSeconds());
        Assertions.assertTrue(so08.isFinite());
        final SplitTime so09 = new SplitTime(-1234L, -1L);
        Assertions.assertEquals(-1235L,              so09.getSeconds());
        Assertions.assertEquals(999999999999999999L, so09.getAttoSeconds());
        Assertions.assertTrue(so09.isFinite());
        final SplitTime so10 = new SplitTime(-1234L, -9223372036854775808L);
        Assertions.assertEquals(-1244L,              so10.getSeconds());
        Assertions.assertEquals(776627963145224192L, so10.getAttoSeconds());
        Assertions.assertTrue(so10.isFinite());
        final SplitTime so11 = new SplitTime(-1234L, 9223372036854775807L);
        Assertions.assertEquals(-1225L,              so11.getSeconds());
        Assertions.assertEquals(223372036854775807L, so11.getAttoSeconds());
        Assertions.assertTrue(so11.isFinite());
        final SplitTime so12 = new SplitTime(-1234L, 0L);
        Assertions.assertEquals(-1234L,              so12.getSeconds());
        Assertions.assertEquals(0L,                  so12.getAttoSeconds());
        Assertions.assertTrue(so12.isFinite());
    }

    @Test
    public void testOverflow() {
        Assertions.assertTrue(new SplitTime(Long.MAX_VALUE, 999999999999999999L).isFinite());
        Assertions.assertTrue(new SplitTime(Long.MAX_VALUE, 1000000000000000000L).isPositiveInfinity());
        Assertions.assertTrue(new SplitTime(Long.MIN_VALUE, 0L).isFinite());
        Assertions.assertTrue(new SplitTime(Long.MIN_VALUE, -1L).isNegativeInfinity());
    }

    @Test
    public void testPositiveDoubleContructor() {
        final double d = 123.4567890123456789;
        final SplitTime so = new SplitTime(d);
        Assertions.assertEquals(123L, so.getSeconds());
        // error is 1676 attosecond because the primitive double is not accurate enough
        Assertions.assertEquals(456789012345680576L, so.getAttoSeconds());
        Assertions.assertTrue(so.isFinite());
        Assertions.assertEquals(d, so.toDouble(), 1.0e-16);
    }

    @Test
    public void testNegativeDoubleContructor() {
        final double d = -123.4567890123456789;
        final SplitTime so = new SplitTime(d);
        Assertions.assertEquals(-124L, so.getSeconds());
        // error is 1676 attosecond because the primitive double is not accurate enough
        Assertions.assertEquals(543210987654319424L, so.getAttoSeconds());
        Assertions.assertTrue(so.isFinite());
        Assertions.assertEquals(d, so.toDouble(), 1.0e-16);
    }

    @Test
    public void testSmallNegativeDoubleContructor() {
        final SplitTime so = new SplitTime(-1.0e-17);
        Assertions.assertEquals(-1L, so.getSeconds());
        Assertions.assertEquals(999999999999999990L, so.getAttoSeconds());
        Assertions.assertTrue(so.isFinite());
    }

    @Test
    public void testNaNDouble() {

        final SplitTime nan = new SplitTime(Double.NaN);
        Assertions.assertEquals(0L, nan.getSeconds());
        Assertions.assertTrue(nan.isNaN());
        Assertions.assertFalse(nan.isInfinite());
        Assertions.assertEquals(Long.MAX_VALUE, nan.getRoundedTime(TimeUnit.DAYS));
        Assertions.assertTrue(Double.isNaN(nan.toDouble()));

        Assertions.assertEquals(0L, SplitTime.NaN.getSeconds());
        Assertions.assertTrue(SplitTime.NaN.isNaN());
        Assertions.assertFalse(SplitTime.NaN.isInfinite());
        Assertions.assertEquals(Long.MAX_VALUE, SplitTime.NaN.getRoundedTime(TimeUnit.DAYS));
        Assertions.assertTrue(Double.isNaN(SplitTime.NaN.toDouble()));

    }

    @Test
    public void testPositiveInfinity() {

        final SplitTime pos = new SplitTime(Double.POSITIVE_INFINITY);
        Assertions.assertEquals(Long.MAX_VALUE, pos.getSeconds());
        Assertions.assertFalse(pos.isNaN());
        Assertions.assertTrue(pos.isInfinite());
        Assertions.assertFalse(pos.isNegativeInfinity());
        Assertions.assertTrue(pos.isPositiveInfinity());
        Assertions.assertEquals(Long.MAX_VALUE, pos.getRoundedTime(TimeUnit.DAYS));
        Assertions.assertTrue(Double.isInfinite(pos.toDouble()));
        Assertions.assertTrue(pos.toDouble() > 0);

        Assertions.assertEquals(Long.MAX_VALUE, SplitTime.POSITIVE_INFINITY.getSeconds());
        Assertions.assertFalse(SplitTime.POSITIVE_INFINITY.isNaN());
        Assertions.assertTrue(SplitTime.POSITIVE_INFINITY.isInfinite());
        Assertions.assertFalse(SplitTime.POSITIVE_INFINITY.isNegativeInfinity());
        Assertions.assertTrue(SplitTime.POSITIVE_INFINITY.isPositiveInfinity());
        Assertions.assertEquals(Long.MAX_VALUE, SplitTime.POSITIVE_INFINITY.getRoundedTime(TimeUnit.DAYS));
        Assertions.assertTrue(Double.isInfinite(SplitTime.POSITIVE_INFINITY.toDouble()));
        Assertions.assertTrue(SplitTime.POSITIVE_INFINITY.toDouble() > 0);

    }

    @Test
    public void testNegativeInfinity() {

        final SplitTime neg = new SplitTime(Double.NEGATIVE_INFINITY);
        Assertions.assertEquals(Long.MIN_VALUE, neg.getSeconds());
        Assertions.assertFalse(neg.isNaN());
        Assertions.assertTrue(neg.isInfinite());
        Assertions.assertTrue(neg.isNegativeInfinity());
        Assertions.assertFalse(neg.isPositiveInfinity());
        Assertions.assertEquals(Long.MIN_VALUE, neg.getRoundedTime(TimeUnit.DAYS));
        Assertions.assertTrue(Double.isInfinite(neg.toDouble()));
        Assertions.assertTrue(neg.toDouble() < 0);

        Assertions.assertEquals(Long.MIN_VALUE, SplitTime.NEGATIVE_INFINITY.getSeconds());
        Assertions.assertFalse(SplitTime.NEGATIVE_INFINITY.isNaN());
        Assertions.assertTrue(SplitTime.NEGATIVE_INFINITY.isInfinite());
        Assertions.assertTrue(SplitTime.NEGATIVE_INFINITY.isNegativeInfinity());
        Assertions.assertFalse(SplitTime.NEGATIVE_INFINITY.isPositiveInfinity());
        Assertions.assertEquals(Long.MIN_VALUE, SplitTime.NEGATIVE_INFINITY.getRoundedTime(TimeUnit.DAYS));
        Assertions.assertTrue(Double.isInfinite(SplitTime.NEGATIVE_INFINITY.toDouble()));
        Assertions.assertTrue(SplitTime.NEGATIVE_INFINITY.toDouble() < 0);

    }

    @Test
    public void testOutOfRangeDouble() {

        final double limit = (double) Long.MAX_VALUE;

        final SplitTime plus = new SplitTime(limit);
        Assertions.assertEquals(limit, plus.getRoundedTime(TimeUnit.SECONDS), 1.0e-15 * limit);
        Assertions.assertFalse(plus.isNaN());
        Assertions.assertFalse(plus.isInfinite());

        final SplitTime minus = new SplitTime(-limit);
        Assertions.assertEquals(-limit, minus.getRoundedTime(TimeUnit.SECONDS), 1.0e-15 * limit);
        Assertions.assertFalse(minus.isNaN());
        Assertions.assertFalse(minus.isInfinite());

        final SplitTime afterPlus = new SplitTime(FastMath.nextAfter(limit, Double.POSITIVE_INFINITY));
        Assertions.assertFalse(afterPlus.isNaN());
        Assertions.assertTrue(afterPlus.isInfinite());
        Assertions.assertTrue(afterPlus.getSeconds() > 0L);
        Assertions.assertFalse(SplitTime.POSITIVE_INFINITY.isNaN());
        Assertions.assertTrue(SplitTime.POSITIVE_INFINITY.isInfinite());
        Assertions.assertTrue(SplitTime.POSITIVE_INFINITY.getSeconds() > 0L);

        final SplitTime beforeMinus = new SplitTime(FastMath.nextAfter(-limit, Double.NEGATIVE_INFINITY));
        Assertions.assertFalse(beforeMinus.isNaN());
        Assertions.assertTrue(beforeMinus.isInfinite());
        Assertions.assertTrue(beforeMinus.getSeconds() < 0L);
        Assertions.assertFalse(beforeMinus.isNaN());
        Assertions.assertTrue(beforeMinus.isInfinite());
        Assertions.assertFalse(SplitTime.NEGATIVE_INFINITY.isNaN());
        Assertions.assertTrue(SplitTime.NEGATIVE_INFINITY.isInfinite());
        Assertions.assertTrue(SplitTime.NEGATIVE_INFINITY.getSeconds() < 0L);

    }

    @Test
    public void testSumConstructor() {
        final SplitTime so = new SplitTime(new SplitTime(1L, 2L),
                                           new SplitTime(3L, 4L),
                                           new SplitTime(5L, 6L),
                                           new SplitTime(7L, 8L),
                                           new SplitTime(9L, 10L),
                                           new SplitTime(11L, 12L));
        Assertions.assertEquals(36L, so.getSeconds());
        Assertions.assertEquals(42L, so.getAttoSeconds());
    }

    @Test
    public void testDaysTimeUnit() {

        final SplitTime days = new SplitTime(2, TimeUnit.DAYS);
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

        final SplitTime hours = new SplitTime(2, TimeUnit.HOURS);
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

        final SplitTime minutes = new SplitTime(2, TimeUnit.MINUTES);
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

        final SplitTime seconds = new SplitTime(2, TimeUnit.SECONDS);
        Assertions.assertEquals(2L, seconds.getSeconds());
        Assertions.assertEquals(0L, seconds.getAttoSeconds());
        Assertions.assertTrue(seconds.isFinite());

    }

    @Test
    public void testMilliSecondsTimeUnit() {

        final SplitTime milliSeconds = new SplitTime(2, TimeUnit.MILLISECONDS);
        Assertions.assertEquals(0L,                milliSeconds.getSeconds());
        Assertions.assertEquals(2000000000000000L, milliSeconds.getAttoSeconds());
        Assertions.assertTrue(milliSeconds.isFinite());

    }

    @Test
    public void testMicroSecondsTimeUnit() {

        final SplitTime microSeconds = new SplitTime(2, TimeUnit.MICROSECONDS);
        Assertions.assertEquals(0L,             microSeconds.getSeconds());
        Assertions.assertEquals(2000000000000L, microSeconds.getAttoSeconds());
        Assertions.assertTrue(microSeconds.isFinite());

    }

    @Test
    public void testNanoTimeUnit() {

        final SplitTime nanoSeconds = new SplitTime(2, TimeUnit.NANOSECONDS);
        Assertions.assertEquals(0L,          nanoSeconds.getSeconds());
        Assertions.assertEquals(2000000000L, nanoSeconds.getAttoSeconds());
        Assertions.assertTrue(nanoSeconds.isFinite());

    }

    private void doTestOutOfRange(final long time, final TimeUnit unit) {

        final SplitTime plus = new SplitTime(time, unit);
        Assertions.assertEquals( time, plus.getRoundedTime(unit));
        Assertions.assertTrue(plus.isFinite());
        Assertions.assertFalse(plus.isNaN());
        Assertions.assertFalse(plus.isInfinite());

        final SplitTime minus = new SplitTime(-time, unit);
        Assertions.assertEquals(-time, minus.getRoundedTime(unit));
        Assertions.assertTrue(minus.isFinite());
        Assertions.assertFalse(minus.isNaN());
        Assertions.assertFalse(minus.isInfinite());

        final SplitTime afterPlus = new SplitTime(time + 1L, unit);
        Assertions.assertEquals(Long.MAX_VALUE, afterPlus.getRoundedTime(unit));
        Assertions.assertFalse(afterPlus.isFinite());
        Assertions.assertFalse(afterPlus.isNaN());
        Assertions.assertTrue(afterPlus.isInfinite());

        final SplitTime beforeMinus = new SplitTime(-time - 1L, unit);
        Assertions.assertEquals(Long.MIN_VALUE, beforeMinus.getRoundedTime(unit));
        Assertions.assertFalse(beforeMinus.isFinite());
        Assertions.assertFalse(beforeMinus.isNaN());
        Assertions.assertTrue(beforeMinus.isInfinite());

    }

    @Test
    public void testSymmetry() {

         for (long time : new long[] {
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
                final SplitTime splitTime = new SplitTime(time, tu);
                final long rebuilt = splitTime.getRoundedTime(tu);
                if (splitTime.isInfinite()) {
                    Assertions.assertEquals(time < 0 ? Long.MIN_VALUE : Long.MAX_VALUE, rebuilt);
                } else {
                    Assertions.assertEquals(time, rebuilt);
                }
            }
        }
    }

    @Test
    public void testAdd() {
        final SplitTime a1 = SplitTime.add(new SplitTime(1234L, 5678L), new SplitTime(76L, -876L));
        Assertions.assertEquals(1310L, a1.getSeconds());
        Assertions.assertEquals(4802L, a1.getAttoSeconds());
        Assertions.assertTrue(a1.isFinite());

        final SplitTime a2 = SplitTime.add(new SplitTime(1L, 500000000000000000L),
                                             new SplitTime(2L, 500000000000000001L));
        Assertions.assertEquals(4L, a2.getSeconds());
        Assertions.assertEquals(1L, a2.getAttoSeconds());
        Assertions.assertTrue(a2.isFinite());

        final SplitTime a3 = SplitTime.add(new SplitTime(1234L, 23L), new SplitTime(76L, -876L));
        Assertions.assertEquals(1309L,               a3.getSeconds());
        Assertions.assertEquals(999999999999999147L, a3.getAttoSeconds());
        Assertions.assertTrue(a3.isFinite());
    }

    @Test
    public void testAddSpecialValues() {

        Assertions.assertTrue(SplitTime.add(SplitTime.NaN, SplitTime.HOUR).isNaN());
        Assertions.assertTrue(SplitTime.add(SplitTime.HOUR, SplitTime.NaN).isNaN());
        Assertions.assertTrue(SplitTime.add(SplitTime.POSITIVE_INFINITY, SplitTime.HOUR).isPositiveInfinity());
        Assertions.assertTrue(SplitTime.add(SplitTime.HOUR, SplitTime.POSITIVE_INFINITY).isPositiveInfinity());
        Assertions.assertTrue(SplitTime.add(SplitTime.NEGATIVE_INFINITY, SplitTime.HOUR).isNegativeInfinity());
        Assertions.assertTrue(SplitTime.add(SplitTime.HOUR, SplitTime.NEGATIVE_INFINITY).isNegativeInfinity());

        Assertions.assertTrue(SplitTime.add(SplitTime.NaN, SplitTime.NaN).isNaN());
        Assertions.assertTrue(SplitTime.add(SplitTime.NaN, SplitTime.NaN).isNaN());
        Assertions.assertTrue(SplitTime.add(SplitTime.POSITIVE_INFINITY, SplitTime.NaN).isNaN());
        Assertions.assertTrue(SplitTime.add(SplitTime.NaN, SplitTime.POSITIVE_INFINITY).isNaN());
        Assertions.assertTrue(SplitTime.add(SplitTime.NEGATIVE_INFINITY, SplitTime.NaN).isNaN());
        Assertions.assertTrue(SplitTime.add(SplitTime.NaN, SplitTime.NEGATIVE_INFINITY).isNaN());

        Assertions.assertTrue(SplitTime.add(SplitTime.NaN, SplitTime.POSITIVE_INFINITY).isNaN());
        Assertions.assertTrue(SplitTime.add(SplitTime.POSITIVE_INFINITY, SplitTime.NaN).isNaN());
        Assertions.assertTrue(SplitTime.add(SplitTime.POSITIVE_INFINITY, SplitTime.POSITIVE_INFINITY).isPositiveInfinity());
        Assertions.assertTrue(SplitTime.add(SplitTime.NEGATIVE_INFINITY, SplitTime.POSITIVE_INFINITY).isNaN());
        Assertions.assertTrue(SplitTime.add(SplitTime.POSITIVE_INFINITY, SplitTime.NEGATIVE_INFINITY).isNaN());

        Assertions.assertTrue(SplitTime.add(SplitTime.NaN, SplitTime.NEGATIVE_INFINITY).isNaN());
        Assertions.assertTrue(SplitTime.add(SplitTime.NEGATIVE_INFINITY, SplitTime.NaN).isNaN());
        Assertions.assertTrue(SplitTime.add(SplitTime.POSITIVE_INFINITY, SplitTime.NEGATIVE_INFINITY).isNaN());
        Assertions.assertTrue(SplitTime.add(SplitTime.NEGATIVE_INFINITY, SplitTime.POSITIVE_INFINITY).isNaN());
        Assertions.assertTrue(SplitTime.add(SplitTime.NEGATIVE_INFINITY, SplitTime.NEGATIVE_INFINITY).isNegativeInfinity());

    }

    @Test
    public void testSubtract() {
        final SplitTime s1 = SplitTime.subtract(new SplitTime(1234L, 5678L), new SplitTime(76L, 876L));
        Assertions.assertEquals(1158L, s1.getSeconds());
        Assertions.assertEquals(4802L, s1.getAttoSeconds());
        Assertions.assertTrue(s1.isFinite());

        final SplitTime s2 = SplitTime.subtract(new SplitTime(1L, 0L), new SplitTime(2L, 1L));
        Assertions.assertEquals(-2L, s2.getSeconds());
        Assertions.assertEquals(999999999999999999L, s2.getAttoSeconds());
        Assertions.assertTrue(s2.isFinite());

        final SplitTime s3 = SplitTime.subtract(new SplitTime(1234L, 999999999999999999L), new SplitTime(76L, 123456L));
        Assertions.assertEquals(1158L,               s3.getSeconds());
        Assertions.assertEquals(999999999999876543L, s3.getAttoSeconds());
        Assertions.assertTrue(s3.isFinite());
    }

    @Test
    public void testSubtractSpecialValues() {

        Assertions.assertTrue(SplitTime.subtract(SplitTime.NaN, SplitTime.HOUR).isNaN());
        Assertions.assertTrue(SplitTime.subtract(SplitTime.HOUR, SplitTime.NaN).isNaN());
        Assertions.assertTrue(SplitTime.subtract(SplitTime.POSITIVE_INFINITY, SplitTime.HOUR).isPositiveInfinity());
        Assertions.assertTrue(SplitTime.subtract(SplitTime.HOUR, SplitTime.POSITIVE_INFINITY).isNegativeInfinity());
        Assertions.assertTrue(SplitTime.subtract(SplitTime.NEGATIVE_INFINITY, SplitTime.HOUR).isNegativeInfinity());
        Assertions.assertTrue(SplitTime.subtract(SplitTime.HOUR, SplitTime.NEGATIVE_INFINITY).isPositiveInfinity());

        Assertions.assertTrue(SplitTime.subtract(SplitTime.NaN, SplitTime.NaN).isNaN());
        Assertions.assertTrue(SplitTime.subtract(SplitTime.NaN, SplitTime.NaN).isNaN());
        Assertions.assertTrue(SplitTime.subtract(SplitTime.POSITIVE_INFINITY, SplitTime.NaN).isNaN());
        Assertions.assertTrue(SplitTime.subtract(SplitTime.NaN, SplitTime.POSITIVE_INFINITY).isNaN());
        Assertions.assertTrue(SplitTime.subtract(SplitTime.NEGATIVE_INFINITY, SplitTime.NaN).isNaN());
        Assertions.assertTrue(SplitTime.subtract(SplitTime.NaN, SplitTime.NEGATIVE_INFINITY).isNaN());

        Assertions.assertTrue(SplitTime.subtract(SplitTime.NaN, SplitTime.POSITIVE_INFINITY).isNaN());
        Assertions.assertTrue(SplitTime.subtract(SplitTime.POSITIVE_INFINITY, SplitTime.NaN).isNaN());
        Assertions.assertTrue(SplitTime.subtract(SplitTime.POSITIVE_INFINITY, SplitTime.POSITIVE_INFINITY).isNaN());
        Assertions.assertTrue(SplitTime.subtract(SplitTime.NEGATIVE_INFINITY, SplitTime.POSITIVE_INFINITY).isNegativeInfinity());
        Assertions.assertTrue(SplitTime.subtract(SplitTime.POSITIVE_INFINITY, SplitTime.NEGATIVE_INFINITY).isPositiveInfinity());

        Assertions.assertTrue(SplitTime.subtract(SplitTime.NaN, SplitTime.NEGATIVE_INFINITY).isNaN());
        Assertions.assertTrue(SplitTime.subtract(SplitTime.NEGATIVE_INFINITY, SplitTime.NaN).isNaN());
        Assertions.assertTrue(SplitTime.subtract(SplitTime.POSITIVE_INFINITY, SplitTime.NEGATIVE_INFINITY).isPositiveInfinity());
        Assertions.assertTrue(SplitTime.subtract(SplitTime.NEGATIVE_INFINITY, SplitTime.POSITIVE_INFINITY).isNegativeInfinity());
        Assertions.assertTrue(SplitTime.subtract(SplitTime.NEGATIVE_INFINITY, SplitTime.NEGATIVE_INFINITY).isNaN());

    }

    @Test
    public void testCompareFinite() {
        Assertions.assertTrue(SplitTime.ZERO.compareTo(new SplitTime(0L, 0L)) == 0);
        Assertions.assertTrue(new SplitTime(1L, 1L).compareTo(new SplitTime(1L, 1L)) == 0);
        Assertions.assertTrue(SplitTime.MICROSECOND.compareTo(SplitTime.MILLISECOND) < 0);
        Assertions.assertTrue(SplitTime.MILLISECOND.compareTo(SplitTime.MICROSECOND) > 0);
    }

    @Test
    public void testCompareSpecialCases() {

        Assertions.assertTrue(SplitTime.NaN.compareTo(SplitTime.NaN)               == 0);
        Assertions.assertTrue(SplitTime.NaN.compareTo(SplitTime.ATTOSECOND)        >  0);
        Assertions.assertTrue(SplitTime.NaN.compareTo(SplitTime.POSITIVE_INFINITY) >  0);
        Assertions.assertTrue(SplitTime.NaN.compareTo(SplitTime.NEGATIVE_INFINITY) >  0);

        Assertions.assertTrue(SplitTime.ATTOSECOND.compareTo(SplitTime.NaN)               <  0);
        Assertions.assertTrue(SplitTime.ATTOSECOND.compareTo(SplitTime.ATTOSECOND)        == 0);
        Assertions.assertTrue(SplitTime.ATTOSECOND.compareTo(SplitTime.POSITIVE_INFINITY) <  0);
        Assertions.assertTrue(SplitTime.ATTOSECOND.compareTo(SplitTime.NEGATIVE_INFINITY) >  0);

        Assertions.assertTrue(SplitTime.POSITIVE_INFINITY.compareTo(SplitTime.NaN)               <  0);
        Assertions.assertTrue(SplitTime.POSITIVE_INFINITY.compareTo(SplitTime.ATTOSECOND)        >  0);
        Assertions.assertTrue(SplitTime.POSITIVE_INFINITY.compareTo(SplitTime.POSITIVE_INFINITY) == 0);
        Assertions.assertTrue(SplitTime.POSITIVE_INFINITY.compareTo(SplitTime.NEGATIVE_INFINITY) >  0);

        Assertions.assertTrue(SplitTime.NEGATIVE_INFINITY.compareTo(SplitTime.NaN)               <  0);
        Assertions.assertTrue(SplitTime.NEGATIVE_INFINITY.compareTo(SplitTime.ATTOSECOND)        <  0);
        Assertions.assertTrue(SplitTime.NEGATIVE_INFINITY.compareTo(SplitTime.POSITIVE_INFINITY) <  0);
        Assertions.assertTrue(SplitTime.NEGATIVE_INFINITY.compareTo(SplitTime.NEGATIVE_INFINITY) == 0);

    }

    @Test
    public void testMultiples() {
        for (int n = 0; n < 100; ++n) {
            checkMultiple(n, SplitTime.ZERO, SplitTime.ZERO);
        }
        checkMultiple( 1000, SplitTime.ATTOSECOND,  SplitTime.FEMTOSECOND);
        checkMultiple( 1000, SplitTime.FEMTOSECOND, SplitTime.PICOSECOND);
        checkMultiple( 1000, SplitTime.PICOSECOND,  SplitTime.NANOSECOND);
        checkMultiple( 1000, SplitTime.NANOSECOND,  SplitTime.MICROSECOND);
        checkMultiple( 1000, SplitTime.MICROSECOND, SplitTime.MILLISECOND);
        checkMultiple( 1000, SplitTime.MILLISECOND, SplitTime.SECOND);
        checkMultiple(   60, SplitTime.SECOND,      SplitTime.MINUTE);
        checkMultiple(   60, SplitTime.MINUTE,      SplitTime.HOUR);
        checkMultiple(   24, SplitTime.HOUR,        SplitTime.DAY);
        checkMultiple(86401, SplitTime.SECOND,      SplitTime.DAY_WITH_POSITIVE_LEAP);
    }

    private void checkMultiple(final int n, final SplitTime small, final SplitTime large) {
        SplitTime sum = SplitTime.ZERO;
        for (int i = 0; i < n; ++i) {
            sum = SplitTime.add(sum, small);
        }
        Assertions.assertTrue(SplitTime.subtract(sum, large).isZero());
    }

}
