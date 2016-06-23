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
import org.orekit.utils.IERSConventions;


public class TDBScaleTest {

    @Test
    public void testReference() {
        TimeScale scale = TimeScalesFactory.getTDB();
        Assert.assertEquals("TDB", scale.toString());
        Assert.assertEquals(32.183927340791372839, scale.offsetFromTAI(AbsoluteDate.J2000_EPOCH), 1.0e-15);
    }

    @Test
    public void testDate5000000() {
        TimeScale scale = TimeScalesFactory.getTDB();
        AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(5000000);
        Assert.assertEquals(32.185364155950634549, scale.offsetFromTAI(date), 1.0e-13);
    }

    @Test
    public void testToTAI5000000() {
        TimeScale scale = TimeScalesFactory.getTDB();
        AbsoluteDate date = new AbsoluteDate(2000, 2, 28, 8, 53, 20.001364155950634549, scale);
        double dt = AbsoluteDate.J2000_EPOCH.shiftedBy(5000000).durationFrom(date);
        Assert.assertEquals(0.0, dt, 1.0e-13);
    }

    @Test
    public void testToTAI() {
        TimeScale scale = TimeScalesFactory.getTDB();
        AbsoluteDate date = new AbsoluteDate(2000, 01, 01, 11, 59, 59.999927340791372839, scale);
        double dt = AbsoluteDate.J2000_EPOCH.durationFrom(date);
        Assert.assertEquals(0.0, dt, 1.0e-13);
    }

    @Test
    public void testSofa() {

        // the reference data for this test was obtained by running the following program
        // with version 2012-03-01 of the SOFA library in C
        //        double tai1, tai2, tttdb;
        //
        //        tai1 = 2448939.5;
        //        tai2 = 0.123;
        //        tttdb = iauDtdb(tai1, tai2, 0.0, 0.0, 0.0, 0.0);
        //
        //        printf("iauDtdb(%.20g, %.20g, 0.0, 0.0, 0.0, 0.0)\n  --> %.20g\n", tai1, tai2, tttdb);
        // which displays the following result:
        //        iauDtdb(2448939.5, 0.12299999999999999822, 0.0, 0.0, 0.0, 0.0)
        //        --> -0.001279984433218163669

        // the difference with SOFA is quite big (10 microseconds) because SOFA uses
        // the full Fairhead & Bretagnon model from 1990, including planetary effects,
        // whereas in Orekit we use only the conventional definition from IAU general
        // assembly 2006. So this difference is expected

        AbsoluteDate date = new AbsoluteDate(1992, 11, 13, 2, 57, 7.2,
                                             TimeScalesFactory.getTAI());
        double delta = TimeScalesFactory.getTDB().offsetFromTAI(date) -
                       TimeScalesFactory.getTT().offsetFromTAI(date);
        Assert.assertEquals(-0.001279984433218163669, delta, 1.0e-5);

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
        DateTimeComponents components = date.getComponents(TimeScalesFactory.getTDB());
        Assert.assertEquals(2004,            components.getDate().getYear());
        Assert.assertEquals(   4,            components.getDate().getMonth());
        Assert.assertEquals(   6,            components.getDate().getDay());
        Assert.assertEquals(   7,            components.getTime().getHour());
        Assert.assertEquals(  52,            components.getTime().getMinute());

        // the "large" threshold in this test is due to the fact TDB model is
        // approximated both in Orekit and in the reference paper. the difference
        // is however small as the model in the paper is announced as being accurate
        // to 50 micro seconds, and the test here is far below this value
        Assert.assertEquals(  32.5716651154, components.getTime().getSecond(), 1.4e-8);

    }

    @Test
    public void testDuringLeap() throws OrekitException {
        final TimeScale utc   = TimeScalesFactory.getUTC();
        final TimeScale scale = TimeScalesFactory.getTDB();
        final AbsoluteDate before = new AbsoluteDate(new DateComponents(1983, 06, 30),
                                                     new TimeComponents(23, 59, 59),
                                                     utc);
        final AbsoluteDate during = before.shiftedBy(1.25);
        Assert.assertEquals(61, utc.minuteDuration(during));
        Assert.assertEquals(1.0, utc.getLeap(during), 1.0e-10);
        Assert.assertEquals(60, scale.minuteDuration(during));
        Assert.assertEquals(0.0, scale.getLeap(during), 1.0e-10);
    }

    @Before
    public void setUp() throws OrekitException {
        Utils.setDataRoot("regular-data");
    }

}
