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
import org.hipparchus.util.Precision;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateTimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.time.UTCScale;

public class KlobucharModelTest {

    private static double epsilon = 1e-6;

    /** ionospheric model. */
    private KlobucharIonoModel model;

    private UTCScale utc;

    @Before
    public void setUp() throws Exception {
        // Navigation message data
        // .3820D-07   .1490D-07  -.1790D-06   .0000D-00          ION ALPHA
        // .1430D+06   .0000D+00  -.3280D+06   .1130D+06          ION BETA
        model = new KlobucharIonoModel(new double[]{.3820e-07, .1490e-07, -.1790e-06,0},
                                       new double[]{.1430e+06, 0, -.3280e+06, .1130e+06});

        Utils.setDataRoot("regular-data");
        utc = TimeScalesFactory.getUTC();
    }

    @After
    public void tearDown() {
        utc = null;
    }

    @Test
    public void testDelay() {
        final double latitude = FastMath.toRadians(45);
        final double longitude = FastMath.toRadians(2);
        final double altitude = 500;
        final double elevation = 70.;
        final double azimuth = 10.;

        final AbsoluteDate date = new AbsoluteDate();

        final GeodeticPoint geo = new GeodeticPoint(latitude, longitude, altitude);

        double delayMeters = model.pathDelay(date, geo,
                                                      FastMath.toRadians(elevation), FastMath.toRadians(azimuth));

        Assert.assertTrue(Precision.compareTo(delayMeters, 12., epsilon) < 0);
        Assert.assertTrue(Precision.compareTo(delayMeters, 0., epsilon) > 0);
    }

    @Test
    public void compareExpectedValue() throws IllegalArgumentException, OrekitException {
        final double latitude = FastMath.toRadians(40);
        final double longitude = FastMath.toRadians(-100);
        final double altitude = 0.;
        final double elevation = 20.;
        final double azimuth = 210.;

        final AbsoluteDate date = new AbsoluteDate(new DateTimeComponents(2000, 1, 1,
                                                                          20, 45, 0),
                                                                          utc);

        final GeodeticPoint geo = new GeodeticPoint(latitude, longitude, altitude);

        final double delayMeters = model.pathDelay(date, geo,
                                                            FastMath.toRadians(elevation),
                                                            FastMath.toRadians(azimuth));

        Assert.assertEquals(23.784, delayMeters, 0.001);
    }
}


