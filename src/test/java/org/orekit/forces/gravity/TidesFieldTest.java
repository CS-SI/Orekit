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

import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.data.PoissonSeries;
import org.orekit.errors.OrekitException;
import org.orekit.forces.gravity.potential.TideSystem;
import org.orekit.frames.FramesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.LoveNumbers;


public class TidesFieldTest {

    @Test
    public void testConventions2003() throws OrekitException, NoSuchFieldException, IllegalAccessException {

        TidesField tidesField =
                new TidesField(IERSConventions.IERS_2003.getLoveNumbers(),
                               IERSConventions.IERS_2003.getTideFrequencyDependenceModel(),
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

        Field fieldKSeries = tidesField.getClass().getDeclaredField("kSeries");
        fieldKSeries.setAccessible(true);
        @SuppressWarnings("unchecked")
        PoissonSeries<DerivativeStructure>[] kSeries =
                (PoissonSeries<DerivativeStructure>[]) fieldKSeries.get(tidesField);
        Assert.assertEquals(21, kSeries[0].getNonPolynomialSize());
        Assert.assertEquals(48, kSeries[1].getNonPolynomialSize());
        Assert.assertEquals( 2, kSeries[2].getNonPolynomialSize());

    }

    @Test
    public void testConventions2010() throws OrekitException, NoSuchFieldException, IllegalAccessException {

        TidesField tidesField =
                new TidesField(IERSConventions.IERS_2010.getLoveNumbers(),
                               IERSConventions.IERS_2010.getTideFrequencyDependenceModel(),
                               FramesFactory.getITRF(IERSConventions.IERS_2003, false),
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

        Field fieldKSeries = tidesField.getClass().getDeclaredField("kSeries");
        fieldKSeries.setAccessible(true);
        @SuppressWarnings("unchecked")
        PoissonSeries<DerivativeStructure>[] kSeries =
                (PoissonSeries<DerivativeStructure>[]) fieldKSeries.get(tidesField);
        Assert.assertEquals(21, kSeries[0].getNonPolynomialSize());
        Assert.assertEquals(48, kSeries[1].getNonPolynomialSize());
        Assert.assertEquals( 2, kSeries[2].getNonPolynomialSize());

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
