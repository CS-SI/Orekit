/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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


import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.TimeZone;

import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.utils.Constants;

public class AbsoluteDateTest {

    @Test
    public void testStandardEpoch() {
        TimeScale tai = TimeScalesFactory.getTAI();
        TimeScale tt  = TimeScalesFactory.getTT();
        Assert.assertEquals(-210866760000000l, AbsoluteDate.JULIAN_EPOCH.toDate(tt).getTime());
        Assert.assertEquals(-3506716800000l,   AbsoluteDate.MODIFIED_JULIAN_EPOCH.toDate(tt).getTime());
        Assert.assertEquals(-631152000000l,    AbsoluteDate.FIFTIES_EPOCH.toDate(tt).getTime());
        Assert.assertEquals(-378691200000l,    AbsoluteDate.CCSDS_EPOCH.toDate(tai).getTime());
        Assert.assertEquals(935280019000l,     AbsoluteDate.GALILEO_EPOCH.toDate(tai).getTime());
        Assert.assertEquals(315964819000l,     AbsoluteDate.GPS_EPOCH.toDate(tai).getTime());
        Assert.assertEquals(315964819000l,     AbsoluteDate.QZSS_EPOCH.toDate(tai).getTime());
        Assert.assertEquals(1136073633000l,    AbsoluteDate.BEIDOU_EPOCH.toDate(tai).getTime());
        Assert.assertEquals(820443629000l,     AbsoluteDate.GLONASS_EPOCH.toDate(tai).getTime());
        Assert.assertEquals(935280019000l,     AbsoluteDate.IRNSS_EPOCH.toDate(tai).getTime());
        Assert.assertEquals(946728000000l,     AbsoluteDate.J2000_EPOCH.toDate(tt).getTime());
    }

    @Test
    public void testStandardEpochStrings() {
        Assert.assertEquals("-4712-01-01T12:00:00.000",
                     AbsoluteDate.JULIAN_EPOCH.toString(TimeScalesFactory.getTT()));
        Assert.assertEquals("1858-11-17T00:00:00.000",
                     AbsoluteDate.MODIFIED_JULIAN_EPOCH.toString(TimeScalesFactory.getTT()));
        Assert.assertEquals("1950-01-01T00:00:00.000",
                            AbsoluteDate.FIFTIES_EPOCH.toString(TimeScalesFactory.getTT()));
        Assert.assertEquals("1958-01-01T00:00:00.000",
                            AbsoluteDate.CCSDS_EPOCH.toString(TimeScalesFactory.getTAI()));
        Assert.assertEquals("1999-08-21T23:59:47.000",
                            AbsoluteDate.GALILEO_EPOCH.toString(TimeScalesFactory.getUTC()));
        Assert.assertEquals("1980-01-06T00:00:00.000",
                            AbsoluteDate.GPS_EPOCH.toString(TimeScalesFactory.getUTC()));
        Assert.assertEquals("1980-01-06T00:00:00.000",
                            AbsoluteDate.QZSS_EPOCH.toString(TimeScalesFactory.getUTC()));
        Assert.assertEquals("2006-01-01T00:00:00.000",
                            AbsoluteDate.BEIDOU_EPOCH.toString(TimeScalesFactory.getUTC()));
        Assert.assertEquals("1995-12-31T21:00:00.000",
                            AbsoluteDate.GLONASS_EPOCH.toString(TimeScalesFactory.getUTC()));
        Assert.assertEquals("1999-08-21T23:59:47.000",
                AbsoluteDate.IRNSS_EPOCH.toString(TimeScalesFactory.getUTC()));
        Assert.assertEquals("2000-01-01T12:00:00.000",
                     AbsoluteDate.J2000_EPOCH.toString(TimeScalesFactory.getTT()));
        Assert.assertEquals("1970-01-01T00:00:00.000",
                     AbsoluteDate.JAVA_EPOCH.toString(TimeScalesFactory.getUTC()));
    }

    @Test
    public void testJulianEpochRate() {

        for (int i = 0; i < 10; ++i) {
            AbsoluteDate j200i = AbsoluteDate.createJulianEpoch(2000.0 + i);
            AbsoluteDate j2000 = AbsoluteDate.J2000_EPOCH;
            double expected    = i * Constants.JULIAN_YEAR;
            Assert.assertEquals(expected, j200i.durationFrom(j2000), 4.0e-15 * expected);
        }

    }

    @Test
    public void testBesselianEpochRate() {

        for (int i = 0; i < 10; ++i) {
            AbsoluteDate b195i = AbsoluteDate.createBesselianEpoch(1950.0 + i);
            AbsoluteDate b1950 = AbsoluteDate.createBesselianEpoch(1950.0);
            double expected    = i * Constants.BESSELIAN_YEAR;
            Assert.assertEquals(expected, b195i.durationFrom(b1950), 4.0e-15 * expected);
        }

    }

    @Test
    public void testLieske() {

        // the following test values correspond to table 1 in the paper:
        // Precession Matrix Based on IAU (1976) System of Astronomical Constants,
        // Jay H. Lieske, Astronomy and Astrophysics, vol. 73, no. 3, Mar. 1979, p. 282-284
        // http://articles.adsabs.harvard.edu/cgi-bin/nph-iarticle_query?1979A%26A....73..282L&defaultprint=YES&filetype=.pdf

        // published table, with limited accuracy
        final double publishedEpsilon = 1.0e-6 * Constants.JULIAN_YEAR;
        checkEpochs(1899.999142, 1900.000000, publishedEpsilon);
        checkEpochs(1900.000000, 1900.000858, publishedEpsilon);
        checkEpochs(1950.000000, 1949.999790, publishedEpsilon);
        checkEpochs(1950.000210, 1950.000000, publishedEpsilon);
        checkEpochs(2000.000000, 1999.998722, publishedEpsilon);
        checkEpochs(2000.001278, 2000.000000, publishedEpsilon);

        // recomputed table, using directly Lieske formulas (i.e. *not* Orekit implementation) with high accuracy
        final double accurateEpsilon = 1.2e-13 * Constants.JULIAN_YEAR;
        checkEpochs(1899.99914161068724704, 1900.00000000000000000, accurateEpsilon);
        checkEpochs(1900.00000000000000000, 1900.00085837097878165, accurateEpsilon);
        checkEpochs(1950.00000000000000000, 1949.99979044229979466, accurateEpsilon);
        checkEpochs(1950.00020956217615449, 1950.00000000000000000, accurateEpsilon);
        checkEpochs(2000.00000000000000000, 1999.99872251362080766, accurateEpsilon);
        checkEpochs(2000.00127751366506194, 2000.00000000000000000, accurateEpsilon);

    }

    private void checkEpochs(final double besselianEpoch, final double julianEpoch, final double epsilon) {
        final AbsoluteDate b = AbsoluteDate.createBesselianEpoch(besselianEpoch);
        final AbsoluteDate j = AbsoluteDate.createJulianEpoch(julianEpoch);
        Assert.assertEquals(0.0, b.durationFrom(j), epsilon);
    }

    @Test
    public void testParse() {
        Assert.assertEquals(AbsoluteDate.MODIFIED_JULIAN_EPOCH,
                            new AbsoluteDate("1858-W46-3", TimeScalesFactory.getTT()));
        Assert.assertEquals(AbsoluteDate.JULIAN_EPOCH,
                            new AbsoluteDate("-4712-01-01T12:00:00.000", TimeScalesFactory.getTT()));
        Assert.assertEquals(AbsoluteDate.FIFTIES_EPOCH,
                            new AbsoluteDate("1950-01-01", TimeScalesFactory.getTT()));
        Assert.assertEquals(AbsoluteDate.CCSDS_EPOCH,
                            new AbsoluteDate("1958-001", TimeScalesFactory.getTAI()));
    }

    @Test
    public void testLocalTimeParsing() {
        TimeScale utc = TimeScalesFactory.getUTC();
        Assert.assertEquals(new AbsoluteDate("2011-12-31T23:00:00",       utc),
                            new AbsoluteDate("2012-01-01T03:30:00+04:30", utc));
        Assert.assertEquals(new AbsoluteDate("2011-12-31T23:00:00",       utc),
                            new AbsoluteDate("2012-01-01T03:30:00+0430",  utc));
        Assert.assertEquals(new AbsoluteDate("2011-12-31T23:30:00",       utc),
                            new AbsoluteDate("2012-01-01T03:30:00+04",    utc));
        Assert.assertEquals(new AbsoluteDate("2012-01-01T05:17:00",       utc),
                            new AbsoluteDate("2011-12-31T22:17:00-07:00", utc));
        Assert.assertEquals(new AbsoluteDate("2012-01-01T05:17:00",       utc),
                            new AbsoluteDate("2011-12-31T22:17:00-0700",  utc));
        Assert.assertEquals(new AbsoluteDate("2012-01-01T05:17:00",       utc),
                            new AbsoluteDate("2011-12-31T22:17:00-07",    utc));
    }

    @Test
    public void testTimeZoneDisplay() {
        final TimeScale utc = TimeScalesFactory.getUTC();
        final AbsoluteDate date = new AbsoluteDate("2000-01-01T01:01:01.000", utc);
        Assert.assertEquals("2000-01-01T01:01:01.000",       date.toString());
        Assert.assertEquals("2000-01-01T11:01:01.000+10:00", date.toString( 600));
        Assert.assertEquals("1999-12-31T23:01:01.000-02:00", date.toString(-120));

        // winter time, Europe is one hour ahead of UTC
        final TimeZone tz = TimeZone.getTimeZone("Europe/Paris");
        Assert.assertEquals("2001-01-22T11:30:00.000+01:00",
                            new AbsoluteDate("2001-01-22T10:30:00", utc).toString(tz));

        // summer time, Europe is two hours ahead of UTC
        Assert.assertEquals("2001-06-23T11:30:00.000+02:00",
                            new AbsoluteDate("2001-06-23T09:30:00", utc).toString(tz));

    }

    @Test
    public void testLocalTimeLeapSecond() throws IOException {

        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate beforeLeap = new AbsoluteDate("2012-06-30T23:59:59.8", utc);
        AbsoluteDate inLeap     = new AbsoluteDate("2012-06-30T23:59:60.5", utc);
        Assert.assertEquals(0.7, inLeap.durationFrom(beforeLeap), 1.0e-12);
        for (int minutesFromUTC = -1500; minutesFromUTC < 1500; ++minutesFromUTC) {
            DateTimeComponents dtcBeforeLeap = beforeLeap.getComponents(minutesFromUTC);
            DateTimeComponents dtcInsideLeap = inLeap.getComponents(minutesFromUTC);
            Assert.assertEquals(dtcBeforeLeap.getDate(), dtcInsideLeap.getDate());
            Assert.assertEquals(dtcBeforeLeap.getTime().getHour(), dtcInsideLeap.getTime().getHour());
            Assert.assertEquals(dtcBeforeLeap.getTime().getMinute(), dtcInsideLeap.getTime().getMinute());
            Assert.assertEquals(minutesFromUTC, dtcBeforeLeap.getTime().getMinutesFromUTC());
            Assert.assertEquals(minutesFromUTC, dtcInsideLeap.getTime().getMinutesFromUTC());
            Assert.assertEquals(59.8, dtcBeforeLeap.getTime().getSecond(), 1.0e-10);
            Assert.assertEquals(60.5, dtcInsideLeap.getTime().getSecond(), 1.0e-10);
        }

    }

    @Test
    public void testTimeZoneLeapSecond() {

        TimeScale utc = TimeScalesFactory.getUTC();
        final TimeZone tz = TimeZone.getTimeZone("Europe/Paris");
        AbsoluteDate localBeforeMidnight = new AbsoluteDate("2012-06-30T21:59:59.800", utc);
        Assert.assertEquals("2012-06-30T23:59:59.800+02:00",
                            localBeforeMidnight.toString(tz));
        Assert.assertEquals("2012-07-01T00:00:00.800+02:00",
                            localBeforeMidnight.shiftedBy(1.0).toString(tz));

        AbsoluteDate beforeLeap = new AbsoluteDate("2012-06-30T23:59:59.8", utc);
        AbsoluteDate inLeap     = new AbsoluteDate("2012-06-30T23:59:60.5", utc);
        Assert.assertEquals(0.7, inLeap.durationFrom(beforeLeap), 1.0e-12);
        Assert.assertEquals("2012-07-01T01:59:59.800+02:00", beforeLeap.toString(tz));
        Assert.assertEquals("2012-07-01T01:59:60.500+02:00", inLeap.toString(tz));

    }

    @Test
    public void testParseLeap() {
        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate beforeLeap = new AbsoluteDate("2012-06-30T23:59:59.8", utc);
        AbsoluteDate inLeap     = new AbsoluteDate("2012-06-30T23:59:60.5", utc);
        Assert.assertEquals(0.7, inLeap.durationFrom(beforeLeap), 1.0e-12);
        Assert.assertEquals("2012-06-30T23:59:60.500", inLeap.toString(utc));
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
    public void testMJDDate() {
        AbsoluteDate dateA = AbsoluteDate.createMJDDate(51544, 0.5 * Constants.JULIAN_DAY,
                                                             TimeScalesFactory.getTT());
        Assert.assertEquals(0.0, AbsoluteDate.J2000_EPOCH.durationFrom(dateA), 1.0e-15);
        AbsoluteDate dateB = AbsoluteDate.createMJDDate(53774, 0.0, TimeScalesFactory.getUTC());
        AbsoluteDate dateC = new AbsoluteDate("2006-02-08T00:00:00", TimeScalesFactory.getUTC());
        Assert.assertEquals(0.0, dateC.durationFrom(dateB), 1.0e-15);
    }

    @Test
    public void testJDDate() {
        final AbsoluteDate date = AbsoluteDate.createJDDate(2400000, 0.5 * Constants.JULIAN_DAY,
                                                            TimeScalesFactory.getTT());
        Assert.assertEquals(0.0, AbsoluteDate.MODIFIED_JULIAN_EPOCH.durationFrom(date), 1.0e-15);
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

    @SuppressWarnings("unlikely-arg-type")
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
    public void testComponents() {
        // this is NOT J2000.0,
        // it is either a few seconds before or after depending on time scale
        DateComponents date = new DateComponents(2000, 1, 1);
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

    @Test
    public void testMonth() {
        TimeScale utc = TimeScalesFactory.getUTC();
        Assert.assertEquals(new AbsoluteDate(2011, 2, 23, utc),
                            new AbsoluteDate(2011, Month.FEBRUARY, 23, utc));
        Assert.assertEquals(new AbsoluteDate(2011, 2, 23, 1, 2, 3.4, utc),
                            new AbsoluteDate(2011, Month.FEBRUARY, 23, 1, 2, 3.4, utc));
    }

    @Test
    public void testCCSDSUnsegmentedNoExtension() {

        AbsoluteDate reference = new AbsoluteDate("2002-05-23T12:34:56.789", utc);
        double lsb = FastMath.pow(2.0, -24);

        byte[] timeCCSDSEpoch = new byte[] { 0x53, 0x7F, 0x40, -0x70, -0x37, -0x05, -0x19 };
        for (int preamble = 0x00; preamble < 0x80; ++preamble) {
            if (preamble == 0x1F) {
                // using CCSDS reference epoch
                AbsoluteDate ccsds1 =
                    AbsoluteDate.parseCCSDSUnsegmentedTimeCode((byte) preamble, (byte) 0x0, timeCCSDSEpoch, null);
                Assert.assertEquals(0, ccsds1.durationFrom(reference), lsb / 2);
            } else {
                try {
                    AbsoluteDate.parseCCSDSUnsegmentedTimeCode((byte) preamble, (byte) 0x0, timeCCSDSEpoch, null);
                    Assert.fail("an exception should have been thrown");
                } catch (OrekitException iae) {
                    // expected
                }

            }
        }

        // missing epoch
        byte[] timeJ2000Epoch = new byte[] { 0x04, 0x7E, -0x0B, -0x10, -0x07, 0x16, -0x79 };
        try {
            AbsoluteDate.parseCCSDSUnsegmentedTimeCode((byte) 0x2F, (byte) 0x0, timeJ2000Epoch, null);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException iae) {
            // expected
        }

        // using J2000.0 epoch
        AbsoluteDate ccsds3 =
            AbsoluteDate.parseCCSDSUnsegmentedTimeCode((byte) 0x2F, (byte) 0x0, timeJ2000Epoch, AbsoluteDate.J2000_EPOCH);
        Assert.assertEquals(0, ccsds3.durationFrom(reference), lsb / 2);

    }

    @Test
    public void testCCSDSUnsegmentedWithExtendedPreamble() {

        AbsoluteDate reference = new AbsoluteDate("2095-03-03T22:02:45.789012345678901", utc);
        int leap = (int) FastMath.rint(utc.offsetFromTAI(reference));
        double lsb = FastMath.pow(2.0, -48);

        byte extendedPreamble = (byte) -0x80;
        byte identification   = (byte)  0x10;
        byte coarseLength1    = (byte)  0x0C; // four (3 + 1) bytes
        byte fineLength1      = (byte)  0x03; // 3 bytes
        byte coarseLength2    = (byte)  0x20; // 1 additional byte for coarse time
        byte fineLength2      = (byte)  0x0C; // 3 additional bytes for fine time
        byte[] timeCCSDSEpoch = new byte[] {
             0x01,  0x02,  0x03,  0x04,  (byte)(0x05 - leap), // 5 bytes for coarse time (seconds)
            -0x37, -0x04, -0x4A, -0x74, -0x2C, -0x3C          // 6 bytes for fine time (sub-seconds)
        };
        byte preamble1 = (byte) (extendedPreamble | identification | coarseLength1 | fineLength1);
        byte preamble2 = (byte) (coarseLength2 | fineLength2);
        AbsoluteDate ccsds1 =
                AbsoluteDate.parseCCSDSUnsegmentedTimeCode(preamble1, preamble2, timeCCSDSEpoch, null);
        Assert.assertEquals(0, ccsds1.durationFrom(reference), lsb / 2);

    }

    @Test
    public void testCCSDSDaySegmented() {

        AbsoluteDate reference = new AbsoluteDate("2002-05-23T12:34:56.789012345678", TimeScalesFactory.getUTC());
        double lsb = 1.0e-13;
        byte[] timeCCSDSEpoch = new byte[] { 0x3F, 0x55, 0x02, -0x4D, 0x2C, -0x6B, 0x00, -0x44, 0x61, 0x4E };

        for (int preamble = 0x00; preamble < 0x100; ++preamble) {
            if (preamble == 0x42) {
                // using CCSDS reference epoch
                AbsoluteDate ccsds1 =
                    AbsoluteDate.parseCCSDSDaySegmentedTimeCode((byte) preamble, timeCCSDSEpoch, null);
                Assert.assertEquals(0, ccsds1.durationFrom(reference), lsb / 2);
            } else {
                try {
                    AbsoluteDate.parseCCSDSDaySegmentedTimeCode((byte) preamble, timeCCSDSEpoch, null);
                    Assert.fail("an exception should have been thrown");
                } catch (OrekitException iae) {
                    // expected
                }

            }
        }

        // missing epoch
        byte[] timeJ2000Epoch = new byte[] { 0x03, 0x69, 0x02, -0x4D, 0x2C, -0x6B, 0x00, -0x44, 0x61, 0x4E };
        try {
            AbsoluteDate.parseCCSDSDaySegmentedTimeCode((byte) 0x4A, timeJ2000Epoch, null);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException iae) {
            // expected
        }

        // using J2000.0 epoch
        AbsoluteDate ccsds3 =
            AbsoluteDate.parseCCSDSDaySegmentedTimeCode((byte) 0x4A, timeJ2000Epoch, DateComponents.J2000_EPOCH);
        Assert.assertEquals(0, ccsds3.durationFrom(reference), lsb / 2);

        // limit to microsecond
        byte[] timeMicrosecond = new byte[] { 0x03, 0x69, 0x02, -0x4D, 0x2C, -0x6B, 0x00, 0x0C };
        AbsoluteDate ccsds4 =
            AbsoluteDate.parseCCSDSDaySegmentedTimeCode((byte) 0x49, timeMicrosecond, DateComponents.J2000_EPOCH);
        Assert.assertEquals(-0.345678e-6, ccsds4.durationFrom(reference), lsb / 2);

    }

    @Test
    public void testCCSDSCalendarSegmented() {

        AbsoluteDate reference = new AbsoluteDate("2002-05-23T12:34:56.789012345678", TimeScalesFactory.getUTC());
        double lsb = 1.0e-13;

        // month of year / day of month variation
        byte[] timeMonthDay = new byte[] { 0x07, -0x2E, 0x05, 0x17, 0x0C, 0x22, 0x38, 0x4E, 0x5A, 0x0C, 0x22, 0x38, 0x4E };
        for (int preamble = 0x00; preamble < 0x100; ++preamble) {
            if (preamble == 0x56) {
                AbsoluteDate ccsds1 =
                    AbsoluteDate.parseCCSDSCalendarSegmentedTimeCode((byte) preamble, timeMonthDay);
                Assert.assertEquals(0, ccsds1.durationFrom(reference), lsb / 2);
            } else {
                try {
                    AbsoluteDate.parseCCSDSCalendarSegmentedTimeCode((byte) preamble, timeMonthDay);
                    Assert.fail("an exception should have been thrown");
                } catch (OrekitException iae) {
                    // expected
                } catch (IllegalArgumentException iae) {
                    // should happen when preamble specifies day of year variation
                    // since there is no day 1303 (= 5 * 256 + 23) in any year ...
                    Assert.assertEquals(preamble & 0x08, 0x08);
                }

            }
        }

        // day of year variation
        byte[] timeDay = new byte[] { 0x07, -0x2E, 0x00, -0x71, 0x0C, 0x22, 0x38, 0x4E, 0x5A, 0x0C, 0x22, 0x38, 0x4E };
        for (int preamble = 0x00; preamble < 0x100; ++preamble) {
            if (preamble == 0x5E) {
                AbsoluteDate ccsds1 =
                    AbsoluteDate.parseCCSDSCalendarSegmentedTimeCode((byte) preamble, timeDay);
                Assert.assertEquals(0, ccsds1.durationFrom(reference), lsb / 2);
            } else {
                try {
                    AbsoluteDate.parseCCSDSCalendarSegmentedTimeCode((byte) preamble, timeDay);
                    Assert.fail("an exception should have been thrown");
                } catch (OrekitException iae) {
                    // expected
                } catch (IllegalArgumentException iae) {
                    // should happen when preamble specifies month of year / day of month variation
                    // since there is no month 0 in any year ...
                    Assert.assertEquals(preamble & 0x08, 0x00);
                }

            }
        }

        // limit to microsecond
        byte[] timeMicrosecond = new byte[] { 0x07, -0x2E, 0x00, -0x71, 0x0C, 0x22, 0x38, 0x4E, 0x5A, 0x0C };
        AbsoluteDate ccsds4 =
            AbsoluteDate.parseCCSDSCalendarSegmentedTimeCode((byte) 0x5B, timeMicrosecond);
        Assert.assertEquals(-0.345678e-6, ccsds4.durationFrom(reference), lsb / 2);

    }

    @Test(expected=IllegalArgumentException.class)
    public void testExpandedConstructors() {
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
        Assert.assertTrue(AbsoluteDate.JULIAN_EPOCH.compareTo(AbsoluteDate.PAST_INFINITY) > 0);
        Assert.assertTrue(AbsoluteDate.JULIAN_EPOCH.compareTo(AbsoluteDate.FUTURE_INFINITY) < 0);
        Assert.assertTrue(AbsoluteDate.J2000_EPOCH.compareTo(AbsoluteDate.PAST_INFINITY) > 0);
        Assert.assertTrue(AbsoluteDate.J2000_EPOCH.compareTo(AbsoluteDate.FUTURE_INFINITY) < 0);
        Assert.assertTrue(AbsoluteDate.PAST_INFINITY.compareTo(AbsoluteDate.PAST_INFINITY) == 0);
        Assert.assertTrue(AbsoluteDate.PAST_INFINITY.compareTo(AbsoluteDate.JULIAN_EPOCH) < 0);
        Assert.assertTrue(AbsoluteDate.PAST_INFINITY.compareTo(AbsoluteDate.J2000_EPOCH) < 0);
        Assert.assertTrue(AbsoluteDate.PAST_INFINITY.compareTo(AbsoluteDate.FUTURE_INFINITY) < 0);
        Assert.assertTrue(AbsoluteDate.FUTURE_INFINITY.compareTo(AbsoluteDate.JULIAN_EPOCH) > 0);
        Assert.assertTrue(AbsoluteDate.FUTURE_INFINITY.compareTo(AbsoluteDate.J2000_EPOCH) > 0);
        Assert.assertTrue(AbsoluteDate.FUTURE_INFINITY.compareTo(AbsoluteDate.PAST_INFINITY) > 0);
        Assert.assertTrue(AbsoluteDate.FUTURE_INFINITY.compareTo(AbsoluteDate.FUTURE_INFINITY) == 0);
        Assert.assertTrue(Double.isInfinite(AbsoluteDate.FUTURE_INFINITY.durationFrom(AbsoluteDate.J2000_EPOCH)));
        Assert.assertTrue(Double.isInfinite(AbsoluteDate.FUTURE_INFINITY.durationFrom(AbsoluteDate.PAST_INFINITY)));
        Assert.assertTrue(Double.isInfinite(AbsoluteDate.PAST_INFINITY.durationFrom(AbsoluteDate.J2000_EPOCH)));
        Assert.assertTrue(Double.isNaN(AbsoluteDate.FUTURE_INFINITY.durationFrom(AbsoluteDate.FUTURE_INFINITY)));
        Assert.assertTrue(Double.isNaN(AbsoluteDate.PAST_INFINITY.durationFrom(AbsoluteDate.PAST_INFINITY)));
        Assert.assertEquals("5881610-07-11T23:59:59.999",  AbsoluteDate.FUTURE_INFINITY.toString());
        Assert.assertEquals("-5877490-03-03T00:00:00.000", AbsoluteDate.PAST_INFINITY.toString());
        Assert.assertEquals(true, AbsoluteDate.FUTURE_INFINITY.equals(AbsoluteDate.FUTURE_INFINITY));
        Assert.assertEquals(true, AbsoluteDate.PAST_INFINITY.equals(AbsoluteDate.PAST_INFINITY));
        Assert.assertEquals(false, AbsoluteDate.PAST_INFINITY.equals(AbsoluteDate.FUTURE_INFINITY));
        Assert.assertEquals(false, AbsoluteDate.FUTURE_INFINITY.equals(AbsoluteDate.PAST_INFINITY));
    }

    @Test
    public void testCompareTo() {
        // check long time spans
        AbsoluteDate epoch =
                new AbsoluteDate(2000, 1, 1, 12, 0, 0, TimeScalesFactory.getTAI());
        Assert.assertTrue(AbsoluteDate.JULIAN_EPOCH.compareTo(epoch) < 0);
        Assert.assertTrue(epoch.compareTo(AbsoluteDate.JULIAN_EPOCH) > 0);
        // check short time spans
        AbsoluteDate d = epoch;
        double epsilon = 1.0 - FastMath.nextDown(1.0);
        Assert.assertTrue(d.compareTo(d.shiftedBy(epsilon)) < 0);
        Assert.assertTrue(d.compareTo(d.shiftedBy(0)) == 0);
        Assert.assertTrue(d.compareTo(d.shiftedBy(-epsilon)) > 0);
        // check date with negative offset
        d = epoch.shiftedBy(496891466)
                .shiftedBy(0.7320114066633323)
                .shiftedBy(-19730.732011406664);
        // offset is 0 in d1
        AbsoluteDate d1 = epoch.shiftedBy(496891466 - 19730);
        Assert.assertTrue(d.compareTo(d1) < 0);
        // decrement epoch, now offset is 0.999... in d1
        d1 = d1.shiftedBy(-1e-16);
        Assert.assertTrue("" + d.durationFrom(d1), d.compareTo(d1) < 0);
        // check large dates
        // these tests fail due to long overflow in durationFrom() Bug #584
        // d = new AbsoluteDate(epoch, Long.MAX_VALUE);
        // Assert.assertEquals(-1, epoch.compareTo(d));
        // Assert.assertTrue(d.compareTo(AbsoluteDate.FUTURE_INFINITY) < 0);
        // d = new AbsoluteDate(epoch, Long.MIN_VALUE);
        // Assert.assertTrue(epoch.compareTo(d) > 0);
        // Assert.assertTrue(d.compareTo(AbsoluteDate.PAST_INFINITY) > 0);
    }

    @Test
    public void testIsEqualTo() {
        Assert.assertTrue(present.isEqualTo(present));
        Assert.assertTrue(present.isEqualTo(presentToo));
        Assert.assertFalse(present.isEqualTo(past));
        Assert.assertFalse(present.isEqualTo(future));
    }

    @Test
    public void testIsCloseTo() {
        double tolerance = 10;
        TimeStamped closeToPresent = new AnyTimeStamped(present.shiftedBy(5));
        Assert.assertTrue(present.isCloseTo(present, tolerance));
        Assert.assertTrue(present.isCloseTo(presentToo, tolerance));
        Assert.assertTrue(present.isCloseTo(closeToPresent, tolerance));
        Assert.assertFalse(present.isCloseTo(past, tolerance));
        Assert.assertFalse(present.isCloseTo(future, tolerance));
    }

    @Test
    public void testIsBefore() {
        Assert.assertFalse(present.isBefore(past));
        Assert.assertFalse(present.isBefore(present));
        Assert.assertFalse(present.isBefore(presentToo));
        Assert.assertTrue(present.isBefore(future));
    }

    @Test
    public void testIsAfter() {
        Assert.assertTrue(present.isAfter(past));
        Assert.assertFalse(present.isAfter(present));
        Assert.assertFalse(present.isAfter(presentToo));
        Assert.assertFalse(present.isAfter(future));
    }

    @Test
    public void testIsBeforeOrEqualTo() {
        Assert.assertFalse(present.isBeforeOrEqualTo(past));
        Assert.assertTrue(present.isBeforeOrEqualTo(present));
        Assert.assertTrue(present.isBeforeOrEqualTo(presentToo));
        Assert.assertTrue(present.isBeforeOrEqualTo(future));
    }

    @Test
    public void testIsAfterOrEqualTo() {
        Assert.assertTrue(present.isAfterOrEqualTo(past));
        Assert.assertTrue(present.isAfterOrEqualTo(present));
        Assert.assertTrue(present.isAfterOrEqualTo(presentToo));
        Assert.assertFalse(present.isAfterOrEqualTo(future));
    }

    @Test
    public void testIsBetween() {
        Assert.assertTrue(present.isBetween(past, future));
        Assert.assertTrue(present.isBetween(future, past));
        Assert.assertFalse(past.getDate().isBetween(present, future));
        Assert.assertFalse(past.getDate().isBetween(future, present));
        Assert.assertFalse(future.getDate().isBetween(past, present));
        Assert.assertFalse(future.getDate().isBetween(present, past));
        Assert.assertFalse(present.isBetween(present, future));
        Assert.assertFalse(present.isBetween(past, present));
        Assert.assertFalse(present.isBetween(past, past));
        Assert.assertFalse(present.isBetween(present, present));
        Assert.assertFalse(present.isBetween(present, presentToo));
    }

    @Test
    public void testIsBetweenOrEqualTo() {
        Assert.assertTrue(present.isBetweenOrEqualTo(past, future));
        Assert.assertTrue(present.isBetweenOrEqualTo(future, past));
        Assert.assertFalse(past.getDate().isBetweenOrEqualTo(present, future));
        Assert.assertFalse(past.getDate().isBetweenOrEqualTo(future, present));
        Assert.assertFalse(future.getDate().isBetweenOrEqualTo(past, present));
        Assert.assertFalse(future.getDate().isBetweenOrEqualTo(present, past));
        Assert.assertTrue(present.isBetweenOrEqualTo(present, future));
        Assert.assertTrue(present.isBetweenOrEqualTo(past, present));
        Assert.assertFalse(present.isBetweenOrEqualTo(past, past));
        Assert.assertTrue(present.isBetweenOrEqualTo(present, present));
        Assert.assertTrue(present.isBetweenOrEqualTo(present, presentToo));
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

        final TimeScale tai = TimeScalesFactory.getTAI();
        final AbsoluteDate t0 = new AbsoluteDate(2010, 6, 21, 18, 42, 0.281, tai);

        // 0.1 is not representable exactly in double precision
        // we will accumulate error, between -0.5ULP and -3ULP at each iteration
        checkIteration(0.1, t0, 10000, 3.0, -1.19, 1.0e-4);

        // 0.125 is representable exactly in double precision
        // error will be null
        checkIteration(0.125, t0, 10000, 1.0e-15, 0.0, 1.0e-15);

    }

    private void checkIteration(final double step, final AbsoluteDate t0, final int nMax,
                                final double maxErrorFactor,
                                final double expectedMean, final double meanTolerance) {
        final double epsilon = FastMath.ulp(step);
        AbsoluteDate iteratedDate = t0;
        double mean = 0;
        for (int i = 1; i < nMax; ++i) {
            iteratedDate = iteratedDate.shiftedBy(step);
            AbsoluteDate directDate = t0.shiftedBy(i * step);
            final double error = iteratedDate.durationFrom(directDate);
            mean += error / (i * epsilon);
            Assert.assertEquals(0.0, iteratedDate.durationFrom(directDate), maxErrorFactor * i * epsilon);
        }
        mean /= nMax;
        Assert.assertEquals(expectedMean, mean, meanTolerance);
    }

    @Test
    public void testIssue142() {

        final AbsoluteDate epoch = AbsoluteDate.JAVA_EPOCH;
        final TimeScale utc = TimeScalesFactory.getUTC();

        Assert.assertEquals("1970-01-01T00:00:00.000", epoch.toString(utc));
        Assert.assertEquals(0.0, epoch.durationFrom(new AbsoluteDate(1970, 1, 1, utc)), 1.0e-15);
        Assert.assertEquals(8.000082,
                            epoch.durationFrom(new AbsoluteDate(DateComponents.JAVA_EPOCH, TimeScalesFactory.getTAI())),
                            1.0e-15);

        //Milliseconds - April 1, 2006, in UTC
        long msOffset = 1143849600000l;
        final AbsoluteDate ad = new AbsoluteDate(epoch, msOffset/1000, TimeScalesFactory.getUTC());
        Assert.assertEquals("2006-04-01T00:00:00.000", ad.toString(utc));

    }

    @Test
    public void testIssue148() {
        final TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate t0 = new AbsoluteDate(2012, 6, 30, 23, 59, 50.0, utc);
        DateTimeComponents components = t0.shiftedBy(11.0 - 200 * Precision.EPSILON).getComponents(utc);
        Assert.assertEquals(2012, components.getDate().getYear());
        Assert.assertEquals(   6, components.getDate().getMonth());
        Assert.assertEquals(  30, components.getDate().getDay());
        Assert.assertEquals(  23, components.getTime().getHour());
        Assert.assertEquals(  59, components.getTime().getMinute());
        Assert.assertEquals(  61 - 200 * Precision.EPSILON,
                            components.getTime().getSecond(), 1.0e-15);
    }

    @Test
    public void testIssue149() {
        final TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate t0 = new AbsoluteDate(2012, 6, 30, 23, 59, 59, utc);
        DateTimeComponents components = t0.shiftedBy(1.0 - Precision.EPSILON).getComponents(utc);
        Assert.assertEquals(2012, components.getDate().getYear());
        Assert.assertEquals(   6, components.getDate().getMonth());
        Assert.assertEquals(  30, components.getDate().getDay());
        Assert.assertEquals(  23, components.getTime().getHour());
        Assert.assertEquals(  59, components.getTime().getMinute());
        Assert.assertEquals(  60 - Precision.EPSILON,
                            components.getTime().getSecond(), 1.0e-15);
    }

    @Test
    public void testWrapAtMinuteEnd() {
        TimeScale tai = TimeScalesFactory.getTAI();
        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate date0 = new AbsoluteDate(DateComponents.J2000_EPOCH, TimeComponents.H12, tai);
        AbsoluteDate ref = date0.shiftedBy(496891466.0).shiftedBy(0.7320114066633323);
        AbsoluteDate date = ref.shiftedBy(33 * -597.9009700426262);
        DateTimeComponents dtc = date.getComponents(utc);
        Assert.assertEquals(2015, dtc.getDate().getYear());
        Assert.assertEquals(   9, dtc.getDate().getMonth());
        Assert.assertEquals(  30, dtc.getDate().getDay());
        Assert.assertEquals(   7, dtc.getTime().getHour());
        Assert.assertEquals(  54, dtc.getTime().getMinute());
        Assert.assertEquals(60 - 9.094947e-13, dtc.getTime().getSecond(), 1.0e-15);
        Assert.assertEquals("2015-09-30T07:55:00.000",
                            date.toString(utc));
        AbsoluteDate beforeMidnight = new AbsoluteDate(2008, 2, 29, 23, 59, 59.9994, utc);
        AbsoluteDate stillBeforeMidnight = beforeMidnight.shiftedBy(2.0e-4);
        Assert.assertEquals(59.9994, beforeMidnight.getComponents(utc).getTime().getSecond(), 1.0e-15);
        Assert.assertEquals(59.9996, stillBeforeMidnight.getComponents(utc).getTime().getSecond(), 1.0e-15);
        Assert.assertEquals("2008-02-29T23:59:59.999", beforeMidnight.toString(utc));
        Assert.assertEquals("2008-03-01T00:00:00.000", stillBeforeMidnight.toString(utc));
    }

    @Test
    public void testLastLeapOutput() {
        UTCScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate t = utc.getLastKnownLeapSecond();
        Assert.assertEquals("23:59:59.500", t.shiftedBy(-0.5).toString(utc).substring(11));
        Assert.assertEquals("23:59:60.000", t.shiftedBy( 0.0).toString(utc).substring(11));
        Assert.assertEquals("23:59:60.500", t.shiftedBy(+0.5).toString(utc).substring(11));
    }

    @Test
    public void testWrapBeforeLeap() {
        UTCScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate t = new AbsoluteDate("2015-06-30T23:59:59.999999", utc);
        Assert.assertEquals(2015,        t.getComponents(utc).getDate().getYear());
        Assert.assertEquals(   6,        t.getComponents(utc).getDate().getMonth());
        Assert.assertEquals(  30,        t.getComponents(utc).getDate().getDay());
        Assert.assertEquals(  23,        t.getComponents(utc).getTime().getHour());
        Assert.assertEquals(  59,        t.getComponents(utc).getTime().getMinute());
        Assert.assertEquals(  59.999999, t.getComponents(utc).getTime().getSecond(), 1.0e-6);
        Assert.assertEquals("2015-06-30T23:59:60.000", t.toString(utc));
        Assert.assertEquals("2015-07-01T02:59:60.000", t.toString(TimeScalesFactory.getGLONASS()));
    }

    @Test
    public void testMjdInLeap() {
        // inside a leap second
        AbsoluteDate date1 = new AbsoluteDate(2008, 12, 31, 23, 59, 60.5, utc);

        // check date to MJD conversion
        DateTimeComponents date1Components = date1.getComponents(utc);
        int mjd = date1Components.getDate().getMJD();
        double seconds = date1Components.getTime().getSecondsInUTCDay();
        Assert.assertEquals(54831, mjd);
        Assert.assertEquals(86400.5, seconds, 0);

        // check MJD to date conversion
        AbsoluteDate date2 = AbsoluteDate.createMJDDate(mjd, seconds, utc);
        Assert.assertEquals(date1, date2);

        // check we still detect seconds overflow
        try {
            AbsoluteDate.createMJDDate(mjd, seconds + 1.0, utc);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitIllegalArgumentException oiae) {
            Assert.assertEquals(OrekitMessages.OUT_OF_RANGE_SECONDS_NUMBER, oiae.getSpecifier());
            Assert.assertEquals(86401.5, ((Double) oiae.getParts()[0]).doubleValue(), 1.0e-10);
        }

    }

    @Test
    public void testIssueTimesStampAccuracy() {
        String testString = "2019-02-01T13:06:03.115";
        TimeScale timeScale=TimeScalesFactory.getUTC();

        DateTimeComponents expectedComponent = DateTimeComponents.parseDateTime(testString);
        AbsoluteDate expectedDate = new AbsoluteDate(expectedComponent, timeScale);

        ZonedDateTime actualComponent = LocalDateTime.from(DateTimeFormatter.ISO_DATE_TIME.parse(testString)).atZone(ZoneOffset.UTC);
        AbsoluteDate actualDate = new AbsoluteDate(Timestamp.from(actualComponent.toInstant()), timeScale);
        Assert.assertEquals(0.0, expectedDate.durationFrom(actualDate), 1.0e-15);

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
        utc = TimeScalesFactory.getUTC();
        present = new AbsoluteDate(new DateComponents(2000, 1, 1),
                                    new TimeComponents(12, 00, 00), utc);
        presentToo = new AnyTimeStamped(present.shiftedBy(0));
        past = new AnyTimeStamped(present.shiftedBy(-1000));
        future = new AnyTimeStamped(present.shiftedBy(1000));
    }

    private TimeScale utc;
    private AbsoluteDate present;
    private AnyTimeStamped past;
    private AnyTimeStamped presentToo;
    private AnyTimeStamped future;

    static class AnyTimeStamped implements TimeStamped {
        AbsoluteDate date;
        public AnyTimeStamped(AbsoluteDate date) {
            this.date = date;
        }

        @Override
        public AbsoluteDate getDate() {
            return date;
        }
    }

}
