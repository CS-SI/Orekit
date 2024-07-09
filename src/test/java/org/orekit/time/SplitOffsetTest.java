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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

import java.util.concurrent.TimeUnit;

public class SplitOffsetTest {

    @Test
    public void testLongsConstructor() {
        final SplitOffset so01 = new SplitOffset(1234L, 5678L);
        Assertions.assertEquals(1234L,               so01.getSeconds());
        Assertions.assertEquals(5678L,               so01.getAttoSeconds());
        final SplitOffset so02 = new SplitOffset(1234L, -5678L);
        Assertions.assertEquals(1233L,               so02.getSeconds());
        Assertions.assertEquals(999999999999994322L, so02.getAttoSeconds());
        final SplitOffset so03 = new SplitOffset(1234L, -1L);
        Assertions.assertEquals(1233L,               so03.getSeconds());
        Assertions.assertEquals(999999999999999999L, so03.getAttoSeconds());
        final SplitOffset so04 = new SplitOffset(1234L, -9223372036854775808L);
        Assertions.assertEquals(1224L,               so04.getSeconds());
        Assertions.assertEquals(776627963145224192L, so04.getAttoSeconds());
        final SplitOffset so05 = new SplitOffset(1234L, 9223372036854775807L);
        Assertions.assertEquals(1243L,               so05.getSeconds());
        Assertions.assertEquals(223372036854775807L, so05.getAttoSeconds());
        final SplitOffset so06 = new SplitOffset(1234L, 0L);
        Assertions.assertEquals(1234L,               so06.getSeconds());
        Assertions.assertEquals(0L,                  so06.getAttoSeconds());
        final SplitOffset so07 = new SplitOffset(-1234L, 5678L);
        Assertions.assertEquals(-1234L,              so07.getSeconds());
        Assertions.assertEquals(5678L,               so07.getAttoSeconds());
        final SplitOffset so08 = new SplitOffset(-1234L, -5678L);
        Assertions.assertEquals(-1235L,              so08.getSeconds());
        Assertions.assertEquals(999999999999994322L, so08.getAttoSeconds());
        final SplitOffset so09 = new SplitOffset(-1234L, -1L);
        Assertions.assertEquals(-1235L,              so09.getSeconds());
        Assertions.assertEquals(999999999999999999L, so09.getAttoSeconds());
        final SplitOffset so10 = new SplitOffset(-1234L, -9223372036854775808L);
        Assertions.assertEquals(-1244L,              so10.getSeconds());
        Assertions.assertEquals(776627963145224192L, so10.getAttoSeconds());
        final SplitOffset so11 = new SplitOffset(-1234L, 9223372036854775807L);
        Assertions.assertEquals(-1225L,              so11.getSeconds());
        Assertions.assertEquals(223372036854775807L, so11.getAttoSeconds());
        final SplitOffset so12 = new SplitOffset(-1234L, 0L);
        Assertions.assertEquals(-1234L,              so12.getSeconds());
        Assertions.assertEquals(0L,                  so12.getAttoSeconds());
    }

    @Test
    public void testDoubleContructor() {
        final SplitOffset so = new SplitOffset(123.4567890123456789);
        Assertions.assertEquals(123L, so.getSeconds());
        Assertions.assertEquals(456789012345680576L, so.getAttoSeconds());
    }

    @Test
    public void testDaysTimeUnit() {

        final SplitOffset days = new SplitOffset(2, TimeUnit.DAYS);
        Assertions.assertEquals(172800L, days.getSeconds());
        Assertions.assertEquals(0L,      days.getAttoSeconds());

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

    }

    @Test
    public void testMilliSecondsTimeUnit() {

        final SplitOffset milliSeconds = new SplitOffset(2, TimeUnit.MILLISECONDS);
        Assertions.assertEquals(0L,                milliSeconds.getSeconds());
        Assertions.assertEquals(2000000000000000L, milliSeconds.getAttoSeconds());

    }

    @Test
    public void testMicroSecondsTimeUnit() {

        final SplitOffset microSeconds = new SplitOffset(2, TimeUnit.MICROSECONDS);
        Assertions.assertEquals(0L,             microSeconds.getSeconds());
        Assertions.assertEquals(2000000000000L, microSeconds.getAttoSeconds());

    }

    @Test
    public void testNanoTimeUnit() {

        final SplitOffset nanoSeconds = new SplitOffset(2, TimeUnit.NANOSECONDS);
        Assertions.assertEquals(0L,          nanoSeconds.getSeconds());
        Assertions.assertEquals(2000000000L, nanoSeconds.getAttoSeconds());

    }

    private void doTestOutOfRange(final long offset, final TimeUnit unit) {
        Assertions.assertEquals(offset, new SplitOffset( offset, unit).getRoundedOffset(unit));
        Assertions.assertEquals(offset, new SplitOffset(-offset, unit).getRoundedOffset(unit));
        checkException( offset + 1L, unit);
        checkException(-offset - 1L, unit);
    }

    private void checkException(final long offset, final TimeUnit unit) {
        try {
            new SplitOffset(offset, unit);
            Assertions.fail("an exceptions should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.OFFSET_OUT_OF_RANGE_FOR_TIME_UNIT, oe.getSpecifier());
            Assertions.assertEquals(offset, oe.getParts()[0]);
            Assertions.assertEquals(unit,   oe.getParts()[1]);
        }
    }

    @Test
    public void testOffsetSymmetry() {

         for (long o : new long[] {
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
                try {
                    Assertions.assertEquals(o, new SplitOffset(o, tu).getRoundedOffset(tu));
                } catch (OrekitException oe) {
                    // some offset/time unit combinations in the test trigger
                    // out of range exceptions, this is expected
                    Assertions.assertEquals(OrekitMessages.OFFSET_OUT_OF_RANGE_FOR_TIME_UNIT, oe.getSpecifier());
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

        final SplitOffset add2 = SplitOffset.add(new SplitOffset(1L, 500000000000000000L),
                                                new SplitOffset(2L, 500000000000000001L));
        Assertions.assertEquals(4L, add2.getSeconds());
        Assertions.assertEquals(1L, add2.getAttoSeconds());

        final SplitOffset add3 = SplitOffset.add(new SplitOffset(1234L, 23L),
                                                new SplitOffset(76L, -876L));
        Assertions.assertEquals(1309L,               add3.getSeconds());
        Assertions.assertEquals(999999999999999147L, add3.getAttoSeconds());
    }

    @Test
    public void testSubtract() {
        final SplitOffset sub1 = SplitOffset.subtract(new SplitOffset(1234L, 5678L),
                                                      new SplitOffset(76L, 876L));
        Assertions.assertEquals(1158L, sub1.getSeconds());
        Assertions.assertEquals(4802L, sub1.getAttoSeconds());

        final SplitOffset sub2 = SplitOffset.subtract(new SplitOffset(1L, 0L),
                                                      new SplitOffset(2L, 1L));
        Assertions.assertEquals(-2L, sub2.getSeconds());
        Assertions.assertEquals(999999999999999999L, sub2.getAttoSeconds());

        final SplitOffset sub3 = SplitOffset.subtract(new SplitOffset(1234L, 999999999999999999L),
                                                      new SplitOffset(76L, 123456L));
        Assertions.assertEquals(1158L,               sub3.getSeconds());
        Assertions.assertEquals(999999999999876543L, sub3.getAttoSeconds());
    }

}
