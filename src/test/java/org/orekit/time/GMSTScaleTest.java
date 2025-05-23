/* Copyright 2002-2025 CS GROUP
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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;


public class GMSTScaleTest {

    @Test
    // reference: http://www.astro.umd.edu/~jph/GST_eqn.pdf
    public void testReference() {
        Assertions.assertEquals("GMST", gmst.toString());
        AbsoluteDate date = new AbsoluteDate(2001, 10, 3, 6, 30, 0.0,
                                             TimeScalesFactory.getUT1(IERSConventions.IERS_2010, true));
        DateTimeComponents gmstComponents = date.getComponents(gmst);
        Assertions.assertEquals(2001,  gmstComponents.getDate().getYear());
        Assertions.assertEquals( 10,   gmstComponents.getDate().getMonth());
        Assertions.assertEquals(  3,   gmstComponents.getDate().getDay());
        Assertions.assertEquals(  7,   gmstComponents.getTime().getHour());
        Assertions.assertEquals( 18,   gmstComponents.getTime().getMinute());
        Assertions.assertEquals(8.329, gmstComponents.getTime().getSecond(), 4.0e-4);
    }

    @Test
    public void testSymmetry() {
        for (double dt = -10000; dt < 10000; dt += 123.456789) {
            AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(dt * Constants.JULIAN_DAY);
            double dt1 = gmst.offsetFromTAI(date).toDouble();
            DateTimeComponents components = date.getComponents(gmst);
            double dt2 = gmst.offsetToTAI(components.getDate(), components.getTime()).toDouble();
            Assertions.assertEquals( 0.0, dt1 + dt2, 1.0e-10);
        }
    }

    @Test
    public void testDuringLeap() {
        final TimeScale utc   = TimeScalesFactory.getUTC();
        final TimeScale scale = gmst;
        final AbsoluteDate before = new AbsoluteDate(new DateComponents(1983, 6, 30),
                                                     new TimeComponents(23, 59, 59),
                                                     utc);
        final AbsoluteDate during = before.shiftedBy(1.25);
        Assertions.assertEquals(61, utc.minuteDuration(during));
        Assertions.assertEquals(1.0, utc.getLeap(during).toDouble(), 1.0e-10);
        Assertions.assertEquals(60, scale.minuteDuration(during));
        Assertions.assertEquals(0.0, scale.getLeap(during).toDouble(), 1.0e-10);
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
        gmst = TimeScalesFactory.getGMST(IERSConventions.IERS_2010, false);
    }

    @AfterEach
    public void tearDown() {
        gmst = null;
    }

    private TimeScale gmst;

}
