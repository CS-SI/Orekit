/* Copyright 2022-2025 Romain Serra
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
package org.orekit.propagation.numerical;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.orekit.TestUtils;
import org.orekit.forces.ForceModel;
import org.orekit.forces.gravity.NewtonianAttraction;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NumericalTimeDerivativesEquationsTest {

    @ParameterizedTest
    @EnumSource(value = OrbitType.class, names = {"EQUINOCTIAL", "KEPLERIAN", "CIRCULAR"})
    void testComputeTimeDerivatives(final OrbitType orbitType) {
        // GIVEN
        final Orbit orbit = TestUtils.getDefaultOrbit(AbsoluteDate.ARBITRARY_EPOCH);
        final SpacecraftState state = new SpacecraftState(orbit);
        final List<ForceModel> forceModelList = new ArrayList<>();
        final ForceModel forceModel = new NewtonianAttraction(Constants.EGM96_EARTH_MU);
        forceModelList.add(forceModel);
        final NumericalTimeDerivativesEquations equations = new NumericalTimeDerivativesEquations(orbitType,
                PositionAngleType.MEAN, forceModelList);
        // WHEN
        final double[] derivatives = equations.computeTimeDerivatives(state);
        // THEN
        assertEquals(7, derivatives.length);
        for (int i = 0; i < 7; i++) {
            if (i == 5) {
                assertNotEquals(0., derivatives[i]);
            } else {
                assertEquals(0., derivatives[i]);
            }
        }
    }

    @Test
    void testComputeTimeDerivativesCartesian() {
        // GIVEN
        final Orbit orbit = TestUtils.getDefaultOrbit(AbsoluteDate.ARBITRARY_EPOCH);
        final SpacecraftState state = new SpacecraftState(orbit);
        final List<ForceModel> forceModelList = new ArrayList<>();
        final NewtonianAttraction forceModel = new NewtonianAttraction(Constants.EGM96_EARTH_MU);
        forceModelList.add(forceModel);
        final NumericalTimeDerivativesEquations equations = new NumericalTimeDerivativesEquations(OrbitType.CARTESIAN,
                PositionAngleType.MEAN, forceModelList);
        // WHEN
        final double[] derivatives = equations.computeTimeDerivatives(state);
        // THEN
        assertEquals(state.getVelocity().getX(), derivatives[0]);
        assertEquals(state.getVelocity().getY(), derivatives[1]);
        assertEquals(state.getVelocity().getZ(), derivatives[2]);
        final Vector3D acceleration = forceModel.acceleration(state, new double[] {forceModel.getMu(orbit.getDate())});
        final double tolerance = 1e-10;
        assertEquals(acceleration.getX(), derivatives[3], tolerance);
        assertEquals(acceleration.getY(), derivatives[4], tolerance);
        assertEquals(acceleration.getZ(), derivatives[5], tolerance);
        assertEquals(0, derivatives[6]);
    }
}
