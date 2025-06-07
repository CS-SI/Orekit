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

package org.orekit.propagation.conversion;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.forces.maneuvers.ImpulseManeuver;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.Propagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

import static org.orekit.propagation.conversion.AbstractPropagatorBuilderTest.assertPropagatorBuilderIsACopy;

public class KeplerianPropagatorBuilderTest {

    @Test
    void testClone() {

        // Given
        final Orbit orbit = getOrbit();
        final KeplerianPropagatorBuilder builder = new KeplerianPropagatorBuilder(orbit, PositionAngleType.MEAN, 1.0);

        // When
        final KeplerianPropagatorBuilder copyBuilder = builder.clone();

        // Then
        assertPropagatorBuilderIsACopy(builder, copyBuilder);
        Assertions.assertEquals(builder.getImpulseManeuvers().size(), copyBuilder.getImpulseManeuvers().size());

    }

    @Test
    void testClearImpulseManeuvers() {
        // Given
        final Orbit orbit = getOrbit();
        final KeplerianPropagatorBuilder builder = new KeplerianPropagatorBuilder(orbit, PositionAngleType.MEAN, 1.0);
        final ImpulseManeuver mockedManeuver = Mockito.mock(ImpulseManeuver.class);
        builder.addImpulseManeuver(mockedManeuver);

        // When
        builder.clearImpulseManeuvers();

        // Then
        final Propagator propagator = builder.buildPropagator();
        Assertions.assertTrue(propagator.getEventDetectors().isEmpty());
    }

    @Test
    void testAddImpulseManeuver() {
        // Given
        final Orbit orbit = getOrbit();
        final KeplerianPropagatorBuilder builder = new KeplerianPropagatorBuilder(orbit, PositionAngleType.MEAN, 1.0);
        final ImpulseManeuver mockedManeuver = Mockito.mock(ImpulseManeuver.class);

        // When
        builder.addImpulseManeuver(mockedManeuver);

        // Then
        final Propagator propagator = builder.buildPropagator();
        Assertions.assertEquals(1, propagator.getEventDetectors().size());
        Assertions.assertEquals(mockedManeuver, propagator.getEventDetectors().toArray()[0]);
    }

    /** Test for issue #1741.
     * <p>This test checks that orbital drivers in cloned propagator builders
     * ain't at the same physical address, i.e. that they're not linked anymore.</p>
     */
    @Test
    void testIssue1741() {

        // Given
        final Orbit orbit = getOrbit();
        final KeplerianPropagatorBuilder builder = new KeplerianPropagatorBuilder(orbit, PositionAngleType.MEAN, 1.0);

        // When
        final KeplerianPropagatorBuilder copyBuilder = builder.clone();


        // Change orbit of the copied builder
        final TimeStampedPVCoordinates modifiedPv = orbit.shiftedBy(3600.).getPVCoordinates();
        copyBuilder.resetOrbit(new CartesianOrbit(modifiedPv, orbit.getFrame(), orbit.getDate(), orbit.getMu()));

        // Then
        // Original builder should still have original orbit
        final PVCoordinates originalPv = orbit.getPVCoordinates();
        final PVCoordinates initialPv = builder.createInitialOrbit().getPVCoordinates();
        final double dP = originalPv.getPosition().distance(initialPv.getPosition());
        final double dV = originalPv.getVelocity().distance(initialPv.getVelocity());
        final double dA = originalPv.getAcceleration().distance(initialPv.getAcceleration());
        Assertions.assertEquals(0., dP, 0.);
        Assertions.assertEquals(0., dV, 0.);
        Assertions.assertEquals(0., dA, 0.);
    }

    private static Orbit getOrbit() {
        return new CartesianOrbit(new PVCoordinates(
                new Vector3D(Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS + 400000, 0, 0),
                new Vector3D(0, 7668.6, 0)), FramesFactory.getGCRF(),
                                  new AbsoluteDate(), Constants.EIGEN5C_EARTH_MU);
    }
}
