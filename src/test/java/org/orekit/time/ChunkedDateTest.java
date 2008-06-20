/* Copyright 2002-2008 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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

import org.orekit.time.ChunkedDate;

import junit.framework.*;

public class ChunkedDateTest
extends TestCase {

    public ChunkedDateTest(String name) {
        super(name);
    }

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
            ChunkedDate date = new ChunkedDate(day);
            assertEquals(reference[i][0], date.getYear());
            assertEquals(reference[i][1], date.getMonth());
            assertEquals(reference[i][2], date.getDay());
        }

    }

    public void testDayOfWeek() {
        assertEquals(7, new ChunkedDate(-4713, 12, 31).getDayOfWeek());
        assertEquals(1, new ChunkedDate(-4712, 01, 01).getDayOfWeek());
        assertEquals(4, new ChunkedDate( 1582, 10, 04).getDayOfWeek());
        assertEquals(5, new ChunkedDate( 1582, 10, 15).getDayOfWeek());
        assertEquals(5, new ChunkedDate( 1999, 12, 31).getDayOfWeek());
        assertEquals(6, new ChunkedDate( 2000, 01, 01).getDayOfWeek());
    }

    public void testDayOfYear() {
        assertEquals(  1, new ChunkedDate(2003,  1,  1).getDayOfYear());
        assertEquals(365, new ChunkedDate(2003, 12, 31).getDayOfYear());
        assertEquals(366, new ChunkedDate(2004, 12, 31).getDayOfYear());
        assertEquals( 59, new ChunkedDate(2003,  2, 28).getDayOfYear());
        assertEquals( 60, new ChunkedDate(2003,  3,  1).getDayOfYear());
        assertEquals( 59, new ChunkedDate(2004,  2, 28).getDayOfYear());
        assertEquals( 60, new ChunkedDate(2004,  2, 29).getDayOfYear());
        assertEquals( 61, new ChunkedDate(2004,  3,  1).getDayOfYear());
        assertEquals(269, new ChunkedDate(2003,  9, 26).getDayOfYear());
    }

    public void testComparisons() {
        ChunkedDate[][] dates = {
                { new ChunkedDate(2003,  1,  1), new ChunkedDate(2003,   1) },
                { new ChunkedDate(2003,  2, 28), new ChunkedDate(2003,  59) },
                { new ChunkedDate(2003,  3,  1), new ChunkedDate(2003,  60) },
                { new ChunkedDate(2003,  9, 26), new ChunkedDate(2003, 269) },
                { new ChunkedDate(2003, 12, 31), new ChunkedDate(2003, 365) },
                { new ChunkedDate(2004,  2, 28), new ChunkedDate(2004,  59) },
                { new ChunkedDate(2004,  2, 29), new ChunkedDate(2004,  60) },
                { new ChunkedDate(2004,  3,  1), new ChunkedDate(2004,  61) },
                { new ChunkedDate(2004, 12, 31), new ChunkedDate(2004, 366) }
        };
        for (int i = 0; i < dates.length; ++i) {
            for (int j = 0; j < dates.length; ++j) {
                if (dates[i][0].compareTo(dates[j][1]) < 0) {
                    assertTrue(dates[j][1].compareTo(dates[i][0]) > 0);
                    assertFalse(dates[i][0].equals(dates[j][1]));
                    assertFalse(dates[j][1].equals(dates[i][0]));
                    assertTrue(dates[i][0].hashCode() != dates[j][1].hashCode());
                    assertTrue(i < j);
                } else if (dates[i][0].compareTo(dates[j][1]) > 0) {
                    assertTrue(dates[j][1].compareTo(dates[i][0]) < 0);
                    assertFalse(dates[i][0].equals(dates[j][1]));
                    assertFalse(dates[j][1].equals(dates[i][0]));
                    assertTrue(dates[i][0].hashCode() != dates[j][1].hashCode());
                    assertTrue(i > j);
                } else {
                    assertTrue(dates[j][1].compareTo(dates[i][0]) == 0);
                    assertTrue(dates[i][0].equals(dates[j][1]));
                    assertTrue(dates[j][1].equals(dates[i][0]));
                    assertTrue(dates[i][0].hashCode() == dates[j][1].hashCode());
                    assertTrue(i == j);
                }
            }
        }
        assertFalse(dates[0][0].equals(this));
    }

    public void testSymmetry() {
        checkSymmetry(-2460000,  20000);
        checkSymmetry( -740000,  20000);
        checkSymmetry( -185000, 200000);
    }

    private void checkSymmetry(int start, int n) {
        for (int i = start; i < start + n; ++i) {
            ChunkedDate date1 = new ChunkedDate(i);
            assertEquals(i, date1.getJ2000Day());
            ChunkedDate date2 = new ChunkedDate(date1.getYear(), date1.getMonth(), date1.getDay());
            assertEquals(i, date2.getJ2000Day());
        }        
    }

    public void testString() {
        assertEquals("2000-01-01", new ChunkedDate(0).toString());
        assertEquals("-4713-12-31", new ChunkedDate(-2451546).toString());
    }

    public void testConstructorDoY() {
        checkConstructorDoY(2003, 0,   true);
        checkConstructorDoY(2003, 1,   false);
        checkConstructorDoY(2003, 365, false);
        checkConstructorDoY(2003, 366, true);
        checkConstructorDoY(2004, 0,   true);
        checkConstructorDoY(2004, 1,   false);
        checkConstructorDoY(2004, 366, false);
        checkConstructorDoY(2004, 367, true);
    }

    private void checkConstructorDoY(int year, int day, boolean shouldFail) {
        try {
            new ChunkedDate(year, day);
            if (shouldFail) {
                fail("an exception should have been thrown");
            }
        } catch (IllegalArgumentException iae) {
            if (! shouldFail) {
                fail(iae.getMessage());
            }
        } catch (Exception e) {
            fail("wrong exception caught");
        }
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

                    // ill-formed dates have predictable chunks
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
                        ChunkedDate date = new ChunkedDate(year, month, day);
                        assertEquals(k++, date.getJ2000Day());
                        assertTrue(!expectedIllFormed);
                    } catch (IllegalArgumentException iae) {
                        assertTrue(expectedIllFormed);
                    }
                }
            }
        }

        assertEquals(endJ2000, --k);

    }

    public static Test suite() {
        return new TestSuite(ChunkedDateTest.class);
    }

}
