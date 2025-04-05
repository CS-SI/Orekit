/* Copyright 2002-2025 CS GROUP
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

import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Test;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.data.DataSource;
import org.orekit.models.earth.weather.ConstantPressureTemperatureHumidityProvider;
import org.orekit.models.earth.weather.GlobalPressureTemperature3;
import org.orekit.models.earth.weather.PressureTemperatureHumidity;
import org.orekit.models.earth.weather.PressureTemperatureHumidityProvider;
import org.orekit.models.earth.weather.water.CIPM2007;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.TrackingCoordinates;
import org.orekit.utils.units.Unit;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

public class MendesPavlisModelTest extends AbstractPathDelayTest<MendesPavlisModel> {

    protected MendesPavlisModel buildTroposphericModel() {
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
    @Override
    public void testDelay() {

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

        doTestDelay(new AbsoluteDate(2009, 8, 12, TimeScalesFactory.getUTC()),
                    new GeodeticPoint(FastMath.toRadians(30.67166667), FastMath.toRadians(-104.0250), 2010.344), new TrackingCoordinates(0, FastMath.toRadians(38.0), 0),
                    1.932992, 0.223375e-2, 3.1334, 0.00362, 3.136995);

    }

    @Test
    @Override
    public void testFieldDelay() {

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

        doTestDelay(Binary64Field.getInstance(),
                    new AbsoluteDate(2009, 8, 12, TimeScalesFactory.getUTC()),
                    new GeodeticPoint(FastMath.toRadians(30.67166667),
                                      FastMath.toRadians(-104.0250),
                                      2010.344),
                    new TrackingCoordinates(0, FastMath.toRadians(38.0), 0),
                    1.932992, 0.223375e-2, 3.1334, 0.00362, 3.136995);

    }

    @Test
    public void testVsMariniMurray() throws IOException, URISyntaxException {
        final TimeScale utc = TimeScalesFactory.getUTC();
        final URL url = ModifiedSaastamoinenModelTest.class.getClassLoader().getResource("gpt-grid/gpt3_5.grd");
        final PressureTemperatureHumidityProvider provider =
            new GlobalPressureTemperature3(new DataSource(url.toURI()), utc);
        final double lambda = 0.532;
        final Unit lambdaUnits = TroposphericModelUtils.MICRO_M;
        doTestVsOtherModel(new MariniMurray(lambda, lambdaUnits, provider), provider,
                           new MendesPavlisModel(provider, lambda, lambdaUnits), provider,
                           1.2e-3, 6.7e-5, 0.18, 3.3e-4);
    }

}
