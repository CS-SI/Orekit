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
package org.orekit.propagation.events;

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.PVCoordinates;

public class EventShifterTest {

    private double           mu;
    private AbsoluteDate     iniDate;
    private Propagator       propagator;
    private List<EventEntry> log;

    private double sunRadius = 696000000.;
    private double earthRadius = 6400000.;

    @Test
    public void testNegNeg() throws OrekitException {
        propagator.addEventDetector(createRawDetector("raw increasing", "raw decreasing", 1.0e-9));
        EclipseDetector raw = createRawDetector("shifted increasing", "shifted decreasing", 1.0e-3);
        final EventHandler<? super EclipseDetector> h = raw.getHandler();
        raw = raw.withHandler(new EventHandler<EclipseDetector>() {

            @Override
            public Action eventOccurred(SpacecraftState s,
                                        EclipseDetector detector,
                                        boolean increasing)
              throws OrekitException {
                h.eventOccurred(s, detector, increasing);
                return Action.RESET_STATE;
            }

            @Override
            public SpacecraftState resetState(EclipseDetector detector,
                                              SpacecraftState oldState)
                                                  throws OrekitException {
                return h.resetState(detector, oldState);
            }

        });
        EventShifter<EclipseDetector> shifter = new EventShifter<EclipseDetector>(raw, true, -15, -20).
                                                withMaxIter(200);
        Assert.assertEquals(-15, shifter.getIncreasingTimeShift(), 1.0e-15);
        Assert.assertEquals(-20, shifter.getDecreasingTimeShift(), 1.0e-15);
        Assert.assertEquals(200, shifter.getMaxIterationCount());
        Assert.assertEquals(100, raw.getMaxIterationCount());
        propagator.addEventDetector(shifter);
        propagator.addEventDetector(new EventShifter<EclipseDetector>(createRawDetector("unshifted increasing", "unshifted decreasing", 1.0e-3),
                                                                      false, -5, -10));
        propagator.propagate(iniDate.shiftedBy(6000));
        Assert.assertEquals(6, log.size());
        log.get(0).checkExpected(log.get(2).getDT() - 20, "shifted decreasing");
        log.get(1).checkExpected(log.get(2).getDT(),      "unshifted decreasing");
        log.get(3).checkExpected(log.get(5).getDT() - 15, "shifted increasing");
        log.get(4).checkExpected(log.get(5).getDT(),      "unshifted increasing");
    }

    @Test
    public void testNegPos() throws OrekitException {
        propagator.addEventDetector(createRawDetector("raw increasing", "raw decreasing", 1.0e-9));
        propagator.addEventDetector(new EventShifter<EclipseDetector>(createRawDetector("shifted increasing", "shifted decreasing", 1.0e-3),
                                                                      true, -15,  20));
        propagator.addEventDetector(new EventShifter<EclipseDetector>(createRawDetector("unshifted increasing", "unshifted decreasing", 1.0e-3),
                                                                      false, -5,  10));
        propagator.propagate(iniDate.shiftedBy(6000));
        Assert.assertEquals(6, log.size());
        log.get(1).checkExpected(log.get(0).getDT(),      "unshifted decreasing");
        log.get(2).checkExpected(log.get(0).getDT() + 20, "shifted decreasing");
        log.get(3).checkExpected(log.get(5).getDT() - 15, "shifted increasing");
        log.get(4).checkExpected(log.get(5).getDT(),      "unshifted increasing");
    }

    @Test
    public void testPosNeg() throws OrekitException {
        propagator.addEventDetector(createRawDetector("raw increasing", "raw decreasing", 1.0e-9));
        propagator.addEventDetector(new EventShifter<EclipseDetector>(createRawDetector("shifted increasing", "shifted decreasing", 1.0e-3),
                                                                      true,  15, -20));
        propagator.addEventDetector(new EventShifter<EclipseDetector>(createRawDetector("unshifted increasing", "unshifted decreasing", 1.0e-3),
                                                                      false,  5, -10));
        propagator.propagate(iniDate.shiftedBy(6000));
        Assert.assertEquals(6, log.size());
        log.get(0).checkExpected(log.get(2).getDT() - 20, "shifted decreasing");
        log.get(1).checkExpected(log.get(2).getDT(),      "unshifted decreasing");
        log.get(4).checkExpected(log.get(3).getDT(),      "unshifted increasing");
        log.get(5).checkExpected(log.get(3).getDT() + 15, "shifted increasing");
    }

    @Test
    public void testPosPos() throws OrekitException {
        propagator.addEventDetector(createRawDetector("raw increasing", "raw decreasing", 1.0e-9));
        propagator.addEventDetector(new EventShifter<EclipseDetector>(createRawDetector("shifted increasing", "shifted decreasing", 1.0e-3),
                                                                      true,  15,  20));
        propagator.addEventDetector(new EventShifter<EclipseDetector>(createRawDetector("unshifted increasing", "unshifted decreasing", 1.0e-3),
                                                                      false,  5,  10));
        propagator.propagate(iniDate.shiftedBy(6000));
        Assert.assertEquals(6, log.size());
        log.get(1).checkExpected(log.get(0).getDT(),      "unshifted decreasing");
        log.get(2).checkExpected(log.get(0).getDT() + 20, "shifted decreasing");
        log.get(4).checkExpected(log.get(3).getDT(),      "unshifted increasing");
        log.get(5).checkExpected(log.get(3).getDT() + 15, "shifted increasing");
    }

    @Test
    public void testIncreasingError() throws OrekitException {
        propagator.addEventDetector(createRawDetector("raw increasing", "raw decreasing", 2.0e-9));
        propagator.addEventDetector(new EventShifter<EclipseDetector>(createRawDetector("-10s increasing", "-10s decreasing", 2.0e-3),
                                                                      true, -10, -10));
        propagator.addEventDetector(new EventShifter<EclipseDetector>(createRawDetector("-100s increasing", "-100s decreasing", 3.0e-2),
                                                                      true, -100, -100));
        propagator.addEventDetector(new EventShifter<EclipseDetector>(createRawDetector("-1000s increasing", "-1000s decreasing", 5.0),
                                                                      true, -1000, -1000));
        propagator.propagate(iniDate.shiftedBy(20100));

        // the raw eclipses (not all within the propagation range) are at times:
        // [ 2300.238,  4376.986]
        // [ 8210.859, 10287.573]
        // [14121.478, 16198.159]
        // [20032.098, 22108.745]
        // [25942.717, 28019.331]
        // [31853.335, 33929.916]
        // [37763.954, 39840.500]
        Assert.assertEquals(28, log.size());
        for (int i = 0; i < log.size() / 4; ++i) {
            EventEntry ref = log.get(4 * i + 3);
            String increasingOrDecreasing = ref.getName().split(" ")[1];
            log.get(4 * i + 0).checkExpected(ref.getDT() - 1000, "-1000s " + increasingOrDecreasing);
            log.get(4 * i + 1).checkExpected(ref.getDT() -  100, "-100s "  + increasingOrDecreasing);
            log.get(4 * i + 2).checkExpected(ref.getDT() -   10, "-10s "   + increasingOrDecreasing);
        }

        for (EventEntry entry : log) {
            double error = entry.getTimeError();
            if (entry.name.contains("10s")) {
                Assert.assertTrue(error > 0.00001);
                Assert.assertTrue(error < 0.0003);
            } else if (entry.name.contains("100s")) {
                Assert.assertTrue(error > 0.002);
                Assert.assertTrue(error < 0.03);
            } else if (entry.name.contains("1000s")) {
                Assert.assertTrue(error > 0.7);
                Assert.assertTrue(error < 3.3);
            }
        }
    }

    private EclipseDetector createRawDetector(final String nameIncreasing, final String nameDecreasing,
                                              final double tolerance)
        throws OrekitException {
        return new EclipseDetector(60., 1.e-10,
                                   CelestialBodyFactory.getSun(), sunRadius,
                                   CelestialBodyFactory.getEarth(), earthRadius).
                                   withHandler(new EventHandler<EclipseDetector>() {
                                       public Action eventOccurred(SpacecraftState s, EclipseDetector detector,
                                                                   boolean increasing) {
                                           log.add(new EventEntry(s.getDate().durationFrom(iniDate), tolerance,
                                                                  increasing ? nameIncreasing : nameDecreasing));
                                           return Action.CONTINUE;
                                       }
                                   });
    }

    @Before
    public void setUp() {
        try {
            Utils.setDataRoot("regular-data");
            mu  = 3.9860047e14;
            double ae  = 6.378137e6;
            double c20 = -1.08263e-3;
            double c30 = 2.54e-6;
            double c40 = 1.62e-6;
            double c50 = 2.3e-7;
            double c60 = -5.5e-7;
            final Vector3D position  = new Vector3D(-6142438.668, 3492467.560, -25767.25680);
            final Vector3D velocity  = new Vector3D(505.8479685, 942.7809215, 7435.922231);
            iniDate = new AbsoluteDate(1969, 7, 28, 4, 0, 0.0, TimeScalesFactory.getTT());
            final Orbit orbit = new EquinoctialOrbit(new PVCoordinates(position,  velocity),
                                                     FramesFactory.getGCRF(), iniDate, mu);
            propagator =
                new EcksteinHechlerPropagator(orbit, ae, mu, c20, c30, c40, c50, c60);
            log = new ArrayList<EventEntry>();
        } catch (OrekitException oe) {
            Assert.fail(oe.getLocalizedMessage());
        }
    }

    @After
    public void tearDown() {
        iniDate = null;
        propagator = null;
        log = null;
    }

    private static class EventEntry {

        private final double dt;
        private double expectedDT;
        private final double tolerance;
        private final String name;

        public EventEntry(final double dt, final double tolerance, final String name) {
            this.dt         = dt;
            this.expectedDT = Double.NaN;
            this.tolerance  = tolerance;
            this.name       = name;
        }

        public void checkExpected(final double expectedDT, final String name) {
            this.expectedDT = expectedDT;
            Assert.assertEquals(expectedDT, dt, tolerance);
            Assert.assertEquals(name, this.name);
        }

        public double getDT() {
            return dt;
        }

        public String getName() {
            return name;
        }

        public double getTimeError() {
            return FastMath.abs(dt - expectedDT);
        }

    }

}

