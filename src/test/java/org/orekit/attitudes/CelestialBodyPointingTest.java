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

import org.apache.commons.math.geometry.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.forces.Sun;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class CelestialBodyPointingTest extends TestCase {

    public CelestialBodyPointingTest(String name) {
        super(name);
    }

    public void testSunPointing() throws OrekitException {
        Sun sun = new Sun();
        AbsoluteDate date = new AbsoluteDate(AbsoluteDate.J2000_EPOCH, 12345.6789);
        AttitudeLaw sunPointing =
            new CelestialBodyPointed(Frame.getJ2000(), sun, Vector3D.PLUS_K,
                                     Vector3D.PLUS_I, Vector3D.PLUS_K);
        PVCoordinates pv =
            new PVCoordinates(new Vector3D(28823536.58654545, 5893400.545304828, 0),
                              new Vector3D(0, 0, 3680.853673522056));
        Attitude attitude = sunPointing.getState(date, pv, Frame.getJ2000());
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
                     Vector3D.angle(xDirection, sun.getPosition(date, Frame.getJ2000())),
                     1.0e-15);

    }

    public static Test suite() {
        return new TestSuite(CelestialBodyPointingTest.class);
    }

}

