/* Copyright 2002-2024 CS GROUP
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;

import static org.orekit.propagation.conversion.AbstractPropagatorBuilderTest.assertPropagatorBuilderIsACopy;

public class EcksteinHechlerPropagatorBuilderTest {

    @BeforeEach
    public void initialize() {
        Utils.setDataRoot("potential");
    }

    @Test
    @SuppressWarnings("deprecation")
    void testCopyMethod() {

        // Given
        final Orbit orbit = new CartesianOrbit(new PVCoordinates(
                new Vector3D(Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS + 400000, 0, 0),
                new Vector3D(10, 7668.6, 3)), FramesFactory.getGCRF(),
                                               new AbsoluteDate(), Constants.EIGEN5C_EARTH_MU);

        final UnnormalizedSphericalHarmonicsProvider harmonicsProvider = GravityFieldFactory.getUnnormalizedProvider(6, 0);

        final EcksteinHechlerPropagatorBuilder builder = new EcksteinHechlerPropagatorBuilder(orbit, harmonicsProvider,
                PositionAngleType.MEAN, 10.0);

        // When
        final EcksteinHechlerPropagatorBuilder copyBuilder = builder.copy();

        // Then
        assertPropagatorBuilderIsACopy(builder, copyBuilder);

    }

    @Test
    void testClone() {

        // Given
        final Orbit orbit = new CartesianOrbit(new PVCoordinates(
                new Vector3D(Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS + 400000, 0, 0),
                new Vector3D(10, 7668.6, 3)), FramesFactory.getGCRF(),
                new AbsoluteDate(), Constants.EIGEN5C_EARTH_MU);

        final UnnormalizedSphericalHarmonicsProvider harmonicsProvider = GravityFieldFactory.getUnnormalizedProvider(6, 0);

        final EcksteinHechlerPropagatorBuilder builder = new EcksteinHechlerPropagatorBuilder(orbit, harmonicsProvider,
                PositionAngleType.MEAN, 10.0);

        // When
        final EcksteinHechlerPropagatorBuilder copyBuilder = (EcksteinHechlerPropagatorBuilder) builder.clone();

        // Then
        assertPropagatorBuilderIsACopy(builder, copyBuilder);
        Assertions.assertEquals(builder.getImpulseManeuvers().size(), copyBuilder.getImpulseManeuvers().size());

    }
}
