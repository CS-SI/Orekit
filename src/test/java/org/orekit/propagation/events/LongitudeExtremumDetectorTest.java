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
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.events.handlers.ContinueOnEvent;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

public class LongitudeExtremumDetectorTest {

    @Test
    public void testNoCrossing() {

        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            Constants.WGS84_EARTH_FLATTENING,
                                                            FramesFactory.getITRF(IERSConventions.IERS_2010, true));

        LongitudeExtremumDetector d =
                new LongitudeExtremumDetector(earth).
                withMaxCheck(60).
                withThreshold(1.e-6).
                withHandler(new ContinueOnEvent());

        Assertions.assertEquals(60.0, d.getMaxCheckInterval().currentInterval(null), 1.0e-15);
        Assertions.assertEquals(1.0e-6, d.getThreshold(), 1.0e-15);
        Assertions.assertEquals(AbstractDetector.DEFAULT_MAX_ITER, d.getMaxIterationCount());
        Assertions.assertSame(earth, d.getBody());

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

    @Test
    public void testZigZag() {

        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            Constants.WGS84_EARTH_FLATTENING,
                                                            FramesFactory.getITRF(IERSConventions.IERS_2010, true));

        LongitudeExtremumDetector d =
                new LongitudeExtremumDetector(600.0, 1.e-6, earth).
                withHandler(new ContinueOnEvent());

        Assertions.assertEquals(600.0, d.getMaxCheckInterval().currentInterval(null), 1.0e-15);
        Assertions.assertEquals(1.0e-6, d.getThreshold(), 1.0e-15);
        Assertions.assertEquals(AbstractDetector.DEFAULT_MAX_ITER, d.getMaxIterationCount());

        KeplerianOrbit orbit =
                        new KeplerianOrbit(24464560.0, 0.7311, 0.122138, 3.10686, 1.00681,
                                           0.048363, PositionAngleType.MEAN,
                                           FramesFactory.getEME2000(),
                                           AbsoluteDate.J2000_EPOCH,
                                           Constants.EIGEN5C_EARTH_MU);


        Propagator propagator = new KeplerianPropagator(orbit);

        EventsLogger logger = new EventsLogger();
        propagator.addEventDetector(logger.monitorDetector(d));

        propagator.propagate(orbit.getDate().shiftedBy(Constants.JULIAN_DAY));
        double[] expectedLongitudes = new double[] {
            74.85115958654778, 39.51032449280883, -84.25729072475329, -119.598124966418, 116.63425894645886
        };
        double[] expectedLatitudes  = new double[] {
            -3.8404256460679336, 3.4237236065561536, -3.840419828222964, 3.4237214483413734, -3.840413360572555
        };
        Assertions.assertEquals(5, logger.getLoggedEvents().size());
        for (int i = 0; i < 5; ++i) {
            SpacecraftState state = logger.getLoggedEvents().get(i).getState();
            GeodeticPoint gp = earth.transform(state.getPosition(earth.getBodyFrame()),
                                               earth.getBodyFrame(), null);
            Assertions.assertEquals(expectedLongitudes[i], FastMath.toDegrees(gp.getLongitude()), 1.0e-10);
            Assertions.assertEquals(expectedLatitudes[i],  FastMath.toDegrees(gp.getLatitude()),  1.0e-10);
        }

    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}

