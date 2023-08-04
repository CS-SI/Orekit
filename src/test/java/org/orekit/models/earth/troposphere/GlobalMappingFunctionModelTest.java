/* Copyright 2002-2023 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;

public class GlobalMappingFunctionModelTest {

    @BeforeAll
    public static void setUpGlobal() {
        Utils.setDataRoot("atmosphere");
    }

    @BeforeEach
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
        final GeodeticPoint point = new GeodeticPoint(latitude, longitude, height);

        final double elevation     = 0.5 * FastMath.PI - 1.278564131;
        final double expectedHydro = 3.425246;
        final double expectedWet   = 3.449589;

        final MappingFunction model = new GlobalMappingFunctionModel();

        final double[] computedMapping = model.mappingFactors(elevation, point, date);

        Assertions.assertEquals(expectedHydro, computedMapping[0], 1.0e-6);
        Assertions.assertEquals(expectedWet,   computedMapping[1], 1.0e-6);
    }

    @Test
    public void testFixedHeight() {
        final AbsoluteDate date = new AbsoluteDate();
        MappingFunction model = new GlobalMappingFunctionModel();
        double[] lastFactors = new double[] {
            Double.MAX_VALUE,
            Double.MAX_VALUE
        };
        GeodeticPoint point = new GeodeticPoint(FastMath.toRadians(45.0), FastMath.toRadians(45.0), 350.0);
        // mapping functions shall decline with increasing elevation angle
        for (double elev = 10d; elev < 90d; elev += 8d) {
            final double[] factors = model.mappingFactors(FastMath.toRadians(elev), point, date);
            Assertions.assertTrue(Precision.compareTo(factors[0], lastFactors[0], 1.0e-6) < 0);
            Assertions.assertTrue(Precision.compareTo(factors[1], lastFactors[1], 1.0e-6) < 0);
            lastFactors[0] = factors[0];
            lastFactors[1] = factors[1];
        }
    }

}
