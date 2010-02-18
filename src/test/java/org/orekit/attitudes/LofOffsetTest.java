/* Copyright 2002-2010 CS Communication & Systèmes
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


import org.apache.commons.math.geometry.CardanEulerSingularityException;
import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.RotationOrder;
import org.apache.commons.math.geometry.Vector3D;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.PVCoordinates;


public class LofOffsetTest {

    // Computation date 
    private AbsoluteDate date;
    
    // Body mu 
    private double mu;

    // Reference frame = ITRF 2005C 
    private Frame frameITRF2005;
        
    // Earth shape
    OneAxisEllipsoid earthSpheric;
    
    //  Satellite position
    CircularOrbit orbit;
    PVCoordinates pvSatEME2000;

    /** Test is the lof offset is the one expected
     */
    @Test
    public void testZero() throws OrekitException, CardanEulerSingularityException {

        //  Satellite position

        // Lof aligned attitude law
        final LofOffset lofAlignedLaw = LofOffset.LOF_ALIGNED;
        final Rotation lofOffsetRot = lofAlignedLaw.getAttitude(orbit).getRotation();
        
        // Check that 
        final Vector3D momentumEME2000 = pvSatEME2000.getMomentum();
        final Vector3D momentumLof = lofOffsetRot.applyTo(momentumEME2000);
        final double cosinus = Math.cos(Vector3D.dotProduct(momentumLof, Vector3D.PLUS_K));
        Assert.assertEquals(1., cosinus, Utils.epsilonAngle);
        
    }
    /** Test if the lof offset is the one expected
     */
    @Test
    public void testOffset() throws OrekitException, CardanEulerSingularityException {

        //  Satellite position
        final CircularOrbit circ =
           new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, Math.toRadians(0.), Math.toRadians(270.),
                                   Math.toRadians(5.300), CircularOrbit.MEAN_LONGITUDE_ARGUMENT, 
                                   FramesFactory.getEME2000(), date, mu);

        // Create target pointing attitude law
        // ************************************  
        // Elliptic earth shape
        final OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, frameITRF2005);
        final GeodeticPoint geoTargetITRF2005 = new GeodeticPoint(Math.toRadians(43.36), Math.toRadians(1.26), 600.);
            
        // Attitude law definition from geodetic point target 
        final TargetPointing targetLaw = new TargetPointing(geoTargetITRF2005, earthShape);
        final Rotation targetRot = targetLaw.getAttitude(circ).getRotation();       
        
        // Create lof aligned attitude law
        // *******************************  
        final LofOffset lofAlignedLaw = LofOffset.LOF_ALIGNED;
        final Rotation lofAlignedRot = lofAlignedLaw.getAttitude(circ).getRotation();

        // Get rotation from LOF to target pointing attitude
        Rotation rollPitchYaw = targetRot.applyTo(lofAlignedRot.revert());
        final double[] angles = rollPitchYaw.getAngles(RotationOrder.ZYX);
        final double yaw = angles[0];
        final double pitch = angles[1];
        final double roll = angles[2];
        
        // Create lof offset attitude law with computed roll, pitch, yaw
        // **************************************************************  
        final LofOffset lofOffsetLaw = new LofOffset(RotationOrder.ZYX, yaw, pitch, roll);
        final Rotation lofOffsetRot = lofOffsetLaw.getAttitude(circ).getRotation();

        // Compose rotations : target pointing attitudes
        final double angleCompo = targetRot.applyInverseTo(lofOffsetRot).getAngle();
        Assert.assertEquals(0., angleCompo, Utils.epsilonAngle);

    } 
    
    /** Test is the target pointed is the one expected
     */
    @Test
    public void testTarget() 
        throws OrekitException, CardanEulerSingularityException {
        
        // Create target point and target pointing law towards that point
        final GeodeticPoint targetDef  = new GeodeticPoint(Math.toRadians(5.), Math.toRadians(-40.), 0.);
        final TargetPointing targetLaw = new TargetPointing(targetDef, earthSpheric);
       
        // Get roll, pitch, yaw angles corresponding to this pointing law
        final LofOffset lofAlignedLaw = LofOffset.LOF_ALIGNED;
        final Rotation lofAlignedRot = lofAlignedLaw.getAttitude(orbit).getRotation();
        final Attitude targetAttitude = targetLaw.getAttitude(orbit);
        final Rotation rollPitchYaw = targetAttitude.getRotation().applyTo(lofAlignedRot.revert());
        final double[] angles = rollPitchYaw.getAngles(RotationOrder.ZYX);
        final double yaw = angles[0];
        final double pitch = angles[1];
        final double roll = angles[2];
        
        // Create a lof offset law from those values
        final LofOffset lofOffsetLaw = new LofOffset(RotationOrder.ZYX, yaw, pitch, roll);
        final LofOffsetPointing lofOffsetPtLaw = new LofOffsetPointing(earthSpheric, lofOffsetLaw, Vector3D.PLUS_K);

        // Check target pointed by this law : shall be the same as defined
        final Vector3D pTargetRes = lofOffsetPtLaw.getTargetPoint(orbit, earthSpheric.getBodyFrame());
        final GeodeticPoint targetRes = earthSpheric.transform(pTargetRes, earthSpheric.getBodyFrame(), date);
        
        Assert.assertEquals(targetDef.getLongitude(), targetRes.getLongitude(), Utils.epsilonAngle);
        Assert.assertEquals(targetDef.getLongitude(), targetRes.getLongitude(), Utils.epsilonAngle);
        
    }

    @Test
    public void testSpin() throws OrekitException {

        final AttitudeLaw law = new LofOffset(RotationOrder.XYX, 0.1, 0.2, 0.3);

        AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 01, 01),
                                             new TimeComponents(3, 25, 45.6789),
                                             TimeScalesFactory.getUTC());
        KeplerianOrbit orbit =
            new KeplerianOrbit(7178000.0, 1.e-4, Math.toRadians(50.),
                              Math.toRadians(10.), Math.toRadians(20.),
                              Math.toRadians(30.), KeplerianOrbit.MEAN_ANOMALY, 
                              FramesFactory.getEME2000(), date, 3.986004415e14);

        Propagator propagator = new KeplerianPropagator(orbit, law);

        double h = 0.01;
        SpacecraftState sMinus = propagator.propagate(date.shiftedBy(-h));
        SpacecraftState s0     = propagator.propagate(date);
        SpacecraftState sPlus  = propagator.propagate(date.shiftedBy(h));

        Vector3D spin0 = s0.getAttitude().getSpin();
        Vector3D reference = Attitude.estimateSpin(sMinus.getAttitude().getRotation(),
                                                   sPlus.getAttitude().getRotation(),
                                                   2 * h);
        Assert.assertEquals(0.0, spin0.subtract(reference).getNorm(), 4.0e-11);

    }

    @Before
    public void setUp() {
        try {

            Utils.setDataRoot("regular-data");

            // Computation date
            date = new AbsoluteDate(new DateComponents(2008, 04, 07),
                                    TimeComponents.H00,
                                    TimeScalesFactory.getUTC());

            // Body mu
            mu = 3.9860047e14;
            
            // Reference frame = ITRF 2005
            frameITRF2005 = FramesFactory.getITRF2005(true);

            // Elliptic earth shape
            earthSpheric =
                new OneAxisEllipsoid(6378136.460, 0., frameITRF2005);
            
            //  Satellite position
            orbit =
                new CircularOrbit(7178000.0, 0.5e-8, -0.5e-8, Math.toRadians(50.), Math.toRadians(150.),
                                       Math.toRadians(5.300), CircularOrbit.MEAN_LONGITUDE_ARGUMENT, 
                                       FramesFactory.getEME2000(), date, mu);
            pvSatEME2000 = orbit.getPVCoordinates();

            
        } catch (OrekitException oe) {
            Assert.fail(oe.getMessage());
        }

    }

    @After
    public void tearDown() {
        date = null;
        frameITRF2005 = null;
        earthSpheric = null;
    }

}

