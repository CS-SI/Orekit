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
package org.orekit.estimation.measurements;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.util.ArrayList;
import java.util.List;

class AbstractMeasurementTest {

    @Test
    void testTheoreticalEvaluationWithoutDerivatives() {
        // GIVEN
        final ObservableSatellite satellite = new ObservableSatellite(0);
        final List<ObservableSatellite> satellites = new ArrayList<>();
        satellites.add(satellite);
        final double mass = 1.;
        final TestMeasurementModel measurement = new TestMeasurementModel(AbsoluteDate.ARBITRARY_EPOCH, mass,
                satellites);
        final int iteration = 2;
        final int evaluation = 1;
        final SpacecraftState mockedState = Mockito.mock(SpacecraftState.class);
        Mockito.when(mockedState.getDate()).thenReturn(measurement.getDate());
        Mockito.when(mockedState.getMass()).thenReturn(mass);
        Mockito.when(mockedState.getPosition()).thenReturn(Vector3D.ZERO);
        // WHEN
        final EstimatedMeasurementBase<TestMeasurementModel> measurementBase = measurement.theoreticalEvaluationWithoutDerivatives(iteration, evaluation, new SpacecraftState[] {mockedState});
        // THEN
        final EstimatedMeasurement<TestMeasurementModel> estimatedMeasurement = measurement.theoreticalEvaluation(iteration, evaluation, new SpacecraftState[] {mockedState});
        Assertions.assertArrayEquals(estimatedMeasurement.getObservedValue(), measurementBase.getObservedValue());
        Assertions.assertArrayEquals(estimatedMeasurement.getEstimatedValue(), measurementBase.getEstimatedValue());
        Assertions.assertEquals(estimatedMeasurement.getStatus(), measurementBase.getStatus());
        Assertions.assertEquals(estimatedMeasurement.getDate(), measurementBase.getDate());
        Assertions.assertEquals(estimatedMeasurement.getIteration(), measurementBase.getIteration());
        Assertions.assertEquals(estimatedMeasurement.getStates()[0].getPosition(),
                measurementBase.getStates()[0].getPosition());
    }

    private static class TestMeasurementModel extends AbstractMeasurement<TestMeasurementModel> {

        TestMeasurementModel(AbsoluteDate date, double observed, List<ObservableSatellite> satellites) {
            super(date, observed, 1., 1., satellites);
        }

        @Override
        protected EstimatedMeasurement<TestMeasurementModel> theoreticalEvaluation(int iteration, int evaluation, SpacecraftState[] states) {
            final ObservableSatellite satellite = new ObservableSatellite(0);
            final List<ObservableSatellite> satellites = new ArrayList<>();
            satellites.add(satellite);
            final TestMeasurementModel testMeasurement = new TestMeasurementModel(states[0].getDate(), states[0].getMass(),
                    satellites);
            final EstimatedMeasurement<TestMeasurementModel> measurement = new EstimatedMeasurement<>(testMeasurement,
                    iteration, evaluation, states, new TimeStampedPVCoordinates[] {states[0].getPVCoordinates()});
            measurement.setEstimatedValue(testMeasurement.getObservedValue());
            return measurement;
        }
    }
}
