/* Copyright 2002-2024 CS GROUP
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
import org.junit.jupiter.api.Test;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.models.earth.weather.ConstantPressureTemperatureHumidityProvider;
import org.orekit.models.earth.weather.PressureTemperatureHumidity;
import org.orekit.models.earth.weather.water.CIPM2007;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.TrackingCoordinates;

public class MendesPavlisMappingFunctionTest extends AbstractMappingFunctionTest<MendesPavlisModel> {

    protected MendesPavlisModel buildMappingFunction() {
        final double height      = 2010.344;
        final double pressure    = TroposphericModelUtils.HECTO_PASCAL.toSI(798.4188);
        final double temperature = 300.15;
        final double humidity    = 0.4;
        final PressureTemperatureHumidity pth =
            new PressureTemperatureHumidity(height, pressure, temperature,
                                            new CIPM2007().waterVaporPressure(pressure, temperature, humidity),
                                            Double.NaN,  Double.NaN);
        return new MendesPavlisModel(new ConstantPressureTemperatureHumidityProvider(pth),
                                     0.532, TroposphericModelUtils.MICRO_M);
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
        doTestMappingFactors(new AbsoluteDate(2009, 8, 12, 12, 0, 0, TimeScalesFactory.getUTC()),
                             new GeodeticPoint(FastMath.toRadians(30.67166667), FastMath.toRadians(-104.0250), 2075),
                             new TrackingCoordinates(0.0, FastMath.toRadians(15.0), 0.0),
                             3.8002, 3.8002);

    }

    @Test
    @Override
    public void testDerivatives() {
        doTestDerivatives(5.0e-16, 1.0e-100, 1.0e-100, 2.0e-8, 1.0e-100);
    }

}
