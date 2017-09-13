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
package org.orekit.models.earth;


import java.lang.reflect.Field;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.data.BodiesElements;
import org.orekit.data.FundamentalNutationArguments;
import org.orekit.errors.OrekitException;
import org.orekit.frames.EOPHistory;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedPVCoordinates;

public class TidalDisplacementTest {

    @Test
    public void testFrame() throws OrekitException {
        IERSConventions conventions = IERSConventions.IERS_2010;
        Frame           itrf        = FramesFactory.getITRF(conventions, false);
        EOPHistory      eopHistory  = FramesFactory.getEOPHistory(conventions, false);
        TidalDisplacement td = new TidalDisplacement(itrf, Constants.EIGEN5C_EARTH_MU,
                                                     Constants.JPL_SSD_SUN_EARTH_PLUS_MOON_MASS_RATIO,
                                                     Constants.JPL_SSD_EARTH_MOON_MASS_RATIO,
                                                     CelestialBodyFactory.getSun(),
                                                     CelestialBodyFactory.getMoon(),
                                                     eopHistory);
        Assert.assertSame(itrf, td.getEarthFrame());
    }

    @Test
    public void testLoveShida() throws OrekitException {
        for (final IERSConventions conventions : IERSConventions.values()) {
            // as of Orekit 9.0, supported conventions are
            // IERS conventions 1996, IERS conventions 2003 and IERS conventions 2010
            // and they all share the same values for anelastic Earth model
            double[] hl = conventions.getNominalTidalDisplacementLoveAndShida();
            Assert.assertEquals(12, hl.length);
            Assert.assertEquals( 0.6078, hl[ 0], 1.0e-15); // h⁽⁰⁾
            Assert.assertEquals(-0.0006, hl[ 1], 1.0e-15); // h⁽²⁾
            Assert.assertEquals( 0.292,  hl[ 2], 1.0e-15); // h₃
            Assert.assertEquals(-0.0025, hl[ 3], 1.0e-15); // hI diurnal
            Assert.assertEquals(-0.0022, hl[ 4], 1.0e-15); // hI semi-diurnal
            Assert.assertEquals( 0.0847, hl[ 5], 1.0e-15); // l⁽⁰⁾
            Assert.assertEquals( 0.0012, hl[ 6], 1.0e-15); // l⁽¹⁾ diurnal
            Assert.assertEquals( 0.0024, hl[ 7], 1.0e-15); // l⁽¹⁾ semi-diurnal
            Assert.assertEquals( 0.0002, hl[ 8], 1.0e-15); // l⁽²⁾
            Assert.assertEquals( 0.015,  hl[ 9], 1.0e-15); // l₃
            Assert.assertEquals(-0.0007, hl[10], 1.0e-15); // lI diurnal
            Assert.assertEquals(-0.0007, hl[11], 1.0e-15); // lI semi-diurnal
        }
    }

    @Test
    public void testDehantOriginalArguments() throws OrekitException {
        // this test intends to reproduce as much as possible the DEHANTTIDEINEL.F test case
        // it does so by replacing the fundamental nutation arguments used by Orekit
        // (which come from IERS conventions) by the Doodson arguments in the aforementioned program
        // The results should be really close to the reference values from the original test case
        doTestDehant(true, 1.0e-10);
    }

    @Test
    public void testDehantIERSArguments() throws OrekitException {
        // this test intends to replay the DEHANTTIDEINEL.F test case but using the
        // fundamental nutation arguments from IERS conventions.
        // The results should be very slightly different from the reference values
        doTestDehant(false, 1.0e-10);
    }

    private void doTestDehant(final boolean replaceFundamentalArguments, final double tolerance)
        throws OrekitException {

        IERSConventions conventions = IERSConventions.IERS_2010;
        Frame           itrf        = FramesFactory.getITRF(conventions, false);
        EOPHistory      eopHistory  = FramesFactory.getEOPHistory(conventions, false);
        TimeScale       ut1         = TimeScalesFactory.getUT1(conventions, false);

        // constants consistent with DEHANTTIDEINEL.F reference program
        // available at <ftp://tai.bipm.org/iers/conv2010/chapter7/dehanttideinel/>
        // and Copyright (C) 2008 IERS Conventions Center
        double massRatioSun  = 332946.0482;
        double massRatioMoon = 0.0123000371;
        double re            = 6378136.6;

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

        TidalDisplacement td = new TidalDisplacement(itrf, re,
                                                     massRatioSun * (1.0 / (1.0 + massRatioMoon)),
                                                     1.0 / massRatioMoon,
                                                     fakeSun,
                                                     fakeMoon,
                                                     eopHistory);

        if (replaceFundamentalArguments) {
            try {
                // we override the official IERS conventions 2010 arguments with fake arguments matching DEHANTTIDEINEL.F code
                String regularArguments = "/assets/org/orekit/IERS-conventions/2010/nutation-arguments.txt";
                FundamentalNutationArguments fakeArguments =
                                new FundamentalNutationArguments(conventions, ut1,
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
                Field correctionFunctionField = td.getClass().getDeclaredField("frequencyCorrection");
                correctionFunctionField.setAccessible(true);
                Object correctionFunction = correctionFunctionField.get(td);
                Field argumentsField = correctionFunction.getClass().getDeclaredField("arguments");
                argumentsField.setAccessible(true);
                argumentsField.set(correctionFunction, fakeArguments);
            } catch (SecurityException | NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
                Assert.fail(e.getLocalizedMessage());
            }

        }

        Vector3D fundamentalStationWettzell = new Vector3D(4075578.385, 931852.890, 4801570.154);
        AbsoluteDate date = new AbsoluteDate(2009, 4, 13, 0, 0, 0.0, ut1);
        Vector3D displacement = td.displacement(date, fundamentalStationWettzell);
        Assert.assertEquals(0.07700420357108125891, displacement.getX(), tolerance);
        Assert.assertEquals(0.06304056321824967613, displacement.getY(), tolerance);
        Assert.assertEquals(0.05516568152597246810, displacement.getZ(), tolerance);

    }

    @Before
    public void setUp() throws Exception {
        Utils.setDataRoot("regular-data");
    }

}
