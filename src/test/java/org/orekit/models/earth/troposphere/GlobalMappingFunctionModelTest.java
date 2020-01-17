/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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
package org.orekit.models.earth.troposphere;

import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;

public class GlobalMappingFunctionModelTest {

    @BeforeClass
    public static void setUpGlobal() {
        Utils.setDataRoot("atmosphere");
    }

    @Before
    public void setUp() throws OrekitException {
        Utils.setDataRoot("regular-data:potential/shm-format");
    }

    @Test
    public void testMappingFactors() {
        
        // Site (NRAO, Green Bank, WV): latitude:  0.6708665767 radians
        //                              longitude: -1.393397187 radians
        //                              height:    844.715 m
        //
        // Date: MJD 55055 -> 12 August 2009 at 0h UT
        //
        // Ref:    Petit, G. and Luzum, B. (eds.), IERS Conventions (2010),
        //         IERS Technical Note No. 36, BKG (2010)
        //
        // Expected mapping factors : hydrostatic -> 3.425246 (Ref)
        //                                    wet -> 3.449589 (Ref)

        final AbsoluteDate date = AbsoluteDate.createMJDDate(55055, 0, TimeScalesFactory.getUTC());
        
        final double latitude    = 0.6708665767;
        final double longitude   = -1.393397187;
        final double height      = 844.715;

        final double elevation     = 0.5 * FastMath.PI - 1.278564131;
        final double expectedHydro = 3.425246;
        final double expectedWet   = 3.449589;

        final MappingFunction model = new GlobalMappingFunctionModel(latitude, longitude);
        
        final double[] computedMapping = model.mappingFactors(elevation, height, model.getParameters(), date);
        
        Assert.assertEquals(expectedHydro, computedMapping[0], 1.0e-6);
        Assert.assertEquals(expectedWet,   computedMapping[1], 1.0e-6);
    }

    @Test
    public void testFixedHeight() {
        final AbsoluteDate date = new AbsoluteDate();
        MappingFunction model = new GlobalMappingFunctionModel(FastMath.toRadians(45.0), FastMath.toRadians(45.0));
        double[] lastFactors = new double[] {
            Double.MAX_VALUE,
            Double.MAX_VALUE
        };
        // mapping functions shall decline with increasing elevation angle
        for (double elev = 10d; elev < 90d; elev += 8d) {
            final double[] factors = model.mappingFactors(FastMath.toRadians(elev), 350, model.getParameters(), date);
            Assert.assertTrue(Precision.compareTo(factors[0], lastFactors[0], 1.0e-6) < 0);
            Assert.assertTrue(Precision.compareTo(factors[1], lastFactors[1], 1.0e-6) < 0);
            lastFactors[0] = factors[0];
            lastFactors[1] = factors[1];
        }
    }

}
