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


import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;

public class GPSScaleTest {

    @Test
    public void testT0() {
        AbsoluteDate t0 =
            new AbsoluteDate(new DateComponents(1980, 1, 6), TimeComponents.H00, TimeScalesFactory.getGPS());
        assertEquals(AbsoluteDate.GPS_EPOCH, t0);
    }

    @Test
    public void testArbitrary() throws OrekitException {
        AbsoluteDate tGPS =
            new AbsoluteDate(new DateComponents(1999, 3, 4), TimeComponents.H00, TimeScalesFactory.getGPS());
        AbsoluteDate tUTC =
            new AbsoluteDate(new DateComponents(1999, 3, 3), new TimeComponents(23, 59, 47),
                             TimeScalesFactory.getUTC());
        assertEquals(tUTC, tGPS);
    }

    @Test
    public void testConstant() {
        TimeScale scale = TimeScalesFactory.getGPS();
        double reference = scale.offsetFromTAI(AbsoluteDate.J2000_EPOCH);
        for (double dt = -10000; dt < 10000; dt += 123.456789) {
            AbsoluteDate date = new AbsoluteDate(AbsoluteDate.J2000_EPOCH, dt * 86400);
            assertEquals(reference, scale.offsetFromTAI(date), 1.0e-15);
        }
    }

    @Test
    public void testSymmetry() {
        TimeScale scale = TimeScalesFactory.getGPS();
        for (double dt = -10000; dt < 10000; dt += 123.456789) {
            AbsoluteDate date = new AbsoluteDate(AbsoluteDate.J2000_EPOCH, dt * 86400);
            double dt1 = scale.offsetFromTAI(date);
            DateTimeComponents components = date.getComponents(scale);
            double dt2 = scale.offsetToTAI(components.getDate(), components.getTime());
            assertEquals( 0.0, dt1 + dt2, 1.0e-10);
        }
    }

    @Before
    public void setUp() {
        String root = getClass().getClassLoader().getResource("regular-data").getPath();
        System.setProperty(DataProvidersManager.OREKIT_DATA_PATH, root);
    }

}
