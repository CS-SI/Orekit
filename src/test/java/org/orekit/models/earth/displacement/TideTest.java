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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.data.BodiesElements;
import org.orekit.data.FundamentalNutationArguments;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalarFunction;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class TideTest {

    @Test
    public void testDoodsonMultipliers() {
        Assertions.assertArrayEquals(new int[] { 2,  0,  0,  0,  0,  0 }, Tide.M2.getDoodsonMultipliers());
        Assertions.assertArrayEquals(new int[] { 2,  2, -2,  0,  0,  0 }, Tide.S2.getDoodsonMultipliers());
        Assertions.assertArrayEquals(new int[] { 2, -1,  0,  1,  0,  0 }, Tide.N2.getDoodsonMultipliers());
        Assertions.assertArrayEquals(new int[] { 2,  2,  0,  0,  0,  0 }, Tide.K2.getDoodsonMultipliers());
        Assertions.assertArrayEquals(new int[] { 1,  1,  0,  0,  0,  0 }, Tide.K1.getDoodsonMultipliers());
        Assertions.assertArrayEquals(new int[] { 1, -1,  0,  0,  0,  0 }, Tide.O1.getDoodsonMultipliers());
        Assertions.assertArrayEquals(new int[] { 1,  1, -2,  0,  0,  0 }, Tide.P1.getDoodsonMultipliers());
        Assertions.assertArrayEquals(new int[] { 1, -2,  0,  1,  0,  0 }, Tide.Q1.getDoodsonMultipliers());
        Assertions.assertArrayEquals(new int[] { 0,  2,  0,  0,  0,  0 }, Tide.MF.getDoodsonMultipliers());
        Assertions.assertArrayEquals(new int[] { 0,  1,  0, -1,  0,  0 }, Tide.MM.getDoodsonMultipliers());
        Assertions.assertArrayEquals(new int[] { 0,  0,  2,  0,  0,  0 }, Tide.SSA.getDoodsonMultipliers());
    }

    @Test
    public void testDelaunayMultipliers() {
        Assertions.assertArrayEquals(new int[] {  0,  0,  2,  0,  2 }, Tide.M2.getDelaunayMultipliers());
        Assertions.assertArrayEquals(new int[] {  0,  0,  2, -2,  2 }, Tide.S2.getDelaunayMultipliers());
        Assertions.assertArrayEquals(new int[] {  1,  0,  2,  0,  2 }, Tide.N2.getDelaunayMultipliers());
        Assertions.assertArrayEquals(new int[] {  0,  0,  0,  0,  0 }, Tide.K2.getDelaunayMultipliers());
        Assertions.assertArrayEquals(new int[] {  0,  0,  0,  0,  0 }, Tide.K1.getDelaunayMultipliers());
        Assertions.assertArrayEquals(new int[] {  0,  0,  2,  0,  2 }, Tide.O1.getDelaunayMultipliers());
        Assertions.assertArrayEquals(new int[] {  0,  0,  2, -2,  2 }, Tide.P1.getDelaunayMultipliers());
        Assertions.assertArrayEquals(new int[] {  1,  0,  2,  0,  2 }, Tide.Q1.getDelaunayMultipliers());
        Assertions.assertArrayEquals(new int[] {  0,  0, -2,  0, -2 }, Tide.MF.getDelaunayMultipliers());
        Assertions.assertArrayEquals(new int[] { -1,  0,  0,  0,  0 }, Tide.MM.getDelaunayMultipliers());
        Assertions.assertArrayEquals(new int[] {  0,  0, -2,  2, -2 }, Tide.SSA.getDelaunayMultipliers());
    }

    @Test
    public void testTauMultiplier() {
        Assertions.assertEquals(2, Tide.M2.getTauMultiplier());
        Assertions.assertEquals(2, Tide.S2.getTauMultiplier());
        Assertions.assertEquals(2, Tide.N2.getTauMultiplier());
        Assertions.assertEquals(2, Tide.K2.getTauMultiplier());
        Assertions.assertEquals(1, Tide.K1.getTauMultiplier());
        Assertions.assertEquals(1, Tide.O1.getTauMultiplier());
        Assertions.assertEquals(1, Tide.P1.getTauMultiplier());
        Assertions.assertEquals(1, Tide.Q1.getTauMultiplier());
        Assertions.assertEquals(0, Tide.MF.getTauMultiplier());
        Assertions.assertEquals(0, Tide.MM.getTauMultiplier());
        Assertions.assertEquals(0, Tide.SSA.getTauMultiplier());
    }

    @Test
    public void testInherited() {
        Assertions.assertEquals(new Tide(135655), Tide.Q1);
        Assertions.assertNotEquals(new Tide(163545), new Tide(163544));
        Assertions.assertNotEquals(new Tide(163545), null);
        Assertions.assertEquals(163545, new Tide(163545).hashCode());
    }

    @Test
    public void testPhase() {
        TimeScale                    ut1      = TimeScalesFactory.getUT1(IERSConventions.IERS_2010, true);
        FundamentalNutationArguments fna      = IERSConventions.IERS_2010.getNutationArguments(ut1);
        BodiesElements               elements = fna.evaluateAll(new AbsoluteDate("2003-05-17T13:41:54",
                                                                                 TimeScalesFactory.getUTC()));
        Assertions.assertEquals(-514.950946, Tide.M2.getPhase(elements),  1.0e-6);
        Assertions.assertEquals(  19.738518, Tide.S2.getPhase(elements),  1.0e-6);
        Assertions.assertEquals(-798.252248, Tide.N2.getPhase(elements),  1.0e-6);
        Assertions.assertEquals(  59.352413, Tide.K2.getPhase(elements),  1.0e-6);
        Assertions.assertEquals(  29.676206, Tide.K1.getPhase(elements),  1.0e-6);
        Assertions.assertEquals(-544.627152, Tide.O1.getPhase(elements),  1.0e-6);
        Assertions.assertEquals(  -9.937688, Tide.P1.getPhase(elements),  1.0e-6);
        Assertions.assertEquals(-827.928455, Tide.Q1.getPhase(elements),  1.0e-6);
        Assertions.assertEquals( 574.303358, Tide.MF.getPhase(elements),  1.0e-6);
        Assertions.assertEquals( 283.301303, Tide.MM.getPhase(elements),  1.0e-6);
        Assertions.assertEquals(  39.613894, Tide.SSA.getPhase(elements), 1.0e-6);
    }

    @Test
    public void testSortedRatesSemiLongPeriod() {
        doTestSort(0, Tide.SSA, Tide.MM, Tide.MF);
    }

    @Test
    public void testSortedRatesDiurnal() {
        doTestSort(1, Tide.Q1, Tide.O1, Tide.P1, Tide.K1);
    }

    @Test
    public void testSortedRatesSemiDiurnal() {
        doTestSort(2, Tide.N2, Tide.M2, Tide.S2, Tide.K2 );
    }

    private void doTestSort(int tauMultiplier, Tide... tides) {
        // this test checks that tides sort order is date-independent for a large time range
        // (almost 180000 years long)
        // tides sort-order is based on rate, but the rates varies slightly with dates
        // (because Delaunay nutation arguments are polynomials)
        // The variations are however small and we want to make sure
        // that if rate(tideA) < rate(tideB) at time t0, this order remains
        // the same for t1 a few millenia around t0
        TimeScale                    ut1      = TimeScalesFactory.getUT1(IERSConventions.IERS_2010, true);
        FundamentalNutationArguments fna      = IERSConventions.IERS_2010.getNutationArguments(ut1);
        for (double dt = -122000; dt < 54000; dt += 100) {
            final AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(dt * Constants.JULIAN_YEAR);
            final BodiesElements el = fna.evaluateAll(date);
            for (int i = 1; i < tides.length; ++i) {
                Assertions.assertTrue(tides[i - 1].getRate(el) < tides[i].getRate(el));
            }
        }

        // check we are in the expected species
        for (final Tide tide : tides) {
            Assertions.assertEquals(tauMultiplier, tide.getTauMultiplier());
        }

    }

    @Test
    public void testTable65a() {
        doTestTable(new double[][] {
            { 12.85429,  125755, 1, -3,  0,  2,   0,  0,   2,  0,  2,  0,  2  },
            { 12.92714,  127555, 1, -3,  2,  0,   0,  0,   0,  0,  2,  2,  2  },
            { 13.39645,  135645, 1, -2,  0,  1,  -1,  0,   1,  0,  2,  0,  1  },
            { 13.39866,  135655, 1, -2,  0,  1,   0,  0,   1,  0,  2,  0,  2  },
            { 13.47151,  137455, 1, -2,  2, -1,   0,  0,  -1,  0,  2,  2,  2  },
            { 13.94083,  145545, 1, -1,  0,  0,  -1,  0,   0,  0,  2,  0,  1  },
            { 13.94303,  145555, 1, -1,  0,  0,   0,  0,   0,  0,  2,  0,  2  },
            { 14.02517,  147555, 1, -1,  2,  0,   0,  0,   0,  0,  0,  2,  0  },
            { 14.41456,  153655, 1,  0, -2,  1,   0,  0,   1,  0,  2, -2,  2  },
            { 14.48520,  155445, 1,  0,  0, -1,  -1,  0,  -1,  0,  2,  0,  1  },
            { 14.48741,  155455, 1,  0,  0, -1,   0,  0,  -1,  0,  2,  0,  2  },
            { 14.49669,  155655, 1,  0,  0,  1,   0,  0,   1,  0,  0,  0,  0  },
            { 14.49890,  155665, 1,  0,  0,  1,   1,  0,   1,  0,  0,  0,  1  },
            { 14.56955,  157455, 1,  0,  2, -1,   0,  0,  -1,  0,  0,  2,  0  },
            { 14.57176,  157465, 1,  0,  2, -1,   1,  0,  -1,  0,  0,  2,  1  },
            { 14.91787,  162556, 1,  1, -3,  0,   0,  1,   0,  1,  2, -2,  2  },
            { 14.95673,  163545, 1,  1, -2,  0,  -1,  0,   0,  0,  2, -2,  1  },
            { 14.95893,  163555, 1,  1, -2,  0,   0,  0,   0,  0,  2, -2,  2  },
            { 15.00000,  164554, 1,  1, -1,  0,   0, -1,   0, -1,  2, -2,  2  },
            { 15.00000,  164556, 1,  1, -1,  0,   0,  1,   0,  1,  0,  0,  0  },
            { 15.02958,  165345, 1,  1,  0, -2,  -1,  0,  -2,  0,  2,  0,  1  },
            { 15.03665,  165535, 1,  1,  0,  0,  -2,  0,   0,  0,  0,  0, -2  },
            { 15.03886,  165545, 1,  1,  0,  0,  -1,  0,   0,  0,  0,  0, -1  },
            { 15.04107,  165555, 1,  1,  0,  0,   0,  0,   0,  0,  0,  0,  0  },
            { 15.04328,  165565, 1,  1,  0,  0,   1,  0,   0,  0,  0,  0,  1  },
            { 15.04548,  165575, 1,  1,  0,  0,   2,  0,   0,  0,  0,  0,  2  },
            { 15.07749,  166455, 1,  1,  1, -1,   0,  0,  -1,  0,  0,  1,  0  },
            { 15.07993,  166544, 1,  1,  1,  0,  -1, -1,   0, -1,  0,  0, -1  },
            { 15.08214,  166554, 1,  1,  1,  0,   0, -1,   0, -1,  0,  0,  0  },
            { 15.08214,  166556, 1,  1,  1,  0,   0,  1,   0,  1, -2,  2, -2  },
            { 15.08434,  166564, 1,  1,  1,  0,   1, -1,   0, -1,  0,  0,  1  },
            { 15.11392,  167355, 1,  1,  2, -2,   0,  0,  -2,  0,  0,  2,  0  },
            { 15.11613,  167365, 1,  1,  2, -2,   1,  0,  -2,  0,  0,  2,  1  },
            { 15.12321,  167555, 1,  1,  2,  0,   0,  0,   0,  0, -2,  2, -2  },
            { 15.12542,  167565, 1,  1,  2,  0,   1,  0,   0,  0, -2,  2, -1  },
            { 15.16427,  168554, 1,  1,  3,  0,   0, -1,   0, -1, -2,  2, -2  },
            { 15.51259,  173655, 1,  2, -2,  1,   0,  0,   1,  0,  0, -2,  0  },
            { 15.51480,  173665, 1,  2, -2,  1,   1,  0,   1,  0,  0, -2,  1  },
            { 15.58323,  175445, 1,  2,  0, -1,  -1,  0,  -1,  0,  0,  0, -1  },
            { 15.58545,  175455, 1,  2,  0, -1,   0,  0,  -1,  0,  0,  0,  0  },
            { 15.58765,  175465, 1,  2,  0, -1,   1,  0,  -1,  0,  0,  0,  1  },
            { 16.05697,  183555, 1,  3, -2,  0,   0,  0,   0,  0,  0, -2,  0  },
            { 16.12989,  185355, 1,  3,  0, -2,   0,  0,  -2,  0,  0,  0,  0  },
            { 16.13911,  185555, 1,  3,  0,  0,   0,  0,   0,  0, -2,  0, -2  },
            { 16.14131,  185565, 1,  3,  0,  0,   1,  0,   0,  0, -2,  0, -1  },
            { 16.14352,  185575, 1,  3,  0,  0,   2,  0,   0,  0, -2,  0,  0  },
            { 16.68348,  195455, 1,  4,  0, -1,   0,  0,  -1,  0, -2,  0, -2  },
            { 16.68569,  195465, 1,  4,  0, -1,   1,  0,  -1,  0, -2,  0, -1  }
        });
    }

    @Test
    public void testTable65b() {
        doTestTable(new double[][] {
            {  0.00221, 55565,  0, 0,  0,  0,  1,  0,  0,  0,  0,  0,  1 },
            {  0.00441, 55575,  0, 0,  0,  0,  2,  0,  0,  0,  0,  0,  2 },
            {  0.04107, 56554,  0, 0,  1,  0,  0, -1,  0, -1,  0,  0,  0 },
            {  0.08214, 57555,  0, 0,  2,  0,  0,  0,  0,  0, -2,  2, -2 },
            {  0.08434, 57565,  0, 0,  2,  0,  1,  0,  0,  0, -2,  2, -1 },
            {  0.12320, 58554,  0, 0,  3,  0,  0, -1,  0, -1, -2,  2, -2 },
            {  0.47152, 63655,  0, 1, -2,  1,  0,  0,  1,  0,  0, -2,  0 },
            {  0.54217, 65445,  0, 1,  0, -1, -1,  0, -1,  0,  0,  0, -1 },
            {  0.54438, 65455,  0, 1,  0, -1,  0,  0, -1,  0,  0,  0,  0 },
            {  0.54658, 65465,  0, 1,  0, -1,  1,  0, -1,  0,  0,  0,  1 },
            {  0.55366, 65655,  0, 1,  0,  1,  0,  0,  1,  0, -2,  0, -2 },
            {  1.01590, 73555,  0, 2, -2,  0,  0,  0,  0,  0,  0, -2,  0 },
            {  1.08875, 75355,  0, 2,  0, -2,  0,  0, -2,  0,  0,  0,  0 },
            {  1.09804, 75555,  0, 2,  0,  0,  0,  0,  0,  0, -2,  0, -2 },
            {  1.10024, 75565,  0, 2,  0,  0,  1,  0,  0,  0, -2,  0, -1 },
            {  1.10245, 75575,  0, 2,  0,  0,  2,  0,  0,  0, -2,  0,  0 },
            {  1.56956, 83655,  0, 3, -2,  1,  0,  0,  1,  0, -2, -2, -2 },
            {  1.64241, 85455,  0, 3,  0, -1,  0,  0, -1,  0, -2,  0, -2 },
            {  1.64462, 85465,  0, 3,  0, -1,  1,  0, -1,  0, -2,  0, -1 },
            {  2.11394, 93555,  0, 4, -2,  0,  0,  0,  0,  0, -2, -2, -2 },
            {  2.18679, 95355,  0, 4,  0, -2,  0,  0, -2,  0, -2,  0, -2 }
        });
    }

    @Test
    public void testTable65c() {
        doTestTable(new double[][] {
            { 28.43973, 245655,  2, -1, 0, 1, 0,   0, 1, 0,  2, 0, 2 },
            { 28.98410, 255555,  2,  0, 0, 0, 0,   0, 0, 0,  2, 0, 2 }
        });
    }

    @Test
    public void testTDFRPH1OriginalEarthRotation() {
        // this is test 1 from the TDFRPH subroutine in IERS HARDISP program
        // using the reference routine hard coded simplified model for Earth rotation
        doTestTDFRPH(true,
                     2009, 6, 25, 0, 0, 0.0, Tide.M2,
                     1.9322736160568788, 303.343338720297,
                     5.6e-11, 1.3e-9);
    }

    @Test
    public void testTDFRPH1IERSEarthRotation() {
        // this is test 1 from the TDFRPH subroutine in IERS HARDISP program
        // but using regular IERS model for Earth rotation
        doTestTDFRPH(false,
                     2009, 6, 25, 0, 0, 0.0, Tide.M2,
                     1.9322736160568788, 303.343338720297,
                     5.6e-11, 0.014);
    }

    @Test
    public void testTDFRPH2OriginalEarthRotation() {
        doTestTDFRPH(true,
                     2009, 6, 25, 12, 1, 45.0, Tide.M2,
                     1.9322736160568872, 291.997959322689,
                     5.6e-11, 3.0e-9);
    }

    @Test
    public void testTDFRPH2IERSEarthRotation() {
        // this is test 2 from the TDFRPH subroutine in IERS HARDISP program
        // but using regular IERS model for Earth rotation
        doTestTDFRPH(false,
                     2009, 6, 25, 12, 1, 45.0, Tide.M2,
                     1.9322736160568872, 291.997959322689,
                     5.6e-11, 0.014);
    }

    @Test
    public void testTDFRPHAdditional1OriginalEarthRotation() {
        // additional tests, for tides other than M2
        // reference values were obtained running the TDFRPH subroutine
        doTestTDFRPH(true,
                     2009, 6, 25, 12, 1, 45.0, Tide.Q1,
                     0.89324405952415054, 358.73529357509688,
                     2.3e-11, 3.4e-9);
        doTestTDFRPH(true,
                     2009, 6, 25, 12, 1, 45.0, Tide.O1,
                     0.92953570674740593, 17.795253026255523,
                     1.5e-11, 2.5e-9);
        doTestTDFRPH(true,
                     2009, 6, 25, 12, 1, 45.0, Tide.SSA,
                     0.0054758186189623609, 187.53041259286692,
                     4.6e-11, 1.1e-9);
        doTestTDFRPH(true,
                     2009, 6, 25, 12, 1, 45.0, Tide.MM,
                     0.036291647223255376, 19.059959451136820,
                     7.5e-12, 9.5e-10);
    }

    @Test
    public void testTDFRPHAdditional1IERSEarthRotation() {
        // additional tests, for tides other than M2
        // reference values were obtained running the TDFRPH subroutine
        doTestTDFRPH(false,
                     2009, 6, 25, 12, 1, 45.0, Tide.Q1,
                     0.89324405952415054, 358.73529357509688,
                     2.3e-11, 0.0066);
        doTestTDFRPH(false,
                     2009, 6, 25, 12, 1, 45.0, Tide.O1,
                     0.92953570674740593, 17.795253026255523,
                     1.5e-11, 0.0066);
        doTestTDFRPH(false,
                     2009, 6, 25, 12, 1, 45.0, Tide.SSA,
                     0.0054758186189623609, 187.53041259286692,
                     4.6e-11, 1.1e-9);
        doTestTDFRPH(false,
                     2009, 6, 25, 12, 1, 45.0, Tide.MM,
                     0.036291647223255376, 19.059959451136820,
                     7.5e-12, 9.5e-10);
    }

    @Test
    public void testTDFRPHAdditional2OriginalEarthRotation() {
        // additional tests, for tides other than M2
        // reference values were obtained running the TDFRPH subroutine
        doTestTDFRPH(true,
                     2009, 6, 25, 1, 10, 45.0, Tide.Q1,
                     0.89324405952416042, 213.35982288935338,
                     2.3e-11, 5.8e-9);
        doTestTDFRPH(true,
                     2009, 6, 25, 1, 10, 45.0, Tide.O1,
                     0.92953570674739971, 226.51331675532856,
                     1.5e-11, 4.1e-9);
        doTestTDFRPH(true,
                     2009, 6, 25, 1, 10, 45.0, Tide.SSA,
                     0.0054758186189598906, 186.63922310518683,
                     4.6e-11, 9.4e-10);
        doTestTDFRPH(true,
                     2009, 6, 25, 1, 10, 45.0, Tide.MM,
                     0.036291647223239284, 13.153493865960627,
                     7.5e-12, 1.8e-9);
    }

    @Test
    public void testTDFRPHAdditional2IERSEarthRotation() {
        // additional tests, for tides other than M2
        // reference values were obtained running the TDFRPH subroutine
        doTestTDFRPH(false,
                     2009, 6, 25, 1, 10, 45.0, Tide.Q1,
                     0.89324405952416042, 213.35982288935338,
                     2.3e-11, 0.0066);
        doTestTDFRPH(false,
                     2009, 6, 25, 1, 10, 45.0, Tide.O1,
                     0.92953570674739971, 226.51331675532856,
                     1.5e-11, 0.0066);
        doTestTDFRPH(false,
                     2009, 6, 25, 1, 10, 45.0, Tide.SSA,
                     0.0054758186189598906, 186.63922310518683,
                     4.6e-11, 9.4e-10);
        doTestTDFRPH(false,
                     2009, 6, 25, 1, 10, 45.0, Tide.MM,
                     0.036291647223239284, 13.153493865960627,
                     7.5e-12, 1.8e-9);
    }

    private void doTestTDFRPH(boolean patchEarthRotation,
                              int year, int month, int day, int hour, int minute, double second,
                              Tide tide, double refRate, double refPhase,
                              double toleranceRate, double tolerancePhase)
        {
        TimeScale                    ut1      = TimeScalesFactory.getUT1(IERSConventions.IERS_2010, true);
        FundamentalNutationArguments fna      = IERSConventions.IERS_2010.getNutationArguments(ut1);
        if (patchEarthRotation) {
            patchEarthRotationModel(fna, ut1);
        }
        AbsoluteDate   date     = new AbsoluteDate(year, month, day, hour, minute, second, ut1);
        BodiesElements elements = fna.evaluateAll(date);
        Assertions.assertEquals(refRate,
                            tide.getRate(elements) * Constants.JULIAN_DAY/ (2 * FastMath.PI),
                            toleranceRate);
        Assertions.assertEquals(refPhase,
                            FastMath.toDegrees(MathUtils.normalizeAngle(tide.getPhase(elements), FastMath.PI)),
                            tolerancePhase);
    }

    /** Patch FundamentalNutationArguments to use a simplified Earth rotation angle.
     * <p>
     * The angle is simply computed such that Sun is at the anti-meridian at 00h00 UT1,
     * and Earth rotates at 15°/hour between 00h00 UT1 and the current date.
     * </p>
     * @param fna FundamentalNutationArguments to patch
     * @param ut1 UT1 time scale to use for computing date components
     */
    public static void patchEarthRotationModel(FundamentalNutationArguments fna, final TimeScale ut1) {
        try {
            final Field conventionsField = FundamentalNutationArguments.class.getDeclaredField("conventions");
            conventionsField.setAccessible(true);
            final IERSConventions conventions = (IERSConventions) conventionsField.get(fna);
            final Field fCoeffField = FundamentalNutationArguments.class.getDeclaredField("fCoefficients");
            fCoeffField.setAccessible(true);
            final double[] fCoefficients = (double[]) fCoeffField.get(fna);
            final Field dCoeffField = FundamentalNutationArguments.class.getDeclaredField("dCoefficients");
            dCoeffField.setAccessible(true);
            final double[] dCoefficients = (double[]) dCoeffField.get(fna);
            final Field oCoeffField = FundamentalNutationArguments.class.getDeclaredField("omegaCoefficients");
            oCoeffField.setAccessible(true);
            final double[] oCoefficients = (double[]) oCoeffField.get(fna);
            final Method valueMethod = FundamentalNutationArguments.class.getDeclaredMethod("value", Double.TYPE, double[].class);
            valueMethod.setAccessible(true);
            final Field gmstFunctionField = FundamentalNutationArguments.class.getDeclaredField("gmstFunction");
            gmstFunctionField.setAccessible(true);
            final TimeScalarFunction old = (TimeScalarFunction) gmstFunctionField.get(fna);
            gmstFunctionField.set(fna, new TimeScalarFunction() {

                private double sunAngle(AbsoluteDate date) {
                    try {
                       double tc = conventions.evaluateTC(date);
                        return ((Double) valueMethod.invoke(fna, tc, fCoefficients)).doubleValue() +
                               ((Double) valueMethod.invoke(fna, tc, oCoefficients)).doubleValue() -
                               ((Double) valueMethod.invoke(fna, tc, dCoefficients)).doubleValue();
                    } catch (InvocationTargetException | IllegalAccessException | IllegalArgumentException e) {
                        return Double.NaN;
                    }
                }

                @Override
                public double value(AbsoluteDate date) {
                    double dayFraction = date.getComponents(ut1).getTime().getSecondsInUTCDay() / Constants.JULIAN_DAY;
                    double v = 2 * FastMath.PI * dayFraction + sunAngle(date) + FastMath.PI;
                    // the patched value is about 24 arc seconds from the IERS value (almost independent on date)
                    double deltaArcSeconds = 3600.0 * FastMath.toDegrees(MathUtils.normalizeAngle(old.value(date) - v, 0.0));
                    Assertions.assertEquals(0.0, deltaArcSeconds, 23.7);
                    return v;
                }

                @Override
                public <T extends CalculusFieldElement<T>> T value(final FieldAbsoluteDate<T> date) {
                    return null;
                }

            });
        } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException |
                 NoSuchMethodException | SecurityException e) {
            Assertions.fail(e.getLocalizedMessage());
        }
    }

    private void doTestTable(final double[][] table) {
        TimeScale                    ut1      = TimeScalesFactory.getUT1(IERSConventions.IERS_2010, true);
        FundamentalNutationArguments fna      = IERSConventions.IERS_2010.getNutationArguments(ut1);
        BodiesElements               elements = fna.evaluateAll(AbsoluteDate.J2000_EPOCH);
        for (double[] r : table) {
            int   doodsonNumber       = (int) r[1];
            int[] dooodsonMultipliers = new int[] { (int) r[2], (int) r[3], (int) r[4], (int) r[5], (int) r[6], (int) r[7] };
            int[] delaunayMultipliers = new int[] { (int) r[8], (int) r[9], (int) r[10], (int) r[11], (int) r[12] };
            final Tide tdN = new Tide(doodsonNumber);
            final Tide tdM = new Tide((int) r[2], (int) r[3], (int) r[4], (int) r[5], (int) r[6], (int) r[7]);
            Assertions.assertEquals(r[0],                     FastMath.toDegrees(tdN.getRate(elements)) * 3600, 7.2e-5);
            Assertions.assertEquals(doodsonNumber,            tdM.getDoodsonNumber());
            Assertions.assertArrayEquals(dooodsonMultipliers, tdN.getDoodsonMultipliers());
            Assertions.assertArrayEquals(delaunayMultipliers, tdN.getDelaunayMultipliers());
            Assertions.assertArrayEquals(delaunayMultipliers, tdM.getDelaunayMultipliers());
        }
    }

    @BeforeEach
    public void setUp() throws Exception {
        Utils.setDataRoot("regular-data");
    }

}
