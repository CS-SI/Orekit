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

public class TimeOffsetTest {

    @Test
    public void testLongsConstructor() {
        final TimeOffset so01 = new TimeOffset(1234L, 5678L);
        Assertions.assertEquals(1234L,               so01.getSeconds());
        Assertions.assertEquals(5678L,               so01.getAttoSeconds());
        Assertions.assertTrue(so01.isFinite());
        final TimeOffset so02 = new TimeOffset(1234L, -5678L);
        Assertions.assertEquals(1233L,               so02.getSeconds());
        Assertions.assertEquals(999999999999994322L, so02.getAttoSeconds());
        Assertions.assertTrue(so02.isFinite());
        final TimeOffset so03 = new TimeOffset(1234L, -1L);
        Assertions.assertEquals(1233L,               so03.getSeconds());
        Assertions.assertEquals(999999999999999999L, so03.getAttoSeconds());
        Assertions.assertTrue(so03.isFinite());
        final TimeOffset so04 = new TimeOffset(1234L, -9223372036854775808L);
        Assertions.assertEquals(1224L,               so04.getSeconds());
        Assertions.assertEquals(776627963145224192L, so04.getAttoSeconds());
        Assertions.assertTrue(so04.isFinite());
        final TimeOffset so05 = new TimeOffset(1234L, 9223372036854775807L);
        Assertions.assertEquals(1243L,               so05.getSeconds());
        Assertions.assertEquals(223372036854775807L, so05.getAttoSeconds());
        Assertions.assertTrue(so05.isFinite());
        final TimeOffset so06 = new TimeOffset(1234L, 0L);
        Assertions.assertEquals(1234L,               so06.getSeconds());
        Assertions.assertEquals(0L,                  so06.getAttoSeconds());
        Assertions.assertTrue(so06.isFinite());
        final TimeOffset so07 = new TimeOffset(-1234L, 5678L);
        Assertions.assertEquals(-1234L,              so07.getSeconds());
        Assertions.assertEquals(5678L,               so07.getAttoSeconds());
        Assertions.assertTrue(so07.isFinite());
        final TimeOffset so08 = new TimeOffset(-1234L, -5678L);
        Assertions.assertEquals(-1235L,              so08.getSeconds());
        Assertions.assertEquals(999999999999994322L, so08.getAttoSeconds());
        Assertions.assertTrue(so08.isFinite());
        final TimeOffset so09 = new TimeOffset(-1234L, -1L);
        Assertions.assertEquals(-1235L,              so09.getSeconds());
        Assertions.assertEquals(999999999999999999L, so09.getAttoSeconds());
        Assertions.assertTrue(so09.isFinite());
        final TimeOffset so10 = new TimeOffset(-1234L, -9223372036854775808L);
        Assertions.assertEquals(-1244L,              so10.getSeconds());
        Assertions.assertEquals(776627963145224192L, so10.getAttoSeconds());
        Assertions.assertTrue(so10.isFinite());
        final TimeOffset so11 = new TimeOffset(-1234L, 9223372036854775807L);
        Assertions.assertEquals(-1225L,              so11.getSeconds());
        Assertions.assertEquals(223372036854775807L, so11.getAttoSeconds());
        Assertions.assertTrue(so11.isFinite());
        final TimeOffset so12 = new TimeOffset(-1234L, 0L);
        Assertions.assertEquals(-1234L,              so12.getSeconds());
        Assertions.assertEquals(0L,                  so12.getAttoSeconds());
        Assertions.assertTrue(so12.isFinite());
    }

    @Test
    public void testMultiplicativeConstructor() {
        final TimeOffset s101 = new TimeOffset(1L, 10000000000000000L);
        Assertions.assertEquals(1.01, s101.toDouble(), 1.0e-15);
        final TimeOffset p303 = new TimeOffset(3, s101);
        Assertions.assertEquals(3L, p303.getSeconds());
        Assertions.assertEquals(30000000000000000L, p303.getAttoSeconds());
        final TimeOffset m303 = new TimeOffset(-3, s101);
        Assertions.assertEquals(-4L, m303.getSeconds());
        Assertions.assertEquals(970000000000000000L, m303.getAttoSeconds());
    }

    @Test
    public void testLinear2Constructor() {
        final TimeOffset p3004 = new TimeOffset(3, TimeOffset.SECOND, 4, TimeOffset.MILLISECOND);
        Assertions.assertEquals(3L, p3004.getSeconds());
        Assertions.assertEquals(4000000000000000L, p3004.getAttoSeconds());
        final TimeOffset m3004 = new TimeOffset(-3, TimeOffset.SECOND, -4, TimeOffset.MILLISECOND);
        Assertions.assertEquals(-4L, m3004.getSeconds());
        Assertions.assertEquals(996000000000000000L, m3004.getAttoSeconds());
    }

    @Test
    public void testLinear3Constructor() {
        final TimeOffset p3004005 = new TimeOffset(3, TimeOffset.SECOND,
                                                   4, TimeOffset.MILLISECOND,
                                                   5, TimeOffset.MICROSECOND);
        Assertions.assertEquals(3L, p3004005.getSeconds());
        Assertions.assertEquals(4005000000000000L, p3004005.getAttoSeconds());
        final TimeOffset m3004005 = new TimeOffset(-3, TimeOffset.SECOND,
                                                   -4, TimeOffset.MILLISECOND,
                                                   -5, TimeOffset.MICROSECOND);
        Assertions.assertEquals(-4L, m3004005.getSeconds());
        Assertions.assertEquals(995995000000000000L, m3004005.getAttoSeconds());
    }

    @Test
    public void testLinear4Constructor() {
        final TimeOffset p3004005006 = new TimeOffset(3, TimeOffset.SECOND,
                                                      4, TimeOffset.MILLISECOND,
                                                      5, TimeOffset.MICROSECOND,
                                                      6, TimeOffset.NANOSECOND);
        Assertions.assertEquals(3L, p3004005006.getSeconds());
        Assertions.assertEquals(4005006000000000L, p3004005006.getAttoSeconds());
        final TimeOffset m3004005006 = new TimeOffset(-3, TimeOffset.SECOND,
                                                      -4, TimeOffset.MILLISECOND,
                                                      -5, TimeOffset.MICROSECOND,
                                                      -6, TimeOffset.NANOSECOND);
        Assertions.assertEquals(-4L, m3004005006.getSeconds());
        Assertions.assertEquals(995994994000000000L, m3004005006.getAttoSeconds());
    }

    @Test
    public void testLinear5Constructor() {
        final TimeOffset p3004005006007 = new TimeOffset(3, TimeOffset.SECOND,
                                                         4, TimeOffset.MILLISECOND,
                                                         5, TimeOffset.MICROSECOND,
                                                         6, TimeOffset.NANOSECOND,
                                                         7, TimeOffset.PICOSECOND);
        Assertions.assertEquals(3L, p3004005006007.getSeconds());
        Assertions.assertEquals(4005006007000000L, p3004005006007.getAttoSeconds());
        final TimeOffset m3004005006007 = new TimeOffset(-3, TimeOffset.SECOND,
                                                         -4, TimeOffset.MILLISECOND,
                                                         -5, TimeOffset.MICROSECOND,
                                                         -6, TimeOffset.NANOSECOND,
                                                         -7, TimeOffset.PICOSECOND);
        Assertions.assertEquals(-4L, m3004005006007.getSeconds());
        Assertions.assertEquals(995994993993000000L, m3004005006007.getAttoSeconds());
    }

    @Test
    public void testZero() {
        Assertions.assertTrue(new TimeOffset(0, 0).isZero());
        Assertions.assertFalse(new TimeOffset(0, 1).isZero());
        Assertions.assertFalse(new TimeOffset(1, 0).isZero());
        Assertions.assertFalse(new TimeOffset(1, 1).isZero());
    }

    @Test
    public void testRoundSeconds() {
        Assertions.assertEquals(1L,
                                new TimeOffset(1L, 499999999999999999L).getRoundedTime(TimeUnit.SECONDS));
        Assertions.assertEquals(2L,
                                new TimeOffset(1L, 500000000000000000L).getRoundedTime(TimeUnit.SECONDS));
    }

    @Test
    public void testPositiveDoubleContructor() {
        final double d = 123.4567890123456789;
        final TimeOffset so = new TimeOffset(d);
        Assertions.assertEquals(123L, so.getSeconds());
        // error is 1676 attosecond because the primitive double is not accurate enough
        Assertions.assertEquals(456789012345680576L, so.getAttoSeconds());
        Assertions.assertTrue(so.isFinite());
        Assertions.assertEquals(d, so.toDouble(), 1.0e-16);
    }

    @Test
    public void testNegativeDoubleContructor() {
        final double d = -123.4567890123456789;
        final TimeOffset so = new TimeOffset(d);
        Assertions.assertEquals(-124L, so.getSeconds());
        // error is 1676 attosecond because the primitive double is not accurate enough
        Assertions.assertEquals(543210987654319424L, so.getAttoSeconds());
        Assertions.assertTrue(so.isFinite());
        Assertions.assertEquals(d, so.toDouble(), 1.0e-16);
    }

    @Test
    public void testSmallNegativeDoubleContructor() {
        final TimeOffset so1 = new TimeOffset(-1.0e-17);
        Assertions.assertEquals(-1L, so1.getSeconds());
        Assertions.assertEquals(999999999999999990L, so1.getAttoSeconds());
        Assertions.assertTrue(so1.isFinite());
        final TimeOffset so2 = new TimeOffset(FastMath.nextDown(0.0));
        Assertions.assertEquals(-1L, so2.getSeconds());
        Assertions.assertEquals(1000000000000000000L, so2.getAttoSeconds());
        Assertions.assertTrue(so2.isFinite());
    }

    @Test
    public void testNaNDouble() {

        final TimeOffset nan = new TimeOffset(Double.NaN);
        Assertions.assertEquals(0L, nan.getSeconds());
        Assertions.assertTrue(nan.isNaN());
        Assertions.assertFalse(nan.isInfinite());
        Assertions.assertEquals(Long.MAX_VALUE, nan.getRoundedTime(TimeUnit.DAYS));
        Assertions.assertTrue(Double.isNaN(nan.toDouble()));

        Assertions.assertEquals(0L, TimeOffset.NaN.getSeconds());
        Assertions.assertTrue(TimeOffset.NaN.isNaN());
        Assertions.assertFalse(TimeOffset.NaN.isInfinite());
        Assertions.assertEquals(Long.MAX_VALUE, TimeOffset.NaN.getRoundedTime(TimeUnit.DAYS));
        Assertions.assertTrue(Double.isNaN(TimeOffset.NaN.toDouble()));

    }

    @Test
    public void testPositiveInfinity() {

        final TimeOffset pos = new TimeOffset(Double.POSITIVE_INFINITY);
        Assertions.assertEquals(Long.MAX_VALUE, pos.getSeconds());
        Assertions.assertFalse(pos.isNaN());
        Assertions.assertTrue(pos.isInfinite());
        Assertions.assertFalse(pos.isNegativeInfinity());
        Assertions.assertTrue(pos.isPositiveInfinity());
        Assertions.assertEquals(Long.MAX_VALUE, pos.getRoundedTime(TimeUnit.DAYS));
        Assertions.assertTrue(Double.isInfinite(pos.toDouble()));
        Assertions.assertTrue(pos.toDouble() > 0);

        Assertions.assertEquals(Long.MAX_VALUE, TimeOffset.POSITIVE_INFINITY.getSeconds());
        Assertions.assertFalse(TimeOffset.POSITIVE_INFINITY.isNaN());
        Assertions.assertTrue(TimeOffset.POSITIVE_INFINITY.isInfinite());
        Assertions.assertFalse(TimeOffset.POSITIVE_INFINITY.isNegativeInfinity());
        Assertions.assertTrue(TimeOffset.POSITIVE_INFINITY.isPositiveInfinity());
        Assertions.assertEquals(Long.MAX_VALUE, TimeOffset.POSITIVE_INFINITY.getRoundedTime(TimeUnit.DAYS));
        Assertions.assertTrue(Double.isInfinite(TimeOffset.POSITIVE_INFINITY.toDouble()));
        Assertions.assertTrue(TimeOffset.POSITIVE_INFINITY.toDouble() > 0);

    }

    @Test
    public void testNegativeInfinity() {

        final TimeOffset neg = new TimeOffset(Double.NEGATIVE_INFINITY);
        Assertions.assertEquals(Long.MIN_VALUE, neg.getSeconds());
        Assertions.assertFalse(neg.isNaN());
        Assertions.assertTrue(neg.isInfinite());
        Assertions.assertTrue(neg.isNegativeInfinity());
        Assertions.assertFalse(neg.isPositiveInfinity());
        Assertions.assertEquals(Long.MIN_VALUE, neg.getRoundedTime(TimeUnit.DAYS));
        Assertions.assertTrue(Double.isInfinite(neg.toDouble()));
        Assertions.assertTrue(neg.toDouble() < 0);

        Assertions.assertEquals(Long.MIN_VALUE, TimeOffset.NEGATIVE_INFINITY.getSeconds());
        Assertions.assertFalse(TimeOffset.NEGATIVE_INFINITY.isNaN());
        Assertions.assertTrue(TimeOffset.NEGATIVE_INFINITY.isInfinite());
        Assertions.assertTrue(TimeOffset.NEGATIVE_INFINITY.isNegativeInfinity());
        Assertions.assertFalse(TimeOffset.NEGATIVE_INFINITY.isPositiveInfinity());
        Assertions.assertEquals(Long.MIN_VALUE, TimeOffset.NEGATIVE_INFINITY.getRoundedTime(TimeUnit.DAYS));
        Assertions.assertTrue(Double.isInfinite(TimeOffset.NEGATIVE_INFINITY.toDouble()));
        Assertions.assertTrue(TimeOffset.NEGATIVE_INFINITY.toDouble() < 0);

    }

    @Test
    public void testOutOfRangeDouble() {

        final double limit = (double) Long.MAX_VALUE;

        final TimeOffset plus = new TimeOffset(limit);
        Assertions.assertEquals(limit, plus.getRoundedTime(TimeUnit.SECONDS), 1.0e-15 * limit);
        Assertions.assertFalse(plus.isNaN());
        Assertions.assertFalse(plus.isInfinite());

        final TimeOffset minus = new TimeOffset(-limit);
        Assertions.assertEquals(-limit, minus.getRoundedTime(TimeUnit.SECONDS), 1.0e-15 * limit);
        Assertions.assertFalse(minus.isNaN());
        Assertions.assertFalse(minus.isInfinite());

        final TimeOffset afterPlus = new TimeOffset(FastMath.nextAfter(limit, Double.POSITIVE_INFINITY));
        Assertions.assertFalse(afterPlus.isNaN());
        Assertions.assertTrue(afterPlus.isInfinite());
        Assertions.assertTrue(afterPlus.getSeconds() > 0L);
        Assertions.assertFalse(TimeOffset.POSITIVE_INFINITY.isNaN());
        Assertions.assertTrue(TimeOffset.POSITIVE_INFINITY.isInfinite());
        Assertions.assertTrue(TimeOffset.POSITIVE_INFINITY.getSeconds() > 0L);

        final TimeOffset beforeMinus = new TimeOffset(FastMath.nextAfter(-limit, Double.NEGATIVE_INFINITY));
        Assertions.assertFalse(beforeMinus.isNaN());
        Assertions.assertTrue(beforeMinus.isInfinite());
        Assertions.assertTrue(beforeMinus.getSeconds() < 0L);
        Assertions.assertFalse(beforeMinus.isNaN());
        Assertions.assertTrue(beforeMinus.isInfinite());
        Assertions.assertFalse(TimeOffset.NEGATIVE_INFINITY.isNaN());
        Assertions.assertTrue(TimeOffset.NEGATIVE_INFINITY.isInfinite());
        Assertions.assertTrue(TimeOffset.NEGATIVE_INFINITY.getSeconds() < 0L);

    }

    @Test
    public void testSumConstructor() {
        final TimeOffset so = new TimeOffset(new TimeOffset(1L, 902000000000000000L),
                                             new TimeOffset(3L, 904000000000000000L),
                                             new TimeOffset(5L, 906000000000000000L),
                                             new TimeOffset(7L, 908000000000000000L),
                                             new TimeOffset(9L, 910000000000000000L),
                                             new TimeOffset(11L, 912000000000000000L),
                                             new TimeOffset(13L, 914000000000000000L),
                                             new TimeOffset(15L, 916000000000000000L),
                                             new TimeOffset(17L, 918000000000000000L),
                                             new TimeOffset(19L, 920000000000000000L),
                                             new TimeOffset(21L, 922000000000000000L),
                                             new TimeOffset(23L, 924000000000000000L));
        Assertions.assertEquals(154L,                so.getSeconds());
        Assertions.assertEquals(956000000000000000L, so.getAttoSeconds());
    }

    @Test
    public void testDaysTimeUnit() {

        final TimeOffset days = new TimeOffset(2, TimeUnit.DAYS);
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

        final TimeOffset hours = new TimeOffset(2, TimeUnit.HOURS);
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

        final TimeOffset minutes = new TimeOffset(2, TimeUnit.MINUTES);
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

        final TimeOffset seconds = new TimeOffset(2, TimeUnit.SECONDS);
        Assertions.assertEquals(2L, seconds.getSeconds());
        Assertions.assertEquals(0L, seconds.getAttoSeconds());
        Assertions.assertTrue(seconds.isFinite());

    }

    @Test
    public void testMilliSecondsTimeUnit() {

        final TimeOffset milliSeconds = new TimeOffset(2, TimeUnit.MILLISECONDS);
        Assertions.assertEquals(0L,                milliSeconds.getSeconds());
        Assertions.assertEquals(2000000000000000L, milliSeconds.getAttoSeconds());
        Assertions.assertTrue(milliSeconds.isFinite());

    }

    @Test
    public void testMicroSecondsTimeUnit() {

        final TimeOffset microSeconds = new TimeOffset(2, TimeUnit.MICROSECONDS);
        Assertions.assertEquals(0L,             microSeconds.getSeconds());
        Assertions.assertEquals(2000000000000L, microSeconds.getAttoSeconds());
        Assertions.assertTrue(microSeconds.isFinite());

    }

    @Test
    public void testNanoTimeUnit() {

        final TimeOffset nanoSeconds = new TimeOffset(2, TimeUnit.NANOSECONDS);
        Assertions.assertEquals(0L,          nanoSeconds.getSeconds());
        Assertions.assertEquals(2000000000L, nanoSeconds.getAttoSeconds());
        Assertions.assertTrue(nanoSeconds.isFinite());

    }

    private void doTestOutOfRange(final long time, final TimeUnit unit) {

        final TimeOffset plus = new TimeOffset(time, unit);
        Assertions.assertEquals( time, plus.getRoundedTime(unit));
        Assertions.assertTrue(plus.isFinite());
        Assertions.assertFalse(plus.isNaN());
        Assertions.assertFalse(plus.isInfinite());

        final TimeOffset minus = new TimeOffset(-time, unit);
        Assertions.assertEquals(-time, minus.getRoundedTime(unit));
        Assertions.assertTrue(minus.isFinite());
        Assertions.assertFalse(minus.isNaN());
        Assertions.assertFalse(minus.isInfinite());

        final TimeOffset afterPlus = new TimeOffset(time + 1L, unit);
        Assertions.assertEquals(Long.MAX_VALUE, afterPlus.getRoundedTime(unit));
        Assertions.assertFalse(afterPlus.isFinite());
        Assertions.assertFalse(afterPlus.isNaN());
        Assertions.assertTrue(afterPlus.isInfinite());

        final TimeOffset beforeMinus = new TimeOffset(-time - 1L, unit);
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
                final TimeOffset timeOffset = new TimeOffset(time, tu);
                final long rebuilt = timeOffset.getRoundedTime(tu);
                if (timeOffset.isInfinite()) {
                    Assertions.assertEquals(time < 0 ? Long.MIN_VALUE : Long.MAX_VALUE, rebuilt);
                } else {
                    Assertions.assertEquals(time, rebuilt);
                }
            }
        }
    }

    @Test
    public void testAdd() {
        final TimeOffset a1 = new TimeOffset(1234L, 5678L).add(new TimeOffset(76L, -876L));
        Assertions.assertEquals(1310L, a1.getSeconds());
        Assertions.assertEquals(4802L, a1.getAttoSeconds());
        Assertions.assertTrue(a1.isFinite());

        final TimeOffset a2 = new TimeOffset(1L, 500000000000000000L).add(new TimeOffset(2L, 500000000000000001L));
        Assertions.assertEquals(4L, a2.getSeconds());
        Assertions.assertEquals(1L, a2.getAttoSeconds());
        Assertions.assertTrue(a2.isFinite());

        final TimeOffset a3 = new TimeOffset(1234L, 23L).add(new TimeOffset(76L, -876L));
        Assertions.assertEquals(1309L,               a3.getSeconds());
        Assertions.assertEquals(999999999999999147L, a3.getAttoSeconds());
        Assertions.assertTrue(a3.isFinite());
    }

    @Test
    public void testAddSpecialValues() {

        Assertions.assertTrue(TimeOffset.NaN.add(TimeOffset.HOUR).isNaN());
        Assertions.assertTrue(TimeOffset.HOUR.add(TimeOffset.NaN).isNaN());
        Assertions.assertTrue(TimeOffset.POSITIVE_INFINITY.add(TimeOffset.HOUR).isPositiveInfinity());
        Assertions.assertTrue(TimeOffset.HOUR.add(TimeOffset.POSITIVE_INFINITY).isPositiveInfinity());
        Assertions.assertTrue(TimeOffset.NEGATIVE_INFINITY.add(TimeOffset.HOUR).isNegativeInfinity());
        Assertions.assertTrue(TimeOffset.HOUR.add(TimeOffset.NEGATIVE_INFINITY).isNegativeInfinity());

        Assertions.assertTrue(TimeOffset.NaN.add(TimeOffset.NaN).isNaN());
        Assertions.assertTrue(TimeOffset.NaN.add(TimeOffset.NaN).isNaN());
        Assertions.assertTrue(TimeOffset.POSITIVE_INFINITY.add(TimeOffset.NaN).isNaN());
        Assertions.assertTrue(TimeOffset.NaN.add(TimeOffset.POSITIVE_INFINITY).isNaN());
        Assertions.assertTrue(TimeOffset.NEGATIVE_INFINITY.add(TimeOffset.NaN).isNaN());
        Assertions.assertTrue(TimeOffset.NaN.add(TimeOffset.NEGATIVE_INFINITY).isNaN());

        Assertions.assertTrue(TimeOffset.NaN.add(TimeOffset.POSITIVE_INFINITY).isNaN());
        Assertions.assertTrue(TimeOffset.POSITIVE_INFINITY.add(TimeOffset.NaN).isNaN());
        Assertions.assertTrue(TimeOffset.POSITIVE_INFINITY.add(TimeOffset.POSITIVE_INFINITY).isPositiveInfinity());
        Assertions.assertTrue(TimeOffset.NEGATIVE_INFINITY.add(TimeOffset.POSITIVE_INFINITY).isNaN());
        Assertions.assertTrue(TimeOffset.POSITIVE_INFINITY.add(TimeOffset.NEGATIVE_INFINITY).isNaN());

        Assertions.assertTrue(TimeOffset.NaN.add(TimeOffset.NEGATIVE_INFINITY).isNaN());
        Assertions.assertTrue(TimeOffset.NEGATIVE_INFINITY.add(TimeOffset.NaN).isNaN());
        Assertions.assertTrue(TimeOffset.POSITIVE_INFINITY.add(TimeOffset.NEGATIVE_INFINITY).isNaN());
        Assertions.assertTrue(TimeOffset.NEGATIVE_INFINITY.add(TimeOffset.POSITIVE_INFINITY).isNaN());
        Assertions.assertTrue(TimeOffset.NEGATIVE_INFINITY.add(TimeOffset.NEGATIVE_INFINITY).isNegativeInfinity());

    }

    @Test
    public void testSubtract() {
        final TimeOffset s1 = new TimeOffset(1234L, 5678L).subtract(new TimeOffset(76L, 876L));
        Assertions.assertEquals(1158L, s1.getSeconds());
        Assertions.assertEquals(4802L, s1.getAttoSeconds());
        Assertions.assertTrue(s1.isFinite());

        final TimeOffset s2 = new TimeOffset(1L, 0L).subtract(new TimeOffset(2L, 1L));
        Assertions.assertEquals(-2L, s2.getSeconds());
        Assertions.assertEquals(999999999999999999L, s2.getAttoSeconds());
        Assertions.assertTrue(s2.isFinite());

        final TimeOffset s3 = new TimeOffset(1234L, 999999999999999999L).subtract(new TimeOffset(76L, 123456L));
        Assertions.assertEquals(1158L,               s3.getSeconds());
        Assertions.assertEquals(999999999999876543L, s3.getAttoSeconds());
        Assertions.assertTrue(s3.isFinite());
    }

    @Test
    public void testSubtractSpecialValues() {

        Assertions.assertTrue(TimeOffset.NaN.subtract(TimeOffset.HOUR).isNaN());
        Assertions.assertTrue(TimeOffset.HOUR.subtract(TimeOffset.NaN).isNaN());
        Assertions.assertTrue(TimeOffset.POSITIVE_INFINITY.subtract(TimeOffset.HOUR).isPositiveInfinity());
        Assertions.assertTrue(TimeOffset.HOUR.subtract(TimeOffset.POSITIVE_INFINITY).isNegativeInfinity());
        Assertions.assertTrue(TimeOffset.NEGATIVE_INFINITY.subtract(TimeOffset.HOUR).isNegativeInfinity());
        Assertions.assertTrue(TimeOffset.HOUR.subtract(TimeOffset.NEGATIVE_INFINITY).isPositiveInfinity());

        Assertions.assertTrue(TimeOffset.NaN.subtract(TimeOffset.NaN).isNaN());
        Assertions.assertTrue(TimeOffset.NaN.subtract(TimeOffset.NaN).isNaN());
        Assertions.assertTrue(TimeOffset.POSITIVE_INFINITY.subtract(TimeOffset.NaN).isNaN());
        Assertions.assertTrue(TimeOffset.NaN.subtract(TimeOffset.POSITIVE_INFINITY).isNaN());
        Assertions.assertTrue(TimeOffset.NEGATIVE_INFINITY.subtract(TimeOffset.NaN).isNaN());
        Assertions.assertTrue(TimeOffset.NaN.subtract(TimeOffset.NEGATIVE_INFINITY).isNaN());

        Assertions.assertTrue(TimeOffset.NaN.subtract(TimeOffset.POSITIVE_INFINITY).isNaN());
        Assertions.assertTrue(TimeOffset.POSITIVE_INFINITY.subtract(TimeOffset.NaN).isNaN());
        Assertions.assertTrue(TimeOffset.POSITIVE_INFINITY.subtract(TimeOffset.POSITIVE_INFINITY).isNaN());
        Assertions.assertTrue(TimeOffset.NEGATIVE_INFINITY.subtract(TimeOffset.POSITIVE_INFINITY).isNegativeInfinity());
        Assertions.assertTrue(TimeOffset.POSITIVE_INFINITY.subtract(TimeOffset.NEGATIVE_INFINITY).isPositiveInfinity());

        Assertions.assertTrue(TimeOffset.NaN.subtract(TimeOffset.NEGATIVE_INFINITY).isNaN());
        Assertions.assertTrue(TimeOffset.NEGATIVE_INFINITY.subtract(TimeOffset.NaN).isNaN());
        Assertions.assertTrue(TimeOffset.POSITIVE_INFINITY.subtract(TimeOffset.NEGATIVE_INFINITY).isPositiveInfinity());
        Assertions.assertTrue(TimeOffset.NEGATIVE_INFINITY.subtract(TimeOffset.POSITIVE_INFINITY).isNegativeInfinity());
        Assertions.assertTrue(TimeOffset.NEGATIVE_INFINITY.subtract(TimeOffset.NEGATIVE_INFINITY).isNaN());

    }

    @Test
    public void testMultiply() {
        try {
            new TimeOffset(1L, 45L).multiply(-1);
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.NOT_POSITIVE, oe.getSpecifier());
            Assertions.assertEquals(-1, (Long) oe.getParts()[0]);
        }
        checkComponents(new TimeOffset(1L, 45L).multiply(0), 0L, 0L);
        checkComponents(new TimeOffset(1L, 45L).multiply(1), 1L, 45L);
        checkComponents(new TimeOffset(1L, 45L).multiply(3), 3L, 135L);
        checkComponents(new TimeOffset(1234L, 123456789012345678L).multiply(7233L), 8926414L, 962954926296288974L);
        checkComponents(new TimeOffset(1234L, 999999999999999999L).multiply(23012696L), 28420679559L, 999999999976987304L);
        checkComponents(new TimeOffset(1234L, 999999999999999999L).multiply(123456789012L),
                        152469134429819L, 999999876543210988L);
         try {
             new TimeOffset(10000000000L, 1L).multiply(123456789012L);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(LocalizedCoreFormats.OVERFLOW_IN_MULTIPLICATION, oe.getSpecifier());
            Assertions.assertEquals(10000000000L, (Long) oe.getParts()[0]);
            Assertions.assertEquals(123456789012L, (Long) oe.getParts()[1]);
        }

        try {
            new TimeOffset(922382683L, 717054400620018329L).multiply(1573105907129L);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(LocalizedCoreFormats.OVERFLOW_IN_MULTIPLICATION, oe.getSpecifier());
            Assertions.assertEquals(922382683L, (Long) oe.getParts()[0]);
            Assertions.assertEquals(1573105907129L, (Long) oe.getParts()[1]);
        }
    }

    @Test
    public void testMultiplySpecialValues() {
        Assertions.assertTrue(TimeOffset.NEGATIVE_INFINITY.multiply(0).isNaN());
        Assertions.assertTrue(TimeOffset.NEGATIVE_INFINITY.multiply(3).isNegativeInfinity());
        Assertions.assertTrue(TimeOffset.POSITIVE_INFINITY.multiply(0).isNaN());
        Assertions.assertTrue(TimeOffset.POSITIVE_INFINITY.multiply(3).isPositiveInfinity());
        Assertions.assertTrue(TimeOffset.NaN.multiply(0).isNaN());
        Assertions.assertTrue(TimeOffset.NaN.multiply(3).isNaN());
    }

    @Test
    public void testDivide() {
        try {
            new TimeOffset(1L, 45L).divide(0);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.NOT_STRICTLY_POSITIVE, oe.getSpecifier());
            Assertions.assertEquals(0, (Integer) oe.getParts()[0]);
        }
        checkComponents(new TimeOffset(1L, 45L).divide(1), 1L, 45L);
        checkComponents(new TimeOffset(3L, 135L).divide(3), 1L, 45L);
        checkComponents(new TimeOffset(8926414L, 962954926296288974L).divide(7233), 1234L, 123456789012345678L);
        checkComponents(new TimeOffset(28420679559L, 999999999976987304L).divide(23012696), 1234L, 999999999999999999L);
        checkComponents(new TimeOffset(1L, 0L).divide(1000000000), 0L, 1000000000L);

        // we consider a 15 nanosecond per UTC second slope for TAI-UTC offset (this is what was used in 1961)
        // then 1 day in UTC corresponds to 1 day + 1296 µs in TAI, and we perform the computation the other way round
        // we start from the 1 day + 1296 µs duration in TAI and recover the 1296 µs change in TAI-UTC offset
        checkComponents(new TimeOffset(86400L, 1296000000000000L).multiply(15).divide(1000000015),
                        0L, 1296000000000000L);

    }

    @Test
    public void testRandomDivide() {
        final RandomGenerator random = new Well1024a(0x83977774b8d4eb2eL);
        for (int i = 0; i < 1000000; i++) {
            final TimeOffset t = new TimeOffset(random.nextLong(1000L * 365L * 86400L),
                                                random.nextLong(1000000000000000000L));
            final int p = FastMath.max(1, random.nextInt(10000000));
            Assertions.assertEquals(0, t.compareTo(t.multiply(p).divide(p)));
        }

    }

    @Test
    public void testDivideSpecialValues() {
        Assertions.assertTrue(TimeOffset.NEGATIVE_INFINITY.divide(3).isNegativeInfinity());
        Assertions.assertTrue(TimeOffset.POSITIVE_INFINITY.divide(3).isPositiveInfinity());
        Assertions.assertTrue(TimeOffset.NaN.divide(3).isNaN());
    }

    @Test
    public void testNegate() {
        for (long s = -1000L; s < 1000L; s += 10L) {
            for (long a = -1000L; a < 1000L; a += 10L) {
                final TimeOffset st = new TimeOffset(s, a);
                Assertions.assertTrue(st.add(st.negate()).isZero());
            }
        }
        Assertions.assertTrue(TimeOffset.POSITIVE_INFINITY.negate().isNegativeInfinity());
        Assertions.assertTrue(TimeOffset.NEGATIVE_INFINITY.negate().isPositiveInfinity());
        Assertions.assertTrue(TimeOffset.NaN.negate().isNaN());
        Assertions.assertTrue(TimeOffset.ZERO.negate().isZero());
    }

    @Test
    public void testCompareFinite() {
        Assertions.assertTrue(TimeOffset.ZERO.compareTo(new TimeOffset(0L, 0L)) == 0);
        Assertions.assertTrue(new TimeOffset(1L, 1L).compareTo(new TimeOffset(1L, 1L)) == 0);
        Assertions.assertTrue(TimeOffset.MICROSECOND.compareTo(TimeOffset.MILLISECOND) < 0);
        Assertions.assertTrue(TimeOffset.MILLISECOND.compareTo(TimeOffset.MICROSECOND) > 0);
        Assertions.assertTrue(new TimeOffset(1, 2).compareTo(new TimeOffset(1, 2)) == 0);
        Assertions.assertTrue(new TimeOffset(1, 1).compareTo(new TimeOffset(1, 2))  < 0);
        Assertions.assertTrue(new TimeOffset(1, 2).compareTo(new TimeOffset(1, 1))  > 0);
        Assertions.assertTrue(new TimeOffset(2, 1).compareTo(new TimeOffset(2, 1)) == 0);
        Assertions.assertTrue(new TimeOffset(1, 1).compareTo(new TimeOffset(2, 1))  < 0);
        Assertions.assertTrue(new TimeOffset(2, 1).compareTo(new TimeOffset(1, 1))  > 0);
    }

    @Test
    public void testCompareSpecialCases() {

        Assertions.assertTrue(TimeOffset.NaN.compareTo(TimeOffset.NaN)               == 0);
        Assertions.assertTrue(TimeOffset.NaN.compareTo(TimeOffset.ATTOSECOND)        >  0);
        Assertions.assertTrue(TimeOffset.NaN.compareTo(TimeOffset.POSITIVE_INFINITY) >  0);
        Assertions.assertTrue(TimeOffset.NaN.compareTo(TimeOffset.NEGATIVE_INFINITY) >  0);

        Assertions.assertTrue(TimeOffset.ATTOSECOND.compareTo(TimeOffset.NaN)               <  0);
        Assertions.assertTrue(TimeOffset.ATTOSECOND.compareTo(TimeOffset.ATTOSECOND)        == 0);
        Assertions.assertTrue(TimeOffset.ATTOSECOND.compareTo(TimeOffset.POSITIVE_INFINITY) <  0);
        Assertions.assertTrue(TimeOffset.ATTOSECOND.compareTo(TimeOffset.NEGATIVE_INFINITY) >  0);

        Assertions.assertTrue(TimeOffset.POSITIVE_INFINITY.compareTo(TimeOffset.NaN)               <  0);
        Assertions.assertTrue(TimeOffset.POSITIVE_INFINITY.compareTo(TimeOffset.ATTOSECOND)        >  0);
        Assertions.assertTrue(TimeOffset.POSITIVE_INFINITY.compareTo(TimeOffset.POSITIVE_INFINITY) == 0);
        Assertions.assertTrue(TimeOffset.POSITIVE_INFINITY.compareTo(TimeOffset.NEGATIVE_INFINITY) >  0);

        Assertions.assertTrue(TimeOffset.NEGATIVE_INFINITY.compareTo(TimeOffset.NaN)               <  0);
        Assertions.assertTrue(TimeOffset.NEGATIVE_INFINITY.compareTo(TimeOffset.ATTOSECOND)        <  0);
        Assertions.assertTrue(TimeOffset.NEGATIVE_INFINITY.compareTo(TimeOffset.POSITIVE_INFINITY) <  0);
        Assertions.assertTrue(TimeOffset.NEGATIVE_INFINITY.compareTo(TimeOffset.NEGATIVE_INFINITY) == 0);

    }

    @Test
    public void testEquals() {
        TimeOffset[] st = new TimeOffset[] {
          new TimeOffset(200L, 300L), new TimeOffset(200L, 199L), new TimeOffset(199L, 300L),
          TimeOffset.POSITIVE_INFINITY, TimeOffset.NEGATIVE_INFINITY,
          TimeOffset.NaN, TimeOffset.DAY, TimeOffset.ZERO, TimeOffset.ATTOSECOND
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
        final TimeOffset timeOffset = new TimeOffset(200L, 300L);
        Assertions.assertEquals(st[0], timeOffset);
        Assertions.assertNotSame(st[0], timeOffset);
        Assertions.assertEquals(0,      st[0].compareTo(timeOffset));
        Assertions.assertEquals(484,         st[0].hashCode());
        Assertions.assertEquals(484, timeOffset.hashCode());
        Assertions.assertEquals(254, TimeOffset.NaN.hashCode());
        Assertions.assertEquals(-2130771969, TimeOffset.NEGATIVE_INFINITY.hashCode());
        Assertions.assertEquals(-2147418369, TimeOffset.POSITIVE_INFINITY.hashCode());
        Assertions.assertEquals(0, TimeOffset.ZERO.hashCode());
        Assertions.assertEquals(1, TimeOffset.ATTOSECOND.hashCode());
        Assertions.assertNotEquals(timeOffset, "timeOffset");
    }

    @Test
    public void testMultiples() {
        for (int n = 0; n < 100; ++n) {
            checkMultiple(n, TimeOffset.ZERO, TimeOffset.ZERO);
        }
        checkMultiple(1000, TimeOffset.ATTOSECOND, TimeOffset.FEMTOSECOND);
        checkMultiple(1000, TimeOffset.FEMTOSECOND, TimeOffset.PICOSECOND);
        checkMultiple(1000, TimeOffset.PICOSECOND, TimeOffset.NANOSECOND);
        checkMultiple(1000, TimeOffset.NANOSECOND, TimeOffset.MICROSECOND);
        checkMultiple(1000, TimeOffset.MICROSECOND, TimeOffset.MILLISECOND);
        checkMultiple(1000, TimeOffset.MILLISECOND, TimeOffset.SECOND);
        checkMultiple(60, TimeOffset.SECOND, TimeOffset.MINUTE);
        checkMultiple(60, TimeOffset.MINUTE, TimeOffset.HOUR);
        checkMultiple(24, TimeOffset.HOUR, TimeOffset.DAY);
        checkMultiple(86401, TimeOffset.SECOND, TimeOffset.DAY_WITH_POSITIVE_LEAP);
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
            TimeOffset.parse(s);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.CANNOT_PARSE_DATA, oe.getSpecifier());
            Assertions.assertEquals(s, oe.getParts()[0]);
        }
    }

    @Test
    public void testParseSpecialValues() {
        Assertions.assertTrue(TimeOffset.parse("NaN").isNaN());
        Assertions.assertTrue(TimeOffset.parse("NAN").isNaN());
        Assertions.assertTrue(TimeOffset.parse("nan").isNaN());
        Assertions.assertTrue(TimeOffset.parse("-∞").isNegativeInfinity());
        Assertions.assertTrue(TimeOffset.parse("+∞").isPositiveInfinity());
    }

    @Test
    public void testParseLargeExponents(){
        Assertions.assertTrue(TimeOffset.parse("-1.0e100").isNegativeInfinity());
        Assertions.assertTrue(TimeOffset.parse("+1.0e100").isPositiveInfinity());
        Assertions.assertTrue(TimeOffset.parse("-0.1e100").isNegativeInfinity());
        Assertions.assertTrue(TimeOffset.parse("+0.1e100").isPositiveInfinity());
        Assertions.assertTrue(TimeOffset.parse("-1.1e100").isNegativeInfinity());
        Assertions.assertTrue(TimeOffset.parse("+1.1e100").isPositiveInfinity());
        checkComponents(TimeOffset.parse("0.0e200000"), 0L, 0L);
    }

    @Test
    public void testParse() {
        checkComponents(TimeOffset.parse("0"), 0L, 0L);
        checkComponents(TimeOffset.parse("1"), 1L, 0L);
        checkComponents(TimeOffset.parse("-1"), -1L, 0L);
        checkComponents(TimeOffset.parse("+0.5"), 0L, 500000000000000000L);
        checkComponents(TimeOffset.parse("-0.5"), -1L, 500000000000000000L);
        checkComponents(TimeOffset.parse("+.5"), 0L, 500000000000000000L);
        checkComponents(TimeOffset.parse("-.5"), -1L, 500000000000000000L);
        checkComponents(TimeOffset.parse("17.42357e+02"), 1742L, 357000000000000000L);
        checkComponents(TimeOffset.parse("9223372036854775807"), Long.MAX_VALUE, 0L);

        // these are the offsets for linear UTC/TAI models before 1972
        checkComponents(TimeOffset.parse("1.4228180"), 1L, 422818000000000000L);
        checkComponents(TimeOffset.parse("1.3728180"), 1L, 372818000000000000L);
        checkComponents(TimeOffset.parse("1.8458580"), 1L, 845858000000000000L);
        checkComponents(TimeOffset.parse("1.9458580"), 1L, 945858000000000000L);
        checkComponents(TimeOffset.parse("3.2401300"), 3L, 240130000000000000L);
        checkComponents(TimeOffset.parse("3.3401300"), 3L, 340130000000000000L);
        checkComponents(TimeOffset.parse("3.4401300"), 3L, 440130000000000000L);
        checkComponents(TimeOffset.parse("3.5401300"), 3L, 540130000000000000L);
        checkComponents(TimeOffset.parse("3.6401300"), 3L, 640130000000000000L);
        checkComponents(TimeOffset.parse("3.7401300"), 3L, 740130000000000000L);
        checkComponents(TimeOffset.parse("3.8401300"), 3L, 840130000000000000L);
        checkComponents(TimeOffset.parse("4.3131700"), 4L, 313170000000000000L);
        checkComponents(TimeOffset.parse("4.2131700"), 4L, 213170000000000000L);

        // these are the drifts for linear UTC/TAI models before 1972
        checkComponents(TimeOffset.parse("0.001296"), 0L, 1296000000000000L);
        checkComponents(TimeOffset.parse("0.0011232"), 0L, 1123200000000000L);
        checkComponents(TimeOffset.parse("0.002592"), 0L, 2592000000000000L);

        // cases with exponents
        checkComponents(TimeOffset.parse("0.001234e-05"), 0L, 12340000000L);
        checkComponents(TimeOffset.parse("-0.001234E+05"), -124L, 600000000000000000L);
        checkComponents(TimeOffset.parse("-0.001234E-05"), -1L, 999999987660000000L);
        checkComponents(TimeOffset.parse("0.001e-15"), 0L, 1L);
        checkComponents(TimeOffset.parse("-0.001e-15"), -1L, 999999999999999999L);
        checkComponents(TimeOffset.parse("-12E-1"), -2L, 800000000000000000L);
        checkComponents(TimeOffset.parse("-12E0"), -12L, 0L);
        checkComponents(TimeOffset.parse("-12E-0"), -12L, 0L);
        checkComponents(TimeOffset.parse("-12E+0"), -12L, 0L);
        checkComponents(TimeOffset.parse("1.234e-50"), 0L, 0L);
        checkComponents(TimeOffset.parse("1.e2"), 100L, 0L);

        // ignoring extra digits after separator
        checkComponents(TimeOffset.parse("0.12345678901234567890123456"), 0L, 123456789012345678L);

        // various resolutions
        checkComponents(TimeOffset.parse("12.3456e-20"), 0L, 0L);
        checkComponents(TimeOffset.parse("12.3456e-19"), 0L, 1L);
        checkComponents(TimeOffset.parse("12.3456e-18"), 0L, 12L);
        checkComponents(TimeOffset.parse("12.3456e-17"), 0L, 123L);
        checkComponents(TimeOffset.parse("12.3456e-16"), 0L, 1234L);
        checkComponents(TimeOffset.parse("12.3456e-15"), 0L, 12345L);
        checkComponents(TimeOffset.parse("12.3456e-14"), 0L, 123456L);
        checkComponents(TimeOffset.parse("12.3456e-13"), 0L, 1234560L);
        checkComponents(TimeOffset.parse("12.3456e-12"), 0L, 12345600L);
        checkComponents(TimeOffset.parse("12.3456e-11"), 0L, 123456000L);
        checkComponents(TimeOffset.parse("12.3456e-10"), 0L, 1234560000L);
        checkComponents(TimeOffset.parse("12.3456e-09"), 0L, 12345600000L);
        checkComponents(TimeOffset.parse("12.3456e-08"), 0L, 123456000000L);
        checkComponents(TimeOffset.parse("12.3456e-07"), 0L, 1234560000000L);
        checkComponents(TimeOffset.parse("12.3456e-06"), 0L, 12345600000000L);
        checkComponents(TimeOffset.parse("12.3456e-05"), 0L, 123456000000000L);
        checkComponents(TimeOffset.parse("12.3456e-04"), 0L, 1234560000000000L);
        checkComponents(TimeOffset.parse("12.3456e-03"), 0L, 12345600000000000L);
        checkComponents(TimeOffset.parse("12.3456e-02"), 0L, 123456000000000000L);
        checkComponents(TimeOffset.parse("12.3456e-01"), 1L, 234560000000000000L);
        checkComponents(TimeOffset.parse("12.3456e+00"), 12L, 345600000000000000L);
        checkComponents(TimeOffset.parse("12.3456e+01"), 123L, 456000000000000000L);
        checkComponents(TimeOffset.parse("12.3456e+02"), 1234L, 560000000000000000L);
        checkComponents(TimeOffset.parse("12.3456e+03"), 12345L, 600000000000000000L);
        checkComponents(TimeOffset.parse("12.3456e+04"), 123456L, 0L);
        checkComponents(TimeOffset.parse("12.3456e+05"), 1234560L, 0L);
        checkComponents(TimeOffset.parse("12.3456e+06"), 12345600L, 0L);
        checkComponents(TimeOffset.parse("12.3456e+07"), 123456000L, 0L);
        checkComponents(TimeOffset.parse("12.3456e+08"), 1234560000L, 0L);
        checkComponents(TimeOffset.parse("12.3456e+09"), 12345600000L, 0L);
        checkComponents(TimeOffset.parse("12.3456e+10"), 123456000000L, 0L);
        checkComponents(TimeOffset.parse("12.3456e+11"), 1234560000000L, 0L);
        checkComponents(TimeOffset.parse("12.3456e+12"), 12345600000000L, 0L);
        checkComponents(TimeOffset.parse("12.3456e+13"), 123456000000000L, 0L);
        checkComponents(TimeOffset.parse("12.3456e+14"), 1234560000000000L, 0L);
        checkComponents(TimeOffset.parse("12.3456e+15"), 12345600000000000L, 0L);
        checkComponents(TimeOffset.parse("12.3456e+16"), 123456000000000000L, 0L);
        checkComponents(TimeOffset.parse("12.3456e+17"), 1234560000000000000L, 0L);

        // truncating to upper attosecond (NOT rounding!)
        checkComponents(TimeOffset.parse("+1234.567890123456785012e-4"), 0L, 123456789012345678L);
        checkComponents(TimeOffset.parse("-1234.567890123456785012e-4"), -1L, 876543210987654322L);
        checkComponents(TimeOffset.parse("+1234.567890123456784012e-4"), 0L, 123456789012345678L);
        checkComponents(TimeOffset.parse("-1234.567890123456784012e-4"), -1L, 876543210987654322L);
        checkComponents(TimeOffset.parse("+9999.999999999999994000e-4"), 0L, 999999999999999999L);
        checkComponents(TimeOffset.parse("+9999.999999999999995000e-4"), 0L, 999999999999999999L);
        checkComponents(TimeOffset.parse("-9999.999999999999994000e-4"), -1L, 1L);
        checkComponents(TimeOffset.parse("-9999.999999999999995000e-4"), -1L, 1L);
        checkComponents(TimeOffset.parse("9.0e-20"), 0L, 0L);
        checkComponents(TimeOffset.parse("4.0e-19"), 0L, 0L);
        checkComponents(TimeOffset.parse("5.0e-19"), 0L, 0L);

        try {
            TimeOffset.parse("A");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.CANNOT_PARSE_DATA, oe.getSpecifier());
            Assertions.assertEquals("A", oe.getParts()[0]);
        }

    }

    private void checkMultiple(final int n, final TimeOffset small, final TimeOffset large) {
        Assertions.assertTrue(small.multiply(n).subtract(large).isZero());
    }

    private void checkComponents(final TimeOffset st , final long seconds, final long attoseconds) {
        Assertions.assertEquals(seconds,     st.getSeconds());
        Assertions.assertEquals(attoseconds, st.getAttoSeconds());
    }

}
