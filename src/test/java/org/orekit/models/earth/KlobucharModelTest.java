/* Copyright 2002-2015 CS Systèmes d'Information
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

import org.apache.commons.math3.util.Precision;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.time.AbsoluteDate;

public class KlobucharModelTest {
    /** utility constant to convert from radians to degrees. */
    private static double RADIANS_TO_DEGREES = 180. / Math.PI;
    /** utility constant to convert from degrees to radians. */
    private static double DEGREES_TO_RADIANS = Math.PI / 180.;
    
    private static double epsilon = 1e-6;
    
    /** ionospheric model. */
    KlobucharIonoModel model;
    
    @Before
    public void setUp() throws Exception {
        // Navigation message data
        // .3820D-07   .1490D-07  -.1790D-06   .0000D-00          ION ALPHA           
        // .1430D+06   .0000D+00  -.3280D+06   .1130D+06          ION BETA              
        model = new KlobucharIonoModel(new double[]{.3820e-07, .1490e-07, -.1790e-06,0},
                                                          new double[]{.1430e+06, 0, -.3280e+06, .1130e+06});

    }


    @Test
    public void testDelay() {
        final double latitude = 45 * DEGREES_TO_RADIANS; 
        final double longitude = 2 * DEGREES_TO_RADIANS;
        final double altitude = 500;
        final double elevation = 70d;
        final double azimuth = 10d;

        final AbsoluteDate date = new AbsoluteDate();

        final GeodeticPoint geo = new GeodeticPoint(latitude, longitude, altitude);
                
        double delayMeters = model.calculatePathDelay(date, geo, elevation, azimuth);

        Assert.assertTrue(Precision.compareTo(delayMeters, 10d, epsilon) < 0);
        Assert.assertTrue(Precision.compareTo(delayMeters, 0d, epsilon) > 0);
    }    
}


