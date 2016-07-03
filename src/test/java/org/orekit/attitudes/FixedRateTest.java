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
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
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
import org.orekit.utils.AngularCoordinates;
import org.orekit.utils.PVCoordinates;

public class FixedRateTest {

    @Test
    public void testZeroRate() throws OrekitException {
        AbsoluteDate date = new AbsoluteDate(new DateComponents(2004, 3, 2),
                                             new TimeComponents(13, 17, 7.865),
                                             TimeScalesFactory.getUTC());
        final Frame frame = FramesFactory.getEME2000();
        FixedRate law = new FixedRate(new Attitude(date, frame,
                                                   new Rotation(0.48, 0.64, 0.36, 0.48, false),
                                                   Vector3D.ZERO, Vector3D.ZERO));
        PVCoordinates pv =
            new PVCoordinates(new Vector3D(28812595.32012577, 5948437.4640250085, 0),
                              new Vector3D(0, 0, 3680.853673522056));
        Orbit orbit = new KeplerianOrbit(pv, frame, date, 3.986004415e14);
        Rotation attitude0 = law.getAttitude(orbit, date, frame).getRotation();
        Assert.assertEquals(0, Rotation.distance(attitude0, law.getReferenceAttitude().getRotation()), 1.0e-10);
        Rotation attitude1 = law.getAttitude(orbit.shiftedBy(10.0), date.shiftedBy(10.0), frame).getRotation();
        Assert.assertEquals(0, Rotation.distance(attitude1, law.getReferenceAttitude().getRotation()), 1.0e-10);
        Rotation attitude2 = law.getAttitude(orbit.shiftedBy(20.0), date.shiftedBy(20.0), frame).getRotation();
        Assert.assertEquals(0, Rotation.distance(attitude2, law.getReferenceAttitude().getRotation()), 1.0e-10);

    }

    @Test
    public void testNonZeroRate() throws OrekitException {
        final AbsoluteDate date = new AbsoluteDate(new DateComponents(2004, 3, 2),
                                                   new TimeComponents(13, 17, 7.865),
                                                   TimeScalesFactory.getUTC());
        final double rate = 2 * FastMath.PI / (12 * 60);
        final Frame frame = FramesFactory.getEME2000();
        FixedRate law = new FixedRate(new Attitude(date, frame,
                                                   new Rotation(0.48, 0.64, 0.36, 0.48, false),
                                                   new Vector3D(rate, Vector3D.PLUS_K), Vector3D.ZERO));
        PVCoordinates pv =
            new PVCoordinates(new Vector3D(28812595.32012577, 5948437.4640250085, 0),
                              new Vector3D(0, 0, 3680.853673522056));
        Orbit orbit = new KeplerianOrbit(pv, FramesFactory.getEME2000(), date, 3.986004415e14);
        Rotation attitude0 = law.getAttitude(orbit, date, frame).getRotation();
        Assert.assertEquals(0, Rotation.distance(attitude0, law.getReferenceAttitude().getRotation()), 1.0e-10);
        Rotation attitude1 = law.getAttitude(orbit.shiftedBy(10.0), date.shiftedBy(10.0), frame).getRotation();
        Assert.assertEquals(10 * rate, Rotation.distance(attitude1, law.getReferenceAttitude().getRotation()), 1.0e-10);
        Rotation attitude2 = law.getAttitude(orbit.shiftedBy(-20.0), date.shiftedBy(-20.0), frame).getRotation();
        Assert.assertEquals(20 * rate, Rotation.distance(attitude2, law.getReferenceAttitude().getRotation()), 1.0e-10);
        Assert.assertEquals(30 * rate, Rotation.distance(attitude2, attitude1), 1.0e-10);
        Rotation attitude3 = law.getAttitude(orbit.shiftedBy(0.0), date, frame).getRotation();
        Assert.assertEquals(0, Rotation.distance(attitude3, law.getReferenceAttitude().getRotation()), 1.0e-10);

    }

    @Test
    public void testSpin() throws OrekitException {

        AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 01, 01),
                                             new TimeComponents(3, 25, 45.6789),
                                             TimeScalesFactory.getUTC());

        final double rate = 2 * FastMath.PI / (12 * 60);
        AttitudeProvider law =
            new FixedRate(new Attitude(date, FramesFactory.getEME2000(),
                                       new Rotation(0.48, 0.64, 0.36, 0.48, false),
                                       new Vector3D(rate, Vector3D.PLUS_K),
                                       Vector3D.ZERO));

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
        Assert.assertEquals(0.0, spin0.subtract(reference).getNorm(), 1.0e-14);

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}

