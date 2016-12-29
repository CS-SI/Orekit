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
package org.orekit.frames;


import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.PVCoordinates;


public class EME2000ProviderTest {

    @Test
    public void testAASReferenceLEO() throws OrekitException {

        // this reference test has been extracted from the following paper:
        // Implementation Issues Surrounding the New IAU Reference Systems for Astrodynamics
        // David A. Vallado, John H. Seago, P. Kenneth Seidelmann
        // http://www.centerforspace.com/downloads/files/pubs/AAS-06-134.pdf
        AbsoluteDate t0 = new AbsoluteDate(new DateComponents(2004, 04, 06),
                                           new TimeComponents(07, 51, 28.386009),
                                           TimeScalesFactory.getUTC());

        Transform t = FramesFactory.getGCRF().getTransformTo(FramesFactory.getEME2000(), t0);

        PVCoordinates pvGcrfIau2000A =
            new PVCoordinates(new Vector3D(5102508.9579, 6123011.4038, 6378136.9252),
                              new Vector3D(-4743.220156, 790.536497, 5533.755728));
        PVCoordinates pvEME2000EqA =
            new PVCoordinates(new Vector3D(5102509.0383, 6123011.9758, 6378136.3118),
                              new Vector3D(-4743.219766, 790.536344, 5533.756084));
        checkPV(pvEME2000EqA, t.transformPVCoordinates(pvGcrfIau2000A), 1.1e-4, 2.6e-7);

        PVCoordinates pvGcrfIau2000B =
            new PVCoordinates(new Vector3D(5102508.9579, 6123011.4012, 6378136.9277),
                              new Vector3D(-4743.220156, 790.536495, 5533.755729));
        PVCoordinates pvEME2000EqB =
            new PVCoordinates(new Vector3D(5102509.0383, 6123011.9733, 6378136.3142),
                              new Vector3D(-4743.219766, 790.536342, 5533.756085));
        checkPV(pvEME2000EqB, t.transformPVCoordinates(pvGcrfIau2000B), 7.4e-5, 2.6e-7);

    }

    @Test
    public void testAASReferenceGEO() throws OrekitException {

        // this reference test has been extracted from the following paper:
        // Implementation Issues Surrounding the New IAU Reference Systems for Astrodynamics
        // David A. Vallado, John H. Seago, P. Kenneth Seidelmann
        // http://www.centerforspace.com/downloads/files/pubs/AAS-06-134.pdf
        AbsoluteDate t0 = new AbsoluteDate(new DateComponents(2004, 06, 01),
                                           TimeComponents.H00,
                                           TimeScalesFactory.getUTC());

        Transform t = FramesFactory.getGCRF().getTransformTo(FramesFactory.getEME2000(), t0);

        PVCoordinates pvGCRFiau2000A =
            new PVCoordinates(new Vector3D(-40588150.3617, -11462167.0397, 27143.1974),
                              new Vector3D(834.787458, -2958.305691, -1.172993));
        PVCoordinates pvEME2000EqA =
            new PVCoordinates(new Vector3D(-40588149.5482, -11462169.9118, 27146.8462),
                              new Vector3D(834.787667, -2958.305632, -1.172963));
        checkPV(pvEME2000EqA, t.transformPVCoordinates(pvGCRFiau2000A), 5.8e-5, 6.4e-7);

        PVCoordinates pvGCRFiau2000B =
            new PVCoordinates(new Vector3D(-40588150.3617,-11462167.0397, 27143.2125),
                              new Vector3D(834.787458,-2958.305691,-1.172999));
        PVCoordinates pvEME2000EqB =
            new PVCoordinates(new Vector3D(-40588149.5481, -11462169.9118, 27146.8613),
                              new Vector3D(834.787667, -2958.305632, -1.172968));
        checkPV(pvEME2000EqB, t.transformPVCoordinates(pvGCRFiau2000B), 1.1e-4, 5.5e-7);

    }

    @Test
    public void testSofaBp00() throws OrekitException {

        // the reference value has been computed using the March 2012 version of the SOFA library
        // http://www.iausofa.org/2012_0301_C.html, with the following code
        //
        //        double utc1, utc2, tai1, tai2, tt1, tt2, rb[3][3], rp[3][3], rbp[3][3];
        //
        //        // 2004-02-14:00:00:00Z, MJD = 53049, UT1-UTC = -0.4093509
        //        utc1  = DJM0 + 53049.0;
        //        utc2  = 0.0;
        //        iauUtctai(utc1, utc2, &tai1, &tai2);
        //        iauTaitt(tai1, tai2, &tt1, &tt2);
        //
        //        iauBp00(tt1, tt2, rb, rp, rbp);
        //
        //        printf("iauBp00(%.20g, %.20g, rb, rp, rbp)\n"
        //               "  rb  --> %.20g %.20g %.20g\n          %.20g %.20g %.20g\n          %.20g %.20g %.20g\n"
        //               "  rp  --> %.20g %.20g %.20g\n          %.20g %.20g %.20g\n          %.20g %.20g %.20g\n"
        //               "  rbp --> %.20g %.20g %.20g\n          %.20g %.20g %.20g\n          %.20g %.20g %.20g\n",
        //               tt1, tt2,
        //               rb[0][0],  rb[0][1],  rb[0][2],
        //               rb[1][0],  rb[1][1],  rb[1][2],
        //               rb[2][0],  rb[2][1],  rb[2][2],
        //               rp[0][0],  rp[0][1],  rp[0][2],
        //               rp[1][0],  rp[1][1],  rp[1][2],
        //               rp[2][0],  rp[2][1],  rp[2][2],
        //               rbp[0][0], rbp[0][1], rbp[0][2],
        //               rbp[1][0], rbp[1][1], rbp[1][2],
        //               rbp[2][0], rbp[2][1], rbp[2][2]);
        //
        // the output of this test reads:
        //        iauBp00(2453049.5, 0.00074287037037037029902, rb, rp, rbp)
        //        rb  --> 0.99999999999999422684 -7.0782797441991980175e-08 8.0562171469761337802e-08
        //                7.0782794778573375197e-08 0.99999999999999689138 3.3060414542221364117e-08
        //                -8.0562173809869716745e-08 -3.3060408839805516801e-08 0.99999999999999622524
        //        rp  --> 0.99999949573309343531 -0.00092105778423522924759 -0.00040023257863225548568
        //                0.00092105778625203805956 0.99999957582617116092 -1.7927962069881782439e-07
        //                0.00040023257399096032498 -1.8935780260465051583e-07 0.99999991990692216337
        //        rbp --> 0.99999949570013624278 -0.00092112855376512230675 -0.00040015204695196122638
        //                0.00092112856903123034591 0.99999957576097886491 -1.4614501776464880046e-07
        //                0.00040015201181019732432 -2.2244653837776004327e-07 0.99999991993915571253

        AbsoluteDate date = new AbsoluteDate(2004, 2, 14, TimeScalesFactory.getUTC());
        Frame eme2000 = FramesFactory.getFrame(Predefined.EME2000);
        Frame gcrf    = FramesFactory.getFrame(Predefined.GCRF);
        checkRotation(new double[][] {
            {  0.99999999999999422684,    -7.0782797441991980175e-08, 8.0562171469761337802e-08 },
            {  7.0782794778573375197e-08,  0.99999999999999689138,    3.3060414542221364117e-08 },
            { -8.0562173809869716745e-08, -3.3060408839805516801e-08, 0.99999999999999622524    }

        }, gcrf.getTransformTo(eme2000, date), 2.5e-16);

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("compressed-data");
    }

    private void checkPV(PVCoordinates reference,
                         PVCoordinates result, double positionThreshold,
                         double velocityThreshold) {

        Vector3D dP = result.getPosition().subtract(reference.getPosition());
        Vector3D dV = result.getVelocity().subtract(reference.getVelocity());
        Assert.assertEquals(0, dP.getNorm(), positionThreshold);
        Assert.assertEquals(0, dV.getNorm(), velocityThreshold);
    }

    private void checkRotation(double[][] reference, Transform t, double epsilon) {
        double[][] mat = t.getRotation().getMatrix();
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                Assert.assertEquals(reference[i][j], mat[i][j], epsilon * FastMath.abs(reference[i][j]));

            }
        }
    }

}
