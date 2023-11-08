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

import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.models.AtmosphericRefractionModel;
import org.orekit.models.earth.EarthITU453AtmosphereRefraction;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.events.EventsLogger.LoggedEvent;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

import java.util.List;

public class GroundAtNightDetectorTest {

    @Test
    public void testMidLatitudeCivilNoRefraction() {
        checkDuration(FastMath.toRadians(43.0), FastMath.toRadians(0.0),
                      GroundAtNightDetector.CIVIL_DAWN_DUSK_ELEVATION,
                      null, 45037.367);
    }

    @Test
    public void testMidLatitudeCivilITURefraction() {
        checkDuration(FastMath.toRadians(43.0), FastMath.toRadians(0.0),
                      GroundAtNightDetector.CIVIL_DAWN_DUSK_ELEVATION,
                      new EarthITU453AtmosphereRefraction(0.0), 43909.148);
    }

    @Test
    public void testMidLatitudeNauticalNoRefraction() {
        checkDuration(FastMath.toRadians(43.0), FastMath.toRadians(0.0),
                      GroundAtNightDetector.NAUTICAL_DAWN_DUSK_ELEVATION,
                      null, 41045.750);
    }

    @Test
    public void testMidLatitudeNauticalITURefraction() {
        checkDuration(FastMath.toRadians(43.0), FastMath.toRadians(0.0),
                      GroundAtNightDetector.NAUTICAL_DAWN_DUSK_ELEVATION,
                      new EarthITU453AtmosphereRefraction(0.0), 39933.656);
    }

    @Test
    public void testMidLatitudeAstronomicalNoRefraction() {
        checkDuration(FastMath.toRadians(43.0), FastMath.toRadians(0.0),
                      GroundAtNightDetector.ASTRONOMICAL_DAWN_DUSK_ELEVATION,
                      null, 37097.821);
    }

    @Test
    public void testMidLatitudeAstronomicalITURefraction() {
        checkDuration(FastMath.toRadians(43.0), FastMath.toRadians(0.0),
                      GroundAtNightDetector.ASTRONOMICAL_DAWN_DUSK_ELEVATION,
                      new EarthITU453AtmosphereRefraction(0.0), 35991.314);
    }

    @Test
    public void testHighLatitudeAstronomicalITURefraction() {
        checkDuration(FastMath.toRadians(84.0), FastMath.toRadians(0.0),
                      GroundAtNightDetector.ASTRONOMICAL_DAWN_DUSK_ELEVATION,
                      new EarthITU453AtmosphereRefraction(0.0), Double.NaN);
    }

    private void checkDuration(double latitude, double longitude, double dawnDuskElevation,
                               AtmosphericRefractionModel refractionModel,
                               double expectedDuration) {
        OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                      Constants.WGS84_EARTH_FLATTENING,
                                                      FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        TimeScale utc = TimeScalesFactory.getUTC();
        CircularOrbit o = new CircularOrbit(7200000.0, 1.0e-3, 2.0e-4,
                                            FastMath.toRadians(50.0), FastMath.toRadians(134.0),
                                            FastMath.toRadians(21.0), PositionAngleType.MEAN, FramesFactory.getGCRF(),
                                            new AbsoluteDate("2003-02-14T14:02:03.000", utc),
                                            Constants.EIGEN5C_EARTH_MU);

        TopocentricFrame topo = new TopocentricFrame(earth, new GeodeticPoint(latitude, longitude, 0.0), "");
        Propagator p = new KeplerianPropagator(o);
        EventsLogger logger = new EventsLogger();
        p.addEventDetector(logger.monitorDetector(new GroundAtNightDetector(topo,
                                                                            CelestialBodyFactory.getSun(),
                                                                            dawnDuskElevation, refractionModel).
                                                  withMaxCheck(120.0)));
        p.propagate(o.getDate().shiftedBy(Constants.JULIAN_DAY));
        List<LoggedEvent> events = logger.getLoggedEvents();
        if (Double.isNaN(expectedDuration)) {
            Assertions.assertEquals(0, events.size());
        } else {
            Assertions.assertEquals(2, events.size());
            Assertions.assertEquals(expectedDuration,
                                events.get(1).getState().getDate().durationFrom(events.get(0).getState().getDate()),
                                1.0e-3);
        }

    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}

