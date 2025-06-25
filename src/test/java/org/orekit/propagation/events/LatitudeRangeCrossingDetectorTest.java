/* Copyright 2023-2025 Alberto Ferrero
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Alberto Ferrero licenses this file to You under the Apache License, Version 2.0
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
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.propagation.events.EventsLogger.LoggedEvent;
import org.orekit.propagation.events.handlers.ContinueOnEvent;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

public class LatitudeRangeCrossingDetectorTest {

    @Test
    public void testRegularCrossing() {

        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            Constants.WGS84_EARTH_FLATTENING,
                                                            FramesFactory.getITRF(IERSConventions.IERS_2010, true));

        LatitudeRangeCrossingDetector d =
                new LatitudeRangeCrossingDetector(60.0, 1.e-6, earth,
                    FastMath.toRadians(50.0), FastMath.toRadians(60.0)).
                withHandler(new ContinueOnEvent());

        Assertions.assertEquals(60.0, d.getMaxCheckInterval().currentInterval(null, true), 1.0e-15);
        Assertions.assertEquals(1.0e-6, d.getThreshold(), 1.0e-15);
        Assertions.assertEquals(50.0, FastMath.toDegrees(d.getFromLatitude()), 1.0e-14);
        Assertions.assertEquals(60.0, FastMath.toDegrees(d.getToLatitude()), 1.0e-14);
        Assertions.assertEquals(AbstractDetector.DEFAULT_MAX_ITER, d.getMaxIterationCount());

        final TimeScale utc = TimeScalesFactory.getUTC();
        final Vector3D position = new Vector3D(-6142438.668, 3492467.56, -25767.257);
        final Vector3D velocity = new Vector3D(505.848, 942.781, 7435.922);
        final AbsoluteDate date = new AbsoluteDate(2003, 9, 16, utc);
        final Orbit orbit = new EquinoctialOrbit(new PVCoordinates(position,  velocity),
                                                 FramesFactory.getEME2000(), date,
                                                 Constants.EIGEN5C_EARTH_MU);

        Propagator propagator =
            new EcksteinHechlerPropagator(orbit,
                                          Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS,
                                          Constants.EIGEN5C_EARTH_MU,
                                          Constants.EIGEN5C_EARTH_C20,
                                          Constants.EIGEN5C_EARTH_C30,
                                          Constants.EIGEN5C_EARTH_C40,
                                          Constants.EIGEN5C_EARTH_C50,
                                          Constants.EIGEN5C_EARTH_C60);

        EventsLogger logger = new EventsLogger();
        propagator.addEventDetector(logger.monitorDetector(d));

        propagator.propagate(date.shiftedBy(Constants.JULIAN_DAY));
        for (LoggedEvent e : logger.getLoggedEvents()) {
            SpacecraftState state = e.getState();
            double latitude = earth.transform(state.getPVCoordinates(earth.getBodyFrame()).getPosition(),
                earth.getBodyFrame(), null).getLatitude();
            if (e.isIncreasing()) {
                if (state.getVelocity().getZ() < 0) {
                    // entering northward
                    Assertions.assertEquals(60.0, FastMath.toDegrees(latitude), FastMath.toRadians(1e-4));
                } else {
                    // entering southward
                    Assertions.assertEquals(50.0, FastMath.toDegrees(latitude), FastMath.toRadians(1e-4));
                }
            } else {
                if (state.getVelocity().getZ() < 0) {
                    // exiting southward
                    Assertions.assertEquals(50.0, FastMath.toDegrees(latitude), FastMath.toRadians(1e-4));
                } else {
                    // exiting northward
                    Assertions.assertEquals(60.0, FastMath.toDegrees(latitude), FastMath.toRadians(1e-4));
                }
            }
        }
        Assertions.assertEquals(30 * 2, logger.getLoggedEvents().size());

    }

    @Test
    public void testNoCrossing() {

        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            Constants.WGS84_EARTH_FLATTENING,
                                                            FramesFactory.getITRF(IERSConventions.IERS_2010, true));

        LatitudeRangeCrossingDetector d =
                new LatitudeRangeCrossingDetector(10.0, 1.e-6, earth,
                    FastMath.toRadians(82.0),  FastMath.toRadians(87.0)).
                withHandler(new ContinueOnEvent());

        Assertions.assertEquals(10.0, d.getMaxCheckInterval().currentInterval(null, true), 1.0e-15);
        Assertions.assertEquals(1.0e-6, d.getThreshold(), 1.0e-15);
        Assertions.assertEquals(82.0, FastMath.toDegrees(d.getFromLatitude()), 1.0e-14);
        Assertions.assertEquals(87.0, FastMath.toDegrees(d.getToLatitude()), 1.0e-14);
        Assertions.assertEquals(AbstractDetector.DEFAULT_MAX_ITER, d.getMaxIterationCount());

        final TimeScale utc = TimeScalesFactory.getUTC();
        final Vector3D position = new Vector3D(-6142438.668, 3492467.56, -25767.257);
        final Vector3D velocity = new Vector3D(505.848, 942.781, 7435.922);
        final AbsoluteDate date = new AbsoluteDate(2003, 9, 16, utc);
        final Orbit orbit = new EquinoctialOrbit(new PVCoordinates(position,  velocity),
                                                 FramesFactory.getEME2000(), date,
                                                 Constants.EIGEN5C_EARTH_MU);

        Propagator propagator =
            new EcksteinHechlerPropagator(orbit,
                                          Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS,
                                          Constants.EIGEN5C_EARTH_MU,
                                          Constants.EIGEN5C_EARTH_C20,
                                          Constants.EIGEN5C_EARTH_C30,
                                          Constants.EIGEN5C_EARTH_C40,
                                          Constants.EIGEN5C_EARTH_C50,
                                          Constants.EIGEN5C_EARTH_C60);

        EventsLogger logger = new EventsLogger();
        propagator.addEventDetector(logger.monitorDetector(d));

        propagator.propagate(date.shiftedBy(Constants.JULIAN_DAY));
        Assertions.assertEquals(0, logger.getLoggedEvents().size());

    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}

