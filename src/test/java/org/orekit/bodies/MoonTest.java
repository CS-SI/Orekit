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
import org.orekit.forces.Moon;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class MoonTest extends TestCase {

    public MoonTest(String name) {
        super(name);
    }

    public void testSpring() throws OrekitException {
        checkDirection(6868800.0,  -0.98153078, -0.19129129, 0.00223147);
    }

    public void testSummer() throws OrekitException {
        checkDirection(14731200.0, 0.52480279, -0.77707939, -0.34746173);
    }

    public void testAutomn() throws OrekitException {
        checkDirection(22766400.0, 0.05415817, 0.93023381, 0.36294898);
    }

    public void testWinter() throws OrekitException {
        checkDirection(30628800.0, -0.81434728, -0.55991656, -0.15274799);
    }

    public void checkDirection(double offsetJ2000, double x, double y, double z) throws OrekitException {
        Vector3D moon = new Moon().getPosition(new AbsoluteDate(AbsoluteDate.J2000_EPOCH, offsetJ2000), Frame.getJ2000());
        AbsoluteDate date = new AbsoluteDate(AbsoluteDate.J2000_EPOCH, offsetJ2000);
        moon = Frame.getJ2000().getTransformTo(Frame.getVeis1950(), date).transformPosition(moon);
        moon = moon.normalize();
        assertEquals(x, moon.getX(), 1.0e-7);
        assertEquals(y, moon.getY(), 1.0e-7);
        assertEquals(z, moon.getZ(), 1.0e-7);
    }

    public static Test suite() {
        return new TestSuite(MoonTest.class);
    }

}

