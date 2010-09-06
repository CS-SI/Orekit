/* Copyright 2002-2010 CS Communication & Systèmes
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

import org.apache.commons.math.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;

public class AbsoluteDateTest {

    @Test
    public void testStandardEpoch() {
        TimeScale tai = TimeScalesFactory.getTAI();
        TimeScale tt  = TimeScalesFactory.getTT();
        Assert.assertEquals(-210866760000000l, AbsoluteDate.JULIAN_EPOCH.toDate(tt).getTime());
        Assert.assertEquals(-3506716800000l,   AbsoluteDate.MODIFIED_JULIAN_EPOCH.toDate(tt).getTime());
        Assert.assertEquals(-631152000000l,    AbsoluteDate.FIFTIES_EPOCH.toDate(tt).getTime());
        Assert.assertEquals(315964819000l,     AbsoluteDate.GPS_EPOCH.toDate(tai).getTime());
        Assert.assertEquals(946728000000l,     AbsoluteDate.J2000_EPOCH.toDate(tt).getTime());
    }

    @Test
    public void testStandardEpochStrings() throws OrekitException {
        Assert.assertEquals("-4712-01-01T12:00:00.000",
                     AbsoluteDate.JULIAN_EPOCH.toString(TimeScalesFactory.getTT()));
        Assert.assertEquals("1858-11-17T00:00:00.000",
                     AbsoluteDate.MODIFIED_JULIAN_EPOCH.toString(TimeScalesFactory.getTT()));
        Assert.assertEquals("1950-01-01T00:00:00.000",
                     AbsoluteDate.FIFTIES_EPOCH.toString(TimeScalesFactory.getTT()));
        Assert.assertEquals("1980-01-06T00:00:00.000",
                     AbsoluteDate.GPS_EPOCH.toString(TimeScalesFactory.getUTC()));
        Assert.assertEquals("2000-01-01T12:00:00.000",
                     AbsoluteDate.J2000_EPOCH.toString(TimeScalesFactory.getTT()));
        Assert.assertEquals("1970-01-01T00:00:00.000",
                     AbsoluteDate.JAVA_EPOCH.toString(TimeScalesFactory.getTT()));
    }

    @Test
    public void testParse() throws OrekitException {
        Assert.assertEquals(AbsoluteDate.MODIFIED_JULIAN_EPOCH,
                            new AbsoluteDate("1858-W46-3", TimeScalesFactory.getTT()));        
        Assert.assertEquals(AbsoluteDate.JULIAN_EPOCH,
                            new AbsoluteDate("-4712-01-01T12:00:00.000", TimeScalesFactory.getTT()));        
        Assert.assertEquals(AbsoluteDate.FIFTIES_EPOCH,
                            new AbsoluteDate("1950-01-01", TimeScalesFactory.getTT()));        
    }

    @Test
    public void testOutput() {
        TimeScale tt = TimeScalesFactory.getTT();
        Assert.assertEquals("1950-01-01T01:01:01.000",
                            AbsoluteDate.FIFTIES_EPOCH.shiftedBy(3661.0).toString(tt));
        Assert.assertEquals("2000-01-01T13:01:01.000",
                            AbsoluteDate.J2000_EPOCH.shiftedBy(3661.0).toString(tt));
    }

    @Test
    public void testJ2000() {
        Assert.assertEquals("2000-01-01T12:00:00.000",
                     AbsoluteDate.J2000_EPOCH.toString(TimeScalesFactory.getTT()));
        Assert.assertEquals("2000-01-01T11:59:27.816",
                     AbsoluteDate.J2000_EPOCH.toString(TimeScalesFactory.getTAI()));
        Assert.assertEquals("2000-01-01T11:58:55.816",
                     AbsoluteDate.J2000_EPOCH.toString(utc));
    }

    @Test
    public void testFraction() {
        AbsoluteDate d =
            new AbsoluteDate(new DateComponents(2000, 01, 01), new TimeComponents(11, 59, 27.816),
                             TimeScalesFactory.getTAI());
        Assert.assertEquals(0, d.durationFrom(AbsoluteDate.J2000_EPOCH), 1.0e-10);
    }

    @Test
    public void testScalesOffset() {
        AbsoluteDate date = new AbsoluteDate(new DateComponents(2006, 02, 24),
                                             new TimeComponents(15, 38, 00),
                                             utc);
        Assert.assertEquals(33,
                     date.timeScalesOffset(TimeScalesFactory.getTAI(), utc),
                     1.0e-10);
    }

    @Test
    public void testUTC() {
        AbsoluteDate date = new AbsoluteDate(new DateComponents(2002, 01, 01),
                                             new TimeComponents(00, 00, 01),
                                             utc);
        Assert.assertEquals("2002-01-01T00:00:01.000", date.toString());
    }

    @Test
    public void test1970() {
        AbsoluteDate date = new AbsoluteDate(new Date(0l), utc);
        Assert.assertEquals("1970-01-01T00:00:00.000", date.toString());
    }

    @Test
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
        Assert.assertEquals(13l, realGap - noLeapGap);

        // 14 seconds offset between GPS time and UTC in 2006
        noLeapGap = ((9712 * 24 + 16) * 60 + 31) * 60 + 17;
        realGap   = (long) date2.durationFrom(dateRef);
        Assert.assertEquals(14l, realGap - noLeapGap);

    }

    @Test
    public void testGpsDate() {
        AbsoluteDate date = AbsoluteDate.createGPSDate(1387, 318677000.0);
        AbsoluteDate ref  = new AbsoluteDate(new DateComponents(2006, 8, 9),
                                             new TimeComponents(16, 31, 03),
                                             utc);
        Assert.assertEquals(0, date.durationFrom(ref), 1.0e-15);
    }

    @Test
    public void testOffsets() {
        final TimeScale tai = TimeScalesFactory.getTAI();
        AbsoluteDate leapStartUTC = new AbsoluteDate(1976, 12, 31, 23, 59, 59, utc);
        AbsoluteDate leapEndUTC   = new AbsoluteDate(1977,  1,  1,  0,  0,  0, utc);
        AbsoluteDate leapStartTAI = new AbsoluteDate(1977,  1,  1,  0,  0, 14, tai);
        AbsoluteDate leapEndTAI   = new AbsoluteDate(1977,  1,  1,  0,  0, 16, tai);
        Assert.assertEquals(leapStartUTC, leapStartTAI);
        Assert.assertEquals(leapEndUTC, leapEndTAI);
        Assert.assertEquals(1, leapEndUTC.offsetFrom(leapStartUTC, utc), 1.0e-10);
        Assert.assertEquals(1, leapEndTAI.offsetFrom(leapStartTAI, utc), 1.0e-10);
        Assert.assertEquals(2, leapEndUTC.offsetFrom(leapStartUTC, tai), 1.0e-10);
        Assert.assertEquals(2, leapEndTAI.offsetFrom(leapStartTAI, tai), 1.0e-10);
        Assert.assertEquals(2, leapEndUTC.durationFrom(leapStartUTC),    1.0e-10);
        Assert.assertEquals(2, leapEndTAI.durationFrom(leapStartTAI),    1.0e-10);
    }

    @Test
    public void testBeforeAndAfterLeap() {
        final TimeScale tai = TimeScalesFactory.getTAI();
        AbsoluteDate leapStart = new AbsoluteDate(1977,  1,  1,  0,  0, 14, tai);
        AbsoluteDate leapEnd   = new AbsoluteDate(1977,  1,  1,  0,  0, 16, tai);
        for (int i = -10; i < 10; ++i) {
            final double dt = 1.1 * (2 * i - 1);
            AbsoluteDate d1 = leapStart.shiftedBy(dt);
            AbsoluteDate d2 = new AbsoluteDate(leapStart, dt, tai);
            AbsoluteDate d3 = new AbsoluteDate(leapStart, dt, utc);
            AbsoluteDate d4 = new AbsoluteDate(leapEnd,   dt, tai);
            AbsoluteDate d5 = new AbsoluteDate(leapEnd,   dt, utc);
            Assert.assertTrue(FastMath.abs(d1.durationFrom(d2)) < 1.0e-10);
            if (dt < 0) {
                Assert.assertTrue(FastMath.abs(d2.durationFrom(d3)) < 1.0e-10);
                Assert.assertTrue(d4.durationFrom(d5) > (1.0 - 1.0e-10));
            } else {
                Assert.assertTrue(d2.durationFrom(d3) < (-1.0 + 1.0e-10));
                Assert.assertTrue(FastMath.abs(d4.durationFrom(d5)) < 1.0e-10);
            }
        }
    }

    @Test
    public void testSymmetry() {
        final TimeScale tai = TimeScalesFactory.getTAI();
        AbsoluteDate leapStart = new AbsoluteDate(1977,  1,  1,  0,  0, 14, tai);
        for (int i = -10; i < 10; ++i) {
            final double dt = 1.1 * (2 * i - 1);
            Assert.assertEquals(dt, new AbsoluteDate(leapStart, dt, utc).offsetFrom(leapStart, utc), 1.0e-10);
            Assert.assertEquals(dt, new AbsoluteDate(leapStart, dt, tai).offsetFrom(leapStart, tai), 1.0e-10);
            Assert.assertEquals(dt, leapStart.shiftedBy(dt).durationFrom(leapStart), 1.0e-10);
        }
    }

    @Test
    public void testEquals() {
        AbsoluteDate d1 =
            new AbsoluteDate(new DateComponents(2006, 2, 25),
                             new TimeComponents(17, 10, 34),
                             utc);
        AbsoluteDate d2 = new AbsoluteDate(new DateComponents(2006, 2, 25),
                                           new TimeComponents(17, 10, 0),
                                           utc).shiftedBy(34);
        Assert.assertTrue(d1.equals(d2));
        Assert.assertFalse(d1.equals(this));
    }

    @Test
    public void testComponents() throws OrekitException {
        // this is NOT J2000.0,
        // it is either a few seconds before or after depending on time scale
        DateComponents date = new DateComponents(2000, 01,01);
        TimeComponents time = new TimeComponents(11, 59, 10);
        TimeScale[] scales = {
            TimeScalesFactory.getTAI(), TimeScalesFactory.getUTC(),
            TimeScalesFactory.getTT(), TimeScalesFactory.getTCG()      
        };
        for (int i = 0; i < scales.length; ++i) {
            AbsoluteDate in = new AbsoluteDate(date, time, scales[i]);
            for (int j = 0; j < scales.length; ++j) {
                DateTimeComponents pair = in.getComponents(scales[j]);
                if (i == j) {
                    Assert.assertEquals(date, pair.getDate());
                    Assert.assertEquals(time, pair.getTime());
                } else {
                    Assert.assertNotSame(date, pair.getDate());
                    Assert.assertNotSame(time, pair.getTime());
                }
            }
        }
    }

    @Test(expected=IllegalArgumentException.class)
    public void testExpandedConstructors() throws OrekitException {
        Assert.assertEquals(new AbsoluteDate(new DateComponents(2002, 05, 28),
                                      new TimeComponents(15, 30, 0),
                                      TimeScalesFactory.getUTC()),
                     new AbsoluteDate(2002, 05, 28, 15, 30, 0, TimeScalesFactory.getUTC()));
        Assert.assertEquals(new AbsoluteDate(new DateComponents(2002, 05, 28), TimeComponents.H00,
                                      TimeScalesFactory.getUTC()),
                     new AbsoluteDate(2002, 05, 28, TimeScalesFactory.getUTC()));
        new AbsoluteDate(2002, 05, 28, 25, 30, 0, TimeScalesFactory.getUTC());
    }

    @Test
    public void testHashcode() {
        AbsoluteDate d1 =
            new AbsoluteDate(new DateComponents(2006, 2, 25),
                             new TimeComponents(17, 10, 34),
                             utc);
        AbsoluteDate d2 = new AbsoluteDate(new DateComponents(2006, 2, 25),
                                           new TimeComponents(17, 10, 0),
                                           utc).shiftedBy(34);
        Assert.assertEquals(d1.hashCode(), d2.hashCode());
        Assert.assertTrue(d1.hashCode() != d1.shiftedBy(1.0e-3).hashCode());
    }

    @Test
    public void testInfinity() {
        Assert.assertTrue(AbsoluteDate.PAST_INFINITY.compareTo(AbsoluteDate.JULIAN_EPOCH) < 0);
        Assert.assertTrue(AbsoluteDate.PAST_INFINITY.compareTo(AbsoluteDate.J2000_EPOCH) < 0);
        Assert.assertTrue(AbsoluteDate.PAST_INFINITY.compareTo(AbsoluteDate.FUTURE_INFINITY) < 0);
        Assert.assertTrue(AbsoluteDate.FUTURE_INFINITY.compareTo(AbsoluteDate.JULIAN_EPOCH) > 0);
        Assert.assertTrue(AbsoluteDate.FUTURE_INFINITY.compareTo(AbsoluteDate.J2000_EPOCH) > 0);
        Assert.assertTrue(Double.isInfinite(AbsoluteDate.FUTURE_INFINITY.durationFrom(AbsoluteDate.J2000_EPOCH)));
        Assert.assertTrue(Double.isInfinite(AbsoluteDate.FUTURE_INFINITY.durationFrom(AbsoluteDate.PAST_INFINITY)));
        Assert.assertTrue(Double.isInfinite(AbsoluteDate.PAST_INFINITY.durationFrom(AbsoluteDate.J2000_EPOCH)));
    }

    @Test
    public void testAccuracy() {
        TimeScale tai = TimeScalesFactory.getTAI();
        double sec = 0.281;
        AbsoluteDate t = new AbsoluteDate(2010, 6, 21, 18, 42, sec, tai);
        double recomputedSec = t.getComponents(tai).getTime().getSecond();
        Assert.assertEquals(sec, recomputedSec, FastMath.ulp(sec));
    }

    @Test
    public void testIterationAccuracy() {
        TimeScale tai = TimeScalesFactory.getTAI();
        final AbsoluteDate t0 = new AbsoluteDate(2010, 6, 21, 18, 42, 0.281, tai);
        final double step = 0.1;
        AbsoluteDate iteratedDate = t0;
        for (int i = 1; i < 10000; ++i) {
            iteratedDate = iteratedDate.shiftedBy(step);
            AbsoluteDate directDate = t0.shiftedBy(i * step);
            Assert.assertEquals(0.0, iteratedDate.durationFrom(directDate), 1.0e-13);
        }
    }

   @Before
    public void setUp() throws OrekitException {
        Utils.setDataRoot("regular-data");
        utc = TimeScalesFactory.getUTC();
    }

    private TimeScale utc;

}
