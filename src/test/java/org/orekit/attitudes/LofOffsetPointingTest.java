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
import org.apache.commons.math.geometry.RotationOrder;
import org.apache.commons.math.geometry.Vector3D;
import org.orekit.Utils;
import org.orekit.attitudes.BodyCenterPointing;
import org.orekit.attitudes.LofOffset;
import org.orekit.attitudes.LofOffsetPointing;
import org.orekit.attitudes.NadirPointing;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.orbits.CircularOrbit;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChunkedDate;
import org.orekit.time.ChunkedTime;
import org.orekit.time.UTCScale;
import org.orekit.utils.PVCoordinates;


public class LofOffsetPointingTest extends TestCase {

    // Computation date 
    private AbsoluteDate date;
    
    // Body mu 
    private double mu;

    // Reference frame = ITRF 2000B 
    private Frame frameItrf2000B;
        
    // Earth shape
    OneAxisEllipsoid earthSpheric;
    
    /** Test class for body center pointing attitude law.
     */
    public LofOffsetPointingTest(String name) {
        super(name);
    }

    /** Test if both constructors are equivalent
     */
    public void testLof() throws OrekitException {

        //  Satellite position
        final CircularOrbit circ =
            new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, Math.toRadians(0.), Math.toRadians(270.),
                                   Math.toRadians(5.300), CircularOrbit.MEAN_LONGITUDE_ARGUMENT, 
                                   Frame.getJ2000(), date, mu);
        final PVCoordinates pvSatJ2000 = circ.getPVCoordinates();

        // Create lof aligned law
        //************************
        final LofOffset lofLaw = LofOffset.LOF_ALIGNED;
        final LofOffsetPointing lofPointing = new LofOffsetPointing(earthSpheric, lofLaw, Vector3D.PLUS_K);
        final Rotation lofRot = lofPointing.getState(date, pvSatJ2000, Frame.getJ2000()).getRotation();
 
        // Compare to body center pointing law
        //*************************************
        final BodyCenterPointing centerLaw = new BodyCenterPointing(earthSpheric.getBodyFrame());
        final Rotation centerRot = centerLaw.getState(date, pvSatJ2000, Frame.getJ2000()).getRotation();
        final double angleBodyCenter = centerRot.applyInverseTo(lofRot).getAngle();
        assertEquals(0., angleBodyCenter, Utils.epsilonAngle);

        // Compare to nadir pointing law
        //*******************************
        final NadirPointing nadirLaw = new NadirPointing(earthSpheric);
        final Rotation nadirRot = nadirLaw.getState(date, pvSatJ2000, Frame.getJ2000()).getRotation();
        final double angleNadir = nadirRot.applyInverseTo(lofRot).getAngle();
        assertEquals(0., angleNadir, Utils.epsilonAngle);

    } 

    public void testMiss() {
        final CircularOrbit circ =
            new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, Math.toRadians(0.), Math.toRadians(270.),
                                   Math.toRadians(5.300), CircularOrbit.MEAN_LONGITUDE_ARGUMENT, 
                                   Frame.getJ2000(), date, mu);
        final LofOffset upsideDown = new LofOffset(RotationOrder.XYX, Math.PI, 0, 0);
        try {
            final LofOffsetPointing pointing = new LofOffsetPointing(earthSpheric, upsideDown, Vector3D.PLUS_K);
            pointing.getObservedGroundPoint(circ.getDate(), circ.getPVCoordinates(), circ.getFrame());
            fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            // expected behavior
        } catch (Exception e) {
            fail("wrong exception caught");
        }
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
        return new TestSuite(LofOffsetPointingTest.class);
    }
}

