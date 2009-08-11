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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.Vector3D;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.orbits.CircularOrbit;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Line;
import org.orekit.utils.PVCoordinates;


public class BodyCenterPointingTest {

    // Computation date 
    private AbsoluteDate date;
    
    // Orbit 
    private CircularOrbit circ;

    // Reference frame = ITRF 2005
    private Frame itrf;
    
    // Transform from EME2000 to ITRF2005
    private Transform eme2000ToItrf;
    
    // Earth center pointing attitude law 
    private BodyCenterPointing earthCenterAttitudeLaw;

    /** Test if target is body center
     */
    @Test
    public void testTarget() throws OrekitException {
        
        // Transform satellite position to position/velocity parameters in EME2000 frame 
        PVCoordinates pvSatEME2000 = circ.getPVCoordinates();
        
        // Call get target method 
        PVCoordinates target = earthCenterAttitudeLaw.getTargetInBodyFrame(date, pvSatEME2000, FramesFactory.getEME2000());

        // Check that target is body center
        double normPos = target.getPosition().getNorm();
        double normVel = target.getVelocity().getNorm();
        assertTrue((normPos < Utils.epsilonTest) && (normVel < Utils.epsilonTest));

    }

    /** Test if body center belongs to the direction pointed by the satellite
     */
    @Test
    public void testBodyCenterInPointingDirection() throws OrekitException {
        
        // Transform satellite position to position/velocity parameters in EME2000 frame
        PVCoordinates pvSatEME2000 = circ.getPVCoordinates();
        
        //  Pointing direction
        // ******************** 
        // Get satellite attitude rotation, i.e rotation from EME2000 frame to satellite frame
        Rotation rotSatEME2000 = earthCenterAttitudeLaw.getState(date, pvSatEME2000, FramesFactory.getEME2000()).getRotation();
        
        // Transform Z axis from satellite frame to EME2000 
        Vector3D zSatEME2000 = rotSatEME2000.applyInverseTo(Vector3D.PLUS_K);
        
        // Transform Z axis from EME2000 to ITRF2005
        Vector3D zSatITRF2005C = eme2000ToItrf.transformPosition(zSatEME2000);
        
        // Transform satellite position/velocity from EME2000 to ITRF2005 
        PVCoordinates pvSatITRF2005C = eme2000ToItrf.transformPVCoordinates(pvSatEME2000);
                
       // Line containing satellite point and following pointing direction
        Line pointingLine = new Line(pvSatITRF2005C.getPosition(), zSatITRF2005C);
        
        // Check that the line contains earth center (distance from line to point less than 1.e-8 m)
        double distance = pointingLine.distance(Vector3D.ZERO);
        
        assertTrue(distance < 1.e-8);
    }

    @Before
    public void setUp() {
        try {

            Utils.setDataRoot("regular-data");

            // Computation date
            date = new AbsoluteDate(new DateComponents(2008, 04, 07),
                                    TimeComponents.H00,
                                    TimeScalesFactory.getUTC());

            // Satellite position as circular parameters
            final double mu = 3.9860047e14;
            final double raan = 270.;
            circ =
                new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, Math.toRadians(50.), Math.toRadians(raan),
                                       Math.toRadians(5.300 - raan), CircularOrbit.MEAN_LONGITUDE_ARGUMENT,
                                       FramesFactory.getEME2000(), date, mu);
            
            
            // Reference frame = ITRF 2005
            itrf = FramesFactory.getITRF2005(true);

            // Transform from EME2000 to ITRF2005
            eme2000ToItrf = FramesFactory.getEME2000().getTransformTo(itrf, date);

            // Create earth center pointing attitude law */
            earthCenterAttitudeLaw = new BodyCenterPointing(itrf);
            
        } catch (OrekitException oe) {
            fail(oe.getMessage());
        }

    }

    @After
    public void tearDown() {
        date = null;
        itrf = null;
        eme2000ToItrf = null;
        earthCenterAttitudeLaw = null;
        circ = null;
    }

}

