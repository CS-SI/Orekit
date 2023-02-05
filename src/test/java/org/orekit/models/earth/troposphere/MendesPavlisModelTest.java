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

;

public class MendesPavlisModelTest {

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
    public void testZenithDelay() {

        // Site:   McDonald Observatory
        //         latitude:  30.67166667 °
        //         longitude: -104.0250 °
        //         height:    2010.344 m
        //
        // Meteo:  pressure:            798.4188 hPa
        //         water vapor presure: 14.322 hPa
        //         temperature:         300.15 K
        //         humidity:            40 %
        //
        // Ref:    Petit, G. and Luzum, B. (eds.), IERS Conventions (2010),
        //         IERS Technical Note No. 36, BKG (2010)

        final double latitude     = FastMath.toRadians(30.67166667);
        final double longitude    = FastMath.toRadians(-104.0250);
        final double height       = 2010.344;
        final double pressure     = 798.4188;
        final double temperature  = 300.15;
        final double humidity     = 0.4;
        final double lambda       = 0.532;
        final GeodeticPoint point = new GeodeticPoint(latitude, longitude, height);

        // Expected zenith hydrostatic delay: 1.932992 m (Ref)
        final double expectedHydroDelay = 1.932992;
        // Expected zenith wet delay: 0.223375*10-2 m (Ref)
        final double expectedWetDelay   = 0.223375e-2;
        // Expected total zenith delay: 1.935226 m (Ref)
        final double expectedDelay      = 1.935226;

        final double precision = 4.0e-6;

        final AbsoluteDate date = new AbsoluteDate(2009, 8, 12, TimeScalesFactory.getUTC());

        final MendesPavlisModel model = new MendesPavlisModel(temperature, pressure,
                                                               humidity, lambda);

        final double[] computedDelay = model.computeZenithDelay(point, model.getParameters(), date);

        Assertions.assertEquals(expectedHydroDelay, computedDelay[0],                    precision);
        Assertions.assertEquals(expectedWetDelay,                      computedDelay[1], precision);
        Assertions.assertEquals(expectedDelay,      computedDelay[0] + computedDelay[1], precision);

    }

    @Test
    public void testMappingFactors() {

        // Site:   McDonald Observatory
        //         latitude:  30.67166667 °
        //         longitude: -104.0250 °
        //         height:    2075 m
        //
        // Meteo:  pressure:            798.4188 hPa
        //         water vapor presure: 14.322 hPa
        //         temperature:         300.15 K
        //         humidity:            40 %
        //
        // Ref:    Petit, G. and Luzum, B. (eds.), IERS Conventions (2010),
        //         IERS Technical Note No. 36, BKG (2010)

        final AbsoluteDate date = new AbsoluteDate(2009, 8, 12, TimeScalesFactory.getUTC());

        final double latitude     = FastMath.toRadians(30.67166667);
        final double longitude    = FastMath.toRadians(-104.0250);
        final double height       = 2075;
        final double pressure     = 798.4188;
        final double temperature  = 300.15;
        final double humidity     = 0.4;
        final double lambda       = 0.532;
        final GeodeticPoint point = new GeodeticPoint(latitude, longitude, height);

        final double elevation        = FastMath.toRadians(15.0);
        // Expected mapping factor: 3.80024367 (Ref)
        final double expectedMapping    = 3.80024367;

        // Test for the second constructor
        final MendesPavlisModel model = new MendesPavlisModel(temperature, pressure,
                                                               humidity, lambda);

        final double[] computedMapping = model.mappingFactors(elevation, point, date);

        Assertions.assertEquals(expectedMapping, computedMapping[0], 5.0e-8);
        Assertions.assertEquals(expectedMapping, computedMapping[1], 5.0e-8);
    }

    @Test
    public void testDelay() {
        final double elevation = 10d;
        final double height = 100d;
        final AbsoluteDate date = new AbsoluteDate();
        final GeodeticPoint point = new GeodeticPoint(FastMath.toRadians(45.0), FastMath.toRadians(45.0), height);
        MendesPavlisModel model = MendesPavlisModel.getStandardModel( 0.6943);
        final double path = model.pathDelay(FastMath.toRadians(elevation), point, model.getParameters(), date);
        Assertions.assertTrue(Precision.compareTo(path, 20d, epsilon) < 0);
        Assertions.assertTrue(Precision.compareTo(path, 0d, epsilon) > 0);
    }

    @Test
    public void testFixedHeight() {
        final AbsoluteDate date = new AbsoluteDate();
        final GeodeticPoint point = new GeodeticPoint(FastMath.toRadians(45.0), FastMath.toRadians(45.0), 350.0);
        MendesPavlisModel model = MendesPavlisModel.getStandardModel(0.6943);
        double lastDelay = Double.MAX_VALUE;
        // delay shall decline with increasing elevation angle
        for (double elev = 10d; elev < 90d; elev += 8d) {
            final double delay = model.pathDelay(FastMath.toRadians(elev), point, model.getParameters(), date);
            Assertions.assertTrue(Precision.compareTo(delay, lastDelay, epsilon) < 0);
            lastDelay = delay;
        }
    }

}
