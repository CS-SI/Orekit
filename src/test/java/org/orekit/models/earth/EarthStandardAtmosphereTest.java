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
package org.orekit.models.earth;

import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class EarthStandardAtmosphereTest {

    private final double epsilon = 1e-15;

    @BeforeEach
    public void setUp()
        throws Exception {
    }

    @AfterEach
    public void tearDown()
        throws Exception {
    }

    @Test
    public void testEarthStandardAtmosphereRefraction() {
        EarthStandardAtmosphereRefraction model = new EarthStandardAtmosphereRefraction();

        Assertions.assertEquals(model.getPressure(), EarthStandardAtmosphereRefraction.DEFAULT_PRESSURE, epsilon);
        Assertions.assertEquals(model.getTemperature(), EarthStandardAtmosphereRefraction.DEFAULT_TEMPERATURE, epsilon);
    }

    @Test
    public void testEarthStandardAtmosphereRefractionDoubleDouble() {
        final double pressure = 100e3;
        final double temperature = 270;
        EarthStandardAtmosphereRefraction model = new EarthStandardAtmosphereRefraction(pressure, temperature);

        Assertions.assertEquals(model.getPressure(), pressure, epsilon);
        Assertions.assertEquals(model.getTemperature(), temperature, epsilon);
    }

    @Test
    public void testSetGetPressure() {
        double pressure = 100e3;
        double temperature = 270;
        EarthStandardAtmosphereRefraction model = new EarthStandardAtmosphereRefraction(pressure, temperature);

        Assertions.assertEquals(model.getPressure(), pressure, epsilon);

        pressure = 105389.2;
        model.setPressure(pressure);

        Assertions.assertEquals(model.getPressure(), pressure, epsilon);
    }

    @Test
    public void testSetGetTemperature() {
        double pressure = 100e3;
        double temperature = 270;
        EarthStandardAtmosphereRefraction model = new EarthStandardAtmosphereRefraction(pressure, temperature);

        Assertions.assertEquals(model.getTemperature(), temperature, epsilon);

        temperature = 273;
        model.setTemperature(temperature);

        Assertions.assertEquals(model.getTemperature(), temperature, epsilon);

    }

    @Test
    public void testGetRefraction() {
        double pressure = 101325;
        double temperature = 290;
        EarthStandardAtmosphereRefraction model = new EarthStandardAtmosphereRefraction(pressure, temperature);

        double refractedElevation = model.getRefraction(FastMath.toRadians(1.0));

        Assertions.assertEquals(0.0061922285, refractedElevation, 1e-9);
    }

}
