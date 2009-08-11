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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.Vector3D;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;


public class InertialAttitudeTest {

    private AbsoluteDate t0;
    private Orbit        orbit0;

    @Test
    public void testIsInertial() throws OrekitException {
        InertialLaw law = new InertialLaw(new Rotation(new Vector3D(0.6, 0.48, 0.64), 0.9));
        KeplerianPropagator propagator = new KeplerianPropagator(orbit0, law);
        Attitude initial = propagator.propagate(t0).getAttitude();
        for (double t = 0; t < 10000.0; t += 100) {
            Attitude attitude =
                propagator.propagate(new AbsoluteDate(t0, t)).getAttitude();
            Rotation evolution = attitude.getRotation().applyTo(initial.getRotation().revert());
            assertEquals(0, evolution.getAngle(), 1.0e-10);
            assertEquals(FramesFactory.getEME2000(), attitude.getReferenceFrame());
        }
    }

    public void testCompensateMomentum() throws OrekitException {
        InertialLaw law = new InertialLaw(new Rotation(new Vector3D(-0.64, 0.6, 0.48), 0.2));
        KeplerianPropagator propagator = new KeplerianPropagator(orbit0, law);
        Attitude initial = propagator.propagate(t0).getAttitude();
        for (double t = 0; t < 10000.0; t += 100) {
            Attitude attitude =
                propagator.propagate(new AbsoluteDate(t0, t)).getAttitude();
            Rotation evolution = attitude.getRotation().applyTo(initial.getRotation().revert());
            assertEquals(0, evolution.getAngle(), 1.0e-10);
            assertEquals(FramesFactory.getEME2000(), attitude.getReferenceFrame());
        }
    }

    @Before
    public void setUp() {
        try {
            Utils.setDataRoot("regular-data");

            t0 = new AbsoluteDate(new DateComponents(2008, 06, 03), TimeComponents.H12,
                                  TimeScalesFactory.getUTC());
            orbit0 =
                new KeplerianOrbit(12345678.9, 0.001, 2.3, 0.1, 3.04, 2.4,
                                   KeplerianOrbit.TRUE_ANOMALY, FramesFactory.getEME2000(),
                                   t0, 3.986004415e14);
        } catch (OrekitException oe) {
            fail(oe.getMessage());
        }
    }

    @After
    public void tearDown() {
        t0     = null;
        orbit0 = null;
    }

}

