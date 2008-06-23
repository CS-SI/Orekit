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
package org.orekit.forces.maneuvers;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.Vector3D;
import org.apache.commons.math.ode.nonstiff.GraggBulirschStoerIntegrator;
import org.apache.commons.math.util.MathUtils;
import org.orekit.attitudes.AttitudeLaw;
import org.orekit.attitudes.InertialLaw;
import org.orekit.errors.OrekitException;
import org.orekit.forces.maneuvers.ConstantThrustManeuver;
import org.orekit.frames.Frame;
import org.orekit.iers.IERSDirectoryCrawler;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.ChunkedDate;
import org.orekit.time.ChunkedTime;
import org.orekit.time.UTCScale;
import org.orekit.utils.PVCoordinates;



public class ConstantThrustManeuverTest extends TestCase {

    // Body mu
    private double mu;

    private CircularOrbit dummyOrbit(AbsoluteDate date) {
        return new CircularOrbit(new PVCoordinates(Vector3D.PLUS_I, Vector3D.PLUS_J),
                                 Frame.getJ2000(), date, mu);
    }

    public void testPositiveDuration() throws OrekitException {
        AbsoluteDate date = new AbsoluteDate(new ChunkedDate(2004, 01, 01),
                                             new ChunkedTime(23, 30, 00.000),
                                             UTCScale.getInstance());
        ConstantThrustManeuver maneuver =
            new ConstantThrustManeuver(date, 10.0, 400.0, 300.0, Vector3D.PLUS_K);
        EventDetector[] switches = maneuver.getEventsDetectors();

        Orbit o1 = dummyOrbit(new AbsoluteDate(date, - 1.0));
        assertTrue(switches[0].g(new SpacecraftState(o1)) < 0);
        Orbit o2 = dummyOrbit(new AbsoluteDate(date,   1.0));
        assertTrue(switches[0].g(new SpacecraftState(o2)) > 0);
        Orbit o3 = dummyOrbit(new AbsoluteDate(date,   9.0));
        assertTrue(switches[1].g(new SpacecraftState(o3)) < 0);
        Orbit o4 = dummyOrbit(new AbsoluteDate(date,  11.0));
        assertTrue(switches[1].g(new SpacecraftState(o4)) > 0);
    }
    
    public void testNegativeDuration() throws OrekitException {
        AbsoluteDate date = new AbsoluteDate(new ChunkedDate(2004, 01, 01),
                                             new ChunkedTime(23, 30, 00.000),
                                             UTCScale.getInstance());
        ConstantThrustManeuver maneuver =
            new ConstantThrustManeuver(date, -10.0, 400.0, 300.0, Vector3D.PLUS_K);
        EventDetector[] switches = maneuver.getEventsDetectors();

        Orbit o1 = dummyOrbit(new AbsoluteDate(date, -11.0));
        assertTrue(switches[0].g(new SpacecraftState(o1)) < 0);
        Orbit o2 = dummyOrbit(new AbsoluteDate(date,  -9.0));
        assertTrue(switches[0].g(new SpacecraftState(o2)) > 0);
        Orbit o3 = dummyOrbit(new AbsoluteDate(date,  -1.0));
        assertTrue(switches[1].g(new SpacecraftState(o3)) < 0);
        Orbit o4 = dummyOrbit(new AbsoluteDate(date,   1.0));
        assertTrue(switches[1].g(new SpacecraftState(o4)) > 0);
    }

    public void testRoughBehaviour() throws OrekitException {
        final double isp = 318;
        final double mass = 2500;
        final double a = 24396159;
        final double e = 0.72831215;
        final double i = Math.toRadians(7);
        final double omega = Math.toRadians(180);
        final double OMEGA = Math.toRadians(261);
        final double lv = 0;

        final double duration = 3653.99;
        final double f = 420;
        final double delta = Math.toRadians(-7.4978);
        final double alpha = Math.toRadians(351);
        final AttitudeLaw law = new InertialLaw(new Rotation(new Vector3D(alpha, delta), Vector3D.PLUS_I));

        final AbsoluteDate initDate = new AbsoluteDate(new ChunkedDate(2004, 01, 01),
                                                       new ChunkedTime(23, 30, 00.000),
                                                       UTCScale.getInstance());
        final Orbit orbit =
            new KeplerianOrbit(a, e, i, omega, OMEGA, lv, KeplerianOrbit.TRUE_ANOMALY,
                               Frame.getJ2000(), initDate, mu);
        final SpacecraftState initialState =
            new SpacecraftState(orbit, law.getState(initDate, orbit.getPVCoordinates(), orbit.getFrame()), mass);

        final AbsoluteDate fireDate = new AbsoluteDate(new ChunkedDate(2004, 01, 02),
                                                       new ChunkedTime(04, 15, 34.080),
                                                       UTCScale.getInstance());
        final ConstantThrustManeuver maneuver =
            new ConstantThrustManeuver(fireDate, duration, f, isp, Vector3D.PLUS_I);

        final NumericalPropagator propagator =
            new NumericalPropagator(new GraggBulirschStoerIntegrator(1e-50, 1000, 0, 1e-08));
        propagator.setInitialState(initialState);
        propagator.setAttitudeLaw(law);
        propagator.addForceModel(maneuver);
        final SpacecraftState finalorb = propagator.propagate(new AbsoluteDate(fireDate, 3800));

        assertEquals(2007.88245442614, finalorb.getMass(), 1e-10);
        assertEquals(2.6872, Math.toDegrees(MathUtils.normalizeAngle(finalorb.getI(), Math.PI)), 1e-4);
        assertEquals(28970, finalorb.getA()/1000, 1);

    }

    public void setUp() {
        System.setProperty(IERSDirectoryCrawler.IERS_ROOT_DIRECTORY, "regular-data");

        // Body mu
        mu = 3.9860047e14;
        
    }

    public static Test suite() {
        return new TestSuite(ConstantThrustManeuverTest.class);
    }

}
