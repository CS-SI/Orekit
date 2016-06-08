/* Copyright 2002-2016 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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


import org.junit.Assert;
import org.junit.Test;


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

}
