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



import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.hipparchus.random.RandomGenerator;
import org.hipparchus.random.Well1024a;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.utils.Constants;

public class UTCScaleTest {

    @Test
    public void testAfter() {
        AbsoluteDate d1 = new AbsoluteDate(new DateComponents(2020, 12, 31),
                                           new TimeComponents(23, 59, 59),
                                           utc);
        Assert.assertEquals("2020-12-31T23:59:59.000", d1.toString());
    }

    @Test
    public void testNoLeap() {
        Assert.assertEquals("UTC", utc.toString());
        AbsoluteDate d1 = new AbsoluteDate(new DateComponents(1999, 12, 31),
                                           new TimeComponents(23, 59, 59),
                                           utc);
        AbsoluteDate d2 = new AbsoluteDate(new DateComponents(2000, 01, 01),
                                           new TimeComponents(00, 00, 01),
                                           utc);
        Assert.assertEquals(2.0, d2.durationFrom(d1), 1.0e-10);
    }

    @Test
    public void testLeap2006() {
        AbsoluteDate leapDate =
            new AbsoluteDate(new DateComponents(2006, 01, 01), TimeComponents.H00, utc);
        AbsoluteDate d1 = leapDate.shiftedBy(-1);
        AbsoluteDate d2 = leapDate.shiftedBy(+1);
        Assert.assertEquals(2.0, d2.durationFrom(d1), 1.0e-10);

        AbsoluteDate d3 = new AbsoluteDate(new DateComponents(2005, 12, 31),
                                           new TimeComponents(23, 59, 59),
                                           utc);
        AbsoluteDate d4 = new AbsoluteDate(new DateComponents(2006, 01, 01),
                                           new TimeComponents(00, 00, 01),
                                           utc);
        Assert.assertEquals(3.0, d4.durationFrom(d3), 1.0e-10);
    }

    @Test
    public void testDuringLeap() {
        AbsoluteDate d = new AbsoluteDate(new DateComponents(1983, 06, 30),
                                          new TimeComponents(23, 59, 59),
                                          utc);
        Assert.assertEquals("1983-06-30T23:58:59.000", d.shiftedBy(-60).toString(utc));
        Assert.assertEquals(60, utc.minuteDuration(d.shiftedBy(-60)));
        Assert.assertFalse(utc.insideLeap(d.shiftedBy(-60)));
        Assert.assertEquals("1983-06-30T23:59:59.000", d.toString(utc));
        Assert.assertEquals(61, utc.minuteDuration(d));
        Assert.assertFalse(utc.insideLeap(d));
        d = d.shiftedBy(0.251);
        Assert.assertEquals("1983-06-30T23:59:59.251", d.toString(utc));
        Assert.assertEquals(61, utc.minuteDuration(d));
        Assert.assertFalse(utc.insideLeap(d));
        d = d.shiftedBy(0.251);
        Assert.assertEquals("1983-06-30T23:59:59.502", d.toString(utc));
        Assert.assertEquals(61, utc.minuteDuration(d));
        Assert.assertFalse(utc.insideLeap(d));
        d = d.shiftedBy(0.251);
        Assert.assertEquals("1983-06-30T23:59:59.753", d.toString(utc));
        Assert.assertEquals(61, utc.minuteDuration(d));
        Assert.assertFalse(utc.insideLeap(d));
        d = d.shiftedBy( 0.251);
        Assert.assertEquals("1983-06-30T23:59:60.004", d.toString(utc));
        Assert.assertEquals(61, utc.minuteDuration(d));
        Assert.assertTrue(utc.insideLeap(d));
        d = d.shiftedBy(0.251);
        Assert.assertEquals("1983-06-30T23:59:60.255", d.toString(utc));
        Assert.assertEquals(61, utc.minuteDuration(d));
        Assert.assertTrue(utc.insideLeap(d));
        d = d.shiftedBy(0.251);
        Assert.assertEquals("1983-06-30T23:59:60.506", d.toString(utc));
        Assert.assertEquals(61, utc.minuteDuration(d));
        d = d.shiftedBy(0.251);
        Assert.assertEquals("1983-06-30T23:59:60.757", d.toString(utc));
        Assert.assertEquals(61, utc.minuteDuration(d));
        Assert.assertTrue(utc.insideLeap(d));
        d = d.shiftedBy(0.251);
        Assert.assertEquals("1983-07-01T00:00:00.008", d.toString(utc));
        Assert.assertEquals(60, utc.minuteDuration(d));
        Assert.assertFalse(utc.insideLeap(d));
    }

    @Test
    public void testWrapBeforeLeap() throws OrekitException {
        AbsoluteDate t = new AbsoluteDate("2015-06-30T23:59:59.999999", utc);
        Assert.assertEquals("2015-06-30T23:59:60.000", t.toString(utc));
    }

    @Test
    public void testMinuteDuration() {
        final AbsoluteDate t0 = new AbsoluteDate("1983-06-30T23:58:59.000", utc);
        for (double dt = 0; dt < 63; dt += 0.3) {
            if (dt < 1.0) {
                // before the minute of the leap
                Assert.assertEquals(60, utc.minuteDuration(t0.shiftedBy(dt)));
            } else if (dt < 62.0) {
                // during the minute of the leap
                Assert.assertEquals(61, utc.minuteDuration(t0.shiftedBy(dt)));
            } else {
                // after the minute of the leap
                Assert.assertEquals(60, utc.minuteDuration(t0.shiftedBy(dt)));
            }
        }
    }

    @Test
    public void testSymmetry() {
        TimeScale scale = TimeScalesFactory.getGPS();
        for (double dt = -10000; dt < 10000; dt += 123.456789) {
            AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(dt * Constants.JULIAN_DAY);
            double dt1 = scale.offsetFromTAI(date);
            DateTimeComponents components = date.getComponents(scale);
            double dt2 = scale.offsetToTAI(components.getDate(), components.getTime());
            Assert.assertEquals( 0.0, dt1 + dt2, 1.0e-10);
        }
    }

    @Test
    public void testOffsets() {

        // we arbitrary put UTC == TAI before 1961-01-01
        checkOffset(1950,  1,  1,   0);

        // excerpt from UTC-TAI.history file:
        //  1961  Jan.  1 - 1961  Aug.  1     1.422 818 0s + (MJD - 37 300) x 0.001 296s
        //        Aug.  1 - 1962  Jan.  1     1.372 818 0s +        ""
        //  1962  Jan.  1 - 1963  Nov.  1     1.845 858 0s + (MJD - 37 665) x 0.001 123 2s
        //  1963  Nov.  1 - 1964  Jan.  1     1.945 858 0s +        ""
        //  1964  Jan.  1 -       April 1     3.240 130 0s + (MJD - 38 761) x 0.001 296s
        //        April 1 -       Sept. 1     3.340 130 0s +        ""
        //        Sept. 1 - 1965  Jan.  1     3.440 130 0s +        ""
        //  1965  Jan.  1 -       March 1     3.540 130 0s +        ""
        //        March 1 -       Jul.  1     3.640 130 0s +        ""
        //        Jul.  1 -       Sept. 1     3.740 130 0s +        ""
        //        Sept. 1 - 1966  Jan.  1     3.840 130 0s +        ""
        //  1966  Jan.  1 - 1968  Feb.  1     4.313 170 0s + (MJD - 39 126) x 0.002 592s
        //  1968  Feb.  1 - 1972  Jan.  1     4.213 170 0s +        ""
        checkOffset(1961,  1,  2,  -(1.422818 +   1 * 0.001296));  // MJD 37300 +   1
        checkOffset(1961,  8,  2,  -(1.372818 + 213 * 0.001296));  // MJD 37300 + 213
        checkOffset(1962,  1,  2,  -(1.845858 +   1 * 0.0011232)); // MJD 37665 +   1
        checkOffset(1963, 11,  2,  -(1.945858 + 670 * 0.0011232)); // MJD 37665 + 670
        checkOffset(1964,  1,  2,  -(3.240130 - 365 * 0.001296));  // MJD 38761 - 365
        checkOffset(1964,  4,  2,  -(3.340130 - 274 * 0.001296));  // MJD 38761 - 274
        checkOffset(1964,  9,  2,  -(3.440130 - 121 * 0.001296));  // MJD 38761 - 121
        checkOffset(1965,  1,  2,  -(3.540130 +   1 * 0.001296));  // MJD 38761 +   1
        checkOffset(1965,  3,  2,  -(3.640130 +  60 * 0.001296));  // MJD 38761 +  60
        checkOffset(1965,  7,  2,  -(3.740130 + 182 * 0.001296));  // MJD 38761 + 182
        checkOffset(1965,  9,  2,  -(3.840130 + 244 * 0.001296));  // MJD 38761 + 244
        checkOffset(1966,  1,  2,  -(4.313170 +   1 * 0.002592));  // MJD 39126 +   1
        checkOffset(1968,  2,  2,  -(4.213170 + 762 * 0.002592));  // MJD 39126 + 762

        // since 1972-01-01, offsets are only whole seconds
        checkOffset(1972,  3,  5, -10);
        checkOffset(1972,  7, 14, -11);
        checkOffset(1979, 12, 31, -18);
        checkOffset(1980,  1, 22, -19);
        checkOffset(2006,  7,  7, -33);

    }

    private void checkOffset(int year, int month, int day, double offset) {
        AbsoluteDate date = new AbsoluteDate(year, month, day, utc);
        Assert.assertEquals(offset, utc.offsetFromTAI(date), 1.0e-10);
    }

    @Test
    public void testCreatingInLeapDateUTC() {
        AbsoluteDate previous = null;
        final double step = 0.0625;
        for (double seconds = 59.0; seconds < 61.0; seconds += step) {
            final AbsoluteDate date = new AbsoluteDate(2008, 12, 31, 23, 59, seconds, utc);
            if (previous != null) {
                Assert.assertEquals(step, date.durationFrom(previous), 1.0e-12);
            }
            previous = date;
        }
        AbsoluteDate ad0 = new AbsoluteDate("2008-12-31T23:59:60", utc);
        Assert.assertTrue(ad0.toString(utc).startsWith("2008-12-31T23:59:"));
        AbsoluteDate ad1 = new AbsoluteDate("2008-12-31T23:59:59", utc).shiftedBy(1);
        Assert.assertEquals(0, ad1.durationFrom(ad0), 1.0e-15);
        Assert.assertEquals(1, new AbsoluteDate("2009-01-01T00:00:00", utc).durationFrom(ad0), 1.0e-15);
        Assert.assertEquals(2, new AbsoluteDate("2009-01-01T00:00:01", utc).durationFrom(ad0), 1.0e-15);
    }

    @Test
    public void testCreatingInLeapDateLocalTime50HoursWest() {
        // yes, I know, there are no time zones 50 hours West of UTC, this is a stress test
        AbsoluteDate previous = null;
        final double step = 0.0625;
        for (double seconds = 59.0; seconds < 61.0; seconds += step) {
            final AbsoluteDate date = new AbsoluteDate(new DateComponents(2008, 12, 29),
                                                       new TimeComponents(21, 59, seconds, -50 * 60),
                                                       utc);
            if (previous != null) {
                Assert.assertEquals(step, date.durationFrom(previous), 1.0e-12);
            }
            previous = date;
        }
        AbsoluteDate ad0 = new AbsoluteDate("2008-12-29T21:59:60-50:00", utc);
        Assert.assertTrue(ad0.toString(utc).startsWith("2008-12-31T23:59:"));
        AbsoluteDate ad1 = new AbsoluteDate("2008-12-29T21:59:59-50:00", utc).shiftedBy(1);
        Assert.assertEquals(0, ad1.durationFrom(ad0), 1.0e-15);
        Assert.assertEquals(1, new AbsoluteDate("2008-12-29T22:00:00-50:00", utc).durationFrom(ad0), 1.0e-15);
        Assert.assertEquals(2, new AbsoluteDate("2008-12-29T22:00:01-50:00", utc).durationFrom(ad0), 1.0e-15);
    }

    @Test
    public void testCreatingInLeapDateLocalTime50HoursEast() {
        // yes, I know, there are no time zones 50 hours East of UTC, this is a stress test
        AbsoluteDate previous = null;
        final double step = 0.0625;
        for (double seconds = 59.0; seconds < 61.0; seconds += step) {
            final AbsoluteDate date = new AbsoluteDate(new DateComponents(2009, 1, 3),
                                                       new TimeComponents(1, 59, seconds, +50 * 60),
                                                       utc);
            if (previous != null) {
                Assert.assertEquals(step, date.durationFrom(previous), 1.0e-12);
            }
            previous = date;
        }
        AbsoluteDate ad0 = new AbsoluteDate("2009-01-03T01:59:60+50:00", utc);
        Assert.assertTrue(ad0.toString(utc).startsWith("2008-12-31T23:59:"));
        AbsoluteDate ad1 = new AbsoluteDate("2009-01-03T01:59:59+50:00", utc).shiftedBy(1);
        Assert.assertEquals(0, ad1.durationFrom(ad0), 1.0e-15);
        Assert.assertEquals(1, new AbsoluteDate("2009-01-03T02:00:00+50:00", utc).durationFrom(ad0), 1.0e-15);
        Assert.assertEquals(2, new AbsoluteDate("2009-01-03T02:00:01+50:00", utc).durationFrom(ad0), 1.0e-15);
    }

    @Test
    public void testDisplayDuringLeap() throws OrekitException {
        AbsoluteDate t0 = utc.getLastKnownLeapSecond().shiftedBy(-1.0);
        for (double dt = 0.0; dt < 3.0; dt += 0.375) {
            AbsoluteDate t = t0.shiftedBy(dt);
            double seconds = t.getComponents(utc).getTime().getSecond();
            if (dt < 2.0) {
                Assert.assertEquals(dt + 59.0, seconds, 1.0e-12);
            } else {
                Assert.assertEquals(dt - 2.0, seconds, 1.0e-12);
            }
        }
    }

    @Test
    public void testMultithreading() {

        // generate reference offsets using a single thread
        RandomGenerator random = new Well1024a(6392073424l);
        List<AbsoluteDate> datesList = new ArrayList<AbsoluteDate>();
        List<Double> offsetsList = new ArrayList<Double>();
        AbsoluteDate reference = utc.getFirstKnownLeapSecond().shiftedBy(-Constants.JULIAN_YEAR);
        double testRange = utc.getLastKnownLeapSecond().durationFrom(reference) + Constants.JULIAN_YEAR;
        for (int i = 0; i < 10000; ++i) {
            AbsoluteDate randomDate = reference.shiftedBy(random.nextDouble() * testRange);
            datesList.add(randomDate);
            offsetsList.add(utc.offsetFromTAI(randomDate));
        }

        // check the offsets in multi-threaded mode
        ExecutorService executorService = Executors.newFixedThreadPool(100);

        for (int i = 0; i < datesList.size(); ++i) {
            final AbsoluteDate date = datesList.get(i);
            final double offset = offsetsList.get(i);
            executorService.execute(new Runnable() {
                public void run() {
                    Assert.assertEquals(offset, utc.offsetFromTAI(date), 1.0e-12);
                }
            });
        }

        try {
            executorService.shutdown();
            executorService.awaitTermination(3, TimeUnit.SECONDS);
        } catch (InterruptedException ie) {
            Assert.fail(ie.getLocalizedMessage());
        }

    }

    @Test
    public void testIssue89() throws OrekitException {
        AbsoluteDate firstDayLastLeap = utc.getLastKnownLeapSecond().shiftedBy(10.0);
        AbsoluteDate rebuilt = new AbsoluteDate(firstDayLastLeap.toString(utc), utc);
        Assert.assertEquals(0.0, rebuilt.durationFrom(firstDayLastLeap), 1.0e-12);
    }

    @Test
    public void testOffsetToTAIBeforeFirstLeapSecond() throws OrekitException {
        TimeScale scale = TimeScalesFactory.getUTC();
        // time before first leap second
        DateComponents dateComponents = new DateComponents(1950, 1, 1);
        double actual = scale.offsetToTAI(dateComponents, TimeComponents.H00);
        Assert.assertEquals(0.0, actual, 1.0e-10);
    }

    @Test
    public void testEmptyOffsets() throws Exception {
        Utils.setDataRoot("no-data");

        TimeScalesFactory.addUTCTAIOffsetsLoader(new UTCTAIOffsetsLoader() {
            public List<OffsetModel> loadOffsets() {
                return Collections.emptyList();
            }
        });

        try {
            TimeScalesFactory.getUTC();
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.NO_IERS_UTC_TAI_HISTORY_DATA_LOADED, oe.getSpecifier());
        }

    }

    @Test
    public void testSerialization() throws OrekitException, IOException, ClassNotFoundException {
        UTCScale utc = TimeScalesFactory.getUTC();

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream    oos = new ObjectOutputStream(bos);
        oos.writeObject(utc);

        Assert.assertTrue(bos.size() > 50);
        Assert.assertTrue(bos.size() < 100);

        ByteArrayInputStream  bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream     ois = new ObjectInputStream(bis);
        UTCScale deserialized  = (UTCScale) ois.readObject();
        Assert.assertTrue(utc == deserialized);

    }

    @Before
    public void setUp() throws OrekitException {
        Utils.setDataRoot("regular-data");
        utc = TimeScalesFactory.getUTC();
    }

    @After
    public void tearDown() {
        utc = null;
    }

    private UTCScale utc;

}
