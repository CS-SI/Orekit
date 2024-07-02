/* Copyright 2002-2024 CS GROUP
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.utils.Constants;

public class QZSSScaleTest {

    @Test
    public void testT0() {
        TimeScale scale = TimeScalesFactory.getQZSS();
        Assertions.assertEquals("QZSS", scale.toString());
        AbsoluteDate t0 =
            new AbsoluteDate(new DateComponents(1980, 1, 6), TimeComponents.H00, scale);
        Assertions.assertEquals(AbsoluteDate.GPS_EPOCH, t0);
    }

    @Test
    public void testArbitrary() {
        AbsoluteDate tQZSS =
            new AbsoluteDate(new DateComponents(1999, 3, 4), TimeComponents.H00, TimeScalesFactory.getQZSS());
        AbsoluteDate tUTC =
            new AbsoluteDate(new DateComponents(1999, 3, 3), new TimeComponents(23, 59, 47),
                             TimeScalesFactory.getUTC());
        Assertions.assertEquals(tUTC, tQZSS);
    }

    @Test
    public void testConstant() {
        TimeScale scale = TimeScalesFactory.getQZSS();
        double reference = scale.offsetFromTAI(AbsoluteDate.J2000_EPOCH);
        for (double dt = -10000; dt < 10000; dt += 123.456789) {
            AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(dt * Constants.JULIAN_DAY);
            Assertions.assertEquals(reference, scale.offsetFromTAI(date), 1.0e-15);
        }
    }

    @Test
    public void testSymmetry() {
        TimeScale scale = TimeScalesFactory.getQZSS();
        for (double dt = -10000; dt < 10000; dt += 123.456789) {
            AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(dt * Constants.JULIAN_DAY);
            double dt1 = scale.offsetFromTAI(date);
            DateTimeComponents components = date.getComponents(scale);
            double dt2 = scale.offsetToTAI(components.getDate(), components.getTime());
            Assertions.assertEquals( 0.0, dt1 + dt2, 1.0e-10);
        }
    }

    @Test
    public void testDuringLeap() {
        final TimeScale utc   = TimeScalesFactory.getUTC();
        final TimeScale scale = TimeScalesFactory.getQZSS();
        final AbsoluteDate before = new AbsoluteDate(new DateComponents(1983, 06, 30),
                                                     new TimeComponents(23, 59, 59),
                                                     utc);
        final AbsoluteDate during = before.shiftedBy(1.25);
        Assertions.assertEquals(61, utc.minuteDuration(during));
        Assertions.assertEquals(1.0, utc.getLeap(during), 1.0e-10);
        Assertions.assertEquals(60, scale.minuteDuration(during));
        Assertions.assertEquals(0.0, scale.getLeap(during), 1.0e-10);
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
