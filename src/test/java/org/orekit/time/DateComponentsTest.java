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


public class DateComponentsTest {

    @Test
    public void testReferenceDates() {

        int[][] reference = {
                { -4713, 12, 31, -2451546 }, { -4712, 01, 01, -2451545 },
                {  0000, 12, 31,  -730122 }, {  0001, 01, 01,  -730121 },
                {  1500, 02, 28,  -182554 }, {  1500, 02, 29,  -182553 },
                {  1500, 03, 01,  -182552 }, {  1582, 10, 04,  -152385 },
                {  1582, 10, 15,  -152384 }, {  1600, 02, 28,  -146039 },
                {  1600, 02, 29,  -146038 }, {  1600, 03, 01,  -146037 },
                {  1700, 02, 28,  -109514 }, {  1700, 03, 01,  -109513 },
                {  1800, 02, 28,   -72990 }, {  1800, 03, 01,   -72989 },
                {  1858, 11, 15,   -51546 }, {  1858, 11, 16,   -51545 },
                {  1999, 12, 31,       -1 }, {  2000, 01, 01,        0 },
                {  2000, 02, 28,       58 }, {  2000, 02, 29,       59 },
                {  2000, 03, 01,       60 }
        };

        for (int i = 0; i < reference.length; ++i) {
            int day  = reference[i][3];
            DateComponents date = new DateComponents(DateComponents.J2000_EPOCH, day);
            Assert.assertEquals(reference[i][0], date.getYear());
            Assert.assertEquals(reference[i][1], date.getMonth());
            Assert.assertEquals(reference[i][2], date.getDay());
        }

    }

    @Test
    public void testCalendarWeek() {
        Assert.assertEquals(52, new DateComponents(1995,  1,  1).getCalendarWeek());
        Assert.assertEquals(52, new DateComponents(1996, 12, 29).getCalendarWeek());
        Assert.assertEquals( 1, new DateComponents(1996, 12, 30).getCalendarWeek());
        Assert.assertEquals( 1, new DateComponents(1997,  1,  5).getCalendarWeek());
        Assert.assertEquals(52, new DateComponents(1997, 12, 28).getCalendarWeek());
        Assert.assertEquals( 1, new DateComponents(1997, 12, 29).getCalendarWeek());
        Assert.assertEquals( 1, new DateComponents(1998,  1,  4).getCalendarWeek());
        Assert.assertEquals( 2, new DateComponents(1998,  1,  5).getCalendarWeek());
        Assert.assertEquals(52, new DateComponents(1998, 12, 27).getCalendarWeek());
        Assert.assertEquals(53, new DateComponents(1998, 12, 28).getCalendarWeek());
        Assert.assertEquals(53, new DateComponents(1999,  1,  3).getCalendarWeek());
        Assert.assertEquals( 1, new DateComponents(1999,  1,  4).getCalendarWeek());
        DateComponents[] firstWeekMonday = new DateComponents[502];
        for (int i = 0; i < firstWeekMonday.length; ++i) {
            firstWeekMonday[i] = firstWeekMonday(1599 + i);
        }
        int startDay = firstWeekMonday[0].getJ2000Day();
        int endDay   = firstWeekMonday[firstWeekMonday.length - 1].getJ2000Day();
        int index    = 0;
        for (int day = startDay; day < endDay; ++day) {
            DateComponents d = new DateComponents(day);
            if (firstWeekMonday[index + 1].compareTo(d) <= 0) {
                ++index;
            }
            int delta = d.getJ2000Day() - firstWeekMonday[index].getJ2000Day();
            Assert.assertEquals(1 + delta / 7, d.getCalendarWeek());
        }
    }

    @Test
    public void testWeekComponents() {
        int[][] reference = {
            { 1994, 52, 7, 1995,  1,  1 },
            { 1996, 52, 7, 1996, 12, 29 },
            { 1997,  1, 1, 1996, 12, 30 },
            { 1997,  1, 7, 1997,  1,  5 },
            { 1997, 52, 7, 1997, 12, 28 },
            { 1998,  1, 1, 1997, 12, 29 },
            { 1998,  1, 7, 1998,  1,  4 },
            { 1998,  2, 1, 1998,  1,  5 },
            { 1998, 52, 7, 1998, 12, 27 },
            { 1998, 53, 1, 1998, 12, 28 },
            { 1998, 53, 7, 1999,  1,  3 },
            { 1999,  1, 1, 1999,  1,  4 },
            { 1582, 40, 4, 1582, 10,  4 },
            { 1582, 40, 5, 1582, 10, 15 },
            { 1582, 51, 5, 1582, 12, 31 },
            { 1582, 51, 6, 1583,  1,  1 },
            { 1582, 51, 7, 1583,  1,  2 },
            { 1583,  1, 1, 1583,  1,  3 }
        };

        for (int i = 0; i < reference.length; ++i) {
            int[] refI = reference[i];
            DateComponents date = DateComponents.createFromWeekComponents(refI[0], refI[1], refI[2]);
            Assert.assertEquals(refI[3], date.getYear());
            Assert.assertEquals(refI[4], date.getMonth());
            Assert.assertEquals(refI[5], date.getDay());
        }

    }

    // poor man's (slow) implementation of first calendar week computation, using ISO rules
    private DateComponents firstWeekMonday(final int year) {
        int i = 0;
        while (true) {
            DateComponents d = new DateComponents(year, 1, ++i);
            if (d.getDayOfWeek() == 4) {
                // this is the first Thursday of the year, Monday is 3 days before
                return new DateComponents(d, -3);
            }
        }
    }

    @Test
    public void testDayOfWeek() {
        Assert.assertEquals(7, new DateComponents(-4713, 12, 31).getDayOfWeek());
        Assert.assertEquals(1, new DateComponents(-4712, 01, 01).getDayOfWeek());
        Assert.assertEquals(4, new DateComponents( 1582, 10, 04).getDayOfWeek());
        Assert.assertEquals(5, new DateComponents( 1582, 10, 15).getDayOfWeek());
        Assert.assertEquals(5, new DateComponents( 1999, 12, 31).getDayOfWeek());
        Assert.assertEquals(6, new DateComponents( 2000, 01, 01).getDayOfWeek());
    }

    @Test
    public void testDayOfYear() {
        Assert.assertEquals(  1, new DateComponents(2003,  1,  1).getDayOfYear());
        Assert.assertEquals(365, new DateComponents(2003, 12, 31).getDayOfYear());
        Assert.assertEquals(366, new DateComponents(2004, 12, 31).getDayOfYear());
        Assert.assertEquals( 59, new DateComponents(2003,  2, 28).getDayOfYear());
        Assert.assertEquals( 60, new DateComponents(2003,  3,  1).getDayOfYear());
        Assert.assertEquals( 59, new DateComponents(2004,  2, 28).getDayOfYear());
        Assert.assertEquals( 60, new DateComponents(2004,  2, 29).getDayOfYear());
        Assert.assertEquals( 61, new DateComponents(2004,  3,  1).getDayOfYear());
        Assert.assertEquals(269, new DateComponents(2003,  9, 26).getDayOfYear());
    }

    @Test
    public void testParse() {

        Assert.assertEquals(-2451546, DateComponents.parseDate("-47131231").getJ2000Day());
        Assert.assertEquals(-2451546, DateComponents.parseDate("-4713-12-31").getJ2000Day());
        Assert.assertEquals(-2451545, DateComponents.parseDate("-47120101").getJ2000Day());
        Assert.assertEquals(-2451545, DateComponents.parseDate("-4712-01-01").getJ2000Day());
        Assert.assertEquals( -730122, DateComponents.parseDate("00001231").getJ2000Day());
        Assert.assertEquals( -730122, DateComponents.parseDate("0000-12-31").getJ2000Day());
        Assert.assertEquals( -730121, DateComponents.parseDate("00010101").getJ2000Day());
        Assert.assertEquals( -730121, DateComponents.parseDate("0001-01-01").getJ2000Day());
        Assert.assertEquals( -182554, DateComponents.parseDate("15000228").getJ2000Day());
        Assert.assertEquals( -182554, DateComponents.parseDate("1500-02-28").getJ2000Day());
        Assert.assertEquals( -182553, DateComponents.parseDate("15000229").getJ2000Day());
        Assert.assertEquals( -182553, DateComponents.parseDate("1500-02-29").getJ2000Day());
        Assert.assertEquals( -182552, DateComponents.parseDate("15000301").getJ2000Day());
        Assert.assertEquals( -182552, DateComponents.parseDate("1500-03-01").getJ2000Day());
        Assert.assertEquals( -152385, DateComponents.parseDate("15821004").getJ2000Day());
        Assert.assertEquals( -152385, DateComponents.parseDate("1582-10-04").getJ2000Day());
        Assert.assertEquals( -152385, DateComponents.parseDate("1582W404").getJ2000Day());
        Assert.assertEquals( -152385, DateComponents.parseDate("1582-W40-4").getJ2000Day());
        Assert.assertEquals( -152384, DateComponents.parseDate("15821015").getJ2000Day());
        Assert.assertEquals( -152384, DateComponents.parseDate("1582-10-15").getJ2000Day());
        Assert.assertEquals( -152384, DateComponents.parseDate("1582W405").getJ2000Day());
        Assert.assertEquals( -152384, DateComponents.parseDate("1582-W40-5").getJ2000Day());
        Assert.assertEquals( -146039, DateComponents.parseDate("16000228").getJ2000Day());
        Assert.assertEquals( -146039, DateComponents.parseDate("1600-02-28").getJ2000Day());
        Assert.assertEquals( -146038, DateComponents.parseDate("16000229").getJ2000Day());
        Assert.assertEquals( -146038, DateComponents.parseDate("1600-02-29").getJ2000Day());
        Assert.assertEquals( -146037, DateComponents.parseDate("1600-03-01").getJ2000Day());
        Assert.assertEquals( -109514, DateComponents.parseDate("17000228").getJ2000Day());
        Assert.assertEquals( -109514, DateComponents.parseDate("1700-02-28").getJ2000Day());
        Assert.assertEquals( -109513, DateComponents.parseDate("17000301").getJ2000Day());
        Assert.assertEquals( -109513, DateComponents.parseDate("1700-03-01").getJ2000Day());
        Assert.assertEquals(  -72990, DateComponents.parseDate("18000228").getJ2000Day());
        Assert.assertEquals(  -72990, DateComponents.parseDate("1800-02-28").getJ2000Day());
        Assert.assertEquals(  -72989, DateComponents.parseDate("18000301").getJ2000Day());
        Assert.assertEquals(  -72989, DateComponents.parseDate("1800-03-01").getJ2000Day());
        Assert.assertEquals(  -51546, DateComponents.parseDate("18581115").getJ2000Day());
        Assert.assertEquals(  -51546, DateComponents.parseDate("1858-11-15").getJ2000Day());
        Assert.assertEquals(  -51545, DateComponents.parseDate("18581116").getJ2000Day());
        Assert.assertEquals(  -51545, DateComponents.parseDate("1858-11-16").getJ2000Day());
        Assert.assertEquals(      -1, DateComponents.parseDate("19991231").getJ2000Day());
        Assert.assertEquals(      -1, DateComponents.parseDate("1999-12-31").getJ2000Day());
        Assert.assertEquals(       0, DateComponents.parseDate("20000101").getJ2000Day());
        Assert.assertEquals(       0, DateComponents.parseDate("2000-01-01").getJ2000Day());
        Assert.assertEquals(       0, DateComponents.parseDate("2000001").getJ2000Day());
        Assert.assertEquals(       0, DateComponents.parseDate("2000-001").getJ2000Day());
        Assert.assertEquals(       0, DateComponents.parseDate("1999-W52-6").getJ2000Day());
        Assert.assertEquals(       0, DateComponents.parseDate("1999W526").getJ2000Day());
        Assert.assertEquals(      58, DateComponents.parseDate("20000228").getJ2000Day());
        Assert.assertEquals(      58, DateComponents.parseDate("2000-02-28").getJ2000Day());
        Assert.assertEquals(      59, DateComponents.parseDate("20000229").getJ2000Day());
        Assert.assertEquals(      59, DateComponents.parseDate("2000-02-29").getJ2000Day());
        Assert.assertEquals(      60, DateComponents.parseDate("20000301").getJ2000Day());
        Assert.assertEquals(      60, DateComponents.parseDate("2000-03-01").getJ2000Day());
    }

    @Test
    public void testMonth() {
        Assert.assertEquals(-51546, new DateComponents(1858, Month.NOVEMBER, 15).getJ2000Day());
        Assert.assertEquals(-51546, new DateComponents(1858, Month.parseMonth("Nov"), 15).getJ2000Day());
        Assert.assertEquals(Month.NOVEMBER, DateComponents.MODIFIED_JULIAN_EPOCH.getMonthEnum());
        Assert.assertEquals(Month.JANUARY, DateComponents.J2000_EPOCH.getMonthEnum());
    }

    @Test
    public void testISO8601Examples() {
        Assert.assertEquals(-5377, DateComponents.parseDate("19850412").getJ2000Day());
        Assert.assertEquals(-5377, DateComponents.parseDate("1985-04-12").getJ2000Day());
        Assert.assertEquals(-5377, DateComponents.parseDate("1985102").getJ2000Day());
        Assert.assertEquals(-5377, DateComponents.parseDate("1985-102").getJ2000Day());
        Assert.assertEquals(-5377, DateComponents.parseDate("1985W155").getJ2000Day());
        Assert.assertEquals(-5377, DateComponents.parseDate("1985-W15-5").getJ2000Day());
    }

    @Test
    public void testComparisons() {
        DateComponents[][] dates = {
                { new DateComponents(2003,  1,  1), new DateComponents(2003,   1) },
                { new DateComponents(2003,  2, 28), new DateComponents(2003,  59) },
                { new DateComponents(2003,  3,  1), new DateComponents(2003,  60) },
                { new DateComponents(2003,  9, 26), new DateComponents(2003, 269) },
                { new DateComponents(2003, 12, 31), new DateComponents(2003, 365) },
                { new DateComponents(2004,  2, 28), new DateComponents(2004,  59) },
                { new DateComponents(2004,  2, 29), new DateComponents(2004,  60) },
                { new DateComponents(2004,  3,  1), new DateComponents(2004,  61) },
                { new DateComponents(2004, 12, 31), new DateComponents(2004, 366) }
        };
        for (int i = 0; i < dates.length; ++i) {
            for (int j = 0; j < dates.length; ++j) {
                if (dates[i][0].compareTo(dates[j][1]) < 0) {
                    Assert.assertTrue(dates[j][1].compareTo(dates[i][0]) > 0);
                    Assert.assertFalse(dates[i][0].equals(dates[j][1]));
                    Assert.assertFalse(dates[j][1].equals(dates[i][0]));
                    Assert.assertTrue(dates[i][0].hashCode() != dates[j][1].hashCode());
                    Assert.assertTrue(i < j);
                } else if (dates[i][0].compareTo(dates[j][1]) > 0) {
                    Assert.assertTrue(dates[j][1].compareTo(dates[i][0]) < 0);
                    Assert.assertFalse(dates[i][0].equals(dates[j][1]));
                    Assert.assertFalse(dates[j][1].equals(dates[i][0]));
                    Assert.assertTrue(dates[i][0].hashCode() != dates[j][1].hashCode());
                    Assert.assertTrue(i > j);
                } else {
                    Assert.assertTrue(dates[j][1].compareTo(dates[i][0]) == 0);
                    Assert.assertTrue(dates[i][0].equals(dates[j][1]));
                    Assert.assertTrue(dates[j][1].equals(dates[i][0]));
                    Assert.assertTrue(dates[i][0].hashCode() == dates[j][1].hashCode());
                    Assert.assertTrue(i == j);
                }
            }
        }
        Assert.assertFalse(dates[0][0].equals(this));
    }

    @Test
    public void testSymmetry() {
        checkSymmetry(-2460000,  20000);
        checkSymmetry( -740000,  20000);
        checkSymmetry( -185000, 200000);
    }

    private void checkSymmetry(int start, int n) {
        for (int i = start; i < start + n; ++i) {
            DateComponents date1 = new DateComponents(DateComponents.J2000_EPOCH, i);
            Assert.assertEquals(i, date1.getJ2000Day());
            DateComponents date2 = new DateComponents(date1.getYear(), date1.getMonth(), date1.getDay());
            Assert.assertEquals(i, date2.getJ2000Day());
        }
    }

    @Test
    public void testString() {
        Assert.assertEquals("2000-01-01", new DateComponents(DateComponents.J2000_EPOCH, 0).toString());
        Assert.assertEquals("-4713-12-31", new DateComponents(DateComponents.J2000_EPOCH, -2451546).toString());
    }

    @Test
    public void testConstructorDoYYearBoundaries() {
        Assert.assertNotNull(new DateComponents(2003, 1));
        Assert.assertNotNull(new DateComponents(2003, 365));
        Assert.assertNotNull(new DateComponents(2004, 1));
        Assert.assertNotNull(new DateComponents(2004, 366));
    }

    @Test(expected=IllegalArgumentException.class)
    public void testConstructorBadDayA() {
        new DateComponents(2003, 0);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testConstructorBadDayB() {
        new DateComponents(2003, 366);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testConstructorBadDayC() {
        new DateComponents(2004, 0);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testConstructorBadDayE() {
        new DateComponents(2004, 367);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testConstructorBadWeek() {
        DateComponents.createFromWeekComponents(2008, 53, 1);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testConstructorBadDayOfWeek1() {
        DateComponents.createFromWeekComponents(2008, 43, 0);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testConstructorBadDayOfWeek2() {
        DateComponents.createFromWeekComponents(2008, 43, 8);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testConstructorBadString() {
        DateComponents.parseDate("197-05-01");
    }

    public void testWellFormed() {
        checkWellFormed(-4800, -4700, -2483687, -2446797);
        checkWellFormed(   -5,     5,  -732313,  -728296);
        checkWellFormed( 1580,  1605,  -153392,  -143906);
        checkWellFormed( 1695,  1705,  -111398,  -107382);
        checkWellFormed( 1795,  1805,   -74874,   -70858);
        checkWellFormed( 1895,  1905,   -38350,   -34334);
        checkWellFormed( 1995,  2005,    -1826,     2191);
    }

    public void checkWellFormed(int startYear, int endYear, int startJ2000, int endJ2000) {

        int[] commonLength = { 0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };
        int[] leapLength   = { 0, 31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };

        int k = startJ2000;
        for (int year = startYear; year <= endYear; ++year) {
            for (int month = 0; month < 14; ++month) {
                for (int day = 0; day < 33; ++day) {

                    // ill-formed dates have predictable components
                    boolean expectedIllFormed = false;
                    if ((month < 1) || (month > 12)) {
                        expectedIllFormed = true;
                    } else if ((year == 1582) && (month == 10) && (day > 4) && (day < 15)) {
                        expectedIllFormed = true;
                    } else if ((year < 1582) && (year % 4 == 0)) {
                        if ((day < 1) || (day > leapLength[month])) {
                            expectedIllFormed = true;
                        }
                    } else if ((year >= 1582) && (year % 4 == 0) &&
                            ((year % 100 != 0) || (year % 400 == 0))) {
                        if ((day < 1) || (day > leapLength[month])) {
                            expectedIllFormed = true;
                        }
                    } else {
                        if ((day < 1) || (day > commonLength[month])) {
                            expectedIllFormed = true;
                        }
                    }

                    try {
                        // well-formed dates should have sequential J2000 days
                        DateComponents date = new DateComponents(year, month, day);
                        Assert.assertEquals(k++, date.getJ2000Day());
                        Assert.assertTrue(!expectedIllFormed);
                    } catch (IllegalArgumentException iae) {
                        Assert.assertTrue(expectedIllFormed);
                    }
                }
            }
        }

        Assert.assertEquals(endJ2000, --k);

    }

    @Test
    public void testMJD() {
        Assert.assertEquals(0, DateComponents.MODIFIED_JULIAN_EPOCH.getMJD());
        Assert.assertEquals(37665, new DateComponents(1962, 1,  1).getMJD());
        Assert.assertEquals(54600, new DateComponents(2008, 5, 14).getMJD());
    }

}
