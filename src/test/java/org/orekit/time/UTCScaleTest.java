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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.orekit.data.DataDirectoryCrawler;
import org.orekit.errors.OrekitException;

public class UTCScaleTest
extends TestCase {

    public UTCScaleTest(String name) {
        super(name);
        utc = null;
    }

    public void testNoLeap() {
        AbsoluteDate d1 = new AbsoluteDate(new ChunkedDate(1999, 12, 31),
                                           new ChunkedTime(23, 59, 59),
                                           utc);
        AbsoluteDate d2 = new AbsoluteDate(new ChunkedDate(2000, 01, 01),
                                           new ChunkedTime(00, 00, 01),
                                           utc);
        assertEquals(2.0, d2.minus(d1), 1.0e-10);
    }

    public void testLeap2006() {
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

    public void testDuringLeap() {
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
        assertEquals("1983-06-30T23:59:60.004", d.toString(utc));
        d = new AbsoluteDate(d, 0.251);
        assertEquals("1983-06-30T23:59:60.255", d.toString(utc));
        d = new AbsoluteDate(d, 0.251);
        assertEquals("1983-06-30T23:59:60.506", d.toString(utc));
        d = new AbsoluteDate(d, 0.251);
        assertEquals("1983-06-30T23:59:60.757", d.toString(utc));
        d = new AbsoluteDate(d, 0.251);
        assertEquals("1983-07-01T00:00:00.008", d.toString(utc));
    }

    public void testSymmetry() {
        TimeScale scale = GPSScale.getInstance();
        for (double dt = -10000; dt < 10000; dt += 123.456789) {
            AbsoluteDate date = new AbsoluteDate(AbsoluteDate.J2000_EPOCH, dt * 86400);
            double dt1 = scale.offsetFromTAI(date);
            ChunksPair chunks = date.getChunks(scale);
            double dt2 = scale.offsetToTAI(chunks.getDate(), chunks.getTime());
            assertEquals( 0.0, dt1 + dt2, 1.0e-10);
        }
    }

    public void testOffsets() {
        checkOffset(1970, 01, 01,   0);
        checkOffset(1972, 03, 05, -10);
        checkOffset(1972, 07, 14, -11);
        checkOffset(1979, 12, 31, -18);
        checkOffset(1980, 01, 22, -19);
        checkOffset(2006, 07, 07, -33);
    }

    private void checkOffset(int year, int month, int day, double offset) {
        AbsoluteDate date = new AbsoluteDate(year, month, day, utc);
        assertEquals(offset, utc.offsetFromTAI(date), 1.0e-10);
    }

    public void setUp() throws OrekitException {
        System.setProperty(DataDirectoryCrawler.DATA_ROOT_DIRECTORY, "regular-data");
        utc = UTCScale.getInstance();
    }

    public static Test suite() {
        return new TestSuite(UTCScaleTest.class);
    }

    private TimeScale utc;

}
