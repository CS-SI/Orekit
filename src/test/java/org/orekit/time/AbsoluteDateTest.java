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

import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.OrekitMatchers;
import org.orekit.Utils;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.utils.Constants;

public class AbsoluteDateTest {

    @Test
    public void testStandardEpoch() {
        TimeScale tai = TimeScalesFactory.getTAI();
        TimeScale tt  = TimeScalesFactory.getTT();
        Assertions.assertEquals(-210866760000000l, AbsoluteDate.JULIAN_EPOCH.toDate(tt).getTime());
        Assertions.assertEquals(-3506716800000l,   AbsoluteDate.MODIFIED_JULIAN_EPOCH.toDate(tt).getTime());
        Assertions.assertEquals(-631152000000l,    AbsoluteDate.FIFTIES_EPOCH.toDate(tt).getTime());
        Assertions.assertEquals(-378691200000l,    AbsoluteDate.CCSDS_EPOCH.toDate(tai).getTime());
        Assertions.assertEquals(935280019000l,     AbsoluteDate.GALILEO_EPOCH.toDate(tai).getTime());
        Assertions.assertEquals(315964819000l,     AbsoluteDate.GPS_EPOCH.toDate(tai).getTime());
        Assertions.assertEquals(315964819000l,     AbsoluteDate.QZSS_EPOCH.toDate(tai).getTime());
        Assertions.assertEquals(1136073633000l,    AbsoluteDate.BEIDOU_EPOCH.toDate(tai).getTime());
        Assertions.assertEquals(820443629000l,     AbsoluteDate.GLONASS_EPOCH.toDate(tai).getTime());
        Assertions.assertEquals(935280019000l,     AbsoluteDate.IRNSS_EPOCH.toDate(tai).getTime());
        Assertions.assertEquals(946728000000l,     AbsoluteDate.J2000_EPOCH.toDate(tt).getTime());
    }

    @Test
    public void testStandardEpochStrings() {
        Assertions.assertEquals("-4712-01-01T12:00:00.000",
                     AbsoluteDate.JULIAN_EPOCH.toString(TimeScalesFactory.getTT()));
        Assertions.assertEquals("1858-11-17T00:00:00.000",
                     AbsoluteDate.MODIFIED_JULIAN_EPOCH.toString(TimeScalesFactory.getTT()));
        Assertions.assertEquals("1950-01-01T00:00:00.000",
                            AbsoluteDate.FIFTIES_EPOCH.toString(TimeScalesFactory.getTT()));
        Assertions.assertEquals("1958-01-01T00:00:00.000",
                            AbsoluteDate.CCSDS_EPOCH.toString(TimeScalesFactory.getTAI()));
        Assertions.assertEquals("1999-08-21T23:59:47.000",
                            AbsoluteDate.GALILEO_EPOCH.toString(TimeScalesFactory.getUTC()));
        Assertions.assertEquals("1980-01-06T00:00:00.000",
                            AbsoluteDate.GPS_EPOCH.toString(TimeScalesFactory.getUTC()));
        Assertions.assertEquals("1980-01-06T00:00:00.000",
                            AbsoluteDate.QZSS_EPOCH.toString(TimeScalesFactory.getUTC()));
        Assertions.assertEquals("2006-01-01T00:00:00.000",
                            AbsoluteDate.BEIDOU_EPOCH.toString(TimeScalesFactory.getUTC()));
        Assertions.assertEquals("1995-12-31T21:00:00.000",
                            AbsoluteDate.GLONASS_EPOCH.toString(TimeScalesFactory.getUTC()));
        Assertions.assertEquals("1999-08-21T23:59:47.000",
                AbsoluteDate.IRNSS_EPOCH.toString(TimeScalesFactory.getUTC()));
        Assertions.assertEquals("2000-01-01T12:00:00.000",
                     AbsoluteDate.J2000_EPOCH.toString(TimeScalesFactory.getTT()));
        Assertions.assertEquals("1970-01-01T00:00:00.000",
                     AbsoluteDate.JAVA_EPOCH.toString(TimeScalesFactory.getUTC()));
    }

    @Test
    public void testJulianEpochRate() {

        for (int i = 0; i < 10; ++i) {
            AbsoluteDate j200i = AbsoluteDate.createJulianEpoch(2000.0 + i);
            AbsoluteDate j2000 = AbsoluteDate.J2000_EPOCH;
            double expected    = i * Constants.JULIAN_YEAR;
            Assertions.assertEquals(expected, j200i.durationFrom(j2000), 4.0e-15 * expected);
        }

    }

    @Test
    public void testBesselianEpochRate() {

        for (int i = 0; i < 10; ++i) {
            AbsoluteDate b195i = AbsoluteDate.createBesselianEpoch(1950.0 + i);
            AbsoluteDate b1950 = AbsoluteDate.createBesselianEpoch(1950.0);
            double expected    = i * Constants.BESSELIAN_YEAR;
            Assertions.assertEquals(expected, b195i.durationFrom(b1950), 4.0e-15 * expected);
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
        Assertions.assertEquals(0.0, b.durationFrom(j), epsilon);
    }

    @Test
    public void testParse() {
        Assertions.assertEquals(AbsoluteDate.MODIFIED_JULIAN_EPOCH,
                            new AbsoluteDate("1858-W46-3", TimeScalesFactory.getTT()));
        Assertions.assertEquals(AbsoluteDate.JULIAN_EPOCH,
                            new AbsoluteDate("-4712-01-01T12:00:00.000", TimeScalesFactory.getTT()));
        Assertions.assertEquals(AbsoluteDate.FIFTIES_EPOCH,
                            new AbsoluteDate("1950-01-01", TimeScalesFactory.getTT()));
        Assertions.assertEquals(AbsoluteDate.CCSDS_EPOCH,
                            new AbsoluteDate("1958-001", TimeScalesFactory.getTAI()));
    }

    @Test
    public void testLocalTimeParsing() {
        TimeScale utc = TimeScalesFactory.getUTC();
        Assertions.assertEquals(new AbsoluteDate("2011-12-31T23:00:00",       utc),
                            new AbsoluteDate("2012-01-01T03:30:00+04:30", utc));
        Assertions.assertEquals(new AbsoluteDate("2011-12-31T23:00:00",       utc),
                            new AbsoluteDate("2012-01-01T03:30:00+0430",  utc));
        Assertions.assertEquals(new AbsoluteDate("2011-12-31T23:30:00",       utc),
                            new AbsoluteDate("2012-01-01T03:30:00+04",    utc));
        Assertions.assertEquals(new AbsoluteDate("2012-01-01T05:17:00",       utc),
                            new AbsoluteDate("2011-12-31T22:17:00-07:00", utc));
        Assertions.assertEquals(new AbsoluteDate("2012-01-01T05:17:00",       utc),
                            new AbsoluteDate("2011-12-31T22:17:00-0700",  utc));
        Assertions.assertEquals(new AbsoluteDate("2012-01-01T05:17:00",       utc),
                            new AbsoluteDate("2011-12-31T22:17:00-07",    utc));
    }

    @Test
    public void testTimeZoneDisplay() {
        final TimeScale utc = TimeScalesFactory.getUTC();
        final AbsoluteDate date = new AbsoluteDate("2000-01-01T01:01:01.000", utc);
        Assertions.assertEquals("2000-01-01T01:01:01.000Z",      date.toString());
        Assertions.assertEquals("2000-01-01T11:01:01.000+10:00", date.toString( 600));
        Assertions.assertEquals("1999-12-31T23:01:01.000-02:00", date.toString(-120));
        Assertions.assertEquals("2000-01-01T01:01:01.000+00:00", date.toString(0));

        // winter time, Europe is one hour ahead of UTC
        TimeZone tz = TimeZone.getTimeZone("Europe/Paris");
        Assertions.assertEquals("2001-01-22T11:30:00.000+01:00",
                            new AbsoluteDate("2001-01-22T10:30:00", utc).toString(tz));

        // summer time, Europe is two hours ahead of UTC
        Assertions.assertEquals("2001-06-23T11:30:00.000+02:00",
                            new AbsoluteDate("2001-06-23T09:30:00", utc).toString(tz));

        // check with UTC
        tz = TimeZone.getTimeZone("UTC");
        Assertions.assertEquals("2001-06-23T09:30:00.000+00:00",
                new AbsoluteDate("2001-06-23T09:30:00", utc).toString(tz));

    }

    @Test
    public void testLocalTimeLeapSecond() throws IOException {

        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate beforeLeap = new AbsoluteDate("2012-06-30T23:59:59.8", utc);
        AbsoluteDate inLeap     = new AbsoluteDate("2012-06-30T23:59:60.5", utc);
        Assertions.assertEquals(0.7, inLeap.durationFrom(beforeLeap), 1.0e-12);
        for (int minutesFromUTC = -1500; minutesFromUTC < 1500; ++minutesFromUTC) {
            DateTimeComponents dtcBeforeLeap = beforeLeap.getComponents(minutesFromUTC);
            DateTimeComponents dtcInsideLeap = inLeap.getComponents(minutesFromUTC);
            Assertions.assertEquals(dtcBeforeLeap.getDate(), dtcInsideLeap.getDate());
            Assertions.assertEquals(dtcBeforeLeap.getTime().getHour(), dtcInsideLeap.getTime().getHour());
            Assertions.assertEquals(dtcBeforeLeap.getTime().getMinute(), dtcInsideLeap.getTime().getMinute());
            Assertions.assertEquals(minutesFromUTC, dtcBeforeLeap.getTime().getMinutesFromUTC());
            Assertions.assertEquals(minutesFromUTC, dtcInsideLeap.getTime().getMinutesFromUTC());
            Assertions.assertEquals(59.8, dtcBeforeLeap.getTime().getSecond(), 1.0e-10);
            Assertions.assertEquals(60.5, dtcInsideLeap.getTime().getSecond(), 1.0e-10);
        }

    }

    @Test
    public void testTimeZoneLeapSecond() {

        TimeScale utc = TimeScalesFactory.getUTC();
        final TimeZone tz = TimeZone.getTimeZone("Europe/Paris");
        AbsoluteDate localBeforeMidnight = new AbsoluteDate("2012-06-30T21:59:59.800", utc);
        Assertions.assertEquals("2012-06-30T23:59:59.800+02:00",
                            localBeforeMidnight.toString(tz));
        Assertions.assertEquals("2012-07-01T00:00:00.800+02:00",
                            localBeforeMidnight.shiftedBy(1.0).toString(tz));

        AbsoluteDate beforeLeap = new AbsoluteDate("2012-06-30T23:59:59.8", utc);
        AbsoluteDate inLeap     = new AbsoluteDate("2012-06-30T23:59:60.5", utc);
        Assertions.assertEquals(0.7, inLeap.durationFrom(beforeLeap), 1.0e-12);
        Assertions.assertEquals("2012-07-01T01:59:59.800+02:00", beforeLeap.toString(tz));
        Assertions.assertEquals("2012-07-01T01:59:60.500+02:00", inLeap.toString(tz));

    }

    @Test
    public void testParseLeap() {
        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate beforeLeap = new AbsoluteDate("2012-06-30T23:59:59.8", utc);
        AbsoluteDate inLeap     = new AbsoluteDate("2012-06-30T23:59:60.5", utc);
        Assertions.assertEquals(0.7, inLeap.durationFrom(beforeLeap), 1.0e-12);
        Assertions.assertEquals("2012-06-30T23:59:60.500", inLeap.toString(utc));
    }

    @Test
    public void testOutput() {
        TimeScale tt = TimeScalesFactory.getTT();
        Assertions.assertEquals("1950-01-01T01:01:01.000",
                            AbsoluteDate.FIFTIES_EPOCH.shiftedBy(3661.0).toString(tt));
        Assertions.assertEquals("2000-01-01T13:01:01.000",
                            AbsoluteDate.J2000_EPOCH.shiftedBy(3661.0).toString(tt));
    }

    @Test
    public void testJ2000() {
        Assertions.assertEquals("2000-01-01T12:00:00.000",
                     AbsoluteDate.J2000_EPOCH.toString(TimeScalesFactory.getTT()));
        Assertions.assertEquals("2000-01-01T11:59:27.816",
                     AbsoluteDate.J2000_EPOCH.toString(TimeScalesFactory.getTAI()));
        Assertions.assertEquals("2000-01-01T11:58:55.816",
                     AbsoluteDate.J2000_EPOCH.toString(utc));
    }

    @Test
    public void testFraction() {
        AbsoluteDate d =
            new AbsoluteDate(new DateComponents(2000, 01, 01), new TimeComponents(11, 59, 27.816),
                             TimeScalesFactory.getTAI());
        Assertions.assertEquals(0, d.durationFrom(AbsoluteDate.J2000_EPOCH), 1.0e-10);
    }

    @Test
    public void testScalesOffset() {
        AbsoluteDate date = new AbsoluteDate(new DateComponents(2006, 02, 24),
                                             new TimeComponents(15, 38, 00),
                                             utc);
        Assertions.assertEquals(33,
                     date.timeScalesOffset(TimeScalesFactory.getTAI(), utc),
                     1.0e-10);
    }

    @Test
    public void testUTC() {
        AbsoluteDate date = new AbsoluteDate(new DateComponents(2002, 01, 01),
                                             new TimeComponents(00, 00, 01),
                                             utc);
        Assertions.assertEquals("2002-01-01T00:00:01.000Z", date.toString());
    }

    @Test
    public void test1970() {
        AbsoluteDate date = new AbsoluteDate(new Date(0l), utc);
        Assertions.assertEquals("1970-01-01T00:00:00.000Z", date.toString());
    }

    @Test
    public void test1970Instant() {
        Assertions.assertEquals("1970-01-01T00:00:00.000Z", new AbsoluteDate(Instant.EPOCH, utc).toString());
        Assertions.assertEquals("1970-01-01T00:00:00.000Z", new AbsoluteDate(Instant.ofEpochMilli(0l), utc).toString());
    }

    @Test
    public void testInstantAccuracy() {
        Assertions.assertEquals("1970-01-02T00:16:40.123456789Z", new AbsoluteDate(Instant.ofEpochSecond(87400, 123456789), utc).toString());
        Assertions.assertEquals("1970-01-07T00:10:00.123456789Z", new AbsoluteDate(Instant.ofEpochSecond(519000, 123456789), utc).toString());
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
        Assertions.assertEquals(13l, realGap - noLeapGap);

        // 14 seconds offset between GPS time and UTC in 2006
        noLeapGap = ((9712 * 24 + 16) * 60 + 31) * 60 + 17;
        realGap   = (long) date2.durationFrom(dateRef);
        Assertions.assertEquals(14l, realGap - noLeapGap);

    }

    @Test
    public void testMJDDate() {
        AbsoluteDate dateA = AbsoluteDate.createMJDDate(51544, 0.5 * Constants.JULIAN_DAY,
                                                             TimeScalesFactory.getTT());
        Assertions.assertEquals(0.0, AbsoluteDate.J2000_EPOCH.durationFrom(dateA), 1.0e-15);
        AbsoluteDate dateB = AbsoluteDate.createMJDDate(53774, 0.0, TimeScalesFactory.getUTC());
        AbsoluteDate dateC = new AbsoluteDate("2006-02-08T00:00:00", TimeScalesFactory.getUTC());
        Assertions.assertEquals(0.0, dateC.durationFrom(dateB), 1.0e-15);
    }

    @Test
    public void testJDDate() {
        final AbsoluteDate date = AbsoluteDate.createJDDate(2400000, 0.5 * Constants.JULIAN_DAY,
                                                            TimeScalesFactory.getTT());
        Assertions.assertEquals(0.0, AbsoluteDate.MODIFIED_JULIAN_EPOCH.durationFrom(date), 1.0e-15);
    }

    @Test
    public void testOffsets() {
        final TimeScale tai = TimeScalesFactory.getTAI();
        AbsoluteDate leapStartUTC = new AbsoluteDate(1976, 12, 31, 23, 59, 59, utc);
        AbsoluteDate leapEndUTC   = new AbsoluteDate(1977,  1,  1,  0,  0,  0, utc);
        AbsoluteDate leapStartTAI = new AbsoluteDate(1977,  1,  1,  0,  0, 14, tai);
        AbsoluteDate leapEndTAI   = new AbsoluteDate(1977,  1,  1,  0,  0, 16, tai);
        Assertions.assertEquals(leapStartUTC, leapStartTAI);
        Assertions.assertEquals(leapEndUTC, leapEndTAI);
        Assertions.assertEquals(1, leapEndUTC.offsetFrom(leapStartUTC, utc), 1.0e-10);
        Assertions.assertEquals(1, leapEndTAI.offsetFrom(leapStartTAI, utc), 1.0e-10);
        Assertions.assertEquals(2, leapEndUTC.offsetFrom(leapStartUTC, tai), 1.0e-10);
        Assertions.assertEquals(2, leapEndTAI.offsetFrom(leapStartTAI, tai), 1.0e-10);
        Assertions.assertEquals(2, leapEndUTC.durationFrom(leapStartUTC),    1.0e-10);
        Assertions.assertEquals(2, leapEndTAI.durationFrom(leapStartTAI),    1.0e-10);
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
            Assertions.assertTrue(FastMath.abs(d1.durationFrom(d2)) < 1.0e-10);
            if (dt < 0) {
                Assertions.assertTrue(FastMath.abs(d2.durationFrom(d3)) < 1.0e-10);
                Assertions.assertTrue(d4.durationFrom(d5) > (1.0 - 1.0e-10));
            } else {
                Assertions.assertTrue(d2.durationFrom(d3) < (-1.0 + 1.0e-10));
                Assertions.assertTrue(FastMath.abs(d4.durationFrom(d5)) < 1.0e-10);
            }
        }
    }

    @Test
    public void testSymmetry() {
        final TimeScale tai = TimeScalesFactory.getTAI();
        AbsoluteDate leapStart = new AbsoluteDate(1977,  1,  1,  0,  0, 14, tai);
        for (int i = -10; i < 10; ++i) {
            final double dt = 1.1 * (2 * i - 1);
            Assertions.assertEquals(dt, new AbsoluteDate(leapStart, dt, utc).offsetFrom(leapStart, utc), 1.0e-10);
            Assertions.assertEquals(dt, new AbsoluteDate(leapStart, dt, tai).offsetFrom(leapStart, tai), 1.0e-10);
            Assertions.assertEquals(dt, leapStart.shiftedBy(dt).durationFrom(leapStart), 1.0e-10);
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
        Assertions.assertTrue(d1.equals(d2));
        Assertions.assertFalse(d1.equals(this));
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
                    Assertions.assertEquals(date, pair.getDate());
                    Assertions.assertEquals(time, pair.getTime());
                } else {
                    Assertions.assertNotSame(date, pair.getDate());
                    Assertions.assertNotSame(time, pair.getTime());
                }
            }
        }
    }

    @Test
    public void testMonth() {
        TimeScale utc = TimeScalesFactory.getUTC();
        Assertions.assertEquals(new AbsoluteDate(2011, 2, 23, utc),
                            new AbsoluteDate(2011, Month.FEBRUARY, 23, utc));
        Assertions.assertEquals(new AbsoluteDate(2011, 2, 23, 1, 2, 3.4, utc),
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
                Assertions.assertEquals(0, ccsds1.durationFrom(reference), lsb / 2);
            } else {
                try {
                    AbsoluteDate.parseCCSDSUnsegmentedTimeCode((byte) preamble, (byte) 0x0, timeCCSDSEpoch, null);
                    Assertions.fail("an exception should have been thrown");
                } catch (OrekitException iae) {
                    // expected
                }

            }
        }

        // missing epoch
        byte[] timeJ2000Epoch = new byte[] { 0x04, 0x7E, -0x0B, -0x10, -0x07, 0x16, -0x79 };
        try {
            AbsoluteDate.parseCCSDSUnsegmentedTimeCode((byte) 0x2F, (byte) 0x0, timeJ2000Epoch, null);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException iae) {
            // expected
        }

        // using J2000.0 epoch
        AbsoluteDate ccsds3 =
            AbsoluteDate.parseCCSDSUnsegmentedTimeCode((byte) 0x2F, (byte) 0x0, timeJ2000Epoch, AbsoluteDate.J2000_EPOCH);
        Assertions.assertEquals(0, ccsds3.durationFrom(reference), lsb / 2);

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
        Assertions.assertEquals(0, ccsds1.durationFrom(reference), lsb / 2);

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
                Assertions.assertEquals(0, ccsds1.durationFrom(reference), lsb / 2);
            } else {
                try {
                    AbsoluteDate.parseCCSDSDaySegmentedTimeCode((byte) preamble, timeCCSDSEpoch, null);
                    Assertions.fail("an exception should have been thrown");
                } catch (OrekitException iae) {
                    // expected
                }

            }
        }

        // missing epoch
        byte[] timeJ2000Epoch = new byte[] { 0x03, 0x69, 0x02, -0x4D, 0x2C, -0x6B, 0x00, -0x44, 0x61, 0x4E };
        try {
            AbsoluteDate.parseCCSDSDaySegmentedTimeCode((byte) 0x4A, timeJ2000Epoch, null);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException iae) {
            // expected
        }

        // using J2000.0 epoch
        AbsoluteDate ccsds3 =
            AbsoluteDate.parseCCSDSDaySegmentedTimeCode((byte) 0x4A, timeJ2000Epoch, DateComponents.J2000_EPOCH);
        Assertions.assertEquals(0, ccsds3.durationFrom(reference), lsb / 2);

        // limit to microsecond
        byte[] timeMicrosecond = new byte[] { 0x03, 0x69, 0x02, -0x4D, 0x2C, -0x6B, 0x00, 0x0C };
        AbsoluteDate ccsds4 =
            AbsoluteDate.parseCCSDSDaySegmentedTimeCode((byte) 0x49, timeMicrosecond, DateComponents.J2000_EPOCH);
        Assertions.assertEquals(-0.345678e-6, ccsds4.durationFrom(reference), lsb / 2);

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
                Assertions.assertEquals(0, ccsds1.durationFrom(reference), lsb / 2);
            } else {
                try {
                    AbsoluteDate.parseCCSDSCalendarSegmentedTimeCode((byte) preamble, timeMonthDay);
                    Assertions.fail("an exception should have been thrown");
                } catch (OrekitException iae) {
                    // expected
                } catch (IllegalArgumentException iae) {
                    // should happen when preamble specifies day of year variation
                    // since there is no day 1303 (= 5 * 256 + 23) in any year ...
                    Assertions.assertEquals(preamble & 0x08, 0x08);
                }

            }
        }

        // day of year variation
        byte[] timeDay = new byte[] { 0x07, -0x2E, 0x00, -0x71, 0x0C, 0x22, 0x38, 0x4E, 0x5A, 0x0C, 0x22, 0x38, 0x4E };
        for (int preamble = 0x00; preamble < 0x100; ++preamble) {
            if (preamble == 0x5E) {
                AbsoluteDate ccsds1 =
                    AbsoluteDate.parseCCSDSCalendarSegmentedTimeCode((byte) preamble, timeDay);
                Assertions.assertEquals(0, ccsds1.durationFrom(reference), lsb / 2);
            } else {
                try {
                    AbsoluteDate.parseCCSDSCalendarSegmentedTimeCode((byte) preamble, timeDay);
                    Assertions.fail("an exception should have been thrown");
                } catch (OrekitException iae) {
                    // expected
                } catch (IllegalArgumentException iae) {
                    // should happen when preamble specifies month of year / day of month variation
                    // since there is no month 0 in any year ...
                    Assertions.assertEquals(preamble & 0x08, 0x00);
                }

            }
        }

        // limit to microsecond
        byte[] timeMicrosecond = new byte[] { 0x07, -0x2E, 0x00, -0x71, 0x0C, 0x22, 0x38, 0x4E, 0x5A, 0x0C };
        AbsoluteDate ccsds4 =
            AbsoluteDate.parseCCSDSCalendarSegmentedTimeCode((byte) 0x5B, timeMicrosecond);
        Assertions.assertEquals(-0.345678e-6, ccsds4.durationFrom(reference), lsb / 2);

    }

    @Test
    public void testExpandedConstructors() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Assertions.assertEquals(new AbsoluteDate(new DateComponents(2002, 05, 28),
                            new TimeComponents(15, 30, 0),
                            TimeScalesFactory.getUTC()),
                    new AbsoluteDate(2002, 05, 28, 15, 30, 0, TimeScalesFactory.getUTC()));
            Assertions.assertEquals(new AbsoluteDate(new DateComponents(2002, 05, 28), TimeComponents.H00,
                            TimeScalesFactory.getUTC()),
                    new AbsoluteDate(2002, 05, 28, TimeScalesFactory.getUTC()));
            new AbsoluteDate(2002, 05, 28, 25, 30, 0, TimeScalesFactory.getUTC());
        });
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
        Assertions.assertEquals(d1.hashCode(), d2.hashCode());
        Assertions.assertTrue(d1.hashCode() != d1.shiftedBy(1.0e-3).hashCode());
    }

    @Test
    public void testInfinity() {
        Assertions.assertTrue(AbsoluteDate.JULIAN_EPOCH.compareTo(AbsoluteDate.PAST_INFINITY) > 0);
        Assertions.assertTrue(AbsoluteDate.JULIAN_EPOCH.compareTo(AbsoluteDate.FUTURE_INFINITY) < 0);
        Assertions.assertTrue(AbsoluteDate.J2000_EPOCH.compareTo(AbsoluteDate.PAST_INFINITY) > 0);
        Assertions.assertTrue(AbsoluteDate.J2000_EPOCH.compareTo(AbsoluteDate.FUTURE_INFINITY) < 0);
        Assertions.assertTrue(AbsoluteDate.PAST_INFINITY.compareTo(AbsoluteDate.PAST_INFINITY) == 0);
        Assertions.assertTrue(AbsoluteDate.PAST_INFINITY.compareTo(AbsoluteDate.JULIAN_EPOCH) < 0);
        Assertions.assertTrue(AbsoluteDate.PAST_INFINITY.compareTo(AbsoluteDate.J2000_EPOCH) < 0);
        Assertions.assertTrue(AbsoluteDate.PAST_INFINITY.compareTo(AbsoluteDate.FUTURE_INFINITY) < 0);
        Assertions.assertTrue(AbsoluteDate.FUTURE_INFINITY.compareTo(AbsoluteDate.JULIAN_EPOCH) > 0);
        Assertions.assertTrue(AbsoluteDate.FUTURE_INFINITY.compareTo(AbsoluteDate.J2000_EPOCH) > 0);
        Assertions.assertTrue(AbsoluteDate.FUTURE_INFINITY.compareTo(AbsoluteDate.PAST_INFINITY) > 0);
        Assertions.assertTrue(AbsoluteDate.FUTURE_INFINITY.compareTo(AbsoluteDate.FUTURE_INFINITY) == 0);
        Assertions.assertTrue(Double.isInfinite(AbsoluteDate.FUTURE_INFINITY.durationFrom(AbsoluteDate.J2000_EPOCH)));
        Assertions.assertTrue(Double.isInfinite(AbsoluteDate.FUTURE_INFINITY.durationFrom(AbsoluteDate.PAST_INFINITY)));
        Assertions.assertTrue(Double.isInfinite(AbsoluteDate.PAST_INFINITY.durationFrom(AbsoluteDate.J2000_EPOCH)));
        Assertions.assertTrue(Double.isNaN(AbsoluteDate.FUTURE_INFINITY.durationFrom(AbsoluteDate.FUTURE_INFINITY)));
        Assertions.assertTrue(Double.isNaN(AbsoluteDate.PAST_INFINITY.durationFrom(AbsoluteDate.PAST_INFINITY)));
        Assertions.assertEquals("5881610-07-11T23:59:59.999Z",  AbsoluteDate.FUTURE_INFINITY.toString());
        Assertions.assertEquals("-5877490-03-03T00:00:00.000Z", AbsoluteDate.PAST_INFINITY.toString());
        Assertions.assertEquals(true, AbsoluteDate.FUTURE_INFINITY.equals(AbsoluteDate.FUTURE_INFINITY));
        Assertions.assertEquals(true, AbsoluteDate.PAST_INFINITY.equals(AbsoluteDate.PAST_INFINITY));
        Assertions.assertEquals(false, AbsoluteDate.PAST_INFINITY.equals(AbsoluteDate.FUTURE_INFINITY));
        Assertions.assertEquals(false, AbsoluteDate.FUTURE_INFINITY.equals(AbsoluteDate.PAST_INFINITY));

        Assertions.assertTrue(AbsoluteDate.J2000_EPOCH.durationFrom(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(Double.NEGATIVE_INFINITY))
                          == Double.POSITIVE_INFINITY);
        Assertions.assertTrue(AbsoluteDate.J2000_EPOCH.durationFrom(AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(Double.POSITIVE_INFINITY))
                          == Double.NEGATIVE_INFINITY);

    }

    @Test
    public void testCompareTo() {
        // check long time spans
        AbsoluteDate epoch =
                new AbsoluteDate(2000, 1, 1, 12, 0, 0, TimeScalesFactory.getTAI());
        Assertions.assertTrue(AbsoluteDate.JULIAN_EPOCH.compareTo(epoch) < 0);
        Assertions.assertTrue(epoch.compareTo(AbsoluteDate.JULIAN_EPOCH) > 0);
        // check short time spans
        AbsoluteDate d = epoch;
        double epsilon = 1.0 - FastMath.nextDown(1.0);
        Assertions.assertTrue(d.compareTo(d.shiftedBy(epsilon)) < 0);
        Assertions.assertTrue(d.compareTo(d.shiftedBy(0)) == 0);
        Assertions.assertTrue(d.compareTo(d.shiftedBy(-epsilon)) > 0);
        // check date with negative offset
        d = epoch.shiftedBy(496891466)
                .shiftedBy(0.7320114066633323)
                .shiftedBy(-19730.732011406664);
        // offset is 0 in d1
        AbsoluteDate d1 = epoch.shiftedBy(496891466 - 19730);
        Assertions.assertTrue(d.compareTo(d1) < 0);
        // decrement epoch, now offset is 0.999... in d1
        d1 = d1.shiftedBy(-1e-16);
        Assertions.assertTrue(d.compareTo(d1) < 0,"" + d.durationFrom(d1));
        // check large dates
        // these tests fail due to long overflow in durationFrom() Bug #584
        // d = new AbsoluteDate(epoch, Long.MAX_VALUE);
        // Assertions.assertEquals(-1, epoch.compareTo(d));
        // Assertions.assertTrue(d.compareTo(AbsoluteDate.FUTURE_INFINITY) < 0);
        // d = new AbsoluteDate(epoch, Long.MIN_VALUE);
        // Assertions.assertTrue(epoch.compareTo(d) > 0);
        // Assertions.assertTrue(d.compareTo(AbsoluteDate.PAST_INFINITY) > 0);
    }

    @Test
    public void testIsEqualTo() {
        Assertions.assertTrue(present.isEqualTo(present));
        Assertions.assertTrue(present.isEqualTo(presentToo));
        Assertions.assertFalse(present.isEqualTo(past));
        Assertions.assertFalse(present.isEqualTo(future));
    }

    @Test
    public void testIsCloseTo() {
        double tolerance = 10;
        TimeStamped closeToPresent = new AnyTimeStamped(present.shiftedBy(5));
        Assertions.assertTrue(present.isCloseTo(present, tolerance));
        Assertions.assertTrue(present.isCloseTo(presentToo, tolerance));
        Assertions.assertTrue(present.isCloseTo(closeToPresent, tolerance));
        Assertions.assertFalse(present.isCloseTo(past, tolerance));
        Assertions.assertFalse(present.isCloseTo(future, tolerance));
    }

    @Test
    public void testIsBefore() {
        Assertions.assertFalse(present.isBefore(past));
        Assertions.assertFalse(present.isBefore(present));
        Assertions.assertFalse(present.isBefore(presentToo));
        Assertions.assertTrue(present.isBefore(future));
    }

    @Test
    public void testIsAfter() {
        Assertions.assertTrue(present.isAfter(past));
        Assertions.assertFalse(present.isAfter(present));
        Assertions.assertFalse(present.isAfter(presentToo));
        Assertions.assertFalse(present.isAfter(future));
    }

    @Test
    public void testIsBeforeOrEqualTo() {
        Assertions.assertFalse(present.isBeforeOrEqualTo(past));
        Assertions.assertTrue(present.isBeforeOrEqualTo(present));
        Assertions.assertTrue(present.isBeforeOrEqualTo(presentToo));
        Assertions.assertTrue(present.isBeforeOrEqualTo(future));
    }

    @Test
    public void testIsAfterOrEqualTo() {
        Assertions.assertTrue(present.isAfterOrEqualTo(past));
        Assertions.assertTrue(present.isAfterOrEqualTo(present));
        Assertions.assertTrue(present.isAfterOrEqualTo(presentToo));
        Assertions.assertFalse(present.isAfterOrEqualTo(future));
    }

    @Test
    public void testIsBetween() {
        Assertions.assertTrue(present.isBetween(past, future));
        Assertions.assertTrue(present.isBetween(future, past));
        Assertions.assertFalse(past.getDate().isBetween(present, future));
        Assertions.assertFalse(past.getDate().isBetween(future, present));
        Assertions.assertFalse(future.getDate().isBetween(past, present));
        Assertions.assertFalse(future.getDate().isBetween(present, past));
        Assertions.assertFalse(present.isBetween(present, future));
        Assertions.assertFalse(present.isBetween(past, present));
        Assertions.assertFalse(present.isBetween(past, past));
        Assertions.assertFalse(present.isBetween(present, present));
        Assertions.assertFalse(present.isBetween(present, presentToo));
    }

    @Test
    public void testIsBetweenOrEqualTo() {
        Assertions.assertTrue(present.isBetweenOrEqualTo(past, future));
        Assertions.assertTrue(present.isBetweenOrEqualTo(future, past));
        Assertions.assertFalse(past.getDate().isBetweenOrEqualTo(present, future));
        Assertions.assertFalse(past.getDate().isBetweenOrEqualTo(future, present));
        Assertions.assertFalse(future.getDate().isBetweenOrEqualTo(past, present));
        Assertions.assertFalse(future.getDate().isBetweenOrEqualTo(present, past));
        Assertions.assertTrue(present.isBetweenOrEqualTo(present, future));
        Assertions.assertTrue(present.isBetweenOrEqualTo(past, present));
        Assertions.assertFalse(present.isBetweenOrEqualTo(past, past));
        Assertions.assertTrue(present.isBetweenOrEqualTo(present, present));
        Assertions.assertTrue(present.isBetweenOrEqualTo(present, presentToo));
    }

    @Test
    public void testAccuracy() {
        TimeScale tai = TimeScalesFactory.getTAI();
        double sec = 0.281;
        AbsoluteDate t = new AbsoluteDate(2010, 6, 21, 18, 42, sec, tai);
        double recomputedSec = t.getComponents(tai).getTime().getSecond();
        Assertions.assertEquals(sec, recomputedSec, FastMath.ulp(sec));
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
            Assertions.assertEquals(0.0, iteratedDate.durationFrom(directDate), maxErrorFactor * i * epsilon);
        }
        mean /= nMax;
        Assertions.assertEquals(expectedMean, mean, meanTolerance);
    }

    @Test
    public void testIssue142() {

        final AbsoluteDate epoch = AbsoluteDate.JAVA_EPOCH;
        final TimeScale utc = TimeScalesFactory.getUTC();

        Assertions.assertEquals("1970-01-01T00:00:00.000", epoch.toString(utc));
        Assertions.assertEquals(0.0, epoch.durationFrom(new AbsoluteDate(1970, 1, 1, utc)), 1.0e-15);
        Assertions.assertEquals(8.000082,
                            epoch.durationFrom(new AbsoluteDate(DateComponents.JAVA_EPOCH, TimeScalesFactory.getTAI())),
                            1.0e-15);

        //Milliseconds - April 1, 2006, in UTC
        long msOffset = 1143849600000l;
        final AbsoluteDate ad = new AbsoluteDate(epoch, msOffset/1000, TimeScalesFactory.getUTC());
        Assertions.assertEquals("2006-04-01T00:00:00.000", ad.toString(utc));

    }

    @Test
    public void testIssue148() {
        final TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate t0 = new AbsoluteDate(2012, 6, 30, 23, 59, 50.0, utc);
        DateTimeComponents components = t0.shiftedBy(11.0 - 200 * Precision.EPSILON).getComponents(utc);
        Assertions.assertEquals(2012, components.getDate().getYear());
        Assertions.assertEquals(   6, components.getDate().getMonth());
        Assertions.assertEquals(  30, components.getDate().getDay());
        Assertions.assertEquals(  23, components.getTime().getHour());
        Assertions.assertEquals(  59, components.getTime().getMinute());
        Assertions.assertEquals(  61 - 200 * Precision.EPSILON,
                            components.getTime().getSecond(), 1.0e-15);
    }

    @Test
    public void testIssue149() {
        final TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate t0 = new AbsoluteDate(2012, 6, 30, 23, 59, 59, utc);
        DateTimeComponents components = t0.shiftedBy(1.0 - Precision.EPSILON).getComponents(utc);
        Assertions.assertEquals(2012, components.getDate().getYear());
        Assertions.assertEquals(   6, components.getDate().getMonth());
        Assertions.assertEquals(  30, components.getDate().getDay());
        Assertions.assertEquals(  23, components.getTime().getHour());
        Assertions.assertEquals(  59, components.getTime().getMinute());
        Assertions.assertEquals(  60 - Precision.EPSILON,  // misleading as 60.0 - eps = 60.0
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
        Assertions.assertEquals(2015, dtc.getDate().getYear());
        Assertions.assertEquals(   9, dtc.getDate().getMonth());
        Assertions.assertEquals(  30, dtc.getDate().getDay());
        Assertions.assertEquals(   7, dtc.getTime().getHour());
        Assertions.assertEquals(  54, dtc.getTime().getMinute());
        Assertions.assertEquals(60 - 9.094947e-13, dtc.getTime().getSecond(), 1.0e-15);
        Assertions.assertEquals("2015-09-30T07:54:59.99999999999909",
                            date.toString(utc));
        AbsoluteDate beforeMidnight = new AbsoluteDate(2008, 2, 29, 23, 59, 59.9994, utc);
        AbsoluteDate stillBeforeMidnight = beforeMidnight.shiftedBy(2.0e-4);
        Assertions.assertEquals(59.9994, beforeMidnight.getComponents(utc).getTime().getSecond(), 1.0e-15);
        Assertions.assertEquals(59.9996, stillBeforeMidnight.getComponents(utc).getTime().getSecond(), 1.0e-15);
        Assertions.assertEquals("2008-02-29T23:59:59.9994", beforeMidnight.toString(utc));
        Assertions.assertEquals("2008-02-29T23:59:59.9996", stillBeforeMidnight.toString(utc));
    }


    @Test
    public void testLastLeapOutput() {
        UTCScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate t = utc.getLastKnownLeapSecond();
        Assertions.assertEquals("23:59:59.500", t.shiftedBy(-0.5).toString(utc).substring(11));
        Assertions.assertEquals("23:59:60.000", t.shiftedBy( 0.0).toString(utc).substring(11));
        Assertions.assertEquals("23:59:60.500", t.shiftedBy(+0.5).toString(utc).substring(11));
    }

    @Test
    public void testWrapBeforeLeap() {
        UTCScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate t = new AbsoluteDate("2015-06-30T23:59:59.999999", utc);
        Assertions.assertEquals(2015,        t.getComponents(utc).getDate().getYear());
        Assertions.assertEquals(   6,        t.getComponents(utc).getDate().getMonth());
        Assertions.assertEquals(  30,        t.getComponents(utc).getDate().getDay());
        Assertions.assertEquals(  23,        t.getComponents(utc).getTime().getHour());
        Assertions.assertEquals(  59,        t.getComponents(utc).getTime().getMinute());
        Assertions.assertEquals(  59.999999, t.getComponents(utc).getTime().getSecond(), 1.0e-6);
        Assertions.assertEquals("2015-06-30T23:59:59.999999", t.toString(utc));
        Assertions.assertEquals("2015-07-01T02:59:59.999999", t.toString(TimeScalesFactory.getGLONASS()));
    }

    @Test
    public void testMjdInLeap() {
        // inside a leap second
        AbsoluteDate date1 = new AbsoluteDate(2008, 12, 31, 23, 59, 60.5, utc);

        // check date to MJD conversion
        DateTimeComponents date1Components = date1.getComponents(utc);
        int mjd = date1Components.getDate().getMJD();
        double seconds = date1Components.getTime().getSecondsInUTCDay();
        Assertions.assertEquals(54831, mjd);
        Assertions.assertEquals(86400.5, seconds, 0);

        // check MJD to date conversion
        AbsoluteDate date2 = AbsoluteDate.createMJDDate(mjd, seconds, utc);
        Assertions.assertEquals(date1, date2);

        // check we still detect seconds overflow
        try {
            AbsoluteDate.createMJDDate(mjd, seconds + 1.0, utc);
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitIllegalArgumentException oiae) {
            Assertions.assertEquals(OrekitMessages.OUT_OF_RANGE_SECONDS_NUMBER_DETAIL, oiae.getSpecifier());
            Assertions.assertEquals(86400.5, (Double) oiae.getParts()[0], 0);
            Assertions.assertEquals(0, ((Number) oiae.getParts()[1]).doubleValue(), 0);
            Assertions.assertEquals(86400, ((Number) oiae.getParts()[2]).doubleValue(), 0);
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
        Assertions.assertEquals(0.0, expectedDate.durationFrom(actualDate), 1.0e-15);

    }

    @Test
    public void testGetComponentsIssue681and676and694() {
        // setup
        AbsoluteDate date = new AbsoluteDate(2009, 1, 1, utc);
        double zeroUlp = FastMath.nextUp(0.0);
        double oneUlp = FastMath.ulp(1.0);
        double sixtyUlp = FastMath.ulp(60.0);
        double one = FastMath.nextDown(1.0);
        double sixty = FastMath.nextDown(60.0);
        double sixtyOne = FastMath.nextDown(61.0);

        // actions + verify
        // translate back to AbsoluteDate has up to half an ULP of error,
        // except when truncated when the error can be up to 1 ULP.
        check(date, 2009, 1, 1, 0, 0, 0, 1, 0, 0);
        check(date.shiftedBy(zeroUlp), 2009, 1, 1, 0, 0, zeroUlp, 0.5, 0, 0);
        check(date.shiftedBy(oneUlp), 2009, 1, 1, 0, 0, oneUlp, 0.5, 0, 0);
        check(date.shiftedBy(one), 2009, 1, 1, 0, 0, one, 0.5, 0, 0);
        // I could also see rounding to a valid time as being reasonable here
        check(date.shiftedBy(59).shiftedBy(one), 2009, 1, 1, 0, 0, sixty, 1, 0, 0);
        check(date.shiftedBy(86399).shiftedBy(one), 2009, 1, 1, 23, 59, sixty, 1, 0, 0);
        check(date.shiftedBy(-zeroUlp), 2009, 1, 1, 0, 0, 0, 0.5, 0, 0);
        check(date.shiftedBy(-oneUlp), 2008, 12, 31, 23, 59, sixtyOne, 1, 0, 0);
        check(date.shiftedBy(-1).shiftedBy(zeroUlp), 2008, 12, 31, 23, 59, 60.0, 0.5, 0, 0);
        check(date.shiftedBy(-1).shiftedBy(-zeroUlp), 2008, 12, 31, 23, 59, 60.0, 0.5, 0, 0);
        check(date.shiftedBy(-1).shiftedBy(-oneUlp), 2008, 12, 31, 23, 59, 60.0, 0.5, 0, 0);
        check(date.shiftedBy(-1).shiftedBy(-sixtyUlp), 2008, 12, 31, 23, 59, sixty, 0.5, 0, 0);
        check(date.shiftedBy(-61).shiftedBy(zeroUlp), 2008, 12, 31, 23, 59, zeroUlp, 0.5, 0, 0);
        check(date.shiftedBy(-61).shiftedBy(oneUlp), 2008, 12, 31, 23, 59, oneUlp, 0.5, 0, 0);

        // check UTC weirdness.
        // These have more error because of additional multiplications and additions
        // up to 2 ULPs or ulp(60.0) of error.
        AbsoluteDate d = new AbsoluteDate(1966, 1, 1, utc);
        double ratePost = 0.0025920 / Constants.JULIAN_DAY;
        double factorPost = ratePost / (1 + ratePost);
        double ratePre = 0.0012960 / Constants.JULIAN_DAY;
        double factorPre = ratePre / (1 + ratePre);
        check(d, 1966, 1, 1, 0, 0, 0, 1, 0, 0);
        check(d.shiftedBy(zeroUlp), 1966, 1, 1, 0, 0, 0, 0.5, 0, 0);
        check(d.shiftedBy(oneUlp), 1966, 1, 1, 0, 0, oneUlp, 0.5, 0, 0);
        check(d.shiftedBy(one), 1966, 1, 1, 0, 0, one * (1 - factorPost), 0.5, 2, 0);
        check(d.shiftedBy(59).shiftedBy(one), 1966, 1, 1, 0, 0, sixty * (1 - factorPost), 1, 1, 0);
        check(d.shiftedBy(86399).shiftedBy(one), 1966, 1, 1, 23, 59, sixty - 86400 * factorPost, 1, 1, 0);
        check(d.shiftedBy(-zeroUlp), 1966, 1, 1, 0, 0, 0, 0.5, 0, 0);
        // actual leap is small ~1e-16, but during a leap rounding up to 60.0 is ok
        check(d.shiftedBy(-oneUlp), 1965, 12, 31, 23, 59, 60.0, 1, 0, 0);
        check(d.shiftedBy(-1).shiftedBy(zeroUlp), 1965, 12, 31, 23, 59, 59 + factorPre, 0.5, 0, 0);
        check(d.shiftedBy(-1).shiftedBy(-zeroUlp), 1965, 12, 31, 23, 59, 59 + factorPre, 0.5, 0, 0);
        check(d.shiftedBy(-1).shiftedBy(-oneUlp), 1965, 12, 31, 23, 59, 59 + factorPre, 0.5, 0, 0);
        check(d.shiftedBy(-1).shiftedBy(-sixtyUlp), 1965, 12, 31, 23, 59, 59 + (1 + sixtyUlp) * factorPre, 0.5, 1, 0);
        // since second ~= 0 there is significant cancellation
        check(d.shiftedBy(-60).shiftedBy(zeroUlp), 1965, 12, 31, 23, 59, 60 * factorPre, 0, 0, sixtyUlp);
        check(d.shiftedBy(-60).shiftedBy(oneUlp), 1965, 12, 31, 23, 59, (oneUlp - oneUlp * factorPre) + 60 * factorPre, 0.5, 0, sixtyUlp);

        // check first whole second leap
        AbsoluteDate d2 = new AbsoluteDate(1972, 7, 1, utc);
        check(d2, 1972, 7, 1, 0, 0, 0, 1, 0, 0);
        check(d2.shiftedBy(zeroUlp), 1972, 7, 1, 0, 0, zeroUlp, 0.5, 0, 0);
        check(d2.shiftedBy(oneUlp), 1972, 7, 1, 0, 0, oneUlp, 0.5, 0, 0);
        check(d2.shiftedBy(one), 1972, 7, 1, 0, 0, one, 0.5, 0, 0);
        check(d2.shiftedBy(59).shiftedBy(one), 1972, 7, 1, 0, 0, sixty, 1, 0, 0);
        check(d2.shiftedBy(86399).shiftedBy(one), 1972, 7, 1, 23, 59, sixty, 1, 0, 0);
        check(d2.shiftedBy(-zeroUlp), 1972, 7, 1, 0, 0, 0, 0.5, 0, 0);
        check(d2.shiftedBy(-oneUlp), 1972, 6, 30, 23, 59, sixtyOne, 1, 0, 0);
        check(d2.shiftedBy(-1).shiftedBy(zeroUlp), 1972, 6, 30, 23, 59, 60.0, 0.5, 0, 0);
        check(d2.shiftedBy(-1).shiftedBy(-zeroUlp), 1972, 6, 30, 23, 59, 60.0, 0.5, 0, 0);
        check(d2.shiftedBy(-1).shiftedBy(-oneUlp), 1972, 6, 30, 23, 59, 60.0, 0.5, 0, 0);
        check(d2.shiftedBy(-1).shiftedBy(-sixtyUlp), 1972, 6, 30, 23, 59, sixty, 0.5, 0, 0);
        check(d2.shiftedBy(-61).shiftedBy(zeroUlp), 1972, 6, 30, 23, 59, zeroUlp, 0.5, 0, 0);
        check(d2.shiftedBy(-61).shiftedBy(oneUlp), 1972, 6, 30, 23, 59, oneUlp, 0.5, 0, 0);

        // check first leap second, which was actually 1.422818 s.
        AbsoluteDate d3 = AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-1230724800);
        check(d3, 1960, 12, 31, 23, 59, 60, 0.5, 0, 0);
        AbsoluteDate d4 = new AbsoluteDate(1961, 1, 1, utc);
        check(d4, 1961, 1, 1, 0, 0, 0, 0.5, 0, 0);
        // FIXME something wrong because a date a smidgen before 1961-01-01 is not in a leap second
        //check(d4.shiftedBy(-oneUlp), 1960, 12, 31, 23, 59, 61.422818, 0.5, 0, 0);

        // check NaN, this is weird that NaNs have valid ymdhm, but not second.
        DateTimeComponents actual = date.shiftedBy(Double.NaN).getComponents(utc);
        DateComponents dc = actual.getDate();
        TimeComponents tc = actual.getTime();
        MatcherAssert.assertThat(dc.getYear(), CoreMatchers.is(2009));
        MatcherAssert.assertThat(dc.getMonth(), CoreMatchers.is(1));
        MatcherAssert.assertThat(dc.getDay(), CoreMatchers.is(1));
        MatcherAssert.assertThat(tc.getHour(), CoreMatchers.is(0));
        MatcherAssert.assertThat(tc.getMinute(), CoreMatchers.is(0));
        MatcherAssert.assertThat("second", tc.getSecond(), CoreMatchers.is(Double.NaN));
        MatcherAssert.assertThat(tc.getMinutesFromUTC(), CoreMatchers.is(0));
        final double difference = new AbsoluteDate(actual, utc).durationFrom(date);
        MatcherAssert.assertThat(difference, CoreMatchers.is(Double.NaN));
    }

    private void check(AbsoluteDate date,
                       int year, int month, int day, int hour, int minute, double second,
                       double roundTripUlps, final int secondUlps, final double absTol) {
        DateTimeComponents actual = date.getComponents(utc);
        DateComponents d = actual.getDate();
        TimeComponents t = actual.getTime();
        MatcherAssert.assertThat(d.getYear(), CoreMatchers.is(year));
        MatcherAssert.assertThat(d.getMonth(), CoreMatchers.is(month));
        MatcherAssert.assertThat(d.getDay(), CoreMatchers.is(day));
        MatcherAssert.assertThat(t.getHour(), CoreMatchers.is(hour));
        MatcherAssert.assertThat(t.getMinute(), CoreMatchers.is(minute));
        MatcherAssert.assertThat("second", t.getSecond(),
                OrekitMatchers.numberCloseTo(second, absTol, secondUlps));
        MatcherAssert.assertThat(t.getMinutesFromUTC(), CoreMatchers.is(0));
        final double tol = FastMath.ulp(second) * roundTripUlps;
        final double difference = new AbsoluteDate(actual, utc).durationFrom(date);
        MatcherAssert.assertThat(difference,
                OrekitMatchers.closeTo(0, FastMath.max(absTol, tol)));
    }

    /** Check {@link AbsoluteDate#toStringRfc3339(TimeScale)}. */
    @Test
    public void testToStringRfc3339() {
        // setup
        AbsoluteDate date = new AbsoluteDate(2009, 1, 1, utc);
        double one = FastMath.nextDown(1.0);
        double zeroUlp = FastMath.nextUp(0.0);
        double oneUlp = FastMath.ulp(1.0);
        //double sixty = FastMath.nextDown(60.0);
        double sixtyUlp = FastMath.ulp(60.0);

        // action
        // test midnight
        check(date, "2009-01-01T00:00:00Z");
        check(date.shiftedBy(1), "2009-01-01T00:00:01Z");
        // test digits and rounding
        check(date.shiftedBy(12.3456789123456789), "2009-01-01T00:00:12.34567891234568Z");
        check(date.shiftedBy(0.0123456789123456789), "2009-01-01T00:00:00.01234567891235Z");
        // test min and max values
        check(date.shiftedBy(zeroUlp), "2009-01-01T00:00:00Z");
        check(date.shiftedBy(59.0).shiftedBy(one), "2009-01-01T00:00:59.99999999999999Z");
        check(date.shiftedBy(86399).shiftedBy(one), "2009-01-01T23:59:59.99999999999999Z");
        check(date.shiftedBy(oneUlp), "2009-01-01T00:00:00Z");
        check(date.shiftedBy(one), "2009-01-01T00:00:01Z");
        check(date.shiftedBy(-zeroUlp), "2009-01-01T00:00:00Z");
        // test leap
        check(date.shiftedBy(-oneUlp), "2008-12-31T23:59:60.99999999999999Z");
        check(date.shiftedBy(-1).shiftedBy(one), "2008-12-31T23:59:60.99999999999999Z");
        check(date.shiftedBy(-0.5), "2008-12-31T23:59:60.5Z");
        check(date.shiftedBy(-1).shiftedBy(zeroUlp), "2008-12-31T23:59:60Z");
        check(date.shiftedBy(-1), "2008-12-31T23:59:60Z");
        check(date.shiftedBy(-1).shiftedBy(-zeroUlp), "2008-12-31T23:59:60Z");
        check(date.shiftedBy(-1).shiftedBy(-oneUlp), "2008-12-31T23:59:60Z");
        check(date.shiftedBy(-2), "2008-12-31T23:59:59Z");
        check(date.shiftedBy(-1).shiftedBy(-sixtyUlp), "2008-12-31T23:59:59.99999999999999Z");
        check(date.shiftedBy(-61).shiftedBy(zeroUlp), "2008-12-31T23:59:00Z");
        check(date.shiftedBy(-61).shiftedBy(oneUlp), "2008-12-31T23:59:00Z");
        // test UTC weirdness
        // These have more error because of additional multiplications and additions
        // up to 2 ULPs or ulp(60.0) of error.
        // toStringRFC3339 only has 14 digits of precision after the decimal point
        final DecimalFormat format = new DecimalFormat("00.##############", new DecimalFormatSymbols(Locale.US));
        AbsoluteDate d = new AbsoluteDate(1966, 1, 1, utc);
        double ratePost = 0.0025920 / Constants.JULIAN_DAY;
        double factorPost = ratePost / (1 + ratePost);
        double ratePre = 0.0012960 / Constants.JULIAN_DAY;
        double factorPre = ratePre / (1 + ratePre);
        check(d, "1966-01-01T00:00:00Z"); //, 1, 0, 0);
        check(d.shiftedBy(zeroUlp), "1966-01-01T00:00:00Z"); //, 0.5, 0, 0);
        check(d.shiftedBy(oneUlp), "1966-01-01T00:00:00Z"); //, oneUlp, 0.5, 0, 0);
        check(d.shiftedBy(one), "1966-01-01T00:00:" + format.format( one * (1 - factorPost)) + "Z"); //, 0.5, 2, 0);
        // one ulp of error
        check(d.shiftedBy(59).shiftedBy(one), "1966-01-01T00:00:59.99999820000005Z"); // + format.format( sixty * (1 - factorPost)) + "Z"); //, 1, 1, 0);
        // one ulp of error
        check(d.shiftedBy(86399).shiftedBy(one), "1966-01-01T23:59:59.99740800007776Z"); // + format.format( sixty - 86400 * factorPost) + "Z"); //, 1, 1, 0);
        check(d.shiftedBy(-zeroUlp), "1966-01-01T00:00:00Z"); // , 0.5, 0, 0);
        // actual leap is small ~1e-16, but during a leap rounding up to 60.0 is ok
        check(d.shiftedBy(-oneUlp), "1965-12-31T23:59:60Z"); // , 1, 0, 0);
        check(d.shiftedBy(-1).shiftedBy(zeroUlp), "1965-12-31T23:59:" + format.format( 59 + factorPre) + "Z"); //, 0.5, 0, 0);
        check(d.shiftedBy(-1).shiftedBy(-zeroUlp), "1965-12-31T23:59:" + format.format( 59 + factorPre) + "Z"); //, 0.5, 0, 0);
        check(d.shiftedBy(-1).shiftedBy(-oneUlp), "1965-12-31T23:59:" + format.format( 59 + factorPre) + "Z"); //, 0.5, 0, 0);
        // one ulp of error
        check(d.shiftedBy(-1).shiftedBy(-sixtyUlp), "1965-12-31T23:59:59.00000001499999Z"); // + format.format( 59 + (1 + sixtyUlp) * factorPre) + "Z"); //, 0.5, 1, 0);
        // since second ~= 0 there is significant cancellation
        check(d.shiftedBy(-60).shiftedBy(zeroUlp), "1965-12-31T23:59:" + format.format( 60 * factorPre) + "Z"); //, 0, 0, sixtyUlp);
        check(d.shiftedBy(-60).shiftedBy(oneUlp), "1965-12-31T23:59:" + format.format( (oneUlp - oneUlp * factorPre) + 60 * factorPre) + "Z"); //, 0.5, 0, sixtyUlp);

        // check first leap second, which was actually 1.422818 s.
        check(new AbsoluteDate(1961, 1, 1, utc), "1961-01-01T00:00:00Z"); //, 0.5, 0, 0);
        AbsoluteDate d3 = AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-1230724800);
        check(d3, "1960-12-31T23:59:60Z"); ///, 0.5, 0, 0);
        // FIXME something wrong because a date a smidgen before 1961-01-01 is not in a leap second
        //check(d3.shiftedBy(FastMath.nextDown(1.422818)), "1960-12-31T23:59:61.422818Z"); //, 0.5, 0, 0);

        // test proleptic
        check(new AbsoluteDate(123, 4, 5, 6, 7, 8.9, utc), "0123-04-05T06:07:08.9Z");

        // there is not way to produce valid RFC3339 for these cases
        // I would rather print something useful than throw an exception
        // so these cases don't check for a correct answer, just an informative one
        check(new AbsoluteDate(-123, 4, 5, 6, 7, 8.9, utc), "-123-04-05T06:07:08.9Z");
        check(new AbsoluteDate(-1230, 4, 5, 6, 7, 8.9, utc), "-1230-04-05T06:07:08.9Z");
        // test far future
        check(new AbsoluteDate(12300, 4, 5, 6, 7, 8.9, utc), "12300-04-05T06:07:08.9Z");
        // test infinity
        check(AbsoluteDate.FUTURE_INFINITY, "5881610-07-11T23:59:59.999Z");
        check(AbsoluteDate.PAST_INFINITY, "-5877490-03-03T00:00:00Z");
        // test NaN
        if ("1.8".equals(System.getProperty("java.specification.version"))) {
            // \uFFFD is "�", the unicode replacement character
            // that is what DecimalFormat uses instead of "NaN"
            check(date.shiftedBy(Double.NaN), "2009-01-01T00:00:\uFFFDZ");
        } else {
            check(date.shiftedBy(Double.NaN), "2009-01-01T00:00:NaNZ");
        }
    }

    private void check(final AbsoluteDate d, final String s) {
        MatcherAssert.assertThat(d.toStringRfc3339(utc),
                CoreMatchers.is(s));
        MatcherAssert.assertThat(d.getComponents(utc).toStringRfc3339(),
                CoreMatchers.is(s));
    }


    /** Check {@link AbsoluteDate#toString()}. */
    @Test
    public void testToString() {
        // setup
        AbsoluteDate date = new AbsoluteDate(2009, 1, 1, utc);
        double one = FastMath.nextDown(1.0);
        double zeroUlp = FastMath.nextUp(0.0);
        double oneUlp = FastMath.ulp(1.0);
        //double sixty = FastMath.nextDown(60.0);
        double sixtyUlp = FastMath.ulp(60.0);

        // action
        // test midnight
        checkToString(date, "2009-01-01T00:00:00.000");
        checkToString(date.shiftedBy(1), "2009-01-01T00:00:01.000");
        // test digits and rounding
        checkToString(date.shiftedBy(12.3456789123456789), "2009-01-01T00:00:12.34567891234568");
        checkToString(date.shiftedBy(0.0123456789123456789), "2009-01-01T00:00:00.01234567891235");
        // test min and max values
        checkToString(date.shiftedBy(zeroUlp), "2009-01-01T00:00:00.000");
        // Orekit 10.1 rounds up
        checkToString(date.shiftedBy(59.0).shiftedBy(one), "2009-01-01T00:00:59.99999999999999");
        // Orekit 10.1 rounds up
        checkToString(date.shiftedBy(86399).shiftedBy(one), "2009-01-01T23:59:59.99999999999999");
        checkToString(date.shiftedBy(oneUlp), "2009-01-01T00:00:00.000");
        checkToString(date.shiftedBy(one), "2009-01-01T00:00:01.000");
        checkToString(date.shiftedBy(-zeroUlp), "2009-01-01T00:00:00.000");
        // test leap
        // Orekit 10.1 throw OIAE, 10.2 rounds up
        checkToString(date.shiftedBy(-oneUlp), "2008-12-31T23:59:60.99999999999999");
        // Orekit 10.1 rounds up
        checkToString(date.shiftedBy(-1).shiftedBy(one), "2008-12-31T23:59:60.99999999999999");
        checkToString(date.shiftedBy(-0.5), "2008-12-31T23:59:60.500");
        checkToString(date.shiftedBy(-1).shiftedBy(zeroUlp), "2008-12-31T23:59:60.000");
        checkToString(date.shiftedBy(-1), "2008-12-31T23:59:60.000");
        checkToString(date.shiftedBy(-1).shiftedBy(-zeroUlp), "2008-12-31T23:59:60.000");
        checkToString(date.shiftedBy(-1).shiftedBy(-oneUlp), "2008-12-31T23:59:60.000");
        checkToString(date.shiftedBy(-2), "2008-12-31T23:59:59.000");
        // Orekit 10.1 rounds up
        checkToString(date.shiftedBy(-1).shiftedBy(-sixtyUlp), "2008-12-31T23:59:59.99999999999999");
        checkToString(date.shiftedBy(-61).shiftedBy(zeroUlp), "2008-12-31T23:59:00.000");
        checkToString(date.shiftedBy(-61).shiftedBy(oneUlp), "2008-12-31T23:59:00.000");
        // test UTC weirdness
        // These have more error because of additional multiplications and additions
        // up to 2 ULPs or ulp(60.0) of error.
        // toStringRFC3339 only has 14 digits of precision after the decimal point
        final DecimalFormat format = new DecimalFormat("00.##############", new DecimalFormatSymbols(Locale.US));
        AbsoluteDate d = new AbsoluteDate(1966, 1, 1, utc);
        double ratePost = 0.0025920 / Constants.JULIAN_DAY;
        double factorPost = ratePost / (1 + ratePost);
        double ratePre = 0.0012960 / Constants.JULIAN_DAY;
        double factorPre = ratePre / (1 + ratePre);
        checkToString(d, "1966-01-01T00:00:00.000"); //, 1, 0, 0);
        checkToString(d.shiftedBy(zeroUlp), "1966-01-01T00:00:00.000"); //, 0.5, 0, 0);
        checkToString(d.shiftedBy(oneUlp), "1966-01-01T00:00:00.000"); //, oneUlp, 0.5, 0, 0);
        checkToString(d.shiftedBy(one), "1966-01-01T00:00:" + format.format( one * (1 - factorPost))); //, 0.5, 2, 0);
        // Orekit 10.1 rounds up
        checkToString(d.shiftedBy(59).shiftedBy(one), "1966-01-01T00:00:" + format.format( 60 * (1 - factorPost))); //  + "Z"); //, 1, 1, 0);
        // one ulp of error
        checkToString(d.shiftedBy(86399).shiftedBy(one), "1966-01-01T23:59:" + format.format( 60 - 86400 * factorPost)); //  + "Z"); //, 1, 1, 0);
        checkToString(d.shiftedBy(-zeroUlp), "1966-01-01T00:00:00.000"); // , 0.5, 0, 0);
        // actual leap is small ~1e-16, but during a leap rounding up to 60.0 is ok
        checkToString(d.shiftedBy(-oneUlp), "1965-12-31T23:59:60.000"); // , 1, 0, 0);
        checkToString(d.shiftedBy(-1).shiftedBy(zeroUlp), "1965-12-31T23:59:" + format.format( 59 + factorPre) ); //, 0.5, 0, 0);
        checkToString(d.shiftedBy(-1).shiftedBy(-zeroUlp), "1965-12-31T23:59:" + format.format( 59 + factorPre) ); //, 0.5, 0, 0);
        checkToString(d.shiftedBy(-1).shiftedBy(-oneUlp), "1965-12-31T23:59:" + format.format( 59 + factorPre) ); //, 0.5, 0, 0);
        // one ulp of error
        checkToString(d.shiftedBy(-1).shiftedBy(-sixtyUlp), "1965-12-31T23:59:59.00000001499999"); // + format.format(59 + factorPre)+ "Z"); //, 0.5, 1, 0);
        // since second ~= 0 there is significant cancellation
        checkToString(d.shiftedBy(-60).shiftedBy(zeroUlp), "1965-12-31T23:59:" + format.format( 60 * factorPre) ); //, 0, 0, sixtyUlp);
        checkToString(d.shiftedBy(-60).shiftedBy(oneUlp), "1965-12-31T23:59:" + format.format( (oneUlp - oneUlp * factorPre) + 60 * factorPre) ); //, 0.5, 0, sixtyUlp);

        // check first leap second, which was actually 1.422818 s.
        checkToString(new AbsoluteDate(1961, 1, 1, utc), "1961-01-01T00:00:00.000"); //, 0.5, 0, 0);
        AbsoluteDate d3 = AbsoluteDate.ARBITRARY_EPOCH.shiftedBy(-1230724800);
        checkToString(d3, "1960-12-31T23:59:60.000"); ///, 0.5, 0, 0);
        // FIXME something wrong because a date a smidgen before 1961-01-01 is not in a leap second
        //checkToString(d3.shiftedBy(FastMath.nextDown(1.422818)), "1960-12-31T23:59:61.423"); //, 0.5, 0, 0);

        // test proleptic
        checkToString(new AbsoluteDate(123, 4, 5, 6, 7, 8.9, utc), "0123-04-05T06:07:08.900");

        // there is not way to produce valid RFC3339 for these cases
        // I would rather print something useful than throw an exception
        // so these cases don't check for a correct answer, just an informative one
        checkToString(new AbsoluteDate(-123, 4, 5, 6, 7, 8.9, utc), "-0123-04-05T06:07:08.900");
        checkToString(new AbsoluteDate(-1230, 4, 5, 6, 7, 8.9, utc), "-1230-04-05T06:07:08.900");
        // test far future
        checkToString(new AbsoluteDate(12300, 4, 5, 6, 7, 8.9, utc), "12300-04-05T06:07:08.900");
        // test infinity
        checkToString(AbsoluteDate.FUTURE_INFINITY, "5881610-07-11T23:59:59.999");
        checkToString(AbsoluteDate.PAST_INFINITY, "-5877490-03-03T00:00:00.000");
        // test NaN
        if ("1.8".equals(System.getProperty("java.specification.version"))) {
            // \uFFFD is "�", the unicode replacement character
            // that is what DecimalFormat used instead of "NaN" up to Java 8
            checkToString(date.shiftedBy(Double.NaN), "2009-01-01T00:00:\uFFFD");
        } else {
            checkToString(date.shiftedBy(Double.NaN), "2009-01-01T00:00:NaN");
        }
    }

    private void checkToString(final AbsoluteDate d, final String s) {
        MatcherAssert.assertThat(d.toString(), CoreMatchers.is(s + "Z"));
        MatcherAssert.assertThat(d.getComponents(utc).toString(), CoreMatchers.is(s + "+00:00"));
    }

    @Test
    public void testToStringWithoutUtcOffset() {
        // setup
        AbsoluteDate date = new AbsoluteDate(2009, 1, 1, utc);
        double one = FastMath.nextDown(1.0);
        double zeroUlp = FastMath.nextUp(0.0);
        double oneUlp = FastMath.ulp(1.0);
        //double sixty = FastMath.nextDown(60.0);
        double sixtyUlp = FastMath.ulp(60.0);

        // action
        // test midnight
        checkToStringNoOffset(date, "2009-01-01T00:00:00.000");
        checkToStringNoOffset(date.shiftedBy(1), "2009-01-01T00:00:01.000");
        // test digits and rounding
        checkToStringNoOffset(date.shiftedBy(12.3456789123456789), "2009-01-01T00:00:12.346");
        checkToStringNoOffset(date.shiftedBy(0.0123456789123456789), "2009-01-01T00:00:00.012");
        // test min and max values
        checkToStringNoOffset(date.shiftedBy(zeroUlp), "2009-01-01T00:00:00.000");
        // Orekit 10.1 rounds up
        checkToStringNoOffset(date.shiftedBy(59.0).shiftedBy(one), "2009-01-01T00:01:00.000");
        // Orekit 10.1 rounds up
        checkToStringNoOffset(date.shiftedBy(86399).shiftedBy(one), "2009-01-02T00:00:00.000");
        checkToStringNoOffset(date.shiftedBy(oneUlp), "2009-01-01T00:00:00.000");
        checkToStringNoOffset(date.shiftedBy(one), "2009-01-01T00:00:01.000");
        checkToStringNoOffset(date.shiftedBy(-zeroUlp), "2009-01-01T00:00:00.000");
        // test leap
        // Orekit 10.1 throw OIAE, 10.2 rounds up
        checkToStringNoOffset(date.shiftedBy(-oneUlp), "2009-01-01T00:00:00.000");
        // Orekit 10.1 rounds up
        checkToStringNoOffset(date.shiftedBy(-1).shiftedBy(one), "2009-01-01T00:00:00.000");
        checkToStringNoOffset(date.shiftedBy(-0.5), "2008-12-31T23:59:60.500");
        checkToStringNoOffset(date.shiftedBy(-1).shiftedBy(zeroUlp), "2008-12-31T23:59:60.000");
        checkToStringNoOffset(date.shiftedBy(-1), "2008-12-31T23:59:60.000");
        checkToStringNoOffset(date.shiftedBy(-1).shiftedBy(-zeroUlp), "2008-12-31T23:59:60.000");
        checkToStringNoOffset(date.shiftedBy(-1).shiftedBy(-oneUlp), "2008-12-31T23:59:60.000");
        checkToStringNoOffset(date.shiftedBy(-2), "2008-12-31T23:59:59.000");
        // Orekit 10.1 rounds up
        checkToStringNoOffset(date.shiftedBy(-1).shiftedBy(-sixtyUlp), "2008-12-31T23:59:60.000");
        checkToStringNoOffset(date.shiftedBy(-61).shiftedBy(zeroUlp), "2008-12-31T23:59:00.000");
        checkToStringNoOffset(date.shiftedBy(-61).shiftedBy(oneUlp), "2008-12-31T23:59:00.000");
    }


    private void checkToStringNoOffset(final AbsoluteDate d, final String s) {
        MatcherAssert.assertThat(d.toStringWithoutUtcOffset(utc, 3), CoreMatchers.is(s));
        MatcherAssert.assertThat(
                d.getComponents(utc).toStringWithoutUtcOffset(utc.minuteDuration(d), 3),
                CoreMatchers.is(s));
    }

    /**
     * Check {@link AbsoluteDate#toString()} when UTC throws an exception. This ~is~ was
     * the most common issue new and old users face.
     */
    @Test
    public void testToStringException() {
        Utils.setDataRoot("no-data");
        try {
            DataContext.getDefault().getTimeScales().getUTC();
            Assertions.fail("Expected Exception");
        } catch (OrekitException e) {
            // expected
            Assertions.assertEquals(e.getSpecifier(), OrekitMessages.NO_IERS_UTC_TAI_HISTORY_DATA_LOADED);
        }
        // try some unusual values
        MatcherAssert.assertThat(present.toString(), CoreMatchers.is("2000-01-01T12:00:32.000 TAI"));
        MatcherAssert.assertThat(present.shiftedBy(Double.POSITIVE_INFINITY).toString(),
                CoreMatchers.is("5881610-07-11T23:59:59.999 TAI"));
        MatcherAssert.assertThat(present.shiftedBy(Double.NEGATIVE_INFINITY).toString(),
                CoreMatchers.is("-5877490-03-03T00:00:00.000 TAI"));
        String nan = "1.8".equals(System.getProperty("java.specification.version")) ? "\uFFFD" : "NaN";
        MatcherAssert.assertThat(present.shiftedBy(Double.NaN).toString(),
                CoreMatchers.is("2000-01-01T12:00:" + nan + " TAI"));
        // infinity is special cased, but I can make AbsoluteDate.offset larger than
        // Long.MAX_VALUE see #584
        AbsoluteDate d = present.shiftedBy(1e300).shiftedBy(1e300).shiftedBy(1e300);
        MatcherAssert.assertThat(d.toString(),
                CoreMatchers.is("(-9223372036854775779 + 3.0E300) seconds past epoch"));
    }

    /** Test for issue 943: management of past and future infinity in equality checks. */
    @Test
    public void test_issue_943() {

        // Run issue test
        final AbsoluteDate date1 = new AbsoluteDate(AbsoluteDate.PAST_INFINITY, 0);
        final AbsoluteDate date2 = new AbsoluteDate(AbsoluteDate.PAST_INFINITY, 0);
        date1.durationFrom(date2);
        Assertions.assertEquals(date1, date2);

        // Check equality is as expected for PAST INFINITY
        final AbsoluteDate date3 = AbsoluteDate.PAST_INFINITY;
        final AbsoluteDate date4 = new AbsoluteDate(AbsoluteDate.PAST_INFINITY, 0);
        Assertions.assertEquals(date3, date4);

        // Check equality is as expected for FUTURE INFINITY
        final AbsoluteDate date5 = AbsoluteDate.FUTURE_INFINITY;
        final AbsoluteDate date6 = new AbsoluteDate(AbsoluteDate.FUTURE_INFINITY, 0);
        Assertions.assertEquals(date5, date6); 

        // Check inequality is as expected
        final AbsoluteDate date7 = new AbsoluteDate(AbsoluteDate.PAST_INFINITY, 0);
        final AbsoluteDate date8 = new AbsoluteDate(AbsoluteDate.FUTURE_INFINITY, 0);
        Assertions.assertNotEquals(date7, date8); 

        // Check inequality is as expected
        final AbsoluteDate date9 = new AbsoluteDate(AbsoluteDate.ARBITRARY_EPOCH.getEpoch(), Double.POSITIVE_INFINITY);
        final AbsoluteDate date10 = new AbsoluteDate(AbsoluteDate.ARBITRARY_EPOCH.getEpoch(), Double.POSITIVE_INFINITY);
        Assertions.assertEquals(date9, date10); 
    }

    public void testNegativeOffsetConstructor() {
        try {
            AbsoluteDate date = new AbsoluteDate(2019, 10, 11, 20, 40,
                                                 FastMath.scalb(6629298651489277.0, -55),
                                                 TimeScalesFactory.getTT());
            AbsoluteDate after = date.shiftedBy(Precision.EPSILON);
            Field epochField = AbsoluteDate.class.getDeclaredField("epoch");
            epochField.setAccessible(true);
            Field offsetField = AbsoluteDate.class.getDeclaredField("offset");
            offsetField.setAccessible(true);
            Assertions.assertEquals(624098367L, epochField.getLong(date));
            Assertions.assertEquals(FastMath.nextAfter(1.0, Double.NEGATIVE_INFINITY), offsetField.getDouble(date), 1.0e-20);
            Assertions.assertEquals(Precision.EPSILON, after.durationFrom(date), 1.0e-20);
        } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
            Assertions.fail(e.getLocalizedMessage());
        }
    }

    @Test
    public void testNegativeOffsetShift() {
        try {
            AbsoluteDate reference = new AbsoluteDate(2019, 10, 11, 20, 40, 1.6667019180022178E-7,
                                                      TimeScalesFactory.getTAI());
            double dt = FastMath.scalb(6596520010750484.0, -39);
            AbsoluteDate shifted = reference.shiftedBy(dt);
            AbsoluteDate after   = shifted.shiftedBy(Precision.EPSILON);
            Field epochField = AbsoluteDate.class.getDeclaredField("epoch");
            epochField.setAccessible(true);
            Field offsetField = AbsoluteDate.class.getDeclaredField("offset");
            offsetField.setAccessible(true);
            Assertions.assertEquals(624110398L, epochField.getLong(shifted));
            Assertions.assertEquals(1.0 - 1.69267e-13, offsetField.getDouble(shifted), 1.0e-15);
            Assertions.assertEquals(Precision.EPSILON, after.durationFrom(shifted), 1.0e-20);
        } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
            Assertions.fail(e.getLocalizedMessage());
        }
    }

    @BeforeEach
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
