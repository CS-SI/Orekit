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

import org.hipparchus.util.Binary64Field;
import org.junit.jupiter.api.Test;
import org.orekit.data.DataSource;
import org.orekit.models.earth.troposphere.AbstractPathDelayTest;
import org.orekit.models.earth.troposphere.CanonicalSaastamoinenModel;
import org.orekit.models.earth.troposphere.ConstantAzimuthalGradientProvider;
import org.orekit.models.earth.troposphere.ConstantViennaAProvider;
import org.orekit.models.earth.troposphere.ModifiedSaastamoinenModel;
import org.orekit.models.earth.troposphere.TroposphericModelUtils;
import org.orekit.models.earth.troposphere.ViennaACoefficients;
import org.orekit.models.earth.troposphere.ViennaOne;
import org.orekit.models.earth.troposphere.ViennaThree;
import org.orekit.models.earth.weather.GlobalPressureTemperature2;
import org.orekit.models.earth.weather.GlobalPressureTemperature3;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

public class ITURP834PathDelayTest extends AbstractPathDelayTest<ITURP834PathDelay> {

    protected ITURP834PathDelay buildTroposphericModel() {
        return new ITURP834PathDelay(TimeScalesFactory.getUTC());
    }

    @Test
    @Override
    public void testDelay() {
        resetWeatherProvider(new ITURP834WeatherParametersProvider(TimeScalesFactory.getUTC()));
        doTestDelay(defaultDate, defaultPoint, defaultTrackingCoordinates,
                    2.07738, 0.05483, 3.36742, 0.088969, 3.456389);
    }

    @Test
    @Override
    public void testFieldDelay() {
        resetWeatherProvider(new ITURP834WeatherParametersProvider(TimeScalesFactory.getUTC()));
        doTestDelay(Binary64Field.getInstance(),
                    defaultDate, defaultPoint, defaultTrackingCoordinates,
                    2.07738, 0.05483, 3.36742, 0.088969, 3.456389);
    }

    @Test
    @Override
    public void testFixedElevation() {
        resetWeatherProvider(new ITURP834WeatherParametersProvider(TimeScalesFactory.getUTC()));
        super.testFixedElevation();
    }

    @Test
    @Override
    public void testFieldFixedElevation() {
        resetWeatherProvider(new ITURP834WeatherParametersProvider(TimeScalesFactory.getUTC()));
        super.testFieldFixedElevation();
    }

    @Test
    @Override
    public void testFixedHeight() {
        resetWeatherProvider(new ITURP834WeatherParametersProvider(TimeScalesFactory.getUTC()));
        super.testFixedHeight();
    }

    @Test
    @Override
    public void testFieldFixedHeight() {
        resetWeatherProvider(new ITURP834WeatherParametersProvider(TimeScalesFactory.getUTC()));
        super.testFieldFixedHeight();
    }

    @Test
    public void testVsVienna1WithCanonicalSaastamoinenAndGPT2() throws IOException, URISyntaxException {
        final TimeScale utc = TimeScalesFactory.getUTC();
        final URL       url = ITURP834PathDelayTest.class.getClassLoader().getResource("gpt-grid/gpt2_5.grd");
        doTestVsOtherModel(new ViennaOne(new ConstantViennaAProvider(new ViennaACoefficients(0.00127683, 0.00060955)),
                                         new ConstantAzimuthalGradientProvider(null),
                                         new CanonicalSaastamoinenModel(),
                                         utc),
                           new GlobalPressureTemperature2(new DataSource(url.toURI())),
                           buildTroposphericModel(),
                           new ITURP834WeatherParametersProvider(utc),
                           0.017, 0.019, 0.144, 0.191);
    }

    @Test
    public void testVsVienna3WithModifiedSaastamoinenAndGPT3() throws IOException, URISyntaxException {
        final TimeScale utc = TimeScalesFactory.getUTC();
        final URL       url = ITURP834PathDelayTest.class.getClassLoader().getResource("gpt-grid/gpt3_5.grd");
        doTestVsOtherModel(new ViennaThree(new ConstantViennaAProvider(new ViennaACoefficients(0.00127683, 0.00060955)),
                                           new ConstantAzimuthalGradientProvider(null),
                                           new ModifiedSaastamoinenModel(TroposphericModelUtils.STANDARD_ATMOSPHERE_PROVIDER),
                                           utc),
                           new GlobalPressureTemperature3(new DataSource(url.toURI()), utc),
                           buildTroposphericModel(),
                           new ITURP834WeatherParametersProvider(utc),
                           0.017, 0.008, 0.098, 0.076);
    }

}
