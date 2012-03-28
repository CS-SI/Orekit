/* Copyright 2002-2011 CS Communication & Systèmes
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


import org.apache.commons.math3.geometry.euclidean.threed.Line;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;
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
import org.orekit.frames.Transform;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
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
        // Target pointing attitude provider without yaw compensation
        NadirPointing nadirLaw = new NadirPointing(earthShape);

        // Target pointing attitude provider with yaw compensation
        YawCompensation yawCompensLaw = new YawCompensation(nadirLaw);

        //  Check target
        // *************
        // without yaw compensation
        Vector3D noYawObserved = nadirLaw.getTargetPoint(circOrbit, date, frameITRF2005);

        // with yaw compensation
        Vector3D yawObserved = yawCompensLaw.getTargetPoint(circOrbit, date, frameITRF2005);

        // Check difference
        Vector3D observedDiff = noYawObserved.subtract(yawObserved);

        Assert.assertEquals(0.0, observedDiff.getNorm(), Utils.epsilonTest);

    }

    /** Test that pointed target motion is along -X sat axis
     */
    @Test
    public void testAlignment() throws OrekitException {

        Frame inertFrame  = circOrbit.getFrame();
        Frame earthFrame  = earthShape.getBodyFrame();
        AttitudeProvider law   = new YawCompensation(new NadirPointing(earthShape));
        Attitude att0     = law.getAttitude(circOrbit, date, circOrbit.getFrame());

        // ground point in satellite Z direction
        Vector3D satInert = circOrbit.getPVCoordinates().getPosition();
        Vector3D zInert   = att0.getRotation().applyInverseTo(Vector3D.PLUS_K);
        GeodeticPoint gp  = earthShape.getIntersectionPoint(new Line(satInert,
                                                                     satInert.add(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, zInert)),
                                                            satInert,
                                                            inertFrame, circOrbit.getDate());
        Vector3D pEarth   = earthShape.transform(gp);

        // velocity of ground point, in inertial frame
        double h = 1.0;
        double s2 = 1.0 / (12 * h);
        double s1 = 8.0 * s2;
        Transform tM2h = earthFrame.getTransformTo(inertFrame, circOrbit.getDate().shiftedBy(-2 * h));
        Vector3D pM2h = tM2h.transformPosition(pEarth);
        Transform tM1h = earthFrame.getTransformTo(inertFrame, circOrbit.getDate().shiftedBy(-h));
        Vector3D pM1h = tM1h.transformPosition(pEarth);
        Transform tP1h = earthFrame.getTransformTo(inertFrame, circOrbit.getDate().shiftedBy( h));
        Vector3D pP1h = tP1h.transformPosition(pEarth);
        Transform tP2h = earthFrame.getTransformTo(inertFrame, circOrbit.getDate().shiftedBy( 2 * h));
        Vector3D pP2h = tP2h.transformPosition(pEarth);
        Vector3D velInert = new Vector3D(s1, pP1h, -s1, pM1h, -s2, pP2h, s2, pM2h);
        Vector3D relativeVelocity = velInert.subtract(circOrbit.getPVCoordinates().getVelocity());

        // relative velocity in satellite frame, must be in (X, Z) plane
        Vector3D relVelSat = att0.getRotation().applyTo(relativeVelocity);
        Assert.assertEquals(0.0, relVelSat.getY(), 2.0e-5);

    }

    /** Test that maximum yaw compensation is at ascending/descending node,
     * and minimum yaw compensation is at maximum latitude.
     */
    @Test
    public void testCompensMinMax() throws OrekitException {

        //  Attitude laws
        // **************
        // Target pointing attitude provider over satellite nadir at date, without yaw compensation
        NadirPointing nadirLaw = new NadirPointing(earthShape);

        // Target pointing attitude provider with yaw compensation
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
            Orbit extrapOrbit = extrapolator.propagate(extrapDate).getOrbit();
            PVCoordinates extrapPvSatEME2000 = extrapOrbit.getPVCoordinates();

            // Satellite latitude at date
            double extrapLat =
                earthShape.transform(extrapPvSatEME2000.getPosition(), FramesFactory.getEME2000(), extrapDate).getLatitude();

            // Compute yaw compensation angle -- rotations composition
            double yawAngle = yawCompensLaw.getYawAngle(extrapOrbit, extrapDate, extrapOrbit.getFrame());

            // Update minimum yaw compensation angle
            if (FastMath.abs(yawAngle) <= yawMin) {
                yawMin = FastMath.abs(yawAngle);
                latMin = extrapLat;
            }

            //     Checks
            // ------------------

            // 1/ Check yaw values around ascending node (max yaw)
            if ((FastMath.abs(extrapLat) < FastMath.toRadians(20.)) &&
                (extrapPvSatEME2000.getVelocity().getZ() >= 0. )) {
                Assert.assertTrue((FastMath.abs(yawAngle) >= FastMath.toRadians(2.51)) &&
                                  (FastMath.abs(yawAngle) <= FastMath.toRadians(2.86)));
            }

            // 2/ Check yaw values around maximum positive latitude (min yaw)
            if ( extrapLat > FastMath.toRadians(50.) ) {
                Assert.assertTrue(FastMath.abs(yawAngle) <= FastMath.toRadians(0.26));
            }

            // 3/ Check yaw values around descending node (max yaw)
            if ( (FastMath.abs(extrapLat) < FastMath.toRadians(2.))
                    && (extrapPvSatEME2000.getVelocity().getZ() <= 0. ) ) {
                Assert.assertTrue((FastMath.abs(yawAngle) >= FastMath.toRadians(2.51)) &&
                                  (FastMath.abs(yawAngle) <= FastMath.toRadians(2.86)));
            }

            // 4/ Check yaw values around maximum negative latitude (min yaw)
            if ( extrapLat < FastMath.toRadians(-50.) ) {
                Assert.assertTrue(FastMath.abs(yawAngle) <= FastMath.toRadians(0.26));
            }

        }

        // 5/ Check that minimum yaw compensation value is around maximum latitude
        Assert.assertEquals(0.0, FastMath.toDegrees(yawMin), 0.004);
        Assert.assertEquals(50.0, FastMath.toDegrees(latMin), 0.22);

    }

    /** Test that compensation rotation axis is Zsat, yaw axis
     */
    @Test
    public void testCompensAxis() throws OrekitException {

        //  Attitude laws
        // **************
        // Target pointing attitude provider over satellite nadir at date, without yaw compensation
        NadirPointing nadirLaw = new NadirPointing(earthShape);

        // Target pointing attitude provider with yaw compensation
        YawCompensation yawCompensLaw = new YawCompensation(nadirLaw);

        // Get attitude rotations from non yaw compensated / yaw compensated laws
        Rotation rotNoYaw = nadirLaw.getAttitude(circOrbit, date, circOrbit.getFrame()).getRotation();
        Rotation rotYaw = yawCompensLaw.getAttitude(circOrbit, date, circOrbit.getFrame()).getRotation();

        // Compose rotations composition
        Rotation compoRot = rotYaw.applyTo(rotNoYaw.revert());
        Vector3D yawAxis = compoRot.getAxis();

        // Check axis
        Assert.assertEquals(0., yawAxis.subtract(Vector3D.PLUS_K).getNorm(), Utils.epsilonTest);

    }

    @Test
    public void testSpin() throws OrekitException {

        NadirPointing nadirLaw = new NadirPointing(earthShape);

        // Target pointing attitude provider with yaw compensation
        AttitudeProvider law = new YawCompensation(nadirLaw);

        KeplerianOrbit orbit =
            new KeplerianOrbit(7178000.0, 1.e-4, FastMath.toRadians(50.),
                              FastMath.toRadians(10.), FastMath.toRadians(20.),
                              FastMath.toRadians(30.), PositionAngle.MEAN,
                              FramesFactory.getEME2000(),
                              date.shiftedBy(-300.0), 3.986004415e14);

        Propagator propagator = new KeplerianPropagator(orbit, law);

        double h = 0.01;
        SpacecraftState sMinus = propagator.propagate(date.shiftedBy(-h));
        SpacecraftState s0     = propagator.propagate(date);
        SpacecraftState sPlus  = propagator.propagate(date.shiftedBy(h));

        // check spin is consistent with attitude evolution
        double errorAngleMinus     = Rotation.distance(sMinus.shiftedBy(h).getAttitude().getRotation(),
                                                       s0.getAttitude().getRotation());
        double evolutionAngleMinus = Rotation.distance(sMinus.getAttitude().getRotation(),
                                                       s0.getAttitude().getRotation());
        Assert.assertEquals(0.0, errorAngleMinus, 1.0e-6 * evolutionAngleMinus);
        double errorAnglePlus      = Rotation.distance(s0.getAttitude().getRotation(),
                                                       sPlus.shiftedBy(-h).getAttitude().getRotation());
        double evolutionAnglePlus  = Rotation.distance(s0.getAttitude().getRotation(),
                                                       sPlus.getAttitude().getRotation());
        Assert.assertEquals(0.0, errorAnglePlus, 1.0e-6 * evolutionAnglePlus);

        Vector3D spin0 = s0.getAttitude().getSpin();
        Vector3D reference = Attitude.estimateSpin(sMinus.getAttitude().getRotation(),
                                                   sPlus.getAttitude().getRotation(),
                                                   2 * h);
        Assert.assertTrue(spin0.getNorm() > 1.0e-3);
        Assert.assertEquals(0.0, spin0.subtract(reference).getNorm(), 1.0e-9);

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
                new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, FastMath.toRadians(50.), FastMath.toRadians(270.),
                                       FastMath.toRadians(5.300), PositionAngle.MEAN,
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

