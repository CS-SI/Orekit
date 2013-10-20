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
package org.orekit.forces.gravity;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.apache.commons.math3.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.data.BodiesElements;
import org.orekit.data.FundamentalNutationArguments;
import org.orekit.data.PoissonSeries;
import org.orekit.data.PoissonSeriesParser;
import org.orekit.errors.OrekitException;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.forces.gravity.potential.TideSystem;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeFunction;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.LoveNumbers;


public class TidesFieldTest {

    @Test
    public void testConventions2003() throws OrekitException, NoSuchFieldException, IllegalAccessException {

        TimeScale ut1 = TimeScalesFactory.getUT1(IERSConventions.IERS_2010, false);
        TidesField tidesField =
                new TidesField(IERSConventions.IERS_2003.getLoveNumbers(),
                               IERSConventions.IERS_2003.getTideFrequencyDependenceFunction(ut1),
                               IERSConventions.IERS_2003.getPermanentTide(),
                               FramesFactory.getITRF(IERSConventions.IERS_2003, false),
                               Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_MU,
                               TideSystem.ZERO_TIDE, CelestialBodyFactory.getSun(), CelestialBodyFactory.getMoon());

        Field fieldReal = tidesField.getClass().getDeclaredField("love");
        fieldReal.setAccessible(true);
        LoveNumbers love = (LoveNumbers) fieldReal.get(tidesField);

        Assert.assertEquals(0.30190, love.getReal(2, 0), 1.0e-10);
        Assert.assertEquals(0.29830, love.getReal(2, 1), 1.0e-10);
        Assert.assertEquals(0.30102, love.getReal(2, 2), 1.0e-10);
        Assert.assertEquals(0.093,   love.getReal(3, 0), 1.0e-10);
        Assert.assertEquals(0.093,   love.getReal(3, 1), 1.0e-10);
        Assert.assertEquals(0.093,   love.getReal(3, 2), 1.0e-10);
        Assert.assertEquals(0.094,   love.getReal(3, 3), 1.0e-10);

        Assert.assertEquals(-0.00000, love.getImaginary(2, 0), 1.0e-10);
        Assert.assertEquals(-0.00144, love.getImaginary(2, 1), 1.0e-10);
        Assert.assertEquals(-0.00130, love.getImaginary(2, 2), 1.0e-10);
        Assert.assertEquals(0.0,      love.getImaginary(3, 0), 1.0e-10);
        Assert.assertEquals(0.0,      love.getImaginary(3, 1), 1.0e-10);
        Assert.assertEquals(0.0,      love.getImaginary(3, 2), 1.0e-10);
        Assert.assertEquals(0.0,      love.getImaginary(3, 3), 1.0e-10);

        Assert.assertEquals(-0.00089, love.getPlus(2, 0), 1.0e-10);
        Assert.assertEquals(-0.00080, love.getPlus(2, 1), 1.0e-10);
        Assert.assertEquals(-0.00057, love.getPlus(2, 2), 1.0e-10);
        Assert.assertEquals(0.0,      love.getPlus(3, 0), 1.0e-10);
        Assert.assertEquals(0.0,      love.getPlus(3, 1), 1.0e-10);
        Assert.assertEquals(0.0,      love.getPlus(3, 2), 1.0e-10);
        Assert.assertEquals(0.0,      love.getPlus(3, 3), 1.0e-10);

    }

    @Test
    public void testConventions2010() throws OrekitException, NoSuchFieldException, IllegalAccessException {

        TimeScale ut1 = TimeScalesFactory.getUT1(IERSConventions.IERS_2010, true);
        TidesField tidesField =
                new TidesField(IERSConventions.IERS_2010.getLoveNumbers(),
                               IERSConventions.IERS_2010.getTideFrequencyDependenceFunction(ut1),
                               IERSConventions.IERS_2010.getPermanentTide(),
                               FramesFactory.getITRF(IERSConventions.IERS_2010, false),
                               Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_MU,
                               TideSystem.ZERO_TIDE, CelestialBodyFactory.getSun(), CelestialBodyFactory.getMoon());

        Field fieldReal = tidesField.getClass().getDeclaredField("love");
        fieldReal.setAccessible(true);
        LoveNumbers love = (LoveNumbers) fieldReal.get(tidesField);

        Assert.assertEquals(-0.00000, love.getImaginary(2, 0), 1.0e-10);
        Assert.assertEquals(-0.00144, love.getImaginary(2, 1), 1.0e-10);
        Assert.assertEquals(-0.00130, love.getImaginary(2, 2), 1.0e-10);
        Assert.assertEquals(0.0,      love.getImaginary(3, 0), 1.0e-10);
        Assert.assertEquals(0.0,      love.getImaginary(3, 1), 1.0e-10);
        Assert.assertEquals(0.0,      love.getImaginary(3, 2), 1.0e-10);
        Assert.assertEquals(0.0,      love.getImaginary(3, 3), 1.0e-10);


        Assert.assertEquals(-0.00089, love.getPlus(2, 0), 1.0e-10);
        Assert.assertEquals(-0.00080, love.getPlus(2, 1), 1.0e-10);
        Assert.assertEquals(-0.00057, love.getPlus(2, 2), 1.0e-10);
        Assert.assertEquals(0.0,      love.getPlus(3, 0), 1.0e-10);
        Assert.assertEquals(0.0,      love.getPlus(3, 1), 1.0e-10);
        Assert.assertEquals(0.0,      love.getPlus(3, 2), 1.0e-10);
        Assert.assertEquals(0.0,      love.getPlus(3, 3), 1.0e-10);

    }

    @Test
    public void testK1Example()
        throws OrekitException, NoSuchFieldException, IllegalAccessException,
               NoSuchMethodException, InvocationTargetException {
        // the reference for this test is the example at the bottom of page 86, IERS conventions 2010 section 6.2.1
        final PoissonSeriesParser<DerivativeStructure> k21Parser =
                new PoissonSeriesParser<DerivativeStructure>(18).
                    withOptionalColumn(1).
                    withDoodson(4, 3).
                    withFirstDelaunay(10);
        final String name = "/tides/tab6.5a-only-K1.txt";
        final double pico = 1.0e-12;
        final PoissonSeries<DerivativeStructure> c21Series =
                k21Parser.
                withSinCos(0, 17, pico, 18, pico).
                parse(getClass().getResourceAsStream(name), name);
        final PoissonSeries<DerivativeStructure> s21Series =
                k21Parser.
                withSinCos(0, 18, -pico, 17, pico).
                parse(getClass().getResourceAsStream(name), name);
        final TimeScale ut1 = TimeScalesFactory.getUT1(IERSConventions.IERS_2010, false);
        final TimeFunction<DerivativeStructure> gmstFunction = IERSConventions.IERS_2010.getGMSTFunction(ut1);
        Method getNA = IERSConventions.class.getDeclaredMethod("getNutationArguments", TimeFunction.class);
        getNA.setAccessible(true);
        final FundamentalNutationArguments arguments =
                (FundamentalNutationArguments) getNA.invoke(IERSConventions.IERS_2010, gmstFunction);
        TimeFunction<double[]> deltaCSFunction = new TimeFunction<double[]>() {
            public double[] value(final AbsoluteDate date) {
                final BodiesElements elements = arguments.evaluateAll(date);
                return new double[] {
                    0.0, c21Series.value(elements), s21Series.value(elements), 0.0, 0.0
                };
            }
        };

        TidesField tf = new TidesField(IERSConventions.IERS_2010.getLoveNumbers(),
                                       deltaCSFunction,
                                       IERSConventions.IERS_2010.getPermanentTide(),
                                       FramesFactory.getITRF(IERSConventions.IERS_2010, false),
                                       Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS,
                                       Constants.EIGEN5C_EARTH_MU,
                                       TideSystem.ZERO_TIDE,
                                       CelestialBodyFactory.getSun(),
                                       CelestialBodyFactory.getMoon());
        Method frequencyDependentPart = TidesField.class.getDeclaredMethod("frequencyDependentPart", AbsoluteDate.class);
        frequencyDependentPart.setAccessible(true);
        Field cachedCNMField = TidesField.class.getDeclaredField("cachedCnm");
        cachedCNMField.setAccessible(true);
        double[][] cachedCNM = (double[][]) cachedCNMField.get(tf);
        Field cachedSNMField = TidesField.class.getDeclaredField("cachedSnm");
        cachedSNMField.setAccessible(true);
        double[][] cachedSNM = (double[][]) cachedSNMField.get(tf);

        AbsoluteDate t0 = new AbsoluteDate(2003, 5, 6, 13, 43, 32.125, TimeScalesFactory.getUTC());
        for (double dt = 0; dt < Constants.JULIAN_DAY; dt += 300) {
            AbsoluteDate date = t0.shiftedBy(dt);
            for (int i = 0; i < cachedCNM.length; ++i) {
                Arrays.fill(cachedCNM[i], 0.0);
                Arrays.fill(cachedSNM[i], 0.0);
            }
            frequencyDependentPart.invoke(tf, date);
            double thetaPlusPi = gmstFunction.value(date).getValue() + FastMath.PI;
            Assert.assertEquals(470.9e-12 * FastMath.sin(thetaPlusPi) - 30.2e-12 * FastMath.cos(thetaPlusPi),
                                cachedCNM[2][1], 2.0e-25);
            Assert.assertEquals(470.9e-12 * FastMath.cos(thetaPlusPi) + 30.2e-12 * FastMath.sin(thetaPlusPi),
                                cachedSNM[2][1], 2.0e-25);
        }

    }

    @Test
    public void testDeltaCnmSnm() throws OrekitException {
        Utils.setDataRoot("regular-data:potential/icgem-format");
        NormalizedSphericalHarmonicsProvider gravityField =
                GravityFieldFactory.getConstantNormalizedProvider(8, 8);
        TimeScale ut1 = TimeScalesFactory.getUT1(IERSConventions.IERS_2010, true);
        TimeScale utc = TimeScalesFactory.getUTC();

        AbsoluteDate date = new AbsoluteDate(2003, 5, 6, 13, 43, 32.125, utc);
        TidesField tidesField =
                new TidesField(IERSConventions.IERS_2010.getLoveNumbers(),
                               IERSConventions.IERS_2010.getTideFrequencyDependenceFunction(ut1),
                               IERSConventions.IERS_2010.getPermanentTide(),
                               FramesFactory.getITRF(IERSConventions.IERS_2010, true),
                               gravityField.getAe(), gravityField.getMu(), TideSystem.TIDE_FREE,
                               CelestialBodyFactory.getSun(), CelestialBodyFactory.getMoon());
        double dateOffset = tidesField.getOffset(date);
        double[][] refDeltaCnm = new double[][] {
            {           0.0,                     0.0,                    0.0,                   0.0,                  0.0  },
            {           0.0,                     0.0,                    0.0,                   0.0,                  0.0  },
            { -2.6732289327355114E-9,   4.9078992447259636E-9,   3.5894110538262888E-9,         0.0 ,                 0.0  },
// should the previous line be as follows?
//            { -2.6598001259297745E-9,   4.9078992447259636E-9,   3.5894110538262888E-9,         0.0 ,                 0.0  },
            { -1.2906396038844278E-11, -9.287425761494864E-14,   8.356574031169308E-12, -2.2644465239439227E-12,      0.0  },
            {  7.88813885692598E-12,   -1.4422209452671492E-11, -6.815519349197347E-12,         0.0,                  0.0  }
        };
        double[][] refDeltaSnm = new double[][] {
            {           0.0,                     0.0,                    0.0,                   0.0,                  0.0 },
            {           0.0,                     0.0,                    0.0,                   0.0,                  0.0 },
            {           0.0,            1.5999274492985954E-9,   2.1815888173803134E-9,         0.0 ,                 0.0 },
            {           0.0,           -4.612996118452923E-14,   1.80975277221385E-11,   1.6338892247419556E-11,      0.0 },
            {           0.0,           -4.8972289760103175E-12, -4.103404269740309E-12,         0.0,                  0.0 }
        };
        for (int n = 0; n < refDeltaCnm.length; ++n) {
            double threshold = (n == 2) ? 1.3e-17 : 1.0e-24;
            for (int m = 0; m <= n; ++m) {
                Assert.assertEquals(refDeltaCnm[n][m], tidesField.getNormalizedCnm(dateOffset, n, m), threshold);
                Assert.assertEquals(refDeltaSnm[n][m], tidesField.getNormalizedSnm(dateOffset, n, m), threshold);
            }
        }
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
