/* Copyright 2002-2013 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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



import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.AngularCoordinates;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinatesProvider;


public class YawSteeringTest {

    // Computation date
    private AbsoluteDate date;

    // Reference frame = ITRF 2005C
    private Frame itrf;

    // Satellite position
    CircularOrbit circOrbit;

    // Earth shape
    OneAxisEllipsoid earthShape;

    @Test
    public void testTarget() throws OrekitException {

        //  Attitude laws
        // **************
        // Target pointing attitude provider without yaw compensation
        NadirPointing nadirLaw = new NadirPointing(earthShape);

        // Target pointing attitude provider with yaw compensation
        YawSteering yawCompensLaw =
            new YawSteering(nadirLaw, CelestialBodyFactory.getSun(), Vector3D.MINUS_I);

        //  Check observed ground point
        // *****************************
        // without yaw compensation
        Vector3D noYawObserved = nadirLaw.getTargetPoint(circOrbit, date, itrf);

        // with yaw compensation
        Vector3D yawObserved = yawCompensLaw.getTargetPoint(circOrbit, date, itrf);

        // Check difference
        Vector3D observedDiff = noYawObserved.subtract(yawObserved);

        Assert.assertTrue(observedDiff.getNorm() < Utils.epsilonTest);
   }

    @Test
    public void testSunAligned() throws OrekitException {

        //  Attitude laws
        // **************
        // Target pointing attitude provider over satellite nadir at date, without yaw compensation
        NadirPointing nadirLaw = new NadirPointing(earthShape);

        // Target pointing attitude provider with yaw compensation
        PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        YawSteering yawCompensLaw = new YawSteering(nadirLaw, sun, Vector3D.MINUS_I);

        // Get sun direction in satellite frame
        Rotation rotYaw = yawCompensLaw.getAttitude(circOrbit, date, circOrbit.getFrame()).getRotation();
        Vector3D sunEME2000 = sun.getPVCoordinates(date, FramesFactory.getEME2000()).getPosition();
        Vector3D sunSat = rotYaw.applyTo(sunEME2000);

        // Check sun is in (X,Z) plane
        Assert.assertEquals(0.0, FastMath.sin(sunSat.getAlpha()), 1.0e-7);

    }

    @Test
    public void testCompensAxis() throws OrekitException {

        //  Attitude laws
        // **************
        // Target pointing attitude provider over satellite nadir at date, without yaw compensation
        NadirPointing nadirLaw = new NadirPointing(earthShape);

        // Target pointing attitude provider with yaw compensation
        YawSteering yawCompensLaw =
            new YawSteering(nadirLaw, CelestialBodyFactory.getSun(), Vector3D.MINUS_I);

        // Get attitude rotations from non yaw compensated / yaw compensated laws
        Rotation rotNoYaw = nadirLaw.getAttitude(circOrbit, date, circOrbit.getFrame()).getRotation();
        Rotation rotYaw = yawCompensLaw.getAttitude(circOrbit, date, circOrbit.getFrame()).getRotation();

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

        // Target pointing attitude provider with yaw compensation
        AttitudeProvider law = new YawSteering(nadirLaw, CelestialBodyFactory.getSun(), Vector3D.MINUS_I);

        KeplerianOrbit orbit =
            new KeplerianOrbit(7178000.0, 1.e-4, FastMath.toRadians(50.),
                              FastMath.toRadians(10.), FastMath.toRadians(20.),
                              FastMath.toRadians(30.), PositionAngle.MEAN,
                              FramesFactory.getEME2000(),
                              date.shiftedBy(-300.0),
                              3.986004415e14);

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
        Assert.assertEquals(0.0, errorAngleMinus, 1.0e-5 * evolutionAngleMinus);
        double errorAnglePlus      = Rotation.distance(s0.getAttitude().getRotation(),
                                                       sPlus.shiftedBy(-h).getAttitude().getRotation());
        double evolutionAnglePlus  = Rotation.distance(s0.getAttitude().getRotation(),
                                                       sPlus.getAttitude().getRotation());
        Assert.assertEquals(0.0, errorAnglePlus, 1.0e-5 * evolutionAnglePlus);

        Vector3D spin0 = s0.getAttitude().getSpin();
        Vector3D reference = AngularCoordinates.estimateRate(sMinus.getAttitude().getRotation(),
                                                             sPlus.getAttitude().getRotation(),
                                                             2 * h);
        Assert.assertTrue(spin0.getNorm() > 1.0e-3);
        Assert.assertEquals(0.0, spin0.subtract(reference).getNorm(), 2.0e-12);

    }

    @Before
    public void setUp() {
        try {
            Utils.setDataRoot("regular-data");

            // Computation date
            date = new AbsoluteDate(new DateComponents(1970, 04, 07),
                                    TimeComponents.H00,
                                    TimeScalesFactory.getUTC());

            // Body mu
            final double mu = 3.9860047e14;

            // Reference frame = ITRF 2005
            itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);

            //  Satellite position
            circOrbit =
                new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, FastMath.toRadians(50.), FastMath.toRadians(270.),
                                       FastMath.toRadians(5.300), PositionAngle.MEAN,
                                       FramesFactory.getEME2000(), date, mu);

            // Elliptic earth shape */
            earthShape =
                new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, itrf);

        } catch (OrekitException oe) {
            Assert.fail(oe.getMessage());
        }

    }

    @After
    public void tearDown() {
        date = null;
        itrf = null;
        circOrbit = null;
        earthShape = null;
    }

}

