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
package org.orekit.models.earth.weather;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.models.earth.troposphere.TroposphericModelUtils;
import org.orekit.models.earth.troposphere.ViennaACoefficients;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;

public class GlobalPressureTemperature2Test {

    private static double epsilon = 1.0e-12;

    @Test
    public void testWeatherParameters() throws IOException, URISyntaxException {

        Utils.setDataRoot("regular-data");

        // Site Vienna: latitude:  48.20°N
        //              longitude: 16.37°E
        //              height:    156 m
        //
        // Date: 2 August 2012
        //
        // Expected outputs are given by the Department of Geodesy and Geoinformation of the Vienna University.
        // Expected parameters : temperature -> 22.12 °C
        //                       pressure    -> 1002.56 hPa
        //                       e           -> 15.63 hPa
        //                       ah          -> 0.0012647
        //                       aw          -> 0.0005726
        //
        // We test the fiability of our implementation by comparing our output values with
        // the ones obtained by the Vienna University.

        final double latitude  = FastMath.toRadians(48.20);
        final double longitude = FastMath.toRadians(16.37);
        final double height    = 156.0;
        final AbsoluteDate date = AbsoluteDate.createMJDDate(56141, 0.0, TimeScalesFactory.getUTC());
        final URL url = GlobalPressureTemperature2Test.class.getClassLoader().getResource("gpt-grid/gpt2_5_extract.grd");
        final GlobalPressureTemperature2 model =
                        new GlobalPressureTemperature2(new DataSource(url.toURI()),
                                                       TimeScalesFactory.getUTC());

        final GeodeticPoint               location = new GeodeticPoint(latitude, longitude, height);
        final ViennaACoefficients         a        = model.getA(location, date);
        final PressureTemperatureHumidity pth      = model.getWeatherParamerers(location, date);

        Assertions.assertEquals(0.0012647,      a.getAh(),                   1.1e-7);
        Assertions.assertEquals(0.0005726,      a.getAw(),                   8.6e-8);
        Assertions.assertEquals(273.15 + 22.12, pth.getTemperature(),        2.3e-1);
        Assertions.assertEquals(1002.56,        TroposphericModelUtils.HECTO_PASCAL.fromSI(pth.getPressure()),           7.4e-1);
        Assertions.assertEquals(15.63,          TroposphericModelUtils.HECTO_PASCAL.fromSI(pth.getWaterVaporPressure()), 5.0e-2);

    }

    @Test
    public void testEquality() throws IOException, URISyntaxException {

        Utils.setDataRoot("regular-data");

        // Commons parameters
        final AbsoluteDate date = AbsoluteDate.createMJDDate(56141, 0.0, TimeScalesFactory.getUTC());
        final double latitude   = FastMath.toRadians(45.0);
        final double height     = 0.0;

        final URL url = GlobalPressureTemperature2Test.class.getClassLoader().getResource("gpt-grid/gpt2_15.grd");
        GlobalPressureTemperature2 model = new GlobalPressureTemperature2(new DataSource(url.toURI()),
                                                                          TimeScalesFactory.getUTC());

        // Test longitude = 181° and longitude = -179°
        GeodeticPoint               location1 = new GeodeticPoint(latitude, FastMath.toRadians(181.0), height);
        ViennaACoefficients         a1        = model.getA(location1, date);
        PressureTemperatureHumidity pth1      = model.getWeatherParamerers(location1, date);
        GeodeticPoint               location2 = new GeodeticPoint(latitude, FastMath.toRadians(-179.0), height);
        ViennaACoefficients         a2        = model.getA(location2, date);
        PressureTemperatureHumidity pth2      = model.getWeatherParamerers(location2, date);

        Assertions.assertEquals(pth1.getTemperature(),        pth2.getTemperature(),        epsilon);
        Assertions.assertEquals(pth1.getPressure(),           pth2.getPressure(),           epsilon);
        Assertions.assertEquals(pth1.getWaterVaporPressure(), pth2.getWaterVaporPressure(), epsilon);
        Assertions.assertEquals(a1.getAh(),                   a2.getAh(),                   epsilon);
        Assertions.assertEquals(a1.getAw(),                   a2.getAw(),                   epsilon);

        // Test longitude = 180° and longitude = -180°
        location1 = new GeodeticPoint(latitude, FastMath.toRadians(180.0), height);
        a1        = model.getA(location1, date);
        pth1      = model.getWeatherParamerers(location1, date);
        location2 = new GeodeticPoint(latitude, FastMath.toRadians(-180.0), height);
        a2        = model.getA(location2, date);
        pth2      = model.getWeatherParamerers(location2, date);

        Assertions.assertEquals(pth1.getTemperature(),        pth2.getTemperature(),        epsilon);
        Assertions.assertEquals(pth1.getPressure(),           pth2.getPressure(),           epsilon);
        Assertions.assertEquals(pth1.getWaterVaporPressure(), pth2.getWaterVaporPressure(), epsilon);
        Assertions.assertEquals(a1.getAh(),                   a2.getAh(),                   epsilon);
        Assertions.assertEquals(a1.getAw(),                   a2.getAw(),                   epsilon);

        // Test longitude = 0° and longitude = 360°
        location1 = new GeodeticPoint(latitude, FastMath.toRadians(0.0), height);
        a1        = model.getA(location1, date);
        pth1      = model.getWeatherParamerers(location1, date);
        location2 = new GeodeticPoint(latitude, FastMath.toRadians(360.0), height);
        a2        = model.getA(location2, date);
        pth2      = model.getWeatherParamerers(location2, date);

        Assertions.assertEquals(pth1.getTemperature(),        pth2.getTemperature(),        epsilon);
        Assertions.assertEquals(pth1.getPressure(),           pth2.getPressure(),           epsilon);
        Assertions.assertEquals(pth1.getWaterVaporPressure(), pth2.getWaterVaporPressure(), epsilon);
        Assertions.assertEquals(a1.getAh(),                   a2.getAh(),                   epsilon);
        Assertions.assertEquals(a1.getAw(),                   a2.getAw(),                   epsilon);

    }

    @Test
    public void testCorruptedFileBadData() throws IOException, URISyntaxException {

        Utils.setDataRoot("regular-data");

        final String fileName = "corrupted-bad-data-gpt2_5.grd";
        final URL url = GlobalPressureTemperature2Test.class.getClassLoader().getResource("gpt-grid/" + fileName);
        try {
            new GlobalPressureTemperature2(new DataSource(url.toURI()), TimeScalesFactory.getUTC());
            Assertions.fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
            Assertions.assertEquals(6, ((Integer) oe.getParts()[0]).intValue());
            Assertions.assertTrue(((String) oe.getParts()[1]).endsWith(fileName));
        }

    }

    @Test
    public void testCorruptedIrregularGrid() throws IOException, URISyntaxException {

        Utils.setDataRoot("regular-data");

        final String fileName = "corrupted-irregular-grid-gpt2_5.grd";
        final URL url = GlobalPressureTemperature2Test.class.getClassLoader().getResource("gpt-grid/" + fileName);
        try {
            new GlobalPressureTemperature2(new DataSource(url.toURI()), TimeScalesFactory.getUTC());
            Assertions.fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.IRREGULAR_OR_INCOMPLETE_GRID, oe.getSpecifier());
            Assertions.assertTrue(((String) oe.getParts()[0]).endsWith(fileName));
        }

    }

    @Test
    public void testCorruptedIncompleteHEader() throws IOException, URISyntaxException {

        Utils.setDataRoot("regular-data");

        final String fileName = "corrupted-incomplete-header.grd";
        final URL url = GlobalPressureTemperature2Test.class.getClassLoader().getResource("gpt-grid/" + fileName);
        try {
            new GlobalPressureTemperature2(new DataSource(url.toURI()), TimeScalesFactory.getUTC());
            Assertions.fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
            Assertions.assertEquals(3, ((Integer) oe.getParts()[0]).intValue());
            Assertions.assertTrue(((String) oe.getParts()[1]).endsWith(fileName));
        }

    }

    @Test
    public void testCorruptedMissingDataFields() throws IOException, URISyntaxException {

        Utils.setDataRoot("regular-data");

        final String fileName = "corrupted-missing-data-fields.grd";
        final URL url = GlobalPressureTemperature2Test.class.getClassLoader().getResource("gpt-grid/" + fileName);
        try {
            new GlobalPressureTemperature2(new DataSource(url.toURI()), TimeScalesFactory.getUTC());
            Assertions.fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
            Assertions.assertEquals(5, ((Integer) oe.getParts()[0]).intValue());
            Assertions.assertTrue(((String) oe.getParts()[1]).endsWith(fileName));
        }

    }

}
