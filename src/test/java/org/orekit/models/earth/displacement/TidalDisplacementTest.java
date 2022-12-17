/* Copyright 2011-2012 Space Applications Services
 * Licensed to CS Communication & Systèmes (CS) under one or more
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
package org.orekit.models.earth.displacement;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.data.BodiesElements;
import org.orekit.data.FundamentalNutationArguments;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class TidalDisplacementTest {

    @Test
    public void testIERSDisplacementNumbers() {
        for (final IERSConventions conventions : IERSConventions.values()) {
            // as of Orekit 9.0, supported conventions are
            // IERS conventions 1996, IERS conventions 2003 and IERS conventions 2010
            // and they all share the same values for anelastic Earth model
            double[] hl = conventions.getNominalTidalDisplacement();
            Assertions.assertEquals(13, hl.length);
            Assertions.assertEquals( 0.6078,  hl[ 0], 1.0e-15); // h⁽⁰⁾
            Assertions.assertEquals(-0.0006,  hl[ 1], 1.0e-15); // h⁽²⁾
            Assertions.assertEquals( 0.292,   hl[ 2], 1.0e-15); // h₃
            Assertions.assertEquals(-0.0025,  hl[ 3], 1.0e-15); // hI diurnal
            Assertions.assertEquals(-0.0022,  hl[ 4], 1.0e-15); // hI semi-diurnal
            Assertions.assertEquals( 0.0847,  hl[ 5], 1.0e-15); // l⁽⁰⁾
            Assertions.assertEquals( 0.0012,  hl[ 6], 1.0e-15); // l⁽¹⁾ diurnal
            Assertions.assertEquals( 0.0024,  hl[ 7], 1.0e-15); // l⁽¹⁾ semi-diurnal
            Assertions.assertEquals( 0.0002,  hl[ 8], 1.0e-15); // l⁽²⁾
            Assertions.assertEquals( 0.015,   hl[ 9], 1.0e-15); // l₃
            Assertions.assertEquals(-0.0007,  hl[10], 1.0e-15); // lI diurnal
            Assertions.assertEquals(-0.0007,  hl[11], 1.0e-15); // lI semi-diurnal
            Assertions.assertEquals(-0.31460, hl[12], 1.0e-15); // H₀ permanent deformation amplitude
        }
    }

    @Test
    public void testDehantOriginalArgumentsNoRemove() {
        // this test intends to reproduce as much as possible the DEHANTTIDEINEL.F test case
        // it does so by replacing the fundamental nutation arguments and frequency correction
        // models used by Orekit (which come from IERS conventions) by the hard-coded models
        // in the aforementioned program
        // The results should be really close to the reference values from the original test case
        doTestDehant(IERSConventions.IERS_2010, false, true,
                     0.07700420357108125891, 0.06304056321824967613, 0.05516568152597246810,
                     1.0e-14);
    }

    @Test
    public void testDehantOriginalArgumentsRemovePermanentTide() {
        // this test intends to reproduce as much as possible the DEHANTTIDEINEL.F test case
        // with step 3 activated
        // it does so by replacing the fundamental nutation arguments and frequency correction
        // models used by Orekit (which come from IERS conventions) by the hard-coded models
        // in the aforementioned program
        // The results should be really close to the reference values from the original test case
        doTestDehant(IERSConventions.IERS_2010, true, true,
                     0.085888815560994619, 0.065071968475918243, 0.10369393753580475,
                     1.0e-14);
    }

    @Test
    public void testDehantIERS1996() {
        // this test intends to replay the DEHANTTIDEINEL.F test case but using the
        // fundamental nutation arguments and frequency correction models from IERS
        // conventions.
        // The results should be very slightly different from the reference values
        doTestDehant(IERSConventions.IERS_1996, false, false,
                     0.07700420357108125891, 0.06304056321824967613, 0.05516568152597246810,
                     5.8e-4);
    }

    @Test
    public void testDehantIERS2003() {
        // this test intends to replay the DEHANTTIDEINEL.F test case but using the
        // fundamental nutation arguments and frequency correction models from IERS
        // conventions.
        // The results should be very slightly different from the reference values
        doTestDehant(IERSConventions.IERS_2003, false, false,
                     0.07700420357108125891, 0.06304056321824967613, 0.05516568152597246810,
                     2.3e-5);
    }

    @Test
    public void testDehantIERS2010() {
        // this test intends to replay the DEHANTTIDEINEL.F test case but using the
        // fundamental nutation arguments and frequency correction models from IERS
        // conventions.
        // The results should be very slightly different from the reference values
        // the differences are more important than with conventions 2003
        // because after discussion with Dr. Hana Krásná from TU Wien,
        // we have fixed a typo in the IERS 2010 table 7.3a (∆Rf(op) for
        // tide P₁ is +0.07mm but the conventions list it as -0.07mm)
        doTestDehant(IERSConventions.IERS_2010, false, false,
                     0.07700420357108125891, 0.06304056321824967613, 0.05516568152597246810,
                     1.3e-4);
    }

    private void doTestDehant(final IERSConventions conventions, final boolean removePermanentDeformation, final boolean replaceModels,
                              final double expectedDx, final double expectedDy, final double expectedDz,
                              final double tolerance)
        {

        Frame     itrf = FramesFactory.getITRF(conventions, false);
        TimeScale ut1  = TimeScalesFactory.getUT1(conventions, false);

        final double re;
        final double sunEarthSystemMassRatio ;
        final double earthMoonMassRatio;
        if (replaceModels) {
            // constants consistent with DEHANTTIDEINEL.F reference program
            // available at <ftp://tai.bipm.org/iers/conv2010/chapter7/dehanttideinel/>
            // and Copyright (C) 2008 IERS Conventions Center
            re                         = 6378136.6;
            final double massRatioSun  = 332946.0482;
            final double massRatioMoon = 0.0123000371;
            sunEarthSystemMassRatio    = massRatioSun * (1.0 / (1.0 + massRatioMoon));
            earthMoonMassRatio         = 1.0 / massRatioMoon;
        } else {
            // constants consistent with IERS and JPL
            re                      = Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS;
            sunEarthSystemMassRatio = Constants.JPL_SSD_SUN_EARTH_PLUS_MOON_MASS_RATIO;
            earthMoonMassRatio      = Constants.JPL_SSD_EARTH_MOON_MASS_RATIO;
        }

        // fake providers generating only the positions from the reference program test
        PVCoordinatesProvider fakeSun  = (date, frame) -> new TimeStampedPVCoordinates(date,
                                                                                       new Vector3D(137859926952.015,
                                                                                                    54228127881.435,
                                                                                                    23509422341.6960),
                                                                                       Vector3D.ZERO,
                                                                                       Vector3D.ZERO);
        PVCoordinatesProvider fakeMoon = (date, frame) -> new TimeStampedPVCoordinates(date,
                                                                                       new Vector3D(-179996231.920342,
                                                                                                    -312468450.131567,
                                                                                                    -169288918.592160),
                                                                                       Vector3D.ZERO,
                                                                                       Vector3D.ZERO);

        TidalDisplacement td = new TidalDisplacement(re, sunEarthSystemMassRatio, earthMoonMassRatio,
                                                     fakeSun, fakeMoon, conventions, removePermanentDeformation);

        FundamentalNutationArguments arguments = null;
        if (replaceModels) {
            try {
                // we override the official IERS conventions 2010 arguments with fake arguments matching DEHANTTIDEINEL.F code
                String regularArguments = "/assets/org/orekit/IERS-conventions/2010/nutation-arguments.txt";
                arguments = new FundamentalNutationArguments(conventions, ut1,
                                                             IERSConventions.class.getResourceAsStream(regularArguments),
                                                             regularArguments) {

                    private static final long serialVersionUID = 20170913L;

                    @Override
                    public BodiesElements evaluateAll(final AbsoluteDate date) {
                        BodiesElements base = super.evaluateAll(date);
                        double fhr = date.getComponents(ut1).getTime().getSecondsInUTCDay() / 3600.0;
                        double t = base.getTC();

                        // Doodson fundamental arguments as per DEHANTTIDEINEL.F code
                        double s   = 218.31664563 + (481267.88194 + (-0.0014663889 + (0.00000185139) * t) * t) * t;
                        double tau = fhr * 15 + 280.4606184 + (36000.7700536 + (0.00038793 + (-0.0000000258) * t) * t) * t - s;
                        double pr  = (1.396971278 + (0.000308889 + (0.000000021 + (0.000000007) * t) * t) * t) * t ;
                        double h   = 280.46645 + (36000.7697489 + (0.00030322222  + (0.000000020 + (-0.00000000654) * t) * t) * t) * t;
                        double p   = 83.35324312 + (4069.01363525 + (-0.01032172222 + (-0.0000124991 + (0.00000005263) * t) * t) * t) * t;
                        double zns = 234.95544499 + (1934.13626197 + (-0.00207561111 + (-0.00000213944 + (0.00000001650) * t) * t) * t) * t;
                        double ps  = 282.93734098 + (1.71945766667 + (0.00045688889 + (-0.00000001778 + (-0.00000000334) * t) * t) * t) * t;
                        s += pr;

                        // rebuild Delaunay arguments from Doodson arguments, ignoring derivatives
                        return new BodiesElements(date, base.getTC(),
                                                  FastMath.toRadians(s + tau), 0.0, FastMath.toRadians(s - p), 0.0, FastMath.toRadians(h - ps), 0.0,
                                                  FastMath.toRadians(s + zns), 0.0, FastMath.toRadians(s - h), 0.0, FastMath.toRadians(-zns),   0.0,
                                                  base.getLMe(),               0.0, base.getLVe(),             0.0, base.getLE(),               0.0,
                                                  base.getLMa(),               0.0, base.getLJu(),             0.0, base.getLSa(),              0.0,
                                                  base.getLUr(),               0.0, base.getLNe(),             0.0, base.getPa(),               0.0);

                    }
                };

                // we override the official IERS conventions 2010 tides displacements with tides displacements matching DEHANTTIDEINEL.F code
                String table73a = "/tides/tab7.3a-Dehant.txt";
                Field diurnalCorrectionField = td.getClass().getDeclaredField("frequencyCorrectionDiurnal");
                diurnalCorrectionField.setAccessible(true);
                Method diurnalCorrectionGetter = IERSConventions.class.
                                                 getDeclaredMethod("getTidalDisplacementFrequencyCorrectionDiurnal",
                                                                   String.class, Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE);
                diurnalCorrectionGetter.setAccessible(true);
                diurnalCorrectionField.set(td, diurnalCorrectionGetter.invoke(null, table73a, 18, 15, 16, 17, 18));

            } catch (SecurityException | NoSuchMethodException | NoSuchFieldException |
                     InvocationTargetException | IllegalArgumentException | IllegalAccessException e) {
                Assertions.fail(e.getLocalizedMessage());
            }
        } else {
            arguments = conventions.getNutationArguments(ut1);
        }


        Vector3D fundamentalStationWettzell = new Vector3D(4075578.385, 931852.890, 4801570.154);
        AbsoluteDate date = new AbsoluteDate(2009, 4, 13, 0, 0, 0.0, ut1);
        Vector3D displacement = td.displacement(arguments.evaluateAll(date), itrf, fundamentalStationWettzell);
        Assertions.assertEquals(expectedDx, displacement.getX(), tolerance);
        Assertions.assertEquals(expectedDy, displacement.getY(), tolerance);
        Assertions.assertEquals(expectedDz, displacement.getZ(), tolerance);


    }

    @BeforeEach
    public void setUp() throws Exception {
        Utils.setDataRoot("regular-data");
    }

}
