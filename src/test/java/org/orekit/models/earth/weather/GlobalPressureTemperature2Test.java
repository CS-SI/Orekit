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
package org.orekit.models.earth.weather;

import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.gravity.potential.GRGSFormatReader;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.frames.FramesFactory;
import org.orekit.models.earth.Geoid;
import org.orekit.models.earth.ReferenceEllipsoid;
import org.orekit.models.earth.troposphere.TroposphericModelUtils;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.IERSConventions;

public class GlobalPressureTemperature2Test {

    private static double epsilon = 1.0e-12;

    @Test
    public void testWeatherParameters() {

        Utils.setDataRoot("regular-data:potential:gpt2-grid");
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));

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
        final Geoid geoid = new Geoid(GravityFieldFactory.getNormalizedProvider(12, 12),
                                      ReferenceEllipsoid.getWgs84(FramesFactory.getITRF(IERSConventions.IERS_2010, true)));
        final GlobalPressureTemperature2 model =
                        new GlobalPressureTemperature2("gpt2_5_extract.grd", geoid);

        final GeodeticPoint               location = new GeodeticPoint(latitude, longitude, height);
        final double                      a[]      = model.getA(location, date);
        final PressureTemperatureHumidity pth      = model.getWeatherParamerers(location, date);

        Assertions.assertEquals(0.0012647,      a[0],                        1.1e-7);
        Assertions.assertEquals(0.0005726,      a[1],                        8.6e-8);
        Assertions.assertEquals(273.15 + 22.12, pth.getTemperature(),        2.3e-1);
        Assertions.assertEquals(1002.56,        TroposphericModelUtils.HECTO_PASCAL.fromSI(pth.getPressure()),           5.1e-1);
        Assertions.assertEquals(15.63,          TroposphericModelUtils.HECTO_PASCAL.fromSI(pth.getWaterVaporPressure()), 5.0e-2);

    }

    @Test
    public void testEquality() {

        Utils.setDataRoot("regular-data:potential:gpt2-grid");
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));

        // Commons parameters
        final Geoid geoid = new Geoid(GravityFieldFactory.getNormalizedProvider(12, 12),
                                      ReferenceEllipsoid.getWgs84(FramesFactory.getITRF(IERSConventions.IERS_2010, true)));

        final AbsoluteDate date = AbsoluteDate.createMJDDate(56141, 0.0, TimeScalesFactory.getUTC());
        final double latitude   = FastMath.toRadians(45.0);
        final double height     = 0.0;

        GlobalPressureTemperature2 model = new GlobalPressureTemperature2(geoid);

        // Test longitude = 181° and longitude = -179°
        GeodeticPoint               location1 = new GeodeticPoint(latitude, FastMath.toRadians(181.0), height);
        double[]                    a1        = model.getA(location1, date);
        PressureTemperatureHumidity pth1      = model.getWeatherParamerers(location1, date);
        GeodeticPoint               location2 = new GeodeticPoint(latitude, FastMath.toRadians(-179.0), height);
        double[]                    a2        = model.getA(location2, date);
        PressureTemperatureHumidity pth2      = model.getWeatherParamerers(location2, date);

        Assertions.assertEquals(pth1.getTemperature(),        pth2.getTemperature(),        epsilon);
        Assertions.assertEquals(pth1.getPressure(),           pth2.getPressure(),           epsilon);
        Assertions.assertEquals(pth1.getWaterVaporPressure(), pth2.getWaterVaporPressure(), epsilon);
        Assertions.assertEquals(a1[0],                        a2[0],                        epsilon);
        Assertions.assertEquals(a1[1],                        a2[1],                        epsilon);

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
        Assertions.assertEquals(a1[0],                        a2[0],                        epsilon);
        Assertions.assertEquals(a1[1],                        a2[1],                        epsilon);

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
        Assertions.assertEquals(a1[0],                        a2[0],                        epsilon);
        Assertions.assertEquals(a1[1],                        a2[1],                        epsilon);

    }

    @Test
    public void testCorruptedFileBadData() {

        Utils.setDataRoot("regular-data:potential:gpt2-grid");
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));

        // Date is not used here
        final Geoid geoid = new Geoid(GravityFieldFactory.getNormalizedProvider(12, 12),
                                      ReferenceEllipsoid.getWgs84(FramesFactory.getITRF(IERSConventions.IERS_2010, true)));

        final String fileName = "corrupted-bad-data-gpt2_5.grd";
        try {
            new GlobalPressureTemperature2(fileName, geoid);
            Assertions.fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
            Assertions.assertEquals(6, ((Integer) oe.getParts()[0]).intValue());
            Assertions.assertTrue(((String) oe.getParts()[1]).endsWith(fileName));
        }

    }

    @Test
    public void testCorruptedIrregularGrid() {

        Utils.setDataRoot("regular-data:potential:gpt2-grid");
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("grim4s4_gr", true));

        // Date is not used here
        final Geoid geoid = new Geoid(GravityFieldFactory.getNormalizedProvider(12, 12),
                                      ReferenceEllipsoid.getWgs84(FramesFactory.getITRF(IERSConventions.IERS_2010, true)));

        final String fileName = "corrupted-irregular-grid-gpt2_5.grd";
        try {
            new GlobalPressureTemperature2(fileName, geoid);
            Assertions.fail("An exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.IRREGULAR_OR_INCOMPLETE_GRID, oe.getSpecifier());
            Assertions.assertTrue(((String) oe.getParts()[0]).endsWith(fileName));
        }

    }

}