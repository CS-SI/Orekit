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
package org.orekit.models.earth;

import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EarthStandardAtmosphereTest {

    private final double epsilon = 1e-15;

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void testEarthStandardAtmosphereRefraction() {
        EarthStandardAtmosphereRefraction model = new EarthStandardAtmosphereRefraction();

        assertEquals(EarthStandardAtmosphereRefraction.DEFAULT_PRESSURE, model.getPressure(), epsilon);
        assertEquals(EarthStandardAtmosphereRefraction.DEFAULT_TEMPERATURE, model.getTemperature(), epsilon);
    }

    @Test
    void testEarthStandardAtmosphereRefractionDoubleDouble() {
        final double pressure = 100e3;
        final double temperature = 270;
        EarthStandardAtmosphereRefraction model = new EarthStandardAtmosphereRefraction(pressure, temperature);

        assertEquals(model.getPressure(), pressure, epsilon);
        assertEquals(model.getTemperature(), temperature, epsilon);
    }

    @Test
    void testSetGetPressure() {
        double pressure = 100e3;
        double temperature = 270;
        EarthStandardAtmosphereRefraction model = new EarthStandardAtmosphereRefraction(pressure, temperature);

        assertEquals(model.getPressure(), pressure, epsilon);

        pressure = 105389.2;
        model.setPressure(pressure);

        assertEquals(model.getPressure(), pressure, epsilon);
    }

    @Test
    void testSetGetTemperature() {
        double pressure = 100e3;
        double temperature = 270;
        EarthStandardAtmosphereRefraction model = new EarthStandardAtmosphereRefraction(pressure, temperature);

        assertEquals(model.getTemperature(), temperature, epsilon);

        temperature = 273;
        model.setTemperature(temperature);

        assertEquals(model.getTemperature(), temperature, epsilon);

    }

    @Test
    void testGetRefraction() {
        double pressure = 101325;
        double temperature = 290;
        EarthStandardAtmosphereRefraction model = new EarthStandardAtmosphereRefraction(pressure, temperature);

        double refractedElevation = model.getRefraction(FastMath.toRadians(1.0));

        assertEquals(0.0061922285, refractedElevation, 1e-9);
    }

}
