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

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

public class FESCnmSnmReaderTest {

    @Test
    public void testCorruptedNumberFile() throws OrekitException {
        try {
            OceanTidesReader reader = new FESCnmSnmReader("fes2004-corrupted-line.dat", 1.0e-11);
            reader.setMaxParseDegree(5);
            reader.setMaxParseOrder(5);
             DataProvidersManager.getInstance().feed(reader.getSupportedNames(), reader);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
            Assert.assertEquals(9, ((Integer) oe.getParts()[0]).intValue());
            Assert.assertEquals(" 56.554 Sa    3   0   0.00###  -0.00050    -0.00000  -0.00000", oe.getParts()[2]);
        }
    }

    @Test
    public void testUnsupportedDegreeFile() throws OrekitException {
        try {
            OceanTidesReader reader = new FESCnmSnmReader("fes2004_Cnm-Snm-8x8.dat", 1.0e-11);
            reader.setMaxParseDegree(20);
            reader.setMaxParseOrder(5);
            DataProvidersManager.getInstance().feed(reader.getSupportedNames(), reader);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.OCEAN_TIDE_DATA_DEGREE_ORDER_LIMITS, oe.getSpecifier());
            Assert.assertEquals(8, ((Integer) oe.getParts()[1]).intValue());
            Assert.assertEquals(8, ((Integer) oe.getParts()[2]).intValue());
        }
    }

    @Test
    public void testUnsupportedOrderFile() throws OrekitException {
        try {
            OceanTidesReader reader = new FESCnmSnmReader("fes2004_Cnm-Snm-8x8.dat", 1.0e-11);
            reader.setMaxParseDegree(5);
            reader.setMaxParseOrder(20);
            DataProvidersManager.getInstance().feed(reader.getSupportedNames(), reader);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.OCEAN_TIDE_DATA_DEGREE_ORDER_LIMITS, oe.getSpecifier());
            Assert.assertEquals(8, ((Integer) oe.getParts()[1]).intValue());
            Assert.assertEquals(8, ((Integer) oe.getParts()[2]).intValue());
        }
    }

    @Test
    public void testTruncatedModelFile()
        throws OrekitException {
        OceanTidesReader reader = new FESCnmSnmReader("fes2004_Cnm-Snm-8x8.dat", 1.0e-11);
        reader.setMaxParseDegree(5);
        reader.setMaxParseOrder(5);
        DataProvidersManager.getInstance().feed(reader.getSupportedNames(), reader);
        List<OceanTidesWave> waves = reader.getWaves();
        Assert.assertEquals(18, waves.size());
        for (OceanTidesWave wave : waves) {
            switch (wave.getDoodson()) {
                case 55565:
                case 55575:
                    Assert.assertEquals(2, wave.getMaxDegree());
                    Assert.assertEquals(0, wave.getMaxOrder());
                    break;
                case 56554:
                case 57555:
                case 65455:
                case 75555:
                case 85455:
                case 93555:
                case 135655:
                case 145555:
                case 163555:
                case 165555:
                case 235755:
                case 245655:
                case 255555:
                case 273555:
                case 275555:
                case 455555:
                    Assert.assertEquals(5, wave.getMaxDegree());
                    Assert.assertEquals(5, wave.getMaxOrder());
                    break;
                default:
                    Assert.fail("unexpected wave " + wave.getDoodson());
            }
        }
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data:tides");
    }

}
