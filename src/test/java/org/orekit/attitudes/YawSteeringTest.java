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
package org.orekit.attitudes;



import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.Vector3D;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CircularOrbit;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;


public class YawSteeringTest {

    // Computation date 
    private AbsoluteDate date;
    
    // Reference frame = ITRF 2005C 
    private Frame frameITRF2005;
    
    // Satellite position
    CircularOrbit circOrbit;
    PVCoordinates pvSatITRF2005C;
    
    // Earth shape
    OneAxisEllipsoid earthShape;
    
    @Test
    public void testTarget() throws OrekitException {

        //  Attitude laws
        // **************
        // Target pointing attitude law without yaw compensation
        NadirPointing nadirLaw = new NadirPointing(earthShape);
 
        // Target pointing attitude law with yaw compensation
        YawSteering yawCompensLaw =
            new YawSteering(nadirLaw, CelestialBodyFactory.getSun(), Vector3D.MINUS_I);
       
        //  Check target
        // **************
        // without yaw compensation
        PVCoordinates noYawTarget = nadirLaw.getTargetInBodyFrame(date, pvSatITRF2005C, frameITRF2005);
         
        // with yaw compensation
        PVCoordinates yawTarget = yawCompensLaw.getTargetInBodyFrame(date, pvSatITRF2005C, frameITRF2005);

        // Check difference
        PVCoordinates targetDiff = new PVCoordinates(1.0, yawTarget, -1.0, noYawTarget);
        double normTargetDiffPos = targetDiff.getPosition().getNorm();
        double normTargetDiffVel = targetDiff.getVelocity().getNorm();
       
        Assert.assertTrue((normTargetDiffPos < Utils.epsilonTest)&&(normTargetDiffVel < Utils.epsilonTest));

        //  Check observed ground point
        // *****************************
        // without yaw compensation
        PVCoordinates noYawObserved = nadirLaw.getObservedGroundPoint(date, pvSatITRF2005C, frameITRF2005);

        // with yaw compensation
        PVCoordinates yawObserved = yawCompensLaw.getObservedGroundPoint(date, pvSatITRF2005C, frameITRF2005);

        // Check difference
        PVCoordinates observedDiff = new PVCoordinates(1.0, yawObserved, -1.0, noYawObserved);
        double normObservedDiffPos = observedDiff.getPosition().getNorm();
        double normObservedDiffVel = observedDiff.getVelocity().getNorm();
       
        Assert.assertTrue((normObservedDiffPos < Utils.epsilonTest)&&(normObservedDiffVel < Utils.epsilonTest));
   }

    @Test
    public void testSunAligned() throws OrekitException {

        PVCoordinates pvSatEME2000 = circOrbit.getPVCoordinates();

        //  Attitude laws
        // **************
        // Target pointing attitude law over satellite nadir at date, without yaw compensation
        NadirPointing nadirLaw = new NadirPointing(earthShape);
 
        // Target pointing attitude law with yaw compensation
        PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        YawSteering yawCompensLaw = new YawSteering(nadirLaw, sun, Vector3D.MINUS_I);

        // Get sun direction in satellite frame
        Rotation rotYaw = yawCompensLaw.getState(date, pvSatEME2000, FramesFactory.getEME2000()).getRotation();
        Vector3D sunEME2000 = sun.getPVCoordinates(date, FramesFactory.getEME2000()).getPosition();
        Vector3D sunSat = rotYaw.applyTo(sunEME2000);
            
        // Check sun is in (X,Z) plane
        Assert.assertEquals(0.0, Math.sin(sunSat.getAlpha()), 1.0e-7);

    }
    
    @Test
    public void testCompensAxis() throws OrekitException {

        PVCoordinates pvSatEME2000 = circOrbit.getPVCoordinates();

        //  Attitude laws
        // **************
        // Target pointing attitude law over satellite nadir at date, without yaw compensation
        NadirPointing nadirLaw = new NadirPointing(earthShape);
 
        // Target pointing attitude law with yaw compensation
        YawSteering yawCompensLaw =
            new YawSteering(nadirLaw, CelestialBodyFactory.getSun(), Vector3D.MINUS_I);

        // Get attitude rotations from non yaw compensated / yaw compensated laws
        Rotation rotNoYaw = nadirLaw.getState(date, pvSatEME2000, FramesFactory.getEME2000()).getRotation();
        Rotation rotYaw = yawCompensLaw.getState(date, pvSatEME2000, FramesFactory.getEME2000()).getRotation();
            
        // Compose rotations composition
        Rotation compoRot = rotYaw.applyTo(rotNoYaw.revert());
        Vector3D yawAxis = compoRot.getAxis();

        // Check axis
        Assert.assertEquals(0., yawAxis.getX(), Utils.epsilonTest);
        Assert.assertEquals(0., yawAxis.getY(), Utils.epsilonTest);
        Assert.assertEquals(1., yawAxis.getZ(), Utils.epsilonTest);

    }

    @Before
    public void setUp() {
        try {
            Utils.setDataRoot("regular-data");

            // Computation date
            date = new AbsoluteDate(new DateComponents(1970, 04, 07),
                                    TimeComponents.H00,
                                    TimeScalesFactory.getUTC());

            // Body mu
            final double mu = 3.9860047e14;
            
            // Reference frame = ITRF 2005
            frameITRF2005 = FramesFactory.getITRF2005(true);

            //  Satellite position
            circOrbit =
                new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, Math.toRadians(50.), Math.toRadians(270.),
                                       Math.toRadians(5.300), CircularOrbit.MEAN_LONGITUDE_ARGUMENT, 
                                       FramesFactory.getEME2000(), date, mu);
            
            pvSatITRF2005C = circOrbit.getPVCoordinates(frameITRF2005);
            
            // Elliptic earth shape */
            earthShape =
                new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, frameITRF2005);
            
        } catch (OrekitException oe) {
            Assert.fail(oe.getMessage());
        }

    }

    @After
    public void tearDown() {
        date = null;
        frameITRF2005 = null;
        circOrbit = null;
        pvSatITRF2005C = null;
        earthShape = null;
    }

}

