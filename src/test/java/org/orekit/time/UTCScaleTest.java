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



import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;

public class UTCScaleTest {

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
        Assert.assertEquals("1983-06-30T23:59:59.000", d.toString(utc));
        d = d.shiftedBy(0.251);
        Assert.assertEquals("1983-06-30T23:59:59.251", d.toString(utc));
        d = d.shiftedBy(0.251);
        Assert.assertEquals("1983-06-30T23:59:59.502", d.toString(utc));
        d = d.shiftedBy(0.251);
        Assert.assertEquals("1983-06-30T23:59:59.753", d.toString(utc));
        d = d.shiftedBy( 0.251);
        Assert.assertEquals("1983-06-30T23:59:60.004", d.toString(utc));
        d = d.shiftedBy(0.251);
        Assert.assertEquals("1983-06-30T23:59:60.255", d.toString(utc));
        d = d.shiftedBy(0.251);
        Assert.assertEquals("1983-06-30T23:59:60.506", d.toString(utc));
        d = d.shiftedBy(0.251);
        Assert.assertEquals("1983-06-30T23:59:60.757", d.toString(utc));
        d = d.shiftedBy(0.251);
        Assert.assertEquals("1983-07-01T00:00:00.008", d.toString(utc));
    }

    @Test
    public void testSymmetry() {
        TimeScale scale = TimeScalesFactory.getGPS();
        for (double dt = -10000; dt < 10000; dt += 123.456789) {
            AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(dt * 86400);
            double dt1 = scale.offsetFromTAI(date);
            DateTimeComponents components = date.getComponents(scale);
            double dt2 = scale.offsetToTAI(components.getDate(), components.getTime());
            Assert.assertEquals( 0.0, dt1 + dt2, 1.0e-10);
        }
    }

    @Test
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
        Assert.assertEquals(offset, utc.offsetFromTAI(date), 1.0e-10);
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

    private TimeScale utc;

}
