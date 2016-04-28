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
package org.orekit.forces.gravity.potential;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

public class FESCHatEpsilonReaderTest {

    @Test
    public void testTooLargeDegree()
        throws OrekitException {

        try {
        AstronomicalAmplitudeReader aaReader =
                new AstronomicalAmplitudeReader("hf-fes2004.dat", 5, 2, 3, 1.0);
        DataProvidersManager.getInstance().feed(aaReader.getSupportedNames(), aaReader);
        Map<Integer, Double> map = aaReader.getAstronomicalAmplitudesMap();
        OceanTidesReader reader2 = new FESCHatEpsilonReader("fes2004-7x7.dat",
                                                            0.01, FastMath.toRadians(1.0),
                                                            OceanLoadDeformationCoefficients.IERS_2010,
                                                            map);
        reader2.setMaxParseDegree(8);
        reader2.setMaxParseOrder(8);
        DataProvidersManager.getInstance().feed(reader2.getSupportedNames(), reader2);
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.OCEAN_TIDE_LOAD_DEFORMATION_LIMITS, oe.getSpecifier());
            Assert.assertEquals(6, ((Integer) oe.getParts()[0]).intValue());
            Assert.assertEquals(7, ((Integer) oe.getParts()[1]).intValue());
        }
    }

    @Test
    public void testCoefficientsConversion2010()
        throws OrekitException, SecurityException, NoSuchFieldException,
               IllegalArgumentException, IllegalAccessException {
        checkConversion(OceanLoadDeformationCoefficients.IERS_2010, 1.0e-14);
    }

    @Test
    public void testCoefficientsConversionGegout()
        throws OrekitException, SecurityException, NoSuchFieldException,
               IllegalArgumentException, IllegalAccessException {
        checkConversion(OceanLoadDeformationCoefficients.GEGOUT, 1.7e-12);
    }

    private void checkConversion(OceanLoadDeformationCoefficients oldc,
                                 double threshold)
        throws OrekitException, SecurityException, NoSuchFieldException,
               IllegalArgumentException, IllegalAccessException {

        Field cGammaField = OceanTidesWave.class.getDeclaredField("cGamma");
        cGammaField.setAccessible(true);
        Field cPlusField = OceanTidesWave.class.getDeclaredField("cPlus");
        cPlusField.setAccessible(true);
        Field sPlusField = OceanTidesWave.class.getDeclaredField("sPlus");
        sPlusField.setAccessible(true);
        Field cMinusField = OceanTidesWave.class.getDeclaredField("cMinus");
        cMinusField.setAccessible(true);
        Field sMinusField = OceanTidesWave.class.getDeclaredField("sMinus");
        sMinusField.setAccessible(true);

        OceanTidesReader reader1 = new FESCnmSnmReader("fes2004_Cnm-Snm-8x8.dat", 1.0e-11);
        reader1.setMaxParseDegree(6);
        reader1.setMaxParseOrder(6);
        DataProvidersManager.getInstance().feed(reader1.getSupportedNames(), reader1);
        List<OceanTidesWave> waves1 =  reader1.getWaves();

        AstronomicalAmplitudeReader aaReader =
                new AstronomicalAmplitudeReader("hf-fes2004.dat", 5, 2, 3, 1.0);
        DataProvidersManager.getInstance().feed(aaReader.getSupportedNames(), aaReader);
        Map<Integer, Double> map = aaReader.getAstronomicalAmplitudesMap();
        OceanTidesReader reader2 = new FESCHatEpsilonReader("fes2004-7x7.dat",
                                                            0.01, FastMath.toRadians(1.0),
                                                            oldc,
                                                            map);
        reader2.setMaxParseDegree(6);
        reader2.setMaxParseOrder(6);
        DataProvidersManager.getInstance().feed(reader2.getSupportedNames(), reader2);
        List<OceanTidesWave> waves2 =  reader2.getWaves();

        for (OceanTidesWave wave1 : waves1) {

            boolean found = false;
            for (OceanTidesWave wave2 : waves2) {
                if (wave1.getDoodson() == wave2.getDoodson()) {
                    found = true;

                    Assert.assertEquals(wave1.getMaxDegree(), wave2.getMaxDegree());
                    Assert.assertEquals(wave1.getMaxOrder(),  wave2.getMaxOrder());
                    double[][] cP1 = (double[][])  cPlusField.get(wave1);
                    double[][] sP1 = (double[][])  sPlusField.get(wave1);
                    double[][] cM1 = (double[][])  cMinusField.get(wave1);
                    double[][] sM1 = (double[][])  sMinusField.get(wave1);
                    double[][] cP2 = (double[][])  cPlusField.get(wave2);
                    double[][] sP2 = (double[][])  sPlusField.get(wave2);
                    double[][] cM2 = (double[][])  cMinusField.get(wave2);
                    double[][] sM2 = (double[][])  sMinusField.get(wave2);

                    for (int n = 2; n <= wave1.getMaxDegree(); ++n) {
                        for (int m = 0; m <= FastMath.min(wave1.getMaxOrder(), n); ++m) {
                            Assert.assertEquals(cP1[n][m], cP2[n][m], threshold);
                            Assert.assertEquals(sP1[n][m], sP2[n][m], threshold);
                            Assert.assertEquals(cM1[n][m], cM2[n][m], threshold);
                            Assert.assertEquals(sM1[n][m], sM2[n][m], threshold);
                        }
                    }

                }
            }
            Assert.assertTrue(found);
        }

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data:tides");
    }

}
