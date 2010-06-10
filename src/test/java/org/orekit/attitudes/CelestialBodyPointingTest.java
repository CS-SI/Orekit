/* Copyright 2002-2010 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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


import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.Vector3D;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
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
        AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 01, 01),
                                             new TimeComponents(3, 25, 45.6789),
                                             TimeScalesFactory.getUTC());
        AttitudeLaw sunPointing =
            new CelestialBodyPointed(FramesFactory.getEME2000(), sun, Vector3D.PLUS_K,
                                     Vector3D.PLUS_I, Vector3D.PLUS_K);
        PVCoordinates pv =
            new PVCoordinates(new Vector3D(28812595.32012577, 5948437.4640250085, 0),
                              new Vector3D(0, 0, 3680.853673522056));
        Orbit orbit = new KeplerianOrbit(pv, FramesFactory.getEME2000(), date, 3.986004415e14);
        Attitude attitude   = sunPointing.getAttitude(orbit);
        Vector3D xDirection = attitude.getRotation().applyInverseTo(Vector3D.PLUS_I);
        Vector3D zDirection = attitude.getRotation().applyInverseTo(Vector3D.PLUS_K);
        Assert.assertEquals(0,
                     Vector3D.dotProduct(zDirection, Vector3D.crossProduct(xDirection, Vector3D.PLUS_K)),
                     1.0e-15);

        // the following statement checks we take parallax into account
        // Sun-Earth-Sat are in quadrature, with distance (Earth, Sat) == distance(Sun, Earth) / 5000
        Assert.assertEquals(Math.atan(1.0 / 5000.0),
                            Vector3D.angle(xDirection,
                                           sun.getPVCoordinates(date, FramesFactory.getEME2000()).getPosition()),
                                           1.0e-15);

        double h = 0.1;
        Attitude aMinus = sunPointing.getAttitude(orbit.shiftedBy(-h));
        Attitude a0     = sunPointing.getAttitude(orbit);
        Attitude aPlus  = sunPointing.getAttitude(orbit.shiftedBy(h));

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

