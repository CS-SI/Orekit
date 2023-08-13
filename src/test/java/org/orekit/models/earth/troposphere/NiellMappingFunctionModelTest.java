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

public class NiellMappingFunctionModelTest {

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

        // Site (Le Mans, France):      latitude:  48.0°
        //                              longitude: 0.20°
        //                              height:    68 m
        //
        // Date: 1st January 1994 at 0h UT
        //
        // Ref:    Mercier F., Perosanz F., Mesures GNSS, Résolution des ambiguités.
        //
        // Expected mapping factors : hydrostatic -> 10.16 (Ref)
        //                                    wet -> 10.75 (Ref)

        final AbsoluteDate date = new AbsoluteDate(1994, 1, 1, TimeScalesFactory.getUTC());

        final double latitude    = FastMath.toRadians(48.0);
        final double longitude   = FastMath.toRadians(0.20);
        final double height      = 68.0;
        final GeodeticPoint point = new GeodeticPoint(latitude, longitude, height);

        final double elevation     = FastMath.toRadians(5.0);
        final double expectedHydro = 10.16;
        final double expectedWet   = 10.75;

        final MappingFunction model = new NiellMappingFunctionModel();

        final double[] computedMapping = model.mappingFactors(elevation, point, date);

        Assertions.assertEquals(expectedHydro, computedMapping[0], 1.0e-2);
        Assertions.assertEquals(expectedWet,   computedMapping[1], 1.0e-2);
    }

    @Test
    public void testFixedHeight() {
        final AbsoluteDate date = new AbsoluteDate();
        final GeodeticPoint point = new GeodeticPoint(FastMath.toRadians(45.0), FastMath.toRadians(45.0), 350.0);
        MappingFunction model = new NiellMappingFunctionModel();
        double[] lastFactors = new double[] {
            Double.MAX_VALUE,
            Double.MAX_VALUE
        };
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
