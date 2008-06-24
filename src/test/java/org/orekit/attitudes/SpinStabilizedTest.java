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

public class SpinStabilizedTest extends TestCase {

    public SpinStabilizedTest(String name) {
        super(name);
    }

    public void testBBQMode() throws OrekitException {
        Sun sun = new Sun();
        AbsoluteDate date = new AbsoluteDate(AbsoluteDate.J2000_EPOCH, 12345.6789);
        double rate = 2.0 * Math.PI / (12 * 60);
        AttitudeLaw bbq =
            new SpinStabilized(new CelestialBodyPointed(Frame.getJ2000(), sun, Vector3D.PLUS_K,
                                     Vector3D.PLUS_I, Vector3D.PLUS_K),
                               date, Vector3D.PLUS_K, rate);
        PVCoordinates pv =
            new PVCoordinates(new Vector3D(28823536.58654545, 5893400.545304828, 0),
                              new Vector3D(0, 0, 3680.853673522056));
        Attitude attitude = bbq.getState(date, pv, Frame.getJ2000());
        Vector3D xDirection = attitude.getRotation().applyInverseTo(Vector3D.PLUS_I);
        assertEquals(Math.atan(1.0 / 5000.0),
                     Vector3D.angle(xDirection, sun.getPosition(date, Frame.getJ2000())),
                     1.0e-15);
        assertEquals(rate, attitude.getSpin().getNorm(), 1.0e-6);

    }

    public static Test suite() {
        return new TestSuite(SpinStabilizedTest.class);
    }

}

