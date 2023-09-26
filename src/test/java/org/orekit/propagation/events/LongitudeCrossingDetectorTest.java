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

import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.NadirPointing;
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
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.propagation.events.EventsLogger.LoggedEvent;
import org.orekit.propagation.events.handlers.ContinueOnEvent;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

public class LongitudeCrossingDetectorTest {

    @Test
    public void testRegularCrossing() {

        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            Constants.WGS84_EARTH_FLATTENING,
                                                            FramesFactory.getITRF(IERSConventions.IERS_2010, true));

        LongitudeCrossingDetector d =
                new LongitudeCrossingDetector(earth, FastMath.toRadians(10.0)).
                withMaxCheck(60).
                withThreshold(1.e-6).
                withHandler(new ContinueOnEvent());

        Assertions.assertEquals(60.0, d.getMaxCheckInterval().currentInterval(null), 1.0e-15);
        Assertions.assertEquals(1.0e-6, d.getThreshold(), 1.0e-15);
        Assertions.assertEquals(10.0, FastMath.toDegrees(d.getLongitude()), 1.0e-14);
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
        AbsoluteDate previous = null;
        for (LoggedEvent e : logger.getLoggedEvents()) {
            SpacecraftState state = e.getState();
            double longitude = earth.transform(state.getPosition(earth.getBodyFrame()),
                                              earth.getBodyFrame(), null).getLongitude();
            Assertions.assertEquals(10.0, FastMath.toDegrees(longitude), 3.5e-7);
            if (previous != null) {
                // same time interval regardless of increasing/decreasing,
                // as increasing/decreasing flag is irrelevant for this detector
                 Assertions.assertEquals(4954.70, state.getDate().durationFrom(previous), 1e10);
            }
            previous = state.getDate();
        }
        Assertions.assertEquals(16, logger.getLoggedEvents().size());

    }

    @Test
    public void testZigZag() {

        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            Constants.WGS84_EARTH_FLATTENING,
                                                            FramesFactory.getITRF(IERSConventions.IERS_2010, true));

        LongitudeCrossingDetector d =
                new LongitudeCrossingDetector(600.0, 1.e-6, earth, FastMath.toRadians(-100.0)).
                withHandler(new ContinueOnEvent());

        Assertions.assertEquals(600.0, d.getMaxCheckInterval().currentInterval(null), 1.0e-15);
        Assertions.assertEquals(1.0e-6, d.getThreshold(), 1.0e-15);
        Assertions.assertEquals(-100.0, FastMath.toDegrees(d.getLongitude()), 1.0e-14);
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
        double[] expectedLatitudes = new double[] { -6.5394381901, -0.4918760372, +6.5916016832 };
        Assertions.assertEquals(3, logger.getLoggedEvents().size());
        for (int i = 0; i < 3; ++i) {
            SpacecraftState state = logger.getLoggedEvents().get(i).getState();
            GeodeticPoint gp = earth.transform(state.getPosition(earth.getBodyFrame()),
                                               earth.getBodyFrame(), null);
            Assertions.assertEquals(expectedLatitudes[i], FastMath.toDegrees(gp.getLatitude()),  1.0e-10);
            Assertions.assertEquals(-100.0,               FastMath.toDegrees(gp.getLongitude()), 1.2e-9);
        }

    }

    @Test
    public void testIssue997() {

        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            Constants.WGS84_EARTH_FLATTENING,
                                                            FramesFactory.getITRF(IERSConventions.IERS_2010, true));

        TLE orekitTle = new TLE("1 25544U 98067A   22257.49623145  .00008872  00000-0  16209-3 0  9998",
                                "2 25544  51.6424 254.1285 0002269 232.3655 233.3422 15.50204391359035");
        AttitudeProvider attitudeProvider = new NadirPointing(FramesFactory.getTEME(), earth);
        TLEPropagator sgp4 = TLEPropagator.selectExtrapolator(orekitTle, attitudeProvider, 466615.0);

        final double startLon = FastMath.toRadians(0.0);
        EventDetector lonEntryDetector = new LongitudeCrossingDetector(earth, startLon).
                                         withHandler(new ContinueOnEvent());
        final double endLon = FastMath.toRadians(10.0);
        EventDetector lonExitDetector  = new LongitudeCrossingDetector(earth, endLon).
                                         withHandler(new ContinueOnEvent());
        EventsLogger logger= new EventsLogger();
        sgp4.addEventDetector(logger.monitorDetector(lonEntryDetector));
        sgp4.addEventDetector(logger.monitorDetector(lonExitDetector));

        sgp4.propagate(new AbsoluteDate("2022-09-14T21:54:34.39728Z", TimeScalesFactory.getUTC()));
        List<EventsLogger.LoggedEvent> loggedEvents = logger.getLoggedEvents();
        Assertions.assertEquals(12, loggedEvents.size());
        for (int i = 0; i < loggedEvents.size(); ++i) {
            final SpacecraftState s  = loggedEvents.get(i).getState();
            final GeodeticPoint   gp = earth.transform(s.getPosition(), s.getFrame(), s.getDate());
            if (i % 2 == 0) {
                Assertions.assertSame(lonEntryDetector, loggedEvents.get(i).getEventDetector());
                Assertions.assertEquals(startLon, gp.getLongitude(), 1.0e-9);
            } else {
                Assertions.assertSame(lonExitDetector, loggedEvents.get(i).getEventDetector());
                Assertions.assertEquals(endLon, gp.getLongitude(), 1.0e-9);
            }
        }

    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}

