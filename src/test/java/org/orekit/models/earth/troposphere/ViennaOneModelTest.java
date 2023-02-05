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

public class ViennaOneModelTest {

    private static double epsilon = 1e-6;

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

        // Site (NRAO, Green Bank, WV): latitude:  38°
        //                              longitude: 280°
        //                              height:    824.17 m
        //
        // Date: MJD 55055 -> 12 August 2009 at 0h UT
        //
        // Ref for the inputs:    Petit, G. and Luzum, B. (eds.), IERS Conventions (2010),
        //                        IERS Technical Note No. 36, BKG (2010)
        //
        // Values: ah  = 0.00127683
        //         aw  = 0.00060955
        //         zhd = 2.0966 m
        //         zwd = 0.2140 m
        //
        // Values taken from: http://vmf.geo.tuwien.ac.at/trop_products/GRID/2.5x2/VMF1/VMF1_OP/2009/VMFG_20090812.H00
        //
        // Expected mapping factors : hydrostatic -> 3.425088
        //                                    wet -> 3.448300
        //
        // Expected outputs are obtained by performing the Matlab script vmf1_ht.m provided by TU WIEN:
        // http://vmf.geo.tuwien.ac.at/codes/
        //

        final AbsoluteDate date = AbsoluteDate.createMJDDate(55055, 0, TimeScalesFactory.getUTC());

        final double latitude     = FastMath.toRadians(38.0);
        final double longitude    = FastMath.toRadians(280.0);
        final double height       = 824.17;
        final GeodeticPoint point = new GeodeticPoint(latitude, longitude, height);

        final double elevation     = 0.5 * FastMath.PI - 1.278564131;
        final double expectedHydro = 3.425088;
        final double expectedWet   = 3.448300;

        final double[] a = { 0.00127683, 0.00060955 };
        final double[] z = {2.0966, 0.2140};

        final ViennaOneModel model = new ViennaOneModel(a, z);

        final double[] computedMapping = model.mappingFactors(elevation, point, date);

        Assertions.assertEquals(expectedHydro, computedMapping[0], 4.1e-6);
        Assertions.assertEquals(expectedWet,   computedMapping[1], 1.0e-6);
    }

    @Test
    public void testDelay() {
        final double elevation = 10d;
        final double height = 100d;
        final AbsoluteDate date = new AbsoluteDate();
        final double[] a = { 0.00127683, 0.00060955 };
        final double[] z = {2.0966, 0.2140};
        final GeodeticPoint point = new GeodeticPoint(FastMath.toRadians(45.0), FastMath.toRadians(45.0), height);
        ViennaOneModel model = new ViennaOneModel(a, z);
        final double path = model.pathDelay(FastMath.toRadians(elevation), point, model.getParameters(date), date);
        Assertions.assertTrue(Precision.compareTo(path, 20d, epsilon) < 0);
        Assertions.assertTrue(Precision.compareTo(path, 0d, epsilon) > 0);
    }

    @Test
    public void testFixedHeight() {
        final AbsoluteDate date = new AbsoluteDate();
        final double[] a = { 0.00127683, 0.00060955 };
        final double[] z = {2.0966, 0.2140};
        final GeodeticPoint point = new GeodeticPoint(FastMath.toRadians(45.0), FastMath.toRadians(45.0), 350.0);
        ViennaOneModel model = new ViennaOneModel(a, z);
        double lastDelay = Double.MAX_VALUE;
        // delay shall decline with increasing elevation angle
        for (double elev = 10d; elev < 90d; elev += 8d) {
            final double delay = model.pathDelay(FastMath.toRadians(elev), point, model.getParameters(date), date);
            Assertions.assertTrue(Precision.compareTo(delay, lastDelay, epsilon) < 0);
            lastDelay = delay;
        }
    }

}
