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

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
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
import java.util.List;

import static org.orekit.propagation.conversion.AbstractPropagatorBuilderTest.assertPropagatorBuilderIsACopy;

/**
 * Unit tests for {@link EphemerisPropagatorBuilder}.
 *
 * @author Vincent Cucchietti
 */
public class EphemerisPropagatorBuilderTest {
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
        final Ephemeris builtPropagator = (Ephemeris) builder.buildPropagator(builder.getSelectedNormalizedParameters());

        // Then
        final Ephemeris expectedPropagator = new Ephemeris(states, stateInterpolator);

        Assertions.assertEquals(expectedPropagator.getFrame(), builtPropagator.getFrame());
        Assertions.assertEquals(expectedPropagator.getMinDate(), builtPropagator.getMinDate());
        Assertions.assertEquals(expectedPropagator.getMaxDate(), builtPropagator.getMaxDate());

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
        final AttitudeProvider                  attitudeProvider  = Mockito.mock(AttitudeProvider.class);

        final EphemerisPropagatorBuilder builder =
                new EphemerisPropagatorBuilder(states, stateInterpolator, attitudeProvider);

        // When
        final EphemerisPropagatorBuilder copyBuilder = builder.copy();

        // Then
        assertPropagatorBuilderIsACopy(builder, copyBuilder);
    }
}
