/* Copyright 2002-2008 CS Communication & Systèmes
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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.math.geometry.Vector3D;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.SolarSystemBody;
import org.orekit.data.DataDirectoryCrawler;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.UTCScale;
import org.orekit.utils.PVCoordinates;

public class CelestialBodyPointingTest extends TestCase {

    public CelestialBodyPointingTest(String name) {
        super(name);
    }

    public void testSunPointing() throws OrekitException {
        CelestialBody sun = SolarSystemBody.getSun();
        AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 01, 01),
                                             new TimeComponents(3, 25, 45.6789),
                                             UTCScale.getInstance());
        AttitudeLaw sunPointing =
            new CelestialBodyPointed(Frame.getEME2000(), sun, Vector3D.PLUS_K,
                                     Vector3D.PLUS_I, Vector3D.PLUS_K);
        PVCoordinates pv =
            new PVCoordinates(new Vector3D(28812595.32012577, 5948437.4640250085, 0),
                              new Vector3D(0, 0, 3680.853673522056));
        Attitude attitude = sunPointing.getState(date, pv, Frame.getEME2000());
        Vector3D xDirection = attitude.getRotation().applyInverseTo(Vector3D.PLUS_I);
        Vector3D zDirection = attitude.getRotation().applyInverseTo(Vector3D.PLUS_K);
        assertEquals(0,
                     Vector3D.dotProduct(zDirection, Vector3D.crossProduct(xDirection, Vector3D.PLUS_K)),
                     1.0e-15);
        double period = 2 * Math.PI / (attitude.getSpin().getNorm() * 86400);
        assertTrue((period > 350) && (period < 370));

        // the following statement checks we take parallax into account
        // Sun-Earth-Sat are in quadrature, with distance (Earth, Sat) == distance(Sun, Earth) / 5000
        assertEquals(Math.atan(1.0 / 5000.0),
                     Vector3D.angle(xDirection,
                                    sun.getPVCoordinates(date, Frame.getEME2000()).getPosition()),
                     1.0e-15);

    }

    public void setUp() {
        System.setProperty(DataDirectoryCrawler.DATA_ROOT_DIRECTORY_FS, "");
        System.setProperty(DataDirectoryCrawler.DATA_ROOT_DIRECTORY_CP, "regular-data");
    }

    public static Test suite() {
        return new TestSuite(CelestialBodyPointingTest.class);
    }

}

