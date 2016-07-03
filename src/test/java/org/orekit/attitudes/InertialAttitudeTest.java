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
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FramesFactory;
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


public class InertialAttitudeTest {

    private AbsoluteDate t0;
    private Orbit        orbit0;

    @Test
    public void testIsInertial() throws OrekitException {
        InertialProvider law = new InertialProvider(new Rotation(new Vector3D(0.6, 0.48, 0.64), 0.9, RotationConvention.VECTOR_OPERATOR));
        KeplerianPropagator propagator = new KeplerianPropagator(orbit0, law);
        Attitude initial = propagator.propagate(t0).getAttitude();
        for (double t = 0; t < 10000.0; t += 100) {
            Attitude attitude = propagator.propagate(t0.shiftedBy(t)).getAttitude();
            Rotation evolution = attitude.getRotation().compose(initial.getRotation().revert(),
                                                                RotationConvention.VECTOR_OPERATOR);
            Assert.assertEquals(0, evolution.getAngle(), 1.0e-10);
            Assert.assertEquals(FramesFactory.getEME2000(), attitude.getReferenceFrame());
        }
    }

    @Test
    public void testCompensateMomentum() throws OrekitException {
        InertialProvider law = new InertialProvider(new Rotation(new Vector3D(-0.64, 0.6, 0.48), 0.2, RotationConvention.VECTOR_OPERATOR));
        KeplerianPropagator propagator = new KeplerianPropagator(orbit0, law);
        Attitude initial = propagator.propagate(t0).getAttitude();
        for (double t = 0; t < 10000.0; t += 100) {
            Attitude attitude = propagator.propagate(t0.shiftedBy(t)).getAttitude();
            Rotation evolution = attitude.getRotation().compose(initial.getRotation().revert(),
                                                                RotationConvention.VECTOR_OPERATOR);
            Assert.assertEquals(0, evolution.getAngle(), 1.0e-10);
            Assert.assertEquals(FramesFactory.getEME2000(), attitude.getReferenceFrame());
        }
    }

    @Test
    public void testSpin() throws OrekitException {

        AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 01, 01),
                                             new TimeComponents(3, 25, 45.6789),
                                             TimeScalesFactory.getUTC());

        AttitudeProvider law = new InertialProvider(new Rotation(new Vector3D(-0.64, 0.6, 0.48), 0.2, RotationConvention.VECTOR_OPERATOR));

        KeplerianOrbit orbit =
            new KeplerianOrbit(7178000.0, 1.e-4, FastMath.toRadians(50.),
                              FastMath.toRadians(10.), FastMath.toRadians(20.),
                              FastMath.toRadians(30.), PositionAngle.MEAN,
                              FramesFactory.getEME2000(), date, 3.986004415e14);

        Propagator propagator = new KeplerianPropagator(orbit, law);

        double h = 100.0;
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

        // compute spin axis using finite differences
        Rotation rMinus = sMinus.getAttitude().getRotation();
        Rotation rPlus  = sPlus.getAttitude().getRotation();
        Rotation dr     = rPlus.compose(rMinus.revert(), RotationConvention.VECTOR_OPERATOR);
        Assert.assertEquals(0, dr.getAngle(), 1.0e-10);

        Vector3D spin0 = s0.getAttitude().getSpin();
        Assert.assertEquals(0, spin0.getNorm(), 1.0e-10);

    }

    @Before
    public void setUp() {
        try {
            Utils.setDataRoot("regular-data");

            t0 = new AbsoluteDate(new DateComponents(2008, 06, 03), TimeComponents.H12,
                                  TimeScalesFactory.getUTC());
            orbit0 =
                new KeplerianOrbit(12345678.9, 0.001, 2.3, 0.1, 3.04, 2.4,
                                   PositionAngle.TRUE, FramesFactory.getEME2000(),
                                   t0, 3.986004415e14);
        } catch (OrekitException oe) {
            Assert.fail(oe.getMessage());
        }
    }

    @After
    public void tearDown() {
        t0     = null;
        orbit0 = null;
    }

}

