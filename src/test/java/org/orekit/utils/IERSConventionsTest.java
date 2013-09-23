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


import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.apache.commons.math3.analysis.differentiation.FiniteDifferencesDifferentiator;
import org.apache.commons.math3.analysis.differentiation.UnivariateDifferentiableFunction;
import org.apache.commons.math3.util.MathUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeFunction;
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
        checkDerivative(IERSConventions.IERS_1996.getGMSTFunction(TimeScalesFactory.getUT1(IERSConventions.IERS_1996)),
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

        final TimeFunction<DerivativeStructure> gmst82 =
                IERSConventions.IERS_1996.getGMSTFunction(TimeScalesFactory.getUT1(IERSConventions.IERS_1996));
        AbsoluteDate date = new AbsoluteDate(2004, 2, 14, TimeScalesFactory.getUTC());
        double gmst = MathUtils.normalizeAngle(gmst82.value(date).getValue(), 0.0);
        Assert.assertEquals(2.5021977627453466653, gmst, 1.0e-12);
        date = new AbsoluteDate(2004, 2, 29, TimeScalesFactory.getUTC());
        gmst = MathUtils.normalizeAngle(gmst82.value(date).getValue(), 0.0);
        Assert.assertEquals(2.7602390405411441066, gmst, 1.0e-12);

    }

    @Test
    public void testGMST00Derivative() throws OrekitException {
        checkDerivative(IERSConventions.IERS_2003.getGMSTFunction(TimeScalesFactory.getUT1(IERSConventions.IERS_2003)),
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

        final TimeFunction<DerivativeStructure> gmst00 =
                IERSConventions.IERS_2003.getGMSTFunction(TimeScalesFactory.getUT1(IERSConventions.IERS_2003));
        AbsoluteDate date = new AbsoluteDate(2004, 2, 14, TimeScalesFactory.getUTC());
        double gmst = MathUtils.normalizeAngle(gmst00.value(date).getValue(), 0.0);
        Assert.assertEquals(2.5021977786243789765, gmst, 1.0e-15);
        date = new AbsoluteDate(2004, 2, 29, TimeScalesFactory.getUTC());
        gmst = MathUtils.normalizeAngle(gmst00.value(date).getValue(), 0.0);
        Assert.assertEquals(2.7602390558728311376, gmst, 1.0e-15);

    }

    @Test
    public void testGMST06Derivative() throws OrekitException {
        checkDerivative(IERSConventions.IERS_2010.getGMSTFunction(TimeScalesFactory.getUT1(IERSConventions.IERS_2010)),
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

        final TimeFunction<DerivativeStructure> gmst06 =
                IERSConventions.IERS_2010.getGMSTFunction(TimeScalesFactory.getUT1(IERSConventions.IERS_2010));
        AbsoluteDate date = new AbsoluteDate(2004, 2, 14, TimeScalesFactory.getUTC());
        double gmst = MathUtils.normalizeAngle(gmst06.value(date).getValue(), 0.0);
        Assert.assertEquals(2.5021977784096232078, gmst, 1.0e-15);
        date = new AbsoluteDate(2004, 2, 29, TimeScalesFactory.getUTC());
        gmst = MathUtils.normalizeAngle(gmst06.value(date).getValue(), 0.0);
        Assert.assertEquals(2.7602390556555129741, gmst, 3.0e-15);

    }

    @Test
    public void testERADerivative() throws OrekitException {
        checkDerivative(IERSConventions.IERS_2010.getGMSTFunction(TimeScalesFactory.getUT1(IERSConventions.IERS_2010)),
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

        final TimeFunction<DerivativeStructure> era00 =
                IERSConventions.IERS_2003.getEarthOrientationAngleFunction(TimeScalesFactory.getUT1(IERSConventions.IERS_2003));
        AbsoluteDate date = new AbsoluteDate(2004, 2, 14, TimeScalesFactory.getUTC());
        double era  = MathUtils.normalizeAngle(era00.value(date).getValue(), 0.0);
        Assert.assertEquals(2.5012766511308228701, era, 1.0e-12);
        date = new AbsoluteDate(2004, 2, 29, TimeScalesFactory.getUTC());
        era  = MathUtils.normalizeAngle(era00.value(date).getValue(), 0.0);
        Assert.assertEquals(2.7593087452455264952, era, 1.0e-12);

    }

    @Test
    public void testGST94Derivative() throws OrekitException {
        checkDerivative(IERSConventions.IERS_1996.getGASTFunction(TimeScalesFactory.getUT1(IERSConventions.IERS_1996)),
                        AbsoluteDate.J2000_EPOCH.shiftedBy(-0.4 * Constants.JULIAN_DAY),
                        0.8 * Constants.JULIAN_DAY, 1800.0, 10.0, 2.0e-12);
    }

    @Test
    public void testGST94Sofa() throws OrekitException {

        // the reference value has been computed using the March 2012 version of the SOFA library
        // http://www.iausofa.org/2012_0301_C.html, with the following code
        //
        //        double utc1, utc2, ut11, ut12, gst;
        //
        //        // 2004-02-14:00:00:00Z, MJD = 53049, UT1-UTC = -0.4093509
        //        utc1  = DJM0 + 53049.0;
        //        utc2  = 0.0;
        //        iauUtcut1(utc1, utc2, -0.4093509, &ut11, &ut12);
        //        gst = iauGst94(ut11, ut12);
        //        printf("iaugst94(%.20g, %.20g)\n  --> %.20g\n", ut11, ut12, gst);
        //
        //        // 2004-02-29:00:00:00Z, MJD = 53064, UT1-UTC = -0.4175723
        //        utc1 = DJM0 + 53064.0;
        //        utc2 = 0.0;
        //        iauUtcut1(utc1, utc2, -0.4175723, &ut11, &ut12);
        //        gst = iauGst94(ut11, ut12);
        //        printf("iaugst94(%.20g, %.20g)\n  --> %.20g\n", ut11, ut12, gst);
        //
        // the output of this test reads:
        //      iaugst94(2453049.5, -4.7378576388888813016e-06)
        //        --> 2.5021490909193064844
        //      iaugst94(2453064.5, -4.8330127314815448519e-06)
        //        --> 2.7601901473152641309
        final TimeFunction<DerivativeStructure> gst94 =
                IERSConventions.IERS_1996.getGASTFunction(TimeScalesFactory.getUT1(IERSConventions.IERS_1996));
        AbsoluteDate date = new AbsoluteDate(2004, 2, 14, TimeScalesFactory.getUTC());
        double gst = MathUtils.normalizeAngle(gst94.value(date).getValue(), 0.0);
        Assert.assertEquals(2.5021490909193064844, gst, 2.0e-10);
        date = new AbsoluteDate(2004, 2, 29, TimeScalesFactory.getUTC());
        gst = MathUtils.normalizeAngle(gst94.value(date).getValue(), 0.0);
        Assert.assertEquals(2.7601901473152641309, gst, 9.0e-11);

    }

    @Test
    public void testGST00ADerivative() throws OrekitException {
        checkDerivative(IERSConventions.IERS_2003.getGASTFunction(TimeScalesFactory.getUT1(IERSConventions.IERS_2003)),
                        AbsoluteDate.J2000_EPOCH.shiftedBy(-0.4 * Constants.JULIAN_DAY),
                        0.8 * Constants.JULIAN_DAY, 1800.0, 10.0, 2.0e-12);
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

        final TimeFunction<DerivativeStructure> gst00a =
                IERSConventions.IERS_2003.getGASTFunction(TimeScalesFactory.getUT1(IERSConventions.IERS_2003));
        AbsoluteDate date = new AbsoluteDate(2004, 2, 14, TimeScalesFactory.getUTC());
        double gst = MathUtils.normalizeAngle(gst00a.value(date).getValue(), 0.0);
        Assert.assertEquals(2.5021491024360624778, gst, 5.0e-11);
        date = new AbsoluteDate(2004, 2, 29, TimeScalesFactory.getUTC());
        gst = MathUtils.normalizeAngle(gst00a.value(date).getValue(), 0.0);
        Assert.assertEquals(2.7601901615614221619, gst, 5.0e-11);

    }

    @Test
    public void testGST06Derivative() throws OrekitException {
        checkDerivative(IERSConventions.IERS_2010.getGASTFunction(TimeScalesFactory.getUT1(IERSConventions.IERS_2010)),
                        AbsoluteDate.J2000_EPOCH.shiftedBy(-0.4 * Constants.JULIAN_DAY),
                        0.8 * Constants.JULIAN_DAY, 1800.0, 10.0, 2.0e-12);
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

        final TimeFunction<DerivativeStructure> gst06 =
                IERSConventions.IERS_2010.getGASTFunction(TimeScalesFactory.getUT1(IERSConventions.IERS_2010));
        AbsoluteDate date = new AbsoluteDate(2004, 2, 14, TimeScalesFactory.getUTC());
        double gst = MathUtils.normalizeAngle(gst06.value(date).getValue(), 0.0);
        Assert.assertEquals(2.5021491022006503435, gst, 5.0e-11);
        date = new AbsoluteDate(2004, 2, 29, TimeScalesFactory.getUTC());
        gst = MathUtils.normalizeAngle(gst06.value(date).getValue(), 0.0);
        Assert.assertEquals(2.7601901613234058885, gst, 4.0e-11);

    }

    @Test
    public void testGMST2000vs82() throws OrekitException {

        final TimeFunction<DerivativeStructure> gmst82 =
                IERSConventions.IERS_1996.getGMSTFunction(TimeScalesFactory.getUT1(IERSConventions.IERS_1996));
        final TimeFunction<DerivativeStructure> gmst00 =
                IERSConventions.IERS_2003.getGMSTFunction(TimeScalesFactory.getUT1(IERSConventions.IERS_2003));
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

        final UT1Scale ut1 = TimeScalesFactory.getUT1(IERSConventions.IERS_2010);
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
