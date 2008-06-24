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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.Vector3D;
import org.orekit.Utils;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.forces.Sun;
import org.orekit.frames.Frame;
import org.orekit.orbits.CircularOrbit;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChunkedDate;
import org.orekit.time.ChunkedTime;
import org.orekit.time.UTCScale;
import org.orekit.utils.PVCoordinates;


public class YawSteeringTest extends TestCase {

    /** Test class for yaw steering attitude law.
     */
    public YawSteeringTest(String name) {
        super(name);
    }

    // Computation date 
    private AbsoluteDate date;
    
    // Reference frame = ITRF 2000B 
    private Frame frameItrf2000B;
    
    // Satellite position
    CircularOrbit circOrbit;
    PVCoordinates pvSatItrf2000B;
    
    // Earth shape
    OneAxisEllipsoid earthShape;
    
    /** Test that pointed target and observed ground point remain the same 
     * with or without yaw compensation
     */
    public void testTarget() throws OrekitException {

        //  Attitude laws
        // **************
        // Target pointing attitude law without yaw compensation
        NadirPointing nadirLaw = new NadirPointing(earthShape);
 
        // Target pointing attitude law with yaw compensation
        YawSteering yawCompensLaw = new YawSteering(nadirLaw, new Sun(), Vector3D.MINUS_I);
       
        //  Check target
        // **************
        // without yaw compensation
        PVCoordinates noYawTarget = nadirLaw.getTargetInBodyFrame(date, pvSatItrf2000B, frameItrf2000B);
         
        // with yaw compensation
        PVCoordinates yawTarget = yawCompensLaw.getTargetInBodyFrame(date, pvSatItrf2000B, frameItrf2000B);

        // Check difference
        PVCoordinates targetDiff = new PVCoordinates(1.0, yawTarget, -1.0, noYawTarget);
        double normTargetDiffPos = targetDiff.getPosition().getNorm();
        double normTargetDiffVel = targetDiff.getVelocity().getNorm();
       
        assertTrue((normTargetDiffPos < Utils.epsilonTest)&&(normTargetDiffVel < Utils.epsilonTest));

        //  Check observed ground point
        // *****************************
        // without yaw compensation
        PVCoordinates noYawObserved = nadirLaw.getObservedGroundPoint(date, pvSatItrf2000B, frameItrf2000B);

        // with yaw compensation
        PVCoordinates yawObserved = yawCompensLaw.getObservedGroundPoint(date, pvSatItrf2000B, frameItrf2000B);

        // Check difference
        PVCoordinates observedDiff = new PVCoordinates(1.0, yawObserved, -1.0, noYawObserved);
        double normObservedDiffPos = observedDiff.getPosition().getNorm();
        double normObservedDiffVel = observedDiff.getVelocity().getNorm();
       
        assertTrue((normObservedDiffPos < Utils.epsilonTest)&&(normObservedDiffVel < Utils.epsilonTest));
   }

    /** Test that compensation rotation axis put sun in the (Xsat,Zsat) plane
     */
    public void testSunAligned() throws OrekitException {

        PVCoordinates pvSatJ2000 = circOrbit.getPVCoordinates();

        //  Attitude laws
        // **************
        // Target pointing attitude law over satellite nadir at date, without yaw compensation
        NadirPointing nadirLaw = new NadirPointing(earthShape);
 
        // Target pointing attitude law with yaw compensation
        Sun sun = new Sun();
        YawSteering yawCompensLaw = new YawSteering(nadirLaw, sun, Vector3D.MINUS_I);

        // Get sun direction in satellite frame
        Rotation rotYaw = yawCompensLaw.getState(date, pvSatJ2000, Frame.getJ2000()).getRotation();
        Vector3D sunJ2000 = sun.getPosition(date, Frame.getJ2000());
        Vector3D sunSat = rotYaw.applyTo(sunJ2000);
            
        // Check sun is in (X,Z) plane
        assertEquals(0.0, Math.sin(sunSat.getAlpha()), 1.0e-7);

    }
    
    /** Test that compensation rotation axis is Zsat, yaw axis
     */
    public void testCompensAxis() throws OrekitException {

        PVCoordinates pvSatJ2000 = circOrbit.getPVCoordinates();

        //  Attitude laws
        // **************
        // Target pointing attitude law over satellite nadir at date, without yaw compensation
        NadirPointing nadirLaw = new NadirPointing(earthShape);
 
        // Target pointing attitude law with yaw compensation
        YawSteering yawCompensLaw = new YawSteering(nadirLaw, new Sun(), Vector3D.MINUS_I);

        // Get attitude rotations from non yaw compensated / yaw compensated laws
        Rotation rotNoYaw = nadirLaw.getState(date, pvSatJ2000, Frame.getJ2000()).getRotation();
        Rotation rotYaw = yawCompensLaw.getState(date, pvSatJ2000, Frame.getJ2000()).getRotation();
            
        // Compose rotations composition
        Rotation compoRot = rotYaw.applyTo(rotNoYaw.revert());
        Vector3D yawAxis = compoRot.getAxis();

        // Check axis
        assertEquals(0., yawAxis.getX(), Utils.epsilonTest);
        assertEquals(0., yawAxis.getY(), Utils.epsilonTest);
        assertEquals(1., yawAxis.getZ(), Utils.epsilonTest);

    }
    
    public void setUp() {
        try {
            // Computation date
            date = new AbsoluteDate(new ChunkedDate(2008, 04, 07),
                                    ChunkedTime.H00,
                                    UTCScale.getInstance());

            // Body mu
            final double mu = 3.9860047e14;
            
            // Reference frame = ITRF 2000B
            frameItrf2000B = Frame.getITRF2000B();

            //  Satellite position
            circOrbit =
                new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, Math.toRadians(50.), Math.toRadians(270.),
                                       Math.toRadians(5.300), CircularOrbit.MEAN_LONGITUDE_ARGUMENT, 
                                       Frame.getJ2000(), date, mu);
            
            pvSatItrf2000B = circOrbit.getPVCoordinates(frameItrf2000B);
            
            // Elliptic earth shape */
            earthShape =
                new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, frameItrf2000B);
            
        } catch (OrekitException oe) {
            fail(oe.getMessage());
        }

    }

    public void tearDown() {
        date = null;
        frameItrf2000B = null;
        circOrbit = null;
        pvSatItrf2000B = null;
        earthShape = null;
    }

    public static Test suite() {
        return new TestSuite(YawSteeringTest.class);
    }
}

