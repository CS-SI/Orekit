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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import org.orekit.errors.OrekitException;
import org.orekit.iers.IERSDirectoryCrawler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChunkedDate;
import org.orekit.time.ChunkedTime;
import org.orekit.time.TimeScale;
import org.orekit.time.UTCScale;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class UTCScaleTest
extends TestCase {

    public UTCScaleTest(String name) {
        super(name);
        utc = null;
    }

    public void testNoLeap() throws ParseException {
        AbsoluteDate d1 = new AbsoluteDate(new ChunkedDate(1999, 12, 31),
                                           new ChunkedTime(23, 59, 59),
                                           utc);
        AbsoluteDate d2 = new AbsoluteDate(new ChunkedDate(2000, 01, 01),
                                           new ChunkedTime(00, 00, 01),
                                           utc);
        assertEquals(2.0, d2.minus(d1), 1.0e-10);
    }

    public void testLeap2006() throws ParseException {
        AbsoluteDate leapDate =
            new AbsoluteDate(new ChunkedDate(2006, 01, 01), ChunkedTime.H00, utc);
        AbsoluteDate d1 = new AbsoluteDate(leapDate, -1);
        AbsoluteDate d2 = new AbsoluteDate(leapDate, +1);
        assertEquals(2.0, d2.minus(d1), 1.0e-10);

        AbsoluteDate d3 = new AbsoluteDate(new ChunkedDate(2005, 12, 31),
                                           new ChunkedTime(23, 59, 59),
                                           utc);
        AbsoluteDate d4 = new AbsoluteDate(new ChunkedDate(2006, 01, 01),
                                           new ChunkedTime(00, 00, 01),
                                           utc);
        assertEquals(3.0, d4.minus(d3), 1.0e-10);

    }

    public void testDuringLeap() throws ParseException {
        AbsoluteDate d = new AbsoluteDate(new ChunkedDate(1983, 06, 30),
                                          new ChunkedTime(23, 59, 59),
                                          utc);
        assertEquals("1983-06-30T23:59:59.000", d.toString(utc));
        d = new AbsoluteDate(d, 0.251);
        assertEquals("1983-06-30T23:59:59.251", d.toString(utc));
        d = new AbsoluteDate(d, 0.251);
        assertEquals("1983-06-30T23:59:59.502", d.toString(utc));
        d = new AbsoluteDate(d, 0.251);
        assertEquals("1983-06-30T23:59:59.753", d.toString(utc));
        d = new AbsoluteDate(d, 0.251);
        assertEquals("1983-06-30T23:59:59.004", d.toString(utc));
        d = new AbsoluteDate(d, 0.251);
        assertEquals("1983-06-30T23:59:59.255", d.toString(utc));
        d = new AbsoluteDate(d, 0.251);
        assertEquals("1983-06-30T23:59:59.506", d.toString(utc));
        d = new AbsoluteDate(d, 0.251);
        assertEquals("1983-06-30T23:59:59.757", d.toString(utc));
    }

    public void testSymetry() {
        // the loop is around the 1977-01-01 leap second introduction
        double tLeap = 220924815;
        TimeScale scale = utc;
        assertEquals("UTC", scale.toString());
        boolean insideTested = false;
        for (double taiTime = tLeap - 60; taiTime < tLeap + 60; taiTime += 0.3) {
            double dt1 = scale.offsetFromTAI(taiTime);
            double dt2 = scale.offsetToTAI(taiTime + dt1);
            if ((taiTime > tLeap) && (taiTime <= tLeap + 1.0)) {
                // we are "inside" the leap second, the TAI scale goes on
                // but the UTC scale "replays" the previous second, before the step
                insideTested = true;
                assertEquals(-1.0, dt1 + dt2, 1.0e-10);
            } else {
                assertEquals( 0.0, dt1 + dt2, 1.0e-10);
            }
        }
        assertTrue(insideTested);
    }

    public void testOffsets() throws ParseException {
        checkOffset("1970-01-01",   0);
        checkOffset("1972-03-05", -10);
        checkOffset("1972-07-14", -11);
        checkOffset("1979-12-31", -18);
        checkOffset("1980-01-22", -19);
        checkOffset("2006-07-07", -33);
    }

    private void checkOffset(String date, double offset) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            double time = format.parse(date).getTime() * 0.001;
            assertEquals(offset, utc.offsetFromTAI(time), 1.0e-10);
        } catch (ParseException pe) {
            fail(pe.getMessage());
        }
    }

    public void setUp() throws OrekitException {
        System.setProperty(IERSDirectoryCrawler.IERS_ROOT_DIRECTORY, "regular-data");
        utc = UTCScale.getInstance();
    }

    public static Test suite() {
        return new TestSuite(UTCScaleTest.class);
    }

    private TimeScale utc;

}
