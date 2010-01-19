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


import org.apache.commons.math.geometry.Rotation;
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
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.PVCoordinates;


public class NadirPointingTest {

    // Computation date 
    private AbsoluteDate date;
    
    // Body mu 
    private double mu;

    // Reference frame = ITRF 2005C 
    private Frame frameITRF2005;

    /** Test in the case of a spheric earth : nadir pointing shall be 
     * the same as earth center pointing
     */
    @Test
    public void testSphericEarth() throws OrekitException {

        // Spheric earth shape 
        OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 0., frameITRF2005);
                
        // Create nadir pointing attitude law 
        NadirPointing nadirAttitudeLaw = new NadirPointing(earthShape);

        // Create earth center pointing attitude law 
        BodyCenterPointing earthCenterAttitudeLaw = new BodyCenterPointing(frameITRF2005);
        
        // Create satellite position as circular parameters 
        CircularOrbit circ =
            new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, Math.toRadians(50.), Math.toRadians(270.),
                                   Math.toRadians(5.300), CircularOrbit.MEAN_LONGITUDE_ARGUMENT, 
                                   FramesFactory.getEME2000(), date, mu);
        
        // Transform satellite position to position/velocity parameters in EME2000 frame 
        PVCoordinates pvSatEME2000 = circ.getPVCoordinates();
        
        // Get nadir attitude
        Rotation rotNadir = nadirAttitudeLaw.getState(date, pvSatEME2000, FramesFactory.getEME2000()).getRotation();
        
        // Get earth center attitude
        Rotation rotCenter = earthCenterAttitudeLaw.getState(date, pvSatEME2000, FramesFactory.getEME2000()).getRotation();
        
        // For a spheric earth, earth center pointing attitude and nadir pointing attitude
        // shall be the same, i.e the composition of inverse earth pointing rotation
        // with nadir pointing rotation shall be identity. 
        Rotation rotCompo = rotCenter.applyInverseTo(rotNadir);
        double angle = rotCompo.getAngle();
        Assert.assertEquals(angle, 0.0, Utils.epsilonAngle);

}
    
    /** Test in the case of an elliptic earth : nadir pointing shall be :
     *   - the same as earth center pointing in case of equatorial or polar position
     *   - different from earth center pointing in any other case
     */
    @Test
    public void testNonSphericEarth() throws OrekitException {

        // Elliptic earth shape 
        OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, frameITRF2005);
                
        // Create nadir pointing attitude law 
        NadirPointing nadirAttitudeLaw = new NadirPointing(earthShape);

        // Create earth center pointing attitude law 
        BodyCenterPointing earthCenterAttitudeLaw = new BodyCenterPointing(frameITRF2005);
        
        //  Satellite on equatorial position
        // ********************************** 
        KeplerianOrbit kep =
            new KeplerianOrbit(7178000.0, 1.e-8, Math.toRadians(50.), 0., 0.,
                                    0., KeplerianOrbit.TRUE_ANOMALY, FramesFactory.getEME2000(), date, mu);
 
        // Transform satellite position to position/velocity parameters in EME2000 frame 
        PVCoordinates pvSatEME2000 = kep.getPVCoordinates();
        
        // Get nadir attitude 
        Rotation rotNadir = nadirAttitudeLaw.getState(date, pvSatEME2000, FramesFactory.getEME2000()).getRotation();
        
        // Get earth center attitude 
        Rotation rotCenter = earthCenterAttitudeLaw.getState(date, pvSatEME2000, FramesFactory.getEME2000()).getRotation();
        
        // For a satellite at equatorial position, earth center pointing attitude and nadir pointing 
        // attitude shall be the same, i.e the composition of inverse earth pointing rotation
        // with nadir pointing rotation shall be identity. 
        Rotation rotCompo = rotCenter.applyInverseTo(rotNadir);
        double angle = rotCompo.getAngle();
        Assert.assertEquals(angle, 0.0, 5.e-6);
       
        //  Satellite on polar position
        // ***************************** 
        CircularOrbit circ =
            new CircularOrbit(7178000.0, 1.e-5, 0., Math.toRadians(90.), 0.,
                                   Math.toRadians(90.), CircularOrbit.TRUE_LONGITUDE_ARGUMENT, 
                                   FramesFactory.getEME2000(), date, mu);
 
        // Transform satellite position to position/velocity parameters in EME2000 frame */
        pvSatEME2000 = circ.getPVCoordinates();
                
        // Get nadir attitude 
        rotNadir = nadirAttitudeLaw.getState(date, pvSatEME2000, FramesFactory.getEME2000()).getRotation();
        
        // Get earth center attitude 
        rotCenter = earthCenterAttitudeLaw.getState(date, pvSatEME2000, FramesFactory.getEME2000()).getRotation();
        
        // For a satellite at polar position, earth center pointing attitude and nadir pointing 
        // attitude shall be the same, i.e the composition of inverse earth pointing rotation 
        // with nadir pointing rotation shall be identity.
        rotCompo = rotCenter.applyInverseTo(rotNadir);
        angle = rotCompo.getAngle();
        Assert.assertEquals(angle, 0.0, 5.e-6);
       
        //  Satellite on any position
        // *************************** 
        circ =
            new CircularOrbit(7178000.0, 1.e-5, 0., Math.toRadians(50.), 0.,
                                   Math.toRadians(90.), CircularOrbit.TRUE_LONGITUDE_ARGUMENT, 
                                   FramesFactory.getEME2000(), date, mu);
 
        // Transform satellite position to position/velocity parameters in EME2000 frame 
        pvSatEME2000 = circ.getPVCoordinates();
        
        // Get nadir attitude 
        rotNadir = nadirAttitudeLaw.getState(date, pvSatEME2000, FramesFactory.getEME2000()).getRotation();
        
        // Get earth center attitude
        rotCenter = earthCenterAttitudeLaw.getState(date, pvSatEME2000, FramesFactory.getEME2000()).getRotation();
        
        // For a satellite at any position, earth center pointing attitude and nadir pointing 
        // and nadir pointing attitude shall not be the same, i.e the composition of inverse earth 
        // pointing rotation with nadir pointing rotation shall be different from identity.
        rotCompo = rotCenter.applyInverseTo(rotNadir);
        angle = rotCompo.getAngle();
        Assert.assertEquals(angle, Math.toRadians(0.16797386586252272), Utils.epsilonAngle);
    }
       
    /** Vertical test : check that Z satellite axis is colinear to local vertical axis,
        which direction is : (cos(lon)*cos(lat), sin(lon)*cos(lat), sin(lat)), 
        where lon et lat stand for observed point coordinates 
        (i.e satellite ones, since they are the same by construction,
        but that's what is to test.
     */
    @Test
    public void testVertical() throws OrekitException {

        // Elliptic earth shape
        OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, frameITRF2005);
                
        // Create earth center pointing attitude law
        NadirPointing nadirAttitudeLaw = new NadirPointing(earthShape);

        //  Satellite on any position
        CircularOrbit circ =
            new CircularOrbit(7178000.0, 1.e-5, 0., Math.toRadians(50.), 0.,
                                   Math.toRadians(90.), CircularOrbit.TRUE_LONGITUDE_ARGUMENT, 
                                   FramesFactory.getEME2000(), date, mu);
 
        // Transform satellite position to position/velocity parameters in EME2000 frame */
        PVCoordinates pvSatEME2000 = circ.getPVCoordinates();
        PVCoordinates pvSatItrf = circ.getPVCoordinates(frameITRF2005);
        
        //  Vertical test
        // *************** 
        // Get observed ground point position/velocity 
        PVCoordinates pvTargetItrf = nadirAttitudeLaw.getObservedGroundPoint(date, pvSatItrf, frameITRF2005);
        
        // Convert to geodetic coordinates
        GeodeticPoint geoTarget = earthShape.transform(pvTargetItrf.getPosition(), frameITRF2005, date);

        // Compute local vertical axis
        double xVert = Math.cos(geoTarget.getLongitude())*Math.cos(geoTarget.getLatitude());
        double yVert = Math.sin(geoTarget.getLongitude())*Math.cos(geoTarget.getLatitude());
        double zVert = Math.sin(geoTarget.getLatitude());
        Vector3D targetVertical = new Vector3D(xVert, yVert, zVert);
        
        // Get attitude rotation state
        Rotation rotSatEME2000 = nadirAttitudeLaw.getState(date, pvSatEME2000, FramesFactory.getEME2000()).getRotation();
                
        // Get satellite Z axis in EME2000 frame
        Vector3D zSatEME2000 = rotSatEME2000.applyInverseTo(Vector3D.PLUS_K);
        Vector3D zSatItrf = FramesFactory.getEME2000().getTransformTo(frameITRF2005, date).transformVector(zSatEME2000);
        
        // Check that satellite Z axis is colinear to local vertical axis
        double angle= Vector3D.angle(zSatItrf, targetVertical);        
        Assert.assertEquals(Math.sin(angle), 0.0, Utils.epsilonTest);
        
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

        } catch (OrekitException oe) {
            Assert.fail(oe.getMessage());
        }

    }

    @After
    public void tearDown() {
        date = null;
        frameITRF2005 = null;
    }

}

