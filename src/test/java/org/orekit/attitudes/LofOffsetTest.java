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


import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
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
import org.orekit.frames.LOFType;
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
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;


public class LofOffsetTest {

    // Computation date
    private AbsoluteDate date;

    // Body mu
    private double mu;

    // Reference frame = ITRF
    private Frame itrf;

    // Earth shape
    OneAxisEllipsoid earthSpheric;

    //  Satellite position
    CircularOrbit orbit;
    PVCoordinates pvSatEME2000;

    /** Test is the lof offset is the one expected
     */
    @Test
    public void testZero() throws OrekitException {

        //  Satellite position

        // Lof aligned attitude provider
        final LofOffset lofAlignedLaw = new LofOffset(orbit.getFrame(), LOFType.VVLH);
        final Rotation lofOffsetRot = lofAlignedLaw.getAttitude(orbit, date, orbit.getFrame()).getRotation();

        // Check that
        final Vector3D momentumEME2000 = pvSatEME2000.getMomentum();
        final Vector3D momentumLof = lofOffsetRot.applyTo(momentumEME2000);
        final double cosinus = FastMath.cos(Vector3D.dotProduct(momentumLof, Vector3D.PLUS_K));
        Assert.assertEquals(1., cosinus, Utils.epsilonAngle);

    }
    /** Test if the lof offset is the one expected
     */
    @Test
    public void testOffset() throws OrekitException {

        //  Satellite position
        final CircularOrbit circ =
           new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, FastMath.toRadians(0.), FastMath.toRadians(270.),
                                   FastMath.toRadians(5.300), PositionAngle.MEAN,
                                   FramesFactory.getEME2000(), date, mu);

        // Create target pointing attitude provider
        // ************************************
        // Elliptic earth shape
        final OneAxisEllipsoid earthShape = new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, itrf);
        final GeodeticPoint geoTargetITRF = new GeodeticPoint(FastMath.toRadians(43.36), FastMath.toRadians(1.26), 600.);

        // Attitude law definition from geodetic point target
        final TargetPointing targetLaw = new TargetPointing(circ.getFrame(), geoTargetITRF, earthShape);
        final Rotation targetRot = targetLaw.getAttitude(circ, date, circ.getFrame()).getRotation();

        // Create lof aligned attitude provider
        // *******************************
        final LofOffset lofAlignedLaw = new LofOffset(orbit.getFrame(), LOFType.VVLH);
        final Rotation lofAlignedRot = lofAlignedLaw.getAttitude(circ, date, circ.getFrame()).getRotation();

        // Get rotation from LOF to target pointing attitude
        Rotation rollPitchYaw = targetRot.compose(lofAlignedRot.revert(), RotationConvention.VECTOR_OPERATOR).revert();
        final double[] angles = rollPitchYaw.getAngles(RotationOrder.ZYX, RotationConvention.VECTOR_OPERATOR);
        final double yaw = angles[0];
        final double pitch = angles[1];
        final double roll = angles[2];

        // Create lof offset attitude provider with computed roll, pitch, yaw
        // **************************************************************
        final LofOffset lofOffsetLaw = new LofOffset(orbit.getFrame(), LOFType.VVLH, RotationOrder.ZYX, yaw, pitch, roll);
        final Rotation lofOffsetRot = lofOffsetLaw.getAttitude(circ, date, circ.getFrame()).getRotation();

        // Compose rotations : target pointing attitudes
        final double angleCompo = targetRot.composeInverse(lofOffsetRot, RotationConvention.VECTOR_OPERATOR).getAngle();
        Assert.assertEquals(0., angleCompo, Utils.epsilonAngle);

    }

    /** Test is the target pointed is the one expected
     */
    @Test
    public void testTarget()
        throws OrekitException {

        // Create target point and target pointing law towards that point
        final GeodeticPoint targetDef  = new GeodeticPoint(FastMath.toRadians(5.), FastMath.toRadians(-40.), 0.);
        final TargetPointing targetLaw = new TargetPointing(orbit.getFrame(), targetDef, earthSpheric);

        // Get roll, pitch, yaw angles corresponding to this pointing law
        final LofOffset lofAlignedLaw = new LofOffset(orbit.getFrame(), LOFType.VVLH);
        final Rotation lofAlignedRot = lofAlignedLaw.getAttitude(orbit, date, orbit.getFrame()).getRotation();
        final Attitude targetAttitude = targetLaw.getAttitude(orbit, date, orbit.getFrame());
        final Rotation rollPitchYaw = targetAttitude.getRotation().compose(lofAlignedRot.revert(), RotationConvention.VECTOR_OPERATOR).revert();
        final double[] angles = rollPitchYaw.getAngles(RotationOrder.ZYX, RotationConvention.VECTOR_OPERATOR);
        final double yaw   = angles[0];
        final double pitch = angles[1];
        final double roll  = angles[2];

        // Create a lof offset law from those values
        final LofOffset lofOffsetLaw = new LofOffset(orbit.getFrame(), LOFType.VVLH, RotationOrder.ZYX, yaw, pitch, roll);
        final LofOffsetPointing lofOffsetPtLaw = new LofOffsetPointing(orbit.getFrame(), earthSpheric, lofOffsetLaw, Vector3D.PLUS_K);

        // Check target pointed by this law : shall be the same as defined
        final Vector3D pTargetRes =
                lofOffsetPtLaw.getTargetPV(orbit, date, earthSpheric.getBodyFrame()).getPosition();
        final GeodeticPoint targetRes = earthSpheric.transform(pTargetRes, earthSpheric.getBodyFrame(), date);

        Assert.assertEquals(targetDef.getLongitude(), targetRes.getLongitude(), Utils.epsilonAngle);
        Assert.assertEquals(targetDef.getLongitude(), targetRes.getLongitude(), Utils.epsilonAngle);

    }

    @Test
    public void testSpin() throws OrekitException {

        final AttitudeProvider law = new LofOffset(orbit.getFrame(), LOFType.VVLH, RotationOrder.XYX, 0.1, 0.2, 0.3);

        AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 01, 01),
                                             new TimeComponents(3, 25, 45.6789),
                                             TimeScalesFactory.getUTC());
        KeplerianOrbit orbit =
            new KeplerianOrbit(7178000.0, 1.e-4, FastMath.toRadians(50.),
                              FastMath.toRadians(10.), FastMath.toRadians(20.),
                              FastMath.toRadians(30.), PositionAngle.MEAN,
                              FramesFactory.getEME2000(), date, 3.986004415e14);

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
        Vector3D reference = AngularCoordinates.estimateRate(sMinus.getAttitude().getRotation(),
                                                             sPlus.getAttitude().getRotation(),
                                                             2 * h);
        Assert.assertEquals(0.0, spin0.subtract(reference).getNorm(), 1.0e-10);

    }

    @Test
    public void testAnglesSign() throws OrekitException {

        AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 01, 01),
                                             new TimeComponents(3, 25, 45.6789),
                                             TimeScalesFactory.getUTC());
        KeplerianOrbit orbit =
            new KeplerianOrbit(7178000.0, 1.e-8, FastMath.toRadians(50.),
                              FastMath.toRadians(10.), FastMath.toRadians(20.),
                              FastMath.toRadians(0.), PositionAngle.MEAN,
                              FramesFactory.getEME2000(), date, 3.986004415e14);

        double alpha = 0.1;
        double cos = FastMath.cos(alpha);
        double sin = FastMath.sin(alpha);

        // Roll
        Attitude attitude = new LofOffset(orbit.getFrame(), LOFType.VVLH, RotationOrder.XYZ, alpha, 0.0, 0.0).getAttitude(orbit, date, orbit.getFrame());
        checkSatVector(orbit, attitude, Vector3D.PLUS_I,  1.0,  0.0,  0.0, 1.0e-8);
        checkSatVector(orbit, attitude, Vector3D.PLUS_J,  0.0,  cos,  sin, 1.0e-8);
        checkSatVector(orbit, attitude, Vector3D.PLUS_K,  0.0, -sin,  cos, 1.0e-8);

        // Pitch
        attitude = new LofOffset(orbit.getFrame(), LOFType.VVLH, RotationOrder.XYZ, 0.0, alpha, 0.0).getAttitude(orbit, date, orbit.getFrame());
        checkSatVector(orbit, attitude, Vector3D.PLUS_I,  cos,  0.0, -sin, 1.0e-8);
        checkSatVector(orbit, attitude, Vector3D.PLUS_J,  0.0,  1.0,  0.0, 1.0e-8);
        checkSatVector(orbit, attitude, Vector3D.PLUS_K,  sin,  0.0,  cos, 1.0e-8);

        // Yaw
        attitude = new LofOffset(orbit.getFrame(), LOFType.VVLH, RotationOrder.XYZ, 0.0, 0.0, alpha).getAttitude(orbit, date, orbit.getFrame());
        checkSatVector(orbit, attitude, Vector3D.PLUS_I,  cos,  sin,  0.0, 1.0e-8);
        checkSatVector(orbit, attitude, Vector3D.PLUS_J, -sin,  cos,  0.0, 1.0e-8);
        checkSatVector(orbit, attitude, Vector3D.PLUS_K,  0.0,  0.0,  1.0, 1.0e-8);

    }

    @Test
    public void testRetrieveAngles() throws OrekitException {
        AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 01, 01),
                                             new TimeComponents(3, 25, 45.6789),
                                             TimeScalesFactory.getUTC());
        KeplerianOrbit orbit =
            new KeplerianOrbit(7178000.0, 1.e-4, FastMath.toRadians(50.),
                              FastMath.toRadians(10.), FastMath.toRadians(20.),
                              FastMath.toRadians(30.), PositionAngle.MEAN,
                              FramesFactory.getEME2000(), date, 3.986004415e14);

        RotationOrder order = RotationOrder.ZXY;
        double alpha1 = 0.123;
        double alpha2 = 0.456;
        double alpha3 = 0.789;
        LofOffset law = new LofOffset(orbit.getFrame(), LOFType.VVLH, order, alpha1, alpha2, alpha3);
        Rotation offsetAtt  = law.getAttitude(orbit, date, orbit.getFrame()).getRotation();
        Rotation alignedAtt = new LofOffset(orbit.getFrame(), LOFType.VVLH).getAttitude(orbit, date, orbit.getFrame()).getRotation();
        Rotation offsetProper = offsetAtt.compose(alignedAtt.revert(), RotationConvention.VECTOR_OPERATOR);
        double[] anglesV = offsetProper.revert().getAngles(order, RotationConvention.VECTOR_OPERATOR);
        Assert.assertEquals(alpha1, anglesV[0], 1.0e-11);
        Assert.assertEquals(alpha2, anglesV[1], 1.0e-11);
        Assert.assertEquals(alpha3, anglesV[2], 1.0e-11);
        double[] anglesF = offsetProper.getAngles(order, RotationConvention.FRAME_TRANSFORM);
        Assert.assertEquals(alpha1, anglesF[0], 1.0e-11);
        Assert.assertEquals(alpha2, anglesF[1], 1.0e-11);
        Assert.assertEquals(alpha3, anglesF[2], 1.0e-11);
   }

    private void checkSatVector(Orbit o, Attitude a, Vector3D satVector,
                                double expectedX, double expectedY, double expectedZ,
                                double threshold) {
        Vector3D zLof = o.getPVCoordinates().getPosition().normalize().negate();
        Vector3D yLof = o.getPVCoordinates().getMomentum().normalize().negate();
        Vector3D xLof = Vector3D.crossProduct(yLof, zLof);
        Assert.assertTrue(Vector3D.dotProduct(xLof, o.getPVCoordinates().getVelocity()) > 0);
        Vector3D v = a.getRotation().applyInverseTo(satVector);
        Assert.assertEquals(expectedX, Vector3D.dotProduct(v, xLof), 1.0e-8);
        Assert.assertEquals(expectedY, Vector3D.dotProduct(v, yLof), 1.0e-8);
        Assert.assertEquals(expectedZ, Vector3D.dotProduct(v, zLof), 1.0e-8);
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

            // Reference frame = ITRF
            itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);

            // Elliptic earth shape
            earthSpheric =
                new OneAxisEllipsoid(6378136.460, 0., itrf);

            //  Satellite position
            orbit =
                new CircularOrbit(7178000.0, 0.5e-8, -0.5e-8, FastMath.toRadians(50.), FastMath.toRadians(150.),
                                       FastMath.toRadians(5.300), PositionAngle.MEAN,
                                       FramesFactory.getEME2000(), date, mu);
            pvSatEME2000 = orbit.getPVCoordinates();


        } catch (OrekitException oe) {
            Assert.fail(oe.getMessage());
        }

    }

    @After
    public void tearDown() {
        date = null;
        itrf = null;
        earthSpheric = null;
    }

}

