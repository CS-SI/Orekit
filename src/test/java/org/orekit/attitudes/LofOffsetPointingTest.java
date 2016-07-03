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
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
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
    public void testLof() throws OrekitException {

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
    public void testMiss() throws OrekitException {
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
    public void testSpin() throws OrekitException {

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

