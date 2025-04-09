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
import org.orekit.data.DataContext;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.models.earth.troposphere.FieldViennaACoefficients;
import org.orekit.models.earth.troposphere.TroposphericModelUtils;
import org.orekit.models.earth.troposphere.ViennaACoefficients;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;

public class GlobalPressureTemperature2Test {

    private static final double epsilon = 1.0e-12;

    @Test
    public void testMatlabRef0() throws IOException, URISyntaxException {

        Utils.setDataRoot("regular-data");

        // reference output obtained by running TU-Wien https://vmf.geo.tuwien.ac.at/codes/gpt2.m matlab script
        final double latitude  = FastMath.toRadians(48.20);
        final double longitude = FastMath.toRadians(16.37);
        final double height    = 156.0;
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(4596.5 * Constants.JULIAN_DAY);
        final URL url = GlobalPressureTemperature2Test.class.getClassLoader().getResource("gpt-grid/gpt2_5.grd");
        final GlobalPressureTemperature2 model =
                        new GlobalPressureTemperature2(new DataSource(url.toURI()),
                                                       DataContext.getDefault().getTimeScales());

        final GeodeticPoint                 location = new GeodeticPoint(latitude, longitude, height);
        final ViennaACoefficients           a        = model.getA(location, date);
        final PressureTemperatureHumidity   pth      = model.getWeatherParameters(location, date);

        Assertions.assertEquals(0.001264669,           a.getAh(),                   1.0e-9);
        Assertions.assertEquals(0.000572557,           a.getAw(),                   1.0e-9);
        Assertions.assertEquals(273.15 + 22.121274184, pth.getTemperature(),        1.0e-9);
        Assertions.assertEquals(1002.555031902,        TroposphericModelUtils.HECTO_PASCAL.fromSI(pth.getPressure()),           1.0e-9);
        Assertions.assertEquals(15.625300653,          TroposphericModelUtils.HECTO_PASCAL.fromSI(pth.getWaterVaporPressure()), 1.0e-9);

    }

    @Test
    public void testMatlabRef1() throws IOException, URISyntaxException {

        Utils.setDataRoot("regular-data");

        // reference output obtained by running TU-Wien https://vmf.geo.tuwien.ac.at/codes/gpt2.m matlab script
        final double latitude  = FastMath.toRadians(77.5);
        final double longitude = FastMath.toRadians(137.5);
        final double height    = 0.0;
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(7395 * Constants.JULIAN_DAY);
        final URL url = GlobalPressureTemperature2Test.class.getClassLoader().getResource("gpt-grid/gpt2_5.grd");
        final GlobalPressureTemperature2 model =
                        new GlobalPressureTemperature2(new DataSource(url.toURI()),
                                                       DataContext.getDefault().getTimeScales());

        final GeodeticPoint                 location = new GeodeticPoint(latitude, longitude, height);
        final ViennaACoefficients           a        = model.getA(location, date);
        final PressureTemperatureHumidity   pth      = model.getWeatherParameters(location, date);

        Assertions.assertEquals(0.001174547,           a.getAh(),                   1.0e-9);
        Assertions.assertEquals(0.000579181,           a.getAw(),                   1.0e-9);
        Assertions.assertEquals(273.15 - 19.053324536, pth.getTemperature(),        1.0e-9);
        Assertions.assertEquals(1020.047182276,        TroposphericModelUtils.HECTO_PASCAL.fromSI(pth.getPressure()),           1.0e-9);
        Assertions.assertEquals(1.189457053,           TroposphericModelUtils.HECTO_PASCAL.fromSI(pth.getWaterVaporPressure()), 1.0e-9);

    }

    @Test
    public void testMatlabRef2() throws IOException, URISyntaxException {

        Utils.setDataRoot("regular-data");

        // reference output obtained by running TU-Wien https://vmf.geo.tuwien.ac.at/codes/gpt2.m matlab script
        final double latitude  = FastMath.toRadians(80.0);
        final double longitude = FastMath.toRadians(120.0);
        final double height    = 100.0;
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(7395 * Constants.JULIAN_DAY);
        final URL url = GlobalPressureTemperature2Test.class.getClassLoader().getResource("gpt-grid/gpt2_5.grd");
        final GlobalPressureTemperature2 model =
                        new GlobalPressureTemperature2(new DataSource(url.toURI()),
                                                       DataContext.getDefault().getTimeScales());

        final GeodeticPoint                 location = new GeodeticPoint(latitude, longitude, height);
        final ViennaACoefficients           a        = model.getA(location, date);
        final PressureTemperatureHumidity   pth      = model.getWeatherParameters(location, date);

        Assertions.assertEquals(0.001172082,           a.getAh(),                   1.0e-9);
        Assertions.assertEquals(0.000582310,           a.getAw(),                   1.0e-9);
        Assertions.assertEquals(273.15 - 20.146149484, pth.getTemperature(),        1.0e-9);
        Assertions.assertEquals(1005.468561351,        TroposphericModelUtils.HECTO_PASCAL.fromSI(pth.getPressure()),           1.0e-9);
        Assertions.assertEquals(1.002152409,           TroposphericModelUtils.HECTO_PASCAL.fromSI(pth.getWaterVaporPressure()), 1.0e-9);

    }

    @Test
    public void testFieldMatlabRef0() throws IOException, URISyntaxException {
        doTestFieldMatlabRef0(Binary64Field.getInstance());
    }

    protected <T extends CalculusFieldElement<T>> void doTestFieldMatlabRef0(final Field<T> field)
        throws IOException, URISyntaxException {

        Utils.setDataRoot("regular-data");

        // reference output obtained by running TU-Wien https://vmf.geo.tuwien.ac.at/codes/gpt2.m matlab script
        final T latitude  = FastMath.toRadians(field.getZero().newInstance(48.20));
        final T longitude = FastMath.toRadians(field.getZero().newInstance(16.37));
        final T height    = field.getZero().newInstance(156.0);
        final FieldAbsoluteDate<T> date = FieldAbsoluteDate.getJ2000Epoch(field).shiftedBy(4596.5 * Constants.JULIAN_DAY);
        final URL url = GlobalPressureTemperature2Test.class.getClassLoader().getResource("gpt-grid/gpt2_5.grd");
        final GlobalPressureTemperature2 model =
                        new GlobalPressureTemperature2(new DataSource(url.toURI()),
                                                       DataContext.getDefault().getTimeScales());

        final FieldGeodeticPoint<T>                 location = new FieldGeodeticPoint<>(latitude, longitude, height);
        final FieldViennaACoefficients<T>           a        = model.getA(location, date);
        final FieldPressureTemperatureHumidity<T>   pth      = model.getWeatherParameters(location, date);

        Assertions.assertEquals(0.001264669,           a.getAh().getReal(),                   1.0e-9);
        Assertions.assertEquals(0.000572557,           a.getAw().getReal(),                   1.0e-9);
        Assertions.assertEquals(273.15 + 22.121274184, pth.getTemperature().getReal(),        1.0e-9);
        Assertions.assertEquals(1002.555031902,        TroposphericModelUtils.HECTO_PASCAL.fromSI(pth.getPressure()).getReal(),           1.0e-9);
        Assertions.assertEquals(15.625300653,          TroposphericModelUtils.HECTO_PASCAL.fromSI(pth.getWaterVaporPressure()).getReal(), 1.0e-9);

    }

    @Test
    public void testFieldMatlabRef1() throws IOException, URISyntaxException {
        doTestFieldMatlabRef1(Binary64Field.getInstance());
    }

    protected <T extends CalculusFieldElement<T>> void doTestFieldMatlabRef1(final Field<T> field)
        throws IOException, URISyntaxException {

        Utils.setDataRoot("regular-data");

        // reference output obtained by running TU-Wien https://vmf.geo.tuwien.ac.at/codes/gpt2.m matlab script
        final T latitude  = FastMath.toRadians(field.getZero().newInstance(77.5));
        final T longitude = FastMath.toRadians(field.getZero().newInstance(137.5));
        final T height    = field.getZero().newInstance(0.0);
        final FieldAbsoluteDate<T> date = FieldAbsoluteDate.getJ2000Epoch(field).shiftedBy(7395 * Constants.JULIAN_DAY);
        final URL url = GlobalPressureTemperature2Test.class.getClassLoader().getResource("gpt-grid/gpt2_5.grd");
        final GlobalPressureTemperature2 model =
                        new GlobalPressureTemperature2(new DataSource(url.toURI()),
                                                       DataContext.getDefault().getTimeScales());

        final FieldGeodeticPoint<T>                 location = new FieldGeodeticPoint<>(latitude, longitude, height);
        final FieldViennaACoefficients<T>           a        = model.getA(location, date);
        final FieldPressureTemperatureHumidity<T>   pth      = model.getWeatherParameters(location, date);

        Assertions.assertEquals(0.001174547,           a.getAh().getReal(),                   1.0e-9);
        Assertions.assertEquals(0.000579181,           a.getAw().getReal(),                   1.0e-9);
        Assertions.assertEquals(273.15 - 19.053324536, pth.getTemperature().getReal(),        1.0e-9);
        Assertions.assertEquals(1020.047182276,        TroposphericModelUtils.HECTO_PASCAL.fromSI(pth.getPressure()).getReal(),           1.0e-9);
        Assertions.assertEquals(1.189457053,           TroposphericModelUtils.HECTO_PASCAL.fromSI(pth.getWaterVaporPressure()).getReal(), 1.0e-9);

    }

    @Test
    public void testFieldMatlabRef2() throws IOException, URISyntaxException {
        doTestFieldMatlabRef2(Binary64Field.getInstance());
    }

    protected <T extends CalculusFieldElement<T>> void doTestFieldMatlabRef2(final Field<T> field)
        throws IOException, URISyntaxException {

        Utils.setDataRoot("regular-data");

        // reference output obtained by running TU-Wien https://vmf.geo.tuwien.ac.at/codes/gpt2_5.m matlab script
        final T latitude  = FastMath.toRadians(field.getZero().newInstance(80.0));
        final T longitude = FastMath.toRadians(field.getZero().newInstance(120.0));
        final T height    = field.getZero().newInstance(100.0);
        final FieldAbsoluteDate<T> date = FieldAbsoluteDate.getJ2000Epoch(field).shiftedBy(7395 * Constants.JULIAN_DAY);
        final URL url = GlobalPressureTemperature2Test.class.getClassLoader().getResource("gpt-grid/gpt2_5.grd");
        final GlobalPressureTemperature2 model =
                        new GlobalPressureTemperature2(new DataSource(url.toURI()),
                                                       DataContext.getDefault().getTimeScales());

        final FieldGeodeticPoint<T>                 location = new FieldGeodeticPoint<>(latitude, longitude, height);
        final FieldViennaACoefficients<T>           a        = model.getA(location, date);
        final FieldPressureTemperatureHumidity<T>   pth      = model.getWeatherParameters(location, date);

        Assertions.assertEquals(0.001172082,           a.getAh().getReal(),                   1.0e-9);
        Assertions.assertEquals(0.000582310,           a.getAw().getReal(),                   1.0e-9);
        Assertions.assertEquals(273.15 - 20.146149484, pth.getTemperature().getReal(),        1.0e-9);
        Assertions.assertEquals(1005.468561351,        TroposphericModelUtils.HECTO_PASCAL.fromSI(pth.getPressure()).getReal(),           1.0e-9);
        Assertions.assertEquals(1.002152409,           TroposphericModelUtils.HECTO_PASCAL.fromSI(pth.getWaterVaporPressure()).getReal(), 1.0e-9);

    }

    @Test
    public void testEquality() throws IOException, URISyntaxException {
        doTestEquality("gpt-grid/gpt2_5.grd");
    }

    @Test
    public void testEqualityLoadingGpt2w() throws IOException, URISyntaxException {
        doTestEquality("gpt-grid/gpt2_5w.grd");
    }

    @Test
    public void testEqualityLoadingGpt3() throws IOException, URISyntaxException {
        doTestEquality("gpt-grid/gpt3_5.grd");
    }

    private void doTestEquality(final String resourceName) throws IOException, URISyntaxException {

        Utils.setDataRoot("regular-data");

        // Commons parameters
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(4596.5 * Constants.JULIAN_DAY);
        final double latitude   = FastMath.toRadians(45.0);
        final double height     = 0.0;

        final URL url = GlobalPressureTemperature2Test.class.getClassLoader().getResource(resourceName);
        GlobalPressureTemperature2 model = new GlobalPressureTemperature2(new DataSource(url.toURI()),
                                                                          DataContext.getDefault().getTimeScales());

        // Test longitude = 181° and longitude = -179°
        GeodeticPoint               location1 = new GeodeticPoint(latitude, FastMath.toRadians(181.0), height);
        ViennaACoefficients         a1        = model.getA(location1, date);
        PressureTemperatureHumidity pth1      = model.getWeatherParameters(location1, date);
        GeodeticPoint               location2 = new GeodeticPoint(latitude, FastMath.toRadians(-179.0), height);
        ViennaACoefficients         a2        = model.getA(location2, date);
        PressureTemperatureHumidity pth2      = model.getWeatherParameters(location2, date);

        Assertions.assertEquals(pth1.getTemperature(),        pth2.getTemperature(),        epsilon);
        Assertions.assertEquals(pth1.getPressure(),           pth2.getPressure(),           epsilon);
        Assertions.assertEquals(pth1.getWaterVaporPressure(), pth2.getWaterVaporPressure(), epsilon);
        Assertions.assertEquals(a1.getAh(),                   a2.getAh(),                   epsilon);
        Assertions.assertEquals(a1.getAw(),                   a2.getAw(),                   epsilon);

        // Test longitude = 180° and longitude = -180°
        location1 = new GeodeticPoint(latitude, FastMath.toRadians(180.0), height);
        a1        = model.getA(location1, date);
        pth1      = model.getWeatherParameters(location1, date);
        location2 = new GeodeticPoint(latitude, FastMath.toRadians(-180.0), height);
        a2        = model.getA(location2, date);
        pth2      = model.getWeatherParameters(location2, date);

        Assertions.assertEquals(pth1.getTemperature(),        pth2.getTemperature(),        epsilon);
        Assertions.assertEquals(pth1.getPressure(),           pth2.getPressure(),           epsilon);
        Assertions.assertEquals(pth1.getWaterVaporPressure(), pth2.getWaterVaporPressure(), epsilon);
        Assertions.assertEquals(a1.getAh(),                   a2.getAh(),                   epsilon);
        Assertions.assertEquals(a1.getAw(),                   a2.getAw(),                   epsilon);

        // Test longitude = 0° and longitude = 360°
        location1 = new GeodeticPoint(latitude, FastMath.toRadians(0.0), height);
        a1        = model.getA(location1, date);
        pth1      = model.getWeatherParameters(location1, date);
        location2 = new GeodeticPoint(latitude, FastMath.toRadians(360.0), height);
        a2        = model.getA(location2, date);
        pth2      = model.getWeatherParameters(location2, date);

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
            new GlobalPressureTemperature2(new DataSource(url.toURI()),
                                           DataContext.getDefault().getTimeScales());
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
            new GlobalPressureTemperature2(new DataSource(url.toURI()),
                                           DataContext.getDefault().getTimeScales());
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
            new GlobalPressureTemperature2(new DataSource(url.toURI()),
                                           DataContext.getDefault().getTimeScales());
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
            new GlobalPressureTemperature2(new DataSource(url.toURI()),
                                           DataContext.getDefault().getTimeScales());
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
            new GlobalPressureTemperature2(new DataSource(url.toURI()),
                                           DataContext.getDefault().getTimeScales());
            Assertions.fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
            Assertions.assertEquals(4, ((Integer) oe.getParts()[0]).intValue());
            Assertions.assertTrue(((String) oe.getParts()[1]).endsWith(fileName));
        }

    }

}
