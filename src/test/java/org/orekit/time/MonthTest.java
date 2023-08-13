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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MonthTest {

    @Test
    public void testUpperCaseName() {
        Assertions.assertEquals("JANUARY",   Month.JANUARY.getUpperCaseName());
        Assertions.assertEquals("FEBRUARY",  Month.FEBRUARY.getUpperCaseName());
        Assertions.assertEquals("MARCH",     Month.MARCH.getUpperCaseName());
        Assertions.assertEquals("APRIL",     Month.APRIL.getUpperCaseName());
        Assertions.assertEquals("MAY",       Month.MAY.getUpperCaseName());
        Assertions.assertEquals("JUNE",      Month.JUNE.getUpperCaseName());
        Assertions.assertEquals("JULY",      Month.JULY.getUpperCaseName());
        Assertions.assertEquals("AUGUST",    Month.AUGUST.getUpperCaseName());
        Assertions.assertEquals("SEPTEMBER", Month.SEPTEMBER.getUpperCaseName());
        Assertions.assertEquals("OCTOBER",   Month.OCTOBER.getUpperCaseName());
        Assertions.assertEquals("NOVEMBER",  Month.NOVEMBER.getUpperCaseName());
        Assertions.assertEquals("DECEMBER",  Month.DECEMBER.getUpperCaseName());
    }

    @Test
    public void testLowerCaseName() {
        Assertions.assertEquals("january",   Month.JANUARY.getLowerCaseName());
        Assertions.assertEquals("february",  Month.FEBRUARY.getLowerCaseName());
        Assertions.assertEquals("march",     Month.MARCH.getLowerCaseName());
        Assertions.assertEquals("april",     Month.APRIL.getLowerCaseName());
        Assertions.assertEquals("may",       Month.MAY.getLowerCaseName());
        Assertions.assertEquals("june",      Month.JUNE.getLowerCaseName());
        Assertions.assertEquals("july",      Month.JULY.getLowerCaseName());
        Assertions.assertEquals("august",    Month.AUGUST.getLowerCaseName());
        Assertions.assertEquals("september", Month.SEPTEMBER.getLowerCaseName());
        Assertions.assertEquals("october",   Month.OCTOBER.getLowerCaseName());
        Assertions.assertEquals("november",  Month.NOVEMBER.getLowerCaseName());
        Assertions.assertEquals("december",  Month.DECEMBER.getLowerCaseName());
    }

    @Test
    public void testCapitalizedCaseName() {
        Assertions.assertEquals("January",   Month.JANUARY.getCapitalizedName());
        Assertions.assertEquals("February",  Month.FEBRUARY.getCapitalizedName());
        Assertions.assertEquals("March",     Month.MARCH.getCapitalizedName());
        Assertions.assertEquals("April",     Month.APRIL.getCapitalizedName());
        Assertions.assertEquals("May",       Month.MAY.getCapitalizedName());
        Assertions.assertEquals("June",      Month.JUNE.getCapitalizedName());
        Assertions.assertEquals("July",      Month.JULY.getCapitalizedName());
        Assertions.assertEquals("August",    Month.AUGUST.getCapitalizedName());
        Assertions.assertEquals("September", Month.SEPTEMBER.getCapitalizedName());
        Assertions.assertEquals("October",   Month.OCTOBER.getCapitalizedName());
        Assertions.assertEquals("November",  Month.NOVEMBER.getCapitalizedName());
        Assertions.assertEquals("December",  Month.DECEMBER.getCapitalizedName());
    }

    @Test
    public void testUpperCaseAbbreviation() {
        Assertions.assertEquals("JAN",       Month.JANUARY.getUpperCaseAbbreviation());
        Assertions.assertEquals("FEB",       Month.FEBRUARY.getUpperCaseAbbreviation());
        Assertions.assertEquals("MAR",       Month.MARCH.getUpperCaseAbbreviation());
        Assertions.assertEquals("APR",       Month.APRIL.getUpperCaseAbbreviation());
        Assertions.assertEquals("MAY",       Month.MAY.getUpperCaseAbbreviation());
        Assertions.assertEquals("JUN",       Month.JUNE.getUpperCaseAbbreviation());
        Assertions.assertEquals("JUL",       Month.JULY.getUpperCaseAbbreviation());
        Assertions.assertEquals("AUG",       Month.AUGUST.getUpperCaseAbbreviation());
        Assertions.assertEquals("SEP",       Month.SEPTEMBER.getUpperCaseAbbreviation());
        Assertions.assertEquals("OCT",       Month.OCTOBER.getUpperCaseAbbreviation());
        Assertions.assertEquals("NOV",       Month.NOVEMBER.getUpperCaseAbbreviation());
        Assertions.assertEquals("DEC",       Month.DECEMBER.getUpperCaseAbbreviation());
    }

    @Test
    public void testLowerCaseAbbreviation() {
        Assertions.assertEquals("jan",       Month.JANUARY.getLowerCaseAbbreviation());
        Assertions.assertEquals("feb",       Month.FEBRUARY.getLowerCaseAbbreviation());
        Assertions.assertEquals("mar",       Month.MARCH.getLowerCaseAbbreviation());
        Assertions.assertEquals("apr",       Month.APRIL.getLowerCaseAbbreviation());
        Assertions.assertEquals("may",       Month.MAY.getLowerCaseAbbreviation());
        Assertions.assertEquals("jun",       Month.JUNE.getLowerCaseAbbreviation());
        Assertions.assertEquals("jul",       Month.JULY.getLowerCaseAbbreviation());
        Assertions.assertEquals("aug",       Month.AUGUST.getLowerCaseAbbreviation());
        Assertions.assertEquals("sep",       Month.SEPTEMBER.getLowerCaseAbbreviation());
        Assertions.assertEquals("oct",       Month.OCTOBER.getLowerCaseAbbreviation());
        Assertions.assertEquals("nov",       Month.NOVEMBER.getLowerCaseAbbreviation());
        Assertions.assertEquals("dec",       Month.DECEMBER.getLowerCaseAbbreviation());
    }

    @Test
    public void testCapitalizedCaseAbbreviation() {
        Assertions.assertEquals("Jan",       Month.JANUARY.getCapitalizedAbbreviation());
        Assertions.assertEquals("Feb",       Month.FEBRUARY.getCapitalizedAbbreviation());
        Assertions.assertEquals("Mar",       Month.MARCH.getCapitalizedAbbreviation());
        Assertions.assertEquals("Apr",       Month.APRIL.getCapitalizedAbbreviation());
        Assertions.assertEquals("May",       Month.MAY.getCapitalizedAbbreviation());
        Assertions.assertEquals("Jun",       Month.JUNE.getCapitalizedAbbreviation());
        Assertions.assertEquals("Jul",       Month.JULY.getCapitalizedAbbreviation());
        Assertions.assertEquals("Aug",       Month.AUGUST.getCapitalizedAbbreviation());
        Assertions.assertEquals("Sep",       Month.SEPTEMBER.getCapitalizedAbbreviation());
        Assertions.assertEquals("Oct",       Month.OCTOBER.getCapitalizedAbbreviation());
        Assertions.assertEquals("Nov",       Month.NOVEMBER.getCapitalizedAbbreviation());
        Assertions.assertEquals("Dec",       Month.DECEMBER.getCapitalizedAbbreviation());
    }

    @Test
    public void testParsing() {
        Assertions.assertEquals(Month.AUGUST, Month.parseMonth("   AUGUST "));
        Assertions.assertEquals(Month.AUGUST, Month.parseMonth(" august  "));
        Assertions.assertEquals(Month.AUGUST, Month.parseMonth("August"));
        Assertions.assertEquals(Month.AUGUST, Month.parseMonth("\tAUG"));
        Assertions.assertEquals(Month.AUGUST, Month.parseMonth("august"));
        Assertions.assertEquals(Month.AUGUST, Month.parseMonth("Aug"));
        Assertions.assertEquals(Month.AUGUST, Month.parseMonth("aUgUsT  "));
        Assertions.assertEquals(Month.AUGUST, Month.parseMonth(" 8 "));
        Assertions.assertEquals(Month.AUGUST, Month.parseMonth("00008"));
    }

    @Test
    public void testParsingErrorEmpty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Month.parseMonth("  ");
        });
    }

    @Test
    public void testParsingErrorTooLow() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Month.parseMonth("0");
        });
    }

    @Test
    public void testParsingErrorTooHigh() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Month.parseMonth("13");
        });
    }

    @Test
    public void testParsingErrorCorruptedString() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Month.parseMonth("AUGUSTE");
        });
    }

}
