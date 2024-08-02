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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.utils.Constants;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GLONASSScaleTest {

    private GLONASSScale glonass;

    @Test
    void testT0() {
        TimeScale scale = TimeScalesFactory.getGLONASS();
        assertEquals("GLONASS", scale.toString());
        AbsoluteDate t0 =
            new AbsoluteDate(new DateComponents(1996, 1, 1), TimeComponents.H00, scale);
        assertEquals(AbsoluteDate.GLONASS_EPOCH, t0);
    }

    @Test
    void testArbitrary() {
        AbsoluteDate tGLONASS =
            new AbsoluteDate(new DateComponents(1999, 3, 4), TimeComponents.H00, glonass);
        AbsoluteDate tUTC =
            new AbsoluteDate(new DateComponents(1999, 3, 3), new TimeComponents(21, 0, 0),
                             TimeScalesFactory.getUTC());
        assertEquals(tUTC, tGLONASS);
    }

    @Test
    void testLeap2006() {
        final UTCScale utc = TimeScalesFactory.getUTC();
        AbsoluteDate leapDate =
            new AbsoluteDate(new DateComponents(2006, 1, 1), TimeComponents.H00, utc);
        AbsoluteDate d1 = leapDate.shiftedBy(-1);
        AbsoluteDate d2 = leapDate.shiftedBy(+1);
        assertEquals(2.0, d2.durationFrom(d1), 1.0e-10);

        AbsoluteDate d3 = new AbsoluteDate(new DateComponents(2006, 1, 1),
                                           new TimeComponents(02, 59, 59),
                                           glonass);
        assertEquals(new AbsoluteDate(new DateComponents(2005, 12, 31),
                                             new TimeComponents(23, 59, 59),
                                             utc),
                            d3);
        AbsoluteDate d4 = new AbsoluteDate(new DateComponents(2006, 1, 1),
                                           new TimeComponents(3, 0, 1),
                                           glonass);
        assertEquals(new AbsoluteDate(new DateComponents(2006, 1, 1),
                                             new TimeComponents(0, 0, 1),
                                             utc),
                            d4);
        assertEquals(3.0, d4.durationFrom(d3), 1.0e-10);
    }

    @Test
    void testDuringLeap() {
        AbsoluteDate d = new AbsoluteDate(new DateComponents(1983, 06, 30),
                                          new TimeComponents(23, 59, 59),
                                          TimeScalesFactory.getUTC());
        assertEquals("1983-07-01T02:58:59.000", d.shiftedBy(-60).toString(glonass));
        assertEquals(60, glonass.minuteDuration(d.shiftedBy(-60)));
        assertFalse(glonass.insideLeap(d.shiftedBy(-60)));
        assertEquals("1983-07-01T02:59:59.000", d.toString(glonass));
        assertEquals(61, glonass.minuteDuration(d));
        assertFalse(glonass.insideLeap(d));
        d = d.shiftedBy(0.251);
        assertEquals("1983-07-01T02:59:59.251", d.toString(glonass));
        assertEquals(61, glonass.minuteDuration(d));
        assertFalse(glonass.insideLeap(d));
        d = d.shiftedBy(0.251);
        assertEquals("1983-07-01T02:59:59.502", d.toString(glonass));
        assertEquals(61, glonass.minuteDuration(d));
        assertFalse(glonass.insideLeap(d));
        d = d.shiftedBy(0.251);
        assertEquals("1983-07-01T02:59:59.753", d.toString(glonass));
        assertEquals(61, glonass.minuteDuration(d));
        assertFalse(glonass.insideLeap(d));
        d = d.shiftedBy( 0.251);
        assertEquals("1983-07-01T02:59:60.004", d.toString(glonass));
        assertEquals(61, glonass.minuteDuration(d));
        assertTrue(glonass.insideLeap(d));
        d = d.shiftedBy(0.251);
        assertEquals("1983-07-01T02:59:60.255", d.toString(glonass));
        assertEquals(61, glonass.minuteDuration(d));
        assertTrue(glonass.insideLeap(d));
        d = d.shiftedBy(0.251);
        assertEquals("1983-07-01T02:59:60.506", d.toString(glonass));
        assertEquals(61, glonass.minuteDuration(d));
        assertTrue(glonass.insideLeap(d));
        d = d.shiftedBy(0.251);
        assertEquals("1983-07-01T02:59:60.757", d.toString(glonass));
        assertEquals(61, glonass.minuteDuration(d));
        assertTrue(glonass.insideLeap(d));
        d = d.shiftedBy(0.251);
        assertEquals("1983-07-01T03:00:00.008", d.toString(glonass));
        assertEquals(60, glonass.minuteDuration(d));
        assertFalse(glonass.insideLeap(d));
    }

    @Test
    void testSymmetry() {
        for (double dt = -10000; dt < 10000; dt += 123.456789) {
            AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(dt * Constants.JULIAN_DAY);
            double dt1 = glonass.offsetFromTAI(date);
            DateTimeComponents components = date.getComponents(glonass);
            double dt2 = glonass.offsetToTAI(components.getDate(), components.getTime());
            assertEquals( 0.0, dt1 + dt2, 1.0e-10);
        }
    }

    @Test
    void testWrapBeforeLeap() {
        AbsoluteDate t = new AbsoluteDate("2015-07-01T02:59:59.999999", glonass);
        assertEquals("2015-07-01T02:59:60.000+00:00",
                t.getComponents(glonass).toString(glonass.minuteDuration(t)));
    }

    @Test
    void testMinuteDuration() {
        final AbsoluteDate t0 = new AbsoluteDate("1983-07-01T02:58:59.000", glonass);
        for (double dt = 0; dt < 63; dt += 0.3) {
            if (dt < 1.0) {
                // before the minute of the leap
                assertEquals(60, glonass.minuteDuration(t0.shiftedBy(dt)));
            } else if (dt < 62.0) {
                // during the minute of the leap
                assertEquals(61, glonass.minuteDuration(t0.shiftedBy(dt)));
            } else {
                // after the minute of the leap
                assertEquals(60, glonass.minuteDuration(t0.shiftedBy(dt)));
            }
        }
    }

    @BeforeEach
    void setUp() {
        Utils.setDataRoot("regular-data");
        glonass = TimeScalesFactory.getGLONASS();
    }

}
