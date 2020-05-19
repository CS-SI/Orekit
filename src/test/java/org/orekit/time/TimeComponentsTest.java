/* Copyright 2002-2020 CS GROUP
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
import org.junit.Assert;
import org.junit.Test;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;


public class TimeComponentsTest {

    @Test(expected=IllegalArgumentException.class)
    public void testOutOfRangeA() throws IllegalArgumentException {
        new TimeComponents(-1, 10, 10);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testOutOfRangeB() throws IllegalArgumentException {
        new TimeComponents(24, 10, 10);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testOutOfRangeC() throws IllegalArgumentException {
        new TimeComponents(10, -1, 10);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testOutOfRangeD() throws IllegalArgumentException {
        new TimeComponents(10, 60, 10);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testOutOfRangeE() throws IllegalArgumentException {
        new TimeComponents(10, 10, -1);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testOutOfRangeF() throws IllegalArgumentException {
        new TimeComponents(10, 10, 61);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testOutOfRangeG() throws IllegalArgumentException {
        new TimeComponents(86399, 4.5);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testOutOfRangeH() throws IllegalArgumentException {
        new TimeComponents(0, -1.0);
    }

    @Test
    public void testInRange() {

        TimeComponents time = new TimeComponents(10, 10, 10);
        Assert.assertEquals(10,   time.getHour());
        Assert.assertEquals(10,   time.getMinute());
        Assert.assertEquals(10.0, time.getSecond(), 1.0e-10);

        time = new TimeComponents(0.0);
        Assert.assertEquals(0.0, time.getSecondsInUTCDay(), 1.0e-10);

        time = new TimeComponents(10, 10, 60.999);
        Assert.assertEquals(10,   time.getHour());
        Assert.assertEquals(10,   time.getMinute());
        Assert.assertEquals(60.999, time.getSecond(), 1.0e-10);

        time = new TimeComponents(43200.0);
        Assert.assertEquals(43200.0, time.getSecondsInUTCDay(), 1.0e-10);

        time = new TimeComponents(86399.999);
        Assert.assertEquals(86399.999, time.getSecondsInUTCDay(), 1.0e-10);

        time = new TimeComponents(2, 30, 0, 180);
        Assert.assertEquals(+9000.0, time.getSecondsInLocalDay(), 1.0e-5);
        Assert.assertEquals(-1800.0, time.getSecondsInUTCDay(),   1.0e-5);
    }

    @Test
    public void testValues() {
        Assert.assertEquals(    0.0, new TimeComponents( 0, 0, 0).getSecondsInLocalDay(), 1.0e-10);
        Assert.assertEquals(21600.0, new TimeComponents( 6, 0, 0).getSecondsInLocalDay(), 1.0e-10);
        Assert.assertEquals(43200.0, new TimeComponents(12, 0, 0).getSecondsInLocalDay(), 1.0e-10);
        Assert.assertEquals(64800.0, new TimeComponents(18, 0, 0).getSecondsInLocalDay(), 1.0e-10);
        Assert.assertEquals(86399.9, new TimeComponents(23, 59, 59.9).getSecondsInLocalDay(), 1.0e-10);
    }

    @Test
    public void testString() {
        Assert.assertEquals("00:00:00.000", new TimeComponents(0).toString());
        Assert.assertEquals("06:00:00.000", new TimeComponents(21600).toString());
        Assert.assertEquals("12:00:00.000", new TimeComponents(43200).toString());
        Assert.assertEquals("18:00:00.000", new TimeComponents(64800).toString());
        Assert.assertEquals("23:59:59.900", new TimeComponents(86399.9).toString());
        Assert.assertEquals("00:00:00.000+10:00", new TimeComponents( 0,  0,  0,    600).toString());
        Assert.assertEquals("06:00:00.000+10:00", new TimeComponents( 6,  0,  0,    600).toString());
        Assert.assertEquals("12:00:00.000-04:30", new TimeComponents(12,  0,  0,   -270).toString());
        Assert.assertEquals("18:00:00.000-04:30", new TimeComponents(18,  0,  0,   -270).toString());
        Assert.assertEquals("23:59:59.900-04:30", new TimeComponents(23, 59, 59.9, -270).toString());
    }

    @Test
    public void testParse() {
        Assert.assertEquals(86399.9, TimeComponents.parseTime("235959.900").getSecondsInLocalDay(), 1.0e-10);
        Assert.assertEquals(86399.9, TimeComponents.parseTime("23:59:59.900").getSecondsInLocalDay(), 1.0e-10);
        Assert.assertEquals(86399.9, TimeComponents.parseTime("23:59:59,900").getSecondsInLocalDay(), 1.0e-10);
        Assert.assertEquals(86399.9, TimeComponents.parseTime("235959.900Z").getSecondsInLocalDay(), 1.0e-10);
        Assert.assertEquals(86399.9, TimeComponents.parseTime("23:59:59.900Z").getSecondsInLocalDay(), 1.0e-10);
        Assert.assertEquals(86399.9, TimeComponents.parseTime("235959.900+10").getSecondsInLocalDay(), 1.0e-10);
        Assert.assertEquals(86399.9, TimeComponents.parseTime("23:59:59.900+00").getSecondsInLocalDay(), 1.0e-10);
        Assert.assertEquals(86399.9, TimeComponents.parseTime("235959.900-00:12").getSecondsInLocalDay(), 1.0e-10);
        Assert.assertEquals(86399.9, TimeComponents.parseTime("23:59:59.900+00:00").getSecondsInLocalDay(), 1.0e-10);
        Assert.assertEquals(86340.0, TimeComponents.parseTime("23:59").getSecondsInLocalDay(), 1.0e-10);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testBadFormat() {
        TimeComponents.parseTime("23h59m59s");
    }

    @Test
    public void testLocalTime() {
        Assert.assertEquals(60, TimeComponents.parseTime("23:59:59+01:00").getMinutesFromUTC());
    }

    @SuppressWarnings("unlikely-arg-type")
    @Test
    public void testComparisons() {
        TimeComponents[] times = {
                 new TimeComponents( 0,  0,  0.0),
                 new TimeComponents( 0,  0,  1.0e-15),
                 new TimeComponents( 0, 12,  3.0),
                 new TimeComponents(15,  9,  3.0),
                 new TimeComponents(23, 59, 59.0),
                 new TimeComponents(23, 59, 60.0 - 1.0e-12)
        };
        for (int i = 0; i < times.length; ++i) {
            for (int j = 0; j < times.length; ++j) {
                if (times[i].compareTo(times[j]) < 0) {
                    Assert.assertTrue(times[j].compareTo(times[i]) > 0);
                    Assert.assertFalse(times[i].equals(times[j]));
                    Assert.assertFalse(times[j].equals(times[i]));
                    Assert.assertTrue(times[i].hashCode() != times[j].hashCode());
                    Assert.assertTrue(i < j);
                } else if (times[i].compareTo(times[j]) > 0) {
                    Assert.assertTrue(times[j].compareTo(times[i]) < 0);
                    Assert.assertFalse(times[i].equals(times[j]));
                    Assert.assertFalse(times[j].equals(times[i]));
                    Assert.assertTrue(times[i].hashCode() != times[j].hashCode());
                    Assert.assertTrue(i > j);
                } else {
                    Assert.assertTrue(times[j].compareTo(times[i]) == 0);
                    Assert.assertTrue(times[i].equals(times[j]));
                    Assert.assertTrue(times[j].equals(times[i]));
                    Assert.assertTrue(times[i].hashCode() == times[j].hashCode());
                    Assert.assertTrue(i == j);
                }
            }
        }
        Assert.assertFalse(times[0].equals(this));
    }

    @Test
    public void testFromSeconds() {
        // setup
        double zeroUlp = FastMath.nextUp(0.0);
        double one = FastMath.nextDown(1.0);
        double sixty = FastMath.nextDown(60.0);
        double sixtyOne = FastMath.nextDown(61.0);

        // action + verify
        MatcherAssert.assertThat(TimeComponents.fromSeconds(0, 0, 0, 60).getSecond(),
                CoreMatchers.is(0.0));
        MatcherAssert.assertThat(TimeComponents.fromSeconds(0, zeroUlp, 0, 60).getSecond(),
                CoreMatchers.is(zeroUlp));
        MatcherAssert.assertThat(TimeComponents.fromSeconds(86399, one, 0, 60).getSecond(),
                CoreMatchers.is(sixty));
        MatcherAssert.assertThat(TimeComponents.fromSeconds(86399, one, 1, 61).getSecond(),
                CoreMatchers.is(sixtyOne));
        // I don't like this NaN behavior, but it matches the 10.1 implementation and
        // GLONASSAnalyticalPropagatorTest relied on it.
        // It seems more logical to throw an out of range exception in this case.
        MatcherAssert.assertThat(TimeComponents.fromSeconds(86399, Double.NaN, 0, 60).getSecond(),
                CoreMatchers.is(Double.NaN));
        MatcherAssert.assertThat(TimeComponents.fromSeconds(86399, Double.NaN, 0, 60).getMinute(),
                CoreMatchers.is(59));
        MatcherAssert.assertThat(TimeComponents.fromSeconds(86399, Double.NaN, 1, 61).getSecond(),
                CoreMatchers.is(Double.NaN));
        MatcherAssert.assertThat(TimeComponents.fromSeconds(86399, Double.NaN, 1, 61).getMinute(),
                CoreMatchers.is(59));

        // check errors
        try {
            TimeComponents.fromSeconds(0, FastMath.nextDown(0), 0, 60);
            Assert.fail("Expected Exception");
        } catch (OrekitIllegalArgumentException e) {
            MatcherAssert.assertThat(e.getSpecifier(),
                    CoreMatchers.is(OrekitMessages.OUT_OF_RANGE_SECONDS_NUMBER_DETAIL));
        }
        try {
            TimeComponents.fromSeconds(86399, 1, 0, 60);
            Assert.fail("Expected Exception");
        } catch (OrekitIllegalArgumentException e) {
            MatcherAssert.assertThat(e.getSpecifier(),
                    CoreMatchers.is(OrekitMessages.OUT_OF_RANGE_SECONDS_NUMBER_DETAIL));
        }
        try {
            TimeComponents.fromSeconds(86399, 1, 1, 61);
            Assert.fail("Expected Exception");
        } catch (OrekitIllegalArgumentException e) {
            MatcherAssert.assertThat(e.getSpecifier(),
                    CoreMatchers.is(OrekitMessages.OUT_OF_RANGE_SECONDS_NUMBER_DETAIL));
        }
        try {
            TimeComponents.fromSeconds(0, 0, -1, 59);
            Assert.fail("Expected Exception");
        } catch (OrekitIllegalArgumentException e) {
            MatcherAssert.assertThat(e.getSpecifier(),
                    CoreMatchers.is(OrekitMessages.OUT_OF_RANGE_SECONDS_NUMBER_DETAIL));
        }
        try {
            TimeComponents.fromSeconds(0, 0, 1, 59);
            Assert.fail("Expected Exception");
        } catch (OrekitIllegalArgumentException e) {
            MatcherAssert.assertThat(e.getSpecifier(),
                    CoreMatchers.is(OrekitMessages.OUT_OF_RANGE_SECONDS_NUMBER_DETAIL));
        }
    }

    @Test
    public void testTimeComponentsDouble658() {
        // setup
        double zeroUlp = FastMath.nextUp(0.0);
        double dayUlp = FastMath.ulp(86400.0);

        // action + verify
        check(new TimeComponents(0.0), 0, 0, 0);
        check(new TimeComponents(zeroUlp), 0, 0, zeroUlp);
        check(new TimeComponents(86399.5), 23, 59, 59.5);
        check(new TimeComponents(FastMath.nextDown(86400.0)), 23, 59, 60 - dayUlp);
        check(new TimeComponents(86400), 23, 59, 60);
        check(new TimeComponents(FastMath.nextUp(86400.0)), 23, 59, 60 + dayUlp);
        check(new TimeComponents(86400.5), 23, 59, 60.5);
        check(new TimeComponents(FastMath.nextDown(86401.0)), 23, 59, 61 - dayUlp);
        try {
            new TimeComponents(86401);
            Assert.fail("Expected Exception");
        } catch (OrekitIllegalArgumentException e) {
            MatcherAssert.assertThat(e.getSpecifier(),
                    CoreMatchers.is(OrekitMessages.OUT_OF_RANGE_SECONDS_NUMBER_DETAIL));
            MatcherAssert.assertThat(e.getParts()[0], CoreMatchers.is(86400.0));
        }
        try {
            new TimeComponents(-zeroUlp);
            Assert.fail("Expected Exception");
        } catch (OrekitIllegalArgumentException e) {
            MatcherAssert.assertThat(e.getSpecifier(),
                    CoreMatchers.is(OrekitMessages.OUT_OF_RANGE_SECONDS_NUMBER_DETAIL));
            MatcherAssert.assertThat(e.getParts()[0], CoreMatchers.is(-zeroUlp));
        }
    }

    @Test
    public void testTimeComponentsIntDouble658() {
        // setup
        double zeroUlp = FastMath.nextUp(0.0);
        double sixtyUlp = FastMath.ulp(60.0);
        double one = FastMath.nextDown(1.0);
        double sixty = FastMath.nextDown(60.0);
        double sixtyOne = FastMath.nextDown(61.0);

        // action + verify
        check(new TimeComponents(0, 0.0), 0, 0, 0);
        check(new TimeComponents(0, zeroUlp), 0, 0, zeroUlp);
        check(new TimeComponents(86399, 0.5), 23, 59, 59.5);
        check(new TimeComponents(86399, one), 23, 59, sixty);
        check(new TimeComponents(86400, 0.0), 23, 59, 60);
        check(new TimeComponents(86400, sixtyUlp), 23, 59, 60 + sixtyUlp);
        check(new TimeComponents(86400, 0.5), 23, 59, 60.5);
        check(new TimeComponents(86400, one), 23, 59, sixtyOne);
        try {
            new TimeComponents(86401, 0.0);
            Assert.fail("Expected Exception");
        } catch (OrekitIllegalArgumentException e) {
            MatcherAssert.assertThat(e.getSpecifier(),
                    CoreMatchers.is(OrekitMessages.OUT_OF_RANGE_SECONDS_NUMBER_DETAIL));
            MatcherAssert.assertThat(e.getParts()[0], CoreMatchers.is(86400.0));
        }
        try {
            new TimeComponents(86400, 1.0);
            Assert.fail("Expected Exception");
        } catch (OrekitIllegalArgumentException e) {
            MatcherAssert.assertThat(e.getSpecifier(),
                    CoreMatchers.is(OrekitMessages.OUT_OF_RANGE_SECONDS_NUMBER_DETAIL));
            MatcherAssert.assertThat(e.getParts()[0], CoreMatchers.is(86400.0));
        }
        try {
            new TimeComponents(0, -zeroUlp);
            Assert.fail("Expected Exception");
        } catch (OrekitIllegalArgumentException e) {
            MatcherAssert.assertThat(e.getSpecifier(),
                    CoreMatchers.is(OrekitMessages.OUT_OF_RANGE_SECONDS_NUMBER_DETAIL));
            MatcherAssert.assertThat(e.getParts()[0], CoreMatchers.is(-zeroUlp));
        }
        try {
            new TimeComponents(-1, 0.0);
            Assert.fail("Expected Exception");
        } catch (OrekitIllegalArgumentException e) {
            MatcherAssert.assertThat(e.getSpecifier(),
                    CoreMatchers.is(OrekitMessages.OUT_OF_RANGE_SECONDS_NUMBER_DETAIL));
            MatcherAssert.assertThat(e.getParts()[0], CoreMatchers.is(-1.0));
        }
    }

    private void check(final TimeComponents tc, int hour, int minute, double second) {
        MatcherAssert.assertThat(tc.getHour(), CoreMatchers.is(hour));
        MatcherAssert.assertThat(tc.getMinute(), CoreMatchers.is(minute));
        MatcherAssert.assertThat(tc.getSecond(), CoreMatchers.is(second));
    }

}
