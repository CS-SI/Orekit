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
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.PVCoordinatesProvider;

public class CelestialBodyPointingTest {

    @Test
    public void testSunPointing() throws OrekitException {
        PVCoordinatesProvider sun = CelestialBodyFactory.getSun();

        final Frame frame = FramesFactory.getGCRF();
        AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 01, 01),
                                             new TimeComponents(3, 25, 45.6789),
                                             TimeScalesFactory.getTAI());
        AttitudeProvider sunPointing =
            new CelestialBodyPointed(frame, sun, Vector3D.PLUS_K,
                                     Vector3D.PLUS_I, Vector3D.PLUS_K);
        PVCoordinates pv =
            new PVCoordinates(new Vector3D(28812595.32120171334, 5948437.45881852374, 0.0),
                              new Vector3D(0, 0, 3680.853673522056));
        Orbit orbit = new KeplerianOrbit(pv, frame, date, 3.986004415e14);
        Attitude attitude   = sunPointing.getAttitude(orbit, date, frame);
        Vector3D xDirection = attitude.getRotation().applyInverseTo(Vector3D.PLUS_I);
        Vector3D zDirection = attitude.getRotation().applyInverseTo(Vector3D.PLUS_K);
        Assert.assertEquals(0,
                     Vector3D.dotProduct(zDirection, Vector3D.crossProduct(xDirection, Vector3D.PLUS_K)),
                     1.0e-15);

        // the following statement checks we take parallax into account
        // Sun-Earth-Sat are in quadrature, with distance (Earth, Sat) == distance(Sun, Earth) / 5000
        Assert.assertEquals(FastMath.atan(1.0 / 5000.0),
                            Vector3D.angle(xDirection,
                                           sun.getPVCoordinates(date, frame).getPosition()),
                                           1.0e-15);

        double h = 0.1;
        Attitude aMinus = sunPointing.getAttitude(orbit.shiftedBy(-h), date.shiftedBy(-h), frame);
        Attitude a0     = sunPointing.getAttitude(orbit, date, frame);
        Attitude aPlus  = sunPointing.getAttitude(orbit.shiftedBy(h), date.shiftedBy(h), frame);

        // check spin is consistent with attitude evolution
        double errorAngleMinus     = Rotation.distance(aMinus.shiftedBy(h).getRotation(),
                                                       a0.getRotation());
        double evolutionAngleMinus = Rotation.distance(aMinus.getRotation(),
                                                       a0.getRotation());
        Assert.assertEquals(0.0, errorAngleMinus, 1.0e-6 * evolutionAngleMinus);
        double errorAnglePlus      = Rotation.distance(a0.getRotation(),
                                                       aPlus.shiftedBy(-h).getRotation());
        double evolutionAnglePlus  = Rotation.distance(a0.getRotation(),
                                                       aPlus.getRotation());
        Assert.assertEquals(0.0, errorAnglePlus, 1.0e-6 * evolutionAnglePlus);

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}

