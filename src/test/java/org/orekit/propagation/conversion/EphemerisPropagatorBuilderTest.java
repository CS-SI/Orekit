/* Contributed in the public domain.
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

import static org.orekit.propagation.conversion.AbstractPropagatorBuilderTest.assertPropagatorBuilderIsACopy;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.orekit.TestUtils;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.SpacecraftStateInterpolator;
import org.orekit.propagation.analytical.Ephemeris;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeInterpolator;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link EphemerisPropagatorBuilder}.
 *
 * @author Vincent Cucchietti
 */
public class EphemerisPropagatorBuilderTest {

    @Test
    @DisplayName("Test issue 1316 : Regression in EphemerisPropagatorBuilder API")
    void testIssue1316() {
        // GIVEN
        final int    interpolationPoints     = 3;
        final double extrapolationThresholds = 0.007;

        // Create mock attitude provider
        final AttitudeProvider mockAttitudeProvider = mock(AttitudeProvider.class);

        // Get default orbit
        final Orbit defaultOrbit = TestUtils.getDefaultOrbit(new AbsoluteDate());

        // Create fake list of states
        final SpacecraftState mockState1 = mock(SpacecraftState.class);
        when(mockState1.getFrame()).thenReturn(defaultOrbit.getFrame());
        when(mockState1.getOrbit()).thenReturn(defaultOrbit);
        final SpacecraftState mockState2 = mock(SpacecraftState.class);
        final SpacecraftState mockState3 = mock(SpacecraftState.class);

        final List<SpacecraftState> fakeStates = Arrays.asList(mockState1, mockState2, mockState3);

        // WHEN
        final EphemerisPropagatorBuilder builder = new EphemerisPropagatorBuilder(fakeStates,
                                                                                  interpolationPoints,
                                                                                  extrapolationThresholds,
                                                                                  mockAttitudeProvider);

        // THEN
        // Assert initial orbit
        assertEquals(defaultOrbit.getFrame(), builder.getFrame());
        assertEquals(defaultOrbit.getType(),  builder.getOrbitType());
        assertEquals(defaultOrbit.getDate(),  builder.getInitialOrbitDate());
        assertEquals(defaultOrbit.getMu(),  builder.getMu());

        // Assert attitude provider
        assertEquals(mockAttitudeProvider,  builder.getAttitudeProvider());
    }

    @Test
    @DisplayName("Test buildPropagator method")
    void should_create_expected_propagator() {

        // Given
        final Orbit orbit = new CartesianOrbit(new PVCoordinates(
                new Vector3D(Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS + 400000, 0, 0),
                new Vector3D(0, 7668.6, 0)), FramesFactory.getGCRF(), new AbsoluteDate(),
                                               Constants.EIGEN5C_EARTH_MU);
        final List<SpacecraftState> states = new ArrayList<>();
        states.add(new SpacecraftState(orbit));
        states.add(new SpacecraftState(orbit.shiftedBy(1)));

        final Frame                             frame             = FramesFactory.getGCRF();
        final TimeInterpolator<SpacecraftState> stateInterpolator = new SpacecraftStateInterpolator(frame);

        final EphemerisPropagatorBuilder builder =
                new EphemerisPropagatorBuilder(states, stateInterpolator);

        // When
        final Ephemeris builtPropagator = (Ephemeris) builder.buildPropagator();

        // Then
        final Ephemeris expectedPropagator = new Ephemeris(states, stateInterpolator);

        assertEquals(expectedPropagator.getFrame(), builtPropagator.getFrame());
        assertEquals(expectedPropagator.getMinDate(), builtPropagator.getMinDate());
        assertEquals(expectedPropagator.getMaxDate(), builtPropagator.getMaxDate());

        Assertions.assertArrayEquals(expectedPropagator.getManagedAdditionalStates(),
                                     builtPropagator.getManagedAdditionalStates());
        // Initial state has also been verified to be equal between both ephemeris (except for the Attitude which is expected
        // to have different memory address)
    }

    @Test
    @DisplayName("Test copy method")
    void testCopyMethod() {

        // Given
        final Orbit orbit = new CartesianOrbit(new PVCoordinates(
                new Vector3D(Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS + 400000, 0, 0),
                new Vector3D(0, 7668.6, 0)), FramesFactory.getGCRF(),
                                               new AbsoluteDate(), Constants.EIGEN5C_EARTH_MU);
        final List<SpacecraftState> states = new ArrayList<>();
        states.add(new SpacecraftState(orbit));
        states.add(new SpacecraftState(orbit));

        final Frame                             frame             = FramesFactory.getGCRF();
        final TimeInterpolator<SpacecraftState> stateInterpolator = new SpacecraftStateInterpolator(frame);
        final AttitudeProvider                  attitudeProvider  = mock(AttitudeProvider.class);

        final EphemerisPropagatorBuilder builder =
                new EphemerisPropagatorBuilder(states, stateInterpolator, attitudeProvider);

        // When
        final EphemerisPropagatorBuilder copyBuilder = builder.copy();

        // Then
        assertPropagatorBuilderIsACopy(builder, copyBuilder);
    }
}
