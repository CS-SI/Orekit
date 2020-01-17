/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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


import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Decimal64Field;
import org.hipparchus.util.FastMath;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
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
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;


public class LofOffsetPointingTest {

    // Computation date
    private AbsoluteDate date;

    // Body mu
    private double mu;

    // Reference frame = ITRF
    private Frame frameItrf;

    // Earth shape
    OneAxisEllipsoid earthSpheric;

    /** Test if both constructors are equivalent
     */
    @Test
    public void testLof() {

        //  Satellite position
        final CircularOrbit circ =
            new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, FastMath.toRadians(0.), FastMath.toRadians(270.),
                                   FastMath.toRadians(5.300), PositionAngle.MEAN,
                                   FramesFactory.getEME2000(), date, mu);

        // Create lof aligned law
        //************************
        final LofOffset lofLaw = new LofOffset(circ.getFrame(), LOFType.VVLH);
        final LofOffsetPointing lofPointing = new LofOffsetPointing(circ.getFrame(), earthSpheric, lofLaw, Vector3D.PLUS_K);
        final Rotation lofRot = lofPointing.getAttitude(circ, date, circ.getFrame()).getRotation();

        // Compare to body center pointing law
        //*************************************
        final BodyCenterPointing centerLaw = new BodyCenterPointing(circ.getFrame(), earthSpheric);
        final Rotation centerRot = centerLaw.getAttitude(circ, date, circ.getFrame()).getRotation();
        final double angleBodyCenter = centerRot.composeInverse(lofRot, RotationConvention.VECTOR_OPERATOR).getAngle();
        Assert.assertEquals(0., angleBodyCenter, Utils.epsilonAngle);

        // Compare to nadir pointing law
        //*******************************
        final NadirPointing nadirLaw = new NadirPointing(circ.getFrame(), earthSpheric);
        final Rotation nadirRot = nadirLaw.getAttitude(circ, date, circ.getFrame()).getRotation();
        final double angleNadir = nadirRot.composeInverse(lofRot, RotationConvention.VECTOR_OPERATOR).getAngle();
        Assert.assertEquals(0., angleNadir, Utils.epsilonAngle);

    }

    @Test
    public void testMiss() {
        final CircularOrbit circ =
            new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, FastMath.toRadians(0.), FastMath.toRadians(270.),
                                   FastMath.toRadians(5.300), PositionAngle.MEAN,
                                   FramesFactory.getEME2000(), date, mu);
        final LofOffset upsideDown = new LofOffset(circ.getFrame(), LOFType.VVLH, RotationOrder.XYX, FastMath.PI, 0, 0);
        final LofOffsetPointing pointing = new LofOffsetPointing(circ.getFrame(), earthSpheric, upsideDown, Vector3D.PLUS_K);
        try {
            pointing.getTargetPV(circ, date, circ.getFrame());
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.ATTITUDE_POINTING_LAW_DOES_NOT_POINT_TO_GROUND, oe.getSpecifier());
        }
    }

    @Test
    public void testSpin() {

        AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 01, 01),
                                             new TimeComponents(3, 25, 45.6789),
                                             TimeScalesFactory.getUTC());
        KeplerianOrbit orbit =
            new KeplerianOrbit(7178000.0, 1.e-4, FastMath.toRadians(50.),
                              FastMath.toRadians(10.), FastMath.toRadians(20.),
                              FastMath.toRadians(30.), PositionAngle.MEAN,
                              FramesFactory.getEME2000(), date, 3.986004415e14);

        final AttitudeProvider law =
            new LofOffsetPointing(orbit.getFrame(), earthSpheric,
                                  new LofOffset(orbit.getFrame(), LOFType.VVLH, RotationOrder.XYX, 0.1, 0.2, 0.3),
                                  Vector3D.PLUS_K);

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
        Assert.assertTrue(spin0.getNorm() > 1.0e-3);
        Assert.assertEquals(0.0, spin0.subtract(reference).getNorm(), 1.0e-10);

    }

    @Test
    public void testTypesField() {
        AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 01, 01),
                                             new TimeComponents(3, 25, 45.6789),
                                             TimeScalesFactory.getUTC());
        KeplerianOrbit orbit =
            new KeplerianOrbit(7178000.0, 1.e-4, FastMath.toRadians(50.),
                              FastMath.toRadians(10.), FastMath.toRadians(20.),
                              FastMath.toRadians(30.), PositionAngle.MEAN,
                              FramesFactory.getEME2000(), date, 3.986004415e14);

        for (final LOFType type : LOFType.values()) {
            RotationOrder order = RotationOrder.ZXY;
            double alpha1 = 0.123;
            double alpha2 = 0.456;
            double alpha3 = 0.789;
            LofOffset law = new LofOffset(orbit.getFrame(), type, order, alpha1, alpha2, alpha3);
            final Vector3D dir;
            switch (type) {
                case TNW:
                    dir = Vector3D.PLUS_J;
                    break;
                case QSW:
                case LVLH:
                    dir = Vector3D.MINUS_I;
                    break;
                case VVLH:
                    dir = Vector3D.PLUS_K;
                    break;
                default : // VNC
                    dir = Vector3D.MINUS_K;
            }
            LofOffsetPointing lop = new LofOffsetPointing(orbit.getFrame(), earthSpheric, law, dir);
            checkField(Decimal64Field.getInstance(), lop, orbit, date, orbit.getFrame());
        }
    }

    private <T extends RealFieldElement<T>> void checkField(final Field<T> field, final GroundPointing provider,
                                                            final Orbit orbit, final AbsoluteDate date,
                                                            final Frame frame)
        {

        final Attitude attitudeD = provider.getAttitude(orbit, date, frame);
        final FieldOrbit<T> orbitF = new FieldSpacecraftState<>(field, new SpacecraftState(orbit)).getOrbit();
        final FieldAbsoluteDate<T> dateF = new FieldAbsoluteDate<>(field, date);
        final FieldAttitude<T> attitudeF = provider.getAttitude(orbitF, dateF, frame);
        Assert.assertEquals(0.0, Rotation.distance(attitudeD.getRotation(), attitudeF.getRotation().toRotation()), 1.0e-15);
        Assert.assertEquals(0.0, Vector3D.distance(attitudeD.getSpin(), attitudeF.getSpin().toVector3D()), 1.0e-15);
        Assert.assertEquals(0.0, Vector3D.distance(attitudeD.getRotationAcceleration(), attitudeF.getRotationAcceleration().toVector3D()), 1.0e-15);

        final TimeStampedPVCoordinates         pvD = provider.getTargetPV(orbit, date, frame);
        final TimeStampedFieldPVCoordinates<T> pvF = provider.getTargetPV(orbitF, dateF, frame);
        Assert.assertEquals(0.0, Vector3D.distance(pvD.getPosition(),     pvF.getPosition().toVector3D()),     9.0e-9);
        Assert.assertEquals(0.0, Vector3D.distance(pvD.getVelocity(),     pvF.getVelocity().toVector3D()),     5.0e-9);
        Assert.assertEquals(0.0, Vector3D.distance(pvD.getAcceleration(), pvF.getAcceleration().toVector3D()), 4.0e-5);

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
            frameItrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);

            // Elliptic earth shape
            earthSpheric =
                new OneAxisEllipsoid(6378136.460, 0., frameItrf);

        } catch (OrekitException oe) {
            Assert.fail(oe.getMessage());
        }

    }

    @After
    public void tearDown() {
        date = null;
        frameItrf = null;
        earthSpheric = null;
    }
}

