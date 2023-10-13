/* Copyright 2002-2023 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.GradientField;
import org.hipparchus.complex.ComplexField;
import org.hipparchus.geometry.euclidean.threed.*;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.FieldAbsoluteDate;
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
    public void testZero() {

        //  Satellite position

        // Lof aligned attitude provider
        final LofOffset lofAlignedLaw = new LofOffset(orbit.getFrame(), LOFType.LVLH_CCSDS);
        final Rotation lofOffsetRot = lofAlignedLaw.getAttitude(orbit, date, orbit.getFrame()).getRotation();

        // Check that
        final Vector3D momentumEME2000 = pvSatEME2000.getMomentum();
        final Vector3D momentumLof = lofOffsetRot.applyTo(momentumEME2000);
        final double cosinus = FastMath.cos(Vector3D.dotProduct(momentumLof, Vector3D.PLUS_K));
        Assertions.assertEquals(1., cosinus, Utils.epsilonAngle);

    }
    /** Test if the lof offset is the one expected
     */
    @Test
    public void testOffset() {

        //  Satellite position
        final CircularOrbit circ =
           new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, FastMath.toRadians(0.), FastMath.toRadians(270.),
                                   FastMath.toRadians(5.300), PositionAngleType.MEAN,
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
        final LofOffset lofAlignedLaw = new LofOffset(orbit.getFrame(), LOFType.LVLH_CCSDS);
        final Rotation lofAlignedRot = lofAlignedLaw.getAttitude(circ, date, circ.getFrame()).getRotation();

        // Get rotation from LOF to target pointing attitude
        Rotation rollPitchYaw = targetRot.compose(lofAlignedRot.revert(), RotationConvention.VECTOR_OPERATOR).revert();
        final double[] angles = rollPitchYaw.getAngles(RotationOrder.ZYX, RotationConvention.VECTOR_OPERATOR);
        final double yaw = angles[0];
        final double pitch = angles[1];
        final double roll = angles[2];

        // Create lof offset attitude provider with computed roll, pitch, yaw
        // **************************************************************
        final LofOffset lofOffsetLaw = new LofOffset(orbit.getFrame(), LOFType.LVLH_CCSDS, RotationOrder.ZYX, yaw, pitch, roll);
        final Rotation lofOffsetRot = lofOffsetLaw.getAttitude(circ, date, circ.getFrame()).getRotation();

        // Compose rotations : target pointing attitudes
        final double angleCompo = targetRot.composeInverse(lofOffsetRot, RotationConvention.VECTOR_OPERATOR).getAngle();
        Assertions.assertEquals(0., angleCompo, Utils.epsilonAngle);

    }

    /** Test is the target pointed is the one expected
     */
    @Test
    public void testTarget()
        {

        // Create target point and target pointing law towards that point
        final GeodeticPoint targetDef  = new GeodeticPoint(FastMath.toRadians(5.), FastMath.toRadians(-40.), 0.);
        final TargetPointing targetLaw = new TargetPointing(orbit.getFrame(), targetDef, earthSpheric);

        // Get roll, pitch, yaw angles corresponding to this pointing law
        final LofOffset lofAlignedLaw = new LofOffset(orbit.getFrame(), LOFType.LVLH_CCSDS);
        final Rotation lofAlignedRot = lofAlignedLaw.getAttitude(orbit, date, orbit.getFrame()).getRotation();
        final Attitude targetAttitude = targetLaw.getAttitude(orbit, date, orbit.getFrame());
        final Rotation rollPitchYaw = targetAttitude.getRotation().compose(lofAlignedRot.revert(), RotationConvention.VECTOR_OPERATOR).revert();
        final double[] angles = rollPitchYaw.getAngles(RotationOrder.ZYX, RotationConvention.VECTOR_OPERATOR);
        final double yaw   = angles[0];
        final double pitch = angles[1];
        final double roll  = angles[2];

        // Create a lof offset law from those values
        final LofOffset lofOffsetLaw = new LofOffset(orbit.getFrame(), LOFType.LVLH_CCSDS, RotationOrder.ZYX, yaw, pitch, roll);
        final LofOffsetPointing lofOffsetPtLaw = new LofOffsetPointing(orbit.getFrame(), earthSpheric, lofOffsetLaw, Vector3D.PLUS_K);

        // Check target pointed by this law : shall be the same as defined
        final Vector3D pTargetRes =
                lofOffsetPtLaw.getTargetPV(orbit, date, earthSpheric.getBodyFrame()).getPosition();
        final GeodeticPoint targetRes = earthSpheric.transform(pTargetRes, earthSpheric.getBodyFrame(), date);

        Assertions.assertEquals(targetDef.getLongitude(), targetRes.getLongitude(), Utils.epsilonAngle);
        Assertions.assertEquals(targetDef.getLongitude(), targetRes.getLongitude(), Utils.epsilonAngle);

    }

    @Test
    public void testSpin() {

        final AttitudeProvider law = new LofOffset(orbit.getFrame(), LOFType.LVLH_CCSDS, RotationOrder.XYX, 0.1, 0.2, 0.3);

        AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 01, 01),
                                             new TimeComponents(3, 25, 45.6789),
                                             TimeScalesFactory.getUTC());
        KeplerianOrbit orbit =
            new KeplerianOrbit(7178000.0, 1.e-4, FastMath.toRadians(50.),
                              FastMath.toRadians(10.), FastMath.toRadians(20.),
                              FastMath.toRadians(30.), PositionAngleType.MEAN,
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
        Assertions.assertEquals(0.0, errorAngleMinus, 1.0e-6 * evolutionAngleMinus);
        double errorAnglePlus      = Rotation.distance(s0.getAttitude().getRotation(),
                                                       sPlus.shiftedBy(-h).getAttitude().getRotation());
        double evolutionAnglePlus  = Rotation.distance(s0.getAttitude().getRotation(),
                                                       sPlus.getAttitude().getRotation());
        Assertions.assertEquals(0.0, errorAnglePlus, 1.0e-6 * evolutionAnglePlus);

        Vector3D spin0 = s0.getAttitude().getSpin();
        Vector3D reference = AngularCoordinates.estimateRate(sMinus.getAttitude().getRotation(),
                                                             sPlus.getAttitude().getRotation(),
                                                             2 * h);
        Assertions.assertEquals(0.0, spin0.subtract(reference).getNorm(), 1.0e-10);

    }

    @Test
    public void testAnglesSign() {

        AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 01, 01),
                                             new TimeComponents(3, 25, 45.6789),
                                             TimeScalesFactory.getUTC());
        KeplerianOrbit orbit =
            new KeplerianOrbit(7178000.0, 1.e-8, FastMath.toRadians(50.),
                              FastMath.toRadians(10.), FastMath.toRadians(20.),
                              FastMath.toRadians(0.), PositionAngleType.MEAN,
                              FramesFactory.getEME2000(), date, 3.986004415e14);

        double alpha = 0.1;
        double cos = FastMath.cos(alpha);
        double sin = FastMath.sin(alpha);

        // Roll
        Attitude attitude = new LofOffset(orbit.getFrame(), LOFType.LVLH_CCSDS, RotationOrder.XYZ, alpha, 0.0, 0.0).getAttitude(orbit, date, orbit.getFrame());
        checkSatVector(orbit, attitude, Vector3D.PLUS_I,  1.0,  0.0,  0.0, 1.0e-8);
        checkSatVector(orbit, attitude, Vector3D.PLUS_J,  0.0,  cos,  sin, 1.0e-8);
        checkSatVector(orbit, attitude, Vector3D.PLUS_K,  0.0, -sin,  cos, 1.0e-8);

        // Pitch
        attitude = new LofOffset(orbit.getFrame(), LOFType.LVLH_CCSDS, RotationOrder.XYZ, 0.0, alpha, 0.0).getAttitude(orbit, date, orbit.getFrame());
        checkSatVector(orbit, attitude, Vector3D.PLUS_I,  cos,  0.0, -sin, 1.0e-8);
        checkSatVector(orbit, attitude, Vector3D.PLUS_J,  0.0,  1.0,  0.0, 1.0e-8);
        checkSatVector(orbit, attitude, Vector3D.PLUS_K,  sin,  0.0,  cos, 1.0e-8);

        // Yaw
        attitude = new LofOffset(orbit.getFrame(), LOFType.LVLH_CCSDS, RotationOrder.XYZ, 0.0, 0.0, alpha).getAttitude(orbit, date, orbit.getFrame());
        checkSatVector(orbit, attitude, Vector3D.PLUS_I,  cos,  sin,  0.0, 1.0e-8);
        checkSatVector(orbit, attitude, Vector3D.PLUS_J, -sin,  cos,  0.0, 1.0e-8);
        checkSatVector(orbit, attitude, Vector3D.PLUS_K,  0.0,  0.0,  1.0, 1.0e-8);

    }

    @Test
    public void testRetrieveAngles() {
        AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 01, 01),
                                             new TimeComponents(3, 25, 45.6789),
                                             TimeScalesFactory.getUTC());
        KeplerianOrbit orbit =
            new KeplerianOrbit(7178000.0, 1.e-4, FastMath.toRadians(50.),
                              FastMath.toRadians(10.), FastMath.toRadians(20.),
                              FastMath.toRadians(30.), PositionAngleType.MEAN,
                              FramesFactory.getEME2000(), date, 3.986004415e14);

        RotationOrder order = RotationOrder.ZXY;
        double alpha1 = 0.123;
        double alpha2 = 0.456;
        double alpha3 = 0.789;
        LofOffset law = new LofOffset(orbit.getFrame(), LOFType.LVLH_CCSDS, order, alpha1, alpha2, alpha3);
        Rotation offsetAtt  = law.getAttitude(orbit, date, orbit.getFrame()).getRotation();
        Rotation alignedAtt = new LofOffset(orbit.getFrame(), LOFType.LVLH_CCSDS).getAttitude(orbit, date, orbit.getFrame()).getRotation();
        Rotation offsetProper = offsetAtt.compose(alignedAtt.revert(), RotationConvention.VECTOR_OPERATOR);
        double[] anglesV = offsetProper.revert().getAngles(order, RotationConvention.VECTOR_OPERATOR);
        Assertions.assertEquals(alpha1, anglesV[0], 1.0e-11);
        Assertions.assertEquals(alpha2, anglesV[1], 1.0e-11);
        Assertions.assertEquals(alpha3, anglesV[2], 1.0e-11);
        double[] anglesF = offsetProper.getAngles(order, RotationConvention.FRAME_TRANSFORM);
        Assertions.assertEquals(alpha1, anglesF[0], 1.0e-11);
        Assertions.assertEquals(alpha2, anglesF[1], 1.0e-11);
        Assertions.assertEquals(alpha3, anglesF[2], 1.0e-11);
    }

    @Test
    public void testTypesField() {
        AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 01, 01),
                                             new TimeComponents(3, 25, 45.6789),
                                             TimeScalesFactory.getUTC());
        KeplerianOrbit orbit =
            new KeplerianOrbit(7178000.0, 1.e-4, FastMath.toRadians(50.),
                              FastMath.toRadians(10.), FastMath.toRadians(20.),
                              FastMath.toRadians(30.), PositionAngleType.MEAN,
                              FramesFactory.getEME2000(), date, 3.986004415e14);

        for (final LOFType type : LOFType.values()) {
            RotationOrder order = RotationOrder.ZXY;
            double alpha1 = 0.123;
            double alpha2 = 0.456;
            double alpha3 = 0.789;
            LofOffset law = new LofOffset(orbit.getFrame(), type, order, alpha1, alpha2, alpha3);
            checkField(Binary64Field.getInstance(), law, orbit, date, orbit.getFrame());
        }
    }

    private void checkSatVector(Orbit o, Attitude a, Vector3D satVector,
                                double expectedX, double expectedY, double expectedZ,
                                double threshold) {
        Vector3D zLof = o.getPosition().normalize().negate();
        Vector3D yLof = o.getPVCoordinates().getMomentum().normalize().negate();
        Vector3D xLof = Vector3D.crossProduct(yLof, zLof);
        Assertions.assertTrue(Vector3D.dotProduct(xLof, o.getPVCoordinates().getVelocity()) > 0);
        Vector3D v = a.getRotation().applyInverseTo(satVector);
        Assertions.assertEquals(expectedX, Vector3D.dotProduct(v, xLof), 1.0e-8);
        Assertions.assertEquals(expectedY, Vector3D.dotProduct(v, yLof), 1.0e-8);
        Assertions.assertEquals(expectedZ, Vector3D.dotProduct(v, zLof), 1.0e-8);
    }

    private <T extends CalculusFieldElement<T>> void checkField(final Field<T> field, final AttitudeProvider provider,
                                                            final Orbit orbit, final AbsoluteDate date,
                                                            final Frame frame)
        {
        Attitude attitudeD = provider.getAttitude(orbit, date, frame);
        final FieldOrbit<T> orbitF = new FieldSpacecraftState<>(field, new SpacecraftState(orbit)).getOrbit();
        final FieldAbsoluteDate<T> dateF = new FieldAbsoluteDate<>(field, date);
        FieldAttitude<T> attitudeF = provider.getAttitude(orbitF, dateF, frame);
        Assertions.assertEquals(0.0, Rotation.distance(attitudeD.getRotation(), attitudeF.getRotation().toRotation()), 1.0e-15);
        Assertions.assertEquals(0.0, Vector3D.distance(attitudeD.getSpin(), attitudeF.getSpin().toVector3D()), 1.0e-15);
        Assertions.assertEquals(0.0, Vector3D.distance(attitudeD.getRotationAcceleration(), attitudeF.getRotationAcceleration().toVector3D()), 1.0e-15);
    }

    @Test
    void testGetAttitudeRotation() {
        // GIVEN
        final AbsoluteDate date = orbit.getDate();
        final LofOffset lofOffset = new LofOffset(orbit.getFrame(), LOFType.QSW);
        // WHEN
        final Rotation actualRotation = lofOffset.getAttitudeRotation(orbit, date, itrf);
        // THEN
        final Rotation expectedRotation = lofOffset.getAttitude(orbit, date, itrf).getRotation();
        Assertions.assertEquals(0., Rotation.distance(expectedRotation, actualRotation));
    }

    @Test
    void testGetAttitudeRotationFieldComplex() {
        final ComplexField complexField = ComplexField.getInstance();
        templateTestGetRotationField(complexField);
    }

    @Test
    void testGetAttitudeRotationFieldGradient() {
        final GradientField gradientField = GradientField.getField(1);
        templateTestGetRotationField(gradientField);
    }

    <T extends CalculusFieldElement<T>> void templateTestGetRotationField(final Field<T> field) {
        // GIVEN
        final LofOffset lofOffset = new LofOffset(orbit.getFrame(), LOFType.QSW);
        final SpacecraftState state = new SpacecraftState(orbit);
        final FieldSpacecraftState<T> fieldState = new FieldSpacecraftState<>(field, state);
        // WHEN
        final FieldRotation<T> actualRotation = lofOffset.getAttitudeRotation(fieldState.getOrbit(), fieldState.getDate(), itrf);
        // THEN
        final FieldRotation<T> expectedRotation = lofOffset.getAttitude(fieldState.getOrbit(), fieldState.getDate(), itrf).getRotation();
        Assertions.assertEquals(0., Rotation.distance(expectedRotation.toRotation(), actualRotation.toRotation()));
    }

    @BeforeEach
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
                                       FastMath.toRadians(5.300), PositionAngleType.MEAN,
                                       FramesFactory.getEME2000(), date, mu);
            pvSatEME2000 = orbit.getPVCoordinates();


        } catch (OrekitException oe) {
            Assertions.fail(oe.getMessage());
        }

    }

    @AfterEach
    public void tearDown() {
        date = null;
        itrf = null;
        earthSpheric = null;
    }

}

