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
package org.orekit.utils;


import org.hipparchus.analysis.UnivariateFunction;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.analysis.differentiation.FiniteDifferencesDifferentiator;
import org.hipparchus.analysis.differentiation.UnivariateDifferentiableFunction;
import org.hipparchus.util.MathUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.frames.EOPHistory;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeFunction;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.time.UT1Scale;


public class IERSConventionsTest {

    @Test
    public void testConventionsNumber() {
        Assert.assertEquals(3, IERSConventions.values().length);
    }

    @Test
    public void testIERS1996NutationAngles() throws OrekitException {

        // the reference value has been computed using the March 2012 version of the SOFA library
        // http://www.iausofa.org/2012_0301_C.html, with the following code
        //
        //        double utc1, utc2, tai1, tai2, tt1, tt2, dpsi, deps, epsa;
        //
        //        // 2004-02-14:00:00:00Z, MJD = 53049, UT1-UTC = -0.4093509
        //        utc1  = DJM0 + 53049.0;
        //        utc2  = 0.0;
        //        iauUtctai(utc1, utc2, &tai1, &tai2);
        //        iauTaitt(tai1, tai2, &tt1, &tt2);
        //
        //        iauNut80(tt1, tt2, &dpsi, &deps);
        //        epsa = iauObl80(tt1, tt2);
        //
        //        printf("iauNut80(%.20g, %.20g, dpsi, deps)\n  --> %.20g %.20g\n",
        //               tt1, tt2, dpsi, deps);
        //        printf("iau0bl80(%.20g, %.20g)\n  --> %.20g\n",
        //               tt1, tt2, epsa);
        //
        // the output of this test reads:
        //      iauNut80(2453049.5, 0.00074287037037037029902, dpsi, deps)
        //        --> -5.3059154211478291722e-05 3.2051803135750973851e-05
        //      iau0bl80(2453049.5, 0.00074287037037037029902)
        //        --> 0.40908345528446415917

        // the thresholds below have been set up large enough to have the test pass with
        // the default Orekit setting and the reference SOFA setting. There are implementation
        // differences between the two libraries, as the Delaunay parameters are not the
        // same (in Orekit, we are compliant with page 23 in chapter 5 of IERS conventions 1996.
        // If the content of the IERS-conventions/1996/nutation-arguments.txt file by Orekit is
        // changed to match SOFA setting instead of IERS conventions as follows:
        //
        //        # Mean Anomaly of the Moon
        //        F1 ≡ l = 485866.733″ + 1717915922.633″t + 31.310″t² + 0.064″t³
        //
        //       # Mean Anomaly of the Sun
        //        F2 ≡ l' = 1287099.804″ + 129596581.224″t − 0.577″t² − 0.012t³
        //
        //       # L − Ω
        //        F3 ≡ F = 335778.877″ + 1739527263.137″t − 13.257″t² + 0.011″t³
        //
        //       # Mean Elongation of the Moon from the Sun
        //        F4 ≡ D = 1072261.307″ + 1602961601.328″t − 6.891″t² + 0.019″t³
        //
        //       # Mean Longitude of the Ascending Node of the Moon
        //        F5 ≡ Ω = 450160.280″ − 6962890.539″t + 7.455″t² + 0.008″t³
        //
        // then the thresholds for the test can be reduced to 3e-13 for ∆ψ and 7e-14 for ∆ε.
        // We decided to stick with IERS published reference values for the default Orekit setting.
        // The differences are nevertheless quite small (4.8e-11 radians is sub-millimeter level
        // in low Earth orbit).
        AbsoluteDate date = new AbsoluteDate(2004, 2, 14, TimeScalesFactory.getUTC());
        double[] angles= IERSConventions.IERS_1996.getNutationFunction().value(date);
        Assert.assertEquals(-5.3059154211478291722e-05, angles[0], 4.8e-11); // 3e-13 with SOFA values
        Assert.assertEquals(3.2051803135750973851e-05,  angles[1], 1.3e-11); // 7e-14 with SOFA values

        double epsilonA = IERSConventions.IERS_1996.getMeanObliquityFunction().value(date);
        Assert.assertEquals(0.40908345528446415917,     epsilonA, 1.0e-16);

    }

    @Test
    public void testGMST82Derivative() throws OrekitException {
        final TimeScale ut1 = TimeScalesFactory.getUT1(IERSConventions.IERS_1996, true);
        checkDerivative(IERSConventions.IERS_1996.getGMSTFunction(ut1),
                        AbsoluteDate.J2000_EPOCH.shiftedBy(-0.4 * Constants.JULIAN_DAY),
                        0.8 * Constants.JULIAN_DAY, 600.0, 10.0, 1.0e-12);
    }

    @Test
    public void testGMST82Sofa() throws OrekitException {

        // the reference value has been computed using the March 2012 version of the SOFA library
        // http://www.iausofa.org/2012_0301_C.html, with the following code
        //
        //        double utc1, utc2, ut11, ut12, gmst;
        //
        //        // 2004-02-14:00:00:00Z, MJD = 53049, UT1-UTC = -0.4093509
        //        utc1  = DJM0 + 53049.0;
        //        utc2  = 0.0;
        //        iauUtcut1(utc1, utc2, -0.4093509, &ut11, &ut12);
        //        gmst = iauGmst82(ut11, ut12);
        //        printf("iaugmst82(%.20g, %.20g)\n  --> %.20g\n", ut11, ut12, gmst);
        //
        //        // 2004-02-29:00:00:00Z, MJD = 53064, UT1-UTC = -0.4175723
        //        utc1 = DJM0 + 53064.0;
        //        utc2 = 0.0;
        //        iauUtcut1(utc1, utc2, -0.4175723, &ut11, &ut12);
        //        gmst = iauGmst82(ut11, ut12);
        //        printf("iaugmst82(%.20g, %.20g)\n  --> %.20g\n", ut11, ut12, gmst);
        //
        // the output of this test reads:
        //      iaugmst82(2453049.5, -4.7378576388888813016e-06)
        //        --> 2.5021977627453466653
        //      iaugmst82(2453064.5, -4.8330127314815448519e-06)
        //        --> 2.7602390405411441066

        final TimeScale ut1 = TimeScalesFactory.getUT1(IERSConventions.IERS_1996, true);
        final TimeFunction<DerivativeStructure> gmst82 =
                IERSConventions.IERS_1996.getGMSTFunction(ut1);
        AbsoluteDate date = new AbsoluteDate(2004, 2, 14, TimeScalesFactory.getUTC());
        double gmst = MathUtils.normalizeAngle(gmst82.value(date).getValue(), 0.0);
        Assert.assertEquals(2.5021977627453466653, gmst, 2.0e-13);
        date = new AbsoluteDate(2004, 2, 29, TimeScalesFactory.getUTC());
        gmst = MathUtils.normalizeAngle(gmst82.value(date).getValue(), 0.0);
        Assert.assertEquals(2.7602390405411441066, gmst, 4.0e-13);

    }

    @Test
    public void testGMST00Derivative() throws OrekitException {
        final TimeScale ut1 = TimeScalesFactory.getUT1(IERSConventions.IERS_2003, true);
        checkDerivative(IERSConventions.IERS_2003.getGMSTFunction(ut1),
                        AbsoluteDate.J2000_EPOCH.shiftedBy(-0.4 * Constants.JULIAN_DAY),
                        0.8 * Constants.JULIAN_DAY, 600.0, 10.0, 1.0e-12);
    }

    @Test
    public void testGMST00Sofa() throws OrekitException {

        // the reference value has been computed using the March 2012 version of the SOFA library
        // http://www.iausofa.org/2012_0301_C.html, with the following code
        //
        //        double utc1, utc2, tai1, tai2, tt1, tt2, ut11, ut12, gmst;
        //
        //        // 2004-02-14:00:00:00Z, MJD = 53049, UT1-UTC = -0.4093509
        //        utc1  = DJM0 + 53049.0;
        //        utc2  = 0.0;
        //        iauUtctai(utc1, utc2, &tai1, &tai2);
        //        iauTaitt(tai1, tai2, &tt1, &tt2);
        //        iauUtcut1(utc1, utc2, -0.4093509, &ut11, &ut12);
        //        gmst = iauGmst00(ut11, ut12, tt1, tt2);
        //        printf("iaugmst00(%.20g, %.20g, %.20g, %.20g)\n  --> %.20g\n",
        //               ut11, ut12, tt1, tt2, gmst);
        //
        //        // 2004-02-29:00:00:00Z, MJD = 53064, UT1-UTC = -0.4175723
        //        utc1 = DJM0 + 53064.0;
        //        utc2 = 0.0;
        //        iauUtctai(utc1, utc2, &tai1, &tai2);
        //        iauTaitt(tai1, tai2, &tt1, &tt2);
        //        iauUtcut1(utc1, utc2, -0.4175723, &ut11, &ut12);
        //        gmst = iauGmst00(ut11, ut12, tt1, tt2);
        //        printf("iaugmst00(%.20g, %.20g, %.20g, %.20g)\n  --> %.20g\n",
        //               ut11, ut12, tt1, tt2, gmst);
        //
        // the output of this test reads:
        //      iaugmst00(2453049.5, -4.7378576388888813016e-06, 2453049.5, 0.00074287037037037029902)
        //        --> 2.5021977786243789765
        //      iaugmst00(2453064.5, -4.8330127314815448519e-06, 2453064.5, 0.00074287037037037029902)
        //        --> 2.7602390558728311376

        final TimeScale ut1 = TimeScalesFactory.getUT1(IERSConventions.IERS_2003, true);
        final TimeFunction<DerivativeStructure> gmst00 =
                IERSConventions.IERS_2003.getGMSTFunction(ut1);
        AbsoluteDate date = new AbsoluteDate(2004, 2, 14, TimeScalesFactory.getUTC());
        double gmst = MathUtils.normalizeAngle(gmst00.value(date).getValue(), 0.0);
        Assert.assertEquals(2.5021977786243789765, gmst, 1.0e-15);
        date = new AbsoluteDate(2004, 2, 29, TimeScalesFactory.getUTC());
        gmst = MathUtils.normalizeAngle(gmst00.value(date).getValue(), 0.0);
        Assert.assertEquals(2.7602390558728311376, gmst, 1.0e-15);

    }

    @Test
    public void testGMST06Derivative() throws OrekitException {
        final TimeScale ut1 = TimeScalesFactory.getUT1(IERSConventions.IERS_2010, true);
        checkDerivative(IERSConventions.IERS_2010.getGMSTFunction(ut1),
                        AbsoluteDate.J2000_EPOCH.shiftedBy(-0.4 * Constants.JULIAN_DAY),
                        0.8 * Constants.JULIAN_DAY, 600.0, 10.0, 1.0e-12);
    }

    @Test
    public void testGMST06Sofa() throws OrekitException {

        // the reference value has been computed using the March 2012 version of the SOFA library
        // http://www.iausofa.org/2012_0301_C.html, with the following code
        //
        //        double utc1, utc2, tai1, tai2, tt1, tt2, ut11, ut12, gmst;
        //
        //        // 2004-02-14:00:00:00Z, MJD = 53049, UT1-UTC = -0.4093509
        //        utc1  = DJM0 + 53049.0;
        //        utc2  = 0.0;
        //        iauUtctai(utc1, utc2, &tai1, &tai2);
        //        iauTaitt(tai1, tai2, &tt1, &tt2);
        //        iauUtcut1(utc1, utc2, -0.4093509, &ut11, &ut12);
        //        gmst = iauGmst06(ut11, ut12, tt1, tt2);
        //        printf("iaugmst06(%.20g, %.20g, %.20g, %.20g)\n  --> %.20g\n",
        //               ut11, ut12, tt1, tt2, gmst);
        //
        //        // 2004-02-29:00:00:00Z, MJD = 53064, UT1-UTC = -0.4175723
        //        utc1 = DJM0 + 53064.0;
        //        utc2 = 0.0;
        //        iauUtctai(utc1, utc2, &tai1, &tai2);
        //        iauTaitt(tai1, tai2, &tt1, &tt2);
        //        iauUtcut1(utc1, utc2, -0.4175723, &ut11, &ut12);
        //        gmst = iauGmst06(ut11, ut12, tt1, tt2);
        //        printf("iaugmst06(%.20g, %.20g, %.20g, %.20g)\n  --> %.20g\n",
        //               ut11, ut12, tt1, tt2, gmst);
        //
        // the output of this test reads:
        //      iaugmst06(2453049.5, -4.7378576388888813016e-06, 2453049.5, 0.00074287037037037029902)
        //        --> 2.5021977784096232078
        //      iaugmst06(2453064.5, -4.8330127314815448519e-06, 2453064.5, 0.00074287037037037029902)
        //        --> 2.7602390556555129741

        final TimeScale ut1 = TimeScalesFactory.getUT1(IERSConventions.IERS_2010, true);
        final TimeFunction<DerivativeStructure> gmst06 =
                IERSConventions.IERS_2010.getGMSTFunction(ut1);
        AbsoluteDate date = new AbsoluteDate(2004, 2, 14, TimeScalesFactory.getUTC());
        double gmst = MathUtils.normalizeAngle(gmst06.value(date).getValue(), 0.0);
        Assert.assertEquals(2.5021977784096232078, gmst, 1.0e-15);
        date = new AbsoluteDate(2004, 2, 29, TimeScalesFactory.getUTC());
        gmst = MathUtils.normalizeAngle(gmst06.value(date).getValue(), 0.0);
        Assert.assertEquals(2.7602390556555129741, gmst, 3.0e-15);

    }

    @Test
    public void testERADerivative() throws OrekitException {
        final TimeScale ut1 = TimeScalesFactory.getUT1(IERSConventions.IERS_2010, true);
        checkDerivative(IERSConventions.IERS_2010.getGMSTFunction(ut1),
                        AbsoluteDate.J2000_EPOCH.shiftedBy(-0.4 * Constants.JULIAN_DAY),
                        0.8 * Constants.JULIAN_DAY, 1800.0, 10.0, 1.0e-12);
    }

    @Test
    public void testERASofa() throws OrekitException {

        // the reference value has been computed using the March 2012 version of the SOFA library
        // http://www.iausofa.org/2012_0301_C.html, with the following code
        //
        //        double tai1, tai2, ut11, ut12, era, taiutc;
        //        taiutc = 32.0;
        //
        //        // 2004-02-14:00:00:00Z, MJD = 53049, UT1-UTC = -0.4093509
        //        tai1 = DJM0 + 53049.0;
        //        tai2 = taiutc / DAYSEC;
        //        iauTaiut1(tai1, tai2, -0.4093509 - taiutc, &ut11, &ut12);
        //        era = iauEra00(ut11, ut12);
        //        printf("iauera00(%.20g, %.20g)\n  --> %.20g\n", ut11, ut12, era);
        //
        //        // 2004-02-29:00:00:00Z, MJD = 53064, UT1-UTC = -0.4175723
        //        tai1 = DJM0 + 53064.0;
        //        tai2 = taiutc / DAYSEC;
        //        iauTaiut1(tai1, tai2, -0.4175723 - taiutc, &ut11, &ut12);
        //        era = iauEra00(ut11, ut12);
        //        printf("iauera00(%.20g, %.20g)\n  --> %.20g\n", ut11, ut12, era);
        //
        // the output of this test reads:
        //      iauera00(2453049.5, -4.7378576388888813016e-06)
        //        --> 2.5012766511308228701
        //      iauera00(2453064.5, -4.8330127314815448519e-06)
        //        --> 2.7593087452455264952

        final TimeScale ut1 = TimeScalesFactory.getUT1(IERSConventions.IERS_2003, true);
        final TimeFunction<DerivativeStructure> era00 =
                IERSConventions.IERS_2003.getEarthOrientationAngleFunction(ut1);
        AbsoluteDate date = new AbsoluteDate(2004, 2, 14, TimeScalesFactory.getUTC());
        double era  = MathUtils.normalizeAngle(era00.value(date).getValue(), 0.0);
        Assert.assertEquals(2.5012766511308228701, era, 1.0e-15);
        date = new AbsoluteDate(2004, 2, 29, TimeScalesFactory.getUTC());
        era  = MathUtils.normalizeAngle(era00.value(date).getValue(), 0.0);
        Assert.assertEquals(2.7593087452455264952, era, 1.0e-15);

    }

    @Test
    public void testGST94Derivative() throws OrekitException {
        EOPHistory eopHistory = FramesFactory.getEOPHistory(IERSConventions.IERS_1996, true);
        checkDerivative(IERSConventions.IERS_1996.getGASTFunction(TimeScalesFactory.getUT1(eopHistory),
                                                                  eopHistory),
                        AbsoluteDate.J2000_EPOCH.shiftedBy(-0.4 * Constants.JULIAN_DAY),
                        0.8 * Constants.JULIAN_DAY, 1800.0, 10.0, 2.0e-12);
    }

    @Test
    public void testGST94Sofa() throws OrekitException {

        // the reference value has been computed using the March 2012 version of the SOFA library
        // http://www.iausofa.org/2012_0301_C.html, with the following code
        //
        //        double utc1, utc2, ut11, ut12, tai1, tai2, tt1, tt2, gmst82, eqeq94;
        //
        //        // 2004-02-14:00:00:00Z, MJD = 53049, UT1-UTC = -0.4093509
        //        utc1  = DJM0 + 53049.0;
        //        utc2  = 0.0;
        //        iauUtcut1(utc1, utc2, -0.4093509, &ut11, &ut12);
        //        iauUtctai(utc1, utc2, &tai1, &tai2);
        //        iauTaitt(tai1, tai2, &tt1, &tt2);
        //        gmst82 = iauGmst82(ut11, ut12);
        //        eqeq94 = iauEqeq94(tt1, tt2);
        //        printf("iauGmst82(%.20g, %.20g)\n  --> %.20g\n", ut11, ut12, gmst82);
        //        printf("iauEqeq94(%.20g, %.20g)\n  --> %.20g\n", tt1, tt2, eqeq94);
        //        printf(" gmst82 + eqeq94  --> %.20g\n", gmst82 + eqeq94);
        //
        //        // 2004-02-29:00:00:00Z, MJD = 53064, UT1-UTC = -0.4175723
        //        utc1 = DJM0 + 53064.0;
        //        utc2 = 0.0;
        //        iauUtcut1(utc1, utc2, -0.4175723, &ut11, &ut12);
        //        iauUtctai(utc1, utc2, &tai1, &tai2);
        //        iauTaitt(tai1, tai2, &tt1, &tt2);
        //        gmst82 = iauGmst82(ut11, ut12);
        //        eqeq94 = iauEqeq94(tt1, tt2);
        //        printf("iauGmst82(%.20g, %.20g)\n  --> %.20g\n", ut11, ut12, gmst82);
        //        printf("iauEqeq94(%.20g, %.20g)\n  --> %.20g\n", tt1, tt2, eqeq94);
        //        printf(" gmst82 + eqeq94  --> %.20g\n", gmst82 + eqeq94);
        //
        // the output of this test reads:
        //      iauGmst82(2453049.5, -4.7378576388888813016e-06)
        //        --> 2.5021977627453466653
        //      iauEqeq94(2453049.5, 0.00074287037037037029902)
        //        --> -4.8671604682267536886e-05
        //       gmst82 + eqeq94  --> 2.5021490911406645274
        //      iauGmst82(2453064.5, -4.8330127314815448519e-06)
        //        --> 2.7602390405411441066
        //      iauEqeq94(2453064.5, 0.00074287037037037029902)
        //        --> -4.8893054690771762302e-05
        //       gmst82 + eqeq94  --> 2.7601901474864534158

        // As can be seen in the code above, we didn't call iauGst94, because as
        // stated in the function header comment in SOFA source files:
        //        "accuracy has been compromised for the sake of
        //         convenience in that UT is used instead of TDB (or TT) to compute
        //         the equation of the equinoxes."
        // So we rather performed the date conversion and then called ourselves iauGmst82
        // with a date in UTC and eqe94 with a date in TT, restoring full accuracy in the
        // SOFA computation for this test.

        // The thresholds below have been set up large enough to have the test pass with
        // the default Orekit setting and the reference SOFA setting. There are implementation
        // differences between the two libraries, as the Delaunay parameters are not the
        // same (in Orekit, we are compliant with page 23 in chapter 5 of IERS conventions 1996.
        // If the content of the IERS-conventions/1996/nutation-arguments.txt file by Orekit is
        // changed to match SOFA setting instead of IERS conventions as follows:
        //
        //        # Mean Anomaly of the Moon
        //        F1 ≡ l = 485866.733″ + 1717915922.633″t + 31.310″t² + 0.064″t³
        //
        //       # Mean Anomaly of the Sun
        //        F2 ≡ l' = 1287099.804″ + 129596581.224″t − 0.577″t² − 0.012t³
        //
        //       # L − Ω
        //        F3 ≡ F = 335778.877″ + 1739527263.137″t − 13.257″t² + 0.011″t³
        //
        //       # Mean Elongation of the Moon from the Sun
        //        F4 ≡ D = 1072261.307″ + 1602961601.328″t − 6.891″t² + 0.019″t³
        //
        //       # Mean Longitude of the Ascending Node of the Moon
        //        F5 ≡ Ω = 450160.280″ − 6962890.539″t + 7.455″t² + 0.008″t³
        //
        // then the thresholds for the test can be reduced to 4.2e-13 for the first test,
        // and 4.7e-13 for the second test.
        // We decided to stick with IERS published reference values for the default Orekit setting.
        // The differences are nevertheless quite small (9e-11 radians is sub-millimeter level
        // in low Earth orbit).
        Utils.setLoaders(IERSConventions.IERS_1996,
                         Utils.buildEOPList(IERSConventions.IERS_1996, new double[][] {
                             { 53047, -0.4093509, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00 },
                             { 53048, -0.4093509, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00 },
                             { 53049, -0.4093509, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00 },
                             { 53050, -0.4093509, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00 },
                             { 53051, -0.4093509, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00 },
                             { 53052, -0.4093509, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00 },
                             { 53053, -0.4093509, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00 },
                             { 53054, -0.4093509, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00 },
                             // ...
                             { 53059, -0.4175723, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00 },
                             { 53060, -0.4175723, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00 },
                             { 53061, -0.4175723, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00 },
                             { 53062, -0.4175723, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00 },
                             { 53063, -0.4175723, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00 },
                             { 53064, -0.4175723, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00 },
                             { 53065, -0.4175723, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00 },
                             { 53066, -0.4175723, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00 }
                         }));
        EOPHistory eopHistory = FramesFactory.getEOPHistory(IERSConventions.IERS_1996, true);
        final TimeFunction<DerivativeStructure> gst94 =
                IERSConventions.IERS_1996.getGASTFunction(TimeScalesFactory.getUT1(eopHistory),
                                                          eopHistory);
        AbsoluteDate date = new AbsoluteDate(2004, 2, 14, TimeScalesFactory.getUTC());
        double gst = MathUtils.normalizeAngle(gst94.value(date).getValue(), 0.0);
        Assert.assertEquals(2.5021490911406645274, gst, 4.4e-11); // 4.2e-13 with SOFA values
        date = new AbsoluteDate(2004, 2, 29, TimeScalesFactory.getUTC());
        gst = MathUtils.normalizeAngle(gst94.value(date).getValue(), 0.0);
        Assert.assertEquals(2.7601901474864534158, gst, 9.0e-11); // 4.7e-13 with SOFA values

    }

    @Test
    public void testGST00ADerivative() throws OrekitException {
        EOPHistory eopHistory = FramesFactory.getEOPHistory(IERSConventions.IERS_2003, true);
        checkDerivative(IERSConventions.IERS_2003.getGASTFunction(TimeScalesFactory.getUT1(eopHistory), eopHistory),
                        AbsoluteDate.J2000_EPOCH.shiftedBy(-0.4 * Constants.JULIAN_DAY),
                        0.8 * Constants.JULIAN_DAY, 1800.0, 10.0, 1.0e-11);
    }

    @Test
    public void testGST00ASofa() throws OrekitException {

        // the reference value has been computed using the March 2012 version of the SOFA library
        // http://www.iausofa.org/2012_0301_C.html, with the following code
        //
        //        double utc1, utc2, tai1, tai2, tt1, tt2, ut11, ut12, gst;
        //
        //        // 2004-02-14:00:00:00Z, MJD = 53049, UT1-UTC = -0.4093509
        //        utc1  = DJM0 + 53049.0;
        //        utc2  = 0.0;
        //        iauUtctai(utc1, utc2, &tai1, &tai2);
        //        iauTaitt(tai1, tai2, &tt1, &tt2);
        //        iauUtcut1(utc1, utc2, -0.4093509, &ut11, &ut12);
        //        gst = iauGst00a(ut11, ut12, tt1, tt2);
        //        printf("iaugst00a(%.20g, %.20g, %.20g, %.20g)\n  --> %.20g\n",
        //               ut11, ut12, tt1, tt2, gst);
        //
        //        // 2004-02-29:00:00:00Z, MJD = 53064, UT1-UTC = -0.4175723
        //        utc1 = DJM0 + 53064.0;
        //        utc2 = 0.0;
        //        iauUtctai(utc1, utc2, &tai1, &tai2);
        //        iauTaitt(tai1, tai2, &tt1, &tt2);
        //        iauUtcut1(utc1, utc2, -0.4175723, &ut11, &ut12);
        //        gst = iauGst00a(ut11, ut12, tt1, tt2);
        //        printf("iaugst00a(%.20g, %.20g, %.20g, %.20g)\n  --> %.20g\n",
        //               ut11, ut12, tt1, tt2, gst);
        //
        // the output of this test reads:
        //      iaugst00a(2453049.5, -4.7378576388888813016e-06, 2453049.5, 0.00074287037037037029902)
        //        --> 2.5021491024360624778
        //      iaugst00a(2453064.5, -4.8330127314815448519e-06, 2453064.5, 0.00074287037037037029902)
        //        --> 2.7601901615614221619

        Utils.setLoaders(IERSConventions.IERS_2003,
                         Utils.buildEOPList(IERSConventions.IERS_2003, new double[][] {
                             { 53047, -0.4093509, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00 },
                             { 53048, -0.4093509, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00 },
                             { 53049, -0.4093509, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00 },
                             { 53050, -0.4093509, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00 },
                             { 53051, -0.4093509, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00 },
                             { 53052, -0.4093509, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00 },
                             { 53053, -0.4093509, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00 },
                             { 53054, -0.4093509, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00 },
                             // ...
                             { 53059, -0.4175723, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00 },
                             { 53060, -0.4175723, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00 },
                             { 53061, -0.4175723, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00 },
                             { 53062, -0.4175723, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00 },
                             { 53063, -0.4175723, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00 },
                             { 53064, -0.4175723, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00 },
                             { 53065, -0.4175723, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00 },
                             { 53066, -0.4175723, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00 }
                         }));
        EOPHistory eopHistory = FramesFactory.getEOPHistory(IERSConventions.IERS_2003, true);
        final TimeFunction<DerivativeStructure> gst00a =
                IERSConventions.IERS_2003.getGASTFunction(TimeScalesFactory.getUT1(eopHistory), eopHistory);
        AbsoluteDate date = new AbsoluteDate(2004, 2, 14, TimeScalesFactory.getUTC());
        double gst = MathUtils.normalizeAngle(gst00a.value(date).getValue(), 0.0);
        Assert.assertEquals(2.5021491024360624778, gst, 2.0e-13);
        date = new AbsoluteDate(2004, 2, 29, TimeScalesFactory.getUTC());
        gst = MathUtils.normalizeAngle(gst00a.value(date).getValue(), 0.0);
        Assert.assertEquals(2.7601901615614221619, gst, 3.0e-13);

    }

    @Test
    public void testGST06Derivative() throws OrekitException {
        EOPHistory eopHistory = FramesFactory.getEOPHistory(IERSConventions.IERS_2010, true);
        checkDerivative(IERSConventions.IERS_2010.getGASTFunction(TimeScalesFactory.getUT1(eopHistory), eopHistory),
                        AbsoluteDate.J2000_EPOCH.shiftedBy(-0.4 * Constants.JULIAN_DAY),
                        0.8 * Constants.JULIAN_DAY, 1800.0, 10.0, 1.0e-11);
    }

    @Test
    public void testGST06Sofa() throws OrekitException {

        // the reference value has been computed using the March 2012 version of the SOFA library
        // http://www.iausofa.org/2012_0301_C.html, with the following code
        //
        //        double utc1, utc2, tai1, tai2, tt1, tt2, ut11, ut12, gst;
        //
        //        // 2004-02-14:00:00:00Z, MJD = 53049, UT1-UTC = -0.4093509
        //        utc1  = DJM0 + 53049.0;
        //        utc2  = 0.0;
        //        iauUtctai(utc1, utc2, &tai1, &tai2);
        //        iauTaitt(tai1, tai2, &tt1, &tt2);
        //        iauUtcut1(utc1, utc2, -0.4093509, &ut11, &ut12);
        //        gst = iauGst06a(ut11, ut12, tt1, tt2);
        //        printf("iaugst06a(%.20g, %.20g, %.20g, %.20g)\n  --> %.20g\n",
        //               ut11, ut12, tt1, tt2, gst);
        //
        //        // 2004-02-29:00:00:00Z, MJD = 53064, UT1-UTC = -0.4175723
        //        utc1 = DJM0 + 53064.0;
        //        utc2 = 0.0;
        //        iauUtctai(utc1, utc2, &tai1, &tai2);
        //        iauTaitt(tai1, tai2, &tt1, &tt2);
        //        iauUtcut1(utc1, utc2, -0.4175723, &ut11, &ut12);
        //        gst = iauGst06a(ut11, ut12, tt1, tt2);
        //        printf("iaugst06a(%.20g, %.20g, %.20g, %.20g)\n  --> %.20g\n",
        //               ut11, ut12, tt1, tt2, gst);
        //
        // the output of this test reads:
        //      iaugst06a(2453049.5, -4.7378576388888813016e-06, 2453049.5, 0.00074287037037037029902)
        //        --> 2.5021491022006503435
        //      iaugst06a(2453064.5, -4.8330127314815448519e-06, 2453064.5, 0.00074287037037037029902)
        //        --> 2.7601901613234058885

        Utils.setLoaders(IERSConventions.IERS_2010,
                         Utils.buildEOPList(IERSConventions.IERS_2010, new double[][] {
                             { 53047, -0.4093509, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00 },
                             { 53048, -0.4093509, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00 },
                             { 53049, -0.4093509, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00 },
                             { 53050, -0.4093509, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00 },
                             { 53051, -0.4093509, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00 },
                             { 53052, -0.4093509, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00 },
                             { 53053, -0.4093509, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00 },
                             { 53054, -0.4093509, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00 },
                             // ...
                             { 53059, -0.4175723, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00 },
                             { 53060, -0.4175723, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00 },
                             { 53061, -0.4175723, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00 },
                             { 53062, -0.4175723, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00 },
                             { 53063, -0.4175723, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00 },
                             { 53064, -0.4175723, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00 },
                             { 53065, -0.4175723, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00 },
                             { 53066, -0.4175723, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00 }
                         }));
        EOPHistory eopHistory = FramesFactory.getEOPHistory(IERSConventions.IERS_2010, true);
        final TimeFunction<DerivativeStructure> gst06 =
                IERSConventions.IERS_2010.getGASTFunction(TimeScalesFactory.getUT1(eopHistory), eopHistory);
        AbsoluteDate date = new AbsoluteDate(2004, 2, 14, TimeScalesFactory.getUTC());
        double gst = MathUtils.normalizeAngle(gst06.value(date).getValue(), 0.0);
        Assert.assertEquals(2.5021491022006503435, gst, 1.3e-12);
        date = new AbsoluteDate(2004, 2, 29, TimeScalesFactory.getUTC());
        gst = MathUtils.normalizeAngle(gst06.value(date).getValue(), 0.0);
        Assert.assertEquals(2.7601901613234058885, gst, 1.2e-12);

    }

    @Test
    public void testGMST2000vs82() throws OrekitException {

        final TimeFunction<DerivativeStructure> gmst82 =
                IERSConventions.IERS_1996.getGMSTFunction(TimeScalesFactory.getUT1(IERSConventions.IERS_1996, true));
        final TimeFunction<DerivativeStructure> gmst00 =
                IERSConventions.IERS_2003.getGMSTFunction(TimeScalesFactory.getUT1(IERSConventions.IERS_2003, true));
        for (double dt = 0; dt < 10 * Constants.JULIAN_YEAR; dt += 10 * Constants.JULIAN_DAY) {
            AbsoluteDate date = new AbsoluteDate(AbsoluteDate.J2000_EPOCH, dt);
            DerivativeStructure delta = gmst00.value(date).subtract(gmst82.value(date));
            // GMST82 and GMST2000 are similar to about 15 milli-arcseconds between 2000 and 2010
            Assert.assertEquals(0.0, MathUtils.normalizeAngle(delta.getValue(), 0.0), 7.1e-8);
            Assert.assertEquals(0.0, delta.getPartialDerivative(1), 1.0e-15);
        }
    }

    @Test
    public void testGMST2000vs2006() throws OrekitException {

        final UT1Scale ut1 = TimeScalesFactory.getUT1(IERSConventions.IERS_2010, true);
        final TimeFunction<DerivativeStructure> gmst00 =
                IERSConventions.IERS_2003.getGMSTFunction(ut1);
        final TimeFunction<DerivativeStructure> gmst06 =
                IERSConventions.IERS_2010.getGMSTFunction(ut1);
        for (double dt = 0; dt < 10 * Constants.JULIAN_YEAR; dt += 10 * Constants.JULIAN_DAY) {
            AbsoluteDate date = new AbsoluteDate(AbsoluteDate.J2000_EPOCH, dt);
            DerivativeStructure delta = gmst06.value(date).subtract(gmst00.value(date));
            // GMST2006 and GMST2000 are similar to about 0.15 milli-arcseconds between 2000 and 2010
            Assert.assertEquals(0.0, MathUtils.normalizeAngle(delta.getValue(), 0.0), 7e-10);
            Assert.assertEquals(0.0, delta.getPartialDerivative(1), 3.0e-18);
        }
    }

    @Test
    public void testIAU1994ResolutionC7Discontinuity() throws OrekitException {
        TimeFunction<double[]> nutation = IERSConventions.IERS_1996.getNutationFunction();
        AbsoluteDate switchDate = new AbsoluteDate(1997, 2, 27, TimeScalesFactory.getUTC());
        double h = 0.01;
        for (double dt = -1.0 - h / 2; dt <= 1.0 + h /2; dt += h) {
            AbsoluteDate d = switchDate.shiftedBy(dt);
            final double currentCorr = nutation.value(d)[2];
                if (dt < 0) {
                    Assert.assertEquals(0.0, currentCorr, 1.0e-20);
                } else {
                    Assert.assertEquals(-7.87098e-12, currentCorr, 1.0e-15);
                }
        }
    }

    @Test
    public void testTidalCorrection1996() throws OrekitException {

        // the reference value has been computed using interp_old.f from
        // ftp://hpiers.obspm.fr/iers/models/ with the following driver program:
        //
        //        PROGRAM OR_TEST
        //        IMPLICIT NONE
        //        INTEGER J, N
        //        PARAMETER (N = 4)
        //        INTEGER MJD(N)
        //        DOUBLE PRECISION TTTAI, TAIUTC
        //        PARAMETER (TAIUTC = 32.000D0)
        //        PARAMETER (TTTAI  = 32.184D0)
        //        DOUBLE PRECISION RJD(N), X(N), Y(N), T(N), H(5)
        //        DOUBLE PRECISION RJDINT, XINT, YINT, TINT, CORX, CORY, CORT
        //        DATA(MJD(J),    T(J),        X(J),      Y(J),
        //       &     J=1,4)/
        //       &    52653, -0.2979055D0, -0.120344D0, 0.217095D0,
        //       &    52654, -0.2984238D0, -0.121680D0, 0.219400D0,
        //       &    52655, -0.2987682D0, -0.122915D0, 0.221760D0,
        //       &    52656, -0.2989957D0, -0.124248D0, 0.224294D0/
        //  C
        //        DATA(H(J),J=1,5)/0.0D0, 3600.0D0, 7200.0D0, 43200.0D0, 86400.0D0/
        //  C
        //        DO 10 J = 1, N
        //            RJD(J) = MJD(J) + (TTTAI + TAIUTC) / 86400.0D0
        //   10   CONTINUE
        //  C
        //        DO 20 J = 1, 5
        //            RJDINT = RJD(2) + H(J) / 86400.0D0
        //            CALL INTERP(RJD,X,Y,T,N,RJDINT,XINT,YINT,TINT)
        //            WRITE(6, 30) H(J), TINT, XINT, YINT
        //   20   CONTINUE
        //   30   FORMAT(F7.1,3(1X, F20.17))
        //  C
        //        END PROGRAM
        //
        // the output of this test reads:
        //           0.0 -0.29839889612705234 -0.12191552977388567  0.21921143351558756
        //        3600.0 -0.29841696994736727 -0.12208276003864491  0.21925416583607277
        //        7200.0 -0.29843402412052122 -0.12218102086455683  0.21930263880545320
        //       43200.0 -0.29866785146390035 -0.12250027826538630  0.22103779809979979
        //       86400.0 -0.29874248853173840 -0.12308592577174847  0.22161565557764881

        Utils.setLoaders(IERSConventions.IERS_1996,
                         Utils.buildEOPList(IERSConventions.IERS_1996, new double[][] {
                             {  52653,  -0.2979055,   0.0005744,  -0.120344,   0.217095, 0.0, 0.0, 0.0, 0.0 },
                             {  52654,  -0.2984238,   0.0004224,  -0.121680,   0.219400, 0.0, 0.0, 0.0, 0.0 },
                             {  52655,  -0.2987682,   0.0002878,  -0.122915,   0.221760, 0.0, 0.0, 0.0, 0.0 },
                             {  52656,  -0.2989957,   0.0001778,  -0.124248,   0.224294, 0.0, 0.0, 0.0, 0.0 }
                         }));
        EOPHistory eopHistory = FramesFactory.getEOPHistory(IERSConventions.IERS_1996, false);

        final AbsoluteDate t0 = new AbsoluteDate(new DateComponents(DateComponents.MODIFIED_JULIAN_EPOCH, 52654),
                                           TimeScalesFactory.getUTC());
        for (double[] row : new double[][] {
            {     0.0, -0.29839889612705234, -0.12191552977388567,  0.21921143351558756 },
            {  3600.0, -0.29841696994736727, -0.12208276003864491,  0.21925416583607277 },
            {  7200.0, -0.29843402412052122, -0.12218102086455683,  0.21930263880545320 },
            { 43200.0, -0.29866785146390035, -0.12250027826538630,  0.22103779809979979 },
            { 86400.0, -0.29874248853173840, -0.12308592577174847,  0.22161565557764881 }
        }) {
            AbsoluteDate date = t0.shiftedBy(row[0]);
            Assert.assertEquals(row[1], eopHistory.getUT1MinusUTC(date), 8.8e-11);
            Assert.assertEquals(row[2] * Constants.ARC_SECONDS_TO_RADIANS,
                                eopHistory.getPoleCorrection(date).getXp(),
                                3.2e-14);
            Assert.assertEquals(row[3] * Constants.ARC_SECONDS_TO_RADIANS,
                                eopHistory.getPoleCorrection(date).getYp(),
                                8.2e-15);
        }

    }

    @Test
    public void testTidalCorrection2003() throws OrekitException {

        // the reference value has been computed using the September 2007 version
        // of interp.f from ftp://hpiers.obspm.fr/iers/models/ adding input/output
        // parameters for LOD (which was already computed in the underlying routines),
        // with the following driver program:
        //
        //        PROGRAM OR_TEST
        //        IMPLICIT NONE
        //        INTEGER J, N
        //        PARAMETER (N = 4)
        //        INTEGER MJD(N)
        //        DOUBLE PRECISION TTTAI, TAIUTC
        //        PARAMETER (TAIUTC = 32.000D0)
        //        PARAMETER (TTTAI  = 32.184D0)
        //        DOUBLE PRECISION RJD(N), UT1(N), LOD(N), X(N), Y(N), H(5)
        //        DOUBLE PRECISION RJD_INT, UT1_INT, LOD_INT, X_INT, Y_INT
        //        DATA(MJD(J),   UT1(J),     LOD(J),      X(J),      Y(J),
        //       &     J=1,4)/
        //       &    52653, -0.2979055D0, 0.0005744D0, -0.120344D0, 0.217095D0,
        //       &    52654, -0.2984238D0, 0.0004224D0, -0.121680D0, 0.219400D0,
        //       &    52655, -0.2987682D0, 0.0002878D0, -0.122915D0, 0.221760D0,
        //       &    52656, -0.2989957D0, 0.0001778D0, -0.124248D0, 0.224294D0/
        //  C
        //        DATA(H(J),J=1,5)/0.0D0, 3600.0D0, 7200.0D0, 43200.0D0, 86400.0D0/
        //  C
        //        DO 10 J = 1, N
        //            RJD(J) = MJD(J) + (TTTAI + TAIUTC) / 86400.0D0
        //   10   CONTINUE
        //  C
        //        DO 20 J = 1, 5
        //            RJD_INT = RJD(2) + H(J) / 86400.0D0
        //            CALL INTERP(RJD,X,Y,UT1,LOD,N,
        //       &                RJD_INT,X_INT,Y_INT,UT1_INT,LOD_INT)
        //            WRITE(6, 30) H(J),UT1_INT,LOD_INT,X_INT,Y_INT
        //   20   CONTINUE
        //   30   FORMAT(F7.1,4(1X, F20.17))
        //  C
        //        END PROGRAM
        //
        // the output of this test reads:
        //           0.0 -0.29840026968370659  0.00045312852893139 -0.12196223480123573  0.21922730818562719
        //        3600.0 -0.29841834564816189  0.00041710864863793 -0.12213345007640604  0.21927433626001305
        //        7200.0 -0.29843503870494986  0.00039207574457087 -0.12222881007999241  0.21932415788122142
        //       43200.0 -0.29866930257052676  0.00042895046506082 -0.12247697694276605  0.22105450666130921
        //       86400.0 -0.29874235341010519  0.00035460263868306 -0.12312252389660779  0.22161364352515728

        Utils.setLoaders(IERSConventions.IERS_2003,
                         Utils.buildEOPList(IERSConventions.IERS_2003, new double[][] {
                             {  52653,  -0.2979055,   0.0005744,  -0.120344,   0.217095, 0.0, 0.0, 0.0, 0.0 },
                             {  52654,  -0.2984238,   0.0004224,  -0.121680,   0.219400, 0.0, 0.0, 0.0, 0.0 },
                             {  52655,  -0.2987682,   0.0002878,  -0.122915,   0.221760, 0.0, 0.0, 0.0, 0.0 },
                             {  52656,  -0.2989957,   0.0001778,  -0.124248,   0.224294, 0.0, 0.0, 0.0, 0.0 }
                         }));
        EOPHistory eopHistory = FramesFactory.getEOPHistory(IERSConventions.IERS_2003, false);

        final AbsoluteDate t0 = new AbsoluteDate(new DateComponents(DateComponents.MODIFIED_JULIAN_EPOCH, 52654),
                                           TimeScalesFactory.getUTC());

        for (double[] row : new double[][] {
            {     0.0, -0.29840026968370659,  0.00045312852893139, -0.12196223480123573,  0.21922730818562719 },
            {  3600.0, -0.29841834564816189,  0.00041710864863793, -0.12213345007640604,  0.21927433626001305 },
            {  7200.0, -0.29843503870494986,  0.00039207574457087, -0.12222881007999241,  0.21932415788122142 },
            { 43200.0, -0.29866930257052676,  0.00042895046506082, -0.12247697694276605,  0.22105450666130921 },
            { 86400.0, -0.29874235341010519,  0.00035460263868306, -0.12312252389660779,  0.22161364352515728 }
        }) {
            AbsoluteDate date = t0.shiftedBy(row[0]);
            Assert.assertEquals(row[1], eopHistory.getUT1MinusUTC(date), 4.3e-8);
            Assert.assertEquals(row[2], eopHistory.getLOD(date), 1.4e-7);
            Assert.assertEquals(row[3] * Constants.ARC_SECONDS_TO_RADIANS,
                                eopHistory.getPoleCorrection(date).getXp(),
                                1.6e-10);
            Assert.assertEquals(row[4] * Constants.ARC_SECONDS_TO_RADIANS,
                                eopHistory.getPoleCorrection(date).getYp(),
                                0.7e-10);
        }

    }

    @Test
    public void testTidalCorrection2010() throws OrekitException {

        // the reference value has been computed using the September 2007 version
        // of interp.f from ftp://hpiers.obspm.fr/iers/models/ adding input/output
        // parameters for LOD (which was already computed in the underlying routines),
        // with the following driver program:
        //
        //        PROGRAM OR_TEST
        //        IMPLICIT NONE
        //        INTEGER J, N
        //        PARAMETER (N = 4)
        //        INTEGER MJD(N)
        //        DOUBLE PRECISION TTTAI, TAIUTC
        //        PARAMETER (TAIUTC = 32.000D0)
        //        PARAMETER (TTTAI  = 32.184D0)
        //        DOUBLE PRECISION RJD(N), UT1(N), LOD(N), X(N), Y(N), H(5)
        //        DOUBLE PRECISION RJD_INT, UT1_INT, LOD_INT, X_INT, Y_INT
        //        DATA(MJD(J),   UT1(J),     LOD(J),      X(J),      Y(J),
        //       &     J=1,4)/
        //       &    52653, -0.2979055D0, 0.0005744D0, -0.120344D0, 0.217095D0,
        //       &    52654, -0.2984238D0, 0.0004224D0, -0.121680D0, 0.219400D0,
        //       &    52655, -0.2987682D0, 0.0002878D0, -0.122915D0, 0.221760D0,
        //       &    52656, -0.2989957D0, 0.0001778D0, -0.124248D0, 0.224294D0/
        //  C
        //        DATA(H(J),J=1,5)/0.0D0, 3600.0D0, 7200.0D0, 43200.0D0, 86400.0D0/
        //  C
        //        DO 10 J = 1, N
        //            RJD(J) = MJD(J) + (TTTAI + TAIUTC) / 86400.0D0
        //   10   CONTINUE
        //  C
        //        DO 20 J = 1, 5
        //            RJD_INT = RJD(2) + H(J) / 86400.0D0
        //            CALL INTERP(RJD,X,Y,UT1,LOD,N,
        //       &                RJD_INT,X_INT,Y_INT,UT1_INT,LOD_INT)
        //            WRITE(6, 30) H(J),UT1_INT,LOD_INT,X_INT,Y_INT
        //   20   CONTINUE
        //   30   FORMAT(F7.1,4(1X, F20.17))
        //  C
        //        END PROGRAM
        //
        // the output of this test reads:
        //           0.0 -0.29840026968370659  0.00045312852893139 -0.12196223480123573  0.21922730818562719
        //        3600.0 -0.29841834564816189  0.00041710864863793 -0.12213345007640604  0.21927433626001305
        //        7200.0 -0.29843503870494986  0.00039207574457087 -0.12222881007999241  0.21932415788122142
        //       43200.0 -0.29866930257052676  0.00042895046506082 -0.12247697694276605  0.22105450666130921
        //       86400.0 -0.29874235341010519  0.00035460263868306 -0.12312252389660779  0.22161364352515728

        Utils.setLoaders(IERSConventions.IERS_2010,
                         Utils.buildEOPList(IERSConventions.IERS_2010, new double[][] {
                             {  52653,  -0.2979055,   0.0005744,  -0.120344,   0.217095, 0.0, 0.0, 0.0, 0.0 },
                             {  52654,  -0.2984238,   0.0004224,  -0.121680,   0.219400, 0.0, 0.0, 0.0, 0.0 },
                             {  52655,  -0.2987682,   0.0002878,  -0.122915,   0.221760, 0.0, 0.0, 0.0, 0.0 },
                             {  52656,  -0.2989957,   0.0001778,  -0.124248,   0.224294, 0.0, 0.0, 0.0, 0.0 }
                         }));
        EOPHistory eopHistory = FramesFactory.getEOPHistory(IERSConventions.IERS_2010, false);

        final AbsoluteDate t0 = new AbsoluteDate(new DateComponents(DateComponents.MODIFIED_JULIAN_EPOCH, 52654),
                                           TimeScalesFactory.getUTC());

        for (double[] row : new double[][] {
            {     0.0, -0.29840026968370659,  0.00045312852893139, -0.12196223480123573,  0.21922730818562719 },
            {  3600.0, -0.29841834564816189,  0.00041710864863793, -0.12213345007640604,  0.21927433626001305 },
            {  7200.0, -0.29843503870494986,  0.00039207574457087, -0.12222881007999241,  0.21932415788122142 },
            { 43200.0, -0.29866930257052676,  0.00042895046506082, -0.12247697694276605,  0.22105450666130921 },
            { 86400.0, -0.29874235341010519,  0.00035460263868306, -0.12312252389660779,  0.22161364352515728 }
        }) {
            AbsoluteDate date = t0.shiftedBy(row[0]);
            Assert.assertEquals(row[1], eopHistory.getUT1MinusUTC(date), 2.5e-12);
            Assert.assertEquals(row[2], eopHistory.getLOD(date), 4.3e-11);
            Assert.assertEquals(row[3] * Constants.ARC_SECONDS_TO_RADIANS,
                                eopHistory.getPoleCorrection(date).getXp(),
                                1.6e-10);
            Assert.assertEquals(row[4] * Constants.ARC_SECONDS_TO_RADIANS,
                                eopHistory.getPoleCorrection(date).getYp(),
                                0.7e-10);
        }

    }

    private void checkDerivative(final TimeFunction<DerivativeStructure> function,
                                 final AbsoluteDate date, final double span, final double sampleStep,
                                 final double h, final double tolerance) {

        UnivariateDifferentiableFunction differentiated =
                new FiniteDifferencesDifferentiator(4, h).differentiate(new UnivariateFunction() {
                    @Override
                    public double value(final double dt) {
                        return function.value(date.shiftedBy(dt)).getValue();
                    }
                });

        for (double dt = 0; dt < span; dt += sampleStep) {
            DerivativeStructure yRef = differentiated.value(new DerivativeStructure(1, 1, 0, dt));
            DerivativeStructure y    = function.value(date.shiftedBy(dt));
            Assert.assertEquals(yRef.getPartialDerivative(1), y.getPartialDerivative(1), tolerance);
        }

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("compressed-data");
    }

}
