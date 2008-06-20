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
import org.orekit.attitudes.NadirPointing;
import org.orekit.attitudes.TargetPointing;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.orbits.CircularOrbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChunkedDate;
import org.orekit.time.ChunkedTime;
import org.orekit.time.UTCScale;
import org.orekit.utils.Line;
import org.orekit.utils.PVCoordinates;


public class TargetPointingTest extends TestCase {

    // Computation date 
    private AbsoluteDate date;
    
    // Body mu 
    private double mu;

    // Reference frame = ITRF 2000B 
    private Frame frameItrf2000B;
        
    // Transform from J2000 to ITRF2000B 
    private Transform j2000ToItrf;
    
    /** Test class for body center pointing attitude law.
     */
    public TargetPointingTest(String name) {
        super(name);
    }

    /** Test if both constructors are equivalent
     */
    public void testConstructors() throws OrekitException {

        //  Satellite position
        // ********************
        CircularOrbit circ =
            new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, Math.toRadians(50.), Math.toRadians(270.),
                                   Math.toRadians(5.300), CircularOrbit.MEAN_LONGITUDE_ARGUMENT, 
                                   Frame.getJ2000(), date, mu);
        
        // Transform satellite position to position/velocity parameters in J2000 frame
        PVCoordinates pvSatJ2000 = circ.getPVCoordinates();
     
        //  Attitude laws
        // *************** 
        // Elliptic earth shape
        OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, frameItrf2000B);
                
        // Target definition as a geodetic point AND as a position/velocity vector
        GeodeticPoint geoTargetItrf2000B = new GeodeticPoint(Math.toRadians(1.26), Math.toRadians(43.36), 600.);
        PVCoordinates pvTargetItrf2000B = new PVCoordinates(earthShape.transform(geoTargetItrf2000B), Vector3D.ZERO);
            
        // Attitude law definition from geodetic point target 
        TargetPointing geoTargetAttitudeLaw = new TargetPointing(geoTargetItrf2000B, earthShape);
        
        //  Attitude law definition from position/velocity target
        TargetPointing pvTargetAttitudeLaw = new TargetPointing(frameItrf2000B, pvTargetItrf2000B);
        
        // Check that both attitude are the same 
        // Get satellite rotation for target pointing law 
        Rotation rotPv = pvTargetAttitudeLaw.getState(date, pvSatJ2000, Frame.getJ2000()).getRotation();

        // Get satellite rotation for nadir pointing law
        Rotation rotGeo = geoTargetAttitudeLaw.getState(date, pvSatJ2000, Frame.getJ2000()).getRotation();

        // Rotations composition
        Rotation rotCompo = rotGeo.applyInverseTo(rotPv);
        double angle = rotCompo.getAngle();
        assertEquals(angle, 0.0, Utils.epsilonAngle);

    }

    /** Test if geodetic constructor works
     */
    public void testGeodeticConstructor() throws OrekitException {

        //  Satellite position
        // ******************** 
        CircularOrbit circ =
            new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, Math.toRadians(50.), Math.toRadians(270.),
                                   Math.toRadians(5.300), CircularOrbit.MEAN_LONGITUDE_ARGUMENT, 
                                   Frame.getJ2000(), date, mu);
        
        // Transform satellite position to position/velocity parameters in J2000 frame 
        PVCoordinates pvSatJ2000 = circ.getPVCoordinates();
     
        //  Attitude law
        // ************** 
        
        // Elliptic earth shape 
        OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, frameItrf2000B);
                
        // Target definition as a geodetic point 
        GeodeticPoint geoTargetItrf2000B = new GeodeticPoint(Math.toRadians(1.26), Math.toRadians(43.36), 600.);
            
        //  Attitude law definition 
        TargetPointing geoTargetAttitudeLaw = new TargetPointing(geoTargetItrf2000B, earthShape);
        
        // Check that observed ground point is the same as defined target 
        PVCoordinates pvObservedJ2000 = geoTargetAttitudeLaw.getObservedGroundPoint(date, pvSatJ2000, Frame.getJ2000());
        GeodeticPoint geoObserved = earthShape.transform(pvObservedJ2000.getPosition(), Frame.getJ2000(), date);

        assertEquals(geoObserved.getLongitude(), geoTargetItrf2000B.getLongitude(), Utils.epsilonAngle);
        assertEquals(geoObserved.getLatitude(), geoTargetItrf2000B.getLatitude(), Utils.epsilonAngle);
        assertEquals(geoObserved.getAltitude(), geoTargetItrf2000B.getAltitude(), 1.e-8);

    }

    /** Test with nadir target : Check that when the target is the same as nadir target at date,
     * satellite attitude is the same as nadir attitude at the same date, but different at a different date.
     */
    public void testNadirTarget() throws OrekitException {

        // Elliptic earth shape
        OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, frameItrf2000B);
                
        // Satellite on any position 
        CircularOrbit circOrbit =
            new CircularOrbit(7178000.0, 1.e-5, 0., Math.toRadians(50.), 0.,
                                   Math.toRadians(90.), CircularOrbit.TRUE_LONGITUDE_ARGUMENT, 
                                   Frame.getJ2000(), date, mu);

        // Transform satellite position to position/velocity parameters in J2000 frame
        PVCoordinates pvSatJ2000 = circOrbit.getPVCoordinates();
        
        
        //  Target attitude law with target under satellite nadir 
        // ******************************************************* 
        // Definition of nadir target 
        // Create nadir pointing attitude law 
        NadirPointing nadirAttitudeLaw = new NadirPointing(earthShape);
        
        // Check nadir target 
        PVCoordinates pvNadirTarget = nadirAttitudeLaw.getObservedGroundPoint(date, j2000ToItrf.transformPVCoordinates(pvSatJ2000), 
                                                                              frameItrf2000B);
        GeodeticPoint geoNadirTarget = earthShape.transform(pvNadirTarget.getPosition(), frameItrf2000B, date);
        
        // Create target attitude law 
        TargetPointing targetAttitudeLaw = new TargetPointing(geoNadirTarget, earthShape);

        //  1/ Test that attitudes are the same at date
        // *********************************************
        // i.e the composition of inverse earth pointing rotation
        // with nadir pointing rotation shall be identity. 
        
        // Get satellite rotation from target pointing law at date
        Rotation rotTarget = targetAttitudeLaw.getState(date, pvSatJ2000, Frame.getJ2000()).getRotation();

        // Get satellite rotation from nadir pointing law at date
        Rotation rotNadir = nadirAttitudeLaw.getState(date, pvSatJ2000, Frame.getJ2000()).getRotation();

        // Compose attitude rotations
        Rotation rotCompo = rotTarget.applyInverseTo(rotNadir);
        double angle = rotCompo.getAngle();
        assertEquals(angle, 0.0, Utils.epsilonAngle);

        
        //  2/ Test that attitudes are different at a different date
        // **********************************************************

        // Extrapolation one minute later
        KeplerianPropagator extrapolator = new KeplerianPropagator(circOrbit);
        double delta_t = 60.0; // extrapolation duration in seconds
        AbsoluteDate extrapDate = new AbsoluteDate(date, delta_t);
        SpacecraftState extrapOrbit = extrapolator.propagate(extrapDate);
        PVCoordinates extrapPvSatJ2000 = extrapOrbit.getPVCoordinates();
        
        // Get satellite rotation from target pointing law at date + 1min
        Rotation extrapRotTarget = targetAttitudeLaw.getState(extrapDate, extrapPvSatJ2000, Frame.getJ2000()).getRotation();
        
        // Get satellite rotation from nadir pointing law at date
        Rotation extrapRotNadir = nadirAttitudeLaw.getState(extrapDate, extrapPvSatJ2000, Frame.getJ2000()).getRotation();

        // Compose attitude rotations
        Rotation extrapRotCompo = extrapRotTarget.applyInverseTo(extrapRotNadir);
        double extrapAngle = extrapRotCompo.getAngle();
        assertEquals(extrapAngle, Math.toRadians(24.684793905118823), Utils.epsilonAngle);
        
    }
       
    /** Test if defined target belongs to the direction pointed by the satellite
     */
    public void testTargetInPointingDirection() throws OrekitException {

        // Create computation date 
        AbsoluteDate date = new AbsoluteDate(new ChunkedDate(2008, 04, 07),
                                             ChunkedTime.H00,
                                             UTCScale.getInstance());
        
        // Reference frame = ITRF 2000B
        Frame frameItrf2000B = Frame.getITRF2000B();

        // Elliptic earth shape 
        OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, frameItrf2000B);
                
        // Create target pointing attitude law 
        GeodeticPoint geoTarget = new GeodeticPoint(Math.toRadians(1.26), Math.toRadians(43.36), 600.);
        TargetPointing targetAttitudeLaw = new TargetPointing(geoTarget, earthShape);
        
        //  Satellite position
        // ********************
        // Create satellite position as circular parameters
        CircularOrbit circ =
            new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, Math.toRadians(50.), Math.toRadians(270.),
                                   Math.toRadians(5.300), CircularOrbit.MEAN_LONGITUDE_ARGUMENT, 
                                   Frame.getJ2000(), date, mu);
        
        // Transform satellite position to position/velocity parameters in J2000 frame
        PVCoordinates pvSatJ2000 = circ.getPVCoordinates();
        
        //  Pointing direction
        // ********************
        // Get satellite attitude rotation, i.e rotation from J2000 frame to satellite frame
        Rotation rotSatJ2000 = targetAttitudeLaw.getState(date, pvSatJ2000, Frame.getJ2000()).getRotation();
        
        // Transform Z axis from satellite frame to J2000 
        Vector3D zSatJ2000 = rotSatJ2000.applyInverseTo(Vector3D.PLUS_K);
        
        // Line containing satellite point and following pointing direction
        Line pointingLine = new Line(j2000ToItrf.transformPosition(pvSatJ2000.getPosition()), j2000ToItrf.transformVector(zSatJ2000));
        
        // Check that the line contains earth center
        double distance = pointingLine.distance(earthShape.transform(geoTarget));
        
        assertTrue(distance < 1.e-7);
    }

    /** Test the difference between pointing over two longitudes separated by 5°
     */
    public void testSlewedTarget() throws OrekitException {

        // Spheric earth shape 
        OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 0., frameItrf2000B);
        
        //  Satellite position
        // ********************
        // Create satellite position as circular parameters
        CircularOrbit circ =
            new CircularOrbit(42164000.0, 0.5e-8, -0.5e-8, 0., 0.,
                                   Math.toRadians(5.300), CircularOrbit.MEAN_LONGITUDE_ARGUMENT, 
                                   Frame.getJ2000(), date, mu);
        
        // Transform satellite position to position/velocity parameters in J2000 frame
        PVCoordinates pvSatJ2000 = circ.getPVCoordinates();
        
        // Create nadir pointing attitude law 
        // ********************************** 
        NadirPointing nadirAttitudeLaw = new NadirPointing(earthShape);
        
        // Get observed ground point from nadir pointing law
        PVCoordinates pvNadirObservedJ2000 = nadirAttitudeLaw.getObservedGroundPoint(date, pvSatJ2000, Frame.getJ2000());
        PVCoordinates pvNadirObservedItrf2000B = j2000ToItrf.transformPVCoordinates(pvNadirObservedJ2000);
        
        GeodeticPoint geoNadirObserved = earthShape.transform(pvNadirObservedItrf2000B.getPosition(), frameItrf2000B, date);

        // Create target pointing attitude law with target equal to nadir target 
        // ********************************************************************* 
        TargetPointing targetLawRef = new TargetPointing(frameItrf2000B, pvNadirObservedItrf2000B);
        
        // Get attitude rotation in J2000
        Rotation rotSatRefJ2000 = targetLawRef.getState(date, pvSatJ2000, Frame.getJ2000()).getRotation();
      
        // Create target pointing attitude law with target 5° from nadir target 
        // ******************************************************************** 
        GeodeticPoint geoTarget = new GeodeticPoint(geoNadirObserved.getLongitude() - Math.toRadians(5),
                                                    geoNadirObserved.getLatitude(), geoNadirObserved.getAltitude());
        PVCoordinates pvTargetItrf2000B = new PVCoordinates(earthShape.transform(geoTarget), Vector3D.ZERO);
        TargetPointing targetLaw = new TargetPointing(frameItrf2000B, pvTargetItrf2000B);
        
        // Get attitude rotation 
        Rotation rotSatJ2000 = targetLaw.getState(date, pvSatJ2000, Frame.getJ2000()).getRotation();
        
        // Compute difference between both attitude laws 
        // *********************************************
        // Difference between attitudes
        //  expected
        double tanDeltaExpected = (6378136.460/(42164000.0-6378136.460))*Math.tan(Math.toRadians(5));
        double deltaExpected = Math.atan(tanDeltaExpected);
         
        //  real
        double deltaReal = rotSatJ2000.applyInverseTo(rotSatRefJ2000).getAngle();
        
        assertEquals(deltaReal, deltaExpected, 1.e-4);
        
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

            // Transform from J2000 to ITRF2000B
            j2000ToItrf = Frame.getJ2000().getTransformTo(frameItrf2000B, date);

        } catch (OrekitException oe) {
            fail(oe.getMessage());
        }

    }

    public void tearDown() {
        date = null;
        frameItrf2000B = null;
        j2000ToItrf = null;
    }

    public static Test suite() {
        return new TestSuite(TargetPointingTest.class);
    }
}

