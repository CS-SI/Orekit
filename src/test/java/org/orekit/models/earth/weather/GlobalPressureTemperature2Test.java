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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.FieldGeodeticPoint;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.models.earth.troposphere.AzimuthalGradientCoefficients;
import org.orekit.models.earth.troposphere.FieldAzimuthalGradientCoefficients;
import org.orekit.models.earth.troposphere.FieldViennaACoefficients;
import org.orekit.models.earth.troposphere.TroposphericModelUtils;
import org.orekit.models.earth.troposphere.ViennaACoefficients;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;

public class GlobalPressureTemperature2Test {

    private static double epsilon = 1.0e-12;

    @Test
    public void testProvidedParameters() throws IOException, URISyntaxException {

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

        final GeodeticPoint                 location = new GeodeticPoint(latitude, longitude, height);
        final ViennaACoefficients           a        = model.getA(location, date);
        final PressureTemperatureHumidity   pth      = model.getWeatherParamerers(location, date);
        final AzimuthalGradientCoefficients gradient = model.getGradientCoefficients(location, date);

        Assertions.assertEquals(0.0012647,      a.getAh(),                   1.1e-7);
        Assertions.assertEquals(0.0005726,      a.getAw(),                   8.6e-8);
        Assertions.assertEquals(273.15 + 22.12, pth.getTemperature(),        2.3e-1);
        Assertions.assertEquals(1002.56,        TroposphericModelUtils.HECTO_PASCAL.fromSI(pth.getPressure()),           7.4e-1);
        Assertions.assertEquals(15.63,          TroposphericModelUtils.HECTO_PASCAL.fromSI(pth.getWaterVaporPressure()), 5.0e-2);
        Assertions.assertTrue(Double.isNaN(pth.getTm()));
        Assertions.assertTrue(Double.isNaN(pth.getLambda()));
        Assertions.assertNull(gradient);

    }

    @Test
    public void testFieldProvidedParameters() throws IOException, URISyntaxException {
        doTestFieldProvidedParameters(Binary64Field.getInstance());
    }

    protected <T extends CalculusFieldElement<T>> void doTestFieldProvidedParameters(final Field<T> field)
        throws IOException, URISyntaxException {

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

        final T latitude  = FastMath.toRadians(field.getZero().newInstance(48.20));
        final T longitude = FastMath.toRadians(field.getZero().newInstance(16.37));
        final T height    = field.getZero().newInstance(156.0);
        final FieldAbsoluteDate<T> date = FieldAbsoluteDate.createMJDDate(56141, field.getZero().newInstance(0.0), TimeScalesFactory.getUTC());
        final URL url = GlobalPressureTemperature2Test.class.getClassLoader().getResource("gpt-grid/gpt2_5_extract.grd");
        final GlobalPressureTemperature2 model =
                        new GlobalPressureTemperature2(new DataSource(url.toURI()),
                                                       TimeScalesFactory.getUTC());

        final FieldGeodeticPoint<T>                 location = new FieldGeodeticPoint<>(latitude, longitude, height);
        final FieldViennaACoefficients<T>           a        = model.getA(location, date);
        final FieldPressureTemperatureHumidity<T>   pth      = model.getWeatherParamerers(location, date);
        final FieldAzimuthalGradientCoefficients<T> gradient = model.getGradientCoefficients(location, date);

        Assertions.assertEquals(0.0012647,      a.getAh().getReal(),                   1.1e-7);
        Assertions.assertEquals(0.0005726,      a.getAw().getReal(),                   8.6e-8);
        Assertions.assertEquals(273.15 + 22.12, pth.getTemperature().getReal(),        2.3e-1);
        Assertions.assertEquals(1002.56,        TroposphericModelUtils.HECTO_PASCAL.fromSI(pth.getPressure()).getReal(),           7.4e-1);
        Assertions.assertEquals(15.63,          TroposphericModelUtils.HECTO_PASCAL.fromSI(pth.getWaterVaporPressure()).getReal(), 5.0e-2);
        Assertions.assertTrue(pth.getTm().isNaN());
        Assertions.assertTrue(pth.getLambda().isNaN());
        Assertions.assertNull(gradient);

    }

    @Test
    public void testEquality() throws IOException, URISyntaxException {
        doTestEquality("gpt-grid/gpt2_15.grd");
    }

    @Test
    public void testEqualityLoadingGpt2w() throws IOException, URISyntaxException {
        doTestEquality("gpt-grid/gpt2_15w.grd");
    }

    @Test
    public void testEqualityLoadingGpt3() throws IOException, URISyntaxException {
        doTestEquality("gpt-grid/gpt3_15.grd");
    }

    private void doTestEquality(final String resourceName) throws IOException, URISyntaxException {

        Utils.setDataRoot("regular-data");

        // Commons parameters
        final AbsoluteDate date = AbsoluteDate.createMJDDate(56141, 0.0, TimeScalesFactory.getUTC());
        final double latitude   = FastMath.toRadians(45.0);
        final double height     = 0.0;

        final URL url = GlobalPressureTemperature2Test.class.getClassLoader().getResource(resourceName);
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

        final String fileName = "corrupted-bad-data-gpt3_15.grd";
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

        final String fileName = "corrupted-irregular-grid-gpt3_15.grd";
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
    public void testCorruptedIncompleteHeader() throws IOException, URISyntaxException {

        Utils.setDataRoot("regular-data");

        final String fileName = "corrupted-incomplete-header.grd";
        final URL url = GlobalPressureTemperature2Test.class.getClassLoader().getResource("gpt-grid/" + fileName);
        try {
            new GlobalPressureTemperature2(new DataSource(url.toURI()), TimeScalesFactory.getUTC());
            Assertions.fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
            Assertions.assertEquals(1, ((Integer) oe.getParts()[0]).intValue());
            Assertions.assertTrue(((String) oe.getParts()[1]).endsWith(fileName));
        }

    }

    @Test
    public void testCorruptedMissingSeasonalColumns() throws IOException, URISyntaxException {

        Utils.setDataRoot("regular-data");

        final String fileName = "corrupted-missing-seasonal-columns-gpt3_15.grd";
        final URL url = GlobalPressureTemperature2Test.class.getClassLoader().getResource("gpt-grid/" + fileName);
        try {
            new GlobalPressureTemperature2(new DataSource(url.toURI()), TimeScalesFactory.getUTC());
            Assertions.fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
            Assertions.assertEquals(1, ((Integer) oe.getParts()[0]).intValue());
            Assertions.assertTrue(((String) oe.getParts()[1]).endsWith(fileName));
        }

    }

    @Test
    public void testCorruptedMissingDataFields() throws IOException, URISyntaxException {

        Utils.setDataRoot("regular-data");

        final String fileName = "corrupted-missing-data-fields-gpt3_15.grd";
        final URL url = GlobalPressureTemperature2Test.class.getClassLoader().getResource("gpt-grid/" + fileName);
        try {
            new GlobalPressureTemperature2(new DataSource(url.toURI()), TimeScalesFactory.getUTC());
            Assertions.fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
            Assertions.assertEquals(4, ((Integer) oe.getParts()[0]).intValue());
            Assertions.assertTrue(((String) oe.getParts()[1]).endsWith(fileName));
        }

    }

}
