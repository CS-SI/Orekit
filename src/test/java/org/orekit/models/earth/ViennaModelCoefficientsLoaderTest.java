/* Copyright 2002-2018 CS Systèmes d'Information
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

import org.junit.Assert;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.DateTimeComponents;

public class ViennaModelCoefficientsLoaderTest {

    @Test
    /**
     * Regular test for 19th of November 2018 with Vienna 1 model
     * @throws OrekitException if file does not exist or data cannot be read properly
     */
    public void testRegularFileVienna1() {

        Utils.setDataRoot("vmf1-tropospheric-coefficients");
        
        final double latitude  = 14.0;
        final double longitude = 67.5;
        ViennaModelCoefficientsLoader tropoLoader = new ViennaModelCoefficientsLoader(latitude, longitude, ViennaModelType.VIENNA_ONE);
        DateTimeComponents dateTimeComponents = new DateTimeComponents(2018, 11, 19, 18, 0, 0.0);
        tropoLoader.loadViennaCoefficients(dateTimeComponents);
        
        final double a[]       = tropoLoader.getA();
        final double delays[]  = tropoLoader.getZenithDelay();
        
        Assert.assertEquals(0.00127935, a[0], 1e-16);
        Assert.assertEquals(0.00064084, a[1], 1e-16);
        
        Assert.assertEquals(2.3131, delays[0], 1e-16);
        Assert.assertEquals(0.3086, delays[1], 1e-16);
    }

    @Test
    /**
     * Regular test for 25th of November 2018 with Vienna 3 model
     * @throws OrekitException if file does not exist or data cannot be read properly
     */
    public void testRegularFile5x5Vienna3() {

        Utils.setDataRoot("vmf3-5x5-tropospheric-coefficients");
        
        final double latitude  = 77.5;
        final double longitude = 2.5;
        ViennaModelCoefficientsLoader tropoLoader = new ViennaModelCoefficientsLoader(latitude, longitude, ViennaModelType.VIENNA_THREE);
        DateTimeComponents dateTimeComponents = new DateTimeComponents(2018, 11, 25, 0, 0, 0.0);
        tropoLoader.loadViennaCoefficients(dateTimeComponents);
        
        final double a[]       = tropoLoader.getA();
        final double delays[]  = tropoLoader.getZenithDelay();
        
        Assert.assertEquals(0.00117002, a[0], 1e-16);
        Assert.assertEquals(0.00045484, a[1], 1e-16);
        
        Assert.assertEquals(2.3203, delays[0], 1e-16);
        Assert.assertEquals(0.0191, delays[1], 1e-16);
    }

    @Test
    /**
     * Regular test for 25th of November 2018 with Vienna 3 model
     * @throws OrekitException if file does not exist or data cannot be read properly
     */
    public void testRegularFile1x1Vienna3() {

        Utils.setDataRoot("vmf3-1x1-tropospheric-coefficients");
        
        final double latitude  = 19.5;
        final double longitude = 276.5;
        ViennaModelCoefficientsLoader tropoLoader = new ViennaModelCoefficientsLoader(latitude, longitude, ViennaModelType.VIENNA_THREE);
        DateTimeComponents dateTimeComponents = new DateTimeComponents(2018, 11, 25, 0, 0, 0.0);
        tropoLoader.loadViennaCoefficients(dateTimeComponents);
        
        final double a[]       = tropoLoader.getA();
        final double delays[]  = tropoLoader.getZenithDelay();
        
        Assert.assertEquals(0.00127606, a[0], 1e-16);
        Assert.assertEquals(0.00056388, a[1], 1e-16);
        
        Assert.assertEquals(2.3117, delays[0], 1e-16);
        Assert.assertEquals(0.2239, delays[1], 1e-16);
    }

    @Test
    /**
     * Test of a corrupted file with improper data.
     */
    public void testCorruptedFileBadData() {
        
        final double latitude  = 14.0;
        final double longitude = 67.5;

        Utils.setDataRoot("vmf1-tropospheric-coefficients");
        final String fileName = "corrupted-bad-data-VMFG_20181119.H18";
        ViennaModelCoefficientsLoader tropoLoader = 
                        new ViennaModelCoefficientsLoader(fileName, latitude, longitude, ViennaModelType.VIENNA_ONE);
                        
        try {
            tropoLoader.loadViennaCoefficients();
            Assert.fail("An exception should have been thrown");
            
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE, oe.getSpecifier());
        }
    }
    
    @Test
    /**
     * Test for a file that cannot be found
     */
    public void testAbsentFile() {
        
        Utils.setDataRoot("vmf1-tropospheric-coefficients");
        final double latitude  = 14.0;
        final double longitude = 67.5;
        ViennaModelCoefficientsLoader tropoLoader = new ViennaModelCoefficientsLoader(latitude, longitude, ViennaModelType.VIENNA_ONE);
        DateTimeComponents dateTimeComponents = new DateTimeComponents(2018, 11, 19, 1, 0, 0);
        
        try {
            tropoLoader.loadViennaCoefficients(dateTimeComponents);
            Assert.fail("An exception should have been thrown");
            
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.VIENNA_ACOEF_OR_ZENITH_DELAY_NOT_AVAILABLE_FOR_DATE,
                                oe.getSpecifier());
            Assert.assertEquals(dateTimeComponents.toString(),
                                (String) oe.getParts()[0]);
        }
    }

}
