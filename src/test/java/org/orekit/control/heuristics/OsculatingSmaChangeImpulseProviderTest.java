/* Copyright 2022-2026 Romain Serra
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
package org.orekit.control.heuristics;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.forces.maneuvers.Control3DVectorCostType;
import org.orekit.forces.maneuvers.ImpulseManeuver;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.events.SingleDateDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class OsculatingSmaChangeImpulseProviderTest {

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testGetImpulseAlready(final boolean isForward) {
        // GIVEN
        final double semiMajorAxis = 1e7;
        final OsculatingSmaChangeImpulseProvider impulseProvider = new OsculatingSmaChangeImpulseProvider(semiMajorAxis);
        final EquinoctialOrbit orbit = new EquinoctialOrbit(semiMajorAxis, 0., 0., 0., 0., 0., PositionAngleType.TRUE,
                FramesFactory.getGCRF(), AbsoluteDate.ARBITRARY_EPOCH, Constants.EGM96_EARTH_MU);
        // WHEN
        final Vector3D impulse = impulseProvider.getImpulse(new SpacecraftState(orbit), isForward);
        // THEN
        assertEquals(Vector3D.ZERO, impulse);
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1e6, 1e6, 2e6, 1e7})
    void testGetImpulseDifferent(final double deltaSma) {
        // GIVEN
        final double semimajorAxis = 1.0e7;
        final OsculatingSmaChangeImpulseProvider impulseProvider = new OsculatingSmaChangeImpulseProvider(semimajorAxis);
        final EquinoctialOrbit orbit = new EquinoctialOrbit(semimajorAxis + deltaSma, 0., 0., 0., 0., 0., PositionAngleType.TRUE,
                FramesFactory.getGCRF(), AbsoluteDate.ARBITRARY_EPOCH, Constants.EGM96_EARTH_MU);
        // WHEN
        final Vector3D impulse = impulseProvider.getImpulse(new SpacecraftState(orbit), true);
        // THEN
        final Vector3D velocityDirection = orbit.getVelocity().normalize();
        final Vector3D expectedDirection = deltaSma > 0. ? velocityDirection.negate() : velocityDirection;
        assertArrayEquals(expectedDirection.toArray(), impulse.normalize().toArray(), 1e-10);
    }

    @Test
    void testGetImpulseDifferentConstrained() {
        // GIVEN
        final double semimajorAxis = 1.0e7;
        final KeplerianOrbit orbit = new KeplerianOrbit(semimajorAxis, 0., 0., 0., 0., FastMath.PI, PositionAngleType.MEAN,
                FramesFactory.getGCRF(), AbsoluteDate.ARBITRARY_EPOCH, Constants.EGM96_EARTH_MU);
        final double maximumMagnitude = 10.;
        final OsculatingSmaChangeImpulseProvider impulseProvider = new OsculatingSmaChangeImpulseProvider(maximumMagnitude, semimajorAxis + 1e6);
        // WHEN
        final Vector3D impulse = impulseProvider.getImpulse(new SpacecraftState(orbit), true);
        // THEN
        assertEquals(maximumMagnitude, impulse.getNorm2());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testIntegration(final boolean isForward) {
        // GIVEN
        final double targetSemiMajorAxis = 1e7;
        final KeplerianOrbit orbit = new KeplerianOrbit(targetSemiMajorAxis + 1e6, 0.8, 1., 0., 0., FastMath.PI, PositionAngleType.MEAN,
                FramesFactory.getGCRF(), AbsoluteDate.ARBITRARY_EPOCH, Constants.EGM96_EARTH_MU);
        final KeplerianPropagator propagator = new KeplerianPropagator(orbit);
        final AbsoluteDate maneuverDate = isForward ? orbit.getDate().shiftedBy(10) : orbit.getDate().shiftedBy(-10.);
        final SingleDateDetector detector = new SingleDateDetector(maneuverDate);
        final ImpulseManeuver maneuver = new ImpulseManeuver(detector, new FrameAlignedProvider(orbit.getFrame()),
                new OsculatingSmaChangeImpulseProvider(targetSemiMajorAxis), Double.POSITIVE_INFINITY, Control3DVectorCostType.NONE);
        propagator.addEventDetector(maneuver);
        // WHEN
        final AbsoluteDate targetDate = isForward ? maneuverDate.shiftedBy(1) : maneuverDate.shiftedBy(-1);
        final SpacecraftState state = propagator.propagate(targetDate);
        // THEN
        assertEquals(targetSemiMajorAxis, state.getOrbit().getA(), 1e-5);
    }
}
