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

import org.apache.commons.math.geometry.CardanEulerSingularityException;
import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.RotationOrder;
import org.apache.commons.math.geometry.Vector3D;
import org.orekit.Utils;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.LofOffset;
import org.orekit.attitudes.LofOffsetPointing;
import org.orekit.attitudes.TargetPointing;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.orbits.CircularOrbit;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChunkedDate;
import org.orekit.time.ChunkedTime;
import org.orekit.time.UTCScale;
import org.orekit.utils.PVCoordinates;


public class LofOffsetTest extends TestCase {

    // Computation date 
    private AbsoluteDate date;
    
    // Body mu 
    private double mu;

    // Reference frame = ITRF 2000B 
    private Frame frameItrf2000B;
        
    // Earth shape
    OneAxisEllipsoid earthSpheric;
    
    //  Satellite position
    CircularOrbit orbit;
    PVCoordinates pvSatJ2000;
    
    /** Test class for body center pointing attitude law.
     */
    public LofOffsetTest(String name) {
        super(name);
    }

    /** Test is the lof offset is the one expected
     */
    public void testZero() throws OrekitException, CardanEulerSingularityException {

        //  Satellite position

        // Lof aligned attitude law
        final LofOffset lofAlignedLaw = LofOffset.LOF_ALIGNED;
        final Rotation lofOffsetRot = lofAlignedLaw.getState(date, pvSatJ2000, Frame.getJ2000()).getRotation();
        
        // Check that 
        final Vector3D p = pvSatJ2000.getPosition();
        final Vector3D v = pvSatJ2000.getVelocity();
        final Vector3D momentumJ2000 = Vector3D.crossProduct(p, v);
        final Vector3D momentumLof = lofOffsetRot.applyTo(momentumJ2000);
        final double cosinus = Math.cos(Vector3D.dotProduct(momentumLof, Vector3D.PLUS_K));
        assertEquals(1., cosinus, Utils.epsilonAngle);
        
    }
    /** Test if the lof offset is the one expected
     */
    public void testOffset() throws OrekitException, CardanEulerSingularityException {

        //  Satellite position
        final CircularOrbit circ =
           new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, Math.toRadians(0.), Math.toRadians(270.),
                                   Math.toRadians(5.300), CircularOrbit.MEAN_LONGITUDE_ARGUMENT, 
                                   Frame.getJ2000(), date, mu);
        final PVCoordinates pvSatJ2000 = circ.getPVCoordinates();

        // Create target pointing attitude law
        // ************************************  
        // Elliptic earth shape
        final OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, frameItrf2000B);
        final GeodeticPoint geoTargetItrf2000B = new GeodeticPoint(Math.toRadians(1.26), Math.toRadians(43.36), 600.);
            
        // Attitude law definition from geodetic point target 
        final TargetPointing targetLaw = new TargetPointing(geoTargetItrf2000B, earthShape);
        final Rotation targetRot = targetLaw.getState(date, pvSatJ2000, Frame.getJ2000()).getRotation();       
        
        // Create lof aligned attitude law
        // *******************************  
        final LofOffset lofAlignedLaw = LofOffset.LOF_ALIGNED;
        final Rotation lofAlignedRot = lofAlignedLaw.getState(date, pvSatJ2000, Frame.getJ2000()).getRotation();

        // Get rotation from LOF to target pointing attitude
        Rotation rollPitchYaw = targetRot.applyTo(lofAlignedRot.revert());
        final double[] angles = rollPitchYaw.getAngles(RotationOrder.ZYX);
        final double yaw = angles[0];
        final double pitch = angles[1];
        final double roll = angles[2];
        
        // Create lof offset attitude law with computed roll, pitch, yaw
        // **************************************************************  
        final LofOffset lofOffsetLaw = new LofOffset(RotationOrder.ZYX, yaw, pitch, roll);
        final Rotation lofOffsetRot = lofOffsetLaw.getState(date, pvSatJ2000, Frame.getJ2000()).getRotation();

        // Compose rotations : target pointing attitudes
        final double angleCompo = targetRot.applyInverseTo(lofOffsetRot).getAngle();
        assertEquals(0., angleCompo, Utils.epsilonAngle);
        
        
    } 
    
    /** Test is the target pointed is the one expected
     */
    public void testTarget() 
        throws OrekitException, CardanEulerSingularityException {
        
        // Create target point and target pointing law towards that point
        final GeodeticPoint targetDef  = new GeodeticPoint(Math.toRadians(-40.), Math.toRadians(5.), 0.);
        final TargetPointing targetLaw = new TargetPointing(targetDef, earthSpheric);
       
        // Get roll, pitch, yaw angles corresponding to this pointing law
        final LofOffset lofAlignedLaw = LofOffset.LOF_ALIGNED;
        final Rotation lofAlignedRot = lofAlignedLaw.getState(date, pvSatJ2000, Frame.getJ2000()).getRotation();
        final Attitude targetAttitude = targetLaw.getState(date, pvSatJ2000, Frame.getJ2000());
        final Rotation rollPitchYaw = targetAttitude.getRotation().applyTo(lofAlignedRot.revert());
        final double[] angles = rollPitchYaw.getAngles(RotationOrder.ZYX);
        final double yaw = angles[0];
        final double pitch = angles[1];
        final double roll = angles[2];
        
        // Create a lof offset law from those values
        final LofOffset lofOffsetLaw = new LofOffset(RotationOrder.ZYX, yaw, pitch, roll);
        final LofOffsetPointing lofOffsetPtLaw = new LofOffsetPointing(earthSpheric, lofOffsetLaw, Vector3D.PLUS_K);

        // Check target pointed by this law : shall be the same as defined
        final PVCoordinates pvTargetRes = lofOffsetPtLaw.getTargetInBodyFrame(date, pvSatJ2000, Frame.getJ2000());
        final GeodeticPoint targetRes = earthSpheric.transform(pvTargetRes.getPosition(), earthSpheric.getBodyFrame(), date);
        
        assertEquals(targetDef.getLongitude(), targetRes.getLongitude(), Utils.epsilonAngle);
        assertEquals(targetDef.getLongitude(), targetRes.getLongitude(), Utils.epsilonAngle);
        
    }
    
    public void setUp() {
        try {
            // Computation date
            date = new AbsoluteDate(new ChunkedDate(2008, 04, 07),
                                    ChunkedTime.H00,
                                    UTCScale.getInstance());

            // Body mu
            mu = 3.9860047e14;
            
            // Reference frame = ITRF 2000B
            frameItrf2000B = Frame.getITRF2000B();

            // Elliptic earth shape
            earthSpheric =
                new OneAxisEllipsoid(6378136.460, 0., frameItrf2000B);
            
            //  Satellite position
            orbit =
                new CircularOrbit(7178000.0, 0.5e-8, -0.5e-8, Math.toRadians(50.), Math.toRadians(150.),
                                       Math.toRadians(5.300), CircularOrbit.MEAN_LONGITUDE_ARGUMENT, 
                                       Frame.getJ2000(), date, mu);
            pvSatJ2000 = orbit.getPVCoordinates();

            
        } catch (OrekitException oe) {
            fail(oe.getMessage());
        }

    }

    public void tearDown() {
        date = null;
        frameItrf2000B = null;
        earthSpheric = null;
    }

    public static Test suite() {
        return new TestSuite(LofOffsetTest.class);
    }
}

