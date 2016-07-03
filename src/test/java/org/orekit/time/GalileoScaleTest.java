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
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.utils.Constants;

public class GalileoScaleTest {

    @Test
    public void testT0() {
        TimeScale scale = TimeScalesFactory.getGST();
        Assert.assertEquals("GST", scale.toString());
        AbsoluteDate t0 =
            new AbsoluteDate(new DateComponents(1999, 8, 22), new TimeComponents(0, 0, 13), scale);
        Assert.assertEquals(AbsoluteDate.GALILEO_EPOCH, t0);
    }

    @Test
    public void test2006() throws OrekitException {
        AbsoluteDate tGalileo =
            new AbsoluteDate(new DateComponents(2006, 1, 2), TimeComponents.H00, TimeScalesFactory.getGST());
        AbsoluteDate tUTC =
            new AbsoluteDate(new DateComponents(2006, 1, 1), new TimeComponents(23, 59, 46),
                             TimeScalesFactory.getUTC());
        Assert.assertEquals(tUTC, tGalileo);
    }

    @Test
    public void testDuringLeap() throws OrekitException {
        final TimeScale utc   = TimeScalesFactory.getUTC();
        final TimeScale scale = TimeScalesFactory.getGST();
        final AbsoluteDate before = new AbsoluteDate(new DateComponents(1983, 06, 30),
                                                     new TimeComponents(23, 59, 59),
                                                     utc);
        final AbsoluteDate during = before.shiftedBy(1.25);
        Assert.assertEquals(61, utc.minuteDuration(during));
        Assert.assertEquals(1.0, utc.getLeap(during), 1.0e-10);
        Assert.assertEquals(60, scale.minuteDuration(during));
        Assert.assertEquals(0.0, scale.getLeap(during), 1.0e-10);
    }

    @Test
    public void testConstant() {
        TimeScale scale = TimeScalesFactory.getGST();
        double reference = scale.offsetFromTAI(AbsoluteDate.J2000_EPOCH);
        for (double dt = -10000; dt < 10000; dt += 123.456789) {
            AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(dt * Constants.JULIAN_DAY);
            Assert.assertEquals(reference, scale.offsetFromTAI(date), 1.0e-15);
        }
    }

    @Test
    public void testSameAsGPS() {
        TimeScale gst = TimeScalesFactory.getGST();
        TimeScale gps = TimeScalesFactory.getGPS();
        for (double dt = -10000; dt < 10000; dt += 123.456789) {
            AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(dt * Constants.JULIAN_DAY);
            Assert.assertEquals(gps.offsetFromTAI(date), gst.offsetFromTAI(date), 1.0e-15);
        }
    }

    @Test
    public void testSymmetry() {
        TimeScale scale = TimeScalesFactory.getGST();
        for (double dt = -10000; dt < 10000; dt += 123.456789) {
            AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(dt * Constants.JULIAN_DAY);
            double dt1 = scale.offsetFromTAI(date);
            DateTimeComponents components = date.getComponents(scale);
            double dt2 = scale.offsetToTAI(components.getDate(), components.getTime());
            Assert.assertEquals( 0.0, dt1 + dt2, 1.0e-10);
        }
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
