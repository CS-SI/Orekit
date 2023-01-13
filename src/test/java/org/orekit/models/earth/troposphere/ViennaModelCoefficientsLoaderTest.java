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
package org.orekit.models.earth.troposphere;

import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.DateTimeComponents;

public class ViennaModelCoefficientsLoaderTest {

    private static double epsilon = 5.0e-16;

    @Test
    /**
     * Regular test for 19th of November 2018 with Vienna 1 model
     * @throws OrekitException if file does not exist or data cannot be read properly
     */
    public void testRegularFileVienna1() {

        Utils.setDataRoot("vmf1-tropospheric-coefficients");

        final double latitude  = FastMath.toRadians(14.0);
        final double longitude = FastMath.toRadians(67.5);
        ViennaModelCoefficientsLoader tropoLoader = new ViennaModelCoefficientsLoader(latitude, longitude, ViennaModelType.VIENNA_ONE);
        DateTimeComponents dateTimeComponents = new DateTimeComponents(2018, 11, 19, 18, 0, 0.0);
        tropoLoader.loadViennaCoefficients(dateTimeComponents);

        final double a[]       = tropoLoader.getA();
        final double delays[]  = tropoLoader.getZenithDelay();

        Assertions.assertEquals(0.00127935, a[0], epsilon);
        Assertions.assertEquals(0.00064084, a[1], epsilon);

        Assertions.assertEquals(2.3131, delays[0], epsilon);
        Assertions.assertEquals(0.3086, delays[1], epsilon);

        Assertions.assertEquals("VMFG_20181119.H18", tropoLoader.getSupportedNames());
    }

    @Test
    /**
     * Regular test for 25th of November 2018 with Vienna 3 model
     * @throws OrekitException if file does not exist or data cannot be read properly
     */
    public void testRegularFile5x5Vienna3() {

        Utils.setDataRoot("vmf3-5x5-tropospheric-coefficients");

        final double latitude  = FastMath.toRadians(77.5);
        final double longitude = FastMath.toRadians(2.5);
        ViennaModelCoefficientsLoader tropoLoader = new ViennaModelCoefficientsLoader(latitude, longitude, ViennaModelType.VIENNA_THREE);
        DateTimeComponents dateTimeComponents = new DateTimeComponents(2018, 11, 25, 0, 0, 0.0);
        tropoLoader.loadViennaCoefficients(dateTimeComponents);

        final double a[]       = tropoLoader.getA();
        final double delays[]  = tropoLoader.getZenithDelay();

        Assertions.assertEquals(0.00117002, a[0], epsilon);
        Assertions.assertEquals(0.00045484, a[1], epsilon);

        Assertions.assertEquals(2.3203, delays[0], epsilon);
        Assertions.assertEquals(0.0191, delays[1], epsilon);

        Assertions.assertEquals("VMF3_20181125.H00", tropoLoader.getSupportedNames());
    }

    @Test
    /**
     * Regular test for 25th of November 2018 with Vienna 3 model
     * @throws OrekitException if file does not exist or data cannot be read properly
     */
    public void testRegularFile1x1Vienna3() {

        Utils.setDataRoot("vmf3-1x1-tropospheric-coefficients");

        final double latitude  = FastMath.toRadians(19.5);
        final double longitude = FastMath.toRadians(276.5);
        ViennaModelCoefficientsLoader tropoLoader = new ViennaModelCoefficientsLoader(latitude, longitude, ViennaModelType.VIENNA_THREE);
        DateTimeComponents dateTimeComponents = new DateTimeComponents(2018, 11, 25, 0, 0, 0.0);
        tropoLoader.loadViennaCoefficients(dateTimeComponents);

        final double a[]       = tropoLoader.getA();
        final double delays[]  = tropoLoader.getZenithDelay();

        Assertions.assertEquals(0.00127606, a[0], epsilon);
        Assertions.assertEquals(0.00056388, a[1], epsilon);

        Assertions.assertEquals(2.3117, delays[0], epsilon);
        Assertions.assertEquals(0.2239, delays[1], epsilon);

        Assertions.assertEquals("VMF3_20181125.H00", tropoLoader.getSupportedNames());
    }

    @Test
    public void testEquality() {

        // Commons parameters
        Utils.setDataRoot("vmf3-1x1-tropospheric-coefficients");
        DateTimeComponents dateTimeComponents = new DateTimeComponents(2018, 11, 25, 0, 0, 0.0);

        final double latitude   = FastMath.toRadians(45.0);

        double longitude1;
        ViennaModelCoefficientsLoader model1;

        double longitude2;
        ViennaModelCoefficientsLoader model2;

        // Test longitude = 181° and longitude = -179°
        longitude1 = FastMath.toRadians(181.0);
        longitude2 = FastMath.toRadians(-179.0);

        model1 = new ViennaModelCoefficientsLoader(latitude, longitude1, ViennaModelType.VIENNA_THREE);
        model2 = new ViennaModelCoefficientsLoader(latitude, longitude2, ViennaModelType.VIENNA_THREE);

        model1.loadViennaCoefficients(dateTimeComponents);
        model2.loadViennaCoefficients(dateTimeComponents);

        Assertions.assertEquals(model1.getA()[0],           model2.getA()[0],           epsilon);
        Assertions.assertEquals(model1.getA()[1],           model2.getA()[1],           epsilon);
        Assertions.assertEquals(model1.getZenithDelay()[0], model2.getZenithDelay()[0], epsilon);
        Assertions.assertEquals(model1.getZenithDelay()[1], model2.getZenithDelay()[1], epsilon);

        // Test longitude = 180° and longitude = -180°
        longitude1 = FastMath.toRadians(180.0);
        longitude2 = FastMath.toRadians(-180.0);

        model1 = new ViennaModelCoefficientsLoader(latitude, longitude1, ViennaModelType.VIENNA_THREE);
        model2 = new ViennaModelCoefficientsLoader(latitude, longitude2, ViennaModelType.VIENNA_THREE);

        model1.loadViennaCoefficients(dateTimeComponents);
        model2.loadViennaCoefficients(dateTimeComponents);

        Assertions.assertEquals(model1.getA()[0],           model2.getA()[0],           epsilon);
        Assertions.assertEquals(model1.getA()[1],           model2.getA()[1],           epsilon);
        Assertions.assertEquals(model1.getZenithDelay()[0], model2.getZenithDelay()[0], epsilon);
        Assertions.assertEquals(model1.getZenithDelay()[1], model2.getZenithDelay()[1], epsilon);

        // Test longitude = 0° and longitude = 360°
        longitude1 = FastMath.toRadians(0.0);
        longitude2 = FastMath.toRadians(360.0);

        model1 = new ViennaModelCoefficientsLoader(latitude, longitude1, ViennaModelType.VIENNA_THREE);
        model2 = new ViennaModelCoefficientsLoader(latitude, longitude2, ViennaModelType.VIENNA_THREE);

        model1.loadViennaCoefficients(dateTimeComponents);
        model2.loadViennaCoefficients(dateTimeComponents);

        Assertions.assertEquals(model1.getA()[0],           model2.getA()[0],           epsilon);
        Assertions.assertEquals(model1.getA()[1],           model2.getA()[1],           epsilon);
        Assertions.assertEquals(model1.getZenithDelay()[0], model2.getZenithDelay()[0], epsilon);
        Assertions.assertEquals(model1.getZenithDelay()[1], model2.getZenithDelay()[1], epsilon);

    }

    @Test
    /**
     * Test of a corrupted file with improper data.
     */
    public void testCorruptedFileBadData() {

        final double latitude  = FastMath.toRadians(14.0);
        final double longitude = FastMath.toRadians(67.5);

        Utils.setDataRoot("vmf1-tropospheric-coefficients");
        final String fileName = "corrupted-bad-data-VMFG_20181119.H18";
        ViennaModelCoefficientsLoader tropoLoader =
                        new ViennaModelCoefficientsLoader(fileName, latitude, longitude, ViennaModelType.VIENNA_ONE);

        try {
            tropoLoader.loadViennaCoefficients();
            Assertions.fail("An exception should have been thrown");

        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
        }
    }

    @Test
    /**
     * Test for a file that cannot be found
     */
    public void testAbsentFile() {

        Utils.setDataRoot("vmf1-tropospheric-coefficients");
        final double latitude  = FastMath.toRadians(14.0);
        final double longitude = FastMath.toRadians(67.5);
        ViennaModelCoefficientsLoader tropoLoader = new ViennaModelCoefficientsLoader(latitude, longitude, ViennaModelType.VIENNA_ONE);
        DateTimeComponents dateTimeComponents = new DateTimeComponents(2018, 11, 19, 1, 0, 0);

        try {
            tropoLoader.loadViennaCoefficients(dateTimeComponents);
            Assertions.fail("An exception should have been thrown");

        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.VIENNA_ACOEF_OR_ZENITH_DELAY_NOT_AVAILABLE_FOR_DATE,
                                oe.getSpecifier());
            Assertions.assertEquals(dateTimeComponents.toString(),
                                (String) oe.getParts()[0]);
        }
    }

}
