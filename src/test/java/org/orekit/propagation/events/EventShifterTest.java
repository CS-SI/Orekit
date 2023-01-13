/* Copyright 2002-2023 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.events.Action;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
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
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

import java.util.ArrayList;
import java.util.List;

public class EventShifterTest {

    private double           mu;
    private AbsoluteDate     iniDate;
    private Propagator       propagator;
    private List<EventEntry> log;

    private double sunRadius = 696000000.;
    private double earthRadius = 6400000.;

    @Test
    public void testNegNeg() {
        propagator.addEventDetector(createRawDetector("raw increasing", "raw decreasing", 1.0e-9));
        EclipseDetector raw = createRawDetector("shifted increasing", "shifted decreasing", 1.0e-3);
        final EventHandler h = raw.getHandler();
        raw = raw.withHandler(new EventHandler() {

            @Override
            public Action eventOccurred(SpacecraftState s,
                                        EventDetector detector,
                                        boolean increasing) {
                h.eventOccurred(s, detector, increasing);
                return Action.RESET_STATE;
            }

            @Override
            public SpacecraftState resetState(EventDetector detector, SpacecraftState oldState)
                                                  {
                return h.resetState(detector, oldState);
            }

        });
        EventShifter shifter = new EventShifter(raw, true, -15, -20).withMaxIter(200);
        Assertions.assertEquals(-15, shifter.getIncreasingTimeShift(), 1.0e-15);
        Assertions.assertEquals(-20, shifter.getDecreasingTimeShift(), 1.0e-15);
        Assertions.assertEquals(200, shifter.getMaxIterationCount());
        Assertions.assertEquals(100, raw.getMaxIterationCount());
        propagator.addEventDetector(shifter);
        propagator.addEventDetector(new EventShifter(createRawDetector("unshifted increasing", "unshifted decreasing", 1.0e-3),
                                                     false, -5, -10));
        propagator.propagate(iniDate.shiftedBy(6000));
        Assertions.assertEquals(6, log.size());
        log.get(0).checkExpected(log.get(2).getDT() - 20, "shifted decreasing");
        log.get(1).checkExpected(log.get(2).getDT(),      "unshifted decreasing");
        log.get(3).checkExpected(log.get(5).getDT() - 15, "shifted increasing");
        log.get(4).checkExpected(log.get(5).getDT(),      "unshifted increasing");
    }

    @Test
    public void testNegPos() {
        propagator.addEventDetector(createRawDetector("raw increasing", "raw decreasing", 1.0e-9));
        propagator.addEventDetector(new EventShifter(createRawDetector("shifted increasing", "shifted decreasing", 1.0e-3),
                                                     true, -15,  20));
        propagator.addEventDetector(new EventShifter(createRawDetector("unshifted increasing", "unshifted decreasing", 1.0e-3),
                                                     false, -5,  10));
        propagator.propagate(iniDate.shiftedBy(6000));
        Assertions.assertEquals(6, log.size());
        log.get(1).checkExpected(log.get(0).getDT(),      "unshifted decreasing");
        log.get(2).checkExpected(log.get(0).getDT() + 20, "shifted decreasing");
        log.get(3).checkExpected(log.get(5).getDT() - 15, "shifted increasing");
        log.get(4).checkExpected(log.get(5).getDT(),      "unshifted increasing");
    }

    @Test
    public void testPosNeg() {
        propagator.addEventDetector(createRawDetector("raw increasing", "raw decreasing", 1.0e-9));
        propagator.addEventDetector(new EventShifter(createRawDetector("shifted increasing", "shifted decreasing", 1.0e-3),
                                                     true,  15, -20));
        propagator.addEventDetector(new EventShifter(createRawDetector("unshifted increasing", "unshifted decreasing", 1.0e-3),
                                                     false,  5, -10));
        propagator.propagate(iniDate.shiftedBy(6000));
        Assertions.assertEquals(6, log.size());
        log.get(0).checkExpected(log.get(2).getDT() - 20, "shifted decreasing");
        log.get(1).checkExpected(log.get(2).getDT(),      "unshifted decreasing");
        log.get(4).checkExpected(log.get(3).getDT(),      "unshifted increasing");
        log.get(5).checkExpected(log.get(3).getDT() + 15, "shifted increasing");
    }

    @Test
    public void testPosPos() {
        propagator.addEventDetector(createRawDetector("raw increasing", "raw decreasing", 1.0e-9));
        propagator.addEventDetector(new EventShifter(createRawDetector("shifted increasing", "shifted decreasing", 1.0e-3),
                                                     true,  15,  20));
        propagator.addEventDetector(new EventShifter(createRawDetector("unshifted increasing", "unshifted decreasing", 1.0e-3),
                                                     false,  5,  10));
        propagator.propagate(iniDate.shiftedBy(6000));
        Assertions.assertEquals(6, log.size());
        log.get(1).checkExpected(log.get(0).getDT(),      "unshifted decreasing");
        log.get(2).checkExpected(log.get(0).getDT() + 20, "shifted decreasing");
        log.get(4).checkExpected(log.get(3).getDT(),      "unshifted increasing");
        log.get(5).checkExpected(log.get(3).getDT() + 15, "shifted increasing");
    }

    @Test
    public void testIncreasingError() {
        final EclipseDetector raw0000 = createRawDetector("raw increasing",    "raw decreasing", 2.0e-9);
        final EclipseDetector raw0010 = createRawDetector("-10s increasing",   "-10s decreasing", 2.0e-3);
        final EclipseDetector raw0100 = createRawDetector("-100s increasing",  "-100s decreasing", 3.0e-2);
        final EclipseDetector raw1000 = createRawDetector("-1000s increasing", "-1000s decreasing", 5.0);
        final EventShifter shift0010 = new EventShifter(raw0010, true,   -10,   -10);
        final EventShifter shift0100 = new EventShifter(raw0100, true,  -100,  -100);
        final EventShifter shift1000 = new EventShifter(raw1000, true, -1000, -1000);
        Assertions.assertSame(raw0010, shift0010.getDetector());
        Assertions.assertSame(raw0100, shift0100.getDetector());
        Assertions.assertSame(raw1000, shift1000.getDetector());
        propagator.addEventDetector(raw0000);
        propagator.addEventDetector(shift0010);
        propagator.addEventDetector(shift0100);
        propagator.addEventDetector(shift1000);
        propagator.propagate(iniDate.shiftedBy(20100));

        // the raw eclipses (not all within the propagation range) are at times:
        // [ 2300.238,  4376.986]
        // [ 8210.859, 10287.573]
        // [14121.478, 16198.159]
        // [20032.098, 22108.745]
        // [25942.717, 28019.331]
        // [31853.335, 33929.916]
        // [37763.954, 39840.500]
        Assertions.assertEquals(28, log.size());
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
                Assertions.assertTrue(error > 1.0e-6);
                Assertions.assertTrue(error < 3.0e-6);
            } else if (entry.name.contains("100s")) {
                Assertions.assertTrue(error > 0.001);
                Assertions.assertTrue(error < 0.003);
            } else if (entry.name.contains("1000s")) {
                Assertions.assertTrue(error > 0.7);
                Assertions.assertTrue(error < 1.1);
            }
        }
    }

    private EclipseDetector createRawDetector(final String nameIncreasing, final String nameDecreasing,
                                              final double tolerance) {
        return new EclipseDetector(CelestialBodyFactory.getSun(), sunRadius,
                                   new OneAxisEllipsoid(earthRadius,
                                                        0.0,
                                                        FramesFactory.getITRF(IERSConventions.IERS_2010, true))).
               withMaxCheck(60.0).
               withThreshold(1.0e-10).
               withHandler(new EventHandler() {
                                       public Action eventOccurred(SpacecraftState s, EventDetector detector, boolean increasing) {
                                           log.add(new EventEntry(s.getDate().durationFrom(iniDate), tolerance,
                                                                  increasing ? nameIncreasing : nameDecreasing));
                                           return Action.CONTINUE;
                                       }
                                   });
    }

    @BeforeEach
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
            Assertions.fail(oe.getLocalizedMessage());
        }
    }

    @AfterEach
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
            Assertions.assertEquals(expectedDT, dt, tolerance);
            Assertions.assertEquals(name, this.name);
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

