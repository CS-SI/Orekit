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
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.orbits.CircularOrbit;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChunkedDate;
import org.orekit.time.ChunkedTime;
import org.orekit.time.UTCScale;
import org.orekit.utils.Line;
import org.orekit.utils.PVCoordinates;


public class BodyCenterPointingTest extends TestCase {

    // Computation date 
    private AbsoluteDate date;
    
    // Orbit 
    private CircularOrbit circ;

    // Reference frame = ITRF 2000B 
    private Frame frameItrf2000B;
    
    // Transform from J2000 to ITRF2000B 
    private Transform j2000ToItrf;
    
    // Earth center pointing attitude law 
    private BodyCenterPointing earthCenterAttitudeLaw;

    /** Test class for body center pointing attitude law.
     */
    public BodyCenterPointingTest(String name) {
        super(name);
    }

    /** Test if target is body center
     */
    public void testTarget() throws OrekitException {
        
        // Transform satellite position to position/velocity parameters in J2000 frame 
        PVCoordinates pvSatJ2000 = circ.getPVCoordinates();
        
        // Call get target method 
        PVCoordinates target = earthCenterAttitudeLaw.getTargetInBodyFrame(date, pvSatJ2000, Frame.getJ2000());

        // Check that target is body center
        double normPos = target.getPosition().getNorm();
        double normVel = target.getVelocity().getNorm();
        assertTrue((normPos < Utils.epsilonTest) && (normVel < Utils.epsilonTest));

    }


    /** Test if body center belongs to the direction pointed by the satellite
     */
    public void testBodyCenterInPointingDirection() throws OrekitException {
        
        // Transform satellite position to position/velocity parameters in J2000 frame
        PVCoordinates pvSatJ2000 = circ.getPVCoordinates();
        
        //  Pointing direction
        // ******************** 
        // Get satellite attitude rotation, i.e rotation from J2000 frame to satellite frame
        Rotation rotSatJ2000 = earthCenterAttitudeLaw.getState(date, pvSatJ2000, Frame.getJ2000()).getRotation();
        
        // Transform Z axis from satellite frame to J2000 
        Vector3D zSatJ2000 = rotSatJ2000.applyInverseTo(Vector3D.PLUS_K);
        
        // Transform Z axis from J2000 to ITRF2000B
        Vector3D zSatItrf2000B = j2000ToItrf.transformPosition(zSatJ2000);
        
        // Transform satellite position/velocity from J2000 to ITRF2000B 
        PVCoordinates pvSatItrf2000B = j2000ToItrf.transformPVCoordinates(pvSatJ2000);
                
       // Line containing satellite point and following pointing direction
        Line pointingLine = new Line(pvSatItrf2000B.getPosition(), zSatItrf2000B);
        
        // Check that the line contains earth center (distance from line to point less than 1.e-8 m)
        double distance = pointingLine.distance(Vector3D.ZERO);
        
        assertTrue(distance < 1.e-8);
    }

    public void setUp() {
        try {
            // Computation date
            date = new AbsoluteDate(new ChunkedDate(2008, 04, 07),
                                    ChunkedTime.H00,
                                    UTCScale.getInstance());

            // Satellite position as circular parameters
            final double mu = 3.9860047e14;
            final double raan = 270.;
            circ =
                new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, Math.toRadians(50.), Math.toRadians(raan),
                                       Math.toRadians(5.300 - raan), CircularOrbit.MEAN_LONGITUDE_ARGUMENT,
                                       Frame.getJ2000(), date, mu);
            
            
            // Reference frame = ITRF 2000B
            frameItrf2000B = Frame.getITRF2000B();

            // Transform from J2000 to ITRF2000B
            j2000ToItrf = Frame.getJ2000().getTransformTo(frameItrf2000B, date);

            // Create earth center pointing attitude law */
            earthCenterAttitudeLaw = new BodyCenterPointing(frameItrf2000B);
            
        } catch (OrekitException oe) {
            fail(oe.getMessage());
        }

    }

    public void tearDown() {
        date = null;
        frameItrf2000B = null;
        j2000ToItrf = null;
        earthCenterAttitudeLaw = null;
        circ = null;
    }

    public static Test suite() {
        return new TestSuite(BodyCenterPointingTest.class);
    }

}

