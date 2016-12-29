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
package org.orekit.propagation.analytical.tle;


import org.hipparchus.geometry.euclidean.threed.Line;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.attitudes.BodyCenterPointing;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;


public class TLEPropagatorTest {

    private TLE tle;
    private double period;

    @Test
    public void testSlaveMode() throws OrekitException {

        TLEPropagator propagator = TLEPropagator.selectExtrapolator(tle);
        AbsoluteDate initDate = tle.getDate();
        SpacecraftState initialState = propagator.getInitialState();

        // Simulate a full period of a GPS satellite
        // -----------------------------------------
        SpacecraftState finalState = propagator.propagate(initDate.shiftedBy(period));

        // Check results
        Assert.assertEquals(initialState.getA(), finalState.getA(), 1e-1);
        Assert.assertEquals(initialState.getEquinoctialEx(), finalState.getEquinoctialEx(), 1e-1);
        Assert.assertEquals(initialState.getEquinoctialEy(), finalState.getEquinoctialEy(), 1e-1);
        Assert.assertEquals(initialState.getHx(), finalState.getHx(), 1e-3);
        Assert.assertEquals(initialState.getHy(), finalState.getHy(), 1e-3);
        Assert.assertEquals(initialState.getLM(), finalState.getLM(), 1e-3);

    }

    @Test
    public void testEphemerisMode() throws OrekitException {

        TLEPropagator propagator = TLEPropagator.selectExtrapolator(tle);
        propagator.setEphemerisMode();

        AbsoluteDate initDate = tle.getDate();
        SpacecraftState initialState = propagator.getInitialState();

        // Simulate a full period of a GPS satellite
        // -----------------------------------------
        AbsoluteDate endDate = initDate.shiftedBy(period);
        propagator.propagate(endDate);

        // get the ephemeris
        BoundedPropagator boundedProp = propagator.getGeneratedEphemeris();

        // get the initial state from the ephemeris and check if it is the same as
        // the initial state from the TLE
        SpacecraftState boundedState = boundedProp.propagate(initDate);

        // Check results
        Assert.assertEquals(initialState.getA(), boundedState.getA(), 0.);
        Assert.assertEquals(initialState.getEquinoctialEx(), boundedState.getEquinoctialEx(), 0.);
        Assert.assertEquals(initialState.getEquinoctialEy(), boundedState.getEquinoctialEy(), 0.);
        Assert.assertEquals(initialState.getHx(), boundedState.getHx(), 0.);
        Assert.assertEquals(initialState.getHy(), boundedState.getHy(), 0.);
        Assert.assertEquals(initialState.getLM(), boundedState.getLM(), 1e-14);

        SpacecraftState finalState = boundedProp.propagate(endDate);

        // Check results
        Assert.assertEquals(initialState.getA(), finalState.getA(), 1e-1);
        Assert.assertEquals(initialState.getEquinoctialEx(), finalState.getEquinoctialEx(), 1e-1);
        Assert.assertEquals(initialState.getEquinoctialEy(), finalState.getEquinoctialEy(), 1e-1);
        Assert.assertEquals(initialState.getHx(), finalState.getHx(), 1e-3);
        Assert.assertEquals(initialState.getHy(), finalState.getHy(), 1e-3);
        Assert.assertEquals(initialState.getLM(), finalState.getLM(), 1e-3);

    }

    /** Test if body center belongs to the direction pointed by the satellite
     */
    @Test
    public void testBodyCenterInPointingDirection() throws OrekitException {

        final Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            Constants.WGS84_EARTH_FLATTENING,
                                                            itrf);
        DistanceChecker checker = new DistanceChecker(itrf);

        // with Earth pointing attitude, distance should be small
        TLEPropagator propagator =
                TLEPropagator.selectExtrapolator(tle,
                                                 new BodyCenterPointing(FramesFactory.getTEME(), earth),
                                                 Propagator.DEFAULT_MASS);
        propagator.setMasterMode(900.0, checker);
        propagator.propagate(tle.getDate().shiftedBy(period));
        Assert.assertEquals(0.0, checker.getMaxDistance(), 2.0e-7);

        // with default attitude mode, distance should be large
        propagator = TLEPropagator.selectExtrapolator(tle);
        propagator.setMasterMode(900.0, checker);
        propagator.propagate(tle.getDate().shiftedBy(period));
        Assert.assertEquals(1.5219e7, checker.getMinDistance(), 1000.0);
        Assert.assertEquals(2.6572e7, checker.getMaxDistance(), 1000.0);

    }

    private static class DistanceChecker implements OrekitFixedStepHandler {

        private final Frame itrf;
        private double minDistance;
        private double maxDistance;

        public DistanceChecker(Frame itrf) {
            this.itrf = itrf;
        }

        public double getMinDistance() {
            return minDistance;
        }

        public double getMaxDistance() {
            return maxDistance;
        }

        public void init(SpacecraftState s0, AbsoluteDate t, double step) {
            minDistance = Double.POSITIVE_INFINITY;
            maxDistance = Double.NEGATIVE_INFINITY;
        }

        public void handleStep(SpacecraftState currentState, boolean isLast)
            throws OrekitException {
            // Get satellite attitude rotation, i.e rotation from inertial frame to satellite frame
            Rotation rotSat = currentState.getAttitude().getRotation();

            // Transform Z axis from satellite frame to inertial frame
            Vector3D zSat = rotSat.applyInverseTo(Vector3D.PLUS_K);

            // Transform Z axis from inertial frame to ITRF
            Transform transform = currentState.getFrame().getTransformTo(itrf, currentState.getDate());
            Vector3D zSatITRF = transform.transformVector(zSat);

            // Transform satellite position/velocity from inertial frame to ITRF
            PVCoordinates pvSatITRF = transform.transformPVCoordinates(currentState.getPVCoordinates());

            // Line containing satellite point and following pointing direction
            Line pointingLine = new Line(pvSatITRF.getPosition(),
                                         pvSatITRF.getPosition().add(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                     zSatITRF),
                                         1.0e-10);

            double distance = pointingLine.distance(Vector3D.ZERO);
            minDistance = FastMath.min(minDistance, distance);
            maxDistance = FastMath.max(maxDistance, distance);
        }

    }

    @Before
    public void setUp() throws OrekitException {
        Utils.setDataRoot("regular-data");

        // setup a TLE for a GPS satellite
        String line1 = "1 37753U 11036A   12090.13205652 -.00000006  00000-0  00000+0 0  2272";
        String line2 = "2 37753  55.0032 176.5796 0004733  13.2285 346.8266  2.00565440  5153";

        tle = new TLE(line1, line2);

        // the period of the GPS satellite
        period = 717.97 * 60.0;
    }

}

