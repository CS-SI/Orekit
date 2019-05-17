/* Copyright 2002-2019 CS Systèmes d'Information
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
package org.orekit.propagation.analytical.gnss;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.gnss.GLONASSAlmanac;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.PVCoordinates;

public class GLONASSAnalyticalPropagatorTest {

    private static GLONASSAlmanac almanac;

    @BeforeClass
    public static void setUpBeforeClass() {
        Utils.setDataRoot("gnss");
        // Reference values for validation are given into Glonass Interface Control Document v1.0 2016
        final double pi = GLONASSOrbitalElements.GLONASS_PI;
        almanac = new GLONASSAlmanac(0, 1, 22, 12, 2007, 33571.625,
                                     -0.293967247009277 * pi,
                                     -0.00012947082519531 * pi,
                                     0.57867431640625 * pi,
                                     0.000432968139648438,
                                     0.01953124999975,
                                     6.103515625e-5,
                                     0.0, 0.0, 0.0);
    }

    @Test
    public void testPerfectValues() {
        final GLONASSAnalyticalPropagator propagator = new GLONASSAnalyticalPropagator.Builder(almanac).build();
        final AbsoluteDate target = new AbsoluteDate(new DateComponents(2007, 12, 23),
                                                     new TimeComponents(51300),
                                                     TimeScalesFactory.getGLONASS());
        final PVCoordinates pvFinal    = propagator.propagateInEcef(target);
        final PVCoordinates pvExpected = new PVCoordinates(new Vector3D(10697116.4874360654,
                                                                        21058292.4241863210,
                                                                        -9635679.33963303405),
                                                           new Vector3D(-0686.100809921691084,
                                                                        -1136.54864124521881,
                                                                        -3249.98587740305799));

        Assert.assertEquals(0.0, pvFinal.getPosition().distance(pvExpected.getPosition()), 1.1e-7);
        Assert.assertEquals(0.0, pvFinal.getVelocity().distance(pvExpected.getVelocity()), 1.1e-5);
    }
}
