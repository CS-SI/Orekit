/* Copyright 2002-2008 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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
package org.orekit.forces.drag;


import java.text.ParseException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.SolarInputs97to05;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.bodies.SolarSystemBody;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;

public class DTM2000Test {

    @Test
    public void testWithOriginalTestsCases() throws OrekitException, ParseException {

        Frame itrf = FramesFactory.getITRF2005(true);
        CelestialBody sun = SolarSystemBody.getSun();
        OneAxisEllipsoid earth = new OneAxisEllipsoid(6378136.460, 1.0 / 298.257222101, itrf);
        SolarInputs97to05 in = SolarInputs97to05.getInstance();
        earth.setAngularThreshold(1e-10);
        DTM2000 atm = new DTM2000(in, sun, earth, itrf);
        double roTestCase;
        double tzTestCase;
        double tinfTestCase;
        double myRo;

        // Inputs :
//      alt=800.
//      lat=40.
//      day=185.
//      hl=16.
//      xlon=0.
//      fm(1)=150.
//      f(1) =fm(1)
//      fm(2)=0.
//      f(2)=0.
//      akp(1)=0.
//      akp(2)=0.
//      akp(3)=0.
//      akp(4)=0.

        // Outputs :
        roTestCase = 1.8710001353820e-17 * 1000;
        tzTestCase = 1165.4839828984;
        tinfTestCase = 1165.4919505608;

        // Computation and results
        myRo = atm.getDensity(185, 800*1000, 0, Math.toRadians(40), 16*Math.PI/12, 150, 150, 0, 0);
        Assert.assertEquals(0, (roTestCase -myRo)/roTestCase, 1e-14);
        Assert.assertEquals(0, (tzTestCase-atm.getT())/tzTestCase, 1e-13);
        Assert.assertEquals(0, (tinfTestCase-atm.getTinf())/tinfTestCase, 1e-13);

//      IDEM., day=275

        roTestCase=    2.8524195214905e-17* 1000;
        tzTestCase=    1157.1872001392;
        tinfTestCase=    1157.1933514185;

        myRo = atm.getDensity(275, 800*1000, 0, Math.toRadians(40), 16*Math.PI/12, 150, 150, 0, 0);
        Assert.assertEquals(0, (roTestCase -myRo)/roTestCase, 1e-14);
        Assert.assertEquals(0, (tzTestCase-atm.getT())/tzTestCase, 1e-13);
        Assert.assertEquals(0, (tinfTestCase-atm.getTinf())/tinfTestCase, 1e-13);

//      IDEM., day=355

        roTestCase=    1.7343324462212e-17* 1000;
        tzTestCase=    1033.0277846356;
        tinfTestCase=    1033.0282703200;

        myRo = atm.getDensity(355, 800*1000, 0, Math.toRadians(40), 16*Math.PI/12, 150, 150, 0, 0);
        Assert.assertEquals(0, (roTestCase -myRo)/roTestCase, 2e-14);
        Assert.assertEquals(0, (tzTestCase-atm.getT())/tzTestCase, 1e-13);
        Assert.assertEquals(0, (tinfTestCase-atm.getTinf())/tinfTestCase, 1e-13);
//      IDEM., day=85

        roTestCase=    2.9983740796297e-17* 1000;
        tzTestCase=    1169.5405086196;
        tinfTestCase=    1169.5485768345;

        myRo = atm.getDensity(85, 800*1000, 0, Math.toRadians(40), 16*Math.PI/12, 150, 150, 0, 0);
        Assert.assertEquals(0, (roTestCase -myRo)/roTestCase, 1e-14);
        Assert.assertEquals(0, (tzTestCase-atm.getT())/tzTestCase, 1e-13);
        Assert.assertEquals(0, (tinfTestCase-atm.getTinf())/tinfTestCase, 1e-13);


//      alt=500.
//      lat=-70.      NB: the subroutine requires latitude in rad
//      day=15.
//      hl=16.        NB: the subroutine requires local time in rad (0hr=0 rad)
//      xlon=0.
//      fm(1)=70.
//      f(1) =fm(1)
//      fm(2)=0.
//      f(2)=0.
//      akp(1)=0.
//      akp(2)=0.
//      akp(3)=0.
//      akp(4)=0.
//      ro=    1.3150282384722D-16
//      tz=    793.65487014559
//      tinf=    793.65549802348
//        roTestCase =    1.3150282384722E-16;
//        tzTestCase=    793.65487014559;
//        tinfTestCase=    793.65549802348;

        atm.getDensity(15, 500*1000, 0, Math.toRadians(-70), 16*Math.PI/12, 70, 70, 0, 0);

//      IDEM., alt=800.
//      ro=    1.9556768571305D-18
//      tz=    793.65549797919
//      tinf=    793.65549802348
        atm.getDensity(15, 800*1000, 0, Math.toRadians(-70), 16*Math.PI/12, 70, 70, 0, 0);

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
