/* Copyright 2002-2024 Thales Alenia Space
 * Licensed to CS Communication & Systèmes (CS) under one or more
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.data.DataSource;
import org.orekit.models.earth.weather.GlobalPressureTemperature3;
import org.orekit.models.earth.weather.PressureTemperatureHumidityProvider;
import org.orekit.time.TimeScalesFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

public class AskneNordiusModelTest extends AbstractPathDelayTest<AskneNordiusModel> {

    @Override
    protected AskneNordiusModel buildTroposphericModel() {
        return new AskneNordiusModel(new GlobalMappingFunctionModel());
    }

    @Test
    @Override
    public void testDelay() {
        resetWeatherProvider(getGPT());
        doTestDelay(defaultDate, defaultPoint, defaultTrackingCoordinates,
                    2.0938, 6.80095, 3.39416, 11.03650, 14.43066);
    }

    @Test
    @Override
    public void testFieldDelay() {
        resetWeatherProvider(getGPT());
        doTestDelay(Binary64Field.getInstance(),
                    defaultDate, defaultPoint, defaultTrackingCoordinates,
                    2.0938, 6.80095, 3.39416, 11.03650, 14.43066);
    }

    @Test
    @Override
    public void testFixedElevation() {
        resetWeatherProvider(getGPT());
        super.testFixedElevation();
    }

    @Test
    @Override
    public void testFieldFixedElevation() {
        resetWeatherProvider(getGPT());
        super.testFieldFixedElevation();
    }

    @Test
    @Override
    public void testFixedHeight() {
        resetWeatherProvider(getGPT());
        super.testFixedHeight();
    }

    @Test
    @Override
    public void testFieldFixedHeight() {
        resetWeatherProvider(getGPT());
        super.testFieldFixedHeight();
    }

    private PressureTemperatureHumidityProvider getGPT() {
        try {
            final URL url = AskneNordiusModelTest.class.getClassLoader().getResource("gpt-grid/gpt3_5.grd");
            return new GlobalPressureTemperature3(new DataSource(url.toURI()), TimeScalesFactory.getUTC());
        } catch (URISyntaxException | IOException use) {
            Assertions.fail(use);
            return null;
        }
    }

}
