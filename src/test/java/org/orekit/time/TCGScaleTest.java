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
import org.orekit.utils.IERSConventions;


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
    public void testDuringLeap() throws OrekitException {
        final TimeScale utc   = TimeScalesFactory.getUTC();
        final TimeScale scale = TimeScalesFactory.getTCG();
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

    @Test
    public void testSofa() {
        TimeScale tt  = TimeScalesFactory.getTT();
        AbsoluteDate date = new AbsoluteDate(2006, 1, 15, 21, 25, 10.5000096, tt);
        double delta = TimeScalesFactory.getTCG().offsetFromTAI(date) - tt.offsetFromTAI(date);
        Assert.assertEquals(Constants.JULIAN_DAY * (0.8924900312508587113 -  0.892482639), delta, 5.0e-10);
    }

    @Test
    public void testAAS06134() throws OrekitException {

        // this reference test has been extracted from the following paper:
        // Implementation Issues Surrounding the New IAU Reference Systems for Astrodynamics
        // David A. Vallado, John H. Seago, P. Kenneth Seidelmann
        // http://www.centerforspace.com/downloads/files/pubs/AAS-06-134.pdf
        // Note that the dUT1 here is -0.439962, whereas it is -0.4399619 in the book
        Utils.setLoaders(IERSConventions.IERS_1996,
                         Utils.buildEOPList(IERSConventions.IERS_1996, new double[][] {
                             { 53098, -0.439962, 0.0015563, -0.140682, 0.333309, -0.052195, -0.003875, Double.NaN, Double.NaN },
                             { 53099, -0.439962, 0.0015563, -0.140682, 0.333309, -0.052195, -0.003875, Double.NaN, Double.NaN },
                             { 53100, -0.439962, 0.0015563, -0.140682, 0.333309, -0.052195, -0.003875, Double.NaN, Double.NaN },
                             { 53101, -0.439962, 0.0015563, -0.140682, 0.333309, -0.052195, -0.003875, Double.NaN, Double.NaN },
                             { 53102, -0.439962, 0.0015563, -0.140682, 0.333309, -0.052195, -0.003875, Double.NaN, Double.NaN },
                             { 53103, -0.439962, 0.0015563, -0.140682, 0.333309, -0.052195, -0.003875, Double.NaN, Double.NaN },
                             { 53104, -0.439962, 0.0015563, -0.140682, 0.333309, -0.052195, -0.003875, Double.NaN, Double.NaN },
                             { 53105, -0.439962, 0.0015563, -0.140682, 0.333309, -0.052195, -0.003875, Double.NaN, Double.NaN }
                         }));
        AbsoluteDate date =
                new AbsoluteDate(2004, 4, 6, 7, 51, 28.386009, TimeScalesFactory.getUTC());
        DateTimeComponents components = date.getComponents(TimeScalesFactory.getTCG());
        Assert.assertEquals(2004,            components.getDate().getYear());
        Assert.assertEquals(   4,            components.getDate().getMonth());
        Assert.assertEquals(   6,            components.getDate().getDay());
        Assert.assertEquals(   7,            components.getTime().getHour());
        Assert.assertEquals(  52,            components.getTime().getMinute());
        Assert.assertEquals(  33.1695861742, components.getTime().getSecond(), 1.0e-10);

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
