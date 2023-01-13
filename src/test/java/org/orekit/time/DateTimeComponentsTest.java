/* Copyright 2002-2023 CS GROUP
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

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class DateTimeComponentsTest {

    @SuppressWarnings("unlikely-arg-type")
    @Test
    public void testComparisons() {
        DateTimeComponents[] dates = {
                new DateTimeComponents(2003,  1,  1, 7, 15, 33),
                new DateTimeComponents(2003,  1,  1, 7, 15, 34),
                new DateTimeComponents(2003,  1,  1, 7, 16, 34),
                new DateTimeComponents(2003,  1,  1, 8, 16, 34),
                new DateTimeComponents(2003,  1,  2, 8, 16, 34),
                new DateTimeComponents(2003,  2,  2, 8, 16, 34),
                new DateTimeComponents(2004,  2,  2, 8, 16, 34)
        };
        for (int i = 0; i < dates.length; ++i) {
            for (int j = 0; j < dates.length; ++j) {
                Assertions.assertEquals(i  < j, dates[i].compareTo(dates[j])  < 0);
                Assertions.assertEquals(i  > j, dates[j].compareTo(dates[i])  < 0);
                Assertions.assertEquals(i == j, dates[i].compareTo(dates[j]) == 0);
                Assertions.assertEquals(i  > j, dates[i].compareTo(dates[j])  > 0);
                Assertions.assertEquals(i  < j, dates[j].compareTo(dates[i])  > 0);
            }
        }
        Assertions.assertFalse(dates[0].equals(this));
        Assertions.assertFalse(dates[0].equals(dates[0].getDate()));
        Assertions.assertFalse(dates[0].equals(dates[0].getTime()));
    }

    @Test
    public void testOffset() {
        DateTimeComponents reference = new DateTimeComponents(2005, 12, 31, 23, 59, 59);
        DateTimeComponents expected  = new DateTimeComponents(2006,  1,  1,  0,  0,  0);
        Assertions.assertEquals(expected, new DateTimeComponents(reference, 1));
    }

    @Test
    public void testSymmetry() {
        DateTimeComponents reference1 = new DateTimeComponents(2005, 12, 31, 12, 0, 0);
        DateTimeComponents reference2 = new DateTimeComponents(2006,  1,  1,  1, 2, 3);
        for (double dt = -100000; dt < 100000; dt += 100) {
            Assertions.assertEquals(dt, new DateTimeComponents(reference1, dt).offsetFrom(reference1), 1.0e-15);
            Assertions.assertEquals(dt, new DateTimeComponents(reference2, dt).offsetFrom(reference2), 1.0e-15);
        }
    }

    @Test
    public void testString() {
        final DateTimeComponents date =
            new DateTimeComponents(DateComponents.J2000_EPOCH, TimeComponents.H12);
        Assertions.assertEquals("2000-01-01T12:00:00.000+00:00", date.toString());
    }

    @Test
    public void testMonth() {
        Assertions.assertEquals(new DateTimeComponents(2011, 2, 23),
                            new DateTimeComponents(2011, Month.FEBRUARY, 23));
        Assertions.assertEquals(new DateTimeComponents(2011, 2, 23, 1, 2, 3.4),
                            new DateTimeComponents(2011, Month.FEBRUARY, 23, 1, 2, 3.4));
    }

    @Test
    public void testParse() {
        String s = "2000-01-02T03:04:05.000";
        Assertions.assertEquals(s, DateTimeComponents.parseDateTime(s).toStringWithoutUtcOffset());
        s = "2000-01-02T03:04:05.000+00:00";
        Assertions.assertEquals(s, DateTimeComponents.parseDateTime(s).toString());
    }

    @Test
    public void testBadDay() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            DateTimeComponents.parseDateTime("2000-02-30T03:04:05.000+00:00");
        });
    }

    @Test
    public void testLocalTime() {
        final DateTimeComponents dtc = DateTimeComponents.parseDateTime("2000-02-29T03:04:05.000+00:01");
        Assertions.assertEquals(1, dtc.getTime().getMinutesFromUTC());
    }

    /**
     * This test is for offsets from UTC. The corresponding test in AbsoluteDateTest
     * handles UTC.
     */
    @Test
    public void testToStringRfc3339() {
        // setup
        int m = 779; // 12 hours, 59 minutes
        final double sixtyOne = FastMath.nextDown(61.0);

        // action + verify
        check(2009, 1, 1, 12, 0, 0, m, "2009-01-01T12:00:00+12:59");
        check(2009, 1, 1, 12, 0, 0, -m, "2009-01-01T12:00:00-12:59");
        check(2009, 1, 1, 12, 0, 0, 1, "2009-01-01T12:00:00+00:01");
        check(2009, 1, 1, 12, 0, 0, -1, "2009-01-01T12:00:00-00:01");
        // 00:00:00 local time
        check(2009, 1, 1, 0, 0, 0, 59, "2009-01-01T00:00:00+00:59");
        check(2009, 1, 1, 0, 0, 0, -59, "2009-01-01T00:00:00-00:59");
        check(2009, 1, 1, 0, 0, 0, 0, "2009-01-01T00:00:00Z");
        // 00:00:00 UTC, but in a time zone
        check(2009, 1, 2, 1, 0, 0, 60, "2009-01-02T01:00:00+01:00");
        check(2009, 1, 1, 23, 0, 0, -60, "2009-01-01T23:00:00-01:00");
        // leap seconds
        check(2009, 12, 31, 23, 59, sixtyOne, m, "2009-12-31T23:59:60.99999999999999+12:59");
        check(2009, 12, 31, 23, 59, sixtyOne, -m, "2009-12-31T23:59:60.99999999999999-12:59");
        check(9999, 2, 3, 4, 5, 60.5, 60, "9999-02-03T04:05:60.5+01:00");
        check(9999, 2, 3, 4, 5, 60.5, -60, "9999-02-03T04:05:60.5-01:00");
        // time zone offsets larger than 99:59?
        // TimeComponents should limit time zone offset to valid values.
        // toString will just format the values given
        check(2009, 1, 1, 12, 0, 0, 100*60, "2009-01-01T12:00:00+100:00");
        check(2009, 1, 1, 12, 0, 0, -100*60, "2009-01-01T12:00:00-100:00");
        // same for negative years
        check(-1, 1, 1, 12, 0, 0, m, "-001-01-01T12:00:00+12:59");
        check(-1, 1, 1, 12, 0, 0, -m, "-001-01-01T12:00:00-12:59");
        check(-1000, 1, 1, 12, 0, 0, m, "-1000-01-01T12:00:00+12:59");
        check(-1000, 1, 1, 12, 0, 0, -m, "-1000-01-01T12:00:00-12:59");
    }

    private static void check(
            int year, int month, int day, int hour, int minute, double second,
            int minutesFromUtc,
            String expected) {
        DateTimeComponents actual = new DateTimeComponents(
                new DateComponents(year, month, day),
                new TimeComponents(hour, minute, second, minutesFromUtc));

        MatcherAssert.assertThat(actual.toStringRfc3339(), CoreMatchers.is(expected));
    }

    @Test
    public void testToStringRounding() {
        // these tests were copied from AbsoluteDateTest
        check(2015, 9, 30, 7, 54, 60 - 9.094947e-13, 60,
                "2015-09-30T07:54:59.99999999999909+00:00",
                "2015-09-30T07:55:00.000+00:00",
                "2015-09-30T07:55:00+00:00");
        check(2008, 2, 29, 23, 59, 59.9994, 60,
                "2008-02-29T23:59:59.99940000000000+00:00",
                "2008-02-29T23:59:59.999+00:00",
                "2008-03-01T00:00:00+00:00");
        check(2008, 2, 29, 23, 59, 59.9996, 60,
                "2008-02-29T23:59:59.99960000000000+00:00",
                "2008-03-01T00:00:00.000+00:00",
                "2008-03-01T00:00:00+00:00");
        // check a leap second
        check(2015, 6, 30, 23, 59, 59.999999, 61,
                "2015-06-30T23:59:59.99999900000000+00:00",
                "2015-06-30T23:59:60.000+00:00",
                "2015-06-30T23:59:60+00:00");
        check(2015, 6, 30, 23, 59, 60.5, 61,
                "2015-06-30T23:59:60.50000000000000+00:00",
                "2015-06-30T23:59:60.500+00:00",
                "2015-07-01T00:00:00+00:00");
        // check a bigger leap second. First leap was 1.422818 s.
        // TODO can't run this test because of #707
        //check(1960, 12, 31, 23, 59, 61.42281, 62,
        //        "1960-12-31T23:59:61.42281000000000+00:00",
        //        "1960-12-31T23:59:61.42300000000000+00:00", // TODO this date is invalid
        //        "1961-01-01T00:00:00+00:00");
    }

    private void check(int year, int month, int day, int hour, int minute, double second,
                       int minuteDuration, String full, String medium, String shor) {
        DateTimeComponents dtc =
                new DateTimeComponents(year, month, day, hour, minute, second);
        MatcherAssert.assertThat(dtc.toString(minuteDuration), CoreMatchers.is(medium));
        MatcherAssert.assertThat(dtc.toString(minuteDuration, 3), CoreMatchers.is(medium));
        MatcherAssert.assertThat(dtc.toString(minuteDuration, 0), CoreMatchers.is(shor));
        MatcherAssert.assertThat(dtc.toString(minuteDuration, 14), CoreMatchers.is(full));
    }

    @Test
    public void testToStringRoundingUtcOffset() {
        DateTimeComponents dtc =
                new DateTimeComponents(new DateComponents(2000, 12, 31), new TimeComponents(23, 59, 59.9, -92));
        MatcherAssert.assertThat(dtc.toString(60), CoreMatchers.is("2000-12-31T23:59:59.900-01:32"));
        MatcherAssert.assertThat(dtc.toString(60, 3), CoreMatchers.is("2000-12-31T23:59:59.900-01:32"));
        MatcherAssert.assertThat(dtc.toString(60, 0), CoreMatchers.is("2001-01-01T00:00:00-01:32"));
        MatcherAssert.assertThat(dtc.toString(60, 14), CoreMatchers.is("2000-12-31T23:59:59.90000000000000-01:32"));
    }

    @Test
    public void testToStringWithoutUtcOffsetRoundingUtcOffset() {
        DateTimeComponents dtc =
                new DateTimeComponents(new DateComponents(2000, 12, 31), new TimeComponents(23, 59, 59.9, -92));
        MatcherAssert.assertThat(dtc.toStringWithoutUtcOffset(60, 3), CoreMatchers.is("2000-12-31T23:59:59.900"));
        MatcherAssert.assertThat(dtc.toStringWithoutUtcOffset(60, 0), CoreMatchers.is("2001-01-01T00:00:00"));
        MatcherAssert.assertThat(dtc.toStringWithoutUtcOffset(60, 14), CoreMatchers.is("2000-12-31T23:59:59.90000000000000"));
    }

}
