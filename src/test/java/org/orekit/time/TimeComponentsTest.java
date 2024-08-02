/* Copyright 2002-2024 CS GROUP
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
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Test;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


class TimeComponentsTest {

    @Test
    void testOutOfRangeA() throws IllegalArgumentException {
        assertThrows(IllegalArgumentException.class, () -> {
            new TimeComponents(-1, 10, 10);
        });
    }

    @Test
    void testOutOfRangeB() throws IllegalArgumentException {
        assertThrows(IllegalArgumentException.class, () -> {
            new TimeComponents(24, 10, 10);
        });
    }

    @Test
    void testOutOfRangeC() throws IllegalArgumentException {
        assertThrows(IllegalArgumentException.class, () -> {
            new TimeComponents(10, -1, 10);
        });
    }

    @Test
    void testOutOfRangeD() throws IllegalArgumentException {
        assertThrows(IllegalArgumentException.class, () -> {
            new TimeComponents(10, 60, 10);
        });
    }

    @Test
    void testOutOfRangeE() throws IllegalArgumentException {
        assertThrows(IllegalArgumentException.class, () -> {
            new TimeComponents(10, 10, -1);
        });
    }

    @Test
    void testOutOfRangeF() throws IllegalArgumentException {
        assertThrows(IllegalArgumentException.class, () -> {
            new TimeComponents(10, 10, 61);
        });
    }

    @Test
    void testOutOfRangeG() throws IllegalArgumentException {
        assertThrows(IllegalArgumentException.class, () -> {
            new TimeComponents(86399, 4.5);
        });
    }

    @Test
    void testOutOfRangeH() throws IllegalArgumentException {
        assertThrows(IllegalArgumentException.class, () -> {
            new TimeComponents(0, -1.0);
        });
    }

    @Test
    void testInRange() {

        TimeComponents time = new TimeComponents(10, 10, 10);
        assertEquals(10,   time.getHour());
        assertEquals(10,   time.getMinute());
        assertEquals(10.0, time.getSecond(), 1.0e-10);

        time = new TimeComponents(0.0);
        assertEquals(0.0, time.getSecondsInUTCDay(), 1.0e-10);

        time = new TimeComponents(10, 10, 60.999);
        assertEquals(10,   time.getHour());
        assertEquals(10,   time.getMinute());
        assertEquals(60.999, time.getSecond(), 1.0e-10);

        time = new TimeComponents(43200.0);
        assertEquals(43200.0, time.getSecondsInUTCDay(), 1.0e-10);

        time = new TimeComponents(86399.999);
        assertEquals(86399.999, time.getSecondsInUTCDay(), 1.0e-10);

        time = new TimeComponents(2, 30, 0, 180);
        assertEquals(+9000.0, time.getSecondsInLocalDay(), 1.0e-5);
        assertEquals(-1800.0, time.getSecondsInUTCDay(),   1.0e-5);
    }

    @Test
    void testValues() {
        assertEquals(    0.0, new TimeComponents( 0, 0, 0).getSecondsInLocalDay(), 1.0e-10);
        assertEquals(21600.0, new TimeComponents( 6, 0, 0).getSecondsInLocalDay(), 1.0e-10);
        assertEquals(43200.0, new TimeComponents(12, 0, 0).getSecondsInLocalDay(), 1.0e-10);
        assertEquals(64800.0, new TimeComponents(18, 0, 0).getSecondsInLocalDay(), 1.0e-10);
        assertEquals(86399.9, new TimeComponents(23, 59, 59.9).getSecondsInLocalDay(), 1.0e-10);
    }

    @Test
    void testString() {
        assertEquals("00:00:00.000+00:00", new TimeComponents(0).toString());
        assertEquals("06:00:00.000+00:00", new TimeComponents(21600).toString());
        assertEquals("12:00:00.000+00:00", new TimeComponents(43200).toString());
        assertEquals("18:00:00.000+00:00", new TimeComponents(64800).toString());
        assertEquals("23:59:59.89999999999418+00:00", new TimeComponents(86399.9).toString());
        assertEquals("00:00:00.000+10:00", new TimeComponents( 0,  0,  0,    600).toString());
        assertEquals("06:00:00.000+10:00", new TimeComponents( 6,  0,  0,    600).toString());
        assertEquals("12:00:00.000-04:30", new TimeComponents(12,  0,  0,   -270).toString());
        assertEquals("18:00:00.000-04:30", new TimeComponents(18,  0,  0,   -270).toString());
        assertEquals("23:59:59.900-04:30", new TimeComponents(23, 59, 59.9, -270).toString());
        // test leap seconds
        assertEquals("23:59:60.500+00:00", TimeComponents.fromSeconds(86399, 0.5, 1, 61).toString());
        // leap second on 1961 is between 1 and 2 seconds in duration
        assertEquals("23:59:61.32281798015773+00:00", TimeComponents.fromSeconds(86399, 0.32281798015773, 2, 62).toString());
        // test rounding
        assertEquals("23:59:59.99999999998545+00:00", new TimeComponents(86399.99999999999).toString());
        assertEquals("23:59:59.99999999999999+00:00", TimeComponents.fromSeconds(86399, FastMath.nextDown(1.0), 0, 60).toString());
    }

    @Test
    void testParse() {
        assertEquals(86399.9, TimeComponents.parseTime("235959.900").getSecondsInLocalDay(), 1.0e-10);
        assertEquals(86399.9, TimeComponents.parseTime("23:59:59.900").getSecondsInLocalDay(), 1.0e-10);
        assertEquals(86399.9, TimeComponents.parseTime("23:59:59,900").getSecondsInLocalDay(), 1.0e-10);
        assertEquals(86399.9, TimeComponents.parseTime("235959.900Z").getSecondsInLocalDay(), 1.0e-10);
        assertEquals(86399.9, TimeComponents.parseTime("23:59:59.900Z").getSecondsInLocalDay(), 1.0e-10);
        assertEquals(86399.9, TimeComponents.parseTime("235959.900+10").getSecondsInLocalDay(), 1.0e-10);
        assertEquals(86399.9, TimeComponents.parseTime("23:59:59.900+00").getSecondsInLocalDay(), 1.0e-10);
        assertEquals(86399.9, TimeComponents.parseTime("235959.900-00:12").getSecondsInLocalDay(), 1.0e-10);
        assertEquals(86399.9, TimeComponents.parseTime("23:59:59.900+00:00").getSecondsInLocalDay(), 1.0e-10);
        assertEquals(86340.0, TimeComponents.parseTime("23:59").getSecondsInLocalDay(), 1.0e-10);
    }

    @Test
    void testBadFormat() {
        assertThrows(IllegalArgumentException.class, () -> {
            TimeComponents.parseTime("23h59m59s");
        });
    }

    @Test
    void testLocalTime() {
        assertEquals(60, TimeComponents.parseTime("23:59:59+01:00").getMinutesFromUTC());
    }

    @SuppressWarnings("unlikely-arg-type")
    @Test
    void testComparisons() {
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
                    assertTrue(times[j].compareTo(times[i]) > 0);
                    assertNotEquals(times[i], times[j]);
                    assertNotEquals(times[j], times[i]);
                    assertTrue(times[i].hashCode() != times[j].hashCode());
                    assertTrue(i < j);
                } else if (times[i].compareTo(times[j]) > 0) {
                    assertTrue(times[j].compareTo(times[i]) < 0);
                    assertNotEquals(times[i], times[j]);
                    assertNotEquals(times[j], times[i]);
                    assertTrue(times[i].hashCode() != times[j].hashCode());
                    assertTrue(i > j);
                } else {
                    assertEquals(0, times[j].compareTo(times[i]));
                    assertEquals(times[i], times[j]);
                    assertEquals(times[j], times[i]);
                    assertEquals(times[i].hashCode(), times[j].hashCode());
                    assertEquals(i, j);
                }
            }
        }
        assertNotEquals(times[0], this);
    }

    @Test
    void testFromSeconds() {
        // setup
        double zeroUlp = FastMath.nextUp(0.0);
        double one = FastMath.nextDown(1.0);
        double sixty = FastMath.nextDown(60.0);
        double sixtyOne = FastMath.nextDown(61.0);

        // action + verify
        assertThat(TimeComponents.fromSeconds(0, 0, 0, 60).getSecond(),
                CoreMatchers.is(0.0));
        assertThat(TimeComponents.fromSeconds(0, zeroUlp, 0, 60).getSecond(),
                CoreMatchers.is(zeroUlp));
        assertThat(TimeComponents.fromSeconds(86399, one, 0, 60).getSecond(),
                CoreMatchers.is(sixty));
        assertThat(TimeComponents.fromSeconds(86399, one, 1, 61).getSecond(),
                CoreMatchers.is(sixtyOne));
        // I don't like this NaN behavior, but it matches the 10.1 implementation and
        // GLONASSAnalyticalPropagatorTest relied on it.
        // It seems more logical to throw an out of range exception in this case.
        assertThat(TimeComponents.fromSeconds(86399, Double.NaN, 0, 60).getSecond(),
                CoreMatchers.is(Double.NaN));
        assertThat(TimeComponents.fromSeconds(86399, Double.NaN, 0, 60).getMinute(),
                CoreMatchers.is(59));
        assertThat(TimeComponents.fromSeconds(86399, Double.NaN, 1, 61).getSecond(),
                CoreMatchers.is(Double.NaN));
        assertThat(TimeComponents.fromSeconds(86399, Double.NaN, 1, 61).getMinute(),
                CoreMatchers.is(59));

        // check errors
        try {
            TimeComponents.fromSeconds(0, FastMath.nextDown(0), 0, 60);
            fail("Expected Exception");
        } catch (OrekitIllegalArgumentException e) {
            assertThat(e.getSpecifier(),
                    CoreMatchers.is(OrekitMessages.OUT_OF_RANGE_SECONDS_NUMBER_DETAIL));
        }
        try {
            TimeComponents.fromSeconds(86399, 1, 0, 60);
            fail("Expected Exception");
        } catch (OrekitIllegalArgumentException e) {
            assertThat(e.getSpecifier(),
                    CoreMatchers.is(OrekitMessages.OUT_OF_RANGE_SECONDS_NUMBER_DETAIL));
        }
        try {
            TimeComponents.fromSeconds(86399, 1, 1, 61);
            fail("Expected Exception");
        } catch (OrekitIllegalArgumentException e) {
            assertThat(e.getSpecifier(),
                    CoreMatchers.is(OrekitMessages.OUT_OF_RANGE_SECONDS_NUMBER_DETAIL));
        }
        try {
            TimeComponents.fromSeconds(0, 0, -1, 59);
            fail("Expected Exception");
        } catch (OrekitIllegalArgumentException e) {
            assertThat(e.getSpecifier(),
                    CoreMatchers.is(OrekitMessages.OUT_OF_RANGE_SECONDS_NUMBER_DETAIL));
        }
        try {
            TimeComponents.fromSeconds(0, 0, 1, 59);
            fail("Expected Exception");
        } catch (OrekitIllegalArgumentException e) {
            assertThat(e.getSpecifier(),
                    CoreMatchers.is(OrekitMessages.OUT_OF_RANGE_SECONDS_NUMBER_DETAIL));
        }
    }

    @Test
    void testTimeComponentsDouble658() {
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
            fail("Expected Exception");
        } catch (OrekitIllegalArgumentException e) {
            assertThat(e.getSpecifier(),
                    CoreMatchers.is(OrekitMessages.OUT_OF_RANGE_SECONDS_NUMBER_DETAIL));
            assertThat(e.getParts()[0], CoreMatchers.is(86400.0));
        }
        try {
            new TimeComponents(-zeroUlp);
            fail("Expected Exception");
        } catch (OrekitIllegalArgumentException e) {
            assertThat(e.getSpecifier(),
                    CoreMatchers.is(OrekitMessages.OUT_OF_RANGE_SECONDS_NUMBER_DETAIL));
            assertThat(e.getParts()[0], CoreMatchers.is(-zeroUlp));
        }
    }

    @Test
    void testTimeComponentsIntDouble658() {
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
            fail("Expected Exception");
        } catch (OrekitIllegalArgumentException e) {
            assertThat(e.getSpecifier(),
                    CoreMatchers.is(OrekitMessages.OUT_OF_RANGE_SECONDS_NUMBER_DETAIL));
            assertThat(e.getParts()[0], CoreMatchers.is(86400.0));
        }
        try {
            new TimeComponents(86400, 1.0);
            fail("Expected Exception");
        } catch (OrekitIllegalArgumentException e) {
            assertThat(e.getSpecifier(),
                    CoreMatchers.is(OrekitMessages.OUT_OF_RANGE_SECONDS_NUMBER_DETAIL));
            assertThat(e.getParts()[0], CoreMatchers.is(86400.0));
        }
        try {
            new TimeComponents(0, -zeroUlp);
            fail("Expected Exception");
        } catch (OrekitIllegalArgumentException e) {
            assertThat(e.getSpecifier(),
                    CoreMatchers.is(OrekitMessages.OUT_OF_RANGE_SECONDS_NUMBER_DETAIL));
            assertThat(e.getParts()[0], CoreMatchers.is(-zeroUlp));
        }
        try {
            new TimeComponents(-1, 0.0);
            fail("Expected Exception");
        } catch (OrekitIllegalArgumentException e) {
            assertThat(e.getSpecifier(),
                    CoreMatchers.is(OrekitMessages.OUT_OF_RANGE_SECONDS_NUMBER_DETAIL));
            assertThat(e.getParts()[0], CoreMatchers.is(-1.0));
        }
    }

    private void check(final TimeComponents tc, int hour, int minute, double second) {
        assertThat(tc.getHour(), CoreMatchers.is(hour));
        assertThat(tc.getMinute(), CoreMatchers.is(minute));
        assertThat(tc.getSecond(), CoreMatchers.is(second));
    }

}
