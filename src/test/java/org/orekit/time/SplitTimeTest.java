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

import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.random.RandomGenerator;
import org.hipparchus.random.Well1024a;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

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
    public void testMultiplicativeConstructor() {
        final SplitTime s101 = new SplitTime(1L, 10000000000000000L);
        Assertions.assertEquals(1.01, s101.toDouble(), 1.0e-15);
        final SplitTime p303 = new SplitTime(3, s101);
        Assertions.assertEquals(3L, p303.getSeconds());
        Assertions.assertEquals(30000000000000000L, p303.getAttoSeconds());
        final SplitTime m303 = new SplitTime(-3, s101);
        Assertions.assertEquals(-4L, m303.getSeconds());
        Assertions.assertEquals(970000000000000000L, m303.getAttoSeconds());
    }

    @Test
    public void testLinear2Constructor() {
        final SplitTime p305 = new SplitTime(3, SplitTime.SECOND, 50, SplitTime.MILLISECOND);
        Assertions.assertEquals(3L, p305.getSeconds());
        Assertions.assertEquals(50000000000000000L, p305.getAttoSeconds());
        final SplitTime m305 = new SplitTime(-3, SplitTime.SECOND, -50, SplitTime.MILLISECOND);
        Assertions.assertEquals(-4L, m305.getSeconds());
        Assertions.assertEquals(950000000000000000L, m305.getAttoSeconds());
    }

    @Test
    public void testLinear3Constructor() {
        final SplitTime p3005007 = new SplitTime(3, SplitTime.SECOND,
                                                 5, SplitTime.MILLISECOND,
                                                 7, SplitTime.MICROSECOND);
        Assertions.assertEquals(3L, p3005007.getSeconds());
        Assertions.assertEquals(5007000000000000L, p3005007.getAttoSeconds());
        final SplitTime m3005007 = new SplitTime(-3, SplitTime.SECOND,
                                                 -5, SplitTime.MILLISECOND,
                                                 -7, SplitTime.MICROSECOND);
        Assertions.assertEquals(-4L, m3005007.getSeconds());
        Assertions.assertEquals(994993000000000000L, m3005007.getAttoSeconds());
    }

    @Test
    public void testZero() {
        Assertions.assertTrue(new SplitTime(0, 0).isZero());
        Assertions.assertFalse(new SplitTime(0, 1).isZero());
        Assertions.assertFalse(new SplitTime(1, 0).isZero());
        Assertions.assertFalse(new SplitTime(1, 1).isZero());
    }

    @Test
    public void testRoundSeconds() {
        Assertions.assertEquals(1L,
                                new SplitTime(1L, 499999999999999999L).getRoundedTime(TimeUnit.SECONDS));
        Assertions.assertEquals(2L,
                                new SplitTime(1L, 500000000000000000L).getRoundedTime(TimeUnit.SECONDS));
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
        final SplitTime so1 = new SplitTime(-1.0e-17);
        Assertions.assertEquals(-1L, so1.getSeconds());
        Assertions.assertEquals(999999999999999990L, so1.getAttoSeconds());
        Assertions.assertTrue(so1.isFinite());
        final SplitTime so2 = new SplitTime(FastMath.nextDown(0.0));
        Assertions.assertEquals(-1L, so2.getSeconds());
        Assertions.assertEquals(1000000000000000000L, so2.getAttoSeconds());
        Assertions.assertTrue(so2.isFinite());
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
        final SplitTime a1 = new SplitTime(1234L, 5678L).add(new SplitTime(76L, -876L));
        Assertions.assertEquals(1310L, a1.getSeconds());
        Assertions.assertEquals(4802L, a1.getAttoSeconds());
        Assertions.assertTrue(a1.isFinite());

        final SplitTime a2 = new SplitTime(1L, 500000000000000000L).add(new SplitTime(2L, 500000000000000001L));
        Assertions.assertEquals(4L, a2.getSeconds());
        Assertions.assertEquals(1L, a2.getAttoSeconds());
        Assertions.assertTrue(a2.isFinite());

        final SplitTime a3 = new SplitTime(1234L, 23L).add(new SplitTime(76L, -876L));
        Assertions.assertEquals(1309L,               a3.getSeconds());
        Assertions.assertEquals(999999999999999147L, a3.getAttoSeconds());
        Assertions.assertTrue(a3.isFinite());
    }

    @Test
    public void testAddSpecialValues() {

        Assertions.assertTrue(SplitTime.NaN.add(SplitTime.HOUR).isNaN());
        Assertions.assertTrue(SplitTime.HOUR.add(SplitTime.NaN).isNaN());
        Assertions.assertTrue(SplitTime.POSITIVE_INFINITY.add(SplitTime.HOUR).isPositiveInfinity());
        Assertions.assertTrue(SplitTime.HOUR.add(SplitTime.POSITIVE_INFINITY).isPositiveInfinity());
        Assertions.assertTrue(SplitTime.NEGATIVE_INFINITY.add(SplitTime.HOUR).isNegativeInfinity());
        Assertions.assertTrue(SplitTime.HOUR.add(SplitTime.NEGATIVE_INFINITY).isNegativeInfinity());

        Assertions.assertTrue(SplitTime.NaN.add(SplitTime.NaN).isNaN());
        Assertions.assertTrue(SplitTime.NaN.add(SplitTime.NaN).isNaN());
        Assertions.assertTrue(SplitTime.POSITIVE_INFINITY.add(SplitTime.NaN).isNaN());
        Assertions.assertTrue(SplitTime.NaN.add(SplitTime.POSITIVE_INFINITY).isNaN());
        Assertions.assertTrue(SplitTime.NEGATIVE_INFINITY.add(SplitTime.NaN).isNaN());
        Assertions.assertTrue(SplitTime.NaN.add(SplitTime.NEGATIVE_INFINITY).isNaN());

        Assertions.assertTrue(SplitTime.NaN.add(SplitTime.POSITIVE_INFINITY).isNaN());
        Assertions.assertTrue(SplitTime.POSITIVE_INFINITY.add(SplitTime.NaN).isNaN());
        Assertions.assertTrue(SplitTime.POSITIVE_INFINITY.add(SplitTime.POSITIVE_INFINITY).isPositiveInfinity());
        Assertions.assertTrue(SplitTime.NEGATIVE_INFINITY.add(SplitTime.POSITIVE_INFINITY).isNaN());
        Assertions.assertTrue(SplitTime.POSITIVE_INFINITY.add(SplitTime.NEGATIVE_INFINITY).isNaN());

        Assertions.assertTrue(SplitTime.NaN.add(SplitTime.NEGATIVE_INFINITY).isNaN());
        Assertions.assertTrue(SplitTime.NEGATIVE_INFINITY.add(SplitTime.NaN).isNaN());
        Assertions.assertTrue(SplitTime.POSITIVE_INFINITY.add(SplitTime.NEGATIVE_INFINITY).isNaN());
        Assertions.assertTrue(SplitTime.NEGATIVE_INFINITY.add(SplitTime.POSITIVE_INFINITY).isNaN());
        Assertions.assertTrue(SplitTime.NEGATIVE_INFINITY.add(SplitTime.NEGATIVE_INFINITY).isNegativeInfinity());

    }

    @Test
    public void testSubtract() {
        final SplitTime s1 = new SplitTime(1234L, 5678L).subtract(new SplitTime(76L, 876L));
        Assertions.assertEquals(1158L, s1.getSeconds());
        Assertions.assertEquals(4802L, s1.getAttoSeconds());
        Assertions.assertTrue(s1.isFinite());

        final SplitTime s2 = new SplitTime(1L, 0L).subtract(new SplitTime(2L, 1L));
        Assertions.assertEquals(-2L, s2.getSeconds());
        Assertions.assertEquals(999999999999999999L, s2.getAttoSeconds());
        Assertions.assertTrue(s2.isFinite());

        final SplitTime s3 = new SplitTime(1234L, 999999999999999999L).subtract(new SplitTime(76L, 123456L));
        Assertions.assertEquals(1158L,               s3.getSeconds());
        Assertions.assertEquals(999999999999876543L, s3.getAttoSeconds());
        Assertions.assertTrue(s3.isFinite());
    }

    @Test
    public void testSubtractSpecialValues() {

        Assertions.assertTrue(SplitTime.NaN.subtract(SplitTime.HOUR).isNaN());
        Assertions.assertTrue(SplitTime.HOUR.subtract(SplitTime.NaN).isNaN());
        Assertions.assertTrue(SplitTime.POSITIVE_INFINITY.subtract(SplitTime.HOUR).isPositiveInfinity());
        Assertions.assertTrue(SplitTime.HOUR.subtract(SplitTime.POSITIVE_INFINITY).isNegativeInfinity());
        Assertions.assertTrue(SplitTime.NEGATIVE_INFINITY.subtract(SplitTime.HOUR).isNegativeInfinity());
        Assertions.assertTrue(SplitTime.HOUR.subtract(SplitTime.NEGATIVE_INFINITY).isPositiveInfinity());

        Assertions.assertTrue(SplitTime.NaN.subtract(SplitTime.NaN).isNaN());
        Assertions.assertTrue(SplitTime.NaN.subtract(SplitTime.NaN).isNaN());
        Assertions.assertTrue(SplitTime.POSITIVE_INFINITY.subtract(SplitTime.NaN).isNaN());
        Assertions.assertTrue(SplitTime.NaN.subtract(SplitTime.POSITIVE_INFINITY).isNaN());
        Assertions.assertTrue(SplitTime.NEGATIVE_INFINITY.subtract(SplitTime.NaN).isNaN());
        Assertions.assertTrue(SplitTime.NaN.subtract(SplitTime.NEGATIVE_INFINITY).isNaN());

        Assertions.assertTrue(SplitTime.NaN.subtract(SplitTime.POSITIVE_INFINITY).isNaN());
        Assertions.assertTrue(SplitTime.POSITIVE_INFINITY.subtract(SplitTime.NaN).isNaN());
        Assertions.assertTrue(SplitTime.POSITIVE_INFINITY.subtract(SplitTime.POSITIVE_INFINITY).isNaN());
        Assertions.assertTrue(SplitTime.NEGATIVE_INFINITY.subtract(SplitTime.POSITIVE_INFINITY).isNegativeInfinity());
        Assertions.assertTrue(SplitTime.POSITIVE_INFINITY.subtract(SplitTime.NEGATIVE_INFINITY).isPositiveInfinity());

        Assertions.assertTrue(SplitTime.NaN.subtract(SplitTime.NEGATIVE_INFINITY).isNaN());
        Assertions.assertTrue(SplitTime.NEGATIVE_INFINITY.subtract(SplitTime.NaN).isNaN());
        Assertions.assertTrue(SplitTime.POSITIVE_INFINITY.subtract(SplitTime.NEGATIVE_INFINITY).isPositiveInfinity());
        Assertions.assertTrue(SplitTime.NEGATIVE_INFINITY.subtract(SplitTime.POSITIVE_INFINITY).isNegativeInfinity());
        Assertions.assertTrue(SplitTime.NEGATIVE_INFINITY.subtract(SplitTime.NEGATIVE_INFINITY).isNaN());

    }

    @Test
    public void testMultiply() {
        try {
            new SplitTime(   1L, 45L).multiply(-1);
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.NOT_POSITIVE, oe.getSpecifier());
            Assertions.assertEquals(-1, (Long) oe.getParts()[0]);
        }
        checkComponents(new SplitTime(   1L, 45L).multiply(0),  0L,                  0L);
        checkComponents(new SplitTime(1L, 45L).multiply(1), 1L, 45L);
        checkComponents(new SplitTime(1L, 45L).multiply(3), 3L, 135L);
        checkComponents(new SplitTime(1234L, 123456789012345678L).multiply(7233L), 8926414L, 962954926296288974L);
        checkComponents(new SplitTime(1234L, 999999999999999999L).multiply(23012696L), 28420679559L, 999999999976987304L);
        checkComponents(new SplitTime(1234L, 999999999999999999L).multiply(123456789012L),
                        152469134429819L, 999999876543210988L);
         try {
             new SplitTime(10000000000L, 1L).multiply(123456789012L);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(LocalizedCoreFormats.OVERFLOW_IN_MULTIPLICATION, oe.getSpecifier());
            Assertions.assertEquals(10000000000L, (Long) oe.getParts()[0]);
            Assertions.assertEquals(123456789012L, (Long) oe.getParts()[1]);
        }

        try {
            new SplitTime(922382683L, 717054400620018329L).multiply(1573105907129L);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(LocalizedCoreFormats.OVERFLOW_IN_MULTIPLICATION, oe.getSpecifier());
            Assertions.assertEquals(922382683L, (Long) oe.getParts()[0]);
            Assertions.assertEquals(1573105907129L, (Long) oe.getParts()[1]);
        }
    }

    @Test
    public void testMultiplySpecialValues() {
        Assertions.assertTrue(SplitTime.NEGATIVE_INFINITY.multiply(0).isNaN());
        Assertions.assertTrue(SplitTime.NEGATIVE_INFINITY.multiply(3).isNegativeInfinity());
        Assertions.assertTrue(SplitTime.POSITIVE_INFINITY.multiply(0).isNaN());
        Assertions.assertTrue(SplitTime.POSITIVE_INFINITY.multiply(3).isPositiveInfinity());
        Assertions.assertTrue(SplitTime.NaN.multiply(              0).isNaN());
        Assertions.assertTrue(SplitTime.NaN.multiply(              3).isNaN());
    }

    @Test
    public void testDivide() {
        try {
            new SplitTime(1L, 45L).divide(0);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.NOT_STRICTLY_POSITIVE, oe.getSpecifier());
            Assertions.assertEquals(0, (Integer) oe.getParts()[0]);
        }
        checkComponents(new SplitTime(1L,  45L).divide(1), 1L, 45L);
        checkComponents(new SplitTime(3L, 135L).divide(3), 1L, 45L);
        checkComponents(new SplitTime(8926414L, 962954926296288974L).divide(7233), 1234L, 123456789012345678L);
        checkComponents(new SplitTime(28420679559L, 999999999976987304L).divide(23012696), 1234L, 999999999999999999L);
        checkComponents(new SplitTime(1L, 0L).divide(1000000000), 0L, 1000000000L);

        // we consider a 15 nanosecond per UTC second slope for TAI-UTC offset (this is what was used in 1961)
        // then 1 day in UTC corresponds to 1 day + 1296 µs in TAI, and we perform the computation the other way round
        // we start from the 1 day + 1296 µs duration in TAI and recover the 1296 µs change in TAI-UTC offset
        checkComponents(new SplitTime(86400L, 1296000000000000L).multiply(15).divide(1000000015),
                        0L, 1296000000000000L);

    }

    @Test
    public void testRandomDivide() {
        final RandomGenerator random = new Well1024a(0x83977774b8d4eb2eL);
        for (int i = 0; i < 1000000; i++) {
            final SplitTime t = new SplitTime(random.nextLong(1000L * 365L * 86400L),
                                              random.nextLong(1000000000000000000L));
            final int p = FastMath.max(1, random.nextInt(10000000));
            Assertions.assertEquals(0, t.compareTo(t.multiply(p).divide(p)));
        }

    }

    @Test
    public void testDivideSpecialValues() {
        Assertions.assertTrue(SplitTime.NEGATIVE_INFINITY.divide(3).isNegativeInfinity());
        Assertions.assertTrue(SplitTime.POSITIVE_INFINITY.divide(3).isPositiveInfinity());
        Assertions.assertTrue(SplitTime.NaN.divide(              3).isNaN());
    }

    @Test
    public void testNegate() {
        for (long s = -1000L; s < 1000L; s += 10L) {
            for (long a = -1000L; a < 1000L; a += 10L) {
                final SplitTime st = new SplitTime(s, a);
                Assertions.assertTrue(st.add(st.negate()).isZero());
            }
        }
        Assertions.assertTrue(SplitTime.POSITIVE_INFINITY.negate().isNegativeInfinity());
        Assertions.assertTrue(SplitTime.NEGATIVE_INFINITY.negate().isPositiveInfinity());
        Assertions.assertTrue(SplitTime.NaN.negate().isNaN());
        Assertions.assertTrue(SplitTime.ZERO.negate().isZero());
    }

    @Test
    public void testCompareFinite() {
        Assertions.assertTrue(SplitTime.ZERO.compareTo(new SplitTime(0L, 0L)) == 0);
        Assertions.assertTrue(new SplitTime(1L, 1L).compareTo(new SplitTime(1L, 1L)) == 0);
        Assertions.assertTrue(SplitTime.MICROSECOND.compareTo(SplitTime.MILLISECOND) < 0);
        Assertions.assertTrue(SplitTime.MILLISECOND.compareTo(SplitTime.MICROSECOND) > 0);
        Assertions.assertTrue(new SplitTime(1, 2).compareTo(new SplitTime(1, 2)) == 0);
        Assertions.assertTrue(new SplitTime(1, 1).compareTo(new SplitTime(1, 2))  < 0);
        Assertions.assertTrue(new SplitTime(1, 2).compareTo(new SplitTime(1, 1))  > 0);
        Assertions.assertTrue(new SplitTime(2, 1).compareTo(new SplitTime(2, 1)) == 0);
        Assertions.assertTrue(new SplitTime(1, 1).compareTo(new SplitTime(2, 1))  < 0);
        Assertions.assertTrue(new SplitTime(2, 1).compareTo(new SplitTime(1, 1))  > 0);
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
    public void testEquals() {
        SplitTime[] st = new SplitTime[] {
          new SplitTime(200L, 300L), new SplitTime(200L, 199L), new SplitTime(199L, 300L),
          SplitTime.POSITIVE_INFINITY, SplitTime.NEGATIVE_INFINITY,
          SplitTime.NaN, SplitTime.DAY, SplitTime.ZERO, SplitTime.ATTOSECOND
        };
        for (int i = 0; i < st.length; i++) {
            for (int j = 0; j < st.length; j++) {
                if (i == j) {
                    Assertions.assertEquals(st[i], st[j]);
                } else {

                    Assertions.assertNotEquals(st[i], st[j]);
                }
            }
        }
        final SplitTime splitTime = new SplitTime(200L, 300L);
        Assertions.assertEquals(st[0],  splitTime);
        Assertions.assertNotSame(st[0], splitTime);
        Assertions.assertEquals(0,      st[0].compareTo(splitTime));
        Assertions.assertEquals(484,         st[0].hashCode());
        Assertions.assertEquals(484,         splitTime.hashCode());
        Assertions.assertEquals(254,         SplitTime.NaN.hashCode());
        Assertions.assertEquals(-2130771969, SplitTime.NEGATIVE_INFINITY.hashCode());
        Assertions.assertEquals(-2147418369, SplitTime.POSITIVE_INFINITY.hashCode());
        Assertions.assertEquals(0,           SplitTime.ZERO.hashCode());
        Assertions.assertEquals(1,           SplitTime.ATTOSECOND.hashCode());
        Assertions.assertNotEquals(splitTime, "splitTime");
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

    @Test
    public void testParseEmpty() {
        doTestParseError("");
    }

    @Test
    public void testParseExtraCharacters() {
        doTestParseError("-1.2e3!xyz");
    }

    @Test
    public void testParseMiddleSign() {
        doTestParseError("-1.2-3");
    }

    @Test
    public void testParseMinusAfterExponent() {
        doTestParseError("-1.2e+3-1");
    }

    @Test
    public void testParsePlusAfterExponent() {
        doTestParseError("-1.2e+3+1");
    }

    @Test
    public void testParseOverflowA() {
        doTestParseError("10000000000000000000");
    }

    @Test
    public void testParseOverflowB() {
        doTestParseError("99999999999999999999");
    }

    @Test
    public void testParseOverflowC() {
        doTestParseError("9223372036854775808");
    }

    @Test
    public void testParseOverflowD() {
        doTestParseError("1.0e10000000000");
    }

    @Test
    public void testParseOverflowE() {
        doTestParseError("1.0e99999999999");
    }

    @Test
    public void testParseOverflowF() {
        doTestParseError("1.0e2147483648");
    }

    @Test
    public void testParseRepeatedSeparator() {
        doTestParseError("1.0.0");
    }

    @Test
    public void testParseRepeatedExponent() {
        doTestParseError("1.0e2e3");
    }

    private void doTestParseError(final String s) {
        try {
            SplitTime.parse(s);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.CANNOT_PARSE_DATA, oe.getSpecifier());
            Assertions.assertEquals(s, oe.getParts()[0]);
        }
    }

    @Test
    public void testParseSpecialValues() {
        Assertions.assertTrue(SplitTime.parse("NaN").isNaN());
        Assertions.assertTrue(SplitTime.parse("NAN").isNaN());
        Assertions.assertTrue(SplitTime.parse("nan").isNaN());
        Assertions.assertTrue(SplitTime.parse("-∞").isNegativeInfinity());
        Assertions.assertTrue(SplitTime.parse("+∞").isPositiveInfinity());
    }

    @Test
    public void testParseLargeExponents(){
        Assertions.assertTrue(SplitTime.parse("-1.0e100").isNegativeInfinity());
        Assertions.assertTrue(SplitTime.parse("+1.0e100").isPositiveInfinity());
        Assertions.assertTrue(SplitTime.parse("-0.1e100").isNegativeInfinity());
        Assertions.assertTrue(SplitTime.parse("+0.1e100").isPositiveInfinity());
        Assertions.assertTrue(SplitTime.parse("-1.1e100").isNegativeInfinity());
        Assertions.assertTrue(SplitTime.parse("+1.1e100").isPositiveInfinity());
        checkComponents(SplitTime.parse("0.0e200000"), 0L, 0L);
    }

    @Test
    public void testParse() {
        checkComponents(SplitTime.parse("0"), 0L, 0L);
        checkComponents(SplitTime.parse("1"), 1L, 0L);
        checkComponents(SplitTime.parse("-1"), -1L, 0L);
        checkComponents(SplitTime.parse("+0.5"),  0L, 500000000000000000L);
        checkComponents(SplitTime.parse("-0.5"), -1L, 500000000000000000L);
        checkComponents(SplitTime.parse("+.5"),   0L, 500000000000000000L);
        checkComponents(SplitTime.parse("-.5"),  -1L, 500000000000000000L);
        checkComponents(SplitTime.parse("17.42357e+02"), 1742L, 357000000000000000L);
        checkComponents(SplitTime.parse("9223372036854775807"), Long.MAX_VALUE, 0L);

        // these are the offsets for linear UTC/TAI models before 1972
        checkComponents(SplitTime.parse("1.4228180"), 1L, 422818000000000000L);
        checkComponents(SplitTime.parse("1.3728180"), 1L, 372818000000000000L);
        checkComponents(SplitTime.parse("1.8458580"), 1L, 845858000000000000L);
        checkComponents(SplitTime.parse("1.9458580"), 1L, 945858000000000000L);
        checkComponents(SplitTime.parse("3.2401300"), 3L, 240130000000000000L);
        checkComponents(SplitTime.parse("3.3401300"), 3L, 340130000000000000L);
        checkComponents(SplitTime.parse("3.4401300"), 3L, 440130000000000000L);
        checkComponents(SplitTime.parse("3.5401300"), 3L, 540130000000000000L);
        checkComponents(SplitTime.parse("3.6401300"), 3L, 640130000000000000L);
        checkComponents(SplitTime.parse("3.7401300"), 3L, 740130000000000000L);
        checkComponents(SplitTime.parse("3.8401300"), 3L, 840130000000000000L);
        checkComponents(SplitTime.parse("4.3131700"), 4L, 313170000000000000L);
        checkComponents(SplitTime.parse("4.2131700"), 4L, 213170000000000000L);

        // these are the drifts for linear UTC/TAI models before 1972
        checkComponents(SplitTime.parse("0.001296"), 0L, 1296000000000000L);
        checkComponents(SplitTime.parse("0.0011232"), 0L, 1123200000000000L);
        checkComponents(SplitTime.parse("0.002592"), 0L, 2592000000000000L);

        // cases with exponents
        checkComponents(SplitTime.parse("0.001234e-05"),     0L, 12340000000L);
        checkComponents(SplitTime.parse("-0.001234E+05"), -124L, 600000000000000000L);
        checkComponents(SplitTime.parse("-0.001234E-05"),   -1L, 999999987660000000L);
        checkComponents(SplitTime.parse("0.001e-15"),        0L, 1L);
        checkComponents(SplitTime.parse("-0.001e-15"),      -1L, 999999999999999999L);
        checkComponents(SplitTime.parse("-12E-1"),          -2L, 800000000000000000L);
        checkComponents(SplitTime.parse("-12E0"),          -12L, 0L);
        checkComponents(SplitTime.parse("-12E-0"),         -12L, 0L);
        checkComponents(SplitTime.parse("-12E+0"),         -12L, 0L);
        checkComponents(SplitTime.parse("1.234e-50"),        0L, 0L);
        checkComponents(SplitTime.parse("1.e2"),           100L, 0L);

        // ignoring extra digits after separator
        checkComponents(SplitTime.parse("0.12345678901234567890123456"), 0L, 123456789012345678L);

        // various resolutions
        checkComponents(SplitTime.parse("12.3456e-20"),                   0L,                  0L);
        checkComponents(SplitTime.parse("12.3456e-19"),                   0L,                  1L);
        checkComponents(SplitTime.parse("12.3456e-18"),                   0L,                 12L);
        checkComponents(SplitTime.parse("12.3456e-17"),                   0L,                123L);
        checkComponents(SplitTime.parse("12.3456e-16"),                   0L,               1234L);
        checkComponents(SplitTime.parse("12.3456e-15"),                   0L,              12345L);
        checkComponents(SplitTime.parse("12.3456e-14"),                   0L,             123456L);
        checkComponents(SplitTime.parse("12.3456e-13"),                   0L,            1234560L);
        checkComponents(SplitTime.parse("12.3456e-12"),                   0L,           12345600L);
        checkComponents(SplitTime.parse("12.3456e-11"),                   0L,          123456000L);
        checkComponents(SplitTime.parse("12.3456e-10"),                   0L,         1234560000L);
        checkComponents(SplitTime.parse("12.3456e-09"),                   0L,        12345600000L);
        checkComponents(SplitTime.parse("12.3456e-08"),                   0L,       123456000000L);
        checkComponents(SplitTime.parse("12.3456e-07"),                   0L,      1234560000000L);
        checkComponents(SplitTime.parse("12.3456e-06"),                   0L,     12345600000000L);
        checkComponents(SplitTime.parse("12.3456e-05"),                   0L,    123456000000000L);
        checkComponents(SplitTime.parse("12.3456e-04"),                   0L,   1234560000000000L);
        checkComponents(SplitTime.parse("12.3456e-03"),                   0L,  12345600000000000L);
        checkComponents(SplitTime.parse("12.3456e-02"),                   0L, 123456000000000000L);
        checkComponents(SplitTime.parse("12.3456e-01"),                   1L, 234560000000000000L);
        checkComponents(SplitTime.parse("12.3456e+00"),                  12L, 345600000000000000L);
        checkComponents(SplitTime.parse("12.3456e+01"),                 123L, 456000000000000000L);
        checkComponents(SplitTime.parse("12.3456e+02"),                1234L, 560000000000000000L);
        checkComponents(SplitTime.parse("12.3456e+03"),               12345L, 600000000000000000L);
        checkComponents(SplitTime.parse("12.3456e+04"),              123456L,                  0L);
        checkComponents(SplitTime.parse("12.3456e+05"),             1234560L,                  0L);
        checkComponents(SplitTime.parse("12.3456e+06"),            12345600L,                  0L);
        checkComponents(SplitTime.parse("12.3456e+07"),           123456000L,                  0L);
        checkComponents(SplitTime.parse("12.3456e+08"),          1234560000L,                  0L);
        checkComponents(SplitTime.parse("12.3456e+09"),         12345600000L,                  0L);
        checkComponents(SplitTime.parse("12.3456e+10"),        123456000000L,                  0L);
        checkComponents(SplitTime.parse("12.3456e+11"),       1234560000000L,                  0L);
        checkComponents(SplitTime.parse("12.3456e+12"),      12345600000000L,                  0L);
        checkComponents(SplitTime.parse("12.3456e+13"),     123456000000000L,                  0L);
        checkComponents(SplitTime.parse("12.3456e+14"),    1234560000000000L,                  0L);
        checkComponents(SplitTime.parse("12.3456e+15"),   12345600000000000L,                  0L);
        checkComponents(SplitTime.parse("12.3456e+16"),  123456000000000000L,                  0L);
        checkComponents(SplitTime.parse("12.3456e+17"), 1234560000000000000L,                  0L);

        // truncating to upper attosecond (NOT rounding!)
        checkComponents(SplitTime.parse("+1234.567890123456785012e-4"),  0L, 123456789012345678L);
        checkComponents(SplitTime.parse("-1234.567890123456785012e-4"), -1L, 876543210987654322L);
        checkComponents(SplitTime.parse("+1234.567890123456784012e-4"),  0L, 123456789012345678L);
        checkComponents(SplitTime.parse("-1234.567890123456784012e-4"), -1L, 876543210987654322L);
        checkComponents(SplitTime.parse("+9999.999999999999994000e-4"),  0L, 999999999999999999L);
        checkComponents(SplitTime.parse("+9999.999999999999995000e-4"),  0L, 999999999999999999L);
        checkComponents(SplitTime.parse("-9999.999999999999994000e-4"), -1L,                  1L);
        checkComponents(SplitTime.parse("-9999.999999999999995000e-4"), -1L,                  1L);
        checkComponents(SplitTime.parse("9.0e-20"), 0L, 0L);
        checkComponents(SplitTime.parse("4.0e-19"), 0L, 0L);
        checkComponents(SplitTime.parse("5.0e-19"), 0L, 0L);

        try {
            SplitTime.parse("A");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.CANNOT_PARSE_DATA, oe.getSpecifier());
            Assertions.assertEquals("A", oe.getParts()[0]);
        }

    }

    private void checkMultiple(final int n, final SplitTime small, final SplitTime large) {
        Assertions.assertTrue(small.multiply(n).subtract(large).isZero());
    }

    private void checkComponents(final SplitTime st , final long seconds, final long attoseconds) {
        Assertions.assertEquals(seconds,     st.getSeconds());
        Assertions.assertEquals(attoseconds, st.getAttoSeconds());
    }

}
