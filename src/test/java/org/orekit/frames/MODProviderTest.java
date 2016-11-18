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


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.data.PolynomialNutation;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;


public class MODProviderTest {

    @Test
    public void testEuler1976() throws OrekitException {

        TransformProvider eulerBasedProvider = new TransformProvider() {
            private static final long serialVersionUID = 1L;
            private final PolynomialNutation<DerivativeStructure> zetaA =
                    new PolynomialNutation<DerivativeStructure>(0.0,
                            2306.2181 * Constants.ARC_SECONDS_TO_RADIANS,
                            0.30188   * Constants.ARC_SECONDS_TO_RADIANS,
                            0.017998  * Constants.ARC_SECONDS_TO_RADIANS);
            private final PolynomialNutation<DerivativeStructure> thetaA =
                    new PolynomialNutation<DerivativeStructure>(0.0,
                            2004.3109 * Constants.ARC_SECONDS_TO_RADIANS,
                            -0.42665  * Constants.ARC_SECONDS_TO_RADIANS,
                            -0.041833 * Constants.ARC_SECONDS_TO_RADIANS);
            private final PolynomialNutation<DerivativeStructure> zA =
                    new PolynomialNutation<DerivativeStructure>(0.0,
                            2306.2181 * Constants.ARC_SECONDS_TO_RADIANS,
                            1.09468   * Constants.ARC_SECONDS_TO_RADIANS,
                            0.018203  * Constants.ARC_SECONDS_TO_RADIANS);

            public Transform getTransform(AbsoluteDate date) {
                final double tc = IERSConventions.IERS_1996.evaluateTC(date);
                final Rotation r1 = new Rotation(Vector3D.PLUS_K,  zA.value(tc), RotationConvention.VECTOR_OPERATOR);
                final Rotation r2 = new Rotation(Vector3D.PLUS_J, -thetaA.value(tc), RotationConvention.VECTOR_OPERATOR);
                final Rotation r3 = new Rotation(Vector3D.PLUS_K,  zetaA.value(tc), RotationConvention.VECTOR_OPERATOR);
                return new Transform(date, r1.compose(r2.compose(r3,
                                                                 RotationConvention.VECTOR_OPERATOR),
                                                      RotationConvention.VECTOR_OPERATOR));
            }
        };

        MODProvider modProvider = new MODProvider(IERSConventions.IERS_1996);

        for (double dt = -5 * Constants.JULIAN_YEAR; dt < 5 * Constants.JULIAN_YEAR; dt += 10 * Constants.JULIAN_DAY) {
            AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(dt);
            Transform t = new Transform(date,
                                        modProvider.getTransform(date).getInverse(),
                                        eulerBasedProvider.getTransform(date));
            Assert.assertEquals(0, t.getRotation().getAngle(), 1.01e-11);
        }

    }

    @Test
    public void testEuler2000() throws OrekitException {

        // this alternate representation of the transform
        // is from equation 33 in IERS conventions 2003
        TransformProvider eulerBasedProvider = new TransformProvider() {
            private static final long serialVersionUID = 1L;
            private final PolynomialNutation<DerivativeStructure> zetaA =
                    new PolynomialNutation<DerivativeStructure>(   2.5976176 * Constants.ARC_SECONDS_TO_RADIANS,
                                           2306.0809506 * Constants.ARC_SECONDS_TO_RADIANS,
                                              0.3019015 * Constants.ARC_SECONDS_TO_RADIANS,
                                              0.0179663 * Constants.ARC_SECONDS_TO_RADIANS,
                                             -0.0000327 * Constants.ARC_SECONDS_TO_RADIANS,
                                             -0.0000002 * Constants.ARC_SECONDS_TO_RADIANS);
            private final PolynomialNutation<DerivativeStructure> thetaA =
                    new PolynomialNutation<DerivativeStructure>(0.0,
                                           2004.1917476 * Constants.ARC_SECONDS_TO_RADIANS,
                                             -0.4269353 * Constants.ARC_SECONDS_TO_RADIANS,
                                             -0.0418251 * Constants.ARC_SECONDS_TO_RADIANS,
                                             -0.0000601 * Constants.ARC_SECONDS_TO_RADIANS,
                                             -0.0000001 * Constants.ARC_SECONDS_TO_RADIANS);
            private final PolynomialNutation<DerivativeStructure> zA =
                    new PolynomialNutation<DerivativeStructure>(  -2.5976176 * Constants.ARC_SECONDS_TO_RADIANS,
                                           2306.0803226 * Constants.ARC_SECONDS_TO_RADIANS,
                                              1.0947790 * Constants.ARC_SECONDS_TO_RADIANS,
                                              0.0182273 * Constants.ARC_SECONDS_TO_RADIANS,
                                              0.0000470 * Constants.ARC_SECONDS_TO_RADIANS,
                                             -0.0000003 * Constants.ARC_SECONDS_TO_RADIANS);

            public Transform getTransform(AbsoluteDate date) {
                final double tc = IERSConventions.IERS_2003.evaluateTC(date);
                final Rotation r1 = new Rotation(Vector3D.PLUS_K,  zA.value(tc), RotationConvention.VECTOR_OPERATOR);
                final Rotation r2 = new Rotation(Vector3D.PLUS_J, -thetaA.value(tc), RotationConvention.VECTOR_OPERATOR);
                final Rotation r3 = new Rotation(Vector3D.PLUS_K,  zetaA.value(tc), RotationConvention.VECTOR_OPERATOR);
                return new Transform(date, r1.compose(r2.compose(r3,
                                                                 RotationConvention.VECTOR_OPERATOR),
                                                      RotationConvention.VECTOR_OPERATOR));
            }
        };

        MODProvider modProvider = new MODProvider(IERSConventions.IERS_2003);

        for (double dt = -Constants.JULIAN_CENTURY; dt < Constants.JULIAN_CENTURY; dt += 50 * Constants.JULIAN_DAY) {
            AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(dt);
            Transform t = new Transform(date,
                                        modProvider.getTransform(date).getInverse(),
                                        eulerBasedProvider.getTransform(date));
            Assert.assertEquals(0, t.getRotation().getAngle(), 6.6e-13);
        }

    }

    @Test
    public void testAASReferenceLEO() throws OrekitException {

        // this reference test has been extracted from the following paper:
        // Implementation Issues Surrounding the New IAU Reference Systems for Astrodynamics
        // David A. Vallado, John H. Seago, P. Kenneth Seidelmann
        // http://www.centerforspace.com/downloads/files/pubs/AAS-06-134.pdf
        AbsoluteDate t0 = new AbsoluteDate(new DateComponents(2004, 04, 06),
                                           new TimeComponents(07, 51, 28.386009),
                                           TimeScalesFactory.getUTC());

        Transform tt = FramesFactory.getGCRF().getTransformTo(FramesFactory.getMOD(IERSConventions.IERS_1996), t0);
        //GCRF iau76 w corr
        PVCoordinates pvGCRFiau76 =
            new PVCoordinates(new Vector3D(5102508.9579, 6123011.4007, 6378136.9282),
                              new Vector3D(-4743.220157, 790.536497, 5533.755727));
        double norm = pvGCRFiau76.getPosition().getNorm();

        //MOD iau76 w corr
        PVCoordinates pvMODiau76Wcorr =
            new PVCoordinates(new Vector3D(5094028.3745, 6127870.8164, 6380248.5164),
                              new Vector3D(-4746.263052, 786.014045, 5531.790562));

        checkPV(pvMODiau76Wcorr, tt.transformPVCoordinates(pvGCRFiau76), 9e-12 * norm, 7.3e-7);

        Transform tf = FramesFactory.getEME2000().getTransformTo(FramesFactory.getMOD(false), t0);
        //J2000 iau76
        PVCoordinates pvJ2000iau76 =
            new PVCoordinates(new Vector3D(5102509.6000, 6123011.5200, 6378136.3000),
                              new Vector3D(-4743.219600, 790.536600, 5533.756190));
        //MOD iau76
        PVCoordinates pvMODiau76 =
            new PVCoordinates(new Vector3D(5094029.0167, 6127870.9363, 6380247.8885),
                              new Vector3D(-4746.262495, 786.014149, 5531.791025));
        checkPV(pvMODiau76, tf.transformPVCoordinates(pvJ2000iau76), 9e-12 * norm, 3.1e-7);

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

        Transform tt = FramesFactory.getGCRF().getTransformTo(FramesFactory.getMOD(IERSConventions.IERS_1996), t0);
        //GCRF iau76 w corr
        PVCoordinates pvGCRFiau76 =
            new PVCoordinates(new Vector3D(-40588150.3649, -11462167.0282, 27143.2028),
                              new Vector3D(834.787457, -2958.305691, -1.172994));
        double norm = pvGCRFiau76.getPosition().getNorm();

        //MOD iau76 w corr
        PVCoordinates pvMODiau76Wcorr =
            new PVCoordinates(new Vector3D(-40576822.6395, -11502231.5015, 9733.7842),
                              new Vector3D(837.708020, -2957.480117, -0.814253));
        checkPV(pvMODiau76Wcorr, tt.transformPVCoordinates(pvGCRFiau76), 9e-12 * norm, 6.9e-7);

        Transform tf = FramesFactory.getEME2000().getTransformTo(FramesFactory.getMOD(false), t0);
        //J2000 iau76
        PVCoordinates pvJ2000iau76 =
            new PVCoordinates(new Vector3D(-40588150.3620, -11462167.0280, 27147.6490),
                              new Vector3D(834.787457, -2958.305691, -1.173016));
        //MOD iau76
        PVCoordinates pvMODiau76 =
            new PVCoordinates(new Vector3D(-40576822.6385, -11502231.5013, 9738.2304),
                              new Vector3D(837.708020, -2957.480118, -0.814275));
        checkPV(pvMODiau76, tf.transformPVCoordinates(pvJ2000iau76), 9e-12 * norm, 6.9e-7);

    }

    @Test
    public void testSofaPmat76() throws OrekitException {

        // the reference value has been computed using the March 2012 version of the SOFA library
        // http://www.iausofa.org/2012_0301_C.html, with the following code
        //
        //        double utc1, utc2, tai1, tai2, tt1, tt2, rmatp[3][3];
        //
        //        // 2004-02-14:00:00:00Z, MJD = 53049, UT1-UTC = -0.4093509
        //        utc1  = DJM0 + 53049.0;
        //        utc2  = 0.0;
        //        iauUtctai(utc1, utc2, &tai1, &tai2);
        //        iauTaitt(tai1, tai2, &tt1, &tt2);
        //
        //        iauPmat76(tt1, tt2, rmatp);
        //
        //        printf("iauPmat76(%.20g, %.20g,rmatp)\n"
        //               "  --> %.20g %.20g %.20g\n"
        //               "      %.20g %.20g %.20g\n"
        //               "      %.20g %.20g %.20g\n",
        //               tt1, tt2,
        //               rmatp[0][0], rmatp[0][1], rmatp[0][2],
        //               rmatp[1][0], rmatp[1][1], rmatp[1][2],
        //               rmatp[2][0], rmatp[2][1], rmatp[2][2]);
        //
        // the output of this test reads:
        //      iauPmat76(2453049.5, 0.00074287037037037029902,rmatp)
        //        --> 0.9999994956729996165 -0.00092111268696996330928 -0.00040025637336518803469
        //            0.00092111268696944094067 0.99999957577560194544 -1.843419633938077413e-07
        //            0.00040025637336639019806 -1.8433935312187383064e-07 0.99999991989739756004

        // the SOFA routine iauPmat76 uses the Euler angles zetaA, thetaA and zA, whereas
        // Orekit uses the "canonical 4-rotation method" with angles epsilon0, psiA, omegaA
        // and chiA. As seen in the thresholds of the test testEuler1976 above, the two methods
        // drift with respect to each other at a rate about 2e-12 radians/year (i.e. about 0.42
        // microarcseconds per year) on the time range around J2000.0). We see the same models
        // difference in this test as in the testEuler1976 above.
        // Note that the difference is much smaller for IERS 2003 (see another test below)
        // because the Euler models for these conventions match much better the 4-rotation angles

        AbsoluteDate date = new AbsoluteDate(2004, 2, 14, TimeScalesFactory.getUTC());
        Frame mod  = FramesFactory.getFrame(Predefined.MOD_CONVENTIONS_1996);
        Frame gcrf = FramesFactory.getFrame(Predefined.GCRF);
        checkRotation(new double[][] {
            { 0.9999994956729996165,     -0.00092111268696996330928, -0.00040025637336518803469 },
            { 0.00092111268696944094067,  0.99999957577560194544,    -1.843419633938077413e-07  },
            { 0.00040025637336639019806, -1.8433935312187383064e-07,  0.99999991989739756004    }

        }, gcrf.getTransformTo(mod, date), 9e-12);

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
        Frame mod  = FramesFactory.getFrame(Predefined.MOD_CONVENTIONS_2003);
        Frame gcrf = FramesFactory.getFrame(Predefined.GCRF);
        checkRotation(new double[][] {
            { 0.99999949570013624278,    -0.00092112855376512230675, -0.00040015204695196122638 },
            { 0.00092112856903123034591,  0.99999957576097886491,    -1.4614501776464880046e-07 },
            { 0.00040015201181019732432, -2.2244653837776004327e-07,  0.99999991993915571253    }

        }, gcrf.getTransformTo(mod, date), 4.0e-16);

    }

    @Test
    public void testSofaBp06() throws OrekitException {

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
        //        iauBp06(tt1, tt2, rb, rp, rbp);
        //
        //        printf("iauBp06(%.20g, %.20g, rb, rp, rbp)\n"
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
        //      iauBp06(2453049.5, 0.00074287037037037029902, rb, rp, rbp)
        //        rb  --> 0.99999999999999411582 -7.0783689609715561276e-08 8.0562139776131860839e-08
        //                7.0783686946376762683e-08 0.99999999999999689138 3.3059437354321374869e-08
        //                -8.0562142116200574817e-08 -3.3059431692183949281e-08 0.99999999999999622524
        //        rp  --> 0.99999949573328705821 -0.00092105756953249828464 -0.00040023258864902886648
        //                0.00092105757159046265214 0.99999957582636889164 -1.7917675327629716543e-07
        //                0.00040023258391302344184 -1.8946059324815925032e-07 0.99999991990691827759
        //        rbp --> 0.99999949570032919954 -0.00092112833995494939818 -0.00040015205699952106997
        //                0.00092112835526181411488 0.9999995757611759295 -1.4604312753574433259e-07
        //                0.00040015202176394668336 -2.2254835219115420841e-07 0.99999991993915182675

        AbsoluteDate date = new AbsoluteDate(2004, 2, 14, TimeScalesFactory.getUTC());
        Frame mod  = FramesFactory.getFrame(Predefined.MOD_CONVENTIONS_2010);
        Frame gcrf = FramesFactory.getFrame(Predefined.GCRF);
        checkRotation(new double[][] {
            { 0.99999949570032919954,    -0.00092112833995494939818, -0.00040015205699952106997 },
            { 0.00092112835526181411488,  0.9999995757611759295,     -1.4604312753574433259e-07 },
            { 0.00040015202176394668336, -2.2254835219115420841e-07,  0.99999991993915182675    }

        }, gcrf.getTransformTo(mod, date), 1.1e-12);

    }

    @Test
    public void testMOD1976vs2006() throws OrekitException {

        final Frame mod1976 = FramesFactory.getMOD(IERSConventions.IERS_1996);
        final Frame mod2006 = FramesFactory.getMOD(IERSConventions.IERS_2010);
        for (double dt = 0; dt < 10 * Constants.JULIAN_YEAR; dt += 10 * Constants.JULIAN_DAY) {
            AbsoluteDate date = new AbsoluteDate(AbsoluteDate.J2000_EPOCH, dt);
            double delta = mod1976.getTransformTo(mod2006, date).getRotation().getAngle();
            // MOD2006 and MOD2000 are similar to about 33 milli-arcseconds between 2000 and 2010
            Assert.assertEquals(0.0, delta, 2.0e-7);
        }
    }

    @Test
    public void testMOD2000vs2006() throws OrekitException {

        final Frame mod2000 = FramesFactory.getMOD(IERSConventions.IERS_2003);
        final Frame mod2006 = FramesFactory.getMOD(IERSConventions.IERS_2010);
        for (double dt = 0; dt < 10 * Constants.JULIAN_YEAR; dt += 10 * Constants.JULIAN_DAY) {
            AbsoluteDate date = new AbsoluteDate(AbsoluteDate.J2000_EPOCH, dt);
            double delta = mod2000.getTransformTo(mod2006, date).getRotation().getAngle();
            // MOD2006 and MOD2000 are similar to about 0.15 milli-arcseconds between 2000 and 2010
            Assert.assertEquals(0.0, delta, 7.2e-10);
        }
    }

    @Test
    public void testSerialization() throws OrekitException, IOException, ClassNotFoundException {
        MODProvider provider = new MODProvider(IERSConventions.IERS_2010);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream    oos = new ObjectOutputStream(bos);
        oos.writeObject(provider);

        Assert.assertTrue(bos.size() > 150);
        Assert.assertTrue(bos.size() < 250);

        ByteArrayInputStream  bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream     ois = new ObjectInputStream(bis);
        MODProvider deserialized  = (MODProvider) ois.readObject();
        for (double dt = 0; dt < Constants.JULIAN_DAY; dt += 3600) {
            AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(dt);
            Transform expectedIdentity = new Transform(date,
                                                       provider.getTransform(date).getInverse(),
                                                       deserialized.getTransform(date));
            Assert.assertEquals(0.0, expectedIdentity.getTranslation().getNorm(), 1.0e-15);
            Assert.assertEquals(0.0, expectedIdentity.getRotation().getAngle(),   1.0e-15);
        }

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("compressed-data");
    }

    private void checkPV(PVCoordinates reference,
                         PVCoordinates result,
                         double positionThreshold,
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
                Assert.assertEquals(reference[i][j], mat[i][j], epsilon);
            }
        }
    }

}
