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
package org.orekit.utils;

import org.apache.commons.math.geometry.Vector3D;
import org.orekit.utils.PVCoordinates;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class PVCoordinatesTest
extends TestCase {

    public PVCoordinatesTest(String name) {
        super(name);
    }

    public void testDefaultConstructor() {
        assertEquals("{P(0.0, 0.0, 0.0), V(0.0, 0.0, 0.0)}", new PVCoordinates().toString());
    }

    public void testLinearConstructors() {
        PVCoordinates pv1 = new PVCoordinates(new Vector3D( 1,  0.1,  10),
                                              new Vector3D(-1, -0.1, -10));
        PVCoordinates pv2 = new PVCoordinates(new Vector3D( 2,  0.2,  20),
                                              new Vector3D(-2, -0.2, -20));
        PVCoordinates pv3 = new PVCoordinates(new Vector3D( 3,  0.3,  30),
                                              new Vector3D(-3, -0.3, -30));
        PVCoordinates pv4 = new PVCoordinates(new Vector3D( 4,  0.4,  40),
                                              new Vector3D(-4, -0.4, -40));
        checkPV(pv4, new PVCoordinates(4, pv1), 1.0e-15);
        checkPV(pv3, new PVCoordinates(1, pv1, 1, pv2), 1.0e-15);
        checkPV(new PVCoordinates(2, pv4), new PVCoordinates(3, pv1, 1, pv2, 1, pv3), 1.0e-15);
        checkPV(new PVCoordinates(3, pv3), new PVCoordinates(3, pv1, 1, pv2, 1, pv4), 1.0e-15);
        checkPV(new PVCoordinates(5, pv4), new PVCoordinates(4, pv1, 3, pv2, 2, pv3, 1, pv4), 1.0e-15);
    }

    public void testToString() {
        PVCoordinates pv =
            new PVCoordinates(new Vector3D( 1,  0.1,  10), new Vector3D(-1, -0.1, -10));
        assertEquals("{P(1.0, 0.1, 10.0), V(-1.0, -0.1, -10.0)}", pv.toString());
    }

    private void checkPV(PVCoordinates expected, PVCoordinates real, double epsilon) {
        assertEquals(expected.getPosition().getX(), real.getPosition().getX(), epsilon);
        assertEquals(expected.getPosition().getY(), real.getPosition().getY(), epsilon);
        assertEquals(expected.getPosition().getZ(), real.getPosition().getZ(), epsilon);
        assertEquals(expected.getVelocity().getX(), real.getVelocity().getX(), epsilon);
        assertEquals(expected.getVelocity().getY(), real.getVelocity().getY(), epsilon);
        assertEquals(expected.getVelocity().getZ(), real.getVelocity().getZ(), epsilon);
    }

    public static Test suite() {
        return new TestSuite(PVCoordinatesTest.class);
    }

}
