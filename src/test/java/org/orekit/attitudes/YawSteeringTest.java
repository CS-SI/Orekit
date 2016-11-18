/* Copyright 2002-2016 CS Systèmes d'Information
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



import java.util.ArrayList;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
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
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.AngularCoordinates;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedPVCoordinates;


public class YawSteeringTest {

    // Computation date
    private AbsoluteDate date;

    // Reference frame = ITRF
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
        NadirPointing nadirLaw = new NadirPointing(circOrbit.getFrame(), earthShape);

        // Target pointing attitude provider with yaw compensation
        YawSteering yawCompensLaw =
            new YawSteering(circOrbit.getFrame(), nadirLaw, CelestialBodyFactory.getSun(), Vector3D.MINUS_I);

        //  Check observed ground point
        // *****************************
        TimeStampedPVCoordinates noYawObserved = nadirLaw.getTargetPV(circOrbit, date, itrf);

        // with yaw compensation
        TimeStampedPVCoordinates yawObserved = yawCompensLaw.getTargetPV(circOrbit, date, itrf);

        // Check difference
        PVCoordinates observedDiff = new PVCoordinates(yawObserved, noYawObserved);

        Assert.assertEquals(0.0, observedDiff.getPosition().getNorm(), Utils.epsilonTest);
        Assert.assertEquals(0.0, observedDiff.getVelocity().getNorm(), Utils.epsilonTest);
        Assert.assertEquals(0.0, observedDiff.getAcceleration().getNorm(), Utils.epsilonTest);
        Assert.assertSame(nadirLaw, yawCompensLaw.getUnderlyingAttitudeProvider());

    }

    @Test
    public void testSunAligned() throws OrekitException {

        //  Attitude laws
        // **************
        // Target pointing attitude provider over satellite nadir at date, without yaw compensation
        NadirPointing nadirLaw = new NadirPointing(circOrbit.getFrame(), earthShape);

        // Target pointing attitude provider with yaw compensation
        PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        YawSteering yawCompensLaw = new YawSteering(circOrbit.getFrame(), nadirLaw, sun, Vector3D.MINUS_I);

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
        NadirPointing nadirLaw = new NadirPointing(circOrbit.getFrame(), earthShape);

        // Target pointing attitude provider with yaw compensation
        YawSteering yawCompensLaw =
            new YawSteering(circOrbit.getFrame(), nadirLaw, CelestialBodyFactory.getSun(), Vector3D.MINUS_I);

        // Get attitude rotations from non yaw compensated / yaw compensated laws
        Rotation rotNoYaw = nadirLaw.getAttitude(circOrbit, date, circOrbit.getFrame()).getRotation();
        Rotation rotYaw = yawCompensLaw.getAttitude(circOrbit, date, circOrbit.getFrame()).getRotation();

        // Compose rotations composition
        Rotation compoRot = rotYaw.compose(rotNoYaw.revert(), RotationConvention.VECTOR_OPERATOR);
        Vector3D yawAxis = compoRot.getAxis(RotationConvention.VECTOR_OPERATOR);

        // Check axis
        Assert.assertEquals(0., yawAxis.getX(), Utils.epsilonTest);
        Assert.assertEquals(0., yawAxis.getY(), Utils.epsilonTest);
        Assert.assertEquals(1., yawAxis.getZ(), Utils.epsilonTest);

    }

    /** Test the derivatives of the sliding target
     */
    @Test
    public void testSlidingDerivatives() throws OrekitException {

        GroundPointing law = new YawSteering(circOrbit.getFrame(),
                                             new NadirPointing(circOrbit.getFrame(), earthShape),
                                             CelestialBodyFactory.getSun(),
                                             Vector3D.MINUS_I);

        List<TimeStampedPVCoordinates> sample = new ArrayList<TimeStampedPVCoordinates>();
        for (double dt = -0.1; dt < 0.1; dt += 0.05) {
            Orbit o = circOrbit.shiftedBy(dt);
            sample.add(law.getTargetPV(o, o.getDate(), o.getFrame()));
        }
        TimeStampedPVCoordinates reference =
                TimeStampedPVCoordinates.interpolate(circOrbit.getDate(),
                                                     CartesianDerivativesFilter.USE_P, sample);

        TimeStampedPVCoordinates target =
                law.getTargetPV(circOrbit, circOrbit.getDate(), circOrbit.getFrame());

        Assert.assertEquals(0.0,
                            Vector3D.distance(reference.getPosition(),     target.getPosition()),
                            1.0e-15 * reference.getPosition().getNorm());
        Assert.assertEquals(0.0,
                            Vector3D.distance(reference.getVelocity(),     target.getVelocity()),
                            4.0e-11 * reference.getVelocity().getNorm());
        Assert.assertEquals(0.0,
                            Vector3D.distance(reference.getAcceleration(), target.getAcceleration()),
                            8.0e-6 * reference.getAcceleration().getNorm());

    }

    @Test
    public void testSpin() throws OrekitException {

        NadirPointing nadirLaw = new NadirPointing(circOrbit.getFrame(), earthShape);

        // Target pointing attitude provider with yaw compensation
        AttitudeProvider law = new YawSteering(circOrbit.getFrame(), nadirLaw, CelestialBodyFactory.getSun(), Vector3D.MINUS_I);

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
        Assert.assertEquals(0.0, errorAngleMinus, 1.0e-9 * evolutionAngleMinus);
        double errorAnglePlus      = Rotation.distance(s0.getAttitude().getRotation(),
                                                       sPlus.shiftedBy(-h).getAttitude().getRotation());
        double evolutionAnglePlus  = Rotation.distance(s0.getAttitude().getRotation(),
                                                       sPlus.getAttitude().getRotation());
        Assert.assertEquals(0.0, errorAnglePlus, 1.0e-9 * evolutionAnglePlus);

        Vector3D spin0 = s0.getAttitude().getSpin();
        Vector3D reference = AngularCoordinates.estimateRate(sMinus.getAttitude().getRotation(),
                                                             sPlus.getAttitude().getRotation(),
                                                             2 * h);
        Assert.assertTrue(spin0.getNorm() > 1.0e-3);
        Assert.assertEquals(0.0, spin0.subtract(reference).getNorm(), 5.0e-13);

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

            // Reference frame = ITRF
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

