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

import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.utils.Constants;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GalileoScaleTest {

    @Test
    void testT0() {
        TimeScale scale = TimeScalesFactory.getGST();
        assertEquals("GST", scale.toString());
        AbsoluteDate t0 =
            new AbsoluteDate(new DateComponents(1999, 8, 22), TimeComponents.H00, scale);
        assertEquals(AbsoluteDate.GALILEO_EPOCH, t0);
    }

    @Test
    void test2006() {
        AbsoluteDate tGalileo =
            new AbsoluteDate(new DateComponents(2006, 1, 2), TimeComponents.H00, TimeScalesFactory.getGST());
        AbsoluteDate tUTC =
            new AbsoluteDate(new DateComponents(2006, 1, 1), new TimeComponents(23, 59, 46),
                             TimeScalesFactory.getUTC());
        assertEquals(tUTC, tGalileo);
    }

    @Test
    void testDuringLeap() {
        final TimeScale utc   = TimeScalesFactory.getUTC();
        final TimeScale scale = TimeScalesFactory.getGST();
        final AbsoluteDate before = new AbsoluteDate(new DateComponents(1983, 6, 30),
                                                     new TimeComponents(23, 59, 59),
                                                     utc);
        final AbsoluteDate during = before.shiftedBy(1.25);
        assertEquals(61, utc.minuteDuration(during));
        assertEquals(1.0, utc.getLeap(during), 1.0e-10);
        assertEquals(60, scale.minuteDuration(during));
        assertEquals(0.0, scale.getLeap(during), 1.0e-10);
    }

    @Test
    void testConstant() {
        TimeScale scale = TimeScalesFactory.getGST();
        double reference = scale.offsetFromTAI(AbsoluteDate.J2000_EPOCH);
        for (double dt = -10000; dt < 10000; dt += 123.456789) {
            AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(dt * Constants.JULIAN_DAY);
            assertEquals(reference, scale.offsetFromTAI(date), 1.0e-15);
        }
    }

    @Test
    void testField() {
        TimeScale scale = TimeScalesFactory.getGST();
        double reference = scale.offsetFromTAI(AbsoluteDate.J2000_EPOCH);
        final Binary64Field field = Binary64Field.getInstance();
        for (double dt = -10000; dt < 10000; dt += 123.456789) {
            FieldAbsoluteDate<Binary64> date = FieldAbsoluteDate.getJ2000Epoch(field).
                                               shiftedBy(dt * Constants.JULIAN_DAY);
            assertEquals(reference, scale.offsetFromTAI(date).getReal(), 1.0e-15);
        }
    }

    @Test
    void testSameAsGPS() {
        TimeScale gst = TimeScalesFactory.getGST();
        TimeScale gps = TimeScalesFactory.getGPS();
        for (double dt = -10000; dt < 10000; dt += 123.456789) {
            AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(dt * Constants.JULIAN_DAY);
            assertEquals(gps.offsetFromTAI(date), gst.offsetFromTAI(date), 1.0e-15);
        }
    }

    @Test
    void testSymmetry() {
        TimeScale scale = TimeScalesFactory.getGST();
        for (double dt = -10000; dt < 10000; dt += 123.456789) {
            AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(dt * Constants.JULIAN_DAY);
            double dt1 = scale.offsetFromTAI(date);
            DateTimeComponents components = date.getComponents(scale);
            double dt2 = scale.offsetToTAI(components.getDate(), components.getTime());
            assertEquals( 0.0, dt1 + dt2, 1.0e-10);
        }
    }

    @BeforeEach
    void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
