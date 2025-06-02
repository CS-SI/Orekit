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
package org.orekit.forces.maneuvers.jacobians;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Test;
import org.orekit.TestUtils;
import org.orekit.forces.ForceModel;
import org.orekit.forces.maneuvers.Control3DVectorCostType;
import org.orekit.forces.maneuvers.Maneuver;
import org.orekit.forces.maneuvers.propulsion.BasicConstantThrustPropulsionModel;
import org.orekit.forces.maneuvers.propulsion.PropulsionModel;
import org.orekit.forces.maneuvers.trigger.ManeuverTriggers;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.integration.CombinedDerivatives;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ParameterDriver;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MassDepletionDelayTest {

    @Test
    void testGetDimension() {
        // GIVEN
        final Maneuver mockedManeuver = mock();
        // WHEN
        final MassDepletionDelay depletionDelay = new MassDepletionDelay("", true, mockedManeuver);
        // THEN
        assertEquals(6, depletionDelay.getDimension());
    }

    @Test
    void testCombinedDerivatives() {
        // GIVEN
        final double mass = 10;
        final double[] depletionVariables = new double[] {0., 0., 0, 1, 2, 3};
        final SpacecraftState state = new SpacecraftState(TestUtils.getDefaultOrbit(AbsoluteDate.ARBITRARY_EPOCH))
                .addAdditionalData("Orekit-depletion-", depletionVariables).withMass(mass);
        final PropulsionModel propulsionModel = new DummyPropulsionModel();
        final Maneuver maneuver = new Maneuver(null, new DummyTrigger(), propulsionModel);
        final Vector3D acceleration = new Vector3D(4, 5, 6);
        final ForceModel mockedForceModel = mock(ForceModel.class);
        final double[] parameters = new double[0];
        when(mockedForceModel.acceleration(state, parameters)).thenReturn(acceleration);
        when(mockedForceModel.getParameters(state.getDate())).thenReturn(parameters);
        final MassDepletionDelay depletionDelay = new MassDepletionDelay("", true, maneuver,
                mockedForceModel);
        // WHEN
        depletionDelay.init(state, AbsoluteDate.FUTURE_INFINITY);
        final CombinedDerivatives combinedDerivatives = depletionDelay.combinedDerivatives(state);
        // THEN
        final double[] derivatives = combinedDerivatives.getAdditionalDerivatives();
        assertEquals(depletionVariables[3], derivatives[0]);
        assertEquals(depletionVariables[4], derivatives[1]);
        assertEquals(depletionVariables[5], derivatives[2]);
        final double delta = 1e-12;
        assertEquals(-acceleration.getX() / mass, derivatives[3], delta);
        assertEquals(-acceleration.getY() / mass, derivatives[4], delta);
        assertEquals(-acceleration.getZ() / mass, derivatives[5], delta);
    }

    private static class DummyTrigger implements ManeuverTriggers {

        @Override
        public boolean isFiring(AbsoluteDate date, double[] parameters) {
            return true;
        }

        @Override
        public <T extends CalculusFieldElement<T>> boolean isFiring(FieldAbsoluteDate<T> date, T[] parameters) {
            return true;
        }

        @Override
        public Stream<EventDetector> getEventDetectors() {
            return Stream.empty();
        }

        @Override
        public <T extends CalculusFieldElement<T>> Stream<FieldEventDetector<T>> getFieldEventDetectors(Field<T> field) {
            return Stream.empty();
        }

        @Override
        public List<ParameterDriver> getParametersDrivers() {
            return Collections.emptyList();
        }
    }

    private static class DummyPropulsionModel extends BasicConstantThrustPropulsionModel {

        public DummyPropulsionModel() {
            super(0., Double.POSITIVE_INFINITY, Vector3D.MINUS_I, Control3DVectorCostType.NONE, "name");
        }
    }
}
