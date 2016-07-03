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


import java.util.ArrayList;

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TTScale;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.time.UT1Scale;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;


public class ITRFEquinoxProviderTest {

    @Test
    public void testEquinoxVersusCIO() throws OrekitException {
        Frame itrfEquinox  = FramesFactory.getITRFEquinox(IERSConventions.IERS_1996, true);
        Frame itrfCIO      = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        AbsoluteDate start = new AbsoluteDate(2011, 4, 10, TimeScalesFactory.getUTC());
        AbsoluteDate end   = new AbsoluteDate(2011, 7,  4, TimeScalesFactory.getUTC());
        for (AbsoluteDate date = start; date.compareTo(end) < 0; date = date.shiftedBy(10000)) {
            double angularOffset =
                    itrfEquinox.getTransformTo(itrfCIO, date).getRotation().getAngle();
            Assert.assertEquals(0, angularOffset / Constants.ARC_SECONDS_TO_RADIANS, 0.07);
        }
    }

    @Test
    public void testAASReferenceLEO() throws OrekitException {

        // this reference test has been extracted from the following paper:
        // Implementation Issues Surrounding the New IAU Reference Systems for Astrodynamics
        // David A. Vallado, John H. Seago, P. Kenneth Seidelmann
        // http://www.centerforspace.com/downloads/files/pubs/AAS-06-134.pdf
        Utils.setLoaders(IERSConventions.IERS_1996,
                         Utils.buildEOPList(IERSConventions.IERS_1996, new double[][] {
                             { 53098, -0.4399619, 0.0015563, -0.140682, 0.333309, -0.052195, -0.003875, Double.NaN, Double.NaN },
                             { 53099, -0.4399619, 0.0015563, -0.140682, 0.333309, -0.052195, -0.003875, Double.NaN, Double.NaN },
                             { 53100, -0.4399619, 0.0015563, -0.140682, 0.333309, -0.052195, -0.003875, Double.NaN, Double.NaN },
                             { 53101, -0.4399619, 0.0015563, -0.140682, 0.333309, -0.052195, -0.003875, Double.NaN, Double.NaN },
                             { 53102, -0.4399619, 0.0015563, -0.140682, 0.333309, -0.052195, -0.003875, Double.NaN, Double.NaN },
                             { 53103, -0.4399619, 0.0015563, -0.140682, 0.333309, -0.052195, -0.003875, Double.NaN, Double.NaN },
                             { 53104, -0.4399619, 0.0015563, -0.140682, 0.333309, -0.052195, -0.003875, Double.NaN, Double.NaN },
                             { 53105, -0.4399619, 0.0015563, -0.140682, 0.333309, -0.052195, -0.003875, Double.NaN, Double.NaN }
                         }));
        AbsoluteDate t0 = new AbsoluteDate(new DateComponents(2004, 04, 06),
                                           new TimeComponents(07, 51, 28.386009),
                                           TimeScalesFactory.getUTC());

        // ITRF
        PVCoordinates pvITRF =
           new PVCoordinates(new Vector3D(-1033479.3830, 7901295.2754, 6380356.5958),
                             new Vector3D(-3225.636520, -2872.451450, 5531.924446));

        // GTOD
        PVCoordinates pvGTOD =
            new PVCoordinates(new Vector3D(-1033475.0313, 7901305.5856, 6380344.5328),
                              new Vector3D(-3225.632747, -2872.442511, 5531.931288));

        Transform t = FramesFactory.getGTOD(IERSConventions.IERS_1996, true).
                getTransformTo(FramesFactory.getITRFEquinox(IERSConventions.IERS_1996, true), t0);
        checkPV(pvITRF, t.transformPVCoordinates(pvGTOD), 8.08e-5, 3.78e-7);

    }

    @Test
    public void testAASReferenceGEO() throws OrekitException {

        // this reference test has been extracted from the following paper:
        // Implementation Issues Surrounding the New IAU Reference Systems for Astrodynamics
        // David A. Vallado, John H. Seago, P. Kenneth Seidelmann
        // http://www.centerforspace.com/downloads/files/pubs/AAS-06-134.pdf
        Utils.setLoaders(IERSConventions.IERS_1996,
                         Utils.buildEOPList(IERSConventions.IERS_1996, new double[][] {
                             { 53153, -0.4709050,  0.0000000, -0.083853,  0.467217, -0.053614, -0.004494, Double.NaN, Double.NaN },
                             { 53154, -0.4709050,  0.0000000, -0.083853,  0.467217, -0.053614, -0.004494, Double.NaN, Double.NaN },
                             { 53155, -0.4709050,  0.0000000, -0.083853,  0.467217, -0.053614, -0.004494, Double.NaN, Double.NaN },
                             { 53156, -0.4709050,  0.0000000, -0.083853,  0.467217, -0.053614, -0.004494, Double.NaN, Double.NaN },
                             { 53157, -0.4709050,  0.0000000, -0.083853,  0.467217, -0.053614, -0.004494, Double.NaN, Double.NaN },
                             { 53158, -0.4709050,  0.0000000, -0.083853,  0.467217, -0.053614, -0.004494, Double.NaN, Double.NaN },
                             { 53159, -0.4709050,  0.0000000, -0.083853,  0.467217, -0.053614, -0.004494, Double.NaN, Double.NaN },
                             { 53160, -0.4709050,  0.0000000, -0.083853,  0.467217, -0.053614, -0.004494, Double.NaN, Double.NaN }
                         }));
        AbsoluteDate t0 = new AbsoluteDate(new DateComponents(2004, 06, 01),
                                           TimeComponents.H00,
                                           TimeScalesFactory.getUTC());

        Transform t = FramesFactory.getGTOD(IERSConventions.IERS_1996, true).
                getTransformTo(FramesFactory.getITRFEquinox(IERSConventions.IERS_1996, true), t0);

        // GTOD
        PVCoordinates pvGTOD =
            new PVCoordinates(new Vector3D(24796919.2956, -34115870.9001, 10293.2583),
                              new Vector3D(-0.979178, -1.476540, -0.928772));

        // ITRF
        PVCoordinates pvITRF =
            new PVCoordinates(new Vector3D(24796919.2915, -34115870.9234, 10226.0621),
                              new Vector3D(-0.979178, -1.476538, -0.928776));

        checkPV(pvITRF, t.transformPVCoordinates(pvGTOD), 3.954e-4, 4.69e-7);

    }

    @Test
    public void testSofaCookbook() throws OrekitException {

        // SOFA cookbook test case:
        //     date       2007 April 05, 12h00m00s.0 UTC
        //     xp         +0′′.0349282
        //     yp         +0′′.4833163
        //     UT1 − UTC  -0s.072073685
        //     dψ 1980    -0′′.0550655
        //     dε 1980    -0′′.0063580
        //     dX 2000    +0′′.0001725
        //     dY 2000    -0′′.0002650
        //     dX 2006    +0′′.0001750
        //     dY 2006    -0′′.0002259

        Utils.setLoaders(IERSConventions.IERS_1996,
                         Utils.buildEOPList(IERSConventions.IERS_1996, new double[][] {
                             { 54192, -0.072073685,  1.4020, 0.0349282, 0.4833163, -0.0550666, -0.0063580, Double.NaN, Double.NaN },
                             { 54193, -0.072073685,  1.4020, 0.0349282, 0.4833163, -0.0550666, -0.0063580, Double.NaN, Double.NaN },
                             { 54194, -0.072073685,  1.4020, 0.0349282, 0.4833163, -0.0550666, -0.0063580, Double.NaN, Double.NaN },
                             { 54195, -0.072073685,  1.4020, 0.0349282, 0.4833163, -0.0550666, -0.0063580, Double.NaN, Double.NaN },
                             { 54196, -0.072073685,  1.4020, 0.0349282, 0.4833163, -0.0550666, -0.0063580, Double.NaN, Double.NaN },
                             { 54197, -0.072073685,  1.4020, 0.0349282, 0.4833163, -0.0550666, -0.0063580, Double.NaN, Double.NaN },
                             { 54198, -0.072073685,  1.4020, 0.0349282, 0.4833163, -0.0550666, -0.0063580, Double.NaN, Double.NaN },
                             { 54199, -0.072073685,  1.4020, 0.0349282, 0.4833163, -0.0550666, -0.0063580, Double.NaN, Double.NaN }
                         }));

        EOPHistory eopHistory = FramesFactory.getEOPHistory(IERSConventions.IERS_1996, true);

        TimeScale utc = TimeScalesFactory.getUTC();
        TTScale tt    = TimeScalesFactory.getTT();
        UT1Scale ut1  = TimeScalesFactory.getUT1(eopHistory);
        Frame gcrf    = FramesFactory.getGCRF();
        Frame itrf    = FramesFactory.getITRFEquinox(IERSConventions.IERS_1996, true);
        Frame gtod    = itrf.getParent();
        Frame tod     = gtod.getParent();

        // time scales checks
        AbsoluteDate date = new AbsoluteDate(new DateComponents(2007, 4, 5), TimeComponents.H12, utc);
        Assert.assertEquals(0.50075444444444,
                            date.getComponents(tt).getTime().getSecondsInUTCDay() / Constants.JULIAN_DAY,
                            5.0e-15);
        Assert.assertEquals(0.499999165813831,
                            date.getComponents(ut1).getTime().getSecondsInUTCDay() / Constants.JULIAN_DAY,
                            1.0e-15);

        // sidereal time check
        double gast = IERSConventions.IERS_1996.getGASTFunction(ut1, eopHistory).value(date).getValue();
        Assert.assertEquals(13.412402380740 * 3600 * 1.0e6,
                            radToMicroAS(MathUtils.normalizeAngle(gast, 0)),
                            25);

        // nutation/precession/bias matrix check
        Rotation refNPB = new Rotation(new double[][] {
            { +0.999998403176203, -0.001639032970562, -0.000712190961847 },
            { +0.001639000942243, +0.999998655799521, -0.000045552846624 },
            { +0.000712264667137, +0.000044385492226, +0.999999745354454 }
        }, 1.0e-13);
        Rotation npb = gcrf.getTransformTo(tod, date).getRotation();
        Assert.assertEquals(0.0, radToMicroAS(Rotation.distance(refNPB, npb)), 27.0);

        // celestial to terrestrial frames matrix, without polar motion
        Rotation refWithoutPolarMotion = new Rotation(new double[][] {
            { +0.973104317592265, +0.230363826166883, -0.000703332813776 },
            { -0.230363798723533, +0.973104570754697, +0.000120888299841 },
            { +0.000712264667137, +0.000044385492226, +0.999999745354454 }
        }, 1.0e-13);
        Rotation withoutPM = gcrf.getTransformTo(gtod, date).getRotation();
        Assert.assertEquals(0.0, radToMicroAS(Rotation.distance(refWithoutPolarMotion, withoutPM)), 9);

        // celestial to terrestrial frames matrix, with polar motion
        Rotation refWithPolarMotion = new Rotation(new double[][] {
            { +0.973104317712772, +0.230363826174782, -0.000703163477127 },
            { -0.230363800391868, +0.973104570648022, +0.000118545116892 },
            { +0.000711560100206, +0.000046626645796, +0.999999745754058 }
        }, 1.0e-13);
        Rotation withPM = gcrf.getTransformTo(itrf, date).getRotation();
        Assert.assertEquals(0.0, radToMicroAS(Rotation.distance(refWithPolarMotion, withPM)), 10);

    }

    @Test
    public void testNROvsEquinoxRealEOP() throws OrekitException {
        Utils.setDataRoot("regular-data");
        checkFrames(FramesFactory.getITRF(IERSConventions.IERS_2010, true),
                    FramesFactory.getITRFEquinox(IERSConventions.IERS_2010, true),
                    1.7);
    }

    @Test
    public void testNROvsEquinoxNoEOP2010() throws OrekitException {
        Utils.setLoaders(IERSConventions.IERS_2010, new ArrayList<EOPEntry>());
        checkFrames(FramesFactory.getITRF(IERSConventions.IERS_2010, true),
                    FramesFactory.getITRFEquinox(IERSConventions.IERS_2010, true),
                    1.7);
    }

    @Test
    public void testNROvsEquinoxNoEOP2003() throws OrekitException {
        Utils.setLoaders(IERSConventions.IERS_2003, new ArrayList<EOPEntry>());
        checkFrames(FramesFactory.getITRF(IERSConventions.IERS_2003, true),
                    FramesFactory.getITRFEquinox(IERSConventions.IERS_2003, true),
                    1.9);
    }

    @Test
    public void testNROvsEquinoxNoEOP1996() throws OrekitException {
        Utils.setLoaders(IERSConventions.IERS_1996, new ArrayList<EOPEntry>());
        checkFrames(FramesFactory.getITRF(IERSConventions.IERS_1996, true),
                    FramesFactory.getITRFEquinox(IERSConventions.IERS_1996, true),
                    100);
    }

    private void checkFrames(Frame frame1, Frame frame2, double toleranceMicroAS)
        throws OrekitException {
        AbsoluteDate t0 = new AbsoluteDate(2005, 5, 30, TimeScalesFactory.getUTC());
        for (double dt = 0; dt < Constants.JULIAN_YEAR; dt += Constants.JULIAN_DAY / 4) {
            AbsoluteDate date = t0.shiftedBy(dt);
            Transform t = FramesFactory.getNonInterpolatingTransform(frame1, frame2, date);
            Vector3D a = t.getRotation().getAxis(RotationConvention.VECTOR_OPERATOR);
            double delta = FastMath.copySign(radToMicroAS(t.getRotation().getAngle()), a.getZ());
            Assert.assertEquals(0.0, delta, toleranceMicroAS);
        }
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("rapid-data-columns");
    }

    private void checkPV(PVCoordinates reference, PVCoordinates result,
                         double expectedPositionError, double expectedVelocityError) {

        Vector3D dP = result.getPosition().subtract(reference.getPosition());
        Vector3D dV = result.getVelocity().subtract(reference.getVelocity());
        Assert.assertEquals(expectedPositionError, dP.getNorm(), 0.01 * expectedPositionError);
        Assert.assertEquals(expectedVelocityError, dV.getNorm(), 0.01 * expectedVelocityError);
    }

    double radToMicroAS(double deltaRad) {
        return deltaRad * 1.0e6 / Constants.ARC_SECONDS_TO_RADIANS;
    }

}
