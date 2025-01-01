/* Copyright 2002-2025 CS GROUP
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
package org.orekit.propagation.analytical.tle;

import org.hamcrest.MatcherAssert;
import org.hipparchus.geometry.euclidean.threed.Line;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.events.Action;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.OrekitMatchers;
import org.orekit.Utils;
import org.orekit.attitudes.BodyCenterPointing;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.StaticTransform;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.EphemerisGenerator;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.DateDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;


public class TLEPropagatorTest {

    private TLE tle;
    private double period;

    @Test
    public void testsecondaryMode() {

        TLEPropagator propagator = TLEPropagator.selectExtrapolator(tle);
        AbsoluteDate initDate = tle.getDate();
        SpacecraftState initialState = propagator.getInitialState();

        // Simulate a full period of a GPS satellite
        // -----------------------------------------
        SpacecraftState finalState = propagator.propagate(initDate.shiftedBy(period));

        // Check results
        Assertions.assertEquals(initialState.getA(), finalState.getA(), 1e-1);
        Assertions.assertEquals(initialState.getEquinoctialEx(), finalState.getEquinoctialEx(), 1e-1);
        Assertions.assertEquals(initialState.getEquinoctialEy(), finalState.getEquinoctialEy(), 1e-1);
        Assertions.assertEquals(initialState.getHx(), finalState.getHx(), 1e-3);
        Assertions.assertEquals(initialState.getHy(), finalState.getHy(), 1e-3);
        Assertions.assertEquals(initialState.getLM(), finalState.getLM(), 1e-3);

    }

    @Test
    public void testEphemerisMode() {

        TLEPropagator propagator = TLEPropagator.selectExtrapolator(tle);
        final EphemerisGenerator generator = propagator.getEphemerisGenerator();

        AbsoluteDate initDate = tle.getDate();
        SpacecraftState initialState = propagator.getInitialState();

        // Simulate a full period of a GPS satellite
        // -----------------------------------------
        AbsoluteDate endDate = initDate.shiftedBy(period);
        propagator.propagate(endDate);

        // get the ephemeris
        BoundedPropagator boundedProp = generator.getGeneratedEphemeris();

        // get the initial state from the ephemeris and check if it is the same as
        // the initial state from the TLE
        SpacecraftState boundedState = boundedProp.propagate(initDate);

        // Check results
        Assertions.assertEquals(initialState.getA(), boundedState.getA(), 0.);
        Assertions.assertEquals(initialState.getEquinoctialEx(), boundedState.getEquinoctialEx(), 0.);
        Assertions.assertEquals(initialState.getEquinoctialEy(), boundedState.getEquinoctialEy(), 0.);
        Assertions.assertEquals(initialState.getHx(), boundedState.getHx(), 0.);
        Assertions.assertEquals(initialState.getHy(), boundedState.getHy(), 0.);
        Assertions.assertEquals(initialState.getLM(), boundedState.getLM(), 1e-14);

        SpacecraftState finalState = boundedProp.propagate(endDate);

        // Check results
        Assertions.assertEquals(initialState.getA(), finalState.getA(), 1e-1);
        Assertions.assertEquals(initialState.getEquinoctialEx(), finalState.getEquinoctialEx(), 1e-1);
        Assertions.assertEquals(initialState.getEquinoctialEy(), finalState.getEquinoctialEy(), 1e-1);
        Assertions.assertEquals(initialState.getHx(), finalState.getHx(), 1e-3);
        Assertions.assertEquals(initialState.getHy(), finalState.getHy(), 1e-3);
        Assertions.assertEquals(initialState.getLM(), finalState.getLM(), 1e-3);

    }

    /** Test if body center belongs to the direction pointed by the satellite
     */
    @Test
    public void testBodyCenterInPointingDirection() {

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
        propagator.setStepHandler(900.0, checker);
        propagator.propagate(tle.getDate().shiftedBy(period));
        Assertions.assertEquals(0.0, checker.getMaxDistance(), 2.0e-7);

        // with default attitude mode, distance should be large
        propagator = TLEPropagator.selectExtrapolator(tle);
        propagator.setStepHandler(900.0, checker);
        propagator.propagate(tle.getDate().shiftedBy(period));
        MatcherAssert.assertThat(checker.getMinDistance(),
                OrekitMatchers.greaterThan(1.5218e7));
        Assertions.assertEquals(2.6572e7, checker.getMaxDistance(), 1000.0);

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

        public void handleStep(SpacecraftState currentState) {
            // Get satellite attitude rotation, i.e rotation from inertial frame to satellite frame
            Rotation rotSat = currentState.getAttitude().getRotation();

            // Transform Z axis from satellite frame to inertial frame
            Vector3D zSat = rotSat.applyInverseTo(Vector3D.PLUS_K);

            // Transform Z axis from inertial frame to ITRF
            StaticTransform transform = currentState.getFrame().getStaticTransformTo(itrf, currentState.getDate());
            Vector3D zSatITRF = transform.transformVector(zSat);

            // Transform satellite position/velocity from inertial frame to ITRF
            Vector3D posSatITRF = transform.transformPosition(currentState.getPosition());

            // Line containing satellite point and following pointing direction
            Line pointingLine = new Line(posSatITRF,
                                         posSatITRF.add(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                        zSatITRF),
                                         1.0e-10);

            double distance = pointingLine.distance(Vector3D.ZERO);
            minDistance = FastMath.min(minDistance, distance);
            maxDistance = FastMath.max(maxDistance, distance);
        }

    }

    @Test
    void testResetInitialState() {
        // GIVEN
        final TLEPropagator tlePropagator = TLEPropagator.selectExtrapolator(tle);
        final SpacecraftState initialState = tlePropagator.getInitialState();
        final double unexpectedMass = initialState.getMass();
        final double expectedMass = 2. * unexpectedMass;
        final SpacecraftState newState = new SpacecraftState(initialState.getOrbit(), initialState.getAttitude(),
                expectedMass);

        // WHEN
        tlePropagator.resetInitialState(newState);

        // THEN
        final SpacecraftState actualState = tlePropagator.getInitialState();
        Assertions.assertEquals(expectedMass, tlePropagator.getMass(actualState.getDate()));
        Assertions.assertEquals(expectedMass, actualState.getMass());
        Assertions.assertNotEquals(unexpectedMass, actualState.getMass());
    }

    @Test
    void testResetIntermediateStateForward() {
        testResetIntermediateStateTemplate(true);
    }

    @Test
    void testResetIntermediateStateBackward() {
        testResetIntermediateStateTemplate(false);
    }

    void testResetIntermediateStateTemplate(final boolean isForward) {
        // GIVEN
        final TLEPropagator tlePropagator = TLEPropagator.selectExtrapolator(tle);
        final double expectedMass = 2000.;
        final SpacecraftState propagatedState = tlePropagator.propagate(tle.getDate().shiftedBy(1));
        final SpacecraftState modifiedState = new SpacecraftState(propagatedState.getOrbit(), expectedMass);

        // WHEN
        tlePropagator.resetIntermediateState(modifiedState, isForward);

        // THEN
        final double tinyTimeShift = (isForward) ? 1e-3 : -1e-3;
        final double actualMass = tlePropagator.getMass(modifiedState.getDate().shiftedBy(tinyTimeShift));
        Assertions.assertEquals(expectedMass, actualMass);
    }

    @Test
    void testResetIntermediateStateHighLevelForward() {
        testResetIntermediateStateHighLevelTemplate(true);
    }

    @Test
    void testResetIntermediateStateHighLevelBackward() {
        testResetIntermediateStateHighLevelTemplate(false);
    }

    void testResetIntermediateStateHighLevelTemplate(final boolean isForward) {
        // GIVEN
        final TLEPropagator tlePropagator = TLEPropagator.selectExtrapolator(tle);
        final AbsoluteDate epoch = tlePropagator.getInitialState().getDate();
        final double totalShift = 1e4;
        final double shiftSign = isForward ? 1 : -1;
        final AbsoluteDate targetDate = epoch.shiftedBy(totalShift * shiftSign);
        final EventHandler stateResetter = (s, detector, increasing) -> Action.RESET_STATE;
        final DateDetector detector = new DateDetector(targetDate)
                .withThreshold(1e-8).withHandler(stateResetter);
        tlePropagator.addEventDetector(detector);

        // WHEN
        final SpacecraftState actualState = tlePropagator.propagate(targetDate);

        // THEN
        final SpacecraftState expectedState = TLEPropagator.selectExtrapolator(tle).propagate(targetDate);
        final Vector3D expectedPosition = expectedState.getPosition();
        final Vector3D actualPosition = actualState.getPosition();
        final double tolerance = 1e-1;
        Assertions.assertEquals(expectedPosition.getX(), actualPosition.getX(), tolerance);
        Assertions.assertEquals(expectedPosition.getY(), actualPosition.getY(), tolerance);
        Assertions.assertEquals(expectedPosition.getZ(), actualPosition.getZ(), tolerance);
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");

        // setup a TLE for a GPS satellite
        String line1 = "1 37753U 11036A   12090.13205652 -.00000006  00000-0  00000+0 0  2272";
        String line2 = "2 37753  55.0032 176.5796 0004733  13.2285 346.8266  2.00565440  5153";

        tle = new TLE(line1, line2);

        // the period of the GPS satellite
        period = 717.97 * 60.0;
    }

}

