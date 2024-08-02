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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MonthTest {

    @Test
    void testUpperCaseName() {
        assertEquals("JANUARY",   Month.JANUARY.getUpperCaseName());
        assertEquals("FEBRUARY",  Month.FEBRUARY.getUpperCaseName());
        assertEquals("MARCH",     Month.MARCH.getUpperCaseName());
        assertEquals("APRIL",     Month.APRIL.getUpperCaseName());
        assertEquals("MAY",       Month.MAY.getUpperCaseName());
        assertEquals("JUNE",      Month.JUNE.getUpperCaseName());
        assertEquals("JULY",      Month.JULY.getUpperCaseName());
        assertEquals("AUGUST",    Month.AUGUST.getUpperCaseName());
        assertEquals("SEPTEMBER", Month.SEPTEMBER.getUpperCaseName());
        assertEquals("OCTOBER",   Month.OCTOBER.getUpperCaseName());
        assertEquals("NOVEMBER",  Month.NOVEMBER.getUpperCaseName());
        assertEquals("DECEMBER",  Month.DECEMBER.getUpperCaseName());
    }

    @Test
    void testLowerCaseName() {
        assertEquals("january",   Month.JANUARY.getLowerCaseName());
        assertEquals("february",  Month.FEBRUARY.getLowerCaseName());
        assertEquals("march",     Month.MARCH.getLowerCaseName());
        assertEquals("april",     Month.APRIL.getLowerCaseName());
        assertEquals("may",       Month.MAY.getLowerCaseName());
        assertEquals("june",      Month.JUNE.getLowerCaseName());
        assertEquals("july",      Month.JULY.getLowerCaseName());
        assertEquals("august",    Month.AUGUST.getLowerCaseName());
        assertEquals("september", Month.SEPTEMBER.getLowerCaseName());
        assertEquals("october",   Month.OCTOBER.getLowerCaseName());
        assertEquals("november",  Month.NOVEMBER.getLowerCaseName());
        assertEquals("december",  Month.DECEMBER.getLowerCaseName());
    }

    @Test
    void testCapitalizedCaseName() {
        assertEquals("January",   Month.JANUARY.getCapitalizedName());
        assertEquals("February",  Month.FEBRUARY.getCapitalizedName());
        assertEquals("March",     Month.MARCH.getCapitalizedName());
        assertEquals("April",     Month.APRIL.getCapitalizedName());
        assertEquals("May",       Month.MAY.getCapitalizedName());
        assertEquals("June",      Month.JUNE.getCapitalizedName());
        assertEquals("July",      Month.JULY.getCapitalizedName());
        assertEquals("August",    Month.AUGUST.getCapitalizedName());
        assertEquals("September", Month.SEPTEMBER.getCapitalizedName());
        assertEquals("October",   Month.OCTOBER.getCapitalizedName());
        assertEquals("November",  Month.NOVEMBER.getCapitalizedName());
        assertEquals("December",  Month.DECEMBER.getCapitalizedName());
    }

    @Test
    void testUpperCaseAbbreviation() {
        assertEquals("JAN",       Month.JANUARY.getUpperCaseAbbreviation());
        assertEquals("FEB",       Month.FEBRUARY.getUpperCaseAbbreviation());
        assertEquals("MAR",       Month.MARCH.getUpperCaseAbbreviation());
        assertEquals("APR",       Month.APRIL.getUpperCaseAbbreviation());
        assertEquals("MAY",       Month.MAY.getUpperCaseAbbreviation());
        assertEquals("JUN",       Month.JUNE.getUpperCaseAbbreviation());
        assertEquals("JUL",       Month.JULY.getUpperCaseAbbreviation());
        assertEquals("AUG",       Month.AUGUST.getUpperCaseAbbreviation());
        assertEquals("SEP",       Month.SEPTEMBER.getUpperCaseAbbreviation());
        assertEquals("OCT",       Month.OCTOBER.getUpperCaseAbbreviation());
        assertEquals("NOV",       Month.NOVEMBER.getUpperCaseAbbreviation());
        assertEquals("DEC",       Month.DECEMBER.getUpperCaseAbbreviation());
    }

    @Test
    void testLowerCaseAbbreviation() {
        assertEquals("jan",       Month.JANUARY.getLowerCaseAbbreviation());
        assertEquals("feb",       Month.FEBRUARY.getLowerCaseAbbreviation());
        assertEquals("mar",       Month.MARCH.getLowerCaseAbbreviation());
        assertEquals("apr",       Month.APRIL.getLowerCaseAbbreviation());
        assertEquals("may",       Month.MAY.getLowerCaseAbbreviation());
        assertEquals("jun",       Month.JUNE.getLowerCaseAbbreviation());
        assertEquals("jul",       Month.JULY.getLowerCaseAbbreviation());
        assertEquals("aug",       Month.AUGUST.getLowerCaseAbbreviation());
        assertEquals("sep",       Month.SEPTEMBER.getLowerCaseAbbreviation());
        assertEquals("oct",       Month.OCTOBER.getLowerCaseAbbreviation());
        assertEquals("nov",       Month.NOVEMBER.getLowerCaseAbbreviation());
        assertEquals("dec",       Month.DECEMBER.getLowerCaseAbbreviation());
    }

    @Test
    void testCapitalizedCaseAbbreviation() {
        assertEquals("Jan",       Month.JANUARY.getCapitalizedAbbreviation());
        assertEquals("Feb",       Month.FEBRUARY.getCapitalizedAbbreviation());
        assertEquals("Mar",       Month.MARCH.getCapitalizedAbbreviation());
        assertEquals("Apr",       Month.APRIL.getCapitalizedAbbreviation());
        assertEquals("May",       Month.MAY.getCapitalizedAbbreviation());
        assertEquals("Jun",       Month.JUNE.getCapitalizedAbbreviation());
        assertEquals("Jul",       Month.JULY.getCapitalizedAbbreviation());
        assertEquals("Aug",       Month.AUGUST.getCapitalizedAbbreviation());
        assertEquals("Sep",       Month.SEPTEMBER.getCapitalizedAbbreviation());
        assertEquals("Oct",       Month.OCTOBER.getCapitalizedAbbreviation());
        assertEquals("Nov",       Month.NOVEMBER.getCapitalizedAbbreviation());
        assertEquals("Dec",       Month.DECEMBER.getCapitalizedAbbreviation());
    }

    @Test
    void testParsing() {
        assertEquals(Month.AUGUST, Month.parseMonth("   AUGUST "));
        assertEquals(Month.AUGUST, Month.parseMonth(" august  "));
        assertEquals(Month.AUGUST, Month.parseMonth("August"));
        assertEquals(Month.AUGUST, Month.parseMonth("\tAUG"));
        assertEquals(Month.AUGUST, Month.parseMonth("august"));
        assertEquals(Month.AUGUST, Month.parseMonth("Aug"));
        assertEquals(Month.AUGUST, Month.parseMonth("aUgUsT  "));
        assertEquals(Month.AUGUST, Month.parseMonth(" 8 "));
        assertEquals(Month.AUGUST, Month.parseMonth("00008"));
    }

    @Test
    void testParsingErrorEmpty() {
        assertThrows(IllegalArgumentException.class, () -> {
            Month.parseMonth("  ");
        });
    }

    @Test
    void testParsingErrorTooLow() {
        assertThrows(IllegalArgumentException.class, () -> {
            Month.parseMonth("0");
        });
    }

    @Test
    void testParsingErrorTooHigh() {
        assertThrows(IllegalArgumentException.class, () -> {
            Month.parseMonth("13");
        });
    }

    @Test
    void testParsingErrorCorruptedString() {
        assertThrows(IllegalArgumentException.class, () -> {
            Month.parseMonth("AUGUSTE");
        });
    }

}
