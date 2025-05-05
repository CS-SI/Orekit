/* Copyright 2022-2025 Thales Alenia Space
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
package org.orekit.models.earth.troposphere.iturp834;

import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.data.DataSource;
import org.orekit.models.earth.weather.GlobalPressureTemperature3;
import org.orekit.models.earth.weather.PressureTemperatureHumidity;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

public class ITURP834WeatherParameterProviderTest {

    @Test
    public void testVsGPT3() throws IOException, URISyntaxException {
        Utils.setDataRoot("regular-data");
        final TimeScale utc = TimeScalesFactory.getUTC();
        final AbsoluteDate date = new AbsoluteDate(2018, 11, 25, 12, 0, 0, utc);
        final GeodeticPoint point = new GeodeticPoint(FastMath.toRadians(47.71675), FastMath.toRadians(6.12264), 300.0);
        final URL url = ITURP834WeatherParameterProviderTest.class.getClassLoader().getResource("gpt-grid/gpt3_5.grd");
        final GlobalPressureTemperature3 gpt3 = new GlobalPressureTemperature3(new DataSource(url.toURI()),
                                                                               TimeScalesFactory.getUTC());
        final ITURP834WeatherParametersProvider itu = new ITURP834WeatherParametersProvider(utc);

        final PressureTemperatureHumidity pth    = gpt3.getWeatherParameters(point, date);
        final PressureTemperatureHumidity pthITU = itu.getWeatherParameters(point, date);
        Assertions.assertEquals(pth.getAltitude(),           pthITU.getAltitude(),           1.0e-15);
        Assertions.assertEquals(pth.getTemperature(),        pthITU.getTemperature(),        0.051);
        Assertions.assertEquals(pth.getPressure(),           pthITU.getPressure(),           540.0);
        Assertions.assertEquals(pth.getWaterVaporPressure(), pthITU.getWaterVaporPressure(),  89.0);
        Assertions.assertEquals(pth.getTm(),                 pthITU.getTm(),                 1.8);
        Assertions.assertEquals(pth.getLambda(),             pthITU.getLambda(),             0.84);

    }

}
