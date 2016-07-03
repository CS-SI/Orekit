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

import org.hipparchus.util.FastMath;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

public class BackAndForthDetectorTest {

    @Test
    public void testBackAndForth() throws OrekitException {

        final TimeScale utc = TimeScalesFactory.getUTC();

        final AbsoluteDate date0 = new AbsoluteDate(2006, 12, 27, 12,  0, 0.0, utc);
        final AbsoluteDate date1 = new AbsoluteDate(2006, 12, 27, 22, 50, 0.0, utc);
        final AbsoluteDate date2 = new AbsoluteDate(2006, 12, 27, 22, 58, 0.0, utc);

        // Orbit
        final double a = 7274000.;
        final double e = 0.00127;
        final double i = FastMath.toRadians(90.);
        final double w = FastMath.toRadians(0.);
        final double raan = FastMath.toRadians(12.5);
        final double lM = FastMath.toRadians(60.);
        Orbit iniOrb = new KeplerianOrbit(a, e, i, w, raan, lM,
                                          PositionAngle.MEAN, FramesFactory.getEME2000(), date0,
                                          Constants.WGS84_EARTH_MU);

        // Propagator
        KeplerianPropagator propagator = new KeplerianPropagator(iniOrb);

        // Station
        final GeodeticPoint stationPosition = new GeodeticPoint(FastMath.toRadians(0.), FastMath.toRadians(100.), 110.);
        final BodyShape earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                     Constants.WGS84_EARTH_FLATTENING,
                                                     FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        final TopocentricFrame stationFrame = new TopocentricFrame(earth, stationPosition, "");

        // Detector
        final Visibility visi = new Visibility();
        propagator.addEventDetector(new ElevationDetector(stationFrame).
                                    withConstantElevation(FastMath.toRadians(10.)).
                                    withHandler(visi));

        // Forward propagation (AOS + LOS)
        propagator.propagate(date1);
        propagator.propagate(date2);
        // Backward propagation (AOS + LOS)
        propagator.propagate(date1);
        propagator.propagate(date0);

        Assert.assertEquals(4, visi.getVisiNb());

    }

    private static class Visibility implements EventHandler<ElevationDetector> {
        private int _visiNb;

        public Visibility() {
            _visiNb = 0;
        }

        public int getVisiNb() {
            return _visiNb;
        }

        public Action eventOccurred(SpacecraftState s, ElevationDetector ed, boolean increasing) {
            _visiNb++;
            return Action.CONTINUE;
        }

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @After
    public void tearDown() {
    }

}

