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

public class MonthTest {

    @Test
    public void testUpperCaseName() {
        Assert.assertEquals("JANUARY",   Month.JANUARY.getUpperCaseName());
        Assert.assertEquals("FEBRUARY",  Month.FEBRUARY.getUpperCaseName());
        Assert.assertEquals("MARCH",     Month.MARCH.getUpperCaseName());
        Assert.assertEquals("APRIL",     Month.APRIL.getUpperCaseName());
        Assert.assertEquals("MAY",       Month.MAY.getUpperCaseName());
        Assert.assertEquals("JUNE",      Month.JUNE.getUpperCaseName());
        Assert.assertEquals("JULY",      Month.JULY.getUpperCaseName());
        Assert.assertEquals("AUGUST",    Month.AUGUST.getUpperCaseName());
        Assert.assertEquals("SEPTEMBER", Month.SEPTEMBER.getUpperCaseName());
        Assert.assertEquals("OCTOBER",   Month.OCTOBER.getUpperCaseName());
        Assert.assertEquals("NOVEMBER",  Month.NOVEMBER.getUpperCaseName());
        Assert.assertEquals("DECEMBER",  Month.DECEMBER.getUpperCaseName());
    }

    @Test
    public void testLowerCaseName() {
        Assert.assertEquals("january",   Month.JANUARY.getLowerCaseName());
        Assert.assertEquals("february",  Month.FEBRUARY.getLowerCaseName());
        Assert.assertEquals("march",     Month.MARCH.getLowerCaseName());
        Assert.assertEquals("april",     Month.APRIL.getLowerCaseName());
        Assert.assertEquals("may",       Month.MAY.getLowerCaseName());
        Assert.assertEquals("june",      Month.JUNE.getLowerCaseName());
        Assert.assertEquals("july",      Month.JULY.getLowerCaseName());
        Assert.assertEquals("august",    Month.AUGUST.getLowerCaseName());
        Assert.assertEquals("september", Month.SEPTEMBER.getLowerCaseName());
        Assert.assertEquals("october",   Month.OCTOBER.getLowerCaseName());
        Assert.assertEquals("november",  Month.NOVEMBER.getLowerCaseName());
        Assert.assertEquals("december",  Month.DECEMBER.getLowerCaseName());
    }

    @Test
    public void testCapitalizedCaseName() {
        Assert.assertEquals("January",   Month.JANUARY.getCapitalizedName());
        Assert.assertEquals("February",  Month.FEBRUARY.getCapitalizedName());
        Assert.assertEquals("March",     Month.MARCH.getCapitalizedName());
        Assert.assertEquals("April",     Month.APRIL.getCapitalizedName());
        Assert.assertEquals("May",       Month.MAY.getCapitalizedName());
        Assert.assertEquals("June",      Month.JUNE.getCapitalizedName());
        Assert.assertEquals("July",      Month.JULY.getCapitalizedName());
        Assert.assertEquals("August",    Month.AUGUST.getCapitalizedName());
        Assert.assertEquals("September", Month.SEPTEMBER.getCapitalizedName());
        Assert.assertEquals("October",   Month.OCTOBER.getCapitalizedName());
        Assert.assertEquals("November",  Month.NOVEMBER.getCapitalizedName());
        Assert.assertEquals("December",  Month.DECEMBER.getCapitalizedName());
    }

    @Test
    public void testUpperCaseAbbreviation() {
        Assert.assertEquals("JAN",       Month.JANUARY.getUpperCaseAbbreviation());
        Assert.assertEquals("FEB",       Month.FEBRUARY.getUpperCaseAbbreviation());
        Assert.assertEquals("MAR",       Month.MARCH.getUpperCaseAbbreviation());
        Assert.assertEquals("APR",       Month.APRIL.getUpperCaseAbbreviation());
        Assert.assertEquals("MAY",       Month.MAY.getUpperCaseAbbreviation());
        Assert.assertEquals("JUN",       Month.JUNE.getUpperCaseAbbreviation());
        Assert.assertEquals("JUL",       Month.JULY.getUpperCaseAbbreviation());
        Assert.assertEquals("AUG",       Month.AUGUST.getUpperCaseAbbreviation());
        Assert.assertEquals("SEP",       Month.SEPTEMBER.getUpperCaseAbbreviation());
        Assert.assertEquals("OCT",       Month.OCTOBER.getUpperCaseAbbreviation());
        Assert.assertEquals("NOV",       Month.NOVEMBER.getUpperCaseAbbreviation());
        Assert.assertEquals("DEC",       Month.DECEMBER.getUpperCaseAbbreviation());
    }

    @Test
    public void testLowerCaseAbbreviation() {
        Assert.assertEquals("jan",       Month.JANUARY.getLowerCaseAbbreviation());
        Assert.assertEquals("feb",       Month.FEBRUARY.getLowerCaseAbbreviation());
        Assert.assertEquals("mar",       Month.MARCH.getLowerCaseAbbreviation());
        Assert.assertEquals("apr",       Month.APRIL.getLowerCaseAbbreviation());
        Assert.assertEquals("may",       Month.MAY.getLowerCaseAbbreviation());
        Assert.assertEquals("jun",       Month.JUNE.getLowerCaseAbbreviation());
        Assert.assertEquals("jul",       Month.JULY.getLowerCaseAbbreviation());
        Assert.assertEquals("aug",       Month.AUGUST.getLowerCaseAbbreviation());
        Assert.assertEquals("sep",       Month.SEPTEMBER.getLowerCaseAbbreviation());
        Assert.assertEquals("oct",       Month.OCTOBER.getLowerCaseAbbreviation());
        Assert.assertEquals("nov",       Month.NOVEMBER.getLowerCaseAbbreviation());
        Assert.assertEquals("dec",       Month.DECEMBER.getLowerCaseAbbreviation());
    }

    @Test
    public void testCapitalizedCaseAbbreviation() {
        Assert.assertEquals("Jan",       Month.JANUARY.getCapitalizedAbbreviation());
        Assert.assertEquals("Feb",       Month.FEBRUARY.getCapitalizedAbbreviation());
        Assert.assertEquals("Mar",       Month.MARCH.getCapitalizedAbbreviation());
        Assert.assertEquals("Apr",       Month.APRIL.getCapitalizedAbbreviation());
        Assert.assertEquals("May",       Month.MAY.getCapitalizedAbbreviation());
        Assert.assertEquals("Jun",       Month.JUNE.getCapitalizedAbbreviation());
        Assert.assertEquals("Jul",       Month.JULY.getCapitalizedAbbreviation());
        Assert.assertEquals("Aug",       Month.AUGUST.getCapitalizedAbbreviation());
        Assert.assertEquals("Sep",       Month.SEPTEMBER.getCapitalizedAbbreviation());
        Assert.assertEquals("Oct",       Month.OCTOBER.getCapitalizedAbbreviation());
        Assert.assertEquals("Nov",       Month.NOVEMBER.getCapitalizedAbbreviation());
        Assert.assertEquals("Dec",       Month.DECEMBER.getCapitalizedAbbreviation());
    }

    @Test
    public void testParsing() {
        Assert.assertEquals(Month.AUGUST, Month.parseMonth("   AUGUST "));
        Assert.assertEquals(Month.AUGUST, Month.parseMonth(" august  "));
        Assert.assertEquals(Month.AUGUST, Month.parseMonth("August"));
        Assert.assertEquals(Month.AUGUST, Month.parseMonth("\tAUG"));
        Assert.assertEquals(Month.AUGUST, Month.parseMonth("august"));
        Assert.assertEquals(Month.AUGUST, Month.parseMonth("Aug"));
        Assert.assertEquals(Month.AUGUST, Month.parseMonth("aUgUsT  "));
        Assert.assertEquals(Month.AUGUST, Month.parseMonth(" 8 "));
        Assert.assertEquals(Month.AUGUST, Month.parseMonth("00008"));
    }

    @Test(expected=IllegalArgumentException.class)
    public void testParsingErrorEmpty() {
        Month.parseMonth("  ");
    }

    @Test(expected=IllegalArgumentException.class)
    public void testParsingErrorTooLow() {
        Month.parseMonth("0");
    }

    @Test(expected=IllegalArgumentException.class)
    public void testParsingErrorTooHigh() {
        Month.parseMonth("13");
    }

    @Test(expected=IllegalArgumentException.class)
    public void testParsingErrorCorruptedString() {
        Month.parseMonth("AUGUSTE");
    }

}
