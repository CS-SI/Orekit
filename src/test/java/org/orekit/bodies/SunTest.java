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
package org.orekit.bodies;


import org.apache.commons.math.geometry.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.forces.Sun;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class SunTest extends TestCase {

    public SunTest(String name) {
        super(name);
    }

    public void testSpring() throws OrekitException {
        checkDirection(6868800.0, 0.99998858, -0.00021267, 0.00477363);
    }

    public void testSummer() throws OrekitException {
        checkDirection(14731200.0, 0.02817610, 0.91707719, 0.39771287);
    }

    public void testAutomn() throws OrekitException {
        checkDirection(22766400.0, -0.99919507, 0.03836434, 0.01172111);
    }

    public void testWinter() throws OrekitException {
        checkDirection(30628800.0, -0.02050325, -0.91726256, -0.39775496);
    }

    public void checkDirection(double offsetJ2000, double x, double y, double z) throws OrekitException {
        Vector3D sun = new Sun().getPosition(new AbsoluteDate(AbsoluteDate.J2000_EPOCH, offsetJ2000), Frame.getJ2000());
        AbsoluteDate date = new AbsoluteDate(AbsoluteDate.J2000_EPOCH, offsetJ2000);
        sun = Frame.getJ2000().getTransformTo(Frame.getVeis1950(), date).transformPosition(sun);
        sun = sun.normalize();
        assertEquals(x, sun.getX(), 1.0e-7);
        assertEquals(y, sun.getY(), 1.0e-7);
        assertEquals(z, sun.getZ(), 1.0e-7);
    }

    public static Test suite() {
        return new TestSuite(SunTest.class);
    }

}

