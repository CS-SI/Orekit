/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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

import org.hipparchus.geometry.euclidean.threed.Line;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.events.Action;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

public class InterSatDirectViewDetectorTest {

    @Test
    public void testFormationFlying() {
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            Constants.WGS84_EARTH_FLATTENING,
                                                            FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        final TimeScale utc = TimeScalesFactory.getUTC();
        final CircularOrbit o1 = new CircularOrbit(7200000.0, 1.0e-3, 2.0e-4,
                                                   FastMath.toRadians(98.7), FastMath.toRadians(134.0),
                                                   FastMath.toRadians(21.0), PositionAngle.MEAN, FramesFactory.getGCRF(),
                                                   new AbsoluteDate("2003-02-14T01:02:03.000", utc),
                                                   Constants.EIGEN5C_EARTH_MU);
        final CircularOrbit o2 = new CircularOrbit(o1.getA(), 2.0e-4, 1.0e-3,
                                                   o1.getI() + 1.0e-6, o1.getRightAscensionOfAscendingNode() - 3.5e-7,
                                                   o1.getAlphaM() + 2.2e-6, PositionAngle.MEAN, o1.getFrame(),
                                                   o1.getDate(),
                                                   Constants.EIGEN5C_EARTH_MU);
        Assert.assertEquals(o1.getKeplerianPeriod(), o2.getKeplerianPeriod(), 1.0e-10);
        final Propagator p = new KeplerianPropagator(o1);
        final EventsLogger logger = new EventsLogger();
        p.addEventDetector(logger.monitorDetector(new InterSatDirectViewDetector(earth, o2).
                                                  withMaxCheck(60.0)));
        p.setMasterMode(10.0, (state, isLast) -> {
            Vector3D pos1 = state.getPVCoordinates().getPosition();
            Vector3D pos2 = o2.getPVCoordinates(state.getDate(), state.getFrame()).getPosition();
            Assert.assertTrue(Vector3D.distance(pos1, pos2) >  8100.0);
            Assert.assertTrue(Vector3D.distance(pos1, pos2) < 16400.0);
        });
        p.propagate(o1.getDate().shiftedBy(o1.getKeplerianPeriod()));
        Assert.assertEquals(0, logger.getLoggedEvents().size());
    }

    @Test
    public void testLeoMeo() {
        OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                      Constants.WGS84_EARTH_FLATTENING,
                                                      FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        TimeScale utc = TimeScalesFactory.getUTC();
        CircularOrbit o1 = new CircularOrbit(7200000.0, 1.0e-3, 2.0e-4,
                                             FastMath.toRadians(50.0), FastMath.toRadians(134.0),
                                             FastMath.toRadians(21.0), PositionAngle.MEAN, FramesFactory.getGCRF(),
                                             new AbsoluteDate("2003-02-14T01:02:03.000", utc),
                                             Constants.EIGEN5C_EARTH_MU);
        final CircularOrbit o2 = new CircularOrbit(29600000.0, 2.0e-4, 1.0e-3,
                                                   FastMath.toRadians(56.0), FastMath.toRadians(111.0),
                                                   o1.getAlphaM() + 2.2e-6, PositionAngle.MEAN, o1.getFrame(),
                                                   o1.getDate(),
                                                   Constants.EIGEN5C_EARTH_MU);

        // LEO as master, MEO as slave
        Propagator pA = new KeplerianPropagator(o1);
        EventsLogger loggerA = new EventsLogger();
        pA.addEventDetector(loggerA.monitorDetector(new InterSatDirectViewDetector(earth, o2).
                                                  withMaxCheck(10.0).
                                                  withHandler(new GrazingHandler())));
        pA.propagate(o1.getDate().shiftedBy(4 * o1.getKeplerianPeriod()));
        Assert.assertEquals(7, loggerA.getLoggedEvents().size());

        // LEO as slave, MEO as master
        Propagator pB = new KeplerianPropagator(o2);
        EventsLogger loggerB = new EventsLogger();
        pB.addEventDetector(loggerB.monitorDetector(new InterSatDirectViewDetector(earth, o1).
                                                  withMaxCheck(10.0).
                                                  withHandler(new GrazingHandler())));
        pB.propagate(o1.getDate().shiftedBy(4 * o1.getKeplerianPeriod()));
        Assert.assertEquals(7, loggerB.getLoggedEvents().size());

    }

    private static class GrazingHandler implements EventHandler<InterSatDirectViewDetector> {
        public Action eventOccurred(SpacecraftState s, InterSatDirectViewDetector detector, boolean increasing) {
            // just before increasing events and just after decreasing events,
            // the master/slave line intersects Earth limb
            final OneAxisEllipsoid earth       = detector.getCentralBody();
            final Frame            frame       = earth.getBodyFrame();
            final double           dt          = increasing ? -1.0e-8 : +1.0e-8;
            final AbsoluteDate     grazingDate = s.getDate().shiftedBy(dt);
            final Vector3D pMaster = s.shiftedBy(dt).
                                     getPVCoordinates(frame).
                                     getPosition();
            final Vector3D pSlave  = detector.getSlave().getPVCoordinates(grazingDate, frame).
                                     getPosition();
            final Vector3D grazing = earth.getCartesianIntersectionPoint(new Line(pMaster,  pSlave, 1.0),
                                                                         pMaster, frame, grazingDate);
            final TopocentricFrame topo = new TopocentricFrame(earth, earth.transform(grazing, frame, grazingDate),
                                                               "grazing");
            Assert.assertEquals(  0.0, FastMath.toDegrees(topo.getElevation(pMaster, frame, grazingDate)), 2.0e-4);
            Assert.assertEquals(  0.0, FastMath.toDegrees(topo.getElevation(pSlave,  frame, grazingDate)), 2.0e-4);
            Assert.assertEquals(180.0,
                                FastMath.abs(FastMath.toDegrees(topo.getAzimuth(pSlave,  frame, grazingDate) -
                                                                topo.getAzimuth(pMaster, frame, grazingDate))),
                                6.0e-14);
            return Action.CONTINUE;
        }
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

}

