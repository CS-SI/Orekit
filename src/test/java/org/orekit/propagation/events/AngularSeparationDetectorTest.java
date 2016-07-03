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
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinatesProvider;

public class AngularSeparationDetectorTest {

    private OneAxisEllipsoid earth;
    private TopocentricFrame acatenango;
    private AbsoluteDate     iniDate;
    private Orbit            initialOrbit;
    private Propagator       propagator;

    @Test
    public void testCentralSunTransit() throws OrekitException {

        double proximityAngle = FastMath.toRadians(0.1);
        double maxCheck = 0.1 * proximityAngle / initialOrbit.getKeplerianMeanMotion();
        PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        AngularSeparationDetector detector =
            new AngularSeparationDetector(sun, acatenango, proximityAngle).
            withMaxCheck(maxCheck).
            withThreshold(1.0e-6);
        Assert.assertEquals(proximityAngle, detector.getProximityAngle(), 1.0e-15);
        Assert.assertSame(sun,    detector.getBeacon());
        Assert.assertSame(acatenango,  detector.getObserver());
        Assert.assertEquals(maxCheck, detector.getMaxCheckInterval(), 1.0e-15);
        propagator.addEventDetector(detector);
        final SpacecraftState finalState = propagator.propagate(iniDate.shiftedBy(7000.0));
        Assert.assertEquals(1921.1311, finalState.getDate().durationFrom(iniDate), 1.0e-3);

    }

    @Test
    public void testRegularProximity() throws OrekitException {

        double proximityAngle = FastMath.toRadians(5.0);
        double maxCheck = 0.1 * proximityAngle / initialOrbit.getKeplerianMeanMotion();
        PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        AngularSeparationDetector detector =
            new AngularSeparationDetector(sun, acatenango, proximityAngle).
            withMaxCheck(maxCheck).
            withThreshold(1.0e-6).
            withHandler(new EventHandler<AngularSeparationDetector>() {
                public Action eventOccurred(SpacecraftState s, AngularSeparationDetector detector, boolean increasing) {
                    if (increasing) {
                        Assert.assertEquals(1928.3659, s.getDate().durationFrom(iniDate), 1.0e-3);
                    } else {
                        Assert.assertEquals(1914.1680, s.getDate().durationFrom(iniDate), 1.0e-3);
                    }
                    return Action.CONTINUE;
                }
            });
        Assert.assertEquals(proximityAngle, detector.getProximityAngle(), 1.0e-15);
        Assert.assertSame(sun,    detector.getBeacon());
        Assert.assertSame(acatenango,  detector.getObserver());
        Assert.assertEquals(maxCheck, detector.getMaxCheckInterval(), 1.0e-15);
        propagator.addEventDetector(detector);
        final SpacecraftState finalState = propagator.propagate(iniDate.shiftedBy(7000.0));
        Assert.assertEquals(7000.0, finalState.getDate().durationFrom(iniDate), 1.0e-3);

    }

    @Before
    public void setUp() {
        try {
            Utils.setDataRoot("regular-data");
            earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                         Constants.WGS84_EARTH_FLATTENING,
                                         FramesFactory.getITRF(IERSConventions.IERS_2010, true));
            acatenango = new TopocentricFrame(earth,
                                              new GeodeticPoint(FastMath.toRadians(14.500833),
                                                                FastMath.toRadians(-90.87583),
                                                                3976.0),
                                              "Acatenango");
            iniDate = new AbsoluteDate(2003, 5, 1, 17, 30, 0.0, TimeScalesFactory.getUTC());
            initialOrbit = new KeplerianOrbit(7e6, 1.0e-4, FastMath.toRadians(98.5),
                                              FastMath.toRadians(87.0), FastMath.toRadians(216.59976025619),
                                              FastMath.toRadians(319.7), PositionAngle.MEAN,
                                              FramesFactory.getEME2000(), iniDate,
                                              Constants.EIGEN5C_EARTH_MU);
            propagator = new KeplerianPropagator(initialOrbit);
        } catch (OrekitException oe) {
            Assert.fail(oe.getLocalizedMessage());
        }
    }

    @After
    public void tearDown() {
        earth        = null;
        iniDate      = null;
        initialOrbit = null;
        propagator   = null;
    }

}

