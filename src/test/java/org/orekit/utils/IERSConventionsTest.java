/* Copyright 2002-2013 CS Systèmes d'Information
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
package org.orekit.utils;


import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;


public class IERSConventionsTest {

    @Test
    public void testConventionsNumber() {
        Assert.assertEquals(3, IERSConventions.values().length);
    }

    @Test
    public void testIERS1996PrecessionAngles() throws OrekitException {

        // the reference data for this test was obtained by running the following program
        // with version 2012-03-01 of the SOFA library in C
        //

        //        double utc1, utc2, tai1, tai2, tt1, tt2, tttdb, tdb1, tdb2;
        //        double zeta, z, theta;
        //     
        //        // 2004-02-14:00:00:00Z, MJD = 53049, UT1-UTC = -0.4093509
        //        utc1  = DJM0 + 53049.0;
        //        utc2  = 0.0;
        //        iauUtctai(utc1, utc2, &tai1, &tai2);
        //        iauTaitt(tai1, tai2, &tt1, &tt2);
        //
        //        iauPrec76(DJ00, 0.0, tt1, tt2, &zeta, &z, &theta);
        //
        //        printf("iauPrec76(%.20g, %.20g, %.20g, %.20g, &zeta, &z, &theta)\n  --> %.20g  %.20g %.20g\n",
        //               DJ00, 0.0, tt1, tt2, zeta, z, theta);

        // which displays the following result:

        //        iauPrec76(2451545, 0, 2453049.5, 0.00074287037037037029902, &zeta, &z, &theta)
        //        --> 0.00046055316630716768315  0.00046055968780715523126 0.00040025642650262118188

        AbsoluteDate date = new AbsoluteDate(2004, 2, 14, TimeScalesFactory.getUTC());
        double[] angles= IERSConventions.IERS_1996.getPrecessionFunction().value(date);
        Assert.assertEquals(0.00046055316630716768315, angles[0], 2.0e-16 * angles[0]);
        Assert.assertEquals(0.00046055968780715523126, angles[2], 2.0e-16 * angles[2]);
        Assert.assertEquals(0.00040025642650262118188, angles[1], 2.0e-16 * angles[1]);

    }

    @Test
    public void testIERS2003() throws OrekitException {
        Assert.assertNotNull(IERSConventions.IERS_2003.getEarthOrientationAngleFunction());
    }

    @Test
    public void testIERS2010() throws OrekitException {
        Assert.assertNotNull(IERSConventions.IERS_2010.getEarthOrientationAngleFunction());
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
