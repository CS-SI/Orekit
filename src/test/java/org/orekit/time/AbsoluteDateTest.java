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

import org.orekit.errors.OrekitException;
import org.orekit.iers.IERSDirectoryCrawler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChunkedDate;
import org.orekit.time.ChunkedTime;
import org.orekit.time.ChunksPair;
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
            new AbsoluteDate(new ChunkedDate(2000, 01, 01), new ChunkedTime(11, 59, 27.816),
                             TAIScale.getInstance());
        assertEquals(0, d.minus(AbsoluteDate.J2000_EPOCH), 1.0e-10);
    }

    public void testScalesOffset() {
        AbsoluteDate date = new AbsoluteDate(new ChunkedDate(2006, 02, 24),
                                             new ChunkedTime(15, 38, 00),
                                             utc);
        assertEquals(33,
                     date.timeScalesOffset(TAIScale.getInstance(), utc),
                     1.0e-10);
    }

    public void testUTC() {
        AbsoluteDate date = new AbsoluteDate(new ChunkedDate(2002, 01, 01),
                                             new ChunkedTime(00, 00, 01),
                                             utc);
        assertEquals("2002-01-01T00:00:01.000", date.toString());
    }

    public void test1970() {
        AbsoluteDate date = new AbsoluteDate(new Date(0l), utc);
        assertEquals("1970-01-01T00:00:00.000", date.toString());
    }

    public void testUtcGpsOffset() {
        AbsoluteDate date1   = new AbsoluteDate(new ChunkedDate(2005, 8, 9),
                                                new ChunkedTime(16, 31, 17),
                                                utc);
        AbsoluteDate date2   = new AbsoluteDate(new ChunkedDate(2006, 8, 9),
                                                new ChunkedTime(16, 31, 17),
                                                utc);
        AbsoluteDate dateRef = new AbsoluteDate(new ChunkedDate(1980, 1, 6),
                                                ChunkedTime.H00,
                                                utc);

        // 13 seconds offset between GPS time and UTC in 2005
        long noLeapGap = ((9347 * 24 + 16) * 60 + 31) * 60 + 17;
        long realGap   = (long) date1.minus(dateRef);
        assertEquals(13, realGap - noLeapGap);

        // 14 seconds offset between GPS time and UTC in 2006
        noLeapGap = ((9712 * 24 + 16) * 60 + 31) * 60 + 17;
        realGap   = (long) date2.minus(dateRef);
        assertEquals(14, realGap - noLeapGap);

    }

    public void testGpsDate() {
        AbsoluteDate date = AbsoluteDate.createGPSDate(1387, 318677000.0);
        AbsoluteDate ref  = new AbsoluteDate(new ChunkedDate(2006, 8, 9),
                                             new ChunkedTime(16, 31, 03),
                                             utc);
        assertEquals(0, date.minus(ref), 1.0e-12);
    }

    public void testEquals() {
        AbsoluteDate d1 =
            new AbsoluteDate(new ChunkedDate(2006, 2, 25),
                             new ChunkedTime(17, 10, 34),
                             utc);
        AbsoluteDate d2 =
            new AbsoluteDate(new AbsoluteDate(new ChunkedDate(2006, 2, 25),
                                              new ChunkedTime(17, 10, 0),
                                              utc),
                             34);
        assertTrue(d1.equals(d2));
        assertFalse(d1.equals(this));
    }

    public void testChunks() throws OrekitException {
        // this is NOT J2000, it is a few seconds before or after depending on time scale
        ChunkedDate date = new ChunkedDate(2000, 01,01);
        ChunkedTime time = new ChunkedTime(11, 59, 10);
        TimeScale[] scales = {
            TAIScale.getInstance(), UTCScale.getInstance(),
            TTScale.getInstance(), TCGScale.getInstance()      
        };
        for (int i = 0; i < scales.length; ++i) {
            AbsoluteDate in = new AbsoluteDate(date, time, scales[i]);
            for (int j = 0; j < scales.length; ++j) {
                ChunksPair pair = in.getChunks(scales[j]);
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

    public void testHashcode() {
        AbsoluteDate d1 =
            new AbsoluteDate(new ChunkedDate(2006, 2, 25),
                             new ChunkedTime(17, 10, 34),
                             utc);
        AbsoluteDate d2 =
            new AbsoluteDate(new AbsoluteDate(new ChunkedDate(2006, 2, 25),
                                              new ChunkedTime(17, 10, 0),
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
        assertTrue(Double.isInfinite(AbsoluteDate.FUTURE_INFINITY.minus(AbsoluteDate.J2000_EPOCH)));
        assertTrue(Double.isInfinite(AbsoluteDate.FUTURE_INFINITY.minus(AbsoluteDate.PAST_INFINITY)));
        assertTrue(Double.isInfinite(AbsoluteDate.PAST_INFINITY.minus(AbsoluteDate.J2000_EPOCH)));
    }

    public void setUp() throws OrekitException {
        System.setProperty(IERSDirectoryCrawler.IERS_ROOT_DIRECTORY, "regular-data");
        utc = UTCScale.getInstance();
    }

    public static Test suite() {
        return new TestSuite(AbsoluteDateTest.class);
    }

    private TimeScale utc;

}
