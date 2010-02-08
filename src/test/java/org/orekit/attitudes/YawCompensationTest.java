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


public class YawCompensationTest {

    // Computation date 
    private AbsoluteDate date;
    
    // Reference frame = ITRF 2005C 
    private Frame frameITRF2005;
    
    // Satellite position
    CircularOrbit circOrbit;
    
    // Earth shape
    OneAxisEllipsoid earthShape;
    
    /** Test that pointed target remains the same with or without yaw compensation
     */
    @Test
    public void testTarget() throws OrekitException {

        //  Attitude laws
        // **************
        // Target pointing attitude law without yaw compensation
        NadirPointing nadirLaw = new NadirPointing(earthShape);
 
        // Target pointing attitude law with yaw compensation
        YawCompensation yawCompensLaw = new YawCompensation(nadirLaw);
       
        //  Check observed ground point
        // *****************************
        // without yaw compensation
        PVCoordinates noYawObserved = nadirLaw.getObservedGroundPoint(circOrbit, frameITRF2005);

        // with yaw compensation
        PVCoordinates yawObserved = yawCompensLaw.getObservedGroundPoint(circOrbit, frameITRF2005);

        // Check difference
        PVCoordinates observedDiff = new PVCoordinates(noYawObserved, yawObserved);
        double normObservedDiffPos = observedDiff.getPosition().getNorm();
        double normObservedDiffVel = observedDiff.getVelocity().getNorm();
       
        Assert.assertTrue((normObservedDiffPos < Utils.epsilonTest)  &&
                          (normObservedDiffVel < Utils.epsilonTest));
   }

    /** Test that maximum yaw compensation is at ascending/descending node, 
     * and minimum yaw compensation is at maximum latitude.
     */
    @Test
    public void testCompensMinMax() throws OrekitException {

        //  Attitude laws
        // **************
        // Target pointing attitude law over satellite nadir at date, without yaw compensation
        NadirPointing nadirLaw = new NadirPointing(earthShape);
 
        // Target pointing attitude law with yaw compensation
        YawCompensation yawCompensLaw = new YawCompensation(nadirLaw);

        
        // Extrapolation over one orbital period (sec)
        double duration = circOrbit.getKeplerianPeriod();
        KeplerianPropagator extrapolator = new KeplerianPropagator(circOrbit);
        
        // Extrapolation initializations
        double delta_t = 15.0; // extrapolation duration in seconds
        AbsoluteDate extrapDate = date; // extrapolation start date

        // Min initialization
        double yawMin = 1.e+12;
        double latMin = 0.;
        
        while (extrapDate.durationFrom(date) < duration)  {
            extrapDate = extrapDate.shiftedBy(delta_t);

            // Extrapolated orbit state at date
            SpacecraftState extrapOrbit = extrapolator.propagate(extrapDate);
            PVCoordinates extrapPvSatEME2000 = extrapOrbit.getPVCoordinates();
            
            // Satellite latitude at date
            double extrapLat = earthShape.transform(extrapPvSatEME2000.getPosition(), FramesFactory.getEME2000(), extrapDate).getLatitude();
            
            // Compute yaw compensation angle -- rotations composition
            double yawAngle = yawCompensLaw.getYawAngle(circOrbit);
                        
            // Update minimum yaw compensation angle
            if (Math.abs(yawAngle) <= yawMin) {
                yawMin = Math.abs(yawAngle);
                latMin = extrapLat;
            }           

            //     Checks
            // ------------------
            
            // 1/ Check yaw values around ascending node (max yaw)
            if ((Math.abs(extrapLat) < Math.toRadians(2.)) &&
                (extrapPvSatEME2000.getVelocity().getZ() >= 0. )) {
                Assert.assertTrue((Math.abs(yawAngle) >= Math.toRadians(2.8488)) 
                        && (Math.abs(yawAngle) <= Math.toRadians(2.8532)));
            }
            
            // 2/ Check yaw values around maximum positive latitude (min yaw)
            if ( extrapLat > Math.toRadians(50.) ) {
                Assert.assertTrue((Math.abs(yawAngle) <= Math.toRadians(0.2628)) 
                        && (Math.abs(yawAngle) >= Math.toRadians(0.0001)));
            }
            
            // 3/ Check yaw values around descending node (max yaw)
            if ( (Math.abs(extrapLat) < Math.toRadians(2.))
                    && (extrapPvSatEME2000.getVelocity().getZ() <= 0. ) ) {
                System.out.println(Math.toDegrees(Math.abs(yawAngle)));
                Assert.assertTrue((Math.abs(yawAngle) >= Math.toRadians(2.8485)) 
                             && (Math.abs(yawAngle) <= Math.toRadians(2.8536)));
            }
         
            // 4/ Check yaw values around maximum negative latitude (min yaw)
            if ( extrapLat < Math.toRadians(-50.) ) {
                Assert.assertTrue((Math.abs(yawAngle) <= Math.toRadians(0.2359)) 
                             && (Math.abs(yawAngle) >= Math.toRadians(0.0141)));
            }

        }
        
        // 5/ Check that minimum yaw compensation value is around maximum latitude
        Assert.assertEquals(Math.toRadians( 0.003229), yawMin, Utils.epsilonAngle);
        Assert.assertEquals(Math.toRadians(50.214853), latMin, Utils.epsilonAngle);

    }

    /** Test that compensation rotation axis is Zsat, yaw axis
     */
    @Test
    public void testCompensAxis() throws OrekitException {

        //  Attitude laws
        // **************
        // Target pointing attitude law over satellite nadir at date, without yaw compensation
        NadirPointing nadirLaw = new NadirPointing(earthShape);
 
        // Target pointing attitude law with yaw compensation
        YawCompensation yawCompensLaw = new YawCompensation(nadirLaw);

        // Get attitude rotations from non yaw compensated / yaw compensated laws
        Rotation rotNoYaw = nadirLaw.getState(circOrbit).getRotation();
        Rotation rotYaw = yawCompensLaw.getState(circOrbit).getRotation();
            
        // Compose rotations composition
        Rotation compoRot = rotYaw.applyTo(rotNoYaw.revert());
        Vector3D yawAxis = compoRot.getAxis();

        // Check axis
        Assert.assertEquals(0., yawAxis.getX(), Utils.epsilonTest);
        Assert.assertEquals(0., yawAxis.getY(), Utils.epsilonTest);
        Assert.assertEquals(1., yawAxis.getZ(), Utils.epsilonTest);

    }

    @Test
    public void testSpin() throws OrekitException {

        NadirPointing nadirLaw = new NadirPointing(earthShape);
        
        // Target pointing attitude law with yaw compensation
        AttitudeLaw law = new YawCompensation(nadirLaw);

        KeplerianOrbit orbit =
            new KeplerianOrbit(7178000.0, 1.e-4, Math.toRadians(50.),
                              Math.toRadians(10.), Math.toRadians(20.),
                              Math.toRadians(30.), KeplerianOrbit.MEAN_ANOMALY, 
                              FramesFactory.getEME2000(),
                              date.shiftedBy(-300.0), 3.986004415e14);

        Propagator propagator = new KeplerianPropagator(orbit, law);

        double h = 0.01;
        SpacecraftState sMinus = propagator.propagate(date.shiftedBy(-h));
        SpacecraftState s0     = propagator.propagate(date);
        SpacecraftState sPlus  = propagator.propagate(date.shiftedBy(h));

        Vector3D spin0 = s0.getAttitude().getSpin();
        Vector3D reference = Attitude.estimateSpin(sMinus.getAttitude().getRotation(),
                                                   sPlus.getAttitude().getRotation(),
                                                   2 * h);
        System.out.println(spin0.getNorm() + " " + (2 * Math.PI / spin0.getNorm()));
        Assert.assertTrue(spin0.getNorm() > 1.0e-3);
        Assert.assertEquals(0.0, spin0.subtract(reference).getNorm(), 2.0e-8);

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
            final double mu = 3.9860047e14;
            
            // Reference frame = ITRF 2005
            frameITRF2005 = FramesFactory.getITRF2005(true);

            //  Satellite position
            circOrbit =
                new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, Math.toRadians(50.), Math.toRadians(270.),
                                       Math.toRadians(5.300), CircularOrbit.MEAN_LONGITUDE_ARGUMENT, 
                                       FramesFactory.getEME2000(), date, mu);
            
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
        earthShape = null;
    }

}

