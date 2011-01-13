/* Copyright 2002-2011 CS Communication & Systèmes
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
import org.orekit.utils.Constants;

public class UT1ScaleTest {

    @Test
    public void testLeap2006() throws OrekitException {
        TimeScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate dateA = new AbsoluteDate(2005, 12, 30, 23, 59,  0.0, utc);
        AbsoluteDate dateB = new AbsoluteDate(2006,  1,  2,  0,  1,  0.0, utc);
        double deltaAUT1 = ut1.offsetFromTAI(dateA);
        double deltaAUTC = utc.offsetFromTAI(dateA);
        double deltaBUT1 = ut1.offsetFromTAI(dateB);
        double deltaBUTC = utc.offsetFromTAI(dateB);

        // there is a leap second between the two dates
        Assert.assertEquals(deltaAUTC - 1.0, deltaBUTC, 1.0e-15);

        // the leap second induces UTC goes from above UT1 to below UT1
        Assert.assertTrue(deltaAUTC > deltaAUT1);
        Assert.assertTrue(deltaBUTC < deltaBUT1);

        // UT1 is continuous, so change should be very small in two days
        Assert.assertEquals(deltaAUT1, deltaBUT1, 3.0e-4);

    }

    @Test
    public void testSymmetry() throws OrekitException {
        for (double dt = -10000; dt < 10000; dt += 123.456789) {
            AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(dt * Constants.JULIAN_DAY);
            double dt1 = ut1.offsetFromTAI(date);
            DateTimeComponents components = date.getComponents(ut1);
            double dt2 = ut1.offsetToTAI(components.getDate(), components.getTime());
            Assert.assertEquals( 0.0, dt1 + dt2, 1.0e-10);
        }
    }

    @Before
    public void setUp() throws OrekitException {
        Utils.setDataRoot("regular-data");
        ut1 = TimeScalesFactory.getUT1();
    }

    @After
    public void tearDown() {
        ut1 = null;
    }

    private TimeScale ut1;

}
