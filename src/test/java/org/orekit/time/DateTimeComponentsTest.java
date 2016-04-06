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


public class DateTimeComponentsTest {

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
                Assert.assertEquals(i  < j, dates[i].compareTo(dates[j])  < 0);
                Assert.assertEquals(i  > j, dates[j].compareTo(dates[i])  < 0);
                Assert.assertEquals(i == j, dates[i].compareTo(dates[j]) == 0);
                Assert.assertEquals(i  > j, dates[i].compareTo(dates[j])  > 0);
                Assert.assertEquals(i  < j, dates[j].compareTo(dates[i])  > 0);
            }
        }
        Assert.assertFalse(dates[0].equals(this));
        Assert.assertFalse(dates[0].equals(dates[0].getDate()));
        Assert.assertFalse(dates[0].equals(dates[0].getTime()));
    }

    @Test
    public void testOffset() {
        DateTimeComponents reference = new DateTimeComponents(2005, 12, 31, 23, 59, 59);
        DateTimeComponents expected  = new DateTimeComponents(2006,  1,  1,  0,  0,  0);
        Assert.assertEquals(expected, new DateTimeComponents(reference, 1));
    }

    @Test
    public void testSymmetry() {
        DateTimeComponents reference1 = new DateTimeComponents(2005, 12, 31, 12, 0, 0);
        DateTimeComponents reference2 = new DateTimeComponents(2006,  1,  1,  1, 2, 3);
        for (double dt = -100000; dt < 100000; dt += 100) {
            Assert.assertEquals(dt, new DateTimeComponents(reference1, dt).offsetFrom(reference1), 1.0e-15);
            Assert.assertEquals(dt, new DateTimeComponents(reference2, dt).offsetFrom(reference2), 1.0e-15);
        }
    }

    @Test
    public void testString() {
        final DateTimeComponents date =
            new DateTimeComponents(DateComponents.J2000_EPOCH, TimeComponents.H12);
        Assert.assertEquals("2000-01-01T12:00:00.000", date.toString());
    }

    @Test
    public void testMonth() {
        Assert.assertEquals(new DateTimeComponents(2011, 2, 23),
                            new DateTimeComponents(2011, Month.FEBRUARY, 23));
        Assert.assertEquals(new DateTimeComponents(2011, 2, 23, 1, 2, 3.4),
                            new DateTimeComponents(2011, Month.FEBRUARY, 23, 1, 2, 3.4));
    }

    @Test
    public void testParse() {
        String s = "2000-01-02T03:04:05.000";
        Assert.assertEquals(s, DateTimeComponents.parseDateTime(s).toString());
    }

    @Test(expected=IllegalArgumentException.class)
    public void testBadDay() {
        DateTimeComponents.parseDateTime("2000-02-30T03:04:05.000+00:00");
    }

    @Test
    public void testLocalTime() {
        final DateTimeComponents dtc = DateTimeComponents.parseDateTime("2000-02-29T03:04:05.000+00:01");
        Assert.assertEquals(1, dtc.getTime().getMinutesFromUTC());
    }

}
