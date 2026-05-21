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

import static org.junit.jupiter.api.Assertions.*;

class CircularizingImpulseProviderTest {

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testGetImpulseCircular(final boolean isForward) {
        // GIVEN
        final CircularizingImpulseProvider impulseProvider = new CircularizingImpulseProvider();
        final EquinoctialOrbit orbit = new EquinoctialOrbit(1e7, 0., 0., 0., 0., 0., PositionAngleType.TRUE,
                FramesFactory.getGCRF(), AbsoluteDate.ARBITRARY_EPOCH, Constants.EGM96_EARTH_MU);
        // WHEN
        final Vector3D impulse = impulseProvider.getImpulse(new SpacecraftState(orbit), isForward);
        // THEN
        assertEquals(Vector3D.ZERO, impulse);
    }

    @Test
    void testGetImpulseNonCircular() {
        // GIVEN
        final CircularizingImpulseProvider impulseProvider = new CircularizingImpulseProvider();
        final double semimajorAxis = 1.0e7;
        final double eccentricity = 0.1;
        final KeplerianOrbit orbit = new KeplerianOrbit(semimajorAxis, eccentricity, 0., 0., 0., 0., PositionAngleType.TRUE,
                FramesFactory.getGCRF(), AbsoluteDate.ARBITRARY_EPOCH, Constants.EGM96_EARTH_MU);
        // WHEN
        final Vector3D impulse = impulseProvider.getImpulse(new SpacecraftState(orbit), true);
        // THEN
        final Vector3D velocity = orbit.getVelocity();
        final double periapsis = semimajorAxis * (1 - eccentricity);
        final double circularSpeed = FastMath.sqrt(orbit.getMu() / periapsis);
        final Vector3D expected = velocity.negate().add(velocity.normalize().scalarMultiply(circularSpeed));
        assertEquals(expected, impulse);
    }

    @Test
    void testGetImpulseNonCircularConstrained() {
        // GIVEN
        final double semimajorAxis = 1.0e7;
        final double eccentricity = 0.1;
        final KeplerianOrbit orbit = new KeplerianOrbit(semimajorAxis, eccentricity, 0., 0., 0., FastMath.PI, PositionAngleType.MEAN,
                FramesFactory.getGCRF(), AbsoluteDate.ARBITRARY_EPOCH, Constants.EGM96_EARTH_MU);
        final double maximumMagnitude = 10.;
        final CircularizingImpulseProvider impulseProvider = new CircularizingImpulseProvider(maximumMagnitude);
        // WHEN
        final Vector3D impulse = impulseProvider.getImpulse(new SpacecraftState(orbit), true);
        // THEN
        final Vector3D velocity = orbit.getVelocity();
        final double apoapsis = semimajorAxis * (1 + eccentricity);
        final double circularSpeed = FastMath.sqrt(orbit.getMu() / apoapsis);
        final Vector3D expectedUnconstrained = velocity.negate().add(velocity.normalize().scalarMultiply(circularSpeed));
        assertArrayEquals(expectedUnconstrained.normalize().toArray(), impulse.normalize().toArray(), 1e-7);
        assertEquals(maximumMagnitude, impulse.getNorm2());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testIntegration(final boolean isForward) {
        // GIVEN
        final KeplerianOrbit orbit = new KeplerianOrbit(1e7, 0.8, 1., 0., 0., FastMath.PI, PositionAngleType.MEAN,
                FramesFactory.getGCRF(), AbsoluteDate.ARBITRARY_EPOCH, Constants.EGM96_EARTH_MU);
        final KeplerianPropagator propagator = new KeplerianPropagator(orbit);
        final AbsoluteDate maneuverDate = isForward ? orbit.getDate().shiftedBy(10) : orbit.getDate().shiftedBy(-10.);
        final SingleDateDetector detector = new SingleDateDetector(maneuverDate);
        final ImpulseManeuver maneuver = new ImpulseManeuver(detector, new FrameAlignedProvider(orbit.getFrame()),
                new CircularizingImpulseProvider(), Double.POSITIVE_INFINITY, Control3DVectorCostType.NONE);
        propagator.addEventDetector(maneuver);
        // WHEN
        final AbsoluteDate targetDate = isForward ? maneuverDate.shiftedBy(1) : maneuverDate.shiftedBy(-1);
        final SpacecraftState state = propagator.propagate(targetDate);
        // THEN
        assertEquals(0., state.getOrbit().getE(), 1e-15);
    }
}
