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
package org.orekit.forces.drag.atmosphere;


import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.SolarInputs97to05;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.forces.drag.atmosphere.DTM2000;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinatesProvider;

public class DTM2000Test {

    @Test
    public void testWithOriginalTestsCases() throws OrekitException {

        Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        OneAxisEllipsoid earth = new OneAxisEllipsoid(6378136.460, 1.0 / 298.257222101, itrf);
        SolarInputs97to05 in = SolarInputs97to05.getInstance();
        earth.setAngularThreshold(1e-10);
        DTM2000 atm = new DTM2000(in, sun, earth);
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
        myRo = atm.getDensity(185, 800*1000, 0, FastMath.toRadians(40), 16*FastMath.PI/12, 150, 150, 0, 0);
        Assert.assertEquals(roTestCase, myRo , roTestCase * 1e-14);
        Assert.assertEquals(tzTestCase,  atm.getT(), tzTestCase * 1e-13);
        Assert.assertEquals(tinfTestCase, atm.getTinf(), tinfTestCase * 1e-13);

//      IDEM., day=275

        roTestCase=    2.8524195214905e-17* 1000;
        tzTestCase=    1157.1872001392;
        tinfTestCase=    1157.1933514185;

        myRo = atm.getDensity(275, 800*1000, 0, FastMath.toRadians(40), 16*FastMath.PI/12, 150, 150, 0, 0);
        Assert.assertEquals(roTestCase, myRo , roTestCase * 1e-14);
        Assert.assertEquals(tzTestCase,  atm.getT(), tzTestCase * 1e-13);
        Assert.assertEquals(tinfTestCase, atm.getTinf(), tinfTestCase * 1e-13);

//      IDEM., day=355

        roTestCase=    1.7343324462212e-17* 1000;
        tzTestCase=    1033.0277846356;
        tinfTestCase=    1033.0282703200;

        myRo = atm.getDensity(355, 800*1000, 0, FastMath.toRadians(40), 16*FastMath.PI/12, 150, 150, 0, 0);
        Assert.assertEquals(roTestCase, myRo , roTestCase * 2e-14);
        Assert.assertEquals(tzTestCase,  atm.getT(), tzTestCase * 1e-13);
        Assert.assertEquals(tinfTestCase, atm.getTinf(), tinfTestCase * 1e-13);
//      IDEM., day=85

        roTestCase=    2.9983740796297e-17* 1000;
        tzTestCase=    1169.5405086196;
        tinfTestCase=    1169.5485768345;

        myRo = atm.getDensity(85, 800*1000, 0, FastMath.toRadians(40), 16*FastMath.PI/12, 150, 150, 0, 0);
        Assert.assertEquals(roTestCase, myRo , roTestCase * 1e-14);
        Assert.assertEquals(tzTestCase,  atm.getT(), tzTestCase * 1e-13);
        Assert.assertEquals(tinfTestCase, atm.getTinf(), tinfTestCase * 1e-13);


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
        // note that the values above are the ones present in the original fortran source comments
        // however, running this original source (converted to double precision) does
        // not yield the results in the comments, but instead gives the following results
        // as all the other tests cases do behave correctly, we assume the comments are wrong
        // and prefer to ensure we get the same result as the original CODE
        // as we don't have access to any other tests cases, we can't really decide if this is
        // the best approach. Indeed, we are able to get the same results as original fortran
        roTestCase =    1.5699108952425600E-016* 1000;
        tzTestCase=     841.20244319707786;
        tinfTestCase=   841.20529446301430;

        myRo = atm.getDensity(15, 500*1000, 0, FastMath.toRadians(-70), 16*FastMath.PI/12, 70, 70, 0, 0);
        Assert.assertEquals(roTestCase, myRo , roTestCase * 1e-14);
        Assert.assertEquals(tzTestCase,  atm.getT(), tzTestCase * 1e-13);
        Assert.assertEquals(tinfTestCase, atm.getTinf(), tinfTestCase * 1e-13);

//      IDEM., alt=800.
//      ro=    1.9556768571305D-18
//      tz=    793.65549797919
//      tinf=    793.65549802348
        // note that the values above are the ones present in the original fortran source comments
        // however, running this original source (converted to double precision) does
        // not yield the results in the comments, but instead gives the following results
        // as all the other tests cases do behave correctly, we assume the comments are wrong
        // and prefer to ensure we get the same result as the original CODE
        // as we don't have access to any other tests cases, we can't really decide if this is
        // the best approach. Indeed, we are able to get the same results as original fortran
        roTestCase =    2.4123751406975562E-018* 1000;
        tzTestCase=     841.20529391519096;
        tinfTestCase=   841.20529446301430;
        myRo = atm.getDensity(15, 800*1000, 0, FastMath.toRadians(-70), 16*FastMath.PI/12, 70, 70, 0, 0);
        Assert.assertEquals(roTestCase, myRo , roTestCase * 1e-14);
        Assert.assertEquals(tzTestCase,  atm.getT(), tzTestCase * 1e-13);
        Assert.assertEquals(tinfTestCase, atm.getTinf(), tinfTestCase * 1e-13);

    }

    @Test
    public void testNonEarthRotationAxisAlignedFrame() throws OrekitException {
        //setup
        AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        Frame ecef = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        Rotation rotation = new Rotation(Vector3D.PLUS_I, FastMath.PI / 2, RotationConvention.VECTOR_OPERATOR);
        Frame frame = new Frame(ecef, new Transform(date, rotation), "other");
        Vector3D pEcef = new Vector3D(6378137 + 300e3, 0, 0);
        Vector3D pFrame = ecef.getTransformTo(frame, date)
                .transformPosition(pEcef);
        PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        OneAxisEllipsoid earth = new OneAxisEllipsoid(
                6378136.460, 1.0 / 298.257222101, ecef);
        SolarInputs97to05 in = SolarInputs97to05.getInstance();
        earth.setAngularThreshold(1e-10);
        DTM2000 atm = new DTM2000(in, sun, earth);

        //action
        final double actual = atm.getDensity(date, pFrame, frame);

        //verify
        Assert.assertEquals(atm.getDensity(date, pEcef, ecef), actual, 0.0);
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}
