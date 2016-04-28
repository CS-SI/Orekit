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


import org.hipparchus.geometry.euclidean.threed.Rotation;
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

public class ITRFProviderTest {

    @Test
    public void testTidalEffects() throws OrekitException {

        final Frame itrfWith    = FramesFactory.getITRF(IERSConventions.IERS_2010, false);
        final Frame itrfWithout = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        final AbsoluteDate date0 = new AbsoluteDate(2007, 10, 20, TimeScalesFactory.getUTC());

        double minCorrection = Double.POSITIVE_INFINITY;
        double maxCorrection = Double.NEGATIVE_INFINITY;
        for (double dt = 0; dt < 3 * Constants.JULIAN_DAY; dt += 60) {
            final AbsoluteDate date = date0.shiftedBy(dt);
            final Transform t = itrfWith.getTransformTo(itrfWithout, date);
            Assert.assertEquals(0, t.getTranslation().getNorm(), 1.0e-15);
            final double milliarcSeconds = FastMath.toDegrees(t.getRotation().getAngle()) * 3600000.0;
            minCorrection = FastMath.min(minCorrection, milliarcSeconds);
            maxCorrection = FastMath.max(maxCorrection, milliarcSeconds);
        }

        Assert.assertEquals(0.064, minCorrection, 0.001);
        Assert.assertEquals(0.613, maxCorrection, 0.001);

    }

    @Test
    public void testAASReferenceLEO() throws OrekitException {

        // this reference test has been extracted from the following paper:
        // Implementation Issues Surrounding the New IAU Reference Systems for Astrodynamics
        // David A. Vallado, John H. Seago, P. Kenneth Seidelmann
        // http://www.centerforspace.com/downloads/files/pubs/AAS-06-134.pdf
        // Reference position & velocity from : "Fundamentals of Astrodynamics and Applications", Third edition, David A. Vallado
        Utils.setLoaders(IERSConventions.IERS_2010,
                         Utils.buildEOPList(IERSConventions.IERS_2010, new double[][] {
                             { 53098, -0.4399619, 0.0015563, -0.140682, 0.333309, Double.NaN, Double.NaN, -0.000199, -0.000252 },
                             { 53099, -0.4399619, 0.0015563, -0.140682, 0.333309, Double.NaN, Double.NaN, -0.000199, -0.000252 },
                             { 53100, -0.4399619, 0.0015563, -0.140682, 0.333309, Double.NaN, Double.NaN, -0.000199, -0.000252 },
                             { 53101, -0.4399619, 0.0015563, -0.140682, 0.333309, Double.NaN, Double.NaN, -0.000199, -0.000252 },
                             { 53102, -0.4399619, 0.0015563, -0.140682, 0.333309, Double.NaN, Double.NaN, -0.000199, -0.000252 },
                             { 53103, -0.4399619, 0.0015563, -0.140682, 0.333309, Double.NaN, Double.NaN, -0.000199, -0.000252 },
                             { 53104, -0.4399619, 0.0015563, -0.140682, 0.333309, Double.NaN, Double.NaN, -0.000199, -0.000252 },
                             { 53105, -0.4399619, 0.0015563, -0.140682, 0.333309, Double.NaN, Double.NaN, -0.000199, -0.000252 }
                         }));
        AbsoluteDate t0 = new AbsoluteDate(new DateComponents(2004, 04, 06),
                                           new TimeComponents(07, 51, 28.386009),
                                           TimeScalesFactory.getUTC());

        // Positions LEO
        Frame itrfA = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        PVCoordinates pvITRF =
            new PVCoordinates(new Vector3D(-1033479.3830, 7901295.2754, 6380356.5958),
                              new Vector3D(-3225.636520, -2872.451450, 5531.924446));

        // Reference coordinates
        PVCoordinates pvGcrfIau2000A =
            new PVCoordinates(new Vector3D(5102508.9579, 6123011.4038, 6378136.9252),
                              new Vector3D(-4743.220156, 790.536497, 5533.755728));
        checkPV(pvGcrfIau2000A,
                itrfA.getTransformTo(FramesFactory.getGCRF(), t0).transformPVCoordinates(pvITRF),
                0.0192, 2.15e-5);

        PVCoordinates pvEME2000EqA =
            new PVCoordinates(new Vector3D(5102509.0383, 6123011.9758, 6378136.3118),
                              new Vector3D(-4743.219766, 790.536344, 5533.756084));
        checkPV(pvEME2000EqA,
                itrfA.getTransformTo(FramesFactory.getEME2000(), t0).transformPVCoordinates(pvITRF),
                0.0191, 2.13e-5);

    }

    @Test
    public void testAASReferenceGEO() throws OrekitException {

        // this reference test has been extracted from the following paper:
        // Implementation Issues Surrounding the New IAU Reference Systems for Astrodynamics
        // David A. Vallado, John H. Seago, P. Kenneth Seidelmann
        // http://www.centerforspace.com/downloads/files/pubs/AAS-06-134.pdf
        Utils.setLoaders(IERSConventions.IERS_2010,
                         Utils.buildEOPList(IERSConventions.IERS_2010, new double[][] {
                             { 53153, -0.4709050,  0.0000000, -0.083853,  0.467217, Double.NaN, Double.NaN, -0.000199, -0.000252 },
                             { 53154, -0.4709050,  0.0000000, -0.083853,  0.467217, Double.NaN, Double.NaN, -0.000199, -0.000252 },
                             { 53155, -0.4709050,  0.0000000, -0.083853,  0.467217, Double.NaN, Double.NaN, -0.000199, -0.000252 },
                             { 53156, -0.4709050,  0.0000000, -0.083853,  0.467217, Double.NaN, Double.NaN, -0.000199, -0.000252 },
                             { 53157, -0.4709050,  0.0000000, -0.083853,  0.467217, Double.NaN, Double.NaN, -0.000199, -0.000252 },
                             { 53158, -0.4709050,  0.0000000, -0.083853,  0.467217, Double.NaN, Double.NaN, -0.000199, -0.000252 },
                             { 53159, -0.4709050,  0.0000000, -0.083853,  0.467217, Double.NaN, Double.NaN, -0.000199, -0.000252 },
                             { 53160, -0.4709050,  0.0000000, -0.083853,  0.467217, Double.NaN, Double.NaN, -0.000199, -0.000252 }
                         }));

        AbsoluteDate t0 = new AbsoluteDate(new DateComponents(2004, 06, 01),
                                           TimeComponents.H00,
                                           TimeScalesFactory.getUTC());

        //  Positions GEO
        Frame itrfA = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        PVCoordinates pvITRF =
            new PVCoordinates(new Vector3D(24796919.2915, -34115870.9234, 10226.0621),
                              new Vector3D(-0.979178, -1.476538, -0.928776));

        PVCoordinates pvGCRFiau2000A =
            new PVCoordinates(new Vector3D(-40588150.3617, -11462167.0397, 27143.1974),
                              new Vector3D(834.787458, -2958.305691, -1.172993));
        checkPV(pvGCRFiau2000A,
                itrfA.getTransformTo(FramesFactory.getGCRF(), t0).transformPVCoordinates(pvITRF),
                0.0806, 1.03e-4);

        PVCoordinates pvEME2000EqA =
            new PVCoordinates(new Vector3D(-40588149.5482, -11462169.9118, 27146.8462),
                              new Vector3D(834.787667, -2958.305632, -1.172963));
        checkPV(pvEME2000EqA,
                itrfA.getTransformTo(FramesFactory.getEME2000(), t0).transformPVCoordinates(pvITRF),
                0.0806, 1.04e-4);

    }

    @Test
    public void testAASReferenceGEODX0DY0() throws OrekitException {

        // this reference test has been extracted from the following paper:
        // Implementation Issues Surrounding the New IAU Reference Systems for Astrodynamics
        // David A. Vallado, John H. Seago, P. Kenneth Seidelmann
        // http://www.centerforspace.com/downloads/files/pubs/AAS-06-134.pdf
        Utils.setLoaders(IERSConventions.IERS_2010,
                         Utils.buildEOPList(IERSConventions.IERS_2010, new double[][] {
                             { 53153, -0.4709050,  0.0000000, -0.083853,  0.467217, 0.0, 0.0, 0.0, 0.0 },
                             { 53154, -0.4709050,  0.0000000, -0.083853,  0.467217, 0.0, 0.0, 0.0, 0.0 },
                             { 53155, -0.4709050,  0.0000000, -0.083853,  0.467217, 0.0, 0.0, 0.0, 0.0 },
                             { 53156, -0.4709050,  0.0000000, -0.083853,  0.467217, 0.0, 0.0, 0.0, 0.0 },
                             { 53157, -0.4709050,  0.0000000, -0.083853,  0.467217, 0.0, 0.0, 0.0, 0.0 },
                             { 53158, -0.4709050,  0.0000000, -0.083853,  0.467217, 0.0, 0.0, 0.0, 0.0 },
                             { 53159, -0.4709050,  0.0000000, -0.083853,  0.467217, 0.0, 0.0, 0.0, 0.0 },
                             { 53160, -0.4709050,  0.0000000, -0.083853,  0.467217, 0.0, 0.0, 0.0, 0.0 }
                         }));

        AbsoluteDate t0 = new AbsoluteDate(new DateComponents(2004, 06, 01),
                                           TimeComponents.H00,
                                           TimeScalesFactory.getUTC());

        //  Positions GEO
        Frame itrfA = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        PVCoordinates pvITRF =
            new PVCoordinates(new Vector3D(24796919.2915, -34115870.9234, 10226.0621),
                              new Vector3D(-0.979178, -1.476538, -0.928776));

        PVCoordinates pvGCRFdx0dy0 =
            new PVCoordinates(new Vector3D(-40588150.3643, -11462167.0302, 27143.1979),
                              new Vector3D(834.787457, -2958.305691, -1.172993));
        checkPV(pvGCRFdx0dy0,
                itrfA.getTransformTo(FramesFactory.getGCRF(), t0).transformPVCoordinates(pvITRF),
                0.0505, 1.06e-4);

        PVCoordinates pvEME2000EqA =
            new PVCoordinates(new Vector3D(-40588149.5482, -11462169.9118, 27146.8462),
                              new Vector3D(834.787667, -2958.305632, -1.172963));
        checkPV(pvEME2000EqA,
                itrfA.getTransformTo(FramesFactory.getEME2000(), t0).transformPVCoordinates(pvITRF),
                0.0603, 1.07e-4);

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

        Utils.setLoaders(IERSConventions.IERS_2010,
                         Utils.buildEOPList(IERSConventions.IERS_2010, new double[][] {
                             { 54192, -0.072073685,  1.4020, 0.0349282, 0.4833163, -Double.NaN, Double.NaN, 0.0001750, -0.0002259 },
                             { 54193, -0.072073685,  1.4020, 0.0349282, 0.4833163, -Double.NaN, Double.NaN, 0.0001750, -0.0002259 },
                             { 54194, -0.072073685,  1.4020, 0.0349282, 0.4833163, -Double.NaN, Double.NaN, 0.0001750, -0.0002259 },
                             { 54195, -0.072073685,  1.4020, 0.0349282, 0.4833163, -Double.NaN, Double.NaN, 0.0001750, -0.0002259 },
                             { 54196, -0.072073685,  1.4020, 0.0349282, 0.4833163, -Double.NaN, Double.NaN, 0.0001750, -0.0002259 },
                             { 54197, -0.072073685,  1.4020, 0.0349282, 0.4833163, -Double.NaN, Double.NaN, 0.0001750, -0.0002259 },
                             { 54198, -0.072073685,  1.4020, 0.0349282, 0.4833163, -Double.NaN, Double.NaN, 0.0001750, -0.0002259 },
                             { 54199, -0.072073685,  1.4020, 0.0349282, 0.4833163, -Double.NaN, Double.NaN, 0.0001750, -0.0002259 }
                         }));

        EOPHistory eopHistory = FramesFactory.getEOPHistory(IERSConventions.IERS_2010, true);

        TimeScale utc = TimeScalesFactory.getUTC();
        TTScale tt    = TimeScalesFactory.getTT();
        UT1Scale ut1  = TimeScalesFactory.getUT1(eopHistory);
        Frame gcrf    = FramesFactory.getGCRF();
        Frame itrf    = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
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
        double era = IERSConventions.IERS_2010.getEarthOrientationAngleFunction(ut1).value(date).getValue();
        Assert.assertEquals(13.318492966097 * 3600 * 1.0e6,
                            radToMicroAS(MathUtils.normalizeAngle(era, 0)),
                            0.0022);

        // nutation/precession/bias matrix check
        Rotation refNPB = new Rotation(new double[][] {
            { +0.999999746339445, -0.000000005138721, -0.000712264730182 },
            { -0.000000026475329, +0.999999999014975, -0.000044385242666 },
            { +0.000712264729708, +0.000044385250265, +0.999999745354420 }
        }, 1.0e-13);
        Rotation npb = gcrf.getTransformTo(tod, date).getRotation();
        Assert.assertEquals(0.0, radToMicroAS(Rotation.distance(refNPB, npb)), 0.31);

        // celestial to terrestrial frames matrix, without polar motion
        Rotation refWithoutPolarMotion = new Rotation(new double[][] {
            { +0.973104317573104, +0.230363826247808, -0.000703332818915 },
            { -0.230363798804281, +0.973104570735550, +0.000120888549767 },
            { +0.000712264729708, +0.000044385250265, +0.999999745354420 }
        }, 1.0e-13);
        Rotation withoutPM = gcrf.getTransformTo(gtod, date).getRotation();
        Assert.assertEquals(0.0, radToMicroAS(Rotation.distance(refWithoutPolarMotion, withoutPM)), 0.31);

        // celestial to terrestrial frames matrix, with polar motion
        Rotation refWithPolarMotion = new Rotation(new double[][] {
            { +0.973104317697512, +0.230363826239227, -0.000703163482268 },
            { -0.230363800456136, +0.973104570632777, +0.000118545366806 },
            { +0.000711560162777, +0.000046626403835, +0.999999745754024 }
        }, 1.0e-13);
        Rotation withPM = gcrf.getTransformTo(itrf, date).getRotation();
        Assert.assertEquals(0.0, radToMicroAS(Rotation.distance(refWithPolarMotion, withPM)), 0.31);

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("compressed-data");
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
