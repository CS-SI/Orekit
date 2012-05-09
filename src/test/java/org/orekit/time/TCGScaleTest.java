/* Copyright 2002-2012 CS Systèmes d'Information
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


public class TCGScaleTest {

    @Test
    public void testRatio() {
        TimeScale scale = TimeScalesFactory.getTCG();
        Assert.assertEquals("TCG", scale.toString());
        final double dtTT = 1e6;
        final AbsoluteDate t1 = AbsoluteDate.J2000_EPOCH;
        final AbsoluteDate t2 = t1.shiftedBy(dtTT);
        final double dtTCG = dtTT + scale.offsetFromTAI(t2) - scale.offsetFromTAI(t1);
        Assert.assertEquals(1 - 6.969290134e-10, dtTT / dtTCG, 1.0e-15);
    }

    @Test
    public void testSymmetry() {
        TimeScale scale = TimeScalesFactory.getTCG();
        for (double dt = -10000; dt < 10000; dt += 123.456789) {
            AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(dt * Constants.JULIAN_DAY);
            double dt1 = scale.offsetFromTAI(date);
            DateTimeComponents components = date.getComponents(scale);
            double dt2 = scale.offsetToTAI(components.getDate(), components.getTime());
            Assert.assertEquals( 0.0, dt1 + dt2, 1.0e-10);
        }
    }

    @Test
    public void testReference() throws OrekitException {
        DateComponents  referenceDate = new DateComponents(1977, 01, 01);
        TimeComponents  thirtyTwo     = new TimeComponents(0, 0, 32.184);
        AbsoluteDate ttRef         = new AbsoluteDate(referenceDate, thirtyTwo, TimeScalesFactory.getTT());
        AbsoluteDate tcgRef        = new AbsoluteDate(referenceDate, thirtyTwo, TimeScalesFactory.getTCG());
        AbsoluteDate taiRef        = new AbsoluteDate(referenceDate, TimeComponents.H00, TimeScalesFactory.getTAI());
        AbsoluteDate utcRef        = new AbsoluteDate(new DateComponents(1976, 12, 31),
                                                      new TimeComponents(23, 59, 45),
                                                      TimeScalesFactory.getUTC());
        Assert.assertEquals(0, ttRef.durationFrom(tcgRef), 1.0e-15);
        Assert.assertEquals(0, ttRef.durationFrom(taiRef), 1.0e-15);
        Assert.assertEquals(0, ttRef.durationFrom(utcRef), 1.0e-15);
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
