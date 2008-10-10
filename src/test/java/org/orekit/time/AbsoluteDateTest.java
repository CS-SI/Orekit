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

import java.util.Date;

import org.orekit.data.DataDirectoryCrawler;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.DateTimeComponents;
import org.orekit.time.TAIScale;
import org.orekit.time.TCGScale;
import org.orekit.time.TTScale;
import org.orekit.time.TimeScale;
import org.orekit.time.UTCScale;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class AbsoluteDateTest
extends TestCase {

    public AbsoluteDateTest(String name) {
        super(name);
    }

    public void testStandardEpoch() {
        TimeScale tai = TAIScale.getInstance();
        TimeScale tt  = TTScale.getInstance();
        assertEquals(-210866760000000l, AbsoluteDate.JULIAN_EPOCH.toDate(tt).getTime());
        assertEquals(-3506716800000l,   AbsoluteDate.MODIFIED_JULIAN_EPOCH.toDate(tt).getTime());
        assertEquals(-631152000000l,    AbsoluteDate.FIFTIES_EPOCH.toDate(tt).getTime());
        assertEquals(315964819000l,     AbsoluteDate.GPS_EPOCH.toDate(tai).getTime());
        assertEquals(946728000000l,     AbsoluteDate.J2000_EPOCH.toDate(tt).getTime());
    }

    public void testStandardEpochStrings() throws OrekitException {
        assertEquals("-4712-01-01T12:00:00.000",
                     AbsoluteDate.JULIAN_EPOCH.toString(TTScale.getInstance()));
        assertEquals("1858-11-17T00:00:00.000",
                     AbsoluteDate.MODIFIED_JULIAN_EPOCH.toString(TTScale.getInstance()));
        assertEquals("1950-01-01T00:00:00.000",
                     AbsoluteDate.FIFTIES_EPOCH.toString(TTScale.getInstance()));
        assertEquals("1980-01-06T00:00:00.000",
                     AbsoluteDate.GPS_EPOCH.toString(UTCScale.getInstance()));
        assertEquals("2000-01-01T12:00:00.000",
                     AbsoluteDate.J2000_EPOCH.toString(TTScale.getInstance()));
        assertEquals("1970-01-01T00:00:00.000",
                     AbsoluteDate.JAVA_EPOCH.toString(TTScale.getInstance()));
    }

    public void testOutput() {
        TimeScale tt = TTScale.getInstance();
        assertEquals("1950-01-01T01:01:01.000",
                     new AbsoluteDate(AbsoluteDate.FIFTIES_EPOCH, 3661.0).toString(tt));
        assertEquals("2000-01-01T13:01:01.000",
                     new AbsoluteDate(AbsoluteDate.J2000_EPOCH, 3661.0).toString(tt));
    }

    public void testJ2000() {
        assertEquals("2000-01-01T12:00:00.000",
                     AbsoluteDate.J2000_EPOCH.toString(TTScale.getInstance()));
        assertEquals("2000-01-01T11:59:27.816",
                     AbsoluteDate.J2000_EPOCH.toString(TAIScale.getInstance()));
        assertEquals("2000-01-01T11:58:55.816",
                     AbsoluteDate.J2000_EPOCH.toString(utc));
    }

    public void testFraction() {
        AbsoluteDate d =
            new AbsoluteDate(new DateComponents(2000, 01, 01), new TimeComponents(11, 59, 27.816),
                             TAIScale.getInstance());
        assertEquals(0, d.durationFrom(AbsoluteDate.J2000_EPOCH), 1.0e-10);
    }

    public void testScalesOffset() {
        AbsoluteDate date = new AbsoluteDate(new DateComponents(2006, 02, 24),
                                             new TimeComponents(15, 38, 00),
                                             utc);
        assertEquals(33,
                     date.timeScalesOffset(TAIScale.getInstance(), utc),
                     1.0e-10);
    }

    public void testUTC() {
        AbsoluteDate date = new AbsoluteDate(new DateComponents(2002, 01, 01),
                                             new TimeComponents(00, 00, 01),
                                             utc);
        assertEquals("2002-01-01T00:00:01.000", date.toString());
    }

    public void test1970() {
        AbsoluteDate date = new AbsoluteDate(new Date(0l), utc);
        assertEquals("1970-01-01T00:00:00.000", date.toString());
    }

    public void testUtcGpsOffset() {
        AbsoluteDate date1   = new AbsoluteDate(new DateComponents(2005, 8, 9),
                                                new TimeComponents(16, 31, 17),
                                                utc);
        AbsoluteDate date2   = new AbsoluteDate(new DateComponents(2006, 8, 9),
                                                new TimeComponents(16, 31, 17),
                                                utc);
        AbsoluteDate dateRef = new AbsoluteDate(new DateComponents(1980, 1, 6),
                                                TimeComponents.H00,
                                                utc);

        // 13 seconds offset between GPS time and UTC in 2005
        long noLeapGap = ((9347 * 24 + 16) * 60 + 31) * 60 + 17;
        long realGap   = (long) date1.durationFrom(dateRef);
        assertEquals(13, realGap - noLeapGap);

        // 14 seconds offset between GPS time and UTC in 2006
        noLeapGap = ((9712 * 24 + 16) * 60 + 31) * 60 + 17;
        realGap   = (long) date2.durationFrom(dateRef);
        assertEquals(14, realGap - noLeapGap);

    }

    public void testGpsDate() {
        AbsoluteDate date = AbsoluteDate.createGPSDate(1387, 318677000.0);
        AbsoluteDate ref  = new AbsoluteDate(new DateComponents(2006, 8, 9),
                                             new TimeComponents(16, 31, 03),
                                             utc);
        assertEquals(0, date.durationFrom(ref), 1.0e-12);
    }

    public void testOffsets() {
        final TimeScale tai = TAIScale.getInstance();
        AbsoluteDate leapStartUTC = new AbsoluteDate(1976, 12, 31, 23, 59, 59, utc);
        AbsoluteDate leapEndUTC   = new AbsoluteDate(1977,  1,  1,  0,  0,  0, utc);
        AbsoluteDate leapStartTAI = new AbsoluteDate(1977,  1,  1,  0,  0, 14, tai);
        AbsoluteDate leapEndTAI   = new AbsoluteDate(1977,  1,  1,  0,  0, 16, tai);
        assertEquals(leapStartUTC, leapStartTAI);
        assertEquals(leapEndUTC, leapEndTAI);
        assertEquals(1, leapEndUTC.offsetFrom(leapStartUTC, utc), 1.0e-10);
        assertEquals(1, leapEndTAI.offsetFrom(leapStartTAI, utc), 1.0e-10);
        assertEquals(2, leapEndUTC.offsetFrom(leapStartUTC, tai), 1.0e-10);
        assertEquals(2, leapEndTAI.offsetFrom(leapStartTAI, tai), 1.0e-10);
        assertEquals(2, leapEndUTC.durationFrom(leapStartUTC),    1.0e-10);
        assertEquals(2, leapEndTAI.durationFrom(leapStartTAI),    1.0e-10);
    }

    public void testBeforeAndAfterLeap() {
        final TimeScale tai = TAIScale.getInstance();
        AbsoluteDate leapStart = new AbsoluteDate(1977,  1,  1,  0,  0, 14, tai);
        AbsoluteDate leapEnd   = new AbsoluteDate(1977,  1,  1,  0,  0, 16, tai);
        for (int i = -10; i < 10; ++i) {
            final double dt = 1.1 * (2 * i - 1);
            AbsoluteDate d1 = new AbsoluteDate(leapStart, dt);
            AbsoluteDate d2 = new AbsoluteDate(leapStart, dt, tai);
            AbsoluteDate d3 = new AbsoluteDate(leapStart, dt, utc);
            AbsoluteDate d4 = new AbsoluteDate(leapEnd,   dt, tai);
            AbsoluteDate d5 = new AbsoluteDate(leapEnd,   dt, utc);
            assertTrue(Math.abs(d1.durationFrom(d2)) < 1.0e-10);
            if (dt < 0) {
                assertTrue(Math.abs(d2.durationFrom(d3)) < 1.0e-10);
                assertTrue(d4.durationFrom(d5) > (1.0 - 1.0e-10));
            } else {
                assertTrue(d2.durationFrom(d3) < (-1.0 + 1.0e-10));
                assertTrue(Math.abs(d4.durationFrom(d5)) < 1.0e-10);
            }
        }
    }

    public void testSymmetry() {
        final TimeScale tai = TAIScale.getInstance();
        AbsoluteDate leapStart = new AbsoluteDate(1977,  1,  1,  0,  0, 14, tai);
        for (int i = -10; i < 10; ++i) {
            final double dt = 1.1 * (2 * i - 1);
            assertEquals(dt, new AbsoluteDate(leapStart, dt, utc).offsetFrom(leapStart, utc), 1.0e-10);
            assertEquals(dt, new AbsoluteDate(leapStart, dt, tai).offsetFrom(leapStart, tai), 1.0e-10);
            assertEquals(dt, new AbsoluteDate(leapStart, dt).durationFrom(leapStart), 1.0e-10);
        }
    }

    public void testEquals() {
        AbsoluteDate d1 =
            new AbsoluteDate(new DateComponents(2006, 2, 25),
                             new TimeComponents(17, 10, 34),
                             utc);
        AbsoluteDate d2 =
            new AbsoluteDate(new AbsoluteDate(new DateComponents(2006, 2, 25),
                                              new TimeComponents(17, 10, 0),
                                              utc),
                             34);
        assertTrue(d1.equals(d2));
        assertFalse(d1.equals(this));
    }

    public void testComponents() throws OrekitException {
        // this is NOT J2000.0,
        // it is either a few seconds before or after depending on time scale
        DateComponents date = new DateComponents(2000, 01,01);
        TimeComponents time = new TimeComponents(11, 59, 10);
        TimeScale[] scales = {
            TAIScale.getInstance(), UTCScale.getInstance(),
            TTScale.getInstance(), TCGScale.getInstance()      
        };
        for (int i = 0; i < scales.length; ++i) {
            AbsoluteDate in = new AbsoluteDate(date, time, scales[i]);
            for (int j = 0; j < scales.length; ++j) {
                DateTimeComponents pair = in.getComponents(scales[j]);
                if (i == j) {
                    assertEquals(date, pair.getDate());
                    assertEquals(time, pair.getTime());
                } else {
                    assertNotSame(date, pair.getDate());
                    assertNotSame(time, pair.getTime());
                }
            }
        }
    }

    public void testExpandedConstructors() throws OrekitException {
        assertEquals(new AbsoluteDate(new DateComponents(2002, 05, 28),
                                      new TimeComponents(15, 30, 0),
                                      UTCScale.getInstance()),
                     new AbsoluteDate(2002, 05, 28, 15, 30, 0, UTCScale.getInstance()));
        assertEquals(new AbsoluteDate(new DateComponents(2002, 05, 28), TimeComponents.H00,
                                      UTCScale.getInstance()),
                     new AbsoluteDate(2002, 05, 28, UTCScale.getInstance()));
        try {
            new AbsoluteDate(2002, 05, 28, 25, 30, 0, UTCScale.getInstance());
        } catch (IllegalArgumentException iae) {
            // expected behavior
        } catch (Exception e) {
            fail("wrong exception caught");
        }
    }

    public void testHashcode() {
        AbsoluteDate d1 =
            new AbsoluteDate(new DateComponents(2006, 2, 25),
                             new TimeComponents(17, 10, 34),
                             utc);
        AbsoluteDate d2 =
            new AbsoluteDate(new AbsoluteDate(new DateComponents(2006, 2, 25),
                                              new TimeComponents(17, 10, 0),
                                              utc),
                             34);
        assertEquals(d1.hashCode(), d2.hashCode());
        assertTrue(d1.hashCode() != new AbsoluteDate(d1, 1.0e-3).hashCode());
    }

    public void testInfinity() {
        assertTrue(AbsoluteDate.PAST_INFINITY.compareTo(AbsoluteDate.JULIAN_EPOCH) < 0);
        assertTrue(AbsoluteDate.PAST_INFINITY.compareTo(AbsoluteDate.J2000_EPOCH) < 0);
        assertTrue(AbsoluteDate.PAST_INFINITY.compareTo(AbsoluteDate.FUTURE_INFINITY) < 0);
        assertTrue(AbsoluteDate.FUTURE_INFINITY.compareTo(AbsoluteDate.JULIAN_EPOCH) > 0);
        assertTrue(AbsoluteDate.FUTURE_INFINITY.compareTo(AbsoluteDate.J2000_EPOCH) > 0);
        assertTrue(Double.isInfinite(AbsoluteDate.FUTURE_INFINITY.durationFrom(AbsoluteDate.J2000_EPOCH)));
        assertTrue(Double.isInfinite(AbsoluteDate.FUTURE_INFINITY.durationFrom(AbsoluteDate.PAST_INFINITY)));
        assertTrue(Double.isInfinite(AbsoluteDate.PAST_INFINITY.durationFrom(AbsoluteDate.J2000_EPOCH)));
    }

    public void setUp() throws OrekitException {
        String root = getClass().getClassLoader().getResource("regular-data").getPath();
        System.setProperty(DataDirectoryCrawler.OREKIT_DATA_PATH, root);
        utc = UTCScale.getInstance();
    }

    public static Test suite() {
        return new TestSuite(AbsoluteDateTest.class);
    }

    private TimeScale utc;

}
