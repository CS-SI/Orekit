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
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FramesFactory;
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
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;

public class SpinStabilizedTest {

    @Test
    public void testBBQMode() throws OrekitException {
        PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 01, 01),
                                             new TimeComponents(3, 25, 45.6789),
                                             TimeScalesFactory.getTAI());
        double rate = 2.0 * FastMath.PI / (12 * 60);
        AttitudeProvider cbp = new CelestialBodyPointed(FramesFactory.getEME2000(), sun, Vector3D.PLUS_K,
                                                        Vector3D.PLUS_I, Vector3D.PLUS_K);
        SpinStabilized bbq = new SpinStabilized(cbp, date, Vector3D.PLUS_K, rate);
        PVCoordinates pv =
            new PVCoordinates(new Vector3D(28812595.32012577, 5948437.4640250085, 0),
                              new Vector3D(0, 0, 3680.853673522056));
        KeplerianOrbit kep = new KeplerianOrbit(pv, FramesFactory.getEME2000(), date, 3.986004415e14);
        Attitude attitude = bbq.getAttitude(kep, date, kep.getFrame());
        Vector3D xDirection = attitude.getRotation().applyInverseTo(Vector3D.PLUS_I);
        Assert.assertEquals(FastMath.atan(1.0 / 5000.0),
                     Vector3D.angle(xDirection, sun.getPVCoordinates(date, FramesFactory.getEME2000()).getPosition()),
                     2.0e-15);
        Assert.assertEquals(rate, attitude.getSpin().getNorm(), 1.0e-6);
        Assert.assertSame(cbp, bbq.getUnderlyingAttitudeProvider());

    }

    @Test
    public void testSpin() throws OrekitException {

        AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 01, 01),
                                             new TimeComponents(3, 25, 45.6789),
                                             TimeScalesFactory.getUTC());
        double rate = 2.0 * FastMath.PI / (12 * 60);
        AttitudeProvider law =
            new SpinStabilized(new InertialProvider(Rotation.IDENTITY),
                               date, Vector3D.PLUS_K, rate);
        KeplerianOrbit orbit =
            new KeplerianOrbit(7178000.0, 1.e-4, FastMath.toRadians(50.),
                              FastMath.toRadians(10.), FastMath.toRadians(20.),
                              FastMath.toRadians(30.), PositionAngle.MEAN,
                              FramesFactory.getEME2000(), date, 3.986004415e14);

        Propagator propagator = new KeplerianPropagator(orbit, law);

        double h = 10.0;
        SpacecraftState sMinus = propagator.propagate(date.shiftedBy(-h));
        SpacecraftState s0     = propagator.propagate(date);
        SpacecraftState sPlus  = propagator.propagate(date.shiftedBy(h));
        Vector3D spin0         = s0.getAttitude().getSpin();

        // check spin is consistent with attitude evolution
        double errorAngleMinus     = Rotation.distance(sMinus.shiftedBy(h).getAttitude().getRotation(),
                                                       s0.getAttitude().getRotation());
        double evolutionAngleMinus = Rotation.distance(sMinus.getAttitude().getRotation(),
                                                       s0.getAttitude().getRotation());
        Assert.assertTrue(errorAngleMinus <= 1.0e-6 * evolutionAngleMinus);
        double errorAnglePlus      = Rotation.distance(s0.getAttitude().getRotation(),
                                                       sPlus.shiftedBy(-h).getAttitude().getRotation());
        double evolutionAnglePlus  = Rotation.distance(s0.getAttitude().getRotation(),
                                                       sPlus.getAttitude().getRotation());
        Assert.assertTrue(errorAnglePlus <= 1.0e-6 * evolutionAnglePlus);

        // compute spin axis using finite differences
        Rotation rM = sMinus.getAttitude().getRotation();
        Rotation rP = sPlus.getAttitude().getRotation();
        Vector3D reference = AngularCoordinates.estimateRate(rM, rP, 2 * h);

        Assert.assertEquals(2 * FastMath.PI / reference.getNorm(), 2 * FastMath.PI / spin0.getNorm(), 0.05);
        Assert.assertEquals(0.0, FastMath.toDegrees(Vector3D.angle(reference, spin0)), 1.0e-10);
        Assert.assertEquals(0.0, FastMath.toDegrees(Vector3D.angle(Vector3D.PLUS_K, spin0)), 1.0e-10);

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}

