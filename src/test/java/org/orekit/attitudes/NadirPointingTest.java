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
import org.orekit.attitudes.BodyCenterPointing;
import org.orekit.attitudes.NadirPointing;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChunkedDate;
import org.orekit.time.ChunkedTime;
import org.orekit.time.UTCScale;
import org.orekit.utils.PVCoordinates;


public class NadirPointingTest extends TestCase {

    // Computation date 
    private AbsoluteDate date;
    
    // Body mu 
    private double mu;

    // Reference frame = ITRF 2000B 
    private Frame frameItrf2000B;
    
    /** Test class for body center pointing attitude law.
     */
    public NadirPointingTest(String name) {
        super(name);
    }

    /** Test in the case of a spheric earth : nadir pointing shall be 
     * the same as earth center pointing
     */
    public void testSphericEarth() throws OrekitException {

        // Spheric earth shape 
        OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 0., frameItrf2000B);
                
        // Create nadir pointing attitude law 
        NadirPointing nadirAttitudeLaw = new NadirPointing(earthShape);

        // Create earth center pointing attitude law 
        BodyCenterPointing earthCenterAttitudeLaw = new BodyCenterPointing(frameItrf2000B);
        
        // Create satellite position as circular parameters 
        CircularOrbit circ =
            new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, Math.toRadians(50.), Math.toRadians(270.),
                                   Math.toRadians(5.300), CircularOrbit.MEAN_LONGITUDE_ARGUMENT, 
                                   Frame.getJ2000(), date, mu);
        
        // Transform satellite position to position/velocity parameters in J2000 frame 
        PVCoordinates pvSatJ2000 = circ.getPVCoordinates();
        
        // Get nadir attitude
        Rotation rotNadir = nadirAttitudeLaw.getState(date, pvSatJ2000, Frame.getJ2000()).getRotation();
        
        // Get earth center attitude
        Rotation rotCenter = earthCenterAttitudeLaw.getState(date, pvSatJ2000, Frame.getJ2000()).getRotation();
        
        // For a spheric earth, earth center pointing attitude and nadir pointing attitude
        // shall be the same, i.e the composition of inverse earth pointing rotation
        // with nadir pointing rotation shall be identity. 
        Rotation rotCompo = rotCenter.applyInverseTo(rotNadir);
        double angle = rotCompo.getAngle();
        assertEquals(angle, 0.0, Utils.epsilonAngle);

}
    
    /** Test in the case of an elliptic earth : nadir pointing shall be :
     *   - the same as earth center pointing in case of equatorial or polar position
     *   - different from earth center pointing in any other case
     */
    public void testNonSphericEarth() throws OrekitException {

        // Elliptic earth shape 
        OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, frameItrf2000B);
                
        // Create nadir pointing attitude law 
        NadirPointing nadirAttitudeLaw = new NadirPointing(earthShape);

        // Create earth center pointing attitude law 
        BodyCenterPointing earthCenterAttitudeLaw = new BodyCenterPointing(frameItrf2000B);
        
        //  Satellite on equatorial position
        // ********************************** 
        KeplerianOrbit kep =
            new KeplerianOrbit(7178000.0, 1.e-8, Math.toRadians(50.), 0., 0.,
                                    0., KeplerianOrbit.TRUE_ANOMALY, Frame.getJ2000(), date, mu);
 
        // Transform satellite position to position/velocity parameters in J2000 frame 
        PVCoordinates pvSatJ2000 = kep.getPVCoordinates();
        
        // Get nadir attitude 
        Rotation rotNadir = nadirAttitudeLaw.getState(date, pvSatJ2000, Frame.getJ2000()).getRotation();
        
        // Get earth center attitude 
        Rotation rotCenter = earthCenterAttitudeLaw.getState(date, pvSatJ2000, Frame.getJ2000()).getRotation();
        
        // For a satellite at equatorial position, earth center pointing attitude and nadir pointing 
        // attitude shall be the same, i.e the composition of inverse earth pointing rotation
        // with nadir pointing rotation shall be identity. 
        Rotation rotCompo = rotCenter.applyInverseTo(rotNadir);
        double angle = rotCompo.getAngle();
        assertEquals(angle, 0.0, 5.e-6);
       
        //  Satellite on polar position
        // ***************************** 
        CircularOrbit circ =
            new CircularOrbit(7178000.0, 1.e-5, 0., Math.toRadians(90.), 0.,
                                   Math.toRadians(90.), CircularOrbit.TRUE_LONGITUDE_ARGUMENT, 
                                   Frame.getJ2000(), date, mu);
 
        // Transform satellite position to position/velocity parameters in J2000 frame */
        pvSatJ2000 = circ.getPVCoordinates();
                
        // Get nadir attitude 
        rotNadir = nadirAttitudeLaw.getState(date, pvSatJ2000, Frame.getJ2000()).getRotation();
        
        // Get earth center attitude 
        rotCenter = earthCenterAttitudeLaw.getState(date, pvSatJ2000, Frame.getJ2000()).getRotation();
        
        // For a satellite at polar position, earth center pointing attitude and nadir pointing 
        // attitude shall be the same, i.e the composition of inverse earth pointing rotation 
        // with nadir pointing rotation shall be identity.
        rotCompo = rotCenter.applyInverseTo(rotNadir);
        angle = rotCompo.getAngle();
        assertEquals(angle, 0.0, 5.e-6);
       
        //  Satellite on any position
        // *************************** 
        circ =
            new CircularOrbit(7178000.0, 1.e-5, 0., Math.toRadians(50.), 0.,
                                   Math.toRadians(90.), CircularOrbit.TRUE_LONGITUDE_ARGUMENT, 
                                   Frame.getJ2000(), date, mu);
 
        // Transform satellite position to position/velocity parameters in J2000 frame 
        pvSatJ2000 = circ.getPVCoordinates();
        
        // Get nadir attitude 
        rotNadir = nadirAttitudeLaw.getState(date, pvSatJ2000, Frame.getJ2000()).getRotation();
        
        // Get earth center attitude
        rotCenter = earthCenterAttitudeLaw.getState(date, pvSatJ2000, Frame.getJ2000()).getRotation();
        
        // For a satellite at any position, earth center pointing attitude and nadir pointing 
        // and nadir pointing attitude shall not be the same, i.e the composition of inverse earth 
        // pointing rotation with nadir pointing rotation shall be different from identity.
        rotCompo = rotCenter.applyInverseTo(rotNadir);
        angle = rotCompo.getAngle();
        assertEquals(angle, Math.toRadians(0.16797386586252272), Utils.epsilonAngle);
    }
    
    
       
    /** Vertical test : check that Z satellite axis is colinear to local vertical axis,
        which direction is : (cos(lon)*cos(lat), sin(lon)*cos(lat), sin(lat)), 
        where lon et lat stand for observed point coordinates 
        (i.e satellite ones, since they are the same by construction,
        but that's what is to test.
     */
    public void testVertical() throws OrekitException {

        // Elliptic earth shape
        OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, frameItrf2000B);
                
        // Create earth center pointing attitude law
        NadirPointing nadirAttitudeLaw = new NadirPointing(earthShape);

        //  Satellite on any position
        CircularOrbit circ =
            new CircularOrbit(7178000.0, 1.e-5, 0., Math.toRadians(50.), 0.,
                                   Math.toRadians(90.), CircularOrbit.TRUE_LONGITUDE_ARGUMENT, 
                                   Frame.getJ2000(), date, mu);
 
        // Transform satellite position to position/velocity parameters in J2000 frame */
        PVCoordinates pvSatJ2000 = circ.getPVCoordinates();
        PVCoordinates pvSatItrf = circ.getPVCoordinates(frameItrf2000B);
        
        //  Vertical test
        // *************** 
        // Get observed ground point position/velocity 
        PVCoordinates pvTargetItrf = nadirAttitudeLaw.getObservedGroundPoint(date, pvSatItrf, frameItrf2000B);
        
        // Convert to geodetic coordinates
        GeodeticPoint geoTarget = earthShape.transform(pvTargetItrf.getPosition(), frameItrf2000B, date);

        // Compute local vertical axis
        double xVert = Math.cos(geoTarget.getLongitude())*Math.cos(geoTarget.getLatitude());
        double yVert = Math.sin(geoTarget.getLongitude())*Math.cos(geoTarget.getLatitude());
        double zVert = Math.sin(geoTarget.getLatitude());
        Vector3D targetVertical = new Vector3D(xVert, yVert, zVert);
        
        // Get attitude rotation state
        Rotation rotSatJ2000 = nadirAttitudeLaw.getState(date, pvSatJ2000, Frame.getJ2000()).getRotation();
                
        // Get satellite Z axis in J2000 frame
        Vector3D zSatJ2000 = rotSatJ2000.applyInverseTo(Vector3D.PLUS_K);
        Vector3D zSatItrf = Frame.getJ2000().getTransformTo(frameItrf2000B, date).transformVector(zSatJ2000);
        
        // Check that satellite Z axis is colinear to local vertical axis
        double angle= Vector3D.angle(zSatItrf, targetVertical);        
        assertEquals(Math.sin(angle), 0.0, Utils.epsilonTest);
        
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

        } catch (OrekitException oe) {
            fail(oe.getMessage());
        }

    }

    public void tearDown() {
        date = null;
        frameItrf2000B = null;
        //j2000ToItrf = null;
    }

    public static Test suite() {
        return new TestSuite(NadirPointingTest.class);
    }
}

