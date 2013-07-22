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
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.gravity.potential.TideSystem;
import org.orekit.frames.FramesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;


public class TidesFieldTest {

    @Test
    public void testConventions2003() throws OrekitException, NoSuchFieldException, IllegalAccessException {

        TidesField tidesField =
                new TidesField(IERSConventions.IERS_2003.getLoveNumbersModel(),
                               IERSConventions.IERS_2003.getK20FrequencyDependenceModel(),
                               IERSConventions.IERS_2003.getK21FrequencyDependenceModel(),
                               IERSConventions.IERS_2003.getK22FrequencyDependenceModel(),
                               FramesFactory.getITRF2008(),
                               Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_MU,
                               TideSystem.ZERO_TIDE, CelestialBodyFactory.getSun(), CelestialBodyFactory.getMoon());

        Field fieldReal = tidesField.getClass().getDeclaredField("loveReal");
        fieldReal.setAccessible(true);
        double[][] loveReal = (double[][]) fieldReal.get(tidesField);
        Assert.assertEquals(0.30190, loveReal[2][0], 1.0e-10);
        Assert.assertEquals(0.29830, loveReal[2][1], 1.0e-10);
        Assert.assertEquals(0.30102, loveReal[2][2], 1.0e-10);
        Assert.assertEquals(0.093,   loveReal[3][0], 1.0e-10);
        Assert.assertEquals(0.093,   loveReal[3][1], 1.0e-10);
        Assert.assertEquals(0.093,   loveReal[3][2], 1.0e-10);
        Assert.assertEquals(0.094,   loveReal[3][3], 1.0e-10);

        Field fieldImaginary = tidesField.getClass().getDeclaredField("loveImaginary");
        fieldImaginary.setAccessible(true);
        double[][] loveImaginary = (double[][]) fieldImaginary.get(tidesField);
        Assert.assertEquals(-0.00000, loveImaginary[2][0], 1.0e-10);
        Assert.assertEquals(-0.00144, loveImaginary[2][1], 1.0e-10);
        Assert.assertEquals(-0.00130, loveImaginary[2][2], 1.0e-10);
        Assert.assertEquals(0.0,      loveImaginary[3][0], 1.0e-10);
        Assert.assertEquals(0.0,      loveImaginary[3][1], 1.0e-10);
        Assert.assertEquals(0.0,      loveImaginary[3][2], 1.0e-10);
        Assert.assertEquals(0.0,      loveImaginary[3][3], 1.0e-10);

        Field fieldPlus = tidesField.getClass().getDeclaredField("lovePlus");
        fieldPlus.setAccessible(true);
        double[][] lovePlus = (double[][]) fieldPlus.get(tidesField);
        Assert.assertEquals(-0.00089, lovePlus[2][0], 1.0e-10);
        Assert.assertEquals(-0.00080, lovePlus[2][1], 1.0e-10);
        Assert.assertEquals(-0.00057, lovePlus[2][2], 1.0e-10);
        Assert.assertEquals(0.0,      lovePlus[3][0], 1.0e-10);
        Assert.assertEquals(0.0,      lovePlus[3][1], 1.0e-10);
        Assert.assertEquals(0.0,      lovePlus[3][2], 1.0e-10);
        Assert.assertEquals(0.0,      lovePlus[3][3], 1.0e-10);

        Field field20 = tidesField.getClass().getDeclaredField("frequencyDependenceK20");
        field20.setAccessible(true);
        Assert.assertEquals(21, ((List<?>) field20.get(tidesField)).size());

        Field field21 = tidesField.getClass().getDeclaredField("frequencyDependenceK21");
        field21.setAccessible(true);
        Assert.assertEquals(48, ((List<?>) field21.get(tidesField)).size());

        Field field22 = tidesField.getClass().getDeclaredField("frequencyDependenceK22");
        field22.setAccessible(true);
        Assert.assertEquals(2, ((List<?>) field22.get(tidesField)).size());

    }

    @Test
    public void testConventions2010() throws OrekitException, NoSuchFieldException, IllegalAccessException {

        TidesField tidesField =
                new TidesField(IERSConventions.IERS_2010.getLoveNumbersModel(),
                               IERSConventions.IERS_2010.getK20FrequencyDependenceModel(),
                               IERSConventions.IERS_2010.getK21FrequencyDependenceModel(),
                               IERSConventions.IERS_2010.getK22FrequencyDependenceModel(),
                               FramesFactory.getITRF2008(),
                               Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_MU,
                               TideSystem.ZERO_TIDE, CelestialBodyFactory.getSun(), CelestialBodyFactory.getMoon());

        Field fieldImaginary = tidesField.getClass().getDeclaredField("loveImaginary");
        fieldImaginary.setAccessible(true);
        double[][] loveImaginary = (double[][]) fieldImaginary.get(tidesField);
        Assert.assertEquals(-0.00000, loveImaginary[2][0], 1.0e-10);
        Assert.assertEquals(-0.00144, loveImaginary[2][1], 1.0e-10);
        Assert.assertEquals(-0.00130, loveImaginary[2][2], 1.0e-10);
        Assert.assertEquals(0.0,      loveImaginary[3][0], 1.0e-10);
        Assert.assertEquals(0.0,      loveImaginary[3][1], 1.0e-10);
        Assert.assertEquals(0.0,      loveImaginary[3][2], 1.0e-10);
        Assert.assertEquals(0.0,      loveImaginary[3][3], 1.0e-10);

        Field fieldPlus = tidesField.getClass().getDeclaredField("lovePlus");
        fieldPlus.setAccessible(true);
        double[][] lovePlus = (double[][]) fieldPlus.get(tidesField);
        Assert.assertEquals(-0.00089, lovePlus[2][0], 1.0e-10);
        Assert.assertEquals(-0.00080, lovePlus[2][1], 1.0e-10);
        Assert.assertEquals(-0.00057, lovePlus[2][2], 1.0e-10);
        Assert.assertEquals(0.0,      lovePlus[3][0], 1.0e-10);
        Assert.assertEquals(0.0,      lovePlus[3][1], 1.0e-10);
        Assert.assertEquals(0.0,      lovePlus[3][2], 1.0e-10);
        Assert.assertEquals(0.0,      lovePlus[3][3], 1.0e-10);

        Field field20 = tidesField.getClass().getDeclaredField("frequencyDependenceK20");
        field20.setAccessible(true);
        Assert.assertEquals(21, ((List<?>) field20.get(tidesField)).size());

        Field field21 = tidesField.getClass().getDeclaredField("frequencyDependenceK21");
        field21.setAccessible(true);
        Assert.assertEquals(48, ((List<?>) field21.get(tidesField)).size());

        Field field22 = tidesField.getClass().getDeclaredField("frequencyDependenceK22");
        field22.setAccessible(true);
        Assert.assertEquals(2, ((List<?>) field22.get(tidesField)).size());

    }

    @Test
    public void testCorruptedDelaunayMultiplier() {
        checkCorrupted("/tides/tab6.5a-corrupted-Delaunay-multiplier.txt", "Q1");
    }

    @Test
    public void testCorruptedDoodsonMultiplier() {
        checkCorrupted("/tides/tab6.5a-corrupted-Doodson-multiplier.txt", "Lk1");
    }

    @Test
    public void testCorruptedDoodsonNumber() {
        checkCorrupted("/tides/tab6.5a-corrupted-Doodson-number.txt", "No1");
    }

    @Test
    public void testMissingAmplitudeScale() {
        checkCorrupted("/tides/tab6.5a-missing-amplitude-scale.txt", null);
    }

    private void checkCorrupted(String resourceName, String lineStart) {
        try {
            new TidesField(IERSConventions.IERS_2010.getLoveNumbersModel(),
                           IERSConventions.IERS_2010.getK20FrequencyDependenceModel(),
                           resourceName,
                           IERSConventions.IERS_2010.getK22FrequencyDependenceModel(),
                           FramesFactory.getITRF2008(),
                           Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_MU,
                           TideSystem.ZERO_TIDE, CelestialBodyFactory.getSun(), CelestialBodyFactory.getMoon());
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            if (lineStart == null) {
                Assert.assertEquals(OrekitMessages.NOT_A_SUPPORTED_IERS_DATA_FILE, oe.getSpecifier());
            } else {
                Assert.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
                Assert.assertTrue(((String) oe.getParts()[2]).trim().startsWith(lineStart));
            }
        } catch (Exception e) {
            Assert.fail("wrong exception caught: " + e);
        }
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
