/* Copyright 2020 Exotrail
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Exotrail licenses this file to You under the Apache License, Version 2.0
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
package org.orekit.forces.maneuvers.propulsion;

import java.util.Collections;
import java.util.List;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.forces.maneuvers.Control3DVectorCostType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.ParameterDriver;

class ThrustPropulsionModelTest {
    @Test
    @DisplayName("Test getDirection method / issue 1046 with zero thrust")
    void testIssue1046ZeroThrust() {
        // Given
        final SpacecraftState stateMock = Mockito.mock(SpacecraftState.class);

        final Vector3D thrustVectorToReturn = Vector3D.ZERO;

        final ThrustPropulsionModel model = new ThrustPropulsionModel() {
            @Override
            public Vector3D getThrustVector(final SpacecraftState s) {
                return thrustVectorToReturn;
            }

            @Override
            public double getFlowRate(final SpacecraftState s) {
                return 0;
            }

            @Override
            public Vector3D getThrustVector(final SpacecraftState s, final double[] parameters) {
                return null;
            }

            @Override
            public double getFlowRate(final SpacecraftState s, final double[] parameters) {
                return 0;
            }

            @Override
            public <T extends CalculusFieldElement<T>> FieldVector3D<T> getThrustVector(final FieldSpacecraftState<T> s,
                                                                                        final T[] parameters) {
                return null;
            }

            @Override
            public <T extends CalculusFieldElement<T>> T getFlowRate(final FieldSpacecraftState<T> s, final T[] parameters) {
                return null;
            }

            @Override
            public List<ParameterDriver> getParametersDrivers() {
                return Collections.emptyList();
            }

            @Override
            public Control3DVectorCostType getControl3DVectorCostType() {
                return Control3DVectorCostType.NONE;
            }
        };

        // When
        final Vector3D returnedDirection = model.getDirection(stateMock);

        // Then
        final Vector3D expectedDirection = Vector3D.ZERO;

        // Assert that returned direction is a zero vector
        Assertions.assertEquals(expectedDirection, returnedDirection);
    }

    @Test
    @DisplayName("Test getDirection method / issue 1046 with non-zero thrust")
    void testIssue1046NonZeroThrust() {
        // Given
        final SpacecraftState stateMock = Mockito.mock(SpacecraftState.class);

        final Vector3D thrustVectorToReturn = new Vector3D(1, 2, 3);

        final ThrustPropulsionModel model = new ThrustPropulsionModel() {
            @Override
            public Vector3D getThrustVector(final SpacecraftState s) {
                return thrustVectorToReturn;
            }

            @Override
            public double getFlowRate(final SpacecraftState s) {
                return 0;
            }

            @Override
            public Vector3D getThrustVector(final SpacecraftState s, final double[] parameters) {
                return null;
            }

            @Override
            public double getFlowRate(final SpacecraftState s, final double[] parameters) {
                return 0;
            }

            @Override
            public <T extends CalculusFieldElement<T>> FieldVector3D<T> getThrustVector(final FieldSpacecraftState<T> s,
                                                                                        final T[] parameters) {
                return null;
            }

            @Override
            public <T extends CalculusFieldElement<T>> T getFlowRate(final FieldSpacecraftState<T> s, final T[] parameters) {
                return null;
            }

            @Override
            public List<ParameterDriver> getParametersDrivers() {
                return Collections.emptyList();
            }

            @Override
            public Control3DVectorCostType getControl3DVectorCostType() {
                return Control3DVectorCostType.NONE;
            }
        };

        // When
        final Vector3D returnedDirection = model.getDirection(stateMock);

        // Then
        final Vector3D expectedDirection = thrustVectorToReturn.normalize();

        // Assert that returned direction is a zero vector
        Assertions.assertEquals(expectedDirection, returnedDirection);
    }
}