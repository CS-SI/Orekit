/* Copyright 2002-2016 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class EarthStandardAtmosphereTest {

    private final double epsilon = 1e-15;

    @Before
    public void setUp()
        throws Exception {
    }

    @After
    public void tearDown()
        throws Exception {
    }

    @Test
    public void testEarthStandardAtmosphereRefraction() {
        EarthStandardAtmosphereRefraction model = new EarthStandardAtmosphereRefraction();

        Assert.assertEquals(model.getPressure(), EarthStandardAtmosphereRefraction.DEFAULT_PRESSURE, epsilon);
        Assert.assertEquals(model.getTemperature(), EarthStandardAtmosphereRefraction.DEFAULT_TEMPERATURE, epsilon);
    }

    @Test
    public void testEarthStandardAtmosphereRefractionDoubleDouble() {
        final double pressure = 100e3;
        final double temperature = 270;
        EarthStandardAtmosphereRefraction model = new EarthStandardAtmosphereRefraction(pressure, temperature);

        Assert.assertEquals(model.getPressure(), pressure, epsilon);
        Assert.assertEquals(model.getTemperature(), temperature, epsilon);
    }

    @Test
    public void testSetGetPressure() {
        double pressure = 100e3;
        double temperature = 270;
        EarthStandardAtmosphereRefraction model = new EarthStandardAtmosphereRefraction(pressure, temperature);

        Assert.assertEquals(model.getPressure(), pressure, epsilon);

        pressure = 105389.2;
        model.setPressure(pressure);

        Assert.assertEquals(model.getPressure(), pressure, epsilon);
    }

    @Test
    public void testSetGetTemperature() {
        double pressure = 100e3;
        double temperature = 270;
        EarthStandardAtmosphereRefraction model = new EarthStandardAtmosphereRefraction(pressure, temperature);

        Assert.assertEquals(model.getTemperature(), temperature, epsilon);

        temperature = 273;
        model.setTemperature(temperature);

        Assert.assertEquals(model.getTemperature(), temperature, epsilon);

    }

    @Test
    public void testGetRefraction() {
        double pressure = 101325;
        double temperature = 290;
        EarthStandardAtmosphereRefraction model = new EarthStandardAtmosphereRefraction(pressure, temperature);

        double refractedElevation = model.getRefraction(FastMath.toRadians(1.0));

        Assert.assertEquals(0.0061922285, refractedElevation, 1e-9);
    }

}
