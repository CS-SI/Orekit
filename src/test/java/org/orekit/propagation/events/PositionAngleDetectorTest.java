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
import org.hipparchus.util.MathUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.propagation.events.EventsLogger.LoggedEvent;
import org.orekit.propagation.events.handlers.ContinueOnEvent;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;

public class PositionAngleDetectorTest {

    @Test
    public void testCartesian() {
        try {
            new PositionAngleDetector(OrbitType.CARTESIAN, PositionAngle.TRUE, 0.0).
            withMaxCheck(600.0).
            withThreshold(1.0e-6);
            Assert.fail("an exception should habe been thrown");
        } catch (OrekitIllegalArgumentException oiae) {
            Assert.assertEquals(OrekitMessages.ORBIT_TYPE_NOT_ALLOWED, oiae.getSpecifier());
            Assert.assertEquals(OrbitType.CARTESIAN, oiae.getParts()[0]);
        }
    }

    @Test
    public void testTrueAnomaly() throws OrekitException {
        doTest(OrbitType.KEPLERIAN, PositionAngle.TRUE, FastMath.toRadians(10.0), 15);
    }

    @Test
    public void testMeanAnomaly() throws OrekitException {
        doTest(OrbitType.KEPLERIAN, PositionAngle.MEAN, FastMath.toRadians(10.0), 15);
    }

    @Test
    public void testEccentricAnomaly() throws OrekitException {
        doTest(OrbitType.KEPLERIAN, PositionAngle.ECCENTRIC, FastMath.toRadians(10.0), 15);
    }

    @Test
    public void testTrueLatitudeArgument() throws OrekitException {
        doTest(OrbitType.CIRCULAR, PositionAngle.TRUE, FastMath.toRadians(730.0), 15);
    }

    @Test
    public void testMeanLatitudeArgument() throws OrekitException {
        doTest(OrbitType.CIRCULAR, PositionAngle.MEAN, FastMath.toRadians(730.0), 15);
    }

    @Test
    public void testEccentricLatitudeArgument() throws OrekitException {
        doTest(OrbitType.CIRCULAR, PositionAngle.ECCENTRIC, FastMath.toRadians(730.0), 15);
    }

    @Test
    public void testTrueLongitudeArgument() throws OrekitException {
        doTest(OrbitType.EQUINOCTIAL, PositionAngle.TRUE, FastMath.toRadians(-45.0), 15);
    }

    @Test
    public void testMeanLongitudeArgument() throws OrekitException {
        doTest(OrbitType.EQUINOCTIAL, PositionAngle.MEAN, FastMath.toRadians(-45.0), 15);
    }

    @Test
    public void testEccentricLongitudeArgument() throws OrekitException {
        doTest(OrbitType.EQUINOCTIAL, PositionAngle.ECCENTRIC, FastMath.toRadians(-45.0), 15);
    }

    private void doTest(final OrbitType orbitType, final PositionAngle positionAngle,
                        final double angle, final int expectedCrossings)
        throws OrekitException {

        PositionAngleDetector d =
                new PositionAngleDetector(orbitType, positionAngle, angle).
                withMaxCheck(60).
                withThreshold(1.e-10).
                withHandler(new ContinueOnEvent<PositionAngleDetector>());

        Assert.assertEquals(60.0, d.getMaxCheckInterval(), 1.0e-15);
        Assert.assertEquals(1.0e-10, d.getThreshold(), 1.0e-15);
        Assert.assertEquals(orbitType, d.getOrbitType());
        Assert.assertEquals(positionAngle, d.getPositionAngle());
        Assert.assertEquals(angle, d.getAngle(), 1.0e-14);
        Assert.assertEquals(AbstractDetector.DEFAULT_MAX_ITER, d.getMaxIterationCount());

        final TimeScale utc = TimeScalesFactory.getUTC();
        final Vector3D position = new Vector3D(-6142438.668, 3492467.56, -25767.257);
        final Vector3D velocity = new Vector3D(506.0, 943.0, 7450);
        final AbsoluteDate date = new AbsoluteDate(2003, 9, 16, utc);
        final Orbit orbit = new CartesianOrbit(new PVCoordinates(position,  velocity),
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

        double[] array = new double[6];
        for (LoggedEvent e : logger.getLoggedEvents()) {
            SpacecraftState state = e.getState();
            orbitType.mapOrbitToArray(state.getOrbit(), positionAngle, array);
            Assert.assertEquals(angle, MathUtils.normalizeAngle(array[5], angle), 1.0e-10);
        }
        Assert.assertEquals(15, logger.getLoggedEvents().size());

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}

