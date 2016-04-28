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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.propagation.events.EventsLogger.LoggedEvent;
import org.orekit.propagation.events.handlers.ContinueOnEvent;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

public class ElevationExtremumDetectorTest {

    @Test
    public void testLEO() throws OrekitException {

        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            Constants.WGS84_EARTH_FLATTENING,
                                                            FramesFactory.getITRF(IERSConventions.IERS_2010, true));

        final GeodeticPoint gp = new GeodeticPoint(FastMath.toRadians(51.0), FastMath.toRadians(66.6), 300.0);
        final ElevationExtremumDetector raw =
                new ElevationExtremumDetector(new TopocentricFrame(earth, gp, "test")).
                withMaxCheck(60).
                withThreshold(1.e-6).
                withHandler(new ContinueOnEvent<ElevationExtremumDetector>());
        final EventSlopeFilter<ElevationExtremumDetector> maxElevationDetector =
                new EventSlopeFilter<ElevationExtremumDetector>(raw, FilterType.TRIGGER_ONLY_DECREASING_EVENTS);

        Assert.assertEquals(60.0, raw.getMaxCheckInterval(), 1.0e-15);
        Assert.assertEquals(1.0e-6, raw.getThreshold(), 1.0e-15);
        Assert.assertEquals(AbstractDetector.DEFAULT_MAX_ITER, raw.getMaxIterationCount());
        Assert.assertEquals("test", raw.getTopocentricFrame().getName());

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
        propagator.addEventDetector(logger.monitorDetector(maxElevationDetector));

        propagator.propagate(date.shiftedBy(Constants.JULIAN_DAY));
        int visibleEvents = 0;
        for (LoggedEvent e : logger.getLoggedEvents()) {
            final double eMinus = raw.getElevation(e.getState().shiftedBy(-10.0));
            final double e0     = raw.getElevation(e.getState());
            final double ePlus  = raw.getElevation(e.getState().shiftedBy(+10.0));
            if (e0 > FastMath.toRadians(5.0)) {
                ++visibleEvents;
            }
            Assert.assertTrue(e0 > eMinus);
            Assert.assertTrue(e0 > ePlus);
        }
        Assert.assertEquals(15, logger.getLoggedEvents().size());
        Assert.assertEquals( 6, visibleEvents);

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}

